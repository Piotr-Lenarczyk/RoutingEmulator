package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterMode;

import java.util.regex.Pattern;

public class ShowIpRouteCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+ip\\s+route$");

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		if (context.router().getMode() != RouterMode.OPERATIONAL) {
			out.println("Invalid command: show [ip]");
			return;
		}

		String output = context.router().showIpRoute();
		out.println(output);
	}

	@Override
	public boolean matches(String command) {
		return PATTERN.matcher(command.trim()).matches();
	}

	@Override
	public String getCommandPattern() {
		return "show ip route";
	}

	@Override
	public String getDescription() {
		return "Display IP routing table";
	}
}