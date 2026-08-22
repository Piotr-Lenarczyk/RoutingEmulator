package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.common.addressing.Subnet;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;
import org.uj.routingemulator.router.model.StaticRoutingEntry;

import java.util.Map;

public class InterfaceTargetStrategy implements RouteTargetStrategy {
	private final boolean hasDistance;

	public InterfaceTargetStrategy(boolean hasDistance) {
		this.hasDistance = hasDistance;
	}

	@Override
	public StaticRoutingEntry createEntry(Router router, String destinationSubnet, Map<String, String> args, int distance) {
		Subnet dest = Subnet.fromString(destinationSubnet);
		RouterInterface iface = router.findFromName(args.get("interface"));
		if (hasDistance) {
			return new StaticRoutingEntry(dest, iface, distance);
		} else {
			return new StaticRoutingEntry(dest, iface);
		}
	}

	@Override
	public String formatErrorPath(RouteOperation operation, String destinationSubnet, Map<String, String> args, int distance) {
		StringBuilder sb = new StringBuilder();
		sb.append("protocols static route ").append(destinationSubnet).append(" interface ").append(args.get("interface"));
		if (hasDistance) {
			sb.append(" distance ").append(distance);
		}
		sb.append(operation.getSuffix());
		return sb.toString();
	}
}