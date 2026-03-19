package com.mediatek.internal.telephony;

import android.content.Context;
import android.telephony.Rlog;

public class MtkProxyControllerExt implements IMtkProxyControllerExt {
    static final String TAG = "MtkProxyControllerExt";
    protected Context mContext;

    public MtkProxyControllerExt() {
    }

    public MtkProxyControllerExt(Context context) {
        this.mContext = context;
    }

    public void log(String str) {
        Rlog.d(TAG, str);
    }

    @Override
    public boolean isNeedSimSwitch(int i, int i2) {
        log("OMisNeedSimSwitch, majorPhoneId = " + i);
        return !RadioCapabilitySwitchUtil.isSkipCapabilitySwitch(i, i2);
    }
}
