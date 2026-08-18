package org.uj.routingemulator.common;

import org.uj.routingemulator.host.Host;
import org.uj.routingemulator.router.Router;
import org.uj.routingemulator.switching.Switch;

public class DeviceLookup {
	private DeviceLookup() {
	}

	public static String getDeviceName(NetworkInterface iface, NetworkTopology topology) {
		for (Router router : topology.getRouters()) {
			if (router.getInterfaces().stream().anyMatch(i -> i.equals(iface))) {
				return router.getName();
			}
		}
		for (Switch sw : topology.getSwitches()) {
			if (sw.getPorts().stream().anyMatch(port -> port.equals(iface))) {
				return sw.getName();
			}
		}
		for (Host host : topology.getHosts()) {
			if (host.getHostInterface().equals(iface)) {
				return host.getHostname();
			}
		}
		return "Unknown";
	}

	public static String getDeviceNameFromObject(Object device) {
		if (device instanceof Router router) return router.getName();
		if (device instanceof Switch sw) return sw.getName();
		if (device instanceof Host host) return host.getHostname();
		return "Unknown";
	}
}