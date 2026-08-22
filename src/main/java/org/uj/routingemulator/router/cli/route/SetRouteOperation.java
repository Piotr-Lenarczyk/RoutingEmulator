package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.StaticRoutingEntry;
import org.uj.routingemulator.router.session.RouterConfigurationService;

public class SetRouteOperation implements RouteOperation {
	private final RouterConfigurationService service = new RouterConfigurationService();

	@Override
	public void apply(Router router, StaticRoutingEntry entry) {
		service.addRoute(router, entry);
	}

	@Override
	public String getSuffix() {
		return "";
	}
}