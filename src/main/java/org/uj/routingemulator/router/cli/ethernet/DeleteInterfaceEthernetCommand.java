package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.cli.*;

import java.util.Optional;

public class DeleteInterfaceEthernetCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("delete interfaces ethernet <interface> address <address>");

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args ->
				new Invocation(args.get("interface"), args.get("address"))
		);
	}

	private record Invocation(String routerInterfaceName, String subnet) implements ParsedCommand {
		@Override
		public CommandResult execute(CommandExecutionContext context) {
			try {
				context.router().getConfigSession().deleteInterfaceAddress(routerInterfaceName);
				return new CommandSuccess("[edit]");
			} catch (RuntimeException e) {
				throw new RuntimeException(CLIErrorHandler.handleException(e, "delete interfaces ethernet " + routerInterfaceName + " address " + subnet));
			}
		}
	}

	@Override
	public String getDescription() {
		return "Remove IP address from an ethernet interface";
	}
}