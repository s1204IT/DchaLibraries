package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@VisibleForTesting
public class DockManager {
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private int mDockState = 0;
    private final DockBroadcastReceiver mReceiver = new DockBroadcastReceiver();

    @VisibleForTesting
    public interface Listener {
        void onDockChanged(boolean z);
    }

    private class DockBroadcastReceiver extends BroadcastReceiver {
        private DockBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.startSession("DM.oR");
            try {
                if ("android.intent.action.DOCK_EVENT".equals(intent.getAction())) {
                    DockManager.this.onDockChanged(intent.getIntExtra("android.intent.extra.DOCK_STATE", 0));
                }
            } finally {
                Log.endSession();
            }
        }
    }

    DockManager(Context context) {
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.DOCK_EVENT"));
    }

    @VisibleForTesting
    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    boolean isDocked() {
        switch (this.mDockState) {
            case 1:
            case CallState.SELECT_PHONE_ACCOUNT:
            case CallState.DIALING:
            case CallState.RINGING:
                return true;
            default:
                return false;
        }
    }

    private void onDockChanged(int i) {
        if (this.mDockState != i) {
            Object[] objArr = new Object[1];
            objArr[0] = Boolean.valueOf(i == 2);
            Log.v(this, "onDockChanged: is docked?%b", objArr);
            this.mDockState = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onDockChanged(isDocked());
            }
        }
    }
}
