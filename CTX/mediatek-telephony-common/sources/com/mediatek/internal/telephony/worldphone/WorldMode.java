package com.mediatek.internal.telephony.worldphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.MtkRIL;

public class WorldMode extends Handler {
    static final String ACTION_ADB_SWITCH_WORLD_MODE = "android.intent.action.ACTION_ADB_SWITCH_WORLD_MODE";
    public static final String ACTION_WORLD_MODE_CHANGED = "mediatek.intent.action.ACTION_WORLD_MODE_CHANGED";
    static final int EVENT_RADIO_ON_1 = 1;
    static final int EVENT_RADIO_ON_2 = 2;
    static final int EVENT_RADIO_ON_3 = 3;
    static final int EVENT_RADIO_ON_4 = 4;
    static final String EXTRA_WORLDMODE = "worldMode";
    public static final String EXTRA_WORLD_MODE_CHANGE_STATE = "worldModeState";
    private static final String LOG_TAG = "WORLDMODE";
    public static final int MASK_CDMA = 32;
    public static final int MASK_GSM = 1;
    public static final int MASK_LTEFDD = 16;
    public static final int MASK_LTETDD = 8;
    public static final int MASK_TDSCDMA = 2;
    public static final int MASK_WCDMA = 4;
    public static final int MD_WM_CHANGED_END = 1;
    public static final int MD_WM_CHANGED_START = 0;
    public static final int MD_WM_CHANGED_UNKNOWN = -1;
    public static final int MD_WORLD_MODE_LCTG = 16;
    public static final int MD_WORLD_MODE_LFCTG = 21;
    public static final int MD_WORLD_MODE_LFTG = 20;
    public static final int MD_WORLD_MODE_LFWCG = 15;
    public static final int MD_WORLD_MODE_LFWG = 14;
    public static final int MD_WORLD_MODE_LTCTG = 17;
    public static final int MD_WORLD_MODE_LTG = 8;
    public static final int MD_WORLD_MODE_LTTG = 13;
    public static final int MD_WORLD_MODE_LTWCG = 19;
    public static final int MD_WORLD_MODE_LTWG = 18;
    public static final int MD_WORLD_MODE_LWCG = 11;
    public static final int MD_WORLD_MODE_LWCTG = 12;
    public static final int MD_WORLD_MODE_LWG = 9;
    public static final int MD_WORLD_MODE_LWTG = 10;
    public static final int MD_WORLD_MODE_UNKNOWN = 0;
    static final int WORLD_MODE_RESULT_ERROR = 101;
    static final int WORLD_MODE_RESULT_SUCCESS = 100;
    static final int WORLD_MODE_RESULT_WM_ID_NOT_SUPPORT = 102;
    private static WorldMode sInstance;
    private final BroadcastReceiver mWorldModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WorldMode.logd("[Receiver]+");
            String action = intent.getAction();
            WorldMode.logd("Action: " + action);
            if (WorldMode.ACTION_WORLD_MODE_CHANGED.equals(action)) {
                int intExtra = intent.getIntExtra(WorldMode.EXTRA_WORLD_MODE_CHANGE_STATE, -1);
                WorldMode.logd("wmState: " + intExtra);
                if (intExtra == 1) {
                    int unused = WorldMode.sCurrentWorldMode = WorldMode.updateCurrentWorldMode();
                }
            } else if (WorldMode.ACTION_ADB_SWITCH_WORLD_MODE.equals(action)) {
                int intExtra2 = intent.getIntExtra(WorldMode.EXTRA_WORLDMODE, 0);
                WorldMode.logd("toMode: " + intExtra2);
                if (intExtra2 >= 8 && intExtra2 <= 21) {
                    WorldMode.setWorldMode(intExtra2);
                }
            }
            WorldMode.logd("[Receiver]-");
        }
    };
    private static final int PROJECT_SIM_NUM = WorldPhoneUtil.getProjectSimNum();
    private static int sCurrentWorldMode = updateCurrentWorldMode();
    private static int sActiveWorldMode = 0;
    private static boolean sSwitchingState = false;
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];
    private static Context sContext = null;
    private static CommandsInterface[] smCi = new CommandsInterface[PROJECT_SIM_NUM];
    private static MtkRIL[] sCi = new MtkRIL[PROJECT_SIM_NUM];
    private static int sUpdateSwitchingFlag = 0;

    public WorldMode() {
        logd("Constructor Init world mode: " + sCurrentWorldMode + "sSwitchingState: " + sSwitchingState);
        sProxyPhones = PhoneFactory.getPhones();
        int i = 0;
        while (i < PROJECT_SIM_NUM) {
            sActivePhones[i] = sProxyPhones[i];
            smCi[i] = sActivePhones[i].mCi;
            sCi[i] = (MtkRIL) smCi[i];
            MtkRIL mtkRIL = sCi[i];
            i++;
            mtkRIL.registerForOn(this, i, null);
        }
        IntentFilter intentFilter = new IntentFilter(ACTION_WORLD_MODE_CHANGED);
        intentFilter.addAction(ACTION_ADB_SWITCH_WORLD_MODE);
        if (PhoneFactory.getDefaultPhone() != null) {
            sContext = PhoneFactory.getDefaultPhone().getContext();
        } else {
            logd("DefaultPhone = null");
        }
        sContext.registerReceiver(this.mWorldModeReceiver, intentFilter);
    }

    public static void init() {
        synchronized (WorldMode.class) {
            if (sInstance == null) {
                sInstance = new WorldMode();
            } else {
                logd("init() called multiple times!  sInstance = " + sInstance);
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        WorldPhoneUtil.getMajorSim();
        switch (message.what) {
            case 1:
                logd("handleMessage : <EVENT_RADIO_ON_1>");
                if (WorldPhoneUtil.getMajorSim() == 0) {
                    sCurrentWorldMode = updateCurrentWorldMode();
                }
                break;
            case 2:
                logd("handleMessage : <EVENT_RADIO_ON_2>");
                if (WorldPhoneUtil.getMajorSim() == 1) {
                    sCurrentWorldMode = updateCurrentWorldMode();
                }
                break;
            case 3:
                logd("handleMessage : <EVENT_RADIO_ON_3>");
                if (WorldPhoneUtil.getMajorSim() == 2) {
                    sCurrentWorldMode = updateCurrentWorldMode();
                }
                break;
            case 4:
                logd("handleMessage : <EVENT_RADIO_ON_4>");
                if (WorldPhoneUtil.getMajorSim() == 3) {
                    sCurrentWorldMode = updateCurrentWorldMode();
                }
                break;
            default:
                logd("Unknown msg:" + message.what);
                break;
        }
    }

    private static boolean checkWmCapability(int i, int i2) {
        int i3 = 19;
        if (i == 8) {
            i3 = 27;
        } else if (i == 13) {
            i3 = 11;
        } else if (i == 10) {
            i3 = 31;
        } else if (i == 14) {
            i3 = 21;
        } else if (i == 9) {
            i3 = 29;
        } else if (i == 12) {
            i3 = 63;
        } else if (i == 16) {
            i3 = 59;
        } else if (i == 17) {
            i3 = 43;
        } else if (i == 15) {
            i3 = 53;
        } else if (i == 11) {
            i3 = 61;
        } else if (i == 18) {
            i3 = 13;
        } else if (i == 19) {
            i3 = 45;
        } else if (i != 20) {
            i3 = i == 21 ? 51 : 0;
        }
        if (true == WorldPhoneUtil.isC2kSupport()) {
            i2 |= 32;
        }
        if (true == WorldPhoneUtil.isWorldPhoneSupport() && (4 == (i3 & 4) || 2 == (i3 & 2))) {
            i2 = i2 | 4 | 2;
        }
        logd("checkWmCapability: modem=" + i + " rat=" + i3 + " bnadMode=" + i2);
        return i3 == (i3 & i2) && (i3 & 32) == (i2 & 32);
    }

    public static int setWorldModeWithBand(int i, int i2) {
        if (!checkWmCapability(i, i2)) {
            logd("setWorldModeWithBand: not match, modem=" + i + " bandMode=" + i2);
            return 102;
        }
        setWorldMode(i);
        return 100;
    }

    public static void setWorldMode(int i) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        logd("[setWorldMode]protocolSim: " + majorSim);
        if (majorSim >= 0 && majorSim <= 3) {
            setWorldMode(sCi[majorSim], i);
        } else {
            setWorldMode(sCi[0], i);
        }
    }

    private static void setWorldMode(MtkRIL mtkRIL, int i) {
        logd("[setWorldMode] worldMode=" + i);
        if (i == sCurrentWorldMode) {
            if (i == 8) {
                logd("Already in uTLG mode");
                return;
            }
            if (i == 9) {
                logd("Already in uLWG mode");
                return;
            }
            if (i == 10) {
                logd("Already in uLWTG mode");
                return;
            }
            if (i == 11) {
                logd("Already in uLWCG mode");
                return;
            }
            if (i == 12) {
                logd("Already in uLWTCG mode");
                return;
            }
            if (i == 13) {
                logd("Already in LtTG mode");
                return;
            }
            if (i == 14) {
                logd("Already in LfWG mode");
                return;
            }
            if (i == 15) {
                logd("Already in uLfWCG mode");
                return;
            }
            if (i == 16) {
                logd("Already in uLCTG mode");
                return;
            }
            if (i == 17) {
                logd("Already in uLtCTG mode");
                return;
            }
            if (i == 18) {
                logd("Already in uLtWG mode");
                return;
            }
            if (i == 19) {
                logd("Already in uLtWCG mode");
                return;
            } else if (i == 20) {
                logd("Already in uLfTG mode");
                return;
            } else {
                if (i == 21) {
                    logd("Already in uLfCTG mode");
                    return;
                }
                return;
            }
        }
        if (mtkRIL.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
            logd("Radio unavailable, can not switch world mode");
            return;
        }
        if (i >= 8 && i <= 21) {
            mtkRIL.reloadModemType(i, null);
            mtkRIL.storeModemType(i, null);
            mtkRIL.resetRadio(null);
        } else {
            logd("Invalid world mode:" + i);
        }
    }

    public static int getWorldMode() {
        sCurrentWorldMode = Integer.valueOf(SystemProperties.get("vendor.ril.active.md", Integer.toString(0))).intValue();
        logd("getWorldMode=" + WorldModeToString(sCurrentWorldMode));
        return sCurrentWorldMode;
    }

    private static int updateCurrentWorldMode() {
        sCurrentWorldMode = Integer.valueOf(SystemProperties.get("vendor.ril.active.md", Integer.toString(0))).intValue();
        logd("updateCurrentWorldMode=" + WorldModeToString(sCurrentWorldMode));
        return sCurrentWorldMode;
    }

    public static boolean updateSwitchingState(boolean z) {
        if (!z && !isWorldModeSwitching()) {
            sUpdateSwitchingFlag++;
            logd("sUpdateSwitchingFlag+ =" + sUpdateSwitchingFlag);
            return false;
        }
        if (sUpdateSwitchingFlag > 0 && true == z) {
            sUpdateSwitchingFlag--;
            logd("sUpdateSwitchingFlag- =" + sUpdateSwitchingFlag);
            return false;
        }
        sSwitchingState = z;
        logd("updateSwitchingState=" + sSwitchingState);
        return true;
    }

    public static boolean resetSwitchingState(int i) {
        logd("reset sUpdateSwitchingFlag = " + sUpdateSwitchingFlag);
        sUpdateSwitchingFlag = 0;
        logd("reset sSwitchingState = " + sSwitchingState);
        sSwitchingState = false;
        return true;
    }

    public static boolean isWorldModeSwitching() {
        if (sSwitchingState) {
            return true;
        }
        return false;
    }

    public static String WorldModeToString(int i) {
        if (i == 8) {
            return "uTLG";
        }
        if (i == 9) {
            return "uLWG";
        }
        if (i == 10) {
            return "uLWTG";
        }
        if (i == 11) {
            return "uLWCG";
        }
        if (i == 12) {
            return "uLWTCG";
        }
        if (i == 13) {
            return "LtTG";
        }
        if (i == 14) {
            return "LfWG";
        }
        if (i == 15) {
            return "uLfWCG";
        }
        if (i == 16) {
            return "uLCTG";
        }
        if (i == 17) {
            return "uLtCTG";
        }
        if (i == 18) {
            return "uLtWG";
        }
        if (i == 19) {
            return "uLtWCG";
        }
        if (i == 20) {
            return "uLfTG";
        }
        if (i == 21) {
            return "uLfCTG";
        }
        return "Invalid world mode";
    }

    private static void logd(String str) {
        Rlog.d("WORLDMODE", "[WorldMode]" + str);
    }
}
