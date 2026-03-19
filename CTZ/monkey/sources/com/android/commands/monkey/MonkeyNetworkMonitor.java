package com.android.commands.monkey;

import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;

public class MonkeyNetworkMonitor extends IIntentReceiver.Stub {
    private static final boolean LDEBUG = false;
    private long mCollectionStartTime;
    private long mEventTime;
    private final IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    private int mLastNetworkType = -1;
    private long mWifiElapsedTime = 0;
    private long mMobileElapsedTime = 0;
    private long mElapsedTime = 0;

    public void performReceive(Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) throws RemoteException {
        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
        updateNetworkStats();
        if (NetworkInfo.State.CONNECTED == networkInfo.getState()) {
            this.mLastNetworkType = networkInfo.getType();
        } else if (NetworkInfo.State.DISCONNECTED == networkInfo.getState()) {
            this.mLastNetworkType = -1;
        }
        this.mEventTime = SystemClock.elapsedRealtime();
    }

    private void updateNetworkStats() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        long j = jElapsedRealtime - this.mEventTime;
        switch (this.mLastNetworkType) {
            case 0:
                this.mMobileElapsedTime += j;
                break;
            case 1:
                this.mWifiElapsedTime += j;
                break;
        }
        this.mElapsedTime = jElapsedRealtime - this.mCollectionStartTime;
    }

    public void start() {
        this.mWifiElapsedTime = 0L;
        this.mMobileElapsedTime = 0L;
        this.mElapsedTime = 0L;
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        this.mCollectionStartTime = jElapsedRealtime;
        this.mEventTime = jElapsedRealtime;
    }

    public void register(IActivityManager iActivityManager) throws RemoteException {
        iActivityManager.registerReceiver((IApplicationThread) null, (String) null, this, this.filter, (String) null, -1, 0);
    }

    public void unregister(IActivityManager iActivityManager) throws RemoteException {
        iActivityManager.unregisterReceiver(this);
    }

    public void stop() {
        updateNetworkStats();
    }

    public void dump() {
        Logger.out.println("## Network stats: elapsed time=" + this.mElapsedTime + "ms (" + this.mMobileElapsedTime + "ms mobile, " + this.mWifiElapsedTime + "ms wifi, " + ((this.mElapsedTime - this.mMobileElapsedTime) - this.mWifiElapsedTime) + "ms not connected)");
    }
}
