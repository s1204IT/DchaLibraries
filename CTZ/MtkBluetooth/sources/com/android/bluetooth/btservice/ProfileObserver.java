package com.android.bluetooth.btservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

public class ProfileObserver extends ContentObserver {
    private Context mContext;
    private AdapterService mService;
    private AdapterStateObserver mStateObserver;

    public ProfileObserver(Context context, AdapterService adapterService, Handler handler) {
        super(handler);
        this.mContext = context;
        this.mService = adapterService;
        this.mStateObserver = new AdapterStateObserver(this);
    }

    public void start() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("bluetooth_disabled_profiles"), false, this);
    }

    private void onBluetoothOff() {
        this.mContext.unregisterReceiver(this.mStateObserver);
        Config.init(this.mContext);
        this.mService.enable();
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this);
    }

    @Override
    public void onChange(boolean z) {
        if (this.mService.isEnabled()) {
            this.mContext.registerReceiver(this.mStateObserver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
            this.mService.disable();
        }
    }

    private static class AdapterStateObserver extends BroadcastReceiver {
        private ProfileObserver mProfileObserver;

        AdapterStateObserver(ProfileObserver profileObserver) {
            this.mProfileObserver = profileObserver;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(intent.getAction()) && intent.getIntExtra("android.bluetooth.adapter.extra.STATE", -1) == 10) {
                this.mProfileObserver.onBluetoothOff();
            }
        }
    }
}
