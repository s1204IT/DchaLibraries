package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;

public class BrickReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Slog.w("BrickReceiver", "!!! BRICKING DEVICE !!!");
        android.os.SystemService.start("brick");
    }
}
