package com.databurner.lib;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Android helper utility for databurner-lib.
 * Automatically checks permissions, displays a non-cancelable Material dialog if missing,
 * and manages CPU WakeLocks and WiFi Locks on Android devices seamlessly.
 */
public class AndroidBurnHelper {

    private static Object wakeLock;
    private static Object wifiLock;
    private static Object activeDialog;

    /**
     * Automatically checks mandatory permissions (READ_PHONE_STATE, POST_NOTIFICATIONS, Battery Optimization).
     * If permissions are missing, displays a non-cancelable dialog box directly on the screen.
     * Once permissions are granted, acquires CPU/WiFi locks and invokes onGranted.
     */
    public static void checkPermissionsAndShowDialogIfNeeded(Object contextObj, Runnable onGranted) {
        if (contextObj == null) {
            if (onGranted != null) onGranted.run();
            return;
        }

        try {
            Class<?> contextClass = Class.forName("android.content.Context");
            Class<?> activityClass = Class.forName("android.app.Activity");

            if (!activityClass.isInstance(contextObj)) {
                // Non-Activity context: Acquire locks and proceed
                acquireLocks(contextObj);
                if (onGranted != null) onGranted.run();
                return;
            }

            Class<?> compatClass = Class.forName("androidx.core.content.ContextCompat");
            Method checkSelfPermission = compatClass.getMethod("checkSelfPermission", contextClass, String.class);

            // 1. Check READ_PHONE_STATE
            int phoneStateResult = (int) checkSelfPermission.invoke(null, contextObj, "android.permission.READ_PHONE_STATE");
            boolean needPhoneState = (phoneStateResult != 0);

            // 2. Check POST_NOTIFICATIONS (Android 13+ / API 33)
            boolean needNotification = false;
            Class<?> buildClass = Class.forName("android.os.Build$VERSION");
            int sdkInt = buildClass.getField("SDK_INT").getInt(null);
            if (sdkInt >= 33) {
                int notifResult = (int) checkSelfPermission.invoke(null, contextObj, "android.permission.POST_NOTIFICATIONS");
                needNotification = (notifResult != 0);
            }

            // 3. Check Battery Optimization
            boolean needBatteryOpt = !isIgnoringBatteryOptimizations(contextObj);

            if (needPhoneState || needNotification || needBatteryOpt) {
                showReflectiveMandatoryDialog(contextObj, needPhoneState, needNotification, needBatteryOpt, onGranted);
            } else {
                acquireLocks(contextObj);
                if (onGranted != null) onGranted.run();
            }
        } catch (Exception e) {
            acquireLocks(contextObj);
            if (onGranted != null) onGranted.run();
        }
    }

    private static void showReflectiveMandatoryDialog(Object activityObj, boolean needPhoneState,
                                                      boolean needNotification, boolean needBatteryOpt,
                                                      Runnable onGranted) {
        try {
            if (activeDialog != null) {
                Method isShowing = activeDialog.getClass().getMethod("isShowing");
                if ((boolean) isShowing.invoke(activeDialog)) return;
            }

            StringBuilder msg = new StringBuilder("Data Burner requires the following permissions to function properly:\n\n");
            if (needPhoneState) msg.append("• Phone State: Detect 5G/4G cellular network status.\n");
            if (needNotification) msg.append("• Notifications: Show active speed & status bar notifications.\n");
            if (needBatteryOpt) msg.append("• Unrestricted Battery: Keep streaming data when screen is OFF.\n");
            msg.append("\nPlease tap 'Grant Permissions' to proceed.");

            Class<?> builderClass;
            try {
                builderClass = Class.forName("com.google.android.material.dialog.MaterialAlertDialogBuilder");
            } catch (ClassNotFoundException e) {
                builderClass = Class.forName("android.app.AlertDialog$Builder");
            }

            Class<?> contextClass = Class.forName("android.content.Context");
            Object builder = builderClass.getConstructor(contextClass).newInstance(activityObj);

            Method setTitle = builderClass.getMethod("setTitle", CharSequence.class);
            Method setMessage = builderClass.getMethod("setMessage", CharSequence.class);
            Method setCancelable = builderClass.getMethod("setCancelable", boolean.class);

            setTitle.invoke(builder, "Mandatory Permissions Required");
            setMessage.invoke(builder, msg.toString());
            setCancelable.invoke(builder, false);

            Class<?> listenerClass = Class.forName("android.content.DialogInterface$OnClickListener");
            Object positiveListener = Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if ("onClick".equals(method.getName())) {
                            activeDialog = null;
                            if (needPhoneState || needNotification) {
                                List<String> list = new ArrayList<>();
                                if (needPhoneState) list.add("android.permission.READ_PHONE_STATE");
                                if (needNotification) list.add("android.permission.POST_NOTIFICATIONS");

                                Class<?> compatClass = Class.forName("androidx.core.app.ActivityCompat");
                                Method requestPermissions = compatClass.getMethod("requestPermissions",
                                        Class.forName("android.app.Activity"), String[].class, int.class);
                                requestPermissions.invoke(null, activityObj, list.toArray(new String[0]), 100);
                            } else if (needBatteryOpt) {
                                requestUnrestrictedBattery(activityObj);
                            }
                        }
                        return null;
                    }
            );

            Method setPositiveButton = builderClass.getMethod("setPositiveButton", CharSequence.class, listenerClass);
            setPositiveButton.invoke(builder, "Grant Permissions", positiveListener);

            Method create = builderClass.getMethod("create");
            activeDialog = create.invoke(builder);
            Method show = activeDialog.getClass().getMethod("show");
            show.invoke(activeDialog);
        } catch (Exception e) {
            acquireLocks(activityObj);
            if (onGranted != null) onGranted.run();
        }
    }

    /**
     * Acquires CPU PARTIAL_WAKE_LOCK and High-Perf WiFi Lock on Android.
     */
    public static synchronized boolean acquireLocks(Object contextObj) {
        if (contextObj == null) return false;
        try {
            Class<?> contextClass = Class.forName("android.content.Context");
            Method getSystemService = contextClass.getMethod("getSystemService", String.class);
            Method getApplicationContext = contextClass.getMethod("getApplicationContext");

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
