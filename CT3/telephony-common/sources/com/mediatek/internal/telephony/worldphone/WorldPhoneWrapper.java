package com.mediatek.internal.telephony.worldphone;

import android.os.SystemProperties;
import android.telephony.Rlog;

public class WorldPhoneWrapper implements IWorldPhone {
    private static int sOperatorSpec = -1;
    private static IWorldPhone sWorldPhoneInstance = null;
    private static WorldPhoneUtil sWorldPhoneUtil = null;

    public static IWorldPhone getWorldPhoneInstance() {
        if (sWorldPhoneInstance == null) {
            String optr = SystemProperties.get("persist.operator.optr");
            if (optr != null && optr.equals("OP01")) {
                sOperatorSpec = 1;
            } else {
                sOperatorSpec = 0;
            }
            sWorldPhoneUtil = new WorldPhoneUtil();
            if (sOperatorSpec == 1) {
                sWorldPhoneInstance = new WorldPhoneOp01();
            } else if (sOperatorSpec == 0) {
                sWorldPhoneInstance = new WorldPhoneOm();
            }
        }
        logd("sOperatorSpec: " + sOperatorSpec + ", isLteSupport: " + WorldPhoneUtil.isLteSupport());
        return sWorldPhoneInstance;
    }

    @Override
    public void setModemSelectionMode(int mode, int modemType) {
        if (sOperatorSpec == 1 || sOperatorSpec == 0) {
            sWorldPhoneInstance.setModemSelectionMode(mode, modemType);
        } else {
            logd("Unknown World Phone Spec");
        }
    }

    @Override
    public void notifyRadioCapabilityChange(int capailitySimId) {
    }

    private static void logd(String msg) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPO_WRAPPER]" + msg);
    }
}
