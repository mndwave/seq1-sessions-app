package net.seq1.sessions;

import android.view.KeyEvent;
import android.view.View;

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

        getBridge()
            .getWebView()
            .setOnKeyListener(
                new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                            // Ignore auto-repeat while held — the web side tracks hold state
                            // itself from one down + one up, not a stream of repeated downs.
                            if (event.getRepeatCount() == 0) {
                                JSObject ret = new JSObject();
                                ret.put("direction", keyCode == KeyEvent.KEYCODE_VOLUME_UP ? "up" : "down");
                                ret.put("phase", event.getAction() == KeyEvent.ACTION_DOWN ? "down" : "up");
                                call.resolve(ret);
                            }
                            return suppressVolumeIndicator;
                        }
                        return false;
                    }
                }
            );

        isStarted = true;
    }

    @PluginMethod(returnType = PluginMethod.RETURN_PROMISE)
    public void clearWatch(final PluginCall call) {
        if (!isStarted) {
            call.resolve();
            return;
        }

        getBridge().getWebView().setOnKeyListener(null);
        if (savedCall != null) {
            getBridge().releaseCall(savedCall);
            savedCall = null;
        }
        isStarted = false;

        call.resolve();
    }
}
