package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.exceptions.UncommittedChangesException;

public class ExitCommand implements RouterCommand {
	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		if (context.router().getMode() != RouterMode.CONFIGURATION) {
			out.println("\n\tInvalid command: [exit]\n");
		} else {
			try {
				context.router().setMode(RouterMode.OPERATIONAL);
				out.println("exit");
			} catch (UncommittedChangesException e) {
				out.println(e.getMessage());
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