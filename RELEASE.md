# Release Guide & Versioning 🚀

This document outlines the release process, automated build workflows, and versioning conventions for **Data Burner** (Data-Waster).

---

## 🤖 Automated Release Bot Workflow

Data Burner uses GitHub Actions to automatically compile, package, and release build assets whenever changes are pushed to `main` or a release tag is created.

### Release Assets Generated Automatically:
1. **`app-release.apk`**: Production Android APK signed and optimized for distribution.
2. **`app-debug.apk`**: Debuggable Android APK for testing and developer troubleshooting.
3. **`databurner-lib.jar`**: Compiled Java library for incorporating Data Burner into external Java/Android projects.

---

## 🏷️ How to Trigger a New Version Release

### Option 1: Automatic Release on Tag (Recommended)
To publish an official version release (e.g., `v1.0.0`), tag your commit and push it to GitHub:

```bash
# Create a semver tag
git tag -a v1.0.0 -m "Release version 1.0.0"

# Push the tag to GitHub
git push origin v1.0.0
```
GitHub Actions will automatically pick up the tag, build all binaries, generate release notes, and create the GitHub Release.

### Option 2: Automatic `latest` Release on Push to `main`
Every push to the `main` branch automatically updates the [`latest`](https://github.com/Latikeshh/Data-Waster/releases/tag/latest) release with fresh binaries.

### Option 3: Manual Workflow Trigger
1. Go to the **Actions** tab in the GitHub repository.
2. Select **APK Builder & Release Maker**.
3. Click **Run workflow** -> Select `main` -> Click **Run workflow**.

---

## 📦 Versioning Scheme

Data Burner follows [Semantic Versioning (SemVer)](https://semver.org/):
- `MAJOR.MINOR.PATCH` (e.g., `v1.0.0`)
  - **MAJOR**: Breaking API or structural changes.
  - **MINOR**: New features or major UI updates (backwards-compatible).
  - **PATCH**: Bug fixes, performance tweaks, or minor adjustments.
