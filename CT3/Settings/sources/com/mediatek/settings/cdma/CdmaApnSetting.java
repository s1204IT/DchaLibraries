package com.mediatek.settings.cdma;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.mediatek.internal.telephony.ITelephonyEx;

public class CdmaApnSetting {
    public static String customizeQuerySelectionforCdma(String where, String numeric, int subId) {
        Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, subId = " + subId);
        if (!CdmaUtils.isSupportCdma(subId)) {
            Log.d("CdmaApnSetting", "not CDMA, just return");
            return where;
        }
        String sqlStr = "";
        try {
            ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            String mvnoType = telephonyEx.getMvnoMatchType(subId);
            String mvnoPattern = telephonyEx.getMvnoPattern(subId, mvnoType);
            sqlStr = " mvno_type='" + replaceNull(mvnoType) + "' and mvno_match_data='" + replaceNull(mvnoPattern) + "'";
        } catch (RemoteException e) {
            Log.d("CdmaApnSetting", "RemoteException " + e);
        }
        String networkNumeric = TelephonyManager.getDefault().getNetworkOperator(subId);
        Log.d("CdmaApnSetting", " numeric = " + numeric + ", networkNumeric = " + networkNumeric);
        if (!isCtNumeric(numeric)) {
            return where;
        }
        Log.d("CdmaApnSetting", "networkNumeric = " + networkNumeric);
        if (!isCtInRoaming(numeric, subId)) {
            String result = "numeric='" + numeric + "' and " + ("((" + sqlStr + ") or (sourceType = '1'))") + " AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";
            Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, result = " + result);
            return result;
        }
        Log.d("CdmaApnSetting", "ROAMING");
        String apn = " and apn <> 'ctwap'";
        String result2 = "numeric='" + networkNumeric + "' and ((" + sqlStr + apn + ") or (sourceType = '1')) AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL))";
        Log.d("CdmaApnSetting", "customizeQuerySelectionforCdma, roaming result = " + result2);
        return result2;
    }

    private static boolean isCtNumeric(String numeric) {
        if (numeric == null) {
            return false;
        }
        if (numeric.contains("46011")) {
            return true;
        }
        return numeric.contains("46003");
    }

    private static boolean isCtInRoaming(String numeric, int subId) {
        String networkNumeric;
        if (isCtNumeric(numeric) && (networkNumeric = TelephonyManager.getDefault().getNetworkOperator(subId)) != null && networkNumeric.length() >= 3 && !networkNumeric.startsWith("460") && !networkNumeric.startsWith("455")) {
            return true;
        }
        return false;
    }

    public static String updateMccMncForCdma(String numeric, int subId) {
        String networkNumeric = TelephonyManager.getDefault().getNetworkOperator(subId);
        Log.d("CdmaApnSetting", "updateMccMncForCdma, subId = " + numeric + ", numeric = " + subId + ", networkNumeric = " + networkNumeric);
        if (CdmaUtils.isSupportCdma(subId) && isCtInRoaming(networkNumeric, subId)) {
            Log.d("CdmaApnSetting", "ROAMING, return " + networkNumeric);
            return networkNumeric;
        }
        return numeric;
    }

    private static String replaceNull(String origString) {
        if (origString == null) {
            return "";
        }
        return origString;
    }
}
