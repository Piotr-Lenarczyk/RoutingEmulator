package org.uj.routingemulator.router.cli.dummy;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisableInterfaceDummyCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+dummy\\s+(\\S+)\\s+disable"
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
		return "set interfaces dummy <interface> disable";
	}

	@Override
	public String getDescription() {
		return "Administratively disable a dummy interface";
	}
}
