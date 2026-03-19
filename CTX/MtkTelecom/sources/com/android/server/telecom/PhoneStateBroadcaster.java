package com.android.server.telecom;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telecom.Log;
import com.android.internal.telephony.ITelephonyRegistry;

final class PhoneStateBroadcaster extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private int mCurrentState = 0;
    private final ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));

    public PhoneStateBroadcaster(CallsManager callsManager) {
        this.mCallsManager = callsManager;
        if (this.mRegistry == null) {
            Log.w(this, "TelephonyRegistry is null", new Object[0]);
        }
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if (call.isExternalCall()) {
            return;
        }
        updateStates(call);
    }

    @Override
    public void onCallAdded(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        updateStates(call);
    }

    @Override
    public void onCallRemoved(Call call) {
        if (call.isExternalCall()) {
            return;
        }
        updateStates(call);
    }

    @Override
    public void onExternalCallChanged(Call call, boolean z) {
        updateStates(call);
    }

    private void updateStates(Call call) {
        int i;
        if (this.mCallsManager.hasRingingCall()) {
            i = 1;
        } else if (this.mCallsManager.getFirstCallWithState(3, 10, 5, 6) != null) {
            i = 2;
        } else {
            i = 0;
        }
        sendPhoneStateChangedBroadcast(call, i);
    }

    int getCallState() {
        return this.mCurrentState;
    }

    private void sendPhoneStateChangedBroadcast(Call call, int i) {
        if (i == this.mCurrentState) {
            return;
        }
        this.mCurrentState = i;
        String schemeSpecificPart = null;
        if (!call.isSelfManaged() && call.getHandle() != null) {
            schemeSpecificPart = call.getHandle().getSchemeSpecificPart();
        }
        try {
            if (this.mRegistry != null) {
                this.mRegistry.notifyCallState(i, schemeSpecificPart);
                Log.i(this, "Broadcasted state change: %s", new Object[]{Integer.valueOf(this.mCurrentState)});
            }
        } catch (RemoteException e) {
            Log.w(this, "RemoteException when notifying TelephonyRegistry of call state change.", new Object[0]);
        }
    }
}
