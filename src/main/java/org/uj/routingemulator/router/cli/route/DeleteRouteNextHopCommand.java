package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.NextHopRouteParameters;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.CommandExecutionContext;
import org.uj.routingemulator.router.cli.CommandOutput;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteRouteNextHopCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+next-hop\\s+(\\S+)"
	);

	private String destinationSubnet;
	private String nextHop;

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		try {
			NextHopRouteParameters nextHopRouteParameters = NextHopRouteParameters.parseRouteParameters(destinationSubnet, nextHop);
			context.router().removeRoute(new StaticRoutingEntry(nextHopRouteParameters.dest(), nextHopRouteParameters.nh()));
			out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e, CLIErrorHandler.formatDeleteRouteNextHop(destinationSubnet, nextHop));
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