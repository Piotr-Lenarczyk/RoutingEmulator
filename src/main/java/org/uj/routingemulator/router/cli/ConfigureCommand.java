package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

import java.util.Optional;

public class ConfigureCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("configure");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			if (context.router().getMode() == RouterMode.OPERATIONAL) {
				context.router().setMode(RouterMode.CONFIGURATION);
				return new CommandSuccess("[edit]");
			} else {
				return new CommandFailure("\n\tInvalid command: [configure]\n\n[edit]");
			}
		});
	}

	@Override
	public String getDescription() {
		return "Enter configuration mode";
	}
}