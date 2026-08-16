package org.uj.routingemulator.router.config;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses VyOS hierarchical configuration format (output from "show configuration").
 * <p>
 * This parser handles the curly-brace format used by VyOS:
 * <pre>
 * interfaces {
 *     ethernet eth0 {
 *         address 192.168.1.1/24
 *     }
 * }
 * </pre>
 */
public class HierarchicalConfigurationParser implements ConfigurationParser {

	private List<String> lines;
	private int position;

	/**
	 * Loads and applies hierarchical configuration to the specified router.
	 * <p>
	 * The method:
	 * <ul>
	 *   <li>Preprocesses the configuration (removes comments, empty lines)</li>
	 *   <li>Puts router in configuration mode</li>
	 *   <li>Clears existing staged configuration</li>
	 *   <li>Recursively parses configuration blocks</li>
	 *   <li>Commits changes on success</li>
	 *   <li>Rolls back on error</li>
	 *   <li>Restores original router mode</li>
	 * </ul>
	 *
	 * @param router the router to configure
	 * @param config the configuration text in hierarchical format
	 * @throws ConfigurationParseException if the configuration is invalid
	 */
	@Override
	public void loadConfiguration(Router router, String config) {
		this.lines = preprocessConfig(config);
		this.position = 0;

		RouterMode originalMode = router.getMode();
		router.setMode(RouterMode.CONFIGURATION);

		try {
			router.clearStagedConfiguration();
			parseConfiguration(router, new ArrayList<>());
			router.commitChanges();
		} catch (RuntimeException e) {
			router.discardChanges();
			throw e;
		} finally {
			router.setMode(originalMode);
		}
	}

	/**
	 * Preprocesses configuration by removing comments and empty lines.
	 *
	 * @param config the raw configuration text
	 * @return list of non-empty, non-comment lines
	 */
	private List<String> preprocessConfig(String config) {
		List<String> result = new ArrayList<>();
		for (String line : config.split("\n")) {
			String trimmed = line.trim();
			// Skip empty lines and comments
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			result.add(line);
		}
		return result;
	}

	/**
	 * Recursively parses configuration blocks.
	 *
	 * @param router the router to configure
	 * @param path current configuration path (e.g., ["interfaces", "ethernet", "eth0"])
	 */
	private void parseConfiguration(Router router, List<String> path) {
		while (position < lines.size()) {
			String line = lines.get(position);
			String trimmed = line.trim();

			// End of block
			if (trimmed.equals("}")) {
				position++;
				return;
			}

			// Parse line
			if (trimmed.endsWith("{")) {
				// Start of new block
				String[] parts = trimmed.substring(0, trimmed.length() - 1).trim().split("\\s+");
				List<String> newPath = new ArrayList<>(path);
				Collections.addAll(newPath, parts);
				position++;

				// Special case: route block - collect all values and apply once
				if (path.size() >= 2 && path.get(0).equals("protocols") && path.get(1).equals("static") && parts[0].equals("route")) {
					parseRouteBlock(router, newPath);
				} else {
					parseConfiguration(router, newPath);
				}
			} else {
				// Single configuration line
				String[] parts = trimmed.split("\\s+");
				List<String> fullPath = new ArrayList<>(path);
				Collections.addAll(fullPath, parts);
				applyConfiguration(router, fullPath);
				position++;
			}
		}
	}

	private static void applyRouteConfiguration(Router router, String nextHop, Subnet subnet, int distance, String interfaceName, boolean disabled) {
		if (addRoute(router, nextHop, subnet, distance, interfaceName)) return;

		if (disabled) {
			// Disable the route we just added
			for (StaticRoutingEntry entry : router.getStagedRoutingTable().getRoutingEntries()) {
				if (entry.getSubnet().equals(subnet)) {
					disableRoute(router, entry);
					return;
				}
			}
		}
	}

	private static boolean addRoute(Router router, String nextHop, Subnet subnet, int distance, String interfaceName) {
		if (nextHop != null) {
			return addNextHopRoute(router, nextHop, subnet, distance);
		} else if (interfaceName != null) {
			RouterInterface iface = router.findFromName(interfaceName);
			if (iface == null) {
				throw new ConfigurationParseException(
						String.format("Interface %s does not exist on this router", interfaceName)
				);
			}
			return addInterfaceRoute(router, subnet, iface, distance);
		}
		return false;
	}

