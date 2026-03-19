package com.mediatek.camera.common.location;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import com.mediatek.camera.common.permission.PermissionManager;

public class LocationProvider {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(LocationProvider.class.getSimpleName());
    private Context mContext;
    LocationListener[] mLocationListeners = {new LocationListener("gps"), new LocationListener("network")};
    private android.location.LocationManager mLocationManager;
    private PermissionManager mLocationPermission;
    private boolean mRecordLocation;

    public LocationProvider(Context context) {
        this.mContext = context;
        this.mLocationPermission = new PermissionManager((Activity) context);
    }

    public Location getCurrentLocation() {
        if (!this.mRecordLocation) {
            return null;
        }
        for (int i = 0; i < this.mLocationListeners.length; i++) {
            Location locationCurrent = this.mLocationListeners[i].current();
            if (locationCurrent != null) {
                return locationCurrent;
            }
        }
        LogHelper.d(TAG, "No location received yet.");
        return null;
    }

    public void recordLocation(boolean z) {
        if (this.mLocationPermission.checkCameraLocationPermissions() && this.mRecordLocation != z) {
            this.mRecordLocation = z;
            if (z) {
                startReceivingLocationUpdates();
            } else {
                stopReceivingLocationUpdates();
            }
        }
    }

    private void startReceivingLocationUpdates() {
        LogHelper.d(TAG, "startReceivingLocationUpdates ++++");
        if (this.mLocationManager == null) {
            this.mLocationManager = (android.location.LocationManager) this.mContext.getSystemService("location");
        }
        if (this.mLocationManager != null) {
            try {
                this.mLocationManager.requestLocationUpdates("network", 5000L, 0.0f, this.mLocationListeners[1]);
            } catch (IllegalArgumentException e) {
                LogHelper.e(TAG, "provider does not exist " + e.getMessage());
            } catch (SecurityException e2) {
                LogHelper.e(TAG, "fail to request location update, ignore", e2);
            }
            try {
                this.mLocationManager.requestLocationUpdates("gps", 5000L, 0.0f, this.mLocationListeners[0]);
            } catch (IllegalArgumentException e3) {
                LogHelper.e(TAG, "provider does not exist " + e3.getMessage());
            } catch (SecurityException e4) {
                LogHelper.e(TAG, "fail to request location update, ignore", e4);
            }
            LogHelper.d(TAG, "startReceivingLocationUpdates----");
        }
    }

    private void stopReceivingLocationUpdates() {
        if (this.mLocationManager != null) {
            LogHelper.d(TAG, "stopReceivingLocationUpdates++++");
            for (int i = 0; i < this.mLocationListeners.length; i++) {
                try {
                    this.mLocationManager.removeUpdates(this.mLocationListeners[i]);
                } catch (Exception e) {
                    LogHelper.e(TAG, "fail to remove location listners, ignore", e);
                }
            }
            LogHelper.d(TAG, "stopReceivingLocationUpdates----");
        }
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;
        String mProvider;
        boolean mValid = false;

        public LocationListener(String str) {
            this.mProvider = str;
            this.mLastLocation = new Location(this.mProvider);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location.getLatitude() == 0.0d && location.getLongitude() == 0.0d) {
                return;
            }
            this.mLastLocation.set(location);
            this.mValid = true;
        }

        @Override
        public void onProviderEnabled(String str) {
        }

        @Override
        public void onProviderDisabled(String str) {
            this.mValid = false;
        }

        @Override
        public void onStatusChanged(String str, int i, Bundle bundle) {
            switch (i) {
                case 0:
                case Camera2Proxy.TEMPLATE_PREVIEW:
                    this.mValid = false;
                    break;
            }
        }

        public Location current() {
            LogHelper.d(LocationProvider.TAG, "[current],mValid = " + this.mValid);
            if (this.mValid) {
                return this.mLastLocation;
            }
            return null;
        }
    }
}
