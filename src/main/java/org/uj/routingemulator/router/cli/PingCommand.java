package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterMode;

import java.io.PrintWriter;
import java.util.regex.Pattern;

/**
 * CLI command for ping: ping [-c count] [-t ttl] <IP>
 */
public class PingCommand implements RouterCommand {
    private static final Pattern MASK_PATTERN = Pattern.compile(".*/\\d{1,2}$");
    private String rawInput;

    @Override
    public String getCommandPattern() {
        return "ping [-c <count>] [-t <ttl>] <ip>";
    }

    @Override
    public String getDescription() {
        return "Send ICMP Echo Requests to an IPv4 address";
    }

    @Override
    public boolean matches(String input) {
        if (input == null) return false;
        String t = input.trim();
        if (!t.startsWith("ping")) return false;
        this.rawInput = t;
        return true;
    }

    @Override
    public void execute(Router router) {
        PrintWriter out = CLIContext.getWriter();

        // Only allowed in OPERATIONAL mode
        if (router.getMode() != RouterMode.OPERATIONAL) {
            out.println("Invalid command: ping");
            out.flush();
            return;
        }

        String input = rawInput == null ? "" : rawInput;
        String[] parts = input.trim().split("\\s+");

        int count = 4;
        int ttl = 64;
        String ipArg = null;

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if ("-c".equals(p) && i + 1 < parts.length) {
                try {
                    count = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    out.println("Invalid count value");
                    out.flush();
                    return;
                }
            } else if ("-t".equals(p) && i + 1 < parts.length) {
                try {
                    ttl = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    out.println("Invalid ttl value");
                    out.flush();
                    return;
                }
            } else if (p.startsWith("-")) {
                out.println("Invalid option: " + p);
                out.flush();
                return;
            } else {
                ipArg = p;
            }
        }

        if (ipArg == null) {
            out.println("Invalid command: ping requires target IP");
            out.flush();
            return;
        }

        // Validation: reject masked addresses and invalid IPs with specific message
        if (MASK_PATTERN.matcher(ipArg).matches()) {
            out.println(String.format("ping: %s: System error", ipArg));
            out.flush();
            return;
        }

        // Basic dotted-quad check: simple regex
        if (!ipArg.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            out.println(String.format("ping: %s: System error", ipArg));
            out.flush();
            return;
        }

        IPAddress dst;
        try {
            dst = IPAddress.fromString(ipArg);
        } catch (RuntimeException e) {
            out.println(String.format("ping: %s: System error", ipArg));
            out.flush();
            return;
        }

        // Delegate to PingService adapter
        PingService svc = new PingService();
        NetworkTopology topology = CLIContext.getNetworkTopology();
        if (topology == null) {
            out.println("ping: no network topology available");
            out.flush();
            return;
        }
        PingStatistics stats = svc.ping(router, dst, count, ttl, topology);

        // Find source IP for display
        IPAddress srcIp = null;
        for (var ri : router.getInterfaces()) {
            if (ri.getSubnet() != null) {
                srcIp = ri.getSubnet().getNetworkAddress();
                break;
            }
        }
        if (srcIp == null) srcIp = new IPAddress(0, 0, 0, 0);

        String outText = PingFormatter.format(dst, srcIp, ttl, stats);
        out.print(outText);
        out.flush();
    }
}
