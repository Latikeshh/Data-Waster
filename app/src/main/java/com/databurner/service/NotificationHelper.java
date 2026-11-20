package com.databurner.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Locale;

public class NotificationHelper {

    private static final String CHANNEL_ID = "burn_channel";
    private static final String ALERT_CHANNEL_ID = "burn_error_alert_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final int ALERT_NOTIFICATION_ID = 200;

    private final Context context;
    private final NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Data Burner Service", NotificationManager.IMPORTANCE_LOW);
            NotificationChannel alertChannel = new NotificationChannel(
                    ALERT_CHANNEL_ID, "Data Burner Alerts & Errors", NotificationManager.IMPORTANCE_HIGH);
            alertChannel.setDescription("High-priority status bar alerts for background stream errors and auto-repairs");
            alertChannel.enableVibration(true);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                notificationManager.createNotificationChannel(alertChannel);
            }
        }
    }

    public void sendErrorAlertNotification(String title, String message) {
        try {
            NotificationCompat.Builder alertBuilder = new NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentTitle(title != null ? title : "⚠️ Data Burner Alert")
                    .setContentText(message != null ? message : "Background stream alert triggered.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);

            if (notificationManager != null) {
                notificationManager.notify(ALERT_NOTIFICATION_ID, alertBuilder.build());
            }
        } catch (Exception ignored) {}
    }

    public NotificationCompat.Builder getBaseBuilder() {
        return getBaseBuilder("5G");
    }

    public NotificationCompat.Builder getBaseBuilder(String initialBadge) {
        if (notificationBuilder == null) {
            String badge = (initialBadge != null && !initialBadge.isEmpty()) ? initialBadge : "5G";
            int color = "5G".equals(badge) ? Color.parseColor("#34D399") : Color.parseColor("#A8C7FF");
            Bitmap iconBitmap = makeNetworkBadgeBitmap(badge, color);

            notificationBuilder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(IconCompat.createWithBitmap(iconBitmap))
                    .setContentTitle("Burner Service")
                    .setContentText("Initializing...")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .addAction(android.R.drawable.ic_media_pause, "STOP", getStopPendingIntent());
        }
        return notificationBuilder;
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(context, BurnService.class);
        stopIntent.setAction(BurnService.ACTION_STOP_SERVICE);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(context, 0, stopIntent, flags);
    }

    public void updateNotification(boolean isBurning, boolean isGuard, String status, float speedMb, String time, long totalBytes, String networkBadge) {
        double gb = totalBytes / (1024.0 * 1024.0 * 1024.0);
        String title;
        String content;

        if (status != null && status.startsWith("Paused")) {
            title = status;
            content = isBurning ? String.format(Locale.US, "Used: %.2f GB • %s", gb, time) : "Monitoring Network...";
        } else if (isBurning && isGuard) {
            title = String.format(Locale.US, "Burning + Guard • %.2f MB/s", speedMb);
            content = String.format(Locale.US, "Used: %.2f GB • %s", gb, time);
        } else if (isBurning) {
            title = String.format(Locale.US, "Burning Active • %.2f MB/s", speedMb);
            content = String.format(Locale.US, "Used: %.2f GB • %s", gb, time);
        } else if (isGuard) {
            title = "5G Guard Active (" + networkBadge + ")";
            content = "Status: " + (status != null ? status : "Monitoring");
        } else {
            title = "Data Burner Service";
            content = "Network: " + networkBadge;
        }

        Bitmap iconBitmap;
        if (isBurning && gb >= 0.1) {
            iconBitmap = makeTwoTextBitmap(String.format(Locale.US, "%.1f", gb), "GB");
        } else {
            String badge = (networkBadge != null && !networkBadge.isEmpty()) ? networkBadge : "5G";
            int color = "5G".equals(badge) ? Color.parseColor("#34D399") : Color.parseColor("#A8C7FF");
            iconBitmap = makeNetworkBadgeBitmap(badge, color);
        }

        getBaseBuilder(networkBadge).setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(IconCompat.createWithBitmap(iconBitmap));

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private Bitmap makeTwoTextBitmap(String bigText, String smallText) {
        final int width = 128;
        final int height = 128;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT);

        Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
        big.setColor(Color.parseColor("#A8C7FF"));
        big.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        big.setTextAlign(Paint.Align.CENTER);
        big.setTextSize(86);
        big.setStyle(Paint.Style.FILL);

        Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
        small.setColor(Color.WHITE);
        small.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        small.setTextAlign(Paint.Align.CENTER);
        small.setTextSize(36);
        small.setStyle(Paint.Style.FILL);

        float centerX = width / 2f;
        canvas.drawText(bigText, centerX, 80, big);
        canvas.drawText(smallText, centerX, 118, small);

        return bmp;
    }

    public Bitmap makeNetworkBadgeBitmap(String badgeText, int color) {
        final int width = 128;
        final int height = 128;
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(badgeText.length() > 2 ? 64 : 82);
        paint.setStyle(Paint.Style.FILL);

        float centerX = width / 2f;
        float centerY = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f);
        canvas.drawText(badgeText, centerX, centerY, paint);

        return bmp;
    }
}
