package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.Connection;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.ArrayList;
import java.util.List;

public class TopologyQueryService {
	private final NetworkTopology topology;

	public TopologyQueryService(NetworkTopology topology) {
		this.topology = topology;
	}

	public TopologyViewModel getTopologyViewModel() {
		return new TopologyViewModel(
				List.copyOf(topology.getRouters()),
				List.copyOf(topology.getSwitches()),
				List.copyOf(topology.getHosts()),
				List.copyOf(topology.getConnections())
		);
	}

	public List<Connection> getDeviceConnections(Object device) {
		List<Connection> relatedConnections = new ArrayList<>();
		for (Connection conn : topology.getConnections()) {
			if (isDeviceInConnection(device, conn)) {
				relatedConnections.add(conn);
			}
		}
		return relatedConnections;
	}

	public boolean isDeviceInConnection(Object device, Connection connection) {
		if (device instanceof Router router) {
			return router.getInterfaces().stream().anyMatch(iface -> iface.equals(connection.interfaceA()) || iface.equals(connection.interfaceB()));
		} else if (device instanceof Switch sw) {
			return sw.getPorts().stream().anyMatch(port -> port.equals(connection.interfaceA()) || port.equals(connection.interfaceB()));
		} else if (device instanceof Host host) {
			return host.getHostInterface().equals(connection.interfaceA()) || host.getHostInterface().equals(connection.interfaceB());
		}
		return false;
	}

	public List<NetworkInterface> getAvailableInterfaces(Object device) {
		List<NetworkInterface> allInterfaces = new ArrayList<>();
		if (device instanceof Router router) {
			allInterfaces.addAll(router.getInterfaces());
		} else if (device instanceof Switch sw) {
			allInterfaces.addAll(sw.getPorts());
		} else if (device instanceof Host host) {
			allInterfaces.add(host.getHostInterface());
		}

		return allInterfaces.stream()
				.filter(iface -> topology.getConnections().stream()
						.noneMatch(conn -> conn.interfaceA().equals(iface) || conn.interfaceB().equals(iface)))
				.toList();
	}

	public Object findDevice(NetworkInterface iface) {
		for (Router router : topology.getRouters()) {
			if (iface instanceof RouterInterface && router.getInterfaces().contains(iface)) {
				return router;
			}
		}
		for (Switch sw : topology.getSwitches()) {
			if (iface instanceof SwitchPort && sw.getPorts().contains(iface)) {
				return sw;
			}
		}
		for (Host host : topology.getHosts()) {
			if (host.getHostInterface().equals(iface)) {
				return host;
			}
		}
		return null;
	}
}