package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

/**
 * Command to forcefully exit configuration mode and discard uncommitted changes.
 * Works regardless of current router mode.
 */
public class ForceExitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		router.setModeForced(RouterMode.OPERATIONAL);
		System.out.println("exit");
	}

	@Override
	public boolean matches(String command) {
		return command.trim().equals("exit discard");
	}

	@Override
	public String getCommandPattern() {
		return "exit discard";
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode and forcibly discard changes";
	}
}
