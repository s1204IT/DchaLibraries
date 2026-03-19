package com.mediatek.vcalendar.valuetype;

import com.android.common.Rfc822Validator;

public final class CalAddress {
    private static final String MAILTO = "mailto:";

    private CalAddress() {
    }

    public static String getUserCalAddress(String str) {
        if (!isValidAddress(str)) {
            return null;
        }
        return MAILTO + str;
    }

    public static String getUserMail(String str) {
        String strSubstring = str.substring(str.indexOf(":") + 1, str.length());
        if (isValidAddress(strSubstring)) {
            return strSubstring;
        }
        return null;
    }

    private static boolean isValidAddress(String str) {
        Rfc822Validator rfc822Validator = new Rfc822Validator(null);
        rfc822Validator.setRemoveInvalid(true);
        return rfc822Validator.isValid(str);
    }
}
