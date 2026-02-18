package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

import java.io.PrintWriter;

/**
 * Command to enter configuration mode.
 * Only works when router is in operational mode.
 */
public class ConfigureCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		if (router.getMode() == RouterMode.OPERATIONAL) {
			router.setMode(RouterMode.CONFIGURATION);
			out.println("[edit]");
			out.flush();
		} else {
			out.println("\n\tInvalid command: [configure]\n\n[edit]");
			out.flush();
		}
	}

	@Override
	public boolean matches(String command) {
		return command.trim().equals("configure");
	}

	@Override
	public String getCommandPattern() {
		return "configure";
	}

	@Override
	public String getDescription() {
		return "Enter configuration mode";
	}
}
