package org.uj.routingemulator.common;

import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Aggregated ping statistics for a ping operation.
 */
public record PingStatistics(List<PingResult> results) {

    public int getSent() {
        return results.size();
    }

    public int getReceived() {
        return (int) results.stream().filter(PingResult::success).count();
    }

    public double getLossPercent() {
        if (getSent() == 0) return 100.0;
        return 100.0 * (getSent() - getReceived()) / getSent();
    }

}

