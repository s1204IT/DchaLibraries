package com.android.server.location;

import android.content.Context;
import android.location.Country;
import android.location.CountryListener;
import android.location.Geocoder;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocationManagerService;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ComprehensiveCountryDetector extends CountryDetectorBase {
    static final boolean DEBUG = LocationManagerService.D;
    private static final long LOCATION_REFRESH_INTERVAL = 86400000;
    private static final int MAX_LENGTH_DEBUG_LOGS = 20;
    private static final String TAG = "CountryDetector";
    private int mCountServiceStateChanges;
    private Country mCountry;
    private Country mCountryFromLocation;
    private final ConcurrentLinkedQueue<Country> mDebugLogs;
    private Country mLastCountryAddedToLogs;
    private CountryListener mLocationBasedCountryDetectionListener;
    protected CountryDetectorBase mLocationBasedCountryDetector;
    protected Timer mLocationRefreshTimer;
    private final Object mObject;
    private PhoneStateListener mPhoneStateListener;
    private long mStartTime;
    private long mStopTime;
    private boolean mStopped;
    private final TelephonyManager mTelephonyManager;
    private int mTotalCountServiceStateChanges;
    private long mTotalTime;

    public ComprehensiveCountryDetector(Context context) {
        super(context);
        this.mStopped = false;
        this.mDebugLogs = new ConcurrentLinkedQueue<>();
        this.mObject = new Object();
        this.mLocationBasedCountryDetectionListener = new CountryListener() {
            public void onCountryDetected(Country country) {
                if (ComprehensiveCountryDetector.DEBUG) {
                    Slog.d(ComprehensiveCountryDetector.TAG, "Country detected via LocationBasedCountryDetector");
                }
                ComprehensiveCountryDetector.this.mCountryFromLocation = country;
                ComprehensiveCountryDetector.this.detectCountry(true, false);
                ComprehensiveCountryDetector.this.stopLocationBasedDetector();
            }
        };
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
    }

    @Override
    public Country detectCountry() {
        return detectCountry(false, !this.mStopped);
    }

    @Override
    public void stop() {
        Slog.i(TAG, "Stop the detector.");
        cancelLocationRefresh();
        removePhoneStateListener();
        stopLocationBasedDetector();
        this.mListener = null;
        this.mStopped = true;
    }

    private Country getCountry() {
        Country result = getNetworkBasedCountry();
        if (result == null) {
            result = getLastKnownLocationBasedCountry();
        }
        if (result == null) {
            result = getSimBasedCountry();
        }
        if (result == null) {
            result = getLocaleCountry();
        }
        addToLogs(result);
        return result;
    }

    private void addToLogs(Country country) {
        if (country == null) {
            return;
        }
        synchronized (this.mObject) {
            if (this.mLastCountryAddedToLogs != null && this.mLastCountryAddedToLogs.equals(country)) {
                return;
            }
            this.mLastCountryAddedToLogs = country;
            if (this.mDebugLogs.size() >= 20) {
                this.mDebugLogs.poll();
            }
            if (DEBUG) {
                Slog.d(TAG, country.toString());
            }
            this.mDebugLogs.add(country);
        }
    }

    private boolean isNetworkCountryCodeAvailable() {
        int phoneType = this.mTelephonyManager.getPhoneType();
        if (DEBUG) {
            Slog.v(TAG, "    phonetype=" + phoneType);
        }
        return phoneType == 1;
    }

    protected Country getNetworkBasedCountry() {
        if (isNetworkCountryCodeAvailable()) {
            String countryIso = this.mTelephonyManager.getNetworkCountryIso();
            if (!TextUtils.isEmpty(countryIso)) {
                return new Country(countryIso, 0);
            }
            return null;
        }
        return null;
    }

    protected Country getLastKnownLocationBasedCountry() {
        return this.mCountryFromLocation;
    }

    protected Country getSimBasedCountry() {
        String countryIso = this.mTelephonyManager.getSimCountryIso();
        if (!TextUtils.isEmpty(countryIso)) {
            return new Country(countryIso, 2);
        }
        return null;
    }

    protected Country getLocaleCountry() {
        Locale defaultLocale = Locale.getDefault();
        if (defaultLocale != null) {
            return new Country(defaultLocale.getCountry(), 3);
        }
        return null;
    }

    private Country detectCountry(boolean notifyChange, boolean startLocationBasedDetection) {
        Country country = getCountry();
        runAfterDetectionAsync(this.mCountry != null ? new Country(this.mCountry) : this.mCountry, country, notifyChange, startLocationBasedDetection);
        this.mCountry = country;
        return this.mCountry;
    }

    protected void runAfterDetectionAsync(final Country country, final Country detectedCountry, final boolean notifyChange, final boolean startLocationBasedDetection) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ComprehensiveCountryDetector.this.runAfterDetection(country, detectedCountry, notifyChange, startLocationBasedDetection);
            }
        });
    }

    @Override
    public void setCountryListener(CountryListener listener) {
        CountryListener prevListener = this.mListener;
        this.mListener = listener;
        if (this.mListener == null) {
            removePhoneStateListener();
            stopLocationBasedDetector();
            cancelLocationRefresh();
            this.mStopTime = SystemClock.elapsedRealtime();
            this.mTotalTime += this.mStopTime;
            return;
        }
        if (prevListener != null) {
            return;
        }
        addPhoneStateListener();
        detectCountry(false, true);
        this.mStartTime = SystemClock.elapsedRealtime();
        this.mStopTime = 0L;
        this.mCountServiceStateChanges = 0;
    }

    void runAfterDetection(Country country, Country detectedCountry, boolean notifyChange, boolean startLocationBasedDetection) {
        notifyIfCountryChanged(country, detectedCountry);
        if (DEBUG) {
            Slog.d(TAG, "startLocationBasedDetection=" + startLocationBasedDetection + " detectCountry=" + (detectedCountry != null ? "(source: " + detectedCountry.getSource() + ", countryISO: " + detectedCountry.getCountryIso() + ")" : null) + " isAirplaneModeOff()=" + isAirplaneModeOff() + " mListener=" + this.mListener + " isGeoCoderImplemnted()=" + isGeoCoderImplemented());
        }
        if (startLocationBasedDetection && ((detectedCountry == null || detectedCountry.getSource() > 1) && isAirplaneModeOff() && this.mListener != null && isGeoCoderImplemented())) {
            if (DEBUG) {
                Slog.d(TAG, "run startLocationBasedDetector()");
            }
            startLocationBasedDetector(this.mLocationBasedCountryDetectionListener);
        }
        if (detectedCountry == null || detectedCountry.getSource() >= 1) {
            scheduleLocationRefresh();
        } else {
            cancelLocationRefresh();
            stopLocationBasedDetector();
        }
    }

    private synchronized void startLocationBasedDetector(CountryListener listener) {
        if (this.mLocationBasedCountryDetector != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "starts LocationBasedDetector to detect Country code via Location info (e.g. GPS)");
        }
        this.mLocationBasedCountryDetector = createLocationBasedCountryDetector();
        this.mLocationBasedCountryDetector.setCountryListener(listener);
        this.mLocationBasedCountryDetector.detectCountry();
    }

    private synchronized void stopLocationBasedDetector() {
        if (DEBUG) {
            Slog.d(TAG, "tries to stop LocationBasedDetector (current detector: " + this.mLocationBasedCountryDetector + ")");
        }
        if (this.mLocationBasedCountryDetector != null) {
            this.mLocationBasedCountryDetector.stop();
            this.mLocationBasedCountryDetector = null;
        }
    }

    protected CountryDetectorBase createLocationBasedCountryDetector() {
        return new LocationBasedCountryDetector(this.mContext);
    }

    protected boolean isAirplaneModeOff() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 0;
    }

    private void notifyIfCountryChanged(Country country, Country detectedCountry) {
        if (detectedCountry == null || this.mListener == null) {
            return;
        }
        if (country != null && country.equals(detectedCountry)) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "" + country + " --> " + detectedCountry);
        }
        notifyListener(detectedCountry);
    }

    private synchronized void scheduleLocationRefresh() {
        if (this.mLocationRefreshTimer != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "start periodic location refresh timer. Interval: 86400000");
        }
        this.mLocationRefreshTimer = new Timer();
        this.mLocationRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (ComprehensiveCountryDetector.DEBUG) {
                    Slog.d(ComprehensiveCountryDetector.TAG, "periodic location refresh event. Starts detecting Country code");
                }
                ComprehensiveCountryDetector.this.mLocationRefreshTimer = null;
                ComprehensiveCountryDetector.this.detectCountry(false, true);
            }
        }, 86400000L);
    }

    private synchronized void cancelLocationRefresh() {
        if (this.mLocationRefreshTimer != null) {
            this.mLocationRefreshTimer.cancel();
            this.mLocationRefreshTimer = null;
        }
    }

    protected synchronized void addPhoneStateListener() {
        if (this.mPhoneStateListener == null) {
            this.mPhoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    ComprehensiveCountryDetector.this.mCountServiceStateChanges++;
                    ComprehensiveCountryDetector.this.mTotalCountServiceStateChanges++;
                    if (ComprehensiveCountryDetector.DEBUG) {
                        Slog.d(ComprehensiveCountryDetector.TAG, "onServiceStateChanged: " + serviceState.getState());
                    }
                    ComprehensiveCountryDetector.this.detectCountry(true, true);
                }
            };
            this.mTelephonyManager.listen(this.mPhoneStateListener, 1);
        }
    }

    protected synchronized void removePhoneStateListener() {
        if (this.mPhoneStateListener != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            this.mPhoneStateListener = null;
        }
    }

    protected boolean isGeoCoderImplemented() {
        return Geocoder.isPresent();
    }

    public String toString() {
        long currentTime = SystemClock.elapsedRealtime();
        long currentSessionLength = 0;
        StringBuilder sb = new StringBuilder();
        sb.append("ComprehensiveCountryDetector{");
        if (this.mStopTime == 0) {
            currentSessionLength = currentTime - this.mStartTime;
            sb.append("timeRunning=").append(currentSessionLength).append(", ");
        } else {
            sb.append("lastRunTimeLength=").append(this.mStopTime - this.mStartTime).append(", ");
        }
        sb.append("totalCountServiceStateChanges=").append(this.mTotalCountServiceStateChanges).append(", ");
        sb.append("currentCountServiceStateChanges=").append(this.mCountServiceStateChanges).append(", ");
        sb.append("totalTime=").append(this.mTotalTime + currentSessionLength).append(", ");
        sb.append("currentTime=").append(currentTime).append(", ");
        sb.append("countries=");
        for (Country country : this.mDebugLogs) {
            sb.append("\n   ").append(country.toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
