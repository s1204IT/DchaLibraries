package com.android.keyguard;

import android.os.SystemClock;
import com.android.internal.telephony.IccCardConstants;
import com.android.keyguard.KeyguardUpdateMonitor;

public class KeyguardUpdateMonitorCallback {
    private boolean mShowing;
    private long mVisibilityChangedCalled;

    public void onRefreshBatteryInfo(KeyguardUpdateMonitor.BatteryStatus batteryStatus) {
    }

    public void onTimeChanged() {
    }

    public void onRefreshCarrierInfo() {
    }

    public void onRingerModeChanged(int i) {
    }

    public void onPhoneStateChanged(int i) {
    }

    public void onKeyguardVisibilityChanged(boolean z) {
    }

    public void onKeyguardVisibilityChangedRaw(boolean z) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        if (z == this.mShowing && jElapsedRealtime - this.mVisibilityChangedCalled < 1000) {
            return;
        }
        onKeyguardVisibilityChanged(z);
        this.mVisibilityChangedCalled = jElapsedRealtime;
        this.mShowing = z;
    }

    public void onKeyguardBouncerChanged(boolean z) {
    }

    public void onClockVisibilityChanged() {
    }

    public void onDeviceProvisioned() {
    }

    public void onDevicePolicyManagerStateChanged() {
    }

    public void onUserSwitching(int i) {
    }

    public void onUserSwitchComplete(int i) {
    }

    public void onSimStateChangedUsingPhoneId(int i, IccCardConstants.State state) {
    }

    public void onUserInfoChanged(int i) {
    }

    public void onUserUnlocked() {
    }

    public void onBootCompleted() {
    }

    public void onEmergencyCallAction() {
    }

    @Deprecated
    public void onStartedWakingUp() {
    }

    @Deprecated
    public void onStartedGoingToSleep(int i) {
    }

    @Deprecated
    public void onFinishedGoingToSleep(int i) {
    }

    @Deprecated
    public void onScreenTurnedOn() {
    }

    @Deprecated
    public void onScreenTurnedOff() {
    }

    public void onTrustChanged(int i) {
    }

    public void onTrustManagedChanged(int i) {
    }

    public void onTrustGrantedWithFlags(int i, int i2) {
    }

    public void onFingerprintAcquired() {
    }

    public void onFingerprintAuthFailed() {
    }

    public void onFingerprintAuthenticated(int i) {
    }

    public void onFingerprintHelp(int i, String str) {
    }

    public void onFingerprintError(int i, String str) {
    }

    public void onFaceUnlockStateChanged(boolean z, int i) {
    }

    public void onFingerprintRunningStateChanged(boolean z) {
    }

    public void onStrongAuthStateChanged(int i) {
    }

    public void onHasLockscreenWallpaperChanged(boolean z) {
    }

    public void onDreamingStateChanged(boolean z) {
    }

    public void onTrustAgentErrorMessage(CharSequence charSequence) {
    }

    public void onLogoutEnabledChanged() {
    }

    public void onAirPlaneModeChanged(boolean z) {
    }
}
