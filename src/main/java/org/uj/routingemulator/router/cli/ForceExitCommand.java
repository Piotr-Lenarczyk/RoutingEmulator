package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

public class ForceExitCommand implements RouterCommand {
	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		context.router().setModeForced(RouterMode.OPERATIONAL);
		out.println("exit");
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