package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

/**
 * Command to enter configuration mode.
 * Only works when router is in operational mode.
 */
public class ConfigureCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		if (router.getMode() == RouterMode.OPERATIONAL) {
			router.setMode(RouterMode.CONFIGURATION);
			System.out.println("[edit]");
		} else {
			System.out.println("\n\tInvalid command: [configure]\n\n[edit]");
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
