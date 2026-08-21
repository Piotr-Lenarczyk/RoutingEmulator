package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.cli.ethernet.DeleteInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.DisableInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.ethernet.SetInterfaceEthernetCommand;
import org.uj.routingemulator.router.cli.route.*;

import java.util.*;

public class CommandRegistry {
	private final Map<String, RouterCommand> commandsByPath = new LinkedHashMap<>();

	public static CommandRegistry defaultRegistry() {
		CommandRegistry registry = new CommandRegistry();

		registry.register(new ShowIpRouteCommand());
		registry.register(new ShowConfigurationCommand());
		registry.register(new ShowInterfacesCommand());
		registry.register(new PingCommand());

		registry.register(new ConfigureCommand());
		registry.register(new CommitCommand());
		registry.register(new ExitCommand());
		registry.register(new ForceExitCommand());

		RouteOperation setOp = new SetRouteOperation();
		RouteOperation deleteOp = new DeleteRouteOperation();
		RouteOperation disableOp = new DisableRouteOperation();

		RouteTargetStrategy nextHopStrategy = new NextHopTargetStrategy(false);
		RouteTargetStrategy nextHopDistStrategy = new NextHopTargetStrategy(true);
		RouteTargetStrategy interfaceStrategy = new InterfaceTargetStrategy(false);
		RouteTargetStrategy interfaceDistStrategy = new InterfaceTargetStrategy(true);

		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> next-hop <next-hop>", "Add static route via next-hop with default distance", setOp, nextHopStrategy, false));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> next-hop <next-hop> distance <distance>", "Add static route via next-hop with custom distance", setOp, nextHopDistStrategy, true));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> interface <interface>", "Add static route via interface with default distance", setOp, interfaceStrategy, false));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> interface <interface> distance <distance>", "Add static route via interface with custom distance", setOp, interfaceDistStrategy, true));

		registry.register(new ParameterizedRouteCommand("delete protocols static route <destination> next-hop <next-hop>", "Delete static route via next-hop with default distance", deleteOp, nextHopStrategy, false));
		registry.register(new ParameterizedRouteCommand("delete protocols static route <destination> next-hop <next-hop> distance <distance>", "Delete static route via next-hop with custom distance", deleteOp, nextHopDistStrategy, true));
		registry.register(new ParameterizedRouteCommand("delete protocols static route <destination> interface <interface>", "Delete static route via interface with default distance", deleteOp, interfaceStrategy, false));
		registry.register(new ParameterizedRouteCommand("delete protocols static route <destination> interface <interface> distance <distance>", "Delete static route via interface with custom distance", deleteOp, interfaceDistStrategy, true));

		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> next-hop <next-hop> disable", "Disable static route via next-hop with default distance", disableOp, nextHopStrategy, false));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> next-hop <next-hop> distance <distance> disable", "Disable static route via next-hop with custom distance", disableOp, nextHopDistStrategy, true));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> interface <interface> disable", "Disable static route via interface with default distance", disableOp, interfaceStrategy, false));
		registry.register(new ParameterizedRouteCommand("set protocols static route <destination> interface <interface> distance <distance> disable", "Disable static route via interface with custom distance", disableOp, interfaceDistStrategy, true));

		registry.register(new DeleteInterfaceEthernetCommand());
		registry.register(new DisableInterfaceEthernetCommand());
		registry.register(new SetInterfaceEthernetCommand());

		return registry;
	}

	public void register(RouterCommand command) {
		commandsByPath.put(command.getSyntax().getPattern(), command);
	}

	public List<RouterCommand> getCommands() {
		return new ArrayList<>(commandsByPath.values());
	}

	public ParsedCommand resolve(String input) {
		if (input == null || input.trim().isEmpty()) {
			return null;
		}

		List<ParsedCommand> exactMatches = new ArrayList<>();
		for (RouterCommand command : commandsByPath.values()) {
			Optional<ParsedCommand> parsed = command.parse(input);
			parsed.ifPresent(exactMatches::add);
		}

		if (exactMatches.size() > 1) {
			return context -> new CommandFailure("Ambiguous command");
		}
		if (exactMatches.size() == 1) {
			return exactMatches.get(0);
		}

		List<RouterCommand> prefixMatches = new ArrayList<>();
		for (RouterCommand command : commandsByPath.values()) {
			if (command.getSyntax().matchesPrefix(input)) {
				prefixMatches.add(command);
			}
		}

		if (prefixMatches.size() > 1) {
			return context -> new CommandFailure("Ambiguous command");
		}
		if (prefixMatches.size() == 1) {
			RouterCommand prefixMatch = prefixMatches.get(0);
			if (prefixMatch.getSyntax().getPattern().contains("<")) {
				return context -> new CommandFailure("Incomplete command");
			}
			return prefixMatch.parse(prefixMatch.getSyntax().getPattern()).orElse(null);
		}

		return null;
	}
}