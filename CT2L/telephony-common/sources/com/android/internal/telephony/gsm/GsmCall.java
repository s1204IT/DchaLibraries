package com.android.internal.telephony.gsm;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.DriverCall;
import com.android.internal.telephony.Phone;
import java.util.List;

class GsmCall extends Call {
    GsmCallTracker mOwner;

    GsmCall(GsmCallTracker owner) {
        this.mOwner = owner;
    }

    public void dispose() {
    }

    @Override
    public List<Connection> getConnections() {
        return this.mConnections;
    }

    @Override
    public Phone getPhone() {
        return this.mOwner.mPhone;
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

    void attach(Connection conn, DriverCall dc) {
        this.mConnections.add(conn);
        this.mState = stateFromDCState(dc.state);
    }

    void attachFake(Connection conn, Call.State state) {
        this.mConnections.add(conn);
        this.mState = state;
    }

    boolean connectionDisconnected(GsmConnection conn) {
        if (this.mState != Call.State.DISCONNECTED) {
            boolean hasOnlyDisconnectedConnections = true;
            int i = 0;
            int s = this.mConnections.size();
            while (true) {
                if (i >= s) {
                    break;
                }
                if (this.mConnections.get(i).getState() == Call.State.DISCONNECTED) {
                    i++;
                } else {
                    hasOnlyDisconnectedConnections = false;
                    break;
                }
            }
            if (hasOnlyDisconnectedConnections) {
                this.mState = Call.State.DISCONNECTED;
                return true;
            }
        }
        return false;
    }

    void detach(GsmConnection conn) {
        this.mConnections.remove(conn);
        if (this.mConnections.size() == 0) {
            this.mState = Call.State.IDLE;
        }
    }

    boolean update(GsmConnection conn, DriverCall dc) {
        Call.State newState = stateFromDCState(dc.state);
        if (newState == this.mState) {
            return false;
        }
        this.mState = newState;
        return true;
    }

    boolean isFull() {
        return this.mConnections.size() == 5;
    }

    void onHangupLocal() {
        int s = this.mConnections.size();
        for (int i = 0; i < s; i++) {
            GsmConnection cn = (GsmConnection) this.mConnections.get(i);
            cn.onHangupLocal();
        }
        this.mState = Call.State.DISCONNECTING;
    }

    synchronized void clearDisconnected() {
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            GsmConnection cn = (GsmConnection) this.mConnections.get(i);
            if (cn.getState() == Call.State.DISCONNECTED) {
                this.mConnections.remove(i);
            }
        }
        if (this.mConnections.size() == 0) {
            this.mState = Call.State.IDLE;
        }
    }
}
