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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkConfig;
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
import android.os.Message;
import android.os.PersistableBundle;
import android.os.RegistrantList;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellLocation;
import android.telephony.PcoData;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.data.DataProfile;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HbpcdLookup;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import com.google.android.mms.pdu.CharacterSets;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class DcTracker extends Handler {
    protected static final String APN_ID = "apn_id";
    private static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 60000;
    private static final int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 360000;
    protected static final String DATA_STALL_ALARM_TAG_EXTRA = "data.stall.alram.tag";
    protected static final boolean DATA_STALL_NOT_SUSPECTED = false;
    protected static final boolean DATA_STALL_SUSPECTED = true;
    private static final boolean DBG = true;
    private static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";
    protected static final String INTENT_DATA_STALL_ALARM = "com.android.internal.telephony.data-stall";
    private static final String INTENT_PROVISIONING_APN_ALARM = "com.android.internal.telephony.provisioning_apn_alarm";
    protected static final String INTENT_RECONNECT_ALARM = "com.android.internal.telephony.data-reconnect";
    protected static final String INTENT_RECONNECT_ALARM_EXTRA_REASON = "reconnect_alarm_extra_reason";
    protected static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    private static final String LOG_TAG = "DCT";
    protected static final int NUMBER_SENT_PACKETS_OF_HANG = 10;
    private static final int POLL_NETSTAT_MILLIS = 1000;
    private static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 600000;
    private static final int POLL_PDP_MILLIS = 5000;
    protected static final Uri PREFERAPN_NO_UPDATE_URI_USING_SUBID;
    private static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 900000;
    private static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";
    private static final int PROVISIONING_SPINNER_TIMEOUT_MILLIS = 120000;
    protected static final String PUPPET_MASTER_RADIO_STRESS_TEST = "gsm.defaultpdpcontext.active";
    private static final boolean RADIO_TESTS = false;
    private static final boolean VDBG = false;
    private static final boolean VDBG_STALL = false;
    private static int sEnableFailFastRefCounter;
    private static Method sMethodUpdateTxRxSumEx;
    public AtomicBoolean isCleanupRequired;
    protected DctConstants.Activity mActivity;
    protected final AlarmManager mAlarmManager;
    protected ArrayList<ApnSetting> mAllApnSettings;
    private RegistrantList mAllDataDisconnectedRegistrants;
    protected final ConcurrentHashMap<String, ApnContext> mApnContexts;
    protected final SparseArray<ApnContext> mApnContextsById;
    protected ApnChangeObserver mApnObserver;
    private HashMap<String, Integer> mApnToDataConnectionId;
    protected AtomicBoolean mAttached;
    protected AtomicBoolean mAutoAttachOnCreation;
    protected boolean mAutoAttachOnCreationConfig;
    protected boolean mCanSetPreferApn;
    private final ConnectivityManager mCm;
    protected HashMap<Integer, DcAsyncChannel> mDataConnectionAcHashMap;
    protected final Handler mDataConnectionTracker;
    protected HashMap<Integer, DataConnection> mDataConnections;
    protected final DataEnabledSettings mDataEnabledSettings;
    private final LocalLog mDataRoamingLeakageLog;
    protected final DataServiceManager mDataServiceManager;
    protected PendingIntent mDataStallAlarmIntent;
    protected int mDataStallAlarmTag;
    private volatile boolean mDataStallDetectionEnabled;
    private TxRxSum mDataStallTxRxSum;
    protected DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    protected DcController mDcc;
    private ArrayList<Message> mDisconnectAllCompleteMsgList;
    protected int mDisconnectPendingCount;
    protected ApnSetting mEmergencyApn;
    protected volatile boolean mFailFast;
    protected final AtomicReference<IccRecords> mIccRecords;
    protected boolean mInVoiceCall;
    private final BroadcastReceiver mIntentReceiver;
    private boolean mIsDisposed;
    protected boolean mIsProvisioning;
    protected boolean mIsPsRestricted;
    protected boolean mIsScreenOn;
    private boolean mMeteredApnDisabled;
    protected boolean mMvnoMatched;
    protected boolean mNetStatPollEnabled;
    private int mNetStatPollPeriod;
    private int mNoRecvPollCount;
    protected final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener;
    protected final Phone mPhone;
    private final Runnable mPollNetStat;
    protected ApnSetting mPreferredApn;
    protected final PriorityQueue<ApnContext> mPrioritySortedApnContexts;
    protected final String mProvisionActionName;
    protected BroadcastReceiver mProvisionBroadcastReceiver;
    private PendingIntent mProvisioningApnAlarmIntent;
    private int mProvisioningApnAlarmTag;
    private ProgressDialog mProvisioningSpinner;
    private String mProvisioningUrl;
    private PendingIntent mReconnectIntent;
    protected final Object mRefCountLock;
    private AsyncChannel mReplyAc;
    protected String mRequestedApnType;
    protected boolean mReregisterOnReconnectFailure;
    protected ContentResolver mResolver;
    protected long mRxPkts;
    protected long mSentSinceLastRecv;
    private int mSetDataProfileStatus;
    protected final SettingsObserver mSettingsObserver;
    protected DctConstants.State mState;
    protected SubscriptionManager mSubscriptionManager;
    protected final int mTransportType;
    protected long mTxPkts;
    protected final UiccController mUiccController;
    protected AtomicInteger mUniqueIdGenerator;

    protected enum RetryFailures {
        ALWAYS,
        ONLY_ON_CHANGE
    }

    static {
        Class<?> cls;
        try {
            cls = Class.forName("com.mediatek.internal.telephony.dataconnection.MtkDcTracker");
        } catch (Exception e) {
            Rlog.d(LOG_TAG, e.toString());
            cls = null;
        }
        if (cls != null) {
            try {
                sMethodUpdateTxRxSumEx = cls.getDeclaredMethod("updateTxRxSumEx", new Class[0]);
                sMethodUpdateTxRxSumEx.setAccessible(true);
            } catch (Exception e2) {
                Rlog.d(LOG_TAG, e2.toString());
            }
        }
        sEnableFailFastRefCounter = 0;
        PREFERAPN_NO_UPDATE_URI_USING_SUBID = Uri.parse("content://telephony/carriers/preferapn_no_update/subId/");
    }

    protected void registerSettingsObserver() {
        this.mSettingsObserver.unobserve();
        String string = "";
        if (TelephonyManager.getDefault().getSimCount() > 1) {
            string = Integer.toString(this.mPhone.getSubId());
        }
        this.mSettingsObserver.observe(Settings.Global.getUriFor("data_roaming" + string), 270384);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("device_provisioned"), 270379);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("device_provisioning_mobile_data"), 270379);
    }

    public static class TxRxSum {
        public long rxPkts;
        public long txPkts;

        public TxRxSum() {
            reset();
        }

        public TxRxSum(long j, long j2) {
            this.txPkts = j;
            this.rxPkts = j2;
        }

        public TxRxSum(TxRxSum txRxSum) {
            this.txPkts = txRxSum.txPkts;
            this.rxPkts = txRxSum.rxPkts;
        }

        public void reset() {
            this.txPkts = -1L;
            this.rxPkts = -1L;
        }

        public String toString() {
            return "{txSum=" + this.txPkts + " rxSum=" + this.rxPkts + "}";
        }

        public void updateTxRxSum() {
            try {
                if (DcTracker.sMethodUpdateTxRxSumEx != null) {
                    Bundle bundle = (Bundle) DcTracker.sMethodUpdateTxRxSumEx.invoke(null, new Object[0]);
                    if (bundle.getBoolean("useEx")) {
                        this.txPkts = bundle.getLong("txPkts");
                        this.rxPkts = bundle.getLong("rxPkts");
                        return;
                    }
                }
            } catch (Exception e) {
                Rlog.d(DcTracker.LOG_TAG, e.toString());
            }
            this.txPkts = TrafficStats.getMobileTcpTxPackets();
            this.rxPkts = TrafficStats.getMobileTcpRxPackets();
        }
    }

    private void onActionIntentReconnectAlarm(Intent intent) {
        Message messageObtainMessage = obtainMessage(270383);
        messageObtainMessage.setData(intent.getExtras());
        sendMessage(messageObtainMessage);
    }

    private void onDataReconnect(Bundle bundle) {
        String string = bundle.getString(INTENT_RECONNECT_ALARM_EXTRA_REASON);
        String string2 = bundle.getString(INTENT_RECONNECT_ALARM_EXTRA_TYPE);
        int subId = this.mPhone.getSubId();
        int i = bundle.getInt("subscription", -1);
        log("onDataReconnect: currSubId = " + i + " phoneSubId=" + subId);
        if (!SubscriptionManager.isValidSubscriptionId(i) || i != subId) {
            log("receive ReconnectAlarm but subId incorrect, ignore");
            return;
        }
        ApnContext apnContext = this.mApnContexts.get(string2);
        log("onDataReconnect: mState=" + this.mState + " reason=" + string + " apnType=" + string2 + " apnContext=" + apnContext + " mDataConnectionAsyncChannels=" + this.mDataConnectionAcHashMap);
        if (apnContext != null && apnContext.isEnabled()) {
            apnContext.setReason(string);
            DctConstants.State state = apnContext.getState();
            log("onDataReconnect: apnContext state=" + state);
            if (state == DctConstants.State.FAILED || state == DctConstants.State.IDLE) {
                log("onDataReconnect: state is FAILED|IDLE, disassociate");
                DcAsyncChannel dcAc = apnContext.getDcAc();
                if (dcAc != null) {
                    log("onDataReconnect: tearDown apnContext=" + apnContext);
                    dcAc.tearDown(apnContext, "", null);
                }
                apnContext.setDataConnectionAc(null);
                apnContext.setState(DctConstants.State.IDLE);
            } else {
                log("onDataReconnect: keep associated");
            }
            sendMessage(obtainMessage(270339, apnContext));
            apnContext.setReconnectIntent(null);
        }
    }

    private void onActionIntentDataStallAlarm(Intent intent) {
        Message messageObtainMessage = obtainMessage(270353, intent.getAction());
        messageObtainMessage.arg1 = intent.getIntExtra(DATA_STALL_ALARM_TAG_EXTRA, 0);
        sendMessage(messageObtainMessage);
    }

    protected class ApnChangeObserver extends ContentObserver {
        public ApnChangeObserver() {
            super(DcTracker.this.mDataConnectionTracker);
        }

        @Override
        public void onChange(boolean z) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270355));
        }
    }

    public DcTracker(Phone phone, int i) {
        this.isCleanupRequired = new AtomicBoolean(false);
        this.mRequestedApnType = "default";
        this.mRefCountLock = new Object();
        this.mPrioritySortedApnContexts = new PriorityQueue<>(5, new Comparator<ApnContext>() {
            @Override
            public int compare(ApnContext apnContext, ApnContext apnContext2) {
                return apnContext2.priority - apnContext.priority;
            }
        });
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
        this.mDataRoamingLeakageLog = new LocalLog(50);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    DcTracker.this.log("screen on");
                    DcTracker.this.mIsScreenOn = true;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    DcTracker.this.log("screen off");
                    DcTracker.this.mIsScreenOn = false;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.startsWith(DcTracker.INTENT_RECONNECT_ALARM)) {
                    DcTracker.this.log("Reconnect alarm. Previous state was " + DcTracker.this.mState);
                    DcTracker.this.onActionIntentReconnectAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_DATA_STALL_ALARM)) {
                    DcTracker.this.log("Data stall alarm");
                    DcTracker.this.onActionIntentDataStallAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_PROVISIONING_APN_ALARM)) {
                    DcTracker.this.log("Provisioning apn alarm");
                    DcTracker.this.onActionIntentProvisioningApnAlarm(intent);
                    return;
                }
                if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    if (DcTracker.this.mIccRecords.get() != null && DcTracker.this.mIccRecords.get().getRecordsLoaded()) {
                        DcTracker.this.setDefaultDataRoamingEnabled();
                        return;
                    }
                    return;
                }
                DcTracker.this.log("onReceive: Unknown action=" + action);
            }
        };
        this.mPollNetStat = new Runnable() {
            @Override
            public void run() {
                DcTracker.this.updateDataActivity();
                if (DcTracker.this.mIsScreenOn) {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
                } else {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_long_poll_interval_ms", DcTracker.POLL_NETSTAT_SCREEN_OFF_MILLIS);
                }
                if (DcTracker.this.mNetStatPollEnabled) {
                    DcTracker.this.mDataConnectionTracker.postDelayed(this, DcTracker.this.mNetStatPollPeriod);
                }
            }
        };
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            public final AtomicInteger mPreviousSubId = new AtomicInteger(-1);

            @Override
            public void onSubscriptionsChanged() {
                DcTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
                int subId = DcTracker.this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId) && DcTracker.this.mtkIsNeedRegisterSettingsObserver(this.mPreviousSubId.get(), subId)) {
                    DcTracker.this.registerSettingsObserver();
                }
                if (this.mPreviousSubId.getAndSet(subId) != subId && SubscriptionManager.isValidSubscriptionId(subId)) {
                    DcTracker.this.onRecordsLoadedOrSubIdChanged();
                }
            }
        };
        this.mDisconnectAllCompleteMsgList = new ArrayList<>();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference<>();
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
        this.mMeteredApnDisabled = false;
        this.mSetDataProfileStatus = 0;
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mPhone = phone;
        log("DCT.constructor");
        this.mTransportType = i;
        this.mDataServiceManager = new DataServiceManager(phone, i);
        this.mResolver = this.mPhone.getContext().getContentResolver();
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 270369, null);
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mCm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        intentFilter.addAction(INTENT_DATA_STALL_ALARM);
        intentFilter.addAction(INTENT_PROVISIONING_APN_ALARM);
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        this.mDataEnabledSettings = new DataEnabledSettings(phone);
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, intentFilter, null, this.mPhone);
        this.mAutoAttachOnCreation.set(PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext()).getBoolean(Phone.DATA_DISABLED_ON_BOOT_KEY, false));
        this.mSubscriptionManager = SubscriptionManager.from(this.mPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        HandlerThread handlerThread = new HandlerThread("DcHandlerThread");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        this.mDcc = DcController.makeDcc(this.mPhone, this, this.mDataServiceManager, handler);
        this.mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(this.mPhone, handler);
        mtkCopyHandlerThread(handlerThread);
        this.mDataConnectionTracker = this;
        registerForAllEvents();
        update();
        this.mApnObserver = new ApnChangeObserver();
        phone.getContext().getContentResolver().registerContentObserver(Telephony.Carriers.CONTENT_URI, true, this.mApnObserver);
        initApnContexts();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            IntentFilter intentFilter2 = new IntentFilter();
            intentFilter2.addAction("com.android.internal.telephony.data-reconnect." + apnContext.getApnType());
            this.mPhone.getContext().registerReceiver(this.mIntentReceiver, intentFilter2, null, this.mPhone);
        }
        initEmergencyApnSetting();
        addEmergencyApnSetting();
        this.mProvisionActionName = "com.android.internal.telephony.PROVISION" + phone.getPhoneId();
        this.mSettingsObserver = new SettingsObserver(this.mPhone.getContext(), this);
        registerSettingsObserver();
    }

    @VisibleForTesting
    public DcTracker() {
        this.isCleanupRequired = new AtomicBoolean(false);
        this.mRequestedApnType = "default";
        this.mRefCountLock = new Object();
        this.mPrioritySortedApnContexts = new PriorityQueue<>(5, new Comparator<ApnContext>() {
            @Override
            public int compare(ApnContext apnContext, ApnContext apnContext2) {
                return apnContext2.priority - apnContext.priority;
            }
        });
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
        this.mDataRoamingLeakageLog = new LocalLog(50);
        this.mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    DcTracker.this.log("screen on");
                    DcTracker.this.mIsScreenOn = true;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    DcTracker.this.log("screen off");
                    DcTracker.this.mIsScreenOn = false;
                    DcTracker.this.stopNetStatPoll();
                    DcTracker.this.startNetStatPoll();
                    DcTracker.this.restartDataStallAlarm();
                    return;
                }
                if (action.startsWith(DcTracker.INTENT_RECONNECT_ALARM)) {
                    DcTracker.this.log("Reconnect alarm. Previous state was " + DcTracker.this.mState);
                    DcTracker.this.onActionIntentReconnectAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_DATA_STALL_ALARM)) {
                    DcTracker.this.log("Data stall alarm");
                    DcTracker.this.onActionIntentDataStallAlarm(intent);
                    return;
                }
                if (action.equals(DcTracker.INTENT_PROVISIONING_APN_ALARM)) {
                    DcTracker.this.log("Provisioning apn alarm");
                    DcTracker.this.onActionIntentProvisioningApnAlarm(intent);
                    return;
                }
                if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    if (DcTracker.this.mIccRecords.get() != null && DcTracker.this.mIccRecords.get().getRecordsLoaded()) {
                        DcTracker.this.setDefaultDataRoamingEnabled();
                        return;
                    }
                    return;
                }
                DcTracker.this.log("onReceive: Unknown action=" + action);
            }
        };
        this.mPollNetStat = new Runnable() {
            @Override
            public void run() {
                DcTracker.this.updateDataActivity();
                if (DcTracker.this.mIsScreenOn) {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
                } else {
                    DcTracker.this.mNetStatPollPeriod = Settings.Global.getInt(DcTracker.this.mResolver, "pdp_watchdog_long_poll_interval_ms", DcTracker.POLL_NETSTAT_SCREEN_OFF_MILLIS);
                }
                if (DcTracker.this.mNetStatPollEnabled) {
                    DcTracker.this.mDataConnectionTracker.postDelayed(this, DcTracker.this.mNetStatPollPeriod);
                }
            }
        };
        this.mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            public final AtomicInteger mPreviousSubId = new AtomicInteger(-1);

            @Override
            public void onSubscriptionsChanged() {
                DcTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
                int subId = DcTracker.this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId) && DcTracker.this.mtkIsNeedRegisterSettingsObserver(this.mPreviousSubId.get(), subId)) {
                    DcTracker.this.registerSettingsObserver();
                }
                if (this.mPreviousSubId.getAndSet(subId) != subId && SubscriptionManager.isValidSubscriptionId(subId)) {
                    DcTracker.this.onRecordsLoadedOrSubIdChanged();
                }
            }
        };
        this.mDisconnectAllCompleteMsgList = new ArrayList<>();
        this.mAllDataDisconnectedRegistrants = new RegistrantList();
        this.mIccRecords = new AtomicReference<>();
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
        this.mMeteredApnDisabled = false;
        this.mSetDataProfileStatus = 0;
        this.mReregisterOnReconnectFailure = false;
        this.mCanSetPreferApn = false;
        this.mAttached = new AtomicBoolean(false);
        this.mAlarmManager = null;
        this.mCm = null;
        this.mPhone = null;
        this.mUiccController = null;
        this.mDataConnectionTracker = null;
        this.mProvisionActionName = null;
        this.mSettingsObserver = new SettingsObserver(null, this);
        this.mDataEnabledSettings = null;
        this.mTransportType = 0;
        this.mDataServiceManager = null;
    }

    public void registerServiceStateTrackerEvents() {
        this.mPhone.getServiceStateTracker().registerForDataConnectionAttached(this, 270352, null);
        this.mPhone.getServiceStateTracker().registerForDataConnectionDetached(this, 270345, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOn(this, 270347, null);
        this.mPhone.getServiceStateTracker().registerForDataRoamingOff(this, 270348, null, true);
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

    protected void registerForAllEvents() {
        if (this.mTransportType == 1) {
            this.mPhone.mCi.registerForAvailable(this, 270337, null);
            this.mPhone.mCi.registerForOffOrNotAvailable(this, 270342, null);
            this.mPhone.mCi.registerForPcoData(this, 270381, null);
        }
        this.mPhone.getCallTracker().registerForVoiceCallEnded(this, 270344, null);
        this.mPhone.getCallTracker().registerForVoiceCallStarted(this, 270343, null);
        registerServiceStateTrackerEvents();
        this.mPhone.mCi.registerForPcoData(this, 270381, null);
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(0, this, 270382, null, false);
        this.mDataServiceManager.registerForServiceBindingChanged(this, 270385, null);
    }

    public void dispose() {
        log("DCT.dispose");
        if (this.mProvisionBroadcastReceiver != null) {
            this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
            this.mProvisionBroadcastReceiver = null;
        }
        if (this.mProvisioningSpinner != null) {
            this.mProvisioningSpinner.dismiss();
            this.mProvisioningSpinner = null;
        }
        cleanUpAllConnections(true, (String) null);
        Iterator<DcAsyncChannel> it = this.mDataConnectionAcHashMap.values().iterator();
        while (it.hasNext()) {
            it.next().disconnect();
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
        this.mApnContexts.clear();
        this.mApnContextsById.clear();
        this.mPrioritySortedApnContexts.clear();
        unregisterForAllEvents();
        destroyDataConnections();
    }

    protected void unregisterForAllEvents() {
        if (this.mTransportType == 1) {
            this.mPhone.mCi.unregisterForAvailable(this);
            this.mPhone.mCi.unregisterForOffOrNotAvailable(this);
            this.mPhone.mCi.unregisterForPcoData(this);
        }
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            iccRecords.unregisterForRecordsLoaded(this);
            this.mIccRecords.set(null);
        }
        this.mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
        this.mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        unregisterServiceStateTrackerEvents();
        this.mPhone.mCi.unregisterForPcoData(this);
        this.mPhone.getCarrierActionAgent().unregisterForCarrierAction(this, 0);
        this.mDataServiceManager.unregisterForServiceBindingChanged(this);
    }

    public void setUserDataEnabled(boolean z) {
        Message messageObtainMessage = obtainMessage(270366);
        messageObtainMessage.arg1 = z ? 1 : 0;
        log("setDataEnabled: sendMessage: enable=" + z);
        sendMessage(messageObtainMessage);
    }

    protected void onSetUserDataEnabled(boolean z) {
        if (this.mDataEnabledSettings.isUserDataEnabled() != z) {
            this.mDataEnabledSettings.setUserDataEnabled(z);
            if (!getDataRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
                if (z) {
                    notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
                } else {
                    notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_DATA_DISABLED);
                }
            }
            this.mPhone.notifyUserMobileDataStateChanged(z);
            if (z) {
                reevaluateDataConnections();
                onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
            } else {
                onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
            }
        }
    }

    protected void reevaluateDataConnections() {
        DcAsyncChannel dcAc;
        if (this.mDataEnabledSettings.isDataEnabled()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (!mtkSkipImsOrEmergencyPdn(apnContext) && apnContext.isConnectedOrConnecting() && (dcAc = apnContext.getDcAc()) != null) {
                    NetworkCapabilities networkCapabilitiesSync = dcAc.getNetworkCapabilitiesSync();
                    if (networkCapabilitiesSync != null && !networkCapabilitiesSync.hasCapability(13) && !networkCapabilitiesSync.hasCapability(11)) {
                        log("Tearing down restricted metered net:" + apnContext);
                        apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                        cleanUpConnection(true, apnContext);
                    } else if (apnContext.getApnSetting().isMetered(this.mPhone) && networkCapabilitiesSync != null && networkCapabilitiesSync.hasCapability(11)) {
                        log("Tearing down unmetered net:" + apnContext);
                        apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                        cleanUpConnection(true, apnContext);
                    }
                }
            }
        }
    }

    private void onDeviceProvisionedChange() {
        if (isDataEnabled()) {
            reevaluateDataConnections();
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
        } else {
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

    public void requestNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        ApnContext apnContext = this.mApnContextsById.get(ApnContext.apnIdForNetworkRequest(networkRequest));
        localLog.log("DcTracker.requestNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) {
            apnContext.requestNetwork(networkRequest, localLog);
        }
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        ApnContext apnContext = this.mApnContextsById.get(ApnContext.apnIdForNetworkRequest(networkRequest));
        localLog.log("DcTracker.releaseNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) {
            apnContext.releaseNetwork(networkRequest, localLog);
        }
    }

    public boolean isApnSupported(String str) {
        if (str == null) {
            loge("isApnSupported: name=null");
            return false;
        }
        if (this.mApnContexts.get(str) == null) {
            loge("Request for unsupported mobile name: " + str);
            return false;
        }
        return true;
    }

    public int getApnPriority(String str) {
        ApnContext apnContext = this.mApnContexts.get(str);
        if (apnContext == null) {
            loge("Request for unsupported mobile name: " + str);
        }
        return apnContext.priority;
    }

    private void setRadio(boolean z) {
        try {
            ITelephony.Stub.asInterface(ServiceManager.checkService("phone")).setRadio(z);
        } catch (Exception e) {
        }
    }

    private class ProvisionNotificationBroadcastReceiver extends BroadcastReceiver {
        private final String mNetworkOperator;
        private final String mProvisionUrl;

        public ProvisionNotificationBroadcastReceiver(String str, String str2) {
            this.mNetworkOperator = str2;
            this.mProvisionUrl = str;
        }

        private void setEnableFailFastMobileData(int i) {
            DcTracker.this.sendMessage(DcTracker.this.obtainMessage(270372, i, 0));
        }

        private void enableMobileProvisioning() {
            Message messageObtainMessage = DcTracker.this.obtainMessage(270373);
            messageObtainMessage.setData(Bundle.forPair("provisioningUrl", this.mProvisionUrl));
            DcTracker.this.sendMessage(messageObtainMessage);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            DcTracker.this.log("onReceive : ProvisionNotificationBroadcastReceiver");
            DcTracker.this.mProvisioningSpinner = new ProgressDialog(context);
            DcTracker.this.mProvisioningSpinner.setTitle(this.mNetworkOperator);
            DcTracker.this.mProvisioningSpinner.setMessage(context.getText(R.string.deleteText));
            DcTracker.this.mProvisioningSpinner.setIndeterminate(true);
            DcTracker.this.mProvisioningSpinner.setCancelable(true);
            DcTracker.this.mProvisioningSpinner.getWindow().setType(2009);
            DcTracker.this.mProvisioningSpinner.show();
            DcTracker.this.sendMessageDelayed(DcTracker.this.obtainMessage(270378, DcTracker.this.mProvisioningSpinner), 120000L);
            DcTracker.this.setRadio(true);
            setEnableFailFastMobileData(1);
            enableMobileProvisioning();
        }
    }

    protected void finalize() {
        if (this.mPhone != null) {
            log("finalize");
        }
    }

    protected ApnContext addApnContext(String str, NetworkConfig networkConfig) {
        ApnContext apnContext = new ApnContext(this.mPhone, str, LOG_TAG, networkConfig, this);
        this.mApnContexts.put(str, apnContext);
        this.mApnContextsById.put(ApnContext.apnIdForApnName(str), apnContext);
        this.mPrioritySortedApnContexts.add(apnContext);
        return apnContext;
    }

    protected void initApnContexts() {
        ApnContext apnContextAddApnContext;
        log("initApnContexts: E");
        for (String str : this.mPhone.getContext().getResources().getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnesses)) {
            NetworkConfig networkConfig = new NetworkConfig(str);
            switch (networkConfig.type) {
                case 0:
                    apnContextAddApnContext = addApnContext("default", networkConfig);
                    break;
                case 1:
                case 6:
                case 7:
                case 8:
                case 9:
                case 13:
                default:
                    log("initApnContexts: skipping unknown type=" + networkConfig.type);
                    continue;
                    break;
                case 2:
                    apnContextAddApnContext = addApnContext("mms", networkConfig);
                    break;
                case 3:
                    apnContextAddApnContext = addApnContext("supl", networkConfig);
                    break;
                case 4:
                    apnContextAddApnContext = addApnContext("dun", networkConfig);
                    break;
                case 5:
                    apnContextAddApnContext = addApnContext("hipri", networkConfig);
                    break;
                case 10:
                    apnContextAddApnContext = addApnContext("fota", networkConfig);
                    break;
                case 11:
                    apnContextAddApnContext = addApnContext("ims", networkConfig);
                    break;
                case 12:
                    apnContextAddApnContext = addApnContext("cbs", networkConfig);
                    break;
                case 14:
                    apnContextAddApnContext = addApnContext("ia", networkConfig);
                    break;
                case 15:
                    apnContextAddApnContext = addApnContext("emergency", networkConfig);
                    break;
            }
            log("initApnContexts: apnContext=" + apnContextAddApnContext);
        }
    }

    public LinkProperties getLinkProperties(String str) {
        DcAsyncChannel dcAc;
        ApnContext apnContext = this.mApnContexts.get(str);
        if (apnContext != null && (dcAc = apnContext.getDcAc()) != null) {
            log("return link properites for " + str);
            return dcAc.getLinkPropertiesSync();
        }
        log("return new LinkProperties");
        return new LinkProperties();
    }

    public NetworkCapabilities getNetworkCapabilities(String str) {
        DcAsyncChannel dcAc;
        ApnContext apnContext = this.mApnContexts.get(str);
        if (apnContext != null && (dcAc = apnContext.getDcAc()) != null) {
            log("get active pdp is not null, return NetworkCapabilities for " + str);
            return dcAc.getNetworkCapabilitiesSync();
        }
        log("return new NetworkCapabilities");
        return new NetworkCapabilities();
    }

    public String[] getActiveApnTypes() {
        log("get all active apn types");
        ArrayList arrayList = new ArrayList();
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                arrayList.add(apnContext.getApnType());
            }
        }
        return (String[]) arrayList.toArray(new String[0]);
    }

    public String getActiveApnString(String str) {
        ApnSetting apnSetting;
        ApnContext apnContext = this.mApnContexts.get(str);
        if (apnContext != null && (apnSetting = apnContext.getApnSetting()) != null) {
            return apnSetting.apn;
        }
        return null;
    }

    public DctConstants.State getState(String str) {
        for (DataConnection dataConnection : this.mDataConnections.values()) {
            ApnSetting apnSetting = dataConnection.getApnSetting();
            if (apnSetting != null && apnSetting.canHandleType(str)) {
                if (dataConnection.isActive()) {
                    return DctConstants.State.CONNECTED;
                }
                if (dataConnection.isActivating()) {
                    return DctConstants.State.CONNECTING;
                }
                if (dataConnection.isInactive()) {
                    return DctConstants.State.IDLE;
                }
                if (dataConnection.isDisconnecting()) {
                    return DctConstants.State.DISCONNECTING;
                }
            }
        }
        return DctConstants.State.IDLE;
    }

    private boolean isProvisioningApn(String str) {
        ApnContext apnContext = this.mApnContexts.get(str);
        if (apnContext != null) {
            return apnContext.isProvisioningApn();
        }
        return false;
    }

    public DctConstants.State getOverallState() {
        boolean z = true;
        boolean z2 = false;
        boolean z3 = false;
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.isEnabled()) {
                switch (AnonymousClass6.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext.getState().ordinal()]) {
                    case 1:
                    case 2:
                        return DctConstants.State.CONNECTED;
                    case 3:
                    case 4:
                        z2 = true;
                        z3 = true;
                        break;
                    case 5:
                    case 6:
                        z2 = true;
                        break;
                    default:
                        z2 = true;
                        continue;
                }
                z = false;
            }
        }
        return !z2 ? DctConstants.State.IDLE : z3 ? DctConstants.State.CONNECTING : !z ? DctConstants.State.IDLE : DctConstants.State.FAILED;
    }

    static class AnonymousClass6 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.RETRYING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.IDLE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.SCANNING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.FAILED.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    @VisibleForTesting
    public boolean isDataEnabled() {
        return this.mDataEnabledSettings.isDataEnabled();
    }

    protected void onDataConnectionDetached() {
        log("onDataConnectionDetached: stop polling and notify detached");
        stopNetStatPoll();
        stopDataStallAlarm();
        notifyDataConnection(PhoneInternalInterface.REASON_DATA_DETACHED);
        this.mAttached.set(false);
    }

    protected void onDataConnectionAttached() {
        log("onDataConnectionAttached");
        this.mAttached.set(true);
        if (getOverallState() == DctConstants.State.CONNECTED) {
            log("onDataConnectionAttached: start polling notify attached");
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

    public boolean isDataAllowed(DataConnectionReasons dataConnectionReasons) {
        return isDataAllowed(null, dataConnectionReasons);
    }

    protected boolean isDataAllowed(ApnContext apnContext, DataConnectionReasons dataConnectionReasons) {
        DataConnectionReasons dataConnectionReasons2 = new DataConnectionReasons();
        boolean zIsInternalDataEnabled = this.mDataEnabledSettings.isInternalDataEnabled();
        boolean z = this.mAttached.get();
        boolean desiredPowerState = this.mPhone.getServiceStateTracker().getDesiredPowerState();
        boolean powerStateFromCarrier = this.mPhone.getServiceStateTracker().getPowerStateFromCarrier();
        int rilDataRadioTechnology = this.mPhone.getServiceState().getRilDataRadioTechnology();
        if (rilDataRadioTechnology == 18) {
            desiredPowerState = true;
            powerStateFromCarrier = true;
        }
        boolean z2 = this.mIccRecords.get() != null && this.mIccRecords.get().getRecordsLoaded();
        boolean zIsValidSubscriptionId = SubscriptionManager.isValidSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean z3 = apnContext == null || ApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone);
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        if (this.mPhone.getCallTracker() != null) {
            state = this.mPhone.getCallTracker().getState();
        }
        if (apnContext != null && apnContext.getApnType().equals("emergency") && apnContext.isConnectable()) {
            if (dataConnectionReasons != null) {
                dataConnectionReasons.add(DataConnectionReasons.DataAllowedReasonType.EMERGENCY_APN);
            }
            return true;
        }
        if (apnContext != null && !apnContext.isConnectable()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.APN_NOT_CONNECTABLE);
        }
        if (apnContext != null && ((apnContext.getApnType().equals("default") || apnContext.getApnType().equals("ia")) && rilDataRadioTechnology == 18)) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.ON_IWLAN);
        }
        if (isEmergency()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.IN_ECBM);
        }
        if (!z && !this.mAutoAttachOnCreation.get()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.NOT_ATTACHED);
        }
        if (!z2) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.RECORD_NOT_LOADED);
        }
        if (state != PhoneConstants.State.IDLE && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.INVALID_PHONE_STATE);
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.CONCURRENT_VOICE_DATA_NOT_ALLOWED);
        }
        if (!zIsInternalDataEnabled) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.INTERNAL_DATA_DISABLED);
        }
        if (!zIsValidSubscriptionId) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if (this.mPhone.getServiceState().getDataRoaming() && !getDataRoamingEnabled()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED);
        }
        if (this.mIsPsRestricted) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.UNDESIRED_POWER_STATE);
        }
        if (!powerStateFromCarrier) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.RADIO_DISABLED_BY_CARRIER);
        }
        if (!this.mDataEnabledSettings.isDataEnabled()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.DATA_DISABLED);
        }
        if (dataConnectionReasons2.containsHardDisallowedReasons()) {
            if (dataConnectionReasons != null) {
                dataConnectionReasons.copyFrom(dataConnectionReasons2);
                return false;
            }
            return false;
        }
        if (!z3 && !dataConnectionReasons2.allowed()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataAllowedReasonType.UNMETERED_APN);
        }
        if (apnContext != null && !apnContext.hasNoRestrictedRequests(true) && dataConnectionReasons2.contains(DataConnectionReasons.DataDisallowedReasonType.DATA_DISABLED)) {
            dataConnectionReasons2.add(DataConnectionReasons.DataAllowedReasonType.RESTRICTED_REQUEST);
        }
        if (dataConnectionReasons2.allowed()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataAllowedReasonType.NORMAL);
        }
        if (dataConnectionReasons != null) {
            dataConnectionReasons.copyFrom(dataConnectionReasons2);
        }
        return dataConnectionReasons2.allowed();
    }

    protected void setupDataOnConnectableApns(String str) {
        setupDataOnConnectableApns(str, RetryFailures.ALWAYS);
    }

    protected void setupDataOnConnectableApns(String str, RetryFailures retryFailures) {
        StringBuilder sb = new StringBuilder(120);
        for (ApnContext apnContext : this.mPrioritySortedApnContexts) {
            sb.append(apnContext.getApnType());
            sb.append(":[state=");
            sb.append(apnContext.getState());
            sb.append(",enabled=");
            sb.append(apnContext.isEnabled());
            sb.append("] ");
        }
        log("setupDataOnConnectableApns: " + str + " " + ((Object) sb));
        for (ApnContext apnContext2 : this.mPrioritySortedApnContexts) {
            if (apnContext2.getState() == DctConstants.State.FAILED || apnContext2.getState() == DctConstants.State.SCANNING) {
                if (retryFailures == RetryFailures.ALWAYS) {
                    apnContext2.releaseDataConnection(str);
                } else if (!apnContext2.isConcurrentVoiceAndDataAllowed() && this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                    apnContext2.releaseDataConnection(str);
                }
            }
            if (apnContext2.isConnectable()) {
                log("isConnectable() call trySetupData");
                apnContext2.setReason(str);
                trySetupData(apnContext2);
            }
        }
    }

    protected boolean isEmergency() {
        boolean z = this.mPhone.isInEcm() || this.mPhone.isInEmergencyCall();
        log("isEmergency: result=" + z);
        return z;
    }

    protected boolean trySetupData(ApnContext apnContext) {
        if (this.mPhone.getSimulatedRadioControl() != null) {
            apnContext.setState(DctConstants.State.CONNECTED);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return true;
        }
        DataConnectionReasons dataConnectionReasons = new DataConnectionReasons();
        boolean zIsDataAllowed = isDataAllowed(apnContext, dataConnectionReasons);
        String str = "trySetupData for APN type " + apnContext.getApnType() + ", reason: " + apnContext.getReason() + ". " + dataConnectionReasons.toString();
        log(str);
        apnContext.requestLog(str);
        if (zIsDataAllowed) {
            if (apnContext.getState() == DctConstants.State.FAILED) {
                log("trySetupData: make a FAILED ApnContext IDLE so its reusable");
                apnContext.requestLog("trySetupData: make a FAILED ApnContext IDLE so its reusable");
                apnContext.setState(DctConstants.State.IDLE);
            }
            int rilDataRadioTechnology = this.mPhone.getServiceState().getRilDataRadioTechnology();
            apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
            if (apnContext.getState() == DctConstants.State.IDLE) {
                ArrayList<ApnSetting> arrayListBuildWaitingApns = buildWaitingApns(apnContext.getApnType(), rilDataRadioTechnology);
                if (arrayListBuildWaitingApns.isEmpty()) {
                    notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    log("trySetupData: X No APN found retValue=false");
                    apnContext.requestLog("trySetupData: X No APN found retValue=false");
                    return false;
                }
                apnContext.setWaitingApns(arrayListBuildWaitingApns);
                log("trySetupData: Create from mAllApnSettings : " + apnListToString(this.mAllApnSettings));
            }
            boolean z = setupData(apnContext, rilDataRadioTechnology, dataConnectionReasons.contains(DataConnectionReasons.DataAllowedReasonType.UNMETERED_APN));
            notifyOffApnsOfAvailability(apnContext.getReason());
            log("trySetupData: X retValue=" + z);
            return z;
        }
        if (!apnContext.getApnType().equals("default") && apnContext.isConnectable()) {
            this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
        notifyOffApnsOfAvailability(apnContext.getReason());
        StringBuilder sb = new StringBuilder();
        sb.append("trySetupData failed. apnContext = [type=" + apnContext.getApnType() + ", mState=" + apnContext.getState() + ", apnEnabled=" + apnContext.isEnabled() + ", mDependencyMet=" + apnContext.getDependencyMet() + "] ");
        if (!this.mDataEnabledSettings.isDataEnabled()) {
            sb.append("isDataEnabled() = false. " + this.mDataEnabledSettings);
        }
        if (apnContext.getState() == DctConstants.State.SCANNING) {
            apnContext.setState(DctConstants.State.FAILED);
            sb.append(" Stop retrying.");
        }
        log(sb.toString());
        apnContext.requestLog(sb.toString());
        return false;
    }

    protected void notifyOffApnsOfAvailability(String str) {
        String reason;
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (!this.mAttached.get() || !apnContext.isReady()) {
                if (mtkIsNeedNotify(apnContext)) {
                    Phone phone = this.mPhone;
                    if (str == null) {
                        reason = apnContext.getReason();
                    } else {
                        reason = str;
                    }
                    phone.notifyDataConnection(reason, apnContext.getApnType(), PhoneConstants.DataState.DISCONNECTED);
                }
            }
        }
    }

    protected boolean cleanUpAllConnections(boolean z, String str) {
        log("cleanUpAllConnections: tearDown=" + z + " reason=" + str);
        boolean z2 = false;
        boolean z3 = !TextUtils.isEmpty(str) && (str.equals(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED) || str.equals(PhoneInternalInterface.REASON_ROAMING_ON) || str.equals(PhoneInternalInterface.REASON_CARRIER_ACTION_DISABLE_METERED_APN) || str.equals(PhoneInternalInterface.REASON_PDP_RESET));
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (z3) {
                ApnSetting apnSetting = apnContext.getApnSetting();
                if (apnSetting != null && apnSetting.isMetered(this.mPhone)) {
                    if (!apnContext.isDisconnected()) {
                        z2 = true;
                    }
                    log("clean up metered ApnContext Type: " + apnContext.getApnType());
                    apnContext.setReason(str);
                    cleanUpConnection(z, apnContext);
                }
            } else if (!str.equals(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION) || !apnContext.getApnType().equals("ims")) {
                if (!apnContext.isDisconnected()) {
                    z2 = true;
                }
                apnContext.setReason(str);
                cleanUpConnection(z, apnContext);
            }
        }
        stopNetStatPoll();
        stopDataStallAlarm();
        this.mRequestedApnType = "default";
        log("cleanUpConnection: mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        if (z && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
        return z2;
    }

    protected void onCleanUpAllConnections(String str) {
        cleanUpAllConnections(true, str);
    }

    public void sendCleanUpConnection(boolean z, ApnContext apnContext) {
        log("sendCleanUpConnection: tearDown=" + z + " apnContext=" + apnContext);
        Message messageObtainMessage = obtainMessage(270360);
        messageObtainMessage.arg1 = z ? 1 : 0;
        messageObtainMessage.arg2 = 0;
        messageObtainMessage.obj = apnContext;
        sendMessage(messageObtainMessage);
    }

    protected void cleanUpConnection(boolean z, ApnContext apnContext) {
        if (apnContext == null) {
            log("cleanUpConnection: apn context is null");
            return;
        }
        DcAsyncChannel dcAc = apnContext.getDcAc();
        apnContext.requestLog("cleanUpConnection: tearDown=" + z + " reason=" + apnContext.getReason());
        if (z) {
            if (apnContext.isDisconnected()) {
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcAc != null) {
                        log("cleanUpConnection: teardown, disconnected, !ready apnContext=" + apnContext);
                        apnContext.requestLog("cleanUpConnection: teardown, disconnected, !ready");
                        dcAc.tearDown(apnContext, "", null);
                    }
                    apnContext.setDataConnectionAc(null);
                }
            } else if (dcAc != null) {
                if (apnContext.getState() != DctConstants.State.DISCONNECTING) {
                    boolean z2 = false;
                    if ("dun".equals(apnContext.getApnType()) && teardownForDun()) {
                        log("cleanUpConnection: disconnectAll DUN connection");
                        z2 = true;
                    }
                    int connectionGeneration = apnContext.getConnectionGeneration();
                    StringBuilder sb = new StringBuilder();
                    sb.append("cleanUpConnection: tearing down");
                    sb.append(z2 ? " all" : "");
                    sb.append(" using gen#");
                    sb.append(connectionGeneration);
                    String string = sb.toString();
                    log(string + "apnContext=" + apnContext);
                    apnContext.requestLog(string);
                    Message messageObtainMessage = obtainMessage(270351, new Pair(apnContext, Integer.valueOf(connectionGeneration)));
                    if (z2) {
                        apnContext.getDcAc().tearDownAll(apnContext.getReason(), messageObtainMessage);
                    } else {
                        apnContext.getDcAc().tearDown(apnContext, apnContext.getReason(), messageObtainMessage);
                    }
                    apnContext.setState(DctConstants.State.DISCONNECTING);
                    this.mDisconnectPendingCount++;
                }
            } else {
                apnContext.setState(DctConstants.State.IDLE);
                apnContext.requestLog("cleanUpConnection: connected, bug no DCAC");
                this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
        } else {
            if (dcAc != null) {
                dcAc.reqReset();
            }
            apnContext.setState(DctConstants.State.IDLE);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            apnContext.setDataConnectionAc(null);
        }
        if (dcAc != null) {
            cancelReconnectAlarm(apnContext);
        }
        String str = "cleanUpConnection: X tearDown=" + z + " reason=" + apnContext.getReason();
        log(str + " apnContext=" + apnContext + " dcac=" + apnContext.getDcAc());
        apnContext.requestLog(str);
    }

    @VisibleForTesting
    public ArrayList<ApnSetting> fetchDunApns() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            log("fetchDunApns: net.tethering.noprovisioning=true ret: empty list");
            return new ArrayList<>(0);
        }
        int rilDataRadioTechnology = this.mPhone.getServiceState().getRilDataRadioTechnology();
        IccRecords iccRecords = this.mIccRecords.get();
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric(iccRecords);
        ArrayList<ApnSetting> arrayList = new ArrayList();
        ArrayList<ApnSetting> arrayList2 = new ArrayList<>();
        String string = Settings.Global.getString(this.mResolver, "tether_dun_apn");
        if (!TextUtils.isEmpty(string)) {
            arrayList.addAll(ApnSetting.arrayFromString(string));
        }
        if (arrayList.isEmpty()) {
            String[] strArrMtkGetDunApnByMccMnc = mtkGetDunApnByMccMnc(this.mPhone.getContext(), this.mPhone.getContext().getResources().getStringArray(R.array.config_deviceTabletopRotations));
            if (!ArrayUtils.isEmpty(strArrMtkGetDunApnByMccMnc)) {
                for (String str : strArrMtkGetDunApnByMccMnc) {
                    ApnSetting apnSettingFromString = ApnSetting.fromString(str);
                    if (apnSettingFromString != null) {
                        arrayList.add(apnSettingFromString);
                    }
                }
            }
        }
        if (arrayList.isEmpty()) {
            synchronized (this.mRefCountLock) {
                if (!ArrayUtils.isEmpty(this.mAllApnSettings)) {
                    for (ApnSetting apnSetting : this.mAllApnSettings) {
                        if (apnSetting.canHandleType("dun")) {
                            arrayList.add(apnSetting);
                        }
                    }
                }
            }
        }
        for (ApnSetting apnSetting2 : arrayList) {
            if (ServiceState.bitmaskHasTech(apnSetting2.networkTypeBitmask, ServiceState.rilRadioTechnologyToNetworkType(rilDataRadioTechnology)) && apnSetting2.numeric.equals(strMtkGetOperatorNumeric)) {
                if (apnSetting2.hasMvnoParams()) {
                    if (iccRecords != null && ApnSetting.mvnoMatches(iccRecords, apnSetting2.mvnoType, apnSetting2.mvnoMatchData)) {
                        arrayList2.add(apnSetting2);
                    }
                } else if (!this.mMvnoMatched) {
                    arrayList2.add(apnSetting2);
                }
            }
        }
        return arrayList2;
    }

    private int getPreferredApnSetId() {
        Cursor cursorQuery = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "preferapnset/subId/" + this.mPhone.getSubId()), new String[]{"apn_set_id"}, null, null, null);
        if (cursorQuery.getCount() < 1) {
            loge("getPreferredApnSetId: no APNs found");
            return 0;
        }
        cursorQuery.moveToFirst();
        return cursorQuery.getInt(0);
    }

    public boolean hasMatchedTetherApnSetting() {
        ArrayList<ApnSetting> arrayListFetchDunApns = fetchDunApns();
        log("hasMatchedTetherApnSetting: APNs=" + arrayListFetchDunApns);
        return arrayListFetchDunApns.size() > 0;
    }

    protected boolean teardownForDun() {
        return ServiceState.isCdma(this.mPhone.getServiceState().getRilDataRadioTechnology()) || fetchDunApns().size() > 0;
    }

    protected void cancelReconnectAlarm(ApnContext apnContext) {
        PendingIntent reconnectIntent;
        if (apnContext != null && (reconnectIntent = apnContext.getReconnectIntent()) != null) {
            ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(reconnectIntent);
            apnContext.setReconnectIntent(null);
        }
    }

    protected String[] parseTypes(String str) {
        if (str == null || str.equals("")) {
            return new String[]{CharacterSets.MIMENAME_ANY_CHARSET};
        }
        return str.split(",");
    }

    public boolean isPermanentFailure(DcFailCause dcFailCause) {
        return dcFailCause.isPermanentFailure(this.mPhone.getContext(), this.mPhone.getSubId()) && !(this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST);
    }

    protected ApnSetting makeApnSetting(Cursor cursor) {
        String[] types = parseTypes(cursor.getString(cursor.getColumnIndexOrThrow("type")));
        int i = cursor.getInt(cursor.getColumnIndexOrThrow("network_type_bitmask"));
        return new ApnSetting(cursor.getInt(cursor.getColumnIndexOrThrow(HbpcdLookup.ID)), cursor.getString(cursor.getColumnIndexOrThrow("numeric")), cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("proxy"))), cursor.getString(cursor.getColumnIndexOrThrow("port")), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("mmsc"))), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("mmsproxy"))), cursor.getString(cursor.getColumnIndexOrThrow("mmsport")), cursor.getString(cursor.getColumnIndexOrThrow("user")), cursor.getString(cursor.getColumnIndexOrThrow("password")), cursor.getInt(cursor.getColumnIndexOrThrow("authtype")), types, cursor.getString(cursor.getColumnIndexOrThrow("protocol")), cursor.getString(cursor.getColumnIndexOrThrow("roaming_protocol")), cursor.getInt(cursor.getColumnIndexOrThrow("carrier_enabled")) == 1, i, cursor.getInt(cursor.getColumnIndexOrThrow("profile_id")), cursor.getInt(cursor.getColumnIndexOrThrow("modem_cognitive")) == 1, cursor.getInt(cursor.getColumnIndexOrThrow("max_conns")), cursor.getInt(cursor.getColumnIndexOrThrow("wait_time")), cursor.getInt(cursor.getColumnIndexOrThrow("max_conns_time")), cursor.getInt(cursor.getColumnIndexOrThrow("mtu")), cursor.getString(cursor.getColumnIndexOrThrow("mvno_type")), cursor.getString(cursor.getColumnIndexOrThrow("mvno_match_data")), cursor.getInt(cursor.getColumnIndexOrThrow("apn_set_id")));
    }

    protected ArrayList<ApnSetting> createApnList(Cursor cursor) {
        ArrayList<ApnSetting> arrayList = new ArrayList<>();
        ArrayList<ApnSetting> arrayList2 = new ArrayList<>();
        IccRecords iccRecords = this.mIccRecords.get();
        if (cursor.moveToFirst()) {
            do {
                ApnSetting apnSettingMakeApnSetting = makeApnSetting(cursor);
                if (apnSettingMakeApnSetting != null) {
                    if (apnSettingMakeApnSetting.hasMvnoParams()) {
                        if (mtkMvnoMatches(apnSettingMakeApnSetting.mvnoType, apnSettingMakeApnSetting.mvnoMatchData) || (iccRecords != null && ApnSetting.mvnoMatches(iccRecords, apnSettingMakeApnSetting.mvnoType, apnSettingMakeApnSetting.mvnoMatchData))) {
                            arrayList2.add(apnSettingMakeApnSetting);
                        }
                    } else {
                        arrayList.add(apnSettingMakeApnSetting);
                    }
                }
            } while (cursor.moveToNext());
        }
        if (arrayList2.isEmpty()) {
            this.mMvnoMatched = false;
        } else {
            this.mMvnoMatched = true;
            arrayList = mtkAddMnoApnsIntoAllApnList(arrayList2, arrayList);
        }
        log("createApnList: X result=" + arrayList);
        return arrayList;
    }

    protected boolean dataConnectionNotInUse(DcAsyncChannel dcAsyncChannel) {
        log("dataConnectionNotInUse: check if dcac is inuse dcac=" + dcAsyncChannel);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.getDcAc() == dcAsyncChannel) {
                log("dataConnectionNotInUse: in use by apnContext=" + apnContext);
                return false;
            }
        }
        if (mtkSkipTearDownAll()) {
            return true;
        }
        log("dataConnectionNotInUse: tearDownAll");
        dcAsyncChannel.tearDownAll("No connection", null);
        log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    protected DcAsyncChannel findFreeDataConnection() {
        for (DcAsyncChannel dcAsyncChannel : this.mDataConnectionAcHashMap.values()) {
            if (dcAsyncChannel.isInactiveSync() && dataConnectionNotInUse(dcAsyncChannel)) {
                log("findFreeDataConnection: found free DataConnection= dcac=" + dcAsyncChannel);
                return dcAsyncChannel;
            }
        }
        log("findFreeDataConnection: NO free DataConnection");
        return null;
    }

    protected boolean setupData(ApnContext apnContext, int i, boolean z) {
        DcAsyncChannel dcAsyncChannelCheckForCompatibleConnectedApnContext;
        ApnSetting apnSettingSync;
        log("setupData: apnContext=" + apnContext);
        apnContext.requestLog("setupData");
        ApnSetting nextApnSetting = apnContext.getNextApnSetting();
        if (nextApnSetting == null) {
            log("setupData: return for no apn found!");
            return false;
        }
        int apnProfileID = nextApnSetting.profileId;
        if (apnProfileID == 0) {
            apnProfileID = getApnProfileID(apnContext.getApnType());
        }
        int i2 = apnProfileID;
        if (!apnContext.getApnType().equals("dun") || ServiceState.isGsm(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
            dcAsyncChannelCheckForCompatibleConnectedApnContext = checkForCompatibleConnectedApnContext(apnContext);
            if (dcAsyncChannelCheckForCompatibleConnectedApnContext != null && (apnSettingSync = dcAsyncChannelCheckForCompatibleConnectedApnContext.getApnSettingSync()) != null) {
                nextApnSetting = apnSettingSync;
            }
        } else {
            dcAsyncChannelCheckForCompatibleConnectedApnContext = null;
        }
        if (dcAsyncChannelCheckForCompatibleConnectedApnContext == null) {
            if (isOnlySingleDcAllowed(i)) {
                if (isHigherPriorityApnContextActive(apnContext)) {
                    log("setupData: Higher priority ApnContext active.  Ignoring call");
                    return false;
                }
                if (!apnContext.getApnType().equals("ims") && cleanUpAllConnections(true, PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION)) {
                    log("setupData: Some calls are disconnecting first. Wait and retry");
                    return false;
                }
                log("setupData: Single pdp. Continue setting up data call.");
            }
            dcAsyncChannelCheckForCompatibleConnectedApnContext = findFreeDataConnection();
            if (dcAsyncChannelCheckForCompatibleConnectedApnContext == null) {
                dcAsyncChannelCheckForCompatibleConnectedApnContext = createDataConnection();
            }
            if (dcAsyncChannelCheckForCompatibleConnectedApnContext == null) {
                log("setupData: No free DataConnection and couldn't create one, WEIRD");
                return false;
            }
        }
        DcAsyncChannel dcAsyncChannel = dcAsyncChannelCheckForCompatibleConnectedApnContext;
        int iIncAndGetConnectionGeneration = apnContext.incAndGetConnectionGeneration();
        log("setupData: dcac=" + dcAsyncChannel + " apnSetting=" + nextApnSetting + " gen#=" + iIncAndGetConnectionGeneration);
        apnContext.setDataConnectionAc(dcAsyncChannel);
        apnContext.setApnSetting(nextApnSetting);
        apnContext.setState(DctConstants.State.CONNECTING);
        this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
        Message messageObtainMessage = obtainMessage();
        messageObtainMessage.what = 270336;
        messageObtainMessage.obj = new Pair(apnContext, Integer.valueOf(iIncAndGetConnectionGeneration));
        dcAsyncChannel.bringUp(apnContext, i2, i, z, messageObtainMessage, iIncAndGetConnectionGeneration);
        log("setupData: initing!");
        return true;
    }

    protected void setInitialAttachApn() {
        ApnSetting apnSetting;
        ApnSetting apnSetting2;
        ApnSetting apnSetting3;
        log("setInitialApn: E mPreferredApn=" + this.mPreferredApn);
        if (this.mPreferredApn != null && this.mPreferredApn.canHandleType("ia")) {
            apnSetting = this.mPreferredApn;
            apnSetting2 = null;
        } else {
            if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
                ApnSetting apnSetting4 = this.mAllApnSettings.get(0);
                log("setInitialApn: firstApnSetting=" + apnSetting4);
                Iterator<ApnSetting> it = this.mAllApnSettings.iterator();
                apnSetting3 = null;
                while (true) {
                    if (it.hasNext()) {
                        ApnSetting next = it.next();
                        if (next.canHandleType("ia")) {
                            log("setInitialApn: iaApnSetting=" + next);
                            apnSetting2 = apnSetting4;
                            apnSetting = next;
                            break;
                        }
                        if (apnSetting3 == null && next.canHandleType("default")) {
                            log("setInitialApn: defaultApnSetting=" + next);
                            apnSetting3 = next;
                        }
                    } else {
                        apnSetting2 = apnSetting4;
                        apnSetting = null;
                        break;
                    }
                }
                if (apnSetting == null) {
                    log("setInitialAttachApn: using iaApnSetting");
                } else if (this.mPreferredApn != null) {
                    log("setInitialAttachApn: using mPreferredApn");
                    apnSetting = this.mPreferredApn;
                } else if (apnSetting3 != null) {
                    log("setInitialAttachApn: using defaultApnSetting");
                    apnSetting = apnSetting3;
                } else if (apnSetting2 != null) {
                    log("setInitialAttachApn: using firstApnSetting");
                    apnSetting = apnSetting2;
                } else {
                    apnSetting = null;
                }
                if (apnSetting != null) {
                    log("setInitialAttachApn: X There in no available apn");
                    return;
                }
                log("setInitialAttachApn: X selected Apn=" + apnSetting);
                this.mDataServiceManager.setInitialAttachApn(createDataProfile(apnSetting), this.mPhone.getServiceState().getDataRoamingFromRegistration(), null);
                return;
            }
            apnSetting = null;
            apnSetting2 = null;
        }
        apnSetting3 = apnSetting2;
        if (apnSetting == null) {
        }
        if (apnSetting != null) {
        }
    }

    protected void onApnChanged() {
        DctConstants.State overallState = getOverallState();
        boolean z = overallState == DctConstants.State.IDLE || overallState == DctConstants.State.FAILED;
        if (this.mPhone instanceof GsmCdmaPhone) {
            ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
        }
        log("onApnChanged: createAllApnList and cleanUpAllConnections");
        createAllApnList();
        setInitialAttachApn();
        cleanUpConnectionsOnUpdatedApns(!z, PhoneInternalInterface.REASON_APN_CHANGED);
        if (this.mPhone.getSubId() == SubscriptionManager.getDefaultDataSubscriptionId()) {
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_APN_CHANGED);
        }
    }

    private DcAsyncChannel findDataConnectionAcByCid(int i) {
        for (DcAsyncChannel dcAsyncChannel : this.mDataConnectionAcHashMap.values()) {
            if (dcAsyncChannel.getCidSync() == i) {
                return dcAsyncChannel;
            }
        }
        return null;
    }

    protected boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        if (apnContext.getApnType().equals("ims")) {
            return false;
        }
        for (ApnContext apnContext2 : this.mPrioritySortedApnContexts) {
            if (!apnContext2.getApnType().equals("ims")) {
                if (apnContext.getApnType().equalsIgnoreCase(apnContext2.getApnType())) {
                    return false;
                }
                if (apnContext2.isEnabled() && apnContext2.getState() != DctConstants.State.FAILED) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean isOnlySingleDcAllowed(int i) {
        int[] intArray;
        PersistableBundle config;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null && (config = carrierConfigManager.getConfig()) != null) {
            intArray = config.getIntArray("only_single_dc_allowed_int_array");
        } else {
            intArray = null;
        }
        boolean z = Build.IS_DEBUGGABLE && SystemProperties.getBoolean("persist.telephony.test.singleDc", false);
        if (intArray != null) {
            for (int i2 = 0; i2 < intArray.length && !z; i2++) {
                if (i == intArray[i2]) {
                    z = true;
                }
            }
        }
        log("isOnlySingleDcAllowed(" + i + "): " + z);
        return z;
    }

    public void sendRestartRadio() {
        log("sendRestartRadio:");
        sendMessage(obtainMessage(270362));
    }

    protected void restartRadio() {
        log("restartRadio: ************TURN OFF RADIO**************");
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        this.mPhone.getServiceStateTracker().powerOffRadioSafely(this);
        SystemProperties.set("net.ppp.reset-by-timeout", String.valueOf(Integer.parseInt(SystemProperties.get("net.ppp.reset-by-timeout", "0")) + 1));
    }

    protected boolean retryAfterDisconnected(ApnContext apnContext) {
        if (PhoneInternalInterface.REASON_RADIO_TURNED_OFF.equals(apnContext.getReason()) || (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && isHigherPriorityApnContextActive(apnContext))) {
            return false;
        }
        return true;
    }

    protected void startAlarmForReconnect(long j, ApnContext apnContext) {
        String apnType = apnContext.getApnType();
        Intent intent = new Intent("com.android.internal.telephony.data-reconnect." + apnType);
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_REASON, apnContext.getReason());
        intent.putExtra(INTENT_RECONNECT_ALARM_EXTRA_TYPE, apnType);
        intent.addFlags(268435456);
        intent.putExtra("subscription", this.mPhone.getSubId());
        log("startAlarmForReconnect: delay=" + j + " action=" + intent.getAction() + " apn=" + apnContext);
        PendingIntent broadcast = PendingIntent.getBroadcast(this.mPhone.getContext(), this.mPhone.getPhoneId() + 1, intent, 201326592);
        apnContext.setReconnectIntent(broadcast);
        this.mAlarmManager.setExact(2, SystemClock.elapsedRealtime() + j, broadcast);
    }

    protected void notifyNoData(DcFailCause dcFailCause, ApnContext apnContext) {
        log("notifyNoData: type=" + apnContext.getApnType());
        if (isPermanentFailure(dcFailCause) && !apnContext.getApnType().equals("default")) {
            this.mPhone.notifyDataConnectionFailed(apnContext.getReason(), apnContext.getApnType());
        }
    }

    public boolean getAutoAttachOnCreation() {
        return this.mAutoAttachOnCreation.get();
    }

    protected void onRecordsLoadedOrSubIdChanged() {
        log("onRecordsLoadedOrSubIdChanged: createAllApnList");
        this.mAutoAttachOnCreationConfig = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.borderRight);
        createAllApnList();
        setInitialAttachApn();
        if (this.mPhone.mCi.getRadioState().isOn()) {
            log("onRecordsLoadedOrSubIdChanged: notifying data availability");
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_SIM_LOADED);
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_SIM_LOADED);
    }

    private void onSetCarrierDataEnabled(AsyncResult asyncResult) {
        if (asyncResult.exception != null) {
            Rlog.e(LOG_TAG, "CarrierDataEnable exception: " + asyncResult.exception);
            return;
        }
        boolean zBooleanValue = ((Boolean) asyncResult.result).booleanValue();
        if (zBooleanValue != this.mDataEnabledSettings.isCarrierDataEnabled()) {
            log("carrier Action: set metered apns enabled: " + zBooleanValue);
            this.mDataEnabledSettings.setCarrierDataEnabled(zBooleanValue);
            if (!zBooleanValue) {
                this.mPhone.notifyOtaspChanged(5);
                cleanUpAllConnections(true, PhoneInternalInterface.REASON_CARRIER_ACTION_DISABLE_METERED_APN);
            } else {
                this.mPhone.notifyOtaspChanged(this.mPhone.getServiceStateTracker().getOtasp());
                reevaluateDataConnections();
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_DATA_ENABLED);
            }
        }
    }

    protected void onSimNotReady() {
        log("onSimNotReady");
        cleanUpAllConnections(true, PhoneInternalInterface.REASON_SIM_NOT_READY);
        synchronized (this.mRefCountLock) {
            this.mAllApnSettings = null;
        }
        this.mAutoAttachOnCreationConfig = false;
        this.mAutoAttachOnCreation.set(false);
    }

    private void onSetDependencyMet(String str, boolean z) {
        ApnContext apnContext;
        if ("hipri".equals(str)) {
            return;
        }
        ApnContext apnContext2 = this.mApnContexts.get(str);
        if (apnContext2 == null) {
            loge("onSetDependencyMet: ApnContext not found in onSetDependencyMet(" + str + ", " + z + ")");
            return;
        }
        applyNewState(apnContext2, apnContext2.isEnabled(), z);
        if (!"default".equals(str) || (apnContext = this.mApnContexts.get("hipri")) == null) {
            return;
        }
        applyNewState(apnContext, apnContext.isEnabled(), z);
    }

    public void setPolicyDataEnabled(boolean z) {
        log("setPolicyDataEnabled: " + z);
        Message messageObtainMessage = obtainMessage(270368);
        messageObtainMessage.arg1 = z ? 1 : 0;
        sendMessage(messageObtainMessage);
    }

    protected void onSetPolicyDataEnabled(boolean z) {
        boolean zIsDataEnabled = isDataEnabled();
        if (this.mDataEnabledSettings.isPolicyDataEnabled() != z) {
            this.mDataEnabledSettings.setPolicyDataEnabled(z);
            if (zIsDataEnabled != isDataEnabled()) {
                if (!zIsDataEnabled) {
                    reevaluateDataConnections();
                    onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
                } else {
                    onCleanUpAllConnections(PhoneInternalInterface.REASON_DATA_SPECIFIC_DISABLED);
                }
            }
        }
    }

    protected void applyNewState(ApnContext apnContext, boolean z, boolean z2) {
        boolean z3;
        String str = "applyNewState(" + apnContext.getApnType() + ", " + z + "(" + apnContext.isEnabled() + "), " + z2 + "(" + apnContext.getDependencyMet() + "))";
        log(str);
        apnContext.requestLog(str);
        boolean zMtkApplyNewStateOnDisable = false;
        if (apnContext.isReady()) {
            if (z && z2) {
                DctConstants.State state = apnContext.getState();
                switch (AnonymousClass6.$SwitchMap$com$android$internal$telephony$DctConstants$State[state.ordinal()]) {
                    case 1:
                    case 2:
                    case 4:
                        log("applyNewState: 'ready' so return");
                        apnContext.requestLog("applyNewState state=" + state + ", so return");
                        break;
                    case 3:
                    case 5:
                    case 6:
                    case 7:
                        apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
                        zMtkApplyNewStateOnDisable = true;
                    default:
                        z3 = zMtkApplyNewStateOnDisable;
                        zMtkApplyNewStateOnDisable = true;
                        break;
                }
                return;
            }
            if (mtkIsApplyNewStateOnDisable(z)) {
                zMtkApplyNewStateOnDisable = mtkApplyNewStateOnDisable(true, apnContext);
                z3 = false;
            } else if (z2) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DISABLED);
                if ((apnContext.getApnType() == "dun" && teardownForDun()) || apnContext.getState() != DctConstants.State.CONNECTED) {
                    String str2 = "Clean up the connection. Apn type = " + apnContext.getApnType() + ", state = " + apnContext.getState();
                    log(str2);
                    apnContext.requestLog(str2);
                }
                z3 = false;
            } else {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_UNMET);
            }
            z3 = zMtkApplyNewStateOnDisable;
            zMtkApplyNewStateOnDisable = true;
        } else if (z && z2) {
            if (apnContext.isEnabled()) {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_DEPENDENCY_MET);
            } else {
                apnContext.setReason(PhoneInternalInterface.REASON_DATA_ENABLED);
            }
            if (apnContext.getState() == DctConstants.State.FAILED) {
                apnContext.setState(DctConstants.State.IDLE);
            }
            z3 = true;
        } else {
            z3 = false;
        }
        apnContext.setEnabled(z);
        apnContext.setDependencyMet(z2);
        if (zMtkApplyNewStateOnDisable) {
            cleanUpConnection(true, apnContext);
        }
        if (z3) {
            apnContext.resetErrorCodeRetries();
            trySetupData(apnContext);
        }
    }

    protected DcAsyncChannel checkForCompatibleConnectedApnContext(ApnContext apnContext) {
        ArrayList<ApnSetting> arrayListSortApnListByPreferred;
        String apnType = apnContext.getApnType();
        if ("dun".equals(apnType)) {
            arrayListSortApnListByPreferred = sortApnListByPreferred(fetchDunApns());
        } else {
            arrayListSortApnListByPreferred = null;
        }
        log("checkForCompatibleConnectedApnContext: apnContext=" + apnContext);
        DcAsyncChannel dcAsyncChannel = null;
        ApnContext apnContext2 = null;
        for (ApnContext apnContext3 : this.mApnContexts.values()) {
            DcAsyncChannel dcAc = apnContext3.getDcAc();
            if (dcAc != null) {
                ApnSetting apnSetting = apnContext3.getApnSetting();
                log("apnSetting: " + apnSetting);
                if (arrayListSortApnListByPreferred != null && arrayListSortApnListByPreferred.size() > 0) {
                    Iterator<ApnSetting> it = arrayListSortApnListByPreferred.iterator();
                    while (it.hasNext()) {
                        if (it.next().equals(apnSetting)) {
                            int i = AnonymousClass6.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext3.getState().ordinal()];
                            if (i == 1) {
                                log("checkForCompatibleConnectedApnContext: found dun conn=" + dcAc + " curApnCtx=" + apnContext3);
                                return dcAc;
                            }
                            switch (i) {
                                case 3:
                                case 4:
                                    apnContext2 = apnContext3;
                                    dcAsyncChannel = dcAc;
                                    break;
                            }
                        }
                    }
                } else if (apnSetting != null && apnSetting.canHandleType(apnType)) {
                    int i2 = AnonymousClass6.$SwitchMap$com$android$internal$telephony$DctConstants$State[apnContext3.getState().ordinal()];
                    if (i2 == 1) {
                        log("checkForCompatibleConnectedApnContext: found canHandle conn=" + dcAc + " curApnCtx=" + apnContext3);
                        return dcAc;
                    }
                    switch (i2) {
                        case 3:
                        case 4:
                            apnContext2 = apnContext3;
                            dcAsyncChannel = dcAc;
                            break;
                    }
                }
            }
        }
        if (dcAsyncChannel != null) {
            log("checkForCompatibleConnectedApnContext: found potential conn=" + dcAsyncChannel + " curApnCtx=" + apnContext2);
            return dcAsyncChannel;
        }
        log("checkForCompatibleConnectedApnContext: NO conn apnContext=" + apnContext);
        return null;
    }

    public void setEnabled(int i, boolean z) {
        Message messageObtainMessage = obtainMessage(270349);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.arg2 = z ? 1 : 0;
        sendMessage(messageObtainMessage);
    }

    private void onEnableApn(int i, int i2) {
        ApnContext apnContext = this.mApnContextsById.get(i);
        if (apnContext == null) {
            loge("onEnableApn(" + i + ", " + i2 + "): NO ApnContext");
            return;
        }
        log("onEnableApn: apnContext=" + apnContext + " call applyNewState");
        applyNewState(apnContext, i2 == 1, apnContext.getDependencyMet());
        if (i2 == 0 && isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && !isHigherPriorityApnContextActive(apnContext)) {
            log("onEnableApn: isOnlySingleDcAllowed true & higher priority APN disabled");
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
        }
    }

    protected boolean onTrySetupData(String str) {
        log("onTrySetupData: reason=" + str);
        setupDataOnConnectableApns(str);
        return true;
    }

    protected boolean onTrySetupData(ApnContext apnContext) {
        log("onTrySetupData: apnContext=" + apnContext);
        return trySetupData(apnContext);
    }

    public boolean isUserDataEnabled() {
        if (this.mDataEnabledSettings.isProvisioning()) {
            return this.mDataEnabledSettings.isProvisioningDataEnabled();
        }
        return this.mDataEnabledSettings.isUserDataEnabled();
    }

    public void setDataRoamingEnabledByUser(boolean z) {
        int subId = this.mPhone.getSubId();
        if (getDataRoamingEnabled() != z) {
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                Settings.Global.putInt(this.mResolver, "data_roaming", z ? 1 : 0);
                setDataRoamingFromUserAction(true);
            } else {
                Settings.Global.putInt(this.mResolver, "data_roaming" + subId, z ? 1 : 0);
            }
            this.mSubscriptionManager.setDataRoaming(z ? 1 : 0, subId);
            log("setDataRoamingEnabledByUser: set phoneSubId=" + subId + " isRoaming=" + z);
            return;
        }
        log("setDataRoamingEnabledByUser: unchanged phoneSubId=" + subId + " isRoaming=" + z);
    }

    public boolean getDataRoamingEnabled() {
        int subId = this.mPhone.getSubId();
        if (TelephonyManager.getDefault().getSimCount() == 1) {
            if (Settings.Global.getInt(this.mResolver, "data_roaming", getDefaultDataRoamingEnabled() ? 1 : 0) == 0) {
                return false;
            }
        } else {
            if (Settings.Global.getInt(this.mResolver, "data_roaming" + subId, getDefaultDataRoamingEnabled() ? 1 : 0) == 0) {
                return false;
            }
        }
        return true;
    }

    private boolean getDefaultDataRoamingEnabled() {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        return carrierConfigManager.getConfigForSubId(this.mPhone.getSubId()).getBoolean("carrier_default_data_roaming_enabled_bool") | "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.dataroaming", "false"));
    }

    private void setDefaultDataRoamingEnabled() {
        String str = "data_roaming";
        boolean z = true;
        if (TelephonyManager.getDefault().getSimCount() != 1) {
            str = "data_roaming" + this.mPhone.getSubId();
            try {
                Settings.Global.getInt(this.mResolver, str);
                z = false;
            } catch (Settings.SettingNotFoundException e) {
            }
        } else if (isDataRoamingFromUserAction()) {
            z = false;
        }
        if (z) {
            boolean defaultDataRoamingEnabled = getDefaultDataRoamingEnabled();
            log("setDefaultDataRoamingEnabled: " + str + "default value: " + defaultDataRoamingEnabled);
            Settings.Global.putInt(this.mResolver, str, defaultDataRoamingEnabled ? 1 : 0);
            this.mSubscriptionManager.setDataRoaming(defaultDataRoamingEnabled ? 1 : 0, this.mPhone.getSubId());
        }
    }

    private boolean isDataRoamingFromUserAction() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        if (!defaultSharedPreferences.contains(Phone.DATA_ROAMING_IS_USER_SETTING_KEY) && Settings.Global.getInt(this.mResolver, "device_provisioned", 0) == 0) {
            defaultSharedPreferences.edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, false).commit();
        }
        return defaultSharedPreferences.getBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, true);
    }

    private void setDataRoamingFromUserAction(boolean z) {
        PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext()).edit().putBoolean(Phone.DATA_ROAMING_IS_USER_SETTING_KEY, z).commit();
    }

    protected void onDataRoamingOff() {
        log("onDataRoamingOff");
        if (!getDataRoamingEnabled()) {
            setInitialAttachApn();
            setDataProfilesAsNeeded();
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_OFF);
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_OFF);
            return;
        }
        notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_OFF);
    }

    protected void onDataRoamingOnOrSettingsChanged(int i) {
        boolean z;
        log("onDataRoamingOnOrSettingsChanged");
        if (i != 270384) {
            z = false;
        } else {
            z = true;
        }
        if (!this.mPhone.getServiceState().getDataRoaming()) {
            log("device is not roaming. ignored the request.");
            return;
        }
        checkDataRoamingStatus(z);
        if (getDataRoamingEnabled()) {
            log("onDataRoamingOnOrSettingsChanged: setup data on roaming");
            setupDataOnConnectableApns(PhoneInternalInterface.REASON_ROAMING_ON);
            notifyDataConnection(PhoneInternalInterface.REASON_ROAMING_ON);
        } else {
            log("onDataRoamingOnOrSettingsChanged: Tear down data connection on roaming.");
            cleanUpAllConnections(true, PhoneInternalInterface.REASON_ROAMING_ON);
            notifyOffApnsOfAvailability(PhoneInternalInterface.REASON_ROAMING_ON);
        }
    }

    protected void checkDataRoamingStatus(boolean z) {
        if (!z && !getDataRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (apnContext.getState() == DctConstants.State.CONNECTED) {
                    LocalLog localLog = this.mDataRoamingLeakageLog;
                    StringBuilder sb = new StringBuilder();
                    sb.append("PossibleRoamingLeakage  connection params: ");
                    sb.append(apnContext.getDcAc() != null ? apnContext.getDcAc().mLastConnectionParams : "");
                    localLog.log(sb.toString());
                }
            }
        }
    }

    private void onRadioAvailable() {
        log("onRadioAvailable");
        if (this.mPhone.getSimulatedRadioControl() != null) {
            notifyDataConnection(null);
            log("onRadioAvailable: We're on the simulator; assuming data is connected");
        }
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null && iccRecords.getRecordsLoaded()) {
            notifyOffApnsOfAvailability(null);
        }
        if (getOverallState() != DctConstants.State.IDLE) {
            cleanUpConnection(true, null);
        }
    }

    protected void onRadioOffOrNotAvailable() {
        this.mReregisterOnReconnectFailure = false;
        this.mAutoAttachOnCreation.set(false);
        if (this.mPhone.getSimulatedRadioControl() != null) {
            log("We're on the simulator; assuming radio off is meaningless");
        } else {
            log("onRadioOffOrNotAvailable: is off and clean up all connections");
            cleanUpAllConnections(false, PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
        }
        if (mtkSkipNotifyOnRadioOffOrNotAvailable()) {
            return;
        }
        notifyOffApnsOfAvailability(null);
    }

    protected void completeConnection(ApnContext apnContext) {
        log("completeConnection: successful, notify the world apnContext=" + apnContext);
        if (this.mIsProvisioning && !TextUtils.isEmpty(this.mProvisioningUrl)) {
            log("completeConnection: MOBILE_PROVISIONING_ACTION url=" + this.mProvisioningUrl);
            Intent intentMakeMainSelectorActivity = Intent.makeMainSelectorActivity("android.intent.action.MAIN", "android.intent.category.APP_BROWSER");
            intentMakeMainSelectorActivity.setData(Uri.parse(this.mProvisioningUrl));
            intentMakeMainSelectorActivity.setFlags(272629760);
            try {
                this.mPhone.getContext().startActivity(intentMakeMainSelectorActivity);
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

    protected void onDataSetupComplete(AsyncResult asyncResult) {
        int i;
        DcFailCause dcFailCause = DcFailCause.UNKNOWN;
        ApnContext validApnContext = getValidApnContext(asyncResult, "onDataSetupComplete");
        if (validApnContext == null) {
            return;
        }
        boolean z = true;
        if (asyncResult.exception == null) {
            DcAsyncChannel dcAc = validApnContext.getDcAc();
            if (dcAc == null) {
                log("onDataSetupComplete: no connection to DC, handle as error");
                DcFailCause dcFailCause2 = DcFailCause.CONNECTION_TO_DATACONNECTIONAC_BROKEN;
            } else {
                ApnSetting apnSetting = validApnContext.getApnSetting();
                StringBuilder sb = new StringBuilder();
                sb.append("onDataSetupComplete: success apn=");
                sb.append(apnSetting == null ? "unknown" : apnSetting.apn);
                log(sb.toString());
                if (apnSetting != null && apnSetting.proxy != null && apnSetting.proxy.length() != 0) {
                    try {
                        String str = apnSetting.port;
                        if (TextUtils.isEmpty(str)) {
                            str = "8080";
                        }
                        dcAc.setLinkPropertiesHttpProxySync(new ProxyInfo(apnSetting.proxy, Integer.parseInt(str), null));
                    } catch (NumberFormatException e) {
                        loge("onDataSetupComplete: NumberFormatException making ProxyProperties (" + apnSetting.port + "): " + e);
                    }
                }
                if (TextUtils.equals(validApnContext.getApnType(), "default")) {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "true");
                    } catch (RuntimeException e2) {
                        log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to true");
                    }
                    if (this.mCanSetPreferApn && this.mPreferredApn == null) {
                        log("onDataSetupComplete: PREFERRED APN is null");
                        this.mPreferredApn = apnSetting;
                        if (this.mPreferredApn != null) {
                            setPreferredApn(this.mPreferredApn.id);
                        }
                    }
                } else {
                    try {
                        SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
                    } catch (RuntimeException e3) {
                        log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
                    }
                }
                validApnContext.setState(DctConstants.State.CONNECTED);
                checkDataRoamingStatus(false);
                boolean zIsProvisioningApn = validApnContext.isProvisioningApn();
                ConnectivityManager connectivityManagerFrom = ConnectivityManager.from(this.mPhone.getContext());
                if (this.mProvisionBroadcastReceiver != null) {
                    this.mPhone.getContext().unregisterReceiver(this.mProvisionBroadcastReceiver);
                    this.mProvisionBroadcastReceiver = null;
                }
                if (!zIsProvisioningApn || this.mIsProvisioning) {
                    connectivityManagerFrom.setProvisioningNotificationVisible(false, 0, this.mProvisionActionName);
                    completeConnection(validApnContext);
                } else {
                    log("onDataSetupComplete: successful, BUT send connected to prov apn as mIsProvisioning:" + this.mIsProvisioning + " == false && (isProvisioningApn:" + zIsProvisioningApn + " == true");
                    this.mProvisionBroadcastReceiver = new ProvisionNotificationBroadcastReceiver(connectivityManagerFrom.getMobileProvisioningUrl(), TelephonyManager.getDefault().getNetworkOperatorName());
                    this.mPhone.getContext().registerReceiver(this.mProvisionBroadcastReceiver, new IntentFilter(this.mProvisionActionName));
                    connectivityManagerFrom.setProvisioningNotificationVisible(true, 0, this.mProvisionActionName);
                    setRadio(false);
                }
                log("onDataSetupComplete: SETUP complete type=" + validApnContext.getApnType() + ", reason:" + validApnContext.getReason());
                if (Build.IS_DEBUGGABLE && (i = SystemProperties.getInt("persist.radio.test.pco", -1)) != -1) {
                    log("PCO testing: read pco value from persist.radio.test.pco " + i);
                    byte[] bArr = {(byte) i};
                    Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE");
                    intent.putExtra("apnType", "default");
                    intent.putExtra("apnProto", "IPV4V6");
                    intent.putExtra("pcoId", 65280);
                    intent.putExtra("pcoValue", bArr);
                    this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
                }
                z = false;
            }
        } else {
            DcFailCause dcFailCause3 = (DcFailCause) asyncResult.result;
            ApnSetting apnSetting2 = validApnContext.getApnSetting();
            Object[] objArr = new Object[2];
            objArr[0] = apnSetting2 == null ? "unknown" : apnSetting2.apn;
            objArr[1] = dcFailCause3;
            log(String.format("onDataSetupComplete: error apn=%s cause=%s", objArr));
            if (dcFailCause3.isEventLoggable()) {
                EventLog.writeEvent(EventLogTags.PDP_SETUP_FAIL, Integer.valueOf(dcFailCause3.ordinal()), Integer.valueOf(getCellLocationId()), Integer.valueOf(TelephonyManager.getDefault().getNetworkType()));
            }
            ApnSetting apnSetting3 = validApnContext.getApnSetting();
            this.mPhone.notifyPreciseDataConnectionFailed(validApnContext.getReason(), validApnContext.getApnType(), apnSetting3 != null ? apnSetting3.apn : "unknown", dcFailCause3.toString());
            Intent intent2 = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED");
            intent2.putExtra("errorCode", dcFailCause3.getErrorCode());
            intent2.putExtra("apnType", validApnContext.getApnType());
            this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent2);
            if (dcFailCause3.isRestartRadioFail(this.mPhone.getContext(), this.mPhone.getSubId()) || validApnContext.restartOnError(dcFailCause3.getErrorCode())) {
                log("Modem restarted.");
                sendRestartRadio();
            }
            if (isPermanentFailure(dcFailCause3) || mtkIgnoredPermanentFailure(dcFailCause3)) {
                log("cause = " + dcFailCause3 + ", mark apn as permanent failed. apn = " + apnSetting3);
                validApnContext.markApnPermanentFailed(apnSetting3);
            }
        }
        if (z) {
            onDataSetupCompleteError(asyncResult);
        }
        if (!this.mDataEnabledSettings.isInternalDataEnabled()) {
            cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED);
        }
    }

    protected ApnContext getValidApnContext(AsyncResult asyncResult, String str) {
        if (asyncResult != null && (asyncResult.userObj instanceof Pair)) {
            Pair pair = (Pair) asyncResult.userObj;
            ApnContext apnContext = (ApnContext) pair.first;
            if (apnContext != null) {
                int connectionGeneration = apnContext.getConnectionGeneration();
                log("getValidApnContext (" + str + ") on " + apnContext + " got " + connectionGeneration + " vs " + pair.second);
                if (connectionGeneration == ((Integer) pair.second).intValue()) {
                    return apnContext;
                }
                log("ignoring obsolete " + str);
                return null;
            }
        }
        throw new RuntimeException(str + ": No apnContext");
    }

    protected void onDataSetupCompleteError(AsyncResult asyncResult) {
        ApnContext validApnContext = getValidApnContext(asyncResult, "onDataSetupCompleteError");
        if (validApnContext == null) {
            return;
        }
        long delayForNextApn = validApnContext.getDelayForNextApn(this.mFailFast);
        if (delayForNextApn >= 0) {
            log("onDataSetupCompleteError: Try next APN. delay = " + delayForNextApn);
            validApnContext.setState(DctConstants.State.SCANNING);
            startAlarmForReconnect(delayForNextApn, validApnContext);
            return;
        }
        validApnContext.setState(DctConstants.State.FAILED);
        this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_APN_FAILED, validApnContext.getApnType());
        validApnContext.setDataConnectionAc(null);
        log("onDataSetupCompleteError: Stop retrying APNs.");
    }

    private void onDataConnectionRedirected(String str) {
        if (!TextUtils.isEmpty(str)) {
            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED");
            intent.putExtra("redirectionUrl", str);
            this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
            log("Notify carrier signal receivers with redirectUrl: " + str);
        }
    }

    protected void onDisconnectDone(AsyncResult asyncResult) {
        ApnContext validApnContext = getValidApnContext(asyncResult, "onDisconnectDone");
        if (validApnContext == null) {
            return;
        }
        log("onDisconnectDone: EVENT_DISCONNECT_DONE apnContext=" + validApnContext);
        validApnContext.setState(DctConstants.State.IDLE);
        this.mPhone.notifyDataConnection(validApnContext.getReason(), validApnContext.getApnType());
        if (isDisconnected() && this.mPhone.getServiceStateTracker().processPendingRadioPowerOffAfterDataOff()) {
            log("onDisconnectDone: radio will be turned off, no retries");
            validApnContext.setApnSetting(null);
            validApnContext.setDataConnectionAc(null);
            if (this.mDisconnectPendingCount > 0) {
                this.mDisconnectPendingCount--;
            }
            if (this.mDisconnectPendingCount == 0) {
                notifyDataDisconnectComplete();
                notifyAllDataDisconnected();
                return;
            }
            return;
        }
        if (this.mAttached.get() && validApnContext.isReady() && retryAfterDisconnected(validApnContext)) {
            try {
                SystemProperties.set(PUPPET_MASTER_RADIO_STRESS_TEST, "false");
            } catch (RuntimeException e) {
                log("Failed to set PUPPET_MASTER_RADIO_STRESS_TEST to false");
            }
            log("onDisconnectDone: attached, ready and retry after disconnect");
            long jMtkModifyInterApnDelay = mtkModifyInterApnDelay(validApnContext.getRetryAfterDisconnectDelay(), validApnContext.getApnType());
            if (jMtkModifyInterApnDelay > 0) {
                startAlarmForReconnect(jMtkModifyInterApnDelay, validApnContext);
            }
        } else {
            boolean z = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.majorWeightMax);
            if (validApnContext.isProvisioningApn() && z) {
                log("onDisconnectDone: restartRadio after provisioning");
                restartRadio();
            }
            validApnContext.setApnSetting(null);
            validApnContext.setDataConnectionAc(null);
            if (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology())) {
                log("onDisconnectDone: isOnlySigneDcAllowed true so setup single apn");
                setupDataOnConnectableApns(PhoneInternalInterface.REASON_SINGLE_PDN_ARBITRATION);
            } else {
                log("onDisconnectDone: not retrying");
            }
        }
        if (this.mDisconnectPendingCount > 0) {
            this.mDisconnectPendingCount--;
        }
        if (this.mDisconnectPendingCount == 0) {
            validApnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
    }

    private void onDisconnectDcRetrying(AsyncResult asyncResult) {
        ApnContext validApnContext = getValidApnContext(asyncResult, "onDisconnectDcRetrying");
        if (validApnContext == null) {
            return;
        }
        validApnContext.setState(DctConstants.State.RETRYING);
        log("onDisconnectDcRetrying: apnContext=" + validApnContext);
        this.mPhone.notifyDataConnection(validApnContext.getReason(), validApnContext.getApnType());
    }

    private void onVoiceCallStarted() {
        log("onVoiceCallStarted");
        this.mInVoiceCall = true;
        if (isConnected() && !this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
            log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_STARTED);
        }
    }

    private void onVoiceCallEnded() {
        log("onVoiceCallEnded");
        this.mInVoiceCall = false;
        if (isConnected()) {
            if (!this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                startNetStatPoll();
                startDataStallAlarm(false);
                notifyDataConnection(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
            } else {
                resetPollStats();
            }
        }
        setupDataOnConnectableApns(PhoneInternalInterface.REASON_VOICE_CALL_ENDED);
    }

    protected void onCleanUpConnection(boolean z, int i, String str) {
        log("onCleanUpConnection");
        ApnContext apnContext = this.mApnContextsById.get(i);
        if (apnContext != null) {
            apnContext.setReason(str);
            cleanUpConnection(z, apnContext);
        }
    }

    protected boolean isConnected() {
        Iterator<ApnContext> it = this.mApnContexts.values().iterator();
        while (it.hasNext()) {
            if (it.next().getState() == DctConstants.State.CONNECTED) {
                return true;
            }
        }
        return false;
    }

    public boolean isDisconnected() {
        Iterator<ApnContext> it = this.mApnContexts.values().iterator();
        while (it.hasNext()) {
            if (!it.next().isDisconnected()) {
                return false;
            }
        }
        return true;
    }

    protected void notifyDataConnection(String str) {
        String reason;
        log("notifyDataConnection: reason=" + str);
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (this.mAttached.get() && apnContext.isReady()) {
                log("notifyDataConnection: type:" + apnContext.getApnType());
                Phone phone = this.mPhone;
                if (str == null) {
                    reason = apnContext.getReason();
                } else {
                    reason = str;
                }
                phone.notifyDataConnection(reason, apnContext.getApnType());
            }
        }
        notifyOffApnsOfAvailability(str);
    }

    protected void setDataProfilesAsNeeded() {
        log("setDataProfilesAsNeeded");
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            ArrayList arrayList = new ArrayList();
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                if (apnSetting.modemCognitive) {
                    DataProfile dataProfileCreateDataProfile = createDataProfile(apnSetting);
                    if (!arrayList.contains(dataProfileCreateDataProfile)) {
                        arrayList.add(dataProfileCreateDataProfile);
                    }
                }
            }
            if (arrayList.size() > 0) {
                this.mDataServiceManager.setDataProfile(arrayList, this.mPhone.getServiceState().getDataRoamingFromRegistration(), null);
            }
        }
    }

    protected void createAllApnList() {
        this.mMvnoMatched = false;
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric(this.mIccRecords.get());
        synchronized (this.mRefCountLock) {
            this.mAllApnSettings = new ArrayList<>();
            if (strMtkGetOperatorNumeric != null) {
                String str = "numeric = '" + strMtkGetOperatorNumeric + "'";
                log("createAllApnList: selection=" + str);
                Cursor cursorQuery = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "filtered"), null, str, null, HbpcdLookup.ID);
                if (cursorQuery != null) {
                    if (cursorQuery.getCount() > 0) {
                        this.mAllApnSettings = createApnList(cursorQuery);
                    }
                    cursorQuery.close();
                }
            }
            mtkAddVsimApnTypeToDefaultApnSetting();
            mtkAddPreemptApnTypeToDefaultApnSetting();
        }
        addEmergencyApnSetting();
        dedupeApnSettings();
        if (this.mAllApnSettings.isEmpty()) {
            log("createAllApnList: No APN found for carrier: " + strMtkGetOperatorNumeric);
            this.mPreferredApn = null;
        } else {
            this.mPreferredApn = getPreferredApn();
            if (this.mPreferredApn != null && !this.mPreferredApn.numeric.equals(strMtkGetOperatorNumeric)) {
                this.mPreferredApn = null;
                setPreferredApn(-1);
            }
            log("createAllApnList: mPreferredApn=" + this.mPreferredApn);
        }
        log("createAllApnList: X mAllApnSettings=" + this.mAllApnSettings);
        if (mtkSkipSetDataProfileAsNeeded(strMtkGetOperatorNumeric)) {
            return;
        }
        setDataProfilesAsNeeded();
    }

    protected void dedupeApnSettings() {
        new ArrayList();
        synchronized (this.mRefCountLock) {
            int i = 0;
            while (i < this.mAllApnSettings.size() - 1) {
                int i2 = i + 1;
                ApnSetting apnSettingMergeApns = this.mAllApnSettings.get(i);
                int i3 = i2;
                while (i3 < this.mAllApnSettings.size()) {
                    ApnSetting apnSetting = this.mAllApnSettings.get(i3);
                    if (apnSettingMergeApns.similar(apnSetting)) {
                        apnSettingMergeApns = mergeApns(apnSettingMergeApns, apnSetting);
                        this.mAllApnSettings.set(i, apnSettingMergeApns);
                        this.mAllApnSettings.remove(i3);
                    } else {
                        i3++;
                    }
                }
                i = i2;
            }
        }
    }

    protected ApnSetting mergeApns(ApnSetting apnSetting, ApnSetting apnSetting2) {
        String str;
        String str2;
        String str3;
        String str4;
        String str5;
        String str6;
        String str7;
        int iConvertBearerBitmaskToNetworkTypeBitmask;
        int i;
        int i2 = apnSetting.id;
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(Arrays.asList(apnSetting.types));
        int i3 = i2;
        for (String str8 : apnSetting2.types) {
            if (!arrayList.contains(str8)) {
                arrayList.add(str8);
            }
            if (str8.equals("default")) {
                i3 = apnSetting2.id;
            }
        }
        if (TextUtils.isEmpty(apnSetting.mmsc)) {
            str = apnSetting2.mmsc;
        } else {
            str = apnSetting.mmsc;
        }
        String str9 = str;
        if (TextUtils.isEmpty(apnSetting.mmsProxy)) {
            str2 = apnSetting2.mmsProxy;
        } else {
            str2 = apnSetting.mmsProxy;
        }
        String str10 = str2;
        if (TextUtils.isEmpty(apnSetting.mmsPort)) {
            str3 = apnSetting2.mmsPort;
        } else {
            str3 = apnSetting.mmsPort;
        }
        String str11 = str3;
        if (TextUtils.isEmpty(apnSetting.proxy)) {
            str4 = apnSetting2.proxy;
        } else {
            str4 = apnSetting.proxy;
        }
        String str12 = str4;
        if (TextUtils.isEmpty(apnSetting.port)) {
            str5 = apnSetting2.port;
        } else {
            str5 = apnSetting.port;
        }
        String str13 = str5;
        if (apnSetting2.protocol.equals("IPV4V6")) {
            str6 = apnSetting2.protocol;
        } else {
            str6 = apnSetting.protocol;
        }
        String str14 = str6;
        if (apnSetting2.roamingProtocol.equals("IPV4V6")) {
            str7 = apnSetting2.roamingProtocol;
        } else {
            str7 = apnSetting.roamingProtocol;
        }
        String str15 = str7;
        if (apnSetting.networkTypeBitmask != 0 && apnSetting2.networkTypeBitmask != 0) {
            iConvertBearerBitmaskToNetworkTypeBitmask = apnSetting.networkTypeBitmask | apnSetting2.networkTypeBitmask;
        } else {
            iConvertBearerBitmaskToNetworkTypeBitmask = 0;
        }
        if (iConvertBearerBitmaskToNetworkTypeBitmask == 0) {
            if (apnSetting.bearerBitmask != 0 && apnSetting2.bearerBitmask != 0) {
                i = apnSetting.bearerBitmask | apnSetting2.bearerBitmask;
            } else {
                i = 0;
            }
            iConvertBearerBitmaskToNetworkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(i);
        }
        return new ApnSetting(i3, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, str12, str13, str9, str10, str11, apnSetting.user, apnSetting.password, apnSetting.authType, (String[]) arrayList.toArray(new String[0]), str14, str15, apnSetting.carrierEnabled, iConvertBearerBitmaskToNetworkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive || apnSetting2.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId);
    }

    protected DcAsyncChannel createDataConnection() {
        log("createDataConnection E");
        int andIncrement = this.mUniqueIdGenerator.getAndIncrement();
        DataConnection dataConnectionMakeDataConnection = DataConnection.makeDataConnection(this.mPhone, andIncrement, this, this.mDataServiceManager, this.mDcTesterFailBringUpAll, this.mDcc);
        this.mDataConnections.put(Integer.valueOf(andIncrement), dataConnectionMakeDataConnection);
        DcAsyncChannel dcAsyncChannelMakeDcAsyncChannel = TelephonyComponentFactory.getInstance().makeDcAsyncChannel(dataConnectionMakeDataConnection, LOG_TAG);
        int iFullyConnectSync = dcAsyncChannelMakeDcAsyncChannel.fullyConnectSync(this.mPhone.getContext(), this, dataConnectionMakeDataConnection.getHandler());
        if (iFullyConnectSync == 0) {
            this.mDataConnectionAcHashMap.put(Integer.valueOf(dcAsyncChannelMakeDcAsyncChannel.getDataConnectionIdSync()), dcAsyncChannelMakeDcAsyncChannel);
        } else {
            loge("createDataConnection: Could not connect to dcac=" + dcAsyncChannelMakeDcAsyncChannel + " status=" + iFullyConnectSync);
        }
        log("createDataConnection() X id=" + andIncrement + " dc=" + dataConnectionMakeDataConnection);
        return dcAsyncChannelMakeDcAsyncChannel;
    }

    private void destroyDataConnections() {
        if (this.mDataConnections != null) {
            log("destroyDataConnections: clear mDataConnectionList");
            this.mDataConnections.clear();
        } else {
            log("destroyDataConnections: mDataConnecitonList is empty, ignore");
        }
    }

    protected ArrayList<ApnSetting> buildWaitingApns(String str, int i) {
        log("buildWaitingApns: E requestedApnType=" + str);
        ArrayList<ApnSetting> arrayList = new ArrayList<>();
        if (str.equals("dun")) {
            ArrayList<ApnSetting> arrayListFetchDunApns = fetchDunApns();
            if (arrayListFetchDunApns.size() > 0) {
                Iterator<ApnSetting> it = arrayListFetchDunApns.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                    log("buildWaitingApns: X added APN_TYPE_DUN apnList=" + arrayList);
                }
                return sortApnListByPreferred(arrayList);
            }
        }
        IccRecords iccRecords = this.mIccRecords.get();
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric(iccRecords);
        boolean z = true;
        try {
            z = true ^ this.mPhone.getContext().getResources().getBoolean(R.^attr-private.enableControlView);
        } catch (Resources.NotFoundException e) {
            log("buildWaitingApns: usePreferred NotFoundException set to true");
        }
        if (z) {
            this.mPreferredApn = getPreferredApn();
        }
        log("buildWaitingApns: usePreferred=" + z + " canSetPreferApn=" + this.mCanSetPreferApn + " mPreferredApn=" + this.mPreferredApn + " operator=" + strMtkGetOperatorNumeric + " radioTech=" + i + " IccRecords r=" + iccRecords);
        if (z && this.mCanSetPreferApn && this.mPreferredApn != null && this.mPreferredApn.canHandleType(str)) {
            log("buildWaitingApns: Preferred APN:" + strMtkGetOperatorNumeric + ":" + this.mPreferredApn.numeric + ":" + this.mPreferredApn);
            if (this.mPreferredApn.numeric.equals(strMtkGetOperatorNumeric) && ServiceState.bitmaskHasTech(this.mPreferredApn.networkTypeBitmask, ServiceState.rilRadioTechnologyToNetworkType(i))) {
                arrayList.add(this.mPreferredApn);
                ArrayList<ApnSetting> arrayListSortApnListByPreferred = sortApnListByPreferred(arrayList);
                log("buildWaitingApns: X added preferred apnList=" + arrayListSortApnListByPreferred);
                return arrayListSortApnListByPreferred;
            }
            log("buildWaitingApns: no preferred APN");
            setPreferredApn(-1);
            this.mPreferredApn = null;
        }
        if (this.mAllApnSettings != null) {
            log("buildWaitingApns: mAllApnSettings=" + this.mAllApnSettings);
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                if (!apnSetting.canHandleType(str)) {
                    log("buildWaitingApns: couldn't handle requested ApnType=" + str);
                } else if (ServiceState.bitmaskHasTech(apnSetting.networkTypeBitmask, ServiceState.rilRadioTechnologyToNetworkType(i))) {
                    log("buildWaitingApns: adding apn=" + apnSetting);
                    arrayList.add(apnSetting);
                } else {
                    log("buildWaitingApns: bearerBitmask:" + apnSetting.bearerBitmask + " or networkTypeBitmask:" + apnSetting.networkTypeBitmask + "do not include radioTech:" + i);
                }
            }
        } else {
            loge("mAllApnSettings is null!");
        }
        ArrayList<ApnSetting> arrayListSortApnListByPreferred2 = sortApnListByPreferred(arrayList);
        log("buildWaitingApns: " + arrayListSortApnListByPreferred2.size() + " APNs in the list: " + arrayListSortApnListByPreferred2);
        return arrayListSortApnListByPreferred2;
    }

    @VisibleForTesting
    public ArrayList<ApnSetting> sortApnListByPreferred(ArrayList<ApnSetting> arrayList) {
        final int preferredApnSetId;
        if (arrayList != null && arrayList.size() > 1 && (preferredApnSetId = getPreferredApnSetId()) != 0) {
            arrayList.sort(new Comparator<ApnSetting>() {
                @Override
                public int compare(ApnSetting apnSetting, ApnSetting apnSetting2) {
                    if (apnSetting.apnSetId == preferredApnSetId) {
                        return -1;
                    }
                    return apnSetting2.apnSetId == preferredApnSetId ? 1 : 0;
                }
            });
        }
        return arrayList;
    }

    protected String apnListToString(ArrayList<ApnSetting> arrayList) {
        StringBuilder sb = new StringBuilder();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            sb.append('[');
            sb.append(arrayList.get(i).toString());
            sb.append(']');
        }
        return sb.toString();
    }

    protected void setPreferredApn(int i) {
        if (!this.mCanSetPreferApn) {
            log("setPreferredApn: X !canSEtPreferApn");
            return;
        }
        Uri uriWithAppendedPath = Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, Long.toString(this.mPhone.getSubId()));
        log("setPreferredApn: delete");
        ContentResolver contentResolver = this.mPhone.getContext().getContentResolver();
        contentResolver.delete(uriWithAppendedPath, null, null);
        if (i >= 0) {
            log("setPreferredApn: insert");
            ContentValues contentValues = new ContentValues();
            contentValues.put(APN_ID, Integer.valueOf(i));
            contentResolver.insert(uriWithAppendedPath, contentValues);
        }
    }

    protected ApnSetting getPreferredApn() {
        if (this.mAllApnSettings == null || this.mAllApnSettings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("getPreferredApn: mAllApnSettings is ");
            sb.append(this.mAllApnSettings == null ? "null" : "empty");
            log(sb.toString());
            return null;
        }
        Cursor cursorQuery = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(PREFERAPN_NO_UPDATE_URI_USING_SUBID, Long.toString(this.mPhone.getSubId())), new String[]{HbpcdLookup.ID, "name", "apn"}, null, null, "name ASC");
        if (cursorQuery != null) {
            this.mCanSetPreferApn = true;
        } else {
            this.mCanSetPreferApn = false;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("getPreferredApn: mRequestedApnType=");
        sb2.append(this.mRequestedApnType);
        sb2.append(" cursor=");
        sb2.append(cursorQuery);
        sb2.append(" cursor.count=");
        sb2.append(cursorQuery != null ? cursorQuery.getCount() : 0);
        log(sb2.toString());
        if (this.mCanSetPreferApn && cursorQuery.getCount() > 0) {
            cursorQuery.moveToFirst();
            int i = cursorQuery.getInt(cursorQuery.getColumnIndexOrThrow(HbpcdLookup.ID));
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                log("getPreferredApn: apnSetting=" + apnSetting);
                if (apnSetting.id == i && apnSetting.canHandleType(this.mRequestedApnType)) {
                    log("getPreferredApn: X found apnSetting" + apnSetting);
                    cursorQuery.close();
                    return apnSetting;
                }
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        log("getPreferredApn: X not found");
        return null;
    }

    @Override
    public void handleMessage(Message message) {
        String str;
        boolean zIsProvisioningApn;
        int i = message.what;
        if (i == 69636) {
            log("DISCONNECTED_CONNECTED: msg=" + message);
            DcAsyncChannel dcAsyncChannel = (DcAsyncChannel) message.obj;
            this.mDataConnectionAcHashMap.remove(Integer.valueOf(dcAsyncChannel.getDataConnectionIdSync()));
            dcAsyncChannel.disconnected();
            return;
        }
        switch (i) {
            case 270336:
                onDataSetupComplete((AsyncResult) message.obj);
                return;
            case 270337:
                break;
            case 270338:
                int subId = this.mPhone.getSubId();
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    onRecordsLoadedOrSubIdChanged();
                    return;
                }
                log("Ignoring EVENT_RECORDS_LOADED as subId is not valid: " + subId);
                return;
            case 270339:
                if (message.obj instanceof ApnContext) {
                    onTrySetupData((ApnContext) message.obj);
                    return;
                } else if (message.obj instanceof String) {
                    onTrySetupData((String) message.obj);
                    return;
                } else {
                    loge("EVENT_TRY_SETUP request w/o apnContext or String");
                    return;
                }
            default:
                switch (i) {
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
                    default:
                        switch (i) {
                            case 270347:
                                onDataRoamingOnOrSettingsChanged(message.what);
                                return;
                            case 270348:
                                onDataRoamingOff();
                                return;
                            case 270349:
                                onEnableApn(message.arg1, message.arg2);
                                return;
                            default:
                                switch (i) {
                                    case 270351:
                                        log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DONE msg=" + message);
                                        onDisconnectDone((AsyncResult) message.obj);
                                        return;
                                    case 270352:
                                        onDataConnectionAttached();
                                        return;
                                    case 270353:
                                        onDataStallAlarm(message.arg1);
                                        return;
                                    case 270354:
                                        doRecovery();
                                        return;
                                    case 270355:
                                        onApnChanged();
                                        return;
                                    default:
                                        switch (i) {
                                            case 270358:
                                                log("EVENT_PS_RESTRICT_ENABLED " + this.mIsPsRestricted);
                                                stopNetStatPoll();
                                                stopDataStallAlarm();
                                                this.mIsPsRestricted = true;
                                                return;
                                            case 270359:
                                                log("EVENT_PS_RESTRICT_DISABLED " + this.mIsPsRestricted);
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
                                                if (apnContext != null) {
                                                    apnContext.setReason(PhoneInternalInterface.REASON_PS_RESTRICT_ENABLED);
                                                    trySetupData(apnContext);
                                                    return;
                                                } else {
                                                    loge("**** Default ApnContext not found ****");
                                                    if (Build.IS_DEBUGGABLE) {
                                                        throw new RuntimeException("Default ApnContext not found");
                                                    }
                                                    return;
                                                }
                                            case 270360:
                                                boolean z = message.arg1 != 0;
                                                log("EVENT_CLEAN_UP_CONNECTION tearDown=" + z);
                                                if (message.obj instanceof ApnContext) {
                                                    cleanUpConnection(z, (ApnContext) message.obj);
                                                    return;
                                                } else {
                                                    onCleanUpConnection(z, message.arg2, (String) message.obj);
                                                    return;
                                                }
                                            default:
                                                switch (i) {
                                                    case 270362:
                                                        restartRadio();
                                                        return;
                                                    case 270363:
                                                        onSetInternalDataEnabled(message.arg1 == 1, (Message) message.obj);
                                                        return;
                                                    default:
                                                        switch (i) {
                                                            case 270365:
                                                                if (message.obj != null && !(message.obj instanceof String)) {
                                                                    message.obj = null;
                                                                }
                                                                onCleanUpAllConnections((String) message.obj);
                                                                return;
                                                            case 270366:
                                                                boolean z2 = message.arg1 == 1;
                                                                log("CMD_SET_USER_DATA_ENABLE enabled=" + z2);
                                                                onSetUserDataEnabled(z2);
                                                                return;
                                                            case 270367:
                                                                boolean z3 = message.arg1 == 1;
                                                                log("CMD_SET_DEPENDENCY_MET met=" + z3);
                                                                Bundle data = message.getData();
                                                                if (data != null && (str = (String) data.get("apnType")) != null) {
                                                                    onSetDependencyMet(str, z3);
                                                                    return;
                                                                }
                                                                return;
                                                            case 270368:
                                                                onSetPolicyDataEnabled(message.arg1 == 1);
                                                                return;
                                                            case 270369:
                                                                onUpdateIcc();
                                                                return;
                                                            case 270370:
                                                                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DC_RETRYING msg=" + message);
                                                                onDisconnectDcRetrying((AsyncResult) message.obj);
                                                                return;
                                                            case 270371:
                                                                onDataSetupCompleteError((AsyncResult) message.obj);
                                                                return;
                                                            case 270372:
                                                                sEnableFailFastRefCounter += message.arg1 == 1 ? 1 : -1;
                                                                log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA:  sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                                                                if (sEnableFailFastRefCounter < 0) {
                                                                    loge("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: sEnableFailFastRefCounter:" + sEnableFailFastRefCounter + " < 0");
                                                                    sEnableFailFastRefCounter = 0;
                                                                }
                                                                boolean z4 = sEnableFailFastRefCounter > 0;
                                                                log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=" + z4 + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                                                                if (this.mFailFast != z4) {
                                                                    this.mFailFast = z4;
                                                                    this.mDataStallDetectionEnabled = !z4;
                                                                    if (this.mDataStallDetectionEnabled && getOverallState() == DctConstants.State.CONNECTED && (!this.mInVoiceCall || this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) {
                                                                        log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                                                                        stopDataStallAlarm();
                                                                        startDataStallAlarm(false);
                                                                        return;
                                                                    } else {
                                                                        log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                                                                        stopDataStallAlarm();
                                                                        return;
                                                                    }
                                                                }
                                                                return;
                                                            case 270373:
                                                                Bundle data2 = message.getData();
                                                                if (data2 != null) {
                                                                    try {
                                                                        this.mProvisioningUrl = (String) data2.get("provisioningUrl");
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
                                                                log("CMD_IS_PROVISIONING_APN");
                                                                try {
                                                                    Bundle data3 = message.getData();
                                                                    String str2 = data3 != null ? (String) data3.get("apnType") : null;
                                                                    if (TextUtils.isEmpty(str2)) {
                                                                        loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                                                                        zIsProvisioningApn = false;
                                                                    } else {
                                                                        zIsProvisioningApn = isProvisioningApn(str2);
                                                                    }
                                                                } catch (ClassCastException e2) {
                                                                    loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                                                                    zIsProvisioningApn = false;
                                                                }
                                                                log("CMD_IS_PROVISIONING_APN: ret=" + zIsProvisioningApn);
                                                                this.mReplyAc.replyToMessage(message, 270374, zIsProvisioningApn ? 1 : 0);
                                                                return;
                                                            case 270375:
                                                                log("EVENT_PROVISIONING_APN_ALARM");
                                                                ApnContext apnContext2 = this.mApnContextsById.get(0);
                                                                if (apnContext2.isProvisioningApn() && apnContext2.isConnectedOrConnecting()) {
                                                                    if (this.mProvisioningApnAlarmTag == message.arg1) {
                                                                        log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                                                                        this.mIsProvisioning = false;
                                                                        this.mProvisioningUrl = null;
                                                                        stopProvisioningApnAlarm();
                                                                        sendCleanUpConnection(true, apnContext2);
                                                                        return;
                                                                    }
                                                                    log("EVENT_PROVISIONING_APN_ALARM: ignore stale tag, mProvisioningApnAlarmTag:" + this.mProvisioningApnAlarmTag + " != arg1:" + message.arg1);
                                                                    return;
                                                                }
                                                                log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                                                                return;
                                                            case 270376:
                                                                if (message.arg1 == 1) {
                                                                    handleStartNetStatPoll((DctConstants.Activity) message.obj);
                                                                    return;
                                                                } else {
                                                                    if (message.arg1 == 0) {
                                                                        handleStopNetStatPoll((DctConstants.Activity) message.obj);
                                                                        return;
                                                                    }
                                                                    return;
                                                                }
                                                            case 270377:
                                                                if (this.mPhone.getServiceState().getRilDataRadioTechnology() != 0) {
                                                                    cleanUpConnectionsOnUpdatedApns(false, PhoneInternalInterface.REASON_NW_TYPE_CHANGED);
                                                                    setupDataOnConnectableApns(PhoneInternalInterface.REASON_NW_TYPE_CHANGED, RetryFailures.ONLY_ON_CHANGE);
                                                                    return;
                                                                }
                                                                return;
                                                            case 270378:
                                                                if (this.mProvisioningSpinner == message.obj) {
                                                                    this.mProvisioningSpinner.dismiss();
                                                                    this.mProvisioningSpinner = null;
                                                                    return;
                                                                }
                                                                return;
                                                            case 270379:
                                                                onDeviceProvisionedChange();
                                                                return;
                                                            case 270380:
                                                                String str3 = (String) message.obj;
                                                                log("dataConnectionTracker.handleMessage: EVENT_REDIRECTION_DETECTED=" + str3);
                                                                onDataConnectionRedirected(str3);
                                                                break;
                                                            case 270381:
                                                                handlePcoData((AsyncResult) message.obj);
                                                                return;
                                                            case 270382:
                                                                onSetCarrierDataEnabled((AsyncResult) message.obj);
                                                                return;
                                                            case 270383:
                                                                onDataReconnect(message.getData());
                                                                return;
                                                            case 270384:
                                                                break;
                                                            case 270385:
                                                                onDataServiceBindingChanged(((Boolean) ((AsyncResult) message.obj).result).booleanValue());
                                                                return;
                                                            default:
                                                                Rlog.e("DcTracker", "Unhandled event=" + message);
                                                                return;
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
        onRadioAvailable();
    }

    protected int getApnProfileID(String str) {
        if (TextUtils.equals(str, "ims")) {
            return 2;
        }
        if (TextUtils.equals(str, "fota")) {
            return 3;
        }
        if (TextUtils.equals(str, "cbs")) {
            return 4;
        }
        return (!TextUtils.equals(str, "ia") && TextUtils.equals(str, "dun")) ? 1 : 0;
    }

    protected int getCellLocationId() {
        CellLocation cellLocation = this.mPhone.getCellLocation();
        if (cellLocation != null) {
            if (cellLocation instanceof GsmCellLocation) {
                return ((GsmCellLocation) cellLocation).getCid();
            }
            if (cellLocation instanceof CdmaCellLocation) {
                return ((CdmaCellLocation) cellLocation).getBaseStationId();
            }
        }
        return -1;
    }

    protected IccRecords getUiccRecords(int i) {
        return this.mUiccController.getIccRecords(this.mPhone.getPhoneId(), i);
    }

    protected void onUpdateIcc() {
        IccRecords uiccRecords;
        IccRecords iccRecords;
        if (this.mUiccController != null && (iccRecords = this.mIccRecords.get()) != (uiccRecords = getUiccRecords(1))) {
            if (iccRecords != null) {
                log("Removing stale icc objects.");
                iccRecords.unregisterForRecordsLoaded(this);
                this.mIccRecords.set(null);
            }
            if (uiccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                    log("New records found.");
                    this.mIccRecords.set(uiccRecords);
                    uiccRecords.registerForRecordsLoaded(this, 270338, null);
                    return;
                }
                return;
            }
            onSimNotReady();
        }
    }

    public void update() {
        log("update sub = " + this.mPhone.getSubId());
        log("update(): Active DDS, register for all events now!");
        onUpdateIcc();
        this.mAutoAttachOnCreation.set(false);
        ((GsmCdmaPhone) this.mPhone).updateCurrentCarrierInProvider();
    }

    public void cleanUpAllConnections(String str) {
        cleanUpAllConnections(str, (Message) null);
    }

    public void updateRecords() {
        onUpdateIcc();
    }

    public void cleanUpAllConnections(String str, Message message) {
        log("cleanUpAllConnections");
        if (message != null) {
            this.mDisconnectAllCompleteMsgList.add(message);
        }
        Message messageObtainMessage = obtainMessage(270365);
        messageObtainMessage.obj = str;
        sendMessage(messageObtainMessage);
    }

    protected void notifyDataDisconnectComplete() {
        log("notifyDataDisconnectComplete");
        Iterator<Message> it = this.mDisconnectAllCompleteMsgList.iterator();
        while (it.hasNext()) {
            it.next().sendToTarget();
        }
        this.mDisconnectAllCompleteMsgList.clear();
    }

    protected void notifyAllDataDisconnected() {
        sEnableFailFastRefCounter = 0;
        this.mFailFast = false;
        this.mAllDataDisconnectedRegistrants.notifyRegistrants();
    }

    public void registerForAllDataDisconnected(Handler handler, int i, Object obj) {
        this.mAllDataDisconnectedRegistrants.addUnique(handler, i, obj);
        if (isDisconnected()) {
            log("notify All Data Disconnected");
            notifyAllDataDisconnected();
        }
    }

    public void unregisterForAllDataDisconnected(Handler handler) {
        this.mAllDataDisconnectedRegistrants.remove(handler);
    }

    public void registerForDataEnabledChanged(Handler handler, int i, Object obj) {
        this.mDataEnabledSettings.registerForDataEnabledChanged(handler, i, obj);
    }

    public void unregisterForDataEnabledChanged(Handler handler) {
        this.mDataEnabledSettings.unregisterForDataEnabledChanged(handler);
    }

    private void onSetInternalDataEnabled(boolean z, Message message) {
        boolean z2;
        log("onSetInternalDataEnabled: enabled=" + z);
        this.mDataEnabledSettings.setInternalDataEnabled(z);
        if (z) {
            log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
            onTrySetupData(PhoneInternalInterface.REASON_DATA_ENABLED);
            z2 = true;
        } else {
            z2 = false;
            log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
            cleanUpAllConnections(PhoneInternalInterface.REASON_DATA_DISABLED, message);
        }
        if (z2 && message != null) {
            message.sendToTarget();
        }
    }

    public boolean setInternalDataEnabled(boolean z) {
        return setInternalDataEnabled(z, null);
    }

    public boolean setInternalDataEnabled(boolean z, Message message) {
        log("setInternalDataEnabled(" + z + ")");
        Message messageObtainMessage = obtainMessage(270363, message);
        messageObtainMessage.arg1 = z ? 1 : 0;
        sendMessage(messageObtainMessage);
        return true;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("DcTracker:");
        printWriter.println(" RADIO_TESTS=false");
        printWriter.println(" mDataEnabledSettings=" + this.mDataEnabledSettings);
        printWriter.println(" isDataAllowed=" + isDataAllowed(null));
        printWriter.flush();
        printWriter.println(" mRequestedApnType=" + this.mRequestedApnType);
        printWriter.println(" mPhone=" + this.mPhone.getPhoneName());
        printWriter.println(" mActivity=" + this.mActivity);
        printWriter.println(" mState=" + this.mState);
        printWriter.println(" mTxPkts=" + this.mTxPkts);
        printWriter.println(" mRxPkts=" + this.mRxPkts);
        printWriter.println(" mNetStatPollPeriod=" + this.mNetStatPollPeriod);
        printWriter.println(" mNetStatPollEnabled=" + this.mNetStatPollEnabled);
        printWriter.println(" mDataStallTxRxSum=" + this.mDataStallTxRxSum);
        printWriter.println(" mDataStallAlarmTag=" + this.mDataStallAlarmTag);
        printWriter.println(" mDataStallDetectionEnabled=" + this.mDataStallDetectionEnabled);
        printWriter.println(" mSentSinceLastRecv=" + this.mSentSinceLastRecv);
        printWriter.println(" mNoRecvPollCount=" + this.mNoRecvPollCount);
        printWriter.println(" mResolver=" + this.mResolver);
        printWriter.println(" mReconnectIntent=" + this.mReconnectIntent);
        printWriter.println(" mAutoAttachOnCreation=" + this.mAutoAttachOnCreation.get());
        printWriter.println(" mIsScreenOn=" + this.mIsScreenOn);
        printWriter.println(" mUniqueIdGenerator=" + this.mUniqueIdGenerator);
        printWriter.println(" mDataRoamingLeakageLog= ");
        this.mDataRoamingLeakageLog.dump(fileDescriptor, printWriter, strArr);
        printWriter.flush();
        printWriter.println(" ***************************************");
        DcController dcController = this.mDcc;
        if (dcController != null) {
            dcController.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println(" mDcc=null");
        }
        printWriter.println(" ***************************************");
        if (this.mDataConnections != null) {
            Set<Map.Entry<Integer, DataConnection>> setEntrySet = this.mDataConnections.entrySet();
            printWriter.println(" mDataConnections: count=" + setEntrySet.size());
            for (Map.Entry<Integer, DataConnection> entry : setEntrySet) {
                printWriter.printf(" *** mDataConnection[%d] \n", entry.getKey());
                entry.getValue().dump(fileDescriptor, printWriter, strArr);
            }
        } else {
            printWriter.println("mDataConnections=null");
        }
        printWriter.println(" ***************************************");
        printWriter.flush();
        HashMap<String, Integer> map = this.mApnToDataConnectionId;
        if (map != null) {
            Set<Map.Entry<String, Integer>> setEntrySet2 = map.entrySet();
            printWriter.println(" mApnToDataConnectonId size=" + setEntrySet2.size());
            for (Map.Entry<String, Integer> entry2 : setEntrySet2) {
                printWriter.printf(" mApnToDataConnectonId[%s]=%d\n", entry2.getKey(), entry2.getValue());
            }
        } else {
            printWriter.println("mApnToDataConnectionId=null");
        }
        printWriter.println(" ***************************************");
        printWriter.flush();
        ConcurrentHashMap<String, ApnContext> concurrentHashMap = this.mApnContexts;
        if (concurrentHashMap != null) {
            Set<Map.Entry<String, ApnContext>> setEntrySet3 = concurrentHashMap.entrySet();
            printWriter.println(" mApnContexts size=" + setEntrySet3.size());
            Iterator<Map.Entry<String, ApnContext>> it = setEntrySet3.iterator();
            while (it.hasNext()) {
                it.next().getValue().dump(fileDescriptor, printWriter, strArr);
            }
            printWriter.println(" ***************************************");
        } else {
            printWriter.println(" mApnContexts=null");
        }
        printWriter.flush();
        ArrayList<ApnSetting> arrayList = this.mAllApnSettings;
        if (arrayList != null) {
            printWriter.println(" mAllApnSettings size=" + arrayList.size());
            for (int i = 0; i < arrayList.size(); i++) {
                printWriter.printf(" mAllApnSettings[%d]: %s\n", Integer.valueOf(i), arrayList.get(i));
            }
            printWriter.flush();
        } else {
            printWriter.println(" mAllApnSettings=null");
        }
        printWriter.println(" mPreferredApn=" + this.mPreferredApn);
        printWriter.println(" mIsPsRestricted=" + this.mIsPsRestricted);
        printWriter.println(" mIsDisposed=" + this.mIsDisposed);
        printWriter.println(" mIntentReceiver=" + this.mIntentReceiver);
        printWriter.println(" mReregisterOnReconnectFailure=" + this.mReregisterOnReconnectFailure);
        printWriter.println(" canSetPreferApn=" + this.mCanSetPreferApn);
        printWriter.println(" mApnObserver=" + this.mApnObserver);
        printWriter.println(" getOverallState=" + getOverallState());
        printWriter.println(" mDataConnectionAsyncChannels=%s\n" + this.mDataConnectionAcHashMap);
        printWriter.println(" mAttached=" + this.mAttached.get());
        this.mDataEnabledSettings.dump(fileDescriptor, printWriter, strArr);
        printWriter.flush();
    }

    public String[] getPcscfAddress(String str) {
        ApnContext apnContext;
        log("getPcscfAddress()");
        if (str == null) {
            log("apnType is null, return null");
            return null;
        }
        if (TextUtils.equals(str, "emergency")) {
            apnContext = this.mApnContextsById.get(9);
        } else if (TextUtils.equals(str, "ims")) {
            apnContext = this.mApnContextsById.get(5);
        } else {
            log("apnType is invalid, return null");
            return null;
        }
        if (apnContext == null) {
            log("apnContext is null, return null");
            return null;
        }
        DcAsyncChannel dcAc = apnContext.getDcAc();
        if (dcAc == null) {
            return null;
        }
        String[] pcscfAddr = dcAc.getPcscfAddr();
        if (pcscfAddr != null) {
            for (int i = 0; i < pcscfAddr.length; i++) {
                log("Pcscf[" + i + "]: " + pcscfAddr[i]);
            }
        }
        return pcscfAddr;
    }

    private void initEmergencyApnSetting() {
        Cursor cursorQuery = this.mPhone.getContext().getContentResolver().query(Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "filtered"), null, "type=\"emergency\"", null, null);
        if (cursorQuery != null) {
            if (cursorQuery.getCount() > 0 && cursorQuery.moveToFirst()) {
                this.mEmergencyApn = makeApnSetting(cursorQuery);
            }
            cursorQuery.close();
        }
    }

    protected void addEmergencyApnSetting() {
        if (this.mEmergencyApn != null) {
            synchronized (this.mRefCountLock) {
                if (this.mAllApnSettings == null) {
                    this.mAllApnSettings = new ArrayList<>();
                } else {
                    boolean z = false;
                    Iterator<ApnSetting> it = this.mAllApnSettings.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        } else if (ArrayUtils.contains(it.next().types, "emergency")) {
                            z = true;
                            break;
                        }
                    }
                    if (!z) {
                        this.mAllApnSettings.add(this.mEmergencyApn);
                    } else {
                        log("addEmergencyApnSetting - E-APN setting is already present");
                    }
                }
            }
        }
    }

    private boolean containsAllApns(ArrayList<ApnSetting> arrayList, ArrayList<ApnSetting> arrayList2) {
        boolean z;
        Iterator<ApnSetting> it = arrayList2.iterator();
        do {
            z = true;
            if (!it.hasNext()) {
                return true;
            }
            ApnSetting next = it.next();
            Iterator<ApnSetting> it2 = arrayList.iterator();
            while (true) {
                if (it2.hasNext()) {
                    if (it2.next().equals(next, this.mPhone.getServiceState().getDataRoamingFromRegistration())) {
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
        } while (z);
        return false;
    }

    protected void cleanUpConnectionsOnUpdatedApns(boolean z, String str) {
        log("cleanUpConnectionsOnUpdatedApns: tearDown=" + z);
        if (this.mAllApnSettings != null && this.mAllApnSettings.isEmpty()) {
            cleanUpAllConnections(z, PhoneInternalInterface.REASON_APN_CHANGED);
        } else {
            if (this.mPhone.getServiceState().getRilDataRadioTechnology() == 0) {
                return;
            }
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (!mtkSkipCleanUpConnectionsOnUpdatedApns(apnContext)) {
                    ArrayList<ApnSetting> waitingApns = apnContext.getWaitingApns();
                    ArrayList<ApnSetting> arrayListBuildWaitingApns = buildWaitingApns(apnContext.getApnType(), this.mPhone.getServiceState().getRilDataRadioTechnology());
                    if (waitingApns != null && (arrayListBuildWaitingApns.size() != waitingApns.size() || !containsAllApns(waitingApns, arrayListBuildWaitingApns))) {
                        apnContext.setWaitingApns(arrayListBuildWaitingApns);
                        if (!apnContext.isDisconnected()) {
                            apnContext.setReason(str);
                            cleanUpConnection(true, apnContext);
                        }
                    }
                }
            }
        }
        if (!isConnected()) {
            stopNetStatPoll();
            stopDataStallAlarm();
        }
        this.mRequestedApnType = "default";
        log("mDisconnectPendingCount = " + this.mDisconnectPendingCount);
        if (z && this.mDisconnectPendingCount == 0) {
            notifyDataDisconnectComplete();
            notifyAllDataDisconnected();
        }
    }

    protected void resetPollStats() {
        this.mTxPkts = -1L;
        this.mRxPkts = -1L;
        this.mNetStatPollPeriod = 1000;
    }

    protected void startNetStatPoll() {
        if (getOverallState() == DctConstants.State.CONNECTED && !this.mNetStatPollEnabled) {
            log("startNetStatPoll");
            resetPollStats();
            this.mNetStatPollEnabled = true;
            this.mPollNetStat.run();
        }
        if (this.mPhone != null) {
            this.mPhone.notifyDataActivity();
        }
    }

    protected void stopNetStatPoll() {
        this.mNetStatPollEnabled = false;
        removeCallbacks(this.mPollNetStat);
        log("stopNetStatPoll");
        if (this.mPhone != null) {
            this.mPhone.notifyDataActivity();
        }
    }

    public void sendStartNetStatPoll(DctConstants.Activity activity) {
        Message messageObtainMessage = obtainMessage(270376);
        messageObtainMessage.arg1 = 1;
        messageObtainMessage.obj = activity;
        sendMessage(messageObtainMessage);
    }

    private void handleStartNetStatPoll(DctConstants.Activity activity) {
        startNetStatPoll();
        startDataStallAlarm(false);
        setActivity(activity);
    }

    public void sendStopNetStatPoll(DctConstants.Activity activity) {
        Message messageObtainMessage = obtainMessage(270376);
        messageObtainMessage.arg1 = 0;
        messageObtainMessage.obj = activity;
        sendMessage(messageObtainMessage);
    }

    private void handleStopNetStatPoll(DctConstants.Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    protected void updateDataActivity() {
        DctConstants.Activity activity;
        TxRxSum txRxSum = new TxRxSum(this.mTxPkts, this.mRxPkts);
        TxRxSum txRxSum2 = new TxRxSum();
        txRxSum2.updateTxRxSum();
        this.mTxPkts = txRxSum2.txPkts;
        this.mRxPkts = txRxSum2.rxPkts;
        if (this.mNetStatPollEnabled) {
            if (txRxSum.txPkts > 0 || txRxSum.rxPkts > 0) {
                long j = this.mTxPkts - txRxSum.txPkts;
                long j2 = this.mRxPkts - txRxSum.rxPkts;
                if (j > 0 && j2 > 0) {
                    activity = DctConstants.Activity.DATAINANDOUT;
                } else if (j > 0 && j2 == 0) {
                    activity = DctConstants.Activity.DATAOUT;
                } else if (j == 0 && j2 > 0) {
                    activity = DctConstants.Activity.DATAIN;
                } else {
                    activity = this.mActivity == DctConstants.Activity.DORMANT ? this.mActivity : DctConstants.Activity.NONE;
                }
                if (this.mActivity != activity && this.mIsScreenOn) {
                    this.mActivity = activity;
                    this.mPhone.notifyDataActivity();
                }
            }
        }
    }

    protected void handlePcoData(AsyncResult asyncResult) {
        if (asyncResult.exception != null) {
            Rlog.e(LOG_TAG, "PCO_DATA exception: " + asyncResult.exception);
            return;
        }
        PcoData pcoData = (PcoData) asyncResult.result;
        ArrayList<DataConnection> arrayList = new ArrayList();
        DataConnection activeDcByCid = this.mDcc.getActiveDcByCid(pcoData.cid);
        if (activeDcByCid != null) {
            arrayList.add(activeDcByCid);
        }
        if (arrayList.size() == 0) {
            Rlog.e(LOG_TAG, "PCO_DATA for unknown cid: " + pcoData.cid + ", inferring");
            Iterator<DataConnection> it = this.mDataConnections.values().iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                DataConnection next = it.next();
                int cid = next.getCid();
                if (cid == pcoData.cid) {
                    arrayList.clear();
                    arrayList.add(next);
                    break;
                } else if (cid == -1) {
                    Iterator<ApnContext> it2 = next.mApnContexts.keySet().iterator();
                    while (true) {
                        if (it2.hasNext()) {
                            if (it2.next().getState() == DctConstants.State.CONNECTING) {
                                arrayList.add(next);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        if (arrayList.size() == 0) {
            Rlog.e(LOG_TAG, "PCO_DATA - couldn't infer cid");
            return;
        }
        for (DataConnection dataConnection : arrayList) {
            if (dataConnection.mApnContexts.size() != 0) {
                Iterator<ApnContext> it3 = dataConnection.mApnContexts.keySet().iterator();
                while (it3.hasNext()) {
                    String apnType = it3.next().getApnType();
                    Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE");
                    intent.putExtra("apnType", apnType);
                    intent.putExtra("apnProto", pcoData.bearerProto);
                    intent.putExtra("pcoId", pcoData.pcoId);
                    intent.putExtra("pcoValue", pcoData.contents);
                    this.mPhone.getCarrierSignalAgent().notifyCarrierSignalReceivers(intent);
                }
            } else {
                return;
            }
        }
    }

    protected static class RecoveryAction {
        public static final int CLEANUP = 1;
        public static final int GET_DATA_CALL_LIST = 0;
        public static final int RADIO_RESTART = 3;
        public static final int REREGISTER = 2;

        protected RecoveryAction() {
        }

        private static boolean isAggressiveRecovery(int i) {
            return i == 1 || i == 2 || i == 3;
        }
    }

    protected int getRecoveryAction() {
        return Settings.System.getInt(this.mResolver, "radio.data.stall.recovery.action", 0);
    }

    protected void putRecoveryAction(int i) {
        Settings.System.putInt(this.mResolver, "radio.data.stall.recovery.action", i);
    }

    protected void broadcastDataStallDetected(int i) {
        Intent intent = new Intent("android.intent.action.DATA_STALL_DETECTED");
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        intent.putExtra("recoveryAction", i);
        this.mPhone.getContext().sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
    }

    protected void doRecovery() {
        if (getOverallState() == DctConstants.State.CONNECTED) {
            int recoveryAction = getRecoveryAction();
            TelephonyMetrics.getInstance().writeDataStallEvent(this.mPhone.getPhoneId(), recoveryAction);
            broadcastDataStallDetected(recoveryAction);
            switch (recoveryAction) {
                case 0:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST, this.mSentSinceLastRecv);
                    log("doRecovery() get data call list");
                    this.mDataServiceManager.getDataCallList(obtainMessage());
                    putRecoveryAction(1);
                    break;
                case 1:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP, this.mSentSinceLastRecv);
                    log("doRecovery() cleanup all connections");
                    cleanUpAllConnections(PhoneInternalInterface.REASON_PDP_RESET);
                    putRecoveryAction(2);
                    break;
                case 2:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_REREGISTER, this.mSentSinceLastRecv);
                    log("doRecovery() re-register");
                    this.mPhone.getServiceStateTracker().reRegisterNetwork(null);
                    putRecoveryAction(3);
                    break;
                case 3:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART, this.mSentSinceLastRecv);
                    log("restarting radio");
                    restartRadio();
                    putRecoveryAction(0);
                    break;
                default:
                    throw new RuntimeException("doRecovery: Invalid recoveryAction=" + recoveryAction);
            }
            this.mSentSinceLastRecv = 0L;
        }
    }

    protected void updateDataStallInfo() {
        TxRxSum txRxSum = new TxRxSum(this.mDataStallTxRxSum);
        this.mDataStallTxRxSum.updateTxRxSum();
        long j = this.mDataStallTxRxSum.txPkts - txRxSum.txPkts;
        long j2 = this.mDataStallTxRxSum.rxPkts - txRxSum.rxPkts;
        if (j > 0 && j2 > 0) {
            this.mSentSinceLastRecv = 0L;
            putRecoveryAction(0);
            return;
        }
        if (j > 0 && j2 == 0) {
            if (isPhoneStateIdle()) {
                this.mSentSinceLastRecv += j;
            } else {
                this.mSentSinceLastRecv = 0L;
            }
            log("updateDataStallInfo: OUT sent=" + j + " mSentSinceLastRecv=" + this.mSentSinceLastRecv);
            return;
        }
        if (j == 0 && j2 > 0) {
            this.mSentSinceLastRecv = 0L;
            putRecoveryAction(0);
        }
    }

    protected boolean isPhoneStateIdle() {
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null && phone.getState() != PhoneConstants.State.IDLE) {
                log("isPhoneStateIdle false: Voice call active on phone " + i);
                return false;
            }
        }
        return true;
    }

    protected void onDataStallAlarm(int i) {
        if (this.mDataStallAlarmTag != i) {
            log("onDataStallAlarm: ignore, tag=" + i + " expecting " + this.mDataStallAlarmTag);
            return;
        }
        updateDataStallInfo();
        boolean z = false;
        if (this.mSentSinceLastRecv >= Settings.Global.getInt(this.mResolver, "pdp_watchdog_trigger_packet_count", 10)) {
            log("onDataStallAlarm: tag=" + i + " do recovery action=" + getRecoveryAction());
            z = true;
            sendMessage(obtainMessage(270354));
        }
        startDataStallAlarm(z);
    }

    protected void startDataStallAlarm(boolean z) {
        int i;
        int recoveryAction = getRecoveryAction();
        if (this.mDataStallDetectionEnabled && getOverallState() == DctConstants.State.CONNECTED) {
            if (this.mIsScreenOn || z || RecoveryAction.isAggressiveRecovery(recoveryAction)) {
                i = Settings.Global.getInt(this.mResolver, "data_stall_alarm_aggressive_delay_in_ms", 60000);
            } else {
                i = Settings.Global.getInt(this.mResolver, "data_stall_alarm_non_aggressive_delay_in_ms", DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            }
            this.mDataStallAlarmTag++;
            Intent intent = new Intent(INTENT_DATA_STALL_ALARM);
            intent.putExtra(DATA_STALL_ALARM_TAG_EXTRA, this.mDataStallAlarmTag);
            this.mDataStallAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), this.mPhone.getPhoneId() + 1, intent, 201326592);
            this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + ((long) i), this.mDataStallAlarmIntent);
        }
    }

    protected void stopDataStallAlarm() {
        this.mDataStallAlarmTag++;
        if (this.mDataStallAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mDataStallAlarmIntent);
            this.mDataStallAlarmIntent = null;
        }
    }

    private void restartDataStallAlarm() {
        if (isConnected()) {
            if (RecoveryAction.isAggressiveRecovery(getRecoveryAction())) {
                log("restartDataStallAlarm: action is pending. not resetting the alarm.");
            } else {
                stopDataStallAlarm();
                startDataStallAlarm(false);
            }
        }
    }

    private void onActionIntentProvisioningApnAlarm(Intent intent) {
        log("onActionIntentProvisioningApnAlarm: action=" + intent.getAction());
        Message messageObtainMessage = obtainMessage(270375, intent.getAction());
        messageObtainMessage.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(messageObtainMessage);
    }

    private void startProvisioningApnAlarm() {
        int i = Settings.Global.getInt(this.mResolver, "provisioning_apn_alarm_delay_in_ms", PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT);
        if (Build.IS_DEBUGGABLE) {
            try {
                i = Integer.parseInt(System.getProperty(DEBUG_PROV_APN_ALARM, Integer.toString(i)));
            } catch (NumberFormatException e) {
                loge("startProvisioningApnAlarm: e=" + e);
            }
        }
        this.mProvisioningApnAlarmTag++;
        log("startProvisioningApnAlarm: tag=" + this.mProvisioningApnAlarmTag + " delay=" + (i / 1000) + "s");
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, this.mProvisioningApnAlarmTag);
        this.mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), this.mPhone.getPhoneId() + 1, intent, 201326592);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) i), this.mProvisioningApnAlarmIntent);
    }

    private void stopProvisioningApnAlarm() {
        log("stopProvisioningApnAlarm: current tag=" + this.mProvisioningApnAlarmTag + " mProvsioningApnAlarmIntent=" + this.mProvisioningApnAlarmIntent);
        this.mProvisioningApnAlarmTag = this.mProvisioningApnAlarmTag + 1;
        if (this.mProvisioningApnAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mProvisioningApnAlarmIntent);
            this.mProvisioningApnAlarmIntent = null;
        }
    }

    protected static DataProfile createDataProfile(ApnSetting apnSetting) {
        return createDataProfile(apnSetting, apnSetting.profileId);
    }

    @VisibleForTesting
    public static DataProfile createDataProfile(ApnSetting apnSetting, int i) {
        int i2;
        int iConvertNetworkTypeBitmaskToBearerBitmask = ServiceState.convertNetworkTypeBitmaskToBearerBitmask(apnSetting.networkTypeBitmask);
        if (iConvertNetworkTypeBitmaskToBearerBitmask == 0) {
            i2 = 0;
        } else if (ServiceState.bearerBitmapHasCdma(iConvertNetworkTypeBitmaskToBearerBitmask)) {
            i2 = 2;
        } else {
            i2 = 1;
        }
        return new DataProfile(i, apnSetting.apn, apnSetting.protocol, apnSetting.authType, apnSetting.user, apnSetting.password, i2, apnSetting.maxConnsTime, apnSetting.maxConns, apnSetting.waitTime, apnSetting.carrierEnabled, apnSetting.typesBitmap, apnSetting.roamingProtocol, iConvertNetworkTypeBitmaskToBearerBitmask, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.modemCognitive);
    }

    protected void onDataServiceBindingChanged(boolean z) {
        if (z) {
            this.mDcc.start();
        } else {
            this.mDcc.dispose();
        }
    }

    protected void mtkCopyHandlerThread(HandlerThread handlerThread) {
    }

    protected long mtkModifyInterApnDelay(long j, String str) {
        return j;
    }

    protected boolean mtkIsApplyNewStateOnDisable(boolean z) {
        return false;
    }

    protected boolean mtkApplyNewStateOnDisable(boolean z, ApnContext apnContext) {
        return z;
    }

    protected boolean mtkSkipTearDownAll() {
        return false;
    }

    protected void mtkAddVsimApnTypeToDefaultApnSetting() {
    }

    protected void mtkAddPreemptApnTypeToDefaultApnSetting() {
    }

    protected boolean mtkSkipNotifyOnRadioOffOrNotAvailable() {
        return false;
    }

    protected boolean mtkIsNeedNotify(ApnContext apnContext) {
        return true;
    }

    protected boolean mtkSkipImsOrEmergencyPdn(ApnContext apnContext) {
        return false;
    }

    protected String[] mtkGetDunApnByMccMnc(Context context, String[] strArr) {
        return strArr;
    }

    protected boolean mtkIgnoredPermanentFailure(DcFailCause dcFailCause) {
        return false;
    }

    protected boolean mtkSkipSetDataProfileAsNeeded(String str) {
        return false;
    }

    protected boolean mtkSkipCleanUpConnectionsOnUpdatedApns(ApnContext apnContext) {
        return false;
    }

    protected ArrayList<ApnSetting> mtkAddMnoApnsIntoAllApnList(ArrayList<ApnSetting> arrayList, ArrayList<ApnSetting> arrayList2) {
        return arrayList;
    }

    protected boolean mtkIsNeedRegisterSettingsObserver(int i, int i2) {
        return true;
    }

    protected String mtkGetOperatorNumeric(IccRecords iccRecords) {
        return iccRecords != null ? iccRecords.getOperatorNumeric() : "";
    }

    protected boolean mtkMvnoMatches(String str, String str2) {
        return false;
    }
}
