package org.uj.routingemulator.common;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.AdminState;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the complete network topology including all devices and their connections.
 * <p>
 * The topology maintains:
 * <ul>
 *   <li>All network devices (routers, switches, hosts)</li>
 *   <li>All connections between interfaces</li>
 * </ul>
 * <p>
 * Provides operations for adding/removing devices and connections,
 * with validation to prevent duplicate or invalid connections.
 */
@Getter
@Setter
public class NetworkTopology {
	private List<Host> hosts;
	private List<Switch> switches;
	private List<Router> routers;
	private List<Connection> connections;

	/**
	 * Creates an empty network topology.
	 */
	public NetworkTopology() {
		this.hosts = new ArrayList<>();
		this.switches = new ArrayList<>();
		this.routers = new ArrayList<>();
		this.connections = new ArrayList<>();
	}

	/**
	 * Creates a network topology with specified devices and connections.
	 *
	 * @param hosts list of host devices
	 * @param switches list of switch devices
	 * @param routers list of router devices
	 * @param connections list of connections between interfaces
	 */
	public NetworkTopology(List<Host> hosts, List<Switch> switches, List<Router> routers, List<Connection> connections) {
		this.hosts = hosts;
		this.switches = switches;
		this.routers = routers;
		this.connections = connections;
	}

	/**
	 * Adds a host to the topology.
	 *
	 * @param host the host to add
	 */
	public void addHost(Host host) {
		this.hosts.add(host);
	}

	/**
	 * Adds a switch to the topology.
	 *
	 * @param sw the switch to add
	 */
	public void addSwitch(Switch sw) {
		this.switches.add(sw);
	}

	/**
	 * Adds a router to the topology.
	 *
	 * @param router the router to add
	 */
	public void addRouter(Router router) {
		this.routers.add(router);
	}

	/**
	 * Adds a connection to the topology.
	 * Validates that the connection doesn't already exist (in either direction).
	 *
	 * @param connection the connection to add
	 * @throws RuntimeException if the connection already exists or if one of the interfaces is already connected
	 */
	public void addConnection(Connection connection) {
		// Check if this exact connection already exists
		if (this.connections.contains(connection)) {
			throw new RuntimeException("Connection already exists");
		}

		// Check if the reverse connection exists (A-B is the same as B-A)
		Connection reverseConnection = new Connection(connection.getInterfaceB(), connection.getInterfaceA());
		if (this.connections.contains(reverseConnection)) {
			throw new RuntimeException("Connection already exists (reverse direction)");
		}

		// Check if either interface is already connected to something else
		for (Connection existingConnection : this.connections) {
			if (existingConnection.getInterfaceA().equals(connection.getInterfaceA()) ||
				existingConnection.getInterfaceB().equals(connection.getInterfaceA())) {
				throw new RuntimeException("Interface " + connection.getInterfaceA().getInterfaceName() +
					" is already connected");
			}
			if (existingConnection.getInterfaceA().equals(connection.getInterfaceB()) ||
				existingConnection.getInterfaceB().equals(connection.getInterfaceB())) {
				throw new RuntimeException("Interface " + connection.getInterfaceB().getInterfaceName() +
					" is already connected");
			}
		}

		this.connections.add(connection);

		// Update link states for both interfaces
		updateInterfaceLinkState(connection.getInterfaceA());
		updateInterfaceLinkState(connection.getInterfaceB());
	}

	/**
	 * Removes a host from the topology.
	 * Also removes all connections involving this host's interface.
	 *
	 * @param host the host to remove
	 */
	public void removeHost(Host host) {
		connections.removeIf(conn ->
				conn.getInterfaceA().equals(host.getHostInterface()) ||
				conn.getInterfaceB().equals(host.getHostInterface())
		);
		this.hosts.remove(host);
	}

	/**
	 * Removes a switch from the topology.
	 * Also removes all connections involving this switch's ports.
	 *
	 * @param sw the switch to remove
	 */
	public void removeSwitch(Switch sw) {
		connections.removeIf(conn ->
			sw.getPorts().stream().anyMatch(port -> port.equals(conn.getInterfaceA())) ||
			sw.getPorts().stream().anyMatch(port -> port.equals(conn.getInterfaceB()))
		);
		this.switches.remove(sw);
	}

	/**
	 * Removes a router from the topology.
	 * Also removes all connections involving this router's interfaces.
	 *
	 * @param router the router to remove
	 */
	public void removeRouter(Router router) {
		connections.removeIf(conn ->
			router.getInterfaces().stream().anyMatch(iface -> iface.equals(conn.getInterfaceA())) ||
			router.getInterfaces().stream().anyMatch(iface -> iface.equals(conn.getInterfaceB()))
		);
		this.routers.remove(router);
	}

	/**
	 * Removes a connection from the topology.
	 *
	 * @param connection the connection to remove
	 */
	public void removeConnection(Connection connection) {
		this.connections.remove(connection);

		// Update link states for both interfaces
		updateInterfaceLinkState(connection.getInterfaceA());
		updateInterfaceLinkState(connection.getInterfaceB());
	}

