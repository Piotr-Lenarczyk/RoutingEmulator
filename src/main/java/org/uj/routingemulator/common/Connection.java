package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.uj.routingemulator.router.AdminState;
import org.uj.routingemulator.router.InterfaceStatus;
import org.uj.routingemulator.router.LinkState;
import org.uj.routingemulator.router.RouterInterface;

@Getter
@EqualsAndHashCode
public class Connection {
	private final NetworkInterface interfaceA;
	private final NetworkInterface interfaceB;

	public Connection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		try {
			validateConnection(interfaceA, interfaceB);
		} catch (RuntimeException e) {
			throw new RuntimeException("Could not establish connection " + e.getMessage());
		}
		this.interfaceA = interfaceA;
		this.interfaceB = interfaceB;
	}

	private void validateConnection(NetworkInterface interfaceA, NetworkInterface interfaceB) {
		handleRouterInterface(interfaceA);
		handleRouterInterface(interfaceB);
	}

	private void handleRouterInterface(NetworkInterface networkInterface) {
		if (networkInterface instanceof RouterInterface) {
			RouterInterface router = (RouterInterface) networkInterface;
			InterfaceStatus status = router.getStatus();
			if (status != null && (status.getAdmin().equals(AdminState.ADMIN_DOWN)
					|| status.getLink().equals(LinkState.DOWN))) {
				throw new RuntimeException("Interface " + networkInterface.getInterfaceName() + " is down.");
			}
		}
	}

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
