package org.example.thesisuj.router.cli;

import org.example.thesisuj.common.IPAddress;
import org.example.thesisuj.common.Subnet;
import org.example.thesisuj.router.Router;
import org.example.thesisuj.router.StaticRoutingEntry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to add a static route with next-hop IP address and custom administrative distance.
 * Format: set protocols static route <destination> next-hop <next-hop> distance <distance>
 */
public class SetRouteNextHopDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+distance\\s+(\\d+)"
	);
	private String destinationSubnet;
	private String nextHop;
	private int distance;

	@Override
	public void execute(Router router) {
		try {
			router.addRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop),
							distance
					)
			);
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
				CLIErrorHandler.formatRouteNextHopDistance(destinationSubnet, nextHop, distance));
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
		return "set protocols static route <destination> next-hop <next-hop> distance <distance>";
	}

	@Override
	public String getDescription() {
		return "Add static route via next-hop with custom distance";
	}
}

