package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;

import java.util.regex.Pattern;

public class ShowInterfacesCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+interfaces$");
	private static final String HEADER = "%-16s %-33s %-4s %-17s %-10s %-6s %s%n";

	@Override
	public void execute(CommandExecutionContext context) {
		CommandOutput out = context.output();
		if (context.router().getMode() != RouterMode.OPERATIONAL) {
			out.println("Invalid command: show [interfaces]");
			return;
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

		out.print(output.toString());
	}

	@Override
	public boolean matches(String command) {
		return PATTERN.matcher(command.trim()).matches();
	}

	@Override
	public String getCommandPattern() {
		return "show interfaces";
	}

	@Override
	public String getDescription() {
		return "Display information about interfaces";
	}
}