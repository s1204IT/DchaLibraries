package com.android.keyguard;

import android.content.Context;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;

public class KeyguardSecurityModel {
    private Context mContext;
    private LockPatternUtils mLockPatternUtils;

    public enum SecurityMode {
        Invalid,
        None,
        Pattern,
        Password,
        PIN,
        Biometric,
        Account,
        SimPin,
        SimPuk
    }

    KeyguardSecurityModel(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
    }

    void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    boolean isBiometricUnlockEnabled() {
        return this.mLockPatternUtils.usingBiometricWeak() && this.mLockPatternUtils.isBiometricWeakInstalled();
    }

    private boolean isBiometricUnlockSuppressed() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        boolean backupIsTimedOut = monitor.getFailedUnlockAttempts() >= 5;
        return monitor.getMaxBiometricUnlockAttemptsReached() || backupIsTimedOut || !monitor.isAlternateUnlockEnabled() || monitor.getPhoneState() != 0;
    }

    SecurityMode getSecurityMode() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        SecurityMode mode = SecurityMode.None;
        if (SubscriptionManager.isValidSubscriptionId(monitor.getNextSubIdForState(IccCardConstants.State.PIN_REQUIRED))) {
            SecurityMode mode2 = SecurityMode.SimPin;
            return mode2;
        }
        if (SubscriptionManager.isValidSubscriptionId(monitor.getNextSubIdForState(IccCardConstants.State.PUK_REQUIRED)) && this.mLockPatternUtils.isPukUnlockScreenEnable()) {
            SecurityMode mode3 = SecurityMode.SimPuk;
            return mode3;
        }
        int security = this.mLockPatternUtils.getKeyguardStoredPasswordQuality();
        switch (security) {
            case 0:
            case 65536:
                if (this.mLockPatternUtils.isLockPatternEnabled()) {
                    if (this.mLockPatternUtils.isPermanentlyLocked()) {
                        SecurityMode mode4 = SecurityMode.Account;
                        return mode4;
                    }
                    SecurityMode mode5 = SecurityMode.Pattern;
                    return mode5;
                }
                return mode;
            case 131072:
            case 196608:
                if (!this.mLockPatternUtils.isLockPasswordEnabled()) {
                    SecurityMode mode6 = SecurityMode.None;
                    return mode6;
                }
                SecurityMode mode7 = SecurityMode.PIN;
                return mode7;
            case 262144:
            case 327680:
            case 393216:
                if (!this.mLockPatternUtils.isLockPasswordEnabled()) {
                    SecurityMode mode8 = SecurityMode.None;
                    return mode8;
                }
                SecurityMode mode9 = SecurityMode.Password;
                return mode9;
            default:
                throw new IllegalStateException("Unknown security quality:" + security);
        }
    }

    SecurityMode getAlternateFor(SecurityMode mode) {
        if (!isBiometricUnlockEnabled() || isBiometricUnlockSuppressed()) {
            return mode;
        }
        if (mode == SecurityMode.Password || mode == SecurityMode.PIN || mode == SecurityMode.Pattern) {
            return SecurityMode.Biometric;
        }
        return mode;
    }

    SecurityMode getBackupSecurityMode(SecurityMode mode) {
        switch (mode) {
            case Biometric:
                return getSecurityMode();
            case Pattern:
                return SecurityMode.Account;
            default:
                return mode;
        }
    }
}
