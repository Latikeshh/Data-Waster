package com.databurner.lib;

/**
 * Data container representing current-session status and telemetry for a stream mirror endpoint.
 */
public class MirrorStatusInfo {
    public final String url;
    public final String friendlyName;
    public final String source; // PREDEFINED_CDN, DYNAMIC_EXTRACTED, CUSTOM_INPUT
    public final boolean isBlacklisted;
    public final String timestamp12Hr;
    public final int failureCount;
    public final long downloadedBytes;
    public final String testResultStatus;
    public final boolean isExtractionSuccess;

    public MirrorStatusInfo(String url, String friendlyName, String source, boolean isBlacklisted,
                            String timestamp12Hr, int failureCount, long downloadedBytes,
                            String testResultStatus, boolean isExtractionSuccess) {
        this.url = url;
        this.friendlyName = friendlyName;
        this.source = source;
        this.isBlacklisted = isBlacklisted;
        this.timestamp12Hr = timestamp12Hr;
        this.failureCount = failureCount;
        this.downloadedBytes = downloadedBytes;
        this.testResultStatus = testResultStatus;
        this.isExtractionSuccess = isExtractionSuccess;
    }
}
