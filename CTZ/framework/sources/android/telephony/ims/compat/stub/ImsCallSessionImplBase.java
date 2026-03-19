package android.telephony.ims.compat.stub;

import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsSuppServiceNotification;
import android.telephony.ims.aidl.IImsCallSessionListener;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsVideoCallProvider;

public class ImsCallSessionImplBase extends IImsCallSession.Stub {
    @Override
    public final void setListener(IImsCallSessionListener iImsCallSessionListener) throws RemoteException {
        setListener(new ImsCallSessionListenerConverter(iImsCallSessionListener));
    }

    public void setListener(com.android.ims.internal.IImsCallSessionListener iImsCallSessionListener) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getCallId() {
        return null;
    }

    @Override
    public ImsCallProfile getCallProfile() {
        return null;
    }

    @Override
    public ImsCallProfile getLocalCallProfile() {
        return null;
    }

    @Override
    public ImsCallProfile getRemoteCallProfile() {
        return null;
    }

    @Override
    public String getProperty(String str) {
        return null;
    }

    @Override
    public int getState() {
        return -1;
    }

    @Override
    public boolean isInCall() {
        return false;
    }

    @Override
    public void setMute(boolean z) {
    }

    @Override
    public void start(String str, ImsCallProfile imsCallProfile) {
    }

    @Override
    public void startConference(String[] strArr, ImsCallProfile imsCallProfile) {
    }

    @Override
    public void accept(int i, ImsStreamMediaProfile imsStreamMediaProfile) {
    }

    @Override
    public void deflect(String str) {
    }

    @Override
    public void reject(int i) {
    }

    @Override
    public void terminate(int i) {
    }

    @Override
    public void hold(ImsStreamMediaProfile imsStreamMediaProfile) {
    }

    @Override
    public void resume(ImsStreamMediaProfile imsStreamMediaProfile) {
    }

    @Override
    public void merge() {
    }

    @Override
    public void update(int i, ImsStreamMediaProfile imsStreamMediaProfile) {
    }

    @Override
    public void extendToConference(String[] strArr) {
    }

    @Override
    public void inviteParticipants(String[] strArr) {
    }

    @Override
    public void removeParticipants(String[] strArr) {
    }

    @Override
    public void sendDtmf(char c, Message message) {
    }

    @Override
    public void startDtmf(char c) {
    }

    @Override
    public void stopDtmf() {
    }

    @Override
    public void sendUssd(String str) {
    }

    @Override
    public IImsVideoCallProvider getVideoCallProvider() {
        return null;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }

    @Override
    public void sendRttModifyRequest(ImsCallProfile imsCallProfile) {
    }

    @Override
    public void sendRttModifyResponse(boolean z) {
    }

    @Override
    public void sendRttMessage(String str) {
    }

    private class ImsCallSessionListenerConverter extends IImsCallSessionListener.Stub {
        private final android.telephony.ims.aidl.IImsCallSessionListener mNewListener;

        public ImsCallSessionListenerConverter(android.telephony.ims.aidl.IImsCallSessionListener iImsCallSessionListener) {
            this.mNewListener = iImsCallSessionListener;
        }

        @Override
        public void callSessionProgressing(IImsCallSession iImsCallSession, ImsStreamMediaProfile imsStreamMediaProfile) throws RemoteException {
            this.mNewListener.callSessionProgressing(imsStreamMediaProfile);
        }

        @Override
        public void callSessionStarted(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionInitiated(imsCallProfile);
        }

        @Override
        public void callSessionStartFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionInitiatedFailed(imsReasonInfo);
        }

        @Override
        public void callSessionTerminated(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionTerminated(imsReasonInfo);
        }

        @Override
        public void callSessionHeld(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionHeld(imsCallProfile);
        }

