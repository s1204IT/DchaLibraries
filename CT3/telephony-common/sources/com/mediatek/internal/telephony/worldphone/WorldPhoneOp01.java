package com.mediatek.internal.telephony.worldphone;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.ModemSwitchHandler;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.ppl.PplSmsFilterExtension;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;

public class WorldPhoneOp01 extends Handler implements IWorldPhone {
    private static int sBtSapState;
    private static int sDataRegState;
    private static int sDenyReason;
    private static int sFddStandByCounter;
    private static boolean sIsAutoSelectEnable;
    private static boolean sIsResumeCampingFail1;
    private static boolean sIsResumeCampingFail2;
    private static boolean sIsResumeCampingFail3;
    private static boolean sIsResumeCampingFail4;
    private static String sLastPlmn;
    private static int sMajorSim;
    private static String[] sNwPlmnStrings;
    private static String sOperatorSpec;
    private static String sPlmnSs;
    private static int sRegion;
    private static int sRilDataRadioTechnology;
    private static int sRilDataRegState;
    private static int sRilVoiceRadioTechnology;
    private static int sRilVoiceRegState;
    private static ServiceState sServiceState;
    private static int sSwitchModemCauseType;
    private static int sTddStandByCounter;
    private static int sUserType;
    private static boolean sVoiceCapable;
    private static int sVoiceRegState;
    private static boolean sWaitInFdd;
    private static boolean sWaitInTdd;
    private static Object sLock = new Object();
    private static final int PROJECT_SIM_NUM = WorldPhoneUtil.getProjectSimNum();
    private static final int[] FDD_STANDBY_TIMER = {60};
    private static final int[] TDD_STANDBY_TIMER = {40};
    private static final String[] PLMN_TABLE_TYPE1 = {"46000", "46002", "46004", "46007", "46008", "00101", "00211", "00321", "00431", "00541", "00651", "00761", "00871", "00902", "01012", "01122", "01232", "46602", "50270"};
    private static final String[] PLMN_TABLE_TYPE3 = {"46001", "46006", "46009", "45407", "46003", "46005", "45502", "46011"};
    private static final String[] MCC_TABLE_DOMESTIC = {RadioCapabilitySwitchUtil.CN_MCC, "001", "002", "003", "004", "005", "006", "007", "008", "009", "010", "011", "012"};
    private static final String[] PROPERTY_RIL_CT3G = {"gsm.ril.ct3g", "gsm.ril.ct3g.2", "gsm.ril.ct3g.3", "gsm.ril.ct3g.4"};
    private static Context sContext = null;
    private static Phone sDefultPhone = null;
    private static Phone[] sProxyPhones = null;
    private static Phone[] sActivePhones = new Phone[PROJECT_SIM_NUM];
    private static CommandsInterface[] sCi = new CommandsInterface[PROJECT_SIM_NUM];
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
    private final BroadcastReceiver mWorldPhoneReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            WorldPhoneOp01.logd("Action: " + action);
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                String simStatus = intent.getStringExtra("ss");
                int slotId = intent.getIntExtra("slot", 0);
                int unused = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                WorldPhoneOp01.logd("slotId: " + slotId + " simStatus: " + simStatus + " sMajorSim: " + WorldPhoneOp01.sMajorSim);
                if ("IMSI".equals(simStatus)) {
                    if (WorldPhoneOp01.sMajorSim == -99) {
                        int unused2 = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                    }
                    UiccController unused3 = WorldPhoneOp01.sUiccController = UiccController.getInstance();
                    if (WorldPhoneOp01.sUiccController == null) {
                        WorldPhoneOp01.logd("Null sUiccController");
                        return;
                    }
                    WorldPhoneOp01.sIccRecordsInstance[slotId] = WorldPhoneOp01.sProxyPhones[slotId].getIccCard().getIccRecords();
                    if (WorldPhoneOp01.sIccRecordsInstance[slotId] == null) {
                        WorldPhoneOp01.logd("Null sIccRecordsInstance");
                        return;
                    }
                    WorldPhoneOp01.sImsi[slotId] = WorldPhoneOp01.sIccRecordsInstance[slotId].getIMSI();
                    WorldPhoneOp01.sIccCardType[slotId] = WorldPhoneOp01.this.getIccCardType(slotId);
                    WorldPhoneOp01.logd("sImsi[" + slotId + "]:" + WorldPhoneOp01.sImsi[slotId]);
                    if (WorldPhoneOp01.sIsAutoSelectEnable && slotId == WorldPhoneOp01.sMajorSim) {
                        WorldPhoneOp01.logd("Major SIM");
                        int unused4 = WorldPhoneOp01.sUserType = WorldPhoneOp01.this.getUserType(WorldPhoneOp01.sImsi[slotId]);
                        if (WorldPhoneOp01.sFirstSelect[slotId]) {
                            WorldPhoneOp01.sFirstSelect[slotId] = false;
                            if (WorldPhoneOp01.sUserType == 1 || WorldPhoneOp01.sUserType == 2) {
                                int unused5 = WorldPhoneOp01.sSwitchModemCauseType = 0;
                                WorldPhoneOp01.logd("sSwitchModemCauseType = " + WorldPhoneOp01.sSwitchModemCauseType);
                                if (WorldPhoneOp01.sRegion == 1) {
                                    WorldPhoneOp01.this.handleSwitchModem(101);
                                } else if (WorldPhoneOp01.sRegion == 2) {
                                    WorldPhoneOp01.this.handleSwitchModem(100);
                                } else {
                                    WorldPhoneOp01.logd("Region unknown");
                                }
                            } else if (WorldPhoneOp01.sUserType == 3) {
                                int unused6 = WorldPhoneOp01.sSwitchModemCauseType = 255;
                                WorldPhoneOp01.logd("sSwitchModemCauseType = " + WorldPhoneOp01.sSwitchModemCauseType);
                                WorldPhoneOp01.this.handleSwitchModem(100);
                            }
                        }
                        if (WorldPhoneOp01.sSuspendWaitImsi[slotId]) {
                            WorldPhoneOp01.sSuspendWaitImsi[slotId] = false;
                            if (WorldPhoneOp01.sNwPlmnStrings != null) {
                                WorldPhoneOp01.logd("IMSI fot slot" + slotId + " now ready, resuming PLMN:" + WorldPhoneOp01.sNwPlmnStrings[0] + " with ID:" + WorldPhoneOp01.sSuspendId[slotId]);
                                WorldPhoneOp01.this.resumeCampingProcedure(slotId);
                            } else {
                                WorldPhoneOp01.logd("sNwPlmnStrings is Null");
                            }
                        }
                    } else {
                        WorldPhoneOp01.logd("Not major SIM");
                        WorldPhoneOp01.this.getUserType(WorldPhoneOp01.sImsi[slotId]);
                        if (WorldPhoneOp01.sSuspendWaitImsi[slotId]) {
                            WorldPhoneOp01.sSuspendWaitImsi[slotId] = false;
                            WorldPhoneOp01.logd("IMSI fot slot" + slotId + " now ready, resuming with ID:" + WorldPhoneOp01.sSuspendId[slotId]);
                            WorldPhoneOp01.sCi[slotId].setResumeRegistration(WorldPhoneOp01.sSuspendId[slotId], null);
                        }
                    }
                } else if (simStatus.equals("ABSENT")) {
                    String unused7 = WorldPhoneOp01.sLastPlmn = null;
                    WorldPhoneOp01.sImsi[slotId] = UsimPBMemInfo.STRING_NOT_SET;
                    WorldPhoneOp01.sFirstSelect[slotId] = true;
                    WorldPhoneOp01.sIsInvalidSim[slotId] = false;
                    WorldPhoneOp01.sSuspendWaitImsi[slotId] = false;
                    WorldPhoneOp01.sIccCardType[slotId] = 0;
                    if (slotId == WorldPhoneOp01.sMajorSim) {
                        WorldPhoneOp01.logd("Major SIM removed, no world phone service");
                        WorldPhoneOp01.this.removeModemStandByTimer();
                        int unused8 = WorldPhoneOp01.sUserType = 0;
                        int unused9 = WorldPhoneOp01.sDenyReason = 1;
                        int unused10 = WorldPhoneOp01.sMajorSim = -99;
                        int unused11 = WorldPhoneOp01.sRegion = 0;
                    } else {
                        WorldPhoneOp01.logd("SIM" + slotId + " is not major SIM");
                    }
                }
            } else if (action.equals("android.intent.action.SERVICE_STATE")) {
                if (intent.getExtras() != null) {
                    ServiceState unused12 = WorldPhoneOp01.sServiceState = ServiceState.newFromBundle(intent.getExtras());
                    if (WorldPhoneOp01.sServiceState != null) {
                        int slotId2 = intent.getIntExtra("slot", 0);
                        String unused13 = WorldPhoneOp01.sPlmnSs = WorldPhoneOp01.sServiceState.getOperatorNumeric();
                        int unused14 = WorldPhoneOp01.sVoiceRegState = WorldPhoneOp01.sServiceState.getVoiceRegState();
                        int unused15 = WorldPhoneOp01.sRilVoiceRegState = WorldPhoneOp01.sServiceState.getRilVoiceRegState();
                        int unused16 = WorldPhoneOp01.sRilVoiceRadioTechnology = WorldPhoneOp01.sServiceState.getRilVoiceRadioTechnology();
                        int unused17 = WorldPhoneOp01.sDataRegState = WorldPhoneOp01.sServiceState.getDataRegState();
                        int unused18 = WorldPhoneOp01.sRilDataRegState = WorldPhoneOp01.sServiceState.getRilDataRegState();
                        int unused19 = WorldPhoneOp01.sRilDataRadioTechnology = WorldPhoneOp01.sServiceState.getRilDataRadioTechnology();
                        WorldPhoneOp01.logd("slotId: " + slotId2 + ", " + WorldPhoneUtil.iccCardTypeToString(WorldPhoneOp01.sIccCardType[slotId2]) + ", sMajorSim: " + WorldPhoneOp01.sMajorSim + ", sPlmnSs: " + WorldPhoneOp01.sPlmnSs + ", sVoiceRegState: " + WorldPhoneUtil.stateToString(WorldPhoneOp01.sVoiceRegState));
                        StringBuilder sbAppend = new StringBuilder().append("sRilVoiceRegState: ").append(WorldPhoneUtil.regStateToString(WorldPhoneOp01.sRilVoiceRegState)).append(", ").append("sRilVoiceRadioTech: ");
                        ServiceState unused20 = WorldPhoneOp01.sServiceState;
                        WorldPhoneOp01.logd(sbAppend.append(ServiceState.rilRadioTechnologyToString(WorldPhoneOp01.sRilVoiceRadioTechnology)).append(", ").append("sDataRegState: ").append(WorldPhoneUtil.stateToString(WorldPhoneOp01.sDataRegState)).toString());
                        StringBuilder sbAppend2 = new StringBuilder().append("sRilDataRegState: ").append(WorldPhoneUtil.regStateToString(WorldPhoneOp01.sRilDataRegState)).append(", ").append("sRilDataRadioTech: ").append(", ");
                        ServiceState unused21 = WorldPhoneOp01.sServiceState;
                        WorldPhoneOp01.logd(sbAppend2.append(ServiceState.rilRadioTechnologyToString(WorldPhoneOp01.sRilDataRadioTechnology)).append(", ").append("sIsAutoSelectEnable: ").append(WorldPhoneOp01.sIsAutoSelectEnable).toString());
                        WorldPhoneOp01.logd(ModemSwitchHandler.modemToString(ModemSwitchHandler.getActiveModemType()));
                        if (WorldPhoneOp01.sIsAutoSelectEnable && slotId2 == WorldPhoneOp01.sMajorSim) {
                            if (WorldPhoneOp01.this.isNoService()) {
                                WorldPhoneOp01.this.handleNoService();
                            } else if (WorldPhoneOp01.this.isInService()) {
                                String unused22 = WorldPhoneOp01.sLastPlmn = WorldPhoneOp01.sPlmnSs;
                                WorldPhoneOp01.this.removeModemStandByTimer();
                                WorldPhoneOp01.sIsInvalidSim[slotId2] = false;
                            }
                        }
                    } else {
                        WorldPhoneOp01.logd("Null sServiceState");
                    }
                }
            } else if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                if (WorldPhoneOp01.sDefaultBootuUpModem == 100) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 5);
                        WorldPhoneOp01.logd("Reload to FDD CSFB modem");
                    } else {
                        ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 3);
                        WorldPhoneOp01.logd("Reload to WG modem");
                    }
                } else if (WorldPhoneOp01.sDefaultBootuUpModem == 101) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 6);
                        WorldPhoneOp01.logd("Reload to TDD CSFB modem");
                    } else {
                        ModemSwitchHandler.reloadModem(WorldPhoneOp01.sCi[0], 4);
                        WorldPhoneOp01.logd("Reload to TG modem");
                    }
                }
            } else if (action.equals(IWorldPhone.ACTION_ADB_SWITCH_MODEM)) {
                int toModem = intent.getIntExtra("mdType", 0);
                WorldPhoneOp01.logd("toModem: " + toModem);
                if (toModem == 3 || toModem == 4 || toModem == 5 || toModem == 6) {
                    WorldPhoneOp01.this.setModemSelectionMode(0, toModem);
                } else {
                    WorldPhoneOp01.this.setModemSelectionMode(1, toModem);
                }
            } else if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    WorldPhoneOp01.logd("Enter flight mode");
                    for (int i = 0; i < WorldPhoneOp01.PROJECT_SIM_NUM; i++) {
                        WorldPhoneOp01.sFirstSelect[i] = true;
                    }
                    int unused23 = WorldPhoneOp01.sRegion = 0;
                } else {
                    WorldPhoneOp01.logd("Leave flight mode");
                    String unused24 = WorldPhoneOp01.sLastPlmn = null;
                    for (int i2 = 0; i2 < WorldPhoneOp01.PROJECT_SIM_NUM; i2++) {
                        WorldPhoneOp01.sIsInvalidSim[i2] = false;
                    }
                }
            } else if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                int unused25 = WorldPhoneOp01.sMajorSim = WorldPhoneUtil.getMajorSim();
                if (WorldPhoneUtil.isSimSwitching()) {
                    WorldPhoneUtil.setSimSwitchingFlag(false);
                    ModemSwitchHandler.setActiveModemType(WorldPhoneUtil.getToModemType());
                }
                WorldPhoneOp01.this.handleSimSwitched();
            } else if (action.equals(IWorldPhone.ACTION_SAP_CONNECTION_STATE_CHANGED)) {
                int sapState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                if (sapState == 2) {
                    WorldPhoneOp01.logd("BT_SAP connection state is CONNECTED");
                    int unused26 = WorldPhoneOp01.sBtSapState = 1;
                } else if (sapState == 0) {
                    WorldPhoneOp01.logd("BT_SAP connection state is DISCONNECTED");
                    int unused27 = WorldPhoneOp01.sBtSapState = 0;
                } else {
                    WorldPhoneOp01.logd("BT_SAP connection state is " + sapState);
                }
            }
            WorldPhoneOp01.logd("Action: " + action + " handle end");
        }
    };
    private Runnable mTddStandByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            WorldPhoneOp01.sTddStandByCounter++;
            if (WorldPhoneOp01.sTddStandByCounter >= WorldPhoneOp01.TDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOp01.sTddStandByCounter = WorldPhoneOp01.TDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOp01.sBtSapState == 0) {
                WorldPhoneOp01.logd("TDD time out!");
                int unused2 = WorldPhoneOp01.sSwitchModemCauseType = 1;
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
            WorldPhoneOp01.sFddStandByCounter++;
            if (WorldPhoneOp01.sFddStandByCounter >= WorldPhoneOp01.FDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOp01.sFddStandByCounter = WorldPhoneOp01.FDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOp01.sBtSapState == 0) {
                WorldPhoneOp01.logd("FDD time out!");
                int unused2 = WorldPhoneOp01.sSwitchModemCauseType = 1;
                WorldPhoneOp01.logd("sSwitchModemCauseType = " + WorldPhoneOp01.sSwitchModemCauseType);
                WorldPhoneOp01.this.handleSwitchModem(101);
                return;
            }
            WorldPhoneOp01.logd("FDD time out but BT SAP is connected, switch not executed!");
        }
    };

    public WorldPhoneOp01() {
        logd("Constructor invoked");
        sOperatorSpec = SystemProperties.get("persist.operator.optr", IWorldPhone.NO_OP);
        logd("Operator Spec:" + sOperatorSpec);
        sDefultPhone = PhoneFactory.getDefaultPhone();
        sProxyPhones = PhoneFactory.getPhones();
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            sActivePhones[i] = sProxyPhones[i];
            sCi[i] = sActivePhones[i].mCi;
        }
        for (int i2 = 0; i2 < PROJECT_SIM_NUM; i2++) {
            sCi[i2].setOnPlmnChangeNotification(this, i2 + 10, null);
            sCi[i2].setOnRegistrationSuspended(this, i2 + 30, null);
            sCi[i2].registerForOn(this, i2 + 0, null);
            sCi[i2].setInvalidSimInfo(this, i2 + 60, null);
            if (WorldPhoneUtil.isC2kSupport()) {
                sCi[i2].registerForGmssRatChanged(this, i2 + IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_1, null);
            }
        }
        sModemSwitchHandler = new ModemSwitchHandler();
        logd(ModemSwitchHandler.modemToString(ModemSwitchHandler.getActiveModemType()));
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
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
        sVoiceCapable = sContext.getResources().getBoolean(R.^attr-private.frameDuration);
        sContext.registerReceiver(this.mWorldPhoneReceiver, intentFilter);
        sTddStandByCounter = 0;
        sFddStandByCounter = 0;
        sWaitInTdd = false;
        sWaitInFdd = false;
        sRegion = 0;
        sLastPlmn = null;
        sBtSapState = 0;
        resetAllProperties();
        if (WorldPhoneUtil.getModemSelectionMode() == 0) {
            logd("Auto select disable");
            sIsAutoSelectEnable = false;
            Settings.Global.putInt(sContext.getContentResolver(), "world_phone_auto_select_mode", 0);
        } else {
            logd("Auto select enable");
            sIsAutoSelectEnable = true;
            Settings.Global.putInt(sContext.getContentResolver(), "world_phone_auto_select_mode", 1);
        }
        FDD_STANDBY_TIMER[sFddStandByCounter] = Settings.Global.getInt(sContext.getContentResolver(), "world_phone_fdd_modem_timer", FDD_STANDBY_TIMER[sFddStandByCounter]);
        Settings.Global.putInt(sContext.getContentResolver(), "world_phone_fdd_modem_timer", FDD_STANDBY_TIMER[sFddStandByCounter]);
        logd("FDD_STANDBY_TIMER = " + FDD_STANDBY_TIMER[sFddStandByCounter] + "s");
        logd("sDefaultBootuUpModem = " + sDefaultBootuUpModem);
    }

    @Override
    public void handleMessage(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        switch (msg.what) {
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
            case 10:
                logd("handleMessage : <EVENT_REG_PLMN_CHANGED_1>");
                handlePlmnChange(ar, 0);
                break;
            case 11:
                logd("handleMessage : <EVENT_REG_PLMN_CHANGED_2>");
                handlePlmnChange(ar, 1);
                break;
            case 12:
                logd("handleMessage : <EVENT_REG_PLMN_CHANGED_3>");
                handlePlmnChange(ar, 2);
                break;
            case 13:
                logd("handleMessage : <EVENT_REG_PLMN_CHANGED_4>");
                handlePlmnChange(ar, 3);
                break;
            case 30:
                logd("handleMessage : <EVENT_REG_SUSPENDED_1>");
                handleRegistrationSuspend(ar, 0);
                break;
            case 31:
                logd("handleMessage : <EVENT_REG_SUSPENDED_2>");
                handleRegistrationSuspend(ar, 1);
                break;
            case 32:
                logd("handleMessage : <EVENT_REG_SUSPENDED_3>");
                handleRegistrationSuspend(ar, 2);
                break;
            case 33:
                logd("handleMessage : <EVENT_REG_SUSPENDED_4>");
                handleRegistrationSuspend(ar, 3);
                break;
            case 60:
                logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_1>");
                handleInvalidSimNotify(0, ar);
                break;
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
                logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_2>");
                handleInvalidSimNotify(1, ar);
                break;
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
                logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_3>");
                handleInvalidSimNotify(2, ar);
                break;
            case 63:
                logd("handleMessage : <EVENT_INVALID_SIM_NOTIFY_4>");
                handleInvalidSimNotify(3, ar);
                break;
            case 70:
                if (ar.exception != null) {
                    logd("handleMessage : <EVENT_RESUME_CAMPING_1> with exception");
                    sIsResumeCampingFail1 = true;
                }
                break;
            case 71:
                if (ar.exception != null) {
                    logd("handleMessage : <EVENT_RESUME_CAMPING_2> with exception");
                    sIsResumeCampingFail2 = true;
                }
                break;
            case 72:
                if (ar.exception != null) {
                    logd("handleMessage : <EVENT_RESUME_CAMPING_3> with exception");
                    sIsResumeCampingFail3 = true;
                }
                break;
            case 73:
                if (ar.exception != null) {
                    logd("handleMessage : <EVENT_RESUME_CAMPING_4> with exception");
                    sIsResumeCampingFail4 = true;
                }
                break;
            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_1:
                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_1>");
                handleGmssRatChange(ar, 0);
                break;
            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_2:
                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_2>");
                handleGmssRatChange(ar, 1);
                break;
            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_3:
                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_3>");
                handleGmssRatChange(ar, 2);
                break;
            case IWorldPhone.EVENT_WP_GMSS_RAT_CHANGED_4:
                logd("handleMessage : <EVENT_WP_GMSS_RAT_CHANGED_4>");
                handleGmssRatChange(ar, 3);
                break;
            default:
                logd("Unknown msg:" + msg.what);
                break;
        }
    }

    private void handleRadioOn(int slotId) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("handleRadioOn Slot:" + slotId + " sMajorSim:" + sMajorSim);
        sIsInvalidSim[slotId] = false;
        switch (slotId) {
            case 0:
                if (sIsResumeCampingFail1) {
                    logd("try to resume camping again");
                    sCi[slotId].setResumeRegistration(sSuspendId[slotId], null);
                    sIsResumeCampingFail1 = false;
                }
                break;
            case 1:
                if (sIsResumeCampingFail2) {
                    logd("try to resume camping again");
                    sCi[slotId].setResumeRegistration(sSuspendId[slotId], null);
                    sIsResumeCampingFail2 = false;
                }
                break;
            case 2:
                if (sIsResumeCampingFail3) {
                    logd("try to resume camping again");
                    sCi[slotId].setResumeRegistration(sSuspendId[slotId], null);
                    sIsResumeCampingFail3 = false;
                }
                break;
            case 3:
                if (sIsResumeCampingFail4) {
                    logd("try to resume camping again");
                    sCi[slotId].setResumeRegistration(sSuspendId[slotId], null);
                    sIsResumeCampingFail4 = false;
                }
                break;
            default:
                logd("unknow slotid");
                break;
        }
    }

    private void handlePlmnChange(AsyncResult ar, int slotId) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("Slot:" + slotId + " sMajorSim:" + sMajorSim);
        if (ar.exception == null && ar.result != null) {
            String[] plmnString = (String[]) ar.result;
            if (slotId == sMajorSim) {
                sNwPlmnStrings = plmnString;
            }
            for (int i = 0; i < plmnString.length; i++) {
                logd("plmnString[" + i + "]=" + plmnString[i]);
            }
            if (!sIsAutoSelectEnable) {
                return;
            }
            if (sMajorSim == slotId && ((sUserType == 1 || sUserType == 2) && sDenyReason != 2)) {
                searchForDesignateService(plmnString[0]);
            }
            sRegion = getRegion(plmnString[0]);
            if (sUserType == 3 || sRegion != 2 || sMajorSim == -1) {
                return;
            }
            sSwitchModemCauseType = 0;
            logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
            handleSwitchModem(100);
            return;
        }
        logd("AsyncResult is wrong " + ar.exception);
    }

    private void handleGmssRatChange(AsyncResult ar, int slotId) {
        sMajorSim = WorldPhoneUtil.getMajorSim();
        logd("Slot:" + slotId + " sMajorSim:" + sMajorSim);
        if (ar.exception == null && ar.result != null) {
            int[] info = (int[]) ar.result;
            String mccString = Integer.toString(info[1]);
            logd("[handleGmssRatChange] mccString=" + mccString);
            if (slotId == sMajorSim && mccString.length() >= 3) {
                if (sNwPlmnStrings == null) {
                    sNwPlmnStrings = new String[1];
                }
                sNwPlmnStrings[0] = mccString;
            }
            if (!sIsAutoSelectEnable) {
                return;
            }
            sRegion = getRegion(mccString);
            if (sUserType == 3 || sRegion != 2 || sMajorSim == -1) {
                return;
            }
            handleSwitchModem(100);
            return;
        }
        logd("AsyncResult is wrong " + ar.exception);
    }

    private void handleRegistrationSuspend(AsyncResult ar, int slotId) {
        logd("Slot" + slotId);
        if (ar.exception == null && ar.result != null) {
            sSuspendId[slotId] = ((int[]) ar.result)[0];
            logd("Suspending with Id=" + sSuspendId[slotId]);
            if (sIsAutoSelectEnable && sMajorSim == slotId) {
                if (sUserType != 0) {
                    resumeCampingProcedure(slotId);
                    return;
                } else {
                    sSuspendWaitImsi[slotId] = true;
                    logd("User type unknown, wait for IMSI");
                    return;
                }
            }
            logd("Not major slot, camp on OK");
            sCi[slotId].setResumeRegistration(sSuspendId[slotId], null);
            return;
        }
        logd("AsyncResult is wrong " + ar.exception);
    }

    private void handleInvalidSimNotify(int slotId, AsyncResult ar) {
        logd("Slot" + slotId);
        if (ar.exception == null && ar.result != null) {
            String[] invalidSimInfo = (String[]) ar.result;
            String plmn = invalidSimInfo[0];
            int cs_invalid = Integer.parseInt(invalidSimInfo[1]);
            int ps_invalid = Integer.parseInt(invalidSimInfo[2]);
            int cause = Integer.parseInt(invalidSimInfo[3]);
            int testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
            if (testMode != 0) {
                logd("Invalid SIM notified during test mode: " + testMode);
                return;
            }
            logd("testMode:" + testMode + ", cause: " + cause + ", cs_invalid: " + cs_invalid + ", ps_invalid: " + ps_invalid + ", plmn: " + plmn);
            if (sVoiceCapable && cs_invalid == 1 && sLastPlmn == null) {
                logd("CS reject, invalid SIM");
                sIsInvalidSim[slotId] = true;
                return;
            } else {
                if (ps_invalid != 1 || sLastPlmn != null) {
                    return;
                }
                logd("PS reject, invalid SIM");
                sIsInvalidSim[slotId] = true;
                return;
            }
        }
        logd("AsyncResult is wrong " + ar.exception);
    }

    private void handleSwitchModem(int toModem) {
        int mMajorSim = WorldPhoneUtil.getMajorSim();
        if (mMajorSim >= 0 && sIsInvalidSim[mMajorSim] && WorldPhoneUtil.getModemSelectionMode() == 1) {
            logd("Invalid SIM, switch not executed!");
            return;
        }
        if (sIsAutoSelectEnable && !isNeedSwitchModem()) {
            logd("[handleSwitchModem]No need to handle, switch not executed!");
            return;
        }
        if (toModem == 101) {
            if (WorldPhoneUtil.isLteSupport()) {
                toModem = 6;
            } else {
                toModem = 4;
            }
        } else if (toModem == 100) {
            if (WorldPhoneUtil.isLteSupport()) {
                toModem = 5;
            } else {
                toModem = 3;
            }
        }
        if (toModem == ModemSwitchHandler.getActiveModemType()) {
            if (toModem == 3) {
                logd("Already in WG modem");
                return;
            }
            if (toModem == 4) {
                logd("Already in TG modem");
                return;
            } else if (toModem == 5) {
                logd("Already in FDD CSFB modem");
                return;
            } else {
                if (toModem == 6) {
                    logd("Already in TDD CSFB modem");
                    return;
                }
                return;
            }
        }
        if (!sIsAutoSelectEnable || sDefaultBootuUpModem == 0) {
            logd("Storing modem type: " + toModem);
            sCi[0].storeModemType(toModem, null);
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
        for (int i = 0; i < PROJECT_SIM_NUM; i++) {
            if (sActivePhones[i].getState() != PhoneConstants.State.IDLE) {
                logd("Phone" + i + " is not idle, modem switch not allowed");
                return;
            }
        }
        removeModemStandByTimer();
        if (toModem == 3) {
            logd("Switching to WG modem");
        } else if (toModem == 4) {
            logd("Switching to TG modem");
        } else if (toModem == 5) {
            logd("Switching to FDD CSFB modem");
        } else if (toModem == 6) {
            logd("Switching to TDD CSFB modem");
        }
        if (WorldPhoneUtil.isSimSwitching() && toModem == WorldPhoneUtil.getToModemType()) {
            logd("sim switching, already will to set modem:" + toModem);
            return;
        }
        SystemProperties.set(IWorldPhone.PROPERTY_SWITCH_MODEM_CAUSE_TYPE, String.valueOf(sSwitchModemCauseType));
        ModemSwitchHandler.switchModem(0, toModem);
        resetNetworkProperties();
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
        if (sImsi[sMajorSim] == null || sImsi[sMajorSim].equals(UsimPBMemInfo.STRING_NOT_SET)) {
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
        int mdType = ModemSwitchHandler.getActiveModemType();
        logd(ModemSwitchHandler.modemToString(mdType));
        IccCardConstants.State iccState = sProxyPhones[sMajorSim].getIccCard().getState();
        if (iccState == IccCardConstants.State.READY) {
            if (sUserType == 1 || sUserType == 2) {
                if (mdType == 6 || mdType == 4) {
                    if (TDD_STANDBY_TIMER[sTddStandByCounter] >= 0) {
                        if (!sWaitInTdd) {
                            sWaitInTdd = true;
                            logd("Wait " + TDD_STANDBY_TIMER[sTddStandByCounter] + "s. Timer index = " + sTddStandByCounter);
                            postDelayed(this.mTddStandByTimerRunnable, TDD_STANDBY_TIMER[sTddStandByCounter] * 1000);
                        } else {
                            logd("Timer already set:" + TDD_STANDBY_TIMER[sTddStandByCounter] + "s");
                        }
                    } else {
                        logd("Standby in TDD modem");
                    }
                } else if (mdType == 5 || mdType == 3) {
                    if (FDD_STANDBY_TIMER[sFddStandByCounter] >= 0) {
                        if (sRegion == 2) {
                            if (!sWaitInFdd) {
                                sWaitInFdd = true;
                                logd("Wait " + FDD_STANDBY_TIMER[sFddStandByCounter] + "s. Timer index = " + sFddStandByCounter);
                                postDelayed(this.mFddStandByTimerRunnable, FDD_STANDBY_TIMER[sFddStandByCounter] * 1000);
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
                if (mdType == 5 || mdType == 3) {
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

    private boolean isAllowCampOn(String plmnString, int slotId) {
        int mdType;
        logd("[isAllowCampOn]+ " + plmnString);
        logd("User type: " + sUserType);
        logd(WorldPhoneUtil.iccCardTypeToString(sIccCardType[slotId]));
        sRegion = getRegion(plmnString);
        if (WorldPhoneUtil.isSimSwitching()) {
            mdType = WorldPhoneUtil.getToModemType();
            logd("SimSwitching mdType:" + ModemSwitchHandler.modemToString(mdType));
        } else {
            mdType = ModemSwitchHandler.getActiveModemType();
            logd("mdType:" + ModemSwitchHandler.modemToString(mdType));
        }
        if (sUserType == 1 || sUserType == 2) {
            if (sRegion == 1) {
                if (mdType == 6 || mdType == 4) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    logd("[isAllowCampOn]-");
                    return true;
                }
                if (mdType == 5 || mdType == 3) {
                    sDenyReason = 3;
                    logd("Camp on REJECT");
                    logd("[isAllowCampOn]-");
                    return false;
                }
            } else if (sRegion == 2) {
                if (mdType == 6 || mdType == 4) {
                    sDenyReason = 2;
                    logd("Camp on REJECT");
                    logd("[isAllowCampOn]-");
                    return false;
                }
                if (mdType == 5 || mdType == 3) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    logd("[isAllowCampOn]-");
                    return true;
                }
            } else {
                logd("Unknow region");
            }
        } else if (sUserType == 3) {
            if (mdType == 6 || mdType == 4) {
                sDenyReason = 2;
                logd("Camp on REJECT");
                logd("[isAllowCampOn]-");
                return false;
            }
            if (mdType == 5 || mdType == 3) {
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
        boolean inService = false;
        if (sVoiceRegState == 0 || sDataRegState == 0) {
            inService = true;
        }
        logd("inService: " + inService);
        return inService;
    }

    private boolean isNoService() {
        boolean noService;
        if (sVoiceRegState == 1 && ((sRilVoiceRegState == 0 || sRilVoiceRegState == 10) && sDataRegState == 1 && sRilDataRegState == 0)) {
            noService = true;
        } else {
            noService = false;
        }
        logd("noService: " + noService);
        return noService;
    }

    private int getIccCardType(int slotId) {
        String simString = sProxyPhones[slotId].getIccCard().getIccCardType();
        if (simString.equals("SIM")) {
            logd("IccCard type: SIM");
            return 1;
        }
        if (simString.equals("USIM")) {
            logd("IccCard type: USIM");
            return 2;
        }
        logd("IccCard type: Unknown");
        return 0;
    }

    private int getRegion(String plmn) {
        if (plmn == null || plmn.equals(UsimPBMemInfo.STRING_NOT_SET) || plmn.length() < 3) {
            logd("[getRegion] Invalid PLMN");
            return 0;
        }
        String currentMcc = plmn.length() >= 5 ? plmn.substring(0, 5) : null;
        if (currentMcc != null && (currentMcc.equals("46602") || currentMcc.equals("50270"))) {
            return 1;
        }
        String currentMcc2 = plmn.substring(0, 3);
        for (String mcc : MCC_TABLE_DOMESTIC) {
            if (currentMcc2.equals(mcc)) {
                logd("[getRegion] REGION_DOMESTIC");
                return 1;
            }
        }
        logd("[getRegion] REGION_FOREIGN");
        return 2;
    }

    private int getUserType(String imsi) {
        if (imsi != null && !imsi.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            String imsi2 = imsi.substring(0, 5);
            for (String mccmnc : PLMN_TABLE_TYPE1) {
                if (imsi2.equals(mccmnc)) {
                    logd("[getUserType] Type1 user");
                    return 1;
                }
            }
            for (String mccmnc2 : PLMN_TABLE_TYPE3) {
                if (imsi2.equals(mccmnc2)) {
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

    private void resumeCampingProcedure(int slotId) {
        logd("Resume camping slot" + slotId);
        String plmnString = sNwPlmnStrings[0];
        if (isAllowCampOn(plmnString, slotId) || !isNeedSwitchModem()) {
            removeModemStandByTimer();
            sCi[slotId].setResumeRegistration(sSuspendId[slotId], obtainMessage(slotId + 70));
            return;
        }
        logd("Because: " + WorldPhoneUtil.denyReasonToString(sDenyReason));
        sSwitchModemCauseType = 0;
        logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        if (sDenyReason == 2) {
            handleSwitchModem(100);
        } else {
            if (sDenyReason != 3) {
                return;
            }
            handleSwitchModem(101);
        }
    }

    private void removeModemStandByTimer() {
        if (sWaitInTdd) {
            logd("Remove TDD wait timer. Set sWaitInTdd = false");
            sWaitInTdd = false;
            removeCallbacks(this.mTddStandByTimerRunnable);
        }
        if (!sWaitInFdd) {
            return;
        }
        logd("Remove FDD wait timer. Set sWaitInFdd = false");
        sWaitInFdd = false;
        removeCallbacks(this.mFddStandByTimerRunnable);
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
    }

    private void resetNetworkProperties() {
        logd("[resetNetworkProperties]");
        synchronized (sLock) {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                sSuspendWaitImsi[i] = false;
            }
            if (sNwPlmnStrings != null) {
                for (int i2 = 0; i2 < sNwPlmnStrings.length; i2++) {
                    sNwPlmnStrings[i2] = UsimPBMemInfo.STRING_NOT_SET;
                }
            }
            sSwitchModemCauseType = 255;
            logd("sSwitchModemCauseType = " + sSwitchModemCauseType);
        }
    }

    private void resetSimProperties() {
        logd("[resetSimProperties]");
        synchronized (sLock) {
            for (int i = 0; i < PROJECT_SIM_NUM; i++) {
                sImsi[i] = UsimPBMemInfo.STRING_NOT_SET;
                sIccCardType[i] = 0;
            }
            sUserType = 0;
            sMajorSim = WorldPhoneUtil.getMajorSim();
        }
    }

    private void searchForDesignateService(String strPlmn) {
        if (strPlmn == null) {
            logd("[searchForDesignateService]- null source");
            return;
        }
        String strPlmn2 = strPlmn.substring(0, 5);
        for (String mccmnc : PLMN_TABLE_TYPE1) {
            if (strPlmn2.equals(mccmnc)) {
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
    public void setModemSelectionMode(int mode, int modemType) {
        Settings.Global.putInt(sContext.getContentResolver(), "world_phone_auto_select_mode", mode);
        if (mode == 1) {
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
        handleSwitchModem(modemType);
        if (modemType != ModemSwitchHandler.getActiveModemType()) {
            return;
        }
        removeModemStandByTimer();
    }

    @Override
    public void notifyRadioCapabilityChange(int capabilitySimId) {
        int toModem;
        logd("[setRadioCapabilityChange]");
        logd("Major capability will be set to slot:" + capabilitySimId);
        if (!sIsAutoSelectEnable) {
            logd("Auto modem selection disabled");
            removeModemStandByTimer();
            return;
        }
        logd("Auto modem selection enabled");
        if (sImsi[capabilitySimId] == null || sImsi[capabilitySimId].equals(UsimPBMemInfo.STRING_NOT_SET)) {
            logd("Capaility slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sUserType = getUserType(sImsi[capabilitySimId]);
        if (sUserType == 1 || sUserType == 2) {
            if (sNwPlmnStrings != null) {
                sRegion = getRegion(sNwPlmnStrings[0]);
            }
            if (sRegion == 1) {
                sFirstSelect[capabilitySimId] = false;
                sIccCardType[capabilitySimId] = getIccCardType(capabilitySimId);
                toModem = 101;
            } else if (sRegion == 2) {
                sFirstSelect[capabilitySimId] = false;
                toModem = 100;
            } else {
                logd("Unknown region");
                return;
            }
        } else if (sUserType == 3) {
            sFirstSelect[capabilitySimId] = false;
            toModem = 100;
        } else {
            logd("Unknown user type");
            return;
        }
        if (toModem == 101) {
            if (WorldPhoneUtil.isLteSupport()) {
                toModem = 6;
            } else {
                toModem = 4;
            }
        } else if (toModem == 100) {
            if (WorldPhoneUtil.isLteSupport()) {
                toModem = 5;
            } else {
                toModem = 3;
            }
        }
        logd("notifyRadioCapabilityChange: Storing modem type: " + toModem);
        if (!isNeedReloadModem(capabilitySimId)) {
            return;
        }
        sCi[0].reloadModemType(toModem, null);
        resetNetworkProperties();
        WorldPhoneUtil.setSimSwitchingFlag(true);
        WorldPhoneUtil.saveToModemType(toModem);
    }

    private boolean isNeedSwitchModem() {
        boolean isNeed = true;
        int majorSimId = WorldPhoneUtil.getMajorSim();
        if (WorldPhoneUtil.isC2kSupport()) {
            int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
            if (sUserType == 2 && (((majorSimId >= 0 && majorSimId == activeSvlteModeSlotId) || isCdmaCard(majorSimId)) && ModemSwitchHandler.getActiveModemType() == 5)) {
                isNeed = false;
            }
        }
        logd("[isNeedSwitchModem] isNeed = " + isNeed);
        return isNeed;
    }

    private boolean isNeedReloadModem(int capabilitySimId) {
        boolean isNeed = true;
        if (WorldPhoneUtil.isC2kSupport()) {
            int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
            logd("[isNeedReloadModem] activeSvlteModeSlotId = " + activeSvlteModeSlotId + ", sUserType = " + sUserType + ", capabilitySimId = " + capabilitySimId);
            if (sUserType == 2 && (((capabilitySimId >= 0 && capabilitySimId == activeSvlteModeSlotId) || isCdmaCard(capabilitySimId)) && ModemSwitchHandler.getActiveModemType() == 5)) {
                isNeed = false;
            }
        }
        logd("[isNeedReloadModem] isNeed = " + isNeed);
        return isNeed;
    }

    private boolean isCdmaCard(int slotId) {
        boolean zIsCt3gDualMode;
        if (!SubscriptionManager.isValidPhoneId(slotId)) {
            return false;
        }
        int[] cardType = WorldPhoneUtil.getC2KWPCardType();
        logd("isCdmaCard(), cardType=" + cardType[slotId]);
        if ((cardType[slotId] & 4) > 0 || (cardType[slotId] & 8) > 0) {
            zIsCt3gDualMode = true;
        } else {
            zIsCt3gDualMode = isCt3gDualMode(slotId);
        }
        logd("isCdmaCard(), slotId=" + slotId + " retCdmaCard=" + zIsCt3gDualMode);
        return zIsCt3gDualMode;
    }

    private boolean isCt3gDualMode(int slotId) {
        if (slotId < 0 || slotId >= PROPERTY_RIL_CT3G.length) {
            logd("isCt3gDualMode: invalid slotId " + slotId);
            return false;
        }
        String result = SystemProperties.get(PROPERTY_RIL_CT3G[slotId], UsimPBMemInfo.STRING_NOT_SET);
        logd("isCt3gDualMode: " + result);
        return "1".equals(result);
    }

    private static void logd(String msg) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPOP01]" + msg);
    }
}
