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
}
