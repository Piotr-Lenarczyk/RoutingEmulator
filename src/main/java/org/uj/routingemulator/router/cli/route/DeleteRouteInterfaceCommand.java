package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.CommandExecutionContext;
import org.uj.routingemulator.router.cli.CommandOutput;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteRouteInterfaceCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+protocols\\s+static\\s+route\\s+(\\S+)\\s+interface\\s+(\\S+)"
	);

	private String destinationSubnet;
	private String interfaceName;

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		try {
			context.router().removeRoute(
					new StaticRoutingEntry(
							Subnet.fromString(destinationSubnet),
							context.router().findFromName(interfaceName)
					)
			);
			out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleRouteException(e, CLIErrorHandler.formatDeleteRouteInterface(destinationSubnet, interfaceName));
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
		return "delete protocols static route <destination> interface <interface>";
	}

	@Override
	public String getDescription() {
		return "Delete static route via interface with default distance";
	}
}