        @Override
        public void callSessionHoldFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionHoldFailed(imsReasonInfo);
        }

        @Override
        public void callSessionHoldReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionHoldReceived(imsCallProfile);
        }

        @Override
        public void callSessionResumed(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionResumed(imsCallProfile);
        }

        @Override
        public void callSessionResumeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionResumeFailed(imsReasonInfo);
        }

        @Override
        public void callSessionResumeReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionResumeReceived(imsCallProfile);
        }

        @Override
        public void callSessionMergeStarted(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionMergeStarted(iImsCallSession2, imsCallProfile);
        }

        @Override
        public void callSessionMergeComplete(IImsCallSession iImsCallSession) throws RemoteException {
            this.mNewListener.callSessionMergeComplete(iImsCallSession);
        }

        @Override
        public void callSessionMergeFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionMergeFailed(imsReasonInfo);
        }

        @Override
        public void callSessionUpdated(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionUpdated(imsCallProfile);
        }

        @Override
        public void callSessionUpdateFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionUpdateFailed(imsReasonInfo);
        }

        @Override
        public void callSessionUpdateReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionUpdateReceived(imsCallProfile);
        }

        @Override
        public void callSessionConferenceExtended(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionConferenceExtended(iImsCallSession2, imsCallProfile);
        }

        @Override
        public void callSessionConferenceExtendFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionConferenceExtendFailed(imsReasonInfo);
        }

        @Override
        public void callSessionConferenceExtendReceived(IImsCallSession iImsCallSession, IImsCallSession iImsCallSession2, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionConferenceExtendReceived(iImsCallSession2, imsCallProfile);
        }

        @Override
        public void callSessionInviteParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException {
            this.mNewListener.callSessionInviteParticipantsRequestDelivered();
        }

        @Override
        public void callSessionInviteParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionInviteParticipantsRequestFailed(imsReasonInfo);
        }

        @Override
        public void callSessionRemoveParticipantsRequestDelivered(IImsCallSession iImsCallSession) throws RemoteException {
            this.mNewListener.callSessionRemoveParticipantsRequestDelivered();
        }

        @Override
        public void callSessionRemoveParticipantsRequestFailed(IImsCallSession iImsCallSession, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionRemoveParticipantsRequestFailed(imsReasonInfo);
        }

        @Override
        public void callSessionConferenceStateUpdated(IImsCallSession iImsCallSession, ImsConferenceState imsConferenceState) throws RemoteException {
            this.mNewListener.callSessionConferenceStateUpdated(imsConferenceState);
        }

        @Override
        public void callSessionUssdMessageReceived(IImsCallSession iImsCallSession, int i, String str) throws RemoteException {
            this.mNewListener.callSessionUssdMessageReceived(i, str);
        }

        @Override
        public void callSessionHandover(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionHandover(i, i2, imsReasonInfo);
        }

        @Override
        public void callSessionHandoverFailed(IImsCallSession iImsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) throws RemoteException {
            this.mNewListener.callSessionHandoverFailed(i, i2, imsReasonInfo);
        }

        @Override
        public void callSessionMayHandover(IImsCallSession iImsCallSession, int i, int i2) throws RemoteException {
            this.mNewListener.callSessionMayHandover(i, i2);
        }

        @Override
        public void callSessionTtyModeReceived(IImsCallSession iImsCallSession, int i) throws RemoteException {
            this.mNewListener.callSessionTtyModeReceived(i);
        }

        @Override
        public void callSessionMultipartyStateChanged(IImsCallSession iImsCallSession, boolean z) throws RemoteException {
            this.mNewListener.callSessionMultipartyStateChanged(z);
        }

        @Override
        public void callSessionSuppServiceReceived(IImsCallSession iImsCallSession, ImsSuppServiceNotification imsSuppServiceNotification) throws RemoteException {
            this.mNewListener.callSessionSuppServiceReceived(imsSuppServiceNotification);
        }

        @Override
        public void callSessionRttModifyRequestReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) throws RemoteException {
            this.mNewListener.callSessionRttModifyRequestReceived(imsCallProfile);
        }

        @Override
        public void callSessionRttModifyResponseReceived(int i) throws RemoteException {
            this.mNewListener.callSessionRttModifyResponseReceived(i);
        }

        @Override
        public void callSessionRttMessageReceived(String str) throws RemoteException {
            this.mNewListener.callSessionRttMessageReceived(str);
        }
    }
}
