package com.databurner.ui.activities;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.google.android.material.appbar.MaterialToolbar;

public class AboutActivity extends AppCompatActivity {

    public static final String EXTRA_PAGE_TYPE = "page_type";
    public static final String PAGE_ABOUT = "ABOUT";
    public static final String PAGE_MORE_INFO = "MORE_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Reuse centralized window inset routing
        DashboardController.applyWindowInsets(findViewById(R.id.aboutMain), R.id.aboutAppBarLayout, 0, R.id.aboutScrollView);

        MaterialToolbar toolbar = findViewById(R.id.aboutToolbar);
        TextView titleText = findViewById(R.id.aboutTitleText);
        TextView subtitleText = findViewById(R.id.aboutSubtitleText);
        LinearLayout aboutSectionLayout = findViewById(R.id.aboutSectionLayout);
        LinearLayout techSpecsSectionLayout = findViewById(R.id.techSpecsSectionLayout);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                finish();
            });
        }

        String pageType = getIntent().getStringExtra(EXTRA_PAGE_TYPE);
        if (pageType == null) pageType = PAGE_ABOUT;

        if (PAGE_MORE_INFO.equalsIgnoreCase(pageType)) {
            if (toolbar != null) toolbar.setTitle("Technical Specifications");
            if (titleText != null) titleText.setText("TECH SPECS & MIRRORS");
            if (subtitleText != null) subtitleText.setText("Detailed Stream Protocol & CDN Configuration");
            if (aboutSectionLayout != null) aboutSectionLayout.setVisibility(View.GONE);
            if (techSpecsSectionLayout != null) techSpecsSectionLayout.setVisibility(View.VISIBLE);
        } else {
            if (toolbar != null) toolbar.setTitle("About Data Burner");
            if (titleText != null) titleText.setText("DATA BURNER v1.0");
            if (subtitleText != null) subtitleText.setText("5G Benchmark • " + com.databurner.util.KeystoreBackup.getSecurityVerificationInfo(this));
            if (aboutSectionLayout != null) aboutSectionLayout.setVisibility(View.VISIBLE);
            if (techSpecsSectionLayout != null) techSpecsSectionLayout.setVisibility(View.GONE);
        }
    }
}
