package com.databurner.ui.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.databurner.util.DebugLogger;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class DebugLogActivity extends AppCompatActivity {

    private TextView logsContainerText, filePathText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_log);

        DashboardController.applyWindowInsets(findViewById(R.id.appBarLayout), R.id.appBarLayout, 0, 0);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        logsContainerText = findViewById(R.id.logsContainerText);
        filePathText = findViewById(R.id.filePathText);
        MaterialButton refreshBtn = findViewById(R.id.refreshBtn);
        MaterialButton clearBtn = findViewById(R.id.clearBtn);

        File downloadFile = DebugLogger.getDownloadFile();
        if (filePathText != null && downloadFile != null) {
            filePathText.setText("Path: " + downloadFile.getAbsolutePath());
        }

        if (refreshBtn != null) {
            refreshBtn.setOnClickListener(v -> loadLogs());
        }

        if (clearBtn != null) {
            clearBtn.setOnClickListener(v -> {
                DebugLogger.clearLogs(this);
                loadLogs();
                Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show();
            });
        }

        loadLogs();
    }

    private void loadLogs() {
        try {
            JSONArray logs = DebugLogger.loadLogArray(this);
            if (logs.length() == 0) {
                logsContainerText.setText("No diagnostic logs recorded yet.\n\nAll background stream events, auto-repairs, and uncaught exceptions will automatically appear here and in /Download/databurner_debug.json");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = logs.length() - 1; i >= 0; i--) { // Reverse order (newest first)
                JSONObject obj = logs.getJSONObject(i);
                sb.append("========================================\n");
                sb.append("Time     : ").append(obj.optString("timestamp", "N/A")).append("\n");
                sb.append("Event    : ").append(obj.optString("event_type", "INFO")).append("\n");
                sb.append("Duration : ").append(formatDuration(obj.optLong("session_seconds", 0))).append("\n");
                sb.append("Data MB  : ").append(obj.optString("total_mb", "0.00")).append(" MB\n");
                sb.append("Threads  : ").append(obj.optInt("active_threads", 0)).append("\n");
                sb.append("Message  : ").append(obj.optString("message", "")).append("\n");

                if (obj.has("error_class")) {
                    sb.append("Error    : ").append(obj.optString("error_class")).append(": ").append(obj.optString("error_message")).append("\n");
                }
                if (obj.has("stack_trace")) {
                    sb.append("Trace    : ").append(obj.optString("stack_trace")).append("\n");
                }
                sb.append("\n");
            }
            logsContainerText.setText(sb.toString());
        } catch (Exception e) {
            logsContainerText.setText("Error reading logs: " + e.getMessage());
        }
    }

    private String formatDuration(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }
}
