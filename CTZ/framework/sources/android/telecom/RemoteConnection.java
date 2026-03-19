package android.telecom;

import android.annotation.SystemApi;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.view.Surface;
import com.android.internal.telecom.IConnectionService;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteConnection {
    private Uri mAddress;
    private int mAddressPresentation;
    private final Set<CallbackRecord> mCallbackRecords;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private RemoteConference mConference;
    private final List<RemoteConnection> mConferenceableConnections;
    private boolean mConnected;
    private int mConnectionCapabilities;
    private final String mConnectionId;
    private int mConnectionProperties;
    private IConnectionService mConnectionService;
    private DisconnectCause mDisconnectCause;
    private Bundle mExtras;
    private boolean mIsVoipAudioMode;
    private boolean mRingbackRequested;
    private int mState;
    private StatusHints mStatusHints;
    private final List<RemoteConnection> mUnmodifiableconferenceableConnections;
    private VideoProvider mVideoProvider;
    private int mVideoState;

    public static abstract class Callback {
        public void onStateChanged(RemoteConnection remoteConnection, int i) {
        }

        public void onDisconnected(RemoteConnection remoteConnection, DisconnectCause disconnectCause) {
        }

        public void onRingbackRequested(RemoteConnection remoteConnection, boolean z) {
        }

        public void onConnectionCapabilitiesChanged(RemoteConnection remoteConnection, int i) {
        }

        public void onConnectionPropertiesChanged(RemoteConnection remoteConnection, int i) {
        }

        public void onPostDialWait(RemoteConnection remoteConnection, String str) {
        }

        public void onPostDialChar(RemoteConnection remoteConnection, char c) {
        }

        public void onVoipAudioChanged(RemoteConnection remoteConnection, boolean z) {
        }

        public void onStatusHintsChanged(RemoteConnection remoteConnection, StatusHints statusHints) {
        }

        public void onAddressChanged(RemoteConnection remoteConnection, Uri uri, int i) {
        }

        public void onCallerDisplayNameChanged(RemoteConnection remoteConnection, String str, int i) {
        }

        public void onVideoStateChanged(RemoteConnection remoteConnection, int i) {
        }

        public void onDestroyed(RemoteConnection remoteConnection) {
        }

        public void onConferenceableConnectionsChanged(RemoteConnection remoteConnection, List<RemoteConnection> list) {
        }

        public void onVideoProviderChanged(RemoteConnection remoteConnection, VideoProvider videoProvider) {
        }

        public void onConferenceChanged(RemoteConnection remoteConnection, RemoteConference remoteConference) {
        }

        public void onExtrasChanged(RemoteConnection remoteConnection, Bundle bundle) {
        }

        public void onConnectionEvent(RemoteConnection remoteConnection, String str, Bundle bundle) {
        }

        public void onRttInitiationSuccess(RemoteConnection remoteConnection) {
        }

        public void onRttInitiationFailure(RemoteConnection remoteConnection, int i) {
        }

        public void onRttSessionRemotelyTerminated(RemoteConnection remoteConnection) {
        }

        public void onRemoteRttRequest(RemoteConnection remoteConnection) {
        }
    }

    public static class VideoProvider {
        private final String mCallingPackage;
        private final int mTargetSdkVersion;
        private final IVideoProvider mVideoProviderBinder;
        private final IVideoCallback mVideoCallbackDelegate = new IVideoCallback() {
            @Override
            public void receiveSessionModifyRequest(VideoProfile videoProfile) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onSessionModifyRequestReceived(VideoProvider.this, videoProfile);
                }
            }

            @Override
            public void receiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onSessionModifyResponseReceived(VideoProvider.this, i, videoProfile, videoProfile2);
                }
            }

            @Override
            public void handleCallSessionEvent(int i) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onCallSessionEvent(VideoProvider.this, i);
                }
            }

            @Override
            public void changePeerDimensions(int i, int i2) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onPeerDimensionsChanged(VideoProvider.this, i, i2);
                }
            }

            @Override
            public void changeCallDataUsage(long j) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onCallDataUsageChanged(VideoProvider.this, j);
                }
            }

            @Override
            public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onCameraCapabilitiesChanged(VideoProvider.this, cameraCapabilities);
                }
            }

            @Override
            public void changeVideoQuality(int i) {
                Iterator it = VideoProvider.this.mCallbacks.iterator();
                while (it.hasNext()) {
                    ((Callback) it.next()).onVideoQualityChanged(VideoProvider.this, i);
                }
            }

            @Override
            public IBinder asBinder() {
                return null;
            }
        };
        private final VideoCallbackServant mVideoCallbackServant = new VideoCallbackServant(this.mVideoCallbackDelegate);
        private final Set<Callback> mCallbacks = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));

        public static abstract class Callback {
            public void onSessionModifyRequestReceived(VideoProvider videoProvider, VideoProfile videoProfile) {
            }

            public void onSessionModifyResponseReceived(VideoProvider videoProvider, int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
            }

            public void onCallSessionEvent(VideoProvider videoProvider, int i) {
            }

            public void onPeerDimensionsChanged(VideoProvider videoProvider, int i, int i2) {
            }

            public void onCallDataUsageChanged(VideoProvider videoProvider, long j) {
            }

            public void onCameraCapabilitiesChanged(VideoProvider videoProvider, VideoProfile.CameraCapabilities cameraCapabilities) {
            }

            public void onVideoQualityChanged(VideoProvider videoProvider, int i) {
            }
        }

        VideoProvider(IVideoProvider iVideoProvider, String str, int i) {
            this.mVideoProviderBinder = iVideoProvider;
            this.mCallingPackage = str;
            this.mTargetSdkVersion = i;
            try {
                this.mVideoProviderBinder.addVideoCallback(this.mVideoCallbackServant.getStub().asBinder());
            } catch (RemoteException e) {
            }
        }

        public void registerCallback(Callback callback) {
            this.mCallbacks.add(callback);
        }

        public void unregisterCallback(Callback callback) {
            this.mCallbacks.remove(callback);
        }

        public void setCamera(String str) {
            try {
                this.mVideoProviderBinder.setCamera(str, this.mCallingPackage, this.mTargetSdkVersion);
            } catch (RemoteException e) {
            }
        }

        public void setPreviewSurface(Surface surface) {
            try {
                this.mVideoProviderBinder.setPreviewSurface(surface);
            } catch (RemoteException e) {
            }
        }

        public void setDisplaySurface(Surface surface) {
            try {
                this.mVideoProviderBinder.setDisplaySurface(surface);
            } catch (RemoteException e) {
            }
        }

        public void setDeviceOrientation(int i) {
            try {
                this.mVideoProviderBinder.setDeviceOrientation(i);
            } catch (RemoteException e) {
            }
        }

        public void setZoom(float f) {
            try {
                this.mVideoProviderBinder.setZoom(f);
            } catch (RemoteException e) {
            }
        }

        public void sendSessionModifyRequest(VideoProfile videoProfile, VideoProfile videoProfile2) {
            try {
                this.mVideoProviderBinder.sendSessionModifyRequest(videoProfile, videoProfile2);
            } catch (RemoteException e) {
            }
        }

        public void sendSessionModifyResponse(VideoProfile videoProfile) {
            try {
                this.mVideoProviderBinder.sendSessionModifyResponse(videoProfile);
            } catch (RemoteException e) {
            }
        }

        public void requestCameraCapabilities() {
            try {
                this.mVideoProviderBinder.requestCameraCapabilities();
            } catch (RemoteException e) {
            }
        }

        public void requestCallDataUsage() {
            try {
                this.mVideoProviderBinder.requestCallDataUsage();
            } catch (RemoteException e) {
            }
        }

        public void setPauseImage(Uri uri) {
            try {
                this.mVideoProviderBinder.setPauseImage(uri);
            } catch (RemoteException e) {
            }
        }
    }

    RemoteConnection(String str, IConnectionService iConnectionService, ConnectionRequest connectionRequest) {
        this.mCallbackRecords = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mConferenceableConnections = new ArrayList();
        this.mUnmodifiableconferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
        this.mState = 1;
        this.mConnectionId = str;
        this.mConnectionService = iConnectionService;
        this.mConnected = true;
        this.mState = 0;
    }

    RemoteConnection(String str, IConnectionService iConnectionService, ParcelableConnection parcelableConnection, String str2, int i) {
        this.mCallbackRecords = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mConferenceableConnections = new ArrayList();
        this.mUnmodifiableconferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
        this.mState = 1;
        this.mConnectionId = str;
        this.mConnectionService = iConnectionService;
        this.mConnected = true;
        this.mState = parcelableConnection.getState();
        this.mDisconnectCause = parcelableConnection.getDisconnectCause();
        this.mRingbackRequested = parcelableConnection.isRingbackRequested();
        this.mConnectionCapabilities = parcelableConnection.getConnectionCapabilities();
        this.mConnectionProperties = parcelableConnection.getConnectionProperties();
        this.mVideoState = parcelableConnection.getVideoState();
        IVideoProvider videoProvider = parcelableConnection.getVideoProvider();
        if (videoProvider != null) {
            this.mVideoProvider = new VideoProvider(videoProvider, str2, i);
        } else {
            this.mVideoProvider = null;
        }
        this.mIsVoipAudioMode = parcelableConnection.getIsVoipAudioMode();
        this.mStatusHints = parcelableConnection.getStatusHints();
        this.mAddress = parcelableConnection.getHandle();
        this.mAddressPresentation = parcelableConnection.getHandlePresentation();
        this.mCallerDisplayName = parcelableConnection.getCallerDisplayName();
        this.mCallerDisplayNamePresentation = parcelableConnection.getCallerDisplayNamePresentation();
        this.mConference = null;
        putExtras(parcelableConnection.getExtras());
        Bundle bundle = new Bundle();
        bundle.putString(Connection.EXTRA_ORIGINAL_CONNECTION_ID, str);
        putExtras(bundle);
    }

    RemoteConnection(DisconnectCause disconnectCause) {
        this.mCallbackRecords = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mConferenceableConnections = new ArrayList();
        this.mUnmodifiableconferenceableConnections = Collections.unmodifiableList(this.mConferenceableConnections);
        this.mState = 1;
        this.mConnectionId = WifiEnterpriseConfig.EMPTY_VALUE;
        this.mConnected = false;
        this.mState = 6;
        this.mDisconnectCause = disconnectCause;
    }

    public void registerCallback(Callback callback) {
        registerCallback(callback, new Handler());
    }

    public void registerCallback(Callback callback, Handler handler) {
        unregisterCallback(callback);
        if (callback != null && handler != null) {
            this.mCallbackRecords.add(new CallbackRecord(callback, handler));
        }
    }

    public void unregisterCallback(Callback callback) {
        if (callback != null) {
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
                if (callbackRecord.getCallback() == callback) {
                    this.mCallbackRecords.remove(callbackRecord);
                    return;
                }
            }
        }
    }

    public int getState() {
        return this.mState;
    }

    public DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    public boolean isVoipAudioMode() {
        return this.mIsVoipAudioMode;
    }

    public StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public Uri getAddress() {
        return this.mAddress;
    }

    public int getAddressPresentation() {
        return this.mAddressPresentation;
    }

    public CharSequence getCallerDisplayName() {
        return this.mCallerDisplayName;
    }

    public int getCallerDisplayNamePresentation() {
        return this.mCallerDisplayNamePresentation;
    }

    public int getVideoState() {
        return this.mVideoState;
    }

    public final VideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public final Bundle getExtras() {
        return this.mExtras;
    }

    public boolean isRingbackRequested() {
        return this.mRingbackRequested;
    }

    public void abort() {
        try {
            if (this.mConnected) {
                this.mConnectionService.abort(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void answer() {
        try {
            if (this.mConnected) {
                this.mConnectionService.answer(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void answer(int i) {
        try {
            if (this.mConnected) {
                this.mConnectionService.answerVideo(this.mConnectionId, i, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void reject() {
        try {
            if (this.mConnected) {
                this.mConnectionService.reject(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void hold() {
        try {
            if (this.mConnected) {
                this.mConnectionService.hold(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void unhold() {
        try {
            if (this.mConnected) {
                this.mConnectionService.unhold(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void disconnect() {
        try {
            if (this.mConnected) {
                this.mConnectionService.disconnect(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void playDtmfTone(char c) {
        try {
            if (this.mConnected) {
                this.mConnectionService.playDtmfTone(this.mConnectionId, c, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void stopDtmfTone() {
        try {
            if (this.mConnected) {
                this.mConnectionService.stopDtmfTone(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void postDialContinue(boolean z) {
        try {
            if (this.mConnected) {
                this.mConnectionService.onPostDialContinue(this.mConnectionId, z, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void pullExternalCall() {
        try {
            if (this.mConnected) {
                this.mConnectionService.pullExternalCall(this.mConnectionId, null);
            }
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
            if (this.mConnected) {
                this.mConnectionService.onCallAudioStateChanged(this.mConnectionId, callAudioState, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void startRtt(Connection.RttTextStream rttTextStream) {
        try {
            if (this.mConnected) {
                this.mConnectionService.startRtt(this.mConnectionId, rttTextStream.getFdFromInCall(), rttTextStream.getFdToInCall(), null);
            }
        } catch (RemoteException e) {
        }
    }

    public void stopRtt() {
        try {
            if (this.mConnected) {
                this.mConnectionService.stopRtt(this.mConnectionId, null);
            }
        } catch (RemoteException e) {
        }
    }

    public void sendRttUpgradeResponse(Connection.RttTextStream rttTextStream) {
        try {
            if (this.mConnected) {
                if (rttTextStream == null) {
                    this.mConnectionService.respondToRttUpgradeRequest(this.mConnectionId, null, null, null);
                } else {
                    this.mConnectionService.respondToRttUpgradeRequest(this.mConnectionId, rttTextStream.getFdFromInCall(), rttTextStream.getFdToInCall(), null);
                }
            }
        } catch (RemoteException e) {
        }
    }

    public List<RemoteConnection> getConferenceableConnections() {
        return this.mUnmodifiableconferenceableConnections;
    }

    public RemoteConference getConference() {
        return this.mConference;
    }

    String getId() {
        return this.mConnectionId;
    }

    IConnectionService getConnectionService() {
        return this.mConnectionService;
    }

    void setState(final int i) {
        if (this.mState != i) {
            this.mState = i;
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onStateChanged(this, i);
                    }
                });
            }
        }
    }

    void setDisconnected(final DisconnectCause disconnectCause) {
        if (this.mState != 6) {
            this.mState = 6;
            this.mDisconnectCause = disconnectCause;
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
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

    void setRingbackRequested(final boolean z) {
        if (this.mRingbackRequested != z) {
            this.mRingbackRequested = z;
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onRingbackRequested(this, z);
                    }
                });
            }
        }
    }

    void setConnectionCapabilities(final int i) {
        this.mConnectionCapabilities = i;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionCapabilitiesChanged(this, i);
                }
            });
        }
    }

    void setConnectionProperties(final int i) {
        this.mConnectionProperties = i;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionPropertiesChanged(this, i);
                }
            });
        }
    }

    void setDestroyed() {
        if (!this.mCallbackRecords.isEmpty()) {
            if (this.mState != 6) {
                setDisconnected(new DisconnectCause(1, "Connection destroyed."));
            }
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onDestroyed(this);
                    }
                });
            }
            this.mCallbackRecords.clear();
            this.mConnected = false;
        }
    }

    void setPostDialWait(final String str) {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onPostDialWait(this, str);
                }
            });
        }
    }

    void onPostDialChar(final char c) {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onPostDialChar(this, c);
                }
            });
        }
    }

    void setVideoState(final int i) {
        this.mVideoState = i;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onVideoStateChanged(this, i);
                }
            });
        }
    }

    void setVideoProvider(final VideoProvider videoProvider) {
        this.mVideoProvider = videoProvider;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onVideoProviderChanged(this, videoProvider);
                }
            });
        }
    }

    void setIsVoipAudioMode(final boolean z) {
        this.mIsVoipAudioMode = z;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onVoipAudioChanged(this, z);
                }
            });
        }
    }

    void setStatusHints(final StatusHints statusHints) {
        this.mStatusHints = statusHints;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onStatusHintsChanged(this, statusHints);
                }
            });
        }
    }

    void setAddress(final Uri uri, final int i) {
        this.mAddress = uri;
        this.mAddressPresentation = i;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onAddressChanged(this, uri, i);
                }
            });
        }
    }

    void setCallerDisplayName(final String str, final int i) {
        this.mCallerDisplayName = str;
        this.mCallerDisplayNamePresentation = i;
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onCallerDisplayNameChanged(this, str, i);
                }
            });
        }
    }

    void setConferenceableConnections(List<RemoteConnection> list) {
        this.mConferenceableConnections.clear();
        this.mConferenceableConnections.addAll(list);
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConferenceableConnectionsChanged(this, RemoteConnection.this.mUnmodifiableconferenceableConnections);
                }
            });
        }
    }

    void setConference(final RemoteConference remoteConference) {
        if (this.mConference != remoteConference) {
            this.mConference = remoteConference;
            for (CallbackRecord callbackRecord : this.mCallbackRecords) {
                final Callback callback = callbackRecord.getCallback();
                callbackRecord.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onConferenceChanged(this, remoteConference);
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
        try {
            this.mExtras.putAll(bundle);
        } catch (BadParcelableException e) {
            Log.w(this, "putExtras: could not unmarshal extras; exception = " + e, new Object[0]);
        }
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
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onExtrasChanged(this, RemoteConnection.this.mExtras);
                }
            });
        }
    }

    void onConnectionEvent(final String str, final Bundle bundle) {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    callback.onConnectionEvent(this, str, bundle);
                }
            });
        }
    }

    void onRttInitiationSuccess() {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttInitiationSuccess(this);
                }
            });
        }
    }

    void onRttInitiationFailure(final int i) {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttInitiationFailure(this, i);
                }
            });
        }
    }

    void onRttSessionRemotelyTerminated() {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRttSessionRemotelyTerminated(this);
                }
            });
        }
    }

    void onRemoteRttRequest() {
        for (CallbackRecord callbackRecord : this.mCallbackRecords) {
            final Callback callback = callbackRecord.getCallback();
            callbackRecord.getHandler().post(new Runnable() {
                @Override
                public final void run() {
                    callback.onRemoteRttRequest(this);
                }
            });
        }
    }

    public static RemoteConnection failure(DisconnectCause disconnectCause) {
        return new RemoteConnection(disconnectCause);
    }

    private static final class CallbackRecord extends Callback {
        private final Callback mCallback;
        private final Handler mHandler;

        public CallbackRecord(Callback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public Callback getCallback() {
            return this.mCallback;
        }

        public Handler getHandler() {
            return this.mHandler;
        }
    }
}
