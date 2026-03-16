package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.TelecomManager;

public class PhoneAccountBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data;
        if ("android.intent.action.PACKAGE_FULLY_REMOVED".equals(intent.getAction()) && (data = intent.getData()) != null) {
            handlePackageRemoved(context, data.getSchemeSpecificPart());
        }
    }

    private void handlePackageRemoved(Context context, String str) {
        TelecomManager telecomManagerFrom = TelecomManager.from(context);
        if (telecomManagerFrom != null) {
            telecomManagerFrom.clearAccountsForPackage(str);
        }
    }
}
