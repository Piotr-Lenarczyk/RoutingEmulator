package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CLI command to administratively disable an ethernet interface.
 * Format: set interfaces ethernet <interface> disable
 */
public class DisableInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+disable"
	);
	private String routerInterfaceName;

	@Override
	public void execute(Router router) {
		try {
			router.disableInterface(routerInterfaceName);
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleInterfaceException(e,
				CLIErrorHandler.formatDisableInterfaceEthernet(routerInterfaceName, ""));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			routerInterfaceName = matcher.group(1);
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "set interfaces ethernet <interface> disable";
	}

	@Override
	public String getDescription() {
		return "Administratively disable an ethernet interface";
	}
}

