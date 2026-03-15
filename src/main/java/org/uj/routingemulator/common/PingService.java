package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple PingService: host-only L3 ping using ForwardingEngine. RTT is mocked deterministically.
 */
public class PingService {
    private static final long BASE_MS = 1;
    private static final long PER_HOP_MS = 1;

    private final ForwardingEngine engine = new ForwardingEngine();

    /**
     * Ping an IP address from the given host. Destination may be given as string.
     *
     * @param src         source host
     * @param dstIpString destination IP in dotted format
     * @param count       number of probes
     * @param topology    network topology
     * @return PingStatistics with results
     */
    public PingStatistics ping(Host src, String dstIpString, int count, NetworkTopology topology) {
        IPAddress dst;
        try {
            dst = IPAddress.fromString(dstIpString);
        } catch (RuntimeException e) {
            // Invalid destination IP - return 'count' failed probes with clear reason
            List<PingResult> failures = new ArrayList<>();
            for (int i = 1; i <= Math.max(1, count); i++) {
                failures.add(new PingResult(i, false, 0, 0, "Invalid destination IP: " + dstIpString));
            }
            return new PingStatistics(failures);
        }
        return ping(src, dst, count, topology);
    }

    /**
     * Ping using IPAddress object.
     */
    public PingStatistics ping(Host src, IPAddress dst, int count, NetworkTopology topology) {
        List<PingResult> results = new ArrayList<>();

        if (count <= 0) count = 4;

        // Validate source host interface
        HostInterface hi = src.getHostInterface();
        if (hi == null) {
            for (int i = 1; i <= count; i++) {
                results.add(new PingResult(i, false, 0, 0, "Source host has no interface"));
            }
            return new PingStatistics(results);
        }

        // Determine a sensible source IP for the packet: prefer interface's configured IP if available
        IPAddress sourceIp = null;
        if (hi.getSubnet() != null) {
            // Note: HostInterface stores a Subnet object. Tests currently initialize it with the
            // interface IP as the networkAddress field (legacy). Use that address as the source.
            sourceIp = hi.getSubnet().getNetworkAddress();
        }

        for (int seq = 1; seq <= count; seq++) {
            IPAddress srcAddr = sourceIp != null ? sourceIp : new IPAddress(0, 0, 0, 0);
            Packet p = new Packet(srcAddr, dst, Packet.PacketType.ICMP_ECHO_REQUEST, 64);
            ForwardingOutcome outcome = engine.forward(p, src, topology);
            if (outcome.isReached()) {
                long rtt = BASE_MS + outcome.getHopCount() * PER_HOP_MS;
                results.add(new PingResult(seq, true, outcome.getHopCount(), rtt, null));
            } else {
                results.add(new PingResult(seq, false, outcome.getHopCount(), 0, outcome.getReason()));
            }
        }

        return new PingStatistics(results);
    }
}
