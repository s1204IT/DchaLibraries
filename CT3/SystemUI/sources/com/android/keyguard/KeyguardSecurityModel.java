package com.android.keyguard;

import android.R;
import android.content.Context;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

public class KeyguardSecurityModel {

    private static final int[] f4x1cbe7e58 = null;
    private final Context mContext;
    private final boolean mIsPukScreenAvailable;
    private LockPatternUtils mLockPatternUtils;

    private static int[] m464xec5d63fc() {
        if (f4x1cbe7e58 != null) {
            return f4x1cbe7e58;
        }
        int[] iArr = new int[SecurityMode.valuesCustom().length];
        try {
            iArr[SecurityMode.AlarmBoot.ordinal()] = 7;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SecurityMode.AntiTheft.ordinal()] = 8;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SecurityMode.Biometric.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[SecurityMode.Invalid.ordinal()] = 9;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[SecurityMode.None.ordinal()] = 10;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[SecurityMode.PIN.ordinal()] = 11;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[SecurityMode.Password.ordinal()] = 12;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[SecurityMode.Pattern.ordinal()] = 13;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe1.ordinal()] = 2;
        } catch (NoSuchFieldError e9) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe2.ordinal()] = 3;
        } catch (NoSuchFieldError e10) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe3.ordinal()] = 4;
        } catch (NoSuchFieldError e11) {
        }
        try {
            iArr[SecurityMode.SimPinPukMe4.ordinal()] = 5;
        } catch (NoSuchFieldError e12) {
        }
        try {
            iArr[SecurityMode.Voice.ordinal()] = 6;
        } catch (NoSuchFieldError e13) {
        }
        f4x1cbe7e58 = iArr;
        return iArr;
    }

    public enum SecurityMode {
        Invalid,
        None,
        Pattern,
        Password,
        PIN,
        SimPinPukMe1,
        SimPinPukMe2,
        SimPinPukMe3,
        SimPinPukMe4,
        AlarmBoot,
        Biometric,
        Voice,
        AntiTheft;

        public static SecurityMode[] valuesCustom() {
            return values();
        }
    }

    public KeyguardSecurityModel(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mIsPukScreenAvailable = this.mContext.getResources().getBoolean(R.^attr-private.enableSubtitle);
    }

    void setLockPatternUtils(LockPatternUtils utils) {
        this.mLockPatternUtils = utils;
    }

    public SecurityMode getSecurityMode() {
        Log.d("KeyguardSecurityModel", "getSecurityMode() is called.");
        KeyguardUpdateMonitor.getInstance(this.mContext);
        SecurityMode mode = SecurityMode.None;
        if (PowerOffAlarmManager.isAlarmBoot()) {
            mode = SecurityMode.AlarmBoot;
        } else {
            int i = 0;
            while (true) {
                if (i >= KeyguardUtils.getNumOfPhone()) {
                    break;
                }
                if (!isPinPukOrMeRequiredOfPhoneId(i)) {
                    i++;
                } else if (i == 0) {
                    mode = SecurityMode.SimPinPukMe1;
                } else if (1 == i) {
                    mode = SecurityMode.SimPinPukMe2;
                } else if (2 == i) {
                    mode = SecurityMode.SimPinPukMe3;
                } else if (3 == i) {
                    mode = SecurityMode.SimPinPukMe4;
                }
            }
        }
        if (AntiTheftManager.isAntiTheftPriorToSecMode(mode)) {
            Log.d("KeyguardSecurityModel", "should show AntiTheft!");
            mode = SecurityMode.AntiTheft;
        }
        if (mode == SecurityMode.None) {
            int security = this.mLockPatternUtils.getActivePasswordQuality(KeyguardUpdateMonitor.getCurrentUser());
            Log.d("KeyguardSecurityModel", "getSecurityMode() - security = " + security);
            switch (security) {
                case 0:
                    return SecurityMode.None;
                case 65536:
                    return SecurityMode.Pattern;
                case 131072:
                case 196608:
                    return SecurityMode.PIN;
                case 262144:
                case 327680:
                case 393216:
                    return SecurityMode.Password;
                default:
                    throw new IllegalStateException("Unknown security quality:" + security);
            }
        }
        Log.d("KeyguardSecurityModel", "getSecurityMode() - mode = " + mode);
        return mode;
    }

    public boolean isPinPukOrMeRequiredOfPhoneId(int phoneId) {
        KeyguardUpdateMonitor updateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if (updateMonitor == null) {
            return false;
        }
        IccCardConstants.State simState = updateMonitor.getSimStateOfPhoneId(phoneId);
        Log.d("KeyguardSecurityModel", "isPinPukOrMeRequiredOfSubId() - phoneId = " + phoneId + ", simState = " + simState);
        if ((simState == IccCardConstants.State.PIN_REQUIRED && !updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId)) || (simState == IccCardConstants.State.PUK_REQUIRED && !updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId) && updateMonitor.getRetryPukCountOfPhoneId(phoneId) != 0)) {
            return true;
        }
        if (simState != IccCardConstants.State.NETWORK_LOCKED || updateMonitor.getPinPukMeDismissFlagOfPhoneId(phoneId) || updateMonitor.getSimMeLeftRetryCountOfPhoneId(phoneId) == 0) {
            return false;
        }
        return KeyguardUtils.isMediatekSimMeLockSupport();
    }

    int getPhoneIdUsingSecurityMode(SecurityMode mode) {
        if (!isSimPinPukSecurityMode(mode)) {
            return -1;
        }
        int phoneId = mode.ordinal() - SecurityMode.SimPinPukMe1.ordinal();
        return phoneId;
    }

    boolean isSimPinPukSecurityMode(SecurityMode mode) {
        switch (m464xec5d63fc()[mode.ordinal()]) {
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    boolean isBiometricUnlockEnabled() {
        return this.mLockPatternUtils.usingBiometricWeak();
    }

    private boolean isBiometricUnlockSuppressed() {
        KeyguardUpdateMonitor monitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        int userId = KeyguardUpdateMonitor.getCurrentUser();
        boolean backupIsTimedOut = monitor.getFailedUnlockAttempts(userId) >= 4;
        return monitor.getMaxBiometricUnlockAttemptsReached() || backupIsTimedOut || !monitor.isAlternateUnlockEnabled() || monitor.getPhoneState() != 0;
    }

    SecurityMode getAlternateFor(SecurityMode mode) {
        if (!isBiometricUnlockSuppressed() && (mode == SecurityMode.Password || mode == SecurityMode.PIN || mode == SecurityMode.Pattern)) {
            if (isBiometricUnlockEnabled()) {
                return SecurityMode.Biometric;
            }
            if (this.mLockPatternUtils.usingVoiceWeak()) {
                return SecurityMode.Voice;
            }
        }
        return mode;
    }
}
