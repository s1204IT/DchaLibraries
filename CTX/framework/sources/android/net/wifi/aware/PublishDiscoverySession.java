package android.net.wifi.aware;

import android.util.Log;

public class PublishDiscoverySession extends DiscoverySession {
    private static final String TAG = "PublishDiscoverySession";

    public PublishDiscoverySession(WifiAwareManager wifiAwareManager, int i, int i2) {
        super(wifiAwareManager, i, i2);
    }

    public void updatePublish(PublishConfig publishConfig) {
        if (this.mTerminated) {
            Log.w(TAG, "updatePublish: called on terminated session");
            return;
        }
        WifiAwareManager wifiAwareManager = this.mMgr.get();
        if (wifiAwareManager == null) {
            Log.w(TAG, "updatePublish: called post GC on WifiAwareManager");
        } else {
            wifiAwareManager.updatePublish(this.mClientId, this.mSessionId, publishConfig);
        }
    }
}
