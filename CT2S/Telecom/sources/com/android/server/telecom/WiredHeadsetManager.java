package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import com.android.internal.util.IndentingPrintWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class WiredHeadsetManager {
    private boolean mIsPluggedIn;
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private final WiredHeadsetBroadcastReceiver mReceiver = new WiredHeadsetBroadcastReceiver();

    interface Listener {
        void onWiredHeadsetPluggedInChanged(boolean z, boolean z2);
    }

    private class WiredHeadsetBroadcastReceiver extends BroadcastReceiver {
        private WiredHeadsetBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.HEADSET_PLUG")) {
                boolean z = intent.getIntExtra("state", 0) == 1;
                Log.v(WiredHeadsetManager.this, "ACTION_HEADSET_PLUG event, plugged in: %b", Boolean.valueOf(z));
                WiredHeadsetManager.this.onHeadsetPluggedInChanged(z);
            }
        }
    }

    WiredHeadsetManager(Context context) {
        this.mIsPluggedIn = ((AudioManager) context.getSystemService("audio")).isWiredHeadsetOn();
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.HEADSET_PLUG"));
    }

    void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    boolean isPluggedIn() {
        return this.mIsPluggedIn;
    }

    private void onHeadsetPluggedInChanged(boolean z) {
        if (this.mIsPluggedIn != z) {
            Log.v(this, "onHeadsetPluggedInChanged, mIsPluggedIn: %b -> %b", Boolean.valueOf(this.mIsPluggedIn), Boolean.valueOf(z));
            boolean z2 = this.mIsPluggedIn;
            this.mIsPluggedIn = z;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onWiredHeadsetPluggedInChanged(z2, this.mIsPluggedIn);
            }
        }
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        indentingPrintWriter.println("mIsPluggedIn: " + this.mIsPluggedIn);
    }
}
