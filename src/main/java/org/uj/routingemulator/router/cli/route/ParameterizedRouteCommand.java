package org.uj.routingemulator.router.cli.route;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.StaticRoutingEntry;
import org.uj.routingemulator.router.cli.*;

import java.util.Map;
import java.util.Optional;

public class ParameterizedRouteCommand implements RouterCommand {
	private final CommandSyntax syntax;
	private final String description;
	private final RouteOperation operation;
	private final RouteTargetStrategy targetStrategy;
	private final boolean hasDistance;

	public ParameterizedRouteCommand(String syntaxStr, String description, RouteOperation operation, RouteTargetStrategy targetStrategy, boolean hasDistance) {
		this.syntax = new CommandSyntax(syntaxStr);
		this.description = description;
		this.operation = operation;
		this.targetStrategy = targetStrategy;
		this.hasDistance = hasDistance;
	}

	@Override
	public CommandSyntax getSyntax() {
		return syntax;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return syntax.parseFully(command).flatMap(args -> {
			int distance = 1;
			if (hasDistance) {
				try {
					distance = Integer.parseInt(args.get("distance"));
				} catch (NumberFormatException e) {
					return Optional.empty();
				}
			}
			return Optional.of(new Invocation(args, distance));
		});
	}

	@Override
	public String getDescription() {
		return description;
	}

	private class Invocation implements ParsedCommand {
		private final Map<String, String> args;
		private final int distance;

		public Invocation(Map<String, String> args, int distance) {
			this.args = args;
			this.distance = distance;
		}

		@Override
		public CommandResult execute(CommandExecutionContext context) {
			Router router = context.router();
			String destinationSubnet = args.get("destination");
			try {
				StaticRoutingEntry entry = targetStrategy.createEntry(router, destinationSubnet, args, distance);
				operation.apply(router, entry);
				return new CommandSuccess("[edit]");
			} catch (RuntimeException e) {
				throw new RuntimeException(CLIErrorHandler.handleException(e, targetStrategy.formatErrorPath(operation, destinationSubnet, args, distance)));
			}
		}
	}
}