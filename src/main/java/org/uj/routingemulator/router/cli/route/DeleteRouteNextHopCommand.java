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
 * Command to delete a static route with next-hop IP address (default distance).
 * <p>
 * Command format: {@code delete protocols static route <destination> next-hop <next-hop>}
 * <p>
 * Example: {@code delete protocols static route 192.168.1.0/24 next-hop 10.0.0.1}
 * <p>
 * This permanently removes the route from the routing table.
 * The route must be re-added if needed again.
 */
public class DeleteRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)"
	);
	private String destinationSubnet;
	private String nextHop;
	@Override
	public void execute(Router router) {
		try {
			router.removeRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop)
					)
			);
			System.out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDeleteRouteNextHop(destinationSubnet, nextHop));
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
		return "delete protocols static route <destination> next-hop <next-hop>";
	}

	@Override
	public String getDescription() {
		return "Delete static route via next-hop with default distance";
	}
}
