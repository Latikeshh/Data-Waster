package com.databurner.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for initializing DataBurnerEngine.
 */
public class DataBurnerConfig {

    private final int threadCount;
    private final long quotaBytes;
    private final int bufferSize;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final String userAgent;
    private final List<String> customServers;

    protected DataBurnerConfig(DataBurnerConfigBuilder builder) {
        this.threadCount = builder.getThreadCount();
        this.quotaBytes = builder.getQuotaBytes();
        this.bufferSize = builder.getBufferSize();
        this.connectTimeoutMs = builder.getConnectTimeoutMs();
        this.readTimeoutMs = builder.getReadTimeoutMs();
        this.userAgent = builder.getUserAgent();
        this.customServers = builder.getCustomServers();
    }

    public int getThreadCount() {
        return threadCount;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public List<String> getCustomServers() {
        return customServers;
    }

    public static class Builder extends DataBurnerConfigBuilder {
    }
}
