package org.uj.routingemulator.common.forwarding;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;
import org.uj.routingemulator.router.model.StaticRoutingEntry;

import java.util.Optional;

/**
 * Resolves exit interfaces and routing decisions, shared by ForwardingEngine and PingService.
 */
public class RouteSelector {

	private RouteSelector() {
		// Prevent instantiation
	}

	public static Optional<RouterInterface> findDirectSubnetInterface(Router router, IPAddress destination) {
		return router.getInterfaces().stream()
				.filter(iface -> iface.getSubnet() != null && iface.getSubnet().contains(destination))
				.findFirst();
	}

	public static Optional<StaticRoutingEntry> findStaticRoute(Router router, IPAddress destination) {
		return router.getRoutingTable().getRoutingEntries().stream()
				.filter(e -> !e.isDisabled() && e.getSubnet() != null && e.getSubnet().contains(destination))
				.findFirst();
	}

	public static RouterInterface determineExitInterface(Router router, IPAddress destination) {
		Optional<RouterInterface> direct = findDirectSubnetInterface(router, destination);
		if (direct.isPresent()) {
			return direct.get();
		}

		Optional<StaticRoutingEntry> routeOpt = findStaticRoute(router, destination);
		if (routeOpt.isPresent()) {
			StaticRoutingEntry route = routeOpt.get();
			if (route.getRouterInterface() != null) {
				return route.getRouterInterface();
			} else if (route.getNextHop() != null) {
				return findDirectSubnetInterface(router, route.getNextHop()).orElse(null);
			}
		}
		return null;
	}

	public static IPAddress determineSourceIp(RouterInterface ri) {
		if (ri != null && ri.getInterfaceAddress() != null) {
			return ri.getInterfaceAddress().ipAddress();
		}
		return null;
	}
}