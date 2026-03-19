package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.ContextHubMsg;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.NanoAppMessage;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class ContextHubClientManager {
    private static final boolean DEBUG_LOG_ENABLED = true;
    private static final int MAX_CLIENT_ID = 32767;
    private static final String TAG = "ContextHubClientManager";
    private final Context mContext;
    private final IContexthub mContextHubProxy;
    private final ConcurrentHashMap<Short, ContextHubClientBroker> mHostEndPointIdToClientMap = new ConcurrentHashMap<>();
    private int mNextHostEndpointId = 0;

    ContextHubClientManager(Context context, IContexthub iContexthub) {
        this.mContext = context;
        this.mContextHubProxy = iContexthub;
    }

    IContextHubClient registerClient(IContextHubClientCallback iContextHubClientCallback, int i) {
        ?? CreateNewClientBroker = createNewClientBroker(iContextHubClientCallback, i);
        try {
            CreateNewClientBroker.attachDeathRecipient();
            Log.d(TAG, "Registered client with host endpoint ID " + ((int) CreateNewClientBroker.getHostEndPointId()));
            return IContextHubClient.Stub.asInterface((IBinder) CreateNewClientBroker);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to attach death recipient to client");
            CreateNewClientBroker.close();
            return null;
        }
    }

    void onMessageFromNanoApp(int i, ContextHubMsg contextHubMsg) {
        NanoAppMessage nanoAppMessageCreateNanoAppMessage = ContextHubServiceUtil.createNanoAppMessage(contextHubMsg);
        Log.v(TAG, "Received " + nanoAppMessageCreateNanoAppMessage);
        if (nanoAppMessageCreateNanoAppMessage.isBroadcastMessage()) {
            broadcastMessage(i, nanoAppMessageCreateNanoAppMessage);
            return;
        }
        ContextHubClientBroker contextHubClientBroker = this.mHostEndPointIdToClientMap.get(Short.valueOf(contextHubMsg.hostEndPoint));
        if (contextHubClientBroker != null) {
            contextHubClientBroker.sendMessageToClient(nanoAppMessageCreateNanoAppMessage);
            return;
        }
        Log.e(TAG, "Cannot send message to unregistered client (host endpoint ID = " + ((int) contextHubMsg.hostEndPoint) + ")");
    }

    void unregisterClient(short s) {
        if (this.mHostEndPointIdToClientMap.remove(Short.valueOf(s)) != null) {
            Log.d(TAG, "Unregistered client with host endpoint ID " + ((int) s));
            return;
        }
        Log.e(TAG, "Cannot unregister non-existing client with host endpoint ID " + ((int) s));
    }

    void onNanoAppLoaded(int i, final long j) {
        forEachClientOfHub(i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppLoaded(j);
            }
        });
    }

    void onNanoAppUnloaded(int i, final long j) {
        forEachClientOfHub(i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppUnloaded(j);
            }
        });
    }

    void onHubReset(int i) {
        forEachClientOfHub(i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onHubReset();
            }
        });
    }

    void onNanoAppAborted(int i, final long j, final int i2) {
        forEachClientOfHub(i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).onNanoAppAborted(j, i2);
            }
        });
    }

    private synchronized ContextHubClientBroker createNewClientBroker(IContextHubClientCallback iContextHubClientCallback, int i) {
        ContextHubClientBroker contextHubClientBroker;
        if (this.mHostEndPointIdToClientMap.size() == 32768) {
            throw new IllegalStateException("Could not register client - max limit exceeded");
        }
        contextHubClientBroker = null;
        int i2 = 0;
        int i3 = this.mNextHostEndpointId;
        int i4 = 0;
        while (true) {
            if (i4 > MAX_CLIENT_ID) {
                break;
            }
            short s = (short) i3;
            if (!this.mHostEndPointIdToClientMap.containsKey(Short.valueOf(s))) {
                contextHubClientBroker = new ContextHubClientBroker(this.mContext, this.mContextHubProxy, this, i, s, iContextHubClientCallback);
                this.mHostEndPointIdToClientMap.put(Short.valueOf(s), contextHubClientBroker);
                if (i3 != MAX_CLIENT_ID) {
                    i2 = i3 + 1;
                }
                this.mNextHostEndpointId = i2;
            } else {
                if (i3 == MAX_CLIENT_ID) {
                    i3 = 0;
                } else {
                    i3++;
                }
                i4++;
            }
        }
        return contextHubClientBroker;
    }

    private void broadcastMessage(int i, final NanoAppMessage nanoAppMessage) {
        forEachClientOfHub(i, new Consumer() {
            @Override
            public final void accept(Object obj) {
                ((ContextHubClientBroker) obj).sendMessageToClient(nanoAppMessage);
            }
        });
    }

    private void forEachClientOfHub(int i, Consumer<ContextHubClientBroker> consumer) {
        for (ContextHubClientBroker contextHubClientBroker : this.mHostEndPointIdToClientMap.values()) {
            if (contextHubClientBroker.getAttachedContextHubId() == i) {
                consumer.accept(contextHubClientBroker);
            }
        }
    }
}
