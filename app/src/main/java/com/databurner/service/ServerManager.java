package com.databurner.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Server Manager for Data Burner.
 * Features a Smart Dynamic Web Link Extractor that parses live web pages,
 * current-session tracking, 12-hour AM/PM timestamps, and strict blacklisting for both default and extracted mirrors.
 */
public class ServerManager {

    private static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
    private static final List<String> SEED_WEB_PAGES = new ArrayList<>();
    private static final Pattern BINARY_URL_PATTERN = Pattern.compile("https?://[^\"]+?\\.(?:zip|iso|dmg|exe|bin|apk)");
    private static final SimpleDateFormat TIME_12HR_FORMAT = new SimpleDateFormat("hh:mm:ss a", Locale.US);
    private static final Random random = new Random();

    static {
        // High-Speed 10G+ Global CDN Mirror Endpoints
        DEFAULT_STREAM_SERVERS.add("https://ash-speed.hetzner.com/1GB.bin");
        DEFAULT_STREAM_SERVERS.add("https://speed.hetzner.de/1GB.bin");
        DEFAULT_STREAM_SERVERS.add("https://speed.cloudflare.com/__down?bytes=100000000");
        DEFAULT_STREAM_SERVERS.add("https://proof.ovh.net/files/1Gb.dat");
        DEFAULT_STREAM_SERVERS.add("https://mirror.nl.leaseweb.net/speedtest/1000mb.bin");
        DEFAULT_STREAM_SERVERS.add("https://mirror.us.leaseweb.net/speedtest/1000mb.bin");
        DEFAULT_STREAM_SERVERS.add("https://speedtest.selectel.ru/100MB.bin");
        DEFAULT_STREAM_SERVERS.add("https://speedtest.tokyo2.linode.com/100MB-linode.bin");
        DEFAULT_STREAM_SERVERS.add("https://speedtest.syd1.linode.com/100MB-linode.bin");
        DEFAULT_STREAM_SERVERS.add("http://speedtest.tele2.net/100MB.zip");

        // Default High-Speed Google Android SDKs, Ubuntu ISOs & GitHub Release CDNs
        DEFAULT_STREAM_SERVERS.add("https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip");
        DEFAULT_STREAM_SERVERS.add("https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip");
        DEFAULT_STREAM_SERVERS.add("https://releases.ubuntu.com/24.04/ubuntu-24.04.1-desktop-amd64.iso");
        DEFAULT_STREAM_SERVERS.add("https://releases.ubuntu.com/22.04/ubuntu-22.04.5-desktop-amd64.iso");
        DEFAULT_STREAM_SERVERS.add("https://github.com/Latikeshh/Data-Waster/releases/download/26.11.20/app-release.apk");
        DEFAULT_STREAM_SERVERS.add("https://dl.google.com/android/repository/platform-tools_r35.0.2-windows.zip");

        // Seed Web Pages to Scrape Dynamically
        SEED_WEB_PAGES.add("https://developer.android.com/studio");
        SEED_WEB_PAGES.add("https://releases.ubuntu.com/");
        SEED_WEB_PAGES.add("https://www.python.org/downloads/");
        SEED_WEB_PAGES.add("https://api.github.com/repos/Latikeshh/Data-Waster/releases/latest");
    }

    public static class MirrorStatusInfo {
        public final String url;
        public final String source; // PREDEFINED_CDN, DYNAMIC_EXTRACTED, CUSTOM_INPUT
        public final boolean isBlacklisted;
        public final String timestamp12Hr;
        public final int failureCount;

        public MirrorStatusInfo(String url, String source, boolean isBlacklisted, String timestamp12Hr, int failureCount) {
            this.url = url;
            this.source = source;
            this.isBlacklisted = isBlacklisted;
            this.timestamp12Hr = timestamp12Hr;
            this.failureCount = failureCount;
        }
    }

    private final List<String> activeServers = new ArrayList<>(DEFAULT_STREAM_SERVERS);
    private final Map<String, String> serverSources = new HashMap<>();
    private final Map<String, String> serverTimestamps = new HashMap<>();
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final Map<String, Long> blacklistTimestamps = new HashMap<>();

    public ServerManager() {
        String nowStr = TIME_12HR_FORMAT.format(new Date());
        for (String url : DEFAULT_STREAM_SERVERS) {
            serverSources.put(url, "PREDEFINED_CDN");
            serverTimestamps.put(url, nowStr);
        }
        fetchAndValidateDynamicMirrorsAsync();
    }

    /**
     * Starts a fresh session: Clears previous session failure history and triggers fresh web scraping.
     */
    public synchronized void startNewSession() {
        failureCounts.clear();
        blacklistTimestamps.clear();
        activeServers.clear();
        activeServers.addAll(DEFAULT_STREAM_SERVERS);
        
        String nowStr = TIME_12HR_FORMAT.format(new Date());
        for (String url : DEFAULT_STREAM_SERVERS) {
            serverSources.put(url, "PREDEFINED_CDN");
            serverTimestamps.put(url, nowStr);
        }
        fetchAndValidateDynamicMirrorsAsync();
    }

