package com.databurner.lib;

/**
 * Data container holding real-time telemetry statistics matching Data Burner app.
 */
public class BurnStats {

    private final long bytesPerSec;
    private final double speedMBps;
    private final double peakSpeedMBps;
    private final double avgSpeedMBps;
    private final long totalBytesBurned;
    private final double totalMBBurned;
    private final long elapsedSeconds;
    private final int activeThreads;
    private final String lastResponseStatus;

    public BurnStats(long bytesPerSec, double speedMBps, double peakSpeedMBps,
                     double avgSpeedMBps, long totalBytesBurned, double totalMBBurned,
                     long elapsedSeconds, int activeThreads, String lastResponseStatus) {
        this.bytesPerSec = bytesPerSec;
        this.speedMBps = speedMBps;
        this.peakSpeedMBps = peakSpeedMBps;
        this.avgSpeedMBps = avgSpeedMBps;
        this.totalBytesBurned = totalBytesBurned;
        this.totalMBBurned = totalMBBurned;
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

    public double getPeakSpeedMBps() {
        return peakSpeedMBps;
    }

    public double getAvgSpeedMBps() {
        return avgSpeedMBps;
    }

    public long getTotalBytesBurned() {
        return totalBytesBurned;
    }

    public double getTotalMBBurned() {
        return totalMBBurned;
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
        return String.format("BurnStats{speed=%.2f MB/s, peak=%.2f MB/s, avg=%.2f MB/s, total=%.2f MB, threads=%d, time=%s, status='%s'}",
                speedMBps, peakSpeedMBps, avgSpeedMBps, totalMBBurned, activeThreads, getFormattedSessionTime(), lastResponseStatus);
    }
}
