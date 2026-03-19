package android.telecom;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.Connection;
import android.util.ArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    private final Set<Listener> mListeners = new CopyOnWriteArraySet();
    private final List<Connection> mChildConnections = new CopyOnWriteArrayList();
    private final List<Connection> mUnmodifiableChildConnections = Collections.unmodifiableList(this.mChildConnections);
    private final List<Connection> mConferenceableConnections = new ArrayList();
    private final List<Connection> mUnmodifiableConferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
    private int mState = 1;
    private long mConnectTimeMillis = 0;
    private long mConnectionStartElapsedRealTime = 0;
    private final Object mExtrasLock = new Object();
    private final Connection.Listener mConnectionDeathListener = new Connection.Listener() {
        @Override
        public void onDestroyed(Connection connection) {
            if (Conference.this.mConferenceableConnections.remove(connection)) {
                Conference.this.fireOnConferenceableConnectionsChanged();
            }
        }
    };

    public static abstract class Listener {
        public void onStateChanged(Conference conference, int i, int i2) {
        }

        public void onDisconnected(Conference conference, DisconnectCause disconnectCause) {
        }

        public void onConnectionAdded(Conference conference, Connection connection) {
        }

        public void onConnectionRemoved(Conference conference, Connection connection) {
        }

        public void onConferenceableConnectionsChanged(Conference conference, List<Connection> list) {
        }

        public void onDestroyed(Conference conference) {
        }

        public void onConnectionCapabilitiesChanged(Conference conference, int i) {
        }

        public void onConnectionPropertiesChanged(Conference conference, int i) {
        }

        public void onVideoStateChanged(Conference conference, int i) {
        }

        public void onVideoProviderChanged(Conference conference, Connection.VideoProvider videoProvider) {
        }

        public void onStatusHintsChanged(Conference conference, StatusHints statusHints) {
        }

        public void onExtrasChanged(Conference conference, Bundle bundle) {
        }

        public void onExtrasRemoved(Conference conference, List<String> list) {
        }
    }

    public Conference(PhoneAccountHandle phoneAccountHandle) {
        this.mPhoneAccount = phoneAccountHandle;
    }

    public final String getTelecomCallId() {
        return this.mTelecomCallId;
    }

    public final void setTelecomCallId(String str) {
        this.mTelecomCallId = str;
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

    public static boolean can(int i, int i2) {
        return (i & i2) != 0;
    }

    public boolean can(int i) {
        return can(this.mConnectionCapabilities, i);
    }

    public void removeCapability(int i) {
        setConnectionCapabilities((~i) & this.mConnectionCapabilities);
    }

    public void addCapability(int i) {
        setConnectionCapabilities(i | this.mConnectionCapabilities);
    }

    @SystemApi
    @Deprecated
    public final AudioState getAudioState() {
        return new AudioState(this.mCallAudioState);
    }

    public final CallAudioState getCallAudioState() {
        return this.mCallAudioState;
    }

    public Connection.VideoProvider getVideoProvider() {
        return null;
    }

    public int getVideoState() {
        return 0;
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

    @SystemApi
    @Deprecated
    public void onAudioStateChanged(AudioState audioState) {
    }

    public void onCallAudioStateChanged(CallAudioState callAudioState) {
    }

    public void onConnectionAdded(Connection connection) {
    }

    public final void setOnHold() {
        setState(5);
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
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDisconnected(this, this.mDisconnectCause);
        }
    }

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public final void setConnectionCapabilities(int i) {
        if (i != this.mConnectionCapabilities) {
            this.mConnectionCapabilities = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
            }
        }
    }

    public final void setConnectionProperties(int i) {
        if (i != this.mConnectionProperties) {
            this.mConnectionProperties = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionPropertiesChanged(this, this.mConnectionProperties);
            }
        }
    }

    public final boolean addConnection(Connection connection) {
        Log.d(this, "Connection=%s, connection=", connection);
        if (connection == null || this.mChildConnections.contains(connection) || !connection.setConference(this)) {
            return false;
        }
        this.mChildConnections.add(connection);
        onConnectionAdded(connection);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionAdded(this, connection);
        }
        return true;
    }

    public final void removeConnection(Connection connection) {
        Log.d(this, "removing %s from %s", connection, this.mChildConnections);
        if (connection != null && this.mChildConnections.remove(connection)) {
            connection.resetConference();
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionRemoved(this, connection);
            }
        }
    }

    public final void setConferenceableConnections(List<Connection> list) {
        clearConferenceableList();
        for (Connection connection : list) {
            if (!this.mConferenceableConnections.contains(connection)) {
                connection.addConnectionListener(this.mConnectionDeathListener);
                this.mConferenceableConnections.add(connection);
            }
        }
        fireOnConferenceableConnectionsChanged();
    }

    public final void setVideoState(Connection connection, int i) {
        Log.d(this, "setVideoState Conference: %s Connection: %s VideoState: %s", this, connection, Integer.valueOf(i));
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(this, i);
        }
    }

    public final void setVideoProvider(Connection connection, Connection.VideoProvider videoProvider) {
        Log.d(this, "setVideoProvider Conference: %s Connection: %s VideoState: %s", this, connection, videoProvider);
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoProviderChanged(this, videoProvider);
        }
    }

    private final void fireOnConferenceableConnectionsChanged() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceableConnectionsChanged(this, getConferenceableConnections());
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
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDestroyed(this);
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

    @SystemApi
    public Connection getPrimaryConnection() {
        if (this.mUnmodifiableChildConnections == null || this.mUnmodifiableChildConnections.isEmpty()) {
            return null;
        }
        return this.mUnmodifiableChildConnections.get(0);
    }

    @SystemApi
    @Deprecated
    public final void setConnectTimeMillis(long j) {
        setConnectionTime(j);
    }

    public final void setConnectionTime(long j) {
        this.mConnectTimeMillis = j;
    }

    public final void setConnectionStartElapsedRealTime(long j) {
        this.mConnectionStartElapsedRealTime = j;
    }

    @SystemApi
    @Deprecated
    public final long getConnectTimeMillis() {
        return getConnectionTime();
    }

    public final long getConnectionTime() {
        return this.mConnectTimeMillis;
    }

    public final long getConnectionStartElapsedRealTime() {
        return this.mConnectionStartElapsedRealTime;
    }

    final void setCallAudioState(CallAudioState callAudioState) {
        Log.d(this, "setCallAudioState %s", callAudioState);
        this.mCallAudioState = callAudioState;
        onAudioStateChanged(getAudioState());
        onCallAudioStateChanged(callAudioState);
    }

    private void setState(int i) {
        if (i != 4 && i != 5 && i != 6) {
            Log.w(this, "Unsupported state transition for Conference call.", Connection.stateToString(i));
            return;
        }
        if (this.mState != i) {
            int i2 = this.mState;
            this.mState = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged(this, i2, i);
            }
        }
    }

    private final void clearConferenceableList() {
        Iterator<Connection> it = this.mConferenceableConnections.iterator();
        while (it.hasNext()) {
            it.next().removeConnectionListener(this.mConnectionDeathListener);
        }
        this.mConferenceableConnections.clear();
    }

    public String toString() {
        return String.format(Locale.US, "[State: %s,Capabilites: %s, VideoState: %s, VideoProvider: %s, ThisObject %s]", Connection.stateToString(this.mState), Call.Details.capabilitiesToString(this.mConnectionCapabilities), Integer.valueOf(getVideoState()), getVideoProvider(), super.toString());
    }

    public final void setStatusHints(StatusHints statusHints) {
        this.mStatusHints = statusHints;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onStatusHintsChanged(this, statusHints);
        }
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final void setExtras(Bundle bundle) {
        synchronized (this.mExtrasLock) {
            putExtras(bundle);
            if (this.mPreviousExtraKeys != null) {
                ArrayList arrayList = new ArrayList();
                for (String str : this.mPreviousExtraKeys) {
                    if (bundle == null || !bundle.containsKey(str)) {
                        arrayList.add(str);
                    }
                }
                if (!arrayList.isEmpty()) {
                    removeExtras(arrayList);
                }
            }
            if (this.mPreviousExtraKeys == null) {
                this.mPreviousExtraKeys = new ArraySet();
            }
            this.mPreviousExtraKeys.clear();
            if (bundle != null) {
                this.mPreviousExtraKeys.addAll(bundle.keySet());
            }
        }
    }

    public final void putExtras(Bundle bundle) {
        Bundle bundle2;
        if (bundle == null) {
            return;
        }
        synchronized (this.mExtrasLock) {
            if (this.mExtras == null) {
                this.mExtras = new Bundle();
            }
            this.mExtras.putAll(bundle);
            bundle2 = new Bundle(this.mExtras);
        }
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onExtrasChanged(this, new Bundle(bundle2));
        }
    }

    public final void putExtra(String str, boolean z) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(str, z);
        putExtras(bundle);
    }

    public final void putExtra(String str, int i) {
        Bundle bundle = new Bundle();
        bundle.putInt(str, i);
        putExtras(bundle);
    }

    public final void putExtra(String str, String str2) {
        Bundle bundle = new Bundle();
        bundle.putString(str, str2);
        putExtras(bundle);
    }

    public final void removeExtras(List<String> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        synchronized (this.mExtrasLock) {
            if (this.mExtras != null) {
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    this.mExtras.remove(it.next());
                }
            }
        }
        List<String> listUnmodifiableList = Collections.unmodifiableList(list);
        Iterator<Listener> it2 = this.mListeners.iterator();
        while (it2.hasNext()) {
            it2.next().onExtrasRemoved(this, listUnmodifiableList);
        }
    }

    public final void removeExtras(String... strArr) {
        removeExtras(Arrays.asList(strArr));
    }

    public final Bundle getExtras() {
        return this.mExtras;
    }

    public void onExtrasChanged(Bundle bundle) {
    }

    final void handleExtrasChanged(Bundle bundle) {
        Bundle bundle2;
        synchronized (this.mExtrasLock) {
            this.mExtras = bundle;
            if (this.mExtras != null) {
                bundle2 = new Bundle(this.mExtras);
            } else {
                bundle2 = null;
            }
        }
        onExtrasChanged(bundle2);
    }
}
