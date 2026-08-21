package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

import java.util.Optional;

public class ForceExitCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("exit discard");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			context.router().setModeForced(RouterMode.OPERATIONAL);
			return new CommandSuccess("exit");
		});
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode and forcibly discard changes";
	}
}