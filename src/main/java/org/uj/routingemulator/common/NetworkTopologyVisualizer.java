package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.switching.Switch;

public class NetworkTopologyVisualizer {
	private NetworkTopologyVisualizer() {
	}

	public static String visualize(NetworkTopology topology) {
		final String EXTENDER = " ";
		StringBuilder sb = new StringBuilder();
		sb.append("=== Network Topology ===\n\n");

		sb.append("Hosts:\n");
		for (Host host : topology.getHosts()) {
			sb.append("  %s ".formatted(EXTENDER)).append(host.getHostname()).append("\n");
			sb.append("        Interface: ").append(host.getHostInterface().getInterfaceName()).append("\n");
			if (host.getHostInterface().getInterfaceAddress() != null) {
				sb.append("        IP: ").append(host.getHostInterface().getInterfaceAddress()).append("\n");
			}
			sb.append("      %s Gateway: ".formatted(EXTENDER)).append(host.getHostInterface().getDefaultGateway()).append("\n\n");
		}

		sb.append("Switches:\n");
		for (Switch sw : topology.getSwitches()) {
			sb.append("  %s ".formatted(EXTENDER)).append(sw.getName()).append("\n");
			sb.append("        Ports: ");
			sb.append(sw.getPorts().stream()
					.map(NetworkInterface::getInterfaceName)
					.reduce((a, b) -> a + ", " + b)
					.orElse("none"));
			sb.append("\n\n");
		}

		sb.append("Routers:\n");
		for (Router router : topology.getRouters()) {
			sb.append("    ").append(router.getName()).append("\n");
			sb.append("        Interfaces: ");
			sb.append(router.getInterfaces().stream()
					.map(iface -> iface.getInterfaceName() + (iface.getSubnet() != null ? " (" + iface.getSubnet().networkAddress() + "/" + iface.getSubnet().subnetMask() + ")" : " (unconfigured)"))
					.reduce((a, b) -> a + ", " + b)
					.orElse("none"));
			sb.append("\n\n");
		}

		sb.append("Connections:\n");
		for (Connection conn : topology.getConnections()) {
			String deviceA = DeviceLookup.getDeviceName(conn.interfaceA(), topology);
			String deviceB = DeviceLookup.getDeviceName(conn.interfaceB(), topology);
			sb.append("  ").append(deviceA)
					.append("[").append(conn.interfaceA().getInterfaceName()).append("]")
					.append(" <──> ")
					.append(deviceB)
					.append("[").append(conn.interfaceB().getInterfaceName()).append("]")
					.append("\n");
		}

		return sb.toString();
	}
}