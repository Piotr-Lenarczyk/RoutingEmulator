package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to add a static route with next-hop IP address.
 * Uses default administrative distance (1).
 * Format: set protocols static route <destination> next-hop <next-hop>
 */
public class SetRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)"
	);
	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(Router router) {
		try {
			router.addRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop)
					)
			);
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
				CLIErrorHandler.formatRouteNextHop(destinationSubnet, nextHop));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			destinationSubnet = matcher.group(1);
			nextHop = matcher.group(2);
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "set protocols static route <destination> next-hop <next-hop>";
	}

	@Override
	public String getDescription() {
		return "Add static route via next-hop with default distance";
	}
}

