package com.android.internal.telephony;

import android.R;
import android.app.PendingIntent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.telephony.CellInfo;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TimeUtils;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ServiceStateTracker extends Handler {
    protected static final String ACTION_RADIO_OFF = "android.intent.action.ACTION_RADIO_OFF";
    protected static final boolean DBG = true;
    public static final int DEFAULT_GPRS_CHECK_PERIOD_MILLIS = 60000;
    protected static final int DELAY_CLEANUP_DATACALL = 3000;
    protected static final int EVENT_CDMA_PRL_VERSION_CHANGED = 40;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 39;
    protected static final int EVENT_CHANGE_IMS_STATE = 45;
    protected static final int EVENT_CHECK_REPORT_GPRS = 22;
    protected static final int EVENT_DELAY_CLEANUP_DATACALL = 80;
    protected static final int EVENT_ERI_FILE_LOADED = 36;
    protected static final int EVENT_GET_CELL_INFO_LIST = 43;
    protected static final int EVENT_GET_LOC_DONE = 15;
    protected static final int EVENT_GET_LOC_DONE_CDMA = 31;
    protected static final int EVENT_GET_PREFERRED_NETWORK_TYPE = 19;
    protected static final int EVENT_GET_SIGNAL_STRENGTH = 3;
    protected static final int EVENT_GET_SIGNAL_STRENGTH_CDMA = 29;
    public static final int EVENT_ICC_CHANGED = 42;
    protected static final int EVENT_IMS_STATE_CHANGED = 46;
    protected static final int EVENT_IMS_STATE_DONE = 47;
    protected static final int EVENT_LOCATION_UPDATES_ENABLED = 18;
    protected static final int EVENT_NETWORK_STATE_CHANGED = 2;
    protected static final int EVENT_NETWORK_STATE_CHANGED_CDMA = 30;
    protected static final int EVENT_NITZ_TIME = 11;
    protected static final int EVENT_NV_LOADED = 33;
    protected static final int EVENT_NV_READY = 35;
    protected static final int EVENT_OEM = 80;
    protected static final int EVENT_OTA_PROVISION_STATUS_CHANGE = 37;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH = 10;
    protected static final int EVENT_POLL_SIGNAL_STRENGTH_CDMA = 28;
    protected static final int EVENT_POLL_STATE_CDMA_SUBSCRIPTION = 34;
    protected static final int EVENT_POLL_STATE_GPRS = 5;
    protected static final int EVENT_POLL_STATE_NETWORK_SELECTION_MODE = 14;
    protected static final int EVENT_POLL_STATE_OPERATOR = 6;
    protected static final int EVENT_POLL_STATE_OPERATOR_CDMA = 25;
    protected static final int EVENT_POLL_STATE_REGISTRATION = 4;
    protected static final int EVENT_POLL_STATE_REGISTRATION_CDMA = 24;
    protected static final int EVENT_RADIO_AVAILABLE = 13;
    protected static final int EVENT_RADIO_ON = 41;
    protected static final int EVENT_RADIO_STATE_CHANGED = 1;
    protected static final int EVENT_RESET_PREFERRED_NETWORK_TYPE = 21;
    protected static final int EVENT_RESTRICTED_STATE_CHANGED = 23;
    protected static final int EVENT_RUIM_READY = 26;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 27;
    protected static final int EVENT_SET_PREFERRED_NETWORK_TYPE = 20;
    protected static final int EVENT_SET_RADIO_POWER_OFF = 38;
    protected static final int EVENT_SIGNAL_STRENGTH_UPDATE = 12;
    protected static final int EVENT_SIM_READY = 17;
    protected static final int EVENT_SIM_RECORDS_LOADED = 16;
    protected static final int EVENT_UNSOL_CELL_INFO_LIST = 44;
    protected static final String[] GMT_COUNTRY_CODES = {"bf", "ci", "eh", "fo", "gb", "gh", "gm", "gn", "gw", "ie", "lr", "is", "ma", "ml", "mr", "pt", "sl", "sn", Telephony.BaseMmsColumns.STATUS, "tg"};
    private static final long LAST_CELL_INFO_LIST_MAX_AGE_MS = 2000;
    private static final String LOG_TAG = "SST";
    public static final int OTASP_NEEDED = 2;
    public static final int OTASP_NOT_NEEDED = 3;
    public static final int OTASP_UNINITIALIZED = 0;
    public static final int OTASP_UNKNOWN = 1;
    protected static final int POLL_PERIOD_MILLIS = 20000;
    protected static final String PROP_FORCE_ROAMING = "telephony.test.forceRoaming";
    protected static final String REGISTRATION_DENIED_AUTH = "Authentication Failure";
    protected static final String REGISTRATION_DENIED_GEN = "General";
    protected static final String TIMEZONE_PROPERTY = "persist.sys.timezone";
    protected static final boolean VDBG = false;
    protected final CellInfo mCellInfo;
    protected CommandsInterface mCi;
    protected boolean mDesiredPowerState;
    protected long mLastCellInfoListTime;
    protected PhoneBase mPhoneBase;
    protected int[] mPollingContext;
    protected SubscriptionController mSubscriptionController;
    protected SubscriptionManager mSubscriptionManager;
    protected UiccController mUiccController;
    protected boolean mVoiceCapable;
    private boolean mWantContinuousLocationUpdates;
    private boolean mWantSingleLocationUpdate;
    protected UiccCardApplication mUiccApplcation = null;
    protected IccRecords mIccRecords = null;
    public ServiceState mSS = new ServiceState();
    protected ServiceState mNewSS = new ServiceState();
    protected List<CellInfo> mLastCellInfoList = null;
    protected SignalStrength mSignalStrength = new SignalStrength();
    public RestrictedState mRestrictedState = new RestrictedState();
    protected boolean mDontPollSignalStrength = false;
    protected RegistrantList mVoiceRoamingOnRegistrants = new RegistrantList();
    protected RegistrantList mVoiceRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mDataRoamingOnRegistrants = new RegistrantList();
    protected RegistrantList mDataRoamingOffRegistrants = new RegistrantList();
    protected RegistrantList mAttachedRegistrants = new RegistrantList();
    protected RegistrantList mDetachedRegistrants = new RegistrantList();
    protected RegistrantList mDataRegStateOrRatChangedRegistrants = new RegistrantList();
    protected RegistrantList mNetworkAttachedRegistrants = new RegistrantList();
    protected RegistrantList mPsRestrictEnabledRegistrants = new RegistrantList();
    protected RegistrantList mPsRestrictDisabledRegistrants = new RegistrantList();
    protected boolean mPendingRadioPowerOffAfterDataOff = false;
    protected int mPendingRadioPowerOffAfterDataOffTag = 0;
    protected boolean mPendingCleanupDataCall = false;
    protected boolean mImsRegistrationOnOff = false;
    protected boolean mAlarmSwitch = false;
    protected IntentFilter mIntentFilter = null;
    protected PendingIntent mRadioOffIntent = null;
    protected boolean mPowerOffDelayNeed = true;
    protected boolean mDeviceShuttingDown = false;
    protected boolean mSpnUpdatePending = false;
    protected String mCurSpn = null;
    protected String mCurPlmn = null;
    protected boolean mCurShowPlmn = false;
    protected boolean mCurShowSpn = false;
    private boolean mImsRegistered = false;
    protected final SstSubscriptionsChangedListener mOnSubscriptionsChangedListener = new SstSubscriptionsChangedListener();
    private SignalStrength mLastSignalStrength = null;

    public abstract int getCurrentDataConnectionState();

    protected abstract Phone getPhone();

    protected abstract void handlePollStateResult(int i, AsyncResult asyncResult);

    protected abstract void hangupAndPowerOff();

    public abstract boolean isConcurrentVoiceAndDataAllowed();

    protected abstract void log(String str);

    protected abstract void loge(String str);

    protected abstract void onUpdateIccAvailability();

    public abstract void pollState();

    public abstract void setImsRegistrationState(boolean z);

    protected abstract void setPowerStateToDesired();

    protected abstract void setRoamingType(ServiceState serviceState);

    protected abstract void updateSpnDisplay();

    private class CellInfoResult {
        List<CellInfo> list;
        Object lockObj;

        private CellInfoResult() {
            this.lockObj = new Object();
        }
    }

    protected class SstSubscriptionsChangedListener extends SubscriptionManager.OnSubscriptionsChangedListener {
        public final AtomicInteger mPreviousSubId = new AtomicInteger(-1);

        protected SstSubscriptionsChangedListener() {
        }

        @Override
        public void onSubscriptionsChanged() {
            ServiceStateTracker.this.log("SubscriptionListener.onSubscriptionInfoChanged");
            int subId = ServiceStateTracker.this.mPhoneBase.getSubId();
            if (this.mPreviousSubId.getAndSet(subId) != subId && SubscriptionManager.isValidSubscriptionId(subId)) {
                Context context = ServiceStateTracker.this.mPhoneBase.getContext();
                int networkType = PhoneFactory.calculatePreferredNetworkType(context, subId);
                ServiceStateTracker.this.mCi.setPreferredNetworkType(networkType, null);
                ServiceStateTracker.this.mPhoneBase.notifyCallForwardingIndicator();
                boolean skipRestoringSelection = context.getResources().getBoolean(R.^attr-private.floatingToolbarItemBackgroundBorderlessDrawable);
                if (!skipRestoringSelection) {
                    ServiceStateTracker.this.mPhoneBase.restoreSavedNetworkSelection(null);
                }
                ServiceStateTracker.this.mPhoneBase.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(ServiceStateTracker.this.mSS.getRilDataRadioTechnology()));
                if (ServiceStateTracker.this.mSpnUpdatePending) {
                    ServiceStateTracker.this.mSubscriptionController.setPlmnSpn(ServiceStateTracker.this.mPhoneBase.getPhoneId(), ServiceStateTracker.this.mCurShowPlmn, ServiceStateTracker.this.mCurPlmn, ServiceStateTracker.this.mCurShowSpn, ServiceStateTracker.this.mCurSpn);
                    ServiceStateTracker.this.mSpnUpdatePending = false;
                }
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                String oldNetworkSelectionName = sp.getString(PhoneBase.NETWORK_SELECTION_NAME_KEY, "");
                String oldNetworkSelection = sp.getString(PhoneBase.NETWORK_SELECTION_KEY, "");
                if (!TextUtils.isEmpty(oldNetworkSelectionName) || !TextUtils.isEmpty(oldNetworkSelection)) {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString(PhoneBase.NETWORK_SELECTION_NAME_KEY + subId, oldNetworkSelectionName);
                    editor.putString(PhoneBase.NETWORK_SELECTION_KEY + subId, oldNetworkSelection);
                    editor.remove(PhoneBase.NETWORK_SELECTION_NAME_KEY);
                    editor.remove(PhoneBase.NETWORK_SELECTION_KEY);
                    editor.commit();
                }
            }
        }
    }

    protected ServiceStateTracker(PhoneBase phoneBase, CommandsInterface ci, CellInfo cellInfo) {
        this.mUiccController = null;
        this.mPhoneBase = phoneBase;
        this.mCellInfo = cellInfo;
        this.mCi = ci;
        this.mVoiceCapable = this.mPhoneBase.getContext().getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 42, null);
        this.mCi.setOnSignalStrengthUpdate(this, 12, null);
        this.mCi.registerForCellInfoList(this, 44, null);
        this.mSubscriptionController = SubscriptionController.getInstance();
        this.mSubscriptionManager = SubscriptionManager.from(phoneBase.getContext());
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        this.mPhoneBase.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(0));
        this.mCi.registerForImsNetworkStateChanged(this, EVENT_IMS_STATE_CHANGED, null);
    }

    void requestShutdown() {
        if (!this.mDeviceShuttingDown) {
            this.mDeviceShuttingDown = true;
            this.mDesiredPowerState = false;
            setPowerStateToDesired();
        }
    }

    public void dispose() {
        this.mCi.unSetOnSignalStrengthUpdate(this);
        this.mUiccController.unregisterForIccChanged(this);
        this.mCi.unregisterForCellInfoList(this);
        this.mSubscriptionManager.removeOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
    }

    public boolean getDesiredPowerState() {
        return this.mDesiredPowerState;
    }

    protected boolean notifySignalStrength() {
        boolean notified = false;
        synchronized (this.mCellInfo) {
            if (!this.mSignalStrength.equals(this.mLastSignalStrength)) {
                try {
                    this.mPhoneBase.notifySignalStrength();
                    notified = true;
                } catch (NullPointerException ex) {
                    loge("updateSignalStrength() Phone already destroyed: " + ex + "SignalStrength not notified");
                }
            }
        }
        return notified;
    }

    protected void notifyDataRegStateRilRadioTechnologyChanged() {
        int rat = this.mSS.getRilDataRadioTechnology();
        int drs = this.mSS.getDataRegState();
        log("notifyDataRegStateRilRadioTechnologyChanged: drs=" + drs + " rat=" + rat);
        this.mPhoneBase.setSystemProperty("gsm.network.type", ServiceState.rilRadioTechnologyToString(rat));
        this.mDataRegStateOrRatChangedRegistrants.notifyResult(new Pair(Integer.valueOf(drs), Integer.valueOf(rat)));
    }

    protected void useDataRegStateForDataOnlyDevices() {
        if (!this.mVoiceCapable) {
            log("useDataRegStateForDataOnlyDevice: VoiceRegState=" + this.mNewSS.getVoiceRegState() + " DataRegState=" + this.mNewSS.getDataRegState());
            this.mNewSS.setVoiceRegState(this.mNewSS.getDataRegState());
        }
    }

    protected void updatePhoneObject() {
        if (this.mPhoneBase.getContext().getResources().getBoolean(R.^attr-private.magnifierHeight)) {
            boolean isRegistered = this.mSS.getVoiceRegState() == 0 || this.mSS.getVoiceRegState() == 2;
            if (!isRegistered) {
                Rlog.d(LOG_TAG, "updatePhoneObject: Ignore update");
            } else {
                this.mPhoneBase.updatePhoneObject(this.mSS.getRilVoiceRadioTechnology());
            }
        }
    }

    public void registerForVoiceRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOnRegistrants.add(r);
        if (this.mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOn(Handler h) {
        this.mVoiceRoamingOnRegistrants.remove(h);
    }

    public void registerForVoiceRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mVoiceRoamingOffRegistrants.add(r);
        if (!this.mSS.getVoiceRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForVoiceRoamingOff(Handler h) {
        this.mVoiceRoamingOffRegistrants.remove(h);
    }

    public void registerForDataRoamingOn(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOnRegistrants.add(r);
        if (this.mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataRoamingOn(Handler h) {
        this.mDataRoamingOnRegistrants.remove(h);
    }

    public void registerForDataRoamingOff(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDataRoamingOffRegistrants.add(r);
        if (!this.mSS.getDataRoaming()) {
            r.notifyRegistrant();
        }
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
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mWantSingleLocationUpdate = true;
            this.mCi.setLocationUpdates(true, obtainMessage(18));
        }
    }

    public void enableLocationUpdates() {
        if (!this.mWantSingleLocationUpdate && !this.mWantContinuousLocationUpdates) {
            this.mWantContinuousLocationUpdates = true;
            this.mCi.setLocationUpdates(true, obtainMessage(18));
        }
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

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 38:
                synchronized (this) {
                    if (this.mPendingRadioPowerOffAfterDataOff && msg.arg1 == this.mPendingRadioPowerOffAfterDataOffTag) {
                        log("EVENT_SET_RADIO_OFF, turn radio off now.");
                        hangupAndPowerOff();
                        this.mPendingRadioPowerOffAfterDataOffTag++;
                        this.mPendingRadioPowerOffAfterDataOff = false;
                    } else {
                        log("EVENT_SET_RADIO_OFF is stale arg1=" + msg.arg1 + "!= tag=" + this.mPendingRadioPowerOffAfterDataOffTag);
                    }
                    break;
                }
                return;
            case 42:
                onUpdateIccAvailability();
                return;
            case EVENT_GET_CELL_INFO_LIST:
                AsyncResult ar = (AsyncResult) msg.obj;
                CellInfoResult result = (CellInfoResult) ar.userObj;
                synchronized (result.lockObj) {
                    if (ar.exception != null) {
                        log("EVENT_GET_CELL_INFO_LIST: error ret null, e=" + ar.exception);
                        result.list = null;
                    } else {
                        result.list = (List) ar.result;
                    }
                    this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                    this.mLastCellInfoList = result.list;
                    result.lockObj.notify();
                    break;
                }
                return;
            case 44:
                AsyncResult ar2 = (AsyncResult) msg.obj;
                if (ar2.exception != null) {
                    log("EVENT_UNSOL_CELL_INFO_LIST: error ignoring, e=" + ar2.exception);
                    return;
                }
                List<CellInfo> list = (List) ar2.result;
                log("EVENT_UNSOL_CELL_INFO_LIST: size=" + list.size() + " list=" + list);
                this.mLastCellInfoListTime = SystemClock.elapsedRealtime();
                this.mLastCellInfoList = list;
                this.mPhoneBase.notifyCellInfo(list);
                return;
            case EVENT_IMS_STATE_CHANGED:
                this.mCi.getImsRegistrationState(obtainMessage(47));
                return;
            case 47:
                AsyncResult ar3 = (AsyncResult) msg.obj;
                if (ar3.exception == null) {
                    int[] responseArray = (int[]) ar3.result;
                    this.mImsRegistered = responseArray[0] == 1;
                    return;
                }
                return;
            case RadioNVItems.RIL_NV_LTE_NEXT_SCAN:
                boolean ret = processPendingCleanUpDataCall();
                log("EVENT_DELAY_CLEANUP_DATACALL ret= " + ret);
                return;
            default:
                log("Unhandled message with number: " + msg.what);
                return;
        }
    }

    public void registerForDataConnectionAttached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mAttachedRegistrants.add(r);
        if (getCurrentDataConnectionState() == 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForDataConnectionAttached(Handler h) {
        this.mAttachedRegistrants.remove(h);
    }

    public void registerForDataConnectionDetached(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mDetachedRegistrants.add(r);
        if (getCurrentDataConnectionState() != 0) {
            r.notifyRegistrant();
        }
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
        if (this.mSS.getVoiceRegState() == 0) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForNetworkAttached(Handler h) {
        this.mNetworkAttachedRegistrants.remove(h);
    }

    public void registerForPsRestrictedEnabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictEnabledRegistrants.add(r);
        if (this.mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedEnabled(Handler h) {
        this.mPsRestrictEnabledRegistrants.remove(h);
    }

    public void registerForPsRestrictedDisabled(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mPsRestrictDisabledRegistrants.add(r);
        if (this.mRestrictedState.isPsRestricted()) {
            r.notifyRegistrant();
        }
    }

    public void unregisterForPsRestrictedDisabled(Handler h) {
        this.mPsRestrictDisabledRegistrants.remove(h);
    }

    public void powerOffRadioSafely(DcTrackerBase dcTracker) {
        synchronized (this) {
            if (!this.mPendingRadioPowerOffAfterDataOff) {
                String[] networkNotClearData = this.mPhoneBase.getContext().getResources().getStringArray(R.array.config_defaultFirstUserRestrictions);
                String currentNetwork = this.mSS.getOperatorNumeric();
                if (networkNotClearData != null && currentNetwork != null) {
                    for (String str : networkNotClearData) {
                        if (currentNetwork.equals(str)) {
                            log("Not disconnecting data for " + currentNetwork);
                            hangupAndPowerOff();
                            return;
                        }
                    }
                }
                if (dcTracker.isDisconnected()) {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    log("Data disconnected, turn off radio right away.");
                    hangupAndPowerOff();
                } else {
                    dcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                    Message msg = Message.obtain(this);
                    msg.what = 38;
                    int i = this.mPendingRadioPowerOffAfterDataOffTag + 1;
                    this.mPendingRadioPowerOffAfterDataOffTag = i;
                    msg.arg1 = i;
                    if (sendMessageDelayed(msg, 30000L)) {
                        log("Wait upto 30s for data to disconnect, then turn off radio.");
                        this.mPendingRadioPowerOffAfterDataOff = true;
                    } else {
                        log("Cannot send delayed Msg, turn off radio right away.");
                        hangupAndPowerOff();
                    }
                }
            }
        }
    }

    public boolean processPendingCleanUpDataCall() {
        boolean z = false;
        synchronized (this) {
            if (this.mPendingCleanupDataCall) {
                log("Process pending request to clean up data call.");
                removeMessages(80);
                this.mPhoneBase.mDcTracker.cleanUpAllConnections(Phone.REASON_RADIO_TURNED_OFF);
                this.mPendingCleanupDataCall = false;
                z = true;
            }
        }
        return z;
    }

    public boolean processPendingRadioPowerOffAfterDataOff() {
        boolean z = false;
        synchronized (this) {
            if (this.mPendingRadioPowerOffAfterDataOff) {
                log("Process pending request to turn radio off.");
                this.mPendingRadioPowerOffAfterDataOffTag++;
                hangupAndPowerOff();
                this.mPendingRadioPowerOffAfterDataOff = false;
                z = true;
            }
        }
        return z;
    }

    protected boolean onSignalStrengthResult(AsyncResult ar, boolean isGsm) {
        SignalStrength signalStrength = this.mSignalStrength;
        if (ar.exception == null && ar.result != null) {
            this.mSignalStrength = (SignalStrength) ar.result;
            this.mSignalStrength.validateInput();
            this.mSignalStrength.setGsm(isGsm);
        } else {
            log("onSignalStrengthResult() Exception from RIL : " + ar.exception);
            this.mSignalStrength = new SignalStrength(isGsm);
        }
        return notifySignalStrength();
    }

    protected void cancelPollState() {
        this.mPollingContext = new int[1];
    }

    protected boolean shouldFixTimeZoneNow(PhoneBase phoneBase, String operatorNumeric, String prevOperatorNumeric, boolean needToFixTimeZone) {
        int prevMcc;
        try {
            int mcc = Integer.parseInt(operatorNumeric.substring(0, 3));
            try {
                prevMcc = Integer.parseInt(prevOperatorNumeric.substring(0, 3));
            } catch (Exception e) {
                prevMcc = mcc + 1;
            }
            boolean iccCardExist = false;
            if (this.mUiccApplcation != null) {
                iccCardExist = this.mUiccApplcation.getState() != IccCardApplicationStatus.AppState.APPSTATE_UNKNOWN;
            }
            boolean retVal = (iccCardExist && mcc != prevMcc) || needToFixTimeZone;
            long ctm = System.currentTimeMillis();
            log("shouldFixTimeZoneNow: retVal=" + retVal + " iccCardExist=" + iccCardExist + " operatorNumeric=" + operatorNumeric + " mcc=" + mcc + " prevOperatorNumeric=" + prevOperatorNumeric + " prevMcc=" + prevMcc + " needToFixTimeZone=" + needToFixTimeZone + " ltod=" + TimeUtils.logTimeOfDay(ctm));
            return retVal;
        } catch (Exception e2) {
            log("shouldFixTimeZoneNow: no mcc, operatorNumeric=" + operatorNumeric + " retVal=false");
            return false;
        }
    }

    public String getSystemProperty(String property, String defValue) {
        return TelephonyManager.getTelephonyProperty(this.mPhoneBase.getPhoneId(), property, defValue);
    }

    public List<CellInfo> getAllCellInfo() {
        List<CellInfo> list = null;
        CellInfoResult cellInfoResult = new CellInfoResult();
        if (this.mCi.getRilVersion() >= 8) {
            if (!isCallerOnDifferentThread()) {
                log("SST.getAllCellInfo(): return last, same thread can't block");
                cellInfoResult.list = this.mLastCellInfoList;
            } else if (SystemClock.elapsedRealtime() - this.mLastCellInfoListTime > LAST_CELL_INFO_LIST_MAX_AGE_MS) {
                Message messageObtainMessage = obtainMessage(EVENT_GET_CELL_INFO_LIST, cellInfoResult);
                synchronized (cellInfoResult.lockObj) {
                    cellInfoResult.list = null;
                    this.mCi.getCellInfoList(messageObtainMessage);
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
                log("SST.getAllCellInfo(): X size=" + cellInfoResult.list.size() + " list=" + cellInfoResult.list);
                list = cellInfoResult.list;
            } else {
                log("SST.getAllCellInfo(): X size=0 list=null");
            }
        }
        return list;
    }

    public SignalStrength getSignalStrength() {
        SignalStrength signalStrength;
        synchronized (this.mCellInfo) {
            signalStrength = this.mSignalStrength;
        }
        return signalStrength;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("ServiceStateTracker:");
        pw.println(" mSS=" + this.mSS);
        pw.println(" mNewSS=" + this.mNewSS);
        pw.println(" mCellInfo=" + this.mCellInfo);
        pw.println(" mRestrictedState=" + this.mRestrictedState);
        pw.println(" mPollingContext=" + this.mPollingContext);
        pw.println(" mDesiredPowerState=" + this.mDesiredPowerState);
        pw.println(" mDontPollSignalStrength=" + this.mDontPollSignalStrength);
        pw.println(" mPendingRadioPowerOffAfterDataOff=" + this.mPendingRadioPowerOffAfterDataOff);
        pw.println(" mPendingRadioPowerOffAfterDataOffTag=" + this.mPendingRadioPowerOffAfterDataOffTag);
        pw.println(" mPendingCleanupDataCall=" + this.mPendingCleanupDataCall);
        pw.flush();
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

    protected void updateCarrierMccMncConfiguration(String newOp, String oldOp, Context context) {
        if ((newOp == null && !TextUtils.isEmpty(oldOp)) || (newOp != null && !newOp.equals(oldOp))) {
            log("update mccmnc=" + newOp + " fromServiceState=true");
            MccTable.updateMccMncConfiguration(context, newOp, true);
        }
    }

    protected boolean inSameCountry(String operatorNumeric) {
        if (TextUtils.isEmpty(operatorNumeric) || operatorNumeric.length() < 5) {
            return false;
        }
        String homeNumeric = getHomeOperatorNumeric();
        if (TextUtils.isEmpty(homeNumeric) || homeNumeric.length() < 5) {
            return false;
        }
        String networkMCC = operatorNumeric.substring(0, 3);
        String homeMCC = homeNumeric.substring(0, 3);
        String networkCountry = MccTable.countryCodeForMcc(Integer.parseInt(networkMCC));
        String homeCountry = MccTable.countryCodeForMcc(Integer.parseInt(homeMCC));
        if (networkCountry.isEmpty() || homeCountry.isEmpty()) {
            return false;
        }
        boolean inSameCountry = homeCountry.equals(networkCountry);
        if (!inSameCountry) {
            if ("us".equals(homeCountry) && "vi".equals(networkCountry)) {
                return true;
            }
            if ("vi".equals(homeCountry) && "us".equals(networkCountry)) {
                return true;
            }
            return inSameCountry;
        }
        return inSameCountry;
    }

    protected String getHomeOperatorNumeric() {
        return ((TelephonyManager) this.mPhoneBase.getContext().getSystemService("phone")).getSimOperatorNumericForPhone(this.mPhoneBase.getPhoneId());
    }

    protected int getPhoneId() {
        return this.mPhoneBase.getPhoneId();
    }
}
