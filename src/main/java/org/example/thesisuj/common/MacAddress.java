package org.example.thesisuj.common;

import lombok.EqualsAndHashCode;

import java.security.SecureRandom;
import java.util.Arrays;

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

	/**
	 * Accepts a MAC address in formats like:
	 *  - "01:23:45:67:89:AB"
	 *  - "01-23-45-67-89-AB"
	 *  - "0123456789AB"
	 */
	public MacAddress(String mac) {
		byte[] parsed = parseMac(mac);
		System.arraycopy(parsed, 0, this.address, 0, 6);
	}

	/**
	 * Returns the MAC address as a byte array.
	 *
	 * @return Copy of the 6-byte MAC address array
	 */
	public byte[] getBytes() {
		return Arrays.copyOf(address, address.length);
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

	/**
	 * Parses a MAC address string into a byte array.
	 * Accepts formats with colons, dashes, or no separators.
	 *
	 * @param mac String representation of MAC address
	 * @return 6-byte array representing the MAC address
	 * @throws IllegalArgumentException if the format is invalid or contains non-hexadecimal characters
	 */
	private static byte[] parseMac(String mac) {
		if (mac == null) {
			throw new IllegalArgumentException("MAC address cannot be null");
		}

		String cleaned = mac.replace(":", "")
				.replace("-", "")
				.trim();

		if (cleaned.length() != 12) {
			throw new IllegalArgumentException("Invalid MAC format: " + mac);
		}

		byte[] bytes = new byte[6];
		try {
			for (int i = 0; i < 6; i++) {
				bytes[i] = (byte) Integer.parseInt(cleaned.substring(i * 2, i * 2 + 2), 16);
			}
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hexadecimal in MAC: " + mac);
		}

		return bytes;
	}
}
