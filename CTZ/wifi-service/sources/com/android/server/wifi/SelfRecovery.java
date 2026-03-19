package com.android.server.wifi;

import android.os.SystemProperties;
import android.util.Log;
import java.util.Iterator;
import java.util.LinkedList;

public class SelfRecovery {
    public static final long MAX_RESTARTS_IN_TIME_WINDOW = 2;
    public static final long MAX_RESTARTS_TIME_WINDOW_MILLIS = 3600000;
    public static final int REASON_LAST_RESORT_WATCHDOG = 0;
    public static final int REASON_STA_IFACE_DOWN = 2;
    protected static final String[] REASON_STRINGS = {"Last Resort Watchdog", "WifiNative Failure", "Sta Interface Down"};
    public static final int REASON_WIFINATIVE_FAILURE = 1;
    public static final long RESTART_WIFI_TIME = 13000;
    private static final String TAG = "WifiSelfRecovery";
    private final Clock mClock;
    private final LinkedList<Long> mPastRestartTimes = new LinkedList<>();
    private final WifiController mWifiController;

    public SelfRecovery(WifiController wifiController, Clock clock) {
        this.mWifiController = wifiController;
        this.mClock = clock;
    }

    public void trigger(int i) {
        if (i == 0 || i == 1 || i == 2) {
            if (i == 2) {
                Log.e(TAG, "STA interface down, disable wifi");
                if (SystemProperties.get("ro.vendor.wlan.gen").equals("gen4_mt7663") || SystemProperties.get("ro.vendor.wlan.gen").equals("gen4_mt7668")) {
                    this.mWifiController.sendMessageDelayed(155665, i, RESTART_WIFI_TIME);
                    return;
                } else {
                    this.mWifiController.sendMessageDelayed(155665, i);
                    return;
                }
            }
            Log.e(TAG, "Triggering recovery for reason: " + REASON_STRINGS[i]);
            if (i == 1) {
                trimPastRestartTimes();
                if (this.mPastRestartTimes.size() >= 2) {
                    Log.e(TAG, "Already restarted wifi (2) times in last (3600000ms ). Disabling wifi");
                    this.mWifiController.sendMessage(155667);
                    return;
                }
                this.mPastRestartTimes.add(Long.valueOf(this.mClock.getElapsedSinceBootMillis()));
            }
            this.mWifiController.sendMessage(155665, i);
            return;
        }
        Log.e(TAG, "Invalid trigger reason. Ignoring...");
    }

    private void trimPastRestartTimes() {
        Iterator<Long> it = this.mPastRestartTimes.iterator();
        long elapsedSinceBootMillis = this.mClock.getElapsedSinceBootMillis();
        while (it.hasNext() && elapsedSinceBootMillis - it.next().longValue() > 3600000) {
            it.remove();
        }
    }
}
