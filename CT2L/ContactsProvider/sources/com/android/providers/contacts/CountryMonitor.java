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
        String country;
        if (this.mCurrentCountryIso == null) {
            CountryDetector countryDetector = (CountryDetector) this.mContext.getSystemService("country_detector");
            Country country2 = countryDetector != null ? countryDetector.detectCountry() : null;
            if (country2 == null) {
                country = Locale.getDefault().getCountry();
            } else {
                this.mCurrentCountryIso = country2.getCountryIso();
                countryDetector.addCountryListener(new CountryListener() {
                    public void onCountryDetected(Country country3) {
                        CountryMonitor.this.mCurrentCountryIso = country3.getCountryIso();
                    }
                }, Looper.getMainLooper());
                country = this.mCurrentCountryIso;
            }
        } else {
            country = this.mCurrentCountryIso;
        }
        return country;
    }
}
