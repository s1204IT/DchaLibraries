package com.mediatek.internal.telephony;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.GsmCdmaCall;
import com.android.internal.telephony.GsmCdmaCallTracker;
import com.android.internal.telephony.GsmCdmaConnection;

public class MtkGsmCdmaCall extends GsmCdmaCall {
    public MtkGsmCdmaCall(GsmCdmaCallTracker gsmCdmaCallTracker) {
        super(gsmCdmaCallTracker);
    }

    public boolean isMultiparty() {
        int i = 0;
        for (int size = this.mConnections.size() - 1; size >= 0; size--) {
            if (((GsmCdmaConnection) this.mConnections.get(size)).getState() == Call.State.DISCONNECTED) {
                i++;
            }
        }
        return this.mConnections.size() > 1 && this.mConnections.size() > 1 && this.mConnections.size() - i > 1 && getState() != Call.State.DIALING;
    }

    public void onHangupLocal() {
        int size = this.mConnections.size();
        for (int i = 0; i < size; i++) {
            ((GsmCdmaConnection) this.mConnections.get(i)).onHangupLocal();
        }
        if (this.mConnections.size() != 0 && getState().isAlive()) {
            this.mState = Call.State.DISCONNECTING;
        }
    }
}
