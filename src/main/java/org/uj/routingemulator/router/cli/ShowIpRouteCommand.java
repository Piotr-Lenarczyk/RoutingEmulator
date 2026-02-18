package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * Command to display the IP routing table.
 * Shows both static routes and connected routes (directly connected networks).
 *
 * <p>Command format: {@code show ip route}
 *
 * <p>Must be executed in OPERATIONAL mode.
 */
public class ShowIpRouteCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+ip\\s+route$");

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		if (router.getMode() != RouterMode.OPERATIONAL) {
			out.println("Invalid command: show [ip]");
			out.flush();
			return;
		}

		String output = router.showIpRoute();
		out.println(output);
		out.flush();
	}

	@Override
	public boolean matches(String command) {
		return PATTERN.matcher(command.trim()).matches();
	}

	@Override
	public String getCommandPattern() {
		return "show ip route";
	}

	@Override
	public String getDescription() {
		return "Display IP routing table";
	}
}

