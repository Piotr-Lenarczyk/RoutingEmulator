package org.uj.routingemulator.router;

public enum InterfaceType {
	ETHERNET,
	VIF,
	DUMMY,
	LOOPBACK,
	UNKNOWN;

	/**
	 * Resolves the interface type based on its standard naming convention.
	 *
	 * @param name the name of the interface (e.g., "eth0", "eth0.1000", "dum0", "lo")
	 * @return the corresponding InterfaceType
	 */
	public static InterfaceType fromName(String name) {
		if (name == null || name.isEmpty()) {
			return UNKNOWN;
		}
		if (name.matches("eth\\d+")) {
			return ETHERNET;
		} else if (name.matches("eth\\d+\\.\\d+")) {
			return VIF;
		} else if (name.matches("dum\\d+")) {
			return DUMMY;
		} else if (name.matches("lo")) {
			return LOOPBACK;
		} else {
			return UNKNOWN;
		}
	}
}
