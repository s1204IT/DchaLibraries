package com.android.onetimeinitializer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OneTimeInitializerReceiver extends BroadcastReceiver {
    private static final String TAG = OneTimeInitializerReceiver.class.getSimpleName().substring(0, 22);

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "OneTimeInitializerReceiver.onReceive");
        context.startService(new Intent(context, (Class<?>) OneTimeInitializerService.class));
    }
}
