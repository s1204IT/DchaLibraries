package com.mediatek.internal.telephony;

import android.os.SystemProperties;
import android.telephony.RadioAccessFamily;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.ArrayList;
import java.util.Arrays;

public class RadioCapabilitySwitchUtil {
    public static final String CN_MCC = "460";
    public static final int DO_SWITCH = 0;
    public static final int ICCID_ERROR = 3;
    public static final String IMSI_NOT_READY = "0";
    public static final int IMSI_NOT_READY_OR_SIM_LOCKED = 2;
    public static final String IMSI_READY = "1";
    private static final String LOG_TAG = "GSM";
    public static final String MAIN_SIM_PROP = "persist.radio.simswitch.iccid";
    public static final int NOT_SHOW_DIALOG = 1;
    public static final int NOT_SWITCH = 1;
    public static final int NOT_SWITCH_SIM_INFO_NOT_READY = 2;
    private static final String NO_SIM_VALUE = "N/A";
    public static final int OP01_6M_PRIORITY_OP01_SIM = 1;
    public static final int OP01_6M_PRIORITY_OP01_USIM = 0;
    public static final int OP01_6M_PRIORITY_OTHER = 2;
    private static final String PROPERTY_ICCID = "ril.iccid.sim";
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
    private static final String[] PROPERTY_SIM_ICCID = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    private static final String[] PROPERTY_SIM_IMSI_STATUS = {"ril.imsi.status.sim1", "ril.imsi.status.sim2", "ril.imsi.status.sim3", "ril.imsi.status.sim4"};

    public static void updateIccid(Phone[] mProxyPhones) {
        for (int i = 0; i < mProxyPhones.length; i++) {
            boolean bIsMajorPhone = false;
            int phoneRAF = mProxyPhones[i].getRadioAccessFamily();
            logd("updateIccid, phone[" + i + "] RAF = " + phoneRAF);
            if ((phoneRAF & 2) == 2) {
                bIsMajorPhone = true;
            }
            if (bIsMajorPhone) {
                String currIccId = SystemProperties.get(PROPERTY_ICCID + (i + 1));
                SystemProperties.set(MAIN_SIM_PROP, currIccId);
                logd("updateIccid " + currIccId);
                return;
            }
        }
    }

