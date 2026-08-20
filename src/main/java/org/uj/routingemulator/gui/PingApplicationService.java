package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.common.PingStatistics;
import org.uj.routingemulator.host.Host;

public class PingApplicationService {
	private final NetworkTopology topology;

	public PingApplicationService(NetworkTopology topology) {
		this.topology = topology;
	}

	public PingStatistics pingFromHost(Host host, String targetIp) {
		return host.ping(targetIp, topology);
	}
}