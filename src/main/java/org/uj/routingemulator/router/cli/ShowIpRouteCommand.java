package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.model.RoutingTablePresenter;

import java.util.Optional;

public class ShowIpRouteCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("show ip route");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			String output = RoutingTablePresenter.showIpRoute(context.router());
			return new CommandSuccess(output);
		});
	}

	@Override
	public String getDescription() {
		return "Display IP routing table";
	}
}