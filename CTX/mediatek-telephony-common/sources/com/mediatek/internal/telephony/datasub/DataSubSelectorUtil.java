package com.mediatek.internal.telephony.datasub;

import android.os.SystemProperties;
import android.telephony.Rlog;
import android.text.TextUtils;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;

public class DataSubSelectorUtil {
    private static final String LOG_TAG = "DSSelectorUtil";
    private static boolean DBG = true;
    private static DataSubSelectorUtil mInstance = null;
    private static DataSubSelector mDataSubSelector = null;
    public static String[] PROPERTY_ICCID = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};

    public static String getIccidFromProp(int i) {
        return SystemProperties.get(PROPERTY_ICCID[i]);
    }

    public static int getIccidNum() {
        return PROPERTY_ICCID.length;
    }

    public boolean isSimInserted(int i) {
        String str = SystemProperties.get(PROPERTY_ICCID[i], "");
        return (TextUtils.isEmpty(str) || DataSubConstants.NO_SIM_VALUE.equals(str)) ? false : true;
    }

    public static boolean isC2kProject() {
        return RatConfiguration.isC2kSupported();
    }

    public static String getDefaultDataIccId() {
        return SystemProperties.get(DataSubConstants.PROPERTY_DEFAULT_DATA_ICCID);
    }

    public static int getMaxIccIdCount() {
        return PROPERTY_ICCID.length;
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
