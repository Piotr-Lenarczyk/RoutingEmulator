package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Minimal forwarding engine: simulates forwarding of a packet through routers using static routing table and connections in NetworkTopology.
 * This is intentionally simple and deterministic for testing purposes.
 */
public class ForwardingEngine {

    private static final Logger logger = Logger.getLogger(ForwardingEngine.class.getName());

    private static final int DEFAULT_TTL = 64;

    private static final String NEXT_HOP_NOT_FOUND = "Next-hop router not found";
    private static final String HOST_NOT_FOUND_ON_SUBNET = "Host not found on connected subnet";
    private static final String FORWARDING_SUCCESS = "Forwarding success: reached destination router %s interface %s";
    private static final String ROUTER_RETURN_REACHED = "Return reached (router interface)";
    private static final String NO_RETURN_ROUTE = "No return route";
    private static final String FORWARDING_FAILURE_NO_RETURN_ROUTE = "Forwarding failure: no return route from destination router %s to source IP";
    private static final String INTERFACE_ADMIN_DOWN = "Exit interface administratively down";
    private static final String NEXT_HOP_NOT_IN_TOPOLOGY = "Next-hop not found in topology";
    private static final String TTL_EXPIRED = "TTL expired";
    private static final String INTERFACE_NOT_CONNECTED = "Exit interface not connected";
    private static final String NO_ROUTE = "No route";
    private static final String FORWARDING_FAILURE_INTERFACE_ADMIN_DOWN = "Forwarding failure: exit interface %s on router %s is administratively down";
    private static final String INVALID_ROUTE = "Invalid route";
    private static final String UNSUPPORTED_NEIGHBOR_TYPE = "Unsupported neighbor type";
    private static final String ROUTER_INTERFACE_REACHED = "Reached (router interface)";

    public ForwardingOutcome forward(Packet packet, Host srcHost, NetworkTopology topology) {
        logger.fine("Starting forwarding of packet from %s to %s".formatted(packet.getSource(), packet.getDestination()));

        normalizeTtl(packet);

        if (isDestinationOnHostSubnet(packet, srcHost)) {
            return new ForwardingOutcome(true, 1, "Reached (same subnet)");
        }

        GatewayResolution gateway = resolveHostGateway(srcHost, topology);
        if (gateway.failure() != null) {
            return gateway.failure();
        }

        return traverse(packet, gateway.router(), 1, topology, true);
    }

    public ForwardingOutcome forward(Packet packet, Router srcRouter, NetworkTopology topology) {
        logger.fine("Starting forwarding (router source) of packet from %s to %s".formatted(packet.getSource(), packet.getDestination()));

        normalizeTtl(packet);

        return traverse(packet, srcRouter, 0, topology, false);
    }

    private void normalizeTtl(Packet packet) {
        if (packet.getTtl() <= 0) {
            packet.setTtl(DEFAULT_TTL);
        }
    }

    private boolean isDestinationOnHostSubnet(Packet packet, Host srcHost) {
        HostInterface hostInterface = srcHost.getHostInterface();
        if (hostInterface == null || hostInterface.getSubnet() == null) {
            return false;
        }

        boolean sameSubnet = hostInterface.getSubnet().contains(packet.getDestination());
        if (sameSubnet) {
            logger.fine("Forwarding success: destination %s is in the same subnet as source host %s"
                    .formatted(packet.getDestination(), srcHost.getHostname()));
        }
        return sameSubnet;
    }

