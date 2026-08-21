package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.RouterConfigurationSession;
import org.uj.routingemulator.router.StaticRoutingEntry;

public class DisableRouteOperation implements RouteOperation {
	@Override
	public void apply(RouterConfigurationSession session, StaticRoutingEntry entry) {
		session.disableRoute(entry);
	}

	@Override
	public String getSuffix() {
		return " disable";
	}
}