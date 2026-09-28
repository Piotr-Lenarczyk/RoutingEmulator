package org.uj.routingemulator.router;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.uj.routingemulator.common.*;
import org.uj.routingemulator.router.exceptions.ConfigurationNotFoundException;
import org.uj.routingemulator.router.exceptions.DuplicateConfigurationException;

import java.util.logging.Logger;

/**
 * Represents a network interface on a router device.
 * By not defining a custom equals/hashCode, this class correctly relies on object identity (memory reference).
 * This ensures that identically named interfaces on different routers (e.g., eth0 on R1 and eth0 on R2)
 * are treated as completely distinct physical entities by the ForwardingEngine and Topology.
 */
@Setter
@Getter
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

	// --- RELATIONAL HELPER METHODS ---

	/**
	 * Checks if this interface is a VLAN child of the provided physical interface.
	 */
	public boolean isChildOf(RouterInterface potentialParent) {
		if (this.type != InterfaceType.VIF || potentialParent.getType() != InterfaceType.ETHERNET) {
			return false;
		}
		return this.interfaceName.startsWith(potentialParent.getInterfaceName() + ".");
	}

	/**
	 * Returns the physical parent name (e.g., returns "eth0" for both "eth0" and "eth0.1000").
	 */
	public String getParentName() {
		if (this.type == InterfaceType.VIF) {
			return this.interfaceName.split("\\.")[0];
		}
		return this.interfaceName;
	}

	/**
	 * Extracts the VLAN ID if this is a VIF.
	 */
	public String getVlanId() {
		if (this.type == InterfaceType.VIF) {
			return this.interfaceName.split("\\.")[1];
		}
		return null;
	}

	// --- CORE LOGIC ---

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
			for (RouterInterface childVif : owner.getInterfaces()) {
				if (childVif.isChildOf(this)) {
					childVif.updateLinkState(topology);
				}
			}
		}
	}

	private void processVifLinkState(NetworkTopology topology) {
		String parentName = this.getParentName();
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