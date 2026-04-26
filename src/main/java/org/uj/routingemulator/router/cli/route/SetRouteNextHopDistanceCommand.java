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
 * Command to add a static route with next-hop IP address and custom administrative distance.
 * <p>
 * Command format: {@code set protocols static route <destination> next-hop <next-hop> distance <distance>}
 * <p>
 * Example: {@code set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 distance 10}
 * <p>
 * Administrative distance (1-255) is used for route selection when multiple routes
 * to the same destination exist. Lower values are preferred.
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
		PrintWriter out = CLIContext.getWriter();
		try {
			Subnet dest;
			try {
				dest = Subnet.fromString(destinationSubnet);
			} catch (RuntimeException e) {
				String msg = String.format("\n\tError: %s is not a valid IPv4 prefix\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed\n\n[edit]", destinationSubnet);
				throw new RuntimeException(msg);
			}

			IPAddress nh;
			try {
				nh = IPAddress.fromString(nextHop);
			} catch (Exception e) {
				if (nextHop != null && nextHop.contains("/")) {
					String msg = String.format("\n\tError: %s is not a valid IPv4 prefix\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed\n\n[edit]", nextHop);
					throw new InvalidNextHopException(msg);
				}
				if (e instanceof RuntimeException) throw (RuntimeException) e;
				throw new RuntimeException(e);
			}

			router.addRoute(
					new StaticRoutingEntry(
							dest,
							nh,
							distance
					)
			);
			out.println("[edit]");
			out.flush();
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
