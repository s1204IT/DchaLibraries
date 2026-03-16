package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;

public final class TelephonyUtil {
    private static final String TAG = TelephonyUtil.class.getSimpleName();
    private static final PhoneAccountHandle DEFAULT_EMERGENCY_PHONE_ACCOUNT_HANDLE = new PhoneAccountHandle(new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService"), "E");

    private TelephonyUtil() {
    }

    static PhoneAccount getDefaultEmergencyPhoneAccount() {
        return PhoneAccount.builder(DEFAULT_EMERGENCY_PHONE_ACCOUNT_HANDLE, "E").setCapabilities(22).build();
    }

    static boolean isPstnComponentName(ComponentName componentName) {
        return new ComponentName("com.android.phone", "com.android.services.telephony.TelephonyConnectionService").equals(componentName);
    }

    static boolean shouldProcessAsEmergency(Context context, Uri uri) {
        return uri != null && PhoneNumberUtils.isPotentialLocalEmergencyNumber(context, uri.getSchemeSpecificPart());
    }
}
