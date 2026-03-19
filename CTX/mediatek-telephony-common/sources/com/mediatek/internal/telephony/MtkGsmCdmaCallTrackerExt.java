package com.mediatek.internal.telephony;

import android.content.Context;
import android.os.Bundle;
import android.telephony.Rlog;
import com.android.internal.telephony.Connection;

public class MtkGsmCdmaCallTrackerExt implements IMtkGsmCdmaCallTrackerExt {
    static final String TAG = "GsmCdmaCallTkrExt";
    protected Context mContext;

    public MtkGsmCdmaCallTrackerExt() {
    }

    public MtkGsmCdmaCallTrackerExt(Context context) {
        this.mContext = context;
    }

    public void log(String str) {
        Rlog.d(TAG, str);
    }

    @Override
    public String convertDialString(Bundle bundle, String str) {
        return null;
    }

    @Override
    public String convertAddress(String str) {
        return null;
    }

    protected static boolean equalsHandlesNulls(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    @Override
    public boolean isAddressChanged(boolean z, String str, String str2, String str3) {
        if (!equalsHandlesNulls(str2, str)) {
            if (!z || !equalsHandlesNulls(str3, str)) {
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public boolean isAddressChanged(boolean z, String str, String str2) {
        if (!equalsHandlesNulls(str2, str)) {
            return true;
        }
        return false;
    }

    @Override
    public Bundle getAddressExtras(String str) {
        return null;
    }

    @Override
    public boolean areConnectionsInSameLine(Connection[] connectionArr) {
        return true;
    }
}
