package com.android.server.location;

import android.content.Context;
import android.location.Address;
import android.location.Country;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.util.Slog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationBasedCountryDetector extends CountryDetectorBase {
    private static final long QUERY_LOCATION_TIMEOUT = 300000;
    private static final String TAG = "LocationBasedCountryDetector";
    private List<String> mEnabledProviders;
    protected List<LocationListener> mLocationListeners;
    private LocationManager mLocationManager;
    protected Thread mQueryThread;
    protected Timer mTimer;

    public LocationBasedCountryDetector(Context context) {
        super(context);
        this.mLocationManager = (LocationManager) context.getSystemService("location");
    }

    protected String getCountryFromLocation(Location location) {
        try {
            List<Address> fromLocation = new Geocoder(this.mContext).getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (fromLocation == null || fromLocation.size() <= 0) {
                return null;
            }
            return fromLocation.get(0).getCountryCode();
        } catch (IOException e) {
            Slog.w(TAG, "Exception occurs when getting country from location");
            return null;
        }
    }

    protected boolean isAcceptableProvider(String str) {
        return "passive".equals(str);
    }

    protected void registerListener(String str, LocationListener locationListener) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mLocationManager.requestLocationUpdates(str, 0L, 0.0f, locationListener);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected void unregisterListener(LocationListener locationListener) {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            this.mLocationManager.removeUpdates(locationListener);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected Location getLastKnownLocation() {
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            Location location = null;
            Iterator<String> it = this.mLocationManager.getAllProviders().iterator();
            while (it.hasNext()) {
                Location lastKnownLocation = this.mLocationManager.getLastKnownLocation(it.next());
                if (lastKnownLocation != null && (location == null || location.getElapsedRealtimeNanos() < lastKnownLocation.getElapsedRealtimeNanos())) {
                    location = lastKnownLocation;
                }
            }
            return location;
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    protected long getQueryLocationTimeout() {
        return 300000L;
    }

    protected List<String> getEnabledProviders() {
        if (this.mEnabledProviders == null) {
            this.mEnabledProviders = this.mLocationManager.getProviders(true);
        }
        return this.mEnabledProviders;
    }

    @Override
    public synchronized Country detectCountry() {
        if (this.mLocationListeners != null) {
            throw new IllegalStateException();
        }
        List<String> enabledProviders = getEnabledProviders();
        int size = enabledProviders.size();
        if (size > 0) {
            this.mLocationListeners = new ArrayList(size);
            for (int i = 0; i < size; i++) {
                String str = enabledProviders.get(i);
                if (isAcceptableProvider(str)) {
                    LocationListener locationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            if (location != null) {
                                LocationBasedCountryDetector.this.stop();
                                LocationBasedCountryDetector.this.queryCountryCode(location);
                            }
                        }

                        @Override
                        public void onProviderDisabled(String str2) {
                        }

                        @Override
                        public void onProviderEnabled(String str2) {
                        }

                        @Override
                        public void onStatusChanged(String str2, int i2, Bundle bundle) {
                        }
                    };
                    this.mLocationListeners.add(locationListener);
                    registerListener(str, locationListener);
                }
            }
            this.mTimer = new Timer();
            this.mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    LocationBasedCountryDetector.this.mTimer = null;
                    LocationBasedCountryDetector.this.stop();
                    LocationBasedCountryDetector.this.queryCountryCode(LocationBasedCountryDetector.this.getLastKnownLocation());
                }
            }, getQueryLocationTimeout());
        } else {
            queryCountryCode(getLastKnownLocation());
        }
        return this.mDetectedCountry;
    }

    @Override
    public synchronized void stop() {
        if (this.mLocationListeners != null) {
            Iterator<LocationListener> it = this.mLocationListeners.iterator();
            while (it.hasNext()) {
                unregisterListener(it.next());
            }
            this.mLocationListeners = null;
        }
        if (this.mTimer != null) {
            this.mTimer.cancel();
            this.mTimer = null;
        }
    }

    private synchronized void queryCountryCode(final Location location) {
        if (location == null) {
            notifyListener(null);
        } else {
            if (this.mQueryThread != null) {
                return;
            }
            this.mQueryThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String countryFromLocation;
                    if (location != null) {
                        countryFromLocation = LocationBasedCountryDetector.this.getCountryFromLocation(location);
                    } else {
                        countryFromLocation = null;
                    }
                    if (countryFromLocation != null) {
                        LocationBasedCountryDetector.this.mDetectedCountry = new Country(countryFromLocation, 1);
                    } else {
                        LocationBasedCountryDetector.this.mDetectedCountry = null;
                    }
                    LocationBasedCountryDetector.this.notifyListener(LocationBasedCountryDetector.this.mDetectedCountry);
                    LocationBasedCountryDetector.this.mQueryThread = null;
                }
            });
            this.mQueryThread.start();
        }
    }
}
