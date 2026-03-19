package android.net.wifi.aware;

import android.annotation.SystemApi;
import android.net.NetworkSpecifier;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import dalvik.system.CloseGuard;
import java.lang.ref.WeakReference;

public class DiscoverySession implements AutoCloseable {
    private static final boolean DBG = false;
    private static final int MAX_SEND_RETRY_COUNT = 5;
    private static final String TAG = "DiscoverySession";
    private static final boolean VDBG = false;
    protected final int mClientId;
    protected WeakReference<WifiAwareManager> mMgr;
    protected final int mSessionId;
    protected boolean mTerminated = false;
    private final CloseGuard mCloseGuard = CloseGuard.get();

    public static int getMaxSendRetryCount() {
        return 5;
    }

    public DiscoverySession(WifiAwareManager wifiAwareManager, int i, int i2) {
        this.mMgr = new WeakReference<>(wifiAwareManager);
        this.mClientId = i;
        this.mSessionId = i2;
        this.mCloseGuard.open("close");
    }

    @Override
    public void close() {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "destroy: called post GC on WifiAwareManager");
            return;
        }
        wifiAwareManager.terminateSession(this.mClientId, this.mSessionId);
        this.mTerminated = true;
        this.mMgr.clear();
        this.mCloseGuard.close();
    }

    public void setTerminated() {
        if (this.mTerminated) {
            Log.w(TAG, "terminate: already terminated.");
            return;
        }
        this.mTerminated = true;
        this.mMgr.clear();
        this.mCloseGuard.close();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            if (!this.mTerminated) {
                close();
            }
        } finally {
            super.finalize();
        }
    }

    @VisibleForTesting
    public int getClientId() {
        return this.mClientId;
    }

    @VisibleForTesting
    public int getSessionId() {
        return this.mSessionId;
    }

    public void sendMessage(PeerHandle peerHandle, int i, byte[] bArr, int i2) {
        if (this.mTerminated) {
            Log.w(TAG, "sendMessage: called on terminated session");
            return;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "sendMessage: called post GC on WifiAwareManager");
        } else {
            wifiAwareManager.sendMessage(this.mClientId, this.mSessionId, peerHandle, bArr, i, i2);
        }
    }

    public void sendMessage(PeerHandle peerHandle, int i, byte[] bArr) {
        sendMessage(peerHandle, i, bArr, 0);
    }

    public NetworkSpecifier createNetworkSpecifierOpen(PeerHandle peerHandle) {
        int i;
        if (this.mTerminated) {
            Log.w(TAG, "createNetworkSpecifierOpen: called on terminated session");
            return null;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "createNetworkSpecifierOpen: called post GC on WifiAwareManager");
            return null;
        }
        if (this instanceof SubscribeDiscoverySession) {
            i = 0;
        } else {
            i = 1;
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, this.mSessionId, peerHandle, null, null);
    }

    public NetworkSpecifier createNetworkSpecifierPassphrase(PeerHandle peerHandle, String str) {
        int i;
        if (!WifiAwareUtils.validatePassphrase(str)) {
            throw new IllegalArgumentException("Passphrase must meet length requirements");
        }
        if (this.mTerminated) {
            Log.w(TAG, "createNetworkSpecifierPassphrase: called on terminated session");
            return null;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "createNetworkSpecifierPassphrase: called post GC on WifiAwareManager");
            return null;
        }
        if (this instanceof SubscribeDiscoverySession) {
            i = 0;
        } else {
            i = 1;
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, this.mSessionId, peerHandle, null, str);
    }

    @SystemApi
    public NetworkSpecifier createNetworkSpecifierPmk(PeerHandle peerHandle, byte[] bArr) {
        int i;
        if (!WifiAwareUtils.validatePmk(bArr)) {
            throw new IllegalArgumentException("PMK must 32 bytes");
        }
        if (this.mTerminated) {
            Log.w(TAG, "createNetworkSpecifierPmk: called on terminated session");
            return null;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "createNetworkSpecifierPmk: called post GC on WifiAwareManager");
            return null;
        }
        if (this instanceof SubscribeDiscoverySession) {
            i = 0;
        } else {
            i = 1;
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, this.mSessionId, peerHandle, bArr, null);
    }
}
