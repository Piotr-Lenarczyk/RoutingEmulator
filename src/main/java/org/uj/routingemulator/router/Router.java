package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.exceptions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"configSession"})
public class Router {
	private static final String INTERFACE_NOT_EXISTS = "WARN: interface %s does not exist, changes will not be commited";
	private static final Logger logger = Logger.getLogger(Router.class.getName());

	private String name;
	private RoutingTable routingTable;
	private List<RouterInterface> interfaces;
	private RouterMode mode;

	private final RouterConfigurationSession configSession;

	public Router(String name) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;

		this.configSession = new RouterConfigurationSession(this);
		logger.fine("Creating new router %s with default configuration".formatted(name));
	}

	public Router(String name, List<RouterInterface> interfaces) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = interfaces;
		this.mode = RouterMode.OPERATIONAL;

		this.configSession = new RouterConfigurationSession(this);
		logger.fine("Creating new router %s with custom interfaces: %s".formatted(name, interfaces));
	}

	public boolean hasUncommittedChanges() {
		return configSession.hasUncommittedChanges();
	}

	public RoutingTable getStagedRoutingTable() {
		return configSession.getStagedRoutingTable();
	}

	public List<RouterInterface> getStagedInterfaces() {
		return configSession.getStagedInterfaces();
	}

	public void addRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to add route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [protocols]");
		}

		RouteValidator.validateSubnet(entry.getSubnet());

		if (configSession.getStagedRoutingTable().contains(entry)) {
			logger.warning("Attempted to add duplicate route: %s".formatted(entry));
			throw new DuplicateConfigurationException("Route already exists");
		}

		if (entry.getNextHop() != null) {
			logger.finer("Validating next-hop %s for the new route".formatted(entry.getNextHop()));
			RouteValidator.validateNextHop(entry.getNextHop(), configSession.getStagedInterfaces());
		}

		configSession.getStagedRoutingTable().addRoute(entry);
		configSession.setHasUncommittedChanges(true);
		logger.info("%s: Creating static route %s".formatted(this.name, entry));
	}

	public void removeRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to remove route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: delete [protocols]");
		}

		if (!configSession.getStagedRoutingTable().getRoutingEntries().contains(entry)) {
			logger.warning("Attempted to remove non-existent route: %s".formatted(entry));
			throw new ConfigurationNotFoundException("Nothing to delete");
		}

		configSession.getStagedRoutingTable().getRoutingEntries().remove(entry);
		configSession.setHasUncommittedChanges(true);
		logger.info("%s: Route %s removed from staged configuration".formatted(this.name, entry));
	}

	public void disableRoute(StaticRoutingEntry entry) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to disable route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [protocols]");
		}

		List<StaticRoutingEntry> entries = configSession.getStagedRoutingTable().getRoutingEntries();
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
		configSession.setHasUncommittedChanges(true);
		logger.info("%s: Route %s disabled in staged configuration".formatted(this.name, entry));
	}

	public void configureInterface(String routerInterfaceName, InterfaceAddress interfaceAddress) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to configure interface while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		RouteValidator.validateInterfaceAddress(interfaceAddress, routerInterfaceName);

		RouterInterface routerInterface = configSession.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException(INTERFACE_NOT_EXISTS.formatted(routerInterfaceName)));

		if (routerInterface.getInterfaceAddress() != null && routerInterface.getInterfaceAddress().equals(interfaceAddress)) {
			logger.warning("Attempted to assign duplicate address %s to interface %s".formatted(interfaceAddress, routerInterfaceName));
			throw new DuplicateConfigurationException("Configuration already exists");
		}

		routerInterface.setInterfaceAddress(interfaceAddress);
		configSession.setHasUncommittedChanges(true);

		if (routerInterface.isDisabled()) {
			logger.info("Interface %s is disabled. Staged change applied but packets routed through this interface will be dropped".formatted(routerInterfaceName));
			String msg = String.format("Interface %s is disabled%nPackets routed through this interface will be dropped%nEnsure this action is deliberate", routerInterface.getInterfaceName());
			logger.warning(msg);
		}
		logger.info("%s: Interface %s configured with address %s in staged configuration".formatted(this.name, routerInterfaceName, interfaceAddress));
	}

	public void disableInterface(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to disable interface while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: set [interfaces]");
		}

		RouterInterface routerInterface = configSession.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException(INTERFACE_NOT_EXISTS.formatted(routerInterfaceName)));

		logger.info("%s: Disabling interface %s in staged configuration".formatted(this.getName(), routerInterfaceName));
		routerInterface.disable();
		configSession.setHasUncommittedChanges(true);
	}

	public void deleteInterfaceAddress(String routerInterfaceName) {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to delete interface address while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: delete [interfaces]");
		}

		RouterInterface routerInterface = configSession.getStagedInterfaces().stream()
				.filter(intf -> intf.getInterfaceName().equals(routerInterfaceName))
				.findFirst()
				.orElseThrow(() -> new InterfaceNotFoundException(INTERFACE_NOT_EXISTS.formatted(routerInterfaceName)));

		if (routerInterface.getInterfaceAddress() == null) {
			logger.warning("Attempted to delete non-existent address from interface %s".formatted(routerInterfaceName));
			throw new ConfigurationNotFoundException("No value to delete");
		}

		routerInterface.setInterfaceAddress(null);
		configSession.setHasUncommittedChanges(true);
		logger.info("%s: Address deleted from interface %s in staged configuration".formatted(this.name, routerInterfaceName));
	}

	public void commitChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			logger.warning("Attempted to commit changes while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: [commit]");
		}
		configSession.commitChanges(this);
		logger.info("%s: Commit complete".formatted(this.name));
	}

	public void discardChanges() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Invalid command: [discard]");
		}
		configSession.discardChanges(this);
	}

	public void clearStagedConfiguration() {
		if (mode != RouterMode.CONFIGURATION) {
			throw new InvalidModeException("Cannot clear configuration in operational mode");
		}
		configSession.clearStagedConfiguration();
	}

	public void setMode(RouterMode mode) {
		if (this.mode == RouterMode.CONFIGURATION && hasUncommittedChanges()) {
			throw new UncommittedChangesException("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]");
		}
		if (mode == RouterMode.CONFIGURATION && this.mode == RouterMode.OPERATIONAL) {
			configSession.discardChanges(this);
		}
		this.mode = mode;
	}

	public void setModeForced(RouterMode mode) {
		if (this.mode == RouterMode.CONFIGURATION && hasUncommittedChanges()) {
			discardChanges();
		}
		this.mode = mode;
	}

	/**
	 * Displays the IP routing table in VyOS format.
	 * Shows both static routes and connected routes (directly connected networks).
	 * Must be executed in OPERATIONAL mode.
	 *
	 * @return Formatted routing table output
	 * @throws InvalidModeException if not in OPERATIONAL mode
	 */
	public String showIpRoute() {
		if (mode != RouterMode.OPERATIONAL) {
			logger.warning("Attempted to show IP route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: show [ip]");
		}

		// Delegate the heavy lifting to the presentation formatter
		return IpRouteTableFormatter.format(this);
	}

	public void reset() {
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;
		this.configSession.discardChanges(this);
	}

	public RouterInterface findFromName(String interfaceName) {
		List<RouterInterface> interfaceList = (mode == RouterMode.CONFIGURATION) ? configSession.getStagedInterfaces() : interfaces;
		return interfaceList.stream()
				.filter(intf -> intf.getInterfaceName().equals(interfaceName))
				.findFirst()
				.orElse(null);
	}

	public PingStatistics ping(String dst, NetworkTopology topology) {
		logger.info("Initializing new PingService for host %s".formatted(this.name));
		PingService svc = new PingService();
		logger.info("%s: Pinging %s with 4 probes...".formatted(this.name, dst));
		return svc.ping(this, IPAddress.fromString(dst), 4, 64, topology);
	}

	@Override
	public String toString() {
		if (mode != RouterMode.OPERATIONAL) {
			throw new InvalidModeException("Configuration path: [ip] is not valid\nShow failed");
		}
		return "Router{" +
				"name='" + name + '\'' +
				", routingTable=" + routingTable +
				'}';
	}
}