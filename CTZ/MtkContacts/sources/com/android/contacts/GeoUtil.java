package com.android.contacts;

import android.content.Context;
import com.android.contacts.location.CountryDetector;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.mediatek.contacts.util.Log;
import java.util.Locale;

public class GeoUtil {
    public static String getCurrentCountryIso(Context context) {
        return CountryDetector.getInstance(context).getCurrentCountryIso();
    }

    public static String getGeocodedLocationFor(Context context, String str) {
        PhoneNumberOfflineGeocoder phoneNumberOfflineGeocoder = PhoneNumberOfflineGeocoder.getInstance();
        try {
            Phonenumber.PhoneNumber phoneNumber = PhoneNumberUtil.getInstance().parse(str, getCurrentCountryIso(context));
            Locale locale = context.getResources().getConfiguration().locale;
            String descriptionForNumber = phoneNumberOfflineGeocoder.getDescriptionForNumber(phoneNumber, locale);
            Log.d("GeoUtil", "location=" + descriptionForNumber + ", structuredPhoneNumber=" + Log.anonymize(phoneNumber) + ", locale=" + locale + ", phoneNumber=" + Log.anonymize(str));
            return descriptionForNumber;
        } catch (NumberParseException e) {
            return null;
        }
    }
}
