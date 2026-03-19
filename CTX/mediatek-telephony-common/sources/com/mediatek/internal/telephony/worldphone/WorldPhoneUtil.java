package com.mediatek.internal.telephony.worldphone;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkProxyController;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;

public class WorldPhoneUtil implements IWorldPhone {
    private static final int ACTIVE_MD_TYPE_LTG = 4;
    private static final int ACTIVE_MD_TYPE_LWCG = 5;
    private static final int ACTIVE_MD_TYPE_LWG = 3;
    private static final int ACTIVE_MD_TYPE_LfWG = 7;
    private static final int ACTIVE_MD_TYPE_LtTG = 6;
    private static final int ACTIVE_MD_TYPE_TG = 2;
    private static final int ACTIVE_MD_TYPE_UNKNOWN = 0;
    private static final int ACTIVE_MD_TYPE_WG = 1;
    public static final int CARD_TYPE_CSIM = 8;
    public static final int CARD_TYPE_NONE = 0;
    public static final int CARD_TYPE_RUIM = 4;
    public static final int CARD_TYPE_SIM = 1;
    public static final int CARD_TYPE_USIM = 2;
    public static final int CSFB_ON_SLOT = -1;
    private static final boolean IS_WORLD_MODE_SUPPORT;
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getSimCount();
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE;
    public static final int RADIO_TECH_MODE_CSFB = 2;
    public static final int RADIO_TECH_MODE_SVLTE = 3;
    public static final int RADIO_TECH_MODE_UNKNOWN = 1;
    public static final int SVLTE_ON_SLOT_0 = 0;
    public static final int SVLTE_ON_SLOT_1 = 1;
    public static final String SVLTE_PROP = "persist.vendor.radio.svlte_slot";
    public static final int UTRAN_DIVISION_DUPLEX_MODE_FDD = 1;
    public static final int UTRAN_DIVISION_DUPLEX_MODE_TDD = 2;
    public static final int UTRAN_DIVISION_DUPLEX_MODE_UNKNOWN = 0;
    private static int[] mC2KWPCardtype;
    private static Phone[] sActivePhones;
    private static int[] sCardModes;
    private static Context sContext;
    private static Phone sDefultPhone;
    private static Phone[] sProxyPhones;
    public static boolean sSimSwitching;
    public static int sToModem;
    private static IWorldPhone sWorldPhone;

