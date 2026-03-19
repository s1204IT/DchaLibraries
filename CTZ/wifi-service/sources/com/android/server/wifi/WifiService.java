package com.android.server.wifi;

import android.content.Context;
import android.util.Log;
import com.android.server.SystemService;
import com.android.server.wifi.util.WifiAsyncChannel;

public final class WifiService extends SystemService {
    private static final String TAG = "WifiService";
    final WifiServiceImpl mImpl;

    public WifiService(Context context) {
        super(context);
        this.mImpl = new WifiServiceImpl(context, new WifiInjector(context), new WifiAsyncChannel(TAG));
    }

    public void onStart() {
        Log.i(TAG, "Registering wifi");
        publishBinderService("wifi", this.mImpl);
    }

    public void onBootPhase(int i) {
        if (i == 500) {
            this.mImpl.checkAndStartWifi();
        }
    }

    public void onSwitchUser(int i) {
        this.mImpl.handleUserSwitch(i);
    }

    public void onUnlockUser(int i) {
        this.mImpl.handleUserUnlock(i);
    }

    public void onStopUser(int i) {
        this.mImpl.handleUserStop(i);
    }
}
