package com.mediatek.internal.telephony.ratconfiguration;

import android.os.SystemProperties;
import android.telephony.Rlog;

public class RatConfiguration {
    static final String CDMA = "C";
    static final String DELIMITER = "/";
    static final String GSM = "G";
    private static final String LOG_TAG = "RatConfig";
    static final String LteFdd = "Lf";
    static final String LteTdd = "Lt";
    public static final int MASK_CDMA = 32;
    public static final int MASK_GSM = 1;
    public static final int MASK_LteFdd = 16;
    public static final int MASK_LteTdd = 8;
    public static final int MASK_TDSCDMA = 2;
    public static final int MASK_WCDMA = 4;
    protected static final int MD_MODE_LCTG = 16;
    protected static final int MD_MODE_LFWCG = 15;
    protected static final int MD_MODE_LFWG = 14;
    protected static final int MD_MODE_LTCTG = 17;
    protected static final int MD_MODE_LTG = 8;
    protected static final int MD_MODE_LTTG = 13;
    protected static final int MD_MODE_LWCG = 11;
    protected static final int MD_MODE_LWCTG = 12;
    protected static final int MD_MODE_LWG = 9;
    protected static final int MD_MODE_LWTG = 10;
    protected static final int MD_MODE_UNKNOWN = 0;
    static final String PROPERTY_BUILD_RAT_CONFIG = "ro.vendor.mtk_protocol1_rat_config";
    static final String PROPERTY_IS_USING_DEFAULT_CONFIG = "ro.boot.opt_using_default";
    static final String PROPERTY_RAT_CONFIG = "ro.boot.opt_ps1_rat";
    static final String TDSCDMA = "T";
    static final String WCDMA = "W";
    private static int max_rat = 0;
    private static boolean max_rat_initialized = false;
    private static int actived_rat = 0;
    private static boolean is_default_config = true;

    protected static int ratToBitmask(String str) {
        int i;
        if (str.contains(CDMA)) {
            i = 32;
        } else {
            i = 0;
        }
        if (str.contains(LteFdd)) {
            i |= 16;
        }
        if (str.contains(LteTdd)) {
            i |= 8;
        }
        if (str.contains(WCDMA)) {
            i |= 4;
        }
        if (str.contains(TDSCDMA)) {
            i |= 2;
        }
        if (str.contains(GSM)) {
            return i | 1;
        }
        return i;
    }

    protected static synchronized int getMaxRat() {
        if (!max_rat_initialized) {
            String str = SystemProperties.get(PROPERTY_BUILD_RAT_CONFIG, "");
            max_rat = ratToBitmask(str);
            is_default_config = SystemProperties.getInt(PROPERTY_IS_USING_DEFAULT_CONFIG, 1) != 0;
            max_rat_initialized = true;
            logd("getMaxRat: initial " + str + " " + max_rat);
        }
        return max_rat;
    }

    protected static boolean checkRatConfig(int i) {
        int maxRat = getMaxRat();
        if ((i | maxRat) == maxRat) {
            return true;
        }
        logd("checkRatConfig: FAIL with " + String.valueOf(i));
        return false;
    }

    protected static int getRatConfig() {
        int maxRat = getMaxRat();
        if (maxRat == 0) {
            actived_rat = 0;
            return actived_rat;
        }
        if (is_default_config) {
            actived_rat = maxRat;
            return maxRat;
        }
        String str = SystemProperties.get(PROPERTY_RAT_CONFIG, "");
        if (str.length() > 0) {
            actived_rat = ratToBitmask(str);
            if (!checkRatConfig(actived_rat)) {
                logd("getRatConfig: invalid PROPERTY_RAT_CONFIG, set to max_rat");
                actived_rat = getMaxRat();
            }
        } else {
            logd("getRatConfig: ger property PROPERTY_RAT_CONFIG fail, initialize");
            actived_rat = getMaxRat();
        }
        return actived_rat;
    }

    protected static String ratToString(int i) {
        String str = "";
        if ((i & 32) == 32) {
            str = "/C";
        }
        if ((i & 16) == 16) {
            str = str + "/Lf";
        }
        if ((i & 8) == 8) {
            str = str + "/Lt";
        }
        if ((i & 4) == 4) {
            str = str + "/W";
        }
        if ((i & 2) == 2) {
            str = str + "/T";
        }
        if ((i & 1) == 1) {
            str = str + "/G";
        }
        if (str.length() > 0) {
            return str.substring(1);
        }
        return str;
    }

    public static boolean isC2kSupported() {
        return ((getMaxRat() & getRatConfig()) & 32) == 32;
    }

    public static boolean isLteFddSupported() {
        return ((getMaxRat() & getRatConfig()) & 16) == 16;
    }

    public static boolean isLteTddSupported() {
        return ((getMaxRat() & getRatConfig()) & 8) == 8;
    }

    public static boolean isWcdmaSupported() {
        return ((getMaxRat() & getRatConfig()) & 4) == 4;
    }

    public static boolean isTdscdmaSupported() {
        return ((getMaxRat() & getRatConfig()) & 2) == 2;
    }

    public static boolean isGsmSupported() {
        return ((getMaxRat() & getRatConfig()) & 1) == 1;
    }

    public static String getActiveRatConfig() {
        String strRatToString = ratToString(getRatConfig());
        logd("getActiveRatConfig: " + strRatToString);
        return strRatToString;
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }
}
