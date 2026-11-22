package com.databurner.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder class for configuring DataBurnerConfig options.
 */
public class DataBurnerConfigBuilder {

    protected int threadCount = 4;
    protected long quotaBytes = 0; // 0 = unlimited
    protected int bufferSize = 64 * 1024; // 64 KB buffer
    protected int connectTimeoutMs = 5000;
    protected int readTimeoutMs = 8000;
    protected String userAgent = "DataBurnerEngine/1.0";
    protected final List<String> customServers = new ArrayList<>();

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

    public DataBurnerConfigBuilder setThreadCount(int threadCount) {
        if (threadCount < 1) throw new IllegalArgumentException("threadCount must be >= 1");
        this.threadCount = Math.min(10, threadCount);
        return this;
    }

    public DataBurnerConfigBuilder setQuotaMB(long quotaMB) {
        this.quotaBytes = quotaMB * 1024L * 1024L;
        return this;
    }

    public DataBurnerConfigBuilder setQuotaBytes(long quotaBytes) {
        this.quotaBytes = quotaBytes;
        return this;
    }

    public DataBurnerConfigBuilder setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    public DataBurnerConfigBuilder setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        return this;
    }

    public DataBurnerConfigBuilder setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
        return this;
    }

    public DataBurnerConfigBuilder setUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public DataBurnerConfigBuilder addCustomServer(String url) {
        if (url != null && !url.trim().isEmpty()) {
            this.customServers.add(url.trim());
        }
        return this;
    }

    public DataBurnerConfig build() {
        return new DataBurnerConfig(this);
    }
}
