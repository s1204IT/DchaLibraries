package android.telecom;

import android.os.Bundle;
import android.telecom.Call;
import android.telecom.Connection;
import android.util.ArraySet;
import com.mediatek.telecom.FormattedLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Conference extends Conferenceable {
    public static final long CONNECT_TIME_NOT_SPECIFIED = 0;
    private CallAudioState mCallAudioState;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private DisconnectCause mDisconnectCause;
    private String mDisconnectMessage;
    private Bundle mExtras;
    private PhoneAccountHandle mPhoneAccount;
    private Set<String> mPreviousExtraKeys;
    private StatusHints mStatusHints;
    private String mTelecomCallId;
    private Connection.VideoProvider mVideoProvider;
    private int mVideoState;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet();
    private final List<Connection> mChildConnections = new CopyOnWriteArrayList();
    private final List<Connection> mUnmodifiableChildConnections = Collections.unmodifiableList(this.mChildConnections);
    private final List<Connection> mConferenceableConnections = new ArrayList();
    private final List<Connection> mUnmodifiableConferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
    private int mState = 1;
    private long mConnectTimeMillis = 0;
    private final Object mExtrasLock = new Object();
    private final Connection.Listener mConnectionDeathListener = new Connection.Listener() {
        @Override
        public void onDestroyed(Connection c) {
            if (!Conference.this.mConferenceableConnections.remove(c)) {
                return;
            }
            Conference.this.fireOnConferenceableConnectionsChanged();
        }
    };

    public static abstract class Listener {
        public void onStateChanged(Conference conference, int oldState, int newState) {
        }

        public void onDisconnected(Conference conference, DisconnectCause disconnectCause) {
        }

        public void onConnectionAdded(Conference conference, Connection connection) {
        }

        public void onConnectionRemoved(Conference conference, Connection connection) {
        }

        public void onConferenceableConnectionsChanged(Conference conference, List<Connection> conferenceableConnections) {
        }

        public void onDestroyed(Conference conference) {
        }

        public void onConnectionCapabilitiesChanged(Conference conference, int connectionCapabilities) {
        }

        public void onConnectionPropertiesChanged(Conference conference, int connectionProperties) {
        }

        public void onVideoStateChanged(Conference c, int videoState) {
        }

        public void onVideoProviderChanged(Conference c, Connection.VideoProvider videoProvider) {
        }

        public void onStatusHintsChanged(Conference conference, StatusHints statusHints) {
        }

        public void onExtrasChanged(Conference c, Bundle extras) {
        }

        public void onExtrasRemoved(Conference c, List<String> keys) {
        }
    }

    public Conference(PhoneAccountHandle phoneAccount) {
        this.mPhoneAccount = phoneAccount;
    }

    public final String getTelecomCallId() {
        return this.mTelecomCallId;
    }

    public final void setTelecomCallId(String telecomCallId) {
        this.mTelecomCallId = telecomCallId;
    }

    public final PhoneAccountHandle getPhoneAccountHandle() {
        return this.mPhoneAccount;
    }

    public final List<Connection> getConnections() {
        return this.mUnmodifiableChildConnections;
    }

    public final int getState() {
        return this.mState;
    }

    public final int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public final int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    public static boolean can(int capabilities, int capability) {
        return (capabilities & capability) != 0;
    }

    public boolean can(int capability) {
        return can(this.mConnectionCapabilities, capability);
    }

    public void removeCapability(int capability) {
        int newCapabilities = this.mConnectionCapabilities;
        setConnectionCapabilities(newCapabilities & (~capability));
    }

    public void addCapability(int capability) {
        int newCapabilities = this.mConnectionCapabilities;
        setConnectionCapabilities(newCapabilities | capability);
    }

    @Deprecated
    public final AudioState getAudioState() {
        return new AudioState(this.mCallAudioState);
    }

    public final CallAudioState getCallAudioState() {
        return this.mCallAudioState;
    }

    public Connection.VideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public void onDisconnect() {
    }

    public void onSeparate(Connection connection) {
    }

    public void onMerge(Connection connection) {
    }

    public void onHold() {
    }

    public void onUnhold() {
    }

    public void onMerge() {
    }

    public void onSwap() {
    }

    public void onPlayDtmfTone(char c) {
    }

    public void onStopDtmfTone() {
    }

    @Deprecated
    public void onAudioStateChanged(AudioState state) {
    }

    public void onCallAudioStateChanged(CallAudioState state) {
    }

    public void onConnectionAdded(Connection connection) {
    }

    public final void setOnHold() {
        setState(5);
    }

    public void onHangupAll() {
    }

    public final void setDialing() {
        setState(3);
    }

    public final void setActive() {
        setState(4);
    }

    public final void setDisconnected(DisconnectCause disconnectCause) {
        this.mDisconnectCause = disconnectCause;
        setState(6);
        for (Listener l : this.mListeners) {
            l.onDisconnected(this, this.mDisconnectCause);
        }
    }

    protected int buildConnectionCapabilities() {
        return 0;
    }

    public final void updateConnectionCapabilities() {
        int newConnectionCapabilities = buildConnectionCapabilities();
        setConnectionCapabilities(newConnectionCapabilities);
    }

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public final void setConnectionCapabilities(int connectionCapabilities) {
        if (connectionCapabilities == this.mConnectionCapabilities) {
            return;
        }
        this.mConnectionCapabilities = connectionCapabilities;
        for (Listener l : this.mListeners) {
            l.onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
        }
    }

    public final void setConnectionProperties(int connectionProperties) {
        if (connectionProperties == this.mConnectionProperties) {
            return;
        }
        this.mConnectionProperties = connectionProperties;
        for (Listener l : this.mListeners) {
            l.onConnectionPropertiesChanged(this, this.mConnectionProperties);
        }
    }

    public final boolean addConnection(Connection connection) {
        Log.d(this, "Connection=%s, connection=", connection);
        if (connection == null || this.mChildConnections.contains(connection) || !connection.setConference(this)) {
            return false;
        }
        this.mChildConnections.add(connection);
        onConnectionAdded(connection);
        for (Listener l : this.mListeners) {
            l.onConnectionAdded(this, connection);
        }
        return true;
    }

    public final void removeConnection(Connection connection) {
        Log.d(this, "removing %s from %s", connection, this.mChildConnections);
        if (connection == null || !this.mChildConnections.remove(connection)) {
            return;
        }
        connection.resetConference();
        for (Listener l : this.mListeners) {
            l.onConnectionRemoved(this, connection);
        }
    }

    public final void setConferenceableConnections(List<Connection> conferenceableConnections) {
        clearConferenceableList();
        for (Connection c : conferenceableConnections) {
            if (!this.mConferenceableConnections.contains(c)) {
                c.addConnectionListener(this.mConnectionDeathListener);
                this.mConferenceableConnections.add(c);
            }
        }
        fireOnConferenceableConnectionsChanged();
    }

    public final void setVideoState(Connection c, int videoState) {
        Log.d(this, "setVideoState Conference: %s Connection: %s VideoState: %s", this, c, Integer.valueOf(videoState));
        this.mVideoState = videoState;
        for (Listener l : this.mListeners) {
            l.onVideoStateChanged(this, videoState);
        }
    }

    public final void setVideoProvider(Connection c, Connection.VideoProvider videoProvider) {
        Log.d(this, "setVideoProvider Conference: %s Connection: %s VideoState: %s", this, c, videoProvider);
        this.mVideoProvider = videoProvider;
        for (Listener l : this.mListeners) {
            l.onVideoProviderChanged(this, videoProvider);
        }
    }

    private final void fireOnConferenceableConnectionsChanged() {
        for (Listener l : this.mListeners) {
            l.onConferenceableConnectionsChanged(this, getConferenceableConnections());
        }
    }

    public final List<Connection> getConferenceableConnections() {
        return this.mUnmodifiableConferenceableConnections;
    }

    public final void destroy() {
        Log.d(this, "destroying conference : %s", this);
        List<Connection> disconnectedChild = new ArrayList<>(this.mChildConnections.size());
        List<Connection> activeChild = new ArrayList<>(this.mChildConnections.size());
        for (Connection connection : this.mChildConnections) {
            if (connection.getState() != 4) {
                disconnectedChild.add(connection);
            } else {
                activeChild.add(connection);
            }
        }
        for (Connection connection2 : disconnectedChild) {
            Log.d(this, "removing connection for disconnectedChild %s", connection2);
            removeConnection(connection2);
        }
        for (Connection connection3 : activeChild) {
            Log.d(this, "removing connection for activeChild %s", connection3);
            removeConnection(connection3);
        }
        if (this.mState != 6) {
            Log.d(this, "setting to disconnected", new Object[0]);
            setDisconnected(new DisconnectCause(2));
        }
        for (Listener l : this.mListeners) {
            l.onDestroyed(this);
        }
    }

    public final Conference addListener(Listener listener) {
        this.mListeners.add(listener);
        return this;
    }

    public final Conference removeListener(Listener listener) {
        this.mListeners.remove(listener);
        return this;
    }

    public Connection getPrimaryConnection() {
        if (this.mUnmodifiableChildConnections == null || this.mUnmodifiableChildConnections.isEmpty()) {
            return null;
        }
        return this.mUnmodifiableChildConnections.get(0);
    }

    @Deprecated
    public final void setConnectTimeMillis(long connectTimeMillis) {
        setConnectionTime(connectTimeMillis);
    }

    public final void setConnectionTime(long connectionTimeMillis) {
        this.mConnectTimeMillis = connectionTimeMillis;
    }

    @Deprecated
    public final long getConnectTimeMillis() {
        return getConnectionTime();
    }

    public final long getConnectionTime() {
        return this.mConnectTimeMillis;
    }

    final void setCallAudioState(CallAudioState state) {
        Log.d(this, "setCallAudioState %s", state);
        this.mCallAudioState = state;
        onAudioStateChanged(getAudioState());
        onCallAudioStateChanged(state);
    }

    private void setState(int newState) {
        FormattedLog formattedLog;
        if (newState != 4 && newState != 5 && newState != 6 && newState != 3) {
            Log.w(this, "Unsupported state transition for Conference call.", Connection.stateToString(newState));
            return;
        }
        if (this.mState == newState) {
            return;
        }
        int oldState = this.mState;
        this.mState = newState;
        FormattedLog.Builder builder = configDumpLogBuilder(new FormattedLog.Builder());
        if (builder != null && (formattedLog = builder.buildDumpInfo()) != null) {
            Log.d(this, formattedLog.toString(), new Object[0]);
        }
        for (Listener l : this.mListeners) {
            l.onStateChanged(this, oldState, newState);
        }
    }

    private final void clearConferenceableList() {
        for (Connection c : this.mConferenceableConnections) {
            c.removeConnectionListener(this.mConnectionDeathListener);
        }
        this.mConferenceableConnections.clear();
    }

    public String toString() {
        return String.format(Locale.US, "[State: %s,Capabilites: %s, VideoState: %s, VideoProvider: %s, ThisObject %s]", Connection.stateToString(this.mState), Call.Details.capabilitiesToString(this.mConnectionCapabilities), Integer.valueOf(getVideoState()), getVideoProvider(), super.toString());
    }

    public final void setStatusHints(StatusHints statusHints) {
        this.mStatusHints = statusHints;
        for (Listener l : this.mListeners) {
            l.onStatusHintsChanged(this, statusHints);
        }
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final void setExtras(Bundle extras) {
        synchronized (this.mExtrasLock) {
            putExtras(extras);
            if (this.mPreviousExtraKeys != null) {
                List<String> toRemove = new ArrayList<>();
                for (String oldKey : this.mPreviousExtraKeys) {
                    if (extras == null || !extras.containsKey(oldKey)) {
                        toRemove.add(oldKey);
                    }
                }
                if (!toRemove.isEmpty()) {
                    removeExtras(toRemove);
                }
            }
            if (this.mPreviousExtraKeys == null) {
                this.mPreviousExtraKeys = new ArraySet();
            }
            this.mPreviousExtraKeys.clear();
            if (extras != null) {
                this.mPreviousExtraKeys.addAll(extras.keySet());
            }
        }
    }

    public final void putExtras(Bundle extras) {
        Bundle listenersBundle;
        if (extras == null) {
            return;
        }
        synchronized (this.mExtrasLock) {
            if (this.mExtras == null) {
                this.mExtras = new Bundle();
            }
            this.mExtras.putAll(extras);
            listenersBundle = new Bundle(this.mExtras);
        }
        for (Listener l : this.mListeners) {
            l.onExtrasChanged(this, new Bundle(listenersBundle));
        }
    }

    public final void putExtra(String key, boolean value) {
        Bundle newExtras = new Bundle();
        newExtras.putBoolean(key, value);
        putExtras(newExtras);
    }

    public final void putExtra(String key, int value) {
        Bundle newExtras = new Bundle();
        newExtras.putInt(key, value);
        putExtras(newExtras);
    }

    public final void putExtra(String key, String value) {
        Bundle newExtras = new Bundle();
        newExtras.putString(key, value);
        putExtras(newExtras);
    }

    public final void removeExtras(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        synchronized (this.mExtrasLock) {
            if (this.mExtras != null) {
                for (String key : keys) {
                    this.mExtras.remove(key);
                }
            }
        }
        List<String> unmodifiableKeys = Collections.unmodifiableList(keys);
        for (Listener l : this.mListeners) {
            l.onExtrasRemoved(this, unmodifiableKeys);
        }
    }

    public final Bundle getExtras() {
        return this.mExtras;
    }

    public void onExtrasChanged(Bundle extras) {
    }

    final void handleExtrasChanged(Bundle extras) {
        Bundle bundle = null;
        synchronized (this.mExtrasLock) {
            this.mExtras = extras;
            if (this.mExtras != null) {
                bundle = new Bundle(this.mExtras);
            }
        }
        onExtrasChanged(bundle);
    }

    public void onInviteConferenceParticipants(List<String> numbers) {
    }

    protected final void setRinging() {
        setState(2);
    }

    protected FormattedLog.Builder configDumpLogBuilder(FormattedLog.Builder builder) {
        if (builder == null) {
            return null;
        }
        builder.setCategory("CC");
        builder.setOpType(FormattedLog.OpType.DUMP);
        builder.setCallNumber("conferenceCall");
        builder.setCallId(Integer.toString(System.identityHashCode(this)));
        builder.setStatusInfo("isConfCall", "Yes");
        builder.setStatusInfo("state", Connection.callStateToFormattedDumpString(this.mState));
        builder.setStatusInfo("isConfChildCall", "No");
        builder.setStatusInfo("capabilities", Connection.capabilitiesToString(getConnectionCapabilities()));
        return builder;
    }

    public void onDisconnect(String pendingCallAction) {
        onDisconnect();
    }
}
