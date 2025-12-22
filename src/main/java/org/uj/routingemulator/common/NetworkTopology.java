package org.uj.routingemulator.common;

import lombok.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.switching.Switch;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NetworkTopology {
	private List<Host> hosts;
	private List<Switch> switches;
	private List<Router> routers;
	private List<Connection> connections;

	public NetworkTopology() {
		this.hosts = new ArrayList<>();
		this.switches = new ArrayList<>();
		this.routers = new ArrayList<>();
		this.connections = new ArrayList<>();
	}

	public NetworkTopology(List<Host> hosts, List<Switch> switches, List<Router> routers, List<Connection> connections) {
		this.hosts = hosts;
		this.switches = switches;
		this.routers = routers;
		this.connections = connections;
	}

	public void addHost(Host host) {
		this.hosts.add(host);
	}

	public void addSwitch(Switch sw) {
		this.switches.add(sw);
	}

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
	}

	public void removeHost(Host host) {
		this.hosts.remove(host);
	}

	public void removeSwitch(Switch sw) {
		this.switches.remove(sw);
	}

	public void removeRouter(Router router) {
		this.routers.remove(router);
	}

	public void removeConnection(Connection connection) {
		this.connections.remove(connection);
	}

	private String getDeviceName(NetworkInterface iface) {
		// Check routers
		for (Router router : routers) {
			if (router.getInterfaces().contains(iface)) {
				return router.getName();
			}
		}

		// Check switches
		for (Switch sw : switches) {
			if (sw.getPorts().contains(iface)) {
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

}
