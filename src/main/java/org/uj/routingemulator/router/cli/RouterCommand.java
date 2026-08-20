package org.uj.routingemulator.router.cli;

public interface RouterCommand {
	void execute(CommandExecutionContext context);
	boolean matches(String command);
	String getCommandPattern();
	String getDescription();
}