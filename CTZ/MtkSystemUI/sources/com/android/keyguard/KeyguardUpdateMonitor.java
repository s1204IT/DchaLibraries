package com.android.keyguard;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.app.UserSwitchObserver;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.dreams.IDreamManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.mediatek.internal.telephony.IMtkTelephonyEx;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class KeyguardUpdateMonitor implements TrustManager.TrustListener {
    public static final boolean CORE_APPS_ONLY;
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final boolean DEBUG_SIM_STATES = KeyguardConstants.DEBUG_SIM_STATES;
    private static final ComponentName FALLBACK_HOME_COMPONENT = new ComponentName("com.android.settings", "com.android.settings.FallbackHome");
    private static int sCurrentUser;
    private static boolean sDisableHandlerCheckForTesting;
    private static KeyguardUpdateMonitor sInstance;
    private boolean mAssistantVisible;
    private BatteryStatus mBatteryStatus;
    private boolean mBootCompleted;
    private boolean mBouncer;
    private final Context mContext;
    private boolean mDeviceInteractive;
    private final DevicePolicyManager mDevicePolicyManager;
    private ContentObserver mDeviceProvisionedObserver;
    private final IDreamManager mDreamManager;
    private CancellationSignal mFingerprintCancelSignal;
    private FingerprintManager mFpm;
    private boolean mGoingToSleep;
    private boolean mHasLockscreenWallpaper;
    private boolean mIsDreaming;
    private boolean mKeyguardGoingAway;
    private boolean mKeyguardIsVisible;
    private boolean mKeyguardOccluded;
    private LockPatternUtils mLockPatternUtils;
    private boolean mLogoutEnabled;
    private boolean mNeedsSlowUnlockTransition;
    private int mPhoneState;
    private int mRingMode;
    private boolean mScreenOn;
    private final StrongAuthTracker mStrongAuthTracker;
    private List<SubscriptionInfo> mSubscriptionInfo;
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    private TrustManager mTrustManager;
    private UserManager mUserManager;
    private WifiManager mWifiManager;
    HashMap<Integer, SimData> mSimDatas = new HashMap<>();
    HashMap<Integer, ServiceState> mServiceStates = new HashMap<>();
    private SparseIntArray mFailedAttempts = new SparseIntArray();
    private final CopyOnWriteArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = new CopyOnWriteArrayList<>();
    private int mFingerprintRunningState = 0;
    private int mHardwareUnavailableRetryCount = 0;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (i != 1015) {
                switch (i) {
                    case 301:
                        KeyguardUpdateMonitor.this.handleTimeUpdate();
                        break;
                    case 302:
                        KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) message.obj);
                        break;
                    default:
                        switch (i) {
                            case 304:
                                KeyguardUpdateMonitor.this.handleSimStateChange((SimData) message.obj);
                                break;
                            case 305:
                                KeyguardUpdateMonitor.this.handleRingerModeChange(message.arg1);
                                break;
                            case 306:
                                KeyguardUpdateMonitor.this.handlePhoneStateChanged();
                                break;
                            default:
                                switch (i) {
                                    case 308:
                                        KeyguardUpdateMonitor.this.handleDeviceProvisioned();
                                        break;
                                    case 309:
                                        KeyguardUpdateMonitor.this.handleDevicePolicyManagerStateChanged();
                                        break;
                                    case 310:
                                        KeyguardUpdateMonitor.this.handleUserSwitching(message.arg1, (IRemoteCallback) message.obj);
                                        break;
                                    default:
                                        switch (i) {
                                            case 312:
                                                KeyguardUpdateMonitor.this.handleKeyguardReset();
                                                break;
                                            case 313:
                                                KeyguardUpdateMonitor.this.handleBootCompleted();
                                                break;
                                            case 314:
                                                KeyguardUpdateMonitor.this.handleUserSwitchComplete(message.arg1);
                                                break;
                                            default:
                                                switch (i) {
                                                    case 317:
                                                        KeyguardUpdateMonitor.this.handleUserInfoChanged(message.arg1);
                                                        break;
                                                    case 318:
                                                        KeyguardUpdateMonitor.this.handleReportEmergencyCallAction();
                                                        break;
                                                    case 319:
                                                        Trace.beginSection("KeyguardUpdateMonitor#handler MSG_STARTED_WAKING_UP");
                                                        KeyguardUpdateMonitor.this.handleStartedWakingUp();
                                                        Trace.endSection();
                                                        break;
                                                    case 320:
                                                        KeyguardUpdateMonitor.this.handleFinishedGoingToSleep(message.arg1);
                                                        break;
                                                    case 321:
                                                        KeyguardUpdateMonitor.this.handleStartedGoingToSleep(message.arg1);
                                                        break;
                                                    case 322:
                                                        KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(message.arg1);
                                                        break;
                                                    default:
                                                        switch (i) {
                                                            case 327:
                                                                Trace.beginSection("KeyguardUpdateMonitor#handler MSG_FACE_UNLOCK_STATE_CHANGED");
                                                                KeyguardUpdateMonitor.this.handleFaceUnlockStateChanged(message.arg1 != 0, message.arg2);
                                                                Trace.endSection();
                                                                break;
                                                            case 328:
                                                                KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                                                                break;
                                                            case 329:
                                                                KeyguardUpdateMonitor.this.handleAirplaneModeChanged();
                                                                break;
                                                            case 330:
                                                                KeyguardUpdateMonitor.this.handleServiceStateChange(message.arg1, (ServiceState) message.obj);
                                                                break;
                                                            case 331:
                                                                KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                                                                break;
                                                            case 332:
                                                                Trace.beginSection("KeyguardUpdateMonitor#handler MSG_SCREEN_TURNED_ON");
                                                                KeyguardUpdateMonitor.this.handleScreenTurnedOff();
                                                                Trace.endSection();
                                                                break;
                                                            case 333:
                                                                KeyguardUpdateMonitor.this.handleDreamingStateChanged(message.arg1);
                                                                break;
                                                            case 334:
                                                                KeyguardUpdateMonitor.this.handleUserUnlocked();
                                                                break;
                                                            case 335:
                                                                KeyguardUpdateMonitor.this.mAssistantVisible = ((Boolean) message.obj).booleanValue();
                                                                KeyguardUpdateMonitor.this.updateFingerprintListeningState();
                                                                break;
                                                            case 336:
                                                                KeyguardUpdateMonitor.this.updateFingerprintListeningState();
                                                                break;
                                                            case 337:
                                                                KeyguardUpdateMonitor.this.updateLogoutEnabled();
                                                                break;
                                                            default:
                                                                super.handleMessage(message);
                                                                break;
                                                        }
                                                        break;
                                                }
                                                break;
                                        }
                                        break;
                                }
                                break;
                        }
                        break;
                }
            }
            KeyguardUpdateMonitor.this.handleAirPlaneModeUpdate(((Boolean) message.obj).booleanValue());
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
    private Runnable mUpdateFingerprintListeningState = new Runnable() {
        @Override
        public final void run() {
            this.f$0.updateFingerprintListeningState();
        }
    };
    private Runnable mRetryFingerprintAuthentication = new Runnable() {
        @Override
        public void run() {
            Log.w("KeyguardUpdateMonitor", "Retrying fingerprint after HW unavailable, attempt " + KeyguardUpdateMonitor.this.mHardwareUnavailableRetryCount);
            KeyguardUpdateMonitor.this.updateFingerprintListeningState();
        }
    };
    private DisplayClientState mDisplayClientState = new DisplayClientState();

    @VisibleForTesting
    protected final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (KeyguardUpdateMonitor.DEBUG) {
                Log.d("KeyguardUpdateMonitor", "received broadcast " + action);
            }
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
                return;
            }
            if ("android.provider.Telephony.SPN_STRINGS_UPDATED".equals(action)) {
                int intExtra = intent.getIntExtra("subscription", -1);
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, sub Id = " + intExtra);
                int phoneIdUsingSubId = KeyguardUtils.getPhoneIdUsingSubId(intExtra);
                if (KeyguardUtils.isValidPhoneId(phoneIdUsingSubId)) {
                    KeyguardUpdateMonitor.this.mTelephonyPlmn.put(Integer.valueOf(phoneIdUsingSubId), KeyguardUpdateMonitor.this.getTelephonyPlmnFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonySpn.put(Integer.valueOf(phoneIdUsingSubId), KeyguardUpdateMonitor.this.getTelephonySpnFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonyCsgId.put(Integer.valueOf(phoneIdUsingSubId), KeyguardUpdateMonitor.this.getTelephonyCsgIdFrom(intent));
                    KeyguardUpdateMonitor.this.mTelephonyHnbName.put(Integer.valueOf(phoneIdUsingSubId), KeyguardUpdateMonitor.this.getTelephonyHnbNameFrom(intent));
                    if (KeyguardUpdateMonitor.DEBUG) {
                        Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, update phoneId=" + phoneIdUsingSubId + ", plmn=" + KeyguardUpdateMonitor.this.mTelephonyPlmn.get(Integer.valueOf(phoneIdUsingSubId)) + ", spn=" + KeyguardUpdateMonitor.this.mTelephonySpn.get(Integer.valueOf(phoneIdUsingSubId)) + ", csgId=" + KeyguardUpdateMonitor.this.mTelephonyCsgId.get(Integer.valueOf(phoneIdUsingSubId)) + ", hnbName=" + KeyguardUpdateMonitor.this.mTelephonyHnbName.get(Integer.valueOf(phoneIdUsingSubId)));
                    }
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(303, Integer.valueOf(phoneIdUsingSubId)));
                    return;
                }
                Log.d("KeyguardUpdateMonitor", "SPN_STRINGS_UPDATED_ACTION, invalid phoneId = " + phoneIdUsingSubId);
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int intExtra2 = intent.getIntExtra("status", 1);
                int intExtra3 = intent.getIntExtra("plugged", 0);
                int intExtra4 = intent.getIntExtra("level", 0);
                int intExtra5 = intent.getIntExtra("health", 1);
                int intExtra6 = intent.getIntExtra("max_charging_current", -1);
                int intExtra7 = intent.getIntExtra("max_charging_voltage", -1);
                if (intExtra7 <= 0) {
                    intExtra7 = 5000000;
                }
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(302, new BatteryStatus(intExtra2, intExtra4, intExtra3, intExtra5, intExtra6 > 0 ? (intExtra6 / 1000) * (intExtra7 / 1000) : -1)));
                return;
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action) || "com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                if (intent.getBooleanExtra("rebroadcastOnUnlock", false)) {
                    return;
                }
                String stringExtra = intent.getStringExtra("ss");
                SimData simDataFromIntent = SimData.fromIntent(intent);
                if (KeyguardUpdateMonitor.DEBUG_SIM_STATES) {
                    Log.v("KeyguardUpdateMonitor", "action=" + action + ", state=" + stringExtra + ", slotId=" + simDataFromIntent.phoneId + ", subId=" + simDataFromIntent.subId + ", simArgs.simState = " + simDataFromIntent.simState);
                }
                if ("com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK".equals(action)) {
                    Log.d("KeyguardUpdateMonitor", "ACTION_UNLOCK_SIM_LOCK, set sim state as UNKNOWN");
                    KeyguardUpdateMonitor.this.mSimStateOfPhoneId.put(Integer.valueOf(simDataFromIntent.phoneId), IccCardConstants.State.UNKNOWN);
                }
                KeyguardUpdateMonitor.this.proceedToHandleSimStateChanged(simDataFromIntent);
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
                boolean booleanExtra = intent.getBooleanExtra("state", false);
                Log.d("KeyguardUpdateMonitor", "Receive ACTION_AIRPLANE_MODE_CHANGED, state = " + booleanExtra);
                Message message = new Message();
                message.what = 1015;
                message.obj = new Boolean(booleanExtra);
                KeyguardUpdateMonitor.this.mHandler.sendMessage(message);
                return;
            }
            if (!"android.intent.action.SERVICE_STATE".equals(action)) {
                if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(337);
                    return;
                }
                return;
            }
            ServiceState serviceStateNewFromBundle = ServiceState.newFromBundle(intent.getExtras());
            int intExtra8 = intent.getIntExtra("subscription", -1);
            if (KeyguardUpdateMonitor.DEBUG) {
                Log.v("KeyguardUpdateMonitor", "action " + action + " serviceState=" + serviceStateNewFromBundle + " subId=" + intExtra8);
            }
            KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(330, intExtra8, 0, serviceStateNewFromBundle));
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
                Trace.beginSection("KeyguardUpdateMonitor.mBroadcastAllReceiver#onReceive ACTION_FACE_UNLOCK_STARTED");
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 1, getSendingUserId()));
                Trace.endSection();
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(327, 0, getSendingUserId()));
            } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            } else if ("android.intent.action.USER_UNLOCKED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(334);
            }
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
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult authenticationResult) {
            Trace.beginSection("KeyguardUpdateMonitor#onAuthenticationSucceeded");
            KeyguardUpdateMonitor.this.handleFingerprintAuthenticated(authenticationResult.getUserId());
            Trace.endSection();
        }

        @Override
        public void onAuthenticationHelp(int i, CharSequence charSequence) {
            KeyguardUpdateMonitor.this.handleFingerprintHelp(i, charSequence.toString());
        }

        @Override
        public void onAuthenticationError(int i, CharSequence charSequence) {
            KeyguardUpdateMonitor.this.handleFingerprintError(i, charSequence.toString());
        }

        public void onAuthenticationAcquired(int i) {
            KeyguardUpdateMonitor.this.handleFingerprintAcquired(i);
        }
    };
    private final SysUiTaskStackChangeListener mTaskStackListener = new SysUiTaskStackChangeListener() {
        @Override
        public void onTaskStackChangedBackground() {
            try {
                ActivityManager.StackInfo stackInfo = ActivityManager.getService().getStackInfo(0, 4);
                if (stackInfo == null) {
                    return;
                }
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(335, Boolean.valueOf(stackInfo.visible)));
            } catch (RemoteException e) {
                Log.e("KeyguardUpdateMonitor", "unable to check task stack", e);
            }
        }
    };
    private HashMap<Integer, Integer> mSimMeCategory = new HashMap<>();
    private HashMap<Integer, Integer> mSimMeLeftRetryCount = new HashMap<>();
    private int mPinPukMeDismissFlag = 0;
    private int mSimmeDismissFlag = 0;
    private HashMap<Integer, IccCardConstants.State> mSimStateOfPhoneId = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonyPlmn = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonySpn = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonyHnbName = new HashMap<>();
    private HashMap<Integer, CharSequence> mTelephonyCsgId = new HashMap<>();
    private boolean mDeviceProvisioned = isDeviceProvisionedInSettingsDb();

    static {
        try {
            CORE_APPS_ONLY = IPackageManager.Stub.asInterface(ServiceManager.getService("package")).isOnlyCoreApps();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public static synchronized void setCurrentUser(int i) {
        sCurrentUser = i;
    }

    public static synchronized int getCurrentUser() {
        return sCurrentUser;
    }

    public void onTrustChanged(boolean z, int i, int i2) {
        checkIsHandlerThread();
        this.mUserHasTrust.put(i, z);
        for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i3).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustChanged(i);
                if (z && i2 != 0) {
                    keyguardUpdateMonitorCallback.onTrustGrantedWithFlags(i2, i);
                }
            }
        }
    }

    public void onTrustError(CharSequence charSequence) {
        dispatchErrorMessage(charSequence);
    }

    private void handleSimSubscriptionInfoChanged() {
        if (DEBUG_SIM_STATES) {
            Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged()");
            List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (activeSubscriptionInfoList != null) {
                Iterator<SubscriptionInfo> it = activeSubscriptionInfoList.iterator();
                while (it.hasNext()) {
                    Log.v("KeyguardUpdateMonitor", "SubInfo:" + it.next());
                }
            } else {
                Log.v("KeyguardUpdateMonitor", "onSubscriptionInfoChanged: list is null");
            }
        }
        List<SubscriptionInfo> subscriptionInfo = getSubscriptionInfo(true);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < subscriptionInfo.size(); i++) {
            SubscriptionInfo subscriptionInfo2 = subscriptionInfo.get(i);
            if (refreshSimState(subscriptionInfo2.getSubscriptionId(), subscriptionInfo2.getSimSlotIndex())) {
                arrayList.add(subscriptionInfo2);
            }
        }
        for (int i2 = 0; i2 < arrayList.size(); i2++) {
            int subscriptionId = ((SubscriptionInfo) arrayList.get(i2)).getSubscriptionId();
            int simSlotIndex = ((SubscriptionInfo) arrayList.get(i2)).getSimSlotIndex();
            Log.d("KeyguardUpdateMonitor", "handleSimSubscriptionInfoChanged() - call callbacks for subId = " + subscriptionId + " & phoneId = " + simSlotIndex);
            for (int i3 = 0; i3 < this.mCallbacks.size(); i3++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i3).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onSimStateChangedUsingPhoneId(simSlotIndex, this.mSimStateOfPhoneId.get(Integer.valueOf(simSlotIndex)));
                }
            }
        }
        for (int i4 = 0; i4 < this.mCallbacks.size(); i4++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback2 = this.mCallbacks.get(i4).get();
            if (keyguardUpdateMonitorCallback2 != null) {
                keyguardUpdateMonitorCallback2.onRefreshCarrierInfo();
            }
        }
    }

    private void handleAirplaneModeChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }

    public List<SubscriptionInfo> getSubscriptionInfo(boolean z) {
        List<SubscriptionInfo> activeSubscriptionInfoList = this.mSubscriptionInfo;
        if (activeSubscriptionInfoList == null || z || (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() == 0)) {
            activeSubscriptionInfoList = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (activeSubscriptionInfoList == null) {
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = activeSubscriptionInfoList;
        }
        return this.mSubscriptionInfo;
    }

    public void onTrustManagedChanged(boolean z, int i) {
        checkIsHandlerThread();
        this.mUserTrustIsManaged.put(i, z);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustManagedChanged(i);
            }
        }
    }

    public void setKeyguardGoingAway(boolean z) {
        this.mKeyguardGoingAway = z;
        updateFingerprintListeningState();
    }

    public void setKeyguardOccluded(boolean z) {
        this.mKeyguardOccluded = z;
        updateFingerprintListeningState();
    }

    public boolean isDreaming() {
        return this.mIsDreaming;
    }

    public void awakenFromDream() {
        if (this.mIsDreaming && this.mDreamManager != null) {
            try {
                this.mDreamManager.awaken();
            } catch (RemoteException e) {
                Log.e("KeyguardUpdateMonitor", "Unable to awaken from dream");
            }
        }
    }

    private void onFingerprintAuthenticated(int i) {
        Trace.beginSection("KeyGuardUpdateMonitor#onFingerPrintAuthenticated");
        this.mUserFingerprintAuthenticated.put(i, true);
        if (getUserCanSkipBouncer(i)) {
            this.mTrustManager.unlockedByFingerprintForUser(i);
        }
        this.mFingerprintCancelSignal = null;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintAuthenticated(i);
            }
        }
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(336), 500L);
        this.mAssistantVisible = false;
        Trace.endSection();
    }

    private void handleFingerprintAuthFailed() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintAuthFailed();
            }
        }
        handleFingerprintHelp(-1, this.mContext.getString(com.android.systemui.R.string.fingerprint_not_recognized));
    }

    private void handleFingerprintAcquired(int i) {
        if (i != 0) {
            return;
        }
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintAcquired();
            }
        }
    }

    private void handleFingerprintAuthenticated(int i) {
        Trace.beginSection("KeyGuardUpdateMonitor#handlerFingerPrintAuthenticated");
        try {
            int i2 = ActivityManager.getService().getCurrentUser().id;
            if (i2 != i) {
                Log.d("KeyguardUpdateMonitor", "Fingerprint authenticated for wrong user: " + i);
                return;
            }
            if (!isFingerprintDisabled(i2)) {
                onFingerprintAuthenticated(i2);
                setFingerprintRunningState(0);
                Trace.endSection();
            } else {
                Log.d("KeyguardUpdateMonitor", "Fingerprint disabled by DPM for userId: " + i2);
            }
        } catch (RemoteException e) {
            Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
        } finally {
            setFingerprintRunningState(0);
        }
    }

    private void handleFingerprintHelp(int i, String str) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintHelp(i, str);
            }
        }
    }

    private void handleFingerprintError(int i, String str) {
        if (i == 5 && this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(0);
            startListeningForFingerprint();
        } else {
            setFingerprintRunningState(0);
        }
        if (i == 1 && this.mHardwareUnavailableRetryCount < 3) {
            this.mHardwareUnavailableRetryCount++;
            this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
            this.mHandler.postDelayed(this.mRetryFingerprintAuthentication, 3000L);
        }
        if (i == 9) {
            this.mLockPatternUtils.requireStrongAuth(8, getCurrentUser());
        }
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintError(i, str);
            }
        }
    }

    private void handleFingerprintLockoutReset() {
        updateFingerprintListeningState();
    }

    private void setFingerprintRunningState(int i) {
        boolean z = this.mFingerprintRunningState == 1;
        boolean z2 = i == 1;
        this.mFingerprintRunningState = i;
        if (z != z2) {
            notifyFingerprintRunningStateChanged();
        }
    }

    private void notifyFingerprintRunningStateChanged() {
        checkIsHandlerThread();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFingerprintRunningStateChanged(isFingerprintDetectionRunning());
            }
        }
    }

    private void handleFaceUnlockStateChanged(boolean z, int i) {
        this.mUserFaceUnlockRunning.put(i, z);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFaceUnlockStateChanged(z, i);
            }
        }
    }

    public boolean isFaceUnlockRunning(int i) {
        return this.mUserFaceUnlockRunning.get(i);
    }

    public boolean isFingerprintDetectionRunning() {
        return this.mFingerprintRunningState == 1;
    }

    private boolean isTrustDisabled(int i) {
        return isSimPinSecure();
    }

    private boolean isFingerprintDisabled(int i) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        return !(devicePolicyManager == null || (devicePolicyManager.getKeyguardDisabledFeatures(null, i) & 32) == 0) || isSimPinSecure();
    }

    public boolean getUserCanSkipBouncer(int i) {
        return getUserHasTrust(i) || (this.mUserFingerprintAuthenticated.get(i) && isUnlockingWithFingerprintAllowed());
    }

    public boolean getUserHasTrust(int i) {
        return !isTrustDisabled(i) && this.mUserHasTrust.get(i);
    }

    public boolean getUserTrustIsManaged(int i) {
        return this.mUserTrustIsManaged.get(i) && !isTrustDisabled(i);
    }

    public boolean isUnlockingWithFingerprintAllowed() {
        return this.mStrongAuthTracker.isUnlockingWithFingerprintAllowed();
    }

    public boolean isUserInLockdown(int i) {
        return this.mStrongAuthTracker.getStrongAuthForUser(i) == 32;
    }

    public boolean needsSlowUnlockTransition() {
        return this.mNeedsSlowUnlockTransition;
    }

    public StrongAuthTracker getStrongAuthTracker() {
        return this.mStrongAuthTracker;
    }

    private void notifyStrongAuthStateChanged(int i) {
        checkIsHandlerThread();
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStrongAuthStateChanged(i);
            }
        }
    }

    public boolean isScreenOn() {
        return this.mScreenOn;
    }

    private void dispatchErrorMessage(CharSequence charSequence) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTrustAgentErrorMessage(charSequence);
            }
        }
    }

    static class DisplayClientState {
        DisplayClientState() {
        }
    }

    private static class SimData {
        public int phoneId;
        public int simMECategory;
        public final IccCardConstants.State simState;
        public int subId;

        SimData(IccCardConstants.State state, int i, int i2) {
            this.phoneId = 0;
            this.simMECategory = 0;
            this.simState = state;
            this.phoneId = i;
            this.subId = i2;
        }

        @VisibleForTesting
        SimData(IccCardConstants.State state, int i, int i2, int i3) {
            this.phoneId = 0;
            this.simMECategory = 0;
            this.simState = state;
            this.phoneId = i;
            this.subId = i2;
            this.simMECategory = i3;
        }

        static SimData fromIntent(Intent intent) {
            IccCardConstants.State state;
            int i;
            IccCardConstants.State state2;
            if (!"android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction()) && !"com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK".equals(intent.getAction())) {
                throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
            }
            String stringExtra = intent.getStringExtra("ss");
            int i2 = 0;
            int intExtra = intent.getIntExtra("slot", 0);
            int intExtra2 = intent.getIntExtra("subscription", -1);
            if ("ABSENT".equals(stringExtra)) {
                if ("PERM_DISABLED".equals(intent.getStringExtra("reason"))) {
                    state = IccCardConstants.State.PERM_DISABLED;
                } else {
                    state = IccCardConstants.State.ABSENT;
                }
            } else if ("READY".equals(stringExtra)) {
                state = IccCardConstants.State.READY;
            } else if ("LOCKED".equals(stringExtra)) {
                String stringExtra2 = intent.getStringExtra("reason");
                Log.d("KeyguardUpdateMonitor", "INTENT_VALUE_ICC_LOCKED, lockedReason=" + stringExtra2);
                if ("PIN".equals(stringExtra2)) {
                    state = IccCardConstants.State.PIN_REQUIRED;
                } else if ("PUK".equals(stringExtra2)) {
                    state = IccCardConstants.State.PUK_REQUIRED;
                } else if ("NETWORK".equals(stringExtra2)) {
                    state = IccCardConstants.State.NETWORK_LOCKED;
                } else {
                    if ("NETWORK_SUBSET".equals(stringExtra2)) {
                        i = 1;
                        state2 = IccCardConstants.State.NETWORK_LOCKED;
                    } else if ("SERVICE_PROVIDER".equals(stringExtra2)) {
                        i = 2;
                        state2 = IccCardConstants.State.NETWORK_LOCKED;
                    } else if ("CORPORATE".equals(stringExtra2)) {
                        i = 3;
                        state2 = IccCardConstants.State.NETWORK_LOCKED;
                    } else if ("SIM".equals(stringExtra2)) {
                        i = 4;
                        state2 = IccCardConstants.State.NETWORK_LOCKED;
                    } else {
                        state = IccCardConstants.State.UNKNOWN;
                    }
                    i2 = i;
                    state = state2;
                }
            } else if ("NETWORK".equals(stringExtra)) {
                state = IccCardConstants.State.NETWORK_LOCKED;
            } else if ("CARD_IO_ERROR".equals(stringExtra)) {
                state = IccCardConstants.State.CARD_IO_ERROR;
            } else if ("LOADED".equals(stringExtra) || "IMSI".equals(stringExtra)) {
                state = IccCardConstants.State.READY;
            } else if ("NOT_READY".equals(stringExtra)) {
                state = IccCardConstants.State.NOT_READY;
            } else {
                state = IccCardConstants.State.UNKNOWN;
            }
            return new SimData(state, intExtra, intExtra2, i2);
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

        public BatteryStatus(int i, int i2, int i3, int i4, int i5) {
            this.status = i;
            this.level = i2;
            this.plugged = i3;
            this.health = i4;
            this.maxChargingWattage = i5;
        }

        public boolean isPluggedIn() {
            return this.plugged == 1 || this.plugged == 2 || this.plugged == 4;
        }

        public boolean isPluggedInWired() {
            return this.plugged == 1 || this.plugged == 2;
        }

        public boolean isCharged() {
            return this.status == 5 || this.level >= 100;
        }

        public final int getChargingSpeed(int i, int i2) {
            if (this.maxChargingWattage <= 0) {
                return -1;
            }
            if (this.maxChargingWattage < i) {
                return 0;
            }
            return this.maxChargingWattage > i2 ? 2 : 1;
        }

        public String toString() {
            return "BatteryStatus{status=" + this.status + ",level=" + this.level + ",plugged=" + this.plugged + ",health=" + this.health + ",maxChargingWattage=" + this.maxChargingWattage + "}";
        }
    }

    public class StrongAuthTracker extends LockPatternUtils.StrongAuthTracker {
        public StrongAuthTracker(Context context) {
            super(context);
        }

        public boolean isUnlockingWithFingerprintAllowed() {
            return isFingerprintAllowedForUser(KeyguardUpdateMonitor.getCurrentUser());
        }

        public boolean hasUserAuthenticatedSinceBoot() {
            return (getStrongAuthForUser(KeyguardUpdateMonitor.getCurrentUser()) & 1) == 0;
        }

        public void onStrongAuthRequiredChanged(int i) {
            KeyguardUpdateMonitor.this.notifyStrongAuthStateChanged(i);
        }
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    protected void handleStartedWakingUp() {
        Trace.beginSection("KeyguardUpdateMonitor#handleStartedWakingUp");
        updateFingerprintListeningState();
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStartedWakingUp();
            }
        }
        Trace.endSection();
    }

    protected void handleStartedGoingToSleep(int i) {
        clearFingerprintRecognized();
        int size = this.mCallbacks.size();
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onStartedGoingToSleep(i);
            }
        }
        this.mGoingToSleep = true;
        updateFingerprintListeningState();
    }

    protected void handleFinishedGoingToSleep(int i) {
        this.mGoingToSleep = false;
        int size = this.mCallbacks.size();
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onFinishedGoingToSleep(i);
            }
        }
        updateFingerprintListeningState();
    }

    private void handleScreenTurnedOn() {
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onScreenTurnedOn();
            }
        }
    }

    private void handleScreenTurnedOff() {
        this.mHardwareUnavailableRetryCount = 0;
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onScreenTurnedOff();
            }
        }
    }

    private void handleDreamingStateChanged(int i) {
        int size = this.mCallbacks.size();
        this.mIsDreaming = i == 1;
        for (int i2 = 0; i2 < size; i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDreamingStateChanged(this.mIsDreaming);
            }
        }
        updateFingerprintListeningState();
    }

    private void handleUserInfoChanged(int i) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserInfoChanged(i);
            }
        }
    }

    private void handleUserUnlocked() {
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserUnlocked();
            }
        }
    }

    @VisibleForTesting
    protected KeyguardUpdateMonitor(Context context) {
        this.mContext = context;
        this.mSubscriptionManager = SubscriptionManager.from(context);
        this.mStrongAuthTracker = new StrongAuthTracker(context);
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "mDeviceProvisioned is:" + this.mDeviceProvisioned);
        }
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        this.mBatteryStatus = new BatteryStatus(1, 100, 0, 0, 0);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        initMembers();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_TICK");
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.AIRPLANE_MODE");
        intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
        intentFilter.addAction("android.intent.action.SERVICE_STATE");
        intentFilter.addAction("android.intent.action.PHONE_STATE");
        intentFilter.addAction("android.media.RINGER_MODE_CHANGED");
        intentFilter.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intentFilter.addAction("com.mediatek.phone.ACTION_UNLOCK_SIM_LOCK");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter, null, this.mHandler);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.setPriority(1000);
        intentFilter2.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mBroadcastReceiver, intentFilter2, null, this.mHandler);
        IntentFilter intentFilter3 = new IntentFilter();
        intentFilter3.addAction("android.intent.action.USER_INFO_CHANGED");
        intentFilter3.addAction("android.app.action.NEXT_ALARM_CLOCK_CHANGED");
        intentFilter3.addAction("com.android.facelock.FACE_UNLOCK_STARTED");
        intentFilter3.addAction("com.android.facelock.FACE_UNLOCK_STOPPED");
        intentFilter3.addAction("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED");
        intentFilter3.addAction("android.intent.action.USER_UNLOCKED");
        context.registerReceiverAsUser(this.mBroadcastAllReceiver, UserHandle.ALL, intentFilter3, null, this.mHandler);
        this.mSubscriptionManager.addOnSubscriptionsChangedListener(this.mSubscriptionListener);
        try {
            ActivityManager.getService().registerUserSwitchObserver(new UserSwitchObserver() {
                public void onUserSwitching(int i, IRemoteCallback iRemoteCallback) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(310, i, 0, iRemoteCallback));
                }

                public void onUserSwitchComplete(int i) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, i, 0));
                }
            }, "KeyguardUpdateMonitor");
        } catch (RemoteException e) {
            e.rethrowAsRuntimeException();
        }
        this.mTrustManager = (TrustManager) context.getSystemService("trust");
        this.mTrustManager.registerTrustListener(this);
        this.mLockPatternUtils = new LockPatternUtils(context);
        this.mLockPatternUtils.registerStrongAuthTracker(this.mStrongAuthTracker);
        this.mDreamManager = IDreamManager.Stub.asInterface(ServiceManager.getService("dreams"));
        if (this.mContext.getPackageManager().hasSystemFeature("android.hardware.fingerprint")) {
            this.mFpm = (FingerprintManager) context.getSystemService("fingerprint");
        }
        updateFingerprintListeningState();
        if (this.mFpm != null) {
            this.mFpm.addLockoutResetCallback(this.mLockoutResetCallback);
        }
        ActivityManagerWrapper.getInstance().registerTaskStackListener(this.mTaskStackListener);
        this.mUserManager = (UserManager) context.getSystemService(UserManager.class);
        this.mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(DevicePolicyManager.class);
        this.mLogoutEnabled = this.mDevicePolicyManager.isLogoutEnabled();
    }

    private void updateFingerprintListeningState() {
        if (this.mHandler.hasMessages(336)) {
            return;
        }
        this.mHandler.removeCallbacks(this.mRetryFingerprintAuthentication);
        boolean zShouldListenForFingerprint = shouldListenForFingerprint();
        if (this.mFingerprintRunningState == 1 && !zShouldListenForFingerprint) {
            stopListeningForFingerprint();
        } else if (this.mFingerprintRunningState != 1 && zShouldListenForFingerprint) {
            startListeningForFingerprint();
        }
    }

    private boolean shouldListenForFingerprintAssistant() {
        return this.mAssistantVisible && this.mKeyguardOccluded && !this.mUserFingerprintAuthenticated.get(getCurrentUser(), false) && !this.mUserHasTrust.get(getCurrentUser(), false);
    }

    private boolean shouldListenForFingerprint() {
        return ((!this.mKeyguardIsVisible && this.mDeviceInteractive && ((!this.mBouncer || this.mKeyguardGoingAway) && !this.mGoingToSleep && !shouldListenForFingerprintAssistant() && (!this.mKeyguardOccluded || !this.mIsDreaming))) || this.mSwitchingUser || isFingerprintDisabled(getCurrentUser()) || this.mKeyguardGoingAway) ? false : true;
    }

    private void startListeningForFingerprint() {
        if (this.mFingerprintRunningState == 2) {
            setFingerprintRunningState(3);
            return;
        }
        if (DEBUG) {
            Log.v("KeyguardUpdateMonitor", "startListeningForFingerprint()");
        }
        int currentUser = ActivityManager.getCurrentUser();
        if (isUnlockWithFingerprintPossible(currentUser)) {
            if (this.mFingerprintCancelSignal != null) {
                this.mFingerprintCancelSignal.cancel();
            }
            this.mFingerprintCancelSignal = new CancellationSignal();
            this.mFpm.authenticate(null, this.mFingerprintCancelSignal, 0, this.mAuthenticationCallback, null, currentUser);
            setFingerprintRunningState(1);
        }
    }

    public boolean isUnlockWithFingerprintPossible(int i) {
        return this.mFpm != null && this.mFpm.isHardwareDetected() && !isFingerprintDisabled(i) && this.mFpm.getEnrolledFingerprints(i).size() > 0;
    }

    private void stopListeningForFingerprint() {
        if (DEBUG) {
            Log.v("KeyguardUpdateMonitor", "stopListeningForFingerprint()");
        }
        if (this.mFingerprintRunningState == 1) {
            if (this.mFingerprintCancelSignal != null) {
                this.mFingerprintCancelSignal.cancel();
                this.mFingerprintCancelSignal = null;
            }
            setFingerprintRunningState(2);
        }
        if (this.mFingerprintRunningState == 3) {
            setFingerprintRunningState(2);
        }
    }

    private boolean isDeviceProvisionedInSettingsDb() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private void watchForDeviceProvisioning() {
        this.mDeviceProvisionedObserver = new ContentObserver(this.mHandler) {
            @Override
            public void onChange(boolean z) {
                super.onChange(z);
                KeyguardUpdateMonitor.this.mDeviceProvisioned = KeyguardUpdateMonitor.this.isDeviceProvisionedInSettingsDb();
                if (KeyguardUpdateMonitor.this.mDeviceProvisioned) {
                    KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(308);
                }
                if (KeyguardUpdateMonitor.DEBUG) {
                    Log.d("KeyguardUpdateMonitor", "DEVICE_PROVISIONED state = " + KeyguardUpdateMonitor.this.mDeviceProvisioned);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean zIsDeviceProvisionedInSettingsDb = isDeviceProvisionedInSettingsDb();
        if (zIsDeviceProvisionedInSettingsDb != this.mDeviceProvisioned) {
            this.mDeviceProvisioned = zIsDeviceProvisionedInSettingsDb;
            if (this.mDeviceProvisioned) {
                this.mHandler.sendEmptyMessage(308);
            }
        }
    }

    public void setHasLockscreenWallpaper(boolean z) {
        checkIsHandlerThread();
        if (z != this.mHasLockscreenWallpaper) {
            this.mHasLockscreenWallpaper = z;
            for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(size).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onHasLockscreenWallpaperChanged(z);
                }
            }
        }
    }

    public boolean hasLockscreenWallpaper() {
        return this.mHasLockscreenWallpaper;
    }

    private void handleDevicePolicyManagerStateChanged() {
        updateFingerprintListeningState();
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(size).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDevicePolicyManagerStateChanged();
            }
        }
    }

    private void handleUserSwitching(int i, IRemoteCallback iRemoteCallback) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserSwitching(i);
            }
        }
        try {
            iRemoteCallback.sendResult((Bundle) null);
        } catch (RemoteException e) {
        }
    }

    private void handleUserSwitchComplete(int i) {
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onUserSwitchComplete(i);
            }
        }
    }

    public void dispatchBootCompleted() {
        this.mHandler.sendEmptyMessage(313);
    }

    private void handleBootCompleted() {
        if (this.mBootCompleted) {
            return;
        }
        this.mBootCompleted = true;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onBootCompleted();
            }
        }
    }

    private void handleDeviceProvisioned() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onDeviceProvisioned();
            }
        }
        if (this.mDeviceProvisionedObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
            this.mDeviceProvisionedObserver = null;
        }
    }

    protected void handlePhoneStateChanged() {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged");
        }
        this.mPhoneState = 0;
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            int callState = TelephonyManager.getDefault().getCallState(KeyguardUtils.getSubIdUsingPhoneId(i));
            if (callState == 2) {
                this.mPhoneState = callState;
            } else if (callState == 1 && this.mPhoneState == 0) {
                this.mPhoneState = callState;
            }
        }
        Log.d("KeyguardUpdateMonitor", "handlePhoneStateChanged() - mPhoneState = " + this.mPhoneState);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onPhoneStateChanged(this.mPhoneState);
            }
        }
    }

    private void handleRingerModeChange(int i) {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleRingerModeChange(" + i + ")");
        }
        this.mRingMode = i;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRingerModeChanged(i);
            }
        }
    }

    private void handleTimeUpdate() {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleTimeUpdate");
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onTimeChanged();
            }
        }
    }

    private void handleBatteryUpdate(BatteryStatus batteryStatus) {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleBatteryUpdate");
        }
        boolean zIsBatteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, batteryStatus);
        this.mBatteryStatus = batteryStatus;
        if (zIsBatteryUpdateInteresting) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onRefreshBatteryInfo(batteryStatus);
                }
            }
        }
    }

    @VisibleForTesting
    void handleSimStateChange(int i, int i2, IccCardConstants.State state) {
        checkIsHandlerThread();
    }

    private void handleSimStateChange(SimData simData) {
        handleSimStateChange(simData.subId, simData.phoneId, simData.simState);
        IccCardConstants.State state = simData.simState;
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleSimStateChange: intentValue = " + simData + " state resolved to " + state.toString() + " phoneId=" + simData.phoneId);
        }
        if (state != this.mSimStateOfPhoneId.get(Integer.valueOf(simData.phoneId))) {
            setSimmeDismissFlagOfPhoneId(simData.phoneId, false);
        }
        if (state != IccCardConstants.State.UNKNOWN) {
            if (state == IccCardConstants.State.NETWORK_LOCKED || state != this.mSimStateOfPhoneId.get(Integer.valueOf(simData.phoneId))) {
                this.mSimStateOfPhoneId.put(Integer.valueOf(simData.phoneId), state);
                int i = simData.phoneId;
                printState();
                for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
                    KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
                    if (keyguardUpdateMonitorCallback != null) {
                        keyguardUpdateMonitorCallback.onSimStateChangedUsingPhoneId(i, state);
                    }
                }
            }
        }
    }

    private void handleServiceStateChange(int i, ServiceState serviceState) {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleServiceStateChange(subId=" + i + ", serviceState=" + serviceState);
        }
        this.mServiceStates.put(Integer.valueOf(i), serviceState);
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }

    public void onKeyguardVisibilityChanged(boolean z) {
        checkIsHandlerThread();
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "onKeyguardVisibilityChanged(" + z + ")");
        }
        this.mKeyguardIsVisible = z;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onKeyguardVisibilityChangedRaw(z);
            }
        }
        updateFingerprintListeningState();
    }

    private void handleKeyguardReset() {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleKeyguardReset");
        }
        updateFingerprintListeningState();
        this.mNeedsSlowUnlockTransition = resolveNeedsSlowUnlockTransition();
    }

    private boolean resolveNeedsSlowUnlockTransition() {
        if (this.mUserManager.isUserUnlocked(getCurrentUser())) {
            return false;
        }
        return FALLBACK_HOME_COMPONENT.equals(this.mContext.getPackageManager().resolveActivity(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"), 0).getComponentInfo().getComponentName());
    }

    private void handleKeyguardBouncerChanged(int i) {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "handleKeyguardBouncerChanged(" + i + ")");
        }
        boolean z = i == 1;
        this.mBouncer = z;
        for (int i2 = 0; i2 < this.mCallbacks.size(); i2++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i2).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onKeyguardBouncerChanged(z);
            }
        }
        updateFingerprintListeningState();
    }

    private void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onEmergencyCallAction();
            }
        }
    }

    private boolean isBatteryUpdateInteresting(BatteryStatus batteryStatus, BatteryStatus batteryStatus2) {
        boolean zIsPluggedIn = batteryStatus2.isPluggedIn();
        boolean zIsPluggedIn2 = batteryStatus.isPluggedIn();
        boolean z = zIsPluggedIn2 && zIsPluggedIn && batteryStatus.status != batteryStatus2.status;
        if (zIsPluggedIn2 == zIsPluggedIn && !z && batteryStatus.level == batteryStatus2.level) {
            return zIsPluggedIn && batteryStatus2.maxChargingWattage != batteryStatus.maxChargingWattage;
        }
        return true;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        checkIsHandlerThread();
        if (DEBUG) {
            Log.v("KeyguardUpdateMonitor", "*** unregister callback for " + keyguardUpdateMonitorCallback);
        }
        for (int size = this.mCallbacks.size() - 1; size >= 0; size--) {
            if (this.mCallbacks.get(size).get() == keyguardUpdateMonitorCallback) {
                this.mCallbacks.remove(size);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        checkIsHandlerThread();
        if (DEBUG) {
            Log.v("KeyguardUpdateMonitor", "*** register callback for " + keyguardUpdateMonitorCallback);
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == keyguardUpdateMonitorCallback) {
                if (DEBUG) {
                    Log.e("KeyguardUpdateMonitor", "Object tried to add another callback", new Exception("Called by"));
                    return;
                }
                return;
            }
        }
        this.mCallbacks.add(new WeakReference<>(keyguardUpdateMonitorCallback));
        removeCallback(null);
        sendUpdates(keyguardUpdateMonitorCallback);
    }

    public boolean isSwitchingUser() {
        return this.mSwitchingUser;
    }

    public void setSwitchingUser(boolean z) {
        this.mSwitchingUser = z;
        this.mHandler.post(this.mUpdateFingerprintListeningState);
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback) {
        keyguardUpdateMonitorCallback.onRefreshBatteryInfo(this.mBatteryStatus);
        keyguardUpdateMonitorCallback.onTimeChanged();
        keyguardUpdateMonitorCallback.onRingerModeChanged(this.mRingMode);
        keyguardUpdateMonitorCallback.onPhoneStateChanged(this.mPhoneState);
        keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
        keyguardUpdateMonitorCallback.onClockVisibilityChanged();
        keyguardUpdateMonitorCallback.onKeyguardVisibilityChangedRaw(this.mKeyguardIsVisible);
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            keyguardUpdateMonitorCallback.onSimStateChangedUsingPhoneId(i, this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
        }
    }

    public void sendKeyguardReset() {
        this.mHandler.obtainMessage(312).sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean z) {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "sendKeyguardBouncerChanged(" + z + ")");
        }
        Message messageObtainMessage = this.mHandler.obtainMessage(322);
        messageObtainMessage.arg1 = z ? 1 : 0;
        messageObtainMessage.sendToTarget();
    }

    public void reportSimUnlocked(int i) {
        handleSimStateChange(new SimData(IccCardConstants.State.READY, i, KeyguardUtils.getSubIdUsingPhoneId(i)));
    }

    public void reportEmergencyCallAction(boolean z) {
        if (!z) {
            this.mHandler.obtainMessage(318).sendToTarget();
        } else {
            checkIsHandlerThread();
            handleReportEmergencyCallAction();
        }
    }

    public boolean isDeviceProvisioned() {
        if (!this.mDeviceProvisioned) {
            Log.d("KeyguardUpdateMonitor", "isDeviceProvisioned get DEVICE_PROVISIONED from db again !!");
            return Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }
        return this.mDeviceProvisioned;
    }

    public void clearFailedUnlockAttempts() {
        this.mFailedAttempts.delete(sCurrentUser);
    }

    public ServiceState getServiceState(int i) {
        return this.mServiceStates.get(Integer.valueOf(i));
    }

    public int getFailedUnlockAttempts(int i) {
        return this.mFailedAttempts.get(i, 0);
    }

    public void reportFailedStrongAuthUnlockAttempt(int i) {
        this.mFailedAttempts.put(i, getFailedUnlockAttempts(i) + 1);
    }

    public void clearFingerprintRecognized() {
        this.mUserFingerprintAuthenticated.clear();
        this.mTrustManager.clearAllFingerprints();
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    public boolean isSimPinSecure() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            if (isSimPinSecure(i)) {
                return true;
            }
        }
        return false;
    }

    public IccCardConstants.State getSimState(int i) {
        if (this.mSimDatas.containsKey(Integer.valueOf(i))) {
            return this.mSimDatas.get(Integer.valueOf(i)).simState;
        }
        return IccCardConstants.State.UNKNOWN;
    }

    private boolean refreshSimState(int i, int i2) {
        IccCardConstants.State stateIntToState;
        int simState = TelephonyManager.from(this.mContext).getSimState(i2);
        try {
            stateIntToState = IccCardConstants.State.intToState(simState);
        } catch (IllegalArgumentException e) {
            Log.w("KeyguardUpdateMonitor", "Unknown sim state: " + simState);
            stateIntToState = IccCardConstants.State.UNKNOWN;
        }
        IccCardConstants.State state = this.mSimStateOfPhoneId.get(Integer.valueOf(i2));
        boolean z = false;
        boolean z2 = state != stateIntToState;
        if (state != IccCardConstants.State.READY || stateIntToState != IccCardConstants.State.PIN_REQUIRED) {
            z = z2;
        }
        if (z) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(i2), stateIntToState);
        }
        Log.d("KeyguardUpdateMonitor", "refreshSimState() - sub = " + i + " phoneId = " + i2 + ", ori-state = " + state + ", new-state = " + stateIntToState + ", changed = " + z);
        return z;
    }

    public boolean isSimPinSecure(int i) {
        IccCardConstants.State state = this.mSimStateOfPhoneId.get(Integer.valueOf(i));
        return (state == IccCardConstants.State.PIN_REQUIRED || state == IccCardConstants.State.PUK_REQUIRED || (state == IccCardConstants.State.NETWORK_LOCKED && KeyguardUtils.isMediatekSimMeLockSupport())) && !getPinPukMeDismissFlagOfPhoneId(i);
    }

    public void dispatchStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchStartedGoingToSleep(int i) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(321, i, 0));
    }

    public void dispatchFinishedGoingToSleep(int i) {
        synchronized (this) {
            this.mDeviceInteractive = false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(320, i, 0));
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

    public void dispatchDreamingStarted() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(333, 1, 0));
    }

    public void dispatchDreamingStopped() {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(333, 0, 0));
    }

    public boolean isDeviceInteractive() {
        return this.mDeviceInteractive;
    }

    public boolean isGoingToSleep() {
        return this.mGoingToSleep;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int i) {
        return getSubscriptionInfoForSubId(i, false);
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int i, boolean z) {
        List<SubscriptionInfo> subscriptionInfo = getSubscriptionInfo(z);
        for (int i2 = 0; i2 < subscriptionInfo.size(); i2++) {
            SubscriptionInfo subscriptionInfo2 = subscriptionInfo.get(i2);
            if (i == subscriptionInfo2.getSubscriptionId()) {
                return subscriptionInfo2;
            }
        }
        return null;
    }

    public boolean isLogoutEnabled() {
        return this.mLogoutEnabled;
    }

    private void updateLogoutEnabled() {
        checkIsHandlerThread();
        boolean zIsLogoutEnabled = this.mDevicePolicyManager.isLogoutEnabled();
        if (this.mLogoutEnabled != zIsLogoutEnabled) {
            this.mLogoutEnabled = zIsLogoutEnabled;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i).get();
                if (keyguardUpdateMonitorCallback != null) {
                    keyguardUpdateMonitorCallback.onLogoutEnabledChanged();
                }
            }
        }
    }

    private void checkIsHandlerThread() {
        if (!sDisableHandlerCheckForTesting && !this.mHandler.getLooper().isCurrentThread()) {
            Log.wtf("KeyguardUpdateMonitor", "must call on mHandler's thread " + this.mHandler.getLooper().getThread() + ", not " + Thread.currentThread());
        }
    }

    @VisibleForTesting
    public static void disableHandlerCheckForTesting(Instrumentation instrumentation) {
        Preconditions.checkNotNull(instrumentation, "Must only call this method in tests!");
        sDisableHandlerCheckForTesting = true;
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("KeyguardUpdateMonitor state:");
        printWriter.println("  SIM States:");
        Iterator<SimData> it = this.mSimDatas.values().iterator();
        while (it.hasNext()) {
            printWriter.println("    " + it.next().toString());
        }
        printWriter.println("  Subs:");
        if (this.mSubscriptionInfo != null) {
            for (int i = 0; i < this.mSubscriptionInfo.size(); i++) {
                printWriter.println("    " + this.mSubscriptionInfo.get(i));
            }
        }
        printWriter.println("  Service states:");
        Iterator<Integer> it2 = this.mServiceStates.keySet().iterator();
        while (it2.hasNext()) {
            int iIntValue = it2.next().intValue();
            printWriter.println("    " + iIntValue + "=" + this.mServiceStates.get(Integer.valueOf(iIntValue)));
        }
        if (this.mFpm != null && this.mFpm.isHardwareDetected()) {
            int currentUser = ActivityManager.getCurrentUser();
            int strongAuthForUser = this.mStrongAuthTracker.getStrongAuthForUser(currentUser);
            printWriter.println("  Fingerprint state (user=" + currentUser + ")");
            StringBuilder sb = new StringBuilder();
            sb.append("    allowed=");
            sb.append(isUnlockingWithFingerprintAllowed());
            printWriter.println(sb.toString());
            printWriter.println("    auth'd=" + this.mUserFingerprintAuthenticated.get(currentUser));
            printWriter.println("    authSinceBoot=" + getStrongAuthTracker().hasUserAuthenticatedSinceBoot());
            printWriter.println("    disabled(DPM)=" + isFingerprintDisabled(currentUser));
            printWriter.println("    possible=" + isUnlockWithFingerprintPossible(currentUser));
            printWriter.println("    strongAuthFlags=" + Integer.toHexString(strongAuthForUser));
            printWriter.println("    trustManaged=" + getUserTrustIsManaged(currentUser));
        }
    }

    private void initMembers() {
        if (DEBUG) {
            Log.d("KeyguardUpdateMonitor", "initMembers() - NumOfPhone=" + KeyguardUtils.getNumOfPhone());
        }
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            this.mSimStateOfPhoneId.put(Integer.valueOf(i), IccCardConstants.State.UNKNOWN);
            this.mTelephonyPlmn.put(Integer.valueOf(i), getDefaultPlmn());
            this.mTelephonyCsgId.put(Integer.valueOf(i), "");
            this.mTelephonyHnbName.put(Integer.valueOf(i), "");
            this.mSimMeCategory.put(Integer.valueOf(i), 0);
            this.mSimMeLeftRetryCount.put(Integer.valueOf(i), 5);
        }
    }

    private void proceedToHandleSimStateChanged(SimData simData) {
        if (IccCardConstants.State.NETWORK_LOCKED == simData.simState && KeyguardUtils.isMediatekSimMeLockSupport()) {
            new simMeStatusQueryThread(simData).start();
        } else {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(304, simData));
        }
    }

    public void setPinPukMeDismissFlagOfPhoneId(int i, boolean z) {
        Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId() - phoneId = " + i);
        if (!KeyguardUtils.isValidPhoneId(i)) {
            return;
        }
        int i2 = 1 << i;
        if (z) {
            this.mPinPukMeDismissFlag = i2 | this.mPinPukMeDismissFlag;
        } else {
            this.mPinPukMeDismissFlag = (~i2) & this.mPinPukMeDismissFlag;
        }
    }

    public boolean getPinPukMeDismissFlagOfPhoneId(int i) {
        int i2 = 1 << i;
        return (this.mPinPukMeDismissFlag & i2) == i2;
    }

    public void setSimmeDismissFlagOfPhoneId(int i, boolean z) {
        Log.d("KeyguardUpdateMonitor", "setSimmeDismissFlagOfPhoneId() - phoneId = " + i + ", dismiss=" + z);
        if (!KeyguardUtils.isValidPhoneId(i)) {
            return;
        }
        int i2 = 16 << i;
        if (z) {
            this.mSimmeDismissFlag = i2 | this.mSimmeDismissFlag;
        } else {
            this.mSimmeDismissFlag = (~i2) & this.mSimmeDismissFlag;
        }
    }

    public boolean getSimmeDismissFlagOfPhoneId(int i) {
        int i2 = 16 << i;
        return (this.mSimmeDismissFlag & i2) == i2;
    }

    public int getRetryPukCountOfPhoneId(int i) {
        if (i == 3) {
            return SystemProperties.getInt("vendor.gsm.sim.retry.puk1.4", -1);
        }
        if (i == 2) {
            return SystemProperties.getInt("vendor.gsm.sim.retry.puk1.3", -1);
        }
        return i == 1 ? SystemProperties.getInt("vendor.gsm.sim.retry.puk1.2", -1) : SystemProperties.getInt("vendor.gsm.sim.retry.puk1", -1);
    }

    private class simMeStatusQueryThread extends Thread {
        SimData simArgs;

        simMeStatusQueryThread(SimData simData) {
            this.simArgs = simData;
        }

        @Override
        public void run() {
            try {
                KeyguardUpdateMonitor.this.mSimMeCategory.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(this.simArgs.simMECategory));
                Log.d("KeyguardUpdateMonitor", "queryNetworkLock, phoneId =" + this.simArgs.phoneId + ", simMECategory =" + this.simArgs.simMECategory);
                if (this.simArgs.simMECategory >= 0 && this.simArgs.simMECategory <= 5) {
                    Bundle bundleQueryNetworkLock = IMtkTelephonyEx.Stub.asInterface(ServiceManager.getService("phoneEx")).queryNetworkLock(KeyguardUtils.getSubIdUsingPhoneId(this.simArgs.phoneId), this.simArgs.simMECategory);
                    boolean z = bundleQueryNetworkLock.getBoolean("com.mediatek.phone.QUERY_SIMME_LOCK_RESULT", false);
                    Log.d("KeyguardUpdateMonitor", "queryNetworkLock, query_result =" + z);
                    if (z) {
                        KeyguardUpdateMonitor.this.mSimMeLeftRetryCount.put(Integer.valueOf(this.simArgs.phoneId), Integer.valueOf(bundleQueryNetworkLock.getInt("com.mediatek.phone.SIMME_LOCK_LEFT_COUNT", 5)));
                    } else {
                        Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock result fail");
                    }
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, this.simArgs));
                }
            } catch (Exception e) {
                Log.e("KeyguardUpdateMonitor", "queryIccNetworkLock got exception: " + e.getMessage());
            }
        }
    }

    public int getSimMeCategoryOfPhoneId(int i) {
        return this.mSimMeCategory.get(Integer.valueOf(i)).intValue();
    }

    public int getSimMeLeftRetryCountOfPhoneId(int i) {
        return this.mSimMeLeftRetryCount.get(Integer.valueOf(i)).intValue();
    }

    public void minusSimMeLeftRetryCountOfPhoneId(int i) {
        int iIntValue = this.mSimMeLeftRetryCount.get(Integer.valueOf(i)).intValue();
        if (iIntValue > 0) {
            this.mSimMeLeftRetryCount.put(Integer.valueOf(i), Integer.valueOf(iIntValue - 1));
        }
    }

    private CharSequence getTelephonyHnbNameFrom(Intent intent) {
        return intent.getStringExtra("hnbName");
    }

    private CharSequence getTelephonyCsgIdFrom(Intent intent) {
        return intent.getStringExtra("csgId");
    }

    public IccCardConstants.State getSimStateOfPhoneId(int i) {
        return this.mSimStateOfPhoneId.get(Integer.valueOf(i));
    }

    public int getSimPinLockPhoneId() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            if (DEBUG) {
                Log.d("KeyguardUpdateMonitor", "getSimPinLockSubId, phoneId=" + i + " mSimStateOfPhoneId.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
            }
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(i)) == IccCardConstants.State.PIN_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(i)) {
                return i;
            }
        }
        return -1;
    }

    public int getSimPukLockPhoneId() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            if (DEBUG) {
                Log.d("KeyguardUpdateMonitor", "getSimPukLockSubId, phoneId=" + i + " mSimStateOfSub.get(phoneId)=" + this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
            }
            if (this.mSimStateOfPhoneId.get(Integer.valueOf(i)) == IccCardConstants.State.PUK_REQUIRED && !getPinPukMeDismissFlagOfPhoneId(i) && getRetryPukCountOfPhoneId(i) != 0) {
                return i;
            }
        }
        return -1;
    }

    private CharSequence getTelephonyPlmnFrom(Intent intent) {
        if (intent.getBooleanExtra("showPlmn", false)) {
            String stringExtra = intent.getStringExtra("plmn");
            return stringExtra != null ? stringExtra : getDefaultPlmn();
        }
        return null;
    }

    public CharSequence getDefaultPlmn() {
        return this.mContext.getResources().getText(com.android.systemui.R.string.keyguard_carrier_default);
    }

    private CharSequence getTelephonySpnFrom(Intent intent) {
        String stringExtra;
        if (intent.getBooleanExtra("showSpn", false) && (stringExtra = intent.getStringExtra("spn")) != null) {
            return stringExtra;
        }
        return null;
    }

    private void printState() {
        for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
            Log.d("KeyguardUpdateMonitor", "Phone# " + i + ", state = " + this.mSimStateOfPhoneId.get(Integer.valueOf(i)));
        }
    }

    private void handleAirPlaneModeUpdate(boolean z) {
        int iIntValue;
        if (!z) {
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                setPinPukMeDismissFlagOfPhoneId(i, false);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId false: " + i);
            }
            for (int i2 = 0; i2 < KeyguardUtils.getNumOfPhone(); i2++) {
                if (DEBUG) {
                    Log.d("KeyguardUpdateMonitor", "phoneId = " + i2 + " state=" + this.mSimStateOfPhoneId.get(Integer.valueOf(i2)));
                }
                if (this.mSimStateOfPhoneId.get(Integer.valueOf(i2)) != null && !this.mSimStateOfPhoneId.get(Integer.valueOf(i2)).equals("")) {
                    switch (AnonymousClass11.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[this.mSimStateOfPhoneId.get(Integer.valueOf(i2)).ordinal()]) {
                        case 1:
                        case 2:
                        case 3:
                            IccCardConstants.State state = this.mSimStateOfPhoneId.get(Integer.valueOf(i2));
                            this.mSimStateOfPhoneId.put(Integer.valueOf(i2), IccCardConstants.State.UNKNOWN);
                            if (this.mSimMeCategory.get(Integer.valueOf(i2)) != null) {
                                iIntValue = this.mSimMeCategory.get(Integer.valueOf(i2)).intValue();
                            } else {
                                iIntValue = 0;
                            }
                            SimData simData = new SimData(state, i2, KeyguardUtils.getSubIdUsingPhoneId(i2), iIntValue);
                            if (DEBUG) {
                                Log.v("KeyguardUpdateMonitor", "SimData state=" + simData.simState + ", phoneId=" + simData.phoneId + ", subId=" + simData.subId + ", SimData.simMECategory = " + simData.simMECategory);
                            }
                            proceedToHandleSimStateChanged(simData);
                            break;
                    }
                }
            }
        } else if (z && KeyguardUtils.isFlightModePowerOffMd()) {
            Log.d("KeyguardUpdateMonitor", "Air mode is on, supress all SIM PIN/PUK/ME Lock views.");
            for (int i3 = 0; i3 < KeyguardUtils.getNumOfPhone(); i3++) {
                setPinPukMeDismissFlagOfPhoneId(i3, true);
                Log.d("KeyguardUpdateMonitor", "setPinPukMeDismissFlagOfPhoneId true: " + i3);
            }
        }
        for (int i4 = 0; i4 < this.mCallbacks.size(); i4++) {
            KeyguardUpdateMonitorCallback keyguardUpdateMonitorCallback = this.mCallbacks.get(i4).get();
            if (keyguardUpdateMonitorCallback != null) {
                keyguardUpdateMonitorCallback.onAirPlaneModeChanged(z);
                keyguardUpdateMonitorCallback.onRefreshCarrierInfo();
            }
        }
    }

    static class AnonymousClass11 {
        static final int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }

    private boolean isAirplaneModeOn() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1;
    }

    public void setDismissFlagWhenWfcOn(IccCardConstants.State state) {
        if ((state == IccCardConstants.State.PIN_REQUIRED || state == IccCardConstants.State.PUK_REQUIRED || state == IccCardConstants.State.NETWORK_LOCKED) && isAirplaneModeOn() && isWifiEnabled() && KeyguardUtils.isFlightModePowerOffMd()) {
            for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                setPinPukMeDismissFlagOfPhoneId(i, false);
                Log.d("KeyguardUpdateMonitor", "Wifi calling opened MD, setPinPukMeDismissFlagOfPhoneId false: " + i);
            }
        }
    }

    private boolean isWifiEnabled() {
        int wifiState = this.mWifiManager.getWifiState();
        Log.d("KeyguardUpdateMonitor", "wifi state:" + wifiState);
        return wifiState != 1;
    }
}
