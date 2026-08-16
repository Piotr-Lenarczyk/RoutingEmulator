package org.uj.routingemulator.router.config;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.router.RouterInterface;
import org.uj.routingemulator.router.RouterMode;
import org.uj.routingemulator.router.StaticRoutingEntry;

import java.util.List;

/**
 * Parses VyOS command-based configuration format ('set' commands).
 * <p>
 * The parser supports:
 * <ul>
 *   <li>Interface configuration (addresses, disable)</li>
 *   <li>Static routing configuration (next-hop, interface, distance, disable)</li>
 *   <li>Comments and empty lines</li>
 * </ul>
 * <p>
 * If parsing fails, all changes are automatically rolled back and the router
 * is restored to its original state.
 */
public class CommandConfigurationParser implements ConfigurationParser {
	private List<Token> tokens;
	private int position;
	private static final String ROUTE_ALREADY_EXISTS = "Route already exists";
	private static final String ALREADY_EXISTS = "already exists";
	private static final String DISABLE = "disable";

	/**
	 * Loads and applies configuration from a string to the specified router.
	 * <p>
	 * The method:
	 * <ul>
	 *   <li>Tokenizes the configuration</li>
	 *   <li>Puts router in configuration mode</li>
	 *   <li>Clears existing staged configuration</li>
	 *   <li>Parses and applies each command</li>
	 *   <li>Commits changes on success</li>
	 *   <li>Rolls back on error</li>
	 *   <li>Restores original router mode</li>
	 * </ul>
	 *
	 * @param router the router to configure
	 * @param config the configuration text in VyOS format
	 * @throws ConfigurationParseException if the configuration is invalid
	 */
	@Override
	public void loadConfiguration(Router router, String config) {
		ConfigurationTokenizer tokenizer = new ConfigurationTokenizer();
		this.tokens = tokenizer.tokenize(config);
		this.position = 0;

		RouterMode originalMode = router.getMode();
		router.setMode(RouterMode.CONFIGURATION);

		try {
			// Clear existing staged configuration before loading new one
			router.clearStagedConfiguration();

			while (position < tokens.size()) {
				parseCommand(router);
			}
			router.commitChanges();
		} catch (RuntimeException e) {
			router.discardChanges();
			throw e;
		} finally {
			router.setMode(originalMode);
		}
	}

	/**
	 * Parses a single 'set' command and applies it to the router.
	 *
	 * @param router the router to configure
	 * @throws ConfigurationParseException if the command is invalid
	 */
	private void parseCommand(Router router) {
		Token token = getCurrentToken();

		if (!token.value().equals("set")) {
			throw new ConfigurationParseException("Expected 'set' command at position ", token);
		}

		advance();
		token = getCurrentToken();

		switch (token.value()) {
			case "interfaces":
				parseInterfaces(router);
				break;
			case "protocols":
				parseProtocols(router);
				break;
			default:
				throw new ConfigurationParseException("Unrecognized configuration path", token);
		}
	}

	/**
	 * Parses interface configuration commands.
	 * Handles 'set interfaces ethernet' commands.
	 *
	 * @param router the router to configure
	 * @throws ConfigurationParseException if the interface command is invalid
	 */
	private void parseInterfaces(Router router) {
		advance();
		Token token = getCurrentToken();

		if (!token.value().equals("ethernet")) {
			throw new ConfigurationParseException("Expected 'ethernet'", token);
		}

		advance();
		String interfaceName = getCurrentToken().value();
		Token interfaceToken = getCurrentToken();
		advance();

		// Check if interface exists before attempting configuration
		RouterInterface routerInterface = router.findFromName(interfaceName);
		if (routerInterface == null) {
			throw new ConfigurationParseException(
				String.format("Interface %s does not exist on this router", interfaceName),
				interfaceToken
			);
		}

		token = getCurrentToken();
		switch (token.value()) {
			case "address":
				advance();
				String[] addressValue = getCurrentToken().value().split("/");
				advance();
				try {
					IPAddress ipAddress = IPAddress.fromString(addressValue[0]);
					SubnetMask mask = SubnetMask.fromString(addressValue[1]);
					InterfaceAddress interfaceAddress = new InterfaceAddress(ipAddress, mask);
					router.configureInterface(interfaceName, interfaceAddress);
				} catch (RuntimeException e) {
					// Ignore "Configuration already exists" errors
					if (e.getMessage() != null && e.getMessage().equals("Configuration already exists")) {
						// Skip this command silently
						return;
					}
					throw new ConfigurationParseException("Invalid interface address: " + e.getMessage(), tokens.get(position - 1));
				}
				break;
			case DISABLE:
				advance();
				try {
					router.disableInterface(interfaceName);
				} catch (RuntimeException e) {
					// Ignore "already exists" errors for disable
					if (e.getMessage() != null && e.getMessage().contains(ALREADY_EXISTS)) {
						return;
					}
					throw new ConfigurationParseException("Failed to disable interface: " + e.getMessage(), token);
				}
				break;
			default:
				throw new ConfigurationParseException("Unrecognized interface configuration option", token);
		}
	}

