package org.uj.routingemulator.router.cli;

public class CliSession {
	private final CommandExecutor executor;
	private final CommandExecutionContext context;

	public CliSession(CommandExecutor executor, CommandExecutionContext context) {
		this.executor = executor;
		this.context = context;
	}

	public void execute(String input) {
		executor.execute(input, context);
	}
}