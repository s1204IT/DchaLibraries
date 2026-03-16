package com.android.internal.telephony.dataconnection;

import android.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.EventLog;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.EventLogTags;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneBase;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncChannel;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class DcTrackerBase extends Handler {
    protected static final int APN_DELAY_DEFAULT_MILLIS = 20000;
    protected static final int APN_FAIL_FAST_DELAY_DEFAULT_MILLIS = 3000;
    protected static final String APN_RESTORE_DELAY_PROP_NAME = "android.telephony.apn-restore";
    protected static final int DATA_CONNECTION_ACTIVE_PH_LINK_DOWN = 1;
    protected static final int DATA_CONNECTION_ACTIVE_PH_LINK_INACTIVE = 0;
    protected static final int DATA_CONNECTION_ACTIVE_PH_LINK_UP = 2;
    protected static final int DATA_STALL_ALARM_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 60000;
    protected static final int DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT = 360000;
    protected static final String DATA_STALL_ALARM_TAG_EXTRA = "data.stall.alram.tag";
    protected static final boolean DATA_STALL_NOT_SUSPECTED = false;
    protected static final int DATA_STALL_NO_RECV_POLL_LIMIT = 1;
    protected static final boolean DATA_STALL_SUSPECTED = true;
    protected static final boolean DBG = true;
    protected static final String DEBUG_PROV_APN_ALARM = "persist.debug.prov_apn_alarm";
    protected static final String DEFALUT_DATA_ON_BOOT_PROP = "net.def_data_on_boot";
    protected static final String DEFAULT_DATA_RETRY_CONFIG = "default_randomization=2000,5000,10000,20000,40000,80000:5000,160000:5000,320000:5000,640000:5000,1280000:5000,1800000:5000";
    protected static final int DEFAULT_MAX_PDP_RESET_FAIL = 3;
    private static final int DEFAULT_MDC_INITIAL_RETRY = 1;
    protected static final String INTENT_DATA_STALL_ALARM = "com.android.internal.telephony.data-stall";
    protected static final String INTENT_PROVISIONING_APN_ALARM = "com.android.internal.telephony.provisioning_apn_alarm";
    protected static final String INTENT_RECONNECT_ALARM = "com.android.internal.telephony.data-reconnect";
    protected static final String INTENT_RECONNECT_ALARM_EXTRA_REASON = "reconnect_alarm_extra_reason";
    protected static final String INTENT_RECONNECT_ALARM_EXTRA_TYPE = "reconnect_alarm_extra_type";
    protected static final String INTENT_RESTART_TRYSETUP_ALARM = "com.android.internal.telephony.data-restart-trysetup";
    protected static final String INTENT_RESTART_TRYSETUP_ALARM_EXTRA_TYPE = "restart_trysetup_alarm_extra_type";
    protected static final int NO_RECV_POLL_LIMIT = 24;
    protected static final String NULL_IP = "0.0.0.0";
    protected static final int NUMBER_SENT_PACKETS_OF_HANG = 10;
    protected static final int POLL_LONGEST_RTT = 120000;
    protected static final int POLL_NETSTAT_MILLIS = 1000;
    protected static final int POLL_NETSTAT_SCREEN_OFF_MILLIS = 600000;
    protected static final int POLL_NETSTAT_SLOW_MILLIS = 5000;
    protected static final int PROVISIONING_APN_ALARM_DELAY_IN_MS_DEFAULT = 900000;
    protected static final String PROVISIONING_APN_ALARM_TAG_EXTRA = "provisioning.apn.alarm.tag";
    protected static final boolean RADIO_TESTS = false;
    protected static final int RESTORE_DEFAULT_APN_DELAY = 60000;
    protected static final String SECONDARY_DATA_RETRY_CONFIG = "max_retries=3, 5000, 5000, 5000";
    protected static final boolean VDBG = false;
    protected static final boolean VDBG_STALL = true;
    protected ApnSetting mActiveApn;
    AlarmManager mAlarmManager;
    protected boolean mAutoAttachOnCreation;
    protected int mCidActive;
    ConnectivityManager mCm;
    private DataRoamingSettingObserver mDataRoamingSettingObserver;
    protected DcTesterFailBringUpAll mDcTesterFailBringUpAll;
    protected DcController mDcc;
    protected int mNetStatPollPeriod;
    protected PhoneBase mPhone;
    protected ContentResolver mResolver;
    protected long mRxPkts;
    protected long mSentSinceLastRecv;
    private SubscriptionManager mSubscriptionManager;
    protected long mTxPkts;
    protected UiccController mUiccController;
    protected boolean mUserDataEnabled;
    static boolean mIsCleanupRequired = false;
    protected static boolean sPolicyDataEnabled = true;
    protected static int sEnableFailFastRefCounter = 0;
    protected Object mDataEnabledLock = new Object();
    protected boolean mInternalDataEnabled = true;
    private boolean[] mDataEnabled = new boolean[10];
    private int mEnabledCount = 0;
    protected String mRequestedApnType = "default";
    protected String RADIO_RESET_PROPERTY = "gsm.radioreset";
    protected AtomicReference<IccRecords> mIccRecords = new AtomicReference<>();
    protected DctConstants.Activity mActivity = DctConstants.Activity.NONE;
    protected DctConstants.State mState = DctConstants.State.IDLE;
    protected Handler mDataConnectionTracker = null;
    protected boolean mNetStatPollEnabled = false;
    protected TxRxSum mDataStallTxRxSum = new TxRxSum(0, 0);
    protected int mDataStallAlarmTag = (int) SystemClock.elapsedRealtime();
    protected PendingIntent mDataStallAlarmIntent = null;
    protected int mNoRecvPollCount = 0;
    protected volatile boolean mDataStallDetectionEnabled = true;
    protected volatile boolean mFailFast = false;
    protected boolean mInVoiceCall = false;
    protected boolean mIsWifiConnected = false;
    protected PendingIntent mReconnectIntent = null;
    protected boolean mAutoAttachOnCreationConfig = false;
    protected boolean mIsScreenOn = true;
    protected AtomicInteger mUniqueIdGenerator = new AtomicInteger(0);
    protected HashMap<Integer, DataConnection> mDataConnections = new HashMap<>();
    protected HashMap<Integer, DcAsyncChannel> mDataConnectionAcHashMap = new HashMap<>();
    protected HashMap<String, Integer> mApnToDataConnectionId = new HashMap<>();
    protected final ConcurrentHashMap<String, ApnContext> mApnContexts = new ConcurrentHashMap<>();
    protected final PriorityQueue<ApnContext> mPrioritySortedApnContexts = new PriorityQueue<>(5, new Comparator<ApnContext>() {
        @Override
        public int compare(ApnContext c1, ApnContext c2) {
            return c2.priority - c1.priority;
        }
    });
    protected ArrayList<ApnSetting> mAllApnSettings = null;
    protected ApnSetting mPreferredApn = null;
    protected boolean mIsPsRestricted = false;
    protected ApnSetting mEmergencyApn = null;
    protected boolean mIsDisposed = false;
    protected boolean mIsProvisioning = false;
    protected String mProvisioningUrl = null;
    protected PendingIntent mProvisioningApnAlarmIntent = null;
    protected int mProvisioningApnAlarmTag = (int) SystemClock.elapsedRealtime();
    protected AsyncChannel mReplyAc = new AsyncChannel();
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            DcTrackerBase.this.log("onReceive: action=" + action);
            if (action.equals("android.intent.action.SCREEN_ON")) {
                DcTrackerBase.this.mIsScreenOn = true;
                DcTrackerBase.this.stopNetStatPoll();
                DcTrackerBase.this.startNetStatPoll();
                DcTrackerBase.this.restartDataStallAlarm();
                return;
            }
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                DcTrackerBase.this.mIsScreenOn = false;
                DcTrackerBase.this.stopNetStatPoll();
                DcTrackerBase.this.startNetStatPoll();
                DcTrackerBase.this.restartDataStallAlarm();
                return;
            }
            if (action.startsWith(DcTrackerBase.INTENT_RECONNECT_ALARM)) {
                DcTrackerBase.this.log("Reconnect alarm. Previous state was " + DcTrackerBase.this.mState);
                DcTrackerBase.this.onActionIntentReconnectAlarm(intent);
                return;
            }
            if (action.startsWith(DcTrackerBase.INTENT_RESTART_TRYSETUP_ALARM)) {
                DcTrackerBase.this.log("Restart trySetup alarm");
                DcTrackerBase.this.onActionIntentRestartTrySetupAlarm(intent);
                return;
            }
            if (action.equals(DcTrackerBase.INTENT_DATA_STALL_ALARM)) {
                DcTrackerBase.this.onActionIntentDataStallAlarm(intent);
                return;
            }
            if (action.equals(DcTrackerBase.INTENT_PROVISIONING_APN_ALARM)) {
                DcTrackerBase.this.onActionIntentProvisioningApnAlarm(intent);
                return;
            }
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                DcTrackerBase.this.mIsWifiConnected = networkInfo != null && networkInfo.isConnected();
                DcTrackerBase.this.log("NETWORK_STATE_CHANGED_ACTION: mIsWifiConnected=" + DcTrackerBase.this.mIsWifiConnected);
            } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                boolean enabled = intent.getIntExtra("wifi_state", 4) == 3;
                if (!enabled) {
                    DcTrackerBase.this.mIsWifiConnected = false;
                }
                DcTrackerBase.this.log("WIFI_STATE_CHANGED_ACTION: enabled=" + enabled + " mIsWifiConnected=" + DcTrackerBase.this.mIsWifiConnected);
            }
        }
    };
    private Runnable mPollNetStat = new Runnable() {
        @Override
        public void run() {
            DcTrackerBase.this.updateDataActivity();
            if (DcTrackerBase.this.mIsScreenOn) {
                DcTrackerBase.this.mNetStatPollPeriod = Settings.Global.getInt(DcTrackerBase.this.mResolver, "pdp_watchdog_poll_interval_ms", 1000);
            } else {
                DcTrackerBase.this.mNetStatPollPeriod = Settings.Global.getInt(DcTrackerBase.this.mResolver, "pdp_watchdog_long_poll_interval_ms", DcTrackerBase.POLL_NETSTAT_SCREEN_OFF_MILLIS);
            }
            if (DcTrackerBase.this.mNetStatPollEnabled) {
                DcTrackerBase.this.mDataConnectionTracker.postDelayed(this, DcTrackerBase.this.mNetStatPollPeriod);
            }
        }
    };
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            DcTrackerBase.this.log("SubscriptionListener.onSubscriptionInfoChanged");
            int subId = DcTrackerBase.this.mPhone.getSubId();
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                if (DcTrackerBase.this.mDataRoamingSettingObserver != null) {
                    DcTrackerBase.this.mDataRoamingSettingObserver.unregister();
                }
                DcTrackerBase.this.mDataRoamingSettingObserver = DcTrackerBase.this.new DataRoamingSettingObserver(DcTrackerBase.this.mPhone, DcTrackerBase.this.mPhone.getContext());
                DcTrackerBase.this.mDataRoamingSettingObserver.register();
            }
        }
    };

    protected abstract void completeConnection(ApnContext apnContext);

    protected abstract DctConstants.State getOverallState();

    public abstract String[] getPcscfAddress(String str);

    public abstract DctConstants.State getState(String str);

    protected abstract void gotoIdleAndNotifyDataConnection(String str);

    protected abstract boolean isApnTypeAvailable(String str);

    protected abstract boolean isDataAllowed();

    public abstract boolean isDataPossible(String str);

    public abstract boolean isDisconnected();

    protected abstract boolean isPermanentFail(DcFailCause dcFailCause);

    protected abstract boolean isProvisioningApn(String str);

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract boolean mvnoMatches(IccRecords iccRecords, String str, String str2);

    protected abstract void onCleanUpAllConnections(String str);

    protected abstract void onCleanUpAllConnectionsExceptIms(String str);

    protected abstract void onCleanUpAllConnectionsWithException(String str, ArrayList<String> arrayList);

    protected abstract void onCleanUpConnection(boolean z, int i, String str);

    protected abstract void onDataSetupComplete(AsyncResult asyncResult);

    protected abstract void onDataSetupCompleteError(AsyncResult asyncResult);

    protected abstract void onDisconnectDcRetrying(int i, AsyncResult asyncResult);

    protected abstract void onDisconnectDone(int i, AsyncResult asyncResult);

    protected abstract void onRadioAvailable();

    protected abstract void onRadioOffOrNotAvailable();

    protected abstract void onRoamingOff();

    protected abstract void onRoamingOn();

    protected abstract boolean onTrySetupData(String str);

    protected abstract void onUpdateIcc();

    protected abstract void onVoiceCallEnded();

    protected abstract void onVoiceCallStarted();

    protected abstract void restartRadio();

    public abstract void setDataAllowed(boolean z, Message message);

    public abstract void setImsRegistrationState(boolean z);

    protected abstract void setState(DctConstants.State state);

    private class DataRoamingSettingObserver extends ContentObserver {
        public DataRoamingSettingObserver(Handler handler, Context context) {
            super(handler);
            DcTrackerBase.this.mResolver = context.getContentResolver();
        }

        public void register() {
            String contentUri;
            if (TelephonyManager.getDefault().getSimCount() == 1) {
                contentUri = "data_roaming";
            } else {
                contentUri = "data_roaming" + DcTrackerBase.this.mPhone.getSubId();
            }
            DcTrackerBase.this.mResolver.registerContentObserver(Settings.Global.getUriFor(contentUri), false, this);
        }

        public void unregister() {
            DcTrackerBase.this.mResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DcTrackerBase.this.mPhone.getServiceState().getDataRoaming()) {
                DcTrackerBase.this.sendMessage(DcTrackerBase.this.obtainMessage(270347));
            }
        }
    }

    protected int getInitialMaxRetry() {
        if (this.mFailFast) {
            return 0;
        }
        int value = SystemProperties.getInt("mdc_initial_max_retry", 1);
        return Settings.Global.getInt(this.mResolver, "mdc_initial_max_retry", value);
    }

    public class TxRxSum {
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
            this.txPkts = TrafficStats.getMobileTcpTxPackets();
            this.rxPkts = TrafficStats.getMobileTcpRxPackets();
        }
    }

    protected void onActionIntentReconnectAlarm(Intent intent) {
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
        log("onActionIntentReconnectAlarm: mState=" + this.mState + " reason=" + reason + " apnType=" + apnType + " apnContext=" + apnContext + " mDataConnectionAsyncChannels=" + this.mDataConnectionAcHashMap);
        if (apnContext != null && apnContext.isEnabled()) {
            apnContext.setReason(reason);
            DctConstants.State apnContextState = apnContext.getState();
            log("onActionIntentReconnectAlarm: apnContext state=" + apnContextState);
            if (apnContextState == DctConstants.State.FAILED || apnContextState == DctConstants.State.IDLE) {
                log("onActionIntentReconnectAlarm: state is FAILED|IDLE, disassociate");
                DcAsyncChannel dcac = apnContext.getDcAc();
                if (dcac != null) {
                    log("onActionIntentReconnectAlarm: tearDown apnContext=" + apnContext);
                    dcac.tearDown(apnContext, "", null);
                }
                apnContext.setDataConnectionAc(null);
                apnContext.setState(DctConstants.State.IDLE);
            } else {
                log("onActionIntentReconnectAlarm: keep associated");
            }
            sendMessage(obtainMessage(270339, apnContext));
            apnContext.setReconnectIntent(null);
        }
    }

    protected void onActionIntentRestartTrySetupAlarm(Intent intent) {
        String apnType = intent.getStringExtra(INTENT_RESTART_TRYSETUP_ALARM_EXTRA_TYPE);
        ApnContext apnContext = this.mApnContexts.get(apnType);
        log("onActionIntentRestartTrySetupAlarm: mState=" + this.mState + " apnType=" + apnType + " apnContext=" + apnContext + " mDataConnectionAsyncChannels=" + this.mDataConnectionAcHashMap);
        sendMessage(obtainMessage(270339, apnContext));
    }

    protected void onActionIntentDataStallAlarm(Intent intent) {
        log("onActionIntentDataStallAlarm: action=" + intent.getAction());
        Message msg = obtainMessage(270353, intent.getAction());
        msg.arg1 = intent.getIntExtra(DATA_STALL_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    protected DcTrackerBase(PhoneBase phone) {
        this.mUserDataEnabled = true;
        this.mAutoAttachOnCreation = false;
        this.mPhone = phone;
        log("DCT.constructor");
        this.mResolver = this.mPhone.getContext().getContentResolver();
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 270369, null);
        this.mAlarmManager = (AlarmManager) this.mPhone.getContext().getSystemService("alarm");
        this.mCm = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
        this.mPhone.getSubId();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction(INTENT_DATA_STALL_ALARM);
        filter.addAction(INTENT_PROVISIONING_APN_ALARM);
        this.mUserDataEnabled = getDataEnabled();
        this.mPhone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone);
        this.mDataEnabled[0] = SystemProperties.getBoolean(DEFALUT_DATA_ON_BOOT_PROP, true);
        if (this.mDataEnabled[0]) {
            this.mEnabledCount++;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mPhone.getContext());
        this.mAutoAttachOnCreation = sp.getBoolean(PhoneBase.DATA_DISABLED_ON_BOOT_KEY, false);
        this.mSubscriptionManager = SubscriptionManager.from(this.mPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        HandlerThread dcHandlerThread = new HandlerThread("DcHandlerThread");
        dcHandlerThread.start();
        Handler dcHandler = new Handler(dcHandlerThread.getLooper());
        this.mDcc = DcController.makeDcc(this.mPhone, this, dcHandler);
        this.mDcTesterFailBringUpAll = new DcTesterFailBringUpAll(this.mPhone, dcHandler);
    }

    public void dispose() {
        log("DCT.dispose");
        for (DcAsyncChannel dcac : this.mDataConnectionAcHashMap.values()) {
            dcac.disconnect();
        }
        this.mDataConnectionAcHashMap.clear();
        this.mIsDisposed = true;
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        this.mUiccController.unregisterForIccChanged(this);
        if (this.mDataRoamingSettingObserver != null) {
            this.mDataRoamingSettingObserver.unregister();
        }
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mDcc.dispose();
        this.mDcTesterFailBringUpAll.dispose();
    }

    public long getSubId() {
        return this.mPhone.getSubId();
    }

    public DctConstants.Activity getActivity() {
        return this.mActivity;
    }

    void setActivity(DctConstants.Activity activity) {
        log("setActivity = " + activity);
        this.mActivity = activity;
        this.mPhone.notifyDataActivity();
    }

    public void incApnRefCount(String name) {
    }

    public void decApnRefCount(String name) {
    }

    public boolean isApnSupported(String name) {
        return false;
    }

    public int getApnPriority(String name) {
        return -1;
    }

    public boolean isApnTypeActive(String type) {
        ApnSetting dunApn;
        return (!"dun".equals(type) || (dunApn = fetchDunApn()) == null) ? this.mActiveApn != null && this.mActiveApn.canHandleType(type) : this.mActiveApn != null && dunApn.toString().equals(this.mActiveApn.toString());
    }

    protected ApnSetting fetchDunApn() {
        if (SystemProperties.getBoolean("net.tethering.noprovisioning", false)) {
            log("fetchDunApn: net.tethering.noprovisioning=true ret: null");
            return null;
        }
        int bearer = -1;
        ApnSetting retDunSetting = null;
        String apnData = Settings.Global.getString(this.mResolver, "tether_dun_apn");
        List<ApnSetting> dunSettings = ApnSetting.arrayFromString(apnData);
        IccRecords r = this.mIccRecords.get();
        for (ApnSetting dunSetting : dunSettings) {
            String operator = r != null ? r.getOperatorNumeric() : "";
            if (dunSetting.bearer != 0) {
                if (bearer == -1) {
                    bearer = this.mPhone.getServiceState().getRilDataRadioTechnology();
                }
                if (dunSetting.bearer != bearer) {
                    continue;
                }
            }
            if (!dunSetting.numeric.equals(operator)) {
                continue;
            } else if (dunSetting.hasMvnoParams()) {
                if (r != null && mvnoMatches(r, dunSetting.mvnoType, dunSetting.mvnoMatchData)) {
                    return dunSetting;
                }
            } else {
                return dunSetting;
            }
        }
        Context c = this.mPhone.getContext();
        String[] apnArrayData = c.getResources().getStringArray(R.array.config_autoKeyboardBacklightBrightnessValues);
        for (String apn : apnArrayData) {
            ApnSetting dunSetting2 = ApnSetting.fromString(apn);
            if (dunSetting2 != null) {
                if (dunSetting2.bearer != 0) {
                    if (bearer == -1) {
                        bearer = this.mPhone.getServiceState().getRilDataRadioTechnology();
                    }
                    if (dunSetting2.bearer != bearer) {
                        continue;
                    }
                } else if (dunSetting2.hasMvnoParams()) {
                    if (r != null && mvnoMatches(r, dunSetting2.mvnoType, dunSetting2.mvnoMatchData)) {
                        return dunSetting2;
                    }
                } else {
                    retDunSetting = dunSetting2;
                }
            }
        }
        return retDunSetting;
    }

    public boolean hasMatchedTetherApnSetting() {
        ApnSetting matched = fetchDunApn();
        log("hasMatchedTetherApnSetting: APN=" + matched);
        return matched != null;
    }

    public String[] getActiveApnTypes() {
        if (this.mActiveApn != null) {
            return this.mActiveApn.types;
        }
        String[] result = {"default"};
        return result;
    }

    public String getActiveApnString(String apnType) {
        if (this.mActiveApn == null) {
            return null;
        }
        String result = this.mActiveApn.apn;
        return result;
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
            this.mSubscriptionManager.setDataRoaming(roaming, phoneSubId);
            log("setDataOnRoamingEnabled: set phoneSubId=" + phoneSubId + " isRoaming=" + enabled);
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
            log("getDataOnRoamingEnabled: SettingNofFoundException snfe=" + snfe);
        }
        log("getDataOnRoamingEnabled: phoneSubId=" + phoneSubId + " isDataRoamingEnabled=" + isDataRoamingEnabled);
        return isDataRoamingEnabled;
    }

    public void setDataEnabled(boolean enable) {
        Message msg = obtainMessage(270366);
        msg.arg1 = enable ? 1 : 0;
        log("setDataEnabled: sendMessage: enable=" + enable);
        sendMessage(msg);
    }

    public boolean getDataEnabled() {
        boolean retVal;
        synchronized (this.mDataEnabledLock) {
            boolean retVal2 = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true"));
            try {
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    retVal = Settings.Global.getInt(this.mResolver, "mobile_data", retVal2 ? 1 : 0) != 0;
                } else {
                    int phoneSubId = this.mPhone.getSubId();
                    retVal = TelephonyManager.getIntWithSubId(this.mResolver, "mobile_data", phoneSubId) != 0;
                }
                log("getDataEnabled: getIntWithSubId retVal=" + retVal);
            } catch (Settings.SettingNotFoundException e) {
                retVal = "true".equalsIgnoreCase(SystemProperties.get("ro.com.android.mobiledata", "true"));
                log("getDataEnabled: system property ro.com.android.mobiledata retVal=" + retVal);
            }
        }
        return retVal;
    }

    @Override
    public void handleMessage(Message msg) {
        boolean isProvApn;
        String apnType;
        switch (msg.what) {
            case 69636:
                log("DISCONNECTED_CONNECTED: msg=" + msg);
                DcAsyncChannel dcac = (DcAsyncChannel) msg.obj;
                this.mDataConnectionAcHashMap.remove(Integer.valueOf(dcac.getDataConnectionIdSync()));
                dcac.disconnected();
                break;
            case 270336:
                this.mCidActive = msg.arg1;
                onDataSetupComplete((AsyncResult) msg.obj);
                break;
            case 270337:
                onRadioAvailable();
                break;
            case 270339:
                String reason = null;
                if (msg.obj instanceof String) {
                    reason = (String) msg.obj;
                }
                onTrySetupData(reason);
                break;
            case 270342:
                onRadioOffOrNotAvailable();
                break;
            case 270343:
                onVoiceCallStarted();
                break;
            case 270344:
                onVoiceCallEnded();
                break;
            case 270347:
                onRoamingOn();
                break;
            case 270348:
                onRoamingOff();
                break;
            case 270349:
                onEnableApn(msg.arg1, msg.arg2);
                break;
            case 270351:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DONE msg=" + msg);
                onDisconnectDone(msg.arg1, (AsyncResult) msg.obj);
                break;
            case 270353:
                onDataStallAlarm(msg.arg1);
                break;
            case 270360:
                boolean tearDown = msg.arg1 != 0;
                onCleanUpConnection(tearDown, msg.arg2, (String) msg.obj);
                break;
            case 270362:
                restartRadio();
                break;
            case 270363:
                onSetInternalDataEnabled(msg.arg1 == 1);
                break;
            case 270364:
                log("EVENT_RESET_DONE");
                onResetDone((AsyncResult) msg.obj);
                break;
            case 270365:
                onCleanUpAllConnections((String) msg.obj);
                break;
            case 270366:
                boolean enabled = msg.arg1 == 1;
                log("CMD_SET_USER_DATA_ENABLE enabled=" + enabled);
                onSetUserDataEnabled(enabled);
                break;
            case 270367:
                boolean met = msg.arg1 == 1;
                log("CMD_SET_DEPENDENCY_MET met=" + met);
                Bundle bundle = msg.getData();
                if (bundle != null && (apnType = (String) bundle.get("apnType")) != null) {
                    onSetDependencyMet(apnType, met);
                    break;
                }
                break;
            case 270368:
                onSetPolicyDataEnabled(msg.arg1 == 1);
                break;
            case 270369:
                onUpdateIcc();
                break;
            case 270370:
                log("DataConnectionTracker.handleMessage: EVENT_DISCONNECT_DC_RETRYING msg=" + msg);
                onDisconnectDcRetrying(msg.arg1, (AsyncResult) msg.obj);
                break;
            case 270371:
                onDataSetupCompleteError((AsyncResult) msg.obj);
                break;
            case 270372:
                sEnableFailFastRefCounter = (msg.arg1 == 1 ? 1 : -1) + sEnableFailFastRefCounter;
                log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA:  sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                if (sEnableFailFastRefCounter < 0) {
                    String s = "CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: sEnableFailFastRefCounter:" + sEnableFailFastRefCounter + " < 0";
                    loge(s);
                    sEnableFailFastRefCounter = 0;
                }
                boolean enabled2 = sEnableFailFastRefCounter > 0;
                log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: enabled=" + enabled2 + " sEnableFailFastRefCounter=" + sEnableFailFastRefCounter);
                if (this.mFailFast != enabled2) {
                    this.mFailFast = enabled2;
                    this.mDataStallDetectionEnabled = !enabled2;
                    if (this.mDataStallDetectionEnabled && getOverallState() == DctConstants.State.CONNECTED && (!this.mInVoiceCall || this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed())) {
                        log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: start data stall");
                        stopDataStallAlarm();
                        startDataStallAlarm(false);
                    } else {
                        log("CMD_SET_ENABLE_FAIL_FAST_MOBILE_DATA: stop data stall");
                        stopDataStallAlarm();
                    }
                }
                break;
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
                } else {
                    loge("CMD_ENABLE_MOBILE_PROVISIONING: provisioningUrl=" + this.mProvisioningUrl);
                    this.mIsProvisioning = true;
                    startProvisioningApnAlarm();
                }
                break;
            case 270374:
                log("CMD_IS_PROVISIONING_APN");
                String apnType2 = null;
                try {
                    Bundle bundle3 = msg.getData();
                    if (bundle3 != null) {
                        apnType2 = (String) bundle3.get("apnType");
                    }
                    if (TextUtils.isEmpty(apnType2)) {
                        loge("CMD_IS_PROVISIONING_APN: apnType is empty");
                        isProvApn = false;
                    } else {
                        isProvApn = isProvisioningApn(apnType2);
                    }
                } catch (ClassCastException e2) {
                    loge("CMD_IS_PROVISIONING_APN: NO provisioning url ignoring");
                    isProvApn = false;
                }
                log("CMD_IS_PROVISIONING_APN: ret=" + isProvApn);
                this.mReplyAc.replyToMessage(msg, 270374, isProvApn ? 1 : 0);
                break;
            case 270375:
                log("EVENT_PROVISIONING_APN_ALARM");
                ApnContext apnCtx = this.mApnContexts.get("default");
                if (apnCtx.isProvisioningApn() && apnCtx.isConnectedOrConnecting()) {
                    if (this.mProvisioningApnAlarmTag == msg.arg1) {
                        log("EVENT_PROVISIONING_APN_ALARM: Disconnecting");
                        this.mIsProvisioning = false;
                        this.mProvisioningUrl = null;
                        stopProvisioningApnAlarm();
                        sendCleanUpConnection(true, apnCtx);
                    } else {
                        log("EVENT_PROVISIONING_APN_ALARM: ignore stale tag, mProvisioningApnAlarmTag:" + this.mProvisioningApnAlarmTag + " != arg1:" + msg.arg1);
                    }
                } else {
                    log("EVENT_PROVISIONING_APN_ALARM: Not connected ignore");
                }
                break;
            case 270376:
                if (msg.arg1 == 1) {
                    handleStartNetStatPoll((DctConstants.Activity) msg.obj);
                } else if (msg.arg1 == 0) {
                    handleStopNetStatPoll((DctConstants.Activity) msg.obj);
                }
                break;
            case 270379:
                log("EVENT_NOTIFY_LINK_PROPERTIES_CHANGED");
                this.mPhone.notifyDataConnection("linkPropertiesChanged", (String) msg.obj);
                break;
            default:
                Rlog.e("DATA", "Unidentified event msg=" + msg);
                break;
        }
    }

    public boolean getAnyDataEnabled() {
        boolean result;
        synchronized (this.mDataEnabledLock) {
            result = this.mInternalDataEnabled && this.mUserDataEnabled && sPolicyDataEnabled && this.mEnabledCount != 0;
        }
        if (!result) {
            log("getAnyDataEnabled " + result);
        }
        return result;
    }

    protected boolean isEmergency() {
        boolean result;
        synchronized (this.mDataEnabledLock) {
            result = this.mPhone.isInEcm() || this.mPhone.isInEmergencyCall();
        }
        log("isEmergency: result=" + result);
        return result;
    }

    protected int apnTypeToId(String type) {
        if (TextUtils.equals(type, "default")) {
            return 0;
        }
        if (TextUtils.equals(type, "mms")) {
            return 1;
        }
        if (TextUtils.equals(type, "supl")) {
            return 2;
        }
        if (TextUtils.equals(type, "dun")) {
            return 3;
        }
        if (TextUtils.equals(type, "hipri")) {
            return 4;
        }
        if (TextUtils.equals(type, "ims")) {
            return 5;
        }
        if (TextUtils.equals(type, "fota")) {
            return 6;
        }
        if (TextUtils.equals(type, "cbs")) {
            return 7;
        }
        if (TextUtils.equals(type, "ia")) {
            return 8;
        }
        if (TextUtils.equals(type, "emergency")) {
            return 9;
        }
        return -1;
    }

    protected String apnIdToType(int id) {
        switch (id) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                break;
            case 9:
                break;
            default:
                log("Unknown id (" + id + ") in apnIdToType");
                break;
        }
        return "default";
    }

    public LinkProperties getLinkProperties(String apnType) {
        int id = apnTypeToId(apnType);
        if (!isApnIdEnabled(id)) {
            return new LinkProperties();
        }
        DcAsyncChannel dcac = this.mDataConnectionAcHashMap.get(0);
        return dcac.getLinkPropertiesSync();
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        int id = apnTypeToId(apnType);
        if (!isApnIdEnabled(id)) {
            return new NetworkCapabilities();
        }
        DcAsyncChannel dcac = this.mDataConnectionAcHashMap.get(0);
        return dcac.getNetworkCapabilitiesSync();
    }

    protected void notifyDataConnection(String reason) {
        for (int id = 0; id < 10; id++) {
            if (this.mDataEnabled[id]) {
                this.mPhone.notifyDataConnection(reason, apnIdToType(id));
            }
        }
        notifyOffApnsOfAvailability(reason);
    }

    static class AnonymousClass5 {
        static final int[] $SwitchMap$com$android$internal$telephony$DctConstants$State = new int[DctConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.IDLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.RETRYING.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTING.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.SCANNING.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.CONNECTED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$DctConstants$State[DctConstants.State.DISCONNECTING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    private void notifyApnIdUpToCurrent(String reason, int apnId) {
        switch (AnonymousClass5.$SwitchMap$com$android$internal$telephony$DctConstants$State[this.mState.ordinal()]) {
            case 2:
            case 3:
            case 4:
                this.mPhone.notifyDataConnection(reason, apnIdToType(apnId), PhoneConstants.DataState.CONNECTING);
                break;
            case 5:
            case 6:
                this.mPhone.notifyDataConnection(reason, apnIdToType(apnId), PhoneConstants.DataState.CONNECTING);
                this.mPhone.notifyDataConnection(reason, apnIdToType(apnId), PhoneConstants.DataState.CONNECTED);
                break;
        }
    }

    private void notifyApnIdDisconnected(String reason, int apnId) {
        this.mPhone.notifyDataConnection(reason, apnIdToType(apnId), PhoneConstants.DataState.DISCONNECTED);
    }

    protected void notifyOffApnsOfAvailability(String reason) {
        log("notifyOffApnsOfAvailability - reason= " + reason);
        for (int id = 0; id < 10; id++) {
            if (!isApnIdEnabled(id)) {
                notifyApnIdDisconnected(reason, id);
            }
        }
    }

    public boolean isApnTypeEnabled(String apnType) {
        if (apnType == null) {
            return false;
        }
        return isApnIdEnabled(apnTypeToId(apnType));
    }

    protected synchronized boolean isApnIdEnabled(int id) {
        return id != -1 ? this.mDataEnabled[id] : false;
    }

    protected void setEnabled(int id, boolean enable) {
        log("setEnabled(" + id + ", " + enable + ") with old state = " + this.mDataEnabled[id] + " and enabledCount = " + this.mEnabledCount);
        Message msg = obtainMessage(270349);
        msg.arg1 = id;
        msg.arg2 = enable ? 1 : 0;
        sendMessage(msg);
    }

    protected void onEnableApn(int apnId, int enabled) {
        log("EVENT_APN_ENABLE_REQUEST apnId=" + apnId + ", apnType=" + apnIdToType(apnId) + ", enabled=" + enabled + ", dataEnabled = " + this.mDataEnabled[apnId] + ", enabledCount = " + this.mEnabledCount + ", isApnTypeActive = " + isApnTypeActive(apnIdToType(apnId)));
        if (enabled == 1) {
            synchronized (this) {
                if (!this.mDataEnabled[apnId]) {
                    this.mDataEnabled[apnId] = true;
                    this.mEnabledCount++;
                }
            }
            String type = apnIdToType(apnId);
            if (!isApnTypeActive(type)) {
                this.mRequestedApnType = type;
                onEnableNewApn();
                return;
            } else {
                notifyApnIdUpToCurrent(Phone.REASON_APN_SWITCHED, apnId);
                return;
            }
        }
        boolean didDisable = false;
        synchronized (this) {
            if (this.mDataEnabled[apnId]) {
                this.mDataEnabled[apnId] = false;
                this.mEnabledCount--;
                didDisable = true;
            }
        }
        if (didDisable) {
            if (this.mEnabledCount == 0 || apnId == 3) {
                this.mRequestedApnType = "default";
                onCleanUpConnection(true, apnId, Phone.REASON_DATA_DISABLED);
            }
            notifyApnIdDisconnected(Phone.REASON_DATA_DISABLED, apnId);
            if (this.mDataEnabled[0] && !isApnTypeActive("default")) {
                this.mRequestedApnType = "default";
                onEnableNewApn();
            }
        }
    }

    protected void onEnableNewApn() {
    }

    protected void onResetDone(AsyncResult ar) {
        log("EVENT_RESET_DONE");
        String reason = null;
        if (ar.userObj instanceof String) {
            reason = (String) ar.userObj;
        }
        gotoIdleAndNotifyDataConnection(reason);
    }

    public boolean setInternalDataEnabled(boolean enable) {
        log("setInternalDataEnabled(" + enable + ")");
        Message msg = obtainMessage(270363);
        msg.arg1 = enable ? 1 : 0;
        sendMessage(msg);
        return true;
    }

    protected void onSetInternalDataEnabled(boolean enabled) {
        synchronized (this.mDataEnabledLock) {
            this.mInternalDataEnabled = enabled;
            if (enabled) {
                log("onSetInternalDataEnabled: changed to enabled, try to setup data call");
                onTrySetupData(Phone.REASON_DATA_ENABLED);
            } else {
                log("onSetInternalDataEnabled: changed to disabled, cleanUpAllConnections");
                cleanUpAllConnections(null);
            }
        }
    }

    public void cleanUpAllConnections(String cause) {
        Message msg = obtainMessage(270365);
        msg.obj = cause;
        sendMessage(msg);
    }

    protected void onSetUserDataEnabled(boolean enabled) {
        synchronized (this.mDataEnabledLock) {
            if (this.mUserDataEnabled != enabled) {
                this.mUserDataEnabled = enabled;
                if (TelephonyManager.getDefault().getSimCount() == 1) {
                    Settings.Global.putInt(this.mResolver, "mobile_data", enabled ? 1 : 0);
                } else {
                    int phoneSubId = this.mPhone.getSubId();
                    Settings.Global.putInt(this.mResolver, "mobile_data" + phoneSubId, enabled ? 1 : 0);
                }
                if (!getDataOnRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
                    if (enabled) {
                        notifyOffApnsOfAvailability(Phone.REASON_ROAMING_ON);
                    } else {
                        notifyOffApnsOfAvailability(Phone.REASON_DATA_DISABLED);
                    }
                }
                if (enabled) {
                    onTrySetupData(Phone.REASON_DATA_ENABLED);
                } else {
                    ArrayList<String> exceptList = new ArrayList<>();
                    exceptList.add("ims");
                    exceptList.add("mms");
                    exceptList.add("supl");
                    exceptList.add("dun");
                    exceptList.add("hipri");
                    exceptList.add("fota");
                    exceptList.add("cbs");
                    exceptList.add("ia");
                    exceptList.add("emergency");
                    onCleanUpAllConnectionsWithException(Phone.REASON_DATA_SPECIFIC_DISABLED, exceptList);
                }
            }
        }
    }

    protected void onSetDependencyMet(String apnType, boolean met) {
    }

    protected void onSetPolicyDataEnabled(boolean enabled) {
        synchronized (this.mDataEnabledLock) {
            boolean prevEnabled = getAnyDataEnabled();
            if (sPolicyDataEnabled != enabled) {
                sPolicyDataEnabled = enabled;
                if (prevEnabled != getAnyDataEnabled()) {
                    if (!prevEnabled) {
                        onTrySetupData(Phone.REASON_DATA_ENABLED);
                    } else {
                        onCleanUpAllConnectionsExceptIms(Phone.REASON_DATA_SPECIFIC_DISABLED);
                    }
                }
            }
        }
    }

    protected String getReryConfig(boolean forDefault) {
        int nt = this.mPhone.getServiceState().getNetworkType();
        if (nt == 4 || nt == 7 || nt == 5 || nt == 6 || nt == 12 || nt == 14) {
            return SystemProperties.get("ro.cdma.data_retry_config");
        }
        if (forDefault) {
            return SystemProperties.get("ro.gsm.data_retry_config");
        }
        return SystemProperties.get("ro.gsm.2nd_data_retry_config");
    }

    protected void resetPollStats() {
        this.mTxPkts = -1L;
        this.mRxPkts = -1L;
        this.mNetStatPollPeriod = 1000;
    }

    void startNetStatPoll() {
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

    void stopNetStatPoll() {
        this.mNetStatPollEnabled = false;
        removeCallbacks(this.mPollNetStat);
        log("stopNetStatPoll");
        if (this.mPhone != null) {
            this.mPhone.notifyDataActivity();
        }
    }

    public void sendStartNetStatPoll(DctConstants.Activity activity) {
        Message msg = obtainMessage(270376);
        msg.arg1 = 1;
        msg.obj = activity;
        sendMessage(msg);
    }

    protected void handleStartNetStatPoll(DctConstants.Activity activity) {
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

    protected void handleStopNetStatPoll(DctConstants.Activity activity) {
        stopNetStatPoll();
        stopDataStallAlarm();
        setActivity(activity);
    }

    public void updateDataActivity() {
        DctConstants.Activity newActivity;
        TxRxSum preTxRxSum = new TxRxSum(this.mTxPkts, this.mRxPkts);
        TxRxSum curTxRxSum = new TxRxSum();
        curTxRxSum.updateTxRxSum();
        this.mTxPkts = curTxRxSum.txPkts;
        this.mRxPkts = curTxRxSum.rxPkts;
        if (this.mNetStatPollEnabled) {
            if (preTxRxSum.txPkts > 0 || preTxRxSum.rxPkts > 0) {
                long sent = this.mTxPkts - preTxRxSum.txPkts;
                long received = this.mRxPkts - preTxRxSum.rxPkts;
                if (sent > 0 && received > 0) {
                    newActivity = DctConstants.Activity.DATAINANDOUT;
                } else if (sent > 0 && received == 0) {
                    newActivity = DctConstants.Activity.DATAOUT;
                } else if (sent == 0 && received > 0) {
                    newActivity = DctConstants.Activity.DATAIN;
                } else {
                    newActivity = this.mActivity == DctConstants.Activity.DORMANT ? this.mActivity : DctConstants.Activity.NONE;
                }
                if (this.mActivity != newActivity && this.mIsScreenOn) {
                    this.mActivity = newActivity;
                    this.mPhone.notifyDataActivity();
                }
            }
        }
    }

    protected static class RecoveryAction {
        public static final int CLEANUP = 1;
        public static final int GET_DATA_CALL_LIST = 0;
        public static final int RADIO_RESTART = 3;
        public static final int RADIO_RESTART_WITH_PROP = 4;
        public static final int REREGISTER = 2;

        protected RecoveryAction() {
        }

        private static boolean isAggressiveRecovery(int value) {
            return value == 1 || value == 2 || value == 3 || value == 4;
        }
    }

    public int getRecoveryAction() {
        int action = Settings.System.getInt(this.mResolver, "radio.data.stall.recovery.action", 0);
        log("getRecoveryAction: " + action);
        return action;
    }

    public void putRecoveryAction(int action) {
        Settings.System.putInt(this.mResolver, "radio.data.stall.recovery.action", action);
        log("putRecoveryAction: " + action);
    }

    protected boolean isConnected() {
        return false;
    }

    protected void doRecovery() {
        if (getOverallState() == DctConstants.State.CONNECTED) {
            int recoveryAction = getRecoveryAction();
            switch (recoveryAction) {
                case 0:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_GET_DATA_CALL_LIST, this.mSentSinceLastRecv);
                    log("doRecovery() get data call list");
                    this.mPhone.mCi.getDataCallList(obtainMessage(270340));
                    putRecoveryAction(1);
                    break;
                case 1:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_CLEANUP, this.mSentSinceLastRecv);
                    log("doRecovery() cleanup all connections");
                    cleanUpAllConnections(Phone.REASON_PDP_RESET);
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
                    putRecoveryAction(4);
                    restartRadio();
                    break;
                case 4:
                    EventLog.writeEvent(EventLogTags.DATA_STALL_RECOVERY_RADIO_RESTART_WITH_PROP, -1);
                    log("restarting radio with gsm.radioreset to true");
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
    }

    private void updateDataStallInfo() {
        TxRxSum preTxRxSum = new TxRxSum(this.mDataStallTxRxSum);
        this.mDataStallTxRxSum.updateTxRxSum();
        log("updateDataStallInfo: mDataStallTxRxSum=" + this.mDataStallTxRxSum + " preTxRxSum=" + preTxRxSum);
        long sent = this.mDataStallTxRxSum.txPkts - preTxRxSum.txPkts;
        long received = this.mDataStallTxRxSum.rxPkts - preTxRxSum.rxPkts;
        if (sent > 0 && received > 0) {
            log("updateDataStallInfo: IN/OUT");
            this.mSentSinceLastRecv = 0L;
            putRecoveryAction(0);
        } else {
            if (sent > 0 && received == 0) {
                if (this.mPhone.getState() == PhoneConstants.State.IDLE) {
                    this.mSentSinceLastRecv += sent;
                } else {
                    this.mSentSinceLastRecv = 0L;
                }
                log("updateDataStallInfo: OUT sent=" + sent + " mSentSinceLastRecv=" + this.mSentSinceLastRecv);
                return;
            }
            if (sent == 0 && received > 0) {
                log("updateDataStallInfo: IN");
                this.mSentSinceLastRecv = 0L;
                putRecoveryAction(0);
                return;
            }
            log("updateDataStallInfo: NONE");
        }
    }

    protected void onDataStallAlarm(int tag) {
        if (this.mDataStallAlarmTag != tag) {
            log("onDataStallAlarm: ignore, tag=" + tag + " expecting " + this.mDataStallAlarmTag);
            return;
        }
        updateDataStallInfo();
        int hangWatchdogTrigger = Settings.Global.getInt(this.mResolver, "pdp_watchdog_trigger_packet_count", 10);
        boolean suspectedStall = false;
        if (this.mSentSinceLastRecv >= hangWatchdogTrigger) {
            log("onDataStallAlarm: tag=" + tag + " do recovery action=" + getRecoveryAction());
            suspectedStall = true;
            sendMessage(obtainMessage(270354));
        } else {
            log("onDataStallAlarm: tag=" + tag + " Sent " + String.valueOf(this.mSentSinceLastRecv) + " pkts since last received, < watchdogTrigger=" + hangWatchdogTrigger);
        }
        startDataStallAlarm(suspectedStall);
    }

    protected void startDataStallAlarm(boolean suspectedStall) {
        int delayInMs;
        int nextAction = getRecoveryAction();
        if (this.mDataStallDetectionEnabled && getOverallState() == DctConstants.State.CONNECTED && SystemProperties.getBoolean("persist.radio.datastall.enabled", false)) {
            if (this.mIsScreenOn || suspectedStall || RecoveryAction.isAggressiveRecovery(nextAction)) {
                delayInMs = Settings.Global.getInt(this.mResolver, "data_stall_alarm_aggressive_delay_in_ms", ServiceStateTracker.DEFAULT_GPRS_CHECK_PERIOD_MILLIS);
            } else {
                delayInMs = Settings.Global.getInt(this.mResolver, "data_stall_alarm_non_aggressive_delay_in_ms", DATA_STALL_ALARM_NON_AGGRESSIVE_DELAY_IN_MS_DEFAULT);
            }
            this.mDataStallAlarmTag++;
            log("startDataStallAlarm: tag=" + this.mDataStallAlarmTag + " delay=" + (delayInMs / 1000) + "s");
            Message msg = obtainMessage(270353);
            msg.arg1 = this.mDataStallAlarmTag;
            sendMessageDelayed(msg, delayInMs);
            return;
        }
        log("startDataStallAlarm: NOT started, no connection tag=" + this.mDataStallAlarmTag);
    }

    protected void stopDataStallAlarm() {
        log("stopDataStallAlarm: current tag=" + this.mDataStallAlarmTag + " mDataStallAlarmIntent=" + this.mDataStallAlarmIntent);
        this.mDataStallAlarmTag++;
        removeMessages(270353);
    }

    protected void restartDataStallAlarm() {
        if (isConnected()) {
            int nextAction = getRecoveryAction();
            if (RecoveryAction.isAggressiveRecovery(nextAction)) {
                log("restartDataStallAlarm: action is pending. not resetting the alarm.");
                return;
            }
            log("restartDataStallAlarm: stop then start.");
            stopDataStallAlarm();
            startDataStallAlarm(false);
        }
    }

    protected void setInitialAttachApn() {
        ApnSetting iaApnSetting = null;
        ApnSetting defaultApnSetting = null;
        ApnSetting firstApnSetting = null;
        log("setInitialApn: E mPreferredApn=" + this.mPreferredApn);
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            firstApnSetting = this.mAllApnSettings.get(0);
            log("setInitialApn: firstApnSetting=" + firstApnSetting);
            Iterator<ApnSetting> it = this.mAllApnSettings.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                ApnSetting apn = it.next();
                if (ArrayUtils.contains(apn.types, "ia") && apn.carrierEnabled) {
                    log("setInitialApn: iaApnSetting=" + apn);
                    iaApnSetting = apn;
                    break;
                } else if (defaultApnSetting == null && apn.canHandleType("default")) {
                    log("setInitialApn: defaultApnSetting=" + apn);
                    defaultApnSetting = apn;
                }
            }
        }
        ApnSetting initialAttachApnSetting = null;
        if (iaApnSetting != null) {
            log("setInitialAttachApn: using iaApnSetting");
            initialAttachApnSetting = iaApnSetting;
        } else if (this.mPreferredApn != null) {
            log("setInitialAttachApn: using mPreferredApn");
            initialAttachApnSetting = this.mPreferredApn;
        } else if (defaultApnSetting != null) {
            log("setInitialAttachApn: using defaultApnSetting");
            initialAttachApnSetting = defaultApnSetting;
        } else if (firstApnSetting != null) {
            log("setInitialAttachApn: using firstApnSetting");
            initialAttachApnSetting = firstApnSetting;
        }
        if (initialAttachApnSetting == null) {
            log("setInitialAttachApn: X There in no available apn");
        } else {
            log("setInitialAttachApn: X selected Apn=" + initialAttachApnSetting);
            this.mPhone.mCi.setInitialAttachApn(initialAttachApnSetting.apn, initialAttachApnSetting.protocol, initialAttachApnSetting.authType, initialAttachApnSetting.user, initialAttachApnSetting.password, null);
        }
    }

    protected void setDataProfilesAsNeeded() {
        log("setDataProfilesAsNeeded");
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            ArrayList<DataProfile> dps = new ArrayList<>();
            for (ApnSetting apn : this.mAllApnSettings) {
                if (apn.modemCognitive) {
                    DataProfile dp = new DataProfile(apn, this.mPhone.getServiceState().getDataRoaming());
                    boolean isDup = false;
                    Iterator<DataProfile> it = dps.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        DataProfile dpIn = it.next();
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
            if (dps.size() > 0) {
                this.mPhone.mCi.setDataProfile((DataProfile[]) dps.toArray(new DataProfile[0]), null);
            }
        }
    }

    protected void onActionIntentProvisioningApnAlarm(Intent intent) {
        log("onActionIntentProvisioningApnAlarm: action=" + intent.getAction());
        Message msg = obtainMessage(270375, intent.getAction());
        msg.arg1 = intent.getIntExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, 0);
        sendMessage(msg);
    }

    protected void startProvisioningApnAlarm() {
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
        log("startProvisioningApnAlarm: tag=" + this.mProvisioningApnAlarmTag + " delay=" + (delayInMs / 1000) + "s");
        Intent intent = new Intent(INTENT_PROVISIONING_APN_ALARM);
        intent.putExtra(PROVISIONING_APN_ALARM_TAG_EXTRA, this.mProvisioningApnAlarmTag);
        this.mProvisioningApnAlarmIntent = PendingIntent.getBroadcast(this.mPhone.getContext(), 0, intent, 134217728);
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + ((long) delayInMs), this.mProvisioningApnAlarmIntent);
    }

    protected void stopProvisioningApnAlarm() {
        log("stopProvisioningApnAlarm: current tag=" + this.mProvisioningApnAlarmTag + " mProvsioningApnAlarmIntent=" + this.mProvisioningApnAlarmIntent);
        this.mProvisioningApnAlarmTag++;
        if (this.mProvisioningApnAlarmIntent != null) {
            this.mAlarmManager.cancel(this.mProvisioningApnAlarmIntent);
            this.mProvisioningApnAlarmIntent = null;
        }
    }

    void sendCleanUpConnection(boolean tearDown, ApnContext apnContext) {
        log("sendCleanUpConnection: tearDown=" + tearDown + " apnContext=" + apnContext);
        Message msg = obtainMessage(270360);
        msg.arg1 = tearDown ? 1 : 0;
        msg.arg2 = 0;
        msg.obj = apnContext;
        sendMessage(msg);
    }

    void sendNotifyLinkPropertyChanged(String apnType) {
        log("sendNotifyLinkPropertyChanged: apnType=" + apnType);
        Message msg = obtainMessage(270379);
        msg.obj = apnType;
        sendMessage(msg);
    }

    void sendRestartRadio() {
        log("sendRestartRadio:");
        Message msg = obtainMessage(270362);
        sendMessage(msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("DcTrackerBase:");
        pw.println(" RADIO_TESTS=false");
        pw.println(" mInternalDataEnabled=" + this.mInternalDataEnabled);
        pw.println(" mUserDataEnabled=" + this.mUserDataEnabled);
        pw.println(" sPolicyDataEnabed=" + sPolicyDataEnabled);
        pw.println(" mDataEnabled:");
        for (int i = 0; i < this.mDataEnabled.length; i++) {
            pw.printf("  mDataEnabled[%d]=%b\n", Integer.valueOf(i), Boolean.valueOf(this.mDataEnabled[i]));
        }
        pw.flush();
        pw.println(" mEnabledCount=" + this.mEnabledCount);
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
        pw.println(" mCidActive=" + this.mCidActive);
        pw.println(" mAutoAttachOnCreation=" + this.mAutoAttachOnCreation);
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
            Iterator<Map.Entry<String, ApnContext>> it = apnCtxsSet.iterator();
            while (it.hasNext()) {
                it.next().getValue().dump(fd, pw, args);
            }
            pw.println(" ***************************************");
        } else {
            pw.println(" mApnContexts=null");
        }
        pw.flush();
        pw.println(" mActiveApn=" + this.mActiveApn);
        ArrayList<ApnSetting> apnSettings = this.mAllApnSettings;
        if (apnSettings != null) {
            pw.println(" mAllApnSettings size=" + apnSettings.size());
            for (int i2 = 0; i2 < apnSettings.size(); i2++) {
                pw.printf(" mAllApnSettings[%d]: %s\n", Integer.valueOf(i2), apnSettings.get(i2));
            }
            pw.flush();
        } else {
            pw.println(" mAllApnSettings=null");
        }
        pw.println(" mPreferredApn=" + this.mPreferredApn);
        pw.println(" mIsPsRestricted=" + this.mIsPsRestricted);
        pw.println(" mIsDisposed=" + this.mIsDisposed);
        pw.println(" mIntentReceiver=" + this.mIntentReceiver);
        pw.println(" mDataRoamingSettingObserver=" + this.mDataRoamingSettingObserver);
        pw.flush();
    }

    public void suspendDataCallByOtherPhone() {
    }

    public void resumeDataCallByOtherPhone() {
    }

    public boolean isDataBlockedByOther() {
        return false;
    }
}
