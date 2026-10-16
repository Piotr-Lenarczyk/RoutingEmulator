package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.NextHopRouteParameters;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to reset the administrative distance of a next-hop route to its default.
 * Format: delete protocols static route <destination> next-hop <next-hop> distance
 */
public class DeleteRouteNextHopDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+distance"
	);
	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			NextHopRouteParameters nextHopRouteParameters = NextHopRouteParameters.parseRouteParameters(destinationSubnet, nextHop);
			router.resetRouteDistance(new StaticRoutingEntry(nextHopRouteParameters.dest(), nextHopRouteParameters.nh()));
			out.println("[edit]");
			out.flush();
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
		return "delete protocols static route <destination> next-hop <next-hop> distance";
	}

	@Override
	public String getDescription() {
		return "Reset static route via next-hop to the default distance";
	}
}
