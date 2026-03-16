package com.android.systemui.keyguard;

import android.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardShowCallback;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.MultiUserAvatarCache;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.SystemUI;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import java.util.ArrayList;
import java.util.List;

public class KeyguardViewMediator extends SystemUI {
    private static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(603979776);
    private AlarmManager mAlarmManager;
    private AudioManager mAudioManager;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private int mDelayedShowingSequence;
    private IKeyguardExitCallback mExitSecureCallback;
    private Animation mHideAnimation;
    private boolean mHiding;
    private boolean mInputRestricted;
    private KeyguardDisplayManager mKeyguardDisplayManager;
    private LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    private int mLockSoundStreamId;
    private float mLockSoundVolume;
    private SoundPool mLockSounds;
    private int mMasterStreamType;
    private PowerManager mPM;
    private boolean mScreenOn;
    private SearchManager mSearchManager;
    private PowerManager.WakeLock mShowKeyguardWakeLock;
    private boolean mShowing;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSwitchingUser;
    private boolean mSystemReady;
    private TrustManager mTrustManager;
    private int mTrustedSoundId;
    private int mUnlockSoundId;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private IWindowManager mWM;
    private boolean mSuppressNextLockSound = true;
    private boolean mExternallyEnabled = true;
    private boolean mNeedToReshowWhenReenabled = false;
    private boolean mOccluded = false;
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    private boolean mWaitingUntilKeyguardVisible = false;
    private boolean mKeyguardDonePending = false;
    private boolean mHideAnimationRun = false;
    private final ArrayList<IKeyguardStateCallback> mKeyguardStateCallbacks = new ArrayList<>();
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitching(int userId) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.mSwitchingUser = true;
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                KeyguardViewMediator.this.resetStateLocked();
                KeyguardViewMediator.this.adjustStatusBarLocked();
                KeyguardUpdateMonitor.getInstance(KeyguardViewMediator.this.mContext).setAlternateUnlockEnabled(true);
            }
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            UserInfo info;
            KeyguardViewMediator.this.mSwitchingUser = false;
            if (userId != 0 && (info = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(userId)) != null && info.isGuest()) {
                KeyguardViewMediator.this.dismiss();
            }
        }

        @Override
        public void onUserRemoved(int userId) {
            KeyguardViewMediator.this.mLockPatternUtils.removeUser(userId);
            MultiUserAvatarCache.getInstance().clear(userId);
        }

        @Override
        public void onUserInfoChanged(int userId) {
            MultiUserAvatarCache.getInstance().clear(userId);
        }

        @Override
        public void onPhoneStateChanged(int phoneState) {
            synchronized (KeyguardViewMediator.this) {
                if (phoneState == 0) {
                    if (!KeyguardViewMediator.this.mScreenOn && KeyguardViewMediator.this.mExternallyEnabled) {
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
            }
        }

        @Override
        public void onClockVisibilityChanged() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        @Override
        public void onDeviceProvisioned() {
            KeyguardViewMediator.this.sendUserPresentBroadcast();
            KeyguardViewMediator.this.updateInputRestricted();
        }

        @Override
        public void onSimStateChanged(int subId, int slotId, IccCardConstants.State simState) {
            Log.d("KeyguardViewMediator", "onSimStateChanged(subId=" + subId + ", slotId=" + slotId + ",state=" + simState + ")");
            try {
                int size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                boolean simPinSecure = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                for (int i = 0; i < size; i++) {
                    ((IKeyguardStateCallback) KeyguardViewMediator.this.mKeyguardStateCallbacks.get(i)).onSimSecureStateChanged(simPinSecure);
                }
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
            }
            switch (AnonymousClass6.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[simState.ordinal()]) {
                case 1:
                case 2:
                    synchronized (this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (KeyguardViewMediator.this.mShowing) {
                                KeyguardViewMediator.this.resetStateLocked();
                            } else {
                                Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            }
                        }
                        break;
                    }
                    return;
                case 3:
                case 4:
                    synchronized (this) {
                        if (KeyguardViewMediator.this.mShowing) {
                            KeyguardViewMediator.this.resetStateLocked();
                        } else {
                            Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        }
                        break;
                    }
                    return;
                case 5:
                    synchronized (this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        break;
                    }
                    return;
                case 6:
                    synchronized (this) {
                        if (KeyguardViewMediator.this.mShowing) {
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        break;
                    }
                    return;
                default:
                    Log.v("KeyguardViewMediator", "Ignoring state: " + simState);
                    return;
            }
        }

        @Override
        public void onFingerprintRecognized(int userId) {
            if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isBouncerShowing()) {
                KeyguardViewMediator.this.mViewMediatorCallback.keyguardDone(true);
            }
        }
    };
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        @Override
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        @Override
        public void keyguardDone(boolean authenticated) {
            if (!KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.keyguardDone(authenticated, true);
            }
        }

        @Override
        public void keyguardDoneDrawing() {
            KeyguardViewMediator.this.mHandler.sendEmptyMessage(10);
        }

        @Override
        public void setNeedsInput(boolean needsInput) {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.setNeedsInput(needsInput);
        }

        @Override
        public void onUserActivityTimeoutChanged() {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.updateUserActivityTimeout();
        }

        @Override
        public void keyguardDonePending() {
            KeyguardViewMediator.this.mKeyguardDonePending = true;
            KeyguardViewMediator.this.mHideAnimationRun = true;
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(null);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(20, 3000L);
        }

        @Override
        public void keyguardGone() {
            KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
        }

        @Override
        public void readyForKeyguardDone() {
            if (KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.keyguardDone(true, true);
            }
        }

        @Override
        public void playTrustedSound() {
            KeyguardViewMediator.this.playTrustedSound();
        }

        @Override
        public boolean isInputRestricted() {
            return KeyguardViewMediator.this.isInputRestricted();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(intent.getAction())) {
                int sequence = intent.getIntExtra("seq", 0);
                synchronized (KeyguardViewMediator.this) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == sequence) {
                        KeyguardViewMediator.this.mSuppressNextLockSound = true;
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 2:
                    KeyguardViewMediator.this.handleShow((Bundle) msg.obj);
                    return;
                case 3:
                    KeyguardViewMediator.this.handleHide();
                    return;
                case 4:
                    KeyguardViewMediator.this.handleReset();
                    return;
                case 5:
                    KeyguardViewMediator.this.handleVerifyUnlock();
                    return;
                case 6:
                    KeyguardViewMediator.this.handleNotifyScreenOff();
                    return;
                case 7:
                    KeyguardViewMediator.this.handleNotifyScreenOn((IKeyguardShowCallback) msg.obj);
                    return;
                case 8:
                case 14:
                case 15:
                case 16:
                default:
                    return;
                case 9:
                    KeyguardViewMediator.this.handleKeyguardDone(msg.arg1 != 0, msg.arg2 != 0);
                    return;
                case 10:
                    KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                    return;
                case 11:
                    KeyguardViewMediator.this.keyguardDone(true, true);
                    return;
                case 12:
                    KeyguardViewMediator.this.handleSetOccluded(msg.arg1 != 0);
                    return;
                case 13:
                    synchronized (KeyguardViewMediator.this) {
                        KeyguardViewMediator.this.doKeyguardLocked((Bundle) msg.obj);
                        break;
                    }
                    return;
                case 17:
                    KeyguardViewMediator.this.handleDismiss();
                    return;
                case 18:
                    StartKeyguardExitAnimParams params = (StartKeyguardExitAnimParams) msg.obj;
                    KeyguardViewMediator.this.handleStartKeyguardExitAnimation(params.startTime, params.fadeoutDuration);
                    return;
                case 19:
                    break;
                case 20:
                    Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                    break;
            }
            KeyguardViewMediator.this.handleOnActivityDrawn();
        }
    };
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                KeyguardViewMediator.this.mWM.keyguardGoingAway(KeyguardViewMediator.this.mStatusBarKeyguardViewManager.shouldDisableWindowAnimationsForUnlock(), KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isGoingToNotificationShade());
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
        }
    };

    static class AnonymousClass6 {
        static final int[] $SwitchMap$com$android$internal$telephony$IccCardConstants$State = new int[IccCardConstants.State.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NOT_READY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.ABSENT.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PERM_DISABLED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
        }
    }

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    private void setupLocked() {
        this.mPM = (PowerManager) this.mContext.getSystemService("power");
        this.mWM = WindowManagerGlobal.getWindowManagerService();
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
        this.mShowKeyguardWakeLock = this.mPM.newWakeLock(1, "show keyguard");
        this.mShowKeyguardWakeLock.setReferenceCounted(false);
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD"));
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        this.mLockPatternUtils.setCurrentUser(ActivityManager.getCurrentUser());
        setShowingLocked((shouldWaitForProvisioning() || this.mLockPatternUtils.isLockScreenDisabled()) ? false : true);
        this.mTrustManager.reportKeyguardShowingChanged();
        this.mStatusBarKeyguardViewManager = new StatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver cr = this.mContext.getContentResolver();
        this.mScreenOn = this.mPM.isScreenOn();
        this.mLockSounds = new SoundPool(1, 1, 0);
        String soundPath = Settings.Global.getString(cr, "lock_sound");
        if (soundPath != null) {
            this.mLockSoundId = this.mLockSounds.load(soundPath, 1);
        }
        if (soundPath == null || this.mLockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load lock sound from " + soundPath);
        }
        String soundPath2 = Settings.Global.getString(cr, "unlock_sound");
        if (soundPath2 != null) {
            this.mUnlockSoundId = this.mLockSounds.load(soundPath2, 1);
        }
        if (soundPath2 == null || this.mUnlockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load unlock sound from " + soundPath2);
        }
        String soundPath3 = Settings.Global.getString(cr, "trusted_sound");
        if (soundPath3 != null) {
            this.mTrustedSoundId = this.mLockSounds.load(soundPath3, 1);
        }
        if (soundPath3 == null || this.mTrustedSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load trusted sound from " + soundPath3);
        }
        int lockSoundDefaultAttenuation = this.mContext.getResources().getInteger(R.integer.auto_data_switch_availability_switchback_stability_time_threshold_millis);
        this.mLockSoundVolume = (float) Math.pow(10.0d, lockSoundDefaultAttenuation / 20.0f);
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.flat_button_state_list_anim_material);
    }

    @Override
    public void start() {
        synchronized (this) {
            setupLocked();
        }
        putComponent(KeyguardViewMediator.class, this);
    }

    public void onSystemReady() {
        this.mSearchManager = (SearchManager) this.mContext.getSystemService("search");
        synchronized (this) {
            this.mSystemReady = true;
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
            if (this.mLockPatternUtils.usingBiometricWeak() && this.mLockPatternUtils.isBiometricWeakInstalled()) {
                this.mUpdateMonitor.setAlternateUnlockEnabled(false);
            } else {
                this.mUpdateMonitor.setAlternateUnlockEnabled(true);
            }
            doKeyguardLocked(null);
        }
        maybeSendUserPresentBroadcast();
    }

    public void onScreenTurnedOff(int why) {
        synchronized (this) {
            this.mScreenOn = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            boolean lockImmediately = this.mLockPatternUtils.getPowerButtonInstantlyLocks() || !this.mLockPatternUtils.isSecure();
            notifyScreenOffLocked();
            if (this.mExitSecureCallback != null) {
                try {
                    this.mExitSecureCallback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
                this.mExitSecureCallback = null;
                if (!this.mExternallyEnabled) {
                    hideLocked();
                }
            } else if (this.mShowing) {
                resetStateLocked();
            } else if (why == 3) {
                doKeyguardLaterLocked(false);
            } else if (why == 2 && !lockImmediately) {
                doKeyguardLaterLocked(true);
            } else {
                doKeyguardLocked(null);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchScreenTurndOff(why);
    }

    private void doKeyguardLaterLocked(boolean from_user) {
        long timeout;
        ContentResolver cr = this.mContext.getContentResolver();
        long displayTimeout = Settings.System.getInt(cr, "screen_off_timeout", 30000);
        long lockAfterTimeout = Settings.Secure.getInt(cr, "lock_screen_lock_after_timeout", 5000);
        long policyTimeout = this.mLockPatternUtils.getDevicePolicyManager().getMaximumTimeToLock(null, this.mLockPatternUtils.getCurrentUser());
        if (policyTimeout > 0) {
            if (!from_user) {
                timeout = Math.min(policyTimeout - Math.max(displayTimeout, 0L), lockAfterTimeout);
            } else {
                timeout = Math.min(policyTimeout, lockAfterTimeout);
            }
        } else {
            timeout = lockAfterTimeout;
        }
        if (timeout <= 0) {
            this.mSuppressNextLockSound = true;
            doKeyguardLocked(null);
            return;
        }
        long when = SystemClock.elapsedRealtime() + timeout;
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        PendingIntent sender = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        this.mAlarmManager.setWindow(2, when, 1000L, sender);
    }

    private void cancelDoKeyguardLaterLocked() {
        this.mDelayedShowingSequence++;
    }

    public void onScreenTurnedOn(IKeyguardShowCallback callback) {
        synchronized (this) {
            this.mScreenOn = true;
            cancelDoKeyguardLaterLocked();
            if (callback != null) {
                notifyScreenOnLocked(callback);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchScreenTurnedOn();
        maybeSendUserPresentBroadcast();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled()) {
            sendUserPresentBroadcast();
        }
    }

    public void onDreamingStarted() {
        synchronized (this) {
            if (this.mScreenOn && this.mLockPatternUtils.isSecure()) {
                doKeyguardLaterLocked(false);
            }
        }
    }

    public void onDreamingStopped() {
        synchronized (this) {
            if (this.mScreenOn) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    public void setKeyguardEnabled(boolean enabled) {
        synchronized (this) {
            this.mExternallyEnabled = enabled;
            if (!enabled && this.mShowing) {
                if (this.mExitSecureCallback == null) {
                    this.mNeedToReshowWhenReenabled = true;
                    updateInputRestrictedLocked();
                    hideLocked();
                }
            } else if (enabled && this.mNeedToReshowWhenReenabled) {
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestrictedLocked();
                if (this.mExitSecureCallback != null) {
                    try {
                        this.mExitSecureCallback.onKeyguardExitResult(false);
                    } catch (RemoteException e) {
                        Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                    }
                    this.mExitSecureCallback = null;
                    resetStateLocked();
                } else {
                    showLocked(null);
                    this.mWaitingUntilKeyguardVisible = true;
                    this.mHandler.sendEmptyMessageDelayed(10, 2000L);
                    while (this.mWaitingUntilKeyguardVisible) {
                        try {
                            wait();
                        } catch (InterruptedException e2) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        synchronized (this) {
            if (shouldWaitForProvisioning()) {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
            } else if (this.mExternallyEnabled) {
                Log.w("KeyguardViewMediator", "verifyUnlock called when not externally disabled");
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e2) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e2);
                }
            } else if (this.mExitSecureCallback != null) {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e3) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e3);
                }
            } else {
                this.mExitSecureCallback = callback;
                verifyUnlockLocked();
            }
        }
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public void setOccluded(boolean isOccluded) {
        this.mHandler.removeMessages(12);
        Message msg = this.mHandler.obtainMessage(12, isOccluded ? 1 : 0, 0);
        this.mHandler.sendMessage(msg);
    }

    private void handleSetOccluded(boolean isOccluded) {
        synchronized (this) {
            if (this.mOccluded != isOccluded) {
                this.mOccluded = isOccluded;
                this.mStatusBarKeyguardViewManager.setOccluded(isOccluded);
                updateActivityLockScreenState();
                adjustStatusBarLocked();
            }
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        this.mHandler.removeMessages(13);
        Message msg = this.mHandler.obtainMessage(13, options);
        this.mHandler.sendMessage(msg);
    }

    public boolean isInputRestricted() {
        return this.mShowing || this.mNeedToReshowWhenReenabled || shouldWaitForProvisioning();
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
        }
    }

    private void updateInputRestrictedLocked() {
        boolean inputRestricted = isInputRestricted();
        if (this.mInputRestricted != inputRestricted) {
            this.mInputRestricted = inputRestricted;
            try {
                int size = this.mKeyguardStateCallbacks.size();
                for (int i = 0; i < size; i++) {
                    this.mKeyguardStateCallbacks.get(i).onInputRestrictedStateChanged(inputRestricted);
                }
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
            }
        }
    }

    private void doKeyguardLocked(Bundle options) {
        if (this.mExternallyEnabled) {
            if (this.mStatusBarKeyguardViewManager.isShowing()) {
                resetStateLocked();
                return;
            }
            boolean requireSim = !SystemProperties.getBoolean("keyguard.no_require_sim", false);
            boolean absent = SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.ABSENT));
            boolean disabled = SubscriptionManager.isValidSubscriptionId(this.mUpdateMonitor.getNextSubIdForState(IccCardConstants.State.PERM_DISABLED));
            boolean lockedOrMissing = this.mUpdateMonitor.isSimPinSecure() || ((absent || disabled) && requireSim);
            if (lockedOrMissing || !shouldWaitForProvisioning()) {
                if (!this.mLockPatternUtils.isLockScreenDisabled() || lockedOrMissing) {
                    if (this.mLockPatternUtils.checkVoldPassword()) {
                        setShowingLocked(false);
                        hideLocked();
                    } else {
                        showLocked(options);
                    }
                }
            }
        }
    }

    private boolean shouldWaitForProvisioning() {
        return (this.mUpdateMonitor.isDeviceProvisioned() || isSecure()) ? false : true;
    }

    public void handleDismiss() {
        if (this.mShowing && !this.mOccluded) {
            this.mStatusBarKeyguardViewManager.dismiss();
        }
    }

    public void dismiss() {
        this.mHandler.sendEmptyMessage(17);
    }

    private void resetStateLocked() {
        Message msg = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessage(msg);
    }

    private void verifyUnlockLocked() {
        this.mHandler.sendEmptyMessage(5);
    }

    private void notifyScreenOffLocked() {
        this.mHandler.sendEmptyMessage(6);
    }

    private void notifyScreenOnLocked(IKeyguardShowCallback result) {
        Message msg = this.mHandler.obtainMessage(7, result);
        this.mHandler.sendMessage(msg);
    }

    private void showLocked(Bundle options) {
        this.mShowKeyguardWakeLock.acquire();
        Message msg = this.mHandler.obtainMessage(2, options);
        this.mHandler.sendMessage(msg);
    }

    private void hideLocked() {
        Message msg = this.mHandler.obtainMessage(3);
        this.mHandler.sendMessage(msg);
    }

    public boolean isSecure() {
        return this.mLockPatternUtils.isSecure() || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure();
    }

    public void setCurrentUser(int newUserId) {
        this.mLockPatternUtils.setCurrentUser(newUserId);
    }

    public void keyguardDone(boolean authenticated, boolean wakeup) {
        EventLog.writeEvent(70000, 2);
        Message msg = this.mHandler.obtainMessage(9, authenticated ? 1 : 0, wakeup ? 1 : 0);
        this.mHandler.sendMessage(msg);
    }

    private void handleKeyguardDone(boolean authenticated, boolean wakeup) {
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        if (authenticated) {
            this.mUpdateMonitor.clearFailedUnlockAttempts();
        }
        this.mUpdateMonitor.clearFingerprintRecognized();
        if (this.mExitSecureCallback != null) {
            try {
                this.mExitSecureCallback.onKeyguardExitResult(authenticated);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(" + authenticated + ")", e);
            }
            this.mExitSecureCallback = null;
            if (authenticated) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
            }
        }
        handleHide();
    }

    private void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                UserHandle currentUser = new UserHandle(this.mLockPatternUtils.getCurrentUser());
                UserManager um = (UserManager) this.mContext.getSystemService("user");
                List<UserInfo> userHandles = um.getProfiles(currentUser.getIdentifier());
                for (UserInfo ui : userHandles) {
                    this.mContext.sendBroadcastAsUser(USER_PRESENT_INTENT, ui.getUserHandle());
                }
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    private void handleKeyguardDoneDrawing() {
        synchronized (this) {
            if (this.mWaitingUntilKeyguardVisible) {
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(10);
            }
        }
    }

    private void playSounds(boolean locked) {
        if (this.mSuppressNextLockSound) {
            this.mSuppressNextLockSound = false;
        } else {
            playSound(locked ? this.mLockSoundId : this.mUnlockSoundId);
        }
    }

    private void playSound(int soundId) {
        if (soundId != 0) {
            ContentResolver cr = this.mContext.getContentResolver();
            if (Settings.System.getInt(cr, "lockscreen_sounds_enabled", 1) == 1) {
                this.mLockSounds.stop(this.mLockSoundStreamId);
                if (this.mAudioManager == null) {
                    this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                    if (this.mAudioManager != null) {
                        this.mMasterStreamType = this.mAudioManager.getMasterStreamType();
                    } else {
                        return;
                    }
                }
                if (!this.mAudioManager.isStreamMute(this.mMasterStreamType)) {
                    this.mLockSoundStreamId = this.mLockSounds.play(soundId, this.mLockSoundVolume, this.mLockSoundVolume, 1, 0, 1.0f);
                }
            }
        }
    }

    private void playTrustedSound() {
        if (!this.mSuppressNextLockSound) {
            playSound(this.mTrustedSoundId);
        }
    }

    private void updateActivityLockScreenState() {
        try {
            ActivityManagerNative.getDefault().setLockScreenShown(this.mShowing && !this.mOccluded);
        } catch (RemoteException e) {
        }
    }

    private void handleShow(Bundle options) {
        synchronized (this) {
            if (this.mSystemReady) {
                setShowingLocked(true);
                this.mStatusBarKeyguardViewManager.show(options);
                this.mHiding = false;
                resetKeyguardDonePendingLocked();
                this.mHideAnimationRun = false;
                updateActivityLockScreenState();
                adjustStatusBarLocked();
                userActivity();
                playSounds(true);
                this.mShowKeyguardWakeLock.release();
                this.mKeyguardDisplayManager.show();
            }
        }
    }

    private void handleHide() {
        synchronized (this) {
            this.mHiding = true;
            if (this.mShowing && !this.mOccluded) {
                if (!this.mHideAnimationRun) {
                    this.mStatusBarKeyguardViewManager.startPreHideAnimation(this.mKeyguardGoingAwayRunnable);
                } else {
                    this.mKeyguardGoingAwayRunnable.run();
                }
            } else {
                handleStartKeyguardExitAnimation(SystemClock.uptimeMillis() + this.mHideAnimation.getStartOffset(), this.mHideAnimation.getDuration());
            }
        }
    }

    private void handleOnActivityDrawn() {
        if (this.mKeyguardDonePending) {
            this.mStatusBarKeyguardViewManager.onActivityDrawn();
        }
    }

    private void handleStartKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        synchronized (this) {
            if (this.mHiding) {
                this.mHiding = false;
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(this.mPhoneState)) {
                    playSounds(false);
                }
                setShowingLocked(false);
                this.mStatusBarKeyguardViewManager.hide(startTime, fadeoutDuration);
                resetKeyguardDonePendingLocked();
                this.mHideAnimationRun = false;
                updateActivityLockScreenState();
                adjustStatusBarLocked();
                sendUserPresentBroadcast();
            }
        }
    }

    private void adjustStatusBarLocked() {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        if (this.mStatusBarManager == null) {
            Log.w("KeyguardViewMediator", "Could not get status bar manager");
            return;
        }
        int flags = 0;
        if (this.mShowing) {
            int flags2 = 0 | 16777216;
            flags = flags2 | 33554432;
        }
        if (isShowingAndNotOccluded()) {
            flags |= 2097152;
        }
        if (!(this.mContext instanceof Activity)) {
            this.mStatusBarManager.disable(flags);
        }
    }

    private void handleReset() {
        synchronized (this) {
            this.mStatusBarKeyguardViewManager.reset();
        }
    }

    private void handleVerifyUnlock() {
        synchronized (this) {
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.verifyUnlock();
            updateActivityLockScreenState();
        }
    }

    private void handleNotifyScreenOff() {
        synchronized (this) {
            this.mStatusBarKeyguardViewManager.onScreenTurnedOff();
        }
    }

    private void handleNotifyScreenOn(IKeyguardShowCallback callback) {
        synchronized (this) {
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn(callback);
        }
    }

    private void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(20);
    }

    @Override
    public void onBootCompleted() {
        this.mUpdateMonitor.dispatchBootCompleted();
        synchronized (this) {
            this.mBootCompleted = true;
            if (this.mBootSendUserPresent) {
                sendUserPresentBroadcast();
            }
        }
    }

    public StatusBarKeyguardViewManager registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController) {
        this.mStatusBarKeyguardViewManager.registerStatusBar(phoneStatusBar, container, statusBarWindowManager, scrimController);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        Message msg = this.mHandler.obtainMessage(18, new StartKeyguardExitAnimParams(startTime, fadeoutDuration));
        this.mHandler.sendMessage(msg);
    }

    public void onActivityDrawn() {
        this.mHandler.sendEmptyMessage(19);
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        private StartKeyguardExitAnimParams(long startTime, long fadeoutDuration) {
            this.startTime = startTime;
            this.fadeoutDuration = fadeoutDuration;
        }
    }

    private void setShowingLocked(boolean showing) {
        if (showing != this.mShowing) {
            this.mShowing = showing;
            try {
                int size = this.mKeyguardStateCallbacks.size();
                for (int i = 0; i < size; i++) {
                    this.mKeyguardStateCallbacks.get(i).onShowingStateChanged(showing);
                }
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
            }
            updateInputRestrictedLocked();
            this.mTrustManager.reportKeyguardShowingChanged();
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback callback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(callback);
            try {
                callback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                callback.onShowingStateChanged(this.mShowing);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged or onSimSecureStateChanged", e);
            }
        }
    }
}
