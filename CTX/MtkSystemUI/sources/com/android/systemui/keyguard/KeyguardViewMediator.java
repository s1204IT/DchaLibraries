package com.android.systemui.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
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
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.policy.IKeyguardDismissCallback;
import com.android.internal.policy.IKeyguardDrawnCallback;
import com.android.internal.policy.IKeyguardExitCallback;
import com.android.internal.policy.IKeyguardStateCallback;
import com.android.internal.telephony.IccCardConstants;
import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardConstants;
import com.android.keyguard.KeyguardDisplayManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.keyguard.KeyguardUtils;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.SystemUI;
import com.android.systemui.SystemUIFactory;
import com.android.systemui.UiOffloadThread;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.statusbar.phone.FingerprintUnlockController;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.mediatek.keyguard.AntiTheft.AntiTheftManager;
import com.mediatek.keyguard.PowerOffAlarm.PowerOffAlarmManager;
import com.mediatek.keyguard.Telephony.KeyguardDialogManager;
import com.mediatek.keyguard.VoiceWakeup.VoiceWakeupManagerProxy;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public class KeyguardViewMediator extends SystemUI {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final boolean DEBUG_SIM_STATES = KeyguardConstants.DEBUG_SIM_STATES;
    private static final Intent USER_PRESENT_INTENT = new Intent("android.intent.action.USER_PRESENT").addFlags(606076928);
    private static boolean mKeyguardDoneOnGoing = false;
    private AlarmManager mAlarmManager;
    private AntiTheftManager mAntiTheftManager;
    private boolean mAodShowing;
    private AudioManager mAudioManager;
    private boolean mBootCompleted;
    private boolean mBootSendUserPresent;
    private CharSequence mCustomMessage;
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
    private KeyguardDisplayManager mKeyguardDisplayManager;
    private boolean mLockLater;
    private LockPatternUtils mLockPatternUtils;
    private int mLockSoundId;
    private int mLockSoundStreamId;
    private float mLockSoundVolume;
    private SoundPool mLockSounds;
    private boolean mLockWhenSimRemoved;
    private PowerManager mPM;
    private boolean mPendingLock;
    private boolean mPendingReset;
    private PowerOffAlarmManager mPowerOffAlarmManager;
    private PowerManager.WakeLock mShowKeyguardWakeLock;
    private boolean mShowing;
    private boolean mShuttingDown;
    private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    private StatusBarManager mStatusBarManager;
    private boolean mSystemReady;
    private TrustManager mTrustManager;
    private int mTrustedSoundId;
    private int mUiSoundsStreamType;
    private int mUnlockSoundId;
    private KeyguardUpdateMonitor mUpdateMonitor;
    private VoiceWakeupManagerProxy mVoiceWakeupManager;
    private boolean mWakeAndUnlocking;
    private WorkLockActivityController mWorkLockController;
    private final UiOffloadThread mUiOffloadThread = (UiOffloadThread) Dependency.get(UiOffloadThread.class);
    private boolean mExternallyEnabled = true;
    private boolean mNeedToReshowWhenReenabled = false;
    private int mSecondaryDisplayShowing = -1;
    private boolean mOccluded = false;
    private final DismissCallbackRegistry mDismissCallbackRegistry = new DismissCallbackRegistry();
    private String mPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
    private boolean mWaitingUntilKeyguardVisible = false;
    private boolean mKeyguardDonePending = false;
    private boolean mHideAnimationRun = false;
    private boolean mHideAnimationRunning = false;
    private final ArrayList<IKeyguardStateCallback> mKeyguardStateCallbacks = new ArrayList<>();
    KeyguardUpdateMonitorCallback mUpdateCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onUserSwitching(int i) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.resetKeyguardDonePendingLocked();
                if (!KeyguardViewMediator.this.mLockPatternUtils.isLockScreenDisabled(i)) {
                    KeyguardViewMediator.this.resetStateLocked();
                } else {
                    KeyguardViewMediator.this.dismiss(null, null);
                }
                KeyguardViewMediator.this.adjustStatusBarLocked();
            }
        }

        @Override
        public void onUserSwitchComplete(int i) {
            UserInfo userInfo;
            if (i == 0 || (userInfo = UserManager.get(KeyguardViewMediator.this.mContext).getUserInfo(i)) == null || KeyguardViewMediator.this.mLockPatternUtils.isSecure(i)) {
                return;
            }
            if (userInfo.isGuest() || userInfo.isDemo()) {
                KeyguardViewMediator.this.dismiss(null, null);
            }
        }

        @Override
        public void onUserInfoChanged(int i) {
        }

        @Override
        public void onPhoneStateChanged(int i) {
            synchronized (KeyguardViewMediator.this) {
                if (i == 0) {
                    try {
                        if (!KeyguardViewMediator.this.mDeviceInteractive && KeyguardViewMediator.this.mExternallyEnabled && KeyguardViewMediator.DEBUG) {
                            Log.d("KeyguardViewMediator", "screen is off and call ended, let's make sure the keyguard is showing");
                        }
                    } catch (Throwable th) {
                        throw th;
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
                if (KeyguardViewMediator.this.mustNotUnlockCurrentUser()) {
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        @Override
        public void onSimStateChangedUsingPhoneId(int i, IccCardConstants.State state) {
            if (KeyguardViewMediator.DEBUG) {
                Log.d("KeyguardViewMediator", "onSimStateChangedUsingSubId: " + state + ", phoneId=" + i);
            }
            switch (AnonymousClass9.$SwitchMap$com$android$internal$telephony$IccCardConstants$State[state.ordinal()]) {
                case 1:
                case 2:
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.shouldWaitForProvisioning()) {
                            if (!KeyguardViewMediator.this.mShowing) {
                                if (KeyguardViewMediator.DEBUG_SIM_STATES) {
                                    Log.d("KeyguardViewMediator", "ICC_ABSENT isn't showing, we need to show the keyguard since the device isn't provisioned yet.");
                                }
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else {
                                KeyguardViewMediator.this.resetStateLocked();
                            }
                        }
                        if (state == IccCardConstants.State.ABSENT) {
                            onSimAbsentLocked();
                        }
                        break;
                    }
                    break;
                case 3:
                case 4:
                case 5:
                    synchronized (this) {
                        if (state == IccCardConstants.State.NETWORK_LOCKED && !KeyguardUtils.isMediatekSimMeLockSupport()) {
                            Log.d("KeyguardViewMediator", "Get NETWORK_LOCKED but not support ME lock. Not show.");
                        } else if (state == IccCardConstants.State.NETWORK_LOCKED && !KeyguardUtils.isSimMeLockValid(i)) {
                            Log.d("KeyguardViewMediator", "Get NETWORK_LOCKED but not to show with specific policy!");
                        } else if (KeyguardUtils.isSystemEncrypted()) {
                            if (KeyguardViewMediator.DEBUG) {
                                Log.d("KeyguardViewMediator", "Currently system needs to be decrypted. Not show.");
                            }
                        } else {
                            KeyguardViewMediator.this.mUpdateMonitor.setDismissFlagWhenWfcOn(state);
                            if (KeyguardViewMediator.this.mUpdateMonitor.getRetryPukCountOfPhoneId(i) == 0) {
                                KeyguardViewMediator.this.mDialogManager.requestShowDialog(new InvalidDialogCallback());
                            } else if (IccCardConstants.State.NETWORK_LOCKED == state && KeyguardViewMediator.this.mUpdateMonitor.getSimMeLeftRetryCountOfPhoneId(i) == 0) {
                                Log.d("KeyguardViewMediator", "SIM ME lock retrycount is 0, only to show dialog");
                                KeyguardViewMediator.this.mDialogManager.requestShowDialog(new MeLockedDialogCallback());
                            } else if (!KeyguardViewMediator.this.isShowing()) {
                                if (KeyguardViewMediator.DEBUG) {
                                    Log.d("KeyguardViewMediator", "INTENT_VALUE_ICC_LOCKED and keygaurd isn't showing; need to show keyguard so user can enter sim pin");
                                }
                                KeyguardViewMediator.this.doKeyguardLocked(null);
                            } else if (!KeyguardViewMediator.mKeyguardDoneOnGoing) {
                                KeyguardViewMediator.this.removeKeyguardDoneMsg();
                                KeyguardViewMediator.this.resetStateLocked();
                            } else {
                                Log.d("KeyguardViewMediator", "mKeyguardDoneOnGoing is true");
                                KeyguardViewMediator.this.doKeyguardLaterLocked();
                            }
                        }
                        break;
                    }
                    break;
                case 6:
                    synchronized (KeyguardViewMediator.this) {
                        if (!KeyguardViewMediator.this.mShowing) {
                            if (KeyguardViewMediator.DEBUG_SIM_STATES) {
                                Log.d("KeyguardViewMediator", "PERM_DISABLED and keygaurd isn't showing.");
                            }
                            KeyguardViewMediator.this.doKeyguardLocked(null);
                        } else {
                            if (KeyguardViewMediator.DEBUG_SIM_STATES) {
                                Log.d("KeyguardViewMediator", "PERM_DISABLED, resetStateLocked toshow permanently disabled message in lockscreen.");
                            }
                            KeyguardViewMediator.this.resetStateLocked();
                        }
                        onSimAbsentLocked();
                        break;
                    }
                    break;
                case 7:
                    synchronized (KeyguardViewMediator.this) {
                        KeyguardViewMediator.this.mLockWhenSimRemoved = true;
                        break;
                    }
                    break;
                default:
                    if (KeyguardViewMediator.DEBUG_SIM_STATES) {
                        Log.v("KeyguardViewMediator", "Unspecific state: " + state);
                    }
                    break;
            }
            try {
                int size = KeyguardViewMediator.this.mKeyguardStateCallbacks.size();
                boolean zIsSimPinSecure = KeyguardViewMediator.this.mUpdateMonitor.isSimPinSecure();
                for (int i2 = 0; i2 < size; i2++) {
                    ((IKeyguardStateCallback) KeyguardViewMediator.this.mKeyguardStateCallbacks.get(i2)).onSimSecureStateChanged(zIsSimPinSecure);
                }
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onSimSecureStateChanged", e);
            }
        }

        private void onSimAbsentLocked() {
            if (KeyguardViewMediator.this.isSecure() && KeyguardViewMediator.this.mLockWhenSimRemoved && !KeyguardViewMediator.this.mShuttingDown) {
                KeyguardViewMediator.this.mLockWhenSimRemoved = false;
                MetricsLogger.action(KeyguardViewMediator.this.mContext, 496, KeyguardViewMediator.this.mShowing);
                if (!KeyguardViewMediator.this.mShowing) {
                    Log.i("KeyguardViewMediator", "SIM removed, showing keyguard");
                    KeyguardViewMediator.this.doKeyguardLocked(null);
                }
            }
        }

        @Override
        public void onFingerprintAuthFailed() {
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(currentUser)) {
                KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportFailedFingerprintAttempt(currentUser);
            }
        }

        @Override
        public void onFingerprintAuthenticated(int i) {
            if (KeyguardViewMediator.this.mLockPatternUtils.isSecure(i)) {
                KeyguardViewMediator.this.mLockPatternUtils.getDevicePolicyManager().reportSuccessfulFingerprintAttempt(i);
            }
        }

        @Override
        public void onTrustChanged(int i) {
            if (i == KeyguardUpdateMonitor.getCurrentUser()) {
                synchronized (KeyguardViewMediator.this) {
                    KeyguardViewMediator.this.notifyTrustedChangedLocked(KeyguardViewMediator.this.mUpdateMonitor.getUserHasTrust(i));
                }
            }
        }

        @Override
        public void onHasLockscreenWallpaperChanged(boolean z) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.notifyHasLockscreenWallpaperChanged(z);
            }
        }
    };
    ViewMediatorCallback mViewMediatorCallback = new ViewMediatorCallback() {
        @Override
        public void userActivity() {
            KeyguardViewMediator.this.userActivity();
        }

        @Override
        public void keyguardDone(boolean z, int i) {
            if (i == ActivityManager.getCurrentUser()) {
                KeyguardViewMediator.this.tryKeyguardDone();
            }
        }

        @Override
        public void keyguardDoneDrawing() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDoneDrawing");
            KeyguardViewMediator.this.mHandler.sendEmptyMessage(8);
            Trace.endSection();
        }

        @Override
        public void setNeedsInput(boolean z) {
            KeyguardViewMediator.this.mStatusBarKeyguardViewManager.setNeedsInput(z);
        }

        @Override
        public void keyguardDonePending(boolean z, int i) {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardDonePending");
            if (i == ActivityManager.getCurrentUser()) {
                KeyguardViewMediator.this.mKeyguardDonePending = true;
                KeyguardViewMediator.this.mHideAnimationRun = true;
                KeyguardViewMediator.this.mHideAnimationRunning = true;
                KeyguardViewMediator.this.mStatusBarKeyguardViewManager.startPreHideAnimation(KeyguardViewMediator.this.mHideAnimationFinishedRunnable);
                KeyguardViewMediator.this.mHandler.sendEmptyMessageDelayed(13, 3000L);
                Trace.endSection();
                return;
            }
            Trace.endSection();
        }

        @Override
        public void keyguardGone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#keyguardGone");
            KeyguardViewMediator.this.mKeyguardDisplayManager.hide();
            KeyguardViewMediator.this.mVoiceWakeupManager.notifyKeyguardIsGone();
            Trace.endSection();
        }

        @Override
        public void readyForKeyguardDone() {
            Trace.beginSection("KeyguardViewMediator.mViewMediatorCallback#readyForKeyguardDone");
            if (KeyguardViewMediator.this.mKeyguardDonePending) {
                KeyguardViewMediator.this.mKeyguardDonePending = false;
                KeyguardViewMediator.this.tryKeyguardDone();
            }
            Trace.endSection();
        }

        @Override
        public void resetKeyguard() {
            resetStateLocked();
        }

        @Override
        public void onBouncerVisiblityChanged(boolean z) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.adjustStatusBarLocked(z);
            }
        }

        @Override
        public void playTrustedSound() {
            KeyguardViewMediator.this.playTrustedSound();
        }

        @Override
        public boolean isScreenOn() {
            return KeyguardViewMediator.this.mDeviceInteractive;
        }

        @Override
        public int getBouncerPromptReason() {
            int currentUser = ActivityManager.getCurrentUser();
            boolean zIsTrustUsuallyManaged = KeyguardViewMediator.this.mTrustManager.isTrustUsuallyManaged(currentUser);
            boolean z = zIsTrustUsuallyManaged || KeyguardViewMediator.this.mUpdateMonitor.isUnlockWithFingerprintPossible(currentUser);
            KeyguardUpdateMonitor.StrongAuthTracker strongAuthTracker = KeyguardViewMediator.this.mUpdateMonitor.getStrongAuthTracker();
            int strongAuthForUser = strongAuthTracker.getStrongAuthForUser(currentUser);
            if (z && !strongAuthTracker.hasUserAuthenticatedSinceBoot()) {
                return 1;
            }
            if (z && (strongAuthForUser & 16) != 0) {
                return 2;
            }
            if (z && (strongAuthForUser & 2) != 0) {
                return 3;
            }
            if (!zIsTrustUsuallyManaged || (strongAuthForUser & 4) == 0) {
                return (!z || (strongAuthForUser & 8) == 0) ? 0 : 5;
            }
            return 4;
        }

        @Override
        public CharSequence consumeCustomMessage() {
            CharSequence charSequence = KeyguardViewMediator.this.mCustomMessage;
            KeyguardViewMediator.this.mCustomMessage = null;
            return charSequence;
        }

        @Override
        public void onSecondaryDisplayShowingChanged(int i) {
            synchronized (KeyguardViewMediator.this) {
                KeyguardViewMediator.this.setShowingLocked(KeyguardViewMediator.this.mShowing, KeyguardViewMediator.this.mAodShowing, i, false);
            }
        }

        @Override
        public boolean isShowing() {
            return KeyguardViewMediator.this.isShowing();
        }

        @Override
        public void showLocked(Bundle bundle) {
            KeyguardViewMediator.this.showLocked(bundle);
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
        public boolean isKeyguardDoneOnGoing() {
            return KeyguardViewMediator.this.isKeyguardDoneOnGoing();
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
    };
    private final BroadcastReceiver mDelayedLockBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD".equals(intent.getAction())) {
                int intExtra = intent.getIntExtra("seq", 0);
                if (KeyguardViewMediator.DEBUG) {
                    Log.d("KeyguardViewMediator", "received DELAYED_KEYGUARD_ACTION with seq = " + intExtra + ", mDelayedShowingSequence = " + KeyguardViewMediator.this.mDelayedShowingSequence);
                }
                synchronized (KeyguardViewMediator.this) {
                    if (KeyguardViewMediator.this.mDelayedShowingSequence == intExtra) {
                        KeyguardViewMediator.this.mSuppressNextLockSound = true;
                        KeyguardViewMediator.this.doKeyguardLocked(null);
                    }
                }
                return;
            }
            if ("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK".equals(intent.getAction())) {
                int intExtra2 = intent.getIntExtra("seq", 0);
                int intExtra3 = intent.getIntExtra("android.intent.extra.USER_ID", 0);
                if (intExtra3 != 0) {
                    synchronized (KeyguardViewMediator.this) {
                        if (KeyguardViewMediator.this.mDelayedProfileShowingSequence == intExtra2) {
                            KeyguardViewMediator.this.lockProfile(intExtra3);
                        }
                    }
                }
            }
        }
    };
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
                synchronized (KeyguardViewMediator.this) {
                    KeyguardViewMediator.this.mShuttingDown = true;
                }
            }
        }
    };
    private Handler mHandler = new Handler(Looper.myLooper(), null, true) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    KeyguardViewMediator.this.handleShow((Bundle) message.obj);
                    return;
                case 2:
                    KeyguardViewMediator.this.handleHide();
                    return;
                case 3:
                    KeyguardViewMediator.this.handleReset();
                    return;
                case 4:
                    Trace.beginSection("KeyguardViewMediator#handleMessage VERIFY_UNLOCK");
                    KeyguardViewMediator.this.handleVerifyUnlock();
                    Trace.endSection();
                    return;
                case 5:
                    KeyguardViewMediator.this.handleNotifyFinishedGoingToSleep();
                    return;
                case 6:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNING_ON");
                    KeyguardViewMediator.this.handleNotifyScreenTurningOn((IKeyguardDrawnCallback) message.obj);
                    Trace.endSection();
                    return;
                case 7:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE");
                    KeyguardViewMediator.this.handleKeyguardDone();
                    Trace.endSection();
                    return;
                case 8:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_DRAWING");
                    KeyguardViewMediator.this.handleKeyguardDoneDrawing();
                    Trace.endSection();
                    return;
                case 9:
                    Trace.beginSection("KeyguardViewMediator#handleMessage SET_OCCLUDED");
                    KeyguardViewMediator.this.handleSetOccluded(message.arg1 != 0, message.arg2 != 0);
                    Trace.endSection();
                    return;
                case 10:
                    synchronized (KeyguardViewMediator.this) {
                        KeyguardViewMediator.this.doKeyguardLocked((Bundle) message.obj);
                        break;
                    }
                    return;
                case 11:
                    DismissMessage dismissMessage = (DismissMessage) message.obj;
                    KeyguardViewMediator.this.handleDismiss(dismissMessage.getCallback(), dismissMessage.getMessage());
                    return;
                case 12:
                    Trace.beginSection("KeyguardViewMediator#handleMessage START_KEYGUARD_EXIT_ANIM");
                    StartKeyguardExitAnimParams startKeyguardExitAnimParams = (StartKeyguardExitAnimParams) message.obj;
                    KeyguardViewMediator.this.handleStartKeyguardExitAnimation(startKeyguardExitAnimParams.startTime, startKeyguardExitAnimParams.fadeoutDuration);
                    FalsingManager.getInstance(KeyguardViewMediator.this.mContext).onSucccessfulUnlock();
                    Trace.endSection();
                    return;
                case 13:
                    Trace.beginSection("KeyguardViewMediator#handleMessage KEYGUARD_DONE_PENDING_TIMEOUT");
                    Log.w("KeyguardViewMediator", "Timeout while waiting for activity drawn!");
                    Trace.endSection();
                    return;
                case 14:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_STARTED_WAKING_UP");
                    KeyguardViewMediator.this.handleNotifyStartedWakingUp();
                    Trace.endSection();
                    return;
                case 15:
                    Trace.beginSection("KeyguardViewMediator#handleMessage NOTIFY_SCREEN_TURNED_ON");
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOn();
                    Trace.endSection();
                    return;
                case 16:
                    KeyguardViewMediator.this.handleNotifyScreenTurnedOff();
                    return;
                case 17:
                    KeyguardViewMediator.this.handleNotifyStartedGoingToSleep();
                    return;
                case 18:
                    KeyguardViewMediator.this.handleSystemReady();
                    return;
                case 19:
                    KeyguardViewMediator.this.handleDismissInternal(message.arg1 == 1);
                    return;
                default:
                    return;
            }
        }
    };
    private final Runnable mKeyguardGoingAwayRunnable = new Runnable() {
        @Override
        public void run() {
            Trace.beginSection("KeyguardViewMediator.mKeyGuardGoingAwayRunnable");
            if (KeyguardViewMediator.DEBUG) {
                Log.d("KeyguardViewMediator", "keyguardGoingAway");
            }
            try {
                KeyguardViewMediator.this.mStatusBarKeyguardViewManager.keyguardGoingAway();
                int i = 0;
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.shouldDisableWindowAnimationsForUnlock() || KeyguardViewMediator.this.mWakeAndUnlocking) {
                    i = 2;
                }
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isGoingToNotificationShade()) {
                    i |= 1;
                }
                if (KeyguardViewMediator.this.mStatusBarKeyguardViewManager.isUnlockWithWallpaper()) {
                    i |= 4;
                }
                KeyguardViewMediator.this.mUpdateMonitor.setKeyguardGoingAway(true);
                ActivityManager.getService().keyguardGoingAway(i);
            } catch (RemoteException e) {
                Log.e("KeyguardViewMediator", "Error while calling WindowManager", e);
            }
            Trace.endSection();
        }
    };
    private final Runnable mHideAnimationFinishedRunnable = new Runnable() {
        @Override
        public final void run() {
            KeyguardViewMediator.lambda$new$4(this.f$0);
        }
    };
    private boolean mSuppressNextLockSound = true;

    static class AnonymousClass9 {
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
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.NETWORK_LOCKED.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.PERM_DISABLED.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$IccCardConstants$State[IccCardConstants.State.READY.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
        }
    }

    public void userActivity() {
        this.mPM.userActivity(SystemClock.uptimeMillis(), false);
    }

    boolean mustNotUnlockCurrentUser() {
        return UserManager.isSplitSystemUser() && KeyguardUpdateMonitor.getCurrentUser() == 0;
    }

    private void setupLocked() {
        this.mPM = (PowerManager) this.mContext.getSystemService("power");
        this.mTrustManager = (TrustManager) this.mContext.getSystemService("trust");
        this.mShowKeyguardWakeLock = this.mPM.newWakeLock(1, "show keyguard");
        this.mShowKeyguardWakeLock.setReferenceCounted(false);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        this.mContext.registerReceiver(this.mBroadcastReceiver, intentFilter);
        IntentFilter intentFilter2 = new IntentFilter();
        intentFilter2.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intentFilter2.addAction("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
        this.mContext.registerReceiver(this.mDelayedLockBroadcastReceiver, intentFilter2, "com.android.systemui.permission.SELF", null);
        this.mKeyguardDisplayManager = new KeyguardDisplayManager(this.mContext, this.mViewMediatorCallback);
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        this.mUpdateMonitor = KeyguardUpdateMonitor.getInstance(this.mContext);
        this.mLockPatternUtils = new LockPatternUtils(this.mContext);
        KeyguardUpdateMonitor.setCurrentUser(ActivityManager.getCurrentUser());
        if (this.mContext.getResources().getBoolean(R.bool.config_enableKeyguardService)) {
            setShowingLocked((shouldWaitForProvisioning() || this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) ? false : true, this.mAodShowing, this.mSecondaryDisplayShowing, true);
        } else {
            setShowingLocked(false, this.mAodShowing, this.mSecondaryDisplayShowing, true);
        }
        this.mStatusBarKeyguardViewManager = SystemUIFactory.getInstance().createStatusBarKeyguardViewManager(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        ContentResolver contentResolver = this.mContext.getContentResolver();
        this.mDeviceInteractive = this.mPM.isInteractive();
        this.mLockSounds = new SoundPool(1, 1, 0);
        String string = Settings.Global.getString(contentResolver, "lock_sound");
        if (string != null) {
            this.mLockSoundId = this.mLockSounds.load(string, 1);
        }
        if (string == null || this.mLockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load lock sound from " + string);
        }
        String string2 = Settings.Global.getString(contentResolver, "unlock_sound");
        if (string2 != null) {
            this.mUnlockSoundId = this.mLockSounds.load(string2, 1);
        }
        if (string2 == null || this.mUnlockSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load unlock sound from " + string2);
        }
        String string3 = Settings.Global.getString(contentResolver, "trusted_sound");
        if (string3 != null) {
            this.mTrustedSoundId = this.mLockSounds.load(string3, 1);
        }
        if (string3 == null || this.mTrustedSoundId == 0) {
            Log.w("KeyguardViewMediator", "failed to load trusted sound from " + string3);
        }
        this.mLockSoundVolume = (float) Math.pow(10.0d, this.mContext.getResources().getInteger(android.R.integer.config_datause_throttle_kbitsps) / 20.0f);
        this.mHideAnimation = AnimationUtils.loadAnimation(this.mContext, android.R.anim.ic_signal_wifi_transient_animation_7);
        this.mWorkLockController = new WorkLockActivityController(this.mContext);
        this.mDialogManager = KeyguardDialogManager.getInstance(this.mContext);
        AntiTheftManager.checkPplStatus();
        this.mAntiTheftManager = AntiTheftManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
        this.mAntiTheftManager.doAntiTheftLockCheck();
        this.mVoiceWakeupManager = VoiceWakeupManagerProxy.getInstance();
        this.mVoiceWakeupManager.init(this.mContext, this.mViewMediatorCallback);
        this.mPowerOffAlarmManager = PowerOffAlarmManager.getInstance(this.mContext, this.mViewMediatorCallback, this.mLockPatternUtils);
    }

    @Override
    public void start() {
        KeyguardUtils.getDefault().initSimmePolicy(this.mContext);
        synchronized (this) {
            setupLocked();
        }
        putComponent(KeyguardViewMediator.class, this);
    }

    public void onSystemReady() {
        this.mHandler.obtainMessage(18).sendToTarget();
    }

    private void handleSystemReady() {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "onSystemReady");
            }
            this.mSystemReady = true;
            doKeyguardLocked(null);
            this.mUpdateMonitor.registerCallback(this.mUpdateCallback);
            this.mPowerOffAlarmManager.onSystemReady();
        }
        maybeSendUserPresentBroadcast();
    }

    public void onStartedGoingToSleep(int i) {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "onStartedGoingToSleep(" + i + ")");
        }
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = true;
            int currentUser = KeyguardUpdateMonitor.getCurrentUser();
            boolean z = this.mLockPatternUtils.getPowerButtonInstantlyLocks(currentUser) || !this.mLockPatternUtils.isSecure(currentUser);
            long lockTimeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
            this.mLockLater = false;
            if (this.mExitSecureCallback != null) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "pending exit secure callback cancelled");
                }
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
            } else if ((i == 3 && lockTimeout > 0) || (i == 2 && !z)) {
                doKeyguardLaterLocked(lockTimeout);
                this.mLockLater = true;
            } else if (!this.mLockPatternUtils.isLockScreenDisabled(currentUser)) {
                this.mPendingLock = true;
            }
            if (this.mPendingLock) {
                playSounds(true);
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedGoingToSleep(i);
        notifyStartedGoingToSleep();
    }

    public void onFinishedGoingToSleep(int i, boolean z) {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "onFinishedGoingToSleep(" + i + ")");
        }
        synchronized (this) {
            this.mDeviceInteractive = false;
            this.mGoingToSleep = false;
            this.mWakeAndUnlocking = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            notifyFinishedGoingToSleep();
            if (z) {
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
            if (!this.mLockLater && !z) {
                doKeyguardForChildProfilesLocked();
            }
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchFinishedGoingToSleep(i);
    }

    private long getLockTimeout(int i) {
        long j = Settings.Secure.getInt(this.mContext.getContentResolver(), "lock_screen_lock_after_timeout", 5000);
        long maximumTimeToLock = this.mLockPatternUtils.getDevicePolicyManager().getMaximumTimeToLock(null, i);
        if (maximumTimeToLock > 0) {
            return Math.max(Math.min(maximumTimeToLock - Math.max(Settings.System.getInt(r0, "screen_off_timeout", 30000), 0L), j), 0L);
        }
        return j;
    }

    private void doKeyguardLaterLocked() {
        long lockTimeout = getLockTimeout(KeyguardUpdateMonitor.getCurrentUser());
        if (lockTimeout == 0) {
            doKeyguardLocked(null);
        } else {
            doKeyguardLaterLocked(lockTimeout);
        }
    }

    private void doKeyguardLaterLocked(long j) {
        long jElapsedRealtime = SystemClock.elapsedRealtime() + j;
        Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_KEYGUARD");
        intent.putExtra("seq", this.mDelayedShowingSequence);
        intent.addFlags(268435456);
        this.mAlarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "setting alarm to turn off keyguard, seq = " + this.mDelayedShowingSequence);
        }
        doKeyguardLaterForChildProfilesLocked();
    }

    private void doKeyguardLaterForChildProfilesLocked() {
        for (int i : UserManager.get(this.mContext).getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                long lockTimeout = getLockTimeout(i);
                if (lockTimeout == 0) {
                    doKeyguardForChildProfilesLocked();
                } else {
                    long jElapsedRealtime = SystemClock.elapsedRealtime() + lockTimeout;
                    Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DELAYED_LOCK");
                    intent.putExtra("seq", this.mDelayedProfileShowingSequence);
                    intent.putExtra("android.intent.extra.USER_ID", i);
                    intent.addFlags(268435456);
                    this.mAlarmManager.setExactAndAllowWhileIdle(2, jElapsedRealtime, PendingIntent.getBroadcast(this.mContext, 0, intent, 268435456));
                }
            }
        }
    }

    private void doKeyguardForChildProfilesLocked() {
        for (int i : UserManager.get(this.mContext).getEnabledProfileIds(UserHandle.myUserId())) {
            if (this.mLockPatternUtils.isSeparateProfileChallengeEnabled(i)) {
                lockProfile(i);
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
        Trace.beginSection("KeyguardViewMediator#onStartedWakingUp");
        synchronized (this) {
            this.mDeviceInteractive = true;
            cancelDoKeyguardLaterLocked();
            cancelDoKeyguardForChildProfilesLocked();
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "onStartedWakingUp, seq = " + this.mDelayedShowingSequence);
            }
            notifyStartedWakingUp();
        }
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchStartedWakingUp();
        maybeSendUserPresentBroadcast();
        Trace.endSection();
    }

    public void onScreenTurningOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#onScreenTurningOn");
        notifyScreenOn(iKeyguardDrawnCallback);
        Trace.endSection();
    }

    public void onScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#onScreenTurnedOn");
        notifyScreenTurnedOn();
        this.mUpdateMonitor.dispatchScreenTurnedOn();
        Trace.endSection();
    }

    public void onScreenTurnedOff() {
        notifyScreenTurnedOff();
        this.mUpdateMonitor.dispatchScreenTurnedOff();
    }

    private void maybeSendUserPresentBroadcast() {
        if (this.mSystemReady && this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser())) {
            sendUserPresentBroadcast();
        } else if (this.mSystemReady && shouldWaitForProvisioning()) {
            getLockPatternUtils().userPresent(KeyguardUpdateMonitor.getCurrentUser());
        }
    }

    public void onDreamingStarted() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStarted();
        synchronized (this) {
            if (this.mDeviceInteractive && this.mLockPatternUtils.isSecure(KeyguardUpdateMonitor.getCurrentUser())) {
                doKeyguardLaterLocked();
            }
        }
    }

    public void onDreamingStopped() {
        KeyguardUpdateMonitor.getInstance(this.mContext).dispatchDreamingStopped();
        synchronized (this) {
            if (this.mDeviceInteractive) {
                cancelDoKeyguardLaterLocked();
            }
        }
    }

    public void setKeyguardEnabled(boolean z) {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "setKeyguardEnabled(" + z + ")");
            }
            this.mExternallyEnabled = z;
            if (!z && this.mShowing) {
                if (this.mExitSecureCallback != null) {
                    if (DEBUG) {
                        Log.d("KeyguardViewMediator", "in process of verifyUnlock request, ignoring");
                    }
                } else {
                    if (DEBUG) {
                        Log.d("KeyguardViewMediator", "remembering to reshow, hiding keyguard, disabling status bar expansion");
                    }
                    this.mNeedToReshowWhenReenabled = true;
                    updateInputRestrictedLocked();
                    hideLocked();
                }
            } else if (z && this.mNeedToReshowWhenReenabled) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "previously hidden, reshowing, reenabling status bar expansion");
                }
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestrictedLocked();
                if (this.mExitSecureCallback != null) {
                    if (DEBUG) {
                        Log.d("KeyguardViewMediator", "onKeyguardExitResult(false), resetting");
                    }
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
                    this.mHandler.sendEmptyMessageDelayed(8, 2000L);
                    if (DEBUG) {
                        Log.d("KeyguardViewMediator", "waiting until mWaitingUntilKeyguardVisible is false");
                    }
                    while (this.mWaitingUntilKeyguardVisible) {
                        try {
                            wait();
                        } catch (InterruptedException e2) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    if (DEBUG) {
                        Log.d("KeyguardViewMediator", "done waiting for mWaitingUntilKeyguardVisible");
                    }
                }
            }
        }
    }

    public void verifyUnlock(IKeyguardExitCallback iKeyguardExitCallback) {
        Trace.beginSection("KeyguardViewMediator#verifyUnlock");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "verifyUnlock");
            }
            if (shouldWaitForProvisioning()) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "ignoring because device isn't provisioned");
                }
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e);
                }
            } else if (this.mExternallyEnabled) {
                Log.w("KeyguardViewMediator", "verifyUnlock called when not externally disabled");
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e2) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e2);
                }
            } else if (this.mExitSecureCallback != null) {
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e3) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e3);
                }
            } else if (!isSecure()) {
                this.mExternallyEnabled = true;
                this.mNeedToReshowWhenReenabled = false;
                updateInputRestricted();
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(true);
                } catch (RemoteException e4) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e4);
                }
            } else {
                try {
                    iKeyguardExitCallback.onKeyguardExitResult(false);
                } catch (RemoteException e5) {
                    Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult(false)", e5);
                }
            }
        }
        Trace.endSection();
    }

    public boolean isShowingAndNotOccluded() {
        return this.mShowing && !this.mOccluded;
    }

    public void setOccluded(boolean z, boolean z2) {
        Trace.beginSection("KeyguardViewMediator#setOccluded");
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "setOccluded " + z);
        }
        this.mHandler.removeMessages(9);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(9, z ? 1 : 0, z2 ? 1 : 0));
        Trace.endSection();
    }

    public boolean isHiding() {
        return this.mHiding;
    }

    private void handleSetOccluded(boolean z, boolean z2) {
        Trace.beginSection("KeyguardViewMediator#handleSetOccluded");
        synchronized (this) {
            if (this.mHiding && z) {
                startKeyguardExitAnimation(0L, 0L);
            }
            this.mOccluded = z;
            this.mUpdateMonitor.setKeyguardOccluded(z);
            this.mStatusBarKeyguardViewManager.setOccluded(z, z2 && this.mDeviceInteractive);
            adjustStatusBarLocked();
        }
        Trace.endSection();
    }

    public void doKeyguardTimeout(Bundle bundle) {
        this.mHandler.removeMessages(10);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(10, bundle));
    }

    public boolean isInputRestricted() {
        return this.mShowing || this.mNeedToReshowWhenReenabled;
    }

    private void updateInputRestricted() {
        synchronized (this) {
            updateInputRestrictedLocked();
        }
    }

    private void updateInputRestrictedLocked() {
        boolean zIsInputRestricted = isInputRestricted();
        if (this.mInputRestricted != zIsInputRestricted) {
            this.mInputRestricted = zIsInputRestricted;
            for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
                IKeyguardStateCallback iKeyguardStateCallback = this.mKeyguardStateCallbacks.get(size);
                try {
                    iKeyguardStateCallback.onInputRestrictedStateChanged(zIsInputRestricted);
                } catch (RemoteException e) {
                    Slog.w("KeyguardViewMediator", "Failed to call onDeviceProvisioned", e);
                    if (e instanceof DeadObjectException) {
                        this.mKeyguardStateCallbacks.remove(iKeyguardStateCallback);
                    }
                }
            }
        }
    }

    private void doKeyguardLocked(Bundle bundle) {
        boolean z;
        if (KeyguardUpdateMonitor.CORE_APPS_ONLY) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because booting to cryptkeeper");
                return;
            }
            return;
        }
        if (!this.mExternallyEnabled || PowerOffAlarmManager.isAlarmBoot()) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because externally disabled");
                return;
            }
            return;
        }
        if (this.mStatusBarKeyguardViewManager.isShowing()) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "doKeyguard: not showing because it is already showing");
            }
            resetStateLocked();
            return;
        }
        if (!mustNotUnlockCurrentUser() || !this.mUpdateMonitor.isDeviceProvisioned()) {
            boolean z2 = !SystemProperties.getBoolean("keyguard.no_require_sim", true);
            int i = 0;
            while (true) {
                if (i < KeyguardUtils.getNumOfPhone()) {
                    if (!isSimLockedOrMissing(i, z2)) {
                        i++;
                    } else {
                        z = true;
                        break;
                    }
                } else {
                    z = false;
                    break;
                }
            }
            boolean zIsAntiTheftLocked = AntiTheftManager.isAntiTheftLocked();
            Log.d("KeyguardViewMediator", "lockedOrMissing is " + z + ", requireSim=" + z2 + ", antiTheftLocked=" + zIsAntiTheftLocked);
            if (!z && shouldWaitForProvisioning() && !zIsAntiTheftLocked) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "doKeyguard: not showing because device isn't provisioned and the sim is not locked or missing");
                    return;
                }
                return;
            }
            boolean z3 = bundle != null && bundle.getBoolean("force_show", false);
            if (this.mLockPatternUtils.isLockScreenDisabled(KeyguardUpdateMonitor.getCurrentUser()) && !z && !z3 && !zIsAntiTheftLocked) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "doKeyguard: not showing because lockscreen is off");
                    return;
                }
                return;
            } else if (this.mLockPatternUtils.checkVoldPassword(KeyguardUpdateMonitor.getCurrentUser()) && KeyguardUtils.isSystemEncrypted()) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "Not showing lock screen since just decrypted");
                }
                setShowingLocked(false, this.mAodShowing);
                hideLocked();
                return;
            }
        }
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "doKeyguard: showing the lock screen");
        }
        showLocked(bundle);
    }

    private void lockProfile(int i) {
        this.mTrustManager.setDeviceLockedForUser(i, true);
    }

    private boolean shouldWaitForProvisioning() {
        return (this.mUpdateMonitor.isDeviceProvisioned() || isSecure()) ? false : true;
    }

    private void handleDismiss(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        if (this.mShowing) {
            if (iKeyguardDismissCallback != null) {
                this.mDismissCallbackRegistry.addCallback(iKeyguardDismissCallback);
            }
            this.mCustomMessage = charSequence;
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
            return;
        }
        if (iKeyguardDismissCallback != null) {
            new DismissCallbackWrapper(iKeyguardDismissCallback).notifyDismissError();
        }
    }

    public void dismiss(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
        this.mHandler.obtainMessage(11, new DismissMessage(iKeyguardDismissCallback, charSequence)).sendToTarget();
    }

    private void resetStateLocked() {
        if (DEBUG) {
            Log.e("KeyguardViewMediator", "resetStateLocked");
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(3));
    }

    private void notifyStartedGoingToSleep() {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyStartedGoingToSleep");
        }
        this.mHandler.sendEmptyMessage(17);
    }

    private void notifyFinishedGoingToSleep() {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyFinishedGoingToSleep");
        }
        this.mHandler.sendEmptyMessage(5);
    }

    private void notifyStartedWakingUp() {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyStartedWakingUp");
        }
        this.mHandler.sendEmptyMessage(14);
    }

    private void notifyScreenOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyScreenOn");
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(6, iKeyguardDrawnCallback));
    }

    private void notifyScreenTurnedOn() {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyScreenTurnedOn");
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(15));
    }

    private void notifyScreenTurnedOff() {
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "notifyScreenTurnedOff");
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(16));
    }

    private void showLocked(Bundle bundle) {
        Trace.beginSection("KeyguardViewMediator#showLocked aqcuiring mShowKeyguardWakeLock");
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "showLocked");
        }
        this.mShowKeyguardWakeLock.acquire();
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, bundle));
        Trace.endSection();
    }

    private void hideLocked() {
        Trace.beginSection("KeyguardViewMediator#hideLocked");
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "hideLocked");
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(2));
        Trace.endSection();
    }

    public boolean isSecure() {
        return isSecure(KeyguardUpdateMonitor.getCurrentUser());
    }

    public boolean isSecure(int i) {
        return this.mLockPatternUtils.isSecure(i) || KeyguardUpdateMonitor.getInstance(this.mContext).isSimPinSecure() || AntiTheftManager.isAntiTheftLocked();
    }

    public void setSwitchingUser(boolean z) {
        KeyguardUpdateMonitor.getInstance(this.mContext).setSwitchingUser(z);
    }

    public void setCurrentUser(int i) {
        KeyguardUpdateMonitor.setCurrentUser(i);
        synchronized (this) {
            notifyTrustedChangedLocked(this.mUpdateMonitor.getUserHasTrust(i));
        }
    }

    public void keyguardDone() {
        Trace.beginSection("KeyguardViewMediator#keyguardDone");
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "keyguardDone()");
        }
        userActivity();
        EventLog.writeEvent(70000, 2);
        this.mHandler.sendMessage(this.mHandler.obtainMessage(7));
        Trace.endSection();
    }

    private void tryKeyguardDone() {
        if (!this.mKeyguardDonePending && this.mHideAnimationRun && !this.mHideAnimationRunning) {
            handleKeyguardDone();
        } else if (!this.mHideAnimationRun) {
            this.mHideAnimationRun = true;
            this.mHideAnimationRunning = true;
            this.mStatusBarKeyguardViewManager.startPreHideAnimation(this.mHideAnimationFinishedRunnable);
        }
    }

    private void handleKeyguardDone() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDone");
        final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                KeyguardViewMediator.lambda$handleKeyguardDone$0(this.f$0, currentUser);
            }
        });
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "handleKeyguardDone");
        }
        synchronized (this) {
            resetKeyguardDonePendingLocked();
        }
        if (AntiTheftManager.isAntiTheftLocked()) {
            Log.d("KeyguardViewMediator", "handleKeyguardDone() - Skip keyguard done! antitheft = true or sim = " + this.mUpdateMonitor.isSimPinSecure());
            return;
        }
        mKeyguardDoneOnGoing = true;
        this.mUpdateMonitor.clearFailedUnlockAttempts();
        this.mUpdateMonitor.clearFingerprintRecognized();
        if (this.mGoingToSleep) {
            Log.i("KeyguardViewMediator", "Device is going to sleep, aborting keyguardDone");
            return;
        }
        if (this.mExitSecureCallback != null) {
            try {
                this.mExitSecureCallback.onKeyguardExitResult(true);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onKeyguardExitResult()", e);
            }
            this.mExitSecureCallback = null;
            this.mExternallyEnabled = true;
            this.mNeedToReshowWhenReenabled = false;
            updateInputRestricted();
        }
        this.mSuppressNextLockSound = false;
        handleHide();
        Trace.endSection();
    }

    public static void lambda$handleKeyguardDone$0(KeyguardViewMediator keyguardViewMediator, int i) {
        if (keyguardViewMediator.mLockPatternUtils.isSecure(i)) {
            keyguardViewMediator.mLockPatternUtils.getDevicePolicyManager().reportKeyguardDismissed(i);
        }
    }

    private void sendUserPresentBroadcast() {
        synchronized (this) {
            if (this.mBootCompleted) {
                final int currentUser = KeyguardUpdateMonitor.getCurrentUser();
                final UserHandle userHandle = new UserHandle(currentUser);
                final UserManager userManager = (UserManager) this.mContext.getSystemService("user");
                this.mUiOffloadThread.submit(new Runnable() {
                    @Override
                    public final void run() {
                        KeyguardViewMediator.lambda$sendUserPresentBroadcast$1(this.f$0, userManager, userHandle, currentUser);
                    }
                });
            } else {
                this.mBootSendUserPresent = true;
            }
        }
    }

    public static void lambda$sendUserPresentBroadcast$1(KeyguardViewMediator keyguardViewMediator, UserManager userManager, UserHandle userHandle, int i) {
        for (int i2 : userManager.getProfileIdsWithDisabled(userHandle.getIdentifier())) {
            keyguardViewMediator.mContext.sendBroadcastAsUser(USER_PRESENT_INTENT, UserHandle.of(i2));
        }
        keyguardViewMediator.getLockPatternUtils().userPresent(i);
    }

    private void handleKeyguardDoneDrawing() {
        Trace.beginSection("KeyguardViewMediator#handleKeyguardDoneDrawing");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing");
            }
            if (this.mWaitingUntilKeyguardVisible) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "handleKeyguardDoneDrawing: notifying mWaitingUntilKeyguardVisible");
                }
                this.mWaitingUntilKeyguardVisible = false;
                notifyAll();
                this.mHandler.removeMessages(8);
            }
        }
        Trace.endSection();
    }

    private void playSounds(boolean z) {
        Log.d("KeyguardViewMediator", "playSounds(locked = " + z + "), mSuppressNextLockSound =" + this.mSuppressNextLockSound);
        if (this.mSuppressNextLockSound) {
            this.mSuppressNextLockSound = false;
        } else {
            playSound(z ? this.mLockSoundId : this.mUnlockSoundId);
        }
    }

    private void playSound(final int i) {
        if (i != 0 && Settings.System.getInt(this.mContext.getContentResolver(), "lockscreen_sounds_enabled", 1) == 1) {
            this.mLockSounds.stop(this.mLockSoundStreamId);
            if (this.mAudioManager == null) {
                this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
                if (this.mAudioManager == null) {
                    return;
                } else {
                    this.mUiSoundsStreamType = this.mAudioManager.getUiSoundsStreamType();
                }
            }
            this.mUiOffloadThread.submit(new Runnable() {
                @Override
                public final void run() {
                    KeyguardViewMediator.lambda$playSound$2(this.f$0, i);
                }
            });
        }
    }

    public static void lambda$playSound$2(KeyguardViewMediator keyguardViewMediator, int i) {
        if (keyguardViewMediator.mAudioManager.isStreamMute(keyguardViewMediator.mUiSoundsStreamType)) {
            return;
        }
        int iPlay = keyguardViewMediator.mLockSounds.play(i, keyguardViewMediator.mLockSoundVolume, keyguardViewMediator.mLockSoundVolume, 1, 0, 1.0f);
        synchronized (keyguardViewMediator) {
            keyguardViewMediator.mLockSoundStreamId = iPlay;
        }
    }

    private void playTrustedSound() {
        if (this.mSuppressNextLockSound) {
            return;
        }
        playSound(this.mTrustedSoundId);
    }

    private void updateActivityLockScreenState(final boolean z, final boolean z2, final int i) {
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                ActivityManager.getService().setLockScreenShown(z, z2, i);
            }
        });
    }

    private void handleShow(Bundle bundle) {
        Trace.beginSection("KeyguardViewMediator#handleShow");
        int currentUser = KeyguardUpdateMonitor.getCurrentUser();
        if (this.mLockPatternUtils.isSecure(currentUser)) {
            this.mLockPatternUtils.getDevicePolicyManager().reportKeyguardSecured(currentUser);
        }
        synchronized (this) {
            if (!this.mSystemReady) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "ignoring handleShow because system is not ready.");
                }
                return;
            }
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleShow");
            }
            setShowingLocked(true, this.mAodShowing);
            this.mStatusBarKeyguardViewManager.show(bundle);
            this.mHiding = false;
            this.mWakeAndUnlocking = false;
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            adjustStatusBarLocked();
            userActivity();
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ActivityManager.isSystemReady()) {
                        try {
                            ActivityManager.getService().closeSystemDialogs("lock");
                        } catch (RemoteException e) {
                        }
                    }
                }
            }, 500L);
            if (PowerOffAlarmManager.isAlarmBoot()) {
                this.mPowerOffAlarmManager.startAlarm();
            }
            this.mShowKeyguardWakeLock.release();
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleShow exit");
            }
            if (this.mKeyguardDisplayManager != null) {
                Log.d("KeyguardViewMediator", "handle show call mKeyguardDisplayManager.show()");
                this.mKeyguardDisplayManager.show();
            } else {
                Log.d("KeyguardViewMediator", "handle show mKeyguardDisplayManager is null");
            }
            Trace.endSection();
        }
    }

    public static void lambda$new$4(KeyguardViewMediator keyguardViewMediator) {
        keyguardViewMediator.mHideAnimationRunning = false;
        keyguardViewMediator.tryKeyguardDone();
    }

    private void handleHide() {
        Trace.beginSection("KeyguardViewMediator#handleHide");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleHide");
            }
            if (mustNotUnlockCurrentUser()) {
                if (DEBUG) {
                    Log.d("KeyguardViewMediator", "Split system user, quit unlocking.");
                }
                return;
            }
            this.mHiding = true;
            if (this.mShowing && !this.mOccluded) {
                this.mKeyguardGoingAwayRunnable.run();
            } else {
                handleStartKeyguardExitAnimation(SystemClock.uptimeMillis() + this.mHideAnimation.getStartOffset(), this.mHideAnimation.getDuration());
            }
            Trace.endSection();
        }
    }

    private void handleStartKeyguardExitAnimation(long j, long j2) {
        Trace.beginSection("KeyguardViewMediator#handleStartKeyguardExitAnimation");
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "handleStartKeyguardExitAnimation startTime=" + j + " fadeoutDuration=" + j2);
        }
        synchronized (this) {
            if (!this.mHiding) {
                setShowingLocked(this.mShowing, this.mAodShowing, this.mSecondaryDisplayShowing, true);
                return;
            }
            this.mHiding = false;
            if (this.mWakeAndUnlocking && this.mDrawnCallback != null) {
                this.mStatusBarKeyguardViewManager.getViewRootImpl().setReportNextDraw();
                notifyDrawn(this.mDrawnCallback);
                this.mDrawnCallback = null;
            }
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(this.mPhoneState) && this.mShowing) {
                playSounds(false);
            }
            this.mWakeAndUnlocking = false;
            setShowingLocked(false, this.mAodShowing);
            this.mDismissCallbackRegistry.notifyDismissSucceeded();
            this.mStatusBarKeyguardViewManager.hide(j, j2);
            resetKeyguardDonePendingLocked();
            this.mHideAnimationRun = false;
            adjustStatusBarLocked();
            sendUserPresentBroadcast();
            this.mUpdateMonitor.setKeyguardGoingAway(false);
            Log.d("KeyguardViewMediator", "set mKeyguardDoneOnGoing = false");
            mKeyguardDoneOnGoing = false;
            Trace.endSection();
        }
    }

    private void adjustStatusBarLocked() {
        adjustStatusBarLocked(false);
    }

    private void adjustStatusBarLocked(boolean z) {
        if (this.mStatusBarManager == null) {
            this.mStatusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
        }
        if (this.mStatusBarManager == null) {
            Log.w("KeyguardViewMediator", "Could not get status bar manager");
            return;
        }
        int i = 0;
        if (this.mShowing && PowerOffAlarmManager.isAlarmBoot()) {
            i = 33554432;
        }
        if (z || isShowingAndNotOccluded()) {
            i |= 18874368;
        }
        if (DEBUG) {
            Log.d("KeyguardViewMediator", "adjustStatusBarLocked: mShowing=" + this.mShowing + " mOccluded=" + this.mOccluded + " isSecure=" + isSecure() + " force=" + z + " --> flags=0x" + Integer.toHexString(i));
        }
        this.mStatusBarManager.disable(i);
    }

    private void handleReset() {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleReset");
            }
            this.mStatusBarKeyguardViewManager.reset(true);
            adjustStatusBarLocked();
        }
    }

    private void handleVerifyUnlock() {
        Trace.beginSection("KeyguardViewMediator#handleVerifyUnlock");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleVerifyUnlock");
            }
            setShowingLocked(true, this.mAodShowing);
            this.mStatusBarKeyguardViewManager.dismissAndCollapse();
        }
        Trace.endSection();
    }

    private void handleNotifyStartedGoingToSleep() {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyStartedGoingToSleep");
            }
            this.mStatusBarKeyguardViewManager.onStartedGoingToSleep();
        }
    }

    private void handleNotifyFinishedGoingToSleep() {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyFinishedGoingToSleep");
            }
            this.mStatusBarKeyguardViewManager.onFinishedGoingToSleep();
        }
    }

    private void handleNotifyStartedWakingUp() {
        Trace.beginSection("KeyguardViewMediator#handleMotifyStartedWakingUp");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyWakingUp");
            }
            this.mStatusBarKeyguardViewManager.onStartedWakingUp();
        }
        Trace.endSection();
    }

    private void handleNotifyScreenTurningOn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurningOn");
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyScreenTurningOn");
            }
            this.mStatusBarKeyguardViewManager.onScreenTurningOn();
            if (iKeyguardDrawnCallback != null) {
                if (this.mWakeAndUnlocking) {
                    this.mDrawnCallback = iKeyguardDrawnCallback;
                } else {
                    notifyDrawn(iKeyguardDrawnCallback);
                }
            }
        }
        Trace.endSection();
    }

    private void handleNotifyScreenTurnedOn() {
        Trace.beginSection("KeyguardViewMediator#handleNotifyScreenTurnedOn");
        if (LatencyTracker.isEnabled(this.mContext)) {
            LatencyTracker.getInstance(this.mContext).onActionEnd(5);
        }
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOn");
            }
            this.mStatusBarKeyguardViewManager.onScreenTurnedOn();
        }
        Trace.endSection();
    }

    private void handleNotifyScreenTurnedOff() {
        synchronized (this) {
            if (DEBUG) {
                Log.d("KeyguardViewMediator", "handleNotifyScreenTurnedOff");
            }
            this.mStatusBarKeyguardViewManager.onScreenTurnedOff();
            this.mDrawnCallback = null;
        }
    }

    private void notifyDrawn(IKeyguardDrawnCallback iKeyguardDrawnCallback) {
        Trace.beginSection("KeyguardViewMediator#notifyDrawn");
        try {
            iKeyguardDrawnCallback.onDrawn();
        } catch (RemoteException e) {
            Slog.w("KeyguardViewMediator", "Exception calling onDrawn():", e);
        }
        Trace.endSection();
    }

    private void resetKeyguardDonePendingLocked() {
        this.mKeyguardDonePending = false;
        this.mHandler.removeMessages(13);
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
        Trace.beginSection("KeyguardViewMediator#onWakeAndUnlocking");
        this.mWakeAndUnlocking = true;
        keyguardDone();
        Trace.endSection();
    }

    public StatusBarKeyguardViewManager registerStatusBar(StatusBar statusBar, ViewGroup viewGroup, NotificationPanelView notificationPanelView, FingerprintUnlockController fingerprintUnlockController) {
        this.mStatusBarKeyguardViewManager.registerStatusBar(statusBar, viewGroup, notificationPanelView, fingerprintUnlockController, this.mDismissCallbackRegistry);
        return this.mStatusBarKeyguardViewManager;
    }

    public void startKeyguardExitAnimation(long j, long j2) {
        Trace.beginSection("KeyguardViewMediator#startKeyguardExitAnimation");
        this.mHandler.sendMessage(this.mHandler.obtainMessage(12, new StartKeyguardExitAnimParams(j, j2)));
        Trace.endSection();
    }

    public void onShortPowerPressedGoHome() {
    }

    public ViewMediatorCallback getViewMediatorCallback() {
        return this.mViewMediatorCallback;
    }

    public LockPatternUtils getLockPatternUtils() {
        return this.mLockPatternUtils;
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.print("  mSystemReady: ");
        printWriter.println(this.mSystemReady);
        printWriter.print("  mBootCompleted: ");
        printWriter.println(this.mBootCompleted);
        printWriter.print("  mBootSendUserPresent: ");
        printWriter.println(this.mBootSendUserPresent);
        printWriter.print("  mExternallyEnabled: ");
        printWriter.println(this.mExternallyEnabled);
        printWriter.print("  mShuttingDown: ");
        printWriter.println(this.mShuttingDown);
        printWriter.print("  mNeedToReshowWhenReenabled: ");
        printWriter.println(this.mNeedToReshowWhenReenabled);
        printWriter.print("  mShowing: ");
        printWriter.println(this.mShowing);
        printWriter.print("  mInputRestricted: ");
        printWriter.println(this.mInputRestricted);
        printWriter.print("  mOccluded: ");
        printWriter.println(this.mOccluded);
        printWriter.print("  mDelayedShowingSequence: ");
        printWriter.println(this.mDelayedShowingSequence);
        printWriter.print("  mExitSecureCallback: ");
        printWriter.println(this.mExitSecureCallback);
        printWriter.print("  mDeviceInteractive: ");
        printWriter.println(this.mDeviceInteractive);
        printWriter.print("  mGoingToSleep: ");
        printWriter.println(this.mGoingToSleep);
        printWriter.print("  mHiding: ");
        printWriter.println(this.mHiding);
        printWriter.print("  mWaitingUntilKeyguardVisible: ");
        printWriter.println(this.mWaitingUntilKeyguardVisible);
        printWriter.print("  mKeyguardDonePending: ");
        printWriter.println(this.mKeyguardDonePending);
        printWriter.print("  mHideAnimationRun: ");
        printWriter.println(this.mHideAnimationRun);
        printWriter.print("  mPendingReset: ");
        printWriter.println(this.mPendingReset);
        printWriter.print("  mPendingLock: ");
        printWriter.println(this.mPendingLock);
        printWriter.print("  mWakeAndUnlocking: ");
        printWriter.println(this.mWakeAndUnlocking);
        printWriter.print("  mDrawnCallback: ");
        printWriter.println(this.mDrawnCallback);
    }

    public void setAodShowing(boolean z) {
        setShowingLocked(this.mShowing, z);
    }

    private static class StartKeyguardExitAnimParams {
        long fadeoutDuration;
        long startTime;

        private StartKeyguardExitAnimParams(long j, long j2) {
            this.startTime = j;
            this.fadeoutDuration = j2;
        }
    }

    private void setShowingLocked(boolean z, boolean z2) {
        setShowingLocked(z, z2, this.mSecondaryDisplayShowing, false);
    }

    private void setShowingLocked(boolean z, boolean z2, int i, boolean z3) {
        boolean z4 = (z == this.mShowing && z2 == this.mAodShowing && !z3) ? false : true;
        if (z4 || i != this.mSecondaryDisplayShowing) {
            this.mShowing = z;
            this.mAodShowing = z2;
            this.mSecondaryDisplayShowing = i;
            if (z4) {
                notifyDefaultDisplayCallbacks(z);
            }
            updateActivityLockScreenState(z, z2, i);
        }
    }

    private void notifyDefaultDisplayCallbacks(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            IKeyguardStateCallback iKeyguardStateCallback = this.mKeyguardStateCallbacks.get(size);
            try {
                iKeyguardStateCallback.onShowingStateChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onShowingStateChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(iKeyguardStateCallback);
                }
            }
        }
        updateInputRestrictedLocked();
        this.mUiOffloadThread.submit(new Runnable() {
            @Override
            public final void run() {
                this.f$0.mTrustManager.reportKeyguardShowingChanged();
            }
        });
    }

    private void notifyTrustedChangedLocked(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            try {
                this.mKeyguardStateCallbacks.get(size).onTrustedChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call notifyTrustedChangedLocked", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(size);
                }
            }
        }
    }

    private void notifyHasLockscreenWallpaperChanged(boolean z) {
        for (int size = this.mKeyguardStateCallbacks.size() - 1; size >= 0; size--) {
            try {
                this.mKeyguardStateCallbacks.get(size).onHasLockscreenWallpaperChanged(z);
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call onHasLockscreenWallpaperChanged", e);
                if (e instanceof DeadObjectException) {
                    this.mKeyguardStateCallbacks.remove(size);
                }
            }
        }
    }

    public void addStateMonitorCallback(IKeyguardStateCallback iKeyguardStateCallback) {
        synchronized (this) {
            this.mKeyguardStateCallbacks.add(iKeyguardStateCallback);
            try {
                iKeyguardStateCallback.onSimSecureStateChanged(this.mUpdateMonitor.isSimPinSecure());
                iKeyguardStateCallback.onShowingStateChanged(this.mShowing);
                iKeyguardStateCallback.onInputRestrictedStateChanged(this.mInputRestricted);
                iKeyguardStateCallback.onTrustedChanged(this.mUpdateMonitor.getUserHasTrust(KeyguardUpdateMonitor.getCurrentUser()));
                iKeyguardStateCallback.onHasLockscreenWallpaperChanged(this.mUpdateMonitor.hasLockscreenWallpaper());
            } catch (RemoteException e) {
                Slog.w("KeyguardViewMediator", "Failed to call to IKeyguardStateCallback", e);
            }
        }
    }

    private static class DismissMessage {
        private final IKeyguardDismissCallback mCallback;
        private final CharSequence mMessage;

        DismissMessage(IKeyguardDismissCallback iKeyguardDismissCallback, CharSequence charSequence) {
            this.mCallback = iKeyguardDismissCallback;
            this.mMessage = charSequence;
        }

        public IKeyguardDismissCallback getCallback() {
            return this.mCallback;
        }

        public CharSequence getMessage() {
            return this.mMessage;
        }
    }

    private void removeKeyguardDoneMsg() {
        this.mHandler.removeMessages(7);
    }

    private class InvalidDialogCallback implements KeyguardDialogManager.DialogShowCallBack {
        private InvalidDialogCallback() {
        }

        @Override
        public void show() {
            KeyguardViewMediator.this.createDialog(KeyguardViewMediator.this.mContext.getString(R.string.invalid_sim_title), KeyguardViewMediator.this.mContext.getString(R.string.invalid_sim_message)).show();
        }
    }

    private class MeLockedDialogCallback implements KeyguardDialogManager.DialogShowCallBack {
        private MeLockedDialogCallback() {
        }

        @Override
        public void show() {
            KeyguardViewMediator.this.createDialog(null, KeyguardViewMediator.this.mContext.getString(R.string.simlock_slot_locked_message)).show();
        }
    }

    private AlertDialog createDialog(String str, String str2) {
        AlertDialog alertDialogCreate = new AlertDialog.Builder(this.mContext).setTitle(str).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(false).setMessage(str2).setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                KeyguardViewMediator.this.mDialogManager.reportDialogClose();
                Log.d("KeyguardViewMediator", "invalid sim card ,reportCloseDialog");
            }
        }).create();
        alertDialogCreate.getWindow().setType(2003);
        return alertDialogCreate;
    }

    public boolean isKeyguardDoneOnGoing() {
        return mKeyguardDoneOnGoing;
    }

    public boolean isShowing() {
        return this.mShowing;
    }

    private boolean isSimLockedOrMissing(int i, boolean z) {
        IccCardConstants.State simStateOfPhoneId = this.mUpdateMonitor.getSimStateOfPhoneId(i);
        return this.mUpdateMonitor.isSimPinSecure(i) || ((simStateOfPhoneId == IccCardConstants.State.ABSENT || simStateOfPhoneId == IccCardConstants.State.PERM_DISABLED) && z);
    }

    public void handleDismissInternal(boolean z) {
        if (this.mShowing) {
            if (z || !this.mOccluded) {
                this.mStatusBarKeyguardViewManager.dismiss(z);
            }
        }
    }

    void setSuppressPlaySoundFlag() {
        this.mSuppressNextLockSound = true;
    }

    void updateNavbarStatus() {
        Log.d("KeyguardViewMediator", "updateNavbarStatus() is called.");
        this.mStatusBarKeyguardViewManager.updateStates();
    }
}
