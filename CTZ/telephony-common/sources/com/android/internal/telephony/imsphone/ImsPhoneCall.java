package com.android.internal.telephony.imsphone;

import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import java.util.Iterator;
import java.util.List;

public class ImsPhoneCall extends Call {
    public static final String CONTEXT_BACKGROUND = "BG";
    public static final String CONTEXT_FOREGROUND = "FG";
    public static final String CONTEXT_HANDOVER = "HO";
    public static final String CONTEXT_RINGING = "RG";
    public static final String CONTEXT_UNKNOWN = "UK";
    private static final boolean FORCE_DEBUG = false;
    protected final String mCallContext;
    protected ImsPhoneCallTracker mOwner;
    protected boolean mRingbackTonePlayed;
    private static final String LOG_TAG = "ImsPhoneCall";
    protected static final boolean DBG = Rlog.isLoggable(LOG_TAG, 3);
    protected static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);

    protected ImsPhoneCall() {
        this.mRingbackTonePlayed = false;
        this.mCallContext = CONTEXT_UNKNOWN;
    }

    public ImsPhoneCall(ImsPhoneCallTracker imsPhoneCallTracker, String str) {
        this.mRingbackTonePlayed = false;
        this.mOwner = imsPhoneCallTracker;
        this.mCallContext = str;
    }

    public void dispose() {
        int i = 0;
        try {
            this.mOwner.hangup(this);
            int size = this.mConnections.size();
            while (i < size) {
                ((ImsPhoneConnection) this.mConnections.get(i)).onDisconnect(14);
                i++;
            }
        } catch (CallStateException e) {
            int size2 = this.mConnections.size();
            while (i < size2) {
                ((ImsPhoneConnection) this.mConnections.get(i)).onDisconnect(14);
                i++;
            }
        } catch (Throwable th) {
            int size3 = this.mConnections.size();
            while (i < size3) {
                ((ImsPhoneConnection) this.mConnections.get(i)).onDisconnect(14);
                i++;
            }
            throw th;
        }
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
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return false;
        }
        return imsCall.isMultiparty();
    }

    @Override
    public void hangup() throws CallStateException {
        this.mOwner.hangup(this);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsPhoneCall ");
        sb.append(this.mCallContext);
        sb.append(" state: ");
        sb.append(this.mState.toString());
        sb.append(" ");
        if (this.mConnections.size() > 1) {
            sb.append(" ERROR_MULTIPLE ");
        }
        Iterator<Connection> it = this.mConnections.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public List<ConferenceParticipant> getConferenceParticipants() {
        ImsCall imsCall = getImsCall();
        if (imsCall == null) {
            return null;
        }
        return imsCall.getConferenceParticipants();
    }

    public void attach(Connection connection) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + this.mCallContext + " conn = " + connection);
        }
        clearDisconnected();
        this.mConnections.add(connection);
        this.mOwner.logState();
    }

    public void attach(Connection connection, Call.State state) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + this.mCallContext + " state = " + state.toString());
        }
        attach(connection);
        this.mState = state;
    }

    public void attachFake(Connection connection, Call.State state) {
        attach(connection, state);
    }

    public boolean connectionDisconnected(ImsPhoneConnection imsPhoneConnection) {
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
                if (VDBG) {
                    Rlog.v(LOG_TAG, "connectionDisconnected : " + this.mCallContext + " state = " + this.mState);
                }
                return true;
            }
        }
        return false;
    }

    public void detach(ImsPhoneConnection imsPhoneConnection) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "detach : " + this.mCallContext + " conn = " + imsPhoneConnection);
        }
        this.mConnections.remove(imsPhoneConnection);
        clearDisconnected();
        this.mOwner.logState();
    }

    boolean isFull() {
        return this.mConnections.size() == 5;
    }

    public void onHangupLocal() {
        int size = this.mConnections.size();
        for (int i = 0; i < size; i++) {
            ((ImsPhoneConnection) this.mConnections.get(i)).onHangupLocal();
        }
        this.mState = Call.State.DISCONNECTING;
        if (VDBG) {
            Rlog.v(LOG_TAG, "onHangupLocal : " + this.mCallContext + " state = " + this.mState);
        }
    }

    @VisibleForTesting
    public ImsPhoneConnection getFirstConnection() {
        if (this.mConnections.size() == 0) {
            return null;
        }
        return (ImsPhoneConnection) this.mConnections.get(0);
    }

    void setMute(boolean z) {
        ImsCall imsCall = getFirstConnection() == null ? null : getFirstConnection().getImsCall();
        if (imsCall != null) {
            try {
                imsCall.setMute(z);
            } catch (ImsException e) {
                Rlog.e(LOG_TAG, "setMute failed : " + e.getMessage());
            }
        }
    }

    public void merge(ImsPhoneCall imsPhoneCall, Call.State state) {
        ImsPhoneConnection firstConnection = getFirstConnection();
        if (firstConnection != null) {
            long conferenceConnectTime = firstConnection.getConferenceConnectTime();
            if (conferenceConnectTime > 0) {
                firstConnection.setConnectTime(conferenceConnectTime);
                firstConnection.setConnectTimeReal(firstConnection.getConnectTimeReal());
            } else if (DBG) {
                Rlog.d(LOG_TAG, "merge: conference connect time is 0");
            }
            setConferenceAsHostIfNecessary(firstConnection);
        }
        if (DBG) {
            Rlog.d(LOG_TAG, "merge(" + this.mCallContext + "): " + imsPhoneCall + "state = " + state);
        }
    }

    @VisibleForTesting
    public ImsCall getImsCall() {
        if (getFirstConnection() == null) {
            return null;
        }
        return getFirstConnection().getImsCall();
    }

    public static boolean isLocalTone(ImsCall imsCall) {
        if (imsCall == null || imsCall.getCallProfile() == null || imsCall.getCallProfile().mMediaProfile == null || imsCall.getCallProfile().mMediaProfile.mAudioDirection != 0) {
            return false;
        }
        return true;
    }

    public boolean update(ImsPhoneConnection imsPhoneConnection, ImsCall imsCall, Call.State state) {
        Call.State state2 = this.mState;
        boolean z = true;
        if (state == Call.State.ALERTING) {
            if (this.mRingbackTonePlayed && !isLocalTone(imsCall)) {
                this.mOwner.mPhone.stopRingbackTone();
                this.mRingbackTonePlayed = false;
            } else if (!this.mRingbackTonePlayed && isLocalTone(imsCall)) {
                this.mOwner.mPhone.startRingbackTone();
                this.mRingbackTonePlayed = true;
            }
        } else if (this.mRingbackTonePlayed) {
            this.mOwner.mPhone.stopRingbackTone();
            this.mRingbackTonePlayed = false;
        }
        if (state != this.mState && state != Call.State.DISCONNECTED) {
            this.mState = state;
        } else if (state != Call.State.DISCONNECTED) {
            z = false;
        }
        if (VDBG) {
            Rlog.v(LOG_TAG, "update : " + this.mCallContext + " state: " + state2 + " --> " + this.mState);
        }
        return z;
    }

    ImsPhoneConnection getHandoverConnection() {
        return (ImsPhoneConnection) getEarliestConnection();
    }

    public void switchWith(ImsPhoneCall imsPhoneCall) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "switchWith : switchCall = " + this + " withCall = " + imsPhoneCall);
        }
        synchronized (ImsPhoneCall.class) {
            ImsPhoneCall imsPhoneCallMakeTempImsPhoneCall = makeTempImsPhoneCall();
            imsPhoneCallMakeTempImsPhoneCall.takeOver(this);
            takeOver(imsPhoneCall);
            imsPhoneCall.takeOver(imsPhoneCallMakeTempImsPhoneCall);
        }
        this.mOwner.logState();
    }

    public void takeOver(ImsPhoneCall imsPhoneCall) {
        this.mConnections = imsPhoneCall.mConnections;
        this.mState = imsPhoneCall.mState;
        Iterator<Connection> it = this.mConnections.iterator();
        while (it.hasNext()) {
            ((ImsPhoneConnection) it.next()).changeParent(this);
        }
    }

    protected void setConferenceAsHostIfNecessary(ImsPhoneConnection imsPhoneConnection) {
    }

    protected ImsPhoneCall makeTempImsPhoneCall() {
        return new ImsPhoneCall();
    }
}
