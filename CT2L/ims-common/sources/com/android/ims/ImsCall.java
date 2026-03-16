package com.android.ims;

import android.R;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.telecom.ConferenceParticipant;
import android.telephony.Rlog;
import com.android.ims.internal.ICall;
import com.android.ims.internal.ImsCallSession;
import com.android.ims.internal.ImsStreamMediaSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImsCall implements ICall {
    private static final boolean FORCE_DEBUG = false;
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
    private Context mContext;
    private static final String TAG = "ImsCall";
    private static final boolean DBG = Rlog.isLoggable(TAG, 3);
    private static final boolean VDBG = Rlog.isLoggable(TAG, 2);
    private Object mLockObj = new Object();
    private boolean mInCall = FORCE_DEBUG;
    private boolean mHold = FORCE_DEBUG;
    private boolean mMute = FORCE_DEBUG;
    private int mUpdateRequest = 0;
    private Listener mListener = null;
    private ImsCall mMergePeer = null;
    private ImsCall mMergeHost = null;
    private ImsCallSession mSession = null;
    private ImsCallProfile mProposedCallProfile = null;
    private ImsReasonInfo mLastReasonInfo = null;
    private ImsStreamMediaSession mMediaSession = null;
    private ImsCallSession mTransientConferenceSession = null;
    private boolean mSessionEndDuringMerge = FORCE_DEBUG;
    private ImsReasonInfo mSessionEndDuringMergeReasonInfo = null;
    private boolean mIsMerged = FORCE_DEBUG;
    private boolean mCallSessionMergePending = FORCE_DEBUG;

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

        public void onCallMerged(ImsCall call, boolean swapCalls) {
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

        public void onCallSessionTtyModeReceived(ImsCall call, int mode) {
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
            }
            this.mCallProfile = null;
            this.mProposedCallProfile = null;
            this.mLastReasonInfo = null;
            this.mMediaSession = null;
        }
    }

    @Override
    public boolean checkIfRemoteUserIsSame(String userId) {
        return userId == null ? FORCE_DEBUG : userId.equals(this.mCallProfile.getCallExtra("remote_uri", ""));
    }

    @Override
    public boolean equalsTo(ICall call) {
        return (call != null && (call instanceof ImsCall)) ? equals(call) : FORCE_DEBUG;
    }

    public static boolean isSessionAlive(ImsCallSession session) {
        if (session == null || !session.isAlive()) {
            return FORCE_DEBUG;
        }
        return true;
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
        ImsCallProfile imsCallProfile;
        synchronized (this.mLockObj) {
            imsCallProfile = !isInCall() ? null : this.mProposedCallProfile;
        }
        return imsCallProfile;
    }

    public int getState() {
        int state;
        synchronized (this.mLockObj) {
            state = this.mSession == null ? 0 : this.mSession.getState();
        }
        return state;
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
            z = this.mUpdateRequest != 0 ? true : FORCE_DEBUG;
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
        boolean zIsMultiparty;
        synchronized (this.mLockObj) {
            zIsMultiparty = this.mSession == null ? FORCE_DEBUG : this.mSession.isMultiparty();
        }
        return zIsMultiparty;
    }

    public void setIsMerged(boolean isMerged) {
        this.mIsMerged = isMerged;
    }

    public boolean isMerged() {
        return this.mIsMerged;
    }

    public void setListener(Listener listener) {
        setListener(listener, FORCE_DEBUG);
    }

    public void setListener(Listener listener, boolean callbackImmediately) {
        synchronized (this.mLockObj) {
            this.mListener = listener;
            if (listener != null && callbackImmediately) {
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
                    loge("setListener()", t);
                }
            }
        }
    }

    public void setMute(boolean muted) throws ImsException {
        synchronized (this.mLockObj) {
            if (this.mMute != muted) {
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
        if (DBG) {
            log("attachSession :: session=" + session);
        }
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
        if (DBG) {
            log("start(1) :: session=" + session + ", callee=" + callee);
        }
        synchronized (this.mLockObj) {
            this.mSession = session;
            try {
                session.setListener(createCallSessionListener());
                session.start(callee, this.mCallProfile);
            } catch (Throwable t) {
                loge("start(1) :: ", t);
                throw new ImsException("start(1)", t, 0);
            }
        }
    }

    public void start(ImsCallSession session, String[] participants) throws ImsException {
        if (DBG) {
            log("start(n) :: session=" + session + ", callee=" + participants);
        }
        synchronized (this.mLockObj) {
            this.mSession = session;
            try {
                session.setListener(createCallSessionListener());
                session.start(participants, this.mCallProfile);
            } catch (Throwable t) {
                loge("start(n) :: ", t);
                throw new ImsException("start(n)", t, 0);
            }
        }
    }

    public void accept(int callType) throws ImsException {
        if (VDBG) {
            log("accept ::");
        }
        accept(callType, createAcceptMediaProfile(callType));
    }

    public void accept(int callType, ImsStreamMediaProfile profile) throws ImsException {
        if (VDBG) {
            log("accept :: callType=" + callType + ", profile=" + profile);
        }
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                throw new ImsException("No call to answer", 148);
            }
            try {
                this.mSession.accept(callType, profile);
                if (this.mInCall && this.mProposedCallProfile != null) {
                    if (DBG) {
                        log("accept :: call profile will be updated");
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
        if (VDBG) {
            log("reject :: reason=" + reason);
        }
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.reject(reason);
            }
            if (this.mInCall && this.mProposedCallProfile != null) {
                if (DBG) {
                    log("reject :: call profile is not updated; destroy it...");
                }
                this.mProposedCallProfile = null;
            }
            if (this.mInCall && this.mUpdateRequest == 6) {
                this.mUpdateRequest = 0;
            }
        }
    }

    public void terminate(int reason) throws ImsException {
        if (VDBG) {
            log("terminate :: ImsCall=" + this + " reason=" + reason);
        }
        synchronized (this.mLockObj) {
            this.mHold = FORCE_DEBUG;
            this.mInCall = FORCE_DEBUG;
            if (this.mSession != null) {
                this.mSession.terminate(reason);
            }
        }
    }

    public void hold() throws ImsException {
        if (VDBG) {
            log("hold :: ImsCall=" + this);
        }
        if (isOnHold()) {
            if (DBG) {
                log("hold :: call is already on hold");
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
                loge("hold :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.hold(createHoldMediaProfile());
            this.mHold = true;
            this.mUpdateRequest = 1;
        }
    }

    public void resume() throws ImsException {
        if (VDBG) {
            log("resume :: ImsCall=" + this);
        }
        if (!isOnHold()) {
            if (DBG) {
                log("resume :: call is in conversation");
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
        if (VDBG) {
            log("merge :: ImsCall=" + this);
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                loge("merge :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                throw new ImsException("Call update is in progress", 102);
            }
            if (this.mSession == null) {
                loge("merge :: no call session");
                throw new ImsException("No call session", 148);
            }
            if (this.mHold || this.mContext.getResources().getBoolean(R.^attr-private.layout_hasNestedScrollIndicator)) {
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
        if (VDBG) {
            log("merge(1) :: bgImsCall=" + bgCall);
        }
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
        merge();
    }

    public void update(int callType, ImsStreamMediaProfile mediaProfile) throws ImsException {
        if (VDBG) {
            log("update ::");
        }
        if (isOnHold()) {
            if (DBG) {
                log("update :: call is on hold");
            }
            throw new ImsException("Not in a call to update call", 102);
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                if (DBG) {
                    log("update :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
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
        if (VDBG) {
            log("extendToConference ::");
        }
        if (isOnHold()) {
            if (DBG) {
                log("extendToConference :: call is on hold");
            }
            throw new ImsException("Not in a call to extend a call to conference", 102);
        }
        synchronized (this.mLockObj) {
            if (this.mUpdateRequest != 0) {
                if (DBG) {
                    log("extendToConference :: update is in progress; request=" + updateRequestToString(this.mUpdateRequest));
                }
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
        if (VDBG) {
            log("inviteParticipants ::");
        }
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("inviteParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.inviteParticipants(participants);
        }
    }

    public void removeParticipants(String[] participants) throws ImsException {
        if (DBG) {
            log("removeParticipants ::");
        }
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("removeParticipants :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.removeParticipants(participants);
        }
    }

    public void sendDtmf(char c, Message result) {
        if (VDBG) {
            log("sendDtmf :: code=" + c);
        }
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.sendDtmf(c, result);
            }
        }
    }

    public void startDtmf(char c) {
        if (DBG) {
            log("startDtmf :: session=" + this.mSession + ", code=" + c);
        }
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.startDtmf(c);
            }
        }
    }

    public void stopDtmf() {
        if (DBG) {
            log("stopDtmf :: session=" + this.mSession);
        }
        synchronized (this.mLockObj) {
            if (this.mSession != null) {
                this.mSession.stopDtmf();
            }
        }
    }

    public void sendUssd(String ussdMessage) throws ImsException {
        if (VDBG) {
            log("sendUssd :: ussdMessage=" + ussdMessage);
        }
        synchronized (this.mLockObj) {
            if (this.mSession == null) {
                loge("sendUssd :: ");
                throw new ImsException("No call session", 148);
            }
            this.mSession.sendUssd(ussdMessage);
        }
    }

    private void clear(ImsReasonInfo lastReasonInfo) {
        this.mInCall = FORCE_DEBUG;
        this.mHold = FORCE_DEBUG;
        this.mUpdateRequest = 0;
        this.mLastReasonInfo = lastReasonInfo;
    }

    private ImsCallSession.Listener createCallSessionListener() {
        return new ImsCallSessionListenerProxy();
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
        if (this.mCallProfile != null) {
            mediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
            mediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
            mediaProfile.mAudioDirection = 2;
            if (mediaProfile.mVideoQuality != 0) {
                mediaProfile.mVideoDirection = 2;
            }
        }
        return mediaProfile;
    }

    private ImsStreamMediaProfile createResumeMediaProfile() {
        ImsStreamMediaProfile mediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile != null) {
            mediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
            mediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
            mediaProfile.mAudioDirection = 3;
            if (mediaProfile.mVideoQuality != 0) {
                mediaProfile.mVideoDirection = 3;
            }
        }
        return mediaProfile;
    }

    private ImsStreamMediaProfile createAcceptMediaProfile(int callType) {
        ImsStreamMediaProfile mediaProfile = new ImsStreamMediaProfile();
        if (this.mCallProfile != null) {
            mediaProfile.mAudioQuality = this.mCallProfile.mMediaProfile.mAudioQuality;
            mediaProfile.mAudioDirection = 3;
            if (callType == 4) {
                mediaProfile.mVideoQuality = this.mCallProfile.mMediaProfile.mVideoQuality;
                mediaProfile.mVideoDirection = 3;
            }
        }
        return mediaProfile;
    }

    private void enforceConversationMode() {
        if (this.mInCall) {
            this.mHold = FORCE_DEBUG;
            this.mUpdateRequest = 0;
        }
    }

    private void mergeInternal() {
        if (VDBG) {
            log("mergeInternal :: ImsCall=" + this);
        }
        this.mSession.merge();
        this.mUpdateRequest = 4;
    }

    private void notifyConferenceSessionTerminated(ImsReasonInfo reasonInfo) {
        Listener listener = this.mListener;
        clear(reasonInfo);
        if (listener != null) {
            try {
                listener.onCallTerminated(this, reasonInfo);
            } catch (Throwable t) {
                loge("notifyConferenceSessionTerminated :: ", t);
            }
        }
    }

    private void notifyConferenceStateUpdated(ImsConferenceState state) {
        Set<Map.Entry<String, Bundle>> participants = state.mParticipants.entrySet();
        if (participants != null) {
            List<ConferenceParticipant> conferenceParticipants = new ArrayList<>(participants.size());
            for (Map.Entry<String, Bundle> entry : participants) {
                String key = entry.getKey();
                Bundle confInfo = entry.getValue();
                String status = confInfo.getString("status");
                String user = confInfo.getString("user");
                String displayName = confInfo.getString("display-text");
                String endpoint = confInfo.getString("endpoint");
                if (DBG) {
                    log("notifyConferenceStateUpdated :: key=" + key + ", status=" + status + ", user=" + user + ", displayName= " + displayName + ", endpoint=" + endpoint);
                }
                Uri handle = Uri.parse(user);
                Uri endpointUri = Uri.parse(endpoint);
                int connectionState = ImsConferenceState.getConnectionStateForStatus(status);
                ConferenceParticipant conferenceParticipant = new ConferenceParticipant(handle, displayName, endpointUri, connectionState);
                conferenceParticipants.add(conferenceParticipant);
            }
            if (!conferenceParticipants.isEmpty() && this.mListener != null) {
                try {
                    this.mListener.onConferenceParticipantsStateChanged(this, conferenceParticipants);
                } catch (Throwable t) {
                    loge("notifyConferenceStateUpdated :: ", t);
                }
            }
        }
    }

    private void processCallTerminated(ImsReasonInfo reasonInfo) {
        if (VDBG) {
            String reasonString = reasonInfo != null ? reasonInfo.toString() : "null";
            log("processCallTerminated :: ImsCall=" + this + " reason=" + reasonString);
        }
        synchronized (this) {
            if (isCallSessionMergePending()) {
                if (DBG) {
                    log("processCallTerminated :: burying termination during ongoing merge.");
                }
                this.mSessionEndDuringMerge = true;
                this.mSessionEndDuringMergeReasonInfo = reasonInfo;
                return;
            }
            if (isMultiparty()) {
                notifyConferenceSessionTerminated(reasonInfo);
                return;
            }
            Listener listener = this.mListener;
            clear(reasonInfo);
            if (listener != null) {
                try {
                    listener.onCallTerminated(this, reasonInfo);
                } catch (Throwable t) {
                    loge("processCallTerminated :: ", t);
                }
            }
        }
    }

    private boolean isTransientConferenceSession(ImsCallSession session) {
        if (session == null || session == this.mSession || session != this.mTransientConferenceSession) {
            return FORCE_DEBUG;
        }
        return true;
    }

    private void setTransientSessionAsPrimary(ImsCallSession transientSession) {
        synchronized (this) {
            this.mSession.setListener(null);
            this.mSession = transientSession;
            this.mSession.setListener(createCallSessionListener());
        }
    }

    private void tryProcessConferenceResult() {
        if (shouldProcessConferenceResult()) {
            if (isMergeHost()) {
                processMergeComplete();
            } else if (this.mMergeHost != null) {
                this.mMergeHost.processMergeComplete();
            } else {
                loge("tryProcessConferenceResult :: No merge host for this conference!");
            }
        }
    }

    private void processMergeComplete() {
        if (VDBG) {
            log("processMergeComplete :: ImsCall=" + this);
        }
        if (!isMergeHost()) {
            loge("processMergeComplete :: We are not the merge host!");
            return;
        }
        boolean swapRequired = FORCE_DEBUG;
        synchronized (this) {
            ImsCall finalHostCall = this;
            ImsCall finalPeerCall = this.mMergePeer;
            if (isMultiparty()) {
                setIsMerged(FORCE_DEBUG);
                if (!isSessionAlive(this.mMergePeer.mSession)) {
                    this.mMergePeer.setIsMerged(true);
                } else {
                    this.mMergePeer.setIsMerged(FORCE_DEBUG);
                }
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
                    finalHostCall = this.mMergePeer;
                    finalPeerCall = this;
                    swapRequired = true;
                    setIsMerged(FORCE_DEBUG);
                    this.mMergePeer.setIsMerged(FORCE_DEBUG);
                    if (VDBG) {
                        log("processMergeComplete :: transient will transfer to merge peer");
                    }
                } else if (!isSessionAlive(this.mSession) && isSessionAlive(this.mMergePeer.getCallSession())) {
                    finalHostCall = this;
                    finalPeerCall = this.mMergePeer;
                    swapRequired = FORCE_DEBUG;
                    setIsMerged(FORCE_DEBUG);
                    this.mMergePeer.setIsMerged(FORCE_DEBUG);
                    if (VDBG) {
                        log("processMergeComplete :: transient will stay with the merge host");
                    }
                } else {
                    finalHostCall = this;
                    finalPeerCall = this.mMergePeer;
                    swapRequired = FORCE_DEBUG;
                    setIsMerged(FORCE_DEBUG);
                    this.mMergePeer.setIsMerged(true);
                    if (VDBG) {
                        log("processMergeComplete :: transient will stay with us (I'm the host).");
                    }
                }
                if (VDBG) {
                    log("processMergeComplete :: call=" + finalHostCall + " is the final host");
                }
                finalHostCall.setTransientSessionAsPrimary(transientConferenceSession);
            }
            Listener listener = finalHostCall.mListener;
            clearMergeInfo();
            finalPeerCall.notifySessionTerminatedDuringMerge();
            finalHostCall.clearSessionTerminationFlags();
            if (listener != null) {
                try {
                    listener.onCallMerged(this, swapRequired);
                } catch (Throwable t) {
                    loge("processMergeComplete :: ", t);
                }
            }
        }
    }

    private void notifySessionTerminatedDuringMerge() {
        Listener listener;
        boolean notifyFailure = FORCE_DEBUG;
        ImsReasonInfo notifyFailureReasonInfo = null;
        synchronized (this) {
            listener = this.mListener;
            if (this.mSessionEndDuringMerge) {
                if (DBG) {
                    log("notifySessionTerminatedDuringMerge ::reporting terminate during merge");
                }
                notifyFailure = true;
                notifyFailureReasonInfo = this.mSessionEndDuringMergeReasonInfo;
            }
            clearSessionTerminationFlags();
        }
        if (listener != null && notifyFailure) {
            try {
                processCallTerminated(notifyFailureReasonInfo);
            } catch (Throwable t) {
                loge("notifySessionTerminatedDuringMerge :: ", t);
            }
        }
    }

    private void clearSessionTerminationFlags() {
        this.mSessionEndDuringMerge = FORCE_DEBUG;
        this.mSessionEndDuringMergeReasonInfo = null;
    }

    private void processMergeFailed(ImsReasonInfo reasonInfo) {
        if (VDBG) {
            log("processMergeFailed :: this=" + this + "reason=" + reasonInfo);
        }
        synchronized (this) {
            if (!isMergeHost()) {
                loge("processMergeFailed :: We are not the merge host!");
                return;
            }
            if (this.mMergePeer == null) {
                loge("processMergeFailed :: No merge peer!");
                return;
            }
            if (!isMultiparty()) {
                if (this.mTransientConferenceSession == null) {
                    loge("processMergeFailed :: No transient session!");
                    return;
                } else {
                    this.mTransientConferenceSession.setListener(null);
                    this.mTransientConferenceSession = null;
                }
            }
            setIsMerged(FORCE_DEBUG);
            this.mMergePeer.setIsMerged(FORCE_DEBUG);
            Listener listener = this.mListener;
            notifySessionTerminatedDuringMerge();
            this.mMergePeer.notifySessionTerminatedDuringMerge();
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

    private void notifyError(int reason, int statusCode, String message) {
    }

    private void throwImsException(Throwable t, int code) throws Throwable {
        if (t instanceof ImsException) {
            throw ((ImsException) t);
        }
        throw new ImsException(String.valueOf(code), t, code);
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void logv(String s) {
        Rlog.v(TAG, s + " imsCall=" + this);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }

    private void loge(String s, Throwable t) {
        Rlog.e(TAG, s, t);
    }

    private class ImsCallSessionListenerProxy extends ImsCallSession.Listener {
        private ImsCallSessionListenerProxy() {
        }

        @Override
        public void callSessionProgressing(ImsCallSession session, ImsStreamMediaProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionProgressing :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionProgressing :: session=" + session + " profile=" + profile);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile.mMediaProfile.copyFrom(profile);
            }
            if (listener != null) {
                try {
                    listener.onCallProgressing(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionProgressing :: ", t);
                }
            }
        }

        @Override
        public void callSessionStarted(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionStarted :: session=" + session + " profile=" + profile);
            }
            if (!ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.setCallSessionMergePending(ImsCall.FORCE_DEBUG);
                ImsCall.this.tryProcessConferenceResult();
                if (!ImsCall.this.isTransientConferenceSession(session)) {
                    synchronized (ImsCall.this) {
                        listener = ImsCall.this.mListener;
                        ImsCall.this.mCallProfile = profile;
                    }
                    if (listener != null) {
                        try {
                            listener.onCallStarted(ImsCall.this);
                            return;
                        } catch (Throwable t) {
                            ImsCall.this.loge("callSessionStarted :: ", t);
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionStarted :: on transient session=" + session);
            }
        }

        @Override
        public void callSessionStartFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionStartFailed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionStartFailed :: session=" + session + " reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mLastReasonInfo = reasonInfo;
            }
            if (listener != null) {
                try {
                    listener.onCallStartFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionStarted :: ", t);
                }
            }
        }

        @Override
        public void callSessionTerminated(ImsCallSession session, ImsReasonInfo reasonInfo) {
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionTerminated :: on transient session=" + session);
                ImsCall.this.processMergeFailed(reasonInfo);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionTerminated :: session=" + session + " reasonInfo=" + reasonInfo);
            }
            ImsCall.this.processCallTerminated(reasonInfo);
            ImsCall.this.setCallSessionMergePending(ImsCall.FORCE_DEBUG);
            ImsCall.this.tryProcessConferenceResult();
        }

        @Override
        public void callSessionHeld(ImsCallSession session, ImsCallProfile profile) {
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionHeld :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionHeld :: session=" + session + "profile=" + profile);
            }
            synchronized (ImsCall.this) {
                ImsCall.this.setCallSessionMergePending(ImsCall.FORCE_DEBUG);
                ImsCall.this.mCallProfile = profile;
                if (ImsCall.this.mUpdateRequest == 2) {
                    ImsCall.this.mergeInternal();
                } else {
                    ImsCall.this.tryProcessConferenceResult();
                    ImsCall.this.mUpdateRequest = 0;
                    Listener listener = ImsCall.this.mListener;
                    if (listener != null) {
                        try {
                            listener.onCallHeld(ImsCall.this);
                        } catch (Throwable t) {
                            ImsCall.this.loge("callSessionHeld :: ", t);
                        }
                    }
                }
            }
        }

        @Override
        public void callSessionHoldFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionHoldFailed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionHoldFailed :: session" + session + "reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                if (ImsCall.this.mUpdateRequest == 2) {
                }
                ImsCall.this.mUpdateRequest = 0;
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallHoldFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionHoldFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionHoldReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionHoldReceived :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionHoldReceived :: session=" + session + "profile=" + profile);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
            }
            if (listener != null) {
                try {
                    listener.onCallHoldReceived(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionHoldReceived :: ", t);
                }
            }
        }

        @Override
        public void callSessionResumed(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionResumed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionResumed :: session=" + session + "profile=" + profile);
            }
            ImsCall.this.setCallSessionMergePending(ImsCall.FORCE_DEBUG);
            ImsCall.this.tryProcessConferenceResult();
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
                ImsCall.this.mUpdateRequest = 0;
                ImsCall.this.mHold = ImsCall.FORCE_DEBUG;
            }
            if (listener != null) {
                try {
                    listener.onCallResumed(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionResumed :: ", t);
                }
            }
        }

        @Override
        public void callSessionResumeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionResumeFailed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionResumeFailed :: session=" + session + "reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallResumeFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionResumeFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionResumeReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionResumeReceived :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionResumeReceived :: session=" + session + "profile=" + profile);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
            }
            if (listener != null) {
                try {
                    listener.onCallResumeReceived(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionResumeReceived :: ", t);
                }
            }
        }

        @Override
        public void callSessionMergeStarted(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionMergeStarted :: session=" + session + " newSession=" + newSession + ", profile=" + profile);
            }
            if (!ImsCall.this.isCallSessionMergePending()) {
                ImsCall.this.log("callSessionMergeStarted :: no merge in progress.");
                return;
            }
            if (session == null) {
                if (ImsCall.DBG) {
                    ImsCall.this.log("callSessionMergeStarted :: merging into existing ImsCallSession");
                }
            } else {
                if (ImsCall.DBG) {
                    ImsCall.this.log("callSessionMergeStarted ::  setting our transient ImsCallSession");
                }
                synchronized (ImsCall.this) {
                    ImsCall.this.mTransientConferenceSession = newSession;
                    ImsCall.this.mTransientConferenceSession.setListener(ImsCall.this.createCallSessionListener());
                }
            }
        }

        @Override
        public void callSessionMergeComplete(ImsCallSession session) {
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionMergeComplete :: session=" + session);
            }
            ImsCall.this.setCallSessionMergePending(ImsCall.FORCE_DEBUG);
            ImsCall.this.tryProcessConferenceResult();
        }

        @Override
        public void callSessionMergeFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionMergeFailed :: session=" + session + "reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                if (!ImsCall.this.isCallSessionMergePending()) {
                    ImsCall.this.log("callSessionMergeFailed :: no merge in progress.");
                    return;
                }
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
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionUpdated :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionUpdated :: session=" + session + " profile=" + profile);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mCallProfile = profile;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallUpdated(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionUpdated :: ", t);
                }
            }
        }

        @Override
        public void callSessionUpdateFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionUpdateFailed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionUpdateFailed :: session=" + session + " reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallUpdateFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionUpdateFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionUpdateReceived(ImsCallSession session, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionUpdateReceived :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionUpdateReceived :: session=" + session + " profile=" + profile);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mProposedCallProfile = profile;
                ImsCall.this.mUpdateRequest = 6;
            }
            if (listener != null) {
                try {
                    listener.onCallUpdateReceived(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionUpdateReceived :: ", t);
                }
            }
        }

        @Override
        public void callSessionConferenceExtended(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionConferenceExtended :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionConferenceExtended :: session=" + session + " newSession=" + newSession + ", profile=" + profile);
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
            if (listener != null) {
                try {
                    listener.onCallConferenceExtended(ImsCall.this, newCall);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionConferenceExtended :: ", t);
                }
            }
        }

        @Override
        public void callSessionConferenceExtendFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionConferenceExtendFailed :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.DBG) {
                ImsCall.this.log("callSessionConferenceExtendFailed :: imsCall=" + ImsCall.this + ", reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
                ImsCall.this.mUpdateRequest = 0;
            }
            if (listener != null) {
                try {
                    listener.onCallConferenceExtendFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionConferenceExtendFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionConferenceExtendReceived(ImsCallSession session, ImsCallSession newSession, ImsCallProfile profile) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionConferenceExtendReceived :: not supported for transient conference session" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionConferenceExtendReceived :: newSession=" + newSession + ", profile=" + profile);
            }
            ImsCall newCall = ImsCall.this.createNewCall(newSession, profile);
            if (newCall != null) {
                synchronized (ImsCall.this) {
                    listener = ImsCall.this.mListener;
                }
                if (listener != null) {
                    try {
                        listener.onCallConferenceExtendReceived(ImsCall.this, newCall);
                    } catch (Throwable t) {
                        ImsCall.this.loge("callSessionConferenceExtendReceived :: ", t);
                    }
                }
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestDelivered(ImsCallSession session) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionInviteParticipantsRequestDelivered :: not supported for conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionInviteParticipantsRequestDelivered ::");
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallInviteParticipantsRequestDelivered(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionInviteParticipantsRequestDelivered :: ", t);
                }
            }
        }

        @Override
        public void callSessionInviteParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionInviteParticipantsRequestFailed :: not supported for conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionInviteParticipantsRequestFailed :: reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallInviteParticipantsRequestFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionInviteParticipantsRequestFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestDelivered(ImsCallSession session) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionRemoveParticipantsRequestDelivered :: not supported for conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionRemoveParticipantsRequestDelivered ::");
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallRemoveParticipantsRequestDelivered(ImsCall.this);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionRemoveParticipantsRequestDelivered :: ", t);
                }
            }
        }

        @Override
        public void callSessionRemoveParticipantsRequestFailed(ImsCallSession session, ImsReasonInfo reasonInfo) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionRemoveParticipantsRequestFailed :: not supported for conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionRemoveParticipantsRequestFailed :: reasonInfo=" + reasonInfo);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallRemoveParticipantsRequestFailed(ImsCall.this, reasonInfo);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionRemoveParticipantsRequestFailed :: ", t);
                }
            }
        }

        @Override
        public void callSessionConferenceStateUpdated(ImsCallSession session, ImsConferenceState state) {
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionConferenceStateUpdated :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionConferenceStateUpdated :: state=" + state);
            }
            ImsCall.this.conferenceStateUpdated(state);
        }

        @Override
        public void callSessionUssdMessageReceived(ImsCallSession session, int mode, String ussdMessage) {
            Listener listener;
            if (ImsCall.this.isTransientConferenceSession(session)) {
                ImsCall.this.log("callSessionUssdMessageReceived :: not supported for transient conference session=" + session);
                return;
            }
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionUssdMessageReceived :: mode=" + mode + ", ussdMessage=" + ussdMessage);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallUssdMessageReceived(ImsCall.this, mode, ussdMessage);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionUssdMessageReceived :: ", t);
                }
            }
        }

        @Override
        public void callSessionTtyModeReceived(ImsCallSession session, int mode) {
            Listener listener;
            if (ImsCall.VDBG) {
                ImsCall.this.log("callSessionTtyModeReceived :: mode=" + mode);
            }
            synchronized (ImsCall.this) {
                listener = ImsCall.this.mListener;
            }
            if (listener != null) {
                try {
                    listener.onCallSessionTtyModeReceived(ImsCall.this, mode);
                } catch (Throwable t) {
                    ImsCall.this.loge("callSessionTtyModeReceived :: ", t);
                }
            }
        }
    }

    public void conferenceStateUpdated(ImsConferenceState state) {
        Listener listener;
        synchronized (this) {
            notifyConferenceStateUpdated(state);
            listener = this.mListener;
        }
        if (listener != null) {
            try {
                listener.onCallConferenceStateUpdated(this, state);
            } catch (Throwable t) {
                loge("callSessionConferenceStateUpdated :: ", t);
            }
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
            default:
                return "UNKNOWN";
        }
    }

    private void clearMergeInfo() {
        if (VDBG) {
            log("clearMergeInfo :: clearing all merge info");
        }
        if (this.mMergeHost != null) {
            this.mMergeHost.mMergePeer = null;
            this.mMergeHost.mUpdateRequest = 0;
            this.mMergeHost.mCallSessionMergePending = FORCE_DEBUG;
        }
        if (this.mMergePeer != null) {
            this.mMergePeer.mMergeHost = null;
            this.mMergePeer.mUpdateRequest = 0;
            this.mMergePeer.mCallSessionMergePending = FORCE_DEBUG;
        }
        this.mMergeHost = null;
        this.mMergePeer = null;
        this.mUpdateRequest = 0;
        this.mCallSessionMergePending = FORCE_DEBUG;
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
        if (this.mMergePeer == null && this.mMergeHost == null) {
            return FORCE_DEBUG;
        }
        return true;
    }

    private boolean isMergeHost() {
        if (this.mMergePeer == null || this.mMergeHost != null) {
            return FORCE_DEBUG;
        }
        return true;
    }

    private boolean isMergePeer() {
        if (this.mMergePeer != null || this.mMergeHost == null) {
            return FORCE_DEBUG;
        }
        return true;
    }

    private boolean isCallSessionMergePending() {
        return this.mCallSessionMergePending;
    }

    private void setCallSessionMergePending(boolean callSessionMergePending) {
        this.mCallSessionMergePending = callSessionMergePending;
    }

    private boolean shouldProcessConferenceResult() {
        boolean areMergeTriggersDone = FORCE_DEBUG;
        synchronized (this) {
            if (!isMergeHost() && !isMergePeer()) {
                if (VDBG) {
                    log("shouldProcessConferenceResult :: no merge in progress");
                }
                return FORCE_DEBUG;
            }
            if (isMergeHost()) {
                if (VDBG) {
                    log("shouldProcessConferenceResult :: We are a merge host=" + this);
                    log("shouldProcessConferenceResult :: Here is the merge peer=" + this.mMergePeer);
                }
                areMergeTriggersDone = (isCallSessionMergePending() || this.mMergePeer.isCallSessionMergePending()) ? false : true;
                if (!isMultiparty()) {
                    areMergeTriggersDone &= isSessionAlive(this.mTransientConferenceSession);
                }
            } else if (isMergePeer()) {
                if (VDBG) {
                    log("shouldProcessConferenceResult :: We are a merge peer=" + this);
                    log("shouldProcessConferenceResult :: Here is the merge host=" + this.mMergeHost);
                }
                boolean areMergeTriggersDone2 = (isCallSessionMergePending() || this.mMergeHost.isCallSessionMergePending()) ? false : true;
                if (!this.mMergeHost.isMultiparty()) {
                    areMergeTriggersDone = areMergeTriggersDone2 & isSessionAlive(this.mMergeHost.mTransientConferenceSession);
                } else {
                    areMergeTriggersDone = !isCallSessionMergePending();
                }
            } else {
                loge("shouldProcessConferenceResult : merge in progress but call is neitherhost nor peer.");
            }
            if (VDBG) {
                log("shouldProcessConferenceResult :: returning:" + (areMergeTriggersDone ? "true" : "false"));
            }
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
        sb.append(" buried term:");
        sb.append(this.mSessionEndDuringMerge ? "Y" : "N");
        sb.append(" session:");
        sb.append(this.mSession);
        sb.append(" transientSession:");
        sb.append(this.mTransientConferenceSession);
        sb.append("]");
        return sb.toString();
    }
}
