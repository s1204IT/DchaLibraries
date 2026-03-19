package com.android.internal.telephony;

import android.R;
import android.telephony.Rlog;

public class TelephonyCapabilities {
    private static final String LOG_TAG = "TelephonyCapabilities";

    private TelephonyCapabilities() {
    }

    public static boolean supportsEcm(Phone phone) {
        try {
            return ((Boolean) Class.forName("com.mediatek.internal.telephony.MtkTelephonyCapabilities").getDeclaredMethod("supportsEcm", Phone.class).invoke(null, phone)).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            Rlog.e(LOG_TAG, "supportsEcm invoke redirect fails. Use AOSP instead.");
            Rlog.d(LOG_TAG, "supportsEcm: Phone type = " + phone.getPhoneType() + " Ims Phone = " + phone.getImsPhone());
            return phone.getPhoneType() == 2 || phone.getImsPhone() != null;
        }
    }

    public static boolean supportsOtasp(Phone phone) {
        return phone.getPhoneType() == 2;
    }

    public static boolean supportsVoiceMessageCount(Phone phone) {
        return phone.getVoiceMessageCount() != -1;
    }

    public static boolean supportsNetworkSelection(Phone phone) {
        return phone.getPhoneType() == 1;
    }

    public static int getDeviceIdLabel(Phone phone) {
        if (phone.getPhoneType() == 1) {
            return R.string.config_mediaProjectionPermissionDialogComponent;
        }
        if (phone.getPhoneType() == 2) {
            return R.string.enable_explore_by_touch_warning_title;
        }
        Rlog.w(LOG_TAG, "getDeviceIdLabel: no known label for phone " + phone.getPhoneName());
        return 0;
    }

    public static boolean supportsConferenceCallManagement(Phone phone) {
        return phone.getPhoneType() == 1 || phone.getPhoneType() == 3;
    }

    public static boolean supportsHoldAndUnhold(Phone phone) {
        return phone.getPhoneType() == 1 || phone.getPhoneType() == 3 || phone.getPhoneType() == 5;
    }

    public static boolean supportsAnswerAndHold(Phone phone) {
        return phone.getPhoneType() == 1 || phone.getPhoneType() == 3;
    }

    public static boolean supportsAdn(int i) {
        return i == 1;
    }

    public static boolean canDistinguishDialingAndConnected(int i) {
        return i == 1;
    }
}
