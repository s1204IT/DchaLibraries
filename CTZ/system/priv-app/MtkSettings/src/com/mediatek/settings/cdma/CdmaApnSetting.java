package com.mediatek.settings.cdma;

import android.telephony.TelephonyManager;
import android.util.Log;

/* loaded from: classes.dex */
public class CdmaApnSetting {
    public static String customizeQuerySelectionforCdma(String str, String str2, int i) {
        Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, subId = " + i);
        String str3 = " mvno_type='" + replaceNull(null) + "' and mvno_match_data='" + replaceNull(null) + "'";
        String networkOperator = TelephonyManager.getDefault().getNetworkOperator(i);
        Log.d("CdmaApnSetting", " numeric = " + str2 + ", networkNumeric = " + networkOperator);
        if (isCtNumeric(str2)) {
            Log.d("CdmaApnSetting", "networkNumeric = " + networkOperator);
            if (isCtInRoaming(str2, i)) {
                Log.d("CdmaApnSetting", "ROAMING");
                String str4 = "numeric='" + networkOperator + "' and ((" + str3 + (" and apn <> 'ctwap'") + ") or (sourceType = '1'))";
                Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, roaming result = " + str4);
                return str4;
            }
            String str5 = "numeric='" + str2 + "' and " + ("((" + str3 + ") or (sourceType = '1'))");
            Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, result = " + str5);
            return str5;
        }
        return str;
    }

    private static boolean isCtNumeric(String str) {
        return str != null && (str.contains("46011") || str.contains("46003"));
    }

    private static boolean isCtInRoaming(String str, int i) {
        String networkOperator;
        if (isCtNumeric(str) && (networkOperator = TelephonyManager.getDefault().getNetworkOperator(i)) != null && networkOperator.length() >= 3 && !networkOperator.startsWith("460") && !networkOperator.startsWith("455")) {
            return true;
        }
        return false;
    }

    public static String updateMccMncForCdma(String str, int i) {
        Log.d("CdmaApnSetting", "updateMccMncForCdma, subId = " + str + ", numeric = " + i + ", networkNumeric = " + TelephonyManager.getDefault().getNetworkOperator(i));
        return str;
    }

    private static String replaceNull(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }
}
