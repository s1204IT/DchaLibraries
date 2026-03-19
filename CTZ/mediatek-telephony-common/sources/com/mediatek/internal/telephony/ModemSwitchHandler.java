package com.mediatek.internal.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;

public class ModemSwitchHandler {
    public static final String ACTION_MD_TYPE_CHANGE = "mediatek.intent.action.ACTION_MD_TYPE_CHANGE";
    public static final String ACTION_MODEM_SWITCH_DONE = "mediatek.intent.action.ACTION_MODEM_SWITCH_DONE";
    private static final int EVENT_RADIO_NOT_AVAILABLE = 2;
    private static final int EVENT_RIL_CONNECTED = 1;
    public static final String EXTRA_MD_TYPE = "mdType";
    private static final String LOG_TAG = "WORLDMODE";
    private static final int MD_SWITCH_DEFAULT = 0;
    private static final int MD_SWITCH_RADIO_UNAVAILABLE = 2;
    private static final int MD_SWITCH_RESET_START = 1;
    public static final int MD_TYPE_FDD = 100;
    public static final int MD_TYPE_LTG = 6;
    public static final int MD_TYPE_LWG = 5;
    public static final int MD_TYPE_TDD = 101;
    public static final int MD_TYPE_TG = 4;
    public static final int MD_TYPE_UNKNOWN = 0;
    public static final int MD_TYPE_WG = 3;
    private static final int PROJECT_SIM_NUM = WorldPhoneUtil.getProjectSimNum();
    private static int sCurrentModemType = initActiveModemType();
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];
    private static Context sContext = null;
    private static CommandsInterface[] smCi = new CommandsInterface[PROJECT_SIM_NUM];
    private static MtkRIL[] sCi = new MtkRIL[PROJECT_SIM_NUM];
    private static int sModemSwitchingFlag = 0;
    private static Handler sWorldPhoneHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            int majorSim = WorldPhoneUtil.getMajorSim();
            ModemSwitchHandler.logd("handleMessage msg.what=" + message.what + " sModemSwitchingFlag=" + ModemSwitchHandler.sModemSwitchingFlag + " protocolSim: " + majorSim);
            switch (message.what) {
                case 1:
                    ModemSwitchHandler.logd("[EVENT_RIL_CONNECTED] mRilVersion=" + ((AsyncResult) message.obj).result);
                    if (ModemSwitchHandler.sModemSwitchingFlag == 2) {
                        int unused = ModemSwitchHandler.sModemSwitchingFlag = 0;
                        ModemSwitchHandler.sContext.sendBroadcast(new Intent(ModemSwitchHandler.ACTION_MODEM_SWITCH_DONE));
                        if (majorSim >= 0 && majorSim <= 3) {
                            ModemSwitchHandler.sCi[majorSim].unregisterForNotAvailable(ModemSwitchHandler.sWorldPhoneHandler);
                            ModemSwitchHandler.sCi[majorSim].unregisterForRilConnected(ModemSwitchHandler.sWorldPhoneHandler);
                            break;
                        }
                    }
                    break;
                case 2:
                    int unused2 = ModemSwitchHandler.sModemSwitchingFlag = 2;
                    break;
            }
        }
    };

    public ModemSwitchHandler() {
        logd("Constructor invoked");
        logd("Init modem type: " + sCurrentModemType);
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = sProxyPhones[i];
            smCi[i] = sActivePhones[i].mCi;
            sCi[i] = (MtkRIL) smCi[i];
        }
        if (PhoneFactory.getDefaultPhone() != null) {
            sContext = PhoneFactory.getDefaultPhone().getContext();
        } else {
            logd("DefaultPhone = null");
        }
    }

    public static void switchModem(int i) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        logd("protocolSim: " + majorSim);
        if (majorSim >= 0 && majorSim <= 3) {
            switchModem(sCi[majorSim], i);
        } else {
            logd("switchModem protocolSim is invalid");
        }
    }

    public static void switchModem(int i, int i2) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        logd("protocolSim: " + majorSim);
        if (majorSim >= 0 && majorSim <= 3) {
            switchModem(i, sCi[majorSim], i2);
        } else {
            logd("switchModem protocolSim is invalid");
        }
    }

    public static void switchModem(MtkRIL mtkRIL, int i) {
        logd("[switchModem] need store modem type");
        switchModem(1, mtkRIL, i);
    }

    public static void switchModem(int i, MtkRIL mtkRIL, int i2) {
        logd("[switchModem]");
        if (mtkRIL.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not switch modem");
            return;
        }
        sCurrentModemType = getActiveModemType();
        if (i2 == sCurrentModemType) {
            if (i2 == 3) {
                logd("Already in WG modem");
                return;
            }
            if (i2 == 4) {
                logd("Already in TG modem");
                return;
            } else if (i2 == 5) {
                logd("Already in FDD CSFB modem");
                return;
            } else {
                if (i2 == 6) {
                    logd("Already in TDD CSFB modem");
                    return;
                }
                return;
            }
        }
        sModemSwitchingFlag = 1;
        mtkRIL.registerForNotAvailable(sWorldPhoneHandler, 2, null);
        mtkRIL.registerForRilConnected(sWorldPhoneHandler, 1, null);
        setModemType(i, mtkRIL, i2);
        setActiveModemType(i2);
        logd("Broadcast intent ACTION_MD_TYPE_CHANGE");
        Intent intent = new Intent(ACTION_MD_TYPE_CHANGE);
        intent.putExtra(EXTRA_MD_TYPE, i2);
        sContext.sendBroadcast(intent);
    }

    private static boolean setModemType(int i, MtkRIL mtkRIL, int i2) {
        if (mtkRIL.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not switch world mode");
            return false;
        }
        if (i2 >= 3 && i2 <= 6) {
            logd("silent reboot isStroeModemType=" + i);
            mtkRIL.reloadModemType(i2, null);
            if (1 == i) {
                mtkRIL.storeModemType(i2, null);
            }
            mtkRIL.setSilentReboot(1, null);
            mtkRIL.resetRadio(null);
            return true;
        }
        logd("Invalid modemType:" + i2);
        return false;
    }

    public static void reloadModem(int i) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        if (majorSim >= 0 && majorSim <= 3) {
            reloadModem(sCi[majorSim], i);
            return;
        }
        logd("Invalid MajorSIM id" + majorSim);
    }

    public static void reloadModem(MtkRIL mtkRIL, int i) {
        logd("[reloadModem]");
        if (mtkRIL.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not reload modem");
        } else {
            mtkRIL.reloadModemType(i, null);
        }
    }

    public static void reloadModemCauseType(MtkRIL mtkRIL, int i) {
        logd("[reloadModemCauseType] " + i);
        mtkRIL.reloadModemType(i, null);
    }

    public static int getActiveModemType() {
        if (!WorldPhoneUtil.isWorldPhoneSupport() || WorldPhoneUtil.isWorldModeSupport()) {
            sCurrentModemType = Integer.valueOf(SystemProperties.get("vendor.ril.active.md", Integer.toString(0))).intValue();
        }
        logd("[getActiveModemType] " + sCurrentModemType);
        return sCurrentModemType;
    }

    public static int initActiveModemType() {
        sCurrentModemType = Integer.valueOf(SystemProperties.get("vendor.ril.active.md", Integer.toString(0))).intValue();
        logd("[initActiveModemType] " + sCurrentModemType);
        return sCurrentModemType;
    }

    public static void setActiveModemType(int i) {
        sCurrentModemType = i;
        logd("[setActiveModemType] " + modemToString(sCurrentModemType));
    }

    public static boolean isModemTypeSwitching() {
        logd("[isModemTypeSwitching]: sModemSwitchingFlag = " + sModemSwitchingFlag);
        if (sModemSwitchingFlag != 0) {
            return true;
        }
        return false;
    }

    public static String modemToString(int i) {
        if (i == 3) {
            return "WG";
        }
        if (i == 4) {
            return "TG";
        }
        if (i == 5) {
            return "FDD CSFB";
        }
        if (i == 6) {
            return "TDD CSFB";
        }
        if (i == 0) {
            return "UNKNOWN";
        }
        return "Invalid modem type";
    }

    private static void logd(String str) {
        Rlog.d("WORLDMODE", "[MSH]" + str);
    }
}
