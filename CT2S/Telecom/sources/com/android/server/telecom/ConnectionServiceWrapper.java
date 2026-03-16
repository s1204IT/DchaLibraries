package com.android.server.telecom;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.telecom.AudioState;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IConnectionService;
import com.android.internal.telecom.IConnectionServiceAdapter;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.RemoteServiceCallback;
import com.android.server.telecom.PhoneAccountRegistrar;
import com.android.server.telecom.ServiceBinder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class ConnectionServiceWrapper extends ServiceBinder<IConnectionService> {
    private final Adapter mAdapter;
    private ServiceBinder<IConnectionService>.Binder mBinder;
    private final CallIdMapper mCallIdMapper;
    private final CallsManager mCallsManager;
    private final ConnectionServiceRepository mConnectionServiceRepository;
    private final Handler mHandler;
    private final Set<Call> mPendingConferenceCalls;
    private final Map<String, CreateConnectionResponse> mPendingResponses;
    private final PhoneAccountRegistrar mPhoneAccountRegistrar;
    private IConnectionService mServiceInterface;

    private final class Adapter extends IConnectionServiceAdapter.Stub {
        private Adapter() {
        }

        public void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection) {
            ConnectionServiceWrapper.this.logIncoming("handleCreateConnectionComplete %s", connectionRequest);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = connectionRequest;
                someArgsObtain.arg3 = parcelableConnection;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
            }
        }

        public void setActive(String str) {
            ConnectionServiceWrapper.this.logIncoming("setActive %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(2, str).sendToTarget();
            }
        }

        public void setRinging(String str) {
            ConnectionServiceWrapper.this.logIncoming("setRinging %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(3, str).sendToTarget();
            }
        }

        public void setVideoProvider(String str, IVideoProvider iVideoProvider) {
            ConnectionServiceWrapper.this.logIncoming("setVideoProvider %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = iVideoProvider;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(14, someArgsObtain).sendToTarget();
            }
        }

        public void setDialing(String str) {
            ConnectionServiceWrapper.this.logIncoming("setDialing %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(4, str).sendToTarget();
            }
        }

        public void setDisconnected(String str, DisconnectCause disconnectCause) {
            ConnectionServiceWrapper.this.logIncoming("setDisconnected %s %s", str, disconnectCause);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                Log.d(this, "disconnect call %s", str);
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = disconnectCause;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(5, someArgsObtain).sendToTarget();
            }
        }

        public void setOnHold(String str) {
            ConnectionServiceWrapper.this.logIncoming("setOnHold %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(6, str).sendToTarget();
            }
        }

        public void setRingbackRequested(String str, boolean z) {
            ConnectionServiceWrapper.this.logIncoming("setRingbackRequested %s %b", str, Boolean.valueOf(z));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(7, z ? 1 : 0, 0, str).sendToTarget();
            }
        }

        public void removeCall(String str) {
            ConnectionServiceWrapper.this.logIncoming("removeCall %s", str);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(11, str).sendToTarget();
            }
        }

        public void setConnectionCapabilities(String str, int i) {
            ConnectionServiceWrapper.this.logIncoming("setConnectionCapabilities %s %d", str, Integer.valueOf(i));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(8, i, 0, str).sendToTarget();
            } else {
                Log.w(this, "ID not valid for setCallCapabilities", new Object[0]);
            }
        }

        public void setIsConferenced(String str, String str2) {
            ConnectionServiceWrapper.this.logIncoming("setIsConferenced %s %s", str, str2);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = str2;
            ConnectionServiceWrapper.this.mHandler.obtainMessage(9, someArgsObtain).sendToTarget();
        }

        public void addConferenceCall(String str, ParcelableConference parcelableConference) {
            ConnectionServiceWrapper.this.logIncoming("addConferenceCall %s %s", str, parcelableConference);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = parcelableConference;
            ConnectionServiceWrapper.this.mHandler.obtainMessage(10, someArgsObtain).sendToTarget();
        }

        public void onPostDialWait(String str, String str2) throws RemoteException {
            ConnectionServiceWrapper.this.logIncoming("onPostDialWait %s %s", str, str2);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = str2;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(12, someArgsObtain).sendToTarget();
            }
        }

        public void onPostDialChar(String str, char c) throws RemoteException {
            ConnectionServiceWrapper.this.logIncoming("onPostDialChar %s %s", str, Character.valueOf(c));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.argi1 = c;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(22, someArgsObtain).sendToTarget();
            }
        }

        public void queryRemoteConnectionServices(RemoteServiceCallback remoteServiceCallback) {
            ConnectionServiceWrapper.this.logIncoming("queryRemoteCSs", new Object[0]);
            ConnectionServiceWrapper.this.mHandler.obtainMessage(13, remoteServiceCallback).sendToTarget();
        }

        public void setVideoState(String str, int i) {
            ConnectionServiceWrapper.this.logIncoming("setVideoState %s %d", str, Integer.valueOf(i));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(19, i, 0, str).sendToTarget();
            }
        }

        public void setIsVoipAudioMode(String str, boolean z) {
            ConnectionServiceWrapper.this.logIncoming("setIsVoipAudioMode %s %b", str, Boolean.valueOf(z));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                ConnectionServiceWrapper.this.mHandler.obtainMessage(15, z ? 1 : 0, 0, str).sendToTarget();
            }
        }

        public void setStatusHints(String str, StatusHints statusHints) {
            ConnectionServiceWrapper.this.logIncoming("setStatusHints %s %s", str, statusHints);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = statusHints;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(16, someArgsObtain).sendToTarget();
            }
        }

        public void setAddress(String str, Uri uri, int i) {
            ConnectionServiceWrapper.this.logIncoming("setAddress %s %s %d", str, uri, Integer.valueOf(i));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = uri;
                someArgsObtain.argi1 = i;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(17, someArgsObtain).sendToTarget();
            }
        }

        public void setCallerDisplayName(String str, String str2, int i) {
            ConnectionServiceWrapper.this.logIncoming("setCallerDisplayName %s %s %d", str, str2, Integer.valueOf(i));
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = str2;
                someArgsObtain.argi1 = i;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(18, someArgsObtain).sendToTarget();
            }
        }

        public void setConferenceableConnections(String str, List<String> list) {
            ConnectionServiceWrapper.this.logIncoming("setConferenceableConnections %s %s", str, list);
            if (ConnectionServiceWrapper.this.mCallIdMapper.isValidCallId(str) || ConnectionServiceWrapper.this.mCallIdMapper.isValidConferenceId(str)) {
                SomeArgs someArgsObtain = SomeArgs.obtain();
                someArgsObtain.arg1 = str;
                someArgsObtain.arg2 = list;
                ConnectionServiceWrapper.this.mHandler.obtainMessage(20, someArgsObtain).sendToTarget();
            }
        }

        public void addExistingConnection(String str, ParcelableConnection parcelableConnection) {
            ConnectionServiceWrapper.this.logIncoming("addExistingConnection  %s %s", str, parcelableConnection);
            SomeArgs someArgsObtain = SomeArgs.obtain();
            someArgsObtain.arg1 = str;
            someArgsObtain.arg2 = parcelableConnection;
            ConnectionServiceWrapper.this.mHandler.obtainMessage(21, someArgsObtain).sendToTarget();
        }
    }

    ConnectionServiceWrapper(ComponentName componentName, ConnectionServiceRepository connectionServiceRepository, PhoneAccountRegistrar phoneAccountRegistrar, Context context, UserHandle userHandle) {
        super("android.telecom.ConnectionService", componentName, context, userHandle);
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                SomeArgs someArgs;
                PhoneAccountHandle targetPhoneAccount;
                switch (message.what) {
                    case 1:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            ConnectionServiceWrapper.this.handleCreateConnectionComplete((String) someArgs.arg1, (ConnectionRequest) someArgs.arg2, (ParcelableConnection) someArgs.arg3);
                            return;
                        } finally {
                        }
                    case 2:
                        Call call = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsActive(call);
                            return;
                        }
                        return;
                    case 3:
                        Call call2 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call2 != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsRinging(call2);
                            return;
                        }
                        return;
                    case 4:
                        Call call3 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call3 != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsDialing(call3);
                            return;
                        }
                        return;
                    case 5:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call4 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            DisconnectCause disconnectCause = (DisconnectCause) someArgs.arg2;
                            Log.d(this, "disconnect call %s %s", disconnectCause, call4);
                            if (call4 != null) {
                                ConnectionServiceWrapper.this.mCallsManager.markCallAsDisconnected(call4, disconnectCause);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 6:
                        Call call5 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call5 != null) {
                            ConnectionServiceWrapper.this.mCallsManager.markCallAsOnHold(call5);
                            return;
                        }
                        return;
                    case 7:
                        Call call6 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call6 != null) {
                            call6.setRingbackRequested(message.arg1 == 1);
                            return;
                        }
                        return;
                    case 8:
                        Call call7 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call7 != null) {
                            call7.setConnectionCapabilities(message.arg1);
                            return;
                        }
                        return;
                    case 9:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call8 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            Log.d(this, "SET_IS_CONFERENCE: %s %s", someArgs.arg1, someArgs.arg2);
                            if (call8 != null) {
                                String str = (String) someArgs.arg2;
                                if (str == null) {
                                    Log.d(this, "unsetting parent: %s", someArgs.arg1);
                                    call8.setParentCall(null);
                                } else {
                                    call8.setParentCall(ConnectionServiceWrapper.this.mCallIdMapper.getCall(str));
                                }
                            }
                            return;
                        } finally {
                        }
                    case 10:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            String str2 = (String) someArgs.arg1;
                            if (ConnectionServiceWrapper.this.mCallIdMapper.getCall(str2) != null) {
                                Log.w(this, "Attempting to add a conference call using an existing call id %s", str2);
                                return;
                            }
                            ParcelableConference parcelableConference = (ParcelableConference) someArgs.arg2;
                            Iterator it = parcelableConference.getConnectionIds().iterator();
                            boolean z = false;
                            while (it.hasNext()) {
                                z = ConnectionServiceWrapper.this.mCallIdMapper.getCall((String) it.next()) != null ? true : z;
                            }
                            if (!z && parcelableConference.getConnectionIds().size() > 0) {
                                Log.d(this, "Attempting to add a conference with no valid calls", new Object[0]);
                                return;
                            }
                            PhoneAccountHandle phoneAccount = (parcelableConference == null || parcelableConference.getPhoneAccount() == null) ? null : parcelableConference.getPhoneAccount();
                            if (phoneAccount == null) {
                                Iterator it2 = parcelableConference.getConnectionIds().iterator();
                                while (it2.hasNext()) {
                                    Call call9 = ConnectionServiceWrapper.this.mCallIdMapper.getCall((String) it2.next());
                                    if (call9 != null && call9.getTargetPhoneAccount() != null) {
                                        targetPhoneAccount = call9.getTargetPhoneAccount();
                                    }
                                }
                                targetPhoneAccount = phoneAccount;
                            } else {
                                targetPhoneAccount = phoneAccount;
                            }
                            Call callCreateConferenceCall = ConnectionServiceWrapper.this.mCallsManager.createConferenceCall(targetPhoneAccount, parcelableConference);
                            ConnectionServiceWrapper.this.mCallIdMapper.addCall(callCreateConferenceCall, str2);
                            callCreateConferenceCall.setConnectionService(ConnectionServiceWrapper.this);
                            Log.d(this, "adding children to conference %s phAcc %s", parcelableConference.getConnectionIds(), targetPhoneAccount);
                            for (String str3 : parcelableConference.getConnectionIds()) {
                                Call call10 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(str3);
                                Log.d(this, "found child: %s", str3);
                                if (call10 != null) {
                                    call10.setParentCall(callCreateConferenceCall);
                                }
                            }
                            return;
                        } finally {
                        }
                    case 11:
                        Call call11 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call11 != null) {
                            if (call11.isAlive()) {
                                ConnectionServiceWrapper.this.mCallsManager.markCallAsDisconnected(call11, new DisconnectCause(3));
                                return;
                            } else {
                                ConnectionServiceWrapper.this.mCallsManager.markCallAsRemoved(call11);
                                return;
                            }
                        }
                        return;
                    case 12:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call12 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            if (call12 != null) {
                                call12.onPostDialWait((String) someArgs.arg2);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 13:
                        ConnectionServiceWrapper.this.queryRemoteConnectionServices((RemoteServiceCallback) message.obj);
                        return;
                    case 14:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call13 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            IVideoProvider iVideoProvider = (IVideoProvider) someArgs.arg2;
                            if (call13 != null) {
                                call13.setVideoProvider(iVideoProvider);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 15:
                        Call call14 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call14 != null) {
                            call14.setIsVoipAudioMode(message.arg1 == 1);
                            return;
                        }
                        return;
                    case 16:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call15 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            StatusHints statusHints = (StatusHints) someArgs.arg2;
                            if (call15 != null) {
                                call15.setStatusHints(statusHints);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 17:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call16 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            if (call16 != null) {
                                call16.setHandle((Uri) someArgs.arg2, someArgs.argi1);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 18:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call17 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            if (call17 != null) {
                                call17.setCallerDisplayName((String) someArgs.arg2, someArgs.argi1);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 19:
                        Call call18 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(message.obj);
                        if (call18 != null) {
                            call18.setVideoState(message.arg1);
                            return;
                        }
                        return;
                    case 20:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call19 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            if (call19 != null) {
                                ArrayList arrayList = new ArrayList(((List) someArgs.arg2).size());
                                Iterator it3 = ((List) someArgs.arg2).iterator();
                                while (it3.hasNext()) {
                                    Call call20 = ConnectionServiceWrapper.this.mCallIdMapper.getCall((String) it3.next());
                                    if (call20 != null && call20 != call19) {
                                        arrayList.add(call20);
                                    }
                                }
                                call19.setConferenceableCalls(arrayList);
                                break;
                            }
                            return;
                        } finally {
                        }
                    case 21:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            String str4 = (String) someArgs.arg1;
                            Call callCreateCallForExistingConnection = ConnectionServiceWrapper.this.mCallsManager.createCallForExistingConnection(str4, (ParcelableConnection) someArgs.arg2);
                            ConnectionServiceWrapper.this.mCallIdMapper.addCall(callCreateCallForExistingConnection, str4);
                            callCreateCallForExistingConnection.setConnectionService(ConnectionServiceWrapper.this);
                            return;
                        } finally {
                        }
                    case 22:
                        someArgs = (SomeArgs) message.obj;
                        try {
                            Call call21 = ConnectionServiceWrapper.this.mCallIdMapper.getCall(someArgs.arg1);
                            if (call21 != null) {
                                call21.onPostDialChar((char) someArgs.argi1);
                                break;
                            }
                            return;
                        } finally {
                        }
                    default:
                        return;
                }
            }
        };
        this.mAdapter = new Adapter();
        this.mCallsManager = CallsManager.getInstance();
        this.mPendingConferenceCalls = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mCallIdMapper = new CallIdMapper("ConnectionService");
        this.mPendingResponses = new HashMap();
        this.mBinder = new ServiceBinder.Binder();
        this.mConnectionServiceRepository = connectionServiceRepository;
        phoneAccountRegistrar.addListener(new PhoneAccountRegistrar.Listener() {
        });
        this.mPhoneAccountRegistrar = phoneAccountRegistrar;
    }

    private void addConnectionServiceAdapter(IConnectionServiceAdapter iConnectionServiceAdapter) {
        if (isServiceValid("addConnectionServiceAdapter")) {
            try {
                logOutgoing("addConnectionServiceAdapter %s", iConnectionServiceAdapter);
                this.mServiceInterface.addConnectionServiceAdapter(iConnectionServiceAdapter);
            } catch (RemoteException e) {
            }
        }
    }

    void createConnection(final Call call, final CreateConnectionResponse createConnectionResponse) {
        Log.d(this, "createConnection(%s) via %s.", call, getComponentName());
        this.mBinder.bind(new ServiceBinder.BindCallback() {
            @Override
            public void onSuccess() {
                String callId = ConnectionServiceWrapper.this.mCallIdMapper.getCallId(call);
                ConnectionServiceWrapper.this.mPendingResponses.put(callId, createConnectionResponse);
                GatewayInfo gatewayInfo = call.getGatewayInfo();
                Bundle extras = call.getExtras();
                if (gatewayInfo != null && gatewayInfo.getGatewayProviderPackageName() != null && gatewayInfo.getOriginalAddress() != null) {
                    extras = (Bundle) extras.clone();
                    extras.putString("android.telecom.extra.GATEWAY_PROVIDER_PACKAGE", gatewayInfo.getGatewayProviderPackageName());
                    extras.putParcelable("android.telecom.extra.GATEWAY_ORIGINAL_ADDRESS", gatewayInfo.getOriginalAddress());
                }
                try {
                    ConnectionServiceWrapper.this.mServiceInterface.createConnection(call.getConnectionManagerPhoneAccount(), callId, new ConnectionRequest(call.getTargetPhoneAccount(), call.getHandle(), extras, call.getVideoState()), call.isIncoming(), call.isUnknown());
                } catch (RemoteException e) {
                    Log.e(this, e, "Failure to createConnection -- %s", ConnectionServiceWrapper.this.getComponentName());
                    ((CreateConnectionResponse) ConnectionServiceWrapper.this.mPendingResponses.remove(callId)).handleCreateConnectionFailure(new DisconnectCause(1, e.toString()));
                }
            }

            @Override
            public void onFailure() {
                Log.e(this, new Exception(), "Failure to call %s", ConnectionServiceWrapper.this.getComponentName());
                createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1));
            }
        });
    }

    void abort(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("abort")) {
            try {
                logOutgoing("abort %s", callId);
                this.mServiceInterface.abort(callId);
            } catch (RemoteException e) {
            }
        }
        removeCall(call, new DisconnectCause(2));
    }

    void hold(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("hold")) {
            try {
                logOutgoing("hold %s", callId);
                this.mServiceInterface.hold(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void unhold(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("unhold")) {
            try {
                logOutgoing("unhold %s", callId);
                this.mServiceInterface.unhold(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void onAudioStateChanged(Call call, AudioState audioState) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onAudioStateChanged")) {
            try {
                logOutgoing("onAudioStateChanged %s %s", callId, audioState);
                this.mServiceInterface.onAudioStateChanged(callId, audioState);
            } catch (RemoteException e) {
            }
        }
    }

    void disconnect(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("disconnect")) {
            try {
                logOutgoing("disconnect %s", callId);
                this.mServiceInterface.disconnect(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void answer(Call call, int i) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("answer")) {
            try {
                logOutgoing("answer %s %d", callId, Integer.valueOf(i));
                if (i == 0) {
                    this.mServiceInterface.answer(callId);
                } else {
                    this.mServiceInterface.answerVideo(callId, i);
                }
            } catch (RemoteException e) {
            }
        }
    }

    void reject(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("reject")) {
            try {
                logOutgoing("reject %s", callId);
                this.mServiceInterface.reject(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void playDtmfTone(Call call, char c) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("playDtmfTone")) {
            try {
                logOutgoing("playDtmfTone %s %c", callId, Character.valueOf(c));
                this.mServiceInterface.playDtmfTone(callId, c);
            } catch (RemoteException e) {
            }
        }
    }

    void stopDtmfTone(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("stopDtmfTone")) {
            try {
                logOutgoing("stopDtmfTone %s", callId);
                this.mServiceInterface.stopDtmfTone(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void addCall(Call call) {
        if (this.mCallIdMapper.getCallId(call) == null) {
            this.mCallIdMapper.addCall(call);
        }
    }

    void removeCall(Call call) {
        removeCall(call, new DisconnectCause(1));
    }

    void removeCall(String str, DisconnectCause disconnectCause) {
        CreateConnectionResponse createConnectionResponseRemove = this.mPendingResponses.remove(str);
        if (createConnectionResponseRemove != null) {
            createConnectionResponseRemove.handleCreateConnectionFailure(disconnectCause);
        }
        this.mCallIdMapper.removeCall(str);
    }

    void removeCall(Call call, DisconnectCause disconnectCause) {
        CreateConnectionResponse createConnectionResponseRemove = this.mPendingResponses.remove(this.mCallIdMapper.getCallId(call));
        if (createConnectionResponseRemove != null) {
            createConnectionResponseRemove.handleCreateConnectionFailure(disconnectCause);
        }
        this.mCallIdMapper.removeCall(call);
    }

    void onPostDialContinue(Call call, boolean z) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("onPostDialContinue")) {
            try {
                logOutgoing("onPostDialContinue %s %b", callId, Boolean.valueOf(z));
                this.mServiceInterface.onPostDialContinue(callId, z);
            } catch (RemoteException e) {
            }
        }
    }

    void conference(Call call, Call call2) {
        String callId = this.mCallIdMapper.getCallId(call);
        String callId2 = this.mCallIdMapper.getCallId(call2);
        if (callId != null && callId2 != null && isServiceValid("conference")) {
            try {
                logOutgoing("conference %s %s", callId, callId2);
                this.mServiceInterface.conference(callId, callId2);
            } catch (RemoteException e) {
            }
        }
    }

    void splitFromConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("splitFromConference")) {
            try {
                logOutgoing("splitFromConference %s", callId);
                this.mServiceInterface.splitFromConference(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void mergeConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("mergeConference")) {
            try {
                logOutgoing("mergeConference %s", callId);
                this.mServiceInterface.mergeConference(callId);
            } catch (RemoteException e) {
            }
        }
    }

    void swapConference(Call call) {
        String callId = this.mCallIdMapper.getCallId(call);
        if (callId != null && isServiceValid("swapConference")) {
            try {
                logOutgoing("swapConference %s", callId);
                this.mServiceInterface.swapConference(callId);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    protected void setServiceInterface(IBinder iBinder) {
        if (iBinder == null) {
            handleConnectionServiceDeath();
            CallsManager.getInstance().handleConnectionServiceDeath(this);
            this.mServiceInterface = null;
        } else {
            this.mServiceInterface = IConnectionService.Stub.asInterface(iBinder);
            addConnectionServiceAdapter(this.mAdapter);
        }
    }

    private void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection) {
        if (parcelableConnection.getState() == 6) {
            removeCall(str, parcelableConnection.getDisconnectCause());
        } else if (this.mPendingResponses.containsKey(str)) {
            this.mPendingResponses.remove(str).handleCreateConnectionSuccess(this.mCallIdMapper, parcelableConnection);
        }
    }

    private void handleConnectionServiceDeath() {
        if (!this.mPendingResponses.isEmpty()) {
            CreateConnectionResponse[] createConnectionResponseArr = (CreateConnectionResponse[]) this.mPendingResponses.values().toArray(new CreateConnectionResponse[this.mPendingResponses.values().size()]);
            this.mPendingResponses.clear();
            for (CreateConnectionResponse createConnectionResponse : createConnectionResponseArr) {
                createConnectionResponse.handleCreateConnectionFailure(new DisconnectCause(1));
            }
        }
        this.mCallIdMapper.clear();
    }

    private void logIncoming(String str, Object... objArr) {
        Log.d(this, "ConnectionService -> Telecom: " + str, objArr);
    }

    private void logOutgoing(String str, Object... objArr) {
        Log.d(this, "Telecom -> ConnectionService: " + str, objArr);
    }

    private void queryRemoteConnectionServices(final RemoteServiceCallback remoteServiceCallback) {
        ConnectionServiceWrapper service;
        PhoneAccountHandle simCallManager = this.mPhoneAccountRegistrar.getSimCallManager();
        Log.d(this, "queryRemoteConnectionServices finds simCallManager = %s", simCallManager);
        if (simCallManager == null || !simCallManager.getComponentName().equals(getComponentName())) {
            noRemoteServices(remoteServiceCallback);
            return;
        }
        final Set<ConnectionServiceWrapper> setNewSetFromMap = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        for (PhoneAccountHandle phoneAccountHandle : this.mPhoneAccountRegistrar.getCallCapablePhoneAccounts()) {
            if ((this.mPhoneAccountRegistrar.getPhoneAccount(phoneAccountHandle).getCapabilities() & 4) != 0 && (service = this.mConnectionServiceRepository.getService(phoneAccountHandle.getComponentName(), phoneAccountHandle.getUserHandle())) != null) {
                setNewSetFromMap.add(service);
            }
        }
        final ArrayList arrayList = new ArrayList();
        final ArrayList arrayList2 = new ArrayList();
        Log.v(this, "queryRemoteConnectionServices, simServices = %s", setNewSetFromMap);
        for (final ConnectionServiceWrapper connectionServiceWrapper : setNewSetFromMap) {
            if (connectionServiceWrapper != this) {
                connectionServiceWrapper.mBinder.bind(new ServiceBinder.BindCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(this, "Adding simService %s", connectionServiceWrapper.getComponentName());
                        arrayList.add(connectionServiceWrapper.getComponentName());
                        arrayList2.add(connectionServiceWrapper.mServiceInterface.asBinder());
                        maybeComplete();
                    }

                    @Override
                    public void onFailure() {
                        Log.d(this, "Failed simService %s", connectionServiceWrapper.getComponentName());
                        ConnectionServiceWrapper.this.noRemoteServices(remoteServiceCallback);
                    }

                    private void maybeComplete() {
                        if (arrayList.size() == setNewSetFromMap.size()) {
                            ConnectionServiceWrapper.this.setRemoteServices(remoteServiceCallback, arrayList, arrayList2);
                        }
                    }
                });
            }
        }
    }

    private void setRemoteServices(RemoteServiceCallback remoteServiceCallback, List<ComponentName> list, List<IBinder> list2) {
        try {
            remoteServiceCallback.onResult(list, list2);
        } catch (RemoteException e) {
            Log.e(this, e, "Contacting ConnectionService %s", getComponentName());
        }
    }

    private void noRemoteServices(RemoteServiceCallback remoteServiceCallback) {
        try {
            remoteServiceCallback.onResult(Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        } catch (RemoteException e) {
            Log.e(this, e, "Contacting ConnectionService %s", getComponentName());
        }
    }
}
