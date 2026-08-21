package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.RouterModeController;

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
			RouterModeController.setModeForced(context.router(), RouterMode.OPERATIONAL);
			return new CommandSuccess("exit");
		});
	}

	@Override
	public String getDescription() {
		return "Exit configuration mode and forcibly discard changes";
	}
}