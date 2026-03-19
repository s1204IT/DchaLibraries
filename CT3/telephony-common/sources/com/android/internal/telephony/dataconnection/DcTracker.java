package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.telephony.CallFailCause;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyEventLog;
import com.android.internal.telephony.dataconnection.DataConnection;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.google.android.mms.pdu.CharacterSets;
import com.mediatek.common.MPlugin;
import com.mediatek.common.telephony.IGsmDCTExt;
import com.mediatek.common.telephony.ITelephonyExt;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.ImsSwitchController;
import com.mediatek.internal.telephony.dataconnection.DataConnectionHelper;
import com.mediatek.internal.telephony.dataconnection.DataSubSelector;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;
import com.mediatek.internal.telephony.dataconnection.FdManager;
import com.mediatek.internal.telephony.dataconnection.IaExtendParam;
import com.mediatek.internal.telephony.gsm.GsmVTProvider;
import com.mediatek.internal.telephony.gsm.GsmVTProviderUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DcTracker extends Handler {

    private static final int[] f23comandroidinternaltelephonyDctConstants$StateSwitchesValues = null;
    private static final int APN_CHANGE_MILLIS = 1000;
    private static final int APN_CLASS_0 = 0;
    private static final int APN_CLASS_1 = 1;
    private static final int APN_CLASS_2 = 2;
    private static final int APN_CLASS_3 = 3;
    private static final int APN_CLASS_4 = 4;
    private static final int APN_CLASS_5 = 5;
    static final String APN_ID = "apn_id";
    private static final String APN_TYPE_KEY = "apnType";
    private static final boolean BSP_PACKAGE;
    private static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 60000;
    private static final int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 360000;
    private static final String DATA_STALL_ALARM_TAG_EXTRA = "data.stall.alram.tag";
    private static final boolean DATA_STALL_NOT_SUSPECTED = false;
    private static final boolean DATA_STALL_SUSPECTED = true;
    private static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";
    private static final boolean EPDG_FEATURE;
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String[] HIGH_THROUGHPUT_APN;
    private static final String[] IMS_APN;
    private static final String INTENT_DATA_STALL_ALARM = "com.android.internal.telephony.data-stall";
    private static final String INTENT_PROVISIONING_APN_ALARM = "com.android.internal.telephony.provisioning_apn_alarm";
    private static final String INTENT_RECONNECT_ALARM = "com.android.internal.telephony.data-reconnect";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_REASON = "reconnect_alarm_extra_reason";
    private static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    private static final int LTE_AS_CONNECTED = 1;
    private static final int MAX_ID_HIGH_TROUGHPUT = 1;
    private static final int MAX_ID_IMS_TROUGHPUT = 6;
    private static final int MAX_ID_OTHERS_TROUGHPUT = 3;
    protected static final String[] MCC_TABLE_DOMESTIC;
    protected static final String[] MCC_TABLE_TEST;
    private static final int MIN_ID_HIGH_TROUGHPUT = 0;
    private static final int MIN_ID_IMS_TROUGHPUT = 4;
    private static final int MIN_ID_OTHERS_TROUGHPUT = 2;
    private static final boolean MTK_APNSYNC_TEST_SUPPORT;
    protected static boolean MTK_CC33_SUPPORT = false;
    private static final boolean MTK_DUAL_APN_SUPPORT;
    private static final boolean MTK_IMS_SUPPORT;
    protected static final boolean MTK_IMS_TESTMODE_SUPPORT;
    protected static final boolean MTK_OP129_IA_SUPPORT;
    protected static final boolean MTK_OP17_IA_SUPPORT;
    protected static final boolean NEED_TO_RESUME_MODEM;
    private static final String NETWORK_TYPE_MOBILE_IMS = "MOBILEIMS";
    private static final String NETWORK_TYPE_WIFI = "WIFI";
    private static final String NO_SIM_VALUE = "N/A";
    private static final int NUMBER_SENT_PACKETS_OF_HANG = 10;
    private static final String OPERATOR_OM = "OM";
    private static final int PDP_CONNECTION_POOL_SIZE = 3;
    private static final String PLMN_OP12 = "311480";
    private static final int POLL_NETSTAT_MILLIS = 1000;
    private static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 600000;
    private static final int POLL_PDP_MILLIS = 5000;
    static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID;
    private static final String PROPERTY_FORCE_APN_CHANGE = "ril.force_apn_change";
    protected static final String PROPERTY_MOBILE_DATA_ENABLE = "persist.radio.mobile.data";
    private static final String PROPERTY_OPERATOR = "persist.operator.optr";
    private static final String PROPERTY_THROTTLING_APN_ENABLED = "ril.throttling.enabled";
    private static final String PROPERTY_THROTTLING_TIME = "persist.radio.throttling_time";
    private static final String PROPERTY_VSIM_ENABLE = "gsm.external.sim.inserted";
    private static final String PROP_APN_CLASS = "ril.md_changed_apn_class";
    private static final String PROP_APN_CLASS_ICCID = "ril.md_changed_apn_class.iccid";
    private static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 900000;
    private static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";
    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120000;
    private static final String PUPPET_MASTER_RADIO_STRESS_TEST = "gsm.defaultpdpcontext.active";
    private static final boolean RADIO_TESTS = false;
    private static final String REDIRECTION_URL_KEY = "redirectionUrl";
    protected static final int REGION_DOMESTIC = 1;
    protected static final int REGION_FOREIGN = 2;
    protected static final int REGION_UNKNOWN = 0;
    private static final String SKIP_DATA_STALL_ALARM = "persist.skip.data.stall.alarm";
    private static final boolean THROTTLING_APN_ENABLED;
    private static final int THROTTLING_MAX_PDP_SIZE = 8;
    private static final int THROTTLING_TIME_DEFAULT = 900;
    private static final String VOLTE_DEFAULT_EMERGENCY_PDN_APN_NAME = "";
    private static final String VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL = "IPV4V6";
    private static final String VOLTE_EMERGENCY_PDN_APN_NAME = "volte.emergency.pdn.name";
    private static final String VOLTE_EMERGENCY_PDN_PROTOCOL = "volte.emergency.pdn.protocol";
    private static final String VZW_800_NI = "VZW800";
    private static final String VZW_ADMIN_NI = "VZWADMIN";
    private static final String VZW_APP_NI = "VZWAPP";
    private static final String VZW_EMERGENCY_NI = "VZWEMERGENCY";
    private static final boolean VZW_FEATURE;
    private static final String VZW_IMS_NI = "VZWIMS";
    private static final String VZW_INTERNET_NI = "VZWINTERNET";
    private static int sEnableFailFastRefCounter;
    private String[] PLMN_EMPTY_APN_PCSCF_SET;
    private String[] PROPERTY_ICCID;
    protected String PROP_IMS_HANDOVER;
    private String RADIO_RESET_PROPERTY;
    public AtomicBoolean isCleanupRequired;
    private DctConstants.Activity mActivity;
    private final AlarmManager mAlarmManager;
    private ArrayList<ApnSetting> mAllApnSettings;
    private RegistrantList mAllDataDisconnectedRegistrants;
    private boolean mAllowConfig;
    private final ConcurrentHashMap<String, ApnContext> mApnContexts;
    private final SparseArray<ApnContext> mApnContextsById;
    private ApnChangeObserver mApnObserver;
    private HashMap<String, Integer> mApnToDataConnectionId;
    private AtomicBoolean mAttached;
    private AtomicBoolean mAutoAttachOnCreation;
    private boolean mAutoAttachOnCreationConfig;
    private boolean mCanSetPreferApn;
    private final ConnectivityManager mCm;
    private boolean mColdSimDetected;
    private HashMap<Integer, DcAsyncChannel> mDataConnectionAcHashMap;
    private final Handler mDataConnectionTracker;
    private HashMap<Integer, DataConnection> mDataConnections;
    private Object mDataEnabledLock;
    private PendingIntent mDataStallAlarmIntent;
    private int mDataStallAlarmTag;
    private volatile boolean mDataStallDetectionEnabled;
    private TxRxSum mDataStallTxRxSum;
    protected DcFailCauseManager mDcFcMgr;
    private HandlerThread mDcHandlerThread;
    private DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    private DcController mDcc;
    private int mDefaultRefCount;
    private ArrayList<Message> mDisconnectAllCompleteMsgList;
    private int mDisconnectPendingCount;
    private ApnSetting mEmergencyApn;
    private volatile boolean mFailFast;
    protected FdManager mFdMgr;
    private IGsmDCTExt mGsmDctExt;
    private AtomicInteger mHighThroughputIdGenerator;
    private final AtomicReference<IccRecords> mIccRecords;
    public boolean mImsRegistrationState;
    private ContentObserver mImsSwitchChangeObserver;
    private AtomicInteger mImsUniqueIdGenerator;
    private boolean mInVoiceCall;
    protected ApnSetting mInitialAttachApnSetting;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mInternalDataEnabled;
    private boolean mIsDisposed;
    private boolean mIsImsHandover;
    private boolean mIsLte;
    private boolean mIsProvisioning;
    private boolean mIsPsRestricted;
    private boolean mIsScreenOn;
    private boolean mIsSharedDefaultApn;
    private boolean mIsWifiConnected;
    private String mLteAccessStratumDataState;
    private ApnSetting mMdChangedAttachApn;
    private boolean mMvnoMatched;
    protected boolean mNeedsResumeModem;
    protected Object mNeedsResumeModemLock;
    private boolean mNetStatPollEnabled;
    private int mNetStatPollPeriod;
    private int mNetworkType;
    private int mNoRecvPollCount;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    private AtomicInteger mOthersUniqueIdGenerator;
    private boolean mOutOfCreditSimDetected;
    private final Phone mPhone;
    protected String[] mPlmnStrings;
    private final Runnable mPollNetStat;
    private ApnSetting mPreferredApn;
    ArrayList<ApnContext> mPrioritySortedApnContexts;
    private final String mProvisionActionName;
    private BroadcastReceiver mProvisionBroadcastReceiver;
    private PendingIntent mProvisioningApnAlarmIntent;
    private int mProvisioningApnAlarmTag;
    private ProgressDialog mProvisioningSpinner;
    private String mProvisioningUrl;
    private PendingIntent mReconnectIntent;
    private String mRedirectUrl;
    protected int mRegion;
    private AsyncChannel mReplyAc;
    private String mRequestedApnType;
    private boolean mReregisterOnReconnectFailure;
    private ContentResolver mResolver;
    private long mRxPkts;
    private long mSentSinceLastRecv;
    private final SettingsObserver mSettingsObserver;
    private DctConstants.State mState;
    private SubscriptionManager mSubscriptionManager;
    protected ArrayList<Integer> mSuspendId;
    private ITelephonyExt mTelephonyExt;
    private long mTxPkts;
    protected AtomicReference<UiccCardApplication> mUiccCardApplication;
    private final UiccController mUiccController;
    private AtomicInteger mUniqueIdGenerator;
    private boolean mUserDataEnabled;
    private Handler mWorkerHandler;
    private HashSet<ApnContext> redirectApnContextSet;
    private static final String LOG_TAG = "DCT";
    private static final boolean DBG = Log.isLoggable(LOG_TAG, 3);
    private static final boolean VDBG = Log.isLoggable(LOG_TAG, 3);
    private static final boolean VDBG_STALL = Log.isLoggable(LOG_TAG, 3);
    private static boolean sPolicyDataEnabled = true;

    private static int[] m360xf0fbc33d() {
        if (f23comandroidinternaltelephonyDctConstants$StateSwitchesValues != null) {
            return f23comandroidinternaltelephonyDctConstants$StateSwitchesValues;
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
        f23comandroidinternaltelephonyDctConstants$StateSwitchesValues = iArr;
        return iArr;
    }

    static {
        MTK_IMS_SUPPORT = SystemProperties.get("persist.mtk_ims_support").equals("1");
        VZW_FEATURE = SystemProperties.get(PROPERTY_OPERATOR).equals("OP12");
        EPDG_FEATURE = SystemProperties.get("persist.mtk_epdg_support").equals("1");
        MTK_APNSYNC_TEST_SUPPORT = SystemProperties.getInt("persist.apnsync.test.support", 0) == 1;
        MTK_CC33_SUPPORT = SystemProperties.getInt("persist.data.cc33.support", 0) == 1;
        MTK_DUAL_APN_SUPPORT = SystemProperties.get("ro.mtk_dtag_dual_apn_support").equals("1");
        MTK_IMS_TESTMODE_SUPPORT = SystemProperties.getInt("persist.imstestmode.support", 0) == 1;
        MCC_TABLE_TEST = new String[]{"001"};
        MCC_TABLE_DOMESTIC = new String[]{"440"};
        MTK_OP129_IA_SUPPORT = SystemProperties.get("gsm.ril.sim.op129").equals("1");
        NEED_TO_RESUME_MODEM = SystemProperties.get("gsm.ril.data.op.suspendmd").equals("1");
        MTK_OP17_IA_SUPPORT = SystemProperties.get(PROPERTY_OPERATOR).equals("op17");
        sEnableFailFastRefCounter = 0;
        PREFERAPN_NO_UPDATE_URI_USING_SUBID = Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
        BSP_PACKAGE = SystemProperties.getBoolean("ro.mtk_bsp_package", false);
        THROTTLING_APN_ENABLED = SystemProperties.get("persist.mtk_volte_support").equals("1");
        HIGH_THROUGHPUT_APN = new String[]{CharacterSets.MIMENAME_ANY_CHARSET, "default", "dun", "hipri", "tethering"};
        IMS_APN = new String[]{ImsSwitchController.IMS_SERVICE, "emergency"};
    }

    public static class DataAllowFailReason {
        private HashSet<DataAllowFailReasonType> mDataAllowFailReasonSet = new HashSet<>();

        public void addDataAllowFailReason(DataAllowFailReasonType type) {
            this.mDataAllowFailReasonSet.add(type);
        }

        public String getDataAllowFailReason() {
            StringBuilder failureReason = new StringBuilder();
            failureReason.append("isDataAllowed: No");
            for (DataAllowFailReasonType reason : this.mDataAllowFailReasonSet) {
                failureReason.append(reason.mFailReasonStr);
            }
            return failureReason.toString();
        }

        public boolean isFailForSingleReason(DataAllowFailReasonType failReasonType) {
            if (this.mDataAllowFailReasonSet.size() == 1) {
                return this.mDataAllowFailReasonSet.contains(failReasonType);
            }
            return false;
        }

        public boolean isFailForReason(DataAllowFailReasonType failReasonType) {
            return this.mDataAllowFailReasonSet.contains(failReasonType);
        }

        public void clearAllReasons() {
            this.mDataAllowFailReasonSet.clear();
        }

        public boolean isFailed() {
            return this.mDataAllowFailReasonSet.size() > 0;
        }

        public int getSizeOfFailReason() {
            return this.mDataAllowFailReasonSet.size();
        }
    }

    public enum DataAllowFailReasonType {
        NOT_ATTACHED(" - Not attached"),
        RECORD_NOT_LOADED(" - SIM not loaded"),
        ROAMING_DISABLED(" - Roaming and data roaming not enabled"),
        INVALID_PHONE_STATE(" - PhoneState is not idle"),
        CONCURRENT_VOICE_DATA_NOT_ALLOWED(" - Concurrent voice and data not allowed"),
        PS_RESTRICTED(" - mIsPsRestricted= true"),
        UNDESIRED_POWER_STATE(" - desiredPowerState= false"),
        INTERNAL_DATA_DISABLED(" - mInternalDataEnabled= false"),
        DEFAULT_DATA_UNSELECTED(" - defaultDataSelected= false"),
        FDN_ENABLED(" - FDN enabled"),
        NOT_ALLOWED(" - Not allowed");

        public String mFailReasonStr;

        public static DataAllowFailReasonType[] valuesCustom() {
            return values();
        }

        DataAllowFailReasonType(String reason) {
            this.mFailReasonStr = reason;
        }
    }

    private static class SettingsObserver extends ContentObserver {
        private static final String TAG = "DcTracker.SettingsObserver";
        private final Context mContext;
        private final Handler mHandler;
        private final HashMap<Uri, Integer> mUriEventMap;

        SettingsObserver(Context context, Handler handler) {
            super(null);
            this.mUriEventMap = new HashMap<>();
            this.mContext = context;
            this.mHandler = handler;
        }

        void observe(Uri uri, int what) {
            this.mUriEventMap.put(uri, Integer.valueOf(what));
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.registerContentObserver(uri, false, this);
        }

        void unobserve() {
            ContentResolver resolver = this.mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            Rlog.e(TAG, "Should never be reached.");
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Integer what = this.mUriEventMap.get(uri);
            if (what != null) {
                this.mHandler.obtainMessage(what.intValue()).sendToTarget();
            } else {
                Rlog.e(TAG, "No matching event to send for URI=" + uri);
            }
        }
    }

    private void registerSettingsObserver() {
        this.mSettingsObserver.unobserve();
        String simSuffix = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            simSuffix = Integer.toString(this.mPhone.getSubId());
        }
        this.mSettingsObserver.observe(Settings.Global.getUriFor("data_roaming" + simSuffix), 270347);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("device_provisioned"), 270379);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("device_provisioning_mobile_data"), 270379);
    }

    public static class TxRxSum {
        public long rxPkts;
        public long txPkts;

        public TxRxSum() {
            reset();
        }

        public TxRxSum(long txPkts, long rxPkts) {
            this.txPkts = txPkts;
            this.rxPkts = rxPkts;
        }

        public TxRxSum(TxRxSum sum) {
            this.txPkts = sum.txPkts;
            this.rxPkts = sum.rxPkts;
        }

        public void reset() {
            this.txPkts = -1L;
            this.rxPkts = -1L;
        }

        public String toString() {
            return "{txSum=" + this.txPkts + " rxSum=" + this.rxPkts + "}";
        }

        public void updateTxRxSum() {
            this.txPkts = TrafficStats.getMobileTxPackets();
            this.rxPkts = TrafficStats.getMobileRxPackets();
        }

        public void updateTcpTxRxSum() {
            this.txPkts = TrafficStats.getMobileTcpTxPackets();
            this.rxPkts = TrafficStats.getMobileTcpRxPackets();
        }
    }

    private void onActionIntentReconnectAlarm(Intent intent) {
        String reason = intent.getStringExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON);
        String apnType = intent.getStringExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE);
        int phoneSubId = this.mPhone.getSubId();
        int currSubId = intent.getIntExtra("subscription", -1);
        log("onActionIntentReconnectAlarm: currSubId = " + currSubId + " phoneSubId=" + phoneSubId);
        if (!SubscriptionManager.isValidSubscriptionId(currSubId) || currSubId != phoneSubId) {
            log("receive ReconnectAlarm but subId incorrect, ignore");
            return;
        }
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (DBG) {
            log("onActionIntentReconnectAlarm: mState=" + this.mState + " reason=" + reason + " apnType=" + apnType + " apnContext=" + apnContext + " mDataConnectionAsyncChannels=" + this.mDataConnectionAcHashMap);
        }
        if (apnContext == null || !apnContext.isEnabled()) {
            return;
        }
        apnContext.setReason(reason);
        DctConstants.State apnContextState = apnContext.getState();
        if (DBG) {
            log("onActionIntentReconnectAlarm: apnContext state=" + apnContextState);
        }
        if (apnContextState == DctConstants.State.FAILED || apnContextState == DctConstants.State.IDLE) {
            if (DBG) {
                log("onActionIntentReconnectAlarm: state is FAILED|IDLE, disassociate");
            }
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac != null) {
                if (DBG) {
                    log("onActionIntentReconnectAlarm: tearDown apnContext=" + apnContext);
                }
                dcac.tearDown(apnContext, "", null);
            }
            apnContext.setDataConnectionAc(null);
            apnContext.setState(DctConstants.State.IDLE);
        } else if (DBG) {
            log("onActionIntentReconnectAlarm: keep associated");
        }
        sendMessage(obtainMessage(270339, apnContext));
        apnContext.setReconnectIntent(null);
    }

    private void onActionIntentDataStallAlarm(Intent intent) {
        if (VDBG_STALL) {
            log("onActionIntentDataStallAlarm: action=" + intent.getAction());
        }
        Message msg = obtainMessage(270353, intent.getAction());
        msg.arg1 = intent.getIntExtra(DATA_STALL_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver() {
            super(DcTracker.this.mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean selfChange) {
            DcTracker.this.removeMessages(270355);
            DcTracker.this.sendMessageDelayed(DcTracker.this.obtainMessage(270355), 1000L);
        }
    }

    public DcTracker(Phone phone) {
        this.isCleanupRequired = new AtomicBoolean(false);
        this.mDataEnabledLock = new Object();
        this.mInternalDataEnabled = true;
        this.mUserDataEnabled = true;
        this.mRequestedApnType = "default";
        this.RADIO_RESET_PROPERTY = "gsm.radioreset";
        this.mPrioritySortedApnContexts = new ArrayList<>();
        this.mAllApnSettings = null;
        this.mPreferredApn = null;
        this.mIsPsRestricted = false;
        this.mEmergencyApn = null;
        this.mIsDisposed = false;
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        this.mProvisioningApnAlarmIntent = null;
        this.mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mReplyAc = new AsyncChannel();
        this.PROPERTY_ICCID = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mIsImsHandover = false;
        this.PROP_IMS_HANDOVER = "ril.imshandover";
        this.mMdChangedAttachApn = null;
        this.mLteAccessStratumDataState = "unknown";
        this.mNetworkType = -1;
        this.mIsLte = false;
        this.mIsSharedDefaultApn = false;
        this.mDefaultRefCount = 0;
        this.mSuspendId = new ArrayList<>();
        this.mRegion = 0;
        this.mNeedsResumeModemLock = new Object();
        this.mNeedsResumeModem = false;
        this.PLMN_EMPTY_APN_PCSCF_SET = new String[]{"26201", "44010"};
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("screen on");
                    }
                    DcTracker.this.mIsScreenOn = true;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("screen off");
                    }
                    DcTracker.this.mIsScreenOn = false;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.startsWith(DcTracker.INTENT_RECONNECT_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Reconnect alarm. Previous state was " + DcTracker.this.mState);
                    }
                    DcTracker.this.onActionIntentReconnectAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_DATA_STALL_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Data stall alarm");
                    }
                    DcTracker.this.onActionIntentDataStallAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_PROVISIONING_APN_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Provisioning apn alarm");
                    }
                    DcTracker.this.onActionIntentProvisioningApnAlarm(intent);
                    return;
                }
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    DcTracker.this.mIsWifiConnected = networkInfo != null ? networkInfo.isConnected() : false;
                    if (DcTracker.DBG) {
                        DcTracker.this.log("NETWORK_STATE_CHANGED_ACTION: mIsWifiConnected=" + DcTracker.this.mIsWifiConnected);
                        return;
                    }
                    return;
                }
                if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Wifi state changed");
                    }
                    boolean enabled = intent.getIntExtra("wifi_state", 4) == 3;
                    if (!enabled) {
                        DcTracker.this.mIsWifiConnected = false;
                    }
                    if (DcTracker.DBG) {
                        DcTracker.this.log("WIFI_STATE_CHANGED_ACTION: enabled=" + enabled + " mIsWifiConnected=" + DcTracker.this.mIsWifiConnected);
                        return;
                    }
                    return;
                }
                if (!action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("onReceive: Unknown action=" + action);
                        return;
                    }
                    return;
                }
                NetworkInfo networkInfo2 = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                int apnType = networkInfo2.getType();
                String typeName = networkInfo2.getTypeName();
                DcTracker.this.logd("onReceive: ConnectivityService action change apnType = " + apnType + " typename =" + typeName);
                if (apnType == 11 && typeName.equals(DcTracker.NETWORK_TYPE_WIFI)) {
                    DcTracker.this.onAttachApnChangedByHandover(true);
                } else if (apnType == 11 && typeName.equals(DcTracker.NETWORK_TYPE_MOBILE_IMS)) {
                    DcTracker.this.onAttachApnChangedByHandover(false);
                }
            }
        };
        this.mPollNetStat = new Runnable() {
            @Override
            public void run() {
                DcTracker.this.updateDataActivity();
                if (DcTracker.this.mIsScreenOn) {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
                } else {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_long_poll_interval_ms", 600000);
                }
                if (!DcTracker.this.mNetStatPollEnabled) {
                    return;
                }
                DcTracker.this.mDataConnectionTracker.postDelayed(this, DcTracker.this.mNetStatPollPeriod);
            }
        };
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            public final AtomicInteger mPreviousSubId = new AtomicInteger(-1);

            @Override
            public void onSubscriptionsChanged() {
                if (DcTracker.DBG) {
                    DcTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged start");
                }
                int subId = DcTracker.this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    DcTracker.this.registerSettingsObserver();
                    DcTracker.this.applyUnProvisionedSimDetected();
                }
                IccRecords r = (IccRecords) DcTracker.this.mIccRecords.get();
                String operatorNumeric = r != null ? r.getOperatorNumeric() : "";
                if (this.mPreviousSubId.getAndSet(subId) == subId || !SubscriptionManager.isValidSubscriptionId(subId) || TextUtils.isEmpty(operatorNumeric)) {
                    return;
                }
                DcTracker.this.onRecordsLoadedOrSubIdChanged();
            }
        };
        this.mDisconnectAllCompleteMsgList = new ArrayList<>();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference<>();
        this.mUiccCardApplication = new AtomicReference<>();
        this.mActivity = DctConstants.Activity.NONE;
        this.mState = DctConstants.State.IDLE;
        this.mNetStatPollEnabled = false;
        this.mDataStallTxRxSum = new TxRxSum(0L, 0L);
        this.mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mDataStallAlarmIntent = null;
        this.mNoRecvPollCount = 0;
        this.mDataStallDetectionEnabled = true;
        this.mFailFast = false;
        this.mInVoiceCall = false;
        this.mIsWifiConnected = false;
        this.mReconnectIntent = null;
        this.mAutoAttachOnCreationConfig = false;
        this.mAutoAttachOnCreation = new AtomicBoolean(false);
        this.mIsScreenOn = true;
        this.mMvnoMatched = false;
        this.mUniqueIdGenerator = new AtomicInteger(0);
        this.mDataConnections = new HashMap<>();
        this.mDataConnectionAcHashMap = new HashMap<>();
        this.mApnToDataConnectionId = new HashMap<>();
        this.mApnContexts = new ConcurrentHashMap<>();
        this.mApnContextsById = new SparseArray<>();
        this.mDisconnectPendingCount = 0;
        this.mRedirectUrl = null;
        this.mColdSimDetected = false;
        this.mOutOfCreditSimDetected = false;
        this.redirectApnContextSet = new HashSet<>();
        this.mAllowConfig = false;
        this.mImsSwitchChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                DcTracker.this.logd("mImsSwitchChangeObserver: onChange=" + selfChange);
                DcTracker.this.setInitialAttachApn();
            }
        };
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mImsRegistrationState = false;
        this.mHighThroughputIdGenerator = new AtomicInteger(0);
        this.mOthersUniqueIdGenerator = new AtomicInteger(2);
        this.mImsUniqueIdGenerator = new AtomicInteger(4);
        this.mPhone = phone;
        if (DBG) {
            log("DCT.constructor");
        }
        this.mResolver = this.mPhone.getContext().getContentResolver();
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 270369, null);
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mCm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction(INTENT_DATA_STALL_ALARM);
        filter.addAction(INTENT_PROVISIONING_APN_ALARM);
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mUserDataEnabled = getDataEnabled();
        notifyMobileDataChange(this.mUserDataEnabled ? 1 : 0);
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        this.mAutoAttachOnCreation.set(sp.getBoolean(Phone.DATA_DISABLED_ON_BOOT_KEY, false));
        this.mSubscriptionManager = SubscriptionManager.from(this.mPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mDcHandlerThread = new HandlerThread("DcHandlerThread");
        this.mDcHandlerThread.start();
        Handler dcHandler = new Handler(this.mDcHandlerThread.getLooper());
        this.mDcc = DcController.makeDcc(this.mPhone, this, dcHandler);
        this.mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(this.mPhone, dcHandler);
        logd("DualApnSupport = " + MTK_DUAL_APN_SUPPORT);
        this.mDataConnectionTracker = this;
        registerForAllEvents();
        update();
        createWorkerHandler();
        this.mApnObserver = new ApnChangeObserver();
        phone.getContext().getContentResolver().registerContentObserver(Telephony.Carriers.CONTENT_URI, true, this.mApnObserver);
        phone.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled"), true, this.mImsSwitchChangeObserver);
        initApnContexts();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            IntentFilter filter2 = new IntentFilter();
            filter2.addAction("com.android.internal.telephony.data-reconnect." + apnContext.getApnType());
            this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter2, null, this.mPhone);
        }
        this.mDcFcMgr = DcFailCauseManager.getInstance(this.mPhone);
        initEmergencyApnSetting();
        addEmergencyApnSetting();
        this.mFdMgr = FdManager.getInstance(phone);
        if (!BSP_PACKAGE) {
            try {
                this.mGsmDctExt = (IGsmDCTExt) MPlugin.createInstance(IGsmDCTExt.class.getName(), this.mPhone.getContext());
                this.mTelephonyExt = (ITelephonyExt) MPlugin.createInstance(ITelephonyExt.class.getName(), this.mPhone.getContext());
                this.mTelephonyExt.init(this.mPhone.getContext());
                this.mTelephonyExt.startDataRoamingStrategy(this.mPhone);
            } catch (Exception e) {
                logw("mGsmDctExt or mTelephonyExt init fail");
                e.printStackTrace();
            }
        }
        this.mProvisionActionName = "com.android.internal.telephony.PROVISION" + phone.getPhoneId();
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        registerSettingsObserver();
    }

    public DcTracker() {
        this.isCleanupRequired = new AtomicBoolean(false);
        this.mDataEnabledLock = new Object();
        this.mInternalDataEnabled = true;
        this.mUserDataEnabled = true;
        this.mRequestedApnType = "default";
        this.RADIO_RESET_PROPERTY = "gsm.radioreset";
        this.mPrioritySortedApnContexts = new ArrayList<>();
        this.mAllApnSettings = null;
        this.mPreferredApn = null;
        this.mIsPsRestricted = false;
        this.mEmergencyApn = null;
        this.mIsDisposed = false;
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        this.mProvisioningApnAlarmIntent = null;
        this.mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mReplyAc = new AsyncChannel();
        this.PROPERTY_ICCID = new String[]{"ril.iccid.sim1", "ril.iccid.sim2", "ril.iccid.sim3", "ril.iccid.sim4"};
        this.mIsImsHandover = false;
        this.PROP_IMS_HANDOVER = "ril.imshandover";
        this.mMdChangedAttachApn = null;
        this.mLteAccessStratumDataState = "unknown";
        this.mNetworkType = -1;
        this.mIsLte = false;
        this.mIsSharedDefaultApn = false;
        this.mDefaultRefCount = 0;
        this.mSuspendId = new ArrayList<>();
        this.mRegion = 0;
        this.mNeedsResumeModemLock = new Object();
        this.mNeedsResumeModem = false;
        this.PLMN_EMPTY_APN_PCSCF_SET = new String[]{"26201", "44010"};
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("screen on");
                    }
                    DcTracker.this.mIsScreenOn = true;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("screen off");
                    }
                    DcTracker.this.mIsScreenOn = false;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.startsWith(DcTracker.INTENT_RECONNECT_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Reconnect alarm. Previous state was " + DcTracker.this.mState);
                    }
                    DcTracker.this.onActionIntentReconnectAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_DATA_STALL_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Data stall alarm");
                    }
                    DcTracker.this.onActionIntentDataStallAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_PROVISIONING_APN_ALARM)) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Provisioning apn alarm");
                    }
                    DcTracker.this.onActionIntentProvisioningApnAlarm(intent);
                    return;
                }
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    DcTracker.this.mIsWifiConnected = networkInfo != null ? networkInfo.isConnected() : false;
                    if (DcTracker.DBG) {
                        DcTracker.this.log("NETWORK_STATE_CHANGED_ACTION: mIsWifiConnected=" + DcTracker.this.mIsWifiConnected);
                        return;
                    }
                    return;
                }
                if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("Wifi state changed");
                    }
                    boolean enabled = intent.getIntExtra("wifi_state", 4) == 3;
                    if (!enabled) {
                        DcTracker.this.mIsWifiConnected = false;
                    }
                    if (DcTracker.DBG) {
                        DcTracker.this.log("WIFI_STATE_CHANGED_ACTION: enabled=" + enabled + " mIsWifiConnected=" + DcTracker.this.mIsWifiConnected);
                        return;
                    }
                    return;
                }
                if (!action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if (DcTracker.DBG) {
                        DcTracker.this.log("onReceive: Unknown action=" + action);
                        return;
                    }
                    return;
                }
                NetworkInfo networkInfo2 = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                int apnType = networkInfo2.getType();
                String typeName = networkInfo2.getTypeName();
                DcTracker.this.logd("onReceive: ConnectivityService action change apnType = " + apnType + " typename =" + typeName);
                if (apnType == 11 && typeName.equals(DcTracker.NETWORK_TYPE_WIFI)) {
                    DcTracker.this.onAttachApnChangedByHandover(true);
                } else if (apnType == 11 && typeName.equals(DcTracker.NETWORK_TYPE_MOBILE_IMS)) {
                    DcTracker.this.onAttachApnChangedByHandover(false);
                }
            }
        };
        this.mPollNetStat = new Runnable() {
            @Override
            public void run() {
                DcTracker.this.updateDataActivity();
                if (DcTracker.this.mIsScreenOn) {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
                } else {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_long_poll_interval_ms", 600000);
                }
                if (!DcTracker.this.mNetStatPollEnabled) {
                    return;
                }
                DcTracker.this.mDataConnectionTracker.postDelayed(this, DcTracker.this.mNetStatPollPeriod);
            }
        };
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            public final AtomicInteger mPreviousSubId = new AtomicInteger(-1);

            @Override
            public void onSubscriptionsChanged() {
                if (DcTracker.DBG) {
                    DcTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged start");
                }
                int subId = DcTracker.this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    DcTracker.this.registerSettingsObserver();
                    DcTracker.this.applyUnProvisionedSimDetected();
                }
                IccRecords r = (IccRecords) DcTracker.this.mIccRecords.get();
                String operatorNumeric = r != null ? r.getOperatorNumeric() : "";
                if (this.mPreviousSubId.getAndSet(subId) == subId || !SubscriptionManager.isValidSubscriptionId(subId) || TextUtils.isEmpty(operatorNumeric)) {
                    return;
                }
                DcTracker.this.onRecordsLoadedOrSubIdChanged();
            }
        };
        this.mDisconnectAllCompleteMsgList = new ArrayList<>();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference<>();
        this.mUiccCardApplication = new AtomicReference<>();
        this.mActivity = DctConstants.Activity.NONE;
        this.mState = DctConstants.State.IDLE;
        this.mNetStatPollEnabled = false;
        this.mDataStallTxRxSum = new TxRxSum(0L, 0L);
        this.mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
        this.mDataStallAlarmIntent = null;
        this.mNoRecvPollCount = 0;
        this.mDataStallDetectionEnabled = true;
        this.mFailFast = false;
        this.mInVoiceCall = false;
        this.mIsWifiConnected = false;
        this.mReconnectIntent = null;
        this.mAutoAttachOnCreationConfig = false;
        this.mAutoAttachOnCreation = new AtomicBoolean(false);
        this.mIsScreenOn = true;
        this.mMvnoMatched = false;
        this.mUniqueIdGenerator = new AtomicInteger(0);
        this.mDataConnections = new HashMap<>();
        this.mDataConnectionAcHashMap = new HashMap<>();
        this.mApnToDataConnectionId = new HashMap<>();
        this.mApnContexts = new ConcurrentHashMap<>();
        this.mApnContextsById = new SparseArray<>();
        this.mDisconnectPendingCount = 0;
        this.mRedirectUrl = null;
        this.mColdSimDetected = false;
        this.mOutOfCreditSimDetected = false;
        this.redirectApnContextSet = new HashSet<>();
        this.mAllowConfig = false;
        this.mImsSwitchChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                DcTracker.this.logd("mImsSwitchChangeObserver: onChange=" + selfChange);
                DcTracker.this.setInitialAttachApn();
            }
        };
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mImsRegistrationState = false;
        this.mHighThroughputIdGenerator = new AtomicInteger(0);
        this.mOthersUniqueIdGenerator = new AtomicInteger(2);
        this.mImsUniqueIdGenerator = new AtomicInteger(4);
        this.mAlarmManager = null;
        this.mCm = null;
        this.mPhone = null;
        this.mUiccController = null;
        this.mDataConnectionTracker = null;
        this.mProvisionActionName = null;
        this.mSettingsObserver = new SettingsObserver(null, this);
    }

    public void registerServiceStateTrackerEvents() {
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this, 270352, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this, 270345, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOn(this, 270347, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 270348, null);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedEnabled(this, 270358, null);
        this.mPhone.getServiceStateTracker().registerForPsRestrictedDisabled(this, 270359, null);
        this.mPhone.getServiceStateTracker().registerForDataRegStateOrRatChanged(this, 270377, null);
    }

    public void unregisterServiceStateTrackerEvents() {
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionAttached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataConnectionDetached(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOn(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRoamingOff(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedEnabled(this);
        this.mPhone.getServiceStateTracker().unregisterForPsRestrictedDisabled(this);
        this.mPhone.getServiceStateTracker().unregisterForDataRegStateOrRatChanged(this);
    }

    private void registerForAllEvents() {
        logd("registerForAllEvents: mPhone = " + this.mPhone);
        this.mPhone.mCi.registerForAvailable(this, 270337, null);
        this.mPhone.mCi.registerForOffOrNotAvailable(this, 270342, null);
        this.mPhone.mCi.registerForDataNetworkStateChanged(this, 270340, null);
        registerServiceStateTrackerEvents();
        this.mPhone.mCi.registerForRemoveRestrictEutran(this, 270842, null);
        if (MTK_OP17_IA_SUPPORT) {
            this.mPhone.mCi.setOnPlmnChangeNotification(this, 270848, null);
            this.mPhone.mCi.setOnRegistrationSuspended(this, 270849, null);
        }
        if (MTK_OP129_IA_SUPPORT) {
            this.mPhone.mCi.setOnPlmnChangeNotification(this, 270848, null);
            if (NEED_TO_RESUME_MODEM) {
                this.mPhone.mCi.setOnRegistrationSuspended(this, 270849, null);
            }
        }
        this.mPhone.mCi.registerForResetAttachApn(this, 270844, null);
        this.mPhone.mCi.registerForAttachApnChanged(this, 270852, null);
        this.mPhone.mCi.registerForPcoStatus(this, 270851, null);
        this.mPhone.mCi.registerForLteAccessStratumState(this, 270847, null);
        this.mPhone.mCi.registerSetDataAllowed(this, 270853, null);
    }

    public void dispose() {
        if (DBG) {
            log("DCT.dispose");
        }
        if (this.mTelephonyExt != null) {
            this.mTelephonyExt.stopDataRoamingStrategy();
        }
        if (this.mProvisionBroadcastReceiver != null) {
            this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
            this.mProvisionBroadcastReceiver = null;
        }
        if (this.mProvisioningSpinner != null) {
            this.mProvisioningSpinner.dismiss();
            this.mProvisioningSpinner = null;
        }
        cleanUpAllConnections(true, (String) null);
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            dcac.disconnect();
        }
        this.mDataConnectionAcHashMap.clear();
        this.mIsDisposed = true;
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        this.mUiccController.unregisterForIccChanged(this);
        this.mSettingsObserver.unobserve();
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mDcc.dispose();
        this.mDcTesterFailBringUpAll.dispose();
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mApnObserver);
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mImsSwitchChangeObserver);
        unregisterForAllEvents();
        this.mApnContexts.clear();
        this.mApnContextsById.clear();
        this.mPrioritySortedApnContexts.clear();
        unregisterForAllEvents();
        destroyDataConnections();
        if (this.mWorkerHandler != null) {
            Looper looper = this.mWorkerHandler.getLooper();
            looper.quit();
        }
        if (this.mDcHandlerThread != null) {
            this.mDcHandlerThread.quit();
            this.mDcHandlerThread = null;
        }
        this.mDcFcMgr.dispose();
    }

    private void unregisterForAllEvents() {
        logd("unregisterForAllEvents: mPhone = " + this.mPhone);
        this.mPhone.mCi.unregisterForAvailable(this);
        this.mPhone.mCi.unregisterForOffOrNotAvailable(this);
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.unregisterForRecordsLoaded(this);
            this.mIccRecords.set(null);
        }
        this.mPhone.mCi.unregisterForDataNetworkStateChanged(this);
        unregisterServiceStateTrackerEvents();
        this.mPhone.mCi.unregisterForRemoveRestrictEutran(this);
        if (MTK_OP17_IA_SUPPORT) {
            this.mPhone.mCi.unSetOnPlmnChangeNotification(this);
            this.mPhone.mCi.unSetOnRegistrationSuspended(this);
        }
        if (MTK_OP129_IA_SUPPORT) {
            this.mPhone.mCi.unSetOnPlmnChangeNotification(this);
            if (NEED_TO_RESUME_MODEM) {
                this.mPhone.mCi.unSetOnRegistrationSuspended(this);
            }
        }
        this.mPhone.mCi.unregisterForResetAttachApn(this);
        this.mPhone.mCi.unregisterForAttachApnChanged(this);
        this.mPhone.mCi.unregisterForPcoStatus(this);
        this.mPhone.mCi.unregisterForLteAccessStratumState(this);
        this.mPhone.mCi.unregisterSetDataAllowed(this);
    }

    private void onResetDone(AsyncResult ar) {
        if (DBG) {
            log("EVENT_RESET_DONE");
        }
        String reason = null;
        if (ar.userObj instanceof String) {
            reason = (String) ar.userObj;
        }
        gotoIdleAndNotifyDataConnection(reason);
    }

    public void setDataEnabled(boolean enable) {
        Message msg = obtainMessage(270366);
        msg.arg1 = enable ? 1 : 0;
        if (DBG) {
            log("setDataEnabled: sendMessage: enable=" + enable);
        }
        sendMessage(msg);
        if (!MTK_CC33_SUPPORT) {
            return;
        }
        this.mPhone.mCi.setDataOnToMD(enable, null);
    }

    private void onSetUserDataEnabled(boolean enabled) {
        synchronized (this.mDataEnabledLock) {
            if (this.mUserDataEnabled != enabled) {
                this.mUserDataEnabled = enabled;
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    Settings.Global.putInt(this.mResolver, "mobile_data", enabled ? 1 : 0);
                } else {
                    int phoneSubId = this.mPhone.getSubId();
                    Settings.Global.putInt(this.mResolver, "mobile_data" + phoneSubId, enabled ? 1 : 0);
                }
                setUserDataProperty(enabled);
                notifyMobileDataChange(enabled ? 1 : 0);
                syncDataSettingsToMd(enabled, getDataOnRoamingEnabled());
                if (!getDataOnRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
                    if (enabled) {
                        notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
                    } else {
                        notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_DATA_DISABLED);
                    }
                }
                if (enabled) {
                    onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
                } else if (BSP_PACKAGE) {
                    onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                } else {
                    for (ApnContext apnContext : this.mApnContexts.values()) {
                        if (!isDataAllowedAsOff(apnContext.getApnType())) {
                            apnContext.setReason(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                            onCleanUpConnection(true, ApnContext.apnIdForApnName(apnContext.getApnType()), PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                        }
                    }
                }
            }
        }
    }

    private void onDeviceProvisionedChange() {
        if (getDataEnabled()) {
            this.mUserDataEnabled = true;
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
        } else {
            this.mUserDataEnabled = false;
            onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
        }
    }

    public long getSubId() {
        return this.mPhone.getSubId();
    }

    public DctConstants.Activity getActivity() {
        return this.mActivity;
    }

    private void setActivity(DctConstants.Activity activity) {
        log("setActivity = " + activity);
        this.mActivity = activity;
        this.mPhone.notifyDataActivity();
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog log) {
        int apnId = ApnContext.apnIdForNetworkRequest(networkRequest);
        ApnContext apnContext = this.mApnContextsById.get(apnId);
        log.log("DcTracker.requestNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) {
            apnContext.incRefCount(log);
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog log) {
        int apnId = ApnContext.apnIdForNetworkRequest(networkRequest);
        ApnContext apnContext = this.mApnContextsById.get(apnId);
        log.log("DcTracker.releaseNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) {
            apnContext.decRefCount(log);
        }
    }

    public boolean isApnSupported(String name) {
        if (name == null) {
            loge("isApnSupported: name=null");
            return false;
        }
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
            return false;
        }
        return true;
    }

    private boolean isColdSimDetected() {
        SubscriptionInfo subInfo;
        int subId = this.mPhone.getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId) && (subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId)) != null) {
            int simProvisioningStatus = subInfo.getSimProvisioningStatus();
            if (simProvisioningStatus == 1) {
                log("Cold Sim Detected on SubId: " + subId);
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean isOutOfCreditSimDetected() {
        SubscriptionInfo subInfo;
        int subId = this.mPhone.getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId) && (subInfo = this.mSubscriptionManager.getActiveSubscriptionInfo(subId)) != null) {
            int simProvisioningStatus = subInfo.getSimProvisioningStatus();
            if (simProvisioningStatus == 2) {
                log("Out Of Credit Sim Detected on SubId: " + subId);
                return true;
            }
            return false;
        }
        return false;
    }

    public int getApnPriority(String name) {
        ApnContext apnContext = this.mApnContexts.get(name);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + name);
        }
        return apnContext.priority;
    }

    private void setRadio(boolean on) {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        try {
            phone.setRadio(on);
        } catch (Exception e) {
        }
    }

    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String provisionUrl, String networkOperator) {
            this.mNetworkOperator = networkOperator;
            this.mProvisionUrl = provisionUrl;
        }

        private void setEnableFailFastMobileData(int enabled) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270372, enabled, 0));
        }

        private void enableMobileProvisioning() {
            Message msg = DcTracker.this.obtainMessage(270373);
            msg.setData(Bundle.forPair("provisioningUrl", this.mProvisionUrl));
            DcTracker.this.sendMessage(msg);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DcTracker.this.mProvisioningSpinner = new ProgressDialog(context);
            DcTracker.this.mProvisioningSpinner.setTitle(this.mNetworkOperator);
            DcTracker.this.mProvisioningSpinner.setMessage(context.getText(R.string.indeterminate_progress_28));
            DcTracker.this.mProvisioningSpinner.setIndeterminate(true);
            DcTracker.this.mProvisioningSpinner.setCancelable(true);
            DcTracker.this.mProvisioningSpinner.getWindow().setType(TelephonyEventLog.TAG_IMS_CALL_MERGE);
            DcTracker.this.mProvisioningSpinner.show();
            DcTracker.this.sendMessageDelayed(DcTracker.this.obtainMessage(270378, DcTracker.this.mProvisioningSpinner), 120000L);
            DcTracker.this.setRadio(true);
            setEnableFailFastMobileData(1);
            enableMobileProvisioning();
        }
    }

    public boolean isDataPossible(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext == null) {
            return false;
        }
        boolean apnContextIsEnabled = apnContext.isEnabled();
        DctConstants.State apnContextState = apnContext.getState();
        boolean apnTypePossible = (apnContextIsEnabled && apnContextState == DctConstants.State.FAILED) ? false : true;
        boolean isEmergencyApn = apnContext.getApnType().equals("emergency");
        boolean dataAllowed = !isEmergencyApn ? isDataAllowed(null) : true;
        boolean z = dataAllowed ? apnTypePossible : false;
        if ((apnContext.getApnType().equals("default") || apnContext.getApnType().equals("ia")) && this.mPhone.getServiceState().getRilDataRadioTechnology() == 18) {
            log("Default data call activation not possible in iwlan.");
            z = false;
        }
        if (VDBG) {
            log(String.format("isDataPossible(%s): possible=%b isDataAllowed=%b apnTypePossible=%b apnContextisEnabled=%b apnContextState()=%s", apnType, Boolean.valueOf(z), Boolean.valueOf(dataAllowed), Boolean.valueOf(apnTypePossible), Boolean.valueOf(apnContextIsEnabled), apnContextState));
        }
        return z;
    }

    protected void finalize() {
        if (DBG) {
            log("finalize");
        }
    }

    private ApnContext addApnContext(String type, NetworkConfig networkConfig) {
        ApnContext apnContext = new ApnContext(this.mPhone, type, LOG_TAG, networkConfig, this);
        this.mApnContexts.put(type, apnContext);
        this.mApnContextsById.put(ApnContext.apnIdForApnName(type), apnContext);
        this.mPrioritySortedApnContexts.add(apnContext);
        return apnContext;
    }

    private void initApnContexts() {
        ApnContext apnContext;
        log("initApnContexts: E");
        String[] networkConfigStrings = this.mPhone.getContext().getResources().getStringArray(R.array.config_ambientThresholdLevels);
        for (String networkConfigString : networkConfigStrings) {
            NetworkConfig networkConfig = new NetworkConfig(networkConfigString);
            switch (networkConfig.type) {
                case 0:
                    apnContext = addApnContext("default", networkConfig);
                    break;
                case 1:
                case 6:
                case 7:
                case 8:
                case 9:
                case 13:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case SmsHeader.ELT_ID_OBJECT_DISTR_INDICATOR:
                case 24:
                case 25:
                case 26:
                case CallFailCause.DESTINATION_OUT_OF_ORDER:
                case CallFailCause.INVALID_NUMBER_FORMAT:
                case CallFailCause.FACILITY_REJECTED:
                case 30:
                case 31:
                case 32:
                case 33:
                case 38:
                default:
                    log("initApnContexts: skipping unknown type=" + networkConfig.type);
                    continue;
                    break;
                case 2:
                    apnContext = addApnContext("mms", networkConfig);
                    break;
                case 3:
                    apnContext = addApnContext("supl", networkConfig);
                    break;
                case 4:
                    apnContext = addApnContext("dun", networkConfig);
                    break;
                case 5:
                    apnContext = addApnContext("hipri", networkConfig);
                    break;
                case 10:
                    apnContext = addApnContext("fota", networkConfig);
                    break;
                case 11:
                    apnContext = addApnContext(ImsSwitchController.IMS_SERVICE, networkConfig);
                    break;
                case 12:
                    apnContext = addApnContext("cbs", networkConfig);
                    break;
                case 14:
                    apnContext = addApnContext("ia", networkConfig);
                    break;
                case 15:
                    apnContext = addApnContext("emergency", networkConfig);
                    break;
                case 34:
                    apnContext = addApnContext("dm", networkConfig);
                    break;
                case 35:
                    apnContext = addApnContext("wap", networkConfig);
                    break;
                case 36:
                    apnContext = addApnContext("net", networkConfig);
                    break;
                case 37:
                    apnContext = addApnContext("cmmail", networkConfig);
                    break;
                case 39:
                    apnContext = addApnContext("rcse", networkConfig);
                    break;
                case 40:
                    apnContext = addApnContext("xcap", networkConfig);
                    break;
                case 41:
                    apnContext = addApnContext("rcs", networkConfig);
                    break;
                case 42:
                    apnContext = addApnContext("bip", networkConfig);
                    break;
            }
            log("initApnContexts: apnContext=" + apnContext);
        }
        Collections.sort(this.mPrioritySortedApnContexts, new Comparator<ApnContext>() {
            @Override
            public int compare(ApnContext c1, ApnContext c2) {
                return c2.priority - c1.priority;
            }
        });
        logd("initApnContexts: mPrioritySortedApnContexts=" + this.mPrioritySortedApnContexts);
        if (VDBG) {
            log("initApnContexts: X mApnContexts=" + this.mApnContexts);
        }
    }

    public LinkProperties getLinkProperties(String apnType) {
        DcAsyncChannel dcac;
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null && (dcac = apnContext.getDcAc()) != null) {
            if (DBG) {
                log("return link properites for " + apnType);
            }
            return dcac.getLinkPropertiesSync();
        }
        if (DBG) {
            log("return new LinkProperties");
        }
        return new LinkProperties();
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        DcAsyncChannel dataConnectionAc;
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null && (dataConnectionAc = apnContext.getDcAc()) != null) {
            if (DBG) {
                log("get active pdp is not null, return NetworkCapabilities for " + apnType);
            }
            return dataConnectionAc.getNetworkCapabilitiesSync();
        }
        if (DBG) {
            log("return new NetworkCapabilities");
        }
        return new NetworkCapabilities();
    }

    public String[] getActiveApnTypes() {
        if (DBG) {
            log("get all active apn types");
        }
        ArrayList<String> result = new ArrayList<>();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                result.add(apnContext.getApnType());
            }
        }
        return (String[]) result.toArray(new String[0]);
    }

    public String getActiveApnString(String apnType) {
        ApnSetting apnSetting;
        if (VDBG) {
            logv("get active apn string for type:" + apnType);
        }
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext == null || (apnSetting = apnContext.getApnSetting()) == null) {
            return null;
        }
        return apnSetting.apn;
    }

    public DctConstants.State getState(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.getState();
        }
        return DctConstants.State.FAILED;
    }

    private boolean isProvisioningApn(String apnType) {
        ApnContext apnContext = this.mApnContexts.get(apnType);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    public DctConstants.State getOverallState() {
        boolean isConnecting = false;
        boolean isFailed = true;
        boolean isAnyEnabled = false;
        StringBuilder builder = new StringBuilder();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext != null) {
                builder.append(apnContext.toString()).append(", ");
            }
        }
        logd("overall state is " + ((Object) builder));
        for (ApnContext apnContext2 : this.mApnContexts.values()) {
            if (apnContext2.isEnabled()) {
                isAnyEnabled = true;
                switch (m360xf0fbc33d()[apnContext2.getState().ordinal()]) {
                    case 1:
                    case 3:
                        if (VDBG) {
                            log("overall state is CONNECTED");
                        }
                        return DctConstants.State.CONNECTED;
                    case 2:
                    case 6:
                        isConnecting = true;
                        isFailed = false;
                        break;
                    case 4:
                    default:
                        isAnyEnabled = true;
                        break;
                    case 5:
                    case 7:
                        isFailed = false;
                        break;
                }
            }
        }
        if (!isAnyEnabled) {
            if (VDBG) {
                log("overall state is IDLE");
            }
            return DctConstants.State.IDLE;
        }
        if (isConnecting) {
            if (VDBG) {
                log("overall state is CONNECTING");
            }
            return DctConstants.State.CONNECTING;
        }
        if (!isFailed) {
            if (VDBG) {
                log("overall state is IDLE");
            }
            return DctConstants.State.IDLE;
        }
        if (VDBG) {
            log("overall state is FAILED");
        }
        return DctConstants.State.FAILED;
    }

    public boolean isApnTypeAvailable(String type) {
        if ((type.equals("dun") && fetchDunApn() != null) || type.equals("emergency")) {
            logd("isApnTypeAvaiable, apn: " + type);
            return true;
        }
        if (this.mAllApnSettings != null) {
            for (ApnSetting apn : this.mAllApnSettings) {
                if (apn.canHandleType(type)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean getAnyDataEnabled() {
        if (!isDataEnabled(true)) {
            return false;
        }
        DataAllowFailReason failureReason = new DataAllowFailReason();
        if (!isDataAllowed(failureReason)) {
            if (DBG) {
                log(failureReason.getDataAllowFailReason());
            }
            return false;
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (isDataAllowedForApn(apnContext)) {
                logd("getAnyDataEnabled1 return true, apn=" + apnContext.getApnType());
                return true;
            }
        }
        log("getAnyDataEnabled1 return false");
        return false;
    }

    public boolean getAnyDataEnabled(boolean checkUserDataEnabled) {
        if (!isDataEnabled(checkUserDataEnabled)) {
            return false;
        }
        DataAllowFailReason failureReason = new DataAllowFailReason();
        if (!isDataAllowed(failureReason)) {
            if (DBG) {
                log(failureReason.getDataAllowFailReason());
            }
            return false;
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (isDataAllowedForApn(apnContext)) {
                logd("getAnyDataEnabled2 return true, apn = " + apnContext.getApnType());
                return true;
            }
        }
        log("getAnyDataEnabled2 return false");
        return false;
    }

    private boolean isDataEnabled(boolean checkUserDataEnabled) {
        synchronized (this.mDataEnabledLock) {
            if (checkUserDataEnabled) {
                this.mUserDataEnabled = getDataEnabled();
                if (!((this.mInternalDataEnabled || (checkUserDataEnabled && !this.mUserDataEnabled)) ? false : checkUserDataEnabled ? sPolicyDataEnabled : true)) {
                    return true;
                }
                log("isDataEnabled return false. mInternalDataEnabled = " + this.mInternalDataEnabled + ", checkUserDataEnabled = " + checkUserDataEnabled + ", mUserDataEnabled = " + this.mUserDataEnabled + ", sPolicyDataEnabled = " + sPolicyDataEnabled);
                return false;
            }
            if (this.mInternalDataEnabled) {
                if (!((this.mInternalDataEnabled || (checkUserDataEnabled && !this.mUserDataEnabled)) ? false : checkUserDataEnabled ? sPolicyDataEnabled : true)) {
                }
            }
        }
    }

    private boolean isDataAllowedForApn(ApnContext apnContext) {
        if ((apnContext.getApnType().equals("default") || apnContext.getApnType().equals("ia")) && this.mPhone.getServiceState().getRilDataRadioTechnology() == 18) {
            log("Default data call activation not allowed in iwlan.");
            return false;
        }
        return apnContext.isReady();
    }

    private void onDataConnectionDetached() {
        if (DBG) {
            log("onDataConnectionDetached: stop polling and notify detached");
        }
        stopNetStatPoll();
        stopDataStallAlarm();
        notifyDataConnection(PhoneInternalInterface.REASON_DATA_DETACHED);
        this.mAttached.set(false);
        if (!this.mAutoAttachOnCreationConfig) {
            return;
        }
        this.mAutoAttachOnCreation.set(false);
    }

    private void onDataConnectionAttached() {
        if (DBG) {
            log("onDataConnectionAttached");
        }
        this.mAttached.set(true);
        if (getOverallState() == DctConstants.State.CONNECTED) {
            if (DBG) {
                log("onDataConnectionAttached: start polling notify attached");
            }
            startNetStatPoll();
            startDataStallAlarm(false);
            notifyDataConnection(PhoneInternalInterface.REASON_DATA_ATTACHED);
        } else {
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_DATA_ATTACHED);
        }
        if (this.mAutoAttachOnCreationConfig) {
            this.mAutoAttachOnCreation.set(true);
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_DATA_ATTACHED);
    }

    private boolean isDataAllowed(DataAllowFailReason failureReason) {
        boolean internalDataEnabled;
        synchronized (this.mDataEnabledLock) {
            internalDataEnabled = this.mInternalDataEnabled;
        }
        boolean attachedState = this.mAttached.get();
        boolean desiredPowerState = this.mPhone.getServiceStateTracker().getDesiredPowerState();
        int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
        if (radioTech == 18) {
            desiredPowerState = true;
        }
        IccRecords r = this.mIccRecords.get();
        boolean recordsLoaded = false;
        if (r != null) {
            recordsLoaded = r.getRecordsLoaded();
            if (DBG && !recordsLoaded) {
                log("isDataAllowed getRecordsLoaded=" + recordsLoaded);
            }
        }
        boolean bIsFdnEnabled = isFdnEnabled();
        int dataSub = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean defaultDataSelected = SubscriptionManager.isValidSubscriptionId(dataSub);
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        if (this.mPhone.getCallTracker() != null) {
            this.mPhone.getCallTracker().getState();
        }
        DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();
        if (failureReason != null) {
            failureReason.clearAllReasons();
        }
        if (!(!attachedState ? this.mAutoAttachOnCreation.get() : true)) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.NOT_ATTACHED);
        }
        if (!recordsLoaded) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.RECORD_NOT_LOADED);
        }
        if (!dcHelper.isAllCallingStateIdle() && !dcHelper.isDataSupportConcurrent(this.mPhone.getPhoneId())) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.INVALID_PHONE_STATE);
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }
        if (!internalDataEnabled) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.INTERNAL_DATA_DISABLED);
        }
        if (!defaultDataSelected) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if ((this.mPhone.getServiceState().getDataRoaming() || this.mPhone.getServiceStateTracker().isPsRegStateRoamByUnsol()) && !getDataOnRoamingEnabled()) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.ROAMING_DISABLED);
        }
        if (this.mIsPsRestricted) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.UNDESIRED_POWER_STATE);
        }
        if (bIsFdnEnabled) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.FDN_ENABLED);
        }
        if (!getAllowConfig()) {
            if (failureReason == null) {
                return false;
            }
            failureReason.addDataAllowFailReason(DataAllowFailReasonType.NOT_ALLOWED);
        }
        return failureReason == null || !failureReason.isFailed();
    }

    private boolean isDataAllowedExt(DataAllowFailReason failureReason, String apnType) {
        int nFailReasonSize = failureReason.getSizeOfFailReason();
        boolean allow = false;
        if (failureReason.mDataAllowFailReasonSet.contains(DataAllowFailReasonType.DEFAULT_DATA_UNSELECTED)) {
            if (!ignoreDefaultDataUnselected(apnType)) {
                return false;
            }
            nFailReasonSize--;
        }
        if (failureReason.mDataAllowFailReasonSet.contains(DataAllowFailReasonType.ROAMING_DISABLED)) {
            if (!ignoreDataRoaming(apnType)) {
                return false;
            }
            nFailReasonSize--;
        }
        if (failureReason.mDataAllowFailReasonSet.contains(DataAllowFailReasonType.NOT_ALLOWED)) {
            if (!ignoreDataAllow(apnType)) {
                return false;
            }
            nFailReasonSize--;
        }
        if (nFailReasonSize == 0) {
            allow = true;
        }
        if (VDBG) {
            log("isDataAllowedExt: " + allow);
        }
        return allow;
    }

    private enum RetryFailures {
        ALWAYS,
        ONLY_ON_CHANGE;

        public static RetryFailures[] valuesCustom() {
            return values();
        }
    }

    private void setupDataOnConnectableApns(String reason) {
        setupDataOnConnectableApns(reason, RetryFailures.ALWAYS);
    }

    private void setupDataOnConnectableApns(String reason, RetryFailures retryFailures) {
        ApnContext apnContextIms;
        if (VDBG) {
            log("setupDataOnConnectableApns: " + reason);
        }
        if (DBG && !VDBG) {
            StringBuilder sb = new StringBuilder(120);
            for (ApnContext apnContext : this.mPrioritySortedApnContexts) {
                sb.append(apnContext.getApnType());
                sb.append(":[state=");
                sb.append(apnContext.getState());
                sb.append(",enabled=");
                sb.append(apnContext.isEnabled());
                sb.append("] ");
            }
            log("setupDataOnConnectableApns: " + reason + " " + ((Object) sb));
        }
        ArrayList<ApnContext> aryApnContext = new ArrayList<>();
        String strTempIA = SystemProperties.get("ril.radio.ia-apn");
        for (ApnContext tmpApnContext : this.mPrioritySortedApnContexts) {
            if ((TextUtils.equals(strTempIA, VZW_IMS_NI) && TextUtils.equals(tmpApnContext.getApnType(), ImsSwitchController.IMS_SERVICE)) || (TextUtils.equals(strTempIA, VZW_INTERNET_NI) && TextUtils.equals(tmpApnContext.getApnType(), "default"))) {
                aryApnContext.add(0, tmpApnContext);
            } else {
                aryApnContext.add(tmpApnContext);
            }
        }
        for (ApnContext apnContext2 : aryApnContext) {
            ArrayList<ApnSetting> waitingApns = null;
            if (ImsSwitchController.IMS_SERVICE.equals(apnContext2.getApnType()) || "emergency".equals(apnContext2.getApnType())) {
                logv("setupDataOnConnectableApns: ignore apnContext " + apnContext2);
            } else {
                if (VDBG) {
                    logv("setupDataOnConnectableApns: apnContext " + apnContext2);
                }
                if (apnContext2.getState() != DctConstants.State.SCANNING || this.mDcFcMgr == null || !this.mDcFcMgr.canIgnoredReason(reason)) {
                    if (apnContext2.getState() == DctConstants.State.FAILED || apnContext2.getState() == DctConstants.State.SCANNING) {
                        if (retryFailures == RetryFailures.ALWAYS) {
                            apnContext2.releaseDataConnection(reason);
                        } else if (!apnContext2.isConcurrentVoiceAndDataAllowed() && this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                            apnContext2.releaseDataConnection(reason);
                        } else {
                            int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                            ArrayList<ApnSetting> originalApns = apnContext2.getWaitingApns();
                            if (originalApns != null && !originalApns.isEmpty()) {
                                waitingApns = buildWaitingApns(apnContext2.getApnType(), radioTech);
                                if (originalApns.size() != waitingApns.size() || !originalApns.containsAll(waitingApns)) {
                                    apnContext2.releaseDataConnection(reason);
                                }
                            }
                        }
                    }
                    if (TextUtils.equals(apnContext2.getApnType(), "default") && TextUtils.equals(strTempIA, VZW_IMS_NI) && (apnContextIms = this.mApnContexts.get(ImsSwitchController.IMS_SERVICE)) != null && !apnContextIms.isEnabled() && !TextUtils.equals(reason, PhoneInternalInterface.REASON_DATA_ATTACHED) && !TextUtils.equals(reason, PhoneInternalInterface.REASON_DATA_ENABLED) && !TextUtils.equals(reason, PhoneInternalInterface.REASON_APN_CHANGED) && !TextUtils.equals(reason, PhoneInternalInterface.REASON_VOICE_CALL_ENDED) && !TextUtils.equals(reason, PhoneInternalInterface.REASON_SIM_LOADED)) {
                        log("setupDataOnConnectableApns: ignore default pdn setup");
                    } else if (apnContext2.isConnectable()) {
                        log("setupDataOnConnectableApns: isConnectable() call trySetupData");
                        apnContext2.setReason(reason);
                        trySetupData(apnContext2, waitingApns);
                    }
                }
            }
        }
    }

    boolean isEmergency() {
        boolean zIsInEmergencyCall;
        synchronized (this.mDataEnabledLock) {
            zIsInEmergencyCall = !this.mPhone.isInEcm() ? this.mPhone.isInEmergencyCall() : true;
        }
        log("isEmergency: result=" + zIsInEmergencyCall);
        return zIsInEmergencyCall;
    }

    private boolean trySetupData(ApnContext apnContext) {
        return trySetupData(apnContext, null);
    }

    private boolean trySetupData(ApnContext apnContext, ArrayList<ApnSetting> waitingApns) {
        if (DBG) {
            log("trySetupData for type:" + apnContext.getApnType() + " due to " + apnContext.getReason() + ", mIsPsRestricted=" + this.mIsPsRestricted);
        }
        apnContext.requestLog("trySetupData due to " + apnContext.getReason());
        if (this.mPhone.getSimulatedRadioControl() != null) {
            apnContext.setState(DctConstants.State.CONNECTED);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return true;
        }
        boolean isEmergencyApn = apnContext.getApnType().equals("emergency");
        ServiceStateTracker sst = this.mPhone.getServiceStateTracker();
        boolean checkUserDataEnabled = ApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone.getContext(), this.mPhone.getSubId(), this.mPhone.getServiceState().getDataRoaming()) && !isDataAllowedAsOff(apnContext.getApnType());
        DataAllowFailReason failureReason = new DataAllowFailReason();
        boolean zIsDataAllowedExt = (isDataAllowed(failureReason) || (failureReason.isFailForSingleReason(DataAllowFailReasonType.ROAMING_DISABLED) && !ApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone.getContext(), this.mPhone.getSubId(), this.mPhone.getServiceState().getDataRoaming()))) ? true : isDataAllowedExt(failureReason, apnContext.getApnType());
        if (!apnContext.isConnectable() || (!(isEmergencyApn || (zIsDataAllowedExt && isDataAllowedForApn(apnContext) && isDataEnabled(checkUserDataEnabled) && !isEmergency())) || this.mColdSimDetected)) {
            if (!apnContext.getApnType().equals("default") && apnContext.isConnectable()) {
                if (apnContext.getApnType().equals("mms") && TelephonyManager.getDefault().isMultiSimEnabled() && !this.mAttached.get()) {
                    log("Wait for attach");
                    if (apnContext.getState() != DctConstants.State.IDLE) {
                        return true;
                    }
                    apnContext.setState(DctConstants.State.RETRYING);
                    return true;
                }
                this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
            }
            notifyOffApnsOfAvailability(apnContext.getReason());
            StringBuilder str = new StringBuilder();
            str.append("trySetupData failed. apnContext = [type=").append(apnContext.getApnType()).append(", mState=").append(apnContext.getState()).append(", mDataEnabled=").append(apnContext.isEnabled()).append(", mDependencyMet=").append(apnContext.getDependencyMet()).append("] ");
            if (!apnContext.isConnectable()) {
                str.append("isConnectable = false. ");
            }
            if (!zIsDataAllowedExt) {
                str.append("data not allowed: ").append(failureReason.getDataAllowFailReason()).append(". ");
            }
            if (!isDataAllowedForApn(apnContext)) {
                str.append("isDataAllowedForApn = false. RAT = ").append(this.mPhone.getServiceState().getRilDataRadioTechnology());
            }
            if (!isDataEnabled(checkUserDataEnabled)) {
                str.append("isDataEnabled(").append(checkUserDataEnabled).append(") = false. ").append("mInternalDataEnabled = ").append(this.mInternalDataEnabled).append(" , mUserDataEnabled = ").append(this.mUserDataEnabled).append(", sPolicyDataEnabled = ").append(sPolicyDataEnabled).append(" ");
            }
            if (isEmergency()) {
                str.append("emergency = true");
            }
            if (this.mColdSimDetected) {
                str.append("coldSimDetected = true");
            }
            if (DBG) {
                log(str.toString());
            }
            apnContext.requestLog(str.toString());
            return false;
        }
        if (apnContext.getState() == DctConstants.State.FAILED) {
            if (DBG) {
                log("trySetupData: make a FAILED ApnContext IDLE so its reusable");
            }
            apnContext.requestLog("trySetupData: make a FAILED ApnContext IDLE so its reusable");
            apnContext.setState(DctConstants.State.IDLE);
        }
        int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
        apnContext.setConcurrentVoiceAndDataAllowed(sst.isConcurrentVoiceAndDataAllowed());
        if (apnContext.getState() == DctConstants.State.IDLE || (apnContext.getApnType().equals("mms") && apnContext.getState() == DctConstants.State.RETRYING)) {
            if (waitingApns == null) {
                if (TextUtils.equals(apnContext.getApnType(), "emergency")) {
                    if (this.mAllApnSettings == null) {
                        logi("mAllApnSettings is null, create first and add emergency one");
                        createAllApnList();
                    } else if (this.mAllApnSettings.isEmpty()) {
                        logi("add mEmergencyApn: " + this.mEmergencyApn + " to mAllApnSettings");
                        addEmergencyApnSetting();
                    }
                }
                waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
            }
            if (waitingApns.isEmpty()) {
                notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                notifyOffApnsOfAvailability(apnContext.getReason());
                if (DBG) {
                    log("trySetupData: X No APN found retValue=false");
                }
                apnContext.requestLog("trySetupData: X No APN found retValue=false");
                return false;
            }
            apnContext.setWaitingApns(waitingApns);
            apnContext.setWifiApns(buildWifiApns(apnContext.getApnType()));
            if (DBG) {
                log("trySetupData: Create from mAllApnSettings : " + apnListToString(this.mAllApnSettings));
            }
        }
        logd("trySetupData: call setupData, waitingApns : " + apnListToString(apnContext.getWaitingApns()) + ", wifiApns : " + apnListToString(apnContext.getWifiApns()));
        boolean retValue = setupData(apnContext, radioTech);
        notifyOffApnsOfAvailability(apnContext.getReason());
        if (DBG) {
            log("trySetupData: X retValue=" + retValue);
        }
        return retValue;
    }

    private void notifyOffApnsOfAvailability(String reason) {
        if (DBG) {
            DataAllowFailReason failureReason = new DataAllowFailReason();
            if (!isDataAllowed(failureReason)) {
                log(failureReason.getDataAllowFailReason());
            }
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if ((!this.mAttached.get() || !apnContext.isReady()) && apnContext.isNeedNotify()) {
                String apnType = apnContext.getApnType();
                if (VDBG) {
                    logv("notifyOffApnOfAvailability type:" + apnType + " reason: " + reason);
                }
                this.mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(), apnType, PhoneConstants.DataState.DISCONNECTED);
            } else if (VDBG) {
                logv("notifyOffApnsOfAvailability skipped apn due to attached && isReady " + apnContext.toString());
            }
        }
    }

    private boolean cleanUpAllConnections(boolean tearDown, String reason) {
        if (DBG) {
            log("cleanUpAllConnections: tearDown=" + tearDown + " reason=" + reason);
        }
        boolean didDisconnect = false;
        boolean specificDisable = false;
        if (!TextUtils.isEmpty(reason)) {
            boolean specificDisable2 = !reason.equals(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED) ? reason.equals(PhoneInternalInterface.REASON_ROAMING_ON) : true;
            boolean specificDisable3 = !specificDisable2 ? reason.equals(PhoneInternalInterface.REASON_RADIO_TURNED_OFF) : true;
            specificDisable = !specificDisable3 ? reason.equals(PhoneInternalInterface.REASON_PDP_RESET) : true;
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                didDisconnect = true;
            }
            if (specificDisable) {
                ApnSetting apnSetting = apnContext.getApnSetting();
                if (apnSetting != null && apnSetting.isMetered(this.mPhone.getContext(), this.mPhone.getSubId(), this.mPhone.getServiceState().getDataRoaming())) {
                    if (DBG) {
                        log("clean up metered ApnContext Type: " + apnContext.getApnType());
                    }
                    apnContext.setReason(reason);
                    cleanUpConnection(tearDown, apnContext);
                }
            } else if (reason != null && reason.equals(PhoneInternalInterface.REASON_ROAMING_ON) && ignoreDataRoaming(apnContext.getApnType())) {
                log("cleanUpConnection: Ignore Data Roaming for apnType = " + apnContext.getApnType());
            } else {
                apnContext.setReason(reason);
                cleanUpConnection(tearDown, apnContext);
            }
        }
        stopNetStatPoll();
        stopDataStallAlarm();
        this.mRequestedApnType = "default";
        log("cleanUpConnection: mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        if (tearDown && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
        return didDisconnect;
    }

    private void onCleanUpAllConnections(String cause) {
        cleanUpAllConnections(true, cause);
    }

    void sendCleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (DBG) {
            log("sendCleanUpConnection: tearDown=" + tearDown + " apnContext=" + apnContext);
        }
        Message msg = obtainMessage(270360);
        msg.arg1 = tearDown ? 1 : 0;
        msg.arg2 = 0;
        msg.obj = apnContext;
        sendMessage(msg);
    }

    private void cleanUpConnection(boolean tearDown, ApnContext apnContext) {
        if (apnContext == null) {
            if (DBG) {
                log("cleanUpConnection: apn context is null");
                return;
            }
            return;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        String str = "cleanUpConnection: tearDown=" + tearDown + " reason=" + apnContext.getReason();
        if (VDBG) {
            log(str + " apnContext=" + apnContext);
        }
        apnContext.requestLog(str);
        if (tearDown) {
            if (apnContext.isDisconnected()) {
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcac != null) {
                        if (DBG) {
                            log("cleanUpConnection: teardown, disconnected, !ready apnContext=" + apnContext);
                        }
                        apnContext.requestLog("cleanUpConnection: teardown, disconnected, !ready");
                        dcac.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                }
            } else if (dcac != null) {
                if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                    boolean disconnectAll = false;
                    if ("dun".equals(apnContext.getApnType()) && teardownForDun()) {
                        if (DBG) {
                            log("cleanUpConnection: disconnectAll DUN connection");
                        }
                        disconnectAll = true;
                    }
                    int generation = apnContext.getConnectionGeneration();
                    String str2 = "cleanUpConnection: tearing down" + (disconnectAll ? " all" : "") + " using gen#" + generation;
                    if (DBG) {
                        log(str2 + "apnContext=" + apnContext);
                    }
                    apnContext.requestLog(str2);
                    Pair<ApnContext, Integer> pair = new Pair<>(apnContext, Integer.valueOf(generation));
                    Message msg = obtainMessage(270351, pair);
                    if (disconnectAll) {
                        apnContext.getDcAc().tearDownAll(apnContext.getReason(), msg);
                    } else {
                        apnContext.getDcAc().tearDown(apnContext, apnContext.getReason(), msg);
                    }
                    apnContext.setState(DctConstants.State.DISCONNECTING);
                    this.mDisconnectPendingCount++;
                }
            } else {
                apnContext.setState(DctConstants.State.IDLE);
                apnContext.requestLog("cleanUpConnection: connected, bug no DCAC");
                if (apnContext.isNeedNotify()) {
                    this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                }
            }
        } else {
            boolean needNotify = true;
            int phoneCount = TelephonyManager.getDefault().getPhoneCount();
            if (apnContext.isDisconnected() && phoneCount > 2) {
                needNotify = false;
            }
            if (dcac != null) {
                dcac.reqReset();
            }
            apnContext.setState(DctConstants.State.IDLE);
            if (apnContext.isNeedNotify() && needNotify) {
                this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
            apnContext.setDataConnectionAc(null);
        }
        if (dcac != null) {
            cancelReconnectAlarm(apnContext);
        }
        String str3 = "cleanUpConnection: X tearDown=" + tearDown + " reason=" + apnContext.getReason();
        if (DBG && apnContext.isNeedNotify()) {
            log(str3 + " apnContext=" + apnContext + " dcac=" + apnContext.getDcAc());
        }
        apnContext.requestLog(str3);
    }

    ApnSetting fetchDunApn() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            log("fetchDunApn: net.tethering.noprovisioning=true ret: null");
            return null;
        }
        int bearer = this.mPhone.getServiceState().getRilDataRadioTechnology();
        ApnSetting retDunSetting = null;
        String apnData = Settings.Global.getString(this.mResolver, "tether_dun_apn");
        List<ApnSetting> dunSettings = ApnSetting.arrayFromString(apnData);
        IccRecords r = this.mIccRecords.get();
        for (ApnSetting dunSetting : dunSettings) {
            String operator = r != null ? r.getOperatorNumeric() : "";
            if (ServiceState.bitmaskHasTech(dunSetting.bearerBitmask, bearer) && dunSetting.numeric.equals(operator)) {
                if (dunSetting.hasMvnoParams()) {
                    if (r != null && ApnSetting.mvnoMatches(r, dunSetting.mvnoType, dunSetting.mvnoMatchData)) {
                        if (VDBG) {
                            log("fetchDunApn: global TETHER_DUN_APN dunSetting=" + dunSetting);
                        }
                        return dunSetting;
                    }
                } else if (!this.mMvnoMatched) {
                    if (VDBG) {
                        log("fetchDunApn: global TETHER_DUN_APN dunSetting=" + dunSetting);
                    }
                    return dunSetting;
                }
            }
        }
        Context c = this.mPhone.getContext();
        String[] apnArrayData = getDunApnByMccMnc(c);
        for (String apn : apnArrayData) {
            ApnSetting dunSetting2 = ApnSetting.fromString(apn);
            if (dunSetting2 != null && ServiceState.bitmaskHasTech(dunSetting2.bearerBitmask, bearer)) {
                if (dunSetting2.hasMvnoParams()) {
                    if (r != null && ApnSetting.mvnoMatches(r, dunSetting2.mvnoType, dunSetting2.mvnoMatchData)) {
                        if (VDBG) {
                            log("fetchDunApn: config_tether_apndata mvno dunSetting=" + dunSetting2);
                        }
                        return dunSetting2;
                    }
                } else if (!this.mMvnoMatched) {
                    retDunSetting = dunSetting2;
                }
            }
        }
        if (VDBG) {
            log("fetchDunApn: config_tether_apndata dunSetting=" + retDunSetting);
        }
        return retDunSetting;
    }

    public boolean hasMatchedTetherApnSetting() {
        ApnSetting matched = fetchDunApn();
        log("hasMatchedTetherApnSetting: APN=" + matched);
        return matched != null;
    }

    private String[] getDunApnByMccMnc(Context context) {
        IccRecords r = this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        int mcc = 0;
        int mnc = 0;
        if (operator != null && operator.length() > 3) {
            mcc = Integer.parseInt(operator.substring(0, 3));
            mnc = Integer.parseInt(operator.substring(3, operator.length()));
        }
        Resources sysResource = context.getResources();
        int sysMcc = sysResource.getConfiguration().mcc;
        int sysMnc = sysResource.getConfiguration().mnc;
        logd("fetchDunApn: Resource mccmnc=" + sysMcc + "," + sysMnc + "; OperatorNumeric mccmnc=" + mcc + "," + mnc);
        Resources resource = null;
        try {
            new Configuration();
            Configuration configuration = context.getResources().getConfiguration();
            configuration.mcc = mcc;
            configuration.mnc = mnc;
            Context resc = context.createConfigurationContext(configuration);
            resource = resc.getResources();
        } catch (Exception e) {
            e.printStackTrace();
            loge("getResourcesUsingMccMnc fail");
        }
        if (TelephonyManager.getDefault().getSimCount() == 1 || ((mcc == sysMcc && mnc == sysMnc) || resource == null)) {
            return sysResource.getStringArray(R.array.config_autoKeyboardBacklightIncreaseLuxThreshold);
        }
        logd("fetchDunApn: get resource from mcc=" + mcc + ", mnc=" + mnc);
        return resource.getStringArray(R.array.config_autoKeyboardBacklightIncreaseLuxThreshold);
    }

    private boolean teardownForDun() {
        int rilRat = this.mPhone.getServiceState().getRilDataRadioTechnology();
        return ServiceState.isCdma(rilRat) || fetchDunApn() != null;
    }

    private void cancelReconnectAlarm(ApnContext apnContext) {
        PendingIntent intent;
        if (apnContext == null || (intent = apnContext.getReconnectIntent()) == null) {
            return;
        }
        AlarmManager am = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        am.cancel(intent);
        apnContext.setReconnectIntent(null);
    }

    private String[] parseTypes(String types) {
        if (types == null || types.equals("")) {
            String[] result = {CharacterSets.MIMENAME_ANY_CHARSET};
            return result;
        }
        return types.split(",");
    }

    boolean isPermanentFail(DcFailCause dcFailCause) {
        boolean z = true;
        if (129 != DataConnectionHelper.getInstance().getSbpIdFromNetworkOperator(this.mPhone.getPhoneId())) {
            if (dcFailCause.isPermanentFail() && isPermanentFailByOp(dcFailCause, DcFailCauseManager.Operator.OP19)) {
                return (this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST) ? false : true;
            }
            return false;
        }
        if (!dcFailCause.isPermanentFail() && dcFailCause != DcFailCause.TCM_ESM_TIMER_TIMEOUT) {
            return false;
        }
        if (this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST) {
            z = false;
        }
        return z;
    }

    private ApnSetting makeApnSetting(Cursor cursor) {
        int inactiveTimer = 0;
        try {
            inactiveTimer = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.INACTIVE_TIMER));
        } catch (IllegalArgumentException e) {
            loge("makeApnSetting: parsing inactive timer failed. " + e);
        }
        String[] types = parseTypes(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        ApnSetting apn = new ApnSetting(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)), cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT)), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT)), cursor.getString(cursor.getColumnIndexOrThrow("user")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)), types, cursor.getString(cursor.getColumnIndexOrThrow("protocol")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.ROAMING_PROTOCOL)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.CARRIER_ENABLED)) == 1, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.BEARER_BITMASK)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MODEM_COGNITIVE)) == 1, cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.WAIT_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS_TIME)), cursor.getInt(cursor.getColumnIndexOrThrow("mtu")), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_TYPE)), cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.MVNO_MATCH_DATA)), inactiveTimer);
        return apn;
    }

    private ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> result;
        ArrayList<ApnSetting> mnoApns = new ArrayList<>();
        ArrayList<ApnSetting> mvnoApns = new ArrayList<>();
        IccRecords r = this.mIccRecords.get();
        if (cursor.moveToFirst()) {
            do {
                ApnSetting apn = makeApnSetting(cursor);
                if (apn != null) {
                    if (apn.hasMvnoParams()) {
                        if (r != null && ApnSetting.mvnoMatches(r, apn.mvnoType, apn.mvnoMatchData)) {
                            mvnoApns.add(apn);
                        }
                    } else {
                        mnoApns.add(apn);
                    }
                }
            } while (cursor.moveToNext());
        }
        if (mvnoApns.isEmpty()) {
            result = mnoApns;
            this.mMvnoMatched = false;
        } else {
            result = mvnoApns;
            this.mMvnoMatched = true;
        }
        if (DBG) {
            log("createApnList: X result=" + result);
        }
        return result;
    }

    private boolean dataConnectionNotInUse(DcAsyncChannel dcac) {
        if (DBG) {
            log("dataConnectionNotInUse: check if dcac is inuse dcac=" + dcac);
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getDcAc() == dcac) {
                if (DBG) {
                    log("dataConnectionNotInUse: in use by apnContext=" + apnContext);
                    return false;
                }
                return false;
            }
        }
        if (DBG) {
            log("dataConnectionNotInUse: not in use return true");
            return true;
        }
        return true;
    }

    private DcAsyncChannel findFreeDataConnection(String reqApnType, ApnSetting apnSetting) {
        int id;
        int id2;
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.isInactiveSync() && dataConnectionNotInUse(dcac)) {
                if (isSupportThrottlingApn()) {
                    for (String apn : HIGH_THROUGHPUT_APN) {
                        if (apnSetting != null && apnSetting.canHandleType(apn) && !"emergency".equals(reqApnType) && !apnSetting.canHandleType(ImsSwitchController.IMS_SERVICE) && dcac != null && ((id2 = dcac.getDataConnectionIdSync()) < 0 || id2 > 1)) {
                            dcac = null;
                        }
                    }
                    if (Arrays.asList(IMS_APN).indexOf(reqApnType) > -1 && apnSetting != null && apnSetting.canHandleType(reqApnType) && dcac != null) {
                        int id3 = dcac.getDataConnectionIdSync();
                        logi("Data connection's interface is: " + id3);
                        if ((id3 == 4 && ImsSwitchController.IMS_SERVICE.equals(reqApnType)) || (id3 == 5 && "emergency".equals(reqApnType))) {
                            logd("findFreeDataConnection: find connection to handle: " + reqApnType);
                        } else {
                            dcac = null;
                        }
                    }
                    if (!"emergency".equals(reqApnType) && !ImsSwitchController.IMS_SERVICE.equals(reqApnType) && dcac != null && (id = dcac.getDataConnectionIdSync()) >= 4 && id <= 6) {
                        log("findFreeDataConnection: free dcac for non-IMS APN");
                        dcac = null;
                    }
                }
                if (dcac != null) {
                    log("findFreeDataConnection: found free DataConnection= dcac=" + dcac);
                    return dcac;
                }
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    private boolean setupData(ApnContext apnContext, int radioTech) {
        ApnSetting dcacApnSetting;
        DcAsyncChannel prevDcac;
        if (DBG) {
            log("setupData: apnContext=" + apnContext);
        }
        apnContext.requestLog("setupData");
        DcAsyncChannel dcac = null;
        ApnSetting apnSetting = apnContext.getNextApnSetting();
        if (apnSetting == null && !apnContext.getApnType().equals("emergency")) {
            log("setupData: return for no apn found!");
            return false;
        }
        int i = apnSetting.profileId;
        int profileId = getApnProfileID(apnContext.getApnType());
        if ((apnContext.getApnType() != "dun" || !teardownForDun()) && (dcac = checkForCompatibleConnectedApnContext(apnContext)) != null && (dcacApnSetting = dcac.getApnSettingSync()) != null) {
            apnSetting = dcacApnSetting;
        }
        if (dcac == null) {
            if (isOnlySingleDcAllowed(radioTech)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    if (DBG) {
                        log("setupData: Higher priority ApnContext active.  Ignoring call");
                        return false;
                    }
                    return false;
                }
                if (cleanUpAllConnections(true, PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION)) {
                    if (DBG) {
                        log("setupData: Some calls are disconnecting first.  Wait and retry");
                        return false;
                    }
                    return false;
                }
                if (DBG) {
                    log("setupData: Single pdp. Continue setting up data call.");
                }
            }
            if (!isSupportThrottlingApn() && !isOnlySingleDcAllowed(radioTech)) {
                boolean isHighThroughputApn = false;
                String[] strArr = HIGH_THROUGHPUT_APN;
                int i2 = 0;
                int length = strArr.length;
                while (true) {
                    if (i2 >= length) {
                        break;
                    }
                    String apn = strArr[i2];
                    if (!apnSetting.canHandleType(apn)) {
                        i2++;
                    } else {
                        isHighThroughputApn = true;
                        break;
                    }
                }
                if (!isHighThroughputApn) {
                    boolean lastDcAlreadyInUse = false;
                    for (DcAsyncChannel asyncChannel : this.mDataConnectionAcHashMap.values()) {
                        if (asyncChannel.getDataConnectionIdSync() == getPdpConnectionPoolSize()) {
                            if (asyncChannel.isInactiveSync() && dataConnectionNotInUse(asyncChannel)) {
                                logd("setupData: find the last dc for non-high-throughput apn, execute tearDownAll to the dc");
                                dcac = asyncChannel;
                                asyncChannel.tearDownAll("No connection", null);
                            } else {
                                log("setupData: the last data connection is already in-use");
                                lastDcAlreadyInUse = true;
                            }
                        }
                    }
                    if (dcac == null && !lastDcAlreadyInUse) {
                        DataConnection conn = DataConnection.makeDataConnection(this.mPhone, getPdpConnectionPoolSize(), this, this.mDcTesterFailBringUpAll, this.mDcc);
                        this.mDataConnections.put(Integer.valueOf(getPdpConnectionPoolSize()), conn);
                        dcac = new DcAsyncChannel(conn, LOG_TAG);
                        int status = dcac.fullyConnectSync(this.mPhone.getContext(), this, conn.getHandler());
                        if (status == 0) {
                            logd("setupData: create the last data connection");
                            this.mDataConnectionAcHashMap.put(Integer.valueOf(dcac.getDataConnectionIdSync()), dcac);
                        } else {
                            loge("setupData: createDataConnection (last) could not connect to dcac=" + dcac + " status=" + status);
                        }
                    }
                }
            }
            if (dcac == null) {
                log("setupData: No ready DataConnection found!");
                dcac = findFreeDataConnection(apnContext.getApnType(), apnSetting);
            }
            if (dcac == null && ((apnContext.getApnType() == "default" || apnContext.getApnType() == "mms") && (prevDcac = apnContext.getDcAc()) != null && prevDcac.isInactiveSync())) {
                dcac = prevDcac;
                ApnSetting dcacApnSetting2 = prevDcac.getApnSettingSync();
                log("setupData: reuse previous DCAC: dcacApnSetting = " + dcacApnSetting2);
                if (dcacApnSetting2 != null) {
                    apnSetting = dcacApnSetting2;
                }
            }
            if (dcac == null) {
                dcac = createDataConnection(apnContext.getApnType(), apnSetting);
            }
            if (dcac == null) {
                if (DBG) {
                    log("setupData: No free DataConnection and couldn't create one, WEIRD");
                    return false;
                }
                return false;
            }
        }
        int generation = apnContext.incAndGetConnectionGeneration();
        if (DBG) {
            log("setupData: dcac=" + dcac + " apnSetting=" + apnSetting + " gen#=" + generation);
        }
        apnContext.setDataConnectionAc(dcac);
        apnContext.setApnSetting(apnSetting);
        apnContext.setState(DctConstants.State.CONNECTING);
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        Message msg = obtainMessage();
        msg.what = 270336;
        msg.obj = new Pair(apnContext, Integer.valueOf(generation));
        dcac.bringUp(apnContext, profileId, radioTech, msg, generation);
        if (DBG) {
            log("setupData: initing!");
            return true;
        }
        return true;
    }

    private void onMdChangedAttachApn(AsyncResult ar) {
        logv("onMdChangedAttachApn");
        int[] ints = (int[]) ar.result;
        int apnId = ints[0];
        if (apnId != 1 && apnId != 3) {
            logw("onMdChangedAttachApn: Not handle APN Class:" + apnId);
            return;
        }
        int phoneId = this.mPhone.getPhoneId();
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            String iccId = SystemProperties.get(this.PROPERTY_ICCID[phoneId], "");
            SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, iccId);
            SystemProperties.set(PROP_APN_CLASS + phoneId, String.valueOf(apnId));
            log("onMdChangedAttachApn, set " + iccId + ", " + apnId);
        }
        updateMdChangedAttachApn(apnId);
        if (this.mMdChangedAttachApn != null) {
            setInitialAttachApn();
        } else {
            logw("onMdChangedAttachApn: MdChangedAttachApn is null, not found APN");
        }
    }

    private void updateMdChangedAttachApn(int apnId) {
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            return;
        }
        for (ApnSetting apn : this.mAllApnSettings) {
            if (apnId == 1 && ArrayUtils.contains(apn.types, ImsSwitchController.IMS_SERVICE)) {
                this.mMdChangedAttachApn = apn;
                log("updateMdChangedAttachApn: MdChangedAttachApn=" + apn);
                return;
            } else if (apnId == 3 && ArrayUtils.contains(apn.types, "default")) {
                this.mMdChangedAttachApn = apn;
                log("updateMdChangedAttachApn: MdChangedAttachApn=" + apn);
                return;
            }
        }
    }

    private boolean isMdChangedAttachApnEnabled() {
        if (this.mMdChangedAttachApn != null && this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            for (ApnSetting apn : this.mAllApnSettings) {
                if (TextUtils.equals(this.mMdChangedAttachApn.apn, apn.apn)) {
                    log("isMdChangedAttachApnEnabled: " + apn);
                    return apn.carrierEnabled;
                }
            }
            return false;
        }
        return false;
    }

    private void setInitialAttachApn() {
        int apnClass;
        boolean needsResumeModem = false;
        boolean isIaApn = false;
        ApnSetting apnSetting = this.mInitialAttachApnSetting;
        IccRecords r = this.mIccRecords.get();
        String operatorNumeric = r != null ? r.getOperatorNumeric() : "";
        if (operatorNumeric == null || operatorNumeric.length() == 0) {
            log("setInitialApn: but no operator numeric");
            return;
        }
        synchronized (this.mNeedsResumeModemLock) {
            if (this.mNeedsResumeModem) {
                this.mNeedsResumeModem = false;
                needsResumeModem = true;
            }
        }
        String currentMcc = operatorNumeric.substring(0, 3);
        log("setInitialApn: currentMcc = " + currentMcc + ", needsResumeModem = " + needsResumeModem);
        String[] dualApnPlmnList = null;
        if (MTK_DUAL_APN_SUPPORT) {
            dualApnPlmnList = this.mPhone.getContext().getResources().getStringArray(134479875);
        }
        log("setInitialApn: current attach Apn [" + this.mInitialAttachApnSetting + "]");
        ApnSetting iaApnSetting = null;
        ApnSetting defaultApnSetting = null;
        ApnSetting firstApnSetting = null;
        ApnSetting manualChangedAttachApn = null;
        log("setInitialApn: E mPreferredApn=" + this.mPreferredApn);
        if ((this.mIsImsHandover || MTK_IMS_TESTMODE_SUPPORT) && (manualChangedAttachApn = getClassTypeApn(3)) != null) {
            log("setInitialAttachApn: manualChangedAttachApn = " + manualChangedAttachApn);
        }
        if (this.mMdChangedAttachApn == null) {
            int phoneId = this.mPhone.getPhoneId();
            if (SubscriptionManager.isValidPhoneId(phoneId) && (apnClass = SystemProperties.getInt(PROP_APN_CLASS + phoneId, -1)) >= 0) {
                String iccId = SystemProperties.get(this.PROPERTY_ICCID[phoneId], "");
                String apnClassIccId = SystemProperties.get(PROP_APN_CLASS_ICCID + phoneId, "");
                log("setInitialAttachApn: " + iccId + " , " + apnClassIccId + ", " + apnClass);
                if (TextUtils.equals(iccId, apnClassIccId)) {
                    updateMdChangedAttachApn(apnClass);
                } else {
                    SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, "");
                    SystemProperties.set(PROP_APN_CLASS + phoneId, "");
                }
            }
        }
        ApnSetting mdChangedAttachApn = this.mMdChangedAttachApn;
        if (this.mMdChangedAttachApn != null && getClassType(this.mMdChangedAttachApn) == 1 && !isMdChangedAttachApnEnabled()) {
            mdChangedAttachApn = null;
        }
        if (mdChangedAttachApn == null && manualChangedAttachApn == null && this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            firstApnSetting = this.mAllApnSettings.get(0);
            log("setInitialApn: firstApnSetting=" + firstApnSetting);
            Iterator apn$iterator = this.mAllApnSettings.iterator();
            while (true) {
                if (!apn$iterator.hasNext()) {
                    break;
                }
                ApnSetting apn = (ApnSetting) apn$iterator.next();
                if (ArrayUtils.contains(apn.types, "ia") && apn.carrierEnabled && checkIfDomesticInitialAttachApn(currentMcc)) {
                    log("setInitialApn: iaApnSetting=" + apn);
                    iaApnSetting = apn;
                    if (ArrayUtils.contains(this.PLMN_EMPTY_APN_PCSCF_SET, operatorNumeric)) {
                        isIaApn = true;
                    }
                } else if (defaultApnSetting == null && apn.canHandleType("default")) {
                    log("setInitialApn: defaultApnSetting=" + apn);
                    defaultApnSetting = apn;
                }
            }
        }
        this.mInitialAttachApnSetting = null;
        if (manualChangedAttachApn != null) {
            log("setInitialAttachApn: using manualChangedAttachApn");
            this.mInitialAttachApnSetting = manualChangedAttachApn;
        } else if (mdChangedAttachApn != null) {
            log("setInitialAttachApn: using mMdChangedAttachApn");
            this.mInitialAttachApnSetting = mdChangedAttachApn;
        } else if (iaApnSetting != null) {
            if (DBG) {
                log("setInitialAttachApn: using iaApnSetting");
            }
            this.mInitialAttachApnSetting = iaApnSetting;
        } else if (this.mPreferredApn != null) {
            if (DBG) {
                log("setInitialAttachApn: using mPreferredApn");
            }
            this.mInitialAttachApnSetting = this.mPreferredApn;
        } else if (defaultApnSetting != null) {
            if (DBG) {
                log("setInitialAttachApn: using defaultApnSetting");
            }
            this.mInitialAttachApnSetting = defaultApnSetting;
        } else if (firstApnSetting != null) {
            if (DBG) {
                log("setInitialAttachApn: using firstApnSetting");
            }
            this.mInitialAttachApnSetting = firstApnSetting;
        }
        if (this.mInitialAttachApnSetting == null) {
            if (DBG) {
                log("setInitialAttachApn: X There in no available apn, use empty");
            }
            IaExtendParam param = new IaExtendParam(operatorNumeric, dualApnPlmnList, VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL);
            this.mPhone.mCi.setInitialAttachApn("", VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL, -1, "", "", param, null);
        } else {
            if (DBG) {
                log("setInitialAttachApn: X selected Apn=" + this.mInitialAttachApnSetting);
            }
            String iaApn = this.mInitialAttachApnSetting.apn;
            if (isIaApn) {
                if (DBG) {
                    log("setInitialAttachApn: ESM flag false, change IA APN to empty");
                }
                iaApn = "";
            }
            Message msg = null;
            if (needsResumeModem) {
                if (DBG) {
                    log("setInitialAttachApn: DCM IA support");
                }
                msg = obtainMessage(270850);
            }
            IaExtendParam param2 = new IaExtendParam(operatorNumeric, this.mInitialAttachApnSetting.canHandleType(ImsSwitchController.IMS_SERVICE), dualApnPlmnList, this.mInitialAttachApnSetting.roamingProtocol);
            this.mPhone.mCi.setInitialAttachApn(iaApn, this.mInitialAttachApnSetting.protocol, this.mInitialAttachApnSetting.authType, this.mInitialAttachApnSetting.user, this.mInitialAttachApnSetting.password, param2, msg);
        }
        if (DBG) {
            log("setInitialAttachApn: new attach Apn [" + this.mInitialAttachApnSetting + "]");
        }
    }

    private void onApnChanged() {
        if (this.mPhone instanceof GsmCdmaPhone) {
            ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
        }
        ArrayList<ApnSetting> prevAllApns = this.mAllApnSettings;
        ApnSetting prevPreferredApn = this.mPreferredApn;
        if (DBG) {
            log("onApnChanged: createAllApnList and set initial attach APN");
        }
        createAllApnList();
        ApnSetting previousAttachApn = this.mInitialAttachApnSetting;
        if (SystemProperties.getInt(PROPERTY_FORCE_APN_CHANGE, 0) == 0) {
            boolean ignoreName = !VZW_FEATURE;
            String prevPreferredApnString = prevPreferredApn == null ? "" : prevPreferredApn.toStringIgnoreName(ignoreName);
            String curPreferredApnString = this.mPreferredApn == null ? "" : this.mPreferredApn.toStringIgnoreName(ignoreName);
            if (previousAttachApn != null) {
                previousAttachApn.toStringIgnoreName(ignoreName);
            }
            if (this.mInitialAttachApnSetting != null) {
                this.mInitialAttachApnSetting.toStringIgnoreName(ignoreName);
            }
            if (TextUtils.equals(prevPreferredApnString, curPreferredApnString) && isApnSettingExist(previousAttachApn)) {
                if ((prevPreferredApn == null || previousAttachApn == null) && !TextUtils.equals(ApnSetting.toStringIgnoreNameForList(prevAllApns, ignoreName), ApnSetting.toStringIgnoreNameForList(this.mAllApnSettings, ignoreName))) {
                    log("onApnChanged: all APN setting changed.");
                } else if (MTK_IMS_SUPPORT && isIMSApnSettingChanged(prevAllApns, this.mAllApnSettings)) {
                    sendOnApnChangedDone(true);
                    log("onApnChanged: IMS apn setting changed!!");
                    return;
                } else {
                    log("onApnChanged: not changed");
                    return;
                }
            }
        }
        IccRecords r = this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        if (operator != null && operator.length() > 0) {
            setInitialAttachApn();
        } else {
            log("onApnChanged: but no operator numeric");
        }
        logd("onApnChanged: cleanUpAllConnections and setup connectable APN");
        sendOnApnChangedDone(false);
    }

    private void sendOnApnChangedDone(boolean bImsApnChanged) {
        Message msg = obtainMessage(270839);
        msg.arg1 = bImsApnChanged ? 1 : 0;
        sendMessage(msg);
    }

    private void onApnChangedDone() {
        DctConstants.State overallState = getOverallState();
        boolean isDisconnected = overallState == DctConstants.State.IDLE || overallState == DctConstants.State.FAILED;
        cleanUpConnectionsOnUpdatedApns(isDisconnected ? false : true);
        logd("onApnChanged: phone.getsubId=" + this.mPhone.getSubId() + "getDefaultDataSubscriptionId()" + SubscriptionManager.getDefaultDataSubscriptionId());
        if (this.mPhone.getSubId() != SubscriptionManager.getDefaultDataSubscriptionId()) {
            return;
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_APN_CHANGED);
    }

    private DcAsyncChannel findDataConnectionAcByCid(int cid) {
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            if (dcac.getCidSync() == cid) {
                return dcac;
            }
        }
        return null;
    }

    private void gotoIdleAndNotifyDataConnection(String reason) {
        if (DBG) {
            log("gotoIdleAndNotifyDataConnection: reason=" + reason);
        }
        notifyDataConnection(reason);
    }

    private boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        for (ApnContext otherContext : this.mPrioritySortedApnContexts) {
            if (apnContext.getApnType().equalsIgnoreCase(otherContext.getApnType())) {
                return false;
            }
            if (otherContext.isEnabled() && otherContext.getState() != DctConstants.State.FAILED) {
                return true;
            }
        }
        return false;
    }

    private boolean isOnlySingleDcAllowed(int rilRadioTech) {
        int[] singleDcRats = this.mPhone.getContext().getResources().getIntArray(R.array.config_companionDeviceCerts);
        boolean onlySingleDcAllowed = false;
        if (!BSP_PACKAGE && this.mTelephonyExt != null) {
            try {
                onlySingleDcAllowed = this.mTelephonyExt.isOnlySingleDcAllowed();
                if (onlySingleDcAllowed) {
                    log("isOnlySingleDcAllowed: " + onlySingleDcAllowed);
                    return true;
                }
            } catch (Exception ex) {
                loge("Fail to create or use plug-in");
                ex.printStackTrace();
            }
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("persist.telephony.test.singleDc", false)) {
            onlySingleDcAllowed = true;
        }
        if (singleDcRats != null) {
            for (int i = 0; i < singleDcRats.length && !onlySingleDcAllowed; i++) {
                if (rilRadioTech == singleDcRats[i]) {
                    onlySingleDcAllowed = true;
                }
            }
        }
        if (DBG) {
            log("isOnlySingleDcAllowed(" + rilRadioTech + "): " + onlySingleDcAllowed);
        }
        return onlySingleDcAllowed;
    }

    void sendRestartRadio() {
        if (DBG) {
            log("sendRestartRadio:");
        }
        Message msg = obtainMessage(270362);
        sendMessage(msg);
    }

    private void restartRadio() {
        if (DBG) {
            log("restartRadio: ************TURN OFF RADIO**************");
        }
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        this.mPhone.getServiceStateTracker().powerOffRadioSafely(this);
        int reset = Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", "0"));
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(reset + 1));
    }

    private boolean retryAfterDisconnected(ApnContext apnContext) {
        String reason = apnContext.getReason();
        if (!PhoneInternalInterface.REASON_RADIO_TURNED_OFF.equals(reason) && !PhoneInternalInterface.REASON_FDN_ENABLED.equals(reason) && (!isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) || !isHigherPriorityApnContextActive(apnContext))) {
            return true;
        }
        return false;
    }

    private void startAlarmForReconnect(long delay, ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        Intent intent = new Intent("com.android.internal.telephony.data-reconnect." + apnType);
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, apnContext.getReason());
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, apnType);
        intent.addFlags(268435456);
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        intent.putExtra("subscription", subId);
        if (DBG) {
            log("startAlarmForReconnect: delay=" + delay + " action=" + intent.getAction() + " apn=" + apnContext);
        }
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        apnContext.setReconnectIntent(alarmIntent);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + delay, alarmIntent);
    }

    private void notifyNoData(DcFailCause lastFailCauseCode, ApnContext apnContext) {
        if (DBG) {
            log("notifyNoData: type=" + apnContext.getApnType());
        }
        if (!isPermanentFail(lastFailCauseCode) || apnContext.getApnType().equals("default")) {
            return;
        }
        this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
    }

    public boolean getAutoAttachOnCreation() {
        return this.mAutoAttachOnCreation.get();
    }

    private void onRecordsLoadedOrSubIdChanged() {
        if (DBG) {
            log("onRecordsLoadedOrSubIdChanged: createAllApnList");
        }
        this.mAutoAttachOnCreationConfig = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.maxFileSize);
        if (this.mFdMgr != null) {
            this.mFdMgr.disableFdWhenTethering();
        }
        if (MTK_CC33_SUPPORT) {
            this.mPhone.mCi.setRemoveRestrictEutranMode(true, null);
            this.mPhone.mCi.setDataOnToMD(this.mUserDataEnabled, null);
        }
        syncDataSettingsToMd(getDataEnabled(), getDataOnRoamingEnabled());
        createAllApnList();
        setInitialAttachApn();
        if (this.mPhone.mCi.getRadioState().isOn()) {
            if (DBG) {
                log("onRecordsLoadedOrSubIdChanged: notifying data availability");
            }
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_SIM_LOADED);
        }
        boolean bGetDataCallList = true;
        if (this.mPhone.getPhoneType() == 2) {
            bGetDataCallList = false;
        }
        if (bGetDataCallList) {
            this.mDcc.getDataCallListForSimLoaded();
        } else {
            sendMessage(obtainMessage(270840));
        }
    }

    private boolean isFdnEnableSupport() {
        if (BSP_PACKAGE || this.mGsmDctExt == null) {
            return false;
        }
        boolean isFdnEnableSupport = this.mGsmDctExt.isFdnEnableSupport();
        return isFdnEnableSupport;
    }

    private boolean isFdnEnabled() {
        if (!isFdnEnableSupport()) {
            return false;
        }
        boolean bIsFdnEnabled = getFdnStatus();
        return bIsFdnEnabled;
    }

    private boolean getFdnStatus() {
        ITelephonyEx telephonyEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telephonyEx != null) {
            try {
                boolean bIsFdnEnabled = telephonyEx.isFdnEnabled(this.mPhone.getSubId());
                return bIsFdnEnabled;
            } catch (RemoteException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        loge("getFdnStatus get telephonyEx failed!!");
        return false;
    }

    private void onFdnChanged() {
        if (isFdnEnableSupport()) {
            logd("onFdnChanged");
            if (getFdnStatus()) {
                logd("fdn enabled, cleanUpAllConnections!");
                cleanUpAllConnections(true, PhoneInternalInterface.REASON_FDN_ENABLED);
                return;
            } else {
                logd("fdn disabled, setupDataOnConnectableApns!");
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_FDN_DISABLED);
                return;
            }
        }
        logd("not support fdn enabled, skip onFdnChanged");
    }

    private void applyUnProvisionedSimDetected() {
        if (isColdSimDetected()) {
            if (this.mColdSimDetected) {
                return;
            }
            if (DBG) {
                log("onColdSimDetected: cleanUpAllDataConnections");
            }
            cleanUpAllConnections(null);
            this.mPhone.notifyOtaspChanged(5);
            this.mColdSimDetected = true;
            return;
        }
        if (isOutOfCreditSimDetected()) {
            if (this.mOutOfCreditSimDetected) {
                return;
            }
            if (DBG) {
                log("onOutOfCreditSimDetected on subId: re-establish data connection");
            }
            for (ApnContext context : this.redirectApnContextSet) {
                onTrySetupData(context);
                this.redirectApnContextSet.remove(context);
            }
            this.mOutOfCreditSimDetected = true;
            return;
        }
        if (DBG) {
            log("Provisioned Sim Detected on subId: " + this.mPhone.getSubId());
        }
        this.mColdSimDetected = false;
        this.mOutOfCreditSimDetected = false;
    }

    private void onSimNotReady() {
        if (DBG) {
            log("onSimNotReady");
        }
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_SIM_NOT_READY);
        if (this.mAllApnSettings != null) {
            this.mAllApnSettings.clear();
        }
        this.mAutoAttachOnCreationConfig = false;
    }

    private void onSetDependencyMet(String apnType, boolean met) {
        ApnContext apnContext;
        if ("hipri".equals(apnType)) {
            return;
        }
        ApnContext apnContext2 = this.mApnContexts.get(apnType);
        if (apnContext2 == null) {
            loge("onSetDependencyMet: ApnContext not found in onSetDependencyMet(" + apnType + ", " + met + ")");
            return;
        }
        applyNewState(apnContext2, apnContext2.isEnabled(), met);
        if (!"default".equals(apnType) || (apnContext = this.mApnContexts.get("hipri")) == null) {
            return;
        }
        applyNewState(apnContext, apnContext.isEnabled(), met);
    }

    private void onSetPolicyDataEnabled(boolean enabled) {
        synchronized (this.mDataEnabledLock) {
            boolean prevEnabled = getAnyDataEnabled();
            if (sPolicyDataEnabled != enabled) {
                sPolicyDataEnabled = enabled;
                if (prevEnabled != getAnyDataEnabled()) {
                    if (!prevEnabled) {
                        onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
                    } else {
                        onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                    }
                }
            }
        }
    }

    private void applyNewState(ApnContext apnContext, boolean enabled, boolean met) {
        boolean cleanup = false;
        boolean trySetup = false;
        String str = "applyNewState(" + apnContext.getApnType() + ", " + enabled + "(" + apnContext.isEnabled() + "), " + met + "(" + apnContext.getDependencyMet() + "))";
        if (DBG) {
            log(str);
        }
        apnContext.requestLog(str);
        if (apnContext.isReady()) {
            cleanup = true;
            if (enabled && met) {
                DctConstants.State state = apnContext.getState();
                switch (m360xf0fbc33d()[state.ordinal()]) {
                    case 1:
                    case 2:
                    case 3:
                    case 7:
                        if (DBG) {
                            log("applyNewState: 'ready' so return");
                        }
                        apnContext.requestLog("applyNewState state=" + state + ", so return");
                        return;
                    case 4:
                    case 5:
                    case 6:
                        trySetup = true;
                        apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                        break;
                }
            } else if (enabled) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_UNMET);
            } else {
                cleanup = true;
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DISABLED);
            }
        } else if (enabled && met) {
            if (apnContext.isEnabled()) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_MET);
            } else {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
            }
            if (apnContext.getState() == DctConstants.State.FAILED) {
                apnContext.setState(DctConstants.State.IDLE);
            }
            trySetup = true;
        }
        apnContext.setEnabled(enabled);
        apnContext.setDependencyMet(met);
        if (cleanup) {
            cleanUpConnection(true, apnContext);
        }
        if (trySetup) {
            apnContext.resetErrorCodeRetries();
            trySetupData(apnContext);
        }
    }

    private DcAsyncChannel checkForCompatibleConnectedApnContext(ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        ApnSetting dunSetting = null;
        boolean bIsRequestApnTypeEmergency = "emergency".equals(apnType);
        if ("dun".equals(apnType)) {
            dunSetting = fetchDunApn();
        }
        if (DBG) {
            log("checkForCompatibleConnectedApnContext: apnContext=" + apnContext);
        }
        DcAsyncChannel potentialDcac = null;
        ApnContext potentialApnCtx = null;
        for (ApnContext curApnCtx : this.mApnContexts.values()) {
            DcAsyncChannel curDcac = curApnCtx.getDcAc();
            if (curDcac != null) {
                ApnSetting apnSetting = curApnCtx.getApnSetting();
                log("apnSetting: " + apnSetting);
                if (dunSetting != null) {
                    if (dunSetting.equals(apnSetting)) {
                        switch (m360xf0fbc33d()[curApnCtx.getState().ordinal()]) {
                            case 1:
                                if (DBG) {
                                    log("checkForCompatibleConnectedApnContext: found dun conn=" + curDcac + " curApnCtx=" + curApnCtx);
                                }
                                return curDcac;
                            case 2:
                            case 6:
                            case 7:
                                potentialDcac = curDcac;
                                potentialApnCtx = curApnCtx;
                                break;
                        }
                    } else {
                        continue;
                    }
                } else if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                    boolean bIsSkip = false;
                    if (bIsRequestApnTypeEmergency && !"emergency".equals(curApnCtx.getApnType())) {
                        log("checkForCompatibleConnectedApnContext: found canHandle conn=" + curDcac + " curApnCtx=" + curApnCtx + ", but not emergency type (skip)");
                        bIsSkip = true;
                    }
                    switch (m360xf0fbc33d()[curApnCtx.getState().ordinal()]) {
                        case 1:
                            if (!bIsSkip) {
                                if (DBG) {
                                    log("checkForCompatibleConnectedApnContext: found canHandle conn=" + curDcac + " curApnCtx=" + curApnCtx);
                                }
                                return curDcac;
                            }
                            break;
                            break;
                        case 2:
                        case 6:
                        case 7:
                            if (!bIsSkip) {
                                potentialDcac = curDcac;
                                potentialApnCtx = curApnCtx;
                            }
                            break;
                    }
                }
            } else if (VDBG) {
                log("checkForCompatibleConnectedApnContext: not conn curApnCtx=" + curApnCtx);
            }
        }
        if (potentialDcac != null) {
            if (DBG) {
                log("checkForCompatibleConnectedApnContext: found potential conn=" + potentialDcac + " curApnCtx=" + potentialApnCtx);
            }
            return potentialDcac;
        }
        if (DBG) {
            log("checkForCompatibleConnectedApnContext: NO conn apnContext=" + apnContext);
        }
        return null;
    }

    public void setEnabled(int id, boolean enable) {
        Message msg = obtainMessage(270349);
        msg.arg1 = id;
        msg.arg2 = enable ? 1 : 0;
        sendMessage(msg);
    }

    private void onEnableApn(int apnId, int enabled) {
        ApnContext apnContext = this.mApnContextsById.get(apnId);
        if (apnContext == null) {
            loge("onEnableApn(" + apnId + ", " + enabled + "): NO ApnContext");
            return;
        }
        if (DBG) {
            log("onEnableApn: apnContext=" + apnContext + " call applyNewState");
        }
        applyNewState(apnContext, enabled == 1, apnContext.getDependencyMet());
    }

    private boolean onTrySetupData(String reason) {
        if (DBG) {
            log("onTrySetupData: reason=" + reason);
        }
        setupDataOnConnectableApns(reason);
        return true;
    }

    private boolean onTrySetupData(ApnContext apnContext) {
        if (DBG) {
            log("onTrySetupData: apnContext=" + apnContext);
        }
        return trySetupData(apnContext);
    }

    public boolean getDataEnabled() {
        int device_provisioned = Settings.Global.getInt(this.mResolver, "device_provisioned", 0);
        boolean retVal = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true"));
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            retVal = Settings.Global.getInt(this.mResolver, "mobile_data", retVal ? 1 : 0) != 0;
        } else {
            int phoneSubId = this.mPhone.getSubId();
            if (VDBG) {
                log("getDataEnabled: phoneSubId = " + phoneSubId);
            }
            try {
                retVal = TelephonyManager.getIntWithSubId(this.mResolver, "mobile_data", phoneSubId) != 0;
            } catch (Settings.SettingNotFoundException e) {
            }
        }
        logd("getDataEnabled: retVal=" + retVal);
        if (device_provisioned == 0) {
            String prov_property = SystemProperties.get("ro.com.android.prov_mobiledata", retVal ? "true" : "false");
            int prov_mobile_data = Settings.Global.getInt(this.mResolver, "device_provisioning_mobile_data", "true".equalsIgnoreCase(prov_property) ? 1 : 0);
            retVal = prov_mobile_data != 0;
            log("getDataEnabled during provisioning retVal=" + retVal + " - (" + prov_property + ", " + prov_mobile_data + ")");
        }
        return retVal;
    }

    public void setDataOnRoamingEnabled(boolean enabled) {
        int phoneSubId = this.mPhone.getSubId();
        if (getDataOnRoamingEnabled() != enabled) {
            int roaming = enabled ? 1 : 0;
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                Settings.Global.putInt(this.mResolver, "data_roaming", roaming);
            } else {
                Settings.Global.putInt(this.mResolver, "data_roaming" + phoneSubId, roaming);
            }
            syncDataSettingsToMd(getDataEnabled(), enabled);
            this.mSubscriptionManager.setDataRoaming(roaming, phoneSubId);
            if (!DBG) {
                return;
            }
            log("setDataOnRoamingEnabled: set phoneSubId=" + phoneSubId + " isRoaming=" + enabled);
            return;
        }
        if (!DBG) {
            return;
        }
        log("setDataOnRoamingEnabled: unchanged phoneSubId=" + phoneSubId + " isRoaming=" + enabled);
    }

    public boolean getDataOnRoamingEnabled() {
        boolean isDataRoamingEnabled = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false"));
        int phoneSubId = this.mPhone.getSubId();
        try {
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                isDataRoamingEnabled = Settings.Global.getInt(this.mResolver, "data_roaming", isDataRoamingEnabled ? 1 : 0) != 0;
            } else {
                isDataRoamingEnabled = TelephonyManager.getIntWithSubId(this.mResolver, "data_roaming", phoneSubId) != 0;
            }
        } catch (Settings.SettingNotFoundException snfe) {
            if (DBG) {
                log("getDataOnRoamingEnabled: SettingNofFoundException snfe=" + snfe);
            }
        }
        if (VDBG) {
            log("getDataOnRoamingEnabled: phoneSubId=" + phoneSubId + " isDataRoamingEnabled=" + isDataRoamingEnabled);
        }
        return isDataRoamingEnabled;
    }

    private boolean ignoreDataRoaming(String apnType) {
        boolean ignoreDataRoaming = false;
        try {
            ignoreDataRoaming = this.mTelephonyExt.ignoreDataRoaming(apnType);
        } catch (Exception e) {
            loge("get ignoreDataRoaming fail!");
            e.printStackTrace();
        }
        if (ignoreDataRoaming) {
            logd("ignoreDataRoaming: " + ignoreDataRoaming + ", apnType = " + apnType);
        }
        return ignoreDataRoaming;
    }

    private boolean ignoreDataAllow(String apnType) {
        if (!ImsSwitchController.IMS_SERVICE.equals(apnType)) {
            return false;
        }
        return true;
    }

    private boolean ignoreDefaultDataUnselected(String apnType) {
        boolean ignoreDefaultDataUnselected = false;
        try {
            ignoreDefaultDataUnselected = this.mTelephonyExt.ignoreDefaultDataUnselected(apnType);
        } catch (Exception e) {
            loge("get ignoreDefaultDataUnselected fail!");
            e.printStackTrace();
        }
        if (!ignoreDefaultDataUnselected && TextUtils.equals(apnType, "default") && isVsimActive(this.mPhone.getPhoneId())) {
            logd("Vsim is enabled, set ignoreDefaultDataUnselected as true");
            ignoreDefaultDataUnselected = true;
        }
        if (ignoreDefaultDataUnselected) {
            logd("ignoreDefaultDataUnselected: " + ignoreDefaultDataUnselected + ", apnType = " + apnType);
        }
        return ignoreDefaultDataUnselected;
    }

    private void onRoamingOff() {
        boolean bDataOnRoamingEnabled = getDataOnRoamingEnabled();
        logd("onRoamingOff bDataOnRoamingEnabled=" + bDataOnRoamingEnabled + ", mUserDataEnabled=" + this.mUserDataEnabled);
        if (this.mUserDataEnabled) {
            if (!bDataOnRoamingEnabled) {
                notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_OFF);
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_OFF);
            } else {
                notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_OFF);
            }
        }
    }

    private void onRoamingOn() {
        boolean bDataOnRoamingEnabled = getDataOnRoamingEnabled();
        if (DBG) {
            log("onRoamingOn bDataOnRoamingEnabled=" + bDataOnRoamingEnabled + ", mUserDataEnabled=" + this.mUserDataEnabled);
        }
        if (!this.mUserDataEnabled) {
            if (DBG) {
                log("data not enabled by user");
                return;
            }
            return;
        }
        if (!this.mPhone.getServiceState().getDataRoaming()) {
            if (DBG) {
                log("device is not roaming. ignored the request.");
            }
        } else {
            if (bDataOnRoamingEnabled) {
                if (DBG) {
                    log("onRoamingOn: setup data on roaming");
                }
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_ON);
                notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_ON);
                return;
            }
            if (DBG) {
                log("onRoamingOn: Tear down data connection on roaming.");
            }
            cleanUpAllConnections(true, PhoneInternalInterface.REASON_ROAMING_ON);
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
        }
    }

    private void onRadioAvailable() {
        if (DBG) {
            log("onRadioAvailable");
        }
        if (this.mPhone.getSimulatedRadioControl() != null) {
            notifyDataConnection(null);
            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }
        IccRecords r = this.mIccRecords.get();
        if (r != null && r.getRecordsLoaded()) {
            notifyOffApnsOfAvailability(null);
        }
        if (getOverallState() == DctConstants.State.IDLE) {
            return;
        }
        cleanUpConnection(true, null);
    }

    private void onRadioOffOrNotAvailable() {
        this.mReregisterOnReconnectFailure = false;
        this.mAutoAttachOnCreation.set(false);
        if (this.mPhone.getSimulatedRadioControl() != null) {
            log("We're on the simulator; assuming radio off is meaningless");
            notifyOffApnsOfAvailability(null);
        } else {
            logd("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        }
    }

    private void completeConnection(ApnContext apnContext) {
        if (DBG) {
            log("completeConnection: successful, notify the world apnContext=" + apnContext);
        }
        if (this.mIsProvisioning && !TextUtils.isEmpty(this.mProvisioningUrl)) {
            if (DBG) {
                log("completeConnection: MOBILE_PROVISIONING_ACTION url=" + this.mProvisioningUrl);
            }
            Intent newIntent = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
            newIntent.setData(Uri.parse(this.mProvisioningUrl));
            newIntent.setFlags(272629760);
            try {
                this.mPhone.getContext().startActivity(newIntent);
            } catch (ActivityNotFoundException e) {
                loge("completeConnection: startActivityAsUser failed" + e);
            }
        }
        this.mIsProvisioning = false;
        this.mProvisioningUrl = null;
        if (this.mProvisioningSpinner != null) {
            sendMessage(obtainMessage(270378, this.mProvisioningSpinner));
        }
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        startNetStatPoll();
        startDataStallAlarm(false);
    }

    private void onDataSetupComplete(AsyncResult ar) {
        DcFailCause dcFailCause = DcFailCause.UNKNOWN;
        boolean handleError = false;
        ApnContext apnContext = getValidApnContext(ar, "onDataSetupComplete");
        if (apnContext == null) {
            return;
        }
        if (ar.exception == null) {
            DcAsyncChannel dcac = apnContext.getDcAc();
            if (dcac == null) {
                log("onDataSetupComplete: no connection to DC, handle as error");
                DcFailCause cause = DcFailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
                apnContext.setState(DctConstants.State.FAILED);
                handleError = true;
            } else {
                ApnSetting apn = apnContext.getApnSetting();
                if (DBG) {
                    log("onDataSetupComplete: success apn=" + (apn == null ? "unknown" : apn.apn));
                }
                if (apn != null && apn.proxy != null && apn.proxy.length() != 0) {
                    try {
                        String port = apn.port;
                        if (TextUtils.isEmpty(port)) {
                            port = "8080";
                        }
                        ProxyInfo proxy = new ProxyInfo(apn.proxy, Integer.parseInt(port), null);
                        dcac.setLinkPropertiesHttpProxySync(proxy);
                    } catch (NumberFormatException e) {
                        loge("onDataSetupComplete: NumberFormatException making ProxyProperties (" + apn.port + "): " + e);
                    }
                }
                if (TextUtils.equals(apnContext.getApnType(), "default")) {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "true");
                    } catch (RuntimeException e2) {
                        log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to true");
                    }
                    if (this.mCanSetPreferApn && this.mPreferredApn == null) {
                        if (DBG) {
                            log("onDataSetupComplete: PREFERRED APN is null");
                        }
                        this.mPreferredApn = apn;
                        if (this.mPreferredApn != null) {
                            setPreferredApn(this.mPreferredApn.id);
                        }
                    }
                } else {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                    } catch (RuntimeException e3) {
                        loge("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                    }
                }
                apnContext.setState(DctConstants.State.CONNECTED);
                boolean isProvApn = apnContext.isProvisioningApn();
                ConnectivityManager cm = ConnectivityManager.from(this.mPhone.getContext());
                if (this.mProvisionBroadcastReceiver != null) {
                    this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
                    this.mProvisionBroadcastReceiver = null;
                }
                if (!isProvApn || this.mIsProvisioning) {
                    cm.setProvisioningNotificationVisible(false, 0, this.mProvisionActionName);
                    completeConnection(apnContext);
                } else {
                    if (DBG) {
                        log("onDataSetupComplete: successful, BUT send connected to prov apn as mIsProvisioning:" + this.mIsProvisioning + " == false && (isProvisioningApn:" + isProvApn + " == true");
                    }
                    this.mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(cm.getMobileProvisioningUrl(), TelephonyManager.getDefault().getNetworkOperatorName());
                    this.mPhone.getContext().registerReceiver(this.mProvisionBroadcastReceiver, new IntentFilter(this.mProvisionActionName));
                    cm.setProvisioningNotificationVisible(true, 0, this.mProvisionActionName);
                    setRadio(false);
                }
                if (DBG) {
                    log("onDataSetupComplete: SETUP complete type=" + apnContext.getApnType() + ", reason:" + apnContext.getReason());
                }
            }
        } else {
            DcFailCause cause2 = (DcFailCause) ar.result;
            if (DBG) {
                ApnSetting apn2 = apnContext.getApnSetting();
                Object[] objArr = new Object[2];
                objArr[0] = apn2 == null ? "unknown" : apn2.apn;
                objArr[1] = cause2;
                log(String.format("onDataSetupComplete: error apn=%s cause=%s", objArr));
            }
            if (cause2.isEventLoggable()) {
                int cid = getCellLocationId();
                EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL, Integer.valueOf(cause2.ordinal()), Integer.valueOf(cid), Integer.valueOf(TelephonyManager.getDefault().getNetworkType()));
            }
            ApnSetting apn3 = apnContext.getApnSetting();
            this.mPhone.notifyPreciseDataConnectionFailed(apnContext.getReason(), apnContext.getApnType(), apn3 != null ? apn3.apn : "unknown", cause2.toString());
            Intent intent = new Intent("android.intent.action.REQUEST_NETWORK_FAILED");
            intent.putExtra("errorCode", cause2.getErrorCode());
            intent.putExtra(APN_TYPE_KEY, apnContext.getApnType());
            notifyCarrierAppWithIntent(intent);
            if (cause2.isRestartRadioFail() || apnContext.restartOnError(cause2.getErrorCode())) {
                if (DBG) {
                    log("Modem restarted.");
                }
                sendRestartRadio();
            }
            if (isPermanentFail(cause2) || (this.mGsmDctExt != null && this.mGsmDctExt.isIgnoredCause(cause2))) {
                log("cause = " + cause2 + ", mark apn as permanent failed. apn = " + apn3);
                apnContext.markApnPermanentFailed(apn3);
            }
            handleError = true;
        }
        if (handleError) {
            onDataSetupCompleteError(ar);
        }
        if (this.mInternalDataEnabled) {
            return;
        }
        cleanUpAllConnections(null);
    }

    private ApnContext getValidApnContext(AsyncResult ar, String logString) {
        if (ar != null && (ar.userObj instanceof Pair)) {
            Pair<ApnContext, Integer> pair = (Pair) ar.userObj;
            ApnContext apnContext = (ApnContext) pair.first;
            if (apnContext != null) {
                int generation = apnContext.getConnectionGeneration();
                if (DBG) {
                    log("getValidApnContext (" + logString + ") on " + apnContext + " got " + generation + " vs " + pair.second);
                }
                if (generation == ((Integer) pair.second).intValue()) {
                    return apnContext;
                }
                log("ignoring obsolete " + logString);
                return null;
            }
        }
        throw new RuntimeException(logString + ": No apnContext");
    }

    private void onDataSetupCompleteError(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDataSetupCompleteError");
        if (apnContext == null) {
            return;
        }
        long delay = apnContext.getDelayForNextApn(this.mFailFast);
        if (delay >= 0) {
            if (DBG) {
                log("onDataSetupCompleteError: Try next APN. delay = " + delay);
            }
            apnContext.setState(DctConstants.State.SCANNING);
            startAlarmForReconnect(delay, apnContext);
            return;
        }
        apnContext.setState(DctConstants.State.FAILED);
        this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_APN_FAILED, apnContext.getApnType());
        apnContext.setDataConnectionAc(null);
        log("onDataSetupCompleteError: Stop retrying APNs.");
    }

    private String[] getActivationAppName() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfig();
        }
        if (b != null) {
            String[] activationApp = b.getStringArray("sim_state_detection_carrier_app_string_array");
            return activationApp;
        }
        String[] activationApp2 = CarrierConfigManager.getDefaultConfig().getStringArray("sim_state_detection_carrier_app_string_array");
        return activationApp2;
    }

    private void onDataConnectionRedirected(String redirectUrl, HashMap<ApnContext, DataConnection.ConnectionParams> apnContextMap) {
        if (TextUtils.isEmpty(redirectUrl)) {
            return;
        }
        this.mRedirectUrl = redirectUrl;
        Intent intent = new Intent("android.intent.action.REDIRECTION_DETECTED");
        intent.putExtra(REDIRECTION_URL_KEY, redirectUrl);
        if (isColdSimDetected() || isOutOfCreditSimDetected() || !checkCarrierAppAvailable(intent)) {
            return;
        }
        log("Starting Activation Carrier app with redirectUrl : " + redirectUrl);
        for (ApnContext context : apnContextMap.keySet()) {
            cleanUpConnection(true, context);
            this.redirectApnContextSet.add(context);
        }
    }

    private void onDisconnectDone(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDone");
        if (apnContext == null) {
            return;
        }
        if (DBG) {
            log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + apnContext);
        }
        apnContext.setState(DctConstants.State.IDLE);
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        if ((isDisconnected() || isOnlyIMSorEIMSPdnConnected()) && this.mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
            if (DBG) {
                log("onDisconnectDone: radio will be turned off, no retries");
            }
            apnContext.setApnSetting(null);
            apnContext.setDataConnectionAc(null);
            if (this.mDisconnectPendingCount > 0) {
                this.mDisconnectPendingCount--;
            }
            if (this.mDisconnectPendingCount == 0) {
                notifyDataDisconnectComplete();
                notifyAllDataDisconnected();
                notifyCarrierAppForRedirection();
                return;
            }
            return;
        }
        if (this.mAttached.get() && apnContext.isReady() && retryAfterDisconnected(apnContext)) {
            if (ImsSwitchController.IMS_SERVICE.equals(apnContext.getApnType()) || "emergency".equals(apnContext.getApnType())) {
                if (DBG) {
                    log("onDisconnectDone: not to retry for " + apnContext.getApnType() + " PDN");
                }
            } else {
                try {
                    SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                } catch (RuntimeException e) {
                    log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                }
                if (DBG) {
                    log("onDisconnectDone: attached, ready and retry after disconnect");
                }
                long delay = getDisconnectDoneRetryTimer(apnContext.getReason(), apnContext.getInterApnDelay(this.mFailFast));
                if (delay > 0) {
                    startAlarmForReconnect(delay, apnContext);
                }
            }
        } else {
            boolean restartRadioAfterProvisioning = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.layout_ignoreOffset);
            if (apnContext.isProvisioningApn() && restartRadioAfterProvisioning) {
                log("onDisconnectDone: restartRadio after provisioning");
                restartRadio();
            }
            apnContext.setApnSetting(null);
            apnContext.setDataConnectionAc(null);
            if (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
                if (DBG) {
                    log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                }
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
            } else if (DBG) {
                log("onDisconnectDone: not retrying");
            }
        }
        if (this.mDisconnectPendingCount > 0) {
            this.mDisconnectPendingCount--;
        }
        if (this.mDisconnectPendingCount != 0) {
            return;
        }
        apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
        notifyDataDisconnectComplete();
        notifyAllDataDisconnected();
        notifyCarrierAppForRedirection();
    }

    private long getDisconnectDoneRetryTimer(String reason, long delay) {
        if (PhoneInternalInterface.REASON_APN_CHANGED.equals(reason)) {
            return 3000L;
        }
        if (BSP_PACKAGE || this.mGsmDctExt == null) {
            return delay;
        }
        try {
            long timer = this.mGsmDctExt.getDisconnectDoneRetryTimer(reason, delay);
            return timer;
        } catch (Exception e) {
            loge("GsmDCTExt.getDisconnectDoneRetryTimer fail!");
            e.printStackTrace();
            return delay;
        }
    }

    private void onDisconnectDcRetrying(AsyncResult ar) {
        ApnContext apnContext = getValidApnContext(ar, "onDisconnectDcRetrying");
        if (apnContext == null) {
            return;
        }
        apnContext.setState(DctConstants.State.RETRYING);
        if (DBG) {
            log("onDisconnectDcRetrying: apnContext=" + apnContext);
        }
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
    }

    public void onVoiceCallStarted() {
        this.mInVoiceCall = true;
        boolean isSupportConcurrent = DataConnectionHelper.getInstance().isDataSupportConcurrent(this.mPhone.getPhoneId());
        logd("onVoiceCallStarted:isDataSupportConcurrent = " + isSupportConcurrent);
        if (isConnected() && !isSupportConcurrent) {
            logd("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_STARTED);
        }
        notifyVoiceCallEventToDataConnection(this.mInVoiceCall, isSupportConcurrent);
    }

    public void onVoiceCallEnded() {
        this.mInVoiceCall = false;
        boolean isSupportConcurrent = DataConnectionHelper.getInstance().isDataSupportConcurrent(this.mPhone.getPhoneId());
        logd("onVoiceCallEnded:isDataSupportConcurrent = " + isSupportConcurrent);
        if (!getDataEnabled()) {
            logd("onVoiceCallEnded: default data disable, cleanup default apn.");
            onCleanUpConnection(true, 0, PhoneInternalInterface.REASON_DATA_DISABLED);
        }
        if (isConnected()) {
            if (!isSupportConcurrent) {
                startNetStatPoll();
                startDataStallAlarm(false);
                notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
            } else {
                resetPollStats();
            }
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
        notifyVoiceCallEventToDataConnection(this.mInVoiceCall, isSupportConcurrent);
    }

    private void onCleanUpConnection(boolean tearDown, int apnId, String reason) {
        if (DBG) {
            log("onCleanUpConnection");
        }
        ApnContext apnContext = this.mApnContextsById.get(apnId);
        if (apnContext == null) {
            return;
        }
        apnContext.setReason(reason);
        cleanUpConnection(tearDown, apnContext);
    }

    private boolean isConnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getState() == DctConstants.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    public boolean isDisconnected() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!apnContext.isDisconnected()) {
                return false;
            }
        }
        return true;
    }

    private void notifyDataConnection(String reason) {
        if (DBG) {
            log("notifyDataConnection: reason=" + reason);
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady() && apnContext.isNeedNotify()) {
                if (DBG) {
                    log("notifyDataConnection: type:" + apnContext.getApnType());
                }
                this.mPhone.notifyDataConnection(reason != null ? reason : apnContext.getReason(), apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    private void setDataProfilesAsNeeded() {
        if (DBG) {
            log("setDataProfilesAsNeeded");
        }
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            return;
        }
        ArrayList<DataProfile> dps = new ArrayList<>();
        for (ApnSetting apn : this.mAllApnSettings) {
            if (apn.modemCognitive) {
                DataProfile dp = new DataProfile(apn, this.mPhone.getServiceState().getDataRoaming());
                boolean isDup = false;
                Iterator dpIn$iterator = dps.iterator();
                while (true) {
                    if (!dpIn$iterator.hasNext()) {
                        break;
                    }
                    DataProfile dpIn = (DataProfile) dpIn$iterator.next();
                    if (dp.equals(dpIn)) {
                        isDup = true;
                        break;
                    }
                }
                if (!isDup) {
                    dps.add(dp);
                }
            }
        }
        if (dps.size() <= 0) {
            return;
        }
        this.mPhone.mCi.setDataProfile((DataProfile[]) dps.toArray(new DataProfile[0]), null);
    }

    private void createAllApnList() {
        this.mMvnoMatched = false;
        this.mAllApnSettings = new ArrayList<>();
        IccRecords r = this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        if (DBG) {
            log("createAllApnList: operator = " + operator);
        }
        String operator2 = this.mTelephonyExt.getOperatorNumericFromImpi(operator, this.mPhone.getPhoneId());
        if (operator2 != null) {
            String selection = "numeric = '" + operator2 + "'";
            if (DBG) {
                log("createAllApnList: selection=" + selection);
            }
            Cursor cursor = this.mPhone.getContext().getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, selection, null, "_id");
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    this.mAllApnSettings = createApnList(cursor);
                }
                cursor.close();
            }
        }
        addEmergencyApnSetting();
        dedupeApnSettings();
        if (this.mAllApnSettings.isEmpty()) {
            if (DBG) {
                log("createAllApnList: No APN found for carrier: " + operator2);
            }
            this.mPreferredApn = null;
        } else {
            this.mPreferredApn = getPreferredApn();
            if (this.mPreferredApn != null && !this.mPreferredApn.numeric.equals(operator2)) {
                this.mPreferredApn = null;
                setPreferredApn(-1);
            }
            if (DBG) {
                log("createAllApnList: mPreferredApn=" + this.mPreferredApn);
            }
        }
        if (DBG) {
            log("createAllApnList: X mAllApnSettings=" + this.mAllApnSettings);
        }
        setDataProfilesAsNeeded();
        syncApnToMd();
        syncApnTableToRds(this.mAllApnSettings);
    }

    private void dedupeApnSettings() {
        new ArrayList();
        for (int i = 0; i < this.mAllApnSettings.size() - 1; i++) {
            ApnSetting first = this.mAllApnSettings.get(i);
            int j = i + 1;
            while (j < this.mAllApnSettings.size()) {
                ApnSetting second = this.mAllApnSettings.get(j);
                if (apnsSimilar(first, second)) {
                    ApnSetting newApn = mergeApns(first, second);
                    this.mAllApnSettings.set(i, newApn);
                    first = newApn;
                    this.mAllApnSettings.remove(j);
                } else {
                    j++;
                }
            }
        }
    }

    private boolean apnTypeSameAny(ApnSetting first, ApnSetting second) {
        if (VDBG) {
            StringBuilder apnType1 = new StringBuilder(first.apn + ": ");
            for (int index1 = 0; index1 < first.types.length; index1++) {
                apnType1.append(first.types[index1]);
                apnType1.append(",");
            }
            StringBuilder apnType2 = new StringBuilder(second.apn + ": ");
            for (int index12 = 0; index12 < second.types.length; index12++) {
                apnType2.append(second.types[index12]);
                apnType2.append(",");
            }
            log("APN1: is " + ((Object) apnType1));
            log("APN2: is " + ((Object) apnType2));
        }
        for (int index13 = 0; index13 < first.types.length; index13++) {
            for (int index2 = 0; index2 < second.types.length; index2++) {
                if (first.types[index13].equals(CharacterSets.MIMENAME_ANY_CHARSET) || second.types[index2].equals(CharacterSets.MIMENAME_ANY_CHARSET) || first.types[index13].equals(second.types[index2])) {
                    if (VDBG) {
                        log("apnTypeSameAny: return true");
                        return true;
                    }
                    return true;
                }
            }
        }
        if (VDBG) {
            log("apnTypeSameAny: return false");
            return false;
        }
        return false;
    }

    private boolean apnsSimilar(ApnSetting first, ApnSetting second) {
        if (!first.canHandleType("dun") && !second.canHandleType("dun") && Objects.equals(first.apn, second.apn) && !apnTypeSameAny(first, second) && xorEquals(first.proxy, second.proxy) && xorEquals(first.port, second.port) && first.carrierEnabled == second.carrierEnabled && first.bearerBitmask == second.bearerBitmask && first.profileId == second.profileId && Objects.equals(first.mvnoType, second.mvnoType) && Objects.equals(first.mvnoMatchData, second.mvnoMatchData) && xorEquals(first.mmsc, second.mmsc) && xorEquals(first.mmsProxy, second.mmsProxy)) {
            return xorEquals(first.mmsPort, second.mmsPort);
        }
        return false;
    }

    private boolean xorEquals(String first, String second) {
        if (Objects.equals(first, second) || TextUtils.isEmpty(first)) {
            return true;
        }
        return TextUtils.isEmpty(second);
    }

    private ApnSetting mergeApns(ApnSetting dest, ApnSetting src) {
        int id = dest.id;
        ArrayList<String> resultTypes = new ArrayList<>();
        resultTypes.addAll(Arrays.asList(dest.types));
        for (String srcType : src.types) {
            if (!resultTypes.contains(srcType)) {
                resultTypes.add(srcType);
            }
            if (srcType.equals("default")) {
                id = src.id;
            }
        }
        String mmsc = TextUtils.isEmpty(dest.mmsc) ? src.mmsc : dest.mmsc;
        String mmsProxy = TextUtils.isEmpty(dest.mmsProxy) ? src.mmsProxy : dest.mmsProxy;
        String mmsPort = TextUtils.isEmpty(dest.mmsPort) ? src.mmsPort : dest.mmsPort;
        String proxy = TextUtils.isEmpty(dest.proxy) ? src.proxy : dest.proxy;
        String port = TextUtils.isEmpty(dest.port) ? src.port : dest.port;
        String protocol = src.protocol.equals(VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL) ? src.protocol : dest.protocol;
        String roamingProtocol = src.roamingProtocol.equals(VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL) ? src.roamingProtocol : dest.roamingProtocol;
        int bearerBitmask = (dest.bearerBitmask == 0 || src.bearerBitmask == 0) ? 0 : dest.bearerBitmask | src.bearerBitmask;
        return new ApnSetting(id, dest.numeric, dest.carrier, dest.apn, proxy, port, mmsc, mmsProxy, mmsPort, dest.user, dest.password, dest.authType, (String[]) resultTypes.toArray(new String[0]), protocol, roamingProtocol, dest.carrierEnabled, 0, bearerBitmask, dest.profileId, !dest.modemCognitive ? src.modemCognitive : true, dest.maxConns, dest.waitTime, dest.maxConnsTime, dest.mtu, dest.mvnoType, dest.mvnoMatchData, dest.inactiveTimer);
    }

    private DcAsyncChannel createDataConnection(String reqApnType, ApnSetting apnSetting) {
        int id;
        if (DBG) {
            log("createDataConnection E");
        }
        if (isSupportThrottlingApn()) {
            id = generateDataConnectionId(reqApnType, apnSetting);
            if (id < 0) {
                return null;
            }
        } else {
            id = this.mUniqueIdGenerator.getAndIncrement();
            if (id >= getPdpConnectionPoolSize()) {
                loge("Max PDP count is " + getPdpConnectionPoolSize() + ",but request " + (id + 1));
                this.mUniqueIdGenerator.getAndDecrement();
                return null;
            }
        }
        DataConnection conn = DataConnection.makeDataConnection(this.mPhone, id, this, this.mDcTesterFailBringUpAll, this.mDcc);
        this.mDataConnections.put(Integer.valueOf(id), conn);
        DcAsyncChannel dcac = new DcAsyncChannel(conn, LOG_TAG);
        int status = dcac.fullyConnectSync(this.mPhone.getContext(), this, conn.getHandler());
        if (status == 0) {
            this.mDataConnectionAcHashMap.put(Integer.valueOf(dcac.getDataConnectionIdSync()), dcac);
        } else {
            loge("createDataConnection: Could not connect to dcac=" + dcac + " status=" + status);
        }
        if (DBG) {
            log("createDataConnection() X id=" + id + " dc=" + conn);
        }
        return dcac;
    }

    private void destroyDataConnections() {
        if (this.mDataConnections != null) {
            if (DBG) {
                log("destroyDataConnections: clear mDataConnectionList");
            }
            this.mDataConnections.clear();
        } else if (DBG) {
            log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    private ArrayList<ApnSetting> buildWaitingApns(String requestedApnType, int radioTech) {
        boolean usePreferred;
        ApnSetting dun;
        if (DBG) {
            log("buildWaitingApns: E requestedApnType=" + requestedApnType);
        }
        ArrayList<ApnSetting> apnList = new ArrayList<>();
        if (requestedApnType.equals("dun") && (dun = fetchDunApn()) != null) {
            apnList.add(dun);
            if (DBG) {
                log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + apnList);
            }
            return apnList;
        }
        IccRecords r = this.mIccRecords.get();
        String operator = r != null ? r.getOperatorNumeric() : "";
        try {
            usePreferred = !this.mPhone.getContext().getResources().getBoolean(R.^attr-private.layout_hasNestedScrollIndicator);
        } catch (Resources.NotFoundException e) {
            if (DBG) {
                log("buildWaitingApns: usePreferred NotFoundException set to true");
            }
            usePreferred = true;
        }
        if (usePreferred) {
            this.mPreferredApn = getPreferredApn();
        }
        if (DBG) {
            log("buildWaitingApns: usePreferred=" + usePreferred + " canSetPreferApn=" + this.mCanSetPreferApn + " mPreferredApn=" + this.mPreferredApn + " operator=" + operator + " radioTech=" + radioTech + " IccRecords r=" + r);
        }
        if (usePreferred && this.mCanSetPreferApn && this.mPreferredApn != null && this.mPreferredApn.canHandleType(requestedApnType) && !isWifiOnlyApn(this.mPreferredApn.bearerBitmask)) {
            if (DBG) {
                log("buildWaitingApns: Preferred APN:" + operator + ":" + this.mPreferredApn.numeric + ":" + this.mPreferredApn);
            }
            if (!this.mPreferredApn.numeric.equals(operator)) {
                if (DBG) {
                    log("buildWaitingApns: no preferred APN");
                }
                setPreferredApn(-1);
                this.mPreferredApn = null;
            } else {
                if (ServiceState.bitmaskHasTech(this.mPreferredApn.bearerBitmask, radioTech)) {
                    apnList.add(this.mPreferredApn);
                    if (DBG) {
                        log("buildWaitingApns: X added preferred apnList=" + apnList);
                    }
                    return apnList;
                }
                if (DBG) {
                    log("buildWaitingApns: no preferred APN");
                }
                setPreferredApn(-1);
                this.mPreferredApn = null;
            }
        }
        if (this.mAllApnSettings != null) {
            if (DBG) {
                log("buildWaitingApns: mAllApnSettings=" + this.mAllApnSettings);
            }
            for (ApnSetting apn : this.mAllApnSettings) {
                if (!apn.canHandleType(requestedApnType) || isWifiOnlyApn(apn.bearerBitmask)) {
                    if (DBG) {
                        log("buildWaitingApns: couldn't handle requested ApnType=" + requestedApnType);
                    }
                } else if (ServiceState.bitmaskHasTech(apn.bearerBitmask, radioTech)) {
                    if (apn.canHandleType("emergency")) {
                        String eimsProtocol = SystemProperties.get(VOLTE_EMERGENCY_PDN_PROTOCOL, VOLTE_DEFAULT_EMERGENCY_PDN_PROTOCOL);
                        logd("initEmergencyApnSetting: eimsProtocol = " + eimsProtocol);
                        ApnSetting apn2 = !eimsProtocol.equals(apn.protocol) ? new ApnSetting(apn.id, apn.numeric, apn.carrier, apn.apn, apn.proxy, apn.port, apn.mmsc, apn.mmsProxy, apn.mmsPort, apn.user, apn.password, apn.authType, apn.types, eimsProtocol, eimsProtocol, apn.carrierEnabled, 0, apn.bearerBitmask, apn.profileId, apn.modemCognitive, apn.maxConns, apn.waitTime, apn.maxConnsTime, apn.mtu, apn.mvnoType, apn.mvnoMatchData, apn.inactiveTimer) : apn;
                        if (DBG) {
                            log("buildWaitingApns: adding apn=" + apn2);
                        }
                        apnList.add(apn2);
                    }
                } else if (DBG) {
                    log("buildWaitingApns: bearerBitmask:" + apn.bearerBitmask + " does not include radioTech:" + radioTech);
                }
            }
        } else {
            loge("mAllApnSettings is null!");
        }
        if (DBG) {
            log("buildWaitingApns: " + apnList.size() + " APNs in the list: " + apnList);
        }
        return apnList;
    }

    private ArrayList<ApnSetting> buildWifiApns(String requestedApnType) {
        if (DBG) {
            log("buildWifiApns: E requestedApnType=" + requestedApnType);
        }
        ArrayList<ApnSetting> apnList = new ArrayList<>();
        if (this.mAllApnSettings != null) {
            if (DBG) {
                log("buildWaitingApns: mAllApnSettings=" + this.mAllApnSettings);
            }
            for (ApnSetting apn : this.mAllApnSettings) {
                if (apn.canHandleType(requestedApnType) && isWifiOnlyApn(apn.bearerBitmask)) {
                    apnList.add(apn);
                }
            }
        }
        if (DBG) {
            log("buildWifiApns: X apnList=" + apnList);
        }
        return apnList;
    }

    private String apnListToString(ArrayList<ApnSetting> apns) {
        StringBuilder result = new StringBuilder();
        try {
            int size = apns.size();
            for (int i = 0; i < size; i++) {
                result.append('[').append(apns.get(i).toString()).append(']');
            }
            return result.toString();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private void setPreferredApn(int pos) {
        if (!this.mCanSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }
        String subId = Long.toString(this.mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        log("setPreferredApn: delete subId=" + subId);
        ContentResolver resolver = this.mPhone.getContext().getContentResolver();
        resolver.delete(uri, null, null);
        if (pos < 0) {
            return;
        }
        log("setPreferredApn: insert pos=" + pos + ", subId=" + subId);
        ContentValues values = new ContentValues();
        values.put(APN_ID, Integer.valueOf(pos));
        resolver.insert(uri, values);
    }

    private ApnSetting getPreferredApn() {
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            log("getPreferredApn: mAllApnSettings is " + (this.mAllApnSettings == null ? "null" : "empty"));
            return null;
        }
        String subId = Long.toString(this.mPhone.getSubId());
        Uri uri = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, subId);
        Cursor cursor = this.mPhone.getContext().getContentResolver().query(uri, new String[]{"_id", "name", "apn"}, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            this.mCanSetPreferApn = true;
        } else {
            this.mCanSetPreferApn = false;
        }
        log("getPreferredApn: mRequestedApnType=" + this.mRequestedApnType + " cursor=" + cursor + " cursor.count=" + (cursor != null ? cursor.getCount() : 0) + " subId=" + subId);
        if (this.mCanSetPreferApn && cursor.getCount() > 0) {
            cursor.moveToFirst();
            int pos = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            for (ApnSetting p : this.mAllApnSettings) {
                log("getPreferredApn: apnSetting=" + p + ", pos=" + pos + ", subId=" + subId);
                if (p.id == pos && p.canHandleType(this.mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + p);
                    cursor.close();
                    return p;
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage(Message msg) {
        boolean zIsProvisioningApn;
        String apnType;
        if (VDBG) {
            log("handleMessage msg=" + msg);
        }
        switch (msg.what) {
            case 69636:
                log("DISCONNECTED_CONNECTED: msg=" + msg);
                DcAsyncChannel dcac = (DcAsyncChannel) msg.obj;
                this.mDataConnectionAcHashMap.remove(Integer.valueOf(dcac.getDataConnectionIdSync()));
                dcac.disconnected();
                return;
            case 270336:
                onDataSetupComplete((AsyncResult) msg.obj);
                return;
            case 270337:
                break;
            case 270338:
                int subId = this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    onRecordsLoadedOrSubIdChanged();
                    return;
                } else {
                    log("Ignoring EVENT_RECORDS_LOADED as subId is not valid: " + subId);
                    return;
                }
            case 270339:
                if (msg.obj instanceof ApnContext) {
                    onTrySetupData((ApnContext) msg.obj);
                    return;
                } else if (msg.obj instanceof String) {
                    onTrySetupData((String) msg.obj);
                    return;
                } else {
                    loge("EVENT_TRY_SETUP request w/o apnContext or String");
                    return;
                }
            case 270340:
                return;
            case 270342:
                onRadioOffOrNotAvailable();
                return;
            case 270343:
                onVoiceCallStarted();
                return;
            case 270344:
                onVoiceCallEnded();
                return;
            case 270345:
                onDataConnectionDetached();
                return;
            case 270347:
                onRoamingOn();
                return;
            case 270348:
                onRoamingOff();
                return;
            case 270349:
                onEnableApn(msg.arg1, msg.arg2);
                return;
            case 270351:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DONE msg=" + msg);
                onDisconnectDone((AsyncResult) msg.obj);
                return;
            case 270352:
                onDataConnectionAttached();
                return;
            case 270353:
                onDataStallAlarm(msg.arg1);
                return;
            case 270354:
                doRecovery();
                return;
            case 270355:
                onApnChanged();
                return;
            case 270358:
                if (DBG) {
                    log("EVENT_PS_RESTRICT_ENABLED " + this.mIsPsRestricted);
                }
                stopNetStatPoll();
                stopDataStallAlarm();
                this.mIsPsRestricted = true;
                return;
            case 270359:
                ConnectivityManager cnnm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
                if (DBG) {
                    log("EVENT_PS_RESTRICT_DISABLED " + this.mIsPsRestricted);
                }
                this.mIsPsRestricted = false;
                if (isConnected()) {
                    startNetStatPoll();
                    startDataStallAlarm(false);
                    return;
                }
                if (this.mState == DctConstants.State.FAILED) {
                    cleanUpAllConnections(false, PhoneInternalInterface.REASON_PS_RESTRICT_ENABLED);
                    this.mReregisterOnReconnectFailure = false;
                }
                ApnContext apnContext = this.mApnContextsById.get(0);
                if (apnContext == null) {
                    loge("**** Default ApnContext not found ****");
                    if (Build.IS_DEBUGGABLE && cnnm.isNetworkSupported(0)) {
                        throw new RuntimeException("Default ApnContext not found");
                    }
                    return;
                }
                if (this.mPhone.getServiceStateTracker().getCurrentDataConnectionState() != 0) {
                    log("EVENT_PS_RESTRICT_DISABLED, data not attached, skip.");
                    return;
                } else {
                    apnContext.setReason(PhoneInternalInterface.REASON_PS_RESTRICT_ENABLED);
                    trySetupData(apnContext);
                    return;
                }
            case 270360:
                boolean tearDown = msg.arg1 != 0;
                if (DBG) {
                    log("EVENT_CLEAN_UP_CONNECTION tearDown=" + tearDown);
                }
                if (msg.obj instanceof ApnContext) {
                    cleanUpConnection(tearDown, (ApnContext) msg.obj);
                    return;
                } else {
                    onCleanUpConnection(tearDown, msg.arg2, (String) msg.obj);
                    return;
                }
            case 270362:
                restartRadio();
                return;
            case 270363:
                onSetInternalDataEnabled(msg.arg1 == 1, (Message) msg.obj);
                return;
            case 270364:
                if (DBG) {
                    log("EVENT_RESET_DONE");
                }
                onResetDone((AsyncResult) msg.obj);
                return;
            case 270365:
                if (msg.obj != null && !(msg.obj instanceof String)) {
                    msg.obj = null;
                }
                onCleanUpAllConnections((String) msg.obj);
                return;
            case 270366:
                boolean enabled = msg.arg1 == 1;
                if (DBG) {
                    log("CMD_SET_USER_DATA_ENABLE enabled=" + enabled);
                }
                onSetUserDataEnabled(enabled);
                return;
            case 270367:
                boolean met = msg.arg1 == 1;
                if (DBG) {
                    log("CMD_SET_DEPENDENCY_MET met=" + met);
                }
                Bundle bundle = msg.getData();
                if (bundle == null || (apnType = (String) bundle.get(APN_TYPE_KEY)) == null) {
                    return;
                }
                onSetDependencyMet(apnType, met);
                return;
            case 270368:
                onSetPolicyDataEnabled(msg.arg1 == 1);
                return;
            case 270369:
                onUpdateIcc();
                return;
            case 270370:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DC_RETRYING msg=" + msg);
                onDisconnectDcRetrying((AsyncResult) msg.obj);
                return;
            case 270371:
                onDataSetupCompleteError((AsyncResult) msg.obj);
                return;
            case 270372:
                sEnableFailFastRefCounter = (msg.arg1 == 1 ? 1 : -1) + sEnableFailFastRefCounter;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA:  sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (sEnableFailFastRefCounter < 0) {
                    String s = "CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: sEnableFailFastRefCounter:" + sEnableFailFastRefCounter + " < 0";
                    loge(s);
                    sEnableFailFastRefCounter = 0;
                }
                boolean enabled2 = sEnableFailFastRefCounter > 0;
                if (DBG) {
                    log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=" + enabled2 + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                }
                if (this.mFailFast != enabled2) {
                    this.mFailFast = enabled2;
                    this.mDataStallDetectionEnabled = !enabled2;
                    if (!this.mDataStallDetectionEnabled || getOverallState() != DctConstants.State.CONNECTED || (this.mInVoiceCall && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) {
                        if (DBG) {
                            log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                        }
                        stopDataStallAlarm();
                        return;
                    } else {
                        if (DBG) {
                            log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                        }
                        stopDataStallAlarm();
                        startDataStallAlarm(false);
                        return;
                    }
                }
                return;
            case 270373:
                Bundle bundle2 = msg.getData();
                if (bundle2 != null) {
                    try {
                        this.mProvisioningUrl = (String) bundle2.get("provisioningUrl");
                    } catch (ClassCastException e) {
                        loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url not a string" + e);
                        this.mProvisioningUrl = null;
                    }
                }
                if (TextUtils.isEmpty(this.mProvisioningUrl)) {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioning url is empty, ignoring");
                    this.mIsProvisioning = false;
                    this.mProvisioningUrl = null;
                    return;
                } else {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioningUrl=" + this.mProvisioningUrl);
                    this.mIsProvisioning = true;
                    startProvisioningApnAlarm();
                    return;
                }
            case 270374:
                if (DBG) {
                    log("CMD_IS_PROVISIONING_APN");
                }
                try {
                    Bundle bundle3 = msg.getData();
                    String apnType2 = bundle3 != null ? (String) bundle3.get(APN_TYPE_KEY) : null;
                    if (TextUtils.isEmpty(apnType2)) {
                        loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                        zIsProvisioningApn = false;
                    } else {
                        zIsProvisioningApn = isProvisioningApn(apnType2);
                    }
                } catch (ClassCastException e2) {
                    loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                    zIsProvisioningApn = false;
                }
                if (DBG) {
                    log("CMD_IS_PROVISIONING_APN: ret=" + zIsProvisioningApn);
                }
                this.mReplyAc.replyToMessage(msg, 270374, zIsProvisioningApn ? 1 : 0);
                return;
            case 270375:
                if (DBG) {
                    log("EVENT_PROVISIONING_APN_ALARM");
                }
                ApnContext apnCtx = this.mApnContextsById.get(0);
                if (!apnCtx.isProvisioningApn() || !apnCtx.isConnectedOrConnecting()) {
                    if (DBG) {
                        log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                        return;
                    }
                    return;
                } else if (this.mProvisioningApnAlarmTag != msg.arg1) {
                    if (DBG) {
                        log("EVENT_PROVISIONING_APN_ALARM: ignore stale tag, mProvisioningApnAlarmTag:" + this.mProvisioningApnAlarmTag + " != arg1:" + msg.arg1);
                        return;
                    }
                    return;
                } else {
                    if (DBG) {
                        log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                    }
                    this.mIsProvisioning = false;
                    this.mProvisioningUrl = null;
                    stopProvisioningApnAlarm();
                    sendCleanUpConnection(true, apnCtx);
                    return;
                }
            case 270376:
                if (msg.arg1 == 1) {
                    handleStartNetStatPoll((DctConstants.Activity) msg.obj);
                    return;
                } else {
                    if (msg.arg1 == 0) {
                        handleStopNetStatPoll((DctConstants.Activity) msg.obj);
                        return;
                    }
                    return;
                }
            case 270377:
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_NW_TYPE_CHANGED, RetryFailures.ONLY_ON_CHANGE);
                return;
            case 270378:
                if (this.mProvisioningSpinner == msg.obj) {
                    this.mProvisioningSpinner.dismiss();
                    this.mProvisioningSpinner = null;
                    return;
                }
                return;
            case 270379:
                onDeviceProvisionedChange();
                return;
            case 270380:
                AsyncResult ar = (AsyncResult) msg.obj;
                String url = (String) ar.userObj;
                log("dataConnectionTracker.handleMessage: EVENT_REDIRECTION_DETECTED=" + url);
                onDataConnectionRedirected(url, (HashMap) ar.result);
                break;
            case 270839:
                boolean bImsApnChanged = msg.arg1 != 0;
                logd("EVENT_APN_CHANGED_DONE");
                if (!bImsApnChanged) {
                    onApnChangedDone();
                    return;
                } else {
                    log("ims apn changed");
                    cleanUpConnection(true, this.mApnContexts.get(ImsSwitchController.IMS_SERVICE));
                    return;
                }
            case 270840:
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_SIM_LOADED);
                return;
            case 270841:
                onFdnChanged();
                return;
            case 270842:
                if (MTK_CC33_SUPPORT) {
                    logd("EVENT_REMOVE_RESTRICT_EUTRAN");
                    this.mReregisterOnReconnectFailure = false;
                    setupDataOnConnectableApns(PhoneInternalInterface.REASON_PS_RESTRICT_DISABLED);
                    return;
                }
                return;
            case 270843:
                logd("EVENT_RESET_PDP_DONE cid=" + msg.arg1);
                return;
            case 270844:
                if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
                    setInitialAttachApn();
                    return;
                } else {
                    if (DBG) {
                        log("EVENT_RESET_ATTACH_APN: Ignore due to null APN list");
                        return;
                    }
                    return;
                }
            case 270846:
                int newDefaultRefCount = msg.arg1;
                onSharedDefaultApnState(newDefaultRefCount);
                return;
            case 270847:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null) {
                    loge("LteAccessStratumState exception: " + ar2.exception);
                    return;
                }
                int[] ints = (int[]) ar2.result;
                int lteAccessStratumDataState = ints.length > 0 ? ints[0] : -1;
                int networkType = ints.length > 1 ? ints[1] : -1;
                if (lteAccessStratumDataState != 1) {
                    notifyPsNetworkTypeChanged(networkType);
                } else {
                    this.mPhone.notifyPsNetworkTypeChanged(13);
                }
                logd("EVENT_LTE_ACCESS_STRATUM_STATE lteAccessStratumDataState = " + lteAccessStratumDataState + ", networkType = " + networkType);
                notifyLteAccessStratumChanged(lteAccessStratumDataState);
                return;
            case 270848:
                handlePlmnChange((AsyncResult) msg.obj);
                return;
            case 270849:
                handleRegistrationSuspend((AsyncResult) msg.obj);
                return;
            case 270850:
                handleSetResume();
                return;
            case 270851:
                onPcoStatus((AsyncResult) msg.obj);
                return;
            case 270852:
                onMdChangedAttachApn((AsyncResult) msg.obj);
                return;
            case 270853:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3 == null || ar3.result == null) {
                    loge("Parameter error: ret should not be NULL");
                    return;
                } else {
                    boolean allowed = ((int[]) ar3.result)[0] == 1;
                    onAllowChanged(allowed);
                    return;
                }
            default:
                Rlog.e("DcTracker", "Unhandled event=" + msg);
                return;
        }
        onRadioAvailable();
    }

    private int getApnProfileID(String apnType) {
        if (TextUtils.equals(apnType, ImsSwitchController.IMS_SERVICE)) {
            return 2;
        }
        if (TextUtils.equals(apnType, "fota")) {
            return 3;
        }
        if (TextUtils.equals(apnType, "cbs")) {
            return 4;
        }
        if (TextUtils.equals(apnType, "ia")) {
            return 0;
        }
        if (TextUtils.equals(apnType, "dun")) {
            return 1;
        }
        if (TextUtils.equals(apnType, "mms")) {
            return 1001;
        }
        if (TextUtils.equals(apnType, "supl")) {
            return 1002;
        }
        if (TextUtils.equals(apnType, "hipri")) {
            return 1003;
        }
        if (TextUtils.equals(apnType, "dm")) {
            return 1004;
        }
        if (TextUtils.equals(apnType, "wap")) {
            return 1005;
        }
        if (TextUtils.equals(apnType, "net")) {
            return 1006;
        }
        if (TextUtils.equals(apnType, "cmmail")) {
            return 1007;
        }
        if (TextUtils.equals(apnType, "rcse")) {
            return 1008;
        }
        if (TextUtils.equals(apnType, "emergency")) {
            return 1009;
        }
        if (TextUtils.equals(apnType, "xcap")) {
            return GsmVTProvider.SESSION_EVENT_START_COUNTER;
        }
        if (TextUtils.equals(apnType, "rcs")) {
            return 1011;
        }
        return TextUtils.equals(apnType, "default") ? 0 : -1;
    }

    private int getCellLocationId() {
        CellLocation loc = this.mPhone.getCellLocation();
        if (loc == null) {
            return -1;
        }
        if (loc instanceof GsmCellLocation) {
            int cid = ((GsmCellLocation) loc).getCid();
            return cid;
        }
        if (!(loc instanceof CdmaCellLocation)) {
            return -1;
        }
        int cid2 = ((CdmaCellLocation) loc).getBaseStationId();
        return cid2;
    }

    private IccRecords getUiccRecords(int appFamily) {
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), appFamily);
    }

    private void onUpdateIcc() {
        if (this.mUiccController == null) {
            return;
        }
        IccRecords newIccRecords = getUiccRecords(1);
        if (newIccRecords == null && this.mPhone.getPhoneType() == 2) {
            newIccRecords = getUiccRecords(2);
        }
        IccRecords r = this.mIccRecords.get();
        logd("onUpdateIcc: newIccRecords=" + newIccRecords + ", r=" + r);
        if (r != newIccRecords) {
            if (r != null) {
                log("Removing stale icc objects.");
                r.unregisterForRecordsLoaded(this);
                this.mIccRecords.set(null);
            }
            if (newIccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                    log("New records found.");
                    this.mIccRecords.set(newIccRecords);
                    newIccRecords.registerForRecordsLoaded(this, 270338, null);
                    SubscriptionController.getInstance().setSimProvisioningStatus(0, this.mPhone.getSubId());
                }
            } else {
                onSimNotReady();
            }
        }
        if (this.mAllApnSettings != null && r == null && newIccRecords == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    DcTracker.this.logd("onUpdateIcc: clear mAllApnSettings, " + (DcTracker.this.mAllApnSettings != null));
                    if (DcTracker.this.mAllApnSettings == null) {
                        return;
                    }
                    DcTracker.this.mAllApnSettings.clear();
                }
            });
        }
        UiccCardApplication app = this.mUiccCardApplication.get();
        UiccCardApplication newUiccCardApp = this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneType() != 2 ? 1 : 2);
        if (app == newUiccCardApp) {
            return;
        }
        if (app != null) {
            log("Removing stale UiccCardApplication objects.");
            app.unregisterForFdnChanged(this);
            this.mUiccCardApplication.set(null);
        }
        if (newUiccCardApp == null) {
            return;
        }
        log("New UiccCardApplication found");
        newUiccCardApp.registerForFdnChanged(this, 270841, null);
        this.mUiccCardApplication.set(newUiccCardApp);
    }

    public void update() {
        log("update sub = " + this.mPhone.getSubId());
        onUpdateIcc();
        this.mUserDataEnabled = getDataEnabled();
        this.mAutoAttachOnCreation.set(false);
        ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
    }

    public void cleanUpAllConnections(String cause) {
        cleanUpAllConnections(cause, (Message) null);
    }

    public void updateRecords() {
        onUpdateIcc();
    }

    public void cleanUpAllConnections(String cause, Message disconnectAllCompleteMsg) {
        log("cleanUpAllConnections");
        if (disconnectAllCompleteMsg != null) {
            this.mDisconnectAllCompleteMsgList.add(disconnectAllCompleteMsg);
        }
        Message msg = obtainMessage(270365);
        msg.obj = cause;
        sendMessage(msg);
    }

    private boolean checkCarrierAppAvailable(Intent intent) {
        String[] activationApp = getActivationAppName();
        if (activationApp == null || activationApp.length != 2) {
            return false;
        }
        intent.setClassName(activationApp[0], activationApp[1]);
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        if (!packageManager.queryBroadcastReceivers(intent, GsmVTProviderUtil.UI_MODE_DESTROY).isEmpty()) {
            return true;
        }
        loge("Activation Carrier app is configured, but not available: " + activationApp[0] + "." + activationApp[1]);
        return false;
    }

    private boolean notifyCarrierAppWithIntent(Intent intent) {
        if (this.mDisconnectPendingCount != 0) {
            loge("Wait for pending disconnect requests done");
            return false;
        }
        if (!checkCarrierAppAvailable(intent)) {
            loge("Carrier app is unavailable");
            return false;
        }
        intent.putExtra("subscription", this.mPhone.getSubId());
        intent.addFlags(268435456);
        try {
            this.mPhone.getContext().sendBroadcast(intent);
            if (DBG) {
                log("send Intent to Carrier app with action: " + intent.getAction());
                return true;
            }
            return true;
        } catch (ActivityNotFoundException e) {
            loge("sendBroadcast failed: " + e);
            return false;
        }
    }

    private void notifyCarrierAppForRedirection() {
        if (isColdSimDetected() || isOutOfCreditSimDetected() || this.mRedirectUrl == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.REDIRECTION_DETECTED");
        intent.putExtra(REDIRECTION_URL_KEY, this.mRedirectUrl);
        if (notifyCarrierAppWithIntent(intent)) {
            this.mRedirectUrl = null;
        }
    }

    private void notifyDataDisconnectComplete() {
        log("notifyDataDisconnectComplete");
        for (Message m : this.mDisconnectAllCompleteMsgList) {
            m.sendToTarget();
        }
        this.mDisconnectAllCompleteMsgList.clear();
    }

    private void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        this.mFailFast = false;
        this.mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        this.mAllDataDisconnectedRegistrants.addUnique(h, what, obj);
        if (!isDisconnected()) {
            return;
        }
        log("notify All Data Disconnected");
        notifyAllDataDisconnected();
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        this.mAllDataDisconnectedRegistrants.remove(h);
    }

    private void onSetInternalDataEnabled(boolean enabled, Message onCompleteMsg) {
        if (DBG) {
            log("onSetInternalDataEnabled: enabled=" + enabled);
        }
        boolean sendOnComplete = true;
        synchronized (this.mDataEnabledLock) {
            this.mInternalDataEnabled = enabled;
            if (enabled) {
                log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
                onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
            } else {
                sendOnComplete = false;
                log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
                cleanUpAllConnections((String) null, onCompleteMsg);
            }
        }
        if (!sendOnComplete || onCompleteMsg == null) {
            return;
        }
        onCompleteMsg.sendToTarget();
    }

    public boolean setInternalDataEnabledFlag(boolean enable) {
        if (DBG) {
            log("setInternalDataEnabledFlag(" + enable + ")");
        }
        if (this.mInternalDataEnabled != enable) {
            this.mInternalDataEnabled = enable;
            return true;
        }
        return true;
    }

    public boolean setInternalDataEnabled(boolean enable) {
        return setInternalDataEnabled(enable, null);
    }

    public boolean setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        if (DBG) {
            log("setInternalDataEnabled(" + enable + ")");
        }
        Message msg = obtainMessage(270363, onCompleteMsg);
        msg.arg1 = enable ? 1 : 0;
        sendMessage(msg);
        return true;
    }

    public void setDataAllowed(boolean enable, Message response) {
        if (DBG) {
            log("setDataAllowed: enable=" + enable);
        }
        this.isCleanupRequired.set(!enable);
        this.mPhone.mCi.setDataAllowed(enable, response);
        this.mInternalDataEnabled = enable;
    }

    private void log(String s) {
        logd(s);
    }

    private void loge(String s) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void logw(String s) {
        Rlog.w(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void logi(String s) {
        Rlog.i(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void logd(String s) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void logv(String s) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DcTracker:");
        pw.println(" RADIO_TESTS=false");
        pw.println(" mInternalDataEnabled=" + this.mInternalDataEnabled);
        pw.println(" mUserDataEnabled=" + this.mUserDataEnabled);
        pw.println(" sPolicyDataEnabed=" + sPolicyDataEnabled);
        pw.flush();
        pw.println(" mRequestedApnType=" + this.mRequestedApnType);
        pw.println(" mPhone=" + this.mPhone.getPhoneName());
        pw.println(" mActivity=" + this.mActivity);
        pw.println(" mState=" + this.mState);
        pw.println(" mTxPkts=" + this.mTxPkts);
        pw.println(" mRxPkts=" + this.mRxPkts);
        pw.println(" mNetStatPollPeriod=" + this.mNetStatPollPeriod);
        pw.println(" mNetStatPollEnabled=" + this.mNetStatPollEnabled);
        pw.println(" mDataStallTxRxSum=" + this.mDataStallTxRxSum);
        pw.println(" mDataStallAlarmTag=" + this.mDataStallAlarmTag);
        pw.println(" mDataStallDetectionEanbled=" + this.mDataStallDetectionEnabled);
        pw.println(" mSentSinceLastRecv=" + this.mSentSinceLastRecv);
        pw.println(" mNoRecvPollCount=" + this.mNoRecvPollCount);
        pw.println(" mResolver=" + this.mResolver);
        pw.println(" mIsWifiConnected=" + this.mIsWifiConnected);
        pw.println(" mReconnectIntent=" + this.mReconnectIntent);
        pw.println(" mAutoAttachOnCreation=" + this.mAutoAttachOnCreation.get());
        pw.println(" mIsScreenOn=" + this.mIsScreenOn);
        pw.println(" mUniqueIdGenerator=" + this.mUniqueIdGenerator);
        pw.flush();
        pw.println(" ***************************************");
        DcController dcc = this.mDcc;
        if (dcc != null) {
            dcc.dump(fd, pw, args);
        } else {
            pw.println(" mDcc=null");
        }
        pw.println(" ***************************************");
        HashMap<Integer, DataConnection> dcs = this.mDataConnections;
        if (dcs != null) {
            Set<Map.Entry<Integer, DataConnection>> mDcSet = this.mDataConnections.entrySet();
            pw.println(" mDataConnections: count=" + mDcSet.size());
            for (Map.Entry<Integer, DataConnection> entry : mDcSet) {
                pw.printf(" *** mDataConnection[%d] \n", entry.getKey());
                entry.getValue().dump(fd, pw, args);
            }
        } else {
            pw.println("mDataConnections=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        HashMap<String, Integer> apnToDcId = this.mApnToDataConnectionId;
        if (apnToDcId != null) {
            Set<Map.Entry<String, Integer>> apnToDcIdSet = apnToDcId.entrySet();
            pw.println(" mApnToDataConnectonId size=" + apnToDcIdSet.size());
            for (Map.Entry<String, Integer> entry2 : apnToDcIdSet) {
                pw.printf(" mApnToDataConnectonId[%s]=%d\n", entry2.getKey(), entry2.getValue());
            }
        } else {
            pw.println("mApnToDataConnectionId=null");
        }
        pw.println(" ***************************************");
        pw.flush();
        ConcurrentHashMap<String, ApnContext> apnCtxs = this.mApnContexts;
        if (apnCtxs != null) {
            Set<Map.Entry<String, ApnContext>> apnCtxsSet = apnCtxs.entrySet();
            pw.println(" mApnContexts size=" + apnCtxsSet.size());
            Iterator entry$iterator = apnCtxsSet.iterator();
            while (entry$iterator.hasNext()) {
                ((Map.Entry) entry$iterator.next()).getValue().dump(fd, pw, args);
            }
            pw.println(" ***************************************");
        } else {
            pw.println(" mApnContexts=null");
        }
        pw.flush();
        ArrayList<ApnSetting> apnSettings = this.mAllApnSettings;
        if (apnSettings != null) {
            pw.println(" mAllApnSettings size=" + apnSettings.size());
            for (int i = 0; i < apnSettings.size(); i++) {
                pw.printf(" mAllApnSettings[%d]: %s\n", Integer.valueOf(i), apnSettings.get(i));
            }
            pw.flush();
        } else {
            pw.println(" mAllApnSettings=null");
        }
        pw.println(" mPreferredApn=" + this.mPreferredApn);
        pw.println(" mIsPsRestricted=" + this.mIsPsRestricted);
        pw.println(" mIsDisposed=" + this.mIsDisposed);
        pw.println(" mIntentReceiver=" + this.mIntentReceiver);
        pw.println(" mReregisterOnReconnectFailure=" + this.mReregisterOnReconnectFailure);
        pw.println(" canSetPreferApn=" + this.mCanSetPreferApn);
        pw.println(" mApnObserver=" + this.mApnObserver);
        pw.println(" getOverallState=" + getOverallState());
        pw.println(" mDataConnectionAsyncChannels=%s\n" + this.mDataConnectionAcHashMap);
        pw.println(" mAttached=" + this.mAttached.get());
        pw.flush();
    }

    public String[] getPcscfAddress(String apnType) {
        ApnContext apnContext;
        log("getPcscfAddress()");
        if (apnType == null) {
            log("apnType is null, return null");
            return null;
        }
        if (TextUtils.equals(apnType, "emergency")) {
            ApnContext apnContext2 = this.mApnContextsById.get(9);
            apnContext = apnContext2;
        } else if (TextUtils.equals(apnType, ImsSwitchController.IMS_SERVICE)) {
            ApnContext apnContext3 = this.mApnContextsById.get(5);
            apnContext = apnContext3;
        } else {
            log("apnType is invalid, return null");
            return null;
        }
        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }
        DcAsyncChannel dcac = apnContext.getDcAc();
        if (dcac == null) {
            return null;
        }
        String[] result = dcac.getPcscfAddr();
        for (int i = 0; i < result.length; i++) {
            log("Pcscf[" + i + "]: " + result[i]);
        }
        return result;
    }

    private void initEmergencyApnSetting() {
        Cursor cursor = this.mPhone.getContext().getContentResolver().query(Telephony.Carriers.CONTENT_URI, null, "type=\"emergency\"", null, null);
        if (cursor == null) {
            return;
        }
        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
            this.mEmergencyApn = makeApnSetting(cursor);
        }
        cursor.close();
    }

    private void addEmergencyApnSetting() {
        if (this.mEmergencyApn == null) {
            return;
        }
        if (this.mAllApnSettings == null) {
            this.mAllApnSettings = new ArrayList<>();
            return;
        }
        boolean hasEmergencyApn = false;
        Iterator apn$iterator = this.mAllApnSettings.iterator();
        while (true) {
            if (!apn$iterator.hasNext()) {
                break;
            }
            ApnSetting apn = (ApnSetting) apn$iterator.next();
            if (ArrayUtils.contains(apn.types, "emergency")) {
                hasEmergencyApn = true;
                break;
            }
        }
        if (!hasEmergencyApn) {
            this.mAllApnSettings.add(this.mEmergencyApn);
        } else {
            log("addEmergencyApnSetting - E-APN setting is already present");
        }
    }

    private void cleanUpConnectionsOnUpdatedApns(boolean tearDown) {
        if (DBG) {
            log("cleanUpConnectionsOnUpdatedApns: tearDown=" + tearDown);
        }
        if (this.mAllApnSettings.isEmpty()) {
            cleanUpAllConnections(tearDown, PhoneInternalInterface.REASON_APN_CHANGED);
        } else {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (VDBG) {
                    log("cleanUpConnectionsOnUpdatedApns for " + apnContext);
                }
                boolean cleanUpApn = true;
                ArrayList<ApnSetting> currentWaitingApns = apnContext.getWaitingApns();
                if (currentWaitingApns != null && !apnContext.isDisconnected()) {
                    int radioTech = this.mPhone.getServiceState().getRilDataRadioTechnology();
                    ArrayList<ApnSetting> waitingApns = buildWaitingApns(apnContext.getApnType(), radioTech);
                    if (VDBG) {
                        log("new waitingApns:" + waitingApns);
                    }
                    if (waitingApns.size() == currentWaitingApns.size()) {
                        cleanUpApn = false;
                        int i = 0;
                        while (true) {
                            if (i >= waitingApns.size()) {
                                break;
                            }
                            String currentWaitingApn = currentWaitingApns.get(i).toStringIgnoreName(!VZW_FEATURE);
                            String waitingApn = waitingApns.get(i).toStringIgnoreName(!VZW_FEATURE);
                            if (TextUtils.equals(currentWaitingApn, waitingApn)) {
                                i++;
                            } else {
                                if (VDBG) {
                                    log("new waiting apn is different at " + i);
                                }
                                cleanUpApn = true;
                                apnContext.setWaitingApns(waitingApns);
                            }
                        }
                    }
                }
                if (cleanUpApn) {
                    apnContext.setReason(PhoneInternalInterface.REASON_APN_CHANGED);
                    cleanUpConnection(true, apnContext);
                }
            }
        }
        if (!isConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }
        this.mRequestedApnType = "default";
        if (DBG) {
            log("mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        }
        if (!tearDown || this.mDisconnectPendingCount != 0) {
            return;
        }
        notifyDataDisconnectComplete();
        notifyAllDataDisconnected();
    }

    private void createWorkerHandler() {
        if (this.mWorkerHandler != null) {
            return;
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                DcTracker.this.mWorkerHandler = new WorkerHandler(DcTracker.this, null);
                DcTracker.this.mWorkerHandler.sendEmptyMessage(270838);
                Looper.loop();
            }
        };
        thread.start();
    }

    private class WorkerHandler extends Handler {
        WorkerHandler(DcTracker this$0, WorkerHandler workerHandler) {
            this();
        }

        private WorkerHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 270838:
                    DcTracker.this.logd("start loading pdn info");
                    DcTracker.this.initEmergencyApnSetting();
                    DcTracker.this.addEmergencyApnSetting();
                    break;
            }
        }
    }

    protected int getPdpConnectionPoolSize() {
        if (isSupportThrottlingApn()) {
            return 8;
        }
        return 2;
    }

    private boolean isSupportThrottlingApn() {
        return THROTTLING_APN_ENABLED || SystemProperties.getInt(PROPERTY_THROTTLING_APN_ENABLED, 0) == 1;
    }

    private int generateDataConnectionId(String reqApnType, ApnSetting apnSetting) {
        int i = 0;
        AtomicInteger idGenerator = this.mOthersUniqueIdGenerator;
        String[] strArr = HIGH_THROUGHPUT_APN;
        int length = strArr.length;
        int i2 = 0;
        while (true) {
            if (i2 >= length) {
                break;
            }
            String apn = strArr[i2];
            if (apnSetting == null || !apnSetting.canHandleType(apn) || "emergency".equals(reqApnType) || apnSetting.canHandleType(ImsSwitchController.IMS_SERVICE)) {
                i2++;
            } else {
                idGenerator = this.mHighThroughputIdGenerator;
                logd("generateDataConnectionId use high throughput DataConnection id generator");
                break;
            }
        }
        if (idGenerator != this.mHighThroughputIdGenerator) {
            String[] strArr2 = IMS_APN;
            int length2 = strArr2.length;
            while (true) {
                if (i >= length2) {
                    break;
                }
                String apn2 = strArr2[i];
                if (("emergency".equals(apn2) && !"emergency".equals(reqApnType)) || apnSetting == null || !apnSetting.canHandleType(apn2)) {
                    i++;
                } else {
                    int idStart = 4;
                    if ("emergency".equals(apn2)) {
                        idStart = 5;
                    }
                    this.mImsUniqueIdGenerator.set(idStart);
                    idGenerator = this.mImsUniqueIdGenerator;
                    logd("generateDataConnectionId use ims DataConnection id generator");
                }
            }
        }
        int id = idGenerator.getAndIncrement();
        if (idGenerator == this.mHighThroughputIdGenerator && id > 1) {
            loge("Max id of highthrouthput is 1, but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        } else if (idGenerator == this.mOthersUniqueIdGenerator && id > 3) {
            loge("Max id of others is 3, but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        } else if (idGenerator == this.mImsUniqueIdGenerator && id > 6) {
            loge("Max id of others is 6, but generated id is " + id);
            idGenerator.getAndDecrement();
            id = -1;
        }
        log("generateDataConnectionId id = " + id);
        return id;
    }

    public void deactivatePdpByCid(int cid) {
        this.mPhone.mCi.deactivateDataCall(cid, 2, obtainMessage(270843, cid, 0));
    }

    public boolean isVsimActive(int phoneId) {
        int phoneNum = TelephonyManager.getDefault().getPhoneCount();
        for (int id = 0; id < phoneNum; id++) {
            if (id != phoneId) {
                TelephonyManager.getDefault();
                String vsimEnabled = TelephonyManager.getTelephonyProperty(id, PROPERTY_VSIM_ENABLE, "0");
                int act = vsimEnabled.isEmpty() ? 0 : Integer.parseInt(vsimEnabled);
                if (act == 2) {
                    logd("Remote Vsim enabled on phone " + id + " and downloaded by phone" + phoneId);
                    return true;
                }
            }
        }
        return false;
    }

    private void syncApnToMd() {
        logv("syncApnToMd");
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            int throttlingTime = SystemProperties.getInt(PROPERTY_THROTTLING_TIME, THROTTLING_TIME_DEFAULT);
            String hPlmn = TelephonyManager.getDefault().getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
            logd("syncApnToMd: hPlmn = " + hPlmn + ", HPLMN_OP12 = " + PLMN_OP12);
            if (!PLMN_OP12.equals(hPlmn) && !MTK_APNSYNC_TEST_SUPPORT) {
                return;
            }
            for (int i = 0; i < this.mAllApnSettings.size(); i++) {
                ApnSetting apn = this.mAllApnSettings.get(i);
                this.mPhone.mCi.syncApnTable(String.valueOf(i), String.valueOf(getClassType(apn)), apn.apn, apn.protocol, "LTE", apn.carrierEnabled ? "Enabled" : "Disabled", "0", String.valueOf(apn.maxConns), String.valueOf(apn.maxConnsTime), String.valueOf(apn.waitTime), String.valueOf(throttlingTime), String.valueOf(apn.inactiveTimer), null);
            }
            return;
        }
        logw("syncApnToMd: All ApnSettings are null or empty!");
    }

    public int getClassType(ApnSetting apn) {
        int classType = 3;
        if (ArrayUtils.contains(apn.types, "emergency") || VZW_EMERGENCY_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = 0;
        } else if (ArrayUtils.contains(apn.types, ImsSwitchController.IMS_SERVICE) || VZW_IMS_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = 1;
        } else if (VZW_ADMIN_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = 2;
        } else if (VZW_APP_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = 4;
        } else if (VZW_800_NI.compareToIgnoreCase(apn.apn) == 0) {
            classType = 5;
        } else if (ArrayUtils.contains(apn.types, "default")) {
            classType = 3;
        } else {
            log("getClassType: set to default class 3");
        }
        logd("getClassType:" + classType);
        return classType;
    }

    public ApnSetting getClassTypeApn(int classType) {
        String apnName;
        ApnSetting classTypeApn = null;
        if (classType == 0) {
            apnName = VZW_EMERGENCY_NI;
        } else if (1 == classType) {
            apnName = VZW_IMS_NI;
        } else if (2 == classType) {
            apnName = VZW_ADMIN_NI;
        } else if (3 == classType) {
            apnName = VZW_INTERNET_NI;
        } else if (4 == classType) {
            apnName = VZW_APP_NI;
        } else if (5 == classType) {
            apnName = VZW_800_NI;
        } else {
            log("getClassTypeApn: can't handle class:" + classType);
            return null;
        }
        if (this.mAllApnSettings != null) {
            for (ApnSetting apn : this.mAllApnSettings) {
                if (apnName.compareToIgnoreCase(apn.apn) == 0) {
                    classTypeApn = apn;
                }
            }
        }
        logd("getClassTypeApn:" + classTypeApn + ", class:" + classType);
        return classTypeApn;
    }

    private void onSharedDefaultApnState(int newDefaultRefCount) {
        logd("onSharedDefaultApnState: newDefaultRefCount = " + newDefaultRefCount + ", curDefaultRefCount = " + this.mDefaultRefCount);
        if (newDefaultRefCount == this.mDefaultRefCount) {
            return;
        }
        if (newDefaultRefCount > 1) {
            this.mIsSharedDefaultApn = true;
        } else {
            this.mIsSharedDefaultApn = false;
        }
        this.mDefaultRefCount = newDefaultRefCount;
        logd("onSharedDefaultApnState: mIsSharedDefaultApn = " + this.mIsSharedDefaultApn);
        notifySharedDefaultApn(this.mIsSharedDefaultApn);
    }

    public void onSetLteAccessStratumReport(boolean enabled, Message response) {
        this.mPhone.mCi.setLteAccessStratumReport(enabled, response);
    }

    public void onSetLteUplinkDataTransfer(int timeMillis, Message response) {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if ("default".equals(apnContext.getApnType())) {
                try {
                    int interfaceId = apnContext.getDcAc().getCidSync();
                    this.mPhone.mCi.setLteUplinkDataTransfer(timeMillis, interfaceId, response);
                } catch (Exception e) {
                    loge("getDcAc fail!");
                    e.printStackTrace();
                    if (response != null) {
                        AsyncResult.forMessage(response, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                        response.sendToTarget();
                    }
                }
            }
        }
    }

    private void notifySharedDefaultApn(boolean isSharedDefaultApn) {
        this.mPhone.notifySharedDefaultApnStateChanged(isSharedDefaultApn);
    }

    private void notifyLteAccessStratumChanged(int lteAccessStratumDataState) {
        String str;
        if (lteAccessStratumDataState == 1) {
            str = "connected";
        } else {
            str = "idle";
        }
        this.mLteAccessStratumDataState = str;
        logd("notifyLteAccessStratumChanged mLteAccessStratumDataState = " + this.mLteAccessStratumDataState);
        this.mPhone.notifyLteAccessStratumChanged(this.mLteAccessStratumDataState);
    }

    private void notifyPsNetworkTypeChanged(int newRilNwType) {
        int newNwType = this.mPhone.getServiceState().rilRadioTechnologyToNetworkTypeEx(newRilNwType);
        logd("notifyPsNetworkTypeChanged mNetworkType = " + this.mNetworkType + ", newNwType = " + newNwType + ", newRilNwType = " + newRilNwType);
        if (newNwType == this.mNetworkType) {
            return;
        }
        this.mNetworkType = newNwType;
        this.mPhone.notifyPsNetworkTypeChanged(this.mNetworkType);
    }

    public String getLteAccessStratumState() {
        return this.mLteAccessStratumDataState;
    }

    public boolean isSharedDefaultApn() {
        return this.mIsSharedDefaultApn;
    }

    private void resetPollStats() {
        this.mTxPkts = -1L;
        this.mRxPkts = -1L;
        this.mNetStatPollPeriod = 1000;
    }

    private void startNetStatPoll() {
        if (getOverallState() == DctConstants.State.CONNECTED && !this.mNetStatPollEnabled) {
            if (DBG) {
                log("startNetStatPoll");
            }
            resetPollStats();
            this.mNetStatPollEnabled = true;
            this.mPollNetStat.run();
        }
        if (this.mPhone == null) {
            return;
        }
        this.mPhone.notifyDataActivity();
    }

    private void stopNetStatPoll() {
        this.mNetStatPollEnabled = false;
        removeCallbacks(this.mPollNetStat);
        if (DBG) {
            log("stopNetStatPoll");
        }
        if (this.mPhone == null) {
            return;
        }
        this.mPhone.notifyDataActivity();
    }

    public void sendStartNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(270376);
        msg.arg1 = 1;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStartNetStatPoll(DctConstants.Activity activity) {
        startNetStatPoll();
        startDataStallAlarm(false);
        setActivity(activity);
    }

    public void sendStopNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(270376);
        msg.arg1 = 0;
        msg.obj = activity;
        sendMessage(msg);
    }

    private void handleStopNetStatPoll(DctConstants.Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    private void updateDataActivity() {
        DctConstants.Activity newActivity;
        TxRxSum preTxRxSum = new TxRxSum(this.mTxPkts, this.mRxPkts);
        TxRxSum curTxRxSum = new TxRxSum();
        String strOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (TextUtils.equals(strOperatorNumeric, "732101")) {
            curTxRxSum.updateTxRxSum();
        } else {
            curTxRxSum.updateTcpTxRxSum();
        }
        this.mTxPkts = curTxRxSum.txPkts;
        this.mRxPkts = curTxRxSum.rxPkts;
        if (VDBG) {
            log("updateDataActivity: curTxRxSum=" + curTxRxSum + " preTxRxSum=" + preTxRxSum);
        }
        if (this.mNetStatPollEnabled) {
            if (preTxRxSum.txPkts <= 0 && preTxRxSum.rxPkts <= 0) {
                return;
            }
            long sent = this.mTxPkts - preTxRxSum.txPkts;
            long received = this.mRxPkts - preTxRxSum.rxPkts;
            if (VDBG) {
                log("updateDataActivity: sent=" + sent + " received=" + received);
            }
            if (sent > 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAINANDOUT;
            } else if (sent > 0 && received == 0) {
                newActivity = DctConstants.Activity.DATAOUT;
            } else if (sent == 0 && received > 0) {
                newActivity = DctConstants.Activity.DATAIN;
            } else {
                newActivity = this.mActivity == DctConstants.Activity.DORMANT ? this.mActivity : DctConstants.Activity.NONE;
            }
            if (this.mActivity == newActivity || !this.mIsScreenOn) {
                return;
            }
            if (VDBG) {
                log("updateDataActivity: newActivity=" + newActivity);
            }
            this.mActivity = newActivity;
            this.mPhone.notifyDataActivity();
        }
    }

    private static class RecoveryAction {
        public static final int CLEANUP = 1;
        public static final int GET_DATA_CALL_LIST = 0;
        public static final int RADIO_RESTART = 3;
        public static final int RADIO_RESTART_WITH_PROP = 4;
        public static final int REREGISTER = 2;

        private RecoveryAction() {
        }

        private static boolean isAggressiveRecovery(int value) {
            return value == 1 || value == 2 || value == 3 || value == 4;
        }
    }

    private int getRecoveryAction() {
        int action = Settings.System.getInt(this.mResolver, "radio.data.stall.recovery.action", 0);
        if (VDBG_STALL) {
            log("getRecoveryAction: " + action);
        }
        return action;
    }

    private void putRecoveryAction(int action) {
        Settings.System.putInt(this.mResolver, "radio.data.stall.recovery.action", action);
        if (VDBG_STALL) {
            log("putRecoveryAction: " + action);
        }
    }

    private void doRecovery() {
        if (getOverallState() != DctConstants.State.CONNECTED) {
            return;
        }
        int recoveryAction = getRecoveryAction();
        switch (recoveryAction) {
            case 0:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST, this.mSentSinceLastRecv);
                if (DBG) {
                    log("doRecovery() get data call list");
                }
                this.mPhone.mCi.getDataCallList(obtainMessage(270340));
                putRecoveryAction(1);
                break;
            case 1:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP, this.mSentSinceLastRecv);
                Intent intent = new Intent("com.mediatek.log2server.EXCEPTION_HAPPEND");
                intent.putExtra("Reason", "SmartLogging");
                intent.putExtra("from_where", LOG_TAG);
                this.mPhone.getContext().sendBroadcast(intent);
                log("Broadcast for SmartLogging - NO DATA");
                if (DBG) {
                    log("doRecovery() cleanup all connections");
                }
                cleanUpAllConnections(PhoneInternalInterface.REASON_PDP_RESET);
                putRecoveryAction(2);
                break;
            case 2:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_REREGISTER, this.mSentSinceLastRecv);
                if (DBG) {
                    log("doRecovery() re-register");
                }
                this.mPhone.getServiceStateTracker().reRegisterNetwork(null);
                putRecoveryAction(3);
                break;
            case 3:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART, this.mSentSinceLastRecv);
                if (DBG) {
                    log("restarting radio");
                }
                putRecoveryAction(4);
                restartRadio();
                break;
            case 4:
                EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART_WITH_PROP, -1);
                if (DBG) {
                    log("restarting radio with gsm.radioreset to true");
                }
                SystemProperties.set(this.RADIO_RESET_PROPERTY, "true");
                try {
                    Thread.sleep(1000L);
                    break;
                } catch (InterruptedException e) {
                }
                restartRadio();
                putRecoveryAction(0);
                break;
            default:
                throw new RuntimeException("doRecovery: Invalid recoveryAction=" + recoveryAction);
        }
        this.mSentSinceLastRecv = 0L;
    }

    private void updateDataStallInfo() {
        TxRxSum preTxRxSum = new TxRxSum(this.mDataStallTxRxSum);
        String strOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (TextUtils.equals(strOperatorNumeric, "732101")) {
            this.mDataStallTxRxSum.updateTxRxSum();
        } else {
            this.mDataStallTxRxSum.updateTcpTxRxSum();
        }
        if (VDBG_STALL) {
            log("updateDataStallInfo: mDataStallTxRxSum=" + this.mDataStallTxRxSum + " preTxRxSum=" + preTxRxSum);
        }
        long sent = this.mDataStallTxRxSum.txPkts - preTxRxSum.txPkts;
        long received = this.mDataStallTxRxSum.rxPkts - preTxRxSum.rxPkts;
        if (sent > 0 && received > 0) {
            if (VDBG_STALL) {
                log("updateDataStallInfo: IN/OUT");
            }
            this.mSentSinceLastRecv = 0L;
            putRecoveryAction(0);
            return;
        }
        if (sent > 0 && received == 0) {
            if (this.mPhone.getState() == PhoneConstants.State.IDLE) {
                this.mSentSinceLastRecv += sent;
            } else {
                this.mSentSinceLastRecv = 0L;
            }
            if (!DBG) {
                return;
            }
            log("updateDataStallInfo: OUT sent=" + sent + " mSentSinceLastRecv=" + this.mSentSinceLastRecv);
            return;
        }
        if (sent == 0 && received > 0) {
            if (VDBG_STALL) {
                log("updateDataStallInfo: IN");
            }
            this.mSentSinceLastRecv = 0L;
            putRecoveryAction(0);
            return;
        }
        if (VDBG_STALL) {
            log("updateDataStallInfo: NONE");
        }
    }

    private void onDataStallAlarm(int tag) {
        if (this.mDataStallAlarmTag != tag) {
            if (DBG) {
                log("onDataStallAlarm: ignore, tag=" + tag + " expecting " + this.mDataStallAlarmTag);
                return;
            }
            return;
        }
        updateDataStallInfo();
        int hangWatchdogTrigger = Settings.Global.getInt(this.mResolver, "pdp_watchdog_trigger_packet_count", 10);
        boolean suspectedStall = false;
        if (this.mSentSinceLastRecv >= hangWatchdogTrigger) {
            if (DBG) {
                log("onDataStallAlarm: tag=" + tag + " do recovery action=" + getRecoveryAction());
            }
            if (isOnlyIMSorEIMSPdnConnected() || skipDataStallAlarm()) {
                log("onDataStallAlarm: only IMS or EIMS Connected, or switch data-stall off, skip it!");
            } else {
                suspectedStall = true;
                sendMessage(obtainMessage(270354));
            }
        } else if (VDBG_STALL) {
            log("onDataStallAlarm: tag=" + tag + " Sent " + String.valueOf(this.mSentSinceLastRecv) + " pkts since last received, < watchdogTrigger=" + hangWatchdogTrigger);
        }
        startDataStallAlarm(suspectedStall);
    }

    private void startDataStallAlarm(boolean suspectedStall) {
        int nextAction = getRecoveryAction();
        if (!this.mDataStallDetectionEnabled || getOverallState() != DctConstants.State.CONNECTED) {
            if (VDBG_STALL) {
                log("startDataStallAlarm: NOT started, no connection tag=" + this.mDataStallAlarmTag);
                return;
            }
            return;
        }
        int delayInMs = (this.mIsScreenOn || suspectedStall || RecoveryAction.isAggressiveRecovery(nextAction)) ? Settings.Global.getInt(this.mResolver, "data_stall_alarm_aggressive_delay_in_ms", 60000) : Settings.Global.getInt(this.mResolver, "data_stall_alarm_non_aggressive_delay_in_ms", DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
        this.mDataStallAlarmTag++;
        if (VDBG_STALL) {
            log("startDataStallAlarm: tag=" + this.mDataStallAlarmTag + " delay=" + (delayInMs / 1000) + "s");
        }
        Intent intent = new Intent(INTENT_DATA_STALL_ALARM);
        intent.putExtra(DATA_STALL_ALARM_TAG_EXTRA, this.mDataStallAlarmTag);
        this.mDataStallAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) delayInMs), this.mDataStallAlarmIntent);
    }

    private void stopDataStallAlarm() {
        if (VDBG_STALL) {
            log("stopDataStallAlarm: current tag=" + this.mDataStallAlarmTag + " mDataStallAlarmIntent=" + this.mDataStallAlarmIntent);
        }
        this.mDataStallAlarmTag++;
        if (this.mDataStallAlarmIntent == null) {
            return;
        }
        this.mAlarmManager.cancel(this.mDataStallAlarmIntent);
        this.mDataStallAlarmIntent = null;
    }

    private void restartDataStallAlarm() {
        if (isConnected()) {
            int nextAction = getRecoveryAction();
            if (RecoveryAction.isAggressiveRecovery(nextAction)) {
                if (DBG) {
                    log("restartDataStallAlarm: action is pending. not resetting the alarm.");
                }
            } else {
                if (VDBG_STALL) {
                    log("restartDataStallAlarm: stop then start.");
                }
                stopDataStallAlarm();
                startDataStallAlarm(false);
            }
        }
    }

    private void onActionIntentProvisioningApnAlarm(Intent intent) {
        if (DBG) {
            log("onActionIntentProvisioningApnAlarm: action=" + intent.getAction());
        }
        Message msg = obtainMessage(270375, intent.getAction());
        msg.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    private void startProvisioningApnAlarm() {
        int delayInMs = Settings.Global.getInt(this.mResolver, "provisioning_apn_alarm_delay_in_ms", PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT);
        if (Build.IS_DEBUGGABLE) {
            String delayInMsStrg = Integer.toString(delayInMs);
            try {
                delayInMs = Integer.parseInt(System.getProperty(DEBUG_PROV_APN_ALARM, delayInMsStrg));
            } catch (NumberFormatException e) {
                loge("startProvisioningApnAlarm: e=" + e);
            }
        }
        this.mProvisioningApnAlarmTag++;
        if (DBG) {
            log("startProvisioningApnAlarm: tag=" + this.mProvisioningApnAlarmTag + " delay=" + (delayInMs / 1000) + "s");
        }
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, this.mProvisioningApnAlarmTag);
        this.mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) delayInMs), this.mProvisioningApnAlarmIntent);
    }

    private void stopProvisioningApnAlarm() {
        if (DBG) {
            log("stopProvisioningApnAlarm: current tag=" + this.mProvisioningApnAlarmTag + " mProvsioningApnAlarmIntent=" + this.mProvisioningApnAlarmIntent);
        }
        this.mProvisioningApnAlarmTag++;
        if (this.mProvisioningApnAlarmIntent == null) {
            return;
        }
        this.mAlarmManager.cancel(this.mProvisioningApnAlarmIntent);
        this.mProvisioningApnAlarmIntent = null;
    }

    public boolean isOnlyIMSorEIMSPdnConnected() {
        boolean bIsOnlyIMSorEIMSConnected = false;
        if (!MTK_IMS_SUPPORT) {
            return false;
        }
        for (ApnContext apnContext : this.mApnContexts.values()) {
            String apnType = apnContext.getApnType();
            if (!apnContext.isDisconnected()) {
                if (!apnType.equals(ImsSwitchController.IMS_SERVICE) && !apnType.equals("emergency")) {
                    logd("apnType: " + apnType + " is still conntected!!");
                    return false;
                }
                bIsOnlyIMSorEIMSConnected = true;
            }
        }
        return bIsOnlyIMSorEIMSConnected;
    }

    private String getIMSApnSetting(ArrayList<ApnSetting> apnSettings) {
        if (apnSettings == null || apnSettings.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ApnSetting t : apnSettings) {
            if (t.canHandleType(ImsSwitchController.IMS_SERVICE)) {
                sb.append(apnToStringIgnoreName(t));
            }
        }
        logd("getIMSApnSetting, apnsToStringIgnoreName: sb = " + sb.toString());
        return sb.toString();
    }

    private boolean isIMSApnSettingChanged(ArrayList<ApnSetting> prevApnList, ArrayList<ApnSetting> currApnList) {
        String prevIMSApn = getIMSApnSetting(prevApnList);
        String currIMSApn = getIMSApnSetting(currApnList);
        if (prevIMSApn.isEmpty() || TextUtils.equals(prevIMSApn, currIMSApn)) {
            return false;
        }
        return true;
    }

    private String apnToStringIgnoreName(ApnSetting apnSetting) {
        if (apnSetting == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(apnSetting.id).append(", ").append(apnSetting.numeric).append(", ").append(apnSetting.apn).append(", ").append(apnSetting.proxy).append(", ").append(apnSetting.mmsc).append(", ").append(apnSetting.mmsProxy).append(", ").append(apnSetting.mmsPort).append(", ").append(apnSetting.port).append(", ").append(apnSetting.authType).append(", ");
        for (int i = 0; i < apnSetting.types.length; i++) {
            sb.append(apnSetting.types[i]);
            if (i < apnSetting.types.length - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(apnSetting.protocol);
        sb.append(", ").append(apnSetting.roamingProtocol);
        sb.append(", ").append(apnSetting.carrierEnabled);
        sb.append(", ").append(apnSetting.bearerBitmask);
        logd("apnToStringIgnoreName: sb = " + sb.toString());
        return sb.toString();
    }

    private boolean isDataAllowedAsOff(String apnType) {
        boolean isDataAllowedAsOff = false;
        if (!BSP_PACKAGE && this.mGsmDctExt != null) {
            isDataAllowedAsOff = this.mGsmDctExt.isDataAllowedAsOff(apnType);
        }
        if (TextUtils.equals(apnType, "default") && isVsimActive(this.mPhone.getPhoneId())) {
            logd("Vsim is enabled, set isDataAllowedAsOff true");
            return true;
        }
        return isDataAllowedAsOff;
    }

    protected void notifyMobileDataChange(int enabled) {
        logd("notifyMobileDataChange, enable = " + enabled);
        Intent intent = new Intent(DataSubSelector.ACTION_MOBILE_DATA_ENABLE);
        intent.putExtra("reason", enabled);
        this.mPhone.getContext().sendBroadcast(intent);
    }

    private void setUserDataProperty(boolean enabled) {
        int phoneId = this.mPhone.getPhoneId();
        String dataOnIccid = "0";
        if (!SubscriptionManager.isValidPhoneId(phoneId)) {
            log("invalid phone id, don't update");
            return;
        }
        if (enabled) {
            dataOnIccid = SystemProperties.get(this.PROPERTY_ICCID[phoneId], "0");
        }
        logd("setUserDataProperty:" + dataOnIccid);
        TelephonyManager.getDefault();
        TelephonyManager.setTelephonyProperty(phoneId, PROPERTY_MOBILE_DATA_ENABLE, dataOnIccid);
    }

    private void handleSetResume() {
        if (SubscriptionManager.isValidPhoneId(this.mPhone.getPhoneId())) {
            this.mPhone.mCi.setResumeRegistration(this.mSuspendId.get(this.mPhone.getPhoneId()).intValue(), null);
        }
    }

    private void handleRegistrationSuspend(AsyncResult ar) {
        if (ar.exception == null && ar.result != null) {
            this.mSuspendId.add(this.mPhone.getPhoneId(), Integer.valueOf(((int[]) ar.result)[0]));
            logd("handleRegistrationSuspend: createAllApnList and set IA APN; suspending with Id=" + this.mSuspendId.get(this.mPhone.getPhoneId()));
            synchronized (this.mNeedsResumeModemLock) {
                this.mNeedsResumeModem = true;
            }
            createAllApnList();
            setInitialAttachApn();
            return;
        }
        log("handleRegistrationSuspend: AsyncResult is wrong " + ar.exception);
    }

    private void handlePlmnChange(AsyncResult ar) {
        if (ar.exception == null && ar.result != null) {
            String[] plmnString = (String[]) ar.result;
            this.mPlmnStrings = plmnString;
            for (int i = 0; i < plmnString.length; i++) {
                logd("plmnString[" + i + "]=" + plmnString[i]);
            }
            this.mRegion = getRegion(plmnString[0]);
            IccRecords r = this.mIccRecords.get();
            String operator = r != null ? r.getOperatorNumeric() : "";
            if (operator != null && operator.length() > 0 && !NEED_TO_RESUME_MODEM && this.mPhone.getPhoneId() == SubscriptionManager.getPhoneId(SubscriptionController.getInstance().getDefaultDataSubId())) {
                logd("handlePlmnChange: createAllApnList and set initial attach APN");
                createAllApnList();
                setInitialAttachApn();
                return;
            }
            logd("No need to update APN for Operator");
            return;
        }
        log("AsyncResult is wrong " + ar.exception);
    }

    private int getRegion(String plmn) {
        if (plmn == null || plmn.equals("") || plmn.length() < 5) {
            logd("[getRegion] Invalid PLMN");
            return 0;
        }
        String currentMcc = plmn.substring(0, 3);
        for (String mcc : MCC_TABLE_TEST) {
            if (currentMcc.equals(mcc)) {
                logd("[getRegion] Test PLMN");
                return 0;
            }
        }
        String[] strArr = MCC_TABLE_DOMESTIC;
        if (strArr.length > 0) {
            String mcc2 = strArr[0];
            if (currentMcc.equals(mcc2)) {
                logd("[getRegion] REGION_DOMESTIC");
                return 1;
            }
            logd("[getRegion] REGION_FOREIGN");
            return 2;
        }
        logd("[getRegion] REGION_UNKNOWN");
        return 0;
    }

    public boolean getImsEnabled() {
        boolean isImsEnabled = Settings.Global.getInt(this.mResolver, "volte_vt_enabled", 0) != 0;
        logd("getImsEnabled: getInt isImsEnabled=" + isImsEnabled);
        return isImsEnabled;
    }

    private void syncApnTableToRds(ArrayList<ApnSetting> apnlist) {
        log("syncApnTableToRds: E");
        if (apnlist != null && apnlist.size() > 0) {
            ArrayList<String> aryApn = new ArrayList<>();
            for (int i = 0; i < apnlist.size(); i++) {
                ApnSetting apn = apnlist.get(i);
                if (TextUtils.isEmpty(apn.apn)) {
                    log("syncApnTableToRds: apn name is empty");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(apn.apn);
                    sb.append(";");
                    int numOfProfileId = 0;
                    for (int j = 0; j < apn.types.length; j++) {
                        int profileId = getApnProfileID(apn.types[j]);
                        if (profileId != -1) {
                            if (numOfProfileId > 0) {
                                sb.append("|");
                            }
                            sb.append(profileId);
                            numOfProfileId++;
                        }
                    }
                    sb.append(";");
                    int rat = getApnRatByBearer(apn.bearerBitmask);
                    log("apn.rat: " + rat);
                    sb.append(rat);
                    sb.append(";");
                    sb.append(apn.protocol);
                    log("syncApnTableToRds: apn: " + sb.toString());
                    aryApn.add(sb.toString());
                }
            }
            if (aryApn.size() > 0) {
                this.mPhone.mCi.syncApnTableToRds((String[]) aryApn.toArray(new String[aryApn.size()]), null);
            }
        }
        log("syncApnTableToRds: X");
    }

    private int getApnRatByBearer(int bearerBitMask) {
        log("getApnRatByBearer: " + bearerBitMask);
        if (bearerBitMask == 0 || !ServiceState.bitmaskHasTech(bearerBitMask, 18)) {
            return 1;
        }
        if (isWifiOnlyApn(bearerBitMask)) {
            return 2;
        }
        return 3;
    }

    private boolean isWifiOnlyApn(int bearerBitMask) {
        return bearerBitMask != 0 && (16646143 & bearerBitMask) == 0;
    }

    public boolean checkIfDomesticInitialAttachApn(String currentMcc) {
        boolean isMccDomestic = false;
        String[] strArr = MCC_TABLE_DOMESTIC;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            }
            String mcc = strArr[i];
            if (!currentMcc.equals(mcc)) {
                i++;
            } else {
                isMccDomestic = true;
                break;
            }
        }
        if (MTK_OP17_IA_SUPPORT && isMccDomestic) {
            return getImsEnabled() && this.mRegion == 1;
        }
        if (MTK_OP129_IA_SUPPORT && isMccDomestic) {
            return this.mRegion == 1;
        }
        logd("checkIfDomesticInitialAttachApn: Not OP17/OP129 or MCC is not in domestic");
        return true;
    }

    private void onPcoStatus(AsyncResult ar) {
        if (ar.exception != null) {
            loge("onPcoStatus exception: " + ar.exception);
            return;
        }
        int[] aryPcoStatus = (int[]) ar.result;
        if (aryPcoStatus == null || aryPcoStatus.length != 6) {
            logw("onPcoStatus: pco status is null");
            return;
        }
        log("onPcoStatus: PCO_MCC = " + aryPcoStatus[0] + ", PCO_MNC = " + aryPcoStatus[1] + ", PCO_VAL = " + aryPcoStatus[2] + ", PCO_TECH = " + aryPcoStatus[3] + ", PCO_PDN_ID = " + aryPcoStatus[5]);
        DcAsyncChannel dcac = this.mDataConnectionAcHashMap.get(Integer.valueOf(aryPcoStatus[5]));
        if (dcac != null) {
            String[] aryApnType = dcac.getApnTypeSync();
            if (aryApnType == null) {
                logw("onPcoStatus: dcac.getApnTypeSync() return null");
                return;
            }
            for (String apnType : aryApnType) {
                Intent intent = new Intent("com.mediatek.intent.action.ACTION_PCO_STATUS");
                intent.putExtra(APN_TYPE_KEY, apnType);
                intent.putExtra("pcoType", aryPcoStatus[2]);
                this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            }
        }
    }

    private void onAllowChanged(boolean allow) {
        if (DBG) {
            log("onAllowChanged: Allow = " + allow);
        }
        this.mAllowConfig = allow;
        if (!allow) {
            return;
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_DATA_ALLOWED);
    }

    private boolean getAllowConfig() {
        DataConnectionHelper.getInstance();
        if (!DataConnectionHelper.isMultiPsAttachSupport()) {
            return true;
        }
        return this.mAllowConfig;
    }

    private boolean isPermanentFailByOp(DcFailCause dcFailCause, DcFailCauseManager.Operator op) {
        if (this.mDcFcMgr == null) {
            loge("mDcFcMgr should not be null, something wrong");
            return true;
        }
        boolean isPermanent = this.mDcFcMgr.isPermanentFailByOp(dcFailCause, op);
        return isPermanent;
    }

    private void syncDataSettingsToMd(boolean dataEnabled, boolean dataRoamingEnabled) {
        logd("syncDataSettingsToMd " + dataEnabled + "," + dataRoamingEnabled);
        this.mPhone.mCi.syncDataSettingsToMd(dataEnabled, dataRoamingEnabled, null);
    }

    private boolean skipDataStallAlarm() {
        boolean isTestSim = false;
        int phoneId = this.mPhone.getPhoneId();
        DataConnectionHelper dcHelper = DataConnectionHelper.getInstance();
        if (SubscriptionManager.isValidPhoneId(phoneId) && dcHelper != null && dcHelper.isTestIccCard(phoneId)) {
            isTestSim = true;
        }
        if (isTestSim) {
            if (SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("0")) {
                return false;
            }
            return true;
        }
        if (SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("1")) {
            return true;
        }
        return false;
    }

    private void notifyVoiceCallEventToDataConnection(boolean bInVoiceCall, boolean bSupportConcurrent) {
        logd("notifyVoiceCallEventToDataConnection: bInVoiceCall = " + bInVoiceCall + ", bSupportConcurrent = " + bSupportConcurrent);
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            dcac.notifyVoiceCallEvent(bInVoiceCall, bSupportConcurrent);
        }
    }

    private boolean isApnSettingExist(ApnSetting apnSetting) {
        if (apnSetting != null && this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            for (ApnSetting apn : this.mAllApnSettings) {
                if (TextUtils.equals(apnSetting.toStringIgnoreName(false), apn.toStringIgnoreName(false))) {
                    log("isApnSettingExist: " + apn);
                    return true;
                }
            }
        }
        return false;
    }

    private void onAttachApnChangedByHandover(boolean isImsHandover) {
        this.mIsImsHandover = isImsHandover;
        log("onAttachApnChangedByHandover: mIsImsHandover = " + this.mIsImsHandover);
        SystemProperties.set(this.PROP_IMS_HANDOVER, this.mIsImsHandover ? "1" : Phone.ACT_TYPE_UTRAN);
        setInitialAttachApn();
    }
}
