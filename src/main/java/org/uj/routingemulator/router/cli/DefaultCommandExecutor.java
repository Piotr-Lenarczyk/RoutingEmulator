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
		ParsedCommand command = parser.parse(input);

		if (command != null) {
			try {
				logger.info("%s: Command match found for input string: %s".formatted(context.router().getName(), input));
				CommandResult result = command.execute(context);
				if (result.getOutput() != null && !result.getOutput().isEmpty()) {
					context.output().print(result.getOutput() + (result.getOutput().endsWith("\n") ? "" : "\n"));
				}
			} catch (RuntimeException e) {
				String formattedError = CLIErrorHandler.handleException(e, input);
				context.output().println(formattedError);
			}
		} else {
			context.output().println("Command not recognized or not supported");
		}
	}
}