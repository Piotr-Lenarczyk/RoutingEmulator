package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to delete an address from an ethernet interface.
 * Format: delete interfaces ethernet <interface> address <address>
 */
public class DeleteInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+address\\s+(\\S+)"
	);
	private String routerInterfaceName;
	private String subnet;

	@Override
	public void execute(Router router) {
		try {
			router.deleteInterfaceAddress(routerInterfaceName);
			System.out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleInterfaceException(e,
				CLIErrorHandler.formatDeleteInterfaceEthernet(routerInterfaceName, subnet));
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

