import { CapacitorConfig } from '@capacitor/cli';

// Bump this when making a native change that requires an APK rebuild.
// Format: MAJOR.MINOR.PATCH — Obtainium uses this to detect updates.
export const APP_VERSION = '2.3.0';

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
    // Cap 8: edge-to-edge is always enabled. sessions.seq1.net handles system bar insets
    // via CSS env(safe-area-inset-*) which Capacitor 8 correctly passes through.
    // adjustMarginsForEdgeToEdge removed from Cap 8 API — CSS env() approach is correct.
  },
  plugins: {
    SplashScreen: {
      // Disabled: launchShowDuration 0 means the native splash is never shown.
      // The AppLoader in sessions.seq1.net handles the entire loading animation
      // in the web layer — no native/web handoff, no immersive bar flash.
      // backgroundColor still sets the WebView background colour so there is no
      // white flash while the WebView initialises before the first paint.
      backgroundColor: '#0c0a09',
      launchShowDuration: 0,
      autoHide: true,
    },
  },
};

export default config;
