package org.uj.routingemulator.router;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(Router.class.getName());

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
	 * Stores the terminal buffer for GUI sessions to preserve history
	 * across dialog open/close cycles.
	 */
	@Getter
	@Setter
	private StringBuilder terminalBuffer = new StringBuilder();

	/**
	 * Constructs a router with default configuration in VyOS.
	 * Default router configuration includes 2 interfaces: eth0 and lo
	 *
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
		logger.fine("Creating new router %s with default configuration".formatted(name));
	}

	/**
	 * `
	 * Constructs a router with specific interfaces.
	 *
	 * @param name       Name of the router
	 * @param interfaces List of interfaces to be added to the router
	 */
	public Router(String name, List<RouterInterface> interfaces) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = interfaces;
		this.mode = RouterMode.OPERATIONAL;
		this.stagedRoutingTable = new RoutingTable();
		this.stagedInterfaces = new ArrayList<>(interfaces);
		logger.fine("Creating new router %s with custom interfaces: %s".formatted(name, interfaces));
	}

	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	/**
	 * Adds a new static route to the routing table.
	 *
	 * @param entry Static routing entry to be added
	 * @throws InvalidModeException            if not in CONFIGURATION mode
	 * @throws DuplicateConfigurationException if route already exists
	 */
	public void addRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to add route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [protocols]");
		}

		// Validate that the subnet represents a proper network address (host bits == 0)
		Subnet routeSubnet = entry.getSubnet();
		if (routeSubnet == null || !routeSubnet.isValidNetworkAddress()) {
			String msg = String.format("\n\tError: %s is not a valid IPv4 prefix\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed\n\n[edit]", routeSubnet == null ? "null" : routeSubnet.toString());
			logger.warning("Invalid subnet (not a network address) provided for route: %s".formatted(routeSubnet));
			throw new RuntimeException(msg);
		}

		if (stagedRoutingTable.contains(entry)) {
			logger.warning("Attempted to add duplicate route: %s".formatted(entry));
			throw new DuplicateConfigurationException("Route already exists");
		}
		// Validate next-hop if present, but do not stage until validation completes
		if (entry.getNextHop() != null) {
			logger.finer("Validating next-hop %s for the new route".formatted(entry.getNextHop()));
			IPAddress nh = entry.getNextHop();
			// Look for an interface in stagedInterfaces that has this exact IP assigned
			RouterInterface found = stagedInterfaces.stream()
					.filter(i -> i.getInterfaceAddress() != null && i.getInterfaceAddress().getIpAddress().equals(nh))
					.findFirst()
					.orElse(null);

			if (found != null) {
				// Next-hop points to an IP assigned to this router -> invalid next-hop (local interface)
				String nhFormatted = found.getInterfaceAddress().toString();
				String msg = String.format("Next-hop interface %s is a local interface on the router%nPackets routed through this route will not be forwarded%nEnsure this action is deliberate", nhFormatted);

				// Log developer message and user-facing warning and continue (do not throw)
				logger.info("Next-hop interface %s is a local interface on the router".formatted(nh));
				logger.warning(msg);
				// continue to stage route (no confirmation mechanism)
			}

			// Determine whether next-hop lies inside any configured subnet on staged interfaces
			Integer inferredMask = null;
			for (RouterInterface ri : stagedInterfaces) {
				logger.finest("Checking interface %s with subnet %s".formatted(ri.getInterfaceName(), ri.getSubnet()));
				if (ri.getSubnet() != null && ri.getSubnet().getSubnetMask() != null) {
					Subnet s = ri.getSubnet();
					long ipAsLong = ((long) nh.getOctet1() << 24) | ((long) nh.getOctet2() << 16) | ((long) nh.getOctet3() << 8) | nh.getOctet4();
					int prefix = s.getSubnetMask().getShortMask();
					long networkMask = (prefix == 0) ? 0 : (0xFFFFFFFFL << (32 - prefix));
					long net = ((long) s.getNetworkAddress().getOctet1() << 24) | ((long) s.getNetworkAddress().getOctet2() << 16) | ((long) s.getNetworkAddress().getOctet3() << 8) | s.getNetworkAddress().getOctet4();
					if ((ipAsLong & networkMask) == (net & networkMask)) {
						inferredMask = prefix;
						break;
					}
				}
			}

			if (inferredMask != null) {
				String nhFormatted = nh + "/" + inferredMask;
				String msg = String.format("Next-hop interface %s not found on the router%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", nhFormatted);

				logger.info("Next-hop interface %s not found on the router".formatted(nh));
				logger.warning(msg);
				// continue to stage route
			} else {
				// Next-hop not found in any configured subnet -> warn user (invalid next-hop)
				String msg = String.format("Next-hop interface %s is not a directly connected neighbor interface%nThis may be fine if configuration is not yet complete%nPackets routed through this route will be dropped until the next-hop is reachable%nEnsure this action is deliberate", nh);

				logger.info("Next-hop interface %s not found on the router".formatted(nh));
				logger.warning(msg);
				// continue to stage route
			}
		}

		// Stage the route after validation
		this.stagedRoutingTable.addRoute(entry);
		hasUncommittedChanges = true;

		logger.info("%s: Creating static route %s".formatted(this.name, entry));
	}

	/**
	 * Removes a static route from the routing table.
	 *
	 * @param entry Static routing entry to be removed
	 * @throws InvalidModeException           if not in CONFIGURATION mode
	 * @throws ConfigurationNotFoundException if route doesn't exist
	 */
	public void removeRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to remove route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: delete [protocols]");
		}
		if (!stagedRoutingTable.getRoutingEntries().contains(entry)) {
			logger.warning("Attempted to remove non-existent route: %s".formatted(entry));
			throw new ConfigurationNotFoundException("Nothing to delete");
		}
		this.stagedRoutingTable.getRoutingEntries().remove(entry);
		hasUncommittedChanges = true;
		logger.info("%s: Route %s removed from staged configuration".formatted(this.name, entry));
	}

	/**
	 * Disables an existing static route in the staged configuration.
	 * If the route does not exist -> throws "Route not found".
	 * If the route is already disabled -> throws "Route already exists" (duplicate configuration).
	 *
	 * @param entry routing entry to disable (matches by subnet/nextHop/interface/distance)
	 * @throws InvalidModeException            if not in CONFIGURATION mode
	 * @throws ConfigurationNotFoundException  if route doesn't exist
	 * @throws DuplicateConfigurationException if route is already disabled
	 */
	public void disableRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to disable route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [protocols]");
		}
		// Find existing entry by equality (equals ignores isDisabled)
		List<StaticRoutingEntry> entries = stagedRoutingTable.getRoutingEntries();
		int idx = entries.indexOf(entry);
		if (idx == -1) {
			logger.warning("Attempted to disable non-existent route: %s".formatted(entry));
			throw new ConfigurationNotFoundException("Route not found");
		}
		StaticRoutingEntry existing = entries.get(idx);
		if (existing.isDisabled()) {
			// disabling an already-disabled route is a duplicate configuration
			logger.warning("Attempted to disable an already disabled route: %s".formatted(entry));
			throw new DuplicateConfigurationException("Route already exists");
		}
		existing.disable();
		hasUncommittedChanges = true;
		logger.info("%s: Route %s disabled in staged configuration".formatted(this.name, entry));
	}

	/**
	 * Configures a router interface with an IP address in the staged configuration.
	 *
	 * @param routerInterfaceName Name of the router interface to be configured
	 * @param interfaceAddress    IP address and mask to be assigned to the interface
	 * @throws InvalidModeException            if not in CONFIGURATION mode
	 * @throws InvalidAddressException         if the address is invalid (network/broadcast address)
	 * @throws InterfaceNotFoundException      if the interface doesn't exist
	 * @throws DuplicateConfigurationException if the interface already has this address
	 */
	public void configureInterface(String routerInterfaceName, InterfaceAddress interfaceAddress) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to configure interface while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		// Provide validation with concise error messages
		if (interfaceAddress.isNetworkAddress()) {
			logger.warning("Attempted to assign network address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new InvalidAddressException(
					String.format("Cannot assign network address %s to the interface. Use a host address instead",
							interfaceAddress)
			);
		}

		if (interfaceAddress.isBroadcastAddress()) {
			logger.warning("Attempted to assign broadcast address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new InvalidAddressException(
					String.format("Cannot assign broadcast address %s to the interface. Use a host address instead",
							interfaceAddress)
			);
		}

		if (!interfaceAddress.isValidHostAddress()) {
			logger.warning("Attempted to assign invalid host address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new InvalidAddressException(interfaceAddress + " is not a valid host IP address");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		// Check duplicate first
		if (routerInterface.getInterfaceAddress() != null && routerInterface.getInterfaceAddress().equals(interfaceAddress)) {
			logger.warning("Attempted to assign duplicate address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new DuplicateConfigurationException("Configuration already exists");
		}

		// Capture previous staged value for rollback (rollback removed)
		final InterfaceAddress previous = routerInterface.getInterfaceAddress();

		// Stage the new address
		routerInterface.setInterfaceAddress(interfaceAddress);
		hasUncommittedChanges = true;

		// If the interface is administratively disabled, log and continue (no confirmation mechanism)
		if (routerInterface.isDisabled()) {
			logger.info("Interface %s is disabled. Staged change applied but packets routed through this interface will be dropped".formatted(routerInterfaceName));
			String msg = String.format("Interface %s is disabled%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", routerInterface.getInterfaceName());
			logger.warning(msg);
			// do not throw; staged change remains
		}
		logger.info("%s: Interface %s configured with address %s in staged configuration".formatted(this.name, routerInterfaceName, interfaceAddress));
	}

	/**
	 * Disables a router interface in the staged configuration.
	 *
	 * @param routerInterfaceName Name of the router interface to be disabled
	 * @throws InvalidModeException       if not in CONFIGURATION mode
	 * @throws InterfaceNotFoundException if the interface doesn't exist
	 */
	public void disableInterface(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to disable interface while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));
		logger.info("%s: Disabling interface %s in staged configuration".formatted(this.getName(), routerInterfaceName));
		routerInterface.disable();
		hasUncommittedChanges = true;
	}

	/**
	 * Removes an address from a router interface in the staged configuration.
	 *
	 * @param routerInterfaceName Name of the router interface
	 * @throws InvalidModeException           if not in CONFIGURATION mode
	 * @throws InterfaceNotFoundException     if the interface doesn't exist
	 * @throws ConfigurationNotFoundException if the interface has no address to delete
	 */
	public void deleteInterfaceAddress(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to delete interface address while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: delete [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		if (routerInterface.getInterfaceAddress() == null) {
			logger.warning("Attempted to delete non-existent address from interface %s".formatted(routerInterfaceName));
			throw new ConfigurationNotFoundException("No value to delete");
		}

		routerInterface.setInterfaceAddress(null);
		hasUncommittedChanges = true;
		logger.info("%s: Address deleted from interface %s in staged configuration".formatted(this.name, routerInterfaceName));
	}

	/**
	 * Commits configuration changes. Takes place immediately but is not persisted meaning if the device restarts/shuts down, changes will not be saved.
	 *
	 * @throws InvalidModeException       if not in CONFIGURATION mode
	 * @throws NoChangesToCommitException if there are no changes to commit
	 */
	public void commitChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to commit changes while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: [commit]");
		}
		if (!hasUncommittedChanges) {
			logger.warning("Attempted to commit with no changes in staged configuration");
			throw new NoChangesToCommitException("No configuration changes to commit");
		}

		// Instead of replacing interface objects (which would break existing Connection references),
		// update existing RouterInterface instances in-place to preserve identity held by NetworkTopology.
		for (RouterInterface stagedIf : stagedInterfaces) {
			logger.finest("Committing interface %s with address %s".formatted(stagedIf.getInterfaceName(), stagedIf.getInterfaceAddress()));
			RouterInterface existing = this.interfaces.stream()
					.filter(i -> i.getInterfaceName().equals(stagedIf.getInterfaceName()))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				logger.finest("Updating existing interface %s in-place".formatted(existing.getInterfaceName()));
				existing.setInterfaceAddress(stagedIf.getInterfaceAddress());
				existing.setMacAddress(stagedIf.getMacAddress());
				existing.setDescription(stagedIf.getDescription());
				existing.setVrf(stagedIf.getVrf());
				existing.setMtu(stagedIf.getMtu());
				existing.setStatus(stagedIf.getStatus());
			} else {
				// New interface added in staged config - append a copy preserving object identity for future commits
				logger.finest("Adding new interface %s to committed configuration".formatted(stagedIf.getInterfaceName()));
				this.interfaces.add(new RouterInterface(stagedIf));
			}
		}

		// Update routing table while mapping staged interface references to the committed interface objects
		this.routingTable = copyRoutingTableWithUpdatedInterfaces(stagedRoutingTable, interfaces);
		hasUncommittedChanges = false;
		logger.info("%s: Commit complete".formatted(this.name));
	}

	/**
	 * Discard configuration changes and restores the last committed state.
	 *
	 * @throws InvalidModeException if not in CONFIGURATION mode
	 */
	public void discardChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: [discard]");
		}
		this.stagedInterfaces = deepCopyInterfaces(interfaces);
		this.stagedRoutingTable = copyRoutingTableWithUpdatedInterfaces(routingTable, stagedInterfaces);
		hasUncommittedChanges = false;
	}

	/**
	 * Clears all staged configuration, removing all interface addresses and routing entries.
	 * This is typically used before loading configuration from a file to ensure a clean slate.
	 * Must be in CONFIGURATION mode to use this method.
	 *
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
	 * Changes router mode. In case of leaving configuration mode, checks for uncommitted changes. Uncommitted changes
	 * will prevent exiting configuration mode.
	 * When entering configuration mode, resets staged configuration to current committed state.
	 *
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
			this.stagedRoutingTable = copyRoutingTableWithUpdatedInterfaces(this.routingTable, this.stagedInterfaces);
		}
		this.mode = mode;
	}

	/**
	 * Changes router mode. In case of leaving configuration mode, forces discarding uncommitted changes.
	 *
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
	 * Copies a routing table while updating RouterInterface references.
	 * Maps interface references from oldInterfaces to corresponding interfaces in newInterfaces.
	 *
	 * @param routingTable  Original routing table
	 * @param newInterfaces New interface list to map to
	 * @return New routing table with updated interface references
	 */
	private RoutingTable copyRoutingTableWithUpdatedInterfaces(
			RoutingTable routingTable,
			List<RouterInterface> newInterfaces) {
		logger.finest("Copying routing table with updated interfaces");
		RoutingTable newTable = new RoutingTable();
		for (StaticRoutingEntry entry : routingTable.getRoutingEntries()) {
			logger.finest("Processing route %s for subnet %s".formatted(entry, entry.getSubnet()));
			StaticRoutingEntry newEntry;
			if (entry.getRouterInterface() != null) {
				logger.finest("Route is interface-based, finding corresponding interface in new configuration");
				// Find corresponding interface in newInterfaces
				String interfaceName = entry.getRouterInterface().getInterfaceName();
				RouterInterface newInterface = newInterfaces.stream()
						.filter(intf -> intf.getInterfaceName().equals(interfaceName))
						.findFirst()
						.orElse(null);
				// Create new entry with updated interface reference
				if (entry.getAdministrativeDistance() == 1) {
					logger.finest("Creating new interface-based route with default administrative distance");
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface);
				} else {
					logger.finest("Creating new interface-based route with administrative distance %d".formatted(entry.getAdministrativeDistance()));
					newEntry = new StaticRoutingEntry(entry.getSubnet(), newInterface, entry.getAdministrativeDistance());
				}
			} else {
				// Next-hop route - just copy
				if (entry.getAdministrativeDistance() == 1) {
					logger.finest("Creating new next-hop route with default administrative distance");
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop());
				} else {
					logger.finest("Creating new next-hop route with administrative distance %d".formatted(entry.getAdministrativeDistance()));
					newEntry = new StaticRoutingEntry(entry.getSubnet(), entry.getNextHop(), entry.getAdministrativeDistance());
				}
			}
			// Preserve disabled state
			if (entry.isDisabled()) {
				logger.finest("Preserving disabled state for route %s".formatted(entry));
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
			logger.warning("Attempted to show IP route while in %s mode".formatted(mode));
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
		List<RouteDisplayEntry> displayEntries = getRouteDisplayEntries();

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

	private List<RouteDisplayEntry> getRouteDisplayEntries() {
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
		return displayEntries;
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

	/**
	 * /**
	 * Creates a deep copy of interface list.
	 *
	 * @param interfaces List of interfaces to copy
	 * @return A new list containing copies of all interfaces
	 */
	private List<RouterInterface> deepCopyInterfaces(List<RouterInterface> interfaces) {
		logger.finest("Creating deep copy of interfaces for staged configuration");
		List<RouterInterface> copy = new ArrayList<>();
		for (RouterInterface iface : interfaces) {
			logger.finest("Copying interface %s with address %s".formatted(iface.getInterfaceName(), iface.getInterfaceAddress()));
			copy.add(new RouterInterface(iface));
		}
		return copy;
	}

	// ---- Confirmation mechanism (for dangerous/stage-but-ask commands) ----

	public PingStatistics ping(String dst, NetworkTopology topology) {
		logger.info("Initializing new PingService for host %s".formatted(this.name));
		PingService svc = new PingService();
		logger.info("%s: Pinging %s with 4 probes...".formatted(this.name, dst));
		return svc.ping(this, IPAddress.fromString(dst), 4, 64, topology);
	}

	public PingStatistics ping(String dst, int count, int ttl, NetworkTopology topology) {
		logger.info("Initializing new PingService for host %s".formatted(this.name));
		PingService svc = new PingService();
		logger.info("%s: Pinging %s with 4 probes...".formatted(this.name, dst));
		return svc.ping(this, IPAddress.fromString(dst), count, ttl, topology);
	}

	/**
	 * Helper class for displaying routing table entries.
	 *
	 * @param type "C" for connected, "S" for static
	 */
	private record RouteDisplayEntry(String type, Subnet subnet, IPAddress nextHop, String interfaceName, int distance,
	                                 boolean isDisabled, boolean isConnected) {
	}
}