    private GatewayResolution resolveHostGateway(Host srcHost, NetworkTopology topology) {
        if (srcHost.getHostInterface() == null || srcHost.getHostInterface().getDefaultGateway() == null) {
            logger.fine("Forwarding failure: no default gateway configured for host %s".formatted(srcHost.getHostname()));
            return GatewayResolution.failed(new ForwardingOutcome(false, 0, "No default gateway configured"));
        }

        logger.finer("Looking for connection from host %s to its default gateway".formatted(srcHost.getHostname()));
        Connection conn = topology.getConnectionForInterface(srcHost.getHostInterface());

        if (conn == null) {
            logger.fine("Forwarding failure: host %s is not connected to any router".formatted(srcHost.getHostname()));
            return GatewayResolution.failed(new ForwardingOutcome(false, 0, "Host not connected to topology"));
        }

        NetworkInterface neighbor = conn.getNeighborInterface(srcHost.getHostInterface());

        if (!(neighbor instanceof RouterInterface currentInterface)) {
            logger.fine("Forwarding failure: default gateway for host %s is not a router interface".formatted(srcHost.getHostname()));
            return GatewayResolution.failed(new ForwardingOutcome(false, 0, "Default gateway is not a router interface"));
        }

        Router currentRouter = findRouterOwningInterface(topology, currentInterface);
        if (currentRouter == null) {
            logger.fine("Forwarding failure: cannot find router for gateway interface of host %s".formatted(srcHost.getHostname()));
            return GatewayResolution.failed(new ForwardingOutcome(false, 0, "Cannot find router for gateway interface"));
        }

        logger.finer("Default gateway reached. Starting hop-by-hop forwarding from router %s".formatted(currentRouter.getName()));
        return GatewayResolution.of(currentRouter);
    }

    private ForwardingOutcome traverse(Packet packet, Router startRouter, int startHops,
                                       NetworkTopology topology, boolean verifyOwnAddressReturn) {
        Router currentRouter = startRouter;
        int hops = startHops;

        while (true) {
            if (packet.decrementTTL()) {
                logger.fine("Forwarding failure: TTL expired while forwarding from router %s".formatted(currentRouter.getName()));
                return new ForwardingOutcome(false, hops, TTL_EXPIRED);
            }

            logger.finer("Checking interfaces of router %s for destination %s".formatted(currentRouter.getName(), packet.getDestination()));
            Optional<RouterInterface> intfToDst = RouteSelector.findDirectSubnetInterface(currentRouter, packet.getDestination());
            if (intfToDst.isPresent()) {
                return resolveDirectSubnet(currentRouter, intfToDst.get(), packet, topology, hops, verifyOwnAddressReturn);
            }

            logger.finer("No directly connected subnet matches destination. Looking for static routes on router %s".formatted(currentRouter.getName()));
            RouteStep step = resolveNextRouterViaStaticRoute(currentRouter, packet.getDestination(), topology, hops);
            if (step.outcome() != null) {
                return step.outcome();
            }

            currentRouter = step.nextRouter();
            hops = step.hops();
        }
    }

    private ForwardingOutcome resolveDirectSubnet(Router currentRouter, RouterInterface dstIf, Packet packet,
                                                  NetworkTopology topology, int hopsBeforeThisHop,
                                                  boolean verifyOwnAddressReturn) {

        if (dstIf.isDisabled()) {
            logger.fine(FORWARDING_FAILURE_INTERFACE_ADMIN_DOWN.formatted(dstIf.getInterfaceName(), currentRouter.getName()));
            return new ForwardingOutcome(false, hopsBeforeThisHop + 1, INTERFACE_ADMIN_DOWN);
        }

        int hops = hopsBeforeThisHop + 1;

        if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().ipAddress().equals(packet.getDestination())) {
            return resolveOwnInterfaceReached(currentRouter, dstIf, packet, topology, hops, verifyOwnAddressReturn);
        }

        NetworkInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(dstIf, packet.getDestination());
        if (foundHost instanceof HostInterface hi) {
            return resolveHostOnSubnetReached(currentRouter, dstIf, hi, packet, topology, hops, verifyOwnAddressReturn);
        }

        RouterInterface neighborRouterIf = findInterfaceByIp(topology, packet.getDestination());
        if (neighborRouterIf != null && isDirectlyConnectedNeighbor(topology, dstIf, neighborRouterIf)) {
            return resolveNeighborInterfaceReached(neighborRouterIf, packet, topology, hops);
        }

