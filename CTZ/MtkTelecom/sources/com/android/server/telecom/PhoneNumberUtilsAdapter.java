package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;

public interface PhoneNumberUtilsAdapter {
    String convertKeypadLettersToDigits(String str);

    String getNumberFromIntent(Intent intent, Context context);

    boolean isLocalEmergencyNumber(Context context, String str);

    boolean isPotentialLocalEmergencyNumber(Context context, String str);

    boolean isSamePhoneNumber(String str, String str2);

    boolean isUriNumber(String str);

    String stripSeparators(String str);
}
