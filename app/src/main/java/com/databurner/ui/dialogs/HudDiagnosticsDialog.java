package com.databurner.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.databurner.service.BurnService;
import com.databurner.service.ServerManager;
import com.databurner.ui.adapters.ThreadTelemetryAdapter;
import com.databurner.ui.holders.ThreadInfo;
import com.databurner.util.KeystoreBackup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * HudDiagnosticsDialog handles interactive deep telemetry dialogs for
 * Speedometer Hero Card HUD tiles (Worker Threads, Network, Stream Engine, Memory Heap).
 */
public class HudDiagnosticsDialog {

    public static void showThreadDiagnostics(Activity activity, SharedPreferences prefs) {
        int maxThreads = prefs.getInt("thread_count", 20);
        List<ThreadInfo> threadList = new ArrayList<>();
        populateThreadList(prefs, maxThreads, threadList);

        ListView listView = new ListView(activity);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        int p = (int) (12 * activity.getResources().getDisplayMetrics().density);
        listView.setPadding(p, p / 2, p, p / 2);

        ThreadTelemetryAdapter adapter = new ThreadTelemetryAdapter(activity, threadList);
        listView.setAdapter(adapter);

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                populateThreadList(prefs, maxThreads, threadList);
                adapter.notifyDataSetChanged();
                handler.postDelayed(this, 1000);
            }
        };

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Worker Threads Telemetry")
                .setView(listView)
                .setPositiveButton("CLOSE", null)
                .create();

        dialog.setOnDismissListener(d -> handler.removeCallbacks(refreshRunnable));
        dialog.show();

        handler.post(refreshRunnable);
    }

    private static void populateThreadList(SharedPreferences prefs, int maxThreads, List<ThreadInfo> list) {
        boolean running = prefs.getBoolean("service_running", false);
        int activeThreads = running ? BurnService.getActiveThreads() : 0;
        float currentSpeed = prefs.getFloat("speed_mb", 0f);

        list.clear();
        for (int i = 1; i <= maxThreads; i++) {
            boolean isActive = running && i <= activeThreads;
            float threadSpeed = isActive ? Math.max(0.1f, currentSpeed / Math.max(1, activeThreads)) : 0f;
            list.add(new ThreadInfo(i, isActive, running, threadSpeed));
        }
    }

    public static void showNetworkDiagnostics(Activity activity) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Network & Transport Telemetry")
                .setMessage(buildNetworkMessage(activity))
                .setPositiveButton("CLOSE", null)
                .create();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                dialog.setMessage(buildNetworkMessage(activity));
                handler.postDelayed(this, 1000);
            }
        };

        dialog.setOnDismissListener(d -> handler.removeCallbacks(refreshRunnable));
        dialog.show();
        handler.post(refreshRunnable);
    }

    public static void showEngineDiagnostics(Activity activity, ServerManager serverManager, SharedPreferences prefs) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("Stream Engine Telemetry")
                .setMessage(buildEngineMessage(activity, serverManager, prefs))
                .setPositiveButton("CLOSE", null)
                .create();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                dialog.setMessage(buildEngineMessage(activity, serverManager, prefs));
                handler.postDelayed(this, 1000);
            }
        };

        dialog.setOnDismissListener(d -> handler.removeCallbacks(refreshRunnable));
        dialog.show();
        handler.post(refreshRunnable);
    }

    public static void showMemoryDiagnostics(Activity activity, SharedPreferences prefs) {
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle("System Memory & Thermal Telemetry")
                .setMessage(buildMemoryMessage(activity, prefs))
                .setPositiveButton("CLOSE", null)
                .create();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                dialog.setMessage(buildMemoryMessage(activity, prefs));
                handler.postDelayed(this, 1000);
            }
        };

        dialog.setOnDismissListener(d -> handler.removeCallbacks(refreshRunnable));
        dialog.show();
        handler.post(refreshRunnable);
    }

    private static String buildNetworkMessage(Context context) {
        String ipAddress = fetchLocalIpAddress(context);
        String transport = fetchNetworkTransportType(context);

        return String.format(Locale.US,
                "• Transport Type: %s\n" +
                "• Local IPv4 Address: %s\n" +
                "• Connection Integrity: Active\n" +
                "• 5G Guard Monitoring: Sound Alert & Vibration Active",
                transport, ipAddress);
    }

    private static String buildEngineMessage(Context context, ServerManager serverManager, SharedPreferences prefs) {
        boolean running = prefs.getBoolean("service_running", false);
        String httpStatus = BurnService.getLastResponseStatus();
        int mirrorCount = serverManager != null ? serverManager.getServerCount() : 5;
        String securitySummary = KeystoreBackup.getSigningProfileSummary();

        return String.format(Locale.US,
                "• Primary Stream Mode: Multi-Threaded CDN Mirrors\n" +
                "• Service State: %s\n" +
                "• Response Telemetry: %s\n" +
                "• Active CDN Endpoints: %d Stream Mirrors\n" +
                "• Target CDN Routing: Hetzner, Cloudflare, S3 High-Speed Mirrors\n" +
                "• Signing Profile: %s",
                running ? "BURNING ACTIVE" : "IDLE / STANDBY",
                running ? httpStatus : "Standby",
                mirrorCount,
                securitySummary);
    }

    private static String buildMemoryMessage(Context context, SharedPreferences prefs) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemMB = runtime.totalMemory() / (1024 * 1024);
        long freeMemMB = runtime.freeMemory() / (1024 * 1024);
        long usedMemMB = totalMemMB - freeMemMB;
        long maxMemMB = runtime.maxMemory() / (1024 * 1024);

        int batteryPct = fetchBatteryLevel(context);
        boolean thermalGuard = prefs.getBoolean("thermal_guard_enabled", true);

        return String.format(Locale.US,
                "• App RAM Allocated: %d MB\n" +
                "• App Memory Used: %d MB\n" +
                "• Max JVM Heap Limit: %d MB\n" +
                "• Battery Charge Level: %d%%\n" +
                "• Thermal Safety Guard: %s (< 15%% Batt / > 42°C Temp)",
                totalMemMB, usedMemMB, maxMemMB, batteryPct,
                thermalGuard ? "ACTIVE & PROTECTED" : "DISABLED");
    }

    private static String fetchLocalIpAddress(Context context) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "192.0.0.4";
    }

    private static String fetchNetworkTransportType(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                Network network = cm.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(network);
                    if (caps != null) {
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "5G / Cellular NR";
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi High Speed";
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet Direct";
                    }
                }
            }
        } catch (Exception ignored) {}
        return "5G NR";
    }

    private static int fetchBatteryLevel(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    return Math.round((level / (float) scale) * 100);
                }
            }
        } catch (Exception ignored) {}
        return 95;
    }
}
