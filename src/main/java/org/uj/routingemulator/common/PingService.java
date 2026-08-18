package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Simple PingService: host-only L3 ping using ForwardingEngine. RTT is mocked deterministically.
 */
public class PingService {
	private static final Logger logger = Logger.getLogger(PingService.class.getName());
	private static final long BASE_MS = 1;
	private static final long PER_HOP_MS = 1;

	private final ForwardingEngine engine = new ForwardingEngine();

	public PingStatistics ping(Host src, String dstIpString, int count, NetworkTopology topology) {
		logger.fine("%s: Pinging %s with %d probes...".formatted(src.getHostname(), dstIpString, count));
		IPAddress dst;
		try {
			dst = IPAddress.fromString(dstIpString);
		} catch (RuntimeException e) {
			List<PingResult> failures = new ArrayList<>();
			for (int i = 1; i <= Math.max(1, count); i++) {
				logger.finest("Probe %d failed: Invalid destination IP: %s".formatted(i, dstIpString));
				failures.add(new PingResult(i, false, 0, 0, "Invalid destination IP: " + dstIpString));
			}
			return new PingStatistics(failures);
		}
		return ping(src, dst, count, topology);
	}

	public PingStatistics ping(Host src, IPAddress dst, int count, NetworkTopology topology) {
		logger.fine("%s: Pinging %s with %d probes...".formatted(src.getHostname(), dst, count));
		List<PingResult> results = new ArrayList<>();
		if (count <= 0) count = 4;

		HostInterface hi = src.getHostInterface();
		if (hi == null) {
			for (int i = 1; i <= count; i++) {
				logger.finest("Probe %d failed: Source host has no interface".formatted(i));
				results.add(new PingResult(i, false, 0, 0, "Source host has no interface"));
			}
			return new PingStatistics(results);
		}

		IPAddress sourceIp = null;
		if (hi.getInterfaceAddress() != null) {
			sourceIp = hi.getInterfaceAddress().ipAddress();
		}

		for (int seq = 1; seq <= count; seq++) {
			IPAddress srcAddr = sourceIp != null ? sourceIp : new IPAddress(0, 0, 0, 0);
			logger.finest("Probe %d: Sending ICMP Echo Request from %s to %s".formatted(seq, srcAddr, dst));
			Packet p = new Packet(srcAddr, dst, Packet.PacketType.ICMP_ECHO_REQUEST, 64);
			logger.finest("Forwarding packet %s to destination %s".formatted(p, dst));

			ForwardingOutcome outcome = engine.forward(p, src, topology);
			if (outcome.reached()) {
				long rtt = BASE_MS + outcome.hopCount() * PER_HOP_MS;
				logger.finest("Probe %d succeeded: Reached destination in %d ms with %d hops".formatted(seq, rtt, outcome.hopCount()));
				results.add(new PingResult(seq, true, outcome.hopCount(), rtt, null));
			} else {
				logger.finest("Probe %d failed: %s after %d hops".formatted(seq, outcome.reason(), outcome.hopCount()));
				results.add(new PingResult(seq, false, outcome.hopCount(), 0, outcome.reason()));
			}
		}
		return new PingStatistics(results);
	}

	public PingStatistics ping(Router srcRouter, IPAddress dst, int count, int ttl, NetworkTopology topology) {
		logger.fine("%s: Router pinging %s with %d probes (ttl=%d)...".formatted(srcRouter.getName(), dst, count, ttl));
		List<PingResult> results = new ArrayList<>();
		if (count <= 0) count = 4;
		if (ttl <= 0) ttl = 64;

		// Select a source IP using the new RouteSelector
		RouterInterface ri = RouteSelector.determineExitInterface(srcRouter, dst);
		if (ri == null) {
			// fallback: pick first interface with a subnet
			ri = srcRouter.getInterfaces().stream()
					.filter(candidate -> candidate.getSubnet() != null)
					.findFirst()
					.orElse(null);
		}

		IPAddress sourceIp = RouteSelector.determineSourceIp(ri);

		for (int seq = 1; seq <= count; seq++) {
			performPing(srcRouter, dst, ttl, topology, sourceIp, seq, results);
		}
		return new PingStatistics(results);
	}

	private void performPing(Router srcRouter, IPAddress dst, int ttl, NetworkTopology topology, IPAddress sourceIp, int seq, List<PingResult> results) {
		IPAddress srcAddr = sourceIp != null ? sourceIp : new IPAddress(0, 0, 0, 0);
		logger.finest("Probe %d: Router %s sending ICMP Echo Request from %s to %s with ttl=%d".formatted(seq, srcRouter.getName(), srcAddr, dst, ttl));
		Packet p = new Packet(srcAddr, dst, Packet.PacketType.ICMP_ECHO_REQUEST, ttl);
		ForwardingOutcome outcome = engine.forward(p, srcRouter, topology);

		if (outcome.reached()) {
			long rtt = BASE_MS + outcome.hopCount() * PER_HOP_MS;
			results.add(new PingResult(seq, true, outcome.hopCount(), rtt, null));
		} else {
			results.add(new PingResult(seq, false, outcome.hopCount(), 0, outcome.reason()));
		}
	}
}