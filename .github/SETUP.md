# GitHub Actions Setup Guide

## Overview

Two workflows are included:

| Workflow | Trigger | What it does |
|---|---|---|
| `build.yml` | push to `main`/`develop`, any `v*.*.*` tag, PRs, manual | Lint → test → debug APK → signed release APK → AAB → GitHub Release |
| `pr-check.yml` | Pull requests to `main`/`develop` | Fast compile + lint + test, posts APK size comment |

---

## Required GitHub Secrets

Go to **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | Your `.jks` keystore encoded as base64 (see below) |
| `KEYSTORE_PASSWORD` | The password for the keystore |
| `KEY_ALIAS` | The key alias inside the keystore |
| `KEY_PASSWORD` | The password for the key (often same as keystore password) |

---

## Generating a Signing Keystore (first time)

If you don't have a keystore yet, run this on your machine:

```bash
keytool -genkey -v \
  -keystore velora-release.jks \
  -alias velora \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Follow the prompts. Keep this file safe — **you cannot re-upload an app to the Play Store with a different keystore.**

---

## Encoding the Keystore for GitHub Secrets

### macOS / Linux
```bash
base64 -i velora-release.jks | pbcopy   # macOS (copies to clipboard)
base64 -w 0 velora-release.jks           # Linux (prints to terminal)
```

### Windows (PowerShell)
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("velora-release.jks")) | clip
```

Paste the output as the value of `KEYSTORE_BASE64`.

---

## Triggering a Release

Push a version tag to create a signed APK + AAB and a GitHub Release automatically:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Tag format rules:
- `v1.0.0` → stable release
- `v1.0.0-beta1` → pre-release (marked as pre-release on GitHub)
- `v1.0.0-rc1` → release candidate (also marked as pre-release)

---

## Workflow Diagram

```
push / PR
    │
    ├─ lint-and-test ──────────────────────────────────────────┐
    │       │                                                   │
    │       ├─ build-debug (all pushes + PRs)                  │
    │       │       └── uploads debug APK (30-day retention)   │
    │       │                                                   │
    │       ├─ build-release (main branch + tags only)         │
    │       │       └── uploads signed APK (90-day retention)  │
    │       │                                                   │
    │       └─ build-aab (tags only)                           │
    │               └── uploads AAB for Play Store             │
    │                                                           │
    └─ github-release (tags only) ──────────────────────────── ┘
            └── creates GitHub Release with APK + AAB attached
```

---

## Manual Build Trigger

You can trigger a build manually from the **Actions** tab in GitHub:

1. Click **Build & Release Velora**
2. Click **Run workflow**
3. Choose `debug` or `release`
4. Click **Run workflow**

The `release` option requires the keystore secrets to be set up.
