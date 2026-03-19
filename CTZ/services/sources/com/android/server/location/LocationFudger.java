package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.security.SecureRandom;

public class LocationFudger {
    private static final int APPROXIMATE_METERS_PER_DEGREE_AT_EQUATOR = 111000;
    private static final long CHANGE_INTERVAL_MS = 3600000;
    private static final double CHANGE_PER_INTERVAL = 0.03d;
    private static final String COARSE_ACCURACY_CONFIG_NAME = "locationCoarseAccuracy";
    private static final boolean D = false;
    private static final float DEFAULT_ACCURACY_IN_METERS = 2000.0f;
    public static final long FASTEST_INTERVAL_MS = 600000;
    private static final double MAX_LATITUDE = 89.999990990991d;
    private static final float MINIMUM_ACCURACY_IN_METERS = 200.0f;
    private static final double NEW_WEIGHT = 0.03d;
    private static final double PREVIOUS_WEIGHT = Math.sqrt(0.9991d);
    private static final String TAG = "LocationFudge";
    private float mAccuracyInMeters;
    private final Context mContext;
    private double mGridSizeInMeters;
    private long mNextInterval;
    private double mOffsetLatitudeMeters;
    private double mOffsetLongitudeMeters;
    private final ContentObserver mSettingsObserver;
    private double mStandardDeviationInMeters;
    private final Object mLock = new Object();
    private final SecureRandom mRandom = new SecureRandom();

    public LocationFudger(Context context, Handler handler) {
        this.mContext = context;
        this.mSettingsObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean z) {
                LocationFudger.this.setAccuracyInMeters(LocationFudger.this.loadCoarseAccuracy());
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(COARSE_ACCURACY_CONFIG_NAME), false, this.mSettingsObserver);
        float fLoadCoarseAccuracy = loadCoarseAccuracy();
        synchronized (this.mLock) {
            setAccuracyInMetersLocked(fLoadCoarseAccuracy);
            this.mOffsetLatitudeMeters = nextOffsetLocked();
            this.mOffsetLongitudeMeters = nextOffsetLocked();
            this.mNextInterval = SystemClock.elapsedRealtime() + 3600000;
        }
    }

    public Location getOrCreate(Location location) {
        synchronized (this.mLock) {
            Location extraLocation = location.getExtraLocation("coarseLocation");
            if (extraLocation == null) {
                return addCoarseLocationExtraLocked(location);
            }
            if (extraLocation.getAccuracy() >= this.mAccuracyInMeters) {
                return extraLocation;
            }
            return addCoarseLocationExtraLocked(location);
        }
    }

    private Location addCoarseLocationExtraLocked(Location location) {
        Location locationCreateCoarseLocked = createCoarseLocked(location);
        location.setExtraLocation("coarseLocation", locationCreateCoarseLocked);
        return locationCreateCoarseLocked;
    }

    private Location createCoarseLocked(Location location) {
        Location location2 = new Location(location);
        location2.removeBearing();
        location2.removeSpeed();
        location2.removeAltitude();
        location2.setExtras(null);
        double latitude = location2.getLatitude();
        double longitude = location2.getLongitude();
        double dWrapLatitude = wrapLatitude(latitude);
        double dWrapLongitude = wrapLongitude(longitude);
        updateRandomOffsetLocked();
        double dMetersToDegreesLongitude = dWrapLongitude + metersToDegreesLongitude(this.mOffsetLongitudeMeters, dWrapLatitude);
        double dWrapLatitude2 = wrapLatitude(dWrapLatitude + metersToDegreesLatitude(this.mOffsetLatitudeMeters));
        double dWrapLongitude2 = wrapLongitude(dMetersToDegreesLongitude);
        double dRound = Math.round(dWrapLatitude2 / r5) * metersToDegreesLatitude(this.mGridSizeInMeters);
        double dRound2 = Math.round(dWrapLongitude2 / r5) * metersToDegreesLongitude(this.mGridSizeInMeters, dRound);
        double dWrapLatitude3 = wrapLatitude(dRound);
        double dWrapLongitude3 = wrapLongitude(dRound2);
        location2.setLatitude(dWrapLatitude3);
        location2.setLongitude(dWrapLongitude3);
        location2.setAccuracy(Math.max(this.mAccuracyInMeters, location2.getAccuracy()));
        return location2;
    }

    private void updateRandomOffsetLocked() {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (jElapsedRealtime < this.mNextInterval) {
            return;
        }
        this.mNextInterval = jElapsedRealtime + 3600000;
        this.mOffsetLatitudeMeters *= PREVIOUS_WEIGHT;
        this.mOffsetLatitudeMeters += nextOffsetLocked() * 0.03d;
        this.mOffsetLongitudeMeters *= PREVIOUS_WEIGHT;
        this.mOffsetLongitudeMeters += 0.03d * nextOffsetLocked();
    }

    private double nextOffsetLocked() {
        return this.mRandom.nextGaussian() * this.mStandardDeviationInMeters;
    }

    private static double wrapLatitude(double d) {
        if (d > MAX_LATITUDE) {
            d = 89.999990990991d;
        }
        if (d < -89.999990990991d) {
            return -89.999990990991d;
        }
        return d;
    }

    private static double wrapLongitude(double d) {
        double d2 = d % 360.0d;
        if (d2 >= 180.0d) {
            d2 -= 360.0d;
        }
        if (d2 < -180.0d) {
            return d2 + 360.0d;
        }
        return d2;
    }

    private static double metersToDegreesLatitude(double d) {
        return d / 111000.0d;
    }

    private static double metersToDegreesLongitude(double d, double d2) {
        return (d / 111000.0d) / Math.cos(Math.toRadians(d2));
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println(String.format("offset: %.0f, %.0f (meters)", Double.valueOf(this.mOffsetLongitudeMeters), Double.valueOf(this.mOffsetLatitudeMeters)));
    }

    private void setAccuracyInMetersLocked(float f) {
        this.mAccuracyInMeters = Math.max(f, MINIMUM_ACCURACY_IN_METERS);
        this.mGridSizeInMeters = this.mAccuracyInMeters;
        this.mStandardDeviationInMeters = this.mGridSizeInMeters / 4.0d;
    }

    private void setAccuracyInMeters(float f) {
        synchronized (this.mLock) {
            setAccuracyInMetersLocked(f);
        }
    }

    private float loadCoarseAccuracy() {
        String string = Settings.Secure.getString(this.mContext.getContentResolver(), COARSE_ACCURACY_CONFIG_NAME);
        if (string == null) {
            return DEFAULT_ACCURACY_IN_METERS;
        }
        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException e) {
            return DEFAULT_ACCURACY_IN_METERS;
        }
    }
}
