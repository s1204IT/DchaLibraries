package com.marvell.powergadget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.marvell.powergadget.eventrelay.EventRelay;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent iEventRelay = new Intent(context, (Class<?>) EventRelay.class);
        iEventRelay.setAction("com.marvell.powergadget.eventrelay.EventRelay");
        context.startService(iEventRelay);
        Log.i("PowerGadget", "start services ...");
    }
}
