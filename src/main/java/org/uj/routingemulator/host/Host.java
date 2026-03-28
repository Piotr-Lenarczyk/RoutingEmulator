package org.uj.routingemulator.host;

import lombok.Data;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.NetworkTopology;
import org.uj.routingemulator.common.PingService;
import org.uj.routingemulator.common.PingStatistics;

import java.util.logging.Logger;

/** Hosts are simplified network endpoints with a single network interface.
 * They can send and receive traffic but do not forward packets.
 */
@Data
public class Host {
	private static final Logger logger = Logger.getLogger(Host.class.getName());
	private String hostname;
	private HostInterface hostInterface;

	/**
	 * Creates a new host with specified hostname and network interface.
	 *
	 * @param hostname the name of the host
	 * @param hostInterface the network interface configuration
	 */
	public Host(String hostname, HostInterface hostInterface) {
		this.hostname = hostname;
		this.hostInterface = hostInterface;
		logger.fine("Creating new host " + hostname + " with interface: " + hostInterface);
	}

	/**
	 * Convenience ping method used by tests and CLI. Sends 4 probes by default.
	 *
	 * @param dst      destination IPv4 string (dotted)
	 * @param topology network topology
	 * @return PingStatistics with results
	 */
	public PingStatistics ping(String dst, NetworkTopology topology) {
		logger.info("Initializing new PingService for host " + hostname);
		PingService svc = new PingService();
		logger.info("%s: Pinging %s with 4 probes...".formatted(hostname, dst));
		return svc.ping(this, dst, 4, topology);
	}

	/**
	 * Convenience ping method with explicit count.
	 */
	public PingStatistics ping(IPAddress dst, int count, NetworkTopology topology) {
		PingService svc = new PingService();
		return svc.ping(this, dst, count, topology);
	}
}
