package org.uj.routingemulator.router.model;

import java.util.ArrayList;
import java.util.List;

public class RoutingTableCopier {
	private RoutingTableCopier() {
	}

	public static List<RouterInterface> deepCopyInterfaces(List<RouterInterface> interfaces) {
		List<RouterInterface> copy = new ArrayList<>();
		for (RouterInterface iface : interfaces) {
			copy.add(new RouterInterface(iface));
		}
		return copy;
	}

	public static RoutingTable copyRoutingTableWithUpdatedInterfaces(RoutingTable routingTable, List<RouterInterface> newInterfaces) {
		RoutingTable newTable = new RoutingTable();
		for (StaticRoutingEntry entry : routingTable.getRoutingEntries()) {
			StaticRoutingEntry newEntry;
			if (entry.getRouterInterface() != null) {
				String interfaceName = entry.getRouterInterface().getInterfaceName();
				RouterInterface newInterface = newInterfaces.stream()
						.filter(intf -> intf.getInterfaceName().equals(interfaceName))
						.findFirst()
						.orElse(null);

				if (entry.getAdministrativeDistance() == 1) {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface);
				} else {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface, entry.getAdministrativeDistance());
				}
			} else {
				if (entry.getAdministrativeDistance() == 1) {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop());
				} else {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop(), entry.getAdministrativeDistance());
				}
			}

			if (entry.isDisabled()) {
				newEntry.disable();
			}
			newTable.addRoute(newEntry);
		}
		return newTable;
	}
}