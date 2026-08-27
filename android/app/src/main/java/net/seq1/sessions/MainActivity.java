package net.seq1.sessions;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {

    private static final String OFFLINE_URL = "file:///android_asset/public/offline.html";

    private boolean showingOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set bar colours before super.onCreate so they apply from the first frame.
        // styles.xml alone is overridden by Theme.SplashScreen; Window API is authoritative.
        getWindow().setStatusBarColor(Color.parseColor("#0c0a09"));
        getWindow().setNavigationBarColor(Color.parseColor("#0c0a09"));

        // Register Nostr/Amber bridge plugin before super.onCreate
        registerPlugin(NostrSignerPlugin.class);
        registerPlugin(HardwareVolumeButtonsPlugin.class);
        super.onCreate(savedInstanceState);

        // Replace the WebViewClient with one that intercepts main-frame load
        // failures (e.g. no internet) and shows our styled offline page
        // instead of the default white-with-green-robot Android error screen.
        WebView webView = this.bridge.getWebView();
        webView.setWebViewClient(new BridgeWebViewClient(this.bridge) {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!url.startsWith("file:///android_asset/public/offline.html")) {
                    showingOffline = false;
                }
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame() && !showingOffline) {
                    showingOffline = true;
                    view.stopLoading();
                    view.loadUrl(OFFLINE_URL);
                    return;
                }
                super.onReceivedError(view, request, error);
            }
        });
    }

    // VOLUME-BUTTON-STILL-JUST-VOLUME-2026-08-27: claim VOLUME_UP/VOLUME_DOWN here, ahead of the
    // view hierarchy entirely, rather than via a WebView.OnKeyListener (see
    // HardwareVolumeButtonsPlugin's class doc for why that didn't work on-device). This is the
    // standard Android technique for apps that repurpose hardware volume keys.
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            PluginHandle handle = getBridge().getPlugin("HardwareVolumeButtons");
            if (handle != null) {
                Plugin instance = handle.getInstance();
                if (instance instanceof HardwareVolumeButtonsPlugin
                        && ((HardwareVolumeButtonsPlugin) instance).handleKeyEvent(event)) {
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
