package net.seq1.sessions;

import android.view.KeyEvent;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Reports hardware Volume Up/Down press AND release as distinct events — the public
 * @capacitor-community/volume-buttons plugin only resolves its JS callback on ACTION_UP
 * (confirmed by reading its Java source: it computes isKeyUp and gates call.resolve() on it),
 * so it cannot support hold-to-record or double-tap-to-latch gestures, which both need to know
 * when the button actually went down, not just that a press-cycle completed. This plugin exposes
 * both phases directly so the web side (lib/use-hardware-volume-record.ts) can build those
 * gestures itself.
 *
 * VOLUME-BUTTON-STILL-JUST-VOLUME-2026-08-27 (Kyle dictation, day-of): the original
 * implementation attached a View.OnKeyListener to getBridge().getWebView() to intercept
 * KEYCODE_VOLUME_*. On-device that never claimed the event — a Chromium WebView's own input
 * pipeline handles hardware keys before a plain View.OnKeyListener reliably sees them, so the
 * press fell through to Android's default volume-adjust behaviour every time (the exact symptom
 * reported: "all it's doing is controlling the volume"). This was never verified live before
 * shipping (no device attached to the build host — see session-learnings-2026-08-27-claude3).
 * Fix: interception now happens in MainActivity.dispatchKeyEvent() — the standard Android
 * technique for claiming hardware volume keys (e.g. camera-shutter-via-volume apps) — which runs
 * before the event is routed into the view hierarchy at all, so it can no longer race the
 * WebView's own handling. handleKeyEvent() below is called from there; this plugin no longer
 * touches the WebView's key listener.
 */
@CapacitorPlugin(name = "HardwareVolumeButtons")
public class HardwareVolumeButtonsPlugin extends Plugin {

    private PluginCall savedCall;
    private boolean isStarted = false;
    private boolean suppressVolumeIndicator = false;

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void watchVolume(final PluginCall call) {
        if (isStarted) {
            call.reject("Volume buttons already watched");
            return;
        }

        suppressVolumeIndicator = Boolean.TRUE.equals(call.getBoolean("suppressVolumeIndicator", true));
        call.setKeepAlive(true);
        savedCall = call;
        isStarted = true;
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void clearWatch(final PluginCall call) {
        if (!isStarted) {
            call.resolve();
            return;
        }

        if (savedCall != null) {
            getBridge().releaseCall(savedCall);
            savedCall = null;
        }
        isStarted = false;

        call.resolve();
    }

    /**
     * Called from MainActivity.dispatchKeyEvent() for every VOLUME_UP/VOLUME_DOWN event, ahead
     * of Android's default volume-adjust handling. Returns true when the event was consumed
     * (a watch is active and this isn't an auto-repeat) so the Activity knows to suppress the
     * system default; false lets it fall through to normal volume behaviour (e.g. no JS-side
     * watch registered yet, or watch was cleared).
     */
    public boolean handleKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP && keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            return false;
        }
        if (!isStarted || savedCall == null) {
            return false;
        }
        // Ignore auto-repeat while held — the web side tracks hold state itself from one
        // down + one up, not a stream of repeated downs.
        if (event.getRepeatCount() == 0) {
            JSObject ret = new JSObject();
            ret.put("direction", keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "up" : "down");
            ret.put("phase", event.getAction() == KeyEvent.ACTION_DOWN ? "down" : "up");
            savedCall.resolve(ret);
        }
        return suppressVolumeIndicator;
    }
}
