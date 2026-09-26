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

	/**
	 * Ping an IP address from the given host. Destination may be given as string.
	 */
	public PingStatistics ping(Host src, String dstIpString, int count, NetworkTopology topology) {
		logger.fine("%s: Pinging %s with %d probes...".formatted(src.getHostname(), dstIpString, count));
		IPAddress dst;
		try {
			dst = IPAddress.fromString(dstIpString);
		} catch (RuntimeException e) {
			// Invalid destination IP - return 'count' failed probes with clear reason
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
		// Priority 1: Standard physical interface
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getType() == InterfaceType.ETHERNET) {
				return candidate;
			}
		}
		// Priority 2: VIF sub-interface
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getType() == InterfaceType.VIF) {
				return candidate;
			}
		}
		// Priority 3: Fallback to a dummy interface
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
				// try to infer which local interface would be used to reach next-hop (next-hop lies in one of router's subnets)
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

	/**
	 * Ping using IPAddress object.
	 */
	public PingStatistics ping(Host src, IPAddress dst, int count, NetworkTopology topology) {
		logger.fine("%s: Pinging %s with %d probes...".formatted(src.getHostname(), dst, count));
		List<PingResult> results = new ArrayList<>();
		if (count <= 0) count = 4;

		// Validate source host interface
		HostInterface hi = src.getHostInterface();
		if (hi == null) {
			for (int i = 1; i <= count; i++) {
				logger.finest("Probe %d failed: Source host has no interface".formatted(i));
				results.add(new PingResult(i, false, 0, 0, "Source host has no interface"));
			}
			return new PingStatistics(results);
		}

		// Determine a sensible source IP for the packet: prefer interface's configured IP if available
		IPAddress sourceIp = null;
		if (hi.getSubnet() != null) {
			// Note: HostInterface stores a Subnet object. Tests currently initialize it with the
			// interface IP as the networkAddress field (legacy). Use that address as the source.
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

				// Translate internal engine reason to authentic VyOS ping output
				String displayReason = determinePingErrorMessage(outcome.reason(), p.getSource(), topology);
				results.add(new PingResult(seq, false, outcome.hopCount(), 0, displayReason));
			}
		}
		return new PingStatistics(results);
	}

	/**
	 * Ping using Router as source. This delegates to the forwarding engine similarly to host-based pings.
	 */
	public PingStatistics ping(Router srcRouter, IPAddress dst, int count, int ttl, NetworkTopology topology) {
		logger.fine("%s: Router pinging %s with %d probes (ttl=%d)...".formatted(srcRouter.getName(), dst, count, ttl));
		List<PingResult> results = new ArrayList<>();
		if (count <= 0) count = 4;
		if (ttl <= 0) ttl = 64;

		// Select a source IP from router interfaces. Prefer an interface that shares subnet with destination.
		RouterInterface ri = null;
		for (RouterInterface candidate : srcRouter.getInterfaces()) {
			if (candidate.getSubnet() != null && candidate.getSubnet().contains(dst)) {
				ri = candidate;
				break;
			}
		}

		if (ri == null) {
			// If no local interface contains the destination, consult routing table to determine exit interface
			ri = findExitInterfaceFromRoutingTable(srcRouter, dst, ri);

			// fallback: pick first interface with a subnet
			if (ri == null) {
				ri = findInterfaceWithSubnet(srcRouter, ri);
			}
		}

		IPAddress sourceIp = null;
		if (ri != null && ri.getSubnet() != null) {
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

			// Translate internal engine reason to authentic VyOS ping output
			String displayReason = determinePingErrorMessage(outcome.reason(), p.getSource(), topology);
			results.add(new PingResult(seq, false, outcome.hopCount(), 0, displayReason));
		}
	}

	/**
	 * Translates internal forwarding engine errors into authentic user-facing ICMP error messages.
	 */
	private String determinePingErrorMessage(String internalReason, IPAddress sourceIp, NetworkTopology topology) {
		// If the packet reached the correct subnet, but no host has that exact IP
		if ("Host not found on connected subnet".equals(internalReason)) {
			return "Destination Host Unreachable";
		}

		// If the packet was lost because the destination couldn't route back
		if ("No return route".equals(internalReason) || "TTL expired".equals(internalReason)) {
			return "Request Timed Out";
		}

		// If the forward path failed (router dropped it)
		if ("No route".equals(internalReason)
				|| "Neighbor router not found".equals(internalReason)
				|| "Next-hop not found".equals(internalReason)
				|| "Exit interface administratively down".equals(internalReason)
				|| "Exit interface not connected".equals(internalReason)) {

			// To send "Destination Net Unreachable", the dropping router must know how to reach the source.
			// Since our engine doesn't explicitly return WHICH router dropped the packet in the outcome,
			// we simulate this by checking if the source IP is globally reachable in the topology.
			// In a real network, the dropping router uses its own routing table. Here we use a heuristic:
			// if we can't find a path back from *anywhere*, it's a timeout.

			boolean canRouteBack = isSourceReachable(sourceIp, topology);

			if (canRouteBack) {
				return "Destination Net Unreachable";
			} else {
				return "Request Timed Out";
			}
		}

		// Catch-all for any other weird errors
		return "Request Timed Out";
	}

	/**
	 * Simple heuristic to determine if the source IP is generally reachable on the network.
	 * Used to decide between "Net Unreachable" and "Timed Out".
	 */
	private boolean isSourceReachable(IPAddress sourceIp, NetworkTopology topology) {
		// Check if ANY router in the topology has a route to the source IP
		for (Router router : topology.getRouters()) {
			// Check connected subnets
			for (RouterInterface ri : router.getInterfaces()) {
				if (ri.getSubnet() != null && ri.getSubnet().contains(sourceIp)) {
					return true;
				}
			}

			// Check static routes
			for (StaticRoutingEntry entry : router.getRoutingTable().getRoutingEntries()) {
				if (!entry.isDisabled() && entry.getSubnet().contains(sourceIp)) {
					return true;
				}
			}
		}
		return false;
	}
}