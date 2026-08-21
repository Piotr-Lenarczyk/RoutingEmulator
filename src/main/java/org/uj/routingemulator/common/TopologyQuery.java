package org.uj.routingemulator.common;

import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;

public interface TopologyQuery {
	boolean isDirectlyConnectedNeighbor(NetworkInterface localIf, NetworkInterface candidate);

	Router findRouterOwningInterface(RouterInterface iface);

	RouterInterface findInterfaceByIp(IPAddress ip);

	Connection getConnectionForInterface(NetworkInterface iface);

	NetworkInterface findHostInterfaceByIpConnectedToInterface(NetworkInterface start, IPAddress ip);
}