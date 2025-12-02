package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

/**
 * Command to exit configuration mode and return to operational mode.
 * Only works when router is in configuration mode.
 */
public class ExitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			System.out.println("\n\tInvalid command: [exit]\n");
		} else {
			router.setMode(RouterMode.OPERATIONAL);
			System.out.println("exit");
		}
	}

	@Override
	public boolean matches(String command) {
		return command.trim().equals("exit");
	}

	@Override
	public String getCommandPattern() {
		return "exit";
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode";
	}
}
