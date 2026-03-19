package android.telecom;

import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.provider.CallLog;
import android.telecom.Conference;
import android.telecom.VideoProfile;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.Surface;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;
import com.mediatek.telecom.FormattedLog;
import com.mediatek.telecom.TelecomManagerEx;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Connection extends Conferenceable {
    public static final int CAPABILITY_BLIND_ASSURED_ECT = 536870912;
    public static final int CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO = 8388608;
    public static final int CAPABILITY_CAN_PAUSE_VIDEO = 1048576;
    public static final int CAPABILITY_CAN_PULL_CALL = 16777216;
    public static final int CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION = 4194304;
    public static final int CAPABILITY_CAN_UPGRADE_TO_VIDEO = 524288;
    public static final int CAPABILITY_CONFERENCE_HAS_NO_CHILDREN = 2097152;
    public static final int CAPABILITY_DISCONNECT_FROM_CONFERENCE = 8192;
    public static final int CAPABILITY_ECT = 67108864;
    public static final int CAPABILITY_HOLD = 1;
    public static final int CAPABILITY_INVITE_PARTICIPANTS = 268435456;
    public static final int CAPABILITY_MANAGE_CONFERENCE = 128;
    public static final int CAPABILITY_MERGE_CONFERENCE = 4;
    public static final int CAPABILITY_MUTE = 64;
    public static final int CAPABILITY_RESPOND_VIA_TEXT = 32;
    public static final int CAPABILITY_SEPARATE_FROM_CONFERENCE = 4096;
    public static final int CAPABILITY_SPEED_UP_MT_AUDIO = 262144;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL = 768;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_RX = 256;
    public static final int CAPABILITY_SUPPORTS_VT_LOCAL_TX = 512;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL = 3072;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_RX = 1024;
    public static final int CAPABILITY_SUPPORTS_VT_REMOTE_TX = 2048;
    public static final int CAPABILITY_SUPPORT_HOLD = 2;
    public static final int CAPABILITY_SWAP_CONFERENCE = 8;
    public static final int CAPABILITY_UNUSED = 16;
    public static final int CAPABILITY_UNUSED_2 = 16384;
    public static final int CAPABILITY_UNUSED_3 = 32768;
    public static final int CAPABILITY_UNUSED_4 = 65536;
    public static final int CAPABILITY_UNUSED_5 = 131072;
    public static final int CAPABILITY_VOICE_RECORD = 33554432;
    public static final int CAPABILITY_VOLTE = 134217728;
    public static final String EVENT_CALL_PULL_FAILED = "android.telecom.event.CALL_PULL_FAILED";
    public static final String EVENT_ON_HOLD_TONE_END = "android.telecom.event.ON_HOLD_TONE_END";
    public static final String EVENT_ON_HOLD_TONE_START = "android.telecom.event.ON_HOLD_TONE_START";
    public static final String EXTRA_CALL_SUBJECT = "android.telecom.extra.CALL_SUBJECT";
    public static final String EXTRA_CHILD_ADDRESS = "android.telecom.extra.CHILD_ADDRESS";
    public static final String EXTRA_LAST_FORWARDED_NUMBER = "android.telecom.extra.LAST_FORWARDED_NUMBER";
    private static final boolean PII_DEBUG = Log.isLoggable(3);
    public static final int PROPERTY_GENERIC_CONFERENCE = 2;
    public static final int PROPERTY_HIGH_DEF_AUDIO = 4;
    public static final int PROPERTY_IS_EXTERNAL_CALL = 16;
    public static final int PROPERTY_SHOW_CALLBACK_NUMBER = 1;
    public static final int PROPERTY_VOLTE = 32;
    public static final int PROPERTY_WIFI = 8;
    public static final int STATE_ACTIVE = 4;
    public static final int STATE_DIALING = 3;
    public static final int STATE_DISCONNECTED = 6;
    public static final int STATE_HOLDING = 5;
    public static final int STATE_INITIALIZING = 0;
    public static final int STATE_NEW = 1;
    public static final int STATE_PULLING_CALL = 7;
    public static final int STATE_RINGING = 2;
    private PhoneAccountHandle mAccountHandle;
    private Uri mAddress;
    private int mAddressPresentation;
    private boolean mAudioModeIsVoip;
    private CallAudioState mCallAudioState;
    private String mCallerDisplayName;
    private int mCallerDisplayNamePresentation;
    private Conference mConference;
    private int mConnectionCapabilities;
    private int mConnectionProperties;
    private ConnectionService mConnectionService;
    private DisconnectCause mDisconnectCause;
    private Bundle mExtras;
    private Set<String> mPreviousExtraKeys;
    private StatusHints mStatusHints;
    private String mTelecomCallId;
    private VideoProvider mVideoProvider;
    private int mVideoState;
    private final Listener mConnectionDeathListener = new Listener() {
        @Override
        public void onDestroyed(Connection c) {
            if (!Connection.this.mConferenceables.remove(c)) {
                return;
            }
            Connection.this.fireOnConferenceableConnectionsChanged();
        }
    };
    private final Conference.Listener mConferenceDeathListener = new Conference.Listener() {
        @Override
        public void onDestroyed(Conference c) {
            if (!Connection.this.mConferenceables.remove(c)) {
                return;
            }
            Connection.this.fireOnConferenceableConnectionsChanged();
        }
    };
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private final List<Conferenceable> mConferenceables = new ArrayList();
    private final List<Conferenceable> mUnmodifiableConferenceables = Collections.unmodifiableList(this.mConferenceables);
    private int mState = 1;
    private boolean mRingbackRequested = false;
    private long mConnectTimeMillis = 0;
    private final Object mExtrasLock = new Object();

    public static boolean can(int capabilities, int capability) {
        return (capabilities & capability) == capability;
    }

    public boolean can(int capability) {
        return can(this.mConnectionCapabilities, capability);
    }

    public void removeCapability(int capability) {
        this.mConnectionCapabilities &= ~capability;
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
            builder.append(" CAPABILITY_SUPPORTS_VT_LOCAL_RX");
        }
        if (can(capabilities, 512)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_LOCAL_TX");
        }
        if (can(capabilities, 768)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL");
        }
        if (can(capabilities, 1024)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_REMOTE_RX");
        }
        if (can(capabilities, 2048)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_REMOTE_TX");
        }
        if (can(capabilities, 3072)) {
            builder.append(" CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL");
        }
        if (can(capabilities, 8388608)) {
            builder.append(" CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO");
        }
        if (can(capabilities, 262144)) {
            builder.append(" CAPABILITY_SPEED_UP_MT_AUDIO");
        }
        if (can(capabilities, 524288)) {
            builder.append(" CAPABILITY_CAN_UPGRADE_TO_VIDEO");
        }
        if (can(capabilities, 1048576)) {
            builder.append(" CAPABILITY_CAN_PAUSE_VIDEO");
        }
        if (can(capabilities, 2097152)) {
            builder.append(" CAPABILITY_SINGLE_PARTY_CONFERENCE");
        }
        if (can(capabilities, 4194304)) {
            builder.append(" CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION");
        }
        if (can(capabilities, 16777216)) {
            builder.append(" CAPABILITY_CAN_PULL_CALL");
        }
        if ((33554432 & capabilities) != 0) {
            builder.append(" CAPABILITY_VOICE_RECORD");
        }
        if ((67108864 & capabilities) != 0) {
            builder.append(" CAPABILITY_ECT");
        }
        if ((134217728 & capabilities) != 0) {
            builder.append(" CAPABILITY_VOLTE");
        }
        if ((268435456 & capabilities) != 0) {
            builder.append(" CAPABILITY_INVITE_PARTICIPANTS");
        }
        if ((capabilities & 4096) != 0) {
            builder.append(" CAPABILITY_SEPARATE_FROM_CONFERENCE");
        }
        if ((capabilities & 8192) != 0) {
            builder.append(" CAPABILITY_DISCONNECT_FROM_CONFERENCE");
        }
        if ((536870912 & capabilities) != 0) {
            builder.append(" CAPABILITY_BLIND_ASSURED_ECT");
        }
        builder.append("]");
        return builder.toString();
    }

    public static String propertiesToString(int properties) {
        StringBuilder builder = new StringBuilder();
        builder.append("[Properties:");
        if (can(properties, 1)) {
            builder.append(" PROPERTY_SHOW_CALLBACK_NUMBER");
        }
        if (can(properties, 4)) {
            builder.append(" PROPERTY_HIGH_DEF_AUDIO");
        }
        if (can(properties, 8)) {
            builder.append(" PROPERTY_WIFI");
        }
        if (can(properties, 2)) {
            builder.append(" PROPERTY_GENERIC_CONFERENCE");
        }
        if (can(properties, 16)) {
            builder.append(" PROPERTY_IS_EXTERNAL_CALL");
        }
        if (can(properties, 32)) {
            builder.append(" PROPERTY_VOLTE");
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

        public void onConnectionPropertiesChanged(Connection c, int properties) {
        }

        public void onVideoProviderChanged(Connection c, VideoProvider videoProvider) {
        }

        public void onAudioModeIsVoipChanged(Connection c, boolean isVoip) {
        }

        public void onStatusHintsChanged(Connection c, StatusHints statusHints) {
        }

        public void onConferenceablesChanged(Connection c, List<Conferenceable> conferenceables) {
        }

        public void onConferenceChanged(Connection c, Conference conference) {
        }

        public void onConferenceParticipantsChanged(Connection c, List<ConferenceParticipant> participants) {
        }

        public void onConferenceStarted() {
        }

        public void onConferenceMergeFailed(Connection c) {
        }

        public void onExtrasChanged(Connection c, Bundle extras) {
        }

        public void onExtrasRemoved(Connection c, List<String> keys) {
        }

        public void onConnectionEvent(Connection c, String event, Bundle extras) {
        }
    }

    public static abstract class VideoProvider {
        private static final int MSG_ADD_VIDEO_CALLBACK = 1;
        private static final int MSG_MTK_BASE = 100;
        private static final int MSG_REMOVE_VIDEO_CALLBACK = 12;
        private static final int MSG_REQUEST_CAMERA_CAPABILITIES = 9;
        private static final int MSG_REQUEST_CONNECTION_DATA_USAGE = 10;
        private static final int MSG_SEND_SESSION_MODIFY_REQUEST = 7;
        private static final int MSG_SEND_SESSION_MODIFY_RESPONSE = 8;
        private static final int MSG_SET_CAMERA = 2;
        private static final int MSG_SET_DEVICE_ORIENTATION = 5;
        private static final int MSG_SET_DISPLAY_SURFACE = 4;
        private static final int MSG_SET_PAUSE_IMAGE = 11;
        private static final int MSG_SET_PREVIEW_SURFACE = 3;
        private static final int MSG_SET_UI_MODE = 100;
        private static final int MSG_SET_ZOOM = 6;
        public static final int SESSION_EVENT_CAMERA_FAILURE = 5;
        public static final int SESSION_EVENT_CAMERA_READY = 6;
        public static final int SESSION_EVENT_ERROR_CAMERA_CRASHED = 8003;
        public static final int SESSION_EVENT_RX_PAUSE = 1;
        public static final int SESSION_EVENT_RX_RESUME = 2;
        public static final int SESSION_EVENT_TX_START = 3;
        public static final int SESSION_EVENT_TX_STOP = 4;
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE = 5;
        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        public static final int SESSION_MODIFY_REQUEST_TIMED_OUT = 4;
        public static final int UI_MODE_BG = 1;
        public static final int UI_MODE_FG = 0;
        public static final int UI_MODE_FULL_SCREEN = 2;
        public static final int UI_MODE_NORMAL_SCREEN = 3;
        private final VideoProviderBinder mBinder;
        private VideoProviderHandler mMessageHandler;
        private ConcurrentHashMap<IBinder, IVideoCallback> mVideoCallbacks;

        public abstract void onRequestCameraCapabilities();

        public abstract void onRequestConnectionDataUsage();

        public abstract void onSendSessionModifyRequest(VideoProfile videoProfile, VideoProfile videoProfile2);

        public abstract void onSendSessionModifyResponse(VideoProfile videoProfile);

        public abstract void onSetCamera(String str);

        public abstract void onSetDeviceOrientation(int i);

        public abstract void onSetDisplaySurface(Surface surface);

        public abstract void onSetPauseImage(Uri uri);

        public abstract void onSetPreviewSurface(Surface surface);

        public abstract void onSetZoom(float f);

        private final class VideoProviderHandler extends Handler {
            public VideoProviderHandler() {
            }

            public VideoProviderHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        IBinder binder = (IBinder) msg.obj;
                        IVideoCallback callback = IVideoCallback.Stub.asInterface((IBinder) msg.obj);
                        if (callback == null) {
                            Log.w(this, "addVideoProvider - skipped; callback is null.", new Object[0]);
                            return;
                        } else if (VideoProvider.this.mVideoCallbacks.containsKey(binder)) {
                            Log.i(this, "addVideoProvider - skipped; already present.", new Object[0]);
                            return;
                        } else {
                            Log.w(this, "Connection addVideocallback put binder=:" + binder + "callback=" + callback, new Object[0]);
                            VideoProvider.this.mVideoCallbacks.put(binder, callback);
                            return;
                        }
                    case 2:
                        VideoProvider.this.onSetCamera((String) msg.obj);
                        return;
                    case 3:
                        VideoProvider.this.onSetPreviewSurface((Surface) msg.obj);
                        return;
                    case 4:
                        VideoProvider.this.onSetDisplaySurface((Surface) msg.obj);
                        return;
                    case 5:
                        VideoProvider.this.onSetDeviceOrientation(msg.arg1);
                        return;
                    case 6:
                        VideoProvider.this.onSetZoom(((Float) msg.obj).floatValue());
                        return;
                    case 7:
                        SomeArgs args = (SomeArgs) msg.obj;
                        try {
                            VideoProvider.this.onSendSessionModifyRequest((VideoProfile) args.arg1, (VideoProfile) args.arg2);
                            return;
                        } finally {
                            args.recycle();
                        }
                    case 8:
                        VideoProvider.this.onSendSessionModifyResponse((VideoProfile) msg.obj);
                        return;
                    case 9:
                        VideoProvider.this.onRequestCameraCapabilities();
                        return;
                    case 10:
                        VideoProvider.this.onRequestConnectionDataUsage();
                        return;
                    case 11:
                        VideoProvider.this.onSetPauseImage((Uri) msg.obj);
                        return;
                    case 12:
                        IBinder binder2 = (IBinder) msg.obj;
                        IVideoCallback callback2 = IVideoCallback.Stub.asInterface((IBinder) msg.obj);
                        if (!VideoProvider.this.mVideoCallbacks.containsKey(binder2)) {
                            Log.i(this, "removeVideoProvider - skipped; not present.", new Object[0]);
                            return;
                        } else {
                            Log.w(this, "Connection removeVideocallback binder=:" + binder2 + "callback=" + callback2, new Object[0]);
                            VideoProvider.this.mVideoCallbacks.remove(binder2);
                            return;
                        }
                    case 100:
                        VideoProvider.this.onSetUIMode(((Integer) msg.obj).intValue());
                        return;
                    default:
                        return;
                }
            }
        }

        private final class VideoProviderBinder extends IVideoProvider.Stub {
            VideoProviderBinder(VideoProvider this$1, VideoProviderBinder videoProviderBinder) {
                this();
            }

            private VideoProviderBinder() {
            }

            public void addVideoCallback(IBinder videoCallbackBinder) {
                VideoProvider.this.mMessageHandler.obtainMessage(1, videoCallbackBinder).sendToTarget();
            }

            public void removeVideoCallback(IBinder videoCallbackBinder) {
                VideoProvider.this.mMessageHandler.obtainMessage(12, videoCallbackBinder).sendToTarget();
            }

            public void setCamera(String cameraId) {
                VideoProvider.this.mMessageHandler.obtainMessage(2, cameraId).sendToTarget();
            }

            public void setPreviewSurface(Surface surface) {
                VideoProvider.this.mMessageHandler.obtainMessage(3, surface).sendToTarget();
            }

            public void setDisplaySurface(Surface surface) {
                VideoProvider.this.mMessageHandler.obtainMessage(4, surface).sendToTarget();
            }

            public void setDeviceOrientation(int rotation) {
                VideoProvider.this.mMessageHandler.obtainMessage(5, rotation, 0).sendToTarget();
            }

            public void setZoom(float value) {
                VideoProvider.this.mMessageHandler.obtainMessage(6, Float.valueOf(value)).sendToTarget();
            }

            public void sendSessionModifyRequest(VideoProfile fromProfile, VideoProfile toProfile) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = fromProfile;
                args.arg2 = toProfile;
                VideoProvider.this.mMessageHandler.obtainMessage(7, args).sendToTarget();
            }

            public void sendSessionModifyResponse(VideoProfile responseProfile) {
                VideoProvider.this.mMessageHandler.obtainMessage(8, responseProfile).sendToTarget();
            }

            public void requestCameraCapabilities() {
                VideoProvider.this.mMessageHandler.obtainMessage(9).sendToTarget();
            }

            public void requestCallDataUsage() {
                VideoProvider.this.mMessageHandler.obtainMessage(10).sendToTarget();
            }

            public void setPauseImage(Uri uri) {
                VideoProvider.this.mMessageHandler.obtainMessage(11, uri).sendToTarget();
            }

            public void setUIMode(int mode) {
                VideoProvider.this.mMessageHandler.obtainMessage(100, Integer.valueOf(mode)).sendToTarget();
            }
        }

        public VideoProvider() {
            this.mVideoCallbacks = new ConcurrentHashMap<>(8, 0.9f, 1);
            this.mBinder = new VideoProviderBinder(this, null);
            this.mMessageHandler = new VideoProviderHandler(Looper.getMainLooper());
        }

        public VideoProvider(Looper looper) {
            this.mVideoCallbacks = new ConcurrentHashMap<>(8, 0.9f, 1);
            this.mBinder = new VideoProviderBinder(this, null);
            this.mMessageHandler = new VideoProviderHandler(looper);
        }

        public final IVideoProvider getInterface() {
            return this.mBinder;
        }

        public void onSetUIMode(int mode) {
        }

        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.receiveSessionModifyRequest(videoProfile);
                } catch (RemoteException ignored) {
                    Log.w(this, "receiveSessionModifyRequest callback failed", ignored);
                }
            }
        }

        public void receiveSessionModifyResponse(int status, VideoProfile requestedProfile, VideoProfile responseProfile) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.receiveSessionModifyResponse(status, requestedProfile, responseProfile);
                } catch (RemoteException ignored) {
                    Log.w(this, "receiveSessionModifyResponse callback failed", ignored);
                }
            }
        }

        public void handleCallSessionEvent(int event) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.handleCallSessionEvent(event);
                } catch (RemoteException ignored) {
                    Log.w(this, "handleCallSessionEvent callback failed", ignored);
                }
            }
        }

        public void changePeerDimensions(int width, int height) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.changePeerDimensions(width, height);
                } catch (RemoteException ignored) {
                    Log.w(this, "changePeerDimensions callback failed", ignored);
                }
            }
        }

        public void changePeerDimensionsWithAngle(int width, int height, int rotation) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.changePeerDimensionsWithAngle(width, height, rotation);
                } catch (RemoteException ignored) {
                    Log.w(this, "changePeerDimensionsWithAngle callback failed", ignored);
                }
            }
        }

        public void setCallDataUsage(long dataUsage) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.changeCallDataUsage(dataUsage);
                } catch (RemoteException ignored) {
                    Log.w(this, "setCallDataUsage callback failed", ignored);
                }
            }
        }

        public void changeCallDataUsage(long dataUsage) {
            setCallDataUsage(dataUsage);
        }

        public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    Log.w(this, "changeCameraCapabilities callback=:" + callback, new Object[0]);
                    callback.changeCameraCapabilities(cameraCapabilities);
                } catch (RemoteException ignored) {
                    Log.w(this, "changeCameraCapabilities callback failed", ignored);
                }
            }
        }

        public void changeVideoQuality(int videoQuality) {
            if (this.mVideoCallbacks == null) {
                return;
            }
            for (IVideoCallback callback : this.mVideoCallbacks.values()) {
                try {
                    callback.changeVideoQuality(videoQuality);
                } catch (RemoteException ignored) {
                    Log.w(this, "changeVideoQuality callback failed", ignored);
                }
            }
        }
    }

    public final String getTelecomCallId() {
        return this.mTelecomCallId;
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

    @Deprecated
    public final AudioState getAudioState() {
        if (this.mCallAudioState == null) {
            return null;
        }
        return new AudioState(this.mCallAudioState);
    }

    public final CallAudioState getCallAudioState() {
        return this.mCallAudioState;
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

    public final long getConnectTimeMillis() {
        return this.mConnectTimeMillis;
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final Bundle getExtras() {
        Bundle bundle = null;
        synchronized (this.mExtrasLock) {
            if (this.mExtras != null) {
                bundle = new Bundle(this.mExtras);
            }
        }
        return bundle;
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

    public void setTelecomCallId(String callId) {
        this.mTelecomCallId = callId;
    }

    final void setCallAudioState(CallAudioState state) {
        checkImmutable();
        Log.d(this, "setAudioState %s", state);
        this.mCallAudioState = state;
        onAudioStateChanged(getAudioState());
        onCallAudioStateChanged(state);
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "INITIALIZING";
            case 1:
                return "NEW";
            case 2:
                return "RINGING";
            case 3:
                return "DIALING";
            case 4:
                return "ACTIVE";
            case 5:
                return "HOLDING";
            case 6:
                return "DISCONNECTED";
            default:
                Log.wtf(Connection.class, "Unknown state %d", Integer.valueOf(state));
                return "UNKNOWN";
        }
    }

    public final int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public final int getConnectionProperties() {
        return this.mConnectionProperties;
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

    public final void setNextPostDialChar(char nextChar) {
        checkImmutable();
        for (Listener l : this.mListeners) {
            l.onPostDialChar(this, nextChar);
        }
    }

    public final void setRingbackRequested(boolean ringback) {
        checkImmutable();
        if (this.mRingbackRequested == ringback) {
            return;
        }
        this.mRingbackRequested = ringback;
        for (Listener l : this.mListeners) {
            l.onRingbackRequested(this, ringback);
        }
    }

    public final void setConnectionCapabilities(int connectionCapabilities) {
        checkImmutable();
        if (this.mConnectionCapabilities == connectionCapabilities) {
            return;
        }
        this.mConnectionCapabilities = connectionCapabilities;
        for (Listener l : this.mListeners) {
            l.onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
        }
    }

    public final void setConnectionProperties(int connectionProperties) {
        checkImmutable();
        if (this.mConnectionProperties == connectionProperties) {
            return;
        }
        this.mConnectionProperties = connectionProperties;
        for (Listener l : this.mListeners) {
            l.onConnectionPropertiesChanged(this, this.mConnectionProperties);
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

    public final void setConnectTimeMillis(long connectTimeMillis) {
        this.mConnectTimeMillis = connectTimeMillis;
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

    public final void setConferenceables(List<Conferenceable> conferenceables) {
        clearConferenceableList();
        for (Conferenceable c : conferenceables) {
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

    public final List<Conferenceable> getConferenceables() {
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
        if (this.mConference == null) {
            this.mConference = conference;
            if (this.mConnectionService != null && this.mConnectionService.containsConference(conference)) {
                fireConferenceChanged();
                return true;
            }
            return true;
        }
        return false;
    }

    public final void resetConference() {
        if (this.mConference == null) {
            return;
        }
        Log.d(this, "Conference reset", new Object[0]);
        this.mConference = null;
        fireConferenceChanged();
    }

    public final void notifyConnectionLost() {
        sendConnectionEvent("com.mediatek.telecom.event.CONNECTION_LOST", null);
    }

    public final void notifyActionFailed(int action) {
        Log.i(this, "notifyActionFailed action = " + action, new Object[0]);
        sendConnectionEvent("com.mediatek.telecom.event.OPERATION_FAIL", TelecomManagerEx.createOperationFailBundle(action));
    }

    public void notifySSNotificationToast(int notiType, int type, int code, String number, int index) {
        Log.i(this, "notifySSNotificationToast notiType = " + notiType + " type = " + type + " code = " + code + " number = " + number + " index = " + index, new Object[0]);
        sendConnectionEvent("com.mediatek.telecom.event.SS_NOTIFICATION", TelecomManagerEx.createSsNotificationBundle(notiType, type, code, number, index));
    }

    public void notifyNumberUpdate(String number) {
        Log.i(this, "notifyNumberUpdate number = " + number, new Object[0]);
        if (TextUtils.isEmpty(number)) {
            return;
        }
        sendConnectionEvent("com.mediatek.telecom.event.NUMBER_UPDATED", TelecomManagerEx.createNumberUpdatedBundle(number));
    }

    public void notifyIncomingInfoUpdate(int type, String alphaid, int cliValidity) {
        Log.i(this, "notifyIncomingInfoUpdate type = " + type + " alphaid = " + alphaid + " cliValidity = " + cliValidity, new Object[0]);
        sendConnectionEvent("com.mediatek.telecom.event.INCOMING_INFO_UPDATED", TelecomManagerEx.createIncomingInfoUpdatedBundle(type, alphaid, cliValidity));
    }

    protected void fireOnCdmaCallAccepted() {
        Log.d(this, "fireOnCdmaCallAccepted: %s", stateToString(this.mState));
        sendConnectionEvent("com.mediatek.telecom.event.CDMA_CALL_ACCEPTED", null);
    }

    public PhoneAccountHandle getAccountHandle() {
        return this.mAccountHandle;
    }

    public void setAccountHandle(PhoneAccountHandle handle) {
        this.mAccountHandle = handle;
        sendConnectionEvent("com.mediatek.telecom.event.PHONE_ACCOUNT_CHANGED", TelecomManagerEx.createPhoneAccountChangedBundle(handle));
    }

    public final void notifyVtStatusInfo(int status) {
        Log.d(this, "notifyVtStatusInfo %s" + status, new Object[0]);
        sendConnectionEvent("com.mediatek.telecom.event.VT_STATUS_UPDATED", TelecomManagerEx.createVtStatudUpdatedBundle(status));
    }

    public void setCallInfo(Bundle bundle) {
        Log.d(this, "setCallInfo", new Object[0]);
        sendConnectionEvent("com.mediatek.telecom.event.UPDATE_VOLTE_EXTRA", bundle);
    }

    public final void setExtras(Bundle extras) {
        checkImmutable();
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
        if (extras == null) {
            return;
        }
        this.mPreviousExtraKeys.addAll(extras.keySet());
    }

    public final void putExtras(Bundle extras) {
        Bundle listenerExtras;
        checkImmutable();
        if (extras == null) {
            return;
        }
        synchronized (this.mExtrasLock) {
            if (this.mExtras == null) {
                this.mExtras = new Bundle();
            }
            this.mExtras.putAll(extras);
            listenerExtras = new Bundle(this.mExtras);
        }
        for (Listener l : this.mListeners) {
            l.onExtrasChanged(this, new Bundle(listenerExtras));
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

    @Deprecated
    public void onAudioStateChanged(AudioState state) {
    }

    public void onCallAudioStateChanged(CallAudioState state) {
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

    public void onReject(String replyMessage) {
    }

    public void onSilence() {
    }

    public void onPostDialContinue(boolean proceed) {
    }

    public void onPullExternalCall() {
    }

    public void onCallEvent(String event, Bundle extras) {
    }

    public void onExtrasChanged(Bundle extras) {
    }

    public void onExplicitCallTransfer() {
    }

    public void onExplicitCallTransfer(String number, int type) {
    }

    public void onHangupAll() {
    }

    static String toLogSafePhoneNumber(String number) {
        if (number == null) {
            return ProxyInfo.LOCAL_EXCL_LIST;
        }
        if (PII_DEBUG) {
            return number;
        }
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

    private void setState(int state) {
        FormattedLog formattedLog;
        checkImmutable();
        if (this.mState == 6 && this.mState != state) {
            Log.d(this, "Connection already DISCONNECTED; cannot transition out of this state.", new Object[0]);
            return;
        }
        if (this.mState == state) {
            return;
        }
        Log.d(this, "setState: %s", stateToString(state));
        this.mState = state;
        FormattedLog.Builder builder = configDumpLogBuilder(new FormattedLog.Builder());
        if (builder != null && (formattedLog = builder.buildDumpInfo()) != null) {
            Log.d(this, formattedLog.toString(), new Object[0]);
        }
        onStateChanged(state);
        for (Listener l : this.mListeners) {
            l.onStateChanged(this, state);
        }
    }

    protected void fireOnCallState() {
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
            if (!this.mImmutable) {
            } else {
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
        for (Conferenceable c : this.mConferenceables) {
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

    protected final void notifyConferenceMergeFailed() {
        for (Listener l : this.mListeners) {
            l.onConferenceMergeFailed(this);
        }
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

    public void sendConnectionEvent(String event, Bundle extras) {
        for (Listener l : this.mListeners) {
            l.onConnectionEvent(this, event, extras);
        }
    }

    protected FormattedLog.Builder configDumpLogBuilder(FormattedLog.Builder builder) {
        if (builder == null) {
            return null;
        }
        builder.setCategory("CC");
        builder.setOpType(FormattedLog.OpType.DUMP);
        if (this.mAddress != null) {
            builder.setCallNumber(this.mAddress.getSchemeSpecificPart());
        }
        builder.setCallId(Integer.toString(System.identityHashCode(this)));
        builder.setStatusInfo("isConfCall", "No");
        builder.setStatusInfo("state", callStateToFormattedDumpString(this.mState));
        if (getConference() == null) {
            builder.setStatusInfo("isConfChildCall", "No");
        } else {
            builder.setStatusInfo("isConfChildCall", "Yes");
        }
        builder.setStatusInfo("capabilities", capabilitiesToString(getConnectionCapabilities()));
        return builder;
    }

    static String callStateToFormattedDumpString(int state) {
        switch (state) {
            case 0:
            case 1:
                return CallLog.Calls.NEW;
            case 2:
                return "ringing";
            case 3:
                return "dialing";
            case 4:
                return "active";
            case 5:
                return "onhold";
            case 6:
                return "disconnected";
            default:
                return "unknown";
        }
    }
}
