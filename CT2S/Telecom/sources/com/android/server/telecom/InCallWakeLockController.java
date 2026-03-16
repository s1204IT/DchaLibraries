package com.android.server.telecom;

import android.content.Context;
import android.os.PowerManager;

class InCallWakeLockController extends CallsManagerListenerBase {
    private final CallsManager mCallsManager;
    private final Context mContext;
    private final PowerManager.WakeLock mFullWakeLock;

    InCallWakeLockController(Context context, CallsManager callsManager) {
        this.mContext = context;
        this.mCallsManager = callsManager;
        this.mFullWakeLock = ((PowerManager) this.mContext.getSystemService("power")).newWakeLock(26, "InCallWakeLockContoller");
        callsManager.addListener(this);
    }

    @Override
    public void onCallAdded(Call call) {
        handleWakeLock();
    }

    @Override
    public void onCallRemoved(Call call) {
        handleWakeLock();
    }

    @Override
    public void onCallStateChanged(Call call, int i, int i2) {
        handleWakeLock();
    }

    private void handleWakeLock() {
        if (this.mCallsManager.getRingingCall() != null) {
            this.mFullWakeLock.acquire();
            Log.i(this, "Acquiring full wake lock", new Object[0]);
        } else if (this.mFullWakeLock.isHeld()) {
            this.mFullWakeLock.release();
            Log.i(this, "Releasing full wake lock", new Object[0]);
        }
    }
}
