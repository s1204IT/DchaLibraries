package com.android.server.power;

import android.R;
import android.app.ActivityManagerInternal;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManagerInternal;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Slog;
import android.view.WindowManagerPolicy;
import android.view.inputmethod.InputMethodManagerInternal;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;

final class Notifier {
    private static final boolean DEBUG = true;
    private static final int INTERACTIVE_STATE_ASLEEP = 2;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_UNKNOWN = 0;
    private static final int MSG_BROADCAST = 2;
    private static final int MSG_SCREEN_BRIGHTNESS_BOOST_CHANGED = 4;
    private static final int MSG_USER_ACTIVITY = 1;
    private static final int MSG_WIRELESS_CHARGING_STARTED = 3;
    private static final String TAG = "PowerManagerNotifier";
    private final IAppOpsService mAppOps;
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
    private boolean mUserActivityPending;
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
            Slog.d(Notifier.TAG, "mWakeUpBroadcastDone - sendNextBroadcast");
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, 1, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final BroadcastReceiver mGoToSleepBroadcastDone = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Slog.d(Notifier.TAG, "mGoToSleepBroadcastDone - sendNextBroadcast");
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_DONE, 0, Long.valueOf(SystemClock.uptimeMillis() - Notifier.this.mBroadcastStartTime), 1);
            Notifier.this.sendNextBroadcast();
        }
    };
    private final ActivityManagerInternal mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
    private final InputManagerInternal mInputManagerInternal = (InputManagerInternal) LocalServices.getService(InputManagerInternal.class);
    private final InputMethodManagerInternal mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
    private final Intent mScreenOnIntent = new Intent("android.intent.action.SCREEN_ON");

    public Notifier(Looper looper, Context context, IBatteryStats batteryStats, IAppOpsService appOps, SuspendBlocker suspendBlocker, WindowManagerPolicy policy) {
        this.mContext = context;
        this.mBatteryStats = batteryStats;
        this.mAppOps = appOps;
        this.mSuspendBlocker = suspendBlocker;
        this.mPolicy = policy;
        this.mHandler = new NotifierHandler(looper);
        this.mScreenOnIntent.addFlags(1342177280);
        this.mScreenOffIntent = new Intent("android.intent.action.SCREEN_OFF");
        this.mScreenOffIntent.addFlags(1342177280);
        this.mScreenBrightnessBoostIntent = new Intent("android.os.action.SCREEN_BRIGHTNESS_BOOST_CHANGED");
        this.mScreenBrightnessBoostIntent.addFlags(1342177280);
        this.mSuspendWhenScreenOffDueToProximityConfig = context.getResources().getBoolean(R.^attr-private.dotColor);
        try {
            this.mBatteryStats.noteInteractive(true);
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockAcquired(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        Slog.d(TAG, "onWakeLockAcquired: flags=" + flags + ", tag=\"" + tag + "\", packageName=" + packageName + ", ownerUid=" + ownerUid + ", ownerPid=" + ownerPid + ", workSource=" + workSource);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        if (monitorType >= 0) {
            boolean unimportantForLogging = ownerUid == 1000 && (1073741824 & flags) != 0;
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStartWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                } else {
                    this.mBatteryStats.noteStartWakelock(ownerUid, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                    this.mAppOps.startOperation(AppOpsManager.getToken(this.mAppOps), 40, ownerUid, packageName);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void onWakeLockChanging(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, int newFlags, String newTag, String newPackageName, int newOwnerUid, int newOwnerPid, WorkSource newWorkSource, String newHistoryTag) {
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        int newMonitorType = getBatteryStatsWakeLockMonitorType(newFlags);
        if (workSource == null || newWorkSource == null || monitorType < 0 || newMonitorType < 0) {
            onWakeLockReleased(flags, tag, packageName, ownerUid, ownerPid, workSource, historyTag);
            onWakeLockAcquired(newFlags, newTag, newPackageName, newOwnerUid, newOwnerPid, newWorkSource, newHistoryTag);
        } else {
            Slog.d(TAG, "onWakeLockChanging: flags=" + newFlags + ", tag=\"" + newTag + "\", packageName=" + newPackageName + ", ownerUid=" + newOwnerUid + ", ownerPid=" + newOwnerPid + ", workSource=" + newWorkSource);
            boolean unimportantForLogging = newOwnerUid == 1000 && (1073741824 & newFlags) != 0;
            try {
                this.mBatteryStats.noteChangeWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, newWorkSource, newOwnerPid, newTag, newHistoryTag, newMonitorType, unimportantForLogging);
            } catch (RemoteException e) {
            }
        }
    }

    public void onWakeLockReleased(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        Slog.d(TAG, "onWakeLockReleased: flags=" + flags + ", tag=\"" + tag + "\", packageName=" + packageName + ", ownerUid=" + ownerUid + ", ownerPid=" + ownerPid + ", workSource=" + workSource);
        int monitorType = getBatteryStatsWakeLockMonitorType(flags);
        if (monitorType >= 0) {
            try {
                if (workSource != null) {
                    this.mBatteryStats.noteStopWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType);
                } else {
                    this.mBatteryStats.noteStopWakelock(ownerUid, ownerPid, tag, historyTag, monitorType);
                    this.mAppOps.finishOperation(AppOpsManager.getToken(this.mAppOps), 40, ownerUid, packageName);
                }
            } catch (RemoteException e) {
            }
        }
    }

    private int getBatteryStatsWakeLockMonitorType(int flags) {
        switch (65535 & flags) {
            case 1:
                return 0;
            case 6:
            case 10:
                return 1;
            case 32:
                return this.mSuspendWhenScreenOffDueToProximityConfig ? -1 : 0;
            case 64:
                return -1;
            case 128:
                return 18;
            default:
                return -1;
        }
    }

    public void onWakefulnessChangeStarted(final int wakefulness, int reason) {
        boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
        Slog.d(TAG, "onWakefulnessChangeStarted: wakefulness=" + wakefulness + ", reason=" + reason + ", interactive=" + interactive);
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Notifier.this.mActivityManagerInternal.onWakefulnessChanged(wakefulness);
            }
        });
        if (this.mInteractive == interactive) {
            return;
        }
        if (this.mInteractiveChanging) {
            handleLateInteractiveChange();
        }
        this.mInputManagerInternal.setInteractive(interactive);
        this.mInputMethodManagerInternal.setInteractive(interactive);
        try {
            this.mBatteryStats.noteInteractive(interactive);
        } catch (RemoteException e) {
        }
        this.mInteractive = interactive;
        this.mInteractiveChangeReason = reason;
        this.mInteractiveChanging = true;
        handleEarlyInteractiveChange();
    }

    public void onWakefulnessChangeFinished() {
        Slog.d(TAG, "onWakefulnessChangeFinished");
        if (!this.mInteractiveChanging) {
            return;
        }
        this.mInteractiveChanging = false;
        handleLateInteractiveChange();
    }

    private void handleEarlyInteractiveChange() {
        synchronized (this.mLock) {
            if (this.mInteractive) {
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        EventLog.writeEvent(EventLogTags.POWER_SCREEN_STATE, 1, 0, 0, 0);
                        Slog.d(Notifier.TAG, "handleEarlyInteractiveChange: mPolicy.startedWakingUp");
                        Notifier.this.mPolicy.startedWakingUp();
                    }
                });
                this.mPendingInteractiveState = 1;
                this.mPendingWakeUpBroadcast = true;
                updatePendingBroadcastLocked();
            } else {
                final int why = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Notifier.this.mPolicy.startedGoingToSleep(why);
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
                final int why = translateOffReason(this.mInteractiveChangeReason);
                this.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        EventLog.writeEvent(EventLogTags.POWER_SCREEN_STATE, 0, Integer.valueOf(why), 0, 0);
                        Notifier.this.mPolicy.finishedGoingToSleep(why);
                    }
                });
                this.mPendingInteractiveState = 2;
                this.mPendingGoToSleepBroadcast = true;
                updatePendingBroadcastLocked();
            }
        }
    }

    private static int translateOffReason(int reason) {
        switch (reason) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 8:
                return 4;
            default:
                return 2;
        }
    }

    public void onScreenBrightnessBoostChanged() {
        Slog.d(TAG, "onScreenBrightnessBoostChanged");
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(4);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    public void onUserActivity(int event, int uid) {
        Slog.d(TAG, "onUserActivity: event=" + event + ", uid=" + uid);
        try {
            this.mBatteryStats.noteUserActivity(uid, event);
        } catch (RemoteException e) {
        }
        synchronized (this.mLock) {
            if (!this.mUserActivityPending) {
                this.mUserActivityPending = true;
                Message msg = this.mHandler.obtainMessage(1);
                msg.setAsynchronous(true);
                this.mHandler.sendMessage(msg);
            }
        }
    }

    public void onWakeUp(String reason, int reasonUid, String opPackageName, int opUid) {
        Slog.d(TAG, "onWakeUp: event=" + reason + ", reasonUid=" + reasonUid + " opPackageName=" + opPackageName + " opUid=" + opUid);
        try {
            this.mBatteryStats.noteWakeUp(reason, reasonUid);
            if (opPackageName == null) {
                return;
            }
            this.mAppOps.noteOperation(61, opUid, opPackageName);
        } catch (RemoteException e) {
        }
    }

    public void onWirelessChargingStarted() {
        Slog.d(TAG, "onWirelessChargingStarted");
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(3);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    private void updatePendingBroadcastLocked() {
        Slog.d(TAG, "updatePendingBroadcastLocked mBroadcastInProgress = " + this.mBroadcastInProgress + ", mPendingInteractiveState = " + this.mPendingInteractiveState + ", mPendingGoToSleepBroadcast = " + this.mPendingGoToSleepBroadcast + ", mBroadcastedInteractiveState = " + this.mBroadcastedInteractiveState);
        if (this.mBroadcastInProgress || this.mPendingInteractiveState == 0) {
            return;
        }
        if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mPendingInteractiveState != this.mBroadcastedInteractiveState) {
            this.mBroadcastInProgress = true;
            this.mSuspendBlocker.acquire();
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void finishPendingBroadcastLocked() {
        Slog.d(TAG, "finishPendingBroadcastLocked");
        this.mBroadcastInProgress = false;
        this.mSuspendBlocker.release();
    }

    private void sendUserActivity() {
        synchronized (this.mLock) {
            if (!this.mUserActivityPending) {
                return;
            }
            this.mUserActivityPending = false;
            this.mPolicy.userActivity();
        }
    }

    private void sendNextBroadcast() {
        Slog.d(TAG, "sendNextBroadcast mBroadcastedInteractiveState = " + this.mBroadcastedInteractiveState + ", mPendingInteractiveState = " + this.mPendingInteractiveState + ", mPendingWakeUpBroadcast = " + this.mPendingWakeUpBroadcast + ", mPendingGoToSleepBroadcast = " + this.mPendingGoToSleepBroadcast);
        synchronized (this.mLock) {
            if (this.mBroadcastedInteractiveState == 0) {
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            } else if (this.mBroadcastedInteractiveState == 1) {
                if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 2) {
                    finishPendingBroadcastLocked();
                    return;
                } else {
                    this.mPendingGoToSleepBroadcast = false;
                    this.mBroadcastedInteractiveState = 2;
                }
            } else if (!this.mPendingWakeUpBroadcast && !this.mPendingGoToSleepBroadcast && this.mPendingInteractiveState != 1) {
                finishPendingBroadcastLocked();
                return;
            } else {
                this.mPendingWakeUpBroadcast = false;
                this.mBroadcastedInteractiveState = 1;
            }
            this.mBroadcastStartTime = SystemClock.uptimeMillis();
            int powerState = this.mBroadcastedInteractiveState;
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_SEND, 1);
            if (powerState == 1) {
                sendWakeUpBroadcast();
            } else {
                sendGoToSleepBroadcast();
            }
        }
    }

    private void sendBrightnessBoostChangedBroadcast() {
        Slog.d(TAG, "Sending brightness boost changed broadcast.");
        this.mContext.sendOrderedBroadcastAsUser(this.mScreenBrightnessBoostIntent, UserHandle.ALL, null, this.mScreeBrightnessBoostChangedDone, this.mHandler, 0, null, null);
    }

    private void sendWakeUpBroadcast() {
        Slog.d(TAG, "sendWakeUpBroadcast");
        if (ActivityManagerNative.isSystemReady()) {
            Slog.d(TAG, "sendWakeUpBroadcast - sendOrderedBroadcastAsUser");
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOnIntent, UserHandle.ALL, null, this.mWakeUpBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 2, 1);
            sendNextBroadcast();
        }
    }

    private void sendGoToSleepBroadcast() {
        Slog.d(TAG, "sendGoToSleepBroadcast");
        if (ActivityManagerNative.isSystemReady()) {
            Slog.d(TAG, "sendGoToSleepBroadcast - sendOrderedBroadcastAsUser");
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOffIntent, UserHandle.ALL, null, this.mGoToSleepBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 3, 1);
            sendNextBroadcast();
        }
    }

    private void playWirelessChargingStartedSound() {
        Uri soundUri;
        Ringtone sfx;
        boolean enabled = Settings.Global.getInt(this.mContext.getContentResolver(), "charging_sounds_enabled", 1) != 0;
        String soundPath = Settings.Global.getString(this.mContext.getContentResolver(), "wireless_charging_started_sound");
        if (enabled && soundPath != null && (soundUri = Uri.parse("file://" + soundPath)) != null && (sfx = RingtoneManager.getRingtone(this.mContext, soundUri)) != null) {
            sfx.setStreamType(1);
            sfx.play();
        }
        this.mSuspendBlocker.release();
    }

    private final class NotifierHandler extends Handler {
        public NotifierHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Notifier.this.sendUserActivity();
                    break;
                case 2:
                    Notifier.this.sendNextBroadcast();
                    break;
                case 3:
                    Notifier.this.playWirelessChargingStartedSound();
                    break;
                case 4:
                    Notifier.this.sendBrightnessBoostChangedBroadcast();
                    break;
            }
        }
    }
}
