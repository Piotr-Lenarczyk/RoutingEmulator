package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to disable a static route with next-hop IP address.
 * Uses default administrative distance (1).
 * Format: set protocols static route <destination> next-hop <next-hop> disable
 */
public class DisableRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+disable"
	);
	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(Router router) {
		try {
			router.disableRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop)
					)
			);
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDisableRouteNextHop(destinationSubnet, nextHop));
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
		return "set protocols static route <destination> next-hop <next-hop> disable";
	}

	@Override
	public String getDescription() {
		return "Disable static route via next-hop with default distance";
	}
}
