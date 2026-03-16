package com.android.internal.telephony.imsphone;

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
    private static final String LOG_TAG = "ImsPhoneCall";
    ImsPhoneCallTracker mOwner;
    private boolean mRingbackTonePlayed = false;

    ImsPhoneCall() {
    }

    ImsPhoneCall(ImsPhoneCallTracker owner) {
        this.mOwner = owner;
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
            return false;
        }
        return imsCall.isMultiparty();
    }

    @Override
    public void hangup() throws CallStateException {
        this.mOwner.hangup(this);
    }

    public String toString() {
        return this.mState.toString();
    }

    void attach(Connection conn) {
        clearDisconnected();
        this.mConnections.add(conn);
    }

    void attach(Connection conn, Call.State state) {
        attach(conn);
        this.mState = state;
    }

    void attachFake(Connection conn, Call.State state) {
        attach(conn, state);
    }

    boolean connectionDisconnected(ImsPhoneConnection conn) {
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

    void detach(ImsPhoneConnection conn) {
        this.mConnections.remove(conn);
        clearDisconnected();
    }

    boolean isFull() {
        return this.mConnections.size() == 5;
    }

    void onHangupLocal() {
        int s = this.mConnections.size();
        for (int i = 0; i < s; i++) {
            ImsPhoneConnection cn = (ImsPhoneConnection) this.mConnections.get(i);
            cn.onHangupLocal();
        }
        this.mState = Call.State.DISCONNECTING;
    }

    void clearDisconnected() {
        for (int i = this.mConnections.size() - 1; i >= 0; i--) {
            ImsPhoneConnection cn = (ImsPhoneConnection) this.mConnections.get(i);
            if (cn.getState() == Call.State.DISCONNECTED) {
                this.mConnections.remove(i);
            }
        }
        if (this.mConnections.size() == 0) {
            this.mState = Call.State.IDLE;
        }
    }

    ImsPhoneConnection getFirstConnection() {
        if (this.mConnections.size() == 0) {
            return null;
        }
        return (ImsPhoneConnection) this.mConnections.get(0);
    }

    void setMute(boolean mute) {
        ImsCall imsCall = getFirstConnection() == null ? null : getFirstConnection().getImsCall();
        if (imsCall != null) {
            try {
                imsCall.setMute(mute);
            } catch (ImsException e) {
                Rlog.e(LOG_TAG, "setMute failed : " + e.getMessage());
            }
        }
    }

    void merge(ImsPhoneCall that, Call.State state) {
        ImsPhoneConnection imsPhoneConnection = getFirstConnection();
        if (imsPhoneConnection != null) {
            long conferenceConnectTime = imsPhoneConnection.getConferenceConnectTime();
            if (conferenceConnectTime > 0) {
                imsPhoneConnection.setConnectTime(conferenceConnectTime);
            }
        }
        ImsPhoneConnection[] cc = (ImsPhoneConnection[]) that.mConnections.toArray(new ImsPhoneConnection[that.mConnections.size()]);
        for (ImsPhoneConnection c : cc) {
            c.update(null, state);
        }
    }

    public ImsCall getImsCall() {
        if (getFirstConnection() == null) {
            return null;
        }
        return getFirstConnection().getImsCall();
    }

    static boolean isLocalTone(ImsCall imsCall) {
        if (imsCall == null || imsCall.getCallProfile() == null || imsCall.getCallProfile().mMediaProfile == null) {
            return false;
        }
        ImsStreamMediaProfile mediaProfile = imsCall.getCallProfile().mMediaProfile;
        return mediaProfile.mAudioDirection == 0;
    }

    boolean update(ImsPhoneConnection conn, ImsCall imsCall, Call.State state) {
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
            return true;
        }
        if (state != Call.State.DISCONNECTED) {
            return false;
        }
        return true;
    }

    ImsPhoneConnection getHandoverConnection() {
        return (ImsPhoneConnection) getEarliestConnection();
    }

    void switchWith(ImsPhoneCall that) {
        synchronized (ImsPhoneCall.class) {
            ImsPhoneCall tmp = new ImsPhoneCall();
            tmp.takeOver(this);
            takeOver(that);
            that.takeOver(tmp);
        }
    }

    private void takeOver(ImsPhoneCall that) {
        this.mConnections = that.mConnections;
        this.mState = that.mState;
        for (Connection c : this.mConnections) {
            ((ImsPhoneConnection) c).changeParent(this);
        }
    }
}
