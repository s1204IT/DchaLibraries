package com.mediatek.internal.telephony.worldphone;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.MtkRIL;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import mediatek.telephony.MtkServiceState;

public class WorldPhoneOp01 extends Handler implements IWorldPhone {
    private static final int EMSR_STANDBY_TIMER = 8;
    private static int sBtSapState;
    private static int sDataRegState;
    private static int sDenyReason;
    private static int sFddStandByCounter;
    private static boolean sIsAutoSelectEnable;
    private static boolean sIsResumeCampingFail1;
    private static boolean sIsResumeCampingFail2;
    private static boolean sIsResumeCampingFail3;
    private static boolean sIsResumeCampingFail4;
    private static int sIsWaintInFddTimeOut;
    private static int sIsWaintInTddTimeOut;
    private static String sLastPlmn;
    private static int sMajorSim;
    private static String[] sNwPlmnStrings;
    private static String sPlmnSs;
    private static int sRegion;
    private static int sRilDataRadioTechnology;
    private static int sRilDataRegState;
    private static int sRilVoiceRadioTechnology;
    private static int sRilVoiceRegState;
    private static MtkServiceState sServiceState;
    private static int sSimLocked;
    private static int sSwitchModemCauseType;
    private static int sTddStandByCounter;
    private static int sUserType;
    private static boolean sVoiceCapable;
    private static int sVoiceRegState;
    private static boolean sWaitInEmsrResume;
    private static boolean sWaitInFdd;
    private static boolean sWaitInTdd;
    private static Object sLock = new Object();
    private static final int PROJECT_SIM_NUM = WorldPhoneUtil.getProjectSimNum();
    private static final int[] FDD_STANDBY_TIMER = {60};
    private static final int[] TDD_STANDBY_TIMER = {40};
    private static final String[] PLMN_TABLE_TYPE1 = {"46000", "46002", "46004", "46007", "46008", "00101", "00211", "00321", "00431", "00541", "00651", "00761", "00871", "00902", "01012", "01122", "01232", "46602", "50270"};
    private static final String[] PLMN_TABLE_TYPE3 = {"46001", "46006", "46009", "45407", "46003", "46005", "45502", "46011"};
    private static final String[] MCC_TABLE_DOMESTIC = {RadioCapabilitySwitchUtil.CN_MCC, "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011", "012"};
    private static final String[] PROPERTY_RIL_CT3G = {"vendor.gsm.ril.ct3g", "vendor.gsm.ril.ct3g.2", "vendor.gsm.ril.ct3g.3", "vendor.gsm.ril.ct3g.4"};
    private static Context sContext = null;
    private static Phone sDefultPhone = null;
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];
    private static CommandsInterface[] smCi = new CommandsInterface[PROJECT_SIM_NUM];
    private static MtkRIL[] sCi = new MtkRIL[PROJECT_SIM_NUM];
    private static String[] sImsi = new String[PROJECT_SIM_NUM];
    private static int sDefaultBootuUpModem = 0;
    private static int[] sSuspendId = new int[PROJECT_SIM_NUM];
    private static int[] sIccCardType = new int[PROJECT_SIM_NUM];
    private static boolean[] sIsInvalidSim = new boolean[PROJECT_SIM_NUM];
    private static boolean[] sSuspendWaitImsi = new boolean[PROJECT_SIM_NUM];
    private static boolean[] sFirstSelect = new boolean[PROJECT_SIM_NUM];
    private static UiccController sUiccController = null;
    private static IccRecords[] sIccRecordsInstance = new IccRecords[PROJECT_SIM_NUM];
    private static ModemSwitchHandler sModemSwitchHandler = null;
    private boolean mIsRegisterEccStateReceiver = false;
    private final BroadcastReceiver mWorldPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            WorldPhoneOp01.logd("Action: " + action);
            if (action.equals("android.telephony.action.SIM_APPLICATION_STATE_CHANGED")) {
                int intExtra = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                int intExtra2 = intent.getIntExtra("slot", 0);
                int unused = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                WorldPhoneOp01.logd("slotId: " + intExtra2 + " state: " + intExtra + " sMajorSim:" + WorldPhoneOp01.sMajorSim);
                WorldPhoneOp01.this.handleSimApplicationStateChanged(intExtra2, intExtra);
            } else if (action.equals("android.telephony.action.SIM_CARD_STATE_CHANGED")) {
                int intExtra3 = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                int intExtra4 = intent.getIntExtra("slot", 0);
                int unused2 = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                WorldPhoneOp01.logd("slotId: " + intExtra4 + " state: " + intExtra3 + " sMajorSim:" + WorldPhoneOp01.sMajorSim);
                WorldPhoneOp01.this.handleSimCardStateChanged(intExtra4, intExtra3);
            } else if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                if (WorldPhoneOp01.sDefaultBootuUpModem != 100) {
                    if (WorldPhoneOp01.sDefaultBootuUpModem == 101) {
                        if (WorldPhoneUtil.isLteSupport()) {
                            ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 6);
                            WorldPhoneOp01.logd("Reload to TDD CSFB modem");
                        } else {
                            ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 4);
                            WorldPhoneOp01.logd("Reload to TG modem");
                        }
                    }
                } else if (WorldPhoneUtil.isLteSupport()) {
                    ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 5);
                    WorldPhoneOp01.logd("Reload to FDD CSFB modem");
                } else {
                    ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 3);
                    WorldPhoneOp01.logd("Reload to WG modem");
                }
            } else if (action.equals(IWorldPhone.ACTION_ADB_SWITCH_MODEM)) {
                int intExtra5 = intent.getIntExtra(ModemSwitchHandler.EXTRA_MD_TYPE, 0);
                WorldPhoneOp01.logd("toModem: " + intExtra5);
                if (intExtra5 == 3 || intExtra5 == 4 || intExtra5 == 5 || intExtra5 == 6) {
                    WorldPhoneOp01.this.setModemSelectionMode(0, intExtra5);
                } else {
                    WorldPhoneOp01.this.setModemSelectionMode(1, intExtra5);
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (!intent.getBooleanExtra("state", false)) {
                    WorldPhoneOp01.logd("Leave flight mode");
                    String unused3 = WorldPhoneOp01.sLastPlmn = null;
                    for (int i = 0; i < WorldPhoneOp01.PROJECT_SIM_NUM; i++) {
                        WorldPhoneOp01.sIsInvalidSim[i] = false;
                    }
                } else {
                    WorldPhoneOp01.logd("Enter flight mode");
                    for (int i2 = 0; i2 < WorldPhoneOp01.PROJECT_SIM_NUM; i2++) {
                        WorldPhoneOp01.sFirstSelect[i2] = true;
                    }
                    int unused4 = WorldPhoneOp01.sRegion = 0;
                }
            } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                int unused5 = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                if (WorldPhoneUtil.isSimSwitching()) {
                    WorldPhoneUtil.setSimSwitchingFlag(false);
                    ModemSwitchHandler.setActiveModemType(WorldPhoneUtil.getToModemType());
                }
                WorldPhoneOp01.this.handleSimSwitched();
            } else if (action.equals(IWorldPhone.ACTION_SAP_CONNECTION_STATE_CHANGED)) {
                int intExtra6 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                if (intExtra6 == 2) {
                    WorldPhoneOp01.logd("BT_SAP connection state is CONNECTED");
                    int unused6 = WorldPhoneOp01.sBtSapState = 1;
                } else if (intExtra6 == 0) {
                    WorldPhoneOp01.logd("BT_SAP connection state is DISCONNECTED");
                    int unused7 = WorldPhoneOp01.sBtSapState = 0;
                } else {
                    WorldPhoneOp01.logd("BT_SAP connection state is " + intExtra6);
                }
            }
            WorldPhoneOp01.logd("Action: " + action + " handle end");
        }
    };
    private Runnable mTddStandByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            WorldPhoneOp01.access$1308();
            if (WorldPhoneOp01.sTddStandByCounter >= WorldPhoneOp01.TDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOp01.sTddStandByCounter = WorldPhoneOp01.TDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOp01.sBtSapState == 0) {
                WorldPhoneOp01.logd("TDD time out!");
                int unused2 = WorldPhoneOp01.sSwitchModemCauseType = 1;
                int unused3 = WorldPhoneOp01.sIsWaintInTddTimeOut = 1;
                WorldPhoneOp01.logd("sSwitchModemCauseType = " + WorldPhoneOp01.sSwitchModemCauseType);
                WorldPhoneOp01.this.handleSwitchModem(100);
                return;
            }
            WorldPhoneOp01.logd("TDD time out but BT SAP is connected, switch not executed!");
        }
    };
    private Runnable mFddStandByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            WorldPhoneOp01.access$1808();
            if (WorldPhoneOp01.sFddStandByCounter >= WorldPhoneOp01.FDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOp01.sFddStandByCounter = WorldPhoneOp01.FDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOp01.sBtSapState == 0) {
                WorldPhoneOp01.logd("FDD time out!");
                int unused2 = WorldPhoneOp01.sSwitchModemCauseType = 1;
                int unused3 = WorldPhoneOp01.sIsWaintInFddTimeOut = 1;
                WorldPhoneOp01.logd("sSwitchModemCauseType = " + WorldPhoneOp01.sSwitchModemCauseType);
                WorldPhoneOp01.this.handleSwitchModem(101);
                return;
            }
            WorldPhoneOp01.logd("FDD time out but BT SAP is connected, switch not executed!");
        }
    };
    private Runnable mEmsrResumeByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            boolean unused = WorldPhoneOp01.sWaitInEmsrResume = false;
            int unused2 = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
            if (WorldPhoneOp01.sMajorSim != -99 && WorldPhoneOp01.sMajorSim != -1 && WorldPhoneOp01.sSuspendWaitImsi[WorldPhoneOp01.sMajorSim]) {
                WorldPhoneOp01.sCi[WorldPhoneOp01.sMajorSim].setResumeRegistration(WorldPhoneOp01.sSuspendId[WorldPhoneOp01.sMajorSim], WorldPhoneOp01.this.obtainMessage(70 + WorldPhoneOp01.sMajorSim));
            }
        }
    };
    private BroadcastReceiver mWorldPhoneEccStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            WorldPhoneOp01.logd("mWorldPhoneEccStateReceiver, received " + intent.getAction());
            if (!WorldPhoneOp01.this.isEccInProgress()) {
                WorldPhoneOp01.this.unRegisterEccStateReceiver();
                WorldPhoneOp01.this.handleSimSwitched();
            }
        }
    };

    static int access$1308() {
        int i = sTddStandByCounter;
        sTddStandByCounter = i + 1;
        return i;
    }

    static int access$1808() {
        int i = sFddStandByCounter;
        sFddStandByCounter = i + 1;
        return i;
    }

    public WorldPhoneOp01() {
        logd("Constructor invoked");
        sDefultPhone = PhoneFactory.getDefaultPhone();
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = sProxyPhones[i];
            smCi[i] = sActivePhones[i].mCi;
            sCi[i] = (MtkRIL) smCi[i];
            sActivePhones[i].registerForServiceStateChanged(this, 80 + i, (Object) null);
        }
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            sCi[i2].setOnPlmnChangeNotification(this, 10 + i2, null);
            sCi[i2].setOnRegistrationSuspended(this, 30 + i2, null);
            sCi[i2].registerForOn(this, 0 + i2, null);
            sCi[i2].setInvalidSimInfo(this, 60 + i2, null);
            if (WorldPhoneUtil.isC2kSupport()) {
                sCi[i2].registerForGmssRatChanged(this, IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_1 + i2, null);
            }
        }
        sModemSwitchHandler = new ModemSwitchHandler();
        logd(ModemSwitchHandler.modemToString(ModemSwitchHandler.getActiveModemType()));
        IntentFilter intentFilter = new IntentFilter("android.telephony.action.SIM_CARD_STATE_CHANGED");
        intentFilter.addAction("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction(IWorldPhone.ACTION_SHUTDOWN_IPO);
        intentFilter.addAction(IWorldPhone.ACTION_ADB_SWITCH_MODEM);
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction(IWorldPhone.ACTION_SAP_CONNECTION_STATE_CHANGED);
        if (sDefultPhone != null) {
            sContext = sDefultPhone.getContext();
        } else {
            logd("DefaultPhone = null");
        }
        sVoiceCapable = sContext.getResources().getBoolean(R.^attr-private.popupPromptView);
        sContext.registerReceiver(this.mWorldPhoneReceiver, intentFilter);
        sTddStandByCounter = 0;
        sFddStandByCounter = 0;
        sWaitInTdd = false;
        sWaitInFdd = false;
        sWaitInEmsrResume = false;
        sRegion = 0;
        sLastPlmn = null;
        sBtSapState = 0;
        sIsWaintInTddTimeOut = 0;
        sIsWaintInFddTimeOut = 0;
        resetAllProperties();
        if (WorldPhoneUtil.getModemSelectionMode() == 0) {
            logd("Auto select disable");
            sIsAutoSelectEnable = false;
            SystemProperties.set(IWorldPhone.WORLD_PHONE_AUTO_SELECT_MODE, Integer.toString(0));
        } else {
            logd("Auto select enable");
            sIsAutoSelectEnable = true;
            SystemProperties.set(IWorldPhone.WORLD_PHONE_AUTO_SELECT_MODE, Integer.toString(1));
        }
        FDD_STANDBY_TIMER[sFddStandByCounter] = SystemProperties.getInt(IWorldPhone.WORLD_PHONE_FDD_MODEM_TIMER, FDD_STANDBY_TIMER[sFddStandByCounter]);
        SystemProperties.set(IWorldPhone.WORLD_PHONE_FDD_MODEM_TIMER, Integer.toString(FDD_STANDBY_TIMER[sFddStandByCounter]));
        logd("FDD_STANDBY_TIMER = " + FDD_STANDBY_TIMER[sFddStandByCounter] + "s");
        StringBuilder sb = new StringBuilder();
        sb.append("sDefaultBootuUpModem = ");
        sb.append(sDefaultBootuUpModem);
        logd(sb.toString());
    }

    private void handleSimCardStateChanged(int i, int i2) {
        if (i2 == 1) {
            sLastPlmn = null;
            sImsi[i] = "";
            sFirstSelect[i] = true;
            sIsInvalidSim[i] = false;
            sSuspendWaitImsi[i] = false;
            sIccCardType[i] = 0;
            if (i == sMajorSim) {
                logd("Major SIM removed, no world phone service");
                removeModemStandByTimer();
                sUserType = 0;
                sDenyReason = 1;
                sMajorSim = -99;
                sRegion = 0;
                return;
            }
            logd("SIM" + i + " is not major SIM");
        }
    }

    private void handleSimApplicationStateChanged(int i, int i2) {
        if (true == WorldPhoneUtil.getSimLockedState(i2) && sIsAutoSelectEnable && i != sMajorSim) {
            sSimLocked = 1;
        }
        if (!WorldPhoneUtil.getSimLockedState(i2) && sIsAutoSelectEnable && i != sMajorSim && sSimLocked == 1) {
            logd("retry to world mode change after not major sim pin unlock");
            sSimLocked = 0;
            handleSimSwitched();
        }
        if (i2 == 10) {
            if (sMajorSim == -99) {
                sMajorSim = WorldPhoneUtil.getMajorSim();
            }
            sUiccController = UiccController.getInstance();
            if (sUiccController != null) {
                sIccRecordsInstance[i] = sProxyPhones[i].getIccCard().getIccRecords();
                if (sIccRecordsInstance[i] != null) {
                    sImsi[i] = sIccRecordsInstance[i].getIMSI();
                    sIccCardType[i] = getIccCardType(i);
                    if (sIsAutoSelectEnable && i == sMajorSim) {
                        logd("Major SIM");
                        sUserType = getUserType(sImsi[i]);
                        if (sFirstSelect[i]) {
                            sFirstSelect[i] = false;
                            if (sUserType == 1 || sUserType == 2) {
                                sSwitchModemCauseType = 0;
                                logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
                                if (sRegion == 1) {
                                    handleSwitchModem(101);
                                } else if (sRegion == 2) {
                                    handleSwitchModem(100);
                                } else {
                                    logd("Region unknown");
                                }
                            } else if (sUserType == 3) {
                                sSwitchModemCauseType = 255;
                                logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
                                handleSwitchModem(100);
                            }
                        }
                        if (sSuspendWaitImsi[i]) {
                            sSuspendWaitImsi[i] = false;
                            if (sNwPlmnStrings != null) {
                                logd("IMSI fot slot" + i + " now ready, resuming PLMN:" + sNwPlmnStrings[0] + " with ID:" + sSuspendId[i] + " sWaitInEmsrResume:" + sWaitInEmsrResume);
                                if (!sWaitInEmsrResume) {
                                    resumeCampingProcedure(i, false);
                                    return;
                                } else {
                                    resumeCampingProcedure(i, true);
                                    return;
                                }
                            }
                            logd("sNwPlmnStrings is Null");
                            return;
                        }
                        return;
                    }
                    logd("Not major SIM");
                    getUserType(sImsi[i]);
                    if (sSuspendWaitImsi[i]) {
                        sSuspendWaitImsi[i] = false;
                        logd("IMSI fot slot" + i + ", resuming with ID:" + sSuspendId[i]);
                        sCi[i].setResumeRegistration(sSuspendId[i], null);
                        return;
                    }
                    return;
                }
                logd("Null sIccRecordsInstance");
                return;
            }
            logd("Null sUiccController");
        }
    }

    @Override
    public void handleMessage(Message message) {
        AsyncResult asyncResult = (AsyncResult) message.obj;
        int i = message.what;
        switch (i) {
            case 0:
                logd("handleMessage : <EVENT_RADIO_ON_1>");
                handleRadioOn(0);
                break;
            case 1:
                logd("handleMessage : <EVENT_RADIO_ON_2>");
                handleRadioOn(1);
                break;
            case 2:
                logd("handleMessage : <EVENT_RADIO_ON_3>");
                handleRadioOn(2);
                break;
            case 3:
                logd("handleMessage : <EVENT_RADIO_ON_4>");
                handleRadioOn(3);
                break;
            default:
                switch (i) {
                    case 10:
                        logd("handleMessage : <EVENT_REG_PLMN_CHANGED_1>");
                        handlePlmnChange(asyncResult, 0);
                        break;
                    case 11:
                        logd("handleMessage : <EVENT_REG_PLMN_CHANGED_2>");
                        handlePlmnChange(asyncResult, 1);
                        break;
                    case 12:
                        logd("handleMessage : <EVENT_REG_PLMN_CHANGED_3>");
                        handlePlmnChange(asyncResult, 2);
                        break;
                    case 13:
                        logd("handleMessage : <EVENT_REG_PLMN_CHANGED_4>");
                        handlePlmnChange(asyncResult, 3);
                        break;
                    default:
                        switch (i) {
                            case 30:
                                logd("handleMessage : <EVENT_REG_SUSPENDED_1>");
                                handleRegistrationSuspend(asyncResult, 0);
                                break;
                            case 31:
                                logd("handleMessage : <EVENT_REG_SUSPENDED_2>");
                                handleRegistrationSuspend(asyncResult, 1);
                                break;
                            case 32:
                                logd("handleMessage : <EVENT_REG_SUSPENDED_3>");
                                handleRegistrationSuspend(asyncResult, 2);
                                break;
                            case 33:
                                logd("handleMessage : <EVENT_REG_SUSPENDED_4>");
                                handleRegistrationSuspend(asyncResult, 3);
                                break;
                            default:
                                switch (i) {
                                    case 60:
                                        logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_1>");
                                        handleInvalidSimNotify(0, asyncResult);
                                        break;
                                    case 61:
                                        logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_2>");
                                        handleInvalidSimNotify(1, asyncResult);
                                        break;
                                    case 62:
                                        logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_3>");
                                        handleInvalidSimNotify(2, asyncResult);
                                        break;
                                    case 63:
                                        logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_4>");
                                        handleInvalidSimNotify(3, asyncResult);
                                        break;
                                    default:
                                        switch (i) {
                                            case 70:
                                                if (asyncResult.exception != null) {
                                                    logd("handleMessage : <EVENT_RESUME_CAMPING_1> with exception");
                                                    sIsResumeCampingFail1 = true;
                                                }
                                                break;
                                            case 71:
                                                if (asyncResult.exception != null) {
                                                    logd("handleMessage : <EVENT_RESUME_CAMPING_2> with exception");
                                                    sIsResumeCampingFail2 = true;
                                                }
                                                break;
                                            case 72:
                                                if (asyncResult.exception != null) {
                                                    logd("handleMessage : <EVENT_RESUME_CAMPING_3> with exception");
                                                    sIsResumeCampingFail3 = true;
                                                }
                                                break;
                                            case 73:
                                                if (asyncResult.exception != null) {
                                                    logd("handleMessage : <EVENT_RESUME_CAMPING_4> with exception");
                                                    sIsResumeCampingFail4 = true;
                                                }
                                                break;
                                            default:
                                                switch (i) {
                                                    case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_1:
                                                        logd("handleMessage : <EVENT_SERVICE_STATE_CHANGED_1>");
                                                        handleServiceStateChange(asyncResult, 0);
                                                        break;
                                                    case 81:
                                                        logd("handleMessage : <EVENT_SERVICE_STATE_CHANGED_2>");
                                                        handleServiceStateChange(asyncResult, 1);
                                                        break;
                                                    case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_3:
                                                        logd("handleMessage : <EVENT_SERVICE_STATE_CHANGED_3>");
                                                        handleServiceStateChange(asyncResult, 2);
                                                        break;
                                                    case IWorldPhone.EVENT_SERVICE_STATE_CHANGED_4:
                                                        logd("handleMessage : <EVENT_SERVICE_STATE_CHANGED_4>");
                                                        handleServiceStateChange(asyncResult, 3);
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_1:
                                                                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_1>");
                                                                handleGmssRatChange(asyncResult, 0);
                                                                break;
                                                            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_2:
                                                                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_2>");
                                                                handleGmssRatChange(asyncResult, 1);
                                                                break;
                                                            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_3:
                                                                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_3>");
                                                                handleGmssRatChange(asyncResult, 2);
                                                                break;
                                                            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_4:
                                                                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_4>");
                                                                handleGmssRatChange(asyncResult, 3);
                                                                break;
                                                            default:
                                                                logd("Unknown msg:" + message.what);
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
                break;
        }
    }

    private void handleRadioOn(int i) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("handleRadioOn Slot:" + i + " sMajorSim:" + sMajorSim);
        sIsInvalidSim[i] = false;
        switch (i) {
            case 0:
                if (sIsResumeCampingFail1) {
                    logd("try to resume camping again");
                    sCi[i].setResumeRegistration(sSuspendId[i], null);
                    sIsResumeCampingFail1 = false;
                }
                break;
            case 1:
                if (sIsResumeCampingFail2) {
                    logd("try to resume camping again");
                    sCi[i].setResumeRegistration(sSuspendId[i], null);
                    sIsResumeCampingFail2 = false;
                }
                break;
            case 2:
                if (sIsResumeCampingFail3) {
                    logd("try to resume camping again");
                    sCi[i].setResumeRegistration(sSuspendId[i], null);
                    sIsResumeCampingFail3 = false;
                }
                break;
            case 3:
                if (sIsResumeCampingFail4) {
                    logd("try to resume camping again");
                    sCi[i].setResumeRegistration(sSuspendId[i], null);
                    sIsResumeCampingFail4 = false;
                }
                break;
            default:
                logd("unknow slotid");
                break;
        }
    }

    private void handlePlmnChange(AsyncResult asyncResult, int i) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("Slot:" + i + " sMajorSim:" + sMajorSim);
        if (asyncResult.exception == null && asyncResult.result != null) {
            String[] strArr = (String[]) asyncResult.result;
            if (i == sMajorSim) {
                sNwPlmnStrings = strArr;
            }
            for (int i2 = 0; i2 < strArr.length; i2++) {
                logd("plmnString[" + i2 + "]=" + strArr[i2]);
            }
            if (sIsAutoSelectEnable) {
                if (sMajorSim == i && ((sUserType == 1 || sUserType == 2) && sDenyReason != 2)) {
                    searchForDesignateService(strArr[0]);
                }
                sRegion = getRegion(strArr[0]);
                if (sUserType != 3 && sRegion == 2 && sMajorSim != -1) {
                    sSwitchModemCauseType = 0;
                    logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
                    handleSwitchModem(100);
                    return;
                }
                return;
            }
            return;
        }
        logd("AsyncResult is wrong " + asyncResult.exception);
    }

    private static synchronized void initNWPlmnString() {
        if (sNwPlmnStrings == null) {
            sNwPlmnStrings = new String[1];
        }
    }

    private void handleGmssRatChange(AsyncResult asyncResult, int i) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("Slot:" + i + " sMajorSim:" + sMajorSim);
        if (asyncResult.exception == null && asyncResult.result != null) {
            String string = Integer.toString(((int[]) asyncResult.result)[1]);
            logd("[handleGmssRatChange] mccString=" + string);
            if (i == sMajorSim && string.length() >= 3) {
                initNWPlmnString();
                sNwPlmnStrings[0] = string;
            }
            if (sIsAutoSelectEnable) {
                sRegion = getRegion(string);
                if (sUserType != 3 && sRegion == 2 && sMajorSim != -1) {
                    handleSwitchModem(100);
                    return;
                }
                return;
            }
            return;
        }
        logd("AsyncResult is wrong " + asyncResult.exception);
    }

    private void handleServiceStateChange(AsyncResult asyncResult, int i) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("Slot:" + i + " sMajorSim:" + sMajorSim + "RadioState:" + sCi[i].getRadioState());
        if (asyncResult.exception == null && asyncResult.result != null) {
            sServiceState = (MtkServiceState) asyncResult.result;
            if (sServiceState != null) {
                sPlmnSs = sServiceState.getOperatorNumeric();
                sVoiceRegState = sServiceState.getVoiceRegState();
                sRilVoiceRegState = sServiceState.getRilVoiceRegState();
                sRilVoiceRadioTechnology = sServiceState.getRilVoiceRadioTechnology();
                sDataRegState = sServiceState.getDataRegState();
                sRilDataRegState = sServiceState.getRilDataRegState();
                sRilDataRadioTechnology = sServiceState.getRilDataRadioTechnology();
                logd("slotId: " + i + ", " + WorldPhoneUtil.iccCardTypeToString(sIccCardType[i]) + ", sMajorSim: " + sMajorSim + ", sPlmnSs: " + sPlmnSs + ", sVoiceRegState: " + WorldPhoneUtil.stateToString(sVoiceRegState));
                StringBuilder sb = new StringBuilder();
                sb.append("sRilVoiceRegState: ");
                sb.append(sRilVoiceRegState);
                sb.append(", sRilVoiceRadioTech: ");
                MtkServiceState mtkServiceState = sServiceState;
                sb.append(MtkServiceState.rilRadioTechnologyToString(sRilVoiceRadioTechnology));
                sb.append(", sDataRegState: ");
                sb.append(WorldPhoneUtil.stateToString(sDataRegState));
                logd(sb.toString());
                StringBuilder sb2 = new StringBuilder();
                sb2.append("sRilDataRegState: ");
                sb2.append(sRilDataRegState);
                sb2.append(", sRilDataRadioTech: , ");
                MtkServiceState mtkServiceState2 = sServiceState;
                sb2.append(MtkServiceState.rilRadioTechnologyToString(sRilDataRadioTechnology));
                sb2.append(", sIsAutoSelectEnable: ");
                sb2.append(sIsAutoSelectEnable);
                logd(sb2.toString());
                logd(ModemSwitchHandler.modemToString(ModemSwitchHandler.getActiveModemType()));
                if (sIsAutoSelectEnable && i == sMajorSim) {
                    if (isNoService() && sCi[i].getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                        handleNoService();
                        return;
                    } else {
                        if (isInService()) {
                            sLastPlmn = sPlmnSs;
                            removeModemStandByTimer();
                            sIsInvalidSim[i] = false;
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            logd("Null sServiceState");
            return;
        }
        logd("AsyncResult is wrong " + asyncResult.exception);
    }

    private void handleRegistrationSuspend(AsyncResult asyncResult, int i) {
        logd("Registration Suspend Slot" + i);
        if (ModemSwitchHandler.isModemTypeSwitching()) {
            return;
        }
        if (asyncResult.exception == null && asyncResult.result != null) {
            sSuspendId[i] = ((int[]) asyncResult.result)[0];
            logd("Suspending with Id=" + sSuspendId[i]);
            if (sIsAutoSelectEnable && sMajorSim == i) {
                if (sUserType != 0) {
                    resumeCampingProcedure(i, true);
                    return;
                }
                sSuspendWaitImsi[i] = true;
                if (!sWaitInEmsrResume) {
                    sWaitInEmsrResume = true;
                    logd("Wait EMSR:8s");
                    postDelayed(this.mEmsrResumeByTimerRunnable, 8000L);
                } else {
                    logd("Emsr Resume Timer already set:8s");
                }
                logd("User type unknown, wait for IMSI");
                return;
            }
            logd("Not major slot, camp on OK");
            sCi[i].setResumeRegistration(sSuspendId[i], null);
            return;
        }
        logd("AsyncResult is wrong " + asyncResult.exception);
    }

    private void handleInvalidSimNotify(int i, AsyncResult asyncResult) {
        logd("Slot" + i);
        if (asyncResult.exception == null && asyncResult.result != null) {
            String[] strArr = (String[]) asyncResult.result;
            String str = strArr[0];
            int i2 = Integer.parseInt(strArr[1]);
            int i3 = Integer.parseInt(strArr[2]);
            int i4 = Integer.parseInt(strArr[3]);
            int i5 = SystemProperties.getInt("vendor.gsm.gcf.testmode", 0);
            if (i5 != 0) {
                logd("Invalid SIM notified during test mode: " + i5);
                return;
            }
            logd("testMode:" + i5 + ", cause: " + i4 + ", cs_invalid: " + i2 + ", ps_invalid: " + i3 + ", plmn: " + str);
            if (sVoiceCapable && i2 == 1 && sLastPlmn == null) {
                logd("CS reject, invalid SIM");
                sIsInvalidSim[i] = true;
                return;
            } else {
                if (i3 == 1 && sLastPlmn == null) {
                    logd("PS reject, invalid SIM");
                    sIsInvalidSim[i] = true;
                    return;
                }
                return;
            }
        }
        logd("AsyncResult is wrong " + asyncResult.exception);
    }

    private void handleSwitchModem(int i) {
        int majorSim = WorldPhoneUtil.getMajorSim();
        if (sIsWaintInFddTimeOut != 0 || sIsWaintInTddTimeOut != 0) {
            sIsWaintInFddTimeOut = 0;
            sIsWaintInTddTimeOut = 0;
        } else if (isEccInProgress()) {
            logd("[handleSwitchModem]In ECC:" + this.mIsRegisterEccStateReceiver);
            if (this.mIsRegisterEccStateReceiver) {
                return;
            }
            registerEccStateReceiver();
            return;
        }
        if (sSimLocked == 1) {
            logd("sim has been locked!");
            return;
        }
        if (majorSim >= 0 && sIsInvalidSim[majorSim] && WorldPhoneUtil.getModemSelectionMode() == 1) {
            logd("Invalid SIM, switch not executed!");
            return;
        }
        if (sIsAutoSelectEnable && !isNeedSwitchModem()) {
            logd("[handleSwitchModem]No need to handle, switch not executed!");
            return;
        }
        if (i == 101) {
            i = WorldPhoneUtil.isLteSupport() ? 6 : 4;
        } else if (i == 100) {
            i = WorldPhoneUtil.isLteSupport() ? 5 : 3;
        }
        if (i == ModemSwitchHandler.getActiveModemType()) {
            if (i == 3) {
                logd("Already in WG modem");
                return;
            }
            if (i == 4) {
                logd("Already in TG modem");
                return;
            } else if (i == 5) {
                logd("Already in FDD CSFB modem");
                return;
            } else {
                if (i == 6) {
                    logd("Already in TDD CSFB modem");
                    return;
                }
                return;
            }
        }
        if (!sIsAutoSelectEnable) {
            logd("Storing modem type: " + i);
            sCi[0].storeModemType(i, null);
        } else if (sDefaultBootuUpModem == 0) {
            logd("Storing modem type: " + i);
            sCi[0].storeModemType(i, null);
        } else if (sDefaultBootuUpModem == 100) {
            if (WorldPhoneUtil.isLteSupport()) {
                logd("Storing modem type: 5");
                sCi[0].storeModemType(5, null);
            } else {
                logd("Storing modem type: 3");
                sCi[0].storeModemType(3, null);
            }
        } else if (sDefaultBootuUpModem == 101) {
            if (WorldPhoneUtil.isLteSupport()) {
                logd("Storing modem type: 6");
                sCi[0].storeModemType(6, null);
            } else {
                logd("Storing modem type: 4");
                sCi[0].storeModemType(4, null);
            }
        }
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            if (sActivePhones[i2].getState() != PhoneConstants.State.IDLE) {
                logd("Phone" + i2 + " is not idle, modem switch not allowed");
                return;
            }
        }
        removeModemStandByTimer();
        if (i == 3) {
            logd("Switching to WG modem");
        } else if (i == 4) {
            logd("Switching to TG modem");
        } else if (i == 5) {
            logd("Switching to FDD CSFB modem");
        } else if (i == 6) {
            logd("Switching to TDD CSFB modem");
        }
        if (WorldPhoneUtil.isSimSwitching() && i == WorldPhoneUtil.getToModemType()) {
            logd("sim switching, already will to set modem:" + i);
        } else {
            ModemSwitchHandler.reloadModemCauseType(sCi[0], sSwitchModemCauseType);
            ModemSwitchHandler.switchModem(0, i);
            resetNetworkProperties();
        }
    }

    private void handleSimSwitched() {
        if (sMajorSim == -1) {
            logd("Major capability turned off");
            removeModemStandByTimer();
            sUserType = 0;
            return;
        }
        if (!sIsAutoSelectEnable) {
            logd("Auto modem selection disabled");
            removeModemStandByTimer();
            return;
        }
        if (sMajorSim == -99) {
            logd("Major SIM unknown");
            return;
        }
        logd("Auto modem selection enabled");
        logd("Major capability in slot" + sMajorSim);
        if (sImsi[sMajorSim] == null || sImsi[sMajorSim].equals("")) {
            logd("Major slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sSwitchModemCauseType = 255;
        logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        sUserType = getUserType(sImsi[sMajorSim]);
        if (sUserType == 1 || sUserType == 2) {
            if (sNwPlmnStrings != null) {
                sRegion = getRegion(sNwPlmnStrings[0]);
            }
            if (sRegion == 1) {
                sFirstSelect[sMajorSim] = false;
                sIccCardType[sMajorSim] = getIccCardType(sMajorSim);
                handleSwitchModem(101);
                return;
            } else if (sRegion == 2) {
                sFirstSelect[sMajorSim] = false;
                handleSwitchModem(100);
                return;
            } else {
                logd("Unknown region");
                return;
            }
        }
        if (sUserType == 3) {
            sFirstSelect[sMajorSim] = false;
            handleSwitchModem(100);
        } else {
            logd("Unknown user type");
        }
    }

    private void handleNoService() {
        logd("[handleNoService]+ Can not find service");
        logd(PplSmsFilterExtension.INSTRUCTION_KEY_TYPE + sUserType + " user");
        logd(WorldPhoneUtil.regionToString(sRegion));
        int activeModemType = ModemSwitchHandler.getActiveModemType();
        logd(ModemSwitchHandler.modemToString(activeModemType));
        if (sProxyPhones[sMajorSim].getIccCard().getState() == IccCardConstants.State.READY) {
            if (sUserType == 1 || sUserType == 2) {
                if (activeModemType == 6 || activeModemType == 4) {
                    if (TDD_STANDBY_TIMER[sTddStandByCounter] >= 0) {
                        if (!sWaitInTdd) {
                            sWaitInTdd = true;
                            logd("Wait " + TDD_STANDBY_TIMER[sTddStandByCounter] + "s. Timer index = " + sTddStandByCounter);
                            postDelayed(this.mTddStandByTimerRunnable, (long) (TDD_STANDBY_TIMER[sTddStandByCounter] * 1000));
                        } else {
                            logd("Timer already set:" + TDD_STANDBY_TIMER[sTddStandByCounter] + "s");
                        }
                    } else {
                        logd("Standby in TDD modem");
                    }
                } else if (activeModemType == 5 || activeModemType == 3) {
                    if (FDD_STANDBY_TIMER[sFddStandByCounter] >= 0) {
                        if (sRegion == 2) {
                            if (!sWaitInFdd) {
                                sWaitInFdd = true;
                                logd("Wait " + FDD_STANDBY_TIMER[sFddStandByCounter] + "s. Timer index = " + sFddStandByCounter);
                                postDelayed(this.mFddStandByTimerRunnable, (long) (FDD_STANDBY_TIMER[sFddStandByCounter] * 1000));
                            } else {
                                logd("Timer already set:" + FDD_STANDBY_TIMER[sFddStandByCounter] + "s");
                            }
                        } else {
                            sSwitchModemCauseType = 1;
                            logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
                            handleSwitchModem(101);
                        }
                    } else {
                        logd("Standby in FDD modem");
                    }
                }
            } else if (sUserType == 3) {
                if (activeModemType == 5 || activeModemType == 3) {
                    logd("Standby in FDD modem");
                } else {
                    logd("Should not enter this state");
                }
            } else {
                logd("Unknow user type");
            }
        } else {
            logd("IccState not ready");
        }
        logd("[handleNoService]-");
    }

    private boolean isAllowCampOn(String str, int i) {
        int activeModemType;
        logd("[isAllowCampOn]+ " + str);
        logd("User type: " + sUserType);
        logd(WorldPhoneUtil.iccCardTypeToString(sIccCardType[i]));
        sRegion = getRegion(str);
        if (WorldPhoneUtil.isSimSwitching()) {
            activeModemType = WorldPhoneUtil.getToModemType();
            logd("SimSwitching mdType:" + ModemSwitchHandler.modemToString(activeModemType));
        } else {
            activeModemType = ModemSwitchHandler.getActiveModemType();
            logd("mdType:" + ModemSwitchHandler.modemToString(activeModemType));
        }
        if (sUserType == 1 || sUserType == 2) {
            if (sRegion == 1) {
                if (activeModemType == 6 || activeModemType == 4) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    logd("[isAllowCampOn]-");
                    return true;
                }
                if (activeModemType == 5 || activeModemType == 3) {
                    sDenyReason = 3;
                    logd("Camp on REJECT");
                    logd("[isAllowCampOn]-");
                    return false;
                }
            } else if (sRegion == 2) {
                if (activeModemType == 6 || activeModemType == 4) {
                    sDenyReason = 2;
                    logd("Camp on REJECT");
                    logd("[isAllowCampOn]-");
                    return false;
                }
                if (activeModemType == 5 || activeModemType == 3) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    logd("[isAllowCampOn]-");
                    return true;
                }
            } else {
                logd("Unknow region");
            }
        } else if (sUserType == 3) {
            if (activeModemType == 6 || activeModemType == 4) {
                sDenyReason = 2;
                logd("Camp on REJECT");
                logd("[isAllowCampOn]-");
                return false;
            }
            if (activeModemType == 5 || activeModemType == 3) {
                sDenyReason = 0;
                logd("Camp on OK");
                logd("[isAllowCampOn]-");
                return true;
            }
        } else {
            logd("Unknown user type");
        }
        sDenyReason = 1;
        logd("Camp on REJECT");
        logd("[isAllowCampOn]-");
        return false;
    }

    private boolean isInService() {
        boolean z;
        if (sVoiceRegState == 0 || sDataRegState == 0) {
            z = true;
        } else {
            z = false;
        }
        logd("inService: " + z);
        return z;
    }

    private boolean isNoService() {
        boolean z = sVoiceRegState == 1 && (sRilVoiceRegState == 0 || sRilVoiceRegState == 10) && sDataRegState == 1 && sRilDataRegState == 0;
        logd("noService: " + z);
        return z;
    }

    private int getIccCardType(int i) {
        String iccCardType = sProxyPhones[i].getIccCard().getIccCardType();
        if (iccCardType.equals("SIM")) {
            logd("IccCard type: SIM");
            return 1;
        }
        if (iccCardType.equals("USIM")) {
            logd("IccCard type: USIM");
            return 2;
        }
        logd("IccCard type: Unknown");
        return 0;
    }

    private int getRegion(String str) {
        if (str == null || str.equals("") || str.length() < 3) {
            logd("[getRegion] Invalid PLMN");
            return 0;
        }
        String strSubstring = str.length() >= 5 ? str.substring(0, 5) : null;
        if (strSubstring != null && (strSubstring.equals("46602") || strSubstring.equals("50270"))) {
            return 1;
        }
        String strSubstring2 = str.substring(0, 3);
        for (String str2 : MCC_TABLE_DOMESTIC) {
            if (strSubstring2.equals(str2)) {
                logd("[getRegion] REGION_DOMESTIC");
                return 1;
            }
        }
        logd("[getRegion] REGION_FOREIGN");
        return 2;
    }

    private int getUserType(String str) {
        if (str != null && !str.equals("")) {
            String strSubstring = str.substring(0, 5);
            for (String str2 : PLMN_TABLE_TYPE1) {
                if (strSubstring.equals(str2)) {
                    logd("[getUserType] Type1 user");
                    return 1;
                }
            }
            for (String str3 : PLMN_TABLE_TYPE3) {
                if (strSubstring.equals(str3)) {
                    logd("[getUserType] Type3 user");
                    return 3;
                }
            }
            logd("[getUserType] Type2 user");
            return 2;
        }
        logd("[getUserType] null IMSI");
        return 0;
    }

    private void resumeCampingProcedure(int i, boolean z) {
        logd("Resume camping slot" + i);
        if (isAllowCampOn(sNwPlmnStrings[0], i) || !isNeedSwitchModem()) {
            removeModemStandByTimer();
            removeEmsrResumeByTimer();
            if (z) {
                sCi[i].setResumeRegistration(sSuspendId[i], obtainMessage(70 + i));
                return;
            }
            return;
        }
        logd("Because: " + WorldPhoneUtil.denyReasonToString(sDenyReason));
        sSwitchModemCauseType = 0;
        logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        if (sDenyReason == 2) {
            handleSwitchModem(100);
        } else if (sDenyReason == 3) {
            handleSwitchModem(101);
        }
    }

    private void removeModemStandByTimer() {
        if (sWaitInTdd) {
            logd("Remove TDD wait timer. Set sWaitInTdd = false");
            sWaitInTdd = false;
            removeCallbacks(this.mTddStandByTimerRunnable);
        }
        if (sWaitInFdd) {
            logd("Remove FDD wait timer. Set sWaitInFdd = false");
            sWaitInFdd = false;
            removeCallbacks(this.mFddStandByTimerRunnable);
        }
    }

    private void removeEmsrResumeByTimer() {
        if (sWaitInEmsrResume) {
            logd("Remove EMSR wait timer. Set sWaitInEmsrResume = false");
            sWaitInEmsrResume = false;
            removeCallbacks(this.mEmsrResumeByTimerRunnable);
        }
    }

    private void resetAllProperties() {
        logd("[resetAllProperties]");
        sNwPlmnStrings = null;
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sFirstSelect[i] = true;
        }
        sDenyReason = 1;
        resetSimProperties();
        resetNetworkProperties();
        sSimLocked = 0;
    }

    private void resetNetworkProperties() {
        logd("[resetNetworkProperties]");
        synchronized (sLock) {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                sSuspendWaitImsi[i] = false;
            }
            if (sNwPlmnStrings != null) {
                for (int i2 = 0; i2 < sNwPlmnStrings.length; i2++) {
                    sNwPlmnStrings[i2] = "";
                }
            }
            removeEmsrResumeByTimer();
            sSwitchModemCauseType = 255;
            logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        }
    }

    private void resetSimProperties() {
        logd("[resetSimProperties]");
        synchronized (sLock) {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                sImsi[i] = "";
                sIccCardType[i] = 0;
            }
            sUserType = 0;
            sMajorSim = WorldPhoneUtil.getMajorSim();
        }
    }

    private void searchForDesignateService(String str) {
        if (str == null) {
            logd("[searchForDesignateService]- null source");
            return;
        }
        String strSubstring = str.substring(0, 5);
        for (String str2 : PLMN_TABLE_TYPE1) {
            if (strSubstring.equals(str2)) {
                logd("Find TD service");
                logd("sUserType: " + sUserType + " sRegion: " + sRegion);
                logd(ModemSwitchHandler.modemToString(ModemSwitchHandler.getActiveModemType()));
                sSwitchModemCauseType = 0;
                logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
                handleSwitchModem(101);
                return;
            }
        }
    }

    @Override
    public void setModemSelectionMode(int i, int i2) {
        SystemProperties.set(IWorldPhone.WORLD_PHONE_AUTO_SELECT_MODE, Integer.toString(i));
        if (i == 1) {
            logd("Modem Selection <AUTO>");
            sIsAutoSelectEnable = true;
            sMajorSim = WorldPhoneUtil.getMajorSim();
            handleSimSwitched();
            return;
        }
        logd("Modem Selection <MANUAL>");
        sIsAutoSelectEnable = false;
        sSwitchModemCauseType = 255;
        logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        handleSwitchModem(i2);
        if (i2 == ModemSwitchHandler.getActiveModemType()) {
            removeModemStandByTimer();
        }
    }

    @Override
    public void notifyRadioCapabilityChange(int i) {
        int i2;
        logd("[setRadioCapabilityChange]");
        logd("Major capability will be set to slot:" + i);
        removeEmsrResumeByTimer();
        if (!sIsAutoSelectEnable) {
            logd("Auto modem selection disabled");
            removeModemStandByTimer();
            return;
        }
        logd("Auto modem selection enabled");
        if (sImsi[i] == null || sImsi[i].equals("")) {
            logd("Capaility slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sUserType = getUserType(sImsi[i]);
        int i3 = 3;
        if (sUserType == 1 || sUserType == 2) {
            if (sNwPlmnStrings != null) {
                sRegion = getRegion(sNwPlmnStrings[0]);
            }
            if (sRegion == 1) {
                sFirstSelect[i] = false;
                sIccCardType[i] = getIccCardType(i);
                i2 = 101;
                if (i2 != 101) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        i3 = 6;
                    } else {
                        i3 = 4;
                    }
                } else if (i2 == 100) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        i3 = 5;
                    }
                } else {
                    i3 = i2;
                }
                logd("notifyRadioCapabilityChange: Storing modem type: " + i3);
                if (isNeedReloadModem(i)) {
                    return;
                }
                sCi[0].reloadModemType(i3, null);
                resetNetworkProperties();
                WorldPhoneUtil.setSimSwitchingFlag(true);
                WorldPhoneUtil.saveToModemType(i3);
                return;
            }
            if (sRegion == 2) {
                sFirstSelect[i] = false;
            } else {
                logd("Unknown region");
                return;
            }
        } else if (sUserType == 3) {
            sFirstSelect[i] = false;
        } else {
            logd("Unknown user type");
            return;
        }
        i2 = 100;
        if (i2 != 101) {
        }
        logd("notifyRadioCapabilityChange: Storing modem type: " + i3);
        if (isNeedReloadModem(i)) {
        }
    }

    private boolean isNeedSwitchModem() {
        boolean z;
        int majorSim = WorldPhoneUtil.getMajorSim();
        if (WorldPhoneUtil.isC2kSupport()) {
            int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
            if (sUserType == 2 && (((majorSim >= 0 && majorSim == activeSvlteModeSlotId) || isCdmaCard(majorSim)) && ModemSwitchHandler.getActiveModemType() == 5)) {
                z = false;
            } else {
                z = true;
            }
        }
        logd("[isNeedSwitchModem] isNeed = " + z);
        return z;
    }

    private boolean isNeedReloadModem(int i) {
        boolean z;
        if (WorldPhoneUtil.isC2kSupport()) {
            int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
            logd("[isNeedReloadModem] activeSvlteModeSlotId = " + activeSvlteModeSlotId + ", sUserType = " + sUserType + ", capabilitySimId = " + i);
            if (sUserType == 2 && (((i >= 0 && i == activeSvlteModeSlotId) || isCdmaCard(i)) && ModemSwitchHandler.getActiveModemType() == 5)) {
                z = false;
            } else {
                z = true;
            }
        }
        logd("[isNeedReloadModem] isNeed = " + z);
        return z;
    }

    private boolean isCdmaCard(int i) {
        if (!SubscriptionManager.isValidPhoneId(i)) {
            return false;
        }
        int[] c2KWPCardType = WorldPhoneUtil.getC2KWPCardType();
        logd("isCdmaCard(), cardType=" + c2KWPCardType[i]);
        boolean z = (c2KWPCardType[i] & 4) > 0 || (c2KWPCardType[i] & 8) > 0 || isCt3gDualMode(i);
        logd("isCdmaCard(), slotId=" + i + " retCdmaCard=" + z);
        return z;
    }

    private boolean isCt3gDualMode(int i) {
        if (i < 0 || i >= PROPERTY_RIL_CT3G.length) {
            logd("isCt3gDualMode: invalid slotId " + i);
            return false;
        }
        String str = SystemProperties.get(PROPERTY_RIL_CT3G[i], "");
        logd("isCt3gDualMode: " + str);
        return "1".equals(str);
    }

    private void registerEccStateReceiver() {
        if (sContext == null) {
            logd("registerEccStateReceiver, context is null => return");
            return;
        }
        IntentFilter intentFilter = new IntentFilter("android.intent.action.ECC_IN_PROGRESS");
        intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        sContext.registerReceiver(this.mWorldPhoneEccStateReceiver, intentFilter);
        this.mIsRegisterEccStateReceiver = true;
    }

    private void unRegisterEccStateReceiver() {
        if (sContext == null) {
            logd("unRegisterEccStateReceiver, context is null => return");
        } else {
            sContext.unregisterReceiver(this.mWorldPhoneEccStateReceiver);
            this.mIsRegisterEccStateReceiver = false;
        }
    }

    private boolean isEccInProgress() {
        boolean zIsEccInProgress;
        String str = SystemProperties.get("ril.cdma.inecmmode", "");
        boolean zContains = str.contains("true");
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface != null) {
            try {
                zIsEccInProgress = iMtkTelephonyExAsInterface.isEccInProgress();
            } catch (RemoteException e) {
                logd("Exception of isEccInProgress");
                zIsEccInProgress = false;
            }
        } else {
            zIsEccInProgress = false;
        }
        logd("isEccInProgress, value:" + str + ", inEcm:" + zContains + ", isInEcc:" + zIsEccInProgress);
        return zContains || zIsEccInProgress;
    }

    private static void logd(String str) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPOP01]" + str);
    }
}
