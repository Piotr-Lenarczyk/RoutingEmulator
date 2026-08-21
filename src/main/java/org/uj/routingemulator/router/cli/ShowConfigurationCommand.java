package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.config.ConfigurationFactory;
import org.uj.routingemulator.router.config.ConfigurationGenerator;

import java.util.Optional;

public class ShowConfigurationCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("show configuration");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			ConfigurationGenerator generator = ConfigurationFactory.getHierarchicalGenerator();

			Router router = context.router();
			Router committedRouter = new Router(router.getName(), router.getInterfaces());
			committedRouter.getRoutingTable().getRoutingEntries().addAll(router.getRoutingTable().getRoutingEntries());

			String output = generator.generateConfiguration(committedRouter);

			if (output.isEmpty()) {
				return new CommandSuccess("/* No configuration */");
			} else {
				return new CommandSuccess(output);
			}
		});
	}

	@Override
	public String getDescription() {
		return "Display the current configuration";
	}
}