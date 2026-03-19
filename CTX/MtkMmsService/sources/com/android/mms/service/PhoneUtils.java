package com.android.mms.service;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import java.util.Locale;

public class PhoneUtils {
    public static String getNationalNumber(TelephonyManager telephonyManager, int i, String str) {
        String simOrDefaultLocaleCountry = getSimOrDefaultLocaleCountry(telephonyManager, i);
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsedNumber = getParsedNumber(phoneNumberUtil, str, simOrDefaultLocaleCountry);
        if (parsedNumber == null) {
            return str;
        }
        return phoneNumberUtil.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.NATIONAL).replaceAll("\\D", "");
    }

    private static Phonenumber.PhoneNumber getParsedNumber(PhoneNumberUtil phoneNumberUtil, String str, String str2) {
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(str, str2);
            if (phoneNumberUtil.isValidNumber(phoneNumber)) {
                return phoneNumber;
            }
            LogUtil.e("getParsedNumber: not a valid phone number for country " + str2);
            return null;
        } catch (NumberParseException e) {
            LogUtil.e("getParsedNumber: Not able to parse phone number", (Throwable) e);
            return null;
        }
    }

    private static String getSimOrDefaultLocaleCountry(TelephonyManager telephonyManager, int i) {
        String simCountry = getSimCountry(telephonyManager, i);
        if (TextUtils.isEmpty(simCountry)) {
            return Locale.getDefault().getCountry();
        }
        return simCountry;
    }

    private static String getSimCountry(TelephonyManager telephonyManager, int i) {
        String simCountryIso = telephonyManager.getSimCountryIso(i);
        if (TextUtils.isEmpty(simCountryIso)) {
            return null;
        }
        return simCountryIso.toUpperCase();
    }
}
