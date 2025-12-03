package org.uj.routingemulator.router;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.Subnet;

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
		this.stagedInterfaces = new ArrayList<>(this.interfaces);
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
	 */
	public void addRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: set [protocols]");
		}
		if (stagedRoutingTable.contains(entry)) {
			throw new RuntimeException("Route already exists");
		}
		this.stagedRoutingTable.addRoute(entry);
		hasUncommittedChanges = true;
		System.out.println("[edit]");
	}

	/**
	 * Removes a static route from the routing table.
	 * @param entry Static routing entry to be removed
	 */
	public void removeRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: delete [protocols]");
		}
		if (!stagedRoutingTable.getRoutingEntries().contains(entry)) {
			throw new RuntimeException("Nothing to delete");
		}
		this.stagedRoutingTable.getRoutingEntries().remove(entry);
		hasUncommittedChanges = true;
		System.out.println("[edit]");
	}

	/**
	 * Disables an existing static route in the staged configuration.
	 * If the route does not exist -> throws "Route not found".
	 * If the route is already disabled -> throws "Route already exists" (duplicate configuration).
	 *
	 * @param entry routing entry to disable (matches by subnet/nextHop/interface/distance)
	 */
	public void disableRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: set [protocols]");
		}
		// Find existing entry by equality (equals ignores isDisabled)
		List<StaticRoutingEntry> entries = stagedRoutingTable.getRoutingEntries();
		int idx = entries.indexOf(entry);
		if (idx == -1) {
			throw new RuntimeException("Route not found");
		}
		StaticRoutingEntry existing = entries.get(idx);
		if (existing.isDisabled()) {
			// disabling an already-disabled route is a duplicate configuration
			throw new RuntimeException("Route already exists");
		}
		existing.disable();
		hasUncommittedChanges = true;
		System.out.println("[edit]");
	}

	/**
	 * Configures a router interface in the staged configuration.
	 * @param routerInterfaceName Name of the router interface to be configured
	 * @param subnet Subnet to be assigned to the interface
	 */
	public void configureInterface(String routerInterfaceName, Subnet subnet) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: set [interfaces]");
		}

		// Reject network addresses (e.g., 192.168.1.0/24, 10.0.0.0/16)
		if (subnet.isNetworkAddress()) {
			throw new RuntimeException("Cannot assign network address to interface");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		if (routerInterface.getSubnet() == null || !routerInterface.getSubnet().equals(subnet)) {
			routerInterface.setSubnet(subnet);
			hasUncommittedChanges = true;
			System.out.println("[edit]");
		} else {
			throw new RuntimeException("Configuration already exists");
		}
	}

	/**
	 * Disables a router interface in the staged configuration.
	 * @param routerInterfaceName Name of the router interface to be disabled
	 */
	public void disableInterface(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: set [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		routerInterface.disable();
		hasUncommittedChanges = true;
	}

	/**
	 * Removes an address from a router interface in the staged configuration.
	 * @param routerInterfaceName Name of the router interface
	 */
	public void deleteInterfaceAddress(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: delete [interfaces]");
		}

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("WARN: interface %s does not exist, changes will not be commited".formatted(routerInterfaceName)));

		if (routerInterface.getSubnet() == null) {
			throw new RuntimeException("No value to delete");
		}

		routerInterface.setSubnet(null);
		hasUncommittedChanges = true;
		System.out.println("[edit]");
	}

	/**
	 * Commits configuration changes. Takes place immediately but is not persisted meaning if the device restarts/shuts down, changes will not be saved.
	 */
	public void commitChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: [commit]");
		}
		if (!hasUncommittedChanges) {
			throw new RuntimeException("No configuration changes to commit");
		}
		this.routingTable = new RoutingTable(stagedRoutingTable);
		this.interfaces = new ArrayList<>(stagedInterfaces);
		hasUncommittedChanges = false;
	}

	/**
	 * Discard configuration changes and restores the last committed state.
	 */
	public void discardChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new RuntimeException("Invalid command: [discard]");
		}
		this.stagedRoutingTable = new RoutingTable(routingTable);
		this.stagedInterfaces = new ArrayList<>(interfaces);
		hasUncommittedChanges = false;
	}

	/**
	 * Saves configuration to persistent storage, ensuring it will stay after a reboot.
	 */
	public void saveConfiguration() {
		throw new RuntimeException("Saving configuration is not supported.");
		//if (mode != RouterMode.CONFIGURATION) {
		//	throw new RuntimeException("Invalid command: [save]");
		//}
		//if (hasUncommittedChanges) {
		//	throw new RuntimeException("Cannot save configuration with uncommitted changes.");
		//}
		//System.out.println("Configuration saved.");
	}

	/**
	 * Changes router mode. In case of leaving configuration mode, checks for uncommitted changes. Uncommitted changes
	 * will prevent exiting configuration mode.
	 * @param mode Target router mode
	 */
	public void setMode(RouterMode mode) {
		if (this.mode == RouterMode.CONFIGURATION && hasUncommittedChanges) {
			throw new RuntimeException("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]");
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
		this.stagedInterfaces = new ArrayList<>(this.interfaces);
		this.hasUncommittedChanges = false;
		System.out.println("Router restarted.");
	}

	/**
	 * Finds a router interface by name.
	 *
	 * @param interfaceName Name of the interface to find
	 * @return RouterInterface object if found, null otherwise
	 */
	public RouterInterface findFromName(String interfaceName) {
		return interfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(interfaceName))
				.findFirst()
				.orElse(null);
	}

	@Override
	public String toString() {
		if (mode != RouterMode.OPERATIONAL) {
			throw new RuntimeException("Configuration path: [ip] is not valid\nShow failed");
		}
		return "Router{" +
				"name='" + name + '\'' +
				", routingTable=" + routingTable +
				'}';
	}
}
