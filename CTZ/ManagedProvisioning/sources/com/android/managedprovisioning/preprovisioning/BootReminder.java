package com.android.managedprovisioning.preprovisioning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReminder extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        EncryptionController.getInstance(context).resumeProvisioning();
    }
}
