package com.android.managedprovisioning.ota;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class OtaService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
