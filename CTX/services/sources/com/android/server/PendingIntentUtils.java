package com.android.server;

import android.app.BroadcastOptions;
import android.os.Bundle;

public class PendingIntentUtils {
    public static Bundle createDontSendToRestrictedAppsBundle(Bundle bundle) {
        BroadcastOptions broadcastOptionsMakeBasic = BroadcastOptions.makeBasic();
        broadcastOptionsMakeBasic.setDontSendToRestrictedApps(true);
        if (bundle == null) {
            return broadcastOptionsMakeBasic.toBundle();
        }
        bundle.putAll(broadcastOptionsMakeBasic.toBundle());
        return bundle;
    }

    private PendingIntentUtils() {
    }
}
