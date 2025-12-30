package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Represents an IPv4 address.
 * <p>
 * An IPv4 address consists of four octets (bytes), each with a value between 0 and 255.
 * The address is represented in dotted-decimal notation (e.g., 192.168.1.1).
 * <p>
 * This class is immutable - once created, the address cannot be changed.
 * <p>
 * Examples:
 * <ul>
 *   <li>192.168.1.1 - typical private network address</li>
 *   <li>10.0.0.1 - private network address</li>
 *   <li>8.8.8.8 - public IP address</li>
 *   <li>127.0.0.1 - localhost</li>
 * </ul>
 */
@Getter
@EqualsAndHashCode
public class IPAddress {
	private final int octet1;
	private final int octet2;
	private final int octet3;
	private final int octet4;

	/**
	 * Creates an IPv4 address with the specified octets.
	 * Each octet must be in the range 0-255.
	 *
	 * @param octet1 first octet (0-255)
	 * @param octet2 second octet (0-255)
	 * @param octet3 third octet (0-255)
	 * @param octet4 fourth octet (0-255)
	 * @throws RuntimeException if any octet is outside the valid range
	 */
	public IPAddress(int octet1, int octet2, int octet3, int octet4) {
		validateOctet(octet1);
		validateOctet(octet2);
		validateOctet(octet3);
		validateOctet(octet4);
		this.octet1 = octet1;
		this.octet2 = octet2;
		this.octet3 = octet3;
		this.octet4 = octet4;
	}

	/**
	 * Validates that an octet value is within the valid range (0-255).
	 *
	 * @param octet Octet value to validate
	 * @throws RuntimeException if the octet is outside the valid range
	 */
	private void validateOctet(int octet) {
		if (octet < 0 || octet > 255) {
			throw new RuntimeException("Octet value must be between 0 and 255. Provided: " + octet);
		}
	}

	/**
	 * Parses an IP address from string format (e.g., "192.168.1.1").
	 *
	 * @param ipString String representation of the IP address in dotted-decimal notation
	 * @return IPAddress object
	 * @throws RuntimeException if the format is invalid or octets are out of range
	 * @throws NumberFormatException if any octet cannot be parsed as an integer
	 */
	public static IPAddress fromString(String ipString) {
	    String[] parts = ipString.split("\\.");
	    if (parts.length != 4) {
	        throw new RuntimeException("Invalid IP address format: " + ipString);
	    }
	    int octet1 = Integer.parseInt(parts[0]);
	    int octet2 = Integer.parseInt(parts[1]);
	    int octet3 = Integer.parseInt(parts[2]);
	    int octet4 = Integer.parseInt(parts[3]);
	    return new IPAddress(octet1, octet2, octet3, octet4);
	}

	/**
	 * Returns the string representation of this IP address in dotted-decimal notation.
	 *
	 * @return IP address as string (e.g., "192.168.1.1")
	 */
	@Override
	public String toString() {
		return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
	}
}
