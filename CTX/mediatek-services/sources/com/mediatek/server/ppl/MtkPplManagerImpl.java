package com.mediatek.server.ppl;

import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class MtkPplManagerImpl extends MtkPplManager {
    private static final String TAG = "MtkPplManager";
    private StatusBarManager mStatusBarManager;
    private boolean mPplStatus = false;
    private final BroadcastReceiver mPPLReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MtkPplManagerImpl.this.pplEnable(context, MtkPplManagerImpl.this.filterPplAction(intent.getAction()));
        }
    };

    public IntentFilter registerPplIntents() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.mediatek.ppl.NOTIFY_LOCK");
        intentFilter.addAction("com.mediatek.ppl.NOTIFY_UNLOCK");
        return intentFilter;
    }

    public int calculateStatusBarStatus(boolean z) {
        if (z) {
            return 983040;
        }
        return 0;
    }

    public boolean getPplLockStatus() {
        return this.mPplStatus;
    }

    public boolean filterPplAction(String str) {
        if (str.equals("com.mediatek.ppl.NOTIFY_LOCK")) {
            Log.d(TAG, "filterPplAction, recevier action = " + str);
            this.mPplStatus = true;
        } else if (str.equals("com.mediatek.ppl.NOTIFY_UNLOCK")) {
            Log.d(TAG, "filterPplAction, recevier action = " + str);
            this.mPplStatus = false;
        }
        return this.mPplStatus;
    }

    public void registerPplReceiver(Context context) {
        context.registerReceiver(this.mPPLReceiver, registerPplIntents());
    }

    private void pplEnable(Context context, boolean z) {
        int iCalculateStatusBarStatus = calculateStatusBarStatus(z);
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) context.getSystemService("statusbar");
        }
        this.mStatusBarManager.disable(iCalculateStatusBarStatus);
    }
}
