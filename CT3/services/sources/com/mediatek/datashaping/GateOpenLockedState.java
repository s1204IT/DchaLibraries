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
        if (!this.mDataShapingUtils.isNetworkTypeLte(intent)) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onSharedDefaultApnStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isSharedDefaultApnEstablished(intent)) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onScreenStateChanged(boolean isOn) {
        if (isOn) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onWifiTetherStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isWifiTetheringEnabled(intent)) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onUsbConnectionChanged(Intent intent) {
        if (this.mDataShapingUtils.isUsbConnected(intent)) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onBTStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isBTStateOn(intent)) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onDeviceIdleStateChanged(boolean enabled) {
        Slog.d(TAG, "[onDeviceIdleStateChanged] DeviceIdle enable is =" + enabled);
        if (enabled) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onAPPStandbyStateChanged(boolean isParoleOn) {
        Slog.d(TAG, "[onAPPStandbyStateChanged] APPStandby parole state is =" + isParoleOn);
        if (isParoleOn) {
            return;
        }
        setStateFromLockedToOpen();
    }

    @Override
    public void onInputFilterStateChanged(boolean isInstall) {
        Slog.d(TAG, "[onInputFilterStateChanged] InputFilter install state is =" + isInstall);
    }

    private void setStateFromLockedToOpen() {
        if (this.mDataShapingUtils.canTurnFromLockedToOpen() && this.mDataShapingUtils.setLteAccessStratumReport(true)) {
            this.mDataShapingManager.setCurrentState(2);
        } else {
            Slog.d(TAG, "Still stay in Open Locked state!");
        }
    }
}
