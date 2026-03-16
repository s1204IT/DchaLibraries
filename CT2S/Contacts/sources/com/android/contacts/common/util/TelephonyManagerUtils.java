package com.android.contacts.common.util;

import android.content.Context;
import android.telephony.TelephonyManager;

public class TelephonyManagerUtils {
    private static final String LOG_TAG = TelephonyManagerUtils.class.getSimpleName();

    public static boolean isSmsCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return telephonyManager != null && telephonyManager.isSmsCapable();
    }
}
