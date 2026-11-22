package com.databurner.ui.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.databurner.service.MirrorStatusInfo;
import com.databurner.service.ServerManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MirrorDiagnosticsActivity extends AppCompatActivity {

    private final ServerManager serverManager = ServerManager.getInstance();
    private TextView mirrorsContainerText, summaryText;
    private MaterialButton tabExtractedBtn, tabPredefinedBtn;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable autoRefreshRunnable;
    private boolean showDynamicExtractedTab = false;

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
        tabExtractedBtn = findViewById(R.id.tabExtractedBtn);
        tabPredefinedBtn = findViewById(R.id.tabPredefinedBtn);
        MaterialButton reExtractBtn = findViewById(R.id.reExtractBtn);

        if (tabExtractedBtn != null) {
            tabExtractedBtn.setOnClickListener(v -> {
                showDynamicExtractedTab = true;
                updateTabStyles();
                loadMirrors();
            });
        }

        if (tabPredefinedBtn != null) {
            tabPredefinedBtn.setOnClickListener(v -> {
                showDynamicExtractedTab = false;
                updateTabStyles();
                loadMirrors();
            });
        }

        if (reExtractBtn != null) {
            reExtractBtn.setOnClickListener(v -> {
                serverManager.fetchAndValidateDynamicMirrorsAsync();
                serverManager.startWebViewExtraction(this);
                Toast.makeText(this, "Started Fresh Web Scrape & WebView Interceptor Pass", Toast.LENGTH_SHORT).show();
                handler.postDelayed(this::loadMirrors, 2000);
            });
        }

        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadMirrors();
                handler.postDelayed(this, 1000); // Live 1-second auto-refresh
            }
        };

        updateTabStyles();
        loadMirrors();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(autoRefreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(autoRefreshRunnable);
    }

    private void updateTabStyles() {
        if (tabExtractedBtn != null && tabPredefinedBtn != null) {
            if (showDynamicExtractedTab) {
                tabExtractedBtn.setTextColor(getColor(R.color.primary_m3));
                tabPredefinedBtn.setTextColor(getColor(R.color.on_surface_variant));
            } else {
                tabExtractedBtn.setTextColor(getColor(R.color.on_surface_variant));
                tabPredefinedBtn.setTextColor(getColor(R.color.primary_m3));
            }
        }
    }

    private void loadMirrors() {
        try {
            List<MirrorStatusInfo> allList = serverManager.getSessionMirrorStatusList();
            List<MirrorStatusInfo> filteredList = new ArrayList<>();

            for (MirrorStatusInfo info : allList) {
                boolean isPredefined = "PREDEFINED_CDN".equalsIgnoreCase(info.source);
                if (showDynamicExtractedTab && !isPredefined) {
                    filteredList.add(info);
                } else if (!showDynamicExtractedTab && isPredefined) {
                    filteredList.add(info);
                }
            }

            if (summaryText != null) {
                String category = showDynamicExtractedTab ? "Dynamic Extracted" : "Predefined CDN";
                summaryText.setText(String.format(Locale.US, "Current Session %s Mirrors: %d", category, filteredList.size()));
            }

            if (filteredList.isEmpty()) {
                mirrorsContainerText.setText(showDynamicExtractedTab ?
                        "No dynamic extracted mirrors active in this session yet.\n\nTap 'Fresh Web Scrape' to fetch live links from Google SDKs, Ubuntu, and GitHub Releases." :
                        "No predefined CDN mirrors loaded.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            int count = 1;
            for (MirrorStatusInfo info : filteredList) {
                String badge;
                if (info.isBlacklisted) {
                    badge = "🚫 [BLACKLISTED / FAILED]";
                } else if (info.activeConnections > 0) {
                    badge = "🔥 [WORKING / STREAMING NOW (" + info.activeConnections + " active thread" + (info.activeConnections > 1 ? "s" : "") + ")]";
                } else if (info.downloadedBytes > 0) {
                    badge = "✅ [IDLE / RECENTLY USED]";
                } else {
                    badge = "⏸️ [IDLE / STANDBY]";
                }

                double mb = info.downloadedBytes / (1024.0 * 1024.0);

                sb.append("────────────────────────────────────────\n");
                sb.append("#").append(count++).append(" ").append(badge).append("\n");
                sb.append("Name       : ").append(info.friendlyName).append("\n");
                sb.append("Test Status: ").append(info.testResultStatus).append("\n");
                sb.append("Active Conn: ").append(info.activeConnections > 0 ? info.activeConnections + " thread(s) downloading" : "0 (Idle)").append("\n");
                sb.append("Data Used  : ").append(String.format(Locale.US, "%.2f MB", mb)).append("\n");
                sb.append("Extracted  : ").append(info.timestamp12Hr).append("\n");
                sb.append("Failures   : ").append(info.failureCount).append("\n");
                sb.append("URL        : ").append(info.url).append("\n\n");
            }
            mirrorsContainerText.setText(sb.toString());
        } catch (Exception e) {
            mirrorsContainerText.setText("Error loading mirror statuses: " + e.getMessage());
        }
    }
}
