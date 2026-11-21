package com.databurner.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.databurner.service.ServerManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MirrorDiagnosticsActivity extends AppCompatActivity {

    private final ServerManager serverManager = new ServerManager();
    private TextView mirrorsContainerText, summaryText;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror_diagnostics);

        DashboardController.applyWindowInsets(findViewById(R.id.mirrorMain), R.id.appBarLayout, 0, R.id.mirrorScrollView);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        if (topAppBar != null) {
            topAppBar.setNavigationOnClickListener(v -> finish());
        }

        mirrorsContainerText = findViewById(R.id.mirrorsContainerText);
        summaryText = findViewById(R.id.summaryText);
        MaterialButton reExtractBtn = findViewById(R.id.reExtractBtn);

        if (reExtractBtn != null) {
            reExtractBtn.setOnClickListener(v -> {
                serverManager.fetchAndValidateDynamicMirrorsAsync();
                Toast.makeText(this, "Started Fresh Web Scrape Pass", Toast.LENGTH_SHORT).show();
                handler.postDelayed(this::loadMirrors, 2000);
            });
        }

        loadMirrors();
    }

    private void loadMirrors() {
        try {
            List<ServerManager.MirrorStatusInfo> list = serverManager.getSessionMirrorStatusList();
            if (summaryText != null) {
                summaryText.setText("Active Current Session Mirrors: " + list.size());
            }

            if (list.isEmpty()) {
                mirrorsContainerText.setText("No active session mirrors available.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            int count = 1;
            for (ServerManager.MirrorStatusInfo info : list) {
                String badge = info.isBlacklisted ? "🚫 [FAILED/BLACKLISTED]" : "✅ [ACTIVE 200 OK]";
                sb.append("────────────────────────────────────────\n");
                sb.append("#").append(count++).append(" ").append(badge).append("\n");
                sb.append("Source    : ").append(info.source).append("\n");
                sb.append("Time      : ").append(info.timestamp12Hr).append("\n");
                sb.append("Failures  : ").append(info.failureCount).append("\n");
                sb.append("URL       : ").append(info.url).append("\n\n");
            }
            mirrorsContainerText.setText(sb.toString());
        } catch (Exception e) {
            mirrorsContainerText.setText("Error loading mirror statuses: " + e.getMessage());
        }
    }
}
