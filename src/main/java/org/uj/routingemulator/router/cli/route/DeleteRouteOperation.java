package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterConfigurationService;
import org.uj.routingemulator.router.StaticRoutingEntry;

public class DeleteRouteOperation implements RouteOperation {
	private final RouterConfigurationService service = new RouterConfigurationService();

	@Override
	public void apply(Router router, StaticRoutingEntry entry) {
		service.removeRoute(router, entry);
	}

	@Override
	public String getSuffix() {
		return "";
	}
}