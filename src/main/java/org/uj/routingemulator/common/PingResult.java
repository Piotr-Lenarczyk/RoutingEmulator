package org.uj.routingemulator.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * Result of a single ping probe.
 */
@Getter
@ToString
@AllArgsConstructor
public class PingResult {
    private final int sequence;
    private final boolean success;
    private final int hopCount;
    private final long rttMs;
    private final String errorMessage;
}

