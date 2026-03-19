package com.android.server.location;

import android.content.Context;
import android.hardware.contexthub.V1_0.IContexthub;
import android.hardware.location.IContextHubClient;
import android.hardware.location.IContextHubClientCallback;
import android.hardware.location.NanoAppMessage;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContextHubClientBroker extends IContextHubClient.Stub implements IBinder.DeathRecipient {
    private static final String TAG = "ContextHubClientBroker";
    private final int mAttachedContextHubId;
    private final IContextHubClientCallback mCallbackInterface;
    private final ContextHubClientManager mClientManager;
    private final AtomicBoolean mConnectionOpen = new AtomicBoolean(true);
    private final Context mContext;
    private final IContexthub mContextHubProxy;
    private final short mHostEndPointId;

    ContextHubClientBroker(Context context, IContexthub iContexthub, ContextHubClientManager contextHubClientManager, int i, short s, IContextHubClientCallback iContextHubClientCallback) {
        this.mContext = context;
        this.mContextHubProxy = iContexthub;
        this.mClientManager = contextHubClientManager;
        this.mAttachedContextHubId = i;
        this.mHostEndPointId = s;
        this.mCallbackInterface = iContextHubClientCallback;
    }

    void attachDeathRecipient() throws RemoteException {
        this.mCallbackInterface.asBinder().linkToDeath(this, 0);
    }

    public int sendMessageToNanoApp(NanoAppMessage nanoAppMessage) throws RemoteException {
        ContextHubServiceUtil.checkPermissions(this.mContext);
        int iSendMessageToHub = 1;
        if (this.mConnectionOpen.get()) {
            try {
                iSendMessageToHub = this.mContextHubProxy.sendMessageToHub(this.mAttachedContextHubId, ContextHubServiceUtil.createHidlContextHubMessage(this.mHostEndPointId, nanoAppMessage));
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in sendMessageToNanoApp (target hub ID = " + this.mAttachedContextHubId + ")", e);
            }
        } else {
            Log.e(TAG, "Failed to send message to nanoapp: client connection is closed");
        }
        return ContextHubServiceUtil.toTransactionResult(iSendMessageToHub);
    }

    public void close() {
        if (this.mConnectionOpen.getAndSet(false)) {
            this.mClientManager.unregisterClient(this.mHostEndPointId);
        }
    }

    @Override
    public void binderDied() {
        try {
            IContextHubClient.Stub.asInterface(this).close();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException while closing client on death", e);
        }
    }

    int getAttachedContextHubId() {
        return this.mAttachedContextHubId;
    }

    short getHostEndPointId() {
        return this.mHostEndPointId;
    }

    void sendMessageToClient(NanoAppMessage nanoAppMessage) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onMessageFromNanoApp(nanoAppMessage);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while sending message to client (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }

    void onNanoAppLoaded(long j) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppLoaded(j);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onNanoAppLoaded on client (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }

    void onNanoAppUnloaded(long j) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppUnloaded(j);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onNanoAppUnloaded on client (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }

    void onHubReset() {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onHubReset();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onHubReset on client (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }

    void onNanoAppAborted(long j, int i) {
        if (this.mConnectionOpen.get()) {
            try {
                this.mCallbackInterface.onNanoAppAborted(j, i);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while calling onNanoAppAborted on client (host endpoint ID = " + ((int) this.mHostEndPointId) + ")", e);
            }
        }
    }
}
