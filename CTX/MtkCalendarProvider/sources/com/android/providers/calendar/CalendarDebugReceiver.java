package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CalendarDebugReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent2 = new Intent("android.intent.action.MAIN");
        intent2.setClass(context, CalendarDebug.class);
        intent2.setFlags(268435456);
        context.startActivity(intent2);
    }
}
