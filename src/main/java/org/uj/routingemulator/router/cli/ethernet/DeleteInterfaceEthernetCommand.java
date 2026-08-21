package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.RouterConfigurationService;
import org.uj.routingemulator.router.cli.*;

import java.util.Optional;

public class DeleteInterfaceEthernetCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("delete interfaces ethernet <interface> address <address>");
	private final RouterConfigurationService service = new RouterConfigurationService();

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> new Invocation(args.get("interface"), args.get("address")));
	}

	private class Invocation implements ParsedCommand {
		private final String routerInterfaceName;
		private final String subnet;

		public Invocation(String routerInterfaceName, String subnet) {
			this.routerInterfaceName = routerInterfaceName;
			this.subnet = subnet;
		}

		@Override
		public CommandResult execute(CommandExecutionContext context) {
			try {
				service.deleteInterfaceAddress(context.router(), routerInterfaceName);
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