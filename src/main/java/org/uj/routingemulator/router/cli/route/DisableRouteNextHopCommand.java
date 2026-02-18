package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to disable a static route with next-hop IP address (default distance).
 * <p>
 * Command format: {@code set protocols static route <destination> next-hop <next-hop> disable}
 * <p>
 * Example: {@code set protocols static route 192.168.1.0/24 next-hop 10.0.0.1 disable}
 * <p>
 * Disabling a route:
 * <ul>
 *   <li>Keeps the route in configuration but marks it inactive</li>
 *   <li>Route will not be used for forwarding</li>
 *   <li>Can be re-enabled without re-entering full configuration</li>
 * </ul>
 */
public class DisableRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+disable"
	);
	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.disableRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							IPAddress.fromString(nextHop)
					)
			);
			out.println("[edit]");
			out.flush();
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
