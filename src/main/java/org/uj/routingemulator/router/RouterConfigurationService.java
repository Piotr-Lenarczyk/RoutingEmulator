package org.uj.routingemulator.router;

import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.router.exceptions.ConfigurationNotFoundException;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;
import org.uj.routingemulator.router.exceptions.InterfaceNotFoundException;
import org.uj.routingemulator.router.exceptions.InvalidModeException;

import java.util.List;
import java.util.logging.Logger;

public class RouterConfigurationService {
	private static final Logger logger = Logger.getLogger(RouterConfigurationService.class.getName());

	public void addRoute(Router router, StaticRoutingEntry entry) {
		requireConfigMode(router, "set [protocols]");
		RouteValidator.validateSubnet(entry.getSubnet());
		ConfigurationSession session = router.getConfigSession();
		if (session.getStagedRoutingTable().contains(entry)) {
			logger.warning("Attempted to add duplicate route: %s".formatted(entry));
			throw new DuplicateConfigurationException("Route already exists");
		}
		if (entry.getNextHop() != null) {
			logger.finer("Validating next-hop %s for the new route".formatted(entry.getNextHop()));
			RouteValidator.validateNextHop(entry.getNextHop(), session.getStagedInterfaces());
		}
		session.getStagedRoutingTable().addRoute(entry);
		session.setHasUncommittedChanges(true);
		logger.info("%s: Creating static route %s".formatted(router.getName(), entry));
	}

	public void removeRoute(Router router, StaticRoutingEntry entry) {
		requireConfigMode(router, "delete [protocols]");
		ConfigurationSession session = router.getConfigSession();
		if (!session.getStagedRoutingTable().getRoutingEntries().contains(entry)) {
			logger.warning("Attempted to remove non-existent route: %s".formatted(entry));
			throw new ConfigurationNotFoundException("Nothing to delete");
		}
		session.getStagedRoutingTable().getRoutingEntries().remove(entry);
		session.setHasUncommittedChanges(true);
		logger.info("%s: Route %s removed from staged configuration".formatted(router.getName(), entry));
	}

	public void disableRoute(Router router, StaticRoutingEntry entry) {
		requireConfigMode(router, "set [protocols]");
		ConfigurationSession session = router.getConfigSession();
		List<StaticRoutingEntry> entries = session.getStagedRoutingTable().getRoutingEntries();
		int idx = entries.indexOf(entry);
		if (idx == -1) {
			logger.warning("Attempted to disable non-existent route: %s".formatted(entry));
			throw new ConfigurationNotFoundException("Route not found");
		}
		StaticRoutingEntry existing = entries.get(idx);
		if (existing.isDisabled()) {
			logger.warning("Attempted to disable an already disabled route: %s".formatted(entry));
			throw new DuplicateConfigurationException("Route already exists");
		}
		existing.disable();
		session.setHasUncommittedChanges(true);
		logger.info("%s: Route %s disabled in staged configuration".formatted(router.getName(), entry));
	}

	public void configureInterface(Router router, String routerInterfaceName, InterfaceAddress interfaceAddress) {
		requireConfigMode(router, "set [interfaces]");
		RouteValidator.validateInterfaceAddress(interfaceAddress, routerInterfaceName);
		ConfigurationSession session = router.getConfigSession();
		RouterInterface routerInterface = session.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));
		if (routerInterface.getInterfaceAddress() != null && routerInterface.getInterfaceAddress().equals(interfaceAddress)) {
			logger.warning("Attempted to assign duplicate address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new DuplicateConfigurationException("Configuration already exists");
		}
		routerInterface.setInterfaceAddress(interfaceAddress);
		session.setHasUncommittedChanges(true);
		if (routerInterface.isDisabled()) {
			logger.info("Interface %s is disabled. Staged change applied but packets routed through this interface will be dropped".formatted(routerInterfaceName));
			String msg = String.format("Interface %s is disabled%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", routerInterface.getInterfaceName());
			logger.warning(msg);
		}
		logger.info("%s: Interface %s configured with address %s in staged configuration".formatted(router.getName(), routerInterfaceName, interfaceAddress));
	}

	public void disableInterface(Router router, String routerInterfaceName) {
		requireConfigMode(router, "set [interfaces]");
		ConfigurationSession session = router.getConfigSession();
		RouterInterface routerInterface = session.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));
		logger.info("%s: Disabling interface %s in staged configuration".formatted(router.getName(), routerInterfaceName));
		routerInterface.disable();
		session.setHasUncommittedChanges(true);
	}

	public void deleteInterfaceAddress(Router router, String routerInterfaceName) {
		requireConfigMode(router, "delete [interfaces]");
		ConfigurationSession session = router.getConfigSession();
		RouterInterface routerInterface = session.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));
		if (routerInterface.getInterfaceAddress() == null) {
			logger.warning("Attempted to delete non-existent address from interface %s".formatted(routerInterfaceName));
			throw new ConfigurationNotFoundException("No value to delete");
		}
		routerInterface.setInterfaceAddress(null);
		session.setHasUncommittedChanges(true);
		logger.info("%s: Address deleted from interface %s in staged configuration".formatted(router.getName(), routerInterfaceName));
	}

	private void requireConfigMode(Router router, String cmd) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to " + cmd + " while in " + router.getMode() + " mode");
			throw new InvalidModeException("Invalid command: " + cmd);
		}
	}
}