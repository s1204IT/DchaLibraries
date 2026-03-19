package com.mediatek.internal.telephony.worldphone;

import android.os.SystemProperties;
import android.telephony.Rlog;
import com.mediatek.internal.telephony.datasub.DataSubConstants;

public class WorldPhoneWrapper implements IWorldPhone {
    private static int sOperatorSpec = -1;
    private static IWorldPhone sWorldPhoneInstance = null;
    private static WorldPhoneUtil sWorldPhoneUtil = null;

    public static IWorldPhone getWorldPhoneInstance() {
        if (sWorldPhoneInstance == null) {
            String str = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR);
            if (str != null && str.equals(DataSubConstants.OPERATOR_OP01)) {
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
    public void setModemSelectionMode(int i, int i2) {
        if (sOperatorSpec == 1 || sOperatorSpec == 0) {
            sWorldPhoneInstance.setModemSelectionMode(i, i2);
        } else {
            logd("Unknown World Phone Spec");
        }
    }

    @Override
    public void notifyRadioCapabilityChange(int i) {
    }

    private static void logd(String str) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPO_WRAPPER]" + str);
    }
}
