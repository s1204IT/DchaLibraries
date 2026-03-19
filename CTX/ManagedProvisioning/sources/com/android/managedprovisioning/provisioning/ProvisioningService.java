package com.android.managedprovisioning.provisioning;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ProvisioningService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
