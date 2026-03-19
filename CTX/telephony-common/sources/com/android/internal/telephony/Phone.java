package com.android.internal.telephony;

import android.R;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.net.NetworkStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellLocation;
import android.telephony.ClientRequestStats;
import android.telephony.ImsiEncryptionInfo;
import android.telephony.PhysicalChannelConfig;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import com.android.ims.ImsCall;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneInternalInterface;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.dataconnection.KeepaliveStatus;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
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

public abstract class Phone extends Handler implements PhoneInternalInterface {
    private static final int ALREADY_IN_AUTO_SELECTION = 1;
    private static final String CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_non_roaming_list_";
    private static final String CDMA_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_roaming_list_";
    public static final String CF_ID = "cf_id_key";
    public static final String CF_STATUS = "cf_status_key";
    public static final String CLIR_KEY = "clir_key";
    public static final String CS_FALLBACK = "cs_fallback";
    public static final String DATA_DISABLED_ON_BOOT_KEY = "disabled_on_boot_key";
    public static final String DATA_ROAMING_IS_USER_SETTING_KEY = "data_roaming_is_user_setting_key";
    private static final int DEFAULT_REPORT_INTERVAL_MS = 200;
    private static final String DNS_SERVER_CHECK_DISABLED_KEY = "dns_server_check_disabled_key";
    protected static final int EVENT_CALL_RING = 14;
    private static final int EVENT_CALL_RING_CONTINUE = 15;
    protected static final int EVENT_CARRIER_CONFIG_CHANGED = 43;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 27;
    private static final int EVENT_CHECK_FOR_NETWORK_AUTOMATIC = 38;
    private static final int EVENT_CONFIG_LCE = 37;
    protected static final int EVENT_EMERGENCY_CALLBACK_MODE_ENTER = 25;
    protected static final int EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE = 26;
    protected static final int EVENT_GET_BASEBAND_VERSION_DONE = 6;
    protected static final int EVENT_GET_CALL_FORWARD_DONE = 13;
    protected static final int EVENT_GET_DEVICE_IDENTITY_DONE = 21;
    protected static final int EVENT_GET_IMEISV_DONE = 10;
    protected static final int EVENT_GET_IMEI_DONE = 9;
    protected static final int EVENT_GET_RADIO_CAPABILITY = 35;
    private static final int EVENT_GET_SIM_STATUS_DONE = 11;
    private static final int EVENT_ICC_CHANGED = 30;
    protected static final int EVENT_ICC_RECORD_EVENTS = 29;
    private static final int EVENT_INITIATE_SILENT_REDIAL = 32;
    protected static final int EVENT_LAST = 45;
    private static final int EVENT_MMI_DONE = 4;
    protected static final int EVENT_MODEM_RESET = 45;
    protected static final int EVENT_NV_READY = 23;
    protected static final int EVENT_RADIO_AVAILABLE = 1;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 33;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 8;
    protected static final int EVENT_RADIO_ON = 5;
    protected static final int EVENT_REGISTERED_TO_NETWORK = 19;
    protected static final int EVENT_REQUEST_VOICE_RADIO_TECH_DONE = 40;
    protected static final int EVENT_RIL_CONNECTED = 41;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 22;
    protected static final int EVENT_SET_CALL_FORWARD_DONE = 12;
    protected static final int EVENT_SET_CLIR_COMPLETE = 18;
    private static final int EVENT_SET_ENHANCED_VP = 24;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC = 28;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC_COMPLETE = 17;
    protected static final int EVENT_SET_NETWORK_MANUAL_COMPLETE = 16;
    protected static final int EVENT_SET_ROAMING_PREFERENCE_DONE = 44;
    protected static final int EVENT_SET_VM_NUMBER_DONE = 20;
    protected static final int EVENT_SIM_RECORDS_LOADED = 3;
    private static final int EVENT_SRVCC_STATE_CHANGED = 31;
    protected static final int EVENT_SS = 36;
    protected static final int EVENT_SSN = 2;
    private static final int EVENT_UNSOL_OEM_HOOK_RAW = 34;
    protected static final int EVENT_UPDATE_PHONE_OBJECT = 42;
    protected static final int EVENT_USSD = 7;
    protected static final int EVENT_VOICE_RADIO_TECH_CHANGED = 39;
    public static final String EXTRA_KEY_ALERT_MESSAGE = "alertMessage";
    public static final String EXTRA_KEY_ALERT_SHOW = "alertShow";
    public static final String EXTRA_KEY_ALERT_TITLE = "alertTitle";
    public static final String EXTRA_KEY_NOTIFICATION_MESSAGE = "notificationMessage";
    private static final String GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_non_roaming_list_";
    private static final String GSM_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_roaming_list_";
    private static final boolean LCE_PULL_MODE = true;
    private static final String LOG_TAG = "Phone";
    public static final String NETWORK_SELECTION_KEY = "network_selection_key";
    public static final String NETWORK_SELECTION_NAME_KEY = "network_selection_name_key";
    public static final String NETWORK_SELECTION_SHORT_KEY = "network_selection_short_key";
    private static final String VM_COUNT = "vm_count_key";
    private static final String VM_ID = "vm_id_key";
    protected static final Object lockForRadioTechnologyChange = new Object();
    protected final int USSD_MAX_QUEUE;
    private final String mActionAttached;
    private final String mActionDetached;
    private final AppSmsManager mAppSmsManager;
    private int mCallRingContinueToken;
    private int mCallRingDelay;
    protected CarrierActionAgent mCarrierActionAgent;
    protected CarrierSignalAgent mCarrierSignalAgent;
    public CommandsInterface mCi;
    protected final Context mContext;
    public DcTracker mDcTracker;
    protected DeviceStateMonitor mDeviceStateMonitor;
    protected final RegistrantList mDisconnectRegistrants;
    private boolean mDnsCheckDisabled;
    private boolean mDoesRilSendMultipleCallRing;
    protected final RegistrantList mEmergencyCallToggledRegistrants;
    private final RegistrantList mHandoverRegistrants;
    protected final AtomicReference<IccRecords> mIccRecords;
    private BroadcastReceiver mImsIntentReceiver;
    protected Phone mImsPhone;
    protected boolean mImsServiceReady;
    private final RegistrantList mIncomingRingRegistrants;
    protected boolean mIsPhoneInEcmState;
    protected boolean mIsVideoCapable;
    private boolean mIsVoiceCapable;
    private int mLceStatus;
    private Looper mLooper;
    protected final RegistrantList mMmiCompleteRegistrants;
    protected final RegistrantList mMmiRegistrants;
    private String mName;
    private final RegistrantList mNewRingingConnectionRegistrants;
    protected PhoneNotifier mNotifier;
    protected int mPhoneId;
    protected Registrant mPostDialHandler;
    private final RegistrantList mPreciseCallStateRegistrants;
    private final AtomicReference<RadioCapability> mRadioCapability;
    protected final RegistrantList mRadioOffOrNotAvailableRegistrants;
    protected final RegistrantList mServiceStateRegistrants;
    private SimActivationTracker mSimActivationTracker;
    protected final RegistrantList mSimRecordsLoadedRegistrants;
    protected SimulatedRadioControl mSimulatedRadioControl;
    public SmsStorageMonitor mSmsStorageMonitor;
    public SmsUsageMonitor mSmsUsageMonitor;
    protected final RegistrantList mSuppServiceFailedRegistrants;
    protected TelephonyComponentFactory mTelephonyComponentFactory;
    public TelephonyTester mTelephonyTester;
    protected AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;
    private boolean mUnitTestMode;
    protected final RegistrantList mUnknownConnectionRegistrants;
    private final RegistrantList mVideoCapabilityChangedRegistrants;
    protected int mVmCount;

    public static class NetworkSelectMessage {
        public Message message;
        public String operatorAlphaLong;
        public String operatorAlphaShort;
        public String operatorNumeric;
    }

    public abstract int getPhoneType();

    public abstract PhoneConstants.State getState();

    protected abstract void onUpdateIccAvailability();

    public abstract void sendEmergencyCallStateChange(boolean z);

    public abstract void setBroadcastEmergencyCallStateChanges(boolean z);

    public void handleExitEmergencyCallbackMode() {
    }

    public IccRecords getIccRecords() {
        return this.mIccRecords.get();
    }

    public String getPhoneName() {
        return this.mName;
    }

    protected void setPhoneName(String str) {
        this.mName = str;
    }

    public String getNai() {
        return null;
    }

    public String getActionDetached() {
        return this.mActionDetached;
    }

    public String getActionAttached() {
        return this.mActionAttached;
    }

    public void setSystemProperty(String str, String str2) {
        if (getUnitTestMode()) {
            return;
        }
        TelephonyManager.setTelephonyProperty(this.mPhoneId, str, str2);
    }

    public void setGlobalSystemProperty(String str, String str2) {
        if (getUnitTestMode()) {
            return;
        }
        TelephonyManager.setTelephonyProperty(str, str2);
    }

    public String getSystemProperty(String str, String str2) {
        if (getUnitTestMode()) {
            return null;
        }
        return SystemProperties.get(str, str2);
    }

