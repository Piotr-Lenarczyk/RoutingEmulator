package org.example.thesisuj.router.cli;

/**
 * Utility class for handling and formatting CLI error messages.
 * Translates generic Router exceptions into VyOS-style configuration path messages.
 */
public class CLIErrorHandler {

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
			return new RuntimeException("\tConfiguration path: [%s] already exists\n\n[edit]".formatted(configPath));
		}

		if ("Route not found".equals(message)) {
			return new RuntimeException("\tConfiguration path: [%s] does not exist\n\n[edit]".formatted(configPath));
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
}

