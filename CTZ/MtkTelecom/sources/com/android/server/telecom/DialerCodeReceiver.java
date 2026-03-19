package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.Log;

public class DialerCodeReceiver extends BroadcastReceiver {
    private final CallsManager mCallsManager;

    DialerCodeReceiver(CallsManager callsManager) {
        this.mCallsManager = callsManager;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SECRET_CODE".equals(intent.getAction()) && intent.getData() != null && intent.getData().getHost() != null) {
            if (intent.getData().getHost().equals("823241")) {
                Log.i("DialerCodeReceiver", "Secret code used to enable extended logging mode", new Object[0]);
                Log.setIsExtendedLoggingEnabled(true);
            } else if (intent.getData().getHost().equals("823240")) {
                Log.i("DialerCodeReceiver", "Secret code used to disable extended logging mode", new Object[0]);
                Log.setIsExtendedLoggingEnabled(false);
            } else if (intent.getData().getHost().equals("826275")) {
                Log.i("DialerCodeReceiver", "Secret code used to mark logs.", new Object[0]);
                Log.addEvent(this.mCallsManager.getActiveCall(), "USER_LOG_MARK");
            }
        }
    }
}
