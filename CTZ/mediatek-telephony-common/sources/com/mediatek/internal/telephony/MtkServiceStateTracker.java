package com.mediatek.internal.telephony;

import android.R;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.radio.V1_2.CellIdentityGsm;
import android.hardware.radio.V1_2.CellIdentityLte;
import android.hardware.radio.V1_2.CellIdentityTdscdma;
import android.hardware.radio.V1_2.CellIdentityWcdma;
import android.hardware.radio.V1_2.DataRegStateResult;
import android.hardware.radio.V1_2.VoiceRegStateResult;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.DataSpecificRegistrationStates;
import android.telephony.NetworkRegistrationState;
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
import android.util.StatsLog;
import android.util.TimeUtils;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.HbpcdUtils;
import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.NetworkRegistrationManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.ProxyController;
import com.android.internal.telephony.RestrictedState;
import com.android.internal.telephony.ServiceStateTracker;
import com.android.internal.telephony.TelephonyTester;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IccRefreshResponse;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.mediatek.internal.telephony.cdma.pluscode.IPlusCodeUtils;
import com.mediatek.internal.telephony.cdma.pluscode.PlusCodeProcessor;
import com.mediatek.internal.telephony.dataconnection.MtkDcTracker;
import com.mediatek.internal.telephony.ppl.PplMessageManager;
import com.mediatek.internal.telephony.uicc.MtkSpnOverride;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import mediatek.telephony.MtkServiceState;

public class MtkServiceStateTracker extends ServiceStateTracker {
    private static final boolean DBG = true;
    protected static final int EVENT_CS_NETWORK_STATE_CHANGED = 100;
    protected static final int EVENT_FEMTO_CELL_INFO = 102;
    protected static final int EVENT_ICC_REFRESH = 106;
    protected static final int EVENT_IMEI_LOCK = 107;
    protected static final int EVENT_INVALID_SIM_INFO = 101;
    protected static final int EVENT_MODULATION_INFO = 105;
    private static final int EVENT_MTK_GET_CELL_INFO_LIST = 1;
    protected static final int EVENT_NETWORK_EVENT = 104;
    protected static final int EVENT_PS_NETWORK_STATE_CHANGED = 103;
    protected static final int EVENT_SIM_OPL_LOADED = 119;
    private static final String LOG_TAG = "MTKSST";
    private static final long MTK_LAST_CELL_INFO_LIST_MAX_AGE_MS = 1000;
    protected static final String PROP_IWLAN_STATE = "persist.vendor.radio.wfc_state";
    protected static final String PROP_MTK_DATA_TYPE = "persist.vendor.radio.mtk_data_type";
    public static final int REJECT_NOTIFICATION = 890;
    private static final boolean VDBG = true;
    public boolean hasPendingPollState;
    private boolean isCsInvalidCard;
    private String mCsgId;
    private RegistrantList mDataRoamingTypeChangedRegistrants;
    private boolean mEnableERI;
    private boolean mEriTriggeredPollState;
    private boolean mEverIVSR;
    private int mFemtocellDomain;
    private boolean mForceBroadcastServiceState;
    private String mHhbName;
    private int mIsFemtocell;
    private boolean mIsImeiLock;
    private Object mLastCellInfoListLock;
    private String mLastPSRegisteredPLMN;
    private int mLastPhoneGetNitz;
    private String mLastRegisteredPLMN;
    private String mLocatedPlmn;
    private BroadcastReceiver mMtkIntentReceiver;
    private boolean mMtkVoiceCapable;
    private boolean mNetworkExsit;
    private Notification mNotification;
    private Notification.Builder mNotificationBuilder;
    private IPlusCodeUtils mPlusCodeUtils;
    private int mPsRegState;
    private int mPsRegStateRaw;
    private String mSavedGuessTimeZone;
    private IServiceStateTrackerExt mServiceStateTrackerExt;
    private OpTelephonyCustomizationFactoryBase mTelephonyCustomizationFactory;
    private String[][] mTimeZoneIdOfCapitalCity;
    private Handler mtkHandler;
    private HandlerThread mtkHandlerThread;

