package com.android.keyguard;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.IUserSwitchObserver;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.ArraySet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.mediatek.internal.telephony.ITelephonyEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KeyguardUpdateMonitor implements TrustManager.TrustListener {

    private static final int[] f7x8dbfd0b5 = null;
    private static int sCurrentUser;
    private static KeyguardUpdateMonitor sInstance;
    private AlarmManager mAlarmManager;
    private boolean mAlternateUnlockEnabled;
    private BatteryStatus mBatteryStatus;
    private boolean mBootCompleted;
    private boolean mBouncer;
    private final Context mContext;
    private boolean mDeviceInteractive;
    private ContentObserver mDeviceProvisionedObserver;
    private boolean mFingerprintAlreadyAuthenticated;
    private CancellationSignal mFingerprintCancelSignal;
    private FingerprintManager mFpm;
    private boolean mGoingToSleep;
    private boolean mKeyguardIsVisible;
    private int mPhoneState;
    private int mRingMode;
    private boolean mScreenOn;
    private final StrongAuthTracker mStrongAuthTracker;
    private List<SubscriptionInfo> mSubscriptionInfo;
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    private TrustManager mTrustManager;
    private WifiManager mWifiManager;
    HashMap<Integer, SimData> mSimDatas = new HashMap<>();
    HashMap<Integer, ServiceState> mServiceStates = new HashMap<>();
    private HashMap<Integer, IccCardConstants.State> mSimStateOfPhoneId = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonyPlmn = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonySpn = new HashMap<>();
    private SparseIntArray mFailedAttempts = new SparseIntArray();
    private ArraySet<Integer> mStrongAuthNotTimedOut = new ArraySet<>();
    private final CopyOnWriteArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = new CopyOnWriteArrayList<>();
    private int mFingerprintRunningState = 0;
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 301:
                    KeyguardUpdateMonitor.this.handleTimeUpdate();
                    break;
                case 302:
                    KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) msg.obj);
                    break;
                case 303:
                    KeyguardUpdateMonitor.this.handleCarrierInfoUpdate(((Integer) msg.obj).intValue());
                    break;
                case 304:
                    KeyguardUpdateMonitor.this.handleSimStateChange((SimData) msg.obj);
                    break;
                case 305:
                    KeyguardUpdateMonitor.this.handleRingerModeChange(msg.arg1);
                    break;
                case 306:
                    KeyguardUpdateMonitor.this.handlePhoneStateChanged();
                    break;
                case 308:
                    KeyguardUpdateMonitor.this.handleDeviceProvisioned();
                    break;
                case 309:
                    KeyguardUpdateMonitor.this.handleDevicePolicyManagerStateChanged();
                    break;
                case 310:
                    KeyguardUpdateMonitor.this.handleUserSwitching(msg.arg1, (IRemoteCallback) msg.obj);
                    break;
                case 312:
                    KeyguardUpdateMonitor.this.handleKeyguardReset();
                    break;
                case 313:
                    KeyguardUpdateMonitor.this.handleBootCompleted();
                    break;
                case 314:
                    KeyguardUpdateMonitor.this.handleUserSwitchComplete(msg.arg1);
                    break;
                case 317:
                    KeyguardUpdateMonitor.this.handleUserInfoChanged(msg.arg1);
                    break;
                case 318:
                    KeyguardUpdateMonitor.this.handleReportEmergencyCallAction();
                    break;
                case 319:
                    KeyguardUpdateMonitor.this.handleStartedWakingUp();
                    break;
                case 320:
                    KeyguardUpdateMonitor.this.handleFinishedGoingToSleep(msg.arg1);
                    break;
                case 321:
                    KeyguardUpdateMonitor.this.handleStartedGoingToSleep(msg.arg1);
                    break;
                case 322:
                    KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(msg.arg1);
                    break;
                case 327:
                    KeyguardUpdateMonitor.this.handleFaceUnlockStateChanged(msg.arg1 != 0, msg.arg2);
                    break;
                case 328:
                    KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                    break;
                case 329:
                    KeyguardUpdateMonitor.this.handleAirplaneModeChanged();
                    break;
                case 330:
                    KeyguardUpdateMonitor.this.handleServiceStateChange(msg.arg1, (ServiceState) msg.obj);
                    break;
                case 331:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                    break;
                case 332:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOff();
                    break;
                case 1015:
                    KeyguardUpdateMonitor.this.handleAirPlaneModeUpdate(((Boolean) msg.obj).booleanValue());
                    break;
            }
        }
    };
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            KeyguardUpdateMonitor.this.mHandler.removeMessages(328);
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(328);
        }
    };
    private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
    private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();
    private SparseBooleanArray mUserFingerprintAuthenticated = new SparseBooleanArray();
    private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
    private DisplayClientState mDisplayClientState = new DisplayClientState();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("KeyguardUpdateMonitor", "received broadcast " + action);
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
                return;
            }
            if ("android.provider.Telephony.SPN_STRINGS_UPDATED".equals(action)) {
                int subId = intent.getIntExtra("subscription", -1);
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, sub Id = " + subId);
                int phoneId = KeyguardUtils.getPhoneIdUsingSubId(subId);
                if (!KeyguardUtils.isValidPhoneId(phoneId)) {
                    Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, invalid phoneId = " + phoneId);
                    return;
                }
                KeyguardUpdateMonitor.this.mTelephonyPlmn.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyPlmnFrom(intent));
                KeyguardUpdateMonitor.this.mTelephonySpn.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonySpnFrom(intent));
                KeyguardUpdateMonitor.this.mTelephonyCsgId.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyCsgIdFrom(intent));
                KeyguardUpdateMonitor.this.mTelephonyHnbName.put(Integer.valueOf(phoneId), KeyguardUpdateMonitor.this.getTelephonyHnbNameFrom(intent));
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, update phoneId=" + phoneId + ", plmn=" + KeyguardUpdateMonitor.this.mTelephonyPlmn.get(Integer.valueOf(phoneId)) + ", spn=" + KeyguardUpdateMonitor.this.mTelephonySpn.get(Integer.valueOf(phoneId)) + ", csgId=" + KeyguardUpdateMonitor.this.mTelephonyCsgId.get(Integer.valueOf(phoneId)) + ", hnbName=" + KeyguardUpdateMonitor.this.mTelephonyHnbName.get(Integer.valueOf(phoneId)));
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(303, Integer.valueOf(phoneId)));
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int status = intent.getIntExtra("status", 1);
                int plugged = intent.getIntExtra("plugged", 0);
                int level = intent.getIntExtra("level", 0);
                int health = intent.getIntExtra("health", 1);
                int maxChargingMicroAmp = intent.getIntExtra("max_charging_current", -1);
                int maxChargingMicroVolt = intent.getIntExtra("max_charging_voltage", -1);
                if (maxChargingMicroVolt <= 0) {
                    maxChargingMicroVolt = 5000000;
                }
                int maxChargingMicroWatt = maxChargingMicroAmp > 0 ? (maxChargingMicroAmp / 1000) * (maxChargingMicroVolt / 1000) : -1;
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(302, new BatteryStatus(status, level, plugged, health, maxChargingMicroWatt)));
                return;
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action) || "mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                String stateExtra = intent.getStringExtra("ss");
                SimData simArgs = SimData.fromIntent(intent);
                Log.v("KeyguardUpdateMonitor", "action=" + action + ", state=" + stateExtra + ", slotId=" + simArgs.phoneId + ", subId=" + simArgs.subId + ", simArgs.simState = " + simArgs.simState);
                if ("mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                    Log.d("KeyguardUpdateMonitor", "ACTION_UNLOCK_SIM_LOCK, set sim state as UNKNOWN");
                    KeyguardUpdateMonitor.this.mSimStateOfPhoneId.put(Integer.valueOf(simArgs.phoneId), IccCardConstants.State.UNKNOWN);
                }
                KeyguardUpdateMonitor.this.proceedToHandleSimStateChanged(simArgs);
                return;
            }
            if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(305, intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1), 0));
                return;
            }
            if ("android.intent.action.PHONE_STATE".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(306, intent.getStringExtra("state")));
                return;
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                KeyguardUpdateMonitor.this.dispatchBootCompleted();
                return;
            }
            if ("android.intent.action.AIRPLANE_MODE".equals(action)) {
                boolean state = intent.getBooleanExtra("state", false);
                Log.d("KeyguardUpdateMonitor", "Receive ACTION_AIRPLANE_MODE_CHANGED, state = " + state);
                Message msg = new Message();
                msg.what = 1015;
                msg.obj = new Boolean(state);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(msg);
                return;
            }
            if ("android.intent.action.SERVICE_STATE".equals(action)) {
                ServiceState serviceState = ServiceState.newFromBundle(intent.getExtras());
                int subId2 = intent.getIntExtra("subscription", -1);
                Log.v("KeyguardUpdateMonitor", "action " + action + " serviceState=" + serviceState + " subId=" + subId2);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(330, subId2, 0, serviceState));
            }
        }
    };
    private final BroadcastReceiver mBroadcastAllReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
                return;
            }
            if ("android.intent.action.USER_INFO_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(317, intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId()), 0));
                return;
            }
            if ("com.android.facelock.FACE_UNLOCK_STARTED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 1, getSendingUserId()));
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 0, getSendingUserId()));
            } else {
                if (!"android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                    return;
                }
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            }
        }
    };
    private final BroadcastReceiver mStrongAuthTimeoutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!"com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT".equals(intent.getAction())) {
                return;
            }
            int userId = intent.getIntExtra("com.android.systemui.USER_ID", -1);
            KeyguardUpdateMonitor.this.mStrongAuthNotTimedOut.remove(Integer.valueOf(userId));
            KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
        }
    };
    private final FingerprintManager.LockoutResetCallback mLockoutResetCallback = new FingerprintManager.LockoutResetCallback() {
        public void onLockoutReset() {
            KeyguardUpdateMonitor.this.handleFingerprintLockoutReset();
        }
    };
    private FingerprintManager.AuthenticationCallback mAuthenticationCallback = new FingerprintManager.AuthenticationCallback() {
        @Override
        public void onAuthenticationFailed() {
            KeyguardUpdateMonitor.this.handleFingerprintAuthFailed();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            KeyguardUpdateMonitor.this.handleFingerprintAuthenticated(result.getUserId());
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            KeyguardUpdateMonitor.this.handleFingerprintHelp(helpMsgId, helpString.toString());
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            KeyguardUpdateMonitor.this.handleFingerprintError(errMsgId, errString.toString());
        }

        public void onAuthenticationAcquired(int acquireInfo) {
            KeyguardUpdateMonitor.this.handleFingerprintAcquired(acquireInfo);
        }
    };
    private boolean mNewClientRegUpdateMonitor = false;
    private boolean mShowing = true;
    private HashMap<Integer, Integer> mSimMeCategory = new HashMap<>();
    private HashMap<Integer, Integer> mSimMeLeftRetryCount = new HashMap<>();
    private int mPinPukMeDismissFlag = 0;
    private HashMap<Integer, CharSequence> mTelephonyHnbName = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonyCsgId = new HashMap<>();
    private int mFailedBiometricUnlockAttempts = 0;
    private boolean mDeviceProvisioned = isDeviceProvisionedInSettingsDb();

    private static int[] m496xf663cf59() {
        if (f7x8dbfd0b5 != null) {
            return f7x8dbfd0b5;
        }
        int[] iArr = new int[IccCardConstants.State.values().length];
        try {
            iArr[IccCardConstants.State.ABSENT.ordinal()] = 4;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 5;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 1;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[IccCardConstants.State.NOT_READY.ordinal()] = 6;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[IccCardConstants.State.PERM_DISABLED.ordinal()] = 7;
        } catch (NoSuchFieldError e5) {
        }
        try {
            iArr[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 2;
        } catch (NoSuchFieldError e6) {
        }
        try {
            iArr[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 3;
        } catch (NoSuchFieldError e7) {
        }
        try {
            iArr[IccCardConstants.State.READY.ordinal()] = 8;
        } catch (NoSuchFieldError e8) {
        }
        try {
            iArr[IccCardConstants.State.UNKNOWN.ordinal()] = 9;
        } catch (NoSuchFieldError e9) {
        }
        f7x8dbfd0b5 = iArr;
        return iArr;
    }

    public static synchronized void setCurrentUser(int currentUser) {
        sCurrentUser = currentUser;
    }

    public static synchronized int getCurrentUser() {
        return sCurrentUser;
    }

    public void onTrustChanged(boolean enabled, int userId, int flags) {
        this.mUserHasTrust.put(userId, enabled);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTrustChanged(userId);
                if (enabled && flags != 0) {
                    cb.onTrustGrantedWithFlags(flags, userId);
                }
            }
        }
    }

    protected void handleSimSubscriptionInfoChanged() {
        Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged()");
        List<SubscriptionInfo> sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        if (sil != null) {
            for (SubscriptionInfo subInfo : sil) {
                Log.v("KeyguardUpdateMonitor", "SubInfo:" + subInfo);
            }
        } else {
            Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged: list is null");
        }
        List<SubscriptionInfo> subscriptionInfos = getSubscriptionInfo(true);
        ArrayList<SubscriptionInfo> changedSubscriptions = new ArrayList<>();
        for (int i = 0; i < subscriptionInfos.size(); i++) {
            SubscriptionInfo info = subscriptionInfos.get(i);
            boolean changed = refreshSimState(info.getSubscriptionId(), info.getSimSlotIndex());
            if (changed) {
                changedSubscriptions.add(info);
            }
        }
        for (int i2 = 0; i2 < changedSubscriptions.size(); i2++) {
            int subId = changedSubscriptions.get(i2).getSubscriptionId();
            int phoneId = changedSubscriptions.get(i2).getSimSlotIndex();
            Log.d("KeyguardUpdateMonitor", "handleSimSubscriptionInfoChanged() - call callbacks for subId = " + subId + " & phoneId = " + phoneId);
            for (int j = 0; j < this.mCallbacks.size(); j++) {
                KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(j).get();
                if (cb != null) {
                    cb.onSimStateChangedUsingPhoneId(phoneId, this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
                }
            }
        }
        for (int j2 = 0; j2 < this.mCallbacks.size(); j2++) {
            KeyguardUpdateMonitorCallback cb2 = this.mCallbacks.get(j2).get();
            if (cb2 != null) {
                cb2.onRefreshCarrierInfo();
            }
        }
    }

    public void handleAirplaneModeChanged() {
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    List<SubscriptionInfo> getSubscriptionInfo(boolean forceReload) {
        List<SubscriptionInfo> sil = this.mSubscriptionInfo;
        if (sil == null || forceReload || (sil != null && sil.size() == 0)) {
            sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (sil == null) {
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = sil;
        }
        Log.d("KeyguardUpdateMonitor", "getSubscriptionInfo() - mSubscriptionInfo.size = " + this.mSubscriptionInfo.size());
        return this.mSubscriptionInfo;
    }

    public void onTrustManagedChanged(boolean managed, int userId) {
        this.mUserTrustIsManaged.put(userId, managed);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTrustManagedChanged(userId);
            }
        }
    }

    private void onFingerprintAuthenticated(int userId) {
        this.mUserFingerprintAuthenticated.put(userId, true);
        this.mFingerprintAlreadyAuthenticated = isUnlockingWithFingerprintAllowed();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAuthenticated(userId);
            }
        }
    }

    public void handleFingerprintAuthFailed() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAuthFailed();
            }
        }
        handleFingerprintHelp(-1, this.mContext.getString(R$string.fingerprint_not_recognized));
    }

    public void handleFingerprintAcquired(int acquireInfo) {
        if (acquireInfo != 0) {
            return;
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAcquired();
            }
        }
    }

    public void handleFingerprintAuthenticated(int authUserId) {
        try {
            int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
            if (userId != authUserId) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint authenticated for wrong user: " + authUserId);
            } else if (isFingerprintDisabled(userId)) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint disabled by DPM for userId: " + userId);
            } else {
                onFingerprintAuthenticated(userId);
            }
        } catch (RemoteException e) {
            Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
        } finally {
            setFingerprintRunningState(0);
        }
    }

    public void handleFingerprintHelp(int msgId, String helpString) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintHelp(msgId, helpString);
            }
        }
    }

    public void handleFingerprintError(int msgId, String errString) {
        if (msgId == 5 && this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(0);
            startListeningForFingerprint();
        } else {
            setFingerprintRunningState(0);
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintError(msgId, errString);
            }
        }
    }

    public void handleFingerprintLockoutReset() {
        updateFingerprintListeningState();
    }

    private void setFingerprintRunningState(int fingerprintRunningState) {
        boolean wasRunning = this.mFingerprintRunningState == 1;
        boolean isRunning = fingerprintRunningState == 1;
        this.mFingerprintRunningState = fingerprintRunningState;
        if (wasRunning == isRunning) {
            return;
        }
        notifyFingerprintRunningStateChanged();
    }

    private void notifyFingerprintRunningStateChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
            }
        }
    }

    public void handleFaceUnlockStateChanged(boolean running, int userId) {
        Log.d("KeyguardUpdateMonitor", "handleFaceUnlockStateChanged(running = " + running + " , userId = " + userId);
        this.mUserFaceUnlockRunning.put(userId, running);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFaceUnlockStateChanged(running, userId);
            }
        }
    }

    public boolean isFaceUnlockRunning(int userId) {
        return this.mUserFaceUnlockRunning.get(userId);
    }

    public boolean isFingerprintDetectionRunning() {
        return this.mFingerprintRunningState == 1;
    }

    private boolean isTrustDisabled(int userId) {
        boolean disabledBySimPin = isSimPinSecure();
        return disabledBySimPin;
    }

    private boolean isFingerprintDisabled(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm == null || (dpm.getKeyguardDisabledFeatures(null, userId) & 32) == 0) {
            return isSimPinSecure();
        }
        return true;
    }

    public boolean getUserCanSkipBouncer(int userId) {
        if (getUserHasTrust(userId)) {
            return true;
        }
        if (this.mUserFingerprintAuthenticated.get(userId)) {
            return isUnlockingWithFingerprintAllowed();
        }
        return false;
    }

    public boolean getUserHasTrust(int userId) {
        if (isTrustDisabled(userId)) {
            return false;
        }
        return this.mUserHasTrust.get(userId);
    }

    public boolean getUserTrustIsManaged(int userId) {
        return this.mUserTrustIsManaged.get(userId) && !isTrustDisabled(userId);
    }

    public boolean isUnlockingWithFingerprintAllowed() {
        return this.mStrongAuthTracker.isUnlockingWithFingerprintAllowed() && !hasFingerprintUnlockTimedOut(sCurrentUser);
    }

    public StrongAuthTracker getStrongAuthTracker() {
        return this.mStrongAuthTracker;
    }

    public boolean hasFingerprintUnlockTimedOut(int userId) {
        return !this.mStrongAuthNotTimedOut.contains(Integer.valueOf(userId));
    }

    public void reportSuccessfulStrongAuthUnlockAttempt() {
        this.mStrongAuthNotTimedOut.add(Integer.valueOf(sCurrentUser));
        scheduleStrongAuthTimeout();
        if (this.mFpm == null) {
            return;
        }
        this.mFpm.resetTimeout(null);
    }

    private void scheduleStrongAuthTimeout() {
        long when = SystemClock.elapsedRealtime() + 259200000;
        Intent intent = new Intent("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
        intent.putExtra("com.android.systemui.USER_ID", sCurrentUser);
        PendingIntent sender = PendingIntent.getBroadcast(this.mContext, sCurrentUser, intent, 268435456);
        this.mAlarmManager.set(3, when, sender);
        notifyStrongAuthStateChanged(sCurrentUser);
    }

    public void notifyStrongAuthStateChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStrongAuthStateChanged(userId);
            }
        }
    }

    static class DisplayClientState {
        DisplayClientState() {
        }
    }

    public void proceedToHandleSimStateChanged(SimData simArgs) {
        if (IccCardConstants.State.NETWORK_LOCKED == simArgs.simState && KeyguardUtils.isMediatekSimMeLockSupport()) {
            new simMeStatusQueryThread(simArgs).start();
        } else {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(304, simArgs));
        }
    }

    static class SimData {
        public int phoneId;
        public int simMECategory;
        public final IccCardConstants.State simState;
        public int subId;

        SimData(IccCardConstants.State state, int phoneId, int subId) {
            this.phoneId = 0;
            this.simMECategory = 0;
            this.simState = state;
            this.phoneId = phoneId;
            this.subId = subId;
        }

        SimData(IccCardConstants.State state, int phoneId, int subId, int meCategory) {
            this.phoneId = 0;
            this.simMECategory = 0;
            this.simState = state;
            this.phoneId = phoneId;
            this.subId = subId;
            this.simMECategory = meCategory;
        }

        static SimData fromIntent(Intent intent) {
            IccCardConstants.State state;
            if (!"android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && !"mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK".equals(intent.getAction())) {
                throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
            }
            int meCategory = 0;
            String stateExtra = intent.getStringExtra("ss");
            int phoneId = intent.getIntExtra("slot", 0);
            int subId = intent.getIntExtra("subscription", -1);
            if ("ABSENT".equals(stateExtra)) {
                String absentReason = intent.getStringExtra("reason");
                if ("PERM_DISABLED".equals(absentReason)) {
                    state = IccCardConstants.State.PERM_DISABLED;
                } else {
                    state = IccCardConstants.State.ABSENT;
                }
            } else if ("READY".equals(stateExtra)) {
                state = IccCardConstants.State.READY;
            } else if ("LOCKED".equals(stateExtra)) {
                String lockedReason = intent.getStringExtra("reason");
                Log.d("KeyguardUpdateMonitor", "INTENT_VALUE_ICC_LOCKED, lockedReason=" + lockedReason);
                if ("PIN".equals(lockedReason)) {
                    state = IccCardConstants.State.PIN_REQUIRED;
                } else if ("PUK".equals(lockedReason)) {
                    state = IccCardConstants.State.PUK_REQUIRED;
                } else if ("NETWORK".equals(lockedReason)) {
                    meCategory = 0;
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else if ("NETWORK_SUBSET".equals(lockedReason)) {
                    meCategory = 1;
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else if ("SERVICE_PROVIDER".equals(lockedReason)) {
                    meCategory = 2;
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else if ("CORPORATE".equals(lockedReason)) {
                    meCategory = 3;
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else if ("SIM".equals(lockedReason)) {
                    meCategory = 4;
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else {
                    state = IccCardConstants.State.UNKNOWN;
                }
            } else if ("NETWORK".equals(stateExtra)) {
                state = IccCardConstants.State.NETWORK_LOCKED;
            } else if ("LOADED".equals(stateExtra) || "IMSI".equals(stateExtra)) {
                state = IccCardConstants.State.READY;
            } else if ("NOT_READY".equals(stateExtra)) {
                state = IccCardConstants.State.NOT_READY;
            } else {
                state = IccCardConstants.State.UNKNOWN;
            }
            return new SimData(state, phoneId, subId, meCategory);
        }

        public String toString() {
            return this.simState.toString();
        }
    }

    public static class BatteryStatus {
        public final int health;
        public final int level;
        public final int maxChargingWattage;
        public final int plugged;
        public final int status;

        public BatteryStatus(int status, int level, int plugged, int health, int maxChargingWattage) {
            this.status = status;
            this.level = level;
            this.plugged = plugged;
            this.health = health;
            this.maxChargingWattage = maxChargingWattage;
        }

        public boolean isPluggedIn() {
            return this.plugged == 1 || this.plugged == 2 || this.plugged == 4;
        }

        public boolean isCharged() {
            return this.status == 5 || this.level >= 100;
        }

        public boolean isBatteryLow() {
            return this.level < 16;
        }

        public final int getChargingSpeed(int slowThreshold, int fastThreshold) {
            if (this.maxChargingWattage <= 0) {
                return -1;
            }
            if (this.maxChargingWattage >= slowThreshold) {
                return this.maxChargingWattage > fastThreshold ? 2 : 1;
            }
            return 0;
        }
    }

    public class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public StrongAuthTracker(Context context) {
            super(context);
        }

        public boolean isUnlockingWithFingerprintAllowed() {
            int userId = KeyguardUpdateMonitor.getCurrentUser();
            return isFingerprintAllowedForUser(userId);
        }

        public boolean hasUserAuthenticatedSinceBoot() {
            int userId = KeyguardUpdateMonitor.getCurrentUser();
            return (getStrongAuthForUser(userId) & 1) == 0;
        }

        public void onStrongAuthRequiredChanged(int userId) {
            KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(userId);
        }
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    protected void handleStartedWakingUp() {
        Log.d("KeyguardUpdateMonitor", "handleStartedWakingUp");
        updateFingerprintListeningState();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStartedWakingUp();
            }
        }
    }

    protected void handleStartedGoingToSleep(int arg1) {
        clearFingerprintRecognized();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onStartedGoingToSleep(arg1);
            }
        }
        this.mGoingToSleep = true;
        this.mFingerprintAlreadyAuthenticated = false;
        updateFingerprintListeningState();
    }

    protected void handleFinishedGoingToSleep(int arg1) {
        this.mGoingToSleep = false;
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFinishedGoingToSleep(arg1);
            }
        }
        updateFingerprintListeningState();
    }

    public void handleScreenTurnedOn() {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOn();
            }
        }
    }

    public void handleScreenTurnedOff() {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOff();
            }
        }
    }

    public void handleUserInfoChanged(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserInfoChanged(userId);
            }
        }
    }

    private KeyguardUpdateMonitor(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mAlarmManager = (AlarmManager) context.getSystemService(AlarmManager.class);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
        Log.d("KeyguardUpdateMonitor", "mDeviceProvisioned is:" + this.mDeviceProvisioned);
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        this.mBatteryStatus = new BatteryStatus(1, 100, 0, 0, 0);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        initMembers();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.SERVICE_STATE");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("mediatek.intent.action.ACTION_UNLOCK_SIM_LOCK");
        filter.addAction("android.intent.action.AIRPLANE_MODE");
        context.registerReceiver(this.mBroadcastReceiver, filter);
        IntentFilter bootCompleteFilter = new IntentFilter();
        bootCompleteFilter.setPriority(1000);
        bootCompleteFilter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBroadcastReceiver, bootCompleteFilter);
        IntentFilter allUserFilter = new IntentFilter();
        allUserFilter.addAction("android.intent.action.USER_INFO_CHANGED");
        allUserFilter.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STARTED");
        allUserFilter.addAction("com.android.facelock.FACE_UNLOCK_STOPPED");
        allUserFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        context.registerReceiverAsUser(this.mBroadcastAllReceiver, UserHandle.ALL, allUserFilter, null, null);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        try {
            ActivityManagerNative.getDefault().registerUserSwitchObserver(new IUserSwitchObserver.Stub() {
                public void onUserSwitching(int newUserId, IRemoteCallback reply) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(310, newUserId, 0, reply));
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, newUserId, 0));
                }

                public void onForegroundProfileSwitch(int newProfileId) {
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        IntentFilter strongAuthTimeoutFilter = new IntentFilter();
        strongAuthTimeoutFilter.addAction("com.android.systemui.ACTION_STRONG_AUTH_TIMEOUT");
        context.registerReceiver(this.mStrongAuthTimeoutReceiver, strongAuthTimeoutFilter, "com.android.systemui.permission.SELF", null);
        this.mTrustManager = (TrustManager) context.getSystemService("trust");
        this.mTrustManager.registerTrustListener(this);
        new LockPatternUtils(context).registerStrongAuthTracker(this.mStrongAuthTracker);
        this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        updateFingerprintListeningState();
        if (this.mFpm == null) {
            return;
        }
        this.mFpm.addLockoutResetCallback(this.mLockoutResetCallback);
    }

    private void updateFingerprintListeningState() {
        boolean shouldListenForFingerprint = shouldListenForFingerprint();
        if (this.mFingerprintRunningState == 1 && !shouldListenForFingerprint) {
            stopListeningForFingerprint();
        } else {
            if (this.mFingerprintRunningState == 1 || !shouldListenForFingerprint) {
                return;
            }
            startListeningForFingerprint();
        }
    }

    private boolean shouldListenForFingerprint() {
        return ((!this.mKeyguardIsVisible && this.mDeviceInteractive && !this.mBouncer && !this.mGoingToSleep) || this.mSwitchingUser || this.mFingerprintAlreadyAuthenticated || isFingerprintDisabled(getCurrentUser())) ? false : true;
    }

    private void startListeningForFingerprint() {
        if (this.mFingerprintRunningState == 2) {
            setFingerprintRunningState(3);
            return;
        }
        Log.v("KeyguardUpdateMonitor", "startListeningForFingerprint()");
        int userId = ActivityManager.getCurrentUser();
        if (!isUnlockWithFingerprintPossible(userId)) {
            return;
        }
        if (this.mFingerprintCancelSignal != null) {
            this.mFingerprintCancelSignal.cancel();
        }
        this.mFingerprintCancelSignal = new CancellationSignal();
        this.mFpm.authenticate(null, this.mFingerprintCancelSignal, 0, this.mAuthenticationCallback, null, userId);
        setFingerprintRunningState(1);
    }

    public boolean isUnlockWithFingerprintPossible(int userId) {
        return this.mFpm != null && this.mFpm.isHardwareDetected() && !isFingerprintDisabled(userId) && this.mFpm.getEnrolledFingerprints(userId).size() > 0;
    }

    private void stopListeningForFingerprint() {
        Log.v("KeyguardUpdateMonitor", "stopListeningForFingerprint()");
        if (this.mFingerprintRunningState == 1) {
            this.mFingerprintCancelSignal.cancel();
            this.mFingerprintCancelSignal = null;
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState != 3) {
            return;
        }
        setFingerprintRunningState(2);
    }

    public boolean isDeviceProvisionedInSettingsDb() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private void watchForDeviceProvisioning() {
        this.mDeviceProvisionedObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                KeyguardUpdateMonitor.this.mDeviceProvisioned = KeyguardUpdateMonitor.this.isDeviceProvisionedInSettingsDb();
                if (KeyguardUpdateMonitor.this.mDeviceProvisioned) {
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(308);
                }
                Log.d("KeyguardUpdateMonitor", "DEVICE_PROVISIONED state = " + KeyguardUpdateMonitor.this.mDeviceProvisioned);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean provisioned = isDeviceProvisionedInSettingsDb();
        if (provisioned == this.mDeviceProvisioned) {
            return;
        }
        this.mDeviceProvisioned = provisioned;
        if (!this.mDeviceProvisioned) {
            return;
        }
        this.mHandler.sendEmptyMessage(308);
    }

    protected void handleDevicePolicyManagerStateChanged() {
        updateFingerprintListeningState();
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDevicePolicyManagerStateChanged();
            }
        }
    }

    protected void handleUserSwitching(int userId, IRemoteCallback reply) {
        this.mSwitchingUser = true;
        updateFingerprintListeningState();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserSwitching(userId);
            }
        }
        try {
            reply.sendResult((Bundle) null);
        } catch (RemoteException e) {
        }
    }

    protected void handleUserSwitchComplete(int userId) {
        this.mSwitchingUser = false;
        updateFingerprintListeningState();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserSwitchComplete(userId);
            }
        }
    }

    public void dispatchBootCompleted() {
        this.mHandler.sendEmptyMessage(313);
    }

    protected void handleBootCompleted() {
        if (this.mBootCompleted) {
            return;
        }
        this.mBootCompleted = true;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onBootCompleted();
            }
        }
    }

    protected void handleDeviceProvisioned() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDeviceProvisioned();
            }
        }
        if (this.mDeviceProvisionedObserver == null) {
            return;
        }
        this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
        this.mDeviceProvisionedObserver = null;
    }

    protected void handlePhoneStateChanged() {
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged");
        this.mPhoneState = 0;
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            int subId = KeyguardUtils.getSubIdUsingPhoneId(i);
            int callState = TelephonyManager.getDefault().getCallState(subId);
            if (callState == 2) {
                this.mPhoneState = callState;
            } else if (callState == 1 && this.mPhoneState == 0) {
                this.mPhoneState = callState;
            }
        }
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged() - mPhoneState = " + this.mPhoneState);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i2).get();
            if (cb != null) {
                cb.onPhoneStateChanged(this.mPhoneState);
            }
        }
    }

    protected void handleRingerModeChange(int mode) {
        Log.d("KeyguardUpdateMonitor", "handleRingerModeChange(" + mode + ")");
        this.mRingMode = mode;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onRingerModeChanged(mode);
            }
        }
    }

    public void handleTimeUpdate() {
        Log.d("KeyguardUpdateMonitor", "handleTimeUpdate");
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTimeChanged();
            }
        }
    }

    public void handleBatteryUpdate(BatteryStatus status) {
        Log.d("KeyguardUpdateMonitor", "handleBatteryUpdate");
        boolean batteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, status);
        this.mBatteryStatus = status;
        if (!batteryUpdateInteresting) {
            return;
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onRefreshBatteryInfo(status);
            }
        }
    }

    public void handleCarrierInfoUpdate(int phoneId) {
    }

    private void printState() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            Log.d("KeyguardUpdateMonitor", "Phone# " + i + ", state = " + this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
        }
    }

    public void handleSimStateChange(SimData simArgs) {
        IccCardConstants.State state = simArgs.simState;
        Log.d("KeyguardUpdateMonitor", "handleSimStateChange: intentValue = " + simArgs + " state resolved to " + state.toString() + " phoneId=" + simArgs.phoneId);
        if (state != IccCardConstants.State.UNKNOWN) {
            if (state == IccCardConstants.State.NETWORK_LOCKED || state != this.mSimStateOfPhoneId.get(Integer.valueOf(simArgs.phoneId))) {
                this.mSimStateOfPhoneId.put(Integer.valueOf(simArgs.phoneId), state);
                int phoneId = simArgs.phoneId;
                printState();
                for (int i = 0; i < this.mCallbacks.size(); i++) {
                    KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
                    if (cb != null) {
                        cb.onSimStateChangedUsingPhoneId(phoneId, state);
                    }
                }
            }
        }
    }

    public void handleServiceStateChange(int subId, ServiceState serviceState) {
        Log.d("KeyguardUpdateMonitor", "handleServiceStateChange(subId=" + subId + ", serviceState=" + serviceState);
        this.mServiceStates.put(Integer.valueOf(subId), serviceState);
        for (int j = 0; j < this.mCallbacks.size(); j++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(j).get();
            if (cb != null) {
                cb.onRefreshCarrierInfo();
            }
        }
    }

    public void onKeyguardVisibilityChanged(boolean showing) {
        Log.d("KeyguardUpdateMonitor", "onKeyguardVisibilityChanged(" + showing + ")");
        this.mKeyguardIsVisible = showing;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardVisibilityChangedRaw(showing);
            }
        }
        if (!showing) {
            this.mFingerprintAlreadyAuthenticated = false;
        }
        updateFingerprintListeningState();
    }

    public void handleKeyguardReset() {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardReset");
        updateFingerprintListeningState();
    }

    public void handleKeyguardBouncerChanged(int bouncer) {
        Log.d("KeyguardUpdateMonitor", "handleKeyguardBouncerChanged(" + bouncer + ")");
        boolean isBouncer = bouncer == 1;
        this.mBouncer = isBouncer;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardBouncerChanged(isBouncer);
            }
        }
        updateFingerprintListeningState();
    }

    public void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onEmergencyCallAction();
            }
        }
    }

    private static boolean isBatteryUpdateInteresting(BatteryStatus old, BatteryStatus current) {
        boolean nowPluggedIn = current.isPluggedIn();
        boolean wasPluggedIn = old.isPluggedIn();
        boolean stateChangedWhilePluggedIn = wasPluggedIn && nowPluggedIn && old.status != current.status;
        if (wasPluggedIn != nowPluggedIn || stateChangedWhilePluggedIn || old.level != current.level) {
            return true;
        }
        if (nowPluggedIn || !current.isBatteryLow() || current.level == old.level) {
            return nowPluggedIn && current.maxChargingWattage != old.maxChargingWattage;
        }
        return true;
    }

    public CharSequence getTelephonyPlmnFrom(Intent intent) {
        if (!intent.getBooleanExtra("showPlmn", false)) {
            return null;
        }
        String plmn = intent.getStringExtra("plmn");
        return plmn != null ? plmn : getDefaultPlmn();
    }

    public CharSequence getDefaultPlmn() {
        return this.mContext.getResources().getText(R$string.keyguard_carrier_default);
    }

    public CharSequence getTelephonySpnFrom(Intent intent) {
        String spn;
        if (!intent.getBooleanExtra("showSpn", false) || (spn = intent.getStringExtra("spn")) == null) {
            return null;
        }
        return spn;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** unregister callback for " + callback);
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            if (this.mCallbacks.get(i).get() == callback) {
                this.mCallbacks.remove(i);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback callback) {
        Log.v("KeyguardUpdateMonitor", "*** register callback for " + callback);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == callback) {
                Log.e("KeyguardUpdateMonitor", "Object tried to add another callback", new Exception("Called by"));
                return;
            }
        }
        this.mCallbacks.add(new WeakReference<>(callback));
        removeCallback(null);
        sendUpdates(callback);
        this.mNewClientRegUpdateMonitor = true;
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback callback) {
        callback.onRefreshBatteryInfo(this.mBatteryStatus);
        callback.onTimeChanged();
        callback.onRingerModeChanged(this.mRingMode);
        callback.onPhoneStateChanged(this.mPhoneState);
        callback.onRefreshCarrierInfo();
        callback.onClockVisibilityChanged();
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            callback.onSimStateChangedUsingPhoneId(phoneId, this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
        }
    }

    public void sendKeyguardReset() {
        this.mHandler.obtainMessage(312).sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean showingBouncer) {
        Log.d("KeyguardUpdateMonitor", "sendKeyguardBouncerChanged(" + showingBouncer + ")");
        Message message = this.mHandler.obtainMessage(322);
        message.arg1 = showingBouncer ? 1 : 0;
        message.sendToTarget();
    }

    public IccCardConstants.State getSimStateOfPhoneId(int phoneId) {
        return this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
    }

    public void reportSimUnlocked(int phoneId) {
        int subId = KeyguardUtils.getSubIdUsingPhoneId(phoneId);
        handleSimStateChange(new SimData(IccCardConstants.State.READY, phoneId, subId));
    }

    public void reportEmergencyCallAction(boolean bypassHandler) {
        if (!bypassHandler) {
            this.mHandler.obtainMessage(318).sendToTarget();
        } else {
            handleReportEmergencyCallAction();
        }
    }

    public boolean isDeviceProvisioned() {
        if (!this.mDeviceProvisioned) {
            Log.d("KeyguardUpdateMonitor", "isDeviceProvisioned get DEVICE_PROVISIONED from db again !!");
            return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }
        Log.d("KeyguardUpdateMonitor", "mDeviceProvisioned == true");
        return this.mDeviceProvisioned;
    }

    public void clearFailedUnlockAttempts() {
        this.mFailedAttempts.delete(sCurrentUser);
        this.mFailedBiometricUnlockAttempts = 0;
    }

    public int getFailedUnlockAttempts(int userId) {
        return this.mFailedAttempts.get(userId, 0);
    }

    public void reportFailedStrongAuthUnlockAttempt(int userId) {
        this.mFailedAttempts.put(userId, getFailedUnlockAttempts(userId) + 1);
    }

    public void clearFingerprintRecognized() {
        this.mUserFingerprintAuthenticated.clear();
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    private boolean refreshSimState(int subId, int slotId) {
        IccCardConstants.State state;
        TelephonyManager tele = TelephonyManager.from(this.mContext);
        int simState = tele.getSimState(slotId);
        try {
            state = IccCardConstants.State.intToState(simState);
        } catch (IllegalArgumentException e) {
            Log.w("KeyguardUpdateMonitor", "Unknown sim state: " + simState);
            state = IccCardConstants.State.UNKNOWN;
        }
        IccCardConstants.State oriState = this.mSimStateOfPhoneId.get(Integer.valueOf(slotId));
        boolean changed = oriState != state;
        if (oriState == IccCardConstants.State.READY && state == IccCardConstants.State.PIN_REQUIRED) {
            changed = false;
        }
        if (changed) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(slotId), state);
        }
        Log.d("KeyguardUpdateMonitor", "refreshSimState() - sub = " + subId + " phoneId = " + slotId + ", ori-state = " + oriState + ", new-state = " + state + ", changed = " + changed);
        return changed;
    }

    public boolean isSimPinSecure() {
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            if (isSimPinSecure(phoneId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isSimPinSecure(int phoneId) {
        IccCardConstants.State state = this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
        return (state == IccCardConstants.State.PIN_REQUIRED || state == IccCardConstants.State.PUK_REQUIRED || (state == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport())) && !getPinPukMeDismissFlagOfPhoneId(phoneId);
    }

    public void dispatchStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchStartedGoingToSleep(int why) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(321, why, 0));
    }

    public void dispatchFinishedGoingToSleep(int why) {
        synchronized (this) {
            this.mDeviceInteractive = false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(320, why, 0));
    }

    public void dispatchScreenTurnedOn() {
        synchronized (this) {
            this.mScreenOn = true;
        }
        this.mHandler.sendEmptyMessage(331);
    }

    public void dispatchScreenTurnedOff() {
        synchronized (this) {
            this.mScreenOn = false;
        }
        this.mHandler.sendEmptyMessage(332);
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public boolean isGoingToSleep() {
        return this.mGoingToSleep;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId) {
        return getSubscriptionInfoForSubId(subId, false);
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId, boolean forceReload) {
        List<SubscriptionInfo> list = getSubscriptionInfo(forceReload);
        for (int i = 0; i < list.size(); i++) {
            SubscriptionInfo info = list.get(i);
            if (subId == info.getSubscriptionId()) {
                return info;
            }
        }
        return null;
    }

    public int getSimPinLockPhoneId() {
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            Log.d("KeyguardUpdateMonitor", "getSimPinLockSubId, phoneId=" + phoneId + " mSimStateOfPhoneId.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)) == IccCardConstants.State.PIN_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(phoneId)) {
                int currentSimPinPhoneId = phoneId;
                return currentSimPinPhoneId;
            }
        }
        return -1;
    }

    public int getSimPukLockPhoneId() {
        for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
            Log.d("KeyguardUpdateMonitor", "getSimPukLockSubId, phoneId=" + phoneId + " mSimStateOfSub.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)) == IccCardConstants.State.PUK_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(phoneId) && getRetryPukCountOfPhoneId(phoneId) != 0) {
                int currentSimPukPhoneId = phoneId;
                return currentSimPukPhoneId;
            }
        }
        return -1;
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("KeyguardUpdateMonitor state:");
        pw.println("  SIM States:");
        for (SimData data : this.mSimDatas.values()) {
            pw.println("    " + data.toString());
        }
        pw.println("  Subs:");
        if (this.mSubscriptionInfo != null) {
            for (int i = 0; i < this.mSubscriptionInfo.size(); i++) {
                pw.println("    " + this.mSubscriptionInfo.get(i));
            }
        }
        pw.println("  Service states:");
        Iterator subId$iterator = this.mServiceStates.keySet().iterator();
        while (subId$iterator.hasNext()) {
            int subId = ((Integer) subId$iterator.next()).intValue();
            pw.println("    " + subId + "=" + this.mServiceStates.get(Integer.valueOf(subId)));
        }
        if (this.mFpm == null || !this.mFpm.isHardwareDetected()) {
            return;
        }
        int userId = ActivityManager.getCurrentUser();
        int strongAuthFlags = this.mStrongAuthTracker.getStrongAuthForUser(userId);
        pw.println("  Fingerprint state (user=" + userId + ")");
        pw.println("    allowed=" + isUnlockingWithFingerprintAllowed());
        pw.println("    auth'd=" + this.mUserFingerprintAuthenticated.get(userId));
        pw.println("    authSinceBoot=" + getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
        pw.println("    disabled(DPM)=" + isFingerprintDisabled(userId));
        pw.println("    possible=" + isUnlockWithFingerprintPossible(userId));
        pw.println("    strongAuthFlags=" + Integer.toHexString(strongAuthFlags));
        pw.println("    timedout=" + hasFingerprintUnlockTimedOut(userId));
        pw.println("    trustManaged=" + getUserTrustIsManaged(userId));
    }

    private void initMembers() {
        Log.d("KeyguardUpdateMonitor", "initMembers() - NumOfPhone=" + KeyguardUtils.getNumOfPhone());
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(i), IccCardConstants.State.UNKNOWN);
            this.mTelephonyPlmn.put(Integer.valueOf(i), getDefaultPlmn());
            this.mTelephonyCsgId.put(Integer.valueOf(i), "");
            this.mTelephonyHnbName.put(Integer.valueOf(i), "");
            this.mSimMeCategory.put(Integer.valueOf(i), 0);
            this.mSimMeLeftRetryCount.put(Integer.valueOf(i), 5);
        }
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    public void setDismissFlagWhenWfcOn(IccCardConstants.State simState) {
        if ((simState != IccCardConstants.State.PIN_REQUIRED && simState != IccCardConstants.State.PUK_REQUIRED && simState != IccCardConstants.State.NETWORK_LOCKED) || !isAirplaneModeOn() || !isWifiEnabled() || !KeyguardUtils.isFlightModePowerOffMd()) {
            return;
        }
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            setPinPukMeDismissFlagOfPhoneId(i, false);
            Log.d("KeyguardUpdateMonitor", "Wifi calling opened MD, setPinPukMeDismissFlagOfPhoneId false: " + i);
        }
    }

    private boolean isWifiEnabled() {
        int wifiState = this.mWifiManager.getWifiState();
        Log.d("KeyguardUpdateMonitor", "wifi state:" + wifiState);
        return wifiState != 1;
    }

    public void setPinPukMeDismissFlagOfPhoneId(int phoneId, boolean dismiss) {
        Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId() - phoneId = " + phoneId);
        if (!KeyguardUtils.isValidPhoneId(phoneId)) {
            return;
        }
        int flag2Dismiss = 1 << phoneId;
        if (dismiss) {
            this.mPinPukMeDismissFlag |= flag2Dismiss;
        } else {
            this.mPinPukMeDismissFlag &= ~flag2Dismiss;
        }
    }

    public boolean getPinPukMeDismissFlagOfPhoneId(int phoneId) {
        int flag2Check = 1 << phoneId;
        return (this.mPinPukMeDismissFlag & flag2Check) == flag2Check;
    }

    public int getRetryPukCountOfPhoneId(int phoneId) {
        if (phoneId == 3) {
            return SystemProperties.getInt("gsm.sim.retry.puk1.4", -1);
        }
        if (phoneId == 2) {
            return SystemProperties.getInt("gsm.sim.retry.puk1.3", -1);
        }
        return phoneId == 1 ? SystemProperties.getInt("gsm.sim.retry.puk1.2", -1) : SystemProperties.getInt("gsm.sim.retry.puk1", -1);
    }

    private class simMeStatusQueryThread extends Thread {
        SimData simArgs;

        simMeStatusQueryThread(SimData simArgs) {
            this.simArgs = simArgs;
        }

        @Override
        public void run() {
            try {
                KeyguardUpdateMonitor.this.mSimMeCategory.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(this.simArgs.simMECategory));
                Log.d("KeyguardUpdateMonitor", "queryNetworkLock, phoneId =" + this.simArgs.phoneId + ", simMECategory =" + this.simArgs.simMECategory);
                if (this.simArgs.simMECategory < 0 || this.simArgs.simMECategory > 5) {
                    return;
                }
                int subId = KeyguardUtils.getSubIdUsingPhoneId(this.simArgs.phoneId);
                Bundle bundle = ITelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).queryNetworkLock(subId, this.simArgs.simMECategory);
                boolean query_result = bundle.getBoolean("com.mediatek.phone.QUERY_SIMME_LOCK_RESULT", false);
                Log.d("KeyguardUpdateMonitor", "queryNetworkLock, query_result =" + query_result);
                if (query_result) {
                    KeyguardUpdateMonitor.this.mSimMeLeftRetryCount.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(bundle.getInt("com.mediatek.phone.SIMME_LOCK_LEFT_COUNT", 5)));
                } else {
                    Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock result fail");
                }
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, this.simArgs));
            } catch (Exception e) {
                Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock got exception: " + e.getMessage());
            }
        }
    }

    public int getSimMeCategoryOfPhoneId(int phoneId) {
        return this.mSimMeCategory.get(Integer.valueOf(phoneId)).intValue();
    }

    public int getSimMeLeftRetryCountOfPhoneId(int phoneId) {
        return this.mSimMeLeftRetryCount.get(Integer.valueOf(phoneId)).intValue();
    }

    public void minusSimMeLeftRetryCountOfPhoneId(int phoneId) {
        int simMeRetryCount = this.mSimMeLeftRetryCount.get(Integer.valueOf(phoneId)).intValue();
        if (simMeRetryCount <= 0) {
            return;
        }
        this.mSimMeLeftRetryCount.put(Integer.valueOf(phoneId), Integer.valueOf(simMeRetryCount - 1));
    }

    public CharSequence getTelephonyHnbNameFrom(Intent intent) {
        String hnbName = intent.getStringExtra("hnbName");
        return hnbName;
    }

    public CharSequence getTelephonyCsgIdFrom(Intent intent) {
        String csgId = intent.getStringExtra("csgId");
        return csgId;
    }

    public void handleAirPlaneModeUpdate(boolean airPlaneModeEnabled) {
        if (!airPlaneModeEnabled) {
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                setPinPukMeDismissFlagOfPhoneId(i, false);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId false: " + i);
            }
            for (int phoneId = 0; phoneId < KeyguardUtils.getNumOfPhone(); phoneId++) {
                Log.d("KeyguardUpdateMonitor", "phoneId = " + phoneId + " state=" + this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)));
                if (this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)) != null && !this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)).equals("")) {
                    switch (m496xf663cf59()[this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId)).ordinal()]) {
                        case 1:
                        case 2:
                        case 3:
                            IccCardConstants.State oriState = this.mSimStateOfPhoneId.get(Integer.valueOf(phoneId));
                            this.mSimStateOfPhoneId.put(Integer.valueOf(phoneId), IccCardConstants.State.UNKNOWN);
                            int meCategory = this.mSimMeCategory.get(Integer.valueOf(phoneId)) != null ? this.mSimMeCategory.get(Integer.valueOf(phoneId)).intValue() : 0;
                            SimData simData = new SimData(oriState, phoneId, KeyguardUtils.getSubIdUsingPhoneId(phoneId), meCategory);
                            Log.v("KeyguardUpdateMonitor", "SimData state=" + simData.simState + ", phoneId=" + simData.phoneId + ", subId=" + simData.subId + ", SimData.simMECategory = " + simData.simMECategory);
                            proceedToHandleSimStateChanged(simData);
                            break;
                    }
                }
            }
        } else if (airPlaneModeEnabled && KeyguardUtils.isFlightModePowerOffMd()) {
            Log.d("KeyguardUpdateMonitor", "Air mode is on, supress all SIM PIN/PUK/ME Lock views.");
            for (int i2 = 0; i2 < KeyguardUtils.getNumOfPhone(); i2++) {
                setPinPukMeDismissFlagOfPhoneId(i2, true);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId true: " + i2);
            }
        }
        for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i3).get();
            if (cb != null) {
                cb.onAirPlaneModeChanged(airPlaneModeEnabled);
                cb.onRefreshCarrierInfo();
            }
        }
    }

    public boolean isAlternateUnlockEnabled() {
        return this.mAlternateUnlockEnabled;
    }

    public int getPhoneState() {
        return this.mPhoneState;
    }

    public void reportFailedBiometricUnlockAttempt() {
        this.mFailedBiometricUnlockAttempts++;
    }

    public boolean getMaxBiometricUnlockAttemptsReached() {
        return this.mFailedBiometricUnlockAttempts >= 3;
    }

    public void setAlternateUnlockEnabled(boolean enabled) {
        Log.d("KeyguardUpdateMonitor", "setAlternateUnlockEnabled(enabled = " + enabled + ")");
        this.mAlternateUnlockEnabled = enabled;
    }
}
