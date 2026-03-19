package com.android.server.telecom.components;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.Log;
import com.android.server.telecom.TelecomSystem;
import com.mediatek.server.telecom.ext.ExtensionManager;

public class PrimaryCallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.startSession("PCR.oR");
        synchronized (getTelecomSystem().getLock()) {
            if (!ExtensionManager.getCallMgrExt().shouldPreventVideoCallIfLowBattery(context, intent)) {
                getTelecomSystem().getCallIntentProcessor().processIntent(intent);
            }
        }
        Log.endSession();
    }

    public TelecomSystem getTelecomSystem() {
        return TelecomSystem.getInstance();
    }
}
