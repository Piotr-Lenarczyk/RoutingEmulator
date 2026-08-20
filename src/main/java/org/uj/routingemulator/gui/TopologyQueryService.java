package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.switching.Switch;

import java.util.ArrayList;
import java.util.List;

public class TopologyQueryService {
	private final NetworkTopology topology;

	public TopologyQueryService(NetworkTopology topology) {
		this.topology = topology;
	}

	public TopologyViewModel getTopologyViewModel() {
		List<Router> routers = new ArrayList<>();
		List<Switch> switches = new ArrayList<>();
		List<Host> hosts = new ArrayList<>();

		for (Device d : topology.getDevices()) {
			if (d instanceof Router r) routers.add(r);
			else if (d instanceof Switch s) switches.add(s);
			else if (d instanceof Host h) hosts.add(h);
		}

		return new TopologyViewModel(
				routers,
				switches,
				hosts,
				List.copyOf(topology.getConnections())
		);
	}

	public List<Connection> getDeviceConnections(Device device) {
		List<Connection> relatedConnections = new ArrayList<>();
		for (Connection conn : topology.getConnections()) {
			if (isDeviceInConnection(device, conn)) {
				relatedConnections.add(conn);
			}
		}
		return relatedConnections;
	}

	public boolean isDeviceInConnection(Device device, Connection connection) {
		return device.getInterfaces().contains(connection.interfaceA()) ||
				device.getInterfaces().contains(connection.interfaceB());
	}

	public List<NetworkInterface> getAvailableInterfaces(Device device) {
		List<NetworkInterface> allInterfaces = new ArrayList<>(device.getInterfaces());

		return allInterfaces.stream()
				.filter(iface -> topology.getConnections().stream()
						.noneMatch(conn -> conn.interfaceA().equals(iface) || conn.interfaceB().equals(iface)))
				.toList();
	}

	public Device findDevice(NetworkInterface iface) {
		return topology.findDeviceByInterface(iface);
	}

	public Device getDevice(DeviceId id) {
		return topology.getDevice(id);
	}

	public Connection getConnection(ConnectionId id) {
		for (Connection conn : topology.getConnections()) {
			if (conn.id().equals(id)) return conn;
		}
		return null;
	}
}