package org.uj.routingemulator.common.forwarding;

/**
 * Outcome of forwarding a packet through the topology.
 * @param reason e.g., NO_ROUTE, TTL_EXPIRED
 */
public record ForwardingOutcome(boolean reached, int hopCount, ForwardingReason reason) {
}