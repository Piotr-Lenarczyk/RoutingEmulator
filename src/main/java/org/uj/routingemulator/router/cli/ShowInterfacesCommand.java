package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.model.RouterInterface;
import org.uj.routingemulator.router.model.RouterMode;

import java.util.Optional;

public class ShowInterfacesCommand implements RouterCommand {
	private static final CommandSyntax SYNTAX = new CommandSyntax("show interfaces");
	private static final String HEADER = "%-16s %-33s %-4s %-17s %-10s %-6s %s%n";

	@Override
	public CommandSyntax getSyntax() {
		return SYNTAX;
	}

	@Override
	public Optional<ParsedCommand> parse(String command) {
		return SYNTAX.parseFully(command).map(args -> context -> {
			if (context.router().getMode() != RouterMode.OPERATIONAL) {
				return new CommandFailure("Invalid command: show [interfaces]");
			}

			StringBuilder output = new StringBuilder();
			output.append(String.format("Codes: S - State, L - Link, u - Up, D - Down, A - Admin Down%n"));
			output.append(String.format(HEADER, "Interface", "IP Address", "S/L", "MAC", "VRF", "MTU", "Description"));
			output.append(String.format(HEADER, "---------", "----------", "---", "---", "---", "---", "-----------"));

			for (RouterInterface iface : context.router().getInterfaces()) {
				String interfaceName = iface.getInterfaceName();
				String ipAddress = iface.getInterfaceAddress() != null ? iface.getInterfaceAddress().toString() : "-";
				String state = iface.getStatus().toString();
				String macAddress = iface.getMacAddress() != null ? iface.getMacAddress().toString() : "-";
				String vrf = iface.getVrf() != null ? iface.getVrf() : "default";
				String mtu = String.valueOf(iface.getMtu());
				String description = iface.getDescription() != null ? iface.getDescription() : "";

				output.append(String.format(HEADER, interfaceName, ipAddress, state, macAddress, vrf, mtu, description));
			}

			return new CommandSuccess(output.toString());
		});
	}

	@Override
	public String getDescription() {
		return "Display information about interfaces";
	}
}