package net.seq1.sessions;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebViewClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends BridgeActivity {

    private static final String OFFLINE_URL = "file:///android_asset/public/offline.html";

    /** Intent extra set by the static App Shortcuts in res/xml/shortcuts.xml. */
    private static final String EXTRA_SEQ1_ACTION = "seq1_action";

    /**
     * Shortcut actions the web app understands (admin-react app/sessions/page.tsx reads
     * ?action=). Anything else is ignored rather than forwarded, so an arbitrary intent
     * extra from another app cannot inject into the URL.
     */
    private static final Set<String> ALLOWED_ACTIONS = new HashSet<>(
        Arrays.asList("voice-launch", "new-session", "record-armed", "resume-session")
    );

    private static final String DEFAULT_SERVER_URL = "https://sessions.seq1.net";

    private boolean showingOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set bar colours before super.onCreate so they apply from the first frame.
        // styles.xml alone is overridden by Theme.SplashScreen; Window API is authoritative.
        getWindow().setStatusBarColor(Color.parseColor("#0c0a09"));
        getWindow().setNavigationBarColor(Color.parseColor("#0c0a09"));

        // Register Nostr/Amber bridge plugin before super.onCreate
        registerPlugin(NostrSignerPlugin.class);
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

        // Cold start from a home-screen shortcut. Skipped when the activity is being
        // recreated (savedInstanceState != null) so a system-driven recreate does not
        // re-fire a shortcut that was already actioned.
        if (savedInstanceState == null) {
            applyShortcutAction(getIntent());
        }
    }

    /**
     * Warm start: launchMode="singleTask" delivers a shortcut tapped while the app is
     * already running here instead of creating a second activity instance.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyShortcutAction(intent);
    }

    /**
     * If the intent carries a recognised seq1_action extra, navigate the WebView to
     * {server}/?action=<value>&t=<now>. The timestamp makes every tap a distinct URL, so
     * tapping the same shortcut twice in a row still re-triggers the web-side handler.
     * The extra is removed once consumed so it cannot fire twice for one tap.
     */
    private void applyShortcutAction(Intent intent) {
        if (intent == null || this.bridge == null) {
            return;
        }
        String action = intent.getStringExtra(EXTRA_SEQ1_ACTION);
        if (action == null || !ALLOWED_ACTIONS.contains(action)) {
            return;
        }
        intent.removeExtra(EXTRA_SEQ1_ACTION);

        String serverUrl = this.bridge.getServerUrl();
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = DEFAULT_SERVER_URL;
        }
        String url = Uri.parse(serverUrl).buildUpon()
            .path("/")
            .clearQuery()
            .appendQueryParameter("action", action)
            .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
            .build()
            .toString();

        this.bridge.getWebView().loadUrl(url);
    }
}
