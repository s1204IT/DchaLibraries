package com.android.keyguard;

import android.app.ActivityManagerNative;
import android.app.IUserSwitchObserver;
import android.app.admin.DevicePolicyManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IRemoteCallback;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.fingerprint.FingerprintManager;
import android.service.fingerprint.FingerprintManagerReceiver;
import android.service.fingerprint.FingerprintUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import com.android.internal.telephony.IccCardConstants;
import com.google.android.collect.Lists;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyguardUpdateMonitor implements TrustManager.TrustListener {
    private static KeyguardUpdateMonitor sInstance;
    private boolean mAlternateUnlockEnabled;
    private BatteryStatus mBatteryStatus;
    private boolean mBootCompleted;
    private boolean mBouncer;
    private final Context mContext;
    private ContentObserver mDeviceProvisionedObserver;
    private boolean mKeyguardIsVisible;
    private int mPhoneState;
    private int mRingMode;
    private boolean mScreenOn;
    private List<SubscriptionInfo> mSubscriptionInfo;
    private SubscriptionManager mSubscriptionManager;
    private boolean mSwitchingUser;
    HashMap<Integer, SimData> mSimDatas = new HashMap<>();
    private int mFailedAttempts = 0;
    private int mFailedBiometricUnlockAttempts = 0;
    private final ArrayList<WeakReference<KeyguardUpdateMonitorCallback>> mCallbacks = Lists.newArrayList();
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 301:
                    KeyguardUpdateMonitor.this.handleTimeUpdate();
                    break;
                case 302:
                    KeyguardUpdateMonitor.this.handleBatteryUpdate((BatteryStatus) msg.obj);
                    break;
                case 304:
                    KeyguardUpdateMonitor.this.handleSimStateChange(msg.arg1, msg.arg2, (IccCardConstants.State) msg.obj);
                    break;
                case 305:
                    KeyguardUpdateMonitor.this.handleRingerModeChange(msg.arg1);
                    break;
                case 306:
                    KeyguardUpdateMonitor.this.handlePhoneStateChanged((String) msg.obj);
                    break;
                case 307:
                    KeyguardUpdateMonitor.this.handleClockVisibilityChanged();
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
                case 311:
                    KeyguardUpdateMonitor.this.handleUserRemoved(msg.arg1);
                    break;
                case 312:
                    KeyguardUpdateMonitor.this.handleKeyguardVisibilityChanged(msg.arg1);
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
                    KeyguardUpdateMonitor.this.handleScreenTurnedOn();
                    break;
                case 320:
                    KeyguardUpdateMonitor.this.handleScreenTurnedOff(msg.arg1);
                    break;
                case 322:
                    KeyguardUpdateMonitor.this.handleKeyguardBouncerChanged(msg.arg1);
                    break;
                case 323:
                    KeyguardUpdateMonitor.this.handleFingerprintProcessed(msg.arg1);
                    break;
                case 324:
                    KeyguardUpdateMonitor.this.handleFingerprintAcquired(msg.arg1);
                    break;
                case 325:
                    KeyguardUpdateMonitor.this.handleFaceUnlockStateChanged(msg.arg1 != 0, msg.arg2);
                    break;
                case 326:
                    KeyguardUpdateMonitor.this.handleSimSubscriptionInfoChanged();
                    break;
            }
        }
    };
    private SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionListener = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(326);
        }
    };
    private SparseBooleanArray mUserHasTrust = new SparseBooleanArray();
    private SparseBooleanArray mUserTrustIsManaged = new SparseBooleanArray();
    private SparseBooleanArray mUserFingerprintRecognized = new SparseBooleanArray();
    private SparseBooleanArray mUserFaceUnlockRunning = new SparseBooleanArray();
    private DisplayClientState mDisplayClientState = new DisplayClientState();
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.TIME_TICK".equals(action) || "android.intent.action.TIME_SET".equals(action) || "android.intent.action.TIMEZONE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(301);
                return;
            }
            if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int status = intent.getIntExtra("status", 1);
                int plugged = intent.getIntExtra("plugged", 0);
                int level = intent.getIntExtra("level", 0);
                int health = intent.getIntExtra("health", 1);
                Message msg = KeyguardUpdateMonitor.this.mHandler.obtainMessage(302, new BatteryStatus(status, level, plugged, health));
                KeyguardUpdateMonitor.this.mHandler.sendMessage(msg);
                return;
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action)) {
                SimData args = SimData.fromIntent(intent);
                Log.v("KeyguardUpdateMonitor", "action " + action + " state: " + intent.getStringExtra("ss") + " slotId: " + args.slotId + " subid: " + args.subId);
                KeyguardUpdateMonitor.this.mHandler.obtainMessage(304, args.subId, args.slotId, args.simState).sendToTarget();
            } else {
                if ("android.media.RINGER_MODE_CHANGED".equals(action)) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(305, intent.getIntExtra("android.media.EXTRA_RINGER_MODE", -1), 0));
                    return;
                }
                if ("android.intent.action.PHONE_STATE".equals(action)) {
                    String state = intent.getStringExtra("state");
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(306, state));
                } else if ("android.intent.action.USER_REMOVED".equals(action)) {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(311, intent.getIntExtra("android.intent.extra.user_handle", 0), 0));
                } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                    KeyguardUpdateMonitor.this.dispatchBootCompleted();
                }
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
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(325, 1, getSendingUserId()));
            } else if ("com.android.facelock.FACE_UNLOCK_STOPPED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(325, 0, getSendingUserId()));
            } else if ("android.app.action.DEVICE_POLICY_MANAGER_STATE_CHANGED".equals(action)) {
                KeyguardUpdateMonitor.this.mHandler.sendEmptyMessage(309);
            }
        }
    };
    private FingerprintManagerReceiver mFingerprintManagerReceiver = new FingerprintManagerReceiver() {
        public void onProcessed(int fingerprintId) {
            KeyguardUpdateMonitor.this.mHandler.obtainMessage(323, fingerprintId, 0).sendToTarget();
        }

        public void onAcquired(int info) {
            KeyguardUpdateMonitor.this.mHandler.obtainMessage(324, info, 0).sendToTarget();
        }

        public void onError(int error) {
        }
    };
    private boolean mDeviceProvisioned = isDeviceProvisionedInSettingsDb();

    public void onTrustChanged(boolean enabled, int userId, boolean initiatedByUser) {
        this.mUserHasTrust.put(userId, enabled);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTrustChanged(userId);
                if (enabled && initiatedByUser) {
                    cb.onTrustInitiatedByUser(userId);
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
            SimData data = this.mSimDatas.get(Integer.valueOf(changedSubscriptions.get(i2).getSubscriptionId()));
            for (int j = 0; j < this.mCallbacks.size(); j++) {
                KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(j).get();
                if (cb != null) {
                    cb.onSimStateChanged(data.subId, data.slotId, data.simState);
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

    List<SubscriptionInfo> getSubscriptionInfo(boolean forceReload) {
        List<SubscriptionInfo> sil = this.mSubscriptionInfo;
        if (sil == null || forceReload) {
            sil = this.mSubscriptionManager.getActiveSubscriptionInfoList();
        }
        if (sil == null) {
            this.mSubscriptionInfo = new ArrayList();
        } else {
            this.mSubscriptionInfo = sil;
        }
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

    private void onFingerprintRecognized(int userId) {
        this.mUserFingerprintRecognized.put(userId, true);
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintRecognized(userId);
            }
        }
    }

    public void handleFingerprintProcessed(int fingerprintId) {
        if (fingerprintId != 0) {
            try {
                int userId = ActivityManagerNative.getDefault().getCurrentUser().id;
                if (isFingerprintDisabled(userId)) {
                    Log.d("KeyguardUpdateMonitor", "Fingerprint disabled by DPM for userId: " + userId);
                    return;
                }
                ContentResolver res = this.mContext.getContentResolver();
                int[] ids = FingerprintUtils.getFingerprintIdsForUser(res, userId);
                for (int i : ids) {
                    if (i == fingerprintId) {
                        onFingerprintRecognized(userId);
                    }
                }
            } catch (RemoteException e) {
                Log.e("KeyguardUpdateMonitor", "Failed to get current user id: ", e);
            }
        }
    }

    public void handleFingerprintAcquired(int info) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onFingerprintAcquired(info);
            }
        }
    }

    public void handleFaceUnlockStateChanged(boolean running, int userId) {
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

    private boolean isTrustDisabled(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (dpm == null) {
            return false;
        }
        boolean disabledBySimPin = isSimPinSecure();
        boolean disabledByDpm = (dpm.getKeyguardDisabledFeatures(null, userId) & 16) != 0;
        return disabledByDpm || disabledBySimPin;
    }

    private boolean isFingerprintDisabled(int userId) {
        DevicePolicyManager dpm = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        return (dpm == null || (dpm.getKeyguardDisabledFeatures(null, userId) & 32) == 0) ? false : true;
    }

    public boolean getUserHasTrust(int userId) {
        return (!isTrustDisabled(userId) && this.mUserHasTrust.get(userId)) || this.mUserFingerprintRecognized.get(userId);
    }

    public boolean getUserTrustIsManaged(int userId) {
        return this.mUserTrustIsManaged.get(userId) && !isTrustDisabled(userId);
    }

    static class DisplayClientState {
        public boolean clearing;
        public int playbackState;

        DisplayClientState() {
        }
    }

    private static class SimData {
        public IccCardConstants.State simState;
        public int slotId;
        public int subId;

        SimData(IccCardConstants.State state, int slot, int id) {
            this.simState = state;
            this.slotId = slot;
            this.subId = id;
        }

        static SimData fromIntent(Intent intent) {
            IccCardConstants.State state;
            if (!"android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                throw new IllegalArgumentException("only handles intent ACTION_SIM_STATE_CHANGED");
            }
            String stateExtra = intent.getStringExtra("ss");
            int slotId = intent.getIntExtra("slot", 0);
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
                if ("PIN".equals(lockedReason)) {
                    state = IccCardConstants.State.PIN_REQUIRED;
                } else if ("PUK".equals(lockedReason)) {
                    state = IccCardConstants.State.PUK_REQUIRED;
                } else {
                    state = IccCardConstants.State.UNKNOWN;
                }
            } else if ("NETWORK".equals(stateExtra)) {
                state = IccCardConstants.State.NETWORK_LOCKED;
            } else if ("LOADED".equals(stateExtra) || "IMSI".equals(stateExtra)) {
                state = IccCardConstants.State.READY;
            } else {
                state = IccCardConstants.State.UNKNOWN;
            }
            return new SimData(state, slotId, subId);
        }

        public String toString() {
            return "SimData{state=" + this.simState + ",slotId=" + this.slotId + ",subId=" + this.subId + "}";
        }
    }

    public static class BatteryStatus {
        public final int health;
        public final int level;
        public final int plugged;
        public final int status;

        public BatteryStatus(int status, int level, int plugged, int health) {
            this.status = status;
            this.level = level;
            this.plugged = plugged;
            this.health = health;
        }

        public boolean isPluggedIn() {
            return this.plugged == 1 || this.plugged == 2 || this.plugged == 4;
        }

        public boolean isCharged() {
            return this.status == 5 || this.level >= 100;
        }

        public boolean isBatteryLow() {
            return this.level < 20;
        }
    }

    public static KeyguardUpdateMonitor getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyguardUpdateMonitor(context);
        }
        return sInstance;
    }

    protected void handleScreenTurnedOn() {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOn();
            }
        }
    }

    protected void handleScreenTurnedOff(int arg1) {
        clearFingerprintRecognized();
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onScreenTurnedOff(arg1);
            }
        }
    }

    public void dispatchSetBackground(Bitmap bmp) {
        int count = this.mCallbacks.size();
        for (int i = 0; i < count; i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onSetBackground(bmp);
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
        if (!this.mDeviceProvisioned) {
            watchForDeviceProvisioning();
        }
        this.mBatteryStatus = new BatteryStatus(1, 100, 0, 0);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_TICK");
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.SIM_STATE_CHANGED");
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction("android.media.RINGER_MODE_CHANGED");
        filter.addAction("android.intent.action.USER_REMOVED");
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
                    KeyguardUpdateMonitor.this.mSwitchingUser = true;
                }

                public void onUserSwitchComplete(int newUserId) throws RemoteException {
                    KeyguardUpdateMonitor.this.mHandler.sendMessage(KeyguardUpdateMonitor.this.mHandler.obtainMessage(314, newUserId, 0));
                    KeyguardUpdateMonitor.this.mSwitchingUser = false;
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        TrustManager trustManager = (TrustManager) context.getSystemService("trust");
        trustManager.registerTrustListener(this);
        FingerprintManager fpm = (FingerprintManager) context.getSystemService("fingerprint");
        fpm.startListening(this.mFingerprintManagerReceiver);
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
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mDeviceProvisionedObserver);
        boolean provisioned = isDeviceProvisionedInSettingsDb();
        if (provisioned != this.mDeviceProvisioned) {
            this.mDeviceProvisioned = provisioned;
            if (this.mDeviceProvisioned) {
                this.mHandler.sendEmptyMessage(308);
            }
        }
    }

    protected void handleDevicePolicyManagerStateChanged() {
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onDevicePolicyManagerStateChanged();
            }
        }
    }

    protected void handleUserSwitching(int userId, IRemoteCallback reply) {
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
        if (!this.mBootCompleted) {
            this.mBootCompleted = true;
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onBootCompleted();
                }
            }
        }
    }

    public boolean hasBootCompleted() {
        return this.mBootCompleted;
    }

    protected void handleUserRemoved(int userId) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onUserRemoved(userId);
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
        if (this.mDeviceProvisionedObserver != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mDeviceProvisionedObserver);
            this.mDeviceProvisionedObserver = null;
        }
    }

    protected void handlePhoneStateChanged(String newState) {
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(newState)) {
            this.mPhoneState = 0;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(newState)) {
            this.mPhoneState = 2;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(newState)) {
            this.mPhoneState = 1;
        }
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onPhoneStateChanged(this.mPhoneState);
            }
        }
    }

    protected void handleRingerModeChange(int mode) {
        this.mRingMode = mode;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onRingerModeChanged(mode);
            }
        }
    }

    public void handleTimeUpdate() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onTimeChanged();
            }
        }
    }

    public void handleBatteryUpdate(BatteryStatus status) {
        boolean batteryUpdateInteresting = isBatteryUpdateInteresting(this.mBatteryStatus, status);
        this.mBatteryStatus = status;
        if (batteryUpdateInteresting) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onRefreshBatteryInfo(status);
                }
            }
        }
    }

    public void handleSimStateChange(int subId, int slotId, IccCardConstants.State state) {
        boolean changed;
        Log.d("KeyguardUpdateMonitor", "handleSimStateChange(subId=" + subId + ", slotId=" + slotId + ", state=" + state + ")");
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            Log.w("KeyguardUpdateMonitor", "invalid subId in handleSimStateChange()");
            return;
        }
        SimData data = this.mSimDatas.get(Integer.valueOf(subId));
        if (data == null) {
            this.mSimDatas.put(Integer.valueOf(subId), new SimData(state, slotId, subId));
            changed = true;
        } else {
            changed = (data.simState == state && data.subId == subId && data.slotId == slotId) ? false : true;
            data.simState = state;
            data.subId = subId;
            data.slotId = slotId;
        }
        if (changed && state != IccCardConstants.State.UNKNOWN) {
            for (int i = 0; i < this.mCallbacks.size(); i++) {
                KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
                if (cb != null) {
                    cb.onSimStateChanged(subId, slotId, state);
                }
            }
        }
    }

    public void handleClockVisibilityChanged() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onClockVisibilityChanged();
            }
        }
    }

    public void handleKeyguardVisibilityChanged(int showing) {
        boolean isShowing = showing == 1;
        this.mKeyguardIsVisible = isShowing;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardVisibilityChangedRaw(isShowing);
            }
        }
    }

    public void handleKeyguardBouncerChanged(int bouncer) {
        boolean isBouncer = bouncer == 1;
        this.mBouncer = isBouncer;
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onKeyguardBouncerChanged(isBouncer);
            }
        }
    }

    public void handleReportEmergencyCallAction() {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            KeyguardUpdateMonitorCallback cb = this.mCallbacks.get(i).get();
            if (cb != null) {
                cb.onEmergencyCallAction();
            }
        }
    }

    public boolean isKeyguardVisible() {
        return this.mKeyguardIsVisible;
    }

    public boolean isKeyguardBouncer() {
        return this.mBouncer;
    }

    private static boolean isBatteryUpdateInteresting(BatteryStatus old, BatteryStatus current) {
        boolean nowPluggedIn = current.isPluggedIn();
        boolean wasPluggedIn = old.isPluggedIn();
        boolean stateChangedWhilePluggedIn = wasPluggedIn && nowPluggedIn && old.status != current.status;
        boolean stateChangedWhilePluggedOut = (wasPluggedIn || nowPluggedIn || old.status == current.status) ? false : true;
        if (wasPluggedIn != nowPluggedIn || stateChangedWhilePluggedIn || stateChangedWhilePluggedOut) {
            return true;
        }
        if (!nowPluggedIn || old.level == current.level) {
            return (nowPluggedIn || !current.isBatteryLow() || current.level == old.level) ? false : true;
        }
        return true;
    }

    public void removeCallback(KeyguardUpdateMonitorCallback callback) {
        for (int i = this.mCallbacks.size() - 1; i >= 0; i--) {
            if (this.mCallbacks.get(i).get() == callback) {
                this.mCallbacks.remove(i);
            }
        }
    }

    public void registerCallback(KeyguardUpdateMonitorCallback callback) {
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            if (this.mCallbacks.get(i).get() == callback) {
                return;
            }
        }
        this.mCallbacks.add(new WeakReference<>(callback));
        removeCallback(null);
        sendUpdates(callback);
    }

    private void sendUpdates(KeyguardUpdateMonitorCallback callback) {
        callback.onRefreshBatteryInfo(this.mBatteryStatus);
        callback.onTimeChanged();
        callback.onRingerModeChanged(this.mRingMode);
        callback.onPhoneStateChanged(this.mPhoneState);
        callback.onRefreshCarrierInfo();
        callback.onClockVisibilityChanged();
        for (Map.Entry<Integer, SimData> data : this.mSimDatas.entrySet()) {
            SimData state = data.getValue();
            callback.onSimStateChanged(state.subId, state.slotId, state.simState);
        }
    }

    public void sendKeyguardVisibilityChanged(boolean showing) {
        Message message = this.mHandler.obtainMessage(312);
        message.arg1 = showing ? 1 : 0;
        message.sendToTarget();
    }

    public void sendKeyguardBouncerChanged(boolean showingBouncer) {
        Message message = this.mHandler.obtainMessage(322);
        message.arg1 = showingBouncer ? 1 : 0;
        message.sendToTarget();
    }

    public void reportSimUnlocked(int subId) {
        Log.v("KeyguardUpdateMonitor", "reportSimUnlocked(subId=" + subId + ")");
        int slotId = SubscriptionManager.getSlotId(subId);
        handleSimStateChange(subId, slotId, IccCardConstants.State.READY);
    }

    public void reportEmergencyCallAction(boolean bypassHandler) {
        if (!bypassHandler) {
            this.mHandler.obtainMessage(318).sendToTarget();
        } else {
            handleReportEmergencyCallAction();
        }
    }

    public boolean isDeviceProvisioned() {
        return this.mDeviceProvisioned;
    }

    public int getFailedUnlockAttempts() {
        return this.mFailedAttempts;
    }

    public void clearFailedUnlockAttempts() {
        this.mFailedAttempts = 0;
        this.mFailedBiometricUnlockAttempts = 0;
    }

    public void clearFingerprintRecognized() {
        this.mUserFingerprintRecognized.clear();
    }

    public void reportFailedUnlockAttempt() {
        this.mFailedAttempts++;
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

    public boolean isAlternateUnlockEnabled() {
        return this.mAlternateUnlockEnabled;
    }

    public void setAlternateUnlockEnabled(boolean enabled) {
        this.mAlternateUnlockEnabled = enabled;
    }

    public boolean isSimPinVoiceSecure() {
        return isSimPinSecure();
    }

    public boolean isSimPinSecure() {
        for (SubscriptionInfo info : getSubscriptionInfo(false)) {
            if (isSimPinSecure(getSimState(info.getSubscriptionId()))) {
                return true;
            }
        }
        return false;
    }

    public IccCardConstants.State getSimState(int subId) {
        return this.mSimDatas.containsKey(Integer.valueOf(subId)) ? this.mSimDatas.get(Integer.valueOf(subId)).simState : IccCardConstants.State.UNKNOWN;
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
        SimData data = this.mSimDatas.get(Integer.valueOf(subId));
        if (data == null) {
            this.mSimDatas.put(Integer.valueOf(subId), new SimData(state, slotId, subId));
            return true;
        }
        boolean changed = data.simState != state;
        data.simState = state;
        return changed;
    }

    public static boolean isSimPinSecure(IccCardConstants.State state) {
        return state == IccCardConstants.State.PIN_REQUIRED || state == IccCardConstants.State.PUK_REQUIRED || state == IccCardConstants.State.PERM_DISABLED;
    }

    public DisplayClientState getCachedDisplayClientState() {
        return this.mDisplayClientState;
    }

    public void dispatchScreenTurnedOn() {
        synchronized (this) {
            this.mScreenOn = true;
        }
        this.mHandler.sendEmptyMessage(319);
    }

    public void dispatchScreenTurndOff(int why) {
        synchronized (this) {
            this.mScreenOn = false;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(320, why, 0));
    }

    public boolean isScreenOn() {
        return this.mScreenOn;
    }

    public int getNextSubIdForState(IccCardConstants.State state) {
        List<SubscriptionInfo> list = getSubscriptionInfo(false);
        int resultId = -1;
        int bestSlotId = Integer.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            SubscriptionInfo info = list.get(i);
            int id = info.getSubscriptionId();
            int slotId = SubscriptionManager.getSlotId(id);
            if (state == getSimState(id) && bestSlotId > slotId) {
                resultId = id;
                bestSlotId = slotId;
            }
        }
        return resultId;
    }

    public SubscriptionInfo getSubscriptionInfoForSubId(int subId) {
        List<SubscriptionInfo> list = getSubscriptionInfo(false);
        for (int i = 0; i < list.size(); i++) {
            SubscriptionInfo info = list.get(i);
            if (subId == info.getSubscriptionId()) {
                return info;
            }
        }
        return null;
    }
}
