package org.uj.routingemulator.router.cli;

public interface CommandExecutor {
	void execute(String input, CommandExecutionContext context);
}