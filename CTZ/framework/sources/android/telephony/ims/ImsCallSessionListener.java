package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsCallSessionListener;
import android.telephony.ims.stub.ImsCallSessionImplBase;
import com.android.ims.internal.IImsCallSession;

@SystemApi
public class ImsCallSessionListener {
    private final IImsCallSessionListener mListener;

    public ImsCallSessionListener(IImsCallSessionListener iImsCallSessionListener) {
        this.mListener = iImsCallSessionListener;
    }

    public void callSessionProgressing(ImsStreamMediaProfile imsStreamMediaProfile) {
        try {
            this.mListener.callSessionProgressing(imsStreamMediaProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionInitiated(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionInitiated(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionInitiatedFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionInitiatedFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionTerminated(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionTerminated(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionHeld(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionHeld(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionHoldFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionHoldFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionHoldReceived(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionHoldReceived(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionResumed(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionResumed(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionResumeFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionResumeFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionResumeReceived(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionResumeReceived(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMergeStarted(ImsCallSessionImplBase imsCallSessionImplBase, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionMergeStarted(imsCallSessionImplBase != null ? imsCallSessionImplBase.getServiceImpl() : null, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMergeStarted(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionMergeStarted(iImsCallSession, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMergeComplete(ImsCallSessionImplBase imsCallSessionImplBase) {
        try {
            this.mListener.callSessionMergeComplete(imsCallSessionImplBase != null ? imsCallSessionImplBase.getServiceImpl() : null);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMergeComplete(IImsCallSession iImsCallSession) {
        try {
            this.mListener.callSessionMergeComplete(iImsCallSession);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMergeFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionMergeFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionUpdated(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionUpdated(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionUpdateFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionUpdateFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionUpdateReceived(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionUpdateReceived(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceExtended(ImsCallSessionImplBase imsCallSessionImplBase, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionConferenceExtended(imsCallSessionImplBase != null ? imsCallSessionImplBase.getServiceImpl() : null, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceExtended(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionConferenceExtended(iImsCallSession, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceExtendFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionConferenceExtendFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceExtendReceived(ImsCallSessionImplBase imsCallSessionImplBase, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionConferenceExtendReceived(imsCallSessionImplBase != null ? imsCallSessionImplBase.getServiceImpl() : null, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceExtendReceived(IImsCallSession iImsCallSession, ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionConferenceExtendReceived(iImsCallSession, imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionInviteParticipantsRequestDelivered() {
        try {
            this.mListener.callSessionInviteParticipantsRequestDelivered();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionInviteParticipantsRequestFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionInviteParticipantsRequestFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionRemoveParticipantsRequestDelivered() {
        try {
            this.mListener.callSessionRemoveParticipantsRequestDelivered();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionRemoveParticipantsRequestFailed(ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionInviteParticipantsRequestFailed(imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionConferenceStateUpdated(ImsConferenceState imsConferenceState) {
        try {
            this.mListener.callSessionConferenceStateUpdated(imsConferenceState);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionUssdMessageReceived(int i, String str) {
        try {
            this.mListener.callSessionUssdMessageReceived(i, str);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMayHandover(int i, int i2) {
        try {
            this.mListener.callSessionMayHandover(i, i2);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionHandover(int i, int i2, ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionHandover(i, i2, imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionHandoverFailed(int i, int i2, ImsReasonInfo imsReasonInfo) {
        try {
            this.mListener.callSessionHandoverFailed(i, i2, imsReasonInfo);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionTtyModeReceived(int i) {
        try {
            this.mListener.callSessionTtyModeReceived(i);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionMultipartyStateChanged(boolean z) {
        try {
            this.mListener.callSessionMultipartyStateChanged(z);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionSuppServiceReceived(ImsSuppServiceNotification imsSuppServiceNotification) {
        try {
            this.mListener.callSessionSuppServiceReceived(imsSuppServiceNotification);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionRttModifyRequestReceived(ImsCallProfile imsCallProfile) {
        try {
            this.mListener.callSessionRttModifyRequestReceived(imsCallProfile);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionRttModifyResponseReceived(int i) {
        try {
            this.mListener.callSessionRttModifyResponseReceived(i);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void callSessionRttMessageReceived(String str) {
        try {
            this.mListener.callSessionRttMessageReceived(str);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }
}
