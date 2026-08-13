package org.uj.routingemulator.common;

/**
 * Outcome of forwarding a packet through the topology.
 *
 * @param reason e.g., "No route", "TTL expired"
 */
public record ForwardingOutcome(boolean reached, int hopCount, String reason) {
}

