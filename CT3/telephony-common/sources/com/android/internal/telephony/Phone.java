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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telecom.VideoProfile;
import android.telephony.CellIdentityCdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.text.TextUtils;
import com.android.ims.ImsManager;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.dataconnection.DcTracker;
import com.android.internal.telephony.test.SimulatedRadioControl;
import com.android.internal.telephony.uicc.IccCardApplicationStatus;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.IsimRecords;
import com.android.internal.telephony.uicc.UiccCard;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UsimServiceTable;
import com.mediatek.internal.telephony.FemtoCellInfo;
import com.mediatek.internal.telephony.NetworkInfoWithAcT;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Phone extends Handler implements PhoneInternalInterface {
    public static final String ACT_TYPE_GSM = "0";
    public static final String ACT_TYPE_LTE = "7";
    public static final String ACT_TYPE_UTRAN = "2";
    private static final String CDMA_NON_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_non_roaming_list_";
    private static final String CDMA_ROAMING_LIST_OVERRIDE_PREFIX = "cdma_roaming_list_";
    private static final String CFU_TIME_SLOT = "persist.radio.cfu.timeslot.";
    public static final String CF_ID = "cf_id_key";
    public static final String CF_STATUS = "cf_status_key";
    public static final String CLIR_KEY = "clir_key";
    public static final String CS_FALLBACK = "cs_fallback";
    public static final String DATA_DISABLED_ON_BOOT_KEY = "disabled_on_boot_key";
    private static final int DEFAULT_REPORT_INTERVAL_MS = 200;
    private static final String DNS_SERVER_CHECK_DISABLED_KEY = "dns_server_check_disabled_key";
    protected static final int EVENT_CALL_RING = 14;
    private static final int EVENT_CALL_RING_CONTINUE = 15;
    protected static final int EVENT_CARRIER_CONFIG_CHANGED = 43;
    protected static final int EVENT_CDMA_SUBSCRIPTION_SOURCE_CHANGED = 27;
    protected static final int EVENT_CFU_IND = 101;
    protected static final int EVENT_CFU_QUERY_TIMEOUT = 102;
    protected static final int EVENT_CHARGING_STOP = 103;
    private static final int EVENT_CHECK_FOR_NETWORK_AUTOMATIC = 38;
    protected static final int EVENT_CIPHER_INDICATION = 1000;
    private static final int EVENT_CONFIG_LCE = 37;
    protected static final int EVENT_CRSS_IND = 1003;
    protected static final int EVENT_EMERGENCY_CALLBACK_MODE_ENTER = 25;
    protected static final int EVENT_EXIT_EMERGENCY_CALLBACK_RESPONSE = 26;
    protected static final int EVENT_GET_BASEBAND_VERSION_DONE = 6;
    protected static final int EVENT_GET_CALL_FORWARD_DONE = 13;
    protected static final int EVENT_GET_CALL_FORWARD_TIME_SLOT_DONE = 109;
    protected static final int EVENT_GET_CALL_WAITING_DONE = 301;
    protected static final int EVENT_GET_DEVICE_IDENTITY_DONE = 21;
    protected static final int EVENT_GET_IMEISV_DONE = 10;
    protected static final int EVENT_GET_IMEI_DONE = 9;
    protected static final int EVENT_GET_RADIO_CAPABILITY = 35;
    private static final int EVENT_GET_SIM_STATUS_DONE = 11;
    private static final int EVENT_ICC_CHANGED = 30;
    protected static final int EVENT_ICC_RECORD_EVENTS = 29;
    protected static final int EVENT_IMS_UT_CSFB = 2001;
    protected static final int EVENT_IMS_UT_DONE = 2000;
    private static final int EVENT_INITIATE_SILENT_REDIAL = 32;
    protected static final int EVENT_LAST = 43;
    private static final int EVENT_MMI_DONE = 4;
    protected static final int EVENT_MTK_BASE = 1000;
    protected static final int EVENT_NV_READY = 23;
    protected static final int EVENT_QUERY_CFU = 2002;
    protected static final int EVENT_RADIO_AVAILABLE = 1;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 33;
    protected static final int EVENT_RADIO_OFF_OR_NOT_AVAILABLE = 8;
    protected static final int EVENT_RADIO_ON = 5;
    protected static final int EVENT_REGISTERED_TO_NETWORK = 19;
    protected static final int EVENT_REQUEST_VOICE_RADIO_TECH_DONE = 40;
    protected static final int EVENT_RIL_CONNECTED = 41;
    protected static final int EVENT_RUIM_RECORDS_LOADED = 22;
    protected static final int EVENT_SET_CALL_FORWARD_DONE = 12;
    protected static final int EVENT_SET_CALL_FORWARD_TIME_SLOT_DONE = 110;
    protected static final int EVENT_SET_CALL_WAITING_DONE = 302;
    protected static final int EVENT_SET_CLIR_COMPLETE = 18;
    private static final int EVENT_SET_ENHANCED_VP = 24;
    protected static final int EVENT_SET_NETWORK_AUTOMATIC = 28;
    private static final int EVENT_SET_NETWORK_AUTOMATIC_COMPLETE = 17;
    private static final int EVENT_SET_NETWORK_MANUAL_COMPLETE = 16;
    protected static final int EVENT_SET_ROAMING_PREFERENCE_DONE = 44;
    protected static final int EVENT_SET_VM_NUMBER_DONE = 20;
    protected static final int EVENT_SIM_RECORDS_LOADED = 3;
    protected static final int EVENT_SPEECH_CODEC_INFO = 1001;
    private static final int EVENT_SRVCC_STATE_CHANGED = 31;
    protected static final int EVENT_SS = 36;
    protected static final int EVENT_SSN = 2;
    private static final int EVENT_UNSOL_OEM_HOOK_RAW = 34;
    protected static final int EVENT_UNSOL_RADIO_CAPABILITY_CHANGED = 111;
    protected static final int EVENT_UPDATE_PHONE_OBJECT = 42;
    protected static final int EVENT_USSD = 7;
    protected static final int EVENT_USSI_CSFB = 2003;
    protected static final int EVENT_VOICE_CALL_INCOMING_INDICATION = 1002;
    protected static final int EVENT_VOICE_RADIO_TECH_CHANGED = 39;
    public static final String EXTRA_KEY_ALERT_MESSAGE = "alertMessage";
    public static final String EXTRA_KEY_ALERT_SHOW = "alertShow";
    public static final String EXTRA_KEY_ALERT_TITLE = "alertTitle";
    protected static final String EXTRA_KEY_NOTIFICATION_MESSAGE = "notificationMessage";
    private static final String GSM_NON_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_non_roaming_list_";
    private static final String GSM_ROAMING_LIST_OVERRIDE_PREFIX = "gsm_roaming_list_";
    private static final boolean LCE_PULL_MODE = true;
    private static final String LOG_TAG = "Phone";
    public static final String LTE_INDICATOR = "4G";
    public static final String NETWORK_SELECTION_KEY = "network_selection_key";
    public static final String NETWORK_SELECTION_NAME_KEY = "network_selection_name_key";
    public static final String NETWORK_SELECTION_SHORT_KEY = "network_selection_short_key";
    public static final String UTRAN_INDICATOR = "3G";
    private static final String VM_COUNT = "vm_count_key";
    private static final String VM_ID = "vm_id_key";
    protected static final Object lockForRadioTechnologyChange = new Object();
    private final String mActionAttached;
    private final String mActionDetached;
    private int mCSFallbackMode;
    private int mCallRingContinueToken;
    private int mCallRingDelay;
    protected final RegistrantList mCdmaCallAcceptedRegistrants;
    public CommandsInterface mCi;
    protected final RegistrantList mCipherIndicationRegistrants;
    protected final Context mContext;
    public DcTracker mDcTracker;
    protected final RegistrantList mDisconnectRegistrants;
    private boolean mDnsCheckDisabled;
    private boolean mDoesRilSendMultipleCallRing;
    protected final RegistrantList mEmergencyCallToggledRegistrants;
    private final RegistrantList mHandoverRegistrants;
    protected final AtomicReference<IccRecords> mIccRecords;
    private BroadcastReceiver mImsIntentReceiver;
    protected Phone mImsPhone;
    private boolean mImsServiceReady;
    private final RegistrantList mIncomingRingRegistrants;
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
    private final RegistrantList mServiceStateRegistrants;
    protected final RegistrantList mSimRecordsLoadedRegistrants;
    protected SimulatedRadioControl mSimulatedRadioControl;
    public SmsStorageMonitor mSmsStorageMonitor;
    public SmsUsageMonitor mSmsUsageMonitor;
    private final RegistrantList mSpeechCodecInfoRegistrants;
    protected final RegistrantList mSuppServiceFailedRegistrants;
    protected TelephonyComponentFactory mTelephonyComponentFactory;
    private TelephonyTester mTelephonyTester;
    protected AtomicReference<UiccCardApplication> mUiccApplication;
    protected UiccController mUiccController;
    private boolean mUnitTestMode;
    protected final RegistrantList mUnknownConnectionRegistrants;
    private final RegistrantList mVideoCapabilityChangedRegistrants;
    protected int mVmCount;
    protected final RegistrantList mVoiceCallIncomingIndicationRegistrants;

    public abstract int getPhoneType();

    public abstract PhoneConstants.State getState();

    protected abstract void onUpdateIccAvailability();

    public abstract void sendEmergencyCallStateChange(boolean z);

    public abstract void setBroadcastEmergencyCallStateChanges(boolean z);

    private static class NetworkSelectMessage {
        public Message message;
        public String operatorAlphaLong;
        public String operatorAlphaShort;
        public String operatorNumeric;

        NetworkSelectMessage(NetworkSelectMessage networkSelectMessage) {
            this();
        }

        private NetworkSelectMessage() {
        }
    }

    public IccRecords getIccRecords() {
        return this.mIccRecords.get();
    }

    public String getPhoneName() {
        return this.mName;
    }

    protected void setPhoneName(String name) {
        this.mName = name;
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

    public void setSystemProperty(String property, String value) {
        if (getUnitTestMode()) {
            return;
        }
        SystemProperties.set(property, value);
    }

    public String getSystemProperty(String property, String defValue) {
        if (getUnitTestMode()) {
            return null;
        }
        return SystemProperties.get(property, defValue);
    }

    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode) {
        this(name, notifier, context, ci, unitTestMode, Integer.MAX_VALUE, TelephonyComponentFactory.getInstance());
    }

    protected Phone(String name, PhoneNotifier notifier, Context context, CommandsInterface ci, boolean unitTestMode, int phoneId, TelephonyComponentFactory telephonyComponentFactory) {
        this.mImsIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: action " + intent.getAction());
                if (intent.hasExtra("android:phone_id")) {
                    int extraPhoneId = intent.getIntExtra("android:phone_id", -1);
                    Rlog.d(Phone.LOG_TAG, "mImsIntentReceiver: extraPhoneId = " + extraPhoneId);
                    if (extraPhoneId == -1 || extraPhoneId != Phone.this.getPhoneId()) {
                        return;
                    }
                }
                synchronized (Phone.lockForRadioTechnologyChange) {
                    Rlog.w(Phone.LOG_TAG, intent.getAction() + ", getSubId=" + Phone.this.getSubId() + ", getPhoneId=" + Phone.this.getPhoneId());
                    if (intent.getAction().equals("com.android.ims.IMS_SERVICE_UP")) {
                        if (SystemProperties.getInt("persist.ims.simulate", 0) == 1 && SystemProperties.getInt("persist.ims.phoneid", 0) != Phone.this.getPhoneId()) {
                            return;
                        }
                        Phone.this.mImsServiceReady = true;
                        Phone.this.updateImsPhone();
                        ImsManager.updateImsServiceConfig(Phone.this.mContext, Phone.this.mPhoneId, false);
                    } else if (intent.getAction().equals("com.android.ims.IMS_SERVICE_DOWN")) {
                        Phone.this.mImsServiceReady = false;
                        Phone.this.updateImsPhone();
                    }
                }
            }
        };
        this.mCSFallbackMode = 0;
        this.mVmCount = 0;
        this.mIsVoiceCapable = true;
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
        this.mCipherIndicationRegistrants = new RegistrantList();
        this.mSpeechCodecInfoRegistrants = new RegistrantList();
        this.mCdmaCallAcceptedRegistrants = new RegistrantList();
        this.mVoiceCallIncomingIndicationRegistrants = new RegistrantList();
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
        this.mIsVoiceCapable = this.mContext.getResources().getBoolean(R.^attr-private.frameDuration);
        this.mDoesRilSendMultipleCallRing = SystemProperties.getBoolean("ro.telephony.call_ring.multiple", true);
        Rlog.d(LOG_TAG, "mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        this.mCallRingDelay = SystemProperties.getInt("ro.telephony.call_ring.delay", 3000);
        Rlog.d(LOG_TAG, "mCallRingDelay=" + this.mCallRingDelay);
        if (getPhoneType() == 5) {
            return;
        }
        Locale carrierLocale = getLocaleFromCarrierProperties(this.mContext);
        if (carrierLocale != null && !TextUtils.isEmpty(carrierLocale.getCountry())) {
            String country = carrierLocale.getCountry();
            try {
                Settings.Global.getInt(this.mContext.getContentResolver(), "wifi_country_code");
            } catch (Settings.SettingNotFoundException e) {
                WifiManager wM = (WifiManager) this.mContext.getSystemService("wifi");
                wM.setCountryCode(country, false);
            }
        }
        this.mTelephonyComponentFactory = telephonyComponentFactory;
        this.mSmsStorageMonitor = this.mTelephonyComponentFactory.makeSmsStorageMonitor(this);
        this.mSmsUsageMonitor = this.mTelephonyComponentFactory.makeSmsUsageMonitor(context);
        this.mUiccController = UiccController.getInstance();
        this.mUiccController.registerForIccChanged(this, 30, null);
        if (getPhoneType() != 3) {
            this.mCi.registerForSrvccStateChanged(this, 31, null);
        }
        this.mCi.setOnUnsolOemHookRaw(this, 34, null);
        this.mCi.startLceService(200, true, obtainMessage(37));
        this.mCi.registerForCipherIndication(this, 1000, null);
        this.mCi.setOnSpeechCodecInfo(this, 1001, null);
        this.mRadioCapability.set(this.mCi.getBootupRadioCapability());
        this.mCi.registerForRadioCapabilityChanged(this, 111, null);
    }

    public void startMonitoringImsService() {
        if (getPhoneType() == 3) {
            return;
        }
        synchronized (lockForRadioTechnologyChange) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.ims.IMS_SERVICE_UP");
            filter.addAction("com.android.ims.IMS_SERVICE_DOWN");
            this.mContext.registerReceiver(this.mImsIntentReceiver, filter);
            ImsManager imsManager = ImsManager.getInstance(this.mContext, getPhoneId());
            if (imsManager != null && imsManager.isServiceAvailable()) {
                this.mImsServiceReady = true;
                updateImsPhone();
                ImsManager.updateImsServiceConfig(this.mContext, this.mPhoneId, false);
            }
        }
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 16:
                if (((AsyncResult) msg.obj).exception != null) {
                    clearSavedNetworkSelection();
                }
                break;
            case 17:
                break;
            default:
                switch (msg.what) {
                    case 14:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING Received state=" + getState());
                        if (((AsyncResult) msg.obj).exception != null) {
                            return;
                        }
                        PhoneConstants.State state = getState();
                        if (!this.mDoesRilSendMultipleCallRing && (state == PhoneConstants.State.RINGING || state == PhoneConstants.State.IDLE)) {
                            this.mCallRingContinueToken++;
                            sendIncomingCallRingNotification(this.mCallRingContinueToken);
                            return;
                        } else {
                            notifyIncomingRing();
                            return;
                        }
                    case 15:
                        Rlog.d(LOG_TAG, "Event EVENT_CALL_RING_CONTINUE Received state=" + getState());
                        if (getState() != PhoneConstants.State.RINGING) {
                            return;
                        }
                        sendIncomingCallRingNotification(msg.arg1);
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
                        if (ar2.exception != null || ar2.result == null) {
                            return;
                        }
                        String dialString = (String) ar2.result;
                        if (TextUtils.isEmpty(dialString)) {
                            return;
                        }
                        try {
                            dialInternal(dialString, null, 0, null);
                            return;
                        } catch (CallStateException e) {
                            Rlog.e(LOG_TAG, "silent redial failed: " + e);
                            return;
                        }
                    case 34:
                        AsyncResult ar3 = (AsyncResult) msg.obj;
                        if (ar3.exception == null) {
                            byte[] data = (byte[]) ar3.result;
                            this.mNotifier.notifyOemHookRawEventForSubscriber(getSubId(), data);
                            return;
                        } else {
                            Rlog.e(LOG_TAG, "OEM hook raw exception: " + ar3.exception);
                            return;
                        }
                    case 37:
                        AsyncResult ar4 = (AsyncResult) msg.obj;
                        if (ar4.exception != null) {
                            Rlog.d(LOG_TAG, "config LCE service failed: " + ar4.exception);
                            return;
                        } else {
                            ArrayList<Integer> statusInfo = (ArrayList) ar4.result;
                            this.mLceStatus = statusInfo.get(0).intValue();
                            return;
                        }
                    case 38:
                        onCheckForNetworkSelectionModeAutomatic(msg);
                        return;
                    case EVENT_CHARGING_STOP:
                        String[] strArr = new String[2];
                        Rlog.d(LOG_TAG, "send special AT cmd to MD");
                        String[] sFun = {"AT+EFUN=1", UsimPBMemInfo.STRING_NOT_SET};
                        invokeOemRilRequestStrings(sFun, null);
                        String[] s = {"AT+ERFTX=1,0,0," + SystemProperties.get("persist.radio.charging_stop", "40"), UsimPBMemInfo.STRING_NOT_SET};
                        invokeOemRilRequestStrings(s, null);
                        return;
                    case 111:
                        AsyncResult ar5 = (AsyncResult) msg.obj;
                        RadioCapability rc_unsol = (RadioCapability) ar5.result;
                        if (ar5.exception != null) {
                            Rlog.d(LOG_TAG, "RIL_UNSOL_RADIO_CAPABILITY fail,no need to change mRadioCapability");
                        } else {
                            this.mRadioCapability.set(rc_unsol);
                        }
                        Rlog.d(LOG_TAG, "EVENT_UNSOL_RADIO_CAPABILITY_CHANGED :phone rc : " + rc_unsol);
                        return;
                    case 1001:
                        AsyncResult ar6 = (AsyncResult) msg.obj;
                        Rlog.d(LOG_TAG, "handle EVENT_SPEECH_CODEC_INFO : " + ((int[]) ar6.result)[0]);
                        notifySpeechCodecInfo(((int[]) ar6.result)[0]);
                        return;
                    default:
                        Rlog.d(LOG_TAG, "Unexpected EVENT id = " + msg.what);
                        throw new RuntimeException("unexpected event not handled");
                }
        }
        handleSetSelectNetwork((AsyncResult) msg.obj);
    }

    public ArrayList<Connection> getHandoverConnection() {
        return null;
    }

    public void notifySrvccState(Call.SrvccState state) {
    }

    public void registerForSilentRedial(Handler h, int what, Object obj) {
    }

    public void unregisterForSilentRedial(Handler h) {
    }

    private void handleSrvccStateChanged(int[] ret) {
        Call.SrvccState srvccState;
        Rlog.d(LOG_TAG, "handleSrvccStateChanged");
        ArrayList<Connection> conn = null;
        Phone imsPhone = this.mImsPhone;
        Call.SrvccState srvccState2 = Call.SrvccState.NONE;
        if (ret == null || ret.length == 0) {
            return;
        }
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

    public Context getContext() {
        return this.mContext;
    }

    public void disableDnsCheck(boolean b) {
        this.mDnsCheckDisabled = b;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(DNS_SERVER_CHECK_DISABLED_KEY, b);
        editor.apply();
    }

    public boolean isDnsCheckDisabled() {
        return this.mDnsCheckDisabled;
    }

    public void registerForPreciseCallStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mPreciseCallStateRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForPreciseCallStateChanged(Handler h) {
        this.mPreciseCallStateRegistrants.remove(h);
    }

    protected void notifyPreciseCallStateChangedP() {
        AsyncResult ar = new AsyncResult((Object) null, this, (Throwable) null);
        this.mPreciseCallStateRegistrants.notifyRegistrants(ar);
        this.mNotifier.notifyPreciseCallState(this);
    }

    public void registerForHandoverStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mHandoverRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForHandoverStateChanged(Handler h) {
        this.mHandoverRegistrants.remove(h);
    }

    public void notifyHandoverStateChanged(Connection cn) {
        AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
        this.mHandoverRegistrants.notifyRegistrants(ar);
    }

    protected void setIsInEmergencyCall() {
    }

    protected void migrateFrom(Phone from) {
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
        if (!from.isInEmergencyCall()) {
            return;
        }
        setIsInEmergencyCall();
    }

    protected void migrate(RegistrantList to, RegistrantList from) {
        from.removeCleared();
        int n = from.size();
        for (int i = 0; i < n; i++) {
            Registrant r = (Registrant) from.get(i);
            Message msg = r.messageForRegistrant();
            if (msg != null) {
                if (msg.obj != CallManager.getInstance().getRegistrantIdentifier()) {
                    to.add((Registrant) from.get(i));
                }
            } else {
                Rlog.d(LOG_TAG, "msg is null");
            }
        }
    }

    public void registerForUnknownConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mUnknownConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForUnknownConnection(Handler h) {
        this.mUnknownConnectionRegistrants.remove(h);
    }

    public void registerForNewRingingConnection(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mNewRingingConnectionRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForNewRingingConnection(Handler h) {
        this.mNewRingingConnectionRegistrants.remove(h);
    }

    public void registerForVideoCapabilityChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mVideoCapabilityChangedRegistrants.addUnique(h, what, obj);
        notifyForVideoCapabilityChanged(this.mIsVideoCapable);
    }

    public void unregisterForVideoCapabilityChanged(Handler h) {
        this.mVideoCapabilityChangedRegistrants.remove(h);
    }

    public void registerForInCallVoicePrivacyOn(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOn(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOn(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOn(h);
    }

    public void registerForInCallVoicePrivacyOff(Handler h, int what, Object obj) {
        this.mCi.registerForInCallVoicePrivacyOff(h, what, obj);
    }

    public void unregisterForInCallVoicePrivacyOff(Handler h) {
        this.mCi.unregisterForInCallVoicePrivacyOff(h);
    }

    public void registerForIncomingRing(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mIncomingRingRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForIncomingRing(Handler h) {
        this.mIncomingRingRegistrants.remove(h);
    }

    public void registerForDisconnect(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mDisconnectRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForDisconnect(Handler h) {
        this.mDisconnectRegistrants.remove(h);
    }

    public void registerForSuppServiceFailed(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mSuppServiceFailedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSuppServiceFailed(Handler h) {
        this.mSuppServiceFailedRegistrants.remove(h);
    }

    public void registerForMmiInitiate(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiInitiate(Handler h) {
        this.mMmiRegistrants.remove(h);
    }

    public void registerForMmiComplete(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForMmiComplete(Handler h) {
        checkCorrectThread(h);
        this.mMmiCompleteRegistrants.remove(h);
    }

    public void registerForSimRecordsLoaded(Handler h, int what, Object obj) {
    }

    public void unregisterForSimRecordsLoaded(Handler h) {
    }

    public void registerForTtyModeReceived(Handler h, int what, Object obj) {
    }

    public void unregisterForTtyModeReceived(Handler h) {
    }

    public void setNetworkSelectionModeAutomatic(Message response) {
        Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic, querying current mode");
        Message msg = obtainMessage(38);
        msg.obj = response;
        this.mCi.getNetworkSelectionMode(msg);
    }

    private void onCheckForNetworkSelectionModeAutomatic(Message fromRil) {
        NetworkSelectMessage networkSelectMessage = null;
        AsyncResult ar = (AsyncResult) fromRil.obj;
        Message response = (Message) ar.userObj;
        boolean doAutomatic = true;
        if (ar.exception == null && ar.result != null) {
            try {
                int[] modes = (int[]) ar.result;
                if (modes[0] == 0) {
                    doAutomatic = false;
                }
            } catch (Exception e) {
            }
        }
        NetworkSelectMessage nsm = new NetworkSelectMessage(networkSelectMessage);
        nsm.message = response;
        nsm.operatorNumeric = UsimPBMemInfo.STRING_NOT_SET;
        nsm.operatorAlphaLong = UsimPBMemInfo.STRING_NOT_SET;
        nsm.operatorAlphaShort = UsimPBMemInfo.STRING_NOT_SET;
        if (doAutomatic) {
            Message msg = obtainMessage(17, nsm);
            this.mCi.setNetworkSelectionModeAutomatic(msg);
        } else {
            Rlog.d(LOG_TAG, "setNetworkSelectionModeAutomatic - already auto, ignoring");
            ar.userObj = nsm;
            handleSetSelectNetwork(ar);
        }
        updateSavedNetworkOperator(nsm);
    }

    public void getNetworkSelectionMode(Message message) {
        this.mCi.getNetworkSelectionMode(message);
    }

    public void selectNetworkManually(OperatorInfo network, boolean persistSelection, Message response) {
        NetworkSelectMessage nsm = new NetworkSelectMessage(null);
        nsm.message = response;
        nsm.operatorNumeric = network.getOperatorNumeric();
        nsm.operatorAlphaLong = network.getOperatorAlphaLong();
        nsm.operatorAlphaShort = network.getOperatorAlphaShort();
        Message msg = obtainMessage(16, nsm);
        if (getPhoneName().equals("GSM")) {
            Rlog.d(LOG_TAG, "GSMPhone selectNetworkManuallyWithAct:" + network);
            String actype = "0";
            if (network.getOperatorAlphaLong() != null && network.getOperatorAlphaLong().endsWith(UTRAN_INDICATOR)) {
                actype = ACT_TYPE_UTRAN;
            } else if (network.getOperatorAlphaLong() != null && network.getOperatorAlphaLong().endsWith(LTE_INDICATOR)) {
                actype = ACT_TYPE_LTE;
            }
            this.mCi.setNetworkSelectionModeManualWithAct(network.getOperatorNumeric(), actype, msg);
        } else {
            this.mCi.setNetworkSelectionModeManual(network.getOperatorNumeric(), msg);
        }
        if (persistSelection) {
            updateSavedNetworkOperator(nsm);
        } else {
            clearSavedNetworkSelection();
        }
    }

    public void registerForEmergencyCallToggle(Handler h, int what, Object obj) {
        Registrant r = new Registrant(h, what, obj);
        this.mEmergencyCallToggledRegistrants.add(r);
    }

    public void unregisterForEmergencyCallToggle(Handler h) {
        this.mEmergencyCallToggledRegistrants.remove(h);
    }

    private void updateSavedNetworkOperator(NetworkSelectMessage nsm) {
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(NETWORK_SELECTION_KEY + subId, nsm.operatorNumeric);
            editor.putString(NETWORK_SELECTION_NAME_KEY + subId, nsm.operatorAlphaLong);
            editor.putString(NETWORK_SELECTION_SHORT_KEY + subId, nsm.operatorAlphaShort);
            if (editor.commit()) {
                return;
            }
            Rlog.e(LOG_TAG, "failed to commit network selection preference");
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
        if (nsm.message == null) {
            return;
        }
        AsyncResult.forMessage(nsm.message, ar.result, ar.exception);
        nsm.message.sendToTarget();
    }

    private OperatorInfo getSavedNetworkSelection() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        String numeric = sp.getString(NETWORK_SELECTION_KEY + getSubId(), UsimPBMemInfo.STRING_NOT_SET);
        String name = sp.getString(NETWORK_SELECTION_NAME_KEY + getSubId(), UsimPBMemInfo.STRING_NOT_SET);
        String shrt = sp.getString(NETWORK_SELECTION_SHORT_KEY + getSubId(), UsimPBMemInfo.STRING_NOT_SET);
        return new OperatorInfo(name, shrt, numeric);
    }

    private void clearSavedNetworkSelection() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().remove(NETWORK_SELECTION_KEY + getSubId()).remove(NETWORK_SELECTION_NAME_KEY + getSubId()).remove(NETWORK_SELECTION_SHORT_KEY + getSubId()).commit();
    }

    public void restoreSavedNetworkSelection(Message response) {
        OperatorInfo networkSelection = getSavedNetworkSelection();
        if (networkSelection == null || TextUtils.isEmpty(networkSelection.getOperatorNumeric())) {
            setNetworkSelectionModeAutomatic(response);
        } else {
            selectNetworkManually(networkSelection, true, response);
        }
    }

    public void saveClirSetting(int commandInterfaceCLIRMode) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(CLIR_KEY + getPhoneId(), commandInterfaceCLIRMode);
        if (editor.commit()) {
            return;
        }
        Rlog.e(LOG_TAG, "Failed to commit CLIR preference");
    }

    private void setUnitTestMode(boolean f) {
        this.mUnitTestMode = f;
    }

    public boolean getUnitTestMode() {
        return this.mUnitTestMode;
    }

    protected void notifyDisconnectP(Connection cn) {
        AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
        this.mDisconnectRegistrants.notifyRegistrants(ar);
    }

    public void registerForServiceStateChanged(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mServiceStateRegistrants.add(h, what, obj);
    }

    public void unregisterForServiceStateChanged(Handler h) {
        this.mServiceStateRegistrants.remove(h);
    }

    public void registerForRingbackTone(Handler h, int what, Object obj) {
        this.mCi.registerForRingbackTone(h, what, obj);
    }

    public void unregisterForRingbackTone(Handler h) {
        this.mCi.unregisterForRingbackTone(h);
    }

    public void registerForOnHoldTone(Handler h, int what, Object obj) {
    }

    public void unregisterForOnHoldTone(Handler h) {
    }

    public void registerForResendIncallMute(Handler h, int what, Object obj) {
        this.mCi.registerForResendIncallMute(h, what, obj);
    }

    public void unregisterForResendIncallMute(Handler h) {
        this.mCi.unregisterForResendIncallMute(h);
    }

    public void setEchoSuppressionEnabled() {
    }

    protected void notifyServiceStateChangedP(ServiceState ss) {
        AsyncResult ar = new AsyncResult((Object) null, ss, (Throwable) null);
        this.mServiceStateRegistrants.notifyRegistrants(ar);
        boolean isVideoCallCapable = isVideoEnabled();
        if (this.mIsVideoCapable != isVideoCallCapable) {
            notifyForVideoCapabilityChanged(isVideoCallCapable);
        }
        this.mNotifier.notifyServiceState(this);
        if (ss.getState() == 3 || !SystemProperties.get("ril.charging_stop_enable", "0").equals("1")) {
            return;
        }
        sendMessageDelayed(obtainMessage(EVENT_CHARGING_STOP, 0, 0), 60000L);
    }

    public SimulatedRadioControl getSimulatedRadioControl() {
        return this.mSimulatedRadioControl;
    }

    private void checkCorrectThread(Handler h) {
        if (h.getLooper() == this.mLooper) {
        } else {
            throw new RuntimeException("com.android.internal.telephony.Phone must be used from within one thread");
        }
    }

    private static Locale getLocaleFromCarrierProperties(Context ctx) {
        String carrier = SystemProperties.get("ro.carrier");
        if (carrier == null || carrier.length() == 0 || "unknown".equals(carrier)) {
            return null;
        }
        CharSequence[] carrierLocales = ctx.getResources().getTextArray(R.array.config_displayWhiteBalanceBaseThresholds);
        for (int i = 0; i < carrierLocales.length; i += 3) {
            String c = carrierLocales[i].toString();
            if (carrier.equals(c)) {
                return Locale.forLanguageTag(carrierLocales[i + 1].toString().replace('_', '-'));
            }
        }
        return null;
    }

    public IccFileHandler getIccFileHandler() {
        IccFileHandler iccFileHandler;
        UiccCardApplication uiccApplication = this.mUiccApplication.get();
        if (uiccApplication == null) {
            Rlog.d(LOG_TAG, "getIccFileHandler: uiccApplication == null, return null");
            iccFileHandler = null;
        } else {
            iccFileHandler = uiccApplication.getIccFileHandler();
        }
        Rlog.d(LOG_TAG, "getIccFileHandler: fh=" + iccFileHandler);
        return iccFileHandler;
    }

    public Handler getHandler() {
        return this;
    }

    public void updatePhoneObject(int voiceRadioTech) {
    }

    public ServiceStateTracker getServiceStateTracker() {
        return null;
    }

    public CallTracker getCallTracker() {
        return null;
    }

    public void updateVoiceMail() {
        Rlog.e(LOG_TAG, "updateVoiceMail() should be overridden");
    }

    public IccCardApplicationStatus.AppType getCurrentUiccAppType() {
        UiccCardApplication currentApp = this.mUiccApplication.get();
        if (currentApp != null) {
            return currentApp.getType();
        }
        return IccCardApplicationStatus.AppType.APPTYPE_UNKNOWN;
    }

    public IccCard getIccCard() {
        return null;
    }

    public String getIccSerialNumber() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getIccId();
        }
        return null;
    }

    public String getFullIccSerialNumber() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getFullIccId();
        }
        return null;
    }

    public boolean getIccRecordsLoaded() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getRecordsLoaded();
        }
        return false;
    }

    public List<CellInfo> getAllCellInfo() {
        List<CellInfo> cellInfoList = getServiceStateTracker().getAllCellInfo();
        return privatizeCellInfoList(cellInfoList);
    }

    private List<CellInfo> privatizeCellInfoList(List<CellInfo> cellInfoList) {
        if (cellInfoList == null) {
            return null;
        }
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

    public void setCellInfoListRate(int rateInMillis) {
        this.mCi.setCellInfoListRate(rateInMillis, null);
    }

    public boolean getMessageWaitingIndicator() {
        return this.mVmCount != 0;
    }

    private int getCallForwardingIndicatorFromSharedPref() {
        String subscriberId;
        int status = 0;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            status = sp.getInt(CF_STATUS + subId, -1);
            Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: for subId " + subId + "= " + status);
            if (status == -1 && (subscriberId = sp.getString(CF_ID, null)) != null) {
                String currentSubscriberId = getSubscriberId();
                if (subscriberId.equals(currentSubscriberId)) {
                    status = sp.getInt(CF_STATUS, 0);
                    setCallForwardingIndicatorInSharedPref(status == 1);
                    Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: " + status);
                } else {
                    Rlog.d(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: returning DISABLED as status for matching subscriberId not found");
                }
                SharedPreferences.Editor editor = sp.edit();
                editor.remove(CF_ID);
                editor.remove(CF_STATUS);
                editor.apply();
            }
        } else {
            Rlog.e(LOG_TAG, "getCallForwardingIndicatorFromSharedPref: invalid subId " + subId);
        }
        return status;
    }

    private void setCallForwardingIndicatorInSharedPref(boolean enable) {
        int status = enable ? 1 : 0;
        int subId = getSubId();
        Rlog.d(LOG_TAG, "setCallForwardingIndicatorInSharedPref: Storing status = " + status + " in pref " + CF_STATUS + subId);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(CF_STATUS + subId, status);
        editor.apply();
    }

    public void setVoiceCallForwardingFlag(int line, boolean enable, String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            r.setVoiceCallForwardingFlag(line, enable, number);
        } else {
            Rlog.d(LOG_TAG, "IccRecords is null, skip set CFU icon.");
        }
    }

    protected void setVoiceCallForwardingFlag(IccRecords r, int line, boolean enable, String number) {
        setCallForwardingIndicatorInSharedPref(enable);
        r.setVoiceCallForwardingFlag(line, enable, number);
    }

    public boolean getCallForwardingIndicator() {
        if (getPhoneType() == 2) {
            Rlog.e(LOG_TAG, "getCallForwardingIndicator: not possible in CDMA");
            return false;
        }
        IccRecords r = this.mIccRecords.get();
        int callForwardingIndicator = -1;
        if (r != null) {
            callForwardingIndicator = r.getVoiceCallForwardingFlag();
        }
        if (callForwardingIndicator == -1) {
            callForwardingIndicator = getCallForwardingIndicatorFromSharedPref();
        }
        return callForwardingIndicator == 1;
    }

    public void queryCdmaRoamingPreference(Message response) {
        this.mCi.queryCdmaRoamingPreference(response);
    }

    public SignalStrength getSignalStrength() {
        ServiceStateTracker sst = getServiceStateTracker();
        if (sst == null) {
            return new SignalStrength();
        }
        return sst.getSignalStrength();
    }

    public void setCdmaRoamingPreference(int cdmaRoamingType, Message response) {
        this.mCi.setCdmaRoamingPreference(cdmaRoamingType, response);
    }

    public void setCdmaSubscription(int cdmaSubscriptionType, Message response) {
        this.mCi.setCdmaSubscriptionSource(cdmaSubscriptionType, response);
    }

    public void setPreferredNetworkType(int networkType, Message response) {
        if (getPhoneType() == 2 || getPhoneType() == 6) {
            this.mCi.setPreferredNetworkType(networkType, response);
            return;
        }
        int modemRaf = getRadioAccessFamily();
        int rafFromType = RadioAccessFamily.getRafFromNetworkType(networkType);
        if (modemRaf != 1 && rafFromType != 1) {
            int filteredRaf = rafFromType & modemRaf;
            int filteredType = RadioAccessFamily.getNetworkTypeFromRaf(filteredRaf);
            Rlog.d(LOG_TAG, "setPreferredNetworkType: networkType = " + networkType + " modemRaf = " + modemRaf + " rafFromType = " + rafFromType + " filteredType = " + filteredType);
            this.mCi.setPreferredNetworkType(filteredType, response);
            return;
        }
        Rlog.d(LOG_TAG, "setPreferredNetworkType: Abort, unknown RAF: " + modemRaf + " " + rafFromType);
        if (response != null) {
            CommandException ex = new CommandException(CommandException.Error.GENERIC_FAILURE);
            AsyncResult.forMessage(response, (Object) null, ex);
            response.sendToTarget();
        }
    }

    public void getPreferredNetworkType(Message response) {
        this.mCi.getPreferredNetworkType(response);
    }

    public void getSmscAddress(Message result) {
        this.mCi.getSmscAddress(result);
    }

    public void setSmscAddress(String address, Message result) {
        this.mCi.setSmscAddress(address, result);
    }

    public void setTTYMode(int ttyMode, Message onComplete) {
        this.mCi.setTTYMode(ttyMode, onComplete);
    }

    public void setUiTTYMode(int uiTtyMode, Message onComplete) {
        Rlog.d(LOG_TAG, "unexpected setUiTTYMode method call");
    }

    public void queryTTYMode(Message onComplete) {
        this.mCi.queryTTYMode(onComplete);
    }

    public void enableEnhancedVoicePrivacy(boolean enable, Message onComplete) {
    }

    public void getEnhancedVoicePrivacy(Message onComplete) {
    }

    public void setBandMode(int bandMode, Message response) {
        this.mCi.setBandMode(bandMode, response);
    }

    public void queryAvailableBandMode(Message response) {
        this.mCi.queryAvailableBandMode(response);
    }

    public void invokeOemRilRequestRaw(byte[] data, Message response) {
        this.mCi.invokeOemRilRequestRaw(data, response);
    }

    public void invokeOemRilRequestStrings(String[] strings, Message response) {
        this.mCi.invokeOemRilRequestStrings(strings, response);
    }

    public void nvReadItem(int itemID, Message response) {
        this.mCi.nvReadItem(itemID, response);
    }

    public void nvWriteItem(int itemID, String itemValue, Message response) {
        this.mCi.nvWriteItem(itemID, itemValue, response);
    }

    public void nvWriteCdmaPrl(byte[] preferredRoamingList, Message response) {
        this.mCi.nvWriteCdmaPrl(preferredRoamingList, response);
    }

    public void nvResetConfig(int resetType, Message response) {
        this.mCi.nvResetConfig(resetType, response);
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

    public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) {
        this.mNotifier.notifyVoLteServiceStateChanged(this, lteState);
    }

    public boolean isInEmergencyCall() {
        return false;
    }

    public boolean isInEcm() {
        return false;
    }

    private static int getVideoState(Call call) {
        Connection conn = call.getEarliestConnection();
        if (conn == null) {
            return 0;
        }
        int videoState = conn.getVideoState();
        return videoState;
    }

    private boolean isVideoCall(Call call) {
        int videoState = getVideoState(call);
        return VideoProfile.isVideo(videoState);
    }

    public boolean isVideoCallPresent() {
        boolean isVideoCallActive = false;
        if (this.mImsPhone != null) {
            if (isVideoCall(this.mImsPhone.getForegroundCall()) || isVideoCall(this.mImsPhone.getBackgroundCall())) {
                isVideoCallActive = true;
            } else {
                isVideoCallActive = isVideoCall(this.mImsPhone.getRingingCall());
            }
        }
        Rlog.d(LOG_TAG, "isVideoCallActive: " + isVideoCallActive);
        return isVideoCallActive;
    }

    public int getVoiceMessageCount() {
        return this.mVmCount;
    }

    public void setVoiceMessageCount(int countWaiting) {
        this.mVmCount = countWaiting;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            Rlog.d(LOG_TAG, "setVoiceMessageCount: Storing Voice Mail Count = " + countWaiting + " for mVmCountKey = " + VM_COUNT + subId + " in preferences.");
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            SharedPreferences.Editor editor = sp.edit();
            editor.putInt(VM_COUNT + subId, countWaiting);
            editor.apply();
        } else {
            Rlog.e(LOG_TAG, "setVoiceMessageCount in sharedPreference: invalid subId " + subId);
        }
        notifyMessageWaitingIndicator();
    }

    protected int getStoredVoiceMessageCount() {
        int countVoiceMessages = 0;
        int subId = getSubId();
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
            int countFromSP = sp.getInt(VM_COUNT + subId, -2);
            if (countFromSP != -2) {
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference for subId " + subId + "= " + countFromSP);
                return countFromSP;
            }
            String subscriberId = sp.getString(VM_ID, null);
            if (subscriberId == null) {
                return 0;
            }
            String currentSubscriberId = getSubscriberId();
            if (currentSubscriberId != null && currentSubscriberId.equals(subscriberId)) {
                countVoiceMessages = sp.getInt(VM_COUNT, 0);
                setVoiceMessageCount(countVoiceMessages);
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: from preference = " + countVoiceMessages);
            } else {
                Rlog.d(LOG_TAG, "getStoredVoiceMessageCount: returning 0 as count for matching subscriberId not found");
            }
            SharedPreferences.Editor editor = sp.edit();
            editor.remove(VM_ID);
            editor.remove(VM_COUNT);
            editor.apply();
            return countVoiceMessages;
        }
        Rlog.e(LOG_TAG, "getStoredVoiceMessageCount: invalid subId " + subId);
        return 0;
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

    public void sendBurstDtmf(String dtmfString, int on, int off, Message onComplete) {
    }

    public void setOnPostDialCharacter(Handler h, int what, Object obj) {
        this.mPostDialHandler = new Registrant(h, what, obj);
    }

    public Registrant getPostDialHandler() {
        return this.mPostDialHandler;
    }

    public void exitEmergencyCallbackMode() {
    }

    public void registerForCdmaOtaStatusChange(Handler h, int what, Object obj) {
    }

    public void unregisterForCdmaOtaStatusChange(Handler h) {
    }

    public void registerForSubscriptionInfoReady(Handler h, int what, Object obj) {
    }

    public void unregisterForSubscriptionInfoReady(Handler h) {
    }

    public boolean needsOtaServiceProvisioning() {
        return false;
    }

    public boolean isOtaSpNumber(String dialStr) {
        return false;
    }

    public void registerForCallWaiting(Handler h, int what, Object obj) {
    }

    public void unregisterForCallWaiting(Handler h) {
    }

    public void registerForEcmTimerReset(Handler h, int what, Object obj) {
    }

    public void unregisterForEcmTimerReset(Handler h) {
    }

    public void registerForSignalInfo(Handler h, int what, Object obj) {
        this.mCi.registerForSignalInfo(h, what, obj);
    }

    public void unregisterForSignalInfo(Handler h) {
        this.mCi.unregisterForSignalInfo(h);
    }

    public void registerForDisplayInfo(Handler h, int what, Object obj) {
        this.mCi.registerForDisplayInfo(h, what, obj);
    }

    public void unregisterForDisplayInfo(Handler h) {
        this.mCi.unregisterForDisplayInfo(h);
    }

    public void registerForNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForNumberInfo(h, what, obj);
    }

    public void unregisterForNumberInfo(Handler h) {
        this.mCi.unregisterForNumberInfo(h);
    }

    public void registerForRedirectedNumberInfo(Handler h, int what, Object obj) {
        this.mCi.registerForRedirectedNumberInfo(h, what, obj);
    }

    public void unregisterForRedirectedNumberInfo(Handler h) {
        this.mCi.unregisterForRedirectedNumberInfo(h);
    }

    public void registerForLineControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForLineControlInfo(h, what, obj);
    }

    public void unregisterForLineControlInfo(Handler h) {
        this.mCi.unregisterForLineControlInfo(h);
    }

    public void registerFoT53ClirlInfo(Handler h, int what, Object obj) {
        this.mCi.registerFoT53ClirlInfo(h, what, obj);
    }

    public void unregisterForT53ClirInfo(Handler h) {
        this.mCi.unregisterForT53ClirInfo(h);
    }

    public void registerForT53AudioControlInfo(Handler h, int what, Object obj) {
        this.mCi.registerForT53AudioControlInfo(h, what, obj);
    }

    public void unregisterForT53AudioControlInfo(Handler h) {
        this.mCi.unregisterForT53AudioControlInfo(h);
    }

    public void setOnEcbModeExitResponse(Handler h, int what, Object obj) {
    }

    public void unsetOnEcbModeExitResponse(Handler h) {
    }

    public void registerForRadioOffOrNotAvailable(Handler h, int what, Object obj) {
        this.mRadioOffOrNotAvailableRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForRadioOffOrNotAvailable(Handler h) {
        this.mRadioOffOrNotAvailableRegistrants.remove(h);
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

    public String getActiveApnHost(String apnType) {
        return this.mDcTracker.getActiveApnString(apnType);
    }

    public LinkProperties getLinkProperties(String apnType) {
        return this.mDcTracker.getLinkProperties(apnType);
    }

    public NetworkCapabilities getNetworkCapabilities(String apnType) {
        return this.mDcTracker.getNetworkCapabilities(apnType);
    }

    public boolean isDataConnectivityPossible() {
        return isDataConnectivityPossible("default");
    }

    public boolean isDataConnectivityPossible(String apnType) {
        if (this.mDcTracker != null) {
            return this.mDcTracker.isDataPossible(apnType);
        }
        return false;
    }

    public void notifyNewRingingConnectionP(Connection cn) {
        if (!this.mIsVoiceCapable) {
            return;
        }
        AsyncResult ar = new AsyncResult((Object) null, cn, (Throwable) null);
        this.mNewRingingConnectionRegistrants.notifyRegistrants(ar);
    }

    public void notifyUnknownConnectionP(Connection cn) {
        this.mUnknownConnectionRegistrants.notifyResult(cn);
    }

    public void notifyForVideoCapabilityChanged(boolean isVideoCallCapable) {
        this.mIsVideoCapable = isVideoCallCapable;
        AsyncResult ar = new AsyncResult((Object) null, Boolean.valueOf(isVideoCallCapable), (Throwable) null);
        this.mVideoCapabilityChangedRegistrants.notifyRegistrants(ar);
    }

    private void notifyIncomingRing() {
        if (!this.mIsVoiceCapable) {
            return;
        }
        AsyncResult ar = new AsyncResult((Object) null, this, (Throwable) null);
        this.mIncomingRingRegistrants.notifyRegistrants(ar);
    }

    private void sendIncomingCallRingNotification(int token) {
        if (!this.mIsVoiceCapable || this.mDoesRilSendMultipleCallRing || token != this.mCallRingContinueToken) {
            Rlog.d(LOG_TAG, "Ignoring ring notification request, mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing + " token=" + token + " mCallRingContinueToken=" + this.mCallRingContinueToken + " mIsVoiceCapable=" + this.mIsVoiceCapable);
            return;
        }
        Rlog.d(LOG_TAG, "Sending notifyIncomingRing");
        notifyIncomingRing();
        sendMessageDelayed(obtainMessage(15, token, 0), this.mCallRingDelay);
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

    public PhoneConstants.DataState getDataConnectionState() {
        return getDataConnectionState("default");
    }

    public void notifyCallForwardingIndicator() {
    }

    public void notifyDataConnectionFailed(String reason, String apnType) {
        this.mNotifier.notifyDataConnectionFailed(this, reason, apnType);
    }

    public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) {
        this.mNotifier.notifyPreciseDataConnectionFailed(this, reason, apnType, apn, failCause);
    }

    public int getLteOnCdmaMode() {
        return this.mCi.getLteOnCdmaMode();
    }

    public void setVoiceMessageWaiting(int line, int countWaiting) {
        Rlog.e(LOG_TAG, "Error! This function should never be executed, inactive Phone.");
    }

    public UsimServiceTable getUsimServiceTable() {
        IccRecords r = this.mIccRecords.get();
        if (r != null) {
            return r.getUsimServiceTable();
        }
        return null;
    }

    public UiccCard getUiccCard() {
        return this.mUiccController.getUiccCard(this.mPhoneId);
    }

    public String[] getPcscfAddress(String apnType) {
        return this.mDcTracker.getPcscfAddress(apnType);
    }

    public void setImsRegistrationState(boolean registered) {
    }

    public Phone getImsPhone() {
        return this.mImsPhone;
    }

    public boolean isUtEnabled() {
        return false;
    }

    public void dispose() {
    }

    private void updateImsPhone() {
        Rlog.d(LOG_TAG, "updateImsPhone mImsServiceReady=" + this.mImsServiceReady);
        if (this.mImsServiceReady && this.mImsPhone == null) {
            this.mImsPhone = PhoneFactory.makeImsPhone(this.mNotifier, this);
            CallManager.getInstance().registerPhone(this.mImsPhone);
            this.mImsPhone.registerForSilentRedial(this, 32, null);
        } else {
            if (this.mImsServiceReady || this.mImsPhone == null) {
                return;
            }
            CallManager.getInstance().unregisterPhone(this.mImsPhone);
            this.mImsPhone.unregisterForSilentRedial(this);
            this.mImsPhone.dispose();
            this.mImsPhone = null;
        }
    }

    protected Connection dialInternal(String dialString, UUSInfo uusInfo, int videoState, Bundle intentExtras) throws CallStateException {
        return null;
    }

    public int getSubId() {
        return SubscriptionController.getInstance().getSubIdUsingPhoneId(this.mPhoneId);
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public int getVoicePhoneServiceState() {
        Phone imsPhone = this.mImsPhone;
        if (imsPhone == null || imsPhone.getServiceState().getState() != 0) {
            return getServiceState().getState();
        }
        return 0;
    }

    public boolean setOperatorBrandOverride(String brand) {
        return false;
    }

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
            return true;
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

    public boolean isImsRegistered() {
        Phone imsPhone = this.mImsPhone;
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

    public boolean isWifiCallingEnabled() {
        Phone imsPhone = this.mImsPhone;
        boolean isWifiCallingEnabled = false;
        if (imsPhone != null) {
            isWifiCallingEnabled = imsPhone.isWifiCallingEnabled();
        }
        Rlog.d(LOG_TAG, "isWifiCallingEnabled =" + isWifiCallingEnabled);
        return isWifiCallingEnabled;
    }

    public boolean isVolteEnabled() {
        Phone imsPhone = this.mImsPhone;
        boolean isVolteEnabled = false;
        if (imsPhone != null) {
            isVolteEnabled = imsPhone.isVolteEnabled();
        }
        Rlog.d(LOG_TAG, "isImsRegistered =" + isVolteEnabled);
        return isVolteEnabled;
    }

    private boolean getRoamingOverrideHelper(String prefix, String key) {
        String iccId = getIccSerialNumber();
        if (TextUtils.isEmpty(iccId) || TextUtils.isEmpty(key)) {
            return false;
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        Set<String> value = sp.getStringSet(prefix + iccId, null);
        if (value == null) {
            return false;
        }
        return value.contains(key);
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

    public void setRadioCapability(RadioCapability rc, Message response) {
        this.mCi.setRadioCapability(rc, response);
    }

    public int getRadioAccessFamily() {
        RadioCapability rc = getRadioCapability();
        if (rc == null) {
            return 1;
        }
        return rc.getRadioAccessFamily();
    }

    public String getModemUuId() {
        RadioCapability rc = getRadioCapability();
        return rc == null ? UsimPBMemInfo.STRING_NOT_SET : rc.getLogicalModemUuid();
    }

    public RadioCapability getRadioCapability() {
        return this.mRadioCapability.get();
    }

    public void radioCapabilityUpdated(RadioCapability rc) {
        this.mRadioCapability.set(rc);
        if (!SubscriptionManager.isValidSubscriptionId(getSubId())) {
            return;
        }
        boolean skipRestoringSelection = this.mContext.getResources().getBoolean(R.^attr-private.glowDot);
        sendSubscriptionSettings(!skipRestoringSelection);
    }

    public void sendSubscriptionSettings(boolean restoreNetworkSelection) {
        ServiceStateTracker sst = getServiceStateTracker();
        if (sst != null) {
            sst.setDeviceRatMode(this.mPhoneId);
        }
        if (!restoreNetworkSelection) {
            return;
        }
        restoreSavedNetworkSelection(null);
    }

    protected void setPreferredNetworkTypeIfSimLoaded() {
        ServiceStateTracker sst = getServiceStateTracker();
        if (sst == null) {
            return;
        }
        sst.setDeviceRatMode(this.mPhoneId);
    }

    public void registerForRadioCapabilityChanged(Handler h, int what, Object obj) {
        this.mCi.registerForRadioCapabilityChanged(h, what, obj);
    }

    public void unregisterForRadioCapabilityChanged(Handler h) {
        this.mCi.unregisterForRadioCapabilityChanged(this);
    }

    public boolean isImsUseEnabled() {
        if (ImsManager.isVolteEnabledByPlatform(this.mContext) && ImsManager.isEnhanced4gLteModeSettingEnabledByUser(this.mContext)) {
            return true;
        }
        if (!ImsManager.isWfcEnabledByPlatform(this.mContext) || !ImsManager.isWfcEnabledByUser(this.mContext)) {
            return false;
        }
        boolean imsUseEnabled = ImsManager.isNonTtyOrTtyOnVolteEnabled(this.mContext);
        return imsUseEnabled;
    }

    public boolean isVideoEnabled() {
        Phone imsPhone = this.mImsPhone;
        boolean ret = false;
        if (imsPhone != null && imsPhone.getServiceState().getState() == 0) {
            ret = imsPhone.isVideoEnabled();
        }
        if (!ret) {
            ret = is3GVTEnabled();
        }
        Rlog.d(LOG_TAG, "isVideoEnabled: " + ret);
        return ret;
    }

    public boolean is3GVTEnabled() {
        int networkType = getServiceState().getVoiceNetworkType();
        Rlog.d(LOG_TAG, "networkType=" + TelephonyManager.getNetworkTypeName(networkType));
        boolean is3GVTNetworkAvailable = networkType == 3 || networkType == 8 || networkType == 9 || networkType == 10 || networkType == 15 || networkType == 13;
        if (SystemProperties.get("ro.mtk_vt3g324m_support").equals("1")) {
            return is3GVTNetworkAvailable;
        }
        return false;
    }

    public int getLceStatus() {
        return this.mLceStatus;
    }

    public void getModemActivityInfo(Message response) {
        this.mCi.getModemActivityInfo(response);
    }

    public void startLceAfterRadioIsAvailable() {
        this.mCi.startLceService(200, true, obtainMessage(37));
    }

    public Locale getLocaleFromSimAndCarrierPrefs() {
        IccRecords records = this.mIccRecords.get();
        if (records != null && records.getSimLanguage() != null) {
            return new Locale(records.getSimLanguage());
        }
        return getLocaleFromCarrierProperties(this.mContext);
    }

    public void updateDataConnectionTracker() {
        this.mDcTracker.update();
    }

    public void setInternalDataEnabled(boolean enable, Message onCompleteMsg) {
        this.mDcTracker.setInternalDataEnabled(enable, onCompleteMsg);
    }

    public boolean updateCurrentCarrierInProvider() {
        return false;
    }

    public void registerForAllDataDisconnected(Handler h, int what, Object obj) {
        this.mDcTracker.registerForAllDataDisconnected(h, what, obj);
    }

    public void unregisterForAllDataDisconnected(Handler h) {
        this.mDcTracker.unregisterForAllDataDisconnected(h);
    }

    public IccSmsInterfaceManager getIccSmsInterfaceManager() {
        return null;
    }

    protected boolean isMatchGid(String gid) {
        String gid1 = getGroupIdLevel1();
        int gidLength = gid.length();
        return !TextUtils.isEmpty(gid1) && gid1.length() >= gidLength && gid1.substring(0, gidLength).equalsIgnoreCase(gid);
    }

    public static void checkWfcWifiOnlyModeBeforeDial(Phone imsPhone, Context context) throws CallStateException {
        boolean wfcWiFiOnly = false;
        if (imsPhone != null && imsPhone.isWifiCallingEnabled()) {
            return;
        }
        if (ImsManager.isWfcEnabledByPlatform(context) && ImsManager.isWfcEnabledByUser(context) && ImsManager.getWfcMode(context) == 0) {
            wfcWiFiOnly = true;
        }
        if (!wfcWiFiOnly) {
        } else {
            throw new CallStateException(1, "WFC Wi-Fi Only Mode: IMS not registered");
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

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Phone: subId=" + getSubId());
        pw.println(" mPhoneId=" + this.mPhoneId);
        pw.println(" mCi=" + this.mCi);
        pw.println(" mDnsCheckDisabled=" + this.mDnsCheckDisabled);
        pw.println(" mDcTracker=" + this.mDcTracker);
        pw.println(" mDoesRilSendMultipleCallRing=" + this.mDoesRilSendMultipleCallRing);
        pw.println(" mCallRingContinueToken=" + this.mCallRingContinueToken);
        pw.println(" mCallRingDelay=" + this.mCallRingDelay);
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
        if (this.mImsPhone != null) {
            try {
                this.mImsPhone.dump(fd, pw, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mDcTracker != null) {
            try {
                this.mDcTracker.dump(fd, pw, args);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (getServiceStateTracker() != null) {
            try {
                getServiceStateTracker().dump(fd, pw, args);
            } catch (Exception e3) {
                e3.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (getCallTracker() != null) {
            try {
                getCallTracker().dump(fd, pw, args);
            } catch (Exception e4) {
                e4.printStackTrace();
            }
            pw.flush();
            pw.println("++++++++++++++++++++++++++++++++");
        }
        if (this.mCi == null || !(this.mCi instanceof RIL)) {
            return;
        }
        try {
            ((RIL) this.mCi).dump(fd, pw, args);
        } catch (Exception e5) {
            e5.printStackTrace();
        }
        pw.flush();
        pw.println("++++++++++++++++++++++++++++++++");
    }

    public synchronized void cancelAvailableNetworks(Message response) {
        Rlog.d(LOG_TAG, "cancelAvailableNetworks");
        this.mCi.unregisterForGetAvailableNetworksDone(this);
        this.mCi.cancelAvailableNetworks(response);
    }

    public void setNetworkSelectionModeSemiAutomatic(OperatorInfo network, Message response) {
        NetworkSelectMessage nsm = new NetworkSelectMessage(null);
        nsm.message = response;
        nsm.operatorNumeric = UsimPBMemInfo.STRING_NOT_SET;
        nsm.operatorAlphaLong = UsimPBMemInfo.STRING_NOT_SET;
        Message msg = obtainMessage(17, nsm);
        String actype = "0";
        if (network.getOperatorAlphaLong() != null && network.getOperatorAlphaLong().endsWith(UTRAN_INDICATOR)) {
            actype = ACT_TYPE_UTRAN;
        } else if (network.getOperatorAlphaLong() != null && network.getOperatorAlphaLong().endsWith(LTE_INDICATOR)) {
            actype = ACT_TYPE_LTE;
        }
        this.mCi.setNetworkSelectionModeSemiAutomatic(network.getOperatorNumeric(), actype, msg);
    }

    public void registerForNeighboringInfo(Handler h, int what, Object obj) {
    }

    public void unregisterForNeighboringInfo(Handler h) {
    }

    public void registerForNetworkInfo(Handler h, int what, Object obj) {
        this.mCi.registerForNetworkInfo(h, what, obj);
    }

    public void unregisterForNetworkInfo(Handler h) {
        this.mCi.unregisterForNetworkInfo(h);
    }

    public void refreshSpnDisplay() {
    }

    public int getNetworkHideState() {
        return 0;
    }

    public String getLocatedPlmn() {
        return null;
    }

    public void getFemtoCellList(String operatorNumeric, int rat, Message response) {
        Rlog.d(LOG_TAG, "getFemtoCellList(),operatorNumeric=" + operatorNumeric + ",rat=" + rat);
        this.mCi.getFemtoCellList(operatorNumeric, rat, response);
    }

    public void abortFemtoCellList(Message response) {
        Rlog.d(LOG_TAG, "abortFemtoCellList()");
        this.mCi.abortFemtoCellList(response);
    }

    public void selectFemtoCell(FemtoCellInfo femtocell, Message response) {
        Rlog.d(LOG_TAG, "selectFemtoCell(): " + femtocell);
        this.mCi.selectFemtoCell(femtocell, response);
    }

    public void getPolCapability(Message onComplete) {
        this.mCi.getPOLCapabilty(onComplete);
    }

    public void getPol(Message onComplete) {
        this.mCi.getCurrentPOLList(onComplete);
    }

    public void setPolEntry(NetworkInfoWithAcT networkWithAct, Message onComplete) {
        this.mCi.setPOLEntry(networkWithAct.getPriority(), networkWithAct.getOperatorNumeric(), networkWithAct.getAccessTechnology(), onComplete);
    }

    public void hangupAll() throws CallStateException {
    }

    @Override
    public void registerForCipherIndication(Handler h, int what, Object obj) {
        this.mCipherIndicationRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForCipherIndication(Handler h) {
        this.mCipherIndicationRegistrants.remove(h);
    }

    public void getFacilityLock(String facility, String password, Message onComplete) {
    }

    public void setFacilityLock(String facility, boolean enable, String password, Message onComplete) {
    }

    public void changeBarringPassword(String facility, String oldPwd, String newPwd, Message onComplete) {
    }

    public Connection dial(List<String> numbers, int videoState) throws CallStateException {
        return null;
    }

    public enum FeatureType {
        VOLTE_ENHANCED_CONFERENCE,
        VOLTE_CONF_REMOVE_MEMBER,
        VIDEO_RESTRICTION;

        public static FeatureType[] valuesCustom() {
            return values();
        }
    }

    public boolean isFeatureSupported(FeatureType feature) {
        if (this.mImsPhone == null) {
            Rlog.d(LOG_TAG, "isFeatureSupported = False with " + feature);
            return false;
        }
        return this.mImsPhone.isFeatureSupported(feature);
    }

    @Override
    public void explicitCallTransfer(String number, int type) {
    }

    public void registerForCrssSuppServiceNotification(Handler h, int what, Object obj) {
    }

    public void unregisterForCrssSuppServiceNotification(Handler h) {
    }

    @Override
    public void registerForVoiceCallIncomingIndication(Handler h, int what, Object obj) {
        Rlog.d(LOG_TAG, "registerForVoiceCallIncomingIndication");
        this.mVoiceCallIncomingIndicationRegistrants.addUnique(h, what, obj);
    }

    @Override
    public void unregisterForVoiceCallIncomingIndication(Handler h) {
        this.mVoiceCallIncomingIndicationRegistrants.remove(h);
    }

    public void registerForSpeechCodecInfo(Handler h, int what, Object obj) {
        this.mSpeechCodecInfoRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForSpeechCodecInfo(Handler h) {
        this.mSpeechCodecInfoRegistrants.remove(h);
    }

    void notifySpeechCodecInfo(int type) {
        this.mSpeechCodecInfoRegistrants.notifyResult(Integer.valueOf(type));
    }

    public void registerForVtStatusInfo(Handler h, int what, Object obj) {
        this.mCi.registerForVtStatusInfo(h, what, obj);
    }

    public void unregisterForVtStatusInfo(Handler h) {
        this.mCi.unregisterForVtStatusInfo(h);
    }

    public void registerForCdmaCallAccepted(Handler h, int what, Object obj) {
        checkCorrectThread(h);
        this.mCdmaCallAcceptedRegistrants.addUnique(h, what, obj);
    }

    public void unregisterForCdmaCallAccepted(Handler h) {
        this.mCdmaCallAcceptedRegistrants.remove(h);
    }

    public void notifyCdmaCallAccepted() {
        AsyncResult ar = new AsyncResult((Object) null, this, (Throwable) null);
        this.mCdmaCallAcceptedRegistrants.notifyRegistrants(ar);
    }

    @Override
    public int getCsFallbackStatus() {
        Rlog.d(LOG_TAG, "getCsFallbackStatus is " + this.mCSFallbackMode);
        return this.mCSFallbackMode;
    }

    public void setCsFallbackStatus(int newStatus) {
        Rlog.d(LOG_TAG, "setCsFallbackStatus to " + newStatus);
        this.mCSFallbackMode = newStatus;
    }

    public void getCallForwardInTimeSlot(int commandInterfaceCFReason, Message onComplete) {
        Rlog.d(LOG_TAG, "whk, need implement this method ");
    }

    public void setCallForwardInTimeSlot(int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, long[] timeSlot, Message onComplete) {
        Rlog.d(LOG_TAG, "whk, need implement this method ");
    }

    public void saveTimeSlot(long[] timeSlot) {
        String timeSlotKey = CFU_TIME_SLOT + this.mPhoneId;
        String timeSlotString = UsimPBMemInfo.STRING_NOT_SET;
        if (timeSlot != null && timeSlot.length == 2) {
            timeSlotString = Long.toString(timeSlot[0]) + "," + Long.toString(timeSlot[1]);
        }
        SystemProperties.set(timeSlotKey, timeSlotString);
        Rlog.d(LOG_TAG, "timeSlotString = " + timeSlotString);
    }

    public long[] getTimeSlot() {
        String timeSlotKey = CFU_TIME_SLOT + this.mPhoneId;
        String timeSlotString = SystemProperties.get(timeSlotKey, UsimPBMemInfo.STRING_NOT_SET);
        long[] timeSlot = null;
        if (timeSlotString != null && !timeSlotString.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            String[] timeArray = timeSlotString.split(",");
            if (timeArray.length == 2) {
                timeSlot = new long[2];
                for (int i = 0; i < 2; i++) {
                    timeSlot[i] = Long.parseLong(timeArray[i]);
                    Calendar calenar = Calendar.getInstance(TimeZone.getDefault());
                    calenar.setTimeInMillis(timeSlot[i]);
                    int hour = calenar.get(11);
                    int min = calenar.get(12);
                    Calendar calenar2 = Calendar.getInstance(TimeZone.getDefault());
                    calenar2.set(11, hour);
                    calenar2.set(12, min);
                    timeSlot[i] = calenar2.getTimeInMillis();
                }
            }
        }
        Rlog.d(LOG_TAG, "timeSlot = " + Arrays.toString(timeSlot));
        return timeSlot;
    }

    public String getMvnoMatchType() {
        return null;
    }

    public String getMvnoPattern(String type) {
        return null;
    }

    public int getCdmaSubscriptionActStatus() {
        return -1;
    }

    public void doGeneralSimAuthentication(int sessionId, int mode, int tag, String param1, String param2, Message result) {
    }

    @Override
    public void triggerModeSwitchByEcc(int mode, Message response) {
        this.mCi.triggerModeSwitchByEcc(mode, response);
    }

    public void queryPhbStorageInfo(int type, Message response) {
    }

    public void notifyLteAccessStratumChanged(String state) {
        this.mNotifier.notifyLteAccessStratumChanged(this, state);
    }

    public void notifyPsNetworkTypeChanged(int nwType) {
        this.mNotifier.notifyPsNetworkTypeChanged(this, nwType);
    }

    public void notifySharedDefaultApnStateChanged(boolean isSharedDefaultApn) {
        this.mNotifier.notifySharedDefaultApnStateChanged(this, isSharedDefaultApn);
    }
}
