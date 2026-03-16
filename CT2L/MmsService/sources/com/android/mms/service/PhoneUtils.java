package com.android.mms.service;

import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import java.util.Locale;

public class PhoneUtils {
    public static String getNationalNumber(TelephonyManager telephonyManager, int subId, String phoneText) {
        String country = getSimOrDefaultLocaleCountry(telephonyManager, subId);
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        Phonenumber.PhoneNumber parsed = getParsedNumber(phoneNumberUtil, phoneText, country);
        return parsed == null ? phoneText : phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL).replaceAll("\\D", "");
    }

    private static Phonenumber.PhoneNumber getParsedNumber(PhoneNumberUtil phoneNumberUtil, String phoneText, String country) {
        try {
            Phonenumber.PhoneNumber phoneNumber = phoneNumberUtil.parse(phoneText, country);
            if (!phoneNumberUtil.isValidNumber(phoneNumber)) {
                Log.e("MmsService", "getParsedNumber: not a valid phone number " + phoneText + " for country " + country);
                return null;
            }
            return phoneNumber;
        } catch (NumberParseException e) {
            Log.e("MmsService", "getParsedNumber: Not able to parse phone number " + phoneText);
            return null;
        }
    }

    private static String getSimOrDefaultLocaleCountry(TelephonyManager telephonyManager, int subId) {
        String country = getSimCountry(telephonyManager, subId);
        if (TextUtils.isEmpty(country)) {
            return Locale.getDefault().getCountry();
        }
        return country;
    }

    private static String getSimCountry(TelephonyManager telephonyManager, int subId) {
        String country = telephonyManager.getSimCountryIso(subId);
        if (TextUtils.isEmpty(country)) {
            return null;
        }
        return country.toUpperCase();
    }
}
