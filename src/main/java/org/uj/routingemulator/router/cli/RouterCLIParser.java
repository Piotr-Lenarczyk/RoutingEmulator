package org.uj.routingemulator.router.cli;

import lombok.Getter;
import org.uj.routingemulator.router.cli.ethernet.DeleteInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.DisableInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.SetInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.route.*;

import java.util.ArrayList;
import java.util.List;

public class RouterCLIParser {

	@Getter
	private final List<RouterCommand> commands;

	public RouterCLIParser() {
		this.commands = new ArrayList<>();
		registerCommands();
	}

	private void registerCommands() {
		commands.add(new ShowIpRouteCommand());
		commands.add(new ShowConfigurationCommand());
		commands.add(new ShowInterfacesCommand());
		commands.add(new PingCommand());

		commands.add(new ConfigureCommand());
		commands.add(new CommitCommand());
		commands.add(new ExitCommand());
		commands.add(new ForceExitCommand());

		commands.add(new DeleteRouteNextHopDistanceCommand());
		commands.add(new DeleteRouteInterfaceDistanceCommand());
		commands.add(new DeleteRouteNextHopCommand());
		commands.add(new DeleteRouteInterfaceCommand());

		commands.add(new DisableRouteNextHopDistanceCommand());
		commands.add(new DisableRouteInterfaceDistanceCommand());
		commands.add(new DisableRouteNextHopCommand());
		commands.add(new DisableRouteInterfaceCommand());

		commands.add(new SetRouteNextHopDistanceCommand());
		commands.add(new SetRouteInterfaceDistanceCommand());
		commands.add(new SetRouteNextHopCommand());
		commands.add(new SetRouteInterfaceCommand());

		commands.add(new DeleteInterfaceEthernetCommand());
		commands.add(new DisableInterfaceEthernetCommand());
		commands.add(new SetInterfaceEthernetCommand());
	}

	public RouterCommand parse(String input) {
		for (RouterCommand command : commands) {
			if (command.matches(input)) {
				return command;
			}
		}
		return CommandMatcher.findUniquePrefixMatch(input, commands);
	}
}