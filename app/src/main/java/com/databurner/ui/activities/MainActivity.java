package com.databurner.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.databurner.R;
import com.databurner.controller.DashboardController;
import com.databurner.service.BurnService;
import com.databurner.service.ServerManager;
import com.databurner.ui.components.RealtimeSpeedChartView;
import com.databurner.ui.components.SpeedometerGaugeView;
import com.databurner.ui.dialogs.HudDiagnosticsDialog;
import com.databurner.ui.holders.FeatureCardHolder;
import com.databurner.ui.holders.StatCardHolder;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

	private static final String KEY_CARD_FLIPPED = "is_card_flipped";

	private TextView statusText, usedText, speedText, timeText, guardStatusBadge, peakText, avgText;
	private View statusDot;
	private SpeedometerGaugeView speedGaugeView;
	private RealtimeSpeedChartView speedChartView;
	private LinearProgressIndicator speedBar;
	private MaterialButton startBtn, stopBtn, quotaBtn;
	private MaterialCardView heroCard;
	private MaterialSwitch guardSwitch;
	private final ServerManager serverManager = new ServerManager();

	// 3D Card Flip View Bindings
	private View heroCardFront, heroCardBack;
	private TextView backTransportText, backIpText, backThreadsText, backThreadsMaxText, backEngineModeText, backProtocolText, backRamText, backMemorySubText;
	private ImageView backNetworkIcon;
	private ProgressBar backThreadsProgressBar;
	private boolean isCardFlipped = false;
	private boolean isFlipping = false;

	// Async Background Executor for Non-blocking I/O
	private final ExecutorService asyncExecutor = Executors.newSingleThreadExecutor();
	private String cachedIpAddress = "10.45.12.89";
	private String cachedRamUsage = "24.5 MB RAM";
	private long lastAsyncCheckTime = 0L;

	private SharedPreferences prefs;
	private final Handler handler = new Handler(Looper.getMainLooper());
	private Runnable updateRunnable;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Modern Window Inset Routing via DashboardController
		DashboardController.applyWindowInsets(findViewById(R.id.main), R.id.appBarLayout, R.id.startBtn, 0);

		prefs = getSharedPreferences("burn_stats", MODE_PRIVATE);

		statusText = findViewById(R.id.statusText);
		speedText = findViewById(R.id.speedText);
		statusDot = findViewById(R.id.statusDot);
		speedGaugeView = findViewById(R.id.speedGaugeView);
		speedBar = findViewById(R.id.speedBar);
		heroCard = findViewById(R.id.heroCard);
		startBtn = findViewById(R.id.startBtn);
		stopBtn = findViewById(R.id.stopBtn);

		// Initialize Dashboard Stat & Feature Cards via DashboardController
		initCards();

		MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
		if (topAppBar != null) {
			topAppBar.setOnMenuItemClickListener(item -> {
				int id = item.getItemId();
				if (id == R.id.action_settings) {
					triggerHapticFeedback(topAppBar);
					Intent intent = new Intent(this, SettingsActivity.class);
					startActivity(intent);
					return true;
				} else if (id == R.id.action_more_info) {
					triggerHapticFeedback(topAppBar);
					Intent intent = new Intent(this, AboutActivity.class);
					intent.putExtra(AboutActivity.EXTRA_PAGE_TYPE, AboutActivity.PAGE_MORE_INFO);
					startActivity(intent);
					return true;
				} else if (id == R.id.action_about) {
					triggerHapticFeedback(topAppBar);
					Intent intent = new Intent(this, AboutActivity.class);
					intent.putExtra(AboutActivity.EXTRA_PAGE_TYPE, AboutActivity.PAGE_ABOUT);
					startActivity(intent);
					return true;
				}
				return false;
			});
		}

		View logoHeaderContainer = findViewById(R.id.logoHeaderContainer);
		if (logoHeaderContainer != null) {
			logoHeaderContainer.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				DashboardController.showLogoPreviewDialog(this);
			});
		}

		// 3D Card Flip Bindings
		heroCardFront = findViewById(R.id.heroCardFront);
		heroCardBack = findViewById(R.id.heroCardBack);
		backTransportText = findViewById(R.id.backTransportText);
		backIpText = findViewById(R.id.backIpText);
		backThreadsText = findViewById(R.id.backThreadsText);
		backThreadsMaxText = findViewById(R.id.backThreadsMaxText);
		backEngineModeText = findViewById(R.id.backEngineModeText);
		backProtocolText = findViewById(R.id.backProtocolText);
		backRamText = findViewById(R.id.backRamText);
		backMemorySubText = findViewById(R.id.backMemorySubText);
		backNetworkIcon = findViewById(R.id.backNetworkIcon);
		backThreadsProgressBar = findViewById(R.id.backThreadsProgressBar);

		float scale = getResources().getDisplayMetrics().density;
		float distance = 8000 * scale;
		if (heroCardFront != null) heroCardFront.setCameraDistance(distance);
		if (heroCardBack != null) heroCardBack.setCameraDistance(distance);

		if (savedInstanceState != null) {
			isCardFlipped = savedInstanceState.getBoolean(KEY_CARD_FLIPPED, false);
			if (isCardFlipped && heroCardFront != null && heroCardBack != null) {
				heroCardFront.setVisibility(View.GONE);
				heroCardBack.setVisibility(View.VISIBLE);
			}
		}

		if (heroCard != null) {
			heroCard.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				toggleCardFlip();
			});
		}

		View tileNetwork = findViewById(R.id.tileNetwork);
		View tileThreads = findViewById(R.id.tileThreads);
		View tileEngine = findViewById(R.id.tileEngine);
		View tileMemory = findViewById(R.id.tileMemory);

		if (tileNetwork != null) {
			tileNetwork.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				HudDiagnosticsDialog.showNetworkDiagnostics(this);
			});
		}
		if (tileThreads != null) {
			tileThreads.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				HudDiagnosticsDialog.showThreadDiagnostics(this, prefs);
			});
		}
		if (tileEngine != null) {
			tileEngine.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				HudDiagnosticsDialog.showEngineDiagnostics(this, serverManager, prefs);
			});
		}
		if (tileMemory != null) {
			tileMemory.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				HudDiagnosticsDialog.showMemoryDiagnostics(this, prefs);
			});
		}

		speedChartView = findViewById(R.id.speedChartView);

		if (guardSwitch != null) {
			guardSwitch.setChecked(prefs.getBoolean("guard_enabled", false));
			guardSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
				triggerHapticFeedback(buttonView);
				prefs.edit().putBoolean("guard_enabled", isChecked).apply();
				if (isChecked) {
					Intent intent = new Intent(this, BurnService.class);
					startForegroundService(intent);
				} else {
					if (!prefs.getBoolean("service_running", false)) {
						stopService(new Intent(this, BurnService.class));
					}
				}
				updateUI();
			});
		}

		if (quotaBtn != null) {
			quotaBtn.setOnClickListener(v -> {
				triggerHapticFeedback(v);
				DashboardController.showQuotaSelectionDialog(
						this,
						prefs,
						this::updateUI,
						() -> DashboardController.showCustomQuotaInputDialog(this, prefs, this::updateUI)
				);
			});
		}

		String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
				new String[]{Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.POST_NOTIFICATIONS} :
				new String[]{Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE};
		ActivityCompat.requestPermissions(this, permissions, 1);

		startBtn.setOnClickListener(v -> {
			triggerHapticFeedback(v);
			Intent startIntent = new Intent(this, BurnService.class);
			startIntent.setAction(BurnService.ACTION_START);
			startForegroundService(startIntent);
			prefs.edit().putBoolean("service_running", true).apply();
			updateUI();
		});

		stopBtn.setOnClickListener(v -> {
			triggerHapticFeedback(v);
			stopService(new Intent(this, BurnService.class));
			prefs.edit().putBoolean("service_running", false).apply();
			updateUI();
		});

		updateRunnable = new Runnable() {
			@Override
			public void run() {
				updateUI();
				handler.postDelayed(this, 1000);
			}
		};
	}

	private void triggerHapticFeedback(View view) {
		if (view != null) {
			view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_CARD_FLIPPED, isCardFlipped);
	}

	private void initCards() {
		View root = findViewById(android.R.id.content);
		StatCardHolder statHolder = DashboardController.setupStatCards(this, root, prefs, this::updateUI);
		usedText = statHolder.usedText;
		timeText = statHolder.timeText;
		peakText = statHolder.peakText;
		avgText = statHolder.avgText;

		FeatureCardHolder featureHolder = DashboardController.bindFeatureCards(this, root);
		quotaBtn = featureHolder.quotaBtn;
		guardStatusBadge = featureHolder.guardStatusBadge;
		guardSwitch = featureHolder.guardSwitch;

		View cardPeak = root.findViewById(R.id.cardPeakWrapper);
		View cardAvg = root.findViewById(R.id.cardAvgWrapper);
		View.OnLongClickListener threadConfigListener = v -> {
			triggerHapticFeedback(v);
			DashboardController.showThreadSliderDialog(this, prefs, this::updateUI);
			return true;
		};
		if (cardPeak != null) cardPeak.setOnLongClickListener(threadConfigListener);
		if (cardAvg != null) cardAvg.setOnLongClickListener(threadConfigListener);
	}

	private void toggleCardFlip() {
		if (isFlipping || heroCardFront == null || heroCardBack == null) return;
		isFlipping = true;

		if (!isCardFlipped) {
			updateBackSideData();
			heroCardFront.animate()
					.rotationY(90f)
					.setDuration(220)
					.withEndAction(() -> {
						heroCardFront.setVisibility(View.GONE);
						heroCardBack.setRotationY(-90f);
						heroCardBack.setVisibility(View.VISIBLE);
						heroCardBack.animate()
								.rotationY(0f)
								.setDuration(220)
								.withEndAction(() -> {
									isCardFlipped = true;
									isFlipping = false;
								})
								.start();
					})
					.start();
		} else {
			heroCardBack.animate()
					.rotationY(90f)
					.setDuration(220)
					.withEndAction(() -> {
						heroCardBack.setVisibility(View.GONE);
						heroCardFront.setRotationY(-90f);
						heroCardFront.setVisibility(View.VISIBLE);
						heroCardFront.animate()
								.rotationY(0f)
								.setDuration(220)
								.withEndAction(() -> {
									isCardFlipped = false;
									isFlipping = false;
								})
								.start();
					})
					.start();
		}
	}

	private void updateBackSideData() {
		boolean running = prefs.getBoolean("service_running", false);
		int activeThreads = BurnService.getActiveThreads();
		String httpStatus = BurnService.getLastResponseStatus();
		String transport = fetchNetworkTransportType();

		if (backTransportText != null) {
			backTransportText.setText(running ? transport : "IDLE");
		}
		if (backNetworkIcon != null) {
			boolean isWifi = transport.contains("Wi-Fi");
			backNetworkIcon.setImageResource(isWifi ? R.drawable.ic_wifi : R.drawable.ic_shield_5g);
		}
		if (backIpText != null) {
			backIpText.setText(cachedIpAddress);
		}
		int maxThreads = prefs.getInt("thread_count", 20);
		int currentActiveThreads = running ? activeThreads : 0;
		if (backThreadsText != null) {
			backThreadsText.setText(running ? activeThreads + " Active" : "0 Active");
		}
		if (backThreadsProgressBar != null) {
			int pct = maxThreads > 0 ? Math.min(100, Math.round((currentActiveThreads / (float) maxThreads) * 100)) : 0;
			backThreadsProgressBar.setProgress(pct, true);
		}
		if (backThreadsMaxText != null) {
			backThreadsMaxText.setText(currentActiveThreads + " / " + maxThreads + " Max");
		}
		if (backEngineModeText != null) {
			backEngineModeText.setText("CDN MIRRORS");
		}
		if (backProtocolText != null) {
			backProtocolText.setText(running ? httpStatus + " • Running" : "Standby");
		}
		if (backRamText != null) {
			backRamText.setText(cachedRamUsage);
		}
		if (backMemorySubText != null) {
			Runtime runtime = Runtime.getRuntime();
			long usedMemMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
			long maxMemMB = runtime.maxMemory() / (1024 * 1024);
			int pct = maxMemMB > 0 ? (int) ((usedMemMB * 100) / maxMemMB) : 0;
			int batt = fetchBatteryLevel();
			backMemorySubText.setText(pct + "% Heap • 🔋 " + batt + "%");
		}

		// Offload Network & RAM I/O to background thread
		long now = System.currentTimeMillis();
		if (now - lastAsyncCheckTime > 3000L) {
			lastAsyncCheckTime = now;
			asyncExecutor.execute(() -> {
				String ip = fetchLocalIpAddressAsync();
				String ram = fetchAppRamUsageAsync();
				runOnUiThread(() -> {
					cachedIpAddress = ip;
					cachedRamUsage = ram;
					if (backIpText != null) backIpText.setText(cachedIpAddress);
					if (backRamText != null) backRamText.setText(cachedRamUsage);
				});
			});
		}
	}

	private String fetchNetworkTransportType() {
		try {
			ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				Network network = cm.getActiveNetwork();
				if (network != null) {
					NetworkCapabilities caps = cm.getNetworkCapabilities(network);
					if (caps != null) {
						if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi High Speed";
						if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "5G NR";
						if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "Ethernet";
					}
				}
			}
		} catch (Exception ignored) {}
		return "5G NR";
	}

	private int fetchBatteryLevel() {
		try {
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			Intent batteryStatus = registerReceiver(null, ifilter);
			if (batteryStatus != null) {
				int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				if (level >= 0 && scale > 0) {
					return Math.round((level / (float) scale) * 100);
				}
			}
		} catch (Exception ignored) {}
		return 95;
	}

	private String fetchLocalIpAddressAsync() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
						return inetAddress.getHostAddress();
					}
				}
			}
		} catch (Exception ignored) {}
		return "10.45.12.89";
	}

	private String fetchAppRamUsageAsync() {
		try {
			Runtime runtime = Runtime.getRuntime();
			long usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
			return usedMem + " MB RAM";
		} catch (Exception ignored) {}
		return "24.5 MB RAM";
	}

	@Override
	protected void onResume() {
		super.onResume();
		handler.post(updateRunnable);
	}

	@Override
	protected void onPause() {
		super.onPause();
		handler.removeCallbacks(updateRunnable);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		asyncExecutor.shutdown();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	private void updateUI() {
		boolean running = prefs.getBoolean("service_running", false);
		boolean guardEnabled = prefs.getBoolean("guard_enabled", false);
		String status = prefs.getString("service_status", running ? "Burning Active" : "System Ready");
		long usedBytes = prefs.getLong("used_bytes", 0);
		float speedMB = prefs.getFloat("speed_mb", 0);
		float peakMB = prefs.getFloat("peak_speed", 0f);
		float avgMB = prefs.getFloat("avg_speed", 0f);
		long quotaMB = prefs.getLong("quota_mb", 0L);
		String time = prefs.getString("session_time", "00:00:00");

		statusText.setText(status);
		usedText.setText(DashboardController.formatData(usedBytes / (1024.0 * 1024.0)));
		speedText.setText(String.format(Locale.US, "%.2f", speedMB));
		timeText.setText(time);

		if (isCardFlipped) {
			updateBackSideData();
		}

		if (peakText != null) {
			peakText.setText(String.format(Locale.US, "%.2f MB/s", peakMB));
		}

		if (avgText != null) {
			avgText.setText(String.format(Locale.US, "%.2f MB/s", avgMB));
		}

		if (quotaBtn != null) {
			quotaBtn.setText(DashboardController.getQuotaLabel(quotaMB));
		}

		if (speedGaugeView != null) {
			speedGaugeView.setSpeed(speedMB);
		}

		if (speedChartView != null && running) {
			speedChartView.addSpeedSample(speedMB);
		}

		startBtn.setVisibility(running ? View.GONE : View.VISIBLE);
		stopBtn.setVisibility(running ? View.VISIBLE : View.GONE);
		startBtn.setEnabled(!running);
		stopBtn.setEnabled(running);

		if (statusDot != null) {
			statusDot.setBackgroundResource(running ? R.drawable.bg_status_dot_active : R.drawable.bg_status_dot_idle);
		}

		if (speedBar != null) {
			int progress = Math.min(100, Math.round(speedMB * 2.0f));
			speedBar.setProgress(progress, true);
		}

		if (guardStatusBadge != null) {
			guardStatusBadge.setText(guardEnabled ? "PROTECTED ON 5G" : "GUARD DISABLED");
			guardStatusBadge.setTextColor(getColor(guardEnabled ? R.color.tertiary_m3 : R.color.on_surface_variant));
		}
	}
}
