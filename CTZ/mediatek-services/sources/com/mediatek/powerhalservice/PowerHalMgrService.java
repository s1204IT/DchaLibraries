package com.mediatek.powerhalservice;

import android.content.Context;
import android.util.Slog;
import com.android.server.SystemService;

public class PowerHalMgrService extends SystemService {
    private final String TAG;
    private PowerHalMgrServiceImpl mPowerHalMgrServiceImpl;

    public PowerHalMgrService(Context context) {
        super(context);
        this.TAG = "PowerHalMgrService";
        this.mPowerHalMgrServiceImpl = new PowerHalMgrServiceImpl();
    }

    public void onStart() {
        Slog.d("PowerHalMgrService", "Start PowerHalMgrService.");
        publishBinderService("power_hal_mgr_service", this.mPowerHalMgrServiceImpl);
    }

    public void onBootPhase(int i) {
        if (i == 500) {
            Slog.d("PowerHalMgrService", "onBootPhase PowerHalMgrService.");
        }
    }
}
