package com.android.server.telecom;

import android.telecom.Log;
import com.android.internal.annotations.VisibleForTesting;

@VisibleForTesting
public class InCallWakeLockController extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final TelecomWakeLock mTelecomWakeLock;

    @VisibleForTesting
    public InCallWakeLockController(TelecomWakeLock telecomWakeLock, CallsManager callsManager) {
        this.mCallsManager = callsManager;
        this.mTelecomWakeLock = telecomWakeLock;
        this.mTelecomWakeLock.setReferenceCounted(false);
    }

    @Override
    public void onCallAdded(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        handleWakeLock();
    }

    @Override
    public void onCallRemoved(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        handleWakeLock();
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (call.isExternalCall()) {
            return;
        }
        if (call != null && ((call.getState() == 7 || call.getState() == 9) && call.getDisconnectCause().getCode() != 6 && call.getDisconnectCause().getCode() != 4)) {
            this.mTelecomWakeLock.wakeUpScreen();
        }
        handleWakeLock();
    }

    private void handleWakeLock() {
        Call ringingCall = this.mCallsManager.getRingingCall();
        Call dialingCall = this.mCallsManager.getDialingCall();
        if (ringingCall == null && dialingCall == null) {
            this.mTelecomWakeLock.release(0);
            Log.i(this, "Releasing full wake lock", new Object[0]);
        } else {
            this.mTelecomWakeLock.acquire();
            Log.i(this, "Acquiring full wake lock", new Object[0]);
        }
    }
}
