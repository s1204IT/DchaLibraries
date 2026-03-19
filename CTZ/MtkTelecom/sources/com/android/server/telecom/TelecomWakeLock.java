package com.android.server.telecom;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telecom.Log;

public class TelecomWakeLock {
    private Context mContext;
    private PowerManager mPowerManager;
    private WakeLockAdapter mWakeLock = getWakeLockFromPowerManager();
    private int mWakeLockLevel;
    private String mWakeLockTag;

    public class WakeLockAdapter {
        private PowerManager.WakeLock mWakeLock;

        public WakeLockAdapter(PowerManager.WakeLock wakeLock) {
            this.mWakeLock = wakeLock;
        }

        public void acquire() {
            this.mWakeLock.acquire();
        }

        public boolean isHeld() {
            return this.mWakeLock.isHeld();
        }

        public void release(int i) {
            this.mWakeLock.release(i);
        }

        public void setReferenceCounted(boolean z) {
            this.mWakeLock.setReferenceCounted(z);
        }
    }

    public TelecomWakeLock(Context context, int i, String str) {
        this.mContext = context;
        this.mWakeLockLevel = i;
        this.mWakeLockTag = str;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
    }

    private WakeLockAdapter getWakeLockFromPowerManager() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        if (powerManager.isWakeLockLevelSupported(this.mWakeLockLevel)) {
            return new WakeLockAdapter(powerManager.newWakeLock(this.mWakeLockLevel, this.mWakeLockTag));
        }
        return null;
    }

    public boolean isHeld() {
        return this.mWakeLock != null && this.mWakeLock.isHeld();
    }

    public void acquire() {
        if (this.mWakeLock == null) {
            Log.i("TelecomWakeLock", "Can not acquire WakeLock (not supported) with level: " + this.mWakeLockLevel, new Object[0]);
            return;
        }
        if (!isHeld()) {
            this.mWakeLock.acquire();
            Log.i("TelecomWakeLock", "Acquiring WakeLock with id: " + this.mWakeLockLevel, new Object[0]);
            return;
        }
        Log.i("TelecomWakeLock", "WakeLock already acquired for id: " + this.mWakeLockLevel, new Object[0]);
    }

    public void release(int i) {
        if (this.mWakeLock == null) {
            Log.i("TelecomWakeLock", "Can not release WakeLock (not supported) with id: " + this.mWakeLockLevel, new Object[0]);
            return;
        }
        if (isHeld()) {
            this.mWakeLock.release(i);
            Log.i("TelecomWakeLock", "Releasing WakeLock with id: " + this.mWakeLockLevel, new Object[0]);
            return;
        }
        Log.i("TelecomWakeLock", "WakeLock already released with id: " + this.mWakeLockLevel, new Object[0]);
    }

    public void setReferenceCounted(boolean z) {
        if (this.mWakeLock == null) {
            return;
        }
        this.mWakeLock.setReferenceCounted(z);
    }

    public String toString() {
        if (this.mWakeLock != null) {
            return this.mWakeLock.toString();
        }
        return "null";
    }

    public void wakeUpScreen() {
        if (!isHeld()) {
            Log.d("TelecomWakeLock", "wake up screen", new Object[0]);
            this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
        }
    }
}
