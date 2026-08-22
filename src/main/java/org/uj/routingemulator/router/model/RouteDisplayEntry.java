package org.uj.routingemulator.router.model;

import org.uj.routingemulator.common.addressing.IPAddress;
import org.uj.routingemulator.common.addressing.Subnet;

/**
 * Helper record for displaying routing table entries.
 */
public record RouteDisplayEntry(
		String type,
		Subnet subnet,
		IPAddress nextHop,
		String interfaceName,
		int distance,
		boolean isDisabled,
		boolean isConnected
) {
}