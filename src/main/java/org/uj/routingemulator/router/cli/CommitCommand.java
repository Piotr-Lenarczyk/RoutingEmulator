package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;

import java.io.PrintWriter;

/**
 * Command to commit configuration changes.
 * Only works when router is in configuration mode.
 */
public class CommitCommand implements RouterCommand {
	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			router.commitChanges();
			out.println("[edit]");
			out.flush();
		} catch (NoChangesToCommitException e) {
			out.println("No configuration changes to commit");
			out.println("[edit]");
			out.flush();
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

