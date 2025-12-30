package org.uj.routingemulator.switching;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

/**
 * Represents a port on a network switch.
 * <p>
 * Switch ports are simplified L2 interfaces. They have a MAC address but typically
 * don't have IP configuration (switches operate at Layer 2).
 * The subnet field is optional and mainly used for management interfaces.
 */
@Getter
@Setter
public class SwitchPort implements NetworkInterface {
	private String interfaceName;
	private Subnet subnet;
	private MacAddress macAddress;

	/**
	 * Creates a switch port with default values.
	 * MAC address is randomly generated.
	 */
	public SwitchPort() {
		this.interfaceName = "";
		this.subnet = null;
		this.macAddress = new MacAddress();
	}

	/**
	 * Creates a switch port with name and subnet configuration.
	 *
	 * @param interfaceName the name of the port (e.g., "GigabitEthernet0/1")
	 * @param subnet the subnet configuration (typically null for L2 ports)
	 */
	public SwitchPort(String interfaceName, Subnet subnet) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = new MacAddress();
	}

	/**
	 * Creates a switch port with only a name.
	 *
	 * @param interfaceName the name of the port
	 */
	public SwitchPort(String interfaceName) {
		this.interfaceName = interfaceName;
		this.subnet = null;
		this.macAddress = new MacAddress();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SwitchPort(interfaceName=").append(interfaceName);
		if (subnet != null) {
			sb.append(", subnet=").append(subnet);
		}
		if (macAddress != null) {
			sb.append(", macAddress=").append(macAddress);
		}
		sb.append(")");
		return sb.toString();
	}
}
