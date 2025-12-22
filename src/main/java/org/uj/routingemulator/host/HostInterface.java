package org.uj.routingemulator.host;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.MacAddress;
import org.uj.routingemulator.common.NetworkInterface;
import org.uj.routingemulator.common.Subnet;

@Getter
@Setter
public class HostInterface implements NetworkInterface {
	private String interfaceName;
	private Subnet subnet;
	private MacAddress macAddress;
	private IPAddress defaultGateway;

	public HostInterface() {
		this.interfaceName = "";
		this.subnet = null;
		this.macAddress = new MacAddress();
		this.defaultGateway = null;
	}

	public HostInterface(String interfaceName, Subnet subnet, IPAddress defaultGateway) {
		this.interfaceName = interfaceName;
		this.subnet = subnet;
		this.macAddress = new MacAddress();
		this.defaultGateway = defaultGateway;
	}
}
