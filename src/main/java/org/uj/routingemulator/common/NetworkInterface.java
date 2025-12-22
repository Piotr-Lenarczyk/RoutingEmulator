package org.uj.routingemulator.common;

public interface NetworkInterface {
	String getInterfaceName();
	void setInterfaceName(String interfaceName);

	Subnet getSubnet();
	void setSubnet(Subnet subnet);

	MacAddress getMacAddress();
	void setMacAddress(MacAddress macAddress);
}
