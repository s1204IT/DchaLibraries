package com.android.server.wifi;

import android.content.Context;
import android.util.Log;
import com.android.server.SystemService;

public class WifiScanningService extends SystemService {
    private static final String TAG = "WifiScanningService";
    WifiScanningServiceImpl mImpl;

    public WifiScanningService(Context context) {
        super(context);
        Log.i(TAG, "Creating wifiscanner");
    }

    public void onStart() {
        this.mImpl = new WifiScanningServiceImpl(getContext());
        Log.i(TAG, "Starting wifiscanner");
        publishBinderService("wifiscanner", this.mImpl);
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            Log.i(TAG, "Registering wifiscanner");
            if (this.mImpl == null) {
                this.mImpl = new WifiScanningServiceImpl(getContext());
            }
            this.mImpl.startService(getContext());
        }
    }
}
