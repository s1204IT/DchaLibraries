package android.telecom;

import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telecom.Conference;
import android.view.Surface;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.IccCardConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Connection implements IConferenceable {
    public static final int CAPABILITY_DISCONNECT_FROM_CONFERENCE = 8192;
    public static final int CAPABILITY_GENERIC_CONFERENCE = 16384;
    public static final int CAPABILITY_HIGH_DEF_AUDIO = 1024;
    public static final int CAPABILITY_HOLD = 1;
    public static final int CAPABILITY_MANAGE_CONFERENCE = 128;
    public static final int CAPABILITY_MERGE_CONFERENCE = 4;
    public static final int CAPABILITY_MUTE = 64;
    public static final int CAPABILITY_RESPOND_VIA_TEXT = 32;
    public static final int CAPABILITY_SEPARATE_FROM_CONFERENCE = 4096;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL = 256;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE = 512;
    public static final int CAPABILITY_SUPPORT_HOLD = 2;
    public static final int CAPABILITY_SWAP_CONFERENCE = 8;
    public static final int CAPABILITY_UNUSED = 16;
    public static final int CAPABILITY_VoWIFI = 2048;
    private static final boolean PII_DEBUG = Log.isLoggable(3);
    public static final int STATE_ACTIVE = 4;
    public static final int STATE_DIALING = 3;
    public static final int STATE_DISCONNECTED = 6;
    public static final int STATE_HOLDING = 5;
    public static final int STATE_INITIALIZING = 0;
    public static final int STATE_NEW = 1;
    public static final int STATE_RINGING = 2;
    private Uri mAddress;
    private int mAddressPresentation;
    private boolean mAudioModeIsVoip;
    private AudioState mAudioState;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private Conference mConference;
    private int mConnectionCapabilities;
    private ConnectionService mConnectionService;
    private DisconnectCause mDisconnectCause;
    private StatusHints mStatusHints;
    private VideoProvider mVideoProvider;
    private int mVideoState;
    private final Listener mConnectionDeathListener = new Listener() {
        @Override
        public void onDestroyed(Connection c) {
            if (Connection.this.mConferenceables.remove(c)) {
                Connection.this.fireOnConferenceableConnectionsChanged();
            }
        }
    };
    private final Conference.Listener mConferenceDeathListener = new Conference.Listener() {
        @Override
        public void onDestroyed(Conference c) {
            if (Connection.this.mConferenceables.remove(c)) {
                Connection.this.fireOnConferenceableConnectionsChanged();
            }
        }
    };
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private final List<IConferenceable> mConferenceables = new ArrayList();
    private final List<IConferenceable> mUnmodifiableConferenceables = Collections.unmodifiableList(this.mConferenceables);
    private int mState = 1;
    private boolean mRingbackRequested = false;

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

    public static String capabilitiesToString(int capabilities) {
        StringBuilder builder = new StringBuilder();
        builder.append("[Capabilities:");
        if (can(capabilities, 1)) {
            builder.append(" CAPABILITY_HOLD");
        }
        if (can(capabilities, 2)) {
            builder.append(" CAPABILITY_SUPPORT_HOLD");
        }
        if (can(capabilities, 4)) {
            builder.append(" CAPABILITY_MERGE_CONFERENCE");
        }
        if (can(capabilities, 8)) {
            builder.append(" CAPABILITY_SWAP_CONFERENCE");
        }
        if (can(capabilities, 32)) {
            builder.append(" CAPABILITY_RESPOND_VIA_TEXT");
        }
        if (can(capabilities, 64)) {
            builder.append(" CAPABILITY_MUTE");
        }
        if (can(capabilities, 128)) {
            builder.append(" CAPABILITY_MANAGE_CONFERENCE");
        }
        if (can(capabilities, 256)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_LOCAL");
        }
        if (can(capabilities, 512)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_REMOTE");
        }
        if (can(capabilities, 1024)) {
            builder.append(" CAPABILITY_HIGH_DEF_AUDIO");
        }
        if (can(capabilities, 2048)) {
            builder.append(" CAPABILITY_VoWIFI");
        }
        if (can(capabilities, 16384)) {
            builder.append(" CAPABILITY_GENERIC_CONFERENCE");
        }
        builder.append("]");
        return builder.toString();
    }

    public static abstract class Listener {
        public void onStateChanged(Connection c, int state) {
        }

        public void onAddressChanged(Connection c, Uri newAddress, int presentation) {
        }

        public void onCallerDisplayNameChanged(Connection c, String callerDisplayName, int presentation) {
        }

        public void onVideoStateChanged(Connection c, int videoState) {
        }

        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
        }

        public void onPostDialWait(Connection c, String remaining) {
        }

        public void onPostDialChar(Connection c, char nextChar) {
        }

        public void onRingbackRequested(Connection c, boolean ringback) {
        }

        public void onDestroyed(Connection c) {
        }

        public void onConnectionCapabilitiesChanged(Connection c, int capabilities) {
        }

        public void onVideoProviderChanged(Connection c, VideoProvider videoProvider) {
        }

        public void onAudioModeIsVoipChanged(Connection c, boolean isVoip) {
        }

        public void onStatusHintsChanged(Connection c, StatusHints statusHints) {
        }

        public void onConferenceablesChanged(Connection c, List<IConferenceable> conferenceables) {
        }

        public void onConferenceChanged(Connection c, Conference conference) {
        }

        public void onConferenceParticipantsChanged(Connection c, List<ConferenceParticipant> participants) {
        }

        public void onConferenceStarted() {
        }
    }

    public static abstract class VideoProvider {
        private static final int MSG_REQUEST_CAMERA_CAPABILITIES = 9;
        private static final int MSG_REQUEST_CONNECTION_DATA_USAGE = 10;
        private static final int MSG_SEND_SESSION_MODIFY_REQUEST = 7;
        private static final int MSG_SEND_SESSION_MODIFY_RESPONSE = 8;
        private static final int MSG_SET_CAMERA = 2;
        private static final int MSG_SET_DEVICE_ORIENTATION = 5;
        private static final int MSG_SET_DISPLAY_SURFACE = 4;
        private static final int MSG_SET_PAUSE_IMAGE = 11;
        private static final int MSG_SET_PREVIEW_SURFACE = 3;
        private static final int MSG_SET_VIDEO_CALLBACK = 1;
        private static final int MSG_SET_ZOOM = 6;
        public static final int SESSION_EVENT_CAMERA_FAILURE = 5;
        public static final int SESSION_EVENT_CAMERA_READY = 6;
        public static final int SESSION_EVENT_RX_PAUSE = 1;
        public static final int SESSION_EVENT_RX_RESUME = 2;
        public static final int SESSION_EVENT_TX_START = 3;
        public static final int SESSION_EVENT_TX_STOP = 4;
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        private final VideoProviderBinder mBinder;
        private final VideoProviderHandler mMessageHandler;
        private IVideoCallback mVideoCallback;

        public abstract void onRequestCameraCapabilities();

        public abstract void onRequestConnectionDataUsage();

        public abstract void onSendSessionModifyRequest(VideoProfile videoProfile);

        public abstract void onSendSessionModifyResponse(VideoProfile videoProfile);

        public abstract void onSetCamera(String str);

        public abstract void onSetDeviceOrientation(int i);

        public abstract void onSetDisplaySurface(Surface surface);

        public abstract void onSetPauseImage(String str);

        public abstract void onSetPreviewSurface(Surface surface);

        public abstract void onSetZoom(float f);

        private final class VideoProviderHandler extends Handler {
            private VideoProviderHandler() {
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        VideoProvider.this.mVideoCallback = IVideoCallback.Stub.asInterface((IBinder) msg.obj);
                        break;
                    case 2:
                        VideoProvider.this.onSetCamera((String) msg.obj);
                        break;
                    case 3:
                        VideoProvider.this.onSetPreviewSurface((Surface) msg.obj);
                        break;
                    case 4:
                        VideoProvider.this.onSetDisplaySurface((Surface) msg.obj);
                        break;
                    case 5:
                        VideoProvider.this.onSetDeviceOrientation(msg.arg1);
                        break;
                    case 6:
                        VideoProvider.this.onSetZoom(((Float) msg.obj).floatValue());
                        break;
                    case 7:
                        VideoProvider.this.onSendSessionModifyRequest((VideoProfile) msg.obj);
                        break;
                    case 8:
                        VideoProvider.this.onSendSessionModifyResponse((VideoProfile) msg.obj);
                        break;
                    case 9:
                        VideoProvider.this.onRequestCameraCapabilities();
                        break;
                    case 10:
                        VideoProvider.this.onRequestConnectionDataUsage();
                        break;
                    case 11:
                        VideoProvider.this.onSetPauseImage((String) msg.obj);
                        break;
                }
            }
        }

        private final class VideoProviderBinder extends IVideoProvider.Stub {
            private VideoProviderBinder() {
            }

            @Override
            public void setVideoCallback(IBinder videoCallbackBinder) {
                VideoProvider.this.mMessageHandler.obtainMessage(1, videoCallbackBinder).sendToTarget();
            }

            @Override
            public void setCamera(String cameraId) {
                VideoProvider.this.mMessageHandler.obtainMessage(2, cameraId).sendToTarget();
            }

            @Override
            public void setPreviewSurface(Surface surface) {
                VideoProvider.this.mMessageHandler.obtainMessage(3, surface).sendToTarget();
            }

            @Override
            public void setDisplaySurface(Surface surface) {
                VideoProvider.this.mMessageHandler.obtainMessage(4, surface).sendToTarget();
            }

            @Override
            public void setDeviceOrientation(int rotation) {
                VideoProvider.this.mMessageHandler.obtainMessage(5, Integer.valueOf(rotation)).sendToTarget();
            }

            @Override
            public void setZoom(float value) {
                VideoProvider.this.mMessageHandler.obtainMessage(6, Float.valueOf(value)).sendToTarget();
            }

            @Override
            public void sendSessionModifyRequest(VideoProfile requestProfile) {
                VideoProvider.this.mMessageHandler.obtainMessage(7, requestProfile).sendToTarget();
            }

            @Override
            public void sendSessionModifyResponse(VideoProfile responseProfile) {
                VideoProvider.this.mMessageHandler.obtainMessage(8, responseProfile).sendToTarget();
            }

            @Override
            public void requestCameraCapabilities() {
                VideoProvider.this.mMessageHandler.obtainMessage(9).sendToTarget();
            }

            @Override
            public void requestCallDataUsage() {
                VideoProvider.this.mMessageHandler.obtainMessage(10).sendToTarget();
            }

            @Override
            public void setPauseImage(String uri) {
                VideoProvider.this.mMessageHandler.obtainMessage(11, uri).sendToTarget();
            }
        }

        public VideoProvider() {
            this.mMessageHandler = new VideoProviderHandler();
            this.mBinder = new VideoProviderBinder();
        }

        public final IVideoProvider getInterface() {
            return this.mBinder;
        }

        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.receiveSessionModifyRequest(videoProfile);
                } catch (RemoteException e) {
                }
            }
        }

        public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.receiveSessionModifyResponse(status, requestedProfile, responseProfile);
                } catch (RemoteException e) {
                }
            }
        }

        public void handleCallSessionEvent(int event) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.handleCallSessionEvent(event);
                } catch (RemoteException e) {
                }
            }
        }

        public void changePeerDimensions(int width, int height) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.changePeerDimensions(width, height);
                } catch (RemoteException e) {
                }
            }
        }

        public void changeCallDataUsage(int dataUsage) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.changeCallDataUsage(dataUsage);
                } catch (RemoteException e) {
                }
            }
        }

        public void changeCameraCapabilities(CameraCapabilities cameraCapabilities) {
            if (this.mVideoCallback != null) {
                try {
                    this.mVideoCallback.changeCameraCapabilities(cameraCapabilities);
                } catch (RemoteException e) {
                }
            }
        }
    }

    public final Uri getAddress() {
        return this.mAddress;
    }

    public final int getAddressPresentation() {
        return this.mAddressPresentation;
    }

    public final String getCallerDisplayName() {
        return this.mCallerDisplayName;
    }

    public final int getCallerDisplayNamePresentation() {
        return this.mCallerDisplayNamePresentation;
    }

    public final int getState() {
        return this.mState;
    }

    public final int getVideoState() {
        return this.mVideoState;
    }

    public final AudioState getAudioState() {
        return this.mAudioState;
    }

    public final Conference getConference() {
        return this.mConference;
    }

    public final boolean isRingbackRequested() {
        return this.mRingbackRequested;
    }

    public final boolean getAudioModeIsVoip() {
        return this.mAudioModeIsVoip;
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final Connection addConnectionListener(Listener l) {
        this.mListeners.add(l);
        return this;
    }

    public final Connection removeConnectionListener(Listener l) {
        if (l != null) {
            this.mListeners.remove(l);
        }
        return this;
    }

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    final void setAudioState(AudioState state) {
        checkImmutable();
        Log.d(this, "setAudioState %s", state);
        this.mAudioState = state;
        onAudioStateChanged(state);
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "STATE_INITIALIZING";
            case 1:
                return "STATE_NEW";
            case 2:
                return "STATE_RINGING";
            case 3:
                return "STATE_DIALING";
            case 4:
                return "STATE_ACTIVE";
            case 5:
                return "STATE_HOLDING";
            case 6:
                return "DISCONNECTED";
            default:
                Log.wtf(Connection.class, "Unknown state %d", Integer.valueOf(state));
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public final int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    @Deprecated
    public final int getCallCapabilities() {
        return getConnectionCapabilities();
    }

    public final void setAddress(Uri address, int presentation) {
        checkImmutable();
        Log.d(this, "setAddress %s", address);
        this.mAddress = address;
        this.mAddressPresentation = presentation;
        for (Listener l : this.mListeners) {
            l.onAddressChanged(this, address, presentation);
        }
    }

    public final void setCallerDisplayName(String callerDisplayName, int presentation) {
        checkImmutable();
        Log.d(this, "setCallerDisplayName %s", callerDisplayName);
        this.mCallerDisplayName = callerDisplayName;
        this.mCallerDisplayNamePresentation = presentation;
        for (Listener l : this.mListeners) {
            l.onCallerDisplayNameChanged(this, callerDisplayName, presentation);
        }
    }

    public final void setVideoState(int videoState) {
        checkImmutable();
        Log.d(this, "setVideoState %d", Integer.valueOf(videoState));
        this.mVideoState = videoState;
        for (Listener l : this.mListeners) {
            l.onVideoStateChanged(this, this.mVideoState);
        }
    }

    public final void setActive() {
        checkImmutable();
        setRingbackRequested(false);
        setState(4);
    }

    public final void setRinging() {
        checkImmutable();
        setState(2);
    }

    public final void setInitializing() {
        checkImmutable();
        setState(0);
    }

    public final void setInitialized() {
        checkImmutable();
        setState(1);
    }

    public final void setDialing() {
        checkImmutable();
        setState(3);
    }

    public final void setOnHold() {
        checkImmutable();
        setState(5);
    }

    public final void setVideoProvider(VideoProvider videoProvider) {
        checkImmutable();
        this.mVideoProvider = videoProvider;
        for (Listener l : this.mListeners) {
            l.onVideoProviderChanged(this, videoProvider);
        }
    }

    public final VideoProvider getVideoProvider() {
        return this.mVideoProvider;
    }

    public final void setDisconnected(DisconnectCause disconnectCause) {
        checkImmutable();
        this.mDisconnectCause = disconnectCause;
        setState(6);
        Log.d(this, "Disconnected with cause %s", disconnectCause);
        for (Listener l : this.mListeners) {
            l.onDisconnected(this, disconnectCause);
        }
    }

    public final void setPostDialWait(String remaining) {
        checkImmutable();
        for (Listener l : this.mListeners) {
            l.onPostDialWait(this, remaining);
        }
    }

    public final void setNextPostDialWaitChar(char nextChar) {
        checkImmutable();
        for (Listener l : this.mListeners) {
            l.onPostDialChar(this, nextChar);
        }
    }

    public final void setRingbackRequested(boolean ringback) {
        checkImmutable();
        if (this.mRingbackRequested != ringback) {
            this.mRingbackRequested = ringback;
            for (Listener l : this.mListeners) {
                l.onRingbackRequested(this, ringback);
            }
        }
    }

    @Deprecated
    public final void setCallCapabilities(int connectionCapabilities) {
        setConnectionCapabilities(connectionCapabilities);
    }

    public final void setConnectionCapabilities(int connectionCapabilities) {
        checkImmutable();
        if (this.mConnectionCapabilities != connectionCapabilities) {
            this.mConnectionCapabilities = connectionCapabilities;
            for (Listener l : this.mListeners) {
                l.onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
            }
        }
    }

    public final void destroy() {
        for (Listener l : this.mListeners) {
            l.onDestroyed(this);
        }
    }

    public final void setAudioModeIsVoip(boolean isVoip) {
        checkImmutable();
        this.mAudioModeIsVoip = isVoip;
        for (Listener l : this.mListeners) {
            l.onAudioModeIsVoipChanged(this, isVoip);
        }
    }

    public final void setStatusHints(StatusHints statusHints) {
        checkImmutable();
        this.mStatusHints = statusHints;
        for (Listener l : this.mListeners) {
            l.onStatusHintsChanged(this, statusHints);
        }
    }

    public final void setConferenceableConnections(List<Connection> conferenceableConnections) {
        checkImmutable();
        clearConferenceableList();
        for (Connection c : conferenceableConnections) {
            if (!this.mConferenceables.contains(c)) {
                c.addConnectionListener(this.mConnectionDeathListener);
                this.mConferenceables.add(c);
            }
        }
        fireOnConferenceableConnectionsChanged();
    }

    public final void setConferenceables(List<IConferenceable> conferenceables) {
        clearConferenceableList();
        for (IConferenceable c : conferenceables) {
            if (!this.mConferenceables.contains(c)) {
                if (c instanceof Connection) {
                    Connection connection = (Connection) c;
                    connection.addConnectionListener(this.mConnectionDeathListener);
                } else if (c instanceof Conference) {
                    Conference conference = (Conference) c;
                    conference.addListener(this.mConferenceDeathListener);
                }
                this.mConferenceables.add(c);
            }
        }
        fireOnConferenceableConnectionsChanged();
    }

    public final List<IConferenceable> getConferenceables() {
        return this.mUnmodifiableConferenceables;
    }

    public final void setConnectionService(ConnectionService connectionService) {
        checkImmutable();
        if (this.mConnectionService != null) {
            Log.e(this, new Exception(), "Trying to set ConnectionService on a connection which is already associated with another ConnectionService.", new Object[0]);
        } else {
            this.mConnectionService = connectionService;
        }
    }

    public final void unsetConnectionService(ConnectionService connectionService) {
        if (this.mConnectionService != connectionService) {
            Log.e(this, new Exception(), "Trying to remove ConnectionService from a Connection that does not belong to the ConnectionService.", new Object[0]);
        } else {
            this.mConnectionService = null;
        }
    }

    public final ConnectionService getConnectionService() {
        return this.mConnectionService;
    }

    public final boolean setConference(Conference conference) {
        checkImmutable();
        if (this.mConference != null) {
            return false;
        }
        this.mConference = conference;
        if (this.mConnectionService != null && this.mConnectionService.containsConference(conference)) {
            fireConferenceChanged();
        }
        return true;
    }

    public final void resetConference() {
        if (this.mConference != null) {
            Log.d(this, "Conference reset", new Object[0]);
            this.mConference = null;
            fireConferenceChanged();
        }
    }

    public void onAudioStateChanged(AudioState state) {
    }

    public void onStateChanged(int state) {
    }

    public void onPlayDtmfTone(char c) {
    }

    public void onStopDtmfTone() {
    }

    public void onDisconnect() {
    }

    public void onDisconnectConferenceParticipant(Uri endpoint) {
    }

    public void onSeparate() {
    }

    public void onAbort() {
    }

    public void onHold() {
    }

    public void onUnhold() {
    }

    public void onAnswer(int videoState) {
    }

    public void onAnswer() {
        onAnswer(0);
    }

    public void onReject() {
    }

    public void onPostDialContinue(boolean proceed) {
    }

    static String toLogSafePhoneNumber(String number) {
        if (number == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (!PII_DEBUG) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < number.length(); i++) {
                char c = number.charAt(i);
                if (c == '-' || c == '@' || c == '.') {
                    builder.append(c);
                } else {
                    builder.append('x');
                }
            }
            return builder.toString();
        }
        return number;
    }

    private void setState(int state) {
        checkImmutable();
        if (this.mState == 6 && this.mState != state) {
            Log.d(this, "Connection already DISCONNECTED; cannot transition out of this state.", new Object[0]);
            return;
        }
        if (this.mState != state) {
            Log.d(this, "setState: %s", stateToString(state));
            this.mState = state;
            onStateChanged(state);
            for (Listener l : this.mListeners) {
                l.onStateChanged(this, state);
            }
        }
    }

    private static class FailureSignalingConnection extends Connection {
        private boolean mImmutable;

        public FailureSignalingConnection(DisconnectCause disconnectCause) {
            this.mImmutable = false;
            setDisconnected(disconnectCause);
            this.mImmutable = true;
        }

        @Override
        public void checkImmutable() {
            if (this.mImmutable) {
                throw new UnsupportedOperationException("Connection is immutable");
            }
        }
    }

    public static Connection createFailedConnection(DisconnectCause disconnectCause) {
        return new FailureSignalingConnection(disconnectCause);
    }

    public void checkImmutable() {
    }

    public static Connection createCanceledConnection() {
        return new FailureSignalingConnection(new DisconnectCause(4));
    }

    private final void fireOnConferenceableConnectionsChanged() {
        for (Listener l : this.mListeners) {
            l.onConferenceablesChanged(this, getConferenceables());
        }
    }

    private final void fireConferenceChanged() {
        for (Listener l : this.mListeners) {
            l.onConferenceChanged(this, this.mConference);
        }
    }

    private final void clearConferenceableList() {
        for (IConferenceable c : this.mConferenceables) {
            if (c instanceof Connection) {
                Connection connection = (Connection) c;
                connection.removeConnectionListener(this.mConnectionDeathListener);
            } else if (c instanceof Conference) {
                Conference conference = (Conference) c;
                conference.removeListener(this.mConferenceDeathListener);
            }
        }
        this.mConferenceables.clear();
    }

    protected final void updateConferenceParticipants(List<ConferenceParticipant> conferenceParticipants) {
        for (Listener l : this.mListeners) {
            l.onConferenceParticipantsChanged(this, conferenceParticipants);
        }
    }

    protected void notifyConferenceStarted() {
        for (Listener l : this.mListeners) {
            l.onConferenceStarted();
        }
    }
}