        logger.fine("Forwarding failure: no host with IP %s found on subnet connected to router %s interface %s"
                .formatted(packet.getDestination(), currentRouter.getName(), dstIf.getInterfaceName()));
        return new ForwardingOutcome(false, hops, HOST_NOT_FOUND_ON_SUBNET);
    }

    private ForwardingOutcome resolveOwnInterfaceReached(Router currentRouter, RouterInterface dstIf, Packet packet,
                                                         NetworkTopology topology, int hops, boolean verifyReturn) {
        if (!verifyReturn) {
            logger.fine(FORWARDING_SUCCESS.formatted(currentRouter.getName(), dstIf.getInterfaceName()));
            return new ForwardingOutcome(true, hops, ROUTER_INTERFACE_REACHED);
        }

        Router dstRouter = findRouterOwningInterface(topology, dstIf);
        if (dstRouter != null) {
            if (verifyReturnRouteFromRouter(dstRouter, dstIf, packet.getSource(), topology)) {
                logger.fine(FORWARDING_FAILURE_NO_RETURN_ROUTE.formatted(dstRouter.getName()));
                return new ForwardingOutcome(false, hops, NO_RETURN_ROUTE);
            }
            logger.fine(FORWARDING_SUCCESS.formatted(dstRouter.getName(), dstIf.getInterfaceName()));
        }

        return new ForwardingOutcome(true, hops, ROUTER_INTERFACE_REACHED);
    }

    private ForwardingOutcome resolveHostOnSubnetReached(Router currentRouter, RouterInterface dstIf, HostInterface foundHost,
                                                         Packet packet, NetworkTopology topology, int hops, boolean verifyReturn) {
        if (verifyReturn && !verifyReturnRouteFromHost(foundHost, packet.getSource(), topology)) {
            logger.fine("Forwarding failure: no return route from destination host  to source IP");
            return new ForwardingOutcome(false, hops, NO_RETURN_ROUTE);
        }

        logger.fine("Forwarding success: reached destination host via router %s interface %s"
                .formatted(currentRouter.getName(), dstIf.getInterfaceName()));
        return new ForwardingOutcome(true, hops, "Reached (host)");
    }

    private ForwardingOutcome resolveNeighborInterfaceReached(RouterInterface neighborRouterIf, Packet packet,
                                                              NetworkTopology topology, int hops) {
        Router dstRouter = findRouterOwningInterface(topology, neighborRouterIf);
        if (dstRouter != null) {
            if (verifyReturnRouteFromRouter(dstRouter, neighborRouterIf, packet.getSource(), topology)) {
                logger.fine(FORWARDING_FAILURE_NO_RETURN_ROUTE.formatted(dstRouter.getName()));
                return new ForwardingOutcome(false, hops, NO_RETURN_ROUTE);
            }
            logger.fine(FORWARDING_SUCCESS.formatted(dstRouter.getName(), neighborRouterIf.getInterfaceName()));
        }

        return new ForwardingOutcome(true, hops, ROUTER_INTERFACE_REACHED);
    }

    private RouteStep resolveNextRouterViaStaticRoute(Router currentRouter, IPAddress destination,
                                                      NetworkTopology topology, int hopsBeforeThisHop) {

        Optional<StaticRoutingEntry> routeOpt = RouteSelector.findStaticRoute(currentRouter, destination);

        if (routeOpt.isEmpty()) {
            logger.fine("Forwarding failure: no route to destination %s on router %s".formatted(destination, currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(false, hopsBeforeThisHop, NO_ROUTE));
        }

        StaticRoutingEntry route = routeOpt.get();
        int hops = hopsBeforeThisHop + 1;

        if (route.getRouterInterface() != null) {
            return resolveInterfaceRoute(currentRouter, route.getRouterInterface(), destination, topology, hops);
        }

        if (route.getNextHop() != null) {
            return resolveNextHopRoute(currentRouter, route.getNextHop(), topology, hops);
        }

        logger.fine("Forwarding failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
        return RouteStep.terminal(new ForwardingOutcome(false, hops, INVALID_ROUTE));
    }

    private RouteStep resolveInterfaceRoute(Router currentRouter, RouterInterface exitIf, IPAddress destination,
                                            NetworkTopology topology, int hops) {
        if (exitIf.isDisabled()) {
            logger.fine(FORWARDING_FAILURE_INTERFACE_ADMIN_DOWN.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(false, hops, INTERFACE_ADMIN_DOWN));
        }

        Connection exitConn = topology.getConnectionForInterface(exitIf);
        if (exitConn == null) {
            logger.fine("Forwarding failure: exit interface %s on router %s is not connected to any other interface"
                    .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(false, hops, INTERFACE_NOT_CONNECTED));
        }

        NetworkInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(exitIf, destination);
        if (foundHost instanceof HostInterface) {
            logger.fine("Forwarding success: reached destination host via exit interface %s on router %s"
                    .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(true, hops, "Reached host"));
        }

        NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);

        if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
            Router neighborRouter = findRouterOwningInterface(topology, neighborRouterIf);
            if (neighborRouter == null) {
                logger.fine("Forwarding failure: neighbor router for exit interface %s on router %s not found"
                        .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                return RouteStep.terminal(new ForwardingOutcome(false, hops, "Neighbor router not found"));
            }
            return RouteStep.advance(neighborRouter, hops);
        }

        logger.fine("Forwarding failure: unsupported neighbor type connected to exit interface %s on router %s"
                .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
        return RouteStep.terminal(new ForwardingOutcome(false, hops, UNSUPPORTED_NEIGHBOR_TYPE));
    }


    private RouteStep resolveNextHopRoute(Router currentRouter, IPAddress nextHop, NetworkTopology topology, int hops) {
        RouterInterface foundIf = findInterfaceByIp(topology, nextHop);
        if (foundIf == null) {
            logger.fine("Forwarding failure: next-hop IP %s for route on router %s not found in topology"
                    .formatted(nextHop, currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(false, hops, NEXT_HOP_NOT_IN_TOPOLOGY));
        }

        Router neighborRouter = findRouterOwningInterface(topology, foundIf);
        if (neighborRouter == null) {
            logger.fine("Forwarding failure: next-hop router for IP %s on router %s not found".formatted(nextHop, currentRouter.getName()));
            return RouteStep.terminal(new ForwardingOutcome(false, hops, NEXT_HOP_NOT_FOUND));
        }

        return RouteStep.advance(neighborRouter, hops);
    }


    private boolean verifyReturnRouteFromRouter(Router dstRouter, RouterInterface dstIf, IPAddress srcIp, NetworkTopology topology) {
        logger.finer("Verifying return route from destination router %s interface %s to source IP %s"
                .formatted(dstRouter.getName(), dstIf.getInterfaceName(), srcIp));

        ForwardingOutcome outcome = forwardFromRouter(dstRouter, dstIf, srcIp, topology);

        logger.finest("Return route verification result: %s".formatted(outcome.reached() ? "reachable" : "unreachable"));
        return !outcome.reached();
    }

    private boolean verifyReturnRouteFromHost(HostInterface dstHostIf, IPAddress srcIp, NetworkTopology topology) {
        logger.finest("Verifying return route from destination host interface %s to source IP %s"
                .formatted(dstHostIf.getInterfaceName(), srcIp));

        if (dstHostIf.getDefaultGateway() == null) {
            logger.finest("Return route verification failure: destination host interface has no default gateway configured");
            return false;
        }

        RouterInterface gatewayIf = findInterfaceByIp(topology, dstHostIf.getDefaultGateway());
        if (gatewayIf == null) {
            logger.finest("Return route verification failure: cannot find gateway interface for destination host's default gateway IP %s"
                    .formatted(dstHostIf.getDefaultGateway()));
            return false;
        }

        Router gatewayRouter = findRouterOwningInterface(topology, gatewayIf);
        if (gatewayRouter == null) {
            logger.finest("Return route verification failure: cannot find router owning gateway interface %s".formatted(gatewayIf.getInterfaceName()));
            return false;
        }

        ForwardingOutcome outcome = forwardFromRouter(gatewayRouter, gatewayIf, srcIp, topology);
        logger.finest("Return route verification result: %s".formatted(outcome.reached() ? "reachable" : "unreachable"));
        return outcome.reached();
    }


    private ForwardingOutcome forwardFromRouter(Router startRouter, RouterInterface startIf, IPAddress dstIp, NetworkTopology topology) {
        logger.finer("Forwarding from router %s interface %s to destination IP %s"
                .formatted(startRouter.getName(), startIf.getInterfaceName(), dstIp));

        Router currentRouter = startRouter;
        int hops = 0;
        int maxHops = 128;

        while (hops < maxHops) {
            hops++;

            Optional<RouterInterface> intfToDst = RouteSelector.findDirectSubnetInterface(currentRouter, dstIp);
            if (intfToDst.isPresent()) {
                return resolveReturnRouteDirectSubnet(currentRouter, intfToDst.get(), dstIp, topology, hops);
            }

            ReturnRouteStep step = resolveReturnRouteViaStaticRoute(currentRouter, dstIp, topology, hops);
            if (step.outcome() != null) {
                return step.outcome();
            }

            currentRouter = step.nextRouter();
        }

        logger.finer("Return route verification failure: maximum hops exceeded while forwarding from router %s".formatted(startRouter.getName()));
        return new ForwardingOutcome(false, hops, TTL_EXPIRED);
    }

    private ForwardingOutcome resolveReturnRouteDirectSubnet(Router currentRouter, RouterInterface dstIf, IPAddress dstIp,
                                                             NetworkTopology topology, int hops) {

        if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().ipAddress().equals(dstIp)) {
            logger.finer("Return route verification success: destination IP %s matches router %s interface %s"
                    .formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
            return new ForwardingOutcome(true, hops, ROUTER_RETURN_REACHED);
        }

        NetworkInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(dstIf, dstIp);
        if (foundHost instanceof HostInterface) {
            logger.finer("Return route verification success: destination IP %s matches host reachable from router %s interface %s"
                    .formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
            return new ForwardingOutcome(true, hops, "Return reached (host)");
        }

        RouterInterface neighborRouterIf = findInterfaceByIp(topology, dstIp);
        if (neighborRouterIf != null && isDirectlyConnectedNeighbor(topology, dstIf, neighborRouterIf)) {
            logger.finer("Return route verification success: destination IP %s matches neighbor router interface %s on router %s"
                    .formatted(dstIp, neighborRouterIf.getInterfaceName(), currentRouter.getName()));
            return new ForwardingOutcome(true, hops, ROUTER_RETURN_REACHED);
        }

        logger.finer("Return route verification failure: no host with IP %s found on subnet connected to router %s interface %s"
                .formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
        return new ForwardingOutcome(false, hops, HOST_NOT_FOUND_ON_SUBNET);
    }

    private ReturnRouteStep resolveReturnRouteViaStaticRoute(Router currentRouter, IPAddress dstIp,
                                                             NetworkTopology topology, int hops) {

        Optional<StaticRoutingEntry> routeOpt = RouteSelector.findStaticRoute(currentRouter, dstIp);

        if (routeOpt.isEmpty()) {
            logger.finer("Return route verification failure: no route to destination IP %s on router %s".formatted(dstIp, currentRouter.getName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, NO_ROUTE));
        }

        StaticRoutingEntry route = routeOpt.get();

        if (route.getRouterInterface() != null) {
            return resolveReturnRouteInterfaceRoute(currentRouter, route.getRouterInterface(), dstIp, topology, hops);
        }

        if (route.getNextHop() != null) {
            return resolveReturnRouteNextHop(currentRouter, route.getNextHop(), topology, hops);
        }

        logger.finer("Return route verification failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
        return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, INVALID_ROUTE));
    }


    private ReturnRouteStep resolveReturnRouteInterfaceRoute(Router currentRouter, RouterInterface exitIf, IPAddress dstIp,
                                                             NetworkTopology topology, int hops) {

        Connection exitConn = topology.getConnectionForInterface(exitIf);
        if (exitConn == null) {
            logger.finer("Return route verification failure: exit interface %s on router %s is not connected to any other interface"
                    .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, INTERFACE_NOT_CONNECTED));
        }

        NetworkInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(exitIf, dstIp);
        if (foundHost instanceof HostInterface) {
            logger.finer("Return route verification success: destination IP %s matches host reachable from router %s exit interface %s"
                    .formatted(dstIp, currentRouter.getName(), exitIf.getInterfaceName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(true, hops, "Return reached host"));
        }

        RouterInterface neighborIf = findInterfaceByIp(topology, dstIp);
        if (neighborIf != null && isDirectlyConnectedNeighbor(topology, exitIf, neighborIf)) {
            logger.finer("Return route verification success: destination IP %s matches neighbor router interface %s on router %s"
                    .formatted(dstIp, neighborIf.getInterfaceName(), currentRouter.getName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(true, hops, ROUTER_RETURN_REACHED));
        }


        NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);

        if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
            Router neighborRouter = findRouterOwningInterface(topology, neighborRouterIf);
            if (neighborRouter == null) {
                logger.finer("Return route verification failure: neighbor router for exit interface %s on router %s not found"
                        .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, NEXT_HOP_NOT_FOUND));
            }
            return ReturnRouteStep.advance(neighborRouter);
        }

        logger.finer("Return route verification failure: unsupported neighbor type connected to exit interface %s on router %s"
                .formatted(exitIf.getInterfaceName(), currentRouter.getName()));
        return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, UNSUPPORTED_NEIGHBOR_TYPE));
    }

    private ReturnRouteStep resolveReturnRouteNextHop(Router currentRouter, IPAddress nextHop, NetworkTopology topology, int hops) {
        RouterInterface foundIf = findInterfaceByIp(topology, nextHop);
        if (foundIf == null) {
            logger.finer("Return route verification failure: next-hop IP %s for route on router %s not found in topology"
                    .formatted(nextHop, currentRouter.getName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, NEXT_HOP_NOT_IN_TOPOLOGY));
        }

        Router neighborRouter = findRouterOwningInterface(topology, foundIf);
        if (neighborRouter == null) {
            logger.finer("Return route verification failure: next-hop router for IP %s on router %s not found".formatted(nextHop, currentRouter.getName()));
            return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, NEXT_HOP_NOT_FOUND));
        }

        return ReturnRouteStep.advance(neighborRouter);
    }


    private boolean isDirectlyConnectedNeighbor(NetworkTopology topology, NetworkInterface localIf, NetworkInterface candidate) {
        Connection directConn = topology.getConnectionForInterface(localIf);
        return directConn != null
                && directConn.getNeighborInterface(localIf) instanceof RouterInterface
                && directConn.getNeighborInterface(localIf).equals(candidate);
    }

    private Router findRouterOwningInterface(NetworkTopology topology, RouterInterface iface) {
        for (Device d : topology.getDevices()) {
            if (d instanceof Router r) {
                for (RouterInterface ri : r.getInterfaces()) {
                    if (ri.equals(iface)) return r;
                }
            }
        }
        return null;
    }

    private RouterInterface findInterfaceByIp(NetworkTopology topology, IPAddress ip) {
        for (Device d : topology.getDevices()) {
            if (d instanceof Router r) {
                for (RouterInterface ri : r.getInterfaces()) {
                    if (ri.getInterfaceAddress() != null && ri.getInterfaceAddress().ipAddress().equals(ip)) {
                        return ri;
                    }
                }
            }
        }
        return null;
    }

    private record GatewayResolution(Router router, ForwardingOutcome failure) {
        static GatewayResolution of(Router router) {
            return new GatewayResolution(router, null);
        }

        static GatewayResolution failed(ForwardingOutcome outcome) {
            return new GatewayResolution(null, outcome);
        }
    }

    private record RouteStep(Router nextRouter, int hops, ForwardingOutcome outcome) {
        static RouteStep advance(Router router, int hops) {
            return new RouteStep(router, hops, null);
        }

        static RouteStep terminal(ForwardingOutcome outcome) {
            return new RouteStep(null, 0, outcome);
        }
    }

    private record ReturnRouteStep(Router nextRouter, ForwardingOutcome outcome) {
        static ReturnRouteStep advance(Router router) {
            return new ReturnRouteStep(router, null);
        }

        static ReturnRouteStep terminal(ForwardingOutcome outcome) {
            return new ReturnRouteStep(null, outcome);
        }
    }
}