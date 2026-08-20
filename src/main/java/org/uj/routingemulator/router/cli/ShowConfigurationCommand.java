package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.config.ConfigurationFactory;
import org.uj.routingemulator.router.config.ConfigurationGenerator;

import java.util.regex.Pattern;

public class ShowConfigurationCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+configuration$");

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		ConfigurationGenerator generator = ConfigurationFactory.getHierarchicalGenerator();

		Router router = context.router();
		Router committedRouter = new Router(router.getName(), router.getInterfaces());
		committedRouter.getRoutingTable().getRoutingEntries().addAll(router.getRoutingTable().getRoutingEntries());

		String output = generator.generateConfiguration(committedRouter);

		if (output.isEmpty()) {
			out.println("/* No configuration */");
		} else {
			out.print(output);
		}
	}

	@Override
	public boolean matches(String command) {
		return PATTERN.matcher(command.trim()).matches();
	}

	@Override
	public String getCommandPattern() {
		return "show configuration";
	}

	@Override
	public String getDescription() {
		return "Display the current configuration";
	}
}