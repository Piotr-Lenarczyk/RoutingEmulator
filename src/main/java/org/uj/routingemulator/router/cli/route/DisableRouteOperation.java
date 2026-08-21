package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterConfigurationService;
import org.uj.routingemulator.router.StaticRoutingEntry;

public class DisableRouteOperation implements RouteOperation {
	private final RouterConfigurationService service = new RouterConfigurationService();

	@Override
	public void apply(Router router, StaticRoutingEntry entry) {
		service.disableRoute(router, entry);
	}

	@Override
	public String getSuffix() {
		return " disable";
	}
}