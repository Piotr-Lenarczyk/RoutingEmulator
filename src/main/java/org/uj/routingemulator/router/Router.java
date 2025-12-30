package org.uj.routingemulator.router;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.exceptions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a network router with VyOS-style configuration management.
 *
 * <p>The router supports two operational modes:
 * <ul>
 *   <li><b>OPERATIONAL</b> - Read-only mode for viewing configuration and status</li>
 *   <li><b>CONFIGURATION</b> - Edit mode allowing configuration changes</li>
 * </ul>
 *
 * <p>Configuration changes are staged and must be committed to take effect.
 * The router tracks uncommitted changes and prevents exiting configuration mode
 * until changes are either committed or discarded.</p>
 */
@Getter
@Setter
@EqualsAndHashCode
public class Router {
	private String name;
	private RoutingTable routingTable;
	private List<RouterInterface> interfaces;
	private RouterMode mode;
	private RoutingTable stagedRoutingTable;
	private List<RouterInterface> stagedInterfaces;
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private boolean hasUncommittedChanges = false;

	/**
	 * Constructs a router with default configuration in VyOS.
	 * Default router configuration includes 2 interfaces: eth0 and lo
	 * @param name Name of the router
	 */
	public Router(String name) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;
		this.stagedRoutingTable = new RoutingTable(this.routingTable);
		this.stagedInterfaces = deepCopyInterfaces(this.interfaces);
	}

	/**`
	 * Constructs a router with specific interfaces.
	 * @param name Name of the router
	 * @param interfaces List of interfaces to be added to the router
	 */
	public Router(String name, List<RouterInterface> interfaces) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = interfaces;
		this.mode = RouterMode.OPERATIONAL;
		this.stagedRoutingTable = new RoutingTable();
		this.stagedInterfaces = new ArrayList<>(interfaces);
	}

	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	public void setUncommittedChanges(boolean hasUncommittedChanges) {
		this.hasUncommittedChanges = hasUncommittedChanges;
	}

	/**
	 * Adds a new static route to the routing table.
	 * @param entry Static routing entry to be added
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws DuplicateConfigurationException if route already exists
	 */
	public void addRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: set [protocols]");
		}
		if (stagedRoutingTable.contains(entry)) {
			throw new DuplicateConfigurationException("Route already exists");
		}
		this.stagedRoutingTable.addRoute(entry);
		hasUncommittedChanges = true;
	}

	/**
	 * Removes a static route from the routing table.
	 * @param entry Static routing entry to be removed
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws ConfigurationNotFoundException if route doesn't exist
	 */
	public void removeRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: delete [protocols]");
		}
		if (!stagedRoutingTable.getRoutingEntries().contains(entry)) {
			throw new ConfigurationNotFoundException("Nothing to delete");
		}
		this.stagedRoutingTable.getRoutingEntries().remove(entry);
		hasUncommittedChanges = true;
	}

	/**
	 * Disables an existing static route in the staged configuration.
	 * If the route does not exist -> throws "Route not found".
	 * If the route is already disabled -> throws "Route already exists" (duplicate configuration).
	 *
	 * @param entry routing entry to disable (matches by subnet/nextHop/interface/distance)
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws ConfigurationNotFoundException if route doesn't exist
	 * @throws DuplicateConfigurationException if route is already disabled
	 */
	public void disableRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: set [protocols]");
		}
		// Find existing entry by equality (equals ignores isDisabled)
		List<StaticRoutingEntry> entries = stagedRoutingTable.getRoutingEntries();
		int idx = entries.indexOf(entry);
		if (idx == -1) {
			throw new ConfigurationNotFoundException("Route not found");
		}
		StaticRoutingEntry existing = entries.get(idx);
		if (existing.isDisabled()) {
			// disabling an already-disabled route is a duplicate configuration
			throw new DuplicateConfigurationException("Route already exists");
		}
		existing.disable();
		hasUncommittedChanges = true;
	}

	/**
	 * Configures a router interface with an IP address in the staged configuration.
	 * @param routerInterfaceName Name of the router interface to be configured
	 * @param interfaceAddress IP address and mask to be assigned to the interface
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws InvalidAddressException if the address is invalid (network/broadcast address)
	 * @throws InterfaceNotFoundException if the interface doesn't exist
	 * @throws DuplicateConfigurationException if the interface already has this address
	 */
	public void configureInterface(String routerInterfaceName, InterfaceAddress interfaceAddress) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		// Provide validation with concise error messages
		if (interfaceAddress.isNetworkAddress()) {
			throw new InvalidAddressException(
				String.format("Cannot assign network address %s to the interface. Use a host address instead",
					interfaceAddress)
			);
		}

		if (interfaceAddress.isBroadcastAddress()) {
			throw new InvalidAddressException(
				String.format("Cannot assign broadcast address %s to the interface. Use a host address instead",
					interfaceAddress)
			);
		}

		if (!interfaceAddress.isValidHostAddress()) {
			throw new InvalidAddressException(interfaceAddress + " is not a valid host IP address");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		if (routerInterface.getInterfaceAddress() == null || !routerInterface.getInterfaceAddress().equals(interfaceAddress)) {
			routerInterface.setInterfaceAddress(interfaceAddress);
			hasUncommittedChanges = true;
		} else {
			throw new DuplicateConfigurationException("Configuration already exists");
		}
	}

	/**
	 * Disables a router interface in the staged configuration.
	 * @param routerInterfaceName Name of the router interface to be disabled
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws InterfaceNotFoundException if the interface doesn't exist
	 */
	public void disableInterface(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		routerInterface.disable();
		hasUncommittedChanges = true;
	}

	/**
	 * Removes an address from a router interface in the staged configuration.
	 * @param routerInterfaceName Name of the router interface
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws InterfaceNotFoundException if the interface doesn't exist
	 * @throws ConfigurationNotFoundException if the interface has no address to delete
	 */
	public void deleteInterfaceAddress(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: delete [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		if (routerInterface.getInterfaceAddress() == null) {
			throw new ConfigurationNotFoundException("No value to delete");
		}

		routerInterface.setInterfaceAddress(null);
		hasUncommittedChanges = true;
	}

	/**
	 * Commits configuration changes. Takes place immediately but is not persisted meaning if the device restarts/shuts down, changes will not be saved.
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 * @throws NoChangesToCommitException if there are no changes to commit
	 */
	public void commitChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: [commit]");
		}
		if (!hasUncommittedChanges) {
			throw new NoChangesToCommitException("No configuration changes to commit");
		}
		this.interfaces = deepCopyInterfaces(stagedInterfaces);
		this.routingTable = copyRoutingTableWithUpdatedInterfaces(stagedRoutingTable, stagedInterfaces, interfaces);
		hasUncommittedChanges = false;
	}

	/**
	 * Discard configuration changes and restores the last committed state.
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 */
	public void discardChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: [discard]");
		}
		this.stagedInterfaces = deepCopyInterfaces(interfaces);
		this.stagedRoutingTable = copyRoutingTableWithUpdatedInterfaces(routingTable, interfaces, stagedInterfaces);
		hasUncommittedChanges = false;
	}

	/**
	 * Clears all staged configuration, removing all interface addresses and routing entries.
	 * This is typically used before loading configuration from a file to ensure a clean slate.
	 * Must be in CONFIGURATION mode to use this method.
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 */
	public void clearStagedConfiguration() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Cannot clear configuration in operational mode");
		}

		// Clear all interface addresses and reset to enabled state
		for (RouterInterface iface : stagedInterfaces) {
			iface.setInterfaceAddress(null);
			// Only enable if administratively disabled (don't care about link state)
			if (iface.getStatus().getAdmin() == AdminState.ADMIN_DOWN) {
				iface.enable();
			}
		}

		// Clear routing table
		this.stagedRoutingTable = new RoutingTable();

		hasUncommittedChanges = true;
	}

	/**
	 * Saves configuration to persistent storage, ensuring it will stay after a reboot.
	 * This operation is currently not supported.
	 * @throws UnsupportedOperationException always thrown as this feature is not implemented
	 */
	public void saveConfiguration() {
		throw new UnsupportedOperationException("Saving configuration is not supported.");
		//if (mode != RouterMode.CONFIGURATION) {
		//	throw new InvalidModeException("Invalid command: [save]");
		//}
		//if (hasUncommittedChanges) {
		//	throw new UncommittedChangesException("Cannot save configuration with uncommitted changes.");
		//}
		//System.out.println("Configuration saved.");
	}

	/**
	 * Changes router mode. In case of leaving configuration mode, checks for uncommitted changes. Uncommitted changes
	 * will prevent exiting configuration mode.
	 * When entering configuration mode, resets staged configuration to current committed state.
	 * @param mode Target router mode
	 * @throws UncommittedChangesException if trying to exit configuration mode with uncommitted changes
	 */
	public void setMode(RouterMode mode) {
		if (this.mode == RouterMode.CONFIGURATION && hasUncommittedChanges) {
			throw new UncommittedChangesException("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]");
		}
		// When entering configuration mode, reset staged configuration to current committed state
		if (mode == RouterMode.CONFIGURATION && this.mode == RouterMode.OPERATIONAL) {
			this.stagedInterfaces = deepCopyInterfaces(this.interfaces);
			this.stagedRoutingTable = copyRoutingTableWithUpdatedInterfaces(this.routingTable, this.interfaces, this.stagedInterfaces);
		}
		this.mode = mode;
	}

	/**
	 * Changes router mode. In case of leaving configuration mode, forces discarding uncommitted changes.
	 * @param mode Target router mode
	 */
	public void setModeForced(RouterMode mode) {
		if (this.mode == RouterMode.CONFIGURATION && hasUncommittedChanges) {
			discardChanges();
		}
		this.mode = mode;
	}

	/**
	 * Resets the router to default configuration as defined in the first constructor.
	 * This includes resetting routing table, interfaces (eth0 and lo), mode, and discarding any uncommitted changes.
	 */
	public void reset() {
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;
		this.stagedRoutingTable = new RoutingTable(this.routingTable);
		this.stagedInterfaces = deepCopyInterfaces(this.interfaces);
		this.hasUncommittedChanges = false;
	}

	/**
	 * Finds a router interface by name.
	 * Returns interface from staged configuration if in CONFIGURATION mode,
	 * otherwise from committed configuration.
	 *
	 * @param interfaceName Name of the interface to find
	 * @return RouterInterface object if found, null otherwise
	 */
	public RouterInterface findFromName(String interfaceName) {
		List<RouterInterface> interfaceList = (mode == RouterMode.CONFIGURATION) ? stagedInterfaces : interfaces;
		return interfaceList.stream()
				.filter(intf -> intf.getInterfaceName().equals(interfaceName))
				.findFirst()
				.orElse(null);
	}

	/**
	/**
	 * Creates a deep copy of interface list.
	 *
	 * @param interfaces List of interfaces to copy
	 * @return A new list containing copies of all interfaces
	 */
	private List<RouterInterface> deepCopyInterfaces(List<RouterInterface> interfaces) {
		List<RouterInterface> copy = new ArrayList<>();
		for (RouterInterface iface : interfaces) {
			copy.add(new RouterInterface(iface));
		}
		return copy;
	}

	/**
	 * Copies a routing table while updating RouterInterface references.
	 * Maps interface references from oldInterfaces to corresponding interfaces in newInterfaces.
	 *
	 * @param routingTable Original routing table
	 * @param oldInterfaces Original interface list
	 * @param newInterfaces New interface list to map to
	 * @return New routing table with updated interface references
	 */
	private RoutingTable copyRoutingTableWithUpdatedInterfaces(
			RoutingTable routingTable,
			List<RouterInterface> oldInterfaces,
			List<RouterInterface> newInterfaces) {

		RoutingTable newTable = new RoutingTable();
		for (StaticRoutingEntry entry : routingTable.getRoutingEntries()) {
			StaticRoutingEntry newEntry;
			if (entry.getRouterInterface() != null) {
				// Find corresponding interface in newInterfaces
				String interfaceName = entry.getRouterInterface().getInterfaceName();
				RouterInterface newInterface = newInterfaces.stream()
						.filter(intf -> intf.getInterfaceName().equals(interfaceName))
						.findFirst()
						.orElse(null);

				// Create new entry with updated interface reference
				if (entry.getAdministrativeDistance() == 1) {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface);
				} else {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface, entry.getAdministrativeDistance());
				}
			} else {
				// Next-hop route - just copy
				if (entry.getAdministrativeDistance() == 1) {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop());
				} else {
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop(), entry.getAdministrativeDistance());
				}
			}
			// Preserve disabled state
			if (entry.isDisabled()) {
				newEntry.disable();
			}
			newTable.addRoute(newEntry);
		}
		return newTable;
	}

	/**
	 * Displays the IP routing table in VyOS format.
	 * Shows both static routes and connected routes (directly connected networks).
	 * Must be executed in OPERATIONAL mode.
	 *
	 * @return Formatted routing table output
	 * @throws InvalidModeException if not in OPERATIONAL mode
	 */
	public String showIpRoute() {
		if (mode != RouterMode.OPERATIONAL) {
			throw new InvalidModeException("Invalid command: show [ip]");
		}

		StringBuilder output = new StringBuilder();

		// Legend (copied from VyOS)
		output.append("Codes: K - kernel route, C - connected, S - static, R - RIP,\n");
		output.append("       O - OSPF, I - IS-IS, B - BGP, E - EIGRP, N - NHRP,\n");
		output.append("       T - Table, v - VNC, V - VNC-Direct, A - Babel, F - PBR,\n");
		output.append("       f - OpenFabric,\n");
		output.append("       > - selected route, * - FIB route, q - queued, r - rejected, b - backup\n");
		output.append("       t - trapped, o - offload failure\n\n");

		// Collect all routes (static + connected)
		List<RouteDisplayEntry> displayEntries = new ArrayList<>();

		// Add connected routes (directly connected networks from configured interfaces)
		for (RouterInterface iface : interfaces) {
			// Show connected route if interface has IP and is administratively UP
			// (Link state doesn't matter for route table - route exists even if link is down)
			if (iface.getSubnet() != null &&
			    iface.getStatus() != null &&
			    iface.getStatus().getAdmin() == AdminState.UP) {

				// getSubnet() now returns the actual network subnet from the interface address
				Subnet connectedNetwork = iface.getSubnet();

				displayEntries.add(new RouteDisplayEntry(
					"C",
					connectedNetwork,
					null,
					iface.getInterfaceName(),
					0,
					false,
					true
				));
			}
		}

		// Add static routes
		for (StaticRoutingEntry entry : routingTable.getRoutingEntries()) {
			displayEntries.add(new RouteDisplayEntry(
				"S",
				entry.getSubnet(),
				entry.getNextHop(),
				entry.getRouterInterface() != null ? entry.getRouterInterface().getInterfaceName() : null,
				entry.getAdministrativeDistance(),
				entry.isDisabled(),
				false
			));
		}

		// Sort routes by subnet (network address, then mask length)
		displayEntries.sort((a, b) -> {
			int addrCompare = a.subnet.getNetworkAddress().toString()
				.compareTo(b.subnet.getNetworkAddress().toString());
			if (addrCompare != 0) return addrCompare;
			return Integer.compare(
				b.subnet.getSubnetMask().getShortMask(),
				a.subnet.getSubnetMask().getShortMask()
			);
		});

		// Display routes
		for (RouteDisplayEntry entry : displayEntries) {
			if (entry.isDisabled) {
				// Disabled routes are not shown in routing table
				continue;
			}

			String prefix = entry.isConnected ? "C>*" : "S>*";
			output.append(prefix).append(" ");
			output.append(entry.subnet.getNetworkAddress()).append("/");
			output.append(entry.subnet.getSubnetMask().getShortMask());

			if (entry.isConnected) {
				// Connected routes - actually connected to the interface
				output.append(" is directly connected, ").append(entry.interfaceName);
			} else {
				// Static routes
				output.append(" [").append(entry.distance).append("]");
				if (entry.nextHop != null) {
					// Static route with next-hop
					output.append(" via ").append(entry.nextHop);
					if (entry.interfaceName != null) {
						output.append(", ").append(entry.interfaceName);
					}
				} else if (entry.interfaceName != null) {
					// Static route via interface (not "directly connected")
					output.append(" via ").append(entry.interfaceName);
				}
			}
			output.append("\n");
		}

		return output.toString();
	}

	/**
	 * Helper class for displaying routing table entries.
	 */
	private static class RouteDisplayEntry {
		String type; // "C" for connected, "S" for static
		Subnet subnet;
		IPAddress nextHop;
		String interfaceName;
		int distance;
		boolean isDisabled;
		boolean isConnected;

		RouteDisplayEntry(String type, Subnet subnet, IPAddress nextHop,
		                  String interfaceName, int distance, boolean isDisabled, boolean isConnected) {
			this.type = type;
			this.subnet = subnet;
			this.nextHop = nextHop;
			this.interfaceName = interfaceName;
			this.distance = distance;
			this.isDisabled = isDisabled;
			this.isConnected = isConnected;
		}
	}

	@Override
	public String toString() {
		if (mode != RouterMode.OPERATIONAL) {
			throw new InvalidModeException("Configuration path: [ip] is not valid\nShow failed");
		}
		return "Router{" +
				"name='" + name + '\'' +
				", routingTable=" + routingTable +
				'}';
	}
}
