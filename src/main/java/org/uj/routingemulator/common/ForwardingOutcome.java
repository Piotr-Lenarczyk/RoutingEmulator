package org.uj.routingemulator.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Outcome of forwarding a packet through the topology.
 */
@Getter
@ToString
@AllArgsConstructor
public class ForwardingOutcome {
	private final boolean reached;
	private final int hopCount;
	private final String reason; // e.g., "No route", "TTL expired"
}

