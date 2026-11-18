package com.databurner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ServerManager {

    private static final List<String> DEFAULT_STREAM_SERVERS = new ArrayList<>();
    private static final Random random = new Random();

    static {
        DEFAULT_STREAM_SERVERS.add("https://ash-speed.hetzner.com/1GB.bin");
        DEFAULT_STREAM_SERVERS.add("https://speed.hetzner.de/1GB.bin");
        DEFAULT_STREAM_SERVERS.add("https://ftp.otenet.gr/test100Mb.db");
        DEFAULT_STREAM_SERVERS.add("http://speedtest.tele2.net/100MB.zip");
        DEFAULT_STREAM_SERVERS.add("https://speedtest.belwue.net/10G");
    }

    private final List<String> activeServers = new ArrayList<>(DEFAULT_STREAM_SERVERS);

    public ServerManager() {
    }

    public synchronized String getRandomServerUrl() {
        if (activeServers.isEmpty()) {
            return DEFAULT_STREAM_SERVERS.get(random.nextInt(DEFAULT_STREAM_SERVERS.size()));
        }
        return activeServers.get(random.nextInt(activeServers.size()));
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
        }
    }

    public synchronized void resetToDefaults() {
        activeServers.clear();
        activeServers.addAll(DEFAULT_STREAM_SERVERS);
    }

    public synchronized int getServerCount() {
        return activeServers.size();
    }
}
