package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.cli.*;
import org.uj.routingemulator.router.session.RouterConfigurationService;

import java.util.Optional;

public class DisableInterfaceEthernetCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("set interfaces ethernet <interface> disable");
	private final RouterConfigurationService service = new RouterConfigurationService();

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> new Invocation(args.get("interface")));
	}

	private class Invocation implements ParsedCommand {
		private final String routerInterfaceName;

		public Invocation(String routerInterfaceName) {
			this.routerInterfaceName = routerInterfaceName;
		}

		@Override
		public CommandResult execute(CommandExecutionContext context) {
			try {
				service.disableInterface(context.router(), routerInterfaceName);
				return new CommandSuccess("[edit]");
			} catch (RuntimeException e) {
				throw new RuntimeException(CLIErrorHandler.handleException(e, "set interfaces ethernet " + routerInterfaceName + " disable"));
			}
		}
	}

	@Override
	public String getDescription() {
		return "Administratively disable an ethernet interface";
	}
}