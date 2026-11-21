package com.databurner.lib;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-performance Server Manager for Data Burner Library.
 * Features a Smart Dynamic Web Link Extractor that parses live web pages,
 * handles relative/absolute URLs, Range validation, friendly name labelling, current-session tracking,
 * 12-hour AM/PM timestamps, and strict blacklisting for both default and extracted mirrors.
 */
public class ServerManager {

    public static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
    private static final List<String> SEED_WEB_PAGES = new ArrayList<>();
    private static final List<String> DYNAMIC_SEED_CANDIDATES = new ArrayList<>();
    private static final Pattern LINK_HREF_PATTERN = Pattern.compile("href=[\"']?([^\"' >]+?\\.(?:zip|iso|dmg|exe|bin|apk))[\"']?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_URL_PATTERN = Pattern.compile("https?://[^\"]+?\\.(?:zip|iso|dmg|exe|bin|apk)", Pattern.CASE_INSENSITIVE);
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

    public static class MirrorStatusInfo {
        public final String url;
        public final String friendlyName;
        public final String source; // PREDEFINED_CDN, DYNAMIC_EXTRACTED, CUSTOM_INPUT
        public final boolean isBlacklisted;
        public final String timestamp12Hr;
        public final int failureCount;

        public MirrorStatusInfo(String url, String friendlyName, String source, boolean isBlacklisted, String timestamp12Hr, int failureCount) {
            this.url = url;
            this.friendlyName = friendlyName;
            this.source = source;
            this.isBlacklisted = isBlacklisted;
            this.timestamp12Hr = timestamp12Hr;
            this.failureCount = failureCount;
        }
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

    private final List<String> activeServers = new ArrayList<>();
    private final Map<String, String> serverSources = new HashMap<>();
    private final Map<String, String> serverTimestamps = new HashMap<>();
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final Map<String, Long> blacklistTimestamps = new HashMap<>();

    public ServerManager() {
        this(null);
    }

    public ServerManager(List<String> customServers) {
        activeServers.addAll(DEFAULT_STREAM_SERVERS);
        String nowStr = TIME_12HR_FORMAT.format(new Date());
        for (String url : DEFAULT_STREAM_SERVERS) {
            serverSources.put(url, "PREDEFINED_CDN");
            serverTimestamps.put(url, nowStr);
        }
        if (customServers != null && !customServers.isEmpty()) {
            for (String server : customServers) {
                addCustomServer(server);
            }
        }
        fetchAndValidateDynamicMirrorsAsync();
    }

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
            // 1. Validate & Register Dynamic Seed Candidates
            for (String candidateUrl : DYNAMIC_SEED_CANDIDATES) {
                if (validateBinaryUrl(candidateUrl)) {
                    addExtractedServer(candidateUrl);
                }
            }

            // 2. Web Scrape Seed Web Pages for Live Binary Links
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

                                    if (validateBinaryUrl(absoluteUrl)) {
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
                            if (validateBinaryUrl(rawUrl)) {
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

    private boolean validateBinaryUrl(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.trim().isEmpty()) return false;
        try {
            URL url = new URL(candidateUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=0-10");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
            int code = conn.getResponseCode();
            conn.disconnect();
            return (code == 200 || code == 206 || code == 301 || code == 302 || code == 303 || code == 307 || code == 308);
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
        Integer current = failureCounts.get(url);
        int count = (current == null ? 0 : current) + 1;
        failureCounts.put(url, count);

        if (count >= 3) {
            blacklistTimestamps.put(url, System.currentTimeMillis() + 60_000L);
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

            list.add(new MirrorStatusInfo(url, friendlyName, source, isBlacklisted, timestamp, failures));
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
