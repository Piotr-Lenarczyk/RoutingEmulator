package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.common.addressing.InterfaceAddress;
import org.uj.routingemulator.router.exceptions.*;

public class CLIErrorHandler {

	private CLIErrorHandler() {
	}

	public static String handleException(RuntimeException e, String input) {
		String configPath = input.trim().replaceFirst("^(set|delete)\\s+", "");

		if (e instanceof NoChangesToCommitException) {
			return "No configuration changes to commit\n[edit]";
		}
		if (e instanceof UncommittedChangesException) {
			return e.getMessage();
		}
		if (e instanceof InvalidNextHopException) {
			String val = ((InvalidNextHopException) e).getRawInput();
			return String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val);
		}
		if (e instanceof InvalidSubnetException) {
			String val = ((InvalidSubnetException) e).getRawInput();
			return String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val);
		}
		if (e instanceof RouteAlreadyExistsException || e instanceof InterfaceAddressAlreadyConfiguredException
				|| e instanceof RouteAlreadyDisabledException || e instanceof InterfaceAlreadyDisabledException) {
			return "\tConfiguration path: [%s] already exists".formatted(configPath);
		}
		if (e instanceof RouteNotFoundException) {
			if ("Nothing to delete".equals(e.getMessage())) {
				return "\tNothing to delete (the specified node does not exist)";
			}
			return "\tConfiguration path: [%s] does not exist".formatted(configPath);
		}
		if (e instanceof InterfaceAddressNotFoundException) {
			return "\tNothing to delete (the specified value does not exist)";
		}
		if (e instanceof InterfaceAlreadyEnabledException) {
			return "\tNothing to delete (the specified node does not exist)";
		}
		if (e instanceof InvalidAddressException iae) {
			if (iae.getReason() == InvalidAddressException.Reason.NETWORK_ADDRESS) {
				return parseNetworkAddress(iae.getInterfaceAddress(), configPath);
			}
			if (iae.getReason() == InvalidAddressException.Reason.BROADCAST_ADDRESS) {
				return parseBroadcastAddress(iae.getInterfaceAddress(), configPath);
			}
			if (iae.getMessage() != null && iae.getMessage().endsWith("is not a valid IPv4 prefix")) {
				String val = iae.getMessage().replace(" is not a valid IPv4 prefix", "");
				return String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val);
			}
			return "\tError: Invalid IP address\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed";
		}
		if (e instanceof InterfaceNotFoundException) {
			return "\t%s".formatted(e.getMessage());
		}
		if (e instanceof InvalidModeException) {
			return e.getMessage();
		}
		return e.getMessage();
	}

	private static String parseBroadcastAddress(InterfaceAddress interfaceAddress, String configPath) {
		String ip;
		String mask;
		if (interfaceAddress != null) {
			ip = interfaceAddress.ipAddress().toString();
			mask = String.valueOf(interfaceAddress.subnetMask().shortMask());
		} else {
			String[] command = configPath.split(" ");
			String ipWithMask = command[command.length - 1];
			ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
			mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
		}
		String[] octets = ip.split("\\.");
		int lastOctet = Integer.parseInt(octets[3]);
		String suggestedIp = String.format("%s.%s.%s.%d", octets[0], octets[1], octets[2], lastOctet - 1);
		return String.format("""
				\t%s is the broadcast address for this subnet
				\tBroadcast addresses cannot be assigned to interfaces
				\tUse a host address from this subnet (e.g., %s/%s)""", ip, suggestedIp, mask);
	}

	private static String parseNetworkAddress(InterfaceAddress interfaceAddress, String configPath) {
		String ip;
		String mask;
		if (interfaceAddress != null) {
			ip = interfaceAddress.ipAddress().toString();
			mask = String.valueOf(interfaceAddress.subnetMask().shortMask());
		} else {
			String[] command = configPath.split(" ");
			String ipWithMask = command[command.length - 1];
			ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
			mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
		}
		return String.format("""
				\t%s is the network address for this subnet
				\tNetwork addresses cannot be assigned to interfaces
				\tUse a host address from this subnet (e.g., %s.1/%s)""", ip, ip.substring(0, ip.lastIndexOf('.')), mask);
	}
}