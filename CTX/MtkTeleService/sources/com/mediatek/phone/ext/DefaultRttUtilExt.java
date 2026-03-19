package com.mediatek.phone.ext;

import android.os.Bundle;
import android.telephony.Rlog;
import com.android.internal.telephony.Connection;

public class DefaultRttUtilExt implements IRttUtilExt {
    public static final String TAG = "DefaultRttUtilExt";

    @Override
    public void setIncomingRttCall(Connection connection, Bundle bundle) {
        Rlog.d(TAG, "setIncomingRttCall default");
    }

    @Override
    public boolean isRttCallAndNotAllowMerge(Connection connection) {
        return false;
    }

    @Override
    public void onStopRtt(boolean z, Connection connection) {
        Rlog.d(TAG, "onStopRtt default");
    }

    @Override
    public boolean setTelephonyConnectionRttStatus(boolean z) {
        Rlog.d(TAG, "setTelephonyConnectionRttStatus default");
        return false;
    }

    @Override
    public int updatePropertyRtt(int i, boolean z, int i2) {
        Rlog.d(TAG, "setTelephonyConnectionRttStatus default");
        return i;
    }

    @Override
    public boolean updateConnectionProperties() {
        Rlog.d(TAG, "updateConnectionProperties default");
        return false;
    }
}
