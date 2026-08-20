package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.CommandExecutionContext;
import org.uj.routingemulator.router.cli.CommandOutput;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+address\\s+(\\S+)"
	);

	private String routerInterfaceName;
	private String subnet;

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		try {
			context.router().deleteInterfaceAddress(routerInterfaceName);
			out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleInterfaceException(e, CLIErrorHandler.formatDeleteInterfaceEthernet(routerInterfaceName, subnet));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			routerInterfaceName = matcher.group(1);
			subnet = matcher.group(2);
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "delete interfaces ethernet <interface> address <address>";
	}

	@Override
	public String getDescription() {
		return "Remove IP address from an ethernet interface";
	}
}