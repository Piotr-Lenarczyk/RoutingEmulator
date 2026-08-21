package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.Map;

public interface RouteTargetStrategy {
	StaticRoutingEntry createEntry(Router router, String destinationSubnet, Map<String, String> args, int distance);

	String formatErrorPath(RouteOperation operation, String destinationSubnet, Map<String, String> args, int distance);
}