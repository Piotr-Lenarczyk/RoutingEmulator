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
 * Command to add a static route with next-hop IP address.
 * <p>
 * Command format: {@code set protocols static route <destination> next-hop <next-hop>}
 * <p>
 * Example: {@code set protocols static route 192.168.1.0/24 next-hop 10.0.0.1}
 * <p>
 * This creates a static route with:
 * <ul>
 *   <li>Destination network in CIDR notation</li>
 *   <li>Next-hop IP address (must be reachable)</li>
 *   <li>Default administrative distance of 1</li>
 * </ul>
 * <p>
 * The next-hop address should be on a directly connected network.
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
			System.out.println("[edit]");
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

