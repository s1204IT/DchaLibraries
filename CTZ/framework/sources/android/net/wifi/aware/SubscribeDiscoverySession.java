package android.net.wifi.aware;

import android.util.Log;

public class SubscribeDiscoverySession extends DiscoverySession {
    private static final String TAG = "SubscribeDiscSession";

    public SubscribeDiscoverySession(WifiAwareManager wifiAwareManager, int i, int i2) {
        super(wifiAwareManager, i, i2);
    }

    public void updateSubscribe(SubscribeConfig subscribeConfig) {
        if (this.mTerminated) {
            Log.w(TAG, "updateSubscribe: called on terminated session");
            return;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "updateSubscribe: called post GC on WifiAwareManager");
        } else {
            wifiAwareManager.updateSubscribe(this.mClientId, this.mSessionId, subscribeConfig);
        }
    }
}
