package com.android.server.connectivity.tethering;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.util.VersionedBroadcastListener;
import android.os.Handler;
import android.util.Log;
import java.util.function.Consumer;

public class SimChangeListener extends VersionedBroadcastListener {
    private static final boolean DBG = false;
    private static final String TAG = SimChangeListener.class.getSimpleName();

    public SimChangeListener(Context context, Handler handler, Runnable runnable) {
        super(TAG, context, handler, makeIntentFilter(), makeCallback(runnable));
    }

    private static IntentFilter makeIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        return intentFilter;
    }

    private static Consumer<Intent> makeCallback(final Runnable runnable) {
        return new Consumer<Intent>() {
            private boolean mSimNotLoadedSeen = false;

            @Override
            public void accept(Intent intent) {
                String stringExtra = intent.getStringExtra("ss");
                Log.d(SimChangeListener.TAG, "got Sim changed to state " + stringExtra + ", mSimNotLoadedSeen=" + this.mSimNotLoadedSeen);
                if (!"LOADED".equals(stringExtra)) {
                    this.mSimNotLoadedSeen = true;
                } else if (this.mSimNotLoadedSeen) {
                    this.mSimNotLoadedSeen = false;
                    runnable.run();
                }
            }
        };
    }
}
