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
import android.os.AsyncResult;
import android.os.BaseBundle;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityTdscdma;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellLocation;
import android.telephony.DataSpecificRegistrationStates;
import android.telephony.NetworkRegistrationState;
import android.telephony.PhysicalChannelConfig;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoiceSpecificRegistrationStates;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.LocalLog;
import android.util.Pair;
import android.util.SparseArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.TransportManager;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.util.NotificationChannelController;
import com.android.internal.telephony.util.TimeStampedValue;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.PatternSyntaxException;

public class ServiceStateTracker extends Handler {
    protected static final String ACTION_RADIO_OFF = "android.intent.action.ACTION_RADIO_OFF";
    public static final int CS_DISABLED = 1004;
    public static final int CS_EMERGENCY_ENABLED = 1006;
    public static final int CS_ENABLED = 1003;
    public static final int CS_NORMAL_ENABLED = 1005;
    public static final int CS_NOTIFICATION = 999;
    public static final int CS_REJECT_CAUSE_ENABLED = 2001;
    public static final int CS_REJECT_CAUSE_NOTIFICATION = 111;
    static final boolean DBG = true;
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60000;
    public static final String DEFAULT_MNC = "00";
    protected static final int EVENT_ALL_DATA_DISCONNECTED = 49;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED = 40;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 39;
    protected static final int EVENT_CHANGE_IMS_STATE = 45;
    protected static final int EVENT_CHECK_REPORT_GPRS = 22;
    protected static final int EVENT_ERI_FILE_LOADED = 36;
    protected static final int EVENT_GET_CELL_INFO_LIST = 43;
    protected static final int EVENT_GET_LOC_DONE = 15;
    protected static final int EVENT_GET_PREFERRED_NETWORK_TYPE = 19;
    protected static final int EVENT_GET_SIGNAL_STRENGTH = 3;
    public static final int EVENT_ICC_CHANGED = 42;
    protected static final int EVENT_IMS_CAPABILITY_CHANGED = 48;
    protected static final int EVENT_IMS_SERVICE_STATE_CHANGED = 53;
    protected static final int EVENT_IMS_STATE_CHANGED = 46;
    protected static final int EVENT_IMS_STATE_DONE = 47;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED = 18;
    protected static final int EVENT_NETWORK_STATE_CHANGED = 2;
    protected static final int EVENT_NITZ_TIME = 11;
    protected static final int EVENT_NV_READY = 35;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE = 37;
    protected static final int EVENT_PHONE_TYPE_SWITCHED = 50;
    protected static final int EVENT_PHYSICAL_CHANNEL_CONFIG = 55;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH = 10;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION = 34;
    protected static final int EVENT_POLL_STATE_GPRS = 5;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE = 14;
    protected static final int EVENT_POLL_STATE_OPERATOR = 6;
    protected static final int EVENT_POLL_STATE_REGISTRATION = 4;
    protected static final int EVENT_RADIO_ON = 41;
    protected static final int EVENT_RADIO_POWER_FROM_CARRIER = 51;
    protected static final int EVENT_RADIO_POWER_OFF_DONE = 54;
    protected static final int EVENT_RADIO_STATE_CHANGED = 1;
    protected static final int EVENT_RESET_PREFERRED_NETWORK_TYPE = 21;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED = 23;
    protected static final int EVENT_RUIM_READY = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 27;
    protected static final int EVENT_SET_PREFERRED_NETWORK_TYPE = 20;
    protected static final int EVENT_SET_RADIO_POWER_OFF = 38;
    protected static final int EVENT_SIGNAL_STRENGTH_UPDATE = 12;
    protected static final int EVENT_SIM_NOT_INSERTED = 52;
    protected static final int EVENT_SIM_READY = 17;
    protected static final int EVENT_SIM_RECORDS_LOADED = 16;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST = 44;
    private static final int INVALID_LTE_EARFCN = -1;
    public static final String INVALID_MCC = "000";
    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    static final String LOG_TAG = "SST";
    public static final int MS_PER_HOUR = 3600000;
    private static final int POLL_PERIOD_MILLIS = 20000;
    protected static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";
    public static final int PS_DISABLED = 1002;
    public static final int PS_ENABLED = 1001;
    public static final int PS_NOTIFICATION = 888;
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";
    protected static final String REGISTRATION_DENIED_GEN = "General";
    public static final String UNACTIVATED_MIN2_VALUE = "000000";
    public static final String UNACTIVATED_MIN_VALUE = "1111110111";
    private static final boolean VDBG = false;
    private CarrierServiceStateTracker mCSST;
    protected CdmaSubscriptionSourceManager mCdmaSSM;
    public CellLocation mCellLoc;
    protected CommandsInterface mCi;
    private final ContentResolver mCr;
    protected int mDefaultRoamingIndicator;
    protected boolean mDesiredPowerState;
    private final HandlerThread mHandlerThread;
    protected boolean mIsInPrl;
    protected long mLastCellInfoListTime;
    private final LocaleTracker mLocaleTracker;
    protected String mMdn;
    protected String mMin;
    protected CellLocation mNewCellLoc;
    protected int mNewRejectCode;
    protected ServiceState mNewSS;
    protected final NitzStateMachine mNitzState;
    private Notification mNotification;
    protected GsmCdmaPhone mPhone;

    @VisibleForTesting
    public int[] mPollingContext;
    private int mPreferredNetworkType;
    protected String mPrlVersion;
    protected final RatRatcheter mRatRatcheter;
    protected String mRegistrationDeniedReason;
    protected int mRejectCode;
    protected boolean mReportedGprsNoReg;
    public RestrictedState mRestrictedState;
    protected int mRoamingIndicator;
    public ServiceState mSS;
    protected SignalStrength mSignalStrength;
    protected boolean mStartedGprsRegCheck;
    protected SubscriptionController mSubscriptionController;
    private SubscriptionManager mSubscriptionManager;
    private final TransportManager mTransportManager;
    protected UiccController mUiccController;
    private boolean mVoiceCapable;
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;
    protected UiccCardApplication mUiccApplcation = null;
    protected IccRecords mIccRecords = null;
    protected List<CellInfo> mLastCellInfoList = null;
    protected List<PhysicalChannelConfig> mLastPhysicalChannelConfigList = null;
    private boolean mDontPollSignalStrength = false;
    protected RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    protected RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    protected RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mAttachedRegistrants = new RegistrantList();
    protected RegistrantList mDetachedRegistrants = new RegistrantList();
    private RegistrantList mDataRegStateOrRatChangedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkDetachedRegistrants = new RegistrantList();
    private RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    protected RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();
    protected boolean mPendingRadioPowerOffAfterDataOff = false;
    protected int mPendingRadioPowerOffAfterDataOffTag = 0;
    protected boolean mImsRegistrationOnOff = false;
    protected boolean mAlarmSwitch = false;
    public boolean mRadioDisabledByCarrier = false;
    protected PendingIntent mRadioOffIntent = null;
    protected boolean mPowerOffDelayNeed = true;
    protected boolean mDeviceShuttingDown = false;
    protected boolean mSpnUpdatePending = false;
    protected String mCurSpn = null;
    protected String mCurDataSpn = null;
    protected String mCurPlmn = null;
    protected boolean mCurShowPlmn = false;
    protected boolean mCurShowSpn = false;

