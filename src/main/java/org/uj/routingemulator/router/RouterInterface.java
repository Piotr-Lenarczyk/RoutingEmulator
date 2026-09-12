package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.exceptions.ConfigurationNotFoundException;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;

import java.util.logging.Logger;

/**
 * Represents a network interface on a router device.
 * <p>
 * Router interfaces are Layer 3 interfaces capable of forwarding IP packets.
 * Each interface can be configured with:
 * <ul>
 *   <li>IP address and subnet mask ({@link InterfaceAddress})</li>
 *   <li>MAC address</li>
 *   <li>Administrative state (enabled/disabled)</li>
 *   <li>MTU (Maximum Transmission Unit)</li>
 *   <li>VRF (Virtual Routing and Forwarding) assignment</li>
 *   <li>Description for documentation</li>
 * </ul>
 * <p>
 * Interface names follow standard conventions:
 * <ul>
 *   <li>eth0, eth1, ... - Ethernet interfaces (MTU 1500)</li>
 *   <li>eth0.1000 - Ethernet VLAN sub-interfaces (MTU 1500)</li>
 *   <li>dum0, dum1, ... - Dummy interfaces (MTU 1500)</li>
 *   <li>lo - Loopback interface (MTU 65536)</li>
 * </ul>
 * <p>
 * The interface status combines administrative state (controlled by configuration)
 * and link state (physical layer status). Both must be UP for the interface
 * to be operational.
 */
@Setter
@Getter
@EqualsAndHashCode
@ToString
public class RouterInterface implements NetworkInterface {

	private static final Logger logger = Logger.getLogger(RouterInterface.class.getName());

	private String interfaceName;
	private InterfaceType type;
	private InterfaceAddress interfaceAddress;
	private MacAddress macAddress;
	private String description;
	private String vrf = "default";
	private int mtu;
	private InterfaceStatus status;

	/**
	 * Creates a router interface with the specified name and determines its type automatically.
	 *
	 * @param interfaceName the name of the interface (e.g., "eth0", "eth0.10", "lo", "dum0")
	 */
	public RouterInterface(String interfaceName) {
		this.interfaceName = interfaceName;
		this.type = InterfaceType.fromName(interfaceName);
		this.interfaceAddress = null;
		this.macAddress = new MacAddress();
		this.description = null;

		if (this.type == InterfaceType.ETHERNET || this.type == InterfaceType.DUMMY || this.type == InterfaceType.VIF) {
			this.mtu = 1500;
		} else if (this.type == InterfaceType.LOOPBACK) {
			this.mtu = 65536;
		}

		// Interface starts with admin UP but link DOWN (no physical connection yet)
		this.status = InterfaceStatus.fromChars('u', 'D');
	}

	public RouterInterface(String interfaceName, LinkState linkState) {
		this.interfaceName = interfaceName;
		this.type = InterfaceType.fromName(interfaceName);
		this.interfaceAddress = null;
		this.macAddress = new MacAddress();
		this.description = null;

		if (this.type == InterfaceType.ETHERNET || this.type == InterfaceType.DUMMY || this.type == InterfaceType.VIF) {
			this.mtu = 1500;
		} else if (this.type == InterfaceType.LOOPBACK) {
			this.mtu = 65536;
		}

		// Interface starts with admin UP
		this.status = InterfaceStatus.fromChars('u', linkState.getCode());
	}

	public RouterInterface(String interfaceName, InterfaceAddress interfaceAddress, MacAddress macAddress, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.type = InterfaceType.fromName(interfaceName);
		this.interfaceAddress = interfaceAddress;
		this.macAddress = macAddress;
		this.mtu = mtu;
		this.status = status;
	}

	public RouterInterface(String interfaceName, InterfaceAddress interfaceAddress, MacAddress macAddress, String vrf, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.type = InterfaceType.fromName(interfaceName);
		this.interfaceAddress = interfaceAddress;
		this.macAddress = macAddress;
		this.vrf = vrf;
		this.mtu = mtu;
		this.status = status;
	}

