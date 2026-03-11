package com.mediatek.apn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OmacpApnReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("OmacpApnReceiver", "get action = " + action);
        if (context.getContentResolver() == null) {
            Log.e("OmacpApnReceiver", "FAILURE unable to get content resolver..");
        } else if ("com.mediatek.omacp.settings".equals(action)) {
            startOmacpService(context, intent);
        }
    }

    private void startOmacpService(Context context, Intent intent) {
        Intent intent2 = new Intent(context, (Class<?>) OmacpApnReceiverService.class);
        intent2.setAction("com.mediatek.apn.action.start.omacpservice");
        intent2.putExtra("android.intent.extra.INTENT", intent);
        Log.d("OmacpApnReceiver", "startService");
        context.startService(intent2);
    }
}
