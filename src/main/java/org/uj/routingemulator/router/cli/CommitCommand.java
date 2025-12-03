package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;

/**
 * Command to commit configuration changes.
 * Only works when router is in configuration mode.
 */
public class CommitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		try {
			router.commitChanges();
			System.out.println("[edit]");
		} catch (RuntimeException e) {
			if ("No configuration changes to commit".equals(e.getMessage())) {
				System.out.println("No configuration changes to commit");
				System.out.println("[edit]");
			} else {
				throw e;
			}
		}
	}

	@Override
	public boolean matches(String command) {
		return command.trim().equals("commit");
	}

	@Override
	public String getCommandPattern() {
		return "commit";
	}

	@Override
	public String getDescription() {
		return "Commit configuration changes";
	}
}

