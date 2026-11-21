package com.databurner.util;

import android.content.Context;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Diagnostic & Exception Logger for Data Burner.
 * Records JSON logs to Downloads folder (databurner_debug.json) and internal app storage.
 * Designed with fallback merge retention so logs survive app reinstalls and UID resets.
 */
public class DebugLogger {

    private static final String FILE_NAME = "databurner_debug.json";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static synchronized void logEvent(Context context, String eventType, String message, long sessionSeconds, long totalBytesBurned, int activeThreads) {
        logFull(context, eventType, message, null, sessionSeconds, totalBytesBurned, activeThreads);
    }

    public static synchronized void logException(Context context, String eventType, String message, Throwable throwable, long sessionSeconds, long totalBytesBurned, int activeThreads) {
        logFull(context, eventType, message, throwable, sessionSeconds, totalBytesBurned, activeThreads);
    }

    private static void logFull(Context context, String eventType, String message, Throwable throwable, long sessionSeconds, long totalBytesBurned, int activeThreads) {
        try {
            JSONArray logs = loadLogArray(context);

            JSONObject entry = new JSONObject();
            entry.put("timestamp", DATE_FORMAT.format(new Date()));
            entry.put("event_type", eventType != null ? eventType : "INFO");
            entry.put("message", message != null ? message : "");
            entry.put("session_seconds", sessionSeconds);
            entry.put("total_mb", String.format(Locale.US, "%.2f", totalBytesBurned / (1024.0 * 1024.0)));
            entry.put("active_threads", activeThreads);

            if (throwable != null) {
                entry.put("error_class", throwable.getClass().getName());
                entry.put("error_message", throwable.getMessage() != null ? throwable.getMessage() : "");
                StringBuilder sb = new StringBuilder();
                for (StackTraceElement ste : throwable.getStackTrace()) {
                    sb.append(ste.toString()).append("\n");
                    if (sb.length() > 500) break; // Limit stack trace size
                }
                entry.put("stack_trace", sb.toString());
            }

            logs.put(entry);

            // Keep max 200 entries to prevent memory ballooning
            while (logs.length() > 200) {
                logs.remove(0);
            }

            saveLogArray(context, logs);
        } catch (Exception ignored) {}
    }

    public static synchronized JSONArray loadLogArray(Context context) {
        JSONArray logs = new JSONArray();
        try {
            File internalFile = getInternalFile(context);
            if (internalFile != null && internalFile.exists()) {
                logs = readFileToJsonArray(internalFile);
            }

            File downloadFile = getDownloadFile();
            if (downloadFile != null && downloadFile.exists() && downloadFile.canRead()) {
                JSONArray downloadLogs = readFileToJsonArray(downloadFile);
                if (downloadLogs.length() > logs.length()) {
                    logs = downloadLogs;
                }
            }
        } catch (Exception ignored) {}
        return logs;
    }

    private static JSONArray readFileToJsonArray(File file) {
        try {
            if (file != null && file.exists() && file.length() > 0) {
                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                int read = fis.read(data);
                fis.close();
                if (read > 0) {
                    return new JSONArray(new String(data, 0, read, StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {}
        return new JSONArray();
    }

    private static void saveLogArray(Context context, JSONArray logs) {
        String str;
        try {
            str = logs.toString(2);
        } catch (Exception e) {
            str = logs.toString();
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);

        // Save to Internal App Storage
        try {
            File internalFile = getInternalFile(context);
            if (internalFile != null) {
                FileOutputStream fos = new FileOutputStream(internalFile);
                fos.write(bytes);
                fos.close();
            }
        } catch (Exception ignored) {}

        // Save to Public Downloads Folder (/storage/emulated/0/Download/databurner_debug.json)
        try {
            File downloadFile = getDownloadFile();
            if (downloadFile != null) {
                FileOutputStream fos = new FileOutputStream(downloadFile);
                fos.write(bytes);
                fos.close();
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void clearLogs(Context context) {
        try {
            File internalFile = getInternalFile(context);
            if (internalFile != null && internalFile.exists()) internalFile.delete();

            File downloadFile = getDownloadFile();
            if (downloadFile != null && downloadFile.exists()) downloadFile.delete();
        } catch (Exception ignored) {}
    }

    public static File getDownloadFile() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (downloadsDir != null) {
                if (!downloadsDir.exists()) downloadsDir.mkdirs();
                return new File(downloadsDir, FILE_NAME);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static File getInternalFile(Context context) {
        if (context == null) return null;
        return new File(context.getFilesDir(), FILE_NAME);
    }
}
