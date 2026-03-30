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

    /**
     * Forwards the packet starting from the source host towards destination IP using topology and routers.
     * Returns ForwardingOutcome with reached=true if destination is reachable.
     */
    public ForwardingOutcome forward(Packet packet, Host srcHost, NetworkTopology topology) {
        logger.fine("Starting forwarding of packet from %s to %s".formatted(packet.getSource(), packet.getDestination()));
        packet.setTtl(DEFAULT_TTL);

        // If destination is in the same subnet as source host, deliver immediately (1 hop)
        if (srcHost.getHostInterface() != null && srcHost.getHostInterface().getSubnet() != null) {
            Subnet hostSubnet = srcHost.getHostInterface().getSubnet();
            // Check if destination IP belongs to this subnet
            if (belongsToSubnet(packet.getDestination(), hostSubnet)) {
                logger.fine("Forwarding success: destination %s is in the same subnet as source host %s".formatted(packet.getDestination(), srcHost.getHostname()));
                return new ForwardingOutcome(true, 1, "Reached (same subnet)");
            }
        }

        // Otherwise, send to default gateway
        if (srcHost.getHostInterface() == null || srcHost.getHostInterface().getDefaultGateway() == null) {
            logger.fine("Forwarding failure: no default gateway configured for host %s".formatted(srcHost.getHostname()));
            return new ForwardingOutcome(false, 0, "No default gateway configured");
        }

        // Find the router interface that is connected to the host's interface (via Connection)
        logger.finer("Looking for connection from host %s to its default gateway".formatted(srcHost.getHostname()));
        Connection conn = topology.getConnectionForInterface(srcHost.getHostInterface());
        if (conn == null) {
            logger.fine("Forwarding failure: host %s is not connected to any router".formatted(srcHost.getHostname()));
            return new ForwardingOutcome(false, 0, "Host not connected to topology");
        }
        NetworkInterface neighbor = conn.getNeighborInterface(srcHost.getHostInterface());
        if (!(neighbor instanceof RouterInterface currentInterface)) {
            logger.fine("Forwarding failure: default gateway for host %s is not a router interface".formatted(srcHost.getHostname()));
            return new ForwardingOutcome(false, 0, "Default gateway is not a router interface");
        }

        // Find the router owning this interface
        Router currentRouter = findRouterOwningInterface(topology, currentInterface);
        if (currentRouter == null) {
            logger.fine("Forwarding failure: cannot find router for gateway interface of host %s".formatted(srcHost.getHostname()));
            return new ForwardingOutcome(false, 0, "Cannot find router for gateway interface");
        }

        int hops = 1; // host->first-router

        // Now perform hop-by-hop forwarding using router routing tables
        logger.finer("Default gateway reached. Starting hop-by-hop forwarding from router %s".formatted(currentRouter.getName()));
        while (true) {
            if (!packet.decrementTTL()) {
                logger.fine("Forwarding failure: TTL expired while forwarding from router %s".formatted(currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "TTL expired");
            }

            // Check if any interface on currentRouter has subnet containing destination
            logger.finer("Checking interfaces of router %s for destination %s".formatted(currentRouter.getName(), packet.getDestination()));
            Optional<RouterInterface> intfToDst = currentRouter.getInterfaces().stream()
                    .filter(iface -> iface.getSubnet() != null && belongsToSubnet(packet.getDestination(), iface.getSubnet()))
                    .findFirst();
            if (intfToDst.isPresent()) {
                logger.finest("Destination directly connected to router %s via interface %s".formatted(currentRouter.getName(), intfToDst.get().getInterfaceName()));
                // Destination is on a directly connected subnet of currentRouter
                RouterInterface dstIf = intfToDst.get();

                // If the interface itself is administratively disabled, treat as blackhole
                if (dstIf.isDisabled()) {
                    logger.fine("Forwarding failure: exit interface %s on router %s is administratively down".formatted(dstIf.getInterfaceName(), currentRouter.getName()));
                    return new ForwardingOutcome(false, hops + 1, "Exit interface administratively down");
                }

                // If the destination equals the router's own interface address -> reached
                if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().getIpAddress().equals(packet.getDestination())) {
                    hops++;
                    // Verify return route from destination router back to source (strict)
                    Router dstRouter = findRouterOwningInterface(topology, dstIf);
                    if (dstRouter != null) {
                        if (!verifyReturnRouteFromRouter(dstRouter, dstIf, packet.getSource(), topology)) {
                            logger.fine("Forwarding failure: no return route from destination router %s to source IP");
                            return new ForwardingOutcome(false, hops, "No return route");
                        }
                        logger.fine("Forwarding success: reached destination router %s interface %s".formatted(dstRouter.getName(), dstIf.getInterfaceName()));
                    }
                    return new ForwardingOutcome(true, hops, "Reached (router interface)");
                }

                // Otherwise, try to discover a host with exact IP reachable from this router interface (through switches)
                logger.finest("Looking for host with IP %s reachable from router %s interface %s".formatted(packet.getDestination(), currentRouter.getName(), dstIf.getInterfaceName()));
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(dstIf, packet.getDestination());
                if (foundHost != null) {
                    hops++;
                    // Verify return route from host back to source (strict)
                    if (!verifyReturnRouteFromHost(foundHost, packet.getSource(), topology)) {
                        logger.fine("Forwarding failure: no return route from destination host  to source IP");
                        return new ForwardingOutcome(false, hops, "No return route");
                    }
                    logger.fine("Forwarding success: reached destination host via router %s interface %s".formatted(currentRouter.getName(), dstIf.getInterfaceName()));
                    return new ForwardingOutcome(true, hops, "Reached (host)");
                }

                // If there is no host with exact IP on this connected segment -> unreachable for our simplified model
                logger.fine("Forwarding failure: no host with IP %s found on subnet connected to router %s interface %s".formatted(packet.getDestination(), currentRouter.getName(), dstIf.getInterfaceName()));
                return new ForwardingOutcome(false, hops + 1, "Host not found on connected subnet");
            }

            // Otherwise, find best static route (first matching non-disabled route)
            logger.finer("No directly connected subnet matches destination. Looking for static routes on router %s".formatted(currentRouter.getName()));
            Optional<StaticRoutingEntry> routeOpt = currentRouter.getRoutingTable().getRoutingEntries().stream()
                    .filter(e -> !e.isDisabled() && belongsToSubnet(packet.getDestination(), e.getSubnet()))
                    .findFirst();

            if (routeOpt.isEmpty()) {
                logger.fine("Forwarding failure: no route to destination %s on router %s".formatted(packet.getDestination(), currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "No route");
            }

            StaticRoutingEntry route = routeOpt.get();
            hops++;

            // Next hop via interface
            if (route.getRouterInterface() != null) {
                RouterInterface exitIf = route.getRouterInterface();

                // If the exit interface is administratively disabled, this is a blackhole
                if (exitIf.isDisabled()) {
                    logger.fine("Forwarding failure: exit interface %s on router %s is administratively down".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Exit interface administratively down");
                }

                // Find the connection where this interface participates
                Connection exitConn = topology.getConnectionForInterface(exitIf);
                if (exitConn == null) {
                    logger.fine("Forwarding failure: exit interface %s on router %s is not connected to any other interface".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Exit interface not connected");
                }

                // First, check if there's any host reachable from this exit interface that has the exact IP
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(exitIf, packet.getDestination());
                if (foundHost != null) {
                    logger.fine("Forwarding success: reached destination host via exit interface %s on router %s".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                    return new ForwardingOutcome(true, hops, "Reached host");
                }

                NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);
                // If neighbor is router interface -> move to that router
                if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
                    Router neighborRouter = findRouterOwningInterface(topology, neighborRouterIf);
                    if (neighborRouter == null) {
                        logger.fine("Forwarding failure: neighbor router for exit interface %s on router %s not found".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                        return new ForwardingOutcome(false, hops, "Neighbor router not found");
                    }
                    currentRouter = neighborRouter;
                    continue;
                }

                // Other neighbor types - assume unreachable
                logger.fine("Forwarding failure: unsupported neighbor type connected to exit interface %s on router %s".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "Unsupported neighbor type");
            } else if (route.getNextHop() != null) {
                // Next-hop based route: find interface with this IP in topology
                IPAddress nh = route.getNextHop();
                // Search all router interfaces for an interface address exactly equal to nh
                RouterInterface foundIf = findInterfaceByIp(topology, nh);
                if (foundIf == null) {
                    logger.fine("Forwarding failure: next-hop IP %s for route on router %s not found in topology".formatted(nh, currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Next-hop not found in topology");
                }
                // Move to the router owning foundIf
                Router neighborRouter = findRouterOwningInterface(topology, foundIf);
                if (neighborRouter == null) {
                    logger.fine("Forwarding failure: next-hop router for IP %s on router %s not found".formatted(nh, currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Next-hop router not found");
                }
                currentRouter = neighborRouter;
            } else {
                logger.fine("Forwarding failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "Invalid route");
            }
        }
    }

    // Helper: verify that destination router/interface can reach source IP (strict simulation)
    private boolean verifyReturnRouteFromRouter(Router dstRouter, RouterInterface dstIf, IPAddress srcIp, NetworkTopology topology) {
        logger.finer("Verifying return route from destination router %s interface %s to source IP %s".formatted(dstRouter.getName(), dstIf.getInterfaceName(), srcIp));
        ForwardingOutcome outcome = forwardFromRouter(dstRouter, dstIf, srcIp, topology);
        logger.finest("Return route verification result: %s".formatted(outcome.isReached() ? "reachable" : "unreachable"));
        return outcome.isReached();
    }

    // Helper: verify that destination host can reach source IP (via its default gateway) (strict)
    private boolean verifyReturnRouteFromHost(HostInterface dstHostIf, IPAddress srcIp, NetworkTopology topology) {
        logger.finest("Verifying return route from destination host interface %s to source IP %s".formatted(dstHostIf.getInterfaceName(), srcIp));
        if (dstHostIf.getDefaultGateway() == null) {
            logger.finest("Return route verification failure: destination host interface has no default gateway configured");
            return false;
        }
        // Try to find the router interface that matches the host's configured gateway IP
        RouterInterface gatewayIf = findInterfaceByIp(topology, dstHostIf.getDefaultGateway());
        if (gatewayIf == null) {
            logger.finest("Return route verification failure: cannot find gateway interface for destination host's default gateway IP %s".formatted(dstHostIf.getDefaultGateway()));
            return false;
        }
        Router gatewayRouter = findRouterOwningInterface(topology, gatewayIf);
        if (gatewayRouter == null) {
            logger.finest("Return route verification failure: cannot find router owning gateway interface %s".formatted(gatewayIf.getInterfaceName()));
            return false;
        }
        ForwardingOutcome outcome = forwardFromRouter(gatewayRouter, gatewayIf, srcIp, topology);
        logger.finest("Return route verification result: %s".formatted(outcome.isReached() ? "reachable" : "unreachable"));
        return outcome.isReached();
    }

    // Simulate forwarding originating at a router/interface towards a destination IP
    private ForwardingOutcome forwardFromRouter(Router startRouter, RouterInterface startIf, IPAddress dstIp, NetworkTopology topology) {
        logger.finer("Forwarding from router %s interface %s to destination IP %s".formatted(startRouter.getName(), startIf.getInterfaceName(), dstIp));
        Router currentRouter = startRouter;
        int hops = 0;
        int maxHops = 128;

        while (hops < maxHops) {
            hops++;
            // Check if any interface on currentRouter has subnet containing destination
            Optional<RouterInterface> intfToDst = currentRouter.getInterfaces().stream()
                    .filter(iface -> iface.getSubnet() != null && belongsToSubnet(dstIp, iface.getSubnet()))
                    .findFirst();
            if (intfToDst.isPresent()) {
                RouterInterface dstIf = intfToDst.get();
                // If destination equals router's own interface
                if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().getIpAddress().equals(dstIp)) {
                    logger.finer("Return route verification success: destination IP %s matches router %s interface %s".formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
                    return new ForwardingOutcome(true, hops, "Return reached (router interface)");
                }
                // Otherwise, check for host on that connected segment
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(dstIf, dstIp);
                if (foundHost != null) {
                    logger.finer("Return route verification success: destination IP %s matches host reachable from router %s interface %s".formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
                    return new ForwardingOutcome(true, hops, "Return reached (host)");
                }
                logger.finer("Return route verification failure: no host with IP %s found on subnet connected to router %s interface %s".formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
                return new ForwardingOutcome(false, hops, "Host not found on connected subnet");
            }

            // Find route on current router
            Optional<StaticRoutingEntry> routeOpt = currentRouter.getRoutingTable().getRoutingEntries().stream()
                    .filter(e -> !e.isDisabled() && belongsToSubnet(dstIp, e.getSubnet()))
                    .findFirst();
            if (routeOpt.isEmpty()) {
                logger.finer("Return route verification failure: no route to destination IP %s on router %s".formatted(dstIp, currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "No route");
            }
            StaticRoutingEntry route = routeOpt.get();

            // Next hop via interface
            if (route.getRouterInterface() != null) {
                RouterInterface exitIf = route.getRouterInterface();
                Connection exitConn = topology.getConnectionForInterface(exitIf);
                if (exitConn == null) {
                    logger.finer("Return route verification failure: exit interface %s on router %s is not connected to any other interface".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Exit interface not connected");
                }

                // If host with exact IP reachable from this exit interface
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(exitIf, dstIp);
                if (foundHost != null) {
                    logger.finer("Return route verification success: destination IP %s matches host reachable from router %s exit interface %s".formatted(dstIp, currentRouter.getName(), exitIf.getInterfaceName()));
                    return new ForwardingOutcome(true, hops, "Return reached host");
                }

                NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);
                if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
                    Router neighborRouter = findRouterOwningInterface(topology, neighborRouterIf);
                    if (neighborRouter == null) {
                        logger.finer("Return route verification failure: neighbor router for exit interface %s on router %s not found".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                        return new ForwardingOutcome(false, hops, "Next-hop router not found");
                    }
                    currentRouter = neighborRouter;
                    continue;
                }
                logger.finer("Return route verification failure: unsupported neighbor type connected to exit interface %s on router %s".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "Unsupported neighbor type");
            } else if (route.getNextHop() != null) {
                IPAddress nh = route.getNextHop();
                RouterInterface foundIf = findInterfaceByIp(topology, nh);
                if (foundIf == null) {
                    logger.finer("Return route verification failure: next-hop IP %s for route on router %s not found in topology".formatted(nh, currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Next-hop not found in topology");
                }
                Router neighborRouter = findRouterOwningInterface(topology, foundIf);
                if (neighborRouter == null) {
                    logger.finer("Return route verification failure: next-hop router for IP %s on router %s not found".formatted(nh, currentRouter.getName()));
                    return new ForwardingOutcome(false, hops, "Next-hop router not found");
                }
                currentRouter = neighborRouter;
            } else {
                logger.finer("Return route verification failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
                return new ForwardingOutcome(false, hops, "Invalid route");
            }
        }
        logger.finer("Return route verification failure: maximum hops exceeded while forwarding from router %s".formatted(startRouter.getName()));
        return new ForwardingOutcome(false, hops, "TTL expired");
    }

    private Router findRouterOwningInterface(NetworkTopology topology, RouterInterface iface) {
        for (Router r : topology.getRouters()) {
            for (RouterInterface ri : r.getInterfaces()) {
                if (ri.equals(iface)) return r;
            }
        }
        return null;
    }

    private boolean belongsToSubnet(IPAddress ip, Subnet subnet) {
        // Convert IP to long
        long ipAsLong = ((long) ip.getOctet1() << 24) | ((long) ip.getOctet2() << 16) | ((long) ip.getOctet3() << 8) | ip.getOctet4();
        SubnetMask mask = subnet.getSubnetMask();
        int prefix = mask.getShortMask();
        long networkMask = (prefix == 0) ? 0 : (0xFFFFFFFFL << (32 - prefix));
        long net = ((long) subnet.getNetworkAddress().getOctet1() << 24) | ((long) subnet.getNetworkAddress().getOctet2() << 16) | ((long) subnet.getNetworkAddress().getOctet3() << 8) | subnet.getNetworkAddress().getOctet4();
        return (ipAsLong & networkMask) == (net & networkMask);
    }

    private RouterInterface findInterfaceByIp(NetworkTopology topology, IPAddress ip) {
        for (Router r : topology.getRouters()) {
            for (RouterInterface ri : r.getInterfaces()) {
                if (ri.getInterfaceAddress() != null && ri.getInterfaceAddress().getIpAddress().equals(ip)) {
                    return ri;
                }
            }
        }
        return null;
    }
}
