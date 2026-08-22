package org.uj.routingemulator.common.forwarding;

import org.uj.routingemulator.common.packet.Packet;
import org.uj.routingemulator.common.topology.TopologyQuery;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;

import java.util.Optional;
import java.util.logging.Logger;

public class PacketForwarder {
	private static final Logger logger = Logger.getLogger(PacketForwarder.class.getName());
	private static final int DEFAULT_TTL = 64;

	private final DestinationResolver destinationResolver;
	private final RouteResolver routeResolver;

	public PacketForwarder(DestinationResolver destinationResolver, RouteResolver routeResolver) {
		this.destinationResolver = destinationResolver;
		this.routeResolver = routeResolver;
	}

	public void normalizeTtl(Packet packet) {
		if (packet.getTtl() <= 0) {
			packet.setTtl(DEFAULT_TTL);
		}
	}

	public boolean isDestinationOnHostSubnet(Packet packet, Host srcHost) {
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

	public ForwardingOutcome traverse(Packet packet, Router startRouter, int startHops,
	                                  TopologyQuery topologyQuery, ForwardingContext ctx) {
		Router currentRouter = startRouter;
		int hops = startHops;

		while (hops < ctx.maxHops()) {
			if (ctx.decrementTtl() && packet != null) {
				if (packet.decrementTTL()) {
					logger.fine("Forwarding failure: TTL expired while forwarding from router %s".formatted(currentRouter.getName()));
					return new ForwardingOutcome(false, hops, ForwardingReason.TTL_EXPIRED);
				}
			}
			if (ctx.isReturnVerification()) {
				hops++;
			}

			logger.finer("Checking interfaces of router %s for destination %s".formatted(currentRouter.getName(), ctx.destination()));
			Optional<RouterInterface> intfToDst = RouteSelector.findDirectSubnetInterface(currentRouter, ctx.destination());
			if (intfToDst.isPresent()) {
				return destinationResolver.resolveDirectSubnet(currentRouter, intfToDst.get(), packet, topologyQuery, hops, ctx);
			}

			logger.finer("No directly connected subnet matches destination. Looking for static routes on router %s".formatted(currentRouter.getName()));
			RouteResolver.RouteStep step = routeResolver.resolveNextRouterViaStaticRoute(currentRouter, topologyQuery, hops, ctx);
			if (step.outcome() != null) {
				return step.outcome();
			}
			currentRouter = step.nextRouter();

			if (!ctx.isReturnVerification()) {
				hops = step.hops();
			}
		}

		logger.finer("Return route verification failure: maximum hops exceeded while forwarding from router %s".formatted(startRouter.getName()));
		return new ForwardingOutcome(false, hops, ForwardingReason.TTL_EXPIRED);
	}
}