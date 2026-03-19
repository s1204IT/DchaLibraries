package com.mediatek.internal.telephony.dataconnection;

import android.content.Context;
import android.os.AsyncResult;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import java.util.ArrayList;

public class DataConnectionExt implements IDataConnectionExt {
    static final String TAG = "DataConnectionExt";

    public DataConnectionExt(Context context) {
    }

    @Override
    public boolean isDomesticRoamingEnabled() {
        return false;
    }

    @Override
    public boolean isDataAllowedAsOff(String str) {
        if (TextUtils.equals(str, "default") || TextUtils.equals(str, "mms") || TextUtils.equals(str, "dun") || TextUtils.equals(str, "preempt")) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isFdnEnableSupport() {
        return false;
    }

    @Override
    public void onDcActivated(String[] strArr, String str) {
    }

    @Override
    public void onDcDeactivated(String[] strArr, String str) {
    }

    @Override
    public long getDisconnectDoneRetryTimer(String str, long j) {
        if (MtkGsmCdmaPhone.REASON_RA_FAILED.equals(str)) {
            return 90000L;
        }
        return j;
    }

    public void log(String str) {
        Rlog.d(TAG, str);
    }

    @Override
    public boolean isOnlySingleDcAllowed() {
        return false;
    }

    @Override
    public boolean ignoreDefaultDataUnselected(String str) {
        if (TextUtils.equals(str, "ims") || TextUtils.equals(str, "emergency") || TextUtils.equals(str, "xcap") || TextUtils.equals(str, "mms")) {
            log("ignoreDefaultDataUnselected, apnType = " + str);
            return true;
        }
        return false;
    }

    @Override
    public boolean ignoreDataRoaming(String str) {
        if (TextUtils.equals(str, "ims")) {
            log("ignoreDataRoaming, apnType = " + str);
            return true;
        }
        return false;
    }

    @Override
    public void startDataRoamingStrategy(Phone phone) {
    }

    @Override
    public void stopDataRoamingStrategy() {
    }

    @Override
    public String getOperatorNumericFromImpi(String str, int i) {
        return str;
    }

    @Override
    public boolean isMeteredApnTypeByLoad() {
        return false;
    }

    @Override
    public boolean isMeteredApnType(String str, boolean z) {
        log("isMeteredApnType, apnType = " + str + ", isRoaming = " + z);
        if (TextUtils.equals(str, "default") || TextUtils.equals(str, "supl") || TextUtils.equals(str, "dun") || TextUtils.equals(str, "mms")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isIgnoredCause(DcFailCause dcFailCause) {
        return false;
    }

    @Override
    public void handlePcoDataAfterAttached(AsyncResult asyncResult, Phone phone, ArrayList<ApnSetting> arrayList) {
    }

    @Override
    public boolean isSmartDataSwtichAllowed() {
        return true;
    }
}
