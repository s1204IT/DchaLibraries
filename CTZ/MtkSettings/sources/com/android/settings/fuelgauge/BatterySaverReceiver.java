package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class BatterySaverReceiver extends BroadcastReceiver {
    private BatterySaverListener mBatterySaverListener;
    private Context mContext;
    private boolean mRegistered;

    public interface BatterySaverListener {
        void onBatteryChanged(boolean z);

        void onPowerSaveModeChanged();
    }

    public BatterySaverReceiver(Context context) {
        this.mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("android.os.action.POWER_SAVE_MODE_CHANGING".equals(action)) {
            if (this.mBatterySaverListener != null) {
                this.mBatterySaverListener.onPowerSaveModeChanged();
            }
        } else if ("android.intent.action.BATTERY_CHANGED".equals(action) && this.mBatterySaverListener != null) {
            this.mBatterySaverListener.onBatteryChanged(intent.getIntExtra("plugged", 0) != 0);
        }
    }

    public void setListening(boolean z) {
        if (!z || this.mRegistered) {
            if (!z && this.mRegistered) {
                this.mContext.unregisterReceiver(this);
                this.mRegistered = false;
                return;
            }
            return;
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.os.action.POWER_SAVE_MODE_CHANGING");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        this.mContext.registerReceiver(this, intentFilter);
        this.mRegistered = true;
    }

    public void setBatterySaverListener(BatterySaverListener batterySaverListener) {
        this.mBatterySaverListener = batterySaverListener;
    }
}
