package com.android.providers.blockednumber;

import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

public class Utils {
    public static String getCurrentCountryIso(Context context) {
        Country countryDetectCountry;
        CountryDetector countryDetector = (CountryDetector) context.getSystemService("country_detector");
        if (countryDetector != null && (countryDetectCountry = countryDetector.detectCountry()) != null) {
            return countryDetectCountry.getCountryIso();
        }
        return context.getResources().getConfiguration().locale.getCountry();
    }

    public static String getE164Number(Context context, String str, String str2) {
        String numberToE164;
        if (str != null && str.contains("@")) {
            return str;
        }
        if (TextUtils.isEmpty(str2)) {
            return (TextUtils.isEmpty(str) || (numberToE164 = PhoneNumberUtils.formatNumberToE164(str, getCurrentCountryIso(context))) == null) ? "" : numberToE164;
        }
        return str2;
    }

    public static String wrapSelectionWithParens(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }
        return "(" + str + ")";
    }
}
