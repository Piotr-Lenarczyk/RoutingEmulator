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
 * Deletes the disable node from an interface-based static route.
 */
public class DeleteDisableRouteInterfaceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+interface\\s+(\\S+)\\s+disable");
	private String destinationSubnet;
	private String interfaceName;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.enableRoute(new StaticRoutingEntry(
					Subnet.fromString(destinationSubnet), router.findFromName(interfaceName)));
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e,
					CLIErrorHandler.formatDisableRouteInterface(destinationSubnet, interfaceName));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (!matcher.matches()) return false;
		destinationSubnet = matcher.group(1);
		interfaceName = matcher.group(2);
		return true;
	}

	@Override
	public String getCommandPattern() {
		return "delete protocols static route <destination> interface <interface> disable";
	}

	@Override
	public String getDescription() {
		return "Re-enable static route via interface";
	}
}