    private class MtkCellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        private MtkCellInfoResult() {
            this.lockObj = new Object();
        }
    }

    private class MtkHandler extends Handler {
        public MtkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                MtkCellInfoResult mtkCellInfoResult = (MtkCellInfoResult) asyncResult.userObj;
                synchronized (mtkCellInfoResult.lockObj) {
                    if (asyncResult.exception != null) {
                        MtkServiceStateTracker.this.log("EVENT_MTK_GET_CELL_INFO_LIST: error ret null, e=" + asyncResult.exception);
                        mtkCellInfoResult.list = null;
                    } else {
                        mtkCellInfoResult.list = (List) asyncResult.result;
                        MtkServiceStateTracker.this.log("EVENT_MTK_GET_CELL_INFO_LIST: size=" + mtkCellInfoResult.list.size() + " list=" + mtkCellInfoResult.list);
                    }
                    synchronized (MtkServiceStateTracker.this.mLastCellInfoListLock) {
                        MtkServiceStateTracker.this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                        MtkServiceStateTracker.this.mLastCellInfoList = mtkCellInfoResult.list;
                    }
                    mtkCellInfoResult.lockObj.notify();
                }
                return;
            }
            MtkServiceStateTracker.this.loge("Should not be here msg.what=" + message.what);
        }
    }

    public MtkServiceStateTracker(GsmCdmaPhone gsmCdmaPhone, CommandsInterface commandsInterface) {
        super(gsmCdmaPhone, commandsInterface);
        this.mEriTriggeredPollState = false;
        this.mEnableERI = false;
        this.mTelephonyCustomizationFactory = null;
        this.mServiceStateTrackerExt = null;
        this.mDataRoamingTypeChangedRegistrants = new RegistrantList();
        this.mLastCellInfoListLock = new Object();
        this.mIsImeiLock = false;
        this.mLocatedPlmn = null;
        this.mPsRegState = 1;
        this.mPsRegStateRaw = 0;
        this.mHhbName = null;
        this.mCsgId = null;
        this.mFemtocellDomain = 0;
        this.mIsFemtocell = 0;
        this.hasPendingPollState = false;
        this.mLastRegisteredPLMN = null;
        this.mLastPSRegisteredPLMN = null;
        this.mEverIVSR = false;
        this.isCsInvalidCard = false;
        this.mMtkVoiceCapable = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.popupPromptView);
        this.mLastPhoneGetNitz = -1;
        this.mTimeZoneIdOfCapitalCity = new String[][]{new String[]{"au", "Australia/Sydney"}, new String[]{"br", "America/Sao_Paulo"}, new String[]{"ca", "America/Toronto"}, new String[]{"cl", "America/Santiago"}, new String[]{"es", "Europe/Madrid"}, new String[]{"fm", "Pacific/Ponape"}, new String[]{"gl", "America/Godthab"}, new String[]{PplMessageManager.PendingMessage.KEY_ID, "Asia/Jakarta"}, new String[]{"kz", "Asia/Almaty"}, new String[]{"mn", "Asia/Ulaanbaatar"}, new String[]{"mx", "America/Mexico_City"}, new String[]{"pf", "Pacific/Tahiti"}, new String[]{"pt", "Europe/Lisbon"}, new String[]{"ru", "Europe/Moscow"}, new String[]{"us", "America/New_York"}, new String[]{"ec", "America/Guayaquil"}, new String[]{"cn", "Asia/Shanghai"}};
        this.mSavedGuessTimeZone = null;
        this.mPlusCodeUtils = PlusCodeProcessor.getPlusCodeUtils();
        this.mNetworkExsit = false;
        this.mForceBroadcastServiceState = false;
        this.mMtkIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int intExtra;
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    MtkServiceStateTracker.this.onCarrierConfigChanged();
                    return;
                }
                if (intent.getAction().equals("android.intent.action.LOCALE_CHANGED")) {
                    MtkServiceStateTracker.this.refreshSpn(MtkServiceStateTracker.this.mSS, MtkServiceStateTracker.this.mCellLoc, false);
                    MtkServiceStateTracker.this.updateSpnDisplay();
                    if (MtkServiceStateTracker.this.mForceBroadcastServiceState) {
                        MtkServiceStateTracker.this.pollState();
                        return;
                    }
                    return;
                }
                if (intent.getAction().equals("android.intent.action.ACTION_RADIO_OFF")) {
                    MtkServiceStateTracker.this.mAlarmSwitch = false;
                    MtkServiceStateTracker.this.powerOffRadioSafely(MtkServiceStateTracker.this.mPhone.mDcTracker);
                    return;
                }
                if (!intent.getAction().equals("android.telephony.action.SIM_CARD_STATE_CHANGED")) {
                    if (intent.getAction().equals("android.telephony.action.SIM_APPLICATION_STATE_CHANGED") && (intExtra = intent.getIntExtra("phone", -1)) == MtkServiceStateTracker.this.mPhone.getPhoneId()) {
                        int intExtra2 = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                        MtkServiceStateTracker.this.log("ACTION_SIM_APPLICATION_STATE_CHANGED, slotId: " + intExtra + " simState[" + intExtra2 + "]");
                        if (intExtra2 == 10) {
                            MtkServiceStateTracker.this.setDeviceRatMode(intExtra);
                            return;
                        }
                        return;
                    }
                    return;
                }
                int intExtra3 = intent.getIntExtra("phone", -1);
                if (intExtra3 == MtkServiceStateTracker.this.mPhone.getPhoneId()) {
                    int intExtra4 = intent.getIntExtra("android.telephony.extra.SIM_STATE", 0);
                    MtkServiceStateTracker.this.log("SIM state change, slotId: " + intExtra3 + " simState[" + intExtra4 + "]");
                    if (!MtkServiceStateTracker.this.mPhone.isPhoneTypeGsm()) {
                        if (1 == intExtra4) {
                            MtkServiceStateTracker.this.mMdn = null;
                        }
                    } else if (intExtra4 == 1) {
                        MtkServiceStateTracker.this.mLastRegisteredPLMN = null;
                        MtkServiceStateTracker.this.mLastPSRegisteredPLMN = null;
                    }
                }
            }
        };
        this.mtkHandlerThread = new HandlerThread("MtkHandlerThread");
        this.mtkHandlerThread.start();
        this.mtkHandler = new MtkHandler(this.mtkHandlerThread.getLooper());
        Context context = this.mPhone.getContext();
        context.unregisterReceiver(this.mIntentReceiver);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        context.registerReceiver(this.mMtkIntentReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("android.intent.action.ACTION_RADIO_OFF");
        context.registerReceiver(this.mMtkIntentReceiver, intentFilter2);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        context.registerReceiver(this.mMtkIntentReceiver, intentFilter3);
        IntentFilter intentFilter4 = new IntentFilter();
        intentFilter4.addAction("android.telephony.action.SIM_APPLICATION_STATE_CHANGED");
        context.registerReceiver(this.mMtkIntentReceiver, intentFilter4);
        IntentFilter intentFilter5 = new IntentFilter();
        intentFilter5.addAction("android.telephony.action.SIM_CARD_STATE_CHANGED");
        context.registerReceiver(this.mMtkIntentReceiver, intentFilter5);
        try {
            this.mTelephonyCustomizationFactory = OpTelephonyCustomizationUtils.getOpFactory(this.mPhone.getContext());
            this.mServiceStateTrackerExt = this.mTelephonyCustomizationFactory.makeServiceStateTrackerExt(this.mPhone.getContext());
        } catch (Exception e) {
            log("mServiceStateTrackerExt init fail");
            e.printStackTrace();
        }
        this.mCi.registerForCsNetworkStateChanged(this, 100, null);
    }

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
        this.mSS = new MtkServiceState();
        this.mNewSS = new MtkServiceState();
        this.mLastCellInfoListTime = 0L;
        this.mLastCellInfoList = null;
        this.mSignalStrength = new SignalStrength();
        this.mRestrictedState = new RestrictedState();
        this.mStartedGprsRegCheck = false;
        this.mReportedGprsNoReg = false;
        this.mMdn = null;
        this.mMin = null;
        this.mPrlVersion = null;
        this.mIsMinInfoReady = false;
        this.mNitzState.handleNetworkUnavailable();
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
            this.mCi.registerForPsNetworkStateChanged(this, EVENT_PS_NETWORK_STATE_CHANGED, null);
            this.mCi.setInvalidSimInfo(this, 101, null);
            this.mCi.registerForIccRefresh(this, EVENT_ICC_REFRESH, null);
            this.mCi.registerForNetworkEvent(this, 104, null);
            this.mCi.registerForModulation(this, 105, null);
            if (SystemProperties.get("ro.vendor.mtk_femto_cell_support").equals("1")) {
                this.mCi.registerForFemtoCellInfo(this, 102, null);
            }
            try {
                if (this.mServiceStateTrackerExt != null && this.mServiceStateTrackerExt.isImeiLocked()) {
                    this.mCi.registerForIMEILock(this, EVENT_IMEI_LOCK, null);
                }
            } catch (RuntimeException e) {
                loge("No isImeiLocked");
            }
        } else {
            this.mCi.unregisterForAvailable(this);
            this.mCi.unSetOnRestrictedStateChanged(this);
            this.mPsRestrictDisabledRegistrants.notifyRegistrants();
            this.mCi.unregisterForCdmaPrlChanged(this);
            this.mPhone.unregisterForEriFileLoaded(this);
            this.mCi.unregisterForCdmaOtaProvision(this);
            this.mPhone.unregisterForSimRecordsLoaded(this);
            this.mCi.unregisterForIccRefresh(this);
            this.mCi.unregisterForPsNetworkStateChanged(this);
            this.mCi.unSetInvalidSimInfo(this);
            this.mCi.unregisterForNetworkEvent(this);
            this.mCi.unregisterForModulation(this);
            try {
                if (this.mServiceStateTrackerExt != null && this.mServiceStateTrackerExt.isImeiLocked()) {
                    this.mCi.unregisterForIMEILock(this);
                }
            } catch (RuntimeException e2) {
                loge("No isImeiLocked");
            }
            if (this.mPhone.isPhoneTypeCdmaLte()) {
                this.mPhone.registerForSimRecordsLoaded(this, 16, (Object) null);
            }
            this.mCellLoc = new CdmaCellLocation();
            this.mNewCellLoc = new CdmaCellLocation();
            this.mCdmaSSM = CdmaSubscriptionSourceManager.getInstance(this.mPhone.getContext(), this.mCi, this, 39, (Object) null);
            this.mIsSubscriptionFromRuim = this.mCdmaSSM.getCdmaSubscriptionSource() == 0;
            this.mCi.registerForCdmaPrlChanged(this, 40, (Object) null);
            this.mPhone.registerForEriFileLoaded(this, 36, (Object) null);
            this.mCi.registerForCdmaOtaProvision(this, 37, (Object) null);
            this.mHbpcdUtils = new HbpcdUtils(this.mPhone.getContext());
            updateOtaspState();
        }
        onUpdateIccAvailability();
        this.mPhone.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(0));
        this.mCi.getSignalStrength(obtainMessage(3));
        sendMessage(obtainMessage(50));
        logPhoneTypeChange();
        notifyDataRegStateRilRadioTechnologyChanged();
    }

    public void registerForDataRoamingTypeChange(Handler handler, int i, Object obj) {
        this.mDataRoamingTypeChangedRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForDataRoamingTypeChange(Handler handler) {
        this.mDataRoamingTypeChangedRegistrants.remove(handler);
    }

    public void dispose() {
        super.dispose();
        this.mCi.unregisterForCsNetworkStateChanged(this);
        this.mtkHandlerThread.quit();
        if (this.mPhone.isPhoneTypeGsm()) {
            this.mCi.unregisterForIccRefresh(this);
            this.mCi.unregisterForPsNetworkStateChanged(this);
            this.mCi.unSetInvalidSimInfo(this);
            this.mCi.unregisterForNetworkEvent(this);
            this.mCi.unregisterForModulation(this);
            try {
                if (this.mServiceStateTrackerExt.isImeiLocked()) {
                    this.mCi.unregisterForIMEILock(this);
                }
            } catch (RuntimeException e) {
                loge("No isImeiLocked");
            }
        }
    }

    public void handleMessage(Message message) {
        int[] subId;
        logv("received event " + message.what);
        int i = message.what;
        if (i != 1) {
            if (i == 11) {
                this.mLastPhoneGetNitz = this.mPhone.getPhoneId();
                AsyncResult asyncResult = (AsyncResult) message.obj;
                final String str = (String) ((Object[]) asyncResult.result)[0];
                final long jLongValue = ((Long) ((Object[]) asyncResult.result)[1]).longValue();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        MtkServiceStateTracker.this.setTimeFromNITZString(str, jLongValue);
                    }
                }).start();
                return;
            }
            if (i == 16) {
                if (this.mPhone.isPhoneTypeGsm()) {
                    refreshSpn(this.mSS, this.mCellLoc, false);
                }
                super.handleMessage(message);
                if (this.mForceBroadcastServiceState) {
                    pollState();
                    return;
                }
                return;
            }
            if (i == 26) {
                if (this.mPhone.isPhoneTypeCdmaLte()) {
                    log("Receive EVENT_RUIM_READY");
                    pollState();
                } else {
                    log("Receive EVENT_RUIM_READY and Send Request getCDMASubscription.");
                    getSubscriptionInfoAndStartPollingThreads();
                }
                this.mCi.getNetworkSelectionMode(obtainMessage(14));
                return;
            }
            if (i == 36) {
                this.mEriTriggeredPollState = true;
                super.handleMessage(message);
                return;
            }
            if (i == 44) {
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + asyncResult2.exception);
                    return;
                }
                List list = (List) asyncResult2.result;
                log("EVENT_UNSOL_CELL_INFO_LIST: size=" + list.size() + " list=" + list);
                synchronized (this.mLastCellInfoListLock) {
                    this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    this.mLastCellInfoList = list;
                }
                this.mPhone.notifyCellInfo(list);
                return;
            }
            if (i != 119) {
                switch (i) {
                    case 49:
                        if (SubscriptionManager.getDefaultDataSubscriptionId() == -1 && (subId = SubscriptionManager.getSubId(RadioCapabilitySwitchUtil.getMainCapabilityPhoneId())) != null && subId.length > 0) {
                            ProxyController.getInstance().unregisterForAllDataDisconnected(subId[0], this);
                        }
                        this.mPhone.unregisterForAllDataDisconnected(this);
                        super.handleMessage(message);
                        return;
                    case 50:
                        break;
                    default:
                        switch (i) {
                            case 52:
                                log("EVENT_SIM_NOT_INSERTED, ignored.");
                                return;
                            case 53:
                                log("EVENT_IMS_SERVICE_STATE_CHANGED");
                                if (this.mSS.getState() != 0 && this.mSS.getDataRegState() == 0) {
                                    this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
                                    return;
                                }
                                return;
                            default:
                                switch (i) {
                                    case 100:
                                        onNetworkStateChangeResult((AsyncResult) message.obj);
                                        return;
                                    case 101:
                                        if (this.mPhone.isPhoneTypeGsm()) {
                                            onInvalidSimInfoReceived((AsyncResult) message.obj);
                                            return;
                                        }
                                        return;
                                    case 102:
                                        onFemtoCellInfoResult((AsyncResult) message.obj);
                                        return;
                                    case EVENT_PS_NETWORK_STATE_CHANGED:
                                        onPsNetworkStateChangeResult((AsyncResult) message.obj);
                                        return;
                                    case 104:
                                        if (this.mPhone.isPhoneTypeGsm()) {
                                            onNetworkEventReceived((AsyncResult) message.obj);
                                            return;
                                        }
                                        return;
                                    case 105:
                                        if (this.mPhone.isPhoneTypeGsm()) {
                                            onModulationInfoReceived((AsyncResult) message.obj);
                                            return;
                                        }
                                        return;
                                    case EVENT_ICC_REFRESH:
                                        if (this.mPhone.isPhoneTypeGsm()) {
                                            AsyncResult asyncResult3 = (AsyncResult) message.obj;
                                            if (asyncResult3.exception == null) {
                                                IccRefreshResponse iccRefreshResponse = (IccRefreshResponse) asyncResult3.result;
                                                if (iccRefreshResponse == null) {
                                                    log("IccRefreshResponse is null");
                                                    return;
                                                }
                                                int i2 = iccRefreshResponse.refreshResult;
                                                if (i2 != 0) {
                                                    switch (i2) {
                                                        case 4:
                                                        case 6:
                                                            this.mLastRegisteredPLMN = null;
                                                            this.mLastPSRegisteredPLMN = null;
                                                            log("Reset mLastRegisteredPLMN/mLastPSRegisteredPLMNfor ICC refresh");
                                                            return;
                                                        case 5:
                                                            break;
                                                        default:
                                                            log("GSST EVENT_ICC_REFRESH IccRefreshResponse =" + iccRefreshResponse);
                                                            return;
                                                    }
                                                }
                                                if (iccRefreshResponse.efId == 28423) {
                                                    this.mLastRegisteredPLMN = null;
                                                    this.mLastPSRegisteredPLMN = null;
                                                    log("Reset flag of IVSR for IMSI update");
                                                    return;
                                                }
                                                return;
                                            }
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
                                    default:
                                        super.handleMessage(message);
                                        return;
                                }
                        }
                }
            } else {
                AsyncResult asyncResult4 = (AsyncResult) message.obj;
                if (asyncResult4 != null && asyncResult4.result != null) {
                    if (((Integer) asyncResult4.result).intValue() == 101) {
                        if (this.mPhone.isPhoneTypeGsm()) {
                            log("EVENT_SIM_OPL_LOADED: EVENT_OPL");
                            refreshSpn(this.mSS, this.mCellLoc, false);
                            if (this.mForceBroadcastServiceState) {
                                pollState();
                                return;
                            }
                            return;
                        }
                        loge("EVENT_SIM_OPL_LOADED should not be here");
                        return;
                    }
                    return;
                }
                loge("EVENT_SIM_OPL_LOADED obj is null");
                return;
            }
        }
        log("handle EVENT_RADIO_STATE_CHANGED");
        if (!this.mPhone.isPhoneTypeGsm() && this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_ON) {
            handleCdmaSubscriptionSource(this.mCdmaSSM.getCdmaSubscriptionSource());
            queueNextSignalStrengthPoll();
        }
        RadioManager.getInstance().setRadioPower(this.mDesiredPowerState, this.mPhone.getPhoneId());
        modemTriggeredPollState();
    }

    protected void handlePollStateResultMessage(int i, AsyncResult asyncResult) {
        NetworkRegistrationState networkRegistrationStateCreateRegistrationStateFromVoiceRegState;
        int networkId;
        NetworkRegistrationState networkRegistrationStateCreateRegistrationStateFromDataRegState;
        int iIntValue;
        Object[] objArr;
        String operatorBrandOverride;
        int systemId = 0;
        if (i != 14) {
            switch (i) {
                case 4:
                    if (asyncResult.result instanceof VoiceRegStateResult) {
                        networkRegistrationStateCreateRegistrationStateFromVoiceRegState = createRegistrationStateFromVoiceRegState(asyncResult.result);
                    } else {
                        networkRegistrationStateCreateRegistrationStateFromVoiceRegState = (NetworkRegistrationState) asyncResult.result;
                    }
                    VoiceSpecificRegistrationStates voiceSpecificStates = networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getVoiceSpecificStates();
                    int regState = networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getRegState();
                    boolean z = voiceSpecificStates.cssSupported;
                    int iNetworkTypeToRilRadioTechnology = ServiceState.networkTypeToRilRadioTechnology(networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getAccessNetworkTechnology());
                    this.mNewSS.setVoiceRegState(regCodeToServiceState(regState));
                    this.mNewSS.setCssIndicator(z ? 1 : 0);
                    this.mNewSS.setRilVoiceRadioTechnology(iNetworkTypeToRilRadioTechnology);
                    this.mNewSS.addNetworkRegistrationState(networkRegistrationStateCreateRegistrationStateFromVoiceRegState);
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getCellIdentity());
                    this.mNewSS.setRilVoiceRegState(regState);
                    int reasonForDenial = networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getReasonForDenial();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mGsmRoaming = regCodeIsRoaming(regState);
                        this.mPhone.getContext().getResources().getBoolean(R.^attr-private.popupPromptView);
                        this.mEmergencyOnly = networkRegistrationStateCreateRegistrationStateFromVoiceRegState.isEmergencyEnabled();
                    } else {
                        int i2 = voiceSpecificStates.roamingIndicator;
                        int i3 = voiceSpecificStates.systemIsInPrl;
                        int i4 = voiceSpecificStates.defaultRoamingIndicator;
                        this.mRegistrationState = regState;
                        boolean z2 = regCodeIsRoaming(regState) && !isRoamIndForHomeSystem(Integer.toString(i2));
                        this.mNewSS.setVoiceRoaming(z2);
                        if (z2) {
                            this.mNewSS.setRilVoiceRegState(5);
                        }
                        this.mRoamingIndicator = i2;
                        this.mIsInPrl = i3 != 0;
                        this.mDefaultRoamingIndicator = i4;
                        CellIdentity cellIdentity = networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getCellIdentity();
                        if (cellIdentity != null && cellIdentity.getType() == 2) {
                            CellIdentityCdma cellIdentityCdma = (CellIdentityCdma) cellIdentity;
                            systemId = cellIdentityCdma.getSystemId();
                            networkId = cellIdentityCdma.getNetworkId();
                        } else {
                            networkId = 0;
                        }
                        this.mNewSS.setCdmaSystemAndNetworkId(systemId, networkId);
                        if (reasonForDenial == 0) {
                            this.mRegistrationDeniedReason = "General";
                        } else if (reasonForDenial == 1) {
                            this.mRegistrationDeniedReason = "Authentication Failure";
                        } else {
                            this.mRegistrationDeniedReason = "";
                        }
                        if (this.mRegistrationState == 3) {
                            log("Registration denied, " + this.mRegistrationDeniedReason);
                        }
                    }
                    processCellLocationInfo(this.mNewCellLoc, networkRegistrationStateCreateRegistrationStateFromVoiceRegState.getCellIdentity());
                    break;
                case 5:
                    if (asyncResult.result instanceof DataRegStateResult) {
                        networkRegistrationStateCreateRegistrationStateFromDataRegState = createRegistrationStateFromDataRegState(asyncResult.result);
                    } else {
                        networkRegistrationStateCreateRegistrationStateFromDataRegState = (NetworkRegistrationState) asyncResult.result;
                    }
                    DataSpecificRegistrationStates dataSpecificStates = networkRegistrationStateCreateRegistrationStateFromDataRegState.getDataSpecificStates();
                    int regState2 = networkRegistrationStateCreateRegistrationStateFromDataRegState.getRegState();
                    int iRegCodeToServiceState = regCodeToServiceState(regState2);
                    int iNetworkTypeToRilRadioTechnology2 = ServiceState.networkTypeToRilRadioTechnology(networkRegistrationStateCreateRegistrationStateFromDataRegState.getAccessNetworkTechnology());
                    this.mNewSS.setDataRegState(iRegCodeToServiceState);
                    this.mNewSS.setRilDataRadioTechnology(iNetworkTypeToRilRadioTechnology2);
                    this.mNewSS.addNetworkRegistrationState(networkRegistrationStateCreateRegistrationStateFromDataRegState);
                    if (iRegCodeToServiceState == 1) {
                        this.mLastPhysicalChannelConfigList = null;
                    }
                    setPhyCellInfoFromCellIdentity(this.mNewSS, networkRegistrationStateCreateRegistrationStateFromDataRegState.getCellIdentity());
                    this.mNewSS.setRilDataRegState(regState2);
                    try {
                        iIntValue = Integer.valueOf(TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), PROP_MTK_DATA_TYPE, "0")).intValue();
                    } catch (Exception e) {
                        loge("INVALID PROP_MTK_DATA_TYPE");
                        iIntValue = 0;
                    }
                    this.mNewSS.setProprietaryDataRadioTechnology(iIntValue);
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mNewReasonDataDenied = networkRegistrationStateCreateRegistrationStateFromDataRegState.getReasonForDenial();
                        this.mNewMaxDataCalls = dataSpecificStates.maxDataCalls;
                        this.mDataRoaming = regCodeIsRoaming(regState2);
                        this.mNewSS.setDataRoamingFromRegistration(this.mDataRoaming);
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
                        if (regCodeIsRoaming(regState2)) {
                            this.mNewSS.setRilDataRegState(5);
                        }
                        boolean zRegCodeIsRoaming2 = regCodeIsRoaming(regState2);
                        this.mNewSS.setDataRoaming(zRegCodeIsRoaming2);
                        this.mNewSS.setDataRoamingFromRegistration(zRegCodeIsRoaming2);
                        log("handlPollStateResultMessage: CdmaLteSST dataServiceState=" + iRegCodeToServiceState + " registrationState=" + regState2 + " dataRadioTechnology=" + iNetworkTypeToRilRadioTechnology2);
                    }
                    updateServiceStateLteEarfcnBoost(this.mNewSS, getLteEarfcn(networkRegistrationStateCreateRegistrationStateFromDataRegState.getCellIdentity()));
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
                            if (strArr2[2] == null || strArr2[2].length() < 5 || "00000".equals(strArr2[2]) || "N/AN/A".equals(strArr2[2])) {
                                strArr2[2] = SystemProperties.get("ro.cdma.home.operator.numeric", "");
                                log("RIL_REQUEST_OPERATOR.response[2], the numeric,  is bad. Using SystemProperties 'ro.cdma.home.operator.numeric'= " + strArr2[2]);
                            }
                            String str = strArr2[2];
                            if (str != null && str.startsWith("2134") && str.length() == 7) {
                                String strCheckMccBySidLtmOff = this.mPlusCodeUtils.checkMccBySidLtmOff(str);
                                if (!strCheckMccBySidLtmOff.equals("0")) {
                                    strArr2[2] = strCheckMccBySidLtmOff + str.substring(4);
                                    log("EVENT_POLL_STATE_OPERATOR_CDMA: checkMccBySidLtmOff: numeric =" + strCheckMccBySidLtmOff + ", plmn =" + strArr2[2]);
                                }
                                objArr = true;
                            } else {
                                objArr = false;
                            }
                            if (!this.mIsSubscriptionFromRuim) {
                                if (objArr != false) {
                                    strArr2[1] = lookupOperatorName(this.mPhone.getContext(), this.mPhone.getSubId(), strArr2[2], false);
                                }
                                this.mNewSS.setOperatorName(null, strArr2[1], strArr2[2]);
                            } else {
                                operatorBrandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
                                if (operatorBrandOverride != null) {
                                    log("EVENT_POLL_STATE_OPERATOR_CDMA: use brand=" + operatorBrandOverride);
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
            this.mPhone.setNetworkSelectionModeAutomatic((Message) null);
            log(" Forcing Automatic Network Selection, manual selection is not allowed");
        }
    }

    protected void updateRoamingState() {
        if (this.mPhone.isPhoneTypeGsm()) {
            boolean z = this.mGsmRoaming || this.mDataRoaming;
            if (this.mGsmRoaming && !isOperatorConsideredRoaming(this.mNewSS) && (isSameNamedOperators(this.mNewSS) || isOperatorConsideredNonRoaming(this.mNewSS))) {
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
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("telephony.test.forceRoaming", false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
    }

    protected void handlePollStateResult(int i, AsyncResult asyncResult) {
        int iIntValue;
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
                commandError = asyncResult.exception.getCommandError();
            }
            if (commandError == CommandException.Error.RADIO_NOT_AVAILABLE) {
                cancelPollState();
                if (this.hasPendingPollState) {
                    this.hasPendingPollState = false;
                    pollState();
                    loge("handlePollStateResult trigger pending pollState()");
                    return;
                } else {
                    if (this.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_ON) {
                        if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_UNAVAILABLE) {
                            this.mNewSS.setStateOutOfService();
                        } else {
                            this.mNewSS.setStateOff();
                        }
                        this.mNewCellLoc.setStateInvalid();
                        setSignalStrengthDefaultValues();
                        this.mPsRegStateRaw = 0;
                        pollStateDone();
                        loge("Mlog: pollStateDone to notify RADIO_NOT_AVAILABLE");
                        return;
                    }
                    return;
                }
            }
            if (commandError != CommandException.Error.OP_NOT_ALLOWED_BEFORE_REG_NW) {
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
            try {
                iIntValue = Integer.valueOf(TelephonyManager.getTelephonyProperty(this.mPhone.getPhoneId(), PROP_IWLAN_STATE, "0")).intValue();
            } catch (Exception e2) {
                loge("INVALID PROP_IWLAN_STATE");
                iIntValue = 0;
            }
            this.mNewSS.setIwlanRegState(regCodeToServiceState(iIntValue));
            if (this.mPhone.isPhoneTypeGsm()) {
                boolean z = this.mNewSS.getVoiceRegState() == 0 || this.mNewSS.getDataRegState() == 0;
                boolean z2 = this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF && (this.mNewSS.getRilDataRadioTechnology() == 18 || this.mNewSS.getIwlanRegState() == 0);
                String operatorNumeric = this.mNewSS.getOperatorNumeric();
                if (((!z && !TextUtils.isEmpty(operatorNumeric)) || (z && TextUtils.isEmpty(operatorNumeric))) && !z2 && this.hasPendingPollState) {
                    loge("Temporary service state, need restart PollState");
                    this.hasPendingPollState = false;
                    cancelPollState();
                    modemTriggeredPollState();
                    return;
                }
                updateRoamingState();
                boolean imsEccOnly = getImsEccOnly();
                if (!z && imsEccOnly) {
                    this.mEmergencyOnly = true;
                }
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
            } else {
                boolean z3 = !isSidsAllZeros() && isHomeSid(this.mNewSS.getCdmaSystemId());
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
                        boolean dataRoaming = this.mNewSS.getDataRoaming();
                        if (this.mNewSS.getDataRoaming() == zIsRoamIndForHomeSystem) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("isRoamIndForHomeSystem=");
                            sb.append(zIsRoamIndForHomeSystem);
                            sb.append(", override data roaming to ");
                            sb.append(!zIsRoamIndForHomeSystem);
                            log(sb.toString());
                            this.mNewSS.setDataRoaming(!zIsRoamIndForHomeSystem);
                        }
                        String[] stringArray = Resources.getSystem().getStringArray(R.array.config_autoKeyboardBacklightBrightnessValues);
                        if (!dataRoaming && !zIsRoamIndForHomeSystem && stringArray != null && stringArray.length == 0) {
                            log("isRoamIndForHomeSystem=" + zIsRoamIndForHomeSystem + ", override data roaming to false");
                            this.mNewSS.setDataRoaming(false);
                        }
                    }
                }
                this.mEmergencyOnly = false;
                if (this.mCi.getRadioState().isOn()) {
                    if (this.mNewSS.getVoiceRegState() == 1 && this.mNewSS.getDataRegState() == 1 && this.mNetworkExsit) {
                        this.mEmergencyOnly = true;
                    }
                    this.mEmergencyOnly = mergeEmergencyOnlyCdmaIms(this.mEmergencyOnly);
                }
                this.mNewSS.setEmergencyOnly(this.mEmergencyOnly);
                this.mNewSS.setCdmaDefaultRoamingIndicator(this.mDefaultRoamingIndicator);
                this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                boolean z4 = TextUtils.isEmpty(this.mPrlVersion) ? false : true;
                if (!z4 || this.mNewSS.getRilVoiceRadioTechnology() == 0) {
                    logv("Turn off roaming indicator if !isPrlLoaded or voice RAT is unknown");
                    this.mNewSS.setCdmaRoamingIndicator(1);
                } else if (!isSidsAllZeros()) {
                    if (!z3 && !this.mIsInPrl) {
                        this.mNewSS.setCdmaRoamingIndicator(this.mDefaultRoamingIndicator);
                    } else if (z3 && !this.mIsInPrl) {
                        if (!ServiceState.isLte(this.mNewSS.getRilVoiceRadioTechnology())) {
                            this.mNewSS.setCdmaRoamingIndicator(2);
                        } else {
                            log("Turn off roaming indicator as voice is LTE");
                            this.mNewSS.setCdmaRoamingIndicator(1);
                        }
                    } else if ((z3 || !this.mIsInPrl) && this.mRoamingIndicator <= 2) {
                        this.mNewSS.setCdmaRoamingIndicator(1);
                    } else {
                        this.mNewSS.setCdmaRoamingIndicator(this.mRoamingIndicator);
                    }
                }
                int cdmaRoamingIndicator = this.mNewSS.getCdmaRoamingIndicator();
                this.mNewSS.setCdmaEriIconIndex(this.mPhone.mEriManager.getCdmaEriIconIndex(cdmaRoamingIndicator, this.mDefaultRoamingIndicator));
                this.mNewSS.setCdmaEriIconMode(this.mPhone.mEriManager.getCdmaEriIconMode(cdmaRoamingIndicator, this.mDefaultRoamingIndicator));
                log("Set CDMA Roaming Indicator to: " + this.mNewSS.getCdmaRoamingIndicator() + ". voiceRoaming = " + this.mNewSS.getVoiceRoaming() + ". dataRoaming = " + this.mNewSS.getDataRoaming() + ", isPrlLoaded = " + z4 + ". namMatch = " + z3 + " , mIsInPrl = " + this.mIsInPrl + ", mRoamingIndicator = " + this.mRoamingIndicator + ", mDefaultRoamingIndicator= " + this.mDefaultRoamingIndicator + ", set mEmergencyOnly=" + this.mEmergencyOnly + ", mNetworkExsit=" + this.mNetworkExsit);
            }
            pollStateDone();
        }
    }

    protected void updateSpnDisplay() {
        String str;
        String str2;
        boolean z;
        boolean z2;
        String string;
        boolean z3;
        boolean z4;
        boolean z5;
        String str3;
        String string2;
        String serviceProviderName;
        boolean z6;
        String str4;
        String str5;
        String str6;
        int i;
        String str7;
        int i2;
        int i3;
        updateOperatorNameFromEri();
        int combinedRegState = getCombinedRegState();
        if (this.mPhone.getImsPhone() != null && this.mPhone.getImsPhone().isWifiCallingEnabled() && combinedRegState == 0) {
            String[] stringArray = this.mPhone.getContext().getResources().getStringArray(R.array.config_foldedDeviceStates);
            CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (carrierConfigManager != null) {
                try {
                    PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
                    if (configForSubId != null) {
                        i2 = configForSubId.getInt("wfc_spn_format_idx_int");
                        try {
                            i3 = configForSubId.getInt("wfc_data_spn_format_idx_int");
                        } catch (Exception e) {
                            e = e;
                            loge("updateSpnDisplay: carrier config error: " + e);
                            i3 = 0;
                        }
                    } else {
                        i3 = 0;
                        i2 = 0;
                    }
                } catch (Exception e2) {
                    e = e2;
                    i2 = 0;
                }
            } else {
                i3 = 0;
                i2 = 0;
            }
            str2 = stringArray[i2];
            str = stringArray[i3];
        } else {
            str = null;
            str2 = null;
        }
        if (!this.mPhone.isPhoneTypeGsm()) {
            String operatorAlpha = this.mSS.getOperatorAlpha();
            boolean z7 = operatorAlpha != null;
            if (operatorAlpha != null && operatorAlpha.equals("")) {
                operatorAlpha = null;
            }
            int[] subId = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
            int i4 = (subId == null || subId.length <= 0) ? -1 : subId[0];
            if (!TextUtils.isEmpty(operatorAlpha) && !TextUtils.isEmpty(str2)) {
                operatorAlpha = String.format(str2, operatorAlpha.trim());
            } else if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
                log("updateSpnDisplay: overwriting plmn from " + operatorAlpha + " to null as radio state is off");
                operatorAlpha = null;
            }
            if (combinedRegState == 1 || combinedRegState == 3) {
                operatorAlpha = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
                log("updateSpnDisplay: radio is on but out of svc, set plmn='" + operatorAlpha + "'");
                z7 = true;
            }
            if (this.mEmergencyOnly && this.mCi.getRadioState().isOn()) {
                log("[CDMA]updateSpnDisplay:phone show emergency call only, mEmergencyOnly = true");
                operatorAlpha = Resources.getSystem().getText(R.string.capability_desc_canRequestTouchExploration).toString();
                z = true;
            } else {
                z = z7;
            }
            String serviceProviderName2 = "";
            try {
                if (this.mServiceStateTrackerExt.allowSpnDisplayed()) {
                    IccRecords iccRecords = this.mPhone.getIccRecords();
                    int displayRule = iccRecords != null ? iccRecords.getDisplayRule(this.mSS) : 2;
                    serviceProviderName2 = iccRecords != null ? iccRecords.getServiceProviderName() : "";
                    if (TextUtils.isEmpty(serviceProviderName2) || (displayRule & 1) != 1 || this.mSS.getVoiceRegState() == 3) {
                        z2 = false;
                        try {
                            log("[CDMA]updateSpnDisplay: rule=" + displayRule + ", spn=" + serviceProviderName2 + ", showSpn=" + z2);
                        } catch (RuntimeException e3) {
                            e = e3;
                            e.printStackTrace();
                        }
                    } else {
                        if (!this.mSS.getRoaming()) {
                            z2 = true;
                        }
                        log("[CDMA]updateSpnDisplay: rule=" + displayRule + ", spn=" + serviceProviderName2 + ", showSpn=" + z2);
                    }
                } else {
                    z2 = false;
                }
            } catch (RuntimeException e4) {
                e = e4;
                z2 = false;
            }
            if (this.mSubId != i4 || z != this.mCurShowPlmn || z2 != this.mCurShowSpn || !TextUtils.equals(serviceProviderName2, this.mCurSpn) || !TextUtils.equals(operatorAlpha, this.mCurPlmn)) {
                z = operatorAlpha != null;
                try {
                    if (this.mServiceStateTrackerExt.allowSpnDisplayed()) {
                        if (this.mSS.getVoiceRegState() == 3 || this.mSS.getVoiceRegState() == 1 || this.mSS.getRoaming() || serviceProviderName2 == null) {
                            z2 = false;
                            z = true;
                        } else if (!serviceProviderName2.equals("")) {
                            z = false;
                            z2 = true;
                        }
                    }
                } catch (RuntimeException e5) {
                    e5.printStackTrace();
                }
                logv(String.format("[CDMA]updateSpnDisplay: changed sending intent subId='%d' showPlmn='%b' plmn='%s' showSpn='%b' spn='%s'", Integer.valueOf(i4), Boolean.valueOf(z), operatorAlpha, Boolean.valueOf(z2), serviceProviderName2));
                Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                    intent.addFlags(536870912);
                }
                intent.putExtra("showSpn", z2);
                intent.putExtra("spn", serviceProviderName2);
                intent.putExtra("showPlmn", z);
                intent.putExtra("plmn", operatorAlpha);
                intent.putExtra("hnbName", this.mHhbName);
                intent.putExtra("csgId", this.mCsgId);
                intent.putExtra("domain", this.mFemtocellDomain);
                intent.putExtra("femtocell", this.mIsFemtocell);
                SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
                this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
                if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z, operatorAlpha, z2, serviceProviderName2)) {
                    this.mSpnUpdatePending = true;
                }
                log("[CDMA]updateSpnDisplay: subId=" + i4 + ", showPlmn=" + z + ", plmn=" + operatorAlpha + ", showSpn=" + z2 + ", spn=" + serviceProviderName2 + ", mSpnUpdatePending=" + this.mSpnUpdatePending);
            }
            this.mSubId = i4;
            this.mCurShowSpn = z2;
            this.mCurShowPlmn = z;
            this.mCurSpn = serviceProviderName2;
            this.mCurPlmn = operatorAlpha;
            return;
        }
        IccRecords iccRecords2 = this.mIccRecords;
        int displayRule2 = iccRecords2 != null ? iccRecords2.getDisplayRule(this.mSS) : 0;
        if (combinedRegState == 1 || combinedRegState == 2) {
            boolean z8 = this.mPhone.getContext().getResources().getBoolean(R.^attr-private.emulated) && !this.mIsSimReady;
            if (!this.mEmergencyOnly || z8) {
                string = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
                z3 = true;
            } else {
                string = Resources.getSystem().getText(R.string.capability_desc_canRequestTouchExploration).toString();
                z3 = false;
            }
            log("updateSpnDisplay: radio is on but out of service, set plmn='" + string + "'");
            z4 = z3;
            z5 = true;
        } else {
            if (combinedRegState != 0) {
                String string3 = Resources.getSystem().getText(R.string.contentServiceTooManyDeletesNotificationDesc).toString();
                log("updateSpnDisplay: radio is off w/ showPlmn=true plmn=" + string3);
                str3 = string3;
                z4 = false;
                z5 = true;
                string2 = this.mServiceStateTrackerExt.onUpdateSpnDisplay(str3, (MtkServiceState) this.mSS, this.mPhone.getPhoneId());
                if (this.mSS.getVoiceRegState() != 0 && this.mSS.getDataRegState() == 0) {
                    if (string2 != null) {
                        log("PLMN name is null when CS not registered and PS registered");
                    } else if (getImsServiceState() != 0) {
                        string2 = string2 + "(" + Resources.getSystem().getText(134545555).toString() + ")";
                    }
                }
                if (this.mIsImeiLock) {
                    string2 = Resources.getSystem().getText(134545510).toString();
                }
                serviceProviderName = iccRecords2 == null ? iccRecords2.getServiceProviderName() : "";
                z6 = z4 && !TextUtils.isEmpty(serviceProviderName) && (displayRule2 & 1) == 1;
                if (!TextUtils.isEmpty(serviceProviderName) || TextUtils.isEmpty(str2) || TextUtils.isEmpty(str)) {
                    if (TextUtils.isEmpty(string2) && !TextUtils.isEmpty(str2)) {
                        string2 = String.format(str2, string2.trim());
                    } else if (this.mSS.getVoiceRegState() != 3 || (z5 && TextUtils.equals(serviceProviderName, string2))) {
                        str4 = serviceProviderName;
                        z6 = false;
                        str5 = null;
                    }
                    str4 = serviceProviderName;
                    str5 = str4;
                } else {
                    String strTrim = serviceProviderName.trim();
                    str5 = String.format(str2, strTrim);
                    str4 = String.format(str, strTrim);
                    z5 = false;
                    z6 = true;
                }
                str6 = string2;
                if (this.mSS.getVoiceRegState() != 0 && this.mSS.getDataRegState() != 0) {
                    z6 = false;
                    str5 = null;
                }
                if (this.mServiceStateTrackerExt.needSpnRuleShowPlmnOnly() && !TextUtils.isEmpty(str6)) {
                    log("origin showSpn:" + z6 + " showPlmn:" + z5 + " rule:" + displayRule2);
                    z6 = false;
                    displayRule2 = 2;
                    z5 = true;
                }
                int[] subId2 = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
                i = (subId2 != null || subId2.length <= 0) ? -1 : subId2[0];
                if (this.mServiceStateTrackerExt.allowSpnDisplayed() && displayRule2 == 3) {
                    z6 = false;
                    str7 = null;
                } else {
                    str7 = str5;
                }
                str5 = str7;
                if (this.mSubId == i || z5 != this.mCurShowPlmn || z6 != this.mCurShowSpn || !TextUtils.equals(str5, this.mCurSpn) || !TextUtils.equals(str4, this.mCurDataSpn) || !TextUtils.equals(str6, this.mCurPlmn)) {
                    log(String.format("updateSpnDisplay: changed sending intent rule=" + displayRule2 + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'", Boolean.valueOf(z5), str6, Boolean.valueOf(z6), str5, str4, Integer.valueOf(i)));
                    Intent intent2 = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
                    intent2.putExtra("showSpn", z6);
                    intent2.putExtra("spn", str5);
                    intent2.putExtra("spnData", str4);
                    intent2.putExtra("showPlmn", z5);
                    intent2.putExtra("plmn", str6);
                    intent2.putExtra("hnbName", this.mHhbName);
                    intent2.putExtra("csgId", this.mCsgId);
                    intent2.putExtra("domain", this.mFemtocellDomain);
                    intent2.putExtra("femtocell", this.mIsFemtocell);
                    SubscriptionManager.putPhoneIdAndSubIdExtra(intent2, this.mPhone.getPhoneId());
                    this.mPhone.getContext().sendStickyBroadcastAsUser(intent2, UserHandle.ALL);
                    if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z5, str6, z6, str5)) {
                        this.mSpnUpdatePending = true;
                    }
                }
                this.mSubId = i;
                this.mCurShowSpn = z6;
                this.mCurShowPlmn = z5;
                this.mCurSpn = str5;
                this.mCurDataSpn = str4;
                this.mCurPlmn = str6;
            }
            string = this.mSS.getOperatorAlpha();
            z5 = !TextUtils.isEmpty(string) && (displayRule2 & 2) == 2;
            z4 = false;
        }
        str3 = string;
        string2 = this.mServiceStateTrackerExt.onUpdateSpnDisplay(str3, (MtkServiceState) this.mSS, this.mPhone.getPhoneId());
        if (this.mSS.getVoiceRegState() != 0) {
            if (string2 != null) {
            }
        }
        if (this.mIsImeiLock) {
        }
        if (iccRecords2 == null) {
        }
        if (z4) {
        }
        if (TextUtils.isEmpty(serviceProviderName)) {
            if (TextUtils.isEmpty(string2)) {
                if (this.mSS.getVoiceRegState() != 3) {
                }
                str4 = serviceProviderName;
                z6 = false;
                str5 = null;
            }
        }
        str6 = string2;
        if (this.mSS.getVoiceRegState() != 0) {
            z6 = false;
            str5 = null;
        }
        if (this.mServiceStateTrackerExt.needSpnRuleShowPlmnOnly()) {
            log("origin showSpn:" + z6 + " showPlmn:" + z5 + " rule:" + displayRule2);
            z6 = false;
            displayRule2 = 2;
            z5 = true;
        }
        int[] subId22 = SubscriptionManager.getSubId(this.mPhone.getPhoneId());
        if (subId22 != null) {
        }
        if (this.mServiceStateTrackerExt.allowSpnDisplayed()) {
            str7 = str5;
            str5 = str7;
        }
        if (this.mSubId == i) {
            log(String.format("updateSpnDisplay: changed sending intent rule=" + displayRule2 + " showPlmn='%b' plmn='%s' showSpn='%b' spn='%s' dataSpn='%s' subId='%d'", Boolean.valueOf(z5), str6, Boolean.valueOf(z6), str5, str4, Integer.valueOf(i)));
            Intent intent22 = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            intent22.putExtra("showSpn", z6);
            intent22.putExtra("spn", str5);
            intent22.putExtra("spnData", str4);
            intent22.putExtra("showPlmn", z5);
            intent22.putExtra("plmn", str6);
            intent22.putExtra("hnbName", this.mHhbName);
            intent22.putExtra("csgId", this.mCsgId);
            intent22.putExtra("domain", this.mFemtocellDomain);
            intent22.putExtra("femtocell", this.mIsFemtocell);
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent22, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent22, UserHandle.ALL);
            if (!this.mSubscriptionController.setPlmnSpn(this.mPhone.getPhoneId(), z5, str6, z6, str5)) {
            }
        }
        this.mSubId = i;
        this.mCurShowSpn = z6;
        this.mCurShowPlmn = z5;
        this.mCurSpn = str5;
        this.mCurDataSpn = str4;
        this.mCurPlmn = str6;
    }

    protected void log(String str) {
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.d(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.d(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        Rlog.d(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + str);
    }

    protected void loge(String str) {
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.e(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.e(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        Rlog.e(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + str);
    }

    protected void logv(String str) {
        if (this.mPhone.isPhoneTypeGsm()) {
            Rlog.v(LOG_TAG, "[GsmSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        if (this.mPhone.isPhoneTypeCdma()) {
            Rlog.v(LOG_TAG, "[CdmaSST" + this.mPhone.getPhoneId() + "] " + str);
            return;
        }
        Rlog.v(LOG_TAG, "[CdmaLteSST" + this.mPhone.getPhoneId() + "] " + str);
    }

    private void onNetworkStateChangeResult(AsyncResult asyncResult) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onNetworkStateChangeResult exception");
            return;
        }
        String[] strArr = (String[]) asyncResult.result;
        if (this.mPhone.isPhoneTypeGsm()) {
            int i = -1;
            if (strArr.length > 0) {
                Integer.parseInt(strArr[0]);
                if (strArr[1] != null && strArr[1].length() > 0) {
                    Integer.parseInt(strArr[1], 16);
                }
                if (strArr[2] != null && strArr[2].length() > 0) {
                    if (strArr[2].equals("FFFFFFFF") || strArr[2].equals("ffffffff")) {
                        log("Invalid cid:" + strArr[2]);
                        strArr[2] = "0000ffff";
                    }
                    Integer.parseInt(strArr[2], 16);
                }
                if (strArr[3] != null && strArr[3].length() > 0) {
                    Integer.parseInt(strArr[3]);
                }
                if (strArr[4] != null && strArr[4].length() > 0) {
                    i = Integer.parseInt(strArr[4]);
                }
                try {
                    if (this.mServiceStateTrackerExt.needRejectCauseNotification(i)) {
                        setRejectCauseNotification(i);
                        return;
                    }
                    return;
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return;
                }
            }
            log("onNetworkStateChangeResult length zero");
            return;
        }
        if (strArr.length > 5) {
            if (strArr[5] != null && strArr[5].length() > 0) {
                this.mNetworkExsit = 1 == Integer.parseInt(strArr[5]);
                return;
            }
            return;
        }
        log("onCdmaNetworkExistStateChanged Network existence not reported");
    }

    private void onPsNetworkStateChangeResult(AsyncResult asyncResult) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onPsNetworkStateChangeResult exception");
            return;
        }
        int[] iArr = (int[]) asyncResult.result;
        int iRegCodeToServiceState = regCodeToServiceState(iArr[0]);
        if (iArr.length >= 4) {
            log("onPsNetworkStateChangeResult, mPsRegState:" + this.mPsRegState + ",newUrcState:" + iRegCodeToServiceState + ",info[0]:" + iArr[0] + ",info[1]:" + iArr[1] + ",info[2]:" + iArr[2] + ",info[3]:" + iArr[3]);
        } else {
            log("onPsNetworkStateChangeResult, mPsRegState:" + this.mPsRegState + ",newUrcState:" + iRegCodeToServiceState + ",info[0]:" + iArr[0] + ",info[1]:" + iArr[1] + ",info[2]:" + iArr[2]);
        }
        this.mPsRegStateRaw = iArr[0];
        String strValueOf = String.valueOf(iArr[1]);
        if (strValueOf != null && strValueOf.length() >= 5) {
            updateLocatedPlmn(strValueOf);
        } else {
            updateLocatedPlmn(null);
        }
    }

    public void pollState(boolean z) {
        StringBuilder sb = new StringBuilder();
        sb.append("pollState: modemTriggered=");
        sb.append(z);
        sb.append(", mPollingContext=");
        sb.append(this.mPollingContext != null ? this.mPollingContext[0] : -1);
        sb.append(", RadioState=");
        sb.append(this.mCi.getRadioState());
        log(sb.toString());
        if (this.mPollingContext != null && this.mCi.getRadioState() != CommandsInterface.RadioState.RADIO_UNAVAILABLE && ((this.mPhone.isPhoneTypeGsm() && this.mPollingContext[0] == 4) || (!this.mPhone.isPhoneTypeGsm() && this.mPollingContext[0] == 3))) {
            this.hasPendingPollState = true;
            return;
        }
        this.mPollingContext = new int[1];
        this.mPollingContext[0] = 0;
        switch (AnonymousClass3.$SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[this.mCi.getRadioState().ordinal()]) {
            case 1:
                this.mNewSS.setStateOutOfService();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                if (this.mPhone.isPhoneTypeGsm()) {
                    this.mPsRegStateRaw = 0;
                }
                pollStateDone();
                return;
            case 2:
                this.mNewSS.setStateOff();
                this.mNewCellLoc.setStateInvalid();
                setSignalStrengthDefaultValues();
                this.mNitzState.handleNetworkUnavailable();
                if (!z && 18 != this.mSS.getRilDataRadioTechnology()) {
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mPsRegStateRaw = 0;
                    }
                    pollStateDone();
                    return;
                }
                break;
        }
        int[] iArr = this.mPollingContext;
        iArr[0] = iArr[0] + 1;
        this.mCi.getOperator(obtainMessage(6, this.mPollingContext));
        if (!((NetworkRegistrationManager) this.mRegStateManagers.get(1)).isServiceConnected()) {
            logv("old poll state");
            int[] iArr2 = this.mPollingContext;
            iArr2[0] = iArr2[0] + 1;
            this.mCi.getDataRegistrationState(obtainMessage(5, this.mPollingContext));
            int[] iArr3 = this.mPollingContext;
            iArr3[0] = iArr3[0] + 1;
            this.mCi.getVoiceRegistrationState(obtainMessage(4, this.mPollingContext));
        } else {
            int[] iArr4 = this.mPollingContext;
            iArr4[0] = iArr4[0] + 1;
            ((NetworkRegistrationManager) this.mRegStateManagers.get(1)).getNetworkRegistrationState(2, obtainMessage(5, this.mPollingContext));
            int[] iArr5 = this.mPollingContext;
            iArr5[0] = iArr5[0] + 1;
            ((NetworkRegistrationManager) this.mRegStateManagers.get(1)).getNetworkRegistrationState(1, obtainMessage(4, this.mPollingContext));
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            int[] iArr6 = this.mPollingContext;
            iArr6[0] = iArr6[0] + 1;
            this.mCi.getNetworkSelectionMode(obtainMessage(14, this.mPollingContext));
        }
    }

    static class AnonymousClass3 {
        static final int[] $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState = new int[CommandsInterface.RadioState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[CommandsInterface.RadioState.RADIO_UNAVAILABLE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$CommandsInterface$RadioState[CommandsInterface.RadioState.RADIO_OFF.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    protected void pollStateDone() {
        boolean dataRoaming;
        boolean z;
        boolean z2;
        boolean z3;
        boolean z4;
        boolean z5;
        boolean z6;
        boolean z7;
        boolean z8;
        MtkServiceState mtkServiceState;
        boolean z9;
        boolean z10;
        boolean z11;
        boolean zIsGsm;
        int rilVoiceRadioTechnology;
        boolean z12;
        boolean z13;
        boolean z14;
        boolean z15;
        boolean z16;
        boolean z17;
        boolean z18;
        boolean z19;
        boolean voiceRoaming;
        String sIMOperatorNumeric;
        int rilDataRadioTechnology;
        boolean z20;
        boolean z21;
        MtkServiceState mtkServiceState2 = this.mSS;
        MtkServiceState mtkServiceState3 = this.mNewSS;
        refreshSpn(this.mNewSS, this.mNewCellLoc, true);
        if (!this.mPhone.isPhoneTypeGsm()) {
            updateRoamingState();
        }
        boolean z22 = false;
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("telephony.test.forceRoaming", false)) {
            this.mNewSS.setVoiceRoaming(true);
            this.mNewSS.setDataRoaming(true);
        }
        useDataRegStateForDataOnlyDevices();
        resetServiceStateInIwlanMode();
        if (Build.IS_DEBUGGABLE) {
            TelephonyTester telephonyTester = this.mPhone.mTelephonyTester;
        }
        MtkServiceState mtkServiceState4 = new MtkServiceState(this.mNewSS);
        setRoamingType(mtkServiceState4);
        mtkServiceState4.mergeIwlanServiceState();
        log("Poll ServiceState done:  oldSS=[" + this.mSS + "] newSS=[" + mtkServiceState4 + "] oldMaxDataCalls=" + this.mMaxDataCalls + " mNewMaxDataCalls=" + this.mNewMaxDataCalls + " oldReasonDataDenied=" + this.mReasonDataDenied + " mNewReasonDataDenied=" + this.mNewReasonDataDenied + " isImsEccOnly= " + getImsEccOnly());
        boolean z23 = this.mSS.getVoiceRegState() != 0 && this.mNewSS.getVoiceRegState() == 0;
        boolean z24 = this.mSS.getVoiceRegState() == 0 && this.mNewSS.getVoiceRegState() != 0;
        boolean z25 = this.mSS.getDataRegState() != 0 && (this.mNewSS.getDataRegState() == 0 || mtkServiceState3.getIwlanRegState() == 0);
        boolean z26 = (this.mSS.getDataRegState() != 0 || this.mNewSS.getDataRegState() == 0 || mtkServiceState3.getIwlanRegState() == 0) ? false : true;
        boolean z27 = mtkServiceState3.getIwlanRegState() != 0 ? this.mSS.getDataRegState() != this.mNewSS.getDataRegState() : this.mSS.getDataRegState() != mtkServiceState3.getIwlanRegState();
        boolean z28 = this.mSS.getVoiceRegState() != this.mNewSS.getVoiceRegState();
        boolean z29 = !this.mNewCellLoc.equals(this.mCellLoc);
        if (mtkServiceState3.getIwlanRegState() == 0 || this.mNewSS.getDataRegState() == 0) {
            this.mRatRatcheter.ratchet(this.mSS, this.mNewSS, z29);
        }
        boolean z30 = this.mSS.getRilVoiceRadioTechnology() != this.mNewSS.getRilVoiceRadioTechnology();
        boolean z31 = mtkServiceState2.getRilDataRadioTechnology() != mtkServiceState4.getRilDataRadioTechnology();
        boolean z32 = !mtkServiceState4.equals(this.mSS) || this.mForceBroadcastServiceState;
        boolean z33 = !this.mSS.getVoiceRoaming() && this.mNewSS.getVoiceRoaming();
        boolean z34 = this.mSS.getVoiceRoaming() && !this.mNewSS.getVoiceRoaming();
        if (mtkServiceState3.getIwlanRegState() != 0) {
            z22 = !this.mSS.getDataRoaming() && this.mNewSS.getDataRoaming();
        }
        if (mtkServiceState3.getIwlanRegState() == 0) {
            dataRoaming = this.mSS.getDataRoaming();
        } else {
            dataRoaming = this.mSS.getDataRoaming() && !this.mNewSS.getDataRoaming();
        }
        boolean z35 = z28;
        boolean z36 = this.mRejectCode != this.mNewRejectCode;
        boolean z37 = this.mSS.getCssIndicator() != this.mNewSS.getCssIndicator();
        if (this.mPhone.isPhoneTypeCdmaLte()) {
            boolean z38 = this.mNewSS.getDataRegState() == 0 && ((ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && this.mNewSS.getRilDataRadioTechnology() == 13) || (this.mSS.getRilDataRadioTechnology() == 13 && ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology())));
            if (ServiceState.isLte(this.mNewSS.getRilDataRadioTechnology())) {
                z20 = z38;
            } else {
                z20 = z38;
                if (this.mNewSS.getRilDataRadioTechnology() != 13) {
                    z21 = false;
                    boolean z39 = z21;
                    z3 = this.mNewSS.getRilDataRadioTechnology() < 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
                    z = z20;
                    z2 = z39;
                }
            }
            if (!ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && this.mSS.getRilDataRadioTechnology() != 13) {
                z21 = true;
            }
            boolean z392 = z21;
            if (this.mNewSS.getRilDataRadioTechnology() < 4) {
                z3 = this.mNewSS.getRilDataRadioTechnology() < 4 && this.mNewSS.getRilDataRadioTechnology() <= 8;
                z = z20;
                z2 = z392;
            }
        } else {
            z = false;
            z2 = false;
            z3 = false;
        }
        boolean z40 = z2;
        if (!this.mSS.getVoiceRoaming() || !mtkServiceState4.getVoiceRoaming()) {
            z4 = z;
        } else {
            z4 = z;
            boolean z41 = this.mSS.getVoiceRoamingType() != mtkServiceState4.getVoiceRoamingType();
            z5 = (this.mSS.getDataRoaming() || !mtkServiceState4.getDataRoaming() || this.mSS.getDataRoamingType() == mtkServiceState4.getDataRoamingType()) ? false : true;
            StringBuilder sb = new StringBuilder();
            sb.append("pollStateDone: hasRegistered=");
            sb.append(z23);
            sb.append(" hasDeregistered=");
            sb.append(z24);
            sb.append(" hasDataAttached=");
            sb.append(z25);
            sb.append(" hasDataDetached=");
            sb.append(z26);
            sb.append(" hasDataRegStateChanged=");
            sb.append(z27);
            sb.append(" hasRilVoiceRadioTechnologyChanged= ");
            sb.append(z30);
            sb.append(" hasRilDataRadioTechnologyChanged=");
            sb.append(z31);
            sb.append(" hasChanged=");
            sb.append(z32);
            sb.append(" hasVoiceRoamingOn=");
            sb.append(z33);
            sb.append(" hasVoiceRoamingOff=");
            sb.append(z34);
            sb.append(" hasDataRoamingOn=");
            sb.append(z22);
            sb.append(" hasDataRoamingOff=");
            sb.append(dataRoaming);
            sb.append(" hasLocationChanged=");
            sb.append(z29);
            sb.append(" has4gHandoff = ");
            boolean z42 = z4;
            sb.append(z42);
            z6 = dataRoaming;
            sb.append(" hasMultiApnSupport=");
            sb.append(z40);
            sb.append(" hasLostMultiApnSupport=");
            sb.append(z3);
            sb.append(" hasCssIndicatorChanged=");
            sb.append(z37);
            z7 = z22;
            sb.append(" hasVoiceRoamingTypeChange=");
            sb.append(z41);
            sb.append(" hasDataRoamingTypeChange=");
            sb.append(z5);
            log(sb.toString());
            if (!z35 || z27) {
                z8 = z5;
                EventLog.writeEvent(!this.mPhone.isPhoneTypeGsm() ? 50114 : 50116, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
            } else {
                z8 = z5;
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                if (z30) {
                    GsmCellLocation gsmCellLocation = (GsmCellLocation) this.mNewCellLoc;
                    int cid = gsmCellLocation != null ? gsmCellLocation.getCid() : -1;
                    EventLog.writeEvent(50123, Integer.valueOf(cid), Integer.valueOf(this.mSS.getRilVoiceRadioTechnology()), Integer.valueOf(this.mNewSS.getRilVoiceRadioTechnology()));
                    log("RAT switched " + ServiceState.rilRadioTechnologyToString(this.mSS.getRilVoiceRadioTechnology()) + " -> " + ServiceState.rilRadioTechnologyToString(this.mNewSS.getRilVoiceRadioTechnology()) + " at cell " + cid);
                }
                if (z37) {
                    this.mPhone.notifyDataConnection("cssIndicatorChanged");
                }
                this.mReasonDataDenied = this.mNewReasonDataDenied;
                this.mMaxDataCalls = this.mNewMaxDataCalls;
                this.mRejectCode = this.mNewRejectCode;
            }
            ServiceState serviceState = this.mPhone.getServiceState();
            int rilDataRadioTechnology2 = this.mSS.getRilDataRadioTechnology();
            ServiceState serviceState2 = this.mSS;
            this.mSS = this.mNewSS;
            this.mNewSS = serviceState2;
            mtkServiceState = this.mSS;
            MtkServiceState mtkServiceState5 = this.mNewSS;
            CellLocation cellLocation = this.mCellLoc;
            z9 = z34;
            this.mCellLoc = this.mNewCellLoc;
            this.mNewCellLoc = cellLocation;
            if (z30) {
                updatePhoneObject();
            }
            TelephonyManager telephonyManager = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
            if (!z31) {
                if (mtkServiceState.getIwlanRegState() != 0) {
                    rilDataRadioTechnology = this.mSS.getRilDataRadioTechnology();
                } else {
                    rilDataRadioTechnology = 18;
                }
                z11 = z33;
                telephonyManager.setDataNetworkTypeForPhone(this.mPhone.getPhoneId(), rilDataRadioTechnology);
                z10 = z27;
                StatsLog.write(76, ServiceState.rilRadioTechnologyToNetworkType(this.mSS.getRilDataRadioTechnology()), this.mPhone.getPhoneId());
                if (mtkServiceState.getIwlanRegState() == 0) {
                    log("pollStateDone: IWLAN enabled");
                }
                if (this.mPhone.isPhoneTypeCdmaLte() && (rilDataRadioTechnology2 == 14 || this.mSS.getRilDataRadioTechnology() == 14)) {
                    log("[CDMALTE]pollStateDone: update signal for RAT switch between diff group");
                    sendMessage(obtainMessage(10));
                }
            } else {
                z10 = z27;
                z11 = z33;
            }
            if (z23) {
                this.mNetworkAttachedRegistrants.notifyRegistrants();
                this.mLastRegisteredPLMN = this.mSS.getOperatorNumeric();
                this.mNitzState.handleNetworkAvailable();
            }
            if (z24) {
                this.mNetworkDetachedRegistrants.notifyRegistrants();
                this.mNitzState.handleNetworkUnavailable();
            }
            if (z36) {
                setNotification(2001);
            }
            if (this.mPhone.isPhoneTypeCdmaLte() && (this.mEriTriggeredPollState || z32)) {
                this.mEriTriggeredPollState = false;
                sIMOperatorNumeric = getSIMOperatorNumeric();
                if (sIMOperatorNumeric == null && (sIMOperatorNumeric.equals("310120") || sIMOperatorNumeric.equals("310009") || sIMOperatorNumeric.equals("311490") || sIMOperatorNumeric.equals("311870"))) {
                    this.mEnableERI = true;
                } else {
                    this.mEnableERI = false;
                }
                z32 = true;
            }
            if (!z30 || z31) {
                zIsGsm = this.mSignalStrength.isGsm();
                int rilDataRadioTechnology3 = this.mSS.getRilDataRadioTechnology();
                rilVoiceRadioTechnology = this.mSS.getRilVoiceRadioTechnology();
                int voiceRegState = this.mSS.getVoiceRegState();
                z12 = z30;
                int dataRegState = this.mSS.getDataRegState();
                z13 = z31;
                z14 = (rilDataRadioTechnology3 == 18 && ServiceState.isGsm(rilDataRadioTechnology3)) || (rilVoiceRadioTechnology != 18 && ServiceState.isGsm(rilVoiceRadioTechnology));
                if (rilVoiceRadioTechnology == 0 && rilDataRadioTechnology3 == 0 && this.mPhone.isPhoneTypeGsm()) {
                    z14 = true;
                }
                if (z14 != zIsGsm && (voiceRegState == 0 || dataRegState == 0)) {
                    this.mSignalStrength.setGsm(z14);
                    notifySignalStrength();
                    log("pollStateDone: correct the mSignalStrength.isGsm New:" + z14 + " Old:" + zIsGsm);
                }
            } else {
                z13 = z31;
                z12 = z30;
            }
            if (!z32) {
                updateSpnDisplay();
                this.mForceBroadcastServiceState = false;
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
                    if (!getDesiredPowerState()) {
                        getLocaleTracker().updateOperatorNumericAsync("");
                    }
                    this.mNitzState.handleNetworkUnavailable();
                } else {
                    if (this.mSS.getRilDataRadioTechnology() != 18 && mtkServiceState.getIwlanRegState() != 0) {
                        if (!this.mPhone.isPhoneTypeGsm()) {
                            setOperatorIdd(operatorNumeric);
                        }
                        getLocaleTracker().updateOperatorNumericSync(operatorNumeric);
                        String currentCountry = getLocaleTracker().getCurrentCountry();
                        boolean zIccCardExists = iccCardExists();
                        boolean zNetworkCountryIsoChanged = networkCountryIsoChanged(currentCountry, networkCountryIso);
                        boolean z43 = zIccCardExists && zNetworkCountryIsoChanged;
                        z16 = z23;
                        z17 = z24;
                        long jCurrentTimeMillis = System.currentTimeMillis();
                        z18 = z26;
                        StringBuilder sb2 = new StringBuilder();
                        z15 = z42;
                        sb2.append("Before handleNetworkCountryCodeKnown: countryChanged=");
                        sb2.append(z43);
                        sb2.append(" iccCardExist=");
                        sb2.append(zIccCardExists);
                        sb2.append(" countryIsoChanged=");
                        sb2.append(zNetworkCountryIsoChanged);
                        sb2.append(" operatorNumeric=");
                        sb2.append(operatorNumeric);
                        sb2.append(" prevOperatorNumeric=");
                        sb2.append(networkOperatorForPhone);
                        sb2.append(" countryIsoCode=");
                        sb2.append(currentCountry);
                        sb2.append(" prevCountryIsoCode=");
                        sb2.append(networkCountryIso);
                        sb2.append(" ltod=");
                        sb2.append(TimeUtils.logTimeOfDay(jCurrentTimeMillis));
                        log(sb2.toString());
                        this.mNitzState.handleNetworkCountryCodeSet(z43);
                    }
                    int phoneId = this.mPhone.getPhoneId();
                    if (this.mPhone.isPhoneTypeGsm()) {
                        voiceRoaming = this.mSS.getVoiceRoaming() || this.mSS.getDataRoaming();
                    } else {
                        voiceRoaming = this.mSS.getVoiceRoaming();
                    }
                    telephonyManager.setNetworkRoamingForPhone(phoneId, voiceRoaming);
                    setRoamingType(this.mSS);
                    this.mSS.mergeIwlanServiceState();
                    log("Broadcasting ServiceState : " + this.mSS);
                    if (!serviceState.equals(this.mPhone.getServiceState())) {
                        this.mPhone.notifyServiceStateChanged(this.mPhone.getServiceState());
                    }
                    this.mCi.setServiceStateToModem(this.mSS.getVoiceRegState(), mtkServiceState.getCellularDataRegState(), this.mSS.getVoiceRoamingType(), mtkServiceState.getCellularDataRoamingType(), mtkServiceState.getRilVoiceRegState(), mtkServiceState.getRilCellularDataRegState(), null);
                    this.mPhone.getContext().getContentResolver().insert(Telephony.ServiceStateTable.getUriForSubscriptionId(this.mPhone.getSubId()), Telephony.ServiceStateTable.getContentValuesForServiceState(this.mSS));
                    TelephonyMetrics.getInstance().writeServiceStateChanged(this.mPhone.getPhoneId(), this.mSS);
                }
                z15 = z42;
                z16 = z23;
                z17 = z24;
                z18 = z26;
                int phoneId2 = this.mPhone.getPhoneId();
                if (this.mPhone.isPhoneTypeGsm()) {
                }
                telephonyManager.setNetworkRoamingForPhone(phoneId2, voiceRoaming);
                setRoamingType(this.mSS);
                this.mSS.mergeIwlanServiceState();
                log("Broadcasting ServiceState : " + this.mSS);
                if (!serviceState.equals(this.mPhone.getServiceState())) {
                }
                this.mCi.setServiceStateToModem(this.mSS.getVoiceRegState(), mtkServiceState.getCellularDataRegState(), this.mSS.getVoiceRoamingType(), mtkServiceState.getCellularDataRoamingType(), mtkServiceState.getRilVoiceRegState(), mtkServiceState.getRilCellularDataRegState(), null);
                this.mPhone.getContext().getContentResolver().insert(Telephony.ServiceStateTable.getUriForSubscriptionId(this.mPhone.getSubId()), Telephony.ServiceStateTable.getContentValuesForServiceState(this.mSS));
                TelephonyMetrics.getInstance().writeServiceStateChanged(this.mPhone.getPhoneId(), this.mSS);
            } else {
                z15 = z42;
                z16 = z23;
                z17 = z24;
                z18 = z26;
                this.mSS = mtkServiceState4;
            }
            if (!z25 || z15 || z18 || z16 || z17) {
                logAttachChange();
            }
            if (!z25 || z15) {
                this.mAttachedRegistrants.notifyRegistrants();
                this.mLastPSRegisteredPLMN = this.mSS.getOperatorNumeric();
            }
            if (z18) {
                this.mDetachedRegistrants.notifyRegistrants();
            }
            if (!z13 || z12) {
                logRatChange();
            }
            this.mPsRegState = this.mSS.getDataRegState();
            if (!z10 || z13) {
                notifyDataRegStateRilRadioTechnologyChanged();
                if (18 != this.mSS.getRilDataRadioTechnology()) {
                    this.mPhone.notifyDataConnection("iwlanAvailable");
                } else {
                    this.mPhone.notifyDataConnection((String) null);
                }
            }
            if (!z11 || z9 || z7 || z6) {
                logRoamingChange();
            }
            if (z11) {
                this.mVoiceRoamingOnRegistrants.notifyRegistrants();
            }
            if (z9) {
                this.mVoiceRoamingOffRegistrants.notifyRegistrants();
            }
            if (z7) {
                this.mDataRoamingOnRegistrants.notifyRegistrants();
            }
            if (z6) {
                this.mDataRoamingOffRegistrants.notifyRegistrants();
            }
            if (z8) {
                log("notify roaming type change.");
                this.mDataRoamingTypeChangedRegistrants.notifyRegistrants();
            }
            if (mtkServiceState.getCellularRegState() == 0 && (this.mLocatedPlmn == null || !this.mLocatedPlmn.equals(mtkServiceState.getOperatorNumeric()))) {
                updateLocatedPlmn(mtkServiceState.getOperatorNumeric());
            }
            if (((MtkDcTracker) this.mPhone.mDcTracker).getPendingDataCallFlag()) {
                ((MtkDcTracker) this.mPhone.mDcTracker).processPendingSetupData(this);
            }
            if (z29) {
                this.mPhone.notifyLocationChanged();
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                if (!isGprsConsistent(this.mSS.getDataRegState(), this.mSS.getVoiceRegState())) {
                    if (!this.mStartedGprsRegCheck && !this.mReportedGprsNoReg) {
                        this.mStartedGprsRegCheck = true;
                        sendMessageDelayed(obtainMessage(22), Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "gprs_register_check_period_ms", 60000));
                    }
                    z19 = false;
                } else {
                    z19 = false;
                    this.mReportedGprsNoReg = false;
                }
            } else {
                z19 = false;
            }
            if (this.hasPendingPollState) {
                this.hasPendingPollState = z19;
                modemTriggeredPollState();
            }
            this.mNewSS.setStateOutOfService();
            if (!notifySignalStrength()) {
                log("PollStateDone with signal notification, level =" + this.mSignalStrength.getLevel());
                return;
            }
            return;
        }
        if (this.mSS.getDataRoaming()) {
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("pollStateDone: hasRegistered=");
        sb3.append(z23);
        sb3.append(" hasDeregistered=");
        sb3.append(z24);
        sb3.append(" hasDataAttached=");
        sb3.append(z25);
        sb3.append(" hasDataDetached=");
        sb3.append(z26);
        sb3.append(" hasDataRegStateChanged=");
        sb3.append(z27);
        sb3.append(" hasRilVoiceRadioTechnologyChanged= ");
        sb3.append(z30);
        sb3.append(" hasRilDataRadioTechnologyChanged=");
        sb3.append(z31);
        sb3.append(" hasChanged=");
        sb3.append(z32);
        sb3.append(" hasVoiceRoamingOn=");
        sb3.append(z33);
        sb3.append(" hasVoiceRoamingOff=");
        sb3.append(z34);
        sb3.append(" hasDataRoamingOn=");
        sb3.append(z22);
        sb3.append(" hasDataRoamingOff=");
        sb3.append(dataRoaming);
        sb3.append(" hasLocationChanged=");
        sb3.append(z29);
        sb3.append(" has4gHandoff = ");
        boolean z422 = z4;
        sb3.append(z422);
        z6 = dataRoaming;
        sb3.append(" hasMultiApnSupport=");
        sb3.append(z40);
        sb3.append(" hasLostMultiApnSupport=");
        sb3.append(z3);
        sb3.append(" hasCssIndicatorChanged=");
        sb3.append(z37);
        z7 = z22;
        sb3.append(" hasVoiceRoamingTypeChange=");
        sb3.append(z41);
        sb3.append(" hasDataRoamingTypeChange=");
        sb3.append(z5);
        log(sb3.toString());
        if (!z35) {
            z8 = z5;
            EventLog.writeEvent(!this.mPhone.isPhoneTypeGsm() ? 50114 : 50116, Integer.valueOf(this.mSS.getVoiceRegState()), Integer.valueOf(this.mSS.getDataRegState()), Integer.valueOf(this.mNewSS.getVoiceRegState()), Integer.valueOf(this.mNewSS.getDataRegState()));
        }
        if (this.mPhone.isPhoneTypeGsm()) {
        }
        ServiceState serviceState3 = this.mPhone.getServiceState();
        int rilDataRadioTechnology22 = this.mSS.getRilDataRadioTechnology();
        ServiceState serviceState22 = this.mSS;
        this.mSS = this.mNewSS;
        this.mNewSS = serviceState22;
        mtkServiceState = this.mSS;
        MtkServiceState mtkServiceState52 = this.mNewSS;
        CellLocation cellLocation2 = this.mCellLoc;
        z9 = z34;
        this.mCellLoc = this.mNewCellLoc;
        this.mNewCellLoc = cellLocation2;
        if (z30) {
        }
        TelephonyManager telephonyManager2 = (TelephonyManager) this.mPhone.getContext().getSystemService("phone");
        if (!z31) {
        }
        if (z23) {
        }
        if (z24) {
        }
        if (z36) {
        }
        if (this.mPhone.isPhoneTypeCdmaLte()) {
            this.mEriTriggeredPollState = false;
            sIMOperatorNumeric = getSIMOperatorNumeric();
            if (sIMOperatorNumeric == null) {
                this.mEnableERI = false;
                z32 = true;
            }
        }
        if (!z30) {
            zIsGsm = this.mSignalStrength.isGsm();
            int rilDataRadioTechnology32 = this.mSS.getRilDataRadioTechnology();
            rilVoiceRadioTechnology = this.mSS.getRilVoiceRadioTechnology();
            int voiceRegState2 = this.mSS.getVoiceRegState();
            z12 = z30;
            int dataRegState2 = this.mSS.getDataRegState();
            z13 = z31;
            if (rilDataRadioTechnology32 == 18) {
                if (rilVoiceRadioTechnology == 0) {
                    z14 = true;
                }
                if (z14 != zIsGsm) {
                    this.mSignalStrength.setGsm(z14);
                    notifySignalStrength();
                    log("pollStateDone: correct the mSignalStrength.isGsm New:" + z14 + " Old:" + zIsGsm);
                }
            } else {
                if (rilVoiceRadioTechnology == 0) {
                }
                if (z14 != zIsGsm) {
                }
            }
        }
        if (!z32) {
        }
        if (!z25) {
            logAttachChange();
        }
        if (!z25) {
            this.mAttachedRegistrants.notifyRegistrants();
            this.mLastPSRegisteredPLMN = this.mSS.getOperatorNumeric();
        }
        if (z18) {
        }
        if (!z13) {
            logRatChange();
        }
        this.mPsRegState = this.mSS.getDataRegState();
        if (!z10) {
            notifyDataRegStateRilRadioTechnologyChanged();
            if (18 != this.mSS.getRilDataRadioTechnology()) {
            }
        }
        if (!z11) {
            logRoamingChange();
        }
        if (z11) {
        }
        if (z9) {
        }
        if (z7) {
        }
        if (z6) {
        }
        if (z8) {
        }
        if (mtkServiceState.getCellularRegState() == 0) {
            updateLocatedPlmn(mtkServiceState.getOperatorNumeric());
        }
        if (((MtkDcTracker) this.mPhone.mDcTracker).getPendingDataCallFlag()) {
        }
        if (z29) {
        }
        if (this.mPhone.isPhoneTypeGsm()) {
        }
        if (this.hasPendingPollState) {
        }
        this.mNewSS.setStateOutOfService();
        if (!notifySignalStrength()) {
        }
    }

    private final boolean isConcurrentVoiceAndDataAllowedForIwlan() {
        if (this.mSS.getDataRegState() == 0 && this.mSS.getRilDataRadioTechnology() == 18 && getImsServiceState() == 0) {
            return true;
        }
        return false;
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        if (this.mSS.getCssIndicator() == 1) {
            return true;
        }
        if (this.mPhone.isPhoneTypeGsm()) {
            if (isConcurrentVoiceAndDataAllowedForVolte() || isConcurrentVoiceAndDataAllowedForIwlan()) {
                return true;
            }
            return this.mSS.getRilVoiceRadioTechnology() != 16 && this.mSS.getRilDataRadioTechnology() >= 3;
        }
        if (this.mPhone.isPhoneTypeCdma()) {
            return false;
        }
        return (SystemProperties.getInt("ro.boot.opt_c2k_lte_mode", 0) == 1 && this.mSS.getRilDataRadioTechnology() == 14) || isConcurrentVoiceAndDataAllowedForVolte() || this.mSS.getCssIndicator() == 1;
    }

    private int calculateDeviceRatMode(int i) {
        int iNeedAutoSwitchRatMode;
        int iCalculatePreferredNetworkType;
        if (this.mPhone.isPhoneTypeGsm()) {
            try {
                if (this.mServiceStateTrackerExt.isSupportRatBalancing()) {
                    log("networkType is controlled by RAT Blancing, no need to set network type");
                    return -1;
                }
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
            try {
                iNeedAutoSwitchRatMode = this.mServiceStateTrackerExt.needAutoSwitchRatMode(i, this.mLocatedPlmn);
            } catch (RuntimeException e2) {
                e2.printStackTrace();
                iNeedAutoSwitchRatMode = -1;
            }
            iCalculatePreferredNetworkType = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), this.mPhone.getSubId());
            if (iNeedAutoSwitchRatMode >= 0 && iNeedAutoSwitchRatMode != iCalculatePreferredNetworkType) {
                log("Revise networkType to " + iNeedAutoSwitchRatMode);
                iCalculatePreferredNetworkType = iNeedAutoSwitchRatMode;
            }
        } else {
            iCalculatePreferredNetworkType = PhoneFactory.calculatePreferredNetworkType(this.mPhone.getContext(), this.mPhone.getSubId());
            try {
                iNeedAutoSwitchRatMode = this.mServiceStateTrackerExt.needAutoSwitchRatMode(i, this.mLocatedPlmn);
            } catch (RuntimeException e3) {
                e3.printStackTrace();
                iNeedAutoSwitchRatMode = -1;
            }
            if (iNeedAutoSwitchRatMode >= 0 && iNeedAutoSwitchRatMode != iCalculatePreferredNetworkType) {
                log("Revise networkType to " + iNeedAutoSwitchRatMode);
                iCalculatePreferredNetworkType = iNeedAutoSwitchRatMode;
            }
        }
        log("restrictedNwMode=" + iNeedAutoSwitchRatMode + " calculateDeviceRatMode=" + iCalculatePreferredNetworkType);
        return iCalculatePreferredNetworkType;
    }

    public void setDeviceRatMode(int i) {
        if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
            int iCalculateDeviceRatMode = calculateDeviceRatMode(i);
            if (iCalculateDeviceRatMode >= 0) {
                this.mPhone.setPreferredNetworkType(iCalculateDeviceRatMode, (Message) null);
                return;
            }
            return;
        }
        log("Invalid subId, skip setDeviceRatMode!");
    }

    public boolean willLocatedPlmnChange() {
        MtkServiceState mtkServiceState = this.mSS;
        if (mtkServiceState == null || this.mLocatedPlmn == null || mtkServiceState.getCellularRegState() != 0 || this.mSS.getOperatorNumeric().equals(this.mLocatedPlmn)) {
            return false;
        }
        return true;
    }

    public String getLocatedPlmn() {
        return this.mLocatedPlmn;
    }

    private void updateLocatedPlmn(String str) {
        if ((this.mLocatedPlmn == null && str != null) || ((this.mLocatedPlmn != null && str == null) || (this.mLocatedPlmn != null && str != null && !this.mLocatedPlmn.equals(str)))) {
            log("updateLocatedPlmn(),previous plmn= " + this.mLocatedPlmn + " ,update to: " + str);
            Intent intent = new Intent("mediatek.intent.action.LOCATED_PLMN_CHANGED");
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            intent.putExtra("plmn", str);
            if (str != null) {
                try {
                    intent.putExtra("iso", MccTable.countryCodeForMcc(Integer.parseInt(str.substring(0, 3))));
                } catch (NumberFormatException e) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + e);
                    intent.putExtra("iso", "");
                } catch (StringIndexOutOfBoundsException e2) {
                    loge("updateLocatedPlmn: countryCodeForMcc error" + e2);
                    intent.putExtra("iso", "");
                }
                this.mLocatedPlmn = str;
                setDeviceRatMode(this.mPhone.getPhoneId());
            } else {
                intent.putExtra("iso", "");
            }
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
        this.mLocatedPlmn = str;
    }

    protected void refreshSpn(ServiceState serviceState, CellLocation cellLocation, boolean z) {
        String strLookupOperatorName;
        String operatorBrandOverride = this.mUiccController.getUiccCard(getPhoneId()) != null ? this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() : null;
        if (operatorBrandOverride != null) {
            log("refreshSpn: use brandOverride" + operatorBrandOverride);
            strLookupOperatorName = operatorBrandOverride;
        } else {
            int lac = this.mPhone.getPhoneType() == 1 ? ((GsmCellLocation) cellLocation).getLac() : -1;
            operatorBrandOverride = this.mCi.lookupOperatorName(this.mPhone.getSubId(), serviceState.getOperatorNumeric(), true, lac);
            strLookupOperatorName = this.mCi.lookupOperatorName(this.mPhone.getSubId(), serviceState.getOperatorNumeric(), false, lac);
        }
        if (!TextUtils.equals(operatorBrandOverride, serviceState.getOperatorAlphaLong()) || !TextUtils.equals(strLookupOperatorName, serviceState.getOperatorAlphaShort())) {
            this.mForceBroadcastServiceState = true;
            if (z) {
                serviceState.setOperatorName(operatorBrandOverride, strLookupOperatorName, serviceState.getOperatorNumeric());
            }
        }
        log("refreshSpn: " + operatorBrandOverride + ", " + strLookupOperatorName + ", fromPollState=" + z + ", mForceBroadcastServiceState=" + this.mForceBroadcastServiceState);
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
            if (this.mPhone.isPhoneTypeGsm()) {
                setDeviceRatMode(this.mPhone.getPhoneId());
            }
            RadioManager.getInstance();
            RadioManager.sendRequestBeforeSetRadioPower(true, this.mPhone.getPhoneId());
            this.mCi.setRadioPower(true, (Message) null);
            return;
        }
        if ((!this.mDesiredPowerState || this.mRadioDisabledByCarrier) && this.mCi.getRadioState().isOn()) {
            if (this.mPhone.isPhoneTypeGsm() && this.mPowerOffDelayNeed) {
                if (this.mImsRegistrationOnOff && !this.mAlarmSwitch) {
                    log("mImsRegistrationOnOff == true");
                    Context context = this.mPhone.getContext();
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService("alarm");
                    this.mRadioOffIntent = PendingIntent.getBroadcast(context, 0, new Intent("android.intent.action.ACTION_RADIO_OFF"), 0);
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
            this.mCi.requestShutdown((Message) null);
        }
    }

    protected void hangupAndPowerOff() {
        if (!this.mPhone.isPhoneTypeGsm() || this.mPhone.isInCall()) {
            this.mPhone.mCT.mRingingCall.hangupIfAlive();
            this.mPhone.mCT.mBackgroundCall.hangupIfAlive();
            this.mPhone.mCT.mForegroundCall.hangupIfAlive();
        }
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsPhone != null && !imsPhone.isWifiCallingEnabled()) {
            imsPhone.getForegroundCall().hangupIfAlive();
            imsPhone.getBackgroundCall().hangupIfAlive();
            imsPhone.getRingingCall().hangupIfAlive();
            log("hangupAndPowerOff: hangup VoLTE call.");
        }
        RadioManager.getInstance();
        RadioManager.sendRequestBeforeSetRadioPower(false, this.mPhone.getPhoneId());
        this.mCi.setRadioPower(false, obtainMessage(54));
    }

    private void onFemtoCellInfoResult(AsyncResult asyncResult) {
        int i;
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onFemtoCellInfo exception");
            return;
        }
        String[] strArr = (String[]) asyncResult.result;
        if (strArr.length > 0) {
            if (strArr[0] != null && strArr[0].length() > 0) {
                this.mFemtocellDomain = Integer.parseInt(strArr[0]);
                log("onFemtoCellInfo: mFemtocellDomain set to " + this.mFemtocellDomain);
            }
            if (strArr[5] != null && strArr[5].length() > 0) {
                i = Integer.parseInt(strArr[5]);
            } else {
                i = 0;
            }
            this.mIsFemtocell = i;
            log("onFemtoCellInfo: domain= " + this.mFemtocellDomain + ",isCsgCell= " + i);
            if (i == 1) {
                if (strArr[6] != null && strArr[6].length() > 0) {
                    this.mCsgId = strArr[6];
                    log("onFemtoCellInfo: mCsgId set to " + this.mCsgId);
                }
                if (strArr[8] != null && strArr[8].length() > 0) {
                    this.mHhbName = new String(IccUtils.hexStringToBytes(strArr[8]));
                    log("onFemtoCellInfo: mHhbName set from " + strArr[8] + " to " + this.mHhbName);
                } else {
                    this.mHhbName = null;
                    log("onFemtoCellInfo: mHhbName is not available ,set to null");
                }
            } else {
                this.mCsgId = null;
                this.mHhbName = null;
                log("onFemtoCellInfo: csgId and hnbName are cleared");
            }
            if (i != 2 && strArr[1] != null && strArr[1].length() > 0 && strArr[9] != null && strArr[0].length() > 0) {
                int i2 = Integer.parseInt(strArr[1]);
                int i3 = Integer.parseInt(strArr[9]);
                try {
                    if (this.mServiceStateTrackerExt.needIgnoreFemtocellUpdate(i2, i3)) {
                        log("needIgnoreFemtocellUpdate due to state= " + i2 + ",cause= " + i3);
                        return;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
            Intent intent = new Intent("android.provider.Telephony.SPN_STRINGS_UPDATED");
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            intent.putExtra("showSpn", this.mCurShowSpn);
            intent.putExtra("spn", this.mCurSpn);
            intent.putExtra("showPlmn", this.mCurShowPlmn);
            intent.putExtra("plmn", this.mCurPlmn);
            intent.putExtra("hnbName", this.mHhbName);
            intent.putExtra("csgId", this.mCsgId);
            intent.putExtra("domain", this.mFemtocellDomain);
            intent.putExtra("femtocell", this.mIsFemtocell);
            this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            int phoneId = this.mPhone.getPhoneId();
            String str = this.mCurPlmn;
            if (this.mHhbName == null && this.mCsgId != null) {
                try {
                    if (this.mServiceStateTrackerExt.needToShowCsgId()) {
                        String str2 = str + " - ";
                        try {
                            str = str2 + this.mCsgId;
                        } catch (RuntimeException e2) {
                            e = e2;
                            str = str2;
                            e.printStackTrace();
                        }
                    }
                } catch (RuntimeException e3) {
                    e = e3;
                }
            } else if (this.mHhbName != null) {
                str = (str + " - ") + this.mHhbName;
            }
            if (!this.mSubscriptionController.setPlmnSpn(phoneId, this.mCurShowPlmn, str, this.mCurShowSpn, this.mCurSpn)) {
                this.mSpnUpdatePending = true;
            }
        }
    }

    private void onInvalidSimInfoReceived(AsyncResult asyncResult) {
        String[] strArr = (String[]) asyncResult.result;
        String str = strArr[0];
        int i = Integer.parseInt(strArr[1]);
        int i2 = Integer.parseInt(strArr[2]);
        int i3 = Integer.parseInt(strArr[3]);
        int i4 = SystemProperties.getInt("vendor.gsm.gcf.testmode", 0);
        log("onInvalidSimInfoReceived testMode:" + i4 + " cause:" + i3 + " cs_invalid:" + i + " ps_invalid:" + i2 + " plmn:" + str + " mEverIVSR:" + this.mEverIVSR);
        if (i4 != 0) {
            log("InvalidSimInfo received during test mode: " + i4);
            return;
        }
        if (this.mServiceStateTrackerExt.isNeedDisableIVSR()) {
            log("Disable IVSR");
            return;
        }
        if (i == 1) {
            this.isCsInvalidCard = true;
        }
        if (this.mMtkVoiceCapable && i == 1 && this.mLastRegisteredPLMN != null && str.equals(this.mLastRegisteredPLMN)) {
            log("InvalidSimInfo reset SIM due to CS invalid");
            setEverIVSR(true);
            this.mLastRegisteredPLMN = null;
            this.mLastPSRegisteredPLMN = null;
            this.mCi.setSimPower(2, null);
            return;
        }
        if (i2 == 1 && isAllowRecoveryOnIvsr(asyncResult) && this.mLastPSRegisteredPLMN != null && str.equals(this.mLastPSRegisteredPLMN)) {
            log("InvalidSimInfo reset SIM due to PS invalid ");
            setEverIVSR(true);
            this.mLastRegisteredPLMN = null;
            this.mLastPSRegisteredPLMN = null;
            this.mCi.setSimPower(2, null);
        }
    }

    private void onNetworkEventReceived(AsyncResult asyncResult) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onNetworkEventReceived exception");
            return;
        }
        int i = ((int[]) asyncResult.result)[1];
        log("[onNetworkEventReceived] event_type:" + i);
        Intent intent = new Intent("android.intent.action.ACTION_NETWORK_EVENT");
        intent.addFlags(536870912);
        intent.putExtra("eventType", i + 1);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private void onModulationInfoReceived(AsyncResult asyncResult) {
        if (asyncResult.exception != null || asyncResult.result == null) {
            loge("onModulationInfoReceived exception");
            return;
        }
        int i = ((int[]) asyncResult.result)[0];
        log("[onModulationInfoReceived] modulation:" + i);
        Intent intent = new Intent("mediatek.intent.action.ACTION_NOTIFY_MODULATION_INFO");
        intent.addFlags(536870912);
        intent.putExtra("modulation_info", i);
        this.mPhone.getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
    }

    private boolean isAllowRecoveryOnIvsr(AsyncResult asyncResult) {
        if (this.mPhone.isInCall()) {
            log("[isAllowRecoveryOnIvsr] isInCall()=true");
            Message messageObtainMessage = obtainMessage();
            messageObtainMessage.what = 101;
            messageObtainMessage.obj = asyncResult;
            sendMessageDelayed(messageObtainMessage, 10000L);
            return false;
        }
        log("isAllowRecoveryOnIvsr() return true");
        return true;
    }

    private void setEverIVSR(boolean z) {
        log("setEverIVSR:" + z);
        this.mEverIVSR = z;
        if (z) {
            Intent intent = new Intent("mediatek.intent.action.IVSR_NOTIFY");
            intent.putExtra("action", "start");
            SubscriptionManager.putPhoneIdAndSubIdExtra(intent, this.mPhone.getPhoneId());
            if (TelephonyManager.getDefault().getPhoneCount() == 1) {
                intent.addFlags(536870912);
            }
            log("broadcast ACTION_IVSR_NOTIFY intent");
            this.mPhone.getContext().sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    private void setNullState() {
        this.isCsInvalidCard = false;
    }

    protected final boolean IsInternationalRoamingException(String str) {
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        if (carrierConfigManager == null) {
            Rlog.e(LOG_TAG, "Carrier config service is not available");
            return false;
        }
        PersistableBundle configForSubId = carrierConfigManager.getConfigForSubId(this.mPhone.getSubId());
        if (configForSubId == null) {
            Rlog.e(LOG_TAG, "Can't get the config. subId = " + this.mPhone.getSubId());
            return false;
        }
        String[] stringArray = configForSubId.getStringArray("carrier_international_roaming_exception_list_strings");
        if (stringArray == null) {
            Rlog.e(LOG_TAG, "carrier_international_roaming_exception_list_strings is not available. subId = " + this.mPhone.getSubId());
            return false;
        }
        HashSet hashSet = new HashSet(Arrays.asList(stringArray));
        Rlog.d(LOG_TAG, "For subId = " + this.mPhone.getSubId() + ", international roaming exceptions are " + Arrays.toString(hashSet.toArray()) + ", operatorNumeric = " + str);
        if (hashSet.contains(str)) {
            Rlog.d(LOG_TAG, str + " in list.");
            return true;
        }
        Rlog.d(LOG_TAG, str + " is not in list.");
        return false;
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
                    if (IsInternationalRoamingException(serviceState.getVoiceOperatorNumeric())) {
                        log(serviceState.getVoiceOperatorNumeric() + " is in operator defined international roaming list");
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
                    }
                    if (inSameCountry(serviceState.getVoiceOperatorNumeric())) {
                        serviceState.setDataRoamingType(2);
                    } else {
                        serviceState.setDataRoamingType(3);
                    }
                    if (IsInternationalRoamingException(serviceState.getVoiceOperatorNumeric())) {
                        log(serviceState.getVoiceOperatorNumeric() + " is in operator defined international roaming list");
                        serviceState.setDataRoamingType(3);
                        return;
                    }
                    return;
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

    private final boolean isConcurrentVoiceAndDataAllowedForVolte() {
        if (this.mSS.getDataRegState() == 0 && ServiceState.isLte(this.mSS.getRilDataRadioTechnology()) && getImsServiceState() == 0) {
            return true;
        }
        return false;
    }

    private final int getImsServiceState() {
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsPhone != null) {
            return imsPhone.getServiceState().getState();
        }
        return 1;
    }

    private final boolean mergeEmergencyOnlyCdmaIms(boolean z) {
        Phone imsPhone;
        if (!z && this.mNewSS.getVoiceRegState() == 1 && this.mNewSS.getDataRegState() == 1 && (imsPhone = this.mPhone.getImsPhone()) != null) {
            return imsPhone.getServiceState().isEmergencyOnly();
        }
        return z;
    }

    private void setRejectCauseNotification(int i) {
        log("setRejectCauseNotification: create notification " + i);
        Context context = this.mPhone.getContext();
        this.mNotificationBuilder = new Notification.Builder(context);
        this.mNotificationBuilder.setWhen(System.currentTimeMillis());
        this.mNotificationBuilder.setAutoCancel(true);
        this.mNotificationBuilder.setSmallIcon(R.drawable.stat_sys_warning);
        this.mNotificationBuilder.setChannel("alert");
        this.mNotificationBuilder.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 134217728));
        CharSequence text = "";
        CharSequence text2 = context.getText(134545511);
        switch (i) {
            case 2:
                text = context.getText(134545512);
                break;
            case 3:
                text = context.getText(134545513);
                break;
            case 5:
                text = context.getText(134545520);
                break;
            case 6:
                text = context.getText(134545521);
                break;
            case 13:
                text = context.getText(134545525);
                break;
        }
        log("setRejectCauseNotification: put notification " + ((Object) text2) + " / " + ((Object) text));
        this.mNotificationBuilder.setContentTitle(text2);
        this.mNotificationBuilder.setContentText(text);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
        this.mNotification = this.mNotificationBuilder.build();
        notificationManager.notify(REJECT_NOTIFICATION, this.mNotification);
    }

    protected boolean isOperatorConsideredNonRoaming(ServiceState serviceState) {
        boolean zIsOperatorConsideredNonRoaming = super.isOperatorConsideredNonRoaming(serviceState);
        if (zIsOperatorConsideredNonRoaming) {
            log("isOperatorConsideredNonRoaming true");
        }
        return zIsOperatorConsideredNonRoaming;
    }

    protected boolean isOperatorConsideredRoaming(ServiceState serviceState) {
        boolean zIsOperatorConsideredRoaming = super.isOperatorConsideredRoaming(serviceState);
        if (zIsOperatorConsideredRoaming) {
            log("isOperatorConsideredRoaming true");
        }
        return zIsOperatorConsideredRoaming;
    }

    protected void onUpdateIccAvailability() {
        if (this.mUiccController == null) {
            return;
        }
        UiccCardApplication uiccCardApplication = getUiccCardApplication();
        if ((this.mPhone.isPhoneTypeCdma() || this.mPhone.isPhoneTypeCdmaLte()) && uiccCardApplication != null) {
            IccCardApplicationStatus.AppState state = uiccCardApplication.getState();
            if ((state == IccCardApplicationStatus.AppState.APPSTATE_PIN || state == IccCardApplicationStatus.AppState.APPSTATE_PUK) && this.mNetworkExsit) {
                this.mEmergencyOnly = true;
            } else {
                this.mEmergencyOnly = false;
            }
            this.mEmergencyOnly = mergeEmergencyOnlyCdmaIms(this.mEmergencyOnly);
            log("[CDMA]onUpdateIccAvailability, appstate=" + state + ", mNetworkExsit=" + this.mNetworkExsit + ", mEmergencyOnly=" + this.mEmergencyOnly);
        }
        if (this.mUiccApplcation != uiccCardApplication) {
            if (this.mUiccApplcation != null) {
                log("Removing stale icc objects.");
                this.mUiccApplcation.unregisterForReady(this);
                if (this.mIccRecords != null) {
                    this.mIccRecords.unregisterForRecordsLoaded(this);
                    if (this.mPhone.isPhoneTypeGsm()) {
                        this.mIccRecords.unregisterForRecordsEvents(this);
                    }
                }
                this.mIccRecords = null;
                this.mUiccApplcation = null;
            }
            if (uiccCardApplication != null) {
                logv("New card found");
                this.mUiccApplcation = uiccCardApplication;
                this.mIccRecords = this.mUiccApplcation.getIccRecords();
                if (this.mPhone.isPhoneTypeGsm()) {
                    this.mUiccApplcation.registerForReady(this, 17, (Object) null);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.registerForRecordsLoaded(this, 16, (Object) null);
                        this.mIccRecords.registerForRecordsEvents(this, 119, (Object) null);
                        return;
                    }
                    return;
                }
                if (this.mIsSubscriptionFromRuim) {
                    this.mUiccApplcation.registerForReady(this, 26, (Object) null);
                    if (this.mIccRecords != null) {
                        this.mIccRecords.registerForRecordsLoaded(this, 27, (Object) null);
                    }
                }
            }
        }
    }

    protected void updateOperatorNameFromEri() {
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
            if (!((this.mUiccController.getUiccCard(getPhoneId()) == null || this.mUiccController.getUiccCard(getPhoneId()).getOperatorBrandOverride() == null) ? false : true) && this.mCi.getRadioState().isOn() && this.mPhone.isEriFileLoaded() && ((!ServiceState.isLte(this.mSS.getRilVoiceRadioTechnology()) || this.mPhone.getContext().getResources().getBoolean(R.^attr-private.__removed3)) && !this.mIsSubscriptionFromRuim && this.mEnableERI)) {
                String operatorAlpha = this.mSS.getOperatorAlpha();
                if (this.mSS.getVoiceRegState() == 0) {
                    if (operatorAlpha == null || operatorAlpha.length() == 0) {
                        operatorAlpha = this.mPhone.getCdmaEriText();
                    } else if (this.mPhone.getCdmaEriText() != null && !this.mPhone.getCdmaEriText().equals("") && this.mSS.getCdmaRoamingIndicator() != 1 && this.mSS.getCdmaRoamingIndicator() != 160) {
                        log("Append ERI text to PLMN String");
                        operatorAlpha = this.mSS.getOperatorAlphaLong() + "- " + this.mPhone.getCdmaEriText();
                    }
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
                boolean csimSpnDisplayCondition = this.mIccRecords.getCsimSpnDisplayCondition();
                if (csimSpnDisplayCondition) {
                    try {
                        csimSpnDisplayCondition = this.mServiceStateTrackerExt.allowSpnDisplayed();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
                int cdmaEriIconIndex = this.mSS.getCdmaEriIconIndex();
                if (csimSpnDisplayCondition && cdmaEriIconIndex == 1 && isInHomeSidNid(this.mSS.getCdmaSystemId(), this.mSS.getCdmaNetworkId()) && this.mIccRecords != null) {
                    this.mSS.setOperatorAlphaLong(this.mIccRecords.getServiceProviderName());
                }
            }
        }
    }

    protected void setOperatorIdd(String str) {
        String iddByMcc;
        try {
            iddByMcc = this.mHbpcdUtils.getIddByMcc(Integer.parseInt(str.substring(0, 3)));
        } catch (NumberFormatException e) {
            loge("setOperatorIdd: idd error" + e);
            iddByMcc = "";
        } catch (StringIndexOutOfBoundsException e2) {
            loge("setOperatorIdd: idd error" + e2);
            iddByMcc = "";
        }
        if (iddByMcc != null && !iddByMcc.isEmpty()) {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", iddByMcc);
        } else {
            this.mPhone.setGlobalSystemProperty("gsm.operator.idpstring", "+");
        }
    }

    public void setRadioPowerFromCarrier(boolean z) {
        this.mRadioDisabledByCarrier = !z;
        RadioManager.getInstance().setRadioPower(z, this.mPhone.getPhoneId());
    }

    private String getSIMOperatorNumeric() {
        String imsi;
        IccRecords iccRecords = this.mIccRecords;
        if (iccRecords != null) {
            String operatorNumeric = iccRecords.getOperatorNumeric();
            if (operatorNumeric == null && (imsi = iccRecords.getIMSI()) != null && !imsi.equals("")) {
                operatorNumeric = imsi.substring(0, 5);
                log("get MCC/MNC from IMSI = " + operatorNumeric);
            }
            if (this.mPhone.isPhoneTypeGsm()) {
                if (operatorNumeric == null || operatorNumeric.equals("")) {
                    String str = "vendor.gsm.ril.uicc.mccmnc";
                    if (this.mPhone.getPhoneId() != 0) {
                        str = "vendor.gsm.ril.uicc.mccmnc." + this.mPhone.getPhoneId();
                    }
                    String str2 = SystemProperties.get(str, "");
                    log("get MccMnc from property(" + str + "): " + str2);
                    return str2;
                }
                return operatorNumeric;
            }
            return operatorNumeric;
        }
        return null;
    }

    protected int mtkReplaceDdsIfUnset(int i) {
        int[] subId;
        if (i == -1 && (subId = SubscriptionManager.getSubId(RadioCapabilitySwitchUtil.getMainCapabilityPhoneId())) != null && subId.length > 0) {
            log("powerOffRadioSafely: replace dds with main protocol sub ");
            return subId[0];
        }
        return i;
    }

    protected void mtkRegisterAllDataDisconnected() {
        boolean zEquals = TextUtils.equals(SystemProperties.get("persist.radio.airplane_mode_on", ""), "1");
        int iMtkReplaceDdsIfUnset = mtkReplaceDdsIfUnset(SubscriptionManager.getDefaultDataSubscriptionId());
        boolean zIsDataDisconnected = ProxyController.getInstance().isDataDisconnected(iMtkReplaceDdsIfUnset);
        log("powerOffRadioSafely: apm:" + zEquals + ", dds:" + iMtkReplaceDdsIfUnset + ", mSubId:" + this.mPhone.getSubId() + ", shutdown:" + isDeviceShuttingDown() + ", isDefaultDataDisconnected:" + zIsDataDisconnected);
        if (iMtkReplaceDdsIfUnset == this.mPhone.getSubId() || ((!zEquals && !isDeviceShuttingDown()) || zIsDataDisconnected)) {
            synchronized (this) {
                log("powerOffRadioSafely: register EVENT_ALL_DATA_DISCONNECTED for self phone");
                this.mPhone.registerForAllDataDisconnected(this, 49, (Object) null);
                this.mPendingRadioPowerOffAfterDataOff = true;
            }
        }
    }

    protected int mtkReplaceDisconnectTimer() {
        if (isDeviceShuttingDown()) {
            log("Shutting down, reduce 30s->5s for data to disconnect, then turn off radio.");
            return 5000;
        }
        return 30000;
    }

    protected static final String lookupOperatorName(Context context, int i, String str, boolean z) {
        String spnByNumeric;
        if (PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i)) == null) {
            Rlog.e(LOG_TAG, "lookupOperatorName getPhone null");
            return str;
        }
        String spnByPattern = MtkSpnOverride.getInstance().getSpnByPattern(i, str);
        boolean zIsChinaTelecomMvno = isChinaTelecomMvno(context, i, str, spnByPattern);
        if (spnByPattern == null || zIsChinaTelecomMvno) {
            spnByNumeric = MtkSpnOverride.getInstance().getSpnByNumeric(str, z, context);
        } else {
            spnByNumeric = spnByPattern;
        }
        return spnByNumeric == null ? str : spnByNumeric;
    }

    private static final boolean isChinaTelecomMvno(Context context, int i, String str, String str2) {
        String string = context.getText(134545641).toString();
        String simOperatorName = TelephonyManager.from(context).getSimOperatorName(i);
        if (string != null && string.equals(str2)) {
            return true;
        }
        if (("20404".equals(str) || "45403".equals(str)) && string != null && string.equals(simOperatorName)) {
            return true;
        }
        return false;
    }

    protected boolean onSignalStrengthResult(AsyncResult asyncResult) {
        this.mSS.getRilDataRadioTechnology();
        this.mSS.getRilVoiceRadioTechnology();
        String str = "";
        if (this.mSignalStrength != null) {
            str = "old:{level:" + this.mSignalStrength.getLevel() + ", raw:" + this.mSignalStrength.toString() + "}, ";
        }
        boolean zOnSignalStrengthResult = super.onSignalStrengthResult(asyncResult);
        if (this.mSignalStrength != null) {
            str = str + "new:{level:" + this.mSignalStrength.getLevel() + ", raw:" + this.mSignalStrength.toString() + "}";
        }
        log(str);
        return zOnSignalStrengthResult;
    }

    protected boolean currentMccEqualsSimMcc(ServiceState serviceState) {
        try {
            return ((TelephonyManager) this.mPhone.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(getPhoneId()).substring(0, 3).equals(serviceState.getOperatorNumeric().substring(0, 3));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean getImsEccOnly() {
        Phone imsPhone = this.mPhone.getImsPhone();
        if (imsPhone != null) {
            return imsPhone.getServiceState().isEmergencyOnly();
        }
        return false;
    }

    protected void resetServiceStateInIwlanMode() {
        boolean z;
        if (this.mCi.getRadioState() == CommandsInterface.RadioState.RADIO_OFF) {
            log("set service state as POWER_OFF");
            if (this.mNewSS.getIwlanRegState() == 0) {
                log("pollStateDone: restore iwlan RAT value");
                z = true;
            } else {
                z = false;
            }
            this.mNewSS.setStateOff();
            if (z) {
                this.mNewSS.setIwlanRegState(0);
                log("pollStateDone: mNewSS = " + this.mNewSS);
            }
        }
    }

    private int getRegStateFromHalRegState(int i) {
        if (i != 10) {
            switch (i) {
                case 0:
                    break;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                case 4:
                    return 4;
                case 5:
                    return 5;
                default:
                    switch (i) {
                        case 12:
                            return 2;
                        case 13:
                            return 3;
                        case 14:
                            return 4;
                        default:
                            return 0;
                    }
            }
        }
        return 0;
    }

    private boolean isEmergencyOnly(int i) {
        switch (i) {
            case 10:
            case 12:
            case 13:
            case 14:
                return true;
            case 11:
            default:
                return false;
        }
    }

    private int[] getAvailableServices(int i, int i2, boolean z) {
        if (z) {
            return new int[]{5};
        }
        if (i == 5 || i == 1) {
            if (i2 == 2) {
                return new int[]{2};
            }
            if (i2 == 1) {
                return new int[]{1, 3, 4};
            }
        }
        return null;
    }

    private CellIdentity convertHalCellIdentityToCellIdentity(android.hardware.radio.V1_2.CellIdentity cellIdentity) {
        CellIdentity cellIdentityGsm;
        if (cellIdentity == null) {
            return null;
        }
        switch (cellIdentity.cellInfoType) {
            case 1:
                if (cellIdentity.cellIdentityGsm.size() != 1) {
                    return null;
                }
                CellIdentityGsm cellIdentityGsm2 = cellIdentity.cellIdentityGsm.get(0);
                cellIdentityGsm = new android.telephony.CellIdentityGsm(cellIdentityGsm2.base.lac, cellIdentityGsm2.base.cid, cellIdentityGsm2.base.arfcn, cellIdentityGsm2.base.bsic, cellIdentityGsm2.base.mcc, cellIdentityGsm2.base.mnc, cellIdentityGsm2.operatorNames.alphaLong, cellIdentityGsm2.operatorNames.alphaShort);
                break;
                break;
            case 2:
                if (cellIdentity.cellIdentityCdma.size() != 1) {
                    return null;
                }
                android.hardware.radio.V1_2.CellIdentityCdma cellIdentityCdma = cellIdentity.cellIdentityCdma.get(0);
                return new CellIdentityCdma(cellIdentityCdma.base.networkId, cellIdentityCdma.base.systemId, cellIdentityCdma.base.baseStationId, cellIdentityCdma.base.longitude, cellIdentityCdma.base.latitude, cellIdentityCdma.operatorNames.alphaLong, cellIdentityCdma.operatorNames.alphaShort);
            case 3:
                if (cellIdentity.cellIdentityLte.size() != 1) {
                    return null;
                }
                CellIdentityLte cellIdentityLte = cellIdentity.cellIdentityLte.get(0);
                return new android.telephony.CellIdentityLte(cellIdentityLte.base.ci, cellIdentityLte.base.pci, cellIdentityLte.base.tac, cellIdentityLte.base.earfcn, cellIdentityLte.bandwidth, cellIdentityLte.base.mcc, cellIdentityLte.base.mnc, cellIdentityLte.operatorNames.alphaLong, cellIdentityLte.operatorNames.alphaShort);
            case 4:
                if (cellIdentity.cellIdentityWcdma.size() != 1) {
                    return null;
                }
                CellIdentityWcdma cellIdentityWcdma = cellIdentity.cellIdentityWcdma.get(0);
                cellIdentityGsm = new android.telephony.CellIdentityWcdma(cellIdentityWcdma.base.lac, cellIdentityWcdma.base.cid, cellIdentityWcdma.base.psc, cellIdentityWcdma.base.uarfcn, cellIdentityWcdma.base.mcc, cellIdentityWcdma.base.mnc, cellIdentityWcdma.operatorNames.alphaLong, cellIdentityWcdma.operatorNames.alphaShort);
                break;
                break;
            case 5:
                if (cellIdentity.cellIdentityTdscdma.size() != 1) {
                    return null;
                }
                CellIdentityTdscdma cellIdentityTdscdma = cellIdentity.cellIdentityTdscdma.get(0);
                return new android.telephony.CellIdentityTdscdma(cellIdentityTdscdma.base.mcc, cellIdentityTdscdma.base.mnc, cellIdentityTdscdma.base.lac, cellIdentityTdscdma.base.cid, cellIdentityTdscdma.base.cpid, cellIdentityTdscdma.operatorNames.alphaLong, cellIdentityTdscdma.operatorNames.alphaShort);
            default:
                return null;
        }
        return cellIdentityGsm;
    }

    private NetworkRegistrationState createRegistrationStateFromVoiceRegState(Object obj) {
        VoiceRegStateResult voiceRegStateResult = (VoiceRegStateResult) obj;
        int regStateFromHalRegState = getRegStateFromHalRegState(voiceRegStateResult.regState);
        int iRilRadioTechnologyToNetworkType = ServiceState.rilRadioTechnologyToNetworkType(voiceRegStateResult.rat);
        int i = voiceRegStateResult.reasonForDenial;
        boolean zIsEmergencyOnly = isEmergencyOnly(voiceRegStateResult.regState);
        return new NetworkRegistrationState(1, 1, regStateFromHalRegState, iRilRadioTechnologyToNetworkType, i, zIsEmergencyOnly, getAvailableServices(regStateFromHalRegState, 1, zIsEmergencyOnly), convertHalCellIdentityToCellIdentity(voiceRegStateResult.cellIdentity), voiceRegStateResult.cssSupported, voiceRegStateResult.roamingIndicator, voiceRegStateResult.systemIsInPrl, voiceRegStateResult.defaultRoamingIndicator);
    }

    private NetworkRegistrationState createRegistrationStateFromDataRegState(Object obj) {
        DataRegStateResult dataRegStateResult = (DataRegStateResult) obj;
        int regStateFromHalRegState = getRegStateFromHalRegState(dataRegStateResult.regState);
        int iRilRadioTechnologyToNetworkType = ServiceState.rilRadioTechnologyToNetworkType(dataRegStateResult.rat);
        int i = dataRegStateResult.reasonDataDenied;
        boolean zIsEmergencyOnly = isEmergencyOnly(dataRegStateResult.regState);
        return new NetworkRegistrationState(1, 2, regStateFromHalRegState, iRilRadioTechnologyToNetworkType, i, zIsEmergencyOnly, getAvailableServices(regStateFromHalRegState, 2, zIsEmergencyOnly), convertHalCellIdentityToCellIdentity(dataRegStateResult.cellIdentity), dataRegStateResult.maxDataCalls);
    }

    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        MtkCellInfoResult mtkCellInfoResult = new MtkCellInfoResult();
        log("SST.getAllCellInfo(): E");
        if (this.mCi.getRilVersion() >= 8) {
            if (!isCallerOnDifferentThread()) {
                log("SST.getAllCellInfo(): return last, same thread can't block");
                mtkCellInfoResult.list = this.mLastCellInfoList;
            } else if (SystemClock.elapsedRealtime() - this.mLastCellInfoListTime > MTK_LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                Message messageObtainMessage = this.mtkHandler.obtainMessage(1, mtkCellInfoResult);
                synchronized (mtkCellInfoResult.lockObj) {
                    mtkCellInfoResult.list = null;
                    this.mCi.getCellInfoList(messageObtainMessage, workSource);
                    try {
                        mtkCellInfoResult.lockObj.wait(5000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                log("SST.getAllCellInfo(): return last, back to back calls");
                mtkCellInfoResult.list = this.mLastCellInfoList;
            }
        } else {
            log("SST.getAllCellInfo(): not implemented");
            mtkCellInfoResult.list = null;
        }
        synchronized (mtkCellInfoResult.lockObj) {
            if (mtkCellInfoResult.list != null) {
                log("SST.getAllCellInfo(): X size=" + mtkCellInfoResult.list.size() + " list=" + mtkCellInfoResult.list);
                return mtkCellInfoResult.list;
            }
            log("SST.getAllCellInfo(): X size=0 list=null");
            return null;
        }
    }
}
