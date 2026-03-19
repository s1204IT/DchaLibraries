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
        if (this.mDataShapingUtils.isLteAccessStratumConnected(intent)) {
            turnStateFromCloseToOpen();
        }
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
        if (!this.mDataShapingUtils.isNetworkTypeLte(intent)) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onSharedDefaultApnStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isSharedDefaultApnEstablished(intent)) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onScreenStateChanged(boolean z) {
        if (z) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onWifiTetherStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isWifiTetheringEnabled(intent)) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onUsbConnectionChanged(Intent intent) {
        if (this.mDataShapingUtils.isUsbConnected(intent)) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onBTStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isBTStateOn(intent)) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onDeviceIdleStateChanged(boolean z) {
        Slog.d(TAG, "[onDeviceIdleStateChanged] DeviceIdle enable is =" + z);
        if (z) {
            turnStateFromCloseToOpenLocked();
        }
    }

    @Override
    public void onAPPStandbyStateChanged(boolean z) {
        Slog.d(TAG, "[onAPPStandbyStateChanged] APPStandby parole state is =" + z);
        if (z) {
            turnStateFromCloseToOpenLocked();
        }
    }

    private void turnStateFromCloseToOpenLocked() {
        Slog.d(TAG, "[turnStateFromCloseToOpenLocked]");
        if (this.mDataShapingUtils.setLteUplinkDataTransfer(true, DataShapingServiceImpl.GATE_CLOSE_SAFE_TIMER)) {
            this.mDataShapingManager.setCurrentState(1);
        } else {
            Slog.d(TAG, "[turnStateFromCloseToOpenLocked] fail!");
        }
        cancelCloseTimer();
    }

    private void turnStateFromCloseToOpen() {
        Slog.d(TAG, "[turnStateFromCloseToOpen]");
        if (this.mDataShapingUtils.setLteUplinkDataTransfer(true, DataShapingServiceImpl.GATE_CLOSE_SAFE_TIMER)) {
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
