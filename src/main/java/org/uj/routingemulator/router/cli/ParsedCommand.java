package org.uj.routingemulator.router.cli;

public interface ParsedCommand {
	CommandResult execute(CommandExecutionContext context);
}