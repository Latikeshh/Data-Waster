# Contributing to Data Burner 🔥

Thank you for your interest in contributing to **Data Burner** (Data-Waster)! We welcome contributions from developers of all skill levels.

---

## 📋 Table of Contents
1. [Code of Conduct](#code-of-conduct)
2. [How to Contribute](#how-to-contribute)
3. [Submitting Pull Requests](#submitting-pull-requests)
4. [Development Guidelines](#development-guidelines)
5. [Reporting Bugs](#reporting-bugs)

---

## 🤝 Code of Conduct

This project adheres to open-source community standards. Please review our [Code of Conduct](CODE_OF_CONDUCT.md) before participating.

---

## 🚀 How to Contribute

### 1. Fork and Clone
```bash
git clone https://github.com/Latikeshh/Data-Waster.git
cd Data-Waster
```

### 2. Create a Feature Branch
Create a branch named after the feature or fix you are working on:
```bash
git checkout -b feature/my-new-feature
```

### 3. Make Changes & Test Locally
Ensure the project compiles without errors before committing:
```bash
# Build Android App
./gradlew assembleDebug

# Build Java Library
./gradlew :databurner-lib:build

# Run Unit Tests
./gradlew test
```

---

## 📥 Submitting Pull Requests

1. Keep your PRs focused on a single feature, bug fix, or documentation improvement.
2. Ensure code is well-formatted and adheres to standard Java/Android conventions.
3. Include a detailed description of the changes in your PR summary.
4. Link any related issues solved by your PR.

---

## 🛠️ Development Guidelines

- **Architecture:** Maintain clean separation of concerns. Keep UI logic in activity/view classes and engine logic in `:databurner-lib` or background services.
- **Java Compatibility:** Ensure `:databurner-lib` remains pure Java (Java 11 compatible) with zero Android-only or external library dependencies.
- **Gradle:** Do not commit hardcoded credentials or keystore secrets.

---

## 🐛 Reporting Bugs

If you find a bug, please open a GitHub Issue with:
- Steps to reproduce.
- Expected vs. actual behavior.
- Android OS version, device model, or Java runtime version.
- Relevant `logcat` or console output.



.
