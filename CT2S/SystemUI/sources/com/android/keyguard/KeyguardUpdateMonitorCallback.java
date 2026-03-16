package com.android.keyguard;

import android.graphics.Bitmap;
import android.os.SystemClock;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;

public class KeyguardUpdateMonitorCallback {
    private boolean mShowing;
    private long mVisibilityChangedCalled;

    public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus status) {
    }

    public void onTimeChanged() {
    }

    public void onRefreshCarrierInfo() {
    }

    public void onRingerModeChanged(int state) {
    }

    public void onPhoneStateChanged(int phoneState) {
    }

    public void onKeyguardVisibilityChanged(boolean showing) {
    }

    public void onKeyguardVisibilityChangedRaw(boolean showing) {
        long now = SystemClock.elapsedRealtime();
        if (showing != this.mShowing || now - this.mVisibilityChangedCalled >= 1000) {
            onKeyguardVisibilityChanged(showing);
            this.mVisibilityChangedCalled = now;
            this.mShowing = showing;
        }
    }

    public void onKeyguardBouncerChanged(boolean bouncer) {
    }

    public void onClockVisibilityChanged() {
    }

    public void onDeviceProvisioned() {
    }

    public void onDevicePolicyManagerStateChanged() {
    }

    public void onUserSwitching(int userId) {
    }

    public void onUserSwitchComplete(int userId) {
    }

    public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
    }

    public void onUserRemoved(int userId) {
    }

    public void onUserInfoChanged(int userId) {
    }

    public void onBootCompleted() {
    }

    public void onEmergencyCallAction() {
    }

    public void onSetBackground(Bitmap bitmap) {
    }

    public void onScreenTurnedOn() {
    }

    public void onScreenTurnedOff(int why) {
    }

    public void onTrustChanged(int userId) {
    }

    public void onTrustManagedChanged(int userId) {
    }

    public void onTrustInitiatedByUser(int userId) {
    }

    public void onFingerprintRecognized(int userId) {
    }

    public void onFingerprintAcquired(int info) {
    }

    public void onFaceUnlockStateChanged(boolean running, int userId) {
    }
}
