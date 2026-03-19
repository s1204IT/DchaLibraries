package com.android.internal.telephony;

import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.internal.telephony.DriverCall;
import java.util.ArrayList;
import java.util.List;

public abstract class Call {

    private static final int[] f1comandroidinternaltelephonyDriverCall$StateSwitchesValues = null;
    protected final String LOG_TAG = "Call";
    public State mState = State.IDLE;
    public ArrayList<Connection> mConnections = new ArrayList<>();

    private static int[] m1xd9c92f69() {
        if (f1comandroidinternaltelephonyDriverCall$StateSwitchesValues != null) {
            return f1comandroidinternaltelephonyDriverCall$StateSwitchesValues;
        }
        int[] iArr = new int[DriverCall.State.valuesCustom().length];
        try {
            iArr[DriverCall.State.ACTIVE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DriverCall.State.ALERTING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DriverCall.State.DIALING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DriverCall.State.HOLDING.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DriverCall.State.INCOMING.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DriverCall.State.WAITING.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        f1comandroidinternaltelephonyDriverCall$StateSwitchesValues = iArr;
        return iArr;
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

        public static State[] valuesCustom() {
            return values();
        }

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

    public static State stateFromDCState(DriverCall.State dcState) {
        switch (m1xd9c92f69()[dcState.ordinal()]) {
            case 1:
                return State.ACTIVE;
            case 2:
                return State.ALERTING;
            case 3:
                return State.DIALING;
            case 4:
                return State.HOLDING;
            case 5:
                return State.INCOMING;
            case 6:
                return State.WAITING;
            default:
                throw new RuntimeException("illegal call state:" + dcState);
        }
    }

    public enum SrvccState {
        NONE,
        STARTED,
        COMPLETED,
        FAILED,
        CANCELED;

        public static SrvccState[] valuesCustom() {
            return values();
        }
    }

    public boolean hasConnection(Connection c) {
        return c.getCall() == this;
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
        long time = Long.MAX_VALUE;
        Connection earliest = null;
        List<Connection> l = getConnections();
        if (l.size() == 0) {
            return null;
        }
        int s = l.size();
        for (int i = 0; i < s; i++) {
            Connection c = l.get(i);
            long t = c.getCreateTime();
            if (t < time) {
                earliest = c;
                time = t;
            }
        }
        return earliest;
    }

    public long getEarliestCreateTime() {
        long time = Long.MAX_VALUE;
        List<Connection> l = getConnections();
        if (l.size() == 0) {
            return 0L;
        }
        int s = l.size();
        for (int i = 0; i < s; i++) {
            Connection c = l.get(i);
            long t = c.getCreateTime();
            if (t < time) {
                time = t;
            }
        }
        return time;
    }

    public long getEarliestConnectTime() {
        long time = Long.MAX_VALUE;
        List<Connection> l = getConnections();
        if (l.size() == 0) {
            return 0L;
        }
        int s = l.size();
        for (int i = 0; i < s; i++) {
            Connection c = l.get(i);
            long t = c.getConnectTime();
            if (t < time) {
                time = t;
            }
        }
        return time;
    }

    public boolean isDialingOrAlerting() {
        return getState().isDialing();
    }

    public boolean isRinging() {
        return getState().isRinging();
    }

    public Connection getLatestConnection() {
        List<Connection> l = getConnections();
        if (l.size() == 0) {
            return null;
        }
        long time = 0;
        Connection latest = null;
        int s = l.size();
        for (int i = 0; i < s; i++) {
            Connection c = l.get(i);
            long t = c.getCreateTime();
            if (t > time) {
                latest = c;
                time = t;
            }
        }
        return latest;
    }

    public void hangupIfAlive() {
        if (!getState().isAlive()) {
            return;
        }
        try {
            hangup();
        } catch (CallStateException ex) {
            Rlog.w("Call", " hangupIfActive: caught " + ex);
        }
    }

    public void clearDisconnected() {
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            Connection c = this.mConnections.get(i);
            if (c.getState() == State.DISCONNECTED) {
                this.mConnections.remove(i);
            }
        }
        if (this.mConnections.size() != 0) {
            return;
        }
        setState(State.IDLE);
    }

    protected void setState(State newState) {
        this.mState = newState;
    }
}
