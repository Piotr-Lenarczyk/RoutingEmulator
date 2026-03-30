package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;

import java.security.SecureRandom;

/**
 * Represents a MAC (Media Access Control) address.
 * Supports random generation and parsing from various string formats.
 */
@EqualsAndHashCode
public class MacAddress {

	private static final SecureRandom RANDOM = new SecureRandom();
	private final byte[] address = new byte[6];

	/**
	 * Generates a random MAC address.
	 * The locally administered bit is set (02:xx:xx:xx:xx:xx)
	 */
	public MacAddress() {
		RANDOM.nextBytes(address);
		address[0] = (byte) (address[0] & (byte) 0b11111110); // Unicast
		address[0] = (byte) (address[0] | (byte) 0b00000010); // Locally administered
	}

	/** String representation (upper-case, colon-separated) */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(17);
		for (int i = 0; i < address.length; i++) {
			sb.append(String.format("%02X", address[i]));
			if (i < address.length - 1) sb.append(":");
		}
		return sb.toString();
	}

}
