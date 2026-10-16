package org.uj.routingemulator.common;

import org.uj.routingemulator.router.*;

/**
 * Shared operational eligibility checks for connected and static routes.
 */
public final class RouteEligibility {
	private RouteEligibility() {
	}

	public static boolean isRouteActive(Router router, StaticRoutingEntry route, NetworkTopology topology) {
		if (route == null || route.isDisabled()) {
			return false;
		}
		if (route.getRouterInterface() != null) {
			return isInterfaceActive(router, route.getRouterInterface(), topology);
		}
		return route.getNextHop() != null && isNextHopActive(router, route.getNextHop(), topology);
	}

	public static boolean isInterfaceActive(Router router, RouterInterface iface, NetworkTopology topology) {
		RouterInterface configuredInterface = findConfiguredInterface(router, iface);
		if (!isAdministrativelyUp(configuredInterface)) {
			return false;
		}
		if (configuredInterface.getType() == InterfaceType.DUMMY
				|| configuredInterface.getType() == InterfaceType.LOOPBACK) {
			return true;
		}

		RouterInterface physicalInterface = getPhysicalInterface(router, configuredInterface);
		if (!isAdministrativelyUp(physicalInterface)
				|| configuredInterface.getStatus().getLink() != LinkState.UP
				|| physicalInterface.getStatus().getLink() != LinkState.UP) {
			return false;
		}
		return topology == null || topology.hasActiveConnection(physicalInterface);
	}

	private static boolean isNextHopActive(Router router, IPAddress nextHop, NetworkTopology topology) {
		for (RouterInterface localInterface : router.getInterfaces()) {
			if (localInterface.getSubnet() == null || !localInterface.getSubnet().contains(nextHop)
					|| !isInterfaceActive(router, localInterface, topology)) {
				continue;
			}
			if (topology == null) {
				return true;
			}

			RouterInterface physicalInterface = getPhysicalInterface(router, localInterface);
			RouterInterface neighborInterface = topology.findRouterInterfaceByIpConnectedToInterface(
					physicalInterface, nextHop);
			Router neighbor = findOwningRouter(topology, neighborInterface);
			if (neighbor == null || neighbor == router
					|| !isInterfaceActive(neighbor, neighborInterface, topology)) {
				continue;
			}
			RouterInterface neighborPhysicalInterface = getPhysicalInterface(neighbor, neighborInterface);
			if (topology.areInterfacesL2Connected(physicalInterface, neighborPhysicalInterface)) {
				return true;
			}
		}
		return false;
	}

	private static RouterInterface findConfiguredInterface(Router router, RouterInterface iface) {
		if (router == null || iface == null) {
			return null;
		}
		return router.getInterfaces().stream()
				.filter(candidate -> candidate == iface
						|| candidate.getInterfaceName().equals(iface.getInterfaceName()))
				.findFirst()
				.orElse(null);
	}

	private static RouterInterface getPhysicalInterface(Router router, RouterInterface iface) {
		if (iface.getType() != InterfaceType.VIF) {
			return iface;
		}
		return router.getInterfaces().stream()
				.filter(candidate -> candidate.getInterfaceName().equals(iface.getParentName()))
				.findFirst()
				.orElse(iface);
	}

	private static Router findOwningRouter(NetworkTopology topology, RouterInterface iface) {
		if (iface == null) {
			return null;
		}
		return topology.getRouters().stream()
				.filter(router -> router.getInterfaces().stream().anyMatch(candidate -> candidate == iface))
				.findFirst()
				.orElse(null);
	}

	private static boolean isAdministrativelyUp(RouterInterface iface) {
		return iface != null && iface.getStatus() != null
				&& iface.getStatus().getAdmin() == AdminState.UP;
	}
}