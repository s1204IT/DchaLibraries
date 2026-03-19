package com.mediatek.datashaping;

import android.content.Context;
import android.content.Intent;
import android.util.Slog;

public class GateOpenState extends DataShapingState {
    private static final String TAG = "GateOpenState";

    public GateOpenState(DataShapingServiceImpl dataShapingServiceImpl, Context context) {
        super(dataShapingServiceImpl, context);
    }

    @Override
    public void onLteAccessStratumStateChanged(Intent intent) {
        if (!this.mDataShapingUtils.isLteAccessStratumConnected(intent)) {
            turnStateFromOpenToClose();
        }
    }

    @Override
    public void onNetworkTypeChanged(Intent intent) {
        if (!this.mDataShapingUtils.isNetworkTypeLte(intent)) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onSharedDefaultApnStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isSharedDefaultApnEstablished(intent)) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onScreenStateChanged(boolean z) {
        if (z) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onWifiTetherStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isWifiTetheringEnabled(intent)) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onUsbConnectionChanged(Intent intent) {
        if (this.mDataShapingUtils.isUsbConnected(intent)) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onBTStateChanged(Intent intent) {
        if (this.mDataShapingUtils.isBTStateOn(intent)) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onDeviceIdleStateChanged(boolean z) {
        Slog.d(TAG, "[onDeviceIdleStateChanged] DeviceIdle enable is =" + z);
        if (z) {
            turnStateFromOpenToOpenLocked();
        }
    }

    @Override
    public void onAPPStandbyStateChanged(boolean z) {
        Slog.d(TAG, "[onAPPStandbyStateChanged] APPStandby parole state is =" + z);
        if (z) {
            turnStateFromOpenToOpenLocked();
        }
    }

    private void turnStateFromOpenToOpenLocked() {
        this.mDataShapingManager.setCurrentState(1);
    }

    private void turnStateFromOpenToClose() {
        if (this.mDataShapingUtils.isMusicActive()) {
            this.mDataShapingUtils.setClosingDelayForMusic(true);
            Slog.d(TAG, "[turnStateFromOpenToClose] music active, so still in open state!");
            return;
        }
        if (this.mDataShapingUtils.getClosingDelayForMusic()) {
            this.mDataShapingUtils.setClosingDelayStartTime(System.currentTimeMillis());
            this.mDataShapingUtils.setClosingDelayForMusic(false);
            Slog.d(TAG, "[turnStateFromOpenToClose] mIsClosingDelayForMusic is true, so still in open state!");
        } else {
            if (System.currentTimeMillis() - this.mDataShapingUtils.getClosingDelayStartTime() < DataShapingUtils.CLOSING_DELAY_BUFFER_FOR_MUSIC) {
                Slog.d(TAG, "[turnStateFromOpenToClose] close delay < buffer time, so still in open state!");
                return;
            }
            this.mDataShapingUtils.setClosingDelayStartTime(0L);
            if (!this.mDataShapingManager.registerListener()) {
                Slog.d(TAG, "[turnStateFromOpenToClose] registerListener Failed so still in open state!");
            } else if (this.mDataShapingUtils.setLteUplinkDataTransfer(false, DataShapingServiceImpl.GATE_CLOSE_SAFE_TIMER)) {
                this.mDataShapingManager.setCurrentState(3);
                this.mDataShapingManager.startCloseExpiredAlarm();
            } else {
                Slog.d(TAG, "[turnStateFromOpenToClose] fail!");
            }
        }
    }
}
