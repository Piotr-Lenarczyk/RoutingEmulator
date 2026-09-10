package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.InterfaceType;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.StaticRoutingEntry;

/**
 * Generates VyOS-style router configuration as a series of 'set' commands.
 * <p>
 * The generated configuration includes:
 * <ul>
 *   <li>Interface addresses and disabled state</li>
 *   <li>Static routes with next-hop, interface, and distance</li>
 * </ul>
 */
public class CommandConfigurationGenerator implements ConfigurationGenerator {
	/**
	 * Generates configuration commands for the specified router.
	 * <p>
	 * The output format matches VyOS CLI syntax and can be parsed back
	 * by {@link ConfigurationParser}.
	 *
	 * @param router the router to generate configuration for
	 * @return configuration as VyOS-style 'set' commands, one per line
	 */
	@Override
	public String generateConfiguration(Router router) {
		StringBuilder configBuilder = new StringBuilder();

		for (RouterInterface iface: router.getInterfaces()) {
			String name = iface.getInterfaceName();
			String prefix;

			if (iface.getType() == InterfaceType.DUMMY) {
				prefix = "set interfaces dummy " + name;
			} else if (iface.getType() == InterfaceType.VIF) {
				String[] parts = name.split("\\.");
				prefix = "set interfaces ethernet " + parts[0] + " vif " + parts[1];
			} else {
				prefix = "set interfaces ethernet " + name;
			}

			if (iface.getInterfaceAddress() != null) {
				configBuilder.append(String.format("%s address %s%n", prefix, iface.getInterfaceAddress()));
			}
			if (iface.isDisabled()) {
				configBuilder.append(String.format("%s disable%n", prefix));
			}
		}

		for (StaticRoutingEntry entry: router.getRoutingTable().getRoutingEntries()) {
			StringBuilder route = new StringBuilder();
			route.append("set protocols static route ")
					.append(entry.getSubnet().toString());

			if (entry.getNextHop() != null) {
				route.append(" next-hop ").append(entry.getNextHop());
			} else if (entry.getRouterInterface() != null) {
				route.append(" interface ").append(entry.getRouterInterface().getInterfaceName());
			}

			if (entry.getAdministrativeDistance() != 1) {
				route.append(" distance ").append(entry.getAdministrativeDistance());
			}

			configBuilder.append(route).append("\n");

			if (entry.isDisabled()) {
				configBuilder.append("set protocols static route ").append(entry.getSubnet()).append(" disable\n");
			}
		}

		return configBuilder.toString();
	}
}
