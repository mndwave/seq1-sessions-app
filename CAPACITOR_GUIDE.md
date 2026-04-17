# Capacitor "Server URL" Pattern — seq1-sessions-app

## What this is

A native Android APK that wraps `sessions.seq1.net` in a Capacitor WebView shell.
This is the **canonical pattern** for converting a Next.js SSR app to Android.

## Architecture

```
APK (Capacitor shell)
 └── WebView → loads https://sessions.seq1.net live
      └── Native plugins bridge: mic, filesystem, vibration
```

### Why Server URL mode (not static bundle)

| Static bundle | Server URL (this project) |
|---|---|
| Requires `next export` (breaks SSR) | Works with SSR — loads live URL |
| APK rebuild on every web change | Web changes = instant, no APK rebuild |
| No server required | App requires internet for core functions anyway |
| Good for truly offline apps | Wrong for apps that are inherently online |

**Rule:** If the web app can't function offline anyway, use Server URL mode.
Only use static bundle if you genuinely need offline-first behaviour.

## When to rebuild the APK

**APK rebuild required:**
- Changes to `capacitor.config.ts`
- Changes to `android/` (permissions, plugins, theme, icons)
- Adding/upgrading Capacitor plugins

**APK rebuild NOT required:**
- Any change to `sessions.seq1.net` web code
- New features, UI changes, bug fixes in the web app
- Backend changes

The GitHub Actions workflow (`build-apk.yml`) is path-scoped to enforce this automatically.

## APK distribution

- **Latest APK:** `https://media.seq1.net/app/seq1-sessions-latest.apk`
- **Download page:** `https://sessions.seq1.net/app`
- **Versioned builds:** `https://media.seq1.net/app/seq1-sessions-{sha}.apk` (90-day retention)

## GitHub Actions secrets required

| Secret | Value source |
|---|---|
| `R2_ACCOUNT_ID` | Cloudflare R2 account ID |
| `R2_ACCESS_KEY_ID` | R2 API token (read+write on media bucket) |
| `R2_SECRET_ACCESS_KEY` | R2 API token secret |
| `R2_BUCKET` | `media` (the seq1 media R2 bucket) |

## Visual parity

Visual parity is **structural** — the app IS the website. No separate testing needed.

For Playwright regression tests: test `sessions.seq1.net` (web). The app shows the same thing.

The only visual difference: the Android system status bar overlays the top of the app. 
Set `android:windowSoftInputMode` and safe area insets if this causes layout issues.

## Adding this pattern to a new client project

1. `mkdir client-app && cd client-app && npm init -y`
2. `npm install @capacitor/core@5 @capacitor/cli@5 @capacitor/android@5 @capacitor/filesystem@5 --legacy-peer-deps`
3. `npx cap init "App Name" "com.client.app" --web-dir=www`
4. Edit `capacitor.config.ts`: set `server.url` to the live Next.js URL
5. `mkdir -p www && echo '<html><body>Loading...</body></html>' > www/index.html`
6. `npx cap add android`
7. Edit `android/app/src/main/AndroidManifest.xml`: add `RECORD_AUDIO`, `INTERNET` etc.
8. Copy `.github/workflows/build-apk.yml` and update secrets
9. Add R2 secrets to GitHub repo settings
10. Push — GitHub Actions builds and uploads the APK

## Android Icon — Complete Guide

### How Android icon resources work

Android uses two parallel icon systems:

| System | When used | Files |
|---|---|---|
| **Adaptive icon** (API 26+) | Android 8.0+ | `mipmap-anydpi-v26/ic_launcher.xml` → background layer + foreground layer |
| **Legacy PNG** | Android < 8.0 | `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` |

On Android 8+, the `mipmap-anydpi-v26/ic_launcher.xml` takes precedence over all density-specific PNGs for the launcher icon. If this XML is wrong, the PNG icons are completely ignored.

### The adaptive icon foreground trap (anti-pattern)

Capacitor generates the adaptive icon XML as:
```xml
<foreground android:drawable="@mipmap/ic_launcher_foreground"/>
```

This references `mipmap-{density}/ic_launcher_foreground.png` — the old Capacitor robot PNG files.

**If you only update the PNG files, the icon won't change on Android 8+ because the adaptive icon XML still points to the old PNGs.**

The correct approach: change the adaptive icon foreground to reference the vector drawable:
```xml
<!-- mipmap-anydpi-v26/ic_launcher.xml -->
<foreground android:drawable="@drawable/ic_launcher_foreground"/>
```

