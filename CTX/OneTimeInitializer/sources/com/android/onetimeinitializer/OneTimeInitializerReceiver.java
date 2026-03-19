package com.android.onetimeinitializer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OneTimeInitializerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("OneTimeInitializerReceiver", "OneTimeInitializerReceiver.onReceive");
        final BroadcastReceiver.PendingResult pendingResultGoAsync = goAsync();
        new Thread(new Runnable() {
            @Override
            public final void run() {
                OneTimeInitializerReceiver.lambda$onReceive$0(context, pendingResultGoAsync);
            }
        }).start();
    }

    static void lambda$onReceive$0(Context context, BroadcastReceiver.PendingResult pendingResult) {
        try {
            new OneTimeInitializer(context).initialize();
        } finally {
            pendingResult.finish();
        }
    }
}
