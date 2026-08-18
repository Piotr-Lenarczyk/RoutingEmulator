package org.uj.routingemulator.common;

import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.switching.Switch;
import org.uj.routingemulator.switching.SwitchPort;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class TopologyGraphSearch {
	private TopologyGraphSearch() {
	}

	public static HostInterface findHostInterfaceByIpConnectedToInterface(NetworkTopology topology, NetworkInterface start, IPAddress ip) {
		Queue<NetworkInterface> q = new ArrayDeque<>();
		Set<NetworkInterface> visited = new HashSet<>();
		q.add(start);
		visited.add(start);

		while (!q.isEmpty()) {
			NetworkInterface cur = q.remove();

			if (cur instanceof HostInterface hif && hasHostIp(hif, ip)) {
				return hif;
			}

			if (cur instanceof SwitchPort sp) {
				addSwitchPorts(topology, sp, visited, q);
			}

			NetworkInterface neighbor = processInterface(topology, cur);
			if (neighbor == null) continue;

			if (!visited.contains(neighbor)) {
				visited.add(neighbor);

				if (neighbor instanceof HostInterface hif && hasHostIp(hif, ip)) {
					return hif;
				}
				q.add(neighbor);
			}
		}
		return null;
	}

	private static boolean hasHostIp(HostInterface hostInterface, IPAddress ip) {
		return hostInterface.getInterfaceAddress() != null && hostInterface.getInterfaceAddress().ipAddress().equals(ip);
	}

	private static void addSwitchPorts(NetworkTopology topology, SwitchPort sp, Set<NetworkInterface> visited, Queue<NetworkInterface> q) {
		for (Switch sw : topology.getSwitches()) {
			if (sw.containsPort(sp)) {
				for (SwitchPort sibling : sw.getPorts()) {
					if (!visited.contains(sibling)) {
						visited.add(sibling);
						q.add(sibling);
					}
				}
				break;
			}
		}
	}

	private static NetworkInterface processInterface(NetworkTopology topology, NetworkInterface cur) {
		Connection c = topology.getConnectionForInterface(cur);
		if (c == null) return null;
		return c.getNeighborInterface(cur);
	}
}