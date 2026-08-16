package org.uj.routingemulator.common;

import org.uj.routingemulator.common.exceptions.NoNeighborInterfaceException;
import org.uj.routingemulator.router.AdminState;
import org.uj.routingemulator.router.InterfaceStatus;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.exceptions.InterfaceAdministrativelyDownException;

import java.util.logging.Logger;

/**
 * Represents a bidirectional connection between two network interfaces.
 * <p>
 * Connections are validated upon creation to ensure interfaces are in operational state.
 * Router interfaces must be administratively up (AdminState.UP).
 * <p>
 * LinkState is set automatically when connection is established - it is a result
 * of the connection, not a prerequisite.
 */
public record Connection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
	private static final Logger logger = Logger.getLogger(Connection.class.getName());

	/**
	 * Creates a new connection between two network interfaces.
	 *
	 * @param interfaceA first interface
	 * @param interfaceB second interface
	 * @throws RuntimeException if connection cannot be established due to interface state
	 */
	public Connection {
		try {
			validateConnection(interfaceA, interfaceB);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Could not establish connection " + e.getMessage());
		}
		logger.fine("Setting up connection between %s and %s".formatted(interfaceA.getInterfaceName(), interfaceB.getInterfaceName()));
	}

	/**
	 * Validates that both interfaces can establish a connection.
	 *
	 * @param interfaceA first interface
	 * @param interfaceB second interface
	 * @throws InterfaceAdministrativelyDownException if either interface is in invalid state
	 */
	private void validateConnection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		handleRouterInterface(interfaceA);
		handleRouterInterface(interfaceB);
	}

	/**
	 * Validates router interface state.
	 *
	 * @param networkInterface interface to validate
	 * @throws InterfaceAdministrativelyDownException if router interface is administratively down
	 */
	private void handleRouterInterface(NetworkInterface networkInterface) {
		if (networkInterface instanceof RouterInterface router) {
			InterfaceStatus status = router.getStatus();
			// Only check administrative state - link state will be set as result of connection
			if (status != null && status.getAdmin().equals(AdminState.ADMIN_DOWN)) {
				logger.warning("Cannot connect to interface %s: Interface is administratively down.".formatted(networkInterface.getInterfaceName()));
				throw new InterfaceAdministrativelyDownException("Interface " + networkInterface.getInterfaceName() + " is administratively down.");
			}
		}
	}

	/**
	 * Gets the neighboring interface for a given interface in this connection.
	 *
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
