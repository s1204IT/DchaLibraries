package com.android.internal.telephony;

import com.android.internal.telephony.Call;
import java.util.List;

public class GsmCdmaCall extends Call {
    GsmCdmaCallTracker mOwner;

    public GsmCdmaCall(GsmCdmaCallTracker gsmCdmaCallTracker) {
        this.mOwner = gsmCdmaCallTracker;
    }

    @Override
    public List<Connection> getConnections() {
        return this.mConnections;
    }

    @Override
    public Phone getPhone() {
        return this.mOwner.getPhone();
    }

    @Override
    public boolean isMultiparty() {
        return this.mConnections.size() > 1;
    }

    @Override
    public void hangup() throws CallStateException {
        this.mOwner.hangup(this);
    }

    public String toString() {
        return this.mState.toString();
    }

    public void attach(Connection connection, DriverCall driverCall) {
        this.mConnections.add(connection);
        this.mState = stateFromDCState(driverCall.state);
    }

    public void attachFake(Connection connection, Call.State state) {
        this.mConnections.add(connection);
        this.mState = state;
    }

    public boolean connectionDisconnected(GsmCdmaConnection gsmCdmaConnection) {
        boolean z;
        if (this.mState != Call.State.DISCONNECTED) {
            int size = this.mConnections.size();
            int i = 0;
            while (true) {
                if (i < size) {
                    if (this.mConnections.get(i).getState() == Call.State.DISCONNECTED) {
                        i++;
                    } else {
                        z = false;
                        break;
                    }
                } else {
                    z = true;
                    break;
                }
            }
            if (z) {
                this.mState = Call.State.DISCONNECTED;
                return true;
            }
        }
        return false;
    }

    public void detach(GsmCdmaConnection gsmCdmaConnection) {
        this.mConnections.remove(gsmCdmaConnection);
        if (this.mConnections.size() == 0) {
            this.mState = Call.State.IDLE;
        }
    }

    public boolean update(GsmCdmaConnection gsmCdmaConnection, DriverCall driverCall) {
        Call.State stateStateFromDCState = stateFromDCState(driverCall.state);
        if (stateStateFromDCState != this.mState) {
            this.mState = stateStateFromDCState;
            return true;
        }
        return false;
    }

    boolean isFull() {
        return this.mConnections.size() == this.mOwner.getMaxConnectionsPerCall();
    }

    public void onHangupLocal() {
        int size = this.mConnections.size();
        for (int i = 0; i < size; i++) {
            ((GsmCdmaConnection) this.mConnections.get(i)).onHangupLocal();
        }
        this.mState = Call.State.DISCONNECTING;
    }
}
