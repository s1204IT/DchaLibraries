package com.android.server.telecom.components;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.android.server.telecom.TelecomSystem;

public final class BluetoothPhoneService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        IBinder binder;
        synchronized (getTelecomSystem().getLock()) {
            binder = getTelecomSystem().getBluetoothPhoneServiceImpl().getBinder();
        }
        return binder;
    }

    public TelecomSystem getTelecomSystem() {
        return TelecomSystem.getInstance();
    }
}
