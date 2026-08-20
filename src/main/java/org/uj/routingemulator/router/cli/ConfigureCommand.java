package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

public class ConfigureCommand implements RouterCommand {
	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		if (context.router().getMode() == RouterMode.OPERATIONAL) {
			context.router().setMode(RouterMode.CONFIGURATION);
			out.println("[edit]");
		} else {
			out.println("\n\tInvalid command: [configure]\n\n[edit]");
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