package com.android.providers.contacts;

import android.content.ContentValues;
import android.content.Context;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.google.android.collect.Sets;
import java.util.Locale;
import java.util.Set;

class DefaultCallLogInsertionHelper implements CallLogInsertionHelper {
    private static final Set<String> LEGACY_UNKNOWN_NUMBERS = Sets.newHashSet(new String[]{"-1", "-2", "-3"});
    private static DefaultCallLogInsertionHelper sInstance;
    private final CountryMonitor mCountryMonitor;
    private final Locale mLocale;
    private PhoneNumberOfflineGeocoder mPhoneNumberOfflineGeocoder;
    private PhoneNumberUtil mPhoneNumberUtil;

    public static synchronized DefaultCallLogInsertionHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DefaultCallLogInsertionHelper(context);
        }
        return sInstance;
    }

    private DefaultCallLogInsertionHelper(Context context) {
        this.mCountryMonitor = new CountryMonitor(context);
        this.mLocale = context.getResources().getConfiguration().locale;
    }

    @Override
    public void addComputedValues(ContentValues values) {
        String countryIso = getCurrentCountryIso();
        values.put("countryiso", countryIso);
        values.put("geocoded_location", getGeocodedLocationFor(values.getAsString("number"), countryIso));
        String number = values.getAsString("number");
        if (LEGACY_UNKNOWN_NUMBERS.contains(number)) {
            values.put("presentation", (Integer) 3);
            values.put("number", "");
        }
        if (!values.containsKey("normalized_number") && !TextUtils.isEmpty(number)) {
            String normalizedNumber = PhoneNumberUtils.formatNumberToE164(number, countryIso);
            if (!TextUtils.isEmpty(normalizedNumber)) {
                values.put("normalized_number", normalizedNumber);
            }
        }
    }

    private String getCurrentCountryIso() {
        return this.mCountryMonitor.getCountryIso();
    }

    private synchronized PhoneNumberUtil getPhoneNumberUtil() {
        if (this.mPhoneNumberUtil == null) {
            this.mPhoneNumberUtil = PhoneNumberUtil.getInstance();
        }
        return this.mPhoneNumberUtil;
    }

    private Phonenumber.PhoneNumber parsePhoneNumber(String number, String countryIso) {
        try {
            return getPhoneNumberUtil().parse(number, countryIso);
        } catch (NumberParseException e) {
            return null;
        }
    }

    private synchronized PhoneNumberOfflineGeocoder getPhoneNumberOfflineGeocoder() {
        if (this.mPhoneNumberOfflineGeocoder == null) {
            this.mPhoneNumberOfflineGeocoder = PhoneNumberOfflineGeocoder.getInstance();
        }
        return this.mPhoneNumberOfflineGeocoder;
    }

    public String getGeocodedLocationFor(String number, String countryIso) {
        Phonenumber.PhoneNumber structuredPhoneNumber = parsePhoneNumber(number, countryIso);
        if (structuredPhoneNumber != null) {
            return getPhoneNumberOfflineGeocoder().getDescriptionForNumber(structuredPhoneNumber, this.mLocale);
        }
        return null;
    }
}
