package com.mediatek.internal.telephony.datasub;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.ProxyController;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;

public class CapabilitySwitch {
    private static final String LOG_TAG = "CapaSwitch";
    private static boolean DBG = true;
    private static CapabilitySwitch mInstance = null;
    private static Context mContext = null;
    private static DataSubSelector mDataSubSelector = null;
    private static final int capability_switch_policy = SystemProperties.getInt(DataSubConstants.PROPERTY_CAPABILITY_SWITCH_POLICY, 1);

    public static CapabilitySwitch getInstance(Context context, DataSubSelector dataSubSelector) {
        if (mInstance == null) {
            mInstance = new CapabilitySwitch(context, dataSubSelector);
        }
        return mInstance;
    }

    public CapabilitySwitch(Context context, DataSubSelector dataSubSelector) {
        mContext = context;
        mDataSubSelector = dataSubSelector;
    }

    public boolean isCanSwitch() {
        if (mDataSubSelector.getAirPlaneModeOn()) {
            log("DataSubselector,isCanSwitch AirplaneModeOn = " + mDataSubSelector.getAirPlaneModeOn());
            return false;
        }
        return isSimUnLocked();
    }

    public boolean isSimUnLocked() {
        int phoneNum = mDataSubSelector.getPhoneNum();
        for (int i = 0; i < phoneNum; i++) {
            int simState = TelephonyManager.from(mContext).getSimState(i);
            if (simState == 2 || simState == 3 || simState == 4 || simState == 6 || simState == 0) {
                log("isSimUnLocked, sim locked, simState = " + simState);
                return false;
            }
        }
        return true;
    }

    public static boolean isNeedWaitUnlock() {
        return SystemProperties.getBoolean(DataSubConstants.NEED_TO_WAIT_UNLOCKED, false);
    }

    public static boolean isNeedWaitUnlockRoaming() {
        return SystemProperties.getBoolean(DataSubConstants.NEED_TO_WAIT_UNLOCKED_ROAMING, false);
    }

    public static void setNeedWaitUnlock(String str) {
        SystemProperties.set(DataSubConstants.NEED_TO_WAIT_UNLOCKED, str);
    }

    public static void setNeedWaitUnlockRoaming(String str) {
        SystemProperties.set(DataSubConstants.NEED_TO_WAIT_UNLOCKED_ROAMING, str);
    }

    public static boolean isNeedWaitImsi() {
        return SystemProperties.getBoolean(DataSubConstants.NEED_TO_WAIT_IMSI, false);
    }

    public static boolean isNeedWaitImsiRoaming() {
        return SystemProperties.getBoolean(DataSubConstants.NEED_TO_WAIT_IMSI_ROAMING, false);
    }

    public static void setNeedWaitImsi(String str) {
        SystemProperties.set(DataSubConstants.NEED_TO_WAIT_IMSI, str);
    }

    public static void setNeedWaitImsiRoaming(String str) {
        SystemProperties.set(DataSubConstants.NEED_TO_WAIT_IMSI_ROAMING, str);
    }

    public static void setSimStatus(Intent intent) {
        if (intent == null) {
            log("setSimStatus, intent is null => return");
            return;
        }
        log("setSimStatus");
        int intExtra = intent.getIntExtra("simDetectStatus", 0);
        if (intExtra != getSimStatus()) {
            SystemProperties.set(DataSubConstants.SIM_STATUS, Integer.toString(intExtra));
        }
    }

    public static void resetSimStatus() {
        log("resetSimStatus");
        SystemProperties.set(DataSubConstants.SIM_STATUS, "");
    }

    public static int getSimStatus() {
        log("getSimStatus");
        return SystemProperties.getInt(DataSubConstants.SIM_STATUS, 0);
    }

    public static void setNewSimSlot(Intent intent) {
        if (intent == null) {
            log("setNewSimSlot, intent is null => return");
            return;
        }
        int intExtra = intent.getIntExtra("newSIMSlot", 0);
        int newSimSlot = getNewSimSlot();
        log("setNewSimSlot, new slot=" + intExtra + ", oldSimSlot=" + newSimSlot);
        if (intExtra != newSimSlot) {
            SystemProperties.set(DataSubConstants.NEW_SIM_SLOT, Integer.toString(intExtra | newSimSlot));
        }
    }

    public static void resetNewSimSlot() {
        log("resetNewSimSlot");
        SystemProperties.set(DataSubConstants.NEW_SIM_SLOT, "");
    }

    public static int getNewSimSlot() {
        log("getNewSimSlot");
        return SystemProperties.getInt(DataSubConstants.NEW_SIM_SLOT, 0);
    }

    public boolean setCapability(int i) {
        int phoneNum = mDataSubSelector.getPhoneNum();
        int[] iArr = new int[phoneNum];
        log("setCapability: " + i + ", current 3G Sim = " + SystemProperties.get("persist.vendor.radio.simswitch", ""));
        ProxyController proxyController = ProxyController.getInstance();
        RadioAccessFamily[] radioAccessFamilyArr = new RadioAccessFamily[phoneNum];
        for (int i2 = 0; i2 < phoneNum; i2++) {
            if (i == i2) {
                iArr[i2] = proxyController.getMaxRafSupported();
            } else {
                iArr[i2] = proxyController.getMinRafSupported();
            }
            radioAccessFamilyArr[i2] = new RadioAccessFamily(i2, iArr[i2]);
        }
        if (!proxyController.setRadioCapability(radioAccessFamilyArr)) {
            log("Set phone rat fail!!! MaxPhoneRat=" + iArr[i]);
            return false;
        }
        return true;
    }

    public boolean setCapabilityIfNeeded(int i) {
        return setCapability(i);
    }

    public int getCapabilitySwitchPolicy() {
        return capability_switch_policy;
    }

    public void handleSimImsiStatus(Intent intent) {
        int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
        int intExtra2 = intent.getIntExtra("slot", 0);
        if (intExtra == 10) {
            RadioCapabilitySwitchUtil.updateSimImsiStatus(intExtra2, "1");
        } else if (intExtra == 6) {
            RadioCapabilitySwitchUtil.updateSimImsiStatus(intExtra2, "0");
        }
    }

    private static void log(String str) {
        if (DBG) {
            Rlog.d(LOG_TAG, str);
        }
    }

    private static void loge(String str) {
        if (DBG) {
            Rlog.e(LOG_TAG, str);
        }
    }
}
