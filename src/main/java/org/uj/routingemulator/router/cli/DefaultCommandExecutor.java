package org.uj.routingemulator.router.cli;

import java.util.logging.Logger;

public class DefaultCommandExecutor implements CommandExecutor {
	private static final Logger logger = Logger.getLogger(DefaultCommandExecutor.class.getName());
	private final RouterCLIParser parser;

	public DefaultCommandExecutor(RouterCLIParser parser) {
		this.parser = parser;
	}

	@Override
	public void execute(String input, CommandExecutionContext context) {
		logger.info("%s: Executing command: %s".formatted(context.router().getName(), input));
		RouterCommand command = parser.parse(input);

		if (command != null) {
			try {
				logger.info("%s: Command match found: %s for input string: %s".formatted(context.router().getName(), command.getCommandPattern(), input));
				command.execute(context);
			} catch (RuntimeException e) {
				context.output().println(e.getMessage());
			}
		} else {
			context.output().println("Command not recognized or not supported");
		}
	}
}