    static {
        IS_WORLD_MODE_SUPPORT = SystemProperties.getInt("ro.vendor.mtk_md_world_mode_support", 0) == 1;
        sContext = null;
        sDefultPhone = null;
        sProxyPhones = null;
        sActivePhones = new Phone[PROJECT_SIM_NUM];
        sToModem = 0;
        sSimSwitching = false;
        sCardModes = initCardModes();
        PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"vendor.gsm.ril.fulluicctype", "vendor.gsm.ril.fulluicctype.2", "vendor.gsm.ril.fulluicctype.3", "vendor.gsm.ril.fulluicctype.4"};
        mC2KWPCardtype = new int[TelephonyManager.getDefault().getPhoneCount()];
        sWorldPhone = null;
    }

    public WorldPhoneUtil() {
        logd("Constructor invoked");
        sDefultPhone = PhoneFactory.getDefaultPhone();
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = sProxyPhones[i];
        }
        if (sDefultPhone != null) {
            sContext = sDefultPhone.getContext();
        } else {
            logd("DefaultPhone = null");
        }
    }

    public static void makeWorldPhoneManager() {
        if (isWorldModeSupport() && isWorldPhoneSupport()) {
            logd("Factory World mode support");
            WorldMode.init();
        } else if (isWorldPhoneSupport()) {
            logd("Factory World phone support");
            sWorldPhone = WorldPhoneWrapper.getWorldPhoneInstance();
        } else {
            logd("Factory World phone not support");
        }
    }

    public static IWorldPhone getWorldPhone() {
        if (sWorldPhone == null) {
            logd("sWorldPhone is null");
        }
        return sWorldPhone;
    }

    public static int getProjectSimNum() {
        return PROJECT_SIM_NUM;
    }

    public static int getMajorSim() {
        if (!((MtkProxyController) ProxyController.getInstance()).isCapabilitySwitching()) {
            String str = SystemProperties.get("persist.vendor.radio.simswitch", "");
            if (str != null && !str.equals("")) {
                StringBuilder sb = new StringBuilder();
                sb.append("[getMajorSim]: ");
                sb.append(Integer.parseInt(str) - 1);
                logd(sb.toString());
                return Integer.parseInt(str) - 1;
            }
            logd("[getMajorSim]: fail to get major SIM");
            return -99;
        }
        logd("[getMajorSim]: radio capability is switching");
        return -99;
    }

    public static int getModemSelectionMode() {
        if (sContext == null) {
            logd("sContext = null");
            return 1;
        }
        return SystemProperties.getInt(IWorldPhone.WORLD_PHONE_AUTO_SELECT_MODE, 1);
    }

    public static boolean isWorldPhoneSupport() {
        return RatConfiguration.isWcdmaSupported() && RatConfiguration.isTdscdmaSupported();
    }

    public static boolean isLteSupport() {
        return RatConfiguration.isLteFddSupported() || RatConfiguration.isLteTddSupported();
    }

    public static String regionToString(int i) {
        switch (i) {
            case 0:
                return "REGION_UNKNOWN";
            case 1:
                return "REGION_DOMESTIC";
            case 2:
                return "REGION_FOREIGN";
            default:
                return "Invalid Region";
        }
    }

    public static String stateToString(int i) {
        switch (i) {
            case 0:
                return "STATE_IN_SERVICE";
            case 1:
                return "STATE_OUT_OF_SERVICE";
            case 2:
                return "STATE_EMERGENCY_ONLY";
            case 3:
                return "STATE_POWER_OFF";
            default:
                return "Invalid State";
        }
    }

    public static String denyReasonToString(int i) {
        switch (i) {
            case 0:
                return "CAMP_ON_NOT_DENIED";
            case 1:
                return "CAMP_ON_DENY_REASON_UNKNOWN";
            case 2:
                return "CAMP_ON_DENY_REASON_NEED_SWITCH_TO_FDD";
            case 3:
                return "CAMP_ON_DENY_REASON_NEED_SWITCH_TO_TDD";
            case 4:
                return "CAMP_ON_DENY_REASON_DOMESTIC_FDD_MD";
            default:
                return "Invalid Reason";
        }
    }

    public static String iccCardTypeToString(int i) {
        switch (i) {
            case 0:
                return "Icc Card Type Unknown";
            case 1:
                return "SIM";
            case 2:
                return "USIM";
            default:
                return "Invalid Icc Card Type";
        }
    }

    @Override
    public void setModemSelectionMode(int i, int i2) {
    }

    @Override
    public void notifyRadioCapabilityChange(int i) {
    }

    public static boolean isWorldModeSupport() {
        return IS_WORLD_MODE_SUPPORT;
    }

    public static int get3GDivisionDuplexMode() {
        int i;
        switch (getActiveModemType()) {
            case 1:
            case 3:
            case 5:
            case 7:
                i = 1;
                break;
            case 2:
            case 4:
            case 6:
                i = 2;
                break;
            default:
                i = 0;
                break;
        }
        logd("get3GDivisionDuplexMode=" + i);
        return i;
    }

    private static int getActiveModemType() {
        int i = 1;
        int iIntValue = -1;
        if (isWorldModeSupport()) {
            int worldMode = WorldMode.getWorldMode();
            iIntValue = Integer.valueOf(SystemProperties.get("vendor.ril.nw.worldmode.activemode", Integer.toString(0))).intValue();
            switch (worldMode) {
                case 8:
                case 16:
                case 20:
                case 21:
                    i = 4;
                    break;
                case 9:
                case 18:
                    i = 3;
                    break;
                case 10:
                case 12:
                    if (iIntValue <= 0) {
                        i = 0;
                    } else if (iIntValue != 1) {
                        i = iIntValue != 2 ? 0 : 4;
                    } else {
                        i = 3;
                    }
                    break;
                case 11:
                case 15:
                case 19:
                    i = 5;
                    break;
                case 13:
                case 17:
                    i = 6;
                    break;
                case 14:
                    i = 7;
                    break;
                default:
                    i = 0;
                    break;
            }
        } else {
            switch (ModemSwitchHandler.getActiveModemType()) {
                case 3:
                    break;
                case 4:
                    i = 2;
                    break;
                case 5:
                    i = 3;
                    break;
                case 6:
                    i = 4;
                    break;
                default:
                    i = 0;
                    break;
            }
        }
        logd("getActiveModemType=" + i + " activeMode=" + iIntValue);
        return i;
    }

    public static boolean isWorldPhoneSwitching() {
        if (isWorldModeSupport()) {
            return WorldMode.isWorldModeSwitching();
        }
        return false;
    }

    private static int[] initCardModes() {
        int[] iArr = new int[TelephonyManager.getDefault().getPhoneCount()];
        String[] strArrSplit = SystemProperties.get(SVLTE_PROP, "3,2,2,2").split(",");
        for (int i = 0; i < iArr.length; i++) {
            if (i < strArrSplit.length) {
                iArr[i] = Integer.parseInt(strArrSplit[i]);
            } else {
                iArr[i] = 1;
            }
        }
        return iArr;
    }

    private static int getFullCardType(int i) {
        if (i < 0 || i >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("getFullCardType invalid slotId:" + i);
            return 0;
        }
        String str = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[i]);
        String[] strArrSplit = str.split(",");
        int i2 = 0;
        for (int i3 = 0; i3 < strArrSplit.length; i3++) {
            if ("USIM".equals(strArrSplit[i3])) {
                i2 |= 2;
            } else if ("SIM".equals(strArrSplit[i3])) {
                i2 |= 1;
            } else if ("CSIM".equals(strArrSplit[i3])) {
                i2 |= 8;
            } else if ("RUIM".equals(strArrSplit[i3])) {
                i2 |= 4;
            }
        }
        logd("getFullCardType fullType=" + i2 + " cardType =" + str);
        return i2;
    }

    public static int[] getC2KWPCardType() {
        for (int i = 0; i < mC2KWPCardtype.length; i++) {
            mC2KWPCardtype[i] = getFullCardType(i);
            logd("getC2KWPCardType mC2KWPCardtype[" + i + "]=" + mC2KWPCardtype[i]);
        }
        return mC2KWPCardtype;
    }

    public static int getActiveSvlteModeSlotId() {
        int i = -1;
        if (!isCdmaLteDcSupport()) {
            logd("[getActiveSvlteModeSlotId] SVLTE not support, return -1.");
            return -1;
        }
        for (int i2 = 0; i2 < sCardModes.length; i2++) {
            if (sCardModes[i2] == 3) {
                i = i2;
            }
        }
        logd("[getActiveSvlteModeSlotId] slotId: " + i);
        return i;
    }

    public static boolean isCdmaLteDcSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1") || SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals(MtkGsmCdmaPhone.ACT_TYPE_UTRAN)) {
            return true;
        }
        return false;
    }

    public static boolean isC2kSupport() {
        return RatConfiguration.isC2kSupported();
    }

    public static boolean getSimLockedState(int i) {
        if (i == 2 || i == 3 || i == 4 || i == 7 || i == 0) {
            return true;
        }
        return false;
    }

    public static void saveToModemType(int i) {
        sToModem = i;
    }

    public static int getToModemType() {
        return sToModem;
    }

    public static boolean isSimSwitching() {
        return sSimSwitching;
    }

    public static void setSimSwitchingFlag(boolean z) {
        sSimSwitching = z;
    }

    private static void logd(String str) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPP_UTIL]" + str);
    }
}
