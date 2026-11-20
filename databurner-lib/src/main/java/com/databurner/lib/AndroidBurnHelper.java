package com.databurner.lib;

import java.lang.reflect.Method;

/**
 * Android helper utility for databurner-lib.
 * Manages CPU WakeLocks, WiFi Locks, and Battery Optimization permissions seamlessly on Android devices
 * without adding hard compile dependencies.
 */
public class AndroidBurnHelper {

    private static Object wakeLock;
    private static Object wifiLock;

    /**
     * Acquires CPU PARTIAL_WAKE_LOCK and High-Perf WiFi Lock on Android.
     * @param contextObj Android Context object (Activity or Service)
     * @return true if locks were acquired successfully
     */
    public static synchronized boolean acquireLocks(Object contextObj) {
        if (contextObj == null) return false;
        try {
            Class<?> contextClass = Class.forName("android.content.Context");
            Method getSystemService = contextClass.getMethod("getSystemService", String.class);
            Method getApplicationContext = contextClass.getMethod("getApplicationContext");

            // PowerManager.WakeLock
            String powerService = (String) contextClass.getField("POWER_SERVICE").get(null);
            Object powerManager = getSystemService.invoke(contextObj, powerService);
            if (powerManager != null) {
                Class<?> pmClass = Class.forName("android.os.PowerManager");
                int partialWakeLock = pmClass.getField("PARTIAL_WAKE_LOCK").getInt(null);
                Method newWakeLock = pmClass.getMethod("newWakeLock", int.class, String.class);
                wakeLock = newWakeLock.invoke(powerManager, partialWakeLock, "DataBurnerLib:WakeLock");
                if (wakeLock != null) {
                    Method acquire = wakeLock.getClass().getMethod("acquire");
                    acquire.invoke(wakeLock);
                }
            }

            // WifiManager.WifiLock
            Object appCtx = getApplicationContext.invoke(contextObj);
            String wifiService = (String) contextClass.getField("WIFI_SERVICE").get(null);
            Object wifiManager = getSystemService.invoke(appCtx, wifiService);
            if (wifiManager != null) {
                Class<?> wmClass = Class.forName("android.net.wifi.WifiManager");
                int modeHighPerf = wmClass.getField("WIFI_MODE_FULL_HIGH_PERF").getInt(null);
                Method createWifiLock = wmClass.getMethod("createWifiLock", int.class, String.class);
                wifiLock = createWifiLock.invoke(wifiManager, modeHighPerf, "DataBurnerLib:WifiLock");
                if (wifiLock != null) {
                    Method acquire = wifiLock.getClass().getMethod("acquire");
                    acquire.invoke(wifiLock);
                }
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Releases any active CPU WakeLock or WiFi Lock.
     */
    public static synchronized void releaseLocks() {
        try {
            if (wakeLock != null) {
                Method isHeld = wakeLock.getClass().getMethod("isHeld");
                boolean held = (boolean) isHeld.invoke(wakeLock);
                if (held) {
                    Method release = wakeLock.getClass().getMethod("release");
                    release.invoke(wakeLock);
                }
                wakeLock = null;
            }
        } catch (Exception ignored) {}
        try {
            if (wifiLock != null) {
                Method isHeld = wifiLock.getClass().getMethod("isHeld");
                boolean held = (boolean) isHeld.invoke(wifiLock);
                if (held) {
                    Method release = wifiLock.getClass().getMethod("release");
                    release.invoke(wifiLock);
                }
                wifiLock = null;
            }
        } catch (Exception ignored) {}
    }

    /**
     * Checks if app is ignoring battery optimizations (Unrestricted Battery Usage).
     * @param contextObj Android Context object
     * @return true if battery optimizations are ignored
     */
    public static boolean isIgnoringBatteryOptimizations(Object contextObj) {
        if (contextObj == null) return true;
        try {
            Class<?> contextClass = Class.forName("android.content.Context");
            Method getSystemService = contextClass.getMethod("getSystemService", String.class);
            Method getPackageName = contextClass.getMethod("getPackageName");

            String powerService = (String) contextClass.getField("POWER_SERVICE").get(null);
            Object powerManager = getSystemService.invoke(contextObj, powerService);
            String pkgName = (String) getPackageName.invoke(contextObj);

            if (powerManager != null) {
                Class<?> pmClass = Class.forName("android.os.PowerManager");
                Method isIgnoring = pmClass.getMethod("isIgnoringBatteryOptimizations", String.class);
                return (boolean) isIgnoring.invoke(powerManager, pkgName);
            }
        } catch (Exception ignored) {}
        return true;
    }

    /**
     * Launches system dialog to request Unrestricted Battery Optimization exemption.
     * @param contextObj Android Context object
     */
    public static void requestUnrestrictedBattery(Object contextObj) {
        if (contextObj == null) return;
        try {
            Class<?> contextClass = Class.forName("android.content.Context");
            Method getPackageName = contextClass.getMethod("getPackageName");
            Method startActivity = contextClass.getMethod("startActivity", Class.forName("android.content.Intent"));
            String pkgName = (String) getPackageName.invoke(contextObj);

            Class<?> intentClass = Class.forName("android.content.Intent");
            Class<?> settingsClass = Class.forName("android.provider.Settings");
            String action = (String) settingsClass.getField("ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS").get(null);

            Class<?> uriClass = Class.forName("android.net.Uri");
            Method parseUri = uriClass.getMethod("parse", String.class);
            Object uri = parseUri.invoke(null, "package:" + pkgName);

            Object intent = intentClass.getConstructor(String.class, uriClass).newInstance(action, uri);
            startActivity.invoke(contextObj, intent);
        } catch (Exception ignored) {}
    }
}
