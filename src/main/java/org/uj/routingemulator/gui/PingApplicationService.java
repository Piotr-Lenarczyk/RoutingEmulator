package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.common.PingService;
import org.uj.routingemulator.common.PingStatistics;
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