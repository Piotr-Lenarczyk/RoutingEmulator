package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.router.RouterConfigurationService;
import org.uj.routingemulator.router.cli.*;

import java.util.Optional;

public class SetInterfaceEthernetCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("set interfaces ethernet <interface> address <address>");
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
		private final String address;

		public Invocation(String routerInterfaceName, String address) {
			this.routerInterfaceName = routerInterfaceName;
			this.address = address;
		}

		@Override
		public CommandResult execute(CommandExecutionContext context) {
			try {
				service.configureInterface(context.router(), routerInterfaceName, InterfaceAddress.fromString(address));
				return new CommandSuccess("[edit]");
			} catch (RuntimeException e) {
				throw new RuntimeException(CLIErrorHandler.handleException(e, "set interfaces ethernet " + routerInterfaceName + " address " + address));
			}
		}
	}

	@Override
	public String getDescription() {
		return "Configure interface with one IP address";
	}
}