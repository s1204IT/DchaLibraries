package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;

public final class TelecomBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.v(this, "Action received: %s.", action);
        MissedCallNotifier missedCallNotifier = CallsManager.getInstance().getMissedCallNotifier();
        if ("com.android.server.telecom.ACTION_SEND_SMS_FROM_NOTIFICATION".equals(action)) {
            closeSystemDialogs(context);
            missedCallNotifier.clearMissedCalls();
            Intent intent2 = new Intent("android.intent.action.SENDTO", intent.getData());
            intent2.setFlags(268435456);
            context.startActivity(intent2);
            return;
        }
        if ("com.android.server.telecom.ACTION_CALL_BACK_FROM_NOTIFICATION".equals(action)) {
            closeSystemDialogs(context);
            missedCallNotifier.clearMissedCalls();
            Intent intent3 = new Intent("android.intent.action.CALL_PRIVILEGED", intent.getData());
            intent3.setFlags(276824064);
            context.startActivity(intent3);
            return;
        }
        if ("com.android.server.telecom.ACTION_CLEAR_MISSED_CALLS".equals(action)) {
            missedCallNotifier.clearMissedCalls();
        }
    }

    private void closeSystemDialogs(Context context) {
        context.sendBroadcastAsUser(new Intent("android.intent.action.CLOSE_SYSTEM_DIALOGS"), UserHandle.ALL);
    }
}
