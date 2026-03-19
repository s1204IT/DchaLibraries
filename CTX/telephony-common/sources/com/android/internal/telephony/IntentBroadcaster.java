package com.android.internal.telephony;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IntentBroadcaster {
    private static final String TAG = "IntentBroadcaster";
    private static IntentBroadcaster sIntentBroadcaster;
    private Map<Integer, Intent> mRebroadcastIntents = new HashMap();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.USER_UNLOCKED")) {
                synchronized (IntentBroadcaster.this.mRebroadcastIntents) {
                    Iterator it = IntentBroadcaster.this.mRebroadcastIntents.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry entry = (Map.Entry) it.next();
                        Intent intent2 = (Intent) entry.getValue();
                        intent2.putExtra("rebroadcastOnUnlock", true);
                        it.remove();
                        IntentBroadcaster.this.logd("Rebroadcasting intent " + intent2.getAction() + " " + intent2.getStringExtra("ss") + " for slotId " + entry.getKey());
                        ActivityManager.broadcastStickyIntent(intent2, -1);
                    }
                }
            }
        }
    };

    private IntentBroadcaster(Context context) {
        context.registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
    }

    public static IntentBroadcaster getInstance(Context context) {
        if (sIntentBroadcaster == null) {
            sIntentBroadcaster = new IntentBroadcaster(context);
        }
        return sIntentBroadcaster;
    }

    public static IntentBroadcaster getInstance() {
        return sIntentBroadcaster;
    }

    public void broadcastStickyIntent(Intent intent, int i) {
        logd("Broadcasting and adding intent for rebroadcast: " + intent.getAction() + " " + intent.getStringExtra("ss") + " for slotId " + i);
        synchronized (this.mRebroadcastIntents) {
            ActivityManager.broadcastStickyIntent(intent, -1);
            this.mRebroadcastIntents.put(Integer.valueOf(i), intent);
        }
    }

    private void logd(String str) {
        Log.d(TAG, str);
    }
}
