package org.uj.routingemulator.common;

/**
 * Result of a single ping probe.
 */
public record PingResult(int sequence, boolean success, int hopCount, long rttMs, String errorMessage) {
}

