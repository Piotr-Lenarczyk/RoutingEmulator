package org.uj.routingemulator.router.model;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.common.addressing.Subnet;
import org.uj.routingemulator.router.exceptions.InvalidNextHopException;
import org.uj.routingemulator.router.exceptions.InvalidSubnetException;

public record NextHopRouteParameters(Subnet dest, IPAddress nh) {

	public static NextHopRouteParameters parseRouteParameters(String destinationSubnet, String nextHop) {
		Subnet dest;
		try {
			dest = Subnet.fromString(destinationSubnet);
		} catch (RuntimeException e) {
			throw new InvalidSubnetException(destinationSubnet + " is not a valid IPv4 prefix");
		}

		IPAddress nh;
		try {
			nh = IPAddress.fromString(nextHop);
		} catch (Exception e) {
			if (nextHop != null && nextHop.contains("/")) {
				throw new InvalidNextHopException(nextHop + " is not a valid IPv4 prefix");
			}
			throw (RuntimeException) e;
		}
		return new NextHopRouteParameters(dest, nh);
	}
}