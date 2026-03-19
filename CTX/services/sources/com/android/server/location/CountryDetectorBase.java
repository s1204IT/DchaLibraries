package com.android.server.location;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.os.Handler;

public abstract class CountryDetectorBase {
    protected final Context mContext;
    protected Country mDetectedCountry;
    protected final Handler mHandler = new Handler();
    protected CountryListener mListener;

    public abstract Country detectCountry();

    public abstract void stop();

    public CountryDetectorBase(Context context) {
        this.mContext = context;
    }

    public void setCountryListener(CountryListener countryListener) {
        this.mListener = countryListener;
    }

    protected void notifyListener(Country country) {
        if (this.mListener != null) {
            this.mListener.onCountryDetected(country);
        }
    }
}
