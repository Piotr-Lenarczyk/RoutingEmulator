package org.uj.routingemulator.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class IPAddress {
	private int octet1;
	private int octet2;
	private int octet3;
	private int octet4;

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
	 * @param ipString String representation of the IP address
	 * @return IPAddress object
	 * @throws RuntimeException if the format is invalid
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

	@Override
	public String toString() {
		return octet1 + "." + octet2 + "." + octet3 + "." + octet4;
	}
}
