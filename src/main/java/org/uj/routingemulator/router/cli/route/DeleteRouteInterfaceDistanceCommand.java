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
 * CLI command to reset the administrative distance of an interface route to its default.
 * Format: delete protocols static route <destination> interface <interface> distance
 */
public class DeleteRouteInterfaceDistanceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+interface\\s+(\\S+)\\s+distance"
	);
	private String destinationSubnet;
	private String interfaceName;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.resetRouteDistance(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							router.findFromName(interfaceName)
					)
			);
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDeleteRouteInterface(destinationSubnet, interfaceName));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			destinationSubnet = matcher.group(1);
			interfaceName = matcher.group(2);
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "delete protocols static route <destination> interface <interface> distance";
	}

	@Override
	public String getDescription() {
		return "Reset static route via interface to the default distance";
	}
}

