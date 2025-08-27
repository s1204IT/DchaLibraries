package com.android.keyguard;

import android.content.Context;
import android.util.Log;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;

/* loaded from: classes.dex */
public class KeyguardSecurityModel {
    private final Context mContext;
    private final boolean mIsPukScreenAvailable;
    private LockPatternUtils mLockPatternUtils;

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
        AntiTheft,
        AlarmBoot
    }

    public KeyguardSecurityModel(Context context) {
        this.mContext = context;
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mIsPukScreenAvailable = this.mContext.getResources().getBoolean(android.R.^attr-private.floatingToolbarOpenDrawable);
    }

    void setLockPatternUtils(LockPatternUtils lockPatternUtils) {
        this.mLockPatternUtils = lockPatternUtils;
    }

    public SecurityMode getSecurityMode(int i) {
        KeyguardUpdateMonitor.getInstance(this.mContext);
        SecurityMode securityMode = SecurityMode.None;
        if (PowerOffAlarmManager.isAlarmBoot()) {
            securityMode = SecurityMode.AlarmBoot;
        } else {
            int i2 = 0;
            while (true) {
                if (i2 >= KeyguardUtils.getNumOfPhone()) {
                    break;
                }
                if (!isPinPukOrMeRequiredOfPhoneId(i2)) {
                    i2++;
                } else if (i2 == 0) {
                    securityMode = SecurityMode.SimPinPukMe1;
                } else if (1 == i2) {
                    securityMode = SecurityMode.SimPinPukMe2;
                } else if (2 == i2) {
                    securityMode = SecurityMode.SimPinPukMe3;
                } else if (3 == i2) {
                    securityMode = SecurityMode.SimPinPukMe4;
                }
            }
        }
        if (AntiTheftManager.isAntiTheftPriorToSecMode(securityMode)) {
            Log.d("KeyguardSecurityModel", "should show AntiTheft!");
            securityMode = SecurityMode.AntiTheft;
        }
        if (securityMode == SecurityMode.None) {
            int activePasswordQuality = this.mLockPatternUtils.getActivePasswordQuality(i);
            if (activePasswordQuality == 0) {
                return SecurityMode.None;
            }
            if (activePasswordQuality == 65536) {
                return SecurityMode.Pattern;
            }
            if (activePasswordQuality == 131072 || activePasswordQuality == 196608) {
                return SecurityMode.PIN;
            }
            if (activePasswordQuality == 262144 || activePasswordQuality == 327680 || activePasswordQuality == 393216 || activePasswordQuality == 524288) {
                return SecurityMode.Password;
            }
            throw new IllegalStateException("Unknown security quality:" + activePasswordQuality);
        }
        Log.d("KeyguardSecurityModel", "getSecurityMode() - mode = " + securityMode);
        return securityMode;
    }

    public boolean isPinPukOrMeRequiredOfPhoneId(int i) {
        KeyguardUpdateMonitor keyguardUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        if (keyguardUpdateMonitor == null) {
            return false;
        }
        IccCardConstants.State simStateOfPhoneId = keyguardUpdateMonitor.getSimStateOfPhoneId(i);
        Log.d("KeyguardSecurityModel", "isPinPukOrMeRequiredOfSubId() - phoneId = " + i + ", simState = " + simStateOfPhoneId);
        return (simStateOfPhoneId == IccCardConstants.State.PIN_REQUIRED && !keyguardUpdateMonitor.getPinPukMeDismissFlagOfPhoneId(i)) || !(simStateOfPhoneId != IccCardConstants.State.PUK_REQUIRED || keyguardUpdateMonitor.getPinPukMeDismissFlagOfPhoneId(i) || keyguardUpdateMonitor.getRetryPukCountOfPhoneId(i) == 0) || (simStateOfPhoneId == IccCardConstants.State.NETWORK_LOCKED && !keyguardUpdateMonitor.getPinPukMeDismissFlagOfPhoneId(i) && keyguardUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(i) != 0 && KeyguardUtils.isMediatekSimMeLockSupport() && !keyguardUpdateMonitor.getSimmeDismissFlagOfPhoneId(i) && KeyguardUtils.isSimMeLockValid(i));
    }

    int getPhoneIdUsingSecurityMode(SecurityMode securityMode) {
        if (isSimPinPukSecurityMode(securityMode)) {
            return securityMode.ordinal() - SecurityMode.SimPinPukMe1.ordinal();
        }
        return -1;
    }

    boolean isSimPinPukSecurityMode(SecurityMode securityMode) {
        switch (securityMode) {
            case SimPinPukMe1:
            case SimPinPukMe2:
            case SimPinPukMe3:
            case SimPinPukMe4:
                return true;
            default:
                return false;
        }
    }
}
