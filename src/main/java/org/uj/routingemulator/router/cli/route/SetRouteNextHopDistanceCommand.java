package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.NextHopRouteParameters;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.CommandExecutionContext;
import org.uj.routingemulator.router.cli.CommandOutput;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetRouteNextHopDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)\\s+distance\\s+(\\d+)"
	);

	private String destinationSubnet;
	private String nextHop;
	private int distance;

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		try {
			NextHopRouteParameters nextHopRouteParameters = NextHopRouteParameters.parseRouteParameters(destinationSubnet, nextHop);
			context.router().addRoute(new StaticRoutingEntry(nextHopRouteParameters.dest(), nextHopRouteParameters.nh(), distance));
			out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e, CLIErrorHandler.formatRouteNextHopDistance(destinationSubnet, nextHop, distance));
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