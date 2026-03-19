package com.mediatek.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import com.android.ims.ImsManager;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RadioManager extends Handler {

    private static final int[] f40x22a12f3f = null;
    protected static final String ACTION_AIRPLANE_CHANGE_DONE = "com.mediatek.intent.action.AIRPLANE_CHANGE_DONE";
    public static final String ACTION_FORCE_SET_RADIO_POWER = "com.mediatek.internal.telephony.RadioManager.intent.action.FORCE_SET_RADIO_POWER";
    private static final String ACTION_WIFI_ONLY_MODE_CHANGED = "android.intent.action.ACTION_WIFI_ONLY_MODE";
    protected static final boolean AIRPLANE_MODE_OFF = false;
    protected static final boolean AIRPLANE_MODE_ON = true;
    private static final int EVENT_RADIO_AVAILABLE = 1;
    private static final int EVENT_VIRTUAL_SIM_ON = 2;
    protected static final String EXTRA_AIRPLANE_MODE = "airplaneMode";
    private static final boolean ICC_READ_NOT_READY = false;
    private static final boolean ICC_READ_READY = true;
    protected static final int INITIAL_RETRY_INTERVAL_MSEC = 200;
    protected static final int INVALID_PHONE_ID = -1;
    private static final String IS_NOT_SILENT_REBOOT = "0";
    protected static final String IS_SILENT_REBOOT = "1";
    static final String LOG_TAG = "RadioManager";
    protected static final boolean MODEM_POWER_OFF = false;
    protected static final boolean MODEM_POWER_ON = true;
    protected static final int MODE_PHONE1_ONLY = 1;
    private static final int MODE_PHONE2_ONLY = 2;
    private static final int MODE_PHONE3_ONLY = 4;
    private static final int MODE_PHONE4_ONLY = 8;
    private static final String MTK_C2K_SUPPORT = "ro.boot.opt_c2k_support";
    protected static final int NO_SIM_INSERTED = 0;
    private static final String PREF_CATEGORY_RADIO_STATUS = "RADIO_STATUS";
    private static final String PROPERTY_SILENT_REBOOT_CDMA = "cdma.ril.eboot";
    protected static final String PROPERTY_SILENT_REBOOT_MD1 = "gsm.ril.eboot";
    protected static final String PROPERTY_SILENT_REBOOT_MD2 = "gsm.ril.eboot.2";
    protected static final boolean RADIO_POWER_OFF = false;
    protected static final boolean RADIO_POWER_ON = true;
    private static final String REGISTRANTS_WITH_NO_NAME = "NO_NAME";
    protected static final int SIM_INSERTED = 1;
    private static final int SIM_NOT_INITIALIZED = -1;
    protected static final String STRING_NO_SIM_INSERTED = "N/A";
    protected static final int TO_SET_MODEM_POWER = 2;
    protected static final int TO_SET_RADIO_POWER = 1;
    protected static final int TO_SKIP_AND_FAKE_DONE = 0;
    private static final String WIFI_OFFLOAD_SERVICE_ON = "mediatek.intent.action.WFC_POWER_ON_MODEM";
    private static final String WIFI_OFFLOAD_SERVICE_ON_EXTRA = "mediatek:POWER_ON_MODEM";
    private static final int WIFI_ONLY_INIT = -1;
    private static final boolean WIFI_ONLY_MODE_OFF = false;
    private static final boolean WIFI_ONLY_MODE_ON = true;
    protected static SharedPreferences sIccidPreference;
    private static RadioManager sRadioManager;
    private boolean bIsInIpoShutdown;
    private boolean bIsQueueIpoPreboot;
    private boolean bIsQueueIpoShutdown;
    protected boolean mAirplaneMode;
    private AirplaneRequestHandler mAirplaneRequestHandler;
    protected int mBitmapForPhoneCount;
    private CommandsInterface[] mCi;
    private Context mContext;
    private ImsSwitchController mImsSwitchController;
    private int[] mInitializeWaitCounter;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            RadioManager.log("BroadcastReceiver: " + intent.getAction());
            if (intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED")) {
                RadioManager.this.onReceiveSimStateChangedIntent(intent);
                return;
            }
            if (intent.getAction().equals(RadioManager.ACTION_FORCE_SET_RADIO_POWER)) {
                RadioManager.this.onReceiveForceSetRadioPowerIntent(intent);
                return;
            }
            if (intent.getAction().equals(RadioManager.ACTION_WIFI_ONLY_MODE_CHANGED)) {
                RadioManager.this.onReceiveWifiOnlyModeStateChangedIntent(intent);
            } else if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                RadioManager.this.onReceiveWifiStateChangedIntent(intent);
            } else {
                if (!intent.getAction().equals(RadioManager.WIFI_OFFLOAD_SERVICE_ON)) {
                    return;
                }
                RadioManager.this.onReceiveWifiStateChangedIntent(intent);
            }
        }
    };
    protected boolean mIsEccCall;
    private boolean mIsWifiOn;
    private Runnable mNotifyMSimModeChangeRunnable;
    private Runnable[] mNotifySimModeChangeRunnable;
    protected int mPhoneCount;
    private Runnable[] mRadioPowerRunnable;
    protected int[] mSimInsertedStatus;
    private int mSimModeSetting;
    private boolean mWifiOnlyMode;
    protected static ConcurrentHashMap<IRadioPower, String> mNotifyRadioPowerChange = new ConcurrentHashMap<>();
    protected static String[] PROPERTY_ICCID_SIM = {"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
    protected static String[] PROPERTY_RADIO_OFF = {"ril.ipo.radiooff", "ril.ipo.radiooff.2"};

    private static int[] m563xa4bc86e3() {
        if (f40x22a12f3f != null) {
            return f40x22a12f3f;
        }
        int[] iArr = new int[TelephonyManager.MultiSimVariants.values().length];
        try {
            iArr[TelephonyManager.MultiSimVariants.DSDA.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.DSDS.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.TSTS.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[TelephonyManager.MultiSimVariants.UNKNOWN.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f40x22a12f3f = iArr;
        return iArr;
    }

    public static RadioManager init(Context context, int phoneCount, CommandsInterface[] ci) {
        RadioManager radioManager;
        synchronized (RadioManager.class) {
            if (sRadioManager == null) {
                sRadioManager = new RadioManager(context, phoneCount, ci);
            }
            radioManager = sRadioManager;
        }
        return radioManager;
    }

    public static RadioManager getInstance() {
        RadioManager radioManager;
        synchronized (RadioManager.class) {
            radioManager = sRadioManager;
        }
        return radioManager;
    }

    protected RadioManager(Context context, int phoneCount, CommandsInterface[] ci) {
        this.mAirplaneMode = false;
        this.mWifiOnlyMode = false;
        this.mIsWifiOn = false;
        this.mImsSwitchController = null;
        int airplaneMode = Settings.Global.getInt(context.getContentResolver(), "airplane_mode_on", 0);
        int wifionlyMode = ImsManager.getWfcMode(context);
        if (ImsManager.isWfcEnabledByPlatform(context)) {
            log("initial actual wifi state when wifi calling is on");
            WifiManager wiFiManager = (WifiManager) context.getSystemService("wifi");
            this.mIsWifiOn = wiFiManager.isWifiEnabled();
        }
        log("Initialize RadioManager under airplane mode:" + airplaneMode + " wifi only mode:" + wifionlyMode + " wifi mode: " + this.mIsWifiOn);
        this.mSimInsertedStatus = new int[phoneCount];
        for (int i = 0; i < phoneCount; i++) {
            this.mSimInsertedStatus[i] = -1;
        }
        this.mInitializeWaitCounter = new int[phoneCount];
        for (int i2 = 0; i2 < phoneCount; i2++) {
            this.mInitializeWaitCounter[i2] = 0;
        }
        this.mRadioPowerRunnable = new RadioPowerRunnable[phoneCount];
        for (int i3 = 0; i3 < phoneCount; i3++) {
            this.mRadioPowerRunnable[i3] = new RadioPowerRunnable(true, i3);
        }
        this.mNotifySimModeChangeRunnable = new SimModeChangeRunnable[phoneCount];
        for (int i4 = 0; i4 < phoneCount; i4++) {
            this.mNotifySimModeChangeRunnable[i4] = new SimModeChangeRunnable(true, i4);
        }
        this.mNotifyMSimModeChangeRunnable = new MSimModeChangeRunnable(3);
        this.mContext = context;
        this.mAirplaneMode = airplaneMode != 0;
        this.mWifiOnlyMode = wifionlyMode == 0;
        this.mCi = ci;
        this.mPhoneCount = phoneCount;
        this.mBitmapForPhoneCount = convertPhoneCountIntoBitmap(phoneCount);
        sIccidPreference = this.mContext.getSharedPreferences(PREF_CATEGORY_RADIO_STATUS, 0);
        this.mSimModeSetting = Settings.Global.getInt(context.getContentResolver(), "msim_mode_setting", this.mBitmapForPhoneCount);
        this.mImsSwitchController = new ImsSwitchController(this.mContext, this.mPhoneCount, this.mCi);
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            log("Not BSP Package, register intent!!!");
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SIM_STATE_CHANGED");
            filter.addAction(ACTION_FORCE_SET_RADIO_POWER);
            filter.addAction(ACTION_WIFI_ONLY_MODE_CHANGED);
            filter.addAction(WIFI_OFFLOAD_SERVICE_ON);
            this.mContext.registerReceiver(this.mIntentReceiver, filter);
            for (int i5 = 0; i5 < phoneCount; i5++) {
                Integer index = new Integer(i5);
                this.mCi[i5].registerForVirtualSimOn(this, 2, index);
                this.mCi[i5].registerForAvailable(this, 1, null);
            }
        }
        this.mAirplaneRequestHandler = new AirplaneRequestHandler(this.mContext, this.mPhoneCount);
    }

    private int convertPhoneCountIntoBitmap(int phoneCount) {
        int ret = 0;
        for (int i = 0; i < phoneCount; i++) {
            ret += 1 << i;
        }
        log("Convert phoneCount " + phoneCount + " into bitmap " + ret);
        return ret;
    }

    protected void onReceiveWifiStateChangedIntent(Intent intent) {
        int extraWifiState;
        if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
            log("Receiving WIFI_STATE_CHANGED_ACTION");
            extraWifiState = intent.getIntExtra("wifi_state", 4);
        } else if (!intent.getAction().equals(WIFI_OFFLOAD_SERVICE_ON)) {
            log("Wrong intent");
            return;
        } else {
            log("Receiving WIFI_OFFLOAD_SERVICE_ON");
            extraWifiState = intent.getBooleanExtra(WIFI_OFFLOAD_SERVICE_ON_EXTRA, false) ? 3 : 1;
        }
        log("airplaneMode: " + this.mAirplaneMode + " isFlightModePowerOffModem:" + isFlightModePowerOffModemConfigEnabled() + ", mIsWifiOn: " + this.mIsWifiOn);
        switch (extraWifiState) {
            case 1:
                log("WIFI_STATE_CHANGED disabled");
                this.mIsWifiOn = false;
                if (this.mAirplaneMode && isFlightModePowerOffModemConfigEnabled()) {
                    log("WIFI_STATE_CHANGED disabled, set modem off");
                    setSilentRebootPropertyForAllModem("1");
                    setModemPower(false, this.mBitmapForPhoneCount);
                    break;
                }
                break;
            case 2:
            default:
                log("default: WIFI_STATE_CHANGED extra" + extraWifiState);
                break;
            case 3:
                log("WIFI_STATE_CHANGED enabled");
                this.mIsWifiOn = true;
                if (this.mAirplaneMode && isFlightModePowerOffModemConfigEnabled()) {
                    log("WIFI_STATE_CHANGED enabled, set modem on");
                    setSilentRebootPropertyForAllModem("1");
                    setModemPower(true, this.mBitmapForPhoneCount);
                    break;
                }
                break;
        }
    }

    protected void onReceiveSimStateChangedIntent(Intent intent) {
        String simStatus = intent.getStringExtra("ss");
        int phoneId = intent.getIntExtra("phone", -1);
        if (!isValidPhoneId(phoneId)) {
            log("INTENT:Invalid phone id:" + phoneId + ", do nothing!");
            return;
        }
        log("INTENT:SIM_STATE_CHANGED: " + intent.getAction() + ", sim status: " + simStatus + ", phoneId: " + phoneId);
        if ("READY".equals(simStatus) || "LOCKED".equals(simStatus) || "LOADED".equals(simStatus)) {
            this.mSimInsertedStatus[phoneId] = 1;
            log("Phone[" + phoneId + "]: " + simStatusToString(1));
            String iccid = readIccIdUsingPhoneId(phoneId);
            if (STRING_NO_SIM_INSERTED.equals(iccid)) {
                log("Phone " + phoneId + ":SIM ready but ICCID not ready, do nothing");
                return;
            } else {
                if (this.mAirplaneMode) {
                    return;
                }
                log("Set Radio Power due to SIM_STATE_CHANGED, power: true, phoneId: " + phoneId);
                setRadioPower(true, phoneId);
                return;
            }
        }
        if (!"ABSENT".equals(simStatus)) {
            return;
        }
        this.mSimInsertedStatus[phoneId] = 0;
        log("Phone[" + phoneId + "]: " + simStatusToString(0));
        if (this.mAirplaneMode) {
            return;
        }
        log("Set Radio Power due to SIM_STATE_CHANGED, power: false, phoneId: " + phoneId);
        setRadioPower(false, phoneId);
    }

    public void onReceiveWifiOnlyModeStateChangedIntent(Intent intent) {
        boolean enabled = intent.getBooleanExtra("state", false);
        log("mReceiver: ACTION_WIFI_ONLY_MODE_CHANGED, enabled = " + enabled);
        if (enabled == this.mWifiOnlyMode) {
            log("enabled = " + enabled + ", mWifiOnlyMode = " + this.mWifiOnlyMode + "is not expected (the same)");
            return;
        }
        this.mWifiOnlyMode = enabled;
        if (this.mAirplaneMode) {
            return;
        }
        boolean radioPower = !enabled;
        for (int i = 0; i < this.mPhoneCount; i++) {
            setRadioPower(radioPower, i);
        }
    }

    private void onReceiveForceSetRadioPowerIntent(Intent intent) {
        int mode = intent.getIntExtra("mode", -1);
        log("force set radio power, mode: " + mode);
        if (mode == -1) {
            log("Invalid mode, MSIM_MODE intent has no extra value");
            return;
        }
        for (int phoneId = 0; phoneId < this.mPhoneCount; phoneId++) {
            boolean singlePhonePower = ((1 << phoneId) & mode) != 0;
            if (singlePhonePower) {
                forceSetRadioPower(true, phoneId);
            }
        }
    }

    protected boolean isValidPhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount();
    }

    protected String simStatusToString(int simStatus) {
        switch (simStatus) {
            case -1:
                return "SIM HAVE NOT INITIALIZED";
            case 0:
                return "NO SIM DETECTED";
            case 1:
                return "SIM DETECTED";
            default:
                return null;
        }
    }

    public void notifyAirplaneModeChange(boolean enabled) {
        if (!this.mAirplaneRequestHandler.allowSwitching()) {
            log("airplane mode switching, not allow switch now ");
            this.mAirplaneRequestHandler.pendingAirplaneModeRequest(enabled);
            return;
        }
        if (this.mAirplaneRequestHandler.waitForReady(enabled)) {
            log("airplane mode switching, wait for ready, not allow switch now");
            return;
        }
        if (enabled == this.mAirplaneMode) {
            log("enabled = " + enabled + ", mAirplaneMode = " + this.mAirplaneMode + "is not expected (the same)");
            return;
        }
        this.mAirplaneMode = enabled;
        log("Airplane mode changed:" + enabled);
        SystemProperties.set("persist.radio.airplane.mode.on", enabled ? "true" : "false");
        boolean currModemPower = true;
        if (isModemPowerOff(0)) {
            currModemPower = false;
        }
        int radioAction = -1;
        if (isFlightModePowerOffModemConfigEnabled() && !isUnderCryptKeeper()) {
            if (this.mIsWifiOn && currModemPower) {
                log("Airplane mode changed: turn on/off all radio due to WFC on");
                radioAction = 1;
            } else if (!this.mIsWifiOn && currModemPower && !this.mAirplaneMode) {
                log("Airplane mode changed: turn on all radio due to mode conflict & WFC off");
                radioAction = 1;
            } else if (!this.mIsWifiOn && !currModemPower && this.mAirplaneMode) {
                log("Airplane mode changed: skip & fake DONE due to mode conflict & WFC off");
                radioAction = 0;
            } else {
                log("Airplane mode changed: turn on/off all modem");
                radioAction = 2;
            }
        } else if (isMSimModeSupport()) {
            log("Airplane mode changed: turn on/off all radio");
            radioAction = 1;
        }
        log("Check OP01 PROPERTY_RIL_MD_OFF");
        if (!currModemPower && !this.mAirplaneMode && SystemProperties.get("persist.operator.optr").equalsIgnoreCase("OP01")) {
            log("PROPERTY_RIL_MD_OFF == true");
            radioAction = 2;
        }
        if (radioAction == 0) {
            Intent intent = new Intent(ACTION_AIRPLANE_CHANGE_DONE);
            intent.putExtra(EXTRA_AIRPLANE_MODE, false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            if (radioAction == 1) {
                boolean radioPower = !enabled;
                for (int i = 0; i < this.mPhoneCount; i++) {
                    setRadioPower(radioPower, i);
                }
                this.mAirplaneRequestHandler.monitorRadioChangeDone(radioPower);
                return;
            }
            if (radioAction != 2) {
                return;
            }
            boolean modemPower = !enabled;
            setSilentRebootPropertyForAllModem("1");
            setModemPower(modemPower, this.mBitmapForPhoneCount);
        }
    }

    protected boolean isUnderCryptKeeper() {
        if (SystemProperties.get("ro.crypto.type").equals("block") && SystemProperties.get("ro.crypto.state").equals("encrypted") && SystemProperties.get("vold.decrypt").equals("trigger_restart_min_framework")) {
            log("[Special Case] Under CryptKeeper, Not to turn on/off modem");
            return true;
        }
        log("[Special Case] Not Under CryptKeeper");
        return false;
    }

    public void setSilentRebootPropertyForAllModem(String isSilentReboot) {
        TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        switch (m563xa4bc86e3()[config.ordinal()]) {
            case 1:
                log("set eboot under DSDA");
                SystemProperties.set(PROPERTY_SILENT_REBOOT_MD1, isSilentReboot);
                SystemProperties.set(PROPERTY_SILENT_REBOOT_MD2, isSilentReboot);
                break;
            case 2:
                log("set eboot under DSDS");
                SystemProperties.set(PROPERTY_SILENT_REBOOT_MD1, isSilentReboot);
                break;
            case 3:
                log("set eboot under TSTS");
                SystemProperties.set(PROPERTY_SILENT_REBOOT_MD1, isSilentReboot);
                break;
            default:
                log("set eboot under SS");
                SystemProperties.set(PROPERTY_SILENT_REBOOT_MD1, isSilentReboot);
                break;
        }
        if (!SystemProperties.get(MTK_C2K_SUPPORT).equals("1")) {
            return;
        }
        log("C2K project, set cdma.ril.eboot to " + isSilentReboot);
        SystemProperties.set(PROPERTY_SILENT_REBOOT_CDMA, isSilentReboot);
    }

    public void notifyRadioAvailable(int phoneId) {
        log("Phone " + phoneId + " notifies radio available");
        log("RADIO_AVAILABLE: airplane mode: " + this.mAirplaneMode + " cryptkeeper: " + isUnderCryptKeeper() + " mIsWifiOn:" + this.mIsWifiOn);
        if (!this.mAirplaneMode || !isFlightModePowerOffModemConfigEnabled() || isUnderCryptKeeper() || this.mIsWifiOn) {
            return;
        }
        Boolean pendingAirplaneReq = this.mAirplaneRequestHandler.hasPendingAirplaneRequest();
        if (pendingAirplaneReq != null && !pendingAirplaneReq.booleanValue()) {
            log("notifyRadioAvailable skip since pendingRequest is not airplane mode");
        } else {
            log("Power off modem because boot up under airplane mode");
            setModemPower(false, 1 << phoneId);
        }
    }

    public void notifyIpoShutDown() {
        log("notify IPO shutdown!");
        this.bIsInIpoShutdown = true;
        this.bIsQueueIpoPreboot = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            log("mCi[" + i + "].getRadioState().isAvailable(): " + this.mCi[i].getRadioState().isAvailable());
            if (!this.mCi[i].getRadioState().isAvailable()) {
                this.bIsQueueIpoShutdown = true;
            }
        }
        setModemPower(false, this.mBitmapForPhoneCount);
    }

    public void notifyIpoPreBoot() {
        log("IPO preboot!");
        this.bIsInIpoShutdown = false;
        this.bIsQueueIpoShutdown = false;
        setSilentRebootPropertyForAllModem("0");
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            log("mCi[" + i + "].getRadioState().isAvailable(): " + this.mCi[i].getRadioState().isAvailable());
            if (!this.mCi[i].getRadioState().isAvailable()) {
                this.bIsQueueIpoPreboot = true;
            }
        }
        setModemPower(true, this.mBitmapForPhoneCount, false);
    }

    private void setModemPower(boolean power, int phoneBitMap, boolean monitor) {
        Message[] responses;
        log("Set Modem Power according to bitmap, Power:" + power + ", PhoneBitMap:" + phoneBitMap);
        TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        if (monitor) {
            responses = this.mAirplaneRequestHandler.monitorModemPowerChangeDone(power, phoneBitMap, findMainCapabilityPhoneId());
        } else {
            responses = new Message[this.mPhoneCount];
        }
        switch (m563xa4bc86e3()[config.ordinal()]) {
            case 1:
                for (int i = 0; i < this.mPhoneCount; i++) {
                    int phoneId = i;
                    if (((1 << i) & phoneBitMap) != 0) {
                        log("Set Modem Power under DSDA mode, Power:" + power + ", phoneId:" + phoneId);
                        this.mCi[phoneId].setModemPower(power, responses[phoneId]);
                        if (!power) {
                            resetSimInsertedStatus(phoneId);
                        }
                    }
                }
                break;
            case 2:
                int phoneId2 = findMainCapabilityPhoneId();
                log("Set Modem Power under DSDS mode, Power:" + power + ", phoneId:" + phoneId2);
                this.mCi[phoneId2].setModemPower(power, responses[phoneId2]);
                if (!power) {
                    for (int i2 = 0; i2 < this.mPhoneCount; i2++) {
                        resetSimInsertedStatus(i2);
                    }
                }
                break;
            case 3:
                int phoneId3 = findMainCapabilityPhoneId();
                log("Set Modem Power under TSTS mode, Power:" + power + ", phoneId:" + phoneId3);
                this.mCi[phoneId3].setModemPower(power, responses[phoneId3]);
                if (!power) {
                    for (int i3 = 0; i3 < this.mPhoneCount; i3++) {
                        resetSimInsertedStatus(i3);
                    }
                }
                break;
            default:
                int phoneId4 = PhoneFactory.getDefaultPhone().getPhoneId();
                log("Set Modem Power under SS mode:" + power + ", phoneId:" + phoneId4);
                this.mCi[phoneId4].setModemPower(power, responses[phoneId4]);
                break;
        }
    }

    public void setModemPower(boolean power, int phoneBitMap) {
        setModemPower(power, phoneBitMap, true);
    }

    protected int findMainCapabilityPhoneId() {
        int switchStatus = Integer.valueOf(SystemProperties.get("persist.radio.simswitch", "1")).intValue();
        int result = switchStatus - 1;
        if (result < 0 || result >= this.mPhoneCount) {
            return 0;
        }
        return result;
    }

    protected class RadioPowerRunnable implements Runnable {
        int retryPhoneId;
        boolean retryPower;

        public RadioPowerRunnable(boolean power, int phoneId) {
            this.retryPower = power;
            this.retryPhoneId = phoneId;
        }

        @Override
        public void run() {
            RadioManager.this.setRadioPower(this.retryPower, this.retryPhoneId);
        }
    }

    public void setRadioPower(boolean power, int phoneId) {
        log("setRadioPower, power=" + power + "  phoneId=" + phoneId);
        if (PhoneFactory.getPhone(phoneId) == null) {
            return;
        }
        if ((isFlightModePowerOffModemEnabled() || power) && this.mAirplaneMode) {
            log("Set Radio Power on under airplane mode, ignore");
            return;
        }
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (!cm.isNetworkSupported(0)) {
            log("wifi-only device, so return");
            return;
        }
        if (isModemPowerOff(phoneId)) {
            log("modem for phone " + phoneId + " off, do not set radio again");
            return;
        }
        removeCallbacks(this.mRadioPowerRunnable[phoneId]);
        if (!isIccIdReady(phoneId)) {
            log("RILD initialize not completed, wait for 200ms");
            this.mRadioPowerRunnable[phoneId] = new RadioPowerRunnable(power, phoneId);
            postDelayed(this.mRadioPowerRunnable[phoneId], 200L);
            return;
        }
        setSimInsertedStatus(phoneId);
        boolean radioPower = power;
        String iccId = readIccIdUsingPhoneId(phoneId);
        if (sIccidPreference.contains(iccId)) {
            log("Adjust radio to off because once manually turned off, iccid: " + iccId + " , phone: " + phoneId);
            radioPower = false;
        }
        if (this.mWifiOnlyMode && !this.mIsEccCall) {
            log("setradiopower but wifi only, turn off");
            radioPower = false;
        }
        boolean isCTACase = checkForCTACase();
        if (getSimInsertedStatus(phoneId) == 0) {
            if (isCTACase) {
                int capabilityPhoneId = findMainCapabilityPhoneId();
                log("No SIM inserted, force to turn on 3G/4G phone " + capabilityPhoneId + " radio if no any sim radio is enabled!");
                PhoneFactory.getPhone(capabilityPhoneId).setRadioPower(true);
                for (int i = 0; i < this.mPhoneCount; i++) {
                    Phone phone = PhoneFactory.getPhone(i);
                    if (phone != null && i != capabilityPhoneId && !this.mIsEccCall) {
                        phone.setRadioPower(false);
                    }
                }
                return;
            }
            if (this.mIsEccCall) {
                log("ECC call Radio Power, power: " + radioPower + ", phoneId: " + phoneId);
                PhoneFactory.getPhone(phoneId).setRadioPower(radioPower);
                return;
            } else {
                log("No SIM inserted, turn Radio off!");
                PhoneFactory.getPhone(phoneId).setRadioPower(false);
                return;
            }
        }
        log("Trigger set Radio Power, power: " + radioPower + ", phoneId: " + phoneId);
        refreshSimSetting(radioPower, phoneId);
        PhoneFactory.getPhone(phoneId).setRadioPower(radioPower);
    }

    protected int getSimInsertedStatus(int phoneId) {
        return this.mSimInsertedStatus[phoneId];
    }

    protected void setSimInsertedStatus(int phoneId) {
        String iccId = readIccIdUsingPhoneId(phoneId);
        if (STRING_NO_SIM_INSERTED.equals(iccId)) {
            this.mSimInsertedStatus[phoneId] = 0;
        } else {
            this.mSimInsertedStatus[phoneId] = 1;
        }
    }

    protected boolean isIccIdReady(int phoneId) {
        String iccId = readIccIdUsingPhoneId(phoneId);
        if (iccId == null || UsimPBMemInfo.STRING_NOT_SET.equals(iccId)) {
            return false;
        }
        return true;
    }

    protected String readIccIdUsingPhoneId(int phoneId) {
        String ret = SystemProperties.get(PROPERTY_ICCID_SIM[phoneId]);
        log("ICCID for phone " + phoneId + " is " + SubscriptionInfo.givePrintableIccid(ret));
        return ret;
    }

    protected boolean checkForCTACase() {
        boolean isCTACase = true;
        if (!this.mAirplaneMode && !this.mWifiOnlyMode) {
            for (int i = 0; i < this.mPhoneCount; i++) {
                log("Check For CTA case: mSimInsertedStatus[" + i + "]:" + this.mSimInsertedStatus[i]);
                if (this.mSimInsertedStatus[i] == 1 || this.mSimInsertedStatus[i] == -1) {
                    isCTACase = false;
                }
            }
        } else {
            isCTACase = false;
        }
        if (!isCTACase && !this.mIsEccCall) {
            turnOffCTARadioIfNecessary();
        }
        log("CTA case: " + isCTACase);
        return isCTACase;
    }

    private void turnOffCTARadioIfNecessary() {
        for (int i = 0; i < this.mPhoneCount; i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null && this.mSimInsertedStatus[i] == 0) {
                if (isModemPowerOff(i)) {
                    log("modem off, not to handle CTA");
                    return;
                } else {
                    log("turn off phone " + i + " radio because we are no longer in CTA mode");
                    phone.setRadioPower(false);
                }
            }
        }
    }

    protected void refreshSimSetting(boolean radioPower, int phoneId) {
        int simMode;
        int simMode2 = Settings.Global.getInt(this.mContext.getContentResolver(), "msim_mode_setting", this.mBitmapForPhoneCount);
        if (!radioPower) {
            simMode = simMode2 & (~(1 << phoneId));
        } else {
            simMode = simMode2 | (1 << phoneId);
        }
        if (simMode == simMode2) {
            return;
        }
        log("Refresh MSIM mode setting to " + simMode + " from " + simMode2);
        Settings.Global.putInt(this.mContext.getContentResolver(), "msim_mode_setting", simMode);
    }

    protected class ForceSetRadioPowerRunnable implements Runnable {
        int mRetryPhoneId;
        boolean mRetryPower;

        public ForceSetRadioPowerRunnable(boolean power, int phoneId) {
            this.mRetryPower = power;
            this.mRetryPhoneId = phoneId;
        }

        @Override
        public void run() {
            RadioManager.this.forceSetRadioPower(this.mRetryPower, this.mRetryPhoneId);
        }
    }

    public void forceSetRadioPower(boolean power, int phoneId) {
        log("force set radio power for phone" + phoneId + " ,power: " + power);
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return;
        }
        if (isFlightModePowerOffModemConfigEnabled() && this.mAirplaneMode) {
            log("Force Set Radio Power under airplane mode, ignore");
            return;
        }
        if (this.bIsInIpoShutdown) {
            log("Force Set Radio Power under ipo shutdown, ignore");
            return;
        }
        if (isModemPowerOff(phoneId) && this.mAirplaneMode) {
            log("Modem Power Off for phone " + phoneId + ", Power on modem first");
            setModemPower(true, 1 << phoneId);
        }
        if (!isIccIdReady(phoneId)) {
            log("force set radio power, read iccid not ready, wait for200ms");
            ForceSetRadioPowerRunnable forceSetRadioPowerRunnable = new ForceSetRadioPowerRunnable(power, phoneId);
            postDelayed(forceSetRadioPowerRunnable, 200L);
        } else {
            refreshIccIdPreference(power, readIccIdUsingPhoneId(phoneId));
            phone.setRadioPower(power);
        }
    }

    public void forceSetRadioPower(boolean power, int phoneId, boolean isEccOn) {
        log("force set radio power isEccOn: " + isEccOn);
        this.mIsEccCall = isEccOn;
        forceSetRadioPower(power, phoneId);
    }

    public void forceSetECCState(boolean isEccOn) {
        log("force set ECC State isEccOn: " + isEccOn);
        this.mIsEccCall = isEccOn;
    }

    private class SimModeChangeRunnable implements Runnable {
        int mPhoneId;
        boolean mPower;

        public SimModeChangeRunnable(boolean power, int phoneId) {
            this.mPower = power;
            this.mPhoneId = phoneId;
        }

        @Override
        public void run() {
            RadioManager.this.notifySimModeChange(this.mPower, this.mPhoneId);
        }
    }

    public void notifySimModeChange(boolean power, int phoneId) {
        log("SIM mode changed, power: " + power + ", phoneId" + phoneId);
        if (!isMSimModeSupport() || this.mAirplaneMode) {
            log("Airplane mode on or MSIM Mode option is closed, do nothing!");
            return;
        }
        removeCallbacks(this.mNotifySimModeChangeRunnable[phoneId]);
        if (!isIccIdReady(phoneId)) {
            log("sim mode read iccid not ready, wait for 200ms");
            this.mNotifySimModeChangeRunnable[phoneId] = new SimModeChangeRunnable(power, phoneId);
            postDelayed(this.mNotifySimModeChangeRunnable[phoneId], 200L);
        } else {
            if (STRING_NO_SIM_INSERTED.equals(readIccIdUsingPhoneId(phoneId))) {
                power = false;
                log("phoneId " + phoneId + " sim not insert, set  power  to false");
            }
            refreshIccIdPreference(power, readIccIdUsingPhoneId(phoneId));
            log("Set Radio Power due to SIM mode change, power: " + power + ", phoneId: " + phoneId);
            setPhoneRadioPower(power, phoneId);
        }
    }

    protected void setPhoneRadioPower(boolean power, int phoneId) {
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone == null) {
            return;
        }
        phone.setRadioPower(power);
    }

    protected class MSimModeChangeRunnable implements Runnable {
        int mRetryMode;

        public MSimModeChangeRunnable(int mode) {
            this.mRetryMode = mode;
        }

        @Override
        public void run() {
            RadioManager.this.notifyMSimModeChange(this.mRetryMode);
        }
    }

    public void notifyMSimModeChange(int mode) {
        log("MSIM mode changed, mode: " + mode);
        if (mode == -1) {
            log("Invalid mode, MSIM_MODE intent has no extra value");
            return;
        }
        if (!isMSimModeSupport() || this.mAirplaneMode) {
            log("Airplane mode on or MSIM Mode option is closed, do nothing!");
            return;
        }
        boolean iccIdReady = true;
        int phoneId = 0;
        while (true) {
            if (phoneId >= this.mPhoneCount) {
                break;
            }
            if (isIccIdReady(phoneId)) {
                phoneId++;
            } else {
                iccIdReady = false;
                break;
            }
        }
        removeCallbacks(this.mNotifyMSimModeChangeRunnable);
        if (!iccIdReady) {
            this.mNotifyMSimModeChangeRunnable = new MSimModeChangeRunnable(mode);
            postDelayed(this.mNotifyMSimModeChangeRunnable, 200L);
            return;
        }
        for (int phoneId2 = 0; phoneId2 < this.mPhoneCount; phoneId2++) {
            boolean singlePhonePower = ((1 << phoneId2) & mode) != 0;
            if (STRING_NO_SIM_INSERTED.equals(readIccIdUsingPhoneId(phoneId2))) {
                singlePhonePower = false;
                log("phoneId " + phoneId2 + " sim not insert, set  power  to false");
            }
            refreshIccIdPreference(singlePhonePower, readIccIdUsingPhoneId(phoneId2));
            log("Set Radio Power due to MSIM mode change, power: " + singlePhonePower + ", phoneId: " + phoneId2);
            setPhoneRadioPower(singlePhonePower, phoneId2);
        }
    }

    protected void refreshIccIdPreference(boolean power, String iccid) {
        log("refresh iccid preference");
        SharedPreferences.Editor editor = sIccidPreference.edit();
        if (!power && !STRING_NO_SIM_INSERTED.equals(iccid)) {
            putIccIdToPreference(editor, iccid);
        } else {
            removeIccIdFromPreference(editor, iccid);
        }
        editor.commit();
    }

    private void putIccIdToPreference(SharedPreferences.Editor editor, String iccid) {
        if (iccid == null) {
            return;
        }
        log("Add radio off SIM: " + iccid);
        editor.putInt(iccid, 0);
    }

    private void removeIccIdFromPreference(SharedPreferences.Editor editor, String iccid) {
        if (iccid == null) {
            return;
        }
        log("Remove radio off SIM: " + iccid);
        editor.remove(iccid);
    }

    public static void sendRequestBeforeSetRadioPower(boolean power, int phoneId) {
        log("Send request before EFUN, power:" + power + " phoneId:" + phoneId);
        notifyRadioPowerChange(power, phoneId);
    }

    public static boolean isPowerOnFeatureAllClosed() {
        if (!isFlightModePowerOffModemConfigEnabled() && !isRadioOffPowerOffModemEnabled() && !isMSimModeSupport()) {
            return true;
        }
        return false;
    }

    public static boolean isRadioOffPowerOffModemEnabled() {
        return SystemProperties.get("ro.mtk_radiooff_power_off_md").equals("1");
    }

    public static boolean isFlightModePowerOffModemConfigEnabled() {
        if (SystemProperties.get("ril.testmode").equals("1")) {
            return SystemProperties.get("ril.test.poweroffmd").equals("1");
        }
        String optr = SystemProperties.get("persist.operator.optr");
        boolean zEqualsIgnoreCase = optr != null ? optr.equalsIgnoreCase("op01") : false;
        boolean zEquals = (SystemProperties.get("gsm.sim.ril.testsim").equals("1") || SystemProperties.get("gsm.sim.ril.testsim.2").equals("1") || SystemProperties.get("gsm.sim.ril.testsim.3").equals("1")) ? true : SystemProperties.get("gsm.sim.ril.testsim.4").equals("1");
        if (zEqualsIgnoreCase && zEquals) {
            return true;
        }
        boolean zHasIccCard = !TelephonyManager.getDefault().hasIccCard(0) ? TelephonyManager.getDefault().hasIccCard(1) : true;
        log("isFlightModePowerOffModemEnabled: hasIccCard: " + zHasIccCard);
        if (!zEqualsIgnoreCase || zHasIccCard) {
            return SystemProperties.get("ro.mtk_flight_mode_power_off_md").equals("1");
        }
        return true;
    }

    public static boolean isFlightModePowerOffModemEnabled() {
        if (getInstance() != null) {
            return isFlightModePowerOffModemConfigEnabled() && !getInstance().mIsWifiOn;
        }
        log("Instance not exists, return config only");
        return isFlightModePowerOffModemConfigEnabled();
    }

    public static boolean isModemPowerOff(int phoneId) {
        return getInstance().isModemOff(phoneId);
    }

    public static boolean isMSimModeSupport() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            return false;
        }
        return true;
    }

    protected void resetSimInsertedStatus(int phoneId) {
        log("reset Sim InsertedStatus for Phone:" + phoneId);
        this.mSimInsertedStatus[phoneId] = -1;
    }

    @Override
    public void handleMessage(Message msg) {
        int phoneIdForMsg = getCiIndex(msg);
        log("handleMessage msg.what: " + eventIdtoString(msg.what));
        switch (msg.what) {
            case 1:
                if (this.bIsQueueIpoShutdown) {
                    log("bIsQueueIpoShutdown is true");
                    this.bIsQueueIpoShutdown = false;
                    log("IPO shut down retry!");
                    setModemPower(false, this.mBitmapForPhoneCount);
                }
                if (this.bIsQueueIpoPreboot) {
                    log("bIsQueueIpoPreboot is true");
                    this.bIsQueueIpoPreboot = false;
                    log("IPO reboot retry!");
                    setModemPower(true, this.mBitmapForPhoneCount);
                }
                break;
            case 2:
                forceSetRadioPower(true, phoneIdForMsg);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    private String eventIdtoString(int what) {
        switch (what) {
            case 1:
                return "EVENT_RADIO_AVAILABLE";
            case 2:
                return "EVENT_VIRTUAL_SIM_ON";
            default:
                return null;
        }
    }

    private int getCiIndex(Message msg) {
        Integer index = new Integer(0);
        if (msg != null) {
            if (msg.obj != null && (msg.obj instanceof Integer)) {
                index = (Integer) msg.obj;
            } else if (msg.obj != null && (msg.obj instanceof AsyncResult)) {
                AsyncResult ar = (AsyncResult) msg.obj;
                if (ar.userObj != null && (ar.userObj instanceof Integer)) {
                    index = (Integer) ar.userObj;
                }
            }
        }
        return index.intValue();
    }

    protected boolean isModemOff(int phoneId) {
        TelephonyManager.MultiSimVariants config = TelephonyManager.getDefault().getMultiSimConfiguration();
        switch (m563xa4bc86e3()[config.ordinal()]) {
            case 1:
                switch (phoneId) {
                    case 0:
                        if (SystemProperties.get("ril.ipo.radiooff").equals("0")) {
                        }
                        break;
                    case 1:
                        if (SystemProperties.get("ril.ipo.radiooff.2").equals("0")) {
                        }
                        break;
                }
                break;
            case 2:
                if (SystemProperties.get("ril.ipo.radiooff").equals("0")) {
                }
                break;
            case 3:
                if (SystemProperties.get("ril.ipo.radiooff").equals("0")) {
                }
                break;
            default:
                if (SystemProperties.get("ril.ipo.radiooff").equals("0")) {
                }
                break;
        }
        return false;
    }

    public static synchronized void registerForRadioPowerChange(String name, IRadioPower iRadioPower) {
        if (name == null) {
            name = REGISTRANTS_WITH_NO_NAME;
        }
        log(name + " registerForRadioPowerChange");
        mNotifyRadioPowerChange.put(iRadioPower, name);
    }

    public static synchronized void unregisterForRadioPowerChange(IRadioPower iRadioPower) {
        log(mNotifyRadioPowerChange.get(iRadioPower) + " unregisterForRadioPowerChange");
        mNotifyRadioPowerChange.remove(iRadioPower);
    }

    private static synchronized void notifyRadioPowerChange(boolean power, int phoneId) {
        for (Map.Entry<IRadioPower, String> e : mNotifyRadioPowerChange.entrySet()) {
            log("notifyRadioPowerChange: user:" + e.getValue());
            IRadioPower iRadioPower = e.getKey();
            iRadioPower.notifyRadioPowerChange(power, phoneId);
        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, "[RadioManager] " + s);
    }

    public boolean isAllowAirplaneModeChange() {
        return this.mAirplaneRequestHandler.allowSwitching();
    }

    public void forceAllowAirplaneModeChange(boolean forceSwitch) {
        this.mAirplaneRequestHandler.setForceSwitch(forceSwitch);
    }
}
