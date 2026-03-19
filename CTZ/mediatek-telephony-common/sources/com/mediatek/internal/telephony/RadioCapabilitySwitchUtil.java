package com.mediatek.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.IPhoneSubInfo;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class RadioCapabilitySwitchUtil {
    public static final String CN_MCC = "460";
    public static final int ENHANCEMENT_T_PLUS_C = 2;
    public static final int ENHANCEMENT_T_PLUS_T = 0;
    public static final int ENHANCEMENT_T_PLUS_W = 1;
    public static final int ENHANCEMENT_W_PLUS_C = 3;
    public static final int ENHANCEMENT_W_PLUS_NA = 5;
    public static final int ENHANCEMENT_W_PLUS_W = 4;
    public static final int ICCID_ERROR = 3;
    public static final String IMSI_NOT_READY = "0";
    public static final int IMSI_NOT_READY_OR_SIM_LOCKED = 2;
    public static final String IMSI_READY = "1";
    private static final String LOG_TAG = "RadioCapabilitySwitchUtil";
    public static final int NOT_SHOW_DIALOG = 1;
    private static final String NO_SIM_VALUE = "N/A";
    public static final int OP01_6M_PRIORITY_OP01_SIM = 1;
    public static final int OP01_6M_PRIORITY_OP01_USIM = 0;
    public static final int OP01_6M_PRIORITY_OTHER = 2;
    private static final String PROPERTY_CAPABILITY_SWITCH = "persist.vendor.radio.simswitch";
    private static final String PROPERTY_ICCID = "vendor.ril.iccid.sim";
    public static final int SHOW_DIALOG = 0;
    public static final int SIM_OP_INFO_OP01 = 2;
    public static final int SIM_OP_INFO_OP02 = 3;
    public static final int SIM_OP_INFO_OP09 = 4;
    public static final int SIM_OP_INFO_OP18 = 4;
    public static final int SIM_OP_INFO_OVERSEA = 1;
    public static final int SIM_OP_INFO_UNKNOWN = 0;
    public static final int SIM_SWITCH_MODE_DUAL_TALK = 3;
    public static final int SIM_SWITCH_MODE_DUAL_TALK_SWAP = 4;
    public static final int SIM_SWITCH_MODE_SINGLE_TALK_MDSYS = 1;
    public static final int SIM_SWITCH_MODE_SINGLE_TALK_MDSYS_LITE = 2;
    public static final int SIM_TYPE_OTHER = 2;
    public static final int SIM_TYPE_SIM = 0;
    public static final int SIM_TYPE_USIM = 1;
    private static final String[] PLMN_TABLE_OP01 = {"46000", "46002", "46007", "46008", "45412", "45413", "00101", "00211", "00321", "00431", "00541", "00651", "00761", "00871", "00902", "01012", "01122", "01232", "46004", "46602", "50270"};
    private static final String[] PLMN_TABLE_OP02 = {"46001", "46006", "46009", "45407"};
    private static final String[] PLMN_TABLE_OP09 = {"46005", "45502", "46003", "46011"};
    private static final String[] PLMN_TABLE_OP18 = {"405840", "405854", "405855", "405856", "405857", "405858", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874"};
    private static final String[] PROPERTY_SIM_ICCID = {"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
    private static final String[] PROPERTY_SIM_IMSI_STATUS = {"vendor.ril.imsi.status.sim1", "vendor.ril.imsi.status.sim2", "vendor.ril.imsi.status.sim3", "vendor.ril.imsi.status.sim4"};
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE = {"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
    private static final String[] PROPERTY_RIL_CT3G = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};

    public static boolean getSimInfo(int[] iArr, int[] iArr2, int i) {
        IPhoneSubInfo iPhoneSubInfoAsInterface;
        String[] strArr = new String[iArr.length];
        String[] strArr2 = new String[iArr.length];
        for (int i2 = 0; i2 < iArr.length; i2++) {
            strArr2[i2] = SystemProperties.get(i2 == 0 ? "vendor.gsm.ril.uicctype" : "vendor.gsm.ril.uicctype." + (i2 + 1), "");
            if (strArr2[i2].equals("SIM")) {
                iArr2[i2] = 0;
            } else if (strArr2[i2].equals("USIM")) {
                iArr2[i2] = 1;
            } else {
                iArr2[i2] = 2;
            }
            logd("SimType[" + i2 + "]= " + strArr2[i2] + ", simType[" + i2 + "]=" + iArr2[i2]);
            try {
                iPhoneSubInfoAsInterface = IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
            } catch (RemoteException e) {
                logd("get subInfo stub fail");
                strArr[i2] = "error";
            }
            if (iPhoneSubInfoAsInterface == null) {
                logd("subInfo stub is null");
                return false;
            }
            int[] subId = SubscriptionManager.getSubId(i2);
            if (subId == null) {
                logd("subIdList is null");
                return false;
            }
            strArr[i2] = iPhoneSubInfoAsInterface.getSubscriberIdForSubscriber(subId[0], "com.mediatek.internal.telephony");
            if (strArr[i2] == null) {
                logd("strMnc[" + i2 + "] is null, get mnc by ril.uim.subscriberid");
                StringBuilder sb = new StringBuilder();
                sb.append("vendor.ril.uim.subscriberid.");
                sb.append(i2 + 1);
                strArr[i2] = SystemProperties.get(sb.toString(), "");
            }
            if (strArr[i2] == null) {
                logd("strMnc[" + i2 + "] is null");
                strArr[i2] = "";
            }
            if (strArr[i2].length() >= 6) {
                strArr[i2] = strArr[i2].substring(0, 6);
            } else if (strArr[i2].length() >= 5) {
                strArr[i2] = strArr[i2].substring(0, 5);
            }
            logd("insertedStatus:" + i + "imsi status:" + getSimImsiStatus(i2));
            if (i >= 0 && ((1 << i2) & i) > 0) {
                if (strArr[i2].equals("") || strArr[i2].equals("error")) {
                    logd("SIM is inserted but no imsi");
                    return false;
                }
                if (strArr[i2].equals("sim_lock")) {
                    logd("SIM is lock, wait pin unlock");
                    return false;
                }
                if (strArr[i2].equals("N/A") || strArr[i2].equals("sim_absent")) {
                    logd("strMnc have invalid value, return false");
                    return false;
                }
            }
            String[] strArr3 = PLMN_TABLE_OP01;
            int length = strArr3.length;
            int i3 = 0;
            while (true) {
                if (i3 >= length) {
                    break;
                }
                if (!strArr[i2].startsWith(strArr3[i3])) {
                    i3++;
                } else {
                    iArr[i2] = 2;
                    break;
                }
            }
            if (iArr[i2] == 0) {
                String[] strArr4 = PLMN_TABLE_OP02;
                int length2 = strArr4.length;
                int i4 = 0;
                while (true) {
                    if (i4 >= length2) {
                        break;
                    }
                    if (!strArr[i2].startsWith(strArr4[i4])) {
                        i4++;
                    } else {
                        iArr[i2] = 3;
                        break;
                    }
                }
            }
            if (iArr[i2] == 0) {
                String[] strArr5 = PLMN_TABLE_OP09;
                int length3 = strArr5.length;
                int i5 = 0;
                while (true) {
                    if (i5 >= length3) {
                        break;
                    }
                    if (!strArr[i2].startsWith(strArr5[i5])) {
                        i5++;
                    } else {
                        iArr[i2] = 4;
                        break;
                    }
                }
            }
            if (SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "").equals(DataSubConstants.OPERATOR_OP18) && iArr[i2] == 0) {
                String[] strArr6 = PLMN_TABLE_OP18;
                int length4 = strArr6.length;
                int i6 = 0;
                while (true) {
                    if (i6 >= length4) {
                        break;
                    }
                    if (!strArr[i2].startsWith(strArr6[i6])) {
                        i6++;
                    } else {
                        iArr[i2] = 4;
                        break;
                    }
                }
            }
            if (iArr[i2] == 0 && !strArr[i2].equals("") && !strArr[i2].equals("N/A")) {
                iArr[i2] = 1;
            }
            logd("strMnc[" + i2 + "]= " + strArr[i2] + ", simOpInfo[" + i2 + "]=" + iArr[i2]);
        }
        logd("getSimInfo(simOpInfo): " + Arrays.toString(iArr));
        logd("getSimInfo(simType): " + Arrays.toString(iArr2));
        return true;
    }

    public static boolean isCdmaCard(int i, int i2) {
        boolean z = false;
        if (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("isCdmaCard invalid phoneId:" + i);
            return false;
        }
        if (i2 == 4) {
            return true;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]);
        if (str.indexOf("CSIM") >= 0 || str.indexOf("RUIM") >= 0) {
            z = true;
        }
        if (!z && "SIM".equals(str) && "1".equals(SystemProperties.get(PROPERTY_RIL_CT3G[i]))) {
            return true;
        }
        return z;
    }

    public static boolean isSupportSimSwitchEnhancement(int i) {
        switch (i) {
            case 0:
            case 1:
            case 4:
            case 5:
                return true;
            case 2:
            case 3:
            default:
                return false;
        }
    }

    public static boolean isSkipCapabilitySwitch(int i, int i2) {
        int[] iArr = new int[i2];
        int[] iArr2 = new int[i2];
        String[] strArr = new String[i2];
        String str = SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR, "OM");
        if (isPS2SupportLTE()) {
            if (i2 > 2) {
                return i < 2 && getMainCapabilityPhoneId() < 2 && !RatConfiguration.isC2kSupported() && !RatConfiguration.isTdscdmaSupported();
            }
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (i3 < i2) {
                StringBuilder sb = new StringBuilder();
                sb.append(PROPERTY_ICCID);
                int i6 = i3 + 1;
                sb.append(i6);
                strArr[i3] = SystemProperties.get(sb.toString());
                if (strArr[i3] == null || "".equals(strArr[i3])) {
                    logd("iccid not found, do capability switch");
                    return false;
                }
                if (!"N/A".equals(strArr[i3])) {
                    i4++;
                    i5 = (1 << i3) | i5;
                }
                i3 = i6;
            }
            if (i4 == 0) {
                logd("no sim card, skip capability switch");
                return true;
            }
            if (!getSimInfo(iArr, iArr2, i5)) {
                logd("cannot get sim operator info, do capability switch");
                return false;
            }
            int i7 = 0;
            int i8 = 0;
            int i9 = 0;
            for (int i10 = 0; i10 < i2; i10++) {
                if (2 == iArr[i10]) {
                    i7++;
                } else if (isCdmaCard(i10, iArr[i10])) {
                    i9++;
                } else if (iArr[i10] != 0) {
                    i8++;
                }
            }
            logd("isSkipCapabilitySwitch : Inserted SIM count: " + i4 + ", insertedStatus: " + i5 + ", tSimCount: " + i7 + ", wSimCount: " + i8 + ", cSimCount: " + i9);
            if ("OM".equals(str)) {
                if (isSupportSimSwitchEnhancement(0) && i4 == 2 && i7 == 2) {
                    return true;
                }
                if (isSupportSimSwitchEnhancement(1) && i4 == 2 && i7 == 1 && i8 == 1 && isTPlusWSupport() && iArr[i] != 2) {
                    return true;
                }
                if (isSupportSimSwitchEnhancement(2) && i4 == 2 && i7 == 1 && i9 == 1 && !isCdmaCard(i, iArr[i])) {
                    return true;
                }
                if (isSupportSimSwitchEnhancement(3) && i4 == 2 && i8 == 1 && i9 == 1 && !isCdmaCard(i, iArr[i])) {
                    return true;
                }
            }
            if (isSupportSimSwitchEnhancement(4) && i4 == 2 && i8 == 2) {
                return true;
            }
            if (isSupportSimSwitchEnhancement(5) && i4 == 1 && i8 == 1) {
                return true;
            }
        }
        return false;
    }

    public static int getHigherPrioritySimForOp01(int i, boolean[] zArr, boolean[] zArr2, boolean[] zArr3, boolean[] zArr4) {
        int length = zArr.length;
        if (zArr[i]) {
            return i;
        }
        int i2 = -1;
        for (int i3 = 0; i3 < length; i3++) {
            if (zArr[i3]) {
                i2 = i3;
            }
        }
        if (i2 != -1 || zArr2[i]) {
            return i2;
        }
        for (int i4 = 0; i4 < length; i4++) {
            if (zArr2[i4]) {
                i2 = i4;
            }
        }
        if (i2 != -1 || zArr3[i]) {
            return i2;
        }
        for (int i5 = 0; i5 < length; i5++) {
            if (zArr3[i5]) {
                i2 = i5;
            }
        }
        if (i2 != -1 || zArr4[i]) {
            return i2;
        }
        for (int i6 = 0; i6 < length; i6++) {
            if (zArr4[i6]) {
                i2 = i6;
            }
        }
        return i2;
    }

    public static int getHighestPriorityPhone(int i, int[] iArr) {
        int length = iArr.length;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        for (int i5 = 0; i5 < length; i5++) {
            if (iArr[i5] < iArr[i2]) {
                i4 = 1 << i5;
                i3 = 1;
                i2 = i5;
            } else if (iArr[i5] == iArr[i2]) {
                i3++;
                i4 |= 1 << i5;
            }
        }
        if (i3 == 1) {
            return i2;
        }
        if (i == -1 || ((1 << i) & i4) == 0) {
            return -1;
        }
        return i;
    }

    public static int getMainCapabilityPhoneId() {
        int i = SystemProperties.getInt("persist.vendor.radio.simswitch", 1) - 1;
        logd("[RadioCapSwitchUtil] getMainCapabilityPhoneId " + i);
        return i;
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, "[RadioCapSwitchUtil] " + str);
    }

    public static int isNeedShowSimDialog() {
        if (SystemProperties.getBoolean("ro.vendor.mtk_disable_cap_switch", false)) {
            logd("mtk_disable_cap_switch is true");
            return 0;
        }
        logd("isNeedShowSimDialog start");
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        int[] iArr = new int[phoneCount];
        int[] iArr2 = new int[phoneCount];
        String[] strArr = new String[phoneCount];
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        ArrayList arrayList3 = new ArrayList();
        ArrayList arrayList4 = new ArrayList();
        int i = 0;
        int i2 = 0;
        for (int i3 = 0; i3 < phoneCount; i3++) {
            strArr[i3] = SystemProperties.get(PROPERTY_SIM_ICCID[i3]);
            logd("currIccid[" + i3 + "] : " + strArr[i3]);
            if (strArr[i3] == null || "".equals(strArr[i3])) {
                Log.e(LOG_TAG, "iccid not found, wait for next sim state change");
                return 3;
            }
            if (!"N/A".equals(strArr[i3])) {
                i++;
                i2 |= 1 << i3;
            }
        }
        if (i < 2) {
            logd("isNeedShowSimDialog: insert sim count < 2, do not show dialog");
            return 1;
        }
        if (!getSimInfo(iArr, iArr2, i2)) {
            Log.e(LOG_TAG, "isNeedShowSimDialog: Can't get SIM information");
            return 2;
        }
        for (int i4 = 0; i4 < phoneCount; i4++) {
            if (1 == iArr2[i4]) {
                arrayList.add(Integer.valueOf(i4));
            } else if (iArr2[i4] == 0) {
                arrayList2.add(Integer.valueOf(i4));
            }
            if (3 == iArr[i4]) {
                arrayList3.add(Integer.valueOf(i4));
            } else {
                arrayList4.add(Integer.valueOf(i4));
            }
        }
        logd("usimIndexList size = " + arrayList.size());
        logd("op02IndexList size = " + arrayList3.size());
        if (arrayList.size() >= 2) {
            int i5 = 0;
            for (int i6 = 0; i6 < arrayList.size(); i6++) {
                if (arrayList3.contains(arrayList.get(i6))) {
                    i5++;
                }
            }
            if (i5 == 1) {
                logd("isNeedShowSimDialog: One OP02Usim inserted, not show dialog");
                return 1;
            }
        } else {
            if (arrayList.size() == 1) {
                logd("isNeedShowSimDialog: One Usim inserted, not show dialog");
                return 1;
            }
            int i7 = 0;
            for (int i8 = 0; i8 < arrayList2.size(); i8++) {
                if (arrayList3.contains(arrayList2.get(i8))) {
                    i7++;
                }
            }
            if (i7 == 1) {
                logd("isNeedShowSimDialog: One non-OP02 Usim inserted, not show dialog");
                return 1;
            }
        }
        logd("isNeedShowSimDialog: Show dialog");
        return 0;
    }

    public static boolean isAnySimLocked(int i) {
        if (RatConfiguration.isC2kSupported()) {
            logd("isAnySimLocked always returns false in C2K");
            return false;
        }
        String[] strArr = new String[i];
        String[] strArr2 = new String[i];
        for (int i2 = 0; i2 < i; i2++) {
            strArr2[i2] = SystemProperties.get(PROPERTY_SIM_ICCID[i2]);
            if (!strArr2[i2].equals("N/A")) {
                strArr[i2] = TelephonyManager.getTelephonyProperty(i2, "vendor.gsm.sim.operator.numeric", "");
                if (strArr[i2].length() >= 6) {
                    strArr[i2] = strArr[i2].substring(0, 6);
                } else if (strArr[i2].length() >= 5) {
                    strArr[i2] = strArr[i2].substring(0, 5);
                }
                if (!strArr[i2].equals("")) {
                    logd("i = " + i2 + " from gsm.sim.operator.numeric:" + strArr[i2] + " ,iccid = " + strArr2[i2]);
                }
            }
            if (!strArr2[i2].equals("N/A") && (strArr[i2].equals("") || strArr[i2].equals("sim_lock"))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isPS2SupportLTE() {
        if (SystemProperties.get("persist.vendor.radio.mtk_ps2_rat").indexOf(76) != -1) {
            logd("isPS2SupportLTE = true");
            return true;
        }
        logd("isPS2SupportLTE = false");
        return false;
    }

    public static boolean isTPlusWSupport() {
        if (SystemProperties.get("vendor.ril.simswitch.tpluswsupport").equals("1")) {
            logd("return true for T+W support");
            return true;
        }
        return false;
    }

    public static void updateSimImsiStatus(int i, String str) {
        logd("updateSimImsiStatus slot = " + i + ", value = " + str);
        SystemProperties.set(PROPERTY_SIM_IMSI_STATUS[i], str);
    }

    private static String getSimImsiStatus(int i) {
        return SystemProperties.get(PROPERTY_SIM_IMSI_STATUS[i], "0");
    }

    public static void clearAllSimImsiStatus() {
        logd("clearAllSimImsiStatus");
        for (int i = 0; i < PROPERTY_SIM_IMSI_STATUS.length; i++) {
            updateSimImsiStatus(i, "0");
        }
    }

    public static boolean isDssNoResetSupport() {
        if (SystemProperties.get("vendor.ril.simswitch.no_reset_support").equals("1")) {
            logd("return true for isDssNoResetSupport");
            return true;
        }
        logd("return false for isDssNoResetSupport");
        return false;
    }

    public static int getProtocolStackId(int i) {
        int mainCapabilityPhoneId = getMainCapabilityPhoneId();
        if (i == mainCapabilityPhoneId) {
            return 1;
        }
        if (isDssNoResetSupport()) {
            if (i < mainCapabilityPhoneId) {
                return i + 2;
            }
        } else if (i == 0) {
            return mainCapabilityPhoneId + 1;
        }
        return i + 1;
    }

    public static String getHashCode(String str) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(str.getBytes());
            return new String(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RadioCapabilitySwitchUtil SHA-256 must exist");
        }
    }
}
