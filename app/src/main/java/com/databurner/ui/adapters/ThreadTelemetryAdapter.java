package com.databurner.ui.adapters;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.databurner.R;
import com.databurner.ui.holders.ThreadInfo;

import java.util.List;
import java.util.Locale;

public class ThreadTelemetryAdapter extends ArrayAdapter<ThreadInfo> {
    private final Activity activity;

    public ThreadTelemetryAdapter(Activity activity, List<ThreadInfo> threads) {
        super(activity, 0, threads);
        this.activity = activity;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        if (row == null) {
            row = activity.getLayoutInflater().inflate(R.layout.item_thread_telemetry, parent, false);
        }
        ThreadInfo item = getItem(position);
        if (item != null) {
            View statusDot = row.findViewById(R.id.threadStatusDot);
            TextView title = row.findViewById(R.id.threadTitle);
            TextView subtitle = row.findViewById(R.id.threadSubtitle);
            TextView badge = row.findViewById(R.id.threadSpeedBadge);

            if (title != null) title.setText(String.format(Locale.US, "Thread #%02d", item.threadId));

            if (item.isRunning && item.isActive) {
                if (statusDot != null) statusDot.setBackgroundResource(R.drawable.bg_status_dot_active);
                if (subtitle != null) subtitle.setText(String.format(Locale.US, "High-Speed Stream #%d • Active", item.threadId));
                if (badge != null) {
                    badge.setText(item.speedMb > 0 ? String.format(Locale.US, "%.2f MB/s", item.speedMb) : "ACTIVE");
                    badge.setTextColor(activity.getColor(R.color.tertiary_m3));
                    badge.setBackgroundResource(R.drawable.bg_m3_chip_emerald);
                }
            } else if (item.isRunning) {
                if (statusDot != null) statusDot.setBackgroundResource(R.drawable.bg_status_dot_idle);
                if (subtitle != null) subtitle.setText("Standby Worker Pool");
                if (badge != null) {
                    badge.setText("IDLE");
                    badge.setTextColor(activity.getColor(R.color.primary_m3));
                    badge.setBackgroundResource(R.drawable.bg_m3_chip_cyan);
                }
            } else {
                if (statusDot != null) statusDot.setBackgroundResource(R.drawable.bg_status_dot_idle);
                if (subtitle != null) subtitle.setText("Engine Paused");
                if (badge != null) {
                    badge.setText("STANDBY");
                    badge.setTextColor(activity.getColor(R.color.on_surface_variant));
                    badge.setBackgroundResource(R.drawable.bg_stat_card);
                }
            }
        }
        return row;
    }
}
