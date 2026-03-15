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

    public long getMinRtt() {
        return results.stream().filter(PingResult::isSuccess).mapToLong(PingResult::getRttMs).min().orElse(0L);
    }

    public long getMaxRtt() {
        return results.stream().filter(PingResult::isSuccess).mapToLong(PingResult::getRttMs).max().orElse(0L);
    }

    public double getAvgRtt() {
        return results.stream().filter(PingResult::isSuccess).mapToLong(PingResult::getRttMs).average().orElse(0.0);
    }
}

