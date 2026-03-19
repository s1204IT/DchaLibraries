package com.mediatek.internal.telephony.dataconnection;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.hardware.radio.V1_0.DataCallFailCause;
import android.net.ConnectivityManager;
import android.net.NetworkConfig;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
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
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.data.DataProfile;
import android.text.TextUtils;
import android.util.LocalLog;
import android.util.Log;
import android.util.Pair;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.DctConstants;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;
import com.android.internal.telephony.TelephonyDevController;
import com.android.internal.telephony.dataconnection.ApnContext;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.telephony.dataconnection.DcAsyncChannel;
import com.android.internal.telephony.dataconnection.DcFailCause;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.RuimRecords;
import com.android.internal.telephony.uicc.SIMRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.util.ArrayUtils;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import com.mediatek.internal.telephony.MtkGsmCdmaPhone;
import com.mediatek.internal.telephony.MtkHardwareConfig;
import com.mediatek.internal.telephony.MtkIccCardConstants;
import com.mediatek.internal.telephony.MtkServiceStateTracker;
import com.mediatek.internal.telephony.MtkSubscriptionController;
import com.mediatek.internal.telephony.OpTelephonyCustomizationFactoryBase;
import com.mediatek.internal.telephony.OpTelephonyCustomizationUtils;
import com.mediatek.internal.telephony.dataconnection.MtkDcHelper;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import com.mediatek.internal.telephony.ims.MtkDedicateDataCallResponse;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.MtkIccUtilsEx;
import com.mediatek.internal.telephony.uicc.MtkUiccCardApplication;
import com.mediatek.internal.telephony.uicc.MtkUiccController;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimConstants;
import com.mediatek.telephony.internal.telephony.vsim.ExternalSimManager;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

public class MtkDcTracker extends DcTracker {
    private static final int APN_CLASS_0 = 0;
    private static final int APN_CLASS_1 = 1;
    private static final int APN_CLASS_2 = 2;
    private static final int APN_CLASS_3 = 3;
    private static final int APN_CLASS_4 = 4;
    private static final int APN_CLASS_5 = 5;
    private static final boolean DBG = true;
    private static final int DEFAULT_DATA_SIM_IDX = 2;
    private static final int DOMESTIC_DATA_ROAMING_IDX = 3;
    private static final String FDN_CONTENT_URI = "content://icc/fdn";
    private static final String FDN_CONTENT_URI_WITH_SUB_ID = "content://icc/fdn/subId/";
    private static final String FDN_FOR_ALLOW_DATA = "*99#";
    private static final String GID1_DEFAULT = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private static final int INTERNATIONAL_DATA_ROAMING_IDX = 4;
    private static final String[] KDDI_OPERATOR;
    private static final int LTE_AS_CONNECTED = 1;
    protected static final String[] MCC_TABLE_DOMESTIC;
    protected static final String[] MCC_TABLE_TEST;
    private static final int MOBILE_DATA_IDX = 0;
    protected static boolean MTK_CC33_SUPPORT = false;
    protected static final boolean MTK_IMS_TESTMODE_SUPPORT;
    private static final String NETWORK_TYPE_MOBILE_IMS = "MOBILEIMS";
    private static final String NETWORK_TYPE_WIFI = "WIFI";
    private static final String[] PRIVATE_APN_OPERATOR;
    private static final String PROP_APN_CLASS = "vendor.ril.md_changed_apn_class.";
    private static final String PROP_APN_CLASS_ICCID = "vendor.ril.md_changed_apn_class.iccid.";
    private static final String PROP_RIL_DATA_CDMA_IMSI = "vendor.ril.data.cdma_imsi";
    private static final String PROP_RIL_DATA_CDMA_MCC_MNC = "vendor.ril.data.cdma_mcc_mnc";
    private static final String PROP_RIL_DATA_CDMA_SPN = "vendor.ril.data.cdma_spn";
    private static final String PROP_RIL_DATA_GID1 = "vendor.ril.data.gid1-";
    private static final String PROP_RIL_DATA_GSM_IMSI = "vendor.ril.data.gsm_imsi";
    private static final String PROP_RIL_DATA_GSM_MCC_MNC = "vendor.ril.data.gsm_mcc_mnc";
    private static final String PROP_RIL_DATA_GSM_SPN = "vendor.ril.data.gsm_spn";
    private static final String PROP_RIL_DATA_PNN = "vendor.ril.data.pnn";
    protected static final int REGION_DOMESTIC = 1;
    protected static final int REGION_FOREIGN = 2;
    protected static final int REGION_UNKNOWN = 0;
    private static final int ROAMING_DATA_IDX = 1;
    private static final int SKIP_DATA_SETTINGS = -2;
    private static final String SKIP_DATA_STALL_ALARM = "persist.vendor.skip.data.stall.alarm";
    private static final String SPRINT_IA_NI = "otasn";
    private static final int THROTTLING_MAX_PDP_SIZE = 8;
    private static final String VZW_800_NI = "VZW800";
    private static final String VZW_ADMIN_NI = "VZWADMIN";
    private static final String VZW_APP_NI = "VZWAPP";
    private static final String VZW_EMERGENCY_NI = "VZWEMERGENCY";
    private static final String VZW_IMS_NI = "VZWIMS";
    private static final String VZW_INTERNET_NI = "VZWINTERNET";
    private String[] MCCMNC_OP18;
    private String[] MCCMNC_OP19;
    private String[] PLMN_EMPTY_APN_PCSCF_SET;
    private String[] PROPERTY_ICCID;
    private boolean mAllowConfig;
    private boolean mCcDomesticRoamingEnabled;
    private String[] mCcDomesticRoamingSpecifiedNw;
    private boolean mCcIntlRoamingEnabled;
    private boolean mCcUniqueSettingsForRoaming;
    private IDataConnectionExt mDataConnectionExt;
    private HandlerThread mDcHandlerThread;
    private int mDedicatedBearerCount;
    private int mDefaultRefCount;
    private boolean mDomesticRoamingsettings;
    private boolean mHasFetchMdAutoSetupImsCapability;
    private boolean mHasFetchModemDeactPdnCapabilityForMultiPS;
    private boolean mHasPsEverAttached;
    private ContentObserver mImsSwitchChangeObserver;
    protected ApnSetting mInitialAttachApnSetting;
    private final BroadcastReceiver mIntentReceiverEx;
    private boolean mInternationalRoamingsettings;
    private boolean mIsAddMnoApnsIntoAllApnList;
    private boolean mIsFdnChecked;
    private boolean mIsImsHandover;
    private boolean mIsLte;
    private boolean mIsMatchFdnForAllowData;
    private boolean mIsOp19Sim;
    private boolean mIsPhbStateChangedIntentRegistered;
    private boolean mIsSharedDefaultApn;
    private boolean mIsSupportConcurrent;
    private String mLteAccessStratumDataState;
    private boolean mMdAutoSetupImsCapability;
    private ApnSetting mMdChangedAttachApn;
    private boolean mModemDeactPdnCapabilityForMultiPS;
    protected boolean mNeedsResumeModem;
    protected Object mNeedsResumeModemLock;
    private int mNetworkType;
    private boolean mPendingDataCall;
    private BroadcastReceiver mPhbStateChangedIntentReceiver;
    private int mPhoneType;
    public ArrayList<ApnContext> mPrioritySortedApnContextsEx;
    private int mRefCount;
    protected int mRegion;
    private int mRilRat;
    protected int mSuspendId;
    private TelephonyDevController mTelDevController;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    private AtomicReference<UiccCardApplication> mUiccCardApplication;
    private Handler mWorkerHandler;
    private static final String LOG_TAG = "MtkDCT";
    private static final boolean VDBG = Log.isLoggable(LOG_TAG, 3);
    private static final boolean VDBG_STALL = Log.isLoggable(LOG_TAG, 3);

    static {
        MTK_CC33_SUPPORT = SystemProperties.getInt("persist.vendor.data.cc33.support", 0) == 1;
        MTK_IMS_TESTMODE_SUPPORT = SystemProperties.getInt("persist.vendor.radio.imstestmode", 0) == 1;
        MCC_TABLE_TEST = new String[]{"001"};
        MCC_TABLE_DOMESTIC = new String[]{"440"};
        PRIVATE_APN_OPERATOR = new String[]{"732101", "330110", "334020", "71610", "74001", "71403", "73003", "72405", "722310", "37002", "71203", "70401", "70601", "708001", "71021", "74810", "74402"};
        KDDI_OPERATOR = new String[]{"44007", "44008", "44050", "44051", "44052", "44053", "44054", "44055", "44056", "44070", "44071", "44072", "44073", "44074", "44075", "44076", "44077", "44078", "44079", "44088", "44089", "44170"};
    }

