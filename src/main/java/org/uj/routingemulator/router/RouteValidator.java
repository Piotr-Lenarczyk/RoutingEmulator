package org.uj.routingemulator.router;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.router.exceptions.InvalidAddressException;
import org.uj.routingemulator.router.exceptions.InvalidSubnetException;

import java.util.List;
import java.util.logging.Logger;

/**
 * Domain logic for validating routes and interface addresses.
 */
public class RouteValidator {
	private static final Logger logger = Logger.getLogger(RouteValidator.class.getName());
	private static final String NEXT_HOP_INTERFACE = "Next-hop interface ";
	private static final String TO_INTERFACE = " to interface ";

	private RouteValidator() {
	}

	public static void validateSubnet(Subnet routeSubnet) {
		if (routeSubnet == null || !routeSubnet.isValidNetworkAddress()) {
			logger.warning("Invalid subnet (not a network address) provided for route: " + routeSubnet);
			throw new InvalidSubnetException((routeSubnet == null ? "null" : routeSubnet.toString()) + " is not a valid IPv4 prefix");
		}
	}

	public static void validateNextHop(IPAddress nh, List<RouterInterface> stagedInterfaces) {
		RouterInterface found = stagedInterfaces.stream()
				.filter(i -> i.getInterfaceAddress() != null && i.getInterfaceAddress().ipAddress().equals(nh))
				.findFirst()
				.orElse(null);

		if (found != null) {
			String nhFormatted = found.getInterfaceAddress().toString();
			String msg = String.format("Next-hop interface %s is a local interface on the router%nPackets routed through this route will not be forwarded%nEnsure this action is deliberate", nhFormatted);
			logger.info(NEXT_HOP_INTERFACE + nh + " is a local interface on the router");
			logger.warning(msg);
		} else {
			Integer inferredMask = findNextHopFromStagedInterfaces(nh, stagedInterfaces);
			if (inferredMask != null) {
				String nhFormatted = nh + "/" + inferredMask;
				String msg = String.format("Next-hop interface %s not found on the router%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", nhFormatted);
				logger.info(NEXT_HOP_INTERFACE + nh + " not found on the router");
				logger.warning(msg);
			} else {
				String msg = String.format("Next-hop interface %s is not a directly connected neighbor interface%nThis may be fine if configuration is not yet complete%nPackets routed through this route will be dropped until the next-hop is reachable%nEnsure this action is deliberate", nh);
				logger.info(NEXT_HOP_INTERFACE + nh + " not found on the router");
				logger.warning(msg);
			}
		}
	}

	private static Integer findNextHopFromStagedInterfaces(IPAddress nh, List<RouterInterface> stagedInterfaces) {
		for (RouterInterface ri : stagedInterfaces) {
			if (ri.getSubnet() != null && ri.getSubnet().subnetMask() != null) {
				Subnet s = ri.getSubnet();
				long ipAsLong = ((long) nh.getOctet1() << 24) | ((long) nh.getOctet2() << 16) | ((long) nh.getOctet3() << 8) | nh.getOctet4();
				int prefix = s.subnetMask().shortMask();
				long networkMask = (prefix == 0) ? 0 : (0xFFFFFFFFL << (32 - prefix));
				long net = ((long) s.networkAddress().getOctet1() << 24) | ((long) s.networkAddress().getOctet2() << 16) | ((long) s.networkAddress().getOctet3() << 8) | s.networkAddress().getOctet4();

				if ((ipAsLong & networkMask) == (net & networkMask)) {
					return prefix;
				}
			}
		}
		return null;
	}

	public static void validateInterfaceAddress(InterfaceAddress interfaceAddress, String routerInterfaceName) {
		if (interfaceAddress.isNetworkAddress()) {
			logger.warning("Attempted to assign network address " + interfaceAddress + TO_INTERFACE + routerInterfaceName);
			throw new InvalidAddressException(String.format("Cannot assign network address %s to the interface. Use a host address instead", interfaceAddress));
		}
		if (interfaceAddress.isBroadcastAddress()) {
			logger.warning("Attempted to assign broadcast address " + interfaceAddress + TO_INTERFACE + routerInterfaceName);
			throw new InvalidAddressException(String.format("Cannot assign broadcast address %s to the interface. Use a host address instead", interfaceAddress));
		}
		if (!interfaceAddress.isValidHostAddress()) {
			logger.warning("Attempted to assign invalid host address " + interfaceAddress + TO_INTERFACE + routerInterfaceName);
			throw new InvalidAddressException(interfaceAddress + " is not a valid host IP address");
		}
	}
}