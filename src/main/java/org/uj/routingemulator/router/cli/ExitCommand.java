package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

import java.util.Optional;

public class ExitCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("exit");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			if (context.router().getMode() != RouterMode.CONFIGURATION) {
				return new CommandFailure("\n\tInvalid command: [exit]\n");
			} else {
				context.router().setMode(RouterMode.OPERATIONAL);
				return new CommandSuccess("exit");
			}
		});
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode";
	}
}