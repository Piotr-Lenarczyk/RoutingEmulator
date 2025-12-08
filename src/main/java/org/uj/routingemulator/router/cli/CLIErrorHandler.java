package org.uj.routingemulator.router.cli;

/**
 * Utility class for handling and formatting CLI error messages.
 * Translates generic Router exceptions into VyOS-style configuration path messages.
 */
public class CLIErrorHandler {

	private CLIErrorHandler() {

	}

	/**
	 * Handles RuntimeException from Router and formats it into a CLI-friendly message.
	 *
	 * @param e The exception thrown by Router
	 * @param configPath The configuration path that caused the error (e.g., "protocols static route 192.168.1.0/24 next-hop 192.168.1.1")
	 * @return RuntimeException with formatted CLI message
	 */
	public static RuntimeException handleRouteException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		if ("Route already exists".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] already exists".formatted(configPath));
		}

		if ("Route not found".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] does not exist".formatted(configPath));
		}

		if ("Route is already disabled".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] is already disabled".formatted(configPath));
		}

		if ("Nothing to delete".equals(message)) {
			return new RuntimeException("\tNothing to delete (the specified node does not exist)");
		}

		// For unknown exceptions, rethrow the original
		return e;
	}

	public static RuntimeException handleInterfaceException(RuntimeException e, String configPath) {
		String message = e.getMessage();

		if ("Configuration already exists".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] already exists".formatted(configPath));
		}

		if("Cannot assign network address to interface".equals(message)) {
			String[] command = configPath.split(" ");
			String ip = command[command.length - 1];
			return new RuntimeException("\tError: %s is not a valid host IP host\n\n\n\tInvalid value\n\tValue validation failed\n\tSet failed".formatted(ip));
		}

		if (message != null && message.startsWith("Configuration path: [interfaces ethernet")) {
			return new RuntimeException("\t%s".formatted(message));
		}

		if ("No value to delete".equals(message)) {
			return new RuntimeException("\tNothing to delete (the specified value does not exist)");
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

