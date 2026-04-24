package org.uj.routingemulator.common;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formats PingStatistics into VyOS-like textual output.
 */
public class PingFormatter {
    /**
     * Formats ping results into human-readable VyOS-style output.
     *
     * @param dst   Destination IPAddress
     * @param src   Source IPAddress used for From lines
     * @param ttl   TTL used for the probes
     * @param stats PingStatistics object
     * @return formatted string (multi-line)
     */
    public static String format(IPAddress dst, IPAddress src, int ttl, PingStatistics stats) {
        StringBuilder sb = new StringBuilder();
        String dstStr = dst.toString();
        String srcStr = src != null ? src.toString() : "0.0.0.0";

        sb.append("PING ").append(dstStr).append(" (").append(dstStr).append("): 56(84) bytes of data.\n");

        List<PingResult> results = stats.getResults();

        for (PingResult r : results) {
            if (r.isSuccess()) {
                sb.append(String.format("64 bytes from %s: icmp_seq=%d ttl=%d time=%dms\n", dstStr, r.getSequence(), ttl, r.getRttMs()));
            } else {
                String reason = r.getErrorMessage();
                if (reason == null || reason.isEmpty()) {
                    reason = "Destination Host Unreachable";
                }
                sb.append(String.format("From %s icmp_seq=%d %s\n", srcStr, r.getSequence(), reason));
            }
        }

        sb.append("\n--- ").append(dstStr).append(" ping statistics ---\n");
        int transmitted = stats.getSent();
        int received = stats.getReceived();
        long errors = transmitted - received; // simple
        double loss = transmitted == 0 ? 100.0 : (100.0 * (transmitted - received) / transmitted);
        sb.append(String.format("%d packets transmitted, %d received, %s errors, %.0f%% packet loss, time %dms\n",
                transmitted, received, (errors > 0 ? "+" + errors : "0"), loss, 0));

        // rtt stats
        List<Long> rtts = results.stream().filter(PingResult::isSuccess).map(PingResult::getRttMs).collect(Collectors.toList());
        if (!rtts.isEmpty()) {
            DoubleSummaryStatistics stat = rtts.stream().mapToDouble(Long::doubleValue).summaryStatistics();
            double min = stat.getMin();
            double avg = stat.getAverage();
            double max = stat.getMax();
            double mdev = Math.sqrt(rtts.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0));
            sb.append(String.format("rtt min/avg/max/mdev = %.2f/%.2f/%.2f/%.2f ms\n", min, avg, max, mdev));
        }

        return sb.toString();
    }
}

