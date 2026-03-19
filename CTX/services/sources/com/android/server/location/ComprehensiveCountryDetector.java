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
import com.android.server.backup.BackupManagerConstants;
import java.util.Iterator;
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

    static int access$308(ComprehensiveCountryDetector comprehensiveCountryDetector) {
        int i = comprehensiveCountryDetector.mCountServiceStateChanges;
        comprehensiveCountryDetector.mCountServiceStateChanges = i + 1;
        return i;
    }

    static int access$408(ComprehensiveCountryDetector comprehensiveCountryDetector) {
        int i = comprehensiveCountryDetector.mTotalCountServiceStateChanges;
        comprehensiveCountryDetector.mTotalCountServiceStateChanges = i + 1;
        return i;
    }

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
        Country networkBasedCountry = getNetworkBasedCountry();
        if (networkBasedCountry == null) {
            networkBasedCountry = getLastKnownLocationBasedCountry();
        }
        if (networkBasedCountry == null) {
            networkBasedCountry = getSimBasedCountry();
        }
        if (networkBasedCountry == null) {
            networkBasedCountry = getLocaleCountry();
        }
        addToLogs(networkBasedCountry);
        return networkBasedCountry;
    }

    private void addToLogs(Country country) {
        if (country == null) {
            return;
        }
        synchronized (this.mObject) {
            if (this.mLastCountryAddedToLogs == null || !this.mLastCountryAddedToLogs.equals(country)) {
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
            String networkCountryIso = this.mTelephonyManager.getNetworkCountryIso();
            if (!TextUtils.isEmpty(networkCountryIso)) {
                return new Country(networkCountryIso, 0);
            }
            return null;
        }
        return null;
    }

    protected Country getLastKnownLocationBasedCountry() {
        return this.mCountryFromLocation;
    }

    protected Country getSimBasedCountry() {
        String simCountryIso = this.mTelephonyManager.getSimCountryIso();
        if (!TextUtils.isEmpty(simCountryIso)) {
            return new Country(simCountryIso, 2);
        }
        return null;
    }

    protected Country getLocaleCountry() {
        Locale locale = Locale.getDefault();
        if (locale != null) {
            return new Country(locale.getCountry(), 3);
        }
        return null;
    }

    private Country detectCountry(boolean z, boolean z2) {
        Country country = getCountry();
        runAfterDetectionAsync(this.mCountry != null ? new Country(this.mCountry) : this.mCountry, country, z, z2);
        this.mCountry = country;
        return this.mCountry;
    }

    protected void runAfterDetectionAsync(final Country country, final Country country2, final boolean z, final boolean z2) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                ComprehensiveCountryDetector.this.runAfterDetection(country, country2, z, z2);
            }
        });
    }

    @Override
    public void setCountryListener(CountryListener countryListener) {
        CountryListener countryListener2 = this.mListener;
        this.mListener = countryListener;
        if (this.mListener == null) {
            removePhoneStateListener();
            stopLocationBasedDetector();
            cancelLocationRefresh();
            this.mStopTime = SystemClock.elapsedRealtime();
            this.mTotalTime += this.mStopTime;
            return;
        }
        if (countryListener2 == null) {
            addPhoneStateListener();
            detectCountry(false, true);
            this.mStartTime = SystemClock.elapsedRealtime();
            this.mStopTime = 0L;
            this.mCountServiceStateChanges = 0;
        }
    }

    void runAfterDetection(Country country, Country country2, boolean z, boolean z2) {
        String str;
        notifyIfCountryChanged(country, country2);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("startLocationBasedDetection=");
            sb.append(z2);
            sb.append(" detectCountry=");
            if (country2 == null) {
                str = null;
            } else {
                str = "(source: " + country2.getSource() + ", countryISO: " + country2.getCountryIso() + ")";
            }
            sb.append(str);
            sb.append(" isAirplaneModeOff()=");
            sb.append(isAirplaneModeOff());
            sb.append(" mListener=");
            sb.append(this.mListener);
            sb.append(" isGeoCoderImplemnted()=");
            sb.append(isGeoCoderImplemented());
            Slog.d(TAG, sb.toString());
        }
        if (z2 && ((country2 == null || country2.getSource() > 1) && isAirplaneModeOff() && this.mListener != null && isGeoCoderImplemented())) {
            if (DEBUG) {
                Slog.d(TAG, "run startLocationBasedDetector()");
            }
            startLocationBasedDetector(this.mLocationBasedCountryDetectionListener);
        }
        if (country2 == null || country2.getSource() >= 1) {
            scheduleLocationRefresh();
        } else {
            cancelLocationRefresh();
            stopLocationBasedDetector();
        }
    }

    private synchronized void startLocationBasedDetector(CountryListener countryListener) {
        if (this.mLocationBasedCountryDetector != null) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "starts LocationBasedDetector to detect Country code via Location info (e.g. GPS)");
        }
        this.mLocationBasedCountryDetector = createLocationBasedCountryDetector();
        this.mLocationBasedCountryDetector.setCountryListener(countryListener);
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

    private void notifyIfCountryChanged(Country country, Country country2) {
        if (country2 != null && this.mListener != null) {
            if (country == null || !country.equals(country2)) {
                if (DEBUG) {
                    Slog.d(TAG, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS + country + " --> " + country2);
                }
                notifyListener(country2);
            }
        }
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
                    ComprehensiveCountryDetector.access$308(ComprehensiveCountryDetector.this);
                    ComprehensiveCountryDetector.access$408(ComprehensiveCountryDetector.this);
                    if (!ComprehensiveCountryDetector.this.isNetworkCountryCodeAvailable()) {
                        return;
                    }
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
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        StringBuilder sb = new StringBuilder();
        sb.append("ComprehensiveCountryDetector{");
        long j = 0;
        if (this.mStopTime == 0) {
            j = jElapsedRealtime - this.mStartTime;
            sb.append("timeRunning=" + j + ", ");
        } else {
            sb.append("lastRunTimeLength=" + (this.mStopTime - this.mStartTime) + ", ");
        }
        sb.append("totalCountServiceStateChanges=" + this.mTotalCountServiceStateChanges + ", ");
        sb.append("currentCountServiceStateChanges=" + this.mCountServiceStateChanges + ", ");
        sb.append("totalTime=" + (this.mTotalTime + j) + ", ");
        sb.append("currentTime=" + jElapsedRealtime + ", ");
        sb.append("countries=");
        Iterator<Country> it = this.mDebugLogs.iterator();
        while (it.hasNext()) {
            sb.append("\n   " + it.next().toString());
        }
        sb.append("}");
        return sb.toString();
    }
}
