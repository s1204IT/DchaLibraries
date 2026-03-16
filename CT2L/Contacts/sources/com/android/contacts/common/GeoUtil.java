package com.android.contacts.common;

import android.content.Context;
import com.android.contacts.common.location.CountryDetector;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import java.util.Locale;

public class GeoUtil {
    public static String getCurrentCountryIso(Context context) {
        return CountryDetector.getInstance(context).getCurrentCountryIso();
    }

    public static String getGeocodedLocationFor(Context context, String phoneNumber) {
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber structuredPhoneNumber = phoneNumberUtil.parse(phoneNumber, getCurrentCountryIso(context));
            Locale locale = context.getResources().getConfiguration().locale;
            return geocoder.getDescriptionForNumber(structuredPhoneNumber, locale);
        } catch (NumberParseException e) {
            return null;
        }
    }
}
