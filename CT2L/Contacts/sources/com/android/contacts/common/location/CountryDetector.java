package com.android.contacts.common.location;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import java.util.Locale;

public class CountryDetector {
    private static CountryDetector sInstance;
    private final String DEFAULT_COUNTRY_ISO;
    private final Context mContext;
    private final LocaleProvider mLocaleProvider;
    private final LocationManager mLocationManager;
    private final TelephonyManager mTelephonyManager;

    public static class LocaleProvider {
        public Locale getDefaultLocale() {
            return Locale.getDefault();
        }
    }

    private CountryDetector(Context context) {
        this(context, (TelephonyManager) context.getSystemService("phone"), (LocationManager) context.getSystemService("location"), new LocaleProvider());
    }

    private CountryDetector(Context context, TelephonyManager telephonyManager, LocationManager locationManager, LocaleProvider localeProvider) {
        this.DEFAULT_COUNTRY_ISO = "US";
        this.mTelephonyManager = telephonyManager;
        this.mLocationManager = locationManager;
        this.mLocaleProvider = localeProvider;
        this.mContext = context;
        registerForLocationUpdates(context, this.mLocationManager);
    }

    public static void registerForLocationUpdates(Context context, LocationManager locationManager) {
        if (Geocoder.isPresent()) {
            Intent activeIntent = new Intent(context, (Class<?>) LocationChangedReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, activeIntent, 134217728);
            locationManager.requestLocationUpdates("passive", 43200000L, 5000.0f, pendingIntent);
        }
    }

    public CountryDetector getInstanceForTest(Context context, TelephonyManager telephonyManager, LocationManager locationManager, LocaleProvider localeProvider, Geocoder geocoder) {
        return new CountryDetector(context, telephonyManager, locationManager, localeProvider);
    }

    public static synchronized CountryDetector getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new CountryDetector(context.getApplicationContext());
        }
        return sInstance;
    }

    public String getCurrentCountryIso() {
        String result = null;
        if (isNetworkCountryCodeAvailable()) {
            result = getNetworkBasedCountryIso();
        }
        if (TextUtils.isEmpty(result)) {
            result = getLocationBasedCountryIso();
        }
        if (TextUtils.isEmpty(result)) {
            result = getSimBasedCountryIso();
        }
        if (TextUtils.isEmpty(result)) {
            result = getLocaleBasedCountryIso();
        }
        if (TextUtils.isEmpty(result)) {
            result = "US";
        }
        return result.toUpperCase(Locale.US);
    }

    private String getNetworkBasedCountryIso() {
        return this.mTelephonyManager.getNetworkCountryIso();
    }

    private String getLocationBasedCountryIso() {
        if (!Geocoder.isPresent()) {
            return null;
        }
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        return sharedPreferences.getString("preference_current_country", null);
    }

    private String getSimBasedCountryIso() {
        return this.mTelephonyManager.getSimCountryIso();
    }

    private String getLocaleBasedCountryIso() {
        Locale defaultLocale = this.mLocaleProvider.getDefaultLocale();
        if (defaultLocale != null) {
            return defaultLocale.getCountry();
        }
        return null;
    }

    private boolean isNetworkCountryCodeAvailable() {
        return this.mTelephonyManager.getPhoneType() == 1;
    }

    public static class LocationChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("location")) {
                Location location = (Location) intent.getExtras().get("location");
                UpdateCountryService.updateCountry(context, location);
            }
        }
    }
}
