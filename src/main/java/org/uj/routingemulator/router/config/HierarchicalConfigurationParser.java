package org.uj.routingemulator.router.config;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.*;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HierarchicalConfigurationParser implements ConfigurationParser {
	private static final String DISABLE_COMMAND = "disable";
	private List<String> lines;
	private int position;
	private final RouterConfigurationService service = new RouterConfigurationService();

	private void applyRouteConfiguration(Router router, String nextHop, Subnet subnet, int distance, String interfaceName, boolean disabled) {
		if (addRoute(router, nextHop, subnet, distance, interfaceName)) return;
		if (disabled) {
			for (StaticRoutingEntry entry : router.getConfigSession().getStagedRoutingTable().getRoutingEntries()) {
				if (entry.getSubnet().equals(subnet)) {
					disableRoute(router, entry);
					return;
				}
			}
		}
	}

	private List<String> preprocessConfig(String config) {
		List<String> result = new ArrayList<>();
		for (String line : config.split("\n")) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			result.add(line);
		}
		return result;
	}

	private boolean addRoute(Router router, String nextHop, Subnet subnet, int distance, String interfaceName) {
		if (nextHop != null) {
			return addNextHopRoute(router, nextHop, subnet, distance);
		} else if (interfaceName != null) {
			RouterInterface iface = router.findFromName(interfaceName);
			if (iface == null) {
				throw new ConfigurationParseException(
						String.format("Interface %s does not exist on this router", interfaceName));
			}
			return addInterfaceRoute(router, subnet, iface, distance);
		}
		return false;
	}

	private boolean addInterfaceRoute(Router router, Subnet subnet, RouterInterface iface, int distance) {
		try {
			service.addRoute(router, new StaticRoutingEntry(subnet, iface, distance));
		} catch (DuplicateConfigurationException e) {
			return true;
		}
		return false;
	}

	private void disableRoute(Router router, StaticRoutingEntry entry) {
		service.disableRoute(router, entry);
	}

	private boolean addNextHopRoute(Router router, String nextHop, Subnet subnet, int distance) {
		IPAddress nextHopAddress = IPAddress.fromString(nextHop);
		try {
			service.addRoute(router, new StaticRoutingEntry(subnet, nextHopAddress, distance));
		} catch (DuplicateConfigurationException e) {
			return true;
		}
		return false;
	}

	private void disableInterface(Router router, String interfaceName) {
		service.disableInterface(router, interfaceName);
	}

	private void configureInterface(Router router, String address, String interfaceName) {
		try {
			String[] parts = address.split("/");
			IPAddress ip = IPAddress.fromString(parts[0]);
			SubnetMask mask = SubnetMask.fromString(parts[1]);
			InterfaceAddress interfaceAddress = new InterfaceAddress(ip, mask);
			service.configureInterface(router, interfaceName, interfaceAddress);
		} catch (RuntimeException e) {
			throw new ConfigurationParseException("Invalid interface address: " + e.getMessage());
		}
	}

	@Override
	public void loadConfiguration(Router router, String config) {
		this.lines = preprocessConfig(config);
		this.position = 0;
		RouterMode originalMode = router.getMode();
		RouterModeController.setModeForced(router, RouterMode.CONFIGURATION);
		try {
			router.getConfigSession().resetCandidateConfiguration();
			parseConfiguration(router, new ArrayList<>());
			router.getConfigSession().commit();
		} catch (RuntimeException e) {
			router.getConfigSession().discard();
			throw e;
		} finally {
			RouterModeController.setModeForced(router, originalMode);
		}
	}

	private void parseConfiguration(Router router, List<String> path) {
		while (position < lines.size()) {
			String line = lines.get(position);
			String trimmed = line.trim();
			if (trimmed.equals("}")) {
				position++;
				return;
			}
			if (trimmed.endsWith("{")) {
				String[] parts = trimmed.substring(0, trimmed.length() - 1).trim().split("\\s+");
				List<String> newPath = new ArrayList<>(path);
				Collections.addAll(newPath, parts);
				position++;
				if (path.size() >= 2 && path.get(0).equals("protocols") && path.get(1).equals("static") && parts[0].equals("route")) {
					parseRouteBlock(router, newPath);
				} else {
					parseConfiguration(router, newPath);
				}
			} else {
				String[] parts = trimmed.split("\\s+");
				List<String> fullPath = new ArrayList<>(path);
				Collections.addAll(fullPath, parts);
				applyConfiguration(router, fullPath);
				position++;
			}
		}
	}

	private void parseRouteBlock(Router router, List<String> path) {
		String destination = path.get(3);
		String nextHop = null;
		String interfaceName = null;
		int distance = 1;
		boolean disabled = false;
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
					case DISABLE_COMMAND:
						disabled = true;
						break;
					default:
						throw new ConfigurationParseException("Unknown route configuration option: " + parts[0]);
				}
			} else if (parts.length == 1 && parts[0].equals(DISABLE_COMMAND)) {
				disabled = true;
			}
			position++;
		}
		try {
			Subnet subnet = Subnet.fromString(destination);
			applyRouteConfiguration(router, nextHop, subnet, distance, interfaceName, disabled);
		} catch (ConfigurationParseException e) {
			throw new ConfigurationParseException("Error parsing route: " + e.getMessage());
		}
	}

	private void applyConfiguration(Router router, List<String> path) {
		if (path.size() < 2) {
			return;
		}
		try {
			if (path.get(0).equals("interfaces") && path.size() >= 4 && path.get(1).equals("ethernet")) {
				String interfaceName = path.get(2);
				RouterInterface iface = router.findFromName(interfaceName);
				if (iface == null) {
					throw new ConfigurationParseException(
							String.format("Interface %s does not exist on this router", interfaceName));
				}
				if (path.get(3).equals("address") && path.size() == 5) {
					String address = path.get(4);
					if (address.equals("dhcp")) {
						return;
					}
					configureInterface(router, address, interfaceName);
				} else if (path.get(3).equals(DISABLE_COMMAND) && path.size() == 4) {
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