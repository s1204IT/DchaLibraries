package com.mediatek.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ClearDataReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.v("ClearDataReceiver", "action = " + intent.getAction());
        String stringExtra = intent.getStringExtra("packageName");
        if (stringExtra != null && stringExtra.equals(context.getPackageName())) {
            LogUtil.i("ClearDataReceiver", stringExtra + ": Calendar App data was cleared. clear the unread messages");
            MTKUtils.writeUnreadReminders(context, 0);
        }
    }
}
