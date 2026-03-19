package com.mediatek.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

public class ModemSwitchHandler {
    private static final String LOG_TAG = "PHONE";
    public static final int MD_TYPE_FDD = 100;
    public static final int MD_TYPE_LTG = 6;
    public static final int MD_TYPE_LWG = 5;
    public static final int MD_TYPE_TDD = 101;
    public static final int MD_TYPE_TG = 4;
    public static final int MD_TYPE_UNKNOWN = 0;
    public static final int MD_TYPE_WG = 3;
    private static final String PROPERTY_SILENT_REBOOT_MD1 = "gsm.ril.eboot";
    private static final int PROJECT_SIM_NUM = WorldPhoneUtil.getProjectSimNum();
    private static int sCurrentModemType = initActiveModemType();
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];
    private static Context sContext = null;
    private static CommandsInterface[] sCi = new CommandsInterface[PROJECT_SIM_NUM];

    public ModemSwitchHandler() {
        logd("Constructor invoked");
        logd("Init modem type: " + sCurrentModemType);
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = sProxyPhones[i];
            sCi[i] = sActivePhones[i].mCi;
        }
        if (PhoneFactory.getDefaultPhone() != null) {
            sContext = PhoneFactory.getDefaultPhone().getContext();
        } else {
            logd("DefaultPhone = null");
        }
    }

    public static void switchModem(int modemType) {
        int protocolSim = WorldPhoneUtil.getMajorSim();
        logd("protocolSim: " + protocolSim);
        if (protocolSim >= 0 && protocolSim <= 3) {
            switchModem(sCi[protocolSim], modemType);
        } else {
            logd("switchModem protocolSim is invalid");
        }
    }

    public static void switchModem(int isStoreModemType, int modemType) {
        int protocolSim = WorldPhoneUtil.getMajorSim();
        logd("protocolSim: " + protocolSim);
        if (protocolSim >= 0 && protocolSim <= 3) {
            switchModem(isStoreModemType, sCi[protocolSim], modemType);
        } else {
            logd("switchModem protocolSim is invalid");
        }
    }

    public static void switchModem(CommandsInterface ci, int modemType) {
        logd("[switchModem] need store modem type");
        switchModem(1, ci, modemType);
    }

    public static void switchModem(int isStoreModemType, CommandsInterface ci, int modemType) {
        logd("[switchModem]");
        if (ci.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not switch modem");
            return;
        }
        sCurrentModemType = getActiveModemType();
        if (modemType == sCurrentModemType) {
            if (modemType == 3) {
                logd("Already in WG modem");
                return;
            }
            if (modemType == 4) {
                logd("Already in TG modem");
                return;
            } else if (modemType == 5) {
                logd("Already in FDD CSFB modem");
                return;
            } else {
                if (modemType == 6) {
                    logd("Already in TDD CSFB modem");
                    return;
                }
                return;
            }
        }
        setModemType(isStoreModemType, ci, modemType);
        setActiveModemType(modemType);
        logd("Broadcast intent ACTION_MD_TYPE_CHANGE");
        Intent intent = new Intent("android.intent.action.ACTION_MD_TYPE_CHANGE");
        intent.putExtra("mdType", modemType);
        sContext.sendBroadcast(intent);
    }

    private static boolean setModemType(int isStoreModemType, CommandsInterface ci, int modemType) {
        if (ci.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not switch world mode");
            return false;
        }
        if (modemType >= 3 && modemType <= 6) {
            logd("silent reboot isStroeModemType=" + isStoreModemType);
            ci.reloadModemType(modemType, null);
            if (1 == isStoreModemType) {
                ci.storeModemType(modemType, null);
            }
            SystemProperties.set(PROPERTY_SILENT_REBOOT_MD1, "1");
            ci.resetRadio(null);
            return true;
        }
        logd("Invalid modemType:" + modemType);
        return false;
    }

    public static void reloadModem(int modemType) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        if (majorSim >= 0 && majorSim <= 3) {
            reloadModem(sCi[majorSim], modemType);
        } else {
            logd("Invalid MajorSIM id" + majorSim);
        }
    }

    public static void reloadModem(CommandsInterface ci, int modemType) {
        logd("[reloadModem]");
        if (ci.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not reload modem");
        } else {
            ci.reloadModemType(modemType, null);
        }
    }

    public static int getActiveModemType() {
        if (!WorldPhoneUtil.isWorldPhoneSupport() || WorldPhoneUtil.isWorldModeSupport()) {
            sCurrentModemType = Integer.valueOf(SystemProperties.get("ril.active.md", Integer.toString(0))).intValue();
        }
        logd("[getActiveModemType] " + sCurrentModemType);
        return sCurrentModemType;
    }

    public static int initActiveModemType() {
        sCurrentModemType = Integer.valueOf(SystemProperties.get("ril.active.md", Integer.toString(0))).intValue();
        logd("[initActiveModemType] " + sCurrentModemType);
        return sCurrentModemType;
    }

    public static void setActiveModemType(int modemType) {
        SystemProperties.set("ril.active.md", Integer.toString(modemType));
        sCurrentModemType = modemType;
        logd("[setActiveModemType] " + modemToString(sCurrentModemType));
    }

    public static String modemToString(int modemType) {
        if (modemType == 3) {
            return "WG";
        }
        if (modemType == 4) {
            return "TG";
        }
        if (modemType == 5) {
            return "FDD CSFB";
        }
        if (modemType == 6) {
            return "TDD CSFB";
        }
        if (modemType == 0) {
            return "UNKNOWN";
        }
        return "Invalid modem type";
    }

    private static void logd(String msg) {
        Rlog.d("PHONE", "[MSH]" + msg);
    }
}
