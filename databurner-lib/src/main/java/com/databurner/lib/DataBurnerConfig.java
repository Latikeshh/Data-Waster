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

    private DataBurnerConfig(Builder builder) {
        this.threadCount = builder.threadCount;
        this.quotaBytes = builder.quotaBytes;
        this.bufferSize = builder.bufferSize;
        this.connectTimeoutMs = builder.connectTimeoutMs;
        this.readTimeoutMs = builder.readTimeoutMs;
        this.userAgent = builder.userAgent;
        this.customServers = builder.customServers;
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

    public static class Builder {
        private int threadCount = 4;
        private long quotaBytes = 0; // 0 = unlimited
        private int bufferSize = 64 * 1024; // 64 KB buffer
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 8000;
        private String userAgent = "DataBurnerEngine/1.0";
        private final List<String> customServers = new ArrayList<>();

        public Builder setThreadCount(int threadCount) {
            if (threadCount < 1) throw new IllegalArgumentException("threadCount must be >= 1");
            this.threadCount = Math.min(10, threadCount);
            return this;
        }

        public Builder setQuotaMB(long quotaMB) {
            this.quotaBytes = quotaMB * 1024L * 1024L;
            return this;
        }

        public Builder setQuotaBytes(long quotaBytes) {
            this.quotaBytes = quotaBytes;
            return this;
        }

        public Builder setBufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder addCustomServer(String url) {
            if (url != null && !url.trim().isEmpty()) {
                this.customServers.add(url.trim());
            }
            return this;
        }

        public DataBurnerConfig build() {
            return new DataBurnerConfig(this);
        }
    }
}
