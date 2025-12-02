package org.example.thesisuj.host;

import lombok.Data;
import org.example.thesisuj.common.IPAddress;
import org.example.thesisuj.common.MacAddress;
import org.example.thesisuj.common.SubnetMask;

@Data
public class Host {
	private IPAddress ipAddress;
	private SubnetMask subnetMask;
	private IPAddress defaultGateway;
	private MacAddress macAddress;

	/**
	 * Creates a new host with the specified network configuration.
	 *
	 * @param ipAddress Host's IP address
	 * @param subnetMask Subnet mask
	 * @param defaultGateway Default gateway IP address
	 * @param macAddress MAC address (if null, a random one is generated)
	 */
	public Host(IPAddress ipAddress, SubnetMask subnetMask, IPAddress defaultGateway, MacAddress macAddress) {
		this.ipAddress = ipAddress;
		this.subnetMask = subnetMask;
		this.defaultGateway = defaultGateway;
		if (macAddress == null) {
			this.macAddress = new MacAddress();
		} else {
			this.macAddress = macAddress;
		}
	}
}
