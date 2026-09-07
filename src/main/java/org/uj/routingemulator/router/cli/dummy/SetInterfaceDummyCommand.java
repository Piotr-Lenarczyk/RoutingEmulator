package org.uj.routingemulator.router.cli.dummy;

import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetInterfaceDummyCommand implements RouterCommand {

	private static final Pattern PATTERN = Pattern.compile(
			"set\\s+interfaces\\s+dummy\\s+(\\S+)\\s+address\\s+(\\S+)"
	);

	private String routerInterfaceName;
	private String address;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		router.configureInterface(routerInterfaceName, InterfaceAddress.fromString(address));
		out.println("[edit]");
		out.flush();
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
		return "set interfaces dummy <interface> address <address>";
	}

	@Override
	public String getDescription() {
		return "Configure dummy interface with one IP address";
	}
}