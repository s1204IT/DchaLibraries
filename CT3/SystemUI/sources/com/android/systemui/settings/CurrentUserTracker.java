package com.android.systemui.settings;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public abstract class CurrentUserTracker extends BroadcastReceiver {
    private Context mContext;
    private int mCurrentUserId;

    public abstract void onUserSwitched(int i);

    public CurrentUserTracker(Context context) {
        this.mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"android.intent.action.USER_SWITCHED".equals(intent.getAction())) {
            return;
        }
        int oldUserId = this.mCurrentUserId;
        this.mCurrentUserId = intent.getIntExtra("android.intent.extra.user_handle", 0);
        if (oldUserId == this.mCurrentUserId) {
            return;
        }
        onUserSwitched(this.mCurrentUserId);
    }

    public void startTracking() {
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        IntentFilter filter = new IntentFilter("android.intent.action.USER_SWITCHED");
        this.mContext.registerReceiver(this, filter);
    }

    public void stopTracking() {
        this.mContext.unregisterReceiver(this);
    }
}
