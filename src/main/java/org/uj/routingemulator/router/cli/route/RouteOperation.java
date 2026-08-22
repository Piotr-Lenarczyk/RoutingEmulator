package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.StaticRoutingEntry;

public interface RouteOperation {
	void apply(Router router, StaticRoutingEntry entry);
	String getSuffix();
}