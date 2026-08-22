package org.uj.routingemulator.common.addressing;

import org.uj.routingemulator.router.exceptions.InvalidAddressException;

/**
 * Represents an IPv4 address.
 * <p>
 * An IPv4 address consists of four octets (bytes), each with a value between 0 and 255.
 * The address is represented in dotted-decimal notation (e.g., 192.168.1.1).
 * <p>
 * This class is immutable - once created, the address cannot be changed.
 */
public record IPAddress(int octet1, int octet2, int octet3, int octet4) {

	public IPAddress {
		validateOctet(octet1);
		validateOctet(octet2);
		validateOctet(octet3);
		validateOctet(octet4);
	}

	private void validateOctet(int octet) {
		if (octet < 0 || octet > 255) {
			throw new IllegalArgumentException("Octet value must be between 0 and 255. Provided: " + octet);
		}
	}

	public static IPAddress fromString(String ipString) {
		try {
			String[] parts = ipString.split("\\.");
			if (parts.length != 4) {
				throw new InvalidAddressException("Invalid IP address format: " + ipString);
			}
			int octet1 = Integer.parseInt(parts[0]);
			int octet2 = Integer.parseInt(parts[1]);
			int octet3 = Integer.parseInt(parts[2]);
			int octet4 = Integer.parseInt(parts[3]);
			return new IPAddress(octet1, octet2, octet3, octet4);
		} catch (NumberFormatException e) {
			if (ipString.matches(".*/\\d{1,2}$")) {
				// Pass a clean message; CLIErrorHandler will apply the VyOS CLI formatting
				throw new InvalidAddressException(ipString + " is not a valid IPv4 prefix");
			}
			throw e;
		}
	}

	@Override
	public String toString() {
		return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
	}
}