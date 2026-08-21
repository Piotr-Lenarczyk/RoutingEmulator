package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.RouterMode;

import java.util.Optional;
import java.util.regex.Pattern;

public class PingCommand implements RouterCommand {
    private static final CommandSyntax SYNTAX = new CommandSyntax("ping [-c <count>] [-t <ttl>] <ip>");
    private static final String PING_ERROR = "ping: %s: System error";
    private static final Pattern MASK_PATTERN = Pattern.compile(".*/\\d{1,2}$");

    @Override
    public CommandSyntax getSyntax() {
        return SYNTAX;
    }

    @Override
    public Optional<ParsedCommand> parse(String input) {
        if (input == null) return Optional.empty();
        String t = input.trim();
        if (!t.startsWith("ping")) return Optional.empty();
        return Optional.of(new Invocation(t));
    }

    @Override
    public String getDescription() {
        return "Send ICMP Echo Requests to an IPv4 address";
    }

    private record Invocation(String rawInput) implements ParsedCommand {
        @Override
        public CommandResult execute(CommandExecutionContext context) {
            if (context.router().getMode() != RouterMode.OPERATIONAL) {
                return new CommandFailure("Invalid command: ping");
            }

            String[] parts = rawInput.split("\\s+");
            int count = 4;
            int ttl = 64;
            String ipArg = null;

            int i = 1;
            while (i < parts.length) {
                String p = parts[i];
                if ("-c".equals(p) && i + 1 < parts.length) {
                    try {
                        count = Integer.parseInt(parts[++i]);
                    } catch (NumberFormatException e) {
                        return new CommandFailure("Invalid count value");
                    }
                } else if ("-t".equals(p) && i + 1 < parts.length) {
                    try {
                        ttl = Integer.parseInt(parts[++i]);
                    } catch (NumberFormatException e) {
                        return new CommandFailure("Invalid ttl value");
                    }
                } else if (p.startsWith("-")) {
                    return new CommandFailure("Invalid option: " + p);
                } else {
                    ipArg = p;
                }
                i++;
            }

            if (ipArg == null) return new CommandFailure("Invalid command: ping requires target IP");
            if (MASK_PATTERN.matcher(ipArg).matches()) return new CommandFailure(String.format(PING_ERROR, ipArg));
            if (!ipArg.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return new CommandFailure(String.format(PING_ERROR, ipArg));

            IPAddress dst;
            try {
                dst = IPAddress.fromString(ipArg);
            } catch (RuntimeException e) {
                return new CommandFailure(String.format(PING_ERROR, ipArg));
            }

            PingService svc = new PingService();
            NetworkTopology topology = context.topology();
            if (topology == null) return new CommandFailure("ping: no network topology available");

            PingStatistics stats = svc.ping(context.router(), dst, count, ttl, topology);

            IPAddress srcIp = null;
            for (var ri : context.router().getInterfaces()) {
                if (ri.getSubnet() != null) {
                    srcIp = ri.getSubnet().networkAddress();
                    break;
                }
            }
            if (srcIp == null) srcIp = new IPAddress(0, 0, 0, 0);

            String outText = PingFormatter.format(dst, srcIp, ttl, stats);
            return new CommandSuccess(outText);
        }
    }
}