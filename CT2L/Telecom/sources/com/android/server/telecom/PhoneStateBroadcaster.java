package com.android.server.telecom;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.ITelephonyRegistry;

final class PhoneStateBroadcaster extends CallsManagerListenerBase {
    private int mCurrentState = 0;
    private final ITelephonyRegistry mRegistry = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));

    public PhoneStateBroadcaster() {
        if (this.mRegistry == null) {
            Log.w(this, "TelephonyRegistry is null", new Object[0]);
        }
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        if ((i2 == 3 || i2 == 5 || i2 == 6) && !CallsManager.getInstance().hasRingingCall()) {
            sendPhoneStateChangedBroadcast(call, 2);
        }
    }

    @Override
    public void onCallAdded(Call call) {
        if (call.getState() == 4) {
            sendPhoneStateChangedBroadcast(call, 1);
        }
    }

    @Override
    public void onCallRemoved(Call call) {
        CallsManager callsManager = CallsManager.getInstance();
        int i = 0;
        if (callsManager.hasRingingCall()) {
            i = 1;
        } else if (callsManager.getFirstCallWithState(3, 5, 6) != null) {
            i = 2;
        }
        sendPhoneStateChangedBroadcast(call, i);
    }

    int getCallState() {
        return this.mCurrentState;
    }

    private void sendPhoneStateChangedBroadcast(Call call, int i) {
        if (i != this.mCurrentState) {
            this.mCurrentState = i;
            String schemeSpecificPart = null;
            if (call.getHandle() != null) {
                schemeSpecificPart = call.getHandle().getSchemeSpecificPart();
            }
            try {
                if (this.mRegistry != null) {
                    this.mRegistry.notifyCallState(i, schemeSpecificPart);
                    Log.i(this, "Broadcasted state change: %s", Integer.valueOf(this.mCurrentState));
                }
            } catch (RemoteException e) {
                Log.w(this, "RemoteException when notifying TelephonyRegistry of call state change.", new Object[0]);
            }
        }
    }
}
