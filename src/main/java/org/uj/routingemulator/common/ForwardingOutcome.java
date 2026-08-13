package org.uj.routingemulator.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Outcome of forwarding a packet through the topology.
 *
 * @param reason e.g., "No route", "TTL expired"
 */
public record ForwardingOutcome(boolean reached, int hopCount, String reason) {
}

