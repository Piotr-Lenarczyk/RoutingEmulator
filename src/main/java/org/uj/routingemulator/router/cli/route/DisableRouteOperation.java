package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.StaticRoutingEntry;
import org.uj.routingemulator.router.session.RouterConfigurationService;

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