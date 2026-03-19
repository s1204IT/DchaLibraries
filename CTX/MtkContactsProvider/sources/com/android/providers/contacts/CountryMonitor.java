package com.android.providers.contacts;

import android.content.Context;
import android.location.Country;
import android.location.CountryDetector;
import android.location.CountryListener;
import android.os.Looper;
import java.util.Locale;

public class CountryMonitor {
    private Context mContext;
    private String mCurrentCountryIso;

    public CountryMonitor(Context context) {
        this.mContext = context;
    }

    public synchronized String getCountryIso() {
        if (this.mCurrentCountryIso == null) {
            CountryDetector countryDetector = (CountryDetector) this.mContext.getSystemService("country_detector");
            Country countryDetectCountry = countryDetector != null ? countryDetector.detectCountry() : null;
            if (countryDetectCountry == null) {
                return Locale.getDefault().getCountry();
            }
            this.mCurrentCountryIso = countryDetectCountry.getCountryIso();
            countryDetector.addCountryListener(new CountryListener() {
                public void onCountryDetected(Country country) {
                    CountryMonitor.this.mCurrentCountryIso = country.getCountryIso();
                }
            }, Looper.getMainLooper());
        }
        return this.mCurrentCountryIso;
    }
}
