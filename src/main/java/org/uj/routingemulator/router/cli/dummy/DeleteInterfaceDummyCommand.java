package org.uj.routingemulator.router.cli.dummy;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to remove IP address configuration from a dummy interface,
 * or to administratively enable it by deleting the 'disable' node.
 */
public class DeleteInterfaceDummyCommand implements RouterCommand {

	// Regex matches either 'address <ip>' OR 'disable' at the end of the command
	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+interfaces\\s+dummy\\s+(\\S+)\\s+(?:address\\s+(\\S+)|(disable))"
	);

	private String routerInterfaceName;
	private String address;
	private boolean isDisable;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			// Branch logic based on what the user wants to delete
			if (isDisable) {
				router.enableInterface(routerInterfaceName);
			} else {
				router.deleteInterfaceAddress(routerInterfaceName);
			}
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			String errorPath = isDisable
					? "delete interfaces dummy " + routerInterfaceName + " disable"
					: CLIErrorHandler.formatDeleteInterfaceEthernet(routerInterfaceName, address);

			throw CLIErrorHandler.handleInterfaceException(e, errorPath);
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			routerInterfaceName = matcher.group(1);
			address = matcher.group(2);
			isDisable = matcher.group(3) != null;
			return true;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "delete interfaces dummy <interface> <address <ip> | disable>";
	}

	@Override
	public String getDescription() {
		return "Remove Ethernet interface configuration line";
	}
}