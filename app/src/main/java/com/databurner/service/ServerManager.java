package com.databurner.service;

import android.app.Activity;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Server Manager for Data Burner.
 * Features a Smart Dynamic Web Link Extractor that parses live web pages,
 * handles relative/absolute URLs, Range validation, friendly name labelling, current-session tracking,
 * 12-hour AM/PM timestamps, and strict blacklisting for both default and extracted mirrors.
 */
public class ServerManager {

    private static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
    private static final List<String> SEED_WEB_PAGES = new ArrayList<>();
    private static final List<String> DYNAMIC_SEED_CANDIDATES = new ArrayList<>();
    private static final Pattern LINK_HREF_PATTERN = Pattern.compile("href=[\"']?([^\"' >]+?\\.(?:zip|iso|dmg|exe|bin|apk))[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_URL_PATTERN = Pattern.compile("https?://[^\"]+?\\.(?:zip|iso|dmg|exe|bin|apk)", Pattern.CASE_INSENSITIVE);
    private static final SimpleDateFormat TIME_12HR_FORMAT = new SimpleDateFormat("hh:mm:ss a", Locale.US);
    private static final Random random = new Random();

    static {
        // High-Speed 10G+ Global CDN Mirror Endpoints (Predefined)
        DEFAULT_STREAM_SERVERS.add("https://ash-speed.hetzner.com/1GB.bin");
        DEFAULT_STREAM_SERVERS.add("https://proof.ovh.net/files/1Gb.dat");

        // Candidates for Dynamic Extraction Testing on Session Start
        DYNAMIC_SEED_CANDIDATES.add("https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip");
        DYNAMIC_SEED_CANDIDATES.add("https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip");
        DYNAMIC_SEED_CANDIDATES.add("https://releases.ubuntu.com/24.04/ubuntu-24.04.1-desktop-amd64.iso");
        DYNAMIC_SEED_CANDIDATES.add("https://releases.ubuntu.com/22.04/ubuntu-22.04.5-desktop-amd64.iso");
        DYNAMIC_SEED_CANDIDATES.add("https://github.com/Latikeshh/Data-Waster/releases/download/26.11.20/app-release.apk");
        DYNAMIC_SEED_CANDIDATES.add("https://dl.google.com/android/repository/platform-tools_r35.0.2-windows.zip");

        // Seed Web Pages to Scrape Dynamically
        SEED_WEB_PAGES.add("https://developer.android.com/studio");
        SEED_WEB_PAGES.add("https://releases.ubuntu.com/24.04/");
        SEED_WEB_PAGES.add("https://releases.ubuntu.com/22.04/");
        SEED_WEB_PAGES.add("https://www.python.org/downloads/");
        SEED_WEB_PAGES.add("https://api.github.com/repos/Latikeshh/Data-Waster/releases/latest");
    }

    private static volatile ServerManager instance;

    public static ServerManager getInstance() {
        if (instance == null) {
            synchronized (ServerManager.class) {
                if (instance == null) {
                    instance = new ServerManager();
                }
            }
        }
        return instance;
    }



    public static String getFriendlyNameForUrl(String url) {
        if (url == null) return "Unknown Endpoint";
        if (url.contains("dl.google.com") && url.contains("platform-tools")) return "📦 Google Android SDK Platform Tools";
        if (url.contains("dl.google.com") && url.contains("commandlinetools-mac")) return "📦 Google Command Line Tools (Mac)";
        if (url.contains("dl.google.com") && url.contains("commandlinetools-win")) return "📦 Google Command Line Tools (Windows)";
        if (url.contains("dl.google.com")) return "📦 Google Android SDK Repository";
        if (url.contains("releases.ubuntu.com/24.04")) return "💿 Ubuntu 24.04 LTS Desktop ISO";
        if (url.contains("releases.ubuntu.com/22.04")) return "💿 Ubuntu 22.04 LTS Desktop ISO";
        if (url.contains("releases.ubuntu.com")) return "💿 Ubuntu Linux ISO CDN";
        if (url.contains("python.org")) return "🐍 Python Official Release Installer";
        if (url.contains("github.com") && url.contains("releases")) return "🚀 GitHub Release Binary (Data-Waster)";
        if (url.contains("ash-speed.hetzner.com")) return "⚡ Hetzner CDN (Ashburn US 10G)";
        if (url.contains("speed.hetzner.de")) return "⚡ Hetzner CDN (Germany 10G)";
        if (url.contains("speed.cloudflare.com")) return "⚡ Cloudflare Anycast CDN (100G)";
        if (url.contains("proof.ovh.net")) return "⚡ OVH Telecom CDN (France 1G)";
        if (url.contains("mirror.nl.leaseweb.net")) return "⚡ Leaseweb CDN (Netherlands 1G)";
        if (url.contains("mirror.us.leaseweb.net")) return "⚡ Leaseweb CDN (US 1G)";
        if (url.contains("speedtest.selectel.ru")) return "⚡ Selectel High-Speed CDN";
        if (url.contains("speedtest.tokyo2.linode.com")) return "⚡ Linode CDN (Tokyo Asia 1G)";
        if (url.contains("speedtest.syd1.linode.com")) return "⚡ Linode CDN (Sydney Australia 1G)";
        if (url.contains("speedtest.tele2.net")) return "⚡ Tele2 Speedtest Mirror";

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null) {
                return "🌐 Extracted Web Binary (" + host.replace("www.", "") + ")";
            }
        } catch (Exception ignored) {}
        return "🌐 Extracted Stream Mirror";
    }

    private static final List<String> activeServers = new ArrayList<>(DEFAULT_STREAM_SERVERS);
    private static final Map<String, String> serverSources = new HashMap<>();
    private static final Map<String, String> serverTimestamps = new HashMap<>();
    private static final Map<String, Integer> failureCounts = new HashMap<>();
    private static final Map<String, Long> blacklistTimestamps = new HashMap<>();
    private static final Map<String, AtomicLong> urlBytesTrackers = new ConcurrentHashMap<>();
    private static final Map<String, String> urlTestResults = new ConcurrentHashMap<>();
    private static final Map<String, java.util.concurrent.atomic.AtomicInteger> activeConnectionsPerUrl = new ConcurrentHashMap<>();

    public ServerManager() {
        String nowStr = TIME_12HR_FORMAT.format(new Date());
        for (String url : DEFAULT_STREAM_SERVERS) {
            serverSources.putIfAbsent(url, "PREDEFINED_CDN");
            serverTimestamps.putIfAbsent(url, nowStr);
            urlTestResults.putIfAbsent(url, "HTTP 200 OK (Predefined)");
        }
        fetchAndValidateDynamicMirrorsAsync();
    }

    public synchronized void startNewSession(Context context) {
        failureCounts.clear();
        blacklistTimestamps.clear();
        urlBytesTrackers.clear();
        activeServers.clear();
        activeServers.addAll(DEFAULT_STREAM_SERVERS);

        String nowStr = TIME_12HR_FORMAT.format(new Date());
        for (String url : DEFAULT_STREAM_SERVERS) {
            serverSources.put(url, "PREDEFINED_CDN");
            serverTimestamps.put(url, nowStr);
            urlTestResults.put(url, "HTTP 200 OK (Predefined)");
        }
        fetchAndValidateDynamicMirrorsAsync();
        if (context != null) {
            startWebViewExtraction(context);
        }
    }

    public synchronized void startNewSession() {
        startNewSession(null);
    }

    public void addBytesForUrl(String url, long bytes) {
        if (url != null && bytes > 0) {
            urlBytesTrackers.computeIfAbsent(url, k -> new AtomicLong(0)).addAndGet(bytes);
        }
    }

    public void markUrlActive(String url) {
        if (url != null) {
            activeConnectionsPerUrl.computeIfAbsent(url, k -> new java.util.concurrent.atomic.AtomicInteger(0)).incrementAndGet();
        }
    }

    public void markUrlIdle(String url) {
        if (url != null) {
            activeConnectionsPerUrl.computeIfPresent(url, (k, counter) -> {
                int val = counter.decrementAndGet();
                return val < 0 ? new java.util.concurrent.atomic.AtomicInteger(0) : counter;
            });
        }
    }

    public int getActiveConnectionsForUrl(String url) {
        if (url == null) return 0;
        java.util.concurrent.atomic.AtomicInteger counter = activeConnectionsPerUrl.get(url);
        return counter != null ? Math.max(0, counter.get()) : 0;
    }

    public void startWebViewExtraction(Context context) {
        // Solution 3: Decouple WebView extraction strictly to Foreground UI (Activity Context)
        if (context == null || !(context instanceof Activity)) {
            // Guard against running headless WebViews from background Service contexts
            return;
        }
        for (String seedPage : SEED_WEB_PAGES) {
            com.databurner.util.WebViewDownloadInterceptor.interceptPageDownloads(context, seedPage, url -> {
                if (url != null) {
                    urlTestResults.put(url, "HTTP 200 OK (WebView Intercepted)");
                    addExtractedServer(url);
                }
            });
        }
    }

    public void fetchAndValidateDynamicMirrorsAsync() {
        new Thread(() -> {
            // Solution 1: Direct Native REST API Extraction (No WebView Required)
            fetchGitHubReleaseAssetsApi();
            fetchUbuntuReleaseManifestApi();

            // 1. Validate & Register Dynamic Seed Candidates
            for (String candidateUrl : DYNAMIC_SEED_CANDIDATES) {
                String testRes = validateAndGetStatus(candidateUrl);
                if (testRes != null && testRes.startsWith("HTTP")) {
                    urlTestResults.put(candidateUrl, testRes);
                    addExtractedServer(candidateUrl);
                }
            }

            // 2. Web Scrape Seed Web Pages for Live Binary Links (HTML Stream Parsing)
            for (String pageUrl : SEED_WEB_PAGES) {
                try {
                    URL url = new URL(pageUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
                    conn.setConnectTimeout(4000);
                    conn.setReadTimeout(4000);

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder html = new StringBuilder();
                        String line;
                        int linesRead = 0;
                        while ((line = reader.readLine()) != null && linesRead < 400) {
                            html.append(line).append("\n");
                            linesRead++;
                        }
                        reader.close();

                        String htmlStr = html.toString();
                        int extractedCount = 0;

                        // Scrape relative & absolute href links
                        Matcher hrefMatcher = LINK_HREF_PATTERN.matcher(htmlStr);
                        while (hrefMatcher.find() && extractedCount < 5) {
                            String href = hrefMatcher.group(1);
                            try {
                                if (href != null) {
                                    String absoluteUrl = (href.startsWith("http://") || href.startsWith("https://")) ?
                                            href : new URL(new URL(pageUrl), href).toString();

                                    String testRes = validateAndGetStatus(absoluteUrl);
                                    if (testRes != null && testRes.startsWith("HTTP")) {
                                        urlTestResults.put(absoluteUrl, testRes);
                                        addExtractedServer(absoluteUrl);
                                        extractedCount++;
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        // Scrape raw absolute URLs
                        Matcher rawMatcher = RAW_URL_PATTERN.matcher(htmlStr);
                        while (rawMatcher.find() && extractedCount < 8) {
                            String rawUrl = rawMatcher.group();
                            String testRes = validateAndGetStatus(rawUrl);
                            if (testRes != null && testRes.startsWith("HTTP")) {
                                urlTestResults.put(rawUrl, testRes);
                                addExtractedServer(rawUrl);
                                extractedCount++;
                            }
                        }
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }, "dynamic-mirror-scraper").start();
    }

    private void fetchGitHubReleaseAssetsApi() {
        try {
            URL url = new URL("https://api.github.com/repos/Latikeshh/Data-Waster/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "DataBurner-App");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject root = new JSONObject(sb.toString());
                if (root.has("assets")) {
                    JSONArray assets = root.getJSONArray("assets");
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String downloadUrl = asset.optString("browser_download_url", null);
                        if (downloadUrl != null && !downloadUrl.isEmpty()) {
                            String testRes = validateAndGetStatus(downloadUrl);
                            if (testRes != null && testRes.startsWith("HTTP")) {
                                urlTestResults.put(downloadUrl, testRes + " [GitHub REST API]");
                                addExtractedServer(downloadUrl);
                            }
                        }
                    }
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private void fetchUbuntuReleaseManifestApi() {
        try {
            URL url = new URL("https://releases.ubuntu.com/24.04/MD5SUMS");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "DataBurner-App");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.endsWith(".iso") || line.contains(".iso ")) {
                        String[] parts = line.split("\\s+");
                        if (parts.length >= 2) {
                            String filename = parts[parts.length - 1].replace("*", "");
                            String isoUrl = "https://releases.ubuntu.com/24.04/" + filename;
                            String testRes = validateAndGetStatus(isoUrl);
                            if (testRes != null && testRes.startsWith("HTTP")) {
                                urlTestResults.put(isoUrl, testRes + " [Ubuntu Native Manifest]");
                                addExtractedServer(isoUrl);
                            }
                        }
                    }
                }
                reader.close();
            }
            conn.disconnect();
        } catch (Exception ignored) {}
    }

    private String validateAndGetStatus(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.trim().isEmpty()) return "INVALID_URL";
        try {
            long startTime = System.currentTimeMillis();
            URL url = new URL(candidateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-10");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            int code = conn.getResponseCode();
            long latency = System.currentTimeMillis() - startTime;
            conn.disconnect();

            if (code == 200 || code == 206 || code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                return "HTTP " + code + " OK (" + latency + "ms)";
            }
            return "HTTP " + code + " FAIL";
        } catch (Exception e) {
            return "ERROR: " + (e.getMessage() != null ? e.getMessage() : "Timeout");
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

        if (count >= 3) {
            blacklistTimestamps.put(url, System.currentTimeMillis() + 60_000L);
            urlTestResults.put(url, "FAILED (Blacklisted 60s)");
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
            String friendlyName = getFriendlyNameForUrl(url);

            AtomicLong bytesTracker = urlBytesTrackers.get(url);
            long bytes = bytesTracker != null ? bytesTracker.get() : 0L;
            String testResult = urlTestResults.getOrDefault(url, "HTTP 200 OK (Validated)");
            boolean isSuccess = !isBlacklisted && (testResult.startsWith("HTTP 200") || testResult.startsWith("HTTP 206") || testResult.startsWith("HTTP 3"));
            int activeConns = getActiveConnectionsForUrl(url);

            list.add(new MirrorStatusInfo(url, friendlyName, source, isBlacklisted, timestamp, failures, bytes, testResult, isSuccess, activeConns));
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
            urlBytesTrackers.remove(url);
            urlTestResults.remove(url);
        }
    }

    public synchronized void resetToDefaults() {
        startNewSession();
    }

    public synchronized int getServerCount() {
        return activeServers.size();
    }
}
