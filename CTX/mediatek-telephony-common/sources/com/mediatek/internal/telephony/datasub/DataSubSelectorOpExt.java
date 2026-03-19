package com.mediatek.internal.telephony.datasub;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import com.android.internal.telephony.SubscriptionController;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import java.util.Arrays;

public class DataSubSelectorOpExt implements IDataSubSelectorOPExt {
    private static final int DSS_RET_CANNOT_GET_SIM_INFO = -2;
    private static final int DSS_RET_INVALID_PHONE_INDEX = -1;
    private Intent mIntent = null;
    private static final boolean USER_BUILD = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    private static boolean DBG = true;
    private static String LOG_TAG = "DSSExt";
    private static Context mContext = null;
    private static DataSubSelectorOpExt mInstance = null;
    private static DataSubSelector mDataSubSelector = null;
    private static ISimSwitchForDSSExt mSimSwitchForDSS = null;
    private static CapabilitySwitch mCapabilitySwitch = null;

    public DataSubSelectorOpExt(Context context) {
        mContext = context;
    }

    @Override
    public void init(DataSubSelector dataSubSelector, ISimSwitchForDSSExt iSimSwitchForDSSExt) {
        mDataSubSelector = dataSubSelector;
        mCapabilitySwitch = CapabilitySwitch.getInstance(mContext, dataSubSelector);
        mSimSwitchForDSS = iSimSwitchForDSSExt;
    }

    @Override
    public void handleSimStateChanged(Intent intent) {
        int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
        intent.getIntExtra("slot", 0);
        if (intExtra == 10) {
            mCapabilitySwitch.handleSimImsiStatus(intent);
            handleNeedWaitImsi(intent);
            handleNeedWaitUnlock(intent);
        } else if (intExtra == 6) {
            mCapabilitySwitch.handleSimImsiStatus(intent);
        }
    }

    @Override
    public void handleSubinfoRecordUpdated(Intent intent) {
        intent.getIntExtra("simDetectStatus", 4);
        subSelector(intent);
    }

    private void handleNeedWaitImsi(Intent intent) {
        if (CapabilitySwitch.isNeedWaitImsi()) {
            CapabilitySwitch.setNeedWaitImsi(Boolean.toString(false));
            subSelector(intent);
        }
        if (CapabilitySwitch.isNeedWaitImsiRoaming()) {
            CapabilitySwitch.setNeedWaitImsiRoaming(Boolean.toString(false));
        }
    }

    private void handleNeedWaitUnlock(Intent intent) {
        if (CapabilitySwitch.isNeedWaitUnlock()) {
            CapabilitySwitch.setNeedWaitUnlock("false");
            subSelector(intent);
        }
        if (CapabilitySwitch.isNeedWaitUnlockRoaming()) {
            CapabilitySwitch.setNeedWaitUnlockRoaming("false");
        }
    }

