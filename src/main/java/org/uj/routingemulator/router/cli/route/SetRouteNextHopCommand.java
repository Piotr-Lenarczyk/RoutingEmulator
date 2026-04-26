package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.exceptions.InvalidNextHopException;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
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
		PrintWriter out = CLIContext.getWriter();
		try {
			Subnet dest;
			try {
				dest = Subnet.fromString(destinationSubnet);
			} catch (RuntimeException e) {
				// Provide user-friendly message for invalid prefix
				String msg = String.format("\n\tError: %s is not a valid IPv4 prefix\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed\n\n[edit]", destinationSubnet);
				throw new RuntimeException(msg);
			}

			IPAddress nh;
			try {
				nh = IPAddress.fromString(nextHop);
			} catch (RuntimeException e) {
				// If nextHop contains a mask, produce a clearer error message
				if (nextHop != null && nextHop.contains("/")) {
					String msg = String.format("\n\tError: %s is not a valid IPv4 prefix\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed\n\n[edit]", nextHop);
					throw new InvalidNextHopException(msg);
				}
				// otherwise rethrow
				throw e;
			}

			router.addRoute(
					new StaticRoutingEntry(
							dest,
							nh
					)
			);
			out.println("[edit]");
			out.flush();
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
