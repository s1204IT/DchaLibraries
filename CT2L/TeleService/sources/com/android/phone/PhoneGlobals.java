package com.android.phone;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Message;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UpdateLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.IccCard;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.phone.OtaUtils;
import com.android.phone.common.CallLogAsync;
import com.android.server.sip.SipService;

public class PhoneGlobals extends ContextWrapper {
    private static final boolean DBG;
    static int mDockState;
    private static PhoneGlobals sMe;
    static boolean sVoiceCapable;
    private BluetoothManager bluetoothManager;
    CallController callController;
    private CallGatewayManager callGatewayManager;
    private CallStateMonitor callStateMonitor;
    CallerInfoCache callerInfoCache;
    public OtaUtils.CdmaOtaConfigData cdmaOtaConfigData;
    public OtaUtils.CdmaOtaInCallScreenUiState cdmaOtaInCallScreenUiState;
    public OtaUtils.CdmaOtaProvisionData cdmaOtaProvisionData;
    public OtaUtils.CdmaOtaScreenState cdmaOtaScreenState;
    CdmaPhoneCallState cdmaPhoneCallState;
    private boolean[] mCFUStatusQueried;
    CallManager mCM;
    private boolean mDataDisconnectedDueToRoaming;
    Handler mHandler;
    private boolean mIsSimPinEnabled;
    private KeyguardManager mKeyguardManager;
    private PhoneConstants.State mLastPhoneState;
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager mPowerManager;
    private IPowerManager mPowerManagerService;
    private final BroadcastReceiver mReceiver;
    private boolean mShouldRestoreMuteOnInCallResume;
    private UpdateLock mUpdateLock;
    private PowerManager.WakeLock mWakeLock;
    private WakeState mWakeState;
    NotificationMgr notificationMgr;
    CallNotifier notifier;
    public OtaUtils otaUtils;
    PhoneInterfaceManager phoneMgr;

    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        mDockState = 0;
        sVoiceCapable = true;
    }

    void setRestoreMuteOnInCallResume(boolean mode) {
        this.mShouldRestoreMuteOnInCallResume = mode;
    }

    public PhoneGlobals(Context context) {
        super(context);
        this.mDataDisconnectedDueToRoaming = false;
        this.mLastPhoneState = PhoneConstants.State.IDLE;
        this.mWakeState = WakeState.SLEEP;
        this.mReceiver = new PhoneAppBroadcastReceiver();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 3:
                        if (PhoneGlobals.this.getResources().getBoolean(R.bool.ignore_sim_network_locked_events)) {
                            Log.i("PhoneApp", "Ignoring EVENT_SIM_NETWORK_LOCKED event; not showing 'SIM network unlock' PIN entry screen");
                        } else {
                            Log.i("PhoneApp", "show sim depersonal panel");
                            IccNetworkDepersonalizationPanel ndpPanel = new IccNetworkDepersonalizationPanel(PhoneGlobals.getInstance());
                            ndpPanel.show();
                        }
                        break;
                    case 8:
                        if (msg.obj.equals("READY")) {
                            if (PhoneGlobals.this.mPUKEntryActivity != null) {
                                PhoneGlobals.this.mPUKEntryActivity.finish();
                                PhoneGlobals.this.mPUKEntryActivity = null;
                            }
                            if (PhoneGlobals.this.mPUKEntryProgressDialog != null) {
                                PhoneGlobals.this.mPUKEntryProgressDialog.dismiss();
                                PhoneGlobals.this.mPUKEntryProgressDialog = null;
                            }
                        }
                        break;
                    case 10:
                        PhoneGlobals.this.notificationMgr.showDataDisconnectedRoaming();
                        break;
                    case 11:
                        PhoneGlobals.this.notificationMgr.hideDataDisconnectedRoaming();
                        break;
                    case 13:
                        boolean inDockMode = false;
                        if (PhoneGlobals.mDockState != 0) {
                            inDockMode = true;
                        }
                        Log.d("PhoneApp", "received EVENT_DOCK_STATE_CHANGED. Phone inDock = " + inDockMode);
                        PhoneConstants.State phoneState = PhoneGlobals.this.mCM.getState();
                        if (phoneState == PhoneConstants.State.OFFHOOK && !PhoneGlobals.this.bluetoothManager.isBluetoothHeadsetAudioOn()) {
                            PhoneUtils.turnOnSpeaker(PhoneGlobals.this.getApplicationContext(), inDockMode, true);
                            break;
                        }
                        break;
                    case 14:
                        SipService.start(PhoneGlobals.this.getApplicationContext());
                        break;
                    case 30:
                        PhoneGlobals.this.handleSSN(msg);
                        break;
                    case 31:
                        PhoneGlobals.this.handleSSFailed((AsyncResult) msg.obj);
                        break;
                    case 52:
                        PhoneGlobals.this.onMMIComplete((AsyncResult) msg.obj);
                        break;
                    case 53:
                        PhoneUtils.cancelMmiCode(PhoneGlobals.this.mCM.getFgPhone());
                        break;
                    case 101:
                        PhoneGlobals.this.handleCF(msg);
                        break;
                }
            }
        };
        sMe = this;
    }

    public void onCreate() {
        Log.v("PhoneApp", "onCreate()...");
        ContentResolver resolver = getContentResolver();
        sVoiceCapable = getResources().getBoolean(android.R.^attr-private.externalRouteEnabledDrawable);
        if (this.mCM == null) {
            PhoneFactory.makeDefaultPhones(this);
            Intent intent = new Intent(this, (Class<?>) TelephonyDebugService.class);
            startService(intent);
            this.mCM = CallManager.getInstance();
            boolean hasCdmaPhoneType = false;
            Phone[] arr$ = PhoneFactory.getPhones();
            for (Phone phone : arr$) {
                this.mCM.registerPhone(phone);
                if (phone.getPhoneType() == 2) {
                    hasCdmaPhoneType = true;
                }
            }
            this.notificationMgr = NotificationMgr.init(this);
            this.mHandler.sendEmptyMessage(14);
            if (hasCdmaPhoneType) {
                this.cdmaPhoneCallState = new CdmaPhoneCallState();
                this.cdmaPhoneCallState.CdmaPhoneCallStateInit();
            }
            this.mPowerManager = (PowerManager) getSystemService("power");
            this.mWakeLock = this.mPowerManager.newWakeLock(26, "PhoneApp");
            this.mPartialWakeLock = this.mPowerManager.newWakeLock(536870913, "PhoneApp");
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
            this.mPowerManagerService = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
            this.mUpdateLock = new UpdateLock("phone");
            if (DBG) {
                Log.d("PhoneApp", "onCreate: mUpdateLock: " + this.mUpdateLock);
            }
            CallLogger callLogger = new CallLogger(this, new CallLogAsync());
            this.callGatewayManager = CallGatewayManager.getInstance();
            this.callController = CallController.init(this, callLogger, this.callGatewayManager);
            this.callerInfoCache = CallerInfoCache.init(this);
            this.callStateMonitor = new CallStateMonitor(this.mCM);
            this.bluetoothManager = new BluetoothManager();
            this.phoneMgr = PhoneInterfaceManager.init(this, PhoneFactory.getDefaultPhone());
            this.notifier = CallNotifier.init(this, callLogger, this.callStateMonitor, this.bluetoothManager);
            PhoneUtils.registerIccStatus(this.mHandler, 3);
            PhoneUtils.registerForSuppServiceNotification(this.mHandler, 30, null);
            this.mCM.registerForMmiComplete(this.mHandler, 52, (Object) null);
            this.mCM.registerForSuppServiceFailed(this.mHandler, 31, (Object) null);
            PhoneUtils.initializeConnectionHandler(this.mCM);
            int simCount = TelephonyManager.getDefault().getSimCount();
            this.mCFUStatusQueried = new boolean[simCount];
            for (int i = 0; i < simCount; i++) {
                this.mCFUStatusQueried[i] = false;
            }
            IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
            intentFilter.addAction("android.intent.action.SIM_ENABLE_CHANGED");
            intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
            intentFilter.addAction("android.intent.action.DOCK_EVENT");
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            intentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
            intentFilter.addAction("android.intent.action.SERVICE_STATE");
            intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
            intentFilter.addAction("android.intent.action.SIM_SYNC_APN");
            registerReceiver(this.mReceiver, intentFilter);
            PreferenceManager.setDefaultValues(this, R.xml.network_setting, false);
            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);
            PhoneUtils.setAudioMode(this.mCM);
        }
        this.cdmaOtaProvisionData = new OtaUtils.CdmaOtaProvisionData();
        this.cdmaOtaConfigData = new OtaUtils.CdmaOtaConfigData();
        this.cdmaOtaScreenState = new OtaUtils.CdmaOtaScreenState();
        this.cdmaOtaInCallScreenUiState = new OtaUtils.CdmaOtaInCallScreenUiState();
        resolver.getType(Uri.parse("content://icc/adn"));
        this.mShouldRestoreMuteOnInCallResume = false;
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            int hac = Settings.System.getInt(getContentResolver(), "hearing_aid", 0);
            AudioManager audioManager = (AudioManager) getSystemService("audio");
            audioManager.setParameter("HACSetting", hac != 0 ? "ON" : "OFF");
        }
    }

    public static PhoneGlobals getInstance() {
        if (sMe == null) {
            throw new IllegalStateException("No PhoneGlobals here!");
        }
        return sMe;
    }

    static PhoneGlobals getInstanceIfPrimary() {
        return sMe;
    }

    public static Phone getPhone() {
        return PhoneFactory.getDefaultPhone();
    }

    public static Phone getPhone(int subId) {
        return PhoneFactory.getPhone(SubscriptionManager.getPhoneId(subId));
    }

    BluetoothManager getBluetoothManager() {
        return this.bluetoothManager;
    }

    CallManager getCallManager() {
        return this.mCM;
    }

    boolean isSimPinEnabled() {
        return this.mIsSimPinEnabled;
    }

    void handleOtaspEvent(Message msg) {
        if (DBG) {
            Log.d("PhoneApp", "handleOtaspEvent(message " + msg + ")...");
        }
        if (this.otaUtils == null) {
            Log.w("PhoneApp", "handleOtaEvents: got an event but otaUtils is null! message = " + msg);
        } else {
            this.otaUtils.onOtaProvisionStatusChanged((AsyncResult) msg.obj);
        }
    }

    void handleOtaspDisconnect() {
        if (DBG) {
            Log.d("PhoneApp", "handleOtaspDisconnect()...");
        }
        if (this.otaUtils == null) {
            Log.w("PhoneApp", "handleOtaspDisconnect: otaUtils is null!");
        } else {
            this.otaUtils.onOtaspDisconnect();
        }
    }

    void setPukEntryActivity(Activity activity) {
        this.mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return this.mPUKEntryActivity;
    }

    void setPukEntryProgressDialog(ProgressDialog dialog) {
        this.mPUKEntryProgressDialog = dialog;
    }

    void requestWakeState(WakeState ws) {
        Log.d("PhoneApp", "requestWakeState(" + ws + ")...");
        synchronized (this) {
            if (this.mWakeState != ws) {
                switch (ws) {
                    case PARTIAL:
                        this.mPartialWakeLock.acquire();
                        if (this.mWakeLock.isHeld()) {
                            this.mWakeLock.release();
                        }
                        break;
                    case FULL:
                        this.mWakeLock.acquire();
                        if (this.mPartialWakeLock.isHeld()) {
                            this.mPartialWakeLock.release();
                        }
                        break;
                    default:
                        if (this.mWakeLock.isHeld()) {
                            this.mWakeLock.release();
                        }
                        if (this.mPartialWakeLock.isHeld()) {
                            this.mPartialWakeLock.release();
                        }
                        break;
                }
                this.mWakeState = ws;
            }
        }
    }

    public void wakeUpScreen() {
        synchronized (this) {
            if (this.mWakeState == WakeState.SLEEP) {
                if (DBG) {
                    Log.d("PhoneApp", "pulse screen lock");
                }
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis());
            }
        }
    }

    void updateWakeState() {
        PhoneConstants.State state = this.mCM.getState();
        if (state != PhoneConstants.State.OFFHOOK || PhoneUtils.isSpeakerOn(this)) {
        }
        boolean isRinging = state == PhoneConstants.State.RINGING;
        boolean isDialing = this.mCM.getFgPhone().getForegroundCall().getState() == Call.State.DIALING;
        boolean keepScreenOn = isRinging || isDialing;
        requestWakeState(keepScreenOn ? WakeState.FULL : WakeState.SLEEP);
    }

    void updatePhoneState(PhoneConstants.State state) {
        if (state != this.mLastPhoneState) {
            this.mLastPhoneState = state;
            if (state != PhoneConstants.State.IDLE) {
                if (!this.mUpdateLock.isHeld()) {
                    this.mUpdateLock.acquire();
                }
            } else if (this.mUpdateLock.isHeld()) {
                this.mUpdateLock.release();
            }
        }
    }

    KeyguardManager getKeyguardManager() {
        return this.mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult r) {
        Log.d("PhoneApp", "onMMIComplete()...");
        MmiCode mmiCode = (MmiCode) r.result;
        PhoneUtils.displayMMIComplete(mmiCode.getPhone(), getInstance(), mmiCode, null, null);
    }

    private void handleSSFailed(AsyncResult r) {
        if (DBG) {
            Log.d("PhoneApp", "handleSSFailed()...");
        }
        Phone.SuppService service = (Phone.SuppService) r.result;
        if (service == Phone.SuppService.TRANSFER) {
            Intent intent = new Intent(this, (Class<?>) ErrorDialogActivity.class);
            intent.setFlags(276824064);
            intent.putExtra("error_message_id", R.string.transfer_call_error);
            startActivity(intent);
        }
    }

    private void initForNewRadioTechnology(int phoneId) {
        if (DBG) {
            Log.d("PhoneApp", "initForNewRadioTechnology...");
        }
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone.getPhoneType() == 2) {
            this.cdmaPhoneCallState = new CdmaPhoneCallState();
            this.cdmaPhoneCallState.CdmaPhoneCallStateInit();
        }
        if (!TelephonyCapabilities.supportsOtasp(phone)) {
            clearOtaState();
        }
        this.notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
        this.callStateMonitor.updateAfterRadioTechnologyChange();
        IccCard sim = phone.getIccCard();
        if (sim != null) {
            if (DBG) {
                Log.d("PhoneApp", "Update registration for ICC status...");
            }
            sim.registerForNetworkLocked(this.mHandler, 3, (Object) null);
        }
    }

    private void updateRadioPowerProperty(boolean airplaneOff, boolean simEnabled, int simId) {
        String radioPowerProperty = simId == PhoneConstants.SimId.SIM1.ordinal() ? "persist.radio.sim1.power" : "persist.radio.sim2.power";
        SystemProperties.set(radioPowerProperty, Boolean.toString(airplaneOff && simEnabled));
    }

    private class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        private PhoneAppBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.AIRPLANE_MODE")) {
                boolean airplaneOff = Settings.System.getInt(PhoneGlobals.this.getContentResolver(), "airplane_mode_on", 0) == 0;
                int phoneCount = TelephonyManager.from(context).getPhoneCount();
                int phoneId = 0;
                while (phoneId < phoneCount) {
                    String simEnableKey = phoneId == PhoneConstants.SimId.SIM1.ordinal() ? "enable_sim1" : "enable_sim2";
                    boolean simEnable = Settings.Global.getInt(PhoneGlobals.this.getContentResolver(), simEnableKey, 1) != 0;
                    if (simEnable) {
                        Phone phone = PhoneFactory.getPhone(phoneId);
                        Boolean syncApnToCp = Boolean.valueOf(SystemProperties.getBoolean("persist.radio.syncApn", false));
                        phone.setRadioPower(airplaneOff && syncApnToCp.booleanValue());
                        PhoneGlobals.this.updateRadioPowerProperty(airplaneOff && syncApnToCp.booleanValue(), simEnable, phoneId);
                    }
                    phoneId++;
                }
                return;
            }
            if (action.equals("android.intent.action.SIM_ENABLE_CHANGED")) {
                int phoneId2 = intent.getIntExtra("phone", -1);
                if (!SubscriptionManager.isValidPhoneId(phoneId2)) {
                    Log.e("PhoneApp", "ACTION_SIM_ENABLE_CHANGED, Invalid PhoneId = " + phoneId2);
                    return;
                }
                boolean airplaneOff2 = Settings.System.getInt(PhoneGlobals.this.getContentResolver(), "airplane_mode_on", 0) == 0;
                Boolean syncApnToCp2 = Boolean.valueOf(SystemProperties.getBoolean("persist.radio.syncApn", false));
                if (airplaneOff2 && syncApnToCp2.booleanValue()) {
                    String simEnableKey2 = phoneId2 == PhoneConstants.SimId.SIM1.ordinal() ? "enable_sim1" : "enable_sim2";
                    boolean simEnable2 = Settings.Global.getInt(PhoneGlobals.this.getContentResolver(), simEnableKey2, 1) != 0;
                    Phone phone2 = PhoneFactory.getPhone(phoneId2);
                    phone2.setRadioPower(simEnable2);
                    PhoneGlobals.this.updateRadioPowerProperty(airplaneOff2 && syncApnToCp2.booleanValue(), simEnable2, phoneId2);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                int subId = intent.getIntExtra("subscription", -1);
                int phoneId3 = SubscriptionManager.getPhoneId(subId);
                String state = intent.getStringExtra("state");
                Log.d("PhoneApp", "mReceiver: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED");
                Log.d("PhoneApp", "- state: " + state);
                Log.d("PhoneApp", "- reason: " + intent.getStringExtra("reason"));
                Log.d("PhoneApp", "- subId: " + subId);
                Log.d("PhoneApp", "- phoneId: " + phoneId3);
                Phone phone3 = SubscriptionManager.isValidPhoneId(phoneId3) ? PhoneFactory.getPhone(phoneId3) : PhoneFactory.getDefaultPhone();
                boolean disconnectedDueToRoaming = !phone3.getDataRoamingEnabled() && PhoneConstants.DataState.DISCONNECTED.equals(state) && "roamingOn".equals(intent.getStringExtra("reason"));
                if (PhoneGlobals.this.mDataDisconnectedDueToRoaming != disconnectedDueToRoaming) {
                    PhoneGlobals.this.mDataDisconnectedDueToRoaming = disconnectedDueToRoaming;
                    PhoneGlobals.this.mHandler.sendEmptyMessage(disconnectedDueToRoaming ? 10 : 11);
                    return;
                }
                return;
            }
            if (action.equals("android.intent.action.SIM_STATE_CHANGED") && PhoneGlobals.this.mPUKEntryActivity != null) {
                PhoneGlobals.this.mHandler.sendMessage(PhoneGlobals.this.mHandler.obtainMessage(8, intent.getStringExtra("ss")));
                return;
            }
            if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                String newPhone = intent.getStringExtra("phoneName");
                int phoneId4 = intent.getIntExtra("phone", -1);
                Log.d("PhoneApp", "Radio technology switched. Now " + newPhone + " (" + phoneId4 + ") is active.");
                PhoneGlobals.this.initForNewRadioTechnology(phoneId4);
                return;
            }
            if (action.equals("android.intent.action.SERVICE_STATE")) {
                PhoneGlobals.this.handleServiceStateChanged(intent);
                return;
            }
            if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                if (TelephonyCapabilities.supportsEcm(PhoneGlobals.this.mCM.getFgPhone())) {
                    Log.d("PhoneApp", "Emergency Callback Mode arrived in PhoneApp.");
                    if (intent.getBooleanExtra("phoneinECMState", false)) {
                        context.startService(new Intent(context, (Class<?>) EmergencyCallbackModeService.class));
                        return;
                    }
                    return;
                }
                Log.e("PhoneApp", "Got ACTION_EMERGENCY_CALLBACK_MODE_CHANGED, but ECM isn't supported for phone: " + PhoneGlobals.this.mCM.getFgPhone().getPhoneName());
                return;
            }
            if (action.equals("android.intent.action.DOCK_EVENT")) {
                PhoneGlobals.mDockState = intent.getIntExtra("android.intent.extra.DOCK_STATE", 0);
                Log.d("PhoneApp", "ACTION_DOCK_EVENT -> mDockState = " + PhoneGlobals.mDockState);
                PhoneGlobals.this.mHandler.sendMessage(PhoneGlobals.this.mHandler.obtainMessage(13, 0));
            } else if (action.equals("android.intent.action.SIM_SYNC_APN")) {
                int phoneId5 = intent.getIntExtra("phone", PhoneConstants.SimId.SIM1.ordinal());
                String simEnableKey3 = phoneId5 == PhoneConstants.SimId.SIM1.ordinal() ? "enable_sim1" : "enable_sim2";
                boolean simEnable3 = Settings.Global.getInt(PhoneGlobals.this.getContentResolver(), simEnableKey3, 1) != 0;
                boolean airplaneOff3 = Settings.System.getInt(PhoneGlobals.this.getContentResolver(), "airplane_mode_on", 0) == 0;
                if (airplaneOff3) {
                    Phone phone4 = PhoneFactory.getPhone(phoneId5);
                    phone4.setRadioPower(simEnable3);
                }
                Log.d("PhoneApp", "received ACTION_SYNC_APN simId = " + phoneId5 + " airplaneOff = " + airplaneOff3);
            }
        }
    }

    public static class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("PhoneApp", "Broadcast from Notification: " + action);
            if (action.equals("com.android.phone.ACTION_HANG_UP_ONGOING_CALL")) {
                PhoneUtils.hangup(PhoneGlobals.getInstance().mCM);
            } else {
                Log.w("PhoneApp", "Received hang-up request from notification, but there's no call the system can hang up.");
            }
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        final int slotId;
        ServiceState ss = ServiceState.newFromBundle(intent.getExtras());
        if (ss != null) {
            int state = ss.getState();
            this.notificationMgr.updateNetworkSelection(state);
            int subId = intent.getIntExtra("subscription", -1);
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                slotId = SubscriptionManager.getSlotId(subId);
            } else {
                slotId = (-2) - subId;
            }
            if (state == 0 && SubscriptionManager.isValidSlotId(slotId) && !this.mCFUStatusQueried[slotId]) {
                String enabled = SystemProperties.get("persist.radio.poweron.cfu", "0");
                if (enabled.equals("1")) {
                    Log.d("PhoneApp", "query CCFC for phone " + slotId);
                    this.mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            PhoneFactory.getPhone(slotId).getCallForwardingOption(0, PhoneGlobals.this.mHandler.obtainMessage(101, 0, slotId));
                        }
                    }, 30000L);
                    this.mCFUStatusQueried[slotId] = true;
                }
            }
        }
    }

    public void clearOtaState() {
        if (DBG) {
            Log.d("PhoneApp", "- clearOtaState ...");
        }
        if (this.otaUtils != null) {
            this.otaUtils.cleanOtaScreen(true);
            if (DBG) {
                Log.d("PhoneApp", "  - clearOtaState clears OTA screen");
            }
        }
    }

    public void dismissOtaDialogs() {
        if (DBG) {
            Log.d("PhoneApp", "- dismissOtaDialogs ...");
        }
        if (this.otaUtils != null) {
            this.otaUtils.dismissAllOtaDialogs();
            if (DBG) {
                Log.d("PhoneApp", "  - dismissOtaDialogs clears OTA dialogs");
            }
        }
    }

    public void refreshMwiIndicator(int subId) {
        this.notificationMgr.refreshMwi(subId);
    }

    private void handleSSN(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        SuppServiceNotification notification = (SuppServiceNotification) ar.result;
        PhoneUtils.displaySSN(getInstance(), notification);
    }

    private void handleCF(Message msg) {
        AsyncResult ar = (AsyncResult) msg.obj;
        final int tryNum = msg.arg1 + 1;
        final int slotId = msg.arg2;
        if (ar.exception != null) {
            Log.e("PhoneApp", "EVENT_CF: get call forward status, ar.exception" + ar.exception);
            if (tryNum < 5) {
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PhoneFactory.getPhone(slotId).getCallForwardingOption(0, PhoneGlobals.this.mHandler.obtainMessage(101, tryNum, slotId));
                    }
                }, 5000L);
            }
        }
    }
}
