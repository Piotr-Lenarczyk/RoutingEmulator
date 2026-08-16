package org.uj.routingemulator.router;

import org.uj.routingemulator.common.IPAddress;
import org.uj.routingemulator.common.Subnet;
import org.uj.routingemulator.common.exceptions.InvalidNextHopException;
import org.uj.routingemulator.common.exceptions.InvalidSubnetException;

public record NextHopRouteParameters(Subnet dest, IPAddress nh) {
	public static NextHopRouteParameters parseRouteParameters(String destinationSubnet, String nextHop) {
		Subnet dest;
		try {
			dest = Subnet.fromString(destinationSubnet);
		} catch (RuntimeException e) {
			String msg = String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", destinationSubnet);
			throw new InvalidSubnetException(msg);
		}

		IPAddress nh;
		try {
			nh = IPAddress.fromString(nextHop);
		} catch (Exception e) {
			if (nextHop != null && nextHop.contains("/")) {
				String msg = String.format("%n\tError: %s is not a valid IPv4 prefix%n%n%n\tInvalid value%n\tValue validation failed%n\tSet failed%n%n[edit]", nextHop);
				throw new InvalidNextHopException(msg);
			}
			throw (RuntimeException) e;
		}
		return new NextHopRouteParameters(dest, nh);
	}
}
