package org.uj.routingemulator.common.forwarding;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.common.topology.Connection;
import org.uj.routingemulator.common.topology.NetworkInterface;
import org.uj.routingemulator.common.topology.TopologyQuery;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;
import org.uj.routingemulator.router.model.StaticRoutingEntry;

import java.util.Optional;
import java.util.logging.Logger;

public class RouteResolver {
	private static final Logger logger = Logger.getLogger(RouteResolver.class.getName());

	public RouteStep resolveNextRouterViaStaticRoute(Router currentRouter, TopologyQuery topologyQuery,
	                                                 int hopsBeforeThisHop, ForwardingContext ctx) {
		Optional<StaticRoutingEntry> routeOpt = RouteSelector.findStaticRoute(currentRouter, ctx.destination());
		if (routeOpt.isEmpty()) {
			logger.fine("Forwarding failure: no route to destination %s on router %s".formatted(ctx.destination(), currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(false, hopsBeforeThisHop, ForwardingReason.NO_ROUTE));
		}

		StaticRoutingEntry route = routeOpt.get();
		int hops = ctx.isReturnVerification() ? hopsBeforeThisHop : hopsBeforeThisHop + 1;

		if (route.getRouterInterface() != null) {
			return resolveInterfaceRoute(currentRouter, route.getRouterInterface(), topologyQuery, hops, ctx);
		}
		if (route.getNextHop() != null) {
			return resolveNextHopRoute(currentRouter, route.getNextHop(), topologyQuery, hops, ctx);
		}

		logger.fine("Forwarding failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
		return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.INVALID_ROUTE));
	}

