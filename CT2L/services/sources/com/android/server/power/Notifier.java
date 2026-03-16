package com.android.server.power;

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
import android.view.WindowManagerPolicy;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IBatteryStats;
import com.android.server.EventLogTags;
import com.android.server.LocalServices;

final class Notifier {
    private static final boolean DEBUG = false;
    private static final int INTERACTIVE_STATE_ASLEEP = 2;
    private static final int INTERACTIVE_STATE_AWAKE = 1;
    private static final int INTERACTIVE_STATE_UNKNOWN = 0;
    private static final int MSG_BROADCAST = 2;
    private static final int MSG_USER_ACTIVITY = 1;
    private static final int MSG_WIRELESS_CHARGING_STARTED = 3;
    private static final String TAG = "PowerManagerNotifier";
    private int mActualInteractiveState;
    private final IAppOpsService mAppOps;
    private final IBatteryStats mBatteryStats;
    private boolean mBroadcastInProgress;
    private long mBroadcastStartTime;
    private int mBroadcastedInteractiveState;
    private final Context mContext;
    private final NotifierHandler mHandler;
    private int mLastReason;
    private boolean mPendingGoToSleepBroadcast;
    private boolean mPendingWakeUpBroadcast;
    private final WindowManagerPolicy mPolicy;
    private final Intent mScreenOffIntent;
    private final SuspendBlocker mSuspendBlocker;
    private boolean mUserActivityPending;
    private final Object mLock = new Object();
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
        try {
            this.mBatteryStats.noteInteractive(true);
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockAcquired(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        try {
            int monitorType = getBatteryStatsWakeLockMonitorType(flags);
            boolean unimportantForLogging = ((1073741824 & flags) == 0 || ownerUid != 1000) ? DEBUG : true;
            if (workSource != null) {
                this.mBatteryStats.noteStartWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
            } else {
                this.mBatteryStats.noteStartWakelock(ownerUid, ownerPid, tag, historyTag, monitorType, unimportantForLogging);
                this.mAppOps.startOperation(AppOpsManager.getToken(this.mAppOps), 40, ownerUid, packageName);
            }
        } catch (RemoteException e) {
        }
    }

    public void onWakeLockChanging(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag, int newFlags, String newTag, String newPackageName, int newOwnerUid, int newOwnerPid, WorkSource newWorkSource, String newHistoryTag) {
        if (workSource != null && newWorkSource != null) {
            int monitorType = getBatteryStatsWakeLockMonitorType(flags);
            int newMonitorType = getBatteryStatsWakeLockMonitorType(newFlags);
            boolean unimportantForLogging = ((1073741824 & newFlags) == 0 || newOwnerUid != 1000) ? DEBUG : true;
            try {
                this.mBatteryStats.noteChangeWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType, newWorkSource, newOwnerPid, newTag, newHistoryTag, newMonitorType, unimportantForLogging);
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        onWakeLockReleased(flags, tag, packageName, ownerUid, ownerPid, workSource, historyTag);
        onWakeLockAcquired(newFlags, newTag, newPackageName, newOwnerUid, newOwnerPid, newWorkSource, newHistoryTag);
    }

    public void onWakeLockReleased(int flags, String tag, String packageName, int ownerUid, int ownerPid, WorkSource workSource, String historyTag) {
        try {
            int monitorType = getBatteryStatsWakeLockMonitorType(flags);
            if (workSource != null) {
                this.mBatteryStats.noteStopWakelockFromSource(workSource, ownerPid, tag, historyTag, monitorType);
            } else {
                this.mBatteryStats.noteStopWakelock(ownerUid, ownerPid, tag, historyTag, monitorType);
                this.mAppOps.finishOperation(AppOpsManager.getToken(this.mAppOps), 40, ownerUid, packageName);
            }
        } catch (RemoteException e) {
        }
    }

    private static int getBatteryStatsWakeLockMonitorType(int flags) {
        switch (65535 & flags) {
            case 1:
            case 32:
                return 0;
            default:
                return 1;
        }
    }

    public void onWakefulnessChangeStarted(int wakefulness, int reason) {
        boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
        if (interactive) {
            handleWakefulnessChange(wakefulness, interactive, reason);
        } else {
            this.mLastReason = reason;
        }
        this.mInputManagerInternal.setInteractive(interactive);
    }

    public void onWakefulnessChangeFinished(int wakefulness) {
        boolean interactive = PowerManagerInternal.isInteractive(wakefulness);
        if (!interactive) {
            handleWakefulnessChange(wakefulness, interactive, this.mLastReason);
        }
    }

    private void handleWakefulnessChange(final int wakefulness, boolean interactive, final int reason) {
        boolean interactiveChanged;
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                Notifier.this.mActivityManagerInternal.onWakefulnessChanged(wakefulness);
            }
        });
        synchronized (this.mLock) {
            if (interactive) {
                interactiveChanged = this.mActualInteractiveState != 1;
                if (interactiveChanged) {
                    this.mActualInteractiveState = 1;
                    this.mPendingWakeUpBroadcast = true;
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            EventLog.writeEvent(EventLogTags.POWER_SCREEN_STATE, 1, 0, 0, 0);
                            Notifier.this.mPolicy.wakingUp();
                        }
                    });
                    updatePendingBroadcastLocked();
                }
            } else {
                interactiveChanged = this.mActualInteractiveState != 2;
                if (interactiveChanged) {
                    this.mActualInteractiveState = 2;
                    this.mPendingGoToSleepBroadcast = true;
                    if (this.mUserActivityPending) {
                        this.mUserActivityPending = DEBUG;
                        this.mHandler.removeMessages(1);
                    }
                    this.mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            int why = 2;
                            switch (reason) {
                                case 1:
                                    why = 1;
                                    break;
                                case 2:
                                    why = 3;
                                    break;
                            }
                            EventLog.writeEvent(EventLogTags.POWER_SCREEN_STATE, 0, Integer.valueOf(why), 0, 0);
                            Notifier.this.mPolicy.goingToSleep(why);
                        }
                    });
                    updatePendingBroadcastLocked();
                }
            }
        }
        if (interactiveChanged) {
            try {
                this.mBatteryStats.noteInteractive(interactive);
            } catch (RemoteException e) {
            }
        }
    }

    public void onUserActivity(int event, int uid) {
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

    public void onWirelessChargingStarted() {
        this.mSuspendBlocker.acquire();
        Message msg = this.mHandler.obtainMessage(3);
        msg.setAsynchronous(true);
        this.mHandler.sendMessage(msg);
    }

    private void updatePendingBroadcastLocked() {
        if (this.mBroadcastInProgress || this.mActualInteractiveState == 0) {
            return;
        }
        if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mActualInteractiveState != this.mBroadcastedInteractiveState) {
            this.mBroadcastInProgress = true;
            this.mSuspendBlocker.acquire();
            Message msg = this.mHandler.obtainMessage(2);
            msg.setAsynchronous(true);
            this.mHandler.sendMessage(msg);
        }
    }

    private void finishPendingBroadcastLocked() {
        this.mBroadcastInProgress = DEBUG;
        this.mSuspendBlocker.release();
    }

    private void sendUserActivity() {
        synchronized (this.mLock) {
            if (this.mUserActivityPending) {
                this.mUserActivityPending = DEBUG;
                this.mPolicy.userActivity();
            }
        }
    }

    private void sendNextBroadcast() {
        synchronized (this.mLock) {
            if (this.mBroadcastedInteractiveState == 0) {
                this.mPendingWakeUpBroadcast = DEBUG;
                this.mBroadcastedInteractiveState = 1;
            } else if (this.mBroadcastedInteractiveState == 1) {
                if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mActualInteractiveState == 2) {
                    this.mPendingGoToSleepBroadcast = DEBUG;
                    this.mBroadcastedInteractiveState = 2;
                } else {
                    finishPendingBroadcastLocked();
                    return;
                }
            } else if (this.mPendingWakeUpBroadcast || this.mPendingGoToSleepBroadcast || this.mActualInteractiveState == 1) {
                this.mPendingWakeUpBroadcast = DEBUG;
                this.mBroadcastedInteractiveState = 1;
            } else {
                finishPendingBroadcastLocked();
                return;
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

    private void sendWakeUpBroadcast() {
        if (ActivityManagerNative.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOnIntent, UserHandle.ALL, null, this.mWakeUpBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 2, 1);
            sendNextBroadcast();
        }
    }

    private void sendGoToSleepBroadcast() {
        if (ActivityManagerNative.isSystemReady()) {
            this.mContext.sendOrderedBroadcastAsUser(this.mScreenOffIntent, UserHandle.ALL, null, this.mGoToSleepBroadcastDone, this.mHandler, 0, null, null);
        } else {
            EventLog.writeEvent(EventLogTags.POWER_SCREEN_BROADCAST_STOP, 3, 1);
            sendNextBroadcast();
        }
    }

    private void playWirelessChargingStartedSound() {
        Uri soundUri;
        Ringtone sfx;
        String soundPath = Settings.Global.getString(this.mContext.getContentResolver(), "wireless_charging_started_sound");
        if (soundPath != null && (soundUri = Uri.parse("file://" + soundPath)) != null && (sfx = RingtoneManager.getRingtone(this.mContext, soundUri)) != null) {
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
            }
        }
    }
}
