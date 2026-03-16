package com.marvell.powergadget.eventrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.powerhint.PowerHintManager;
import android.util.Log;
import java.util.HashMap;

public class SystemIntentReceiver extends BroadcastReceiver {
    private HashMap<String, String> mHintMap = new HashMap<String, String>() {
        {
            put("android.intent.action.BOOT_COMPLETED", "boot");
            put("android.intent.action.PHONE_STATE", "phone");
            put("android.intent.action.SCREEN_ON", "screen");
            put("android.intent.action.SCREEN_OFF", "screen");
            put("com.marvell.fmradio.ENABLE", "fm");
            put("com.marvell.fmradio.DISABLE", "fm");
        }
    };

    public SystemIntentReceiver() {
        sendToPowerHint(new Intent("android.intent.action.BOOT_COMPLETED"));
    }

    public void sendToPowerHint(Intent intent) {
        PowerHintManager phm = new PowerHintManager();
        String action = intent.getAction();
        String hint = this.mHintMap.get(action);
        if (hint == null) {
            Log.w("EventRelay", "SystemIntentReceiver: No such intent: " + action);
            return;
        }
        if (action.equals("android.intent.action.PHONE_STATE")) {
            String extra = intent.getStringExtra("state");
            phm.sendPowerHint(hint, extra);
            Log.d("EventRelay", "SystemIntentReceiver: New Hint: " + hint + " " + extra);
        } else {
            phm.sendPowerHint(hint, action);
            Log.d("EventRelay", "SystemIntentReceiver: New Hint: " + hint + " " + action);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sendToPowerHint(intent);
    }
}
