package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.config.ConfigurationFactory;
import org.uj.routingemulator.router.config.ConfigurationGenerator;

import java.util.regex.Pattern;

/**
 * Command to display the router's configuration in hierarchical format.
 * Shows the current committed configuration.
 *
 * <p>Command format: {@code show configuration}
 *
 * <p>Can be executed in both OPERATIONAL and CONFIGURATION mode.
 * In CONFIGURATION mode, shows the committed configuration (not staged changes).
 */
public class ShowConfigurationCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+configuration$");

	@Override
	public void execute(Router router) {
		ConfigurationGenerator generator = ConfigurationFactory.getHierarchicalGenerator();

		// Create a temporary router with committed state to generate configuration
		Router committedRouter = new Router(router.getName(), router.getInterfaces());
		committedRouter.getRoutingTable().getRoutingEntries().addAll(router.getRoutingTable().getRoutingEntries());

		String output = generator.generateConfiguration(committedRouter);
		if (output.isEmpty()) {
			System.out.println("/* No configuration */");
		} else {
			System.out.print(output);
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