	public ReturnRouteStep resolveReturnRouteViaStaticRoute(Router currentRouter, IPAddress dstIp,
	                                                        TopologyQuery topologyQuery, int hops) {
		Optional<StaticRoutingEntry> routeOpt = RouteSelector.findStaticRoute(currentRouter, dstIp);
		if (routeOpt.isEmpty()) {
			logger.finer("Return route verification failure: no route to destination IP %s on router %s".formatted(dstIp, currentRouter.getName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NO_ROUTE));
		}
		StaticRoutingEntry route = routeOpt.get();
		if (route.getRouterInterface() != null) {
			return resolveReturnRouteInterfaceRoute(currentRouter, route.getRouterInterface(), dstIp, topologyQuery, hops);
		}
		if (route.getNextHop() != null) {
			return resolveReturnRouteNextHop(currentRouter, route.getNextHop(), topologyQuery, hops);
		}
		logger.finer("Return route verification failure: invalid route on router %s (no next-hop or exit interface)".formatted(currentRouter.getName()));
		return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.INVALID_ROUTE));
	}

	private RouteStep resolveInterfaceRoute(Router currentRouter, RouterInterface exitIf,
	                                        TopologyQuery topologyQuery, int hops, ForwardingContext ctx) {
		if (exitIf.isDisabled()) {
			logger.fine("Forwarding failure: exit interface %s on router %s is administratively down".formatted(exitIf.getInterfaceName(), currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.INTERFACE_ADMIN_DOWN));
		}

		Connection exitConn = topologyQuery.getConnectionForInterface(exitIf);
		if (exitConn == null) {
			logger.fine("Forwarding failure: exit interface %s on router %s is not connected to any other interface"
					.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.INTERFACE_NOT_CONNECTED));
		}

		NetworkInterface foundHost = topologyQuery.findHostInterfaceByIpConnectedToInterface(exitIf, ctx.destination());
		if (foundHost instanceof HostInterface) {
			if (ctx.isReturnVerification()) {
				logger.fine("Return route verification success: destination IP %s matches host reachable from router %s exit interface %s"
						.formatted(ctx.destination(), currentRouter.getName(), exitIf.getInterfaceName()));
				return RouteStep.terminal(new ForwardingOutcome(true, hops, ForwardingReason.RETURN_REACHED_HOST));
			}
			logger.fine("Forwarding success: reached destination host via exit interface %s on router %s"
					.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(true, hops, ForwardingReason.REACHED_HOST));
		}

		if (ctx.isReturnVerification()) {
			RouterInterface neighborIf = topologyQuery.findInterfaceByIp(ctx.destination());
			if (neighborIf != null && topologyQuery.isDirectlyConnectedNeighbor(exitIf, neighborIf)) {
				logger.fine("Return route verification success: destination IP %s matches neighbor router interface %s on router %s"
						.formatted(ctx.destination(), neighborIf.getInterfaceName(), currentRouter.getName()));
				return RouteStep.terminal(new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED));
			}
		}

		NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);
		if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
			Router neighborRouter = topologyQuery.findRouterOwningInterface(neighborRouterIf);
			if (neighborRouter == null) {
				logger.fine("Forwarding failure: neighbor router for exit interface %s on router %s not found"
						.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
				return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEIGHBOR_ROUTER_NOT_FOUND));
			}
			return RouteStep.advance(neighborRouter, hops);
		}

		logger.fine("Forwarding failure: unsupported neighbor type connected to exit interface %s on router %s"
				.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
		return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.UNSUPPORTED_NEIGHBOR_TYPE));
	}

	private RouteStep resolveNextHopRoute(Router currentRouter, IPAddress nextHop,
	                                      TopologyQuery topologyQuery, int hops, ForwardingContext ctx) {
		RouterInterface foundIf = topologyQuery.findInterfaceByIp(nextHop);
		if (foundIf == null) {
			logger.fine("Forwarding failure: next-hop IP %s for route on router %s not found in topology"
					.formatted(nextHop, currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEXT_HOP_NOT_IN_TOPOLOGY));
		}

		Router neighborRouter = topologyQuery.findRouterOwningInterface(foundIf);
		if (neighborRouter == null) {
			logger.fine("Forwarding failure: next-hop router for IP %s on router %s not found".formatted(nextHop, currentRouter.getName()));
			return RouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEXT_HOP_NOT_FOUND));
		}
		return RouteStep.advance(neighborRouter, hops);
	}

	private ReturnRouteStep resolveReturnRouteInterfaceRoute(Router currentRouter, RouterInterface exitIf, IPAddress dstIp,
	                                                         TopologyQuery topologyQuery, int hops) {
		Connection exitConn = topologyQuery.getConnectionForInterface(exitIf);
		if (exitConn == null) {
			logger.finer("Return route verification failure: exit interface %s on router %s is not connected to any other interface"
					.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.INTERFACE_NOT_CONNECTED));
		}
		NetworkInterface foundHost = topologyQuery.findHostInterfaceByIpConnectedToInterface(exitIf, dstIp);
		if (foundHost instanceof HostInterface) {
			logger.finer("Return route verification success: destination IP %s matches host reachable from router %s exit interface %s"
					.formatted(dstIp, currentRouter.getName(), exitIf.getInterfaceName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(true, hops, ForwardingReason.RETURN_REACHED_HOST));
		}
		RouterInterface neighborIf = topologyQuery.findInterfaceByIp(dstIp);
		if (neighborIf != null && topologyQuery.isDirectlyConnectedNeighbor(exitIf, neighborIf)) {
			logger.finer("Return route verification success: destination IP %s matches neighbor router interface %s on router %s"
					.formatted(dstIp, neighborIf.getInterfaceName(), currentRouter.getName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(true, hops, ForwardingReason.ROUTER_RETURN_REACHED));
		}
		NetworkInterface nextNeighbor = exitConn.getNeighborInterface(exitIf);
		if (nextNeighbor instanceof RouterInterface neighborRouterIf) {
			Router neighborRouter = topologyQuery.findRouterOwningInterface(neighborRouterIf);
			if (neighborRouter == null) {
				logger.finer("Return route verification failure: neighbor router for exit interface %s on router %s not found"
						.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
				return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEIGHBOR_ROUTER_NOT_FOUND));
			}
			return ReturnRouteStep.advance(neighborRouter);
		}
		logger.finer("Return route verification failure: unsupported neighbor type connected to exit interface %s on router %s"
				.formatted(exitIf.getInterfaceName(), currentRouter.getName()));
		return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.UNSUPPORTED_NEIGHBOR_TYPE));
	}

	private ReturnRouteStep resolveReturnRouteNextHop(Router currentRouter, IPAddress nextHop,
	                                                  TopologyQuery topologyQuery, int hops) {
		RouterInterface foundIf = topologyQuery.findInterfaceByIp(nextHop);
		if (foundIf == null) {
			logger.finer("Return route verification failure: next-hop IP %s for route on router %s not found in topology"
					.formatted(nextHop, currentRouter.getName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEXT_HOP_NOT_IN_TOPOLOGY));
		}
		Router neighborRouter = topologyQuery.findRouterOwningInterface(foundIf);
		if (neighborRouter == null) {
			logger.finer("Return route verification failure: next-hop router for IP %s on router %s not found".formatted(nextHop, currentRouter.getName()));
			return ReturnRouteStep.terminal(new ForwardingOutcome(false, hops, ForwardingReason.NEXT_HOP_NOT_FOUND));
		}
		return ReturnRouteStep.advance(neighborRouter);
	}

	public record RouteStep(Router nextRouter, int hops, ForwardingOutcome outcome) {
		public static RouteStep advance(Router router, int hops) {
			return new RouteStep(router, hops, null);
		}

		public static RouteStep terminal(ForwardingOutcome outcome) {
			return new RouteStep(null, 0, outcome);
		}
	}

	public record ReturnRouteStep(Router nextRouter, ForwardingOutcome outcome) {
		public static ReturnRouteStep advance(Router router) {
			return new ReturnRouteStep(router, null);
		}

		public static ReturnRouteStep terminal(ForwardingOutcome outcome) {
			return new ReturnRouteStep(null, outcome);
		}
	}
}