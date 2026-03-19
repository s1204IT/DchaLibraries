package com.mediatek.providers.calendar.packagedataclear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import com.mediatek.providers.calendar.LogUtil;

public class StorageClearReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String stringExtra = intent.getStringExtra("packageName");
        if (stringExtra != null && stringExtra.equals(context.getPackageName())) {
            broadStorageCleared(context);
        }
    }

    private void broadStorageCleared(Context context) {
        LogUtil.d("StorageClearReceiver", "CalendarProvider package data was cleared...");
        context.sendBroadcast(new Intent("android.intent.action.PROVIDER_CHANGED", CalendarContract.CONTENT_URI), null);
    }
}
