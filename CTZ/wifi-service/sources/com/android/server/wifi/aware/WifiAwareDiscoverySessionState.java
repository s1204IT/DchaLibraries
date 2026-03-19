package com.android.server.wifi.aware;

import android.net.wifi.aware.IWifiAwareDiscoverySessionCallback;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.SubscribeConfig;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import libcore.util.HexEncoding;

public class WifiAwareDiscoverySessionState {
    private static final String TAG = "WifiAwareDiscSessState";
    private static final boolean VDBG = false;
    private static int sNextPeerIdToBeAllocated = 100;
    private IWifiAwareDiscoverySessionCallback mCallback;
    private final long mCreationTime;
    private boolean mIsPublishSession;
    private boolean mIsRangingEnabled;
    private byte mPubSubId;
    private int mSessionId;
    private final WifiAwareNativeApi mWifiAwareNativeApi;
    boolean mDbg = false;
    private final SparseArray<PeerInfo> mPeerInfoByRequestorInstanceId = new SparseArray<>();

    static class PeerInfo {
        int mInstanceId;
        byte[] mMac;

        PeerInfo(int i, byte[] bArr) {
            this.mInstanceId = i;
            this.mMac = bArr;
        }

        public String toString() {
            return "instanceId [" + this.mInstanceId + ", mac=" + HexEncoding.encode(this.mMac) + "]";
        }
    }

    public WifiAwareDiscoverySessionState(WifiAwareNativeApi wifiAwareNativeApi, int i, byte b, IWifiAwareDiscoverySessionCallback iWifiAwareDiscoverySessionCallback, boolean z, boolean z2, long j) {
        this.mWifiAwareNativeApi = wifiAwareNativeApi;
        this.mSessionId = i;
        this.mPubSubId = b;
        this.mCallback = iWifiAwareDiscoverySessionCallback;
        this.mIsPublishSession = z;
        this.mIsRangingEnabled = z2;
        this.mCreationTime = j;
    }

    public int getSessionId() {
        return this.mSessionId;
    }

    public int getPubSubId() {
        return this.mPubSubId;
    }

    public boolean isPublishSession() {
        return this.mIsPublishSession;
    }

    public boolean isRangingEnabled() {
        return this.mIsRangingEnabled;
    }

    public long getCreationTime() {
        return this.mCreationTime;
    }

    public IWifiAwareDiscoverySessionCallback getCallback() {
        return this.mCallback;
    }

    public PeerInfo getPeerInfo(int i) {
        return this.mPeerInfoByRequestorInstanceId.get(i);
    }

    public void terminate() {
        this.mCallback = null;
        if (this.mIsPublishSession) {
            this.mWifiAwareNativeApi.stopPublish((short) 0, this.mPubSubId);
        } else {
            this.mWifiAwareNativeApi.stopSubscribe((short) 0, this.mPubSubId);
        }
    }

    public boolean isPubSubIdSession(int i) {
        return this.mPubSubId == i;
    }

    public boolean updatePublish(short s, PublishConfig publishConfig) {
        if (!this.mIsPublishSession) {
            Log.e(TAG, "A SUBSCRIBE session is being used to publish");
            try {
                this.mCallback.onSessionConfigFail(1);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "updatePublish: RemoteException=" + e);
                return false;
            }
        }
        boolean zPublish = this.mWifiAwareNativeApi.publish(s, this.mPubSubId, publishConfig);
        if (!zPublish) {
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e2) {
                Log.w(TAG, "updatePublish onSessionConfigFail(): RemoteException (FYI): " + e2);
            }
        }
        return zPublish;
    }

    public boolean updateSubscribe(short s, SubscribeConfig subscribeConfig) {
        if (this.mIsPublishSession) {
            Log.e(TAG, "A PUBLISH session is being used to subscribe");
            try {
                this.mCallback.onSessionConfigFail(1);
                return false;
            } catch (RemoteException e) {
                Log.e(TAG, "updateSubscribe: RemoteException=" + e);
                return false;
            }
        }
        boolean zSubscribe = this.mWifiAwareNativeApi.subscribe(s, this.mPubSubId, subscribeConfig);
        if (!zSubscribe) {
            try {
                this.mCallback.onSessionConfigFail(1);
            } catch (RemoteException e2) {
                Log.w(TAG, "updateSubscribe onSessionConfigFail(): RemoteException (FYI): " + e2);
            }
        }
        return zSubscribe;
    }

    public boolean sendMessage(short s, int i, byte[] bArr, int i2) {
        PeerInfo peerInfo = this.mPeerInfoByRequestorInstanceId.get(i);
        if (peerInfo == null) {
            Log.e(TAG, "sendMessage: attempting to send a message to an address which didn't match/contact us");
            try {
                this.mCallback.onMessageSendFail(i2, 1);
            } catch (RemoteException e) {
                Log.e(TAG, "sendMessage: RemoteException=" + e);
            }
            return false;
        }
        boolean zSendMessage = this.mWifiAwareNativeApi.sendMessage(s, this.mPubSubId, peerInfo.mInstanceId, peerInfo.mMac, bArr, i2);
        if (!zSendMessage) {
            try {
                this.mCallback.onMessageSendFail(i2, 1);
            } catch (RemoteException e2) {
                Log.e(TAG, "sendMessage: RemoteException=" + e2);
            }
            return false;
        }
        return zSendMessage;
    }

    public void onMatch(int i, byte[] bArr, byte[] bArr2, byte[] bArr3, int i2, int i3) {
        int peerIdOrAddIfNew = getPeerIdOrAddIfNew(i, bArr);
        try {
            if (i2 == 0) {
                this.mCallback.onMatch(peerIdOrAddIfNew, bArr2, bArr3);
            } else {
                this.mCallback.onMatchWithDistance(peerIdOrAddIfNew, bArr2, bArr3, i3);
            }
        } catch (RemoteException e) {
            Log.w(TAG, "onMatch: RemoteException (FYI): " + e);
        }
    }

    public void onMessageReceived(int i, byte[] bArr, byte[] bArr2) {
        try {
            this.mCallback.onMessageReceived(getPeerIdOrAddIfNew(i, bArr), bArr2);
        } catch (RemoteException e) {
            Log.w(TAG, "onMessageReceived: RemoteException (FYI): " + e);
        }
    }

    private int getPeerIdOrAddIfNew(int i, byte[] bArr) {
        for (int i2 = 0; i2 < this.mPeerInfoByRequestorInstanceId.size(); i2++) {
            PeerInfo peerInfoValueAt = this.mPeerInfoByRequestorInstanceId.valueAt(i2);
            if (peerInfoValueAt.mInstanceId == i && Arrays.equals(bArr, peerInfoValueAt.mMac)) {
                return this.mPeerInfoByRequestorInstanceId.keyAt(i2);
            }
        }
        int i3 = sNextPeerIdToBeAllocated;
        sNextPeerIdToBeAllocated = i3 + 1;
        this.mPeerInfoByRequestorInstanceId.put(i3, new PeerInfo(i, bArr));
        return i3;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("AwareSessionState:");
        printWriter.println("  mSessionId: " + this.mSessionId);
        printWriter.println("  mIsPublishSession: " + this.mIsPublishSession);
        printWriter.println("  mPubSubId: " + ((int) this.mPubSubId));
        printWriter.println("  mPeerInfoByRequestorInstanceId: [" + this.mPeerInfoByRequestorInstanceId + "]");
    }
}
