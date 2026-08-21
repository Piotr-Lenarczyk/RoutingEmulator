package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;

public interface RouteOperation {
	void apply(Router router, StaticRoutingEntry entry);
	String getSuffix();
}