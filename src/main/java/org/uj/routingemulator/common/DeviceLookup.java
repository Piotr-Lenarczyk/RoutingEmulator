package org.uj.routingemulator.common;

public class DeviceLookup {

	private DeviceLookup() {
	}

	public static String getDeviceName(NetworkInterface iface, NetworkTopology topology) {
		Device dev = topology.findDeviceByInterface(iface);
		if (dev != null) return dev.getDeviceName();
		return "Unknown";
	}

	public static String getDeviceNameFromObject(Object device) {
		if (device instanceof Device dev) return dev.getDeviceName();
		return "Unknown";
	}
}