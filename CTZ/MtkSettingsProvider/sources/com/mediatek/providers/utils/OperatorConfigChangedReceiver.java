package com.mediatek.providers.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OperatorConfigChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v("OperatorConfigChangedReceiver", action);
        if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
            new ProvidersUtils(context).loadNewOperatorSettings();
        }
    }
}
