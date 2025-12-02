package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;

import java.util.ArrayList;
import java.util.List;

public class RouterCLIParser {
	private final List<RouterCommand> commands;

	public RouterCLIParser() {
		this.commands = new ArrayList<>();
		registerCommands();
	}

	/**
	 * Registers all available CLI commands.
	 * Order is important: more specific patterns must be registered before general ones.
	 */
	private void registerCommands() {
		commands.add(new ConfigureCommand());
		commands.add(new ExitCommand());
		commands.add(new ForceExitCommand());
		// Register route commands - order matters: more specific patterns first
		// Disable commands (with distance first, then without)
		commands.add(new DisableRouteNextHopDistanceCommand());
		commands.add(new DisableRouteInterfaceDistanceCommand());
		commands.add(new DisableRouteNextHopCommand());
		commands.add(new DisableRouteInterfaceCommand());
		// Set commands (with distance first, then without)
		commands.add(new SetRouteNextHopDistanceCommand());
		commands.add(new SetRouteInterfaceDistanceCommand());
		commands.add(new SetRouteNextHopCommand());
		commands.add(new SetRouteInterfaceCommand());
	}

	public void executeCommand(String input, Router router) {
		for (RouterCommand command : commands) {
			if (command.matches(input)) {
				try {
					command.execute(router);
				} catch (RuntimeException e) {
					System.out.println(e.getMessage());
					System.out.flush();
				}
				return;
			}
		}
		System.out.println("Command not recognized or not supported");
	}

	/**
	 * Prints help information for all registered commands.
	 */
	public void printHelp() {
		for (RouterCommand command : commands) {
			System.out.printf(" - %s: %s%n", command.getCommandPattern(), command.getDescription());
		}
	}
}
