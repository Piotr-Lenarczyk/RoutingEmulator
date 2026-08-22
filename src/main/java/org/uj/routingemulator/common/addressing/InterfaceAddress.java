package org.uj.routingemulator.common.addressing;

/**
 * Represents an IP address assigned to a network interface along with its subnet mask.
 * This is distinct from {@link Subnet} which represents a network address/range.
 *
 * <p>For example, an interface might have InterfaceAddress of 192.168.1.1/24,
 * which means the interface has IP 192.168.1.1 and belongs to the 192.168.1.0/24 network.
 */
public record InterfaceAddress(IPAddress ipAddress, SubnetMask subnetMask) {
	/**
	 * Parses an interface address from CIDR notation (e.g., "192.168.1.1/24").
	 *
	 * @param addressString String in CIDR format
	 * @return InterfaceAddress object
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static InterfaceAddress fromString(String addressString) {
		String[] parts = addressString.split("/");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid interface address format: " + addressString);
		}
		IPAddress ipAddress = IPAddress.fromString(parts[0]);
		SubnetMask subnetMask = new SubnetMask(Integer.parseInt(parts[1]));
		return new InterfaceAddress(ipAddress, subnetMask);
	}

	/**
	 * Calculates the network address (subnet) this interface belongs to.
	 * For example, if interface has 192.168.1.1/24, this returns a Subnet of 192.168.1.0/24.
	 *
	 * @return Subnet representing the network this interface belongs to
	 */
	public Subnet getSubnet() {
		long networkAsLong = getNetworkAsLong();

		// Convert back to octets
		int octet1 = (int) ((networkAsLong >> 24) & 0xFF);
		int octet2 = (int) ((networkAsLong >> 16) & 0xFF);
		int octet3 = (int) ((networkAsLong >> 8) & 0xFF);
		int octet4 = (int) (networkAsLong & 0xFF);

		IPAddress networkAddress = new IPAddress(octet1, octet2, octet3, octet4);
		return new Subnet(networkAddress, subnetMask);
	}

	private long getNetworkAsLong() {
		int prefixLength = subnetMask.shortMask();

		// Convert IP to long
		long ipAsLong = ((long) ipAddress.octet1() << 24) |
				((long) ipAddress.octet2() << 16) |
				((long) ipAddress.octet3() << 8) |
				(ipAddress.octet4());

		// Create network mask (prefixLength 1s followed by 0s)
		long networkMask = (prefixLength == 0) ? 0 : (0xFFFFFFFFL << (32 - prefixLength));

		// Apply mask to get network address
		return ipAsLong & networkMask;
	}

	public boolean isValidHostAddress() {
		int prefixLength = subnetMask.shortMask();
		if (prefixLength == 32 || prefixLength == 31) {
			return true;
		}

		long hostMask = getHostMask(prefixLength);
		long hostPortion = getHostPortion(hostMask);

		return hostPortion != 0 && hostPortion != hostMask;
	}

	public boolean isNetworkAddress() {
		int prefixLength = subnetMask.shortMask();
		if (prefixLength == 32 || prefixLength == 31) {
			return false;
		}

		long hostMask = getHostMask(prefixLength);
		long hostPortion = getHostPortion(hostMask);

		return hostPortion == 0;
	}

	public boolean isBroadcastAddress() {
		int prefixLength = subnetMask.shortMask();
		if (prefixLength == 32 || prefixLength == 31) {
			return false;
		}

		long hostMask = getHostMask(prefixLength);
		long hostPortion = getHostPortion(hostMask);

		return hostPortion == hostMask;
	}

	/**
	 * Converts the 4 octets of the IP address into a single 32-bit unsigned long.
	 */
	private long getIpAsLong() {
		return ((long) ipAddress.octet1() << 24) |
				((long) ipAddress.octet2() << 16) |
				((long) ipAddress.octet3() << 8) |
				(ipAddress.octet4());
	}

	/**
	 * Calculates the bitmask for the host portion based on the prefix length.
	 */
	private long getHostMask(int prefixLength) {
		int hostBits = 32 - prefixLength;
		return (1L << hostBits) - 1;
	}

	/**
	 * Applies the host mask to the IP address to isolate the host portion.
	 */
	private long getHostPortion(long hostMask) {
		return getIpAsLong() & hostMask;
	}

	@Override
	public String toString() {
		return ipAddress.toString() + "/" + subnetMask.shortMask();
	}
}

