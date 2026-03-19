package android.telecom;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import com.android.internal.telecom.IConnectionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

public final class RemoteConference {
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private final IConnectionService mConnectionService;
    private DisconnectCause mDisconnectCause;
    private Bundle mExtras;
    private final String mId;
    private final Set<CallbackRecord<Callback>> mCallbackRecords = new CopyOnWriteArraySet();
    private final List<RemoteConnection> mChildConnections = new CopyOnWriteArrayList();
    private final List<RemoteConnection> mUnmodifiableChildConnections = Collections.unmodifiableList(this.mChildConnections);
    private final List<RemoteConnection> mConferenceableConnections = new ArrayList();
    private final List<RemoteConnection> mUnmodifiableConferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
    private int mState = 1;

    public static abstract class Callback {
        public void onStateChanged(RemoteConference remoteConference, int i, int i2) {
        }

        public void onDisconnected(RemoteConference remoteConference, DisconnectCause disconnectCause) {
        }

        public void onConnectionAdded(RemoteConference remoteConference, RemoteConnection remoteConnection) {
        }

        public void onConnectionRemoved(RemoteConference remoteConference, RemoteConnection remoteConnection) {
        }

        public void onConnectionCapabilitiesChanged(RemoteConference remoteConference, int i) {
        }

        public void onConnectionPropertiesChanged(RemoteConference remoteConference, int i) {
        }

        public void onConferenceableConnectionsChanged(RemoteConference remoteConference, List<RemoteConnection> list) {
        }

        public void onDestroyed(RemoteConference remoteConference) {
        }

        public void onExtrasChanged(RemoteConference remoteConference, Bundle bundle) {
        }
    }

    RemoteConference(String str, IConnectionService iConnectionService) {
        this.mId = str;
        this.mConnectionService = iConnectionService;
    }

    String getId() {
        return this.mId;
    }

    void setDestroyed() {
        Iterator<RemoteConnection> it = this.mChildConnections.iterator();
        while (it.hasNext()) {
            it.next().setConference(null);
        }
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onDestroyed(this);
                }
            });
        }
    }

    void setState(final int i) {
        if (i != 4 && i != 5 && i != 6) {
            Log.w(this, "Unsupported state transition for Conference call.", Connection.stateToString(i));
            return;
        }
        if (this.mState != i) {
            final int i2 = this.mState;
            this.mState = i;
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onStateChanged(this, i2, i);
                    }
                });
            }
        }
    }

    void addConnection(final RemoteConnection remoteConnection) {
        if (!this.mChildConnections.contains(remoteConnection)) {
            this.mChildConnections.add(remoteConnection);
            remoteConnection.setConference(this);
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectionAdded(this, remoteConnection);
                    }
                });
            }
        }
    }

    void removeConnection(final RemoteConnection remoteConnection) {
        if (this.mChildConnections.contains(remoteConnection)) {
            this.mChildConnections.remove(remoteConnection);
            remoteConnection.setConference(null);
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectionRemoved(this, remoteConnection);
                    }
                });
            }
        }
    }

    void setConnectionCapabilities(int i) {
        if (this.mConnectionCapabilities != i) {
            this.mConnectionCapabilities = i;
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectionCapabilitiesChanged(this, RemoteConference.this.mConnectionCapabilities);
                    }
                });
            }
        }
    }

    void setConnectionProperties(int i) {
        if (this.mConnectionProperties != i) {
            this.mConnectionProperties = i;
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConnectionPropertiesChanged(this, RemoteConference.this.mConnectionProperties);
                    }
                });
            }
        }
    }

    void setConferenceableConnections(List<RemoteConnection> list) {
        this.mConferenceableConnections.clear();
        this.mConferenceableConnections.addAll(list);
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConferenceableConnectionsChanged(this, RemoteConference.this.mUnmodifiableConferenceableConnections);
                }
            });
        }
    }

    void setDisconnected(final DisconnectCause disconnectCause) {
        if (this.mState != 6) {
            this.mDisconnectCause = disconnectCause;
            setState(6);
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDisconnected(this, disconnectCause);
                    }
                });
            }
        }
    }

    void putExtras(Bundle bundle) {
        if (bundle == null) {
            return;
        }
        if (this.mExtras == null) {
            this.mExtras = new Bundle();
        }
        this.mExtras.putAll(bundle);
        notifyExtrasChanged();
    }

    void removeExtras(List<String> list) {
        if (this.mExtras == null || list == null || list.isEmpty()) {
            return;
        }
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            this.mExtras.remove(it.next());
        }
        notifyExtrasChanged();
    }

    private void notifyExtrasChanged() {
        for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onExtrasChanged(this, RemoteConference.this.mExtras);
                }
            });
        }
    }

    public final List<RemoteConnection> getConnections() {
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

    public final Bundle getExtras() {
        return this.mExtras;
    }

    public void disconnect() {
        try {
            this.mConnectionService.disconnect(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    public void separate(RemoteConnection remoteConnection) {
        if (this.mChildConnections.contains(remoteConnection)) {
            try {
                this.mConnectionService.splitFromConference(remoteConnection.getId(), null);
            } catch (RemoteException e) {
            }
        }
    }

    public void merge() {
        try {
            this.mConnectionService.mergeConference(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    public void swap() {
        try {
            this.mConnectionService.swapConference(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    public void hold() {
        try {
            this.mConnectionService.hold(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    public void unhold() {
        try {
            this.mConnectionService.unhold(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    public DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public void playDtmfTone(char c) {
        try {
            this.mConnectionService.playDtmfTone(this.mId, c, null);
        } catch (RemoteException e) {
        }
    }

    public void stopDtmfTone() {
        try {
            this.mConnectionService.stopDtmfTone(this.mId, null);
        } catch (RemoteException e) {
        }
    }

    @SystemApi
    @Deprecated
    public void setAudioState(AudioState audioState) {
        setCallAudioState(new CallAudioState(audioState));
    }

    public void setCallAudioState(CallAudioState callAudioState) {
        try {
            this.mConnectionService.onCallAudioStateChanged(this.mId, callAudioState, null);
        } catch (RemoteException e) {
        }
    }

    public List<RemoteConnection> getConferenceableConnections() {
        return this.mUnmodifiableConferenceableConnections;
    }

    public final void registerCallback(Callback callback) {
        registerCallback(callback, new Handler());
    }

    public final void registerCallback(Callback callback, Handler handler) {
        unregisterCallback(callback);
        if (callback != null && handler != null) {
            this.mCallbackRecords.add(new CallbackRecord<>(callback, handler));
        }
    }

    public final void unregisterCallback(Callback callback) {
        if (callback != null) {
            for (CallbackRecord<Callback> callbackRecord : this.mCallbackRecords) {
                if (callbackRecord.getCallback() == callback) {
                    this.mCallbackRecords.remove(callbackRecord);
                    return;
                }
            }
        }
    }
}
