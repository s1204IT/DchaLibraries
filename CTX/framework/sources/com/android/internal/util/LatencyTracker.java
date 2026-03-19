package com.android.internal.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.EventLog;
import android.util.Log;
import android.util.SparseLongArray;

public class LatencyTracker {
    public static final int ACTION_CHECK_CREDENTIAL = 3;
    public static final int ACTION_CHECK_CREDENTIAL_UNLOCKED = 4;
    public static final int ACTION_EXPAND_PANEL = 0;
    public static final int ACTION_FINGERPRINT_WAKE_AND_UNLOCK = 2;
    private static final String ACTION_RELOAD_PROPERTY = "com.android.systemui.RELOAD_LATENCY_TRACKER_PROPERTY";
    public static final int ACTION_ROTATE_SCREEN = 6;
    public static final int ACTION_TOGGLE_RECENTS = 1;
    public static final int ACTION_TURN_ON_SCREEN = 5;
    private static final String[] NAMES = {"expand panel", "toggle recents", "fingerprint wake-and-unlock", "check credential", "check credential unlocked", "turn on screen", "rotate the screen"};
    private static final String TAG = "LatencyTracker";
    private static LatencyTracker sLatencyTracker;
    private boolean mEnabled;
    private final SparseLongArray mStartRtc = new SparseLongArray();

    public static LatencyTracker getInstance(Context context) {
        if (sLatencyTracker == null) {
            sLatencyTracker = new LatencyTracker(context);
        }
        return sLatencyTracker;
    }

    private LatencyTracker(Context context) {
        context.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                LatencyTracker.this.reloadProperty();
            }
        }, new IntentFilter(ACTION_RELOAD_PROPERTY));
        reloadProperty();
    }

    private void reloadProperty() {
        this.mEnabled = SystemProperties.getBoolean("debug.systemui.latency_tracking", false);
    }

    public static boolean isEnabled(Context context) {
        return Build.IS_DEBUGGABLE && getInstance(context).mEnabled;
    }

    public void onActionStart(int i) {
        if (!this.mEnabled) {
            return;
        }
        Trace.asyncTraceBegin(4096L, NAMES[i], 0);
        this.mStartRtc.put(i, SystemClock.elapsedRealtime());
    }

    public void onActionEnd(int i) {
        if (!this.mEnabled) {
            return;
        }
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = this.mStartRtc.get(i, -1L);
        if (j == -1) {
            return;
        }
        this.mStartRtc.delete(i);
        Trace.asyncTraceEnd(4096L, NAMES[i], 0);
        logAction(i, (int) (jElapsedRealtime - j));
    }

    public static void logAction(int i, int i2) {
        Log.i(TAG, "action=" + i + " latency=" + i2);
        EventLog.writeEvent(36070, Integer.valueOf(i), Integer.valueOf(i2));
    }
}
