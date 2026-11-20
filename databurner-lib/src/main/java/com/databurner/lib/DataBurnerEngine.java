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
 * High-performance multi-threaded Data Burning Engine matching Data Burner App implementation.
 */
public class DataBurnerEngine {

    private final DataBurnerConfig config;
    private final ServerManager serverManager;
    private final AtomicLong totalBytesThisSec = new AtomicLong(0);
    private final AtomicLong totalBytesSession = new AtomicLong(0);
    private final AtomicLong totalActiveSeconds = new AtomicLong(0);
    private static final AtomicInteger activeThreads = new AtomicInteger(0);

    private volatile boolean isRunning = false;
    private volatile boolean streamsRunning = false;
    private volatile boolean stopRequested = false;
    private static volatile String lastResponseStatus = "Idle";
    private volatile float peakSpeed = 0f;

    private ExecutorService executorService;
    private ScheduledExecutorService monitorScheduler;
    private DataBurnerListener listener;

    public DataBurnerEngine() {
        this(new DataBurnerConfig.Builder().build());
    }

    public DataBurnerEngine(DataBurnerConfig config) {
        this.config = config != null ? config : new DataBurnerConfig.Builder().build();
        this.serverManager = new ServerManager();
        if (this.config.getCustomServers() != null) {
            for (String server : this.config.getCustomServers()) {
                this.serverManager.addCustomServer(server);
            }
        }
    }

    public synchronized void setListener(DataBurnerListener listener) {
        this.listener = listener;
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    public static int getActiveThreads() {
        return activeThreads.get();
    }

    public static String getLastResponseStatus() {
        return lastResponseStatus;
    }

    public synchronized void start(Object androidContext) {
        if (androidContext != null) {
            AndroidBurnHelper.checkPermissionsAndShowDialogIfNeeded(androidContext, this::startInternal);
        } else {
            startInternal();
        }
    }

    public synchronized void start() {
        startInternal();
    }

    private synchronized void startInternal() {
        if (isRunning) return;
        isRunning = true;
        streamsRunning = true;
        stopRequested = false;

        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "35");

        int threadCount = config.getThreadCount();
        executorService = Executors.newFixedThreadPool(threadCount + 2);

        for (int i = 0; i < threadCount; i++) {
            executorService.execute(this::runStreamLoop);
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
        streamsRunning = false;
        isRunning = false;

        AndroidBurnHelper.releaseLocks();

        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
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
        peakSpeed = 0f;
    }

    private void runStreamLoop() {
        byte[] buffer = new byte[64 * 1024];
        activeThreads.incrementAndGet();
        try {
            while (!stopRequested && streamsRunning) {
                HttpURLConnection conn = null;
                InputStream in = null;
                boolean errorOccurred = false;
                try {
                    URL url = new URL(serverManager.getRandomServerUrl());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Connection", "keep-alive");
                    conn.setRequestProperty("User-Agent", "DataBurner/2.0");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(8000);
                    conn.connect();

                    int code = conn.getResponseCode();
                    lastResponseStatus = code + " " + conn.getResponseMessage();

                    if (code == 200) {
                        in = new BufferedInputStream(conn.getInputStream());
                        int read;
                        while (!stopRequested && streamsRunning && (read = in.read(buffer)) != -1) {
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
                    // Do NOT call conn.disconnect() to preserve persistent HTTP Keep-Alive connection pooling!
                }
                if (!stopRequested && streamsRunning && errorOccurred) {
                    try { Thread.sleep(60); } catch (InterruptedException ignored) {}
                }
            }
        } finally {
            activeThreads.decrementAndGet();
        }
    }

    private void monitorTick() {
        if (!isRunning) return;

        long elapsedSec = totalActiveSeconds.incrementAndGet();
        long bytes = totalBytesThisSec.getAndSet(0);
        float mb = bytes / (1024f * 1024f);

        if (mb > peakSpeed) {
            peakSpeed = mb;
        }

        long totalBytes = totalBytesSession.get();
        double totalMB = totalBytes / (1024.0 * 1024.0);
        float avgSpeed = elapsedSec > 0 ? (float) (totalMB / elapsedSec) : 0f;

        BurnStats stats = new BurnStats(
                bytes,
                mb,
                peakSpeed,
                avgSpeed,
                totalBytes,
                totalMB,
                elapsedSec,
                activeThreads.get(),
                lastResponseStatus
        );

        if (listener != null) {
            listener.onTick(stats);
        }

        long quotaBytes = config.getQuotaBytes();
        if (quotaBytes > 0 && totalBytes >= quotaBytes) {
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