	private static void disableNextHopRoute(Router router, StaticRoutingEntry subnet) {
		try {
			router.disableRoute(subnet);
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().contains(ALREADY_EXISTS)) {
				return;
			}
			throw e;
		}
	}

	private static void addNextHopRoute(Router router, StaticRoutingEntry subnet) {
		try {
			router.addRoute(subnet);
		} catch (RuntimeException e) {
			if (e.getMessage() != null && e.getMessage().equals(ROUTE_ALREADY_EXISTS)) {
				return;
			}
			throw e;
		}
	}

	/**
	 * Parses static routing protocol configuration commands.
	 * Handles 'set protocols static route' commands with various options.
	 *
	 * @param router the router to configure
	 * @throws ConfigurationParseException if the route command is invalid
	 */
	private void parseProtocols(Router router) {
		advance();
		Token token = getCurrentToken();

		if (!token.value().equals("static")) {
			throw new ConfigurationParseException("Expected 'static'", token);
		}

		advance();
		token = getCurrentToken();

		if (!token.value().equals("route")) {
			throw new ConfigurationParseException("Expected 'route'", token);
		}

		advance(); // skip 'route'
		String destination = getCurrentToken().value();
		advance(); // skip subnet

		try {
			Subnet subnet = Subnet.fromString(destination);
			token = getCurrentToken();

			if (token.value().equals("next-hop")) {
				advance();
				IPAddress nextHop = IPAddress.fromString(getCurrentToken().value());
				advance();

				// Check for additional options or end of command
				parseNextHopRoute(router, subnet, nextHop);
			} else if (token.value().equals("interface")) {
				advance();
				String interfaceName = getCurrentToken().value();
				Token interfaceToken = getCurrentToken();
				advance();

				parseInterfaceRoute(router, interfaceName, interfaceToken, subnet);
			} else {
				throw new ConfigurationParseException("Expected 'next-hop' or 'interface'", token);
			}
		} catch (NumberFormatException e) {
			throw new ConfigurationParseException("Invalid distance value", tokens.get(position - 1));
		} catch (ConfigurationParseException e) {
			throw new ConfigurationParseException("Invalid route configuration: " + e.getMessage(),
					position > 0 ? tokens.get(position - 1) : tokens.getFirst());
		}
	}

	private void parseInterfaceRoute(Router router, String interfaceName, Token interfaceToken, Subnet subnet) {
		Token token;
		RouterInterface routerInterface = router.findFromName(interfaceName);
		if (routerInterface == null) {
			throw new ConfigurationParseException(
					String.format("Interface %s does not exist on this router", interfaceName),
					interfaceToken
			);
		}

		// Check for additional options or end of command
		if (position >= tokens.size() || getCurrentToken().value().equals("set")) {
			// End of command - add route
			addNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface));// Ignore duplicate
		} else {
			token = getCurrentToken();
			if (token.value().equals(DISABLE)) {
				advance();
				disableNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface));// Ignore duplicate
			} else if (token.value().equals("distance")) {
				advance();
				int administrativeDistance = Integer.parseInt(getCurrentToken().value());
				advance();
				addNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface, administrativeDistance));// Ignore duplicate
			} else {
				throw new ConfigurationParseException("Unrecognized route option", token);
			}
		}
	}

	private void parseNextHopRoute(Router router, Subnet subnet, IPAddress nextHop) {
		Token token;
		if (position >= tokens.size() || getCurrentToken().value().equals("set")) {
			// End of command - add route
			addNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop));// Ignore duplicate
		} else {
			token = getCurrentToken();
			if (token.value().equals(DISABLE)) {
				advance();
				disableNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop));// Ignore duplicate
			} else if (token.value().equals("distance")) {
				advance();
				int administrativeDistance = Integer.parseInt(getCurrentToken().value());
				advance();
				addNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop, administrativeDistance));// Ignore duplicate
			} else {
				throw new ConfigurationParseException("Unrecognized route option", token);
			}
		}
	}

	/**
	 * Gets the token at the current parsing position.
	 *
	 * @return the current token
	 * @throws ConfigurationParseException if position is beyond the end of tokens
	 */
	private Token getCurrentToken() {
		if (position >= tokens.size()) {
			throw new ConfigurationParseException(
					"Unexpected end of configuration at line " +
							(tokens.isEmpty() ? 1 : tokens.getLast().line())
			);
		}
		return tokens.get(position);
	}

	/**
	 * Advances the parser to the next token.
	 */
	private void advance() {
		position++;
	}
}

