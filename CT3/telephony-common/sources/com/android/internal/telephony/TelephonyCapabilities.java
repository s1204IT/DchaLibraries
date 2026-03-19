package com.android.internal.telephony;

import android.R;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;

public class TelephonyCapabilities {
    private static final String LOG_TAG = "TelephonyCapabilities";

    private TelephonyCapabilities() {
    }

    public static boolean supportsEcm(Phone phone) {
        Rlog.d(LOG_TAG, "supportsEcm: Phone type = " + phone.getPhoneType() + " Ims Phone = " + phone.getImsPhone());
        return phone.getPhoneType() == 2 || phone.getImsPhone() != null;
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
            return R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_ERROR;
        }
        if (phone.getPhoneType() == 2) {
            if (TelephonyManager.getDefault().getLteOnCdmaMode() == 1) {
                Rlog.d(LOG_TAG, "getDeviceIdLabel, imei");
                return R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_ERROR;
            }
            return R.string.PERSOSUBSTATE_SIM_NETWORK_SUBSET_PUK_IN_PROGRESS;
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

    public static boolean supportsAdn(int phoneType) {
        return phoneType == 1;
    }

    public static boolean canDistinguishDialingAndConnected(int phoneType) {
        return phoneType == 1;
    }
}
