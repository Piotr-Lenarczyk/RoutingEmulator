package org.uj.routingemulator.common;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.AdminState;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;

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
	private static final Logger logger = Logger.getLogger(NetworkTopology.class.getName());
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
		logger.config("Initialized new empty network topology");
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
		logger.config("Initialized custom network topology with provided devices and connections");
	}

	/**
	 * Adds a host to the topology.
	 *
	 * @param host the host to add
	 */
	public void addHost(Host host) {
		this.hosts.add(host);
		logger.info("Host %s added to topology".formatted(host.getHostname()));
	}

	/**
	 * Adds a switch to the topology.
	 *
	 * @param sw the switch to add
	 */
	public void addSwitch(Switch sw) {
		this.switches.add(sw);
		logger.info("Switch %s added to topology".formatted(sw.getName()));
	}

	/**
	 * Adds a router to the topology.
	 *
	 * @param router the router to add
	 */
	public void addRouter(Router router) {
		this.routers.add(router);
		logger.info("Router %s added to topology".formatted(router.getName()));
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
			logger.warning("Attempted to add duplicate connection between %s and %s".formatted(
					connection.getInterfaceA().getInterfaceName(), connection.getInterfaceB().getInterfaceName()));
			throw new RuntimeException("Connection already exists");
		}

		// Check if the reverse connection exists (A-B is the same as B-A)
		Connection reverseConnection = new Connection(connection.getInterfaceB(), connection.getInterfaceA());
		if (this.connections.contains(reverseConnection)) {
			logger.warning("Attempted to add duplicate connection (reverse direction) between %s and %s".formatted(
					connection.getInterfaceB().getInterfaceName(), connection.getInterfaceA().getInterfaceName()));
			throw new RuntimeException("Connection already exists (reverse direction)");
		}

		// Check if either interface is already connected to something else
		for (Connection existingConnection : this.connections) {
			logger.finest("Checking connection %s <-> %s".formatted(
					existingConnection.getInterfaceA().getInterfaceName(),
					existingConnection.getInterfaceB().getInterfaceName()));
			if (existingConnection.getInterfaceA().equals(connection.getInterfaceA()) ||
				existingConnection.getInterfaceB().equals(connection.getInterfaceA())) {
				logger.warning("Interface %s is already connected in connection between %s and %s".formatted(
						connection.getInterfaceA().getInterfaceName(),
						existingConnection.getInterfaceA().getInterfaceName(),
						existingConnection.getInterfaceB().getInterfaceName()));
				throw new RuntimeException("Interface " + connection.getInterfaceA().getInterfaceName() +
					" is already connected");
			}
			if (existingConnection.getInterfaceA().equals(connection.getInterfaceB()) ||
				existingConnection.getInterfaceB().equals(connection.getInterfaceB())) {
				logger.warning("Interface %s is already connected in connection between %s and %s".formatted(
						connection.getInterfaceB().getInterfaceName(),
						existingConnection.getInterfaceA().getInterfaceName(),
						existingConnection.getInterfaceB().getInterfaceName()));
				throw new RuntimeException("Interface " + connection.getInterfaceB().getInterfaceName() +
					" is already connected");
			}
		}

		logger.info("Adding connection between %s and %s".formatted(
				connection.getInterfaceA().getInterfaceName(), connection.getInterfaceB().getInterfaceName()));
		this.connections.add(connection);

		// Update link states for both interfaces
		logger.finer("Updating link states for interface %s".formatted(connection.getInterfaceA().getInterfaceName()));
		updateInterfaceLinkState(connection.getInterfaceA());
		logger.finer("Updating link states for interface %s".formatted(connection.getInterfaceB().getInterfaceName()));
		updateInterfaceLinkState(connection.getInterfaceB());
	}

	/**
	 * Removes a host from the topology.
	 * Also removes all connections involving this host's interface.
	 *
	 * @param host the host to remove
	 */
	public void removeHost(Host host) {
		logger.finer("Removing host %s connections".formatted(host.getHostname()));
		connections.removeIf(conn ->
				conn.getInterfaceA().equals(host.getHostInterface()) ||
				conn.getInterfaceB().equals(host.getHostInterface())
		);
		logger.info("Removing host %s from topology".formatted(host.getHostname()));
		this.hosts.remove(host);
	}

	/**
	 * Removes a switch from the topology.
	 * Also removes all connections involving this switch's ports.
	 *
	 * @param sw the switch to remove
	 */
	public void removeSwitch(Switch sw) {
		logger.finer("Removing switch %s connections".formatted(sw.getName()));
		connections.removeIf(conn ->
			sw.getPorts().stream().anyMatch(port -> port.equals(conn.getInterfaceA())) ||
			sw.getPorts().stream().anyMatch(port -> port.equals(conn.getInterfaceB()))
		);
		logger.info("Removing switch %s from topology".formatted(sw.getName()));
		this.switches.remove(sw);
	}

	/**
	 * Removes a router from the topology.
	 * Also removes all connections involving this router's interfaces.
	 *
	 * @param router the router to remove
	 */
	public void removeRouter(Router router) {
		logger.finer("Removing router %s connections".formatted(router.getName()));
		connections.removeIf(conn ->
			router.getInterfaces().stream().anyMatch(iface -> iface.equals(conn.getInterfaceA())) ||
			router.getInterfaces().stream().anyMatch(iface -> iface.equals(conn.getInterfaceB()))
		);
		logger.info("Removing router %s from topology".formatted(router.getName()));
		this.routers.remove(router);
	}

	/**
	 * Removes a connection from the topology.
	 *
	 * @param connection the connection to remove
	 */
	public void removeConnection(Connection connection) {
		logger.info("Removing connection between %s and %s".formatted(
				connection.getInterfaceA().getInterfaceName(), connection.getInterfaceB().getInterfaceName()));
		this.connections.remove(connection);

		// Update link states for both interfaces
		logger.finer("Updating link states for interface %s after connection removal".formatted(connection.getInterfaceA().getInterfaceName()));
		updateInterfaceLinkState(connection.getInterfaceA());
		logger.finer("Updating link states for interface %s after connection removal".formatted(connection.getInterfaceB().getInterfaceName()));
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
		logger.finer("Searching for connection involving interface %s".formatted(iface.getInterfaceName()));
		for (Connection conn : connections) {
			logger.finest("Checking connection between %s and %s".formatted(
					conn.getInterfaceA().getInterfaceName(), conn.getInterfaceB().getInterfaceName()));
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
			logger.finest("Checking if interface %s neighbor %s is administratively up".formatted(iface.getInterfaceName(), routerNeighbor.getInterfaceName()));
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
			logger.finest("Updating link state for router interface %s".formatted(routerIface.getInterfaceName()));
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

	/**
	 * Finds a host interface with exactly the given IP address that is reachable from the given starting interface using connections graph.
	 * This performs a BFS across connections (through switches and other devices) starting at the provided interface.
	 *
	 * @param start the interface to start searching from (typically a router interface)
	 * @param ip    the exact host IP to find
	 * @return the HostInterface if found, otherwise null
	 */
	public HostInterface findHostInterfaceByIpConnectedToInterface(NetworkInterface start, IPAddress ip) {
		Queue<NetworkInterface> q = new java.util.ArrayDeque<>();
		Set<NetworkInterface> visited = new java.util.HashSet<>();

		q.add(start);
		visited.add(start);

		while (!q.isEmpty()) {
			NetworkInterface cur = q.remove();

			// If current is a HostInterface, check directly
			if (cur instanceof HostInterface hif) {
				if (hif.getSubnet() != null && hif.getSubnet().getNetworkAddress().equals(ip)) {
					return hif;
				}
			}

			// If current is a switch port, treat switch as hub: add all other ports of the same switch
			if (cur instanceof SwitchPort sp) {
				for (Switch sw : switches) {
					if (sw.containsPort(sp)) {
						for (SwitchPort sibling : sw.getPorts()) {
							if (!visited.contains(sibling)) {
								visited.add(sibling);
								q.add(sibling);
							}
						}
						break;
					}
				}
			}

			// Process the connection for current interface (if any)
			Connection c = getConnectionForInterface(cur);
			if (c == null) continue;

			NetworkInterface neighbor = c.getNeighborInterface(cur);
			if (neighbor == null) continue;

			if (!visited.contains(neighbor)) {
				visited.add(neighbor);
				// If neighbor is HostInterface, check if it has the exact IP assigned
				if (neighbor instanceof HostInterface hif) {
					if (hif.getSubnet() != null && hif.getSubnet().getNetworkAddress().equals(ip)) {
						return hif;
					}
				}
				q.add(neighbor);
			}
		}

		return null;
	}

	/**
	 * Convenience overload for RouterInterface start parameter.
	 */
	public HostInterface findHostInterfaceByIpConnectedToInterface(RouterInterface start, IPAddress ip) {
		return findHostInterfaceByIpConnectedToInterface((NetworkInterface) start, ip);
	}

}
