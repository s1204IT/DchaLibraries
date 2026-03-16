package com.android.internal.backup;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LocalTransportService extends Service {
    private static LocalTransport sTransport = null;

    @Override
    public void onCreate() {
        if (sTransport == null) {
            sTransport = new LocalTransport(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sTransport.getBinder();
    }
}
