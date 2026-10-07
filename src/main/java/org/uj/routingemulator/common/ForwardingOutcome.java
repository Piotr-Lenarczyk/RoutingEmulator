package org.uj.routingemulator.common;

/**
 * Outcome of forwarding a packet through the topology.
 *
 * @param reason e.g., "No route", "TTL expired"
 * @param errorResponseDeliverable whether the router that stopped forwarding can return an ICMP error
 *                                 to the source
 */
public record ForwardingOutcome(boolean reached, int hopCount, String reason, boolean errorResponseDeliverable) {
	public ForwardingOutcome(boolean reached, int hopCount, String reason) {
		this(reached, hopCount, reason, true);
	}
}