    public MtkDcTracker(Phone phone, int i) {
        super(phone, i);
        this.mTelephonyCustomizationFactory = null;
        this.mDataConnectionExt = null;
        this.mDedicatedBearerCount = 0;
        this.PROPERTY_ICCID = new String[]{"vendor.ril.iccid.sim1", "vendor.ril.iccid.sim2", "vendor.ril.iccid.sim3", "vendor.ril.iccid.sim4"};
        this.mLteAccessStratumDataState = "unknown";
        this.mNetworkType = -1;
        this.mIsLte = false;
        this.mIsSharedDefaultApn = false;
        this.mDefaultRefCount = 0;
        this.mAllowConfig = false;
        this.mHasFetchModemDeactPdnCapabilityForMultiPS = false;
        this.mModemDeactPdnCapabilityForMultiPS = false;
        this.mHasFetchMdAutoSetupImsCapability = false;
        this.mMdAutoSetupImsCapability = false;
        this.mIsSupportConcurrent = false;
        this.mDomesticRoamingsettings = false;
        this.mInternationalRoamingsettings = false;
        this.mIsImsHandover = false;
        this.mMdChangedAttachApn = null;
        this.mSuspendId = 0;
        this.mRegion = 0;
        this.mNeedsResumeModemLock = new Object();
        this.mNeedsResumeModem = false;
        this.PLMN_EMPTY_APN_PCSCF_SET = new String[]{"26201", "44010"};
        this.mImsSwitchChangeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean z) {
                MtkDcTracker.this.log("mImsSwitchChangeObserver: onChange=" + z);
                if (MtkDcTracker.this.isOp17IaSupport()) {
                    MtkDcTracker.this.log("IA : OP17, set IA");
                    MtkDcTracker.this.setInitialAttachApn();
                }
            }
        };
        this.MCCMNC_OP18 = new String[]{"405840", "405854", "405855", "405856", "405857", "405858", "405859", "405860", "405861", "405862", "405863", "405864", "405865", "405866", "405867", "405868", "405869", "405870", "405871", "405872", "405873", "405874"};
        this.mTelDevController = TelephonyDevController.getInstance();
        this.mCcDomesticRoamingEnabled = false;
        this.mCcDomesticRoamingSpecifiedNw = null;
        this.mCcIntlRoamingEnabled = false;
        this.mCcUniqueSettingsForRoaming = false;
        this.mIsAddMnoApnsIntoAllApnList = false;
        this.mHasPsEverAttached = false;
        this.mPhoneType = 1;
        this.MCCMNC_OP19 = new String[]{"50501"};
        this.mRefCount = 0;
        this.mIsOp19Sim = false;
        this.mRilRat = Integer.MAX_VALUE;
        this.mIntentReceiverEx = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                MtkDcTracker.this.log("mIntentReceiverEx onReceive: action=" + action);
                if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    int intExtra = intent.getIntExtra("subscription", -1);
                    MtkDcTracker.this.log("mIntentReceiverEx ACTION_CARRIER_CONFIG_CHANGED: subId=" + intExtra + ", mPhone.getSubId()=" + MtkDcTracker.this.mPhone.getSubId());
                    if (intExtra == MtkDcTracker.this.mPhone.getSubId()) {
                        MtkDcTracker.this.log("CarrierConfigLoader is loading complete!");
                        MtkDcTracker.this.sendMessage(MtkDcTracker.this.obtainMessage(270854));
                        MtkDcTracker.this.loadCarrierConfig(intExtra);
                        return;
                    }
                    return;
                }
                if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                    if (!MtkDcTracker.this.hasOperatorIaCapability()) {
                        NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                        int type = networkInfo.getType();
                        String typeName = networkInfo.getTypeName();
                        MtkDcTracker.this.logd("onReceive: ConnectivityService action change apnType = " + type + " typename =" + typeName);
                        if (type == 11 && typeName.equals(MtkDcTracker.NETWORK_TYPE_WIFI)) {
                            MtkDcTracker.this.onAttachApnChangedByHandover(true);
                            return;
                        } else {
                            if (type == 11 && typeName.equals(MtkDcTracker.NETWORK_TYPE_MOBILE_IMS)) {
                                MtkDcTracker.this.onAttachApnChangedByHandover(false);
                                return;
                            }
                            return;
                        }
                    }
                    return;
                }
                if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                    MtkDcTracker.this.reloadOpCustomizationFactory();
                    return;
                }
                MtkDcTracker.this.log("onReceive: Unknown action=" + action);
            }
        };
        this.mIsFdnChecked = false;
        this.mIsMatchFdnForAllowData = false;
        this.mIsPhbStateChangedIntentRegistered = false;
        this.mPhbStateChangedIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                MtkDcTracker.this.log("onReceive: action=" + action);
                if (action.equals("mediatek.intent.action.PHB_STATE_CHANGED")) {
                    boolean booleanExtra = intent.getBooleanExtra("ready", false);
                    MtkDcTracker.this.log("bPhbReady: " + booleanExtra);
                    if (booleanExtra) {
                        MtkDcTracker.this.onFdnChanged();
                    }
                }
            }
        };
        this.mPendingDataCall = false;
        reloadOpCustomizationFactory();
        if (!hasOperatorIaCapability()) {
            phone.getContext().getContentResolver().registerContentObserver(Settings.Global.getUriFor("volte_vt_enabled"), true, this.mImsSwitchChangeObserver);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
        this.mPhone.getContext().registerReceiver(this.mIntentReceiverEx, intentFilter, null, this.mPhone);
        if (TelephonyManager.getDefault().getPhoneCount() == 1) {
            this.mAllowConfig = true;
        }
        createWorkerHandler();
    }

    public void registerServiceStateTrackerEvents() {
        super.registerServiceStateTrackerEvents();
        ((MtkServiceStateTracker) this.mPhone.getServiceStateTracker()).registerForDataRoamingTypeChange(this, 270850, null);
    }

    public void unregisterServiceStateTrackerEvents() {
        super.unregisterServiceStateTrackerEvents();
        ((MtkServiceStateTracker) this.mPhone.getServiceStateTracker()).unregisterForDataRoamingTypeChange(this);
    }

    protected void registerForAllEvents() {
        super.registerForAllEvents();
        this.mPhone.getCallTracker().unregisterForVoiceCallEnded(this);
        this.mPhone.getCallTracker().unregisterForVoiceCallStarted(this);
        logd("registerForAllEvents: mPhone = " + this.mPhone);
        this.mPhone.mCi.registerForRemoveRestrictEutran(this, 270852, null);
        this.mPhone.mCi.registerForMdDataRetryCountReset(this, 270851, null);
        if (!hasOperatorIaCapability()) {
            if (!WorldPhoneUtil.isWorldPhoneSupport() && !DataSubConstants.OPERATOR_OP01.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR))) {
                this.mPhone.mCi.setOnPlmnChangeNotification(this, 270844, null);
                this.mPhone.mCi.setOnRegistrationSuspended(this, 270845, null);
            }
            this.mPhone.mCi.registerForResetAttachApn(this, 270847, null);
            this.mPhone.mCi.registerForRilConnected(this, 270861, (Object) null);
            this.mPhone.mCi.registerForAttachApnChanged(this, 270843, null);
        }
        this.mPhone.mCi.registerForLteAccessStratumState(this, 270849, null);
        this.mPhone.mCi.registerForDataAllowed(this, 270853, null);
        this.mPhone.mCi.registerForPcoDataAfterAttached(this, 270856, null);
        this.mPhone.mCi.registerForDedicatedBearerActivated(this, 270857, null);
        this.mPhone.mCi.registerForDedicatedBearerModified(this, 270858, null);
        this.mPhone.mCi.registerForDedicatedBearerDeactivationed(this, 270859, null);
        registerForDataEnabledChanged(this, 270855, null);
        this.mPhone.mCi.registerForNetworkReject(this, 270862, null);
    }

    protected void registerSettingsObserver() {
        super.registerSettingsObserver();
        this.mSettingsObserver.observe(Settings.Global.getUriFor("domestic_data_roaming" + Integer.toString(this.mPhone.getSubId())), 270384);
        this.mSettingsObserver.observe(Settings.Global.getUriFor("international_data_roaming" + Integer.toString(this.mPhone.getSubId())), 270384);
        registerFdnContentObserver();
    }

    public void dispose() {
        super.dispose();
        if (this.mDataConnectionExt != null) {
            this.mDataConnectionExt.stopDataRoamingStrategy();
        }
        this.mPrioritySortedApnContextsEx.clear();
        this.mPhone.getContext().getContentResolver().unregisterContentObserver(this.mImsSwitchChangeObserver);
        this.mPhone.getContext().unregisterReceiver(this.mIntentReceiverEx);
        if (this.mDcHandlerThread != null) {
            this.mDcHandlerThread.quitSafely();
            this.mDcHandlerThread = null;
        }
        this.mIsFdnChecked = false;
        this.mIsMatchFdnForAllowData = false;
        this.mIsPhbStateChangedIntentRegistered = false;
        this.mPhone.getContext().unregisterReceiver(this.mPhbStateChangedIntentReceiver);
        if (this.mWorkerHandler != null) {
            this.mWorkerHandler.getLooper().quit();
        }
        this.mRefCount = 0;
    }

    protected void unregisterForAllEvents() {
        super.unregisterForAllEvents();
        logd("unregisterForAllEvents: mPhone = " + this.mPhone);
        this.mPhone.mCi.unregisterForRemoveRestrictEutran(this);
        this.mPhone.mCi.unregisterForMdDataRetryCountReset(this);
        if (!hasOperatorIaCapability()) {
            if (!WorldPhoneUtil.isWorldPhoneSupport() && !DataSubConstants.OPERATOR_OP01.equals(SystemProperties.get(DataSubConstants.PROPERTY_OPERATOR_OPTR))) {
                this.mPhone.mCi.unSetOnPlmnChangeNotification(this);
                this.mPhone.mCi.unSetOnRegistrationSuspended(this);
            }
            this.mPhone.mCi.unregisterForResetAttachApn(this);
            this.mPhone.mCi.unregisterForRilConnected(this);
            this.mPhone.mCi.unregisterForAttachApnChanged(this);
        }
        this.mPhone.mCi.unregisterForLteAccessStratumState(this);
        this.mPhone.mCi.unregisterForDataAllowed(this);
        unregisterForDataEnabledChanged(this);
        this.mPhone.mCi.unregisterForPcoDataAfterAttached(this);
        this.mPhone.mCi.unregisterForDedicatedBearerActivated(this);
        this.mPhone.mCi.unregisterForDedicatedBearerModified(this);
        this.mPhone.mCi.unregisterForDedicatedBearerDeactivationed(this);
        this.mPhone.mCi.unregisterForNetworkReject(this);
    }

    protected void onSetUserDataEnabled(boolean z) {
        if (this.mDataEnabledSettings.isUserDataEnabled() != z) {
            this.mDataEnabledSettings.setUserDataEnabled(z);
            syncDataSettingsToMd(new int[]{z ? 1 : 0, -2, -2, -2, -2});
        }
    }

    public void setDataRoamingEnabledByUser(boolean z) {
        if (getDataRoamingEnabled() != z) {
            syncDataSettingsToMd(new int[]{-2, z ? 1 : 0, -2, -2, -2});
        }
        super.setDataRoamingEnabledByUser(z);
    }

    public void releaseNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        int iApnIdForNetworkRequest = MtkApnContext.apnIdForNetworkRequest(networkRequest);
        MtkApnContext mtkApnContext = (MtkApnContext) this.mApnContextsById.get(iApnIdForNetworkRequest);
        localLog.log("DcTracker.releaseNetwork for " + networkRequest + " found " + mtkApnContext);
        if (mtkApnContext != null) {
            mtkApnContext.releaseNetwork(networkRequest, localLog);
            if (iApnIdForNetworkRequest == 0 && this.mIsOp19Sim) {
                this.mRefCount--;
                log("releaseNetwork for default apn, mRefCount =" + this.mRefCount);
                if (this.mRefCount < 0) {
                    this.mRefCount = 0;
                }
            }
        }
    }

    protected ApnContext addApnContext(String str, NetworkConfig networkConfig) {
        MtkApnContext mtkApnContext = new MtkApnContext(this.mPhone, str, LOG_TAG, networkConfig, this);
        this.mApnContexts.put(str, mtkApnContext);
        this.mApnContextsById.put(ApnContext.apnIdForApnName(str), mtkApnContext);
        this.mPrioritySortedApnContextsEx.add(mtkApnContext);
        return mtkApnContext;
    }

    protected void initApnContexts() {
        ApnContext apnContextAddApnContext;
        log("initApnContexts: E");
        if (this.mPrioritySortedApnContextsEx == null) {
            this.mPrioritySortedApnContextsEx = new ArrayList<>();
        }
        for (String str : this.mPhone.getContext().getResources().getStringArray(R.array.config_displayWhiteBalanceHighLightAmbientBrightnesses)) {
            NetworkConfig networkConfig = new NetworkConfig(str);
            int i = networkConfig.type;
            if (i == 0) {
                apnContextAddApnContext = addApnContext("default", networkConfig);
            } else if (i != 21) {
                switch (i) {
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
                    default:
                        switch (i) {
                            case 10:
                                apnContextAddApnContext = addApnContext("fota", networkConfig);
                                break;
                            case 11:
                                apnContextAddApnContext = addApnContext("ims", networkConfig);
                                break;
                            case 12:
                                apnContextAddApnContext = addApnContext("cbs", networkConfig);
                                break;
                            default:
                                switch (i) {
                                    case 14:
                                        apnContextAddApnContext = addApnContext("ia", networkConfig);
                                        break;
                                    case 15:
                                        apnContextAddApnContext = addApnContext("emergency", networkConfig);
                                        break;
                                    default:
                                        switch (i) {
                                            case 25:
                                                apnContextAddApnContext = addApnContext("xcap", networkConfig);
                                                break;
                                            case 26:
                                                apnContextAddApnContext = addApnContext("rcs", networkConfig);
                                                break;
                                            case 27:
                                                apnContextAddApnContext = addApnContext("bip", networkConfig);
                                                break;
                                            case 28:
                                                apnContextAddApnContext = addApnContext("vsim", networkConfig);
                                                break;
                                            case 29:
                                                apnContextAddApnContext = addApnContext("preempt", networkConfig);
                                                break;
                                            default:
                                                log("initApnContexts: skipping unknown type=" + networkConfig.type);
                                                continue;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
            } else {
                apnContextAddApnContext = addApnContext("wap", networkConfig);
            }
            log("initApnContexts: apnContext=" + apnContextAddApnContext);
        }
        Collections.sort(this.mPrioritySortedApnContextsEx, new Comparator<ApnContext>() {
            @Override
            public int compare(ApnContext apnContext, ApnContext apnContext2) {
                return apnContext2.priority - apnContext.priority;
            }
        });
        logd("initApnContexts: mPrioritySortedApnContextsEx=" + this.mPrioritySortedApnContextsEx);
        if (VDBG) {
            log("initApnContexts: X mApnContexts=" + this.mApnContexts);
        }
    }

    protected void onDataConnectionDetached() {
        ApnSetting apnSetting;
        super.onDataConnectionDetached();
        if (this.mAutoAttachOnCreationConfig) {
            this.mAutoAttachOnCreation.set(false);
        }
        if (MTK_CC33_SUPPORT && !getAutoAttachOnCreation()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                cancelReconnectAlarm(apnContext);
                if (TextUtils.equals(apnContext.getApnType(), "default") && (apnSetting = apnContext.getApnSetting()) != null) {
                    apnSetting.permanentFailed = false;
                    log("set permanentFailed as false for default apn");
                }
            }
            return;
        }
        if (this.mIsOp19Sim) {
            this.mRefCount = 0;
        }
    }

    protected void onDataConnectionAttached() {
        if (!this.mHasPsEverAttached) {
            logi("onDataConnectionAttached: optimization done");
            this.mHasPsEverAttached = true;
        }
        super.onDataConnectionAttached();
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
        boolean z2 = (apnContext != null && (TextUtils.equals(apnContext.getApnType(), "default") || TextUtils.equals(apnContext.getApnType(), "ims"))) || (this.mIccRecords.get() != null && ((IccRecords) this.mIccRecords.get()).getRecordsLoaded());
        if (z2 && MtkDcHelper.isCdma3GCard(this.mPhone.getPhoneId())) {
            if (this.mIccRecords == null) {
                logd("isDataAllowed: icc records is null.");
                z2 = false;
            }
            IccRecords iccRecords = (IccRecords) this.mIccRecords.get();
            boolean roaming = ((MtkServiceStateTracker) this.mPhone.getServiceStateTracker()).mSS.getRoaming();
            int defaultDataSubId = SubscriptionController.getInstance().getDefaultDataSubId();
            log("isDataAllowed: , current sub=" + this.mPhone.getSubId() + ", default sub=" + defaultDataSubId + ", isRoaming=" + roaming + ", icc records=" + this.mIccRecords);
            if (this.mPhone.getSubId() == defaultDataSubId && !roaming && iccRecords != null && ((iccRecords instanceof SIMRecords) || ((iccRecords instanceof RuimRecords) && !iccRecords.isLoaded()))) {
                z2 = false;
            }
        }
        boolean zIsValidSubscriptionId = SubscriptionManager.isValidSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean z3 = apnContext == null || MtkApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone);
        boolean zIsFdnEnabled = isFdnEnabled();
        PhoneConstants.State state = PhoneConstants.State.IDLE;
        if (this.mPhone.getCallTracker() != null) {
            this.mPhone.getCallTracker().getState();
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
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        if (mtkDcHelper != null && !mtkDcHelper.isDataAllowedForConcurrent(this.mPhone.getPhoneId())) {
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
        isDataAllowedForRoamingFeature(dataConnectionReasons2);
        if (this.mIsPsRestricted) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.PS_RESTRICTED);
        }
        if (!desiredPowerState) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.UNDESIRED_POWER_STATE);
        }
        if (!powerStateFromCarrier) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.RADIO_DISABLED_BY_CARRIER);
        }
        if (apnContext != null && !this.mDataEnabledSettings.isDataEnabled() && (!isDataAllowedAsOff(apnContext.getApnType()) || !this.mDataEnabledSettings.isPolicyDataEnabled())) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.DATA_DISABLED);
        }
        if (zIsFdnEnabled) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.MTK_FDN_ENABLED);
        }
        if (!getAllowConfig()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.MTK_NOT_ALLOWED);
        }
        if (MtkUiccController.getVsimCardType(this.mPhone.getPhoneId()).isAllowOnlyVsimNetwork()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.MTK_NON_VSIM_PDN_NOT_ALLOWED);
        }
        if (apnContext != null && apnContext.getApnType().equals("preempt") && !this.mPhone.isDuringImsCall()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.MTK_PREEMPT_PDN_NOT_ALLOWED);
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
        if (apnContext != null && dataConnectionReasons2.allowed() && isLocatedPlmnChanged()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataDisallowedReasonType.MTK_LOCATED_PLMN_CHANGED);
        }
        if (dataConnectionReasons2.allowed()) {
            dataConnectionReasons2.add(DataConnectionReasons.DataAllowedReasonType.NORMAL);
        }
        if (dataConnectionReasons != null) {
            dataConnectionReasons.copyFrom(dataConnectionReasons2);
        }
        return dataConnectionReasons2.allowed();
    }

    protected void setupDataOnConnectableApns(String str, DcTracker.RetryFailures retryFailures) {
        if (VDBG) {
            log("setupDataOnConnectableApns: " + str);
        }
        if (!VDBG) {
            StringBuilder sb = new StringBuilder(DataCallFailCause.IFACE_AND_POL_FAMILY_MISMATCH);
            for (ApnContext apnContext : this.mPrioritySortedApnContextsEx) {
                sb.append(apnContext.getApnType());
                sb.append(":[state=");
                sb.append(apnContext.getState());
                sb.append(",enabled=");
                sb.append(apnContext.isEnabled());
                sb.append("] ");
            }
            log("setupDataOnConnectableApns: " + str + " " + ((Object) sb));
        }
        for (ApnContext apnContext2 : this.mPrioritySortedApnContextsEx) {
            if (VDBG) {
                log("setupDataOnConnectableApns: apnContext " + apnContext2);
            }
            if (TextUtils.equals(apnContext2.getApnType(), "default") && TextUtils.equals(str, MtkGsmCdmaPhone.REASON_CARRIER_CONFIG_LOADED) && this.mIsOp19Sim) {
                log("ignore default apn setup for op19 for 'carrierConfigLoaded' reason");
            } else {
                if (apnContext2.getState() == DctConstants.State.FAILED || apnContext2.getState() == DctConstants.State.SCANNING) {
                    if (retryFailures == DcTracker.RetryFailures.ALWAYS) {
                        apnContext2.releaseDataConnection(str);
                    } else if (!apnContext2.isConcurrentVoiceAndDataAllowed() && this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed()) {
                        apnContext2.releaseDataConnection(str);
                    }
                }
                if (apnContext2.isConnectable()) {
                    if (!isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) || !isHigherPriorityApnContextActive(apnContext2)) {
                        log("isConnectable() call trySetupData");
                        apnContext2.setReason(str);
                        trySetupData(apnContext2);
                    } else {
                        log("No need to trysetupdata as higher priority apncontext exists");
                    }
                }
            }
        }
    }

    protected boolean trySetupData(ApnContext apnContext) {
        if (this.mPhone.getSimulatedRadioControl() != null) {
            apnContext.setState(DctConstants.State.CONNECTED);
            this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            log("trySetupData: X We're on the simulator; assuming connected retValue=true");
            return true;
        }
        DataConnectionReasons dataConnectionReasons = new DataConnectionReasons();
        boolean z = isDataAllowed(apnContext, dataConnectionReasons) || isDataAllowedExt(dataConnectionReasons, apnContext.getApnType());
        boolean zEquals = apnContext.getApnType().equals("emergency");
        if (!hasMdAutoSetupImsCapability() && zEquals) {
            int activeDcCount = ((MtkDcController) this.mDcc).getActiveDcCount();
            log("defaultBearerCount: " + activeDcCount + ", mDedicatedBearerCount: " + this.mDedicatedBearerCount);
            if (activeDcCount + this.mDedicatedBearerCount >= 7) {
                teardownDataByEmergencyPolicy();
                return false;
            }
        }
        String str = "trySetupData for APN type " + apnContext.getApnType() + ", reason: " + apnContext.getReason() + ". " + dataConnectionReasons.toString();
        log(str);
        apnContext.requestLog(str);
        if (z) {
            if (apnContext.getState() == DctConstants.State.FAILED) {
                log("trySetupData: make a FAILED ApnContext IDLE so its reusable");
                apnContext.requestLog("trySetupData: make a FAILED ApnContext IDLE so its reusable");
                apnContext.setState(DctConstants.State.IDLE);
            }
            int rilDataRadioTechnology = this.mPhone.getServiceState().getRilDataRadioTechnology();
            apnContext.setConcurrentVoiceAndDataAllowed(this.mPhone.getServiceStateTracker().isConcurrentVoiceAndDataAllowed());
            if (apnContext.getState() == DctConstants.State.IDLE) {
                if (TextUtils.equals(apnContext.getApnType(), "emergency")) {
                    if (this.mAllApnSettings == null) {
                        log("mAllApnSettings is null, create first and add emergency one");
                        createAllApnList();
                    } else if (this.mAllApnSettings.isEmpty()) {
                        log("add mEmergencyApn: " + this.mEmergencyApn + " to mAllApnSettings");
                        addEmergencyApnSetting();
                    }
                }
                ArrayList arrayListBuildWaitingApns = buildWaitingApns(apnContext.getApnType(), rilDataRadioTechnology);
                if (arrayListBuildWaitingApns.isEmpty()) {
                    notifyNoData(DcFailCause.MISSING_UNKNOWN_APN, apnContext);
                    notifyOffApnsOfAvailability(apnContext.getReason());
                    log("trySetupData: X No APN found retValue=false");
                    apnContext.requestLog("trySetupData: X No APN found retValue=false");
                    return false;
                }
                apnContext.setWaitingApns(arrayListBuildWaitingApns);
                ((MtkApnContext) apnContext).setWifiApns(buildWifiApns(apnContext.getApnType()));
                log("trySetupData: Create from mAllApnSettings : " + apnListToString(this.mAllApnSettings));
            }
            logd("trySetupData: call setupData, waitingApns : " + apnListToString(apnContext.getWaitingApns()) + ", wifiApns : " + apnListToString(((MtkApnContext) apnContext).getWifiApns()));
            boolean z2 = setupData(apnContext, rilDataRadioTechnology, dataConnectionReasons.contains(DataConnectionReasons.DataAllowedReasonType.UNMETERED_APN));
            notifyOffApnsOfAvailability(apnContext.getReason());
            log("trySetupData: X retValue=" + z2);
            return z2;
        }
        if (!apnContext.getApnType().equals("default") && apnContext.isConnectable()) {
            if (apnContext.getApnType().equals("mms") && TelephonyManager.getDefault().isMultiSimEnabled() && !this.mAttached.get()) {
                log("Wait for attach");
                return true;
            }
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
        String apnType = apnContext.getApnType();
        if ("preempt".equals(apnType) && !this.mDataEnabledSettings.isPolicyDataEnabled() && isDataAllowedAsOff(apnType)) {
            apnContext.setState(DctConstants.State.SCANNING);
            sb.append(" set state to SCANNING for preempt.");
        }
        log(sb.toString());
        apnContext.requestLog(sb.toString());
        return false;
    }

    protected void notifyOffApnsOfAvailability(String str) {
        if (!this.mHasPsEverAttached) {
            boolean z = false;
            if (!TextUtils.isEmpty(str) && (str.equals("dataDetached") || str.equals("roamingOff"))) {
                z = true;
            }
            if (!this.mAttached.get() && z) {
                logi("notifyOffApnsOfAvailability optimize reason: " + str + ", notify only for type default and emergency");
                this.mPhone.notifyDataConnection(str, "default", PhoneConstants.DataState.DISCONNECTED);
                this.mPhone.notifyDataConnection(str, "emergency", PhoneConstants.DataState.DISCONNECTED);
                return;
            }
        }
        super.notifyOffApnsOfAvailability(str);
    }

    protected boolean cleanUpAllConnections(boolean z, String str) {
        log("cleanUpAllConnections: tearDown=" + z + " reason=" + str);
        boolean z2 = false;
        boolean z3 = !TextUtils.isEmpty(str) && (str.equals("specificDisabled") || str.equals("roamingOn") || str.equals("carrierActionDisableMeteredApn") || str.equals("pdpReset") || str.equals("radioTurnedOff") || str.equals(MtkGsmCdmaPhone.REASON_FDN_ENABLED));
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
            } else if (!str.equals("SinglePdnArbitration") || !apnContext.getApnType().equals("ims")) {
                if (str != null && str.equals("roamingOn") && ignoreDataRoaming(apnContext.getApnType())) {
                    log("cleanUpConnection: Ignore Data Roaming for apnType = " + apnContext.getApnType());
                } else {
                    if (!apnContext.isDisconnected()) {
                        z2 = true;
                    }
                    apnContext.setReason(str);
                    cleanUpConnection(z, apnContext);
                }
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

    protected void cleanUpConnection(boolean z, ApnContext apnContext) {
        if (apnContext == null) {
            log("cleanUpConnection: apn context is null");
            return;
        }
        DcAsyncChannel dcAc = apnContext.getDcAc();
        String str = "cleanUpConnection: tearDown=" + z + " reason=" + apnContext.getReason();
        if (VDBG) {
            log(str + " apnContext=" + apnContext);
        }
        apnContext.requestLog(str);
        if (z) {
            if (apnContext.isDisconnected()) {
                apnContext.setState(DctConstants.State.IDLE);
                if (!apnContext.isReady()) {
                    if (dcAc != null) {
                        logi("cleanUpConnection: teardown, disconnected, !ready apnContext=" + apnContext);
                        apnContext.requestLog("cleanUpConnection: teardown, disconnected, !ready");
                        dcAc.tearDown(apnContext, "", (Message) null);
                    }
                    apnContext.setDataConnectionAc((DcAsyncChannel) null);
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
                    logi(string + "apnContext=" + apnContext);
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
                if (((MtkApnContext) apnContext).isNeedNotify()) {
                    this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
                }
            }
        } else {
            if (dcAc != null) {
                dcAc.reqReset();
            }
            apnContext.setState(DctConstants.State.IDLE);
            if (((MtkApnContext) apnContext).isNeedNotify()) {
                this.mPhone.notifyDataConnection(apnContext.getReason(), apnContext.getApnType());
            }
            apnContext.setDataConnectionAc((DcAsyncChannel) null);
        }
        if (dcAc != null) {
            cancelReconnectAlarm(apnContext);
        }
        String str2 = "cleanUpConnection: X tearDown=" + z + " reason=" + apnContext.getReason();
        if (((MtkApnContext) apnContext).isNeedNotify()) {
            log(str2 + " apnContext=" + apnContext + " dcac=" + apnContext.getDcAc());
        }
        apnContext.requestLog(str2);
    }

    public boolean isPermanentFailure(DcFailCause dcFailCause) {
        return dcFailCause.isPermanentFailure(this.mPhone.getContext(), this.mPhone.getSubId()) && !(this.mAttached.get() && dcFailCause == DcFailCause.SIGNAL_LOST);
    }

    protected ApnSetting makeApnSetting(Cursor cursor) {
        int i;
        try {
            i = cursor.getInt(cursor.getColumnIndexOrThrow("inactive_timer"));
            if (i != 0) {
                try {
                    logd("makeApnSetting: inactive_timer=" + i);
                } catch (IllegalArgumentException e) {
                }
            }
        } catch (IllegalArgumentException e2) {
            i = 0;
        }
        int i2 = i;
        String[] types = parseTypes(cursor.getString(cursor.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_TYPE)));
        int iConvertBearerBitmaskToNetworkTypeBitmask = cursor.getInt(cursor.getColumnIndexOrThrow("network_type_bitmask"));
        if (iConvertBearerBitmaskToNetworkTypeBitmask == 0) {
            iConvertBearerBitmaskToNetworkTypeBitmask = ServiceState.convertBearerBitmaskToNetworkTypeBitmask(cursor.getInt(cursor.getColumnIndexOrThrow("bearer_bitmask")));
        }
        return new MtkApnSetting(cursor.getInt(cursor.getColumnIndexOrThrow("_id")), cursor.getString(cursor.getColumnIndexOrThrow("numeric")), cursor.getString(cursor.getColumnIndexOrThrow("name")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("proxy"))), cursor.getString(cursor.getColumnIndexOrThrow("port")), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("mmsc"))), NetworkUtils.trimV4AddrZeros(cursor.getString(cursor.getColumnIndexOrThrow("mmsproxy"))), cursor.getString(cursor.getColumnIndexOrThrow("mmsport")), cursor.getString(cursor.getColumnIndexOrThrow(DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER)), cursor.getString(cursor.getColumnIndexOrThrow("password")), cursor.getInt(cursor.getColumnIndexOrThrow("authtype")), types, cursor.getString(cursor.getColumnIndexOrThrow("protocol")), cursor.getString(cursor.getColumnIndexOrThrow("roaming_protocol")), cursor.getInt(cursor.getColumnIndexOrThrow("carrier_enabled")) == 1, iConvertBearerBitmaskToNetworkTypeBitmask, cursor.getInt(cursor.getColumnIndexOrThrow("profile_id")), cursor.getInt(cursor.getColumnIndexOrThrow("modem_cognitive")) == 1, cursor.getInt(cursor.getColumnIndexOrThrow("max_conns")), cursor.getInt(cursor.getColumnIndexOrThrow("wait_time")), cursor.getInt(cursor.getColumnIndexOrThrow("max_conns_time")), cursor.getInt(cursor.getColumnIndexOrThrow("mtu")), cursor.getString(cursor.getColumnIndexOrThrow("mvno_type")), cursor.getString(cursor.getColumnIndexOrThrow("mvno_match_data")), cursor.getInt(cursor.getColumnIndexOrThrow("apn_set_id")), i2);
    }

    protected void setInitialAttachApn() {
        boolean z;
        boolean z2;
        ApnSetting classTypeApn;
        ApnSetting apnSetting;
        ApnSetting apnSetting2;
        ApnSetting apnSetting3;
        if (hasOperatorIaCapability()) {
            super.setInitialAttachApn();
            return;
        }
        ApnSetting apnSetting4 = this.mInitialAttachApnSetting;
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        if (strMtkGetOperatorNumeric == null || strMtkGetOperatorNumeric.length() == 0) {
            log("setInitialApn: but no operator numeric");
            return;
        }
        synchronized (this.mNeedsResumeModemLock) {
            z = false;
            if (this.mNeedsResumeModem) {
                this.mNeedsResumeModem = false;
                z2 = true;
            } else {
                z2 = false;
            }
        }
        String strSubstring = strMtkGetOperatorNumeric.substring(0, 3);
        log("setInitialApn: currentMcc = " + strSubstring + ", needsResumeModem = " + z2);
        StringBuilder sb = new StringBuilder();
        sb.append("setInitialAttachApn: current attach Apn [");
        sb.append(this.mInitialAttachApnSetting);
        sb.append("]");
        log(sb.toString());
        log("setInitialApn: E mPreferredApn=" + this.mPreferredApn);
        Message messageObtainMessage = null;
        if (this.mIsImsHandover || MTK_IMS_TESTMODE_SUPPORT) {
            classTypeApn = getClassTypeApn(3);
            if (classTypeApn != null) {
                log("setInitialAttachApn: manualChangedAttachApn = " + classTypeApn);
            }
        } else {
            classTypeApn = null;
        }
        if (this.mMdChangedAttachApn == null) {
            int phoneId = this.mPhone.getPhoneId();
            if (SubscriptionManager.isValidPhoneId(phoneId)) {
                int i = SystemProperties.getInt(PROP_APN_CLASS + phoneId, -1);
                if (i >= 0) {
                    String str = SystemProperties.get(this.PROPERTY_ICCID[phoneId], "");
                    String str2 = SystemProperties.get(PROP_APN_CLASS_ICCID + phoneId, "");
                    log("setInitialAttachApn: " + str + " , " + str2 + ", " + i);
                    if (TextUtils.equals(str, str2)) {
                        updateMdChangedAttachApn(i);
                    } else {
                        SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, "");
                        SystemProperties.set(PROP_APN_CLASS + phoneId, "");
                    }
                }
            }
        }
        ApnSetting apnSetting5 = this.mMdChangedAttachApn;
        if (this.mMdChangedAttachApn != null && getClassType(this.mMdChangedAttachApn) == 1 && !isMdChangedAttachApnEnabled()) {
            apnSetting5 = null;
        }
        if (apnSetting5 != null || classTypeApn != null) {
            apnSetting = null;
            apnSetting2 = null;
            apnSetting3 = null;
        } else if (this.mPreferredApn != null && this.mPreferredApn.canHandleType("ia")) {
            apnSetting3 = this.mPreferredApn;
            apnSetting = null;
            apnSetting2 = null;
        } else if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            apnSetting = (ApnSetting) this.mAllApnSettings.get(0);
            log("setInitialApn: firstApnSetting=" + apnSetting);
            Iterator it = this.mAllApnSettings.iterator();
            boolean z3 = false;
            apnSetting2 = null;
            while (true) {
                if (it.hasNext()) {
                    apnSetting3 = (ApnSetting) it.next();
                    if (this.mIsAddMnoApnsIntoAllApnList) {
                        z3 = !isSimActivated() && SPRINT_IA_NI.compareToIgnoreCase(apnSetting3.apn) == 0;
                    }
                    log("setInitialApn: isSelectOpIa=" + z3);
                    if ((ArrayUtils.contains(apnSetting3.types, "ia") || z3) && apnSetting3.carrierEnabled && checkIfDomesticInitialAttachApn(strSubstring)) {
                        log("setInitialApn: iaApnSetting=" + apnSetting3);
                        if (ArrayUtils.contains(this.PLMN_EMPTY_APN_PCSCF_SET, strMtkGetOperatorNumeric)) {
                            z = true;
                        }
                    } else if (apnSetting2 == null && apnSetting3.canHandleType("default")) {
                        log("setInitialApn: defaultApnSetting=" + apnSetting3);
                        apnSetting2 = apnSetting3;
                    }
                } else {
                    apnSetting3 = null;
                    break;
                }
            }
        }
        this.mInitialAttachApnSetting = null;
        if (classTypeApn != null) {
            log("setInitialAttachApn: using manualChangedAttachApn");
            this.mInitialAttachApnSetting = classTypeApn;
        } else if (apnSetting5 != null) {
            log("setInitialAttachApn: using mMdChangedAttachApn");
            this.mInitialAttachApnSetting = apnSetting5;
        } else if (apnSetting3 != null) {
            log("setInitialAttachApn: using iaApnSetting");
            this.mInitialAttachApnSetting = apnSetting3;
        } else if (this.mPreferredApn != null) {
            log("setInitialAttachApn: using mPreferredApn");
            this.mInitialAttachApnSetting = this.mPreferredApn;
        } else if (apnSetting2 != null) {
            log("setInitialAttachApn: using defaultApnSetting");
            this.mInitialAttachApnSetting = apnSetting2;
        } else if (apnSetting != null) {
            log("setInitialAttachApn: using firstApnSetting");
            this.mInitialAttachApnSetting = apnSetting;
        }
        if (this.mInitialAttachApnSetting == null) {
            log("setInitialAttachApn: X There in no available apn, use empty");
            this.mDataServiceManager.setInitialAttachApn(createDataProfile(new ApnSetting(0, "", "", "", "", "", "", "", "", "", "", 0, new String[]{"ia"}, "IPV4V6", "IPV4V6", true, 0, 0, false, 0, 0, 0, 0, "", "", 0)), this.mPhone.getServiceState().getDataRoamingFromRegistration(), (Message) null);
        } else {
            log("setInitialAttachApn: X selected Apn=" + this.mInitialAttachApnSetting);
            String str3 = this.mInitialAttachApnSetting.apn;
            if (z) {
                log("setInitialAttachApn: ESM flag false, change IA APN to empty");
            }
            if (z2) {
                log("setInitialAttachApn: DCM IA support");
                messageObtainMessage = obtainMessage(270846);
            }
            this.mDataServiceManager.setInitialAttachApn(createDataProfile(this.mInitialAttachApnSetting), this.mPhone.getServiceState().getDataRoamingFromRegistration(), messageObtainMessage);
        }
        log("setInitialAttachApn: new attach Apn [" + this.mInitialAttachApnSetting + "]");
    }

    protected void onApnChanged() {
        if (this.mPhone instanceof GsmCdmaPhone) {
            this.mPhone.updateCurrentCarrierInProvider();
        }
        log("onApnChanged: createAllApnList and cleanUpAllConnections");
        createAllApnList();
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        if (strMtkGetOperatorNumeric != null && strMtkGetOperatorNumeric.length() > 0) {
            setInitialAttachApn();
        } else {
            log("onApnChanged: but no operator numeric");
        }
        sendOnApnChangedDone(false);
    }

    protected boolean isHigherPriorityApnContextActive(ApnContext apnContext) {
        if (apnContext.getApnType().equals("ims")) {
            return false;
        }
        for (ApnContext apnContext2 : this.mPrioritySortedApnContextsEx) {
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
        if (this.mDataConnectionExt != null) {
            try {
                boolean zIsOnlySingleDcAllowed = this.mDataConnectionExt.isOnlySingleDcAllowed();
                if (zIsOnlySingleDcAllowed) {
                    log("isOnlySingleDcAllowed: " + zIsOnlySingleDcAllowed);
                    return true;
                }
            } catch (Exception e) {
                loge("Fail to create or use plug-in");
                e.printStackTrace();
            }
        }
        return super.isOnlySingleDcAllowed(i);
    }

    protected boolean retryAfterDisconnected(ApnContext apnContext) {
        String reason = apnContext.getReason();
        if ("radioTurnedOff".equals(reason) || MtkGsmCdmaPhone.REASON_FDN_ENABLED.equals(reason) || (isOnlySingleDcAllowed(this.mPhone.getServiceState().getRilDataRadioTechnology()) && isHigherPriorityApnContextActive(apnContext))) {
            return false;
        }
        return true;
    }

    protected void onRecordsLoadedOrSubIdChanged() {
        if (TextUtils.isEmpty(mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get()))) {
            logd("onRecordsLoadedOrSubIdChanged: empty operator numeric, return");
            return;
        }
        if (MTK_CC33_SUPPORT) {
            this.mPhone.mCi.setRemoveRestrictEutranMode(true, null);
        }
        syncDataSettingsToMd(new int[]{isUserDataEnabled() ? 1 : 0, getDataRoamingEnabled() ? 1 : 0, -2, getDomesticDataRoamingEnabledFromSettings() ? 1 : 0, getInternationalDataRoamingEnabledFromSettings() ? 1 : 0});
        MtkDcHelper.getInstance().syncDefaultDataSlotId(MtkSubscriptionController.getMtkInstance().getSlotIndex(SubscriptionController.getInstance().getDefaultDataSubId()));
        this.mIsFdnChecked = false;
        this.mDomesticRoamingsettings = getDomesticDataRoamingEnabledFromSettings();
        this.mInternationalRoamingsettings = getInternationalDataRoamingEnabledFromSettings();
        this.mIsOp19Sim = isOp19Sim();
        super.onRecordsLoadedOrSubIdChanged();
    }

    public void syncDefaultDataSlotId(int i) {
        log("syncDefaultDataSlotId slot: " + i);
        syncDataSettingsToMd(new int[]{-2, -2, i, -2, -2});
    }

    private void syncDataSettingsToMd(int[] iArr) {
        logd("syncDataSettingsToMd(), " + iArr[0] + ", " + iArr[1] + ", " + iArr[2] + ", " + iArr[3] + ", " + iArr[4]);
        this.mPhone.mCi.syncDataSettingsToMd(iArr, null);
    }

    protected void onSetPolicyDataEnabled(boolean z) {
        boolean zIsDataEnabled = isDataEnabled();
        if (this.mDataEnabledSettings.isPolicyDataEnabled() != z) {
            this.mDataEnabledSettings.setPolicyDataEnabled(z);
            if (zIsDataEnabled != isDataEnabled() || hasConnectedOrConnectingMeteredApn()) {
                if (!zIsDataEnabled && z) {
                    reevaluateDataConnections();
                    onTrySetupData("dataEnabled");
                } else {
                    onCleanUpAllConnections("specificDisabled");
                }
            }
        }
    }

    public boolean isUserDataEnabled() {
        boolean zIsUserDataEnabled = super.isUserDataEnabled();
        log("isUserDataEnabled: retVal=" + zIsUserDataEnabled + ", phoneSubId=" + this.mPhone.getSubId());
        return zIsUserDataEnabled;
    }

    protected void onDataRoamingOff() {
        log("onDataRoamingOff getDataRoamingEnabled=" + getDataRoamingEnabled() + ", mUserDataEnabled=" + this.mDataEnabledSettings.isUserDataEnabled());
        if (this.mCcUniqueSettingsForRoaming) {
            boolean domesticDataRoamingEnabledFromSettings = getDomesticDataRoamingEnabledFromSettings();
            boolean internationalDataRoamingEnabledFromSettings = getInternationalDataRoamingEnabledFromSettings();
            log("onDomOrIntRoamingOn bDomDataOnRoamingEnabled=" + domesticDataRoamingEnabledFromSettings + ", bIntDataOnRoamingEnabled=" + internationalDataRoamingEnabledFromSettings + ", currentRoamingType=" + this.mPhone.getServiceState().getDataRoamingType());
            if (!domesticDataRoamingEnabledFromSettings || !internationalDataRoamingEnabledFromSettings) {
                log("onDomOrIntRoamingOn: setup data for HOME.");
                setInitialAttachApn();
                setDataProfilesAsNeeded();
                setupDataOnConnectableApns("roamingOff");
                notifyDataConnection("roamingOff");
            } else {
                notifyDataConnection("roamingOff");
            }
        } else if (!getDataRoamingEnabled()) {
            boolean zHasOperatorIaCapability = hasOperatorIaCapability();
            log("onDataRoamingOff: bHasOperatorIaCapability=" + zHasOperatorIaCapability);
            if (!zHasOperatorIaCapability) {
                setInitialAttachApn();
                setDataProfilesAsNeeded();
            }
            notifyOffApnsOfAvailability("roamingOff");
            setupDataOnConnectableApns("roamingOff");
        } else {
            notifyDataConnection("roamingOff");
        }
        if (!hasOperatorIaCapability() && isOp18Sim()) {
            setInitialAttachApn();
        }
    }

    protected void onDataRoamingOnOrSettingsChanged(int i) {
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        log("onDataRoamingOnOrSettingsChanged getDataRoamingEnabled=" + getDataRoamingEnabled() + ", mUserDataEnabled=" + ((MtkDcTracker) this).mDataEnabledSettings.isUserDataEnabled());
        boolean z5 = i == 270384;
        if (this.mCcUniqueSettingsForRoaming) {
            if (this.mDomesticRoamingsettings == getDomesticDataRoamingEnabledFromSettings()) {
                z = false;
                z2 = false;
            } else {
                this.mDomesticRoamingsettings = !this.mDomesticRoamingsettings;
                z2 = this.mDomesticRoamingsettings;
                z = true;
            }
            if (this.mInternationalRoamingsettings == getInternationalDataRoamingEnabledFromSettings()) {
                z3 = false;
                z4 = false;
            } else {
                this.mInternationalRoamingsettings = !this.mInternationalRoamingsettings;
                z4 = this.mInternationalRoamingsettings;
                z3 = true;
            }
            logd("onDataRoamingOnOrSettingsChanged, op20");
            ?? r6 = new int[5];
            r6[0] = -2;
            r6[1] = -2;
            r6[2] = -2;
            ?? r3 = z2;
            if (!z) {
                r3 = -2;
            }
            r6[3] = r3;
            ?? r5 = z4;
            if (!z3) {
                r5 = -2;
            }
            r6[4] = r5;
            syncDataSettingsToMd(r6);
        }
        if (!((MtkDcTracker) this).mPhone.getServiceState().getDataRoaming()) {
            log("device is not roaming. ignored the request.");
            return;
        }
        checkDataRoamingStatus(z5);
        if (!hasOperatorIaCapability() && isOp18Sim()) {
            setInitialAttachApn();
        }
        if (this.mCcUniqueSettingsForRoaming) {
            if (checkDomesticDataRoamingEnabled() || checkInternationalDataRoamingEnabled()) {
                log("onDataRoamingOnOrSettingsChanged: setup data on roaming");
                setupDataOnConnectableApns("roamingOn");
                notifyDataConnection("roamingOn");
                return;
            } else {
                log("onDataRoamingOnOrSettingsChanged: Tear down data connection on roaming.");
                cleanUpAllConnections(true, "roamingOn");
                notifyOffApnsOfAvailability("roamingOn");
                return;
            }
        }
        if (getDataRoamingEnabled() || getDomesticRoamingEnabled()) {
            log("onDataRoamingOnOrSettingsChanged: setup data on roaming");
            setupDataOnConnectableApns("roamingOn");
            notifyDataConnection("roamingOn");
        } else {
            log("onDataRoamingOnOrSettingsChanged: Tear down data connection on roaming.");
            if (MtkUiccController.getVsimCardType(((MtkDcTracker) this).mPhone.getPhoneId()) == MtkIccCardConstants.VsimType.REMOTE_SIM) {
                log("RSim, not tear down any data connection since ignore data roaming");
            } else {
                cleanUpAllConnections(true, "roamingOn");
                notifyOffApnsOfAvailability("roamingOn");
            }
        }
    }

    protected void onDisconnectDone(AsyncResult asyncResult) {
        super.onDisconnectDone(asyncResult);
        ApnContext validApnContext = getValidApnContext(asyncResult, "onDisconnectDone");
        if (validApnContext == null) {
            return;
        }
        if (!hasMdAutoSetupImsCapability() && "pdnOccupied".equals(validApnContext.getReason())) {
            log("try setup emergency PDN");
            trySetupData((ApnContext) this.mApnContextsById.get(9));
        }
        String apnType = validApnContext.getApnType();
        if ("preempt".equals(apnType) && !this.mDataEnabledSettings.isPolicyDataEnabled() && isDataAllowedAsOff(apnType)) {
            validApnContext.setState(DctConstants.State.SCANNING);
            log("onDisconnectDone, set preempt state to SCANNING");
        }
    }

    protected void notifyDataConnection(String str) {
        String reason;
        log("notifyDataConnection: reason=" + str);
        if (this.mAttached.get()) {
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (apnContext.isReady() && ((MtkApnContext) apnContext).isNeedNotify()) {
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
        }
        notifyOffApnsOfAvailability(str);
    }

    protected void setDataProfilesAsNeeded() {
        log("setDataProfilesAsNeeded");
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            ArrayList<DataProfile> arrayList = new ArrayList<>();
            ArrayList<ApnSetting> arrayListFetchDunApns = fetchDunApns();
            Iterator it = this.mAllApnSettings.iterator();
            while (it.hasNext()) {
                DataProfile dataProfileCreateDataProfile = createDataProfile(encodeInactiveTimer((ApnSetting) it.next()));
                if (!arrayList.contains(dataProfileCreateDataProfile) || isDataProfileUnique(arrayList, dataProfileCreateDataProfile)) {
                    arrayList.add(dataProfileCreateDataProfile);
                }
            }
            if (arrayListFetchDunApns != null && arrayListFetchDunApns.size() > 0) {
                for (ApnSetting apnSetting : arrayListFetchDunApns) {
                    DataProfile dataProfileCreateDataProfile2 = createDataProfile(apnSetting);
                    if (!arrayList.contains(dataProfileCreateDataProfile2) || isDataProfileUnique(arrayList, dataProfileCreateDataProfile2)) {
                        arrayList.add(dataProfileCreateDataProfile2);
                        log("setDataProfilesAsNeeded: add DUN APN : " + apnSetting);
                    }
                }
            }
            if (arrayList.size() > 0) {
                this.mDataServiceManager.setDataProfile(arrayList, this.mPhone.getServiceState().getDataRoamingFromRegistration(), (Message) null);
            }
        }
    }

    private boolean isDataProfileUnique(ArrayList<DataProfile> arrayList, DataProfile dataProfile) {
        int iIndexOf;
        if (Build.IS_USER && (iIndexOf = arrayList.indexOf(dataProfile)) != -1) {
            DataProfile dataProfile2 = arrayList.get(iIndexOf);
            if (!TextUtils.equals(dataProfile2.getApn(), dataProfile.getApn()) || !TextUtils.equals(dataProfile2.getUserName(), dataProfile.getUserName()) || !TextUtils.equals(dataProfile2.getPassword(), dataProfile.getPassword())) {
                log("isDataProfileUnique: apn/user/password is unique, added dp=" + dataProfile);
                return true;
            }
            return false;
        }
        return false;
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
        return new MtkApnSetting(i3, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, str12, str13, str9, str10, str11, apnSetting.user, apnSetting.password, apnSetting.authType, (String[]) arrayList.toArray(new String[0]), str14, str15, apnSetting.carrierEnabled, iConvertBearerBitmaskToNetworkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive || apnSetting2.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId, ((MtkApnSetting) apnSetting).inactiveTimer);
    }

    protected String apnListToString(ArrayList<ApnSetting> arrayList) {
        try {
            return super.apnListToString(arrayList);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void setPreferredApn(int i) {
        if (this.mCanSetPreferApn) {
            log("setPreferredApn: insert pos=" + i + ", subId=" + this.mPhone.getSubId());
        }
        super.setPreferredApn(i);
    }

    public void handleMessage(Message message) {
        if (VDBG) {
            log("handleMessage msg=" + message);
        }
        int i = message.what;
        if (i == 270359) {
            ConnectivityManager connectivityManager = (ConnectivityManager) this.mPhone.getContext().getSystemService("connectivity");
            log("EVENT_PS_RESTRICT_DISABLED " + this.mIsPsRestricted);
            this.mIsPsRestricted = false;
            if (isConnected()) {
                startNetStatPoll();
                startDataStallAlarm(false);
                return;
            }
            if (this.mState == DctConstants.State.FAILED) {
                cleanUpAllConnections(false, "psRestrictEnabled");
                this.mReregisterOnReconnectFailure = false;
            }
            ApnContext apnContext = (ApnContext) this.mApnContextsById.get(0);
            if (apnContext != null) {
                if (this.mPhone.getServiceStateTracker().getCurrentDataConnectionState() == 0) {
                    apnContext.setReason("psRestrictEnabled");
                    trySetupData(apnContext);
                } else {
                    log("EVENT_PS_RESTRICT_DISABLED, data not attached, skip.");
                }
            } else {
                loge("**** Default ApnContext not found ****");
                if (Build.IS_DEBUGGABLE && connectivityManager.isNetworkSupported(0)) {
                    throw new RuntimeException("Default ApnContext not found");
                }
            }
            ApnContext apnContext2 = (ApnContext) this.mApnContextsById.get(1);
            if (apnContext2 != null && apnContext2.isConnectable()) {
                apnContext2.setReason("psRestrictEnabled");
                trySetupData(apnContext2);
                return;
            } else {
                loge("**** MMS ApnContext not found ****");
                return;
            }
        }
        if (i != 270377) {
            switch (i) {
                case 270839:
                    logd("EVENT_APN_CHANGED_DONE");
                    onApnChangedDone();
                    return;
                case 270840:
                    onFdnChanged();
                    return;
                case 270841:
                    logd("EVENT_RESET_PDP_DONE cid=" + message.arg1);
                    return;
                case 270842:
                    onProcessPendingSetupData();
                    return;
                case 270843:
                    onMdChangedAttachApn((AsyncResult) message.obj);
                    return;
                case 270844:
                    log("handleMessage : <EVENT_REG_PLMN_CHANGED>");
                    if (isOp129IaSupport() || isOp17IaSupport()) {
                        handlePlmnChange((AsyncResult) message.obj);
                        return;
                    }
                    return;
                case 270845:
                    log("handleMessage : <EVENT_REG_SUSPENDED>");
                    if ((isOp129IaSupport() || isOp17IaSupport()) && isNeedToResumeMd()) {
                        handleRegistrationSuspend((AsyncResult) message.obj);
                        return;
                    }
                    return;
                case 270846:
                    log("handleMessage : <EVENT_SET_RESUME>");
                    if (isOp129IaSupport() || isOp17IaSupport()) {
                        handleSetResume();
                        return;
                    }
                    return;
                case 270847:
                    if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
                        setInitialAttachApn();
                        return;
                    } else {
                        log("EVENT_RESET_ATTACH_APN: Ignore due to null APN list");
                        return;
                    }
                case 270848:
                    onSharedDefaultApnState(message.arg1);
                    return;
                case 270849:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    if (asyncResult.exception == null) {
                        int[] iArr = (int[]) asyncResult.result;
                        int i2 = iArr.length > 0 ? iArr[0] : -1;
                        int i3 = iArr.length > 1 ? iArr[1] : -1;
                        if (i2 != 1) {
                            notifyPsNetworkTypeChanged(i3);
                        } else {
                            broadcastPsNetworkTypeChanged(13);
                        }
                        logd("EVENT_LTE_ACCESS_STRATUM_STATE lteAccessStratumDataState = " + i2 + ", networkType = " + i3);
                        notifyLteAccessStratumChanged(i2);
                        return;
                    }
                    loge("LteAccessStratumState exception: " + asyncResult.exception);
                    return;
                case 270850:
                    onRoamingTypeChanged();
                    return;
                case 270851:
                    logd("EVENT_MD_DATA_RETRY_COUNT_RESET");
                    setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_MD_DATA_RETRY_COUNT_RESET, DcTracker.RetryFailures.ALWAYS);
                    return;
                case 270852:
                    if (MTK_CC33_SUPPORT) {
                        logd("EVENT_REMOVE_RESTRICT_EUTRAN");
                        this.mReregisterOnReconnectFailure = false;
                        setupDataOnConnectableApns("psRestrictDisabled", DcTracker.RetryFailures.ALWAYS);
                        return;
                    }
                    return;
                case 270853:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    if (asyncResult2 != null && asyncResult2.result != null) {
                        onAllowChanged(((int[]) asyncResult2.result)[0] == 1);
                        return;
                    } else {
                        loge("Parameter error: ret should not be NULL");
                        return;
                    }
                case 270854:
                    setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_CARRIER_CONFIG_LOADED);
                    return;
                case 270855:
                    AsyncResult asyncResult3 = (AsyncResult) message.obj;
                    if (asyncResult3.result instanceof Pair) {
                        Pair pair = (Pair) asyncResult3.result;
                        onDataEnabledSettings(((Boolean) pair.first).booleanValue(), ((Integer) pair.second).intValue());
                        return;
                    }
                    return;
                case 270856:
                    handlePcoDataAfterAttached((AsyncResult) message.obj);
                    return;
                case 270857:
                    this.mDedicatedBearerCount++;
                    AsyncResult asyncResult4 = (AsyncResult) message.obj;
                    if (asyncResult4.result instanceof MtkDedicateDataCallResponse) {
                        onDedecatedBearerActivated((MtkDedicateDataCallResponse) asyncResult4.result);
                        return;
                    }
                    return;
                case 270858:
                    AsyncResult asyncResult5 = (AsyncResult) message.obj;
                    if (asyncResult5.result instanceof MtkDedicateDataCallResponse) {
                        onDedecatedBearerModified((MtkDedicateDataCallResponse) asyncResult5.result);
                        return;
                    }
                    return;
                case 270859:
                    if (this.mDedicatedBearerCount > 0) {
                        this.mDedicatedBearerCount--;
                    }
                    onDedecatedBearerDeactivated(((Integer) ((AsyncResult) message.obj).result).intValue());
                    return;
                default:
                    switch (i) {
                        case 270861:
                            SystemProperties.set(PROP_APN_CLASS_ICCID + this.mPhone.getPhoneId(), "");
                            SystemProperties.set(PROP_APN_CLASS + this.mPhone.getPhoneId(), "");
                            return;
                        case 270862:
                            onNetworkRejectReceived((AsyncResult) message.obj);
                            return;
                        default:
                            super.handleMessage(message);
                            return;
                    }
            }
        }
        if (this.mRilRat != this.mPhone.getServiceState().getRilDataRadioTechnology()) {
            this.mRilRat = this.mPhone.getServiceState().getRilDataRadioTechnology();
            super.handleMessage(message);
        }
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
        if (TextUtils.equals(str, "ia")) {
            return 0;
        }
        if (TextUtils.equals(str, "dun")) {
            return 1;
        }
        if (TextUtils.equals(str, "mms")) {
            return 1001;
        }
        if (TextUtils.equals(str, "supl")) {
            return 1002;
        }
        if (TextUtils.equals(str, "hipri")) {
            return 1003;
        }
        if (TextUtils.equals(str, "wap")) {
            return 1004;
        }
        if (TextUtils.equals(str, "emergency")) {
            return 1005;
        }
        if (TextUtils.equals(str, "xcap")) {
            return 1006;
        }
        if (TextUtils.equals(str, "rcs")) {
            return 1007;
        }
        if (TextUtils.equals(str, "default")) {
            return 0;
        }
        if (TextUtils.equals(str, "bip")) {
            return 1008;
        }
        if (TextUtils.equals(str, "*")) {
            return ExternalSimConstants.MSG_ID_UICC_AUTHENTICATION_ABORT_IND;
        }
        if (TextUtils.equals(str, "vsim")) {
            return 1009;
        }
        return TextUtils.equals(str, "preempt") ? 0 : -1;
    }

    protected void onUpdateIcc() {
        int i;
        if (this.mUiccController == null) {
            return;
        }
        int i2 = 1;
        IccRecords uiccRecords = getUiccRecords(1);
        if (MtkDcHelper.isCdmaDualActivationSupport()) {
            if ((MtkDcHelper.isCdma3GDualModeCard(this.mPhone.getPhoneId()) || MtkDcHelper.isCdma3GCard(this.mPhone.getPhoneId())) && this.mPhone.getPhoneType() == 2) {
                uiccRecords = getUiccRecords(2);
                i = 2;
            }
            i = 1;
        } else {
            if (uiccRecords == null && this.mPhone.getPhoneType() == 2 && MtkDcHelper.isCdma3GCard(this.mPhone.getPhoneId())) {
                uiccRecords = getUiccRecords(2);
                i = 2;
            }
            i = 1;
        }
        IccRecords iccRecords = (IccRecords) this.mIccRecords.get();
        logd("onUpdateIcc: newIccRecords=" + uiccRecords + ", r=" + iccRecords);
        if (iccRecords != uiccRecords) {
            if (iccRecords != null) {
                log("Removing stale icc objects.");
                iccRecords.unregisterForRecordsLoaded(this);
                this.mIccRecords.set(null);
            }
            if (uiccRecords != null) {
                if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                    log("New records found.");
                    this.mPhoneType = i;
                    this.mIccRecords.set(uiccRecords);
                    uiccRecords.registerForRecordsLoaded(this, 270338, (Object) null);
                }
            } else {
                onSimNotReady();
            }
        }
        if (this.mUiccCardApplication == null) {
            this.mUiccCardApplication = new AtomicReference<>();
        }
        UiccCardApplication uiccCardApplication = this.mUiccCardApplication.get();
        MtkUiccController mtkUiccController = (MtkUiccController) this.mUiccController;
        if (this.mPhone.getPhoneType() == 2) {
            i2 = 2;
        }
        UiccCardApplication uiccCardApplication2 = mtkUiccController.getUiccCardApplication(i2);
        if (uiccCardApplication != uiccCardApplication2) {
            if (uiccCardApplication != null) {
                log("Removing stale UiccCardApplication objects.");
                ((MtkUiccCardApplication) uiccCardApplication).unregisterForFdnChanged(this);
                this.mUiccCardApplication.set(null);
            }
            if (uiccCardApplication2 != null) {
                log("New UiccCardApplication found");
                ((MtkUiccCardApplication) uiccCardApplication2).registerForFdnChanged(this, 270840, null);
                this.mUiccCardApplication.set(uiccCardApplication2);
            }
        }
    }

    public String[] getPcscfAddress(String str) {
        ApnContext apnContext;
        super.getPcscfAddress(str);
        log("getPcscfAddress() for RCS, apnType=" + str);
        if (TextUtils.equals(str, "default")) {
            apnContext = (ApnContext) this.mApnContextsById.get(0);
        } else {
            apnContext = null;
        }
        if (apnContext == null) {
            log("apnContext is null for RCS, return null");
            return null;
        }
        DcAsyncChannel dcAc = apnContext.getDcAc();
        if (dcAc == null) {
            return null;
        }
        String[] pcscfAddr = dcAc.getPcscfAddr();
        for (int i = 0; i < pcscfAddr.length; i++) {
            log("Pcscf[" + i + "]: " + pcscfAddr[i]);
        }
        return pcscfAddr;
    }

    public void update() {
        synchronized (this.mDataEnabledSettings) {
            super.update();
        }
    }

    public void log(String str) {
        logd(str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logw(String str) {
        Rlog.w(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logi(String str) {
        Rlog.i(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logd(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void logv(String str) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void handlePcoDataAfterAttached(AsyncResult asyncResult) {
        if (this.mDataConnectionExt != null) {
            this.mDataConnectionExt.handlePcoDataAfterAttached(asyncResult, this.mPhone, this.mAllApnSettings);
        }
    }

    protected void startDataStallAlarm(boolean z) {
        if (skipDataStallAlarm()) {
            log("onDataStallAlarm: switch data-stall off, skip it!");
        } else {
            super.startDataStallAlarm(z);
        }
    }

    private boolean isDataAllowedExt(DataConnectionReasons dataConnectionReasons, String str) {
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.MTK_LOCATED_PLMN_CHANGED)) {
            log("isDataAllowedExt: located plmn changed, setSetupDataPendingFlag");
            this.mPendingDataCall = true;
            return false;
        }
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.DEFAULT_DATA_UNSELECTED)) {
            if (!ignoreDefaultDataUnselected(str)) {
                return false;
            }
            dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.DEFAULT_DATA_UNSELECTED);
        }
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED)) {
            if (!ignoreDataRoaming(str) && !getDomesticRoamingEnabled()) {
                return false;
            }
            dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED);
        }
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.MTK_NOT_ALLOWED)) {
            if (!ignoreDataAllow(str)) {
                return false;
            }
            dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.MTK_NOT_ALLOWED);
        }
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.MTK_NON_VSIM_PDN_NOT_ALLOWED)) {
            if (!TextUtils.equals(str, "vsim")) {
                return false;
            }
            dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.MTK_NON_VSIM_PDN_NOT_ALLOWED);
        }
        if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.MTK_FDN_ENABLED)) {
            if (!"emergency".equals(str) && !"ims".equals(str)) {
                return false;
            }
            log("isDataAllowedExt allow IMS/EIMS for reason FDN_ENABLED");
            dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.MTK_FDN_ENABLED);
        }
        if (VDBG) {
            log("isDataAllowedExt: " + dataConnectionReasons.allowed());
        }
        return dataConnectionReasons.allowed();
    }

    private String[] getDunApnByMccMnc(Context context) {
        int i;
        Resources resources;
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        int i2 = 0;
        if (strMtkGetOperatorNumeric != null && strMtkGetOperatorNumeric.length() > 3) {
            try {
                i = Integer.parseInt(strMtkGetOperatorNumeric.substring(0, 3));
            } catch (NumberFormatException e) {
                e = e;
                i = 0;
            }
            try {
                i2 = Integer.parseInt(strMtkGetOperatorNumeric.substring(3, strMtkGetOperatorNumeric.length()));
            } catch (NumberFormatException e2) {
                e = e2;
                e.printStackTrace();
                loge("operator numeric is invalid");
            }
        } else {
            i = 0;
        }
        Resources resources2 = context.getResources();
        int i3 = resources2.getConfiguration().mcc;
        int i4 = resources2.getConfiguration().mnc;
        logd("fetchDunApns: Resource mccmnc=" + i3 + "," + i4 + "; OperatorNumeric mccmnc=" + i + "," + i2);
        try {
            new Configuration();
            Configuration configuration = context.getResources().getConfiguration();
            configuration.mcc = i;
            configuration.mnc = i2;
            resources = context.createConfigurationContext(configuration).getResources();
        } catch (Exception e3) {
            e3.printStackTrace();
            loge("getResourcesUsingMccMnc fail");
            resources = null;
        }
        if (TelephonyManager.getDefault().getSimCount() == 1 || resources == null) {
            logd("fetchDunApns: get sysResource mcc=" + i3 + ", mnc=" + i4);
            return resources2.getStringArray(R.array.config_deviceTabletopRotations);
        }
        logd("fetchDunApns: get resource from mcc=" + i + ", mnc=" + i2);
        return resources.getStringArray(R.array.config_deviceTabletopRotations);
    }

    private void onMdChangedAttachApn(AsyncResult asyncResult) {
        logv("onMdChangedAttachApn");
        int iIntValue = ((Integer) asyncResult.result).intValue();
        if (iIntValue != 1 && iIntValue != 3) {
            logw("onMdChangedAttachApn: Not handle APN Class:" + iIntValue);
            return;
        }
        int phoneId = this.mPhone.getPhoneId();
        if (SubscriptionManager.isValidPhoneId(phoneId)) {
            String str = SystemProperties.get(this.PROPERTY_ICCID[phoneId], "");
            SystemProperties.set(PROP_APN_CLASS_ICCID + phoneId, str);
            SystemProperties.set(PROP_APN_CLASS + phoneId, String.valueOf(iIntValue));
            log("onMdChangedAttachApn, set " + str + ", " + iIntValue);
        }
        updateMdChangedAttachApn(iIntValue);
        if (this.mMdChangedAttachApn != null) {
            setInitialAttachApn();
        } else {
            logw("onMdChangedAttachApn: MdChangedAttachApn is null, not found APN");
        }
    }

    private void updateMdChangedAttachApn(int i) {
        if (this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                if (i == 1 && ArrayUtils.contains(apnSetting.types, "ims")) {
                    this.mMdChangedAttachApn = apnSetting;
                    log("updateMdChangedAttachApn: MdChangedAttachApn=" + apnSetting);
                    return;
                }
                if (i == 3 && ArrayUtils.contains(apnSetting.types, "default")) {
                    this.mMdChangedAttachApn = apnSetting;
                    log("updateMdChangedAttachApn: MdChangedAttachApn=" + apnSetting);
                    return;
                }
            }
        }
    }

    private boolean isMdChangedAttachApnEnabled() {
        if (this.mMdChangedAttachApn != null && this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                if (TextUtils.equals(this.mMdChangedAttachApn.apn, apnSetting.apn)) {
                    log("isMdChangedAttachApnEnabled: " + apnSetting);
                    return apnSetting.carrierEnabled;
                }
            }
            return false;
        }
        return false;
    }

    private void sendOnApnChangedDone(boolean z) {
        Message messageObtainMessage = obtainMessage(270839);
        messageObtainMessage.arg1 = z ? 1 : 0;
        sendMessage(messageObtainMessage);
    }

    private void onApnChangedDone() {
        DctConstants.State overallState = getOverallState();
        cleanUpConnectionsOnUpdatedApns(!(overallState == DctConstants.State.IDLE || overallState == DctConstants.State.FAILED), "apnChanged");
        logd("onApnChangedDone: phone.getsubId=" + this.mPhone.getSubId() + "getDefaultDataSubscriptionId()" + SubscriptionManager.getDefaultDataSubscriptionId());
        if (this.mPhone.getSubId() == SubscriptionManager.getDefaultDataSubscriptionId()) {
            setupDataOnConnectableApns("apnChanged");
        }
    }

    private void registerFdnContentObserver() {
        Uri uri;
        if (isFdnEnableSupport()) {
            if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
                uri = Uri.parse(FDN_CONTENT_URI_WITH_SUB_ID + this.mPhone.getSubId());
            } else {
                uri = Uri.parse(FDN_CONTENT_URI);
            }
            this.mSettingsObserver.observe(uri, 270840);
        }
    }

    private boolean isFdnEnableSupport() {
        if (this.mDataConnectionExt != null) {
            return this.mDataConnectionExt.isFdnEnableSupport();
        }
        return false;
    }

    private boolean isFdnEnabled() {
        boolean z = false;
        if (!isFdnEnableSupport()) {
            return false;
        }
        IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (iMtkTelephonyExAsInterface != null) {
            try {
                boolean zIsFdnEnabled = iMtkTelephonyExAsInterface.isFdnEnabled(this.mPhone.getSubId());
                try {
                    log("isFdnEnabled(), bFdnEnabled = " + zIsFdnEnabled);
                    if (zIsFdnEnabled) {
                        if (this.mIsFdnChecked) {
                            log("isFdnEnabled(), match FDN for allow data = " + this.mIsMatchFdnForAllowData);
                            return !this.mIsMatchFdnForAllowData;
                        }
                        boolean zIsPhbReady = iMtkTelephonyExAsInterface.isPhbReady(this.mPhone.getSubId());
                        log("isFdnEnabled(), bPhbReady = " + zIsPhbReady);
                        if (zIsPhbReady) {
                            this.mWorkerHandler.sendEmptyMessage(270860);
                        } else if (!this.mIsPhbStateChangedIntentRegistered) {
                            IntentFilter intentFilter = new IntentFilter();
                            intentFilter.addAction("mediatek.intent.action.PHB_STATE_CHANGED");
                            this.mPhone.getContext().registerReceiver(this.mPhbStateChangedIntentReceiver, intentFilter);
                            this.mIsPhbStateChangedIntentRegistered = true;
                        }
                    } else if (this.mIsPhbStateChangedIntentRegistered) {
                        this.mIsPhbStateChangedIntentRegistered = false;
                        this.mPhone.getContext().unregisterReceiver(this.mPhbStateChangedIntentReceiver);
                    }
                    return zIsFdnEnabled;
                } catch (RemoteException e) {
                    e = e;
                    z = zIsFdnEnabled;
                    e.printStackTrace();
                    return z;
                }
            } catch (RemoteException e2) {
                e = e2;
            }
        } else {
            loge("isFdnEnabled(), get telephonyEx failed!!");
            return false;
        }
    }

    private void onFdnChanged() {
        boolean zIsFdnEnabled;
        RemoteException e;
        boolean zIsPhbReady;
        if (isFdnEnableSupport()) {
            log("onFdnChanged()");
            IMtkTelephonyEx iMtkTelephonyExAsInterface = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
            if (iMtkTelephonyExAsInterface != null) {
                try {
                    zIsFdnEnabled = iMtkTelephonyExAsInterface.isFdnEnabled(this.mPhone.getSubId());
                    try {
                        zIsPhbReady = iMtkTelephonyExAsInterface.isPhbReady(this.mPhone.getSubId());
                    } catch (RemoteException e2) {
                        e = e2;
                        zIsPhbReady = false;
                    }
                } catch (RemoteException e3) {
                    zIsFdnEnabled = false;
                    e = e3;
                    zIsPhbReady = false;
                }
                try {
                    log("onFdnChanged(), bFdnEnabled = " + zIsFdnEnabled + ", bPhbReady = " + zIsPhbReady);
                } catch (RemoteException e4) {
                    e = e4;
                    e.printStackTrace();
                }
            } else {
                loge("onFdnChanged(), get telephonyEx failed!!");
                zIsPhbReady = false;
                zIsFdnEnabled = false;
            }
            if (zIsPhbReady) {
                if (zIsFdnEnabled) {
                    log("fdn enabled, check fdn list");
                    this.mWorkerHandler.sendEmptyMessage(270860);
                    return;
                } else {
                    log("fdn disabled, call setupDataOnConnectableApns()");
                    setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_FDN_DISABLED);
                    return;
                }
            }
            if (!this.mIsPhbStateChangedIntentRegistered) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction("mediatek.intent.action.PHB_STATE_CHANGED");
                this.mPhone.getContext().registerReceiver(this.mPhbStateChangedIntentReceiver, intentFilter);
                this.mIsPhbStateChangedIntentRegistered = true;
                return;
            }
            return;
        }
        log("not support fdn enabled, skip onFdnChanged");
    }

    private void cleanOrSetupDataConnByCheckFdn() {
        Uri uri;
        log("cleanOrSetupDataConnByCheckFdn()");
        if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
            uri = Uri.parse(FDN_CONTENT_URI_WITH_SUB_ID + this.mPhone.getSubId());
        } else {
            uri = Uri.parse(FDN_CONTENT_URI);
        }
        Cursor cursorQuery = this.mPhone.getContext().getContentResolver().query(uri, new String[]{PplMessageManager.PendingMessage.KEY_NUMBER}, null, null, null);
        this.mIsMatchFdnForAllowData = false;
        if (cursorQuery != null) {
            this.mIsFdnChecked = true;
            if (cursorQuery.getCount() > 0 && cursorQuery.moveToFirst()) {
                while (true) {
                    String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow(PplMessageManager.PendingMessage.KEY_NUMBER));
                    log("strFdnNumber = " + string);
                    if (string.equals(FDN_FOR_ALLOW_DATA)) {
                        this.mIsMatchFdnForAllowData = true;
                        break;
                    } else if (!cursorQuery.moveToNext()) {
                        break;
                    }
                }
            }
            cursorQuery.close();
        }
        if (this.mIsMatchFdnForAllowData) {
            log("match FDN for allow data, call setupDataOnConnectableApns(REASON_FDN_DISABLED)");
            setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_FDN_DISABLED);
        } else {
            log("not match FDN for allow data, call cleanUpAllConnections(REASON_FDN_ENABLED)");
            cleanUpAllConnections(true, MtkGsmCdmaPhone.REASON_FDN_ENABLED);
        }
    }

    private void createWorkerHandler() {
        if (this.mWorkerHandler == null) {
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    MtkDcTracker.this.mWorkerHandler = new WorkerHandler();
                    Looper.loop();
                }
            }.start();
        }
    }

    private class WorkerHandler extends Handler {
        private WorkerHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 270860) {
                MtkDcTracker.this.cleanOrSetupDataConnByCheckFdn();
            }
        }
    }

    private boolean ignoreDataRoaming(String str) {
        boolean zIgnoreDataRoaming;
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        boolean z = false;
        try {
            zIgnoreDataRoaming = this.mDataConnectionExt.ignoreDataRoaming(str);
        } catch (Exception e) {
            loge("get ignoreDataRoaming fail!");
            e.printStackTrace();
            zIgnoreDataRoaming = false;
        }
        boolean z2 = true;
        if (mtkDcHelper.isOperatorMccMnc(MtkDcHelper.Operator.OP156, this.mPhone.getPhoneId())) {
            if (this.mPhone.getServiceState().getIwlanRegState() == 0) {
                z = true;
            }
            log("ignoreDataRoaming: OP156 check apnType = " + str + ", Epdg=" + z);
            if (z && (str.equals("mms") || str.equals("xcap"))) {
                zIgnoreDataRoaming = true;
            }
        }
        if (zIgnoreDataRoaming) {
            logd("ignoreDataRoaming: " + zIgnoreDataRoaming + ", apnType = " + str);
        } else {
            MtkIccCardConstants.VsimType vsimCardType = MtkUiccController.getVsimCardType(this.mPhone.getPhoneId());
            if (vsimCardType == MtkIccCardConstants.VsimType.REMOTE_SIM) {
                log("RSim, set ignoreDataRoaming as true for any apn type");
            } else if (TextUtils.equals(str, "vsim") && vsimCardType == MtkIccCardConstants.VsimType.SOFT_AKA_SIM) {
                log("Aka sim and soft sim, set ignoreDataRoaming as true for vsim type");
            }
            if (!z2) {
                syncDataSettingsToMd(new int[]{-2, 1, -2, -2, -2});
            } else if (!getDataRoamingEnabled()) {
                syncDataSettingsToMd(new int[]{-2, 0, -2, -2, -2});
            }
            return z2;
        }
        z2 = zIgnoreDataRoaming;
        if (!z2) {
        }
        return z2;
    }

    private boolean ignoreDefaultDataUnselected(String str) {
        boolean zIgnoreDefaultDataUnselected;
        try {
            zIgnoreDefaultDataUnselected = this.mDataConnectionExt.ignoreDefaultDataUnselected(str);
        } catch (Exception e) {
            loge("get ignoreDefaultDataUnselected fail!");
            e.printStackTrace();
            zIgnoreDefaultDataUnselected = false;
        }
        if (zIgnoreDefaultDataUnselected) {
            logd("ignoreDefaultDataUnselected: " + zIgnoreDefaultDataUnselected + ", apnType = " + str);
        }
        if (!zIgnoreDefaultDataUnselected && TextUtils.equals(str, "vsim")) {
            log("Vsim is enabled, set ignoreDefaultDataUnselected as true");
            zIgnoreDefaultDataUnselected = true;
        }
        if (zIgnoreDefaultDataUnselected || !TextUtils.equals(str, "preempt")) {
            return zIgnoreDefaultDataUnselected;
        }
        log("Preempt is enabled, set ignoreDefaultDataUnselected as true");
        return true;
    }

    private boolean ignoreDataAllow(String str) {
        boolean z = "ims".equals(str);
        if (!z && TextUtils.equals(str, "vsim")) {
            log("Vsim is enabled, set ignoreDataAllow as true");
            z = true;
        }
        if (z || !TextUtils.equals(str, "preempt")) {
            return z;
        }
        log("Preempt is enabled, set ignoreDataAllow as true");
        return true;
    }

    private boolean getDomesticRoamingEnabled() {
        log("getDomesticRoamingEnabled: isDomesticRoaming=" + isDomesticRoaming() + ", bDomesticRoamingEnabled=" + getDomesticRoamingEnabledBySim());
        return isDomesticRoaming() && getDomesticRoamingEnabledBySim();
    }

    private boolean getIntlRoamingEnabled() {
        log("getIntlRoamingEnabled: isIntlRoaming=" + isIntlRoaming() + ", bIntlRoamingEnabled=" + this.mCcIntlRoamingEnabled);
        return isIntlRoaming() && this.mCcIntlRoamingEnabled;
    }

    private boolean isDomesticRoaming() {
        return this.mPhone.getServiceState().getDataRoamingType() == 2;
    }

    private boolean isIntlRoaming() {
        return this.mPhone.getServiceState().getDataRoamingType() == 3;
    }

    private void onRoamingTypeChanged() {
        boolean dataRoamingEnabled = getDataRoamingEnabled();
        boolean zIsUserDataEnabled = this.mDataEnabledSettings.isUserDataEnabled();
        boolean domesticRoamingEnabledBySim = getDomesticRoamingEnabledBySim();
        boolean z = this.mCcIntlRoamingEnabled;
        boolean z2 = this.mCcUniqueSettingsForRoaming;
        log("onRoamingTypeChanged: bDataOnRoamingEnabled=" + dataRoamingEnabled + ", bUserDataEnabled=" + zIsUserDataEnabled + ", bDomesticSpecialSim=" + domesticRoamingEnabledBySim + ", bIntlSpecialSim=" + z + ", bDomAndIntRoamingFeatureEnabled=" + z2 + ", roamingType=" + this.mPhone.getServiceState().getDataRoamingType());
        if (!domesticRoamingEnabledBySim && !z && !z2) {
            log("onRoamingTypeChanged: is not specific SIM. ignored the request.");
            return;
        }
        if (!this.mPhone.getServiceState().getDataRoaming()) {
            log("onRoamingTypeChanged: device is not roaming. ignored the request.");
            return;
        }
        boolean z3 = false;
        if (z2) {
            if (checkDomesticDataRoamingEnabled() || checkInternationalDataRoamingEnabled()) {
                z3 = true;
            }
        } else if (isDomesticRoaming()) {
            if (domesticRoamingEnabledBySim) {
            }
        } else if (isIntlRoaming()) {
            if (!z ? !(!zIsUserDataEnabled || !dataRoamingEnabled) : dataRoamingEnabled) {
            }
        } else {
            loge("onRoamingTypeChanged error: unexpected roaming type");
        }
        if (z3) {
            log("onRoamingTypeChanged: setup data on roaming");
            setupDataOnConnectableApns("roamingOn");
            notifyDataConnection("roamingOn");
        } else {
            log("onRoamingTypeChanged: Tear down data connection on roaming.");
            cleanUpAllConnections(true, "roamingOn");
            notifyOffApnsOfAvailability("roamingOn");
        }
    }

    private long getDisconnectDoneRetryTimer(String str, long j) {
        if ("apnChanged".equals(str)) {
            return 3000L;
        }
        if (this.mDataConnectionExt != null) {
            try {
                return this.mDataConnectionExt.getDisconnectDoneRetryTimer(str, j);
            } catch (Exception e) {
                loge("DataConnectionExt.getDisconnectDoneRetryTimer fail!");
                e.printStackTrace();
                return j;
            }
        }
        return j;
    }

    private ArrayList<ApnSetting> buildWifiApns(String str) {
        log("buildWifiApns: E requestedApnType=" + str);
        ArrayList<ApnSetting> arrayList = new ArrayList<>();
        if (this.mAllApnSettings != null) {
            log("buildWaitingApns: mAllApnSettings=" + this.mAllApnSettings);
            for (ApnSetting apnSetting : this.mAllApnSettings) {
                if (apnSetting.canHandleType(str) && isWifiOnlyApn(apnSetting.networkTypeBitmask)) {
                    arrayList.add(apnSetting);
                }
            }
        }
        log("buildWifiApns: X apnList=" + arrayList);
        return arrayList;
    }

    private boolean isWifiOnlyApn(int i) {
        return i != 0 && (i & 16646143) == 0;
    }

    public void deactivatePdpByCid(int i) {
        this.mDataServiceManager.deactivateDataCall(i, 1, obtainMessage(270841, i, 0));
    }

    private void onSharedDefaultApnState(int i) {
        logd("onSharedDefaultApnState: newDefaultRefCount = " + i + ", curDefaultRefCount = " + this.mDefaultRefCount);
        if (i != this.mDefaultRefCount) {
            if (i > 1) {
                this.mIsSharedDefaultApn = true;
            } else {
                this.mIsSharedDefaultApn = false;
            }
            this.mDefaultRefCount = i;
            logd("onSharedDefaultApnState: mIsSharedDefaultApn = " + this.mIsSharedDefaultApn);
            broadcastSharedDefaultApnStateChanged(this.mIsSharedDefaultApn);
        }
    }

    public void onSetLteAccessStratumReport(boolean z, Message message) {
        this.mPhone.mCi.setLteAccessStratumReport(z, message);
    }

    public void onSetLteUplinkDataTransfer(int i, Message message) {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if ("default".equals(apnContext.getApnType())) {
                try {
                    this.mPhone.mCi.setLteUplinkDataTransfer(i, apnContext.getDcAc().getCidSync(), message);
                } catch (Exception e) {
                    loge("getDcAc fail!");
                    e.printStackTrace();
                    if (message != null) {
                        AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                        message.sendToTarget();
                    }
                }
            }
        }
    }

    private void notifyLteAccessStratumChanged(int i) {
        String str;
        if (i == 1) {
            str = "connected";
        } else {
            str = "idle";
        }
        this.mLteAccessStratumDataState = str;
        logd("notifyLteAccessStratumChanged mLteAccessStratumDataState = " + this.mLteAccessStratumDataState);
        broadcastLteAccessStratumChanged(this.mLteAccessStratumDataState);
    }

    private void notifyPsNetworkTypeChanged(int i) {
        this.mPhone.getServiceState();
        int iRilRadioTechnologyToNetworkType = ServiceState.rilRadioTechnologyToNetworkType(i);
        logd("notifyPsNetworkTypeChanged mNetworkType = " + this.mNetworkType + ", newNwType = " + iRilRadioTechnologyToNetworkType + ", newRilNwType = " + i);
        if (iRilRadioTechnologyToNetworkType != this.mNetworkType) {
            this.mNetworkType = iRilRadioTechnologyToNetworkType;
            broadcastPsNetworkTypeChanged(this.mNetworkType);
        }
    }

    public String getLteAccessStratumState() {
        return this.mLteAccessStratumDataState;
    }

    public boolean isSharedDefaultApn() {
        return this.mIsSharedDefaultApn;
    }

    private void broadcastLteAccessStratumChanged(String str) {
        Intent intent = new Intent("mediatek.intent.action.LTE_ACCESS_STRATUM_STATE_CHANGED");
        intent.putExtra("lteAccessStratumState", str);
        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE");
    }

    private void broadcastPsNetworkTypeChanged(int i) {
        Intent intent = new Intent("mediatek.intent.action.PS_NETWORK_TYPE_CHANGED");
        intent.putExtra("psNetworkType", i);
        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE");
    }

    private void broadcastSharedDefaultApnStateChanged(boolean z) {
        Intent intent = new Intent("mediatek.intent.action.SHARED_DEFAULT_APN_STATE_CHANGED");
        intent.putExtra("sharedDefaultApn", z);
        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PHONE_STATE");
    }

    private boolean skipDataStallAlarm() {
        int phoneId = this.mPhone.getPhoneId();
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        if (SubscriptionManager.isValidPhoneId(phoneId) && mtkDcHelper != null && mtkDcHelper.isTestIccCard(phoneId)) {
            if (!SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("0")) {
                return true;
            }
        } else if (SystemProperties.get(SKIP_DATA_STALL_ALARM).equals("1")) {
            return true;
        }
        return false;
    }

    private boolean isDataAllowedAsOff(String str) {
        boolean zIsDataAllowedAsOff;
        MtkDcHelper.getInstance();
        if (this.mDataConnectionExt != null) {
            zIsDataAllowedAsOff = this.mDataConnectionExt.isDataAllowedAsOff(str);
        } else {
            zIsDataAllowedAsOff = false;
        }
        if (this.mCcIntlRoamingEnabled) {
            log("isDataAllowedAsOff: getDataRoamingEnabled=" + getDataRoamingEnabled() + ", bIsInternationalRoaming=" + isIntlRoaming());
            if (getDataRoamingEnabled() && isIntlRoaming()) {
                zIsDataAllowedAsOff = true;
            }
        }
        if (zIsDataAllowedAsOff || !TextUtils.equals(str, "vsim") || !MtkUiccController.getVsimCardType(this.mPhone.getPhoneId()).isUserDataAllowed()) {
            return zIsDataAllowedAsOff;
        }
        log("Vsim is enabled, set isDataAllowedAsOff true");
        return true;
    }

    private boolean getDomesticDataRoamingEnabledFromSettings() {
        int phoneId = this.mPhone.getPhoneId();
        boolean z = false;
        try {
            if (TelephonyManager.getIntAtIndex(this.mResolver, "domestic_data_roaming", phoneId) != 0) {
                z = true;
            }
        } catch (Settings.SettingNotFoundException e) {
            log("getDomesticDataRoamingEnabled: SettingNofFoundException snfe=" + e);
        }
        if (VDBG) {
            log("getDomesticDataRoamingEnabled: phoneId=" + phoneId + " isDomDataRoamingEnabled=" + z);
        }
        return z;
    }

    private boolean getInternationalDataRoamingEnabledFromSettings() {
        int phoneId = this.mPhone.getPhoneId();
        boolean z = true;
        try {
            if (TelephonyManager.getIntAtIndex(this.mResolver, "international_data_roaming", phoneId) == 0) {
                z = false;
            }
        } catch (Settings.SettingNotFoundException e) {
            log("getInternationalDataRoamingEnabled: SettingNofFoundException snfe=" + e);
        }
        if (VDBG) {
            log("getInternationalDataRoamingEnabled: phoneId=" + phoneId + " isIntDataRoamingEnabled=" + z);
        }
        return z;
    }

    private boolean isDataRoamingTypeAllowed() {
        boolean z = false;
        if (this.mCcUniqueSettingsForRoaming) {
            boolean domesticDataRoamingEnabledFromSettings = getDomesticDataRoamingEnabledFromSettings();
            boolean internationalDataRoamingEnabledFromSettings = getInternationalDataRoamingEnabledFromSettings();
            log("isDataRoamingTypeAllowed bDomDataOnRoamingEnabled=" + domesticDataRoamingEnabledFromSettings + ", bIntDataOnRoamingEnabled=" + internationalDataRoamingEnabledFromSettings + ", getDataRoaming=" + this.mPhone.getServiceState().getDataRoaming() + ", currentRoamingType=" + this.mPhone.getServiceState().getDataRoamingType() + ", mUserDataEnabled=" + this.mDataEnabledSettings.isUserDataEnabled());
            if (!this.mPhone.getServiceState().getDataRoaming() || ((domesticDataRoamingEnabledFromSettings && isDomesticRoaming()) || (internationalDataRoamingEnabledFromSettings && isIntlRoaming()))) {
                z = true;
            }
        }
        log("isDataRoamingTypeAllowed : " + z);
        return z;
    }

    public boolean getPendingDataCallFlag() {
        return this.mPendingDataCall;
    }

    private boolean isLocatedPlmnChanged() {
        if (this.mPhone.getPhoneType() == 2) {
            return false;
        }
        return ((MtkServiceStateTracker) this.mPhone.getServiceStateTracker()).willLocatedPlmnChange();
    }

    private void onProcessPendingSetupData() {
        setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_RESUME_PENDING_DATA);
    }

    public void processPendingSetupData(MtkServiceStateTracker mtkServiceStateTracker) {
        this.mPendingDataCall = false;
        sendMessage(obtainMessage(270842));
    }

    public int getClassType(ApnSetting apnSetting) {
        int i = 3;
        if (ArrayUtils.contains(apnSetting.types, "emergency") || VZW_EMERGENCY_NI.compareToIgnoreCase(apnSetting.apn) == 0) {
            i = 0;
        } else if (ArrayUtils.contains(apnSetting.types, "ims") || VZW_IMS_NI.compareToIgnoreCase(apnSetting.apn) == 0) {
            i = 1;
        } else if (VZW_ADMIN_NI.compareToIgnoreCase(apnSetting.apn) == 0) {
            i = 2;
        } else if (VZW_APP_NI.compareToIgnoreCase(apnSetting.apn) == 0) {
            i = 4;
        } else if (VZW_800_NI.compareToIgnoreCase(apnSetting.apn) == 0) {
            i = 5;
        } else if (!ArrayUtils.contains(apnSetting.types, "default")) {
            log("getClassType: set to default class 3");
        }
        logd("getClassType:" + i);
        return i;
    }

    public ApnSetting getClassTypeApn(int i) {
        String str;
        ApnSetting apnSetting = null;
        if (i == 0) {
            str = VZW_EMERGENCY_NI;
        } else if (1 == i) {
            str = VZW_IMS_NI;
        } else if (2 == i) {
            str = VZW_ADMIN_NI;
        } else if (3 == i) {
            str = VZW_INTERNET_NI;
        } else if (4 == i) {
            str = VZW_APP_NI;
        } else {
            if (5 != i) {
                log("getClassTypeApn: can't handle class:" + i);
                return null;
            }
            str = VZW_800_NI;
        }
        if (this.mAllApnSettings != null) {
            for (ApnSetting apnSetting2 : this.mAllApnSettings) {
                if (str.compareToIgnoreCase(apnSetting2.apn) == 0) {
                    apnSetting = apnSetting2;
                }
            }
        }
        logd("getClassTypeApn:" + apnSetting + ", class:" + i);
        return apnSetting;
    }

    private void handleSetResume() {
        if (SubscriptionManager.isValidPhoneId(this.mPhone.getPhoneId())) {
            this.mPhone.mCi.setResumeRegistration(this.mSuspendId, null);
        }
    }

    private void handleRegistrationSuspend(AsyncResult asyncResult) {
        if (asyncResult.exception == null && asyncResult.result != null) {
            log("handleRegistrationSuspend: createAllApnList and set initial attach APN");
            this.mSuspendId = ((int[]) asyncResult.result)[0];
            log("handleRegistrationSuspend: suspending with Id=" + this.mSuspendId);
            synchronized (this.mNeedsResumeModemLock) {
                this.mNeedsResumeModem = true;
            }
            createAllApnList();
            setInitialAttachApn();
            return;
        }
        log("handleRegistrationSuspend: AsyncResult is wrong " + asyncResult.exception);
    }

    private void handlePlmnChange(AsyncResult asyncResult) {
        if (asyncResult.exception == null && asyncResult.result != null) {
            String[] strArr = (String[]) asyncResult.result;
            for (int i = 0; i < strArr.length; i++) {
                logd("plmnString[" + i + "]=" + strArr[i]);
            }
            this.mRegion = getRegion(strArr[0]);
            if (!TextUtils.isEmpty(mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get())) && !isNeedToResumeMd() && this.mPhone.getPhoneId() == SubscriptionManager.getPhoneId(SubscriptionController.getInstance().getDefaultDataSubId())) {
                logd("handlePlmnChange: createAllApnList and set initial attach APN");
                createAllApnList();
                setInitialAttachApn();
                return;
            }
            logd("No need to update APN for Operator");
            return;
        }
        log("AsyncResult is wrong " + asyncResult.exception);
    }

    private int getRegion(String str) {
        if (str != null && !str.equals("") && str.length() >= 5) {
            String strSubstring = str.substring(0, 3);
            for (String str2 : MCC_TABLE_TEST) {
                if (strSubstring.equals(str2)) {
                    logd("[getRegion] Test PLMN");
                    return 0;
                }
            }
            String[] strArr = MCC_TABLE_DOMESTIC;
            if (strArr.length > 0) {
                if (strSubstring.equals(strArr[0])) {
                    logd("[getRegion] REGION_DOMESTIC");
                    return 1;
                }
                logd("[getRegion] REGION_FOREIGN");
                return 2;
            }
            logd("[getRegion] REGION_UNKNOWN");
            return 0;
        }
        logd("[getRegion] Invalid PLMN");
        return 0;
    }

    public boolean getImsEnabled() {
        return false;
    }

    public boolean checkIfDomesticInitialAttachApn(String str) {
        boolean z;
        String[] strArr = MCC_TABLE_DOMESTIC;
        int length = strArr.length;
        int i = 0;
        while (true) {
            if (i < length) {
                if (!str.equals(strArr[i])) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } else {
                z = false;
                break;
            }
        }
        if (isOp17IaSupport() && z) {
            return getImsEnabled() && this.mRegion == 1;
        }
        if (enableOpIA()) {
            return this.mRegion == 1;
        }
        log("checkIfDomesticInitialAttachApn: Not OP129 or MCC is not in domestic for OP129");
        return true;
    }

    public boolean enableOpIA() {
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        if (TextUtils.isEmpty(strMtkGetOperatorNumeric)) {
            return false;
        }
        String strSubstring = strMtkGetOperatorNumeric.substring(0, 3);
        log("enableOpIA: currentMcc = " + strSubstring);
        String[] strArr = MCC_TABLE_DOMESTIC;
        int length = strArr.length;
        for (int i = 0; i < length; i++) {
            if (strSubstring.equals(strArr[i])) {
                return true;
            }
        }
        return false;
    }

    private void onAttachApnChangedByHandover(boolean z) {
        this.mIsImsHandover = z;
        log("onAttachApnChangedByHandover: mIsImsHandover = " + this.mIsImsHandover);
        this.mPhone.mCi.setPropImsHandover(this.mIsImsHandover ? "1" : MtkGsmCdmaPhone.ACT_TYPE_UTRAN, null);
        setInitialAttachApn();
    }

    private boolean isOp17IaSupport() {
        return TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), "vendor.gsm.ril.sim.op17", "0").equals("1");
    }

    private boolean isOp129IaSupport() {
        return SystemProperties.get("vendor.gsm.ril.sim.op129").equals("1");
    }

    private boolean isNeedToResumeMd() {
        return SystemProperties.get("vendor.gsm.ril.data.op.suspendmd").equals("1");
    }

    private boolean isOp18Sim() {
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        if (strMtkGetOperatorNumeric != null) {
            for (int i = 0; i < this.MCCMNC_OP18.length; i++) {
                if (strMtkGetOperatorNumeric.startsWith(this.MCCMNC_OP18[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasOperatorIaCapability() {
        if (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasOperatorIaCapability()) {
            return false;
        }
        log("hasOpIaCapability: true");
        return true;
    }

    private void onAllowChanged(boolean z) {
        log("onAllowChanged: Allow = " + z);
        this.mAllowConfig = z;
        if (z) {
            setupDataOnConnectableApns(MtkGsmCdmaPhone.REASON_DATA_ALLOWED);
        }
    }

    protected boolean getAllowConfig() {
        if (MtkDcHelper.getInstance().isMultiPsAttachSupport() && !hasModemDeactPdnCapabilityForMultiPS()) {
            return this.mAllowConfig;
        }
        return true;
    }

    public void onVoiceCallStartedEx() {
        log("onVoiceCallStartedEx");
        this.mInVoiceCall = true;
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        this.mIsSupportConcurrent = mtkDcHelper == null ? false : mtkDcHelper.isDataSupportConcurrent(this.mPhone.getPhoneId());
        if (isConnected() && !this.mIsSupportConcurrent) {
            log("onVoiceCallStarted stop polling");
            stopNetStatPoll();
            stopDataStallAlarm();
            notifyDataConnection("2GVoiceCallStarted");
        }
        notifyVoiceCallEventToDataConnection(this.mInVoiceCall, this.mIsSupportConcurrent);
    }

    public void onVoiceCallEndedEx() {
        log("onVoiceCallEndedEx");
        boolean zIsDataSupportConcurrent = false;
        this.mInVoiceCall = false;
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        if (isConnected()) {
            if (!this.mIsSupportConcurrent) {
                startNetStatPoll();
                startDataStallAlarm(false);
                notifyDataConnection("2GVoiceCallEnded");
            } else {
                resetPollStats();
            }
        }
        if (MtkDcHelper.MTK_SVLTE_SUPPORT) {
            if (mtkDcHelper != null) {
                zIsDataSupportConcurrent = mtkDcHelper.isDataSupportConcurrent(this.mPhone.getPhoneId());
            }
            this.mIsSupportConcurrent = zIsDataSupportConcurrent;
            if (mtkDcHelper != null && !mtkDcHelper.isAllCallingStateIdle()) {
                this.mInVoiceCall = true;
                log("SVLTE denali dual call one end, left one call.");
            }
        }
        setupDataOnConnectableApns("2GVoiceCallEnded");
        notifyVoiceCallEventToDataConnection(this.mInVoiceCall, this.mIsSupportConcurrent);
    }

    private void notifyVoiceCallEventToDataConnection(boolean z, boolean z2) {
        logd("notifyVoiceCallEventToDataConnection: bInVoiceCall = " + z + ", bSupportConcurrent = " + z2);
        Iterator it = this.mDataConnectionAcHashMap.values().iterator();
        while (it.hasNext()) {
            ((MtkDcAsyncChannel) ((DcAsyncChannel) it.next())).notifyVoiceCallEvent(z, z2);
        }
    }

    private boolean getDomesticRoamingEnabledBySim() {
        if (this.mCcDomesticRoamingEnabled) {
            if (this.mCcDomesticRoamingSpecifiedNw != null) {
                return ArrayUtils.contains(this.mCcDomesticRoamingSpecifiedNw, TelephonyManager.getDefault().getNetworkOperatorForPhone(this.mPhone.getPhoneId()));
            }
            return true;
        }
        return false;
    }

    private boolean isImsApnSettingChanged(ArrayList<ApnSetting> arrayList, ArrayList<ApnSetting> arrayList2) {
        String imsApnSetting = getImsApnSetting(arrayList);
        String imsApnSetting2 = getImsApnSetting(arrayList2);
        if (!imsApnSetting.isEmpty() && !TextUtils.equals(imsApnSetting, imsApnSetting2)) {
            return true;
        }
        return false;
    }

    private String getImsApnSetting(ArrayList<ApnSetting> arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ApnSetting apnSetting : arrayList) {
            if (apnSetting.canHandleType("ims")) {
                sb.append(((MtkApnSetting) apnSetting).toStringIgnoreName(true));
            }
        }
        log("getImsApnSetting, apnsToStringIgnoreName: sb = " + sb.toString());
        return sb.toString();
    }

    private boolean checkDomesticDataRoamingEnabled() {
        log("checkDomesticDataRoamingEnabled: getDomesticDataRoamingFromSettings=" + getDomesticDataRoamingEnabledFromSettings() + ", isDomesticRoaming=" + isDomesticRoaming());
        return getDomesticDataRoamingEnabledFromSettings() && isDomesticRoaming();
    }

    private boolean checkInternationalDataRoamingEnabled() {
        log("checkInternationalDataRoamingEnabled: getInternationalDataRoamingFromSettings=" + getInternationalDataRoamingEnabledFromSettings() + ", isIntlRoaming=" + isIntlRoaming());
        return getInternationalDataRoamingEnabledFromSettings() && isIntlRoaming();
    }

    private boolean hasModemDeactPdnCapabilityForMultiPS() {
        if (!this.mHasFetchModemDeactPdnCapabilityForMultiPS) {
            if (this.mTelDevController != null && this.mTelDevController.getModem(0) != null && ((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasModemDeactPdnCapabilityForMultiPS()) {
                this.mModemDeactPdnCapabilityForMultiPS = true;
            } else {
                this.mModemDeactPdnCapabilityForMultiPS = false;
            }
            this.mHasFetchModemDeactPdnCapabilityForMultiPS = true;
            log("hasModemDeactPdnCapabilityForMultiPS: " + this.mModemDeactPdnCapabilityForMultiPS);
        }
        return this.mModemDeactPdnCapabilityForMultiPS;
    }

    private void teardownDataByEmergencyPolicy() {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        } else {
            configForSubId = null;
        }
        if (configForSubId != null) {
            for (String str : configForSubId.getStringArray("emergency_bearer_management_policy")) {
                Iterator it = this.mApnContexts.values().iterator();
                while (true) {
                    if (it.hasNext()) {
                        ApnContext apnContext = (ApnContext) it.next();
                        if (!apnContext.isDisconnected()) {
                            ApnSetting apnSetting = apnContext.getApnSetting();
                            log("compare apn: " + apnSetting.apn + " by filter: " + str);
                            if (apnSetting.apn.equalsIgnoreCase(str)) {
                                apnContext.setReason("pdnOccupied");
                                cleanUpConnection(true, apnContext);
                                break;
                            }
                        }
                    }
                }
            }
            return;
        }
        loge("Couldn't find CarrierConfigService.");
    }

    private void onDataEnabledSettings(boolean z, int i) {
        log("onDataEnabledSettings: enabled=" + z + ", reason=" + i);
        if (i == 2) {
            if (!getDataRoamingEnabled() && this.mPhone.getServiceState().getDataRoaming()) {
                if (z) {
                    notifyOffApnsOfAvailability("roamingOn");
                } else {
                    notifyOffApnsOfAvailability("dataDisabled");
                }
            }
            this.mPhone.notifyUserMobileDataStateChanged(z);
            if (z) {
                reevaluateDataConnections();
                onTrySetupData("dataEnabled");
                return;
            }
            for (ApnContext apnContext : this.mApnContexts.values()) {
                if (!isDataAllowedAsOff(apnContext.getApnType())) {
                    apnContext.setReason("specificDisabled");
                    onCleanUpConnection(true, ApnContext.apnIdForApnName(apnContext.getApnType()), "specificDisabled");
                }
            }
        }
    }

    private boolean isApnSettingExist(ApnSetting apnSetting) {
        if (apnSetting != null && this.mAllApnSettings != null && !this.mAllApnSettings.isEmpty()) {
            for (ApnSetting apnSetting2 : this.mAllApnSettings) {
                if (TextUtils.equals(((MtkApnSetting) apnSetting).toStringIgnoreName(false), ((MtkApnSetting) apnSetting2).toStringIgnoreName(false))) {
                    log("isApnSettingExist: " + apnSetting2);
                    return true;
                }
            }
        }
        return false;
    }

    protected void mtkCopyHandlerThread(HandlerThread handlerThread) {
        this.mDcHandlerThread = handlerThread;
    }

    private static Bundle updateTxRxSumEx() {
        boolean z;
        long mobileRxPackets;
        String simOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumeric(SubscriptionManager.getDefaultDataSubscriptionId());
        long mobileTxPackets = 0;
        if (simOperatorNumeric != null) {
            for (int i = 0; i < PRIVATE_APN_OPERATOR.length; i++) {
                if (simOperatorNumeric.startsWith(PRIVATE_APN_OPERATOR[i])) {
                    mobileTxPackets = TrafficStats.getMobileTxPackets();
                    mobileRxPackets = TrafficStats.getMobileRxPackets();
                    z = true;
                    break;
                }
            }
            z = false;
            mobileRxPackets = 0;
        } else {
            z = false;
            mobileRxPackets = 0;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("useEx", z);
        bundle.putLong("txPkts", mobileTxPackets);
        bundle.putLong("rxPkts", mobileRxPackets);
        return bundle;
    }

    protected long mtkModifyInterApnDelay(long j, String str) {
        if ("vsim".equals(str)) {
            return 1000L;
        }
        return j;
    }

    protected boolean mtkIsApplyNewStateOnDisable(boolean z) {
        return !z;
    }

    protected boolean mtkApplyNewStateOnDisable(boolean z, ApnContext apnContext) {
        apnContext.setReason("dataDisabled");
        return true;
    }

    protected boolean mtkSkipTearDownAll() {
        log("dataConnectionNotInUse: not in use return true");
        return true;
    }

    protected void mtkAddVsimApnTypeToDefaultApnSetting() {
        int i;
        MtkDcTracker mtkDcTracker = this;
        if (ExternalSimManager.isNonDsdaRemoteSimSupport() && mtkDcTracker.mAllApnSettings != null) {
            int i2 = 0;
            int i3 = 0;
            while (i2 < mtkDcTracker.mAllApnSettings.size()) {
                ApnSetting apnSetting = (ApnSetting) mtkDcTracker.mAllApnSettings.get(i2);
                if (apnSetting.canHandleType("default")) {
                    String[] strArr = (String[]) Arrays.copyOf(apnSetting.types, apnSetting.types.length + 1);
                    strArr[strArr.length - 1] = "vsim";
                    if (apnSetting instanceof MtkApnSetting) {
                        i3 = ((MtkApnSetting) apnSetting).inactiveTimer;
                    }
                    int i4 = i3;
                    MtkApnSetting mtkApnSetting = new MtkApnSetting(apnSetting.id, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, apnSetting.proxy, apnSetting.port, apnSetting.mmsc, apnSetting.mmsProxy, apnSetting.mmsPort, apnSetting.user, apnSetting.password, apnSetting.authType, strArr, apnSetting.protocol, apnSetting.roamingProtocol, apnSetting.carrierEnabled, apnSetting.networkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId, i4);
                    mtkDcTracker = this;
                    i = i2;
                    mtkDcTracker.mAllApnSettings.set(i, mtkApnSetting);
                    i3 = i4;
                } else {
                    i = i2;
                }
                i2 = i + 1;
            }
        }
    }

    protected void mtkAddPreemptApnTypeToDefaultApnSetting() {
        MtkDcTracker mtkDcTracker = this;
        int i = 0;
        int i2 = 1;
        if (SystemProperties.getInt("persist.vendor.radio.smart.data.switch", 0) == 1 && mtkDcTracker.mAllApnSettings != null) {
            int i3 = 0;
            while (i < mtkDcTracker.mAllApnSettings.size()) {
                ApnSetting apnSetting = (ApnSetting) mtkDcTracker.mAllApnSettings.get(i);
                if (apnSetting.canHandleType("default")) {
                    String[] strArr = (String[]) Arrays.copyOf(apnSetting.types, apnSetting.types.length + i2);
                    strArr[strArr.length - i2] = "preempt";
                    if (apnSetting instanceof MtkApnSetting) {
                        i3 = ((MtkApnSetting) apnSetting).inactiveTimer;
                    }
                    int i4 = i3;
                    mtkDcTracker = this;
                    i = i;
                    mtkDcTracker.mAllApnSettings.set(i, new MtkApnSetting(apnSetting.id, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, apnSetting.proxy, apnSetting.port, apnSetting.mmsc, apnSetting.mmsProxy, apnSetting.mmsPort, apnSetting.user, apnSetting.password, apnSetting.authType, strArr, apnSetting.protocol, apnSetting.roamingProtocol, apnSetting.carrierEnabled, apnSetting.networkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId, i4));
                    i3 = i4;
                }
                i++;
                i2 = 1;
            }
        }
    }

    protected boolean mtkSkipNotifyOnRadioOffOrNotAvailable() {
        this.mMdChangedAttachApn = null;
        SystemProperties.set(PROP_APN_CLASS_ICCID + this.mPhone.getPhoneId(), "");
        SystemProperties.set(PROP_APN_CLASS + this.mPhone.getPhoneId(), "");
        return true;
    }

    protected boolean mtkIsNeedNotify(ApnContext apnContext) {
        if (TextUtils.equals(apnContext.getApnType(), "preempt") && SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId()) == this.mPhone.getPhoneId()) {
            return false;
        }
        return ((MtkApnContext) apnContext).isNeedNotify();
    }

    protected boolean mtkSkipImsOrEmergencyPdn(ApnContext apnContext) {
        MtkDcHelper mtkDcHelper = MtkDcHelper.getInstance();
        ApnSetting apnSetting = apnContext.getApnSetting();
        if (mtkDcHelper != null && apnSetting != null && MtkDcHelper.isImsOrEmergencyApn(apnSetting.types)) {
            log("reevaluateDataConnections: ignore ImsOrEmergency pdn");
            return true;
        }
        return false;
    }

    protected String[] mtkGetDunApnByMccMnc(Context context, String[] strArr) {
        return getDunApnByMccMnc(context);
    }

    protected boolean mtkIgnoredPermanentFailure(DcFailCause dcFailCause) {
        boolean zIsIgnoredCause;
        String strMtkGetOperatorNumeric;
        if (this.mDataConnectionExt != null) {
            try {
                zIsIgnoredCause = this.mDataConnectionExt.isIgnoredCause(dcFailCause);
            } catch (Exception e) {
                logd("mDataConnectionExt.isIgnoredCause exception");
                e.printStackTrace();
                zIsIgnoredCause = false;
            }
        } else {
            zIsIgnoredCause = false;
        }
        if (dcFailCause == DcFailCause.MTK_TCM_ESM_TIMER_TIMEOUT && (strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get())) != null) {
            for (int i = 0; i < KDDI_OPERATOR.length; i++) {
                if (strMtkGetOperatorNumeric.startsWith(KDDI_OPERATOR[i])) {
                    return true;
                }
            }
            return zIsIgnoredCause;
        }
        return zIsIgnoredCause;
    }

    protected String mtkGetEmergencyApnSelection(String str) {
        return str + " and numeric=''";
    }

    private void isDataAllowedForRoamingFeature(DataConnectionReasons dataConnectionReasons) {
        if (this.mCcUniqueSettingsForRoaming) {
            if (dataConnectionReasons.contains(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED)) {
                dataConnectionReasons.mDataDisallowedReasonSet.remove(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED);
            }
            if (!isDataRoamingTypeAllowed()) {
                dataConnectionReasons.add(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED);
            }
        }
    }

    private void onDedecatedBearerActivated(MtkDedicateDataCallResponse mtkDedicateDataCallResponse) {
        log("onDedecatedBearerActivated, dataInfo: " + mtkDedicateDataCallResponse);
        notifyDedicateDataConnection(mtkDedicateDataCallResponse.mCid, DctConstants.State.CONNECTED, mtkDedicateDataCallResponse, DcFailCause.NONE, MtkDedicateDataCallResponse.REASON_BEARER_ACTIVATION);
    }

    private void onDedecatedBearerModified(MtkDedicateDataCallResponse mtkDedicateDataCallResponse) {
        log("onDedecatedBearerModified, dataInfo: " + mtkDedicateDataCallResponse);
        notifyDedicateDataConnection(mtkDedicateDataCallResponse.mCid, DctConstants.State.CONNECTED, mtkDedicateDataCallResponse, DcFailCause.NONE, MtkDedicateDataCallResponse.REASON_BEARER_MODIFICATION);
    }

    private void onDedecatedBearerDeactivated(int i) {
        log("onDedecatedBearerDeactivated, Cid: " + i);
        notifyDedicateDataConnection(i, DctConstants.State.IDLE, null, DcFailCause.NONE, MtkDedicateDataCallResponse.REASON_BEARER_DEACTIVATION);
    }

    private void notifyDedicateDataConnection(int i, DctConstants.State state, MtkDedicateDataCallResponse mtkDedicateDataCallResponse, DcFailCause dcFailCause, String str) {
        log("notifyDedicateDataConnection ddcId=" + i + ", state=" + state + ", failCause=" + dcFailCause + ", reason=" + str + ", dataInfo=" + mtkDedicateDataCallResponse);
        Intent intent = new Intent("com.mediatek.phone.ACTION_ANY_DEDICATE_DATA_CONNECTION_STATE_CHANGED");
        intent.putExtra("DdcId", i);
        if (mtkDedicateDataCallResponse != null && mtkDedicateDataCallResponse.mCid >= 0) {
            intent.putExtra("linkProperties", mtkDedicateDataCallResponse);
        }
        intent.putExtra("state", (Serializable) state);
        intent.putExtra("cause", dcFailCause.getErrorCode());
        intent.putExtra(DataSubConstants.EXTRA_MOBILE_DATA_ENABLE_REASON, str);
        intent.putExtra("phone", this.mPhone.getPhoneId());
        this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.READ_PRECISE_PHONE_STATE");
    }

    protected boolean mtkSkipSetDataProfileAsNeeded(String str) {
        if (str == null) {
            return true;
        }
        return false;
    }

    protected int getRecoveryAction() {
        int i = Settings.System.getInt(this.mResolver, "radio.data.stall.recovery.action" + String.valueOf(this.mPhone.getPhoneId()), 0);
        if (VDBG_STALL) {
            log("getRecoveryAction: " + i);
        }
        return i;
    }

    protected void putRecoveryAction(int i) {
        Settings.System.putInt(this.mResolver, "radio.data.stall.recovery.action" + String.valueOf(this.mPhone.getPhoneId()), i);
        if (VDBG_STALL) {
            log("putRecoveryAction: " + i);
        }
    }

    protected boolean mtkSkipCleanUpConnectionsOnUpdatedApns(ApnContext apnContext) {
        if (apnContext.getWaitingApns() == null) {
            return true;
        }
        return false;
    }

    private void loadCarrierConfig(int i) {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager != null) {
            configForSubId = carrierConfigManager.getConfigForSubId(i);
        } else {
            configForSubId = null;
        }
        if (configForSubId != null) {
            this.mCcDomesticRoamingEnabled = configForSubId.getBoolean("mtk_domestic_roaming_enabled_only_by_mobile_data_setting");
            this.mCcDomesticRoamingSpecifiedNw = configForSubId.getStringArray("mtk_domestic_roaming_enabled_only_by_mobile_data_setting_check_nw_plmn");
            this.mCcIntlRoamingEnabled = configForSubId.getBoolean("mtk_intl_roaming_enabled_only_by_roaming_data_setting");
            this.mCcUniqueSettingsForRoaming = configForSubId.getBoolean("mtk_unique_settings_for_domestic_and_intl_roaming");
            this.mIsAddMnoApnsIntoAllApnList = configForSubId.getBoolean("mtk_key_add_mnoapns_into_allapnlist");
            StringBuilder sb = new StringBuilder();
            sb.append("loadCarrierConfig: DomesticRoamingEnabled ");
            sb.append(this.mCcDomesticRoamingEnabled);
            sb.append(", SpecifiedNw ");
            sb.append(this.mCcDomesticRoamingSpecifiedNw != null);
            sb.append("; IntlRoamingEnabled ");
            sb.append(this.mCcIntlRoamingEnabled);
            sb.append("; UniqueSettingsForRoaming ");
            sb.append(this.mCcUniqueSettingsForRoaming);
            log(sb.toString());
        }
    }

    private boolean hasMdAutoSetupImsCapability() {
        if (!this.mHasFetchMdAutoSetupImsCapability) {
            if (this.mTelDevController != null && this.mTelDevController.getModem(0) != null && ((MtkHardwareConfig) this.mTelDevController.getModem(0)).hasMdAutoSetupImsCapability()) {
                this.mMdAutoSetupImsCapability = true;
            }
            this.mHasFetchMdAutoSetupImsCapability = true;
            logd("hasMdAutoSetupImsCapability: " + this.mMdAutoSetupImsCapability);
        }
        return this.mMdAutoSetupImsCapability;
    }

    protected ArrayList<ApnSetting> mtkAddMnoApnsIntoAllApnList(ArrayList<ApnSetting> arrayList, ArrayList<ApnSetting> arrayList2) {
        if (this.mIsAddMnoApnsIntoAllApnList) {
            log("mtkAddMnoApnsIntoAllApnList: mnoApns=" + arrayList2);
            arrayList.addAll(arrayList2);
        }
        return arrayList;
    }

    private void reloadOpCustomizationFactory() {
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext());
            this.mDataConnectionExt = this.mTelephonyCustomizationFactory.makeDataConnectionExt(this.mPhone.getContext());
            this.mDataConnectionExt.stopDataRoamingStrategy();
            this.mDataConnectionExt.startDataRoamingStrategy(this.mPhone);
        } catch (Exception e) {
            log("mDataConnectionExt init fail");
            e.printStackTrace();
        }
    }

    private boolean isSimActivated() {
        String str = SystemProperties.get(PROP_RIL_DATA_GID1 + this.mPhone.getPhoneId(), "");
        log("gid1: " + str);
        if (GID1_DEFAULT.compareToIgnoreCase(str) == 0) {
            return false;
        }
        return true;
    }

    private boolean hasConnectedOrConnectingMeteredApn() {
        for (ApnContext apnContext : this.mApnContexts.values()) {
            if (apnContext.isConnectedOrConnecting() && ApnSetting.isMeteredApnType(apnContext.getApnType(), this.mPhone)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mtkIsNeedRegisterSettingsObserver(int i, int i2) {
        log("mtkIsNeedRegisterSettingsObserver: pSubId=" + i + ", subId=" + i2);
        return i != i2;
    }

    private ApnSetting encodeInactiveTimer(ApnSetting apnSetting) {
        if (apnSetting == null) {
            loge("encodeInactiveTimer apn is null");
            return null;
        }
        if (apnSetting.authType > 7 || apnSetting.authType < -1) {
            loge("encodeInactiveTimer invalid authType: " + apnSetting.authType);
        } else if (apnSetting instanceof MtkApnSetting) {
            MtkApnSetting mtkApnSetting = (MtkApnSetting) apnSetting;
            int i = 536870911;
            int i2 = 0;
            if (mtkApnSetting.inactiveTimer >= 0) {
                if (mtkApnSetting.inactiveTimer <= 536870911) {
                    i = mtkApnSetting.inactiveTimer;
                }
            } else {
                i = 0;
            }
            if (apnSetting.authType == -1) {
                if (!TextUtils.isEmpty(apnSetting.user)) {
                    i2 = 3;
                }
            } else {
                i2 = apnSetting.authType;
            }
            return new ApnSetting(apnSetting.id, apnSetting.numeric, apnSetting.carrier, apnSetting.apn, apnSetting.proxy, apnSetting.port, apnSetting.mmsc, apnSetting.mmsProxy, apnSetting.mmsPort, apnSetting.user, apnSetting.password, i2 + (i << 3), apnSetting.types, apnSetting.protocol, apnSetting.roamingProtocol, apnSetting.carrierEnabled, apnSetting.networkTypeBitmask, apnSetting.profileId, apnSetting.modemCognitive, apnSetting.maxConns, apnSetting.waitTime, apnSetting.maxConnsTime, apnSetting.mtu, apnSetting.mvnoType, apnSetting.mvnoMatchData, apnSetting.apnSetId);
        }
        return apnSetting;
    }

    protected String mtkGetOperatorNumeric(IccRecords iccRecords) {
        String operatorNumeric;
        if (this.mPhoneType == 2) {
            operatorNumeric = SystemProperties.get(PROP_RIL_DATA_CDMA_MCC_MNC + this.mPhone.getPhoneId());
        } else {
            operatorNumeric = SystemProperties.get(PROP_RIL_DATA_GSM_MCC_MNC + this.mPhone.getPhoneId());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("mtkGetOperatorNumeric: operator from IccRecords = ");
        sb.append(iccRecords != null ? iccRecords.getOperatorNumeric() : "");
        sb.append(", operator from RIL = ");
        sb.append(operatorNumeric);
        log(sb.toString());
        if (TextUtils.isEmpty(operatorNumeric)) {
            operatorNumeric = iccRecords != null ? iccRecords.getOperatorNumeric() : "";
        }
        if (this.mDataConnectionExt != null) {
            return this.mDataConnectionExt.getOperatorNumericFromImpi(operatorNumeric, this.mPhone.getPhoneId());
        }
        return operatorNumeric;
    }

    protected boolean mtkMvnoMatches(String str, String str2) {
        String str3;
        String str4;
        String spnToString;
        log("mvnoMatchData=" + str2);
        if (str.equalsIgnoreCase("spn")) {
            if (this.mPhoneType == 2) {
                str4 = SystemProperties.get(PROP_RIL_DATA_CDMA_SPN + this.mPhone.getPhoneId(), "");
            } else {
                str4 = SystemProperties.get(PROP_RIL_DATA_GSM_SPN + this.mPhone.getPhoneId(), "");
            }
            if (str4.length() == 0) {
                return false;
            }
            if (this.mPhoneType == 2) {
                spnToString = MtkIccUtilsEx.parseSpnToString(2, IccUtils.hexStringToBytes(str4));
            } else {
                spnToString = MtkIccUtilsEx.parseSpnToString(1, IccUtils.hexStringToBytes(str4));
            }
            log("strSpn=" + spnToString);
            if (spnToString != null && spnToString.equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("imsi")) {
            if (this.mPhoneType == 2) {
                str3 = SystemProperties.get(PROP_RIL_DATA_CDMA_IMSI + this.mPhone.getPhoneId(), "");
            } else {
                str3 = SystemProperties.get(PROP_RIL_DATA_GSM_IMSI + this.mPhone.getPhoneId(), "");
            }
            if (str3 != null && ApnSetting.imsiMatches(str2, str3)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("gid")) {
            String str5 = SystemProperties.get(PROP_RIL_DATA_GID1 + this.mPhone.getPhoneId(), "");
            log("gid1=" + str5);
            int length = str2.length();
            if (str5 != null && str5.length() >= length && str5.substring(0, length).equalsIgnoreCase(str2)) {
                return true;
            }
        } else if (str.equalsIgnoreCase("pnn")) {
            String str6 = SystemProperties.get(PROP_RIL_DATA_PNN + this.mPhone.getPhoneId(), "");
            if (str6.length() == 0) {
                return false;
            }
            String pnnToString = MtkIccUtilsEx.parsePnnToString(IccUtils.hexStringToBytes(str6));
            log("strPnn=" + pnnToString);
            if (pnnToString != null && pnnToString.equalsIgnoreCase(str2)) {
                return true;
            }
        }
        return false;
    }

    public void requestNetwork(NetworkRequest networkRequest, LocalLog localLog) {
        int iApnIdForNetworkRequest = ApnContext.apnIdForNetworkRequest(networkRequest);
        ApnContext apnContext = (ApnContext) this.mApnContextsById.get(iApnIdForNetworkRequest);
        localLog.log("DcTracker.requestNetwork for " + networkRequest + " found " + apnContext);
        if (apnContext != null) {
            boolean z = false;
            if (iApnIdForNetworkRequest == 0) {
                if (MTK_CC33_SUPPORT) {
                    ApnSetting apnSetting = apnContext.getApnSetting();
                    if (apnSetting != null && apnSetting.permanentFailed) {
                        log("requestNetwork: ignore default apn request for cc33");
                        z = true;
                    }
                } else if (this.mIsOp19Sim) {
                    int i = this.mRefCount;
                    this.mRefCount = i + 1;
                    if (i > 0) {
                        z = true;
                    }
                    log("requestNetwork: ignore default apn request for op19,mRefCount = " + this.mRefCount);
                }
            }
            if (!z) {
                apnContext.requestNetwork(networkRequest, localLog);
            }
        }
    }

    private boolean isOp19Sim() {
        String strMtkGetOperatorNumeric = mtkGetOperatorNumeric((IccRecords) this.mIccRecords.get());
        if (TextUtils.isEmpty(strMtkGetOperatorNumeric)) {
            strMtkGetOperatorNumeric = TelephonyManager.getDefault().getSimOperatorNumeric(this.mPhone.getSubId());
        }
        if (strMtkGetOperatorNumeric != null) {
            for (int i = 0; i < this.MCCMNC_OP19.length; i++) {
                if (strMtkGetOperatorNumeric.startsWith(this.MCCMNC_OP19[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void onDataServiceBindingChanged(boolean z) {
        super.onDataServiceBindingChanged(z);
        log("onDataServiceBindingChanged: bound = " + z);
        if (z && SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
            onRecordsLoadedOrSubIdChanged();
        }
    }

    private void onNetworkRejectReceived(AsyncResult asyncResult) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onNetworkRejectReceived exception");
            return;
        }
        int[] iArr = (int[]) asyncResult.result;
        if (iArr.length < 3) {
            loge("onNetworkRejectReceived urc format error");
            return;
        }
        int i = iArr[0];
        int i2 = iArr[1];
        int i3 = iArr[2];
        log("onNetworkRejectReceived emm_cause:" + i + ", esm_cause:" + i2 + ", event_type:" + i3);
        Intent intent = new Intent("android.intent.action.ACTION_NETWORK_REJECT_CAUSE");
        SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
        intent.addFlags(536870912);
        intent.putExtra("emmCause", i);
        intent.putExtra("esmCause", i2);
        intent.putExtra("rejectEventType", i3);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }
}
