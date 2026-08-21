package org.uj.routingemulator.common;

import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;

import java.util.logging.Logger;

public class DestinationResolver {
	private static final Logger logger = Logger.getLogger(DestinationResolver.class.getName());
	private final ReturnPathVerifier returnPathVerifier;

	public DestinationResolver(ReturnPathVerifier returnPathVerifier) {
		this.returnPathVerifier = returnPathVerifier;
	}

	public ForwardingOutcome resolveDirectSubnet(Router currentRouter, RouterInterface dstIf, Packet packet,
	                                             TopologyQuery topologyQuery, int hopsBeforeThisHop,
	                                             ForwardingContext ctx) {
		if (dstIf.isDisabled()) {
			logger.fine("Forwarding failure: exit interface %s on router %s is administratively down".formatted(dstIf.getInterfaceName(), currentRouter.getName()));
			return new ForwardingOutcome(false, hopsBeforeThisHop + (ctx.isReturnVerification() ? 0 : 1), ForwardingReason.INTERFACE_ADMIN_DOWN);
		}

		int hops = ctx.isReturnVerification() ? hopsBeforeThisHop : hopsBeforeThisHop + 1;

		if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().ipAddress().equals(ctx.destination())) {
			return resolveOwnInterfaceReached(currentRouter, dstIf, topologyQuery, hops, ctx);
		}

		NetworkInterface foundHost = topologyQuery.findHostInterfaceByIpConnectedToInterface(dstIf, ctx.destination());
		if (foundHost instanceof HostInterface hi) {
			return resolveHostOnSubnetReached(currentRouter, dstIf, hi, topologyQuery, hops, ctx);
		}

		RouterInterface neighborRouterIf = topologyQuery.findInterfaceByIp(ctx.destination());
		if (neighborRouterIf != null && topologyQuery.isDirectlyConnectedNeighbor(dstIf, neighborRouterIf)) {
			return resolveNeighborInterfaceReached(neighborRouterIf, topologyQuery, hops, ctx);
		}

		logger.fine("Forwarding failure: no host with IP %s found on subnet connected to router %s interface %s"
				.formatted(ctx.destination(), currentRouter.getName(), dstIf.getInterfaceName()));
		return new ForwardingOutcome(false, hops, ForwardingReason.HOST_NOT_FOUND_ON_SUBNET);
	}

	public ForwardingOutcome resolveReturnRouteDirectSubnet(Router currentRouter, RouterInterface dstIf, IPAddress dstIp,
	                                                        TopologyQuery topologyQuery, int hops) {
		if (dstIf.getInterfaceAddress() != null && dstIf.getInterfaceAddress().ipAddress().equals(dstIp)) {
			logger.finer("Return route verification success: destination IP %s matches router %s interface %s"
					.formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED);
		}
		NetworkInterface foundHost = topologyQuery.findHostInterfaceByIpConnectedToInterface(dstIf, dstIp);
		if (foundHost instanceof HostInterface) {
			logger.finer("Return route verification success: destination IP %s matches host reachable from router %s interface %s"
					.formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.RETURN_REACHED_HOST);
		}
		RouterInterface neighborRouterIf = topologyQuery.findInterfaceByIp(dstIp);
		if (neighborRouterIf != null && topologyQuery.isDirectlyConnectedNeighbor(dstIf, neighborRouterIf)) {
			logger.finer("Return route verification success: destination IP %s matches neighbor router interface %s on router %s"
					.formatted(dstIp, neighborRouterIf.getInterfaceName(), currentRouter.getName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED);
		}
		logger.finer("Return route verification failure: no host with IP %s found on subnet connected to router %s interface %s"
				.formatted(dstIp, currentRouter.getName(), dstIf.getInterfaceName()));
		return new ForwardingOutcome(false, hops, ForwardingReason.HOST_NOT_FOUND_ON_SUBNET);
	}

	private ForwardingOutcome resolveOwnInterfaceReached(Router currentRouter, RouterInterface dstIf,
	                                                     TopologyQuery topologyQuery, int hops,
	                                                     ForwardingContext ctx) {
		if (ctx.isReturnVerification()) {
			logger.finer("Return route verification success: destination IP %s matches router %s interface %s"
					.formatted(ctx.destination(), currentRouter.getName(), dstIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED);
		}

		if (!ctx.verifyReturn()) {
			logger.fine("Forwarding success: reached destination router %s interface %s".formatted(currentRouter.getName(), dstIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_INTERFACE_REACHED);
		}

		Router dstRouter = topologyQuery.findRouterOwningInterface(dstIf);
		if (dstRouter != null) {
			if (returnPathVerifier.verifyReturnRouteFromRouter(dstRouter, dstIf, ctx.source(), topologyQuery)) {
				logger.fine("Forwarding failure: no return route from destination router %s to source IP".formatted(dstRouter.getName()));
				return new ForwardingOutcome(false, hops, ForwardingReason.NO_RETURN_ROUTE);
			}
			logger.fine("Forwarding success: reached destination router %s interface %s".formatted(dstRouter.getName(), dstIf.getInterfaceName()));
		}
		return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_INTERFACE_REACHED);
	}

	private ForwardingOutcome resolveHostOnSubnetReached(Router currentRouter, RouterInterface dstIf,
	                                                     HostInterface foundHost, TopologyQuery topologyQuery,
	                                                     int hops, ForwardingContext ctx) {
		if (ctx.isReturnVerification()) {
			logger.finer("Return route verification success: destination IP %s matches host reachable from router %s interface %s"
					.formatted(ctx.destination(), currentRouter.getName(), dstIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.RETURN_REACHED_HOST);
		}

		if (ctx.verifyReturn() && !returnPathVerifier.verifyReturnRouteFromHost(foundHost, ctx.source(), topologyQuery)) {
			logger.fine("Forwarding failure: no return route from destination host  to source IP");
			return new ForwardingOutcome(false, hops, ForwardingReason.NO_RETURN_ROUTE);
		}

		logger.fine("Forwarding success: reached destination host via router %s interface %s"
				.formatted(currentRouter.getName(), dstIf.getInterfaceName()));
		return new ForwardingOutcome(true, hops, ForwardingReason.REACHED_HOST);
	}

	private ForwardingOutcome resolveNeighborInterfaceReached(RouterInterface neighborRouterIf,
	                                                          TopologyQuery topologyQuery, int hops,
	                                                          ForwardingContext ctx) {
		if (ctx.isReturnVerification()) {
			logger.finer("Return route verification success: destination IP %s matches neighbor router interface %s"
					.formatted(ctx.destination(), neighborRouterIf.getInterfaceName()));
			return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED);
		}

		Router dstRouter = topologyQuery.findRouterOwningInterface(neighborRouterIf);
		if (dstRouter != null) {
			// Re-enabling unconditional return route check for neighbor interfaces as in the original implementation
			if (returnPathVerifier.verifyReturnRouteFromRouter(dstRouter, neighborRouterIf, ctx.source(), topologyQuery)) {
				logger.fine("Forwarding failure: no return route from destination router %s to source IP".formatted(dstRouter.getName()));
				return new ForwardingOutcome(false, hops, ForwardingReason.NO_RETURN_ROUTE);
			}
			logger.fine("Forwarding success: reached destination router %s interface %s".formatted(dstRouter.getName(), neighborRouterIf.getInterfaceName()));
		}
		return new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_INTERFACE_REACHED);
	}
}