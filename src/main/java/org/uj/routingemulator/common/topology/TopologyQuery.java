package org.uj.routingemulator.common.topology;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;

public interface TopologyQuery {
	boolean isDirectlyConnectedNeighbor(NetworkInterface localIf, NetworkInterface candidate);

	Router findRouterOwningInterface(RouterInterface iface);

	RouterInterface findInterfaceByIp(IPAddress ip);

	Connection getConnectionForInterface(NetworkInterface iface);

	NetworkInterface findHostInterfaceByIpConnectedToInterface(NetworkInterface start, IPAddress ip);
}