Then update `drawable-v24/ic_launcher_foreground.xml` with your actual icon design.

### Android launcher icon caching — the invisible blocker

Android launchers (Pixel Launcher, Samsung One UI Home, etc.) cache app icons in their own SQLite database, **independently of the APK**. This cache survives:
- APK uninstall + reinstall of the same package ID
- APK updates
- System reboots (usually)

**Symptom:** You install a new APK with a new icon, but the old icon is still showing on the home screen.

**This is not a build error.** The APK contains the correct icon — the launcher is serving a stale cache entry.

**Fix (on device):**
1. Settings → Apps → [your launcher app] → Storage → Clear Cache
2. Common launcher package names: `com.google.android.apps.nexuslauncher` (Pixel), `com.sec.android.app.launcher` (Samsung)
3. Or: restart the phone (usually clears launcher in-memory cache)
4. Or: long-press home screen → launcher settings → clear icon cache

**Verification (before blaming the build):** Open Settings → Apps → SEQ1 Sessions → the app info icon is read directly from the APK, not from the launcher cache. If the app info shows the correct icon but the home screen doesn't, it's a launcher cache issue.

### How to verify the icon inside the APK (without a device)

```bash
# Find the xxxhdpi launcher PNG inside the APK
for f in $(unzip -l app-release.apk | awk '{print $4}' | grep "\.png$"); do
  unzip -p app-release.apk "$f" > /tmp/chk.png 2>/dev/null
  dims=$(identify /tmp/chk.png 2>/dev/null | awk '{print $3}')
  [ "$dims" = "192x192" ] && echo "Found 192px: $f" && cp /tmp/chk.png /tmp/icon-verify.png && break
done
# Then visually inspect /tmp/icon-verify.png
```

### Signed release build (not debug)

Debug APKs work fine for personal use but show `(debug)` in the installer and have `webContentsDebuggingEnabled` active (Chrome DevTools can attach).

To build a signed release:
```bash
# Generate keystore once (store this file safely — loss = can't update the app)
keytool -genkey -v -keystore seq1-sessions-release.keystore \
  -alias seq1sessions -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=SEQ1 Sessions, OU=SEQ1, O=SEQ1, L=London, S=England, C=GB" \
  -storepass yourpassword -keypass yourpassword

# Configure in android/app/build.gradle signingConfigs block (see build.gradle)
# Then build:
./gradlew clean assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk
```

Key differences vs debug:
- Proper certificate (not the debug keystore shared by all Android devs)
- `webContentsDebuggingEnabled: false` in capacitor.config.json
- Smaller APK (debug symbols stripped)
- `versionName` / `versionCode` matter for auto-update detection (bump versionCode on every release)

## Known anti-patterns

### ❌ Using `next export` for SSR apps

`next export` doesn't work if your app uses:
- API routes (`/api/...`)
- Server components
- `getServerSideProps`
- Middleware

None of these can be statically exported. `server.url` is the correct approach.

### ❌ Rebuilding APK on every web push

The whole point of Server URL mode is that you don't need to. If you add a path trigger 
on `app/**` or `src/**`, you're rebuilding unnecessarily. Only trigger on `android/**` and 
`capacitor.config.ts`.

### ❌ Using `webContentsDebuggingEnabled: true` in production

This allows any Chrome DevTools client to attach to the WebView remotely.
The workflow strips this flag on production builds. Don't merge code with it set to `true`
in a release branch.

### ❌ Putting signing keys in the workflow file

The debug build (`.apk`) is fine for internal distribution. For Play Store releases,
you need a signed release build. Store the keystore as a GitHub encrypted secret,
never in the repo.

### ❌ Assuming getUserMedia works the same as browser

Android WebView's `getUserMedia` requires the permission to be granted natively first
(via `AndroidManifest.xml RECORD_AUDIO`). The first launch triggers a native permission
dialog. If the user denies, `getUserMedia` will throw `NotAllowedError` permanently.
Handle this gracefully — show a settings deep-link to let the user re-grant.

## File structure

```
seq1-sessions-app/
├── capacitor.config.ts          ← Server URL + plugin config
├── www/                         ← Stub (not served — server.url overrides)
│   └── index.html
├── android/                     ← Generated by `npx cap add android`
│   └── app/src/main/
│       └── AndroidManifest.xml  ← Permissions — edit this, not the generated files
└── .github/workflows/
    └── build-apk.yml            ← Path-scoped trigger; uploads to R2
```
