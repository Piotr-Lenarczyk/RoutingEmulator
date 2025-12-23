package org.uj.routingemulator.switching;

import lombok.Getter;
import lombok.Setter;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

@Getter
@Setter
public class SwitchPort implements NetworkInterface {
	private String interfaceName;
	private Subnet subnet;
	private MacAddress macAddress;

	public SwitchPort() {
		this.interfaceName = "";
		this.subnet = null;
		this.macAddress = new MacAddress();
	}

	public SwitchPort(String interfaceName, Subnet subnet) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = new MacAddress();
	}

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
