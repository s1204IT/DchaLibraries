package com.android.internal.telephony.imsphone;

import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.ims.ImsCall;
import com.android.ims.ImsException;
import com.android.ims.ImsStreamMediaProfile;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import java.util.List;

public class ImsPhoneCall extends Call {
    public static final String CONTEXT_BACKGROUND = "BG";
    public static final String CONTEXT_FOREGROUND = "FG";
    public static final String CONTEXT_HANDOVER = "HO";
    public static final String CONTEXT_RINGING = "RG";
    public static final String CONTEXT_UNKNOWN = "UK";
    private static final boolean FORCE_DEBUG = false;
    private final String mCallContext;
    ImsPhoneCallTracker mOwner;
    private boolean mRingbackTonePlayed;
    private static final String LOG_TAG = "ImsPhoneCall";
    private static final boolean DBG = Rlog.isLoggable(LOG_TAG, 3);
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);

    ImsPhoneCall() {
        this.mRingbackTonePlayed = FORCE_DEBUG;
        this.mCallContext = CONTEXT_UNKNOWN;
    }

    public ImsPhoneCall(ImsPhoneCallTracker owner, String context) {
        this.mRingbackTonePlayed = FORCE_DEBUG;
        this.mOwner = owner;
        this.mCallContext = context;
    }

    public void dispose() {
        try {
            this.mOwner.hangup(this);
            int s = this.mConnections.size();
            for (int i = 0; i < s; i++) {
                ImsPhoneConnection c = (ImsPhoneConnection) this.mConnections.get(i);
                c.onDisconnect(14);
            }
        } catch (CallStateException e) {
            int s2 = this.mConnections.size();
            for (int i2 = 0; i2 < s2; i2++) {
                ImsPhoneConnection c2 = (ImsPhoneConnection) this.mConnections.get(i2);
                c2.onDisconnect(14);
            }
        } catch (Throwable th) {
            int s3 = this.mConnections.size();
            for (int i3 = 0; i3 < s3; i3++) {
                ImsPhoneConnection c3 = (ImsPhoneConnection) this.mConnections.get(i3);
                c3.onDisconnect(14);
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
            return FORCE_DEBUG;
        }
        return imsCall.isMultiparty();
    }

    @Override
    public void hangup() throws CallStateException {
        if (this.mOwner != null) {
            this.mOwner.logDebugMessagesWithOpFormat("CC", "Hangup", getFirstConnection(), "ImsphoneCall.hangup");
        }
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
        for (Connection conn : this.mConnections) {
            sb.append(conn);
            sb.append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public List<ConferenceParticipant> getConferenceParticipants() {
        ImsCall call = getImsCall();
        if (call == null) {
            return null;
        }
        return call.getConferenceParticipants();
    }

    public void attach(Connection conn) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + this.mCallContext + " conn = " + conn);
        }
        clearDisconnected();
        this.mConnections.add(conn);
        this.mOwner.logState();
    }

    public void attach(Connection conn, Call.State state) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "attach : " + this.mCallContext + " state = " + state.toString());
        }
        attach(conn);
        this.mState = state;
    }

    public void attachFake(Connection conn, Call.State state) {
        attach(conn, state);
    }

    public boolean connectionDisconnected(ImsPhoneConnection conn) {
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
                    hasOnlyDisconnectedConnections = FORCE_DEBUG;
                    break;
                }
            }
            if (hasOnlyDisconnectedConnections) {
                this.mState = Call.State.DISCONNECTED;
                return true;
            }
            return FORCE_DEBUG;
        }
        return FORCE_DEBUG;
    }

    public void detach(ImsPhoneConnection conn) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "detach : " + this.mCallContext + " conn = " + conn);
        }
        this.mConnections.remove(conn);
        clearDisconnected();
        this.mOwner.logState();
    }

    boolean isFull() {
        if (this.mConnections.size() == 5) {
            return true;
        }
        return FORCE_DEBUG;
    }

    void onHangupLocal() {
        int s = this.mConnections.size();
        for (int i = 0; i < s; i++) {
            ImsPhoneConnection cn = (ImsPhoneConnection) this.mConnections.get(i);
            cn.onHangupLocal();
        }
        this.mState = Call.State.DISCONNECTING;
    }

    ImsPhoneConnection getFirstConnection() {
        if (this.mConnections.size() == 0) {
            return null;
        }
        return (ImsPhoneConnection) this.mConnections.get(0);
    }

    void setMute(boolean mute) {
        ImsCall imsCall = getFirstConnection() != null ? getFirstConnection().getImsCall() : null;
        if (imsCall == null) {
            return;
        }
        try {
            imsCall.setMute(mute);
        } catch (ImsException e) {
            Rlog.e(LOG_TAG, "setMute failed : " + e.getMessage());
        }
    }

    void merge(ImsPhoneCall that, Call.State state) {
        ImsPhoneConnection imsPhoneConnection = getFirstConnection();
        if (imsPhoneConnection != null) {
            long conferenceConnectTime = imsPhoneConnection.getConferenceConnectTime();
            if (conferenceConnectTime > 0) {
                imsPhoneConnection.setConnectTime(conferenceConnectTime);
            } else if (DBG) {
                Rlog.d(LOG_TAG, "merge: conference connect time is 0");
            }
            imsPhoneConnection.setConferenceAsHost();
        }
        if (!DBG) {
            return;
        }
        Rlog.d(LOG_TAG, "merge(" + this.mCallContext + "): " + that + "state = " + state);
    }

    public ImsCall getImsCall() {
        if (getFirstConnection() == null) {
            return null;
        }
        return getFirstConnection().getImsCall();
    }

    static boolean isLocalTone(ImsCall imsCall) {
        if (imsCall == null || imsCall.getCallProfile() == null || imsCall.getCallProfile().mMediaProfile == null) {
            return FORCE_DEBUG;
        }
        ImsStreamMediaProfile mediaProfile = imsCall.getCallProfile().mMediaProfile;
        if (mediaProfile.mAudioDirection == 0) {
            return true;
        }
        return FORCE_DEBUG;
    }

    public boolean update(ImsPhoneConnection conn, ImsCall imsCall, Call.State state) {
        if (state == Call.State.ALERTING) {
            if (this.mRingbackTonePlayed && !isLocalTone(imsCall)) {
                this.mOwner.mPhone.stopRingbackTone();
                this.mRingbackTonePlayed = FORCE_DEBUG;
            } else if (!this.mRingbackTonePlayed && isLocalTone(imsCall)) {
                this.mOwner.mPhone.startRingbackTone();
                this.mRingbackTonePlayed = true;
            }
        } else if (this.mRingbackTonePlayed) {
            this.mOwner.mPhone.stopRingbackTone();
            this.mRingbackTonePlayed = FORCE_DEBUG;
        }
        if (state != this.mState && state != Call.State.DISCONNECTED) {
            this.mState = state;
            return true;
        }
        if (state != Call.State.DISCONNECTED) {
            return FORCE_DEBUG;
        }
        return true;
    }

    ImsPhoneConnection getHandoverConnection() {
        return (ImsPhoneConnection) getEarliestConnection();
    }

    public void switchWith(ImsPhoneCall that) {
        if (VDBG) {
            Rlog.v(LOG_TAG, "switchWith : switchCall = " + this + " withCall = " + that);
        }
        synchronized (ImsPhoneCall.class) {
            ImsPhoneCall tmp = new ImsPhoneCall(this.mOwner, CONTEXT_UNKNOWN);
            tmp.takeOver(this);
            takeOver(that);
            that.takeOver(tmp);
        }
        this.mOwner.logState();
    }

    private void takeOver(ImsPhoneCall that) {
        this.mConnections = that.mConnections;
        this.mState = that.mState;
        for (Connection c : this.mConnections) {
            ((ImsPhoneConnection) c).changeParent(this);
        }
    }

    void resetRingbackTone() {
        this.mRingbackTonePlayed = FORCE_DEBUG;
    }
}
