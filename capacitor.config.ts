import { CapacitorConfig } from '@capacitor/cli';

// Bump this when making a native change that requires an APK rebuild.
// Format: MAJOR.MINOR.PATCH — Obtainium uses this to detect updates.
export const APP_VERSION = '1.0.2';

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
    // Allow mixed content is off — sessions.seq1.net is HTTPS only.
    allowMixedContent: false,
    // Capture input is needed for the voice recording UI.
    captureInput: true,
    // WebContentsDebuggingEnabled makes Chrome DevTools attach to the WebView.
    // Set to false in production builds via GitHub Actions env var.
    webContentsDebuggingEnabled: true,
  },
  plugins: {
    // Filesystem plugin: used for buffering voice chunks locally.
    Filesystem: {
      // Store voice chunks in the app's private cache directory.
      // Survives app backgrounding; cleared when user uninstalls.
    },
  },
};

export default config;
