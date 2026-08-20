package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.*;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.ArrayList;
import java.util.List;

public class TopologyApplicationService {

	private final NetworkTopology topology;

	public TopologyApplicationService(NetworkTopology topology) {
		this.topology = topology;
	}

	public Router addRouter(String name, int numInterfaces) {
		List<RouterInterface> interfaces = new ArrayList<>();
		for (int i = 0; i < numInterfaces; i++) {
			interfaces.add(new RouterInterface("eth" + i));
		}
		Router router = new Router(name, interfaces);
		topology.addDevice(router);
		return router;
	}

	public Switch addSwitch(String name, int numPorts) {
		List<SwitchPort> ports = new ArrayList<>();
		for (int i = 0; i < numPorts; i++) {
			ports.add(new SwitchPort("GigabitEthernet0/" + (i + 1)));
		}
		Switch sw = new Switch(name, ports);
		topology.addDevice(sw);
		return sw;
	}

	public Host addHost(String name, String ipText, String maskText, String gatewayText) {
		IPAddress ip = IPAddress.fromString(ipText);
		SubnetMask mask = new SubnetMask(Integer.parseInt(maskText));

		IPAddress gateway = null;
		if (gatewayText != null && !gatewayText.trim().isEmpty()) {
			gateway = IPAddress.fromString(gatewayText);
		}

		HostInterface hostInterface = new HostInterface(
				"Ethernet0",
				new InterfaceAddress(ip, mask),
				gateway
		);
		Host host = new Host(name, hostInterface);
		topology.addDevice(host);
		return host;
	}

	public void removeDevice(DeviceId deviceId) {
		topology.removeDevice(deviceId);
	}

	public Connection addConnection(NetworkInterface start, NetworkInterface end) {
		Connection connection = new Connection(start, end);
		topology.addConnection(connection);
		return connection;
	}

	public void removeConnection(Connection connection) {
		topology.removeConnection(connection);
	}
}