package com.mediatek.settings.cdma;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.mediatek.phone.PhoneFeatureConstants;

public class LteDataOnlySwitchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("LteDataOnlySwitchReceiver", "Receive: " + action);
        if (PhoneFeatureConstants.FeatureOption.isMtkTddDataOnlySupport() && TelephonyUtilsEx.is4GDataOnly(context)) {
            if (action.equals("android.intent.action.BOOT_COMPLETED") || action.equals("com.mediatek.intent.action.STARTSELF_LTE_SEARCH_TIMEOUT_CHECK") || action.equals("com.mediatek.intent.action.ACTION_NETWORK_CHANGED")) {
                Log.d("LteDataOnlySwitchReceiver", "start LteSearchTimeoutCheckService for " + action);
                if (context != null) {
                    context.startService(new Intent(context, (Class<?>) LteSearchTimeoutCheckService.class));
                }
            }
        }
    }
}
