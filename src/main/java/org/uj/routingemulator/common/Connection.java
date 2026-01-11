package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.uj.routingemulator.router.AdminState;
import org.uj.routingemulator.router.InterfaceStatus;
import org.uj.routingemulator.router.RouterInterface;

/**
 * Represents a bidirectional connection between two network interfaces.
 * <p>
 * Connections are validated upon creation to ensure interfaces are in operational state.
 * Router interfaces must be administratively up (AdminState.UP).
 * <p>
 * LinkState is set automatically when connection is established - it is a result
 * of the connection, not a prerequisite.
 */
@Getter
@EqualsAndHashCode
public class Connection {
	private final NetworkInterface interfaceA;
	private final NetworkInterface interfaceB;

	/**
	 * Creates a new connection between two network interfaces.
	 *
	 * @param interfaceA first interface
	 * @param interfaceB second interface
	 * @throws RuntimeException if connection cannot be established due to interface state
	 */
	public Connection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		try {
			validateConnection(interfaceA, interfaceB);
		} catch (RuntimeException e) {
			throw new RuntimeException("Could not establish connection " + e.getMessage());
		}
		this.interfaceA = interfaceA;
		this.interfaceB = interfaceB;
	}

	/**
	 * Validates that both interfaces can establish a connection.
	 *
	 * @param interfaceA first interface
	 * @param interfaceB second interface
	 * @throws RuntimeException if either interface is in invalid state
	 */
	private void validateConnection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		handleRouterInterface(interfaceA);
		handleRouterInterface(interfaceB);
	}

	/**
	 * Validates router interface state.
	 *
	 * @param networkInterface interface to validate
	 * @throws RuntimeException if router interface is administratively down
	 */
	private void handleRouterInterface(NetworkInterface networkInterface) {
		if (networkInterface instanceof RouterInterface router) {
			InterfaceStatus status = router.getStatus();
			// Only check administrative state - link state will be set as result of connection
			if (status != null && status.getAdmin().equals(AdminState.ADMIN_DOWN)) {
				throw new RuntimeException("Interface " + networkInterface.getInterfaceName() + " is administratively down.");
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
			throw new RuntimeException("Interface not part of this connection");
		}
	}
}
