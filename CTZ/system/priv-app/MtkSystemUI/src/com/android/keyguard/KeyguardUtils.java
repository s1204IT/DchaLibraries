package com.android.keyguard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import com.mediatek.internal.telephony.MtkSubscriptionManager;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.io.File;

/* loaded from: classes.dex */
public class KeyguardUtils {
    private SubscriptionManager mSubscriptionManager;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final boolean mIsPrivacyProtectionLockSupport = SystemProperties.get("ro.vendor.mtk_privacy_protection_lock").equals("1");
    private static KeyguardUtils sInstance = new KeyguardUtils();
    private static boolean mIsMediatekSimMeLockSupport = SystemProperties.get("ro.vendor.sim_me_lock_mode", "0").equals("0");
    private static int sPhoneCount = 0;
    private static boolean mIsDismissEnabled = false;
    private static boolean mIsSimmePolicyEnabled = false;
    private static int mValidPhoneIds = -1;
    private static boolean mIsDismissSimMeLockSupport = false;

    public KeyguardUtils(Context context) {
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        this.mSubscriptionManager = SubscriptionManager.from(context);
    }

    private KeyguardUtils() {
    }

    public static KeyguardUtils getDefault() {
        return sInstance;
    }

    public String getOptrNameUsingPhoneId(int i, Context context) {
        SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(getSubIdUsingPhoneId(i));
        if (activeSubscriptionInfo == null) {
            if (DEBUG) {
                Log.d("KeyguardUtils", "getOptrNameUsingPhoneId, return null");
                return null;
            }
            return null;
        }
        if (DEBUG) {
            Log.d("KeyguardUtils", "getOptrNameUsingPhoneId mDisplayName=" + ((Object) activeSubscriptionInfo.getDisplayName()));
        }
        if (activeSubscriptionInfo.getDisplayName() != null) {
            return activeSubscriptionInfo.getDisplayName().toString();
        }
        return null;
    }

    public Bitmap getOptrBitmapUsingPhoneId(int i, Context context) {
        SubscriptionInfo activeSubscriptionInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(getSubIdUsingPhoneId(i));
        if (activeSubscriptionInfo == null) {
            if (DEBUG) {
                Log.d("KeyguardUtils", "getOptrBitmapUsingPhoneId, return null");
            }
            return null;
        }
        return activeSubscriptionInfo.createIconBitmap(context);
    }

    public static final boolean isMediatekSimMeLockSupport() {
        return mIsMediatekSimMeLockSupport;
    }

    public static final boolean isDismissSimMeLockSupport() {
        return mIsDismissSimMeLockSupport;
    }

    public static boolean isFlightModePowerOffMd() {
        boolean zEquals = SystemProperties.get("ro.vendor.mtk_flight_mode_power_off_md").equals("1");
        Log.d("KeyguardUtils", "powerOffMd = " + zEquals);
        return zEquals;
    }

    public static int getNumOfPhone() {
        if (sPhoneCount == 0) {
            sPhoneCount = TelephonyManager.getDefault().getPhoneCount();
            sPhoneCount = sPhoneCount <= 4 ? sPhoneCount : 4;
        }
        return sPhoneCount;
    }

    public static boolean isValidPhoneId(int i) {
        return i != Integer.MAX_VALUE && i >= 0 && i < getNumOfPhone();
    }

    public static int getPhoneIdUsingSubId(int i) {
        Log.e("KeyguardUtils", "getPhoneIdUsingSubId: subId = " + i);
        int phoneId = SubscriptionManager.getPhoneId(i);
        if (phoneId < 0 || phoneId >= getNumOfPhone()) {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: invalid phonId = " + phoneId);
        } else {
            Log.e("KeyguardUtils", "getPhoneIdUsingSubId: get phone ID = " + phoneId);
        }
        return phoneId;
    }

    public static int getSubIdUsingPhoneId(int i) {
        return MtkSubscriptionManager.getSubIdUsingPhoneId(i);
    }

    public static void requestImeStatusRefresh(Context context) {
        if (((InputMethodManager) context.getSystemService("input_method")) != null && DEBUG) {
            Log.d("KeyguardUtils", "call imm.requestImeStatusRefresh()");
        }
    }

    public static boolean isSystemEncrypted() {
        String str = SystemProperties.get("ro.crypto.type");
        String str2 = SystemProperties.get("ro.crypto.state");
        String str3 = SystemProperties.get("vold.decrypt");
        boolean z = false;
        if ("unsupported".equals(str2)) {
            return false;
        }
        if (!"unencrypted".equals(str2) ? !(!"".equals(str2) && "encrypted".equals(str2) && ("file".equals(str) || ("block".equals(str) && "trigger_restart_framework".equals(str3)))) : !"".equals(str3)) {
            z = true;
        }
        if (DEBUG) {
            Log.d("KeyguardUtils", "cryptoType=" + str + " ro.crypto.state=" + str2 + " vold.decrypt=" + str3 + " sysEncrypted=" + z);
        }
        return z;
    }

    public static final boolean isPrivacyProtectionLockSupport() {
        return mIsPrivacyProtectionLockSupport;
    }

    public static final boolean isSimMeLockValid(int i) {
        if (mValidPhoneIds == -1) {
            return true;
        }
        int i2 = 1 << i;
        int i3 = mValidPhoneIds & i2;
        Log.d("KeyguardUtils", "isSimMeLockValid phoneId=" + i + ", result=" + i3);
        return i2 == i3;
    }

    public final void initSimmePolicy(Context context) {
        if (mIsMediatekSimMeLockSupport) {
            return;
        }
        if ("1".equals(context.getResources().getString(com.android.systemui.R.string.simme_dependency_enabled))) {
            Log.d("KeyguardUtils", "config enabled for simme dependency");
            mIsSimmePolicyEnabled = true;
        } else {
            mIsSimmePolicyEnabled = new File(Environment.getExternalStorageDirectory(), "SimmeLock").exists();
        }
        if ("1".equals(context.getResources().getString(com.android.systemui.R.string.dismiss_button_enabled))) {
            Log.d("KeyguardUtils", "config enabled for simme dismiss");
            mIsDismissEnabled = true;
        } else {
            mIsDismissEnabled = new File(Environment.getExternalStorageDirectory(), "SimmeDismiss").exists();
        }
        int simLockPolicy = MtkTelephonyManagerEx.getDefault().getSimLockPolicy();
        mIsMediatekSimMeLockSupport = simLockPolicy > 0;
        if (simLockPolicy <= 0) {
            return;
        }
        if (mIsSimmePolicyEnabled) {
            mValidPhoneIds = parseValueFromConfig(context, "simme_lock_with_request_", simLockPolicy);
            if (mValidPhoneIds > 15) {
                mValidPhoneIds = -1;
            }
        }
        if (mIsDismissEnabled) {
            mIsDismissSimMeLockSupport = true;
            if (parseValueFromConfig(context, "dismiss_button_with_request_", simLockPolicy) == 0) {
                mIsDismissSimMeLockSupport = false;
            }
        }
    }

    private final int parseValueFromConfig(Context context, String str, int i) throws Resources.NotFoundException {
        int identifier = context.getResources().getIdentifier(str + i, "string", context.getPackageName());
        if (identifier == 0) {
            Log.w("KeyguardUtils", "Cannot get valid source id for " + str + i);
            return -1;
        }
        String string = context.getResources().getString(identifier);
        Log.d("KeyguardUtils", "Get config for " + str + i + ", value=" + string);
        if (string == null || string.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            Log.e("KeyguardUtils", "Exception happened: " + e);
            return -1;
        }
    }
}
