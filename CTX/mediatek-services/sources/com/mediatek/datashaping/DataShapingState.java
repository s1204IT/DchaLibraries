package com.mediatek.datashaping;

import android.content.Context;
import android.content.Intent;

public abstract class DataShapingState {
    public DataShapingServiceImpl mDataShapingManager;
    public DataShapingUtils mDataShapingUtils;

    public DataShapingState(DataShapingServiceImpl dataShapingServiceImpl, Context context) {
        this.mDataShapingManager = dataShapingServiceImpl;
        this.mDataShapingUtils = DataShapingUtils.getInstance(context);
    }

    public void onLteAccessStratumStateChanged(Intent intent) {
    }

    public void onNetworkTypeChanged(Intent intent) {
    }

    public void onAlarmManagerTrigger() {
    }

    public void onCloseTimeExpired() {
    }

    public void onScreenStateChanged(boolean z) {
    }

    public void onSharedDefaultApnStateChanged(Intent intent) {
    }

    public void onUsbTetherStateChanged() {
    }

    public void onWifiTetherStateChanged(Intent intent) {
    }

    public void onUsbConnectionChanged(Intent intent) {
    }

    public void onMediaButtonTrigger() {
    }

    public void onBTStateChanged(Intent intent) {
    }

    public void onDeviceIdleStateChanged(boolean z) {
    }

    public void onAPPStandbyStateChanged(boolean z) {
    }
}
