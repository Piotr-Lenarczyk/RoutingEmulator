package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;

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
		} catch (NoChangesToCommitException e) {
			System.out.println("No configuration changes to commit");
			System.out.println("[edit]");
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