	private static boolean addInterfaceRoute(Router router, Subnet subnet, RouterInterface iface, int distance) {
		try {
			router.addRoute(new StaticRoutingEntry(subnet, iface, distance));
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
				return true;
			}
			throw e;
		}
		return false;
	}

	private static void disableRoute(Router router, StaticRoutingEntry entry) {
		try {
			router.disableRoute(entry);
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().contains("already exists")) {
				return;
			}
			throw e;
		}
	}

	private static boolean addNextHopRoute(Router router, String nextHop, Subnet subnet, int distance) {
		IPAddress nextHopAddress = IPAddress.fromString(nextHop);
		try {
			router.addRoute(new StaticRoutingEntry(subnet, nextHopAddress, distance));
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
				return true;
			}
			throw e;
		}
		return false;
	}

	private static void disableInterface(Router router, String interfaceName) {
		try {
			router.disableInterface(interfaceName);
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().contains("already exists")) {
				return;
			}
			throw e;
		}
	}

	private static void configureInterface(Router router, String address, String interfaceName) {
		try {
			String[] parts = address.split("/");
			IPAddress ip = IPAddress.fromString(parts[0]);
			SubnetMask mask = SubnetMask.fromString(parts[1]);
			InterfaceAddress interfaceAddress = new InterfaceAddress(ip, mask);
			router.configureInterface(interfaceName, interfaceAddress);
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().equals("Configuration already exists")) {
				return;
			}
			throw new ConfigurationParseException("Invalid interface address: " + e.getMessage());
		}
	}

	/**
	 * Parses a complete route block and collects all configuration values.
	 * <p>
	 * Route blocks in hierarchical format contain multiple configuration lines
	 * (next-hop, interface, distance, disable). This method collects all values
	 * before creating the routing entry.
	 *
	 * @param router the router to configure
	 * @param path current path including route destination (e.g., ["protocols", "static", "route", "192.168.1.0/24"])
	 * @throws ConfigurationParseException if route configuration is invalid
	 */
	private void parseRouteBlock(Router router, List<String> path) {
		// path is: ["protocols", "static", "route", "192.168.1.0/24"]
		String destination = path.get(3);

		String nextHop = null;
		String interfaceName = null;
		int distance = 1;
		boolean disabled = false;

		// Parse route block content
		while (position < lines.size()) {
			String line = lines.get(position);
			String trimmed = line.trim();

			if (trimmed.equals("}")) {
				position++;
				break;
			}

			String[] parts = trimmed.split("\\s+");

			if (parts.length >= 2) {
				switch (parts[0]) {
					case "next-hop":
						nextHop = parts[1];
						break;
					case "interface":
						interfaceName = parts[1];
						break;
					case "distance":
						try {
							distance = Integer.parseInt(parts[1]);
						} catch (NumberFormatException e) {
							throw new ConfigurationParseException("Invalid distance value: " + parts[1]);
						}
						break;
					case "disable":
						disabled = true;
						break;
					default:
						throw new ConfigurationParseException("Unknown route configuration option: " + parts[0]);
				}

			}

			position++;
		}

		// Apply route configuration
		try {
			Subnet subnet = Subnet.fromString(destination);

			applyRouteConfiguration(router, nextHop, subnet, distance, interfaceName, disabled);
		} catch (ConfigurationParseException e) {
			throw new ConfigurationParseException("Error parsing route: " + e.getMessage());
		}
	}

	/**
	 * Applies a single configuration path to the router.
	 * <p>
	 * Used for interface configuration (routes are handled by parseRouteBlock).
	 * Handles configuration lines that are not part of a nested block structure.
	 *
	 * @param router the router to configure
	 * @param path the full configuration path as a list of tokens
	 * @throws ConfigurationParseException if configuration cannot be applied
	 */
	private void applyConfiguration(Router router, List<String> path) {
		if (path.size() < 2) {
			return; // Invalid path
		}

		try {
			// interfaces ethernet eth0 address 192.168.1.1/24
			if (path.get(0).equals("interfaces") && path.size() >= 4 && path.get(1).equals("ethernet")) {
					String interfaceName = path.get(2);

					// Check if interface exists
					RouterInterface iface = router.findFromName(interfaceName);
					if (iface == null) {
						throw new ConfigurationParseException(
							String.format("Interface %s does not exist on this router", interfaceName)
						);
					}

					if (path.get(3).equals("address") && path.size() == 5) {
						String address = path.get(4);
						// Skip dhcp addresses
						if (address.equals("dhcp")) {
							return;
						}
						configureInterface(router, address, interfaceName);
					} else if (path.get(3).equals("disable") && path.size() == 4) {
						disableInterface(router, interfaceName);
					}

			}
		} catch (ConfigurationParseException e) {
			throw e;
		} catch (Exception e) {
			throw new ConfigurationParseException("Error parsing configuration: " + e.getMessage());
		}
	}
}

