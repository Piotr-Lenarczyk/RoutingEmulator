package org.uj.routingemulator.router.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.uj.routingemulator.common.topology.Device;
import org.uj.routingemulator.common.topology.DeviceId;
import org.uj.routingemulator.router.session.ConfigurationSession;
import org.uj.routingemulator.router.session.RouterConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@Getter
@EqualsAndHashCode(exclude = {"configSession"})
public class Router implements Device {
	private static final Logger logger = Logger.getLogger(Router.class.getName());

	private final DeviceId id = DeviceId.generate();
	private String name;
	private RoutingTable routingTable;
	private List<RouterInterface> interfaces;
	private RouterMode mode;
	private final ConfigurationSession configSession;

	public Router(String name) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>();
		this.interfaces.add(new RouterInterface("eth0"));
		this.interfaces.add(new RouterInterface("lo"));
		this.mode = RouterMode.OPERATIONAL;
		this.configSession = new ConfigurationSession(this);
		this.configSession.discard();
		logger.fine("Creating new router %s with default configuration".formatted(name));
	}

	public Router(String name, List<RouterInterface> interfaces) {
		this.name = name;
		this.routingTable = new RoutingTable();
		this.interfaces = new ArrayList<>(interfaces);
		this.mode = RouterMode.OPERATIONAL;
		this.configSession = new ConfigurationSession(this);
		this.configSession.discard();
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

	public void setName(String name) {
		this.name = name;
	}

	public void setMode(RouterMode mode) {
		this.mode = mode;
	}

	public List<RouterInterface> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
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
		return "Router{" +
				"name='" + name + '\'' +
				", routingTable=" + routingTable +
				'}';
	}
}