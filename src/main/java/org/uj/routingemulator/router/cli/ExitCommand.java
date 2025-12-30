package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.exceptions.UncommittedChangesException;

/**
 * Command to exit configuration mode and return to operational mode.
 * Only works when router is in configuration mode.
 * If there are uncommitted changes, this command will fail.
 */
public class ExitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			System.out.println("\n\tInvalid command: [exit]\n");
		} else {
			try {
				router.setMode(RouterMode.OPERATIONAL);
				System.out.println("exit");
			} catch (UncommittedChangesException e) {
				System.out.println(e.getMessage());
			}
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
