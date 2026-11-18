# Data Burner (Data-Waster) 🔥

[![APK Builder & Release Maker](https://github.com/Latikeshh/Data-Waster/actions/workflows/apk-builder.yml/badge.svg)](https://github.com/Latikeshh/Data-Waster/actions/workflows/apk-builder.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Android Min SDK: 26](https://img.shields.io/badge/Min%20SDK-26%20(Android%208.0)-brightgreen.svg)](https://developer.android.com)
[![Java 11](https://img.shields.io/badge/Java-11%2B-orange.svg)](https://www.oracle.com/java/)

**Data Burner** (Data-Waster) is a high-performance multi-threaded cellular & Wi-Fi network stress-testing utility and Android app. Designed for 5G speed benchmarks, cellular data quota testing, network stability diagnosis, and stress-testing cellular carrier bandwidth limits.

---

## ⚡ Features

- **Multi-Threaded Engine:** Up to 35+ concurrent persistent HTTP connections for maximum network bandwidth utilization.
- **5G / Cellular Guard:** Automatically pauses or alerts when non-5G / cellular networks are disconnected to protect your main data plan.
- **Quota Management:** Set data quota limits (in MB or GB) with auto-stop when targets are met.
- **Real-Time Telemetry & HUD:** Live speedometer gauge, real-time speed chart, active thread telemetry, peak speed, and average speed analytics.
- **Custom Mirrors & CDNs:** Pre-configured with high-speed 10G CDN binaries (Hetzner, Belwue, Otenet, Tele2) + full support for custom mirror URLs.
- **Foreground Service & Quick Tile:** Android Foreground Service with continuous notification and Android Quick Settings Tile integration.
- **Java Library (`databurner-lib`):** Standalone, dependency-free Java/Android library to integrate data burning & stress testing into your own applications!

---

## 🚀 Quick Start: Java Library Integration (`databurner-lib`)

You can use the core Data Burner engine directly in your Java/Android projects without any UI dependencies!

### 1. Add Gradle Dependency

Include `:databurner-lib` in your project or import the compiled JAR/AAR:

```kotlin
// settings.gradle.kts
include(":databurner-lib")

// app/build.gradle.kts
dependencies {
    implementation(project(":databurner-lib"))
}
```

### 2. Basic Usage in Java / Kotlin

```java
import com.databurner.lib.DataBurnerEngine;
import com.databurner.lib.DataBurnerConfig;
import com.databurner.lib.DataBurnerListener;
import com.databurner.lib.BurnStats;

// Configure the engine
DataBurnerConfig config = new DataBurnerConfig.Builder()
        .setThreadCount(20)                 // Concurrent streams
        .setQuotaMB(1000)                    // 1 GB Quota limit (0 = unlimited)
        .addCustomServer("https://speed.hetzner.de/1GB.bin")
        .build();

// Create Engine instance
DataBurnerEngine engine = new DataBurnerEngine(config);

// Register telemetry listener
engine.setListener(new DataBurnerListener() {
    @Override
    public void onTick(BurnStats stats) {
        System.out.printf("Speed: %.2f MB/s | Total: %.2f MB | Threads: %d | Status: %s%n",
                stats.getSpeedMBps(),
                stats.getTotalMBBurned(),
                stats.getActiveThreads(),
                stats.getLastResponseStatus());
    }

    @Override
    public void onStatusChanged(String status) {
        System.out.println("Engine status: " + status);
    }

    @Override
    public void onQuotaReached(BurnStats finalStats) {
        System.out.println("Quota Target Reached! Total Burned: " + finalStats.getTotalMBBurned() + " MB");
    }

    @Override
    public void onError(Exception e) {
        System.err.println("Burn error: " + e.getMessage());
    }
});

// Start data burning
engine.start();

// Stop data burning
engine.stop();
```

---

## 🛠️ Building & Developing

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or IntelliJ IDEA.
- JDK 17 or higher.
- Android SDK 35/37.

### Build Commands
```bash
# Clone the repository
git clone https://github.com/Latikeshh/Data-Waster.git
cd Data-Waster

# Build the Android APKs
./gradlew assembleRelease

# Build the Java Library
./gradlew :databurner-lib:build
```

---

## 🔒 Security & Usage Warning

> [!CAUTION]
> **Data Consumption Alert:** Data Burner consumes large amounts of mobile data at very high speeds (up to several gigabytes per minute on 5G). Ensure you have an unlimited cellular data plan or set strict quota limits before running prolonged sessions.

---

## 📜 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on code style, submitting issues, and pull request workflows.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
