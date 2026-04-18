import { CapacitorConfig } from '@capacitor/cli';

// Bump this when making a native change that requires an APK rebuild.
// Format: MAJOR.MINOR.PATCH — Obtainium uses this to detect updates.
export const APP_VERSION = '1.1.0';

const config: CapacitorConfig = {
  appId: 'net.seq1.sessions',
  appName: 'SEQ1 Sessions',
  webDir: 'www',
  server: {
    // Server URL mode: loads the live web app rather than bundled assets.
    // This means web deploys update the app instantly — no APK rebuild needed.
    // Only rebuild the APK when native plugins or AndroidManifest change.
    url: 'https://sessions.seq1.net',
    cleartext: false,
    androidScheme: 'https',
  },
  android: {
    allowMixedContent: false,
    captureInput: true,
    webContentsDebuggingEnabled: false,
    // Prevent overscroll glow/bounce effect — this is an app, not a webpage.
    overScrollMode: 'never',
    // Cap 7 / Android 15: disable Capacitor's automatic margin-based inset handling.
    // sessions.seq1.net handles system bar insets via CSS env(safe-area-inset-*) — the
    // Capacitor margin approach and CSS env() are mutually exclusive; margins would zero
    // out the CSS values, breaking the .pb-safe recording bar layout.
    adjustMarginsForEdgeToEdge: 'disable',
  },
  plugins: {
    SplashScreen: {
      // Dark background matching stone-950 — no white flash while WebView loads.
      backgroundColor: '#0c0a09',
      // We call SplashScreen.hide() programmatically once content is ready.
      autoHide: false,
      // Instant show (app launch), smooth fade out when we hide.
      launchShowDuration: 0,
      splashFullScreen: true,
      splashImmersive: true,
    },
  },
};

export default config;
