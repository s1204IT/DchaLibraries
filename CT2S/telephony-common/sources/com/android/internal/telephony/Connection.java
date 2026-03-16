package com.android.internal.telephony;

import android.net.Uri;
import android.os.SystemClock;
import android.telecom.ConferenceParticipant;
import android.telecom.Connection;
import android.telephony.Rlog;
import com.android.internal.telephony.Call;
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
    protected String mCnapName;
    protected long mConnectTime;
    protected long mConnectTimeReal;
    protected String mConvertedNumber;
    protected long mCreateTime;
    protected String mDialString;
    protected long mDuration;
    protected long mHoldingStartTime;
    protected boolean mIsIncoming;
    private boolean mLocalVideoCapable;
    protected Connection mOrigConnection;
    private boolean mRemoteVideoCapable;
    Object mUserData;
    private Connection.VideoProvider mVideoProvider;
    private int mVideoState;
    protected int mCnapNamePresentation = 1;
    protected int mNumberPresentation = 1;
    private List<PostDialListener> mPostDialListeners = new ArrayList();
    public Set<Listener> mListeners = new CopyOnWriteArraySet();
    protected boolean mNumberConverted = false;
    public Call.State mPreHandoverState = Call.State.IDLE;

    public interface Listener {
        void onAudioQualityChanged(int i);

        void onConferenceParticipantsChanged(List<ConferenceParticipant> list);

        void onLocalVideoCapabilityChanged(boolean z);

        void onRemoteVideoCapabilityChanged(boolean z);

        void onVideoProviderChanged(Connection.VideoProvider videoProvider);

        void onVideoStateChanged(int i);
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

    public abstract Call getCall();

    public abstract int getDisconnectCause();

    public abstract long getDisconnectTime();

    public abstract long getHoldDurationMillis();

    public abstract int getNumberPresentation();

    public abstract PostDialState getPostDialState();

    public abstract int getPreciseDisconnectCause();

    public abstract String getRemainingPostDialString();

    public abstract UUSInfo getUUSInfo();

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
        public void onLocalVideoCapabilityChanged(boolean capable) {
        }

        @Override
        public void onRemoteVideoCapabilityChanged(boolean capable) {
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

    public boolean isIncoming() {
        return this.mIsIncoming;
    }

    public Call.State getState() {
        Call c = getCall();
        return c == null ? Call.State.IDLE : c.getState();
    }

    public Call.State getStateBeforeHandover() {
        return this.mPreHandoverState;
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

    public void clearUserData() {
        this.mUserData = null;
    }

    public final void addPostDialListener(PostDialListener listener) {
        if (!this.mPostDialListeners.contains(listener)) {
            this.mPostDialListeners.add(listener);
        }
    }

    protected final void clearPostDialListeners() {
        this.mPostDialListeners.clear();
    }

    protected final void notifyPostDialListeners() {
        if (getPostDialState() == PostDialState.WAIT) {
            for (PostDialListener listener : new ArrayList(this.mPostDialListeners)) {
                listener.onPostDialWait();
            }
        }
    }

    protected final void notifyPostDialListenersNextChar(char c) {
        for (PostDialListener listener : new ArrayList(this.mPostDialListeners)) {
            listener.onPostDialChar(c);
        }
    }

    public Connection getOrigConnection() {
        return this.mOrigConnection;
    }

    public void migrateFrom(Connection c) {
        if (c != null) {
            this.mListeners = c.mListeners;
            this.mAddress = c.getAddress();
            this.mNumberPresentation = c.getNumberPresentation();
            this.mDialString = c.getOrigDialString();
            this.mCnapName = c.getCnapName();
            this.mCnapNamePresentation = c.getCnapNamePresentation();
            this.mIsIncoming = c.isIncoming();
            this.mCreateTime = c.getCreateTime();
            this.mConnectTime = c.getConnectTime();
            this.mConnectTimeReal = c.getConnectTimeReal();
            this.mHoldingStartTime = c.getHoldingStartTime();
            this.mOrigConnection = c.getOrigConnection();
        }
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

    public boolean isLocalVideoCapable() {
        return this.mLocalVideoCapable;
    }

    public boolean isRemoteVideoCapable() {
        return this.mRemoteVideoCapable;
    }

    public Connection.VideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getAudioQuality() {
        return this.mAudioQuality;
    }

    public void setVideoState(int videoState) {
        this.mVideoState = videoState;
        for (Listener l : this.mListeners) {
            l.onVideoStateChanged(this.mVideoState);
        }
    }

    public void setLocalVideoCapable(boolean capable) {
        this.mLocalVideoCapable = capable;
        for (Listener l : this.mListeners) {
            l.onLocalVideoCapabilityChanged(this.mLocalVideoCapable);
        }
    }

    public void setRemoteVideoCapable(boolean capable) {
        this.mRemoteVideoCapable = capable;
        for (Listener l : this.mListeners) {
            l.onRemoteVideoCapabilityChanged(this.mRemoteVideoCapable);
        }
    }

    public void setAudioQuality(int audioQuality) {
        this.mAudioQuality = audioQuality;
        for (Listener l : this.mListeners) {
            l.onAudioQualityChanged(this.mAudioQuality);
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

    public void onDisconnectConferenceParticipant(Uri endpoint) {
    }

    public String toString() {
        StringBuilder str = new StringBuilder(128);
        if (Rlog.isLoggable(LOG_TAG, 3)) {
            str.append("addr: " + getAddress()).append(" pres.: " + getNumberPresentation()).append(" dial: " + getOrigDialString()).append(" postdial: " + getRemainingPostDialString()).append(" cnap name: " + getCnapName()).append("(" + getCnapNamePresentation() + ")");
        }
        str.append(" incoming: " + isIncoming()).append(" state: " + getState()).append(" post dial state: " + getPostDialState());
        return str.toString();
    }
}
