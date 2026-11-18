package com.databurner.lib;

/**
 * Event listener interface for receiving real-time telemetry from DataBurnerEngine.
 */
public interface DataBurnerListener {

    /**
     * Triggered every second with updated telemetry statistics.
     */
    void onTick(BurnStats stats);

    /**
     * Triggered when engine status changes (e.g., Running, Stopped, Paused).
     */
    void onStatusChanged(String status);

    /**
     * Triggered when configured quota limit is reached.
     */
    void onQuotaReached(BurnStats finalStats);

    /**
     * Triggered when an unrecoverable error occurs.
     */
    void onError(Exception exception);
}
