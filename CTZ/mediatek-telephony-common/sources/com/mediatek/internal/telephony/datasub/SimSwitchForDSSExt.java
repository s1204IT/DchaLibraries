package com.mediatek.internal.telephony.datasub;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

public class SimSwitchForDSSExt implements ISimSwitchForDSSExt {
    public static final boolean USER_BUILD = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    public static boolean DBG = true;
    private static DataSubSelector mDataSubSelector = null;
    protected static Context mContext = null;

    public SimSwitchForDSSExt(Context context) {
    }

    @Override
    public void init(DataSubSelector dataSubSelector) {
        mDataSubSelector = dataSubSelector;
    }

    @Override
    public boolean checkCapSwitch(int i) {
        return false;
    }

    @Override
    public int isNeedSimSwitch() {
        return 2;
    }
}
