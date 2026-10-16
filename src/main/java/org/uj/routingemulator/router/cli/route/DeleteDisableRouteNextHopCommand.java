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
 * Deletes the disable node from a next-hop static route.
 */
public class DeleteDisableRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+disable");
	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			NextHopRouteParameters parameters = NextHopRouteParameters.parseRouteParameters(destinationSubnet, nextHop);
			router.enableRoute(new StaticRoutingEntry(parameters.dest(), parameters.nh()));
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
		if (!matcher.matches()) return false;
		destinationSubnet = matcher.group(1);
		nextHop = matcher.group(2);
		return true;
	}

	@Override
	public String getCommandPattern() {
		return "delete protocols static route <destination> next-hop <next-hop> disable";
	}

	@Override
	public String getDescription() {
		return "Re-enable static route via next-hop";
	}
}