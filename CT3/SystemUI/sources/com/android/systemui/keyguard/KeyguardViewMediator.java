package com.android.systemui.keyguard;

import android.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.ViewGroup;
import android.view.WindowManagerGlobal;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardSecurityModel;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.phone.FingerprintUnlockController;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.ScrimController;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.Plugin.KeyguardPluginFactory;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;
import com.mediatek.keyguard.Telephony.KeyguardDialogManager;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class KeyguardViewMediator extends SystemUI {
    private static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(603979776);
    private static boolean mKeyguardDoneOnGoing = false;
    private static final boolean sIsUserBuild = SystemProperties.get("ro.build.type").equals("user");
    private AlarmManager mAlarmManager;
    private AntiTheftManager mAntiTheftManager;
    private AudioManager mAudioManager;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private int mDelayedProfileShowingSequence;
    private int mDelayedShowingSequence;
    private boolean mDeviceInteractive;
    private KeyguardDialogManager mDialogManager;
    private IKeyguardDrawnCallback mDrawnCallback;
    private IKeyguardExitCallback mExitSecureCallback;
    private boolean mGoingToSleep;
    private Animation mHideAnimation;
    private boolean mHiding;
    private boolean mInputRestricted;
    private boolean mIsPerUserLock;
    private KeyguardDisplayManager mKeyguardDisplayManager;
    private boolean mLockLater;
    private LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    private int mLockSoundStreamId;
    private float mLockSoundVolume;
    private SoundPool mLockSounds;
    private PowerManager mPM;
    private boolean mPendingLock;
    private boolean mPendingReset;
    private PowerOffAlarmManager mPowerOffAlarmManager;
    private SearchManager mSearchManager;
    private KeyguardSecurityModel mSecurityModel;
    private PowerManager.WakeLock mShowKeyguardWakeLock;
    private boolean mShowing;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSwitchingUser;
    private boolean mSystemReady;
    private TrustManager mTrustManager;
    private int mTrustedSoundId;
    private int mUiSoundsStreamType;
    private int mUnlockSoundId;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private VoiceWakeupManager mVoiceWakeupManager;
    private IWindowManager mWM;
    private boolean mWakeAndUnlocking;
    private boolean mSuppressNextLockSound = true;
    private boolean mExternallyEnabled = true;
    private boolean mNeedToReshowWhenReenabled = false;
    private boolean mReadyToShow = false;
    private boolean mOccluded = false;
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    private boolean mWaitingUntilKeyguardVisible = false;
    private boolean mKeyguardDonePending = false;
    private boolean mHideAnimationRun = false;
    private final ArrayList<IKeyguardStateCallback> mKeyguardStateCallbacks = new ArrayList<>();
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {

        private static final int[] f8x8dbfd0b5 = null;

        private static int[] m799xf663cf59() {
            if (f8x8dbfd0b5 != null) {
                return f8x8dbfd0b5;
            }
            int[] iArr = new int[IccCardConstants.State.values().length];
            try {
                iArr[IccCardConstants.State.ABSENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[IccCardConstants.State.CARD_IO_ERROR.ordinal()] = 8;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 2;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[IccCardConstants.State.NOT_READY.ordinal()] = 3;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[IccCardConstants.State.PERM_DISABLED.ordinal()] = 4;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[IccCardConstants.State.PIN_REQUIRED.ordinal()] = 5;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[IccCardConstants.State.PUK_REQUIRED.ordinal()] = 6;
            } catch (NoSuchFieldError e7) {
            }
            try {
                iArr[IccCardConstants.State.READY.ordinal()] = 7;
            } catch (NoSuchFieldError e8) {
            }
            try {
                iArr[IccCardConstants.State.UNKNOWN.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            f8x8dbfd0b5 = iArr;
            return iArr;
        }

        @Override
        public void onUserSwitching(int userId) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.mSwitchingUser = true;
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                KeyguardViewMediator.this.resetStateLocked();
                KeyguardViewMediator.this.adjustStatusBarLocked();
            }
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            UserInfo info;
            KeyguardViewMediator.this.mSwitchingUser = false;
            if (userId == 0 || (info = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(userId)) == null || !info.isGuest()) {
                return;
            }
            KeyguardViewMediator.this.dismiss();
        }

        @Override
        public void onUserInfoChanged(int userId) {
        }

        @Override
        public void onPhoneStateChanged(int phoneState) {
            synchronized (KeyguardViewMediator.this) {
                if (phoneState == 0) {
                    if (!KeyguardViewMediator.this.mDeviceInteractive && KeyguardViewMediator.this.mExternallyEnabled) {
                        Log.d("KeyguardViewMediator", "screen is off and call ended, let's make sure the keyguard is showing");
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
            synchronized (KeyguardViewMediator.this) {
                if (UserManager.isSplitSystemUser() && KeyguardUpdateMonitor.getCurrentUser() == 0) {
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        @Override
        public void onSimStateChangedUsingPhoneId(int phoneId, IccCardConstants.State simState) {
            int size;
            int i;
            Log.d("KeyguardViewMediator", "onSimStateChangedUsingSubId: " + simState + ", phoneId=" + phoneId);
            switch (m799xf663cf59()[simState.ordinal()]) {
                case 1:
                case 3:
                    synchronized (this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else {
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                    }
                    try {
                        size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                        boolean simPinSecure = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                        for (i = 0; i < size; i++) {
                            ((IKeyguardStateCallback) KeyguardViewMediator.this.mKeyguardStateCallbacks.get(i)).onSimSecureStateChanged(simPinSecure);
                        }
                        return;
                    } catch (RemoteException e) {
                        Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
                        return;
                    }
                case 2:
                case 5:
                case 6:
                    synchronized (this) {
                        if (simState == IccCardConstants.State.NETWORK_LOCKED && !KeyguardUtils.isMediatekSimMeLockSupport()) {
                            Log.d("KeyguardViewMediator", "Get NETWORK_LOCKED but not support ME lock. Not show.");
                        } else if (KeyguardUtils.isSystemEncrypted()) {
                            Log.d("KeyguardViewMediator", "Currently system needs to be decrypted. Not show.");
                        } else {
                            KeyguardViewMediator.this.mUpdateMonitor.setDismissFlagWhenWfcOn(simState);
                            if (KeyguardViewMediator.this.mUpdateMonitor.getRetryPukCountOfPhoneId(phoneId) == 0) {
                                KeyguardViewMediator.this.mDialogManager.requestShowDialog(new InvalidDialogCallback(KeyguardViewMediator.this, null));
                            } else if (IccCardConstants.State.NETWORK_LOCKED == simState && KeyguardViewMediator.this.mUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(phoneId) == 0) {
                                Log.d("KeyguardViewMediator", "SIM ME lock retrycount is 0, only to show dialog");
                                KeyguardViewMediator.this.mDialogManager.requestShowDialog(new MeLockedDialogCallback(KeyguardViewMediator.this, null));
                            } else if (!KeyguardViewMediator.this.isShowing()) {
                                Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else if (KeyguardViewMediator.mKeyguardDoneOnGoing) {
                                Log.d("KeyguardViewMediator", "mKeyguardDoneOnGoing is true");
                                KeyguardViewMediator.this.doKeyguardLaterLocked();
                            } else {
                                KeyguardViewMediator.this.removeKeyguardDoneMsg();
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                        size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                        boolean simPinSecure2 = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                        while (i < size) {
                        }
                        return;
                    }
                case 4:
                    synchronized (this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                        boolean simPinSecure22 = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                        while (i < size) {
                        }
                        return;
                    }
                default:
                    Log.v("KeyguardViewMediator", "Ignoring state: " + simState);
                case 7:
                    size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                    boolean simPinSecure222 = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                    while (i < size) {
                    }
                    return;
            }
        }

        @Override
        public void onFingerprintAuthFailed() {
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (!KeyguardViewMediator.this.mLockPatternUtils.isSecure(currentUser)) {
                return;
            }
            KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportFailedFingerprintAttempt(currentUser);
        }

        @Override
        public void onFingerprintAuthenticated(int userId) {
            if (!KeyguardViewMediator.this.mLockPatternUtils.isSecure(userId)) {
                return;
            }
            KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportSuccessfulFingerprintAttempt(userId);
        }
    };
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        @Override
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        @Override
        public void keyguardDone(boolean strongAuth) {
            if (!KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.keyguardDone(true);
            }
            if (!strongAuth) {
                return;
            }
            KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
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
        public void keyguardDonePending(boolean strongAuth) {
            KeyguardViewMediator.this.mKeyguardDonePending = true;
            KeyguardViewMediator.this.mHideAnimationRun = true;
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(null);
            KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(20, 3000L);
            if (!strongAuth) {
                return;
            }
            KeyguardViewMediator.this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
        }

        @Override
        public void keyguardGone() {
            if (KeyguardViewMediator.this.mKeyguardDisplayManager != null) {
                Log.d("KeyguardViewMediator", "keyguard gone, call mKeyguardDisplayManager.hide()");
                KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
            } else {
                Log.d("KeyguardViewMediator", "keyguard gone, mKeyguardDisplayManager is null");
            }
            KeyguardViewMediator.this.mVoiceWakeupManager.notifyKeyguardIsGone();
        }

        @Override
        public void readyForKeyguardDone() {
            if (!KeyguardViewMediator.this.mKeyguardDonePending) {
                return;
            }
            KeyguardViewMediator.this.keyguardDone(true);
        }

        @Override
        public void resetKeyguard() {
            resetStateLocked();
        }

        @Override
        public void playTrustedSound() {
            KeyguardViewMediator.this.playTrustedSound();
        }

        @Override
        public boolean isInputRestricted() {
            return KeyguardViewMediator.this.isInputRestricted();
        }

        @Override
        public boolean isScreenOn() {
            return KeyguardViewMediator.this.mDeviceInteractive;
        }

        @Override
        public int getBouncerPromptReason() {
            int currentUser = ActivityManager.getCurrentUser();
            boolean trust = KeyguardViewMediator.this.mTrustManager.isTrustUsuallyManaged(currentUser);
            boolean fingerprint = KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(currentUser);
            boolean z = !trust ? fingerprint : true;
            KeyguardUpdateMonitor.StrongAuthTracker strongAuthTracker = KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker();
            int strongAuth = strongAuthTracker.getStrongAuthForUser(currentUser);
            if (z && !strongAuthTracker.hasUserAuthenticatedSinceBoot()) {
                return 1;
            }
            if (fingerprint && KeyguardViewMediator.this.mUpdateMonitor.hasFingerprintUnlockTimedOut(currentUser)) {
                return 2;
            }
            if (z && (strongAuth & 2) != 0) {
                return 3;
            }
            if (trust && (strongAuth & 4) != 0) {
                return 4;
            }
            if (!z || (strongAuth & 8) == 0) {
                return (!trust || (strongAuth & 16) == 0) ? 0 : 6;
            }
            return 5;
        }

        @Override
        public void dismiss(boolean authenticated) {
            KeyguardViewMediator.this.dismiss(authenticated);
        }

        @Override
        public boolean isShowing() {
            return KeyguardViewMediator.this.isShowing();
        }

        @Override
        public void showLocked(Bundle options) {
            KeyguardViewMediator.this.showLocked(options);
        }

        @Override
        public void resetStateLocked() {
            KeyguardViewMediator.this.resetStateLocked();
        }

        @Override
        public void adjustStatusBarLocked() {
            KeyguardViewMediator.this.adjustStatusBarLocked();
        }

        @Override
        public void hideLocked() {
            KeyguardViewMediator.this.hideLocked();
        }

        @Override
        public boolean isSecure() {
            return KeyguardViewMediator.this.isSecure();
        }

        @Override
        public void setSuppressPlaySoundFlag() {
            KeyguardViewMediator.this.setSuppressPlaySoundFlag();
        }

        @Override
        public void updateNavbarStatus() {
            KeyguardViewMediator.this.updateNavbarStatus();
        }

        @Override
        public boolean isKeyguardDoneOnGoing() {
            return KeyguardViewMediator.this.isKeyguardDoneOnGoing();
        }

        @Override
        public void updateAntiTheftLocked() {
            KeyguardViewMediator.this.updateAntiTheftLocked();
        }

        @Override
        public boolean isKeyguardExternallyEnabled() {
            return KeyguardViewMediator.this.isKeyguardExternallyEnabled();
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyguardViewMediator keyguardViewMediator;
            String action = intent.getAction();
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(action)) {
                int sequence = intent.getIntExtra("seq", 0);
                Log.d("KeyguardViewMediator", "received DELAYED_KEYGUARD_ACTION with seq = " + sequence + ", mDelayedShowingSequence = " + KeyguardViewMediator.this.mDelayedShowingSequence);
                keyguardViewMediator = KeyguardViewMediator.this;
                synchronized (keyguardViewMediator) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == sequence) {
                        KeyguardViewMediator.this.mSuppressNextLockSound = true;
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
            } else if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK".equals(intent.getAction())) {
                int sequence2 = intent.getIntExtra("seq", 0);
                int userId = intent.getIntExtra("android.intent.extra.USER_ID", 0);
                if (userId == 0) {
                    return;
                }
                keyguardViewMediator = KeyguardViewMediator.this;
                synchronized (keyguardViewMediator) {
                    if (KeyguardViewMediator.this.mDelayedProfileShowingSequence == sequence2) {
                        KeyguardViewMediator.this.lockProfile(userId);
                    }
                }
            } else {
                if ("android.intent.action.ACTION_PRE_SHUTDOWN".equals(action)) {
                    Log.w("KeyguardViewMediator", "PRE_SHUTDOWN: " + action);
                    KeyguardViewMediator.this.mSuppressNextLockSound = true;
                    return;
                }
                if ("android.intent.action.ACTION_SHUTDOWN_IPO".equals(action)) {
                    Log.w("KeyguardViewMediator", "IPO_SHUTDOWN: " + action);
                    KeyguardViewMediator.this.mIsIPOShutDown = true;
                    KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(1002, 4000L);
                    return;
                } else {
                    if (!"android.intent.action.ACTION_PREBOOT_IPO".equals(action)) {
                        return;
                    }
                    Log.w("KeyguardViewMediator", "IPO_BOOTUP: " + action);
                    KeyguardViewMediator.this.mIsIPOShutDown = false;
                    for (int i = 0; i < KeyguardUtils.getNumOfPhone(); i++) {
                        KeyguardViewMediator.this.mUpdateMonitor.setPinPukMeDismissFlagOfPhoneId(i, false);
                        Log.d("KeyguardViewMediator", "setPinPukMeDismissFlagOfPhoneId false: " + i);
                    }
                    return;
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
                    KeyguardViewMediator.this.handleNotifyFinishedGoingToSleep();
                    return;
                case 7:
                    KeyguardViewMediator.this.handleNotifyScreenTurningOn((IKeyguardDrawnCallback) msg.obj);
                    return;
                case 8:
                case 11:
                case 14:
                case 15:
                case 16:
                default:
                    return;
                case 9:
                    KeyguardViewMediator.this.handleKeyguardDone(msg.arg1 != 0);
                    return;
                case 10:
                    KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                    return;
                case 12:
                    KeyguardViewMediator.this.handleSetOccluded(msg.arg1 != 0);
                    return;
                case 13:
                    synchronized (KeyguardViewMediator.this) {
                        Log.d("KeyguardViewMediator", "doKeyguardLocked, because:KEYGUARD_TIMEOUT");
                        KeyguardViewMediator.this.doKeyguardLocked((Bundle) msg.obj);
                    }
                    return;
                case 17:
                    KeyguardViewMediator.this.handleDismiss(((Boolean) msg.obj).booleanValue());
                    return;
                case 18:
                    StartKeyguardExitAnimParams params = (StartKeyguardExitAnimParams) msg.obj;
                    KeyguardViewMediator.this.handleStartKeyguardExitAnimation(params.startTime, params.fadeoutDuration);
                    FalsingManager.getInstance(KeyguardViewMediator.this.mContext).onSucccessfulUnlock();
                    return;
                case 19:
                    break;
                case 20:
                    Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                    break;
                case 21:
                    KeyguardViewMediator.this.handleNotifyStartedWakingUp();
                    return;
                case 22:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOn();
                    return;
                case 23:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOff();
                    return;
                case 24:
                    KeyguardViewMediator.this.handleNotifyStartedGoingToSleep();
                    return;
            }
            KeyguardViewMediator.this.handleOnActivityDrawn();
        }
    };
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                KeyguardViewMediator.this.mStatusBarKeyguardViewManager.keyguardGoingAway();
                int flags = 0;
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.shouldDisableWindowAnimationsForUnlock() || KeyguardViewMediator.this.mWakeAndUnlocking) {
                    flags = 2;
                }
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isGoingToNotificationShade()) {
                    flags |= 1;
                }
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isUnlockWithWallpaper()) {
                    flags |= 4;
                }
                ActivityManagerNative.getDefault().keyguardGoingAway(flags);
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
        }
    };
    private boolean mIsIPOShutDown = false;

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
        this.mContext.registerReceiver(this.mBroadcastReceiver, new IntentFilter("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK"));
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        filter.addAction("android.intent.action.ACTION_PRE_SHUTDOWN");
        filter.addAction("android.intent.action.ACTION_SHUTDOWN_IPO");
        filter.addAction("android.intent.action.ACTION_PREBOOT_IPO");
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        KeyguardUpdateMonitor.setCurrentUser(ActivityManager.getCurrentUser());
        setShowingLocked((shouldWaitForProvisioning() || this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) ? false : true);
        updateInputRestrictedLocked();
        this.mTrustManager.reportKeyguardShowingChanged();
        this.mStatusBarKeyguardViewManager = SystemUIFactory.getInstance().createStatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver cr = this.mContext.getContentResolver();
        this.mDeviceInteractive = this.mPM.isInteractive();
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
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, R.anim.ic_signal_wifi_transient_animation_2);
        this.mDialogManager = KeyguardDialogManager.getInstance(this.mContext);
        AntiTheftManager.checkPplStatus();
        this.mAntiTheftManager = AntiTheftManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        this.mAntiTheftManager.doAntiTheftLockCheck();
        this.mPowerOffAlarmManager = PowerOffAlarmManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        this.mVoiceWakeupManager = VoiceWakeupManager.getInstance();
        this.mVoiceWakeupManager.init(this.mContext, this.mViewMediatorCallback);
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
            Log.d("KeyguardViewMediator", "onSystemReady");
            this.mSystemReady = true;
            doKeyguardLocked(null);
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
            this.mPowerOffAlarmManager.onSystemReady();
        }
        this.mIsPerUserLock = StorageManager.isFileEncryptedNativeOrEmulated();
        maybeSendUserPresentBroadcast();
    }

    public void onStartedGoingToSleep(int why) {
        Log.d("KeyguardViewMediator", "onStartedGoingToSleep(" + why + ")");
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = true;
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            boolean lockImmediately = this.mLockPatternUtils.getPowerButtonInstantlyLocks(currentUser) || !this.mLockPatternUtils.isSecure(currentUser);
            long timeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
            this.mLockLater = false;
            KeyguardPluginFactory.getKeyguardUtilExt(this.mContext).lockImmediatelyWhenScreenTimeout();
            Log.d("KeyguardViewMediator", "onStartedGoingToSleep(" + why + ") ---ScreenOff mScreenOn = false; After--boolean lockImmediately=" + lockImmediately + ", mExitSecureCallback=" + this.mExitSecureCallback + ", mShowing=" + this.mShowing + ", mIsIPOShutDown = " + this.mIsIPOShutDown);
            if (this.mExitSecureCallback != null) {
                Log.d("KeyguardViewMediator", "pending exit secure callback cancelled");
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
                this.mPendingReset = true;
            } else if ((why == 3 && timeout > 0) || (why == 2 && !lockImmediately && !this.mIsIPOShutDown)) {
                doKeyguardLaterLocked(timeout);
                this.mLockLater = true;
            } else if (why == 4) {
                Log.d("KeyguardViewMediator", "Screen off because PROX_SENSOR, do not draw lock view.");
            } else if (!this.mLockPatternUtils.isLockScreenDisabled(currentUser)) {
                this.mPendingLock = true;
            }
            if (this.mPendingLock) {
                playSounds(true);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedGoingToSleep(why);
        notifyStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep(int why, boolean cameraGestureTriggered) {
        Log.d("KeyguardViewMediator", "onFinishedGoingToSleep(" + why + ")");
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            notifyFinishedGoingToSleep();
            if (cameraGestureTriggered) {
                Log.i("KeyguardViewMediator", "Camera gesture was triggered, preventing Keyguard locking.");
                ((PowerManager) this.mContext.getSystemService(PowerManager.class)).wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
                this.mPendingLock = false;
                this.mPendingReset = false;
            }
            if (this.mPendingReset) {
                resetStateLocked();
                this.mPendingReset = false;
            }
            if (this.mPendingLock) {
                doKeyguardLocked(null);
                this.mPendingLock = false;
            }
            if (!this.mLockLater && !cameraGestureTriggered) {
                doKeyguardForChildProfilesLocked();
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchFinishedGoingToSleep(why);
    }

    private long getLockTimeout(int userId) {
        ContentResolver cr = this.mContext.getContentResolver();
        long lockAfterTimeout = Settings.Secure.getInt(cr, "lock_screen_lock_after_timeout", 5000);
        long policyTimeout = this.mLockPatternUtils.getDevicePolicyManager().getMaximumTimeToLockForUserAndProfiles(userId);
        if (policyTimeout <= 0) {
            return lockAfterTimeout;
        }
        long displayTimeout = Settings.System.getInt(cr, "screen_off_timeout", 30000);
        long timeout = Math.min(policyTimeout - Math.max(displayTimeout, 0L), lockAfterTimeout);
        return Math.max(timeout, 0L);
    }

    public void doKeyguardLaterLocked() {
        long timeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
        if (timeout == 0) {
            doKeyguardLocked(null);
        } else {
            doKeyguardLaterLocked(timeout);
        }
    }

    private void doKeyguardLaterLocked(long timeout) {
        long when = SystemClock.elapsedRealtime() + timeout;
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        intent.addFlags(268435456);
        PendingIntent sender = PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456);
        this.mAlarmManager.setExactAndAllowWhileIdle(2, when, sender);
        Log.d("KeyguardViewMediator", "setting alarm to turn off keyguard, seq = " + this.mDelayedShowingSequence);
        doKeyguardLaterForChildProfilesLocked();
    }

    private void doKeyguardLaterForChildProfilesLocked() {
        UserManager um = UserManager.get(this.mContext);
        for (int profileId : um.getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(profileId)) {
                long userTimeout = getLockTimeout(profileId);
                if (userTimeout == 0) {
                    doKeyguardForChildProfilesLocked();
                } else {
                    long userWhen = SystemClock.elapsedRealtime() + userTimeout;
                    Intent lockIntent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
                    lockIntent.putExtra("seq", this.mDelayedProfileShowingSequence);
                    lockIntent.putExtra("android.intent.extra.USER_ID", profileId);
                    lockIntent.addFlags(268435456);
                    PendingIntent lockSender = PendingIntent.getBroadcast(this.mContext, 0, lockIntent, 268435456);
                    this.mAlarmManager.setExactAndAllowWhileIdle(2, userWhen, lockSender);
                }
            }
        }
    }

    private void doKeyguardForChildProfilesLocked() {
        UserManager um = UserManager.get(this.mContext);
        for (int profileId : um.getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(profileId)) {
                lockProfile(profileId);
            }
        }
    }

    private void cancelDoKeyguardLaterLocked() {
        this.mDelayedShowingSequence++;
    }

    private void cancelDoKeyguardForChildProfilesLocked() {
        this.mDelayedProfileShowingSequence++;
    }

    public void onStartedWakingUp() {
        synchronized (this) {
            this.mDeviceInteractive = true;
            cancelDoKeyguardLaterLocked();
            cancelDoKeyguardForChildProfilesLocked();
            Log.d("KeyguardViewMediator", "onStartedWakingUp, seq = " + this.mDelayedShowingSequence);
            notifyStartedWakingUp();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedWakingUp();
        maybeSendUserPresentBroadcast();
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback callback) {
        notifyScreenOn(callback);
    }

    public void onScreenTurnedOn() {
        notifyScreenTurnedOn();
        this.mUpdateMonitor.dispatchScreenTurnedOn();
    }

    public void onScreenTurnedOff() {
        notifyScreenTurnedOff();
        this.mUpdateMonitor.dispatchScreenTurnedOff();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            sendUserPresentBroadcast();
        } else {
            if (!this.mSystemReady || !shouldWaitForProvisioning()) {
                return;
            }
            getLockPatternUtils().userPresent(KeyguardUpdateMonitor.getCurrentUser());
        }
    }

    public void onDreamingStarted() {
        synchronized (this) {
            if (this.mDeviceInteractive && this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                doKeyguardLaterLocked();
            }
        }
    }

    public void onDreamingStopped() {
        synchronized (this) {
            if (this.mDeviceInteractive) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    public void setKeyguardEnabled(boolean enabled) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "setKeyguardEnabled(" + enabled + ")");
            this.mExternallyEnabled = enabled;
            if (!enabled && this.mShowing) {
                if (this.mExitSecureCallback != null) {
                    Log.d("KeyguardViewMediator", "in process of verifyUnlock request, ignoring");
                    return;
                }
                Log.d("KeyguardViewMediator", "remembering to reshow, hiding keyguard, disabling status bar expansion");
                this.mNeedToReshowWhenReenabled = true;
                updateInputRestrictedLocked();
                hideLocked();
            } else if (enabled && this.mNeedToReshowWhenReenabled) {
                Log.d("KeyguardViewMediator", "previously hidden, reshowing, reenabling status bar expansion");
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestrictedLocked();
                if (this.mExitSecureCallback != null) {
                    Log.d("KeyguardViewMediator", "onKeyguardExitResult(false), resetting");
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
                    Log.d("KeyguardViewMediator", "waiting until mWaitingUntilKeyguardVisible is false");
                    while (this.mWaitingUntilKeyguardVisible) {
                        try {
                            wait();
                        } catch (InterruptedException e2) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    Log.d("KeyguardViewMediator", "done waiting for mWaitingUntilKeyguardVisible");
                }
            }
        }
    }

    public void verifyUnlock(IKeyguardExitCallback callback) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "verifyUnlock");
            if (shouldWaitForProvisioning()) {
                Log.d("KeyguardViewMediator", "ignoring because device isn't provisioned");
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
            } else if (!isSecure()) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
                try {
                    callback.onKeyguardExitResult(true);
                } catch (RemoteException e4) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e4);
                }
            } else {
                try {
                    callback.onKeyguardExitResult(false);
                } catch (RemoteException e5) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e5);
                }
            }
        }
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public void setOccluded(boolean isOccluded) {
        if (this.mOccluded == isOccluded) {
            return;
        }
        Log.d("KeyguardViewMediator", "setOccluded, mOccluded=" + this.mOccluded + ", isOccluded=" + isOccluded);
        this.mOccluded = isOccluded;
        this.mHandler.removeMessages(12);
        Message msg = this.mHandler.obtainMessage(12, isOccluded ? 1 : 0, 0);
        this.mHandler.sendMessage(msg);
    }

    public void handleSetOccluded(boolean isOccluded) {
        synchronized (this) {
            if (this.mHiding && isOccluded) {
                startKeyguardExitAnimation(0L, 0L);
            }
            this.mStatusBarKeyguardViewManager.setOccluded(isOccluded);
            updateActivityLockScreenState();
            adjustStatusBarLocked();
        }
    }

    public void doKeyguardTimeout(Bundle options) {
        this.mHandler.removeMessages(13);
        Message msg = this.mHandler.obtainMessage(13, options);
        this.mHandler.sendMessage(msg);
    }

    public boolean isInputRestricted() {
        if (!sIsUserBuild) {
            Log.d("KeyguardViewMediator", "isInputRestricted: showing=" + this.mShowing + ", needReshow=" + this.mNeedToReshowWhenReenabled);
        }
        if (this.mShowing) {
            return true;
        }
        return this.mNeedToReshowWhenReenabled;
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
            Log.d("KeyguardViewMediator", "isInputRestricted: showing=" + this.mShowing + ", needReshow=" + this.mNeedToReshowWhenReenabled + ", provisioned=" + this.mUpdateMonitor.isDeviceProvisioned());
        }
    }

    private void updateInputRestrictedLocked() {
        boolean inputRestricted = isInputRestricted();
        if (this.mInputRestricted == inputRestricted) {
            return;
        }
        this.mInputRestricted = inputRestricted;
        int size = this.mKeyguardStateCallbacks.size();
        for (int i = size - 1; i >= 0; i--) {
            try {
                this.mKeyguardStateCallbacks.get(i).onInputRestrictedStateChanged(inputRestricted);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(i);
                }
            }
        }
    }

    public void doKeyguardLocked(Bundle options) {
        if (!this.mExternallyEnabled || PowerOffAlarmManager.isAlarmBoot()) {
            Log.d("KeyguardViewMediator", "doKeyguard : externally disabled reason..mExternallyEnabled = " + this.mExternallyEnabled);
            return;
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            resetStateLocked();
            Log.d("KeyguardViewMediator", "doKeyguard: not showing because it is already showing");
            return;
        }
        if (!UserManager.isSplitSystemUser() || KeyguardUpdateMonitor.getCurrentUser() != 0 || !this.mUpdateMonitor.isDeviceProvisioned()) {
            boolean requireSim = !SystemProperties.getBoolean("keyguard.no_require_sim", true);
            boolean provisioned = this.mUpdateMonitor.isDeviceProvisioned();
            boolean lockedOrMissing = false;
            int i = 0;
            while (true) {
                if (i >= KeyguardUtils.getNumOfPhone()) {
                    break;
                }
                if (!isSimLockedOrMissing(i, requireSim)) {
                    i++;
                } else {
                    lockedOrMissing = true;
                    break;
                }
            }
            boolean antiTheftLocked = AntiTheftManager.isAntiTheftLocked();
            Log.d("KeyguardViewMediator", "lockedOrMissing is " + lockedOrMissing + ", requireSim=" + requireSim + ", provisioned=" + provisioned + ", antiTheftLocked=" + antiTheftLocked);
            if (!lockedOrMissing && shouldWaitForProvisioning() && !antiTheftLocked) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because device isn't provisioned and the sim is not locked or missing");
                return;
            }
            if (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) && !lockedOrMissing && !antiTheftLocked) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because lockscreen is off");
                return;
            } else if (this.mLockPatternUtils.checkVoldPassword(KeyguardUpdateMonitor.getCurrentUser()) && KeyguardUtils.isSystemEncrypted()) {
                Log.d("KeyguardViewMediator", "Not showing lock screen since just decrypted");
                setShowingLocked(false);
                hideLocked();
                this.mUpdateMonitor.reportSuccessfulStrongAuthUnlockAttempt();
                return;
            }
        }
        Log.d("KeyguardViewMediator", "doKeyguard: showing the lock screen");
        showLocked(options);
    }

    public void lockProfile(int userId) {
        this.mTrustManager.setDeviceLockedForUser(userId, true);
    }

    public boolean shouldWaitForProvisioning() {
        return (this.mUpdateMonitor.isDeviceProvisioned() || isSecure()) ? false : true;
    }

    private boolean isSimLockedOrMissing(int phoneId, boolean requireSim) {
        IccCardConstants.State state = this.mUpdateMonitor.getSimStateOfPhoneId(phoneId);
        if (this.mUpdateMonitor.isSimPinSecure(phoneId)) {
            return true;
        }
        if (state == IccCardConstants.State.ABSENT || state == IccCardConstants.State.PERM_DISABLED) {
            return requireSim;
        }
        return false;
    }

    public void handleDismiss(boolean authenticated) {
        if (!this.mShowing || this.mOccluded) {
            return;
        }
        this.mStatusBarKeyguardViewManager.dismiss(authenticated);
    }

    public void dismiss() {
        dismiss(false);
    }

    public void dismiss(boolean authenticated) {
        Log.d("KeyguardViewMediator", "dismiss, authenticated = " + authenticated);
        Message msg = this.mHandler.obtainMessage(17, new Boolean(authenticated));
        this.mHandler.sendMessage(msg);
    }

    public void resetStateLocked() {
        Log.e("KeyguardViewMediator", "resetStateLocked");
        Message msg = this.mHandler.obtainMessage(4);
        this.mHandler.sendMessage(msg);
    }

    private void notifyStartedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyStartedGoingToSleep");
        this.mHandler.sendEmptyMessage(24);
    }

    private void notifyFinishedGoingToSleep() {
        Log.d("KeyguardViewMediator", "notifyFinishedGoingToSleep");
        this.mHandler.sendEmptyMessage(6);
    }

    private void notifyStartedWakingUp() {
        Log.d("KeyguardViewMediator", "notifyStartedWakingUp");
        this.mHandler.sendEmptyMessage(21);
    }

    private void notifyScreenOn(IKeyguardDrawnCallback callback) {
        Log.d("KeyguardViewMediator", "notifyScreenOn");
        Message msg = this.mHandler.obtainMessage(7, callback);
        this.mHandler.sendMessage(msg);
    }

    private void notifyScreenTurnedOn() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOn");
        Message msg = this.mHandler.obtainMessage(22);
        this.mHandler.sendMessage(msg);
    }

    private void notifyScreenTurnedOff() {
        Log.d("KeyguardViewMediator", "notifyScreenTurnedOff");
        Message msg = this.mHandler.obtainMessage(23);
        this.mHandler.sendMessage(msg);
    }

    public void showLocked(Bundle options) {
        Log.d("KeyguardViewMediator", "showLocked");
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
        if (SystemProperties.getInt("ro.special", 0) == 1 && securityMode == KeyguardSecurityModel.SecurityMode.None) {
            return;
        }
        setReadyToShow(true);
        updateActivityLockScreenState();
        this.mShowKeyguardWakeLock.acquire();
        Message msg = this.mHandler.obtainMessage(2, options);
        this.mHandler.sendMessage(msg);
    }

    public void hideLocked() {
        Log.d("KeyguardViewMediator", "hideLocked");
        Message msg = this.mHandler.obtainMessage(3);
        this.mHandler.sendMessage(msg);
    }

    public boolean isSecure() {
        if (this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser()) || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure()) {
            return true;
        }
        return AntiTheftManager.isAntiTheftLocked();
    }

    public void setCurrentUser(int newUserId) {
        KeyguardUpdateMonitor.setCurrentUser(newUserId);
    }

    public void keyguardDone(boolean authenticated) {
        Log.d("KeyguardViewMediator", "keyguardDone(" + authenticated + ")");
        EventLog.writeEvent(70000, 2);
        Message msg = this.mHandler.obtainMessage(9, Integer.valueOf(authenticated ? 1 : 0));
        this.mHandler.sendMessage(msg);
    }

    public void handleKeyguardDone(boolean authenticated) {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (this.mLockPatternUtils.isSecure(currentUser)) {
            this.mLockPatternUtils.getDevicePolicyManager().reportKeyguardDismissed(currentUser);
        }
        Log.d("KeyguardViewMediator", "handleKeyguardDone");
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        if (AntiTheftManager.isAntiTheftLocked()) {
            Log.d("KeyguardViewMediator", "handleKeyguardDone() - Skip keyguard done! antitheft = " + AntiTheftManager.isAntiTheftLocked() + " or sim = " + this.mUpdateMonitor.isSimPinSecure());
            return;
        }
        mKeyguardDoneOnGoing = true;
        if (authenticated) {
            this.mUpdateMonitor.clearFailedUnlockAttempts();
        }
        this.mUpdateMonitor.clearFingerprintRecognized();
        if (this.mGoingToSleep) {
            Log.i("KeyguardViewMediator", "Device is going to sleep, aborting keyguardDone");
            return;
        }
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
        this.mSuppressNextLockSound = false;
        handleHide();
    }

    public void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                int currentUserId = KeyguardUpdateMonitor.getCurrentUser();
                UserHandle currentUser = new UserHandle(currentUserId);
                UserManager um = (UserManager) this.mContext.getSystemService("user");
                for (int profileId : um.getProfileIdsWithDisabled(currentUser.getIdentifier())) {
                    this.mContext.sendBroadcastAsUser(USER_PRESENT_INTENT, UserHandle.of(profileId));
                }
                getLockPatternUtils().userPresent(currentUserId);
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    public void handleKeyguardDoneDrawing() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing");
            if (this.mWaitingUntilKeyguardVisible) {
                Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(10);
            }
        }
    }

    private void playSounds(boolean locked) {
        Log.d("KeyguardViewMediator", "playSounds(locked = " + locked + "), mSuppressNextLockSound =" + this.mSuppressNextLockSound);
        if (this.mSuppressNextLockSound) {
            this.mSuppressNextLockSound = false;
        } else {
            playSound(locked ? this.mLockSoundId : this.mUnlockSoundId);
        }
    }

    private void playSound(int soundId) {
        if (soundId == 0) {
            return;
        }
        ContentResolver cr = this.mContext.getContentResolver();
        if (Settings.System.getInt(cr, "lockscreen_sounds_enabled", 1) != 1) {
            return;
        }
        this.mLockSounds.stop(this.mLockSoundStreamId);
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
            if (this.mAudioManager == null) {
                return;
            } else {
                this.mUiSoundsStreamType = this.mAudioManager.getUiSoundsStreamType();
            }
        }
        if (this.mAudioManager.isStreamMute(this.mUiSoundsStreamType)) {
            return;
        }
        this.mLockSoundStreamId = this.mLockSounds.play(soundId, this.mLockSoundVolume, this.mLockSoundVolume, 1, 0, 1.0f);
    }

    public void playTrustedSound() {
        if (this.mSuppressNextLockSound) {
            return;
        }
        playSound(this.mTrustedSoundId);
    }

    private void setReadyToShow(boolean readyToShow) {
        this.mReadyToShow = readyToShow;
        Log.d("KeyguardViewMediator", "mReadyToShow set as " + this.mReadyToShow);
    }

    private void updateActivityLockScreenState() {
        try {
            Log.d("KeyguardViewMediator", "updateActivityLockScreenState() - mShowing = " + this.mShowing + " !mOccluded = " + (!this.mOccluded));
            ActivityManagerNative.getDefault().setLockScreenShown(this.mShowing, this.mOccluded);
        } catch (RemoteException e) {
        }
    }

    public void handleShow(Bundle options) {
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (this.mLockPatternUtils.isSecure(currentUser)) {
            this.mLockPatternUtils.getDevicePolicyManager().reportKeyguardSecured(currentUser);
        }
        synchronized (this) {
            if (!this.mSystemReady) {
                Log.d("KeyguardViewMediator", "ignoring handleShow because system is not ready.");
                setReadyToShow(false);
                updateActivityLockScreenState();
                return;
            }
            Log.d("KeyguardViewMediator", "handleShow");
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.show(options);
            this.mHiding = false;
            this.mWakeAndUnlocking = false;
            resetKeyguardDonePendingLocked();
            setReadyToShow(false);
            this.mHideAnimationRun = false;
            updateActivityLockScreenState();
            adjustStatusBarLocked();
            userActivity();
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        ActivityManagerNative.getDefault().closeSystemDialogs("lock");
                    } catch (RemoteException e) {
                        Log.e("KeyguardViewMediator", "handleShow() - error in closeSystemDialogs()");
                    }
                }
            }, 500L);
            if (PowerOffAlarmManager.isAlarmBoot()) {
                this.mPowerOffAlarmManager.startAlarm();
            }
            this.mShowKeyguardWakeLock.release();
            Log.d("KeyguardViewMediator", "handleShow exit");
            if (this.mKeyguardDisplayManager != null) {
                Log.d("KeyguardViewMediator", "handle show call mKeyguardDisplayManager.show()");
                this.mKeyguardDisplayManager.show();
            } else {
                Log.d("KeyguardViewMediator", "handle show mKeyguardDisplayManager is null");
            }
        }
    }

    public void handleHide() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleHide");
            if (UserManager.isSplitSystemUser() && KeyguardUpdateMonitor.getCurrentUser() == 0) {
                Log.d("KeyguardViewMediator", "Split system user, quit unlocking.");
                return;
            }
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

    public void handleOnActivityDrawn() {
        Log.d("KeyguardViewMediator", "handleOnActivityDrawn: mKeyguardDonePending=" + this.mKeyguardDonePending);
        if (!this.mKeyguardDonePending) {
            return;
        }
        this.mStatusBarKeyguardViewManager.onActivityDrawn();
    }

    public void handleStartKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation() is called.");
        synchronized (this) {
            if (!this.mHiding) {
                Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation() - returns, !mHiding = " + (this.mHiding ? false : true));
                return;
            }
            this.mHiding = false;
            if (this.mWakeAndUnlocking && this.mDrawnCallback != null) {
                this.mStatusBarKeyguardViewManager.getViewRootImpl().setReportNextDraw();
                notifyDrawn(this.mDrawnCallback);
            }
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(this.mPhoneState) && this.mShowing) {
                playSounds(false);
            }
            setShowingLocked(false);
            this.mStatusBarKeyguardViewManager.hide(startTime, fadeoutDuration);
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            updateActivityLockScreenState();
            adjustStatusBarLocked();
            sendUserPresentBroadcast();
            Log.d("KeyguardViewMediator", "set mKeyguardDoneOnGoing = false");
            mKeyguardDoneOnGoing = false;
        }
    }

    void adjustStatusBarLocked() {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        if (this.mStatusBarManager == null) {
            Log.w("KeyguardViewMediator", "Could not get status bar manager");
            return;
        }
        isSecure();
        int flags = 0;
        if (this.mShowing) {
            flags = 16777216;
            if (PowerOffAlarmManager.isAlarmBoot()) {
                flags = 16777216 | 33554432;
            }
        }
        if (isShowingAndNotOccluded()) {
            flags |= 2097152;
        }
        Log.d("KeyguardViewMediator", "adjustStatusBarLocked: mShowing=" + this.mShowing + " mOccluded=" + this.mOccluded + " isSecure=" + isSecure() + " --> flags=0x" + Integer.toHexString(flags));
        if (this.mContext instanceof Activity) {
            return;
        }
        this.mStatusBarManager.disable(flags);
    }

    public void handleReset() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleReset");
            this.mStatusBarKeyguardViewManager.reset();
            adjustStatusBarLocked();
        }
    }

    public void handleVerifyUnlock() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleVerifyUnlock");
            setShowingLocked(true);
            this.mStatusBarKeyguardViewManager.verifyUnlock();
            updateActivityLockScreenState();
        }
    }

    public void handleNotifyStartedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyStartedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onStartedGoingToSleep();
        }
    }

    public void handleNotifyFinishedGoingToSleep() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyFinishedGoingToSleep");
            this.mStatusBarKeyguardViewManager.onFinishedGoingToSleep();
        }
    }

    public void handleNotifyStartedWakingUp() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyWakingUp");
            this.mStatusBarKeyguardViewManager.onStartedWakingUp();
        }
    }

    public void handleNotifyScreenTurningOn(IKeyguardDrawnCallback callback) {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurningOn");
            this.mStatusBarKeyguardViewManager.onScreenTurningOn();
            if (callback != null) {
                if (this.mWakeAndUnlocking) {
                    this.mDrawnCallback = callback;
                } else {
                    notifyDrawn(callback);
                }
            }
        }
    }

    public void handleNotifyScreenTurnedOn() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOn");
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn();
        }
    }

    public void handleNotifyScreenTurnedOff() {
        synchronized (this) {
            Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOff");
            this.mStatusBarKeyguardViewManager.onScreenTurnedOff();
            this.mWakeAndUnlocking = false;
        }
    }

    private void notifyDrawn(IKeyguardDrawnCallback callback) {
        try {
            callback.onDrawn();
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Exception calling onDrawn():", e);
        }
    }

    public void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(20);
    }

    @Override
    public void onBootCompleted() {
        Log.d("KeyguardViewMediator", "onBootCompleted() is called");
        this.mUpdateMonitor.dispatchBootCompleted();
        synchronized (this) {
            this.mBootCompleted = true;
            if (this.mBootSendUserPresent) {
                sendUserPresentBroadcast();
            }
        }
    }

    public void onWakeAndUnlocking() {
        this.mWakeAndUnlocking = true;
        keyguardDone(true);
    }

    public StatusBarKeyguardViewManager registerStatusBar(PhoneStatusBar phoneStatusBar, ViewGroup container, StatusBarWindowManager statusBarWindowManager, ScrimController scrimController, FingerprintUnlockController fingerprintUnlockController) {
        this.mStatusBarKeyguardViewManager.registerStatusBar(phoneStatusBar, container, statusBarWindowManager, scrimController, fingerprintUnlockController);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long startTime, long fadeoutDuration) {
        Message msg = this.mHandler.obtainMessage(18, new StartKeyguardExitAnimParams(startTime, fadeoutDuration, null));
        this.mHandler.sendMessage(msg);
    }

    public void onActivityDrawn() {
        this.mHandler.sendEmptyMessage(19);
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    public LockPatternUtils getLockPatternUtils() {
        return this.mLockPatternUtils;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.print("  mSystemReady: ");
        pw.println(this.mSystemReady);
        pw.print("  mBootCompleted: ");
        pw.println(this.mBootCompleted);
        pw.print("  mBootSendUserPresent: ");
        pw.println(this.mBootSendUserPresent);
        pw.print("  mExternallyEnabled: ");
        pw.println(this.mExternallyEnabled);
        pw.print("  mNeedToReshowWhenReenabled: ");
        pw.println(this.mNeedToReshowWhenReenabled);
        pw.print("  mShowing: ");
        pw.println(this.mShowing);
        pw.print("  mInputRestricted: ");
        pw.println(this.mInputRestricted);
        pw.print("  mOccluded: ");
        pw.println(this.mOccluded);
        pw.print("  mDelayedShowingSequence: ");
        pw.println(this.mDelayedShowingSequence);
        pw.print("  mExitSecureCallback: ");
        pw.println(this.mExitSecureCallback);
        pw.print("  mDeviceInteractive: ");
        pw.println(this.mDeviceInteractive);
        pw.print("  mGoingToSleep: ");
        pw.println(this.mGoingToSleep);
        pw.print("  mHiding: ");
        pw.println(this.mHiding);
        pw.print("  mWaitingUntilKeyguardVisible: ");
        pw.println(this.mWaitingUntilKeyguardVisible);
        pw.print("  mKeyguardDonePending: ");
        pw.println(this.mKeyguardDonePending);
        pw.print("  mHideAnimationRun: ");
        pw.println(this.mHideAnimationRun);
        pw.print("  mPendingReset: ");
        pw.println(this.mPendingReset);
        pw.print("  mPendingLock: ");
        pw.println(this.mPendingLock);
        pw.print("  mWakeAndUnlocking: ");
        pw.println(this.mWakeAndUnlocking);
        pw.print("  mDrawnCallback: ");
        pw.println(this.mDrawnCallback);
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        StartKeyguardExitAnimParams(long startTime, long fadeoutDuration, StartKeyguardExitAnimParams startKeyguardExitAnimParams) {
            this(startTime, fadeoutDuration);
        }

        private StartKeyguardExitAnimParams(long startTime, long fadeoutDuration) {
            this.startTime = startTime;
            this.fadeoutDuration = fadeoutDuration;
        }
    }

    private void setShowingLocked(boolean showing) {
        Log.d("KeyguardViewMediator", "setShowingLocked() - showing = " + showing + ", mShowing = " + this.mShowing);
        if (showing == this.mShowing) {
            return;
        }
        this.mSecurityModel = new KeyguardSecurityModel(this.mContext);
        KeyguardSecurityModel.SecurityMode securityMode = this.mSecurityModel.getSecurityMode();
        if (SystemProperties.getInt("ro.special", 0) == 1 && securityMode == KeyguardSecurityModel.SecurityMode.None) {
            this.mShowing = false;
        } else {
            this.mShowing = showing;
        }
        int size = this.mKeyguardStateCallbacks.size();
        for (int i = size - 1; i >= 0; i--) {
            try {
                this.mKeyguardStateCallbacks.get(i).onShowingStateChanged(showing);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(i);
                }
            }
        }
        updateInputRestrictedLocked();
        this.mTrustManager.reportKeyguardShowingChanged();
    }

    public void addStateMonitorCallback(IKeyguardStateCallback callback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(callback);
            try {
                callback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                callback.onShowingStateChanged(this.mShowing);
                callback.onInputRestrictedStateChanged(this.mInputRestricted);
                callback.onAntiTheftStateChanged(AntiTheftManager.isAntiTheftLocked());
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged or onSimSecureStateChanged or onInputRestrictedStateChanged", e);
            }
        }
    }

    public void removeKeyguardDoneMsg() {
        this.mHandler.removeMessages(9);
    }

    private class InvalidDialogCallback implements KeyguardDialogManager.DialogShowCallBack {
        InvalidDialogCallback(KeyguardViewMediator this$0, InvalidDialogCallback invalidDialogCallback) {
            this();
        }

        private InvalidDialogCallback() {
        }

        @Override
        public void show() {
            String title = KeyguardViewMediator.this.mContext.getString(com.android.systemui.R.string.invalid_sim_title);
            String message = KeyguardViewMediator.this.mContext.getString(com.android.systemui.R.string.invalid_sim_message);
            AlertDialog dialog = KeyguardViewMediator.this.createDialog(title, message);
            dialog.show();
        }
    }

    private class MeLockedDialogCallback implements KeyguardDialogManager.DialogShowCallBack {
        MeLockedDialogCallback(KeyguardViewMediator this$0, MeLockedDialogCallback meLockedDialogCallback) {
            this();
        }

        private MeLockedDialogCallback() {
        }

        @Override
        public void show() {
            String message = KeyguardViewMediator.this.mContext.getString(com.android.systemui.R.string.simlock_slot_locked_message);
            AlertDialog dialog = KeyguardViewMediator.this.createDialog(null, message);
            dialog.show();
        }
    }

    public AlertDialog createDialog(String title, String message) {
        AlertDialog dialog = new AlertDialog.Builder(this.mContext).setTitle(title).setIcon(R.drawable.ic_dialog_alert).setCancelable(false).setMessage(message).setNegativeButton(com.android.systemui.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog2, int which) {
                KeyguardViewMediator.this.mDialogManager.reportDialogClose();
                Log.d("KeyguardViewMediator", "invalid sim card ,reportCloseDialog");
            }
        }).create();
        dialog.getWindow().setType(2003);
        return dialog;
    }

    void setSuppressPlaySoundFlag() {
        this.mSuppressNextLockSound = true;
    }

    public boolean isKeyguardDoneOnGoing() {
        return mKeyguardDoneOnGoing;
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    void updateNavbarStatus() {
        Log.d("KeyguardViewMediator", "updateNavbarStatus() is called.");
        this.mStatusBarKeyguardViewManager.updateStates();
    }

    public void updateAntiTheftLocked() {
        boolean isAntiTheftLocked = AntiTheftManager.isAntiTheftLocked();
        Log.d("KeyguardViewMediator", "updateAntiTheftLocked() - isAntiTheftLocked = " + isAntiTheftLocked);
        try {
            int size = this.mKeyguardStateCallbacks.size();
            for (int i = 0; i < size; i++) {
                this.mKeyguardStateCallbacks.get(i).onAntiTheftStateChanged(isAntiTheftLocked);
            }
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Failed to call onAntiTheftStateChanged", e);
        }
    }

    public boolean isKeyguardExternallyEnabled() {
        return this.mExternallyEnabled;
    }
}