	/**
	 * Gets the device name for a given interface.
	 *
	 * @param iface the interface to find the owner device for
	 * @return device name, or "Unknown" if not found
	 */
	private String getDeviceName(NetworkInterface iface) {
		// Check routers
		for (Router router : routers) {
			if (router.getInterfaces().stream().anyMatch(i -> i.equals(iface))) {
				return router.getName();
			}
		}

		// Check switches
		for (Switch sw : switches) {
			if (sw.getPorts().stream().anyMatch(port -> port.equals(iface))) {
				return sw.getName();
			}
		}

		// Check hosts
		for (Host host : hosts) {
			if (host.getHostInterface().equals(iface)) {
				return host.getHostname();
			}
		}

		return "Unknown";
	}

	/**
	 * Generates a text-based visualization of the network topology.
	 * <p>
	 * The visualization includes:
	 * <ul>
	 *   <li>All hosts with their IP addresses and gateways</li>
	 *   <li>All switches with their ports</li>
	 *   <li>All routers with their interfaces</li>
	 *   <li>All connections between interfaces</li>
	 * </ul>
	 *
	 * @return text representation of the network topology
	 */
	public String visualize() {
		StringBuilder sb = new StringBuilder();
		sb.append("=== Network Topology ===\n\n");

		// Hosts
		sb.append("Hosts:\n");
		for (Host host : hosts) {
			sb.append("  └─ ").append(host.getHostname()).append("\n");
			sb.append("      ├─ Interface: ").append(host.getHostInterface().getInterfaceName()).append("\n");
			sb.append("      ├─ IP: ").append(host.getHostInterface().getSubnet().getNetworkAddress()).append("\n");
			sb.append("      └─ Gateway: ").append(host.getHostInterface().getDefaultGateway()).append("\n\n");
		}

		// Switches
		sb.append("Switches:\n");
		for (Switch sw : switches) {
			sb.append("  └─ ").append(sw.getName()).append("\n");
			sb.append("      └─ Ports: ");
			sb.append(sw.getPorts().stream()
					.map(NetworkInterface::getInterfaceName)
					.reduce((a, b) -> a + ", " + b)
					.orElse("none"));
			sb.append("\n\n");
		}

		// Routers
		sb.append("Routers:\n");
		for (Router router : routers) {
			sb.append("  └─ ").append(router.getName()).append("\n");
			sb.append("      └─ Interfaces: ");
			sb.append(router.getInterfaces().stream()
					.map(iface -> iface.getInterfaceName() + (iface.getSubnet() != null ? " (" + iface.getSubnet().getNetworkAddress() + "/" + iface.getSubnet().getSubnetMask() + ")" : " (unconfigured)"))
					.reduce((a, b) -> a + ", " + b)
					.orElse("none"));
			sb.append("\n\n");
		}

		// Connections
		sb.append("Connections:\n");
		for (Connection conn : connections) {
			String deviceA = getDeviceName(conn.getInterfaceA());
			String deviceB = getDeviceName(conn.getInterfaceB());

			sb.append("  ").append(deviceA)
					.append("[").append(conn.getInterfaceA().getInterfaceName()).append("]")
					.append(" <──> ")
					.append(deviceB)
					.append("[").append(conn.getInterfaceB().getInterfaceName()).append("]")
					.append("\n");
		}

		return sb.toString();
	}

	/**
	 * Finds the connection associated with the given interface.
	 *
	 * @param iface the interface to find connection for
	 * @return the connection containing this interface, or null if not connected
	 */
	public Connection getConnectionForInterface(NetworkInterface iface) {
		for (Connection conn : connections) {
			if (conn.getInterfaceA().equals(iface) || conn.getInterfaceB().equals(iface)) {
				return conn;
			}
		}
		return null;
	}

	/**
	 * Checks if an interface has an active physical connection.
	 * <p>
	 * An interface has an active connection if:
	 * <ul>
	 *   <li>It is part of a Connection</li>
	 *   <li>The neighboring interface exists</li>
	 *   <li>If neighbor is a RouterInterface, it must be administratively UP</li>
	 * </ul>
	 *
	 * @param iface the interface to check
	 * @return true if the interface has an active connection
	 */
	public boolean hasActiveConnection(NetworkInterface iface) {
		Connection conn = getConnectionForInterface(iface);
		if (conn == null) {
			return false;
		}

		// Get the neighbor interface
		NetworkInterface neighbor = conn.getNeighborInterface(iface);

		// If neighbor is a RouterInterface, check if it's administratively up
		if (neighbor instanceof RouterInterface routerNeighbor) {
			return routerNeighbor.getStatus().getAdmin() == AdminState.UP;
		}

		// For other interface types (Switch, Host), assume they're always up
		return true;
	}

	/**
	 * Updates the link state of an interface if it's a RouterInterface.
	 * <p>
	 * This method should be called automatically whenever connections change.
	 *
	 * @param iface the interface to update
	 */
	private void updateInterfaceLinkState(NetworkInterface iface) {
		if (iface instanceof RouterInterface routerIface) {
			routerIface.updateLinkState(this);
		}
	}

	/**
	 * Updates the link states of all neighboring interfaces.
	 * <p>
	 * Should be called when an interface's administrative state changes,
	 * as this affects the link state of connected interfaces.
	 *
	 * @param iface the interface whose neighbors should be updated
	 */
	public void updateNeighborLinkStates(NetworkInterface iface) {
		Connection conn = getConnectionForInterface(iface);
		if (conn != null) {
			NetworkInterface neighbor = conn.getNeighborInterface(iface);
			updateInterfaceLinkState(neighbor);
		}
	}

}
