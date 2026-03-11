package com.android.settings.datetime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class TimeChangeListenerMixin extends BroadcastReceiver implements LifecycleObserver, OnPause, OnResume {
    private final UpdateTimeAndDateCallback mCallback;
    private final Context mContext;

    public TimeChangeListenerMixin(Context context, UpdateTimeAndDateCallback updateTimeAndDateCallback) {
        this.mContext = context;
        this.mCallback = updateTimeAndDateCallback;
    }

    @Override
    public void onResume() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        this.mContext.registerReceiver(this, intentFilter, null, null);
    }

    @Override
    public void onPause() {
        this.mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (this.mCallback != null) {
            this.mCallback.updateTimeAndDateDisplay(this.mContext);
        }
    }
}
