package org.uj.routingemulator.common;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Aggregated ping statistics for a ping operation.
 */
@Getter
@ToString
public class PingStatistics {
    private final List<PingResult> results;

    public PingStatistics(List<PingResult> results) {
        this.results = results;
    }

    public int getSent() {
        return results.size();
    }

    public int getReceived() {
        return (int) results.stream().filter(PingResult::isSuccess).count();
    }

    public double getLossPercent() {
        if (getSent() == 0) return 100.0;
        return 100.0 * (getSent() - getReceived()) / getSent();
    }

}

