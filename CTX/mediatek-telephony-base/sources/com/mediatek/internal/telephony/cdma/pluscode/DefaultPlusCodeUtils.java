package com.mediatek.internal.telephony.cdma.pluscode;

import android.os.Build;
import android.telephony.Rlog;

public class DefaultPlusCodeUtils implements IPlusCodeUtils {
    public static final boolean DBG = "eng".equals(Build.TYPE);
    private static final String LOG_TAG = "DefaultPlusCodeUtils";

    @Override
    public String checkMccBySidLtmOff(String str) {
        log("checkMccBySidLtmOff mccMnc=" + str);
        return str;
    }

    @Override
    public boolean canFormatPlusToIddNdd() {
        log("canFormatPlusToIddNdd");
        return false;
    }

    @Override
    public boolean canFormatPlusCodeForSms() {
        log("canFormatPlusCodeForSms");
        return false;
    }

    @Override
    public String replacePlusCodeWithIddNdd(String str) {
        log("replacePlusCodeWithIddNdd number=" + str);
        return str;
    }

    @Override
    public String replacePlusCodeForSms(String str) {
        log("replacePlusCodeForSms number=" + str);
        return str;
    }

    @Override
    public String removeIddNddAddPlusCodeForSms(String str) {
        log("removeIddNddAddPlusCodeForSms number=" + str);
        return str;
    }

    @Override
    public String removeIddNddAddPlusCode(String str) {
        log("removeIddNddAddPlusCode number=" + str);
        return str;
    }

    private static void log(String str) {
        if (DBG) {
            Rlog.d(LOG_TAG, str);
        }
    }
}
