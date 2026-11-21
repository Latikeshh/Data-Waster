package com.databurner.lib;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Server Manager for Data Burner Library.
 * Features a Smart Dynamic Web Link Extractor that parses live developer web pages
 * (developer.android.com/studio, releases.ubuntu.com) to extract direct binary CDN download links.
 */
public class ServerManager {

    public static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
    private static final List<String> SEED_WEB_PAGES = new ArrayList<>();
    private static final Pattern BINARY_URL_PATTERN = Pattern.compile("https?://[^\"]+?\\.(?:zip|iso|dmg|exe|bin|apk)");
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

        // Seed Web Pages to Scrape Dynamically
        SEED_WEB_PAGES.add("https://developer.android.com/studio");
        SEED_WEB_PAGES.add("https://releases.ubuntu.com/");
        SEED_WEB_PAGES.add("https://www.python.org/downloads/");
        SEED_WEB_PAGES.add("https://api.github.com/repos/Latikeshh/Data-Waster/releases/latest");
    }

    private final List<String> activeServers = new ArrayList<>();
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final Map<String, Long> blacklistTimestamps = new HashMap<>();

    public ServerManager() {
        this(null);
    }

    public ServerManager(List<String> customServers) {
        activeServers.addAll(DEFAULT_STREAM_SERVERS);
        if (customServers != null && !customServers.isEmpty()) {
            for (String server : customServers) {
                addCustomServer(server);
            }
        }
        fetchAndValidateDynamicMirrorsAsync();
    }

    /**
     * Asynchronously scrapes seed web pages (Google Android Studio, Ubuntu Releases),
     * parses HTML for direct binary CDN URLs, and registers verified links into active stream pool.
     */
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
                                addCustomServer(extractedUrl);
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

    /**
     * Evenly assigns a mirror URL to a worker thread based on threadIndex.
     * E.g. 20 threads and 5 URLs = exactly 4 threads per URL.
     */
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
        Integer current = failureCounts.get(url);
        int count = (current == null ? 0 : current) + 1;
        failureCounts.put(url, count);

        if (count >= 3) {
            blacklistTimestamps.put(url, System.currentTimeMillis() + 60_000L);
        }
    }

    public synchronized List<String> getActiveServers() {
        return Collections.unmodifiableList(new ArrayList<>(activeServers));
    }

    public synchronized void addCustomServer(String url) {
        if (url != null && !url.trim().isEmpty() && !activeServers.contains(url.trim())) {
            activeServers.add(url.trim());
        }
    }

    public synchronized void removeCustomServer(String url) {
        if (url != null) {
            activeServers.remove(url);
            failureCounts.remove(url);
            blacklistTimestamps.remove(url);
        }
    }

    public synchronized void resetToDefaults() {
        activeServers.clear();
        activeServers.addAll(DEFAULT_STREAM_SERVERS);
        failureCounts.clear();
        blacklistTimestamps.clear();
        fetchAndValidateDynamicMirrorsAsync();
    }

    public synchronized int getServerCount() {
        return activeServers.size();
    }
}
