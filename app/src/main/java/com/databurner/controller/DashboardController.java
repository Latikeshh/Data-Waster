package com.databurner.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.databurner.R;
import com.databurner.service.BurnService;
import com.databurner.service.ServerManager;
import com.databurner.ui.holders.FeatureCardHolder;
import com.databurner.ui.holders.SettingsCardHolder;
import com.databurner.ui.holders.StatCardHolder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

/**
 * DashboardController handles UI card setups, dialogs, 3D flip animations,
 * window insets, and metric formatting for MainActivity & AboutActivity.
 */
public class DashboardController {

    private static final long[] QUOTA_VALUES = {0L, 500L, 1024L, 2048L, 5120L, 10240L};
    private static final String[] QUOTA_OPTIONS = {
            "OFF (Unlimited)", "500 MB", "1 GB", "2 GB", "5 GB", "10 GB", "Custom Amount..."
    };

    public static void applyWindowInsets(View mainView, int appBarId, int bottomButtonId, int extraPaddingId) {
        if (mainView == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            View appBarLayout = mainView.findViewById(appBarId);
            if (appBarLayout != null) {
                appBarLayout.setPadding(0, systemBars.top, 0, 0);
            }
            v.setPadding(systemBars.left, 0, systemBars.right, 0);

            if (bottomButtonId != 0) {
                int bottomMargin = systemBars.bottom + (int) (24 * mainView.getResources().getDisplayMetrics().density);
                View startBtnView = mainView.findViewById(bottomButtonId);
                View stopBtnView = mainView.findViewById(R.id.stopBtn);
                if (startBtnView != null && startBtnView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) startBtnView.getLayoutParams();
                    lp.bottomMargin = bottomMargin;
                    startBtnView.setLayoutParams(lp);
                }
                if (stopBtnView != null && stopBtnView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) stopBtnView.getLayoutParams();
                    lp.bottomMargin = bottomMargin;
                    stopBtnView.setLayoutParams(lp);
                }
            }

