package com.mediatek.datashaping;

import android.content.Context;
import android.content.Intent;
import android.util.Slog;

public class GateCloseState extends DataShapingState {
    private static final String TAG = "GateCloseState";

    public GateCloseState(DataShapingServiceImpl dataShapingServiceImpl, Context context) {
        super(dataShapingServiceImpl, context);
    }

    @Override
    public void onLteAccessStratumStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isLteAccessStratumConnected(intent)) {
            return;
        }
        turnStateFromCloseToOpen();
    }

    @Override
    public void onMediaButtonTrigger() {
        Slog.d(TAG, "[onMediaButtonTrigger]");
        turnStateFromCloseToOpen();
    }

    @Override
    public void onAlarmManagerTrigger() {
        turnStateFromCloseToOpen();
    }

    @Override
    public void onCloseTimeExpired() {
        turnStateFromCloseToOpen();
    }

    @Override
    public void onNetworkTypeChanged(Intent intent) {
        if (this.mDataShapingUtils.isNetworkTypeLte(intent)) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onSharedDefaultApnStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isSharedDefaultApnEstablished(intent)) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onScreenStateChanged(boolean isOn) {
        if (!isOn) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onWifiTetherStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isWifiTetheringEnabled(intent)) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onUsbConnectionChanged(Intent intent) {
        if (!this.mDataShapingUtils.isUsbConnected(intent)) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onBTStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isBTStateOn(intent)) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onDeviceIdleStateChanged(boolean enabled) {
        Slog.d(TAG, "[onDeviceIdleStateChanged] DeviceIdle enable is =" + enabled);
        if (!enabled) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onAPPStandbyStateChanged(boolean isParoleOn) {
        Slog.d(TAG, "[onAPPStandbyStateChanged] APPStandby parole state is =" + isParoleOn);
        if (!isParoleOn) {
            return;
        }
        turnStateFromCloseToOpenLocked();
    }

    @Override
    public void onInputFilterStateChanged(boolean isInstall) {
        Slog.d(TAG, "[onInputFilterStateChanged] InputFilter install state is =" + isInstall);
        if (isInstall) {
            return;
        }
        turnStateFromCloseToOpen();
    }

    private void turnStateFromCloseToOpenLocked() {
        Slog.d(TAG, "[turnStateFromCloseToOpenLocked]");
        if (this.mDataShapingUtils.setLteUplinkDataTransfer(true, 600000)) {
            this.mDataShapingManager.setCurrentState(1);
        } else {
            Slog.d(TAG, "[turnStateFromCloseToOpenLocked] fail!");
        }
        cancelCloseTimer();
    }

    private void turnStateFromCloseToOpen() {
        Slog.d(TAG, "[turnStateFromCloseToOpen]");
        if (this.mDataShapingUtils.setLteUplinkDataTransfer(true, 600000)) {
            this.mDataShapingManager.setCurrentState(2);
        } else {
            Slog.d(TAG, "[turnStateFromCloseToOpen] fail!");
        }
        cancelCloseTimer();
    }

    private void cancelCloseTimer() {
        this.mDataShapingManager.cancelCloseExpiredAlarm();
    }
}