    protected Phone(String str, PhoneNotifier phoneNotifier, Context context, CommandsInterface commandsInterface, boolean z) {
        this(str, phoneNotifier, context, commandsInterface, z, KeepaliveStatus.INVALID_HANDLE, TelephonyComponentFactory.getInstance());
    }

    protected Phone(String str, PhoneNotifier phoneNotifier, Context context, CommandsInterface commandsInterface, boolean z, int i, TelephonyComponentFactory telephonyComponentFactory) {
        this.USSD_MAX_QUEUE = 10;
        this.mImsIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: action " + intent.getAction());
                if (intent.hasExtra("android:phone_id")) {
                    int intExtra = intent.getIntExtra("android:phone_id", -1);
                    Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: extraPhoneId = " + intExtra);
                    if (intExtra == -1 || intExtra != Phone.this.getPhoneId()) {
                        return;
                    }
                }
                synchronized (Phone.lockForRadioTechnologyChange) {
                    if (intent.getAction().equals("com.android.ims.IMS_SERVICE_UP")) {
                        Phone.this.mImsServiceReady = true;
                        Phone.this.updateImsPhone();
                        ImsManager.getInstance(Phone.this.mContext, Phone.this.mPhoneId).updateImsServiceConfig(false);
                    } else if (intent.getAction().equals("com.android.ims.IMS_SERVICE_DOWN")) {
                        Phone.this.mImsServiceReady = false;
                        Phone.this.updateImsPhone();
                    }
                }
            }
        };
        this.mVmCount = 0;
        this.mIsVoiceCapable = true;
        this.mIsPhoneInEcmState = false;
        this.mIsVideoCapable = false;
        this.mUiccController = null;
        this.mIccRecords = new AtomicReference<>();
        this.mUiccApplication = new AtomicReference<>();
        this.mImsServiceReady = false;
        this.mImsPhone = null;
        this.mRadioCapability = new AtomicReference<>();
        this.mLceStatus = -1;
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
        this.mVideoCapabilityChangedRegistrants = new RegistrantList();
        this.mEmergencyCallToggledRegistrants = new RegistrantList();
        this.mPhoneId = i;
        this.mName = str;
        this.mNotifier = phoneNotifier;
        this.mContext = context;
        this.mLooper = Looper.myLooper();
        this.mCi = commandsInterface;
        this.mActionDetached = getClass().getPackage().getName() + ".action_detached";
        this.mActionAttached = getClass().getPackage().getName() + ".action_attached";
        this.mAppSmsManager = telephonyComponentFactory.makeAppSmsManager(context);
        if (Build.IS_DEBUGGABLE) {
            this.mTelephonyTester = new TelephonyTester(this);
        }
        setUnitTestMode(z);
        this.mDnsCheckDisabled = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(DNS_SERVER_CHECK_DISABLED_KEY, false);
        this.mCi.setOnCallRing(this, 14, null);
        this.mIsVoiceCapable = this.mContext.getResources().getBoolean(R.^attr-private.popupPromptView);
        this.mDoesRilSendMultipleCallRing = SystemProperties.getBoolean("ro.telephony.call_ring.multiple", true);
        Rlog.d(LOG_TAG, "mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        this.mCallRingDelay = SystemProperties.getInt("ro.telephony.call_ring.delay", 3000);
        Rlog.d(LOG_TAG, "mCallRingDelay=" + this.mCallRingDelay);
        if (getPhoneType() == 5) {
            return;
        }
        Locale localeFromCarrierProperties = getLocaleFromCarrierProperties(this.mContext);
        if (localeFromCarrierProperties != null && !TextUtils.isEmpty(localeFromCarrierProperties.getCountry())) {
            String country = localeFromCarrierProperties.getCountry();
            try {
                Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_country_code");
            } catch (Settings.SettingNotFoundException e) {
                ((WifiManager) this.mContext.getSystemService("wifi")).setCountryCode(country);
            }
        }
        this.mTelephonyComponentFactory = telephonyComponentFactory;
        this.mSmsStorageMonitor = this.mTelephonyComponentFactory.makeSmsStorageMonitor(this);
        this.mSmsUsageMonitor = this.mTelephonyComponentFactory.makeSmsUsageMonitor(context);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 30, null);
        this.mSimActivationTracker = this.mTelephonyComponentFactory.makeSimActivationTracker(this);
        if (getPhoneType() != 3) {
            this.mCi.registerForSrvccStateChanged(this, 31, null);
        }
        this.mCi.setOnUnsolOemHookRaw(this, 34, null);
        this.mCi.startLceService(200, true, obtainMessage(37));
    }

    public void startMonitoringImsService() {
        if (getPhoneType() == 3) {
            return;
        }
        synchronized (lockForRadioTechnologyChange) {
            IntentFilter intentFilter = new IntentFilter();
            ImsManager imsManager = ImsManager.getInstance(this.mContext, getPhoneId());
            if (imsManager != null && !imsManager.isDynamicBinding()) {
                intentFilter.addAction("com.android.ims.IMS_SERVICE_UP");
                intentFilter.addAction("com.android.ims.IMS_SERVICE_DOWN");
            }
            this.mContext.registerReceiver(this.mImsIntentReceiver, intentFilter);
            if (imsManager != null && (imsManager.isDynamicBinding() || imsManager.isServiceAvailable())) {
                this.mImsServiceReady = true;
                updateImsPhone();
            }
        }
    }

    public boolean supportsConversionOfCdmaCallerIdMmiCodesWhileRoaming() {
        PersistableBundle config = ((CarrierConfigManager) getContext().getSystemService("carrier_config")).getConfig();
        if (config != null) {
            return config.getBoolean("convert_cdma_caller_id_mmi_codes_while_roaming_on_3gpp_bool", false);
        }
        return false;
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case 16:
            case 17:
                handleSetSelectNetwork((AsyncResult) message.obj);
                return;
            default:
                switch (message.what) {
                    case 14:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING Received state=" + getState());
                        if (((AsyncResult) message.obj).exception == null) {
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
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING_CONTINUE Received state=" + getState());
                        if (getState() == PhoneConstants.State.RINGING) {
                            sendIncomingCallRingNotification(message.arg1);
                            return;
                        }
                        return;
                    case 30:
                        onUpdateIccAvailability();
                        return;
                    case 31:
                        AsyncResult asyncResult = (AsyncResult) message.obj;
                        if (asyncResult.exception == null) {
                            handleSrvccStateChanged((int[]) asyncResult.result);
                            return;
                        }
                        Rlog.e(LOG_TAG, "Srvcc exception: " + asyncResult.exception);
                        return;
                    case 32:
                        Rlog.d(LOG_TAG, "Event EVENT_INITIATE_SILENT_REDIAL Received");
                        AsyncResult asyncResult2 = (AsyncResult) message.obj;
                        if (asyncResult2.exception == null && asyncResult2.result != null) {
                            String str = (String) asyncResult2.result;
                            if (TextUtils.isEmpty(str)) {
                                return;
                            }
                            try {
                                dialInternal(str, new PhoneInternalInterface.DialArgs.Builder().build());
                                return;
                            } catch (CallStateException e) {
                                Rlog.e(LOG_TAG, "silent redial failed: " + e);
                                return;
                            }
                        }
                        return;
                    case 34:
                        AsyncResult asyncResult3 = (AsyncResult) message.obj;
                        if (asyncResult3.exception == null) {
                            this.mNotifier.notifyOemHookRawEventForSubscriber(getSubId(), (byte[]) asyncResult3.result);
                            return;
                        } else {
                            Rlog.e(LOG_TAG, "OEM hook raw exception: " + asyncResult3.exception);
                            return;
                        }
                    case 37:
                        AsyncResult asyncResult4 = (AsyncResult) message.obj;
                        if (asyncResult4.exception != null) {
                            Rlog.d(LOG_TAG, "config LCE service failed: " + asyncResult4.exception);
                            return;
                        }
                        this.mLceStatus = ((Integer) ((ArrayList) asyncResult4.result).get(0)).intValue();
                        return;
                    case 38:
                        onCheckForNetworkSelectionModeAutomatic(message);
                        return;
                    default:
                        throw new RuntimeException("unexpected event not handled");
                }
        }
    }

    public ArrayList<Connection> getHandoverConnection() {
        return null;
    }

    public void notifySrvccState(Call.SrvccState srvccState) {
    }

    public void registerForSilentRedial(Handler handler, int i, Object obj) {
    }

    public void unregisterForSilentRedial(Handler handler) {
    }

    private void handleSrvccStateChanged(int[] iArr) {
        Call.SrvccState srvccState;
        Rlog.d(LOG_TAG, "handleSrvccStateChanged");
        Phone phone = this.mImsPhone;
        Call.SrvccState srvccState2 = Call.SrvccState.NONE;
        if (iArr != null && iArr.length != 0) {
            int i = iArr[0];
            ArrayList<Connection> handoverConnection = null;
            switch (i) {
                case 0:
                    srvccState = Call.SrvccState.STARTED;
                    if (phone != null) {
                        handoverConnection = phone.getHandoverConnection();
                        migrateFrom(phone);
                    } else {
                        Rlog.d(LOG_TAG, "HANDOVER_STARTED: mImsPhone null");
                    }
                    break;
                case 1:
                    srvccState = Call.SrvccState.COMPLETED;
                    if (phone != null) {
                        phone.notifySrvccState(srvccState);
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
            getCallTracker().notifySrvccState(srvccState, handoverConnection);
            notifyVoLteServiceStateChanged(new VoLteServiceState(i));
        }
    }

    public Context getContext() {
        return this.mContext;
    }

    public void disableDnsCheck(boolean z) {
        this.mDnsCheckDisabled = z;
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editorEdit.putBoolean(DNS_SERVER_CHECK_DISABLED_KEY, z);
        editorEdit.apply();
    }

    public boolean isDnsCheckDisabled() {
        return this.mDnsCheckDisabled;
    }

    public void registerForPreciseCallStateChanged(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mPreciseCallStateRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForPreciseCallStateChanged(Handler handler) {
        this.mPreciseCallStateRegistrants.remove(handler);
    }

    protected void notifyPreciseCallStateChangedP() {
        this.mPreciseCallStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, this, (Throwable) null));
        this.mNotifier.notifyPreciseCallState(this);
    }

    public void registerForHandoverStateChanged(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mHandoverRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForHandoverStateChanged(Handler handler) {
        this.mHandoverRegistrants.remove(handler);
    }

    public void notifyHandoverStateChanged(Connection connection) {
        this.mHandoverRegistrants.notifyRegistrants(new AsyncResult((Object) null, connection, (Throwable) null));
    }

    protected void setIsInEmergencyCall() {
    }

    protected void migrateFrom(Phone phone) {
        migrate(this.mHandoverRegistrants, phone.mHandoverRegistrants);
        migrate(this.mPreciseCallStateRegistrants, phone.mPreciseCallStateRegistrants);
        migrate(this.mNewRingingConnectionRegistrants, phone.mNewRingingConnectionRegistrants);
        migrate(this.mIncomingRingRegistrants, phone.mIncomingRingRegistrants);
        migrate(this.mDisconnectRegistrants, phone.mDisconnectRegistrants);
        migrate(this.mServiceStateRegistrants, phone.mServiceStateRegistrants);
        migrate(this.mMmiCompleteRegistrants, phone.mMmiCompleteRegistrants);
        migrate(this.mMmiRegistrants, phone.mMmiRegistrants);
        migrate(this.mUnknownConnectionRegistrants, phone.mUnknownConnectionRegistrants);
        migrate(this.mSuppServiceFailedRegistrants, phone.mSuppServiceFailedRegistrants);
        if (phone.isInEmergencyCall()) {
            setIsInEmergencyCall();
        }
    }

    protected void migrate(RegistrantList registrantList, RegistrantList registrantList2) {
        registrantList2.removeCleared();
        int size = registrantList2.size();
        for (int i = 0; i < size; i++) {
            Message messageMessageForRegistrant = ((Registrant) registrantList2.get(i)).messageForRegistrant();
            if (messageMessageForRegistrant != null) {
                if (messageMessageForRegistrant.obj != CallManager.getInstance().getRegistrantIdentifier()) {
                    registrantList.add((Registrant) registrantList2.get(i));
                }
            } else {
                Rlog.d(LOG_TAG, "msg is null");
            }
        }
    }

    public void registerForUnknownConnection(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mUnknownConnectionRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForUnknownConnection(Handler handler) {
        this.mUnknownConnectionRegistrants.remove(handler);
    }

    public void registerForNewRingingConnection(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mNewRingingConnectionRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForNewRingingConnection(Handler handler) {
        this.mNewRingingConnectionRegistrants.remove(handler);
    }

    public void registerForVideoCapabilityChanged(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mVideoCapabilityChangedRegistrants.addUnique(handler, i, obj);
        notifyForVideoCapabilityChanged(this.mIsVideoCapable);
    }

    public void unregisterForVideoCapabilityChanged(Handler handler) {
        this.mVideoCapabilityChangedRegistrants.remove(handler);
    }

    public void registerForInCallVoicePrivacyOn(Handler handler, int i, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOn(handler, i, obj);
    }

    public void unregisterForInCallVoicePrivacyOn(Handler handler) {
        this.mCi.unregisterForInCallVoicePrivacyOn(handler);
    }

    public void registerForInCallVoicePrivacyOff(Handler handler, int i, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOff(handler, i, obj);
    }

    public void unregisterForInCallVoicePrivacyOff(Handler handler) {
        this.mCi.unregisterForInCallVoicePrivacyOff(handler);
    }

    public void registerForIncomingRing(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mIncomingRingRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForIncomingRing(Handler handler) {
        this.mIncomingRingRegistrants.remove(handler);
    }

    public void registerForDisconnect(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mDisconnectRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForDisconnect(Handler handler) {
        this.mDisconnectRegistrants.remove(handler);
    }

    public void registerForSuppServiceFailed(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mSuppServiceFailedRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForSuppServiceFailed(Handler handler) {
        this.mSuppServiceFailedRegistrants.remove(handler);
    }

    public void registerForMmiInitiate(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mMmiRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForMmiInitiate(Handler handler) {
        this.mMmiRegistrants.remove(handler);
    }

    public void registerForMmiComplete(Handler handler, int i, Object obj) {
        checkCorrectThread(handler);
        this.mMmiCompleteRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForMmiComplete(Handler handler) {
        checkCorrectThread(handler);
        this.mMmiCompleteRegistrants.remove(handler);
    }

    public void registerForSimRecordsLoaded(Handler handler, int i, Object obj) {
    }

    public void unregisterForSimRecordsLoaded(Handler handler) {
    }

    public void registerForTtyModeReceived(Handler handler, int i, Object obj) {
    }

    public void unregisterForTtyModeReceived(Handler handler) {
    }

    public void setNetworkSelectionModeAutomatic(Message message) {
        Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic, querying current mode");
        Message messageObtainMessage = obtainMessage(38);
        messageObtainMessage.obj = message;
        this.mCi.getNetworkSelectionMode(messageObtainMessage);
    }

    protected void onCheckForNetworkSelectionModeAutomatic(Message message) {
        boolean z;
        AsyncResult asyncResult = (AsyncResult) message.obj;
        Message message2 = (Message) asyncResult.userObj;
        if (asyncResult.exception == null && asyncResult.result != null) {
            try {
                z = false;
                if (((int[]) asyncResult.result)[0] != 0) {
                    z = true;
                }
            } catch (Exception e) {
                z = true;
            }
        } else {
            z = true;
        }
        NetworkSelectMessage networkSelectMessage = new NetworkSelectMessage();
        networkSelectMessage.message = message2;
        networkSelectMessage.operatorNumeric = "";
        networkSelectMessage.operatorAlphaLong = "";
        networkSelectMessage.operatorAlphaShort = "";
        if (z) {
            this.mCi.setNetworkSelectionModeAutomatic(obtainMessage(17, networkSelectMessage));
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            if (networkSelectMessage.message != null) {
                networkSelectMessage.message.arg1 = 1;
            }
            asyncResult.userObj = networkSelectMessage;
            handleSetSelectNetwork(asyncResult);
        }
        updateSavedNetworkOperator(networkSelectMessage);
    }

    public void getNetworkSelectionMode(Message message) {
        this.mCi.getNetworkSelectionMode(message);
    }

    public List<ClientRequestStats> getClientRequestStats() {
        return this.mCi.getClientRequestStats();
    }

    public void selectNetworkManually(OperatorInfo operatorInfo, boolean z, Message message) {
        NetworkSelectMessage networkSelectMessage = new NetworkSelectMessage();
        networkSelectMessage.message = message;
        networkSelectMessage.operatorNumeric = operatorInfo.getOperatorNumeric();
        networkSelectMessage.operatorAlphaLong = operatorInfo.getOperatorAlphaLong();
        networkSelectMessage.operatorAlphaShort = operatorInfo.getOperatorAlphaShort();
        this.mCi.setNetworkSelectionModeManual(operatorInfo.getOperatorNumeric(), obtainMessage(16, networkSelectMessage));
        if (z) {
            updateSavedNetworkOperator(networkSelectMessage);
        } else {
            clearSavedNetworkSelection();
        }
    }

    public void registerForEmergencyCallToggle(Handler handler, int i, Object obj) {
        this.mEmergencyCallToggledRegistrants.add(new Registrant(handler, i, obj));
    }

    public void unregisterForEmergencyCallToggle(Handler handler) {
        this.mEmergencyCallToggledRegistrants.remove(handler);
    }

    protected void updateSavedNetworkOperator(NetworkSelectMessage networkSelectMessage) {
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editorEdit.putString(NETWORK_SELECTION_KEY + subId, networkSelectMessage.operatorNumeric);
            editorEdit.putString(NETWORK_SELECTION_NAME_KEY + subId, networkSelectMessage.operatorAlphaLong);
            editorEdit.putString(NETWORK_SELECTION_SHORT_KEY + subId, networkSelectMessage.operatorAlphaShort);
            if (!editorEdit.commit()) {
                Rlog.e(LOG_TAG, "failed to commit network selection preference");
                return;
            }
            return;
        }
        Rlog.e(LOG_TAG, "Cannot update network selection preference due to invalid subId " + subId);
    }

    protected void handleSetSelectNetwork(AsyncResult asyncResult) {
        if (!(asyncResult.userObj instanceof NetworkSelectMessage)) {
            Rlog.e(LOG_TAG, "unexpected result from user object.");
            return;
        }
        NetworkSelectMessage networkSelectMessage = (NetworkSelectMessage) asyncResult.userObj;
        if (networkSelectMessage.message != null) {
            AsyncResult.forMessage(networkSelectMessage.message, asyncResult.result, asyncResult.exception);
            networkSelectMessage.message.sendToTarget();
        }
    }

    private OperatorInfo getSavedNetworkSelection() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String string = defaultSharedPreferences.getString(NETWORK_SELECTION_KEY + getSubId(), "");
        return new OperatorInfo(defaultSharedPreferences.getString(NETWORK_SELECTION_NAME_KEY + getSubId(), ""), defaultSharedPreferences.getString(NETWORK_SELECTION_SHORT_KEY + getSubId(), ""), string);
    }

    protected void clearSavedNetworkSelection() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove(NETWORK_SELECTION_KEY + getSubId()).remove(NETWORK_SELECTION_NAME_KEY + getSubId()).remove(NETWORK_SELECTION_SHORT_KEY + getSubId()).commit();
    }

    protected void restoreSavedNetworkSelection(Message message) {
        OperatorInfo savedNetworkSelection = getSavedNetworkSelection();
        if (savedNetworkSelection == null || TextUtils.isEmpty(savedNetworkSelection.getOperatorNumeric())) {
            setNetworkSelectionModeAutomatic(message);
        } else {
            selectNetworkManually(savedNetworkSelection, true, message);
        }
    }

    public void saveClirSetting(int i) {
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        editorEdit.putInt(CLIR_KEY + getPhoneId(), i);
        Rlog.i(LOG_TAG, "saveClirSetting: clir_key" + getPhoneId() + "=" + i);
        if (!editorEdit.commit()) {
            Rlog.e(LOG_TAG, "Failed to commit CLIR preference");
        }
    }

    private void setUnitTestMode(boolean z) {
        this.mUnitTestMode = z;
    }

    public boolean getUnitTestMode() {
        return this.mUnitTestMode;
    }

    protected void notifyDisconnectP(Connection connection) {
        this.mDisconnectRegistrants.notifyRegistrants(new AsyncResult((Object) null, connection, (Throwable) null));
    }

    public void registerForServiceStateChanged(Handler handler, int i, Object obj) {
        this.mServiceStateRegistrants.add(handler, i, obj);
    }

    public void unregisterForServiceStateChanged(Handler handler) {
        this.mServiceStateRegistrants.remove(handler);
    }

    public void registerForRingbackTone(Handler handler, int i, Object obj) {
        this.mCi.registerForRingbackTone(handler, i, obj);
    }

    public void unregisterForRingbackTone(Handler handler) {
        this.mCi.unregisterForRingbackTone(handler);
    }

    public void registerForOnHoldTone(Handler handler, int i, Object obj) {
    }

    public void unregisterForOnHoldTone(Handler handler) {
    }

    public void registerForResendIncallMute(Handler handler, int i, Object obj) {
        this.mCi.registerForResendIncallMute(handler, i, obj);
    }

    public void unregisterForResendIncallMute(Handler handler) {
        this.mCi.unregisterForResendIncallMute(handler);
    }

    public void setEchoSuppressionEnabled() {
    }

    protected void notifyServiceStateChangedP(ServiceState serviceState) {
        this.mServiceStateRegistrants.notifyRegistrants(new AsyncResult((Object) null, serviceState, (Throwable) null));
        this.mNotifier.notifyServiceState(this);
    }

    public SimulatedRadioControl getSimulatedRadioControl() {
        return this.mSimulatedRadioControl;
    }

    private void checkCorrectThread(Handler handler) {
        if (handler.getLooper() != this.mLooper) {
            throw new RuntimeException("com.android.internal.telephony.Phone must be used from within one thread");
        }
    }

    private static Locale getLocaleFromCarrierProperties(Context context) {
        String str = SystemProperties.get("ro.carrier");
        if (str == null || str.length() == 0 || "unknown".equals(str)) {
            return null;
        }
        CharSequence[] textArray = context.getResources().getTextArray(R.array.config_optionalIpSecAlgorithms);
        for (int i = 0; i < textArray.length; i += 3) {
            if (str.equals(textArray[i].toString())) {
                return Locale.forLanguageTag(textArray[i + 1].toString().replace('_', '-'));
            }
        }
        return null;
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler iccFileHandler;
        UiccCardApplication uiccCardApplication = this.mUiccApplication.get();
        if (uiccCardApplication == null) {
            Rlog.d(LOG_TAG, "getIccFileHandler: uiccApplication == null, return null");
            iccFileHandler = null;
        } else {
            iccFileHandler = uiccCardApplication.getIccFileHandler();
        }
        Rlog.d(LOG_TAG, "getIccFileHandler: fh=" + iccFileHandler);
        return iccFileHandler;
    }

    public Handler getHandler() {
        return this;
    }

    public void updatePhoneObject(int i) {
    }

    public ServiceStateTracker getServiceStateTracker() {
        return null;
    }

    public CallTracker getCallTracker() {
        return null;
    }

    public void setVoiceActivationState(int i) {
        this.mSimActivationTracker.setVoiceActivationState(i);
    }

    public void setDataActivationState(int i) {
        this.mSimActivationTracker.setDataActivationState(i);
    }

    public int getVoiceActivationState() {
        return this.mSimActivationTracker.getVoiceActivationState();
    }

    public int getDataActivationState() {
        return this.mSimActivationTracker.getDataActivationState();
    }

    public void updateVoiceMail() {
        Rlog.e(LOG_TAG, "updateVoiceMail() should be overridden");
    }

    public IccCardApplicationStatus.AppType getCurrentUiccAppType() {
        UiccCardApplication uiccCardApplication = this.mUiccApplication.get();
        if (uiccCardApplication != null) {
            return uiccCardApplication.getType();
        }
        return IccCardApplicationStatus.AppType.APPTYPE_UNKNOWN;
    }

    public IccCard getIccCard() {
        return null;
    }

    public String getIccSerialNumber() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            return iccRecords.getIccId();
        }
        return null;
    }

    public String getFullIccSerialNumber() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            return iccRecords.getFullIccId();
        }
        return null;
    }

    public boolean getIccRecordsLoaded() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            return iccRecords.getRecordsLoaded();
        }
        return false;
    }

    public List<CellInfo> getAllCellInfo(WorkSource workSource) {
        return privatizeCellInfoList(getServiceStateTracker().getAllCellInfo(workSource));
    }

    public CellLocation getCellLocation() {
        return getCellLocation(null);
    }

    private List<CellInfo> privatizeCellInfoList(List<CellInfo> list) {
        if (list == null) {
            return null;
        }
        if (Settings.Secure.getInt(getContext().getContentResolver(), "location_mode", 0) != 0) {
            return list;
        }
        ArrayList arrayList = new ArrayList(list.size());
        for (CellInfo cellInfo : list) {
            if (cellInfo instanceof CellInfoCdma) {
                CellInfoCdma cellInfoCdma = (CellInfoCdma) cellInfo;
                CellIdentityCdma cellIdentity = cellInfoCdma.getCellIdentity();
                CellIdentityCdma cellIdentityCdma = new CellIdentityCdma(cellIdentity.getNetworkId(), cellIdentity.getSystemId(), cellIdentity.getBasestationId(), KeepaliveStatus.INVALID_HANDLE, KeepaliveStatus.INVALID_HANDLE);
                CellInfoCdma cellInfoCdma2 = new CellInfoCdma(cellInfoCdma);
                cellInfoCdma2.setCellIdentity(cellIdentityCdma);
                arrayList.add(cellInfoCdma2);
            } else {
                arrayList.add(cellInfo);
            }
        }
        return arrayList;
    }

    public void setCellInfoListRate(int i, WorkSource workSource) {
        this.mCi.setCellInfoListRate(i, null, workSource);
    }

    public boolean getMessageWaitingIndicator() {
        return this.mVmCount != 0;
    }

    protected int getCallForwardingIndicatorFromSharedPref() {
        String string;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            int i = defaultSharedPreferences.getInt(CF_STATUS + subId, -1);
            Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: for subId " + subId + "= " + i);
            if (i == -1 && (string = defaultSharedPreferences.getString(CF_ID, null)) != null) {
                if (string.equals(getSubscriberId())) {
                    i = defaultSharedPreferences.getInt(CF_STATUS, 0);
                    boolean z = true;
                    if (i != 1) {
                        z = false;
                    }
                    setCallForwardingIndicatorInSharedPref(z);
                    Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: " + i);
                } else {
                    Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: returning DISABLED as status for matching subscriberId not found");
                }
                SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
                editorEdit.remove(CF_ID);
                editorEdit.remove(CF_STATUS);
                editorEdit.apply();
            }
            return i;
        }
        Rlog.e(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: invalid subId " + subId);
        return 0;
    }

    protected void setCallForwardingIndicatorInSharedPref(boolean z) {
        int i = z ? 1 : 0;
        int subId = getSubId();
        Rlog.i(LOG_TAG, "setCallForwardingIndicatorInSharedPref: Storing status = " + i + " in pref " + CF_STATUS + subId);
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        StringBuilder sb = new StringBuilder();
        sb.append(CF_STATUS);
        sb.append(subId);
        editorEdit.putInt(sb.toString(), i);
        editorEdit.apply();
    }

    public void setVoiceCallForwardingFlag(int i, boolean z, String str) {
        setCallForwardingIndicatorInSharedPref(z);
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            iccRecords.setVoiceCallForwardingFlag(i, z, str);
        }
    }

    protected void setVoiceCallForwardingFlag(IccRecords iccRecords, int i, boolean z, String str) {
        setCallForwardingIndicatorInSharedPref(z);
        iccRecords.setVoiceCallForwardingFlag(i, z, str);
    }

    public boolean getCallForwardingIndicator() {
        int callForwardingIndicatorFromSharedPref;
        if (getPhoneType() == 2) {
            Rlog.e(LOG_TAG, "getCallForwardingIndicator: not possible in CDMA");
            return false;
        }
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            callForwardingIndicatorFromSharedPref = iccRecords.getVoiceCallForwardingFlag();
        } else {
            callForwardingIndicatorFromSharedPref = -1;
        }
        if (callForwardingIndicatorFromSharedPref == -1) {
            callForwardingIndicatorFromSharedPref = getCallForwardingIndicatorFromSharedPref();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("getCallForwardingIndicator: iccForwardingFlag=");
        sb.append(iccRecords != null ? Integer.valueOf(iccRecords.getVoiceCallForwardingFlag()) : "null");
        sb.append(", sharedPrefFlag=");
        sb.append(getCallForwardingIndicatorFromSharedPref());
        Rlog.v(LOG_TAG, sb.toString());
        return callForwardingIndicatorFromSharedPref == 1;
    }

    public CarrierSignalAgent getCarrierSignalAgent() {
        return this.mCarrierSignalAgent;
    }

    public CarrierActionAgent getCarrierActionAgent() {
        return this.mCarrierActionAgent;
    }

    public void queryCdmaRoamingPreference(Message message) {
        this.mCi.queryCdmaRoamingPreference(message);
    }

    public SignalStrength getSignalStrength() {
        ServiceStateTracker serviceStateTracker = getServiceStateTracker();
        if (serviceStateTracker == null) {
            return new SignalStrength();
        }
        return serviceStateTracker.getSignalStrength();
    }

    public boolean isConcurrentVoiceAndDataAllowed() {
        ServiceStateTracker serviceStateTracker = getServiceStateTracker();
        if (serviceStateTracker == null) {
            return false;
        }
        return serviceStateTracker.isConcurrentVoiceAndDataAllowed();
    }

    public void setCdmaRoamingPreference(int i, Message message) {
        this.mCi.setCdmaRoamingPreference(i, message);
    }

    public void setCdmaSubscription(int i, Message message) {
        this.mCi.setCdmaSubscriptionSource(i, message);
    }

    public void setPreferredNetworkType(int i, Message message) {
        int radioAccessFamily = getRadioAccessFamily();
        int rafFromNetworkType = RadioAccessFamily.getRafFromNetworkType(i);
        if (radioAccessFamily == 1 || rafFromNetworkType == 1) {
            Rlog.d(LOG_TAG, "setPreferredNetworkType: Abort, unknown RAF: " + radioAccessFamily + " " + rafFromNetworkType);
            if (message != null) {
                AsyncResult.forMessage(message, (Object) null, new CommandException(CommandException.Error.GENERIC_FAILURE));
                message.sendToTarget();
                return;
            }
            return;
        }
        int networkTypeFromRaf = RadioAccessFamily.getNetworkTypeFromRaf(rafFromNetworkType & radioAccessFamily);
        Rlog.d(LOG_TAG, "setPreferredNetworkType: networkType = " + i + " modemRaf = " + radioAccessFamily + " rafFromType = " + rafFromNetworkType + " filteredType = " + networkTypeFromRaf);
        this.mCi.setPreferredNetworkType(networkTypeFromRaf, message);
    }

    public void getPreferredNetworkType(Message message) {
        this.mCi.getPreferredNetworkType(message);
    }

    public void getSmscAddress(Message message) {
        this.mCi.getSmscAddress(message);
    }

    public void setSmscAddress(String str, Message message) {
        this.mCi.setSmscAddress(str, message);
    }

    public void setTTYMode(int i, Message message) {
        this.mCi.setTTYMode(i, message);
    }

    public void setUiTTYMode(int i, Message message) {
        Rlog.d(LOG_TAG, "unexpected setUiTTYMode method call");
    }

    public void queryTTYMode(Message message) {
        this.mCi.queryTTYMode(message);
    }

    public void enableEnhancedVoicePrivacy(boolean z, Message message) {
    }

    public void getEnhancedVoicePrivacy(Message message) {
    }

    public void setBandMode(int i, Message message) {
        this.mCi.setBandMode(i, message);
    }

    public void queryAvailableBandMode(Message message) {
        this.mCi.queryAvailableBandMode(message);
    }

    @Deprecated
    public void invokeOemRilRequestRaw(byte[] bArr, Message message) {
        this.mCi.invokeOemRilRequestRaw(bArr, message);
    }

    @Deprecated
    public void invokeOemRilRequestStrings(String[] strArr, Message message) {
        this.mCi.invokeOemRilRequestStrings(strArr, message);
    }

    public void nvReadItem(int i, Message message) {
        this.mCi.nvReadItem(i, message);
    }

    public void nvWriteItem(int i, String str, Message message) {
        this.mCi.nvWriteItem(i, str, message);
    }

    public void nvWriteCdmaPrl(byte[] bArr, Message message) {
        this.mCi.nvWriteCdmaPrl(bArr, message);
    }

    public void nvResetConfig(int i, Message message) {
        this.mCi.nvResetConfig(i, message);
    }

    public void notifyDataActivity() {
        this.mNotifier.notifyDataActivity(this);
    }

    private void notifyMessageWaitingIndicator() {
        if (!this.mIsVoiceCapable) {
            return;
        }
        this.mNotifier.notifyMessageWaitingChanged(this);
    }

    public void notifyDataConnection(String str, String str2, PhoneConstants.DataState dataState) {
        this.mNotifier.notifyDataConnection(this, str, str2, dataState);
    }

    public void notifyDataConnection(String str, String str2) {
        this.mNotifier.notifyDataConnection(this, str, str2, getDataConnectionState(str2));
    }

    public void notifyDataConnection(String str) {
        for (String str2 : getActiveApnTypes()) {
            this.mNotifier.notifyDataConnection(this, str, str2, getDataConnectionState(str2));
        }
    }

    public void notifyOtaspChanged(int i) {
        this.mNotifier.notifyOtaspChanged(this, i);
    }

    public void notifyVoiceActivationStateChanged(int i) {
        this.mNotifier.notifyVoiceActivationStateChanged(this, i);
    }

    public void notifyDataActivationStateChanged(int i) {
        this.mNotifier.notifyDataActivationStateChanged(this, i);
    }

    public void notifyUserMobileDataStateChanged(boolean z) {
        this.mNotifier.notifyUserMobileDataStateChanged(this, z);
    }

    public void notifySignalStrength() {
        this.mNotifier.notifySignalStrength(this);
    }

    public void notifyCellInfo(List<CellInfo> list) {
        this.mNotifier.notifyCellInfo(this, privatizeCellInfoList(list));
    }

    public void notifyPhysicalChannelConfiguration(List<PhysicalChannelConfig> list) {
        this.mNotifier.notifyPhysicalChannelConfiguration(this, list);
    }

    public void notifyVoLteServiceStateChanged(VoLteServiceState voLteServiceState) {
        this.mNotifier.notifyVoLteServiceStateChanged(this, voLteServiceState);
    }

    public boolean isInEmergencyCall() {
        return false;
    }

    protected static boolean getInEcmMode() {
        return SystemProperties.getBoolean("ril.cdma.inecmmode", false);
    }

    public boolean isInEcm() {
        return this.mIsPhoneInEcmState;
    }

    public void setIsInEcm(boolean z) {
        setGlobalSystemProperty("ril.cdma.inecmmode", String.valueOf(z));
        this.mIsPhoneInEcmState = z;
    }

    private static int getVideoState(Call call) {
        Connection earliestConnection = call.getEarliestConnection();
        if (earliestConnection != null) {
            return earliestConnection.getVideoState();
        }
        return 0;
    }

    private boolean isVideoCallOrConference(Call call) {
        if (call.isMultiparty()) {
            return true;
        }
        if (!(call instanceof ImsPhoneCall)) {
            return false;
        }
        ImsCall imsCall = ((ImsPhoneCall) call).getImsCall();
        return imsCall != null && (imsCall.isVideoCall() || imsCall.wasVideoCall());
    }

    public boolean isImsVideoCallOrConferencePresent() {
        boolean z = false;
        if (this.mImsPhone != null && (isVideoCallOrConference(this.mImsPhone.getForegroundCall()) || isVideoCallOrConference(this.mImsPhone.getBackgroundCall()) || isVideoCallOrConference(this.mImsPhone.getRingingCall()))) {
            z = true;
        }
        Rlog.d(LOG_TAG, "isImsVideoCallOrConferencePresent: " + z);
        return z;
    }

    public int getVoiceMessageCount() {
        return this.mVmCount;
    }

    public void setVoiceMessageCount(int i) {
        this.mVmCount = i;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            Rlog.d(LOG_TAG, "setVoiceMessageCount: Storing Voice Mail Count = " + i + " for mVmCountKey = " + VM_COUNT + subId + " in preferences.");
            SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
            StringBuilder sb = new StringBuilder();
            sb.append(VM_COUNT);
            sb.append(subId);
            editorEdit.putInt(sb.toString(), i);
            editorEdit.apply();
        } else {
            Rlog.e(LOG_TAG, "setVoiceMessageCount in sharedPreference: invalid subId " + subId);
        }
        notifyMessageWaitingIndicator();
    }

    protected int getStoredVoiceMessageCount() {
        int subId = getSubId();
        int i = 0;
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            int i2 = defaultSharedPreferences.getInt(VM_COUNT + subId, -2);
            if (i2 != -2) {
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference for subId " + subId + "= " + i2);
                return i2;
            }
            String string = defaultSharedPreferences.getString(VM_ID, null);
            if (string == null) {
                return 0;
            }
            String subscriberId = getSubscriberId();
            if (subscriberId != null && subscriberId.equals(string)) {
                i = defaultSharedPreferences.getInt(VM_COUNT, 0);
                setVoiceMessageCount(i);
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference = " + i);
            } else {
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: returning 0 as count for matching subscriberId not found");
            }
            SharedPreferences.Editor editorEdit = defaultSharedPreferences.edit();
            editorEdit.remove(VM_ID);
            editorEdit.remove(VM_COUNT);
            editorEdit.apply();
            return i;
        }
        Rlog.e(LOG_TAG, "getStoredVoiceMessageCount: invalid subId " + subId);
        return 0;
    }

    public void sendDialerSpecialCode(String str) {
        if (!TextUtils.isEmpty(str)) {
            Intent intent = new Intent("android.provider.Telephony.SECRET_CODE", Uri.parse("android_secret_code://" + str));
            intent.addFlags(16777216);
            this.mContext.sendBroadcast(intent);
        }
    }

    public int getCdmaEriIconIndex() {
        return -1;
    }

    public int getCdmaEriIconMode() {
        return -1;
    }

    public String getCdmaEriText() {
        return "GSM nw, no ERI";
    }

    public String getCdmaMin() {
        return null;
    }

    public boolean isMinInfoReady() {
        return false;
    }

    public String getCdmaPrlVersion() {
        return null;
    }

    public void sendBurstDtmf(String str, int i, int i2, Message message) {
    }

    public void setOnPostDialCharacter(Handler handler, int i, Object obj) {
        this.mPostDialHandler = new Registrant(handler, i, obj);
    }

    public Registrant getPostDialHandler() {
        return this.mPostDialHandler;
    }

    public void exitEmergencyCallbackMode() {
    }

    public void registerForCdmaOtaStatusChange(Handler handler, int i, Object obj) {
    }

    public void unregisterForCdmaOtaStatusChange(Handler handler) {
    }

    public void registerForSubscriptionInfoReady(Handler handler, int i, Object obj) {
    }

    public void unregisterForSubscriptionInfoReady(Handler handler) {
    }

    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    public boolean isOtaSpNumber(String str) {
        return false;
    }

    public void registerForCallWaiting(Handler handler, int i, Object obj) {
    }

    public void unregisterForCallWaiting(Handler handler) {
    }

    public void registerForEcmTimerReset(Handler handler, int i, Object obj) {
    }

    public void unregisterForEcmTimerReset(Handler handler) {
    }

    public void registerForSignalInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForSignalInfo(handler, i, obj);
    }

    public void unregisterForSignalInfo(Handler handler) {
        this.mCi.unregisterForSignalInfo(handler);
    }

    public void registerForDisplayInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForDisplayInfo(handler, i, obj);
    }

    public void unregisterForDisplayInfo(Handler handler) {
        this.mCi.unregisterForDisplayInfo(handler);
    }

    public void registerForNumberInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForNumberInfo(handler, i, obj);
    }

    public void unregisterForNumberInfo(Handler handler) {
        this.mCi.unregisterForNumberInfo(handler);
    }

    public void registerForRedirectedNumberInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForRedirectedNumberInfo(handler, i, obj);
    }

    public void unregisterForRedirectedNumberInfo(Handler handler) {
        this.mCi.unregisterForRedirectedNumberInfo(handler);
    }

    public void registerForLineControlInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForLineControlInfo(handler, i, obj);
    }

    public void unregisterForLineControlInfo(Handler handler) {
        this.mCi.unregisterForLineControlInfo(handler);
    }

    public void registerFoT53ClirlInfo(Handler handler, int i, Object obj) {
        this.mCi.registerFoT53ClirlInfo(handler, i, obj);
    }

    public void unregisterForT53ClirInfo(Handler handler) {
        this.mCi.unregisterForT53ClirInfo(handler);
    }

    public void registerForT53AudioControlInfo(Handler handler, int i, Object obj) {
        this.mCi.registerForT53AudioControlInfo(handler, i, obj);
    }

    public void unregisterForT53AudioControlInfo(Handler handler) {
        this.mCi.unregisterForT53AudioControlInfo(handler);
    }

    public void setOnEcbModeExitResponse(Handler handler, int i, Object obj) {
    }

    public void unsetOnEcbModeExitResponse(Handler handler) {
    }

    public void registerForRadioOffOrNotAvailable(Handler handler, int i, Object obj) {
        this.mRadioOffOrNotAvailableRegistrants.addUnique(handler, i, obj);
    }

    public void unregisterForRadioOffOrNotAvailable(Handler handler) {
        this.mRadioOffOrNotAvailableRegistrants.remove(handler);
    }

    public String[] getActiveApnTypes() {
        if (this.mDcTracker == null) {
            return null;
        }
        return this.mDcTracker.getActiveApnTypes();
    }

    public boolean hasMatchedTetherApnSetting() {
        return this.mDcTracker.hasMatchedTetherApnSetting();
    }

    public String getActiveApnHost(String str) {
        return this.mDcTracker.getActiveApnString(str);
    }

    public LinkProperties getLinkProperties(String str) {
        return this.mDcTracker.getLinkProperties(str);
    }

    public NetworkCapabilities getNetworkCapabilities(String str) {
        return this.mDcTracker.getNetworkCapabilities(str);
    }

    public boolean isDataAllowed() {
        return this.mDcTracker != null && this.mDcTracker.isDataAllowed(null);
    }

    public boolean isDataAllowed(DataConnectionReasons dataConnectionReasons) {
        return this.mDcTracker != null && this.mDcTracker.isDataAllowed(dataConnectionReasons);
    }

    public void carrierActionSetMeteredApnsEnabled(boolean z) {
        this.mCarrierActionAgent.carrierActionSetMeteredApnsEnabled(z);
    }

    public void carrierActionSetRadioEnabled(boolean z) {
        this.mCarrierActionAgent.carrierActionSetRadioEnabled(z);
    }

    public void carrierActionReportDefaultNetworkStatus(boolean z) {
        this.mCarrierActionAgent.carrierActionReportDefaultNetworkStatus(z);
    }

    public void notifyNewRingingConnectionP(Connection connection) {
        if (!this.mIsVoiceCapable) {
            return;
        }
        this.mNewRingingConnectionRegistrants.notifyRegistrants(new AsyncResult((Object) null, connection, (Throwable) null));
    }

    public void notifyUnknownConnectionP(Connection connection) {
        this.mUnknownConnectionRegistrants.notifyResult(connection);
    }

    public void notifyForVideoCapabilityChanged(boolean z) {
        this.mIsVideoCapable = z;
        this.mVideoCapabilityChangedRegistrants.notifyRegistrants(new AsyncResult((Object) null, Boolean.valueOf(z), (Throwable) null));
    }

    private void notifyIncomingRing() {
        if (!this.mIsVoiceCapable) {
            return;
        }
        this.mIncomingRingRegistrants.notifyRegistrants(new AsyncResult((Object) null, this, (Throwable) null));
    }

    private void sendIncomingCallRingNotification(int i) {
        if (this.mIsVoiceCapable && !this.mDoesRilSendMultipleCallRing && i == this.mCallRingContinueToken) {
            Rlog.d(LOG_TAG, "Sending notifyIncomingRing");
            notifyIncomingRing();
            sendMessageDelayed(obtainMessage(15, i, 0), this.mCallRingDelay);
            return;
        }
        Rlog.d(LOG_TAG, "Ignoring ring notification request, mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing + " token=" + i + " mCallRingContinueToken=" + this.mCallRingContinueToken + " mIsVoiceCapable=" + this.mIsVoiceCapable);
    }

    public boolean isCspPlmnEnabled() {
        return false;
    }

    public IsimRecords getIsimRecords() {
        Rlog.e(LOG_TAG, "getIsimRecords() is only supported on LTE devices");
        return null;
    }

    public String getMsisdn() {
        return null;
    }

    public String getPlmn() {
        return null;
    }

    public PhoneConstants.DataState getDataConnectionState() {
        return getDataConnectionState("default");
    }

    public void notifyCallForwardingIndicator() {
    }

    public void notifyDataConnectionFailed(String str, String str2) {
        this.mNotifier.notifyDataConnectionFailed(this, str, str2);
    }

    public void notifyPreciseDataConnectionFailed(String str, String str2, String str3, String str4) {
        this.mNotifier.notifyPreciseDataConnectionFailed(this, str, str2, str3, str4);
    }

    public int getLteOnCdmaMode() {
        return this.mCi.getLteOnCdmaMode();
    }

    public void setVoiceMessageWaiting(int i, int i2) {
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive Phone.");
    }

    public UsimServiceTable getUsimServiceTable() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null) {
            return iccRecords.getUsimServiceTable();
        }
        return null;
    }

    public UiccCard getUiccCard() {
        return this.mUiccController.getUiccCard(this.mPhoneId);
    }

    public String[] getPcscfAddress(String str) {
        return this.mDcTracker.getPcscfAddress(str);
    }

    public void setImsRegistrationState(boolean z) {
    }

    public Phone getImsPhone() {
        return this.mImsPhone;
    }

    public ImsiEncryptionInfo getCarrierInfoForImsiEncryption(int i) {
        return null;
    }

    public void setCarrierInfoForImsiEncryption(ImsiEncryptionInfo imsiEncryptionInfo) {
    }

    public int getCarrierId() {
        return -1;
    }

    public String getCarrierName() {
        return null;
    }

    public int getCarrierIdListVersion() {
        return -1;
    }

    public void resetCarrierKeysForImsiEncryption() {
    }

    public boolean isUtEnabled() {
        if (this.mImsPhone != null) {
            return this.mImsPhone.isUtEnabled();
        }
        return false;
    }

    public void dispose() {
    }

    protected void updateImsPhone() {
        Rlog.d(LOG_TAG, "updateImsPhone mImsServiceReady=" + this.mImsServiceReady);
        if (this.mImsServiceReady && this.mImsPhone == null) {
            this.mImsPhone = PhoneFactory.makeImsPhone(this.mNotifier, this);
            CallManager.getInstance().registerPhone(this.mImsPhone);
            this.mImsPhone.registerForSilentRedial(this, 32, null);
        } else if (!this.mImsServiceReady && this.mImsPhone != null) {
            CallManager.getInstance().unregisterPhone(this.mImsPhone);
            this.mImsPhone.unregisterForSilentRedial(this);
            this.mImsPhone.dispose();
            this.mImsPhone = null;
        }
    }

    protected Connection dialInternal(String str, PhoneInternalInterface.DialArgs dialArgs) throws CallStateException {
        return null;
    }

    public int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public int getVoicePhoneServiceState() {
        Phone phone = this.mImsPhone;
        if (phone != null && phone.getServiceState().getState() == 0) {
            return 0;
        }
        return getServiceState().getState();
    }

    public boolean setOperatorBrandOverride(String str) {
        return false;
    }

    public boolean setRoamingOverride(List<String> list, List<String> list2, List<String> list3, List<String> list4) {
        String iccSerialNumber = getIccSerialNumber();
        if (TextUtils.isEmpty(iccSerialNumber)) {
            return false;
        }
        setRoamingOverrideHelper(list, GSM_ROAMING_LIST_OVERRIDE_PREFIX, iccSerialNumber);
        setRoamingOverrideHelper(list2, GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccSerialNumber);
        setRoamingOverrideHelper(list3, CDMA_ROAMING_LIST_OVERRIDE_PREFIX, iccSerialNumber);
        setRoamingOverrideHelper(list4, CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, iccSerialNumber);
        ServiceStateTracker serviceStateTracker = getServiceStateTracker();
        if (serviceStateTracker != null) {
            serviceStateTracker.pollState();
            return true;
        }
        return true;
    }

    private void setRoamingOverrideHelper(List<String> list, String str, String str2) {
        SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(this.mContext).edit();
        String str3 = str + str2;
        if (list == null || list.isEmpty()) {
            editorEdit.remove(str3).commit();
        } else {
            editorEdit.putStringSet(str3, new HashSet(list)).commit();
        }
    }

    public boolean isMccMncMarkedAsRoaming(String str) {
        return getRoamingOverrideHelper(GSM_ROAMING_LIST_OVERRIDE_PREFIX, str);
    }

    public boolean isMccMncMarkedAsNonRoaming(String str) {
        return getRoamingOverrideHelper(GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX, str);
    }

    public boolean isSidMarkedAsRoaming(int i) {
        return getRoamingOverrideHelper(CDMA_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(i));
    }

    public boolean isSidMarkedAsNonRoaming(int i) {
        return getRoamingOverrideHelper(CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX, Integer.toString(i));
    }

    public boolean isImsRegistered() {
        boolean zIsImsRegistered;
        Phone phone = this.mImsPhone;
        if (phone != null) {
            zIsImsRegistered = phone.isImsRegistered();
        } else {
            ServiceStateTracker serviceStateTracker = getServiceStateTracker();
            if (serviceStateTracker != null) {
                zIsImsRegistered = serviceStateTracker.isImsRegistered();
            } else {
                zIsImsRegistered = false;
            }
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + zIsImsRegistered);
        return zIsImsRegistered;
    }

    public boolean isWifiCallingEnabled() {
        boolean zIsWifiCallingEnabled;
        Phone phone = this.mImsPhone;
        if (phone != null) {
            zIsWifiCallingEnabled = phone.isWifiCallingEnabled();
        } else {
            zIsWifiCallingEnabled = false;
        }
        Rlog.d(LOG_TAG, "isWifiCallingEnabled =" + zIsWifiCallingEnabled);
        return zIsWifiCallingEnabled;
    }

    public boolean isVolteEnabled() {
        boolean zIsVolteEnabled;
        Phone phone = this.mImsPhone;
        if (phone != null) {
            zIsVolteEnabled = phone.isVolteEnabled();
        } else {
            zIsVolteEnabled = false;
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + zIsVolteEnabled);
        return zIsVolteEnabled;
    }

    public int getImsRegistrationTech() {
        int imsRegistrationTech;
        Phone phone = this.mImsPhone;
        if (phone != null) {
            imsRegistrationTech = phone.getImsRegistrationTech();
        } else {
            imsRegistrationTech = -1;
        }
        Rlog.d(LOG_TAG, "getImsRegistrationTechnology =" + imsRegistrationTech);
        return imsRegistrationTech;
    }

    private boolean getRoamingOverrideHelper(String str, String str2) {
        String iccSerialNumber = getIccSerialNumber();
        if (TextUtils.isEmpty(iccSerialNumber) || TextUtils.isEmpty(str2)) {
            return false;
        }
        Set<String> stringSet = PreferenceManager.getDefaultSharedPreferences(this.mContext).getStringSet(str + iccSerialNumber, null);
        if (stringSet == null) {
            return false;
        }
        return stringSet.contains(str2);
    }

    public boolean isRadioAvailable() {
        return this.mCi.getRadioState().isAvailable();
    }

    public boolean isRadioOn() {
        return this.mCi.getRadioState().isOn();
    }

    public void shutdownRadio() {
        getServiceStateTracker().requestShutdown();
    }

    public boolean isShuttingDown() {
        return getServiceStateTracker().isDeviceShuttingDown();
    }

    public void setRadioCapability(RadioCapability radioCapability, Message message) {
        this.mCi.setRadioCapability(radioCapability, message);
    }

    public int getRadioAccessFamily() {
        RadioCapability radioCapability = getRadioCapability();
        if (radioCapability == null) {
            return 1;
        }
        return radioCapability.getRadioAccessFamily();
    }

    public String getModemUuId() {
        RadioCapability radioCapability = getRadioCapability();
        return radioCapability == null ? "" : radioCapability.getLogicalModemUuid();
    }

    public RadioCapability getRadioCapability() {
        return this.mRadioCapability.get();
    }

    public void radioCapabilityUpdated(RadioCapability radioCapability) {
        this.mRadioCapability.set(radioCapability);
        if (SubscriptionManager.isValidSubscriptionId(getSubId())) {
            sendSubscriptionSettings(!this.mContext.getResources().getBoolean(R.^attr-private.showAtTop));
        }
    }

    public void sendSubscriptionSettings(boolean z) {
        setPreferredNetworkType(PhoneFactory.calculatePreferredNetworkType(this.mContext, getSubId()), null);
        if (z) {
            restoreSavedNetworkSelection(null);
        }
    }

    protected void setPreferredNetworkTypeIfSimLoaded() {
        if (SubscriptionManager.isValidSubscriptionId(getSubId())) {
            setPreferredNetworkType(PhoneFactory.calculatePreferredNetworkType(this.mContext, getSubId()), null);
        }
    }

    public void registerForRadioCapabilityChanged(Handler handler, int i, Object obj) {
        this.mCi.registerForRadioCapabilityChanged(handler, i, obj);
    }

    public void unregisterForRadioCapabilityChanged(Handler handler) {
        this.mCi.unregisterForRadioCapabilityChanged(this);
    }

    public boolean isImsUseEnabled() {
        ImsManager imsManager = ImsManager.getInstance(this.mContext, this.mPhoneId);
        return (imsManager.isVolteEnabledByPlatform() && imsManager.isEnhanced4gLteModeSettingEnabledByUser()) || (imsManager.isWfcEnabledByPlatform() && imsManager.isWfcEnabledByUser() && imsManager.isNonTtyOrTtyOnVolteEnabled());
    }

    public boolean isImsAvailable() {
        if (this.mImsPhone == null) {
            return false;
        }
        return this.mImsPhone.isImsAvailable();
    }

    public boolean isVideoEnabled() {
        Phone phone = this.mImsPhone;
        if (phone != null) {
            return phone.isVideoEnabled();
        }
        return false;
    }

    public int getLceStatus() {
        return this.mLceStatus;
    }

    public void getModemActivityInfo(Message message) {
        this.mCi.getModemActivityInfo(message);
    }

    public void startLceAfterRadioIsAvailable() {
        this.mCi.startLceService(200, true, obtainMessage(37));
    }

    public void setAllowedCarriers(List<android.service.carrier.CarrierIdentifier> list, Message message) {
        this.mCi.setAllowedCarriers(list, message);
    }

    public void setSignalStrengthReportingCriteria(int[] iArr, int i) {
    }

    public void setLinkCapacityReportingCriteria(int[] iArr, int[] iArr2, int i) {
    }

    public void getAllowedCarriers(Message message) {
        this.mCi.getAllowedCarriers(message);
    }

    public Locale getLocaleFromSimAndCarrierPrefs() {
        IccRecords iccRecords = this.mIccRecords.get();
        if (iccRecords != null && iccRecords.getSimLanguage() != null) {
            return new Locale(iccRecords.getSimLanguage());
        }
        return getLocaleFromCarrierProperties(this.mContext);
    }

    public void updateDataConnectionTracker() {
        this.mDcTracker.update();
    }

    public void setInternalDataEnabled(boolean z, Message message) {
        this.mDcTracker.setInternalDataEnabled(z, message);
    }

    public boolean updateCurrentCarrierInProvider() {
        return false;
    }

    public void registerForAllDataDisconnected(Handler handler, int i, Object obj) {
        this.mDcTracker.registerForAllDataDisconnected(handler, i, obj);
    }

    public void unregisterForAllDataDisconnected(Handler handler) {
        this.mDcTracker.unregisterForAllDataDisconnected(handler);
    }

    public void registerForDataEnabledChanged(Handler handler, int i, Object obj) {
        this.mDcTracker.registerForDataEnabledChanged(handler, i, obj);
    }

    public void unregisterForDataEnabledChanged(Handler handler) {
        this.mDcTracker.unregisterForDataEnabledChanged(handler);
    }

    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return null;
    }

    protected boolean isMatchGid(String str) {
        String groupIdLevel1 = getGroupIdLevel1();
        int length = str.length();
        return !TextUtils.isEmpty(groupIdLevel1) && groupIdLevel1.length() >= length && groupIdLevel1.substring(0, length).equalsIgnoreCase(str);
    }

    public static void checkWfcWifiOnlyModeBeforeDial(Phone phone, int i, Context context) throws CallStateException {
        boolean z;
        if (phone == null || !phone.isWifiCallingEnabled()) {
            ImsManager imsManager = ImsManager.getInstance(context, i);
            if (!imsManager.isWfcEnabledByPlatform() || !imsManager.isWfcEnabledByUser() || imsManager.getWfcMode() != 0) {
                z = false;
            } else {
                z = true;
            }
            if (z) {
                throw new CallStateException(1, "WFC Wi-Fi Only Mode: IMS not registered");
            }
        }
    }

    public void startRingbackTone() {
    }

    public void stopRingbackTone() {
    }

    public void callEndCleanupHandOverCallIfAny() {
    }

    public void cancelUSSD() {
    }

    public Phone getDefaultPhone() {
        return this;
    }

    public NetworkStats getVtDataUsage(boolean z) {
        if (this.mImsPhone == null) {
            return null;
        }
        return this.mImsPhone.getVtDataUsage(z);
    }

    public void setPolicyDataEnabled(boolean z) {
        this.mDcTracker.setPolicyDataEnabled(z);
    }

    public Uri[] getCurrentSubscriberUris() {
        return null;
    }

    public AppSmsManager getAppSmsManager() {
        return this.mAppSmsManager;
    }

    public void setSimPowerState(int i) {
        this.mCi.setSimCardPower(i, null);
    }

    public void setRadioIndicationUpdateMode(int i, int i2) {
        if (this.mDeviceStateMonitor != null) {
            this.mDeviceStateMonitor.setIndicationUpdateMode(i, i2);
        }
    }

    public void setCarrierTestOverride(String str, String str2, String str3, String str4, String str5, String str6, String str7) {
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("Phone: subId=" + getSubId());
        printWriter.println(" mPhoneId=" + this.mPhoneId);
        printWriter.println(" mCi=" + this.mCi);
        printWriter.println(" mDnsCheckDisabled=" + this.mDnsCheckDisabled);
        printWriter.println(" mDcTracker=" + this.mDcTracker);
        printWriter.println(" mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        printWriter.println(" mCallRingContinueToken=" + this.mCallRingContinueToken);
        printWriter.println(" mCallRingDelay=" + this.mCallRingDelay);
        printWriter.println(" mIsVoiceCapable=" + this.mIsVoiceCapable);
        printWriter.println(" mIccRecords=" + this.mIccRecords.get());
        printWriter.println(" mUiccApplication=" + this.mUiccApplication.get());
        printWriter.println(" mSmsStorageMonitor=" + this.mSmsStorageMonitor);
        printWriter.println(" mSmsUsageMonitor=" + this.mSmsUsageMonitor);
        printWriter.flush();
        printWriter.println(" mLooper=" + this.mLooper);
        printWriter.println(" mContext=" + this.mContext);
        printWriter.println(" mNotifier=" + this.mNotifier);
        printWriter.println(" mSimulatedRadioControl=" + this.mSimulatedRadioControl);
        printWriter.println(" mUnitTestMode=" + this.mUnitTestMode);
        printWriter.println(" isDnsCheckDisabled()=" + isDnsCheckDisabled());
        printWriter.println(" getUnitTestMode()=" + getUnitTestMode());
        printWriter.println(" getState()=" + getState());
        printWriter.println(" getIccSerialNumber()=" + getIccSerialNumber());
        printWriter.println(" getIccRecordsLoaded()=" + getIccRecordsLoaded());
        printWriter.println(" getMessageWaitingIndicator()=" + getMessageWaitingIndicator());
        printWriter.println(" getCallForwardingIndicator()=" + getCallForwardingIndicator());
        printWriter.println(" isInEmergencyCall()=" + isInEmergencyCall());
        printWriter.flush();
        printWriter.println(" isInEcm()=" + isInEcm());
        printWriter.println(" getPhoneName()=" + getPhoneName());
        printWriter.println(" getPhoneType()=" + getPhoneType());
        printWriter.println(" getVoiceMessageCount()=" + getVoiceMessageCount());
        printWriter.println(" getActiveApnTypes()=" + getActiveApnTypes());
        printWriter.println(" needsOtaServiceProvisioning=" + needsOtaServiceProvisioning());
        printWriter.flush();
        printWriter.println("++++++++++++++++++++++++++++++++");
        if (this.mImsPhone != null) {
            try {
                this.mImsPhone.dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mDcTracker != null) {
            try {
                this.mDcTracker.dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (getServiceStateTracker() != null) {
            try {
                getServiceStateTracker().dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e3) {
                e3.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCarrierActionAgent != null) {
            try {
                this.mCarrierActionAgent.dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e4) {
                e4.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCarrierSignalAgent != null) {
            try {
                this.mCarrierSignalAgent.dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e5) {
                e5.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (getCallTracker() != null) {
            try {
                getCallTracker().dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e6) {
                e6.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mSimActivationTracker != null) {
            try {
                this.mSimActivationTracker.dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e7) {
                e7.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mDeviceStateMonitor != null) {
            printWriter.println("DeviceStateMonitor:");
            this.mDeviceStateMonitor.dump(fileDescriptor, printWriter, strArr);
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCi != null && (this.mCi instanceof RIL)) {
            try {
                ((RIL) this.mCi).dump(fileDescriptor, printWriter, strArr);
            } catch (Exception e8) {
                e8.printStackTrace();
            }
            printWriter.flush();
            printWriter.println("++++++++++++++++++++++++++++++++");
        }
    }
}
