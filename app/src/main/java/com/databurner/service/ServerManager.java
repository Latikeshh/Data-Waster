package com.databurner.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * High-performance Server Manager for Data Burner.
 * Manages ultra-high-speed 10G+ CDN binary stream mirrors with even thread distribution, smart failover, and auto-blacklisting.
 */
public class ServerManager {

    private static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
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
    }

    private final List<String> activeServers = new ArrayList<>(DEFAULT_STREAM_SERVERS);
    private final Map<String, Integer> failureCounts = new HashMap<>();
    private final Map<String, Long> blacklistTimestamps = new HashMap<>();

    public ServerManager() {
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
        int count = failureCounts.getOrDefault(url, 0) + 1;
        failureCounts.put(url, count);

        // Blacklist for 60 seconds if server fails 3 times
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
    }

    public synchronized int getServerCount() {
        return activeServers.size();
    }
}
