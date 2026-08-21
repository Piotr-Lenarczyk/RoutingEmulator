package org.uj.routingemulator.common;

import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;

import java.util.Optional;
import java.util.logging.Logger;

public class ReturnPathVerifier {
	private static final Logger logger = Logger.getLogger(ReturnPathVerifier.class.getName());
	private DestinationResolver destinationResolver;
	private RouteResolver routeResolver;

	public void setResolvers(DestinationResolver destinationResolver, RouteResolver routeResolver) {
		this.destinationResolver = destinationResolver;
		this.routeResolver = routeResolver;
	}

	public boolean verifyReturnRouteFromRouter(Router dstRouter, RouterInterface dstIf, IPAddress srcIp, TopologyQuery topologyQuery) {
		logger.finer("Verifying return route from destination router %s interface %s to source IP %s"
				.formatted(dstRouter.getName(), dstIf.getInterfaceName(), srcIp));
		ForwardingOutcome outcome = forwardFromRouter(dstRouter, dstIf, srcIp, topologyQuery);
		logger.finest("Return route verification result: %s".formatted(outcome.reached() ? "reachable" : "unreachable"));
		return !outcome.reached();
	}

	public boolean verifyReturnRouteFromHost(HostInterface dstHostIf, IPAddress srcIp, TopologyQuery topologyQuery) {
		logger.finest("Verifying return route from destination host interface %s to source IP %s"
				.formatted(dstHostIf.getInterfaceName(), srcIp));
		if (dstHostIf.getDefaultGateway() == null) {
			logger.finest("Return route verification failure: destination host interface has no default gateway configured");
			return false;
		}
		RouterInterface gatewayIf = topologyQuery.findInterfaceByIp(dstHostIf.getDefaultGateway());
		if (gatewayIf == null) {
			logger.finest("Return route verification failure: cannot find gateway interface for destination host's default gateway IP %s"
					.formatted(dstHostIf.getDefaultGateway()));
			return false;
		}
		Router gatewayRouter = topologyQuery.findRouterOwningInterface(gatewayIf);
		if (gatewayRouter == null) {
			logger.finest("Return route verification failure: cannot find router owning gateway interface %s".formatted(gatewayIf.getInterfaceName()));
			return false;
		}
		ForwardingOutcome outcome = forwardFromRouter(gatewayRouter, gatewayIf, srcIp, topologyQuery);
		logger.finest("Return route verification result: %s".formatted(outcome.reached() ? "reachable" : "unreachable"));
		return outcome.reached();
	}

	private ForwardingOutcome forwardFromRouter(Router startRouter, RouterInterface startIf, IPAddress dstIp, TopologyQuery topologyQuery) {
		logger.finer("Forwarding from router %s interface %s to destination IP %s"
				.formatted(startRouter.getName(), startIf.getInterfaceName(), dstIp));
		Router currentRouter = startRouter;
		int hops = 0;
		int maxHops = 128;
		while (hops < maxHops) {
			hops++;
			Optional<RouterInterface> intfToDst = RouteSelector.findDirectSubnetInterface(currentRouter, dstIp);
			if (intfToDst.isPresent()) {
				return destinationResolver.resolveReturnRouteDirectSubnet(currentRouter, intfToDst.get(), dstIp, topologyQuery, hops);
			}
			RouteResolver.ReturnRouteStep step = routeResolver.resolveReturnRouteViaStaticRoute(currentRouter, dstIp, topologyQuery, hops);
			if (step.outcome() != null) {
				return step.outcome();
			}
			currentRouter = step.nextRouter();
		}
		logger.finer("Return route verification failure: maximum hops exceeded while forwarding from router %s".formatted(startRouter.getName()));
		return new ForwardingOutcome(false, hops, ForwardingReason.TTL_EXPIRED);
	}
}