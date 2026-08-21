package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.exceptions.*;

public class CLIErrorHandler {
	private CLIErrorHandler() {
	}

	public static String handleException(RuntimeException e, String input) {
		String configPath = input.trim().replaceFirst("^(set|delete)\\s+", "");
		String message = e.getMessage();

		if (e instanceof NoChangesToCommitException) {
			return "No configuration changes to commit\n[edit]";
		}

		if (e instanceof UncommittedChangesException) {
			return message;
		}

		if (e instanceof InvalidNextHopException || e instanceof InvalidSubnetException) {
			String val = message.replace(" is not a valid IPv4 prefix", "");
			return String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val);
		}

		if (e instanceof DuplicateConfigurationException) {
			return "\tConfiguration path: [%s] already exists".formatted(configPath);
		}

		if (e instanceof ConfigurationNotFoundException) {
			if ("Route not found".equals(message)) {
				return "\tConfiguration path: [%s] does not exist".formatted(configPath);
			}
			if ("Nothing to delete".equals(message)) {
				return "\tNothing to delete (the specified node does not exist)";
			}
			if ("No value to delete".equals(message)) {
				return "\tNothing to delete (the specified value does not exist)";
			}
			return "\t" + message;
		}

		if ("Route is already disabled".equals(message)) {
			return "\tConfiguration path: [%s] is already disabled".formatted(configPath);
		}

		if (e instanceof InvalidAddressException) {
			if (message != null && message.endsWith("is not a valid IPv4 prefix")) {
				String val = message.replace(" is not a valid IPv4 prefix", "");
				return String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val);
			}
			String[] command = configPath.split(" ");
			String ipWithMask = command[command.length - 1];
			if (message != null && message.contains("Cannot assign network address")) {
				return parseNetworkAddress(ipWithMask);
			}
			if (message != null && message.contains("Cannot assign broadcast address")) {
				return parseBroadcastAddress(ipWithMask);
			}
			return "\tError: Invalid IP address\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed";
		}

		if (e instanceof InterfaceNotFoundException) {
			return "\t%s".formatted(message);
		}

		if (message != null && message.startsWith("Configuration path: [interfaces ethernet")) {
			return "\t%s".formatted(message);
		}

		if (e instanceof InvalidModeException) {
			return message;
		}

		return message;
	}

	private static String parseBroadcastAddress(String ipWithMask) {
		String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
		String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
		String[] octets = ip.split("\\.");
		int lastOctet = Integer.parseInt(octets[3]);
		String suggestedIp = String.format("%s.%s.%s.%d", octets[0], octets[1], octets[2], lastOctet - 1);
		return String.format("""
				\t%s is the broadcast address for this subnet
				\tBroadcast addresses cannot be assigned to interfaces
				\tUse a host address from this subnet (e.g., %s/%s)""", ip, suggestedIp, mask);
	}

	private static String parseNetworkAddress(String ipWithMask) {
		String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
		String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
		return String.format("""
				\t%s is the network address for this subnet
				\tNetwork addresses cannot be assigned to interfaces
				\tUse a host address from this subnet (e.g., %s.1/%s)""", ip, ip.substring(0, ip.lastIndexOf('.')), mask);
	}
}