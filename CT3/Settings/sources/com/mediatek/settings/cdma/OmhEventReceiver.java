package com.mediatek.settings.cdma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class OmhEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!"com.mediatek.internal.omh.cardcheck".equals(intent.getAction())) {
            return;
        }
        Bundle extra = intent.getExtras();
        int subId = extra.getInt("subid", -1);
        boolean isReady = extra.getBoolean("is_ready", false);
        Log.d("OmhEventReceiver", "omh card ready, subid = " + subId + ", is ready = " + isReady);
        if (!CdmaUtils.isNonOmhSimInOmhDevice(subId) || CdmaUtils.hasNonOmhRecord(context, subId)) {
            return;
        }
        CdmaUtils.recordNonOmhSub(context, subId);
        Log.d("OmhEventReceiver", "new OMH record, send new request...");
        Message message = OmhEventHandler.getInstance(context).obtainMessage(101, 1000, -1);
        message.sendToTarget();
    }
}
