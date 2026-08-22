package org.uj.routingemulator.common.topology;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;

public class NetworkTopologyQuery implements TopologyQuery {
	private final NetworkTopology topology;
	private final TopologyIndex index;

	public NetworkTopologyQuery(NetworkTopology topology) {
		this.topology = topology;
		this.index = new TopologyIndex(topology);
	}

	@Override
	public boolean isDirectlyConnectedNeighbor(NetworkInterface localIf, NetworkInterface candidate) {
		Connection directConn = topology.getConnectionForInterface(localIf);
		return directConn != null
				&& directConn.getNeighborInterface(localIf) instanceof RouterInterface
				&& directConn.getNeighborInterface(localIf).equals(candidate);
	}

	@Override
	public Router findRouterOwningInterface(RouterInterface iface) {
		return index.getRouterForInterface(iface);
	}

	@Override
	public RouterInterface findInterfaceByIp(IPAddress ip) {
		return index.getInterfaceForIp(ip);
	}

	@Override
	public Connection getConnectionForInterface(NetworkInterface iface) {
		return topology.getConnectionForInterface(iface);
	}

	@Override
	public NetworkInterface findHostInterfaceByIpConnectedToInterface(NetworkInterface start, IPAddress ip) {
		return topology.findHostInterfaceByIpConnectedToInterface(start, ip);
	}
}