package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to disable a static route with next-hop IP address and custom administrative distance.
 * Format: set protocols static route <destination> next-hop <next-hop> distance <distance> disable
 */
public class DisableRouteNextHopDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+distance\\s+(\\d+)\\s+disable"
	);
	private String destinationSubnet;
	private String nextHop;
	private int distance;

	@Override
	public void execute(Router router) {
		try {
			router.disableRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop),
							distance
					)
			);
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDisableRouteNextHopDistance(destinationSubnet, nextHop, distance));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			destinationSubnet = matcher.group(1);
			nextHop = matcher.group(2);
			distance = Integer.parseInt(matcher.group(3));
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "set protocols static route <destination> next-hop <next-hop> distance <distance> disable";
	}

	@Override
	public String getDescription() {
		return "Disable static route via next-hop with custom distance";
	}
}

