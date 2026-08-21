package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.exceptions.NoChangesToCommitException;

import java.util.Optional;

public class CommitCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("commit");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			try {
				context.router().getConfigSession().commit();
				return new CommandSuccess("[edit]");
			} catch (NoChangesToCommitException e) {
				return new CommandFailure("No configuration changes to commit\n[edit]");
			}
		});
	}

	@Override
	public String getDescription() {
		return "Commit configuration changes";
	}
}