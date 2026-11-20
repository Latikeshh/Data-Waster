# Data Burner (Data-Waster) 🔥

[![APK Builder & Release Maker](https://github.com/Latikeshh/Data-Waster/actions/workflows/apk-builder.yml/badge.svg)](https://github.com/Latikeshh/Data-Waster/actions/workflows/apk-builder.yml)
[![JitPack](https://jitpack.io/v/Latikeshh/Data-Waster.svg)](https://jitpack.io/#Latikeshh/Data-Waster)
[![Dependabot](https://img.shields.io/badge/Dependabot-enabled-blue.svg)](.github/dependabot.yml)
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

## 🤖 Automated Free Release Bots & Workflows

This repository is configured with automated GitHub bots and workflows:
- **`github-actions[bot]`**: Automatically builds signed `app-release.apk`, `app-debug.apk`, and `databurner-lib.jar` binaries whenever a commit or tag is pushed.
- **`dependabot[bot]`**: Automatically checks and submits pull requests for weekly updates to Gradle dependencies and GitHub Actions.

For detailed release workflows and versioning instructions, see [**`RELEASE.md`**](RELEASE.md).

---

## 🚀 Quick Start: Java Library Integration (`databurner-lib`)

You can use the core Data Burner engine directly in your Java/Android projects without any UI dependencies via **JitPack / Maven** or local project inclusion!

### Option 1: Via JitPack (Recommended for External Apps)

Add the JitPack repository to your `settings.gradle.kts` (or root `build.gradle`):
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Add the library dependency to your `app/build.gradle.kts`:
```kotlin
dependencies {
    implementation("com.github.Latikeshh:Data-Waster:26.03.30")
    // Or target latest main branch:
    // implementation("com.github.Latikeshh:Data-Waster:main-SNAPSHOT")
}
```

#### Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Latikeshh</groupId>
    <artifactId>Data-Waster</artifactId>
    <version>26.03.30</version>
</dependency>
```

---

### Option 2: Local Subproject Inclusion

```kotlin
// settings.gradle.kts
include(":databurner-lib")

// app/build.gradle.kts
dependencies {
    implementation(project(":databurner-lib"))
}
```

---

### 💻 Basic Usage in Java / Kotlin

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
        System.out.printf("Speed: %.2f MB/s | Peak: %.2f MB/s | Total: %.2f MB | Threads: %d | Status: %s%n",
                stats.getSpeedMBps(),
                stats.getPeakSpeedMBps(),
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

// On Android: Pass Activity ('this').
// The library AUTOMATICALLY checks permissions, shows non-cancelable dialogs if missing,
// acquires CPU WakeLocks & WiFi Locks, and starts streaming—zero permission code needed!
engine.start(this); 

// On Standard JVM:
// engine.start();

// Stop data burning (Automatically releases CPU & WiFi locks)
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

## 📜 Contributing & Releases

- Please see [**`CONTRIBUTING.md`**](CONTRIBUTING.md) for contribution guidelines.
- Please see [**`RELEASE.md`**](RELEASE.md) for automated build and release instructions.
- Please see [**`SECURITY.md`**](SECURITY.md) for security policy.

---

## 📄 License

This project is licensed under the MIT License - see the [**`LICENSE`**](LICENSE) file for details.
