package com.android.providers.calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CalendarDebugReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent("android.intent.action.MAIN");
        i.setClass(context, CalendarDebug.class);
        i.setFlags(268435456);
        context.startActivity(i);
    }
}
