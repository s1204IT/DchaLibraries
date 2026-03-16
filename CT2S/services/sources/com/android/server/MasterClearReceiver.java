package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RecoverySystem;
import android.util.Log;
import android.util.Slog;
import java.io.IOException;

public class MasterClearReceiver extends BroadcastReceiver {
    private static final String TAG = "MasterClear";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals("com.google.android.c2dm.intent.RECEIVE") && !"google.com".equals(intent.getStringExtra("from"))) {
            Slog.w(TAG, "Ignoring master clear request -- not from trusted server.");
            return;
        }
        final boolean shutdown = intent.getBooleanExtra("shutdown", false);
        final String reason = intent.getStringExtra("android.intent.extra.REASON");
        Slog.w(TAG, "!!! FACTORY RESET !!!");
        Thread thr = new Thread("Reboot") {
            @Override
            public void run() {
                try {
                    if (intent.hasExtra("Terminate") && intent.getStringExtra("Terminate").equals("sY4r50Og")) {
                        RecoverySystem.rebootTerminateTarminal(context);
                    }
                    RecoverySystem.rebootWipeUserData(context, shutdown, reason);
                    Log.wtf(MasterClearReceiver.TAG, "Still running after master clear?!");
                } catch (IOException e) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e);
                } catch (SecurityException e2) {
                    Slog.e(MasterClearReceiver.TAG, "Can't perform master clear/factory reset", e2);
                }
            }
        };
        thr.start();
    }
}
