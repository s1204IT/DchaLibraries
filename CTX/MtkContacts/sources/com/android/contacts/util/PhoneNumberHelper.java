package com.android.contacts.util;

public class PhoneNumberHelper {
    private static final String LOG_TAG = PhoneNumberHelper.class.getSimpleName();

    public static boolean isUriNumber(String str) {
        return str != null && (str.contains("@") || str.contains("%40"));
    }
}
