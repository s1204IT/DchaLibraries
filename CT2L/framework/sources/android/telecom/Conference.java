package android.telecom;

import android.telecom.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Conference implements IConferenceable {
    public static long CONNECT_TIME_NOT_SPECIFIED = 0;
    private AudioState mAudioState;
    private int mConnectionCapabilities;
    private DisconnectCause mDisconnectCause;
    private String mDisconnectMessage;
    protected PhoneAccountHandle mPhoneAccount;
    private final Set<Listener> mListeners = new CopyOnWriteArraySet();
    private final List<Connection> mChildConnections = new CopyOnWriteArrayList();
    private final List<Connection> mUnmodifiableChildConnections = Collections.unmodifiableList(this.mChildConnections);
    private final List<Connection> mConferenceableConnections = new ArrayList();
    private final List<Connection> mUnmodifiableConferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
    private int mState = 1;
    private long mConnectTimeMillis = CONNECT_TIME_NOT_SPECIFIED;
    private final Connection.Listener mConnectionDeathListener = new Connection.Listener() {
        @Override
        public void onDestroyed(Connection c) {
            if (Conference.this.mConferenceableConnections.remove(c)) {
                Conference.this.fireOnConferenceableConnectionsChanged();
            }
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
    }

    public Conference(PhoneAccountHandle phoneAccount) {
        this.mPhoneAccount = phoneAccount;
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

    @Deprecated
    public final int getCapabilities() {
        return getConnectionCapabilities();
    }

    public final int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public static boolean can(int capabilities, int capability) {
        return (capabilities & capability) != 0;
    }

    public boolean can(int capability) {
        return can(this.mConnectionCapabilities, capability);
    }

    public void removeCapability(int capability) {
        this.mConnectionCapabilities &= capability ^ (-1);
    }

    public void addCapability(int capability) {
        this.mConnectionCapabilities |= capability;
    }

    public final AudioState getAudioState() {
        return this.mAudioState;
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

    public void onAudioStateChanged(AudioState state) {
    }

    public void onConnectionAdded(Connection connection) {
    }

    public final void setOnHold() {
        setState(5);
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

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    @Deprecated
    public final void setCapabilities(int connectionCapabilities) {
        setConnectionCapabilities(connectionCapabilities);
    }

    public final void setConnectionCapabilities(int connectionCapabilities) {
        if (connectionCapabilities != this.mConnectionCapabilities) {
            this.mConnectionCapabilities = connectionCapabilities;
            for (Listener l : this.mListeners) {
                l.onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
            }
        }
    }

    public final boolean addConnection(Connection connection) {
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
        if (connection != null && this.mChildConnections.remove(connection)) {
            connection.resetConference();
            for (Listener l : this.mListeners) {
                l.onConnectionRemoved(this, connection);
            }
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
        for (Connection connection : this.mChildConnections) {
            Log.d(this, "removing connection %s", connection);
            removeConnection(connection);
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

    public void setConnectTimeMillis(long connectTimeMillis) {
        this.mConnectTimeMillis = connectTimeMillis;
    }

    public long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    final void setAudioState(AudioState state) {
        Log.d(this, "setAudioState %s", state);
        this.mAudioState = state;
        onAudioStateChanged(state);
    }

    private void setState(int newState) {
        if (newState != 4 && newState != 5 && newState != 6) {
            Log.w(this, "Unsupported state transition for Conference call.", Connection.stateToString(newState));
            return;
        }
        if (this.mState != newState) {
            int oldState = this.mState;
            this.mState = newState;
            for (Listener l : this.mListeners) {
                l.onStateChanged(this, oldState, newState);
            }
        }
    }

    private final void clearConferenceableList() {
        for (Connection c : this.mConferenceableConnections) {
            c.removeConnectionListener(this.mConnectionDeathListener);
        }
        this.mConferenceableConnections.clear();
    }
}
