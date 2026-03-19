package com.android.server.wifi.aware;

import android.app.AppOpsManager;
import android.content.Context;
import android.net.wifi.aware.ConfigRequest;
import android.net.wifi.aware.IWifiAwareEventCallback;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class WifiAwareClientState {
    private static final byte[] ALL_ZERO_MAC = {0, 0, 0, 0, 0, 0};
    static final int CLUSTER_CHANGE_EVENT_JOINED = 1;
    static final int CLUSTER_CHANGE_EVENT_STARTED = 0;
    private static final String TAG = "WifiAwareClientState";
    private static final boolean VDBG = false;
    private final AppOpsManager mAppOps;
    private final IWifiAwareEventCallback mCallback;
    private final String mCallingPackage;
    private final int mClientId;
    private ConfigRequest mConfigRequest;
    private final Context mContext;
    private final long mCreationTime;
    private final boolean mNotifyIdentityChange;
    private final int mPid;
    private final int mUid;
    boolean mDbg = false;
    private final SparseArray<WifiAwareDiscoverySessionState> mSessions = new SparseArray<>();
    private byte[] mLastDiscoveryInterfaceMac = ALL_ZERO_MAC;

    public WifiAwareClientState(Context context, int i, int i2, int i3, String str, IWifiAwareEventCallback iWifiAwareEventCallback, ConfigRequest configRequest, boolean z, long j) {
        this.mContext = context;
        this.mClientId = i;
        this.mUid = i2;
        this.mPid = i3;
        this.mCallingPackage = str;
        this.mCallback = iWifiAwareEventCallback;
        this.mConfigRequest = configRequest;
        this.mNotifyIdentityChange = z;
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mCreationTime = j;
    }

    public void destroy() {
        for (int i = 0; i < this.mSessions.size(); i++) {
            this.mSessions.valueAt(i).terminate();
        }
        this.mSessions.clear();
        this.mConfigRequest = null;
    }

    public ConfigRequest getConfigRequest() {
        return this.mConfigRequest;
    }

    public int getClientId() {
        return this.mClientId;
    }

    public int getUid() {
        return this.mUid;
    }

    public String getCallingPackage() {
        return this.mCallingPackage;
    }

    public boolean getNotifyIdentityChange() {
        return this.mNotifyIdentityChange;
    }

    public long getCreationTime() {
        return this.mCreationTime;
    }

    public SparseArray<WifiAwareDiscoverySessionState> getSessions() {
        return this.mSessions;
    }

    public WifiAwareDiscoverySessionState getAwareSessionStateForPubSubId(int i) {
        for (int i2 = 0; i2 < this.mSessions.size(); i2++) {
            WifiAwareDiscoverySessionState wifiAwareDiscoverySessionStateValueAt = this.mSessions.valueAt(i2);
            if (wifiAwareDiscoverySessionStateValueAt.isPubSubIdSession(i)) {
                return wifiAwareDiscoverySessionStateValueAt;
            }
        }
        return null;
    }

    public void addSession(WifiAwareDiscoverySessionState wifiAwareDiscoverySessionState) {
        int sessionId = wifiAwareDiscoverySessionState.getSessionId();
        if (this.mSessions.get(sessionId) != null) {
            Log.w(TAG, "createSession: sessionId already exists (replaced) - " + sessionId);
        }
        this.mSessions.put(sessionId, wifiAwareDiscoverySessionState);
    }

    public void removeSession(int i) {
        if (this.mSessions.get(i) == null) {
            Log.e(TAG, "removeSession: sessionId doesn't exist - " + i);
            return;
        }
        this.mSessions.delete(i);
    }

    public WifiAwareDiscoverySessionState terminateSession(int i) {
        WifiAwareDiscoverySessionState wifiAwareDiscoverySessionState = this.mSessions.get(i);
        if (wifiAwareDiscoverySessionState == null) {
            Log.e(TAG, "terminateSession: sessionId doesn't exist - " + i);
            return null;
        }
        wifiAwareDiscoverySessionState.terminate();
        this.mSessions.delete(i);
        return wifiAwareDiscoverySessionState;
    }

    public WifiAwareDiscoverySessionState getSession(int i) {
        return this.mSessions.get(i);
    }

    public void onInterfaceAddressChange(byte[] bArr) {
        if (this.mNotifyIdentityChange && !Arrays.equals(bArr, this.mLastDiscoveryInterfaceMac)) {
            try {
                this.mCallback.onIdentityChanged(hasLocationingPermission() ? bArr : ALL_ZERO_MAC);
            } catch (RemoteException e) {
                Log.w(TAG, "onIdentityChanged: RemoteException - ignored: " + e);
            }
        }
        this.mLastDiscoveryInterfaceMac = bArr;
    }

    public void onClusterChange(int i, byte[] bArr, byte[] bArr2) {
        byte[] bArr3;
        if (this.mNotifyIdentityChange && !Arrays.equals(bArr2, this.mLastDiscoveryInterfaceMac)) {
            try {
                boolean zHasLocationingPermission = hasLocationingPermission();
                IWifiAwareEventCallback iWifiAwareEventCallback = this.mCallback;
                if (zHasLocationingPermission) {
                    bArr3 = bArr2;
                } else {
                    bArr3 = ALL_ZERO_MAC;
                }
                iWifiAwareEventCallback.onIdentityChanged(bArr3);
            } catch (RemoteException e) {
                Log.w(TAG, "onIdentityChanged: RemoteException - ignored: " + e);
            }
        }
        this.mLastDiscoveryInterfaceMac = bArr2;
    }

    private boolean hasLocationingPermission() {
        return this.mContext.checkPermission("android.permission.ACCESS_COARSE_LOCATION", this.mPid, this.mUid) == 0 && this.mAppOps.noteOp(0, this.mUid, this.mCallingPackage) == 0;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("AwareClientState:");
        printWriter.println("  mClientId: " + this.mClientId);
        printWriter.println("  mConfigRequest: " + this.mConfigRequest);
        printWriter.println("  mNotifyIdentityChange: " + this.mNotifyIdentityChange);
        printWriter.println("  mCallback: " + this.mCallback);
        printWriter.println("  mSessions: [" + this.mSessions + "]");
        for (int i = 0; i < this.mSessions.size(); i++) {
            this.mSessions.valueAt(i).dump(fileDescriptor, printWriter, strArr);
        }
    }
}
