package org.uj.routingemulator.router.session;

import org.uj.routingemulator.common.addressing.InterfaceAddress;
import org.uj.routingemulator.router.exceptions.*;
import org.uj.routingemulator.router.model.*;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class RouterConfigurationSession {
	private static final Logger logger = Logger.getLogger(RouterConfigurationSession.class.getName());

	private final Router router;
	private RoutingTable stagedRoutingTable;
	private List<RouterInterface> stagedInterfaces;
	private boolean hasUncommittedChanges;

	public RouterConfigurationSession(Router router) {
		this.router = router;
		discard();
	}

	public boolean hasUncommittedChanges() {
		return hasUncommittedChanges;
	}

	public List<RouterInterface> getStagedInterfaces() {
		return Collections.unmodifiableList(stagedInterfaces);
	}

	public RoutingTable getStagedRoutingTable() {
		return stagedRoutingTable;
	}

	private void requireConfigMode(String cmd) {
		if (router.getMode() != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to " + cmd + " while in " + router.getMode() + " mode");
			throw new InvalidModeException("Invalid command: " + cmd);
		}
	}

	public void commit() {
		requireConfigMode("[commit]");
		if (!hasUncommittedChanges) {
			throw new NoChangesToCommitException("No configuration changes to commit");
		}
		List<RouterInterface> newInterfaces = RoutingTableCopier.deepCopyInterfaces(stagedInterfaces);
		RoutingTable newTable = RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(stagedRoutingTable, newInterfaces);

		RouterConfiguration newConfig = new RouterConfiguration(newInterfaces, newTable);
		router.applyConfiguration(newConfig);
		this.hasUncommittedChanges = false;
		logger.info("%s: Commit complete".formatted(router.getName()));
	}

	public void discard() {
		this.stagedInterfaces = RoutingTableCopier.deepCopyInterfaces(router.getInterfaces());
		this.stagedRoutingTable = RoutingTableCopier.copyRoutingTableWithUpdatedInterfaces(router.getRoutingTable(), this.stagedInterfaces);
		this.hasUncommittedChanges = false;
	}

	public void resetCandidateConfiguration() {
		requireConfigMode("clear configuration");
		for (RouterInterface iface : stagedInterfaces) {
			iface.setInterfaceAddress(null);
			if (iface.getStatus().admin() == AdminState.ADMIN_DOWN) {
				iface.enable();
			}
		}
		this.stagedRoutingTable = new RoutingTable();
		this.hasUncommittedChanges = true;
	}

	public void addRoute(StaticRoutingEntry entry) {
		requireConfigMode("set [protocols]");
		RouteValidator.validateSubnet(entry.getSubnet());
		if (stagedRoutingTable.contains(entry)) {
			logger.warning("Attempted to add duplicate route: %s".formatted(entry));
			throw new RouteAlreadyExistsException("Route already exists");
		}
		if (entry.getNextHop() != null) {
			logger.finer("Validating next-hop %s for the new route".formatted(entry.getNextHop()));
			RouteValidator.validateNextHop(entry.getNextHop(), stagedInterfaces);
		}
		stagedRoutingTable.addRoute(entry);
		hasUncommittedChanges = true;
		logger.info("%s: Creating static route %s".formatted(router.getName(), entry));
	}

	public void removeRoute(StaticRoutingEntry entry) {
		requireConfigMode("delete [protocols]");
		if (!stagedRoutingTable.getRoutingEntries().contains(entry)) {
			logger.warning("Attempted to remove non-existent route: %s".formatted(entry));
			throw new RouteNotFoundException("Nothing to delete");
		}
		stagedRoutingTable.getRoutingEntries().remove(entry);
		hasUncommittedChanges = true;
		logger.info("%s: Route %s removed from staged configuration".formatted(router.getName(), entry));
	}

	public void disableRoute(StaticRoutingEntry entry) {
		requireConfigMode("set [protocols]");
		List<StaticRoutingEntry> entries = stagedRoutingTable.getRoutingEntries();
		int idx = entries.indexOf(entry);
		if (idx == -1) {
			logger.warning("Attempted to disable non-existent route: %s".formatted(entry));
			throw new RouteNotFoundException("Route not found");
		}
		StaticRoutingEntry existing = entries.get(idx);
		if (existing.isDisabled()) {
			logger.warning("Attempted to disable an already disabled route: %s".formatted(entry));
			throw new RouteAlreadyDisabledException("Route already exists");
		}
		existing.disable();
		hasUncommittedChanges = true;
		logger.info("%s: Route %s disabled in staged configuration".formatted(router.getName(), entry));
	}

	public void configureInterface(String routerInterfaceName, InterfaceAddress interfaceAddress) {
		requireConfigMode("set [interfaces]");
		RouteValidator.validateInterfaceAddress(interfaceAddress, routerInterfaceName);

		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));

		if (routerInterface.getInterfaceAddress() != null && routerInterface.getInterfaceAddress().equals(interfaceAddress)) {
			logger.warning("Attempted to assign duplicate address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new InterfaceAddressAlreadyConfiguredException("Configuration already exists");
		}
		routerInterface.setInterfaceAddress(interfaceAddress);
		hasUncommittedChanges = true;
		if (routerInterface.isDisabled()) {
			logger.info("Interface %s is disabled. Staged change applied but packets routed through this interface will be dropped".formatted(routerInterfaceName));
			String msg = String.format("Interface %s is disabled%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", routerInterface.getInterfaceName());
			logger.warning(msg);
		}
		logger.info("%s: Interface %s configured with address %s in staged configuration".formatted(router.getName(), routerInterfaceName, interfaceAddress));
	}

	public void disableInterface(String routerInterfaceName) {
		requireConfigMode("set [interfaces]");
		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));

		logger.info("%s: Disabling interface %s in staged configuration".formatted(router.getName(), routerInterfaceName));
		routerInterface.disable();
		hasUncommittedChanges = true;
	}

	public void deleteInterfaceAddress(String routerInterfaceName) {
		requireConfigMode("delete [interfaces]");
		RouterInterface routerInterface = stagedInterfaces.stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException("WARN: interface " + routerInterfaceName + " does not exist, changes will not be commited"));

		if (routerInterface.getInterfaceAddress() == null) {
			logger.warning("Attempted to delete non-existent address from interface %s".formatted(routerInterfaceName));
			throw new InterfaceAddressNotFoundException("No value to delete");
		}
		routerInterface.setInterfaceAddress(null);
		hasUncommittedChanges = true;
		logger.info("%s: Address deleted from interface %s in staged configuration".formatted(router.getName(), routerInterfaceName));
	}
}