package com.android.settingslib.wrapper;

import android.location.LocationManager;
import android.os.UserHandle;

public class LocationManagerWrapper {
    private LocationManager mLocationManager;

    public LocationManagerWrapper(LocationManager locationManager) {
        this.mLocationManager = locationManager;
    }

    public void setLocationEnabledForUser(boolean z, UserHandle userHandle) {
        this.mLocationManager.setLocationEnabledForUser(z, userHandle);
    }
}
