package com.android.server.telecom.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.server.telecom.TelecomSystem;

public final class TelecomBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        synchronized (getTelecomSystem().getLock()) {
            getTelecomSystem().getTelecomBroadcastIntentProcessor().processIntent(intent);
        }
    }

    public TelecomSystem getTelecomSystem() {
        return TelecomSystem.getInstance();
    }
}
