package org.uj.routingemulator.router;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

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
	private String interfaceName;
	private InterfaceAddress interfaceAddress;
	private MacAddress macAddress;
	private String description;
	private String vrf = "default";
	private int mtu;
	private InterfaceStatus status;

	/**
	 * Creates a router interface with the specified name.
	 * <p>
	 * The interface is created with:
	 * <ul>
	 *   <li>No IP address configured</li>
	 *   <li>Random MAC address</li>
	 *   <li>Administrative state UP</li>
	 *   <li>MTU based on interface type (1500 for eth*, 65536 for lo)</li>
	 *   <li>Default VRF</li>
	 * </ul>
	 *
	 * @param interfaceName the name of the interface (e.g., "eth0", "lo")
	 */
	public RouterInterface(String interfaceName) {
		this.interfaceName = interfaceName;
		this.interfaceAddress = null;
		this.macAddress = new MacAddress();
		this.description = null;
		if (interfaceName.startsWith("eth")) {
			this.mtu = 1500;
		} else if (interfaceName.startsWith("lo")) {
			this.mtu = 65536;
		}
		// Interface starts with admin UP but link DOWN (no physical connection yet)
		this.status = InterfaceStatus.fromChars('u', 'D');
	}

	/**
	 * Creates a router interface with full configuration.
	 *
	 * @param interfaceName the name of the interface
	 * @param interfaceAddress the IP address and subnet mask
	 * @param macAddress the MAC address
	 * @param mtu the Maximum Transmission Unit
	 * @param status the interface status (admin and link state)
	 */
	public RouterInterface(String interfaceName, InterfaceAddress interfaceAddress, MacAddress macAddress, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.interfaceAddress = interfaceAddress;
		this.macAddress = macAddress;
		this.mtu = mtu;
		this.status = status;
	}

	/**
	 * Creates a router interface with full configuration including VRF.
	 *
	 * @param interfaceName the name of the interface
	 * @param interfaceAddress the IP address and subnet mask
	 * @param macAddress the MAC address
	 * @param vrf the VRF (Virtual Routing and Forwarding) name
	 * @param mtu the Maximum Transmission Unit
	 * @param status the interface status (admin and link state)
	 */
	public RouterInterface(String interfaceName, InterfaceAddress interfaceAddress, MacAddress macAddress, String vrf, int mtu, InterfaceStatus status) {
		this.interfaceName = interfaceName;
		this.interfaceAddress = interfaceAddress;
		this.macAddress = macAddress;
		this.vrf = vrf;
		this.mtu = mtu;
		this.status = status;
	}

	/**
	 * Gets the subnet (network) this interface belongs to.
	 * This is a convenience method that calculates the subnet from the interface address.
	 *
	 * @return Subnet this interface belongs to, or null if no address is configured
	 */
	public Subnet getSubnet() {
		return interfaceAddress != null ? interfaceAddress.getSubnet() : null;
	}

	/**
	 * Sets the subnet by converting to interface address.
	 * This is deprecated - use setInterfaceAddress instead.
	 *
	 * @param subnet the subnet to set
	 * @deprecated Use {@link #setInterfaceAddress(InterfaceAddress)} instead
	 */
	@Deprecated
	public void setSubnet(Subnet subnet) {
		// For backward compatibility - interpret as setting the interface address
		// to the network address (though this is semantically incorrect)
		if (subnet != null) {
			this.interfaceAddress = new InterfaceAddress(subnet.getNetworkAddress(), subnet.getSubnetMask());
		} else {
			this.interfaceAddress = null;
		}
	}

	/**
	 * Copy constructor for creating a deep copy of RouterInterface.
	 * @param other The RouterInterface to copy
	 */
	public RouterInterface(RouterInterface other) {
		this.interfaceName = other.interfaceName;
		this.interfaceAddress = other.interfaceAddress; // InterfaceAddress is immutable
		this.macAddress = other.macAddress; // MacAddress is immutable
		this.description = other.description;
		this.vrf = other.vrf;
		this.mtu = other.mtu;
		this.status = other.status; // InterfaceStatus is immutable
	}

	/**
	 * Administratively disables the interface.
	 * <p>
	 * Sets administrative state to ADMIN_DOWN while preserving the current link state.
	 *
	 * @throws RuntimeException if the interface is already administratively disabled
	 */
	public void disable() {
		if (this.status.getAdmin() == AdminState.ADMIN_DOWN) {
			throw new RuntimeException("Configuration path: [interfaces ethernet %s disable] already exists".formatted(this.interfaceName));
		}
		this.status = new InterfaceStatus(AdminState.ADMIN_DOWN, this.status.getLink());
	}

	/**
	 * Administratively enables the interface.
	 * <p>
	 * Sets administrative state to UP while preserving the current link state.
	 *
	 * @throws RuntimeException if the interface is already administratively enabled
	 */
	public void enable() {
		if (this.status.getAdmin() == AdminState.UP) {
			throw new RuntimeException("Nothing to delete (the specified node does not exist)");
		}
		this.status = new InterfaceStatus(AdminState.UP, this.status.getLink());
	}

	/**
	 * Checks if the interface is disabled (either administratively or physically).
	 * <p>
	 * An interface is considered disabled if:
	 * <ul>
	 *   <li>Administrative state is ADMIN_DOWN (shutdown), or</li>
	 *   <li>Link state is DOWN (no physical connection)</li>
	 * </ul>
	 *
	 * @return true if the interface is disabled, false if it is operational
	 */
	public boolean isDisabled() {
		return this.status.getAdmin() == AdminState.ADMIN_DOWN || this.status.getLink() == LinkState.DOWN;
	}

	/**
	 * Updates the link state based on the network topology.
	 * <p>
	 * Link state is set to UP if:
	 * <ul>
	 *   <li>Interface has a physical connection in the topology</li>
	 *   <li>The neighboring interface is administratively UP (for RouterInterface neighbors)</li>
	 * </ul>
	 * <p>
	 * Otherwise, link state is set to DOWN.
	 * <p>
	 * This method should be called whenever:
	 * <ul>
	 *   <li>A connection is added or removed</li>
	 *   <li>A neighboring interface changes administrative state</li>
	 * </ul>
	 *
	 * @param topology the network topology to check for connections
	 */
	public void updateLinkState(org.uj.routingemulator.common.NetworkTopology topology) {
		LinkState newLinkState = topology.hasActiveConnection(this) ? LinkState.UP : LinkState.DOWN;
		this.status = new InterfaceStatus(this.status.getAdmin(), newLinkState);
	}
}
