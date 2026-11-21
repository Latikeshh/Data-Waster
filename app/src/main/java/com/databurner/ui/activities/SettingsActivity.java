package com.databurner.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.databurner.service.ServerManager;
import com.databurner.ui.holders.SettingsCardHolder;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private ServerManager serverManager;
    private SettingsCardHolder cardHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        DashboardController.applyWindowInsets(findViewById(R.id.settingsMain), R.id.settingsAppBarLayout, 0, R.id.settingsScrollView);

        prefs = getSharedPreferences("burn_stats", MODE_PRIVATE);
        serverManager = ServerManager.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                finish();
            });
        }

        cardHolder = DashboardController.bindSettingsActivity(this, serverManager, prefs, this::updateSettingsUi);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSettingsUi();
    }

    private void updateSettingsUi() {
        if (cardHolder == null) return;
        int threads = prefs.getInt("thread_count", 20);
        if (cardHolder.threadBtn != null) {
            cardHolder.threadBtn.setText(threads + " THREADS");
        }
        long quotaMB = prefs.getLong("quota_mb", 0L);
        if (cardHolder.quotaBtn != null) {
            cardHolder.quotaBtn.setText(DashboardController.getQuotaLabel(quotaMB));
        }

        View cardThread = findViewById(R.id.cardThreadSetting);
        if (cardThread != null) {
            TextView subtitle = cardThread.findViewById(R.id.featureSubtitle);
            if (subtitle != null) {
                subtitle.setText(threads + " High-Speed Parallel Worker Streams");
            }
        }

        View cardMirror = findViewById(R.id.cardMirrorSetting);
        if (cardMirror != null && serverManager != null) {
            TextView subtitle = cardMirror.findViewById(R.id.featureSubtitle);
            if (subtitle != null) {
                subtitle.setText(serverManager.getServerCount() + " Active CDN Stream Mirror Endpoints");
            }
        }
    }
}
