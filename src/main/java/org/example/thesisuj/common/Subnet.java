package org.example.thesisuj.common;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Subnet {
	private IPAddress networkAddress;
	private SubnetMask subnetMask;

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

	@Override
	public String toString() {
		return networkAddress.toString() + "/" + subnetMask.toString();
	}
}
