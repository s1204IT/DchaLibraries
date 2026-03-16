package com.marvell.powergadget.eventrelay;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.powerhint.PowerHintManager;
import android.util.Log;
import java.util.HashMap;

public class SsgIntentReceiver extends BroadcastReceiver {
    private HashMap<String, String> mHintMap = new HashMap<String, String>() {
        {
            put("com.sec.android.intent.action.CPU_BOOSTER_MIN", "MIN");
            put("com.sec.android.intent.action.CPU_BOOSTER_MAX", "MAX");
            put("com.sec.android.intent.action.GPU_BOOSTER_MIN", "MIN");
            put("com.sec.android.intent.action.GPU_BOOSTER_MAX", "MAX");
            put("com.sec.android.intent.action.DDR_BOOSTER_MIN", "MIN");
            put("com.sec.android.intent.action.DDR_BOOSTER_MAX", "MAX");
            put("com.sec.android.intent.action.CPU_BOOSTER_CORE_NUM", "NUM");
        }
    };

    public void sendToPowerHint(Intent intent) {
        new PowerHintManager();
        String action = intent.getAction();
        String extraKey = this.mHintMap.get(action);
        if (extraKey == null) {
            Log.w("EventRelay", "SsgIntentReceiver: No such intent : " + action);
            return;
        }
        String extraValue = intent.getStringExtra(extraKey);
        Log.d("EventRelay", "New Hint: " + action + " " + extraKey + " " + extraValue);
        Parcel parcel = Parcel.obtain();
        parcel.writeString(action);
        parcel.writeString(extraValue);
        parcel.setDataPosition(0);
        parcel.recycle();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        sendToPowerHint(intent);
    }
}
