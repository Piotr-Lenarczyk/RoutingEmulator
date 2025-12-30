package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents a subnet mask in CIDR notation (prefix length).
 * <p>
 * The mask is stored as an integer representing the number of network bits
 * in the subnet mask. For example:
 * <ul>
 *   <li>/24 = 255.255.255.0 (24 network bits)</li>
 *   <li>/16 = 255.255.0.0 (16 network bits)</li>
 *   <li>/8 = 255.0.0.0 (8 network bits)</li>
 * </ul>
 * <p>
 * Valid range is 0-32 for IPv4 addresses.
 */
@Getter
@EqualsAndHashCode
public class SubnetMask {
	private int shortMask;

	/**
	 * Creates a subnet mask with the specified prefix length.
	 *
	 * @param shortMask prefix length (0-32)
	 * @throws RuntimeException if prefix length is outside valid range
	 */
	public SubnetMask(int shortMask) {
		validateShortMask(shortMask);
		this.shortMask = shortMask;
	}

	/**
	 * Validates that the prefix length is within valid range (0-32).
	 *
	 * @param shortMask prefix length to validate
	 * @throws RuntimeException if prefix length is invalid
	 */
	private void validateShortMask(int shortMask) {
		if (shortMask < 0 || shortMask > 32) {
			throw new RuntimeException("Subnet mask must be between 0 and 32. Provided: " + shortMask);
		}
	}

	/**
	 * Parses a subnet mask from string representation.
	 *
	 * @param str String representation of prefix length
	 * @return SubnetMask object
	 * @throws NumberFormatException if string cannot be parsed as integer
	 */
	public static SubnetMask fromString(String str) {
		int shortMask = Integer.parseInt(str);
		return new SubnetMask(shortMask);
	}

	@Override
	public String toString() {
		return shortMask + "";
	}
}
