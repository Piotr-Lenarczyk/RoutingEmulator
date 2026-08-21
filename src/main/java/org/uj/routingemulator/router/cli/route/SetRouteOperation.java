package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.RouterConfigurationSession;
import org.uj.routingemulator.router.StaticRoutingEntry;

public class SetRouteOperation implements RouteOperation {
	@Override
	public void apply(RouterConfigurationSession session, StaticRoutingEntry entry) {
		session.addRoute(entry);
	}

	@Override
	public String getSuffix() {
		return "";
	}
}