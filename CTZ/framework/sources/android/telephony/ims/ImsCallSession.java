package android.telephony.ims;

import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.util.Log;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsVideoCallProvider;
import com.android.internal.telephony.IccCardConstants;
import java.util.Objects;

public class ImsCallSession {
    private static final String TAG = "ImsCallSession";
    protected boolean mClosed;
    protected Listener mListener;
    protected IImsCallSession miSession;

    public static class State {
        public static final int ESTABLISHED = 4;
        public static final int ESTABLISHING = 3;
        public static final int IDLE = 0;
        public static final int INITIATED = 1;
        public static final int INVALID = -1;
        public static final int NEGOTIATING = 2;
        public static final int REESTABLISHING = 6;
        public static final int RENEGOTIATING = 5;
        public static final int TERMINATED = 8;
        public static final int TERMINATING = 7;

        public static String toString(int i) {
            switch (i) {
                case 0:
                    return "IDLE";
                case 1:
                    return "INITIATED";
                case 2:
                    return "NEGOTIATING";
                case 3:
                    return "ESTABLISHING";
                case 4:
                    return "ESTABLISHED";
                case 5:
                    return "RENEGOTIATING";
                case 6:
                    return "REESTABLISHING";
                case 7:
                    return "TERMINATING";
                case 8:
                    return "TERMINATED";
                default:
                    return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
            }
        }

        private State() {
        }
    }

    public static class Listener {
        public void callSessionProgressing(ImsCallSession imsCallSession, ImsStreamMediaProfile imsStreamMediaProfile) {
        }

        public void callSessionStarted(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionStartFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionTerminated(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionHeld(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionHoldFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionHoldReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionResumed(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionResumeFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionResumeReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionMergeStarted(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
        }

        public void callSessionMergeComplete(ImsCallSession imsCallSession) {
        }

        public void callSessionMergeFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionUpdated(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionUpdateFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionUpdateReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionConferenceExtended(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
        }

        public void callSessionConferenceExtendFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionConferenceExtendReceived(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
        }

        public void callSessionInviteParticipantsRequestDelivered(ImsCallSession imsCallSession) {
        }

        public void callSessionInviteParticipantsRequestFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionRemoveParticipantsRequestDelivered(ImsCallSession imsCallSession) {
        }

        public void callSessionRemoveParticipantsRequestFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionConferenceStateUpdated(ImsCallSession imsCallSession, ImsConferenceState imsConferenceState) {
        }

        public void callSessionUssdMessageReceived(ImsCallSession imsCallSession, int i, String str) {
        }

        public void callSessionMayHandover(ImsCallSession imsCallSession, int i, int i2) {
        }

        public void callSessionHandover(ImsCallSession imsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionHandoverFailed(ImsCallSession imsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionTtyModeReceived(ImsCallSession imsCallSession, int i) {
        }

        public void callSessionMultipartyStateChanged(ImsCallSession imsCallSession, boolean z) {
        }

        public void callSessionSuppServiceReceived(ImsCallSession imsCallSession, ImsSuppServiceNotification imsSuppServiceNotification) {
        }

        public void callSessionRttModifyRequestReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        }

        public void callSessionRttModifyResponseReceived(int i) {
        }

        public void callSessionRttMessageReceived(String str) {
        }

        public void callSessionTransferred(ImsCallSession imsCallSession) {
        }

        public void callSessionTransferFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionDeviceSwitched(ImsCallSession imsCallSession) {
        }

        public void callSessionDeviceSwitchFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
        }

        public void callSessionTextCapabilityChanged(ImsCallSession imsCallSession, int i, int i2, int i3, int i4) {
        }

        public void callSessionRttEventReceived(ImsCallSession imsCallSession, int i) {
        }
    }

    protected ImsCallSession() {
        this.mClosed = false;
        this.miSession = null;
    }

    public ImsCallSession(IImsCallSession iImsCallSession) {
        this.mClosed = false;
        this.miSession = iImsCallSession;
        if (iImsCallSession != null) {
            try {
                iImsCallSession.setListener(new IImsCallSessionListenerProxy());
            } catch (RemoteException e) {
            }
        } else {
            this.mClosed = true;
        }
    }

    public ImsCallSession(IImsCallSession iImsCallSession, Listener listener) {
        this(iImsCallSession);
        setListener(listener);
    }

    public void close() {
        synchronized (this) {
            if (this.mClosed) {
                return;
            }
            try {
                this.miSession.close();
                this.mClosed = true;
            } catch (RemoteException e) {
            }
        }
    }

    public String getCallId() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getCallId();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getLocalCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getLocalCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public ImsCallProfile getRemoteCallProfile() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getRemoteCallProfile();
        } catch (RemoteException e) {
            return null;
        }
    }

    public IImsVideoCallProvider getVideoCallProvider() {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getVideoCallProvider();
        } catch (RemoteException e) {
            return null;
        }
    }

    public String getProperty(String str) {
        if (this.mClosed) {
            return null;
        }
        try {
            return this.miSession.getProperty(str);
        } catch (RemoteException e) {
            return null;
        }
    }

    public int getState() {
        if (this.mClosed) {
            return -1;
        }
        try {
            return this.miSession.getState();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public boolean isAlive() {
        if (this.mClosed) {
            return false;
        }
        switch (getState()) {
        }
        return false;
    }

    public IImsCallSession getSession() {
        return this.miSession;
    }

    public boolean isInCall() {
        if (this.mClosed) {
            return false;
        }
        try {
            return this.miSession.isInCall();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setMute(boolean z) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.setMute(z);
        } catch (RemoteException e) {
        }
    }

    public void start(String str, ImsCallProfile imsCallProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.start(str, imsCallProfile);
        } catch (RemoteException e) {
        }
    }

    public void start(String[] strArr, ImsCallProfile imsCallProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.startConference(strArr, imsCallProfile);
        } catch (RemoteException e) {
        }
    }

    public void accept(int i, ImsStreamMediaProfile imsStreamMediaProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.accept(i, imsStreamMediaProfile);
        } catch (RemoteException e) {
        }
    }

