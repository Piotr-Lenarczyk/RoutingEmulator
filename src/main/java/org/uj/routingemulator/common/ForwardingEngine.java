package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.Optional;

/**
 * Minimal forwarding engine: simulates forwarding of a packet through routers using static routing table and connections in NetworkTopology.
 * This is intentionally simple and deterministic for testing purposes.
 */
public class ForwardingEngine {
    private static final int DEFAULT_TTL = 64;

    /**
     * Forwards the packet starting from the source host towards destination IP using topology and routers.
     * Returns ForwardingOutcome with reached=true if destination is reachable.
     */
    public ForwardingOutcome forward(Packet packet, Host srcHost, NetworkTopology topology) {
        packet.setTtl(DEFAULT_TTL);

        // If destination is in the same subnet as source host, deliver immediately (1 hop)
        if (srcHost.getHostInterface() != null && srcHost.getHostInterface().getSubnet() != null) {
            Subnet hostSubnet = srcHost.getHostInterface().getSubnet();
            // Check if destination IP belongs to this subnet
            if (belongsToSubnet(packet.getDestination(), hostSubnet)) {
                return new ForwardingOutcome(true, 1, "Reached (same subnet)");
            }
        }

        // Otherwise, send to default gateway
        if (srcHost.getHostInterface() == null || srcHost.getHostInterface().getDefaultGateway() == null) {
            return new ForwardingOutcome(false, 0, "No default gateway configured");
        }

        IPAddress nextHopIp = srcHost.getHostInterface().getDefaultGateway();

        // Find the router interface that is connected to the host's interface (via Connection)
        Connection conn = topology.getConnectionForInterface(srcHost.getHostInterface());
        if (conn == null) {
            return new ForwardingOutcome(false, 0, "Host not connected to topology");
        }
        NetworkInterface neighbor = conn.getNeighborInterface(srcHost.getHostInterface());
        if (!(neighbor instanceof RouterInterface currentInterface)) {
            return new ForwardingOutcome(false, 0, "Default gateway is not a router interface");
        }

	    // Find the router owning this interface
        Router currentRouter = findRouterOwningInterface(topology, currentInterface);
        if (currentRouter == null) {
            return new ForwardingOutcome(false, 0, "Cannot find router for gateway interface");
        }

        int hops = 1; // host->first-router

        // Now perform hop-by-hop forwarding using router routing tables
        while (true) {
            if (!packet.decrementTTL()) {
                return new ForwardingOutcome(false, hops, "TTL expired");
            }

            // Check if any interface on currentRouter has subnet containing destination
            Optional<RouterInterface> intfToDst = currentRouter.getInterfaces().stream()
                    .filter(iface -> iface.getSubnet() != null && belongsToSubnet(packet.getDestination(), iface.getSubnet()))
                    .findFirst();
            if (intfToDst.isPresent()) {
                // Destination is on a directly connected subnet of currentRouter
                RouterInterface dstIf = intfToDst.get();

                // If the destination equals the router's own interface address -> reached
                if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().getIpAddress().equals(packet.getDestination())) {
                    hops++;
                    return new ForwardingOutcome(true, hops, "Reached (router interface)");
                }

                // Otherwise, try to discover a host with exact IP reachable from this router interface (through switches)
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(dstIf, packet.getDestination());
                if (foundHost != null) {
                    hops++;
                    return new ForwardingOutcome(true, hops, "Reached (host)");
                }

                // If there is no host with exact IP on this connected segment -> unreachable for our simplified model
                return new ForwardingOutcome(false, hops + 1, "Host not found on connected subnet");
            }

            // Otherwise, find best static route (first matching non-disabled route)
            Optional<StaticRoutingEntry> routeOpt = currentRouter.getRoutingTable().getRoutingEntries().stream()
                    .filter(e -> !e.isDisabled() && belongsToSubnet(packet.getDestination(), e.getSubnet()))
                    .findFirst();

            if (routeOpt.isEmpty()) {
                return new ForwardingOutcome(false, hops, "No route");
            }

            StaticRoutingEntry route = routeOpt.get();
            hops++;

            // Next hop via interface
            if (route.getRouterInterface() != null) {
                RouterInterface exitIf = route.getRouterInterface();
                // Find the connection where this interface participates
                Connection exitConn = topology.getConnectionForInterface(exitIf);
                if (exitConn == null) {
                    return new ForwardingOutcome(false, hops, "Exit interface not connected");
                }

                // First, check if there's any host reachable from this exit interface that has the exact IP
                HostInterface foundHost = topology.findHostInterfaceByIpConnectedToInterface(exitIf, packet.getDestination());
                if (foundHost != null) {
                    return new ForwardingOutcome(true, hops, "Reached host");
                }

                NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);
                // If neighbor is router interface -> move to that router
                if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
	                Router neighborRouter = findRouterOwningInterface(topology, neighborRouterIf);
                    if (neighborRouter == null) return new ForwardingOutcome(false, hops, "Neighbor router not found");
                    currentRouter = neighborRouter;
                    currentInterface = neighborRouterIf;
                    continue;
                }

                // Other neighbor types - assume unreachable
                return new ForwardingOutcome(false, hops, "Unsupported neighbor type");
            } else if (route.getNextHop() != null) {
                // Next-hop based route: find interface with this IP in topology
                IPAddress nh = route.getNextHop();
                // Search all router interfaces for an interface address exactly equal to nh
                RouterInterface foundIf = findInterfaceByIp(topology, nh);
                if (foundIf == null) {
                    return new ForwardingOutcome(false, hops, "Next-hop not found in topology");
                }
                // Move to the router owning foundIf
                Router neighborRouter = findRouterOwningInterface(topology, foundIf);
                if (neighborRouter == null) return new ForwardingOutcome(false, hops, "Next-hop router not found");
                currentRouter = neighborRouter;
                currentInterface = foundIf;
                continue;
            } else {
                return new ForwardingOutcome(false, hops, "Invalid route");
            }
        }
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

    private boolean hostsSubnetMatches(IPAddress ip, HostInterface hostIf) {
        Subnet subnet = hostIf.getSubnet();
        if (subnet == null) return false;
        return belongsToSubnet(ip, subnet);
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
