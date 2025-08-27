package com.android.settings.bluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;

/* loaded from: classes.dex */
public class AlwaysDiscoverable extends BroadcastReceiver {
    private Context mContext;
    private IntentFilter mIntentFilter = new IntentFilter();
    private LocalBluetoothAdapter mLocalAdapter;
    boolean mStarted;

    public AlwaysDiscoverable(Context context, LocalBluetoothAdapter localBluetoothAdapter) {
        this.mContext = context;
        this.mLocalAdapter = localBluetoothAdapter;
        this.mIntentFilter.addAction("android.bluetooth.adapter.action.SCAN_MODE_CHANGED");
    }

    public void start() {
        if (this.mStarted) {
            return;
        }
        this.mContext.registerReceiver(this, this.mIntentFilter);
        this.mStarted = true;
        if (this.mLocalAdapter.getScanMode() != 23) {
            this.mLocalAdapter.setScanMode(23);
        }
    }

    public void stop() {
        if (!this.mStarted) {
            return;
        }
        this.mContext.unregisterReceiver(this);
        this.mStarted = false;
        this.mLocalAdapter.setScanMode(21);
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == "android.bluetooth.adapter.action.SCAN_MODE_CHANGED" && this.mLocalAdapter.getScanMode() != 23) {
            this.mLocalAdapter.setScanMode(23);
        }
    }
}