    @VisibleForTesting
    public int mSubId = -1;
    protected int mPrevSubId = -1;
    private boolean mImsRegistered = false;
    private final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SstSubscriptionsChangedListener();
    private final LocalLog mRoamingLog = new LocalLog(10);
    private final LocalLog mAttachLog = new LocalLog(10);
    private final LocalLog mPhoneTypeLog = new LocalLog(10);
    private final LocalLog mRatLog = new LocalLog(20);
    public final LocalLog mRadioPowerLog = new LocalLog(20);
    protected int mMaxDataCalls = 1;
    protected int mNewMaxDataCalls = 1;
    protected int mReasonDataDenied = -1;
    protected int mNewReasonDataDenied = -1;
    protected boolean mGsmRoaming = false;
    protected boolean mDataRoaming = false;
    protected boolean mEmergencyOnly = false;
    protected boolean mIsSimReady = false;
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                ServiceStateTracker.this.onCarrierConfigChanged();
                return;
            }
            if (!ServiceStateTracker.this.mPhone.isPhoneTypeGsm()) {
                ServiceStateTracker.this.loge("Ignoring intent " + intent + " received on CDMA phone");
                return;
            }
            if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                ServiceStateTracker.this.updateSpnDisplay();
            } else if (intent.getAction().equals(ServiceStateTracker.ACTION_RADIO_OFF)) {
                ServiceStateTracker.this.mAlarmSwitch = false;
                ServiceStateTracker.this.powerOffRadioSafely(ServiceStateTracker.this.mPhone.mDcTracker);
            }
        }
    };
    private int mCurrentOtaspMode = 0;
    protected int mRegistrationState = -1;
    private RegistrantList mCdmaForSubscriptionInfoReadyRegistrants = new RegistrantList();
    private int[] mHomeSystemId = null;
    private int[] mHomeNetworkId = null;
    protected boolean mIsMinInfoReady = false;
    private boolean mIsEriTextLoaded = false;
    protected boolean mIsSubscriptionFromRuim = false;
    protected HbpcdUtils mHbpcdUtils = null;
    private String mCurrentCarrier = null;
    protected final SparseArray<NetworkRegistrationManager> mRegStateManagers = new SparseArray<>();
    private ArrayList<Pair<Integer, Integer>> mEarfcnPairListForRsrpBoost = null;
    private int mLteRsrpBoost = 0;
    private final Object mLteRsrpBoostLock = new Object();
    private SignalStrength mLastSignalStrength = null;

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        private CellInfoResult() {
            this.lockObj = new Object();
        }
    }

    private class SstSubscriptionsChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        public final AtomicInteger mPreviousSubId;

        private SstSubscriptionsChangedListener() {
            this.mPreviousSubId = new AtomicInteger(-1);
        }

        @Override
        public void onSubscriptionsChanged() {
            ServiceStateTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
            int subId = ServiceStateTracker.this.mPhone.getSubId();
            ServiceStateTracker.this.mPrevSubId = this.mPreviousSubId.get();
            if (this.mPreviousSubId.getAndSet(subId) != subId) {
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    Context context = ServiceStateTracker.this.mPhone.getContext();
                    ServiceStateTracker.this.mPhone.notifyPhoneStateChanged();
                    ServiceStateTracker.this.mPhone.notifyCallForwardingIndicator();
                    ServiceStateTracker.this.mPhone.sendSubscriptionSettings(!context.getResources().getBoolean(R.^attr-private.showAtTop));
                    ServiceStateTracker.this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(ServiceStateTracker.this.mSS.getRilDataRadioTechnology()));
                    if (ServiceStateTracker.this.mSpnUpdatePending) {
                        ServiceStateTracker.this.mSubscriptionController.setPlmnSpn(ServiceStateTracker.this.mPhone.getPhoneId(), ServiceStateTracker.this.mCurShowPlmn, ServiceStateTracker.this.mCurPlmn, ServiceStateTracker.this.mCurShowSpn, ServiceStateTracker.this.mCurSpn);
                        ServiceStateTracker.this.mSpnUpdatePending = false;
                    }
                    SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                    String string = defaultSharedPreferences.getString(Phone.NETWORK_SELECTION_KEY, "");
                    String string2 = defaultSharedPreferences.getString(Phone.NETWORK_SELECTION_NAME_KEY, "");
                    String string3 = defaultSharedPreferences.getString(Phone.NETWORK_SELECTION_SHORT_KEY, "");
                    if (!TextUtils.isEmpty(string) || !TextUtils.isEmpty(string2) || !TextUtils.isEmpty(string3)) {
                        SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
                        editorEdit.putString(Phone.NETWORK_SELECTION_KEY + subId, string);
                        editorEdit.putString(Phone.NETWORK_SELECTION_NAME_KEY + subId, string2);
                        editorEdit.putString(Phone.NETWORK_SELECTION_SHORT_KEY + subId, string3);
                        editorEdit.remove(Phone.NETWORK_SELECTION_KEY);
                        editorEdit.remove(Phone.NETWORK_SELECTION_NAME_KEY);
                        editorEdit.remove(Phone.NETWORK_SELECTION_SHORT_KEY);
                        editorEdit.commit();
                    }
                    ServiceStateTracker.this.updateSpnDisplay();
                }
                ServiceStateTracker.this.mPhone.updateVoiceMail();
                if (ServiceStateTracker.this.mSubscriptionController.getSlotIndex(subId) == -1) {
                    ServiceStateTracker.this.sendMessage(ServiceStateTracker.this.obtainMessage(52));
                }
            }
        }
    }

    public ServiceStateTracker(GsmCdmaPhone gsmCdmaPhone, CommandsInterface commandsInterface) {
        this.mUiccController = null;
        this.mNitzState = TelephonyComponentFactory.getInstance().makeNitzStateMachine(gsmCdmaPhone);
        this.mPhone = gsmCdmaPhone;
        this.mCi = commandsInterface;
        this.mRatRatcheter = new RatRatcheter(this.mPhone);
        this.mVoiceCapable = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.popupPromptView);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 42, null);
        this.mCi.setOnSignalStrengthUpdate(this, 12, null);
        this.mCi.registerForCellInfoList(this, 44, null);
        this.mCi.registerForPhysicalChannelConfiguration(this, 55, null);
        this.mSubscriptionController = SubscriptionController.getInstance();
        this.mSubscriptionManager = SubscriptionManager.from(gsmCdmaPhone.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mRestrictedState = new RestrictedState();
        this.mTransportManager = new TransportManager();
        Iterator<Integer> it = this.mTransportManager.getAvailableTransports().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            this.mRegStateManagers.append(iIntValue, new NetworkRegistrationManager(iIntValue, gsmCdmaPhone));
            this.mRegStateManagers.get(iIntValue).registerForNetworkRegistrationStateChanged(this, 2, null);
        }
        this.mHandlerThread = new HandlerThread(LocaleTracker.class.getSimpleName());
        this.mHandlerThread.start();
        this.mLocaleTracker = TelephonyComponentFactory.getInstance().makeLocaleTracker(this.mPhone, this.mHandlerThread.getLooper());
        this.mCi.registerForImsNetworkStateChanged(this, 46, null);
        this.mCi.registerForRadioStateChanged(this, 1, null);
        this.mCi.setOnNITZTime(this, 11, null);
        this.mCr = gsmCdmaPhone.getContext().getContentResolver();
        int i = Settings.Global.getInt(this.mCr, "airplane_mode_on", 0);
        int i2 = Settings.Global.getInt(this.mCr, "enable_cellular_on_boot", 1);
        this.mDesiredPowerState = i2 > 0 && i <= 0;
        this.mRadioPowerLog.log("init : airplane mode = " + i + " enableCellularOnBoot = " + i2);
        setSignalStrengthDefaultValues();
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(1, this, 51, null, false);
        Context context = this.mPhone.getContext();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mIntentReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction(ACTION_RADIO_OFF);
        context.registerReceiver(this.mIntentReceiver, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mIntentReceiver, intentFilter3);
        this.mPhone.notifyOtaspChanged(0);
        this.mCi.setOnRestrictedStateChanged(this, 23, null);
        updatePhoneType();
        this.mCSST = new CarrierServiceStateTracker(gsmCdmaPhone, this);
        registerForNetworkAttached(this.mCSST, 101, null);
        registerForNetworkDetached(this.mCSST, 102, null);
        registerForDataConnectionAttached(this.mCSST, 103, null);
        registerForDataConnectionDetached(this.mCSST, 104, null);
    }

    @VisibleForTesting
    public void updatePhoneType() {
        if (this.mSS != null && this.mSS.getVoiceRoaming()) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getDataRoaming()) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getVoiceRegState() == 0) {
            this.mNetworkDetachedRegistrants.notifyRegistrants();
        }
        if (this.mSS != null && this.mSS.getDataRegState() == 0) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        this.mSS = new ServiceState();
        this.mNewSS = new ServiceState();
        this.mLastCellInfoListTime = 0L;
        this.mLastCellInfoList = null;
        this.mSignalStrength = new SignalStrength();
        this.mStartedGprsRegCheck = false;
        this.mReportedGprsNoReg = false;
        this.mMdn = null;
        this.mMin = null;
        this.mPrlVersion = null;
        this.mIsMinInfoReady = false;
        this.mNitzState.handleNetworkUnavailable();
        cancelPollState();
        if (!this.mPhone.isPhoneTypeGsm()) {
            this.mPhone.registerForSimRecordsLoaded(this, 16, null);
            this.mCellLoc = new CdmaCellLocation();
            this.mNewCellLoc = new CdmaCellLocation();
            this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(this.mPhone.getContext(), this.mCi, this, 39, null);
            this.mIsSubscriptionFromRuim = this.mCdmaSSM.getCdmaSubscriptionSource() == 0;
            this.mCi.registerForCdmaPrlChanged(this, 40, null);
            this.mPhone.registerForEriFileLoaded(this, 36, null);
            this.mCi.registerForCdmaOtaProvision(this, 37, null);
            this.mHbpcdUtils = new HbpcdUtils(this.mPhone.getContext());
            updateOtaspState();
        } else {
            if (this.mCdmaSSM != null) {
                this.mCdmaSSM.dispose(this);
            }
            this.mCi.unregisterForCdmaPrlChanged(this);
            this.mPhone.unregisterForEriFileLoaded(this);
            this.mCi.unregisterForCdmaOtaProvision(this);
            this.mPhone.unregisterForSimRecordsLoaded(this);
            this.mCellLoc = new GsmCellLocation();
            this.mNewCellLoc = new GsmCellLocation();
        }
        onUpdateIccAvailability();
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(0));
        this.mCi.getSignalStrength(obtainMessage(3));
        sendMessage(obtainMessage(50));
        logPhoneTypeChange();
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    @VisibleForTesting
    public void requestShutdown() {
        if (this.mDeviceShuttingDown) {
            return;
        }
        this.mDeviceShuttingDown = true;
        this.mDesiredPowerState = false;
        setPowerStateToDesired();
    }

    public void dispose() {
        this.mCi.unSetOnSignalStrengthUpdate(this);
        this.mUiccController.unregisterForIccChanged(this);
        this.mCi.unregisterForCellInfoList(this);
        this.mCi.unregisterForPhysicalChannelConfiguration(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mHandlerThread.quit();
        this.mCi.unregisterForImsNetworkStateChanged(this);
        this.mPhone.getCarrierActionAgent().unregisterForCarrierAction(this, 1);
        if (this.mCSST != null) {
            this.mCSST.dispose();
            this.mCSST = null;
        }
    }

    public boolean getDesiredPowerState() {
        return this.mDesiredPowerState;
    }

    public boolean getPowerStateFromCarrier() {
        return !this.mRadioDisabledByCarrier;
    }

    protected boolean notifySignalStrength() {
        boolean z = false;
        if (this.mSignalStrength.equals(this.mLastSignalStrength)) {
            return false;
        }
        try {
            this.mPhone.notifySignalStrength();
            z = true;
            this.mLastSignalStrength = this.mSignalStrength;
            return true;
        } catch (NullPointerException e) {
            loge("updateSignalStrength() Phone already destroyed: " + e + "SignalStrength not notified");
            return z;
        }
    }

    protected void notifyDataRegStateRilRadioTechnologyChanged() {
        int rilDataRadioTechnology = this.mSS.getRilDataRadioTechnology();
        int dataRegState = this.mSS.getDataRegState();
        log("notifyDataRegStateRilRadioTechnologyChanged: drs=" + dataRegState + " rat=" + rilDataRadioTechnology);
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(rilDataRadioTechnology));
        this.mDataRegStateOrRatChangedRegistrants.notifyResult(new Pair(Integer.valueOf(dataRegState), Integer.valueOf(rilDataRadioTechnology)));
    }

    protected void useDataRegStateForDataOnlyDevices() {
        if (!this.mVoiceCapable) {
            log("useDataRegStateForDataOnlyDevice: VoiceRegState=" + this.mNewSS.getVoiceRegState() + " DataRegState=" + this.mNewSS.getDataRegState());
            this.mNewSS.setVoiceRegState(this.mNewSS.getDataRegState());
        }
    }

    protected void updatePhoneObject() {
        if (this.mPhone.getContext().getResources().getBoolean(R.^attr-private.pointerIconHelp)) {
            if (!(this.mSS.getVoiceRegState() == 0 || this.mSS.getVoiceRegState() == 2)) {
                log("updatePhoneObject: Ignore update");
            } else {
                this.mPhone.updatePhoneObject(this.mSS.getRilVoiceRadioTechnology());
            }
        }
    }

    public void registerForVoiceRoamingOn(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mVoiceRoamingOnRegistrants.add(registrant);
        if (this.mSS.getVoiceRoaming()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOn(Handler handler) {
        this.mVoiceRoamingOnRegistrants.remove(handler);
    }

    public void registerForVoiceRoamingOff(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mVoiceRoamingOffRegistrants.add(registrant);
        if (!this.mSS.getVoiceRoaming()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOff(Handler handler) {
        this.mVoiceRoamingOffRegistrants.remove(handler);
    }

    public void registerForDataRoamingOn(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mDataRoamingOnRegistrants.add(registrant);
        if (this.mSS.getDataRoaming()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOn(Handler handler) {
        this.mDataRoamingOnRegistrants.remove(handler);
    }

    public void registerForDataRoamingOff(Handler handler, int i, Object obj, boolean z) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mDataRoamingOffRegistrants.add(registrant);
        if (z && !this.mSS.getDataRoaming()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOff(Handler handler) {
        this.mDataRoamingOffRegistrants.remove(handler);
    }

    public void reRegisterNetwork(Message message) {
        this.mCi.getPreferredNetworkType(obtainMessage(19, message));
    }

    public void setRadioPower(boolean z) {
        this.mDesiredPowerState = z;
        setPowerStateToDesired();
    }

    public void setRadioPowerFromCarrier(boolean z) {
        this.mRadioDisabledByCarrier = !z;
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
        this.mWantSingleLocationUpdate = false;
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mCi.setLocationUpdates(false, null);
        }
    }

    public void disableLocationUpdates() {
        this.mWantContinuousLocationUpdates = false;
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mCi.setLocationUpdates(false, null);
        }
    }

    protected void processCellLocationInfo(CellLocation cellLocation, CellIdentity cellIdentity) {
        int i;
        int networkId;
        int i2;
        int latitude;
        int longitude;
        int i3;
        int i4;
        int lac;
        int cid;
        int psc = -1;
        if (this.mPhone.isPhoneTypeGsm()) {
            if (cellIdentity != null) {
                int type = cellIdentity.getType();
                if (type == 1) {
                    CellIdentityGsm cellIdentityGsm = (CellIdentityGsm) cellIdentity;
                    cid = cellIdentityGsm.getCid();
                    lac = cellIdentityGsm.getLac();
                } else {
                    switch (type) {
                        case 3:
                            CellIdentityLte cellIdentityLte = (CellIdentityLte) cellIdentity;
                            cid = cellIdentityLte.getCi();
                            lac = cellIdentityLte.getTac();
                            break;
                        case 4:
                            CellIdentityWcdma cellIdentityWcdma = (CellIdentityWcdma) cellIdentity;
                            int cid2 = cellIdentityWcdma.getCid();
                            int lac2 = cellIdentityWcdma.getLac();
                            psc = cellIdentityWcdma.getPsc();
                            lac = lac2;
                            cid = cid2;
                            break;
                        case 5:
                            CellIdentityTdscdma cellIdentityTdscdma = (CellIdentityTdscdma) cellIdentity;
                            cid = cellIdentityTdscdma.getCid();
                            lac = cellIdentityTdscdma.getLac();
                            break;
                    }
                }
            } else {
                lac = -1;
                cid = -1;
            }
            GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLocation;
            gsmCellLocation.setLacAndCid(lac, cid);
            gsmCellLocation.setPsc(psc);
            return;
        }
        if (cellIdentity == null || cellIdentity.getType() != 2) {
            i = 0;
            networkId = 0;
            i2 = -1;
            latitude = Integer.MAX_VALUE;
            longitude = Integer.MAX_VALUE;
        } else {
            CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) cellIdentity;
            int basestationId = cellIdentityCdma.getBasestationId();
            latitude = cellIdentityCdma.getLatitude();
            longitude = cellIdentityCdma.getLongitude();
            int systemId = cellIdentityCdma.getSystemId();
            networkId = cellIdentityCdma.getNetworkId();
            i2 = basestationId;
            i = systemId;
        }
        if (latitude == 0 && longitude == 0) {
            i3 = Integer.MAX_VALUE;
            i4 = Integer.MAX_VALUE;
        } else {
            i3 = latitude;
            i4 = longitude;
        }
        ((CdmaCellLocation) cellLocation).setCellLocationData(i2, i3, i4, i, networkId);
    }

    protected int getLteEarfcn(CellIdentity cellIdentity) {
        if (cellIdentity != null && cellIdentity.getType() == 3) {
            return ((CellIdentityLte) cellIdentity).getEarfcn();
        }
        return -1;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 1:
            case 50:
                if (!this.mPhone.isPhoneTypeGsm() && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
                    handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                    queueNextSignalStrengthPoll();
                }
                setPowerStateToDesired();
                modemTriggeredPollState();
                return;
            case 2:
                modemTriggeredPollState();
                return;
            case 3:
                if (!this.mCi.getRadioState().isOn()) {
                    return;
                }
                onSignalStrengthResult((AsyncResult) message.obj);
                queueNextSignalStrengthPoll();
                return;
            case 4:
            case 5:
            case 6:
                handlePollStateResult(message.what, (AsyncResult) message.obj);
                return;
            case 7:
            case 8:
            case 9:
            case 13:
            case 24:
            case 25:
            case 28:
            case 29:
            case 30:
            case 31:
            case 32:
            case 33:
            case 41:
            default:
                log("Unhandled message with number: " + message.what);
                return;
            case 10:
                this.mCi.getSignalStrength(obtainMessage(3));
                return;
            case 11:
                AsyncResult asyncResult = (AsyncResult) message.obj;
                setTimeFromNITZString((String) ((Object[]) asyncResult.result)[0], ((Long) ((Object[]) asyncResult.result)[1]).longValue());
                return;
            case 12:
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                this.mDontPollSignalStrength = true;
                onSignalStrengthResult(asyncResult2);
                return;
            case 14:
                log("EVENT_POLL_STATE_NETWORK_SELECTION_MODE");
                AsyncResult asyncResult3 = (AsyncResult) message.obj;
                if (this.mPhone.isPhoneTypeGsm()) {
                    handlePollStateResult(message.what, asyncResult3);
                    return;
                }
                if (asyncResult3.exception == null && asyncResult3.result != null) {
                    if (((int[]) asyncResult3.result)[0] == 1) {
                        this.mPhone.setNetworkSelectionModeAutomatic(null);
                        return;
                    }
                    return;
                }
                log("Unable to getNetworkSelectionMode");
                return;
            case 15:
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4.exception == null) {
                    processCellLocationInfo(this.mCellLoc, ((NetworkRegistrationState) asyncResult4.result).getCellIdentity());
                    this.mPhone.notifyLocationChanged();
                }
                disableSingleLocationUpdate();
                return;
            case 16:
                log("EVENT_SIM_RECORDS_LOADED: what=" + message.what);
                updatePhoneObject();
                updateOtaspState();
                if (this.mPhone.isPhoneTypeGsm()) {
                    updateSpnDisplay();
                    return;
                }
                return;
            case 17:
                this.mOnSubscriptionsChangedListener.mPreviousSubId.set(-1);
                this.mPrevSubId = -1;
                this.mIsSimReady = true;
                pollState();
                queueNextSignalStrengthPoll();
                return;
            case 18:
                if (((AsyncResult) message.obj).exception == null) {
                    this.mRegStateManagers.get(1).getNetworkRegistrationState(1, obtainMessage(15, null));
                    return;
                }
                return;
            case 19:
                AsyncResult asyncResult5 = (AsyncResult) message.obj;
                if (asyncResult5.exception == null) {
                    this.mPreferredNetworkType = ((int[]) asyncResult5.result)[0];
                } else {
                    this.mPreferredNetworkType = 7;
                }
                this.mCi.setPreferredNetworkType(7, obtainMessage(20, asyncResult5.userObj));
                return;
            case 20:
                this.mCi.setPreferredNetworkType(this.mPreferredNetworkType, obtainMessage(21, ((AsyncResult) message.obj).userObj));
                return;
            case 21:
                AsyncResult asyncResult6 = (AsyncResult) message.obj;
                if (asyncResult6.userObj != null) {
                    AsyncResult.forMessage((Message) asyncResult6.userObj).exception = asyncResult6.exception;
                    ((Message) asyncResult6.userObj).sendToTarget();
                    return;
                }
                return;
            case 22:
                if (this.mPhone.isPhoneTypeGsm() && this.mSS != null && !isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                    GsmCellLocation gsmCellLocation = (GsmCellLocation) this.mPhone.getCellLocation();
                    Object[] objArr = new Object[2];
                    objArr[0] = this.mSS.getOperatorNumeric();
                    objArr[1] = Integer.valueOf(gsmCellLocation != null ? gsmCellLocation.getCid() : -1);
                    EventLog.writeEvent(EventLogTags.DATA_NETWORK_REGISTRATION_FAIL, objArr);
                    this.mReportedGprsNoReg = true;
                }
                this.mStartedGprsRegCheck = false;
                return;
            case 23:
                if (this.mPhone.isPhoneTypeGsm()) {
                    log("EVENT_RESTRICTED_STATE_CHANGED");
                    onRestrictedStateChanged((AsyncResult) message.obj);
                    return;
                }
                return;
            case 26:
                if (this.mPhone.getLteOnCdmaMode() == 1) {
                    log("Receive EVENT_RUIM_READY");
                    pollState();
                } else {
                    log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                return;
            case 27:
                if (!this.mPhone.isPhoneTypeGsm()) {
                    log("EVENT_RUIM_RECORDS_LOADED: what=" + message.what);
                    updatePhoneObject();
                    if (this.mPhone.isPhoneTypeCdma()) {
                        updateSpnDisplay();
                        return;
                    }
                    RuimRecords ruimRecords = (RuimRecords) this.mIccRecords;
                    if (ruimRecords != null) {
                        if (ruimRecords.isProvisioned()) {
                            this.mMdn = ruimRecords.getMdn();
                            this.mMin = ruimRecords.getMin();
                            parseSidNid(ruimRecords.getSid(), ruimRecords.getNid());
                            this.mPrlVersion = ruimRecords.getPrlVersion();
                            this.mIsMinInfoReady = true;
                        }
                        updateOtaspState();
                        notifyCdmaSubscriptionInfoReady();
                    }
                    pollState();
                    return;
                }
                return;
            case 34:
                if (!this.mPhone.isPhoneTypeGsm()) {
                    AsyncResult asyncResult7 = (AsyncResult) message.obj;
                    if (asyncResult7.exception == null) {
                        String[] strArr = (String[]) asyncResult7.result;
                        if (strArr != null && strArr.length >= 5) {
                            this.mMdn = strArr[0];
                            parseSidNid(strArr[1], strArr[2]);
                            this.mMin = strArr[3];
                            this.mPrlVersion = strArr[4];
                            log("GET_CDMA_SUBSCRIPTION: MDN=" + this.mMdn);
                            this.mIsMinInfoReady = true;
                            updateOtaspState();
                            notifyCdmaSubscriptionInfoReady();
                            if (!this.mIsSubscriptionFromRuim && this.mIccRecords != null) {
                                log("GET_CDMA_SUBSCRIPTION set imsi in mIccRecords");
                                this.mIccRecords.setImsi(getImsi());
                                return;
                            } else {
                                log("GET_CDMA_SUBSCRIPTION either mIccRecords is null or NV type device - not setting Imsi in mIccRecords");
                                return;
                            }
                        }
                        log("GET_CDMA_SUBSCRIPTION: error parsing cdmaSubscription params num=" + strArr.length);
                        return;
                    }
                    return;
                }
                return;
            case 35:
                updatePhoneObject();
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                getSubscriptionInfoAndStartPollingThreads();
                return;
            case 36:
                log("ERI file has been loaded, repolling.");
                pollState();
                return;
            case 37:
                AsyncResult asyncResult8 = (AsyncResult) message.obj;
                if (asyncResult8.exception == null) {
                    int i = ((int[]) asyncResult8.result)[0];
                    if (i == 8 || i == 10) {
                        log("EVENT_OTA_PROVISION_STATUS_CHANGE: Complete, Reload MDN");
                        this.mCi.getCDMASubscription(obtainMessage(34));
                        return;
                    }
                    return;
                }
                return;
            case 38:
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff && message.arg1 == this.mPendingRadioPowerOffAfterDataOffTag) {
                        log("EVENT_SET_RADIO_OFF, turn radio off now.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOffTag++;
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_SET_RADIO_OFF is stale arg1=" + message.arg1 + "!= tag=" + this.mPendingRadioPowerOffAfterDataOffTag);
                    }
                    break;
                }
                return;
            case 39:
                handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
                return;
            case 40:
                AsyncResult asyncResult9 = (AsyncResult) message.obj;
                if (asyncResult9.exception == null) {
                    this.mPrlVersion = Integer.toString(((int[]) asyncResult9.result)[0]);
                    return;
                }
                return;
            case 42:
                onUpdateIccAvailability();
                if (this.mUiccApplcation != null && this.mUiccApplcation.getState() != IccCardApplicationStatus.AppState.APPSTATE_READY) {
                    this.mIsSimReady = false;
                    updateSpnDisplay();
                    return;
                }
                return;
            case 43:
                AsyncResult asyncResult10 = (AsyncResult) message.obj;
                CellInfoResult cellInfoResult = (CellInfoResult) asyncResult10.userObj;
                synchronized (cellInfoResult.lockObj) {
                    if (asyncResult10.exception != null) {
                        log("EVENT_GET_CELL_INFO_LIST: error ret null, e=" + asyncResult10.exception);
                        cellInfoResult.list = null;
                    } else {
                        cellInfoResult.list = (List) asyncResult10.result;
                    }
                    this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    this.mLastCellInfoList = cellInfoResult.list;
                    cellInfoResult.lockObj.notify();
                    break;
                }
                return;
            case 44:
                AsyncResult asyncResult11 = (AsyncResult) message.obj;
                if (asyncResult11.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + asyncResult11.exception);
                    return;
                }
                List<CellInfo> list = (List) asyncResult11.result;
                this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                this.mLastCellInfoList = list;
                this.mPhone.notifyCellInfo(list);
                return;
            case 45:
                log("EVENT_CHANGE_IMS_STATE:");
                setPowerStateToDesired();
                return;
            case 46:
                this.mCi.getImsRegistrationState(obtainMessage(47));
                return;
            case 47:
                AsyncResult asyncResult12 = (AsyncResult) message.obj;
                if (asyncResult12.exception == null) {
                    this.mImsRegistered = ((int[]) asyncResult12.result)[0] == 1;
                    return;
                }
                return;
            case 48:
                log("EVENT_IMS_CAPABILITY_CHANGED");
                updateSpnDisplay();
                return;
            case 49:
                ProxyController.getInstance().unregisterForAllDataDisconnected(SubscriptionManager.getDefaultDataSubscriptionId(), this);
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff) {
                        log("EVENT_ALL_DATA_DISCONNECTED, turn radio off now.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_ALL_DATA_DISCONNECTED is stale");
                    }
                    break;
                }
                return;
            case 51:
                AsyncResult asyncResult13 = (AsyncResult) message.obj;
                if (asyncResult13.exception == null) {
                    boolean zBooleanValue = ((Boolean) asyncResult13.result).booleanValue();
                    log("EVENT_RADIO_POWER_FROM_CARRIER: " + zBooleanValue);
                    setRadioPowerFromCarrier(zBooleanValue);
                    return;
                }
                return;
            case 52:
                log("EVENT_SIM_NOT_INSERTED");
                cancelAllNotifications();
                this.mMdn = null;
                this.mMin = null;
                this.mIsMinInfoReady = false;
                return;
            case 53:
                log("EVENT_IMS_SERVICE_STATE_CHANGED");
                if (this.mSS.getState() != 0) {
                    this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
                    return;
                }
                return;
            case 54:
                log("EVENT_RADIO_POWER_OFF_DONE");
                if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
                    this.mCi.requestShutdown(null);
                    return;
                }
                return;
            case 55:
                AsyncResult asyncResult14 = (AsyncResult) message.obj;
                if (asyncResult14.exception == null) {
                    List<PhysicalChannelConfig> list2 = (List) asyncResult14.result;
                    this.mPhone.notifyPhysicalChannelConfiguration(list2);
                    this.mLastPhysicalChannelConfigList = list2;
                    if (RatRatcheter.updateBandwidths(getBandwidthsFromConfigs(list2), this.mSS)) {
                        this.mPhone.notifyServiceStateChanged(this.mSS);
                        return;
                    }
                    return;
                }
                return;
        }
    }

    private int[] getBandwidthsFromConfigs(List<PhysicalChannelConfig> list) {
        return list.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((PhysicalChannelConfig) obj).getCellBandwidthDownlink());
            }
        }).mapToInt(new ToIntFunction() {
            @Override
            public final int applyAsInt(Object obj) {
                return ((Integer) obj).intValue();
            }
        }).toArray();
    }

    protected boolean isSidsAllZeros() {
        if (this.mHomeSystemId != null) {
            for (int i = 0; i < this.mHomeSystemId.length; i++) {
                if (this.mHomeSystemId[i] != 0) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    protected boolean isHomeSid(int i) {
        if (this.mHomeSystemId != null) {
            for (int i2 = 0; i2 < this.mHomeSystemId.length; i2++) {
                if (i == this.mHomeSystemId[i2]) {
                    return true;
                }
            }
        }
        return false;
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
        String simOperatorNumericForPhone = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (!TextUtils.isEmpty(simOperatorNumericForPhone) && getCdmaMin() != null) {
            return simOperatorNumericForPhone + getCdmaMin();
        }
        return null;
    }

    public boolean isMinInfoReady() {
        return this.mIsMinInfoReady;
    }

    public int getOtasp() {
        if (!this.mPhone.getIccRecordsLoaded()) {
            log("getOtasp: otasp uninitialized due to sim not loaded");
            return 0;
        }
        int i = 3;
        if (this.mPhone.isPhoneTypeGsm()) {
            log("getOtasp: otasp not needed for GSM");
            return 3;
        }
        if (this.mIsSubscriptionFromRuim && this.mMin == null) {
            return 2;
        }
        if (this.mMin == null || this.mMin.length() < 6) {
            log("getOtasp: bad mMin='" + this.mMin + "'");
            i = 1;
        } else if (this.mMin.equals(UNACTIVATED_MIN_VALUE) || this.mMin.substring(0, 6).equals(UNACTIVATED_MIN2_VALUE) || SystemProperties.getBoolean("test_cdma_setup", false)) {
            i = 2;
        }
        log("getOtasp: state=" + i);
        return i;
    }

    protected void parseSidNid(String str, String str2) {
        if (str != null) {
            String[] strArrSplit = str.split(",");
            this.mHomeSystemId = new int[strArrSplit.length];
            for (int i = 0; i < strArrSplit.length; i++) {
                try {
                    this.mHomeSystemId[i] = Integer.parseInt(strArrSplit[i]);
                } catch (NumberFormatException e) {
                    loge("error parsing system id: " + e);
                }
            }
        }
        log("CDMA_SUBSCRIPTION: SID=" + str);
        if (str2 != null) {
            String[] strArrSplit2 = str2.split(",");
            this.mHomeNetworkId = new int[strArrSplit2.length];
            for (int i2 = 0; i2 < strArrSplit2.length; i2++) {
                try {
                    this.mHomeNetworkId[i2] = Integer.parseInt(strArrSplit2[i2]);
                } catch (NumberFormatException e2) {
                    loge("CDMA_SUBSCRIPTION: error parsing network id: " + e2);
                }
            }
        }
        log("CDMA_SUBSCRIPTION: NID=" + str2);
    }

    protected void updateOtaspState() {
        int otasp = getOtasp();
        int i = this.mCurrentOtaspMode;
        this.mCurrentOtaspMode = otasp;
        if (i != this.mCurrentOtaspMode) {
            log("updateOtaspState: call notifyOtaspChanged old otaspMode=" + i + " new otaspMode=" + this.mCurrentOtaspMode);
            this.mPhone.notifyOtaspChanged(this.mCurrentOtaspMode);
        }
    }

    protected Phone getPhone() {
        return this.mPhone;
    }

    protected void handlePollStateResult(int i, AsyncResult asyncResult) {
        boolean zIsRoamingBetweenOperators;
        if (asyncResult.userObj != this.mPollingContext) {
            return;
        }
        if (asyncResult.exception != null) {
            CommandException.Error commandError = null;
            if (asyncResult.exception instanceof IllegalStateException) {
                log("handlePollStateResult exception " + asyncResult.exception);
            }
            if (asyncResult.exception instanceof CommandException) {
                commandError = ((CommandException) asyncResult.exception).getCommandError();
            }
            if (commandError == CommandException.Error.RADIO_NOT_AVAILABLE) {
                cancelPollState();
                return;
            } else if (commandError != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
                loge("RIL implementation has returned an error where it must succeed" + asyncResult.exception);
            }
        } else {
            try {
                handlePollStateResultMessage(i, asyncResult);
            } catch (RuntimeException e) {
                loge("Exception while polling service state. Probably malformed RIL response." + e);
            }
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] - 1;
        if (this.mPollingContext[0] == 0) {
            if (this.mPhone.isPhoneTypeGsm()) {
                updateRoamingState();
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
            } else {
                boolean z = !isSidsAllZeros() && isHomeSid(this.mNewSS.getCdmaSystemId());
                if (this.mIsSubscriptionFromRuim && (zIsRoamingBetweenOperators = isRoamingBetweenOperators(this.mNewSS.getVoiceRoaming(), this.mNewSS)) != this.mNewSS.getVoiceRoaming()) {
                    log("isRoamingBetweenOperators=" + zIsRoamingBetweenOperators + ". Override CDMA voice roaming to " + zIsRoamingBetweenOperators);
                    this.mNewSS.setVoiceRoaming(zIsRoamingBetweenOperators);
                }
                if (ServiceState.isCdma(this.mNewSS.getRilDataRadioTechnology())) {
                    if (this.mNewSS.getVoiceRegState() == 0) {
                        boolean voiceRoaming = this.mNewSS.getVoiceRoaming();
                        if (this.mNewSS.getDataRoaming() != voiceRoaming) {
                            log("Data roaming != Voice roaming. Override data roaming to " + voiceRoaming);
                            this.mNewSS.setDataRoaming(voiceRoaming);
                        }
                    } else {
                        boolean zIsRoamIndForHomeSystem = isRoamIndForHomeSystem(Integer.toString(this.mRoamingIndicator));
                        if (this.mNewSS.getDataRoaming() == zIsRoamIndForHomeSystem) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("isRoamIndForHomeSystem=");
                            sb.append(zIsRoamIndForHomeSystem);
                            sb.append(", override data roaming to ");
                            sb.append(!zIsRoamIndForHomeSystem);
                            log(sb.toString());
                            this.mNewSS.setDataRoaming(!zIsRoamIndForHomeSystem);
                        }
                    }
                }
                this.mNewSS.setCdmaDefaultRoamingIndicator(this.mDefaultRoamingIndicator);
                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                boolean z2 = TextUtils.isEmpty(this.mPrlVersion) ? false : true;
                if (!z2 || this.mNewSS.getRilVoiceRadioTechnology() == 0) {
                    log("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    this.mNewSS.setCdmaRoamingIndicator(1);
                } else if (!isSidsAllZeros()) {
                    if (!z && !this.mIsInPrl) {
                        this.mNewSS.setCdmaRoamingIndicator(this.mDefaultRoamingIndicator);
                    } else if (z && !this.mIsInPrl) {
                        if (!ServiceState.isLte(this.mNewSS.getRilVoiceRadioTechnology())) {
                            this.mNewSS.setCdmaRoamingIndicator(2);
                        } else {
                            log("Turn off roaming indicator as voice is LTE");
                            this.mNewSS.setCdmaRoamingIndicator(1);
                        }
                    } else if ((z || !this.mIsInPrl) && this.mRoamingIndicator <= 2) {
                        this.mNewSS.setCdmaRoamingIndicator(1);
                    } else {
                        this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                    }
                }
                int cdmaRoamingIndicator = this.mNewSS.getCdmaRoamingIndicator();
                this.mNewSS.setCdmaEriIconIndex(this.mPhone.mEriManager.getCdmaEriIconIndex(cdmaRoamingIndicator, this.mDefaultRoamingIndicator));
                this.mNewSS.setCdmaEriIconMode(this.mPhone.mEriManager.getCdmaEriIconMode(cdmaRoamingIndicator, this.mDefaultRoamingIndicator));
                log("Set CDMA Roaming Indicator to: " + this.mNewSS.getCdmaRoamingIndicator() + ". voiceRoaming = " + this.mNewSS.getVoiceRoaming() + ". dataRoaming = " + this.mNewSS.getDataRoaming() + ", isPrlLoaded = " + z2 + ". namMatch = " + z + " , mIsInPrl = " + this.mIsInPrl + ", mRoamingIndicator = " + this.mRoamingIndicator + ", mDefaultRoamingIndicator= " + this.mDefaultRoamingIndicator);
            }
            pollStateDone();
        }
    }

    protected boolean isRoamingBetweenOperators(boolean z, ServiceState serviceState) {
        return z && !isSameOperatorNameFromSimAndSS(serviceState);
    }

    protected void handlePollStateResultMessage(int i, AsyncResult asyncResult) {
        int networkId;
        String operatorBrandOverride;
        int systemId = 0;
        if (i != 14) {
            switch (i) {
                case 4:
                    NetworkRegistrationState networkRegistrationState = (NetworkRegistrationState) asyncResult.result;
                    VoiceSpecificRegistrationStates voiceSpecificStates = networkRegistrationState.getVoiceSpecificStates();
                    int regState = networkRegistrationState.getRegState();
                    boolean z = voiceSpecificStates.cssSupported;
                    int iNetworkTypeToRilRadioTechnology = ServiceState.networkTypeToRilRadioTechnology(networkRegistrationState.getAccessNetworkTechnology());
                    this.mNewSS.setVoiceRegState(regCodeToServiceState(regState));
                    this.mNewSS.setCssIndicator(z ? 1 : 0);
                    this.mNewSS.setRilVoiceRadioTechnology(iNetworkTypeToRilRadioTechnology);
                    this.mNewSS.addNetworkRegistrationState(networkRegistrationState);
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegistrationState.getCellIdentity());
                    int reasonForDenial = networkRegistrationState.getReasonForDenial();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mGsmRoaming = regCodeIsRoaming(regState);
                        this.mNewRejectCode = reasonForDenial;
                        this.mPhone.getContext().getResources().getBoolean(R.^attr-private.popupPromptView);
                        this.mEmergencyOnly = networkRegistrationState.isEmergencyEnabled();
                    } else {
                        int i2 = voiceSpecificStates.roamingIndicator;
                        int i3 = voiceSpecificStates.systemIsInPrl;
                        int i4 = voiceSpecificStates.defaultRoamingIndicator;
                        this.mRegistrationState = regState;
                        this.mNewSS.setVoiceRoaming(regCodeIsRoaming(regState) && !isRoamIndForHomeSystem(Integer.toString(i2)));
                        this.mRoamingIndicator = i2;
                        this.mIsInPrl = i3 != 0;
                        this.mDefaultRoamingIndicator = i4;
                        CellIdentity cellIdentity = networkRegistrationState.getCellIdentity();
                        if (cellIdentity != null && cellIdentity.getType() == 2) {
                            CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) cellIdentity;
                            systemId = cellIdentityCdma.getSystemId();
                            networkId = cellIdentityCdma.getNetworkId();
                        } else {
                            networkId = 0;
                        }
                        this.mNewSS.setCdmaSystemAndNetworkId(systemId, networkId);
                        if (reasonForDenial == 0) {
                            this.mRegistrationDeniedReason = REGISTRATION_DENIED_GEN;
                        } else if (reasonForDenial == 1) {
                            this.mRegistrationDeniedReason = REGISTRATION_DENIED_AUTH;
                        } else {
                            this.mRegistrationDeniedReason = "";
                        }
                        if (this.mRegistrationState == 3) {
                            log("Registration denied, " + this.mRegistrationDeniedReason);
                        }
                    }
                    processCellLocationInfo(this.mNewCellLoc, networkRegistrationState.getCellIdentity());
                    log("handlPollVoiceRegResultMessage: regState=" + regState + " radioTechnology=" + iNetworkTypeToRilRadioTechnology);
                    break;
                case 5:
                    NetworkRegistrationState networkRegistrationState2 = (NetworkRegistrationState) asyncResult.result;
                    DataSpecificRegistrationStates dataSpecificStates = networkRegistrationState2.getDataSpecificStates();
                    int regState2 = networkRegistrationState2.getRegState();
                    int iRegCodeToServiceState = regCodeToServiceState(regState2);
                    int iNetworkTypeToRilRadioTechnology2 = ServiceState.networkTypeToRilRadioTechnology(networkRegistrationState2.getAccessNetworkTechnology());
                    this.mNewSS.setDataRegState(iRegCodeToServiceState);
                    this.mNewSS.setRilDataRadioTechnology(iNetworkTypeToRilRadioTechnology2);
                    this.mNewSS.addNetworkRegistrationState(networkRegistrationState2);
                    if (iRegCodeToServiceState == 1) {
                        this.mLastPhysicalChannelConfigList = null;
                    }
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegistrationState2.getCellIdentity());
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mNewReasonDataDenied = networkRegistrationState2.getReasonForDenial();
                        this.mNewMaxDataCalls = dataSpecificStates.maxDataCalls;
                        this.mDataRoaming = regCodeIsRoaming(regState2);
                        this.mNewSS.setDataRoamingFromRegistration(this.mDataRoaming);
                        log("handlPollStateResultMessage: GsmSST dataServiceState=" + iRegCodeToServiceState + " regState=" + regState2 + " dataRadioTechnology=" + iNetworkTypeToRilRadioTechnology2);
                    } else if (this.mPhone.isPhoneTypeCdma()) {
                        boolean zRegCodeIsRoaming = regCodeIsRoaming(regState2);
                        this.mNewSS.setDataRoaming(zRegCodeIsRoaming);
                        this.mNewSS.setDataRoamingFromRegistration(zRegCodeIsRoaming);
                        log("handlPollStateResultMessage: cdma dataServiceState=" + iRegCodeToServiceState + " regState=" + regState2 + " dataRadioTechnology=" + iNetworkTypeToRilRadioTechnology2);
                    } else {
                        int rilDataRadioTechnology = this.mSS.getRilDataRadioTechnology();
                        if ((rilDataRadioTechnology == 0 && iNetworkTypeToRilRadioTechnology2 != 0) || ((ServiceState.isCdma(rilDataRadioTechnology) && ServiceState.isLte(iNetworkTypeToRilRadioTechnology2)) || (ServiceState.isLte(rilDataRadioTechnology) && ServiceState.isCdma(iNetworkTypeToRilRadioTechnology2)))) {
                            this.mCi.getSignalStrength(obtainMessage(3));
                        }
                        boolean zRegCodeIsRoaming2 = regCodeIsRoaming(regState2);
                        this.mNewSS.setDataRoaming(zRegCodeIsRoaming2);
                        this.mNewSS.setDataRoamingFromRegistration(zRegCodeIsRoaming2);
                        log("handlPollStateResultMessage: CdmaLteSST dataServiceState=" + iRegCodeToServiceState + " registrationState=" + regState2 + " dataRadioTechnology=" + iNetworkTypeToRilRadioTechnology2);
                    }
                    updateServiceStateLteEarfcnBoost(this.mNewSS, getLteEarfcn(networkRegistrationState2.getCellIdentity()));
                    break;
                case 6:
                    if (this.mPhone.isPhoneTypeGsm()) {
                        String[] strArr = (String[]) asyncResult.result;
                        if (strArr != null && strArr.length >= 3) {
                            operatorBrandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                            if (operatorBrandOverride != null) {
                                log("EVENT_POLL_STATE_OPERATOR: use brandOverride=" + operatorBrandOverride);
                                this.mNewSS.setOperatorName(operatorBrandOverride, operatorBrandOverride, strArr[2]);
                            } else {
                                this.mNewSS.setOperatorName(strArr[0], strArr[1], strArr[2]);
                            }
                            break;
                        }
                    } else {
                        String[] strArr2 = (String[]) asyncResult.result;
                        if (strArr2 != null && strArr2.length >= 3) {
                            if (strArr2[2] == null || strArr2[2].length() < 5 || "00000".equals(strArr2[2])) {
                                strArr2[2] = SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "00000");
                                log("RIL_REQUEST_OPERATOR.response[2], the numeric,  is bad. Using SystemProperties 'ro.cdma.home.operator.numeric'= " + strArr2[2]);
                            }
                            if (!this.mIsSubscriptionFromRuim) {
                                this.mNewSS.setOperatorName(strArr2[0], strArr2[1], strArr2[2]);
                            } else {
                                operatorBrandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                                if (operatorBrandOverride != null) {
                                    this.mNewSS.setOperatorName(operatorBrandOverride, operatorBrandOverride, strArr2[2]);
                                } else {
                                    this.mNewSS.setOperatorName(strArr2[0], strArr2[1], strArr2[2]);
                                }
                            }
                        } else {
                            log("EVENT_POLL_STATE_OPERATOR_CDMA: error parsing opNames");
                        }
                        break;
                    }
                    break;
                default:
                    loge("handlePollStateResultMessage: Unexpected RIL response received: " + i);
                    break;
            }
        }
        int[] iArr = (int[]) asyncResult.result;
        this.mNewSS.setIsManualSelection(iArr[0] == 1);
        if (iArr[0] == 1 && this.mPhone.shouldForceAutoNetworkSelect()) {
            this.mPhone.setNetworkSelectionModeAutomatic(null);
            log(" Forcing Automatic Network Selection, manual selection is not allowed");
        }
    }

    private static boolean isValidLteBandwidthKhz(int i) {
        if (i == 1400 || i == 3000 || i == 5000 || i == 10000 || i == 15000 || i == POLL_PERIOD_MILLIS) {
            return true;
        }
        return false;
    }

    protected void setPhyCellInfoFromCellIdentity(ServiceState serviceState, CellIdentity cellIdentity) {
        if (cellIdentity == null) {
            log("Could not set ServiceState channel number. CellIdentity null");
            return;
        }
        serviceState.setChannelNumber(cellIdentity.getChannelNumber());
        if (cellIdentity instanceof CellIdentityLte) {
            CellIdentityLte cellIdentityLte = (CellIdentityLte) cellIdentity;
            int[] iArr = null;
            if (!ArrayUtils.isEmpty(this.mLastPhysicalChannelConfigList)) {
                int[] bandwidthsFromConfigs = getBandwidthsFromConfigs(this.mLastPhysicalChannelConfigList);
                int length = bandwidthsFromConfigs.length;
                int i = 0;
                while (true) {
                    if (i < length) {
                        int i2 = bandwidthsFromConfigs[i];
                        if (isValidLteBandwidthKhz(i2)) {
                            i++;
                        } else {
                            loge("Invalid LTE Bandwidth in RegistrationState, " + i2);
                            break;
                        }
                    } else {
                        iArr = bandwidthsFromConfigs;
                        break;
                    }
                }
            }
            if (iArr == null || iArr.length == 1) {
                int bandwidth = cellIdentityLte.getBandwidth();
                if (isValidLteBandwidthKhz(bandwidth)) {
                    iArr = new int[]{bandwidth};
                } else if (bandwidth != Integer.MAX_VALUE) {
                    loge("Invalid LTE Bandwidth in RegistrationState, " + bandwidth);
                }
            }
            if (iArr != null) {
                serviceState.setCellBandwidths(iArr);
            }
        }
    }

    protected boolean isRoamIndForHomeSystem(String str) {
        String[] stringArray = Resources.getSystem().getStringArray(R.array.config_autoKeyboardBacklightBrightnessValues);
        log("isRoamIndForHomeSystem: homeRoamIndicators=" + Arrays.toString(stringArray));
        if (stringArray != null) {
            for (String str2 : stringArray) {
                if (str2.equals(str)) {
                    return true;
                }
            }
            log("isRoamIndForHomeSystem: No match found against list for roamInd=" + str);
            return false;
        }
        log("isRoamIndForHomeSystem: No list found");
        return false;
    }

    protected void updateRoamingState() {
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean z = this.mGsmRoaming || this.mDataRoaming;
            if (this.mGsmRoaming && !isOperatorConsideredRoaming(this.mNewSS) && (isSameNamedOperators(this.mNewSS) || isOperatorConsideredNonRoaming(this.mNewSS))) {
                log("updateRoamingState: resource override set non roaming.isSameNamedOperators=" + isSameNamedOperators(this.mNewSS) + ",isOperatorConsideredNonRoaming=" + isOperatorConsideredNonRoaming(this.mNewSS));
                z = false;
            }
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager != null) {
                try {
                    PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
                    if (alwaysOnHomeNetwork(configForSubId)) {
                        log("updateRoamingState: carrier config override always on home network");
                    } else if (isNonRoamingInGsmNetwork(configForSubId, this.mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set non roaming:" + this.mNewSS.getOperatorNumeric());
                    } else if (isRoamingInGsmNetwork(configForSubId, this.mNewSS.getOperatorNumeric())) {
                        log("updateRoamingState: carrier config override set roaming:" + this.mNewSS.getOperatorNumeric());
                        z = true;
                    }
                    z = false;
                } catch (Exception e) {
                    loge("updateRoamingState: unable to access carrier config service");
                }
            } else {
                log("updateRoamingState: no carrier config service available");
            }
            this.mNewSS.setVoiceRoaming(z);
            this.mNewSS.setDataRoaming(z);
            return;
        }
        CarrierConfigManager carrierConfigManager2 = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager2 != null) {
            try {
                PersistableBundle configForSubId2 = carrierConfigManager2.getConfigForSubId(this.mPhone.getSubId());
                String string = Integer.toString(this.mNewSS.getCdmaSystemId());
                if (alwaysOnHomeNetwork(configForSubId2)) {
                    log("updateRoamingState: carrier config override always on home network");
                    setRoamingOff();
                } else if (isNonRoamingInGsmNetwork(configForSubId2, this.mNewSS.getOperatorNumeric()) || isNonRoamingInCdmaNetwork(configForSubId2, string)) {
                    log("updateRoamingState: carrier config override set non-roaming:" + this.mNewSS.getOperatorNumeric() + ", " + string);
                    setRoamingOff();
                } else if (isRoamingInGsmNetwork(configForSubId2, this.mNewSS.getOperatorNumeric()) || isRoamingInCdmaNetwork(configForSubId2, string)) {
                    log("updateRoamingState: carrier config override set roaming:" + this.mNewSS.getOperatorNumeric() + ", " + string);
                    setRoamingOn();
                }
            } catch (Exception e2) {
                loge("updateRoamingState: unable to access carrier config service");
            }
        } else {
            log("updateRoamingState: no carrier config service available");
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
    }

    protected void setRoamingOn() {
        this.mNewSS.setVoiceRoaming(true);
        this.mNewSS.setDataRoaming(true);
        this.mNewSS.setCdmaEriIconIndex(0);
        this.mNewSS.setCdmaEriIconMode(0);
    }

    protected void setRoamingOff() {
        this.mNewSS.setVoiceRoaming(false);
        this.mNewSS.setDataRoaming(false);
        this.mNewSS.setCdmaEriIconIndex(1);
    }

    protected void updateSpnDisplay() {
        String str;
        String str2;
        String string;
        String string2;
        boolean z;
        boolean z2;
        String serviceProviderName;
        boolean z3;
        String str3;
        int[] subId;
        int i;
        int i2;
        updateOperatorNameFromEri();
        int combinedRegState = getCombinedRegState();
        String str4 = null;
        if (this.mPhone.getImsPhone() != null && this.mPhone.getImsPhone().isWifiCallingEnabled() && combinedRegState == 0) {
            String[] stringArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_foldedDeviceStates);
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager != null) {
                try {
                    PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
                    if (configForSubId != null) {
                        i = configForSubId.getInt("wfc_spn_format_idx_int");
                        try {
                            i2 = configForSubId.getInt("wfc_data_spn_format_idx_int");
                        } catch (Exception e) {
                            e = e;
                            loge("updateSpnDisplay: carrier config error: " + e);
                            i2 = 0;
                        }
                    } else {
                        i2 = 0;
                        i = 0;
                    }
                } catch (Exception e2) {
                    e = e2;
                    i = 0;
                }
            } else {
                i2 = 0;
                i = 0;
            }
            str2 = stringArray[i];
            str = stringArray[i2];
        } else {
            str = null;
            str2 = null;
        }
        int i3 = -1;
        if (!this.mPhone.isPhoneTypeGsm()) {
            String operatorAlpha = this.mSS.getOperatorAlpha();
            boolean z4 = operatorAlpha != null;
            int[] subId2 = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
            if (subId2 != null && subId2.length > 0) {
                i3 = subId2[0];
            }
            if (!TextUtils.isEmpty(operatorAlpha) && !TextUtils.isEmpty(str2)) {
                str4 = String.format(str2, operatorAlpha.trim());
            } else if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
                log("updateSpnDisplay: overwriting plmn from " + operatorAlpha + " to null as radio state is off");
            } else {
                str4 = operatorAlpha;
            }
            if (combinedRegState == 1) {
                string = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
                log("updateSpnDisplay: radio is on but out of svc, set plmn='" + string + "'");
            } else {
                string = str4;
            }
            if (this.mSubId != i3 || !TextUtils.equals(string, this.mCurPlmn)) {
                log(String.format("updateSpnDisplay: changed sending intent showPlmn='%b' plmn='%s' subId='%d'", Boolean.valueOf(z4), string, Integer.valueOf(i3)));
                Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                intent.putExtra("showSpn", false);
                intent.putExtra("spn", "");
                intent.putExtra("showPlmn", z4);
                intent.putExtra("plmn", string);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
                this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z4, string, false, "")) {
                    this.mSpnUpdatePending = true;
                }
            }
            this.mSubId = i3;
            this.mCurShowSpn = false;
            this.mCurShowPlmn = z4;
            this.mCurSpn = "";
            this.mCurPlmn = string;
            return;
        }
        IccRecords iccRecords = this.mIccRecords;
        int displayRule = iccRecords != null ? iccRecords.getDisplayRule(this.mSS) : 0;
        if (combinedRegState == 1 || combinedRegState == 2) {
            boolean z5 = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.emulated) && !this.mIsSimReady;
            if (!this.mEmergencyOnly || z5) {
                string2 = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
                z = true;
            } else {
                string2 = Resources.getSystem().getText(R.string.capability_desc_canRequestTouchExploration).toString();
                z = false;
            }
            log("updateSpnDisplay: radio is on but out of service, set plmn='" + string2 + "'");
        } else {
            if (combinedRegState == 0) {
                string2 = this.mSS.getOperatorAlpha();
                z2 = !TextUtils.isEmpty(string2) && (displayRule & 2) == 2;
                z = false;
                serviceProviderName = iccRecords == null ? iccRecords.getServiceProviderName() : "";
                z3 = z && !TextUtils.isEmpty(serviceProviderName) && (displayRule & 1) == 1;
                if (!TextUtils.isEmpty(serviceProviderName) || TextUtils.isEmpty(str2) || TextUtils.isEmpty(str)) {
                    if (TextUtils.isEmpty(string2) && !TextUtils.isEmpty(str2)) {
                        string2 = String.format(str2, string2.trim());
                    } else if (this.mSS.getVoiceRegState() != 3 || (z2 && TextUtils.equals(serviceProviderName, string2))) {
                        z3 = false;
                        str3 = serviceProviderName;
                        serviceProviderName = null;
                    }
                    str3 = serviceProviderName;
                } else {
                    String strTrim = serviceProviderName.trim();
                    serviceProviderName = String.format(str2, strTrim);
                    str3 = String.format(str, strTrim);
                    z2 = false;
                    z3 = true;
                }
                subId = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
                if (subId != null && subId.length > 0) {
                    i3 = subId[0];
                }
                if (this.mSubId == i3 || z2 != this.mCurShowPlmn || z3 != this.mCurShowSpn || !TextUtils.equals(serviceProviderName, this.mCurSpn) || !TextUtils.equals(str3, this.mCurDataSpn) || !TextUtils.equals(string2, this.mCurPlmn)) {
                    log(String.format("updateSpnDisplay: changed sending intent rule=" + displayRule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'", Boolean.valueOf(z2), string2, Boolean.valueOf(z3), serviceProviderName, str3, Integer.valueOf(i3)));
                    Intent intent2 = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                    intent2.putExtra("showSpn", z3);
                    intent2.putExtra("spn", serviceProviderName);
                    intent2.putExtra("spnData", str3);
                    intent2.putExtra("showPlmn", z2);
                    intent2.putExtra("plmn", string2);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, this.mPhone.getPhoneId());
                    this.mPhone.getContext().sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
                    if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z2, string2, z3, serviceProviderName)) {
                        this.mSpnUpdatePending = true;
                    }
                }
                this.mSubId = i3;
                this.mCurShowSpn = z3;
                this.mCurShowPlmn = z2;
                this.mCurSpn = serviceProviderName;
                this.mCurDataSpn = str3;
                this.mCurPlmn = string2;
            }
            string2 = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
            log("updateSpnDisplay: radio is off w/ showPlmn=true plmn=" + string2);
            z = false;
        }
        z2 = true;
        if (iccRecords == null) {
        }
        if (z) {
        }
        if (TextUtils.isEmpty(serviceProviderName)) {
            if (TextUtils.isEmpty(string2)) {
                if (this.mSS.getVoiceRegState() != 3) {
                }
                z3 = false;
                str3 = serviceProviderName;
                serviceProviderName = null;
            }
        }
        subId = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
        if (subId != null) {
            i3 = subId[0];
        }
        if (this.mSubId == i3) {
            log(String.format("updateSpnDisplay: changed sending intent rule=" + displayRule + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'", Boolean.valueOf(z2), string2, Boolean.valueOf(z3), serviceProviderName, str3, Integer.valueOf(i3)));
            Intent intent22 = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent22.putExtra("showSpn", z3);
            intent22.putExtra("spn", serviceProviderName);
            intent22.putExtra("spnData", str3);
            intent22.putExtra("showPlmn", z2);
            intent22.putExtra("plmn", string2);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent22, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent22, UserHandle.ALL);
            if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z2, string2, z3, serviceProviderName)) {
            }
        }
        this.mSubId = i3;
        this.mCurShowSpn = z3;
        this.mCurShowPlmn = z2;
        this.mCurSpn = serviceProviderName;
        this.mCurDataSpn = str3;
        this.mCurPlmn = string2;
    }

    protected void setPowerStateToDesired() {
        String str = "mDeviceShuttingDown=" + this.mDeviceShuttingDown + ", mDesiredPowerState=" + this.mDesiredPowerState + ", getRadioState=" + this.mCi.getRadioState() + ", mPowerOffDelayNeed=" + this.mPowerOffDelayNeed + ", mAlarmSwitch=" + this.mAlarmSwitch + ", mRadioDisabledByCarrier=" + this.mRadioDisabledByCarrier;
        log(str);
        this.mRadioPowerLog.log(str);
        if (this.mPhone.isPhoneTypeGsm() && this.mAlarmSwitch) {
            log("mAlarmSwitch == true");
            ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
        }
        if (this.mDesiredPowerState && !this.mRadioDisabledByCarrier && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            this.mCi.setRadioPower(true, null);
            return;
        }
        if ((!this.mDesiredPowerState || this.mRadioDisabledByCarrier) && this.mCi.getRadioState().isOn()) {
            if (this.mPhone.isPhoneTypeGsm() && this.mPowerOffDelayNeed) {
                if (this.mImsRegistrationOnOff && !this.mAlarmSwitch) {
                    log("mImsRegistrationOnOff == true");
                    Context context = this.mPhone.getContext();
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
                    this.mRadioOffIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_RADIO_OFF), 67108864);
                    this.mAlarmSwitch = true;
                    log("Alarm setting");
                    alarmManager.set(2, SystemClock.elapsedRealtime() + 3000, this.mRadioOffIntent);
                    return;
                }
                powerOffRadioSafely(this.mPhone.mDcTracker);
                return;
            }
            powerOffRadioSafely(this.mPhone.mDcTracker);
            return;
        }
        if (this.mDeviceShuttingDown && this.mCi.getRadioState().isAvailable()) {
            this.mCi.requestShutdown(null);
        }
    }

    protected void onUpdateIccAvailability() {
        UiccCardApplication uiccCardApplication;
        if (this.mUiccController != null && this.mUiccApplcation != (uiccCardApplication = getUiccCardApplication())) {
            if (this.mUiccApplcation != null) {
                log("Removing stale icc objects.");
                this.mUiccApplcation.unregisterForReady(this);
                if (this.mIccRecords != null) {
                    this.mIccRecords.unregisterForRecordsLoaded(this);
                }
                this.mIccRecords = null;
                this.mUiccApplcation = null;
            }
            if (uiccCardApplication != null) {
                log("New card found");
                this.mUiccApplcation = uiccCardApplication;
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

    protected void logRoamingChange() {
        this.mRoamingLog.log(this.mSS.toString());
    }

    protected void logAttachChange() {
        this.mAttachLog.log(this.mSS.toString());
    }

    protected void logPhoneTypeChange() {
        this.mPhoneTypeLog.log(Integer.toString(this.mPhone.getPhoneType()));
    }

    protected void logRatChange() {
        this.mRatLog.log(this.mSS.toString());
    }

    protected void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "] " + str);
    }

    protected void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "] " + str);
    }

    public int getCurrentDataConnectionState() {
        return this.mSS.getDataRegState();
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        if (this.mSS.getCssIndicator() == 1) {
            return true;
        }
        return this.mPhone.isPhoneTypeGsm() && this.mSS.getRilDataRadioTechnology() >= 3;
    }

    public void onImsServiceStateChanged() {
        sendMessage(obtainMessage(53));
    }

    public void setImsRegistrationState(boolean z) {
        log("ImsRegistrationState - registered : " + z);
        if (this.mImsRegistrationOnOff && !z && this.mAlarmSwitch) {
            this.mImsRegistrationOnOff = z;
            ((AlarmManager) this.mPhone.getContext().getSystemService("alarm")).cancel(this.mRadioOffIntent);
            this.mAlarmSwitch = false;
            sendMessage(obtainMessage(45));
            return;
        }
        this.mImsRegistrationOnOff = z;
    }

    public void onImsCapabilityChanged() {
        sendMessage(obtainMessage(48));
    }

    public boolean isRadioOn() {
        return this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON;
    }

    public void pollState() {
        pollState(false);
    }

    protected void modemTriggeredPollState() {
        pollState(true);
    }

    public void pollState(boolean z) {
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        log("pollState: modemTriggered=" + z);
        switch (this.mCi.getRadioState()) {
            case RADIO_UNAVAILABLE:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                pollStateDone();
                return;
            case RADIO_OFF:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                if (this.mDeviceShuttingDown || (!z && 18 != this.mSS.getRilDataRadioTechnology())) {
                    pollStateDone();
                    return;
                }
                break;
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
        int[] iArr2 = this.mPollingContext;
        iArr2[0] = iArr2[0] + 1;
        this.mRegStateManagers.get(1).getNetworkRegistrationState(2, obtainMessage(5, this.mPollingContext));
        int[] iArr3 = this.mPollingContext;
        iArr3[0] = iArr3[0] + 1;
        this.mRegStateManagers.get(1).getNetworkRegistrationState(1, obtainMessage(4, this.mPollingContext));
        if (this.mPhone.isPhoneTypeGsm()) {
            int[] iArr4 = this.mPollingContext;
            iArr4[0] = iArr4[0] + 1;
            this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
        }
    }

    protected void pollStateDone() {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        boolean z16;
        if (!this.mPhone.isPhoneTypeGsm()) {
            updateRoamingState();
        }
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean(PROP_FORCE_ROAMING, false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        if (Build.IS_DEBUGGABLE && this.mPhone.mTelephonyTester != null) {
            this.mPhone.mTelephonyTester.overrideServiceState(this.mNewSS);
        }
        log("Poll ServiceState done:  oldSS=[" + this.mSS + "] newSS=[" + this.mNewSS + "] oldMaxDataCalls=" + this.mMaxDataCalls + " mNewMaxDataCalls=" + this.mNewMaxDataCalls + " oldReasonDataDenied=" + this.mReasonDataDenied + " mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        boolean z17 = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean z18 = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean z19 = this.mSS.getDataRegState() != 0 && this.mNewSS.getDataRegState() == 0;
        boolean z20 = this.mSS.getDataRegState() == 0 && this.mNewSS.getDataRegState() != 0;
        boolean z21 = this.mSS.getDataRegState() != this.mNewSS.getDataRegState();
        boolean z22 = this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState();
        boolean z23 = !this.mNewCellLoc.equals(this.mCellLoc);
        if (this.mNewSS.getDataRegState() == 0) {
            this.mRatRatcheter.ratchet(this.mSS, this.mNewSS, z23);
        }
        boolean z24 = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean z25 = this.mSS.getRilDataRadioTechnology() != this.mNewSS.getRilDataRadioTechnology();
        boolean z26 = !this.mNewSS.equals(this.mSS);
        boolean z27 = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        boolean z28 = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        boolean z29 = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        boolean z30 = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        boolean z31 = z22;
        boolean z32 = this.mRejectCode != this.mNewRejectCode;
        boolean z33 = this.mSS.getCssIndicator() != this.mNewSS.getCssIndicator();
        if (this.mPhone.isPhoneTypeCdmaLte()) {
            boolean z34 = this.mNewSS.getDataRegState() == 0 && ((ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && this.mNewSS.getRilDataRadioTechnology() == 13) || (this.mSS.getRilDataRadioTechnology() == 13 && ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology())));
            if (ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology())) {
                z15 = z34;
            } else {
                z15 = z34;
                if (this.mNewSS.getRilDataRadioTechnology() != 13) {
                    z16 = false;
                    boolean z35 = z16;
                    z = this.mNewSS.getRilDataRadioTechnology() < 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
                    z2 = z15;
                    z3 = z35;
                }
            }
            if (!ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && this.mSS.getRilDataRadioTechnology() != 13) {
                z16 = true;
            }
            boolean z352 = z16;
            if (this.mNewSS.getRilDataRadioTechnology() < 4) {
                z = this.mNewSS.getRilDataRadioTechnology() < 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
                z2 = z15;
                z3 = z352;
            }
        } else {
            z = false;
            z2 = false;
            z3 = false;
        }
        log("pollStateDone: hasRegistered=" + z17 + " hasDeregistered=" + z18 + " hasDataAttached=" + z19 + " hasDataDetached=" + z20 + " hasDataRegStateChanged=" + z21 + " hasRilVoiceRadioTechnologyChanged= " + z24 + " hasRilDataRadioTechnologyChanged=" + z25 + " hasChanged=" + z26 + " hasVoiceRoamingOn=" + z27 + " hasVoiceRoamingOff=" + z28 + " hasDataRoamingOn=" + z29 + " hasDataRoamingOff=" + z30 + " hasLocationChanged=" + z23 + " has4gHandoff = " + z2 + " hasMultiApnSupport=" + z3 + " hasLostMultiApnSupport=" + z + " hasCssIndicatorChanged=" + z33);
        if (z31 || z21) {
            z4 = z30;
            z5 = z29;
            EventLog.writeEvent(this.mPhone.isPhoneTypeGsm() ? EventLogTags.GSM_SERVICE_STATE_CHANGE : EventLogTags.CDMA_SERVICE_STATE_CHANGE, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        } else {
            z4 = z30;
            z5 = z29;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (z24) {
                GsmCellLocation gsmCellLocation = (GsmCellLocation) this.mNewCellLoc;
                int cid = gsmCellLocation != null ? gsmCellLocation.getCid() : -1;
                EventLog.writeEvent(EventLogTags.GSM_RAT_SWITCHED_NEW, Integer.valueOf(cid), Integer.valueOf(this.mSS.getRilVoiceRadioTechnology()), Integer.valueOf(this.mNewSS.getRilVoiceRadioTechnology()));
                log("RAT switched " + ServiceState.rilRadioTechnologyToString(this.mSS.getRilVoiceRadioTechnology()) + " -> " + ServiceState.rilRadioTechnologyToString(this.mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
            }
            if (z33) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_CSS_INDICATOR_CHANGED);
            }
            this.mReasonDataDenied = this.mNewReasonDataDenied;
            this.mMaxDataCalls = this.mNewMaxDataCalls;
            this.mRejectCode = this.mNewRejectCode;
        }
        ServiceState serviceState = this.mPhone.getServiceState();
        ServiceState serviceState2 = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = serviceState2;
        this.mNewSS.setStateOutOfService();
        CellLocation cellLocation = this.mCellLoc;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = cellLocation;
        if (z24) {
            updatePhoneObject();
        }
        TelephonyManager telephonyManager = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (z25) {
            telephonyManager.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), this.mSS.getRilDataRadioTechnology());
            z6 = z28;
            StatsLog.write(76, ServiceState.rilRadioTechnologyToNetworkType(this.mSS.getRilDataRadioTechnology()), this.mPhone.getPhoneId());
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                log("pollStateDone: IWLAN enabled");
            }
        } else {
            z6 = z28;
        }
        if (z17) {
            this.mNetworkAttachedRegistrants.notifyRegistrants();
            mtkIvsrUpdateCsPlmn();
            this.mNitzState.handleNetworkAvailable();
        }
        if (z18) {
            this.mNetworkDetachedRegistrants.notifyRegistrants();
            this.mNitzState.handleNetworkUnavailable();
        }
        if (z32) {
            setNotification(CS_REJECT_CAUSE_ENABLED);
        }
        if (z26) {
            updateSpnDisplay();
            telephonyManager.setNetworkOperatorNameForPhone(this.mPhone.getPhoneId(), this.mSS.getOperatorAlpha());
            String networkOperatorForPhone = telephonyManager.getNetworkOperatorForPhone(this.mPhone.getPhoneId());
            String networkCountryIso = telephonyManager.getNetworkCountryIso(this.mPhone.getPhoneId());
            String operatorNumeric = this.mSS.getOperatorNumeric();
            if (!this.mPhone.isPhoneTypeGsm() && isInvalidOperatorNumeric(operatorNumeric)) {
                operatorNumeric = fixUnknownMcc(operatorNumeric, this.mSS.getCdmaSystemId());
            }
            telephonyManager.setNetworkOperatorNumericForPhone(this.mPhone.getPhoneId(), operatorNumeric);
            if (isInvalidOperatorNumeric(operatorNumeric)) {
                log("operatorNumeric " + operatorNumeric + " is invalid");
                this.mLocaleTracker.updateOperatorNumericAsync("");
                this.mNitzState.handleNetworkUnavailable();
                z7 = z17;
                z8 = z2;
                z9 = z18;
                z10 = z21;
                z11 = z24;
                z12 = z25;
                z13 = z27;
            } else {
                z13 = z27;
                if (this.mSS.getRilDataRadioTechnology() != 18) {
                    if (!this.mPhone.isPhoneTypeGsm()) {
                        setOperatorIdd(operatorNumeric);
                    }
                    this.mLocaleTracker.updateOperatorNumericSync(operatorNumeric);
                    String currentCountry = this.mLocaleTracker.getCurrentCountry();
                    boolean zIccCardExists = iccCardExists();
                    z10 = z21;
                    boolean zNetworkCountryIsoChanged = networkCountryIsoChanged(currentCountry, networkCountryIso);
                    if (zIccCardExists && zNetworkCountryIsoChanged) {
                        z11 = z24;
                        z14 = true;
                    } else {
                        z11 = z24;
                        z14 = false;
                    }
                    z8 = z2;
                    z9 = z18;
                    long jCurrentTimeMillis = System.currentTimeMillis();
                    z12 = z25;
                    StringBuilder sb = new StringBuilder();
                    z7 = z17;
                    sb.append("Before handleNetworkCountryCodeKnown: countryChanged=");
                    sb.append(z14);
                    sb.append(" iccCardExist=");
                    sb.append(zIccCardExists);
                    sb.append(" countryIsoChanged=");
                    sb.append(zNetworkCountryIsoChanged);
                    sb.append(" operatorNumeric=");
                    sb.append(operatorNumeric);
                    sb.append(" prevOperatorNumeric=");
                    sb.append(networkOperatorForPhone);
                    sb.append(" countryIsoCode=");
                    sb.append(currentCountry);
                    sb.append(" prevCountryIsoCode=");
                    sb.append(networkCountryIso);
                    sb.append(" ltod=");
                    sb.append(TimeUtils.logTimeOfDay(jCurrentTimeMillis));
                    log(sb.toString());
                    this.mNitzState.handleNetworkCountryCodeSet(z14);
                } else {
                    z7 = z17;
                    z8 = z2;
                    z9 = z18;
                    z10 = z21;
                    z11 = z24;
                    z12 = z25;
                }
            }
            telephonyManager.setNetworkRoamingForPhone(this.mPhone.getPhoneId(), this.mPhone.isPhoneTypeGsm() ? this.mSS.getVoiceRoaming() : this.mSS.getVoiceRoaming() || this.mSS.getDataRoaming());
            setRoamingType(this.mSS);
            log("Broadcasting ServiceState : " + this.mSS);
            if (!serviceState.equals(this.mPhone.getServiceState())) {
                this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
            }
            this.mPhone.getContext().getContentResolver().insert(Telephony.ServiceStateTable.getUriForSubscriptionId(this.mPhone.getSubId()), Telephony.ServiceStateTable.getContentValuesForServiceState(this.mSS));
            TelephonyMetrics.getInstance().writeServiceStateChanged(this.mPhone.getPhoneId(), this.mSS);
        } else {
            z7 = z17;
            z8 = z2;
            z9 = z18;
            z10 = z21;
            z11 = z24;
            z12 = z25;
            z13 = z27;
        }
        if (z19 || z8 || z20 || z7 || z9) {
            logAttachChange();
        }
        if (z19 || z8) {
            mtkIvsrUpdatePsPlmn();
            this.mAttachedRegistrants.notifyRegistrants();
        }
        if (z20) {
            this.mDetachedRegistrants.notifyRegistrants();
        }
        if (z12 || z11) {
            logRatChange();
        }
        if (z10 || z12) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 == this.mSS.getRilDataRadioTechnology()) {
                this.mPhone.notifyDataConnection(PhoneInternalInterface.REASON_IWLAN_AVAILABLE);
            } else {
                this.mPhone.notifyDataConnection(null);
            }
        }
        if (z13 || z6 || z5 || z4) {
            logRoamingChange();
        }
        if (z13) {
            this.mVoiceRoamingOnRegistrants.notifyRegistrants();
        }
        if (z6) {
            this.mVoiceRoamingOffRegistrants.notifyRegistrants();
        }
        if (z5) {
            this.mDataRoamingOnRegistrants.notifyRegistrants();
        }
        if (z4) {
            this.mDataRoamingOffRegistrants.notifyRegistrants();
        }
        if (z23) {
            this.mPhone.notifyLocationChanged();
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                this.mReportedGprsNoReg = false;
            } else {
                if (this.mStartedGprsRegCheck || this.mReportedGprsNoReg) {
                    return;
                }
                this.mStartedGprsRegCheck = true;
                sendMessageDelayed(obtainMessage(22), Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "gprs_register_check_period_ms", DEFAULT_GPRS_CHECK_PERIOD_MILLIS));
            }
        }
    }

    protected void updateOperatorNameFromEri() {
        boolean z;
        String string;
        if (this.mPhone.isPhoneTypeCdma()) {
            if (this.mCi.getRadioState().isOn() && !this.mIsSubscriptionFromRuim) {
                if (this.mSS.getVoiceRegState() == 0) {
                    string = this.mPhone.getCdmaEriText();
                } else {
                    string = this.mPhone.getContext().getText(R.string.lockscreen_access_pattern_cleared).toString();
                }
                this.mSS.setOperatorAlphaLong(string);
                return;
            }
            return;
        }
        if (this.mPhone.isPhoneTypeCdmaLte()) {
            if (this.mUiccController.getUiccCard(getPhoneId()) == null || this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() == null) {
                z = false;
            } else {
                z = true;
            }
            if (!z && this.mCi.getRadioState().isOn() && this.mPhone.isEriFileLoaded() && (!ServiceState.isLte(this.mSS.getRilVoiceRadioTechnology()) || this.mPhone.getContext().getResources().getBoolean(R.^attr-private.__removed3))) {
                String operatorAlpha = this.mSS.getOperatorAlpha();
                if (this.mSS.getVoiceRegState() == 0) {
                    operatorAlpha = this.mPhone.getCdmaEriText();
                } else if (this.mSS.getVoiceRegState() == 3) {
                    operatorAlpha = this.mIccRecords != null ? this.mIccRecords.getServiceProviderName() : null;
                    if (TextUtils.isEmpty(operatorAlpha)) {
                        operatorAlpha = SystemProperties.get("ro.cdma.home.operator.alpha");
                    }
                } else if (this.mSS.getDataRegState() != 0) {
                    operatorAlpha = this.mPhone.getContext().getText(R.string.lockscreen_access_pattern_cleared).toString();
                }
                this.mSS.setOperatorAlphaLong(operatorAlpha);
            }
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY && this.mIccRecords != null && getCombinedRegState() == 0 && !ServiceState.isLte(this.mSS.getRilVoiceRadioTechnology())) {
                boolean csimSpnDisplayCondition = ((RuimRecords) this.mIccRecords).getCsimSpnDisplayCondition();
                int cdmaEriIconIndex = this.mSS.getCdmaEriIconIndex();
                if (csimSpnDisplayCondition && cdmaEriIconIndex == 1 && isInHomeSidNid(this.mSS.getCdmaSystemId(), this.mSS.getCdmaNetworkId()) && this.mIccRecords != null) {
                    this.mSS.setOperatorAlphaLong(this.mIccRecords.getServiceProviderName());
                }
            }
        }
    }

    protected boolean isInHomeSidNid(int i, int i2) {
        if (isSidsAllZeros() || this.mHomeSystemId.length != this.mHomeNetworkId.length || i == 0) {
            return true;
        }
        for (int i3 = 0; i3 < this.mHomeSystemId.length; i3++) {
            if (this.mHomeSystemId[i3] == i && (this.mHomeNetworkId[i3] == 0 || this.mHomeNetworkId[i3] == 65535 || i2 == 0 || i2 == 65535 || this.mHomeNetworkId[i3] == i2)) {
                return true;
            }
        }
        return false;
    }

    protected void setOperatorIdd(String str) {
        String iddByMcc = this.mHbpcdUtils.getIddByMcc(Integer.parseInt(str.substring(0, 3)));
        if (iddByMcc != null && !iddByMcc.isEmpty()) {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", iddByMcc);
        } else {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", "+");
        }
    }

    protected boolean isInvalidOperatorNumeric(String str) {
        return str == null || str.length() < 5 || str.startsWith(INVALID_MCC);
    }

    protected String fixUnknownMcc(String str, int i) {
        TimeZone timeZoneGuessZoneByNitzStatic;
        boolean z;
        int rawOffset;
        if (i <= 0) {
            return str;
        }
        if (this.mNitzState.getSavedTimeZoneId() != null) {
            timeZoneGuessZoneByNitzStatic = TimeZone.getTimeZone(this.mNitzState.getSavedTimeZoneId());
            z = true;
        } else {
            NitzData cachedNitzData = this.mNitzState.getCachedNitzData();
            if (cachedNitzData == null) {
                timeZoneGuessZoneByNitzStatic = null;
            } else {
                timeZoneGuessZoneByNitzStatic = TimeZoneLookupHelper.guessZoneByNitzStatic(cachedNitzData);
                StringBuilder sb = new StringBuilder();
                sb.append("fixUnknownMcc(): guessNitzTimeZone returned ");
                sb.append(timeZoneGuessZoneByNitzStatic == null ? timeZoneGuessZoneByNitzStatic : timeZoneGuessZoneByNitzStatic.getID());
                log(sb.toString());
            }
            z = false;
        }
        if (timeZoneGuessZoneByNitzStatic != null) {
            rawOffset = timeZoneGuessZoneByNitzStatic.getRawOffset() / MS_PER_HOUR;
        } else {
            rawOffset = 0;
        }
        NitzData cachedNitzData2 = this.mNitzState.getCachedNitzData();
        int mcc = this.mHbpcdUtils.getMcc(i, rawOffset, (cachedNitzData2 == null || !cachedNitzData2.isDst()) ? 0 : 1, z);
        if (mcc > 0) {
            return Integer.toString(mcc) + DEFAULT_MNC;
        }
        return str;
    }

    protected boolean isGprsConsistent(int i, int i2) {
        return i2 != 0 || i == 0;
    }

    protected int regCodeToServiceState(int i) {
        if (i != 1 && i != 5) {
            return 1;
        }
        return 0;
    }

    protected boolean regCodeIsRoaming(int i) {
        return 5 == i;
    }

    private boolean isSameOperatorNameFromSimAndSS(ServiceState serviceState) {
        String simOperatorNameForPhone = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNameForPhone(getPhoneId());
        return (!TextUtils.isEmpty(simOperatorNameForPhone) && simOperatorNameForPhone.equalsIgnoreCase(serviceState.getOperatorAlphaLong())) || (!TextUtils.isEmpty(simOperatorNameForPhone) && simOperatorNameForPhone.equalsIgnoreCase(serviceState.getOperatorAlphaShort()));
    }

    protected boolean isSameNamedOperators(ServiceState serviceState) {
        return currentMccEqualsSimMcc(serviceState) && isSameOperatorNameFromSimAndSS(serviceState);
    }

    protected boolean currentMccEqualsSimMcc(ServiceState serviceState) {
        try {
            return ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId()).substring(0, 3).equals(serviceState.getOperatorNumeric().substring(0, 3));
        } catch (Exception e) {
            return true;
        }
    }

    protected boolean isOperatorConsideredNonRoaming(ServiceState serviceState) {
        String[] stringArray;
        PersistableBundle configForSubId;
        String operatorNumeric = serviceState.getOperatorNumeric();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) != null) {
            stringArray = configForSubId.getStringArray("non_roaming_operator_string_array");
        } else {
            stringArray = null;
        }
        if (ArrayUtils.isEmpty(stringArray) || operatorNumeric == null) {
            return false;
        }
        for (String str : stringArray) {
            if (!TextUtils.isEmpty(str) && operatorNumeric.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    protected boolean isOperatorConsideredRoaming(ServiceState serviceState) {
        String[] stringArray;
        PersistableBundle configForSubId;
        String operatorNumeric = serviceState.getOperatorNumeric();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) != null) {
            stringArray = configForSubId.getStringArray("roaming_operator_string_array");
        } else {
            stringArray = null;
        }
        if (ArrayUtils.isEmpty(stringArray) || operatorNumeric == null) {
            return false;
        }
        for (String str : stringArray) {
            if (!TextUtils.isEmpty(str) && operatorNumeric.startsWith(str)) {
                return true;
            }
        }
        return false;
    }

    private void onRestrictedStateChanged(AsyncResult asyncResult) {
        RestrictedState restrictedState = new RestrictedState();
        log("onRestrictedStateChanged: E rs " + this.mRestrictedState);
        if (asyncResult.exception == null && asyncResult.result != null) {
            int iIntValue = ((Integer) asyncResult.result).intValue();
            restrictedState.setCsEmergencyRestricted(((iIntValue & 1) == 0 && (iIntValue & 4) == 0) ? false : true);
            if (this.mUiccApplcation != null && this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_READY) {
                restrictedState.setCsNormalRestricted(((iIntValue & 2) == 0 && (iIntValue & 4) == 0) ? false : true);
                restrictedState.setPsRestricted((iIntValue & 16) != 0);
            }
            log("onRestrictedStateChanged: new rs " + restrictedState);
            if (!this.mRestrictedState.isPsRestricted() && restrictedState.isPsRestricted()) {
                this.mPsRestrictEnabledRegistrants.notifyRegistrants();
                setNotification(1001);
            } else if (this.mRestrictedState.isPsRestricted() && !restrictedState.isPsRestricted()) {
                this.mPsRestrictDisabledRegistrants.notifyRegistrants();
                setNotification(1002);
            }
            if (this.mRestrictedState.isCsRestricted()) {
                if (!restrictedState.isAnyCsRestricted()) {
                    setNotification(1004);
                } else if (!restrictedState.isCsNormalRestricted()) {
                    setNotification(1006);
                } else if (!restrictedState.isCsEmergencyRestricted()) {
                    setNotification(1005);
                }
            } else if (this.mRestrictedState.isCsEmergencyRestricted() && !this.mRestrictedState.isCsNormalRestricted()) {
                if (!restrictedState.isAnyCsRestricted()) {
                    setNotification(1004);
                } else if (restrictedState.isCsRestricted()) {
                    setNotification(1003);
                } else if (restrictedState.isCsNormalRestricted()) {
                    setNotification(1005);
                }
            } else if (!this.mRestrictedState.isCsEmergencyRestricted() && this.mRestrictedState.isCsNormalRestricted()) {
                if (!restrictedState.isAnyCsRestricted()) {
                    setNotification(1004);
                } else if (restrictedState.isCsRestricted()) {
                    setNotification(1003);
                } else if (restrictedState.isCsEmergencyRestricted()) {
                    setNotification(1006);
                }
            } else if (restrictedState.isCsRestricted()) {
                setNotification(1003);
            } else if (restrictedState.isCsEmergencyRestricted()) {
                setNotification(1006);
            } else if (restrictedState.isCsNormalRestricted()) {
                setNotification(1005);
            }
            this.mRestrictedState = restrictedState;
        }
        log("onRestrictedStateChanged: X rs " + this.mRestrictedState);
    }

    public CellLocation getCellLocation(WorkSource workSource) {
        if (((GsmCellLocation) this.mCellLoc).getLac() >= 0 && ((GsmCellLocation) this.mCellLoc).getCid() >= 0) {
            return this.mCellLoc;
        }
        List<CellInfo> allCellInfo = getAllCellInfo(workSource);
        if (allCellInfo != null) {
            GsmCellLocation gsmCellLocation = new GsmCellLocation();
            for (CellInfo cellInfo : allCellInfo) {
                if (cellInfo instanceof CellInfoGsm) {
                    CellIdentityGsm cellIdentity = ((CellInfoGsm) cellInfo).getCellIdentity();
                    gsmCellLocation.setLacAndCid(cellIdentity.getLac(), cellIdentity.getCid());
                    gsmCellLocation.setPsc(cellIdentity.getPsc());
                    return gsmCellLocation;
                }
                if (cellInfo instanceof CellInfoWcdma) {
                    CellIdentityWcdma cellIdentity2 = ((CellInfoWcdma) cellInfo).getCellIdentity();
                    gsmCellLocation.setLacAndCid(cellIdentity2.getLac(), cellIdentity2.getCid());
                    gsmCellLocation.setPsc(cellIdentity2.getPsc());
                    return gsmCellLocation;
                }
                if ((cellInfo instanceof CellInfoLte) && (gsmCellLocation.getLac() < 0 || gsmCellLocation.getCid() < 0)) {
                    CellIdentityLte cellIdentity3 = ((CellInfoLte) cellInfo).getCellIdentity();
                    if (cellIdentity3.getTac() != Integer.MAX_VALUE && cellIdentity3.getCi() != Integer.MAX_VALUE) {
                        gsmCellLocation.setLacAndCid(cellIdentity3.getTac(), cellIdentity3.getCi());
                        gsmCellLocation.setPsc(0);
                    }
                }
            }
            return gsmCellLocation;
        }
        return this.mCellLoc;
    }

    protected void setTimeFromNITZString(String str, long j) {
        long jElapsedRealtime = SystemClock.elapsedRealtime();
        Rlog.d(LOG_TAG, "NITZ: " + str + "," + j + " start=" + jElapsedRealtime + " delay=" + (jElapsedRealtime - j));
        NitzData nitzData = NitzData.parse(str);
        if (nitzData != null) {
            try {
                this.mNitzState.handleNitzReceived(new TimeStampedValue<>(nitzData, j));
                long jElapsedRealtime2 = SystemClock.elapsedRealtime();
                Rlog.d(LOG_TAG, "NITZ: end=" + jElapsedRealtime2 + " dur=" + (jElapsedRealtime2 - jElapsedRealtime));
            } catch (Throwable th) {
                long jElapsedRealtime3 = SystemClock.elapsedRealtime();
                Rlog.d(LOG_TAG, "NITZ: end=" + jElapsedRealtime3 + " dur=" + (jElapsedRealtime3 - jElapsedRealtime));
                throw th;
            }
        }
    }

    private void cancelAllNotifications() {
        log("cancelAllNotifications: mPrevSubId=" + this.mPrevSubId);
        NotificationManager notificationManager = (NotificationManager) this.mPhone.getContext().getSystemService("notification");
        if (SubscriptionManager.isValidSubscriptionId(this.mPrevSubId)) {
            notificationManager.cancel(Integer.toString(this.mPrevSubId), PS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(this.mPrevSubId), CS_NOTIFICATION);
            notificationManager.cancel(Integer.toString(this.mPrevSubId), 111);
        }
    }

    @VisibleForTesting
    public void setNotification(int i) {
        PersistableBundle config;
        log("setNotification: create notification " + i);
        if (!SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            loge("cannot setNotification on invalid subid mSubId=" + this.mSubId);
            return;
        }
        if (!this.mPhone.getContext().getResources().getBoolean(R.^attr-private.pointerIconZoomOut)) {
            log("Ignore all the notifications");
            return;
        }
        Context context = this.mPhone.getContext();
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService("carrier_config");
        if (carrierConfigManager != null && (config = carrierConfigManager.getConfig()) != null && config.getBoolean("disable_voice_barring_notification_bool", false) && (i == 1003 || i == 1005 || i == 1006)) {
            log("Voice/emergency call barred notification disabled");
            return;
        }
        CharSequence text = "";
        CharSequence string = "";
        int i2 = CS_NOTIFICATION;
        int i3 = R.drawable.stat_sys_warning;
        boolean z = true;
        boolean z2 = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getPhoneCount() > 1;
        int slotIndex = this.mSubscriptionController.getSlotIndex(this.mSubId) + 1;
        if (i == 2001) {
            i2 = 111;
            int iSelectResourceForRejectCode = selectResourceForRejectCode(this.mRejectCode, z2);
            if (iSelectResourceForRejectCode == 0) {
                loge("setNotification: mRejectCode=" + this.mRejectCode + " is not handled.");
                return;
            }
            i3 = R.drawable.pointer_grabbing_large_icon;
            string = context.getString(iSelectResourceForRejectCode, Integer.valueOf(this.mSubId));
            text = null;
        } else {
            switch (i) {
                case 1001:
                    if (SubscriptionManager.getDefaultDataSubscriptionId() == this.mPhone.getSubId()) {
                        string = context.getText(R.string.config_systemAudioIntelligence);
                        if (z2) {
                            text = context.getString(R.string.config_systemActivityRecognizer, Integer.valueOf(slotIndex));
                        } else {
                            text = context.getText(R.string.config_systemVisualIntelligence);
                        }
                        i2 = PS_NOTIFICATION;
                        break;
                    }
                    break;
                case 1002:
                    i2 = PS_NOTIFICATION;
                    break;
                case 1003:
                    string = context.getText(R.string.config_systemAmbientAudioIntelligence);
                    if (z2) {
                        text = context.getString(R.string.config_systemActivityRecognizer, Integer.valueOf(slotIndex));
                    } else {
                        text = context.getText(R.string.config_systemVisualIntelligence);
                    }
                    break;
                case 1005:
                    string = context.getText(R.string.config_systemTextIntelligence);
                    if (z2) {
                        text = context.getString(R.string.config_systemActivityRecognizer, Integer.valueOf(slotIndex));
                    } else {
                        text = context.getText(R.string.config_systemVisualIntelligence);
                    }
                    break;
                case 1006:
                    string = context.getText(R.string.config_systemNotificationIntelligence);
                    if (z2) {
                        text = context.getString(R.string.config_systemActivityRecognizer, Integer.valueOf(slotIndex));
                    } else {
                        text = context.getText(R.string.config_systemVisualIntelligence);
                    }
                    break;
            }
            return;
        }
        log("setNotification, create notification, notifyType: " + i + ", title: " + ((Object) string) + ", details: " + ((Object) text) + ", subId: " + this.mSubId);
        this.mNotification = new Notification.Builder(context).setWhen(System.currentTimeMillis()).setAutoCancel(true).setSmallIcon(i3).setTicker(string).setColor(context.getResources().getColor(R.color.car_colorPrimary)).setContentTitle(string).setStyle(new Notification.BigTextStyle().bigText(text)).setContentText(text).setChannel(NotificationChannelController.CHANNEL_ID_ALERT).build();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        if (i == 1002 || i == 1004) {
            notificationManager.cancel(Integer.toString(this.mSubId), i2);
            return;
        }
        if ((!this.mSS.isEmergencyOnly() || i != 1006) && i != 2001 && this.mSS.getState() != 0) {
            z = false;
        }
        if (z) {
            notificationManager.notify(Integer.toString(this.mSubId), i2, this.mNotification);
        }
    }

    private int selectResourceForRejectCode(int i, boolean z) {
        if (i != 6) {
            switch (i) {
                case 1:
                    if (z) {
                        return R.string.expand_button_content_description_expanded;
                    }
                    return R.string.expand_button_content_description_collapsed;
                case 2:
                    if (z) {
                        return R.string.ext_media_checking_notification_title;
                    }
                    return R.string.ext_media_checking_notification_message;
                case 3:
                    if (z) {
                        return R.string.ext_media_browse_action;
                    }
                    return R.string.ext_media_badremoval_notification_title;
                default:
                    return 0;
            }
        }
        if (z) {
            return R.string.ext_media_badremoval_notification_message;
        }
        return R.string.expires_on;
    }

    protected UiccCardApplication getUiccCardApplication() {
        if (this.mPhone.isPhoneTypeGsm()) {
            return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 1);
        }
        return this.mUiccController.getUiccCardApplication(this.mPhone.getPhoneId(), 2);
    }

    protected void queueNextSignalStrengthPoll() {
        if (this.mDontPollSignalStrength) {
            return;
        }
        Message messageObtainMessage = obtainMessage();
        messageObtainMessage.what = 10;
        sendMessageDelayed(messageObtainMessage, 20000L);
    }

    private void notifyCdmaSubscriptionInfoReady() {
        if (this.mCdmaForSubscriptionInfoReadyRegistrants != null) {
            log("CDMA_SUBSCRIPTION: call notifyRegistrants()");
            this.mCdmaForSubscriptionInfoReadyRegistrants.notifyRegistrants();
        }
    }

    public void registerForDataConnectionAttached(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mAttachedRegistrants.add(registrant);
        if (getCurrentDataConnectionState() == 0) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForDataConnectionAttached(Handler handler) {
        this.mAttachedRegistrants.remove(handler);
    }

    public void registerForDataConnectionDetached(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mDetachedRegistrants.add(registrant);
        if (getCurrentDataConnectionState() != 0) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForDataConnectionDetached(Handler handler) {
        this.mDetachedRegistrants.remove(handler);
    }

    public void registerForDataRegStateOrRatChanged(Handler handler, int i, Object obj) {
        this.mDataRegStateOrRatChangedRegistrants.add(new Registrant(handler, i, obj));
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    public void unregisterForDataRegStateOrRatChanged(Handler handler) {
        this.mDataRegStateOrRatChangedRegistrants.remove(handler);
    }

    public void registerForNetworkAttached(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mNetworkAttachedRegistrants.add(registrant);
        if (this.mSS.getVoiceRegState() == 0) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForNetworkAttached(Handler handler) {
        this.mNetworkAttachedRegistrants.remove(handler);
    }

    public void registerForNetworkDetached(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mNetworkDetachedRegistrants.add(registrant);
        if (this.mSS.getVoiceRegState() != 0) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForNetworkDetached(Handler handler) {
        this.mNetworkDetachedRegistrants.remove(handler);
    }

    public void registerForPsRestrictedEnabled(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mPsRestrictEnabledRegistrants.add(registrant);
        if (this.mRestrictedState.isPsRestricted()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedEnabled(Handler handler) {
        this.mPsRestrictEnabledRegistrants.remove(handler);
    }

    public void registerForPsRestrictedDisabled(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mPsRestrictDisabledRegistrants.add(registrant);
        if (this.mRestrictedState.isPsRestricted()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedDisabled(Handler handler) {
        this.mPsRestrictDisabledRegistrants.remove(handler);
    }

    public void powerOffRadioSafely(DcTracker dcTracker) {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                int iMtkReplaceDdsIfUnset = mtkReplaceDdsIfUnset(SubscriptionManager.getDefaultDataSubscriptionId());
                if (dcTracker.isDisconnected() && (iMtkReplaceDdsIfUnset == this.mPhone.getSubId() || (iMtkReplaceDdsIfUnset != this.mPhone.getSubId() && ProxyController.getInstance().isDataDisconnected(iMtkReplaceDdsIfUnset)))) {
                    dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                    log("Data disconnected, turn off radio right away.");
                    hangupAndPowerOff();
                } else {
                    if (this.mPhone.isPhoneTypeGsm() && this.mPhone.isInCall()) {
                        this.mPhone.mCT.mRingingCall.hangupIfAlive();
                        this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
                        this.mPhone.mCT.mForegroundCall.hangupIfAlive();
                    }
                    dcTracker.cleanUpAllConnections(PhoneInternalInterface.REASON_RADIO_TURNED_OFF);
                    if (iMtkReplaceDdsIfUnset != this.mPhone.getSubId() && !ProxyController.getInstance().isDataDisconnected(iMtkReplaceDdsIfUnset)) {
                        log("Data is active on DDS.  Wait for all data disconnect");
                        ProxyController.getInstance().registerForAllDataDisconnected(iMtkReplaceDdsIfUnset, this, 49, null);
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    }
                    mtkRegisterAllDataDisconnected();
                    Message messageObtain = Message.obtain(this);
                    messageObtain.what = 38;
                    int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                    this.mPendingRadioPowerOffAfterDataOffTag = i;
                    messageObtain.arg1 = i;
                    if (sendMessageDelayed(messageObtain, mtkReplaceDisconnectTimer())) {
                        log("Wait upto 30s for data to disconnect, then turn off radio.");
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    }
                }
            }
        }
    }

    public boolean processPendingRadioPowerOffAfterDataOff() {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                return false;
            }
            log("Process pending request to turn radio off.");
            this.mPendingRadioPowerOffAfterDataOffTag++;
            hangupAndPowerOff();
            this.mPendingRadioPowerOffAfterDataOff = false;
            return true;
        }
    }

    private boolean containsEarfcnInEarfcnRange(ArrayList<Pair<Integer, Integer>> arrayList, int i) {
        if (arrayList != null) {
            for (Pair<Integer, Integer> pair : arrayList) {
                if (i >= ((Integer) pair.first).intValue() && i <= ((Integer) pair.second).intValue()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    ArrayList<Pair<Integer, Integer>> convertEarfcnStringArrayToPairList(String[] strArr) {
        int i;
        int i2;
        ArrayList<Pair<Integer, Integer>> arrayList = new ArrayList<>();
        if (strArr != null) {
            for (String str : strArr) {
                try {
                    String[] strArrSplit = str.split("-");
                    if (strArrSplit.length != 2 || (i = Integer.parseInt(strArrSplit[0])) > (i2 = Integer.parseInt(strArrSplit[1]))) {
                        return null;
                    }
                    arrayList.add(new Pair<>(Integer.valueOf(i), Integer.valueOf(i2)));
                } catch (NumberFormatException e) {
                    return null;
                } catch (PatternSyntaxException e2) {
                    return null;
                }
            }
        }
        return arrayList;
    }

    protected void onCarrierConfigChanged() {
        PersistableBundle configForSubId = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
        if (configForSubId != null) {
            updateLteEarfcnLists(configForSubId);
            updateReportingCriteria(configForSubId);
        }
    }

    protected void updateLteEarfcnLists(PersistableBundle persistableBundle) {
        synchronized (this.mLteRsrpBoostLock) {
            this.mLteRsrpBoost = persistableBundle.getInt("lte_earfcns_rsrp_boost_int", 0);
            this.mEarfcnPairListForRsrpBoost = convertEarfcnStringArrayToPairList(persistableBundle.getStringArray("boosted_lte_earfcns_string_array"));
        }
    }

    private void updateReportingCriteria(PersistableBundle persistableBundle) {
        this.mPhone.setSignalStrengthReportingCriteria(persistableBundle.getIntArray("lte_rsrp_thresholds_int_array"), 3);
        this.mPhone.setSignalStrengthReportingCriteria(persistableBundle.getIntArray("wcdma_rscp_thresholds_int_array"), 2);
    }

    protected void updateServiceStateLteEarfcnBoost(ServiceState serviceState, int i) {
        synchronized (this.mLteRsrpBoostLock) {
            if (i != -1) {
                try {
                    if (containsEarfcnInEarfcnRange(this.mEarfcnPairListForRsrpBoost, i)) {
                        serviceState.setLteEarfcnRsrpBoost(this.mLteRsrpBoost);
                    } else {
                        serviceState.setLteEarfcnRsrpBoost(0);
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        }
    }

    protected boolean onSignalStrengthResult(AsyncResult asyncResult) {
        boolean z;
        int rilDataRadioTechnology = this.mSS.getRilDataRadioTechnology();
        int rilVoiceRadioTechnology = this.mSS.getRilVoiceRadioTechnology();
        if ((rilDataRadioTechnology != 18 && ServiceState.isGsm(rilDataRadioTechnology)) || (rilVoiceRadioTechnology != 18 && ServiceState.isGsm(rilVoiceRadioTechnology))) {
            z = true;
        } else {
            z = false;
        }
        if (asyncResult.exception == null && asyncResult.result != null) {
            this.mSignalStrength = (SignalStrength) asyncResult.result;
            this.mSignalStrength.validateInput();
            if (rilDataRadioTechnology == 0 && rilVoiceRadioTechnology == 0) {
                this.mSignalStrength.fixType();
            } else {
                this.mSignalStrength.setGsm(z);
            }
            this.mSignalStrength.setLteRsrpBoost(this.mSS.getLteEarfcnRsrpBoost());
            PersistableBundle carrierConfig = getCarrierConfig();
            this.mSignalStrength.setUseOnlyRsrpForLteLevel(carrierConfig.getBoolean("use_only_rsrp_for_lte_signal_bar_bool"));
            this.mSignalStrength.setLteRsrpThresholds(carrierConfig.getIntArray("lte_rsrp_thresholds_int_array"));
            this.mSignalStrength.setWcdmaDefaultSignalMeasurement(carrierConfig.getString("wcdma_default_signal_strength_measurement_string"));
            this.mSignalStrength.setWcdmaRscpThresholds(carrierConfig.getIntArray("wcdma_rscp_thresholds_int_array"));
        } else {
            log("onSignalStrengthResult() Exception from RIL : " + asyncResult.exception);
            this.mSignalStrength = new SignalStrength(z);
        }
        return notifySignalStrength();
    }

    protected void hangupAndPowerOff() {
        if (!this.mPhone.isPhoneTypeGsm() || this.mPhone.isInCall()) {
            this.mPhone.mCT.mRingingCall.hangupIfAlive();
            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        this.mCi.setRadioPower(false, obtainMessage(54));
    }

    protected void cancelPollState() {
        this.mPollingContext = new int[1];
    }

    protected boolean networkCountryIsoChanged(String str, String str2) {
        if (TextUtils.isEmpty(str)) {
            log("countryIsoChanged: no new country ISO code");
            return false;
        }
        if (TextUtils.isEmpty(str2)) {
            log("countryIsoChanged: no previous country ISO code");
            return true;
        }
        return !str.equals(str2);
    }

    protected boolean iccCardExists() {
        return (this.mUiccApplcation == null || this.mUiccApplcation.getState() == IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN) ? false : true;
    }

    public String getSystemProperty(String str, String str2) {
        return TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), str, str2);
    }

    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        CellInfoResult cellInfoResult = new CellInfoResult();
        if (this.mCi.getRilVersion() >= 8) {
            if (!isCallerOnDifferentThread()) {
                log("SST.getAllCellInfo(): return last, same thread can't block");
                cellInfoResult.list = this.mLastCellInfoList;
            } else if (SystemClock.elapsedRealtime() - this.mLastCellInfoListTime > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                Message messageObtainMessage = obtainMessage(43, cellInfoResult);
                synchronized (cellInfoResult.lockObj) {
                    cellInfoResult.list = null;
                    this.mCi.getCellInfoList(messageObtainMessage, workSource);
                    try {
                        cellInfoResult.lockObj.wait(5000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                log("SST.getAllCellInfo(): return last, back to back calls");
                cellInfoResult.list = this.mLastCellInfoList;
            }
        } else {
            log("SST.getAllCellInfo(): not implemented");
            cellInfoResult.list = null;
        }
        synchronized (cellInfoResult.lockObj) {
            if (cellInfoResult.list != null) {
                return cellInfoResult.list;
            }
            log("SST.getAllCellInfo(): X size=0 list=null");
            return null;
        }
    }

    public SignalStrength getSignalStrength() {
        return this.mSignalStrength;
    }

    public void registerForSubscriptionInfoReady(Handler handler, int i, Object obj) {
        Registrant registrant = new Registrant(handler, i, obj);
        this.mCdmaForSubscriptionInfoReadyRegistrants.add(registrant);
        if (isMinInfoReady()) {
            registrant.notifyRegistrant();
        }
    }

    public void unregisterForSubscriptionInfoReady(Handler handler) {
        this.mCdmaForSubscriptionInfoReadyRegistrants.remove(handler);
    }

    private void saveCdmaSubscriptionSource(int i) {
        log("Storing cdma subscription source: " + i);
        Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", i);
        log("Read from settings: " + Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", -1));
    }

    protected void getSubscriptionInfoAndStartPollingThreads() {
        this.mCi.getCDMASubscription(obtainMessage(34));
        pollState();
    }

    protected void handleCdmaSubscriptionSource(int i) {
        log("Subscription Source : " + i);
        this.mIsSubscriptionFromRuim = i == 0;
        log("isFromRuim: " + this.mIsSubscriptionFromRuim);
        saveCdmaSubscriptionSource(i);
        if (!this.mIsSubscriptionFromRuim) {
            sendMessage(obtainMessage(35));
        }
    }

    private void dumpEarfcnPairList(PrintWriter printWriter) {
        printWriter.print(" mEarfcnPairListForRsrpBoost={");
        if (this.mEarfcnPairListForRsrpBoost != null) {
            int size = this.mEarfcnPairListForRsrpBoost.size();
            for (Pair<Integer, Integer> pair : this.mEarfcnPairListForRsrpBoost) {
                printWriter.print("(");
                printWriter.print(pair.first);
                printWriter.print(",");
                printWriter.print(pair.second);
                printWriter.print(")");
                size--;
                if (size != 0) {
                    printWriter.print(",");
                }
            }
        }
        printWriter.println("}");
    }

    private void dumpCellInfoList(PrintWriter printWriter) {
        printWriter.print(" mLastCellInfoList={");
        if (this.mLastCellInfoList != null) {
            boolean z = true;
            for (CellInfo cellInfo : this.mLastCellInfoList) {
                if (!z) {
                    printWriter.print(",");
                }
                z = false;
                printWriter.print(cellInfo.toString());
            }
        }
        printWriter.println("}");
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("ServiceStateTracker:");
        printWriter.println(" mSubId=" + this.mSubId);
        printWriter.println(" mSS=" + this.mSS);
        printWriter.println(" mNewSS=" + this.mNewSS);
        printWriter.println(" mVoiceCapable=" + this.mVoiceCapable);
        printWriter.println(" mRestrictedState=" + this.mRestrictedState);
        StringBuilder sb = new StringBuilder();
        sb.append(" mPollingContext=");
        sb.append(this.mPollingContext);
        sb.append(" - ");
        sb.append(this.mPollingContext != null ? Integer.valueOf(this.mPollingContext[0]) : "");
        printWriter.println(sb.toString());
        printWriter.println(" mDesiredPowerState=" + this.mDesiredPowerState);
        printWriter.println(" mDontPollSignalStrength=" + this.mDontPollSignalStrength);
        printWriter.println(" mSignalStrength=" + this.mSignalStrength);
        printWriter.println(" mLastSignalStrength=" + this.mLastSignalStrength);
        printWriter.println(" mRestrictedState=" + this.mRestrictedState);
        printWriter.println(" mPendingRadioPowerOffAfterDataOff=" + this.mPendingRadioPowerOffAfterDataOff);
        printWriter.println(" mPendingRadioPowerOffAfterDataOffTag=" + this.mPendingRadioPowerOffAfterDataOffTag);
        printWriter.println(" mCellLoc=" + Rlog.pii(false, this.mCellLoc));
        printWriter.println(" mNewCellLoc=" + Rlog.pii(false, this.mNewCellLoc));
        printWriter.println(" mLastCellInfoListTime=" + this.mLastCellInfoListTime);
        dumpCellInfoList(printWriter);
        printWriter.flush();
        printWriter.println(" mPreferredNetworkType=" + this.mPreferredNetworkType);
        printWriter.println(" mMaxDataCalls=" + this.mMaxDataCalls);
        printWriter.println(" mNewMaxDataCalls=" + this.mNewMaxDataCalls);
        printWriter.println(" mReasonDataDenied=" + this.mReasonDataDenied);
        printWriter.println(" mNewReasonDataDenied=" + this.mNewReasonDataDenied);
        printWriter.println(" mGsmRoaming=" + this.mGsmRoaming);
        printWriter.println(" mDataRoaming=" + this.mDataRoaming);
        printWriter.println(" mEmergencyOnly=" + this.mEmergencyOnly);
        printWriter.flush();
        this.mNitzState.dumpState(printWriter);
        printWriter.flush();
        printWriter.println(" mStartedGprsRegCheck=" + this.mStartedGprsRegCheck);
        printWriter.println(" mReportedGprsNoReg=" + this.mReportedGprsNoReg);
        printWriter.println(" mNotification=" + this.mNotification);
        printWriter.println(" mCurSpn=" + this.mCurSpn);
        printWriter.println(" mCurDataSpn=" + this.mCurDataSpn);
        printWriter.println(" mCurShowSpn=" + this.mCurShowSpn);
        printWriter.println(" mCurPlmn=" + this.mCurPlmn);
        printWriter.println(" mCurShowPlmn=" + this.mCurShowPlmn);
        printWriter.flush();
        printWriter.println(" mCurrentOtaspMode=" + this.mCurrentOtaspMode);
        printWriter.println(" mRoamingIndicator=" + this.mRoamingIndicator);
        printWriter.println(" mIsInPrl=" + this.mIsInPrl);
        printWriter.println(" mDefaultRoamingIndicator=" + this.mDefaultRoamingIndicator);
        printWriter.println(" mRegistrationState=" + this.mRegistrationState);
        printWriter.println(" mMdn=" + this.mMdn);
        printWriter.println(" mHomeSystemId=" + this.mHomeSystemId);
        printWriter.println(" mHomeNetworkId=" + this.mHomeNetworkId);
        printWriter.println(" mMin=" + this.mMin);
        printWriter.println(" mPrlVersion=" + this.mPrlVersion);
        printWriter.println(" mIsMinInfoReady=" + this.mIsMinInfoReady);
        printWriter.println(" mIsEriTextLoaded=" + this.mIsEriTextLoaded);
        printWriter.println(" mIsSubscriptionFromRuim=" + this.mIsSubscriptionFromRuim);
        printWriter.println(" mCdmaSSM=" + this.mCdmaSSM);
        printWriter.println(" mRegistrationDeniedReason=" + this.mRegistrationDeniedReason);
        printWriter.println(" mCurrentCarrier=" + this.mCurrentCarrier);
        printWriter.flush();
        printWriter.println(" mImsRegistered=" + this.mImsRegistered);
        printWriter.println(" mImsRegistrationOnOff=" + this.mImsRegistrationOnOff);
        printWriter.println(" mAlarmSwitch=" + this.mAlarmSwitch);
        printWriter.println(" mRadioDisabledByCarrier" + this.mRadioDisabledByCarrier);
        printWriter.println(" mPowerOffDelayNeed=" + this.mPowerOffDelayNeed);
        printWriter.println(" mDeviceShuttingDown=" + this.mDeviceShuttingDown);
        printWriter.println(" mSpnUpdatePending=" + this.mSpnUpdatePending);
        printWriter.println(" mLteRsrpBoost=" + this.mLteRsrpBoost);
        dumpEarfcnPairList(printWriter);
        this.mLocaleTracker.dump(fileDescriptor, printWriter, strArr);
        printWriter.println(" Roaming Log:");
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.increaseIndent();
        this.mRoamingLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println(" Attach Log:");
        indentingPrintWriter.increaseIndent();
        this.mAttachLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println(" Phone Change Log:");
        indentingPrintWriter.increaseIndent();
        this.mPhoneTypeLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println(" Rat Change Log:");
        indentingPrintWriter.increaseIndent();
        this.mRatLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println(" Radio power Log:");
        indentingPrintWriter.increaseIndent();
        this.mRadioPowerLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        this.mNitzState.dumpLogs(fileDescriptor, indentingPrintWriter, strArr);
    }

    public boolean isImsRegistered() {
        return this.mImsRegistered;
    }

    protected void checkCorrectThread() {
        if (Thread.currentThread() != getLooper().getThread()) {
            throw new RuntimeException("ServiceStateTracker must be used from within one thread");
        }
    }

    protected boolean isCallerOnDifferentThread() {
        return Thread.currentThread() != getLooper().getThread();
    }

    protected void updateCarrierMccMncConfiguration(String str, String str2, Context context) {
        if ((str == null && !TextUtils.isEmpty(str2)) || (str != null && !str.equals(str2))) {
            log("update mccmnc=" + str + " fromServiceState=true");
            MccTable.updateMccMncConfiguration(context, str, true);
        }
    }

    protected boolean inSameCountry(String str) {
        if (TextUtils.isEmpty(str) || str.length() < 5) {
            return false;
        }
        String homeOperatorNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeOperatorNumeric) || homeOperatorNumeric.length() < 5) {
            return false;
        }
        String strSubstring = str.substring(0, 3);
        String strSubstring2 = homeOperatorNumeric.substring(0, 3);
        String strCountryCodeForMcc = MccTable.countryCodeForMcc(Integer.parseInt(strSubstring));
        String strCountryCodeForMcc2 = MccTable.countryCodeForMcc(Integer.parseInt(strSubstring2));
        if (strCountryCodeForMcc.isEmpty() || strCountryCodeForMcc2.isEmpty()) {
            return false;
        }
        boolean zEquals = strCountryCodeForMcc2.equals(strCountryCodeForMcc);
        if (zEquals) {
            return zEquals;
        }
        if ("us".equals(strCountryCodeForMcc2) && "vi".equals(strCountryCodeForMcc)) {
            return true;
        }
        if ("vi".equals(strCountryCodeForMcc2) && "us".equals(strCountryCodeForMcc)) {
            return true;
        }
        return zEquals;
    }

    protected void setRoamingType(ServiceState serviceState) {
        boolean z = serviceState.getVoiceRegState() == 0;
        if (z) {
            if (serviceState.getVoiceRoaming()) {
                if (this.mPhone.isPhoneTypeGsm()) {
                    if (inSameCountry(serviceState.getVoiceOperatorNumeric())) {
                        serviceState.setVoiceRoamingType(2);
                    } else {
                        serviceState.setVoiceRoamingType(3);
                    }
                } else {
                    int[] intArray = this.mPhone.getContext().getResources().getIntArray(R.array.config_autoKeyboardBacklightDecreaseLuxThreshold);
                    if (intArray != null && intArray.length > 0) {
                        serviceState.setVoiceRoamingType(2);
                        int cdmaRoamingIndicator = serviceState.getCdmaRoamingIndicator();
                        int i = 0;
                        while (true) {
                            if (i >= intArray.length) {
                                break;
                            }
                            if (cdmaRoamingIndicator != intArray[i]) {
                                i++;
                            } else {
                                serviceState.setVoiceRoamingType(3);
                                break;
                            }
                        }
                    } else if (inSameCountry(serviceState.getVoiceOperatorNumeric())) {
                        serviceState.setVoiceRoamingType(2);
                    } else {
                        serviceState.setVoiceRoamingType(3);
                    }
                }
            } else {
                serviceState.setVoiceRoamingType(0);
            }
        }
        boolean z2 = serviceState.getDataRegState() == 0;
        int rilDataRadioTechnology = serviceState.getRilDataRadioTechnology();
        if (z2) {
            if (!serviceState.getDataRoaming()) {
                serviceState.setDataRoamingType(0);
                return;
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                if (ServiceState.isGsm(rilDataRadioTechnology)) {
                    if (z) {
                        serviceState.setDataRoamingType(serviceState.getVoiceRoamingType());
                        return;
                    } else {
                        serviceState.setDataRoamingType(1);
                        return;
                    }
                }
                serviceState.setDataRoamingType(1);
                return;
            }
            if (ServiceState.isCdma(rilDataRadioTechnology)) {
                if (z) {
                    serviceState.setDataRoamingType(serviceState.getVoiceRoamingType());
                    return;
                } else {
                    serviceState.setDataRoamingType(1);
                    return;
                }
            }
            if (inSameCountry(serviceState.getDataOperatorNumeric())) {
                serviceState.setDataRoamingType(2);
            } else {
                serviceState.setDataRoamingType(3);
            }
        }
    }

    protected void setSignalStrengthDefaultValues() {
        this.mSignalStrength = new SignalStrength(true);
    }

    protected String getHomeOperatorNumeric() {
        String simOperatorNumericForPhone = ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
        if (!this.mPhone.isPhoneTypeGsm() && TextUtils.isEmpty(simOperatorNumericForPhone)) {
            return SystemProperties.get(GsmCdmaPhone.PROPERTY_CDMA_HOME_OPERATOR_NUMERIC, "");
        }
        return simOperatorNumericForPhone;
    }

    protected int getPhoneId() {
        return this.mPhone.getPhoneId();
    }

    protected void resetServiceStateInIwlanMode() {
        boolean z;
        if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            log("set service state as POWER_OFF");
            if (18 == this.mNewSS.getRilDataRadioTechnology()) {
                log("pollStateDone: mNewSS = " + this.mNewSS);
                log("pollStateDone: reset iwlan RAT value");
                z = true;
            } else {
                z = false;
            }
            String operatorAlphaLong = this.mNewSS.getOperatorAlphaLong();
            this.mNewSS.setStateOff();
            if (z) {
                this.mNewSS.setRilDataRadioTechnology(18);
                this.mNewSS.setDataRegState(0);
                this.mNewSS.setOperatorAlphaLong(operatorAlphaLong);
                log("pollStateDone: mNewSS = " + this.mNewSS);
            }
        }
    }

    protected final boolean alwaysOnHomeNetwork(BaseBundle baseBundle) {
        return baseBundle.getBoolean("force_home_network_bool");
    }

    private boolean isInNetwork(BaseBundle baseBundle, String str, String str2) {
        String[] stringArray = baseBundle.getStringArray(str2);
        if (stringArray != null && Arrays.asList(stringArray).contains(str)) {
            return true;
        }
        return false;
    }

    protected final boolean isRoamingInGsmNetwork(BaseBundle baseBundle, String str) {
        return isInNetwork(baseBundle, str, "gsm_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInGsmNetwork(BaseBundle baseBundle, String str) {
        return isInNetwork(baseBundle, str, "gsm_nonroaming_networks_string_array");
    }

    protected final boolean isRoamingInCdmaNetwork(BaseBundle baseBundle, String str) {
        return isInNetwork(baseBundle, str, "cdma_roaming_networks_string_array");
    }

    protected final boolean isNonRoamingInCdmaNetwork(BaseBundle baseBundle, String str) {
        return isInNetwork(baseBundle, str, "cdma_nonroaming_networks_string_array");
    }

    public boolean isDeviceShuttingDown() {
        return this.mDeviceShuttingDown;
    }

    protected int getCombinedRegState() {
        int voiceRegState = this.mSS.getVoiceRegState();
        int dataRegState = this.mSS.getDataRegState();
        if ((voiceRegState != 1 && voiceRegState != 3) || dataRegState != 0) {
            return voiceRegState;
        }
        log("getCombinedRegState: return STATE_IN_SERVICE as Data is in service");
        return dataRegState;
    }

    protected PersistableBundle getCarrierConfig() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null && (configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId())) != null) {
            return configForSubId;
        }
        return CarrierConfigManager.getDefaultConfig();
    }

    protected void mtkIvsrUpdateCsPlmn() {
    }

    protected void mtkIvsrUpdatePsPlmn() {
    }

    protected int mtkReplaceDdsIfUnset(int i) {
        return i;
    }

    protected void mtkRegisterAllDataDisconnected() {
    }

    protected int mtkReplaceDisconnectTimer() {
        return 30000;
    }

    public LocaleTracker getLocaleTracker() {
        return this.mLocaleTracker;
    }
}
