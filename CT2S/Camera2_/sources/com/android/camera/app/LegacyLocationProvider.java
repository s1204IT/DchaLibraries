package com.android.camera.app;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import com.android.camera.debug.Log;

public class LegacyLocationProvider implements LocationProvider {
    private static final Log.Tag TAG = new Log.Tag("LcyLocProvider");
    private Context mContext;
    LocationListener[] mLocationListeners = {new LocationListener("gps"), new LocationListener("network")};
    private android.location.LocationManager mLocationManager;
    private boolean mRecordLocation;

    public LegacyLocationProvider(Context context) {
        this.mContext = context;
    }

    @Override
    public Location getCurrentLocation() {
        if (!this.mRecordLocation) {
            return null;
        }
        for (int i = 0; i < this.mLocationListeners.length; i++) {
            Location l = this.mLocationListeners[i].current();
            if (l != null) {
                return l;
            }
        }
        Log.d(TAG, "No location received yet.");
        return null;
    }

    @Override
    public void recordLocation(boolean recordLocation) {
        if (this.mRecordLocation != recordLocation) {
            this.mRecordLocation = recordLocation;
            if (recordLocation) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }
    }

    @Override
    public void disconnect() {
        Log.d(TAG, "disconnect");
    }

    private void startReceivingLocationUpdates() {
        if (this.mLocationManager == null) {
            this.mLocationManager = (android.location.LocationManager) this.mContext.getSystemService("location");
        }
        if (this.mLocationManager != null) {
            try {
                this.mLocationManager.requestLocationUpdates("network", 1000L, 0.0f, this.mLocationListeners[1]);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "provider does not exist " + ex.getMessage());
            } catch (SecurityException ex2) {
                Log.i(TAG, "fail to request location update, ignore", ex2);
            }
            try {
                this.mLocationManager.requestLocationUpdates("gps", 1000L, 0.0f, this.mLocationListeners[0]);
            } catch (IllegalArgumentException ex3) {
                Log.d(TAG, "provider does not exist " + ex3.getMessage());
            } catch (SecurityException ex4) {
                Log.i(TAG, "fail to request location update, ignore", ex4);
            }
            Log.d(TAG, "startReceivingLocationUpdates");
        }
    }

    private void stopReceivingLocationUpdates() {
        if (this.mLocationManager != null) {
            for (int i = 0; i < this.mLocationListeners.length; i++) {
                try {
                    this.mLocationManager.removeUpdates(this.mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
            Log.d(TAG, "stopReceivingLocationUpdates");
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        String mProvider;
        boolean mValid = false;

        public LocationListener(String provider) {
            this.mProvider = provider;
            this.mLastLocation = new Location(this.mProvider);
        }

        @Override
        public void onLocationChanged(Location newLocation) {
            if (newLocation.getLatitude() != 0.0d || newLocation.getLongitude() != 0.0d) {
                if (!this.mValid) {
                    Log.d(LegacyLocationProvider.TAG, "Got first location.");
                }
                this.mLastLocation.set(newLocation);
                this.mValid = true;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
            this.mValid = false;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case 0:
                case 1:
                    this.mValid = false;
                    break;
            }
        }

        public Location current() {
            if (this.mValid) {
                return this.mLastLocation;
            }
            return null;
        }
    }
}