    public void deflect(String str) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.deflect(str);
        } catch (RemoteException e) {
        }
    }

    public void reject(int i) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.reject(i);
        } catch (RemoteException e) {
        }
    }

    public void terminate(int i) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.terminate(i);
        } catch (RemoteException e) {
        }
    }

    public void hold(ImsStreamMediaProfile imsStreamMediaProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.hold(imsStreamMediaProfile);
        } catch (RemoteException e) {
        }
    }

    public void resume(ImsStreamMediaProfile imsStreamMediaProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.resume(imsStreamMediaProfile);
        } catch (RemoteException e) {
        }
    }

    public void merge() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.merge();
        } catch (RemoteException e) {
        }
    }

    public void update(int i, ImsStreamMediaProfile imsStreamMediaProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.update(i, imsStreamMediaProfile);
        } catch (RemoteException e) {
        }
    }

    public void extendToConference(String[] strArr) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.extendToConference(strArr);
        } catch (RemoteException e) {
        }
    }

    public void inviteParticipants(String[] strArr) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.inviteParticipants(strArr);
        } catch (RemoteException e) {
        }
    }

    public void removeParticipants(String[] strArr) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.removeParticipants(strArr);
        } catch (RemoteException e) {
        }
    }

    public void sendDtmf(char c, Message message) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendDtmf(c, message);
        } catch (RemoteException e) {
        }
    }

    public void startDtmf(char c) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.startDtmf(c);
        } catch (RemoteException e) {
        }
    }

    public void stopDtmf() {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.stopDtmf();
        } catch (RemoteException e) {
        }
    }

    public void sendUssd(String str) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendUssd(str);
        } catch (RemoteException e) {
        }
    }

    public boolean isMultiparty() {
        if (this.mClosed) {
            return false;
        }
        try {
            return this.miSession.isMultiparty();
        } catch (RemoteException e) {
            return false;
        }
    }

    public void sendRttMessage(String str) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttMessage(str);
        } catch (RemoteException e) {
        }
    }

    public void sendRttModifyRequest(ImsCallProfile imsCallProfile) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttModifyRequest(imsCallProfile);
        } catch (RemoteException e) {
        }
    }

    public void sendRttModifyResponse(boolean z) {
        if (this.mClosed) {
            return;
        }
        try {
            this.miSession.sendRttModifyResponse(z);
        } catch (RemoteException e) {
        }
    }

    public class IImsCallSessionListenerProxy extends IImsCallSessionListener.Stub {
        public IImsCallSessionListenerProxy() {
        }

        @Override
        public void callSessionProgressing(ImsStreamMediaProfile imsStreamMediaProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionProgressing(ImsCallSession.this, imsStreamMediaProfile);
            }
        }

        @Override
        public void callSessionInitiated(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionStarted(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionInitiatedFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionStartFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionTerminated(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTerminated(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionHeld(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHeld(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionHoldFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHoldFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionHoldReceived(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHoldReceived(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionResumed(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumed(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionResumeFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumeFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionResumeReceived(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionResumeReceived(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionMergeStarted(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
            Log.d(ImsCallSession.TAG, "callSessionMergeStarted");
        }

        @Override
        public void callSessionMergeComplete(IImsCallSession iImsCallSession) {
            if (ImsCallSession.this.mListener != null) {
                if (iImsCallSession != null) {
                    ImsCallSession imsCallSession = ImsCallSession.this;
                    try {
                        if (!Objects.equals(ImsCallSession.this.miSession.getCallId(), iImsCallSession.getCallId())) {
                            imsCallSession = new ImsCallSession(iImsCallSession);
                        }
                    } catch (RemoteException e) {
                        Log.e(ImsCallSession.TAG, "callSessionMergeComplete: exception for getCallId!");
                    }
                    ImsCallSession.this.mListener.callSessionMergeComplete(imsCallSession);
                    return;
                }
                ImsCallSession.this.mListener.callSessionMergeComplete(null);
            }
        }

        @Override
        public void callSessionMergeFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMergeFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionUpdated(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdated(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionUpdateFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdateFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionUpdateReceived(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUpdateReceived(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionConferenceExtended(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtended(ImsCallSession.this, new ImsCallSession(iImsCallSession), imsCallProfile);
            }
        }

        @Override
        public void callSessionConferenceExtendFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtendFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionConferenceExtendReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceExtendReceived(ImsCallSession.this, new ImsCallSession(iImsCallSession), imsCallProfile);
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestDelivered() {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionInviteParticipantsRequestDelivered(ImsCallSession.this);
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionInviteParticipantsRequestFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestDelivered() {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRemoveParticipantsRequestDelivered(ImsCallSession.this);
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestFailed(ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRemoveParticipantsRequestFailed(ImsCallSession.this, imsReasonInfo);
            }
        }

        @Override
        public void callSessionConferenceStateUpdated(ImsConferenceState imsConferenceState) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionConferenceStateUpdated(ImsCallSession.this, imsConferenceState);
            }
        }

        @Override
        public void callSessionUssdMessageReceived(int i, String str) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionUssdMessageReceived(ImsCallSession.this, i, str);
            }
        }

        @Override
        public void callSessionMayHandover(int i, int i2) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMayHandover(ImsCallSession.this, i, i2);
            }
        }

        @Override
        public void callSessionHandover(int i, int i2, ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHandover(ImsCallSession.this, i, i2, imsReasonInfo);
            }
        }

        @Override
        public void callSessionHandoverFailed(int i, int i2, ImsReasonInfo imsReasonInfo) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionHandoverFailed(ImsCallSession.this, i, i2, imsReasonInfo);
            }
        }

        @Override
        public void callSessionTtyModeReceived(int i) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionTtyModeReceived(ImsCallSession.this, i);
            }
        }

        @Override
        public void callSessionMultipartyStateChanged(boolean z) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionMultipartyStateChanged(ImsCallSession.this, z);
            }
        }

        @Override
        public void callSessionSuppServiceReceived(ImsSuppServiceNotification imsSuppServiceNotification) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionSuppServiceReceived(ImsCallSession.this, imsSuppServiceNotification);
            }
        }

        @Override
        public void callSessionRttModifyRequestReceived(ImsCallProfile imsCallProfile) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttModifyRequestReceived(ImsCallSession.this, imsCallProfile);
            }
        }

        @Override
        public void callSessionRttModifyResponseReceived(int i) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttModifyResponseReceived(i);
            }
        }

        @Override
        public void callSessionRttMessageReceived(String str) {
            if (ImsCallSession.this.mListener != null) {
                ImsCallSession.this.mListener.callSessionRttMessageReceived(str);
            }
        }
    }

    public String toString() {
        return "[ImsCallSession objId:" + System.identityHashCode(this) + " state:" + State.toString(getState()) + " callId:" + getCallId() + "]";
    }
}
