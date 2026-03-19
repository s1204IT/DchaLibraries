package com.mediatek.datashaping;

import android.content.Context;
import android.content.Intent;
import android.util.Slog;

public class GateOpenLockedState extends DataShapingState {
    private static final String TAG = "GateOpenLockedState";

    public GateOpenLockedState(DataShapingServiceImpl dataShapingServiceImpl, Context context) {
        super(dataShapingServiceImpl, context);
    }

    @Override
    public void onNetworkTypeChanged(Intent intent) {
        if (this.mDataShapingUtils.isNetworkTypeLte(intent)) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onSharedDefaultApnStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isSharedDefaultApnEstablished(intent)) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onScreenStateChanged(boolean z) {
        if (!z) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onWifiTetherStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isWifiTetheringEnabled(intent)) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onUsbConnectionChanged(Intent intent) {
        if (!this.mDataShapingUtils.isUsbConnected(intent)) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onBTStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isBTStateOn(intent)) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onDeviceIdleStateChanged(boolean z) {
        Slog.d(TAG, "[onDeviceIdleStateChanged] DeviceIdle enable is =" + z);
        if (!z) {
            setStateFromLockedToOpen();
        }
    }

    @Override
    public void onAPPStandbyStateChanged(boolean z) {
        Slog.d(TAG, "[onAPPStandbyStateChanged] APPStandby parole state is =" + z);
        if (!z) {
            setStateFromLockedToOpen();
        }
    }

    private void setStateFromLockedToOpen() {
        if (this.mDataShapingUtils.canTurnFromLockedToOpen() && this.mDataShapingUtils.setLteAccessStratumReport(true)) {
            this.mDataShapingManager.setCurrentState(2);
        } else {
            Slog.d(TAG, "Still stay in Open Locked state!");
        }
    }
}
