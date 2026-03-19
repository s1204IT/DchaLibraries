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
    private Context mContext;
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
        this.mContext = context;
    }

    @Override
    public void addComputedValues(ContentValues contentValues) {
        String currentCountryIso = getCurrentCountryIso();
        contentValues.put("countryiso", currentCountryIso);
        contentValues.put("geocoded_location", getGeocodedLocationFor(contentValues.getAsString("number"), currentCountryIso));
        String asString = contentValues.getAsString("number");
        if (LEGACY_UNKNOWN_NUMBERS.contains(asString)) {
            contentValues.put("presentation", (Integer) 3);
            contentValues.put("number", "");
        }
        if (!contentValues.containsKey("normalized_number") && !TextUtils.isEmpty(asString)) {
            String numberToE164 = PhoneNumberUtils.formatNumberToE164(asString, currentCountryIso);
            if (!TextUtils.isEmpty(numberToE164)) {
                contentValues.put("normalized_number", numberToE164);
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

    private Phonenumber.PhoneNumber parsePhoneNumber(String str, String str2) {
        try {
            return getPhoneNumberUtil().parse(str, str2);
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

    public String getGeocodedLocationFor(String str, String str2) {
        Phonenumber.PhoneNumber phoneNumber = parsePhoneNumber(str, str2);
        if (phoneNumber != null) {
            return getPhoneNumberOfflineGeocoder().getDescriptionForNumber(phoneNumber, this.mContext.getResources().getConfiguration().locale);
        }
        return null;
    }
}
