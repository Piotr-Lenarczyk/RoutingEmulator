package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.Map;

public class NextHopTargetStrategy implements RouteTargetStrategy {
	private final boolean hasDistance;

	public NextHopTargetStrategy(boolean hasDistance) {
		this.hasDistance = hasDistance;
	}

	@Override
	public StaticRoutingEntry createEntry(Router router, String destinationSubnet, Map<String, String> args, int distance) {
		org.uj.routingemulator.router.NextHopRouteParameters params = org.uj.routingemulator.router.NextHopRouteParameters.parseRouteParameters(destinationSubnet, args.get("next-hop"));
		if (hasDistance) {
			return new StaticRoutingEntry(params.dest(), params.nh(), distance);
		} else {
			return new StaticRoutingEntry(params.dest(), params.nh());
		}
	}

	@Override
	public String formatErrorPath(RouteOperation operation, String destinationSubnet, Map<String, String> args, int distance) {
		StringBuilder sb = new StringBuilder();
		sb.append("protocols static route ").append(destinationSubnet).append(" next-hop ").append(args.get("next-hop"));
		if (hasDistance) {
			sb.append(" distance ").append(distance);
		}
		sb.append(operation.getSuffix());
		return sb.toString();
	}
}