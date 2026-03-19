package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.BroadcastOptions;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.server.DeviceIdleController;
import com.android.server.am.ActiveServices;
import com.mediatek.am.AMEventHookData;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.server.am.AMEventHook;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

public final class BroadcastQueue {
    static final int BROADCAST_INTENT_MSG = 200;
    static final int BROADCAST_TIMEOUT_MSG = 201;
    static final int MAX_BROADCAST_HISTORY;
    static final int MAX_BROADCAST_SUMMARY_HISTORY;
    static final int SCHEDULE_TEMP_WHITELIST_MSG = 202;
    private static final String TAG = "BroadcastQueue";
    private static final String TAG_BROADCAST = TAG + ActivityManagerDebugConfig.POSTFIX_BROADCAST;
    private static final String TAG_MU = "BroadcastQueue_MU";
    final boolean mDelayBehindServices;
    final BroadcastHandler mHandler;
    int mPendingBroadcastRecvIndex;
    boolean mPendingBroadcastTimeoutMessage;
    final String mQueueName;
    final ActivityManagerService mService;
    final long mTimeoutPeriod;
    final ArrayList<BroadcastRecord> mParallelBroadcasts = new ArrayList<>();
    final ArrayList<BroadcastRecord> mOrderedBroadcasts = new ArrayList<>();
    final BroadcastRecord[] mBroadcastHistory = new BroadcastRecord[MAX_BROADCAST_HISTORY];
    int mHistoryNext = 0;
    final Intent[] mBroadcastSummaryHistory = new Intent[MAX_BROADCAST_SUMMARY_HISTORY];
    int mSummaryHistoryNext = 0;
    final long[] mSummaryHistoryEnqueueTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    final long[] mSummaryHistoryDispatchTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    final long[] mSummaryHistoryFinishTime = new long[MAX_BROADCAST_SUMMARY_HISTORY];
    boolean mBroadcastsScheduled = false;
    BroadcastRecord mPendingBroadcast = null;
    final AnrBroadcastQueue mAnrBroadcastQueue = new AnrBroadcastQueue();

    static {
        MAX_BROADCAST_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 10 : 50;
        MAX_BROADCAST_SUMMARY_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 25 : 300;
    }

    class AnrBroadcastQueue implements ANRManager.IAnrBroadcastQueue {
        AnrBroadcastQueue() {
        }

        @Override
        public int getOrderedBroadcastsPid() {
            BroadcastRecord br;
            int pid = -1;
            synchronized (BroadcastQueue.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (BroadcastQueue.this.mOrderedBroadcasts.size() > 0 && (br = BroadcastQueue.this.mOrderedBroadcasts.get(0)) != null && br.curApp != null) {
                        pid = br.curApp.pid;
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            return pid;
        }
    }

    private final class BroadcastHandler extends Handler {
        public BroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BroadcastQueue.BROADCAST_INTENT_MSG:
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(BroadcastQueue.TAG_BROADCAST, "Received BROADCAST_INTENT_MSG");
                    }
                    BroadcastQueue.this.processNextBroadcast(true);
                    return;
                case BroadcastQueue.BROADCAST_TIMEOUT_MSG:
                    synchronized (BroadcastQueue.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            BroadcastQueue.this.broadcastTimeoutLocked(true);
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                case BroadcastQueue.SCHEDULE_TEMP_WHITELIST_MSG:
                    DeviceIdleController.LocalService dic = BroadcastQueue.this.mService.mLocalDeviceIdleController;
                    if (dic == null) {
                        return;
                    }
                    dic.addPowerSaveTempWhitelistAppDirect(UserHandle.getAppId(msg.arg1), msg.arg2, true, (String) msg.obj);
                    return;
                default:
                    return;
            }
        }
    }

    private final class AppNotResponding implements Runnable {
        private final String mAnnotation;
        private final ProcessRecord mApp;

        public AppNotResponding(ProcessRecord app, String annotation) {
            this.mApp = app;
            this.mAnnotation = annotation;
        }

        @Override
        public void run() {
            BroadcastQueue.this.mService.mAppErrors.appNotResponding(this.mApp, null, null, false, this.mAnnotation);
        }
    }

    BroadcastQueue(ActivityManagerService service, Handler handler, String name, long timeoutPeriod, boolean allowDelayBehindServices) {
        this.mService = service;
        this.mHandler = new BroadcastHandler(handler.getLooper());
        this.mQueueName = name;
        this.mTimeoutPeriod = timeoutPeriod;
        this.mDelayBehindServices = allowDelayBehindServices;
    }

    public boolean isPendingBroadcastProcessLocked(int pid) {
        return this.mPendingBroadcast != null && this.mPendingBroadcast.curApp.pid == pid;
    }

    public void enqueueParallelBroadcastLocked(BroadcastRecord r) {
        this.mParallelBroadcasts.add(r);
        r.enqueueClockTime = System.currentTimeMillis();
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        this.mOrderedBroadcasts.add(r);
        r.enqueueClockTime = System.currentTimeMillis();
    }

