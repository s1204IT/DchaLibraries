package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.RadioAccessFamily;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.uicc.UiccController;
import com.mediatek.internal.telephony.ITelephonyEx;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.internal.telephony.uicc.UsimPBMemInfo;
import com.mediatek.internal.telephony.worldphone.WorldPhoneUtil;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyController {
    private static final int EVENT_APPLY_RC_RESPONSE = 3;
    private static final int EVENT_FINISH_RC_RESPONSE = 4;
    private static final int EVENT_NOTIFICATION_RC_CHANGED = 1;
    private static final int EVENT_RADIO_AVAILABLE = 6;
    private static final int EVENT_START_RC_RESPONSE = 2;
    private static final int EVENT_TIMEOUT = 5;
    static final String LOG_TAG = "ProxyController";
    private static final String MTK_C2K_SUPPORT = "ro.boot.opt_c2k_support";
    private static final int RC_RETRY_CAUSE_AIRPLANE_MODE = 5;
    private static final int RC_RETRY_CAUSE_CAPABILITY_SWITCHING = 2;
    private static final int RC_RETRY_CAUSE_IN_CALL = 3;
    private static final int RC_RETRY_CAUSE_NONE = 0;
    private static final int RC_RETRY_CAUSE_RADIO_UNAVAILABLE = 4;
    private static final int RC_RETRY_CAUSE_WORLD_MODE_SWITCHING = 1;
    private static final int SET_RC_STATUS_APPLYING = 3;
    private static final int SET_RC_STATUS_FAIL = 5;
    private static final int SET_RC_STATUS_IDLE = 0;
    private static final int SET_RC_STATUS_STARTED = 2;
    private static final int SET_RC_STATUS_STARTING = 1;
    private static final int SET_RC_STATUS_SUCCESS = 4;
    private static final int SET_RC_TIMEOUT_WAITING_MSEC = 45000;
    private static ProxyController sProxyController;
    private CommandsInterface[] mCi;
    private Context mContext;
    private String[] mCurrentLogicalModemIds;
    private boolean mIsCapSwitching;
    private String[] mNewLogicalModemIds;
    private int[] mNewRadioAccessFamily;
    private int[] mOldRadioAccessFamily;
    private PhoneSubInfoController mPhoneSubInfoController;
    private PhoneSwitcher mPhoneSwitcher;
    private Phone[] mPhones;
    private int mRadioAccessFamilyStatusCounter;
    private int mRadioCapabilitySessionId;
    private int[] mSetRadioAccessFamilyStatus;
    private int mSetRafRetryCause;
    private UiccController mUiccController;
    private UiccPhoneBookController mUiccPhoneBookController;
    private UiccSmsController mUiccSmsController;
    PowerManager.WakeLock mWakeLock;
    private boolean mTransactionFailed = false;
    private AtomicInteger mUniqueIdGenerator = new AtomicInteger(new Random().nextInt());
    private boolean mHasRegisterWorldModeReceiver = false;
    private boolean mHasRegisterPhoneStateReceiver = false;
    private boolean mHasRegisterEccStateReceiver = false;
    RadioAccessFamily[] mNextRafs = null;
    private int onExceptionCount = 0;
    private long mDoSimSwitchTime = 0;
    protected BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            ProxyController.this.logd("onReceive: action=" + action);
            if (!"android.intent.action.AIRPLANE_MODE".equals(action)) {
                return;
            }
            boolean mAirplaneModeOn = intent.getBooleanExtra("state", false);
            ProxyController.this.logd("ACTION_AIRPLANE_MODE_CHANGED, enabled = " + mAirplaneModeOn);
            if (mAirplaneModeOn || ProxyController.this.mSetRafRetryCause != 5) {
                return;
            }
            ProxyController.this.mSetRafRetryCause = 0;
            try {
                if (ProxyController.this.setRadioCapability(ProxyController.this.mNextRafs)) {
                    return;
                }
                ProxyController.this.sendCapabilityFailBroadcast();
            } catch (RuntimeException e) {
                ProxyController.this.sendCapabilityFailBroadcast();
            }
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            ProxyController.this.logd("handleMessage msg.what=" + msg.what);
            switch (msg.what) {
                case 1:
                    ProxyController.this.onNotificationRadioCapabilityChanged(msg);
                    break;
                case 2:
                    ProxyController.this.onStartRadioCapabilityResponse(msg);
                    break;
                case 3:
                    ProxyController.this.onApplyRadioCapabilityResponse(msg);
                    break;
                case 4:
                    ProxyController.this.onFinishRadioCapabilityResponse(msg);
                    break;
                case 5:
                    ProxyController.this.onTimeoutRadioCapability(msg);
                    break;
                case 6:
                    ProxyController.this.onRetryWhenRadioAvailable(msg);
                    break;
            }
        }
    };
    private BroadcastReceiver mWorldModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            ProxyController.this.logd("mWorldModeReceiver: action = " + action);
            if (!"android.intent.action.ACTION_WORLD_MODE_CHANGED".equals(action)) {
                return;
            }
            int wmState = intent.getIntExtra("worldModeState", -1);
            ProxyController.this.logd("wmState: " + wmState);
            if (wmState != 1 || ProxyController.this.mNextRafs == null || ProxyController.this.mSetRafRetryCause != 1) {
                return;
            }
            try {
                if (ProxyController.this.setRadioCapability(ProxyController.this.mNextRafs)) {
                    return;
                }
                ProxyController.this.sendCapabilityFailBroadcast();
            } catch (RuntimeException e) {
                ProxyController.this.sendCapabilityFailBroadcast();
            }
        }
    };
    private BroadcastReceiver mPhoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String str = TelephonyManager.EXTRA_STATE_OFFHOOK;
            ProxyController.this.logd("mPhoneStateReceiver: action = " + action);
            if (!"android.intent.action.PHONE_STATE".equals(action)) {
                return;
            }
            String phoneState = intent.getStringExtra("state");
            ProxyController.this.logd("phoneState: " + phoneState);
            if (!TelephonyManager.EXTRA_STATE_IDLE.equals(phoneState) || ProxyController.this.mNextRafs == null || ProxyController.this.mSetRafRetryCause != 3) {
                return;
            }
            try {
                if (ProxyController.this.setRadioCapability(ProxyController.this.mNextRafs)) {
                    return;
                }
                ProxyController.this.sendCapabilityFailBroadcast();
            } catch (RuntimeException e) {
                ProxyController.this.sendCapabilityFailBroadcast();
            }
        }
    };
    private BroadcastReceiver mEccStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ProxyController.this.logd("mEccStateReceiver, received " + intent.getAction());
            if (ProxyController.this.isEccInProgress() || ProxyController.this.mNextRafs == null || ProxyController.this.mSetRafRetryCause != 3) {
                return;
            }
            try {
                if (ProxyController.this.setRadioCapability(ProxyController.this.mNextRafs)) {
                    return;
                }
                ProxyController.this.sendCapabilityFailBroadcast();
            } catch (RuntimeException e) {
                ProxyController.this.sendCapabilityFailBroadcast();
            }
        }
    };

    public static ProxyController getInstance(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher ps) {
        if (sProxyController == null) {
            sProxyController = new ProxyController(context, phone, uiccController, ci, ps);
        }
        return sProxyController;
    }

    public static ProxyController getInstance() {
        return sProxyController;
    }

    private ProxyController(Context context, Phone[] phone, UiccController uiccController, CommandsInterface[] ci, PhoneSwitcher phoneSwitcher) {
        logd("Constructor - Enter");
        this.mContext = context;
        this.mPhones = phone;
        this.mUiccController = uiccController;
        this.mCi = ci;
        this.mPhoneSwitcher = phoneSwitcher;
        this.mUiccPhoneBookController = new UiccPhoneBookController(this.mPhones);
        this.mPhoneSubInfoController = new PhoneSubInfoController(this.mContext, this.mPhones);
        this.mUiccSmsController = new UiccSmsController(this.mPhones);
        this.mSetRadioAccessFamilyStatus = new int[this.mPhones.length];
        this.mNewRadioAccessFamily = new int[this.mPhones.length];
        this.mOldRadioAccessFamily = new int[this.mPhones.length];
        this.mCurrentLogicalModemIds = new String[this.mPhones.length];
        this.mNewLogicalModemIds = new String[this.mPhones.length];
        PowerManager pm = (PowerManager) context.getSystemService("power");
        this.mWakeLock = pm.newWakeLock(1, LOG_TAG);
        this.mWakeLock.setReferenceCounted(false);
        clearTransaction();
        for (int i = 0; i < this.mPhones.length; i++) {
            this.mPhones[i].registerForRadioCapabilityChanged(this.mHandler, 1, null);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        logd("Constructor - Exit");
    }

    public void updateDataConnectionTracker(int sub) {
        this.mPhones[sub].updateDataConnectionTracker();
    }

    public void enableDataConnectivity(int sub) {
        this.mPhones[sub].setInternalDataEnabled(true, null);
    }

    public void disableDataConnectivity(int sub, Message dataCleanedUpMsg) {
        this.mPhones[sub].setInternalDataEnabled(false, dataCleanedUpMsg);
    }

    public void updateCurrentCarrierInProvider(int sub) {
        this.mPhones[sub].updateCurrentCarrierInProvider();
    }

    public void registerForAllDataDisconnected(int subId, Handler h, int what, Object obj) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return;
        }
        this.mPhones[phoneId].registerForAllDataDisconnected(h, what, obj);
    }

    public void unregisterForAllDataDisconnected(int subId, Handler h) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId < 0 || phoneId >= TelephonyManager.getDefault().getPhoneCount()) {
            return;
        }
        this.mPhones[phoneId].unregisterForAllDataDisconnected(h);
    }

    public boolean isDataDisconnected(int subId) {
        int phoneId = SubscriptionController.getInstance().getPhoneId(subId);
        if (phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount()) {
            return this.mPhones[phoneId].mDcTracker.isDisconnected();
        }
        return true;
    }

    public int getRadioAccessFamily(int phoneId) {
        if (phoneId >= this.mPhones.length) {
            return 1;
        }
        return this.mPhones[phoneId].getRadioAccessFamily();
    }

    public boolean setRadioCapability(RadioAccessFamily[] rafs) {
        if (rafs.length != this.mPhones.length) {
            throw new RuntimeException("Length of input rafs must equal to total phone count");
        }
        if (SystemProperties.getBoolean("ro.mtk_disable_cap_switch", false)) {
            completeRadioCapabilityTransaction();
            logd("skip switching because mtk_disable_cap_switch is true");
            return true;
        }
        this.mNextRafs = rafs;
        if (WorldPhoneUtil.isWorldPhoneSwitching()) {
            logd("world mode switching");
            if (!this.mHasRegisterWorldModeReceiver) {
                registerWorldModeReceiver();
            }
            this.mSetRafRetryCause = 1;
            return true;
        }
        if (this.mSetRafRetryCause == 1 && this.mHasRegisterWorldModeReceiver) {
            unRegisterWorldModeReceiver();
            this.mSetRafRetryCause = 0;
        }
        if (SystemProperties.getInt("gsm.gcf.testmode", 0) == 2) {
            completeRadioCapabilityTransaction();
            logd("skip switching because FTA mode");
            return true;
        }
        if (SystemProperties.getInt("persist.radio.simswitch.emmode", 1) == 0) {
            completeRadioCapabilityTransaction();
            logd("skip switching because EM disable mode");
            return true;
        }
        if (TelephonyManager.getDefault().getCallState() != 0) {
            logd("setCapability in calling, fail to set RAT for phones");
            if (!this.mHasRegisterPhoneStateReceiver) {
                registerPhoneStateReceiver();
            }
            this.mSetRafRetryCause = 3;
            this.mNextRafs = rafs;
            return false;
        }
        if (isEccInProgress()) {
            logd("setCapability in ECC, fail to set RAT for phones");
            if (!this.mHasRegisterEccStateReceiver) {
                registerEccStateReceiver();
            }
            this.mSetRafRetryCause = 3;
            return false;
        }
        if (this.mSetRafRetryCause == 3) {
            if (this.mHasRegisterPhoneStateReceiver) {
                unRegisterPhoneStateReceiver();
                this.mSetRafRetryCause = 0;
            }
            if (this.mHasRegisterEccStateReceiver) {
                unRegisterEccStateReceiver();
                this.mSetRafRetryCause = 0;
            }
        }
        int airplaneMode = Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
        if (airplaneMode > 0) {
            logd("airplane mode is on, fail to set RAT for phones");
            this.mSetRafRetryCause = 5;
            this.mNextRafs = rafs;
            return false;
        }
        if (this.mIsCapSwitching) {
            logd("keep it and return,because capability swithing");
            this.mSetRafRetryCause = 2;
            this.mNextRafs = rafs;
            return true;
        }
        if (this.mSetRafRetryCause == 2) {
            logd("setCapability, mIsCapSwitching is not switching, can switch");
            this.mSetRafRetryCause = 0;
        }
        for (int i = 0; i < this.mPhones.length; i++) {
            if (!this.mPhones[i].isRadioAvailable()) {
                this.mSetRafRetryCause = 4;
                this.mCi[i].registerForAvailable(this.mHandler, 6, null);
                logd("setCapability fail,Phone" + i + " is not available");
                this.mNextRafs = rafs;
                return false;
            }
            if (this.mSetRafRetryCause == 4) {
                this.mCi[i].unregisterForAvailable(this.mHandler);
                if (i == this.mPhones.length - 1) {
                    this.mSetRafRetryCause = 0;
                }
            }
        }
        logd("setCapability,All Phones is available");
        int switchStatus = Integer.valueOf(SystemProperties.get("persist.radio.simswitch", "1")).intValue();
        boolean bIsboth3G = false;
        int newMajorPhoneId = 0;
        for (int i2 = 0; i2 < rafs.length; i2++) {
            boolean bIsMajorPhone = false;
            if ((rafs[i2].getRadioAccessFamily() & 2) > 0) {
                bIsMajorPhone = true;
            }
            if (bIsMajorPhone) {
                newMajorPhoneId = rafs[i2].getPhoneId();
                if (newMajorPhoneId == switchStatus - 1) {
                    logd("no change, skip setRadioCapability");
                    this.mSetRafRetryCause = 0;
                    this.mNextRafs = null;
                    completeRadioCapabilityTransaction();
                    return true;
                }
                if (bIsboth3G) {
                    logd("set more than one 3G phone, fail");
                    throw new RuntimeException("input parameter is incorrect");
                }
                bIsboth3G = true;
            }
        }
        if (!bIsboth3G) {
            throw new RuntimeException("input parameter is incorrect - no 3g phone");
        }
        if (SystemProperties.getInt("ro.mtk_external_sim_support", 0) == 1) {
            for (int i3 = 0; i3 < this.mPhones.length; i3++) {
                TelephonyManager.getDefault();
                String isVsimEnabled = TelephonyManager.getTelephonyProperty(i3, "gsm.external.sim.enabled", "0");
                TelephonyManager.getDefault();
                String isVsimInserted = TelephonyManager.getTelephonyProperty(i3, "gsm.external.sim.inserted", "0");
                int defaultPhoneId = SubscriptionManager.getPhoneId(SubscriptionManager.getDefaultDataSubscriptionId());
                if ("1".equals(isVsimEnabled) && (("0".equals(isVsimInserted) || UsimPBMemInfo.STRING_NOT_SET.equals(isVsimInserted)) && newMajorPhoneId != defaultPhoneId)) {
                    throw new RuntimeException("vsim not ready, can't switch to another sim!");
                }
            }
            int mainPhoneId = RadioCapabilitySwitchUtil.getMainCapabilityPhoneId();
            TelephonyManager.getDefault();
            String isVsimEnabledOnMain = TelephonyManager.getTelephonyProperty(mainPhoneId, "gsm.external.sim.enabled", "0");
            TelephonyManager.getDefault();
            String mainPhoneIdSimType = TelephonyManager.getTelephonyProperty(mainPhoneId, "gsm.external.sim.inserted", "0");
            if (isVsimEnabledOnMain.equals("1") && mainPhoneIdSimType.equals(Phone.ACT_TYPE_UTRAN)) {
                throw new RuntimeException("vsim enabled, can't switch to another sim!");
            }
        }
        switch (RadioCapabilitySwitchUtil.isNeedSwitchInOpPackage(this.mPhones, rafs)) {
            case 0:
                logd("do setRadioCapability");
                synchronized (this.mSetRadioAccessFamilyStatus) {
                    for (int i4 = 0; i4 < this.mPhones.length; i4++) {
                        if (this.mSetRadioAccessFamilyStatus[i4] != 0) {
                            loge("setRadioCapability: Phone[" + i4 + "] is not idle. Rejecting request.");
                            return false;
                        }
                        break;
                    }
                    boolean same = true;
                    for (int i5 = 0; i5 < this.mPhones.length; i5++) {
                        if (this.mPhones[i5].getRadioAccessFamily() != rafs[i5].getRadioAccessFamily()) {
                            same = false;
                        }
                    }
                    if (same) {
                        logd("setRadioCapability: Already in requested configuration, nothing to do.");
                        return true;
                    }
                    if (!WorldPhoneUtil.isWorldModeSupport() && WorldPhoneUtil.isWorldPhoneSupport()) {
                        PhoneFactory.getWorldPhone().notifyRadioCapabilityChange(newMajorPhoneId);
                    }
                    clearTransaction();
                    this.mWakeLock.acquire();
                    return doSetRadioCapabilities(rafs);
                }
            case 1:
                logd("no change in op check, skip setRadioCapability");
                completeRadioCapabilityTransaction();
                return true;
            case 2:
                logd("Sim status/info is not ready, skip setRadioCapability");
                return true;
            default:
                logd("should not be here...!!");
                return true;
        }
    }

    private boolean doSetRadioCapabilities(RadioAccessFamily[] rafs) {
        this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
        Message msg = this.mHandler.obtainMessage(5, this.mRadioCapabilitySessionId, 0);
        this.mHandler.sendMessageDelayed(msg, 45000L);
        this.mDoSimSwitchTime = System.currentTimeMillis() / 1000;
        SystemProperties.set("ril.time.stamp", Long.toString(this.mDoSimSwitchTime));
        SystemProperties.set("ril.switch.session.id", Integer.toString(this.mRadioCapabilitySessionId));
        logd("setRadioCapability: timestamp =" + this.mDoSimSwitchTime);
        this.mIsCapSwitching = true;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("setRadioCapability: new request session id=" + this.mRadioCapabilitySessionId);
            resetRadioAccessFamilyStatusCounter();
            this.onExceptionCount = 0;
            for (int i = 0; i < rafs.length; i++) {
                int phoneId = rafs[i].getPhoneId();
                this.mSetRadioAccessFamilyStatus[phoneId] = 1;
                this.mOldRadioAccessFamily[phoneId] = this.mPhones[phoneId].getRadioAccessFamily();
                int requestedRaf = rafs[i].getRadioAccessFamily();
                this.mNewRadioAccessFamily[phoneId] = requestedRaf;
                this.mCurrentLogicalModemIds[phoneId] = this.mPhones[phoneId].getModemUuId();
                this.mNewLogicalModemIds[phoneId] = getLogicalModemIdFromRaf(requestedRaf);
                logd("setRadioCapability: phoneId=" + phoneId + " status=STARTINGmOldRadioAccessFamily[" + phoneId + "]=" + this.mOldRadioAccessFamily[phoneId] + "mNewRadioAccessFamily[" + phoneId + "]=" + this.mNewRadioAccessFamily[phoneId]);
                sendRadioCapabilityRequest(phoneId, this.mRadioCapabilitySessionId, 1, this.mOldRadioAccessFamily[phoneId], this.mCurrentLogicalModemIds[phoneId], 0, 2);
            }
        }
        return true;
    }

    private void onStartRadioCapabilityResponse(Message msg) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception != null) {
                if (this.onExceptionCount == 0) {
                    CommandException.Error err = null;
                    this.onExceptionCount = 1;
                    if (ar.exception instanceof CommandException) {
                        err = ((CommandException) ar.exception).getCommandError();
                    }
                    if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        this.mSetRafRetryCause = 4;
                        for (int i = 0; i < this.mPhones.length; i++) {
                            this.mCi[i].registerForAvailable(this.mHandler, 6, null);
                        }
                        loge("onStartRadioCapabilityResponse: Retry later due to modem off");
                    }
                }
                logd("onStartRadioCapabilityResponse got exception=" + ar.exception);
                this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
                Intent intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
                this.mContext.sendBroadcast(intent);
                clearTransaction();
                return;
            }
            RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
            if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
                logd("onStartRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }
            this.mRadioAccessFamilyStatusCounter--;
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null) {
                logd("onStartRadioCapabilityResponse: Error response session=" + rc.getSession());
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            } else {
                logd("onStartRadioCapabilityResponse: phoneId=" + id + " status=STARTED");
                this.mSetRadioAccessFamilyStatus[id] = 2;
            }
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                logd("onStartRadioCapabilityResponse: success=" + (!this.mTransactionFailed));
                if (this.mTransactionFailed) {
                    issueFinish(this.mRadioCapabilitySessionId);
                } else {
                    resetRadioAccessFamilyStatusCounter();
                    for (int i2 = 0; i2 < this.mPhones.length; i2++) {
                        sendRadioCapabilityRequest(i2, this.mRadioCapabilitySessionId, 2, this.mNewRadioAccessFamily[i2], this.mNewLogicalModemIds[i2], 0, 3);
                        logd("onStartRadioCapabilityResponse: phoneId=" + i2 + " status=APPLYING");
                        this.mSetRadioAccessFamilyStatus[i2] = 3;
                    }
                }
            }
        }
    }

    private void onApplyRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        AsyncResult ar = (AsyncResult) msg.obj;
        CommandException.Error err = null;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            if (rc == null && ar.exception != null && this.onExceptionCount == 0) {
                this.onExceptionCount = 1;
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) ar.exception).getCommandError();
                }
                if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    this.mSetRafRetryCause = 4;
                    for (int i = 0; i < this.mPhones.length; i++) {
                        this.mCi[i].registerForAvailable(this.mHandler, 6, null);
                    }
                    loge("onApplyRadioCapabilityResponse: Retry later due to RADIO_NOT_AVAILABLE");
                } else {
                    loge("onApplyRadioCapabilityResponse: exception=" + ar.exception);
                }
                this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
                Intent intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
                this.mContext.sendBroadcast(intent);
                clearTransaction();
                return;
            }
            logd("onApplyRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        logd("onApplyRadioCapabilityResponse: rc=" + rc);
        if (((AsyncResult) msg.obj).exception != null) {
            synchronized (this.mSetRadioAccessFamilyStatus) {
                logd("onApplyRadioCapabilityResponse: Error response session=" + rc.getSession());
                int id = rc.getPhoneId();
                if (ar.exception instanceof CommandException) {
                    err = ((CommandException) ar.exception).getCommandError();
                }
                if (err == CommandException.Error.RADIO_NOT_AVAILABLE) {
                    this.mSetRafRetryCause = 4;
                    this.mCi[id].registerForAvailable(this.mHandler, 6, null);
                    loge("onApplyRadioCapabilityResponse: Retry later due to modem off");
                } else {
                    loge("onApplyRadioCapabilityResponse: exception=" + ar.exception);
                }
                logd("onApplyRadioCapabilityResponse: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            }
            return;
        }
        logd("onApplyRadioCapabilityResponse: Valid start expecting notification rc=" + rc);
    }

    private void onNotificationRadioCapabilityChanged(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        if (!this.mIsCapSwitching) {
            logd("radio change is not triggered by sim switch, notification should be ignore");
            clearTransaction();
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd("onNotificationRadioCapabilityChanged: rc=" + rc);
            if (rc.getSession() != this.mRadioCapabilitySessionId) {
                logd("onNotificationRadioCapabilityChanged: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
                return;
            }
            int id = rc.getPhoneId();
            if (((AsyncResult) msg.obj).exception != null || rc.getStatus() == 2) {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=FAIL");
                this.mSetRadioAccessFamilyStatus[id] = 5;
                this.mTransactionFailed = true;
            } else {
                logd("onNotificationRadioCapabilityChanged: phoneId=" + id + " status=SUCCESS");
                this.mSetRadioAccessFamilyStatus[id] = 4;
                this.mPhoneSwitcher.resendDataAllowed(id);
                this.mPhones[id].radioCapabilityUpdated(rc);
            }
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                logd("onNotificationRadioCapabilityChanged: APPLY URC success=" + this.mTransactionFailed);
                issueFinish(this.mRadioCapabilitySessionId);
            }
        }
    }

    void onFinishRadioCapabilityResponse(Message msg) {
        RadioCapability rc = (RadioCapability) ((AsyncResult) msg.obj).result;
        if (rc == null || rc.getSession() != this.mRadioCapabilitySessionId) {
            if (SystemProperties.get(MTK_C2K_SUPPORT).equals("1") && rc == null && ((AsyncResult) msg.obj).exception != null) {
                synchronized (this.mSetRadioAccessFamilyStatus) {
                    logd("onFinishRadioCapabilityResponse C2K mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
                    this.mRadioAccessFamilyStatusCounter--;
                    if (this.mRadioAccessFamilyStatusCounter == 0) {
                        completeRadioCapabilityTransaction();
                    }
                }
                return;
            }
            logd("onFinishRadioCapabilityResponse: Ignore session=" + this.mRadioCapabilitySessionId + " rc=" + rc);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            logd(" onFinishRadioCapabilityResponse mRadioAccessFamilyStatusCounter=" + this.mRadioAccessFamilyStatusCounter);
            this.mRadioAccessFamilyStatusCounter--;
            if (this.mRadioAccessFamilyStatusCounter == 0) {
                completeRadioCapabilityTransaction();
            }
        }
    }

    private void onTimeoutRadioCapability(Message msg) {
        if (msg.arg1 != this.mRadioCapabilitySessionId) {
            logd("RadioCapability timeout: Ignore msg.arg1=" + msg.arg1 + "!= mRadioCapabilitySessionId=" + this.mRadioCapabilitySessionId);
            return;
        }
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("RadioCapability timeout: mSetRadioAccessFamilyStatus[" + i + "]=" + this.mSetRadioAccessFamilyStatus[i]);
            }
            int uniqueDifferentId = this.mUniqueIdGenerator.getAndIncrement();
            this.mTransactionFailed = true;
            issueFinish(uniqueDifferentId);
        }
    }

    private void issueFinish(int sessionId) {
        synchronized (this.mSetRadioAccessFamilyStatus) {
            resetRadioAccessFamilyStatusCounter();
            for (int i = 0; i < this.mPhones.length; i++) {
                logd("issueFinish: phoneId=" + i + " sessionId=" + sessionId + " mTransactionFailed=" + this.mTransactionFailed);
                sendRadioCapabilityRequest(i, sessionId, 4, this.mOldRadioAccessFamily[i], this.mCurrentLogicalModemIds[i], this.mTransactionFailed ? 2 : 1, 4);
                if (this.mTransactionFailed) {
                    logd("issueFinish: phoneId: " + i + " status: FAIL");
                    this.mSetRadioAccessFamilyStatus[i] = 5;
                }
            }
        }
    }

    private void completeRadioCapabilityTransaction() {
        Intent intent;
        logd("onFinishRadioCapabilityResponse: success=" + (!this.mTransactionFailed));
        if (!this.mTransactionFailed) {
            ArrayList<? extends Parcelable> arrayList = new ArrayList<>();
            for (int i = 0; i < this.mPhones.length; i++) {
                int raf = this.mPhones[i].getRadioAccessFamily();
                logd("radioAccessFamily[" + i + "]=" + raf);
                RadioAccessFamily phoneRC = new RadioAccessFamily(i, raf);
                arrayList.add(phoneRC);
            }
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_DONE");
            intent.putParcelableArrayListExtra("rafs", arrayList);
            this.mRadioCapabilitySessionId = this.mUniqueIdGenerator.getAndIncrement();
            clearTransaction();
        } else {
            intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
            this.mTransactionFailed = false;
            if (retryToSetRadioCapabilityIfTimeout()) {
                this.mSetRafRetryCause = 2;
            } else {
                clearTransaction();
            }
        }
        RadioCapabilitySwitchUtil.updateIccid(this.mPhones);
        this.mContext.sendBroadcast(intent, "android.permission.READ_PHONE_STATE");
        if (this.mNextRafs == null || this.mSetRafRetryCause != 2) {
            return;
        }
        logd("has next capability switch request,trigger it");
        try {
            if (!setRadioCapability(this.mNextRafs)) {
                sendCapabilityFailBroadcast();
            } else {
                this.mSetRafRetryCause = 0;
                this.mNextRafs = null;
            }
        } catch (RuntimeException e) {
            sendCapabilityFailBroadcast();
        }
    }

    private void clearTransaction() {
        logd("clearTransaction mIsCapSwitching =" + this.mIsCapSwitching);
        if (this.mIsCapSwitching) {
            this.mHandler.removeMessages(5);
        }
        this.mIsCapSwitching = false;
        synchronized (this.mSetRadioAccessFamilyStatus) {
            for (int i = 0; i < this.mPhones.length; i++) {
                this.mSetRadioAccessFamilyStatus[i] = 0;
                this.mOldRadioAccessFamily[i] = 0;
                this.mNewRadioAccessFamily[i] = 0;
                this.mTransactionFailed = false;
            }
            logd("clearTransaction: All phones status=IDLE");
            if (this.mWakeLock.isHeld()) {
                this.mWakeLock.release();
            }
        }
    }

    private void resetRadioAccessFamilyStatusCounter() {
        this.mRadioAccessFamilyStatusCounter = this.mPhones.length;
    }

    private void sendRadioCapabilityRequest(int phoneId, int sessionId, int rcPhase, int radioFamily, String logicalModemId, int status, int eventId) {
        if (logicalModemId == null || logicalModemId.equals(UsimPBMemInfo.STRING_NOT_SET)) {
            logicalModemId = "modem_sys3";
        }
        RadioCapability requestRC = new RadioCapability(phoneId, sessionId, rcPhase, radioFamily, logicalModemId, status);
        this.mPhones[phoneId].setRadioCapability(requestRC, this.mHandler.obtainMessage(eventId));
    }

    public int getMaxRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int maxNumRafBit = 0;
        int maxRaf = 1;
        for (int len = 0; len < this.mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (maxNumRafBit < numRafSupported[len]) {
                maxNumRafBit = numRafSupported[len];
                maxRaf = this.mPhones[len].getRadioAccessFamily();
            }
        }
        logd("getMaxRafSupported: maxRafBit=" + maxNumRafBit + " maxRaf=" + maxRaf + " flag=" + (maxRaf & 2));
        return maxRaf;
    }

    public int getMinRafSupported() {
        int[] numRafSupported = new int[this.mPhones.length];
        int minNumRafBit = 0;
        int minRaf = 1;
        for (int len = 0; len < this.mPhones.length; len++) {
            numRafSupported[len] = Integer.bitCount(this.mPhones[len].getRadioAccessFamily());
            if (minNumRafBit == 0 || minNumRafBit > numRafSupported[len]) {
                minNumRafBit = numRafSupported[len];
                minRaf = this.mPhones[len].getRadioAccessFamily();
            }
        }
        logd("getMinRafSupported: minRafBit=" + minNumRafBit + " minRaf=" + minRaf + " flag=" + (minRaf & 2));
        return minRaf;
    }

    private String getLogicalModemIdFromRaf(int raf) {
        for (int phoneId = 0; phoneId < this.mPhones.length; phoneId++) {
            if (this.mPhones[phoneId].getRadioAccessFamily() == raf) {
                String modemUuid = this.mPhones[phoneId].getModemUuId();
                return modemUuid;
            }
        }
        return null;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }

    private void loge(String string) {
        Rlog.e(LOG_TAG, string);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        try {
            this.mPhoneSwitcher.dump(fd, pw, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isCapabilitySwitching() {
        return this.mIsCapSwitching;
    }

    private boolean retryToSetRadioCapabilityIfTimeout() {
        int iRet = SystemProperties.getInt("ril.switch.result", 0);
        SystemProperties.set("ril.switch.result", "0");
        logd("retryToSetRadioCapabilityIfTimeout ret = " + iRet);
        return iRet == 1;
    }

    private void onRetryWhenRadioAvailable(Message msg) {
        logd("onRetryWhenRadioAvailable,mSetRafRetryCause:" + this.mSetRafRetryCause);
        for (int i = 0; i < this.mPhones.length; i++) {
            if (RadioManager.isModemPowerOff(i)) {
                logd("onRetryWhenRadioAvailable, Phone" + i + " modem off");
                return;
            }
        }
        if (this.mNextRafs == null || this.mSetRafRetryCause != 4) {
            return;
        }
        try {
            setRadioCapability(this.mNextRafs);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private void sendCapabilityFailBroadcast() {
        if (this.mContext == null) {
            return;
        }
        Intent intent = new Intent("android.intent.action.ACTION_SET_RADIO_CAPABILITY_FAILED");
        this.mContext.sendBroadcast(intent);
    }

    private void registerWorldModeReceiver() {
        if (this.mContext == null) {
            logd("registerWorldModeReceiver, context is null => return");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.ACTION_WORLD_MODE_CHANGED");
        this.mContext.registerReceiver(this.mWorldModeReceiver, filter);
        this.mHasRegisterWorldModeReceiver = true;
    }

    private void unRegisterWorldModeReceiver() {
        if (this.mContext == null) {
            logd("unRegisterWorldModeReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mWorldModeReceiver);
            this.mHasRegisterWorldModeReceiver = false;
        }
    }

    private void registerPhoneStateReceiver() {
        if (this.mContext == null) {
            logd("registerPhoneStateReceiver, context is null => return");
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PHONE_STATE");
        this.mContext.registerReceiver(this.mPhoneStateReceiver, filter);
        this.mHasRegisterPhoneStateReceiver = true;
    }

    private void unRegisterPhoneStateReceiver() {
        if (this.mContext == null) {
            logd("unRegisterPhoneStateReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mPhoneStateReceiver);
            this.mHasRegisterPhoneStateReceiver = false;
        }
    }

    private void registerEccStateReceiver() {
        if (this.mContext == null) {
            logd("registerEccStateReceiver, context is null => return");
            return;
        }
        IntentFilter filter = new IntentFilter("android.intent.action.ECC_IN_PROGRESS");
        filter.addAction("android.intent.action.EMERGENCY_CALLBACK_MODE_CHANGED");
        this.mContext.registerReceiver(this.mEccStateReceiver, filter);
        this.mHasRegisterEccStateReceiver = true;
    }

    private void unRegisterEccStateReceiver() {
        if (this.mContext == null) {
            logd("unRegisterEccStateReceiver, context is null => return");
        } else {
            this.mContext.unregisterReceiver(this.mEccStateReceiver);
            this.mHasRegisterEccStateReceiver = false;
        }
    }

    private boolean isEccInProgress() {
        String value = SystemProperties.get("ril.cdma.inecmmode", UsimPBMemInfo.STRING_NOT_SET);
        boolean inEcm = value.contains("true");
        boolean isInEcc = false;
        ITelephonyEx telEx = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx"));
        if (telEx != null) {
            try {
                isInEcc = telEx.isEccInProgress();
            } catch (RemoteException e) {
                loge("Exception of isEccInProgress");
            }
        }
        logd("isEccInProgress, value:" + value + ", inEcm:" + inEcm + ", isInEcc:" + isInEcc);
        if (inEcm) {
            return true;
        }
        return isInEcc;
    }
}
