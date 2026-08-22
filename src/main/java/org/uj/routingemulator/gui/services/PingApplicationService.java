package org.uj.routingemulator.gui.services;

import org.uj.routingemulator.common.ping.PingService;
import org.uj.routingemulator.common.ping.PingStatistics;
import org.uj.routingemulator.common.topology.NetworkTopology;
import org.uj.routingemulator.host.Host;

public class PingApplicationService {
	private final NetworkTopology topology;

	public PingApplicationService(NetworkTopology topology) {
		this.topology = topology;
	}

	public PingStatistics pingFromHost(Host host, String targetIp) {
		return new PingService().ping(host, targetIp, 4, topology);
	}
}