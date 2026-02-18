package org.uj.routingemulator.router.cli.route;

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
 * CLI command to disable a static route via an interface with custom administrative distance.
 * Format: set protocols static route <destination> interface <interface> distance <distance> disable
 */
public class DisableRouteInterfaceDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+interface\\s+(\\S+)\\s+distance\\s+(\\d+)\\s+disable"
	);
	private String destinationSubnet;
	private String interfaceName;
	private int distance;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.disableRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							router.findFromName(interfaceName),
							distance
					)
			);
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDisableRouteInterfaceDistance(destinationSubnet, interfaceName, distance));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			destinationSubnet = matcher.group(1);
			interfaceName = matcher.group(2);
			distance = Integer.parseInt(matcher.group(3));
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "set protocols static route <destination> interface <interface> distance <distance> disable";
	}

	@Override
	public String getDescription() {
		return "Disable static route via interface with custom distance";
	}
}

