package com.android.internal.telephony;

import android.R;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telecom.VideoProfile;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.Log;
import com.android.ims.ImsException;
import com.android.ims.ImsManager;
import com.android.ims.ImsUtInterface;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.cdma.EriManager;
import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.gsm.SuppCrssNotification;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCallTracker;
import com.android.internal.telephony.imsphone.ImsPhoneMmiCode;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.CsimFileHandler;
import com.android.internal.telephony.uicc.IccCardProxy;
import com.android.internal.telephony.uicc.IccException;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccVmNotSupportedException;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.IsimUiccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.ISupplementaryServiceExt;
import com.mediatek.internal.telephony.ImsSwitchController;
import com.mediatek.internal.telephony.OperatorUtils;
import com.mediatek.internal.telephony.uicc.CsimPhbStorageInfo;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GsmCdmaPhone extends Phone {

    private static final int[] f7xfa7940f = null;

    private static final int[] f8comandroidinternaltelephonyDctConstants$StateSwitchesValues = null;
    public static final int CANCEL_ECM_TIMER = 1;
    private static final String CFB_KEY = "CFB";
    private static final String CFNRC_KEY = "CFNRC";
    private static final String CFNR_KEY = "CFNR";
    private static final String CFU_QUERY_ICCID_PROP = "persist.radio.cfu.iccid.";
    private static final int CFU_QUERY_MAX_COUNT = 60;
    private static final String CFU_QUERY_PROPERTY_NAME = "gsm.poweron.cfu.query.";
    private static final String CFU_QUERY_SIM_CHANGED_PROP = "persist.radio.cfu.change.";
    private static final boolean DBG = true;
    private static final int DEFAULT_ECM_EXIT_TIMER_VALUE = 300000;
    public static final String IMS_DEREG_OFF = "0";
    public static final String IMS_DEREG_ON = "1";
    public static final String IMS_DEREG_PROP = "gsm.radio.ss.imsdereg";
    private static final int INVALID_SYSTEM_SELECTION_CODE = -1;
    private static final String IS683A_FEATURE_CODE = "*228";
    private static final int IS683A_FEATURE_CODE_NUM_DIGITS = 4;
    private static final int IS683A_SYS_SEL_CODE_NUM_DIGITS = 2;
    private static final int IS683A_SYS_SEL_CODE_OFFSET = 4;
    private static final int IS683_CONST_1900MHZ_A_BLOCK = 2;
    private static final int IS683_CONST_1900MHZ_B_BLOCK = 3;
    private static final int IS683_CONST_1900MHZ_C_BLOCK = 4;
    private static final int IS683_CONST_1900MHZ_D_BLOCK = 5;
    private static final int IS683_CONST_1900MHZ_E_BLOCK = 6;
    private static final int IS683_CONST_1900MHZ_F_BLOCK = 7;
    private static final int IS683_CONST_800MHZ_A_BAND = 0;
    private static final int IS683_CONST_800MHZ_B_BAND = 1;
    public static final String LOG_TAG = "GsmCdmaPhone";
    public static final int MESSAGE_SET_CF = 1;
    private static final boolean MTK_IMS_SUPPORT;
    private static final boolean MTK_SVLTE_SUPPORT;
    public static final String PROPERTY_CDMA_HOME_OPERATOR_NUMERIC = "ro.cdma.home.operator.numeric";
    private static final String PROP_MTK_CDMA_LTE_MODE = "ro.boot.opt_c2k_lte_mode";
    public static final int RESTART_ECM_TIMER = 0;
    public static final int TBCW_NOT_OPTBCW = 1;
    public static final int TBCW_OPTBCW_NOT_VOLTE_USER = 3;
    public static final int TBCW_OPTBCW_VOLTE_USER = 2;
    public static final int TBCW_OPTBCW_WITH_CS = 4;
    public static final int TBCW_UNKNOWN = 0;
    private static final boolean VDBG = false;
    private static final String VM_NUMBER = "vm_number_key";
    private static final String VM_NUMBER_CDMA = "vm_number_key_cdma";
    private static final String VM_SIM_IMSI = "vm_sim_imsi_key";
    private static final int cfuQueryWaitTime = 1000;
    private static Pattern pOtaSpNumSchema;
    private boolean mBroadcastEmergencyCallStateChanges;
    private BroadcastReceiver mBroadcastReceiver;
    public GsmCdmaCallTracker mCT;
    private AsyncResult mCachedCrssn;
    private AsyncResult mCachedSsn;
    RegistrantList mCallRelatedSuppSvcRegistrants;
    private String mCarrierOtaSpNumSchema;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public int mCdmaSubscriptionSource;
    private int mCfuQueryRetryCount;
    private int mDeviceIdAbnormal;
    private Registrant mEcmExitRespRegistrant;
    private final RegistrantList mEcmTimerResetRegistrants;
    private final RegistrantList mEriFileLoadedRegistrants;
    public EriManager mEriManager;
    private String mEsn;
    private Runnable mExitEcmRunnable;
    private IccCardProxy mIccCardProxy;
    private IccPhoneBookInterfaceManager mIccPhoneBookIntManager;
    private IccSmsInterfaceManager mIccSmsInterfaceManager;
    private String mImei;
    private String mImeiSv;
    public boolean mIsNetworkInitiatedUssd;
    private boolean mIsPhoneInEcmState;
    private IsimUiccRecords mIsimUiccRecords;
    private String mMeid;
    private int mNewVoiceTech;
    private ArrayList<MmiCode> mPendingMMIs;
    private int mPrecisePhoneType;
    private boolean mResetModemOnRadioTechnologyChange;
    private int mRilVersion;
    SSRequestDecisionMaker mSSReqDecisionMaker;
    public ServiceStateTracker mSST;
    private SIMRecords mSimRecords;
    private RegistrantList mSsnRegistrants;
    ISupplementaryServiceExt mSupplementaryServiceExt;
    private int mTbcwMode;
    private String mVmNumber;
    private PowerManager.WakeLock mWakeLock;
    private boolean needQueryCfu;

    private static int[] m19xd0f730eb() {
        if (f7xfa7940f != null) {
            return f7xfa7940f;
        }
        int[] iArr = new int[DctConstants.Activity.values().length];
        try {
            iArr[DctConstants.Activity.DATAIN.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DctConstants.Activity.DATAINANDOUT.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DctConstants.Activity.DATAOUT.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DctConstants.Activity.DORMANT.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DctConstants.Activity.NONE.ordinal()] = 12;
        } catch (NoSuchFieldError e5) {
        }
        f7xfa7940f = iArr;
        return iArr;
    }

    private static int[] m20xf0fbc33d() {
        if (f8comandroidinternaltelephonyDctConstants$StateSwitchesValues != null) {
            return f8comandroidinternaltelephonyDctConstants$StateSwitchesValues;
        }
        int[] iArr = new int[DctConstants.State.values().length];
        try {
            iArr[DctConstants.State.CONNECTED.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[DctConstants.State.CONNECTING.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[DctConstants.State.DISCONNECTING.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[DctConstants.State.FAILED.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[DctConstants.State.IDLE.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[DctConstants.State.RETRYING.ordinal()] = 6;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[DctConstants.State.SCANNING.ordinal()] = 7;
        } catch (NoSuchFieldError e7) {
        }
        f8comandroidinternaltelephonyDctConstants$StateSwitchesValues = iArr;
        return iArr;
    }

    static {
        MTK_SVLTE_SUPPORT = SystemProperties.getInt(PROP_MTK_CDMA_LTE_MODE, 0) == 1;
        MTK_IMS_SUPPORT = SystemProperties.get("persist.mtk_ims_support").equals("1");
        pOtaSpNumSchema = Pattern.compile("[,\\s]+");
    }

    private static class Cfu {
        final Message mOnComplete;
        final String mSetCfNumber;

        Cfu(String cfNumber, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mOnComplete = onComplete;
        }
    }

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, int phoneId, int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory) {
        this(context, ci, notifier, VDBG, phoneId, precisePhoneType, telephonyComponentFactory);
    }

    public GsmCdmaPhone(Context context, CommandsInterface ci, PhoneNotifier notifier, boolean unitTestMode, int phoneId, int precisePhoneType, TelephonyComponentFactory telephonyComponentFactory) {
        super(precisePhoneType == 1 ? "GSM" : "CDMA", notifier, context, ci, unitTestMode, phoneId, telephonyComponentFactory);
        this.mSsnRegistrants = new RegistrantList();
        this.mCdmaSubscriptionSource = -1;
        this.mEriFileLoadedRegistrants = new RegistrantList();
        this.needQueryCfu = true;
        this.mCfuQueryRetryCount = 0;
        this.mTbcwMode = 0;
        this.mIsNetworkInitiatedUssd = VDBG;
        this.mExitEcmRunnable = new Runnable() {
            @Override
            public void run() {
                GsmCdmaPhone.this.exitEmergencyCallbackMode();
            }
        };
        this.mPendingMMIs = new ArrayList<>();
        this.mEcmTimerResetRegistrants = new RegistrantList();
        this.mDeviceIdAbnormal = 0;
        this.mResetModemOnRadioTechnologyChange = VDBG;
        this.mBroadcastEmergencyCallStateChanges = VDBG;
        this.mCallRelatedSuppSvcRegistrants = new RegistrantList();
        this.mCachedSsn = null;
        this.mCachedCrssn = null;
        this.mNewVoiceTech = -1;
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                String action = intent.getAction();
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    GsmCdmaPhone.this.sendMessage(GsmCdmaPhone.this.obtainMessage(43));
                    return;
                }
                if (!"android.intent.action.ACTION_SUBINFO_RECORD_UPDATED".equals(action)) {
                    if (!action.equals("com.android.ims.IMS_STATE_CHANGED")) {
                        if (action.equals("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE")) {
                            Rlog.d(GsmCdmaPhone.LOG_TAG, "set needQueryCfu to true.");
                            GsmCdmaPhone.this.needQueryCfu = true;
                            return;
                        } else {
                            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                                boolean bAirplaneModeOn = intent.getBooleanExtra("state", GsmCdmaPhone.VDBG);
                                Rlog.d(GsmCdmaPhone.LOG_TAG, "ACTION_AIRPLANE_MODE_CHANGED, bAirplaneModeOn = " + bAirplaneModeOn);
                                if (bAirplaneModeOn) {
                                    Rlog.d(GsmCdmaPhone.LOG_TAG, "Set needQueryCfu true.");
                                    GsmCdmaPhone.this.needQueryCfu = true;
                                    return;
                                }
                                return;
                            }
                            return;
                        }
                    }
                    int reg = intent.getIntExtra("android:regState", -1);
                    int slotId = intent.getIntExtra("android:phone_id", -1);
                    Rlog.d(GsmCdmaPhone.LOG_TAG, "onReceive ACTION_IMS_STATE_CHANGED: reg=" + reg + ", SimID=" + slotId);
                    if (slotId == GsmCdmaPhone.this.getPhoneId() && reg == 0) {
                        if (GsmCdmaPhone.this.isOpTbcwWithCS(GsmCdmaPhone.this.getPhoneId())) {
                            GsmCdmaPhone.this.setTbcwMode(4);
                            GsmCdmaPhone.this.setTbcwToEnabledOnIfDisabled();
                        } else {
                            GsmCdmaPhone.this.setTbcwMode(2);
                            GsmCdmaPhone.this.setTbcwToEnabledOnIfDisabled();
                        }
                        Rlog.d(GsmCdmaPhone.LOG_TAG, "needQueryCfu for IMS CFU status.");
                        GsmCdmaPhone.this.needQueryCfu = true;
                        Message msgQueryCfu = GsmCdmaPhone.this.obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE);
                        GsmCdmaPhone.this.sendMessage(msgQueryCfu);
                        return;
                    }
                    return;
                }
                SubscriptionManager subMgr = SubscriptionManager.from(GsmCdmaPhone.this.mContext);
                SubscriptionInfo mySubInfo = subMgr != null ? subMgr.getActiveSubscriptionInfo(GsmCdmaPhone.this.getSubId()) : null;
                String mySettingName = GsmCdmaPhone.CFU_QUERY_ICCID_PROP + GsmCdmaPhone.this.getPhoneId();
                String oldIccId = SystemProperties.get(mySettingName, UsimPBMemInfo.STRING_NOT_SET);
                String defaultQueryCfuMode = "0";
                if (GsmCdmaPhone.this.mSupplementaryServiceExt != null) {
                    defaultQueryCfuMode = GsmCdmaPhone.this.mSupplementaryServiceExt.getOpDefaultQueryCfuMode();
                    Rlog.d(GsmCdmaPhone.LOG_TAG, "defaultQueryCfuMode = " + defaultQueryCfuMode);
                }
                String cfuSetting = SystemProperties.get("persist.radio.cfu.querytype", defaultQueryCfuMode);
                if (mySubInfo != null && mySubInfo.getIccId() != null) {
                    if (!mySubInfo.getIccId().equals(oldIccId)) {
                        Rlog.w(GsmCdmaPhone.LOG_TAG, " mySubId " + GsmCdmaPhone.this.getSubId() + " mySettingName " + mySettingName + " old iccid : " + oldIccId + " new iccid : " + mySubInfo.getIccId());
                        SystemProperties.set(mySettingName, mySubInfo.getIccId());
                        String isChanged = GsmCdmaPhone.CFU_QUERY_SIM_CHANGED_PROP + GsmCdmaPhone.this.getPhoneId();
                        SystemProperties.set(isChanged, "1");
                        GsmCdmaPhone.this.needQueryCfu = true;
                        GsmCdmaPhone.this.setCsFallbackStatus(0);
                        GsmCdmaPhone.this.setTbcwMode(0);
                        GsmCdmaPhone.this.setSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
                        GsmCdmaPhone.this.saveTimeSlot(null);
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(GsmCdmaPhone.this.getContext());
                        int clirSetting = sp.getInt(Phone.CLIR_KEY + GsmCdmaPhone.this.getPhoneId(), -1);
                        if (clirSetting != -1) {
                            SharedPreferences.Editor editor = sp.edit();
                            editor.remove(Phone.CLIR_KEY + GsmCdmaPhone.this.getPhoneId());
                            if (!editor.commit()) {
                                Rlog.e(GsmCdmaPhone.LOG_TAG, "failed to commit the removal of CLIR preference");
                            }
                        }
                        GsmCdmaPhone.this.setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                        if (GsmCdmaPhone.this.mSST != null && GsmCdmaPhone.this.mSST.mSS != null && GsmCdmaPhone.this.mSST.mSS.getState() == 0) {
                            Rlog.w(GsmCdmaPhone.LOG_TAG, "Send EVENT_QUERY_CFU");
                            Message msgQueryCfu2 = GsmCdmaPhone.this.obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE);
                            GsmCdmaPhone.this.sendMessage(msgQueryCfu2);
                        }
                    } else if (cfuSetting.equals(Phone.ACT_TYPE_UTRAN)) {
                        Rlog.d(GsmCdmaPhone.LOG_TAG, "Always query CFU.");
                        if (GsmCdmaPhone.this.mSST != null && GsmCdmaPhone.this.mSST.mSS != null && GsmCdmaPhone.this.mSST.mSS.getState() == 0) {
                            GsmCdmaPhone.this.needQueryCfu = true;
                            Message msgQueryCfu3 = GsmCdmaPhone.this.obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE);
                            GsmCdmaPhone.this.sendMessage(msgQueryCfu3);
                        }
                    }
                }
                Rlog.d(GsmCdmaPhone.LOG_TAG, "onReceive(): ACTION_SUBINFO_RECORD_UPDATED: mTbcwMode = " + GsmCdmaPhone.this.mTbcwMode);
                if (GsmCdmaPhone.this.mTbcwMode == 0 && GsmCdmaPhone.this.isIccCardMncMccAvailable(GsmCdmaPhone.this.getPhoneId()) && GsmCdmaPhone.this.isOpTbcwWithCS(GsmCdmaPhone.this.getPhoneId())) {
                    GsmCdmaPhone.this.setTbcwMode(4);
                    GsmCdmaPhone.this.setTbcwToEnabledOnIfDisabled();
                }
            }
        };
        this.mPrecisePhoneType = precisePhoneType;
        initOnce(ci);
        initRatSpecific(precisePhoneType);
        this.mSST = this.mTelephonyComponentFactory.makeServiceStateTracker(this, this.mCi);
        this.mDcTracker = this.mTelephonyComponentFactory.makeDcTracker(this);
        this.mSST.registerForNetworkAttached(this, 19, null);
        logd("GsmCdmaPhone: constructor: sub = " + this.mPhoneId);
    }

    private void initOnce(CommandsInterface ci) {
        if (ci instanceof SimulatedRadioControl) {
            this.mSimulatedRadioControl = (SimulatedRadioControl) ci;
        }
        this.mCT = this.mTelephonyComponentFactory.makeGsmCdmaCallTracker(this);
        this.mIccPhoneBookIntManager = this.mTelephonyComponentFactory.makeIccPhoneBookInterfaceManager(this);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, LOG_TAG);
        this.mIccSmsInterfaceManager = this.mTelephonyComponentFactory.makeIccSmsInterfaceManager(this);
        this.mIccCardProxy = this.mTelephonyComponentFactory.makeIccCardProxy(this.mContext, this.mCi, this.mPhoneId);
        this.mSSReqDecisionMaker = new SSRequestDecisionMaker(this.mContext, this);
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                this.mSupplementaryServiceExt = (ISupplementaryServiceExt) MPlugin.createInstance(ISupplementaryServiceExt.class.getName(), this.mContext);
                if (this.mSupplementaryServiceExt != null) {
                    this.mSupplementaryServiceExt.registerReceiver(this.mContext, this.mPhoneId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        this.mCi.registerForAvailable(this, 1, null);
        this.mCi.registerForOffOrNotAvailable(this, 8, null);
        this.mCi.registerForOn(this, 5, null);
        this.mCi.setOnSuppServiceNotification(this, 2, null);
        this.mCi.setOnUSSD(this, 7, null);
        this.mCi.setOnSs(this, 36, null);
        this.mCT.registerForVoiceCallIncomingIndication(this, 1002, null);
        this.mCi.setOnCallRelatedSuppSvc(this, 1003, null);
        this.mCdmaSSM = this.mTelephonyComponentFactory.getCdmaSubscriptionSourceManagerInstance(this.mContext, this.mCi, this, 27, null);
        this.mEriManager = this.mTelephonyComponentFactory.makeEriManager(this, this.mContext, 0);
        this.mCi.setEmergencyCallbackMode(this, 25, null);
        this.mCi.registerForExitEmergencyCallbackMode(this, 26, null);
        this.mCarrierOtaSpNumSchema = TelephonyManager.from(this.mContext).getOtaSpNumberSchemaForPhone(getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
        this.mResetModemOnRadioTechnologyChange = SystemProperties.getBoolean("persist.radio.reset_on_switch", VDBG);
        this.mCi.registerForRilConnected(this, 41, null);
        this.mCi.registerForVoiceRadioTechChanged(this, 39, null);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        filter.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter.addAction("com.android.ims.IMS_STATE_CHANGED");
        filter.addAction("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private void initRatSpecific(int precisePhoneType) {
        this.mPendingMMIs.clear();
        this.mVmCount = 0;
        this.mEsn = null;
        this.mMeid = null;
        this.mPrecisePhoneType = precisePhoneType;
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        if (isPhoneTypeGsm()) {
            this.mCi.setPhoneType(1);
            tm.setPhoneType(getPhoneId(), 1);
            this.mIccCardProxy.setVoiceRadioTech(3);
            return;
        }
        this.mCdmaSubscriptionSource = -1;
        String inEcm = getSystemProperty("ril.cdma.inecmmode", "false");
        this.mIsPhoneInEcmState = inEcm.equals("true");
        if (this.mIsPhoneInEcmState) {
            this.mCi.exitEmergencyCallbackMode(obtainMessage(26));
        }
        this.mCi.setPhoneType(2);
        tm.setPhoneType(getPhoneId(), 2);
        this.mIccCardProxy.setVoiceRadioTech(6);
        String operatorAlpha = SystemProperties.get("ro.cdma.home.operator.alpha");
        String operatorNumeric = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
        logd("init: operatorAlpha='" + operatorAlpha + "' operatorNumeric='" + operatorNumeric + "'");
        if (this.mUiccController.getUiccCardApplication(this.mPhoneId, 1) == null || isPhoneTypeCdmaLte()) {
            if (!TextUtils.isEmpty(operatorAlpha)) {
                logd("init: set 'gsm.sim.operator.alpha' to operator='" + operatorAlpha + "'");
                tm.setSimOperatorNameForPhone(this.mPhoneId, operatorAlpha);
            }
            if (!TextUtils.isEmpty(operatorNumeric)) {
                logd("init: set 'gsm.sim.operator.numeric' to operator='" + operatorNumeric + "'");
                logd("update icc_operator_numeric=" + operatorNumeric);
                tm.setSimOperatorNumericForPhone(this.mPhoneId, operatorNumeric);
                SubscriptionController.getInstance().setMccMnc(operatorNumeric, getSubId());
                setIsoCountryProperty(operatorNumeric);
                logd("update mccmnc=" + operatorNumeric);
                MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, VDBG);
            }
        }
        updateCurrentCarrierInProvider(operatorNumeric);
    }

    private void setIsoCountryProperty(String operatorNumeric) {
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        if (TextUtils.isEmpty(operatorNumeric)) {
            logd("setIsoCountryProperty: clear 'gsm.sim.operator.iso-country'");
            tm.setSimCountryIsoForPhone(this.mPhoneId, UsimPBMemInfo.STRING_NOT_SET);
            return;
        }
        String iso = UsimPBMemInfo.STRING_NOT_SET;
        try {
            iso = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
        } catch (NumberFormatException ex) {
            Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", ex);
        } catch (StringIndexOutOfBoundsException ex2) {
            Rlog.e(LOG_TAG, "setIsoCountryProperty: countryCodeForMcc error", ex2);
        }
        logd("setIsoCountryProperty: set 'gsm.sim.operator.iso-country' to iso=" + iso);
        tm.setSimCountryIsoForPhone(this.mPhoneId, iso);
    }

    public boolean isPhoneTypeGsm() {
        if (this.mPrecisePhoneType == 1) {
            return true;
        }
        return VDBG;
    }

    public boolean isPhoneTypeCdma() {
        if (this.mPrecisePhoneType == 2) {
            return true;
        }
        return VDBG;
    }

    public boolean isPhoneTypeCdmaLte() {
        if (this.mPrecisePhoneType == 6) {
            return true;
        }
        return VDBG;
    }

    private void switchPhoneType(int precisePhoneType) {
        removeCallbacks(this.mExitEcmRunnable);
        initRatSpecific(precisePhoneType);
        this.mSST.updatePhoneType();
        setPhoneName(precisePhoneType == 1 ? "GSM" : "CDMA");
        onUpdateIccAvailability();
        this.mCT.updatePhoneType();
        CommandsInterface.RadioState radioState = this.mCi.getRadioState();
        if (radioState.isAvailable()) {
            handleRadioAvailable();
            if (radioState.isOn()) {
                handleRadioOn();
            }
        }
        if (radioState.isAvailable() && radioState.isOn()) {
            return;
        }
        handleRadioOffOrNotAvailable();
    }

    protected void finalize() {
        logd("GsmCdmaPhone finalized");
        if (!this.mWakeLock.isHeld()) {
            return;
        }
        Rlog.e(LOG_TAG, "UNEXPECTED; mWakeLock is held when finalizing.");
        this.mWakeLock.release();
    }

    @Override
    public ServiceState getServiceState() {
        Phone phone;
        if ((this.mSST == null || (this.mSST.mSS.getState() != 0 && this.mSST.mSS.getDataRegState() == 0)) && (phone = this.mImsPhone) != null) {
            return ServiceState.mergeServiceStates(this.mSST == null ? new ServiceState() : this.mSST.mSS, phone.getServiceState());
        }
        if (this.mSST != null) {
            return this.mSST.mSS;
        }
        return new ServiceState();
    }

    @Override
    public CellLocation getCellLocation() {
        if (isPhoneTypeGsm()) {
            return this.mSST.getCellLocation();
        }
        CdmaCellLocation loc = (CdmaCellLocation) this.mSST.mCellLoc;
        int mode = Settings.Secure.getInt(getContext().getContentResolver(), "location_mode", 0);
        if (mode == 0) {
            CdmaCellLocation privateLoc = new CdmaCellLocation();
            privateLoc.setCellLocationData(loc.getBaseStationId(), Integer.MAX_VALUE, Integer.MAX_VALUE, loc.getSystemId(), loc.getNetworkId());
            return privateLoc;
        }
        return loc;
    }

    @Override
    public PhoneConstants.State getState() {
        PhoneConstants.State imsState;
        if (this.mImsPhone != null && (imsState = this.mImsPhone.getState()) != PhoneConstants.State.IDLE) {
            return imsState;
        }
        return this.mCT.mState;
    }

    @Override
    public int getPhoneType() {
        return this.mPrecisePhoneType == 1 ? 1 : 2;
    }

    @Override
    public ServiceStateTracker getServiceStateTracker() {
        return this.mSST;
    }

    @Override
    public CallTracker getCallTracker() {
        return this.mCT;
    }

    @Override
    public void updateVoiceMail() {
        if (isPhoneTypeGsm()) {
            int countVoiceMessages = 0;
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                countVoiceMessages = r.getVoiceMessageCount();
            }
            int countVoiceMessagesStored = getStoredVoiceMessageCount();
            if (countVoiceMessages == -1 && countVoiceMessagesStored != 0) {
                countVoiceMessages = countVoiceMessagesStored;
            }
            logd("updateVoiceMail countVoiceMessages = " + countVoiceMessages + " subId " + getSubId());
            setVoiceMessageCount(countVoiceMessages);
            return;
        }
        setVoiceMessageCount(getStoredVoiceMessageCount());
    }

    @Override
    public List<? extends MmiCode> getPendingMmiCodes() {
        Rlog.d(LOG_TAG, "getPendingMmiCodes");
        dumpPendingMmi();
        ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
        ArrayList<MmiCode> imsphonePendingMMIs = new ArrayList<>();
        if (imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            for (ImsPhoneMmiCode mmi : imsPhone.getPendingMmiCodes()) {
                imsphonePendingMMIs.add(mmi);
            }
        }
        ArrayList<MmiCode> allPendingMMIs = new ArrayList<>(this.mPendingMMIs);
        allPendingMMIs.addAll(imsphonePendingMMIs);
        Rlog.d(LOG_TAG, "allPendingMMIs.size() = " + allPendingMMIs.size());
        int s = allPendingMMIs.size();
        for (int i = 0; i < s; i++) {
            Rlog.d(LOG_TAG, "dump allPendingMMIs: " + allPendingMMIs.get(i));
        }
        return allPendingMMIs;
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState(String apnType) {
        PhoneConstants.DataState ret = PhoneConstants.DataState.DISCONNECTED;
        if (this.mSST == null) {
            ret = PhoneConstants.DataState.DISCONNECTED;
        } else if (this.mSST.getCurrentDataConnectionState() != 0 && (isPhoneTypeCdma() || (isPhoneTypeGsm() && !apnType.equals("emergency")))) {
            logd("getDataConnectionState: dataConnectionState is not in service");
            if (MTK_IMS_SUPPORT && apnType.equals(ImsSwitchController.IMS_SERVICE)) {
                switch (m20xf0fbc33d()[this.mDcTracker.getState(apnType).ordinal()]) {
                    case 1:
                        ret = PhoneConstants.DataState.CONNECTED;
                        break;
                    case 2:
                    case 7:
                        ret = PhoneConstants.DataState.CONNECTING;
                        break;
                    case 3:
                    case 4:
                    case 5:
                    default:
                        ret = PhoneConstants.DataState.DISCONNECTED;
                        break;
                    case 6:
                        logd("getDataConnectionState: apnType: " + apnType + " is in retrying state!! return connecting state");
                        ret = PhoneConstants.DataState.CONNECTING;
                        break;
                }
            } else {
                ret = PhoneConstants.DataState.DISCONNECTED;
            }
        } else {
            switch (m20xf0fbc33d()[this.mDcTracker.getState(apnType).ordinal()]) {
                case 1:
                case 3:
                    if (this.mCT.mState != PhoneConstants.State.IDLE && !this.mSST.isConcurrentVoiceAndDataAllowed()) {
                        ret = PhoneConstants.DataState.SUSPENDED;
                    } else {
                        ret = PhoneConstants.DataState.CONNECTED;
                    }
                    int phoneCount = TelephonyManager.getDefault().getPhoneCount();
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        int i = 0;
                        while (true) {
                            if (i < phoneCount) {
                                Phone pf = PhoneFactory.getPhone(i);
                                if (pf == null || i == getPhoneId() || pf.getState() == PhoneConstants.State.IDLE) {
                                    i++;
                                } else {
                                    logd("getDataConnectionState: Phone[" + getPhoneId() + "] Phone" + i + " is in call.");
                                    if (MTK_SVLTE_SUPPORT) {
                                        int phoneType = pf.getPhoneType();
                                        int rilRat = getServiceState().getRilDataRadioTechnology();
                                        logd("getDataConnectionState: SVLTE, phoneType: " + phoneType + " rilRat: " + rilRat);
                                        if (phoneType == 1 && ServiceState.isGsm(rilRat)) {
                                            ret = PhoneConstants.DataState.SUSPENDED;
                                        }
                                    } else {
                                        logd("getDataConnectionState: set Data state as SUSPENDED");
                                        ret = PhoneConstants.DataState.SUSPENDED;
                                    }
                                }
                            }
                        }
                    }
                    if (ret == PhoneConstants.DataState.CONNECTED && apnType == "default" && this.mDcTracker.getState(apnType) == DctConstants.State.DISCONNECTING && !this.mDcTracker.getDataEnabled()) {
                        logd("getDataConnectionState: Connected but default data is not open.");
                        ret = PhoneConstants.DataState.DISCONNECTED;
                    }
                    break;
                case 2:
                case 7:
                    ret = PhoneConstants.DataState.CONNECTING;
                    break;
                case 4:
                case 5:
                case 6:
                    ret = PhoneConstants.DataState.DISCONNECTED;
                    break;
            }
        }
        logd("getDataConnectionState apnType=" + apnType + " ret=" + ret);
        return ret;
    }

    @Override
    public PhoneInternalInterface.DataActivityState getDataActivityState() {
        PhoneInternalInterface.DataActivityState ret = PhoneInternalInterface.DataActivityState.NONE;
        if (this.mSST.getCurrentDataConnectionState() == 0) {
            switch (m19xd0f730eb()[this.mDcTracker.getActivity().ordinal()]) {
                case 1:
                    PhoneInternalInterface.DataActivityState ret2 = PhoneInternalInterface.DataActivityState.DATAIN;
                    return ret2;
                case 2:
                    PhoneInternalInterface.DataActivityState ret3 = PhoneInternalInterface.DataActivityState.DATAINANDOUT;
                    return ret3;
                case 3:
                    PhoneInternalInterface.DataActivityState ret4 = PhoneInternalInterface.DataActivityState.DATAOUT;
                    return ret4;
                case 4:
                    PhoneInternalInterface.DataActivityState ret5 = PhoneInternalInterface.DataActivityState.DORMANT;
                    return ret5;
                default:
                    PhoneInternalInterface.DataActivityState ret6 = PhoneInternalInterface.DataActivityState.NONE;
                    return ret6;
            }
        }
        return ret;
    }

    public void notifyPhoneStateChanged() {
        this.mNotifier.notifyPhoneState(this);
    }

    public void notifyPreciseCallStateChanged() {
        super.notifyPreciseCallStateChangedP();
    }

    public void notifyNewRingingConnection(Connection c) {
        super.notifyNewRingingConnectionP(c);
    }

    public void notifyDisconnect(Connection cn) {
        this.mDisconnectRegistrants.notifyResult(cn);
        this.mNotifier.notifyDisconnectCause(cn.getDisconnectCause(), cn.getPreciseDisconnectCause());
    }

    public void notifyUnknownConnection(Connection cn) {
        super.notifyUnknownConnectionP(cn);
    }

    @Override
    public boolean isInEmergencyCall() {
        if (isPhoneTypeGsm()) {
            return VDBG;
        }
        return this.mCT.isInEmergencyCall();
    }

    @Override
    protected void setIsInEmergencyCall() {
        if (isPhoneTypeGsm()) {
            return;
        }
        this.mCT.setIsInEmergencyCall();
    }

    @Override
    public boolean isInEcm() {
        if (isPhoneTypeGsm()) {
            return VDBG;
        }
        return this.mIsPhoneInEcmState;
    }

    @Override
    public void queryPhbStorageInfo(int type, Message response) {
        if (isPhoneTypeGsm()) {
            this.mCi.queryPhbStorageInfo(type, response);
            return;
        }
        IccFileHandler fh = getIccFileHandler();
        if (fh != null && (fh instanceof CsimFileHandler)) {
            CsimPhbStorageInfo.checkPhbRecordInfo(response);
        } else {
            this.mCi.queryPhbStorageInfo(type, response);
        }
        Rlog.d(LOG_TAG, "queryPhbStorageInfo IccFileHandler" + fh);
    }

    private void sendEmergencyCallbackModeChange() {
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        intent.putExtra("phoneinECMState", this.mIsPhoneInEcmState);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        logd("sendEmergencyCallbackModeChange");
    }

    @Override
    public void sendEmergencyCallStateChange(boolean callActive) {
        if (!this.mBroadcastEmergencyCallStateChanges) {
            return;
        }
        Intent intent = new Intent("android.intent.action.EMERGENCY_CALL_STATE_CHANGED");
        intent.putExtra("phoneInEmergencyCall", callActive);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, getPhoneId());
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
        Rlog.d(LOG_TAG, "sendEmergencyCallStateChange");
    }

    @Override
    public void setBroadcastEmergencyCallStateChanges(boolean broadcast) {
        this.mBroadcastEmergencyCallStateChanges = broadcast;
    }

    public void notifySuppServiceFailed(PhoneInternalInterface.SuppService code) {
        this.mSuppServiceFailedRegistrants.notifyResult(code);
    }

    public void notifyServiceStateChanged(ServiceState ss) {
        super.notifyServiceStateChangedP(ss);
    }

    public void notifyLocationChanged() {
        this.mNotifier.notifyCellLocation(this);
    }

    @Override
    public void notifyCallForwardingIndicator() {
        TelephonyManager tm = TelephonyManager.from(this.mContext);
        int simState = tm.getSimState(this.mPhoneId);
        Rlog.d(LOG_TAG, "notifyCallForwardingIndicator: " + simState);
        if (simState != 5) {
            return;
        }
        this.mNotifier.notifyCallForwardingChanged(this);
    }

    @Override
    public void setSystemProperty(String property, String value) {
        if (getUnitTestMode()) {
            return;
        }
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            TelephonyManager.setTelephonyProperty(this.mPhoneId, property, value);
        } else {
            super.setSystemProperty(property, value);
        }
    }

    @Override
    public void registerForSuppServiceNotification(Handler h, int what, Object obj) {
        this.mSsnRegistrants.addUnique(h, what, obj);
        if (this.mCachedSsn == null) {
            return;
        }
        this.mSsnRegistrants.notifyRegistrants(this.mCachedSsn);
    }

    @Override
    public void unregisterForSuppServiceNotification(Handler h) {
        this.mSsnRegistrants.remove(h);
        this.mCachedSsn = null;
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        this.mSimRecordsLoadedRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        this.mSimRecordsLoadedRegistrants.remove(h);
    }

    @Override
    public void acceptCall(int videoState) throws CallStateException {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null && imsPhone.getRingingCall().isRinging()) {
            imsPhone.acceptCall(videoState);
        } else {
            this.mCT.acceptCall(videoState);
        }
    }

    @Override
    public void rejectCall() throws CallStateException {
        this.mCT.rejectCall();
    }

    @Override
    public void switchHoldingAndActive() throws CallStateException {
        this.mCT.switchWaitingOrHoldingAndActive();
    }

    @Override
    public String getIccSerialNumber() {
        IccRecords r = this.mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            r = this.mUiccController.getIccRecords(this.mPhoneId, 1);
        }
        if (r != null) {
            return r.getIccId();
        }
        return null;
    }

    @Override
    public String getFullIccSerialNumber() {
        IccRecords r = this.mIccRecords.get();
        if (!isPhoneTypeGsm() && r == null) {
            r = this.mUiccController.getIccRecords(this.mPhoneId, 1);
        }
        if (r != null) {
            return r.getFullIccId();
        }
        return null;
    }

    @Override
    public boolean canConference() {
        if (this.mImsPhone != null && this.mImsPhone.canConference()) {
            return true;
        }
        if (isPhoneTypeGsm()) {
            return this.mCT.canConference();
        }
        loge("canConference: not possible in CDMA");
        return VDBG;
    }

    @Override
    public void conference() {
        if (this.mImsPhone != null && this.mImsPhone.canConference()) {
            logd("conference() - delegated to IMS phone");
            try {
                this.mImsPhone.conference();
                return;
            } catch (CallStateException e) {
                loge(e.toString());
                return;
            }
        }
        if (isPhoneTypeGsm()) {
            this.mCT.conference();
        } else {
            loge("conference: not possible in CDMA");
        }
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("enableEnhancedVoicePrivacy: not expected on GSM");
        } else {
            this.mCi.setPreferredVoicePrivacy(enable, onComplete);
        }
    }

    @Override
    public void getEnhancedVoicePrivacy(Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("getEnhancedVoicePrivacy: not expected on GSM");
        } else {
            this.mCi.getPreferredVoicePrivacy(onComplete);
        }
    }

    @Override
    public void clearDisconnected() {
        this.mCT.clearDisconnected();
    }

    @Override
    public boolean canTransfer() {
        if (isPhoneTypeGsm()) {
            return this.mCT.canTransfer();
        }
        loge("canTransfer: not possible in CDMA");
        return VDBG;
    }

    @Override
    public void explicitCallTransfer() {
        if (isPhoneTypeGsm()) {
            this.mCT.explicitCallTransfer();
        } else {
            loge("explicitCallTransfer: not possible in CDMA");
        }
    }

    @Override
    public GsmCdmaCall getForegroundCall() {
        return this.mCT.mForegroundCall;
    }

    @Override
    public GsmCdmaCall getBackgroundCall() {
        return this.mCT.mBackgroundCall;
    }

    @Override
    public Call getRingingCall() {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null && imsPhone.getRingingCall().isRinging()) {
            return imsPhone.getRingingCall();
        }
        return this.mCT.mRingingCall;
    }

    private boolean handleUdubIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return VDBG;
        }
        if (getRingingCall().getState() != Call.State.IDLE || getBackgroundCall().getState() != Call.State.IDLE) {
            Rlog.d(LOG_TAG, "MmiCode 0: hangupWaitingOrBackground");
            this.mCT.hangupWaitingOrBackground();
        }
        return true;
    }

    private boolean handleCallDeflectionIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return VDBG;
        }
        if (getRingingCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: rejectCall");
            try {
                this.mCT.rejectCall();
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "reject failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.REJECT);
            }
        } else if (getBackgroundCall().getState() != Call.State.IDLE) {
            logd("MmiCode 0: hangupWaitingOrBackground");
            this.mCT.hangupWaitingOrBackground();
        }
        return true;
    }

    private boolean handleCallWaitingIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return VDBG;
        }
        GsmCdmaCall call = getForegroundCall();
        try {
            if (len > 1) {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                if (callIndex >= 1 && callIndex <= 19) {
                    logd("MmiCode 1: hangupConnectionByIndex " + callIndex);
                    this.mCT.hangupConnectionByIndex(call, callIndex);
                }
            } else if (call.getState() != Call.State.IDLE) {
                logd("MmiCode 1: hangup foreground");
                this.mCT.hangup(call);
            } else {
                logd("MmiCode 1: switchWaitingOrHoldingAndActive");
                this.mCT.switchWaitingOrHoldingAndActive();
            }
        } catch (CallStateException e) {
            Rlog.d(LOG_TAG, "hangup failed", e);
            notifySuppServiceFailed(PhoneInternalInterface.SuppService.HANGUP);
        }
        return true;
    }

    private boolean handleCallHoldIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len > 2) {
            return VDBG;
        }
        GsmCdmaCall call = getForegroundCall();
        if (len > 1) {
            try {
                char ch = dialString.charAt(1);
                int callIndex = ch - '0';
                GsmCdmaConnection conn = this.mCT.getConnectionByIndex(call, callIndex);
                if (conn != null && callIndex >= 1 && callIndex <= 19) {
                    logd("MmiCode 2: separate call " + callIndex);
                    this.mCT.separate(conn);
                } else {
                    logd("separate: invalid call index " + callIndex);
                    notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
                }
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "separate failed", e);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SEPARATE);
            }
        } else {
            try {
                if (getRingingCall().getState() != Call.State.IDLE) {
                    logd("MmiCode 2: accept ringing call");
                    this.mCT.acceptCall();
                } else {
                    logd("MmiCode 2: switchWaitingOrHoldingAndActive");
                    this.mCT.switchWaitingOrHoldingAndActive();
                }
            } catch (CallStateException e2) {
                Rlog.d(LOG_TAG, "switch failed", e2);
                notifySuppServiceFailed(PhoneInternalInterface.SuppService.SWITCH);
            }
        }
        return true;
    }

    private boolean handleMultipartyIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return VDBG;
        }
        logd("MmiCode 3: merge calls");
        conference();
        return true;
    }

    private boolean handleEctIncallSupplementaryService(String dialString) {
        int len = dialString.length();
        if (len != 1) {
            return VDBG;
        }
        logd("MmiCode 4: explicit call transfer");
        explicitCallTransfer();
        return true;
    }

    private boolean handleCcbsIncallSupplementaryService(String dialString) {
        if (dialString.length() > 1) {
            return VDBG;
        }
        Rlog.i(LOG_TAG, "MmiCode 5: CCBS not supported!");
        notifySuppServiceFailed(PhoneInternalInterface.SuppService.UNKNOWN);
        return true;
    }

    public Call getCSRingingCall() {
        return this.mCT.mRingingCall;
    }

    boolean isInCSCall() {
        Call.State foregroundCallState = getForegroundCall().getState();
        Call.State backgroundCallState = getBackgroundCall().getState();
        Call.State ringingCallState = getCSRingingCall().getState();
        if (foregroundCallState.isAlive() || backgroundCallState.isAlive()) {
            return true;
        }
        return ringingCallState.isAlive();
    }

    @Override
    public boolean handleInCallMmiCommands(String dialString) throws CallStateException {
        if (!isPhoneTypeGsm()) {
            loge("method handleInCallMmiCommands is NOT supported in CDMA!");
            return VDBG;
        }
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null && imsPhone.getServiceState().getState() == 0 && !isInCSCall()) {
            return imsPhone.handleInCallMmiCommands(dialString);
        }
        if (!isInCall() || TextUtils.isEmpty(dialString)) {
            return VDBG;
        }
        char ch = dialString.charAt(0);
        switch (ch) {
            case '0':
                boolean result = handleUdubIncallSupplementaryService(dialString);
                return result;
            case '1':
                boolean result2 = handleCallWaitingIncallSupplementaryService(dialString);
                return result2;
            case '2':
                boolean result3 = handleCallHoldIncallSupplementaryService(dialString);
                return result3;
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
                boolean result4 = handleMultipartyIncallSupplementaryService(dialString);
                return result4;
            case RadioNVItems.RIL_NV_CDMA_BC10:
                boolean result5 = handleEctIncallSupplementaryService(dialString);
                return result5;
            case '5':
                boolean result6 = handleCcbsIncallSupplementaryService(dialString);
                return result6;
            default:
                return VDBG;
        }
    }

    public boolean isInCall() {
        Call.State foregroundCallState = getForegroundCall().getState();
        Call.State backgroundCallState = getBackgroundCall().getState();
        Call.State ringingCallState = getRingingCall().getState();
        if (foregroundCallState.isAlive() || backgroundCallState.isAlive()) {
            return true;
        }
        return ringingCallState.isAlive();
    }

    @Override
    public Connection dial(String dialString, int videoState) throws CallStateException {
        return dial(dialString, null, videoState, null);
    }

    @Override
    public Connection dial(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) throws CallStateException {
        if (!isPhoneTypeGsm() && uusInfo != null) {
            throw new CallStateException("Sending UUS information NOT supported in CDMA!");
        }
        boolean isEmergency = PhoneNumberUtils.isEmergencyNumber(dialString);
        Phone imsPhone = this.mImsPhone;
        CarrierConfigManager configManager = (CarrierConfigManager) this.mContext.getSystemService("carrier_config");
        boolean alwaysTryImsForEmergencyCarrierConfig = configManager.getConfigForSubId(getSubId()).getBoolean("carrier_use_ims_first_for_emergency_bool");
        boolean imsUseEnabled = (isImsUseEnabled() && imsPhone != null && (imsPhone.isVolteEnabled() || imsPhone.isWifiCallingEnabled() || (imsPhone.isVideoEnabled() && VideoProfile.isVideo(videoState))) && imsPhone.getServiceState().getState() == 0) ? true : VDBG;
        boolean useImsForEmergency = (imsPhone != null && isEmergency && alwaysTryImsForEmergencyCarrierConfig && ImsManager.isNonTtyOrTtyOnVolteEnabled(this.mContext) && imsPhone.getServiceState().getState() != 3) ? true : VDBG;
        if (!isPhoneTypeGsm()) {
            useImsForEmergency = VDBG;
        }
        String dialPart = PhoneNumberUtils.extractNetworkPortionAlt(PhoneNumberUtils.stripSeparators(dialString));
        boolean zEndsWith = (dialPart.startsWith(CharacterSets.MIMENAME_ANY_CHARSET) || dialPart.startsWith("#")) ? dialPart.endsWith("#") : VDBG;
        boolean useImsForUt = (imsPhone == null || !imsPhone.isUtEnabled() || OperatorUtils.isNotSupportXcap(getOperatorNumeric())) ? VDBG : true;
        logd("imsUseEnabled=" + imsUseEnabled + ", useImsForEmergency=" + useImsForEmergency + ", useImsForUt=" + useImsForUt + ", isUt=" + zEndsWith + ", imsPhone=" + imsPhone + ", imsPhone.isVolteEnabled()=" + (imsPhone != null ? Boolean.valueOf(imsPhone.isVolteEnabled()) : "N/A") + ", imsPhone.isVowifiEnabled()=" + (imsPhone != null ? Boolean.valueOf(imsPhone.isWifiCallingEnabled()) : "N/A") + ", imsPhone.isVideoEnabled()=" + (imsPhone != null ? Boolean.valueOf(imsPhone.isVideoEnabled()) : "N/A") + ", imsPhone.getServiceState().getState()=" + (imsPhone != null ? Integer.valueOf(imsPhone.getServiceState().getState()) : "N/A"));
        Phone.checkWfcWifiOnlyModeBeforeDial(this.mImsPhone, this.mContext);
        Rlog.w(LOG_TAG, "IMS: imsphone = " + imsPhone + "isEmergencyNumber = " + isEmergency);
        if (imsPhone != null) {
            Rlog.w(LOG_TAG, "service state = " + imsPhone.getServiceState().getState());
        }
        if ((imsUseEnabled && (!zEndsWith || useImsForUt)) || useImsForEmergency) {
            if (isInCSCall()) {
                Rlog.d(LOG_TAG, "has CS Call. Don't try IMS PS Call!");
            } else {
                try {
                    if (videoState == 0) {
                        Rlog.d(LOG_TAG, "Trying IMS PS call");
                        return imsPhone.dial(dialString, uusInfo, videoState, intentExtras);
                    }
                    if (SystemProperties.get("persist.mtk_vilte_support").equals("1")) {
                        Rlog.d(LOG_TAG, "Trying IMS PS video call");
                        return imsPhone.dial(dialString, uusInfo, videoState, intentExtras);
                    }
                    Rlog.d(LOG_TAG, "Trying (non-IMS) CS video call");
                    return dialInternal(dialString, uusInfo, videoState, intentExtras);
                } catch (CallStateException e) {
                    logd("IMS PS call exception " + e + "imsUseEnabled =" + imsUseEnabled + ", imsPhone =" + imsPhone);
                    if (!Phone.CS_FALLBACK.equals(e.getMessage())) {
                        CallStateException ce = new CallStateException(e.getMessage());
                        ce.setStackTrace(e.getStackTrace());
                        throw ce;
                    }
                }
            }
        }
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) != 2 && this.mSST != null && this.mSST.mSS.getState() == 1 && this.mSST.mSS.getDataRegState() != 0 && !isEmergency) {
            throw new CallStateException("cannot dial in current state");
        }
        logd("Trying (non-IMS) CS call");
        return isPhoneTypeGsm() ? dialInternal(dialString, null, videoState, intentExtras) : dialInternal(dialString, null, videoState, intentExtras);
    }

    @Override
    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) throws CallStateException {
        String newDialString = dialString;
        if (!PhoneNumberUtils.isUriNumber(dialString)) {
            newDialString = PhoneNumberUtils.stripSeparators(dialString);
        }
        if (isPhoneTypeGsm()) {
            if (handleInCallMmiCommands(newDialString)) {
                return null;
            }
            String networkPortion = PhoneNumberUtils.extractNetworkPortionAlt(newDialString);
            GsmMmiCode mmi = GsmMmiCode.newFromDialString(networkPortion, this, this.mUiccApplication.get());
            logd("dialing w/ mmi '" + mmi + "'...");
            if (mmi == null) {
                if (videoState == 0) {
                    return this.mCT.dial(newDialString, uusInfo, intentExtras);
                }
                if (!is3GVTEnabled()) {
                    throw new CallStateException("cannot vtDial for non-3GVT-capable device");
                }
                return this.mCT.vtDial(newDialString, uusInfo, intentExtras);
            }
            if (mmi.isTemporaryModeCLIR()) {
                if (videoState == 0) {
                    return this.mCT.dial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo, intentExtras);
                }
                if (!is3GVTEnabled()) {
                    throw new CallStateException("cannot vtDial for non-3GVT-capable device");
                }
                return this.mCT.vtDial(mmi.mDialingNumber, mmi.getCLIRMode(), uusInfo, intentExtras);
            }
            this.mPendingMMIs.add(mmi);
            Rlog.d(LOG_TAG, "dialInternal: " + dialString + ", mmi=" + mmi);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
            try {
                mmi.processCode();
            } catch (CallStateException e) {
            }
            return null;
        }
        return this.mCT.dial(newDialString);
    }

    @Override
    public boolean handlePinMmi(String dialString) {
        MmiCode mmi;
        if (isPhoneTypeGsm()) {
            mmi = GsmMmiCode.newFromDialString(dialString, this, this.mUiccApplication.get());
        } else {
            mmi = CdmaMmiCode.newFromDialString(dialString, this, this.mUiccApplication.get());
        }
        if (mmi != null && mmi.isPinPukCommand()) {
            this.mPendingMMIs.add(mmi);
            Rlog.d(LOG_TAG, "handlePinMmi: " + dialString + ", mmi=" + mmi);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
            try {
                mmi.processCode();
                return true;
            } catch (CallStateException e) {
                return true;
            }
        }
        loge("Mmi is null or unrecognized!");
        return VDBG;
    }

    @Override
    public void sendUssdResponse(String ussdMessge) {
        if (isPhoneTypeGsm()) {
            GsmMmiCode mmi = GsmMmiCode.newFromUssdUserInput(ussdMessge, this, this.mUiccApplication.get());
            this.mPendingMMIs.add(mmi);
            Rlog.d(LOG_TAG, "sendUssdResponse: " + ussdMessge + ", mmi=" + mmi);
            dumpPendingMmi();
            this.mMmiRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
            mmi.sendUssd(ussdMessge);
            return;
        }
        loge("sendUssdResponse: not possible in CDMA");
    }

    @Override
    public void sendDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("sendDtmf called with invalid character '" + c + "'");
        } else {
            if (this.mCT.mState != PhoneConstants.State.OFFHOOK) {
                return;
            }
            this.mCi.sendDtmf(c, null);
        }
    }

    @Override
    public void startDtmf(char c) {
        if (!PhoneNumberUtils.is12Key(c)) {
            loge("startDtmf called with invalid character '" + c + "'");
        } else {
            this.mCi.startDtmf(c, null);
        }
    }

    @Override
    public void stopDtmf() {
        this.mCi.stopDtmf(null);
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] sendBurstDtmf() is a CDMA method");
            return;
        }
        boolean check = true;
        int itr = 0;
        while (true) {
            if (itr >= dtmfString.length()) {
                break;
            }
            if (PhoneNumberUtils.is12Key(dtmfString.charAt(itr))) {
                itr++;
            } else {
                Rlog.e(LOG_TAG, "sendDtmf called with invalid character '" + dtmfString.charAt(itr) + "'");
                check = VDBG;
                break;
            }
        }
        if (this.mCT.mState != PhoneConstants.State.OFFHOOK || !check) {
            return;
        }
        this.mCi.sendBurstDtmf(dtmfString, on, off, onComplete);
    }

    @Override
    public void setRadioPower(boolean power) {
        this.mSST.setRadioPower(power);
    }

    private void storeVoiceMailNumber(String number) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        if (isPhoneTypeGsm()) {
            editor.putString(VM_NUMBER + getPhoneId(), number);
            editor.apply();
            setVmSimImsi(getSubscriberId());
        } else {
            editor.putString(VM_NUMBER_CDMA + getPhoneId(), number);
            editor.apply();
        }
    }

    @Override
    public String getVoiceMailNumber() {
        String number;
        String[] listArray;
        String[] defaultVMNumberArray;
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            number = r != null ? r.getVoiceMailNumber() : UsimPBMemInfo.STRING_NOT_SET;
            if (TextUtils.isEmpty(number)) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                number = sp.getString(VM_NUMBER + getPhoneId(), null);
            }
        } else {
            SharedPreferences sp2 = PreferenceManager.getDefaultSharedPreferences(getContext());
            number = sp2.getString(VM_NUMBER_CDMA + getPhoneId(), null);
        }
        if (TextUtils.isEmpty(number) && (listArray = getContext().getResources().getStringArray(R.array.config_defaultPinnerServiceFiles)) != null && listArray.length > 0) {
            int i = 0;
            while (true) {
                if (i >= listArray.length) {
                    break;
                }
                if (!TextUtils.isEmpty(listArray[i]) && (defaultVMNumberArray = listArray[i].split(";")) != null && defaultVMNumberArray.length > 0) {
                    if (defaultVMNumberArray.length == 1) {
                        number = defaultVMNumberArray[0];
                    } else if (defaultVMNumberArray.length == 2 && !TextUtils.isEmpty(defaultVMNumberArray[1]) && isMatchGid(defaultVMNumberArray[1])) {
                        number = defaultVMNumberArray[0];
                        break;
                    }
                }
                i++;
            }
        }
        if (!isPhoneTypeGsm() && TextUtils.isEmpty(number)) {
            if (getContext().getResources().getBoolean(R.^attr-private.glyphMap)) {
                String number2 = getLine1Number();
                return number2;
            }
            return "*86";
        }
        return number;
    }

    private String getVmSimImsi() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getString(VM_SIM_IMSI + getPhoneId(), null);
    }

    private void setVmSimImsi(String imsi) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VM_SIM_IMSI + getPhoneId(), imsi);
        editor.apply();
    }

    @Override
    public String getVoiceMailAlphaTag() {
        String ret = UsimPBMemInfo.STRING_NOT_SET;
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            ret = r != null ? r.getVoiceMailAlphaTag() : UsimPBMemInfo.STRING_NOT_SET;
        }
        if (ret == null || ret.length() == 0) {
            return this.mContext.getText(R.string.defaultVoiceMailAlphaTag).toString();
        }
        return ret;
    }

    @Override
    public String getDeviceId() {
        if (isPhoneTypeGsm()) {
            return this.mImei;
        }
        if (getLteOnCdmaMode() == 1) {
            Rlog.d(LOG_TAG, "getDeviceId() in LTE_ON_CDMA_TRUE : return Imei");
            return getImei();
        }
        String id = getMeid();
        if (id == null || id.matches("^0*$")) {
            loge("getDeviceId(): MEID is not initialized use ESN");
            return getEsn();
        }
        return id;
    }

    public int isDeviceIdAbnormal() {
        return this.mDeviceIdAbnormal;
    }

    public void setDeviceIdAbnormal(int abnormal) {
        this.mDeviceIdAbnormal = abnormal;
    }

    @Override
    public String getDeviceSvn() {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            return this.mImeiSv;
        }
        loge("getDeviceSvn(): return 0");
        return "0";
    }

    @Override
    public IsimRecords getIsimRecords() {
        return this.mIsimUiccRecords;
    }

    @Override
    public String getImei() {
        return this.mImei;
    }

    @Override
    public String getEsn() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getEsn() is a CDMA method");
            return "0";
        }
        return this.mEsn;
    }

    @Override
    public String getMeid() {
        if (isPhoneTypeGsm()) {
            loge("[GsmCdmaPhone] getMeid() is a CDMA method");
            return "0";
        }
        return this.mMeid;
    }

    @Override
    public String getNai() {
        IccRecords r = this.mUiccController.getIccRecords(this.mPhoneId, 2);
        if (Log.isLoggable(LOG_TAG, 2)) {
            Rlog.v(LOG_TAG, "IccRecords is " + r);
        }
        if (r != null) {
            return r.getNAI();
        }
        return null;
    }

    @Override
    public String getSubscriberId() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getIMSI();
            }
            return null;
        }
        if (isPhoneTypeCdma()) {
            logd("getSubscriberId, phone type is CDMA Imsi = " + this.mSST.getImsi());
            return this.mSST.getImsi();
        }
        IccRecords r2 = this.mIccRecords.get();
        if (this.mSimRecords != null) {
            return this.mSimRecords.getIMSI();
        }
        if (r2 != null) {
            return r2.getIMSI();
        }
        return null;
    }

    @Override
    public String getGroupIdLevel1() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getGid1();
            }
            return null;
        }
        if (!isPhoneTypeCdma()) {
            return this.mSimRecords != null ? this.mSimRecords.getGid1() : UsimPBMemInfo.STRING_NOT_SET;
        }
        loge("GID1 is not available in CDMA");
        return null;
    }

    @Override
    public String getGroupIdLevel2() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getGid2();
            }
            return null;
        }
        if (!isPhoneTypeCdma()) {
            return this.mSimRecords != null ? this.mSimRecords.getGid2() : UsimPBMemInfo.STRING_NOT_SET;
        }
        loge("GID2 is not available in CDMA");
        return null;
    }

    @Override
    public String getLine1Number() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getMsisdnNumber();
            }
            return null;
        }
        return this.mSST.getMdnNumber();
    }

    @Override
    public String getCdmaPrlVersion() {
        return this.mSST.getPrlVersion();
    }

    @Override
    public String getCdmaMin() {
        return this.mSST.getCdmaMin();
    }

    @Override
    public boolean isMinInfoReady() {
        return this.mSST.isMinInfoReady();
    }

    @Override
    public String getMsisdn() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getMsisdnNumber();
            }
            return null;
        }
        if (isPhoneTypeCdmaLte()) {
            if (this.mSimRecords != null) {
                return this.mSimRecords.getMsisdnNumber();
            }
            return null;
        }
        loge("getMsisdn: not expected on CDMA");
        return null;
    }

    @Override
    public String getLine1AlphaTag() {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getMsisdnAlphaTag();
            }
            return null;
        }
        loge("getLine1AlphaTag: not possible in CDMA");
        return null;
    }

    @Override
    public boolean setLine1Number(String alphaTag, String number, Message onComplete) {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r == null) {
                return VDBG;
            }
            r.setMsisdnNumber(alphaTag, number, onComplete);
            return true;
        }
        loge("setLine1Number: not possible in CDMA");
        return VDBG;
    }

    @Override
    public void setVoiceMailNumber(String alphaTag, String voiceMailNumber, Message onComplete) {
        this.mVmNumber = voiceMailNumber;
        Message resp = obtainMessage(20, 0, 0, onComplete);
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return;
        }
        r.setVoiceMailNumber(alphaTag, this.mVmNumber, resp);
    }

    private boolean isValidCommandInterfaceCFReason(int commandInterfaceCFReason) {
        switch (commandInterfaceCFReason) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return VDBG;
        }
    }

    @Override
    public String getSystemProperty(String property, String defValue) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            if (getUnitTestMode()) {
                return null;
            }
            return TelephonyManager.getTelephonyProperty(this.mPhoneId, property, defValue);
        }
        return super.getSystemProperty(property, defValue);
    }

    private boolean isValidCommandInterfaceCFAction(int commandInterfaceCFAction) {
        switch (commandInterfaceCFAction) {
            case 0:
            case 1:
            case 3:
            case 4:
                return true;
            case 2:
            default:
                return VDBG;
        }
    }

    private boolean isCfEnable(int action) {
        if (action == 1 || action == 3) {
            return true;
        }
        return VDBG;
    }

    @Override
    public void getCallForwardingOption(int commandInterfaceCFReason, Message onComplete) {
        Message resp;
        if (isPhoneTypeGsm()) {
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(12, onComplete);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                if (isOpReregisterForCF() && onComplete.arg2 == 1) {
                    Rlog.d(LOG_TAG, "Set ims dereg to ON.");
                    SystemProperties.set(IMS_DEREG_PROP, "1");
                }
                imsPhone.getCallForwardingOption(commandInterfaceCFReason, imsUtResult);
                return;
            }
            if (!isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
                return;
            }
            logd("requesting call forwarding query.");
            if (commandInterfaceCFReason == 0) {
                resp = obtainMessage(13, onComplete);
            } else {
                resp = onComplete;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mSSReqDecisionMaker.queryCallForwardStatus(commandInterfaceCFReason, 0, null, resp);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.queryCallForwardStatus(commandInterfaceCFReason, 0, null, resp);
            return;
        }
        loge("getCallForwardingOption: not possible in CDMA");
    }

    @Override
    public void setCallForwardingOption(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message onComplete) {
        Message resp;
        if (isPhoneTypeGsm()) {
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(11, onComplete);
                ss.mParcel.writeInt(commandInterfaceCFAction);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                ss.mParcel.writeString(dialingNumber);
                ss.mParcel.writeInt(timerSeconds);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, imsUtResult);
                return;
            }
            if (!isValidCommandInterfaceCFAction(commandInterfaceCFAction) || !isValidCommandInterfaceCFReason(commandInterfaceCFReason)) {
                return;
            }
            if (commandInterfaceCFReason == 0) {
                int origUtCfuMode = 0;
                String utCfuMode = getSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                if ("enabled_ut_cfu_mode_on".equals(utCfuMode)) {
                    origUtCfuMode = 1;
                } else if ("enabled_ut_cfu_mode_off".equals(utCfuMode)) {
                    origUtCfuMode = 2;
                }
                setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                Object cfu = new Cfu(dialingNumber, onComplete);
                resp = obtainMessage(12, isCfEnable(commandInterfaceCFAction) ? 1 : 0, origUtCfuMode, cfu);
            } else {
                resp = onComplete;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mSSReqDecisionMaker.setCallForward(commandInterfaceCFAction, commandInterfaceCFReason, 1, dialingNumber, timerSeconds, resp);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.setCallForward(commandInterfaceCFAction, commandInterfaceCFReason, 1, dialingNumber, timerSeconds, resp);
            return;
        }
        loge("setCallForwardingOption: not possible in CDMA");
    }

    public int[] getSavedClirSetting() {
        int presentationMode;
        int getClirResult;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int clirSetting = sp.getInt(Phone.CLIR_KEY + getPhoneId(), -1);
        if (clirSetting == 0 || clirSetting == -1) {
            presentationMode = 4;
            getClirResult = 0;
        } else if (clirSetting == 1) {
            presentationMode = 3;
            getClirResult = 1;
        } else {
            presentationMode = 4;
            getClirResult = 2;
        }
        int[] getClirResponse = {getClirResult, presentationMode};
        Rlog.d(LOG_TAG, "getClirResult: " + getClirResult);
        Rlog.d(LOG_TAG, "presentationMode: " + presentationMode);
        return getClirResponse;
    }

    @Override
    public void getOutgoingCallerIdDisplay(Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0 && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                int[] result = getSavedClirSetting();
                if (result[0] == 0) {
                    Rlog.d(LOG_TAG, "CLIR DEFAULT, so return DEFAULT directly.");
                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, result, (Throwable) null);
                        onComplete.sendToTarget();
                        return;
                    }
                    return;
                }
                if (isOpTbClir()) {
                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, result, (Throwable) null);
                        onComplete.sendToTarget();
                        return;
                    }
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(4, onComplete);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.getOutgoingCallerIdDisplay(imsUtResult);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                int[] result2 = getSavedClirSetting();
                if (result2[0] == 0 && !isOp(OperatorUtils.OPID.OP01)) {
                    Rlog.d(LOG_TAG, "CLIR DEFAULT, so return DEFAULT directly.");
                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, result2, (Throwable) null);
                        onComplete.sendToTarget();
                        return;
                    }
                    return;
                }
                if (isOpTbClir()) {
                    if (onComplete != null) {
                        AsyncResult.forMessage(onComplete, result2, (Throwable) null);
                        onComplete.sendToTarget();
                        return;
                    }
                    return;
                }
                this.mSSReqDecisionMaker.getCLIR(onComplete);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.getCLIR(onComplete);
            return;
        }
        loge("getOutgoingCallerIdDisplay: not possible in CDMA");
    }

    @Override
    public void setOutgoingCallerIdDisplay(int commandInterfaceCLIRMode, Message onComplete) {
        if (isPhoneTypeGsm()) {
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0 && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportCallIdentity()) {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                if (isOpTbClir()) {
                    this.mCi.setCLIR(commandInterfaceCLIRMode, obtainMessage(18, commandInterfaceCLIRMode, 0, onComplete));
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(3, onComplete);
                ss.mParcel.writeInt(commandInterfaceCLIRMode);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, imsUtResult);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                if (isOpTbClir()) {
                    this.mCi.setCLIR(commandInterfaceCLIRMode, obtainMessage(18, commandInterfaceCLIRMode, 0, onComplete));
                    return;
                } else {
                    this.mSSReqDecisionMaker.setCLIR(commandInterfaceCLIRMode, obtainMessage(18, commandInterfaceCLIRMode, 0, onComplete));
                    return;
                }
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.setCLIR(commandInterfaceCLIRMode, obtainMessage(18, commandInterfaceCLIRMode, 0, onComplete));
            return;
        }
        loge("setOutgoingCallerIdDisplay: not possible in CDMA");
    }

    private void initTbcwMode() {
        if (this.mTbcwMode == 0) {
            if (isOpTbcwWithCS(getPhoneId())) {
                setTbcwMode(4);
                setTbcwToEnabledOnIfDisabled();
            } else if (!isUsimCard()) {
                setTbcwMode(3);
                setSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
            }
        }
        Rlog.d(LOG_TAG, "initTbcwMode: " + this.mTbcwMode);
    }

    public int getTbcwMode() {
        if (this.mTbcwMode == 0) {
            initTbcwMode();
        }
        return this.mTbcwMode;
    }

    public void setTbcwMode(int newMode) {
        Rlog.d(LOG_TAG, "Set tbcwmode: " + newMode);
        this.mTbcwMode = newMode;
    }

    public void setTbcwToEnabledOnIfDisabled() {
        String tbcwMode = getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
        if (!"disabled_tbcw".equals(tbcwMode)) {
            return;
        }
        setSystemProperty("persist.radio.terminal-based.cw", "enabled_tbcw_on");
    }

    public void getTerminalBasedCallWaiting(Message onComplete) {
        String tbcwMode = getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
        Rlog.d(LOG_TAG, "getTerminalBasedCallWaiting(): tbcwMode = " + tbcwMode + ", onComplete = " + onComplete);
        if ("enabled_tbcw_on".equals(tbcwMode)) {
            if (onComplete != null) {
                int[] cwInfos = {1, 1};
                AsyncResult.forMessage(onComplete, cwInfos, (Throwable) null);
                onComplete.sendToTarget();
                return;
            }
            return;
        }
        if ("enabled_tbcw_off".equals(tbcwMode)) {
            if (onComplete != null) {
                int[] cwInfos2 = {0, 0};
                AsyncResult.forMessage(onComplete, cwInfos2, (Throwable) null);
                onComplete.sendToTarget();
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "getTerminalBasedCallWaiting(): ERROR: tbcwMode = " + tbcwMode);
    }

    @Override
    public void getCallWaiting(Message onComplete) {
        if (isPhoneTypeGsm()) {
            if (!isOpNwCW()) {
                if (this.mTbcwMode == 0) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "getCallWaiting(): mTbcwMode = " + this.mTbcwMode + ", onComplete = " + onComplete);
                if (this.mTbcwMode == 2) {
                    getTerminalBasedCallWaiting(onComplete);
                    return;
                } else if (this.mTbcwMode == 3) {
                    this.mCi.queryCallWaiting(0, onComplete);
                    return;
                } else if (this.mTbcwMode == 4) {
                    Message resp = obtainMessage(301, onComplete);
                    this.mCi.queryCallWaiting(0, resp);
                    return;
                }
            }
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNwCW()) {
                    Rlog.d(LOG_TAG, "isOpNwCW(), getCallWaiting() by Ut interface");
                    SuppSrvRequest ss = SuppSrvRequest.obtain(14, onComplete);
                    Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                    imsPhone.getCallWaiting(imsUtResult);
                    return;
                }
                Rlog.d(LOG_TAG, "isOpTbCW(), getTerminalBasedCallWaiting");
                setTbcwMode(2);
                setTbcwToEnabledOnIfDisabled();
                getTerminalBasedCallWaiting(onComplete);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mSSReqDecisionMaker.queryCallWaiting");
                this.mSSReqDecisionMaker.queryCallWaiting(0, onComplete);
                return;
            } else {
                this.mCi.queryCallWaiting(0, onComplete);
                return;
            }
        }
        this.mCi.queryCallWaiting(1, onComplete);
    }

    public void setTerminalBasedCallWaiting(boolean enable, Message onComplete) {
        String tbcwMode = getSystemProperty("persist.radio.terminal-based.cw", "disabled_tbcw");
        Rlog.d(LOG_TAG, "setTerminalBasedCallWaiting(): tbcwMode = " + tbcwMode + ", enable = " + enable);
        if ("enabled_tbcw_on".equals(tbcwMode)) {
            if (!enable) {
                setSystemProperty("persist.radio.terminal-based.cw", "enabled_tbcw_off");
            }
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, (Object) null, (Throwable) null);
                onComplete.sendToTarget();
                return;
            }
            return;
        }
        if ("enabled_tbcw_off".equals(tbcwMode)) {
            if (enable) {
                setSystemProperty("persist.radio.terminal-based.cw", "enabled_tbcw_on");
            }
            if (onComplete != null) {
                AsyncResult.forMessage(onComplete, (Object) null, (Throwable) null);
                onComplete.sendToTarget();
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "setTerminalBasedCallWaiting(): ERROR: tbcwMode = " + tbcwMode);
    }

    @Override
    public void setCallWaiting(boolean enable, Message onComplete) {
        if (isPhoneTypeGsm()) {
            if (!isOpNwCW()) {
                if (this.mTbcwMode == 0) {
                    initTbcwMode();
                }
                Rlog.d(LOG_TAG, "setCallWaiting(): mTbcwMode = " + this.mTbcwMode + ", onComplete = " + onComplete);
                if (this.mTbcwMode == 2) {
                    setTerminalBasedCallWaiting(enable, onComplete);
                    return;
                } else if (this.mTbcwMode == 3) {
                    this.mCi.setCallWaiting(enable, 1, onComplete);
                    return;
                } else if (this.mTbcwMode == 4) {
                    Message resp = obtainMessage(302, enable ? 1 : 0, 0, onComplete);
                    this.mCi.setCallWaiting(enable, 1, resp);
                    return;
                }
            }
            Phone imsPhone = this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && ((imsPhone.getServiceState().getState() == 0 || imsPhone.isUtEnabled()) && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport())))) {
                if (isOpNwCW()) {
                    Rlog.d(LOG_TAG, "isOpNwCW(), setCallWaiting(): IMS in service");
                    SuppSrvRequest ss = SuppSrvRequest.obtain(13, onComplete);
                    int enableState = enable ? 1 : 0;
                    ss.mParcel.writeInt(enableState);
                    Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                    imsPhone.setCallWaiting(enable, imsUtResult);
                    return;
                }
                Rlog.d(LOG_TAG, "isOpTbCW(), setTerminalBasedCallWaiting(): IMS in service");
                setTbcwMode(2);
                setTbcwToEnabledOnIfDisabled();
                setTerminalBasedCallWaiting(enable, onComplete);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                Rlog.d(LOG_TAG, "mSSReqDecisionMaker.setCallWaiting");
                this.mSSReqDecisionMaker.setCallWaiting(enable, 1, onComplete);
                return;
            } else {
                if (getCsFallbackStatus() == 1) {
                    setCsFallbackStatus(0);
                }
                this.mCi.setCallWaiting(enable, 1, onComplete);
                return;
            }
        }
        loge("method setCallWaiting is NOT supported in CDMA!");
    }

    @Override
    public void getFacilityLock(String facility, String password, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0 && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportOCB(facility)) {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(10, onComplete);
                ss.mParcel.writeString(facility);
                ss.mParcel.writeString(password);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.getCallBarring(facility, imsUtResult);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mSSReqDecisionMaker.queryFacilityLock(facility, password, 1, onComplete);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.queryFacilityLock(facility, password, 1, onComplete);
            return;
        }
        loge("method getFacilityLock is NOT supported in CDMA!");
    }

    @Override
    public void setFacilityLock(String facility, boolean enable, String password, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
            if (getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0 && (imsPhone.isVolteEnabled() || (imsPhone.isWifiCallingEnabled() && isWFCUtSupport()))) {
                if (isOpNotSupportOCB(facility)) {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
                SuppSrvRequest ss = SuppSrvRequest.obtain(9, onComplete);
                ss.mParcel.writeString(facility);
                int enableState = enable ? 1 : 0;
                ss.mParcel.writeInt(enableState);
                ss.mParcel.writeString(password);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.setCallBarring(facility, enable, password, imsUtResult);
                return;
            }
            if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                this.mSSReqDecisionMaker.setFacilityLock(facility, enable, password, 1, onComplete);
                return;
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            this.mCi.setFacilityLock(facility, enable, password, 1, onComplete);
            return;
        }
        loge("method setFacilityLock is NOT supported in CDMA!");
    }

    @Override
    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message onComplete) {
        if (isPhoneTypeGsm()) {
            if (isDuringImsCall()) {
                if (onComplete == null) {
                    return;
                }
                CommandException ce = new CommandException(CommandException.Error.GENERIC_FAILURE);
                AsyncResult.forMessage(onComplete, (Object) null, ce);
                onComplete.sendToTarget();
                return;
            }
            this.mCi.changeBarringPassword(facility, oldPwd, newPwd, onComplete);
            return;
        }
        loge("method setFacilityLock is NOT supported in CDMA!");
    }

    private static class CfuEx {
        final Message mOnComplete;
        final String mSetCfNumber;
        final long[] mSetTimeSlot;

        CfuEx(String cfNumber, long[] cfTimeSlot, Message onComplete) {
            this.mSetCfNumber = cfNumber;
            this.mSetTimeSlot = cfTimeSlot;
            this.mOnComplete = onComplete;
        }
    }

    @Override
    public void getCallForwardInTimeSlot(int commandInterfaceCFReason, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
            if (getCsFallbackStatus() == 0 && isOp(OperatorUtils.OPID.OP01) && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
                imsPhone.getCallForwardInTimeSlot(commandInterfaceCFReason, onComplete);
                return;
            }
            if (commandInterfaceCFReason == 0) {
                Rlog.d(LOG_TAG, "requesting call forwarding in time slot query.");
                Message resp = obtainMessage(CharacterSets.ISO_8859_13, onComplete);
                if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                    setSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                    this.mSSReqDecisionMaker.queryCallForwardInTimeSlotStatus(commandInterfaceCFReason, 1, resp);
                    return;
                } else {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
            }
            if (onComplete == null) {
                return;
            }
            sendErrorResponse(onComplete, CommandException.Error.GENERIC_FAILURE);
            return;
        }
        loge("method getCallForwardInTimeSlot is NOT supported in CDMA!");
    }

    @Override
    public void setCallForwardInTimeSlot(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, long[] timeSlot, Message onComplete) {
        if (isPhoneTypeGsm()) {
            ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
            if (getCsFallbackStatus() == 0 && isOp(OperatorUtils.OPID.OP01) && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
                SuppSrvRequest ss = SuppSrvRequest.obtain(17, onComplete);
                ss.mParcel.writeInt(commandInterfaceCFAction);
                ss.mParcel.writeInt(commandInterfaceCFReason);
                ss.mParcel.writeString(dialingNumber);
                ss.mParcel.writeInt(timerSeconds);
                Message imsUtResult = obtainMessage(ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT, ss);
                imsPhone.setCallForwardInTimeSlot(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, timeSlot, imsUtResult);
                return;
            }
            if (isValidCommandInterfaceCFAction(commandInterfaceCFAction) && commandInterfaceCFReason == 0) {
                Object cfuEx = new CfuEx(dialingNumber, timeSlot, onComplete);
                Message resp = obtainMessage(CharacterSets.ISO_8859_14, isCfEnable(commandInterfaceCFAction) ? 1 : 0, 0, cfuEx);
                if (getCsFallbackStatus() == 0 && isGsmUtSupport()) {
                    this.mSSReqDecisionMaker.setCallForwardInTimeSlot(commandInterfaceCFAction, commandInterfaceCFReason, 1, dialingNumber, timerSeconds, timeSlot, resp);
                    return;
                } else {
                    sendErrorResponse(onComplete, CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED);
                    return;
                }
            }
            sendErrorResponse(onComplete, CommandException.Error.GENERIC_FAILURE);
            return;
        }
        loge("method setCallForwardInTimeSlot is NOT supported in CDMA!");
    }

    private void handleCfuInTimeSlotQueryResult(CallForwardInfoEx[] infos) {
        boolean z = VDBG;
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return;
        }
        if (infos == null || infos.length == 0) {
            setVoiceCallForwardingFlag(1, VDBG, null);
            setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
            return;
        }
        int s = infos.length;
        for (int i = 0; i < s; i++) {
            if ((infos[i].serviceClass & 1) != 0) {
                if (infos[i].status == 1) {
                    z = true;
                }
                setVoiceCallForwardingFlag(1, z, infos[i].number);
                String mode = infos[i].status == 1 ? "enabled_ut_cfu_mode_on" : "enabled_ut_cfu_mode_off";
                setSystemProperty("persist.radio.ut.cfu.mode", mode);
                saveTimeSlot(infos[i].timeSlot);
                return;
            }
        }
    }

    void sendErrorResponse(Message onComplete, CommandException.Error error) {
        Rlog.d(LOG_TAG, "sendErrorResponse" + error);
        if (onComplete == null) {
            return;
        }
        AsyncResult.forMessage(onComplete, (Object) null, new CommandException(error));
        onComplete.sendToTarget();
    }

    public boolean queryCfuOrWait() {
        String oppositePropertyValue1 = SystemProperties.get(CFU_QUERY_PROPERTY_NAME + 99);
        String oppositePropertyValue2 = SystemProperties.get(CFU_QUERY_PROPERTY_NAME + 99);
        if (oppositePropertyValue1.equals("1") || oppositePropertyValue2.equals("1")) {
            Message message = obtainMessage(CallFailCause.RECOVERY_ON_TIMER_EXPIRY);
            sendMessageDelayed(message, 1000L);
            return VDBG;
        }
        if (isPhoneTypeGsm()) {
            boolean bDataEnable = getDataEnabled();
            Rlog.d(LOG_TAG, "bDataEnable: " + bDataEnable);
            if (getCsFallbackStatus() == 0 && isGsmUtSupport() && bDataEnable) {
                this.mSSReqDecisionMaker.queryCallForwardInTimeSlotStatus(0, 1, obtainMessage(CharacterSets.ISO_8859_13, 1, 0, null));
            } else {
                Phone imsPhone = this.mImsPhone;
                if (getCsFallbackStatus() == 0 && imsPhone != null && imsPhone.getServiceState().getState() == 0 && !bDataEnable && isOp(OperatorUtils.OPID.OP01)) {
                    Rlog.d(LOG_TAG, "No need query CFU in CS domain!");
                } else {
                    if (getCsFallbackStatus() == 1) {
                        setCsFallbackStatus(0);
                    }
                    this.mCi.queryCallForwardStatus(0, 1, null, obtainMessage(13));
                }
            }
        }
        return true;
    }

    public SSRequestDecisionMaker getSSRequestDecisionMaker() {
        return this.mSSReqDecisionMaker;
    }

    public boolean isDuringImsCall() {
        if (this.mImsPhone != null) {
            Call.State foregroundCallState = this.mImsPhone.getForegroundCall().getState();
            Call.State backgroundCallState = this.mImsPhone.getBackgroundCall().getState();
            Call.State ringingCallState = this.mImsPhone.getRingingCall().getState();
            boolean isDuringImsCall = (foregroundCallState.isAlive() || backgroundCallState.isAlive()) ? true : ringingCallState.isAlive();
            if (isDuringImsCall) {
                Rlog.d(LOG_TAG, "During IMS call.");
                return true;
            }
            return VDBG;
        }
        return VDBG;
    }

    private void handleImsUtCsfb(Message msg) {
        SuppSrvRequest ss = (SuppSrvRequest) msg.obj;
        if (ss == null) {
            Rlog.e(LOG_TAG, "handleImsUtCsfb: Error SuppSrvRequest null!");
            return;
        }
        if (isDuringImsCall()) {
            Message resultCallback = ss.getResultCallback();
            if (resultCallback != null) {
                CommandException ce = new CommandException(CommandException.Error.GENERIC_FAILURE);
                AsyncResult.forMessage(resultCallback, (Object) null, ce);
                resultCallback.sendToTarget();
            }
            if (getCsFallbackStatus() == 1) {
                setCsFallbackStatus(0);
            }
            ss.setResultCallback(null);
            ss.mParcel.recycle();
            return;
        }
        int requestCode = ss.getRequestCode();
        ss.mParcel.setDataPosition(0);
        switch (requestCode) {
            case 3:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CLIR");
                int commandInterfaceCLIRMode = ss.mParcel.readInt();
                setOutgoingCallerIdDisplay(commandInterfaceCLIRMode, ss.getResultCallback());
                break;
            case 4:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CLIR");
                getOutgoingCallerIdDisplay(ss.getResultCallback());
                break;
            case 5:
            case 6:
            case 7:
            case 8:
            default:
                Rlog.e(LOG_TAG, "handleImsUtCsfb: invalid requestCode = " + requestCode);
                break;
            case 9:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CB");
                String facility = ss.mParcel.readString();
                int enableState = ss.mParcel.readInt();
                boolean enable = enableState != 0 ? true : VDBG;
                String password = ss.mParcel.readString();
                setFacilityLock(facility, enable, password, ss.getResultCallback());
                break;
            case 10:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CB");
                String facility2 = ss.mParcel.readString();
                String password2 = ss.mParcel.readString();
                getFacilityLock(facility2, password2, ss.getResultCallback());
                break;
            case 11:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CF");
                int commandInterfaceCFAction = ss.mParcel.readInt();
                int commandInterfaceCFReason = ss.mParcel.readInt();
                String dialingNumber = ss.mParcel.readString();
                int timerSeconds = ss.mParcel.readInt();
                setCallForwardingOption(commandInterfaceCFAction, commandInterfaceCFReason, dialingNumber, timerSeconds, ss.getResultCallback());
                break;
            case 12:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CF");
                int commandInterfaceCFReason2 = ss.mParcel.readInt();
                getCallForwardingOption(commandInterfaceCFReason2, ss.getResultCallback());
                break;
            case 13:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_SET_CW");
                int enableState2 = ss.mParcel.readInt();
                boolean enable2 = enableState2 != 0 ? true : VDBG;
                setCallWaiting(enable2, ss.getResultCallback());
                break;
            case 14:
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_GET_CW");
                getCallWaiting(ss.getResultCallback());
                break;
            case 15:
                String dialString = ss.mParcel.readString();
                Rlog.d(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: dialString = " + dialString);
                try {
                    dial(dialString, 0);
                } catch (CallStateException ex) {
                    Rlog.e(LOG_TAG, "handleImsUtCsfb: SUPP_SRV_REQ_MMI_CODE: CallStateException!");
                    ex.printStackTrace();
                }
                break;
        }
        ss.setResultCallback(null);
        ss.mParcel.recycle();
    }

    private void handleUssiCsfb(String dialString) {
        Rlog.d(LOG_TAG, "handleUssiCsfb: dialString=" + dialString);
        try {
            dial(dialString, 0);
        } catch (CallStateException ex) {
            Rlog.e(LOG_TAG, "handleUssiCsfb: CallStateException!");
            ex.printStackTrace();
        }
    }

    @Override
    public void getAvailableNetworks(Message response) {
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            this.mCi.getAvailableNetworks(response);
        } else {
            loge("getAvailableNetworks: not possible in CDMA");
        }
    }

    @Override
    public void getNeighboringCids(Message response) {
        if (isPhoneTypeGsm()) {
            this.mCi.getNeighboringCids(response);
        } else {
            if (response == null) {
                return;
            }
            CommandException ce = new CommandException(CommandException.Error.REQUEST_NOT_SUPPORTED);
            AsyncResult.forMessage(response).exception = ce;
            response.sendToTarget();
        }
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        if (this.mImsPhone == null) {
            return;
        }
        this.mImsPhone.setUiTTYMode(uiTtyMode, onComplete);
    }

    @Override
    public void setMute(boolean muted) {
        this.mCT.setMute(muted);
    }

    @Override
    public boolean getMute() {
        return this.mCT.getMute();
    }

    @Override
    public void getDataCallList(Message response) {
        this.mCi.getDataCallList(response);
    }

    @Override
    public void updateServiceLocation() {
        this.mSST.enableSingleLocationUpdate();
    }

    @Override
    public void enableLocationUpdates() {
        this.mSST.enableLocationUpdates();
    }

    @Override
    public void disableLocationUpdates() {
        this.mSST.disableLocationUpdates();
    }

    @Override
    public boolean getDataRoamingEnabled() {
        return this.mDcTracker.getDataOnRoamingEnabled();
    }

    @Override
    public void setDataRoamingEnabled(boolean enable) {
        this.mDcTracker.setDataOnRoamingEnabled(enable);
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        this.mCi.registerForCdmaOtaProvision(h, what, obj);
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler h) {
        this.mCi.unregisterForCdmaOtaProvision(h);
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        this.mSST.registerForSubscriptionInfoReady(h, what, obj);
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mSST.unregisterForSubscriptionInfoReady(h);
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        this.mEcmExitRespRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        this.mEcmExitRespRegistrant.clear();
    }

    @Override
    public void registerForCallWaiting(Handler h, int what, Object obj) {
        this.mCT.registerForCallWaiting(h, what, obj);
    }

    @Override
    public void unregisterForCallWaiting(Handler h) {
        this.mCT.unregisterForCallWaiting(h);
    }

    @Override
    public boolean getDataEnabled() {
        return this.mDcTracker.getDataEnabled();
    }

    @Override
    public void setDataEnabled(boolean enable) {
        this.mDcTracker.setDataEnabled(enable);
    }

    public void onMMIDone(MmiCode mmi) {
        Rlog.d(LOG_TAG, "onMMIDone: " + mmi);
        dumpPendingMmi();
        if (!this.mPendingMMIs.remove(mmi)) {
            if (!isPhoneTypeGsm()) {
                return;
            }
            if (!mmi.isUssdRequest() && !((GsmMmiCode) mmi).isSsInfo()) {
                return;
            }
        }
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    public void onMMIDone(GsmMmiCode mmi, Object obj) {
        Rlog.d(LOG_TAG, "onMMIDone: " + mmi + ", obj=" + obj);
        dumpPendingMmi();
        if (!this.mPendingMMIs.remove(mmi) && !mmi.isUssdRequest() && !mmi.isSsInfo()) {
            return;
        }
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult(obj, mmi, (Throwable) null));
    }

    public void dumpPendingMmi() {
        int size = this.mPendingMMIs.size();
        if (size == 0) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: none");
            return;
        }
        for (int i = 0; i < size; i++) {
            Rlog.d(LOG_TAG, "dumpPendingMmi: " + this.mPendingMMIs.get(i));
        }
    }

    private void onNetworkInitiatedUssd(MmiCode mmi) {
        this.mMmiCompleteRegistrants.notifyRegistrants(new AsyncResult((Object) null, mmi, (Throwable) null));
    }

    private void onIncomingUSSD(int ussdMode, String ussdMessage) {
        if (!isPhoneTypeGsm()) {
            loge("onIncomingUSSD: not expected on GSM");
        }
        boolean isUssdRequest = ussdMode == 1 ? true : VDBG;
        boolean isUssdError = (ussdMode == 4 || ussdMode == 5) ? true : VDBG;
        boolean isUssdhandleByStk = ussdMode == 3 ? true : VDBG;
        boolean isUssdRelease = ussdMode == 2 ? true : VDBG;
        GsmMmiCode found = null;
        Rlog.d(LOG_TAG, "USSD:mPendingMMIs= " + this.mPendingMMIs + " size=" + this.mPendingMMIs.size());
        int i = 0;
        int s = this.mPendingMMIs.size();
        while (true) {
            if (i >= s) {
                break;
            }
            Rlog.d(LOG_TAG, "i= " + i + " isPending=" + ((GsmMmiCode) this.mPendingMMIs.get(i)).isPendingUSSD());
            if (!((GsmMmiCode) this.mPendingMMIs.get(i)).isPendingUSSD()) {
                i++;
            } else {
                found = (GsmMmiCode) this.mPendingMMIs.get(i);
                Rlog.d(LOG_TAG, "found = " + found);
                break;
            }
        }
        if (found != null) {
            Rlog.d(LOG_TAG, "setUserInitiatedMMI  TRUE");
            found.setUserInitiatedMMI(true);
            if (isUssdRelease && this.mIsNetworkInitiatedUssd) {
                Rlog.d(LOG_TAG, "onIncomingUSSD(): USSD_MODE_NW_RELEASE.");
                found.onUssdRelease();
            } else if (isUssdError) {
                found.onUssdFinishedError();
            } else if (isUssdhandleByStk) {
                found.onUssdStkHandling(ussdMessage, isUssdRequest);
            } else {
                found.onUssdFinished(ussdMessage, isUssdRequest);
            }
        } else {
            Rlog.d(LOG_TAG, "The default value of UserInitiatedMMI is FALSE");
            this.mIsNetworkInitiatedUssd = true;
            Rlog.d(LOG_TAG, "onIncomingUSSD(): Network Initialized USSD");
            if (!isUssdError && ussdMessage != null) {
                GsmMmiCode mmi = GsmMmiCode.newNetworkInitiatedUssd(ussdMessage, isUssdRequest, this, this.mUiccApplication.get());
                onNetworkInitiatedUssd(mmi);
            } else if (isUssdError) {
                GsmMmiCode mmi2 = GsmMmiCode.newNetworkInitiatedUssdError(ussdMessage, isUssdRequest, this, this.mUiccApplication.get());
                onNetworkInitiatedUssd(mmi2);
            }
        }
        if (!isUssdRelease && !isUssdError) {
            return;
        }
        this.mIsNetworkInitiatedUssd = VDBG;
    }

    private void syncClirSetting() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        int clirSetting = sp.getInt(Phone.CLIR_KEY + getPhoneId(), -1);
        if (clirSetting < 0) {
            return;
        }
        this.mCi.setCLIR(clirSetting, null);
    }

    private void handleRadioAvailable() {
        this.mCi.getBasebandVersion(obtainMessage(6));
        if (isPhoneTypeGsm()) {
            this.mCi.getIMEI(obtainMessage(9));
            this.mCi.getIMEISV(obtainMessage(10));
        } else {
            this.mCi.getDeviceIdentity(obtainMessage(21));
        }
        this.mCi.getRadioCapability(obtainMessage(35));
        TelephonyDevController.getInstance();
        TelephonyDevController.registerRIL(this.mCi);
        startLceAfterRadioIsAvailable();
    }

    private void handleRadioOn() {
        this.mCi.getVoiceRadioTechnology(obtainMessage(40));
        if (!isPhoneTypeGsm()) {
            this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
        }
        setPreferredNetworkTypeIfSimLoaded();
    }

    private void handleRadioOffOrNotAvailable() {
        if (isPhoneTypeGsm()) {
            for (int i = this.mPendingMMIs.size() - 1; i >= 0; i--) {
                if (((GsmMmiCode) this.mPendingMMIs.get(i)).isPendingUSSD()) {
                    ((GsmMmiCode) this.mPendingMMIs.get(i)).onUssdFinishedError();
                }
            }
        }
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null && !imsPhone.isWifiCallingEnabled()) {
            imsPhone.getServiceState().setState(1);
        }
        this.mRadioOffOrNotAvailableRegistrants.notifyRegistrants();
    }

    @Override
    public void handleMessage(Message msg) {
        Connection cn;
        switch (msg.what) {
            case 1:
                handleRadioAvailable();
                break;
            case 2:
                logd("Event EVENT_SSN Received");
                if (isPhoneTypeGsm()) {
                    AsyncResult ar = (AsyncResult) msg.obj;
                    this.mCachedSsn = ar;
                    this.mSsnRegistrants.notifyRegistrants(ar);
                }
                break;
            case 3:
                if (isPhoneTypeGsm()) {
                    updateCurrentCarrierInProvider();
                    String imsi = getVmSimImsi();
                    String imsiFromSIM = getSubscriberId();
                    if (imsi != null && imsiFromSIM != null && !imsiFromSIM.equals(imsi)) {
                        storeVoiceMailNumber(null);
                        setVmSimImsi(null);
                    }
                }
                this.mSimRecordsLoadedRegistrants.notifyRegistrants();
                break;
            case 5:
                logd("Event EVENT_RADIO_ON Received");
                handleRadioOn();
                break;
            case 6:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception == null) {
                    logd("Baseband version: " + ar2.result);
                    TelephonyManager.from(this.mContext).setBasebandVersionForPhone(getPhoneId(), (String) ar2.result);
                }
                break;
            case 7:
                String[] ussdResult = (String[]) ((AsyncResult) msg.obj).result;
                if (ussdResult.length > 1) {
                    try {
                        onIncomingUSSD(Integer.parseInt(ussdResult[0]), ussdResult[1]);
                    } catch (NumberFormatException e) {
                        Rlog.w(LOG_TAG, "error parsing USSD");
                        return;
                    }
                }
                break;
            case 8:
                logd("Event EVENT_RADIO_OFF_OR_NOT_AVAILABLE Received");
                handleRadioOffOrNotAvailable();
                break;
            case 9:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception != null) {
                    Rlog.e(LOG_TAG, "Invalid DeviceId (IMEI)");
                    setDeviceIdAbnormal(1);
                } else {
                    this.mImei = (String) ar3.result;
                    Rlog.d(LOG_TAG, "IMEI: ****" + this.mImei.substring(10));
                    try {
                        Long.parseLong(this.mImei);
                        setDeviceIdAbnormal(0);
                    } catch (NumberFormatException e2) {
                        setDeviceIdAbnormal(1);
                        Rlog.e(LOG_TAG, "Invalid DeviceId (IMEI) Format: " + e2.toString() + ")");
                        return;
                    }
                }
                break;
            case 10:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    this.mImeiSv = (String) ar4.result;
                }
                break;
            case 12:
                AsyncResult ar5 = (AsyncResult) msg.obj;
                IccRecords r = this.mIccRecords.get();
                Cfu cfu = (Cfu) ar5.userObj;
                if (ar5.exception == null && r != null) {
                    if (!queryCFUAgainAfterSet()) {
                        setVoiceCallForwardingFlag(1, msg.arg1 == 1 ? true : VDBG, cfu.mSetCfNumber);
                    } else if (ar5.result != null) {
                        CallForwardInfo[] cfinfo = (CallForwardInfo[]) ar5.result;
                        if (cfinfo == null || cfinfo.length == 0) {
                            Rlog.d(LOG_TAG, "cfinfo is null or length is 0.");
                        } else {
                            Rlog.d(LOG_TAG, "[EVENT_SET_CALL_FORWARD_DONE check cfinfo");
                            int i = 0;
                            while (true) {
                                if (i < cfinfo.length) {
                                    if ((cfinfo[i].serviceClass & 1) != 0) {
                                        setVoiceCallForwardingFlag(1, cfinfo[i].status == 1 ? true : VDBG, cfinfo[i].number);
                                    } else {
                                        i++;
                                    }
                                }
                            }
                        }
                    } else {
                        Rlog.e(LOG_TAG, "EVENT_SET_CALL_FORWARD_DONE: ar.result is null.");
                    }
                }
                if (ar5.exception != null && msg.arg2 != 0) {
                    if (msg.arg2 == 1) {
                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                    } else {
                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                    }
                }
                if (cfu.mOnComplete != null) {
                    AsyncResult.forMessage(cfu.mOnComplete, ar5.result, ar5.exception);
                    cfu.mOnComplete.sendToTarget();
                }
                break;
            case 13:
                Rlog.d(LOG_TAG, "mPhoneId= " + this.mPhoneId + "subId=" + getSubId());
                setSystemProperty(CFU_QUERY_PROPERTY_NAME + this.mPhoneId, "0");
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.exception == null) {
                    handleCfuQueryResult((CallForwardInfo[]) ar6.result);
                }
                Message onComplete = (Message) ar6.userObj;
                if (onComplete != null) {
                    AsyncResult.forMessage(onComplete, ar6.result, ar6.exception);
                    onComplete.sendToTarget();
                }
                break;
            case 18:
                AsyncResult ar7 = (AsyncResult) msg.obj;
                if (ar7.exception == null) {
                    saveClirSetting(msg.arg1);
                }
                Message onComplete2 = (Message) ar7.userObj;
                if (onComplete2 != null) {
                    AsyncResult.forMessage(onComplete2, ar7.result, ar7.exception);
                    onComplete2.sendToTarget();
                }
                break;
            case 19:
                logd("Event EVENT_REGISTERED_TO_NETWORK Received");
                if (isPhoneTypeGsm()) {
                    syncClirSetting();
                }
                sendMessage(obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE));
                break;
            case 20:
                AsyncResult ar8 = (AsyncResult) msg.obj;
                if ((isPhoneTypeGsm() && IccVmNotSupportedException.class.isInstance(ar8.exception)) || (!isPhoneTypeGsm() && IccException.class.isInstance(ar8.exception))) {
                    storeVoiceMailNumber(this.mVmNumber);
                    ar8.exception = null;
                }
                Message onComplete3 = (Message) ar8.userObj;
                if (onComplete3 != null) {
                    AsyncResult.forMessage(onComplete3, ar8.result, ar8.exception);
                    onComplete3.sendToTarget();
                }
                break;
            case 21:
                AsyncResult ar9 = (AsyncResult) msg.obj;
                if (ar9.exception == null) {
                    String[] respId = (String[]) ar9.result;
                    this.mImei = respId[0];
                    this.mImeiSv = respId[1];
                    this.mEsn = respId[2];
                    this.mMeid = respId[3];
                    setDeviceIdAbnormal(0);
                } else {
                    setDeviceIdAbnormal(1);
                    Rlog.e(LOG_TAG, "Invalid Device Id");
                }
                break;
            case 22:
                logd("Event EVENT_RUIM_RECORDS_LOADED Received");
                updateCurrentCarrierInProvider();
                break;
            case 25:
                handleEnterEmergencyCallbackMode(msg);
                break;
            case 26:
                handleExitEmergencyCallbackMode(msg);
                break;
            case CallFailCause.DESTINATION_OUT_OF_ORDER:
                logd("EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED");
                this.mCdmaSubscriptionSource = this.mCdmaSSM.getCdmaSubscriptionSource();
                break;
            case CallFailCause.INVALID_NUMBER_FORMAT:
                AsyncResult ar10 = (AsyncResult) msg.obj;
                if (!this.mSST.mSS.getIsManualSelection()) {
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: already automatic, ignore");
                } else {
                    setNetworkSelectionModeAutomatic((Message) ar10.result);
                    logd("SET_NETWORK_SELECTION_AUTOMATIC: set to automatic");
                }
                break;
            case CallFailCause.FACILITY_REJECTED:
                Rlog.d(LOG_TAG, "EVENT_ICC_RECORD_EVENTS");
                processIccRecordEvents(((Integer) ((AsyncResult) msg.obj).result).intValue());
                break;
            case 35:
                AsyncResult ar11 = (AsyncResult) msg.obj;
                RadioCapability rc = (RadioCapability) ar11.result;
                if (ar11.exception != null) {
                    Rlog.d(LOG_TAG, "get phone radio capability fail, no need to change mRadioCapability");
                } else {
                    radioCapabilityUpdated(rc);
                }
                Rlog.d(LOG_TAG, "EVENT_GET_RADIO_CAPABILITY: phone rc: " + rc);
                break;
            case 36:
                AsyncResult ar12 = (AsyncResult) msg.obj;
                logd("Event EVENT_SS received");
                if (isPhoneTypeGsm()) {
                    GsmMmiCode mmi = new GsmMmiCode(this, this.mUiccApplication.get());
                    mmi.processSsData(ar12);
                }
                break;
            case 39:
            case 40:
                String what = msg.what == 39 ? "EVENT_VOICE_RADIO_TECH_CHANGED" : "EVENT_REQUEST_VOICE_RADIO_TECH_DONE";
                AsyncResult ar13 = (AsyncResult) msg.obj;
                if (ar13.exception != null) {
                    loge(what + ": exception=" + ar13.exception);
                } else if (ar13.result == null || ((int[]) ar13.result).length == 0) {
                    loge(what + ": has no tech!");
                } else {
                    int newVoiceTech = ((int[]) ar13.result)[0];
                    logd(what + ": newVoiceTech=" + newVoiceTech);
                    phoneObjectUpdater(newVoiceTech);
                }
                break;
            case 41:
                AsyncResult ar14 = (AsyncResult) msg.obj;
                if (ar14.exception == null && ar14.result != null) {
                    this.mRilVersion = ((Integer) ar14.result).intValue();
                } else {
                    logd("Unexpected exception on EVENT_RIL_CONNECTED");
                    this.mRilVersion = -1;
                }
                break;
            case 42:
                phoneObjectUpdater(msg.arg1);
                break;
            case CallFailCause.ACCESS_INFORMATION_DISCARDED:
                if (!this.mContext.getResources().getBoolean(R.^attr-private.minorWeightMin)) {
                    this.mCi.getVoiceRadioTechnology(obtainMessage(40));
                }
                ImsManager.updateImsServiceConfig(this.mContext, this.mPhoneId, true);
                CarrierConfigManager configMgr = (CarrierConfigManager) getContext().getSystemService("carrier_config");
                PersistableBundle b = configMgr.getConfigForSubId(getSubId());
                if (b != null) {
                    boolean broadcastEmergencyCallStateChanges = b.getBoolean("broadcast_emergency_call_state_changes_bool");
                    logd("broadcastEmergencyCallStateChanges =" + broadcastEmergencyCallStateChanges);
                    setBroadcastEmergencyCallStateChanges(broadcastEmergencyCallStateChanges);
                } else {
                    loge("didn't get broadcastEmergencyCallStateChanges from carrier config");
                }
                if (b != null) {
                    int config_cdma_roaming_mode = b.getInt("cdma_roaming_mode_int");
                    int current_cdma_roaming_mode = Settings.Global.getInt(getContext().getContentResolver(), "roaming_settings", -1);
                    switch (config_cdma_roaming_mode) {
                        case -1:
                            if (current_cdma_roaming_mode != config_cdma_roaming_mode) {
                                logd("cdma_roaming_mode is going to changed to " + current_cdma_roaming_mode);
                                setCdmaRoamingPreference(current_cdma_roaming_mode, obtainMessage(44));
                            }
                            loge("Invalid cdma_roaming_mode settings: " + config_cdma_roaming_mode);
                            break;
                        case 0:
                        case 1:
                        case 2:
                            logd("cdma_roaming_mode is going to changed to " + config_cdma_roaming_mode);
                            setCdmaRoamingPreference(config_cdma_roaming_mode, obtainMessage(44));
                            break;
                        default:
                            loge("Invalid cdma_roaming_mode settings: " + config_cdma_roaming_mode);
                            break;
                    }
                } else {
                    loge("didn't get the cdma_roaming_mode changes from the carrier config.");
                }
                prepareEri();
                if (!isPhoneTypeGsm()) {
                    this.mSST.pollState();
                }
                break;
            case CallFailCause.CHANNEL_NOT_AVAIL:
                logd("cdma_roaming_mode change is done");
                break;
            case CharacterSets.ISO_8859_13:
                Rlog.d(LOG_TAG, "mPhoneId = " + this.mPhoneId + ", subId = " + getSubId());
                setSystemProperty(CFU_QUERY_PROPERTY_NAME + this.mPhoneId, "0");
                AsyncResult ar15 = (AsyncResult) msg.obj;
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]ar.exception = " + ar15.exception);
                if (ar15.exception == null) {
                    handleCfuInTimeSlotQueryResult((CallForwardInfoEx[]) ar15.result);
                }
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE]msg.arg1 = " + msg.arg1);
                if (ar15.exception != null && (ar15.exception instanceof CommandException)) {
                    CommandException cmdException = (CommandException) ar15.exception;
                    if (msg.arg1 == 1 && cmdException != null && cmdException.getCommandError() == CommandException.Error.SPECAIL_UT_COMMAND_NOT_SUPPORTED && this.mSST != null && this.mSST.mSS != null && this.mSST.mSS.getState() == 0) {
                        getCallForwardingOption(0, obtainMessage(13));
                    }
                }
                Message onComplete4 = (Message) ar15.userObj;
                if (onComplete4 != null) {
                    AsyncResult.forMessage(onComplete4, ar15.result, ar15.exception);
                    onComplete4.sendToTarget();
                }
                break;
            case CharacterSets.ISO_8859_14:
                AsyncResult ar16 = (AsyncResult) msg.obj;
                IccRecords records = this.mIccRecords.get();
                CfuEx cfuEx = (CfuEx) ar16.userObj;
                if (ar16.exception == null && records != null) {
                    records.setVoiceCallForwardingFlag(1, msg.arg1 == 1 ? true : VDBG, cfuEx.mSetCfNumber);
                    saveTimeSlot(cfuEx.mSetTimeSlot);
                    if (msg.arg1 == 1) {
                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                    } else {
                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                    }
                }
                if (cfuEx.mOnComplete != null) {
                    AsyncResult.forMessage(cfuEx.mOnComplete, ar16.result, ar16.exception);
                    cfuEx.mOnComplete.sendToTarget();
                }
                break;
            case 301:
                AsyncResult ar17 = (AsyncResult) msg.obj;
                Rlog.d(LOG_TAG, "[EVENT_GET_CALL_WAITING_]ar.exception = " + ar17.exception);
                Message onComplete5 = (Message) ar17.userObj;
                if (ar17.exception == null) {
                    int[] cwArray = (int[]) ar17.result;
                    try {
                        Rlog.d(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE cwArray[0]:cwArray[1] = " + cwArray[0] + ":" + cwArray[1]);
                        boolean csEnable = (cwArray[0] == 1 && (cwArray[1] & 1) == 1) ? true : VDBG;
                        setTerminalBasedCallWaiting(csEnable, null);
                        if (onComplete5 != null) {
                            AsyncResult.forMessage(onComplete5, ar17.result, (Throwable) null);
                            onComplete5.sendToTarget();
                        }
                    } catch (ArrayIndexOutOfBoundsException e3) {
                        Rlog.e(LOG_TAG, "EVENT_GET_CALL_WAITING_DONE: improper result: err =" + e3.getMessage());
                        if (onComplete5 != null) {
                            AsyncResult.forMessage(onComplete5, ar17.result, (Throwable) null);
                            onComplete5.sendToTarget();
                            return;
                        }
                        return;
                    }
                } else if (onComplete5 != null) {
                    AsyncResult.forMessage(onComplete5, ar17.result, ar17.exception);
                    onComplete5.sendToTarget();
                }
                break;
            case 302:
                AsyncResult ar18 = (AsyncResult) msg.obj;
                Message onComplete6 = (Message) ar18.userObj;
                if (ar18.exception == null) {
                    boolean enable = msg.arg1 == 1 ? true : VDBG;
                    setTerminalBasedCallWaiting(enable, onComplete6);
                } else {
                    Rlog.d(LOG_TAG, "EVENT_SET_CALL_WAITING_DONE: ar.exception=" + ar18.exception);
                    if (onComplete6 != null) {
                        AsyncResult.forMessage(onComplete6, ar18.result, ar18.exception);
                        onComplete6.sendToTarget();
                    }
                }
                break;
            case 1002:
                Rlog.d(LOG_TAG, "handle EVENT_VOICE_CALL_INCOMING_INDICATION");
                this.mVoiceCallIncomingIndicationRegistrants.notifyRegistrants(new AsyncResult((Object) null, this, (Throwable) null));
                break;
            case 1003:
                AsyncResult ar19 = (AsyncResult) msg.obj;
                SuppCrssNotification noti = (SuppCrssNotification) ar19.result;
                if (noti.code == 2) {
                    if (getRingingCall().getState() != Call.State.IDLE) {
                        Connection cn2 = getRingingCall().getConnections().get(0);
                        Rlog.d(LOG_TAG, "set number presentation to connection : " + noti.cli_validity);
                        switch (noti.cli_validity) {
                            case 1:
                                cn2.setNumberPresentation(2);
                                break;
                            case 2:
                                cn2.setNumberPresentation(3);
                                break;
                            case 3:
                                cn2.setNumberPresentation(4);
                                break;
                            default:
                                cn2.setNumberPresentation(1);
                                break;
                        }
                    }
                } else if (noti.code == 3) {
                    Rlog.d(LOG_TAG, "[COLP]noti.number = " + noti.number);
                    if (getForegroundCall().getState() != Call.State.IDLE && (cn = getForegroundCall().getConnections().get(0)) != null && cn.getAddress() != null && !cn.getAddress().equals(noti.number)) {
                        cn.setRedirectingAddress(noti.number);
                        Rlog.d(LOG_TAG, "[COLP]Redirecting address = " + cn.getRedirectingAddress());
                    }
                }
                this.mCachedCrssn = ar19;
                this.mCallRelatedSuppSvcRegistrants.notifyRegistrants(ar19);
                break;
            case ServiceStateTracker.NITZ_UPDATE_DIFF_DEFAULT:
                AsyncResult ar20 = (AsyncResult) msg.obj;
                if (ar20 != null) {
                    SuppSrvRequest ss = (SuppSrvRequest) ar20.userObj;
                    if (ss == null) {
                        Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error SuppSrvRequest null!");
                    } else if (17 != ss.getRequestCode()) {
                        CommandException cmdException2 = null;
                        ImsException imsException = null;
                        if (ar20.exception != null && (ar20.exception instanceof CommandException)) {
                            cmdException2 = (CommandException) ar20.exception;
                        }
                        if (ar20.exception != null && (ar20.exception instanceof ImsException)) {
                            imsException = (ImsException) ar20.exception;
                        }
                        if (cmdException2 != null && cmdException2.getCommandError() == CommandException.Error.UT_XCAP_403_FORBIDDEN) {
                            setCsFallbackStatus(2);
                            Message msgCSFB = obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START, ss);
                            sendMessage(msgCSFB);
                        } else if (cmdException2 != null && cmdException2.getCommandError() == CommandException.Error.UT_UNKNOWN_HOST) {
                            setCsFallbackStatus(1);
                            Message msgCSFB2 = obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START, ss);
                            sendMessage(msgCSFB2);
                        } else if (imsException != null && imsException.getCode() == 830) {
                            setCsFallbackStatus(2);
                            Message msgCSFB3 = obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START, ss);
                            sendMessage(msgCSFB3);
                        } else if (imsException != null && imsException.getCode() == 831) {
                            setCsFallbackStatus(1);
                            Message msgCSFB4 = obtainMessage(TelephonyEventLog.TAG_IMS_CALL_START, ss);
                            sendMessage(msgCSFB4);
                        } else {
                            if (ar20.exception == null && 11 == ss.getRequestCode()) {
                                ss.mParcel.setDataPosition(0);
                                Rlog.d(LOG_TAG, "EVENT_IMS_UT_DONE: SUPP_SRV_REQ_SET_CF");
                                int commandInterfaceCFAction = ss.mParcel.readInt();
                                int commandInterfaceCFReason = ss.mParcel.readInt();
                                ss.mParcel.readString();
                                if (commandInterfaceCFReason == 0) {
                                    if (queryCFUAgainAfterSet()) {
                                        if (ar20.result != null) {
                                            CallForwardInfo[] cfinfo2 = (CallForwardInfo[]) ar20.result;
                                            if (cfinfo2 == null || cfinfo2.length == 0) {
                                                Rlog.d(LOG_TAG, "cfinfo is null or 0.");
                                            } else {
                                                int i2 = 0;
                                                while (true) {
                                                    if (i2 < cfinfo2.length) {
                                                        if ((cfinfo2[i2].serviceClass & 1) == 0) {
                                                            i2++;
                                                        } else if (cfinfo2[i2].status == 1) {
                                                            Rlog.d(LOG_TAG, "Set enable, serviceClass: " + cfinfo2[i2].serviceClass);
                                                            setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                                                        } else {
                                                            Rlog.d(LOG_TAG, "Set disable, serviceClass: " + cfinfo2[i2].serviceClass);
                                                            setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Rlog.d(LOG_TAG, "ar.result is null.");
                                        }
                                    } else if (isCfEnable(commandInterfaceCFAction)) {
                                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                                    } else {
                                        setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                                    }
                                }
                            } else if (imsException == null || imsException.getCode() != 832) {
                                if (cmdException2 == null || cmdException2.getCommandError() != CommandException.Error.UT_XCAP_404_NOT_FOUND) {
                                    if (imsException == null || imsException.getCode() != 833) {
                                        if (cmdException2 != null && cmdException2.getCommandError() == CommandException.Error.UT_XCAP_409_CONFLICT) {
                                            if (isEnableXcapHttpResponse409()) {
                                                Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT.");
                                            } else {
                                                Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT, return GENERIC_FAILURE");
                                                ar20.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
                                            }
                                        }
                                    } else if (isEnableXcapHttpResponse409()) {
                                        Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT.");
                                        ar20.exception = new CommandException(CommandException.Error.UT_XCAP_409_CONFLICT);
                                    } else {
                                        Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_409_CONFLICT, return GENERIC_FAILURE");
                                        ar20.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
                                    }
                                } else if (isOpTransferXcap404() && (ss.getRequestCode() == 10 || ss.getRequestCode() == 9)) {
                                    Rlog.d(LOG_TAG, "GSMPhone get UT_XCAP_404_NOT_FOUND.");
                                } else {
                                    ar20.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
                                }
                            } else if (isOpTransferXcap404() && (ss.getRequestCode() == 10 || ss.getRequestCode() == 9)) {
                                ar20.exception = new CommandException(CommandException.Error.UT_XCAP_404_NOT_FOUND);
                            } else {
                                ar20.exception = new CommandException(CommandException.Error.GENERIC_FAILURE);
                            }
                            Message onComplete7 = ss.getResultCallback();
                            if (onComplete7 != null) {
                                AsyncResult.forMessage(onComplete7, ar20.result, ar20.exception);
                                onComplete7.sendToTarget();
                            }
                            ss.mParcel.recycle();
                        }
                    } else {
                        if (ar20.exception == null) {
                            ss.mParcel.setDataPosition(0);
                            Rlog.d(LOG_TAG, "EVENT_IMS_UT_DONE: SUPP_SRV_REQ_SET_CF_IN_TIME_SLOT");
                            int commandInterfaceCFAction2 = ss.mParcel.readInt();
                            int commandInterfaceCFReason2 = ss.mParcel.readInt();
                            ss.mParcel.readString();
                            if (commandInterfaceCFReason2 == 0) {
                                if (isCfEnable(commandInterfaceCFAction2)) {
                                    setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_on");
                                } else {
                                    setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
                                }
                            }
                        }
                        Message onComplete8 = ss.getResultCallback();
                        if (onComplete8 != null) {
                            AsyncResult.forMessage(onComplete8, ar20.result, ar20.exception);
                            onComplete8.sendToTarget();
                        }
                        ss.mParcel.recycle();
                    }
                } else {
                    Rlog.e(LOG_TAG, "EVENT_IMS_UT_DONE: Error AsyncResult null!");
                }
                break;
            case TelephonyEventLog.TAG_IMS_CALL_START:
                handleImsUtCsfb(msg);
                break;
            case TelephonyEventLog.TAG_IMS_CALL_START_CONFERENCE:
                Rlog.d(LOG_TAG, "Receive EVENT_QUERY_CFU phoneid: " + getPhoneId() + " needQueryCfu:" + this.needQueryCfu);
                if (this.needQueryCfu) {
                    String defaultQueryCfuMode = "0";
                    if (this.mSupplementaryServiceExt != null) {
                        defaultQueryCfuMode = this.mSupplementaryServiceExt.getOpDefaultQueryCfuMode();
                        Rlog.d(LOG_TAG, "defaultQueryCfuMode = " + defaultQueryCfuMode);
                    }
                    String cfuSetting = !TelephonyManager.from(this.mContext).isVoiceCapable() ? SystemProperties.get("persist.radio.cfu.querytype", "1") : SystemProperties.get("persist.radio.cfu.querytype", defaultQueryCfuMode);
                    String isTestSim = "0";
                    boolean isRRMEnv = VDBG;
                    if (this.mPhoneId == 0) {
                        isTestSim = SystemProperties.get("gsm.sim.ril.testsim", "0");
                    } else if (this.mPhoneId == 1) {
                        isTestSim = SystemProperties.get("gsm.sim.ril.testsim.2", "0");
                    }
                    String operatorNumeric = getServiceState().getOperatorNumeric();
                    if (operatorNumeric != null && operatorNumeric.equals("46602")) {
                        isRRMEnv = true;
                    }
                    Rlog.d(LOG_TAG, "[GSMPhone] CFU_KEY = " + cfuSetting + " isTestSIM : " + isTestSim + " isRRMEnv : " + isRRMEnv + " phoneid: " + getPhoneId());
                    if (isTestSim.equals("0") && !isRRMEnv) {
                        String isChangedProp = CFU_QUERY_SIM_CHANGED_PROP + getPhoneId();
                        String isChanged = SystemProperties.get(isChangedProp, "0");
                        Rlog.d(LOG_TAG, "[GSMPhone] isChanged " + isChanged);
                        if (cfuSetting.equals(Phone.ACT_TYPE_UTRAN) || (cfuSetting.equals("0") && isChanged.equals("1"))) {
                            this.mCfuQueryRetryCount = 0;
                            queryCfuOrWait();
                            this.needQueryCfu = VDBG;
                            SystemProperties.set(isChangedProp, "0");
                        } else {
                            String utCfuMode = getSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                            Rlog.d(LOG_TAG, "utCfuMode: " + utCfuMode);
                            if ("enabled_ut_cfu_mode_on".equals(utCfuMode)) {
                                IccRecords r2 = this.mIccRecords.get();
                                if (r2 != null) {
                                    setVoiceCallForwardingFlag(1, true, UsimPBMemInfo.STRING_NOT_SET);
                                }
                            } else if ("enabled_ut_cfu_mode_off".equals(utCfuMode)) {
                                IccRecords r3 = this.mIccRecords.get();
                                if (r3 != null) {
                                    setVoiceCallForwardingFlag(1, VDBG, UsimPBMemInfo.STRING_NOT_SET);
                                }
                            }
                        }
                    } else {
                        String utCfuMode2 = getSystemProperty("persist.radio.ut.cfu.mode", "disabled_ut_cfu_mode");
                        Rlog.d(LOG_TAG, "utCfuMode: " + utCfuMode2);
                        if ("enabled_ut_cfu_mode_on".equals(utCfuMode2)) {
                            IccRecords r4 = this.mIccRecords.get();
                            if (r4 != null) {
                                setVoiceCallForwardingFlag(1, true, UsimPBMemInfo.STRING_NOT_SET);
                            }
                        } else if ("enabled_ut_cfu_mode_off".equals(utCfuMode2)) {
                            IccRecords r5 = this.mIccRecords.get();
                            if (r5 != null) {
                                setVoiceCallForwardingFlag(1, VDBG, UsimPBMemInfo.STRING_NOT_SET);
                            }
                        }
                    }
                }
                break;
            case TelephonyEventLog.TAG_IMS_CALL_RECEIVE:
                handleUssiCsfb((String) msg.obj);
                break;
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public UiccCardApplication getUiccCardApplication() {
        if (isPhoneTypeGsm()) {
            return this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhoneId, 2);
    }

    @Override
    protected void onUpdateIccAvailability() {
        if (this.mUiccController == null) {
            return;
        }
        if (isPhoneTypeGsm() || isPhoneTypeCdmaLte()) {
            UiccCardApplication newUiccApplication = this.mUiccController.getUiccCardApplication(this.mPhoneId, 3);
            IsimUiccRecords newIsimUiccRecords = null;
            if (newUiccApplication != null) {
                newIsimUiccRecords = (IsimUiccRecords) newUiccApplication.getIccRecords();
                logd("New ISIM application found");
            }
            this.mIsimUiccRecords = newIsimUiccRecords;
        }
        if (this.mSimRecords != null) {
            this.mSimRecords.unregisterForRecordsLoaded(this);
        }
        if (isPhoneTypeCdmaLte()) {
            UiccCardApplication newUiccApplication2 = this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
            SIMRecords newSimRecords = newUiccApplication2 != null ? (SIMRecords) newUiccApplication2.getIccRecords() : null;
            this.mSimRecords = newSimRecords;
            if (this.mSimRecords != null) {
                this.mSimRecords.registerForRecordsLoaded(this, 3, null);
            }
        } else {
            this.mSimRecords = null;
        }
        UiccCardApplication newUiccApplication3 = getUiccCardApplication();
        if (!isPhoneTypeGsm() && newUiccApplication3 == null) {
            logd("can't find 3GPP2 application; trying APP_FAM_3GPP");
            newUiccApplication3 = this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
        }
        UiccCardApplication app = this.mUiccApplication.get();
        IccRecords iccRecords = newUiccApplication3 != null ? newUiccApplication3.getIccRecords() : null;
        if (app != newUiccApplication3 || this.mIccRecords.get() != iccRecords) {
            if (app != null) {
                logd("Removing stale icc objects.");
                if (this.mIccRecords.get() != null) {
                    unregisterForIccRecordEvents();
                    this.mIccPhoneBookIntManager.updateIccRecords(null);
                }
                this.mIccRecords.set(null);
                this.mUiccApplication.set(null);
            }
            if (newUiccApplication3 != null) {
                logd("New Uicc application found. type = " + newUiccApplication3.getType());
                this.mUiccApplication.set(newUiccApplication3);
                this.mIccRecords.set(newUiccApplication3.getIccRecords());
                registerForIccRecordEvents();
                this.mIccPhoneBookIntManager.updateIccRecords(this.mIccRecords.get());
            }
        }
        Rlog.d(LOG_TAG, "isPhoneTypeCdmaLte:" + isPhoneTypeCdmaLte() + " isCdmaOnlyCard: " + isCdmaOnlyCard() + " mNewVoiceTech: " + this.mNewVoiceTech);
        if (this.mNewVoiceTech != -1) {
            if (!(isPhoneTypeCdmaLte() && isCdmaOnlyCard()) && (!isPhoneTypeCdma() || isCdmaOnlyCard())) {
                return;
            }
            updatePhoneObject(this.mNewVoiceTech);
        }
    }

    private void processIccRecordEvents(int eventCode) {
        switch (eventCode) {
            case 1:
                Rlog.d(LOG_TAG, "processIccRecordEvents");
                notifyCallForwardingIndicator();
                break;
        }
    }

    @Override
    public boolean updateCurrentCarrierInProvider() {
        if (!isPhoneTypeGsm() && !isPhoneTypeCdmaLte()) {
            return true;
        }
        long currentDds = SubscriptionManager.getDefaultDataSubscriptionId();
        String operatorNumeric = getOperatorNumeric();
        logd("updateCurrentCarrierInProvider: mSubId = " + getSubId() + " currentDds = " + currentDds + " operatorNumeric = " + operatorNumeric);
        if (!TextUtils.isEmpty(operatorNumeric) && getSubId() == currentDds) {
            try {
                Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, Telephony.Carriers.CURRENT);
                ContentValues map = new ContentValues();
                map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                this.mContext.getContentResolver().insert(uri, map);
                return true;
            } catch (SQLException e) {
                Rlog.e(LOG_TAG, "Can't store current operator", e);
                return VDBG;
            }
        }
        return VDBG;
    }

    private boolean updateCurrentCarrierInProvider(String operatorNumeric) {
        if (isPhoneTypeCdma() || (isPhoneTypeCdmaLte() && this.mUiccController.getUiccCardApplication(this.mPhoneId, 1) == null)) {
            logd("CDMAPhone: updateCurrentCarrierInProvider called");
            if (!TextUtils.isEmpty(operatorNumeric)) {
                try {
                    Uri uri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, Telephony.Carriers.CURRENT);
                    ContentValues map = new ContentValues();
                    map.put(Telephony.Carriers.NUMERIC, operatorNumeric);
                    logd("updateCurrentCarrierInProvider from system: numeric=" + operatorNumeric);
                    getContext().getContentResolver().insert(uri, map);
                    logd("update mccmnc=" + operatorNumeric);
                    MccTable.updateMccMncConfiguration(this.mContext, operatorNumeric, VDBG);
                    return true;
                } catch (SQLException e) {
                    Rlog.e(LOG_TAG, "Can't store current operator", e);
                }
            }
            return VDBG;
        }
        logd("updateCurrentCarrierInProvider not updated X retVal=true");
        return true;
    }

    private void handleCfuQueryResult(CallForwardInfo[] infos) {
        boolean z = VDBG;
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return;
        }
        if (infos == null || infos.length == 0) {
            setVoiceCallForwardingFlag(1, VDBG, null);
            setSystemProperty("persist.radio.ut.cfu.mode", "enabled_ut_cfu_mode_off");
            return;
        }
        int s = infos.length;
        for (int i = 0; i < s; i++) {
            if ((infos[i].serviceClass & 1) != 0) {
                if (infos[i].status == 1) {
                    z = true;
                }
                setVoiceCallForwardingFlag(1, z, infos[i].number);
                String mode = infos[i].status == 1 ? "enabled_ut_cfu_mode_on" : "enabled_ut_cfu_mode_off";
                setSystemProperty("persist.radio.ut.cfu.mode", mode);
                return;
            }
        }
    }

    @Override
    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager() {
        return this.mIccPhoneBookIntManager;
    }

    public void registerForEriFileLoaded(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEriFileLoadedRegistrants.add(r);
    }

    public void unregisterForEriFileLoaded(Handler h) {
        this.mEriFileLoadedRegistrants.remove(h);
    }

    public void prepareEri() {
        if (this.mEriManager == null) {
            Rlog.e(LOG_TAG, "PrepareEri: Trying to access stale objects");
            return;
        }
        this.mEriManager.loadEriFile();
        if (!this.mEriManager.isEriFileLoaded()) {
            return;
        }
        logd("ERI read, notify registrants");
        this.mEriFileLoadedRegistrants.notifyRegistrants();
    }

    public boolean isEriFileLoaded() {
        return this.mEriManager.isEriFileLoaded();
    }

    @Override
    public void activateCellBroadcastSms(int activate, Message response) {
        loge("[GsmCdmaPhone] activateCellBroadcastSms() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public void getCellBroadcastSmsConfig(Message response) {
        loge("[GsmCdmaPhone] getCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public void setCellBroadcastSmsConfig(int[] configValuesArray, Message response) {
        loge("[GsmCdmaPhone] setCellBroadcastSmsConfig() is obsolete; use SmsManager");
        response.sendToTarget();
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        if (isPhoneTypeGsm() || this.mSST.getOtasp() == 3) {
            return VDBG;
        }
        return true;
    }

    @Override
    public boolean isCspPlmnEnabled() {
        IccRecords r = this.mIccRecords.get();
        return r != null ? r.isCspPlmnEnabled() : VDBG;
    }

    public boolean isManualNetSelAllowed() {
        int nwMode = Phone.PREFERRED_NT_MODE;
        int subId = getSubId();
        int nwMode2 = Settings.Global.getInt(this.mContext.getContentResolver(), "preferred_network_mode" + subId, nwMode);
        logd("isManualNetSelAllowed in mode = " + nwMode2);
        if (isManualSelProhibitedInGlobalMode() && (nwMode2 == 10 || nwMode2 == 7)) {
            logd("Manual selection not supported in mode = " + nwMode2);
            return VDBG;
        }
        logd("Manual selection is supported in mode = " + nwMode2);
        return true;
    }

    private boolean isManualSelProhibitedInGlobalMode() {
        String[] configArray;
        boolean isProhibited = VDBG;
        String configString = getContext().getResources().getString(R.string.PERSOSUBSTATE_RUIM_CORPORATE_PUK_ERROR);
        if (!TextUtils.isEmpty(configString) && (configArray = configString.split(";")) != null && ((configArray.length == 1 && configArray[0].equalsIgnoreCase("true")) || (configArray.length == 2 && !TextUtils.isEmpty(configArray[1]) && configArray[0].equalsIgnoreCase("true") && isMatchGid(configArray[1])))) {
            isProhibited = true;
        }
        logd("isManualNetSelAllowedInGlobal in current carrier is " + isProhibited);
        return isProhibited;
    }

    private void registerForIccRecordEvents() {
        Rlog.d(LOG_TAG, "registerForIccRecordEvents, phonetype: " + isPhoneTypeGsm());
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return;
        }
        if (isPhoneTypeGsm()) {
            r.registerForNetworkSelectionModeAutomatic(this, 28, null);
            r.registerForRecordsEvents(this, 29, null);
            r.registerForRecordsLoaded(this, 3, null);
            return;
        }
        r.registerForRecordsLoaded(this, 22, null);
    }

    private void unregisterForIccRecordEvents() {
        Rlog.d(LOG_TAG, "unregisterForIccRecordEvents");
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            return;
        }
        r.unregisterForNetworkSelectionModeAutomatic(this);
        r.unregisterForRecordsEvents(this);
        r.unregisterForRecordsLoaded(this);
    }

    @Override
    public void exitEmergencyCallbackMode() {
        if (isPhoneTypeGsm()) {
            if (this.mImsPhone == null) {
                return;
            }
            this.mImsPhone.exitEmergencyCallbackMode();
        } else {
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
            this.mCi.exitEmergencyCallbackMode(obtainMessage(26));
        }
    }

    private void handleEnterEmergencyCallbackMode(Message msg) {
        Rlog.d(LOG_TAG, "handleEnterEmergencyCallbackMode,mIsPhoneInEcmState= " + this.mIsPhoneInEcmState);
        if (this.mIsPhoneInEcmState) {
            return;
        }
        this.mIsPhoneInEcmState = true;
        sendEmergencyCallbackModeChange();
        setSystemProperty("ril.cdma.inecmmode", "true");
        long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
        postDelayed(this.mExitEcmRunnable, delayInMillis);
        this.mWakeLock.acquire();
    }

    private void handleExitEmergencyCallbackMode(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        Rlog.d(LOG_TAG, "handleExitEmergencyCallbackMode,ar.exception , mIsPhoneInEcmState " + ar.exception + this.mIsPhoneInEcmState);
        removeCallbacks(this.mExitEcmRunnable);
        if (this.mEcmExitRespRegistrant != null) {
            this.mEcmExitRespRegistrant.notifyRegistrant(ar);
        }
        if (ar.exception != null) {
            return;
        }
        if (this.mWakeLock.isHeld()) {
            this.mWakeLock.release();
        }
        if (this.mIsPhoneInEcmState) {
            this.mIsPhoneInEcmState = VDBG;
            setSystemProperty("ril.cdma.inecmmode", "false");
        }
        sendEmergencyCallbackModeChange();
        this.mDcTracker.setInternalDataEnabled(true);
        notifyEmergencyCallRegistrants(VDBG);
    }

    public void notifyEmergencyCallRegistrants(boolean started) {
        this.mEmergencyCallToggledRegistrants.notifyResult(Integer.valueOf(started ? 1 : 0));
    }

    public void handleTimerInEmergencyCallbackMode(int action) {
        switch (action) {
            case 0:
                long delayInMillis = SystemProperties.getLong("ro.cdma.ecmexittimer", 300000L);
                postDelayed(this.mExitEcmRunnable, delayInMillis);
                this.mEcmTimerResetRegistrants.notifyResult(Boolean.FALSE);
                break;
            case 1:
                removeCallbacks(this.mExitEcmRunnable);
                this.mEcmTimerResetRegistrants.notifyResult(Boolean.TRUE);
                break;
            default:
                Rlog.e(LOG_TAG, "handleTimerInEmergencyCallbackMode, unsupported action " + action);
                break;
        }
    }

    private static boolean isIs683OtaSpDialStr(String dialStr) {
        int dialStrLen = dialStr.length();
        if (dialStrLen == 4) {
            if (!dialStr.equals(IS683A_FEATURE_CODE)) {
                return VDBG;
            }
            return true;
        }
        int sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
        switch (sysSelCodeInt) {
        }
        return VDBG;
    }

    private static int extractSelCodeFromOtaSpNum(String dialStr) {
        int dialStrLen = dialStr.length();
        int sysSelCodeInt = -1;
        if (dialStr.regionMatches(0, IS683A_FEATURE_CODE, 0, 4) && dialStrLen >= 6) {
            sysSelCodeInt = Integer.parseInt(dialStr.substring(4, 6));
        }
        Rlog.d(LOG_TAG, "extractSelCodeFromOtaSpNum " + sysSelCodeInt);
        return sysSelCodeInt;
    }

    private static boolean checkOtaSpNumBasedOnSysSelCode(int sysSelCodeInt, String[] sch) {
        try {
            int selRc = Integer.parseInt(sch[1]);
            for (int i = 0; i < selRc; i++) {
                if (!TextUtils.isEmpty(sch[i + 2]) && !TextUtils.isEmpty(sch[i + 3])) {
                    int selMin = Integer.parseInt(sch[i + 2]);
                    int selMax = Integer.parseInt(sch[i + 3]);
                    if (sysSelCodeInt >= selMin && sysSelCodeInt <= selMax) {
                        return true;
                    }
                }
            }
            return VDBG;
        } catch (NumberFormatException ex) {
            Rlog.e(LOG_TAG, "checkOtaSpNumBasedOnSysSelCode, error", ex);
            return VDBG;
        }
    }

    private boolean isCarrierOtaSpNum(String dialStr) {
        int sysSelCodeInt = extractSelCodeFromOtaSpNum(dialStr);
        if (sysSelCodeInt == -1) {
            return VDBG;
        }
        if (!TextUtils.isEmpty(this.mCarrierOtaSpNumSchema)) {
            Matcher m = pOtaSpNumSchema.matcher(this.mCarrierOtaSpNumSchema);
            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,schema" + this.mCarrierOtaSpNumSchema);
            if (m.find()) {
                String[] sch = pOtaSpNumSchema.split(this.mCarrierOtaSpNumSchema);
                if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("SELC")) {
                    if (sysSelCodeInt != -1) {
                        boolean isOtaSpNum = checkOtaSpNumBasedOnSysSelCode(sysSelCodeInt, sch);
                        return isOtaSpNum;
                    }
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,sysSelCodeInt is invalid");
                    return VDBG;
                }
                if (!TextUtils.isEmpty(sch[0]) && sch[0].equals("FC")) {
                    int fcLen = Integer.parseInt(sch[1]);
                    String fc = sch[2];
                    if (dialStr.regionMatches(0, fc, 0, fcLen)) {
                        return true;
                    }
                    Rlog.d(LOG_TAG, "isCarrierOtaSpNum,not otasp number");
                    return VDBG;
                }
                Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema not supported" + sch[0]);
                return VDBG;
            }
            Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern not right" + this.mCarrierOtaSpNumSchema);
            return VDBG;
        }
        Rlog.d(LOG_TAG, "isCarrierOtaSpNum,ota schema pattern empty");
        return VDBG;
    }

    @Override
    public boolean isOtaSpNumber(String dialStr) {
        if (isPhoneTypeGsm()) {
            return super.isOtaSpNumber(dialStr);
        }
        boolean isOtaSpNum = VDBG;
        String dialableStr = PhoneNumberUtils.extractNetworkPortionAlt(dialStr);
        if (dialableStr != null && !(isOtaSpNum = isIs683OtaSpDialStr(dialableStr))) {
            isOtaSpNum = isCarrierOtaSpNum(dialableStr);
        }
        Rlog.d(LOG_TAG, "isOtaSpNumber " + isOtaSpNum);
        return isOtaSpNum;
    }

    @Override
    public int getCdmaEriIconIndex() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconIndex();
        }
        return getServiceState().getCdmaEriIconIndex();
    }

    @Override
    public int getCdmaEriIconMode() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriIconMode();
        }
        return getServiceState().getCdmaEriIconMode();
    }

    @Override
    public String getCdmaEriText() {
        if (isPhoneTypeGsm()) {
            return super.getCdmaEriText();
        }
        int roamInd = getServiceState().getCdmaRoamingIndicator();
        int defRoamInd = getServiceState().getCdmaDefaultRoamingIndicator();
        return this.mEriManager.getCdmaEriText(roamInd, defRoamInd);
    }

    private void phoneObjectUpdater(int newVoiceRadioTech) {
        boolean z = true;
        logd("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech);
        this.mNewVoiceTech = newVoiceRadioTech;
        if (newVoiceRadioTech == 14 || newVoiceRadioTech == 0) {
            CarrierConfigManager configMgr = (CarrierConfigManager) getContext().getSystemService("carrier_config");
            PersistableBundle b = configMgr.getConfigForSubId(getSubId());
            if (b != null) {
                int volteReplacementRat = b.getInt("volte_replacement_rat_int");
                logd("phoneObjectUpdater: volteReplacementRat=" + volteReplacementRat);
                if (volteReplacementRat != 0) {
                    newVoiceRadioTech = volteReplacementRat;
                }
            } else {
                loge("phoneObjectUpdater: didn't get volteReplacementRat from carrier config");
            }
        }
        if (this.mRilVersion == 6 && getLteOnCdmaMode() == 1) {
            if (getPhoneType() == 2) {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Use CDMA Phone newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + getPhoneName());
                return;
            } else {
                logd("phoneObjectUpdater: LTE ON CDMA property is set. Switch to CDMALTEPhone newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + getPhoneName());
                newVoiceRadioTech = 6;
            }
        } else {
            if (isShuttingDown()) {
                logd("Device is shutting down. No need to switch phone now.");
                return;
            }
            boolean matchCdma = ServiceState.isCdma(newVoiceRadioTech);
            boolean matchGsm = ServiceState.isGsm(newVoiceRadioTech);
            if ((matchCdma && getPhoneType() == 2) || (matchGsm && getPhoneType() == 1)) {
                if (matchCdma && getPhoneType() == 2) {
                    this.mIccCardProxy.setVoiceRadioTech(newVoiceRadioTech);
                }
                if ((!isPhoneTypeCdmaLte() || !isCdmaOnlyCard()) && (!isPhoneTypeCdma() || isCdmaOnlyCard())) {
                    z = false;
                }
                if (!z) {
                    logd("phoneObjectUpdater: No change ignore, newVoiceRadioTech=" + newVoiceRadioTech + " mActivePhone=" + getPhoneName());
                    return;
                }
            }
            if (!matchCdma && !matchGsm) {
                loge("phoneObjectUpdater: newVoiceRadioTech=" + newVoiceRadioTech + " doesn't match either CDMA or GSM - error! No phone change");
                return;
            }
        }
        if (newVoiceRadioTech == 0) {
            logd("phoneObjectUpdater: Unknown rat ignore,  newVoiceRadioTech=Unknown. mActivePhone=" + getPhoneName());
            return;
        }
        boolean oldPowerState = VDBG;
        if (this.mResetModemOnRadioTechnologyChange && this.mCi.getRadioState().isOn()) {
            oldPowerState = true;
            logd("phoneObjectUpdater: Setting Radio Power to Off");
            this.mCi.setRadioPower(VDBG, null);
        }
        switchVoiceRadioTech(newVoiceRadioTech);
        if (this.mResetModemOnRadioTechnologyChange && oldPowerState) {
            logd("phoneObjectUpdater: Resetting Radio");
            this.mCi.setRadioPower(oldPowerState, null);
        }
        this.mIccCardProxy.setVoiceRadioTech(newVoiceRadioTech);
        Intent intent = new Intent("android.intent.action.RADIO_TECHNOLOGY");
        intent.addFlags(536870912);
        intent.putExtra("phoneName", getPhoneName());
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhoneId);
        ActivityManagerNative.broadcastStickyIntent(intent, (String) null, -1);
    }

    private void switchVoiceRadioTech(int newVoiceRadioTech) {
        String outgoingPhoneName = getPhoneName();
        logd("Switching Voice Phone : " + outgoingPhoneName + " >>> " + (ServiceState.isGsm(newVoiceRadioTech) ? "GSM" : "CDMA"));
        if (ServiceState.isCdma(newVoiceRadioTech)) {
            if (isCdmaOnlyCard()) {
                switchPhoneType(2);
                return;
            } else {
                switchPhoneType(6);
                return;
            }
        }
        if (ServiceState.isGsm(newVoiceRadioTech)) {
            switchPhoneType(1);
        } else {
            loge("deleteAndCreatePhone: newVoiceRadioTech=" + newVoiceRadioTech + " is not CDMA or GSM (error) - aborting!");
        }
    }

    @Override
    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return this.mIccSmsInterfaceManager;
    }

    @Override
    public void updatePhoneObject(int voiceRadioTech) {
        logd("updatePhoneObject: radioTechnology=" + voiceRadioTech);
        sendMessage(obtainMessage(42, voiceRadioTech, 0, null));
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        this.mSST.setImsRegistrationState(registered);
    }

    @Override
    public boolean getIccRecordsLoaded() {
        return this.mIccCardProxy.getIccRecordsLoaded();
    }

    @Override
    public IccCard getIccCard() {
        return this.mIccCardProxy;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("GsmCdmaPhone extends:");
        super.dump(fd, pw, args);
        pw.println(" mPrecisePhoneType=" + this.mPrecisePhoneType);
        pw.println(" mCT=" + this.mCT);
        pw.println(" mSST=" + this.mSST);
        pw.println(" mPendingMMIs=" + this.mPendingMMIs);
        pw.println(" mIccPhoneBookIntManager=" + this.mIccPhoneBookIntManager);
        pw.println(" mVmNumber=" + this.mVmNumber);
        pw.println(" mCdmaSSM=" + this.mCdmaSSM);
        pw.println(" mCdmaSubscriptionSource=" + this.mCdmaSubscriptionSource);
        pw.println(" mEriManager=" + this.mEriManager);
        pw.println(" mWakeLock=" + this.mWakeLock);
        pw.println(" mIsPhoneInEcmState=" + this.mIsPhoneInEcmState);
        pw.println(" mCarrierOtaSpNumSchema=" + this.mCarrierOtaSpNumSchema);
        if (!isPhoneTypeGsm()) {
            pw.println(" getCdmaEriIconIndex()=" + getCdmaEriIconIndex());
            pw.println(" getCdmaEriIconMode()=" + getCdmaEriIconMode());
            pw.println(" getCdmaEriText()=" + getCdmaEriText());
            pw.println(" isMinInfoReady()=" + isMinInfoReady());
        }
        pw.println(" isCspPlmnEnabled()=" + isCspPlmnEnabled());
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            this.mIccCardProxy.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        UiccCard card;
        if (this.mUiccController == null || (card = this.mUiccController.getUiccCard(getPhoneId())) == null) {
            return VDBG;
        }
        boolean status = card.setOperatorBrandOverride(brand);
        if (status) {
            IccRecords iccRecords = this.mIccRecords.get();
            if (iccRecords != null) {
                TelephonyManager.from(this.mContext).setSimOperatorNameForPhone(getPhoneId(), iccRecords.getServiceProviderName());
            }
            if (this.mSST != null) {
                this.mSST.pollState();
            }
        }
        return status;
    }

    private String getOperatorNumeric() {
        String operatorNumeric = null;
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                return r.getOperatorNumeric();
            }
            return null;
        }
        IccRecords curIccRecords = null;
        if (this.mCdmaSubscriptionSource == 1) {
            operatorNumeric = SystemProperties.get(PROPERTY_CDMA_HOME_OPERATOR_NUMERIC);
        } else if (this.mCdmaSubscriptionSource == 0) {
            curIccRecords = this.mSimRecords;
            if (curIccRecords != null) {
                operatorNumeric = curIccRecords.getOperatorNumeric();
            } else {
                curIccRecords = this.mIccRecords.get();
                if (curIccRecords != null && (curIccRecords instanceof RuimRecords)) {
                    RuimRecords csim = (RuimRecords) curIccRecords;
                    operatorNumeric = csim.getRUIMOperatorNumeric();
                }
            }
        }
        if (operatorNumeric == null) {
            loge("getOperatorNumeric: Cannot retrieve operatorNumeric: mCdmaSubscriptionSource = " + this.mCdmaSubscriptionSource + " mIccRecords = " + (curIccRecords != null ? Boolean.valueOf(curIccRecords.getRecordsLoaded()) : null));
        }
        logd("getOperatorNumeric: mCdmaSubscriptionSource = " + this.mCdmaSubscriptionSource + " operatorNumeric = " + operatorNumeric);
        return operatorNumeric;
    }

    public void notifyEcbmTimerReset(Boolean flag) {
        this.mEcmTimerResetRegistrants.notifyResult(flag);
    }

    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        this.mEcmTimerResetRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        this.mEcmTimerResetRegistrants.remove(h);
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        if (isPhoneTypeGsm()) {
            IccRecords r = this.mIccRecords.get();
            if (r != null) {
                r.setVoiceMessageWaiting(line, countWaiting);
                return;
            } else {
                logd("SIM Records not found, MWI not updated");
                return;
            }
        }
        setVoiceMessageCount(countWaiting);
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[GsmCdmaPhone] " + s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[GsmCdmaPhone] " + s);
    }

    @Override
    public boolean isUtEnabled() {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone != null) {
            return imsPhone.isUtEnabled();
        }
        logd("isUtEnabled: called for GsmCdma");
        return VDBG;
    }

    public ImsUtInterface getUtInterface() throws ImsException {
        ImsUtInterface ut = null;
        ImsPhone imsPhone = (ImsPhone) this.mImsPhone;
        if (imsPhone != null) {
            ImsPhoneCallTracker imsPhoneCallTracker = (ImsPhoneCallTracker) imsPhone.getCallTracker();
            ut = imsPhoneCallTracker.getUtInterface();
        }
        logd("getUtInterface: " + ut);
        return ut;
    }

    public String getDtmfToneDelayKey() {
        if (isPhoneTypeGsm()) {
            return "gsm_dtmf_tone_delay_int";
        }
        return "cdma_dtmf_tone_delay_int";
    }

    public PowerManager.WakeLock getWakeLock() {
        return this.mWakeLock;
    }

    @Override
    public void hangupAll() throws CallStateException {
        this.mCT.hangupAll();
    }

    public void setIncomingCallIndicationResponse(boolean accept) {
        Rlog.d(LOG_TAG, "setIncomingCallIndicationResponse " + accept);
        this.mCT.setIncomingCallIndicationResponse(accept);
    }

    @Override
    public void registerForCrssSuppServiceNotification(Handler h, int what, Object obj) {
        this.mCallRelatedSuppSvcRegistrants.addUnique(h, what, obj);
        if (this.mCachedCrssn == null) {
            return;
        }
        this.mCallRelatedSuppSvcRegistrants.notifyRegistrants(this.mCachedCrssn);
    }

    @Override
    public void unregisterForCrssSuppServiceNotification(Handler h) {
        this.mCallRelatedSuppSvcRegistrants.remove(h);
        this.mCachedCrssn = null;
    }

    @Override
    public Connection dial(List<String> numbers, int videoState) throws CallStateException {
        boolean imsUseEnabled = VDBG;
        Phone imsPhone = this.mImsPhone;
        if (ImsManager.isVolteEnabledByPlatform(this.mContext) && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mContext)) {
            imsUseEnabled = ImsManager.isNonTtyOrTtyOnVolteEnabled(this.mContext);
        }
        if (!imsUseEnabled) {
            Rlog.w(LOG_TAG, "IMS is disabled and can not dial conference call directly.");
            return null;
        }
        if (imsPhone != null) {
            Rlog.w(LOG_TAG, "service state = " + imsPhone.getServiceState().getState());
        }
        if (imsUseEnabled && imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            try {
                Rlog.d(LOG_TAG, "Trying IMS PS conference call");
                return imsPhone.dial(numbers, videoState);
            } catch (CallStateException e) {
                Rlog.d(LOG_TAG, "IMS PS conference call exception " + e);
                if (!Phone.CS_FALLBACK.equals(e.getMessage())) {
                    CallStateException ce = new CallStateException(e.getMessage());
                    ce.setStackTrace(e.getStackTrace());
                    throw ce;
                }
            }
        }
        return null;
    }

    @Override
    public void doGeneralSimAuthentication(int sessionId, int mode, int tag, String param1, String param2, Message result) {
        if (!isPhoneTypeGsm()) {
            return;
        }
        this.mCi.doGeneralSimAuthentication(sessionId, mode, tag, param1, param2, result);
    }

    @Override
    public String getMvnoMatchType() {
        String type = UsimPBMemInfo.STRING_NOT_SET;
        if (isPhoneTypeGsm()) {
            if (this.mIccRecords.get() != null) {
                type = this.mIccRecords.get().getMvnoMatchType();
            }
            logd("getMvnoMatchType: Type = " + type);
        }
        return type;
    }

    @Override
    public String getMvnoPattern(String type) {
        if (!isPhoneTypeGsm() || this.mIccRecords.get() == null) {
            return UsimPBMemInfo.STRING_NOT_SET;
        }
        if (type.equals(Telephony.Carriers.SPN)) {
            String pattern = this.mIccRecords.get().getSpNameInEfSpn();
            return pattern;
        }
        if (type.equals(Telephony.Carriers.IMSI)) {
            String pattern2 = this.mIccRecords.get().isOperatorMvnoForImsi();
            return pattern2;
        }
        if (type.equals(Telephony.Carriers.PNN)) {
            String pattern3 = this.mIccRecords.get().isOperatorMvnoForEfPnn();
            return pattern3;
        }
        if (type.equals("gid")) {
            String pattern4 = this.mIccRecords.get().getGid1();
            return pattern4;
        }
        logd("getMvnoPattern: Wrong type = " + type);
        return UsimPBMemInfo.STRING_NOT_SET;
    }

    @Override
    public int getCdmaSubscriptionActStatus() {
        if (this.mCdmaSSM != null) {
            return this.mCdmaSSM.getActStatus();
        }
        return 0;
    }

    public boolean isGsmUtSupport() {
        if (!SystemProperties.get("persist.mtk_ims_support").equals("1") || !SystemProperties.get("persist.mtk_volte_support").equals("1") || !OperatorUtils.isGsmUtSupport(getOperatorNumeric()) || !isUsimCard()) {
            return VDBG;
        }
        boolean zIsWifiCallingEnabled = this.mImsPhone != null ? this.mImsPhone.isWifiCallingEnabled() : VDBG;
        boolean isWfcUtSupport = isWFCUtSupport();
        logd("in isGsmUtSupport isWfcEnable -->" + zIsWifiCallingEnabled + "isWfcUtSupport-->" + isWfcUtSupport);
        if (!zIsWifiCallingEnabled || isWfcUtSupport) {
            return true;
        }
        return VDBG;
    }

    public boolean isWFCUtSupport() {
        if (SystemProperties.get("ro.mtk_bsp_package").equals("1") || !SystemProperties.get("persist.mtk_ims_support").equals("1") || !SystemProperties.get("persist.mtk_wfc_support").equals("1") || isOp(OperatorUtils.OPID.OP11) || isOp(OperatorUtils.OPID.OP15)) {
            return VDBG;
        }
        return true;
    }

    private boolean isUsimCard() {
        boolean r = VDBG;
        String iccCardType = PhoneFactory.getPhone(getPhoneId()).getIccCard().getIccCardType();
        if (iccCardType != null && iccCardType.equals("USIM")) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isUsimCard: " + r + ", " + iccCardType);
        return r;
    }

    public boolean isOpNotSupportOCB(String facility) {
        boolean r = VDBG;
        boolean isOcb = VDBG;
        if (facility.equals(CommandsInterface.CB_FACILITY_BAOC) || facility.equals(CommandsInterface.CB_FACILITY_BAOIC) || facility.equals(CommandsInterface.CB_FACILITY_BAOICxH)) {
            isOcb = true;
        }
        if (isOcb && isOp(OperatorUtils.OPID.OP01)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpNotSupportOCB: " + r + ", facility=" + facility);
        return r;
    }

    private boolean isOp(OperatorUtils.OPID id) {
        return OperatorUtils.isOperator(getOperatorNumeric(), id);
    }

    private boolean isOpTbcwWithCS(int phoneId) {
        boolean r = VDBG;
        if (OperatorUtils.isNotSupportXcap(getOperatorNumeric())) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpTbcwWithCS: " + r);
        return r;
    }

    public boolean isOpTbClir() {
        boolean r = VDBG;
        if (OperatorUtils.isTbClir(getOperatorNumeric())) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpTbClir: " + r);
        return r;
    }

    public boolean isOpNwCW() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP50)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpNwCW(): true");
        return r;
    }

    public boolean isEnableXcapHttpResponse409() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP05)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isEnableXcapHttpResponse409: " + r);
        return r;
    }

    public boolean isOpTransferXcap404() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP05)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpTransferXcap404: " + r);
        return r;
    }

    public boolean isOpNotSupportCallIdentity() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP01)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpNotSupportCallIdentity: " + r);
        return r;
    }

    public boolean isOpReregisterForCF() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP08)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isOpReregisterForCF: " + r);
        return r;
    }

    private boolean isIccCardMncMccAvailable(int phoneId) {
        UiccController uiccCtl = UiccController.getInstance();
        IccRecords iccRecords = uiccCtl.getIccRecords(phoneId, 1);
        if (iccRecords != null) {
            String mccMnc = iccRecords.getOperatorNumeric();
            Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): mccMnc is " + mccMnc);
            if (mccMnc != null) {
                return true;
            }
            return VDBG;
        }
        Rlog.d(LOG_TAG, "isIccCardMncMccAvailable(): false");
        return VDBG;
    }

    public boolean isSupportSaveCFNumber() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP07)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "isSupportSaveCFNumber: " + r);
        return r;
    }

    public void clearCFSharePreference(int cfReason) {
        String key;
        switch (cfReason) {
            case 1:
                key = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                key = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                key = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.e(LOG_TAG, "No need to store cfreason: " + cfReason);
                return;
        }
        Rlog.e(LOG_TAG, "Read to clear the key: " + key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + key);
        } else {
            Rlog.e(LOG_TAG, "Commit the removal of CF preference: " + key);
        }
    }

    public boolean applyCFSharePreference(int cfReason, String setNumber) {
        String key;
        switch (cfReason) {
            case 1:
                key = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                key = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                key = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to store cfreason: " + cfReason);
                return VDBG;
        }
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return VDBG;
        }
        String currentImsi = r.getIMSI();
        if (currentImsi == null || currentImsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return VDBG;
        }
        if (setNumber == null || setNumber.isEmpty()) {
            Rlog.d(LOG_TAG, "setNumber is empty");
            return VDBG;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        String content = currentImsi + ";" + setNumber;
        if (content == null || content.isEmpty()) {
            Rlog.e(LOG_TAG, "imsi or content are empty or null.");
            return VDBG;
        }
        Rlog.e(LOG_TAG, "key: " + key);
        Rlog.e(LOG_TAG, "content: " + content);
        editor.putString(key, content);
        editor.apply();
        return true;
    }

    public String getCFPreviousDialNumber(int cfReason) {
        String key;
        switch (cfReason) {
            case 1:
                key = "CFB_" + String.valueOf(this.mPhoneId);
                break;
            case 2:
                key = "CFNR_" + String.valueOf(this.mPhoneId);
                break;
            case 3:
                key = "CFNRC_" + String.valueOf(this.mPhoneId);
                break;
            default:
                Rlog.d(LOG_TAG, "No need to do the reason: " + cfReason);
                return null;
        }
        Rlog.d(LOG_TAG, "key: " + key);
        IccRecords r = this.mIccRecords.get();
        if (r == null) {
            Rlog.d(LOG_TAG, "No iccRecords");
            return null;
        }
        String currentImsi = r.getIMSI();
        if (currentImsi == null || currentImsi.isEmpty()) {
            Rlog.d(LOG_TAG, "currentImsi is empty");
            return null;
        }
        Rlog.d(LOG_TAG, "currentImsi: " + currentImsi);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String info = sp.getString(key, null);
        if (info == null) {
            Rlog.d(LOG_TAG, "Sharedpref not with: " + key);
            return null;
        }
        String[] infoAry = info.split(";");
        if (infoAry == null || infoAry.length < 2) {
            Rlog.d(LOG_TAG, "infoAry.length < 2");
            return null;
        }
        String imsi = infoAry[0];
        String number = infoAry[1];
        if (imsi == null || imsi.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref imsi is empty.");
            return null;
        }
        if (number == null || number.isEmpty()) {
            Rlog.d(LOG_TAG, "Sharedpref number is empty.");
            return null;
        }
        Rlog.d(LOG_TAG, "Sharedpref imsi: " + imsi);
        Rlog.d(LOG_TAG, "Sharedpref number: " + number);
        if (currentImsi.equals(imsi)) {
            Rlog.d(LOG_TAG, "Get dial number from sharepref: " + number);
            return number;
        }
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(key);
        if (!editor.commit()) {
            Rlog.e(LOG_TAG, "failed to commit the removal of CF preference: " + key);
        }
        return null;
    }

    public boolean queryCFUAgainAfterSet() {
        boolean r = VDBG;
        if (isOp(OperatorUtils.OPID.OP05) || isOp(OperatorUtils.OPID.OP11)) {
            r = true;
        }
        Rlog.d(LOG_TAG, "queryCFUAgainAfterSet: " + r);
        return r;
    }

    @Override
    public void refreshSpnDisplay() {
        this.mSST.refreshSpnDisplay();
    }

    @Override
    public int getNetworkHideState() {
        if (this.mSST.dontUpdateNetworkStateFlag) {
            return 1;
        }
        return this.mSST.mSS.getState();
    }

    @Override
    public String getLocatedPlmn() {
        return this.mSST.getLocatedPlmn();
    }

    private boolean isCdmaOnlyCard() {
        UiccCardApplication uiccApplication3gpp = this.mUiccController.getUiccCardApplication(this.mPhoneId, 1);
        UiccCardApplication uiccApplication3gpp2 = this.mUiccController.getUiccCardApplication(this.mPhoneId, 2);
        if (uiccApplication3gpp != null || uiccApplication3gpp2 == null) {
            return VDBG;
        }
        return true;
    }
}
