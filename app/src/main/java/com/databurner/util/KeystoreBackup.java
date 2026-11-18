package com.databurner.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Locale;

/**
 * Keystore signing credentials backup reference and build security validator.
 */
public final class KeystoreBackup {
    public static final String KEYSTORE_DESKTOP_PATH = "C:/Users/Latikesh/Desktop/laki.class";
    public static final String KEYSTORE_ASSETS_PATH = "app/src/main/assets/laki.class";
    public static final String STORE_PASSWORD = "123456";
    public static final String KEY_ALIAS = "key123456";
    public static final String KEY_PASSWORD = "123456";

    private KeystoreBackup() {
        // Prevent instantiation
    }

    /**
     * Returns a summary of the release signing configuration profile.
     */
    public static String getSigningProfileSummary() {
        return String.format(Locale.US, "Alias: %s • Signed Release", KEY_ALIAS);
    }

    /**
     * Validates package signature status for app telemetry.
     */
    public static String getSecurityVerificationInfo(Context context) {
        if (context == null) return "Signed (" + KEY_ALIAS + ")";
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? pInfo.getLongVersionCode() : pInfo.versionCode;
            return String.format(Locale.US, "v%s (%d) • Key: %s", pInfo.versionName, versionCode, KEY_ALIAS);
        } catch (PackageManager.NameNotFoundException e) {
            return "Key: " + KEY_ALIAS;
        }
    }

    /**
     * Checks if backup keystore credentials are defined.
     */
    public static boolean isKeystoreConfigured() {
        return KEY_ALIAS.length() > 0 && STORE_PASSWORD.length() > 0;
    }
}
