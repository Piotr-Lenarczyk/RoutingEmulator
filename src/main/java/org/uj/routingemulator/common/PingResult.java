package org.uj.routingemulator.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Result of a single ping probe.
 */
public record PingResult(int sequence, boolean success, int hopCount, long rttMs, String errorMessage) {
}

