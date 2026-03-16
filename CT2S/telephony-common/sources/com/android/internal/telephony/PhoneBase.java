package com.android.internal.telephony;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.dataconnection.DcTrackerBase;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UsimServiceTable;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class PhoneBase extends Handler implements Phone {
    private static final String CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_non_roaming_list_";
    private static final String CDMA_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_roaming_list_";
    public static final String CLIR_KEY = "clir_key";
    public static final String DATA_DISABLED_ON_BOOT_KEY = "disabled_on_boot_key";
    public static final String DNS_SERVER_CHECK_DISABLED_KEY = "dns_server_check_disabled_key";
    protected static final int EVENT_CALL_RING = 14;
    protected static final int EVENT_CALL_RING_CONTINUE = 15;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 27;
    protected static final int EVENT_EMERGENCY_CALLBACK_MODE_ENTER = 25;
    protected static final int EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE = 26;
    protected static final int EVENT_GET_BASEBAND_VERSION_DONE = 6;
    protected static final int EVENT_GET_CALL_FORWARD_DONE = 13;
    protected static final int EVENT_GET_DEVICE_IDENTITY_DONE = 21;
    protected static final int EVENT_GET_IMEISV_DONE = 10;
    protected static final int EVENT_GET_IMEI_DONE = 9;
    protected static final int EVENT_GET_RADIO_CAPABILITY = 35;
    protected static final int EVENT_GET_SIM_STATUS_DONE = 11;
    protected static final int EVENT_ICC_CHANGED = 30;
    protected static final int EVENT_ICC_RECORD_EVENTS = 29;
    protected static final int EVENT_INITIATE_SILENT_REDIAL = 32;
    protected static final int EVENT_LAST = 36;
    protected static final int EVENT_MMI_DONE = 4;
    protected static final int EVENT_NV_READY = 23;
    protected static final int EVENT_RADIO_AVAILABLE = 1;
    protected static final int EVENT_RADIO_NOT_AVAILABLE = 33;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 8;
    protected static final int EVENT_RADIO_ON = 5;
    protected static final int EVENT_REGISTERED_TO_NETWORK = 19;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 22;
    protected static final int EVENT_SET_CALL_FORWARD_DONE = 12;
    protected static final int EVENT_SET_CLIR_COMPLETE = 18;
    protected static final int EVENT_SET_ENHANCED_VP = 24;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC = 28;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC_COMPLETE = 17;
    protected static final int EVENT_SET_NETWORK_MANUAL_COMPLETE = 16;
    protected static final int EVENT_SET_VM_NUMBER_DONE = 20;
    protected static final int EVENT_SIM_RECORDS_LOADED = 3;
    protected static final int EVENT_SRVCC_STATE_CHANGED = 31;
    protected static final int EVENT_SS = 36;
    protected static final int EVENT_SSN = 2;
    protected static final int EVENT_UNSOL_OEM_HOOK_RAW = 34;
    protected static final int EVENT_USSD = 7;
    private static final String GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_non_roaming_list_";
    private static final String GSM_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_roaming_list_";
    private static final String LOG_TAG = "PhoneBase";
    public static final String NETWORK_SELECTION_KEY = "network_selection_key";
    public static final String NETWORK_SELECTION_NAME_KEY = "network_selection_name_key";
    public static final String NETWORK_SELECTION_RAT_KEY = "network_selection_rat_key";
    public static final String VM_COUNT = "vm_count_key";
    public static final String VM_ID = "vm_id_key";
    private final String mActionAttached;
    private final String mActionDetached;
    int mCallRingContinueToken;
    int mCallRingDelay;
    public CommandsInterface mCi;
    protected final Context mContext;
    public DcTrackerBase mDcTracker;
    protected final RegistrantList mDisconnectRegistrants;
    boolean mDnsCheckDisabled;
    boolean mDoesRilSendMultipleCallRing;
    protected DomainSelection mDomainSelection;
    protected final RegistrantList mHandoverRegistrants;
    public AtomicReference<IccRecords> mIccRecords;
    private BroadcastReceiver mImsIntentReceiver;
    private final Object mImsLock;
    protected ImsPhone mImsPhone;
    private boolean mImsServiceReady;
    protected final RegistrantList mIncomingRingRegistrants;
    public boolean mIsTheCurrentActivePhone;
    boolean mIsVoiceCapable;
    protected Looper mLooper;
    protected final RegistrantList mMmiCompleteRegistrants;
    protected final RegistrantList mMmiRegistrants;
    private final String mName;
    protected final RegistrantList mNewRingingConnectionRegistrants;
    protected PhoneNotifier mNotifier;
    protected int mPhoneId;
    protected final RegistrantList mPreciseCallStateRegistrants;
    protected int mRadioAccessFamily;
    protected final RegistrantList mRadioOffOrNotAvailableRegistrants;
    protected final RegistrantList mServiceStateRegistrants;
    protected final RegistrantList mSimRecordsLoadedRegistrants;
    protected SimulatedRadioControl mSimulatedRadioControl;
    public SmsStorageMonitor mSmsStorageMonitor;
    public SmsUsageMonitor mSmsUsageMonitor;
    protected final RegistrantList mSuppServiceFailedRegistrants;
    private TelephonyTester mTelephonyTester;
    protected AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;
    boolean mUnitTestMode;
    protected final RegistrantList mUnknownConnectionRegistrants;
    private int mVmCount;

    @Override
    public abstract int getPhoneType();

    @Override
    public abstract PhoneConstants.State getState();

    protected abstract void onUpdateIccAvailability();

    protected static class NetworkSelectMessage {
        public Message message;
        public String operatorAlphaLong;
        public String operatorNumeric;
        public int rat;

        protected NetworkSelectMessage() {
        }
    }

    @Override
    public String getPhoneName() {
        return this.mName;
    }

    @Override
    public String getNai() {
        return null;
    }

    public String getActionDetached() {
        return this.mActionDetached;
    }

    public String getActionAttached() {
        return this.mActionAttached;
    }

    public void setSystemProperty(String property, String value) {
        if (!getUnitTestMode()) {
            SystemProperties.set(property, value);
        }
    }

    public String getSystemProperty(String property, String defValue) {
        if (getUnitTestMode()) {
            return null;
        }
        return SystemProperties.get(property, defValue);
    }

    protected PhoneBase(String name, PhoneNotifier notifier, Context context, CommandsInterface ci) {
        this(name, notifier, context, ci, false);
    }

    protected PhoneBase(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode) {
        this(name, notifier, context, ci, unitTestMode, Integer.MAX_VALUE);
    }

    protected PhoneBase(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode, int phoneId) {
        this.mImsIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(PhoneBase.LOG_TAG, "mImsIntentReceiver: action " + intent.getAction());
                if (intent.hasExtra("android:phone_id")) {
                    int extraPhoneId = intent.getIntExtra("android:phone_id", -1);
                    Rlog.d(PhoneBase.LOG_TAG, "mImsIntentReceiver: extraPhoneId = " + extraPhoneId);
                    if (extraPhoneId == -1 || extraPhoneId != PhoneBase.this.getPhoneId()) {
                        return;
                    }
                }
                if (intent.getAction().equals("com.android.ims.IMS_SERVICE_UP")) {
                    PhoneBase.this.mImsServiceReady = true;
                    PhoneBase.this.updateImsPhone();
                } else if (intent.getAction().equals("com.android.ims.IMS_SERVICE_DOWN")) {
                    PhoneBase.this.mImsServiceReady = false;
                    PhoneBase.this.updateImsPhone();
                }
            }
        };
        this.mVmCount = 0;
        this.mIsTheCurrentActivePhone = true;
        this.mIsVoiceCapable = true;
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference<>();
        this.mUiccApplication = new AtomicReference<>();
        this.mImsLock = new Object();
        this.mImsServiceReady = false;
        this.mImsPhone = null;
        this.mDomainSelection = null;
        this.mRadioAccessFamily = 1;
        this.mPreciseCallStateRegistrants = new RegistrantList();
        this.mHandoverRegistrants = new RegistrantList();
        this.mNewRingingConnectionRegistrants = new RegistrantList();
        this.mIncomingRingRegistrants = new RegistrantList();
        this.mDisconnectRegistrants = new RegistrantList();
        this.mServiceStateRegistrants = new RegistrantList();
        this.mMmiCompleteRegistrants = new RegistrantList();
        this.mMmiRegistrants = new RegistrantList();
        this.mUnknownConnectionRegistrants = new RegistrantList();
        this.mSuppServiceFailedRegistrants = new RegistrantList();
        this.mRadioOffOrNotAvailableRegistrants = new RegistrantList();
        this.mSimRecordsLoadedRegistrants = new RegistrantList();
        this.mPhoneId = phoneId;
        this.mName = name;
        this.mNotifier = notifier;
        this.mContext = context;
        this.mLooper = Looper.myLooper();
        this.mCi = ci;
        this.mActionDetached = getClass().getPackage().getName() + ".action_detached";
        this.mActionAttached = getClass().getPackage().getName() + ".action_attached";
        if (Build.IS_DEBUGGABLE) {
            this.mTelephonyTester = new TelephonyTester(this);
        }
        setUnitTestMode(unitTestMode);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.mDnsCheckDisabled = sp.getBoolean(DNS_SERVER_CHECK_DISABLED_KEY, false);
        this.mCi.setOnCallRing(this, 14, null);
        this.mIsVoiceCapable = this.mContext.getResources().getBoolean(R.^attr-private.externalRouteEnabledDrawable);
        this.mDoesRilSendMultipleCallRing = SystemProperties.getBoolean("ro.telephony.call_ring.multiple", true);
        Rlog.d(LOG_TAG, "mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        this.mCallRingDelay = SystemProperties.getInt("ro.telephony.call_ring.delay", 3000);
        Rlog.d(LOG_TAG, "mCallRingDelay=" + this.mCallRingDelay);
        if (getPhoneType() != 5) {
            setPropertiesByCarrier();
            this.mSmsStorageMonitor = new SmsStorageMonitor(this);
            this.mSmsUsageMonitor = new SmsUsageMonitor(context);
            this.mUiccController = UiccController.getInstance();
            this.mUiccController.registerForIccChanged(this, 30, null);
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.ims.IMS_SERVICE_UP");
            filter.addAction("com.android.ims.IMS_SERVICE_DOWN");
            this.mContext.registerReceiver(this.mImsIntentReceiver, filter);
            this.mCi.registerForSrvccStateChanged(this, 31, null);
            this.mCi.setOnUnsolOemHookRaw(this, 34, null);
        }
    }

    @Override
    public void dispose() {
        synchronized (PhoneProxy.lockForRadioTechnologyChange) {
            this.mContext.unregisterReceiver(this.mImsIntentReceiver);
            this.mCi.unSetOnCallRing(this);
            this.mDcTracker.cleanUpAllConnections(null);
            this.mIsTheCurrentActivePhone = false;
            this.mSmsStorageMonitor.dispose();
            this.mSmsUsageMonitor.dispose();
            this.mUiccController.unregisterForIccChanged(this);
            this.mCi.unregisterForSrvccStateChanged(this);
            this.mCi.unSetOnUnsolOemHookRaw(this);
            if (this.mTelephonyTester != null) {
                this.mTelephonyTester.dispose();
            }
            ImsPhone imsPhone = this.mImsPhone;
            if (imsPhone != null) {
                imsPhone.unregisterForSilentRedial(this);
                imsPhone.dispose();
            }
        }
    }

    @Override
    public void removeReferences() {
        this.mSmsStorageMonitor = null;
        this.mSmsUsageMonitor = null;
        this.mIccRecords.set(null);
        this.mUiccApplication.set(null);
        this.mDcTracker = null;
        this.mUiccController = null;
        ImsPhone imsPhone = this.mImsPhone;
        if (imsPhone != null) {
            imsPhone.removeReferences();
            this.mImsPhone = null;
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 16:
            case 17:
                handleSetSelectNetwork((AsyncResult) msg.obj);
                return;
            default:
                if (!this.mIsTheCurrentActivePhone) {
                    Rlog.e(LOG_TAG, "Received message " + msg + "[" + msg.what + "] while being destroyed. Ignoring.");
                    return;
                }
                switch (msg.what) {
                    case 14:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING Received state=" + getState());
                        if (((AsyncResult) msg.obj).exception == null) {
                            PhoneConstants.State state = getState();
                            if (!this.mDoesRilSendMultipleCallRing && (state == PhoneConstants.State.RINGING || state == PhoneConstants.State.IDLE)) {
                                this.mCallRingContinueToken++;
                                sendIncomingCallRingNotification(this.mCallRingContinueToken);
                                return;
                            } else {
                                notifyIncomingRing();
                                return;
                            }
                        }
                        return;
                    case 15:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING_CONTINUE Received stat=" + getState());
                        if (getState() == PhoneConstants.State.RINGING) {
                            sendIncomingCallRingNotification(msg.arg1);
                            return;
                        }
                        return;
                    case 30:
                        onUpdateIccAvailability();
                        return;
                    case 31:
                        AsyncResult ar = (AsyncResult) msg.obj;
                        if (ar.exception == null) {
                            handleSrvccStateChanged((int[]) ar.result);
                            return;
                        } else {
                            Rlog.e(LOG_TAG, "Srvcc exception: " + ar.exception);
                            return;
                        }
                    case 32:
                        Rlog.d(LOG_TAG, "Event EVENT_INITIATE_SILENT_REDIAL Received");
                        AsyncResult ar2 = (AsyncResult) msg.obj;
                        if (ar2.exception == null && ar2.result != null) {
                            String dialString = (String) ar2.result;
                            if (!TextUtils.isEmpty(dialString)) {
                                try {
                                    Connection conn = dialInternal(dialString, null, 0);
                                    this.mImsPhone.notifyHandoverStateChanged(conn);
                                    return;
                                } catch (CallStateException e) {
                                    Rlog.e(LOG_TAG, "silent redial failed: " + e);
                                    return;
                                }
                            }
                            return;
                        }
                        return;
                    case 34:
                        AsyncResult ar3 = (AsyncResult) msg.obj;
                        if (ar3.exception == null) {
                            byte[] data = (byte[]) ar3.result;
                            Rlog.d(LOG_TAG, "EVENT_UNSOL_OEM_HOOK_RAW data=" + IccUtils.bytesToHexString(data));
                            this.mNotifier.notifyOemHookRawEventForSubscriber(getSubId(), data);
                            return;
                        }
                        Rlog.e(LOG_TAG, "OEM hook raw exception: " + ar3.exception);
                        return;
                    case 35:
                        AsyncResult ar4 = (AsyncResult) msg.obj;
                        RadioCapability rc = (RadioCapability) ar4.result;
                        if (ar4.exception != null) {
                            Rlog.d(LOG_TAG, "get phone radio capability fail,no need to change mRadioAccessFamily");
                        } else {
                            this.mRadioAccessFamily = rc.getRadioAccessFamily();
                        }
                        Rlog.d(LOG_TAG, "EVENT_GET_RADIO_CAPABILITY :phone RAF : " + this.mRadioAccessFamily);
                        return;
                    default:
                        throw new RuntimeException("unexpected event not handled");
                }
        }
    }

    private void handleSrvccStateChanged(int[] ret) {
        Call.SrvccState srvccState;
        Rlog.d(LOG_TAG, "handleSrvccStateChanged");
        ArrayList<Connection> conn = null;
        ImsPhone imsPhone = this.mImsPhone;
        Call.SrvccState srvccState2 = Call.SrvccState.NONE;
        if (ret != null && ret.length != 0) {
            int state = ret[0];
            switch (state) {
                case 0:
                    srvccState = Call.SrvccState.STARTED;
                    if (imsPhone != null) {
                        conn = imsPhone.getHandoverConnection();
                        migrateFrom(imsPhone);
                    } else {
                        Rlog.d(LOG_TAG, "HANDOVER_STARTED: mImsPhone null");
                    }
                    break;
                case 1:
                    srvccState = Call.SrvccState.COMPLETED;
                    if (imsPhone != null) {
                        imsPhone.notifySrvccState(srvccState);
                    } else {
                        Rlog.d(LOG_TAG, "HANDOVER_COMPLETED: mImsPhone null");
                    }
                    break;
                case 2:
                case 3:
                    srvccState = Call.SrvccState.FAILED;
                    break;
                default:
                    return;
            }
            getCallTracker().notifySrvccState(srvccState, conn);
            VoLteServiceState lteState = new VoLteServiceState(state);
            notifyVoLteServiceStateChanged(lteState);
        }
    }

    @Override
    public Context getContext() {
        return this.mContext;
    }

    @Override
    public void disableDnsCheck(boolean b) {
        this.mDnsCheckDisabled = b;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DNS_SERVER_CHECK_DISABLED_KEY, b);
        editor.apply();
    }

    @Override
    public boolean isDnsCheckDisabled() {
        return this.mDnsCheckDisabled;
    }

    @Override
    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForPreciseCallStateChanged(Handler h) {
        this.mPreciseCallStateRegistrants.remove(h);
    }

    protected void notifyPreciseCallStateChangedP() {
        AsyncResult ar = new AsyncResult((Object) null, this, (Throwable) null);
        this.mPreciseCallStateRegistrants.notifyRegistrants(ar);
        this.mNotifier.notifyPreciseCallState(this);
    }

    @Override
    public void registerForHandoverStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mHandoverRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForHandoverStateChanged(Handler h) {
        this.mHandoverRegistrants.remove(h);
    }

    public void notifyHandoverStateChanged(Connection cn) {
        AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
        this.mHandoverRegistrants.notifyRegistrants(ar);
    }

    public void migrateFrom(PhoneBase from) {
        migrate(this.mHandoverRegistrants, from.mHandoverRegistrants);
        migrate(this.mPreciseCallStateRegistrants, from.mPreciseCallStateRegistrants);
        migrate(this.mNewRingingConnectionRegistrants, from.mNewRingingConnectionRegistrants);
        migrate(this.mIncomingRingRegistrants, from.mIncomingRingRegistrants);
        migrate(this.mDisconnectRegistrants, from.mDisconnectRegistrants);
        migrate(this.mServiceStateRegistrants, from.mServiceStateRegistrants);
        migrate(this.mMmiCompleteRegistrants, from.mMmiCompleteRegistrants);
        migrate(this.mMmiRegistrants, from.mMmiRegistrants);
        migrate(this.mUnknownConnectionRegistrants, from.mUnknownConnectionRegistrants);
        migrate(this.mSuppServiceFailedRegistrants, from.mSuppServiceFailedRegistrants);
    }

    public void migrate(RegistrantList to, RegistrantList from) {
        from.removeCleared();
        int n = from.size();
        for (int i = 0; i < n; i++) {
            to.add((Registrant) from.get(i));
        }
    }

    @Override
    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForUnknownConnection(Handler h) {
        this.mUnknownConnectionRegistrants.remove(h);
    }

    @Override
    public void registerForNewRingingConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForNewRingingConnection(Handler h) {
        this.mNewRingingConnectionRegistrants.remove(h);
    }

    @Override
    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOn(h, what, obj);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOn(h);
    }

    @Override
    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOff(h, what, obj);
    }

    @Override
    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOff(h);
    }

    @Override
    public void registerForIncomingRing(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForIncomingRing(Handler h) {
        this.mIncomingRingRegistrants.remove(h);
    }

    @Override
    public void registerForDisconnect(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mDisconnectRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForDisconnect(Handler h) {
        this.mDisconnectRegistrants.remove(h);
    }

    @Override
    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForSuppServiceFailed(Handler h) {
        this.mSuppServiceFailedRegistrants.remove(h);
    }

    @Override
    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForMmiInitiate(Handler h) {
        this.mMmiRegistrants.remove(h);
    }

    @Override
    public void registerForMmiComplete(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForMmiComplete(Handler h) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.remove(h);
    }

    @Override
    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("registerForSimRecordsLoaded");
    }

    @Override
    public void unregisterForSimRecordsLoaded(Handler h) {
        logUnexpectedCdmaMethodCall("unregisterForSimRecordsLoaded");
    }

    @Override
    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForTtyModeReceived(Handler h) {
    }

    @Override
    public void setNetworkSelectionModeAutomatic(Message response) {
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = "";
        nsm.operatorAlphaLong = "";
        nsm.rat = -1;
        Message msg = obtainMessage(17, nsm);
        this.mCi.setNetworkSelectionModeAutomatic(msg);
        updateSavedNetworkOperator(nsm);
    }

    @Override
    public void getNetworkSelectionMode(Message message) {
        this.mCi.getNetworkSelectionMode(message);
    }

    @Override
    public void selectNetworkManually(OperatorInfo network, Message response) {
        NetworkSelectMessage nsm = new NetworkSelectMessage();
        nsm.message = response;
        nsm.operatorNumeric = network.getOperatorNumeric();
        nsm.operatorAlphaLong = network.getOperatorAlphaLong();
        nsm.rat = network.getRat().ordinal();
        Message msg = obtainMessage(16, nsm);
        this.mCi.setNetworkSelectionModeManualExt(nsm.operatorNumeric, nsm.rat, msg);
        updateSavedNetworkOperator(nsm);
    }

    private void updateSavedNetworkOperator(NetworkSelectMessage nsm) {
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(NETWORK_SELECTION_KEY + subId, nsm.operatorNumeric);
            editor.putString(NETWORK_SELECTION_NAME_KEY + subId, nsm.operatorAlphaLong);
            editor.putInt(NETWORK_SELECTION_RAT_KEY + subId, nsm.rat);
            if (!editor.commit()) {
                Rlog.e(LOG_TAG, "failed to commit network selection preference");
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "Cannot update network selection preference due to invalid subId " + subId);
    }

    private void handleSetSelectNetwork(AsyncResult ar) {
        if (!(ar.userObj instanceof NetworkSelectMessage)) {
            Rlog.e(LOG_TAG, "unexpected result from user object.");
            return;
        }
        NetworkSelectMessage nsm = (NetworkSelectMessage) ar.userObj;
        if (nsm.message != null) {
            AsyncResult.forMessage(nsm.message, ar.result, ar.exception);
            nsm.message.sendToTarget();
        }
    }

    private String getSavedNetworkSelection() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getString(NETWORK_SELECTION_KEY + getSubId(), "");
    }

    private int getSavedNetworkSelectionRat() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        return sp.getInt(NETWORK_SELECTION_RAT_KEY + getSubId(), -1);
    }

    public void restoreSavedNetworkSelection(Message response) {
        String networkSelection = getSavedNetworkSelection();
        int networkSelectionRat = getSavedNetworkSelectionRat();
        if (TextUtils.isEmpty(networkSelection)) {
            this.mCi.setNetworkSelectionModeAutomatic(response);
        } else {
            this.mCi.setNetworkSelectionModeManualExt(networkSelection, networkSelectionRat, response);
        }
    }

    @Override
    public void setUnitTestMode(boolean f) {
        this.mUnitTestMode = f;
    }

    @Override
    public boolean getUnitTestMode() {
        return this.mUnitTestMode;
    }

    protected void notifyDisconnectP(Connection cn) {
        AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
        this.mDisconnectRegistrants.notifyRegistrants(ar);
    }

    @Override
    public void registerForServiceStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mServiceStateRegistrants.add(h, what, obj);
    }

    @Override
    public void unregisterForServiceStateChanged(Handler h) {
        this.mServiceStateRegistrants.remove(h);
    }

    @Override
    public void registerForRingbackTone(Handler h, int what, Object obj) {
        this.mCi.registerForRingbackTone(h, what, obj);
    }

    @Override
    public void unregisterForRingbackTone(Handler h) {
        this.mCi.unregisterForRingbackTone(h);
    }

    @Override
    public void registerForOnHoldTone(Handler h, int what, Object obj) {
    }

    @Override
    public void unregisterForOnHoldTone(Handler h) {
    }

    @Override
    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        this.mCi.registerForResendIncallMute(h, what, obj);
    }

    @Override
    public void unregisterForResendIncallMute(Handler h) {
        this.mCi.unregisterForResendIncallMute(h);
    }

    @Override
    public void setEchoSuppressionEnabled() {
    }

    public void notifyServiceStateChangedP(ServiceState ss) {
        AsyncResult ar = new AsyncResult((Object) null, ss, (Throwable) null);
        this.mServiceStateRegistrants.notifyRegistrants(ar);
        this.mNotifier.notifyServiceState(this);
    }

    @Override
    public SimulatedRadioControl getSimulatedRadioControl() {
        return this.mSimulatedRadioControl;
    }

    private void checkCorrectThread(Handler h) {
        if (h.getLooper() != this.mLooper) {
            throw new RuntimeException("com.android.internal.telephony.Phone must be used from within one thread");
        }
    }

    private void setPropertiesByCarrier() {
        String carrier = SystemProperties.get("ro.carrier");
        if (carrier != null && carrier.length() != 0 && !"unknown".equals(carrier)) {
            CharSequence[] carrierLocales = this.mContext.getResources().getTextArray(R.array.config_deviceStatesOnWhichToWakeUp);
            for (int i = 0; i < carrierLocales.length; i += 3) {
                String c = carrierLocales[i].toString();
                if (carrier.equals(c)) {
                    Locale l = Locale.forLanguageTag(carrierLocales[i + 1].toString().replace('_', '-'));
                    String country = l.getCountry();
                    MccTable.setSystemLocale(this.mContext, l.getLanguage(), country);
                    if (!country.isEmpty()) {
                        try {
                            Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_country_code");
                            return;
                        } catch (Settings.SettingNotFoundException e) {
                            WifiManager wM = (WifiManager) this.mContext.getSystemService("wifi");
                            wM.setCountryCode(country, false);
                            return;
                        }
                    }
                    return;
                }
            }
        }
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler fh;
        UiccCardApplication uiccApplication = this.mUiccApplication.get();
        if (uiccApplication == null) {
            Rlog.d(LOG_TAG, "getIccFileHandler: uiccApplication == null, return null");
            fh = null;
        } else {
            fh = uiccApplication.getIccFileHandler();
        }
        Rlog.d(LOG_TAG, "getIccFileHandler: fh=" + fh);
        return fh;
    }

    public Handler getHandler() {
        return this;
    }

    @Override
    public void updatePhoneObject(int voiceRadioTech) {
        PhoneFactory.getDefaultPhone().updatePhoneObject(voiceRadioTech);
    }

    public ServiceStateTracker getServiceStateTracker() {
        return null;
    }

    public CallTracker getCallTracker() {
        return null;
    }

    @Override
    public IccCardApplicationStatus.AppType getCurrentUiccAppType() {
        UiccCardApplication currentApp = this.mUiccApplication.get();
        return currentApp != null ? currentApp.getType() : IccCardApplicationStatus.AppType.APPTYPE_UNKNOWN;
    }

    @Override
    public IccCard getIccCard() {
        return null;
    }

    @Override
    public String getIccSerialNumber() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getIccId();
        }
        return null;
    }

    @Override
    public boolean getIccRecordsLoaded() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getRecordsLoaded();
        }
        return false;
    }

    @Override
    public List<CellInfo> getAllCellInfo() {
        List<CellInfo> cellInfoList = getServiceStateTracker().getAllCellInfo();
        return privatizeCellInfoList(cellInfoList);
    }

    private List<CellInfo> privatizeCellInfoList(List<CellInfo> cellInfoList) {
        int mode = Settings.Secure.getInt(getContext().getContentResolver(), "location_mode", 0);
        if (mode == 0) {
            ArrayList<CellInfo> privateCellInfoList = new ArrayList<>(cellInfoList.size());
            for (CellInfo c : cellInfoList) {
                if (c instanceof CellInfoCdma) {
                    CellInfoCdma cellInfoCdma = (CellInfoCdma) c;
                    CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
                    CellIdentityCdma maskedCellIdentity = new CellIdentityCdma(cellIdentity.getNetworkId(), cellIdentity.getSystemId(), cellIdentity.getBasestationId(), Integer.MAX_VALUE, Integer.MAX_VALUE);
                    CellInfoCdma privateCellInfoCdma = new CellInfoCdma(cellInfoCdma);
                    privateCellInfoCdma.setCellIdentity(maskedCellIdentity);
                    privateCellInfoList.add(privateCellInfoCdma);
                } else {
                    privateCellInfoList.add(c);
                }
            }
            return privateCellInfoList;
        }
        return cellInfoList;
    }

    @Override
    public void setCellInfoListRate(int rateInMillis) {
        this.mCi.setCellInfoListRate(rateInMillis, null);
    }

    @Override
    public boolean getMessageWaitingIndicator() {
        return this.mVmCount != 0;
    }

    @Override
    public boolean getCallForwardingIndicator() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getVoiceCallForwardingFlag();
        }
        return false;
    }

    @Override
    public void queryCdmaRoamingPreference(Message response) {
        this.mCi.queryCdmaRoamingPreference(response);
    }

    @Override
    public SignalStrength getSignalStrength() {
        ServiceStateTracker sst = getServiceStateTracker();
        return sst == null ? new SignalStrength() : sst.getSignalStrength();
    }

    @Override
    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        this.mCi.setCdmaRoamingPreference(cdmaRoamingType, response);
    }

    @Override
    public void setCdmaSubscription(int cdmaSubscriptionType, Message response) {
        this.mCi.setCdmaSubscriptionSource(cdmaSubscriptionType, response);
    }

    @Override
    public void setPreferredNetworkType(int networkType, Message response) {
        this.mCi.setPreferredNetworkType(networkType, response);
    }

    @Override
    public void getPreferredNetworkType(Message response) {
        this.mCi.getPreferredNetworkType(response);
    }

    @Override
    public void getSmscAddress(Message result) {
        this.mCi.getSmscAddress(result);
    }

    @Override
    public void setSmscAddress(String address, Message result) {
        this.mCi.setSmscAddress(address, result);
    }

    @Override
    public void setTTYMode(int ttyMode, Message onComplete) {
        this.mCi.setTTYMode(ttyMode, onComplete);
    }

    @Override
    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        Rlog.d(LOG_TAG, "unexpected setUiTTYMode method call");
    }

    @Override
    public void queryTTYMode(Message onComplete) {
        this.mCi.queryTTYMode(onComplete);
    }

    @Override
    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
        logUnexpectedCdmaMethodCall("enableEnhancedVoicePrivacy");
    }

    @Override
    public void getEnhancedVoicePrivacy(Message onComplete) {
        logUnexpectedCdmaMethodCall("getEnhancedVoicePrivacy");
    }

    @Override
    public void setBandMode(int bandMode, Message response) {
        this.mCi.setBandMode(bandMode, response);
    }

    @Override
    public void queryAvailableBandMode(Message response) {
        this.mCi.queryAvailableBandMode(response);
    }

    @Override
    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        this.mCi.invokeOemRilRequestRaw(data, response);
    }

    @Override
    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        this.mCi.invokeOemRilRequestStrings(strings, response);
    }

    @Override
    public void nvReadItem(int itemID, Message response) {
        this.mCi.nvReadItem(itemID, response);
    }

    @Override
    public void nvWriteItem(int itemID, String itemValue, Message response) {
        this.mCi.nvWriteItem(itemID, itemValue, response);
    }

    @Override
    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        this.mCi.nvWriteCdmaPrl(preferredRoamingList, response);
    }

    @Override
    public void nvResetConfig(int resetType, Message response) {
        this.mCi.nvResetConfig(resetType, response);
    }

    @Override
    public void notifyDataActivity() {
        this.mNotifier.notifyDataActivity(this);
    }

    public void notifyMessageWaitingIndicator() {
        if (this.mIsVoiceCapable) {
            this.mNotifier.notifyMessageWaitingChanged(this);
        }
    }

    public void notifyDataConnection(String reason, String apnType, PhoneConstants.DataState state) {
        this.mNotifier.notifyDataConnection(this, reason, apnType, state);
    }

    public void notifyDataConnection(String reason, String apnType) {
        this.mNotifier.notifyDataConnection(this, reason, apnType, getDataConnectionState(apnType));
    }

    public void notifyDataConnection(String reason) {
        String[] types = getActiveApnTypes();
        for (String apnType : types) {
            this.mNotifier.notifyDataConnection(this, reason, apnType, getDataConnectionState(apnType));
        }
    }

    public void notifyOtaspChanged(int otaspMode) {
        this.mNotifier.notifyOtaspChanged(this, otaspMode);
    }

    public void notifySignalStrength() {
        this.mNotifier.notifySignalStrength(this);
    }

    public void notifyCellInfo(List<CellInfo> cellInfo) {
        this.mNotifier.notifyCellInfo(this, privatizeCellInfoList(cellInfo));
    }

    public void notifyDataConnectionRealTimeInfo(DataConnectionRealTimeInfo dcRtInfo) {
        this.mNotifier.notifyDataConnectionRealTimeInfo(this, dcRtInfo);
    }

    public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) {
        this.mNotifier.notifyVoLteServiceStateChanged(this, lteState);
    }

    public boolean isInEmergencyCall() {
        return false;
    }

    public boolean isInEcm() {
        return false;
    }

    @Override
    public int getVoiceMessageCount() {
        return this.mVmCount;
    }

    public void setVoiceMessageCount(int countWaiting) {
        this.mVmCount = countWaiting;
        notifyMessageWaitingIndicator();
    }

    protected int getStoredVoiceMessageCount() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        String subscriberId = sp.getString(VM_ID, null);
        String currentSubscriberId = getSubscriberId();
        Rlog.d(LOG_TAG, "Voicemail count retrieval for subscriberId = " + subscriberId + " current subscriberId = " + currentSubscriberId);
        if (subscriberId == null || currentSubscriberId == null || !currentSubscriberId.equals(subscriberId)) {
            return 0;
        }
        int countVoiceMessages = sp.getInt(VM_COUNT, 0);
        Rlog.d(LOG_TAG, "Voice Mail Count from preference = " + countVoiceMessages);
        return countVoiceMessages;
    }

    @Override
    public int getCdmaEriIconIndex() {
        logUnexpectedCdmaMethodCall("getCdmaEriIconIndex");
        return -1;
    }

    @Override
    public int getCdmaEriIconMode() {
        logUnexpectedCdmaMethodCall("getCdmaEriIconMode");
        return -1;
    }

    @Override
    public String getCdmaEriText() {
        logUnexpectedCdmaMethodCall("getCdmaEriText");
        return "GSM nw, no ERI";
    }

    @Override
    public String getCdmaMin() {
        logUnexpectedCdmaMethodCall("getCdmaMin");
        return null;
    }

    @Override
    public boolean isMinInfoReady() {
        logUnexpectedCdmaMethodCall("isMinInfoReady");
        return false;
    }

    @Override
    public String getCdmaPrlVersion() {
        logUnexpectedCdmaMethodCall("getCdmaPrlVersion");
        return null;
    }

    @Override
    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
        logUnexpectedCdmaMethodCall("sendBurstDtmf");
    }

    @Override
    public void exitEmergencyCallbackMode() {
        logUnexpectedCdmaMethodCall("exitEmergencyCallbackMode");
    }

    @Override
    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("registerForCdmaOtaStatusChange");
    }

    @Override
    public void unregisterForCdmaOtaStatusChange(Handler h) {
        logUnexpectedCdmaMethodCall("unregisterForCdmaOtaStatusChange");
    }

    @Override
    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("registerForSubscriptionInfoReady");
    }

    @Override
    public void unregisterForSubscriptionInfoReady(Handler h) {
        logUnexpectedCdmaMethodCall("unregisterForSubscriptionInfoReady");
    }

    @Override
    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    @Override
    public boolean isOtaSpNumber(String dialStr) {
        return false;
    }

    @Override
    public void registerForCallWaiting(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("registerForCallWaiting");
    }

    @Override
    public void unregisterForCallWaiting(Handler h) {
        logUnexpectedCdmaMethodCall("unregisterForCallWaiting");
    }

    @Override
    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("registerForEcmTimerReset");
    }

    @Override
    public void unregisterForEcmTimerReset(Handler h) {
        logUnexpectedCdmaMethodCall("unregisterForEcmTimerReset");
    }

    @Override
    public void registerForSignalInfo(Handler h, int what, Object obj) {
        this.mCi.registerForSignalInfo(h, what, obj);
    }

    @Override
    public void unregisterForSignalInfo(Handler h) {
        this.mCi.unregisterForSignalInfo(h);
    }

    @Override
    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        this.mCi.registerForDisplayInfo(h, what, obj);
    }

    @Override
    public void unregisterForDisplayInfo(Handler h) {
        this.mCi.unregisterForDisplayInfo(h);
    }

    @Override
    public void registerForNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForNumberInfo(h, what, obj);
    }

    @Override
    public void unregisterForNumberInfo(Handler h) {
        this.mCi.unregisterForNumberInfo(h);
    }

    @Override
    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForRedirectedNumberInfo(h, what, obj);
    }

    @Override
    public void unregisterForRedirectedNumberInfo(Handler h) {
        this.mCi.unregisterForRedirectedNumberInfo(h);
    }

    @Override
    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForLineControlInfo(h, what, obj);
    }

    @Override
    public void unregisterForLineControlInfo(Handler h) {
        this.mCi.unregisterForLineControlInfo(h);
    }

    @Override
    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        this.mCi.registerFoT53ClirlInfo(h, what, obj);
    }

    @Override
    public void unregisterForT53ClirInfo(Handler h) {
        this.mCi.unregisterForT53ClirInfo(h);
    }

    @Override
    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForT53AudioControlInfo(h, what, obj);
    }

    @Override
    public void unregisterForT53AudioControlInfo(Handler h) {
        this.mCi.unregisterForT53AudioControlInfo(h);
    }

    @Override
    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
        logUnexpectedCdmaMethodCall("setOnEcbModeExitResponse");
    }

    @Override
    public void unsetOnEcbModeExitResponse(Handler h) {
        logUnexpectedCdmaMethodCall("unsetOnEcbModeExitResponse");
    }

    @Override
    public void registerForRadioOffOrNotAvailable(Handler h, int what, Object obj) {
        this.mRadioOffOrNotAvailableRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForRadioOffOrNotAvailable(Handler h) {
        this.mRadioOffOrNotAvailableRegistrants.remove(h);
    }

    @Override
    public String[] getActiveApnTypes() {
        return this.mDcTracker.getActiveApnTypes();
    }

    @Override
    public boolean hasMatchedTetherApnSetting() {
        return this.mDcTracker.hasMatchedTetherApnSetting();
    }

    @Override
    public String getActiveApnHost(String apnType) {
        return this.mDcTracker.getActiveApnString(apnType);
    }

    @Override
    public LinkProperties getLinkProperties(String apnType) {
        return this.mDcTracker.getLinkProperties(apnType);
    }

    @Override
    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        return this.mDcTracker.getNetworkCapabilities(apnType);
    }

    @Override
    public boolean isDataConnectivityPossible() {
        return isDataConnectivityPossible("default");
    }

    @Override
    public boolean isDataConnectivityPossible(String apnType) {
        return this.mDcTracker != null && this.mDcTracker.isDataPossible(apnType);
    }

    public void notifyNewRingingConnectionP(Connection cn) {
        if (this.mIsVoiceCapable) {
            AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
            this.mNewRingingConnectionRegistrants.notifyRegistrants(ar);
        }
    }

    private void notifyIncomingRing() {
        if (this.mIsVoiceCapable) {
            AsyncResult ar = new AsyncResult((Object) null, this, (Throwable) null);
            this.mIncomingRingRegistrants.notifyRegistrants(ar);
        }
    }

    private void sendIncomingCallRingNotification(int token) {
        if (this.mIsVoiceCapable && !this.mDoesRilSendMultipleCallRing && token == this.mCallRingContinueToken) {
            Rlog.d(LOG_TAG, "Sending notifyIncomingRing");
            notifyIncomingRing();
            sendMessageDelayed(obtainMessage(15, token, 0), this.mCallRingDelay);
            return;
        }
        Rlog.d(LOG_TAG, "Ignoring ring notification request, mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing + " token=" + token + " mCallRingContinueToken=" + this.mCallRingContinueToken + " mIsVoiceCapable=" + this.mIsVoiceCapable);
    }

    @Override
    public boolean isCspPlmnEnabled() {
        logUnexpectedGsmMethodCall("isCspPlmnEnabled");
        return false;
    }

    @Override
    public IsimRecords getIsimRecords() {
        Rlog.e(LOG_TAG, "getIsimRecords() is only supported on LTE devices");
        return null;
    }

    @Override
    public String getMsisdn() {
        logUnexpectedGsmMethodCall("getMsisdn");
        return null;
    }

    private static void logUnexpectedCdmaMethodCall(String name) {
        Rlog.e(LOG_TAG, "Error! " + name + "() in PhoneBase should not be called, CDMAPhone inactive.");
    }

    @Override
    public PhoneConstants.DataState getDataConnectionState() {
        return getDataConnectionState("default");
    }

    private static void logUnexpectedGsmMethodCall(String name) {
        Rlog.e(LOG_TAG, "Error! " + name + "() in PhoneBase should not be called, GSMPhone inactive.");
    }

    public void notifyCallForwardingIndicator() {
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive CDMAPhone.");
    }

    public void notifyDataConnectionFailed(String reason, String apnType) {
        this.mNotifier.notifyDataConnectionFailed(this, reason, apnType);
    }

    public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
        this.mNotifier.notifyPreciseDataConnectionFailed(this, reason, apnType, apn, failCause);
    }

    @Override
    public int getLteOnCdmaMode() {
        return this.mCi.getLteOnCdmaMode();
    }

    @Override
    public void setVoiceMessageWaiting(int line, int countWaiting) {
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive Phone.");
    }

    @Override
    public UsimServiceTable getUsimServiceTable() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getUsimServiceTable();
        }
        return null;
    }

    @Override
    public UiccCard getUiccCard() {
        return this.mUiccController.getUiccCard(this.mPhoneId);
    }

    @Override
    public String[] getPcscfAddress(String apnType) {
        return this.mDcTracker.getPcscfAddress(apnType);
    }

    @Override
    public void setImsRegistrationState(boolean registered) {
        this.mDcTracker.setImsRegistrationState(registered);
    }

    @Override
    public Phone getImsPhone() {
        return this.mImsPhone;
    }

    @Override
    public ImsPhone relinquishOwnershipOfImsPhone() {
        ImsPhone imsPhone = null;
        synchronized (this.mImsLock) {
            if (this.mImsPhone != null) {
                imsPhone = this.mImsPhone;
                this.mImsPhone = null;
                CallManager.getInstance().unregisterPhone(imsPhone);
                imsPhone.unregisterForSilentRedial(this);
            }
        }
        return imsPhone;
    }

    @Override
    public void acquireOwnershipOfImsPhone(ImsPhone imsPhone) {
        synchronized (this.mImsLock) {
            if (imsPhone != null) {
                if (this.mImsPhone != null) {
                    Rlog.e(LOG_TAG, "acquireOwnershipOfImsPhone: non-null mImsPhone. Shouldn't happen - but disposing");
                    this.mImsPhone.dispose();
                }
                this.mImsPhone = imsPhone;
                this.mImsServiceReady = true;
                this.mImsPhone.updateParentPhone(this);
                CallManager.getInstance().registerPhone(this.mImsPhone);
                this.mImsPhone.registerForSilentRedial(this, 32, null);
            }
        }
    }

    protected void updateImsPhone() {
        synchronized (this.mImsLock) {
            Rlog.d(LOG_TAG, "updateImsPhone mImsServiceReady=" + this.mImsServiceReady);
            if (this.mImsServiceReady && this.mImsPhone == null) {
                this.mImsPhone = PhoneFactory.makeImsPhone(this.mNotifier, this);
                CallManager.getInstance().registerPhone(this.mImsPhone);
                this.mImsPhone.registerForSilentRedial(this, 32, null);
                if (this.mDomainSelection == null) {
                    this.mDomainSelection = new DomainSelection(this.mContext, this);
                }
            } else if (!this.mImsServiceReady && this.mImsPhone != null) {
                CallManager.getInstance().unregisterPhone(this.mImsPhone);
                this.mImsPhone.unregisterForSilentRedial(this);
                this.mImsPhone.dispose();
                this.mImsPhone = null;
                if (this.mDomainSelection != null) {
                    this.mDomainSelection = null;
                }
            }
        }
    }

    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState) throws CallStateException {
        return null;
    }

    @Override
    public int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
    }

    @Override
    public int getPhoneId() {
        return this.mPhoneId;
    }

    @Override
    public int getVoicePhoneServiceState() {
        ImsPhone imsPhone = this.mImsPhone;
        if (imsPhone == null || imsPhone.getServiceState().getState() != 0) {
            return getServiceState().getState();
        }
        return 0;
    }

    @Override
    public boolean setOperatorBrandOverride(String brand) {
        return false;
    }

    @Override
    public boolean setRoamingOverride(List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId)) {
            return false;
        }
        setRoamingOverrideHelper(gsmRoamingList, GSM_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(gsmNonRoamingList, GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaRoamingList, CDMA_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        setRoamingOverrideHelper(cdmaNonRoamingList, CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccId);
        ServiceStateTracker tracker = getServiceStateTracker();
        if (tracker != null) {
            tracker.pollState();
        }
        return true;
    }

    private void setRoamingOverrideHelper(List<String> list, String prefix, String iccId) {
        SharedPreferences.Editor spEditor = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String key = prefix + iccId;
        if (list == null || list.isEmpty()) {
            spEditor.remove(key).commit();
        } else {
            spEditor.putStringSet(key, new HashSet(list)).commit();
        }
    }

    public boolean isMccMncMarkedAsRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isMccMncMarkedAsNonRoaming(String mccMnc) {
        return getRoamingOverrideHelper(GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, mccMnc);
    }

    public boolean isSidMarkedAsRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(SID));
    }

    public boolean isSidMarkedAsNonRoaming(int SID) {
        return getRoamingOverrideHelper(CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(SID));
    }

    @Override
    public boolean isImsRegistered() {
        ImsPhone imsPhone = this.mImsPhone;
        boolean isImsRegistered = false;
        if (imsPhone != null) {
            isImsRegistered = imsPhone.isImsRegistered();
        } else {
            ServiceStateTracker sst = getServiceStateTracker();
            if (sst != null) {
                isImsRegistered = sst.isImsRegistered();
            }
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + isImsRegistered);
        return isImsRegistered;
    }

    private boolean getRoamingOverrideHelper(String prefix, String key) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId) || TextUtils.isEmpty(key)) {
            return false;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        Set<String> value = sp.getStringSet(prefix + iccId, null);
        if (value != null) {
            return value.contains(key);
        }
        return false;
    }

    @Override
    public boolean isRadioAvailable() {
        return this.mCi.getRadioState().isAvailable();
    }

    @Override
    public void shutdownRadio() {
        getServiceStateTracker().requestShutdown();
    }

    @Override
    public void setRadioCapability(RadioCapability rc, Message response) {
        this.mCi.setRadioCapability(rc, response);
    }

    @Override
    public int getRadioAccessFamily() {
        return this.mRadioAccessFamily;
    }

    @Override
    public int getSupportedRadioAccessFamily() {
        return this.mCi.getSupportedRadioAccessFamily();
    }

    @Override
    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        this.mCi.registerForRadioCapabilityChanged(h, what, obj);
    }

    @Override
    public void unregisterForRadioCapabilityChanged(Handler h) {
        this.mCi.unregisterForRadioCapabilityChanged(this);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("PhoneBase: subId=" + getSubId());
        pw.println(" mPhoneId=" + this.mPhoneId);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mDnsCheckDisabled=" + this.mDnsCheckDisabled);
        pw.println(" mDcTracker=" + this.mDcTracker);
        pw.println(" mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        pw.println(" mCallRingContinueToken=" + this.mCallRingContinueToken);
        pw.println(" mCallRingDelay=" + this.mCallRingDelay);
        pw.println(" mIsTheCurrentActivePhone=" + this.mIsTheCurrentActivePhone);
        pw.println(" mIsVoiceCapable=" + this.mIsVoiceCapable);
        pw.println(" mIccRecords=" + this.mIccRecords.get());
        pw.println(" mUiccApplication=" + this.mUiccApplication.get());
        pw.println(" mSmsStorageMonitor=" + this.mSmsStorageMonitor);
        pw.println(" mSmsUsageMonitor=" + this.mSmsUsageMonitor);
        pw.flush();
        pw.println(" mLooper=" + this.mLooper);
        pw.println(" mContext=" + this.mContext);
        pw.println(" mNotifier=" + this.mNotifier);
        pw.println(" mSimulatedRadioControl=" + this.mSimulatedRadioControl);
        pw.println(" mUnitTestMode=" + this.mUnitTestMode);
        pw.println(" isDnsCheckDisabled()=" + isDnsCheckDisabled());
        pw.println(" getUnitTestMode()=" + getUnitTestMode());
        pw.println(" getState()=" + getState());
        pw.println(" getIccSerialNumber()=" + getIccSerialNumber());
        pw.println(" getIccRecordsLoaded()=" + getIccRecordsLoaded());
        pw.println(" getMessageWaitingIndicator()=" + getMessageWaitingIndicator());
        pw.println(" getCallForwardingIndicator()=" + getCallForwardingIndicator());
        pw.println(" isInEmergencyCall()=" + isInEmergencyCall());
        pw.flush();
        pw.println(" isInEcm()=" + isInEcm());
        pw.println(" getPhoneName()=" + getPhoneName());
        pw.println(" getPhoneType()=" + getPhoneType());
        pw.println(" getVoiceMessageCount()=" + getVoiceMessageCount());
        pw.println(" getActiveApnTypes()=" + getActiveApnTypes());
        pw.println(" isDataConnectivityPossible()=" + isDataConnectivityPossible());
        pw.println(" needsOtaServiceProvisioning=" + needsOtaServiceProvisioning());
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            this.mDcTracker.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            getServiceStateTracker().dump(fd, pw, args);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            getCallTracker().dump(fd, pw, args);
        } catch (Exception e3) {
            e3.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
        try {
            ((RIL) this.mCi).dump(fd, pw, args);
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
    }

    @Override
    public Connection dialVT(String dialString) throws CallStateException {
        return null;
    }

    @Override
    public void getSIMLockInfo(Message response) {
        this.mCi.getSIMLockInfo(response);
    }

    @Override
    public void disableDataCall() {
        this.mDcTracker.setInternalDataEnabled(false);
    }

    @Override
    public void enableDataCall() {
        this.mDcTracker.setInternalDataEnabled(true);
    }

    public void suspendDataCallByOtherPhone() {
        this.mDcTracker.suspendDataCallByOtherPhone();
    }

    public void resumeDataCallByOtherPhone() {
        this.mDcTracker.resumeDataCallByOtherPhone();
    }

    @Override
    public void turnOnIms() {
        if (this.mImsPhone != null) {
            this.mImsPhone.turnOnIms();
        }
    }

    @Override
    public void turnOffIms() {
        if (this.mImsPhone != null) {
            this.mImsPhone.turnOffIms();
        }
    }

    public DomainSelection getDomainSelection() {
        return this.mDomainSelection;
    }
}
