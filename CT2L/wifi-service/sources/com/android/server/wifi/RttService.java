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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

class RttService extends SystemService {
    public static final boolean DBG = true;
    private static final String TAG = "RttService";
    RttServiceImpl mImpl;

    class RttServiceImpl extends IRttManager.Stub {
        private static final int BASE = 160512;
        private static final int CMD_DRIVER_LOADED = 160512;
        private static final int CMD_DRIVER_UNLOADED = 160513;
        private static final int CMD_ISSUE_NEXT_REQUEST = 160514;
        private static final int CMD_RTT_RESPONSE = 160515;
        private ClientHandler mClientHandler;
        private Context mContext;
        private RttStateMachine mStateMachine;
        private Queue<RttRequest> mRequestQueue = new LinkedList();
        private HashMap<Messenger, ClientInfo> mClients = new HashMap<>(4);
        private WifiNative.RttEventHandler mEventHandler = new WifiNative.RttEventHandler() {
            @Override
            public void onRttResults(RttManager.RttResult[] result) {
                RttServiceImpl.this.mStateMachine.sendMessage(RttServiceImpl.CMD_RTT_RESPONSE, result);
            }
        };

        public Messenger getMessenger() {
            return new Messenger(this.mClientHandler);
        }

        private class ClientHandler extends Handler {
            ClientHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                Log.d(RttService.TAG, "ClientHandler got" + msg);
                switch (msg.what) {
                    case 69632:
                        if (msg.arg1 == 0) {
                            AsyncChannel c = (AsyncChannel) msg.obj;
                            Slog.d(RttService.TAG, "New client listening to asynchronous messages: " + msg.replyTo);
                            ClientInfo cInfo = RttServiceImpl.this.new ClientInfo(c, msg.replyTo);
                            RttServiceImpl.this.mClients.put(msg.replyTo, cInfo);
                        } else {
                            Slog.e(RttService.TAG, "Client connection failure, error=" + msg.arg1);
                        }
                        break;
                    case 69633:
                        AsyncChannel ac = new AsyncChannel();
                        ac.connect(RttServiceImpl.this.mContext, this, msg.replyTo);
                        break;
                    case 69634:
                    case 69635:
                    default:
                        if (((ClientInfo) RttServiceImpl.this.mClients.get(msg.replyTo)) == null) {
                            Slog.e(RttService.TAG, "Could not find client info for message " + msg.replyTo);
                            RttServiceImpl.this.replyFailed(msg, -3, "Could not find listener");
                        } else {
                            int[] validCommands = {160256, 160257};
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
        }

        RttServiceImpl() {
        }

        RttServiceImpl(Context context) {
            this.mContext = context;
        }

        public void startService(Context context) {
            this.mContext = context;
            HandlerThread thread = new HandlerThread("WifiRttService");
            thread.start();
            this.mClientHandler = new ClientHandler(thread.getLooper());
            this.mStateMachine = new RttStateMachine(thread.getLooper());
            this.mContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context2, Intent intent) {
                    int state = intent.getIntExtra("scan_enabled", 1);
                    Log.d(RttService.TAG, "SCAN_AVAILABLE : " + state);
                    if (state == 3) {
                        RttServiceImpl.this.mStateMachine.sendMessage(160512);
                    } else if (state == 1) {
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

            private RttRequest() {
            }
        }

        private class ClientInfo {
            private final AsyncChannel mChannel;
            private final Messenger mMessenger;
            HashMap<Integer, RttRequest> mRequests = new HashMap<>();

            ClientInfo(AsyncChannel c, Messenger m) {
                this.mChannel = c;
                this.mMessenger = m;
            }

            boolean addRttRequest(int key, RttManager.ParcelableRttParams parcelableParams) {
                if (parcelableParams == null) {
                    return false;
                }
                RttManager.RttParams[] params = parcelableParams.mParams;
                RttRequest request = new RttRequest();
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
                this.mChannel.sendMessage(160260, key);
                this.mRequests.remove(Integer.valueOf(key));
            }

            void cleanup() {
                this.mRequests.clear();
            }
        }

        class RttStateMachine extends StateMachine {
            DefaultState mDefaultState;
            EnabledState mEnabledState;
            RequestPendingState mRequestPendingState;

            RttStateMachine(Looper looper) {
                super("RttStateMachine", looper);
                this.mDefaultState = new DefaultState();
                this.mEnabledState = new EnabledState();
                this.mRequestPendingState = new RequestPendingState();
                addState(this.mDefaultState);
                addState(this.mEnabledState);
                addState(this.mRequestPendingState, this.mEnabledState);
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
                            break;
                        case 160257:
                            break;
                        case 160512:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mEnabledState);
                            break;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST:
                            RttStateMachine.this.deferMessage(msg);
                            break;
                    }
                    return true;
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
                            if (params == null) {
                                RttServiceImpl.this.replyFailed(msg, -4, "No params");
                            } else if (!ci.addRttRequest(msg.arg2, params)) {
                                RttServiceImpl.this.replyFailed(msg, -4, "Unspecified");
                            } else {
                                RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            }
                            return true;
                        case 160257:
                            Iterator<RttRequest> it = RttServiceImpl.this.mRequestQueue.iterator();
                            while (true) {
                                if (it.hasNext()) {
                                    RttRequest request = it.next();
                                    if (request.key.intValue() == msg.arg2) {
                                        Log.d(RttService.TAG, "Cancelling not-yet-scheduled RTT");
                                        RttServiceImpl.this.mRequestQueue.remove(request);
                                        request.ci.reportAborted(request.key.intValue());
                                    }
                                }
                            }
                            return true;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED:
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mDefaultState);
                            return true;
                        case RttServiceImpl.CMD_ISSUE_NEXT_REQUEST:
                            RttStateMachine.this.deferMessage(msg);
                            RttStateMachine.this.transitionTo(RttStateMachine.this.mRequestPendingState);
                            return true;
                        default:
                            return false;
                    }
                }
            }

