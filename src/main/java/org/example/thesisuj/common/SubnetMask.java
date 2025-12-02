package org.example.thesisuj.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class SubnetMask {
	private int shortMask;

	public SubnetMask(int shortMask) {
		validateShortMask(shortMask);
		this.shortMask = shortMask;
	}

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