            if (extraPaddingId != 0) {
                View extraView = mainView.findViewById(extraPaddingId);
                if (extraView != null) {
                    extraView.setPadding(0, 0, 0, systemBars.bottom);
                }
            }
            return insets;
        });
    }

    public static StatCardHolder setupStatCards(Activity activity, View root, SharedPreferences prefs, Runnable onResetClicked) {
        setupSingleStatCard(root, R.id.cardBurned, "TOTAL BURNED", R.drawable.ic_download);
        setupSingleStatCard(root, R.id.cardTime, "DURATION", R.drawable.ic_clock);
        setupSingleStatCard(root, R.id.cardPeak, "PEAK SPEED", R.drawable.ic_zap);
        setupSingleStatCard(root, R.id.cardAvg, "AVG SPEED", R.drawable.ic_avg);

        StatCardHolder holder = new StatCardHolder();
        holder.usedText = getStatValueText(root, R.id.cardBurned);
        holder.timeText = getStatValueText(root, R.id.cardTime);
        holder.peakText = getStatValueText(root, R.id.cardPeak);
        holder.avgText = getStatValueText(root, R.id.cardAvg);

        ImageButton gridResetBtn = root.findViewById(R.id.gridResetBtn);
        if (gridResetBtn != null) {
            gridResetBtn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                prefs.edit()
                    .putLong("used_bytes", 0L)
                    .putLong("total_seconds", 0L)
                    .putFloat("peak_speed", 0f)
                    .putFloat("avg_speed", 0f)
                    .putString("session_time", "00:00:00")
                    .apply();
                Intent resetIntent = new Intent(activity, BurnService.class);
                resetIntent.setAction(BurnService.ACTION_RESET_TOTAL);
                if (prefs.getBoolean("service_running", false)) {
                    activity.startForegroundService(resetIntent);
                }
                if (onResetClicked != null) onResetClicked.run();
            });
        }

        return holder;
    }

    private static TextView getStatValueText(View root, int cardId) {
        View card = root.findViewById(cardId);
        return card != null ? card.findViewById(R.id.statValue) : null;
    }

    private static void setupSingleStatCard(View root, int cardId, String titleText, int iconRes) {
        View card = root.findViewById(cardId);
        if (card == null) return;
        TextView title = card.findViewById(R.id.statTitle);
        ImageView icon = card.findViewById(R.id.statIcon);

        if (title != null) title.setText(titleText);
        if (icon != null && iconRes != 0) {
            icon.setImageResource(iconRes);
        }
    }

    public static FeatureCardHolder bindFeatureCards(Context context, View root) {
        FeatureCardHolder holder = new FeatureCardHolder();

        // 1. Quota Card Setup (SettingsActivity)
        View cardQuota = root.findViewById(R.id.cardQuotaSetting);
        if (cardQuota != null) {
            holder.quotaBtn = setupSingleFeatureRow(context, cardQuota, R.drawable.ic_quota, R.color.tertiary_m3,
                    "Target Quota Limit", "Auto-stop when limit reached", null, true);
        }

        // 2. 5G Network Guard Card Setup (MainActivity)
        View cardGuard = root.findViewById(R.id.cardGuard);
        if (cardGuard != null) {
            setupSingleFeatureRow(context, cardGuard, R.drawable.ic_shield_5g, R.color.tertiary_m3,
                    "5G Network Guard", "Sound alert on connection drop", "DISABLED", false);
            holder.guardStatusBadge = cardGuard.findViewById(R.id.featureBadge);
            holder.guardSwitch = cardGuard.findViewById(R.id.featureSwitch);
        }

        return holder;
    }

    public static SettingsCardHolder bindSettingsActivity(Activity activity, ServerManager serverManager, SharedPreferences prefs, Runnable onUpdateUi) {
        SettingsCardHolder holder = new SettingsCardHolder();

        // 1. Thread Setting Card
        View cardThread = activity.findViewById(R.id.cardThreadSetting);
        if (cardThread != null) {
            holder.threadBtn = setupSingleFeatureRow(activity, cardThread, R.drawable.ic_zap, R.color.tertiary_m3,
                    "Parallel Concurrency Threads", "Adjust thread count for network stress", null, true);
            if (holder.threadBtn != null) {
                holder.threadBtn.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    showThreadSliderDialog(activity, prefs, onUpdateUi);
                });
            }
        }

        // 3. Mirror Setting Card
        View cardMirror = activity.findViewById(R.id.cardMirrorSetting);
        if (cardMirror != null) {
            MaterialButton mirrorBtn = setupSingleFeatureRow(activity, cardMirror, R.drawable.ic_download, R.color.primary_m3,
                    "Stream Mirror URLs", "Add custom S3, Cloudflare, or test file links", null, true);
            if (mirrorBtn != null) {
                mirrorBtn.setText("MANAGE");
                mirrorBtn.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    showCustomMirrorManagerDialog(activity, serverManager, onUpdateUi);
                });
            }
        }

        // 4. Quota Setting Card
        View cardQuota = activity.findViewById(R.id.cardQuotaSetting);
        if (cardQuota != null) {
            holder.quotaBtn = setupSingleFeatureRow(activity, cardQuota, R.drawable.ic_quota, R.color.tertiary_m3,
                    "Target Data Quota", "Auto-stop when limit reached", null, true);
            if (holder.quotaBtn != null) {
                holder.quotaBtn.setOnClickListener(v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    showQuotaSelectionDialog(activity, prefs, onUpdateUi,
                            () -> showCustomQuotaInputDialog(activity, prefs, onUpdateUi));
                });
            }
        }

        // 5. Thermal Setting Card
        View cardThermal = activity.findViewById(R.id.cardThermalSetting);
        if (cardThermal != null) {
            setupSingleFeatureRow(activity, cardThermal, R.drawable.ic_shield_5g, R.color.primary_m3,
                    "Thermal & Battery Protection", "Auto-pause if battery < 15% or temp > 42°C", null, false);
            holder.thermalSwitch = cardThermal.findViewById(R.id.featureSwitch);
            if (holder.thermalSwitch != null) {
                holder.thermalSwitch.setChecked(prefs.getBoolean("thermal_guard_enabled", true));
                holder.thermalSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                    btn.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    prefs.edit().putBoolean("thermal_guard_enabled", isChecked).apply();
                });
            }
        }

        return holder;
    }

    private static MaterialButton setupSingleFeatureRow(Context context, View card, int iconRes, int tintColorRes,
                                                          String titleText, String subtitleText, String badgeText, boolean isButton) {
        if (card == null) return null;
        ImageView icon = card.findViewById(R.id.featureIcon);
        TextView title = card.findViewById(R.id.featureTitle);
        TextView subtitle = card.findViewById(R.id.featureSubtitle);
        TextView badge = card.findViewById(R.id.featureBadge);
        MaterialButton btn = card.findViewById(R.id.featureBtn);
        MaterialSwitch sw = card.findViewById(R.id.featureSwitch);

        if (icon != null) {
            icon.setImageResource(iconRes);
            icon.setImageTintList(context.getColorStateList(tintColorRes));
        }
        if (title != null) title.setText(titleText);
        if (subtitle != null) subtitle.setText(subtitleText);

        if (badgeText != null && badge != null) {
            badge.setText(badgeText);
            badge.setVisibility(View.VISIBLE);
        }

        if (isButton && btn != null) {
            btn.setVisibility(View.VISIBLE);
            return btn;
        } else if (!isButton && sw != null) {
            sw.setVisibility(View.VISIBLE);
        }
        return null;
    }

    public static void showLogoPreviewDialog(Activity activity) {
        if (activity == null) return;
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_logo_preview, null);
        new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .setPositiveButton("CLOSE", null)
                .show();
    }

    private static void showSingleChoiceDialog(Context context, String title, String[] options, int selectedIndex, OnOptionSelectedListener listener) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    if (listener != null) listener.onSelected(which);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public static void showQuotaSelectionDialog(Context context, SharedPreferences prefs, Runnable onUpdateUi, Runnable onCustomQuotaInput) {
        long currentQuota = prefs.getLong("quota_mb", 0L);
        int selectedIndex = 0;
        for (int i = 0; i < QUOTA_VALUES.length; i++) {
            if (currentQuota == QUOTA_VALUES[i]) {
                selectedIndex = i;
                break;
            } else if (currentQuota > 0L) {
                selectedIndex = 6;
            }
        }

        showSingleChoiceDialog(context, "Target Data Quota", QUOTA_OPTIONS, selectedIndex, which -> {
            if (which >= 0 && which < QUOTA_VALUES.length) {
                prefs.edit().putLong("quota_mb", QUOTA_VALUES[which]).apply();
                if (onUpdateUi != null) onUpdateUi.run();
            } else if (which == 6 && onCustomQuotaInput != null) {
                onCustomQuotaInput.run();
            }
        });
    }

    private static void showCustomInputDialog(Activity activity, String title, int layoutRes, int inputId,
                                                String positiveBtnText, String neutralBtnText,
                                                OnCustomInputSubmittedListener onSubmitted, Runnable onNeutralClicked) {
        View dialogView = activity.getLayoutInflater().inflate(layoutRes, null);
        TextInputEditText input = dialogView.findViewById(inputId);
        RadioButton radioGb = dialogView.findViewById(R.id.radioGb);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(positiveBtnText, (dialog, which) -> {
                    String valStr = input != null && input.getText() != null ? input.getText().toString().trim() : "";
                    if (onSubmitted != null) {
                        onSubmitted.onSubmitted(valStr, radioGb != null && radioGb.isChecked());
                    }
                })
                .setNegativeButton("CANCEL", null);

        if (neutralBtnText != null && onNeutralClicked != null) {
            builder.setNeutralButton(neutralBtnText, (dialog, which) -> onNeutralClicked.run());
        }

        builder.show();
    }

    public static void showCustomQuotaInputDialog(Activity activity, SharedPreferences prefs, Runnable onUpdateUi) {
        showCustomInputDialog(activity, "Set Custom Target", R.layout.dialog_custom_quota, R.id.quotaInput,
                "SET QUOTA", "TURN OFF", (valStr, isGbChecked) -> {
                    if (!valStr.isEmpty()) {
                        try {
                            double val = Double.parseDouble(valStr);
                            if (val <= 0) {
                                Toast.makeText(activity, "Please enter a positive limit", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            long quotaMB = isGbChecked ? Math.round(val * 1024.0) : Math.round(val);
                            prefs.edit().putLong("quota_mb", quotaMB).apply();
                            if (onUpdateUi != null) onUpdateUi.run();
                        } catch (Exception e) {
                            Toast.makeText(activity, "Invalid number entered", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, () -> {
                    prefs.edit().putLong("quota_mb", 0L).apply();
                    if (onUpdateUi != null) onUpdateUi.run();
                });
    }

    public static void showCustomMirrorManagerDialog(Activity activity, ServerManager serverManager, Runnable onUpdated) {
        showCustomInputDialog(activity, "Add Custom Stream Mirror", R.layout.dialog_custom_mirror, R.id.mirrorUrlInput,
                "ADD MIRROR", "RESET DEFAULTS", (url, isGbChecked) -> {
                    if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                        serverManager.addCustomServer(url);
                        Toast.makeText(activity, "Custom mirror added successfully", Toast.LENGTH_SHORT).show();
                        if (onUpdated != null) onUpdated.run();
                    } else {
                        Toast.makeText(activity, "Please enter a valid http/https URL", Toast.LENGTH_SHORT).show();
                    }
                }, () -> {
                    serverManager.resetToDefaults();
                    Toast.makeText(activity, "Mirrors reset to defaults", Toast.LENGTH_SHORT).show();
                    if (onUpdated != null) onUpdated.run();
                });
    }

    public static void showThreadSliderDialog(Activity activity, SharedPreferences prefs, Runnable onUpdated) {
        String[] threadOptions = new String[]{"5 Threads", "10 Threads", "20 Threads (Default)", "35 Threads", "50 Threads (Stress)"};
        int[] threadValues = new int[]{5, 10, 20, 35, 50};
        int currentThreads = prefs.getInt("thread_count", 20);
        int selectedIndex = 2;
        if (currentThreads == 5) selectedIndex = 0;
        else if (currentThreads == 10) selectedIndex = 1;
        else if (currentThreads == 20) selectedIndex = 2;
        else if (currentThreads == 35) selectedIndex = 3;
        else if (currentThreads == 50) selectedIndex = 4;

        showSingleChoiceDialog(activity, "Parallel Concurrency Threads", threadOptions, selectedIndex, which -> {
            if (which >= 0 && which < threadValues.length) {
                prefs.edit().putInt("thread_count", threadValues[which]).apply();
                Toast.makeText(activity, "Set to " + threadValues[which] + " parallel threads", Toast.LENGTH_SHORT).show();
                if (onUpdated != null) onUpdated.run();
            }
        });
    }

    public static String formatData(double mb) {
        if (mb >= 1024) {
            return String.format(Locale.US, "%.2f GB", mb / 1024.0);
        } else {
            return String.format(Locale.US, "%.2f MB", mb);
        }
    }

    public static String getQuotaLabel(long quotaMB) {
        if (quotaMB <= 0L) return "OFF";
        for (int i = 1; i < QUOTA_VALUES.length; i++) {
            if (quotaMB == QUOTA_VALUES[i]) {
                return QUOTA_OPTIONS[i];
            }
        }
        if (quotaMB >= 1024L && quotaMB % 1024L == 0) {
            return String.format(Locale.US, "%d GB", quotaMB / 1024L);
        } else if (quotaMB >= 1024L) {
            return String.format(Locale.US, "%.2f GB", quotaMB / 1024.0);
        } else {
            return String.format(Locale.US, "%d MB", quotaMB);
        }
    }
}
