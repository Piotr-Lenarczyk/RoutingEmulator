package org.uj.routingemulator.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

/**
 * Represents a network interface on a host device.
 * <p>
 * Host interfaces have a subnet configuration and default gateway for routing.
 * The interface is always active (no administrative state management).
 */
@Getter
@Setter
public class HostInterface implements NetworkInterface {
	private String interfaceName;
	private Subnet subnet;
	private MacAddress macAddress;
	private IPAddress defaultGateway;

	/**
	 * Creates a host interface with default values.
	 * MAC address is randomly generated.
	 */
	public HostInterface() {
		this.interfaceName = "";
		this.subnet = null;
		this.macAddress = new MacAddress();
		this.defaultGateway = null;
	}

	/**
	 * Creates a host interface with specified configuration.
	 *
	 * @param interfaceName the name of the interface
	 * @param subnet the subnet configuration including IP and mask
	 * @param defaultGateway the default gateway IP address
	 */
	public HostInterface(String interfaceName, Subnet subnet, IPAddress defaultGateway) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = new MacAddress();
		this.defaultGateway = defaultGateway;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("HostInterface(interfaceName=").append(interfaceName);
		if (subnet != null) {
			sb.append(", subnet=").append(subnet);
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
