package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.IRttManager;
import android.net.wifi.RttManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.SystemService;
import com.android.server.wifi.WifiNative;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public final class RttService extends SystemService {
    public static final boolean DBG = true;
    private static final String TAG = "RttService";
    private final HandlerThread mHandlerThread;
    RttServiceImpl mImpl;

    static class RttServiceImpl extends IRttManager.Stub {
        private static final int BASE = 160512;
        private static final int CMD_DRIVER_LOADED = 160512;
        private static final int CMD_DRIVER_UNLOADED = 160513;
        private static final int CMD_ISSUE_NEXT_REQUEST = 160514;
        private static final int CMD_RTT_RESPONSE = 160515;
        private static final int MAX_RESPONDER_DURATION_SECONDS = 600;
        private ClientHandler mClientHandler;
        private final Context mContext;
        private final Looper mLooper;
        private RttStateMachine mStateMachine;
        private Queue<RttRequest> mRequestQueue = new LinkedList();
        private HashMap<Messenger, ClientInfo> mClients = new HashMap<>(4);
        private WifiNative.RttEventHandler mEventHandler = new WifiNative.RttEventHandler() {
            @Override
            public void onRttResults(RttManager.RttResult[] result) {
                RttServiceImpl.this.mStateMachine.sendMessage(RttServiceImpl.CMD_RTT_RESPONSE, result);
            }
        };
        private final WifiNative mWifiNative = WifiNative.getWlanNativeInterface();

        public Messenger getMessenger() {
            return new Messenger(this.mClientHandler);
        }

        private class ClientHandler extends Handler {
            ClientHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                Log.d(RttService.TAG, "ClientHandler got" + msg + " what = " + getDescription(msg.what));
                switch (msg.what) {
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connected(RttServiceImpl.this.mContext, this, msg.replyTo);
                        ClientInfo client = RttServiceImpl.this.new ClientInfo(ac, msg.replyTo);
                        RttServiceImpl.this.mClients.put(msg.replyTo, client);
                        ac.replyToMessage(msg, 69634, 0);
                        break;
                    case 69634:
                    case 69635:
                    default:
                        if (((ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo)) == null) {
                            Slog.e(RttService.TAG, "Could not find client info for message " + msg.replyTo);
                            RttServiceImpl.this.replyFailed(msg, -3, "Could not find listener");
                        } else if (!RttServiceImpl.this.enforcePermissionCheck(msg)) {
                            RttServiceImpl.this.replyFailed(msg, -5, "Client doesn't have LOCATION_HARDWARE permission");
                        } else {
                            int[] validCommands = {160256, 160257, 160261, 160262};
                            for (int cmd : validCommands) {
                                if (cmd == msg.what) {
                                    RttServiceImpl.this.mStateMachine.sendMessage(Message.obtain(msg));
                                }
                                break;
                            }
                            RttServiceImpl.this.replyFailed(msg, -4, "Invalid request");
                        }
                        break;
                    case 69636:
                        if (msg.arg1 == 2) {
                            Slog.e(RttService.TAG, "Send failed, client connection lost");
                        } else {
                            Slog.d(RttService.TAG, "Client connection lost with reason: " + msg.arg1);
                        }
                        Slog.d(RttService.TAG, "closing client " + msg.replyTo);
                        ClientInfo ci = (ClientInfo) RttServiceImpl.this.mClients.remove(msg.replyTo);
                        if (ci != null) {
                            ci.cleanup();
                        }
                        break;
                }
            }

            private String getDescription(int what) {
                switch (what) {
                    case 160261:
                        return "CMD_OP_ENABLE_RESPONDER";
                    case 160262:
                        return "CMD_OP_DISABLE_RESPONDER";
                    default:
                        return "CMD_UNKNOWN";
                }
            }
        }

        RttServiceImpl(Context context, Looper looper) {
            this.mContext = context;
            this.mLooper = looper;
        }

        public void startService() {
            this.mClientHandler = new ClientHandler(this.mLooper);
            this.mStateMachine = new RttStateMachine(this.mLooper);
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int state = intent.getIntExtra("scan_enabled", 1);
                    Log.d(RttService.TAG, "SCAN_AVAILABLE : " + state);
                    if (state == 3) {
                        RttServiceImpl.this.mStateMachine.sendMessage(160512);
                    } else {
                        if (state != 1) {
                            return;
                        }
                        RttServiceImpl.this.mStateMachine.sendMessage(RttServiceImpl.CMD_DRIVER_UNLOADED);
                    }
                }
            }, new IntentFilter("wifi_scan_available"));
            this.mStateMachine.start();
        }

        private class RttRequest {
            ClientInfo ci;
            Integer key;
            RttManager.RttParams[] params;

            RttRequest(RttServiceImpl this$1, RttRequest rttRequest) {
                this();
            }

            private RttRequest() {
            }

            public String toString() {
                String str = getClass().getName() + "@" + Integer.toHexString(hashCode());
                if (this.key != null) {
                    return str + " key: " + this.key;
                }
                return str + " key:  , null";
            }
        }

        private class ClientInfo {
            private final AsyncChannel mChannel;
            private final Messenger mMessenger;
            HashMap<Integer, RttRequest> mRequests = new HashMap<>();
            Set<Integer> mResponderRequests = new HashSet();

            ClientInfo(AsyncChannel c, Messenger m) {
                this.mChannel = c;
                this.mMessenger = m;
            }

            void addResponderRequest(int key) {
                this.mResponderRequests.add(Integer.valueOf(key));
            }

            void removeResponderRequest(int key) {
                this.mResponderRequests.remove(Integer.valueOf(key));
            }

            boolean addRttRequest(int key, RttManager.ParcelableRttParams parcelableParams) {
                RttRequest rttRequest = null;
                if (parcelableParams == null) {
                    return false;
                }
                RttManager.RttParams[] params = parcelableParams.mParams;
                RttRequest request = new RttRequest(RttServiceImpl.this, rttRequest);
                request.key = Integer.valueOf(key);
                request.ci = this;
                request.params = params;
                this.mRequests.put(Integer.valueOf(key), request);
                RttServiceImpl.this.mRequestQueue.add(request);
                return true;
            }

            void removeRttRequest(int key) {
                this.mRequests.remove(Integer.valueOf(key));
            }

            void reportResponderEnableSucceed(int key, RttManager.ResponderConfig config) {
                this.mChannel.sendMessage(160263, 0, key, config);
            }

            void reportResponderEnableFailed(int key, int reason) {
                this.mChannel.sendMessage(160264, reason, key);
                this.mResponderRequests.remove(Integer.valueOf(key));
            }

            void reportResult(RttRequest request, RttManager.RttResult[] results) {
                RttManager.ParcelableRttResults parcelableResults = new RttManager.ParcelableRttResults(results);
                this.mChannel.sendMessage(160259, 0, request.key.intValue(), parcelableResults);
                this.mRequests.remove(request.key);
            }

            void reportFailed(RttRequest request, int reason, String description) {
                reportFailed(request.key.intValue(), reason, description);
            }

            void reportFailed(int key, int reason, String description) {
                Bundle bundle = new Bundle();
                bundle.putString("android.net.wifi.RttManager.Description", description);
                this.mChannel.sendMessage(160258, key, reason, bundle);
                this.mRequests.remove(Integer.valueOf(key));
            }

            void reportAborted(int key) {
                this.mChannel.sendMessage(160260, 0, key);
                cleanup();
            }

            void cleanup() {
                this.mRequests.clear();
                RttServiceImpl.this.mRequestQueue.clear();
                this.mResponderRequests.clear();
                RttServiceImpl.this.mStateMachine.sendMessage(160262);
            }
        }

        class RttStateMachine extends StateMachine {
            DefaultState mDefaultState;
            EnabledState mEnabledState;
            InitiatorEnabledState mInitiatorEnabledState;
            RttManager.ResponderConfig mResponderConfig;
            ResponderEnabledState mResponderEnabledState;

            RttStateMachine(Looper looper) {
                super("RttStateMachine", looper);
                this.mDefaultState = new DefaultState();
                this.mEnabledState = new EnabledState();
                this.mInitiatorEnabledState = new InitiatorEnabledState();
                this.mResponderEnabledState = new ResponderEnabledState();
                addState(this.mDefaultState);
                addState(this.mEnabledState);
                addState(this.mInitiatorEnabledState, this.mEnabledState);
                addState(this.mResponderEnabledState, this.mEnabledState);
                setInitialState(this.mDefaultState);
            }

            class DefaultState extends State {
                DefaultState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "DefaultState got" + msg);
                    switch (msg.what) {
                        case 160256:
                            RttServiceImpl.this.replyFailed(msg, -2, "Try later");
                            return true;
                        case 160257:
                            return true;
                        case 160261:
                            ClientInfo client = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                            if (client == null) {
                                Log.e(RttService.TAG, "client not connected yet!");
                            } else {
                                int key = msg.arg2;
                                client.reportResponderEnableFailed(key, -2);
                            }
                            return true;
                        case 160262:
                            return true;
                        case 160512:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            return true;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST:
                            RttStateMachine.this.deferMessage(msg);
                            return true;
                        default:
                            return false;
                    }
                }
            }

            class EnabledState extends State {
                EnabledState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "EnabledState got" + msg);
                    ClientInfo ci = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                    switch (msg.what) {
                        case 160256:
                            RttManager.ParcelableRttParams params = (RttManager.ParcelableRttParams) msg.obj;
                            if (params == null || params.mParams == null || params.mParams.length == 0) {
                                RttServiceImpl.this.replyFailed(msg, -4, "No params");
                                return true;
                            }
                            if (!ci.addRttRequest(msg.arg2, params)) {
                                RttServiceImpl.this.replyFailed(msg, -4, "Unspecified");
                                return true;
                            }
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            return true;
                        case 160257:
                            for (RttRequest request : RttServiceImpl.this.mRequestQueue) {
                                if (request.key.intValue() == msg.arg2) {
                                    Log.d(RttService.TAG, "Cancelling not-yet-scheduled RTT");
                                    RttServiceImpl.this.mRequestQueue.remove(request);
                                    request.ci.reportAborted(request.key.intValue());
                                    return true;
                                }
                            }
                            return true;
                        case 160261:
                            int key = msg.arg2;
                            RttStateMachine.this.mResponderConfig = RttServiceImpl.this.mWifiNative.enableRttResponder(RttServiceImpl.MAX_RESPONDER_DURATION_SECONDS);
                            Log.d(RttService.TAG, "mWifiNative.enableRttResponder called");
                            if (RttStateMachine.this.mResponderConfig != null) {
                                RttStateMachine.this.mResponderConfig.macAddress = RttServiceImpl.this.mWifiNative.getMacAddress();
                                ci.addResponderRequest(key);
                                ci.reportResponderEnableSucceed(key, RttStateMachine.this.mResponderConfig);
                                RttStateMachine.this.transitionTo(RttStateMachine.this.mResponderEnabledState);
                                return true;
                            }
                            Log.e(RttService.TAG, "enable responder failed");
                            ci.reportResponderEnableFailed(key, -1);
                            return true;
                        case 160262:
                            return true;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mDefaultState);
                            return true;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST:
                            RttStateMachine.this.deferMessage(msg);
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mInitiatorEnabledState);
                            return true;
                        default:
                            return false;
                    }
                }
            }

            class InitiatorEnabledState extends State {
                RttRequest mOutstandingRequest;

                InitiatorEnabledState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "RequestPendingState got" + msg);
                    switch (msg.what) {
                        case 160257:
                            if (this.mOutstandingRequest == null || msg.arg2 != this.mOutstandingRequest.key.intValue()) {
                                return false;
                            }
                            Log.d(RttService.TAG, "Cancelling ongoing RTT of: " + msg.arg2);
                            RttServiceImpl.this.mWifiNative.cancelRtt(this.mOutstandingRequest.params);
                            this.mOutstandingRequest.ci.reportAborted(this.mOutstandingRequest.key.intValue());
                            this.mOutstandingRequest = null;
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            return true;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED:
                            if (this.mOutstandingRequest != null) {
                                RttServiceImpl.this.mWifiNative.cancelRtt(this.mOutstandingRequest.params);
                                Log.d(RttService.TAG, "abort key: " + this.mOutstandingRequest.key);
                                this.mOutstandingRequest.ci.reportAborted(this.mOutstandingRequest.key.intValue());
                                this.mOutstandingRequest = null;
                            }
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mDefaultState);
                            return true;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST:
                            if (this.mOutstandingRequest == null) {
                                this.mOutstandingRequest = RttServiceImpl.this.issueNextRequest();
                                if (this.mOutstandingRequest == null) {
                                    RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                                }
                                if (this.mOutstandingRequest != null) {
                                    Log.d(RttService.TAG, "new mOutstandingRequest.key is: " + this.mOutstandingRequest.key);
                                    return true;
                                }
                                Log.d(RttService.TAG, "CMD_ISSUE_NEXT_REQUEST: mOutstandingRequest =null ");
                                return true;
                            }
                            Log.d(RttService.TAG, "Current mOutstandingRequest.key is: " + this.mOutstandingRequest.key);
                            Log.d(RttService.TAG, "Ignoring CMD_ISSUE_NEXT_REQUEST");
                            return true;
                        case RttServiceImpl.CMD_RTT_RESPONSE:
                            Log.d(RttService.TAG, "Received an RTT response from: " + msg.arg2);
                            this.mOutstandingRequest.ci.reportResult(this.mOutstandingRequest, (RttManager.RttResult[]) msg.obj);
                            this.mOutstandingRequest = null;
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            return true;
                        default:
                            return false;
                    }
                }
            }

            private boolean hasOutstandingReponderRequests() {
                for (ClientInfo client : RttServiceImpl.this.mClients.values()) {
                    if (!client.mResponderRequests.isEmpty()) {
                        return true;
                    }
                }
                return false;
            }

            class ResponderEnabledState extends State {
                ResponderEnabledState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "ResponderEnabledState got " + msg);
                    ClientInfo ci = (ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo);
                    int key = msg.arg2;
                    switch (msg.what) {
                        case 160256:
                        case 160257:
                            RttServiceImpl.this.replyFailed(msg, -6, "Initiator not allowed when responder is turned on");
                            break;
                        case 160261:
                            ci.addResponderRequest(key);
                            ci.reportResponderEnableSucceed(key, RttStateMachine.this.mResponderConfig);
                            break;
                        case 160262:
                            if (ci != null) {
                                ci.removeResponderRequest(key);
                            }
                            if (!RttStateMachine.this.hasOutstandingReponderRequests()) {
                                if (!RttServiceImpl.this.mWifiNative.disableRttResponder()) {
                                    Log.e(RttService.TAG, "disable responder failed");
                                }
                                Log.d(RttService.TAG, "mWifiNative.disableRttResponder called");
                                RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            }
                            break;
                    }
                    return true;
                }
            }
        }

        void replySucceeded(Message msg, Object obj) {
            if (msg.replyTo == null) {
                return;
            }
            Message reply = Message.obtain();
            reply.what = 160259;
            reply.arg2 = msg.arg2;
            reply.obj = obj;
            try {
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }

        void replyFailed(Message msg, int reason, String description) {
            Message reply = Message.obtain();
            reply.what = 160258;
            reply.arg1 = reason;
            reply.arg2 = msg.arg2;
            Bundle bundle = new Bundle();
            bundle.putString("android.net.wifi.RttManager.Description", description);
            reply.obj = bundle;
            try {
                if (msg.replyTo == null) {
                    return;
                }
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }

        boolean enforcePermissionCheck(Message msg) {
            try {
                this.mContext.enforcePermission("android.permission.LOCATION_HARDWARE", -1, msg.sendingUid, "LocationRTT");
                return true;
            } catch (SecurityException e) {
                Log.e(RttService.TAG, "UID: " + msg.sendingUid + " has no LOCATION_HARDWARE Permission");
                return false;
            }
        }

        RttRequest issueNextRequest() {
            while (!this.mRequestQueue.isEmpty()) {
                RttRequest request = this.mRequestQueue.remove();
                if (request != null) {
                    if (this.mWifiNative.requestRtt(request.params, this.mEventHandler)) {
                        Log.d(RttService.TAG, "Issued next RTT request with key: " + request.key);
                        return request;
                    }
                    Log.e(RttService.TAG, "Fail to issue key at native layer");
                    request.ci.reportFailed(request, -1, "Failed to start");
                }
            }
            Log.d(RttService.TAG, "No more requests left");
            return null;
        }

        public RttManager.RttCapabilities getRttCapabilities() {
            return this.mWifiNative.getRttCapabilities();
        }
    }

    public RttService(Context context) {
        super(context);
        this.mHandlerThread = new HandlerThread("WifiRttService");
        this.mHandlerThread.start();
        Log.i(TAG, "Creating rttmanager");
    }

    public void onStart() {
        this.mImpl = new RttServiceImpl(getContext(), this.mHandlerThread.getLooper());
        Log.i(TAG, "Starting rttmanager");
        publishBinderService("rttmanager", this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase != 500) {
            return;
        }
        Log.i(TAG, "Registering rttmanager");
        if (this.mImpl == null) {
            this.mImpl = new RttServiceImpl(getContext(), this.mHandlerThread.getLooper());
        }
        this.mImpl.startService();
    }
}
