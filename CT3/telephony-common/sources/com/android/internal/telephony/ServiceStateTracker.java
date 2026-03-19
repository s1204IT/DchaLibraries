package com.android.internal.telephony;

import android.R;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.AsyncResult;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Pair;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.test.SimulatedCommands;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.SpnOverride;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IServiceStateExt;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.IWorldPhone;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class ServiceStateTracker extends Handler {

    private static final int[] f10xba025bb = null;
    private static final String ACTION_RADIO_OFF = "android.intent.action.ACTION_RADIO_OFF";
    public static final int CS_DISABLED = 1004;
    public static final int CS_EMERGENCY_ENABLED = 1006;
    public static final int CS_ENABLED = 1003;
    public static final int CS_NORMAL_ENABLED = 1005;
    public static final int CS_NOTIFICATION = 999;
    private static boolean DBG = false;
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60000;
    public static final String DEFAULT_MNC = "00";
    protected static final int EVENT_ALL_DATA_DISCONNECTED = 49;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED = 40;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 39;
    protected static final int EVENT_CHANGE_IMS_STATE = 45;
    protected static final int EVENT_CHECK_REPORT_GPRS = 22;
    protected static final int EVENT_DATA_CONNECTION_DETACHED = 100;
    protected static final int EVENT_DISABLE_EMMRRS_STATUS = 104;
    protected static final int EVENT_ENABLE_EMMRRS_STATUS = 105;
    protected static final int EVENT_ERI_FILE_LOADED = 36;
    protected static final int EVENT_ETS_DEV_CHANGED_LOGGER = 205;
    protected static final int EVENT_FEMTO_CELL_INFO = 107;
    protected static final int EVENT_GET_CELL_INFO_LIST = 43;
    protected static final int EVENT_GET_CELL_INFO_LIST_BY_RATE = 108;
    protected static final int EVENT_GET_LOC_DONE = 15;
    protected static final int EVENT_GET_PREFERRED_NETWORK_TYPE = 19;
    protected static final int EVENT_GET_SIGNAL_STRENGTH = 3;
    public static final int EVENT_ICC_CHANGED = 42;
    protected static final int EVENT_ICC_REFRESH = 106;
    protected static final int EVENT_IMEI_LOCK = 103;
    protected static final int EVENT_IMS_CAPABILITY_CHANGED = 48;
    protected static final int EVENT_IMS_DISABLED_URC = 111;
    protected static final int EVENT_IMS_REGISTRATION_INFO = 112;
    protected static final int EVENT_IMS_STATE_CHANGED = 46;
    protected static final int EVENT_IMS_STATE_DONE = 47;
    protected static final int EVENT_INVALID_SIM_INFO = 101;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED = 18;
    protected static final int EVENT_MODULATION_INFO = 117;
    protected static final int EVENT_NETWORK_EVENT = 118;
    protected static final int EVENT_NETWORK_STATE_CHANGED = 2;
    protected static final int EVENT_NITZ_TIME = 11;
    protected static final int EVENT_NV_READY = 35;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE = 37;
    protected static final int EVENT_PHONE_TYPE_SWITCHED = 50;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH = 10;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION = 34;
    protected static final int EVENT_POLL_STATE_GPRS = 5;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE = 14;
    protected static final int EVENT_POLL_STATE_OPERATOR = 6;
    protected static final int EVENT_POLL_STATE_REGISTRATION = 4;
    protected static final int EVENT_PS_NETWORK_STATE_CHANGED = 102;
    protected static final int EVENT_PS_NETWORK_TYPE_CHANGED = 113;
    protected static final int EVENT_RADIO_AVAILABLE = 13;
    protected static final int EVENT_RADIO_ON = 41;
    protected static final int EVENT_RADIO_STATE_CHANGED = 1;
    protected static final int EVENT_RESET_PREFERRED_NETWORK_TYPE = 21;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED = 23;
    protected static final int EVENT_RUIM_READY = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 27;
    protected static final int EVENT_SET_AUTO_SELECT_NETWORK_DONE = 50;
    protected static final int EVENT_SET_IMS_DISABLE_DONE = 110;
    protected static final int EVENT_SET_IMS_ENABLED_DONE = 109;
    protected static final int EVENT_SET_PREFERRED_NETWORK_TYPE = 20;
    protected static final int EVENT_SET_RADIO_POWER_OFF = 38;
    protected static final int EVENT_SIGNAL_STRENGTH_UPDATE = 12;
    protected static final int EVENT_SIM_READY = 17;
    protected static final int EVENT_SIM_RECORDS_LOADED = 16;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST = 44;
    protected static final String[] GMT_COUNTRY_CODES;
    public static final String INVALID_MCC = "000";
    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    private static final String LOG_TAG = "SST";
    private static final int MAX_NITZ_YEAR = 2037;
    public static final int MS_PER_HOUR = 3600000;
    public static final int NITZ_UPDATE_DIFF_DEFAULT = 2000;
    public static final int NITZ_UPDATE_SPACING_DEFAULT = 600000;
    public static final int OTASP_NEEDED = 2;
    public static final int OTASP_NOT_NEEDED = 3;
    public static final int OTASP_SIM_UNPROVISIONED = 5;
    public static final int OTASP_UNINITIALIZED = 0;
    public static final int OTASP_UNKNOWN = 1;
    private static final int POLL_PERIOD_MILLIS = 10000;
    protected static final String PROPERTY_AUTO_RAT_SWITCH = "persist.radio.autoratswitch";
    private static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";
    public static final int PS_DISABLED = 1002;
    public static final int PS_ENABLED = 1001;
    public static final int PS_NOTIFICATION = 888;
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";
    protected static final String REGISTRATION_DENIED_GEN = "General";
    static final int REJECT_NOTIFICATION = 890;
    static final int SPECIAL_CARD_TYPE_NOTIFICATION = 8903;
    protected static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    public static final String UNACTIVATED_MIN2_VALUE = "000000";
    public static final String UNACTIVATED_MIN_VALUE = "1111110111";
    private static final boolean VDBG = false;
    public static final String WAKELOCK_TAG = "ServiceStateTracker";
    public static final String[][] customEhplmn;
    private static final String[][] customOperatorConsiderRoamingMcc;
    private static Timer mCellInfoTimer;
    private static final boolean mEngLoad;
    private static int mLogLv;
    private static boolean[] sReceiveNitz;
    private CdmaSubscriptionSourceManager mCdmaSSM;
    public CellLocation mCellLoc;
    private CommandsInterface mCi;
    private ContentResolver mCr;
    private int mDefaultRoamingIndicator;
    private boolean mDesiredPowerState;
    private TelephonyEventLog mEventLog;
    private boolean mIsInPrl;
    private long mLastCellInfoListTime;
    private String mMdn;
    private String mMin;
    private CellLocation mNewCellLoc;
    private ServiceState mNewSS;
    private Notification mNotification;
    private Notification.Builder mNotificationBuilder;
    private GsmCdmaPhone mPhone;
    private int[] mPollingContext;
    private int mPreferredNetworkType;
    private String mPrlVersion;
    private String mRegistrationDeniedReason;
    private boolean mReportedGprsNoReg;
    public RestrictedState mRestrictedState;
    private int mRoamingIndicator;
    public ServiceState mSS;
    private long mSavedAtTime;
    private long mSavedTime;
    private String mSavedTimeZone;
    private IServiceStateExt mServiceStateExt;
    private SignalStrength mSignalStrength;
    protected boolean mSmsCapable;
    private boolean mStartedGprsRegCheck;
    private SubscriptionController mSubscriptionController;
    private SubscriptionManager mSubscriptionManager;
    private boolean mVoiceCapable;
    private PowerManager.WakeLock mWakeLock;
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;
    private boolean mZoneDst;
    private int mZoneOffset;
    private long mZoneTime;
    private UiccController mUiccController = null;
    private UiccCardApplication mUiccApplcation = null;
    private IccRecords mIccRecords = null;
    private List<CellInfo> mLastCellInfoList = null;
    protected int mCellInfoRate = Integer.MAX_VALUE;
    private boolean mDontPollSignalStrength = VDBG;
    private RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    private RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    private RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    private RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mAttachedRegistrants = new RegistrantList();
    protected RegistrantList mDetachedRegistrants = new RegistrantList();
    private RegistrantList mDataRegStateOrRatChangedRegistrants = new RegistrantList();
    private RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();
    protected RegistrantList mSignalStrengthChangedRegistrants = new RegistrantList();
    private boolean mPendingRadioPowerOffAfterDataOff = VDBG;
    private int mPendingRadioPowerOffAfterDataOffTag = 0;
    protected boolean mPendingPsRestrictDisabledNotify = VDBG;
    private boolean mImsRegistrationOnOff = VDBG;
    private boolean mAlarmSwitch = VDBG;
    private PendingIntent mRadioOffIntent = null;
    private boolean mPowerOffDelayNeed = true;
    private boolean mDeviceShuttingDown = VDBG;
    private boolean mSpnUpdatePending = VDBG;
    private String mCurSpn = null;
    private String mCurDataSpn = null;
    private String mCurPlmn = null;
    private boolean mCurShowPlmn = VDBG;
    private boolean mCurShowSpn = VDBG;
    private int mSubId = -1;
    private boolean mImsRegistered = VDBG;
    private final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SstSubscriptionsChangedListener(this, null);
    private boolean mNeedFixZoneAfterNitz = VDBG;
    private boolean mGotCountryCode = VDBG;
    private ContentObserver mAutoTimeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i(ServiceStateTracker.LOG_TAG, "Auto time state changed");
            ServiceStateTracker.this.revertToNitzTime();
        }
    };
    private ContentObserver mAutoTimeZoneObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i(ServiceStateTracker.LOG_TAG, "Auto time zone state changed");
            ServiceStateTracker.this.revertToNitzTimeZone();
        }
    };
    private int mMaxDataCalls = 1;
    private int mNewMaxDataCalls = 1;
    private int mReasonDataDenied = -1;
    private int mNewReasonDataDenied = -1;
    private boolean mGsmRoaming = VDBG;
    private boolean mDataRoaming = VDBG;
    private boolean mEmergencyOnly = VDBG;
    private boolean mNitzUpdatedTime = VDBG;
    private int gprsState = 1;
    private int newGPRSState = 1;
    private String mHhbName = null;
    private String mCsgId = null;
    private int mFemtocellDomain = 0;
    private int mIsFemtocell = 0;
    public boolean dontUpdateNetworkStateFlag = VDBG;
    private boolean mFirstRadioChange = true;
    private int explict_update_spn = 0;
    private String mLastRegisteredPLMN = null;
    private String mLastPSRegisteredPLMN = null;
    private boolean mEverIVSR = VDBG;
    private boolean isCsInvalidCard = VDBG;
    private String mLocatedPlmn = null;
    private int mPsRegState = 1;
    private int mPsRegStateRaw = 0;
    private String mSimType = UsimPBMemInfo.STRING_NOT_SET;
    private String[][] mTimeZoneIdOfCapitalCity = {new String[]{"au", "Australia/Sydney"}, new String[]{"br", "America/Sao_Paulo"}, new String[]{"ca", "America/Toronto"}, new String[]{Telephony.Mms.Part.CONTENT_LOCATION, "America/Santiago"}, new String[]{"es", "Europe/Madrid"}, new String[]{"fm", "Pacific/Ponape"}, new String[]{"gl", "America/Godthab"}, new String[]{PplMessageManager.PendingMessage.KEY_ID, "Asia/Jakarta"}, new String[]{"kz", "Asia/Almaty"}, new String[]{"mn", "Asia/Ulaanbaatar"}, new String[]{"mx", "America/Mexico_City"}, new String[]{"pf", "Pacific/Tahiti"}, new String[]{"pt", "Europe/Lisbon"}, new String[]{"ru", "Europe/Moscow"}, new String[]{"us", "America/New_York"}, new String[]{"ec", "America/Guayaquil"}};
    private String[][] mTimeZoneIdByMcc = {new String[]{RadioCapabilitySwitchUtil.CN_MCC, "Asia/Shanghai"}, new String[]{"404", "Asia/Calcutta"}, new String[]{"454", "Asia/Hong_Kong"}};
    private boolean mIsImeiLock = VDBG;
    private boolean mIsForceSendScreenOnForUpdateNwInfo = VDBG;
    protected boolean bHasDetachedDuringPolling = VDBG;
    private boolean mNeedNotify = VDBG;
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!ServiceStateTracker.this.mPhone.isPhoneTypeGsm()) {
                if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                    ServiceStateTracker.this.refreshSpnDisplay();
                    return;
                }
                if (intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED")) {
                    int phoneId = intent.getIntExtra("phone", -1);
                    ServiceStateTracker.this.log("[CDMA]phoneId:" + phoneId + ", mPhoneId:" + ServiceStateTracker.this.mPhone.getPhoneId());
                    if (phoneId == ServiceStateTracker.this.mPhone.getPhoneId()) {
                        String simStatus = intent.getStringExtra("ss");
                        ServiceStateTracker.this.log("[CDMA]simStatus: " + simStatus);
                        if ("ABSENT".equals(simStatus)) {
                            ServiceStateTracker.this.mMdn = null;
                            ServiceStateTracker.this.log("[CDMA]Clear MDN!");
                            return;
                        }
                        return;
                    }
                    return;
                }
                ServiceStateTracker.this.loge("Ignoring intent " + intent + " received on CDMA phone");
                return;
            }
            if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                ServiceStateTracker.this.refreshSpnDisplay();
                return;
            }
            if (intent.getAction().equals(ServiceStateTracker.ACTION_RADIO_OFF)) {
                ServiceStateTracker.this.mAlarmSwitch = ServiceStateTracker.VDBG;
                DcTracker dcTracker = ServiceStateTracker.this.mPhone.mDcTracker;
                ServiceStateTracker.this.powerOffRadioSafely(dcTracker);
                return;
            }
            if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                ServiceStateTracker.this.explict_update_spn = 1;
                if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    return;
                }
                try {
                    if (!ServiceStateTracker.this.mServiceStateExt.needEMMRRS() || !ServiceStateTracker.this.isCurrentPhoneDataConnectionOn()) {
                        return;
                    }
                    ServiceStateTracker.this.getEINFO(105);
                    return;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return;
                }
            }
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    return;
                }
                try {
                    if (!ServiceStateTracker.this.mServiceStateExt.needEMMRRS() || !ServiceStateTracker.this.isCurrentPhoneDataConnectionOn()) {
                        return;
                    }
                    ServiceStateTracker.this.getEINFO(104);
                    return;
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                    return;
                }
            }
            if (intent.getAction().equals("android.intent.action.SIM_STATE_CHANGED")) {
                String simState = "UNKNOWN";
                int slotId = intent.getIntExtra("phone", -1);
                if (slotId == ServiceStateTracker.this.mPhone.getPhoneId()) {
                    simState = intent.getStringExtra("ss");
                    ServiceStateTracker.this.log("SIM state change, slotId: " + slotId + " simState[" + simState + "]");
                }
                if (simState.equals("READY") && ServiceStateTracker.this.mSimType.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                    ServiceStateTracker.this.mSimType = PhoneFactory.getPhone(ServiceStateTracker.this.mPhone.getPhoneId()).getIccCard().getIccCardType();
                    ServiceStateTracker.this.log("SimType= " + ServiceStateTracker.this.mSimType);
                    if (ServiceStateTracker.this.mSimType != null && !ServiceStateTracker.this.mSimType.equals(UsimPBMemInfo.STRING_NOT_SET) && ((ServiceStateTracker.this.mSimType.equals("SIM") || ServiceStateTracker.this.mSimType.equals("USIM")) && !SystemProperties.get("ro.mtk_bsp_package").equals("1"))) {
                        try {
                            if (ServiceStateTracker.this.mServiceStateExt.needIccCardTypeNotification(ServiceStateTracker.this.mSimType)) {
                                if (TelephonyManager.getDefault().getPhoneCount() > 1) {
                                    int raf = ServiceStateTracker.this.mPhone.getRadioAccessFamily();
                                    ServiceStateTracker.this.log("check RAF=" + raf);
                                    if ((raf & 16384) == 16384) {
                                        ServiceStateTracker.this.setSpecialCardTypeNotification(ServiceStateTracker.this.mSimType, 0, 0);
                                    }
                                } else {
                                    ServiceStateTracker.this.setSpecialCardTypeNotification(ServiceStateTracker.this.mSimType, 0, 0);
                                }
                            }
                        } catch (RuntimeException e3) {
                            e3.printStackTrace();
                        }
                    }
                }
                if (slotId == ServiceStateTracker.this.mPhone.getPhoneId() && simState.equals("IMSI")) {
                    ServiceStateTracker.this.setDeviceRatMode(slotId);
                }
                if (!simState.equals("ABSENT") && !simState.equals("NOT_READY")) {
                    return;
                }
                ServiceStateTracker.this.mSimType = UsimPBMemInfo.STRING_NOT_SET;
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                notificationManager.cancel(ServiceStateTracker.SPECIAL_CARD_TYPE_NOTIFICATION);
                ServiceStateTracker.this.setReceivedNitz(ServiceStateTracker.this.mPhone.getPhoneId(), ServiceStateTracker.VDBG);
                ServiceStateTracker.this.mLastRegisteredPLMN = null;
                ServiceStateTracker.this.mLastPSRegisteredPLMN = null;
                if (!simState.equals("ABSENT")) {
                    return;
                }
                ServiceStateTracker.this.dontUpdateNetworkStateFlag = ServiceStateTracker.VDBG;
                return;
            }
            if (!intent.getAction().equals("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED") || intent.getIntExtra(CatService.AnonymousClass4.INTENT_KEY_DETECT_STATUS, 4) == 4) {
                return;
            }
            ServiceStateTracker.this.updateSpnDisplayGsm(true);
        }
    };
    private ContentObserver mDataConnectionSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            ServiceStateTracker.this.log("Data Connection Setting changed");
            if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                return;
            }
            try {
                if (ServiceStateTracker.this.mServiceStateExt.needEMMRRS()) {
                    if (ServiceStateTracker.this.isCurrentPhoneDataConnectionOn()) {
                        ServiceStateTracker.this.getEINFO(105);
                    } else {
                        ServiceStateTracker.this.getEINFO(104);
                    }
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    };
    private ContentObserver mMsicFeatureConfigObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Rlog.i("GsmServiceStateTracker", "Msic Feature Config has changed");
            ServiceStateTracker.this.pollState();
        }
    };
    private int mCurrentOtaspMode = 0;
    private int mNitzUpdateSpacing = SystemProperties.getInt("ro.nitz_update_spacing", NITZ_UPDATE_SPACING_DEFAULT);
    private int mNitzUpdateDiff = SystemProperties.getInt("ro.nitz_update_diff", NITZ_UPDATE_DIFF_DEFAULT);
    private int mRegistrationState = -1;
    private RegistrantList mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
    private int[] mHomeSystemId = null;
    private int[] mHomeNetworkId = null;
    private boolean mIsMinInfoReady = VDBG;
    private boolean mIsEriTextLoaded = VDBG;
    private boolean mIsSubscriptionFromRuim = VDBG;
    private HbpcdUtils mHbpcdUtils = null;
    private String mCurrentCarrier = null;
    private boolean mNetworkExsit = true;
    private IPlusCodeUtils mPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();
    protected SignalStrength mLastSignalStrength = null;
    int psLac = -1;
    int psCid = -1;
    private String iso = UsimPBMemInfo.STRING_NOT_SET;
    private String mcc = UsimPBMemInfo.STRING_NOT_SET;

    private static int[] m109x804b995f() {
        if (f10xba025bb != null) {
            return f10xba025bb;
        }
        int[] iArr = new int[CommandsInterface.RadioState.valuesCustom().length];
        try {
            iArr[CommandsInterface.RadioState.RADIO_OFF.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[CommandsInterface.RadioState.RADIO_ON.ordinal()] = 3;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[CommandsInterface.RadioState.RADIO_UNAVAILABLE.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        f10xba025bb = iArr;
        return iArr;
    }

    static {
        mEngLoad = SystemProperties.get("ro.build.type").equals("eng");
        mLogLv = SystemProperties.getInt("persist.log.tag.tel_dbg", 0);
        DBG = mEngLoad || mLogLv > 0;
        GMT_COUNTRY_CODES = new String[]{"bf", "ci", "eh", "fo", "gb", "gh", "gm", "gn", "gw", "ie", "lr", "is", "ma", "ml", "mr", "pt", "sl", "sn", Telephony.BaseMmsColumns.STATUS, "tg"};
        sReceiveNitz = new boolean[TelephonyManager.getDefault().getPhoneCount()];
        customEhplmn = new String[][]{new String[]{"46000", "46002", "46004", "46007", "46008"}, new String[]{"45400", "45402", "45418"}, new String[]{"46001", "46009"}, new String[]{"45403", "45404"}, new String[]{"45412", "45413"}, new String[]{"45416", "45419"}, new String[]{"45501", "45504"}, new String[]{"45503", "45505"}, new String[]{"45002", "45008"}, new String[]{"52501", "52502"}, new String[]{"43602", "43612"}, new String[]{"52010", "52099"}, new String[]{"52005", "52018"}, new String[]{"24001", "24005"}, new String[]{"26207", "26208", "26203", "26277"}, new String[]{"23430", "23431", "23432", "23433", "23434"}, new String[]{"72402", "72403", "72404"}, new String[]{"72406", "72410", "72411", "72423"}, new String[]{"72432", "72433", "72434"}, new String[]{"31026", "31031", "310160", "310200", "310210", "310220", "310230", "310240", "310250", SimulatedCommands.FAKE_MCC_MNC, "310270", "310280", "311290", "310300", "310310", "310320", "311330", "310660", "310800"}, new String[]{"310150", "310170", "310380", "310410"}, new String[]{"31033", "310330"}, new String[]{"21401", "21402", "21403", "21404", "21405", "21406", "21407", "21408", "21409", "21410", "21411", "21412", "21413", "21414", "21415", "21416", "21417", "21418", "21419", "21420", "21421"}, new String[]{"20815", "20801"}};
        customOperatorConsiderRoamingMcc = new String[][]{new String[]{"404", "404", "405"}, new String[]{"405", "404", "405"}};
        mCellInfoTimer = null;
    }

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        CellInfoResult(ServiceStateTracker this$0, CellInfoResult cellInfoResult) {
            this();
        }

        private CellInfoResult() {
            this.lockObj = new Object();
        }
    }

    private class SstSubscriptionsChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        public final AtomicInteger mPreviousSubId;

        SstSubscriptionsChangedListener(ServiceStateTracker this$0, SstSubscriptionsChangedListener sstSubscriptionsChangedListener) {
            this();
        }

        private SstSubscriptionsChangedListener() {
            this.mPreviousSubId = new AtomicInteger(-1);
        }

        @Override
        public void onSubscriptionsChanged() {
            int subId = ServiceStateTracker.this.mPhone.getSubId();
            if (ServiceStateTracker.DBG) {
                ServiceStateTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged start " + subId);
            }
            if (this.mPreviousSubId.getAndSet(subId) != subId) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    Context context = ServiceStateTracker.this.mPhone.getContext();
                    ServiceStateTracker.this.mPhone.notifyPhoneStateChanged();
                    ServiceStateTracker.this.mPhone.notifyCallForwardingIndicator();
                    boolean restoreSelection = context.getResources().getBoolean(R.^attr-private.glowDot) ? ServiceStateTracker.VDBG : true;
                    ServiceStateTracker.this.mPhone.sendSubscriptionSettings(restoreSelection);
                    ServiceStateTracker.this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(ServiceStateTracker.this.mSS.getRilDataRadioTechnology()));
                    if (ServiceStateTracker.this.mSpnUpdatePending) {
                        ServiceStateTracker.this.mSubscriptionController.setPlmnSpn(ServiceStateTracker.this.mPhone.getPhoneId(), ServiceStateTracker.this.mCurShowPlmn, ServiceStateTracker.this.mCurPlmn, ServiceStateTracker.this.mCurShowSpn, ServiceStateTracker.this.mCurSpn);
                        ServiceStateTracker.this.mSpnUpdatePending = ServiceStateTracker.VDBG;
                    }
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                    String oldNetworkSelection = sp.getString(Phone.NETWORK_SELECTION_KEY, UsimPBMemInfo.STRING_NOT_SET);
                    String oldNetworkSelectionName = sp.getString(Phone.NETWORK_SELECTION_NAME_KEY, UsimPBMemInfo.STRING_NOT_SET);
                    String oldNetworkSelectionShort = sp.getString(Phone.NETWORK_SELECTION_SHORT_KEY, UsimPBMemInfo.STRING_NOT_SET);
                    boolean skipRestoringSelection = ServiceStateTracker.this.mPhone.getContext().getResources().getBoolean(R.^attr-private.glowDot);
                    if (skipRestoringSelection) {
                        sp.edit().remove(Phone.NETWORK_SELECTION_KEY + subId).remove(Phone.NETWORK_SELECTION_NAME_KEY + subId).remove(Phone.NETWORK_SELECTION_SHORT_KEY + subId).commit();
                    } else if (!TextUtils.isEmpty(oldNetworkSelection) || !TextUtils.isEmpty(oldNetworkSelectionName) || !TextUtils.isEmpty(oldNetworkSelectionShort)) {
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(Phone.NETWORK_SELECTION_KEY + subId, oldNetworkSelection);
                        editor.putString(Phone.NETWORK_SELECTION_NAME_KEY + subId, oldNetworkSelectionName);
                        editor.putString(Phone.NETWORK_SELECTION_SHORT_KEY + subId, oldNetworkSelectionShort);
                        editor.remove(Phone.NETWORK_SELECTION_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_NAME_KEY);
                        editor.remove(Phone.NETWORK_SELECTION_SHORT_KEY);
                        editor.commit();
                    }
                    ServiceStateTracker.this.updateSpnDisplay();
                }
                ServiceStateTracker.this.mPhone.updateVoiceMail();
            }
            if (ServiceStateTracker.this.mSubscriptionController.isReady()) {
                int phoneId = ServiceStateTracker.this.mPhone.getPhoneId();
                ServiceStateTracker.this.log("phoneId= " + phoneId + " ,mSpnUpdatePending= " + ServiceStateTracker.this.mSpnUpdatePending);
                if (ServiceStateTracker.this.mSpnUpdatePending) {
                    ServiceStateTracker.this.mSubscriptionController.setPlmnSpn(phoneId, ServiceStateTracker.this.mCurShowPlmn, ServiceStateTracker.this.mCurPlmn, ServiceStateTracker.this.mCurShowSpn, ServiceStateTracker.this.mCurSpn);
                    ServiceStateTracker.this.mSpnUpdatePending = ServiceStateTracker.VDBG;
                }
            }
        }
    }

    public ServiceStateTracker(GsmCdmaPhone phone, CommandsInterface ci) {
        initOnce(phone, ci);
        updatePhoneType();
    }

    private void initOnce(GsmCdmaPhone phone, CommandsInterface ci) {
        this.mPhone = phone;
        this.mCi = ci;
        this.mVoiceCapable = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.frameDuration);
        this.mSmsCapable = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.fromLeft);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 42, null);
        this.mCi.setOnSignalStrengthUpdate(this, 12, null);
        this.mCi.registerForCellInfoList(this, 44, null);
        this.mSubscriptionController = SubscriptionController.getInstance();
        this.mSubscriptionManager = SubscriptionManager.from(phone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mCi.registerForImsNetworkStateChanged(this, 46, null);
        PowerManager powerManager = (PowerManager) phone.getContext().getSystemService("power");
        this.mWakeLock = powerManager.newWakeLock(1, WAKELOCK_TAG);
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                this.mServiceStateExt = (IServiceStateExt) MPlugin.createInstance(IServiceStateExt.class.getName(), phone.getContext());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        this.mCi.registerForRadioStateChanged(this, 1, null);
        this.mCi.registerForVoiceNetworkStateChanged(this, 2, null);
        this.mCi.setOnNITZTime(this, 11, null);
        this.mCr = phone.getContext().getContentResolver();
        int airplaneMode = Settings.Global.getInt(this.mCr, "airplane_mode_on", 0);
        int enableCellularOnBoot = Settings.Global.getInt(this.mCr, "enable_cellular_on_boot", 1);
        this.mDesiredPowerState = enableCellularOnBoot > 0 && airplaneMode <= 0;
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time"), true, this.mAutoTimeObserver);
        this.mCr.registerContentObserver(Settings.Global.getUriFor("auto_time_zone"), true, this.mAutoTimeZoneObserver);
        setSignalStrengthDefaultValues();
        Context context = this.mPhone.getContext();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mIntentReceiver, filter);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_RADIO_OFF);
        filter2.addAction("android.intent.action.SCREEN_ON");
        filter2.addAction("android.intent.action.SCREEN_OFF");
        filter2.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter2.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
        filter2.addAction("android.intent.action.RADIO_TECHNOLOGY");
        context.registerReceiver(this.mIntentReceiver, filter2);
        this.mEventLog = new TelephonyEventLog(this.mPhone.getPhoneId());
        this.mPhone.notifyOtaspChanged(0);
    }

    public void updatePhoneType() {
        this.mSS = new ServiceState();
        this.mNewSS = new ServiceState();
        this.mLastCellInfoListTime = 0L;
        this.mLastCellInfoList = null;
        this.mSignalStrength = new SignalStrength();
        this.mRestrictedState = new RestrictedState();
        this.mStartedGprsRegCheck = VDBG;
        this.mReportedGprsNoReg = VDBG;
        this.mMdn = null;
        this.mMin = null;
        this.mPrlVersion = null;
        this.mIsMinInfoReady = VDBG;
        this.mNitzUpdatedTime = VDBG;
        cancelPollState();
        if (this.mPhone.isPhoneTypeGsm()) {
            if (this.mCdmaSSM != null) {
                this.mCdmaSSM.dispose(this);
            }
            this.mCi.unregisterForCdmaPrlChanged(this);
            this.mPhone.unregisterForEriFileLoaded(this);
            this.mCi.unregisterForCdmaOtaProvision(this);
            this.mPhone.unregisterForSimRecordsLoaded(this);
            this.mCellLoc = new GsmCellLocation();
            this.mNewCellLoc = new GsmCellLocation();
            this.mCi.registerForAvailable(this, 13, null);
            this.mCi.setOnRestrictedStateChanged(this, 23, null);
            this.mCi.registerForPsNetworkStateChanged(this, 102, null);
            this.mCi.setInvalidSimInfo(this, 101, null);
            this.mCi.registerForModulation(this, EVENT_MODULATION_INFO, null);
            try {
                if (this.mServiceStateExt.isImeiLocked()) {
                    this.mCi.registerForIMEILock(this, EVENT_IMEI_LOCK, null);
                }
            } catch (RuntimeException e) {
                loge("No isImeiLocked");
            }
            this.mCi.registerForIccRefresh(this, 106, null);
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                this.mCi.registerForFemtoCellInfo(this, EVENT_FEMTO_CELL_INFO, null);
            }
            this.mCi.registerForNetworkEvent(this, EVENT_NETWORK_EVENT, null);
            this.mCr.registerContentObserver(Settings.Global.getUriFor("telephony_misc_feature_config"), true, this.mMsicFeatureConfigObserver);
            this.mCr.registerContentObserver(Settings.System.getUriFor("multi_sim_data_call"), true, this.mDataConnectionSettingObserver);
            this.mCr.registerContentObserver(Settings.System.getUriFor("mobile_data"), true, this.mDataConnectionSettingObserver);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.needEMMRRS()) {
                        if (isCurrentPhoneDataConnectionOn()) {
                            getEINFO(105);
                        } else {
                            getEINFO(104);
                        }
                    }
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            }
            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
                setReceivedNitz(i, VDBG);
            }
        } else {
            this.mCi.unregisterForAvailable(this);
            this.mCi.unSetOnRestrictedStateChanged(this);
            this.mPsRestrictDisabledRegistrants.notifyRegistrants();
            this.mCi.unregisterForPsNetworkStateChanged(this);
            this.mCi.unSetInvalidSimInfo(this);
            this.mCi.unregisterForModulation(this);
            try {
                if (this.mServiceStateExt.isImeiLocked()) {
                    this.mCi.unregisterForIMEILock(this);
                }
            } catch (RuntimeException e3) {
                loge("No isImeiLocked");
            }
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                this.mCi.unregisterForFemtoCellInfo(this);
            }
            this.mCi.unregisterForIccRefresh(this);
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                this.mCi.unregisterForFemtoCellInfo(this);
            }
            if (SystemProperties.get("persist.mtk_ims_support").equals("1")) {
                this.mCi.unregisterForImsDisable(this);
                this.mCi.unregisterForImsRegistrationInfo(this);
            }
            this.mCi.unregisterForNetworkEvent(this);
            this.mCr.unregisterContentObserver(this.mMsicFeatureConfigObserver);
            this.mCr.unregisterContentObserver(this.mDataConnectionSettingObserver);
            if (this.mPhone.isPhoneTypeCdmaLte()) {
                this.mPhone.registerForSimRecordsLoaded(this, 16, null);
            }
            this.mCellLoc = new CdmaCellLocation();
            this.mNewCellLoc = new CdmaCellLocation();
            this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(this.mPhone.getContext(), this.mCi, this, 39, null);
            this.mIsSubscriptionFromRuim = this.mCdmaSSM.getCdmaSubscriptionSource() == 0;
            this.mCi.registerForCdmaPrlChanged(this, 40, null);
            this.mPhone.registerForEriFileLoaded(this, 36, null);
            this.mCi.registerForCdmaOtaProvision(this, 37, null);
            this.mHbpcdUtils = new HbpcdUtils(this.mPhone.getContext());
            updateOtaspState();
        }
        onUpdateIccAvailability();
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(0));
        this.mCi.getSignalStrength(obtainMessage(3));
        sendMessage(obtainMessage(50));
    }

    public void requestShutdown() {
        if (this.mDeviceShuttingDown) {
            return;
        }
        this.mDeviceShuttingDown = true;
        this.mDesiredPowerState = VDBG;
        int phoneId = getPhone().getPhoneId();
        RadioManager.getInstance().setModemPower(VDBG, 1 << phoneId);
    }

    public void dispose() {
        this.mCi.unSetOnSignalStrengthUpdate(this);
        this.mUiccController.unregisterForIccChanged(this);
        this.mCi.unregisterForCellInfoList(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mCi.unregisterForImsNetworkStateChanged(this);
        if (this.mPhone.isPhoneTypeGsm()) {
            this.mCi.unregisterForAvailable(this);
            this.mCi.unSetOnRestrictedStateChanged(this);
            this.mCi.unregisterForPsNetworkStateChanged(this);
            this.mCi.unSetInvalidSimInfo(this);
            this.mCi.unregisterForModulation(this);
            try {
                if (this.mServiceStateExt.isImeiLocked()) {
                    this.mCi.unregisterForIMEILock(this);
                }
            } catch (RuntimeException e) {
                loge("No isImeiLocked");
            }
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                this.mCi.unregisterForFemtoCellInfo(this);
            }
            this.mCi.unregisterForIccRefresh(this);
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                this.mCi.unregisterForFemtoCellInfo(this);
            }
            this.mCi.unregisterForNetworkEvent(this);
            this.mCr.unregisterContentObserver(this.mMsicFeatureConfigObserver);
            this.mCr.unregisterContentObserver(this.mDataConnectionSettingObserver);
        }
        if (!this.mPhone.isPhoneTypeCdma() && !this.mPhone.isPhoneTypeCdmaLte()) {
            return;
        }
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
    }

    public boolean getDesiredPowerState() {
        return this.mDesiredPowerState;
    }

    protected boolean notifySignalStrength() {
        if (this.mSignalStrength.equals(this.mLastSignalStrength)) {
            return VDBG;
        }
        try {
            if (DBG) {
                log("notifySignalStrength: mSignalStrength.getLevel=" + this.mSignalStrength.getLevel());
            }
            this.mPhone.notifySignalStrength();
            this.mLastSignalStrength = new SignalStrength(this.mSignalStrength);
            return true;
        } catch (NullPointerException ex) {
            loge("updateSignalStrength() Phone already destroyed: " + ex + "SignalStrength not notified");
            return VDBG;
        }
    }

    protected void notifyDataRegStateRilRadioTechnologyChanged() {
        int rat = this.mSS.getRilDataRadioTechnology();
        int drs = this.mSS.getDataRegState();
        if (DBG) {
            log("notifyDataRegStateRilRadioTechnologyChanged: drs=" + drs + " rat=" + rat);
        }
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(rat));
        this.mDataRegStateOrRatChangedRegistrants.notifyResult(new Pair(Integer.valueOf(drs), Integer.valueOf(rat)));
    }

    protected void useDataRegStateForDataOnlyDevices() {
        if (this.mSmsCapable) {
            return;
        }
        if (DBG) {
            log("useDataRegStateForDataOnlyDevice: VoiceRegState=" + this.mNewSS.getVoiceRegState() + " DataRegState=" + this.mNewSS.getDataRegState());
        }
        this.mNewSS.setVoiceRegState(this.mNewSS.getDataRegState());
        this.mNewSS.setRegState(1);
    }

    protected void updatePhoneObject() {
        boolean isRegistered = true;
        if (!this.mPhone.getContext().getResources().getBoolean(R.^attr-private.minorWeightMin)) {
            return;
        }
        if (this.mSS.getVoiceRegState() != 0 && this.mSS.getVoiceRegState() != 2) {
            isRegistered = false;
        }
        if (!isRegistered) {
            log("updatePhoneObject: Ignore update");
        } else {
            this.mPhone.updatePhoneObject(this.mSS.getRilVoiceRadioTechnology());
        }
    }

    public void registerForVoiceRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOnRegistrants.add(r);
        if (!this.mSS.getVoiceRoaming()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForVoiceRoamingOn(Handler h) {
        this.mVoiceRoamingOnRegistrants.remove(h);
    }

    public void registerForVoiceRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOffRegistrants.add(r);
        if (this.mSS.getVoiceRoaming()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForVoiceRoamingOff(Handler h) {
        this.mVoiceRoamingOffRegistrants.remove(h);
    }

    public void registerForDataRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOnRegistrants.add(r);
        if (!this.mSS.getDataRoaming()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForDataRoamingOn(Handler h) {
        this.mDataRoamingOnRegistrants.remove(h);
    }

    public void registerForDataRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOffRegistrants.add(r);
        if (this.mSS.getDataRoaming()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForDataRoamingOff(Handler h) {
        this.mDataRoamingOffRegistrants.remove(h);
    }

    public void reRegisterNetwork(Message onComplete) {
        this.mCi.getPreferredNetworkType(obtainMessage(19, onComplete));
    }

    public void setRadioPower(boolean power) {
        this.mDesiredPowerState = power;
        setPowerStateToDesired();
    }

    public void enableSingleLocationUpdate() {
        if (this.mWantSingleLocationUpdate || this.mWantContinuousLocationUpdates) {
            return;
        }
        this.mWantSingleLocationUpdate = true;
        this.mCi.setLocationUpdates(true, obtainMessage(18));
    }

    public void enableLocationUpdates() {
        if (this.mWantSingleLocationUpdate || this.mWantContinuousLocationUpdates) {
            return;
        }
        this.mWantContinuousLocationUpdates = true;
        this.mCi.setLocationUpdates(true, obtainMessage(18));
    }

    protected void disableSingleLocationUpdate() {
        this.mWantSingleLocationUpdate = VDBG;
        if (this.mWantSingleLocationUpdate || this.mWantContinuousLocationUpdates) {
            return;
        }
        this.mCi.setLocationUpdates(VDBG, null);
    }

    public void disableLocationUpdates() {
        this.mWantContinuousLocationUpdates = VDBG;
        if (this.mWantSingleLocationUpdate || this.mWantContinuousLocationUpdates) {
            return;
        }
        this.mCi.setLocationUpdates(VDBG, null);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
            case 50:
                log("handle EVENT_RADIO_STATE_CHANGED");
                if (!this.mPhone.isPhoneTypeGsm() && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
                    handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                    queueNextSignalStrengthPoll();
                }
                if (RadioManager.isMSimModeSupport()) {
                    logd("MTK propiertary Power on flow, setRadioPower:  mDesiredPowerState=" + this.mDesiredPowerState + "  phoneId=" + this.mPhone.getPhoneId());
                    RadioManager.getInstance().setRadioPower(this.mDesiredPowerState, this.mPhone.getPhoneId());
                } else {
                    log("BSP package but use MTK Power on flow");
                    RadioManager.getInstance().setRadioPower(this.mDesiredPowerState, this.mPhone.getPhoneId());
                }
                pollState();
                return;
            case 2:
                if (this.mPhone.isPhoneTypeGsm()) {
                    logd("handle EVENT_NETWORK_STATE_CHANGED GSM");
                    onNetworkStateChangeResult((AsyncResult) msg.obj);
                }
                modemTriggeredPollState();
                return;
            case 3:
                log("handle EVENT_GET_SIGNAL_STRENGTH");
                if (this.mCi.getRadioState().isOn()) {
                    onSignalStrengthResult((AsyncResult) msg.obj);
                    queueNextSignalStrengthPoll();
                    return;
                }
                return;
            case 4:
            case 5:
            case 6:
                handlePollStateResult(msg.what, (AsyncResult) msg.obj);
                return;
            case 7:
            case 8:
            case 9:
            case 24:
            case 25:
            case CallFailCause.INVALID_NUMBER_FORMAT:
            case CallFailCause.FACILITY_REJECTED:
            case 30:
            case 31:
            case 32:
            case 33:
            case 41:
            case RadioNVItems.RIL_NV_CDMA_PRL_VERSION:
            case RadioNVItems.RIL_NV_CDMA_BC10:
            case 53:
            case RadioNVItems.RIL_NV_CDMA_SO68:
            case 55:
            case 56:
            case 57:
            case 58:
            case RadioNVItems.RIL_NV_CDMA_EHRPD_FORCED:
            case 60:
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_2:
            case IWorldPhone.EVENT_INVALID_SIM_NOTIFY_3:
            case 63:
            case 64:
            case CallFailCause.BEARER_NOT_IMPLEMENT:
            case 66:
            case 67:
            case CallFailCause.ACM_LIMIT_EXCEEDED:
            case CallFailCause.FACILITY_NOT_IMPLEMENT:
            case 70:
            case 71:
            case 72:
            case 73:
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_25:
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_26:
            case RadioNVItems.RIL_NV_LTE_SCAN_PRIORITY_41:
            case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_25:
            case RadioNVItems.RIL_NV_LTE_HIDDEN_BAND_PRIORITY_26:
            case 79:
            case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
            case 81:
            case RadioNVItems.RIL_NV_LTE_BSR_MAX_TIME:
            case 83:
            case 84:
            case 85:
            case 86:
            case 87:
            case CallFailCause.INCOMPATIBLE_DESTINATION:
            case 89:
            case 90:
            case CallFailCause.INVALID_TRANSIT_NETWORK_SELECTION:
            case 92:
            case 93:
            case 94:
            case CallFailCause.SEMANTICALLY_INCORRECT_MESSAGE:
            case 96:
            case CallFailCause.MESSAGE_TYPE_NON_EXISTENT:
            case CallFailCause.MESSAGE_TYPE_NOT_COMPATIBLE_WITH_PROT_STATE:
            case CallFailCause.IE_NON_EXISTENT_OR_NOT_IMPLEMENTED:
            case 100:
            case 109:
            case 110:
            case 111:
            case 112:
            case 113:
            case CharacterSets.GB18030:
            case 115:
            case 116:
            default:
                log("Unhandled message with number: " + msg.what);
                return;
            case 10:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_POLL_SIGNAL_STRENGTH GSM " + this.mDontPollSignalStrength);
                    if (this.mDontPollSignalStrength) {
                        return;
                    }
                }
                this.mCi.getSignalStrength(obtainMessage(3));
                return;
            case 11:
                log("handle EVENT_NITZ_TIME");
                AsyncResult ar = (AsyncResult) msg.obj;
                String nitzString = (String) ((Object[]) ar.result)[0];
                long nitzReceiveTime = ((Long) ((Object[]) ar.result)[1]).longValue();
                setTimeFromNITZString(nitzString, nitzReceiveTime);
                return;
            case 12:
                log("handle EVENT_SIGNAL_STRENGTH_UPDATE");
                AsyncResult ar2 = (AsyncResult) msg.obj;
                this.mDontPollSignalStrength = true;
                if (this.mPhone.isPhoneTypeGsm() && ar2.exception == null && ar2.result != null) {
                    this.mSignalStrengthChangedRegistrants.notifyResult(new SignalStrength((SignalStrength) ar2.result));
                }
                onSignalStrengthResult(ar2);
                return;
            case 13:
                log("handle EVENT_RADIO_AVAILABLE");
                if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    return;
                }
                log("not BSP package, notify!");
                RadioManager.getInstance().notifyRadioAvailable(this.mPhone.getPhoneId());
                return;
            case 14:
                if (DBG) {
                    log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                }
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (this.mPhone.isPhoneTypeGsm()) {
                    handlePollStateResult(msg.what, ar3);
                    return;
                }
                if (ar3.exception != null || ar3.result == null) {
                    log("Unable to getNetworkSelectionMode");
                    return;
                }
                int[] ints = (int[]) ar3.result;
                if (ints[0] == 1) {
                    this.mPhone.setNetworkSelectionModeAutomatic(null);
                    return;
                }
                return;
            case 15:
                AsyncResult ar4 = (AsyncResult) msg.obj;
                if (ar4.exception == null) {
                    String[] states = (String[]) ar4.result;
                    if (this.mPhone.isPhoneTypeGsm()) {
                        int lac = -1;
                        int cid = -1;
                        if (states.length >= 3) {
                            try {
                                if (states[1] != null && states[1].length() > 0) {
                                    lac = Integer.parseInt(states[1], 16);
                                }
                                if (states[2] != null && states[2].length() > 0) {
                                    cid = Integer.parseInt(states[2], 16);
                                }
                            } catch (NumberFormatException ex) {
                                Rlog.w(LOG_TAG, "error parsing location: " + ex);
                            }
                        }
                        ((GsmCellLocation) this.mCellLoc).setLacAndCid(lac, cid);
                    } else {
                        int networkId = -1;
                        if (states.length > 9) {
                            try {
                                baseStationId = states[4] != null ? Integer.parseInt(states[4]) : -1;
                                baseStationLatitude = states[5] != null ? Integer.parseInt(states[5]) : Integer.MAX_VALUE;
                                baseStationLongitude = states[6] != null ? Integer.parseInt(states[6]) : Integer.MAX_VALUE;
                                if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                                    baseStationLatitude = Integer.MAX_VALUE;
                                    baseStationLongitude = Integer.MAX_VALUE;
                                }
                                systemId = states[8] != null ? Integer.parseInt(states[8]) : -1;
                                if (states[9] != null) {
                                    networkId = Integer.parseInt(states[9]);
                                }
                            } catch (NumberFormatException ex2) {
                                loge("error parsing cell location data: " + ex2);
                            }
                        }
                        ((CdmaCellLocation) this.mCellLoc).setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude, systemId, networkId);
                    }
                    this.mPhone.notifyLocationChanged();
                    break;
                }
                disableSingleLocationUpdate();
                return;
            case 16:
                log("EVENT_SIM_RECORDS_LOADED: what=" + msg.what);
                updatePhoneObject();
                updateOtaspState();
                if (this.mPhone.isPhoneTypeGsm()) {
                    refreshSpnDisplay();
                    if (this.mNeedNotify) {
                        this.mPhone.notifyServiceStateChanged(this.mSS);
                        this.mNeedNotify = VDBG;
                        return;
                    }
                    return;
                }
                return;
            case 17:
                this.mOnSubscriptionsChangedListener.mPreviousSubId.set(-1);
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_SIM_READY GSM");
                    boolean skipRestoringSelection = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.glowDot);
                    if (DBG) {
                        log("skipRestoringSelection=" + skipRestoringSelection);
                    }
                    if (!skipRestoringSelection) {
                        this.mPhone.restoreSavedNetworkSelection(null);
                    }
                }
                pollState();
                queueNextSignalStrengthPoll();
                return;
            case 18:
                log("handle EVENT_LOCATION_UPDATES_ENABLED");
                if (((AsyncResult) msg.obj).exception == null) {
                    this.mCi.getVoiceRegistrationState(obtainMessage(15, null));
                    return;
                }
                return;
            case 19:
                log("handle EVENT_GET_PREFERRED_NETWORK_TYPE");
                AsyncResult ar5 = (AsyncResult) msg.obj;
                if (ar5.exception == null) {
                    this.mPreferredNetworkType = ((int[]) ar5.result)[0];
                } else {
                    this.mPreferredNetworkType = 7;
                }
                Message message = obtainMessage(20, ar5.userObj);
                this.mCi.setPreferredNetworkType(7, message);
                return;
            case 20:
                log("handle EVENT_SET_PREFERRED_NETWORK_TYPE");
                Message message2 = obtainMessage(21, ((AsyncResult) msg.obj).userObj);
                this.mCi.setPreferredNetworkType(this.mPreferredNetworkType, message2);
                return;
            case 21:
                log("handle EVENT_RESET_PREFERRED_NETWORK_TYPE");
                AsyncResult ar6 = (AsyncResult) msg.obj;
                if (ar6.userObj != null) {
                    AsyncResult.forMessage((Message) ar6.userObj).exception = ar6.exception;
                    ((Message) ar6.userObj).sendToTarget();
                    return;
                }
                return;
            case 22:
                log("handle EVENT_CHECK_REPORT_GPRS");
                if (this.mPhone.isPhoneTypeGsm() && this.mSS != null && !isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                    GsmCellLocation loc = (GsmCellLocation) this.mPhone.getCellLocation();
                    Object[] objArr = new Object[2];
                    objArr[0] = this.mSS.getOperatorNumeric();
                    objArr[1] = Integer.valueOf(loc != null ? loc.getCid() : -1);
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL, objArr);
                    this.mReportedGprsNoReg = true;
                }
                this.mStartedGprsRegCheck = VDBG;
                return;
            case 23:
                log("handle EVENT_RESTRICTED_STATE_CHANGED");
                if (this.mPhone.isPhoneTypeGsm()) {
                    if (DBG) {
                        log("EVENT_RESTRICTED_STATE_CHANGED");
                    }
                    onRestrictedStateChanged((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 26:
                if (this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) {
                    this.mIsSubscriptionFromRuim = true;
                }
                if (this.mPhone.isPhoneTypeCdmaLte()) {
                    if (DBG) {
                        log("Receive EVENT_RUIM_READY");
                    }
                    pollState();
                } else {
                    if (DBG) {
                        log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    }
                    getSubscriptionInfoAndStartPollingThreads();
                }
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                return;
            case 27:
                if (this.mPhone.isPhoneTypeGsm()) {
                    return;
                }
                log("EVENT_RUIM_RECORDS_LOADED: what=" + msg.what);
                updatePhoneObject();
                if (this.mPhone.isPhoneTypeCdma()) {
                    updateSpnDisplay();
                    return;
                }
                RuimRecords ruim = (RuimRecords) this.mIccRecords;
                if (ruim != null) {
                    if (ruim.isProvisioned()) {
                        this.mMdn = ruim.getMdn();
                        this.mMin = ruim.getMin();
                        parseSidNid(ruim.getSid(), ruim.getNid());
                        this.mPrlVersion = ruim.getPrlVersion();
                        this.mIsMinInfoReady = true;
                    }
                    updateOtaspState();
                    updateSpnDisplay();
                    notifyCdmaSubscriptionInfoReady();
                }
                pollState();
                return;
            case 34:
                if (this.mPhone.isPhoneTypeGsm()) {
                    return;
                }
                AsyncResult ar7 = (AsyncResult) msg.obj;
                if (ar7.exception == null) {
                    String[] cdmaSubscription = (String[]) ar7.result;
                    if (cdmaSubscription == null || cdmaSubscription.length < 5) {
                        if (DBG) {
                            log("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription params num=" + cdmaSubscription.length);
                            return;
                        }
                        return;
                    }
                    this.mMdn = cdmaSubscription[0];
                    parseSidNid(cdmaSubscription[1], cdmaSubscription[2]);
                    this.mMin = cdmaSubscription[3];
                    this.mPrlVersion = cdmaSubscription[4];
                    if (DBG) {
                        log("GET_CDMA_SUBSCRIPTION: MDN=" + this.mMdn);
                    }
                    this.mIsMinInfoReady = true;
                    updateOtaspState();
                    notifyCdmaSubscriptionInfoReady();
                    if (this.mIsSubscriptionFromRuim || this.mIccRecords == null) {
                        if (DBG) {
                            log("GET_CDMA_SUBSCRIPTION either mIccRecords is null or NV type device - not setting Imsi in mIccRecords");
                            return;
                        }
                        return;
                    } else {
                        if (DBG) {
                            log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                        }
                        this.mIccRecords.setImsi(getImsi());
                        return;
                    }
                }
                return;
            case 35:
                updatePhoneObject();
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                getSubscriptionInfoAndStartPollingThreads();
                return;
            case 36:
                if (DBG) {
                    log("ERI file has been loaded, repolling.");
                }
                pollState();
                return;
            case 37:
                AsyncResult ar8 = (AsyncResult) msg.obj;
                if (ar8.exception == null) {
                    int[] ints2 = (int[]) ar8.result;
                    int otaStatus = ints2[0];
                    if (otaStatus == 8 || otaStatus == 10) {
                        if (DBG) {
                            log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                        }
                        this.mCi.getCDMASubscription(obtainMessage(34));
                        return;
                    }
                    return;
                }
                return;
            case 38:
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff && msg.arg1 == this.mPendingRadioPowerOffAfterDataOffTag) {
                        if (DBG) {
                            log("EVENT_SET_RADIO_OFF, turn radio off now.");
                        }
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOffTag++;
                        this.mPendingRadioPowerOffAfterDataOff = VDBG;
                    } else {
                        log("EVENT_SET_RADIO_OFF is stale arg1=" + msg.arg1 + "!= tag=" + this.mPendingRadioPowerOffAfterDataOffTag);
                    }
                    break;
                }
            case 39:
                handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                return;
            case 40:
                AsyncResult ar9 = (AsyncResult) msg.obj;
                if (ar9.exception == null) {
                    int[] ints3 = (int[]) ar9.result;
                    this.mPrlVersion = Integer.toString(ints3[0]);
                    return;
                }
                return;
            case 42:
                onUpdateIccAvailability();
                return;
            case 43:
            case EVENT_GET_CELL_INFO_LIST_BY_RATE:
                AsyncResult ar10 = (AsyncResult) msg.obj;
                CellInfoResult result = (CellInfoResult) ar10.userObj;
                synchronized (result.lockObj) {
                    if (ar10.exception != null) {
                        log("EVENT_GET_CELL_INFO_LIST: error ret null, e=" + ar10.exception);
                        result.list = null;
                    } else {
                        result.list = (List) ar10.result;
                        if (DBG) {
                            log("EVENT_GET_CELL_INFO_LIST: size=" + result.list.size() + " list=" + result.list);
                        }
                    }
                    this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    this.mLastCellInfoList = result.list;
                    if (msg.what == EVENT_GET_CELL_INFO_LIST_BY_RATE) {
                        log("EVENT_GET_CELL_INFO_LIST_BY_RATE notify result");
                        this.mPhone.notifyCellInfo(result.list);
                    } else {
                        result.lockObj.notify();
                        log("EVENT_GET_CELL_INFO_LIST notify result");
                    }
                }
                return;
            case 44:
                AsyncResult ar11 = (AsyncResult) msg.obj;
                if (ar11.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + ar11.exception);
                    return;
                }
                List<CellInfo> list = (List) ar11.result;
                this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                this.mLastCellInfoList = list;
                this.mPhone.notifyCellInfo(list);
                return;
            case EVENT_CHANGE_IMS_STATE:
                log("handle EVENT_CHANGE_IMS_STATE");
                if (DBG) {
                    log("EVENT_CHANGE_IMS_STATE:");
                }
                setPowerStateToDesired();
                return;
            case 46:
                this.mCi.getImsRegistrationState(obtainMessage(47));
                return;
            case 47:
                AsyncResult ar12 = (AsyncResult) msg.obj;
                if (ar12.exception == null) {
                    int[] responseArray = (int[]) ar12.result;
                    this.mImsRegistered = responseArray[0] == 1 ? true : VDBG;
                    return;
                }
                return;
            case EVENT_IMS_CAPABILITY_CHANGED:
                if (DBG) {
                    log("EVENT_IMS_CAPABILITY_CHANGED");
                }
                updateSpnDisplay();
                return;
            case 49:
                log("handle EVENT_ALL_DATA_DISCONNECTED");
                int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                ProxyController.getInstance().unregisterForAllDataDisconnected(dds, this);
                synchronized (this) {
                    if (!this.mPendingRadioPowerOffAfterDataOff) {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    } else {
                        if (DBG) {
                            log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        }
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = VDBG;
                    }
                    break;
                }
                break;
            case 101:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_INVALID_SIM_INFO GSM");
                    onInvalidSimInfoReceived((AsyncResult) msg.obj);
                    return;
                }
                return;
            case 102:
                if (this.mPhone.isPhoneTypeGsm()) {
                    logd("handle EVENT_PS_NETWORK_STATE_CHANGED");
                    onPsNetworkStateChangeResult((AsyncResult) msg.obj);
                    modemTriggeredPollState();
                    return;
                }
                return;
            case EVENT_IMEI_LOCK:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_IMEI_LOCK GSM");
                    this.mIsImeiLock = true;
                    return;
                }
                return;
            case 104:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_DISABLE_EMMRRS_STATUS GSM");
                    AsyncResult ar13 = (AsyncResult) msg.obj;
                    if (ar13.exception == null) {
                        String[] data = (String[]) ar13.result;
                        log("EVENT_DISABLE_EMMRRS_STATUS, data[0] is : " + data[0]);
                        log("EVENT_DISABLE_EMMRRS_STATUS, einfo value is : " + data[0].substring(8));
                        try {
                            int oldValue = Integer.valueOf(data[0].substring(8)).intValue();
                            int value = oldValue & 65407;
                            log("EVENT_DISABLE_EMMRRS_STATUS, einfo value change is : " + value);
                            if (oldValue != value) {
                                setEINFO(value, null);
                            }
                        } catch (NumberFormatException ex3) {
                            loge("Unexpected einfo value : " + ex3);
                        }
                        break;
                    }
                    log("EVENT_DISABLE_EMMRRS_STATUS GSM end");
                    return;
                }
                return;
            case 105:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_ENABLE_EMMRRS_STATUS GSM");
                    AsyncResult ar14 = (AsyncResult) msg.obj;
                    if (ar14.exception == null) {
                        String[] data2 = (String[]) ar14.result;
                        log("EVENT_ENABLE_EMMRRS_STATUS, data[0] is : " + data2[0]);
                        log("EVENT_ENABLE_EMMRRS_STATUS, einfo value is : " + data2[0].substring(8));
                        int oldValue2 = Integer.valueOf(data2[0].substring(8)).intValue();
                        int value2 = oldValue2 | 128;
                        log("EVENT_ENABLE_EMMRRS_STATUS, einfo value change is : " + value2);
                        if (oldValue2 != value2) {
                            setEINFO(value2, null);
                        }
                    }
                    log("EVENT_ENABLE_EMMRRS_STATUS GSM end");
                    return;
                }
                return;
            case 106:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_ICC_REFRESH");
                    AsyncResult ar15 = (AsyncResult) msg.obj;
                    if (ar15.exception == null) {
                        IccRefreshResponse res = (IccRefreshResponse) ar15.result;
                        if (res == null) {
                            log("IccRefreshResponse is null");
                            return;
                        }
                        switch (res.refreshResult) {
                            case 0:
                            case 5:
                                if (res.efId == 28423) {
                                    this.mLastRegisteredPLMN = null;
                                    this.mLastPSRegisteredPLMN = null;
                                    log("Reset flag of IVSR for IMSI update");
                                    return;
                                }
                                return;
                            case 1:
                            case 2:
                            case 3:
                            default:
                                log("GSST EVENT_ICC_REFRESH IccRefreshResponse =" + res);
                                return;
                            case 4:
                            case 6:
                                this.mLastRegisteredPLMN = null;
                                this.mLastPSRegisteredPLMN = null;
                                log("Reset mLastRegisteredPLMN/mLastPSRegisteredPLMNfor ICC refresh");
                                return;
                        }
                    }
                    return;
                }
                return;
            case EVENT_FEMTO_CELL_INFO:
                log("handle EVENT_FEMTO_CELL_INFO");
                onFemtoCellInfoResult((AsyncResult) msg.obj);
                return;
            case EVENT_MODULATION_INFO:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_MODULATION_INFO GSM");
                    onModulationInfoReceived((AsyncResult) msg.obj);
                    return;
                }
                return;
            case EVENT_NETWORK_EVENT:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("handle EVENT_NETWORK_EVENT");
                    onNetworkEventReceived((AsyncResult) msg.obj);
                    return;
                }
                return;
        }
    }

    protected int calculateDeviceRatMode(int phoneId) {
        int networkType = -1;
        if (this.mPhone.isPhoneTypeGsm()) {
            int restrictedNwMode = -1;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.isSupportRatBalancing()) {
                        logd("networkType is controlled by RAT Blancing, no need to set network type");
                        return -1;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    restrictedNwMode = this.mServiceStateExt.needAutoSwitchRatMode(phoneId, this.mLocatedPlmn);
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            }
            networkType = getPreferredNetworkModeSettings(phoneId);
            logd("restrictedNwMode = " + restrictedNwMode);
            if (restrictedNwMode >= 0 && restrictedNwMode != networkType) {
                logd("Revise networkType to " + restrictedNwMode);
                networkType = restrictedNwMode;
            }
        } else {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    networkType = this.mServiceStateExt.getNetworkTypeForMota(phoneId);
                    log("[CDMA], networkType for mota is: " + networkType);
                } catch (RuntimeException e3) {
                    e3.printStackTrace();
                }
            }
            if (networkType == -1) {
                networkType = getPreferredNetworkModeSettings(phoneId);
            }
        }
        logd("calculateDeviceRatMode = " + networkType);
        return networkType;
    }

    protected void setDeviceRatMode(int phoneId) {
        int networkType = calculateDeviceRatMode(phoneId);
        log("[setDeviceRatMode]: " + networkType);
        if (networkType < 0) {
            return;
        }
        this.mPhone.setPreferredNetworkType(networkType, null);
    }

    public boolean isPsRegStateRoamByUnsol() {
        return regCodeIsRoaming(this.mPsRegStateRaw);
    }

    protected boolean isSidsAllZeros() {
        if (this.mHomeSystemId != null) {
            for (int i = 0; i < this.mHomeSystemId.length; i++) {
                if (this.mHomeSystemId[i] != 0) {
                    return VDBG;
                }
            }
            return true;
        }
        return true;
    }

    private boolean isHomeSid(int sid) {
        if (this.mHomeSystemId != null) {
            for (int i = 0; i < this.mHomeSystemId.length; i++) {
                if (sid == this.mHomeSystemId[i]) {
                    return true;
                }
            }
            return VDBG;
        }
        return VDBG;
    }

    public String getMdnNumber() {
        return this.mMdn;
    }

    public String getCdmaMin() {
        return this.mMin;
    }

    public String getPrlVersion() {
        return this.mPrlVersion;
    }

    public String getImsi() {
        String operatorNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (TextUtils.isEmpty(operatorNumeric) || getCdmaMin() == null) {
            return null;
        }
        return operatorNumeric + getCdmaMin();
    }

    public boolean isMinInfoReady() {
        return this.mIsMinInfoReady;
    }

    public int getOtasp() {
        int provisioningState;
        if (!this.mPhone.getIccRecordsLoaded()) {
            if (DBG) {
                log("getOtasp: otasp uninitialized due to sim not loaded");
            }
            return 0;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (DBG) {
                log("getOtasp: otasp not needed for GSM");
                return 3;
            }
            return 3;
        }
        if (this.mIsSubscriptionFromRuim && this.mMin == null) {
            return 2;
        }
        if (this.mMin == null || this.mMin.length() < 6) {
            if (DBG) {
                log("getOtasp: bad mMin='" + this.mMin + "'");
            }
            provisioningState = 1;
        } else if (this.mMin.equals(UNACTIVATED_MIN_VALUE) || this.mMin.substring(0, 6).equals(UNACTIVATED_MIN2_VALUE) || SystemProperties.getBoolean("test_cdma_setup", VDBG)) {
            provisioningState = 2;
        } else {
            provisioningState = 3;
        }
        if (DBG) {
            log("getOtasp: state=" + provisioningState);
        }
        return provisioningState;
    }

    protected void parseSidNid(String sidStr, String nidStr) {
        if (sidStr != null) {
            String[] sid = sidStr.split(",");
            this.mHomeSystemId = new int[sid.length];
            for (int i = 0; i < sid.length; i++) {
                try {
                    this.mHomeSystemId[i] = Integer.parseInt(sid[i]);
                } catch (NumberFormatException ex) {
                    loge("error parsing system id: " + ex);
                }
            }
        }
        if (DBG) {
            log("CDMA_SUBSCRIPTION: SID=" + sidStr);
        }
        if (nidStr != null) {
            String[] nid = nidStr.split(",");
            this.mHomeNetworkId = new int[nid.length];
            for (int i2 = 0; i2 < nid.length; i2++) {
                try {
                    this.mHomeNetworkId[i2] = Integer.parseInt(nid[i2]);
                } catch (NumberFormatException ex2) {
                    loge("CDMA_SUBSCRIPTION: error parsing network id: " + ex2);
                }
            }
        }
        if (DBG) {
            log("CDMA_SUBSCRIPTION: NID=" + nidStr);
        }
    }

    protected void updateOtaspState() {
        int otaspMode = getOtasp();
        int oldOtaspMode = this.mCurrentOtaspMode;
        this.mCurrentOtaspMode = otaspMode;
        if (oldOtaspMode == this.mCurrentOtaspMode) {
            return;
        }
        if (DBG) {
            log("updateOtaspState: call notifyOtaspChanged old otaspMode=" + oldOtaspMode + " new otaspMode=" + this.mCurrentOtaspMode);
        }
        this.mPhone.notifyOtaspChanged(this.mCurrentOtaspMode);
    }

    protected Phone getPhone() {
        return this.mPhone;
    }

    protected void handlePollStateResult(int what, AsyncResult ar) {
        this.psLac = -1;
        this.psCid = -1;
        boolean ignore = VDBG;
        if (ar.userObj != this.mPollingContext) {
            ignore = true;
        }
        if (what == 4) {
            logd("handle EVENT_POLL_STATE_REGISTRATION" + (ignore ? " return due to (ar.userObj != mPollingContext)" : UsimPBMemInfo.STRING_NOT_SET));
        } else if (what == 5) {
            logd("handle EVENT_POLL_STATE_GPRS" + (ignore ? " return due to (ar.userObj != mPollingContext)" : UsimPBMemInfo.STRING_NOT_SET));
        } else if (what == 6) {
            logd("handle EVENT_POLL_STATE_OPERATOR" + (ignore ? " return due to (ar.userObj != mPollingContext)" : UsimPBMemInfo.STRING_NOT_SET));
        }
        if (ignore) {
            return;
        }
        if (ar.exception != null) {
            CommandException.Error err = ar.exception instanceof CommandException ? ((CommandException) ar.exception).getCommandError() : null;
            if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                cancelPollState();
                loge("handlePollStateResult cancelPollState due to RADIO_NOT_AVAILABLE");
                if (this.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_ON) {
                    this.mNewSS.setStateOff();
                    this.mNewCellLoc.setStateInvalid();
                    setSignalStrengthDefaultValues();
                    this.mGotCountryCode = VDBG;
                    this.mNitzUpdatedTime = VDBG;
                    setNullState();
                    this.mPsRegStateRaw = 0;
                    pollStateDone();
                    loge("handlePollStateResult pollStateDone to notify RADIO_NOT_AVAILABLE");
                    return;
                }
                return;
            }
            if (err != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                loge("RIL implementation has returned an error where it must succeed" + ar.exception);
            }
        } else {
            try {
                handlePollStateResultMessage(what, ar);
            } catch (RuntimeException ex) {
                loge("Exception while polling service state. Probably malformed RIL response." + ex);
            }
        }
        this.mPollingContext[0] = r8[0] - 1;
        if (this.mPollingContext[0] == 0) {
            if (this.mPhone.isPhoneTypeGsm()) {
                if (this.mPendingPsRestrictDisabledNotify) {
                    this.mPsRestrictDisabledRegistrants.notifyRegistrants();
                    setNotification(1002);
                    this.mPendingPsRestrictDisabledNotify = VDBG;
                }
                if (this.mNewSS.getState() != 0 && this.mNewSS.getDataRegState() == 0) {
                    log("update cellLoc by +CGREG");
                    ((GsmCellLocation) this.mNewCellLoc).setLacAndCid(this.psLac, this.psCid);
                }
                updateRoamingState();
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
            } else {
                boolean namMatch = VDBG;
                if (!isSidsAllZeros() && isHomeSid(this.mNewSS.getSystemId())) {
                    namMatch = true;
                }
                if (this.mIsSubscriptionFromRuim) {
                    this.mNewSS.setVoiceRoaming(isRoamingBetweenOperators(this.mNewSS.getVoiceRoaming(), this.mNewSS));
                }
                boolean isVoiceInService = this.mNewSS.getVoiceRegState() == 0 ? true : VDBG;
                int dataRegType = this.mNewSS.getRilDataRadioTechnology();
                if (isVoiceInService && ServiceState.isCdma(dataRegType)) {
                    this.mNewSS.setDataRoaming(this.mNewSS.getVoiceRoaming());
                }
                this.mEmergencyOnly = VDBG;
                if (this.mCi.getRadioState().isOn() && this.mNewSS.getVoiceRegState() == 1 && this.mNewSS.getDataRegState() == 1 && this.mNetworkExsit) {
                    log("[CDMA]handlePollStateResult: OUT_OF_SERVICE, mEmergencyOnly=true");
                    this.mEmergencyOnly = true;
                }
                if (DBG) {
                    log("[CDMA]handlePollStateResult: set mEmergencyOnly=" + this.mEmergencyOnly + ", mNetworkExsit=" + this.mNetworkExsit);
                }
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
                this.mNewSS.setCdmaDefaultRoamingIndicator(this.mDefaultRoamingIndicator);
                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                boolean isPrlLoaded = TextUtils.isEmpty(this.mPrlVersion) ? VDBG : true;
                if (!isPrlLoaded || this.mNewSS.getRilVoiceRadioTechnology() == 0) {
                    log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    this.mNewSS.setCdmaRoamingIndicator(1);
                } else if (!isSidsAllZeros()) {
                    if (!namMatch && !this.mIsInPrl) {
                        this.mNewSS.setCdmaRoamingIndicator(this.mDefaultRoamingIndicator);
                    } else if (!namMatch || this.mIsInPrl) {
                        if ((namMatch || !this.mIsInPrl) && this.mRoamingIndicator <= 2) {
                            this.mNewSS.setCdmaRoamingIndicator(1);
                        } else {
                            this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                        }
                    } else if (this.mNewSS.getRilVoiceRadioTechnology() == 14) {
                        log("Turn off roaming indicator as voice is LTE");
                        this.mNewSS.setCdmaRoamingIndicator(1);
                    } else {
                        this.mNewSS.setCdmaRoamingIndicator(2);
                    }
                }
                int roamingIndicator = this.mNewSS.getCdmaRoamingIndicator();
                this.mNewSS.setCdmaEriIconIndex(this.mPhone.mEriManager.getCdmaEriIconIndex(roamingIndicator, this.mDefaultRoamingIndicator));
                this.mNewSS.setCdmaEriIconMode(this.mPhone.mEriManager.getCdmaEriIconMode(roamingIndicator, this.mDefaultRoamingIndicator));
                if (DBG) {
                    log("Set CDMA Roaming Indicator to: " + this.mNewSS.getCdmaRoamingIndicator() + ". voiceRoaming = " + this.mNewSS.getVoiceRoaming() + ". dataRoaming = " + this.mNewSS.getDataRoaming() + ", isPrlLoaded = " + isPrlLoaded + ". namMatch = " + namMatch + " , mIsInPrl = " + this.mIsInPrl + ", mRoamingIndicator = " + this.mRoamingIndicator + ", mDefaultRoamingIndicator= " + this.mDefaultRoamingIndicator);
                }
            }
            pollStateDone();
        }
    }

    private boolean isRoamingBetweenOperators(boolean cdmaRoaming, ServiceState s) {
        if (!cdmaRoaming || isSameOperatorNameFromSimAndSS(s)) {
            return VDBG;
        }
        return true;
    }

    void handlePollStateResultMessage(int what, AsyncResult ar) {
        switch (what) {
            case 4:
                if (!this.mPhone.isPhoneTypeGsm()) {
                    String[] states = (String[]) ar.result;
                    if (states.length < 14) {
                        throw new RuntimeException("Warning! Wrong number of parameters returned from RIL_REQUEST_REGISTRATION_STATE: expected 14 or more strings and got " + states.length + " strings");
                    }
                    try {
                        registrationState = states[0] != null ? Integer.parseInt(states[0]) : 4;
                        radioTechnology = states[3] != null ? Integer.parseInt(states[3]) : -1;
                        baseStationId = states[4] != null ? Integer.parseInt(states[4]) : -1;
                        baseStationLatitude = states[5] != null ? Integer.parseInt(states[5]) : Integer.MAX_VALUE;
                        baseStationLongitude = states[6] != null ? Integer.parseInt(states[6]) : Integer.MAX_VALUE;
                        if (baseStationLatitude == 0 && baseStationLongitude == 0) {
                            baseStationLatitude = Integer.MAX_VALUE;
                            baseStationLongitude = Integer.MAX_VALUE;
                        }
                        cssIndicator = states[7] != null ? Integer.parseInt(states[7]) : 0;
                        systemId = states[8] != null ? Integer.parseInt(states[8]) : 0;
                        networkId = states[9] != null ? Integer.parseInt(states[9]) : 0;
                        roamingIndicator = states[10] != null ? Integer.parseInt(states[10]) : -1;
                        systemIsInPrl = states[11] != null ? Integer.parseInt(states[11]) : 0;
                        defaultRoamingIndicator = states[12] != null ? Integer.parseInt(states[12]) : 0;
                        reasonForDenial = states[13] != null ? Integer.parseInt(states[13]) : 0;
                        if (states.length > 15 && states[15] != null) {
                            this.mNetworkExsit = 1 == Integer.parseInt(states[15]) ? true : VDBG;
                        }
                        break;
                    } catch (NumberFormatException ex) {
                        loge("EVENT_POLL_STATE_REGISTRATION_CDMA: error parsing: " + ex);
                    }
                    this.mRegistrationState = registrationState;
                    boolean cdmaRoaming = (!regCodeIsRoaming(registrationState) || isRoamIndForHomeSystem(states[10])) ? VDBG : true;
                    this.mNewSS.setVoiceRoaming(cdmaRoaming);
                    if (cdmaRoaming) {
                        this.mNewSS.setRilVoiceRegState(5);
                    } else {
                        this.mNewSS.setRilVoiceRegState(registrationState);
                    }
                    this.mNewSS.setVoiceRegState(regCodeToServiceState(registrationState));
                    this.mNewSS.setRilVoiceRadioTechnology(radioTechnology);
                    this.mNewSS.setCssIndicator(cssIndicator);
                    this.mNewSS.setSystemAndNetworkId(systemId, networkId);
                    this.mRoamingIndicator = roamingIndicator;
                    this.mIsInPrl = systemIsInPrl == 0 ? VDBG : true;
                    this.mDefaultRoamingIndicator = defaultRoamingIndicator;
                    ((CdmaCellLocation) this.mNewCellLoc).setCellLocationData(baseStationId, baseStationLatitude, baseStationLongitude, systemId, networkId);
                    if (reasonForDenial == 0) {
                        this.mRegistrationDeniedReason = REGISTRATION_DENIED_GEN;
                    } else if (reasonForDenial == 1) {
                        this.mRegistrationDeniedReason = REGISTRATION_DENIED_AUTH;
                    } else {
                        this.mRegistrationDeniedReason = UsimPBMemInfo.STRING_NOT_SET;
                    }
                    if (this.mRegistrationState == 3 && DBG) {
                        log("Registration denied, " + this.mRegistrationDeniedReason);
                        return;
                    }
                    return;
                }
                String[] states2 = (String[]) ar.result;
                int lac = -1;
                int cid = -1;
                int regState = 4;
                int psc = -1;
                if (states2.length > 0) {
                    try {
                        regState = Integer.parseInt(states2[0]);
                        if (states2.length >= 3) {
                            if (states2[1] != null && states2[1].length() > 0) {
                                int tempLac = Integer.parseInt(states2[1], 16);
                                if (tempLac < 0) {
                                    log("set Lac to previous value");
                                    tempLac = ((GsmCellLocation) this.mCellLoc).getLac();
                                }
                                lac = tempLac;
                            }
                            if (states2[2] != null && states2[2].length() > 0) {
                                int tempCid = Integer.parseInt(states2[2], 16);
                                if (tempCid < 0) {
                                    log("set Cid to previous value");
                                    tempCid = ((GsmCellLocation) this.mCellLoc).getCid();
                                }
                                cid = tempCid;
                            }
                            if (states2.length >= 4 && states2[3] != null && states2[3].length() > 0) {
                                updateNetworkInfo(regState, Integer.parseInt(states2[3]));
                            }
                            if (states2.length >= 14 && states2[13] != null && states2[13].length() > 0) {
                                int rejCause = Integer.parseInt(states2[13]);
                                this.mNewSS.setVoiceRejectCause(rejCause);
                                logd("set voice reject cause to " + rejCause);
                            }
                        }
                        if (states2.length > 14 && states2[14] != null && states2[14].length() > 0) {
                            psc = Integer.parseInt(states2[14], 16);
                        }
                        log("EVENT_POLL_STATE_REGISTRATION mSS getRilVoiceRadioTechnology:" + this.mSS.getRilVoiceRadioTechnology() + ", regState:" + regState + ", NewSS RilVoiceRadioTechnology:" + this.mNewSS.getRilVoiceRadioTechnology() + ", lac:" + lac + ", cid:" + cid);
                    } catch (NumberFormatException ex2) {
                        loge("error parsing RegistrationState: " + ex2);
                    }
                    break;
                }
                this.mGsmRoaming = regCodeIsRoaming(regState);
                this.mNewSS.setVoiceRegState(regCodeToServiceState(regState));
                boolean isVoiceCapable = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.frameDuration);
                if ((regState == 13 || regState == 10 || regState == 12 || regState == 14) && isVoiceCapable) {
                    this.mEmergencyOnly = true;
                } else {
                    this.mEmergencyOnly = VDBG;
                }
                log("regState = " + regState + ", isVoiceCapable = " + isVoiceCapable + ", mEmergencyOnly = " + this.mEmergencyOnly);
                if (states2.length > 3) {
                    logd("states.length > 3");
                    if (lac == 65534 || cid == 268435455) {
                        log("unknown lac:" + lac + " or cid:" + cid);
                    } else if (regCodeToServiceState(regState) != 1) {
                        ((GsmCellLocation) this.mNewCellLoc).setLacAndCid(lac, cid);
                    }
                }
                ((GsmCellLocation) this.mNewCellLoc).setPsc(psc);
                return;
            case 5:
                if (this.mPhone.isPhoneTypeGsm()) {
                    String[] states3 = (String[]) ar.result;
                    int type = 0;
                    int regState2 = 4;
                    this.mNewReasonDataDenied = -1;
                    this.mNewMaxDataCalls = 1;
                    if (states3.length > 0) {
                        try {
                            regState2 = Integer.parseInt(states3[0]);
                            if (states3.length >= 3) {
                                if (states3[1] != null && states3[1].length() > 0) {
                                    int tempLac2 = Integer.parseInt(states3[1], 16);
                                    if (tempLac2 < 0) {
                                        log("set Lac to previous value");
                                        tempLac2 = ((GsmCellLocation) this.mCellLoc).getLac();
                                    }
                                    this.psLac = tempLac2;
                                }
                                if (states3[2] != null && states3[2].length() > 0) {
                                    int tempCid2 = Integer.parseInt(states3[2], 16);
                                    if (tempCid2 < 0) {
                                        log("set Cid to previous value");
                                        tempCid2 = ((GsmCellLocation) this.mCellLoc).getCid();
                                    }
                                    this.psCid = tempCid2;
                                }
                            }
                            if (states3.length >= 4 && states3[3] != null) {
                                type = Integer.parseInt(states3[3]);
                            }
                            if (states3.length >= 5 && states3[4] != null && regState2 == 3) {
                                this.mNewReasonDataDenied = Integer.parseInt(states3[4]);
                                log("<mNewReasonDataDenied> " + this.mNewReasonDataDenied);
                                this.mNewSS.setDataRejectCause(this.mNewReasonDataDenied);
                                log("set data reject cause to " + this.mNewReasonDataDenied);
                            }
                            if (states3.length >= 6 && states3[5] != null) {
                                this.mNewMaxDataCalls = Integer.parseInt(states3[5]);
                                logd("<mNewMaxDataCalls> " + this.mNewMaxDataCalls);
                            }
                        } catch (NumberFormatException ex3) {
                            loge("error parsing GprsRegistrationState: " + ex3);
                        }
                        break;
                    }
                    int dataRegState = regCodeToServiceState(regState2);
                    this.mNewSS.setRilDataRegState(regState2);
                    this.mNewSS.setDataRegState(dataRegState);
                    this.mDataRoaming = regCodeIsRoaming(regState2);
                    this.mNewSS.setRilDataRadioTechnology(type);
                    this.mNewSS.setProprietaryDataRadioTechnology(type);
                    if (DBG) {
                        log("handlPollStateResultMessage: GsmSST setDataRegState=" + dataRegState + " regState=" + regState2 + " dataRadioTechnology=" + type);
                        return;
                    }
                    return;
                }
                if (this.mPhone.isPhoneTypeCdma()) {
                    String[] states4 = (String[]) ar.result;
                    if (DBG) {
                        log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" + states4.length + " states=" + states4);
                    }
                    int regState3 = 4;
                    int dataRadioTechnology = 0;
                    if (states4.length > 0) {
                        try {
                            regState3 = Integer.parseInt(states4[0]);
                            if (states4.length >= 4 && states4[3] != null) {
                                dataRadioTechnology = Integer.parseInt(states4[3]);
                            }
                        } catch (NumberFormatException ex4) {
                            loge("handlePollStateResultMessage: error parsing GprsRegistrationState: " + ex4);
                        }
                        break;
                    }
                    int dataRegState2 = regCodeToServiceState(regState3);
                    this.mNewSS.setDataRegState(dataRegState2);
                    this.mNewSS.setRilDataRegState(regState3);
                    this.mNewSS.setRilDataRadioTechnology(dataRadioTechnology);
                    this.mNewSS.setDataRoaming(regCodeIsRoaming(regState3));
                    if (DBG) {
                        log("handlPollStateResultMessage: cdma setDataRegState=" + dataRegState2 + " regState=" + regState3 + " dataRadioTechnology=" + dataRadioTechnology);
                        return;
                    }
                    return;
                }
                String[] states5 = (String[]) ar.result;
                if (DBG) {
                    log("handlePollStateResultMessage: EVENT_POLL_STATE_GPRS states.length=" + states5.length + " states=" + states5);
                }
                int newDataRAT = 0;
                int regState4 = -1;
                if (states5.length > 0) {
                    try {
                        regState4 = Integer.parseInt(states5[0]);
                        if (states5.length >= 4 && states5[3] != null) {
                            newDataRAT = Integer.parseInt(states5[3]);
                        }
                    } catch (NumberFormatException ex5) {
                        loge("handlePollStateResultMessage: error parsing GprsRegistrationState: " + ex5);
                    }
                    break;
                }
                int oldDataRAT = this.mSS.getRilDataRadioTechnology();
                if ((oldDataRAT == 0 && newDataRAT != 0) || ((ServiceState.isCdma(oldDataRAT) && newDataRAT == 14) || (oldDataRAT == 14 && ServiceState.isCdma(newDataRAT)))) {
                    this.mCi.getSignalStrength(obtainMessage(3));
                }
                this.mNewSS.setRilDataRadioTechnology(newDataRAT);
                int dataRegState3 = regCodeToServiceState(regState4);
                this.mNewSS.setDataRegState(dataRegState3);
                this.mNewSS.setRilDataRegState(regState4);
                this.mNewSS.setProprietaryDataRadioTechnology(newDataRAT);
                boolean isDateRoaming = regCodeIsRoaming(regState4);
                this.mNewSS.setDataRoaming(isDateRoaming);
                if (isDateRoaming) {
                    this.mNewSS.setRilDataRegState(5);
                }
                if (DBG) {
                    log("handlPollStateResultMessage: CdmaLteSST setDataRegState=" + dataRegState3 + " regState=" + regState4 + " dataRadioTechnology=" + newDataRAT);
                    return;
                }
                return;
            case 6:
                if (this.mPhone.isPhoneTypeGsm()) {
                    String[] opNames = (String[]) ar.result;
                    if (opNames == null || opNames.length < 3) {
                        if (opNames == null || opNames.length != 1) {
                            return;
                        }
                        log("opNames:" + opNames[0] + " len=" + opNames[0].length());
                        this.mNewSS.setOperatorName(null, null, null);
                        if (opNames[0].length() < 5 || opNames[0].equals(UNACTIVATED_MIN2_VALUE)) {
                            updateLocatedPlmn(null);
                            return;
                        } else {
                            updateLocatedPlmn(opNames[0]);
                            return;
                        }
                    }
                    String brandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                    if (brandOverride != null) {
                        log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + brandOverride);
                        this.mNewSS.setOperatorName(brandOverride, brandOverride, opNames[2]);
                    } else {
                        SpnOverride spnOverride = SpnOverride.getInstance();
                        String strOperatorLong = this.mCi.lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames[2], true);
                        if (strOperatorLong != null) {
                            log("EVENT_POLL_STATE_OPERATOR: OperatorLong use lookFromNetwork");
                        } else {
                            String strOperatorLong2 = spnOverride.lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames[2], true, this.mPhone.getContext());
                            if (strOperatorLong2 != null) {
                                logd("EVENT_POLL_STATE_OPERATOR: OperatorLong use lookupOperatorName");
                                strOperatorLong = this.mServiceStateExt.updateOpAlphaLongForHK(strOperatorLong2, opNames[2], this.mPhone.getPhoneId());
                            } else {
                                log("EVENT_POLL_STATE_OPERATOR: OperatorLong use value from ril");
                                strOperatorLong = opNames[0];
                            }
                        }
                        String strOperatorShort = this.mCi.lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames[2], VDBG);
                        if (strOperatorShort != null) {
                            log("EVENT_POLL_STATE_OPERATOR: OperatorShort use lookupOperatorNameFromNetwork");
                        } else {
                            strOperatorShort = spnOverride.lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames[2], VDBG, this.mPhone.getContext());
                            if (strOperatorShort != null) {
                                logd("EVENT_POLL_STATE_OPERATOR: OperatorShort use lookupOperatorName");
                            } else {
                                log("EVENT_POLL_STATE_OPERATOR: OperatorShort use value from ril");
                                strOperatorShort = opNames[1];
                            }
                        }
                        log("EVENT_POLL_STATE_OPERATOR: " + strOperatorLong + ", " + strOperatorShort);
                        this.mNewSS.setOperatorName(strOperatorLong, strOperatorShort, opNames[2]);
                    }
                    updateLocatedPlmn(opNames[2]);
                    return;
                }
                String[] opNames2 = (String[]) ar.result;
                if (opNames2 == null || opNames2.length < 3) {
                    if (DBG) {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                        return;
                    }
                    return;
                }
                if (opNames2[2] == null || opNames2[2].length() < 5 || "00000".equals(opNames2[2]) || "N/AN/A".equals(opNames2[2])) {
                    opNames2[2] = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, UsimPBMemInfo.STRING_NOT_SET);
                    if (DBG) {
                        log("RIL_REQUEST_OPERATOR.response[2], the numeric,  is bad. Using SystemProperties 'ro.cdma.home.operator.numeric'= " + opNames2[2]);
                    }
                }
                String numeric = opNames2[2];
                boolean plusCode = VDBG;
                if (numeric.startsWith("2134") && numeric.length() == 7) {
                    String tempStr = this.mPlusCodeUtils.checkMccBySidLtmOff(numeric);
                    if (!tempStr.equals("0")) {
                        opNames2[2] = tempStr + numeric.substring(4);
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: checkMccBySidLtmOff: numeric =" + tempStr + ", plmn =" + opNames2[2]);
                    }
                    plusCode = true;
                }
                if (!this.mIsSubscriptionFromRuim) {
                    if (plusCode) {
                        opNames2[1] = SpnOverride.getInstance().lookupOperatorName(this.mPhone.getSubId(), opNames2[2], VDBG, this.mPhone.getContext());
                    }
                    this.mNewSS.setOperatorName(null, opNames2[1], opNames2[2]);
                    return;
                }
                String brandOverride2 = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                if (brandOverride2 != null) {
                    log("EVENT_POLL_STATE_OPERATOR_CDMA: use brand=" + brandOverride2);
                    this.mNewSS.setOperatorName(brandOverride2, brandOverride2, opNames2[2]);
                    return;
                }
                SpnOverride spnOverride2 = SpnOverride.getInstance();
                String strOperatorLong3 = this.mCi.lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames2[2], true);
                if (strOperatorLong3 != null) {
                    log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong use lookupOperatorNameFromNetwork");
                } else {
                    strOperatorLong3 = spnOverride2.lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames2[2], true, this.mPhone.getContext());
                    if (strOperatorLong3 != null) {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong use lookupOperatorName");
                    } else {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorLong use value from ril");
                        strOperatorLong3 = opNames2[0];
                    }
                }
                String strOperatorShort2 = this.mCi.lookupOperatorNameFromNetwork(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames2[2], VDBG);
                if (strOperatorShort2 != null) {
                    log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort use lookupOperatorNameFromNetwork");
                } else {
                    strOperatorShort2 = spnOverride2.lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), opNames2[2], VDBG, this.mPhone.getContext());
                    if (strOperatorShort2 != null) {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort use lookupOperatorName");
                    } else {
                        log("EVENT_POLL_STATE_OPERATOR_CDMA: OperatorShort use value from ril");
                        strOperatorShort2 = opNames2[1];
                    }
                }
                log("EVENT_POLL_STATE_OPERATOR_CDMA: " + strOperatorLong3 + ", " + strOperatorShort2);
                this.mNewSS.setOperatorName(strOperatorLong3, strOperatorShort2, opNames2[2]);
                return;
            case 14:
                int[] ints = (int[]) ar.result;
                this.mNewSS.setIsManualSelection(ints[0] == 1 ? true : VDBG);
                if (ints[0] != 1 || this.mPhone.isManualNetSelAllowed()) {
                    return;
                }
                this.mPhone.setNetworkSelectionModeAutomatic(null);
                log(" Forcing Automatic Network Selection, manual selection is not allowed");
                return;
            default:
                loge("handlePollStateResultMessage: Unexpected RIL response received: " + what);
                return;
        }
    }

    private boolean isRoamIndForHomeSystem(String roamInd) {
        String[] homeRoamIndicators = this.mPhone.getContext().getResources().getStringArray(R.array.config_defaultImperceptibleKillingExemptionProcStates);
        if (homeRoamIndicators == null) {
            return VDBG;
        }
        for (String homeRoamInd : homeRoamIndicators) {
            if (homeRoamInd.equals(roamInd)) {
                return true;
            }
        }
        return VDBG;
    }

    protected void updateRoamingState() {
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean roaming = !this.mGsmRoaming ? this.mDataRoaming : true;
            log("set roaming=" + roaming + ",mGsmRoaming= " + this.mGsmRoaming + ",mDataRoaming= " + this.mDataRoaming);
            boolean isRoamingForSpecialSim = VDBG;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                String simType = PhoneFactory.getPhone(this.mPhone.getPhoneId()).getIccCard().getIccCardType();
                try {
                    if (this.mNewSS.getOperatorNumeric() != null && getSIMOperatorNumeric() != null && simType != null && !simType.equals(UsimPBMemInfo.STRING_NOT_SET) && simType.equals("CSIM")) {
                        if (this.mServiceStateExt.isRoamingForSpecialSIM(this.mNewSS.getOperatorNumeric(), getSIMOperatorNumeric())) {
                            isRoamingForSpecialSim = true;
                        }
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (!isRoamingForSpecialSim) {
                if (roaming && isSameNamedOperators(this.mNewSS) && !isOperatorConsideredRoamingMtk(this.mNewSS)) {
                    roaming = VDBG;
                }
                if (this.mPhone.isMccMncMarkedAsNonRoaming(this.mNewSS.getOperatorNumeric())) {
                    roaming = VDBG;
                } else if (this.mPhone.isMccMncMarkedAsRoaming(this.mNewSS.getOperatorNumeric())) {
                    roaming = true;
                }
            }
            this.mNewSS.setDataRoamingFromRegistration(roaming);
            CarrierConfigManager configLoader = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(this.mPhone.getSubId());
                    if (alwaysOnHomeNetwork(b)) {
                        log("updateRoamingState: carrier config override always on home network");
                        roaming = VDBG;
                    } else if (isNonRoamingInGsmNetwork(b, this.mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set non roaming:" + this.mNewSS.getOperatorNumeric());
                        roaming = VDBG;
                    } else if (isRoamingInGsmNetwork(b, this.mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set roaming:" + this.mNewSS.getOperatorNumeric());
                        roaming = true;
                    }
                } catch (Exception e2) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }
            this.mNewSS.setVoiceRoaming(roaming);
            this.mNewSS.setDataRoaming(roaming);
            return;
        }
        this.mNewSS.setDataRoamingFromRegistration(this.mNewSS.getDataRoaming());
        CarrierConfigManager configLoader2 = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (configLoader2 != null) {
            try {
                PersistableBundle b2 = configLoader2.getConfigForSubId(this.mPhone.getSubId());
                String systemId = Integer.toString(this.mNewSS.getSystemId());
                if (alwaysOnHomeNetwork(b2)) {
                    log("updateRoamingState: carrier config override always on home network");
                    setRoamingOff();
                } else if (isNonRoamingInGsmNetwork(b2, this.mNewSS.getOperatorNumeric()) || isNonRoamingInCdmaNetwork(b2, systemId)) {
                    log("updateRoamingState: carrier config override set non-roaming:" + this.mNewSS.getOperatorNumeric() + ", " + systemId);
                    setRoamingOff();
                } else if (isRoamingInGsmNetwork(b2, this.mNewSS.getOperatorNumeric()) || isRoamingInCdmaNetwork(b2, systemId)) {
                    log("updateRoamingState: carrier config override set roaming:" + this.mNewSS.getOperatorNumeric() + ", " + systemId);
                    setRoamingOn();
                }
            } catch (Exception e3) {
                loge("updateRoamingState: unable to access carrier config service");
            }
        } else {
            log("updateRoamingState: no carrier config service available");
        }
        if (!Build.IS_DEBUGGABLE || !SystemProperties.getBoolean(PROP_FORCE_ROAMING, VDBG)) {
            return;
        }
        this.mNewSS.setVoiceRoaming(true);
        this.mNewSS.setDataRoaming(true);
    }

    private void setRoamingOn() {
        this.mNewSS.setVoiceRoaming(true);
        this.mNewSS.setDataRoaming(true);
        this.mNewSS.setCdmaEriIconIndex(0);
        this.mNewSS.setCdmaEriIconMode(0);
    }

    private void setRoamingOff() {
        this.mNewSS.setVoiceRoaming(VDBG);
        this.mNewSS.setDataRoaming(VDBG);
        this.mNewSS.setCdmaEriIconIndex(1);
    }

    public void refreshSpnDisplay() {
        String numeric = this.mSS.getOperatorNumeric();
        if (numeric != null && !numeric.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            String newAlphaLong = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), numeric, true, this.mPhone.getContext());
            String newAlphaShort = SpnOverride.getInstance().lookupOperatorName(SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId()), numeric, VDBG, this.mPhone.getContext());
            if (this.mPhone.isPhoneTypeGsm() && newAlphaLong != null) {
                newAlphaLong = this.mServiceStateExt.updateOpAlphaLongForHK(newAlphaLong, numeric, this.mPhone.getPhoneId());
            }
            if (!TextUtils.equals(newAlphaLong, this.mSS.getOperatorAlphaLong())) {
                this.mNeedNotify = true;
            }
            log("refreshSpnDisplay set alpha to " + newAlphaLong + "," + newAlphaShort + "," + numeric + ", mNeedNotify=" + this.mNeedNotify);
            this.mSS.setOperatorName(newAlphaLong, newAlphaShort, numeric);
        }
        updateSpnDisplay();
    }

    protected void updateSpnDisplay() {
        if (this.mPhone.isPhoneTypeGsm()) {
            updateSpnDisplayGsm(VDBG);
        } else {
            updateSpnDisplayCdma(VDBG);
        }
    }

    protected void updateSpnDisplayGsm(boolean forceUpdate) {
        IccRecords r = this.mPhone.mIccRecords.get();
        SIMRecords simRecords = r != null ? (SIMRecords) r : null;
        int rule = simRecords != null ? simRecords.getDisplayRule(this.mSS.getOperatorNumeric()) : 2;
        String strNumPlmn = this.mSS.getOperatorNumeric();
        String spn = simRecords != null ? simRecords.getServiceProviderName() : UsimPBMemInfo.STRING_NOT_SET;
        String sEons = null;
        boolean showPlmn = VDBG;
        String plmn = null;
        String mSimOperatorNumeric = simRecords != null ? simRecords.getOperatorNumeric() : UsimPBMemInfo.STRING_NOT_SET;
        if (simRecords != null) {
            try {
                sEons = simRecords.getEonsIfExist(this.mSS.getOperatorNumeric(), ((GsmCellLocation) this.mCellLoc).getLac(), true);
            } catch (RuntimeException ex) {
                loge("Exception while getEonsIfExist. " + ex);
            }
        } else {
            sEons = null;
        }
        if (sEons != null) {
            plmn = sEons;
        } else if (strNumPlmn != null && strNumPlmn.equals(mSimOperatorNumeric)) {
            log("Home PLMN, get CPHS ons");
            plmn = simRecords != null ? simRecords.getSIMCPHSOns() : UsimPBMemInfo.STRING_NOT_SET;
        }
        if (TextUtils.isEmpty(plmn)) {
            log("No matched EONS and No CPHS ONS");
            plmn = this.mSS.getOperatorAlphaLong();
            if (TextUtils.isEmpty(plmn) || plmn.equals(this.mSS.getOperatorNumeric())) {
                plmn = this.mSS.getOperatorAlphaShort();
            }
        }
        String realPlmn = plmn;
        if (this.mSS.getVoiceRegState() != 0 && this.mSS.getDataRegState() != 0) {
            showPlmn = true;
            plmn = Resources.getSystem().getText(R.string.config_help_url_action_disabled_by_advanced_protection).toString();
        }
        log("updateSpnDisplay mVoiceCapable=" + this.mVoiceCapable + " mEmergencyOnly=" + this.mEmergencyOnly + " mCi.getRadioState().isOn()=" + this.mCi.getRadioState().isOn() + " getVoiceRegState()=" + this.mSS.getVoiceRegState() + " getDataRegState()" + this.mSS.getDataRegState());
        if (this.mVoiceCapable && this.mEmergencyOnly && this.mCi.getRadioState().isOn() && this.mSS.getDataRegState() != 0) {
            log("updateSpnDisplay show mEmergencyOnly");
            showPlmn = true;
            plmn = Resources.getSystem().getText(R.string.config_notificationHandlerPackage).toString();
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.needBlankDisplay(this.mSS.getVoiceRejectCause())) {
                        log("Do NOT show emergency call only display");
                        plmn = UsimPBMemInfo.STRING_NOT_SET;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
        int imeiAbnormal = this.mPhone.isDeviceIdAbnormal();
        if (imeiAbnormal == 1) {
            if (this.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                plmn = Resources.getSystem().getText(134545435).toString();
            }
        } else if (imeiAbnormal == 2) {
            plmn = Resources.getSystem().getText(134545436).toString();
        } else if (imeiAbnormal == 0) {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    plmn = this.mServiceStateExt.onUpdateSpnDisplay(plmn, this.mSS, this.mPhone.getPhoneId());
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            }
            if (this.mSS.getVoiceRegState() != 0 && this.mSS.getDataRegState() == 0) {
                if (plmn != null) {
                    plmn = plmn + "(" + Resources.getSystem().getText(134545555).toString() + ")";
                } else {
                    log("PLMN name is null when CS not registered and PS registered");
                }
            }
        }
        if (this.mIsImeiLock) {
            plmn = Resources.getSystem().getText(134545510).toString();
        }
        if (this.mSS.getVoiceRegState() == 0 || this.mSS.getDataRegState() == 0) {
            showPlmn = (TextUtils.isEmpty(plmn) || (rule & 2) != 2) ? VDBG : true;
        }
        String dataSpn = spn;
        boolean showSpn = (TextUtils.isEmpty(spn) || (rule & 1) != 1) ? VDBG : true;
        if (!TextUtils.isEmpty(spn) && this.mPhone.getImsPhone() != null && ((ImsPhone) this.mPhone.getImsPhone()).isWifiCallingEnabled()) {
            String[] wfcSpnFormats = this.mPhone.getContext().getResources().getStringArray(R.array.config_displayCutoutSideOverrideArray);
            int voiceIdx = 0;
            int dataIdx = 0;
            CarrierConfigManager configLoader = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (configLoader != null) {
                try {
                    PersistableBundle b = configLoader.getConfigForSubId(this.mPhone.getSubId());
                    if (b != null) {
                        voiceIdx = b.getInt("wfc_spn_format_idx_int");
                        dataIdx = b.getInt("wfc_data_spn_format_idx_int");
                    }
                } catch (Exception e3) {
                    loge("updateSpnDisplay: carrier config error: " + e3);
                }
            }
            String formatVoice = wfcSpnFormats[voiceIdx];
            String formatData = wfcSpnFormats[dataIdx];
            String originalSpn = spn.trim();
            spn = String.format(formatVoice, originalSpn);
            dataSpn = String.format(formatData, originalSpn);
            showSpn = true;
            showPlmn = VDBG;
        } else if (this.mSS.getVoiceRegState() == 3 || ((showPlmn && TextUtils.equals(spn, plmn)) || (this.mSS.getVoiceRegState() != 0 && this.mSS.getDataRegState() != 0))) {
            spn = null;
            showSpn = VDBG;
        }
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                if (this.mServiceStateExt.needSpnRuleShowPlmnOnly() && !TextUtils.isEmpty(plmn)) {
                    log("origin showSpn:" + showSpn + " showPlmn:" + showPlmn + " rule:" + rule);
                    showSpn = VDBG;
                    showPlmn = true;
                    rule = 2;
                }
            } catch (RuntimeException e4) {
                e4.printStackTrace();
            }
        }
        try {
            plmn = this.mServiceStateExt.onUpdateSpnDisplayForIms(plmn, this.mSS, ((GsmCellLocation) this.mCellLoc).getLac(), this.mPhone.getPhoneId(), simRecords);
        } catch (RuntimeException e5) {
            e5.printStackTrace();
        }
        int subId = -1;
        int[] subIds = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }
        if (this.mSubId != subId || showPlmn != this.mCurShowPlmn || showSpn != this.mCurShowSpn || !TextUtils.equals(spn, this.mCurSpn) || !TextUtils.equals(dataSpn, this.mCurDataSpn) || !TextUtils.equals(plmn, this.mCurPlmn) || forceUpdate) {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (!this.mServiceStateExt.allowSpnDisplayed()) {
                        log("For CT test case don't show SPN.");
                        if (rule == 3) {
                            showSpn = VDBG;
                            spn = null;
                        }
                    }
                } catch (RuntimeException e6) {
                    e6.printStackTrace();
                }
            }
            if (DBG) {
                log(String.format("updateSpnDisplay: changed sending intent rule=" + rule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'", Boolean.valueOf(showPlmn), plmn, Boolean.valueOf(showSpn), spn, dataSpn, Integer.valueOf(subId)));
            }
            Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            intent.putExtra("showSpn", showSpn);
            intent.putExtra(Telephony.Carriers.SPN, spn);
            intent.putExtra("spnData", dataSpn);
            intent.putExtra("showPlmn", showPlmn);
            intent.putExtra(Telephony.CellBroadcasts.PLMN, plmn);
            intent.putExtra("hnbName", this.mHhbName);
            intent.putExtra("csgId", this.mCsgId);
            intent.putExtra("domain", this.mFemtocellDomain);
            intent.putExtra("femtocell", this.mIsFemtocell);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            int phoneId = this.mPhone.getPhoneId();
            if (SystemProperties.get("ro.mtk_femto_cell_support").equals("1")) {
                if (this.mHhbName != null || this.mCsgId == null) {
                    if (this.mHhbName != null) {
                        plmn = (plmn + " - ") + this.mHhbName;
                    }
                } else if (SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    plmn = (plmn + " - ") + this.mCsgId;
                } else {
                    try {
                        if (this.mServiceStateExt.needToShowCsgId()) {
                            plmn = (plmn + " - ") + this.mCsgId;
                        }
                    } catch (RuntimeException e7) {
                        e7.printStackTrace();
                    }
                }
            }
            boolean setResult = this.mSubscriptionController.setPlmnSpn(phoneId, showPlmn, plmn, showSpn, spn);
            if (!setResult) {
                this.mSpnUpdatePending = true;
            }
            log("showSpn:" + showSpn + " spn:" + spn + " showPlmn:" + showPlmn + " plmn:" + plmn + " rule:" + rule + " setResult:" + setResult + " phoneId:" + phoneId);
        }
        String operatorLong = this.mSS.getOperatorAlphaLong();
        if (!showSpn || showPlmn || spn == null) {
            if (operatorLong == null || !operatorLong.equals(realPlmn)) {
                this.mSS.setOperatorAlphaLong(realPlmn);
                this.mNeedNotify = true;
            }
            log("updateAllOpertorInfo with realPlmn:" + realPlmn + ", mNeedNotify=" + this.mNeedNotify);
            updateOperatorAlpha(realPlmn);
        } else {
            if (operatorLong == null || !operatorLong.equals(spn)) {
                this.mSS.setOperatorAlphaLong(spn);
                this.mNeedNotify = true;
            }
            log("updateAllOpertorInfo with spn:" + spn + ", mNeedNotify=" + this.mNeedNotify);
            updateOperatorAlpha(spn);
        }
        this.mSubId = subId;
        this.mCurShowSpn = showSpn;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = spn;
        this.mCurDataSpn = dataSpn;
        this.mCurPlmn = plmn;
    }

    private void updateSpnDisplayCdma(boolean forceUpdate) {
        String plmn = this.mSS.getOperatorAlphaLong();
        boolean showPlmn = plmn != null ? true : VDBG;
        int subId = -1;
        int[] subIds = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
        if (subIds != null && subIds.length > 0) {
            subId = subIds[0];
        }
        if ((plmn == null || plmn.equals(UsimPBMemInfo.STRING_NOT_SET)) && ((plmn = this.mSS.getOperatorAlphaLong()) == null || plmn.equals(this.mSS.getOperatorNumeric()))) {
            plmn = this.mSS.getOperatorAlphaShort();
        }
        if (plmn != null) {
            showPlmn = true;
            if (plmn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                plmn = null;
            }
        }
        log("[CDMA]updateSpnDisplay: getOperatorAlphaLong=" + this.mSS.getOperatorAlphaLong() + ", getOperatorAlphaShort=" + this.mSS.getOperatorAlphaShort() + ", plmn=" + plmn + ", forceUpdate=" + forceUpdate);
        if (this.mSS.getState() != 0 && this.mSS.getDataRegState() != 0) {
            log("[CDMA]updateSpnDisplay: Do not display SPN before get normal service");
            showPlmn = true;
            plmn = Resources.getSystem().getText(R.string.config_help_url_action_disabled_by_advanced_protection).toString();
        }
        if (this.mEmergencyOnly && this.mCi.getRadioState().isOn()) {
            log("[CDMA]updateSpnDisplay: phone show emergency call only, mEmergencyOnly = true");
            showPlmn = true;
            plmn = Resources.getSystem().getText(R.string.config_notificationHandlerPackage).toString();
        }
        String spn = UsimPBMemInfo.STRING_NOT_SET;
        boolean showSpn = VDBG;
        if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
            try {
                if (this.mServiceStateExt.allowSpnDisplayed()) {
                    IccRecords r = this.mPhone.mIccRecords.get();
                    int rule = r != null ? r.getDisplayRule(this.mSS.getOperatorNumeric()) : 2;
                    spn = r != null ? r.getServiceProviderName() : UsimPBMemInfo.STRING_NOT_SET;
                    showSpn = (TextUtils.isEmpty(spn) || (rule & 1) != 1 || this.mSS.getVoiceRegState() == 3 || this.mSS.getRoaming()) ? VDBG : true;
                    log("[CDMA]updateSpnDisplay: rule=" + rule + ", spn=" + spn + ", showSpn=" + showSpn);
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (this.mSubId != subId || showPlmn != this.mCurShowPlmn || showSpn != this.mCurShowSpn || !TextUtils.equals(spn, this.mCurSpn) || !TextUtils.equals(plmn, this.mCurPlmn) || forceUpdate) {
            showPlmn = plmn != null ? true : VDBG;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.allowSpnDisplayed()) {
                        if (this.mSS.getVoiceRegState() == 3 || this.mSS.getVoiceRegState() == 1 || this.mSS.getRoaming() || spn == null) {
                            showSpn = VDBG;
                            showPlmn = true;
                        } else if (!spn.equals(UsimPBMemInfo.STRING_NOT_SET)) {
                            showSpn = true;
                            showPlmn = VDBG;
                        }
                    }
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            }
            if (DBG) {
                log(String.format("[CDMA]updateSpnDisplay: changed sending intent subId='%d' showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'", Integer.valueOf(subId), Boolean.valueOf(showPlmn), plmn, Boolean.valueOf(showSpn), spn));
            }
            Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            intent.putExtra("showSpn", showSpn);
            intent.putExtra(Telephony.Carriers.SPN, spn);
            intent.putExtra("showPlmn", showPlmn);
            intent.putExtra(Telephony.CellBroadcasts.PLMN, plmn);
            intent.putExtra("hnbName", (String) null);
            intent.putExtra("csgId", (String) null);
            intent.putExtra("domain", 0);
            intent.putExtra("femtocell", this.mIsFemtocell);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            boolean setResult = this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), showPlmn, plmn, showSpn, spn);
            if (!setResult) {
                this.mSpnUpdatePending = true;
            }
            log("[CDMA]updateSpnDisplay: subId=" + subId + ", showPlmn=" + showPlmn + ", plmn=" + plmn + ", showSpn=" + showSpn + ", spn=" + spn + ", setResult=" + setResult + ", mSpnUpdatePending=" + this.mSpnUpdatePending);
        }
        this.mSubId = subId;
        this.mCurShowSpn = showSpn;
        this.mCurShowPlmn = showPlmn;
        this.mCurSpn = spn;
        this.mCurPlmn = plmn;
    }

    protected void setPowerStateToDesired() {
        if (DBG) {
            log("mDeviceShuttingDown=" + this.mDeviceShuttingDown + ", mDesiredPowerState=" + this.mDesiredPowerState + ", getRadioState=" + this.mCi.getRadioState() + ", mPowerOffDelayNeed=" + this.mPowerOffDelayNeed + ", mAlarmSwitch=" + this.mAlarmSwitch);
        }
        if (this.mPhone.isPhoneTypeGsm() && this.mAlarmSwitch) {
            if (DBG) {
                log("mAlarmSwitch == true");
            }
            AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
            am.cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = VDBG;
        }
        if (this.mDesiredPowerState && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            if (this.mPhone.isPhoneTypeGsm()) {
                setDeviceRatMode(this.mPhone.getPhoneId());
            }
            RadioManager.getInstance();
            RadioManager.sendRequestBeforeSetRadioPower(true, this.mPhone.getPhoneId());
            this.mCi.setRadioPower(true, null);
            return;
        }
        if (this.mDesiredPowerState || !this.mCi.getRadioState().isOn()) {
            if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
                this.mCi.requestShutdown(null);
                return;
            }
            return;
        }
        if (!this.mPhone.isPhoneTypeGsm() || !this.mPowerOffDelayNeed) {
            DcTracker dcTracker = this.mPhone.mDcTracker;
            powerOffRadioSafely(dcTracker);
            return;
        }
        if (!this.mImsRegistrationOnOff || this.mAlarmSwitch) {
            DcTracker dcTracker2 = this.mPhone.mDcTracker;
            powerOffRadioSafely(dcTracker2);
            return;
        }
        if (DBG) {
            log("mImsRegistrationOnOff == true");
        }
        Context context = this.mPhone.getContext();
        AlarmManager am2 = (AlarmManager) context.getSystemService("alarm");
        Intent intent = new Intent(ACTION_RADIO_OFF);
        this.mRadioOffIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        this.mAlarmSwitch = true;
        if (DBG) {
            log("Alarm setting");
        }
        am2.set(2, SystemClock.elapsedRealtime() + 3000, this.mRadioOffIntent);
    }

    protected void onUpdateIccAvailability() {
        if (this.mUiccController == null) {
            return;
        }
        UiccCardApplication newUiccApplication = getUiccCardApplication();
        if ((this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) && newUiccApplication != null) {
            IccCardApplicationStatus.AppState appState = newUiccApplication.getState();
            if ((appState == IccCardApplicationStatus.AppState.APPSTATE_PIN || appState == IccCardApplicationStatus.AppState.APPSTATE_PUK) && this.mNetworkExsit) {
                this.mEmergencyOnly = true;
            } else {
                this.mEmergencyOnly = VDBG;
            }
            log("[CDMA]onUpdateIccAvailability, appstate=" + appState + ", mNetworkExsit=" + this.mNetworkExsit + ", mEmergencyOnly=" + this.mEmergencyOnly);
        }
        if (this.mUiccApplcation != newUiccApplication) {
            if (this.mUiccApplcation != null) {
                log("Removing stale icc objects.");
                this.mUiccApplcation.unregisterForReady(this);
                if (this.mIccRecords != null) {
                    this.mIccRecords.unregisterForRecordsLoaded(this);
                }
                this.mIccRecords = null;
                this.mUiccApplcation = null;
            }
            if (newUiccApplication != null) {
                log("New card found");
                this.mUiccApplcation = newUiccApplication;
                this.mIccRecords = this.mUiccApplcation.getIccRecords();
                if (this.mPhone.isPhoneTypeGsm()) {
                    this.mUiccApplcation.registerForReady(this, 17, null);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.registerForRecordsLoaded(this, 16, null);
                        return;
                    }
                    return;
                }
                if (this.mIsSubscriptionFromRuim) {
                    this.mUiccApplcation.registerForReady(this, 26, null);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.registerForRecordsLoaded(this, 27, null);
                    }
                }
            }
        }
    }

    protected void logd(String s) {
        if (!mEngLoad && mLogLv <= 0) {
            return;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.d(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + s);
        } else if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.d(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.d(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + s);
        }
    }

    protected void log(String s) {
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.d(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + s);
        } else if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.d(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.d(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + s);
        }
    }

    protected void loge(String s) {
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.e(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + s);
        } else if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.e(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + s);
        } else {
            Rlog.e(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + s);
        }
    }

    public int getCurrentDataConnectionState() {
        return this.mSS.getDataRegState();
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean isAllowed = VDBG;
            if (this.mSS.isVoiceRadioTechnologyHigher(3) || this.mSS.getRilVoiceRadioTechnology() == 3) {
                isAllowed = true;
            }
            if (DBG) {
                log("isConcurrentVoiceAndDataAllowed(): " + isAllowed);
            }
            return isAllowed;
        }
        if (this.mPhone.isPhoneTypeCdma()) {
            return VDBG;
        }
        if ((SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1 && this.mSS.getRilDataRadioTechnology() == 14) || this.mSS.getCssIndicator() == 1) {
            return true;
        }
        return VDBG;
    }

    public void setImsRegistrationState(boolean registered) {
        log("ImsRegistrationState - registered : " + registered);
        if (this.mImsRegistrationOnOff && !registered && this.mAlarmSwitch) {
            this.mImsRegistrationOnOff = registered;
            Context context = this.mPhone.getContext();
            AlarmManager am = (AlarmManager) context.getSystemService("alarm");
            am.cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = VDBG;
            sendMessage(obtainMessage(EVENT_CHANGE_IMS_STATE));
            return;
        }
        this.mImsRegistrationOnOff = registered;
    }

    public void onImsCapabilityChanged() {
        if (!this.mPhone.isPhoneTypeGsm()) {
            return;
        }
        sendMessage(obtainMessage(EVENT_IMS_CAPABILITY_CHANGED));
    }

    private void onNetworkStateChangeResult(AsyncResult ar) {
        int lac = -1;
        int cid = -1;
        int Act = -1;
        int cause = -1;
        if (ar.exception != null || ar.result == null) {
            loge("onNetworkStateChangeResult exception");
            return;
        }
        String[] info = (String[]) ar.result;
        if (info.length > 0) {
            int state = Integer.parseInt(info[0]);
            if (info[1] != null && info[1].length() > 0) {
                lac = Integer.parseInt(info[1], 16);
            }
            if (info[2] != null && info[2].length() > 0) {
                if (info[2].equals("FFFFFFFF") || info[2].equals("ffffffff")) {
                    log("Invalid cid:" + info[2]);
                    info[2] = "0000ffff";
                }
                cid = Integer.parseInt(info[2], 16);
            }
            if (info[3] != null && info[3].length() > 0) {
                Act = Integer.parseInt(info[3]);
            }
            if (info[4] != null && info[4].length() > 0) {
                cause = Integer.parseInt(info[4]);
            }
            log("onNetworkStateChangeResult state:" + state + " lac:" + lac + " cid:" + cid + " Act:" + Act + " cause:" + cause);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1") && SystemProperties.get("ro.mtk_md_world_mode_support").equals("0")) {
                try {
                    if (this.mServiceStateExt.needIgnoredState(this.mSS.getVoiceRegState(), state, cause)) {
                        log("onNetworkStateChangeResult isCsInvalidCard:" + this.isCsInvalidCard);
                        if (!this.isCsInvalidCard) {
                            if (!this.dontUpdateNetworkStateFlag) {
                                broadcastHideNetworkState(Telephony.BaseMmsColumns.START, 1);
                            }
                            this.dontUpdateNetworkStateFlag = true;
                            return;
                        }
                        return;
                    }
                    if (this.dontUpdateNetworkStateFlag) {
                        broadcastHideNetworkState("stop", 1);
                    }
                    this.dontUpdateNetworkStateFlag = VDBG;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            if (lac != -1 && cid != -1 && regCodeToServiceState(state) == 1) {
                if (lac == 65534 || cid == 268435455) {
                    log("unknown lac:" + lac + " or cid:" + cid);
                } else {
                    log("mNewCellLoc Updated, lac:" + lac + " and cid:" + cid);
                    ((GsmCellLocation) this.mNewCellLoc).setLacAndCid(lac, cid);
                }
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.needRejectCauseNotification(cause)) {
                        setRejectCauseNotification(cause);
                        return;
                    }
                    return;
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                    return;
                }
            }
            return;
        }
        logd("onNetworkStateChangeResult length zero");
    }

    public void setEverIVSR(boolean value) {
        log("setEverIVSR:" + value);
        this.mEverIVSR = value;
        if (!value) {
            return;
        }
        Intent intent = new Intent("mediatek.intent.action.IVSR_NOTIFY");
        intent.putExtra("action", Telephony.BaseMmsColumns.START);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        if (TelephonyManager.getDefault().getPhoneCount() == 1) {
            intent.addFlags(536870912);
        }
        log("broadcast ACTION_IVSR_NOTIFY intent");
        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public String getLocatedPlmn() {
        return this.mLocatedPlmn;
    }

    private void updateLocatedPlmn(String plmn) {
        logd("updateLocatedPlmn(),previous plmn= " + this.mLocatedPlmn + " ,update to: " + plmn);
        if ((this.mLocatedPlmn == null && plmn != null) || ((this.mLocatedPlmn != null && plmn == null) || (this.mLocatedPlmn != null && plmn != null && !this.mLocatedPlmn.equals(plmn)))) {
            Intent intent = new Intent("mediatek.intent.action.LOCATED_PLMN_CHANGED");
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            intent.putExtra(Telephony.CellBroadcasts.PLMN, plmn);
            if (plmn != null) {
                try {
                    int mcc = Integer.parseInt(plmn.substring(0, 3));
                    intent.putExtra("iso", MccTable.countryCodeForMcc(mcc));
                } catch (NumberFormatException ex) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex);
                    intent.putExtra("iso", UsimPBMemInfo.STRING_NOT_SET);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + ex2);
                    intent.putExtra("iso", UsimPBMemInfo.STRING_NOT_SET);
                }
                if (SystemProperties.get(PROPERTY_AUTO_RAT_SWITCH).equals("0")) {
                    loge("updateLocatedPlmn: framework auto RAT switch disabled");
                } else {
                    this.mLocatedPlmn = plmn;
                    setDeviceRatMode(this.mPhone.getPhoneId());
                }
            } else {
                intent.putExtra("iso", UsimPBMemInfo.STRING_NOT_SET);
            }
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mLocatedPlmn = plmn;
    }

    private void onFemtoCellInfoResult(AsyncResult ar) {
        int isCsgCell = 0;
        if (ar.exception != null || ar.result == null) {
            loge("onFemtoCellInfo exception");
            return;
        }
        String[] info = (String[]) ar.result;
        if (info.length <= 0) {
            return;
        }
        if (info[0] != null && info[0].length() > 0) {
            this.mFemtocellDomain = Integer.parseInt(info[0]);
            log("onFemtoCellInfo: mFemtocellDomain set to " + this.mFemtocellDomain);
        }
        if (info[5] != null && info[5].length() > 0) {
            isCsgCell = Integer.parseInt(info[5]);
        }
        this.mIsFemtocell = isCsgCell;
        log("onFemtoCellInfo: domain= " + this.mFemtocellDomain + ",isCsgCell= " + isCsgCell);
        if (isCsgCell == 1) {
            if (info[6] != null && info[6].length() > 0) {
                this.mCsgId = info[6];
                log("onFemtoCellInfo: mCsgId set to " + this.mCsgId);
            }
            if (info[8] != null && info[8].length() > 0) {
                this.mHhbName = new String(IccUtils.hexStringToBytes(info[8]));
                log("onFemtoCellInfo: mHhbName set from " + info[8] + " to " + this.mHhbName);
            } else {
                this.mHhbName = null;
                log("onFemtoCellInfo: mHhbName is not available ,set to null");
            }
        } else {
            this.mCsgId = null;
            this.mHhbName = null;
            log("onFemtoCellInfo: csgId and hnbName are cleared");
        }
        if (isCsgCell != 2 && info[1] != null && info[1].length() > 0 && info[9] != null && info[0].length() > 0) {
            int state = Integer.parseInt(info[1]);
            int cause = Integer.parseInt(info[9]);
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.needIgnoreFemtocellUpdate(state, cause)) {
                        log("needIgnoreFemtocellUpdate due to state= " + state + ",cause= " + cause);
                        return;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
        Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        if (TelephonyManager.getDefault().getPhoneCount() == 1) {
            intent.addFlags(536870912);
        }
        intent.putExtra("showSpn", this.mCurShowSpn);
        intent.putExtra(Telephony.Carriers.SPN, this.mCurSpn);
        intent.putExtra("showPlmn", this.mCurShowPlmn);
        intent.putExtra(Telephony.CellBroadcasts.PLMN, this.mCurPlmn);
        intent.putExtra("hnbName", this.mHhbName);
        intent.putExtra("csgId", this.mCsgId);
        intent.putExtra("domain", this.mFemtocellDomain);
        intent.putExtra("femtocell", this.mIsFemtocell);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        int phoneId = this.mPhone.getPhoneId();
        String plmn = this.mCurPlmn;
        if (this.mHhbName == null && this.mCsgId != null) {
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    if (this.mServiceStateExt.needToShowCsgId()) {
                        plmn = (plmn + " - ") + this.mCsgId;
                    }
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            } else {
                plmn = (plmn + " - ") + this.mCsgId;
            }
        } else if (this.mHhbName != null) {
            plmn = (plmn + " - ") + this.mHhbName;
        }
        boolean setResult = this.mSubscriptionController.setPlmnSpn(phoneId, this.mCurShowPlmn, plmn, this.mCurShowSpn, this.mCurSpn);
        if (setResult) {
            return;
        }
        this.mSpnUpdatePending = true;
    }

    private void broadcastHideNetworkState(String action, int state) {
        if (DBG) {
            log("broadcastHideNetworkUpdate action=" + action + " state=" + state);
        }
        Intent intent = new Intent("mediatek.intent.action.ACTION_HIDE_NETWORK_STATE");
        if (TelephonyManager.getDefault().getPhoneCount() == 1) {
            intent.addFlags(536870912);
        }
        intent.putExtra("action", action);
        intent.putExtra("state", state);
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void onInvalidSimInfoReceived(AsyncResult ar) {
        String[] InvalidSimInfo = (String[]) ar.result;
        String plmn = InvalidSimInfo[0];
        int cs_invalid = Integer.parseInt(InvalidSimInfo[1]);
        int ps_invalid = Integer.parseInt(InvalidSimInfo[2]);
        int cause = Integer.parseInt(InvalidSimInfo[3]);
        int testMode = SystemProperties.getInt("gsm.gcf.testmode", 0);
        log("onInvalidSimInfoReceived testMode:" + testMode + " cause:" + cause + " cs_invalid:" + cs_invalid + " ps_invalid:" + ps_invalid + " plmn:" + plmn + " mEverIVSR:" + this.mEverIVSR);
        if (testMode != 0) {
            log("InvalidSimInfo received during test mode: " + testMode);
            return;
        }
        if (this.mServiceStateExt.isNeedDisableIVSR()) {
            log("Disable IVSR");
            return;
        }
        if (cs_invalid == 1) {
            this.isCsInvalidCard = true;
        }
        if (this.mVoiceCapable && cs_invalid == 1 && this.mLastRegisteredPLMN != null && plmn.equals(this.mLastRegisteredPLMN)) {
            log("InvalidSimInfo reset SIM due to CS invalid");
            setEverIVSR(true);
            this.mLastRegisteredPLMN = null;
            this.mLastPSRegisteredPLMN = null;
            this.mCi.setSimPower(2, null);
            return;
        }
        if (ps_invalid == 1 && isAllowRecoveryOnIvsr(ar) && this.mLastPSRegisteredPLMN != null && plmn.equals(this.mLastPSRegisteredPLMN)) {
            log("InvalidSimInfo reset SIM due to PS invalid ");
            setEverIVSR(true);
            this.mLastRegisteredPLMN = null;
            this.mLastPSRegisteredPLMN = null;
            this.mCi.setSimPower(2, null);
        }
    }

    private void onModulationInfoReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
            loge("onModulationInfoReceived exception");
            return;
        }
        int[] info = (int[]) ar.result;
        int modulation = info[0];
        log("[onModulationInfoReceived] modulation:" + modulation);
        Intent intent = new Intent("mediatek.intent.action.ACTION_NOTIFY_MODULATION_INFO");
        intent.addFlags(536870912);
        intent.putExtra("modulation_info", modulation);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isAllowRecoveryOnIvsr(AsyncResult ar) {
        if (this.mPhone.isInCall()) {
            log("[isAllowRecoveryOnIvsr] isInCall()=true");
            Message msg = obtainMessage();
            msg.what = 101;
            msg.obj = ar;
            sendMessageDelayed(msg, 10000L);
            return VDBG;
        }
        log("isAllowRecoveryOnIvsr() return true");
        return true;
    }

    private void setRejectCauseNotification(int cause) {
        if (DBG) {
            log("setRejectCauseNotification: create notification " + cause);
        }
        Context context = this.mPhone.getContext();
        this.mNotificationBuilder = new Notification.Builder(context);
        this.mNotificationBuilder.setWhen(System.currentTimeMillis());
        this.mNotificationBuilder.setAutoCancel(true);
        this.mNotificationBuilder.setSmallIcon(R.drawable.stat_sys_warning);
        Intent intent = new Intent();
        this.mNotificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 134217728));
        CharSequence details = UsimPBMemInfo.STRING_NOT_SET;
        CharSequence title = context.getText(134545511);
        switch (cause) {
            case 2:
                details = context.getText(134545512);
                break;
            case 3:
                details = context.getText(134545513);
                break;
            case 5:
                details = context.getText(134545520);
                break;
            case 6:
                details = context.getText(134545521);
                break;
            case 13:
                details = context.getText(134545525);
                break;
        }
        if (DBG) {
            log("setRejectCauseNotification: put notification " + title + " / " + details);
        }
        this.mNotificationBuilder.setContentTitle(title);
        this.mNotificationBuilder.setContentText(details);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotification = this.mNotificationBuilder.build();
        notificationManager.notify(REJECT_NOTIFICATION, this.mNotification);
    }

    private void setSpecialCardTypeNotification(String iccCardType, int titleType, int detailType) {
        if (DBG) {
            log("setSpecialCardTypeNotification: create notification for " + iccCardType);
        }
        Context context = this.mPhone.getContext();
        this.mNotificationBuilder = new Notification.Builder(context);
        this.mNotificationBuilder.setWhen(System.currentTimeMillis());
        this.mNotificationBuilder.setAutoCancel(true);
        this.mNotificationBuilder.setSmallIcon(R.drawable.stat_sys_warning);
        Intent intent = new Intent();
        this.mNotificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 134217728));
        CharSequence title = UsimPBMemInfo.STRING_NOT_SET;
        switch (titleType) {
            case 0:
                title = context.getText(134545559);
                break;
        }
        CharSequence details = UsimPBMemInfo.STRING_NOT_SET;
        switch (detailType) {
            case 0:
                details = context.getText(134545560);
                break;
        }
        if (DBG) {
            log("setSpecialCardTypeNotification: put notification " + title + " / " + details);
        }
        this.mNotificationBuilder.setContentTitle(title);
        this.mNotificationBuilder.setContentText(details);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotification = this.mNotificationBuilder.build();
        notificationManager.notify(SPECIAL_CARD_TYPE_NOTIFICATION, this.mNotification);
    }

    private int getDstForMcc(int mcc, long when) {
        String tzId;
        if (mcc == 0 || (tzId = MccTable.defaultTimeZoneForMcc(mcc)) == null) {
            return 0;
        }
        TimeZone timeZone = TimeZone.getTimeZone(tzId);
        Date date = new Date(when);
        boolean isInDaylightTime = timeZone.inDaylightTime(date);
        if (!isInDaylightTime) {
            return 0;
        }
        log("[NITZ] getDstForMcc: dst=1");
        return 1;
    }

    private int getMobileCountryCode() {
        String operatorNumeric = this.mSS.getOperatorNumeric();
        if (operatorNumeric == null) {
            return 0;
        }
        try {
            int mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
            return mcc;
        } catch (NumberFormatException ex) {
            loge("countryCodeForMcc error" + ex);
            return 0;
        } catch (StringIndexOutOfBoundsException ex2) {
            loge("countryCodeForMcc error" + ex2);
            return 0;
        }
    }

    private TimeZone getTimeZonesWithCapitalCity(String iso) {
        if (this.mZoneOffset == 0 && !this.mZoneDst) {
            for (int i = 0; i < this.mTimeZoneIdOfCapitalCity.length; i++) {
                if (iso.equals(this.mTimeZoneIdOfCapitalCity[i][0])) {
                    TimeZone tz = TimeZone.getTimeZone(this.mTimeZoneIdOfCapitalCity[i][1]);
                    log("uses TimeZone of Capital City:" + this.mTimeZoneIdOfCapitalCity[i][1]);
                    return tz;
                }
            }
            return null;
        }
        log("don't udpate with capital city, cause we have received nitz");
        return null;
    }

    private String getTimeZonesByMcc(String mcc) {
        for (int i = 0; i < this.mTimeZoneIdByMcc.length; i++) {
            if (mcc.equals(this.mTimeZoneIdByMcc[i][0])) {
                String tz = this.mTimeZoneIdByMcc[i][1];
                log("uses Timezone of GsmSST by mcc: " + this.mTimeZoneIdByMcc[i][1]);
                return tz;
            }
        }
        return null;
    }

    protected void fixTimeZone() {
        TimeZone zone = null;
        String iso = UsimPBMemInfo.STRING_NOT_SET;
        String operatorNumeric = this.mSS.getOperatorNumeric();
        if (operatorNumeric == null || operatorNumeric.equals(UsimPBMemInfo.STRING_NOT_SET) || !isNumeric(operatorNumeric)) {
            log("fixTimeZone but not registered and operatorNumeric is null or invalid value");
            return;
        }
        String mcc = operatorNumeric.substring(0, 3);
        try {
            iso = MccTable.countryCodeForMcc(Integer.parseInt(mcc));
        } catch (NumberFormatException ex) {
            loge("fixTimeZone countryCodeForMcc error" + ex);
        }
        if (!mcc.equals(INVALID_MCC) && !TextUtils.isEmpty(iso) && getAutoTimeZone()) {
            boolean testOneUniqueOffsetPath = (SystemProperties.getBoolean("telephony.test.ignore.nitz", VDBG) && (SystemClock.uptimeMillis() & 1) == 0) ? true : VDBG;
            ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(iso);
            if (uniqueZones.size() == 1 || testOneUniqueOffsetPath) {
                zone = uniqueZones.get(0);
                if (DBG) {
                    log("fixTimeZone: no nitz but one TZ for iso-cc=" + iso + " with zone.getID=" + zone.getID() + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                }
                setAndBroadcastNetworkSetTimeZone(zone.getID());
            } else if (uniqueZones.size() > 1) {
                log("uniqueZones.size=" + uniqueZones.size());
                zone = getTimeZonesWithCapitalCity(iso);
                if (zone != null) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
            } else if (DBG) {
                log("fixTimeZone: there are " + uniqueZones.size() + " unique offsets for iso-cc='" + iso + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath + "', do nothing");
            }
        }
        if (zone == null) {
            log("fixTimeZone: zone == null");
            return;
        }
        log("fixTimeZone: zone != null zone.getID=" + zone.getID());
        if (getAutoTimeZone()) {
            setAndBroadcastNetworkSetTimeZone(zone.getID());
        }
        saveNitzTimeZone(zone.getID());
    }

    public boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException eNFE) {
            log("isNumeric:" + eNFE.toString());
            return VDBG;
        } catch (Exception e) {
            log("isNumeric:" + e.toString());
            return VDBG;
        }
    }

    public class timerTask extends TimerTask {
        public timerTask() {
        }

        @Override
        public void run() {
            ServiceStateTracker.this.log("CellInfo Timeout invoke getAllCellInfoByRate()");
            if (ServiceStateTracker.this.mCellInfoRate != Integer.MAX_VALUE && ServiceStateTracker.this.mCellInfoRate != 0 && ServiceStateTracker.mCellInfoTimer != null) {
                ServiceStateTracker.this.log("timerTask schedule timer with period = " + ServiceStateTracker.this.mCellInfoRate + " ms");
                ServiceStateTracker.mCellInfoTimer.schedule(ServiceStateTracker.this.new timerTask(), ServiceStateTracker.this.mCellInfoRate);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ServiceStateTracker.this.log("timerTask invoke getAllCellInfoByRate() in another thread");
                    ServiceStateTracker.this.getAllCellInfoByRate();
                }
            }).start();
        }
    }

    private void onPsNetworkStateChangeResult(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
            loge("onPsNetworkStateChangeResult exception");
            return;
        }
        int[] info = (int[]) ar.result;
        int newUrcState = regCodeToServiceState(info[0]);
        log("mPsRegState:" + this.mPsRegState + ",new:" + newUrcState + ",result:" + info[0]);
        this.mPsRegStateRaw = info[0];
        if (this.mPsRegState != 0 || newUrcState == 0) {
            return;
        }
        log("set flag for ever detach, may notify attach later");
        this.bHasDetachedDuringPolling = true;
    }

    private void handlePsRegNotification(int oldState, int newState) {
        boolean specificNotify = VDBG;
        log("old:" + oldState + " ,mPsRegState:" + this.mPsRegState + ",new:" + newState);
        boolean hasGprsAttached = (oldState == 0 || this.mPsRegState != 0) ? VDBG : true;
        boolean hasGprsDetached = (oldState != 0 || this.mPsRegState == 0) ? VDBG : true;
        if (hasGprsAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
            this.mLastPSRegisteredPLMN = this.mSS.getOperatorNumeric();
            log("mLastPSRegisteredPLMN= " + this.mLastPSRegisteredPLMN);
            this.bHasDetachedDuringPolling = VDBG;
        }
        if (hasGprsDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        boolean hasGprsAttached2 = (this.mPsRegState == 0 || newState != 0) ? VDBG : true;
        boolean hasGprsDetached2 = (this.mPsRegState != 0 || newState == 0) ? VDBG : true;
        if (!hasGprsAttached2 && this.bHasDetachedDuringPolling && newState == 0) {
            specificNotify = true;
            log("need to compensate for notifying");
        }
        if (hasGprsAttached2 || specificNotify) {
            this.mAttachedRegistrants.notifyRegistrants();
            this.mLastPSRegisteredPLMN = this.mSS.getOperatorNumeric();
            log("mLastPSRegisteredPLMN= " + this.mLastPSRegisteredPLMN);
        }
        if (hasGprsDetached2) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        this.mPsRegState = newState;
        this.bHasDetachedDuringPolling = VDBG;
    }

    private void getEINFO(int eventId) {
        this.mPhone.invokeOemRilRequestStrings(new String[]{"AT+EINFO?", "+EINFO"}, obtainMessage(eventId));
        log("getEINFO for EMMRRS");
    }

    private void setEINFO(int value, Message onComplete) {
        String[] Cmd = {"AT+EINFO=" + value, "+EINFO"};
        this.mPhone.invokeOemRilRequestStrings(Cmd, onComplete);
        log("setEINFO for EMMRRS, ATCmd[0]=" + Cmd[0]);
    }

    private boolean isCurrentPhoneDataConnectionOn() {
        int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean userDataEnabled = true;
        try {
            userDataEnabled = TelephonyManager.getIntWithSubId(this.mPhone.getContext().getContentResolver(), "mobile_data", defaultDataSubId) == 1 ? true : VDBG;
        } catch (Settings.SettingNotFoundException snfe) {
            if (DBG) {
                log("isCurrentPhoneDataConnectionOn: SettingNofFoundException snfe=" + snfe);
            }
        }
        log("userDataEnabled=" + userDataEnabled + ", defaultDataSubId=" + defaultDataSubId);
        if (userDataEnabled && defaultDataSubId == SubscriptionManager.getSubIdUsingPhoneId(this.mPhone.getPhoneId())) {
            return true;
        }
        return VDBG;
    }

    protected int updateOperatorAlpha(String operatorAlphaLong) {
        int myPhoneId = this.mPhone.getPhoneId();
        if (myPhoneId == 0) {
            SystemProperties.set("gsm.operator.alpha", operatorAlphaLong);
        } else if (myPhoneId == 1) {
            SystemProperties.set("gsm.operator.alpha.2", operatorAlphaLong);
        } else if (myPhoneId == 2) {
            SystemProperties.set("gsm.operator.alpha.3", operatorAlphaLong);
        } else if (myPhoneId == 3) {
            SystemProperties.set("gsm.operator.alpha.4", operatorAlphaLong);
        }
        return 1;
    }

    private void updateNetworkInfo(int newRegState, int newNetworkType) {
        boolean isRegisted;
        int displayState = this.mCi.getDisplayState();
        if (newRegState == 1 || newRegState == 5) {
            isRegisted = true;
        } else {
            isRegisted = VDBG;
        }
        if (displayState != 1 || this.mIsForceSendScreenOnForUpdateNwInfo || (!isRegisted && displayState == 1)) {
            this.mNewSS.setRilVoiceRadioTechnology(newNetworkType);
            return;
        }
        if (this.mSS.getVoiceRegState() == 1 && isRegisted && displayState == 1) {
            if (this.mIsForceSendScreenOnForUpdateNwInfo) {
                return;
            }
            log("send screen state ON to change format of CREG");
            this.mIsForceSendScreenOnForUpdateNwInfo = true;
            this.mCi.sendScreenState(true);
            pollState();
            return;
        }
        if (displayState != 1 || !isRegisted) {
            return;
        }
        this.mNewSS.setRilVoiceRadioTechnology(this.mSS.getRilVoiceRadioTechnology());
        log("set Voice network type=" + this.mNewSS.getRilVoiceRadioTechnology() + " update network type with old type.");
    }

    public boolean isSameRadioTechnologyMode(int nRadioTechnology1, int nRadioTechnology2) {
        if ((nRadioTechnology1 == 14 && nRadioTechnology2 == 14) || (nRadioTechnology1 == 16 && nRadioTechnology2 == 16)) {
            return true;
        }
        if ((nRadioTechnology1 >= 3 && nRadioTechnology1 <= 13) || nRadioTechnology1 == 15) {
            if ((nRadioTechnology2 < 3 || nRadioTechnology2 > 13) && nRadioTechnology2 != 15) {
                return VDBG;
            }
            return true;
        }
        return VDBG;
    }

    private void setReceivedNitz(int phoneId, boolean receivedNitz) {
        log("setReceivedNitz : phoneId = " + phoneId);
        sReceiveNitz[phoneId] = receivedNitz;
    }

    private boolean getReceivedNitz() {
        return sReceiveNitz[this.mPhone.getPhoneId()];
    }

    private void onNetworkEventReceived(AsyncResult ar) {
        if (ar.exception != null || ar.result == null) {
            loge("onNetworkEventReceived exception");
            return;
        }
        int nwEventType = ((int[]) ar.result)[1];
        log("[onNetworkEventReceived] event_type:" + nwEventType);
        Intent intent = new Intent("android.intent.action.ACTION_NETWORK_EVENT");
        intent.addFlags(536870912);
        intent.putExtra("eventType", nwEventType + 1);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    public void pollState() {
        pollState(VDBG);
    }

    private void modemTriggeredPollState() {
        pollState(true);
    }

    public void pollState(boolean modemTriggered) {
        int currentNetworkMode = getPreferredNetworkModeSettings(this.mPhone.getPhoneId());
        log("pollState RadioState is " + this.mCi.getRadioState() + ", currentNetworkMode= " + currentNetworkMode);
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        if (this.mPhone.isPhoneTypeGsm() && this.dontUpdateNetworkStateFlag) {
            log("pollState is ignored!!");
            return;
        }
        switch (m109x804b995f()[this.mCi.getRadioState().ordinal()]) {
            case 1:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = VDBG;
                this.mNitzUpdatedTime = VDBG;
                if (this.mPhone.isPhoneTypeGsm()) {
                    setNullState();
                }
                if (!modemTriggered && 18 != this.mSS.getRilDataRadioTechnology() && regCodeToServiceState(this.mPsRegStateRaw) != 0) {
                    this.mPsRegStateRaw = 0;
                    pollStateDone();
                    return;
                } else if (this.mPhone.isPhoneTypeCdma()) {
                    return;
                }
            case 2:
                if (!this.mPhone.isPhoneTypeCdmaLte()) {
                    this.mNewSS.setStateOff();
                } else {
                    this.mNewSS.setStateOutOfService();
                }
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mGotCountryCode = VDBG;
                this.mNitzUpdatedTime = VDBG;
                if (this.mPhone.isPhoneTypeGsm()) {
                    setNullState();
                    this.mPsRegStateRaw = 0;
                }
                pollStateDone();
                return;
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
        int[] iArr2 = this.mPollingContext;
        iArr2[0] = iArr2[0] + 1;
        this.mCi.getDataRegistrationState(obtainMessage(5, this.mPollingContext));
        int[] iArr3 = this.mPollingContext;
        iArr3[0] = iArr3[0] + 1;
        this.mCi.getVoiceRegistrationState(obtainMessage(4, this.mPollingContext));
        if (!this.mPhone.isPhoneTypeGsm()) {
            return;
        }
        int[] iArr4 = this.mPollingContext;
        iArr4[0] = iArr4[0] + 1;
        this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
    }

    private void pollStateDone() {
        if (this.mPhone.isPhoneTypeGsm()) {
            pollStateDoneGsm();
        } else if (this.mPhone.isPhoneTypeCdma()) {
            pollStateDoneCdma();
        } else {
            pollStateDoneCdmaLte();
        }
    }

    private void pollStateDoneGsm() {
        this.iso = UsimPBMemInfo.STRING_NOT_SET;
        this.mcc = UsimPBMemInfo.STRING_NOT_SET;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, VDBG)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        log("Poll ServiceState done:  oldSS=[" + this.mSS + "] newSS=[" + this.mNewSS + "] oldMaxDataCalls=" + this.mMaxDataCalls + " mNewMaxDataCalls=" + this.mNewMaxDataCalls + " oldReasonDataDenied=" + this.mReasonDataDenied + " mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        if (this.mIsForceSendScreenOnForUpdateNwInfo) {
            log("send screen state OFF to restore format of CREG");
            this.mIsForceSendScreenOnForUpdateNwInfo = VDBG;
            if (this.mCi.getDisplayState() == 1) {
                this.mCi.sendScreenState(VDBG);
            }
        }
        boolean hasRegistered = (this.mSS.getVoiceRegState() == 0 || this.mNewSS.getVoiceRegState() != 0) ? VDBG : true;
        boolean hasDeregistered = (this.mSS.getVoiceRegState() != 0 || this.mNewSS.getVoiceRegState() == 0) ? VDBG : true;
        boolean hasGprsAttached = (this.mSS.getDataRegState() == 0 || this.mNewSS.getDataRegState() != 0) ? VDBG : true;
        boolean hasGprsDetached = (this.mSS.getDataRegState() != 0 || this.mNewSS.getDataRegState() == 0) ? VDBG : true;
        boolean hasDataRegStateChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState() ? true : VDBG;
        boolean hasVoiceRegStateChanged = this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState() ? true : VDBG;
        if (this.mSS.getRilVoiceRegState() != this.mNewSS.getRilVoiceRegState()) {
        }
        boolean hasRilVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology() ? true : VDBG;
        boolean hasRilDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology() ? true : VDBG;
        boolean z = this.mNewSS.equals(this.mSS) ? this.mNeedNotify : true;
        boolean voiceRoaming = !this.mSS.getVoiceRoaming() ? this.mNewSS.getVoiceRoaming() : VDBG;
        boolean hasVoiceRoamingOff = (!this.mSS.getVoiceRoaming() || this.mNewSS.getVoiceRoaming()) ? VDBG : true;
        boolean dataRoaming = !this.mSS.getDataRoaming() ? this.mNewSS.getDataRoaming() : VDBG;
        boolean hasDataRoamingOff = (!this.mSS.getDataRoaming() || this.mNewSS.getDataRoaming()) ? VDBG : true;
        boolean hasLocationChanged = this.mNewCellLoc.equals(this.mCellLoc) ? VDBG : true;
        boolean hasLacChanged = ((GsmCellLocation) this.mNewCellLoc).getLac() != ((GsmCellLocation) this.mCellLoc).getLac() ? true : VDBG;
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        log("pollStateDone,hasRegistered:" + hasRegistered + ",hasDeregistered:" + hasDeregistered + ",hasGprsAttached:" + hasGprsAttached + ",hasRilVoiceRadioTechnologyChanged:" + hasRilVoiceRadioTechnologyChanged + ",hasRilDataRadioTechnologyChanged:" + hasRilDataRadioTechnologyChanged + ",hasVoiceRegStateChanged:" + hasVoiceRegStateChanged + ",hasDataRegStateChanged:" + hasDataRegStateChanged + ",hasChanged:" + z + ",hasVoiceRoamingOn:" + voiceRoaming + ",hasVoiceRoamingOff:" + hasVoiceRoamingOff + ",hasDataRoamingOn:" + dataRoaming + ",hasDataRoamingOff:" + hasDataRoamingOff + ",hasLocationChanged:" + hasLocationChanged + ",hasLacChanged:" + hasLacChanged + ",sReceiveNitz:" + getReceivedNitz());
        if (hasVoiceRegStateChanged || hasDataRegStateChanged) {
            EventLog.writeEvent(EventLogTags.GSM_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        if (hasRilVoiceRadioTechnologyChanged) {
            GsmCellLocation loc = (GsmCellLocation) this.mNewCellLoc;
            int cid = loc != null ? loc.getCid() : -1;
            EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, Integer.valueOf(cid), Integer.valueOf(this.mSS.getRilVoiceRadioTechnology()), Integer.valueOf(this.mNewSS.getRilVoiceRadioTechnology()));
            if (DBG) {
                log("RAT switched " + ServiceState.rilRadioTechnologyToString(this.mSS.getRilVoiceRadioTechnology()) + " -> " + ServiceState.rilRadioTechnologyToString(this.mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
            }
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        GsmCellLocation tcl = (GsmCellLocation) this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        this.mReasonDataDenied = this.mNewReasonDataDenied;
        this.mMaxDataCalls = this.mNewMaxDataCalls;
        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
            this.mLastRegisteredPLMN = this.mSS.getOperatorNumeric();
            log("mLastRegisteredPLMN= " + this.mLastRegisteredPLMN);
            if (DBG) {
                log("pollStateDone: registering current mNitzUpdatedTime=" + this.mNitzUpdatedTime + " changing to false");
            }
            this.mNitzUpdatedTime = VDBG;
        }
        if (this.explict_update_spn == 1) {
            if (!z) {
                log("explict_update_spn trigger to refresh SPN");
                updateSpnDisplay();
            }
            this.explict_update_spn = 0;
        }
        if (z) {
            updateSpnDisplay();
            this.mNeedNotify = VDBG;
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (operatorNumeric == null || isNumeric(operatorNumeric)) {
                if (TextUtils.isEmpty(operatorNumeric)) {
                    if (DBG) {
                        log("operatorNumeric is null");
                    }
                    updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
                    tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
                    this.mGotCountryCode = VDBG;
                    this.mNitzUpdatedTime = VDBG;
                } else {
                    try {
                        this.mcc = operatorNumeric.substring(0, 3);
                        this.iso = MccTable.countryCodeForMcc(Integer.parseInt(this.mcc));
                    } catch (NumberFormatException ex) {
                        loge("pollStateDone: countryCodeForMcc error" + ex);
                    } catch (StringIndexOutOfBoundsException ex2) {
                        loge("pollStateDone: countryCodeForMcc error" + ex2);
                    }
                    tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), this.iso);
                    this.mGotCountryCode = true;
                    if (!this.mNitzUpdatedTime && !this.mcc.equals(INVALID_MCC) && !TextUtils.isEmpty(this.iso) && getAutoTimeZone()) {
                        boolean testOneUniqueOffsetPath = (SystemProperties.getBoolean("telephony.test.ignore.nitz", VDBG) && (SystemClock.uptimeMillis() & 1) == 0) ? true : VDBG;
                        ArrayList<TimeZone> uniqueZones = TimeUtils.getTimeZonesWithUniqueOffsets(this.iso);
                        if (uniqueZones.size() == 1 || testOneUniqueOffsetPath) {
                            TimeZone zone = uniqueZones.get(0);
                            if (DBG) {
                                log("pollStateDone: no nitz but one TZ for iso-cc=" + this.iso + " with zone.getID=" + zone.getID() + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath);
                            }
                            setAndBroadcastNetworkSetTimeZone(zone.getID());
                        } else if (uniqueZones.size() > 1) {
                            log("uniqueZones.size=" + uniqueZones.size() + " iso= " + this.iso);
                            TimeZone zone2 = getTimeZonesWithCapitalCity(this.iso);
                            if (zone2 != null) {
                                setAndBroadcastNetworkSetTimeZone(zone2.getID());
                            } else {
                                log("Can't find time zone for capital city");
                            }
                        } else if (DBG) {
                            log("pollStateDone: there are " + uniqueZones.size() + " unique offsets for iso-cc='" + this.iso + " testOneUniqueOffsetPath=" + testOneUniqueOffsetPath + "', do nothing");
                        }
                    }
                    if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZoneAfterNitz)) {
                        fixTimeZone(this.iso);
                    }
                }
            } else if (DBG) {
                log("operatorNumeric is Invalid value, don't update timezone");
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mSS.getVoiceRoaming());
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
            this.mEventLog.writeServiceStateChanged(this.mSS);
        }
        if (hasGprsAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasGprsDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasDataRegStateChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_IWLAN_AVAILABLE);
            } else {
                this.mPhone.notifyDataConnection(null);
            }
        }
        if (voiceRoaming) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasVoiceRoamingOff) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (dataRoaming) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOff) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        } else if (((this.mNewSS.getRilDataRegState() == 1 && (this.mSS.getRilDataRegState() == 1 || this.mSS.getRilDataRegState() == 5)) || (this.mSS.getRilDataRegState() == 5 && !this.mDataRoaming)) && this.mPsRegStateRaw == 5) {
            log("recover setup data for roaming off. OldDataRegState:" + this.mNewSS.getRilDataRegState() + " NewDataRegState:" + this.mSS.getRilDataRegState() + " NewRoamingState:" + this.mSS.getRoaming() + " NewDataRoamingState:" + this.mDataRoaming + " PsRegState:" + this.mPsRegStateRaw);
            this.mPsRegStateRaw = 1;
            if (!this.mSS.getRoaming()) {
                this.mDataRoamingOffRegistrants.notifyRegistrants();
            }
        }
        if (hasLocationChanged) {
            this.mPhone.notifyLocationChanged();
        }
        if (isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
            this.mReportedGprsNoReg = VDBG;
        } else {
            if (this.mStartedGprsRegCheck || this.mReportedGprsNoReg) {
                return;
            }
            this.mStartedGprsRegCheck = true;
            int check_period = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "gprs_register_check_period_ms", DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
            sendMessageDelayed(obtainMessage(22), check_period);
        }
    }

    protected void pollStateDoneCdma() {
        String eriText;
        updateRoamingState();
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        if (DBG) {
            log("pollStateDone: cdma oldSS=[" + this.mSS + "] newSS=[" + this.mNewSS + "]");
        }
        boolean hasRegistered = (this.mSS.getVoiceRegState() == 0 || this.mNewSS.getVoiceRegState() != 0) ? VDBG : true;
        boolean hasCdmaDataConnectionAttached = (this.mSS.getDataRegState() == 0 || this.mNewSS.getDataRegState() != 0) ? VDBG : true;
        boolean hasCdmaDataConnectionDetached = (this.mSS.getDataRegState() != 0 || this.mNewSS.getDataRegState() == 0) ? VDBG : true;
        boolean hasCdmaDataConnectionChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState() ? true : VDBG;
        boolean hasRilVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology() ? true : VDBG;
        boolean hasRilDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology() ? true : VDBG;
        boolean hasChanged = this.mNewSS.equals(this.mSS) ? VDBG : true;
        boolean voiceRoaming = !this.mSS.getVoiceRoaming() ? this.mNewSS.getVoiceRoaming() : VDBG;
        boolean hasVoiceRoamingOff = (!this.mSS.getVoiceRoaming() || this.mNewSS.getVoiceRoaming()) ? VDBG : true;
        boolean dataRoaming = !this.mSS.getDataRoaming() ? this.mNewSS.getDataRoaming() : VDBG;
        boolean hasDataRoamingOff = (!this.mSS.getDataRoaming() || this.mNewSS.getDataRoaming()) ? VDBG : true;
        boolean hasLocationChanged = this.mNewCellLoc.equals(this.mCellLoc) ? VDBG : true;
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState() || this.mSS.getDataRegState() != this.mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        CdmaCellLocation tcl = (CdmaCellLocation) this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        if (hasRilVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasRilDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
        }
        if (hasChanged) {
            if (this.mCi.getRadioState().isOn() && !this.mIsSubscriptionFromRuim) {
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText = this.mPhone.getCdmaEriText();
                } else {
                    eriText = this.mPhone.getContext().getText(R.string.accessibility_autoclick_scroll_right).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText);
            }
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = this.mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) {
                    log("operatorNumeric " + operatorNumeric + "is invalid");
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
                this.mGotCountryCode = VDBG;
            } else {
                String isoCountryCode = UsimPBMemInfo.STRING_NOT_SET;
                operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("pollStateDone: countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("pollStateDone: countryCodeForMcc error" + ex2);
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), isoCountryCode);
                this.mGotCountryCode = true;
                setOperatorIdd(operatorNumeric);
                if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZoneAfterNitz)) {
                    fixTimeZone(isoCountryCode);
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), !this.mSS.getVoiceRoaming() ? this.mSS.getDataRoaming() : true);
            updateSpnDisplay();
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
        }
        if (hasCdmaDataConnectionAttached) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionChanged || hasRilDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_IWLAN_AVAILABLE);
            } else {
                this.mPhone.notifyDataConnection(null);
            }
        }
        if (voiceRoaming) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasVoiceRoamingOff) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (dataRoaming) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOff) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (!hasLocationChanged) {
            return;
        }
        this.mPhone.notifyLocationChanged();
    }

    protected void pollStateDoneCdmaLte() {
        UiccCardApplication newUiccApplication;
        updateRoamingState();
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, VDBG)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        log("pollStateDone: lte 1 ss=[" + this.mSS + "] newSS=[" + this.mNewSS + "]");
        boolean hasRegistered = (this.mSS.getVoiceRegState() == 0 || this.mNewSS.getVoiceRegState() != 0) ? VDBG : true;
        boolean hasDeregistered = (this.mSS.getVoiceRegState() != 0 || this.mNewSS.getVoiceRegState() == 0) ? VDBG : true;
        boolean hasCdmaDataConnectionAttached = (this.mSS.getDataRegState() == 0 || this.mNewSS.getDataRegState() != 0) ? VDBG : true;
        boolean hasCdmaDataConnectionDetached = (this.mSS.getDataRegState() != 0 || this.mNewSS.getDataRegState() == 0) ? VDBG : true;
        boolean hasCdmaDataConnectionChanged = this.mSS.getDataRegState() != this.mNewSS.getDataRegState() ? true : VDBG;
        boolean hasVoiceRadioTechnologyChanged = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology() ? true : VDBG;
        boolean hasDataRadioTechnologyChanged = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology() ? true : VDBG;
        boolean hasChanged = this.mNewSS.equals(this.mSS) ? VDBG : true;
        boolean voiceRoaming = !this.mSS.getVoiceRoaming() ? this.mNewSS.getVoiceRoaming() : VDBG;
        boolean hasVoiceRoamingOff = (!this.mSS.getVoiceRoaming() || this.mNewSS.getVoiceRoaming()) ? VDBG : true;
        boolean dataRoaming = !this.mSS.getDataRoaming() ? this.mNewSS.getDataRoaming() : VDBG;
        boolean hasDataRoamingOff = (!this.mSS.getDataRoaming() || this.mNewSS.getDataRoaming()) ? VDBG : true;
        boolean hasLocationChanged = this.mNewCellLoc.equals(this.mCellLoc) ? VDBG : true;
        boolean has4gHandoff = this.mNewSS.getDataRegState() == 0 ? (this.mSS.getRilDataRadioTechnology() == 14 && this.mNewSS.getRilDataRadioTechnology() == 13) ? true : (this.mSS.getRilDataRadioTechnology() == 13 && this.mNewSS.getRilDataRadioTechnology() == 14) ? true : VDBG : VDBG;
        boolean hasMultiApnSupport = ((this.mNewSS.getRilDataRadioTechnology() != 14 && this.mNewSS.getRilDataRadioTechnology() != 13) || this.mSS.getRilDataRadioTechnology() == 14 || this.mSS.getRilDataRadioTechnology() == 13) ? VDBG : true;
        boolean hasLostMultiApnSupport = (this.mNewSS.getRilDataRadioTechnology() < 4 || this.mNewSS.getRilDataRadioTechnology() > 8) ? VDBG : true;
        TelephonyManager tm = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (DBG) {
            log("pollStateDone: hasRegistered=" + hasRegistered + " hasDeegistered=" + hasDeregistered + " hasCdmaDataConnectionAttached=" + hasCdmaDataConnectionAttached + " hasCdmaDataConnectionDetached=" + hasCdmaDataConnectionDetached + " hasCdmaDataConnectionChanged=" + hasCdmaDataConnectionChanged + " hasVoiceRadioTechnologyChanged= " + hasVoiceRadioTechnologyChanged + " hasDataRadioTechnologyChanged=" + hasDataRadioTechnologyChanged + " hasChanged=" + hasChanged + " hasVoiceRoamingOn=" + voiceRoaming + " hasVoiceRoamingOff=" + hasVoiceRoamingOff + " hasDataRoamingOn=" + dataRoaming + " hasDataRoamingOff=" + hasDataRoamingOff + " hasLocationChanged=" + hasLocationChanged + " has4gHandoff = " + has4gHandoff + " hasMultiApnSupport=" + hasMultiApnSupport + " hasLostMultiApnSupport=" + hasLostMultiApnSupport);
        }
        if (this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState() || this.mSS.getDataRegState() != this.mNewSS.getDataRegState()) {
            EventLog.writeEvent(EventLogTags.CDMA_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        int oldRilDataRadioTechnology = this.mSS.getRilDataRadioTechnology();
        ServiceState tss = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = tss;
        this.mNewSS.setStateOutOfService();
        CdmaCellLocation tcl = (CdmaCellLocation) this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = tcl;
        this.mNewSS.setStateOutOfService();
        if (hasVoiceRadioTechnologyChanged) {
            updatePhoneObject();
        }
        if (hasDataRadioTechnologyChanged) {
            tm.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
            if (oldRilDataRadioTechnology == 14 || this.mSS.getRilDataRadioTechnology() == 14) {
                log("[CDMALTE]pollStateDone: update signal for RAT switch between diff group");
                sendMessage(obtainMessage(10));
            }
        }
        if (hasRegistered) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
        }
        if (hasChanged) {
            boolean hasBrandOverride = (this.mUiccController.getUiccCard(getPhoneId()) == null || this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() == null) ? VDBG : true;
            if (!hasBrandOverride && this.mCi.getRadioState().isOn() && this.mPhone.isEriFileLoaded() && ((this.mSS.getRilVoiceRadioTechnology() != 14 || this.mPhone.getContext().getResources().getBoolean(R.^attr-private.multiChoiceItemLayout)) && !this.mIsSubscriptionFromRuim)) {
                String eriText = this.mSS.getOperatorAlphaLong();
                if (this.mSS.getVoiceRegState() == 0) {
                    eriText = this.mPhone.getCdmaEriText();
                } else if (this.mSS.getVoiceRegState() == 3) {
                    eriText = this.mIccRecords != null ? this.mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(eriText)) {
                        eriText = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (this.mSS.getDataRegState() != 0) {
                    eriText = this.mPhone.getContext().getText(R.string.accessibility_autoclick_scroll_right).toString();
                }
                this.mSS.setOperatorAlphaLong(eriText);
            }
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mIccRecords != null && this.mSS.getVoiceRegState() == 0 && this.mSS.getRilVoiceRadioTechnology() != 14) {
                boolean showSpn = ((RuimRecords) this.mIccRecords).getCsimSpnDisplayCondition();
                int iconIndex = this.mSS.getCdmaEriIconIndex();
                if (showSpn && iconIndex == 1 && isInHomeSidNid(this.mSS.getSystemId(), this.mSS.getNetworkId()) && this.mIccRecords != null && !SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                    try {
                        if (this.mServiceStateExt.allowSpnDisplayed()) {
                            String rltSpn = this.mIccRecords.getServiceProviderName();
                            if (rltSpn == null && (newUiccApplication = this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1)) != null) {
                                rltSpn = newUiccApplication.getIccRecords().getServiceProviderName();
                            }
                            log("[CDMALTE] rltSpn:" + rltSpn);
                            this.mSS.setOperatorAlphaLong(rltSpn);
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
            tm.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlphaLong());
            String prevOperatorNumeric = tm.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                int sid = this.mSS.getSystemId();
                operatorNumeric = fixUnknownMcc(operatorNumeric, sid);
            }
            tm.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            updateCarrierMccMncConfiguration(operatorNumeric, prevOperatorNumeric, this.mPhone.getContext());
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                if (DBG) {
                    log("operatorNumeric is null");
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), UsimPBMemInfo.STRING_NOT_SET);
                this.mGotCountryCode = VDBG;
            } else {
                String isoCountryCode = UsimPBMemInfo.STRING_NOT_SET;
                operatorNumeric.substring(0, 3);
                try {
                    isoCountryCode = MccTable.countryCodeForMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
                } catch (NumberFormatException ex) {
                    loge("countryCodeForMcc error" + ex);
                } catch (StringIndexOutOfBoundsException ex2) {
                    loge("countryCodeForMcc error" + ex2);
                }
                tm.setNetworkCountryIsoForPhone(this.mPhone.getPhoneId(), isoCountryCode);
                this.mGotCountryCode = true;
                setOperatorIdd(operatorNumeric);
                if (shouldFixTimeZoneNow(this.mPhone, operatorNumeric, prevOperatorNumeric, this.mNeedFixZoneAfterNitz)) {
                    fixTimeZone(isoCountryCode);
                }
            }
            tm.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), !this.mSS.getVoiceRoaming() ? this.mSS.getDataRoaming() : true);
            updateSpnDisplay();
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            this.mPhone.notifyServiceStateChanged(this.mSS);
        }
        if (hasCdmaDataConnectionAttached || has4gHandoff) {
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionDetached) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (hasCdmaDataConnectionChanged || hasDataRadioTechnologyChanged) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_IWLAN_AVAILABLE);
            } else {
                this.mPhone.notifyDataConnection(null);
            }
        }
        if (voiceRoaming) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasVoiceRoamingOff) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (dataRoaming) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (hasDataRoamingOff) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (hasLocationChanged) {
            this.mPhone.notifyLocationChanged();
        }
    }

    private boolean isInHomeSidNid(int sid, int nid) {
        if (isSidsAllZeros() || this.mHomeSystemId.length != this.mHomeNetworkId.length || sid == 0) {
            return true;
        }
        for (int i = 0; i < this.mHomeSystemId.length; i++) {
            if (this.mHomeSystemId[i] == sid && (this.mHomeNetworkId[i] == 0 || this.mHomeNetworkId[i] == 65535 || nid == 0 || nid == 65535 || this.mHomeNetworkId[i] == nid)) {
                return true;
            }
        }
        return VDBG;
    }

    protected void setOperatorIdd(String operatorNumeric) {
        String idd = UsimPBMemInfo.STRING_NOT_SET;
        try {
            idd = this.mHbpcdUtils.getIddByMcc(Integer.parseInt(operatorNumeric.substring(0, 3)));
        } catch (NumberFormatException ex) {
            loge("setOperatorIdd: idd error" + ex);
        } catch (StringIndexOutOfBoundsException ex2) {
            loge("setOperatorIdd: idd error" + ex2);
        }
        if (idd != null && !idd.isEmpty()) {
            this.mPhone.setSystemProperty("gsm.operator.idpstring", idd);
        } else {
            this.mPhone.setSystemProperty("gsm.operator.idpstring", "+");
        }
    }

    protected boolean isInvalidOperatorNumeric(String operatorNumeric) {
        if (operatorNumeric == null || operatorNumeric.length() < 5) {
            return true;
        }
        return operatorNumeric.startsWith(INVALID_MCC);
    }

    protected String fixUnknownMcc(String operatorNumeric, int sid) {
        if (sid <= 0) {
            return operatorNumeric;
        }
        boolean isNitzTimeZone = VDBG;
        int timeZone = 0;
        if (this.mSavedTimeZone != null) {
            timeZone = TimeZone.getTimeZone(this.mSavedTimeZone).getRawOffset() / MS_PER_HOUR;
            isNitzTimeZone = true;
        } else {
            TimeZone tzone = getNitzTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime);
            if (tzone != null) {
                timeZone = tzone.getRawOffset() / MS_PER_HOUR;
            }
        }
        int mcc = this.mHbpcdUtils.getMcc(sid, timeZone, this.mZoneDst ? 1 : 0, isNitzTimeZone);
        if (mcc > 0) {
            return Integer.toString(mcc) + DEFAULT_MNC;
        }
        return operatorNumeric;
    }

    protected void fixTimeZone(String isoCountryCode) {
        TimeZone zone;
        String zoneName = SystemProperties.get(TIMEZONE_PROPERTY);
        if (DBG) {
            log("fixTimeZone zoneName='" + zoneName + "' mZoneOffset=" + this.mZoneOffset + " mZoneDst=" + this.mZoneDst + " iso-cc='" + isoCountryCode + "' iso-cc-idx=" + Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode));
        }
        if (UsimPBMemInfo.STRING_NOT_SET.equals(isoCountryCode) && this.mNeedFixZoneAfterNitz) {
            zone = getNitzTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime);
            if (DBG) {
                log("pollStateDone: using NITZ TimeZone");
            }
        } else if (this.mZoneOffset != 0 || this.mZoneDst || zoneName == null || zoneName.length() <= 0 || Arrays.binarySearch(GMT_COUNTRY_CODES, isoCountryCode) >= 0) {
            zone = TimeUtils.getTimeZone(this.mZoneOffset, this.mZoneDst, this.mZoneTime, isoCountryCode);
            if (DBG) {
                log("fixTimeZone: using getTimeZone(off, dst, time, iso)");
            }
        } else {
            zone = TimeZone.getDefault();
            if (this.mPhone.isPhoneTypeGsm() && isAllowFixTimeZone()) {
                try {
                    String mccTz = getTimeZonesByMcc(this.mcc);
                    if (mccTz == null) {
                        mccTz = MccTable.defaultTimeZoneForMcc(Integer.parseInt(this.mcc));
                    }
                    if (mccTz != null) {
                        zone = TimeZone.getTimeZone(mccTz);
                        if (DBG) {
                            log("pollStateDone: try to fixTimeZone mcc:" + this.mcc + " mccTz:" + mccTz + " zone.getID=" + zone.getID());
                        }
                    }
                } catch (Exception e) {
                    log("pollStateDone: parse error: mcc=" + this.mcc);
                }
            }
            if (this.mNeedFixZoneAfterNitz) {
                long ctm = System.currentTimeMillis();
                long tzOffset = zone.getOffset(ctm);
                if (DBG) {
                    log("fixTimeZone: tzOffset=" + tzOffset + " ltod=" + TimeUtils.logTimeOfDay(ctm));
                }
                if (getAutoTime()) {
                    long adj = ctm - tzOffset;
                    if (DBG) {
                        log("fixTimeZone: adj ltod=" + TimeUtils.logTimeOfDay(adj));
                    }
                    setAndBroadcastNetworkSetTime(adj);
                } else {
                    this.mSavedTime -= tzOffset;
                    if (DBG) {
                        log("fixTimeZone: adj mSavedTime=" + this.mSavedTime);
                    }
                }
            }
            if (DBG) {
                log("fixTimeZone: using default TimeZone");
            }
        }
        this.mNeedFixZoneAfterNitz = VDBG;
        if (zone == null) {
            log("fixTimeZone: zone == null, do nothing for zone");
            return;
        }
        log("fixTimeZone: zone != null zone.getID=" + zone.getID());
        if (getAutoTimeZone()) {
            setAndBroadcastNetworkSetTimeZone(zone.getID());
        } else {
            log("fixTimeZone: skip changing zone as getAutoTimeZone was false");
        }
        saveNitzTimeZone(zone.getID());
        if (this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) {
            TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "cdma.operator.nitztimezoneid", zone.getID());
        }
    }

    private boolean isGprsConsistent(int dataRegState, int voiceRegState) {
        if (voiceRegState != 0 || dataRegState == 0) {
            return true;
        }
        return VDBG;
    }

    private TimeZone getNitzTimeZone(int offset, boolean dst, long when) {
        TimeZone guess = findTimeZone(offset, dst, when);
        if (guess == null) {
            guess = findTimeZone(offset, dst ? VDBG : true, when);
        }
        if (DBG) {
            log("getNitzTimeZone returning " + (guess == null ? guess : guess.getID()));
        }
        return guess;
    }

    private TimeZone findTimeZone(int offset, boolean dst, long when) {
        log("[NITZ],findTimeZone,offset:" + offset + ",dst:" + dst + ",when:" + when);
        int rawOffset = offset;
        if (dst) {
            rawOffset = offset - MS_PER_HOUR;
        }
        String[] zones = TimeZone.getAvailableIDs(rawOffset);
        Date d = new Date(when);
        for (String zone : zones) {
            TimeZone tz = TimeZone.getTimeZone(zone);
            if (tz.getOffset(when) == offset && tz.inDaylightTime(d) == dst) {
                log("[NITZ],find time zone.");
                return tz;
            }
        }
        return null;
    }

    private int regCodeToServiceState(int code) {
        switch (code) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 10:
            case 12:
            case 13:
            case 14:
                break;
            case 1:
            case 5:
                break;
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            default:
                loge("regCodeToServiceState: unexpected service state " + code);
                break;
        }
        return 1;
    }

    private int regCodeToRegState(int code) {
        switch (code) {
            case 10:
                return 0;
            case 11:
            default:
                return code;
            case 12:
                return 2;
            case 13:
                return 3;
            case 14:
                return 4;
        }
    }

    private String getSIMOperatorNumeric() {
        String imsi;
        IccRecords r = this.mIccRecords;
        if (r == null) {
            return null;
        }
        String mccmnc = r.getOperatorNumeric();
        if (mccmnc == null && (imsi = r.getIMSI()) != null && !imsi.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            String mccmnc2 = imsi.substring(0, 5);
            log("get MCC/MNC from IMSI = " + mccmnc2);
            return mccmnc2;
        }
        return mccmnc;
    }

    private boolean regCodeIsRoaming(int code) {
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean isRoaming = VDBG;
            String strHomePlmn = getSIMOperatorNumeric();
            String strServingPlmn = this.mNewSS.getOperatorNumeric();
            boolean ignoreDomesticRoaming = VDBG;
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                String simType = PhoneFactory.getPhone(this.mPhone.getPhoneId()).getIccCard().getIccCardType();
                if (strServingPlmn != null && strHomePlmn != null && simType != null) {
                    try {
                        if (!simType.equals(UsimPBMemInfo.STRING_NOT_SET) && simType.equals("CSIM")) {
                            if (this.mServiceStateExt.isRoamingForSpecialSIM(strServingPlmn, strHomePlmn)) {
                                return true;
                            }
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (5 == code) {
                isRoaming = true;
            }
            if (!SystemProperties.get("ro.mtk_bsp_package").equals("1")) {
                try {
                    ignoreDomesticRoaming = this.mServiceStateExt.ignoreDomesticRoaming();
                } catch (RuntimeException e2) {
                    e2.printStackTrace();
                }
            }
            if (ignoreDomesticRoaming && isRoaming && strServingPlmn != null && strHomePlmn != null) {
                log("ServingPlmn = " + strServingPlmn + " HomePlmn = " + strHomePlmn);
                if (strHomePlmn.substring(0, 3).equals(strServingPlmn.substring(0, 3))) {
                    log("Same MCC,don't set as roaming");
                    isRoaming = VDBG;
                }
            }
            if (isRoaming && strServingPlmn != null && strHomePlmn != null) {
                log("strServingPlmn = " + strServingPlmn + " strHomePlmn = " + strHomePlmn);
                for (int i = 0; i < customEhplmn.length; i++) {
                    boolean isServingPlmnInGroup = VDBG;
                    boolean isHomePlmnInGroup = VDBG;
                    for (int j = 0; j < customEhplmn[i].length; j++) {
                        if (strServingPlmn.equals(customEhplmn[i][j])) {
                            isServingPlmnInGroup = true;
                        }
                        if (strHomePlmn.equals(customEhplmn[i][j])) {
                            isHomePlmnInGroup = true;
                        }
                    }
                    if (isServingPlmnInGroup && isHomePlmnInGroup) {
                        log("Ignore roaming");
                        return VDBG;
                    }
                }
                return isRoaming;
            }
            return isRoaming;
        }
        if (5 == code) {
            return true;
        }
        return VDBG;
    }

    private boolean isSameOperatorNameFromSimAndSS(ServiceState s) {
        String spn = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNameForPhone(getPhoneId());
        String onsl = s.getOperatorAlphaLong();
        String onss = s.getOperatorAlphaShort();
        boolean zEqualsIgnoreCase = !TextUtils.isEmpty(spn) ? spn.equalsIgnoreCase(onsl) : VDBG;
        boolean zEqualsIgnoreCase2 = !TextUtils.isEmpty(spn) ? spn.equalsIgnoreCase(onss) : VDBG;
        if (zEqualsIgnoreCase) {
            return true;
        }
        return zEqualsIgnoreCase2;
    }

    private boolean isSameNamedOperators(ServiceState s) {
        return currentMccEqualsSimMcc(s) ? isSameOperatorNameFromSimAndSS(s) : VDBG;
    }

    private boolean currentMccEqualsSimMcc(ServiceState s) {
        String simNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId());
        String operatorNumeric = s.getOperatorNumeric();
        try {
            boolean equalsMcc = simNumeric.substring(0, 3).equals(operatorNumeric.substring(0, 3));
            return equalsMcc;
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isOperatorConsideredNonRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_defaultAllowlistLaunchOnPrivateDisplayPackages);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return VDBG;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return VDBG;
    }

    private boolean isOperatorConsideredRoamingMtk(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String simOperatorNumeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId());
        if (customOperatorConsiderRoamingMcc.length == 0 || TextUtils.isEmpty(operatorNumeric) || TextUtils.isEmpty(simOperatorNumeric)) {
            return VDBG;
        }
        for (String[] numerics : customOperatorConsiderRoamingMcc) {
            if (simOperatorNumeric.startsWith(numerics[0])) {
                for (int idx = 1; idx < numerics.length; idx++) {
                    if (operatorNumeric.startsWith(numerics[idx])) {
                        return true;
                    }
                }
            }
        }
        return VDBG;
    }

    private boolean isOperatorConsideredRoaming(ServiceState s) {
        String operatorNumeric = s.getOperatorNumeric();
        String[] numericArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_defaultAmbientContextServices);
        if (numericArray.length == 0 || operatorNumeric == null) {
            return VDBG;
        }
        for (String numeric : numericArray) {
            if (operatorNumeric.startsWith(numeric)) {
                return true;
            }
        }
        return VDBG;
    }

    private void onRestrictedStateChanged(AsyncResult ar) {
        RestrictedState newRs = new RestrictedState();
        if (DBG) {
            log("onRestrictedStateChanged: E rs " + this.mRestrictedState);
        }
        if (ar.exception == null) {
            int[] ints = (int[]) ar.result;
            int state = ints[0];
            boolean z = ((state & 1) == 0 && (state & 4) == 0) ? false : true;
            newRs.setCsEmergencyRestricted(z);
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                boolean z2 = ((state & 2) == 0 && (state & 4) == 0) ? false : true;
                newRs.setCsNormalRestricted(z2);
                newRs.setPsRestricted((state & 16) != 0);
            } else if (this.mPhone.isPhoneTypeGsm()) {
                log("IccCard state Not ready ");
                if (this.mRestrictedState.isCsNormalRestricted() && (state & 2) == 0 && (state & 4) == 0) {
                    newRs.setCsNormalRestricted(VDBG);
                }
                if (this.mRestrictedState.isPsRestricted() && (state & 16) == 0) {
                    newRs.setPsRestricted(VDBG);
                }
            }
            if (DBG) {
                log("onRestrictedStateChanged: new rs " + newRs);
            }
            if (!this.mRestrictedState.isPsRestricted() && newRs.isPsRestricted()) {
                this.mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(1001);
            } else if (this.mRestrictedState.isPsRestricted() && !newRs.isPsRestricted()) {
                if (this.mPhone.isPhoneTypeGsm() && this.mPollingContext[0] != 0) {
                    this.mPendingPsRestrictDisabledNotify = true;
                } else {
                    this.mPsRestrictDisabledRegistrants.notifyRegistrants();
                    setNotification(1002);
                }
            }
            if (this.mRestrictedState.isCsRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (!newRs.isCsNormalRestricted()) {
                    setNotification(1006);
                } else if (!newRs.isCsEmergencyRestricted()) {
                    setNotification(1005);
                }
            } else if (this.mRestrictedState.isCsEmergencyRestricted() && !this.mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (newRs.isCsRestricted()) {
                    setNotification(1003);
                } else if (newRs.isCsNormalRestricted()) {
                    setNotification(1005);
                }
            } else if (!this.mRestrictedState.isCsEmergencyRestricted() && this.mRestrictedState.isCsNormalRestricted()) {
                if (!newRs.isCsRestricted()) {
                    setNotification(1004);
                } else if (newRs.isCsRestricted()) {
                    setNotification(1003);
                } else if (newRs.isCsEmergencyRestricted()) {
                    setNotification(1006);
                }
            } else if (newRs.isCsRestricted()) {
                setNotification(1003);
            } else if (newRs.isCsEmergencyRestricted()) {
                setNotification(1006);
            } else if (newRs.isCsNormalRestricted()) {
                setNotification(1005);
            }
            this.mRestrictedState = newRs;
        }
        log("onRestrictedStateChanged: X rs " + this.mRestrictedState);
    }

    public CellLocation getCellLocation() {
        if (((GsmCellLocation) this.mCellLoc).getLac() >= 0 && ((GsmCellLocation) this.mCellLoc).getCid() >= 0) {
            if (DBG) {
                log("getCellLocation(): X good mCellLoc=" + this.mCellLoc);
            }
            return this.mCellLoc;
        }
        List<CellInfo> result = getAllCellInfo();
        if (result != null) {
            GsmCellLocation cellLocOther = new GsmCellLocation();
            for (CellInfo ci : result) {
                if (ci instanceof CellInfoGsm) {
                    CellInfoGsm cellInfoGsm = (CellInfoGsm) ci;
                    CellIdentityGsm cellIdentityGsm = cellInfoGsm.getCellIdentity();
                    cellLocOther.setLacAndCid(cellIdentityGsm.getLac(), cellIdentityGsm.getCid());
                    cellLocOther.setPsc(cellIdentityGsm.getPsc());
                    if (DBG) {
                        log("getCellLocation(): X ret GSM info=" + cellLocOther);
                    }
                    return cellLocOther;
                }
                if (ci instanceof CellInfoWcdma) {
                    CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) ci;
                    CellIdentityWcdma cellIdentityWcdma = cellInfoWcdma.getCellIdentity();
                    cellLocOther.setLacAndCid(cellIdentityWcdma.getLac(), cellIdentityWcdma.getCid());
                    cellLocOther.setPsc(cellIdentityWcdma.getPsc());
                    if (DBG) {
                        log("getCellLocation(): X ret WCDMA info=" + cellLocOther);
                    }
                    return cellLocOther;
                }
                if ((ci instanceof CellInfoLte) && (cellLocOther.getLac() < 0 || cellLocOther.getCid() < 0)) {
                    CellInfoLte cellInfoLte = (CellInfoLte) ci;
                    CellIdentityLte cellIdentityLte = cellInfoLte.getCellIdentity();
                    if (cellIdentityLte.getTac() != Integer.MAX_VALUE && cellIdentityLte.getCi() != Integer.MAX_VALUE) {
                        cellLocOther.setLacAndCid(cellIdentityLte.getTac(), cellIdentityLte.getCi());
                        cellLocOther.setPsc(0);
                        if (DBG) {
                            log("getCellLocation(): possible LTE cellLocOther=" + cellLocOther);
                        }
                    }
                }
            }
            if (DBG) {
                log("getCellLocation(): X ret best answer cellLocOther=" + cellLocOther);
            }
            return cellLocOther;
        }
        if (DBG) {
            log("getCellLocation(): X empty mCellLoc and CellInfo mCellLoc=" + this.mCellLoc);
        }
        return this.mCellLoc;
    }

    private void setTimeFromNITZString(String nitz, long nitzReceiveTime) {
        long start = SystemClock.elapsedRealtime();
        if (DBG) {
            log("NITZ: " + nitz + "," + nitzReceiveTime + " start=" + start + " delay=" + (start - nitzReceiveTime));
        }
        if ((this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) && nitz.length() <= 0) {
            return;
        }
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.clear();
            c.set(16, 0);
            String[] nitzSubs = nitz.split("[/:,+-]");
            int year = Integer.parseInt(nitzSubs[0]) + NITZ_UPDATE_DIFF_DEFAULT;
            if (year > MAX_NITZ_YEAR) {
                if (DBG) {
                    loge("NITZ year: " + year + " exceeds limit, skip NITZ time update");
                    return;
                }
                return;
            }
            c.set(1, year);
            int month = Integer.parseInt(nitzSubs[1]) - 1;
            c.set(2, month);
            int date = Integer.parseInt(nitzSubs[2]);
            c.set(5, date);
            int hour = Integer.parseInt(nitzSubs[3]);
            c.set(10, hour);
            int minute = Integer.parseInt(nitzSubs[4]);
            c.set(12, minute);
            int second = Integer.parseInt(nitzSubs[5]);
            c.set(13, second);
            boolean sign = nitz.indexOf(EVENT_CHANGE_IMS_STATE) == -1 ? true : VDBG;
            int tzOffset = Integer.parseInt(nitzSubs[6]);
            int dst = nitzSubs.length >= 8 ? Integer.parseInt(nitzSubs[7]) : 0;
            if (this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) {
                int ltmoffset = (sign ? 1 : -1) * tzOffset;
                if (DBG) {
                    log("[CDMA] NITZ: year = " + year + ", month = " + month + ", date = " + date + ", hour = " + hour + ", minute = " + minute + ", second = " + second + ", tzOffset = " + tzOffset + ", ltmoffset = " + ltmoffset + ", dst = " + dst);
                }
                TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "cdma.operator.ltmoffset", Integer.toString(ltmoffset));
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                dst = nitzSubs.length >= 8 ? Integer.parseInt(nitzSubs[7]) : getDstForMcc(getMobileCountryCode(), c.getTimeInMillis());
            }
            int tzOffset2 = (sign ? 1 : -1) * tzOffset * 15 * 60 * 1000;
            TimeZone zone = null;
            if (nitzSubs.length >= 9) {
                String tzname = nitzSubs[8].replace('!', '/');
                zone = TimeZone.getTimeZone(tzname);
                log("[NITZ] setTimeFromNITZString,tzname:" + tzname + " zone:" + zone);
            }
            String iso = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getNetworkCountryIsoForPhone(this.mPhone.getPhoneId());
            log("[NITZ] setTimeFromNITZString,mGotCountryCode:" + this.mGotCountryCode);
            if (zone == null && this.mGotCountryCode) {
                if (iso == null || iso.length() <= 0) {
                    zone = getNitzTimeZone(tzOffset2, dst != 0 ? true : VDBG, c.getTimeInMillis());
                } else {
                    zone = TimeUtils.getTimeZone(tzOffset2, dst != 0 ? true : VDBG, c.getTimeInMillis(), iso);
                }
            }
            if (zone == null || this.mZoneOffset != tzOffset2) {
                this.mNeedFixZoneAfterNitz = true;
                this.mZoneOffset = tzOffset2;
                this.mZoneDst = dst == 0 ? true : VDBG;
                this.mZoneTime = c.getTimeInMillis();
                setReceivedNitz(this.mPhone.getPhoneId(), true);
            } else {
                if (this.mZoneDst != (dst != 0 ? true : VDBG)) {
                    this.mNeedFixZoneAfterNitz = true;
                    this.mZoneOffset = tzOffset2;
                    this.mZoneDst = dst == 0 ? true : VDBG;
                    this.mZoneTime = c.getTimeInMillis();
                    setReceivedNitz(this.mPhone.getPhoneId(), true);
                }
            }
            if (DBG) {
                log("NITZ: tzOffset=" + tzOffset2 + " dst=" + dst + " zone=" + (zone != null ? zone.getID() : "NULL") + " iso=" + iso + " mGotCountryCode=" + this.mGotCountryCode + " mNeedFixZoneAfterNitz=" + this.mNeedFixZoneAfterNitz);
            }
            if (zone != null) {
                if (getAutoTimeZone()) {
                    setAndBroadcastNetworkSetTimeZone(zone.getID());
                }
                saveNitzTimeZone(zone.getID());
                if (this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) {
                    TelephonyManager.setTelephonyProperty(this.mPhone.getPhoneId(), "cdma.operator.nitztimezoneid", zone.getID());
                }
            }
            String ignore = SystemProperties.get("gsm.ignore-nitz");
            if (ignore != null && ignore.equals("yes")) {
                log("NITZ: Not setting clock because gsm.ignore-nitz is set");
                return;
            }
            try {
                this.mWakeLock.acquire();
                if (!this.mPhone.isPhoneTypeGsm() || getAutoTime()) {
                    long millisSinceNitzReceived = SystemClock.elapsedRealtime() - nitzReceiveTime;
                    if (millisSinceNitzReceived < 0) {
                        if (DBG) {
                            log("NITZ: not setting time, clock has rolled backwards since NITZ time was received, " + nitz);
                        }
                        if (DBG) {
                            long end = SystemClock.elapsedRealtime();
                            log("NITZ: end=" + end + " dur=" + (end - start));
                        }
                        this.mWakeLock.release();
                        return;
                    }
                    if (millisSinceNitzReceived > 2147483647L) {
                        if (DBG) {
                            log("NITZ: not setting time, processing has taken " + (millisSinceNitzReceived / 86400000) + " days");
                        }
                        if (DBG) {
                            long end2 = SystemClock.elapsedRealtime();
                            log("NITZ: end=" + end2 + " dur=" + (end2 - start));
                        }
                        this.mWakeLock.release();
                        return;
                    }
                    c.add(14, (int) millisSinceNitzReceived);
                    if (DBG) {
                        log("NITZ: Setting time of day to " + c.getTime() + " NITZ receive delay(ms): " + millisSinceNitzReceived + " gained(ms): " + (c.getTimeInMillis() - System.currentTimeMillis()) + " from " + nitz);
                    }
                    if (this.mPhone.isPhoneTypeGsm()) {
                        setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                        Rlog.i(LOG_TAG, "NITZ: after Setting time of day");
                    } else if (getAutoTime()) {
                        long gained = c.getTimeInMillis() - System.currentTimeMillis();
                        long timeSinceLastUpdate = SystemClock.elapsedRealtime() - this.mSavedAtTime;
                        int nitzUpdateSpacing = Settings.Global.getInt(this.mCr, "nitz_update_spacing", this.mNitzUpdateSpacing);
                        int nitzUpdateDiff = Settings.Global.getInt(this.mCr, "nitz_update_diff", this.mNitzUpdateDiff);
                        if (this.mSavedAtTime != 0 && timeSinceLastUpdate <= nitzUpdateSpacing && Math.abs(gained) <= nitzUpdateDiff) {
                            if (DBG) {
                                log("NITZ: ignore, a previous update was " + timeSinceLastUpdate + "ms ago and gained=" + gained + "ms");
                            }
                            if (DBG) {
                                long end3 = SystemClock.elapsedRealtime();
                                log("NITZ: end=" + end3 + " dur=" + (end3 - start));
                            }
                            this.mWakeLock.release();
                            return;
                        }
                        if (DBG) {
                            log("NITZ: Auto updating time of day to " + c.getTime() + " NITZ receive delay=" + millisSinceNitzReceived + "ms gained=" + gained + "ms from " + nitz);
                        }
                        setAndBroadcastNetworkSetTime(c.getTimeInMillis());
                    }
                }
                SystemProperties.set("gsm.nitz.time", String.valueOf(c.getTimeInMillis()));
                saveNitzTime(c.getTimeInMillis());
                this.mNitzUpdatedTime = true;
            } finally {
                if (DBG) {
                    long end4 = SystemClock.elapsedRealtime();
                    log("NITZ: end=" + end4 + " dur=" + (end4 - start));
                }
                this.mWakeLock.release();
            }
        } catch (RuntimeException ex) {
            loge("NITZ: Parsing NITZ time " + nitz + " ex=" + ex);
        }
    }

    private boolean isAllowFixTimeZone() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            if (sReceiveNitz[i]) {
                log("Phone" + i + " has received NITZ!!");
                return VDBG;
            }
        }
        log("Fix time zone allowed");
        return true;
    }

    private boolean getAutoTime() {
        try {
            if (Settings.Global.getInt(this.mCr, "auto_time") > 0) {
                return true;
            }
            return VDBG;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private boolean getAutoTimeZone() {
        try {
            if (Settings.Global.getInt(this.mCr, "auto_time_zone") > 0) {
                return true;
            }
            return VDBG;
        } catch (Settings.SettingNotFoundException e) {
            return true;
        }
    }

    private void saveNitzTimeZone(String zoneId) {
        log("saveNitzTimeZone zoneId:" + zoneId);
        this.mSavedTimeZone = zoneId;
    }

    private void saveNitzTime(long time) {
        if (DBG) {
            log("saveNitzTime: time=" + time);
        }
        this.mSavedTime = time;
        this.mSavedAtTime = SystemClock.elapsedRealtime();
    }

    private void setAndBroadcastNetworkSetTimeZone(String zoneId) {
        if (DBG) {
            log("setAndBroadcastNetworkSetTimeZone: setTimeZone=" + zoneId);
        }
        AlarmManager alarm = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        alarm.setTimeZone(zoneId);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIMEZONE");
        intent.addFlags(536870912);
        intent.putExtra("time-zone", zoneId);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        if (!DBG) {
            return;
        }
        log("setAndBroadcastNetworkSetTimeZone: call alarm.setTimeZone and broadcast zoneId=" + zoneId);
    }

    private void setAndBroadcastNetworkSetTime(long time) {
        if (DBG) {
            log("setAndBroadcastNetworkSetTime: time=" + time + "ms");
        }
        SystemClock.setCurrentTimeMillis(time);
        Intent intent = new Intent("android.intent.action.NETWORK_SET_TIME");
        intent.addFlags(536870912);
        intent.putExtra("time", time);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void revertToNitzTime() {
        if (Settings.Global.getInt(this.mCr, "auto_time", 0) == 0) {
            log("[NITZ]:revertToNitz,AUTO_TIME is 0");
            return;
        }
        if (DBG) {
            log("Reverting to NITZ Time: mSavedTime=" + this.mSavedTime + " mSavedAtTime=" + this.mSavedAtTime + " tz='" + this.mSavedTimeZone + "'");
        }
        if (this.mSavedTime == 0 || this.mSavedAtTime == 0) {
            return;
        }
        setAndBroadcastNetworkSetTime(this.mSavedTime + (SystemClock.elapsedRealtime() - this.mSavedAtTime));
    }

    private void revertToNitzTimeZone() {
        if (Settings.Global.getInt(this.mCr, "auto_time_zone", 0) == 0) {
            return;
        }
        if (getReceivedNitz()) {
            if (DBG) {
                log("Reverting to NITZ TimeZone: tz='" + this.mSavedTimeZone);
            }
            if (this.mSavedTimeZone != null) {
                setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZone);
            }
        }
        if (isAllowFixTimeZone()) {
            fixTimeZone();
            if (DBG) {
                log("Reverting to fixed TimeZone: tz='" + this.mSavedTimeZone);
            }
            if (this.mSavedTimeZone != null) {
                setAndBroadcastNetworkSetTimeZone(this.mSavedTimeZone);
                return;
            }
            return;
        }
        if (DBG) {
            log("Do nothing since other phone has received NITZ, but this phone didn't");
        }
    }

    private void setNotification(int notifyType) {
    }

    private UiccCardApplication getUiccCardApplication() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 2);
    }

    private void queueNextSignalStrengthPoll() {
        if (this.mDontPollSignalStrength) {
            return;
        }
        Message msg = obtainMessage();
        msg.what = 10;
        sendMessageDelayed(msg, 10000L);
    }

    private void notifyCdmaSubscriptionInfoReady() {
        if (this.mCdmaForSubscriptionInfoReadyRegistrants == null) {
            return;
        }
        if (DBG) {
            log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
        }
        this.mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
    }

    public void registerForDataConnectionAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAttachedRegistrants.add(r);
        if (getCurrentDataConnectionState() != 0) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForDataConnectionAttached(Handler h) {
        this.mAttachedRegistrants.remove(h);
    }

    public void registerForDataConnectionDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDetachedRegistrants.add(r);
        if (getCurrentDataConnectionState() == 0) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForDataConnectionDetached(Handler h) {
        this.mDetachedRegistrants.remove(h);
    }

    public void registerForDataRegStateOrRatChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRegStateOrRatChangedRegistrants.add(r);
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    public void unregisterForDataRegStateOrRatChanged(Handler h) {
        this.mDataRegStateOrRatChangedRegistrants.remove(h);
    }

    public void registerForNetworkAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mNetworkAttachedRegistrants.add(r);
        if (this.mSS.getVoiceRegState() != 0) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForNetworkAttached(Handler h) {
        this.mNetworkAttachedRegistrants.remove(h);
    }

    public void registerForPsRestrictedEnabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictEnabledRegistrants.add(r);
        if (!this.mRestrictedState.isPsRestricted()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForPsRestrictedEnabled(Handler h) {
        this.mPsRestrictEnabledRegistrants.remove(h);
    }

    public void registerForPsRestrictedDisabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictDisabledRegistrants.add(r);
        if (!this.mRestrictedState.isPsRestricted()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForPsRestrictedDisabled(Handler h) {
        this.mPsRestrictDisabledRegistrants.remove(h);
    }

    public void registerForSignalStrengthChanged(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mSignalStrengthChangedRegistrants.add(r);
    }

    public void unregisterForSignalStrengthChanged(Handler h) {
        this.mSignalStrengthChangedRegistrants.remove(h);
    }

    public void powerOffRadioSafely(DcTracker dcTracker) {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                if (this.mPhone.isPhoneTypeGsm() || this.mPhone.isPhoneTypeCdmaLte()) {
                    int dds = SubscriptionManager.getDefaultDataSubscriptionId();
                    int phoneSubId = this.mPhone.getSubId();
                    log("powerOffRadioSafely phoneId=" + SubscriptionManager.getPhoneId(dds) + ", dds=" + dds + ", mPhone.getSubId()=" + this.mPhone.getSubId() + ", phoneSubId=" + phoneSubId);
                    if (dds != -1 && (dcTracker.isDisconnected() || dds != phoneSubId)) {
                        dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                        if (DBG) {
                            log("Data disconnected, turn off radio right away.");
                        }
                        hangupAndPowerOff();
                    } else if (this.mPhone.isPhoneTypeGsm() || !dcTracker.isDisconnected() || (dds != this.mPhone.getSubId() && (dds == this.mPhone.getSubId() || !ProxyController.getInstance().isDataDisconnected(dds)))) {
                        if (this.mPhone.isPhoneTypeGsm() && this.mPhone.isInCall()) {
                            this.mPhone.mCT.mRingingCall.hangupIfAlive();
                            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
                            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
                        }
                        dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                        if (this.mPhone.isPhoneTypeGsm()) {
                            if (dds == -1 || SubscriptionManager.getPhoneId(dds) == Integer.MAX_VALUE) {
                                if (dcTracker.isDisconnected() || dcTracker.isOnlyIMSorEIMSPdnConnected()) {
                                    if (DBG) {
                                        log("Data disconnected (no data sub), turn off radio right away.");
                                    }
                                    hangupAndPowerOff();
                                    return;
                                } else {
                                    if (DBG) {
                                        log("Data is active on.  Wait for all data disconnect");
                                    }
                                    this.mPhone.registerForAllDataDisconnected(this, 49, null);
                                    this.mPendingRadioPowerOffAfterDataOff = true;
                                }
                            }
                        } else if (dds != this.mPhone.getSubId() && !ProxyController.getInstance().isDataDisconnected(dds)) {
                            if (DBG) {
                                log("Data is active on DDS. Wait for all data disconnect");
                            }
                            ProxyController.getInstance().registerForAllDataDisconnected(dds, this, 49, null);
                            this.mPendingRadioPowerOffAfterDataOff = true;
                        }
                        if (dcTracker.isOnlyIMSorEIMSPdnConnected()) {
                            if (DBG) {
                                log("Only IMS or EIMS connected, turn off radio right away.");
                            }
                            hangupAndPowerOff();
                            return;
                        }
                        Message msg = Message.obtain(this);
                        msg.what = 38;
                        int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                        this.mPendingRadioPowerOffAfterDataOffTag = i;
                        msg.arg1 = i;
                        if (sendMessageDelayed(msg, 5000L)) {
                            if (DBG) {
                                log("Wait upto 5s for data to disconnect, then turn off radio.");
                            }
                            this.mPendingRadioPowerOffAfterDataOff = true;
                        } else {
                            log("Cannot send delayed Msg, turn off radio right away.");
                            hangupAndPowerOff();
                            this.mPendingRadioPowerOffAfterDataOff = VDBG;
                        }
                    } else {
                        dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                        if (DBG) {
                            log("Data disconnected, turn off radio right away.");
                        }
                        hangupAndPowerOff();
                    }
                } else {
                    String[] networkNotClearData = this.mPhone.getContext().getResources().getStringArray(R.array.config_deviceSpecificSystemServices);
                    String currentNetwork = this.mSS.getOperatorNumeric();
                    if (networkNotClearData != null && currentNetwork != null) {
                        for (String str : networkNotClearData) {
                            if (currentNetwork.equals(str)) {
                                if (DBG) {
                                    log("Not disconnecting data for " + currentNetwork);
                                }
                                hangupAndPowerOff();
                                return;
                            }
                        }
                    }
                    if (dcTracker.isDisconnected()) {
                        dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                        if (DBG) {
                            log("Data disconnected, turn off radio right away.");
                        }
                        hangupAndPowerOff();
                    } else {
                        dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                        Message msg2 = Message.obtain(this);
                        msg2.what = 38;
                        int i2 = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                        this.mPendingRadioPowerOffAfterDataOffTag = i2;
                        msg2.arg1 = i2;
                        if (sendMessageDelayed(msg2, 30000L)) {
                            if (DBG) {
                                log("Wait upto 30s for data to disconnect, then turn off radio.");
                            }
                            this.mPendingRadioPowerOffAfterDataOff = true;
                        } else {
                            log("Cannot send delayed Msg, turn off radio right away.");
                            hangupAndPowerOff();
                        }
                    }
                }
            }
        }
    }

    public boolean processPendingRadioPowerOffAfterDataOff() {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                return VDBG;
            }
            if (DBG) {
                log("Process pending request to turn radio off.");
            }
            this.mPendingRadioPowerOffAfterDataOffTag++;
            hangupAndPowerOff();
            this.mPendingRadioPowerOffAfterDataOff = VDBG;
            return true;
        }
    }

    protected boolean onSignalStrengthResult(AsyncResult ar) {
        boolean isGsm = VDBG;
        if (this.mPhone.isPhoneTypeGsm() || (this.mPhone.isPhoneTypeCdmaLte() && this.mSS.getRilDataRadioTechnology() == 14)) {
            isGsm = true;
        }
        if (ar.exception != null || ar.result == null) {
            log("onSignalStrengthResult() Exception from RIL : " + ar.exception);
            this.mSignalStrength = new SignalStrength(isGsm);
        } else {
            this.mSignalStrength = (SignalStrength) ar.result;
            this.mSignalStrength.validateInput();
            this.mSignalStrength.setGsm(isGsm);
            if (DBG) {
                log("onSignalStrengthResult():" + (this.mLastSignalStrength != null ? "LastSignalStrength=" + this.mLastSignalStrength.toString() : UsimPBMemInfo.STRING_NOT_SET) + "new mSignalStrength=" + this.mSignalStrength.toString());
            }
        }
        boolean ssChanged = notifySignalStrength();
        return ssChanged;
    }

    protected void hangupAndPowerOff() {
        if (!this.mPhone.isPhoneTypeGsm() || this.mPhone.isInCall()) {
            this.mPhone.mCT.mRingingCall.hangupIfAlive();
            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        RadioManager.getInstance();
        RadioManager.sendRequestBeforeSetRadioPower(VDBG, this.mPhone.getPhoneId());
        this.mCi.setRadioPower(VDBG, null);
    }

    protected void cancelPollState() {
        this.mPollingContext = new int[1];
    }

    protected boolean shouldFixTimeZoneNow(Phone phone, String operatorNumeric, String prevOperatorNumeric, boolean needToFixTimeZone) {
        int prevMcc;
        try {
            int mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
            try {
                prevMcc = Integer.parseInt(prevOperatorNumeric.substring(0, 3));
            } catch (Exception e) {
                prevMcc = mcc + 1;
            }
            boolean iccCardExist = VDBG;
            if (this.mUiccApplcation != null) {
                iccCardExist = this.mUiccApplcation.getState() != IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN ? true : VDBG;
            }
            boolean z = (!iccCardExist || mcc == prevMcc) ? needToFixTimeZone : true;
            if (DBG) {
                long ctm = System.currentTimeMillis();
                log("shouldFixTimeZoneNow: retVal=" + z + " iccCardExist=" + iccCardExist + " operatorNumeric=" + operatorNumeric + " mcc=" + mcc + " prevOperatorNumeric=" + prevOperatorNumeric + " prevMcc=" + prevMcc + " needToFixTimeZone=" + needToFixTimeZone + " ltod=" + TimeUtils.logTimeOfDay(ctm));
            }
            return z;
        } catch (Exception e2) {
            if (DBG) {
                log("shouldFixTimeZoneNow: no mcc, operatorNumeric=" + operatorNumeric + " retVal=false");
            }
            return VDBG;
        }
    }

    public String getSystemProperty(String property, String defValue) {
        return TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), property, defValue);
    }

    public List<CellInfo> getAllCellInfo() {
        CellInfoResult result = new CellInfoResult(this, null);
        String mLog = "SST.getAllCellInfo(): ";
        int ver = this.mCi.getRilVersion();
        if (ver < 8) {
            mLog = "SST.getAllCellInfo(): not implemented. ";
            result.list = null;
        } else if (!isCallerOnDifferentThread()) {
            mLog = "SST.getAllCellInfo(): return last, same thread can't block. ";
            result.list = this.mLastCellInfoList;
        } else if (SystemClock.elapsedRealtime() - this.mLastCellInfoListTime <= LAST_CELL_INFO_LIST_MAX_AGE_MS) {
            mLog = "SST.getAllCellInfo(): return last, back to back calls. ";
            result.list = this.mLastCellInfoList;
        } else {
            Message msg = obtainMessage(43, result);
            synchronized (result.lockObj) {
                result.list = null;
                this.mCi.getCellInfoList(msg);
                try {
                    result.lockObj.wait(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        synchronized (result.lockObj) {
            if (result.list != null) {
                return result.list;
            }
            String mLog2 = mLog + "X size=0 list=null.";
            if (DBG) {
                log(mLog2);
            }
            return null;
        }
    }

    protected List<CellInfo> getAllCellInfoByRate() {
        CellInfoResult result = new CellInfoResult(this, null);
        if (DBG) {
            log("SST.getAllCellInfoByRate(): enter");
        }
        int ver = this.mCi.getRilVersion();
        if (ver >= 8) {
            if (!isCallerOnDifferentThread()) {
                if (DBG) {
                    log("SST.getAllCellInfoByRate(): return last, same thread can't block");
                }
                result.list = this.mLastCellInfoList;
            } else if (SystemClock.elapsedRealtime() - this.mLastCellInfoListTime > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                Message msg = obtainMessage(EVENT_GET_CELL_INFO_LIST_BY_RATE, result);
                synchronized (result.lockObj) {
                    this.mCi.getCellInfoList(msg);
                    try {
                        result.lockObj.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        result.list = null;
                    }
                }
            } else {
                if (DBG) {
                    log("SST.getAllCellInfoByRate(): return last, back to back calls");
                }
                result.list = this.mLastCellInfoList;
            }
        } else {
            if (DBG) {
                log("SST.getAllCellInfoByRate(): not implemented");
            }
            result.list = null;
        }
        if (DBG) {
            if (result.list != null) {
                log("SST.getAllCellInfoByRate(): X size=" + result.list.size() + " list=" + result.list);
            } else {
                log("SST.getAllCellInfoByRate(): X size=0 list=null");
            }
        }
        return result.list;
    }

    public void setCellInfoRate(int rateInMillis) {
        log("SST.setCellInfoRate()");
        this.mCellInfoRate = rateInMillis;
        updateCellInfoRate();
    }

    protected void updateCellInfoRate() {
        log("SST.updateCellInfoRate()");
        if (!this.mPhone.isPhoneTypeGsm()) {
            return;
        }
        log("updateCellInfoRate(),mCellInfoRate= " + this.mCellInfoRate);
        if (this.mCellInfoRate != Integer.MAX_VALUE && this.mCellInfoRate != 0) {
            if (mCellInfoTimer != null) {
                log("cancel previous timer if any");
                mCellInfoTimer.cancel();
                mCellInfoTimer = null;
            }
            mCellInfoTimer = new Timer(true);
            log("schedule timer with period = " + this.mCellInfoRate + " ms");
            mCellInfoTimer.schedule(new timerTask(), this.mCellInfoRate);
            return;
        }
        if ((this.mCellInfoRate != 0 && this.mCellInfoRate != Integer.MAX_VALUE) || mCellInfoTimer == null) {
            return;
        }
        log("cancel cell info timer if any");
        mCellInfoTimer.cancel();
        mCellInfoTimer = null;
    }

    public SignalStrength getSignalStrength() {
        return this.mSignalStrength;
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mCdmaForSubscriptionInfoReadyRegistrants.add(r);
        if (!isMinInfoReady()) {
            return;
        }
        r.notifyRegistrant();
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
        this.mCdmaForSubscriptionInfoReadyRegistrants.remove(h);
    }

    private void saveCdmaSubscriptionSource(int source) {
        log("Storing cdma subscription source: " + source);
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", source);
        log("Read from settings: " + Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", -1));
    }

    private void getSubscriptionInfoAndStartPollingThreads() {
        this.mCi.getCDMASubscription(obtainMessage(34));
        pollState();
    }

    private void handleCdmaSubscriptionSource(int newSubscriptionSource) {
        boolean z = VDBG;
        log("Subscription Source : " + newSubscriptionSource);
        if (newSubscriptionSource == 0) {
            z = true;
        }
        this.mIsSubscriptionFromRuim = z;
        log("isFromRuim: " + this.mIsSubscriptionFromRuim);
        saveCdmaSubscriptionSource(newSubscriptionSource);
        if (this.mIsSubscriptionFromRuim) {
            return;
        }
        sendMessage(obtainMessage(35));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ServiceStateTracker:");
        pw.println(" mSubId=" + this.mSubId);
        pw.println(" mSS=" + this.mSS);
        pw.println(" mNewSS=" + this.mNewSS);
        pw.println(" mVoiceCapable=" + this.mVoiceCapable);
        pw.println(" mRestrictedState=" + this.mRestrictedState);
        pw.println(" mPollingContext=" + this.mPollingContext + " - " + (this.mPollingContext != null ? Integer.valueOf(this.mPollingContext[0]) : UsimPBMemInfo.STRING_NOT_SET));
        pw.println(" mDesiredPowerState=" + this.mDesiredPowerState);
        pw.println(" mDontPollSignalStrength=" + this.mDontPollSignalStrength);
        pw.println(" mSignalStrength=" + this.mSignalStrength);
        pw.println(" mLastSignalStrength=" + this.mLastSignalStrength);
        pw.println(" mRestrictedState=" + this.mRestrictedState);
        pw.println(" mPendingRadioPowerOffAfterDataOff=" + this.mPendingRadioPowerOffAfterDataOff);
        pw.println(" mPendingRadioPowerOffAfterDataOffTag=" + this.mPendingRadioPowerOffAfterDataOffTag);
        pw.println(" mCellLoc=" + this.mCellLoc);
        pw.println(" mNewCellLoc=" + this.mNewCellLoc);
        pw.println(" mLastCellInfoListTime=" + this.mLastCellInfoListTime);
        pw.println(" mPreferredNetworkType=" + this.mPreferredNetworkType);
        pw.println(" mMaxDataCalls=" + this.mMaxDataCalls);
        pw.println(" mNewMaxDataCalls=" + this.mNewMaxDataCalls);
        pw.println(" mReasonDataDenied=" + this.mReasonDataDenied);
        pw.println(" mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        pw.println(" mGsmRoaming=" + this.mGsmRoaming);
        pw.println(" mDataRoaming=" + this.mDataRoaming);
        pw.println(" mEmergencyOnly=" + this.mEmergencyOnly);
        pw.println(" mNeedFixZoneAfterNitz=" + this.mNeedFixZoneAfterNitz);
        pw.flush();
        pw.println(" mZoneOffset=" + this.mZoneOffset);
        pw.println(" mZoneDst=" + this.mZoneDst);
        pw.println(" mZoneTime=" + this.mZoneTime);
        pw.println(" mGotCountryCode=" + this.mGotCountryCode);
        pw.println(" mNitzUpdatedTime=" + this.mNitzUpdatedTime);
        pw.println(" mSavedTimeZone=" + this.mSavedTimeZone);
        pw.println(" mSavedTime=" + this.mSavedTime);
        pw.println(" mSavedAtTime=" + this.mSavedAtTime);
        pw.println(" mStartedGprsRegCheck=" + this.mStartedGprsRegCheck);
        pw.println(" mReportedGprsNoReg=" + this.mReportedGprsNoReg);
        pw.println(" mNotification=" + this.mNotification);
        pw.println(" mWakeLock=" + this.mWakeLock);
        pw.println(" mCurSpn=" + this.mCurSpn);
        pw.println(" mCurDataSpn=" + this.mCurDataSpn);
        pw.println(" mCurShowSpn=" + this.mCurShowSpn);
        pw.println(" mCurPlmn=" + this.mCurPlmn);
        pw.println(" mCurShowPlmn=" + this.mCurShowPlmn);
        pw.flush();
        pw.println(" mCurrentOtaspMode=" + this.mCurrentOtaspMode);
        pw.println(" mRoamingIndicator=" + this.mRoamingIndicator);
        pw.println(" mIsInPrl=" + this.mIsInPrl);
        pw.println(" mDefaultRoamingIndicator=" + this.mDefaultRoamingIndicator);
        pw.println(" mRegistrationState=" + this.mRegistrationState);
        pw.println(" mMdn=" + this.mMdn);
        pw.println(" mHomeSystemId=" + this.mHomeSystemId);
        pw.println(" mHomeNetworkId=" + this.mHomeNetworkId);
        pw.println(" mMin=" + this.mMin);
        pw.println(" mPrlVersion=" + this.mPrlVersion);
        pw.println(" mIsMinInfoReady=" + this.mIsMinInfoReady);
        pw.println(" mIsEriTextLoaded=" + this.mIsEriTextLoaded);
        pw.println(" mIsSubscriptionFromRuim=" + this.mIsSubscriptionFromRuim);
        pw.println(" mCdmaSSM=" + this.mCdmaSSM);
        pw.println(" mRegistrationDeniedReason=" + this.mRegistrationDeniedReason);
        pw.println(" mCurrentCarrier=" + this.mCurrentCarrier);
        pw.flush();
        pw.println(" mImsRegistered=" + this.mImsRegistered);
        pw.println(" mImsRegistrationOnOff=" + this.mImsRegistrationOnOff);
        pw.println(" mAlarmSwitch=" + this.mAlarmSwitch);
        pw.println(" mPowerOffDelayNeed=" + this.mPowerOffDelayNeed);
        pw.println(" mDeviceShuttingDown=" + this.mDeviceShuttingDown);
        pw.println(" mSpnUpdatePending=" + this.mSpnUpdatePending);
    }

    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    protected void checkCorrectThread() {
        if (Thread.currentThread() == getLooper().getThread()) {
        } else {
            throw new RuntimeException("ServiceStateTracker must be used from within one thread");
        }
    }

    protected boolean isCallerOnDifferentThread() {
        if (Thread.currentThread() != getLooper().getThread()) {
            return true;
        }
        return VDBG;
    }

    protected void updateCarrierMccMncConfiguration(String newOp, String oldOp, Context context) {
        if ((newOp != null || TextUtils.isEmpty(oldOp)) && (newOp == null || newOp.equals(oldOp))) {
            return;
        }
        log("update mccmnc=" + newOp + " fromServiceState=true");
        MccTable.updateMccMncConfiguration(context, newOp, true);
    }

    protected boolean inSameCountry(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || operatorNumeric.length() < 5) {
            return VDBG;
        }
        String homeNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeNumeric) || homeNumeric.length() < 5) {
            return VDBG;
        }
        String networkMCC = operatorNumeric.substring(0, 3);
        String homeMCC = homeNumeric.substring(0, 3);
        String networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
        String homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            return VDBG;
        }
        boolean inSameCountry = homeCountry.equals(networkCountry);
        if (inSameCountry) {
            return inSameCountry;
        }
        if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
            return true;
        }
        if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
            return true;
        }
        return inSameCountry;
    }

    protected void setRoamingType(ServiceState currentServiceState) {
        boolean isVoiceInService = currentServiceState.getVoiceRegState() == 0;
        if (isVoiceInService) {
            if (currentServiceState.getVoiceRoaming()) {
                if (this.mPhone.isPhoneTypeGsm()) {
                    if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                        currentServiceState.setVoiceRoamingType(2);
                    } else {
                        currentServiceState.setVoiceRoamingType(3);
                    }
                } else {
                    int[] intRoamingIndicators = this.mPhone.getContext().getResources().getIntArray(R.array.config_deviceStatesOnWhichToWakeUp);
                    if (intRoamingIndicators != null && intRoamingIndicators.length > 0) {
                        currentServiceState.setVoiceRoamingType(2);
                        int curRoamingIndicator = currentServiceState.getCdmaRoamingIndicator();
                        int i = 0;
                        while (true) {
                            if (i >= intRoamingIndicators.length) {
                                break;
                            }
                            if (curRoamingIndicator != intRoamingIndicators[i]) {
                                i++;
                            } else {
                                currentServiceState.setVoiceRoamingType(3);
                                break;
                            }
                        }
                    } else if (inSameCountry(currentServiceState.getVoiceOperatorNumeric())) {
                        currentServiceState.setVoiceRoamingType(2);
                    } else {
                        currentServiceState.setVoiceRoamingType(3);
                    }
                }
            } else {
                currentServiceState.setVoiceRoamingType(0);
            }
        }
        boolean isDataInService = currentServiceState.getDataRegState() == 0 ? true : VDBG;
        int dataRegType = currentServiceState.getRilDataRadioTechnology();
        if (!isDataInService) {
            return;
        }
        if (!currentServiceState.getDataRoaming()) {
            currentServiceState.setDataRoamingType(0);
            return;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (ServiceState.isGsm(dataRegType)) {
                if (isVoiceInService) {
                    currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
                    return;
                } else {
                    currentServiceState.setDataRoamingType(1);
                    return;
                }
            }
            currentServiceState.setDataRoamingType(1);
            return;
        }
        if (ServiceState.isCdma(dataRegType)) {
            if (isVoiceInService) {
                currentServiceState.setDataRoamingType(currentServiceState.getVoiceRoamingType());
                return;
            } else {
                currentServiceState.setDataRoamingType(1);
                return;
            }
        }
        if (inSameCountry(currentServiceState.getDataOperatorNumeric())) {
            currentServiceState.setDataRoamingType(2);
        } else {
            currentServiceState.setDataRoamingType(3);
        }
    }

    private void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(true);
    }

    private void setNullState() {
        this.mGsmRoaming = VDBG;
        this.mNewReasonDataDenied = -1;
        this.mNewMaxDataCalls = 1;
        this.mDataRoaming = VDBG;
        this.mEmergencyOnly = VDBG;
        updateLocatedPlmn(null);
        this.mDontPollSignalStrength = VDBG;
        this.mLastSignalStrength = new SignalStrength(true);
        this.isCsInvalidCard = VDBG;
        this.mPsRegState = 1;
    }

    protected String getHomeOperatorNumeric() {
        String numeric = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (!this.mPhone.isPhoneTypeGsm() && TextUtils.isEmpty(numeric)) {
            return SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, UsimPBMemInfo.STRING_NOT_SET);
        }
        return numeric;
    }

    protected int getPhoneId() {
        return this.mPhone.getPhoneId();
    }

    protected void resetServiceStateInIwlanMode() {
        if (this.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_OFF) {
            return;
        }
        boolean resetIwlanRatVal = VDBG;
        log("set service state as POWER_OFF");
        if (18 == this.mNewSS.getRilDataRadioTechnology()) {
            log("pollStateDone: mNewSS = " + this.mNewSS);
            log("pollStateDone: reset iwlan RAT value");
            resetIwlanRatVal = true;
        }
        this.mNewSS.setStateOff();
        if (!resetIwlanRatVal) {
            return;
        }
        this.mNewSS.setRilDataRadioTechnology(18);
        this.mNewSS.setDataRegState(0);
        log("pollStateDone: mNewSS = " + this.mNewSS);
    }

    protected final boolean alwaysOnHomeNetwork(BaseBundle b) {
        return b.getBoolean("force_home_network_bool");
    }

    private boolean isInNetwork(BaseBundle b, String network, String key) {
        String[] networks = b.getStringArray(key);
        if (networks != null && Arrays.asList(networks).contains(network)) {
            return true;
        }
        return VDBG;
    }

    protected final boolean isRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "gsm_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInGsmNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "gsm_nonroaming_networks_string_array");
    }

    protected final boolean isRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "cdma_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInCdmaNetwork(BaseBundle b, String network) {
        return isInNetwork(b, network, "cdma_nonroaming_networks_string_array");
    }

    protected int getPreferredNetworkModeSettings(int phoneId) {
        int[] subId = SubscriptionManager.getSubId(phoneId);
        if (subId != null && SubscriptionManager.isValidSubscriptionId(subId[0])) {
            int networkType = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), subId[0]);
            return networkType;
        }
        log("Invalid subId, return invalid networkType");
        return -1;
    }

    protected void setSignalStrength(AsyncResult ar, boolean isGsm) {
        SignalStrength signalStrength = this.mSignalStrength;
        if (DBG && this.mLastSignalStrength != null) {
            log("Before combine Signal Strength, setSignalStrength(): isGsm = " + isGsm + " LastSignalStrength = " + this.mLastSignalStrength.toString());
        }
        if (ar.exception == null && ar.result != null) {
            this.mSignalStrength = (SignalStrength) ar.result;
            this.mSignalStrength.validateInput();
            this.mSignalStrength.setGsm(isGsm);
            if (!DBG) {
                return;
            }
            log("Before combine Signal Strength, setSignalStrength(): isGsm = " + isGsm + "new mSignalStrength = " + this.mSignalStrength.toString());
            return;
        }
        log("Before combine Signal Strength, setSignalStrength() Exception from RIL : " + ar.exception);
        this.mSignalStrength = new SignalStrength(isGsm);
    }

    public boolean isHPlmn(String plmn) {
        if (!this.mPhone.isPhoneTypeGsm()) {
            return VDBG;
        }
        String mccmnc = getSIMOperatorNumeric();
        if (plmn == null) {
            return VDBG;
        }
        if (mccmnc == null || mccmnc.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            log("isHPlmn getSIMOperatorNumeric error: " + mccmnc);
            return VDBG;
        }
        if (plmn.equals(mccmnc)) {
            return true;
        }
        if (plmn.length() == 5 && mccmnc.length() == 6 && plmn.equals(mccmnc.substring(0, 5))) {
            return true;
        }
        if (this.mPhone.getPhoneType() == 1) {
            for (int i = 0; i < customEhplmn.length; i++) {
                boolean isServingPlmnInGroup = VDBG;
                boolean isHomePlmnInGroup = VDBG;
                for (int j = 0; j < customEhplmn[i].length; j++) {
                    if (plmn.equals(customEhplmn[i][j])) {
                        isServingPlmnInGroup = true;
                    }
                    if (mccmnc.equals(customEhplmn[i][j])) {
                        isHomePlmnInGroup = true;
                    }
                }
                if (isServingPlmnInGroup && isHomePlmnInGroup) {
                    log("plmn:" + plmn + "is in customized ehplmn table");
                    return true;
                }
            }
        }
        return VDBG;
    }

    public boolean isDeviceShuttingDown() {
        return this.mDeviceShuttingDown;
    }
}
