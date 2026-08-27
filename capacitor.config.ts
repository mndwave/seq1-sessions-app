import { CapacitorConfig } from '@capacitor/cli';

// Bump this when making a native change that requires an APK rebuild.
// Format: MAJOR.MINOR.PATCH — Obtainium uses this to detect updates.
export const APP_VERSION = '2.7.3';

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
    // captureInput was on for the in-app record button, but it swaps in Capacitor's
    // simplified InputConnection (built for discrete JS key events), which drops the
    // bulk commitText() calls third-party IME voice-typing (e.g. FUTO Keyboard STT)
    // uses to insert a whole transcribed phrase at once — so external voice dictation
    // silently failed to land in the message textarea. Off restores the standard
    // InputConnection; the in-app record button doesn't depend on this flag.
    captureInput: false,
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
    LocalNotifications: {
      smallIcon: 'ic_stat_icon_config_sample',
      iconColor: '#14b8a6',
      sound: 'beep.wav',
      channels: [
        {
          id: 'voice-delivery',
          name: 'Voice Transcription',
          description: 'Delivered when a voice recording has been transcribed',
          importance: 4,
          visibility: 1,
          vibration: true,
        },
      ],
    },
    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },
  },
};

export default config;
