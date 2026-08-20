package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;

public class CommitCommand implements RouterCommand {
	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		try {
			context.router().commitChanges();
			out.println("[edit]");
		} catch (NoChangesToCommitException e) {
			out.println("No configuration changes to commit");
			out.println("[edit]");
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