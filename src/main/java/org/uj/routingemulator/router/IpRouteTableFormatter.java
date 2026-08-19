package org.uj.routingemulator.router;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for formatting the routing table into a VyOS-style text display.
 */
public class IpRouteTableFormatter {

	private IpRouteTableFormatter() {
	}

	/**
	 * Formats the router's routing table and connected interfaces into a display string.
	 */
	public static String format(Router router) {
		StringBuilder output = new StringBuilder();

		output.append("Codes: K - kernel route, C - connected, S - static, R - RIP,\n");
		output.append("       O - OSPF, I - IS-IS, B - BGP, E - EIGRP, N - NHRP,\n");
		output.append("       T - Table, v - VNC, V - VNC-Direct, A - Babel, F - PBR,\n");
		output.append("       f - OpenFabric,\n");
		output.append("       > - selected route, * - FIB route, q - queued, r - rejected, b - backup\n");
		output.append("       t - trapped, o - offload failure\n\n");

		List<RouteDisplayEntry> displayEntries = getRouteDisplayEntries(router);

		for (RouteDisplayEntry entry : displayEntries) {
			if (entry.isDisabled()) {
				continue;
			}

			parseRouteFormatting(entry, output);
		}
		return output.toString();
	}

	private static void parseRouteFormatting(RouteDisplayEntry entry, StringBuilder output) {
		String prefix = entry.isConnected() ? "C>*" : "S>*";
		output.append(prefix).append(" ");
		output.append(entry.subnet().networkAddress()).append("/");
		output.append(entry.subnet().subnetMask().shortMask());

		if (entry.isConnected()) {
			output.append(" is directly connected, ").append(entry.interfaceName());
		} else {
			output.append(" [").append(entry.distance()).append("]");
			if (entry.nextHop() != null) {
				output.append(" via ").append(entry.nextHop());
				if (entry.interfaceName() != null) {
					output.append(", ").append(entry.interfaceName());
				}
			} else if (entry.interfaceName() != null) {
				output.append(" via ").append(entry.interfaceName());
			}
		}
		output.append("\n");
	}

	private static List<RouteDisplayEntry> getRouteDisplayEntries(Router router) {
		List<RouteDisplayEntry> displayEntries = new ArrayList<>();

		for (RouterInterface iface : router.getInterfaces()) {
			if (iface.getSubnet() != null && iface.getStatus() != null && iface.getStatus().getAdmin() == AdminState.UP) {
				displayEntries.add(new RouteDisplayEntry(
						"C", iface.getSubnet(), null, iface.getInterfaceName(), 0, false, true));
			}
		}

		for (StaticRoutingEntry entry : router.getRoutingTable().getRoutingEntries()) {
			displayEntries.add(new RouteDisplayEntry(
					"S", entry.getSubnet(), entry.getNextHop(),
					entry.getRouterInterface() != null ? entry.getRouterInterface().getInterfaceName() : null,
					entry.getAdministrativeDistance(), entry.isDisabled(), false));
		}

		displayEntries.sort((a, b) -> {
			int addrCompare = a.subnet().networkAddress().toString()
					.compareTo(b.subnet().networkAddress().toString());
			if (addrCompare != 0) return addrCompare;

			return Integer.compare(
					b.subnet().subnetMask().shortMask(),
					a.subnet().subnetMask().shortMask()
			);
		});

		return displayEntries;
	}
}