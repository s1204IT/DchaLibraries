package com.android.server.telecom;

import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;

public class ProximitySensorManager extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final TelecomWakeLock mTelecomWakeLock;

    public ProximitySensorManager(TelecomWakeLock telecomWakeLock, CallsManager callsManager) {
        this.mTelecomWakeLock = telecomWakeLock;
        this.mCallsManager = callsManager;
        Log.d(this, "onCreate: mProximityWakeLock: ", new Object[]{this.mTelecomWakeLock});
    }

    @Override
    public void onCallRemoved(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        if (this.mCallsManager.getCalls().isEmpty()) {
            Log.i(this, "All calls removed, resetting proximity sensor to default state", new Object[0]);
            turnOff(false);
        }
        super.onCallRemoved(call);
    }

    @VisibleForTesting
    public void turnOn() {
        if (this.mCallsManager.getCalls().isEmpty()) {
            Log.w(this, "Asking to turn on prox sensor without a call? I don't think so.", new Object[0]);
        } else {
            this.mTelecomWakeLock.acquire();
        }
    }

    @VisibleForTesting
    public void turnOff(boolean z) {
        this.mTelecomWakeLock.release(!z ? 1 : 0);
    }
}