    public void fetchAndValidateDynamicMirrorsAsync() {
        new Thread(() -> {
            for (String pageUrl : SEED_WEB_PAGES) {
                try {
                    URL url = new URL(pageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) DataBurner/2.0");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder html = new StringBuilder();
                        String line;
                        int linesRead = 0;
                        while ((line = reader.readLine()) != null && linesRead < 300) {
                            html.append(line).append("\n");
                            linesRead++;
                        }
                        reader.close();

                        Matcher matcher = BINARY_URL_PATTERN.matcher(html.toString());
                        int extractedCount = 0;
                        while (matcher.find() && extractedCount < 3) {
                            String extractedUrl = matcher.group();
                            if (validateBinaryUrl(extractedUrl)) {
                                addExtractedServer(extractedUrl);
                                extractedCount++;
                            }
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }, "dynamic-mirror-scraper").start();
    }

    private boolean validateBinaryUrl(String candidateUrl) {
        try {
            URL url = new URL(candidateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "DataBurner/2.0");
            int code = conn.getResponseCode();
            conn.disconnect();
            return (code == 200 || code == 301 || code == 302);
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized String getServerUrlForThread(int threadIndex) {
        long now = System.currentTimeMillis();
        List<String> available = new ArrayList<>();

        for (String server : activeServers) {
            Long blacklistedUntil = blacklistTimestamps.get(server);
            if (blacklistedUntil == null || now > blacklistedUntil) {
                available.add(server);
            }
        }

        if (available.isEmpty()) {
            blacklistTimestamps.clear();
            failureCounts.clear();
            available.addAll(activeServers);
        }

        if (available.isEmpty()) {
            return DEFAULT_STREAM_SERVERS.get(Math.abs(threadIndex) % DEFAULT_STREAM_SERVERS.size());
        }
        return available.get(Math.abs(threadIndex) % available.size());
    }

    public synchronized String getRandomServerUrl() {
        return getServerUrlForThread(random.nextInt(1000));
    }

    public synchronized void markServerSuccess(String url) {
        if (url != null) {
            failureCounts.remove(url);
            blacklistTimestamps.remove(url);
        }
    }

    public synchronized void markServerFailed(String url) {
        if (url == null) return;
        int count = failureCounts.getOrDefault(url, 0) + 1;
        failureCounts.put(url, count);

        // Strict Blacklisting: If ANY server (Predefined or Extracted) fails 3 times, blacklist for 60s
        if (count >= 3) {
            blacklistTimestamps.put(url, System.currentTimeMillis() + 60_000L);
            // Trigger background re-extraction if active servers are dropping
            fetchAndValidateDynamicMirrorsAsync();
        }
    }

    public synchronized List<MirrorStatusInfo> getSessionMirrorStatusList() {
        long now = System.currentTimeMillis();
        List<MirrorStatusInfo> list = new ArrayList<>();
        for (String url : activeServers) {
            Long blacklistedUntil = blacklistTimestamps.get(url);
            boolean isBlacklisted = (blacklistedUntil != null && now < blacklistedUntil);
            String source = serverSources.getOrDefault(url, "DYNAMIC_EXTRACTED");
            String timestamp = serverTimestamps.getOrDefault(url, TIME_12HR_FORMAT.format(new Date()));
            int failures = failureCounts.getOrDefault(url, 0);

            list.add(new MirrorStatusInfo(url, source, isBlacklisted, timestamp, failures));
        }
        return list;
    }

    public synchronized List<String> getActiveServers() {
        return Collections.unmodifiableList(new ArrayList<>(activeServers));
    }

    public synchronized void addExtractedServer(String url) {
        if (url != null && !url.trim().isEmpty() && !activeServers.contains(url.trim())) {
            String trimmed = url.trim();
            activeServers.add(trimmed);
            serverSources.put(trimmed, "DYNAMIC_EXTRACTED");
            serverTimestamps.put(trimmed, TIME_12HR_FORMAT.format(new Date()));
        }
    }

    public synchronized void addCustomServer(String url) {
        if (url != null && !url.trim().isEmpty() && !activeServers.contains(url.trim())) {
            String trimmed = url.trim();
            activeServers.add(trimmed);
            serverSources.put(trimmed, "CUSTOM_INPUT");
            serverTimestamps.put(trimmed, TIME_12HR_FORMAT.format(new Date()));
        }
    }

    public synchronized void removeCustomServer(String url) {
        if (url != null) {
            activeServers.remove(url);
            failureCounts.remove(url);
            blacklistTimestamps.remove(url);
            serverSources.remove(url);
            serverTimestamps.remove(url);
        }
    }

    public synchronized void resetToDefaults() {
        startNewSession();
    }

    public synchronized int getServerCount() {
        return activeServers.size();
    }
}
