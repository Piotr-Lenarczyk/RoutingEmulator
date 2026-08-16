package org.uj.routingemulator.host;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.*;

/**
 * Represents a network interface on a host device.
 * <p>
 * Host interfaces have an IP address, subnet mask, and default gateway for routing.
 * The interface is always active (no administrative state management).
 */
@Getter
@Setter
public class HostInterface implements NetworkInterface {
	private String interfaceName;
	private InterfaceAddress interfaceAddress;
	private MacAddress macAddress;
	private IPAddress defaultGateway;

	/**
	 * Creates a host interface with default values.
	 * MAC address is randomly generated.
	 */
	public HostInterface() {
		this.interfaceName = "";
		this.interfaceAddress = null;
		this.macAddress = new MacAddress();
		this.defaultGateway = null;
	}

	/**
	 * Creates a host interface with specified configuration.
	 *
	 * @param interfaceName the name of the interface
	 * @param interfaceAddress the IP address and subnet mask assigned to this interface
	 * @param defaultGateway the default gateway IP address
	 */
	public HostInterface(String interfaceName, InterfaceAddress interfaceAddress, IPAddress defaultGateway) {
		this.interfaceName = interfaceName;
		this.interfaceAddress = interfaceAddress;
		this.macAddress = new MacAddress();
		this.defaultGateway = defaultGateway;
	}

	/**
	 * Gets the subnet (network) this interface belongs to.
	 *
	 * @return Subnet this interface belongs to, or null if no address is configured
	 */
	@Override
	public Subnet getSubnet() {
		return interfaceAddress != null ? interfaceAddress.getSubnet() : null;
	}

	/**
	 * Sets the subnet by converting to interface address.
	 *
	 * @param subnet the subnet to set
	 */
	@Override
	public void setSubnet(Subnet subnet) {
		if (subnet != null) {
			this.interfaceAddress = new InterfaceAddress(subnet.networkAddress(), subnet.subnetMask());
		} else {
			this.interfaceAddress = null;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("HostInterface(interfaceName=").append(interfaceName);
		if (interfaceAddress != null) {
			sb.append(", interfaceAddress=").append(interfaceAddress);
		}
		if (macAddress != null) {
			sb.append(", macAddress=").append(macAddress);
		}
		if (defaultGateway != null) {
			sb.append(", defaultGateway=").append(defaultGateway);
		}
		sb.append(")");
		return sb.toString();
	}
}
