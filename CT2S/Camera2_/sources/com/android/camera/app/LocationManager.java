package com.android.camera.app;

import android.content.Context;
import android.location.Location;
import com.android.camera.debug.Log;

public class LocationManager {
    private static final Log.Tag TAG = new Log.Tag("LocationManager");
    LocationProvider mLocationProvider;
    private boolean mRecordLocation;

    public LocationManager(Context context) {
        Log.d(TAG, "Using legacy location provider.");
        LegacyLocationProvider llp = new LegacyLocationProvider(context);
        this.mLocationProvider = llp;
    }

    public void recordLocation(boolean recordLocation) {
        this.mRecordLocation = recordLocation;
        this.mLocationProvider.recordLocation(this.mRecordLocation);
    }

    public Location getCurrentLocation() {
        return this.mLocationProvider.getCurrentLocation();
    }

    public void disconnect() {
        this.mLocationProvider.disconnect();
    }
}
