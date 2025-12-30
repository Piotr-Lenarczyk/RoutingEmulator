package org.uj.routingemulator.router.config;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.StaticRoutingEntry;

/**
 * Generates VyOS hierarchical configuration format (output from "show configuration").
 * <p>
 * Example output:
 * <pre>
 * interfaces {
 *     ethernet eth0 {
 *         address 192.168.1.1/24
 *     }
 * }
 * </pre>
 */
public class HierarchicalConfigurationGenerator implements ConfigurationGenerator {

	@Override
	public String generateConfiguration(Router router) {
		StringBuilder config = new StringBuilder();

		// Generate interfaces block
		if (router.getInterfaces().stream().anyMatch(i -> i.getInterfaceAddress() != null || i.isDisabled())) {
			config.append("interfaces {\n");
			for (RouterInterface iface : router.getInterfaces()) {
				if (iface.getInterfaceAddress() != null || iface.isDisabled()) {
					config.append("    ethernet ").append(iface.getInterfaceName()).append(" {\n");
					if (iface.getInterfaceAddress() != null) {
						config.append("        address ").append(iface.getInterfaceAddress()).append("\n");
					}
					if (iface.isDisabled()) {
						config.append("        disable\n");
					}
					config.append("    }\n");
				}
			}
			config.append("}\n");
		}

		// Generate protocols block
		if (!router.getRoutingTable().getRoutingEntries().isEmpty()) {
			config.append("protocols {\n");
			config.append("    static {\n");
			for (StaticRoutingEntry entry : router.getRoutingTable().getRoutingEntries()) {
				config.append("        route ").append(entry.getSubnet()).append(" {\n");

				if (entry.getNextHop() != null) {
					config.append("            next-hop ").append(entry.getNextHop()).append("\n");
				} else if (entry.getRouterInterface() != null) {
					config.append("            interface ").append(entry.getRouterInterface().getInterfaceName()).append("\n");
				}

				if (entry.getAdministrativeDistance() != 1) {
					config.append("            distance ").append(entry.getAdministrativeDistance()).append("\n");
				}

				if (entry.isDisabled()) {
					config.append("            disable\n");
				}

				config.append("        }\n");
			}
			config.append("    }\n");
			config.append("}\n");
		}

		return config.toString();
	}
}

