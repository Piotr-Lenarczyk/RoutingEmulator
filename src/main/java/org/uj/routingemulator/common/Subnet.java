package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents a network subnet (network address + subnet mask).
 * The network address should always have all host bits set to 0.
 * For example: 192.168.1.0/24, 10.0.0.0/8, etc.
 *
 * <p>This is distinct from {@link InterfaceAddress} which represents
 * an IP address assigned to an interface.
 */
@Getter
@EqualsAndHashCode
public class Subnet {
	private final IPAddress networkAddress;
	private final SubnetMask subnetMask;

	/**
	 * Creates a subnet with the given network address and mask.
	 *
	 * @param networkAddress the network address (should have host bits = 0)
	 * @param subnetMask the subnet mask
	 */
	public Subnet(IPAddress networkAddress, SubnetMask subnetMask) {
		this.networkAddress = networkAddress;
		this.subnetMask = subnetMask;
	}

	/**
	 * Parses a subnet from CIDR notation (e.g., "192.168.1.0/24").
	 *
	 * @param subnetString String in CIDR format
	 * @return Subnet object
	 * @throws IllegalArgumentException if the format is invalid
	 */
	public static Subnet fromString(String subnetString) {
		String[] parts = subnetString.split("/");
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid subnet format: " + subnetString);
		}
		IPAddress networkAddress = IPAddress.fromString(parts[0]);
		SubnetMask subnetMask = new SubnetMask(Integer.parseInt(parts[1]));
		return new Subnet(networkAddress, subnetMask);
	}

	/**
	 * Checks if the network address is actually a network address (all host bits are 0).
	 * For example, 192.168.1.0/24 is a network address, but 192.168.1.1/24 is not.
	 *
	 * @return true if this is a valid network address, false otherwise
	 */
	public boolean isValidNetworkAddress() {
		int prefixLength = subnetMask.getShortMask();
		if (prefixLength == 32) {
			// /32 is a host route, but technically valid
			return true;
		}

		// Calculate the number of host bits
		int hostBits = 32 - prefixLength;

		// Create a mask for host bits (all 1s in host portion)
		long hostMask = (1L << hostBits) - 1;

		// Convert IP address to a 32-bit integer
		long ipAsLong = ((long) networkAddress.getOctet1() << 24) |
		                ((long) networkAddress.getOctet2() << 16) |
		                ((long) networkAddress.getOctet3() << 8) |
		                (networkAddress.getOctet4());

		// Check if all host bits are 0
		return (ipAsLong & hostMask) == 0;
	}

	@Override
	public String toString() {
		return networkAddress.toString() + "/" + subnetMask.getShortMask();
	}
}