    public static boolean getSimInfo(int[] simOpInfo, int[] simType, int insertedStatus) {
        String[] strMnc = new String[simOpInfo.length];
        String[] strSimType = new String[simOpInfo.length];
        int i = 0;
        while (i < simOpInfo.length) {
            strSimType[i] = SystemProperties.get(i == 0 ? "gsm.ril.uicctype" : "gsm.ril.uicctype." + (i + 1), UsimPBMemInfo.STRING_NOT_SET);
            if (strSimType[i].equals("SIM")) {
                simType[i] = 0;
            } else if (strSimType[i].equals("USIM")) {
                simType[i] = 1;
            } else {
                simType[i] = 2;
            }
            logd("SimType[" + i + "]= " + strSimType[i] + ", simType[" + i + "]=" + simType[i]);
            String propStr = i == 0 ? "gsm.sim.ril.mcc.mnc" : "gsm.sim.ril.mcc.mnc." + (i + 1);
            strMnc[i] = SystemProperties.get(propStr, UsimPBMemInfo.STRING_NOT_SET);
            if (!strMnc[i].equals(UsimPBMemInfo.STRING_NOT_SET) && !strMnc[i].equals("error") && !strMnc[i].equals("sim_absent")) {
                logd("strMnc[" + i + "] from ril.mcc.mnc:" + strMnc[i]);
            } else if ("1".equals(getSimImsiStatus(i))) {
                if (isC2kSupport() && simType[i] == 2) {
                    propStr = "ril.uim.subscriberid." + (i + 1);
                    strMnc[i] = SystemProperties.get(propStr, UsimPBMemInfo.STRING_NOT_SET);
                }
                if (strMnc[i].equals(UsimPBMemInfo.STRING_NOT_SET) || strMnc[i].equals(NO_SIM_VALUE) || strMnc[i].equals("error") || strMnc[i].equals("sim_absent")) {
                    propStr = "gsm.sim.operator.imsi";
                    strMnc[i] = TelephonyManager.getTelephonyProperty(i, "gsm.sim.operator.imsi", UsimPBMemInfo.STRING_NOT_SET);
                }
                if (strMnc[i].length() >= 6) {
                    strMnc[i] = strMnc[i].substring(0, 6);
                } else if (strMnc[i].length() >= 5) {
                    strMnc[i] = strMnc[i].substring(0, 5);
                }
                logd("strMnc[" + i + "] from " + propStr + ":" + strMnc[i]);
            }
            logd("insertedStatus:" + insertedStatus);
            if (insertedStatus >= 0 && ((1 << i) & insertedStatus) > 0) {
                if (strMnc[i].equals(UsimPBMemInfo.STRING_NOT_SET) || strMnc[i].equals("error")) {
                    logd("SIM is inserted but no imsi");
                    return false;
                }
                if (strMnc[i].equals("sim_lock")) {
                    logd("SIM is lock, wait pin unlock");
                    return false;
                }
                if (strMnc[i].equals(NO_SIM_VALUE) || strMnc[i].equals("sim_absent")) {
                    logd("strMnc have invalid value, return false");
                    return false;
                }
            }
            String[] strArr = PLMN_TABLE_OP01;
            int i2 = 0;
            int length = strArr.length;
            while (true) {
                if (i2 >= length) {
                    break;
                }
                String mccmnc = strArr[i2];
                if (strMnc[i].startsWith(mccmnc)) {
                    simOpInfo[i] = 2;
                    break;
                }
                i2++;
            }
            if (simOpInfo[i] == 0) {
                String[] strArr2 = PLMN_TABLE_OP02;
                int i3 = 0;
                int length2 = strArr2.length;
                while (true) {
                    if (i3 >= length2) {
                        break;
                    }
                    String mccmnc2 = strArr2[i3];
                    if (strMnc[i].startsWith(mccmnc2)) {
                        simOpInfo[i] = 3;
                        break;
                    }
                    i3++;
                }
            }
            if (simOpInfo[i] == 0) {
                String[] strArr3 = PLMN_TABLE_OP09;
                int i4 = 0;
                int length3 = strArr3.length;
                while (true) {
                    if (i4 >= length3) {
                        break;
                    }
                    String mccmnc3 = strArr3[i4];
                    if (strMnc[i].startsWith(mccmnc3)) {
                        simOpInfo[i] = 4;
                        break;
                    }
                    i4++;
                }
            }
            if (SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET).equals("OP18") && simOpInfo[i] == 0) {
                String[] strArr4 = PLMN_TABLE_OP18;
                int i5 = 0;
                int length4 = strArr4.length;
                while (true) {
                    if (i5 >= length4) {
                        break;
                    }
                    String mccmnc4 = strArr4[i5];
                    if (strMnc[i].startsWith(mccmnc4)) {
                        simOpInfo[i] = 4;
                        break;
                    }
                    i5++;
                }
            }
            if (simOpInfo[i] == 0 && !strMnc[i].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                simOpInfo[i] = 1;
            }
            logd("strMnc[" + i + "]= " + strMnc[i] + ", simOpInfo[" + i + "]=" + simOpInfo[i]);
            i++;
        }
        logd("getSimInfo(simOpInfo): " + Arrays.toString(simOpInfo));
        logd("getSimInfo(simType): " + Arrays.toString(simType));
        return true;
    }

    public static int isNeedSwitchInOpPackage(Phone[] mProxyPhones, RadioAccessFamily[] rats) {
        String operatorSpec = SystemProperties.get("persist.operator.optr", UsimPBMemInfo.STRING_NOT_SET);
        int[] iArr = new int[mProxyPhones.length];
        int[] iArr2 = new int[mProxyPhones.length];
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        String[] strArr = new String[phoneCount];
        logd("Operator Spec:" + operatorSpec);
        if (!SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
            return 0;
        }
        logd("mtk_disable_cap_switch is true");
        return 1;
    }

    public static int getHigherPrioritySimForOp01(int curId, boolean[] op01Usim, boolean[] op01Sim, boolean[] overseaUsim, boolean[] overseaSim) {
        int targetSim = -1;
        int phoneNum = op01Usim.length;
        if (op01Usim[curId]) {
            return curId;
        }
        for (int i = 0; i < phoneNum; i++) {
            if (op01Usim[i]) {
                targetSim = i;
            }
        }
        if (targetSim != -1 || op01Sim[curId]) {
            return targetSim;
        }
        for (int i2 = 0; i2 < phoneNum; i2++) {
            if (op01Sim[i2]) {
                targetSim = i2;
            }
        }
        if (targetSim != -1 || overseaUsim[curId]) {
            return targetSim;
        }
        for (int i3 = 0; i3 < phoneNum; i3++) {
            if (overseaUsim[i3]) {
                targetSim = i3;
            }
        }
        if (targetSim != -1 || overseaSim[curId]) {
            return targetSim;
        }
        for (int i4 = 0; i4 < phoneNum; i4++) {
            if (overseaSim[i4]) {
                targetSim = i4;
            }
        }
        return targetSim;
    }

    public static int getHighestPriorityPhone(int capPhoneId, int[] priority) {
        int targetPhone = 0;
        int phoneNum = priority.length;
        int highestPriorityCount = 0;
        int highestPriorityBitMap = 0;
        for (int i = 0; i < phoneNum; i++) {
            if (priority[i] < priority[targetPhone]) {
                targetPhone = i;
                highestPriorityCount = 1;
                highestPriorityBitMap = 1 << i;
            } else if (priority[i] == priority[targetPhone]) {
                highestPriorityCount++;
                highestPriorityBitMap |= 1 << i;
            }
        }
        if (highestPriorityCount == 1) {
            return targetPhone;
        }
        if (capPhoneId == -1 || ((1 << capPhoneId) & highestPriorityBitMap) == 0) {
            return -1;
        }
        return capPhoneId;
    }

    private static boolean checkOp01(int targetPhoneId, int[] simOpInfo, int[] simType) {
        int curPhoneId = Integer.valueOf(SystemProperties.get("persist.radio.simswitch", "1")).intValue() - 1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        int phoneNum = simOpInfo.length;
        String[] currIccId = new String[phoneNum];
        for (int i = 0; i < phoneNum; i++) {
            currIccId[i] = SystemProperties.get(PROPERTY_ICCID + (i + 1));
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        logd("checkOp01 : curPhoneId: " + curPhoneId + ", insertedSimCount: " + insertedSimCount);
        if (insertedSimCount == 1) {
            logd("checkOp01 : single SIM case, switch!");
            return true;
        }
        if (simOpInfo[targetPhoneId] == 2) {
            if (simType[targetPhoneId] == 0) {
                if (simOpInfo[curPhoneId] == 2 && simType[curPhoneId] != 0) {
                    logd("checkOp01 : case 1,2; stay in current phone");
                    return false;
                }
                logd("checkOp01 : case 3,4");
                return true;
            }
            logd("checkOp01 : case 1,2");
            return true;
        }
        if (simOpInfo[targetPhoneId] == 1) {
            if (simOpInfo[curPhoneId] == 2) {
                logd("checkOp01 : case 1,2,3,4; stay in current phone");
                return false;
            }
            if (simType[targetPhoneId] == 0) {
                if (simOpInfo[curPhoneId] == 1 && simType[curPhoneId] != 0) {
                    logd("checkOp01 : case 5,6; stay in current phone");
                    return false;
                }
                logd("checkOp01 : case 7,8");
                return true;
            }
            logd("checkOp01 : case 5,6");
            return true;
        }
        if (insertedSimCount == 2 && simType[curPhoneId] == 2 && simType[targetPhoneId] == 2) {
            logd("checkOp01 : case C+C, switch!");
            return true;
        }
        if (simOpInfo[targetPhoneId] == 0) {
            logd("checkOp01 : case 10, target IMSI not ready");
            if (insertedStatus <= 2) {
                logd("checkOp01 : case 10, single SIM case, switch!");
                return true;
            }
        }
        if (SystemProperties.get("ro.mtk_world_phone_policy").equals("1") && simOpInfo[curPhoneId] != 2 && simOpInfo[curPhoneId] != 1) {
            logd("checkOp01 : case 11, op01-B, switch it!");
            return true;
        }
        logd("checkOp01 : case 9");
        return false;
    }

    private static boolean checkOp01LC(int targetPhoneId, int[] simOpInfo, int[] simType) {
        int curPhoneId = Integer.valueOf(SystemProperties.get("persist.radio.simswitch", "1")).intValue() - 1;
        int insertedSimCount = 0;
        int insertedStatus = 0;
        int phoneNum = simOpInfo.length;
        String[] currIccId = new String[phoneNum];
        int[] priority = new int[phoneNum];
        for (int i = 0; i < phoneNum; i++) {
            currIccId[i] = SystemProperties.get(PROPERTY_ICCID + (i + 1));
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
            if (simOpInfo[i] == 2) {
                if (simType[i] == 1) {
                    priority[i] = 0;
                } else if (simType[i] == 0) {
                    priority[i] = 1;
                }
            } else {
                priority[i] = 2;
            }
        }
        logd("checkOp01LC(curPhoneId): " + curPhoneId);
        logd("checkOp01LC(insertedSimCount): " + insertedSimCount);
        if (insertedSimCount == 1) {
            logd("checkOp01LC : single SIM case, switch!");
            return true;
        }
        if (priority[targetPhoneId] <= priority[curPhoneId]) {
            logd("checkOp01LC : target priority greater than or equal to current, switch!");
            return true;
        }
        logd("checkOp01LC : target priority lower than current; stay in current phone");
        return false;
    }

    private static boolean checkOp18(int targetPhoneId, int[] simOpInfo, int[] simType) {
        int curPhoneId = Integer.valueOf(SystemProperties.get("persist.radio.simswitch", "1")).intValue() - 1;
        logd("checkOp18 : curPhoneId: " + curPhoneId);
        if (simOpInfo[targetPhoneId] == 4) {
            logd("checkOp18 : case 1");
            return true;
        }
        if (simOpInfo[curPhoneId] == 4) {
            logd("checkOp18 : case 2; stay in current phone");
            return false;
        }
        logd("checkOp18 : case 3; all are not op18");
        return true;
    }

    public static int getMainCapabilityPhoneId() {
        int phoneId = SystemProperties.getInt("persist.radio.simswitch", 1) - 1;
        Log.d(LOG_TAG, "[RadioCapSwitchUtil] getMainCapabilityPhoneId " + phoneId);
        return phoneId;
    }

    private static void logd(String s) {
        Log.d(LOG_TAG, "[RadioCapSwitchUtil] " + s);
    }

    public static int isNeedShowSimDialog() {
        if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
            logd("mtk_disable_cap_switch is true");
            return 0;
        }
        logd("isNeedShowSimDialog start");
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        int[] simOpInfo = new int[phoneCount];
        int[] simType = new int[phoneCount];
        String[] currIccId = new String[phoneCount];
        int insertedSimCount = 0;
        int insertedStatus = 0;
        int op02CardCount = 0;
        ArrayList<Integer> usimIndexList = new ArrayList<>();
        ArrayList<Integer> simIndexList = new ArrayList<>();
        ArrayList<Integer> op02IndexList = new ArrayList<>();
        ArrayList<Integer> otherIndexList = new ArrayList<>();
        for (int i = 0; i < phoneCount; i++) {
            currIccId[i] = SystemProperties.get(PROPERTY_SIM_ICCID[i]);
            logd("currIccid[" + i + "] : " + currIccId[i]);
            if (currIccId[i] == null || UsimPBMemInfo.STRING_NOT_SET.equals(currIccId[i])) {
                Log.e(LOG_TAG, "iccid not found, wait for next sim state change");
                return 3;
            }
            if (!NO_SIM_VALUE.equals(currIccId[i])) {
                insertedSimCount++;
                insertedStatus |= 1 << i;
            }
        }
        if (insertedSimCount < 2) {
            logd("isNeedShowSimDialog: insert sim count < 2, do not show dialog");
            return 1;
        }
        if (!getSimInfo(simOpInfo, simType, insertedStatus)) {
            Log.e(LOG_TAG, "isNeedShowSimDialog: Can't get SIM information");
            return 2;
        }
        for (int i2 = 0; i2 < phoneCount; i2++) {
            if (1 == simType[i2]) {
                usimIndexList.add(Integer.valueOf(i2));
            } else if (simType[i2] == 0) {
                simIndexList.add(Integer.valueOf(i2));
            }
            if (3 == simOpInfo[i2]) {
                op02IndexList.add(Integer.valueOf(i2));
            } else {
                otherIndexList.add(Integer.valueOf(i2));
            }
        }
        logd("usimIndexList size = " + usimIndexList.size());
        logd("op02IndexList size = " + op02IndexList.size());
        if (usimIndexList.size() >= 2) {
            for (int i3 = 0; i3 < usimIndexList.size(); i3++) {
                if (op02IndexList.contains(usimIndexList.get(i3))) {
                    op02CardCount++;
                }
            }
            if (op02CardCount == 1) {
                logd("isNeedShowSimDialog: One OP02Usim inserted, not show dialog");
                return 1;
            }
        } else {
            if (usimIndexList.size() == 1) {
                logd("isNeedShowSimDialog: One Usim inserted, not show dialog");
                return 1;
            }
            for (int i4 = 0; i4 < simIndexList.size(); i4++) {
                if (op02IndexList.contains(simIndexList.get(i4))) {
                    op02CardCount++;
                }
            }
            if (op02CardCount == 1) {
                logd("isNeedShowSimDialog: One non-OP02 Usim inserted, not show dialog");
                return 1;
            }
        }
        logd("isNeedShowSimDialog: Show dialog");
        return 0;
    }

    public static boolean isAnySimLocked(int phoneNum) {
        String propStr;
        String propStr2;
        if (SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1") || SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals(Phone.ACT_TYPE_UTRAN)) {
            logd("isAnySimLocked always returns false in C2K");
            return false;
        }
        String[] mnc = new String[phoneNum];
        String[] iccid = new String[phoneNum];
        for (int i = 0; i < phoneNum; i++) {
            iccid[i] = SystemProperties.get(PROPERTY_SIM_ICCID[i]);
            if (!iccid[i].equals(NO_SIM_VALUE)) {
                mnc[i] = TelephonyManager.getTelephonyProperty(i, "gsm.sim.operator.imsi", UsimPBMemInfo.STRING_NOT_SET);
                if (mnc[i].length() >= 6) {
                    mnc[i] = mnc[i].substring(0, 6);
                } else if (mnc[i].length() >= 5) {
                    mnc[i] = mnc[i].substring(0, 5);
                }
                if (mnc[i].equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    if (i == 0) {
                        propStr2 = "gsm.sim.ril.mcc.mnc";
                    } else {
                        propStr2 = "gsm.sim.ril.mcc.mnc." + (i + 1);
                    }
                    mnc[i] = SystemProperties.get(propStr2, UsimPBMemInfo.STRING_NOT_SET);
                    logd("mnc[" + i + "] from ril.mcc.mnc:" + mnc[i] + " ,iccid = " + iccid[i]);
                } else {
                    logd("i = " + i + " from gsm.sim.operator.imsi:" + mnc[i] + " ,iccid = " + iccid[i]);
                }
            }
            if (!iccid[i].equals(NO_SIM_VALUE) && (mnc[i].equals(UsimPBMemInfo.STRING_NOT_SET) || mnc[i].equals("sim_lock"))) {
                return true;
            }
            if ("1".equals(getSimImsiStatus(i))) {
                logd("clear mcc.mnc:" + i);
                if (i == 0) {
                    propStr = "gsm.sim.ril.mcc.mnc";
                } else {
                    propStr = "gsm.sim.ril.mcc.mnc." + (i + 1);
                }
                SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
            }
        }
        return false;
    }

    public static boolean isC2kSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_support").equals("1")) {
            logd("return true for C2K project");
            return true;
        }
        return false;
    }

    public static boolean isPS2SupportLTE() {
        if (SystemProperties.get("persist.radio.mtk_ps2_rat").indexOf(76) != -1) {
            logd("isPS2SupportLTE = true");
            return true;
        }
        logd("isPS2SupportLTE = false");
        return false;
    }

    public static void updateSimImsiStatus(int slot, String value) {
        logd("updateSimImsiStatus slot = " + slot + ", value = " + value);
        String propStr = PROPERTY_SIM_IMSI_STATUS[slot];
        SystemProperties.set(propStr, value);
    }

    private static String getSimImsiStatus(int slot) {
        String propStr = PROPERTY_SIM_IMSI_STATUS[slot];
        return SystemProperties.get(propStr, "0");
    }

    public static void clearRilMccMnc(int slot) {
        String propStr;
        if (slot == 0) {
            propStr = "gsm.sim.ril.mcc.mnc";
        } else {
            propStr = "gsm.sim.ril.mcc.mnc." + (slot + 1);
        }
        SystemProperties.set(propStr, UsimPBMemInfo.STRING_NOT_SET);
        logd("clear property " + propStr);
    }

    public static void clearAllSimImsiStatus() {
        logd("clearAllSimImsiStatus");
        for (int i = 0; i < PROPERTY_SIM_IMSI_STATUS.length; i++) {
            updateSimImsiStatus(i, "0");
        }
    }

    public static void clearAllRilMccMnc(int phoneNum) {
        logd("clearAllRilMccMnc");
        for (int i = 0; i < phoneNum; i++) {
            clearRilMccMnc(i);
        }
    }
}
