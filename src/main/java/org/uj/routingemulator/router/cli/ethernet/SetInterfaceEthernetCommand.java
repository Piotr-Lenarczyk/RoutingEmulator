package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+address\\s+(\\S+)"
	);
	private String routerInterfaceName;
	private String address;

	@Override
	public void execute(Router router) {
		try {
			router.configureInterface(routerInterfaceName, InterfaceAddress.fromString(address));
			System.out.println("[edit]");
		} catch (RuntimeException e) {
			throw CLIErrorHandler.handleInterfaceException(e,
				CLIErrorHandler.formatSetInterfaceEthernet(routerInterfaceName, address));
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			routerInterfaceName = matcher.group(1);
			address = matcher.group(2);
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "set interfaces ethernet <interface> address <address>";
	}

	@Override
	public String getDescription() {
		return "Configure interface with one IP address";
	}
}
