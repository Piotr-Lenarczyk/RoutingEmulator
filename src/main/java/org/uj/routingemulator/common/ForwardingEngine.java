package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;

import java.util.logging.Logger;

/**
 * Minimal forwarding engine: simulates forwarding of a packet through routers using static routing table and connections in NetworkTopology.
 * This is intentionally simple and deterministic for testing purposes.
 */
public class ForwardingEngine {
    private static final Logger logger = Logger.getLogger(ForwardingEngine.class.getName());

    private final GatewayResolver gatewayResolver;
    private final PacketForwarder packetForwarder;

    public ForwardingEngine() {
        this.gatewayResolver = new GatewayResolver();
        ReturnPathVerifier returnPathVerifier = new ReturnPathVerifier();
        DestinationResolver destinationResolver = new DestinationResolver(returnPathVerifier);
        RouteResolver routeResolver = new RouteResolver();
        this.packetForwarder = new PacketForwarder(destinationResolver, routeResolver);

        // Resolve circular dependency safely
        returnPathVerifier.setResolvers(destinationResolver, routeResolver);
    }

    public ForwardingOutcome forward(Packet packet, Host srcHost, NetworkTopology topology) {
        TopologyQuery topologyQuery = new NetworkTopologyQuery(topology);
        logger.fine("Starting forwarding of packet from %s to %s".formatted(packet.getSource(), packet.getDestination()));
        packetForwarder.normalizeTtl(packet);
        if (packetForwarder.isDestinationOnHostSubnet(packet, srcHost)) {
            return new ForwardingOutcome(true, 1, ForwardingReason.REACHED_SAME_SUBNET);
        }

        GatewayResolver.GatewayResolution gateway = gatewayResolver.resolveHostGateway(srcHost, topologyQuery);
        if (gateway.failure() != null) {
            return gateway.failure();
        }

        ForwardingContext ctx = new ForwardingContext(packet.getSource(), packet.getDestination(), 128, true, true, false);
        return packetForwarder.traverse(packet, gateway.router(), 1, topologyQuery, ctx);
    }

    public ForwardingOutcome forward(Packet packet, Router srcRouter, NetworkTopology topology) {
        TopologyQuery topologyQuery = new NetworkTopologyQuery(topology);
        logger.fine("Starting forwarding (router source) of packet from %s to %s".formatted(packet.getSource(), packet.getDestination()));
        packetForwarder.normalizeTtl(packet);
        ForwardingContext ctx = new ForwardingContext(packet.getSource(), packet.getDestination(), 128, true, false, false);
        return packetForwarder.traverse(packet, srcRouter, 0, topologyQuery, ctx);
    }
}