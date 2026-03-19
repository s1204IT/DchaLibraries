package com.android.phone;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UpdateLock;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.LocalLog;
import android.util.Log;
import android.widget.Toast;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.telephony.TelephonyCapabilities;
import com.android.internal.telephony.dataconnection.DataConnectionReasons;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.phone.common.CallLogAsync;
import com.android.phone.settings.SettingsConstants;
import com.android.phone.vvm.CarrierVvmPackageInstalledReceiver;
import com.android.services.telephony.sip.SipAccountRegistry;
import com.android.services.telephony.sip.SipUtil;
import com.google.android.collect.Sets;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.phone.ext.ExtensionManager;
import com.mediatek.services.telephony.MtkLogUtils;
import com.mediatek.settings.TelephonyUtils;
import com.mediatek.settings.cdma.CdmaVolteServiceChecker;
import com.mediatek.telephony.MtkTelephonyManagerEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PhoneGlobals extends ContextWrapper {
    public static final String ACTION_PRIMARY_SIM_CHANGED = "com.mediatek.settings.PRIMARY_SIM_CHANGED";
    public static final int AIRPLANE_OFF = 0;
    public static final int AIRPLANE_ON = 1;
    private static final boolean DBG;
    public static final int DBG_LEVEL = 1;
    private static final int EVENT_DATA_ROAMING_DISCONNECTED = 10;
    private static final int EVENT_DATA_ROAMING_OK = 11;
    private static final int EVENT_DATA_ROAMING_SETTINGS_CHANGED = 14;
    private static final int EVENT_MOBILE_DATA_SETTINGS_CHANGED = 15;
    private static final int EVENT_RESTART_SIP = 13;
    private static final int EVENT_SIM_NETWORK_LOCKED = 3;
    private static final int EVENT_SIM_STATE_CHANGED = 8;
    private static final int EVENT_UNSOL_CDMA_INFO_RECORD = 12;
    public static final String EXTRA_SUBSCRIPTION_ID = "subscription_id";
    public static final String LOG_TAG = "PhoneGlobals";
    public static final int MMI_CANCEL = 53;
    public static final int MMI_COMPLETE = 52;
    public static final int MMI_INITIATE = 51;
    private static final boolean VDBG = false;
    private static PhoneGlobals sMe;
    static boolean sVoiceCapable;
    private CallGatewayManager callGatewayManager;
    CallerInfoCache callerInfoCache;
    CdmaPhoneCallState cdmaPhoneCallState;
    CarrierConfigLoader configLoader;
    CallManager mCM;
    private final CarrierVvmPackageInstalledReceiver mCarrierVvmPackageInstalledReceiver;
    private final LocalLog mDataRoamingNotifLog;
    private int mDefaultDataSubId;
    Handler mHandler;
    private KeyguardManager mKeyguardManager;
    private boolean mNoDataDueToRoaming;
    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener;
    private Activity mPUKEntryActivity;
    private ProgressDialog mPUKEntryProgressDialog;
    private PowerManager.WakeLock mPartialWakeLock;
    private PowerManager mPowerManager;
    private final BroadcastReceiver mReceiver;
    private final SettingsObserver mSettingsObserver;
    private final BroadcastReceiver mSimSettingReceiver;
    private final SipReceiver mSipReceiver;
    private final Set<SubInfoUpdateListener> mSubInfoUpdateListeners;
    private List<SubscriptionInfo> mSubscriptionInfos;
    private MtkTelephonyManagerEx mTelEx;
    private UpdateLock mUpdateLock;
    private PowerManager.WakeLock mWakeLock;
    private WakeState mWakeState;
    NotificationMgr notificationMgr;
    CallNotifier notifier;
    private Phone phoneInEcm;
    public PhoneInterfaceManager phoneMgr;

    public interface SubInfoUpdateListener {
        void handleSubInfoUpdate();
    }

    public enum WakeState {
        SLEEP,
        PARTIAL,
        FULL
    }

    static {
        DBG = SystemProperties.getInt("ro.debuggable", 0) == 1;
        sVoiceCapable = true;
    }

    public PhoneGlobals(Context context) {
        super(context);
        this.mNoDataDueToRoaming = false;
        this.mWakeState = WakeState.SLEEP;
        this.mDefaultDataSubId = -1;
        this.mDataRoamingNotifLog = new LocalLog(50);
        this.mTelEx = MtkTelephonyManagerEx.getDefault();
        this.mReceiver = new PhoneAppBroadcastReceiver();
        this.mSipReceiver = new SipReceiver();
        this.mCarrierVvmPackageInstalledReceiver = new CarrierVvmPackageInstalledReceiver();
        this.mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                int i = message.what;
                if (i == 3) {
                    if (PhoneGlobals.this.getCarrierConfig().getBoolean("ignore_sim_network_locked_events_bool")) {
                        Log.i(PhoneGlobals.LOG_TAG, "Ignoring EVENT_SIM_NETWORK_LOCKED event; not showing 'SIM network unlock' PIN entry screen");
                        return;
                    } else {
                        Log.i(PhoneGlobals.LOG_TAG, "show sim depersonal panel");
                        IccNetworkDepersonalizationPanel.showDialog((Phone) ((AsyncResult) message.obj).userObj);
                        return;
                    }
                }
                if (i != 8) {
                    switch (i) {
                        case 10:
                            PhoneGlobals.this.notificationMgr.showDataDisconnectedRoaming(message.arg1);
                            break;
                        case 11:
                            PhoneGlobals.this.notificationMgr.hideDataDisconnectedRoaming();
                            break;
                        case 12:
                            break;
                        case 13:
                            UserManager userManager = UserManager.get(PhoneGlobals.sMe);
                            if (userManager != null && userManager.isUserUnlocked()) {
                                SipUtil.startSipService();
                                break;
                            }
                            break;
                        case 14:
                        case 15:
                            PhoneGlobals.this.updateDataRoamingStatus();
                            break;
                        default:
                            switch (i) {
                                case 52:
                                    PhoneGlobals.this.onMMIComplete((AsyncResult) message.obj);
                                    break;
                                case 53:
                                    PhoneUtils.cancelMmiCode(PhoneGlobals.this.mCM.getFgPhone());
                                    PhoneUtils.dismissUssdDialog();
                                    break;
                            }
                            break;
                    }
                    return;
                }
                if (message.obj.equals("READY")) {
                    if (PhoneGlobals.this.mPUKEntryActivity != null) {
                        PhoneGlobals.this.mPUKEntryActivity.finish();
                        PhoneGlobals.this.mPUKEntryActivity = null;
                    }
                    if (PhoneGlobals.this.mPUKEntryProgressDialog != null) {
                        PhoneGlobals.this.mPUKEntryProgressDialog.dismiss();
                        PhoneGlobals.this.mPUKEntryProgressDialog = null;
                    }
                }
            }
        };
        this.mSubInfoUpdateListeners = Sets.newArraySet();
        this.mOnSubscriptionsChangeListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
            @Override
            public void onSubscriptionsChanged() {
                Log.d(PhoneGlobals.LOG_TAG, "onSubscriptionsChanged start");
                List<SubscriptionInfo> activeSubInfoList = PhoneUtils.getActiveSubInfoList();
                if (TelephonyUtils.isHotSwapHanppened(PhoneGlobals.this.mSubscriptionInfos, activeSubInfoList)) {
                    PhoneGlobals.this.mSubscriptionInfos = activeSubInfoList;
                    Iterator it = PhoneGlobals.this.mSubInfoUpdateListeners.iterator();
                    while (it.hasNext()) {
                        ((SubInfoUpdateListener) it.next()).handleSubInfoUpdate();
                    }
                }
                Log.d(PhoneGlobals.LOG_TAG, "onSubscriptionsChanged end");
            }
        };
        this.mSimSettingReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                Log.d(PhoneGlobals.LOG_TAG, "SimSettingReceiver.onReceive, action=" + intent.getAction());
                ExtensionManager.getSimDialogExt().customBroadcast(intent);
                Intent intent2 = new Intent(intent);
                intent2.setAction("com.mediatek.settings.sim.ACTION_SUBINFO_RECORD_UPDATED");
                intent2.setComponent(new ComponentName("com.android.settings", "com.android.settings.sim.SimSelectNotification"));
                context2.sendBroadcastAsUser(intent2, UserHandle.ALL);
            }
        };
        sMe = this;
        this.mSettingsObserver = new SettingsObserver(context, this.mHandler);
    }

    public void onCreate() {
        MtkLogUtils.initLogging(this);
        ContentResolver contentResolver = getContentResolver();
        sVoiceCapable = getResources().getBoolean(android.R.^attr-private.popupPromptView);
        if (this.mCM == null) {
            PhoneFactory.makeDefaultPhones(this);
            startService(new Intent(this, (Class<?>) TelephonyDebugService.class));
            this.mCM = CallManager.getInstance();
            for (Phone phone : PhoneFactory.getPhones()) {
                this.mCM.registerPhone(phone);
            }
            this.notificationMgr = NotificationMgr.init(this);
            this.mHandler.sendEmptyMessage(13);
            this.cdmaPhoneCallState = new CdmaPhoneCallState();
            this.cdmaPhoneCallState.CdmaPhoneCallStateInit();
            this.mPowerManager = (PowerManager) getSystemService("power");
            this.mWakeLock = this.mPowerManager.newWakeLock(26, LOG_TAG);
            this.mPartialWakeLock = this.mPowerManager.newWakeLock(536870913, LOG_TAG);
            this.mKeyguardManager = (KeyguardManager) getSystemService("keyguard");
            this.mUpdateLock = new UpdateLock("phone");
            if (DBG) {
                Log.d(LOG_TAG, "onCreate: mUpdateLock: " + this.mUpdateLock);
            }
            new CallLogger(this, new CallLogAsync());
            this.callGatewayManager = CallGatewayManager.getInstance();
            this.callerInfoCache = CallerInfoCache.init(this);
            this.phoneMgr = PhoneInterfaceManager.init(this, PhoneFactory.getDefaultPhone());
            this.configLoader = CarrierConfigLoader.init(this);
            this.notifier = CallNotifier.init(this);
            PhoneUtils.registerIccStatus(this.mHandler, 3);
            this.mCM.registerForMmiComplete(this.mHandler, 52, (Object) null);
            PhoneUtils.initializeConnectionHandler(this.mCM);
            IntentFilter intentFilter = new IntentFilter("android.intent.action.AIRPLANE_MODE");
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            intentFilter.addAction("android.intent.action.RADIO_TECHNOLOGY");
            intentFilter.addAction("android.intent.action.SERVICE_STATE");
            intentFilter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
            intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
            intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
            intentFilter.addAction("com.mediatek.common.carrierexpress.operator_config_changed");
            intentFilter.addAction(ACTION_PRIMARY_SIM_CHANGED);
            registerReceiver(this.mReceiver, intentFilter);
            SubscriptionManager.from(this).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangeListener);
            this.mSubscriptionInfos = PhoneUtils.getActiveSubInfoList();
            IntentFilter intentFilter2 = new IntentFilter("android.intent.action.BOOT_COMPLETED");
            intentFilter2.addAction("android.net.sip.SIP_SERVICE_UP");
            intentFilter2.addAction("com.android.phone.SIP_CALL_OPTION_CHANGED");
            intentFilter2.addAction("com.android.phone.SIP_REMOVE_PHONE");
            registerReceiver(this.mSipReceiver, intentFilter2);
            ?? r1 = Settings.System.getInt(getContentResolver(), "airplane_mode_on", 0) != 0 ? 1 : 0;
            if (DBG) {
                Log.d(LOG_TAG, "Notify RadioManager with airplane mode:" + ((boolean) r1));
            }
            handleAirplaneModeChange(this, r1);
            this.mCarrierVvmPackageInstalledReceiver.register(this);
            PreferenceManager.setDefaultValues(this, R.xml.network_setting_fragment, false);
            PreferenceManager.setDefaultValues(this, R.xml.call_feature_setting, false);
            PhoneUtils.setAudioMode(this.mCM);
            IntentFilter intentFilter3 = new IntentFilter();
            intentFilter3.addAction("android.intent.action.ACTION_SUBINFO_RECORD_UPDATED");
            registerReceiver(this.mSimSettingReceiver, intentFilter3);
            Log.d(LOG_TAG, "Register receiver for SIM Settings.");
        }
        contentResolver.getType(Uri.parse(ADNList.ICC_ADN_URI));
        if (getResources().getBoolean(R.bool.hac_enabled)) {
            ((AudioManager) getSystemService("audio")).setParameter(SettingsConstants.HAC_KEY, Settings.System.getInt(getContentResolver(), "hearing_aid", 0) == 1 ? SettingsConstants.HAC_VAL_ON : SettingsConstants.HAC_VAL_OFF);
        }
        CdmaVolteServiceChecker.getInstance(this).init();
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

    public static Phone getPhone(int i) {
        return PhoneFactory.getPhone(SubscriptionManager.getPhoneId(i));
    }

    CallManager getCallManager() {
        return this.mCM;
    }

    public PersistableBundle getCarrierConfig() {
        return getCarrierConfigForSubId(SubscriptionManager.getDefaultSubscriptionId());
    }

    public PersistableBundle getCarrierConfigForSubId(int i) {
        return this.configLoader.getConfigForSubId(i);
    }

    private void registerSettingsObserver() {
        int i;
        this.mSettingsObserver.unobserve();
        String str = "data_roaming";
        String str2 = "mobile_data";
        if (TelephonyManager.getDefault().getSimCount() > 1 && (i = this.mDefaultDataSubId) != -1) {
            str = "data_roaming" + i;
            str2 = "mobile_data" + i;
        }
        this.mSettingsObserver.observe(Settings.Global.getUriFor(str), 14);
        this.mSettingsObserver.observe(Settings.Global.getUriFor(str2), 15);
    }

    void setPukEntryActivity(Activity activity) {
        this.mPUKEntryActivity = activity;
    }

    Activity getPUKEntryActivity() {
        return this.mPUKEntryActivity;
    }

    void setPukEntryProgressDialog(ProgressDialog progressDialog) {
        this.mPUKEntryProgressDialog = progressDialog;
    }

    void requestWakeState(WakeState wakeState) {
        synchronized (this) {
            if (this.mWakeState != wakeState) {
                switch (wakeState) {
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
                this.mWakeState = wakeState;
            }
        }
    }

    void wakeUpScreen() {
        synchronized (this) {
            if (this.mWakeState == WakeState.SLEEP) {
                if (DBG) {
                    Log.d(LOG_TAG, "pulse screen lock");
                }
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "android.phone:WAKE");
            }
        }
    }

    void updateWakeState() {
        PhoneConstants.State state = this.mCM.getState();
        if (state == PhoneConstants.State.OFFHOOK) {
            PhoneUtils.isSpeakerOn(this);
        }
        requestWakeState((state == PhoneConstants.State.RINGING) || (this.mCM.getFgPhone().getForegroundCall().getState() == Call.State.DIALING) ? WakeState.FULL : WakeState.SLEEP);
    }

    KeyguardManager getKeyguardManager() {
        return this.mKeyguardManager;
    }

    private void onMMIComplete(AsyncResult asyncResult) {
        MmiCode mmiCode = (MmiCode) asyncResult.result;
        PhoneUtils.displayMMIComplete(mmiCode.getPhone(), getInstance(), mmiCode, null, null);
    }

    private void initForNewRadioTechnology() {
        if (DBG) {
            Log.d(LOG_TAG, "initForNewRadioTechnology...");
        }
        this.notifier.updateCallNotifierRegistrationsAfterRadioTechnologyChange();
    }

    private void handleAirplaneModeChange(Context context, int i) {
        int i2 = Settings.Global.getInt(context.getContentResolver(), "cell_on", 1);
        boolean z = i == 1;
        switch (i2) {
            case 1:
                maybeTurnCellOff(context, z);
                break;
            case 2:
                maybeTurnCellOn(context, z);
                break;
        }
    }

    private boolean isCellOffInAirplaneMode(Context context) {
        String string = Settings.Global.getString(context.getContentResolver(), "airplane_mode_radios");
        return string == null || string.contains("cell");
    }

    private void setRadioPowerOff(Context context) {
        Log.i(LOG_TAG, "Turning radio off - airplane");
        Settings.Global.putInt(context.getContentResolver(), "cell_on", 2);
        SystemProperties.set("persist.radio.airplane_mode_on", SettingsConstants.DUA_VAL_ON);
        Settings.Global.putInt(getContentResolver(), "enable_cellular_on_boot", 0);
        RadioManager.getInstance().notifyAirplaneModeChange(true);
        RadioManager.getInstance();
        if (RadioManager.isPowerOnFeatureAllClosed()) {
            PhoneUtils.setRadioPower(false);
        }
    }

    private void setRadioPowerOn(Context context) {
        Log.i(LOG_TAG, "Turning radio on - airplane");
        Settings.Global.putInt(context.getContentResolver(), "cell_on", 1);
        Settings.Global.putInt(getContentResolver(), "enable_cellular_on_boot", 1);
        SystemProperties.set("persist.radio.airplane_mode_on", SettingsConstants.DUAL_VAL_OFF);
        RadioManager.getInstance().notifyAirplaneModeChange(false);
        RadioManager.getInstance();
        if (RadioManager.isPowerOnFeatureAllClosed()) {
            PhoneUtils.setRadioPower(true);
        }
    }

    private void maybeTurnCellOff(Context context, boolean z) {
        if (z) {
            if (this.mTelEx.isEccInProgress()) {
                ConnectivityManager.from(this).setAirplaneMode(false);
                Toast.makeText(this, R.string.radio_off_during_emergency_call, 1).show();
                Log.i(LOG_TAG, "Ignoring airplane mode: emergency call. Turning airplane off");
            } else if (isCellOffInAirplaneMode(context)) {
                setRadioPowerOff(context);
            } else {
                Log.i(LOG_TAG, "Ignoring airplane mode: settings prevent cell radio power off");
            }
        }
    }

    private void maybeTurnCellOn(Context context, boolean z) {
        if (!z) {
            setRadioPowerOn(context);
        }
    }

    private class PhoneAppBroadcastReceiver extends BroadcastReceiver {
        private PhoneAppBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (!action.equals("android.intent.action.AIRPLANE_MODE")) {
                if (action.equals("android.intent.action.SIM_STATE_CHANGED") && PhoneGlobals.this.mPUKEntryActivity != null) {
                    PhoneGlobals.this.mHandler.sendMessage(PhoneGlobals.this.mHandler.obtainMessage(8, intent.getStringExtra("ss")));
                    return;
                }
                if (action.equals("android.intent.action.RADIO_TECHNOLOGY")) {
                    Log.d(PhoneGlobals.LOG_TAG, "Radio technology switched. Now " + intent.getStringExtra("phoneName") + " is active.");
                    PhoneGlobals.this.initForNewRadioTechnology();
                    return;
                }
                if (action.equals("android.intent.action.SERVICE_STATE")) {
                    PhoneGlobals.this.handleServiceStateChanged(intent);
                    return;
                }
                if (action.equals("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED")) {
                    int intExtra = intent.getIntExtra("phone", 0);
                    PhoneGlobals.this.phoneInEcm = PhoneFactory.getPhone(intExtra);
                    Log.d(PhoneGlobals.LOG_TAG, "Emergency Callback Mode. phoneId:" + intExtra);
                    if (PhoneGlobals.this.phoneInEcm != null) {
                        if (!TelephonyCapabilities.supportsEcm(PhoneGlobals.this.phoneInEcm)) {
                            Log.e(PhoneGlobals.LOG_TAG, "Got ACTION_EMERGENCY_CALLBACK_MODE_CHANGED, but ECM isn't supported for phone: " + PhoneGlobals.this.phoneInEcm.getPhoneName());
                            PhoneGlobals.this.phoneInEcm = null;
                            return;
                        }
                        Log.d(PhoneGlobals.LOG_TAG, "Emergency Callback Mode arrived in PhoneApp.");
                        if (!intent.getBooleanExtra("phoneinECMState", false)) {
                            PhoneGlobals.this.phoneInEcm = null;
                            return;
                        } else {
                            context.startService(new Intent(context, (Class<?>) EmergencyCallbackModeService.class));
                            return;
                        }
                    }
                    Log.w(PhoneGlobals.LOG_TAG, "phoneInEcm is null.");
                    return;
                }
                if (action.equals("com.mediatek.intent.action.MSIM_MODE")) {
                    RadioManager.getInstance().notifyMSimModeChange(intent.getIntExtra("mode", -1));
                    return;
                }
                if (action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    PhoneGlobals.this.updateDataRoamingStatus();
                    return;
                }
                if (action.equals("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED")) {
                    PhoneGlobals.this.mDefaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                    PhoneGlobals.this.registerSettingsObserver();
                    if (PhoneGlobals.getPhone(PhoneGlobals.this.mDefaultDataSubId) != null) {
                        PhoneGlobals.this.updateDataRoamingStatus();
                        return;
                    }
                    return;
                }
                if (action.equals("com.mediatek.common.carrierexpress.operator_config_changed")) {
                    Log.v(PhoneGlobals.LOG_TAG, "Reset app context for plugin");
                    ExtensionManager.resetApplicationContext(context);
                    return;
                } else {
                    if (action.equals(PhoneGlobals.ACTION_PRIMARY_SIM_CHANGED)) {
                        int intExtra2 = intent.getIntExtra(PhoneGlobals.EXTRA_SUBSCRIPTION_ID, -1);
                        Log.d(PhoneGlobals.LOG_TAG, "Receive ACTION_PRIMARY_SIM_CHANGED for subid " + intExtra2);
                        ExtensionManager.getPhoneGlobalsExt().handlePrimarySimUpdate(context, intExtra2);
                        return;
                    }
                    return;
                }
            }
            int i = Settings.Global.getInt(PhoneGlobals.this.getContentResolver(), "airplane_mode_on", 0);
            if (i != 0) {
                i = 1;
            }
            PhoneGlobals.this.handleAirplaneModeChange(context, i);
        }
    }

    private class SipReceiver extends BroadcastReceiver {
        private SipReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            SipAccountRegistry sipAccountRegistry = SipAccountRegistry.getInstance();
            if (action.equals("android.intent.action.BOOT_COMPLETED")) {
                SipUtil.startSipService();
                return;
            }
            if (action.equals("android.net.sip.SIP_SERVICE_UP") || action.equals("com.android.phone.SIP_CALL_OPTION_CHANGED")) {
                sipAccountRegistry.setup(context);
                return;
            }
            if (action.equals("com.android.phone.SIP_REMOVE_PHONE")) {
                if (PhoneGlobals.DBG) {
                    Log.d(PhoneGlobals.LOG_TAG, "SIP_REMOVE_PHONE " + intent.getStringExtra("android:localSipUri"));
                }
                sipAccountRegistry.removeSipProfile(intent.getStringExtra("android:localSipUri"));
                return;
            }
            if (PhoneGlobals.DBG) {
                Log.d(PhoneGlobals.LOG_TAG, "onReceive, action not processed: " + action);
            }
        }
    }

    private void handleServiceStateChanged(Intent intent) {
        ServiceState serviceStateNewFromBundle;
        Bundle extras = intent.getExtras();
        if (extras != null && (serviceStateNewFromBundle = ServiceState.newFromBundle(extras)) != null) {
            int state = serviceStateNewFromBundle.getState();
            int intExtra = intent.getIntExtra("subscription", -1);
            this.notificationMgr.updateNetworkSelection(state, intExtra);
            if (intExtra == this.mDefaultDataSubId) {
                updateDataRoamingStatus();
            }
        }
    }

    private void updateDataRoamingStatus() {
        Phone phone = getPhone(this.mDefaultDataSubId);
        if (phone == null) {
            Log.w(LOG_TAG, "Can't get phone with sub id = " + this.mDefaultDataSubId);
            return;
        }
        DataConnectionReasons dataConnectionReasons = new DataConnectionReasons();
        boolean zIsDataAllowed = phone.isDataAllowed(dataConnectionReasons);
        this.mDataRoamingNotifLog.log("dataAllowed=" + zIsDataAllowed + ", reasons=" + dataConnectionReasons);
        if (!this.mNoDataDueToRoaming && !zIsDataAllowed && dataConnectionReasons.containsOnly(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED)) {
            this.mNoDataDueToRoaming = true;
            Log.d(LOG_TAG, "Show roaming disconnected notification");
            this.mDataRoamingNotifLog.log("Show");
            Message messageObtainMessage = this.mHandler.obtainMessage(10);
            messageObtainMessage.arg1 = this.mDefaultDataSubId;
            messageObtainMessage.sendToTarget();
            return;
        }
        if (this.mNoDataDueToRoaming) {
            if (zIsDataAllowed || !dataConnectionReasons.containsOnly(DataConnectionReasons.DataDisallowedReasonType.ROAMING_DISABLED)) {
                this.mNoDataDueToRoaming = false;
                Log.d(LOG_TAG, "Dismiss roaming disconnected notification");
                this.mDataRoamingNotifLog.log("Hide. data allowed=" + zIsDataAllowed + ", reasons=" + dataConnectionReasons);
                this.mHandler.sendEmptyMessage(11);
            }
        }
    }

    public Phone getPhoneInEcm() {
        return this.phoneInEcm;
    }

    public void refreshMwiIndicator(int i) {
        this.notificationMgr.refreshMwi(i);
    }

    public void clearMwiIndicator(int i) {
        Phone phone = getPhone(i);
        if (phone == null) {
            Log.w(LOG_TAG, "clearMwiIndicator on null phone, subId:" + i);
            return;
        }
        phone.setVoiceMessageCount(0);
    }

    public void setShouldCheckVisualVoicemailConfigurationForMwi(int i, boolean z) {
        this.notificationMgr.setShouldCheckVisualVoicemailConfigurationForMwi(i, z);
    }

    public boolean isAllowAirplaneModeChange() {
        return RadioManager.getInstance().isAllowAirplaneModeChange();
    }

    public void addSubInfoUpdateListener(SubInfoUpdateListener subInfoUpdateListener) {
        Preconditions.checkNotNull(subInfoUpdateListener);
        this.mSubInfoUpdateListeners.add(subInfoUpdateListener);
    }

    public void removeSubInfoUpdateListener(SubInfoUpdateListener subInfoUpdateListener) {
        Preconditions.checkNotNull(subInfoUpdateListener);
        this.mSubInfoUpdateListeners.remove(subInfoUpdateListener);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        indentingPrintWriter.println("------- PhoneGlobals -------");
        indentingPrintWriter.increaseIndent();
        indentingPrintWriter.println("mNoDataDueToRoaming=" + this.mNoDataDueToRoaming);
        indentingPrintWriter.println("mDefaultDataSubId=" + this.mDefaultDataSubId);
        indentingPrintWriter.println("mDataRoamingNotifLog:");
        indentingPrintWriter.increaseIndent();
        this.mDataRoamingNotifLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.decreaseIndent();
        indentingPrintWriter.println("------- End PhoneGlobals -------");
    }
}
