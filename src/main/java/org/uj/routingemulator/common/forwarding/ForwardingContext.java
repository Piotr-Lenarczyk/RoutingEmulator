package org.uj.routingemulator.common.forwarding;

import org.uj.routingemulator.common.addressing.IPAddress;

/**
 * Immutable context containing all explicit parameters for a packet forwarding traversal,
 * unifying both primary forwarding and return-path verification semantics.
 */
public record ForwardingContext(
		IPAddress source,
		IPAddress destination,
		int maxHops,
		boolean decrementTtl,
		boolean verifyReturn,
		boolean isReturnVerification
) {
}