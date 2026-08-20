package org.uj.routingemulator.common;

import org.uj.routingemulator.common.exceptions.NoNeighborInterfaceException;
import java.util.logging.Logger;

/**
 * Represents a bidirectional connection between two network interfaces.
 * <p>
 * Connections are validated upon creation to ensure interfaces are in operational state.
 * <p>
 * LinkState is set automatically when connection is established - it is a result
 * of the connection, not a prerequisite.
 */
public record Connection(ConnectionId id, NetworkInterface interfaceA, NetworkInterface interfaceB) {
	private static final Logger logger = Logger.getLogger(Connection.class.getName());

	/**
	 * Creates a new connection between two network interfaces.
	 * @param interfaceA first interface
	 * @param interfaceB second interface
	 * @throws RuntimeException if connection cannot be established due to interface state
	 */
	public Connection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		this(ConnectionId.generate(), interfaceA, interfaceB);
	}

	public Connection {
		if (id == null) {
			id = ConnectionId.generate();
		}

		if (!interfaceA.isOperational()) {
			throw new IllegalStateException("Interface " + interfaceA.getInterfaceName() + " is down");
		}
		if (!interfaceB.isOperational()) {
			throw new IllegalStateException("Interface " + interfaceB.getInterfaceName() + " is down");
		}

		logger.fine("Setting up connection between %s and %s".formatted(interfaceA.getInterfaceName(), interfaceB.getInterfaceName()));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Connection that = (Connection) o;
		return interfaceA.equals(that.interfaceA) && interfaceB.equals(that.interfaceB);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(interfaceA, interfaceB);
	}

	/**
	 * Gets the neighboring interface for a given interface in this connection.
	 * @param iface the interface to find neighbor for
	 * @return the neighboring interface
	 * @throws RuntimeException if the given interface is not part of this connection
	 */
	public NetworkInterface getNeighborInterface(NetworkInterface iface) {
		if (iface.equals(interfaceA)) {
			return interfaceB;
		} else if (iface.equals(interfaceB)) {
			return interfaceA;
		} else {
			logger.severe("Interface %s is not part of this connection between %s and %s".formatted(
					iface.getInterfaceName(), interfaceA.getInterfaceName(), interfaceB.getInterfaceName()));
			throw new NoNeighborInterfaceException("Interface not part of this connection");
		}
	}
}