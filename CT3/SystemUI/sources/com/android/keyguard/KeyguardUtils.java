package com.android.keyguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

public class KeyguardUtils {
    private SubscriptionManager mSubscriptionManager;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private static final boolean mIsOwnerSdcardOnlySupport = SystemProperties.get("ro.mtk_owner_sdcard_support").equals("1");
    private static final boolean mIsPrivacyProtectionLockSupport = SystemProperties.get("ro.mtk_privacy_protection_lock").equals("1");
    private static final boolean mIsMediatekSimMeLockSupport = SystemProperties.get("ro.sim_me_lock_mode", "0").equals("0");
    private static int sPhoneCount = 0;

    public KeyguardUtils(Context context) {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    public String getOptrNameUsingPhoneId(int phoneId, Context context) {
        int subId = getSubIdUsingPhoneId(phoneId);
        SubscriptionInfo info = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            Log.d("KeyguardUtils", "getOptrNameUsingPhoneId, return null");
        } else {
            Log.d("KeyguardUtils", "getOptrNameUsingPhoneId mDisplayName=" + info.getDisplayName());
            if (info.getDisplayName() != null) {
                return info.getDisplayName().toString();
            }
        }
        return null;
    }

    public Bitmap getOptrBitmapUsingPhoneId(int phoneId, Context context) {
        int subId = getSubIdUsingPhoneId(phoneId);
        SubscriptionInfo info = this.mSubscriptionManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            Log.d("KeyguardUtils", "getOptrBitmapUsingPhoneId, return null");
            return null;
        }
        Bitmap bgBitmap = info.createIconBitmap(context);
        return bgBitmap;
    }

    public static final boolean isPrivacyProtectionLockSupport() {
        return mIsPrivacyProtectionLockSupport;
    }

    public static final boolean isVoiceWakeupSupport(Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        if (am == null) {
            Log.d("KeyguardUtils", "isVoiceWakeupSupport() - get AUDIO_SERVICE fails, return false.");
            return false;
        }
        String val = am.getParameters("MTK_VOW_SUPPORT");
        if (val != null) {
            return val.equalsIgnoreCase("MTK_VOW_SUPPORT=true");
        }
        return false;
    }

    public static final boolean isMediatekSimMeLockSupport() {
        return mIsMediatekSimMeLockSupport;
    }

    public static void requestImeStatusRefresh(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService("input_method");
        if (imm == null) {
            return;
        }
        Log.d("KeyguardUtils", "call imm.requestImeStatusRefresh()");
        imm.refreshImeWindowVisibility();
    }

    public static boolean isFlightModePowerOffMd() {
        boolean powerOffMd = SystemProperties.get("ro.mtk_flight_mode_power_off_md").equals("1");
        Log.d("KeyguardUtils", "powerOffMd = " + powerOffMd);
        return powerOffMd;
    }

    public static int getNumOfPhone() {
        if (sPhoneCount == 0) {
            sPhoneCount = TelephonyManager.getDefault().getPhoneCount();
            sPhoneCount = sPhoneCount <= 4 ? sPhoneCount : 4;
        }
        return sPhoneCount;
    }

    public static boolean isValidPhoneId(int phoneId) {
        return phoneId != Integer.MAX_VALUE && phoneId >= 0 && phoneId < getNumOfPhone();
    }

    public static int getPhoneIdUsingSubId(int subId) {
        Log.e("KeyguardUtils", "getPhoneIdUsingSubId: subId = " + subId);
        int phoneId = SubscriptionManager.getPhoneId(subId);
        if (phoneId < 0 || phoneId >= getNumOfPhone()) {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: invalid phonId = " + phoneId);
        } else {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: get phone ID = " + phoneId);
        }
        return phoneId;
    }

    public static int getSubIdUsingPhoneId(int phoneId) {
        int subId = SubscriptionManager.getSubIdUsingPhoneId(phoneId);
        Log.d("KeyguardUtils", "getSubIdUsingPhoneId(phoneId = " + phoneId + ") = " + subId);
        return subId;
    }

    public static boolean isSystemEncrypted() {
        boolean bRet = true;
        String cryptoType = SystemProperties.get("ro.crypto.type");
        String state = SystemProperties.get("ro.crypto.state");
        String decrypt = SystemProperties.get("vold.decrypt");
        if ("unsupported".equals(state)) {
            return false;
        }
        if ("unencrypted".equals(state)) {
            if ("".equals(decrypt)) {
                bRet = false;
            }
        } else if (!"".equals(state) && "encrypted".equals(state)) {
            if ("file".equals(cryptoType)) {
                bRet = false;
            } else if ("block".equals(cryptoType) && "trigger_restart_framework".equals(decrypt)) {
                bRet = false;
            }
        }
        Log.d("KeyguardUtils", "cryptoType=" + cryptoType + " ro.crypto.state=" + state + " vold.decrypt=" + decrypt + " sysEncrypted=" + bRet);
        return bRet;
    }
}
