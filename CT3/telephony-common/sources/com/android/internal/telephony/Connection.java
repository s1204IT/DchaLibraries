package com.android.internal.telephony;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Connection {
    public static final int AUDIO_QUALITY_HIGH_DEFINITION = 2;
    public static final int AUDIO_QUALITY_STANDARD = 1;
    private static String LOG_TAG = "Connection";
    protected String mAddress;
    private int mAudioQuality;
    private int mCallSubstate;
    protected String mCnapName;
    protected long mConnectTime;
    protected long mConnectTimeReal;
    private int mConnectionCapabilities;
    protected String mConvertedNumber;
    protected long mCreateTime;
    protected String mDialString;
    protected long mDuration;
    private Bundle mExtras;
    String mForwardingAddress;
    protected long mHoldingStartTime;
    protected boolean mIsIncoming;
    private boolean mIsWifi;
    protected int mNextPostDialChar;
    protected Connection mOrigConnection;
    private int mPhoneType;
    protected String mPostDialString;
    String mRedirectingAddress;
    private String mTelecomCallId;
    Object mUserData;
    private Connection.VideoProvider mVideoProvider;
    private int mVideoState;
    protected int mCnapNamePresentation = 1;
    protected int mNumberPresentation = 1;
    private List<PostDialListener> mPostDialListeners = new ArrayList();
    public Set<Listener> mListeners = new CopyOnWriteArraySet();
    protected boolean mNumberConverted = false;
    protected int mCause = 0;
    protected PostDialState mPostDialState = PostDialState.NOT_STARTED;
    public Call.State mPreHandoverState = Call.State.IDLE;
    public boolean mPreMultipartyState = false;
    public boolean mPreMultipartyHostState = false;

    public static class Capability {
        public static final int IS_EXTERNAL_CONNECTION = 16;
        public static final int IS_PULLABLE = 32;
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL = 1;
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE = 2;
        public static final int SUPPORTS_VT_LOCAL_BIDIRECTIONAL = 4;
        public static final int SUPPORTS_VT_REMOTE_BIDIRECTIONAL = 8;
    }

    public interface Listener {
        void onAddressDisplayChanged();

        void onAudioQualityChanged(int i);

        void onCallSubstateChanged(int i);

        void onConferenceConnectionsConfigured(ArrayList<Connection> arrayList);

        void onConferenceMergedFailed();

        void onConferenceParticipantsChanged(List<ConferenceParticipant> list);

        void onConferenceParticipantsInvited(boolean z);

        void onConnectionCapabilitiesChanged(int i);

        void onExitedEcmMode();

        void onExtrasChanged(Bundle bundle);

        void onMultipartyStateChanged(boolean z);

        void onRemoteHeld(boolean z);

        void onVideoProviderChanged(Connection.VideoProvider videoProvider);

        void onVideoStateChanged(int i);

        void onWifiChanged(boolean z);
    }

    public interface PostDialListener {
        void onPostDialChar(char c);

        void onPostDialWait();
    }

    public abstract void cancelPostDial();

    public abstract Call getCall();

    public abstract long getDisconnectTime();

    public abstract long getHoldDurationMillis();

    public abstract int getNumberPresentation();

    public abstract int getPreciseDisconnectCause();

    public abstract UUSInfo getUUSInfo();

    public abstract String getVendorDisconnectCause();

    public abstract void hangup() throws CallStateException;

    public abstract boolean isMultiparty();

    public abstract void proceedAfterWaitChar();

    public abstract void proceedAfterWildChar(String str);

    public abstract void separate() throws CallStateException;

    public static abstract class ListenerBase implements Listener {
        @Override
        public void onVideoStateChanged(int videoState) {
        }

        @Override
        public void onConnectionCapabilitiesChanged(int capability) {
        }

        @Override
        public void onWifiChanged(boolean isWifi) {
        }

        @Override
        public void onVideoProviderChanged(Connection.VideoProvider videoProvider) {
        }

        @Override
        public void onAudioQualityChanged(int audioQuality) {
        }

        @Override
        public void onConferenceParticipantsChanged(List<ConferenceParticipant> participants) {
        }

        @Override
        public void onCallSubstateChanged(int callSubstate) {
        }

        @Override
        public void onMultipartyStateChanged(boolean isMultiParty) {
        }

        @Override
        public void onConferenceMergedFailed() {
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
        }

        @Override
        public void onExitedEcmMode() {
        }

        @Override
        public void onConferenceParticipantsInvited(boolean isSuccess) {
        }

        @Override
        public void onConferenceConnectionsConfigured(ArrayList<Connection> radioConnections) {
        }

        @Override
        public void onRemoteHeld(boolean isHeld) {
        }

        @Override
        public void onAddressDisplayChanged() {
        }
    }

    protected Connection(int phoneType) {
        this.mPhoneType = phoneType;
    }

    public String getTelecomCallId() {
        return this.mTelecomCallId;
    }

    public void setTelecomCallId(String telecomCallId) {
        this.mTelecomCallId = telecomCallId;
    }

    public String getAddress() {
        return this.mAddress;
    }

    public String getCnapName() {
        return this.mCnapName;
    }

    public String getOrigDialString() {
        return null;
    }

    public int getCnapNamePresentation() {
        return this.mCnapNamePresentation;
    }

    public long getCreateTime() {
        return this.mCreateTime;
    }

    public long getConnectTime() {
        return this.mConnectTime;
    }

    public void setConnectTime(long connectTime) {
        this.mConnectTime = connectTime;
    }

    public long getConnectTimeReal() {
        return this.mConnectTimeReal;
    }

    public long getDurationMillis() {
        if (this.mConnectTimeReal == 0) {
            return 0L;
        }
        if (this.mDuration == 0) {
            return SystemClock.elapsedRealtime() - this.mConnectTimeReal;
        }
        return this.mDuration;
    }

    public long getHoldingStartTime() {
        return this.mHoldingStartTime;
    }

    public int getDisconnectCause() {
        return this.mCause;
    }

    public boolean isIncoming() {
        return this.mIsIncoming;
    }

    public Call.State getState() {
        Call c = getCall();
        if (c == null) {
            return Call.State.IDLE;
        }
        return c.getState();
    }

    public Call.State getStateBeforeHandover() {
        return this.mPreHandoverState;
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        Call c = getCall();
        if (c == null) {
            return null;
        }
        return c.getConferenceParticipants();
    }

    public boolean isAlive() {
        return getState().isAlive();
    }

    public boolean isRinging() {
        return getState().isRinging();
    }

    public Object getUserData() {
        return this.mUserData;
    }

    public void setUserData(Object userdata) {
        this.mUserData = userdata;
    }

    public enum PostDialState {
        NOT_STARTED,
        STARTED,
        WAIT,
        WILD,
        COMPLETE,
        CANCELLED,
        PAUSE;

        public static PostDialState[] valuesCustom() {
            return values();
        }
    }

    public void clearUserData() {
        this.mUserData = null;
    }

    public final void addPostDialListener(PostDialListener listener) {
        if (this.mPostDialListeners.contains(listener)) {
            return;
        }
        this.mPostDialListeners.add(listener);
    }

    public final void removePostDialListener(PostDialListener listener) {
        this.mPostDialListeners.remove(listener);
    }

    protected final void clearPostDialListeners() {
        this.mPostDialListeners.clear();
    }

    protected final void notifyPostDialListeners() {
        if (getPostDialState() != PostDialState.WAIT) {
            return;
        }
        for (PostDialListener listener : new ArrayList(this.mPostDialListeners)) {
            listener.onPostDialWait();
        }
    }

    protected final void notifyPostDialListenersNextChar(char c) {
        for (PostDialListener listener : new ArrayList(this.mPostDialListeners)) {
            listener.onPostDialChar(c);
        }
    }

    public PostDialState getPostDialState() {
        return this.mPostDialState;
    }

    public String getRemainingPostDialString() {
        if (this.mPostDialState == PostDialState.CANCELLED || this.mPostDialState == PostDialState.COMPLETE || this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        return this.mPostDialString.substring(this.mNextPostDialChar);
    }

    public boolean onDisconnect(int cause) {
        return false;
    }

    public Connection getOrigConnection() {
        return this.mOrigConnection;
    }

    public boolean isConferenceHost() {
        return false;
    }

    public boolean isMemberOfPeerConference() {
        return false;
    }

    public void migrateFrom(Connection c) {
        if (c == null) {
            return;
        }
        this.mListeners = c.mListeners;
        this.mDialString = c.getOrigDialString();
        this.mCreateTime = c.getCreateTime();
        this.mConnectTime = c.getConnectTime();
        this.mConnectTimeReal = c.getConnectTimeReal();
        this.mHoldingStartTime = c.getHoldingStartTime();
        this.mOrigConnection = c.getOrigConnection();
        this.mPostDialString = c.mPostDialString;
        this.mNextPostDialChar = c.mNextPostDialChar;
    }

    public final void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public final void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public static int addCapability(int capabilities, int capability) {
        return capabilities | capability;
    }

    public static int removeCapability(int capabilities, int capability) {
        return (~capability) & capabilities;
    }

    public boolean isWifi() {
        return this.mIsWifi;
    }

    public Connection.VideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getAudioQuality() {
        return this.mAudioQuality;
    }

    public int getCallSubstate() {
        return this.mCallSubstate;
    }

    public void setVideoState(int videoState) {
        this.mVideoState = videoState;
        for (Listener l : this.mListeners) {
            l.onVideoStateChanged(this.mVideoState);
        }
    }

    public void setConnectionCapabilities(int capabilities) {
        if (this.mConnectionCapabilities == capabilities) {
            return;
        }
        this.mConnectionCapabilities = capabilities;
        for (Listener l : this.mListeners) {
            l.onConnectionCapabilitiesChanged(this.mConnectionCapabilities);
        }
    }

    public void setWifi(boolean isWifi) {
        this.mIsWifi = isWifi;
        for (Listener l : this.mListeners) {
            l.onWifiChanged(this.mIsWifi);
        }
    }

    public void setAudioQuality(int audioQuality) {
        this.mAudioQuality = audioQuality;
        for (Listener l : this.mListeners) {
            l.onAudioQualityChanged(this.mAudioQuality);
        }
    }

    public void setConnectionExtras(Bundle extras) {
        if (extras != null) {
            this.mExtras = new Bundle(extras);
        } else {
            this.mExtras = null;
        }
        for (Listener l : this.mListeners) {
            l.onExtrasChanged(this.mExtras);
        }
    }

    public Bundle getConnectionExtras() {
        return this.mExtras;
    }

    public void setCallSubstate(int callSubstate) {
        this.mCallSubstate = callSubstate;
        for (Listener l : this.mListeners) {
            l.onCallSubstateChanged(this.mCallSubstate);
        }
    }

    public void setVideoProvider(Connection.VideoProvider videoProvider) {
        this.mVideoProvider = videoProvider;
        for (Listener l : this.mListeners) {
            l.onVideoProviderChanged(this.mVideoProvider);
        }
    }

    public void setConverted(String oriNumber) {
        this.mNumberConverted = true;
        this.mConvertedNumber = this.mAddress;
        this.mAddress = oriNumber;
        this.mDialString = oriNumber;
    }

    public void updateConferenceParticipants(List<ConferenceParticipant> conferenceParticipants) {
        for (Listener l : this.mListeners) {
            l.onConferenceParticipantsChanged(conferenceParticipants);
        }
    }

    public void updateMultipartyState(boolean isMultiparty) {
        for (Listener l : this.mListeners) {
            l.onMultipartyStateChanged(isMultiparty);
        }
    }

    public void onConferenceMergeFailed() {
        for (Listener l : this.mListeners) {
            l.onConferenceMergedFailed();
        }
    }

    public void onExitedEcmMode() {
        for (Listener l : this.mListeners) {
            l.onExitedEcmMode();
        }
    }

    public void onDisconnectConferenceParticipant(Uri endpoint) {
    }

    public void pullExternalCall() {
    }

    public int getPhoneType() {
        return this.mPhoneType;
    }

    public String toString() {
        StringBuilder str = new StringBuilder(128);
        str.append(" callId: ").append(getTelecomCallId());
        if (Rlog.isLoggable(LOG_TAG, 3)) {
            str.append("addr: ").append(getAddress()).append(" pres.: ").append(getNumberPresentation()).append(" dial: ").append(getOrigDialString()).append(" postdial: ").append(getRemainingPostDialString()).append(" cnap name: ").append(getCnapName()).append("(").append(getCnapNamePresentation()).append(")");
        }
        str.append(" incoming: ").append(isIncoming()).append(" state: ").append(getState()).append(" post dial state: ").append(getPostDialState());
        return str.toString();
    }

    public boolean isConfHostBeforeHandover() {
        return this.mPreMultipartyHostState;
    }

    public boolean isMultipartyBeforeHandover() {
        return this.mPreMultipartyState;
    }

    public boolean isIncomingCallMultiparty() {
        return false;
    }

    public void notifyConferenceParticipantsInvited(boolean isSuccess) {
        for (Listener l : this.mListeners) {
            l.onConferenceParticipantsInvited(isSuccess);
        }
    }

    public void notifyConferenceConnectionsConfigured(ArrayList<Connection> radioConnections) {
        for (Listener l : this.mListeners) {
            l.onConferenceConnectionsConfigured(radioConnections);
        }
    }

    public void notifyRemoteHeld(boolean isHeld) {
        Rlog.d(LOG_TAG, "Connection: notify remote hold");
        for (Listener l : this.mListeners) {
            l.onRemoteHeld(isHeld);
        }
    }

    public void setConnectionAddressDisplay() {
        for (Listener l : this.mListeners) {
            l.onAddressDisplayChanged();
        }
    }

    public boolean isVideo() {
        Rlog.d(LOG_TAG, "Connection: isVideo = false");
        return false;
    }

    public String getForwardingAddress() {
        return this.mForwardingAddress;
    }

    public void setForwardingAddress(String address) {
        this.mForwardingAddress = address;
    }

    public String getRedirectingAddress() {
        return this.mRedirectingAddress;
    }

    public void setRedirectingAddress(String address) {
        this.mRedirectingAddress = address;
    }

    public void setNumberPresentation(int num) {
        this.mNumberPresentation = num;
    }
}
