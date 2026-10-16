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
 * Deletes the disable node from a distance-qualified next-hop static route.
 */
public class DeleteDisableRouteNextHopDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+distance\\s+(\\d+)\\s+disable");
	private String destinationSubnet;
	private String nextHop;
	private int distance;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.enableRoute(new StaticRoutingEntry(
					Subnet.fromString(destinationSubnet), IPAddress.fromString(nextHop), distance));
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDisableRouteNextHopDistance(destinationSubnet, nextHop, distance));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (!matcher.matches()) return false;
		destinationSubnet = matcher.group(1);
		nextHop = matcher.group(2);
		distance = Integer.parseInt(matcher.group(3));
		return true;
	}

	@Override
	public String getCommandPattern() {
		return "delete protocols static route <destination> next-hop <next-hop> distance <distance> disable";
	}

	@Override
	public String getDescription() {
		return "Re-enable distance-qualified static route via next-hop";
	}
}