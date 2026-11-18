package com.databurner.lib;

/**
 * Data container containing real-time data burning telemetry statistics.
 */
public class BurnStats {

    private final long bytesPerSec;
    private final double speedMBps;
    private final long totalBytesBurned;
    private final double totalMBBurned;
    private final double avgSpeedMBps;
    private final long elapsedSeconds;
    private final int activeThreads;
    private final String lastResponseStatus;

    public BurnStats(long bytesPerSec, double speedMBps, long totalBytesBurned,
                     double totalMBBurned, double avgSpeedMBps, long elapsedSeconds,
                     int activeThreads, String lastResponseStatus) {
        this.bytesPerSec = bytesPerSec;
        this.speedMBps = speedMBps;
        this.totalBytesBurned = totalBytesBurned;
        this.totalMBBurned = totalMBBurned;
        this.avgSpeedMBps = avgSpeedMBps;
        this.elapsedSeconds = elapsedSeconds;
        this.activeThreads = activeThreads;
        this.lastResponseStatus = lastResponseStatus;
    }

    public long getBytesPerSec() {
        return bytesPerSec;
    }

    public double getSpeedMBps() {
        return speedMBps;
    }

    public long getTotalBytesBurned() {
        return totalBytesBurned;
    }

    public double getTotalMBBurned() {
        return totalMBBurned;
    }

    public double getAvgSpeedMBps() {
        return avgSpeedMBps;
    }

    public long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getActiveThreads() {
        return activeThreads;
    }

    public String getLastResponseStatus() {
        return lastResponseStatus;
    }

    public String getFormattedSessionTime() {
        long sec = elapsedSeconds;
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }

    @Override
    public String toString() {
        return String.format("BurnStats{speed=%.2f MB/s, total=%.2f MB, threads=%d, time=%s, status='%s'}",
                speedMBps, totalMBBurned, activeThreads, getFormattedSessionTime(), lastResponseStatus);
    }
}
