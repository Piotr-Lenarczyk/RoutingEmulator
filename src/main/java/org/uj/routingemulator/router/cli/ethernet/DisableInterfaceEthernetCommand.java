package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to administratively disable an ethernet interface (shutdown).
 * <p>
 * Command format: {@code set interfaces ethernet <interface> disable}
 * <p>
 * Example: {@code set interfaces ethernet eth0 disable}
 * <p>
 * When an interface is disabled:
 * <ul>
 *   <li>Administrative state is set to ADMIN_DOWN</li>
 *   <li>Interface cannot pass traffic</li>
 *   <li>Routing entries using this interface remain but are inactive</li>
 *   <li>Configuration is preserved and can be re-enabled</li>
 * </ul>
 */
public class DisableInterfaceEthernetCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+ethernet\\s+(\\S+)\\s+disable"
	);
	private String routerInterfaceName;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.disableInterface(routerInterfaceName);
			out.println("[edit]");
			out.flush();
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

