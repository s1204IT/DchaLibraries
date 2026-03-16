package com.android.location.fused;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FusedLocationService extends Service {
    private FusedLocationProvider mProvider;

    @Override
    public IBinder onBind(Intent intent) {
        if (this.mProvider == null) {
            this.mProvider = new FusedLocationProvider(getApplicationContext());
        }
        return this.mProvider.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (this.mProvider != null) {
            this.mProvider.onDisable();
            return false;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        this.mProvider = null;
    }
}
