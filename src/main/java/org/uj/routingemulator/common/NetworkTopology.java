package org.uj.routingemulator.common;

import org.uj.routingemulator.common.exceptions.DuplicateConnectionException;
import org.uj.routingemulator.common.exceptions.InterfaceAlreadyConnected;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public record NetworkTopology(List<Device> devices, List<Connection> connections) {
	private static final Logger logger = Logger.getLogger(NetworkTopology.class.getName());

	/**
	 * Creates an empty network topology.
	 */
	public NetworkTopology() {
		this(new ArrayList<>(), new ArrayList<>());
		logger.config("Initialized new empty network topology");
	}

	/**
	 * Creates a network topology with specified devices and connections.
	 *
	 * @param devices     list of devices
	 * @param connections list of connections between interfaces
	 */
	public NetworkTopology(List<Device> devices, List<Connection> connections) {
		this.devices = new ArrayList<>(devices);
		this.connections = new ArrayList<>(connections);
		logger.config("Initialized custom network topology with provided devices and connections");
	}

	@Override
	public List<Device> devices() {
		return Collections.unmodifiableList(devices);
	}

	@Override
	public List<Connection> connections() {
		return Collections.unmodifiableList(connections);
	}

	/**
	 * Adds a device to the topology.
	 *
	 * @param device the device to add
	 */
	public void addDevice(Device device) {
		this.devices.add(device);
		logger.info("Device %s added to topology".formatted(device.getDeviceName()));
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
					connection.interfaceA().getInterfaceName(),
					connection.interfaceB().getInterfaceName()));
			throw new DuplicateConnectionException("Connection already exists");
		}

		// Check if the reverse connection exists (A-B is the same as B-A)
		Connection reverseConnection = new Connection(connection.interfaceB(), connection.interfaceA());
		if (this.connections.contains(reverseConnection)) {
			logger.warning("Attempted to add duplicate connection (reverse direction) between %s and %s".formatted(
					connection.interfaceB().getInterfaceName(),
					connection.interfaceA().getInterfaceName()));
			throw new DuplicateConnectionException("Connection already exists (reverse direction)");
		}

		// Check if either interface is already connected to something else
		for (Connection existingConnection : this.connections) {
			logger.finest("Checking connection %s <-> %s".formatted(
					existingConnection.interfaceA().getInterfaceName(),
					existingConnection.interfaceB().getInterfaceName()));

			if (existingConnection.interfaceA().equals(connection.interfaceA()) || existingConnection.interfaceB().equals(connection.interfaceA())) {
				logger.warning("Interface %s is already connected in connection between %s and %s".formatted(
						connection.interfaceA().getInterfaceName(),
						existingConnection.interfaceA().getInterfaceName(),
						existingConnection.interfaceB().getInterfaceName()));
				throw new InterfaceAlreadyConnected("Interface " + connection.interfaceA().getInterfaceName() + " is already connected");
			}

			if (existingConnection.interfaceA().equals(connection.interfaceB()) || existingConnection.interfaceB().equals(connection.interfaceB())) {
				logger.warning("Interface %s is already connected in connection between %s and %s".formatted(
						connection.interfaceB().getInterfaceName(),
						existingConnection.interfaceA().getInterfaceName(),
						existingConnection.interfaceB().getInterfaceName()));
				throw new InterfaceAlreadyConnected("Interface " + connection.interfaceB().getInterfaceName() + " is already connected");
			}
		}

		logger.info("Adding connection between %s and %s".formatted(
				connection.interfaceA().getInterfaceName(),
				connection.interfaceB().getInterfaceName()));
		this.connections.add(connection);
	}

	/**
	 * Removes a device from the topology.
	 * Also removes all connections involving this device's interfaces.
	 *
	 * @param deviceId the ID of the device to remove
	 */
	public void removeDevice(DeviceId deviceId) {
		Device device = getDevice(deviceId);
		if (device != null) {
			logger.finer("Removing device %s connections".formatted(device.getDeviceName()));
			connections.removeIf(conn -> device.getInterfaces().contains(conn.interfaceA()) || device.getInterfaces().contains(conn.interfaceB()));
			logger.info("Removing device %s from topology".formatted(device.getDeviceName()));
			this.devices.remove(device);
		}
	}

	/**
	 * Removes a connection from the topology.
	 *
	 * @param connection the connection to remove
	 */
	public void removeConnection(Connection connection) {
		logger.info("Removing connection between %s and %s".formatted(
				connection.interfaceA().getInterfaceName(),
				connection.interfaceB().getInterfaceName()));
		this.connections.remove(connection);
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
					conn.interfaceA().getInterfaceName(),
					conn.interfaceB().getInterfaceName()));
			if (conn.interfaceA().equals(iface) || conn.interfaceB().equals(iface)) {
				return conn;
			}
		}
		return null;
	}

	/**
	 * Checks if an interface has an active physical connection.
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
		return neighbor.isOperational();
	}

	/**
	 * Generates a text-based visualization of the network topology.
	 *
	 * @return text representation of the network topology
	 */
	public String visualize() {
		return NetworkTopologyVisualizer.visualize(this);
	}

	/**
	 * Finds a host interface with exactly the given IP address that is reachable from the given starting interface using connections graph.
	 *
	 * @param start the interface to start searching from (typically a router interface)
	 * @param ip    the exact host IP to find
	 * @return the HostInterface if found, otherwise null
	 */
	public NetworkInterface findHostInterfaceByIpConnectedToInterface(NetworkInterface start, IPAddress ip) {
		return TopologyGraphSearch.findHostInterfaceByIpConnectedToInterface(this, start, ip);
	}

	public Device findDeviceByInterface(NetworkInterface iface) {
		for (Device device : devices) {
			if (device.getInterfaces().contains(iface)) return device;
		}
		return null;
	}

	public Device getDevice(DeviceId id) {
		for (Device device : devices) {
			if (device.getId().equals(id)) return device;
		}
		return null;
	}
}