package org.uj.routingemulator.router.cli;

import java.util.Optional;

public interface RouterCommand {
	CommandSyntax getSyntax();

	Optional<ParsedCommand> parse(String command);
	String getDescription();
}