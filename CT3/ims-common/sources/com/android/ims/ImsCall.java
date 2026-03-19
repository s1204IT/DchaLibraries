package com.android.ims;

import android.R;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.telecom.ConferenceParticipant;
import android.util.Log;
import com.android.ims.internal.ICall;
import com.android.ims.internal.ImsCallSession;
import com.android.ims.internal.ImsStreamMediaSession;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ImsCall implements ICall {
    private static final boolean CONF_DBG = true;
    private static final boolean FORCE_DEBUG = true;
    private static final String TAG = "ImsCall";
    private static final int UPDATE_ECT = 7;
    private static final int UPDATE_EXTEND_TO_CONFERENCE = 5;
    private static final int UPDATE_HOLD = 1;
    private static final int UPDATE_HOLD_MERGE = 2;
    private static final int UPDATE_MERGE = 4;
    private static final int UPDATE_NONE = 0;
    private static final int UPDATE_RESUME = 3;
    private static final int UPDATE_UNSPECIFIED = 6;
    public static final int USSD_MODE_NOTIFY = 0;
    public static final int USSD_MODE_REQUEST = 1;
    private ImsCallProfile mCallProfile;
    private CopyOnWriteArrayList<ConferenceParticipant> mConferenceParticipants;
    private Context mContext;
    private static final boolean DBG = true;
    private static final boolean VDBG = true;
    private Object mLockObj = new Object();
    private boolean mInCall = false;
    private boolean mHold = false;
    private boolean mMute = false;
    private int mUpdateRequest = 0;
    private Listener mListener = null;
    private ImsCall mMergePeer = null;
    private ImsCall mMergeHost = null;
    private boolean mMergeRequestedByConference = false;
    private ImsCallSession mSession = null;
    private ImsCallProfile mProposedCallProfile = null;
    private ImsReasonInfo mLastReasonInfo = null;
    private ImsStreamMediaSession mMediaSession = null;
    private ImsCallSession mTransientConferenceSession = null;
    private boolean mSessionEndDuringMerge = false;
    private ImsReasonInfo mSessionEndDuringMergeReasonInfo = null;
    private boolean mIsMerged = false;
    private boolean mCallSessionMergePending = false;
    private boolean mIsConferenceMerging = false;
    private String mAddress = null;
    private boolean mTerminationRequestPending = false;
    private boolean mIsConferenceHost = false;

    public static class Listener {
        public void onCallProgressing(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallStarted(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallStartFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallTerminated(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallStateChanged(call);
        }

        public void onCallHeld(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallHoldFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallHoldReceived(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallResumed(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallResumeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallResumeReceived(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallMerged(ImsCall call, ImsCall peerCall, boolean swapCalls) {
            onCallStateChanged(call);
        }

        public void onCallMergeFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallUpdated(ImsCall call) {
            onCallStateChanged(call);
        }

        public void onCallUpdateFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallUpdateReceived(ImsCall call) {
        }

        public void onCallConferenceExtended(ImsCall call, ImsCall newCall) {
            onCallStateChanged(call);
        }

        public void onCallConferenceExtendFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }

        public void onCallConferenceExtendReceived(ImsCall call, ImsCall newCall) {
            onCallStateChanged(call);
        }

        public void onCallInviteParticipantsRequestDelivered(ImsCall call) {
        }

        public void onCallInviteParticipantsRequestFailed(ImsCall call, ImsReasonInfo reasonInfo) {
        }

        public void onCallRemoveParticipantsRequestDelivered(ImsCall call) {
        }

        public void onCallRemoveParticipantsRequestFailed(ImsCall call, ImsReasonInfo reasonInfo) {
        }

        public void onCallConferenceStateUpdated(ImsCall call, ImsConferenceState state) {
        }

        public void onConferenceParticipantsStateChanged(ImsCall call, List<ConferenceParticipant> participants) {
        }

        public void onCallUssdMessageReceived(ImsCall call, int mode, String ussdMessage) {
        }

        public void onCallError(ImsCall call, ImsReasonInfo reasonInfo) {
        }

        public void onCallStateChanged(ImsCall call) {
        }

        public void onCallStateChanged(ImsCall call, int state) {
        }

        public void onCallSuppServiceReceived(ImsCall call, ImsSuppServiceNotification suppServiceInfo) {
        }

        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
        }

        public void onCallHandover(ImsCall imsCall, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
        }

        public void onCallHandoverFailed(ImsCall imsCall, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
        }

        public void onMultipartyStateChanged(ImsCall imsCall, boolean isMultiParty) {
        }

        public void onPauInfoChanged(ImsCall imsCall) {
        }

        public void onCallTransferred(ImsCall call) {
        }

        public void onCallTransferFailed(ImsCall call, ImsReasonInfo reasonInfo) {
            onCallError(call, reasonInfo);
        }
    }

    public ImsCall(Context context, ImsCallProfile profile) {
        this.mCallProfile = null;
        this.mContext = context;
        this.mCallProfile = profile;
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
    public boolean checkIfRemoteUserIsSame(String userId) {
        if (userId == null) {
            return false;
        }
        String address = this.mCallProfile.getCallExtra("oi", "").replace("+", "");
        if (address == null || address.equals("")) {
            address = this.mAddress;
        }
        logi("address= " + address);
        if (address != null && !address.equals("") && (userId.equals(address) || userId.indexOf(address) != -1)) {
            return true;
        }
        String remoteSipUri = this.mCallProfile.getCallExtra("remote_uri", "");
        logi("REMOTE_SIP_URI= " + remoteSipUri);
        if (remoteSipUri == null || remoteSipUri.equals("")) {
            return false;
        }
        return userId.equals(remoteSipUri) || userId.indexOf(remoteSipUri) != -1;
    }

    @Override
    public boolean equalsTo(ICall call) {
        if (call != null && (call instanceof ImsCall)) {
            return equals(call);
        }
        return false;
    }

    public static boolean isSessionAlive(ImsCallSession session) {
        if (session != null) {
            return session.isAlive();
        }
        return false;
    }

    public ImsCallProfile getCallProfile() {
        ImsCallProfile imsCallProfile;
        synchronized (this.mLockObj) {
            imsCallProfile = this.mCallProfile;
        }
        return imsCallProfile;
    }

    public ImsCallProfile getLocalCallProfile() throws ImsException {
        ImsCallProfile localCallProfile;
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            try {
                localCallProfile = this.mSession.getLocalCallProfile();
            } catch (Throwable t) {
                loge("getLocalCallProfile :: ", t);
                throw new ImsException("getLocalCallProfile()", t, 0);
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
            } catch (Throwable t) {
                loge("getRemoteCallProfile :: ", t);
                throw new ImsException("getRemoteCallProfile()", t, 0);
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
        CopyOnWriteArrayList<ConferenceParticipant> copyOnWriteArrayList;
        synchronized (this.mLockObj) {
            logi("getConferenceParticipants :: mConferenceParticipants" + this.mConferenceParticipants);
            copyOnWriteArrayList = this.mConferenceParticipants;
        }
        return copyOnWriteArrayList;
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

    public String getCallExtra(String name) throws ImsException {
        String property;
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            try {
                property = this.mSession.getProperty(name);
            } catch (Throwable t) {
                loge("getCallExtra :: ", t);
                throw new ImsException("getCallExtra()", t, 0);
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
            z = this.mUpdateRequest == 1;
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
            z = isMultiparty() ? this.mIsConferenceHost : false;
        }
        return z;
    }

    public void setIsMerged(boolean isMerged) {
        this.mIsMerged = isMerged;
    }

    public boolean isMerged() {
        return this.mIsMerged;
    }

    public void setListener(Listener listener) {
        setListener(listener, false);
    }

    public void setListener(Listener listener, boolean callbackImmediately) {
        synchronized (this.mLockObj) {
            this.mListener = listener;
            if (listener == null || !callbackImmediately) {
                return;
            }
            boolean inCall = this.mInCall;
            boolean onHold = this.mHold;
            int state = getState();
            ImsReasonInfo lastReasonInfo = this.mLastReasonInfo;
            try {
                if (lastReasonInfo != null) {
                    listener.onCallError(this, lastReasonInfo);
                } else if (inCall) {
                    if (onHold) {
                        listener.onCallHeld(this);
                    } else {
                        listener.onCallStarted(this);
                    }
                } else {
                    switch (state) {
                        case 3:
                            listener.onCallProgressing(this);
                            break;
                        case 8:
                            listener.onCallTerminated(this, lastReasonInfo);
                            break;
                    }
                }
            } catch (Throwable t) {
                loge("setListener() :: ", t);
            }
        }
    }

    public void setMute(boolean muted) throws ImsException {
        synchronized (this.mLockObj) {
            if (this.mMute != muted) {
                logi("setMute :: turning mute " + (muted ? "on" : "off"));
                this.mMute = muted;
                try {
                    this.mSession.setMute(muted);
                } catch (Throwable t) {
                    loge("setMute :: ", t);
                    throwImsException(t, 0);
                }
            }
        }
    }

    public void attachSession(ImsCallSession session) throws ImsException {
        logi("attachSession :: session=" + session);
        synchronized (this.mLockObj) {
            this.mSession = session;
            try {
                this.mSession.setListener(createCallSessionListener());
            } catch (Throwable t) {
                loge("attachSession :: ", t);
                throwImsException(t, 0);
            }
        }
    }

    public void start(ImsCallSession session, String callee) throws ImsException {
        logi("start(1) :: session=" + session + ", callee=" + callee);
        synchronized (this.mLockObj) {
            this.mSession = session;
            try {
                session.setListener(createCallSessionListener());
                session.start(callee, this.mCallProfile);
                this.mAddress = callee;
            } catch (Throwable t) {
                loge("start(1) :: ", t);
                throw new ImsException("start(1)", t, 0);
            }
        }
    }

    public void start(ImsCallSession session, String[] participants) throws ImsException {
        ImsException imsException;
        logi("start(n) :: session=" + session + ", callee=" + participants);
        synchronized (this.mLockObj) {
            this.mSession = session;
            if (this.mCallProfile != null && this.mCallProfile.getCallExtraBoolean("conference")) {
                this.mIsConferenceHost = true;
            }
            try {
                session.setListener(createCallSessionListener());
                session.start(participants, this.mCallProfile);
            } finally {
            }
        }
    }

    public void accept(int callType) throws ImsException {
        accept(callType, new ImsStreamMediaProfile());
    }

    public void accept(int callType, ImsStreamMediaProfile profile) throws ImsException {
        logi("accept :: callType=" + callType + ", profile=" + profile);
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call to answer", 148);
            }
            try {
                this.mSession.accept(callType, profile);
                if (this.mInCall && this.mProposedCallProfile != null) {
                    if (DBG) {
                        logi("accept :: call profile will be updated");
                    }
                    this.mCallProfile = this.mProposedCallProfile;
                    this.mProposedCallProfile = null;
                }
                if (this.mInCall && this.mUpdateRequest == 6) {
                    this.mUpdateRequest = 0;
                }
            } catch (Throwable t) {
                loge("accept :: ", t);
                throw new ImsException("accept()", t, 0);
            }
        }
    }

    public void reject(int reason) throws ImsException {
        logi("reject :: reason=" + reason);
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.reject(reason);
            }
            if (this.mInCall && this.mProposedCallProfile != null) {
                if (DBG) {
                    logi("reject :: call profile is not updated; destroy it...");
                }
                this.mProposedCallProfile = null;
            }
            if (this.mInCall && this.mUpdateRequest == 6) {
                this.mUpdateRequest = 0;
            }
        }
    }

    public void terminate(int reason) throws ImsException {
        logi("terminate :: reason=" + reason);
        synchronized (this.mLockObj) {
            this.mHold = false;
            this.mInCall = false;
            this.mTerminationRequestPending = true;
            if (this.mSession != null) {
                this.mSession.terminate(reason);
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
            this.mUpdateRequest = 3;
            this.mSession.resume(createResumeMediaProfile());
        }
    }

    private void merge() throws ImsException {
        logi("merge :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("merge :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("merge :: no call session");
                throw new ImsException("No call session", 148);
            }
            if (this.mHold || this.mContext.getResources().getBoolean(R.^attr-private.magnifierColorOverlay)) {
                if (this.mMergePeer != null && !this.mMergePeer.isMultiparty() && !isMultiparty()) {
                    this.mUpdateRequest = 4;
                    this.mMergePeer.mUpdateRequest = 4;
                }
                this.mSession.merge();
            } else {
                this.mSession.hold(createHoldMediaProfile());
                this.mHold = true;
                this.mUpdateRequest = 2;
            }
        }
    }

    public void merge(ImsCall bgCall) throws ImsException {
        logi("merge(1) :: bgImsCall=" + bgCall);
        if (bgCall == null) {
            throw new ImsException("No background call", ImsManager.INCOMING_CALL_RESULT_CODE);
        }
        synchronized (this.mLockObj) {
            setCallSessionMergePending(true);
            bgCall.setCallSessionMergePending(true);
            if ((!isMultiparty() && !bgCall.isMultiparty()) || isMultiparty()) {
                setMergePeer(bgCall);
            } else {
                setMergeHost(bgCall);
            }
        }
        if (isMultiparty()) {
            this.mMergeRequestedByConference = true;
        } else {
            logi("merge : mMergeRequestedByConference not set");
        }
        merge();
    }

    public void update(int callType, ImsStreamMediaProfile mediaProfile) throws ImsException {
        logi("update :: callType=" + callType + ", mediaProfile=" + mediaProfile);
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
            this.mSession.update(callType, mediaProfile);
            this.mUpdateRequest = 6;
        }
    }

    public void extendToConference(String[] participants) throws ImsException {
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
            this.mSession.extendToConference(participants);
            this.mUpdateRequest = 5;
        }
    }

    public void inviteParticipants(String[] participants) throws ImsException {
        logi("inviteParticipants ::");
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("inviteParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.inviteParticipants(participants);
        }
    }

    public void removeParticipants(String[] participants) throws ImsException {
        logi("removeParticipants :: session=" + this.mSession);
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("removeParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.removeParticipants(participants);
        }
    }

    public void sendDtmf(char c, Message result) {
        logi("sendDtmf :: code=" + c);
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.sendDtmf(c, result);
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

    public void sendUssd(String ussdMessage) throws ImsException {
        logi("sendUssd :: ussdMessage=" + ussdMessage);
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendUssd :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.sendUssd(ussdMessage);
        }
    }

    private void clear(ImsReasonInfo lastReasonInfo) {
        this.mInCall = false;
        this.mHold = false;
        this.mUpdateRequest = 0;
        this.mLastReasonInfo = lastReasonInfo;
    }

    private ImsCallSession.Listener createCallSessionListener() {
        return new ImsCallSessionListenerProxy(this, null);
    }

    private ImsCall createNewCall(ImsCallSession session, ImsCallProfile profile) {
        ImsCall call = new ImsCall(this.mContext, profile);
        try {
            call.attachSession(session);
            return call;
        } catch (ImsException e) {
            if (call != null) {
                call.close();
                return null;
            }
            return call;
        }
    }

    private ImsStreamMediaProfile createHoldMediaProfile() {
        ImsStreamMediaProfile mediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile == null) {
            return mediaProfile;
        }
        mediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
        mediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
        mediaProfile.mAudioDirection = 2;
        if (mediaProfile.mVideoQuality != 0) {
            mediaProfile.mVideoDirection = 2;
        }
        return mediaProfile;
    }

    private ImsStreamMediaProfile createResumeMediaProfile() {
        ImsStreamMediaProfile mediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile == null) {
            return mediaProfile;
        }
        mediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
        mediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
        mediaProfile.mAudioDirection = 3;
        if (mediaProfile.mVideoQuality != 0) {
            mediaProfile.mVideoDirection = 3;
        }
        return mediaProfile;
    }

    private void enforceConversationMode() {
        if (!this.mInCall) {
            return;
        }
        this.mHold = false;
        this.mUpdateRequest = 0;
    }

    private void mergeInternal() {
        logi("mergeInternal :: ");
        this.mSession.merge();
        this.mUpdateRequest = 4;
    }

    private void notifyConferenceSessionTerminated(ImsReasonInfo reasonInfo) {
        Listener listener = this.mListener;
        clear(reasonInfo);
        if (listener == null) {
            return;
        }
        try {
            listener.onCallTerminated(this, reasonInfo);
        } catch (Throwable t) {
            loge("notifyConferenceSessionTerminated :: ", t);
        }
    }

    private void notifyConferenceStateUpdated(ImsConferenceState state) {
        Set<Map.Entry<String, Bundle>> participants;
        if (state == null || state.mParticipants == null || (participants = state.mParticipants.entrySet()) == null) {
            return;
        }
        this.mConferenceParticipants = new CopyOnWriteArrayList<>();
        for (Map.Entry<String, Bundle> entry : participants) {
            String key = entry.getKey();
            Bundle confInfo = entry.getValue();
            String status = confInfo.getString("status");
            String user = confInfo.getString("user");
            String displayName = confInfo.getString("display-text");
            String endpoint = confInfo.getString("endpoint");
            logi("notifyConferenceStateUpdated :: key=" + key + ", status=" + status + ", user=" + user + ", displayName= " + displayName + ", endpoint=" + endpoint);
            Uri handle = Uri.parse(user);
            if (endpoint == null) {
                endpoint = "";
            }
            Uri endpointUri = Uri.parse(endpoint);
            int connectionState = ImsConferenceState.getConnectionStateForStatus(status);
            if (connectionState != 6) {
                ConferenceParticipant conferenceParticipant = new ConferenceParticipant(handle, displayName, endpointUri, connectionState);
                this.mConferenceParticipants.add(conferenceParticipant);
            }
        }
        if (this.mConferenceParticipants == null || this.mListener == null) {
            return;
        }
        try {
            this.mListener.onConferenceParticipantsStateChanged(this, this.mConferenceParticipants);
        } catch (Throwable t) {
            loge("notifyConferenceStateUpdated :: ", t);
        }
    }

    private void processCallTerminated(ImsReasonInfo reasonInfo) {
        logi("processCallTerminated :: reason=" + reasonInfo + " userInitiated = " + this.mTerminationRequestPending);
        synchronized (this) {
            if (isCallSessionMergePending() && !this.mTerminationRequestPending) {
                logi("processCallTerminated :: burying termination during ongoing merge.");
                this.mSessionEndDuringMerge = true;
                this.mSessionEndDuringMergeReasonInfo = reasonInfo;
            } else {
                if (isMultiparty()) {
                    notifyConferenceSessionTerminated(reasonInfo);
                    return;
                }
                Listener listener = this.mListener;
                clear(reasonInfo);
                if (listener == null) {
                    return;
                }
                try {
                    listener.onCallTerminated(this, reasonInfo);
                } catch (Throwable t) {
                    loge("processCallTerminated :: ", t);
                }
            }
        }
    }

    private boolean isTransientConferenceSession(ImsCallSession session) {
        if (session != null && session != this.mSession && session == this.mTransientConferenceSession) {
            return true;
        }
        return false;
    }

    private void setTransientSessionAsPrimary(ImsCallSession transientSession) {
        synchronized (this) {
            if (this.mSession != null) {
                this.mSession.setListener(null);
                this.mSession.close();
            }
            this.mSession = transientSession;
            if (this.mSession != null) {
                this.mSession.setListener(createCallSessionListener());
                ImsCallProfile imsCallProfile = this.mSession.getCallProfile();
                if (imsCallProfile != null) {
                    this.mCallProfile.updateCallType(imsCallProfile);
                }
            }
        }
    }

    private void markCallAsMerged(boolean playDisconnectTone) {
        String reasonInfo;
        if (isSessionAlive(this.mSession)) {
            return;
        }
        logi("markCallAsMerged");
        setIsMerged(!playDisconnectTone);
        this.mSessionEndDuringMerge = true;
        if (playDisconnectTone) {
            reasonInfo = "Call ended by network";
        } else {
            reasonInfo = "Call ended during conference merge process.";
        }
        this.mSessionEndDuringMergeReasonInfo = new ImsReasonInfo(0, 0, reasonInfo);
    }

    public boolean isMergeRequestedByConf() {
        boolean z;
        synchronized (this.mLockObj) {
            z = this.mMergeRequestedByConference;
        }
        return z;
    }

    public void resetIsMergeRequestedByConf(boolean value) {
        synchronized (this.mLockObj) {
            this.mMergeRequestedByConference = value;
        }
    }

    public ImsCallSession getSession() {
        ImsCallSession imsCallSession;
        synchronized (this.mLockObj) {
            imsCallSession = this.mSession;
        }
        return imsCallSession;
    }

    private void processMergeComplete() {
        ImsCall finalHostCall;
        ImsCall finalPeerCall;
        logi("processMergeComplete :: ");
        if (!isMergeHost()) {
            loge("processMergeComplete :: We are not the merge host!");
            return;
        }
        boolean swapRequired = false;
        synchronized (this) {
            if (isMultiparty()) {
                setIsMerged(false);
                if (!this.mMergeRequestedByConference) {
                    this.mHold = false;
                    swapRequired = true;
                }
                this.mMergePeer.markCallAsMerged(false);
                finalHostCall = this;
                finalPeerCall = this.mMergePeer;
            } else {
                if (this.mTransientConferenceSession == null) {
                    loge("processMergeComplete :: No transient session!");
                    return;
                }
                if (this.mMergePeer == null) {
                    loge("processMergeComplete :: No merge peer!");
                    return;
                }
                ImsCallSession transientConferenceSession = this.mTransientConferenceSession;
                this.mTransientConferenceSession = null;
                transientConferenceSession.setListener(null);
                if (isSessionAlive(this.mSession) && !isSessionAlive(this.mMergePeer.getCallSession())) {
                    this.mMergePeer.mHold = false;
                    this.mHold = true;
                    if (this.mConferenceParticipants != null && !this.mConferenceParticipants.isEmpty()) {
                        this.mMergePeer.mConferenceParticipants = this.mConferenceParticipants;
                    }
                    finalHostCall = this.mMergePeer;
                    finalPeerCall = this;
                    swapRequired = true;
                    setIsMerged(false);
                    this.mMergePeer.setIsMerged(false);
                    logi("processMergeComplete :: transient will transfer to merge peer");
                } else if (!isSessionAlive(this.mSession) && isSessionAlive(this.mMergePeer.getCallSession())) {
                    finalHostCall = this;
                    finalPeerCall = this.mMergePeer;
                    swapRequired = false;
                    setIsMerged(false);
                    this.mMergePeer.setIsMerged(false);
                    logi("processMergeComplete :: transient will stay with the merge host");
                } else {
                    finalHostCall = this;
                    finalPeerCall = this.mMergePeer;
                    this.mMergePeer.markCallAsMerged(false);
                    swapRequired = false;
                    setIsMerged(false);
                    this.mMergePeer.setIsMerged(true);
                    logi("processMergeComplete :: transient will stay with us (I'm the host).");
                }
                logi("processMergeComplete :: call=" + finalHostCall + " is the final host");
                finalHostCall.setTransientSessionAsPrimary(transientConferenceSession);
            }
            Listener listener = finalHostCall.mListener;
            updateCallProfile(finalPeerCall);
            updateCallProfile(finalHostCall);
            clearMergeInfo();
            finalPeerCall.notifySessionTerminatedDuringMerge();
            finalHostCall.clearSessionTerminationFlags();
            finalHostCall.mIsConferenceHost = true;
            if (listener != null) {
                try {
                    listener.onCallMerged(finalHostCall, finalPeerCall, swapRequired);
                } catch (Throwable t) {
                    loge("processMergeComplete :: ", t);
                }
                if (this.mConferenceParticipants != null && !this.mConferenceParticipants.isEmpty()) {
                    try {
                        listener.onConferenceParticipantsStateChanged(finalHostCall, this.mConferenceParticipants);
                    } catch (Throwable t2) {
                        loge("processMergeComplete :: ", t2);
                    }
                }
            }
        }
    }

    private static void updateCallProfile(ImsCall call) {
        if (call == null) {
            return;
        }
        call.updateCallProfile();
    }

    private void updateCallProfile() {
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mCallProfile = this.mSession.getCallProfile();
            }
        }
    }

    private void notifySessionTerminatedDuringMerge() {
        Listener listener;
        boolean notifyFailure = false;
        ImsReasonInfo notifyFailureReasonInfo = null;
        synchronized (this) {
            listener = this.mListener;
            if (this.mSessionEndDuringMerge) {
                logi("notifySessionTerminatedDuringMerge ::reporting terminate during merge");
                notifyFailure = true;
                notifyFailureReasonInfo = this.mSessionEndDuringMergeReasonInfo;
            }
            clearSessionTerminationFlags();
        }
        if (listener == null || !notifyFailure) {
            return;
        }
        try {
            processCallTerminated(notifyFailureReasonInfo);
        } catch (Throwable t) {
            loge("notifySessionTerminatedDuringMerge :: ", t);
        }
    }

    private void clearSessionTerminationFlags() {
        this.mSessionEndDuringMerge = false;
        this.mSessionEndDuringMergeReasonInfo = null;
    }

    private void processMergeFailed(ImsReasonInfo reasonInfo) {
        logi("processMergeFailed :: reason=" + reasonInfo);
        synchronized (this) {
            if (!isMergeHost()) {
                loge("processMergeFailed :: We are not the merge host!");
                return;
            }
            if (this.mTransientConferenceSession != null) {
                this.mTransientConferenceSession.setListener(null);
                this.mTransientConferenceSession = null;
            }
            Listener listener = this.mListener;
            setIsMerged(false);
            markCallAsMerged(true);
            setCallSessionMergePending(false);
            notifySessionTerminatedDuringMerge();
            if (this.mMergePeer != null) {
                this.mMergePeer.setIsMerged(false);
                this.mMergePeer.markCallAsMerged(true);
                this.mMergePeer.setCallSessionMergePending(false);
                this.mMergePeer.notifySessionTerminatedDuringMerge();
            } else {
                loge("processMergeFailed :: No merge peer!");
            }
            clearMergeInfo();
            if (listener != null) {
                try {
                    listener.onCallMergeFailed(this, reasonInfo);
                } catch (Throwable t) {
                    loge("processMergeFailed :: ", t);
                }
            }
        }
    }

    private class ImsCallSessionListenerProxy extends ImsCallSession.Listener {
        ImsCallSessionListenerProxy(ImsCall this$0, ImsCallSessionListenerProxy imsCallSessionListenerProxy) {
            this();
        }

        private ImsCallSessionListenerProxy() {
        }

        @Override
        public void callSessionProgressing(ImsCallSession session, ImsStreamMediaProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionProgressing :: session=" + session + " profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionProgressing :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                if (ImsCall.this.mCallProfile != null) {
                    ImsCall.this.mCallProfile.mMediaProfile.copyFrom(profile);
                }
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallProgressing(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionProgressing :: ", t);
            }
        }

        @Override
        public void callSessionStarted(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionStarted :: session=" + session + " profile=" + profile);
            if (!ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.setCallSessionMergePending(false);
                if (ImsCall.this.isTransientConferenceSession(session)) {
                    return;
                }
                synchronized (ImsCall.this) {
                    listener = ImsCall.this.mListener;
                    ImsCall.this.mCallProfile = profile;
                }
                if (listener == null) {
                    return;
                }
                try {
                    listener.onCallStarted(ImsCall.this);
                    return;
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionStarted :: ", t);
                    return;
                }
            }
            ImsCall.this.logi("callSessionStarted :: on transient session=" + session);
        }

        @Override
        public void callSessionStartFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionStartFailed :: session=" + session + " reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionStartFailed :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mLastReasonInfo = reasonInfo;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallStartFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionStarted :: ", t);
            }
        }

        @Override
        public void callSessionTerminated(ImsCallSession session, ImsReasonInfo reasonInfo) {
            ImsCall.this.logi("callSessionTerminated :: session=" + session + " reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionTerminated :: on transient session=" + session);
                ImsCall.this.processMergeFailed(reasonInfo);
                return;
            }
            if (ImsCall.this.isCallSessionMergePending() && ImsCall.this.isMultiparty()) {
                ImsCall.this.logi("this is a conference host during merging, and is disconnected..");
                ImsCall.this.processMergeFailed(reasonInfo);
            }
            ImsCall.this.processCallTerminated(reasonInfo);
            ImsCall.this.setCallSessionMergePending(false);
        }

        @Override
        public void callSessionHeld(ImsCallSession session, ImsCallProfile profile) {
            ImsCall.this.logi("callSessionHeld :: session=" + session + "profile=" + profile);
            synchronized (ImsCall.this) {
                ImsCall.this.mCallProfile = profile;
                if (ImsCall.this.mUpdateRequest == 2) {
                    ImsCall.this.mergeInternal();
                    return;
                }
                ImsCall.this.mUpdateRequest = 0;
                Listener listener = ImsCall.this.mListener;
                ImsCall.this.mHold = true;
                if (listener == null) {
                    return;
                }
                try {
                    listener.onCallHeld(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionHeld :: ", t);
                }
            }
        }

        @Override
        public void callSessionHoldFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionHoldFailed :: session" + session + "reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionHoldFailed :: not supported for transient conference session=" + session);
                return;
            }
            ImsCall.this.logi("callSessionHoldFailed :: session=" + session + ", reasonInfo=" + reasonInfo);
            synchronized (ImsCall.this.mLockObj) {
                ImsCall.this.mHold = false;
            }
            synchronized (ImsCall.this) {
                if (ImsCall.this.mUpdateRequest == 2) {
                }
                ImsCall.this.mUpdateRequest = 0;
                listener = ImsCall.this.mListener;
                ImsCall.this.mHold = false;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallHoldFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionHoldFailed :: ", t);
            }
        }

        @Override
        public void callSessionHoldReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionHoldReceived :: session=" + session + "profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionHoldReceived :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallHoldReceived(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionHoldReceived :: ", t);
            }
        }

        @Override
        public void callSessionResumed(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionResumed :: session=" + session + "profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionResumed :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
                ImsCall.this.mUpdateRequest = 0;
                ImsCall.this.mHold = false;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallResumed(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionResumed :: ", t);
            }
        }

        @Override
        public void callSessionResumeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionResumeFailed :: session=" + session + "reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionResumeFailed :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this.mLockObj) {
                ImsCall.this.mHold = true;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallResumeFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionResumeFailed :: ", t);
            }
        }

        @Override
        public void callSessionResumeReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionResumeReceived :: session=" + session + "profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionResumeReceived :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallResumeReceived(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionResumeReceived :: ", t);
            }
        }

        @Override
        public void callSessionMergeStarted(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            ImsCall.this.logi("callSessionMergeStarted :: session=" + session + " newSession=" + newSession + ", profile=" + profile);
        }

        private boolean doesCallSessionExistsInMerge(ImsCallSession cs) {
            String callId = cs.getCallId();
            if ((ImsCall.this.isMergeHost() && Objects.equals(ImsCall.this.mMergePeer.mSession.getCallId(), callId)) || (ImsCall.this.isMergePeer() && Objects.equals(ImsCall.this.mMergeHost.mSession.getCallId(), callId))) {
                return true;
            }
            return Objects.equals(ImsCall.this.mSession.getCallId(), callId);
        }

        @Override
        public void callSessionMergeComplete(ImsCallSession newSession) {
            ImsCall.this.logi("callSessionMergeComplete :: newSession =" + newSession);
            if (!ImsCall.this.isMergeHost()) {
                if (ImsCall.this.mMergeHost == null) {
                    ImsCall.this.logd("merge host is null, terminate conf");
                    if (newSession != null) {
                        newSession.terminate(102);
                        return;
                    }
                    return;
                }
                ImsCall.this.mMergeHost.processMergeComplete();
                return;
            }
            if (newSession != null) {
                ImsCall imsCall = ImsCall.this;
                if (doesCallSessionExistsInMerge(newSession)) {
                    newSession = null;
                }
                imsCall.mTransientConferenceSession = newSession;
            }
            ImsCall.this.processMergeComplete();
        }

        @Override
        public void callSessionMergeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            ImsCall.this.loge("callSessionMergeFailed :: session=" + session + "reasonInfo=" + reasonInfo);
            synchronized (ImsCall.this) {
                if (ImsCall.this.isMergeHost()) {
                    ImsCall.this.processMergeFailed(reasonInfo);
                } else if (ImsCall.this.mMergeHost != null) {
                    ImsCall.this.mMergeHost.processMergeFailed(reasonInfo);
                } else {
                    ImsCall.this.loge("callSessionMergeFailed :: No merge host for this conference!");
                }
            }
        }

        @Override
        public void callSessionUpdated(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionUpdated :: session=" + session + " profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionUpdated :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallUpdated(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionUpdated :: ", t);
            }
        }

        @Override
        public void callSessionUpdateFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionUpdateFailed :: session=" + session + " reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionUpdateFailed :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallUpdateFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionUpdateFailed :: ", t);
            }
        }

        @Override
        public void callSessionUpdateReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionUpdateReceived :: session=" + session + " profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionUpdateReceived :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mProposedCallProfile = profile;
                ImsCall.this.mUpdateRequest = 6;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallUpdateReceived(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionUpdateReceived :: ", t);
            }
        }

        @Override
        public void callSessionConferenceExtended(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionConferenceExtended :: session=" + session + " newSession=" + newSession + ", profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionConferenceExtended :: not supported for transient conference session=" + session);
                return;
            }
            ImsCall newCall = ImsCall.this.createNewCall(newSession, profile);
            if (newCall == null) {
                callSessionConferenceExtendFailed(session, new ImsReasonInfo());
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallConferenceExtended(ImsCall.this, newCall);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionConferenceExtended :: ", t);
            }
        }

        @Override
        public void callSessionConferenceExtendFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionConferenceExtendFailed :: reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionConferenceExtendFailed :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallConferenceExtendFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionConferenceExtendFailed :: ", t);
            }
        }

        @Override
        public void callSessionConferenceExtendReceived(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            Listener listener;
            ImsCall.this.logi("callSessionConferenceExtendReceived :: newSession=" + newSession + ", profile=" + profile);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionConferenceExtendReceived :: not supported for transient conference session" + session);
                return;
            }
            ImsCall newCall = ImsCall.this.createNewCall(newSession, profile);
            if (newCall == null) {
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallConferenceExtendReceived(ImsCall.this, newCall);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionConferenceExtendReceived :: ", t);
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestDelivered(ImsCallSession session) {
            Listener listener;
            ImsCall.this.logi("callSessionInviteParticipantsRequestDelivered ::");
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionInviteParticipantsRequestDelivered :: not supported for conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                if (ImsCall.this.mIsConferenceMerging) {
                    ImsCall.this.mIsConferenceMerging = false;
                }
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallInviteParticipantsRequestDelivered(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionInviteParticipantsRequestDelivered :: ", t);
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionInviteParticipantsRequestFailed :: reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionInviteParticipantsRequestFailed :: not supported for conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mIsConferenceMerging = false;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallInviteParticipantsRequestFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionInviteParticipantsRequestFailed :: ", t);
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestDelivered(ImsCallSession session) {
            Listener listener;
            ImsCall.this.logi("callSessionRemoveParticipantsRequestDelivered ::");
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionRemoveParticipantsRequestDelivered :: not supported for conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallRemoveParticipantsRequestDelivered(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionRemoveParticipantsRequestDelivered :: ", t);
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionRemoveParticipantsRequestFailed :: reasonInfo=" + reasonInfo);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionRemoveParticipantsRequestFailed :: not supported for conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallRemoveParticipantsRequestFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionRemoveParticipantsRequestFailed :: ", t);
            }
        }

        @Override
        public void callSessionConferenceStateUpdated(ImsCallSession session, ImsConferenceState state) {
            ImsCall.this.logi("callSessionConferenceStateUpdated :: state=" + state);
            ImsCall.this.conferenceStateUpdated(state);
        }

        @Override
        public void callSessionUssdMessageReceived(ImsCallSession session, int mode, String ussdMessage) {
            Listener listener;
            ImsCall.this.logi("callSessionUssdMessageReceived :: mode=" + mode + ", ussdMessage=" + ussdMessage);
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionUssdMessageReceived :: not supported for transient conference session=" + session);
                return;
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallUssdMessageReceived(ImsCall.this, mode, ussdMessage);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionUssdMessageReceived :: ", t);
            }
        }

        @Override
        public void callSessionTtyModeReceived(ImsCallSession session, int mode) {
            Listener listener;
            ImsCall.this.logi("callSessionTtyModeReceived :: mode=" + mode);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallSessionTtyModeReceived(ImsCall.this, mode);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionTtyModeReceived :: ", t);
            }
        }

        @Override
        public void callSessionMultipartyStateChanged(ImsCallSession session, boolean isMultiParty) {
            Listener listener;
            if (ImsCall.VDBG) {
                ImsCall.this.logi("callSessionMultipartyStateChanged isMultiParty: " + (isMultiParty ? "Y" : "N"));
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onMultipartyStateChanged(ImsCall.this, isMultiParty);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionMultipartyStateChanged :: ", t);
            }
        }

        @Override
        public void callSessionHandover(ImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.logi("callSessionHandover :: session=" + session + ", srcAccessTech=" + srcAccessTech + ", targetAccessTech=" + targetAccessTech + ", reasonInfo=" + reasonInfo);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallHandover(ImsCall.this, srcAccessTech, targetAccessTech, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionHandover :: ", t);
            }
        }

        @Override
        public void callSessionHandoverFailed(ImsCallSession session, int srcAccessTech, int targetAccessTech, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionHandoverFailed :: session=" + session + ", srcAccessTech=" + srcAccessTech + ", targetAccessTech=" + targetAccessTech + ", reasonInfo=" + reasonInfo);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallHandoverFailed(ImsCall.this, srcAccessTech, targetAccessTech, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionHandoverFailed :: ", t);
            }
        }

        @Override
        public void callSessionSuppServiceReceived(ImsCallSession session, ImsSuppServiceNotification suppServiceInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.logi("callSessionSuppServiceReceived :: not supported for transient conference session=" + session);
                return;
            }
            ImsCall.this.logi("callSessionSuppServiceReceived :: session=" + session + ", suppServiceInfo" + suppServiceInfo);
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallSuppServiceReceived(ImsCall.this, suppServiceInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionSuppServiceReceived :: ", t);
            }
        }

        @Override
        public void callSessionTransferred(ImsCallSession session) {
            Listener listener;
            ImsCall.this.loge("callSessionTransferred :: session=" + session);
            synchronized (ImsCall.this) {
                ImsCall.this.mUpdateRequest = 0;
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallTransferred(ImsCall.this);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionTransferred :: ", t);
            }
        }

        @Override
        public void callSessionTransferFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            ImsCall.this.loge("callSessionTransferFailed :: session=" + session + " reasonInfo=" + reasonInfo);
            synchronized (ImsCall.this) {
                ImsCall.this.mUpdateRequest = 0;
                listener = ImsCall.this.mListener;
            }
            if (listener == null) {
                return;
            }
            try {
                listener.onCallTransferFailed(ImsCall.this, reasonInfo);
            } catch (Throwable t) {
                ImsCall.this.loge("callSessionTransferFailed :: ", t);
            }
        }
    }

    public void conferenceStateUpdated(ImsConferenceState state) {
        Listener listener;
        synchronized (this) {
            notifyConferenceStateUpdated(state);
            listener = this.mListener;
        }
        if (listener == null) {
            return;
        }
        try {
            listener.onCallConferenceStateUpdated(this, state);
        } catch (Throwable t) {
            loge("callSessionConferenceStateUpdated :: ", t);
        }
    }

    private String updateRequestToString(int updateRequest) {
        switch (updateRequest) {
            case 0:
                return "NONE";
            case 1:
                return "HOLD";
            case 2:
                return "HOLD_MERGE";
            case 3:
                return "RESUME";
            case 4:
                return "MERGE";
            case 5:
                return "EXTEND_TO_CONFERENCE";
            case 6:
                return "UNSPECIFIED";
            case 7:
                return "ECT";
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

    private void setMergePeer(ImsCall mergePeer) {
        this.mMergePeer = mergePeer;
        this.mMergeHost = null;
        mergePeer.mMergeHost = this;
        mergePeer.mMergePeer = null;
    }

    public void setMergeHost(ImsCall mergeHost) {
        this.mMergeHost = mergeHost;
        this.mMergePeer = null;
        mergeHost.mMergeHost = null;
        mergeHost.mMergePeer = this;
    }

    private boolean isMerging() {
        return (this.mMergePeer == null && this.mMergeHost == null) ? false : true;
    }

    private boolean isMergeHost() {
        return this.mMergePeer != null && this.mMergeHost == null;
    }

    private boolean isMergePeer() {
        return this.mMergePeer == null && this.mMergeHost != null;
    }

    private boolean isCallSessionMergePending() {
        return this.mCallSessionMergePending;
    }

    private void setCallSessionMergePending(boolean callSessionMergePending) {
        this.mCallSessionMergePending = callSessionMergePending;
    }

    private boolean shouldProcessConferenceResult() {
        boolean areMergeTriggersDone = false;
        synchronized (this) {
            if (!isMergeHost() && !isMergePeer()) {
                loge("shouldProcessConferenceResult :: no merge in progress");
                return false;
            }
            if (isMergeHost()) {
                logi("shouldProcessConferenceResult :: We are a merge host");
                logi("shouldProcessConferenceResult :: Here is the merge peer=" + this.mMergePeer);
                areMergeTriggersDone = (isCallSessionMergePending() || this.mMergePeer.isCallSessionMergePending()) ? false : true;
                if (!isMultiparty()) {
                    areMergeTriggersDone &= isSessionAlive(this.mTransientConferenceSession);
                }
            } else if (isMergePeer()) {
                logi("shouldProcessConferenceResult :: We are a merge peer");
                logi("shouldProcessConferenceResult :: Here is the merge host=" + this.mMergeHost);
                boolean areMergeTriggersDone2 = (isCallSessionMergePending() || this.mMergeHost.isCallSessionMergePending()) ? false : true;
                if (!this.mMergeHost.isMultiparty()) {
                    areMergeTriggersDone = areMergeTriggersDone2 & isSessionAlive(this.mMergeHost.mTransientConferenceSession);
                } else {
                    areMergeTriggersDone = !isCallSessionMergePending();
                }
            } else {
                loge("shouldProcessConferenceResult : merge in progress but call is neither host nor peer.");
            }
            logi("shouldProcessConferenceResult :: returning:" + (areMergeTriggersDone ? "true" : "false"));
            return areMergeTriggersDone;
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
        sb.append(" session:");
        sb.append(this.mSession);
        sb.append(" transientSession:");
        sb.append(this.mTransientConferenceSession);
        sb.append("]");
        return sb.toString();
    }

    private void throwImsException(Throwable t, int code) throws Throwable {
        if (t instanceof ImsException) {
            throw ((ImsException) t);
        }
        throw new ImsException(String.valueOf(code), t, code);
    }

    private String appendImsCallInfoToString(String s) {
        return s + " ImsCall=" + this;
    }

    private void logi(String s) {
        Log.i(TAG, appendImsCallInfoToString(s));
    }

    private void logd(String s) {
        Log.d(TAG, appendImsCallInfoToString(s));
    }

    private void logv(String s) {
        Log.v(TAG, appendImsCallInfoToString(s));
    }

    private void loge(String s) {
        Log.e(TAG, appendImsCallInfoToString(s));
    }

    private void loge(String s, Throwable t) {
        Log.e(TAG, appendImsCallInfoToString(s), t);
    }

    public boolean isIncomingCallMultiparty() {
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                return false;
            }
            return this.mSession.isIncomingCallMultiparty();
        }
    }

    public void explicitCallTransfer() throws ImsException {
        logi("explicitCallTransfer :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("explicitCallTransfer :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            this.mSession.explicitCallTransfer();
            this.mUpdateRequest = 7;
        }
    }

    public void unattendedCallTransfer(String number, int type) throws ImsException {
        logi("explicitCallTransfer :: ");
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("explicitCallTransfer :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                throw new ImsException("No call session", 148);
            }
            this.mSession.unattendedCallTransfer(number, type);
            this.mUpdateRequest = 7;
        }
    }
}