	public RouterInterface(RouterInterface other) {
		this.interfaceName = other.interfaceName;
		this.type = other.type;
		this.interfaceAddress = other.interfaceAddress;
		this.macAddress = other.macAddress;
		this.description = other.description;
		this.vrf = other.vrf;
		this.mtu = other.mtu;
		this.status = other.status;
	}

	public Subnet getSubnet() {
		return interfaceAddress != null ? interfaceAddress.getSubnet() : null;
	}

	public void setSubnet(Subnet subnet) {
		if (subnet != null) {
			this.interfaceAddress = new InterfaceAddress(subnet.networkAddress(), subnet.subnetMask());
		} else {
			this.interfaceAddress = null;
		}
	}

	public void disable() {
		if (this.status.getAdmin() == AdminState.ADMIN_DOWN) {
			logger.warning("Interface %s is already administratively disabled.".formatted(this.interfaceName));
			throw new DuplicateConfigurationException("Configuration path: [interfaces %s %s disable] already exists".formatted(this.type.name().toLowerCase(), this.interfaceName));
		}
		logger.finer("Disabling interface %s. Previous status: %s".formatted(this.interfaceName, this.status));
		this.status = new InterfaceStatus(AdminState.ADMIN_DOWN, this.status.getLink());
		logger.finest("Setting interface %s admin state to %s".formatted(this.interfaceName, this.status.getAdmin()));
	}

	public void enable() {
		if (this.status.getAdmin() == AdminState.UP) {
			logger.warning("Interface %s is already enabled.".formatted(this.interfaceName));
			throw new ConfigurationNotFoundException("Nothing to delete (the specified node does not exist)");
		}
		logger.finer("Enabling interface %s. Previous status: %s".formatted(this.interfaceName, this.status));
		this.status = new InterfaceStatus(AdminState.UP, this.status.getLink());
		logger.finest("Setting interface %s admin state to %s".formatted(this.interfaceName, this.status.getAdmin()));
	}

	public boolean isDisabled() {
		return this.status.getAdmin() == AdminState.ADMIN_DOWN;
	}

	public void updateLinkState(NetworkTopology topology) {
		if (this.type == InterfaceType.DUMMY) {
			logger.finer("Interface %s is a dummy interface. Link state remains UP".formatted(this.interfaceName));
			this.status = new InterfaceStatus(this.status.getAdmin(), LinkState.UP);
			return;
		}

		if (this.type == InterfaceType.VIF) {
			processVifLinkState(topology);
			return;
		}

		LinkState newLinkState = topology.hasActiveConnection(this) ? LinkState.UP : LinkState.DOWN;
		logger.finer("Updating link state for interface %s to %s".formatted(this.interfaceName, newLinkState));
		this.status = new InterfaceStatus(this.status.getAdmin(), newLinkState);

		Router owner = null;
		for (Router router : topology.getRouters()) {
			if (router.getInterfaces().contains(this)) {
				owner = router;
				break;
			}
		}

		if (owner != null) {
			String childPrefix = this.interfaceName + ".";
			for (RouterInterface childVif : owner.getInterfaces()) {
				if (childVif.getType() == InterfaceType.VIF && childVif.getInterfaceName().startsWith(childPrefix)) {
					logger.finer("Cascading link state update from parent %s to child VIF %s"
							.formatted(this.interfaceName, childVif.getInterfaceName()));
					childVif.updateLinkState(topology);
				}
			}
		}
	}

	private void processVifLinkState(NetworkTopology topology) {
		String parentName = this.interfaceName.split("\\.")[0];
		Router owner = null;

		for (Router router : topology.getRouters()) {
			if (router.getInterfaces().contains(this)) {
				owner = router;
				break;
			}
		}

		if (owner != null) {
			RouterInterface parent = owner.findFromName(parentName);
			if (parent != null) {
				if (parent.getStatus().getAdmin() == AdminState.ADMIN_DOWN || parent.getStatus().getLink() == LinkState.DOWN) {
					this.status = new InterfaceStatus(this.status.getAdmin(), LinkState.DOWN);
				} else {
					this.status = new InterfaceStatus(this.status.getAdmin(), LinkState.UP);
				}
				return;
			}
		}

		this.status = new InterfaceStatus(this.status.getAdmin(), LinkState.DOWN);
	}
}