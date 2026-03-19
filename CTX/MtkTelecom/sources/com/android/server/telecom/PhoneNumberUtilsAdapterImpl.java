package com.android.server.telecom;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;

public class PhoneNumberUtilsAdapterImpl implements PhoneNumberUtilsAdapter {
    @Override
    public boolean isLocalEmergencyNumber(Context context, String str) {
        return PhoneNumberUtils.isLocalEmergencyNumber(context, str);
    }

    @Override
    public boolean isPotentialLocalEmergencyNumber(Context context, String str) {
        return PhoneNumberUtils.isPotentialLocalEmergencyNumber(context, str);
    }

    @Override
    public boolean isUriNumber(String str) {
        return PhoneNumberUtils.isUriNumber(str);
    }

    @Override
    public boolean isSamePhoneNumber(String str, String str2) {
        return PhoneNumberUtils.compare(str, str2);
    }

    @Override
    public String getNumberFromIntent(Intent intent, Context context) {
        return PhoneNumberUtils.getNumberFromIntent(intent, context);
    }

    @Override
    public String convertKeypadLettersToDigits(String str) {
        return PhoneNumberUtils.convertKeypadLettersToDigits(str);
    }

    @Override
    public String stripSeparators(String str) {
        return PhoneNumberUtils.stripSeparators(str);
    }
}
