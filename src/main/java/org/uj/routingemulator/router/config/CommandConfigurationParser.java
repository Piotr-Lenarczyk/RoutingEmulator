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

		if (!token.getValue().equals("set")) {
			throw new ConfigurationParseException("Expected 'set' command at position ", token);
		}

		advance();
		token = getCurrentToken();

		switch (token.getValue()) {
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

		if (!token.getValue().equals("ethernet")) {
			throw new ConfigurationParseException("Expected 'ethernet'", token);
		}

		advance();
		String interfaceName = getCurrentToken().getValue();
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
		switch (token.getValue()) {
			case "address":
				advance();
				String[] addressValue = getCurrentToken().getValue().split("/");
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
			case "disable":
				advance();
				try {
					router.disableInterface(interfaceName);
				} catch (RuntimeException e) {
					// Ignore "already exists" errors for disable
					if (e.getMessage() != null && e.getMessage().contains("already exists")) {
						return;
					}
					throw new ConfigurationParseException("Failed to disable interface: " + e.getMessage(), token);
				}
				break;
			default:
				throw new ConfigurationParseException("Unrecognized interface configuration option", token);
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

		if (!token.getValue().equals("static")) {
			throw new ConfigurationParseException("Expected 'static'", token);
		}

		advance();
		token = getCurrentToken();

		if (!token.getValue().equals("route")) {
			throw new ConfigurationParseException("Expected 'route'", token);
		}

		advance(); // skip 'route'
		String destination = getCurrentToken().getValue();
		advance(); // skip subnet

		try {
			Subnet subnet = Subnet.fromString(destination);
			token = getCurrentToken();

			if (token.getValue().equals("next-hop")) {
				advance();
				IPAddress nextHop = IPAddress.fromString(getCurrentToken().getValue());
				advance();

				// Check for additional options or end of command
				if (position >= tokens.size() || getCurrentToken().getValue().equals("set")) {
					// End of command - add route
					try {
						router.addRoute(new StaticRoutingEntry(subnet, nextHop));
					} catch (RuntimeException e) {
						if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
							return; // Ignore duplicate
						}
						throw e;
					}
				} else {
					token = getCurrentToken();
					if (token.getValue().equals("disable")) {
						advance();
						try {
							router.disableRoute(new StaticRoutingEntry(subnet, nextHop));
						} catch (RuntimeException e) {
							if (e.getMessage() != null && e.getMessage().contains("already exists")) {
								return; // Ignore duplicate
							}
							throw e;
						}
					} else if (token.getValue().equals("distance")) {
						advance();
						int administrativeDistance = Integer.parseInt(getCurrentToken().getValue());
						advance();
						try {
							router.addRoute(new StaticRoutingEntry(subnet, nextHop, administrativeDistance));
						} catch (RuntimeException e) {
							if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
								return; // Ignore duplicate
							}
							throw e;
						}
					} else {
						throw new ConfigurationParseException("Unrecognized route option", token);
					}
				}
			} else if (token.getValue().equals("interface")) {
				advance();
				String interfaceName = getCurrentToken().getValue();
				Token interfaceToken = getCurrentToken();
				advance();

				RouterInterface routerInterface = router.findFromName(interfaceName);
				if (routerInterface == null) {
					throw new ConfigurationParseException(
						String.format("Interface %s does not exist on this router", interfaceName),
						interfaceToken
					);
				}

				// Check for additional options or end of command
				if (position >= tokens.size() || getCurrentToken().getValue().equals("set")) {
					// End of command - add route
					try {
						router.addRoute(new StaticRoutingEntry(subnet, routerInterface));
					} catch (RuntimeException e) {
						if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
							return; // Ignore duplicate
						}
						throw e;
					}
				} else {
					token = getCurrentToken();
					if (token.getValue().equals("disable")) {
						advance();
						try {
							router.disableRoute(new StaticRoutingEntry(subnet, routerInterface));
						} catch (RuntimeException e) {
							if (e.getMessage() != null && e.getMessage().contains("already exists")) {
								return; // Ignore duplicate
							}
							throw e;
						}
					} else if (token.getValue().equals("distance")) {
						advance();
						int administrativeDistance = Integer.parseInt(getCurrentToken().getValue());
						advance();
						try {
							router.addRoute(new StaticRoutingEntry(subnet, routerInterface, administrativeDistance));
						} catch (RuntimeException e) {
							if (e.getMessage() != null && e.getMessage().equals("Route already exists")) {
								return; // Ignore duplicate
							}
							throw e;
						}
					} else {
						throw new ConfigurationParseException("Unrecognized route option", token);
					}
				}
			} else {
				throw new ConfigurationParseException("Expected 'next-hop' or 'interface'", token);
			}
		} catch (NumberFormatException e) {
			throw new ConfigurationParseException("Invalid distance value", tokens.get(position - 1));
		} catch (RuntimeException e) {
			if (e instanceof ConfigurationParseException) {
				throw e;
			}
			throw new ConfigurationParseException("Invalid route configuration: " + e.getMessage(),
					position > 0 ? tokens.get(position - 1) : tokens.getFirst());
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
							(tokens.isEmpty() ? 1 : tokens.getLast().getLine())
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

