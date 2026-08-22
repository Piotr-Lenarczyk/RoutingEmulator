package org.uj.routingemulator.common.topology;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.model.Router;
import org.uj.routingemulator.switching.Switch;

public class NetworkTopologyVisualizer {

	private NetworkTopologyVisualizer() {
	}

	public static String visualize(NetworkTopology topology) {
		final String EXTENDER = " ";
		StringBuilder sb = new StringBuilder();

		sb.append("=== Network Topology ===\n\n");

		sb.append("Hosts:\n");
		for (Device d : topology.devices()) {
			if (d instanceof Host host) {
				sb.append("  %s ".formatted(EXTENDER)).append(host.getHostname()).append("\n");
				sb.append("        Interface: ").append(host.getHostInterface().getInterfaceName()).append("\n");
				if (host.getHostInterface().getInterfaceAddress() != null) {
					sb.append("        IP: ").append(host.getHostInterface().getInterfaceAddress()).append("\n");
				}
				sb.append("      %s Gateway: ".formatted(EXTENDER)).append(host.getHostInterface().getDefaultGateway()).append("\n\n");
			}
		}

		sb.append("Switches:\n");
		for (Device d : topology.devices()) {
			if (d instanceof Switch sw) {
				sb.append("  %s ".formatted(EXTENDER)).append(sw.getName()).append("\n");
				sb.append("        Ports: ");
				sb.append(sw.getPorts().stream()
						.map(NetworkInterface::getInterfaceName)
						.reduce((a, b) -> a + ", " + b)
						.orElse("none"));
				sb.append("\n\n");
			}
		}

		sb.append("Routers:\n");
		for (Device d : topology.devices()) {
			if (d instanceof Router router) {
				sb.append("    ").append(router.getName()).append("\n");
				sb.append("        Interfaces: ");
				sb.append(router.getInterfaces().stream()
						.map(iface -> iface.getInterfaceName() + (iface.getSubnet() != null ? " (" + iface.getSubnet().networkAddress() + "/" + iface.getSubnet().subnetMask() + ")" : " (unconfigured)"))
						.reduce((a, b) -> a + ", " + b)
						.orElse("none"));
				sb.append("\n\n");
			}
		}

		sb.append("Connections:\n");
		for (Connection conn : topology.connections()) {
			String deviceA = DeviceLookup.getDeviceName(conn.interfaceA(), topology);
			String deviceB = DeviceLookup.getDeviceName(conn.interfaceB(), topology);

			sb.append("  ").append(deviceA)
					.append("[").append(conn.interfaceA().getInterfaceName()).append("]")
					.append(" < > ")
					.append(deviceB)
					.append("[").append(conn.interfaceB().getInterfaceName()).append("]")
					.append("\n");
		}

		return sb.toString();
	}
}