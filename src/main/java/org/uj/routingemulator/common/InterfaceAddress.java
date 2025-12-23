package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents an IP address assigned to a network interface along with its subnet mask.
 * This is distinct from {@link Subnet} which represents a network address/range.
 *
 * <p>For example, an interface might have InterfaceAddress of 192.168.1.1/24,
 * which means the interface has IP 192.168.1.1 and belongs to the 192.168.1.0/24 network.
 */
@Getter
@EqualsAndHashCode
public class InterfaceAddress {
	private final IPAddress ipAddress;
	private final SubnetMask subnetMask;

	/**
	 * Creates an interface address configuration.
	 *
	 * @param ipAddress the IP address assigned to the interface (e.g., 192.168.1.1)
	 * @param subnetMask the subnet mask (e.g., /24)
	 */
	public InterfaceAddress(IPAddress ipAddress, SubnetMask subnetMask) {
		this.ipAddress = ipAddress;
		this.subnetMask = subnetMask;
	}

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
		int prefixLength = subnetMask.getShortMask();

		// Convert IP to long
		long ipAsLong = ((long) ipAddress.getOctet1() << 24) |
		                ((long) ipAddress.getOctet2() << 16) |
		                ((long) ipAddress.getOctet3() << 8) |
		                (ipAddress.getOctet4());

		// Create network mask (prefixLength 1s followed by 0s)
		long networkMask = (prefixLength == 0) ? 0 : (0xFFFFFFFFL << (32 - prefixLength));

		// Apply mask to get network address
		long networkAsLong = ipAsLong & networkMask;

		// Convert back to octets
		int octet1 = (int) ((networkAsLong >> 24) & 0xFF);
		int octet2 = (int) ((networkAsLong >> 16) & 0xFF);
		int octet3 = (int) ((networkAsLong >> 8) & 0xFF);
		int octet4 = (int) (networkAsLong & 0xFF);

		IPAddress networkAddress = new IPAddress(octet1, octet2, octet3, octet4);
		return new Subnet(networkAddress, subnetMask);
	}

	/**
	 * Checks if the IP address is a valid host address (not network or broadcast).
	 * For example, 192.168.1.0/24 is not a valid host address (network address),
	 * and 192.168.1.255/24 is not valid (broadcast address).
	 *
	 * @return true if this is a valid host address, false otherwise
	 */
	public boolean isValidHostAddress() {
		int prefixLength = subnetMask.getShortMask();
		if (prefixLength == 32) {
			// /32 is always valid (single host)
			return true;
		}
		if (prefixLength == 31) {
			// /31 point-to-point links - both addresses are valid
			return true;
		}

		// Convert IP to long
		long ipAsLong = ((long) ipAddress.getOctet1() << 24) |
		                ((long) ipAddress.getOctet2() << 16) |
		                ((long) ipAddress.getOctet3() << 8) |
		                (ipAddress.getOctet4());

		// Calculate host bits
		int hostBits = 32 - prefixLength;
		long hostMask = (1L << hostBits) - 1;

		// Get host portion
		long hostPortion = ipAsLong & hostMask;

		// Check if it's network address (all 0s) or broadcast (all 1s)
		return hostPortion != 0 && hostPortion != hostMask;
	}

	/**
	 * Checks if this IP address is a network address (all host bits are 0).
	 * For example, 192.168.1.0/24 is a network address.
	 *
	 * @return true if this is a network address
	 */
	public boolean isNetworkAddress() {
		int prefixLength = subnetMask.getShortMask();
		if (prefixLength == 32 || prefixLength == 31) {
			return false;
		}

		long ipAsLong = ((long) ipAddress.getOctet1() << 24) |
		                ((long) ipAddress.getOctet2() << 16) |
		                ((long) ipAddress.getOctet3() << 8) |
		                (ipAddress.getOctet4());

		int hostBits = 32 - prefixLength;
		long hostMask = (1L << hostBits) - 1;
		long hostPortion = ipAsLong & hostMask;

		return hostPortion == 0;
	}

	/**
	 * Checks if this IP address is a broadcast address (all host bits are 1).
	 * For example, 192.168.1.255/24 is a broadcast address.
	 *
	 * @return true if this is a broadcast address
	 */
	public boolean isBroadcastAddress() {
		int prefixLength = subnetMask.getShortMask();
		if (prefixLength == 32 || prefixLength == 31) {
			return false;
		}

		long ipAsLong = ((long) ipAddress.getOctet1() << 24) |
		                ((long) ipAddress.getOctet2() << 16) |
		                ((long) ipAddress.getOctet3() << 8) |
		                (ipAddress.getOctet4());

		int hostBits = 32 - prefixLength;
		long hostMask = (1L << hostBits) - 1;
		long hostPortion = ipAsLong & hostMask;

		return hostPortion == hostMask;
	}

	@Override
	public String toString() {
		return ipAddress.toString() + "/" + subnetMask.getShortMask();
	}
}

