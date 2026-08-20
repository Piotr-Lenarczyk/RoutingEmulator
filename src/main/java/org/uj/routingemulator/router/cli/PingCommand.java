package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.RouterMode;

import java.util.regex.Pattern;

public class PingCommand implements RouterCommand {
    private static final String PING_ERROR = "ping: %s: System error";
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

    private static PingParameters parsePingParameters(String[] parts, int count, CommandOutput out, int ttl, String ipArg) {
        int i = 1;
        while (i < parts.length) {
            String p = parts[i];
            if ("-c".equals(p) && i + 1 < parts.length) {
                try {
                    count = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    out.println("Invalid count value");
                    return null;
                }
            } else if ("-t".equals(p) && i + 1 < parts.length) {
                try {
                    ttl = Integer.parseInt(parts[++i]);
                } catch (NumberFormatException e) {
                    out.println("Invalid ttl value");
                    return null;
                }
            } else if (p.startsWith("-")) {
                out.println("Invalid option: " + p);
                return null;
            } else {
                ipArg = p;
            }
            i++;
        }
        return new PingParameters(count, ttl, ipArg);
    }

    @Override
    public void execute(CommandExecutionContext context) {
        CommandOutput out = context.output();

        if (context.router().getMode() != RouterMode.OPERATIONAL) {
            out.println("Invalid command: ping");
            return;
        }

        String input = rawInput == null ? "" : rawInput;
        String[] parts = input.trim().split("\\s+");
        int count = 4;
        int ttl = 64;

        PingParameters pingParameters = parsePingParameters(parts, count, out, ttl, null);
        if (pingParameters == null) return;

        if (pingParameters.ipArg() == null) {
            out.println("Invalid command: ping requires target IP");
            return;
        }

        if (MASK_PATTERN.matcher(pingParameters.ipArg()).matches()) {
            out.println(String.format(PING_ERROR, pingParameters.ipArg()));
            return;
        }

        if (!pingParameters.ipArg().matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            out.println(String.format(PING_ERROR, pingParameters.ipArg()));
            return;
        }

        IPAddress dst;
        try {
            dst = IPAddress.fromString(pingParameters.ipArg());
        } catch (RuntimeException e) {
            out.println(String.format(PING_ERROR, pingParameters.ipArg()));
            return;
        }

        PingService svc = new PingService();
        NetworkTopology topology = context.topology();
        if (topology == null) {
            out.println("ping: no network topology available");
            return;
        }

        PingStatistics stats = svc.ping(context.router(), dst, pingParameters.count(), pingParameters.ttl(), topology);

        IPAddress srcIp = null;
        for (var ri : context.router().getInterfaces()) {
            if (ri.getSubnet() != null) {
                srcIp = ri.getSubnet().networkAddress();
                break;
            }
        }
        if (srcIp == null) srcIp = new IPAddress(0, 0, 0, 0);

        String outText = PingFormatter.format(dst, srcIp, pingParameters.ttl(), stats);
        out.print(outText);
    }

    private record PingParameters(int count, int ttl, String ipArg) {
    }
}