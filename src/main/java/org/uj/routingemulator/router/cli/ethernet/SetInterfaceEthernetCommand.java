package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to configure an IP address on an ethernet interface.
 * <p>
 * Command format: {@code set interfaces ethernet <interface> address <address>}
 * <p>
 * Example: {@code set interfaces ethernet eth0 address 192.168.1.1/24}
 * <p>
 * The command validates:
 * <ul>
 *   <li>Interface exists on the router</li>
 *   <li>IP address is in valid format (CIDR notation)</li>
 *   <li>IP is not a network or broadcast address</li>
 *   <li>Configuration doesn't already exist</li>
 * </ul>
 */
public class SetInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+address\\s+(\\S+)"
	);
	private String routerInterfaceName;
	private String address;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.configureInterface(routerInterfaceName, InterfaceAddress.fromString(address));
			out.println("[edit]");
			out.flush();
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
