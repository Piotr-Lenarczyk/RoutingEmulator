package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;
import org.uj.routingemulator.router.InterfaceType;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	private static RouterInterface findInterfaceWithSubnet(Router srcRouter, RouterInterface ri) {
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getType() == InterfaceType.ETHERNET) {
				return candidate;
			}
		}
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getType() == InterfaceType.VIF) {
				return candidate;
			}
		}
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getType() == InterfaceType.DUMMY) {
				return candidate;
			}
		}
		return ri;
	}

	private static IPAddress findSourceIp(RouterInterface ri) {
		IPAddress sourceIp;
		if (ri.getInterfaceAddress() != null && ri.getInterfaceAddress().ipAddress() != null) {
			sourceIp = ri.getInterfaceAddress().ipAddress();
		} else {
			sourceIp = ri.getSubnet().networkAddress();
		}
		return sourceIp;
	}

	private static RouterInterface findExitInterfaceFromRoutingTable(Router srcRouter, IPAddress dst, RouterInterface ri) {
		Optional<StaticRoutingEntry> matchedRoute = srcRouter.getRoutingTable().getRoutingEntries().stream()
				.filter(e -> !e.isDisabled() && e.getSubnet() != null && e.getSubnet().contains(dst))
				.findFirst();

		if (matchedRoute.isPresent()) {
			StaticRoutingEntry route = matchedRoute.get();
			if (route.getRouterInterface() != null) {
				ri = route.getRouterInterface();
			} else if (route.getNextHop() != null) {
				for (RouterInterface candidate : srcRouter.getInterfaces()) {
					if (candidate.getSubnet() != null && candidate.getSubnet().contains(route.getNextHop())) {
						ri = candidate;
						break;
					}
				}
			}
		}
		return ri;
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
		if (hi.getSubnet() != null) {
			sourceIp = hi.getSubnet().networkAddress();
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
				logger.finest("Probe %d failed internally with: %s after %d hops".formatted(seq, outcome.reason(), outcome.hopCount()));
				String displayReason = determinePingErrorMessage(outcome.reason(), srcAddr, topology);
				results.add(new PingResult(seq, false, outcome.hopCount(), 0, displayReason));
			}
		}
		return new PingStatistics(results);
	}

	public PingStatistics ping(Router srcRouter, IPAddress dst, int count, int ttl, NetworkTopology topology) {
		logger.fine("%s: Router pinging %s with %d probes (ttl=%d)...".formatted(srcRouter.getName(), dst, count, ttl));
		List<PingResult> results = new ArrayList<>();
		if (count <= 0) count = 4;
		if (ttl <= 0) ttl = 64;

		RouterInterface ri = null;
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getSubnet().contains(dst)) {
				ri = candidate;
				break;
			}
		}

		if (ri == null) {
			ri = findExitInterfaceFromRoutingTable(srcRouter, dst, ri);
			if (ri == null) {
				ri = findInterfaceWithSubnet(srcRouter, ri);
			}
		}

		// Implementation of VyOS behavior for routers:
		// If the router itself does not have ANY route capable of transmitting the ping,
		// it throws a system error (Network unreachable) immediately rather than printing pings.
		if (ri == null) {
			throw new RuntimeException("connect: Network is unreachable");
		}

		IPAddress sourceIp = null;
		if (ri.getSubnet() != null) {
			sourceIp = findSourceIp(ri);
		}

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
			logger.finest("Probe %d failed internally with: %s after %d hops".formatted(seq, outcome.reason(), outcome.hopCount()));
			String displayReason = determinePingErrorMessage(outcome.reason(), srcAddr, topology);
			results.add(new PingResult(seq, false, outcome.hopCount(), 0, displayReason));
		}
	}

	/**
	 * Translates internal forwarding engine errors into authentic user-facing ICMP error messages.
	 */
	private String determinePingErrorMessage(String internalReason, IPAddress sourceIp, NetworkTopology topology) {
		if ("Host not found on connected subnet".equals(internalReason)) {
			return "Destination Host Unreachable";
		}

		if ("No return route".equals(internalReason) || "TTL expired".equals(internalReason) || "Traffic via egress dummy interface discarded".equals(internalReason)) {
			return ""; // Represents timeout in the formatter
		}

		if ("No route".equals(internalReason)
				|| "Neighbor router not found".equals(internalReason)
				|| "Next-hop not found".equals(internalReason)
				|| "Exit interface administratively down".equals(internalReason)
				|| "Exit interface not connected".equals(internalReason)
				|| "Next-hop not found in topology".equals(internalReason)
				|| "Unsupported neighbor type".equals(internalReason)) {

			boolean canRouteBack = isSourceReachable(sourceIp, topology);
			if (canRouteBack) {
				return "Destination Net Unreachable";
			} else {
				return ""; // Represents timeout
			}
		}

		return ""; // Represents timeout
	}

	private boolean isSourceReachable(IPAddress sourceIp, NetworkTopology topology) {
		for (Router router : topology.getRouters()) {
			for (RouterInterface ri : router.getInterfaces()) {
				if (ri.getSubnet() != null && ri.getSubnet().contains(sourceIp)) {
					return true;
				}
			}
			for (StaticRoutingEntry entry : router.getRoutingTable().getRoutingEntries()) {
				if (!entry.isDisabled() && entry.getSubnet().contains(sourceIp)) {
					return true;
				}
			}
		}
		return false;
	}
}