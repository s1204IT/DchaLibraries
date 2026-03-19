package com.android.internal.telephony;

import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Connection {
    public static final int AUDIO_QUALITY_HIGH_DEFINITION = 2;
    public static final int AUDIO_QUALITY_STANDARD = 1;
    protected String mAddress;
    private boolean mAllowAddCallDuringVideoCall;
    private boolean mAnsweringDisconnectsActiveCall;
    private boolean mAudioModeIsVoip;
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
    protected long mHoldingStartTime;
    protected boolean mIsIncoming;
    private boolean mIsWifi;
    protected int mNextPostDialChar;
    protected Connection mOrigConnection;
    private int mPhoneType;
    protected String mPostDialString;
    private int mPulledDialogId;
    private String mTelecomCallId;
    Object mUserData;
    private Connection.VideoProvider mVideoProvider;
    private int mVideoState;
    private static final String TAG = "Connection";
    private static String LOG_TAG = TAG;
    protected int mCnapNamePresentation = 1;
    protected int mNumberPresentation = 1;
    private List<PostDialListener> mPostDialListeners = new ArrayList();
    public Set<Listener> mListeners = new CopyOnWriteArraySet();
    protected boolean mNumberConverted = false;
    public int mCause = 0;
    protected PostDialState mPostDialState = PostDialState.NOT_STARTED;
    public Call.State mPreHandoverState = Call.State.IDLE;
    private boolean mIsPulledCall = false;

    public static class Capability {
        public static final int IS_EXTERNAL_CONNECTION = 16;
        public static final int IS_PULLABLE = 32;
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_LOCAL = 1;
        public static final int SUPPORTS_DOWNGRADE_TO_VOICE_REMOTE = 2;
        public static final int SUPPORTS_VT_LOCAL_BIDIRECTIONAL = 4;
        public static final int SUPPORTS_VT_REMOTE_BIDIRECTIONAL = 8;
    }

    public interface Listener {
        void onAudioQualityChanged(int i);

        void onCallPullFailed(Connection connection);

        void onCallSubstateChanged(int i);

        void onConferenceMergedFailed();

        void onConferenceParticipantsChanged(List<ConferenceParticipant> list);

        void onConnectionCapabilitiesChanged(int i);

        void onConnectionEvent(String str, Bundle bundle);

        void onDisconnect(int i);

        void onExitedEcmMode();

        void onExtrasChanged(Bundle bundle);

        void onHandoverToWifiFailed();

        void onMultipartyStateChanged(boolean z);

        void onRttInitiated();

        void onRttModifyRequestReceived();

        void onRttModifyResponseReceived(int i);

        void onRttTerminated();

        void onVideoProviderChanged(Connection.VideoProvider videoProvider);

        void onVideoStateChanged(int i);

        void onWifiChanged(boolean z);
    }

    public interface PostDialListener {
        void onPostDialChar(char c);

        void onPostDialWait();
    }

    public enum PostDialState {
        NOT_STARTED,
        STARTED,
        WAIT,
        WILD,
        COMPLETE,
        CANCELLED,
        PAUSE
    }

    public abstract void cancelPostDial();

    public abstract void deflect(String str) throws CallStateException;

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
        public void onVideoStateChanged(int i) {
        }

        @Override
        public void onConnectionCapabilitiesChanged(int i) {
        }

        @Override
        public void onWifiChanged(boolean z) {
        }

        @Override
        public void onVideoProviderChanged(Connection.VideoProvider videoProvider) {
        }

        @Override
        public void onAudioQualityChanged(int i) {
        }

        @Override
        public void onConferenceParticipantsChanged(List<ConferenceParticipant> list) {
        }

        @Override
        public void onCallSubstateChanged(int i) {
        }

        @Override
        public void onMultipartyStateChanged(boolean z) {
        }

        @Override
        public void onConferenceMergedFailed() {
        }

        @Override
        public void onExtrasChanged(Bundle bundle) {
        }

        @Override
        public void onExitedEcmMode() {
        }

        @Override
        public void onCallPullFailed(Connection connection) {
        }

        @Override
        public void onHandoverToWifiFailed() {
        }

        @Override
        public void onConnectionEvent(String str, Bundle bundle) {
        }

        @Override
        public void onRttModifyRequestReceived() {
        }

        @Override
        public void onRttModifyResponseReceived(int i) {
        }

        @Override
        public void onDisconnect(int i) {
        }

        @Override
        public void onRttInitiated() {
        }

        @Override
        public void onRttTerminated() {
        }
    }

    protected Connection(int i) {
        this.mPhoneType = i;
    }

    public String getTelecomCallId() {
        return this.mTelecomCallId;
    }

    public void setTelecomCallId(String str) {
        this.mTelecomCallId = str;
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

    public void setConnectTime(long j) {
        this.mConnectTime = j;
    }

    public void setConnectTimeReal(long j) {
        this.mConnectTimeReal = j;
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

    public void setIsIncoming(boolean z) {
        this.mIsIncoming = z;
    }

    public Call.State getState() {
        Call call = getCall();
        if (call == null) {
            return Call.State.IDLE;
        }
        return call.getState();
    }

    public Call.State getStateBeforeHandover() {
        return this.mPreHandoverState;
    }

    public List<ConferenceParticipant> getConferenceParticipants() {
        Call call = getCall();
        if (call == null) {
            return null;
        }
        return call.getConferenceParticipants();
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

    public void setUserData(Object obj) {
        this.mUserData = obj;
    }

    public void clearUserData() {
        this.mUserData = null;
    }

    public void addPostDialListener(PostDialListener postDialListener) {
        if (!this.mPostDialListeners.contains(postDialListener)) {
            this.mPostDialListeners.add(postDialListener);
        }
    }

    public final void removePostDialListener(PostDialListener postDialListener) {
        this.mPostDialListeners.remove(postDialListener);
    }

    protected final void clearPostDialListeners() {
        if (this.mPostDialListeners != null) {
            this.mPostDialListeners.clear();
        }
    }

    protected final void notifyPostDialListeners() {
        if (getPostDialState() == PostDialState.WAIT) {
            Iterator it = new ArrayList(this.mPostDialListeners).iterator();
            while (it.hasNext()) {
                ((PostDialListener) it.next()).onPostDialWait();
            }
        }
    }

    protected final void notifyPostDialListenersNextChar(char c) {
        Iterator it = new ArrayList(this.mPostDialListeners).iterator();
        while (it.hasNext()) {
            ((PostDialListener) it.next()).onPostDialChar(c);
        }
    }

    public PostDialState getPostDialState() {
        return this.mPostDialState;
    }

    public String getRemainingPostDialString() {
        if (this.mPostDialState == PostDialState.CANCELLED || this.mPostDialState == PostDialState.COMPLETE || this.mPostDialString == null || this.mPostDialString.length() <= this.mNextPostDialChar) {
            return "";
        }
        return this.mPostDialString.substring(this.mNextPostDialChar);
    }

    public boolean onDisconnect(int i) {
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

    public void migrateFrom(Connection connection) {
        if (connection == null) {
            return;
        }
        this.mListeners = connection.mListeners;
        this.mDialString = connection.getOrigDialString();
        this.mCreateTime = connection.getCreateTime();
        this.mConnectTime = connection.getConnectTime();
        this.mConnectTimeReal = connection.getConnectTimeReal();
        this.mHoldingStartTime = connection.getHoldingStartTime();
        this.mOrigConnection = connection.getOrigConnection();
        this.mPostDialString = connection.mPostDialString;
        this.mNextPostDialChar = connection.mNextPostDialChar;
        this.mPostDialState = connection.mPostDialState;
    }

    public void addListener(Listener listener) {
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

    public boolean hasCapabilities(int i) {
        return (this.mConnectionCapabilities & i) == i;
    }

    public static int addCapability(int i, int i2) {
        return i | i2;
    }

    public static int removeCapability(int i, int i2) {
        return i & (~i2);
    }

    public boolean isWifi() {
        return this.mIsWifi;
    }

    public boolean getAudioModeIsVoip() {
        return this.mAudioModeIsVoip;
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

    public void setVideoState(int i) {
        this.mVideoState = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(this.mVideoState);
        }
    }

    public void setConnectionCapabilities(int i) {
        if (this.mConnectionCapabilities != i) {
            this.mConnectionCapabilities = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionCapabilitiesChanged(this.mConnectionCapabilities);
            }
        }
    }

    public void setWifi(boolean z) {
        this.mIsWifi = z;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onWifiChanged(this.mIsWifi);
        }
    }

    public void setAudioModeIsVoip(boolean z) {
        this.mAudioModeIsVoip = z;
    }

    public void setAudioQuality(int i) {
        this.mAudioQuality = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAudioQualityChanged(this.mAudioQuality);
        }
    }

    public void setConnectionExtras(Bundle bundle) {
        if (bundle != null) {
            this.mExtras = new Bundle(bundle);
            int size = this.mExtras.size();
            this.mExtras = this.mExtras.filterValues();
            int size2 = this.mExtras.size();
            if (size2 != size) {
                Rlog.i(TAG, "setConnectionExtras: filtering " + (size - size2) + " invalid extras.");
            }
        } else {
            this.mExtras = null;
        }
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onExtrasChanged(this.mExtras);
        }
    }

    public Bundle getConnectionExtras() {
        if (this.mExtras == null) {
            return null;
        }
        return new Bundle(this.mExtras);
    }

    public boolean isActiveCallDisconnectedOnAnswer() {
        return this.mAnsweringDisconnectsActiveCall;
    }

    public void setActiveCallDisconnectedOnAnswer(boolean z) {
        this.mAnsweringDisconnectsActiveCall = z;
    }

    public boolean shouldAllowAddCallDuringVideoCall() {
        return this.mAllowAddCallDuringVideoCall;
    }

    public void setAllowAddCallDuringVideoCall(boolean z) {
        this.mAllowAddCallDuringVideoCall = z;
    }

    public void setIsPulledCall(boolean z) {
        this.mIsPulledCall = z;
    }

    public boolean isPulledCall() {
        return this.mIsPulledCall;
    }

    public void setPulledDialogId(int i) {
        this.mPulledDialogId = i;
    }

    public int getPulledDialogId() {
        return this.mPulledDialogId;
    }

    public void setCallSubstate(int i) {
        this.mCallSubstate = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallSubstateChanged(this.mCallSubstate);
        }
    }

    public void setVideoProvider(Connection.VideoProvider videoProvider) {
        this.mVideoProvider = videoProvider;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoProviderChanged(this.mVideoProvider);
        }
    }

    public void setConverted(String str) {
        this.mNumberConverted = true;
        this.mConvertedNumber = this.mAddress;
        this.mAddress = str;
        this.mDialString = str;
    }

    public void updateConferenceParticipants(List<ConferenceParticipant> list) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceParticipantsChanged(list);
        }
    }

    public void updateMultipartyState(boolean z) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onMultipartyStateChanged(z);
        }
    }

    public void onConferenceMergeFailed() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceMergedFailed();
        }
    }

    public void onExitedEcmMode() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onExitedEcmMode();
        }
    }

    public void onCallPullFailed(Connection connection) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallPullFailed(connection);
        }
    }

    public void onHandoverToWifiFailed() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onHandoverToWifiFailed();
        }
    }

    public void onConnectionEvent(String str, Bundle bundle) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionEvent(str, bundle);
        }
    }

    public void onDisconnectConferenceParticipant(Uri uri) {
    }

    public void pullExternalCall() {
    }

    public void onRttModifyRequestReceived() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRttModifyRequestReceived();
        }
    }

    public void onRttModifyResponseReceived(int i) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRttModifyResponseReceived(i);
        }
    }

    public void onRttInitiated() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRttInitiated();
        }
    }

    public void onRttTerminated() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onRttTerminated();
        }
    }

    protected void notifyDisconnect(int i) {
        Rlog.i(TAG, "notifyDisconnect: callId=" + getTelecomCallId() + ", reason=" + i);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDisconnect(i);
        }
    }

    public int getPhoneType() {
        return this.mPhoneType;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(" callId: " + getTelecomCallId());
        StringBuilder sb2 = new StringBuilder();
        sb2.append(" isExternal: ");
        sb2.append((this.mConnectionCapabilities & 16) == 16 ? "Y" : "N");
        sb.append(sb2.toString());
        if (Rlog.isLoggable(LOG_TAG, 3)) {
            sb.append("addr: " + getAddress());
            sb.append(" pres.: " + getNumberPresentation());
            sb.append(" dial: " + getOrigDialString());
            sb.append(" postdial: " + getRemainingPostDialString());
            sb.append(" cnap name: " + getCnapName());
            sb.append("(" + getCnapNamePresentation() + ")");
        }
        sb.append(" incoming: " + isIncoming());
        sb.append(" state: " + getState());
        sb.append(" post dial state: " + getPostDialState());
        return sb.toString();
    }
}
