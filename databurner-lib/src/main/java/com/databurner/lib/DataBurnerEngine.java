package com.databurner.lib;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core high-performance multi-threaded Data Burning Engine.
 */
public class DataBurnerEngine {

    private final DataBurnerConfig config;
    private final ServerManager serverManager;
    private final AtomicLong totalBytesThisSec = new AtomicLong(0);
    private final AtomicLong totalBytesSession = new AtomicLong(0);
    private final AtomicLong totalActiveSeconds = new AtomicLong(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);

    private volatile boolean isRunning = false;
    private volatile boolean stopRequested = false;
    private volatile String lastResponseStatus = "Idle";

    private ExecutorService threadPool;
    private ScheduledExecutorService monitorScheduler;
    private DataBurnerListener listener;

    public DataBurnerEngine() {
        this(new DataBurnerConfig.Builder().build());
    }

    public DataBurnerEngine(DataBurnerConfig config) {
        this.config = config != null ? config : new DataBurnerConfig.Builder().build();
        this.serverManager = new ServerManager(this.config.getCustomServers());
    }

    public synchronized void setListener(DataBurnerListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public synchronized void start() {
        if (isRunning) return;
        isRunning = true;
        stopRequested = false;

        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", String.valueOf(config.getThreadCount() + 10));

        threadPool = Executors.newFixedThreadPool(config.getThreadCount());
        for (int i = 0; i < config.getThreadCount(); i++) {
            threadPool.execute(this::runStreamLoop);
        }

        monitorScheduler = Executors.newSingleThreadScheduledExecutor();
        monitorScheduler.scheduleAtFixedRate(this::monitorTick, 1, 1, TimeUnit.SECONDS);

        if (listener != null) {
            listener.onStatusChanged("Running");
        }
    }

    public synchronized void stop() {
        if (!isRunning) return;
        stopRequested = true;
        isRunning = false;

        if (threadPool != null) {
            threadPool.shutdownNow();
            threadPool = null;
        }
        if (monitorScheduler != null) {
            monitorScheduler.shutdownNow();
            monitorScheduler = null;
        }

        if (listener != null) {
            listener.onStatusChanged("Stopped");
        }
    }

    public void resetStats() {
        totalBytesThisSec.set(0);
        totalBytesSession.set(0);
        totalActiveSeconds.set(0);
    }

    private void runStreamLoop() {
        byte[] buffer = new byte[config.getBufferSize()];
        activeThreads.incrementAndGet();
        try {
            while (!stopRequested && isRunning) {
                HttpURLConnection conn = null;
                InputStream in = null;
                boolean errorOccurred = false;
                try {
                    URL url = new URL(serverManager.getRandomServerUrl());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Connection", "keep-alive");
                    conn.setRequestProperty("User-Agent", config.getUserAgent());
                    conn.setConnectTimeout(config.getConnectTimeoutMs());
                    conn.setReadTimeout(config.getReadTimeoutMs());
                    conn.connect();

                    int code = conn.getResponseCode();
                    lastResponseStatus = code + " " + conn.getResponseMessage();

                    if (code == 200) {
                        in = new BufferedInputStream(conn.getInputStream());
                        int read;
                        while (!stopRequested && isRunning && (read = in.read(buffer)) != -1) {
                            totalBytesThisSec.addAndGet(read);
                            totalBytesSession.addAndGet(read);
                        }
                    }
                } catch (Exception e) {
                    errorOccurred = true;
                    lastResponseStatus = "Connecting...";
                    if (listener != null) {
                        listener.onError(e);
                    }
                } finally {
                    try {
                        if (in != null) in.close();
                    } catch (Exception ignored) {}
                }
                if (!stopRequested && isRunning && errorOccurred) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                }
            }
        } finally {
            activeThreads.decrementAndGet();
        }
    }

    private void monitorTick() {
        if (!isRunning) return;

        long bytesLastSec = totalBytesThisSec.getAndSet(0);
        long totalBytes = totalBytesSession.get();
        long activeSecs = totalActiveSeconds.incrementAndGet();

        double speedMBs = bytesLastSec / (1024.0 * 1024.0);
        double totalMBs = totalBytes / (1024.0 * 1024.0);
        double avgSpeedMBs = activeSecs > 0 ? (totalMBs / activeSecs) : 0.0;

        BurnStats stats = new BurnStats(
                bytesLastSec,
                speedMBs,
                totalBytes,
                totalMBs,
                avgSpeedMBs,
                activeSecs,
                activeThreads.get(),
                lastResponseStatus
        );

        if (listener != null) {
            listener.onTick(stats);
        }

        if (config.getQuotaBytes() > 0 && totalBytes >= config.getQuotaBytes()) {
            stop();
            if (listener != null) {
                listener.onQuotaReached(stats);
            }
        }
    }

    public ServerManager getServerManager() {
        return serverManager;
    }

    public DataBurnerConfig getConfig() {
        return config;
    }
}
