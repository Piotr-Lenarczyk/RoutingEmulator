package org.uj.routingemulator.router.model;

import org.uj.routingemulator.router.exceptions.InvalidModeException;

import java.util.logging.Logger;

public class RoutingTablePresenter {
	private static final Logger logger = Logger.getLogger(RoutingTablePresenter.class.getName());

	private RoutingTablePresenter() {
	}

	public static String showIpRoute(Router router) {
		if (router.getMode() != RouterMode.OPERATIONAL) {
			logger.warning("Attempted to show IP route while in %s mode".formatted(router.getMode()));
			throw new InvalidModeException("Invalid command: show [ip]");
		}
		return IpRouteTableFormatter.format(router);
	}
}