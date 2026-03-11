package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.settings.Settings;

public class TestingSettingsBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals("android.provider.Telephony.SECRET_CODE")) {
            return;
        }
        Intent i = new Intent("android.intent.action.MAIN");
        i.setClass(context, Settings.TestingSettingsActivity.class);
        i.setFlags(268435456);
        context.startActivity(i);
    }
}
