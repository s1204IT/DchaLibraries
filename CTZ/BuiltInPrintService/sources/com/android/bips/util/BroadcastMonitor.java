package com.android.bips.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BroadcastMonitor extends BroadcastReceiver {
    Context mContext;
    BroadcastReceiver mReceiver;

    public BroadcastMonitor(Context context, BroadcastReceiver broadcastReceiver, String... strArr) {
        IntentFilter intentFilter = new IntentFilter();
        for (String str : strArr) {
            intentFilter.addAction(str);
        }
        this.mContext = context;
        this.mReceiver = broadcastReceiver;
        this.mContext.registerReceiver(this, intentFilter);
    }

    public void close() {
        if (this.mReceiver != null) {
            this.mReceiver = null;
            this.mContext.unregisterReceiver(this);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && this.mReceiver != null) {
            this.mReceiver.onReceive(context, intent);
        }
    }
}
