package net.seq1.sessions;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Register Nostr/Amber bridge plugin before super.onCreate
        registerPlugin(NostrSignerPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
