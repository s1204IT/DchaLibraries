package com.android.contacts.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.sip.SipManager;
import android.telephony.TelephonyManager;
import java.util.List;

public final class PhoneCapabilityTester {
    private static boolean sIsInitialized;
    private static boolean sIsPhone;
    private static boolean sIsSipPhone;

    public static boolean isIntentRegistered(Context context, Intent intent) {
        PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> receiverList = packageManager.queryIntentActivities(intent, 65536);
        return receiverList.size() > 0;
    }

    public static boolean isPhone(Context context) {
        if (!sIsInitialized) {
            initialize(context);
        }
        return sIsPhone;
    }

    private static void initialize(Context context) {
        TelephonyManager telephonyManager = new TelephonyManager(context);
        sIsPhone = telephonyManager.isVoiceCapable();
        sIsSipPhone = sIsPhone && SipManager.isVoipSupported(context);
        sIsInitialized = true;
    }

    public static boolean isSipPhone(Context context) {
        if (!sIsInitialized) {
            initialize(context);
        }
        return sIsSipPhone;
    }

    public static boolean isCameraIntentRegistered(Context context) {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        return isIntentRegistered(context, intent);
    }
}
