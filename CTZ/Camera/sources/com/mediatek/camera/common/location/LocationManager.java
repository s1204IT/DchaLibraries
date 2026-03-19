package com.mediatek.camera.common.location;

import android.content.Context;
import android.location.Location;

public class LocationManager {
    LocationProvider mLocationProvider;

    public LocationManager(Context context) {
        this.mLocationProvider = new LocationProvider(context);
    }

    public void recordLocation(boolean z) {
        this.mLocationProvider.recordLocation(z);
    }

    public Location getCurrentLocation() {
        return this.mLocationProvider.getCurrentLocation();
    }
}
