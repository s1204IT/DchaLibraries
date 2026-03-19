package android.telecom;

import android.annotation.SystemApi;
import android.bluetooth.BluetoothDevice;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.util.ArraySet;
import android.view.Surface;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IVideoCallback;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.transition.EpicenterTranslateClipReveal;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class Connection extends Conferenceable {
    public static final int CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO = 8388608;
    public static final int CAPABILITY_CAN_PAUSE_VIDEO = 1048576;
    public static final int CAPABILITY_CAN_PULL_CALL = 16777216;
    public static final int CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION = 4194304;
    public static final int CAPABILITY_CAN_UPGRADE_TO_VIDEO = 524288;
    public static final int CAPABILITY_CONFERENCE_HAS_NO_CHILDREN = 2097152;
    public static final int CAPABILITY_DISCONNECT_FROM_CONFERENCE = 8192;
    public static final int CAPABILITY_HOLD = 1;
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
    public static final int CAPABILITY_SUPPORT_DEFLECT = 33554432;
    public static final int CAPABILITY_SUPPORT_HOLD = 2;
    public static final int CAPABILITY_SWAP_CONFERENCE = 8;
    public static final int CAPABILITY_UNUSED = 16;
    public static final int CAPABILITY_UNUSED_2 = 16384;
    public static final int CAPABILITY_UNUSED_3 = 32768;
    public static final int CAPABILITY_UNUSED_4 = 65536;
    public static final int CAPABILITY_UNUSED_5 = 131072;
    public static final String EVENT_CALL_MERGE_FAILED = "android.telecom.event.CALL_MERGE_FAILED";
    public static final String EVENT_CALL_PULL_FAILED = "android.telecom.event.CALL_PULL_FAILED";
    public static final String EVENT_CALL_REMOTELY_HELD = "android.telecom.event.CALL_REMOTELY_HELD";
    public static final String EVENT_CALL_REMOTELY_UNHELD = "android.telecom.event.CALL_REMOTELY_UNHELD";
    public static final String EVENT_HANDOVER_COMPLETE = "android.telecom.event.HANDOVER_COMPLETE";
    public static final String EVENT_HANDOVER_FAILED = "android.telecom.event.HANDOVER_FAILED";
    public static final String EVENT_MERGE_COMPLETE = "android.telecom.event.MERGE_COMPLETE";
    public static final String EVENT_MERGE_START = "android.telecom.event.MERGE_START";
    public static final String EVENT_ON_HOLD_TONE_END = "android.telecom.event.ON_HOLD_TONE_END";
    public static final String EVENT_ON_HOLD_TONE_START = "android.telecom.event.ON_HOLD_TONE_START";
    public static final String EXTRA_ANSWERING_DROPS_FG_CALL = "android.telecom.extra.ANSWERING_DROPS_FG_CALL";
    public static final String EXTRA_ANSWERING_DROPS_FG_CALL_APP_NAME = "android.telecom.extra.ANSWERING_DROPS_FG_CALL_APP_NAME";
    public static final String EXTRA_CALL_SUBJECT = "android.telecom.extra.CALL_SUBJECT";
    public static final String EXTRA_CHILD_ADDRESS = "android.telecom.extra.CHILD_ADDRESS";
    public static final String EXTRA_DISABLE_ADD_CALL = "android.telecom.extra.DISABLE_ADD_CALL";
    public static final String EXTRA_LAST_FORWARDED_NUMBER = "android.telecom.extra.LAST_FORWARDED_NUMBER";
    public static final String EXTRA_ORIGINAL_CONNECTION_ID = "android.telecom.extra.ORIGINAL_CONNECTION_ID";
    private static final boolean PII_DEBUG = Log.isLoggable(3);
    public static final int PROPERTY_ASSISTED_DIALING_USED = 512;
    public static final int PROPERTY_EMERGENCY_CALLBACK_MODE = 1;
    public static final int PROPERTY_GENERIC_CONFERENCE = 2;
    public static final int PROPERTY_HAS_CDMA_VOICE_PRIVACY = 32;
    public static final int PROPERTY_HIGH_DEF_AUDIO = 4;
    public static final int PROPERTY_IS_DOWNGRADED_CONFERENCE = 64;
    public static final int PROPERTY_IS_EXTERNAL_CALL = 16;
    public static final int PROPERTY_IS_RTT = 256;
    public static final int PROPERTY_SELF_MANAGED = 128;
    public static final int PROPERTY_WIFI = 8;
    public static final int STATE_ACTIVE = 4;
    public static final int STATE_DIALING = 3;
    public static final int STATE_DISCONNECTED = 6;
    public static final int STATE_HOLDING = 5;
    public static final int STATE_INITIALIZING = 0;
    public static final int STATE_NEW = 1;
    public static final int STATE_PULLING_CALL = 7;
    public static final int STATE_RINGING = 2;
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
    private PhoneAccountHandle mPhoneAccountHandle;
    private Set<String> mPreviousExtraKeys;
    private StatusHints mStatusHints;
    private String mTelecomCallId;
    private VideoProvider mVideoProvider;
    private int mVideoState;
    private final Listener mConnectionDeathListener = new Listener() {
        @Override
        public void onDestroyed(Connection connection) {
            if (Connection.this.mConferenceables.remove(connection)) {
                Connection.this.fireOnConferenceableConnectionsChanged();
            }
        }
    };
    private final Conference.Listener mConferenceDeathListener = new Conference.Listener() {
        @Override
        public void onDestroyed(Conference conference) {
            if (Connection.this.mConferenceables.remove(conference)) {
                Connection.this.fireOnConferenceableConnectionsChanged();
            }
        }
    };
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
    private final List<Conferenceable> mConferenceables = new ArrayList();
    private final List<Conferenceable> mUnmodifiableConferenceables = Collections.unmodifiableList(this.mConferenceables);
    private int mState = 1;
    private boolean mRingbackRequested = false;
    private int mSupportedAudioRoutes = 15;
    private long mConnectTimeMillis = 0;
    private long mConnectElapsedTimeMillis = 0;
    private final Object mExtrasLock = new Object();

    public static boolean can(int i, int i2) {
        return (i & i2) == i2;
    }

    public boolean can(int i) {
        return can(this.mConnectionCapabilities, i);
    }

    public void removeCapability(int i) {
        this.mConnectionCapabilities = (~i) & this.mConnectionCapabilities;
    }

    public void addCapability(int i) {
        this.mConnectionCapabilities = i | this.mConnectionCapabilities;
    }

    public static String capabilitiesToString(int i) {
        return capabilitiesToStringInternal(i, true);
    }

    public static String capabilitiesToStringShort(int i) {
        return capabilitiesToStringInternal(i, false);
    }

    protected static String capabilitiesToStringInternal(int i, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (z) {
            sb.append("Capabilities:");
        }
        if (can(i, 1)) {
            sb.append(z ? " CAPABILITY_HOLD" : " hld");
        }
        if (can(i, 2)) {
            sb.append(z ? " CAPABILITY_SUPPORT_HOLD" : " sup_hld");
        }
        if (can(i, 4)) {
            sb.append(z ? " CAPABILITY_MERGE_CONFERENCE" : " mrg_cnf");
        }
        if (can(i, 8)) {
            sb.append(z ? " CAPABILITY_SWAP_CONFERENCE" : " swp_cnf");
        }
        if (can(i, 32)) {
            sb.append(z ? " CAPABILITY_RESPOND_VIA_TEXT" : " txt");
        }
        if (can(i, 64)) {
            sb.append(z ? " CAPABILITY_MUTE" : " mut");
        }
        if (can(i, 128)) {
            sb.append(z ? " CAPABILITY_MANAGE_CONFERENCE" : " mng_cnf");
        }
        if (can(i, 256)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_LOCAL_RX" : " VTlrx");
        }
        if (can(i, 512)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_LOCAL_TX" : " VTltx");
        }
        if (can(i, 768)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL" : " VTlbi");
        }
        if (can(i, 1024)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_REMOTE_RX" : " VTrrx");
        }
        if (can(i, 2048)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_REMOTE_TX" : " VTrtx");
        }
        if (can(i, 3072)) {
            sb.append(z ? " CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL" : " VTrbi");
        }
        if (can(i, 8388608)) {
            sb.append(z ? " CAPABILITY_CANNOT_DOWNGRADE_VIDEO_TO_AUDIO" : " !v2a");
        }
        if (can(i, 262144)) {
            sb.append(z ? " CAPABILITY_SPEED_UP_MT_AUDIO" : " spd_aud");
        }
        if (can(i, 524288)) {
            sb.append(z ? " CAPABILITY_CAN_UPGRADE_TO_VIDEO" : " a2v");
        }
        if (can(i, 1048576)) {
            sb.append(z ? " CAPABILITY_CAN_PAUSE_VIDEO" : " paus_VT");
        }
        if (can(i, 2097152)) {
            sb.append(z ? " CAPABILITY_SINGLE_PARTY_CONFERENCE" : " 1p_cnf");
        }
        if (can(i, 4194304)) {
            sb.append(z ? " CAPABILITY_CAN_SEND_RESPONSE_VIA_CONNECTION" : " rsp_by_con");
        }
        if (can(i, 16777216)) {
            sb.append(z ? " CAPABILITY_CAN_PULL_CALL" : " pull");
        }
        if (can(i, 33554432)) {
            sb.append(z ? " CAPABILITY_SUPPORT_DEFLECT" : " sup_def");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String propertiesToString(int i) {
        return propertiesToStringInternal(i, true);
    }

    public static String propertiesToStringShort(int i) {
        return propertiesToStringInternal(i, false);
    }

    private static String propertiesToStringInternal(int i, boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (z) {
            sb.append("Properties:");
        }
        if (can(i, 128)) {
            sb.append(z ? " PROPERTY_SELF_MANAGED" : " self_mng");
        }
        if (can(i, 1)) {
            sb.append(z ? " PROPERTY_EMERGENCY_CALLBACK_MODE" : " ecbm");
        }
        if (can(i, 4)) {
            sb.append(z ? " PROPERTY_HIGH_DEF_AUDIO" : " HD");
        }
        if (can(i, 8)) {
            sb.append(z ? " PROPERTY_WIFI" : " wifi");
        }
        if (can(i, 2)) {
            sb.append(z ? " PROPERTY_GENERIC_CONFERENCE" : " gen_conf");
        }
        if (can(i, 16)) {
            sb.append(z ? " PROPERTY_IS_EXTERNAL_CALL" : " xtrnl");
        }
        if (can(i, 32)) {
            sb.append(z ? " PROPERTY_HAS_CDMA_VOICE_PRIVACY" : " priv");
        }
        if (can(i, 256)) {
            sb.append(z ? " PROPERTY_IS_RTT" : " rtt");
        }
        sb.append("]");
        return sb.toString();
    }

    public static abstract class Listener {
        public void onStateChanged(Connection connection, int i) {
        }

        public void onAddressChanged(Connection connection, Uri uri, int i) {
        }

        public void onCallerDisplayNameChanged(Connection connection, String str, int i) {
        }

        public void onVideoStateChanged(Connection connection, int i) {
        }

        public void onDisconnected(Connection connection, DisconnectCause disconnectCause) {
        }

        public void onPostDialWait(Connection connection, String str) {
        }

        public void onPostDialChar(Connection connection, char c) {
        }

        public void onRingbackRequested(Connection connection, boolean z) {
        }

        public void onDestroyed(Connection connection) {
        }

        public void onConnectionCapabilitiesChanged(Connection connection, int i) {
        }

        public void onConnectionPropertiesChanged(Connection connection, int i) {
        }

        public void onSupportedAudioRoutesChanged(Connection connection, int i) {
        }

        public void onVideoProviderChanged(Connection connection, VideoProvider videoProvider) {
        }

        public void onAudioModeIsVoipChanged(Connection connection, boolean z) {
        }

        public void onStatusHintsChanged(Connection connection, StatusHints statusHints) {
        }

        public void onConferenceablesChanged(Connection connection, List<Conferenceable> list) {
        }

        public void onConferenceChanged(Connection connection, Conference conference) {
        }

        public void onConferenceParticipantsChanged(Connection connection, List<ConferenceParticipant> list) {
        }

        public void onConferenceStarted() {
        }

        public void onConferenceMergeFailed(Connection connection) {
        }

        public void onExtrasChanged(Connection connection, Bundle bundle) {
        }

        public void onExtrasRemoved(Connection connection, List<String> list) {
        }

        public void onConnectionEvent(Connection connection, String str, Bundle bundle) {
        }

        public void onConferenceSupportedChanged(Connection connection, boolean z) {
        }

        public void onAudioRouteChanged(Connection connection, int i, String str) {
        }

        public void onRttInitiationSuccess(Connection connection) {
        }

        public void onRttInitiationFailure(Connection connection, int i) {
        }

        public void onRttSessionRemotelyTerminated(Connection connection) {
        }

        public void onRemoteRttRequest(Connection connection) {
        }

        public void onPhoneAccountChanged(Connection connection, PhoneAccountHandle phoneAccountHandle) {
        }
    }

    public static final class RttTextStream {
        private static final int READ_BUFFER_SIZE = 1000;
        private final ParcelFileDescriptor mFdFromInCall;
        private final ParcelFileDescriptor mFdToInCall;
        private final InputStreamReader mPipeFromInCall;
        private final OutputStreamWriter mPipeToInCall;
        private char[] mReadBuffer = new char[1000];

        public RttTextStream(ParcelFileDescriptor parcelFileDescriptor, ParcelFileDescriptor parcelFileDescriptor2) {
            this.mFdFromInCall = parcelFileDescriptor2;
            this.mFdToInCall = parcelFileDescriptor;
            this.mPipeFromInCall = new InputStreamReader(new FileInputStream(parcelFileDescriptor2.getFileDescriptor()));
            this.mPipeToInCall = new OutputStreamWriter(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
        }

        public void write(String str) throws IOException {
            this.mPipeToInCall.write(str);
            this.mPipeToInCall.flush();
        }

        public String read() throws IOException {
            int i = this.mPipeFromInCall.read(this.mReadBuffer, 0, 1000);
            if (i < 0) {
                return null;
            }
            return new String(this.mReadBuffer, 0, i);
        }

        public String readImmediately() throws IOException {
            if (this.mPipeFromInCall.ready()) {
                return read();
            }
            return null;
        }

        public ParcelFileDescriptor getFdFromInCall() {
            return this.mFdFromInCall;
        }

        public ParcelFileDescriptor getFdToInCall() {
            return this.mFdToInCall;
        }
    }

    public static final class RttModifyStatus {
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE = 5;
        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        public static final int SESSION_MODIFY_REQUEST_TIMED_OUT = 4;

        private RttModifyStatus() {
        }
    }

    public static abstract class VideoProvider {
        private static final int MSG_ADD_VIDEO_CALLBACK = 1;
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
        private static final int MSG_SET_ZOOM = 6;
        public static final int SESSION_EVENT_CAMERA_FAILURE = 5;
        private static final String SESSION_EVENT_CAMERA_FAILURE_STR = "CAMERA_FAIL";
        public static final int SESSION_EVENT_CAMERA_PERMISSION_ERROR = 7;
        private static final String SESSION_EVENT_CAMERA_PERMISSION_ERROR_STR = "CAMERA_PERMISSION_ERROR";
        public static final int SESSION_EVENT_CAMERA_READY = 6;
        private static final String SESSION_EVENT_CAMERA_READY_STR = "CAMERA_READY";
        public static final int SESSION_EVENT_RX_PAUSE = 1;
        private static final String SESSION_EVENT_RX_PAUSE_STR = "RX_PAUSE";
        public static final int SESSION_EVENT_RX_RESUME = 2;
        private static final String SESSION_EVENT_RX_RESUME_STR = "RX_RESUME";
        public static final int SESSION_EVENT_TX_START = 3;
        private static final String SESSION_EVENT_TX_START_STR = "TX_START";
        public static final int SESSION_EVENT_TX_STOP = 4;
        private static final String SESSION_EVENT_TX_STOP_STR = "TX_STOP";
        private static final String SESSION_EVENT_UNKNOWN_STR = "UNKNOWN";
        public static final int SESSION_MODIFY_REQUEST_FAIL = 2;
        public static final int SESSION_MODIFY_REQUEST_INVALID = 3;
        public static final int SESSION_MODIFY_REQUEST_REJECTED_BY_REMOTE = 5;
        public static final int SESSION_MODIFY_REQUEST_SUCCESS = 1;
        public static final int SESSION_MODIFY_REQUEST_TIMED_OUT = 4;
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
            public void handleMessage(Message message) {
                SomeArgs someArgs;
                switch (message.what) {
                    case 1:
                        IBinder iBinder = (IBinder) message.obj;
                        IVideoCallback iVideoCallbackAsInterface = IVideoCallback.Stub.asInterface((IBinder) message.obj);
                        if (iVideoCallbackAsInterface != null) {
                            if (!VideoProvider.this.mVideoCallbacks.containsKey(iBinder)) {
                                VideoProvider.this.mVideoCallbacks.put(iBinder, iVideoCallbackAsInterface);
                                return;
                            } else {
                                Log.i(this, "addVideoProvider - skipped; already present.", new Object[0]);
                                return;
                            }
                        }
                        Log.w(this, "addVideoProvider - skipped; callback is null.", new Object[0]);
                        return;
                    case 2:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            VideoProvider.this.onSetCamera((String) someArgs.arg1);
                            VideoProvider.this.onSetCamera((String) someArgs.arg1, (String) someArgs.arg2, someArgs.argi1, someArgs.argi2, someArgs.argi3);
                            return;
                        } finally {
                        }
                    case 3:
                        VideoProvider.this.onSetPreviewSurface((Surface) message.obj);
                        return;
                    case 4:
                        VideoProvider.this.onSetDisplaySurface((Surface) message.obj);
                        return;
                    case 5:
                        VideoProvider.this.onSetDeviceOrientation(message.arg1);
                        return;
                    case 6:
                        VideoProvider.this.onSetZoom(((Float) message.obj).floatValue());
                        return;
                    case 7:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            VideoProvider.this.onSendSessionModifyRequest((VideoProfile) someArgs.arg1, (VideoProfile) someArgs.arg2);
                            return;
                        } finally {
                        }
                    case 8:
                        VideoProvider.this.onSendSessionModifyResponse((VideoProfile) message.obj);
                        return;
                    case 9:
                        VideoProvider.this.onRequestCameraCapabilities();
                        return;
                    case 10:
                        VideoProvider.this.onRequestConnectionDataUsage();
                        return;
                    case 11:
                        VideoProvider.this.onSetPauseImage((Uri) message.obj);
                        return;
                    case 12:
                        IBinder iBinder2 = (IBinder) message.obj;
                        IVideoCallback.Stub.asInterface((IBinder) message.obj);
                        if (VideoProvider.this.mVideoCallbacks.containsKey(iBinder2)) {
                            VideoProvider.this.mVideoCallbacks.remove(iBinder2);
                            return;
                        } else {
                            Log.i(this, "removeVideoProvider - skipped; not present.", new Object[0]);
                            return;
                        }
                    default:
                        return;
                }
            }
        }

        private final class VideoProviderBinder extends IVideoProvider.Stub {
            private VideoProviderBinder() {
            }

            @Override
            public void addVideoCallback(IBinder iBinder) {
                VideoProvider.this.mMessageHandler.obtainMessage(1, iBinder).sendToTarget();
            }

            @Override
            public void removeVideoCallback(IBinder iBinder) {
                VideoProvider.this.mMessageHandler.obtainMessage(12, iBinder).sendToTarget();
            }

            @Override
            public void setCamera(String str, String str2, int i) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = str2;
                someArgsObtain.argi1 = Binder.getCallingUid();
                someArgsObtain.argi2 = Binder.getCallingPid();
                someArgsObtain.argi3 = i;
                VideoProvider.this.mMessageHandler.obtainMessage(2, someArgsObtain).sendToTarget();
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
            public void setDeviceOrientation(int i) {
                VideoProvider.this.mMessageHandler.obtainMessage(5, i, 0).sendToTarget();
            }

            @Override
            public void setZoom(float f) {
                VideoProvider.this.mMessageHandler.obtainMessage(6, Float.valueOf(f)).sendToTarget();
            }

            @Override
            public void sendSessionModifyRequest(VideoProfile videoProfile, VideoProfile videoProfile2) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = videoProfile;
                someArgsObtain.arg2 = videoProfile2;
                VideoProvider.this.mMessageHandler.obtainMessage(7, someArgsObtain).sendToTarget();
            }

            @Override
            public void sendSessionModifyResponse(VideoProfile videoProfile) {
                VideoProvider.this.mMessageHandler.obtainMessage(8, videoProfile).sendToTarget();
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
            public void setPauseImage(Uri uri) {
                VideoProvider.this.mMessageHandler.obtainMessage(11, uri).sendToTarget();
            }
        }

        public VideoProvider() {
            this.mVideoCallbacks = new ConcurrentHashMap<>(8, 0.9f, 1);
            this.mBinder = new VideoProviderBinder();
            this.mMessageHandler = new VideoProviderHandler(Looper.getMainLooper());
        }

        public VideoProvider(Looper looper) {
            this.mVideoCallbacks = new ConcurrentHashMap<>(8, 0.9f, 1);
            this.mBinder = new VideoProviderBinder();
            this.mMessageHandler = new VideoProviderHandler(looper);
        }

        public final IVideoProvider getInterface() {
            return this.mBinder;
        }

        public void onSetCamera(String str, String str2, int i, int i2, int i3) {
        }

        public void receiveSessionModifyRequest(VideoProfile videoProfile) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().receiveSessionModifyRequest(videoProfile);
                    } catch (RemoteException e) {
                        Log.w(this, "receiveSessionModifyRequest callback failed", e);
                    }
                }
            }
        }

        public void receiveSessionModifyResponse(int i, VideoProfile videoProfile, VideoProfile videoProfile2) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().receiveSessionModifyResponse(i, videoProfile, videoProfile2);
                    } catch (RemoteException e) {
                        Log.w(this, "receiveSessionModifyResponse callback failed", e);
                    }
                }
            }
        }

        public void handleCallSessionEvent(int i) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().handleCallSessionEvent(i);
                    } catch (RemoteException e) {
                        Log.w(this, "handleCallSessionEvent callback failed", e);
                    }
                }
            }
        }

        public void changePeerDimensions(int i, int i2) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().changePeerDimensions(i, i2);
                    } catch (RemoteException e) {
                        Log.w(this, "changePeerDimensions callback failed", e);
                    }
                }
            }
        }

        public void setCallDataUsage(long j) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().changeCallDataUsage(j);
                    } catch (RemoteException e) {
                        Log.w(this, "setCallDataUsage callback failed", e);
                    }
                }
            }
        }

        public void changeCallDataUsage(long j) {
            setCallDataUsage(j);
        }

        public void changeCameraCapabilities(VideoProfile.CameraCapabilities cameraCapabilities) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().changeCameraCapabilities(cameraCapabilities);
                    } catch (RemoteException e) {
                        Log.w(this, "changeCameraCapabilities callback failed", e);
                    }
                }
            }
        }

        public void changeVideoQuality(int i) {
            if (this.mVideoCallbacks != null) {
                Iterator<IVideoCallback> it = this.mVideoCallbacks.values().iterator();
                while (it.hasNext()) {
                    try {
                        it.next().changeVideoQuality(i);
                    } catch (RemoteException e) {
                        Log.w(this, "changeVideoQuality callback failed", e);
                    }
                }
            }
        }

        public static String sessionEventToString(int i) {
            switch (i) {
                case 1:
                    return SESSION_EVENT_RX_PAUSE_STR;
                case 2:
                    return SESSION_EVENT_RX_RESUME_STR;
                case 3:
                    return SESSION_EVENT_TX_START_STR;
                case 4:
                    return SESSION_EVENT_TX_STOP_STR;
                case 5:
                    return SESSION_EVENT_CAMERA_FAILURE_STR;
                case 6:
                    return SESSION_EVENT_CAMERA_READY_STR;
                case 7:
                    return SESSION_EVENT_CAMERA_PERMISSION_ERROR_STR;
                default:
                    return "UNKNOWN " + i;
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

    @SystemApi
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

    public final long getConnectElapsedTimeMillis() {
        return this.mConnectElapsedTimeMillis;
    }

    public final StatusHints getStatusHints() {
        return this.mStatusHints;
    }

    public final Bundle getExtras() {
        Bundle bundle;
        synchronized (this.mExtrasLock) {
            if (this.mExtras != null) {
                bundle = new Bundle(this.mExtras);
            } else {
                bundle = null;
            }
        }
        return bundle;
    }

    public final Connection addConnectionListener(Listener listener) {
        this.mListeners.add(listener);
        return this;
    }

    public final Connection removeConnectionListener(Listener listener) {
        if (listener != null) {
            this.mListeners.remove(listener);
        }
        return this;
    }

    public final DisconnectCause getDisconnectCause() {
        return this.mDisconnectCause;
    }

    public void setTelecomCallId(String str) {
        this.mTelecomCallId = str;
    }

    final void setCallAudioState(CallAudioState callAudioState) {
        checkImmutable();
        Log.d(this, "setAudioState %s", callAudioState);
        this.mCallAudioState = callAudioState;
        onAudioStateChanged(getAudioState());
        onCallAudioStateChanged(callAudioState);
    }

    public static String stateToString(int i) {
        switch (i) {
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
            case 7:
                return "PULLING_CALL";
            default:
                Log.wtf(Connection.class, "Unknown state %d", Integer.valueOf(i));
                return IccCardConstants.INTENT_VALUE_ICC_UNKNOWN;
        }
    }

    public final int getConnectionCapabilities() {
        return this.mConnectionCapabilities;
    }

    public final int getConnectionProperties() {
        return this.mConnectionProperties;
    }

    public final int getSupportedAudioRoutes() {
        return this.mSupportedAudioRoutes;
    }

    public final void setAddress(Uri uri, int i) {
        checkImmutable();
        Log.d(this, "setAddress %s", uri);
        this.mAddress = uri;
        this.mAddressPresentation = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAddressChanged(this, uri, i);
        }
    }

    public final void setCallerDisplayName(String str, int i) {
        checkImmutable();
        Log.d(this, "setCallerDisplayName %s", str);
        this.mCallerDisplayName = str;
        this.mCallerDisplayNamePresentation = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCallerDisplayNameChanged(this, str, i);
        }
    }

    public final void setVideoState(int i) {
        checkImmutable();
        Log.d(this, "setVideoState %d", Integer.valueOf(i));
        this.mVideoState = i;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoStateChanged(this, this.mVideoState);
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

    public final void setPulling() {
        checkImmutable();
        setState(7);
    }

    public final void setOnHold() {
        checkImmutable();
        setState(5);
    }

    public final void setVideoProvider(VideoProvider videoProvider) {
        checkImmutable();
        this.mVideoProvider = videoProvider;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onVideoProviderChanged(this, videoProvider);
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
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDisconnected(this, disconnectCause);
        }
    }

    public final void setPostDialWait(String str) {
        checkImmutable();
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPostDialWait(this, str);
        }
    }

    public final void setNextPostDialChar(char c) {
        checkImmutable();
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPostDialChar(this, c);
        }
    }

    public final void setRingbackRequested(boolean z) {
        checkImmutable();
        if (this.mRingbackRequested != z) {
            this.mRingbackRequested = z;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onRingbackRequested(this, z);
            }
        }
    }

    public final void setConnectionCapabilities(int i) {
        checkImmutable();
        if (this.mConnectionCapabilities != i) {
            this.mConnectionCapabilities = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionCapabilitiesChanged(this, this.mConnectionCapabilities);
            }
        }
    }

    public final void setConnectionProperties(int i) {
        checkImmutable();
        if (this.mConnectionProperties != i) {
            this.mConnectionProperties = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onConnectionPropertiesChanged(this, this.mConnectionProperties);
            }
        }
    }

    public final void setSupportedAudioRoutes(int i) {
        if ((i & 9) == 0) {
            throw new IllegalArgumentException("supported audio routes must include either speaker or earpiece");
        }
        if (this.mSupportedAudioRoutes != i) {
            this.mSupportedAudioRoutes = i;
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onSupportedAudioRoutesChanged(this, this.mSupportedAudioRoutes);
            }
        }
    }

    public final void destroy() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onDestroyed(this);
        }
    }

    public final void setAudioModeIsVoip(boolean z) {
        checkImmutable();
        this.mAudioModeIsVoip = z;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAudioModeIsVoipChanged(this, z);
        }
    }

    public final void setConnectTimeMillis(long j) {
        this.mConnectTimeMillis = j;
    }

    public final void setConnectionStartElapsedRealTime(long j) {
        this.mConnectElapsedTimeMillis = j;
    }

    public final void setStatusHints(StatusHints statusHints) {
        checkImmutable();
        this.mStatusHints = statusHints;
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onStatusHintsChanged(this, statusHints);
        }
    }

    public final void setConferenceableConnections(List<Connection> list) {
        checkImmutable();
        clearConferenceableList();
        for (Connection connection : list) {
            if (!this.mConferenceables.contains(connection)) {
                connection.addConnectionListener(this.mConnectionDeathListener);
                this.mConferenceables.add(connection);
            }
        }
        fireOnConferenceableConnectionsChanged();
    }

    public final void setConferenceables(List<Conferenceable> list) {
        clearConferenceableList();
        for (Conferenceable conferenceable : list) {
            if (!this.mConferenceables.contains(conferenceable)) {
                if (conferenceable instanceof Connection) {
                    ((Connection) conferenceable).addConnectionListener(this.mConnectionDeathListener);
                } else if (conferenceable instanceof Conference) {
                    ((Conference) conferenceable).addListener(this.mConferenceDeathListener);
                }
                this.mConferenceables.add(conferenceable);
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
        if (this.mConference != null) {
            Log.d(this, "Conference reset", new Object[0]);
            this.mConference = null;
            fireConferenceChanged();
        }
    }

    public final void setExtras(Bundle bundle) {
        checkImmutable();
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

    public final void putExtras(Bundle bundle) {
        Bundle bundle2;
        checkImmutable();
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

    public final void setAudioRoute(int i) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAudioRouteChanged(this, i, null);
        }
    }

    public void requestBluetoothAudio(BluetoothDevice bluetoothDevice) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onAudioRouteChanged(this, 2, bluetoothDevice.getAddress());
        }
    }

    public final void sendRttInitiationSuccess() {
        this.mListeners.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Connection.Listener) obj).onRttInitiationSuccess(this.f$0);
            }
        });
    }

    public final void sendRttInitiationFailure(final int i) {
        this.mListeners.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Connection.Listener) obj).onRttInitiationFailure(this.f$0, i);
            }
        });
    }

    public final void sendRttSessionRemotelyTerminated() {
        this.mListeners.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Connection.Listener) obj).onRttSessionRemotelyTerminated(this.f$0);
            }
        });
    }

    public final void sendRemoteRttRequest() {
        this.mListeners.forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((Connection.Listener) obj).onRemoteRttRequest(this.f$0);
            }
        });
    }

    @SystemApi
    @Deprecated
    public void onAudioStateChanged(AudioState audioState) {
    }

    public void onCallAudioStateChanged(CallAudioState callAudioState) {
    }

    public void onStateChanged(int i) {
    }

    public void onPlayDtmfTone(char c) {
    }

    public void onStopDtmfTone() {
    }

    public void onDisconnect() {
    }

    public void onDisconnectConferenceParticipant(Uri uri) {
    }

    public void onSeparate() {
    }

    public void onAbort() {
    }

    public void onHold() {
    }

    public void onUnhold() {
    }

    public void onAnswer(int i) {
    }

    public void onAnswer() {
        onAnswer(0);
    }

    public void onDeflect(Uri uri) {
    }

    public void onReject() {
    }

    public void onReject(String str) {
    }

    public void onSilence() {
    }

    public void onPostDialContinue(boolean z) {
    }

    public void onPullExternalCall() {
    }

    public void onCallEvent(String str, Bundle bundle) {
    }

    public void onHandoverComplete() {
    }

    public void onExtrasChanged(Bundle bundle) {
    }

    public void onShowIncomingCallUi() {
    }

    public void onStartRtt(RttTextStream rttTextStream) {
    }

    public void onStopRtt() {
    }

    public void handleRttUpgradeResponse(RttTextStream rttTextStream) {
    }

    public static String toLogSafePhoneNumber(String str) {
        if (str == null) {
            return "";
        }
        if (PII_DEBUG) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '-' || cCharAt == '@' || cCharAt == '.') {
                sb.append(cCharAt);
            } else {
                sb.append(EpicenterTranslateClipReveal.StateProperty.TARGET_X);
            }
        }
        return sb.toString();
    }

    private void setState(int i) {
        checkImmutable();
        if (this.mState == 6 && this.mState != i) {
            Log.d(this, "Connection already DISCONNECTED; cannot transition out of this state.", new Object[0]);
            return;
        }
        if (this.mState != i) {
            Log.d(this, "setState: %s", stateToString(i));
            this.mState = i;
            onStateChanged(i);
            Iterator<Listener> it = this.mListeners.iterator();
            while (it.hasNext()) {
                it.next().onStateChanged(this, i);
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
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceablesChanged(this, getConferenceables());
        }
    }

    private final void fireConferenceChanged() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceChanged(this, this.mConference);
        }
    }

    private final void clearConferenceableList() {
        for (Conferenceable conferenceable : this.mConferenceables) {
            if (conferenceable instanceof Connection) {
                ((Connection) conferenceable).removeConnectionListener(this.mConnectionDeathListener);
            } else if (conferenceable instanceof Conference) {
                ((Conference) conferenceable).removeListener(this.mConferenceDeathListener);
            }
        }
        this.mConferenceables.clear();
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

    protected final void notifyConferenceMergeFailed() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceMergeFailed(this);
        }
    }

    protected final void updateConferenceParticipants(List<ConferenceParticipant> list) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceParticipantsChanged(this, list);
        }
    }

    protected void notifyConferenceStarted() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceStarted();
        }
    }

    protected void notifyConferenceSupportedChanged(boolean z) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConferenceSupportedChanged(this, z);
        }
    }

    public void notifyPhoneAccountChanged(PhoneAccountHandle phoneAccountHandle) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPhoneAccountChanged(this, phoneAccountHandle);
        }
    }

    public void setPhoneAccountHandle(PhoneAccountHandle phoneAccountHandle) {
        if (this.mPhoneAccountHandle != phoneAccountHandle) {
            this.mPhoneAccountHandle = phoneAccountHandle;
            notifyPhoneAccountChanged(phoneAccountHandle);
        }
    }

    public PhoneAccountHandle getPhoneAccountHandle() {
        return this.mPhoneAccountHandle;
    }

    public void sendConnectionEvent(String str, Bundle bundle) {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onConnectionEvent(this, str, bundle);
        }
    }
}
