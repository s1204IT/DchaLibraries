package com.android.server.power;

import android.R;
import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.app.trust.TrustManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManagerInternal;
import android.media.AudioAttributes;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.metrics.LogMaker;
import android.net.Uri;
import android.net.util.NetworkConstants;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.EventLog;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.app.IBatteryStats;
import com.android.internal.logging.MetricsLogger;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;
import com.android.server.hdmi.HdmiCecKeycode;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;

final class Notifier {
    private static final boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_ASLEEP = 2;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_UNKNOWN = 0;
    private static final int MSG_BROADCAST = 2;
    private static final int MSG_PROFILE_TIMED_OUT = 5;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_CHANGED = 4;
    private static final int MSG_USER_ACTIVITY = 1;
    private static final int MSG_WIRED_CHARGING_STARTED = 6;
    private static final int MSG_WIRELESS_CHARGING_STARTED = 3;
    private static final String TAG = "PowerManagerNotifier";
    private final AppOpsManager mAppOps;
    private final IBatteryStats mBatteryStats;
    private boolean mBroadcastInProgress;
    private long mBroadcastStartTime;
    private int mBroadcastedInteractiveState;
    private final Context mContext;
    private final NotifierHandler mHandler;
    private int mInteractiveChangeReason;
    private boolean mInteractiveChanging;
    private boolean mPendingGoToSleepBroadcast;
    private int mPendingInteractiveState;
    private boolean mPendingWakeUpBroadcast;
    private final WindowManagerPolicy mPolicy;
    private final Intent mScreenBrightnessBoostIntent;
    private final Intent mScreenOffIntent;
    private final SuspendBlocker mSuspendBlocker;
    private final boolean mSuspendWhenScreenOffDueToProximityConfig;
    private final TrustManager mTrustManager;
    private boolean mUserActivityPending;
    private final Vibrator mVibrator;
    private static final long[] WIRELESS_VIBRATION_TIME = {40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40};
    private static final int[] WIRELESS_VIBRATION_AMPLITUDE = {1, 4, 11, 25, 44, 67, 91, 114, 123, HdmiCecKeycode.CEC_KEYCODE_TUNE_FUNCTION, 79, 55, 34, 17, 7, 2};
    private static final VibrationEffect WIRELESS_CHARGING_VIBRATION_EFFECT = VibrationEffect.createWaveform(WIRELESS_VIBRATION_TIME, WIRELESS_VIBRATION_AMPLITUDE, -1);
    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder().setContentType(4).build();
    private final Object mLock = new Object();
    private boolean mInteractive = true;
    private final BroadcastReceiver mScreeBrightnessBoostChangedDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Notifier.this.mSuspendBlocker.release();
        }
    };
    private final BroadcastReceiver mWakeUpBroadcastDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, 1, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final BroadcastReceiver mGoToSleepBroadcastDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, 0, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
    private final InputMethodManagerInternal mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
    private final StatusBarManagerInternal mStatusBarManagerInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
    private final Intent mScreenOnIntent = new Intent("android.intent.action.SCREEN_ON");

    public Notifier(Looper looper, Context context, IBatteryStats iBatteryStats, SuspendBlocker suspendBlocker, WindowManagerPolicy windowManagerPolicy) {
        this.mContext = context;
        this.mBatteryStats = iBatteryStats;
        this.mAppOps = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mSuspendBlocker = suspendBlocker;
        this.mPolicy = windowManagerPolicy;
        this.mTrustManager = (TrustManager) this.mContext.getSystemService(TrustManager.class);
        this.mVibrator = (Vibrator) this.mContext.getSystemService(Vibrator.class);
        this.mHandler = new NotifierHandler(looper);
        this.mScreenOnIntent.addFlags(1344274432);
        this.mScreenOffIntent = new Intent("android.intent.action.SCREEN_OFF");
        this.mScreenOffIntent.addFlags(1344274432);
        this.mScreenBrightnessBoostIntent = new Intent("android.os.action.SCREEN_BRIGHTNESS_BOOST_CHANGED");
        this.mScreenBrightnessBoostIntent.addFlags(1342177280);
        this.mSuspendWhenScreenOffDueToProximityConfig = context.getResources().getBoolean(R.^attr-private.pointerIconCrosshair);
        try {
            this.mBatteryStats.noteInteractive(true);
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockAcquired(int i, String str, String str2, int i2, int i3, WorkSource workSource, String str3) {
        int batteryStatsWakeLockMonitorType = getBatteryStatsWakeLockMonitorType(i);
        if (batteryStatsWakeLockMonitorType >= 0) {
            boolean z = i2 == 1000 && (i & 1073741824) != 0;
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStartWakelockFromSource(workSource, i3, str, str3, batteryStatsWakeLockMonitorType, z);
                } else {
                    this.mBatteryStats.noteStartWakelock(i2, i3, str, str3, batteryStatsWakeLockMonitorType, z);
                    this.mAppOps.startOpNoThrow(40, i2, str2);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void onLongPartialWakeLockStart(String str, int i, WorkSource workSource, String str2) {
        try {
            if (workSource != null) {
                this.mBatteryStats.noteLongPartialWakelockStartFromSource(str, str2, workSource);
            } else {
                this.mBatteryStats.noteLongPartialWakelockStart(str, str2, i);
            }
        } catch (RemoteException e) {
        }
    }

    public void onLongPartialWakeLockFinish(String str, int i, WorkSource workSource, String str2) {
        try {
            if (workSource != null) {
                this.mBatteryStats.noteLongPartialWakelockFinishFromSource(str, str2, workSource);
            } else {
                this.mBatteryStats.noteLongPartialWakelockFinish(str, str2, i);
            }
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockChanging(int i, String str, String str2, int i2, int i3, WorkSource workSource, String str3, int i4, String str4, String str5, int i5, int i6, WorkSource workSource2, String str6) {
        int batteryStatsWakeLockMonitorType = getBatteryStatsWakeLockMonitorType(i);
        int batteryStatsWakeLockMonitorType2 = getBatteryStatsWakeLockMonitorType(i4);
        if (workSource != null && workSource2 != null && batteryStatsWakeLockMonitorType >= 0 && batteryStatsWakeLockMonitorType2 >= 0) {
            try {
                this.mBatteryStats.noteChangeWakelockFromSource(workSource, i3, str, str3, batteryStatsWakeLockMonitorType, workSource2, i6, str4, str6, batteryStatsWakeLockMonitorType2, i5 == 1000 && (i4 & 1073741824) != 0);
            } catch (RemoteException e) {
            }
        } else {
            onWakeLockReleased(i, str, str2, i2, i3, workSource, str3);
            onWakeLockAcquired(i4, str4, str5, i5, i6, workSource2, str6);
        }
    }

    public void onWakeLockReleased(int i, String str, String str2, int i2, int i3, WorkSource workSource, String str3) {
        int batteryStatsWakeLockMonitorType = getBatteryStatsWakeLockMonitorType(i);
        if (batteryStatsWakeLockMonitorType >= 0) {
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStopWakelockFromSource(workSource, i3, str, str3, batteryStatsWakeLockMonitorType);
                } else {
                    this.mBatteryStats.noteStopWakelock(i2, i3, str, str3, batteryStatsWakeLockMonitorType);
                    this.mAppOps.finishOp(40, i2, str2);
                }
            } catch (RemoteException e) {
            }
        }
    }

    private int getBatteryStatsWakeLockMonitorType(int i) {
        int i2 = i & NetworkConstants.ARP_HWTYPE_RESERVED_HI;
        if (i2 == 1) {
            return 0;
        }
        if (i2 == 6 || i2 == 10) {
            return 1;
        }
        return i2 != 32 ? (i2 == 64 || i2 != 128) ? -1 : 18 : this.mSuspendWhenScreenOffDueToProximityConfig ? -1 : 0;
    }

    public void onWakefulnessChangeStarted(final int i, int i2) {
        boolean zIsInteractive = PowerManagerInternal.isInteractive(i);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Notifier.this.mActivityManagerInternal.onWakefulnessChanged(i);
            }
        });
        if (this.mInteractive != zIsInteractive) {
            if (this.mInteractiveChanging) {
                handleLateInteractiveChange();
            }
            this.mInputManagerInternal.setInteractive(zIsInteractive);
            this.mInputMethodManagerInternal.setInteractive(zIsInteractive);
            try {
                this.mBatteryStats.noteInteractive(zIsInteractive);
            } catch (RemoteException e) {
            }
            this.mInteractive = zIsInteractive;
            this.mInteractiveChangeReason = i2;
            this.mInteractiveChanging = true;
            handleEarlyInteractiveChange();
        }
    }

    public void onWakefulnessChangeFinished() {
        if (this.mInteractiveChanging) {
            this.mInteractiveChanging = false;
            handleLateInteractiveChange();
        }
    }

    private void handleEarlyInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Notifier.this.mPolicy.startedWakingUp();
                    }
                });
                this.mPendingInteractiveState = 1;
                this.mPendingWakeUpBroadcast = true;
                updatePendingBroadcastLocked();
            } else {
                final int iTranslateOffReason = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Notifier.this.mPolicy.startedGoingToSleep(iTranslateOffReason);
                    }
                });
            }
        }
    }

    private void handleLateInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Notifier.this.mPolicy.finishedWakingUp();
                    }
                });
            } else {
                if (this.mUserActivityPending) {
                    this.mUserActivityPending = false;
                    this.mHandler.removeMessages(1);
                }
                final int iTranslateOffReason = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogMaker logMaker = new LogMaker(198);
                        logMaker.setType(2);
                        logMaker.setSubtype(iTranslateOffReason);
                        MetricsLogger.action(logMaker);
                        EventLogTags.writePowerScreenState(0, iTranslateOffReason, 0L, 0, 0);
                        Notifier.this.mPolicy.finishedGoingToSleep(iTranslateOffReason);
                    }
                });
                this.mPendingInteractiveState = 2;
                this.mPendingGoToSleepBroadcast = true;
                updatePendingBroadcastLocked();
            }
        }
    }

    private static int translateOffReason(int i) {
        switch (i) {
            case 1:
                return 1;
            case 2:
                return 3;
            default:
                return 2;
        }
    }

    public void onScreenBrightnessBoostChanged() {
        this.mSuspendBlocker.acquire();
        Message messageObtainMessage = this.mHandler.obtainMessage(4);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void onUserActivity(int i, int i2) {
        try {
            this.mBatteryStats.noteUserActivity(i2, i);
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            if (!this.mUserActivityPending) {
                this.mUserActivityPending = true;
                Message messageObtainMessage = this.mHandler.obtainMessage(1);
                messageObtainMessage.setAsynchronous(true);
                this.mHandler.sendMessage(messageObtainMessage);
            }
        }
    }

    public void onWakeUp(String str, int i, String str2, int i2) {
        try {
            this.mBatteryStats.noteWakeUp(str, i);
            if (str2 != null) {
                this.mAppOps.noteOpNoThrow(61, i2, str2);
            }
        } catch (RemoteException e) {
        }
    }

    public void onProfileTimeout(int i) {
        Message messageObtainMessage = this.mHandler.obtainMessage(5);
        messageObtainMessage.setAsynchronous(true);
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void onWirelessChargingStarted(int i) {
        this.mSuspendBlocker.acquire();
        Message messageObtainMessage = this.mHandler.obtainMessage(3);
        messageObtainMessage.setAsynchronous(true);
        messageObtainMessage.arg1 = i;
        this.mHandler.sendMessage(messageObtainMessage);
    }

    public void onWiredChargingStarted() {
        this.mSuspendBlocker.acquire();
        Message messageObtainMessage = this.mHandler.obtainMessage(6);
        messageObtainMessage.setAsynchronous(true);
        this.mHandler.sendMessage(messageObtainMessage);
    }

    private void updatePendingBroadcastLocked() {
        if (this.mBroadcastInProgress || this.mPendingInteractiveState == 0) {
            return;
        }
        if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mPendingInteractiveState != this.mBroadcastedInteractiveState) {
            this.mBroadcastInProgress = true;
            this.mSuspendBlocker.acquire();
            Message messageObtainMessage = this.mHandler.obtainMessage(2);
            messageObtainMessage.setAsynchronous(true);
            this.mHandler.sendMessage(messageObtainMessage);
        }
    }

    private void finishPendingBroadcastLocked() {
        this.mBroadcastInProgress = false;
        this.mSuspendBlocker.release();
    }

    private void sendUserActivity() {
        synchronized (this.mLock) {
            if (this.mUserActivityPending) {
                this.mUserActivityPending = false;
                this.mPolicy.userActivity();
            }
        }
    }

    private void sendNextBroadcast() {
        synchronized (this.mLock) {
            if (this.mBroadcastedInteractiveState == 0) {
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            } else if (this.mBroadcastedInteractiveState == 1) {
                if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 2) {
                    finishPendingBroadcastLocked();
                    return;
                }
                this.mPendingGoToSleepBroadcast = false;
                this.mBroadcastedInteractiveState = 2;
            } else {
                if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 1) {
                    finishPendingBroadcastLocked();
                    return;
                }
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            }
            this.mBroadcastStartTime = SystemClock.uptimeMillis();
            int i = this.mBroadcastedInteractiveState;
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_SEND, 1);
            if (i == 1) {
                sendWakeUpBroadcast();
            } else {
                sendGoToSleepBroadcast();
            }
        }
    }

    private void sendBrightnessBoostChangedBroadcast() {
        this.mContext.sendOrderedBroadcastAsUser(this.mScreenBrightnessBoostIntent, UserHandle.ALL, null, this.mScreeBrightnessBoostChangedDone, this.mHandler, 0, null, null);
    }

    private void sendWakeUpBroadcast() {
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOnIntent, UserHandle.ALL, null, this.mWakeUpBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 2, 1);
            sendNextBroadcast();
        }
    }

    private void sendGoToSleepBroadcast() {
        if (this.mActivityManagerInternal.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOffIntent, UserHandle.ALL, null, this.mGoToSleepBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 3, 1);
            sendNextBroadcast();
        }
    }

    private void playChargingStartedSound() {
        Ringtone ringtone;
        String string = Settings.Global.getString(this.mContext.getContentResolver(), "wireless_charging_started_sound");
        if (isChargingFeedbackEnabled() && string != null) {
            Uri uri = Uri.parse("file://" + string);
            if (uri != null && (ringtone = RingtoneManager.getRingtone(this.mContext, uri)) != null) {
                ringtone.setStreamType(1);
                ringtone.play();
            }
        }
    }

    private void showWirelessChargingStarted(int i) {
        playWirelessChargingVibration();
        playChargingStartedSound();
        if (this.mStatusBarManagerInternal != null) {
            this.mStatusBarManagerInternal.showChargingAnimation(i);
        }
        this.mSuspendBlocker.release();
    }

    private void showWiredChargingStarted() {
        playChargingStartedSound();
        this.mSuspendBlocker.release();
    }

    private void lockProfile(int i) {
        this.mTrustManager.setDeviceLockedForUser(i, true);
    }

    private void playWirelessChargingVibration() {
        if ((Settings.Global.getInt(this.mContext.getContentResolver(), "charging_vibration_enabled", 0) != 0) && isChargingFeedbackEnabled()) {
            this.mVibrator.vibrate(WIRELESS_CHARGING_VIBRATION_EFFECT, VIBRATION_ATTRIBUTES);
        }
    }

    private boolean isChargingFeedbackEnabled() {
        return (Settings.Global.getInt(this.mContext.getContentResolver(), "charging_sounds_enabled", 1) != 0) && (Settings.Global.getInt(this.mContext.getContentResolver(), "zen_mode", 1) == 0);
    }

    private final class NotifierHandler extends Handler {
        public NotifierHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Notifier.this.sendUserActivity();
                    break;
                case 2:
                    Notifier.this.sendNextBroadcast();
                    break;
                case 3:
                    Notifier.this.showWirelessChargingStarted(message.arg1);
                    break;
                case 4:
                    Notifier.this.sendBrightnessBoostChangedBroadcast();
                    break;
                case 5:
                    Notifier.this.lockProfile(message.arg1);
                    break;
                case 6:
                    Notifier.this.showWiredChargingStarted();
                    break;
            }
        }
    }
}
