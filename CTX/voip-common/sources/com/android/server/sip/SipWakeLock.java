package com.android.server.sip;

import android.os.PowerManager;
import android.telephony.Rlog;
import java.util.HashSet;

class SipWakeLock {
    private static final boolean DBG = false;
    private static final String TAG = "SipWakeLock";
    private HashSet<Object> mHolders = new HashSet<>();
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mTimerWakeLock;
    private PowerManager.WakeLock mWakeLock;

    SipWakeLock(PowerManager powerManager) {
        this.mPowerManager = powerManager;
    }

    synchronized void reset() {
        this.mHolders.clear();
        release(null);
    }

    synchronized void acquire(long j) {
        if (this.mTimerWakeLock == null) {
            this.mTimerWakeLock = this.mPowerManager.newWakeLock(1, "SipWakeLock.timer");
            this.mTimerWakeLock.setReferenceCounted(true);
        }
        this.mTimerWakeLock.acquire(j);
    }

    synchronized void acquire(Object obj) {
        this.mHolders.add(obj);
        if (this.mWakeLock == null) {
            this.mWakeLock = this.mPowerManager.newWakeLock(1, TAG);
        }
        if (!this.mWakeLock.isHeld()) {
            this.mWakeLock.acquire();
        }
    }

    synchronized void release(Object obj) {
        this.mHolders.remove(obj);
        if (this.mWakeLock != null && this.mHolders.isEmpty() && this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }
}
