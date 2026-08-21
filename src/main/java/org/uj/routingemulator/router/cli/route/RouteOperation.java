package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.RouterConfigurationSession;
import org.uj.routingemulator.router.StaticRoutingEntry;

public interface RouteOperation {
	void apply(RouterConfigurationSession session, StaticRoutingEntry entry);

	String getSuffix();
}