package org.uj.routingemulator.gui;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.InterfaceAddress;
import org.uj.routingemulator.common.SubnetMask;
import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.host.HostInterface;

public class HostConfigurationService {

	public void configureHost(Host host, String ipText, String prefixText, String gatewayText) {
		IPAddress ip = IPAddress.fromString(ipText);
		int prefix = Integer.parseInt(prefixText);
		SubnetMask mask = new SubnetMask(prefix);
		InterfaceAddress interfaceAddress = new InterfaceAddress(ip, mask);

		IPAddress gateway = null;
		if (gatewayText != null && !gatewayText.trim().isEmpty()) {
			gateway = IPAddress.fromString(gatewayText);
		}

		HostInterface hi = host.getHostInterface();
		if (hi == null) {
			hi = new HostInterface();
		}

		hi.setInterfaceAddress(interfaceAddress);
		hi.setDefaultGateway(gateway);
		host.setHostInterface(hi);
	}
}