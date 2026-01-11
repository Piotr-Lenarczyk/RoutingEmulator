package org.uj.routingemulator.router.cli;

import org.uj.routingemulator.router.exceptions.ConfigurationNotFoundException;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;
import org.uj.routingemulator.router.exceptions.InterfaceNotFoundException;
import org.uj.routingemulator.router.exceptions.InvalidAddressException;

/**
 * Utility class for handling and formatting CLI error messages.
 * Translates Router exceptions into VyOS-style configuration path messages.
 */
public class CLIErrorHandler {

	private CLIErrorHandler() {

	}

	/**
	 * Handles RouterException from Router and formats it into a CLI-friendly message.
	 *
	 * @param e The exception thrown by Router
	 * @param configPath The configuration path that caused the error (e.g., "protocols static route 192.168.1.0/24 next-hop 192.168.1.1")
	 * @return RuntimeException with formatted CLI message
	 */
	public static RuntimeException handleRouteException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		// Handle specific exception types
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

		// For unknown exceptions, rethrow the original
		return e;
	}

	public static RuntimeException handleInterfaceException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		// Handle specific exception types
		if (e instanceof DuplicateConfigurationException) {
			return new RuntimeException("\tConfiguration path: [%s] already exists".formatted(configPath));
		}

		if (e instanceof InvalidAddressException) {
			// Extract the IP address from the config path
			String[] command = configPath.split(" ");
			String ipWithMask = command[command.length - 1];

			// Provide educational error messages
			if (message != null && message.contains("Cannot assign network address")) {
				// Extract just the IP (without mask) for display
				String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
				String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
				return new RuntimeException(
						String.format("\t%s is the network address for this subnet\n" +
										"\tNetwork addresses cannot be assigned to interfaces\n" +
										"\tUse a host address from this subnet (e.g., %s.1/%s)",
								ip, ip.substring(0, ip.lastIndexOf('.')), mask)
				);
			}

			if (message != null && message.contains("Cannot assign broadcast address")) {
				// Extract just the IP (without mask) for display
				String ip = ipWithMask.contains("/") ? ipWithMask.split("/")[0] : ipWithMask;
				String mask = ipWithMask.contains("/") ? ipWithMask.split("/")[1] : "unknown";
				// Calculate suggested IP (one less than broadcast)
				String[] octets = ip.split("\\.");
				int lastOctet = Integer.parseInt(octets[3]);
				String suggestedIp = String.format("%s.%s.%s.%d", octets[0], octets[1], octets[2], lastOctet - 1);
				return new RuntimeException(
						String.format("\t%s is the broadcast address for this subnet\n" +
										"\tBroadcast addresses cannot be assigned to interfaces\n" +
										"\tUse a host address from this subnet (e.g., %s/%s)",
								ip, suggestedIp, mask)
				);
			}

			// Generic invalid address error
			return new RuntimeException("\tError: Invalid IP address\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed");
		}

		if (e instanceof ConfigurationNotFoundException) {
			return new RuntimeException("\tNothing to delete (the specified value does not exist)");
		}

		if (e instanceof InterfaceNotFoundException) {
			return new RuntimeException("\t%s".formatted(message));
		}

		// Legacy message-based handling for backwards compatibility
		if (message != null && message.startsWith("Configuration path: [interfaces ethernet")) {
			return new RuntimeException("\t%s".formatted(message));
		}


		// For unknown exceptions, rethrow the original
		return e;
	}

	/**
	 * Formats a configuration path for static route with next-hop.
	 */
	public static String formatRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s".formatted(destination, nextHop);
	}

	/**
	 * Formats a configuration path for static route with next-hop and distance.
	 */
	public static String formatRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d".formatted(destination, nextHop, distance);
	}

	/**
	 * Formats a configuration path for static route with interface.
	 */
	public static String formatRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s".formatted(destination, interfaceName);
	}

	/**
	 * Formats a configuration path for static route with interface and distance.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param interfaceName Name of the outgoing interface
	 * @param distance Administrative distance
	 * @return Formatted configuration path string
	 */
	public static String formatRouteInterfaceDistance(String destination, String interfaceName, int distance) {
		return "protocols static route %s interface %s distance %d".formatted(destination, interfaceName, distance);
	}

	// Delete command formatters

	/**
	 * Formats a configuration path for deleting static route with next-hop.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param nextHop Next-hop IP address
	 * @return Formatted configuration path string
	 */
	public static String formatDeleteRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s".formatted(destination, nextHop);
	}

	/**
	 * Formats a configuration path for deleting static route with next-hop and distance.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param nextHop Next-hop IP address
	 * @param distance Administrative distance
	 * @return Formatted configuration path string
	 */
	public static String formatDeleteRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d".formatted(destination, nextHop, distance);
	}

	/**
	 * Formats a configuration path for deleting static route with interface.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param interfaceName Name of the outgoing interface
	 * @return Formatted configuration path string
	 */
	public static String formatDeleteRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s".formatted(destination, interfaceName);
	}

	/**
	 * Formats a configuration path for deleting static route with interface and distance.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param interfaceName Name of the outgoing interface
	 * @param distance Administrative distance
	 * @return Formatted configuration path string
	 */
	public static String formatDeleteRouteInterfaceDistance(String destination, String interfaceName, int distance) {
		return "protocols static route %s interface %s distance %d".formatted(destination, interfaceName, distance);
	}

	// Disable command formatters

	/**
	 * Formats a configuration path for disabling static route with next-hop.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param nextHop Next-hop IP address
	 * @return Formatted configuration path string
	 */
	public static String formatDisableRouteNextHop(String destination, String nextHop) {
		return "protocols static route %s next-hop %s disable".formatted(destination, nextHop);
	}

	/**
	 * Formats a configuration path for disabling static route with next-hop and distance.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param nextHop Next-hop IP address
	 * @param distance Administrative distance
	 * @return Formatted configuration path string
	 */
	public static String formatDisableRouteNextHopDistance(String destination, String nextHop, int distance) {
		return "protocols static route %s next-hop %s distance %d disable".formatted(destination, nextHop, distance);
	}

	/**
	 * Formats a configuration path for disabling static route with interface.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param interfaceName Name of the outgoing interface
	 * @return Formatted configuration path string
	 */
	public static String formatDisableRouteInterface(String destination, String interfaceName) {
		return "protocols static route %s interface %s disable".formatted(destination, interfaceName);
	}

	/**
	 * Formats a configuration path for disabling static route with interface and distance.
	 *
	 * @param destination Destination subnet in CIDR notation
	 * @param interfaceName Name of the outgoing interface
	 * @param distance Administrative distance
	 * @return Formatted configuration path string
	 */
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

