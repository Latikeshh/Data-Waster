package com.databurner.service;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class BurnService extends Service {

    public static final String ACTION_START = "START_BURNING";
    public static final String ACTION_STOP_SERVICE = "STOP_SERVICE";
    public static final String ACTION_RESET_TOTAL = "RESET_TOTAL";
    public static final String ACTION_NEW_SESSION = "NEW_SESSION";

    private static final int THREAD_COUNT = 20;
    private final ServerManager serverManager = new ServerManager();

    private volatile boolean stopRequested = false;
    private volatile boolean streamsRunning = false;
    private final AtomicLong totalBytesThisSec = new AtomicLong(0);
    private final AtomicLong totalBytesSession = new AtomicLong(0);
    private final AtomicLong totalActiveSeconds = new AtomicLong(0);
    private static final java.util.concurrent.atomic.AtomicInteger activeThreads = new java.util.concurrent.atomic.AtomicInteger(0);
    private static volatile String lastResponseStatus = "Idle";

    public static int getActiveThreads() {
        return activeThreads.get();
    }

    public static String getLastResponseStatus() {
        return lastResponseStatus;
    }

    private Handler mainHandler;
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private ExecutorService executorService;
    private boolean isBurningStarted = false;
    private volatile boolean isSirenActive = false;
    private Ringtone alertRingtone;
    private SoundPool soundPool;
    private int soundId = -1;
    private boolean isSoundLoaded = false;
    private NotificationHelper notificationHelper;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override
    public void onCreate() {
        super.onCreate();
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "35");
        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("burn_stats", MODE_PRIVATE);
        
        // Restore session duration from previous run
        totalActiveSeconds.set(prefs.getLong("total_seconds", 0L));
        
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        notificationHelper = new NotificationHelper(this);

        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DataBurner:ServiceWakeLock");
                wakeLock.acquire();
            }
        } catch (Exception ignored) {}

        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DataBurner:ServiceWifiLock");
                wifiLock.acquire();
            }
        } catch (Exception ignored) {}

        initSoundPool();
        startEngine();
    }

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) isSoundLoaded = true;
        });

        try {
            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(notificationUri, "r");
            if (afd != null) {
                soundId = soundPool.load(afd, 1);
            }
        } catch (Exception ignored) {}
    }

    private void startEngine() {
        if (executorService != null) return;
        int threadCount = prefs.getInt("thread_count", 20);
        executorService = Executors.newFixedThreadPool(threadCount + 2); 
        startSpeedMonitor();
        startStreamingSupervisor();
        startPersistenceWorker();
    }

    private boolean isThermalOrBatteryLimitExceeded() {
        boolean thermalGuardEnabled = prefs.getBoolean("thermal_guard_enabled", true);
        if (!thermalGuardEnabled) return false;

        try {
            Intent batteryIntent = registerReceiver(null, new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent != null) {
                int level = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = (level >= 0 && scale > 0) ? (level / (float) scale) * 100f : 100f;

                int tempTenths = batteryIntent.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                float tempCelsius = tempTenths / 10.0f;

                int minBattery = prefs.getInt("min_battery_pct", 15);
                float maxTemp = prefs.getFloat("max_temp_celsius", 42.0f);

                if (batteryPct < minBattery || tempCelsius > maxTemp) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private void startBurning() {
        if (isBurningStarted) return;
        isBurningStarted = true;
        prefs.edit().putBoolean("service_running", true).apply();
        
        // Immediate trigger attempt
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            if (is5GConnected() && !streamsRunning) {
                startStreams();
            }
        }
    }

    private void startStreamingSupervisor() {
        new Thread(() -> {
            while (!stopRequested) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        boolean on5G = is5GConnected();
                        boolean guardEnabled = prefs.getBoolean("guard_enabled", false);
                        boolean hotspotEnabled = isHotspotEnabled();

                        boolean thermalOrBatteryExceeded = isThermalOrBatteryLimitExceeded();

                        // 1. Siren Logic (Guard ON + 5G Loss OR Thermal/Battery Exceeded)
                        if ((guardEnabled && !on5G) || thermalOrBatteryExceeded) {
                            startSirenLoop();
                        } else {
                            isSirenActive = false;
                        }

                        // 2. Hotspot Vibration Logic
                        if (hotspotEnabled && !on5G) {
                            startContinuousVibration();
                        } else {
                            stopContinuousVibration();
                        }

                        // 3. Burner Stream Logic
                        if (thermalOrBatteryExceeded) {
                            broadcastStatus("PAUSED — THERMAL / BATTERY GUARD");
                            stopStreams();
                        } else if (!on5G) {
                            broadcastStatus("Paused — No 5G");
                            stopStreams();
                        } else {
                            broadcastStatus("Running on 5G");
                            if (!streamsRunning && isBurningStarted) {
                                startStreams();
                            }
                        }
                    } else {
                        broadcastStatus("Paused — No Permission");
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }, "network-guard-thread").start();
    }

    private void startSirenLoop() {
        if (isSirenActive) return;
        isSirenActive = true;
        
        new Thread(() -> {
            while (isSirenActive && !stopRequested) {
                playShortAlert();
                
                // Rapid vibration pulse (override for silent mode)
                if (vibrator != null) {
                    vibrator.vibrate(300);
                }
                
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
            }
        }, "siren-thread").start();
    }

    private void playShortAlert() {
        try {
            if (isSoundLoaded && soundId != -1) {
                // Priority 1: Use SoundPool for reliable repeating alerts on Android 16+
                soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
            } else {
                // Fallback to legacy Ringtone if SoundPool isn't ready
                if (alertRingtone == null) {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    alertRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                }
                if (alertRingtone != null) {
                    alertRingtone.play();
                }
            }
        } catch (Exception ignored) {}
    }

    private void startStreams() {
        streamsRunning = true;
        int threadCount = prefs.getInt("thread_count", 20);
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(this::runStreamLoop);
        }
    }

    private void stopStreams() {
        streamsRunning = false;
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

    private void startSpeedMonitor() {
        executorService.execute(() -> {
            while (!stopRequested) {
                try {
                    Thread.sleep(1000);
                    
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        if (is5GConnected() && isBurningStarted) {
                            totalActiveSeconds.incrementAndGet();
                        }
                    }

                    long bytes = totalBytesThisSec.getAndSet(0);
                    float mb = bytes / (1024f * 1024f);
                    
                    long elapsedSec = totalActiveSeconds.get();
                    String time = String.format("%02d:%02d:%02d", elapsedSec / 3600, (elapsedSec % 3600) / 60, elapsedSec % 60);

                    // Peak speed tracking
                    float currentPeak = prefs.getFloat("peak_speed", 0f);
                    if (mb > currentPeak) {
                        currentPeak = mb;
                    }

                    // Average speed tracking
                    long totalUsedBytes = prefs.getLong("used_bytes", 0L);
                    double totalUsedMB = totalUsedBytes / (1024.0 * 1024.0);
                    float avgSpeed = elapsedSec > 0 ? (float) (totalUsedMB / elapsedSec) : 0f;

                    // Quota Target Check
                    long quotaMB = prefs.getLong("quota_mb", 0L);
                    if (quotaMB > 0 && totalUsedMB >= quotaMB && isBurningStarted) {
                        broadcastStatus("Quota Reached — Stopped");
                        playShortAlert();
                        mainHandler.post(this::stopSelf);
                    }

                    prefs.edit()
                        .putFloat("speed_mb", mb)
                        .putFloat("peak_speed", currentPeak)
                        .putFloat("avg_speed", avgSpeed)
                        .putString("session_time", time)
                        .putLong("total_seconds", elapsedSec)
                        .apply();

                    mainHandler.post(this::updateNotificationNow);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private void startPersistenceWorker() {
        executorService.execute(() -> {
            while (!stopRequested) {
                try {
                    Thread.sleep(2000);
                    long sessionBytes = totalBytesSession.getAndSet(0);
                    long currentTotal = prefs.getLong("used_bytes", 0L);
                    prefs.edit().putLong("used_bytes", currentTotal + sessionBytes).apply();
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private void updateNotificationNow() {
        long totalBytes = prefs.getLong("used_bytes", 0L);
        float speedMb = prefs.getFloat("speed_mb", 0f);
        String time = prefs.getString("session_time", "00:00:00");
        boolean guardEnabled = prefs.getBoolean("guard_enabled", false);
        String serviceStatus = prefs.getString("service_status", "System Ready");
        String badge = getNetworkBadge();

        notificationHelper.updateNotification(
                isBurningStarted,
                guardEnabled,
                serviceStatus,
                speedMb,
                time,
                totalBytes,
                badge
        );
    }

    public String getNetworkBadge() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm == null) return "NO";
            Network active = cm.getActiveNetwork();
            if (active == null) return "NO";
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null) return "NO";

            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return "WF";
            }
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    if (is5GConnected()) {
                        return "5G";
                    }
                    TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
                    if (tm != null) {
                        int type = tm.getDataNetworkType();
                        if (type == TelephonyManager.NETWORK_TYPE_LTE) {
                            return "4G";
                        } else if (type == TelephonyManager.NETWORK_TYPE_UMTS || type == TelephonyManager.NETWORK_TYPE_HSDPA || type == TelephonyManager.NETWORK_TYPE_HSPA) {
                            return "3G";
                        }
                    }
                }
                return "4G";
            }
        } catch (Exception ignored) {}
        return "NO";
    }

    private void broadcastStatus(String status) {
        prefs.edit().putString("service_status", status).apply();
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private boolean is5GConnected() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            if (active == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(active);
            if (caps == null) return false;
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return false;
            TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (tm == null) return false;
            int type = tm.getDataNetworkType();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return type == TelephonyManager.NETWORK_TYPE_NR;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private boolean isHotspotEnabled() {
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            java.lang.reflect.Method method = wm.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (boolean) method.invoke(wm);
        } catch (Exception e) { return false; }
    }

    private volatile boolean isVibrating = false;
    private void startContinuousVibration() {
        if (vibrator == null || isVibrating) return;
        isVibrating = true;
        new Thread(() -> {
            while (isVibrating && !stopRequested) {
                vibrator.vibrate(500);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            isVibrating = false;
        }, "vibration-thread").start();
    }

    private void stopContinuousVibration() {
        isVibrating = false;
        if (vibrator != null) vibrator.cancel();
    }

    @Override
    public void onDestroy() {
        stopRequested = true;
        streamsRunning = false;
        isSirenActive = false;
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (isBurningStarted) {
            long sessionBytes = totalBytesSession.get();
            prefs.edit()
                .putLong("used_bytes", prefs.getLong("used_bytes", 0L) + sessionBytes)
                .apply();
        }
        isBurningStarted = false;
        prefs.edit().putBoolean("service_running", false).apply();
        if (executorService != null) executorService.shutdownNow();
        stopContinuousVibration();

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception ignored) {}
        try {
            if (wifiLock != null && wifiLock.isHeld()) {
                wifiLock.release();
            }
        } catch (Exception ignored) {}

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String badge = getNetworkBadge();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationHelper.getBaseBuilder(badge).build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notificationHelper.getBaseBuilder(badge).build());
        }

        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                // Smart Start: Reset session and total data before burning
                totalBytesSession.set(0);
                totalActiveSeconds.set(0);
                prefs.edit()
                    .putLong("used_bytes", 0L)
                    .putLong("total_seconds", 0L)
                    .putFloat("peak_speed", 0f)
                    .putFloat("avg_speed", 0f)
                    .apply();
                updateNotificationNow();
                startBurning();
            } else if (ACTION_STOP_SERVICE.equals(action)) {
                stopSelf();
            } else if (ACTION_RESET_TOTAL.equals(action)) {
                totalBytesSession.set(0);
                totalActiveSeconds.set(0);
                prefs.edit()
                    .putLong("used_bytes", 0L)
                    .putLong("total_seconds", 0L)
                    .putFloat("peak_speed", 0f)
                    .putFloat("avg_speed", 0f)
                    .apply();
                updateNotificationNow();
            } else if (ACTION_NEW_SESSION.equals(action)) {
                totalActiveSeconds.set(0);
                totalBytesSession.set(0);
                prefs.edit().putLong("total_seconds", 0L).apply();
                updateNotificationNow();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
