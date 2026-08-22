package org.uj.routingemulator.common.forwarding;

import org.uj.routingemulator.common.topology.Connection;
import org.uj.routingemulator.common.topology.NetworkInterface;
import org.uj.routingemulator.common.topology.TopologyQuery;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;

import java.util.logging.Logger;

public class GatewayResolver {
	private static final Logger logger = Logger.getLogger(GatewayResolver.class.getName());

	public GatewayResolution resolveHostGateway(Host srcHost, TopologyQuery topologyQuery) {
		if (srcHost.getHostInterface() == null || srcHost.getHostInterface().getDefaultGateway() == null) {
			logger.fine("Forwarding failure: no default gateway configured for host %s".formatted(srcHost.getHostname()));
			return GatewayResolution.failed(new ForwardingOutcome(false, 0, ForwardingReason.NO_DEFAULT_GATEWAY));
		}
		logger.finer("Looking for connection from host %s to its default gateway".formatted(srcHost.getHostname()));
		Connection conn = topologyQuery.getConnectionForInterface(srcHost.getHostInterface());
		if (conn == null) {
			logger.fine("Forwarding failure: host %s is not connected to any router".formatted(srcHost.getHostname()));
			return GatewayResolution.failed(new ForwardingOutcome(false, 0, ForwardingReason.HOST_NOT_CONNECTED));
		}
		NetworkInterface neighbor = conn.getNeighborInterface(srcHost.getHostInterface());
		if (!(neighbor instanceof RouterInterface currentInterface)) {
			logger.fine("Forwarding failure: default gateway for host %s is not a router interface".formatted(srcHost.getHostname()));
			return GatewayResolution.failed(new ForwardingOutcome(false, 0, ForwardingReason.DEFAULT_GATEWAY_NOT_ROUTER));
		}
		Router currentRouter = topologyQuery.findRouterOwningInterface(currentInterface);
		if (currentRouter == null) {
			logger.fine("Forwarding failure: cannot find router for gateway interface of host %s".formatted(srcHost.getHostname()));
			return GatewayResolution.failed(new ForwardingOutcome(false, 0, ForwardingReason.CANNOT_FIND_ROUTER_FOR_GATEWAY));
		}
		logger.finer("Default gateway reached. Starting hop-by-hop forwarding from router %s".formatted(currentRouter.getName()));
		return GatewayResolution.of(currentRouter);
	}

	public record GatewayResolution(Router router, ForwardingOutcome failure) {
		public static GatewayResolution of(Router router) {
			return new GatewayResolution(router, null);
		}

		public static GatewayResolution failed(ForwardingOutcome outcome) {
			return new GatewayResolution(null, outcome);
		}
	}
}