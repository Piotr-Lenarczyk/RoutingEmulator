package org.uj.routingemulator.router.cli;

import java.util.List;

public class RouterCLIParser {

	private final CommandRegistry registry;

	public RouterCLIParser(CommandRegistry registry) {
		this.registry = registry;
	}

	public List<RouterCommand> getCommands() {
		return registry.getCommands();
	}

	public ParsedCommand parse(String input) {
		return registry.resolve(input);
	}
}