            class RequestPendingState extends State {
                RttRequest mOutstandingRequest;

                RequestPendingState() {
                }

                public boolean processMessage(Message msg) {
                    Log.d(RttService.TAG, "RequestPendingState got" + msg);
                    switch (msg.what) {
                        case 160257:
                            if (this.mOutstandingRequest == null || msg.arg2 != this.mOutstandingRequest.key.intValue()) {
                                return false;
                            }
                            Log.d(RttService.TAG, "Cancelling ongoing RTT");
                            WifiNative.cancelRtt(this.mOutstandingRequest.params);
                            this.mOutstandingRequest.ci.reportAborted(this.mOutstandingRequest.key.intValue());
                            this.mOutstandingRequest = null;
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            return true;
                        case RttServiceImpl.CMD_DRIVER_UNLOADED:
                            if (this.mOutstandingRequest != null) {
                                WifiNative.cancelRtt(this.mOutstandingRequest.params);
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
                            } else {
                                Log.d(RttService.TAG, "Ignoring CMD_ISSUE_NEXT_REQUEST");
                            }
                            return true;
                        case RttServiceImpl.CMD_RTT_RESPONSE:
                            Log.d(RttService.TAG, "Received an RTT response");
                            this.mOutstandingRequest.ci.reportResult(this.mOutstandingRequest, (RttManager.RttResult[]) msg.obj);
                            this.mOutstandingRequest = null;
                            RttStateMachine.this.sendMessage(RttServiceImpl.CMD_ISSUE_NEXT_REQUEST);
                            return true;
                        default:
                            return false;
                    }
                }
            }
        }

        void replySucceeded(Message msg, Object obj) {
            if (msg.replyTo != null) {
                Message reply = Message.obtain();
                reply.what = 160259;
                reply.arg2 = msg.arg2;
                reply.obj = obj;
                try {
                    msg.replyTo.send(reply);
                } catch (RemoteException e) {
                }
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
                msg.replyTo.send(reply);
            } catch (RemoteException e) {
            }
        }

        RttRequest issueNextRequest() {
            while (!this.mRequestQueue.isEmpty()) {
                RttRequest request = this.mRequestQueue.remove();
                if (WifiNative.requestRtt(request.params, this.mEventHandler)) {
                    Log.d(RttService.TAG, "Issued next RTT request");
                    return request;
                }
                request.ci.reportFailed(request, -1, "Failed to start");
            }
            Log.d(RttService.TAG, "No more requests left");
            return null;
        }
    }

    public RttService(Context context) {
        super(context);
        Log.i(TAG, "Creating rttmanager");
    }

    public void onStart() {
        this.mImpl = new RttServiceImpl(getContext());
        Log.i(TAG, "Starting rttmanager");
        publishBinderService("rttmanager", this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            Log.i(TAG, "Registering rttmanager");
            if (this.mImpl == null) {
                this.mImpl = new RttServiceImpl(getContext());
            }
            this.mImpl.startService(getContext());
        }
    }
}
