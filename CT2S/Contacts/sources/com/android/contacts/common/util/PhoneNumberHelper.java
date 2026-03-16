package com.android.contacts.common.util;

public class PhoneNumberHelper {
    private static final String LOG_TAG = PhoneNumberHelper.class.getSimpleName();

    public static boolean isUriNumber(String number) {
        return number != null && (number.contains("@") || number.contains("%40"));
    }
}
