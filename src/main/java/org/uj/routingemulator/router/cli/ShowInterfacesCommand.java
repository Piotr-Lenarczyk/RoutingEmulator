package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;

import java.util.regex.Pattern;

/**
 * Command to display information about router interfaces.
 * Shows interface names, IP addresses, status, MAC address, VRF, MTU, and description.
 *
 * <p>Command format: {@code show interfaces}
 *
 * <p>Must be executed in OPERATIONAL mode.
 * <p>Output format is similar to VyOS "show interfaces" command:
 * <pre>
 * Codes: S - State, L - Link, u - Up, D - Down, A - Admin Down
 * Interface        IP Address                        S/L  MAC               VRF        MTU    Description
 * ---------        ----------                        ---  ---               ---        ---    -----------
 * eth0             192.168.1.1/24                    u/u  00:50:79:66:68:00 default    1500
 * eth1             -                                 u/D  00:50:79:66:68:01 default    1500
 * lo               127.0.0.1/8                       u/u  00:50:79:66:68:02 default    65536
 * </pre>
 */
public class ShowInterfacesCommand implements RouterCommand {
	private static final Pattern PATTERN = Pattern.compile("^show\\s+interfaces$");

	@Override
	public void execute(Router router) {
		if (router.getMode() != RouterMode.OPERATIONAL) {
			System.out.println("Invalid command: show [interfaces]");
			return;
		}

		StringBuilder output = new StringBuilder();
		output.append("Codes: S - State, L - Link, u - Up, D - Down, A - Admin Down\n");
		output.append(String.format("%-16s %-33s %-4s %-17s %-10s %-6s %s\n",
			"Interface", "IP Address", "S/L", "MAC", "VRF", "MTU", "Description"));
		output.append(String.format("%-16s %-33s %-4s %-17s %-10s %-6s %s\n",
			"---------", "----------", "---", "---", "---", "---", "-----------"));

		for (RouterInterface iface : router.getInterfaces()) {
			String interfaceName = iface.getInterfaceName();
			String ipAddress = iface.getInterfaceAddress() != null
				? iface.getInterfaceAddress().toString()
				: "-";
			String state = iface.getStatus().toString();
			String macAddress = iface.getMacAddress() != null
				? iface.getMacAddress().toString()
				: "-";
			String vrf = iface.getVrf() != null ? iface.getVrf() : "default";
			String mtu = String.valueOf(iface.getMtu());
			String description = iface.getDescription() != null ? iface.getDescription() : "";

			output.append(String.format("%-16s %-33s %-4s %-17s %-10s %-6s %s\n",
				interfaceName,
				ipAddress,
				state,
				macAddress,
				vrf,
				mtu,
				description));
		}

		System.out.print(output.toString());
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

