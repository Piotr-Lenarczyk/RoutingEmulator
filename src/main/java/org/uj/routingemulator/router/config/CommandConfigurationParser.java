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
 */
public class CommandConfigurationParser implements ConfigurationParser {
	private static final String DISABLE_COMMAND = "disable";
	private List<Token> tokens;
	private int position;

	private static void disableNextHopRoute(Router router, StaticRoutingEntry subnet) {
		router.disableRoute(subnet);
	}

	private static void addNextHopRoute(Router router, StaticRoutingEntry subnet) {
		router.addRoute(subnet);
	}

	@Override
	public void loadConfiguration(Router router, String config) {
		ConfigurationTokenizer tokenizer = new ConfigurationTokenizer();
		this.tokens = tokenizer.tokenize(config);
		this.position = 0;

		RouterMode originalMode = router.getMode();
		router.setMode(RouterMode.CONFIGURATION);

		try {
			router.clearStagedConfiguration();

			while (position < tokens.size()) {
				parseCommand(router);
			}

			// Commit happens ONCE after all commands are successfully parsed
			router.commitChanges();

		} catch (RuntimeException e) {
			router.discardChanges();
			throw e;
		} finally {
			router.setMode(originalMode);
		}
	}

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
					throw new ConfigurationParseException("Invalid interface address: " + e.getMessage(), tokens.get(position - 1));
				}
				break;

			case DISABLE_COMMAND:
				advance();
				try {
					router.disableInterface(interfaceName);
				} catch (RuntimeException e) {
					throw new ConfigurationParseException("Failed to disable interface: " + e.getMessage(), token);
				}
				break;

			default:
				throw new ConfigurationParseException("Unrecognized interface configuration option", token);
		}
	}

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
		advance();

		String destination = getCurrentToken().value();
		advance();

		try {
			Subnet subnet = Subnet.fromString(destination);
			token = getCurrentToken();

			if (token.value().equals("next-hop")) {
				advance();
				IPAddress nextHop = IPAddress.fromString(getCurrentToken().value());
				advance();
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
			throw new ConfigurationParseException("Invalid route configuration: " + e.getMessage(), position > 0 ? tokens.get(position - 1) : tokens.getFirst());
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

		if (position >= tokens.size() || getCurrentToken().value().equals("set")) {
			addNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface));
		} else {
			token = getCurrentToken();
			if (token.value().equals(DISABLE_COMMAND)) {
				advance();
				disableNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface));
			} else if (token.value().equals("distance")) {
				advance();
				int administrativeDistance = Integer.parseInt(getCurrentToken().value());
				advance();
				addNextHopRoute(router, new StaticRoutingEntry(subnet, routerInterface, administrativeDistance));
			} else {
				throw new ConfigurationParseException("Unrecognized route option", token);
			}
		}
	}

	private void parseNextHopRoute(Router router, Subnet subnet, IPAddress nextHop) {
		Token token;
		if (position >= tokens.size() || getCurrentToken().value().equals("set")) {
			addNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop));
		} else {
			token = getCurrentToken();
			if (token.value().equals(DISABLE_COMMAND)) {
				advance();
				disableNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop));
			} else if (token.value().equals("distance")) {
				advance();
				int administrativeDistance = Integer.parseInt(getCurrentToken().value());
				advance();
				addNextHopRoute(router, new StaticRoutingEntry(subnet, nextHop, administrativeDistance));
			} else {
				throw new ConfigurationParseException("Unrecognized route option", token);
			}
		}
	}

	private Token getCurrentToken() {
		if (position >= tokens.size()) {
			throw new ConfigurationParseException(
					"Unexpected end of configuration at line " + (tokens.isEmpty() ? 1 : tokens.getLast().line())
			);
		}
		return tokens.get(position);
	}

	private void advance() {
		position++;
	}
}