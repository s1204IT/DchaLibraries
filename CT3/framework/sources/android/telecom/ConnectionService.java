package android.telecom;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.telecom.Conference;
import android.telecom.Connection;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IConnectionService;
import com.android.internal.telecom.IConnectionServiceAdapter;
import com.android.internal.telecom.RemoteServiceCallback;
import com.mediatek.telecom.FormattedLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ConnectionService extends Service {
    private static final int MSG_ABORT = 3;
    private static final int MSG_ADD_CONNECTION_SERVICE_ADAPTER = 1;
    private static final int MSG_ANSWER = 4;
    private static final int MSG_ANSWER_VIDEO = 17;
    private static final int MSG_BLIND_ASSURED_ECT = 1005;
    private static final int MSG_CONFERENCE = 12;
    private static final int MSG_CREATE_CONFERENCE = 1003;
    private static final int MSG_CREATE_CONNECTION = 2;
    private static final int MSG_DISCONNECT = 6;
    private static final int MSG_ECT = 1000;
    private static final int MSG_HANDLE_ORDERED_USER_OPERATION = 1004;
    private static final int MSG_HANGUP_ALL = 1001;
    private static final int MSG_HOLD = 7;
    private static final int MSG_INVITE_CONFERENCE_PARTICIPANTS = 1002;
    private static final int MSG_MERGE_CONFERENCE = 18;
    private static final int MSG_ON_CALL_AUDIO_STATE_CHANGED = 9;
    private static final int MSG_ON_EXTRAS_CHANGED = 24;
    private static final int MSG_ON_POST_DIAL_CONTINUE = 14;
    private static final int MSG_PLAY_DTMF_TONE = 10;
    private static final int MSG_PULL_EXTERNAL_CALL = 22;
    private static final int MSG_REJECT = 5;
    private static final int MSG_REJECT_WITH_MESSAGE = 20;
    private static final int MSG_REMOVE_CONNECTION_SERVICE_ADAPTER = 16;
    private static final int MSG_SEND_CALL_EVENT = 23;
    private static final int MSG_SILENCE = 21;
    private static final int MSG_SPLIT_FROM_CONFERENCE = 13;
    private static final int MSG_STOP_DTMF_TONE = 11;
    private static final int MSG_SWAP_CONFERENCE = 19;
    private static final int MSG_UNHOLD = 8;
    private static final int MTK_MSG_BASE = 1000;
    private static final boolean PII_DEBUG = Log.isLoggable(3);
    public static final String SERVICE_INTERFACE = "android.telecom.ConnectionService";
    private static Connection sNullConnection;
    private Conference sNullConference;
    private final Map<String, Connection> mConnectionById = new ConcurrentHashMap();
    private final Map<Connection, String> mIdByConnection = new ConcurrentHashMap();
    private final Map<String, Conference> mConferenceById = new ConcurrentHashMap();
    private final Map<Conference, String> mIdByConference = new ConcurrentHashMap();
    private final RemoteConnectionManager mRemoteConnectionManager = new RemoteConnectionManager(this);
    private final List<Runnable> mPreInitializationConnectionRequests = new ArrayList();
    private final ConnectionServiceAdapter mAdapter = new ConnectionServiceAdapter();
    private boolean mAreAccountsInitialized = false;
    private Object mIdSyncRoot = new Object();
    private int mId = 0;
    private final IBinder mBinder = new IConnectionService.Stub() {
        public void addConnectionServiceAdapter(IConnectionServiceAdapter adapter) {
            ConnectionService.this.mHandler.obtainMessage(1, adapter).sendToTarget();
        }

        public void removeConnectionServiceAdapter(IConnectionServiceAdapter adapter) {
            ConnectionService.this.mHandler.obtainMessage(16, adapter).sendToTarget();
        }

        public void createConnection(PhoneAccountHandle connectionManagerPhoneAccount, String id, ConnectionRequest request, boolean isIncoming, boolean isUnknown) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = connectionManagerPhoneAccount;
            args.arg2 = id;
            args.arg3 = request;
            args.argi1 = isIncoming ? 1 : 0;
            args.argi2 = isUnknown ? 1 : 0;
            ConnectionService.this.mHandler.obtainMessage(2, args).sendToTarget();
        }

        public void abort(String callId) {
            ConnectionService.this.mHandler.obtainMessage(3, callId).sendToTarget();
        }

        public void answerVideo(String callId, int videoState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.argi1 = videoState;
            ConnectionService.this.mHandler.obtainMessage(17, args).sendToTarget();
        }

        public void answer(String callId) {
            ConnectionService.this.mHandler.obtainMessage(4, callId).sendToTarget();
        }

        public void reject(String callId) {
            ConnectionService.this.mHandler.obtainMessage(5, callId).sendToTarget();
        }

        public void rejectWithMessage(String callId, String message) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = message;
            ConnectionService.this.mHandler.obtainMessage(20, args).sendToTarget();
        }

        public void silence(String callId) {
            ConnectionService.this.mHandler.obtainMessage(21, callId).sendToTarget();
        }

        public void disconnect(String callId) {
            ConnectionService.this.mHandler.obtainMessage(6, callId).sendToTarget();
        }

        public void hold(String callId) {
            ConnectionService.this.mHandler.obtainMessage(7, callId).sendToTarget();
        }

        public void unhold(String callId) {
            ConnectionService.this.mHandler.obtainMessage(8, callId).sendToTarget();
        }

        public void onCallAudioStateChanged(String callId, CallAudioState callAudioState) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = callAudioState;
            ConnectionService.this.mHandler.obtainMessage(9, args).sendToTarget();
        }

        public void playDtmfTone(String callId, char digit) {
            ConnectionService.this.mHandler.obtainMessage(10, digit, 0, callId).sendToTarget();
        }

        public void stopDtmfTone(String callId) {
            ConnectionService.this.mHandler.obtainMessage(11, callId).sendToTarget();
        }

        public void conference(String callId1, String callId2) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId1;
            args.arg2 = callId2;
            ConnectionService.this.mHandler.obtainMessage(12, args).sendToTarget();
        }

        public void splitFromConference(String callId) {
            ConnectionService.this.mHandler.obtainMessage(13, callId).sendToTarget();
        }

        public void mergeConference(String callId) {
            ConnectionService.this.mHandler.obtainMessage(18, callId).sendToTarget();
        }

        public void swapConference(String callId) {
            ConnectionService.this.mHandler.obtainMessage(19, callId).sendToTarget();
        }

        public void onPostDialContinue(String callId, boolean proceed) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.argi1 = proceed ? 1 : 0;
            ConnectionService.this.mHandler.obtainMessage(14, args).sendToTarget();
        }

        public void pullExternalCall(String callId) {
            ConnectionService.this.mHandler.obtainMessage(22, callId).sendToTarget();
        }

        public void sendCallEvent(String callId, String event, Bundle extras) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = event;
            args.arg3 = extras;
            ConnectionService.this.mHandler.obtainMessage(23, args).sendToTarget();
        }

        public void onExtrasChanged(String callId, Bundle extras) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = extras;
            ConnectionService.this.mHandler.obtainMessage(24, args).sendToTarget();
        }

        public void explicitCallTransfer(String callId) {
            ConnectionService.this.mHandler.obtainMessage(1000, callId).sendToTarget();
        }

        public void blindAssuredEct(String callId, String number, int type) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = number;
            args.argi1 = type;
            ConnectionService.this.mHandler.obtainMessage(1005, args).sendToTarget();
        }

        public void hangupAll(String callId) {
            ConnectionService.this.mHandler.obtainMessage(1001, callId).sendToTarget();
        }

        public void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = conferenceCallId;
            args.arg2 = numbers;
            ConnectionService.this.mHandler.obtainMessage(1002, args).sendToTarget();
        }

        public void createConference(PhoneAccountHandle connectionManagerPhoneAccount, String conferenceCallId, ConnectionRequest request, List<String> numbers, boolean isIncoming) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = connectionManagerPhoneAccount;
            args.arg2 = conferenceCallId;
            args.arg3 = request;
            args.arg4 = numbers;
            args.argi1 = isIncoming ? 1 : 0;
            ConnectionService.this.mHandler.obtainMessage(1003, args).sendToTarget();
        }

        public void handleOrderedOperation(String callId, String currentOperation, String pendingOperation) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = currentOperation;
            args.arg3 = pendingOperation;
            ConnectionService.this.mHandler.obtainMessage(1004, args).sendToTarget();
        }
    };
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            SomeArgs args;
            switch (msg.what) {
                case 1:
                    ConnectionService.this.mAdapter.addAdapter((IConnectionServiceAdapter) msg.obj);
                    ConnectionService.this.onAdapterAttached();
                    return;
                case 2:
                    args = (SomeArgs) msg.obj;
                    try {
                        final PhoneAccountHandle connectionManagerPhoneAccount = (PhoneAccountHandle) args.arg1;
                        final String id = (String) args.arg2;
                        final ConnectionRequest request = (ConnectionRequest) args.arg3;
                        final boolean isIncoming = args.argi1 == 1;
                        final boolean isUnknown = args.argi2 == 1;
                        if (!ConnectionService.this.mAreAccountsInitialized) {
                            Log.d(this, "Enqueueing pre-init request %s", id);
                            ConnectionService.this.mPreInitializationConnectionRequests.add(new Runnable() {
                                @Override
                                public void run() {
                                    ConnectionService.this.createConnection(connectionManagerPhoneAccount, id, request, isIncoming, isUnknown);
                                }
                            });
                        } else {
                            ConnectionService.this.createConnection(connectionManagerPhoneAccount, id, request, isIncoming, isUnknown);
                        }
                        return;
                    } finally {
                    }
                case 3:
                    ConnectionService.this.abort((String) msg.obj);
                    return;
                case 4:
                    ConnectionService.this.answer((String) msg.obj);
                    return;
                case 5:
                    ConnectionService.this.reject((String) msg.obj);
                    return;
                case 6:
                    ConnectionService.this.disconnect((String) msg.obj);
                    return;
                case 7:
                    ConnectionService.this.hold((String) msg.obj);
                    return;
                case 8:
                    ConnectionService.this.unhold((String) msg.obj);
                    return;
                case 9:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId = (String) args.arg1;
                        CallAudioState audioState = (CallAudioState) args.arg2;
                        ConnectionService.this.onCallAudioStateChanged(callId, new CallAudioState(audioState));
                        return;
                    } finally {
                    }
                case 10:
                    ConnectionService.this.playDtmfTone((String) msg.obj, (char) msg.arg1);
                    return;
                case 11:
                    ConnectionService.this.stopDtmfTone((String) msg.obj);
                    return;
                case 12:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId1 = (String) args.arg1;
                        String callId2 = (String) args.arg2;
                        ConnectionService.this.conference(callId1, callId2);
                        return;
                    } finally {
                    }
                case 13:
                    ConnectionService.this.splitFromConference((String) msg.obj);
                    return;
                case 14:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId3 = (String) args.arg1;
                        boolean proceed = args.argi1 == 1;
                        ConnectionService.this.onPostDialContinue(callId3, proceed);
                        return;
                    } finally {
                    }
                case 16:
                    ConnectionService.this.mAdapter.removeAdapter((IConnectionServiceAdapter) msg.obj);
                    return;
                case 17:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId4 = (String) args.arg1;
                        int videoState = args.argi1;
                        ConnectionService.this.answerVideo(callId4, videoState);
                        return;
                    } finally {
                    }
                case 18:
                    ConnectionService.this.mergeConference((String) msg.obj);
                    return;
                case 19:
                    ConnectionService.this.swapConference((String) msg.obj);
                    return;
                case 20:
                    args = (SomeArgs) msg.obj;
                    try {
                        ConnectionService.this.reject((String) args.arg1, (String) args.arg2);
                        return;
                    } finally {
                    }
                case 21:
                    ConnectionService.this.silence((String) msg.obj);
                    return;
                case 22:
                    ConnectionService.this.pullExternalCall((String) msg.obj);
                    return;
                case 23:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId5 = (String) args.arg1;
                        String event = (String) args.arg2;
                        Bundle extras = (Bundle) args.arg3;
                        ConnectionService.this.sendCallEvent(callId5, event, extras);
                        return;
                    } finally {
                    }
                case 24:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId6 = (String) args.arg1;
                        Bundle extras2 = (Bundle) args.arg2;
                        ConnectionService.this.handleExtrasChanged(callId6, extras2);
                        return;
                    } finally {
                    }
                case 1000:
                    ConnectionService.this.explicitCallTransfer((String) msg.obj);
                    return;
                case 1001:
                    ConnectionService.this.hangupAll((String) msg.obj);
                    return;
                case 1002:
                    args = (SomeArgs) msg.obj;
                    try {
                        ConnectionService.this.inviteConferenceParticipants((String) args.arg1, (List) args.arg2);
                        return;
                    } finally {
                    }
                case 1003:
                    args = (SomeArgs) msg.obj;
                    try {
                        final PhoneAccountHandle connectionManagerPhoneAccount2 = (PhoneAccountHandle) args.arg1;
                        final String conferenceCallId = (String) args.arg2;
                        final ConnectionRequest request2 = (ConnectionRequest) args.arg3;
                        final List<String> numbers = (List) args.arg4;
                        boolean isIncoming2 = args.argi1 == 1;
                        if (!ConnectionService.this.mAreAccountsInitialized) {
                            Log.d(this, "Enqueueing pre-init request %s", conferenceCallId);
                            final boolean z = isIncoming2;
                            ConnectionService.this.mPreInitializationConnectionRequests.add(new Runnable() {
                                @Override
                                public void run() {
                                    ConnectionService.this.createConference(connectionManagerPhoneAccount2, conferenceCallId, request2, numbers, z);
                                }
                            });
                        } else {
                            ConnectionService.this.createConference(connectionManagerPhoneAccount2, conferenceCallId, request2, numbers, isIncoming2);
                        }
                        return;
                    } finally {
                    }
                case 1004:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId7 = (String) args.arg1;
                        String currentOperation = (String) args.arg2;
                        String pendingOperation = (String) args.arg3;
                        if ("disconnect".equals(currentOperation)) {
                            ConnectionService.this.disconnect(callId7, pendingOperation);
                            break;
                        }
                        return;
                    } finally {
                    }
                case 1005:
                    args = (SomeArgs) msg.obj;
                    try {
                        String callId8 = (String) args.arg1;
                        String number = (String) args.arg2;
                        int type = args.argi1;
                        ConnectionService.this.explicitCallTransfer(callId8, number, type);
                        return;
                    } finally {
                    }
                default:
                    return;
            }
        }
    };
    private final Conference.Listener mConferenceListener = new Conference.Listener() {
        @Override
        public void onStateChanged(Conference conference, int oldState, int newState) {
            String id = (String) ConnectionService.this.mIdByConference.get(conference);
            switch (newState) {
                case 4:
                    ConnectionService.this.mAdapter.setActive(id);
                    break;
                case 5:
                    ConnectionService.this.mAdapter.setOnHold(id);
                    break;
            }
        }

        @Override
        public void onDisconnected(Conference conference, DisconnectCause disconnectCause) {
            String id = (String) ConnectionService.this.mIdByConference.get(conference);
            ConnectionService.this.mAdapter.setDisconnected(id, disconnectCause);
        }

        @Override
        public void onConnectionAdded(Conference conference, Connection connection) {
        }

        @Override
        public void onConnectionRemoved(Conference conference, Connection connection) {
        }

        @Override
        public void onConferenceableConnectionsChanged(Conference conference, List<Connection> conferenceableConnections) {
            ConnectionService.this.mAdapter.setConferenceableConnections((String) ConnectionService.this.mIdByConference.get(conference), ConnectionService.this.createConnectionIdList(conferenceableConnections));
        }

        @Override
        public void onDestroyed(Conference conference) {
            ConnectionService.this.removeConference(conference);
        }

        @Override
        public void onConnectionCapabilitiesChanged(Conference conference, int connectionCapabilities) {
            String id = (String) ConnectionService.this.mIdByConference.get(conference);
            Log.d(this, "call capabilities: conference: %s", Connection.capabilitiesToString(connectionCapabilities));
            ConnectionService.this.mAdapter.setConnectionCapabilities(id, connectionCapabilities);
        }

        @Override
        public void onConnectionPropertiesChanged(Conference conference, int connectionProperties) {
            String id = (String) ConnectionService.this.mIdByConference.get(conference);
            Log.d(this, "call capabilities: conference: %s", Connection.propertiesToString(connectionProperties));
            ConnectionService.this.mAdapter.setConnectionProperties(id, connectionProperties);
        }

        @Override
        public void onVideoStateChanged(Conference c, int videoState) {
            String id = (String) ConnectionService.this.mIdByConference.get(c);
            Log.d(this, "onVideoStateChanged set video state %d", Integer.valueOf(videoState));
            ConnectionService.this.mAdapter.setVideoState(id, videoState);
        }

        @Override
        public void onVideoProviderChanged(Conference c, Connection.VideoProvider videoProvider) {
            String id = (String) ConnectionService.this.mIdByConference.get(c);
            Log.d(this, "onVideoProviderChanged: Connection: %s, VideoProvider: %s", c, videoProvider);
            ConnectionService.this.mAdapter.setVideoProvider(id, videoProvider);
        }

        @Override
        public void onStatusHintsChanged(Conference conference, StatusHints statusHints) {
            String id = (String) ConnectionService.this.mIdByConference.get(conference);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.setStatusHints(id, statusHints);
        }

        @Override
        public void onExtrasChanged(Conference c, Bundle extras) {
            String id = (String) ConnectionService.this.mIdByConference.get(c);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.putExtras(id, extras);
        }

        @Override
        public void onExtrasRemoved(Conference c, List<String> keys) {
            String id = (String) ConnectionService.this.mIdByConference.get(c);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.removeExtras(id, keys);
        }
    };
    private final Connection.Listener mConnectionListener = new Connection.Listener() {
        @Override
        public void onStateChanged(Connection c, int state) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter set state %s %s", id, Connection.stateToString(state));
            switch (state) {
                case 2:
                    ConnectionService.this.mAdapter.setRinging(id);
                    break;
                case 3:
                    ConnectionService.this.mAdapter.setDialing(id);
                    break;
                case 4:
                    ConnectionService.this.mAdapter.setActive(id);
                    break;
                case 5:
                    ConnectionService.this.mAdapter.setOnHold(id);
                    break;
            }
        }

        @Override
        public void onDisconnected(Connection c, DisconnectCause disconnectCause) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter set disconnected %s", disconnectCause);
            ConnectionService.this.mAdapter.setDisconnected(id, disconnectCause);
        }

        @Override
        public void onVideoStateChanged(Connection c, int videoState) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter set video state %d", Integer.valueOf(videoState));
            ConnectionService.this.mAdapter.setVideoState(id, videoState);
        }

        @Override
        public void onAddressChanged(Connection c, Uri address, int presentation) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            ConnectionService.this.mAdapter.setAddress(id, address, presentation);
        }

        @Override
        public void onCallerDisplayNameChanged(Connection c, String callerDisplayName, int presentation) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            ConnectionService.this.mAdapter.setCallerDisplayName(id, callerDisplayName, presentation);
        }

        @Override
        public void onDestroyed(Connection c) {
            ConnectionService.this.removeConnection(c);
        }

        @Override
        public void onPostDialWait(Connection c, String remaining) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter onPostDialWait %s, %s", c, remaining);
            ConnectionService.this.mAdapter.onPostDialWait(id, remaining);
        }

        @Override
        public void onPostDialChar(Connection c, char nextChar) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter onPostDialChar %s, %s", c, Character.valueOf(nextChar));
            ConnectionService.this.mAdapter.onPostDialChar(id, nextChar);
        }

        @Override
        public void onRingbackRequested(Connection c, boolean ringback) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "Adapter onRingback %b", Boolean.valueOf(ringback));
            ConnectionService.this.mAdapter.setRingbackRequested(id, ringback);
        }

        @Override
        public void onConnectionCapabilitiesChanged(Connection c, int capabilities) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "capabilities: parcelableconnection: %s", Connection.capabilitiesToString(capabilities));
            ConnectionService.this.mAdapter.setConnectionCapabilities(id, capabilities);
        }

        @Override
        public void onConnectionPropertiesChanged(Connection c, int properties) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "properties: parcelableconnection: %s", Connection.propertiesToString(properties));
            ConnectionService.this.mAdapter.setConnectionProperties(id, properties);
        }

        @Override
        public void onVideoProviderChanged(Connection c, Connection.VideoProvider videoProvider) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            Log.d(this, "onVideoProviderChanged: Connection: %s, VideoProvider: %s", c, videoProvider);
            ConnectionService.this.mAdapter.setVideoProvider(id, videoProvider);
        }

        @Override
        public void onAudioModeIsVoipChanged(Connection c, boolean isVoip) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            ConnectionService.this.mAdapter.setIsVoipAudioMode(id, isVoip);
        }

        @Override
        public void onStatusHintsChanged(Connection c, StatusHints statusHints) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            ConnectionService.this.mAdapter.setStatusHints(id, statusHints);
        }

        @Override
        public void onConferenceablesChanged(Connection connection, List<Conferenceable> conferenceables) {
            ConnectionService.this.mAdapter.setConferenceableConnections((String) ConnectionService.this.mIdByConnection.get(connection), ConnectionService.this.createIdList(conferenceables));
        }

        @Override
        public void onConferenceChanged(Connection connection, Conference conference) {
            String id = (String) ConnectionService.this.mIdByConnection.get(connection);
            if (id == null) {
                return;
            }
            String conferenceId = null;
            if (conference != null) {
                conferenceId = (String) ConnectionService.this.mIdByConference.get(conference);
            }
            ConnectionService.this.mAdapter.setIsConferenced(id, conferenceId);
        }

        @Override
        public void onConferenceMergeFailed(Connection connection) {
            String id = (String) ConnectionService.this.mIdByConnection.get(connection);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.onConferenceMergeFailed(id);
        }

        @Override
        public void onExtrasChanged(Connection c, Bundle extras) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.putExtras(id, extras);
        }

        @Override
        public void onExtrasRemoved(Connection c, List<String> keys) {
            String id = (String) ConnectionService.this.mIdByConnection.get(c);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.removeExtras(id, keys);
        }

        @Override
        public void onConnectionEvent(Connection connection, String event, Bundle extras) {
            String id = (String) ConnectionService.this.mIdByConnection.get(connection);
            if (id == null) {
                return;
            }
            ConnectionService.this.mAdapter.onConnectionEvent(id, event, extras);
        }
    };

    @Override
    public final IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        endAllConnections();
        return super.onUnbind(intent);
    }

    private void createConnection(PhoneAccountHandle callManagerAccount, String callId, ConnectionRequest request, boolean isIncoming, boolean isUnknown) {
        Log.d(this, "createConnection, callManagerAccount: %s, callId: %s, request: %s, isIncoming: %b, isUnknown: %b", callManagerAccount, callId, request, Boolean.valueOf(isIncoming), Boolean.valueOf(isUnknown));
        if (!isIncoming) {
            String callNumber = null;
            if (request != null && request.getAddress() != null) {
                callNumber = request.getAddress().getSchemeSpecificPart();
            }
            FormattedLog formattedLog = new FormattedLog.Builder().setCategory("CC").setServiceName(getConnectionServiceName()).setOpType(FormattedLog.OpType.OPERATION).setActionName("Dial").setCallNumber(callNumber).setCallId(ProxyInfo.LOCAL_EXCL_LIST).buildDebugMsg();
            if (formattedLog != null) {
                Log.d(this, formattedLog.toString(), new Object[0]);
            }
        }
        Connection connection = isUnknown ? onCreateUnknownConnection(callManagerAccount, request) : isIncoming ? onCreateIncomingConnection(callManagerAccount, request) : onCreateOutgoingConnection(callManagerAccount, request);
        Log.d(this, "createConnection, connection: %s", connection);
        if (connection == null) {
            connection = Connection.createFailedConnection(new DisconnectCause(1));
        }
        connection.setTelecomCallId(callId);
        if (connection.getState() != 6) {
            addConnection(callId, connection);
        }
        Uri address = connection.getAddress();
        String number = address == null ? "null" : address.getSchemeSpecificPart();
        Log.v(this, "createConnection, number: %s, state: %s, capabilities: %s, properties: %s", Connection.toLogSafePhoneNumber(number), Connection.stateToString(connection.getState()), Connection.capabilitiesToString(connection.getConnectionCapabilities()), Connection.propertiesToString(connection.getConnectionProperties()));
        Log.d(this, "createConnection, calling handleCreateConnectionSuccessful %s", callId);
        PhoneAccountHandle handle = connection.getAccountHandle();
        if (handle == null) {
            handle = request.getAccountHandle();
        } else {
            Log.d(this, "createConnection, set back phone account:%s", handle);
        }
        this.mAdapter.handleCreateConnectionComplete(callId, request, new ParcelableConnection(handle, connection.getState(), connection.getConnectionCapabilities(), connection.getConnectionProperties(), connection.getAddress(), connection.getAddressPresentation(), connection.getCallerDisplayName(), connection.getCallerDisplayNamePresentation(), connection.getVideoProvider() == null ? null : connection.getVideoProvider().getInterface(), connection.getVideoState(), connection.isRingbackRequested(), connection.getAudioModeIsVoip(), connection.getConnectTimeMillis(), connection.getStatusHints(), connection.getDisconnectCause(), createIdList(connection.getConferenceables()), connection.getExtras()));
        if (isUnknown) {
            triggerConferenceRecalculate();
        }
        if (connection.getState() != 6) {
            forceSuppMessageUpdate(connection);
        }
    }

    public void createConnectionInternal(String callId, ConnectionRequest request) {
        Log.d(this, "createConnectionInternal, callId: %s, request: %s", callId, request);
        Connection connection = onCreateOutgoingConnection(null, request);
        Log.d(this, "createConnectionInternal, connection: %s", connection);
        if (connection == null) {
            connection = Connection.createFailedConnection(new DisconnectCause(1));
        }
        connection.setTelecomCallId(callId);
        if (connection.getState() != 6) {
            addConnection(callId, connection);
        }
        Uri address = connection.getAddress();
        String number = address == null ? "null" : address.getSchemeSpecificPart();
        Log.v(this, "createConnectionInternal, number:%s, state:%s, capabilities:%s, properties:%s", Connection.toLogSafePhoneNumber(number), Connection.stateToString(connection.getState()), Connection.capabilitiesToString(connection.getConnectionCapabilities()), Connection.propertiesToString(connection.getConnectionProperties()));
        Log.d(this, "createConnectionInternal, calling handleCreateConnectionComplete %s", callId);
        PhoneAccountHandle handle = connection.getAccountHandle();
        if (handle == null) {
            handle = request.getAccountHandle();
        }
        this.mAdapter.handleCreateConnectionComplete(callId, request, new ParcelableConnection(handle, connection.getState(), connection.getConnectionCapabilities(), connection.getConnectionProperties(), connection.getAddress(), connection.getAddressPresentation(), connection.getCallerDisplayName(), connection.getCallerDisplayNamePresentation(), connection.getVideoProvider() == null ? null : connection.getVideoProvider().getInterface(), connection.getVideoState(), connection.isRingbackRequested(), connection.getAudioModeIsVoip(), connection.getConnectTimeMillis(), connection.getStatusHints(), connection.getDisconnectCause(), createIdList(connection.getConferenceables()), connection.getExtras()));
    }

    private void abort(String callId) {
        Log.d(this, "abort %s", callId);
        findConnectionForAction(callId, "abort").onAbort();
    }

    private void answerVideo(String callId, int videoState) {
        Log.d(this, "answerVideo %s", callId);
        findConnectionForAction(callId, "answer").onAnswer(videoState);
    }

    private void answer(String callId) {
        logDebugMsgWithOpFormat("CC", "Answer", callId, null);
        Log.d(this, "answer %s", callId);
        findConnectionForAction(callId, "answer").onAnswer();
    }

    private void reject(String callId) {
        logDebugMsgWithOpFormat("CC", "Reject", callId, null);
        Log.d(this, "reject %s", callId);
        findConnectionForAction(callId, "reject").onReject();
    }

    private void reject(String callId, String rejectWithMessage) {
        Log.d(this, "reject %s with message", callId);
        findConnectionForAction(callId, "reject").onReject(rejectWithMessage);
    }

    private void silence(String callId) {
        Log.d(this, "silence %s", callId);
        findConnectionForAction(callId, "silence").onSilence();
    }

    private void disconnect(String callId) {
        if (this.mConnectionById.containsKey(callId) && this.mConnectionById.get(callId) != null && this.mConnectionById.get(callId).getConference() != null) {
            logDebugMsgWithOpFormat("CC", "RemoveMember", callId, null);
        } else {
            logDebugMsgWithOpFormat("CC", "Hangup", callId, null);
        }
        Log.d(this, "disconnect %s", callId);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "disconnect").onDisconnect();
        } else {
            findConferenceForAction(callId, "disconnect").onDisconnect();
        }
    }

    private void hold(String callId) {
        logDebugMsgWithOpFormat("CC", "Hold", callId, null);
        Log.d(this, "hold %s", callId);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "hold").onHold();
        } else {
            findConferenceForAction(callId, "hold").onHold();
        }
    }

    private void unhold(String callId) {
        logDebugMsgWithOpFormat("CC", "Unhold", callId, null);
        Log.d(this, "unhold %s", callId);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "unhold").onUnhold();
        } else {
            findConferenceForAction(callId, "unhold").onUnhold();
        }
    }

    private void onCallAudioStateChanged(String callId, CallAudioState callAudioState) {
        Log.d(this, "onAudioStateChanged %s %s", callId, callAudioState);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "onCallAudioStateChanged").setCallAudioState(callAudioState);
        } else {
            findConferenceForAction(callId, "onCallAudioStateChanged").setCallAudioState(callAudioState);
        }
    }

    private void playDtmfTone(String callId, char digit) {
        Log.d(this, "playDtmfTone %s %c", callId, Character.valueOf(digit));
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "playDtmfTone").onPlayDtmfTone(digit);
        } else {
            findConferenceForAction(callId, "playDtmfTone").onPlayDtmfTone(digit);
        }
    }

    private void stopDtmfTone(String callId) {
        Log.d(this, "stopDtmfTone %s", callId);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "stopDtmfTone").onStopDtmfTone();
        } else {
            findConferenceForAction(callId, "stopDtmfTone").onStopDtmfTone();
        }
    }

    private void conference(String callId1, String callId2) {
        logDebugMsgWithOpFormat("CC", "Conference", callId1, null);
        Log.d(this, "conference %s, %s", callId1, callId2);
        Connection connection2 = findConnectionForAction(callId2, "conference");
        Conference conference2 = getNullConference();
        if (connection2 == getNullConnection() && (conference2 = findConferenceForAction(callId2, "conference")) == getNullConference()) {
            Log.w(this, "Connection2 or Conference2 missing in conference request %s.", callId2);
            return;
        }
        Connection connection1 = findConnectionForAction(callId1, "conference");
        if (connection1 == getNullConnection()) {
            Conference conference1 = findConferenceForAction(callId1, "addConnection");
            if (conference1 == getNullConference()) {
                Log.w(this, "Connection1 or Conference1 missing in conference request %s.", callId1);
                return;
            } else if (connection2 != getNullConnection()) {
                conference1.onMerge(connection2);
                return;
            } else {
                Log.wtf(this, "There can only be one conference and an attempt was made to merge two conferences.", new Object[0]);
                return;
            }
        }
        if (conference2 != getNullConference()) {
            conference2.onMerge(connection1);
        } else {
            onConference(connection1, connection2);
        }
    }

    private void splitFromConference(String callId) {
        Log.d(this, "splitFromConference(%s)", callId);
        Connection connection = findConnectionForAction(callId, "splitFromConference");
        if (connection == getNullConnection()) {
            Log.w(this, "Connection missing in conference request %s.", callId);
            return;
        }
        Conference conference = connection.getConference();
        if (conference == null) {
            return;
        }
        conference.onSeparate(connection);
    }

    private void mergeConference(String callId) {
        Log.d(this, "mergeConference(%s)", callId);
        Conference conference = findConferenceForAction(callId, "mergeConference");
        if (conference == null) {
            return;
        }
        conference.onMerge();
    }

    private void swapConference(String callId) {
        Log.d(this, "swapConference(%s)", callId);
        Conference conference = findConferenceForAction(callId, "swapConference");
        if (conference == null) {
            return;
        }
        conference.onSwap();
    }

    private void pullExternalCall(String callId) {
        Log.d(this, "pullExternalCall(%s)", callId);
        Connection connection = findConnectionForAction(callId, "pullExternalCall");
        if (connection == null) {
            return;
        }
        connection.onPullExternalCall();
    }

    private void sendCallEvent(String callId, String event, Bundle extras) {
        Log.d(this, "sendCallEvent(%s, %s)", callId, event);
        Connection connection = findConnectionForAction(callId, "sendCallEvent");
        if (connection == null) {
            return;
        }
        connection.onCallEvent(event, extras);
    }

    private void handleExtrasChanged(String callId, Bundle extras) {
        Log.d(this, "handleExtrasChanged(%s, %s)", callId, extras);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "handleExtrasChanged").handleExtrasChanged(extras);
        } else {
            if (!this.mConferenceById.containsKey(callId)) {
                return;
            }
            findConferenceForAction(callId, "handleExtrasChanged").handleExtrasChanged(extras);
        }
    }

    private void onPostDialContinue(String callId, boolean proceed) {
        Log.d(this, "onPostDialContinue(%s)", callId);
        findConnectionForAction(callId, "stopDtmfTone").onPostDialContinue(proceed);
    }

    private void explicitCallTransfer(String callId) {
        if (!canTransfer(this.mConnectionById.get(callId))) {
            Log.d(this, "explicitCallTransfer %s fail", callId);
        } else {
            Log.d(this, "explicitCallTransfer %s", callId);
            findConnectionForAction(callId, "explicitCallTransfer").onExplicitCallTransfer();
        }
    }

    private void explicitCallTransfer(String callId, String number, int type) {
        if (!canBlindAssuredTransfer(this.mConnectionById.get(callId))) {
            Log.d(this, "explicitCallTransfer %s fail", callId);
        } else {
            Log.d(this, "explicitCallTransfer %s %s %d", callId, number, Integer.valueOf(type));
            findConnectionForAction(callId, "explicitCallTransfer").onExplicitCallTransfer(number, type);
        }
    }

    private void hangupAll(String callId) {
        Log.d(this, "hangupAll %s", callId);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "hangupAll").onHangupAll();
        } else {
            findConferenceForAction(callId, "hangupAll").onHangupAll();
        }
    }

    private void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
        StringBuilder sb = new StringBuilder();
        for (String number : numbers) {
            sb.append(number);
        }
        logDebugMsgWithOpFormat("CC", "AddMember", conferenceCallId, " numbers=" + sb.toString());
        Log.d(this, "inviteConferenceParticipants %s", conferenceCallId);
        if (!this.mConferenceById.containsKey(conferenceCallId)) {
            return;
        }
        findConferenceForAction(conferenceCallId, "inviteConferenceParticipants").onInviteConferenceParticipants(numbers);
    }

    private void createConference(PhoneAccountHandle callManagerAccount, String conferenceCallId, ConnectionRequest request, List<String> numbers, boolean isIncoming) {
        Log.d(this, "createConference, callManagerAccount: %s, conferenceCallId: %s, request: %s, numbers: %s, isIncoming: %b", callManagerAccount, conferenceCallId, request, numbers, Boolean.valueOf(isIncoming));
        if (!isIncoming) {
            StringBuilder sb = new StringBuilder();
            for (String number : numbers) {
                sb.append(number);
            }
            FormattedLog formattedLog = new FormattedLog.Builder().setCategory("CC").setServiceName(getConnectionServiceName()).setOpType(FormattedLog.OpType.OPERATION).setActionName("DialConf").setCallNumber("conferenceCall").setCallId(ProxyInfo.LOCAL_EXCL_LIST).setExtraMessage("numbers=" + sb.toString()).buildDebugMsg();
            if (formattedLog != null) {
                Log.d(this, formattedLog.toString(), new Object[0]);
            }
        }
        Conference conference = onCreateConference(callManagerAccount, conferenceCallId, request, numbers, isIncoming);
        if (conference == null) {
            Log.d(this, "Fail to create conference!", new Object[0]);
            conference = getNullConference();
        } else if (conference.getState() != 6) {
            if (this.mIdByConference.containsKey(conference)) {
                Log.d(this, "Re-adding an existing conference: %s.", conference);
            } else {
                this.mConferenceById.put(conferenceCallId, conference);
                this.mIdByConference.put(conference, conferenceCallId);
                conference.addListener(this.mConferenceListener);
            }
        }
        ParcelableConference parcelableConference = new ParcelableConference(conference.getPhoneAccountHandle(), conference.getState(), conference.getConnectionCapabilities(), conference.getConnectionProperties(), null, conference.getVideoProvider() == null ? null : conference.getVideoProvider().getInterface(), conference.getVideoState(), conference.getConnectTimeMillis(), conference.getStatusHints(), conference.getExtras(), conference.getDisconnectCause());
        this.mAdapter.handleCreateConferenceComplete(conferenceCallId, request, parcelableConference);
    }

    protected Conference onCreateConference(PhoneAccountHandle callManagerAccount, String conferenceCallId, ConnectionRequest request, List<String> numbers, boolean isIncoming) {
        return null;
    }

    protected void replaceConference(Conference oldConf, Conference newConf) {
        Log.d(this, "SRVCC: oldConf= %s , newConf= %s", oldConf, newConf);
        if (oldConf == newConf || !this.mIdByConference.containsKey(oldConf)) {
            return;
        }
        Log.d(this, "SRVCC: start to do replacement", new Object[0]);
        oldConf.removeListener(this.mConferenceListener);
        String id = this.mIdByConference.get(oldConf);
        this.mConferenceById.remove(id);
        this.mIdByConference.remove(oldConf);
        this.mConferenceById.put(id, newConf);
        this.mIdByConference.put(newConf, id);
        newConf.addListener(this.mConferenceListener);
        this.mConferenceListener.onConnectionCapabilitiesChanged(newConf, newConf.getConnectionCapabilities());
    }

    private void disconnect(String callId, String pendingOperation) {
        Log.d(this, "disconnect %s, pending call action %s", callId, pendingOperation);
        if (this.mConnectionById.containsKey(callId)) {
            findConnectionForAction(callId, "disconnect").onDisconnect();
        } else {
            findConferenceForAction(callId, "disconnect").onDisconnect(pendingOperation);
        }
    }

    private void onAdapterAttached() {
        if (this.mAreAccountsInitialized) {
            return;
        }
        this.mAdapter.queryRemoteConnectionServices(new RemoteServiceCallback.Stub() {
            public void onResult(final List<ComponentName> componentNames, final List<IBinder> services) {
                ConnectionService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < componentNames.size() && i < services.size(); i++) {
                            ConnectionService.this.mRemoteConnectionManager.addConnectionService((ComponentName) componentNames.get(i), IConnectionService.Stub.asInterface((IBinder) services.get(i)));
                        }
                        ConnectionService.this.onAccountsInitialized();
                        Log.d(this, "remote connection services found: " + services, new Object[0]);
                    }
                });
            }

            public void onError() {
                ConnectionService.this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionService.this.mAreAccountsInitialized = true;
                    }
                });
            }
        });
    }

    public final RemoteConnection createRemoteIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        return this.mRemoteConnectionManager.createRemoteConnection(connectionManagerPhoneAccount, request, true);
    }

    public final RemoteConnection createRemoteOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        return this.mRemoteConnectionManager.createRemoteConnection(connectionManagerPhoneAccount, request, false);
    }

    public final void conferenceRemoteConnections(RemoteConnection remoteConnection1, RemoteConnection remoteConnection2) {
        this.mRemoteConnectionManager.conferenceRemoteConnections(remoteConnection1, remoteConnection2);
    }

    public final void addConference(Conference conference) {
        Log.d(this, "addConference: conference=%s", conference);
        String id = addConferenceInternal(conference);
        if (id == null) {
            return;
        }
        List<String> connectionIds = new ArrayList<>(2);
        for (Connection connection : conference.getConnections()) {
            if (this.mIdByConnection.containsKey(connection)) {
                connectionIds.add(this.mIdByConnection.get(connection));
            }
        }
        conference.setTelecomCallId(id);
        ParcelableConference parcelableConference = new ParcelableConference(conference.getPhoneAccountHandle(), conference.getState(), conference.getConnectionCapabilities(), conference.getConnectionProperties(), connectionIds, conference.getVideoProvider() == null ? null : conference.getVideoProvider().getInterface(), conference.getVideoState(), conference.getConnectTimeMillis(), conference.getStatusHints(), conference.getExtras());
        this.mAdapter.addConferenceCall(id, parcelableConference);
        this.mAdapter.setVideoProvider(id, conference.getVideoProvider());
        this.mAdapter.setVideoState(id, conference.getVideoState());
        Iterator connection$iterator = conference.getConnections().iterator();
        while (connection$iterator.hasNext()) {
            String connectionId = this.mIdByConnection.get((Connection) connection$iterator.next());
            if (connectionId != null) {
                this.mAdapter.setIsConferenced(connectionId, id);
            }
        }
    }

    public final void addExistingConnection(PhoneAccountHandle phoneAccountHandle, Connection connection) {
        String id = addExistingConnectionInternal(phoneAccountHandle, connection);
        if (id == null) {
            return;
        }
        List<String> emptyList = new ArrayList<>(0);
        ParcelableConnection parcelableConnection = new ParcelableConnection(phoneAccountHandle, connection.getState(), connection.getConnectionCapabilities(), connection.getConnectionProperties(), connection.getAddress(), connection.getAddressPresentation(), connection.getCallerDisplayName(), connection.getCallerDisplayNamePresentation(), connection.getVideoProvider() == null ? null : connection.getVideoProvider().getInterface(), connection.getVideoState(), connection.isRingbackRequested(), connection.getAudioModeIsVoip(), connection.getConnectTimeMillis(), connection.getStatusHints(), connection.getDisconnectCause(), emptyList, connection.getExtras());
        this.mAdapter.addExistingConnection(id, parcelableConnection);
    }

    public final Collection<Connection> getAllConnections() {
        return this.mConnectionById.values();
    }

    public final Collection<Conference> getAllConferences() {
        return this.mConferenceById.values();
    }

    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        return null;
    }

    public void triggerConferenceRecalculate() {
    }

    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        return null;
    }

    public Connection onCreateUnknownConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        return null;
    }

    public void onConference(Connection connection1, Connection connection2) {
    }

    public void onRemoteConferenceAdded(RemoteConference conference) {
    }

    public void onRemoteExistingConnectionAdded(RemoteConnection connection) {
    }

    public boolean containsConference(Conference conference) {
        return this.mIdByConference.containsKey(conference);
    }

    void addRemoteConference(RemoteConference remoteConference) {
        onRemoteConferenceAdded(remoteConference);
    }

    void addRemoteExistingConnection(RemoteConnection remoteConnection) {
        onRemoteExistingConnectionAdded(remoteConnection);
    }

    private void onAccountsInitialized() {
        this.mAreAccountsInitialized = true;
        for (Runnable r : this.mPreInitializationConnectionRequests) {
            r.run();
        }
        this.mPreInitializationConnectionRequests.clear();
    }

    private String addExistingConnectionInternal(PhoneAccountHandle handle, Connection connection) {
        String id;
        if (handle == null) {
            id = UUID.randomUUID().toString();
        } else {
            id = handle.getComponentName().getClassName() + "@" + getNextCallId();
        }
        addConnection(id, connection);
        return id;
    }

    private void addConnection(String callId, Connection connection) {
        connection.setTelecomCallId(callId);
        this.mConnectionById.put(callId, connection);
        this.mIdByConnection.put(connection, callId);
        connection.addConnectionListener(this.mConnectionListener);
        connection.setConnectionService(this);
        connection.fireOnCallState();
    }

    protected void removeConnection(Connection connection) {
        connection.unsetConnectionService(this);
        connection.removeConnectionListener(this.mConnectionListener);
        String id = this.mIdByConnection.get(connection);
        if (id == null) {
            return;
        }
        this.mConnectionById.remove(id);
        this.mIdByConnection.remove(connection);
        this.mAdapter.removeCall(id);
    }

    protected String removeConnectionInternal(Connection connection) {
        String id = this.mIdByConnection.get(connection);
        connection.unsetConnectionService(this);
        connection.removeConnectionListener(this.mConnectionListener);
        this.mConnectionById.remove(this.mIdByConnection.get(connection));
        this.mIdByConnection.remove(connection);
        Log.d(this, "removeConnectionInternal, callId: %s, connection: %s", id, connection);
        return id;
    }

    private String addConferenceInternal(Conference conference) {
        if (this.mIdByConference.containsKey(conference)) {
            Log.w(this, "Re-adding an existing conference: %s.", conference);
        } else if (conference != null) {
            String id = UUID.randomUUID().toString();
            this.mConferenceById.put(id, conference);
            this.mIdByConference.put(conference, id);
            conference.addListener(this.mConferenceListener);
            return id;
        }
        return null;
    }

    private void removeConference(Conference conference) {
        if (!this.mIdByConference.containsKey(conference)) {
            return;
        }
        conference.removeListener(this.mConferenceListener);
        String id = this.mIdByConference.get(conference);
        this.mConferenceById.remove(id);
        this.mIdByConference.remove(conference);
        this.mAdapter.removeCall(id);
    }

    private Connection findConnectionForAction(String callId, String action) {
        if (this.mConnectionById.containsKey(callId)) {
            return this.mConnectionById.get(callId);
        }
        Log.w(this, "%s - Cannot find Connection %s", action, callId);
        return getNullConnection();
    }

    static synchronized Connection getNullConnection() {
        if (sNullConnection == null) {
            sNullConnection = new Connection() {
            };
        }
        return sNullConnection;
    }

    private Conference findConferenceForAction(String conferenceId, String action) {
        if (this.mConferenceById.containsKey(conferenceId)) {
            return this.mConferenceById.get(conferenceId);
        }
        Log.w(this, "%s - Cannot find conference %s", action, conferenceId);
        return getNullConference();
    }

    private List<String> createConnectionIdList(List<Connection> connections) {
        List<String> ids = new ArrayList<>();
        for (Connection c : connections) {
            if (this.mIdByConnection.containsKey(c)) {
                ids.add(this.mIdByConnection.get(c));
            }
        }
        Collections.sort(ids);
        return ids;
    }

    private List<String> createIdList(List<Conferenceable> conferenceables) {
        List<String> ids = new ArrayList<>();
        for (Conferenceable c : conferenceables) {
            if (c instanceof Connection) {
                Connection connection = (Connection) c;
                if (this.mIdByConnection.containsKey(connection)) {
                    ids.add(this.mIdByConnection.get(connection));
                }
            } else if (c instanceof Conference) {
                Conference conference = (Conference) c;
                if (this.mIdByConference.containsKey(conference)) {
                    ids.add(this.mIdByConference.get(conference));
                }
            }
        }
        Collections.sort(ids);
        return ids;
    }

    private Conference getNullConference() {
        PhoneAccountHandle phoneAccountHandle = null;
        if (this.sNullConference == null) {
            this.sNullConference = new Conference(phoneAccountHandle) {
            };
        }
        return this.sNullConference;
    }

    private void endAllConnections() {
        for (Connection connection : this.mIdByConnection.keySet()) {
            if (connection.getConference() == null) {
                connection.onDisconnect();
            }
        }
        for (Conference conference : this.mIdByConference.keySet()) {
            conference.onDisconnect();
        }
    }

    private int getNextCallId() {
        int i;
        synchronized (this.mIdSyncRoot) {
            i = this.mId + 1;
            this.mId = i;
        }
        return i;
    }

    public boolean canDial(PhoneAccountHandle accountHandle, String dialString) {
        return true;
    }

    public boolean canTransfer(Connection bgConnection) {
        return false;
    }

    public boolean canBlindAssuredTransfer(Connection bgConnection) {
        return false;
    }

    protected void forceSuppMessageUpdate(Connection conn) {
    }

    protected void logDebugMsgWithOpFormat(String category, String action, String callId, String msg) {
        if (category == null || action == null || callId == null) {
            return;
        }
        if (msg == null) {
            msg = ProxyInfo.LOCAL_EXCL_LIST;
        }
        String callNumber = "null";
        String localCallId = "null";
        if (this.mConnectionById.containsKey(callId)) {
            Connection conn = this.mConnectionById.get(callId);
            if (conn != null && conn.getAddress() != null) {
                callNumber = conn.getAddress().getSchemeSpecificPart();
            }
            localCallId = Integer.toString(System.identityHashCode(conn));
        } else if (this.mConferenceById.containsKey(callId)) {
            callNumber = "conferenceCall";
            localCallId = Integer.toString(System.identityHashCode(this.mConferenceById.get(callId)));
        }
        FormattedLog formattedLog = new FormattedLog.Builder().setCategory(category).setServiceName(getConnectionServiceName()).setOpType(FormattedLog.OpType.OPERATION).setActionName(action).setCallNumber(callNumber).setCallId(localCallId).setExtraMessage(msg).buildDebugMsg();
        if (formattedLog == null) {
            return;
        }
        Log.d(this, formattedLog.toString(), new Object[0]);
    }

    private String getConnectionServiceName() {
        String className = getClass().getSimpleName();
        int index = className.indexOf("ConnectionService");
        if (index != -1) {
            return className.substring(0, index);
        }
        return className;
    }
}