    public final boolean replaceParallelBroadcastLocked(BroadcastRecord r) {
        for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            if (r.intent.filterEquals(this.mParallelBroadcasts.get(i).intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "***** DROPPING PARALLEL [" + this.mQueueName + "]: " + r.intent);
                }
                this.mParallelBroadcasts.set(i, r);
                return true;
            }
        }
        return false;
    }

    public final boolean replaceOrderedBroadcastLocked(BroadcastRecord r) {
        for (int i = this.mOrderedBroadcasts.size() - 1; i > 0; i--) {
            if (r.intent.filterEquals(this.mOrderedBroadcasts.get(i).intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "***** DROPPING ORDERED [" + this.mQueueName + "]: " + r.intent);
                }
                this.mOrderedBroadcasts.set(i, r);
                return true;
            }
        }
        return false;
    }

    private final void processCurBroadcastLocked(BroadcastRecord broadcastRecord, ProcessRecord processRecord) throws RemoteException {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Process cur broadcast " + broadcastRecord + " for app " + processRecord);
        }
        if (processRecord.thread == null) {
            throw new RemoteException();
        }
        if (processRecord.inFullBackup) {
            skipReceiverLocked(broadcastRecord);
            return;
        }
        broadcastRecord.receiver = processRecord.thread.asBinder();
        broadcastRecord.curApp = processRecord;
        processRecord.curReceiver = broadcastRecord;
        processRecord.forceProcessStateUpTo(11);
        this.mService.updateLruProcessLocked(processRecord, false, null);
        this.mService.updateOomAdjLocked();
        broadcastRecord.intent.setComponent(broadcastRecord.curComponent);
        boolean z = false;
        try {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Delivering to component " + broadcastRecord.curComponent + ": " + broadcastRecord);
            }
            this.mService.notifyPackageUse(broadcastRecord.intent.getComponent().getPackageName(), 3);
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.d(TAG_BROADCAST, "BDC-Delivering broadcast: " + broadcastRecord.intent + ", queue=" + this.mQueueName + ", ordered=" + broadcastRecord.ordered + ", app=" + processRecord + ", receiver=" + broadcastRecord.receiver);
            }
            processRecord.thread.scheduleReceiver(new Intent(broadcastRecord.intent), broadcastRecord.curReceiver, this.mService.compatibilityInfoForPackageLocked(broadcastRecord.curReceiver.applicationInfo), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.userId, processRecord.repProcState);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "Process cur broadcast " + broadcastRecord + " DELIVERED for app " + processRecord);
            }
            boolean z2 = true;
        } finally {
            if (!z) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Process cur broadcast " + broadcastRecord + ": NOT STARTED!");
                }
                broadcastRecord.receiver = null;
                broadcastRecord.curApp = null;
                processRecord.curReceiver = null;
            }
        }
    }

    public boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br == null || br.curApp.pid != app.pid) {
            return false;
        }
        if (br.curApp != app) {
            Slog.e(TAG, "App mismatch when sending pending broadcast to " + app.processName + ", intended target is " + br.curApp.processName);
            return false;
        }
        try {
            this.mPendingBroadcast = null;
            if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                AMEventHookData.ReadyToStartStaticReceiver eventData = AMEventHookData.ReadyToStartStaticReceiver.createInstance();
                eventData.set(new Object[]{app.info.packageName, br.callerPackage, Integer.valueOf(br.callingUid)});
                this.mService.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartStaticReceiver, eventData);
            }
            processCurBroadcastLocked(br, app);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Exception in new application when starting receiver " + (br.curComponent != null ? br.curComponent.flattenToShortString() : "(null)"), e);
            logBroadcastReceiverDiscardLocked(br);
            finishReceiverLocked(br, br.resultCode, br.resultData, br.resultExtras, br.resultAbort, false);
            scheduleBroadcastsLocked();
            br.state = 0;
            throw new RuntimeException(e.getMessage());
        }
    }

    public void skipPendingBroadcastLocked(int pid) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br == null || br.curApp.pid != pid) {
            return;
        }
        br.state = 0;
        br.nextReceiver = this.mPendingBroadcastRecvIndex;
        this.mPendingBroadcast = null;
        scheduleBroadcastsLocked();
    }

    public void skipCurrentReceiverLocked(ProcessRecord app) {
        BroadcastRecord r = null;
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord br = this.mOrderedBroadcasts.get(0);
            if (br.curApp == app) {
                r = br;
            }
        }
        if (r == null && this.mPendingBroadcast != null && this.mPendingBroadcast.curApp == app) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "[" + this.mQueueName + "] skip & discard pending app " + r);
            }
            r = this.mPendingBroadcast;
        }
        if (r == null) {
            return;
        }
        skipReceiverLocked(r);
    }

    private void skipReceiverLocked(BroadcastRecord r) {
        logBroadcastReceiverDiscardLocked(r);
        finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
        scheduleBroadcastsLocked();
    }

    public void scheduleBroadcastsLocked() {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Schedule broadcasts [" + this.mQueueName + "]: current=" + this.mBroadcastsScheduled);
        }
        if (this.mBroadcastsScheduled) {
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(BROADCAST_INTENT_MSG, this));
        this.mBroadcastsScheduled = true;
    }

    public BroadcastRecord getMatchingOrderedReceiver(IBinder receiver) {
        BroadcastRecord r;
        if (this.mOrderedBroadcasts.size() <= 0 || (r = this.mOrderedBroadcasts.get(0)) == null || r.receiver != receiver) {
            return null;
        }
        return r;
    }

    public boolean finishReceiverLocked(BroadcastRecord r, int resultCode, String resultData, Bundle resultExtras, boolean resultAbort, boolean waitForServices) {
        ActivityInfo nextReceiver;
        int state = r.state;
        ActivityInfo receiver = r.curReceiver;
        r.state = 0;
        if (state == 0) {
            Slog.w(TAG, "finishReceiver [" + this.mQueueName + "] called but state is IDLE");
        }
        r.receiver = null;
        r.intent.setComponent(null);
        if (r.curApp != null && r.curApp.curReceiver == r) {
            r.curApp.curReceiver = null;
        }
        if (r.curFilter != null) {
            r.curFilter.receiverList.curBroadcast = null;
        }
        r.curFilter = null;
        r.curReceiver = null;
        r.curApp = null;
        this.mPendingBroadcast = null;
        r.resultCode = resultCode;
        r.resultData = resultData;
        r.resultExtras = resultExtras;
        if (resultAbort && (r.intent.getFlags() & 134217728) == 0) {
            r.resultAbort = resultAbort;
        } else {
            r.resultAbort = false;
        }
        if (waitForServices && r.curComponent != null && r.queue.mDelayBehindServices && r.queue.mOrderedBroadcasts.size() > 0 && r.queue.mOrderedBroadcasts.get(0) == r) {
            if (r.nextReceiver < r.receivers.size()) {
                Object obj = r.receivers.get(r.nextReceiver);
                nextReceiver = obj instanceof ActivityInfo ? (ActivityInfo) obj : null;
            } else {
                nextReceiver = null;
            }
            if ((receiver == null || nextReceiver == null || receiver.applicationInfo.uid != nextReceiver.applicationInfo.uid || !receiver.processName.equals(nextReceiver.processName)) && this.mService.mServices.hasBackgroundServices(r.userId)) {
                ActiveServices.ServiceMap smap = this.mService.mServices.mServiceMap.get(r.userId);
                if (smap != null) {
                    Slog.d(TAG_BROADCAST, "BDC-mStartingBackground size = " + smap.mStartingBackground.size() + " mStartingBackground = " + smap.mStartingBackground + " mMaxStartingBackground = " + this.mService.mServices.mMaxStartingBackground);
                }
                Slog.i(TAG, "Delay finish: " + r.curComponent.flattenToShortString());
                r.state = 4;
                return false;
            }
        }
        r.curComponent = null;
        return state == 1 || state == 3;
    }

    public void backgroundServicesFinishedLocked(int userId) {
        if (this.mOrderedBroadcasts.size() <= 0) {
            return;
        }
        BroadcastRecord br = this.mOrderedBroadcasts.get(0);
        if (br.userId != userId || br.state != 4) {
            return;
        }
        Slog.i(TAG, "Resuming delayed broadcast");
        br.curComponent = null;
        br.state = 0;
        processNextBroadcast(false);
    }

    void performReceiveLocked(ProcessRecord app, IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        if (app == null) {
            receiver.performReceive(intent, resultCode, data, extras, ordered, sticky, sendingUser);
            return;
        }
        if (app.thread == null) {
            throw new RemoteException("app.thread must not be null");
        }
        try {
            app.thread.scheduleRegisteredReceiver(receiver, intent, resultCode, data, extras, ordered, sticky, sendingUser, app.repProcState);
        } catch (RemoteException ex) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    Slog.w(TAG, "Can't deliver broadcast to " + app.processName + " (pid " + app.pid + "). Crashing it.");
                    app.scheduleCrash("can't deliver broadcast");
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw ex;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
    }

    private void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered, int index) {
        boolean skip = false;
        if (filter.requiredPermission != null) {
            int perm = this.mService.checkComponentPermission(filter.requiredPermission, r.callingPid, r.callingUid, -1, true);
            if (perm != 0) {
                Slog.w(TAG, "Permission Denial: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") requires " + filter.requiredPermission + " due to registered receiver " + filter);
                skip = true;
            } else {
                int opCode = AppOpsManager.permissionToOpCode(filter.requiredPermission);
                if (opCode != -1 && this.mService.mAppOpsService.noteOperation(opCode, r.callingUid, r.callerPackage) != 0) {
                    Slog.w(TAG, "Appop Denial: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") requires appop " + AppOpsManager.permissionToOp(filter.requiredPermission) + " due to registered receiver " + filter);
                    skip = true;
                }
            }
        }
        if (!skip && r.requiredPermissions != null && r.requiredPermissions.length > 0) {
            int i = 0;
            while (true) {
                if (i >= r.requiredPermissions.length) {
                    break;
                }
                String requiredPermission = r.requiredPermissions[i];
                int perm2 = this.mService.checkComponentPermission(requiredPermission, filter.receiverList.pid, filter.receiverList.uid, -1, true);
                if (perm2 == 0) {
                    int appOp = AppOpsManager.permissionToOpCode(requiredPermission);
                    if (appOp != -1 && appOp != r.appOp && this.mService.mAppOpsService.noteOperation(appOp, filter.receiverList.uid, filter.packageName) != 0) {
                        Slog.w(TAG, "Appop Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") requires appop " + AppOpsManager.permissionToOp(requiredPermission) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                        skip = true;
                        break;
                    }
                    i++;
                } else {
                    Slog.w(TAG, "Permission Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") requires " + requiredPermission + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                    skip = true;
                    break;
                }
            }
        }
        if (!skip && (r.requiredPermissions == null || r.requiredPermissions.length == 0)) {
            int perm3 = this.mService.checkComponentPermission(null, filter.receiverList.pid, filter.receiverList.uid, -1, true);
            if (perm3 != 0) {
                Slog.w(TAG, "Permission Denial: security check failed when receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                skip = true;
            }
        }
        if (!skip && r.appOp != -1 && this.mService.mAppOpsService.noteOperation(r.appOp, filter.receiverList.uid, filter.packageName) != 0) {
            Slog.w(TAG, "Appop Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") requires appop " + AppOpsManager.opToName(r.appOp) + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
            skip = true;
        }
        if (!skip) {
            int allowed = this.mService.checkAllowBackgroundLocked(filter.receiverList.uid, filter.packageName, -1, true);
            if (allowed == 2) {
                Slog.w(TAG, "Background execution not allowed: receiving " + r.intent + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ")");
                skip = true;
            }
        }
        if (!this.mService.mIntentFirewall.checkBroadcast(r.intent, r.callingUid, r.callingPid, r.resolvedType, filter.receiverList.uid)) {
            skip = true;
        }
        if (!skip && (filter.receiverList.app == null || filter.receiverList.app.crashing)) {
            Slog.w(TAG, "Skipping deliver [" + this.mQueueName + "] " + r + " to " + filter.receiverList + ": process crashing");
            skip = true;
        }
        if (filter.receiverList.app != null && filter.receiverList.app.pid != filter.receiverList.pid) {
            Slog.e(TAG, "Process " + filter.receiverList.app + " has been restarted , so skip the broadcast " + r);
            skip = true;
        }
        if (skip) {
            r.delivery[index] = 2;
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.d(TAG_BROADCAST, "BDC-Skip broadcast: " + r.intent + ", queue=" + this.mQueueName + ", ordered=" + ordered + ", filter=" + filter + ", broadcastRecord=" + r + ", receiver=" + r.receiver + ", #" + index);
                return;
            }
            return;
        }
        if (Build.isPermissionReviewRequired() && !requestStartTargetPermissionsReviewIfNeededLocked(r, filter.packageName, filter.owningUserId)) {
            r.delivery[index] = 2;
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.d(TAG_BROADCAST, "BDC-Skip by permissions, broadcast: " + r.intent + ", queue=" + this.mQueueName + ", ordered=" + ordered + ", filter=" + filter + ", broadcastRecord=" + r + ", receiver=" + r.receiver + ", #" + index);
                return;
            }
            return;
        }
        r.delivery[index] = 1;
        if (ordered) {
            r.receiver = filter.receiverList.receiver.asBinder();
            r.curFilter = filter;
            filter.receiverList.curBroadcast = r;
            r.state = 2;
            if (filter.receiverList.app != null) {
                r.curApp = filter.receiverList.app;
                filter.receiverList.app.curReceiver = r;
                this.mService.updateOomAdjLocked(r.curApp);
            }
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.i(TAG_BROADCAST, "Delivering to " + filter + " : " + r);
            }
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.d(TAG_BROADCAST, "BDC-Delivering broadcast: " + r.intent + ", queue=" + this.mQueueName + ", ordered=" + ordered + ", filter=" + filter + ", broadcastRecord=" + r + ", receiver=" + r.receiver + ", #" + index);
            }
            if (filter.receiverList.app == null || !filter.receiverList.app.inFullBackup) {
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.ReadyToStartDynamicReceiver eventData = AMEventHookData.ReadyToStartDynamicReceiver.createInstance();
                    eventData.set(new Object[]{filter.packageName, r.callerPackage, Integer.valueOf(r.callingUid)});
                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartDynamicReceiver, eventData);
                }
                performReceiveLocked(filter.receiverList.app, filter.receiverList.receiver, new Intent(r.intent), r.resultCode, r.resultData, r.resultExtras, r.ordered, r.initialSticky, r.userId);
            } else if (ordered) {
                skipReceiverLocked(r);
            }
            if (ordered) {
                r.state = 3;
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure sending broadcast " + r.intent, e);
            if (ordered) {
                r.receiver = null;
                r.curFilter = null;
                filter.receiverList.curBroadcast = null;
                if (filter.receiverList.app != null) {
                    filter.receiverList.app.curReceiver = null;
                }
            }
        }
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(BroadcastRecord receiverRecord, String receivingPackageName, final int receivingUserId) {
        if (!this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(receivingPackageName, receivingUserId)) {
            return true;
        }
        boolean callerForeground = receiverRecord.callerApp == null || receiverRecord.callerApp.setSchedGroup != 0;
        if (callerForeground && receiverRecord.intent.getComponent() != null) {
            IIntentSender target = this.mService.getIntentSenderLocked(1, receiverRecord.callerPackage, receiverRecord.callingUid, receiverRecord.userId, null, null, 0, new Intent[]{receiverRecord.intent}, new String[]{receiverRecord.intent.resolveType(this.mService.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", receivingPackageName);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW || !ActivityManagerService.IS_USER_BUILD) {
                Slog.i(TAG, "u" + receivingUserId + " Launching permission review for package " + receivingPackageName);
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BroadcastQueue.this.mService.mContext.startActivityAsUser(intent, new UserHandle(receivingUserId));
                }
            });
            return false;
        }
        Slog.w(TAG, "u" + receivingUserId + " Receiving a broadcast in package" + receivingPackageName + " requires a permissions review");
        return false;
    }

    final void scheduleTempWhitelistLocked(int uid, long duration, BroadcastRecord r) {
        if (duration > 2147483647L) {
            duration = 2147483647L;
        }
        StringBuilder b = new StringBuilder();
        b.append("broadcast:");
        UserHandle.formatUid(b, r.callingUid);
        b.append(":");
        if (r.intent.getAction() != null) {
            b.append(r.intent.getAction());
        } else if (r.intent.getComponent() != null) {
            b.append(r.intent.getComponent().flattenToShortString());
        } else if (r.intent.getData() != null) {
            b.append(r.intent.getData());
        }
        this.mHandler.obtainMessage(SCHEDULE_TEMP_WHITELIST_MSG, uid, (int) duration, b.toString()).sendToTarget();
    }

    final void processNextBroadcast(boolean fromMsg) {
        int opCode;
        int allowed;
        int perm;
        boolean z;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: " + this.mParallelBroadcasts.size() + " broadcasts, " + this.mOrderedBroadcasts.size() + " ordered broadcasts");
                }
                this.mService.updateCpuStats();
                if (fromMsg) {
                    this.mBroadcastsScheduled = false;
                }
                while (this.mParallelBroadcasts.size() > 0) {
                    BroadcastRecord r = this.mParallelBroadcasts.remove(0);
                    r.dispatchTime = SystemClock.uptimeMillis();
                    r.dispatchClockTime = System.currentTimeMillis();
                    int N = r.receivers.size();
                    if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        Slog.v(TAG_BROADCAST, "BDC-Processing parallel broadcast [" + this.mQueueName + "] " + r + ", " + N + " receivers");
                    }
                    for (int i = 0; i < N; i++) {
                        Object target = r.receivers.get(i);
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Delivering non-ordered on [" + this.mQueueName + "] to registered " + target + ": " + r);
                        }
                        deliverToRegisteredReceiverLocked(r, (BroadcastFilter) target, false, i);
                    }
                    addBroadcastToHistoryLocked(r);
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        Slog.v(TAG_BROADCAST, "Done with parallel broadcast [" + this.mQueueName + "] " + r);
                    }
                }
                if (this.mPendingBroadcast != null) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: waiting for " + this.mPendingBroadcast.curApp);
                    }
                    synchronized (this.mService.mPidsSelfLocked) {
                        ProcessRecord proc = this.mService.mPidsSelfLocked.get(this.mPendingBroadcast.curApp.pid);
                        z = proc != null ? proc.crashing : true;
                    }
                    if (!z) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    Slog.w(TAG, "pending app  [" + this.mQueueName + "]" + this.mPendingBroadcast.curApp + " died before responding to broadcast");
                    this.mPendingBroadcast.state = 0;
                    this.mPendingBroadcast.nextReceiver = this.mPendingBroadcastRecvIndex;
                    this.mPendingBroadcast = null;
                }
                boolean looped = false;
                while (this.mOrderedBroadcasts.size() != 0) {
                    BroadcastRecord r2 = this.mOrderedBroadcasts.get(0);
                    boolean forceReceive = false;
                    int numReceivers = r2.receivers != null ? r2.receivers.size() : 0;
                    if (this.mService.mProcessesReady && r2.dispatchTime > 0) {
                        long now = SystemClock.uptimeMillis();
                        if (numReceivers > 0 && now > r2.dispatchTime + (this.mTimeoutPeriod * 2 * ((long) numReceivers))) {
                            ActivityManagerService activityManagerService = this.mService;
                            if (!ActivityManagerService.mANRManager.isAnrDeferrable()) {
                                Slog.w(TAG, "Hung broadcast [" + this.mQueueName + "] discarded after timeout failure: now=" + now + " dispatchTime=" + r2.dispatchTime + " startTime=" + r2.receiverTime + " intent=" + r2.intent + " numReceivers=" + numReceivers + " nextReceiver=" + r2.nextReceiver + " state=" + r2.state);
                                broadcastTimeoutLocked(false);
                                forceReceive = true;
                                r2.state = 0;
                            }
                        }
                    }
                    if (r2.state != 0) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.d(TAG_BROADCAST, "processNextBroadcast(" + this.mQueueName + ") called when not idle (state=" + r2.state + ")");
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (r2.receivers == null || r2.nextReceiver >= numReceivers || r2.resultAbort || forceReceive) {
                        if (r2.resultTo != null) {
                            try {
                                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                    Slog.i(TAG_BROADCAST, "BDC-Finishing broadcast [" + this.mQueueName + "] " + r2.intent.getAction() + " app=" + r2.callerApp + " receiver=" + r2.resultTo);
                                }
                                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                    AMEventHookData.ReadyToStartDynamicReceiver eventData = AMEventHookData.ReadyToStartDynamicReceiver.createInstance();
                                    eventData.set(new Object[]{r2.callerPackage, r2.callerPackage, Integer.valueOf(r2.callingUid)});
                                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartDynamicReceiver, eventData);
                                }
                                performReceiveLocked(r2.callerApp, r2.resultTo, new Intent(r2.intent), r2.resultCode, r2.resultData, r2.resultExtras, false, false, r2.userId);
                                r2.resultTo = null;
                            } catch (RemoteException e) {
                                r2.resultTo = null;
                                Slog.w(TAG, "Failure [" + this.mQueueName + "] sending broadcast result of " + r2.intent, e);
                            }
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Cancelling BROADCAST_TIMEOUT_MSG");
                        }
                        cancelBroadcastTimeoutLocked();
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                            Slog.v(TAG_BROADCAST, "Finished with ordered broadcast " + r2);
                        }
                        addBroadcastToHistoryLocked(r2);
                        if (r2.intent.getComponent() == null && r2.intent.getPackage() == null && (r2.intent.getFlags() & 1073741824) == 0) {
                            this.mService.addBroadcastStatLocked(r2.intent.getAction(), r2.callerPackage, r2.manifestCount, r2.manifestSkipCount, r2.finishTime - r2.dispatchTime);
                        }
                        this.mOrderedBroadcasts.remove(0);
                        r2 = null;
                        looped = true;
                    }
                    if (r2 != null) {
                        int recIdx = r2.nextReceiver;
                        r2.nextReceiver = recIdx + 1;
                        r2.receiverTime = SystemClock.uptimeMillis();
                        if (recIdx == 0) {
                            r2.dispatchTime = r2.receiverTime;
                            r2.dispatchClockTime = System.currentTimeMillis();
                            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                                Slog.v(TAG_BROADCAST, "BDC-Processing ordered broadcast [" + this.mQueueName + "] " + r2 + ", " + r2.receivers.size() + " receivers");
                            }
                        }
                        if (!this.mPendingBroadcastTimeoutMessage) {
                            long timeoutTime = r2.receiverTime + this.mTimeoutPeriod;
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                Slog.v(TAG_BROADCAST, "Submitting BROADCAST_TIMEOUT_MSG [" + this.mQueueName + "] for " + r2 + " at " + timeoutTime);
                            }
                            setBroadcastTimeoutLocked(timeoutTime);
                        }
                        BroadcastOptions brOptions = r2.options;
                        Object nextReceiver = r2.receivers.get(recIdx);
                        if (nextReceiver instanceof BroadcastFilter) {
                            BroadcastFilter filter = (BroadcastFilter) nextReceiver;
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                Slog.v(TAG_BROADCAST, "Delivering ordered [" + this.mQueueName + "] to registered " + filter + ": " + r2);
                            }
                            deliverToRegisteredReceiverLocked(r2, filter, r2.ordered, recIdx);
                            if (r2.receiver == null || !r2.ordered) {
                                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                    Slog.v(TAG_BROADCAST, "Quick finishing [" + this.mQueueName + "]: ordered=" + r2.ordered + " receiver=" + r2.receiver);
                                }
                                r2.state = 0;
                                scheduleBroadcastsLocked();
                            } else if (brOptions != null && brOptions.getTemporaryAppWhitelistDuration() > 0) {
                                scheduleTempWhitelistLocked(filter.owningUid, brOptions.getTemporaryAppWhitelistDuration(), r2);
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        ResolveInfo info = (ResolveInfo) nextReceiver;
                        ComponentName component = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                        if (ActivityManagerService.IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.d(TAG_BROADCAST, r2 + ", #" + recIdx + " " + info.activityInfo);
                        }
                        boolean skip = false;
                        if (brOptions != null && (info.activityInfo.applicationInfo.targetSdkVersion < brOptions.getMinManifestReceiverApiLevel() || info.activityInfo.applicationInfo.targetSdkVersion > brOptions.getMaxManifestReceiverApiLevel())) {
                            skip = true;
                        }
                        int perm2 = this.mService.checkComponentPermission(info.activityInfo.permission, r2.callingPid, r2.callingUid, info.activityInfo.applicationInfo.uid, info.activityInfo.exported);
                        if (!skip && perm2 != 0) {
                            if (info.activityInfo.exported) {
                                Slog.w(TAG, "Permission Denial: broadcasting " + r2.intent.toString() + " from " + r2.callerPackage + " (pid=" + r2.callingPid + ", uid=" + r2.callingUid + ") requires " + info.activityInfo.permission + " due to receiver " + component.flattenToShortString());
                            } else {
                                Slog.w(TAG, "Permission Denial: broadcasting " + r2.intent.toString() + " from " + r2.callerPackage + " (pid=" + r2.callingPid + ", uid=" + r2.callingUid + ") is not exported from uid " + info.activityInfo.applicationInfo.uid + " due to receiver " + component.flattenToShortString());
                            }
                            skip = true;
                        } else if (!skip && info.activityInfo.permission != null && (opCode = AppOpsManager.permissionToOpCode(info.activityInfo.permission)) != -1 && this.mService.mAppOpsService.noteOperation(opCode, r2.callingUid, r2.callerPackage) != 0) {
                            Slog.w(TAG, "Appop Denial: broadcasting " + r2.intent.toString() + " from " + r2.callerPackage + " (pid=" + r2.callingPid + ", uid=" + r2.callingUid + ") requires appop " + AppOpsManager.permissionToOp(info.activityInfo.permission) + " due to registered receiver " + component.flattenToShortString());
                            skip = true;
                        }
                        if (!skip && info.activityInfo.applicationInfo.uid != 1000 && r2.requiredPermissions != null && r2.requiredPermissions.length > 0) {
                            int i2 = 0;
                            while (true) {
                                if (i2 >= r2.requiredPermissions.length) {
                                    break;
                                }
                                String requiredPermission = r2.requiredPermissions[i2];
                                try {
                                    perm = AppGlobals.getPackageManager().checkPermission(requiredPermission, info.activityInfo.applicationInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                                } catch (RemoteException e2) {
                                    perm = -1;
                                }
                                if (perm != 0) {
                                    Slog.w(TAG, "Permission Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires " + requiredPermission + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                                    skip = true;
                                    break;
                                }
                                int appOp = AppOpsManager.permissionToOpCode(requiredPermission);
                                if (appOp != -1 && appOp != r2.appOp && this.mService.mAppOpsService.noteOperation(appOp, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName) != 0) {
                                    Slog.w(TAG, "Appop Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires appop " + AppOpsManager.permissionToOp(requiredPermission) + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                                    skip = true;
                                    break;
                                }
                                i2++;
                            }
                        }
                        if (!skip && r2.appOp != -1 && this.mService.mAppOpsService.noteOperation(r2.appOp, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName) != 0) {
                            Slog.w(TAG, "Appop Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires appop " + AppOpsManager.opToName(r2.appOp) + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                            skip = true;
                        }
                        if (!skip) {
                            skip = !this.mService.mIntentFirewall.checkBroadcast(r2.intent, r2.callingUid, r2.callingPid, r2.resolvedType, info.activityInfo.applicationInfo.uid);
                        }
                        boolean isSingleton = false;
                        try {
                            isSingleton = this.mService.isSingleton(info.activityInfo.processName, info.activityInfo.applicationInfo, info.activityInfo.name, info.activityInfo.flags);
                        } catch (SecurityException e3) {
                            Slog.w(TAG, e3.getMessage());
                            skip = true;
                        }
                        if ((info.activityInfo.flags & 1073741824) != 0 && ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS", info.activityInfo.applicationInfo.uid) != 0) {
                            Slog.w(TAG, "Permission Denial: Receiver " + component.flattenToShortString() + " requests FLAG_SINGLE_USER, but app does not hold android.permission.INTERACT_ACROSS_USERS");
                            skip = true;
                        }
                        if (skip) {
                            r2.manifestSkipCount++;
                        } else {
                            r2.manifestCount++;
                        }
                        if (r2.curApp != null && r2.curApp.crashing) {
                            Slog.w(TAG, "Skipping deliver ordered [" + this.mQueueName + "] " + r2 + " to " + r2.curApp + ": process crashing");
                            skip = true;
                        }
                        if (!skip) {
                            boolean isAvailable = false;
                            try {
                                isAvailable = AppGlobals.getPackageManager().isPackageAvailable(info.activityInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid));
                            } catch (Exception e4) {
                                Slog.w(TAG, "Exception getting recipient info for " + info.activityInfo.packageName, e4);
                            }
                            if (!isAvailable) {
                                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                    Slog.v(TAG_BROADCAST, "Skipping delivery to " + info.activityInfo.packageName + " / " + info.activityInfo.applicationInfo.uid + " : package no longer available");
                                }
                                skip = true;
                            }
                        }
                        if (Build.isPermissionReviewRequired() && !skip && !requestStartTargetPermissionsReviewIfNeededLocked(r2, info.activityInfo.packageName, UserHandle.getUserId(info.activityInfo.applicationInfo.uid))) {
                            skip = true;
                        }
                        int receiverUid = info.activityInfo.applicationInfo.uid;
                        if (r2.callingUid != 1000 && isSingleton && this.mService.isValidSingletonCall(r2.callingUid, receiverUid)) {
                            info.activityInfo = this.mService.getActivityInfoForUser(info.activityInfo, 0);
                        }
                        String targetProcess = info.activityInfo.processName;
                        ProcessRecord app = this.mService.getProcessRecordLocked(targetProcess, info.activityInfo.applicationInfo.uid, false);
                        if (!skip && (allowed = this.mService.checkAllowBackgroundLocked(info.activityInfo.applicationInfo.uid, info.activityInfo.packageName, -1, false)) != 0) {
                            if (allowed == 2) {
                                Slog.w(TAG, "Background execution disabled: receiving " + r2.intent + " to " + component.flattenToShortString());
                                skip = true;
                            } else if ((r2.intent.getFlags() & 8388608) != 0 || (r2.intent.getComponent() == null && r2.intent.getPackage() == null && (r2.intent.getFlags() & 16777216) == 0)) {
                                Slog.w(TAG, "Background execution not allowed: receiving " + r2.intent + " to " + component.flattenToShortString());
                                skip = true;
                            }
                        }
                        if (skip) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                Slog.v(TAG_BROADCAST, "Skipping delivery of ordered [" + this.mQueueName + "] " + r2 + " for whatever reason");
                            }
                            r2.delivery[recIdx] = 2;
                            r2.receiver = null;
                            r2.curFilter = null;
                            r2.state = 0;
                            scheduleBroadcastsLocked();
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        r2.delivery[recIdx] = 1;
                        r2.state = 1;
                        r2.curComponent = component;
                        r2.curReceiver = info.activityInfo;
                        if (ActivityManagerDebugConfig.DEBUG_MU && r2.callingUid > 100000) {
                            Slog.v(TAG_MU, "Updated broadcast record activity info for secondary user, " + info.activityInfo + ", callingUid = " + r2.callingUid + ", uid = " + info.activityInfo.applicationInfo.uid);
                        }
                        if (brOptions != null && brOptions.getTemporaryAppWhitelistDuration() > 0) {
                            scheduleTempWhitelistLocked(receiverUid, brOptions.getTemporaryAppWhitelistDuration(), r2);
                        }
                        try {
                            AppGlobals.getPackageManager().setPackageStoppedState(r2.curComponent.getPackageName(), false, UserHandle.getUserId(r2.callingUid));
                        } catch (RemoteException e5) {
                        } catch (IllegalArgumentException e6) {
                            Slog.w(TAG, "Failed trying to unstop package " + r2.curComponent.getPackageName() + ": " + e6);
                        }
                        if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                            AMEventHookData.PackageStoppedStatusChanged eventData1 = AMEventHookData.PackageStoppedStatusChanged.createInstance();
                            eventData1.set(new Object[]{r2.curComponent.getPackageName(), 0, "processNextBroadcast"});
                            this.mService.getAMEventHook().hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData1);
                        }
                        if (app != null && app.thread != null) {
                            try {
                                app.addPackage(info.activityInfo.packageName, info.activityInfo.applicationInfo.versionCode, this.mService.mProcessStats);
                                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                                    AMEventHookData.ReadyToStartStaticReceiver eventData2 = AMEventHookData.ReadyToStartStaticReceiver.createInstance();
                                    eventData2.set(new Object[]{info.activityInfo.packageName, r2.callerPackage, Integer.valueOf(r2.callingUid)});
                                    this.mService.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartStaticReceiver, eventData2);
                                }
                                processCurBroadcastLocked(r2, app);
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return;
                            } catch (RemoteException e7) {
                                Slog.w(TAG, "Exception when sending broadcast to " + r2.curComponent, e7);
                            } catch (RuntimeException e8) {
                                Slog.wtf(TAG, "Failed sending broadcast to " + r2.curComponent + " with " + r2.intent, e8);
                                logBroadcastReceiverDiscardLocked(r2);
                                finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                                scheduleBroadcastsLocked();
                                r2.state = 0;
                                ActivityManagerService.resetPriorityAfterLockedSection();
                                return;
                            }
                        }
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Need to start app [" + this.mQueueName + "] " + targetProcess + " for broadcast " + r2);
                        }
                        ProcessRecord processRecordStartProcessLocked = this.mService.startProcessLocked(targetProcess, info.activityInfo.applicationInfo, true, r2.intent.getFlags() | 4, "broadcast", r2.curComponent, (r2.intent.getFlags() & 33554432) != 0, false, false);
                        r2.curApp = processRecordStartProcessLocked;
                        if (processRecordStartProcessLocked != null) {
                            this.mPendingBroadcast = r2;
                            this.mPendingBroadcastRecvIndex = recIdx;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        } else {
                            Slog.w(TAG, "Unable to launch app " + info.activityInfo.applicationInfo.packageName + "/" + info.activityInfo.applicationInfo.uid + " for broadcast " + r2.intent + ": process is bad");
                            logBroadcastReceiverDiscardLocked(r2);
                            finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                            scheduleBroadcastsLocked();
                            r2.state = 0;
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                    }
                }
                this.mService.scheduleAppGcsLocked();
                if (looped) {
                    this.mService.updateOomAdjLocked();
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    final void setBroadcastTimeoutLocked(long timeoutTime) {
        if (this.mPendingBroadcastTimeoutMessage) {
            return;
        }
        Message msg = this.mHandler.obtainMessage(BROADCAST_TIMEOUT_MSG, this);
        this.mHandler.sendMessageAtTime(msg, timeoutTime);
        this.mPendingBroadcastTimeoutMessage = true;
        if (2 != ANRManager.enableANRDebuggingMechanism()) {
            return;
        }
        Message msg2 = this.mService.mAnrHandler.obtainMessage(ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG, this.mAnrBroadcastQueue);
        this.mService.mAnrHandler.sendMessageAtTime(msg2, timeoutTime - (this.mTimeoutPeriod / 2));
    }

    final void cancelBroadcastTimeoutLocked() {
        if (!this.mPendingBroadcastTimeoutMessage) {
            return;
        }
        this.mHandler.removeMessages(BROADCAST_TIMEOUT_MSG, this);
        this.mPendingBroadcastTimeoutMessage = false;
        if (2 != ANRManager.enableANRDebuggingMechanism()) {
            return;
        }
        this.mService.mAnrHandler.removeMessages(ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG, this.mAnrBroadcastQueue);
    }

    final void broadcastTimeoutLocked(boolean fromMsg) {
        if (fromMsg) {
            this.mPendingBroadcastTimeoutMessage = false;
            if (2 == ANRManager.enableANRDebuggingMechanism()) {
                this.mService.mAnrHandler.removeMessages(ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG, this.mAnrBroadcastQueue);
            }
        }
        if (this.mOrderedBroadcasts.size() == 0) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        BroadcastRecord r = this.mOrderedBroadcasts.get(0);
        if (fromMsg) {
            if (this.mService.mProcessesReady) {
                ActivityManagerService activityManagerService = this.mService;
                if (ActivityManagerService.mANRManager.isAnrDeferrable()) {
                    Slog.d(TAG, "Skip BROADCAST_TIMEOUT ANR: " + r);
                    this.mService.mDidDexOpt = true;
                }
            }
            if (this.mService.mDidDexOpt) {
                this.mService.mDidDexOpt = false;
                setBroadcastTimeoutLocked(SystemClock.uptimeMillis() + this.mTimeoutPeriod);
                return;
            } else {
                if (!this.mService.mProcessesReady) {
                    return;
                }
                long timeoutTime = r.receiverTime + this.mTimeoutPeriod;
                if (timeoutTime > now) {
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Premature timeout [" + this.mQueueName + "] @ " + now + ": resetting BROADCAST_TIMEOUT_MSG for " + timeoutTime);
                    }
                    setBroadcastTimeoutLocked(timeoutTime);
                    return;
                }
            }
        }
        BroadcastRecord br = this.mOrderedBroadcasts.get(0);
        if (br.state == 4) {
            Slog.i(TAG, "Waited long enough for: " + (br.curComponent != null ? br.curComponent.flattenToShortString() : "(null)"));
            br.curComponent = null;
            br.state = 0;
            processNextBroadcast(false);
            return;
        }
        Slog.w(TAG, "Timeout of broadcast " + r + " - receiver=" + r.receiver + ", started " + (now - r.receiverTime) + "ms ago");
        r.receiverTime = now;
        r.anrCount++;
        if (r.nextReceiver <= 0) {
            Slog.w(TAG, "Timeout on receiver with nextReceiver <= 0");
            return;
        }
        ProcessRecord app = null;
        Object curReceiver = r.receivers.get(r.nextReceiver - 1);
        r.delivery[r.nextReceiver - 1] = 3;
        Slog.w(TAG, "Receiver during timeout: " + curReceiver);
        logBroadcastReceiverDiscardLocked(r);
        if (curReceiver instanceof BroadcastFilter) {
            BroadcastFilter bf = (BroadcastFilter) curReceiver;
            if (bf.receiverList.pid != 0 && bf.receiverList.pid != ActivityManagerService.MY_PID) {
                synchronized (this.mService.mPidsSelfLocked) {
                    app = this.mService.mPidsSelfLocked.get(bf.receiverList.pid);
                }
            }
        } else {
            app = r.curApp;
        }
        String anrMessage = app != null ? "Broadcast of " + r.intent.toString() : null;
        if (this.mPendingBroadcast == r) {
            this.mPendingBroadcast = null;
        }
        finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
        scheduleBroadcastsLocked();
        if (anrMessage != null) {
            this.mHandler.post(new AppNotResponding(app, anrMessage));
        }
    }

    private final int ringAdvance(int x, int increment, int ringSize) {
        int x2 = x + increment;
        if (x2 < 0) {
            return ringSize - 1;
        }
        if (x2 >= ringSize) {
            return 0;
        }
        return x2;
    }

    private final void addBroadcastToHistoryLocked(BroadcastRecord r) {
        if (r.callingUid < 0) {
            return;
        }
        r.finishTime = SystemClock.uptimeMillis();
        if (ActivityManagerService.IS_ENG_BUILD || ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.d(TAG_BROADCAST, (r.ordered ? "Ordered" : "Non-ordered") + " [" + this.mQueueName + "] " + r + ", " + (r.receivers != null ? Integer.valueOf(r.receivers.size()) : "null") + " receivers, Total: " + (r.finishTime - r.enqueueTime) + ", Waiting: " + (r.dispatchTime - r.enqueueTime) + ", Processing: " + (r.finishTime - r.dispatchTime));
        }
        this.mBroadcastHistory[this.mHistoryNext] = r;
        this.mHistoryNext = ringAdvance(this.mHistoryNext, 1, MAX_BROADCAST_HISTORY);
        this.mBroadcastSummaryHistory[this.mSummaryHistoryNext] = r.intent;
        this.mSummaryHistoryEnqueueTime[this.mSummaryHistoryNext] = r.enqueueClockTime;
        this.mSummaryHistoryDispatchTime[this.mSummaryHistoryNext] = r.dispatchClockTime;
        this.mSummaryHistoryFinishTime[this.mSummaryHistoryNext] = System.currentTimeMillis();
        this.mSummaryHistoryNext = ringAdvance(this.mSummaryHistoryNext, 1, MAX_BROADCAST_SUMMARY_HISTORY);
    }

    boolean cleanupDisabledPackageReceiversLocked(String packageName, Set<String> filterByClasses, int userId, boolean doit) {
        boolean didSomething = false;
        for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            didSomething |= this.mParallelBroadcasts.get(i).cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        for (int i2 = this.mOrderedBroadcasts.size() - 1; i2 >= 0; i2--) {
            didSomething |= this.mOrderedBroadcasts.get(i2).cleanupDisabledPackageReceiversLocked(packageName, filterByClasses, userId, doit);
            if (!doit && didSomething) {
                return true;
            }
        }
        return didSomething;
    }

    final void logBroadcastReceiverDiscardLocked(BroadcastRecord r) {
        int logIndex = r.nextReceiver - 1;
        if (logIndex >= 0 && logIndex < r.receivers.size()) {
            Object curReceiver = r.receivers.get(logIndex);
            if (curReceiver instanceof BroadcastFilter) {
                BroadcastFilter bf = (BroadcastFilter) curReceiver;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(bf.owningUserId), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), Integer.valueOf(System.identityHashCode(bf)));
                return;
            } else {
                ResolveInfo ri = (ResolveInfo) curReceiver;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, Integer.valueOf(UserHandle.getUserId(ri.activityInfo.applicationInfo.uid)), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(logIndex), ri.toString());
                return;
            }
        }
        if (logIndex < 0) {
            Slog.w(TAG, "Discarding broadcast before first receiver is invoked: " + r);
        }
        EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, -1, Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver), "NONE");
    }

    final boolean dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean needSep) {
        int ringIndex;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (this.mParallelBroadcasts.size() > 0 || this.mOrderedBroadcasts.size() > 0 || this.mPendingBroadcast != null) {
            boolean printed = false;
            for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
                BroadcastRecord br = this.mParallelBroadcasts.get(i);
                if (dumpPackage == null || dumpPackage.equals(br.callerPackage)) {
                    if (!printed) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        printed = true;
                        pw.println("  Active broadcasts [" + this.mQueueName + "]:");
                    }
                    pw.println("  Active Broadcast " + this.mQueueName + " #" + i + ":");
                    br.dump(pw, "    ", sdf);
                }
            }
            boolean printed2 = false;
            needSep = true;
            for (int i2 = this.mOrderedBroadcasts.size() - 1; i2 >= 0; i2--) {
                BroadcastRecord br2 = this.mOrderedBroadcasts.get(i2);
                if (dumpPackage == null || dumpPackage.equals(br2.callerPackage)) {
                    if (!printed2) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        printed2 = true;
                        pw.println("  Active ordered broadcasts [" + this.mQueueName + "]:");
                    }
                    pw.println("  Active Ordered Broadcast " + this.mQueueName + " #" + i2 + ":");
                    this.mOrderedBroadcasts.get(i2).dump(pw, "    ", sdf);
                }
            }
            if (dumpPackage == null || (this.mPendingBroadcast != null && dumpPackage.equals(this.mPendingBroadcast.callerPackage))) {
                if (needSep) {
                    pw.println();
                }
                pw.println("  Pending broadcast [" + this.mQueueName + "]:");
                if (this.mPendingBroadcast != null) {
                    this.mPendingBroadcast.dump(pw, "    ", sdf);
                } else {
                    pw.println("    (null)");
                }
                needSep = true;
            }
        }
        boolean printed3 = false;
        int i3 = -1;
        int lastIndex = this.mHistoryNext;
        int ringIndex2 = lastIndex;
        do {
            ringIndex2 = ringAdvance(ringIndex2, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord r = this.mBroadcastHistory[ringIndex2];
            if (r != null) {
                i3++;
                if (dumpPackage == null || dumpPackage.equals(r.callerPackage)) {
                    if (!printed3) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Historical broadcasts [" + this.mQueueName + "]:");
                        printed3 = true;
                    }
                    if (dumpAll) {
                        pw.print("  Historical Broadcast " + this.mQueueName + " #");
                        pw.print(i3);
                        pw.println(":");
                        r.dump(pw, "    ", sdf);
                    } else {
                        pw.print("  #");
                        pw.print(i3);
                        pw.print(": ");
                        pw.println(r);
                        pw.print("    ");
                        pw.println(r.intent.toShortString(false, true, true, false));
                        if (r.targetComp != null && r.targetComp != r.intent.getComponent()) {
                            pw.print("    targetComp: ");
                            pw.println(r.targetComp.toShortString());
                        }
                        Bundle bundle = r.intent.getExtras();
                        if (bundle != null) {
                            pw.print("    extras: ");
                            pw.println(bundle.toString());
                        }
                    }
                }
            }
        } while (ringIndex2 != lastIndex);
        if (dumpPackage == null) {
            int ringIndex3 = this.mSummaryHistoryNext;
            if (dumpAll) {
                printed3 = false;
                i3 = -1;
                ringIndex = ringIndex3;
            } else {
                int j = i3;
                ringIndex = ringIndex3;
                while (j > 0 && ringIndex != ringIndex3) {
                    ringIndex = ringAdvance(ringIndex, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                    if (this.mBroadcastHistory[ringIndex] != null) {
                        j--;
                    }
                }
            }
            while (true) {
                ringIndex = ringAdvance(ringIndex, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                Intent intent = this.mBroadcastSummaryHistory[ringIndex];
                if (intent != null) {
                    if (!printed3) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Historical broadcasts summary [" + this.mQueueName + "]:");
                        printed3 = true;
                    }
                    if (!dumpAll && i3 >= 50) {
                        pw.println("  ...");
                        break;
                    }
                    i3++;
                    pw.print("  #");
                    pw.print(i3);
                    pw.print(": ");
                    pw.println(intent.toShortString(false, true, true, false));
                    pw.print("    ");
                    TimeUtils.formatDuration(this.mSummaryHistoryDispatchTime[ringIndex] - this.mSummaryHistoryEnqueueTime[ringIndex], pw);
                    pw.print(" dispatch ");
                    TimeUtils.formatDuration(this.mSummaryHistoryFinishTime[ringIndex] - this.mSummaryHistoryDispatchTime[ringIndex], pw);
                    pw.println(" finish");
                    pw.print("    enq=");
                    pw.print(sdf.format(new Date(this.mSummaryHistoryEnqueueTime[ringIndex])));
                    pw.print(" disp=");
                    pw.print(sdf.format(new Date(this.mSummaryHistoryDispatchTime[ringIndex])));
                    pw.print(" fin=");
                    pw.println(sdf.format(new Date(this.mSummaryHistoryFinishTime[ringIndex])));
                    Bundle bundle2 = intent.getExtras();
                    if (bundle2 != null) {
                        pw.print("    extras: ");
                        pw.println(bundle2.toString());
                    }
                }
                if (ringIndex == ringIndex3) {
                    break;
                }
            }
        }
        return needSep;
    }
}