    private int getHighCapabilityPhoneIdBySimType() {
        int[] iArr = new int[mDataSubSelector.getPhoneNum()];
        int[] iArr2 = new int[mDataSubSelector.getPhoneNum()];
        String[] strArr = new String[mDataSubSelector.getPhoneNum()];
        int i = -1;
        if (RadioCapabilitySwitchUtil.isPS2SupportLTE() && mDataSubSelector.getPhoneNum() == 2) {
            int i2 = 0;
            int i3 = 0;
            for (int i4 = 0; i4 < mDataSubSelector.getPhoneNum(); i4++) {
                strArr[i4] = DataSubSelectorUtil.getIccidFromProp(i4);
                if (strArr[i4] == null || "".equals(strArr[i4])) {
                    log("iccid not found, can not get high capability phone id");
                    return -1;
                }
                if (!DataSubConstants.NO_SIM_VALUE.equals(strArr[i4])) {
                    i2++;
                    i3 |= 1 << i4;
                }
            }
            if (i2 == 0) {
                log("no sim card, don't switch");
                return -1;
            }
            if (!RadioCapabilitySwitchUtil.getSimInfo(iArr, iArr2, i3)) {
                log("cannot get sim operator info, don't switch");
                return -2;
            }
            int i5 = 0;
            int i6 = 0;
            int i7 = 0;
            for (int i8 = 0; i8 < mDataSubSelector.getPhoneNum(); i8++) {
                if (2 == iArr[i8]) {
                    i5++;
                } else if (RadioCapabilitySwitchUtil.isCdmaCard(i8, iArr[i8])) {
                    i7++;
                    iArr[i8] = 4;
                } else if (iArr[i8] != 0) {
                    i6++;
                    iArr[i8] = 3;
                }
            }
            log("getHighCapabilityPhoneIdBySimType : Inserted SIM count: " + i2 + ", insertedStatus: " + i3 + ", tSimCount: " + i5 + ", wSimCount: " + i6 + ", cSimCount: " + i7 + Arrays.toString(iArr));
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(1) && RadioCapabilitySwitchUtil.isTPlusWSupport()) {
                if (iArr[0] != 2 || iArr[1] != 3) {
                    if (iArr[0] == 3 && iArr[1] == 2) {
                        i = 1;
                    }
                } else {
                    i = 0;
                }
            }
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(2)) {
                if (iArr[0] != 2 || !RadioCapabilitySwitchUtil.isCdmaCard(1, iArr[1])) {
                    if (RadioCapabilitySwitchUtil.isCdmaCard(0, iArr[0]) && iArr[1] == 2) {
                        i = 0;
                    }
                } else {
                    i = 1;
                }
            }
            if (RadioCapabilitySwitchUtil.isSupportSimSwitchEnhancement(3)) {
                if (!RadioCapabilitySwitchUtil.isCdmaCard(0, iArr[0]) || iArr[1] != 3) {
                    if (iArr[0] == 3 && RadioCapabilitySwitchUtil.isCdmaCard(1, iArr[1])) {
                        i = 1;
                    }
                } else {
                    i = 0;
                }
            }
        }
        log("getHighCapabilityPhoneIdBySimType : " + i);
        return i;
    }

    @Override
    public void subSelector(Intent intent) {
        String[] strArr = new String[mDataSubSelector.getPhoneNum()];
        int highCapabilityPhoneIdBySimType = getHighCapabilityPhoneIdBySimType();
        if (highCapabilityPhoneIdBySimType == -2) {
            CapabilitySwitch.setNeedWaitImsi(Boolean.toString(true));
        } else if (highCapabilityPhoneIdBySimType == -1) {
            String iccidFromProp = "";
            int phoneId = SubscriptionManager.getPhoneId(SubscriptionController.getInstance().getDefaultDataSubId());
            if (phoneId >= 0) {
                if (phoneId >= DataSubSelectorUtil.getIccidNum()) {
                    log("phoneId out of boundary :" + phoneId);
                } else {
                    iccidFromProp = DataSubSelectorUtil.getIccidFromProp(phoneId);
                }
            }
            if (!USER_BUILD) {
                log("Default data Iccid = " + SubscriptionInfo.givePrintableIccid(iccidFromProp));
            }
            if (DataSubConstants.NO_SIM_VALUE.equals(iccidFromProp) || "".equals(iccidFromProp)) {
                return;
            }
            int i = 0;
            while (true) {
                if (i >= mDataSubSelector.getPhoneNum()) {
                    break;
                }
                strArr[i] = DataSubSelectorUtil.getIccidFromProp(i);
                if (strArr[i] == null || "".equals(strArr[i])) {
                    break;
                }
                if (!iccidFromProp.equals(strArr[i])) {
                    i++;
                } else {
                    highCapabilityPhoneIdBySimType = i;
                    break;
                }
            }
        }
        if (!mCapabilitySwitch.isSimUnLocked()) {
            log("DataSubSelector for OM: do not switch because of sim locking");
            CapabilitySwitch.setNeedWaitUnlock("true");
            this.mIntent = intent;
            CapabilitySwitch.setSimStatus(intent);
            CapabilitySwitch.setNewSimSlot(intent);
            return;
        }
        log("DataSubSelector for OM: no pin lock");
        CapabilitySwitch.setNeedWaitUnlock("false");
        log("Default data phoneid = " + highCapabilityPhoneIdBySimType);
        if (highCapabilityPhoneIdBySimType >= 0) {
            mCapabilitySwitch.setCapabilityIfNeeded(highCapabilityPhoneIdBySimType);
        }
        CapabilitySwitch.resetSimStatus();
        CapabilitySwitch.resetNewSimSlot();
    }

    @Override
    public void handleAirPlaneModeOff(Intent intent) {
        subSelector(intent);
    }

    @Override
    public void handlePlmnChanged(Intent intent) {
    }

    @Override
    public void handleDefaultDataChanged(Intent intent) {
    }

    private void log(String str) {
        if (DBG) {
            Rlog.d(LOG_TAG, str);
        }
    }

    private void loge(String str) {
        if (DBG) {
            Rlog.e(LOG_TAG, str);
        }
    }
}
