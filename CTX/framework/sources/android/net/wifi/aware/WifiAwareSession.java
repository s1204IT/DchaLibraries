package android.net.wifi.aware;

import android.annotation.SystemApi;
import android.net.NetworkSpecifier;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import dalvik.system.CloseGuard;
import java.lang.ref.WeakReference;

public class WifiAwareSession implements AutoCloseable {
    private static final boolean DBG = false;
    private static final String TAG = "WifiAwareSession";
    private static final boolean VDBG = false;
    private final Binder mBinder;
    private final int mClientId;
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private final WeakReference<WifiAwareManager> mMgr;
    private boolean mTerminated;

    public WifiAwareSession(WifiAwareManager wifiAwareManager, Binder binder, int i) {
        this.mTerminated = true;
        this.mMgr = new WeakReference<>(wifiAwareManager);
        this.mBinder = binder;
        this.mClientId = i;
        this.mTerminated = false;
        this.mCloseGuard.open("close");
    }

    @Override
    public void close() {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "destroy: called post GC on WifiAwareManager");
            return;
        }
        wifiAwareManager.disconnect(this.mClientId, this.mBinder);
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

    public void publish(PublishConfig publishConfig, DiscoverySessionCallback discoverySessionCallback, Handler handler) {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.e(TAG, "publish: called post GC on WifiAwareManager");
        } else if (this.mTerminated) {
            Log.e(TAG, "publish: called after termination");
        } else {
            wifiAwareManager.publish(this.mClientId, handler == null ? Looper.getMainLooper() : handler.getLooper(), publishConfig, discoverySessionCallback);
        }
    }

    public void subscribe(SubscribeConfig subscribeConfig, DiscoverySessionCallback discoverySessionCallback, Handler handler) {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.e(TAG, "publish: called post GC on WifiAwareManager");
        } else if (this.mTerminated) {
            Log.e(TAG, "publish: called after termination");
        } else {
            wifiAwareManager.subscribe(this.mClientId, handler == null ? Looper.getMainLooper() : handler.getLooper(), subscribeConfig, discoverySessionCallback);
        }
    }

    public NetworkSpecifier createNetworkSpecifierOpen(int i, byte[] bArr) {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.e(TAG, "createNetworkSpecifierOpen: called post GC on WifiAwareManager");
            return null;
        }
        if (this.mTerminated) {
            Log.e(TAG, "createNetworkSpecifierOpen: called after termination");
            return null;
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, bArr, null, null);
    }

    public NetworkSpecifier createNetworkSpecifierPassphrase(int i, byte[] bArr, String str) {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.e(TAG, "createNetworkSpecifierPassphrase: called post GC on WifiAwareManager");
            return null;
        }
        if (this.mTerminated) {
            Log.e(TAG, "createNetworkSpecifierPassphrase: called after termination");
            return null;
        }
        if (!WifiAwareUtils.validatePassphrase(str)) {
            throw new IllegalArgumentException("Passphrase must meet length requirements");
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, bArr, null, str);
    }

    @SystemApi
    public NetworkSpecifier createNetworkSpecifierPmk(int i, byte[] bArr, byte[] bArr2) {
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.e(TAG, "createNetworkSpecifierPmk: called post GC on WifiAwareManager");
            return null;
        }
        if (this.mTerminated) {
            Log.e(TAG, "createNetworkSpecifierPmk: called after termination");
            return null;
        }
        if (!WifiAwareUtils.validatePmk(bArr2)) {
            throw new IllegalArgumentException("PMK must 32 bytes");
        }
        return wifiAwareManager.createNetworkSpecifier(this.mClientId, i, bArr, bArr2, null);
    }
}
