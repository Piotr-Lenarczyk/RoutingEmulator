package org.uj.routingemulator.common.topology;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.router.model.RouterInterface;

import java.util.HashMap;
import java.util.Map;

public class TopologyIndex {
	private final Map<RouterInterface, Router> interfaceToRouter = new HashMap<>();
	private final Map<IPAddress, RouterInterface> ipToInterface = new HashMap<>();

	public TopologyIndex(NetworkTopology topology) {
		for (Device d : topology.devices()) {
			if (d instanceof Router r) {
				for (RouterInterface ri : r.getInterfaces()) {
					interfaceToRouter.put(ri, r);
					if (ri.getInterfaceAddress() != null && ri.getInterfaceAddress().ipAddress() != null) {
						ipToInterface.put(ri.getInterfaceAddress().ipAddress(), ri);
					}
				}
			}
		}
	}

	public Router getRouterForInterface(RouterInterface iface) {
		return interfaceToRouter.get(iface);
	}

	public RouterInterface getInterfaceForIp(IPAddress ip) {
		return ipToInterface.get(ip);
	}
}