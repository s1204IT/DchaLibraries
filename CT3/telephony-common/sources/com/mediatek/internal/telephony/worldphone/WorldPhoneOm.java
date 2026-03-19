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
import java.util.ArrayList;

public class WorldPhoneOm extends Handler implements IWorldPhone {
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
    private static ArrayList<String> sMccDomestic;
    private static String[] sNwPlmnStrings;
    private static String sOperatorSpec;
    private static String sPlmnSs;
    private static ArrayList<String> sPlmnType1;
    private static ArrayList<String> sPlmnType1Ext;
    private static ArrayList<String> sPlmnType3;
    private static int sRegion;
    private static int sRilDataRadioTechnology;
    private static int sRilDataRegState;
    private static int sRilVoiceRadioTechnology;
    private static int sRilVoiceRegState;
    private static ServiceState sServiceState;
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
    private static final String[] PLMN_TABLE_TYPE1 = {"46000", "46002", "46004", "46007", "46008"};
    private static final String[] PLMN_TABLE_TYPE1_EXT = {"45412"};
    private static final String[] PLMN_TABLE_TYPE3 = {"46001", "46006", "46009", "45407", "46003", "46005", "45502", "46011"};
    private static final String[] MCC_TABLE_DOMESTIC = {RadioCapabilitySwitchUtil.CN_MCC};
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
            WorldPhoneOm.logd("Action: " + action);
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                String simStatus = intent.getStringExtra("ss");
                int slotId = intent.getIntExtra("slot", 0);
                int unused = WorldPhoneOm.sMajorSim = WorldPhoneUtil.getMajorSim();
                WorldPhoneOm.logd("slotId: " + slotId + " simStatus: " + simStatus + " sMajorSim:" + WorldPhoneOm.sMajorSim);
                if (!"IMSI".equals(simStatus)) {
                    if (!simStatus.equals("ABSENT")) {
                        if (simStatus.equals("READY")) {
                            WorldPhoneOm.logd("reset sIsInvalidSim by solt:" + slotId);
                            WorldPhoneOm.sIsInvalidSim[slotId] = false;
                            return;
                        }
                        return;
                    }
                    String unused2 = WorldPhoneOm.sLastPlmn = null;
                    WorldPhoneOm.sImsi[slotId] = UsimPBMemInfo.STRING_NOT_SET;
                    WorldPhoneOm.sFirstSelect[slotId] = true;
                    WorldPhoneOm.sSuspendWaitImsi[slotId] = false;
                    WorldPhoneOm.sIccCardType[slotId] = 0;
                    if (slotId != WorldPhoneOm.sMajorSim) {
                        WorldPhoneOm.logd("SIM" + slotId + " is not major SIM");
                        return;
                    }
                    WorldPhoneOm.logd("Major SIM removed, no world phone service");
                    WorldPhoneOm.this.removeModemStandByTimer();
                    int unused3 = WorldPhoneOm.sUserType = 0;
                    int unused4 = WorldPhoneOm.sDenyReason = 1;
                    int unused5 = WorldPhoneOm.sMajorSim = -99;
                    int unused6 = WorldPhoneOm.sRegion = 0;
                    return;
                }
                UiccController unused7 = WorldPhoneOm.sUiccController = UiccController.getInstance();
                if (WorldPhoneOm.sUiccController == null) {
                    WorldPhoneOm.logd("Null sUiccController");
                    return;
                }
                WorldPhoneOm.sIccRecordsInstance[slotId] = WorldPhoneOm.sProxyPhones[slotId].getIccCard().getIccRecords();
                if (WorldPhoneOm.sIccRecordsInstance[slotId] == null) {
                    WorldPhoneOm.logd("Null sIccRecordsInstance");
                    return;
                }
                WorldPhoneOm.sImsi[slotId] = WorldPhoneOm.sIccRecordsInstance[slotId].getIMSI();
                WorldPhoneOm.sIccCardType[slotId] = WorldPhoneOm.this.getIccCardType(slotId);
                WorldPhoneOm.logd("sImsi[" + slotId + "]:" + WorldPhoneOm.sImsi[slotId]);
                if (!WorldPhoneOm.sIsAutoSelectEnable || slotId != WorldPhoneOm.sMajorSim) {
                    WorldPhoneOm.logd("Not major SIM or in maual selection mode");
                    WorldPhoneOm.this.getUserType(WorldPhoneOm.sImsi[slotId]);
                    if (WorldPhoneOm.sSuspendWaitImsi[slotId]) {
                        WorldPhoneOm.sSuspendWaitImsi[slotId] = false;
                        WorldPhoneOm.logd("IMSI fot slot" + slotId + " now ready, resuming with ID:" + WorldPhoneOm.sSuspendId[slotId]);
                        WorldPhoneOm.sCi[slotId].setResumeRegistration(WorldPhoneOm.sSuspendId[slotId], null);
                        return;
                    }
                    return;
                }
                WorldPhoneOm.logd("Major SIM");
                int unused8 = WorldPhoneOm.sUserType = WorldPhoneOm.this.getUserType(WorldPhoneOm.sImsi[slotId]);
                if (WorldPhoneOm.sFirstSelect[slotId]) {
                    WorldPhoneOm.sFirstSelect[slotId] = false;
                    if (WorldPhoneOm.sUserType == 1) {
                        if (WorldPhoneOm.sRegion == 1) {
                            WorldPhoneOm.this.handleSwitchModem(101);
                        } else if (WorldPhoneOm.sRegion == 2) {
                            WorldPhoneOm.this.handleSwitchModem(100);
                        } else {
                            WorldPhoneOm.logd("Region unknown");
                        }
                    } else if (WorldPhoneOm.sUserType == 2 || WorldPhoneOm.sUserType == 3) {
                        WorldPhoneOm.this.handleSwitchModem(100);
                    }
                }
                if (WorldPhoneOm.sSuspendWaitImsi[slotId]) {
                    WorldPhoneOm.sSuspendWaitImsi[slotId] = false;
                    if (WorldPhoneOm.sNwPlmnStrings == null) {
                        WorldPhoneOm.logd("sNwPlmnStrings is Null");
                        return;
                    } else {
                        WorldPhoneOm.logd("IMSI fot slot" + slotId + " now ready, resuming PLMN:" + WorldPhoneOm.sNwPlmnStrings[0] + " with ID:" + WorldPhoneOm.sSuspendId[slotId]);
                        WorldPhoneOm.this.resumeCampingProcedure(slotId);
                        return;
                    }
                }
                return;
            }
            if (action.equals("android.intent.action.SERVICE_STATE")) {
                if (intent.getExtras() != null) {
                    ServiceState unused9 = WorldPhoneOm.sServiceState = ServiceState.newFromBundle(intent.getExtras());
                    if (WorldPhoneOm.sServiceState == null) {
                        WorldPhoneOm.logd("Null sServiceState");
                        return;
                    }
                    int slotId2 = intent.getIntExtra("slot", 0);
                    String unused10 = WorldPhoneOm.sPlmnSs = WorldPhoneOm.sServiceState.getOperatorNumeric();
                    int unused11 = WorldPhoneOm.sVoiceRegState = WorldPhoneOm.sServiceState.getVoiceRegState();
                    int unused12 = WorldPhoneOm.sRilVoiceRegState = WorldPhoneOm.sServiceState.getRilVoiceRegState();
                    int unused13 = WorldPhoneOm.sRilVoiceRadioTechnology = WorldPhoneOm.sServiceState.getRilVoiceRadioTechnology();
                    int unused14 = WorldPhoneOm.sDataRegState = WorldPhoneOm.sServiceState.getDataRegState();
                    int unused15 = WorldPhoneOm.sRilDataRegState = WorldPhoneOm.sServiceState.getRilDataRegState();
                    int unused16 = WorldPhoneOm.sRilDataRadioTechnology = WorldPhoneOm.sServiceState.getRilDataRadioTechnology();
                    WorldPhoneOm.logd("slotId: " + slotId2 + ", " + WorldPhoneUtil.iccCardTypeToString(WorldPhoneOm.sIccCardType[slotId2]) + ", sMajorSim: " + WorldPhoneOm.sMajorSim + ", sPlmnSs: " + WorldPhoneOm.sPlmnSs + ", sVoiceRegState: " + WorldPhoneUtil.stateToString(WorldPhoneOm.sVoiceRegState));
                    StringBuilder sbAppend = new StringBuilder().append("sRilVoiceRegState: ").append(WorldPhoneUtil.regStateToString(WorldPhoneOm.sRilVoiceRegState)).append(", ").append("sRilVoiceRadioTech: ");
                    ServiceState unused17 = WorldPhoneOm.sServiceState;
                    WorldPhoneOm.logd(sbAppend.append(ServiceState.rilRadioTechnologyToString(WorldPhoneOm.sRilVoiceRadioTechnology)).append(", ").append("sDataRegState: ").append(WorldPhoneUtil.stateToString(WorldPhoneOm.sDataRegState)).toString());
                    StringBuilder sbAppend2 = new StringBuilder().append("sRilDataRegState: ").append(WorldPhoneUtil.regStateToString(WorldPhoneOm.sRilDataRegState)).append(", ").append("sRilDataRadioTech: ").append(", ");
                    ServiceState unused18 = WorldPhoneOm.sServiceState;
                    WorldPhoneOm.logd(sbAppend2.append(ServiceState.rilRadioTechnologyToString(WorldPhoneOm.sRilDataRadioTechnology)).append(", ").append("sIsAutoSelectEnable: ").append(WorldPhoneOm.sIsAutoSelectEnable).toString());
                    ModemSwitchHandler.getActiveModemType();
                    if (slotId2 == WorldPhoneOm.sMajorSim) {
                        if (!WorldPhoneOm.sIsAutoSelectEnable) {
                            if (WorldPhoneOm.this.isInService()) {
                                WorldPhoneOm.logd("reset sIsInvalidSim in manual mode");
                                String unused19 = WorldPhoneOm.sLastPlmn = WorldPhoneOm.sPlmnSs;
                                WorldPhoneOm.sIsInvalidSim[slotId2] = false;
                                return;
                            }
                            return;
                        }
                        if (WorldPhoneOm.this.isNoService()) {
                            WorldPhoneOm.this.handleNoService();
                            return;
                        } else {
                            if (WorldPhoneOm.this.isInService()) {
                                String unused20 = WorldPhoneOm.sLastPlmn = WorldPhoneOm.sPlmnSs;
                                WorldPhoneOm.this.removeModemStandByTimer();
                                WorldPhoneOm.logd("reset sIsInvalidSim");
                                WorldPhoneOm.sIsInvalidSim[slotId2] = false;
                                return;
                            }
                            return;
                        }
                    }
                    return;
                }
                return;
            }
            if (action.equals(IWorldPhone.ACTION_SHUTDOWN_IPO)) {
                if (WorldPhoneOm.sDefaultBootuUpModem == 100) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        ModemSwitchHandler.reloadModem(WorldPhoneOm.sCi[0], 5);
                        WorldPhoneOm.logd("Reload to FDD CSFB modem");
                        return;
                    } else {
                        ModemSwitchHandler.reloadModem(WorldPhoneOm.sCi[0], 3);
                        WorldPhoneOm.logd("Reload to WG modem");
                        return;
                    }
                }
                if (WorldPhoneOm.sDefaultBootuUpModem == 101) {
                    if (WorldPhoneUtil.isLteSupport()) {
                        ModemSwitchHandler.reloadModem(WorldPhoneOm.sCi[0], 6);
                        WorldPhoneOm.logd("Reload to TDD CSFB modem");
                        return;
                    } else {
                        ModemSwitchHandler.reloadModem(WorldPhoneOm.sCi[0], 4);
                        WorldPhoneOm.logd("Reload to TG modem");
                        return;
                    }
                }
                return;
            }
            if (action.equals(IWorldPhone.ACTION_ADB_SWITCH_MODEM)) {
                int toModem = intent.getIntExtra("mdType", 0);
                WorldPhoneOm.logd("toModem: " + toModem);
                if (toModem == 3 || toModem == 4 || toModem == 5 || toModem == 6) {
                    WorldPhoneOm.this.setModemSelectionMode(0, toModem);
                    return;
                } else {
                    WorldPhoneOm.this.setModemSelectionMode(1, toModem);
                    return;
                }
            }
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (intent.getBooleanExtra("state", false)) {
                    WorldPhoneOm.logd("Enter flight mode");
                    for (int i = 0; i < WorldPhoneOm.PROJECT_SIM_NUM; i++) {
                        WorldPhoneOm.sFirstSelect[i] = true;
                    }
                    return;
                }
                WorldPhoneOm.logd("Leave flight mode");
                String unused21 = WorldPhoneOm.sLastPlmn = null;
                for (int i2 = 0; i2 < WorldPhoneOm.PROJECT_SIM_NUM; i2++) {
                    WorldPhoneOm.sIsInvalidSim[i2] = false;
                }
                return;
            }
            if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                int unused22 = WorldPhoneOm.sMajorSim = WorldPhoneUtil.getMajorSim();
                if (WorldPhoneUtil.isSimSwitching()) {
                    WorldPhoneUtil.setSimSwitchingFlag(false);
                    ModemSwitchHandler.setActiveModemType(WorldPhoneUtil.getToModemType());
                }
                WorldPhoneOm.this.handleSimSwitched();
                return;
            }
            if (!action.equals(IWorldPhone.ACTION_TEST_WORLDPHONE)) {
                if (action.equals(IWorldPhone.ACTION_SAP_CONNECTION_STATE_CHANGED)) {
                    int sapState = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                    if (sapState == 2) {
                        WorldPhoneOm.logd("BT_SAP connection state is CONNECTED");
                        int unused23 = WorldPhoneOm.sBtSapState = 1;
                        return;
                    } else if (sapState != 0) {
                        WorldPhoneOm.logd("BT_SAP connection state is " + sapState);
                        return;
                    } else {
                        WorldPhoneOm.logd("BT_SAP connection state is DISCONNECTED");
                        int unused24 = WorldPhoneOm.sBtSapState = 0;
                        return;
                    }
                }
                return;
            }
            int fakeUserType = intent.getIntExtra(IWorldPhone.EXTRA_FAKE_USER_TYPE, 0);
            int fakeRegion = intent.getIntExtra(IWorldPhone.EXTRA_FAKE_REGION, 0);
            boolean hasChanged = false;
            if (fakeUserType == 0 && fakeRegion == 0) {
                WorldPhoneOm.logd("Leave ADB Test mode");
                WorldPhoneOm.sPlmnType1.clear();
                WorldPhoneOm.sPlmnType1Ext.clear();
                WorldPhoneOm.sPlmnType3.clear();
                WorldPhoneOm.sMccDomestic.clear();
                WorldPhoneOm.loadDefaultData();
                return;
            }
            int unused25 = WorldPhoneOm.sMajorSim = WorldPhoneUtil.getMajorSim();
            if (WorldPhoneOm.sMajorSim == -99 || WorldPhoneOm.sMajorSim == -1) {
                WorldPhoneOm.logd("sMajorSim is Unknown or Capability OFF");
            } else {
                String imsi = WorldPhoneOm.sImsi[WorldPhoneOm.sMajorSim];
                if (imsi != null && !imsi.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    String imsi2 = imsi.substring(0, 5);
                    switch (fakeUserType) {
                        case 1:
                            WorldPhoneOm.sPlmnType1.add(imsi2);
                            hasChanged = true;
                            break;
                        case 2:
                        default:
                            WorldPhoneOm.logd("Unknown fakeUserType:" + fakeUserType);
                            break;
                        case 3:
                            WorldPhoneOm.sPlmnType3.add(imsi2);
                            hasChanged = true;
                            break;
                    }
                } else {
                    WorldPhoneOm.logd("Imsi of sMajorSim is unknown");
                }
                String currentMcc = WorldPhoneOm.sNwPlmnStrings[0];
                if (currentMcc == null || currentMcc.equals(UsimPBMemInfo.STRING_NOT_SET) || currentMcc.length() < 5) {
                    WorldPhoneOm.logd("Invalid sNwPlmnStrings");
                } else {
                    String currentMcc2 = currentMcc.substring(0, 3);
                    if (fakeRegion == 1) {
                        WorldPhoneOm.sMccDomestic.add(currentMcc2);
                        hasChanged = true;
                    } else if (fakeRegion == 2) {
                        WorldPhoneOm.sMccDomestic.remove(currentMcc2);
                        hasChanged = true;
                    } else {
                        WorldPhoneOm.logd("Unknown fakeRegion:" + fakeRegion);
                    }
                }
            }
            if (hasChanged) {
                WorldPhoneOm.logd("sPlmnType1:" + WorldPhoneOm.sPlmnType1);
                WorldPhoneOm.logd("sPlmnType1Ext:" + WorldPhoneOm.sPlmnType1Ext);
                WorldPhoneOm.logd("sPlmnType3:" + WorldPhoneOm.sPlmnType3);
                WorldPhoneOm.logd("sMccDomestic:" + WorldPhoneOm.sMccDomestic);
                WorldPhoneOm.this.handleRadioTechModeSwitch();
            }
        }
    };
    private Runnable mTddStandByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            WorldPhoneOm.sTddStandByCounter++;
            if (WorldPhoneOm.sTddStandByCounter >= WorldPhoneOm.TDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOm.sTddStandByCounter = WorldPhoneOm.TDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOm.sBtSapState == 0) {
                WorldPhoneOm.logd("TDD time out!");
                WorldPhoneOm.this.handleSwitchModem(100);
            } else {
                WorldPhoneOm.logd("TDD time out but BT SAP is connected, switch not executed!");
            }
        }
    };
    private Runnable mFddStandByTimerRunnable = new Runnable() {
        @Override
        public void run() {
            WorldPhoneOm.sFddStandByCounter++;
            if (WorldPhoneOm.sFddStandByCounter >= WorldPhoneOm.FDD_STANDBY_TIMER.length) {
                int unused = WorldPhoneOm.sFddStandByCounter = WorldPhoneOm.FDD_STANDBY_TIMER.length - 1;
            }
            if (WorldPhoneOm.sBtSapState == 0) {
                WorldPhoneOm.logd("FDD time out!");
                WorldPhoneOm.this.handleSwitchModem(101);
            } else {
                WorldPhoneOm.logd("FDD time out but BT SAP is connected, switch not executed!");
            }
        }
    };

    public WorldPhoneOm() {
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
        ModemSwitchHandler.getActiveModemType();
        IntentFilter intentFilter = new IntentFilter("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction(IWorldPhone.ACTION_SHUTDOWN_IPO);
        intentFilter.addAction(IWorldPhone.ACTION_ADB_SWITCH_MODEM);
        intentFilter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        intentFilter.addAction(IWorldPhone.ACTION_SAP_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(IWorldPhone.ACTION_TEST_WORLDPHONE);
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
        sPlmnType1 = new ArrayList<>();
        sPlmnType1Ext = new ArrayList<>();
        sPlmnType3 = new ArrayList<>();
        sMccDomestic = new ArrayList<>();
        loadDefaultData();
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
            sRegion = getRegion(plmnString[0]);
            if (sMajorSim == slotId && sUserType == 1 && sDenyReason != 2) {
                searchForDesignateService(plmnString[0]);
            }
            if (sUserType == 3 || sRegion != 2 || sMajorSim == -1) {
                return;
            }
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
            logd("Not major slot or in maual selection mode, camp on OK");
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
        if (sIsAutoSelectEnable && WorldPhoneUtil.isCdmaLteDcSupport() && !isNeedSwitchModem(mMajorSim)) {
            logd("[handleSwitchModem]No need to handle, switch not executed!");
            return;
        }
        if (mMajorSim >= 0 && sIsInvalidSim[mMajorSim] && WorldPhoneUtil.getModemSelectionMode() == 1) {
            logd("[handleSwitchModem] Invalid SIM, switch not executed!");
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
        if (!sIsAutoSelectEnable) {
            logd("[handleSwitchModem] Auto select disable, storing modem type: " + toModem);
            sCi[0].storeModemType(toModem, null);
        } else if (sDefaultBootuUpModem == 0) {
            logd("[handleSwitchModem] Storing modem type: " + toModem);
            sCi[0].storeModemType(toModem, null);
        } else if (sDefaultBootuUpModem == 100) {
            if (WorldPhoneUtil.isLteSupport()) {
                logd("[handleSwitchModem] Storing modem type: 5");
                sCi[0].storeModemType(5, null);
            } else {
                logd("[handleSwitchModem] Storing modem type: 3");
                sCi[0].storeModemType(3, null);
            }
        } else if (sDefaultBootuUpModem == 101) {
            if (WorldPhoneUtil.isLteSupport()) {
                logd("[handleSwitchModem] Storing modem type: 6");
                sCi[0].storeModemType(6, null);
            } else {
                logd("[handleSwitchModem] Storing modem type: 4");
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
        } else {
            ModemSwitchHandler.switchModem(0, toModem);
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
        logd("Major capability in slot" + sMajorSim);
        if (sImsi[sMajorSim] == null || sImsi[sMajorSim].equals(UsimPBMemInfo.STRING_NOT_SET)) {
            logd("Major slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sUserType = getUserType(sImsi[sMajorSim]);
        if (sUserType == 1) {
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
        if (sUserType == 2 || sUserType == 3) {
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
        IccCardConstants.State iccState = sProxyPhones[sMajorSim].getIccCard().getState();
        if (iccState == IccCardConstants.State.READY) {
            if (sUserType == 1) {
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
                        if (!sWaitInFdd) {
                            sWaitInFdd = true;
                            logd("Wait " + FDD_STANDBY_TIMER[sFddStandByCounter] + "s. Timer index = " + sFddStandByCounter);
                            postDelayed(this.mFddStandByTimerRunnable, FDD_STANDBY_TIMER[sFddStandByCounter] * 1000);
                        } else {
                            logd("Timer already set:" + FDD_STANDBY_TIMER[sFddStandByCounter] + "s");
                        }
                    } else {
                        logd("Standby in FDD modem");
                    }
                }
            } else if (sUserType == 2 || sUserType == 3) {
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
        logd("[isAllowCampOn] " + plmnString);
        logd("User type: " + sUserType);
        logd(WorldPhoneUtil.iccCardTypeToString(sIccCardType[slotId]));
        sRegion = getRegion(plmnString);
        if (WorldPhoneUtil.isSimSwitching()) {
            mdType = WorldPhoneUtil.getToModemType();
            logd("SimSwitching mdType:" + ModemSwitchHandler.modemToString(mdType));
        } else {
            mdType = ModemSwitchHandler.getActiveModemType();
        }
        if (sUserType == 1) {
            if (sRegion == 1) {
                if (mdType == 6 || mdType == 4) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    return true;
                }
                if (mdType == 5 || mdType == 3) {
                    sDenyReason = 3;
                    logd("Camp on REJECT");
                    return false;
                }
            } else if (sRegion == 2) {
                if (mdType == 6 || mdType == 4) {
                    sDenyReason = 2;
                    logd("Camp on REJECT");
                    return false;
                }
                if (mdType == 5 || mdType == 3) {
                    sDenyReason = 0;
                    logd("Camp on OK");
                    return true;
                }
            } else {
                logd("Unknow region");
            }
        } else if (sUserType == 2 || sUserType == 3) {
            if (mdType == 6 || mdType == 4) {
                sDenyReason = 2;
                logd("Camp on REJECT");
                return false;
            }
            if (mdType == 5 || mdType == 3) {
                sDenyReason = 0;
                logd("Camp on OK");
                return true;
            }
        } else {
            logd("Unknown user type");
        }
        sDenyReason = 1;
        logd("Camp on REJECT");
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
        String currentMcc = plmn.substring(0, 3);
        for (String mcc : sMccDomestic) {
            if (currentMcc.equals(mcc)) {
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
            for (String mccmnc : sPlmnType1) {
                if (imsi2.equals(mccmnc)) {
                    logd("[getUserType] Type1 user");
                    return 1;
                }
            }
            for (String mccmnc2 : sPlmnType1Ext) {
                if (imsi2.equals(mccmnc2)) {
                    logd("[getUserType] Extended Type1 user");
                    return 1;
                }
            }
            for (String mccmnc3 : sPlmnType3) {
                if (imsi2.equals(mccmnc3)) {
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
        logd("Resume camping slot " + slotId);
        if (sNwPlmnStrings != null && sNwPlmnStrings[0] != null) {
            String plmnString = sNwPlmnStrings[0];
            if (isAllowCampOn(plmnString, slotId)) {
                removeModemStandByTimer();
                sCi[slotId].setResumeRegistration(sSuspendId[slotId], obtainMessage(slotId + 70));
                return;
            }
            logd("Because: " + WorldPhoneUtil.denyReasonToString(sDenyReason));
            if (sDenyReason == 2) {
                handleSwitchModem(100);
                return;
            } else {
                if (sDenyReason != 3) {
                    return;
                }
                handleSwitchModem(101);
                return;
            }
        }
        logd("sNwPlmnStrings[0] is null");
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
        sIsResumeCampingFail1 = false;
        sIsResumeCampingFail2 = false;
        sIsResumeCampingFail3 = false;
        sIsResumeCampingFail4 = false;
        sBtSapState = 0;
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
        if (strPlmn == null || strPlmn.length() < 5) {
            logd("[searchForDesignateService]- null source");
            return;
        }
        String strPlmn2 = strPlmn.substring(0, 5);
        for (String mccmnc : sPlmnType1) {
            if (strPlmn2.equals(mccmnc)) {
                logd("Find TD service");
                logd("sUserType: " + sUserType + " sRegion: " + sRegion);
                if (sRegion == 1) {
                    ModemSwitchHandler.getActiveModemType();
                    handleSwitchModem(101);
                    return;
                }
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
        handleSwitchModem(modemType);
        if (modemType != ModemSwitchHandler.getActiveModemType()) {
            return;
        }
        removeModemStandByTimer();
    }

    @Override
    public void notifyRadioCapabilityChange(int capailitySimId) {
        int toModem;
        int majorSimId = WorldPhoneUtil.getMajorSim();
        int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
        logd("[setRadioCapabilityChange] majorSimId:" + majorSimId + " capailitySimId=" + capailitySimId);
        if (!sIsAutoSelectEnable) {
            logd("[setRadioCapabilityChange] Auto modem selection disabled");
            removeModemStandByTimer();
            return;
        }
        if (sImsi[capailitySimId] == null || sImsi[capailitySimId].equals(UsimPBMemInfo.STRING_NOT_SET)) {
            logd("Capaility slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sUserType = getUserType(sImsi[capailitySimId]);
        if (sUserType == 1) {
            if (sNwPlmnStrings != null) {
                sRegion = getRegion(sNwPlmnStrings[0]);
            }
            if (sRegion == 1) {
                sFirstSelect[capailitySimId] = false;
                sIccCardType[capailitySimId] = getIccCardType(capailitySimId);
                toModem = 101;
            } else if (sRegion == 2) {
                sFirstSelect[capailitySimId] = false;
                toModem = 100;
            } else {
                logd("Unknown region");
                return;
            }
        } else if (sUserType == 2 || sUserType == 3) {
            sFirstSelect[capailitySimId] = false;
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
        CommandsInterface ci = null;
        if (majorSimId != -99) {
            if (WorldPhoneUtil.isCdmaLteDcSupport()) {
                if (!isSpecialCardMode()) {
                    if (capailitySimId != activeSvlteModeSlotId) {
                        logd("new RT mode is CSFB");
                        ci = sCi[majorSimId];
                    } else if (toModem == 5) {
                        logd("new RT mode is SVLTE and new type is LWG");
                        ci = sCi[majorSimId];
                    }
                } else {
                    logd("isSpecialCardMode=true, ignore this change!");
                }
            } else {
                ci = sCi[0];
            }
            if (ci == null) {
                return;
            }
            ci.reloadModemType(toModem, null);
            resetNetworkProperties();
            WorldPhoneUtil.setSimSwitchingFlag(true);
            WorldPhoneUtil.saveToModemType(toModem);
            ci.storeModemType(toModem, null);
            return;
        }
        logd("notifyRadioCapabilityChange: major sim is unknown");
    }

    public void handleRadioTechModeSwitch() {
        int toModem;
        logd("[handleRadioTechModeSwitch]");
        if (!sIsAutoSelectEnable) {
            logd("Auto modem selection disabled");
            removeModemStandByTimer();
            return;
        }
        logd("Auto modem selection enabled");
        if (sImsi[sMajorSim] == null || sImsi[sMajorSim].equals(UsimPBMemInfo.STRING_NOT_SET)) {
            logd("Capaility slot IMSI not ready");
            sUserType = 0;
            return;
        }
        sUserType = getUserType(sImsi[sMajorSim]);
        if (sUserType == 1) {
            if (sNwPlmnStrings != null) {
                sRegion = getRegion(sNwPlmnStrings[0]);
            }
            if (sRegion == 1) {
                sFirstSelect[sMajorSim] = false;
                sIccCardType[sMajorSim] = getIccCardType(sMajorSim);
                toModem = 101;
            } else if (sRegion == 2) {
                sFirstSelect[sMajorSim] = false;
                toModem = 100;
            } else {
                logd("Unknown region");
                return;
            }
        } else if (sUserType == 2 || sUserType == 3) {
            sFirstSelect[sMajorSim] = false;
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
        logd("[handleRadioTechModeSwitch]: switch type: " + toModem);
        handleSwitchModem(toModem);
        resetNetworkProperties();
    }

    private boolean isNeedSwitchModem(int majorSimId) {
        boolean isNeed = true;
        if (WorldPhoneUtil.isC2kSupport()) {
            int activeSvlteModeSlotId = WorldPhoneUtil.getActiveSvlteModeSlotId();
            if (sUserType == 2 && majorSimId >= 0 && majorSimId == activeSvlteModeSlotId && ModemSwitchHandler.getActiveModemType() == 5) {
                isNeed = false;
            }
        }
        logd("[isNeedSwitchModem] isNeed = " + isNeed);
        return isNeed;
    }

    private boolean isSpecialCardMode() {
        boolean specialCardMode = false;
        int[] cardType = WorldPhoneUtil.getC2KWPCardType();
        if ((is4GCdmaCard(cardType[0]) && is4GCdmaCard(cardType[1])) || (is3GCdmaCard(cardType[0]) && is3GCdmaCard(cardType[1]))) {
            logd("isSpecialCardMode cardType1=" + cardType[0] + ", cardType2=" + cardType[1]);
            specialCardMode = true;
        }
        logd("isSpecialCardMode:" + specialCardMode);
        return specialCardMode;
    }

    private boolean is4GCdmaCard(int cardType) {
        return (cardType & 2) > 0 && containsCdma(cardType);
    }

    private boolean is3GCdmaCard(int cardType) {
        return (cardType & 2) == 0 && (cardType & 1) == 0 && containsCdma(cardType);
    }

    private boolean containsCdma(int cardType) {
        return (cardType & 4) > 0 || (cardType & 8) > 0;
    }

    private static void loadDefaultData() {
        for (String plmn : PLMN_TABLE_TYPE1) {
            sPlmnType1.add(plmn);
        }
        for (String plmn2 : PLMN_TABLE_TYPE1_EXT) {
            sPlmnType1Ext.add(plmn2);
        }
        for (String plmn3 : PLMN_TABLE_TYPE3) {
            sPlmnType3.add(plmn3);
        }
        for (String mcc : MCC_TABLE_DOMESTIC) {
            sMccDomestic.add(mcc);
        }
    }

    private static void logd(String msg) {
        Rlog.d(IWorldPhone.LOG_TAG, "[WPOM]" + msg);
    }
}
