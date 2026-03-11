package com.android.launcher3.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Process;
import android.util.Log;
import com.android.launcher3.Utilities;

public class ConfigMonitor extends BroadcastReceiver {
    private final Context mContext;
    private final int mDensity;
    private final float mFontScale;

    public ConfigMonitor(Context context) {
        this.mContext = context;
        Configuration config = context.getResources().getConfiguration();
        this.mFontScale = config.fontScale;
        this.mDensity = getDensity(config);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Configuration config = context.getResources().getConfiguration();
        if (this.mFontScale == config.fontScale && this.mDensity == getDensity(config)) {
            return;
        }
        Log.d("ConfigMonitor", "Configuration changed, restarting launcher");
        this.mContext.unregisterReceiver(this);
        Process.killProcess(Process.myPid());
    }

    public void register() {
        this.mContext.registerReceiver(this, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    private static int getDensity(Configuration config) {
        if (Utilities.ATLEAST_JB_MR1) {
            return config.densityDpi;
        }
        return 0;
    }
}
