package com.mediatek.stk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Slog;
import com.mediatek.am.AMEventHookData;

public class IdleScreen {
    private static final boolean DEBUG = false;
    private static final String TAG = "IdleScreen";
    private static boolean mNotifyNeeded = false;

    public void onSystemReady(AMEventHookData.SystemReady data) {
        int phase = data.getInt(AMEventHookData.SystemReady.Index.phase);
        if (phase != 0) {
            return;
        }
        Context context = (Context) data.get(AMEventHookData.SystemReady.Index.context);
        registerIdleScreenReceiver(context);
    }

    public void onEndOfActivityIdle(AMEventHookData.EndOfActivityIdle data) {
        Context context = (Context) data.get(AMEventHookData.EndOfActivityIdle.Index.context);
        Intent idleIntent = (Intent) data.get(AMEventHookData.EndOfActivityIdle.Index.intent);
        activityIdleScreen(context, idleIntent);
    }

    private void registerIdleScreenReceiver(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.IDLE_SCREEN_NEEDED");
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (!"android.intent.action.IDLE_SCREEN_NEEDED".equals(intent.getAction())) {
                    return;
                }
                boolean unused = IdleScreen.mNotifyNeeded = intent.getBooleanExtra("_enable", false);
                Slog.v(IdleScreen.TAG, "mNotifyNeeded = " + IdleScreen.mNotifyNeeded);
            }
        }, filter);
    }

    private void activityIdleScreen(Context context, Intent idleIntent) {
        if (!mNotifyNeeded || idleIntent == null || !idleIntent.hasCategory("android.intent.category.HOME")) {
            return;
        }
        Slog.v(TAG, "In IDLE SCREEN, broadcast intent to receivers");
        Intent intent = new Intent("android.intent.action.stk.IDLE_SCREEN_AVAILABLE");
        context.sendBroadcast(intent);
    }
}
