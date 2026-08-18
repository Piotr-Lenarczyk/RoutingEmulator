package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.exceptions.*;

/**
 * Utility class for handling and formatting CLI error messages.
 * Translates Router exceptions into VyOS-style configuration path messages.
 */
public class CLIErrorHandler {

	private CLIErrorHandler() {
	}

	public static RuntimeException handleRouteException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		if (e instanceof InvalidNextHopException || e instanceof InvalidSubnetException) {
			String val = message.replace(" is not a valid IPv4 prefix", "");
			return new RuntimeException(String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val));
		}

		if (e instanceof DuplicateConfigurationException) {
			return new RuntimeException("\tConfiguration path: [%s] already exists".formatted(configPath));
		}

		if (e instanceof ConfigurationNotFoundException) {
			if ("Route not found".equals(message)) {
				return new RuntimeException("\tConfiguration path: [%s] does not exist".formatted(configPath));
			}
			return new RuntimeException("\tNothing to delete (the specified node does not exist)");
		}

		// Legacy message-based handling for backwards compatibility
		if ("Route is already disabled".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] is already disabled".formatted(configPath));
		}

		return e;
	}

	public static RuntimeException handleInterfaceException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		switch (e) {
			case DuplicateConfigurationException duplicateConfigurationException -> {
				return new RuntimeException("\tConfiguration path: [%s] already exists".formatted(configPath));
			}
			case InvalidAddressException invalidAddressException -> {
				if (message != null && message.endsWith("is not a valid IPv4 prefix")) {
					String val = message.replace(" is not a valid IPv4 prefix", "");
					return new RuntimeException(String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", val));
				}

				String[] command = configPath.split(" ");
				String ipWithMask = command[command.length - 1];

				if (message != null && message.contains("Cannot assign network address")) {
					return parseNetworkAddress(ipWithMask);
				}
				if (message != null && message.contains("Cannot assign broadcast address")) {
					return parseBroadcastAddress(ipWithMask);
				}
				return new RuntimeException("\tError: Invalid IP address\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed");
			}
			case ConfigurationNotFoundException configurationNotFoundException -> {
				return new RuntimeException("\tNothing to delete (the specified value does not exist)");
			}
			case InterfaceNotFoundException interfaceNotFoundException -> {
				return new RuntimeException("\t%s".formatted(message));
			}
			default -> {
				if (message != null && message.startsWith("Configuration path: [interfaces ethernet")) {
					return new RuntimeException("\t%s".formatted(message));
				}
				return e;
			}
		}
	}

	private static RuntimeException parseBroadcastAddress(String ipWithMask) {
		String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
		String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";

		String[] octets = ip.split("\\.");
		int lastOctet = Integer.parseInt(octets[3]);
		String suggestedIp = String.format("%s.%s.%s.%d", octets[0], octets[1], octets[2], lastOctet - 1);

		return new RuntimeException(
				String.format("""
						\t%s is the broadcast address for this subnet
						\tBroadcast addresses cannot be assigned to interfaces
						\tUse a host address from this subnet (e.g., %s/%s)""", ip, suggestedIp, mask)
		);
	}

	private static RuntimeException parseNetworkAddress(String ipWithMask) {
		String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
		String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";

		return new RuntimeException(
				String.format("""
						\t%s is the network address for this subnet
						\tNetwork addresses cannot be assigned to interfaces
						\tUse a host address from this subnet (e.g., %s.1/%s)""", ip, ip.substring(0, ip.lastIndexOf('.')), mask)
		);
	}

	public static String formatRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s".formatted(destination, nextHop);
	}

	public static String formatRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d".formatted(destination, nextHop, distance);
	}

	public static String formatRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s".formatted(destination, interfaceName);
	}

	public static String formatRouteInterfaceDistance(String destination, String interfaceName, int distance) {
		return "protocols static route %s interface %s distance %d".formatted(destination, interfaceName, distance);
	}

	public static String formatDeleteRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s".formatted(destination, nextHop);
	}

	public static String formatDeleteRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d".formatted(destination, nextHop, distance);
	}

	public static String formatDeleteRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s".formatted(destination, interfaceName);
	}

	public static String formatDeleteRouteInterfaceDistance(String destination, String interfaceName, int distance) {
		return "protocols static route %s interface %s distance %d".formatted(destination, interfaceName, distance);
	}

	public static String formatDisableRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s disable".formatted(destination, nextHop);
	}

	public static String formatDisableRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d disable".formatted(destination, nextHop, distance);
	}

	public static String formatDisableRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s disable".formatted(destination, interfaceName);
	}

	public static String formatDisableRouteInterfaceDistance(String destination, String interfaceName, int distance) {
		return "protocols static route %s interface %s distance %d disable".formatted(destination, interfaceName, distance);
	}

	public static String formatSetInterfaceEthernet(String routerInterfaceName, String subnet) {
		return "interfaces ethernet %s address %s".formatted(routerInterfaceName, subnet);
	}

	public static String formatDisableInterfaceEthernet(String routerInterfaceName, String subnet) {
		if (subnet == null || subnet.isEmpty()) {
			return "interfaces ethernet %s disable".formatted(routerInterfaceName);
		}
		return "interfaces ethernet %s address %s disable".formatted(routerInterfaceName, subnet);
	}

	public static String formatDeleteInterfaceEthernet(String routerInterfaceName, String subnet) {
		return "interfaces ethernet %s address %s".formatted(routerInterfaceName, subnet);
	}
}