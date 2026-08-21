package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.Device;
import org.uj.routingemulator.common.DeviceId;
import org.uj.routingemulator.router.exceptions.InvalidModeException;
import org.uj.routingemulator.router.exceptions.UncommittedChangesException;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"configSession"})
public class Router implements Device {
	private static final String INTERFACE_NOT_EXISTS = "WARN: interface %s does not exist, changes will not be commited";
	private static final Logger logger = Logger.getLogger(Router.class.getName());

	private final DeviceId id = DeviceId.generate();
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
		this.interfaces = new ArrayList<>(interfaces);
		this.mode = RouterMode.OPERATIONAL;
		this.configSession = new RouterConfigurationSession(this);
		logger.fine("Creating new router %s with custom interfaces: %s".formatted(name, interfaces));
	}

	@Override
	public DeviceId getId() {
		return id;
	}

	@Override
	public String getDeviceName() {
		return name;
	}

	public void applyConfiguration(RouterConfiguration configuration) {
		for (RouterInterface newIf : configuration.interfaces()) {
			RouterInterface existing = this.interfaces.stream()
					.filter(i -> i.getInterfaceName().equals(newIf.getInterfaceName()))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				existing.setInterfaceAddress(newIf.getInterfaceAddress());
				existing.setMacAddress(newIf.getMacAddress());
				existing.setDescription(newIf.getDescription());
				existing.setVrf(newIf.getVrf());
				existing.setMtu(newIf.getMtu());
				existing.setStatus(newIf.getStatus());
			} else {
				this.interfaces.add(newIf);
			}
		}

		this.interfaces.removeIf(existing -> configuration.interfaces().stream()
				.noneMatch(newIf -> newIf.getInterfaceName().equals(existing.getInterfaceName())));

		this.routingTable = configuration.routingTable();
	}

	public boolean hasUncommittedChanges() {
		return configSession.hasUncommittedChanges();
	}

	public void setMode(RouterMode mode) {
		if (mode == RouterMode.OPERATIONAL && this.mode == RouterMode.CONFIGURATION && configSession.hasUncommittedChanges()) {
			throw new UncommittedChangesException("Cannot exit: configuration modified.\nUse 'exit discard' to discard the changes and exit.\n[edit]");
		}

		if (mode == RouterMode.CONFIGURATION && this.mode == RouterMode.OPERATIONAL) {
			configSession.discard();
		}

		this.mode = mode;
	}

	public void setModeForced(RouterMode mode) {
		if (mode == RouterMode.OPERATIONAL && this.mode == RouterMode.CONFIGURATION && configSession.hasUncommittedChanges()) {
			configSession.discard();
		}
		this.mode = mode;
	}

	public String showIpRoute() {
		if (mode != RouterMode.OPERATIONAL) {
			logger.warning("Attempted to show IP route while in %s mode".formatted(mode));
			throw new InvalidModeException("Invalid command: show [ip]");
		}
		return IpRouteTableFormatter.format(this);
	}

	public void reset() {
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;
		this.configSession.discard();
	}

	public RouterInterface findFromName(String interfaceName) {
		List<RouterInterface> interfaceList = (mode == RouterMode.CONFIGURATION) ? configSession.getStagedInterfaces() : interfaces;
		return interfaceList.stream()
				.filter(intf -> intf.getInterfaceName().equals(interfaceName))
				.findFirst()
				.orElse(null);
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