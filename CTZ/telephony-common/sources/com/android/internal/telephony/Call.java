package com.android.internal.telephony;

import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.internal.telephony.DriverCall;
import java.util.ArrayList;
import java.util.List;

public abstract class Call {
    protected final String LOG_TAG = "Call";
    public State mState = State.IDLE;
    public ArrayList<Connection> mConnections = new ArrayList<>();

    public enum SrvccState {
        NONE,
        STARTED,
        COMPLETED,
        FAILED,
        CANCELED
    }

    public abstract List<Connection> getConnections();

    public abstract Phone getPhone();

    public abstract void hangup() throws CallStateException;

    public abstract boolean isMultiparty();

    public enum State {
        IDLE,
        ACTIVE,
        HOLDING,
        DIALING,
        ALERTING,
        INCOMING,
        WAITING,
        DISCONNECTED,
        DISCONNECTING;

        public boolean isAlive() {
            return (this == IDLE || this == DISCONNECTED || this == DISCONNECTING) ? false : true;
        }

        public boolean isRinging() {
            return this == INCOMING || this == WAITING;
        }

        public boolean isDialing() {
            return this == DIALING || this == ALERTING;
        }
    }

    public static State stateFromDCState(DriverCall.State state) {
        switch (state) {
            case ACTIVE:
                return State.ACTIVE;
            case HOLDING:
                return State.HOLDING;
            case DIALING:
                return State.DIALING;
            case ALERTING:
                return State.ALERTING;
            case INCOMING:
                return State.INCOMING;
            case WAITING:
                return State.WAITING;
            default:
                throw new RuntimeException("illegal call state:" + state);
        }
    }

    public boolean hasConnection(Connection connection) {
        return connection.getCall() == this;
    }

    public boolean hasConnections() {
        List<Connection> connections = getConnections();
        return connections != null && connections.size() > 0;
    }

    public State getState() {
        return this.mState;
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        return null;
    }

    public boolean isIdle() {
        return !getState().isAlive();
    }

    public Connection getEarliestConnection() {
        List<Connection> connections = getConnections();
        Connection connection = null;
        if (connections.size() == 0) {
            return null;
        }
        int size = connections.size();
        long j = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            Connection connection2 = connections.get(i);
            long createTime = connection2.getCreateTime();
            if (createTime < j) {
                connection = connection2;
                j = createTime;
            }
        }
        return connection;
    }

    public long getEarliestCreateTime() {
        List<Connection> connections = getConnections();
        if (connections.size() == 0) {
            return 0L;
        }
        int size = connections.size();
        long j = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            long createTime = connections.get(i).getCreateTime();
            if (createTime < j) {
                j = createTime;
            }
        }
        return j;
    }

    public long getEarliestConnectTime() {
        List<Connection> connections = getConnections();
        if (connections.size() == 0) {
            return 0L;
        }
        int size = connections.size();
        long j = Long.MAX_VALUE;
        for (int i = 0; i < size; i++) {
            long connectTime = connections.get(i).getConnectTime();
            if (connectTime < j) {
                j = connectTime;
            }
        }
        return j;
    }

    public boolean isDialingOrAlerting() {
        return getState().isDialing();
    }

    public boolean isRinging() {
        return getState().isRinging();
    }

    public Connection getLatestConnection() {
        List<Connection> connections = getConnections();
        Connection connection = null;
        if (connections.size() == 0) {
            return null;
        }
        long j = 0;
        int size = connections.size();
        for (int i = 0; i < size; i++) {
            Connection connection2 = connections.get(i);
            long createTime = connection2.getCreateTime();
            if (createTime > j) {
                connection = connection2;
                j = createTime;
            }
        }
        return connection;
    }

    public void hangupIfAlive() {
        if (getState().isAlive()) {
            try {
                hangup();
            } catch (CallStateException e) {
                Rlog.w("Call", " hangupIfActive: caught " + e);
            }
        }
    }

    public void clearDisconnected() {
        for (int size = this.mConnections.size() - 1; size >= 0; size--) {
            if (this.mConnections.get(size).getState() == State.DISCONNECTED) {
                this.mConnections.remove(size);
            }
        }
        if (this.mConnections.size() == 0) {
            setState(State.IDLE);
        }
    }

    protected void setState(State state) {
        this.mState = state;
    }
}
