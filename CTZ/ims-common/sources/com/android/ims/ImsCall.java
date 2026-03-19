package com.android.ims;

import android.R;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcel;
import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import android.telephony.ims.ImsCallProfile;
import android.telephony.ims.ImsCallSession;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsStreamMediaProfile;
import android.telephony.ims.ImsSuppServiceNotification;
import android.util.Log;
import com.android.ims.internal.ICall;
import com.android.ims.internal.ImsStreamMediaSession;
import com.android.internal.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ImsCall implements ICall {
    private static final boolean CONF_DBG = true;
    private static final boolean FORCE_DEBUG = true;
    private static final String TAG = "ImsCall";
    private static final int UPDATE_EXTEND_TO_CONFERENCE = 5;
    private static final int UPDATE_HOLD = 1;
    protected static final int UPDATE_HOLD_MERGE = 2;
    private static final int UPDATE_MERGE = 4;
    protected static final int UPDATE_NONE = 0;
    private static final int UPDATE_RESUME = 3;
    private static final int UPDATE_UNSPECIFIED = 6;
    public static final int USSD_MODE_NOTIFY = 0;
    public static final int USSD_MODE_REQUEST = 1;
    private CopyOnWriteArrayList<ConferenceParticipant> mConferenceParticipants;
    protected Context mContext;
    protected ImsCallSessionListenerProxy mImsCallSessionListenerProxy;
    public final int uniqueId;
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private static final AtomicInteger sUniqueIdGenerator = new AtomicInteger();
    protected Object mLockObj = new Object();
    private boolean mInCall = false;
    protected boolean mHold = false;
    private boolean mMute = false;
    protected int mUpdateRequest = 0;
    protected Listener mListener = null;
    protected ImsCall mMergePeer = null;
    protected ImsCall mMergeHost = null;
    private boolean mMergeRequestedByConference = false;
    public ImsCallSession mSession = null;
    protected ImsCallProfile mCallProfile = null;
    private ImsCallProfile mProposedCallProfile = null;
    private ImsReasonInfo mLastReasonInfo = null;
    private ImsStreamMediaSession mMediaSession = null;
    protected ImsCallSession mTransientConferenceSession = null;
    private boolean mSessionEndDuringMerge = false;
    private ImsReasonInfo mSessionEndDuringMergeReasonInfo = null;
    private boolean mIsMerged = false;
    private boolean mCallSessionMergePending = false;
    protected boolean mTerminationRequestPending = false;
    protected boolean mIsConferenceHost = false;
    private boolean mWasVideoCall = false;
    protected int mOverrideReason = 0;
    private boolean mAnswerWithRtt = false;

    public static class Listener {
        public void onCallProgressing(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallStarted(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallStartFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallTerminated(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallStateChanged(imsCall);
        }

        public void onCallHeld(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallHoldFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallHoldReceived(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallResumed(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallResumeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallResumeReceived(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallMerged(ImsCall imsCall, ImsCall imsCall2, boolean z) {
            onCallStateChanged(imsCall);
        }

        public void onCallMergeFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallUpdated(ImsCall imsCall) {
            onCallStateChanged(imsCall);
        }

        public void onCallUpdateFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallUpdateReceived(ImsCall imsCall) {
        }

        public void onCallConferenceExtended(ImsCall imsCall, ImsCall imsCall2) {
            onCallStateChanged(imsCall);
        }

        public void onCallConferenceExtendFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
            onCallError(imsCall, imsReasonInfo);
        }

        public void onCallConferenceExtendReceived(ImsCall imsCall, ImsCall imsCall2) {
            onCallStateChanged(imsCall);
        }

        public void onCallInviteParticipantsRequestDelivered(ImsCall imsCall) {
        }

        public void onCallInviteParticipantsRequestFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
        }

        public void onCallRemoveParticipantsRequestDelivered(ImsCall imsCall) {
        }

        public void onCallRemoveParticipantsRequestFailed(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
        }

        public void onCallConferenceStateUpdated(ImsCall imsCall, ImsConferenceState imsConferenceState) {
        }

        public void onConferenceParticipantsStateChanged(ImsCall imsCall, List<ConferenceParticipant> list) {
        }

        public void onCallUssdMessageReceived(ImsCall imsCall, int i, String str) {
        }

        public void onCallError(ImsCall imsCall, ImsReasonInfo imsReasonInfo) {
        }

        public void onCallStateChanged(ImsCall imsCall) {
        }

        public void onCallStateChanged(ImsCall imsCall, int i) {
        }

        public void onCallSuppServiceReceived(ImsCall imsCall, ImsSuppServiceNotification imsSuppServiceNotification) {
        }

        public void onCallSessionTtyModeReceived(ImsCall imsCall, int i) {
        }

        public void onCallHandover(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
        }

        public void onRttModifyRequestReceived(ImsCall imsCall) {
        }

        public void onRttModifyResponseReceived(ImsCall imsCall, int i) {
        }

        public void onRttMessageReceived(ImsCall imsCall, String str) {
        }

        public void onCallHandoverFailed(ImsCall imsCall, int i, int i2, ImsReasonInfo imsReasonInfo) {
        }

        public void onMultipartyStateChanged(ImsCall imsCall, boolean z) {
        }
    }

    public ImsCall(Context context, ImsCallProfile imsCallProfile) {
        this.mContext = context;
        setCallProfile(imsCallProfile);
        this.uniqueId = sUniqueIdGenerator.getAndIncrement();
    }

    @Override
    public void close() {
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.close();
                this.mSession = null;
            } else {
                logi("close :: Cannot close Null call session!");
            }
            this.mCallProfile = null;
            this.mProposedCallProfile = null;
            this.mLastReasonInfo = null;
            this.mMediaSession = null;
        }
    }

    @Override
    public boolean checkIfRemoteUserIsSame(String str) {
        if (str == null) {
            return false;
        }
        return str.equals(this.mCallProfile.getCallExtra("remote_uri", ""));
    }

    @Override
    public boolean equalsTo(ICall iCall) {
        if (iCall == null || !(iCall instanceof ImsCall)) {
            return false;
        }
        return equals(iCall);
    }

    public static boolean isSessionAlive(ImsCallSession imsCallSession) {
        return imsCallSession != null && imsCallSession.isAlive();
    }

    public ImsCallProfile getCallProfile() {
        ImsCallProfile imsCallProfile;
        synchronized (this.mLockObj) {
            imsCallProfile = this.mCallProfile;
        }
        return imsCallProfile;
    }

    @VisibleForTesting
    public void setCallProfile(ImsCallProfile imsCallProfile) {
        synchronized (this.mLockObj) {
            this.mCallProfile = imsCallProfile;
            trackVideoStateHistory(this.mCallProfile);
        }
    }

    public ImsCallProfile getLocalCallProfile() throws ImsException {
        ImsCallProfile localCallProfile;
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            try {
                localCallProfile = this.mSession.getLocalCallProfile();
            } catch (Throwable th) {
                loge("getLocalCallProfile :: ", th);
                throw new ImsException("getLocalCallProfile()", th, 0);
            }
        }
        return localCallProfile;
    }

    public ImsCallProfile getRemoteCallProfile() throws ImsException {
        ImsCallProfile remoteCallProfile;
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            try {
                remoteCallProfile = this.mSession.getRemoteCallProfile();
            } catch (Throwable th) {
                loge("getRemoteCallProfile :: ", th);
                throw new ImsException("getRemoteCallProfile()", th, 0);
            }
        }
        return remoteCallProfile;
    }

    public ImsCallProfile getProposedCallProfile() {
        synchronized (this.mLockObj) {
            if (!isInCall()) {
                return null;
            }
            return this.mProposedCallProfile;
        }
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        synchronized (this.mLockObj) {
            logi("getConferenceParticipants :: mConferenceParticipants" + this.mConferenceParticipants);
            if (this.mConferenceParticipants == null) {
                return null;
            }
            if (this.mConferenceParticipants.isEmpty()) {
                return new ArrayList(0);
            }
            return new ArrayList(this.mConferenceParticipants);
        }
    }

    public int getState() {
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                return 0;
            }
            return this.mSession.getState();
        }
    }

    public ImsCallSession getCallSession() {
        ImsCallSession imsCallSession;
        synchronized (this.mLockObj) {
            imsCallSession = this.mSession;
        }
        return imsCallSession;
    }

    public ImsStreamMediaSession getMediaSession() {
        ImsStreamMediaSession imsStreamMediaSession;
        synchronized (this.mLockObj) {
            imsStreamMediaSession = this.mMediaSession;
        }
        return imsStreamMediaSession;
    }

    public String getCallExtra(String str) throws ImsException {
        String property;
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            try {
                property = this.mSession.getProperty(str);
            } catch (Throwable th) {
                loge("getCallExtra :: ", th);
                throw new ImsException("getCallExtra()", th, 0);
            }
        }
        return property;
    }

    public ImsReasonInfo getLastReasonInfo() {
        ImsReasonInfo imsReasonInfo;
        synchronized (this.mLockObj) {
            imsReasonInfo = this.mLastReasonInfo;
        }
        return imsReasonInfo;
    }

    public boolean hasPendingUpdate() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mUpdateRequest != 0;
        }
        return z;
    }

    public boolean isPendingHold() {
        boolean z;
        synchronized (this.mLockObj) {
            z = true;
            if (this.mUpdateRequest != 1) {
                z = false;
            }
        }
        return z;
    }

    public boolean isInCall() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mInCall;
        }
        return z;
    }

    public boolean isMuted() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mMute;
        }
        return z;
    }

    public boolean isOnHold() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mHold;
        }
        return z;
    }

    public boolean isMultiparty() {
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                return false;
            }
            return this.mSession.isMultiparty();
        }
    }

    public boolean isConferenceHost() {
        boolean z;
        synchronized (this.mLockObj) {
            z = isMultiparty() && this.mIsConferenceHost;
        }
        return z;
    }

    public void setIsMerged(boolean z) {
        this.mIsMerged = z;
    }

    public boolean isMerged() {
        return this.mIsMerged;
    }

    public void setListener(Listener listener) {
        setListener(listener, false);
    }

    public void setListener(Listener listener, boolean z) {
        synchronized (this.mLockObj) {
            this.mListener = listener;
            if (listener != null && z) {
                boolean z2 = this.mInCall;
                boolean z3 = this.mHold;
                int state = getState();
                ImsReasonInfo imsReasonInfo = this.mLastReasonInfo;
                try {
                    if (imsReasonInfo != null) {
                        listener.onCallError(this, imsReasonInfo);
                    } else if (z2) {
                        if (z3) {
                            listener.onCallHeld(this);
                        } else {
                            listener.onCallStarted(this);
                        }
                    } else if (state == UPDATE_RESUME) {
                        listener.onCallProgressing(this);
                    } else if (state == 8) {
                        listener.onCallTerminated(this, imsReasonInfo);
                    }
                } catch (Throwable th) {
                    loge("setListener() :: ", th);
                }
            }
        }
    }

    public void setMute(boolean z) throws ImsException {
        synchronized (this.mLockObj) {
            if (this.mMute != z) {
                StringBuilder sb = new StringBuilder();
                sb.append("setMute :: turning mute ");
                sb.append(z ? "on" : "off");
                logi(sb.toString());
                this.mMute = z;
                try {
                    this.mSession.setMute(z);
                } catch (Throwable th) {
                    loge("setMute :: ", th);
                    throwImsException(th, 0);
                }
            }
        }
    }

    public void attachSession(ImsCallSession imsCallSession) throws ImsException {
        logi("attachSession :: session=" + imsCallSession);
        synchronized (this.mLockObj) {
            this.mSession = imsCallSession;
            try {
                this.mSession.setListener(createCallSessionListener());
            } catch (Throwable th) {
                loge("attachSession :: ", th);
                throwImsException(th, 0);
            }
        }
    }

    public void start(ImsCallSession imsCallSession, String str) throws ImsException {
        logi("start(1) :: session=" + imsCallSession);
        synchronized (this.mLockObj) {
            this.mSession = imsCallSession;
            try {
                imsCallSession.setListener(createCallSessionListener());
                imsCallSession.start(str, this.mCallProfile);
            } catch (Throwable th) {
                loge("start(1) :: ", th);
                throw new ImsException("start(1)", th, 0);
            }
        }
    }

    public void start(ImsCallSession imsCallSession, String[] strArr) throws ImsException {
        logi("start(n) :: session=" + imsCallSession);
        synchronized (this.mLockObj) {
            this.mSession = imsCallSession;
            try {
                imsCallSession.setListener(createCallSessionListener());
                imsCallSession.start(strArr, this.mCallProfile);
            } catch (Throwable th) {
                loge("start(n) :: ", th);
                throw new ImsException("start(n)", th, 0);
            }
        }
    }

    public void accept(int i) throws ImsException {
        accept(i, new ImsStreamMediaProfile());
    }

    public void accept(int i, ImsStreamMediaProfile imsStreamMediaProfile) throws ImsException {
        logi("accept :: callType=" + i + ", profile=" + imsStreamMediaProfile);
        if (this.mAnswerWithRtt) {
            imsStreamMediaProfile.mRttMode = 1;
            logi("accept :: changing media profile RTT mode to full");
        }
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call to answer", 148);
            }
            try {
                this.mSession.accept(i, imsStreamMediaProfile);
                if (this.mInCall && this.mProposedCallProfile != null) {
                    if (DBG) {
                        logi("accept :: call profile will be updated");
                    }
                    this.mCallProfile = this.mProposedCallProfile;
                    trackVideoStateHistory(this.mCallProfile);
                    this.mProposedCallProfile = null;
                }
                if (this.mInCall && this.mUpdateRequest == UPDATE_UNSPECIFIED) {
                    this.mUpdateRequest = 0;
                }
            } catch (Throwable th) {
                loge("accept :: ", th);
                throw new ImsException("accept()", th, 0);
            }
        }
    }

    public void deflect(String str) throws ImsException {
        logi("deflect :: session=" + this.mSession + ", number=" + Rlog.pii(TAG, str));
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call to deflect", 148);
            }
            try {
                this.mSession.deflect(str);
            } catch (Throwable th) {
                loge("deflect :: ", th);
                throw new ImsException("deflect()", th, 0);
            }
        }
    }

    public void reject(int i) throws ImsException {
        logi("reject :: reason=" + i);
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.reject(i);
            }
            if (this.mInCall && this.mProposedCallProfile != null) {
                if (DBG) {
                    logi("reject :: call profile is not updated; destroy it...");
                }
                this.mProposedCallProfile = null;
            }
            if (this.mInCall && this.mUpdateRequest == UPDATE_UNSPECIFIED) {
                this.mUpdateRequest = 0;
            }
        }
    }

    public void terminate(int i, int i2) throws ImsException {
        logi("terminate :: reason=" + i + " ; overrideReadon=" + i2);
        this.mOverrideReason = i2;
        terminate(i);
    }

    public void terminate(int i) throws ImsException {
        logi("terminate :: reason=" + i);
        synchronized (this.mLockObj) {
            this.mHold = false;
            this.mInCall = false;
            this.mTerminationRequestPending = true;
            if (this.mSession != null) {
                this.mSession.terminate(i);
            }
        }
    }

    public void hold() throws ImsException {
        logi("hold :: ");
        if (isOnHold()) {
            if (DBG) {
                logi("hold :: call is already on hold");
                return;
            }
            return;
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("hold :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            this.mSession.hold(createHoldMediaProfile());
            this.mHold = true;
            this.mUpdateRequest = 1;
        }
    }

    public void resume() throws ImsException {
        logi("resume :: ");
        if (!isOnHold()) {
            if (DBG) {
                logi("resume :: call is not being held");
                return;
            }
            return;
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("resume :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("resume :: ");
                throw new ImsException("No call session", 148);
            }
            this.mUpdateRequest = UPDATE_RESUME;
            this.mSession.resume(createResumeMediaProfile());
        }
    }

    private void merge() throws ImsException {
        logi("merge :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                setCallSessionMergePending(false);
                if (this.mMergePeer != null) {
                    this.mMergePeer.setCallSessionMergePending(false);
                }
                loge("merge :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mMergePeer != null && this.mMergePeer.mUpdateRequest != 0) {
                setCallSessionMergePending(false);
                this.mMergePeer.setCallSessionMergePending(false);
                loge("merge :: peer call update is in progress; request=" + updateRequestToString(this.mMergePeer.mUpdateRequest));
                throw new ImsException("Peer call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("merge :: no call session");
                throw new ImsException("No call session", 148);
            }
            if (this.mHold || this.mContext.getResources().getBoolean(R.^attr-private.selectionScrollOffset)) {
                if (this.mMergePeer != null && !this.mMergePeer.isMultiparty() && !isMultiparty()) {
                    this.mUpdateRequest = UPDATE_MERGE;
                    this.mMergePeer.mUpdateRequest = UPDATE_MERGE;
                }
                this.mSession.merge();
            } else {
                this.mSession.hold(createHoldMediaProfile());
                this.mHold = true;
                this.mUpdateRequest = 2;
            }
        }
    }

    public void merge(ImsCall imsCall) throws ImsException {
        logi("merge(1) :: bgImsCall=" + imsCall);
        if (imsCall == null) {
            throw new ImsException("No background call", ImsManager.INCOMING_CALL_RESULT_CODE);
        }
        synchronized (this.mLockObj) {
            setCallSessionMergePending(true);
            imsCall.setCallSessionMergePending(true);
            if ((!isMultiparty() && !imsCall.isMultiparty()) || isMultiparty()) {
                setMergePeer(imsCall);
            } else {
                setMergeHost(imsCall);
            }
        }
        if (isMultiparty()) {
            this.mMergeRequestedByConference = true;
        } else {
            logi("merge : mMergeRequestedByConference not set");
        }
        merge();
    }

    public void update(int i, ImsStreamMediaProfile imsStreamMediaProfile) throws ImsException {
        logi("update :: callType=" + i + ", mediaProfile=" + imsStreamMediaProfile);
        if (isOnHold()) {
            if (DBG) {
                logi("update :: call is on hold");
            }
            throw new ImsException("Not in a call to update call", 102);
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                if (DBG) {
                    logi("update :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                }
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("update :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.update(i, imsStreamMediaProfile);
            this.mUpdateRequest = UPDATE_UNSPECIFIED;
        }
    }

    public void extendToConference(String[] strArr) throws ImsException {
        logi("extendToConference ::");
        if (isOnHold()) {
            if (DBG) {
                logi("extendToConference :: call is on hold");
            }
            throw new ImsException("Not in a call to extend a call to conference", 102);
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                logi("extendToConference :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("extendToConference :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.extendToConference(strArr);
            this.mUpdateRequest = UPDATE_EXTEND_TO_CONFERENCE;
        }
    }

    public void inviteParticipants(String[] strArr) throws ImsException {
        logi("inviteParticipants ::");
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("inviteParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.inviteParticipants(strArr);
        }
    }

    public void removeParticipants(String[] strArr) throws ImsException {
        logi("removeParticipants :: session=" + this.mSession);
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("removeParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.removeParticipants(strArr);
        }
    }

    public void sendDtmf(char c, Message message) {
        logi("sendDtmf :: code=" + c);
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.sendDtmf(c, message);
            }
        }
    }

    public void startDtmf(char c) {
        logi("startDtmf :: code=" + c);
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.startDtmf(c);
            }
        }
    }

    public void stopDtmf() {
        logi("stopDtmf :: ");
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.stopDtmf();
            }
        }
    }

    public void sendUssd(String str) throws ImsException {
        logi("sendUssd :: ussdMessage=" + str);
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendUssd :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.sendUssd(str);
        }
    }

    public void sendRttMessage(String str) {
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendRttMessage::no session");
            }
            if (!this.mCallProfile.mMediaProfile.isRttCall()) {
                logi("sendRttMessage::Not an rtt call, ignoring");
            } else {
                this.mSession.sendRttMessage(str);
            }
        }
    }

    public void sendRttModifyRequest() {
        logi("sendRttModifyRequest");
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendRttModifyRequest::no session");
            }
            if (this.mCallProfile.mMediaProfile.isRttCall()) {
                logi("sendRttModifyRequest::Already RTT call, ignoring.");
                return;
            }
            Parcel parcelObtain = Parcel.obtain();
            this.mCallProfile.writeToParcel(parcelObtain, 0);
            parcelObtain.setDataPosition(0);
            ImsCallProfile imsCallProfile = new ImsCallProfile();
            imsCallProfile.mMediaProfile.setRttMode(1);
            this.mSession.sendRttModifyRequest(imsCallProfile);
        }
    }

    public void sendRttModifyResponse(boolean z) {
        logi("sendRttModifyResponse");
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendRttModifyResponse::no session");
            }
            if (this.mCallProfile.mMediaProfile.isRttCall()) {
                logi("sendRttModifyResponse::Already RTT call, ignoring.");
            } else {
                this.mSession.sendRttModifyResponse(z);
            }
        }
    }

    public void setAnswerWithRtt() {
        this.mAnswerWithRtt = true;
    }

    private void clear(ImsReasonInfo imsReasonInfo) {
        this.mInCall = false;
        this.mHold = false;
        this.mUpdateRequest = 0;
        this.mLastReasonInfo = imsReasonInfo;
    }

    protected ImsCallSession.Listener createCallSessionListener() {
        this.mImsCallSessionListenerProxy = new ImsCallSessionListenerProxy();
        return this.mImsCallSessionListenerProxy;
    }

    @VisibleForTesting
    public ImsCallSessionListenerProxy getImsCallSessionListenerProxy() {
        return this.mImsCallSessionListenerProxy;
    }

    protected ImsCall createNewCall(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
        ImsCall imsCall = new ImsCall(this.mContext, imsCallProfile);
        try {
            imsCall.attachSession(imsCallSession);
            return imsCall;
        } catch (ImsException e) {
            imsCall.close();
            return null;
        }
    }

    private ImsStreamMediaProfile createHoldMediaProfile() {
        ImsStreamMediaProfile imsStreamMediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile == null) {
            return imsStreamMediaProfile;
        }
        imsStreamMediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
        imsStreamMediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
        imsStreamMediaProfile.mAudioDirection = 2;
        if (imsStreamMediaProfile.mVideoQuality != 0) {
            imsStreamMediaProfile.mVideoDirection = 2;
        }
        return imsStreamMediaProfile;
    }

    private ImsStreamMediaProfile createResumeMediaProfile() {
        ImsStreamMediaProfile imsStreamMediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile == null) {
            return imsStreamMediaProfile;
        }
        imsStreamMediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
        imsStreamMediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
        imsStreamMediaProfile.mAudioDirection = UPDATE_RESUME;
        if (imsStreamMediaProfile.mVideoQuality != 0) {
            imsStreamMediaProfile.mVideoDirection = UPDATE_RESUME;
        }
        return imsStreamMediaProfile;
    }

    private void enforceConversationMode() {
        if (this.mInCall) {
            this.mHold = false;
            this.mUpdateRequest = 0;
        }
    }

    protected void mergeInternal() {
        logi("mergeInternal :: ");
        this.mSession.merge();
        this.mUpdateRequest = UPDATE_MERGE;
    }

    private void notifyConferenceSessionTerminated(ImsReasonInfo imsReasonInfo) {
        Listener listener = this.mListener;
        clear(imsReasonInfo);
        if (listener != null) {
            try {
                listener.onCallTerminated(this, imsReasonInfo);
            } catch (Throwable th) {
                loge("notifyConferenceSessionTerminated :: ", th);
            }
        }
    }

    private void notifyConferenceStateUpdated(ImsConferenceState imsConferenceState) {
        Set<Map.Entry> setEntrySet;
        if (imsConferenceState == null || imsConferenceState.mParticipants == null || (setEntrySet = imsConferenceState.mParticipants.entrySet()) == null) {
            return;
        }
        this.mConferenceParticipants = new CopyOnWriteArrayList<>();
        for (Map.Entry entry : setEntrySet) {
            String str = (String) entry.getKey();
            Bundle bundle = (Bundle) entry.getValue();
            String string = bundle.getString("status");
            String string2 = bundle.getString("user");
            String string3 = bundle.getString("display-text");
            String string4 = bundle.getString("endpoint");
            logi("notifyConferenceStateUpdated :: key=" + Rlog.pii(TAG, str) + ", status=" + string + ", user=" + Rlog.pii(TAG, string2) + ", displayName= " + Rlog.pii(TAG, string3) + ", endpoint=" + string4);
            Uri uri = Uri.parse(string2);
            if (string4 == null) {
                string4 = "";
            }
            Uri uri2 = Uri.parse(string4);
            int connectionStateForStatus = ImsConferenceState.getConnectionStateForStatus(string);
            if (connectionStateForStatus != UPDATE_UNSPECIFIED) {
                this.mConferenceParticipants.add(new ConferenceParticipant(uri, string3, uri2, connectionStateForStatus));
            }
        }
        if (this.mConferenceParticipants != null && this.mListener != null) {
            try {
                this.mListener.onConferenceParticipantsStateChanged(this, this.mConferenceParticipants);
            } catch (Throwable th) {
                loge("notifyConferenceStateUpdated :: ", th);
            }
        }
    }

    protected void processCallTerminated(ImsReasonInfo imsReasonInfo) {
        logi("processCallTerminated :: reason=" + imsReasonInfo + " userInitiated = " + this.mTerminationRequestPending);
        synchronized (this) {
            if (isCallSessionMergePending() && !this.mTerminationRequestPending) {
                logi("processCallTerminated :: burying termination during ongoing merge.");
                this.mSessionEndDuringMerge = true;
                this.mSessionEndDuringMergeReasonInfo = imsReasonInfo;
            } else {
                if (isMultiparty()) {
                    notifyConferenceSessionTerminated(imsReasonInfo);
                    return;
                }
                Listener listener = this.mListener;
                clear(imsReasonInfo);
                if (listener != null) {
                    try {
                        listener.onCallTerminated(this, imsReasonInfo);
                    } catch (Throwable th) {
                        loge("processCallTerminated :: ", th);
                    }
                }
            }
        }
    }

    protected boolean isTransientConferenceSession(ImsCallSession imsCallSession) {
        if (imsCallSession != null && imsCallSession != this.mSession && imsCallSession == this.mTransientConferenceSession) {
            return true;
        }
        return false;
    }

    protected void setTransientSessionAsPrimary(ImsCallSession imsCallSession) {
        synchronized (this) {
            this.mSession.setListener((ImsCallSession.Listener) null);
            this.mSession = imsCallSession;
            this.mSession.setListener(createCallSessionListener());
        }
    }

    private void markCallAsMerged(boolean z) {
        int i;
        String str;
        if (!isSessionAlive(this.mSession)) {
            logi("markCallAsMerged");
            setIsMerged(!z);
            this.mSessionEndDuringMerge = true;
            if (z) {
                i = 510;
                str = "Call ended by network";
            } else {
                i = 108;
                str = "Call ended during conference merge process.";
            }
            this.mSessionEndDuringMergeReasonInfo = new ImsReasonInfo(i, 0, str);
        }
    }

    public boolean isMergeRequestedByConf() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mMergeRequestedByConference;
        }
        return z;
    }

    public void resetIsMergeRequestedByConf(boolean z) {
        synchronized (this.mLockObj) {
            this.mMergeRequestedByConference = z;
        }
    }

    public ImsCallSession getSession() {
        ImsCallSession imsCallSession;
        synchronized (this.mLockObj) {
            imsCallSession = this.mSession;
        }
        return imsCallSession;
    }

    public void processMergeComplete() {
        ImsCall imsCall;
        boolean z;
        ImsCall imsCall2;
        ImsCall imsCall3;
        boolean z2;
        logi("processMergeComplete :: ");
        if (!isMergeHost()) {
            loge("processMergeComplete :: We are not the merge host!");
            return;
        }
        synchronized (this) {
            if (isMultiparty()) {
                setIsMerged(false);
                if (this.mMergeRequestedByConference) {
                    z2 = false;
                } else {
                    this.mHold = false;
                    z2 = true;
                }
                this.mMergePeer.markCallAsMerged(false);
                imsCall2 = this.mMergePeer;
                imsCall3 = this;
            } else {
                if (this.mTransientConferenceSession == null) {
                    loge("processMergeComplete :: No transient session!");
                    return;
                }
                if (this.mMergePeer == null) {
                    loge("processMergeComplete :: No merge peer!");
                    return;
                }
                ImsCallSession imsCallSession = this.mTransientConferenceSession;
                this.mTransientConferenceSession = null;
                imsCallSession.setListener((ImsCallSession.Listener) null);
                if (isSessionAlive(this.mSession) && !isSessionAlive(this.mMergePeer.getCallSession())) {
                    this.mMergePeer.mHold = false;
                    this.mHold = true;
                    if (this.mConferenceParticipants != null && !this.mConferenceParticipants.isEmpty()) {
                        this.mMergePeer.mConferenceParticipants = this.mConferenceParticipants;
                    }
                    imsCall3 = this.mMergePeer;
                    setIsMerged(false);
                    this.mMergePeer.setIsMerged(false);
                    logi("processMergeComplete :: transient will transfer to merge peer");
                    imsCall2 = this;
                    z = true;
                } else {
                    if (!isSessionAlive(this.mSession) && isSessionAlive(this.mMergePeer.getCallSession())) {
                        imsCall = this.mMergePeer;
                        setIsMerged(false);
                        this.mMergePeer.setIsMerged(false);
                        logi("processMergeComplete :: transient will stay with the merge host");
                    } else {
                        imsCall = this.mMergePeer;
                        this.mMergePeer.markCallAsMerged(false);
                        setIsMerged(false);
                        this.mMergePeer.setIsMerged(true);
                        logi("processMergeComplete :: transient will stay with us (I'm the host).");
                    }
                    z = false;
                    imsCall2 = imsCall;
                    imsCall3 = this;
                }
                logi("processMergeComplete :: call=" + imsCall3 + " is the final host");
                imsCall3.setTransientSessionAsPrimary(imsCallSession);
                z2 = z;
            }
            Listener listener = imsCall3.mListener;
            updateCallProfile(imsCall2);
            updateCallProfile(imsCall3);
            clearMergeInfo();
            imsCall2.notifySessionTerminatedDuringMerge();
            imsCall3.clearSessionTerminationFlags();
            imsCall3.mIsConferenceHost = true;
            if (listener != null) {
                try {
                    listener.onCallMerged(imsCall3, imsCall2, z2);
                } catch (Throwable th) {
                    loge("processMergeComplete :: ", th);
                }
                if (this.mConferenceParticipants != null && !this.mConferenceParticipants.isEmpty()) {
                    try {
                        listener.onConferenceParticipantsStateChanged(imsCall3, this.mConferenceParticipants);
                    } catch (Throwable th2) {
                        loge("processMergeComplete :: ", th2);
                    }
                }
            }
        }
    }

    private static void updateCallProfile(ImsCall imsCall) {
        if (imsCall != null) {
            imsCall.updateCallProfile();
        }
    }

    private void updateCallProfile() {
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                setCallProfile(this.mSession.getCallProfile());
            }
        }
    }

    private void notifySessionTerminatedDuringMerge() {
        Listener listener;
        boolean z;
        ImsReasonInfo imsReasonInfo;
        synchronized (this) {
            listener = this.mListener;
            if (this.mSessionEndDuringMerge) {
                logi("notifySessionTerminatedDuringMerge ::reporting terminate during merge");
                z = true;
                imsReasonInfo = this.mSessionEndDuringMergeReasonInfo;
            } else {
                z = false;
                imsReasonInfo = null;
            }
            clearSessionTerminationFlags();
        }
        if (listener != null && z) {
            try {
                processCallTerminated(imsReasonInfo);
            } catch (Throwable th) {
                loge("notifySessionTerminatedDuringMerge :: ", th);
            }
        }
    }

    private void clearSessionTerminationFlags() {
        this.mSessionEndDuringMerge = false;
        this.mSessionEndDuringMergeReasonInfo = null;
    }

    protected void processMergeFailed(ImsReasonInfo imsReasonInfo) {
        logi("processMergeFailed :: reason=" + imsReasonInfo);
        synchronized (this) {
            if (!isMergeHost()) {
                loge("processMergeFailed :: We are not the merge host!");
                return;
            }
            if (this.mTransientConferenceSession != null) {
                this.mTransientConferenceSession.setListener((ImsCallSession.Listener) null);
                this.mTransientConferenceSession = null;
            }
            Listener listener = this.mListener;
            markCallAsMerged(true);
            setCallSessionMergePending(false);
            notifySessionTerminatedDuringMerge();
            if (this.mMergePeer != null) {
                this.mMergePeer.markCallAsMerged(true);
                this.mMergePeer.setCallSessionMergePending(false);
                this.mMergePeer.notifySessionTerminatedDuringMerge();
            } else {
                loge("processMergeFailed :: No merge peer!");
            }
            clearMergeInfo();
            if (listener != null) {
                try {
                    listener.onCallMergeFailed(this, imsReasonInfo);
                } catch (Throwable th) {
                    loge("processMergeFailed :: ", th);
                }
            }
        }
    }

    @VisibleForTesting
    public class ImsCallSessionListenerProxy extends ImsCallSession.Listener {
        public ImsCallSessionListenerProxy() {
        }

        public void callSessionProgressing(ImsCallSession imsCallSession, ImsStreamMediaProfile imsStreamMediaProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionProgressing :: session=" + imsCallSession + " profile=" + imsStreamMediaProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionProgressing :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.copyCallProfileIfNecessary(imsStreamMediaProfile);
            }
            if (listener != null) {
                try {
                    listener.onCallProgressing(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionProgressing :: ", th);
                }
            }
        }

        public void callSessionStarted(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionStarted :: session=" + imsCallSession + " profile=" + imsCallProfile);
            if (!ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.setCallSessionMergePending(false);
                if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                    return;
                }
                synchronized (ImsCall.this) {
                    listener = ImsCall.this.mListener;
                    ImsCall.this.setCallProfile(imsCallProfile);
                }
                if (listener != null) {
                    try {
                        listener.onCallStarted(ImsCall.this);
                        return;
                    } catch (Throwable th) {
                        ImsCall.this.loge("callSessionStarted :: ", th);
                        return;
                    }
                }
                return;
            }
            ImsCall.this.logi("callSessionStarted :: on transient session=" + imsCallSession);
        }

        public void callSessionStartFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionStartFailed :: session=" + imsCallSession + " reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionStartFailed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mLastReasonInfo = imsReasonInfo;
            }
            if (listener != null) {
                try {
                    listener.onCallStartFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionStarted :: ", th);
                }
            }
        }

        public void callSessionTerminated(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            ImsReasonInfo imsReasonInfo2;
            ImsCall.this.logi("callSessionTerminated :: session=" + imsCallSession + " reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionTerminated :: on transient session=" + imsCallSession);
                ImsCall.this.processMergeFailed(imsReasonInfo);
                return;
            }
            ImsCall.this.checkIfConferenceMerge(imsReasonInfo);
            if (ImsCall.this.mOverrideReason != 0) {
                ImsCall.this.logi("callSessionTerminated :: overrideReasonInfo=" + ImsCall.this.mOverrideReason);
                imsReasonInfo2 = new ImsReasonInfo(ImsCall.this.mOverrideReason, imsReasonInfo.getExtraCode(), imsReasonInfo.getExtraMessage());
            } else {
                imsReasonInfo2 = imsReasonInfo;
            }
            ImsCall.this.processCallTerminated(imsReasonInfo2);
            ImsCall.this.setCallSessionMergePending(false);
        }

        public void callSessionHeld(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            ImsCall.this.logi("callSessionHeld :: session=" + imsCallSession + "profile=" + imsCallProfile);
            synchronized (ImsCall.this) {
                if (!ImsCall.this.shouldSkipResetMergePending()) {
                    ImsCall.this.setCallSessionMergePending(false);
                }
                ImsCall.this.setCallProfile(imsCallProfile);
                if (ImsCall.this.mUpdateRequest == 2) {
                    ImsCall.this.mergeInternal();
                    return;
                }
                ImsCall.this.mUpdateRequest = 0;
                Listener listener = ImsCall.this.mListener;
                ImsCall.this.updateHoldStateIfNecessary(true);
                if (listener != null) {
                    try {
                        listener.onCallHeld(ImsCall.this);
                    } catch (Throwable th) {
                        ImsCall.this.loge("callSessionHeld :: ", th);
                    }
                }
            }
        }

        public void callSessionHoldFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionHoldFailed :: session" + imsCallSession + "reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionHoldFailed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            ImsCall.this.logi("callSessionHoldFailed :: session=" + imsCallSession + ", reasonInfo=" + imsReasonInfo);
            synchronized (ImsCall.this.mLockObj) {
                ImsCall.this.mHold = false;
            }
            synchronized (ImsCall.this) {
                if (ImsCall.this.mUpdateRequest == 2) {
                }
                ImsCall.this.mUpdateRequest = 0;
                listener = ImsCall.this.mListener;
                ImsCall.this.updateHoldStateIfNecessary(false);
            }
            if (listener != null) {
                try {
                    listener.onCallHoldFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionHoldFailed :: ", th);
                }
            }
        }

        public void callSessionHoldReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionHoldReceived :: session=" + imsCallSession + "profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionHoldReceived :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.setCallProfile(imsCallProfile);
            }
            if (listener != null) {
                try {
                    listener.onCallHoldReceived(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionHoldReceived :: ", th);
                }
            }
        }

        public void callSessionResumed(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionResumed :: session=" + imsCallSession + "profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionResumed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            if (!ImsCall.this.shouldSkipResetMergePending()) {
                ImsCall.this.setCallSessionMergePending(false);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.setCallProfile(imsCallProfile);
                ImsCall.this.mUpdateRequest = 0;
                ImsCall.this.mHold = false;
            }
            if (listener != null) {
                try {
                    listener.onCallResumed(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionResumed :: ", th);
                }
            }
        }

        public void callSessionResumeFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionResumeFailed :: session=" + imsCallSession + "reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionResumeFailed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this.mLockObj) {
                ImsCall.this.mHold = true;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallResumeFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionResumeFailed :: ", th);
                }
            }
        }

        public void callSessionResumeReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionResumeReceived :: session=" + imsCallSession + "profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionResumeReceived :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.setCallProfile(imsCallProfile);
            }
            if (listener != null) {
                try {
                    listener.onCallResumeReceived(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionResumeReceived :: ", th);
                }
            }
        }

        public void callSessionMergeStarted(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
            ImsCall.this.logi("callSessionMergeStarted :: session=" + imsCallSession + " newSession=" + imsCallSession2 + ", profile=" + imsCallProfile);
        }

        protected boolean doesCallSessionExistsInMerge(ImsCallSession imsCallSession) {
            String callId = imsCallSession.getCallId();
            return (ImsCall.this.isMergeHost() && Objects.equals(ImsCall.this.mMergePeer.mSession.getCallId(), callId)) || (ImsCall.this.isMergePeer() && Objects.equals(ImsCall.this.mMergeHost.mSession.getCallId(), callId)) || Objects.equals(ImsCall.this.mSession.getCallId(), callId);
        }

        public void callSessionMergeComplete(ImsCallSession imsCallSession) {
            ImsCall.this.logi("callSessionMergeComplete :: newSession =" + imsCallSession);
            if (!ImsCall.this.isMergeHost()) {
                ImsCall.this.mMergeHost.processMergeComplete();
                return;
            }
            if (imsCallSession != null) {
                ImsCall imsCall = ImsCall.this;
                if (doesCallSessionExistsInMerge(imsCallSession)) {
                    imsCallSession = null;
                }
                imsCall.mTransientConferenceSession = imsCallSession;
            }
            ImsCall.this.processMergeComplete();
        }

        public void callSessionMergeFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            ImsCall.this.loge("callSessionMergeFailed :: session=" + imsCallSession + "reasonInfo=" + imsReasonInfo);
            synchronized (ImsCall.this) {
                if (ImsCall.this.isMergeHost()) {
                    ImsCall.this.processMergeFailed(imsReasonInfo);
                } else if (ImsCall.this.mMergeHost != null) {
                    ImsCall.this.mMergeHost.processMergeFailed(imsReasonInfo);
                } else {
                    ImsCall.this.loge("callSessionMergeFailed :: No merge host for this conference!");
                }
            }
        }

        public void callSessionUpdated(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionUpdated :: session=" + imsCallSession + " profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionUpdated :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.setCallProfile(imsCallProfile);
            }
            if (listener != null) {
                try {
                    listener.onCallUpdated(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionUpdated :: ", th);
                }
            }
        }

        public void callSessionUpdateFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionUpdateFailed :: session=" + imsCallSession + " reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionUpdateFailed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallUpdateFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionUpdateFailed :: ", th);
                }
            }
        }

        public void callSessionUpdateReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionUpdateReceived :: session=" + imsCallSession + " profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionUpdateReceived :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mProposedCallProfile = imsCallProfile;
                ImsCall.this.mUpdateRequest = ImsCall.UPDATE_UNSPECIFIED;
            }
            if (listener != null) {
                try {
                    listener.onCallUpdateReceived(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionUpdateReceived :: ", th);
                }
            }
        }

        public void callSessionConferenceExtended(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionConferenceExtended :: session=" + imsCallSession + " newSession=" + imsCallSession2 + ", profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionConferenceExtended :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            ImsCall imsCallCreateNewCall = ImsCall.this.createNewCall(imsCallSession2, imsCallProfile);
            if (imsCallCreateNewCall == null) {
                callSessionConferenceExtendFailed(imsCallSession, new ImsReasonInfo());
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallConferenceExtended(ImsCall.this, imsCallCreateNewCall);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionConferenceExtended :: ", th);
                }
            }
        }

        public void callSessionConferenceExtendFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionConferenceExtendFailed :: reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionConferenceExtendFailed :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallConferenceExtendFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionConferenceExtendFailed :: ", th);
                }
            }
        }

        public void callSessionConferenceExtendReceived(ImsCallSession imsCallSession, ImsCallSession imsCallSession2, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionConferenceExtendReceived :: newSession=" + imsCallSession2 + ", profile=" + imsCallProfile);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionConferenceExtendReceived :: not supported for transient conference session" + imsCallSession);
                return;
            }
            ImsCall imsCallCreateNewCall = ImsCall.this.createNewCall(imsCallSession2, imsCallProfile);
            if (imsCallCreateNewCall == null) {
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallConferenceExtendReceived(ImsCall.this, imsCallCreateNewCall);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionConferenceExtendReceived :: ", th);
                }
            }
        }

        public void callSessionInviteParticipantsRequestDelivered(ImsCallSession imsCallSession) {
            Listener listener;
            ImsCall.this.logi("callSessionInviteParticipantsRequestDelivered ::");
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionInviteParticipantsRequestDelivered :: not supported for conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                ImsCall.this.resetConferenceMergingFlag();
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallInviteParticipantsRequestDelivered(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionInviteParticipantsRequestDelivered :: ", th);
                }
            }
        }

        public void callSessionInviteParticipantsRequestFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionInviteParticipantsRequestFailed :: reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionInviteParticipantsRequestFailed :: not supported for conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                ImsCall.this.resetConferenceMergingFlag();
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallInviteParticipantsRequestFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionInviteParticipantsRequestFailed :: ", th);
                }
            }
        }

        public void callSessionRemoveParticipantsRequestDelivered(ImsCallSession imsCallSession) {
            Listener listener;
            ImsCall.this.logi("callSessionRemoveParticipantsRequestDelivered ::");
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionRemoveParticipantsRequestDelivered :: not supported for conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallRemoveParticipantsRequestDelivered(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionRemoveParticipantsRequestDelivered :: ", th);
                }
            }
        }

        public void callSessionRemoveParticipantsRequestFailed(ImsCallSession imsCallSession, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionRemoveParticipantsRequestFailed :: reasonInfo=" + imsReasonInfo);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionRemoveParticipantsRequestFailed :: not supported for conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallRemoveParticipantsRequestFailed(ImsCall.this, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionRemoveParticipantsRequestFailed :: ", th);
                }
            }
        }

        public void callSessionConferenceStateUpdated(ImsCallSession imsCallSession, ImsConferenceState imsConferenceState) {
            ImsCall.this.logi("callSessionConferenceStateUpdated :: state=" + imsConferenceState);
            ImsCall.this.conferenceStateUpdated(imsConferenceState);
        }

        public void callSessionUssdMessageReceived(ImsCallSession imsCallSession, int i, String str) {
            Listener listener;
            ImsCall.this.logi("callSessionUssdMessageReceived :: mode=" + i + ", ussdMessage=" + str);
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionUssdMessageReceived :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallUssdMessageReceived(ImsCall.this, i, str);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionUssdMessageReceived :: ", th);
                }
            }
        }

        public void callSessionTtyModeReceived(ImsCallSession imsCallSession, int i) {
            Listener listener;
            ImsCall.this.logi("callSessionTtyModeReceived :: mode=" + i);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallSessionTtyModeReceived(ImsCall.this, i);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionTtyModeReceived :: ", th);
                }
            }
        }

        public void callSessionMultipartyStateChanged(ImsCallSession imsCallSession, boolean z) {
            Listener listener;
            if (ImsCall.VDBG) {
                ImsCall imsCall = ImsCall.this;
                StringBuilder sb = new StringBuilder();
                sb.append("callSessionMultipartyStateChanged isMultiParty: ");
                sb.append(z ? "Y" : "N");
                imsCall.logi(sb.toString());
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onMultipartyStateChanged(ImsCall.this, z);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionMultipartyStateChanged :: ", th);
                }
            }
        }

        public void callSessionHandover(ImsCallSession imsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.logi("callSessionHandover :: session=" + imsCallSession + ", srcAccessTech=" + i + ", targetAccessTech=" + i2 + ", reasonInfo=" + imsReasonInfo);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallHandover(ImsCall.this, i, i2, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionHandover :: ", th);
                }
            }
        }

        public void callSessionHandoverFailed(ImsCallSession imsCallSession, int i, int i2, ImsReasonInfo imsReasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionHandoverFailed :: session=" + imsCallSession + ", srcAccessTech=" + i + ", targetAccessTech=" + i2 + ", reasonInfo=" + imsReasonInfo);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallHandoverFailed(ImsCall.this, i, i2, imsReasonInfo);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionHandoverFailed :: ", th);
                }
            }
        }

        public void callSessionSuppServiceReceived(ImsCallSession imsCallSession, ImsSuppServiceNotification imsSuppServiceNotification) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(imsCallSession)) {
                ImsCall.this.logi("callSessionSuppServiceReceived :: not supported for transient conference session=" + imsCallSession);
                return;
            }
            ImsCall.this.logi("callSessionSuppServiceReceived :: session=" + imsCallSession + ", suppServiceInfo" + imsSuppServiceNotification);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallSuppServiceReceived(ImsCall.this, imsSuppServiceNotification);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionSuppServiceReceived :: ", th);
                }
            }
        }

        public void callSessionRttModifyRequestReceived(ImsCallSession imsCallSession, ImsCallProfile imsCallProfile) {
            Listener listener;
            ImsCall.this.logi("callSessionRttModifyRequestReceived");
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (!imsCallProfile.mMediaProfile.isRttCall()) {
                ImsCall.this.logi("callSessionRttModifyRequestReceived:: ignoring request, requested profile is not RTT.");
            } else if (listener != null) {
                try {
                    listener.onRttModifyRequestReceived(ImsCall.this);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionRttModifyRequestReceived:: ", th);
                }
            }
        }

        public void callSessionRttModifyResponseReceived(int i) {
            Listener listener;
            ImsCall.this.logi("callSessionRttModifyResponseReceived: " + i);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onRttModifyResponseReceived(ImsCall.this, i);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionRttModifyResponseReceived:: ", th);
                }
            }
        }

        public void callSessionRttMessageReceived(String str) {
            Listener listener;
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onRttMessageReceived(ImsCall.this, str);
                } catch (Throwable th) {
                    ImsCall.this.loge("callSessionRttModifyResponseReceived:: ", th);
                }
            }
        }
    }

    @VisibleForTesting
    public void conferenceStateUpdated(ImsConferenceState imsConferenceState) {
        Listener listener;
        synchronized (this) {
            notifyConferenceStateUpdated(imsConferenceState);
            listener = this.mListener;
        }
        if (listener != null) {
            try {
                listener.onCallConferenceStateUpdated(this, imsConferenceState);
            } catch (Throwable th) {
                loge("callSessionConferenceStateUpdated :: ", th);
            }
        }
    }

    protected String updateRequestToString(int i) {
        switch (i) {
            case 0:
                return "NONE";
            case 1:
                return "HOLD";
            case 2:
                return "HOLD_MERGE";
            case UPDATE_RESUME:
                return "RESUME";
            case UPDATE_MERGE:
                return "MERGE";
            case UPDATE_EXTEND_TO_CONFERENCE:
                return "EXTEND_TO_CONFERENCE";
            case UPDATE_UNSPECIFIED:
                return "UNSPECIFIED";
            default:
                return "UNKNOWN";
        }
    }

    private void clearMergeInfo() {
        logi("clearMergeInfo :: clearing all merge info");
        if (this.mMergeHost != null) {
            this.mMergeHost.mMergePeer = null;
            this.mMergeHost.mUpdateRequest = 0;
            this.mMergeHost.mCallSessionMergePending = false;
        }
        if (this.mMergePeer != null) {
            this.mMergePeer.mMergeHost = null;
            this.mMergePeer.mUpdateRequest = 0;
            this.mMergePeer.mCallSessionMergePending = false;
        }
        this.mMergeHost = null;
        this.mMergePeer = null;
        this.mUpdateRequest = 0;
        this.mCallSessionMergePending = false;
    }

    private void setMergePeer(ImsCall imsCall) {
        this.mMergePeer = imsCall;
        this.mMergeHost = null;
        imsCall.mMergeHost = this;
        imsCall.mMergePeer = null;
    }

    public void setMergeHost(ImsCall imsCall) {
        this.mMergeHost = imsCall;
        this.mMergePeer = null;
        imsCall.mMergeHost = null;
        imsCall.mMergePeer = this;
    }

    private boolean isMerging() {
        return (this.mMergePeer == null && this.mMergeHost == null) ? false : true;
    }

    protected boolean isMergeHost() {
        return this.mMergePeer != null && this.mMergeHost == null;
    }

    protected boolean isMergePeer() {
        return this.mMergePeer == null && this.mMergeHost != null;
    }

    public boolean isCallSessionMergePending() {
        return this.mCallSessionMergePending;
    }

    protected void setCallSessionMergePending(boolean z) {
        this.mCallSessionMergePending = z;
    }

    private boolean shouldProcessConferenceResult() {
        synchronized (this) {
            boolean zIsSessionAlive = false;
            if (!isMergeHost() && !isMergePeer()) {
                loge("shouldProcessConferenceResult :: no merge in progress");
                return false;
            }
            if (isMergeHost()) {
                logi("shouldProcessConferenceResult :: We are a merge host");
                logi("shouldProcessConferenceResult :: Here is the merge peer=" + this.mMergePeer);
                if (!isCallSessionMergePending() && !this.mMergePeer.isCallSessionMergePending()) {
                    zIsSessionAlive = true;
                }
                if (!isMultiparty()) {
                    zIsSessionAlive &= isSessionAlive(this.mTransientConferenceSession);
                }
            } else if (isMergePeer()) {
                logi("shouldProcessConferenceResult :: We are a merge peer");
                logi("shouldProcessConferenceResult :: Here is the merge host=" + this.mMergeHost);
                if (!isCallSessionMergePending() && !this.mMergeHost.isCallSessionMergePending()) {
                    zIsSessionAlive = true;
                }
                if (!this.mMergeHost.isMultiparty()) {
                    zIsSessionAlive &= isSessionAlive(this.mMergeHost.mTransientConferenceSession);
                } else {
                    zIsSessionAlive = !isCallSessionMergePending();
                }
            } else {
                loge("shouldProcessConferenceResult : merge in progress but call is neither host nor peer.");
            }
            StringBuilder sb = new StringBuilder();
            sb.append("shouldProcessConferenceResult :: returning:");
            sb.append(zIsSessionAlive ? ImsManager.TRUE : ImsManager.FALSE);
            logi(sb.toString());
            return zIsSessionAlive;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ImsCall objId:");
        sb.append(System.identityHashCode(this));
        sb.append(" onHold:");
        sb.append(isOnHold() ? "Y" : "N");
        sb.append(" mute:");
        sb.append(isMuted() ? "Y" : "N");
        if (this.mCallProfile != null) {
            sb.append(" mCallProfile:" + this.mCallProfile);
            sb.append(" tech:");
            sb.append(this.mCallProfile.getCallExtra("CallRadioTech"));
        }
        sb.append(" updateRequest:");
        sb.append(updateRequestToString(this.mUpdateRequest));
        sb.append(" merging:");
        sb.append(isMerging() ? "Y" : "N");
        if (isMerging()) {
            if (isMergePeer()) {
                sb.append("P");
            } else {
                sb.append("H");
            }
        }
        sb.append(" merge action pending:");
        sb.append(isCallSessionMergePending() ? "Y" : "N");
        sb.append(" merged:");
        sb.append(isMerged() ? "Y" : "N");
        sb.append(" multiParty:");
        sb.append(isMultiparty() ? "Y" : "N");
        sb.append(" confHost:");
        sb.append(isConferenceHost() ? "Y" : "N");
        sb.append(" buried term:");
        sb.append(this.mSessionEndDuringMerge ? "Y" : "N");
        sb.append(" isVideo: ");
        sb.append(isVideoCall() ? "Y" : "N");
        sb.append(" wasVideo: ");
        sb.append(this.mWasVideoCall ? "Y" : "N");
        sb.append(" isWifi: ");
        sb.append(isWifiCall() ? "Y" : "N");
        sb.append(" session:");
        sb.append(this.mSession);
        sb.append(" transientSession:");
        sb.append(this.mTransientConferenceSession);
        sb.append("]");
        return sb.toString();
    }

    private void throwImsException(Throwable th, int i) throws ImsException {
        if (th instanceof ImsException) {
            throw ((ImsException) th);
        }
        throw new ImsException(String.valueOf(i), th, i);
    }

    private String appendImsCallInfoToString(String str) {
        return str + " ImsCall=" + this;
    }

    private void trackVideoStateHistory(ImsCallProfile imsCallProfile) {
        this.mWasVideoCall = this.mWasVideoCall || imsCallProfile.isVideoCall();
    }

    public boolean wasVideoCall() {
        return this.mWasVideoCall;
    }

    public boolean isVideoCall() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mCallProfile != null && this.mCallProfile.isVideoCall();
        }
        return z;
    }

    public boolean isWifiCall() {
        synchronized (this.mLockObj) {
            if (this.mCallProfile == null) {
                return false;
            }
            return getRadioTechnology() == 18;
        }
    }

    public int getRadioTechnology() {
        int i;
        synchronized (this.mLockObj) {
            if (this.mCallProfile == null) {
                return 0;
            }
            String callExtra = this.mCallProfile.getCallExtra("CallRadioTech");
            if (callExtra == null || callExtra.isEmpty()) {
                callExtra = this.mCallProfile.getCallExtra("callRadioTech");
            }
            try {
                i = Integer.parseInt(callExtra);
            } catch (NumberFormatException e) {
                i = 0;
            }
            return i;
        }
    }

    protected void logi(String str) {
        Log.i(TAG, appendImsCallInfoToString(str));
    }

    protected void logd(String str) {
        Log.d(TAG, appendImsCallInfoToString(str));
    }

    private void logv(String str) {
        Log.v(TAG, appendImsCallInfoToString(str));
    }

    protected void loge(String str) {
        Log.e(TAG, appendImsCallInfoToString(str));
    }

    protected void loge(String str, Throwable th) {
        Log.e(TAG, appendImsCallInfoToString(str), th);
    }

    protected void copyCallProfileIfNecessary(ImsStreamMediaProfile imsStreamMediaProfile) {
        this.mCallProfile.mMediaProfile.copyFrom(imsStreamMediaProfile);
    }

    protected void checkIfConferenceMerge(ImsReasonInfo imsReasonInfo) {
    }

    protected void updateHoldStateIfNecessary(boolean z) {
    }

    protected boolean shouldSkipResetMergePending() {
        return false;
    }

    protected void resetConferenceMergingFlag() {
    }
}
