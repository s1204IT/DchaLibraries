package com.mediatek.internal.telephony.worldphone;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.ratconfiguration.RatConfiguration;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;

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
    private static final boolean IS_LTE_SUPPORT;
    private static final boolean IS_WORLD_MODE_SUPPORT;
    private static final int PROJECT_SIM_NUM = TelephonyManager.getDefault().getSimCount();
    private static final String PROPERTY_MAJOR_SIM = "persist.radio.simswitch";
    private static final String[] PROPERTY_RIL_FULL_UICC_TYPE;
    public static final int RADIO_TECH_MODE_CSFB = 2;
    public static final int RADIO_TECH_MODE_SVLTE = 3;
    public static final int RADIO_TECH_MODE_UNKNOWN = 1;
    public static final int SVLTE_ON_SLOT_0 = 0;
    public static final int SVLTE_ON_SLOT_1 = 1;
    public static final String SVLTE_PROP = "persist.radio.svlte_slot";
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

    static {
        IS_LTE_SUPPORT = SystemProperties.getInt("ro.boot.opt_lte_support", 0) == 1;
        IS_WORLD_MODE_SUPPORT = SystemProperties.getInt("ro.mtk_md_world_mode_support", 0) == 1;
        sContext = null;
        sDefultPhone = null;
        sProxyPhones = null;
        sActivePhones = new Phone[PROJECT_SIM_NUM];
        sToModem = 0;
        sSimSwitching = false;
        sCardModes = initCardModes();
        PROPERTY_RIL_FULL_UICC_TYPE = new String[]{"gsm.ril.fulluicctype", "gsm.ril.fulluicctype.2", "gsm.ril.fulluicctype.3", "gsm.ril.fulluicctype.4"};
        mC2KWPCardtype = new int[TelephonyManager.getDefault().getPhoneCount()];
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

    public static int getProjectSimNum() {
        return PROJECT_SIM_NUM;
    }

    public static int getMajorSim() {
        if (!ProxyController.getInstance().isCapabilitySwitching()) {
            String currMajorSim = SystemProperties.get(PROPERTY_MAJOR_SIM, UsimPBMemInfo.STRING_NOT_SET);
            if (currMajorSim != null && !currMajorSim.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                logd("[getMajorSim]: " + (Integer.parseInt(currMajorSim) - 1));
                return Integer.parseInt(currMajorSim) - 1;
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
        return Settings.Global.getInt(sContext.getContentResolver(), "world_phone_auto_select_mode", 1);
    }

    public static boolean isWorldPhoneSupport() {
        if (RatConfiguration.isWcdmaSupported()) {
            return RatConfiguration.isTdscdmaSupported();
        }
        return false;
    }

    public static boolean isLteSupport() {
        return IS_LTE_SUPPORT;
    }

    public static String regionToString(int region) {
        switch (region) {
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

    public static String stateToString(int state) {
        switch (state) {
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

    public static String regStateToString(int regState) {
        switch (regState) {
            case 0:
                return "REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING";
            case 1:
                return "REGISTRATION_STATE_HOME_NETWORK";
            case 2:
                return "REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING";
            case 3:
                return "REGISTRATION_STATE_REGISTRATION_DENIED";
            case 4:
                return "REGISTRATION_STATE_UNKNOWN";
            case 5:
                return "REGISTRATION_STATE_ROAMING";
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            default:
                return "Invalid RegState";
            case 10:
                return "REGISTRATION_STATE_NOT_REGISTERED_AND_NOT_SEARCHING_EMERGENCY_CALL_ENABLED";
            case 12:
                return "REGISTRATION_STATE_NOT_REGISTERED_AND_SEARCHING_EMERGENCY_CALL_ENABLED";
            case 13:
                return "REGISTRATION_STATE_REGISTRATION_DENIED_EMERGENCY_CALL_ENABLED";
            case 14:
                return "REGISTRATION_STATE_UNKNOWN_EMERGENCY_CALL_ENABLED";
        }
    }

    public static String denyReasonToString(int reason) {
        switch (reason) {
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

    public static String iccCardTypeToString(int iccCardType) {
        switch (iccCardType) {
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
    public void setModemSelectionMode(int mode, int modemType) {
    }

    @Override
    public void notifyRadioCapabilityChange(int capailitySimId) {
    }

    public static boolean isWorldModeSupport() {
        return IS_WORLD_MODE_SUPPORT;
    }

    public static int get3GDivisionDuplexMode() {
        int duplexMode;
        int activeMdType = getActiveModemType();
        switch (activeMdType) {
            case 1:
            case 3:
            case 5:
            case 7:
                duplexMode = 1;
                break;
            case 2:
            case 4:
            case 6:
                duplexMode = 2;
                break;
            default:
                duplexMode = 0;
                break;
        }
        logd("get3GDivisionDuplexMode=" + duplexMode);
        return duplexMode;
    }

    private static int getActiveModemType() {
        int activeMdType = 0;
        if (!isWorldModeSupport()) {
            int modemType = ModemSwitchHandler.getActiveModemType();
            switch (modemType) {
                case 3:
                    activeMdType = 1;
                    break;
                case 4:
                    activeMdType = 2;
                    break;
                case 5:
                    activeMdType = 3;
                    break;
                case 6:
                    activeMdType = 4;
                    break;
                default:
                    activeMdType = 0;
                    break;
            }
        } else {
            int modemType2 = WorldMode.getWorldMode();
            int activeMode = Integer.valueOf(SystemProperties.get("ril.nw.worldmode.activemode", Integer.toString(0))).intValue();
            logd("[getActiveModemType]: activeMode" + activeMode);
            switch (modemType2) {
                case 8:
                case 16:
                case 20:
                case 21:
                    activeMdType = 4;
                    break;
                case 9:
                case 18:
                    activeMdType = 3;
                    break;
                case 10:
                case 12:
                    if (activeMode > 0) {
                        if (activeMode == 1) {
                            activeMdType = 3;
                        } else if (activeMode == 2) {
                            activeMdType = 4;
                        }
                    }
                    break;
                case 11:
                case 15:
                case 19:
                    activeMdType = 5;
                    break;
                case 13:
                case 17:
                    activeMdType = 6;
                    break;
                case 14:
                    activeMdType = 7;
                    break;
                default:
                    activeMdType = 0;
                    break;
            }
        }
        logd("getActiveModemType=" + activeMdType);
        return activeMdType;
    }

    public static boolean isWorldPhoneSwitching() {
        if (isWorldModeSupport()) {
            return WorldMode.isWorldModeSwitching();
        }
        return false;
    }

    private static int[] initCardModes() {
        int[] cardModes = new int[TelephonyManager.getDefault().getPhoneCount()];
        String[] svlteType = SystemProperties.get(SVLTE_PROP, "3,2,2,2").split(",");
        for (int i = 0; i < cardModes.length; i++) {
            if (i < svlteType.length) {
                cardModes[i] = Integer.parseInt(svlteType[i]);
            } else {
                cardModes[i] = 1;
            }
        }
        return cardModes;
    }

    private static int getFullCardType(int slotId) {
        if (slotId < 0 || slotId >= TelephonyManager.getDefault().getPhoneCount()) {
            logd("getFullCardType invalid slotId:" + slotId);
            return 0;
        }
        String cardType = SystemProperties.get(PROPERTY_RIL_FULL_UICC_TYPE[slotId]);
        String[] appType = cardType.split(",");
        int fullType = 0;
        for (int i = 0; i < appType.length; i++) {
            if ("USIM".equals(appType[i])) {
                fullType |= 2;
            } else if ("SIM".equals(appType[i])) {
                fullType |= 1;
            } else if ("CSIM".equals(appType[i])) {
                fullType |= 8;
            } else if ("RUIM".equals(appType[i])) {
                fullType |= 4;
            }
        }
        logd("getFullCardType fullType=" + fullType + " cardType =" + cardType);
        return fullType;
    }

    public static int[] getC2KWPCardType() {
        for (int i = 0; i < mC2KWPCardtype.length; i++) {
            mC2KWPCardtype[i] = getFullCardType(i);
            logd("getC2KWPCardType mC2KWPCardtype[" + i + "]=" + mC2KWPCardtype[i]);
        }
        return mC2KWPCardtype;
    }

    public static int getActiveSvlteModeSlotId() {
        int svlteSlotId = -1;
        if (!isCdmaLteDcSupport()) {
            logd("[getActiveSvlteModeSlotId] SVLTE not support, return -1.");
            return -1;
        }
        for (int i = 0; i < sCardModes.length; i++) {
            if (sCardModes[i] == 3) {
                svlteSlotId = i;
            }
        }
        logd("[getActiveSvlteModeSlotId] slotId: " + svlteSlotId);
        return svlteSlotId;
    }

    public static boolean isCdmaLteDcSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals("1") || SystemProperties.get("ro.boot.opt_c2k_lte_mode").equals(Phone.ACT_TYPE_UTRAN)) {
            return true;
        }
        return false;
    }

    public static boolean isC2kSupport() {
        if (SystemProperties.get("ro.boot.opt_c2k_support").equals("1")) {
            return true;
        }
        return false;
    }

    public static void saveToModemType(int modemType) {
        sToModem = modemType;
    }

    public static int getToModemType() {
        return sToModem;
    }

    public static boolean isSimSwitching() {
        return sSimSwitching;
    }

    public static void setSimSwitchingFlag(boolean flag) {
        sSimSwitching = flag;
    }

    private static void logd(String msg) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPP_UTIL]" + msg);
    }
}
