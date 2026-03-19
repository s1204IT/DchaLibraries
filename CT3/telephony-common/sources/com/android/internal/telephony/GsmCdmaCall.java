package com.android.internal.telephony;

import com.android.internal.telephony.Call;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import java.util.List;

public class GsmCdmaCall extends Call {
    GsmCdmaCallTracker mOwner;
    GsmVTProvider mVTProvider = null;

    public GsmCdmaCall(GsmCdmaCallTracker owner) {
        this.mOwner = owner;
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
        int discConn = 0;
        for (int j = this.mConnections.size() - 1; j >= 0; j--) {
            GsmCdmaConnection cn = (GsmCdmaConnection) this.mConnections.get(j);
            if (cn.getState() == Call.State.DISCONNECTED) {
                discConn++;
            }
        }
        if (this.mConnections.size() <= 1 || this.mConnections.size() <= 1 || this.mConnections.size() - discConn <= 1 || getState() == Call.State.DIALING) {
            return false;
        }
        return true;
    }

    @Override
    public void hangup() throws CallStateException {
        this.mOwner.hangup(this);
    }

    public String toString() {
        return this.mState.toString();
    }

    public void attach(Connection conn, DriverCall dc) {
        this.mConnections.add(conn);
        this.mState = stateFromDCState(dc.state);
    }

    public void attachFake(Connection conn, Call.State state) {
        this.mConnections.add(conn);
        this.mState = state;
    }

    public boolean connectionDisconnected(GsmCdmaConnection conn) {
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
            return false;
        }
        return false;
    }

    public void detach(GsmCdmaConnection conn) {
        this.mConnections.remove(conn);
        if (this.mConnections.size() != 0) {
            return;
        }
        this.mState = Call.State.IDLE;
    }

    boolean update(GsmCdmaConnection conn, DriverCall dc) {
        Call.State newState = stateFromDCState(dc.state);
        if (newState == this.mState) {
            return false;
        }
        this.mState = newState;
        return true;
    }

    boolean isFull() {
        return this.mConnections.size() == this.mOwner.getMaxConnectionsPerCall();
    }

    void onHangupLocal() {
        int s = this.mConnections.size();
        for (int i = 0; i < s; i++) {
            GsmCdmaConnection cn = (GsmCdmaConnection) this.mConnections.get(i);
            cn.onHangupLocal();
        }
        if (this.mConnections.size() == 0 || !getState().isAlive()) {
            return;
        }
        this.mState = Call.State.DISCONNECTING;
    }
}
