package org.uj.routingemulator.router.cli.ethernet;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.cli.CLIContext;
import org.uj.routingemulator.router.cli.CLIErrorHandler;
import org.uj.routingemulator.router.cli.RouterCommand;

import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to remove an Ethernet VIF, an IP address configuration, or the
 * administrative disable node.
 */
public class DeleteInterfaceEthernetCommand implements RouterCommand {

	private static final Pattern PATTERN = Pattern.compile(
			"delete\\s+interfaces\\s+ethernet\\s+(\\S+)(?:\\s+vif\\s+(\\d+))?(?:\\s+(?:address\\s+(\\S+)|(disable)))?"
	);

	private String ethernetName;
	private String vifId;
	private String routerInterfaceName;
	private String address;
	private boolean isDisable;
	private boolean isVifDelete;

	@Override
	public void execute(Router router) {
		PrintWriter out = CLIContext.getWriter();
		try {
			if (isVifDelete) {
				router.deleteVif(routerInterfaceName);
			} else if (isDisable) {
				router.enableInterface(routerInterfaceName);
			} else {
				router.deleteInterfaceAddress(routerInterfaceName);
			}
			out.println("[edit]");
			out.flush();
		} catch (RuntimeException e) {
			String errorPath;
			if (isVifDelete) {
				errorPath = "delete interfaces ethernet " + ethernetName + " vif " + vifId;
			} else if (isDisable) {
				errorPath = "delete interfaces ethernet " + routerInterfaceName + " disable";
			} else {
				errorPath = CLIErrorHandler.formatDeleteInterfaceEthernet(routerInterfaceName, address);
			}

			throw CLIErrorHandler.handleInterfaceException(e, errorPath);
		}
	}

	@Override
	public boolean matches(String command) {
		Matcher matcher = PATTERN.matcher(command.trim());
		if (matcher.matches()) {
			ethernetName = matcher.group(1);
			vifId = matcher.group(2);
			routerInterfaceName = vifId != null ? ethernetName + "." + vifId : ethernetName;

			address = matcher.group(3);
			isDisable = matcher.group(4) != null;
			isVifDelete = vifId != null && address == null && !isDisable;
			return address != null || isDisable || isVifDelete;
		}
		return false;
	}

	@Override
	public String getCommandPattern() {
		return "delete interfaces ethernet <interface> [vif <id> [address <ip> | disable] | address <ip> | disable]";
	}

	@Override
	public String getDescription() {
		return "Remove Ethernet VIF or interface configuration line";
	}
}