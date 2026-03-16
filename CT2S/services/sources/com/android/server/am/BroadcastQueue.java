package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Slog;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class BroadcastQueue {
    static final int BROADCAST_INTENT_MSG = 200;
    static final int BROADCAST_TIMEOUT_MSG = 201;
    static final boolean DEBUG_BROADCAST = false;
    static final boolean DEBUG_BROADCAST_LIGHT = false;
    static final boolean DEBUG_MU = false;
    static final int MAX_BROADCAST_HISTORY;
    static final int MAX_BROADCAST_SUMMARY_HISTORY;
    static final String TAG = "BroadcastQueue";
    static final String TAG_MU = "ActivityManagerServiceMU";
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
    final Intent[] mBroadcastSummaryHistory = new Intent[MAX_BROADCAST_SUMMARY_HISTORY];
    boolean mBroadcastsScheduled = false;
    BroadcastRecord mPendingBroadcast = null;

    static {
        MAX_BROADCAST_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 10 : 50;
        MAX_BROADCAST_SUMMARY_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 25 : 300;
    }

    private final class BroadcastHandler extends Handler {
        public BroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BroadcastQueue.BROADCAST_INTENT_MSG:
                    BroadcastQueue.this.processNextBroadcast(true);
                    return;
                case BroadcastQueue.BROADCAST_TIMEOUT_MSG:
                    synchronized (BroadcastQueue.this.mService) {
                        BroadcastQueue.this.broadcastTimeoutLocked(true);
                        break;
                    }
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
        public void run() throws Throwable {
            BroadcastQueue.this.mService.appNotResponding(this.mApp, null, null, false, this.mAnnotation);
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
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord r) {
        this.mOrderedBroadcasts.add(r);
    }

    public final boolean replaceParallelBroadcastLocked(BroadcastRecord r) {
        for (int i = this.mParallelBroadcasts.size() - 1; i >= 0; i--) {
            if (r.intent.filterEquals(this.mParallelBroadcasts.get(i).intent)) {
                this.mParallelBroadcasts.set(i, r);
                return true;
            }
        }
        return false;
    }

    public final boolean replaceOrderedBroadcastLocked(BroadcastRecord r) {
        for (int i = this.mOrderedBroadcasts.size() - 1; i > 0; i--) {
            if (r.intent.filterEquals(this.mOrderedBroadcasts.get(i).intent)) {
                this.mOrderedBroadcasts.set(i, r);
                return true;
            }
        }
        return false;
    }

    private final void processCurBroadcastLocked(BroadcastRecord broadcastRecord, ProcessRecord processRecord) throws RemoteException {
        if (processRecord.thread == null) {
            throw new RemoteException();
        }
        broadcastRecord.receiver = processRecord.thread.asBinder();
        broadcastRecord.curApp = processRecord;
        processRecord.curReceiver = broadcastRecord;
        processRecord.forceProcessStateUpTo(8);
        this.mService.updateLruProcessLocked(processRecord, false, null);
        this.mService.updateOomAdjLocked();
        broadcastRecord.intent.setComponent(broadcastRecord.curComponent);
        boolean z = false;
        try {
            this.mService.ensurePackageDexOpt(broadcastRecord.intent.getComponent().getPackageName());
            processRecord.thread.scheduleReceiver(new Intent(broadcastRecord.intent), broadcastRecord.curReceiver, this.mService.compatibilityInfoForPackageLocked(broadcastRecord.curReceiver.applicationInfo), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.userId, processRecord.repProcState);
            boolean z2 = true;
        } finally {
            if (!z) {
                broadcastRecord.receiver = null;
                broadcastRecord.curApp = null;
                processRecord.curReceiver = null;
            }
        }
    }

    public boolean sendPendingBroadcastsLocked(ProcessRecord app) {
        boolean didSomething = false;
        BroadcastRecord br = this.mPendingBroadcast;
        if (br != null && br.curApp.pid == app.pid) {
            if (br.curApp != app) {
                Slog.e(TAG, "App mismatch when sending pending broadcast to " + app.processName + ", intended target is " + br.curApp.processName);
                return false;
            }
            try {
                this.mPendingBroadcast = null;
                processCurBroadcastLocked(br, app);
                didSomething = true;
            } catch (Exception e) {
                Slog.w(TAG, "Exception in new application when starting receiver " + br.curComponent.flattenToShortString(), e);
                logBroadcastReceiverDiscardLocked(br);
                finishReceiverLocked(br, br.resultCode, br.resultData, br.resultExtras, br.resultAbort, false);
                scheduleBroadcastsLocked();
                br.state = 0;
                throw new RuntimeException(e.getMessage());
            }
        }
        return didSomething;
    }

    public void skipPendingBroadcastLocked(int pid) {
        BroadcastRecord br = this.mPendingBroadcast;
        if (br != null && br.curApp.pid == pid) {
            br.state = 0;
            br.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
            scheduleBroadcastsLocked();
        }
    }

    public void skipCurrentReceiverLocked(ProcessRecord app) {
        boolean reschedule = false;
        BroadcastRecord r = app.curReceiver;
        if (r != null && r.queue == this) {
            logBroadcastReceiverDiscardLocked(r);
            finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
            reschedule = true;
        }
        BroadcastRecord r2 = this.mPendingBroadcast;
        if (r2 != null && r2.curApp == app) {
            logBroadcastReceiverDiscardLocked(r2);
            finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
            reschedule = true;
        }
        if (reschedule) {
            scheduleBroadcastsLocked();
        }
    }

    public void scheduleBroadcastsLocked() {
        if (!this.mBroadcastsScheduled) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(BROADCAST_INTENT_MSG, this));
            this.mBroadcastsScheduled = true;
        }
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
                Slog.i("ActivityManager", "Delay finish: " + r.curComponent.flattenToShortString());
                r.state = 4;
                return false;
            }
        }
        r.curComponent = null;
        return state == 1 || state == 3;
    }

    public void backgroundServicesFinishedLocked(int userId) {
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord br = this.mOrderedBroadcasts.get(0);
            if (br.userId == userId && br.state == 4) {
                Slog.i("ActivityManager", "Resuming delayed broadcast");
                br.curComponent = null;
                br.state = 0;
                processNextBroadcast(false);
            }
        }
    }

    private static void performReceiveLocked(ProcessRecord app, IIntentReceiver receiver, Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        if (app != null) {
            if (app.thread != null) {
                app.thread.scheduleRegisteredReceiver(receiver, intent, resultCode, data, extras, ordered, sticky, sendingUser, app.repProcState);
                return;
            }
            throw new RemoteException("app.thread must not be null");
        }
        receiver.performReceive(intent, resultCode, data, extras, ordered, sticky, sendingUser);
    }

    private final void deliverToRegisteredReceiverLocked(BroadcastRecord r, BroadcastFilter filter, boolean ordered) {
        boolean skip = false;
        if (filter.requiredPermission != null) {
            int perm = this.mService.checkComponentPermission(filter.requiredPermission, r.callingPid, r.callingUid, -1, true);
            if (perm != 0) {
                Slog.w(TAG, "Permission Denial: broadcasting " + r.intent.toString() + " from " + r.callerPackage + " (pid=" + r.callingPid + ", uid=" + r.callingUid + ") requires " + filter.requiredPermission + " due to registered receiver " + filter);
                skip = true;
            }
        }
        if (!skip && r.requiredPermission != null) {
            int perm2 = this.mService.checkComponentPermission(r.requiredPermission, filter.receiverList.pid, filter.receiverList.uid, -1, true);
            if (perm2 != 0) {
                Slog.w(TAG, "Permission Denial: receiving " + r.intent.toString() + " to " + filter.receiverList.app + " (pid=" + filter.receiverList.pid + ", uid=" + filter.receiverList.uid + ") requires " + r.requiredPermission + " due to sender " + r.callerPackage + " (uid " + r.callingUid + ")");
                skip = true;
            }
        }
        if (r.appOp != -1) {
            int mode = this.mService.mAppOpsService.noteOperation(r.appOp, filter.receiverList.uid, filter.packageName);
            if (mode != 0) {
                skip = true;
            }
        }
        if (!skip) {
            skip = !this.mService.mIntentFirewall.checkBroadcast(r.intent, r.callingUid, r.callingPid, r.resolvedType, filter.receiverList.uid);
        }
        if (filter.receiverList.app == null || filter.receiverList.app.crashing) {
            Slog.w(TAG, "Skipping deliver [" + this.mQueueName + "] " + r + " to " + filter.receiverList + ": process crashing");
            skip = true;
        }
        if (!skip) {
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
                performReceiveLocked(filter.receiverList.app, filter.receiverList.receiver, new Intent(r.intent), r.resultCode, r.resultData, r.resultExtras, r.ordered, r.initialSticky, r.userId);
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
    }

    final void processNextBroadcast(boolean fromMsg) {
        boolean looped;
        int perm;
        boolean isDead;
        synchronized (this.mService) {
            this.mService.updateCpuStats();
            if (fromMsg) {
                this.mBroadcastsScheduled = false;
            }
            while (this.mParallelBroadcasts.size() > 0) {
                BroadcastRecord r = this.mParallelBroadcasts.remove(0);
                r.dispatchTime = SystemClock.uptimeMillis();
                r.dispatchClockTime = System.currentTimeMillis();
                int N = r.receivers.size();
                for (int i = 0; i < N; i++) {
                    Object target = r.receivers.get(i);
                    deliverToRegisteredReceiverLocked(r, (BroadcastFilter) target, false);
                }
                addBroadcastToHistoryLocked(r);
            }
            if (this.mPendingBroadcast != null) {
                synchronized (this.mService.mPidsSelfLocked) {
                    ProcessRecord proc = this.mService.mPidsSelfLocked.get(this.mPendingBroadcast.curApp.pid);
                    isDead = proc == null || proc.crashing;
                }
                if (isDead) {
                    Slog.w(TAG, "pending app  [" + this.mQueueName + "]" + this.mPendingBroadcast.curApp + " died before responding to broadcast");
                    this.mPendingBroadcast.state = 0;
                    this.mPendingBroadcast.nextReceiver = this.mPendingBroadcastRecvIndex;
                    this.mPendingBroadcast = null;
                    looped = false;
                    while (this.mOrderedBroadcasts.size() != 0) {
                        BroadcastRecord r2 = this.mOrderedBroadcasts.get(0);
                        boolean forceReceive = false;
                        int numReceivers = r2.receivers != null ? r2.receivers.size() : 0;
                        if (this.mService.mProcessesReady && r2.dispatchTime > 0) {
                            long now = SystemClock.uptimeMillis();
                            if (numReceivers > 0 && now > r2.dispatchTime + (2 * this.mTimeoutPeriod * ((long) numReceivers))) {
                                Slog.w(TAG, "Hung broadcast [" + this.mQueueName + "] discarded after timeout failure: now=" + now + " dispatchTime=" + r2.dispatchTime + " startTime=" + r2.receiverTime + " intent=" + r2.intent + " numReceivers=" + numReceivers + " nextReceiver=" + r2.nextReceiver + " state=" + r2.state);
                                broadcastTimeoutLocked(false);
                                forceReceive = true;
                                r2.state = 0;
                            }
                        }
                        if (r2.state == 0) {
                            if (r2.receivers == null || r2.nextReceiver >= numReceivers || r2.resultAbort || forceReceive) {
                                if (r2.resultTo != null) {
                                    try {
                                        performReceiveLocked(r2.callerApp, r2.resultTo, new Intent(r2.intent), r2.resultCode, r2.resultData, r2.resultExtras, false, false, r2.userId);
                                        r2.resultTo = null;
                                    } catch (RemoteException e) {
                                        r2.resultTo = null;
                                        Slog.w(TAG, "Failure [" + this.mQueueName + "] sending broadcast result of " + r2.intent, e);
                                    }
                                }
                                cancelBroadcastTimeoutLocked();
                                addBroadcastToHistoryLocked(r2);
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
                                }
                                if (!this.mPendingBroadcastTimeoutMessage) {
                                    long timeoutTime = r2.receiverTime + this.mTimeoutPeriod;
                                    setBroadcastTimeoutLocked(timeoutTime);
                                }
                                Object nextReceiver = r2.receivers.get(recIdx);
                                if (nextReceiver instanceof BroadcastFilter) {
                                    BroadcastFilter filter = (BroadcastFilter) nextReceiver;
                                    deliverToRegisteredReceiverLocked(r2, filter, r2.ordered);
                                    if (r2.receiver == null || !r2.ordered) {
                                        r2.state = 0;
                                        scheduleBroadcastsLocked();
                                    }
                                    return;
                                }
                                ResolveInfo info = (ResolveInfo) nextReceiver;
                                ComponentName component = new ComponentName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                                boolean skip = false;
                                int perm2 = this.mService.checkComponentPermission(info.activityInfo.permission, r2.callingPid, r2.callingUid, info.activityInfo.applicationInfo.uid, info.activityInfo.exported);
                                if (perm2 != 0) {
                                    if (!info.activityInfo.exported) {
                                        Slog.w(TAG, "Permission Denial: broadcasting " + r2.intent.toString() + " from " + r2.callerPackage + " (pid=" + r2.callingPid + ", uid=" + r2.callingUid + ") is not exported from uid " + info.activityInfo.applicationInfo.uid + " due to receiver " + component.flattenToShortString());
                                    } else {
                                        Slog.w(TAG, "Permission Denial: broadcasting " + r2.intent.toString() + " from " + r2.callerPackage + " (pid=" + r2.callingPid + ", uid=" + r2.callingUid + ") requires " + info.activityInfo.permission + " due to receiver " + component.flattenToShortString());
                                    }
                                    skip = true;
                                }
                                if (info.activityInfo.applicationInfo.uid != 1000 && r2.requiredPermission != null) {
                                    try {
                                        perm = AppGlobals.getPackageManager().checkPermission(r2.requiredPermission, info.activityInfo.applicationInfo.packageName);
                                    } catch (RemoteException e2) {
                                        perm = -1;
                                    }
                                    if (perm != 0) {
                                        Slog.w(TAG, "Permission Denial: receiving " + r2.intent + " to " + component.flattenToShortString() + " requires " + r2.requiredPermission + " due to sender " + r2.callerPackage + " (uid " + r2.callingUid + ")");
                                        skip = true;
                                    }
                                }
                                if (r2.appOp != -1) {
                                    int mode = this.mService.mAppOpsService.noteOperation(r2.appOp, info.activityInfo.applicationInfo.uid, info.activityInfo.packageName);
                                    if (mode != 0) {
                                        skip = true;
                                    }
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
                                        skip = true;
                                    }
                                    if (!skip) {
                                        r2.receiver = null;
                                        r2.curFilter = null;
                                        r2.state = 0;
                                        scheduleBroadcastsLocked();
                                        return;
                                    }
                                    r2.state = 1;
                                    String targetProcess = info.activityInfo.processName;
                                    r2.curComponent = component;
                                    int receiverUid = info.activityInfo.applicationInfo.uid;
                                    if (r2.callingUid != 1000 && isSingleton && this.mService.isValidSingletonCall(r2.callingUid, receiverUid)) {
                                        info.activityInfo = this.mService.getActivityInfoForUser(info.activityInfo, 0);
                                    }
                                    r2.curReceiver = info.activityInfo;
                                    try {
                                        AppGlobals.getPackageManager().setPackageStoppedState(r2.curComponent.getPackageName(), false, UserHandle.getUserId(r2.callingUid));
                                    } catch (RemoteException e5) {
                                    } catch (IllegalArgumentException e6) {
                                        Slog.w(TAG, "Failed trying to unstop package " + r2.curComponent.getPackageName() + ": " + e6);
                                    }
                                    ProcessRecord app = this.mService.getProcessRecordLocked(targetProcess, info.activityInfo.applicationInfo.uid, false);
                                    if (app != null && app.thread != null) {
                                        try {
                                            try {
                                                app.addPackage(info.activityInfo.packageName, info.activityInfo.applicationInfo.versionCode, this.mService.mProcessStats);
                                                processCurBroadcastLocked(r2, app);
                                                return;
                                            } catch (RemoteException e7) {
                                                Slog.w(TAG, "Exception when sending broadcast to " + r2.curComponent, e7);
                                            }
                                        } catch (RuntimeException e8) {
                                            Slog.wtf(TAG, "Failed sending broadcast to " + r2.curComponent + " with " + r2.intent, e8);
                                            logBroadcastReceiverDiscardLocked(r2);
                                            finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                                            scheduleBroadcastsLocked();
                                            r2.state = 0;
                                            return;
                                        }
                                    }
                                    ProcessRecord processRecordStartProcessLocked = this.mService.startProcessLocked(targetProcess, info.activityInfo.applicationInfo, true, r2.intent.getFlags() | 4, "broadcast", r2.curComponent, (r2.intent.getFlags() & 33554432) != 0, false, false);
                                    r2.curApp = processRecordStartProcessLocked;
                                    if (processRecordStartProcessLocked == null) {
                                        Slog.w(TAG, "Unable to launch app " + info.activityInfo.applicationInfo.packageName + "/" + info.activityInfo.applicationInfo.uid + " for broadcast " + r2.intent + ": process is bad");
                                        logBroadcastReceiverDiscardLocked(r2);
                                        finishReceiverLocked(r2, r2.resultCode, r2.resultData, r2.resultExtras, r2.resultAbort, false);
                                        scheduleBroadcastsLocked();
                                        r2.state = 0;
                                        return;
                                    }
                                    this.mPendingBroadcast = r2;
                                    this.mPendingBroadcastRecvIndex = recIdx;
                                    return;
                                }
                                if (!skip) {
                                }
                            }
                        } else {
                            return;
                        }
                    }
                    this.mService.scheduleAppGcsLocked();
                    if (looped) {
                        this.mService.updateOomAdjLocked();
                    }
                    return;
                }
                return;
            }
            looped = false;
            while (this.mOrderedBroadcasts.size() != 0) {
            }
            this.mService.scheduleAppGcsLocked();
            if (looped) {
            }
            return;
        }
    }

    final void setBroadcastTimeoutLocked(long timeoutTime) {
        if (!this.mPendingBroadcastTimeoutMessage) {
            Message msg = this.mHandler.obtainMessage(BROADCAST_TIMEOUT_MSG, this);
            this.mHandler.sendMessageAtTime(msg, timeoutTime);
            this.mPendingBroadcastTimeoutMessage = true;
        }
    }

    final void cancelBroadcastTimeoutLocked() {
        if (this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.removeMessages(BROADCAST_TIMEOUT_MSG, this);
            this.mPendingBroadcastTimeoutMessage = false;
        }
    }

    final void broadcastTimeoutLocked(boolean fromMsg) {
        if (fromMsg) {
            this.mPendingBroadcastTimeoutMessage = false;
        }
        if (this.mOrderedBroadcasts.size() != 0) {
            long now = SystemClock.uptimeMillis();
            BroadcastRecord r = this.mOrderedBroadcasts.get(0);
            if (fromMsg) {
                if (this.mService.mDidDexOpt) {
                    this.mService.mDidDexOpt = false;
                    setBroadcastTimeoutLocked(SystemClock.uptimeMillis() + this.mTimeoutPeriod);
                    return;
                } else if (this.mService.mProcessesReady) {
                    long timeoutTime = r.receiverTime + this.mTimeoutPeriod;
                    if (timeoutTime > now) {
                        setBroadcastTimeoutLocked(timeoutTime);
                        return;
                    }
                } else {
                    return;
                }
            }
            BroadcastRecord br = this.mOrderedBroadcasts.get(0);
            if (br.state == 4) {
                Slog.i("ActivityManager", "Waited long enough for: " + (br.curComponent != null ? br.curComponent.flattenToShortString() : "(null)"));
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
            String anrMessage = null;
            Object curReceiver = r.receivers.get(r.nextReceiver - 1);
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
            if (app != null) {
                anrMessage = "Broadcast of " + r.intent.toString();
            }
            if (this.mPendingBroadcast == r) {
                this.mPendingBroadcast = null;
            }
            finishReceiverLocked(r, r.resultCode, r.resultData, r.resultExtras, r.resultAbort, false);
            scheduleBroadcastsLocked();
            if (anrMessage != null) {
                this.mHandler.post(new AppNotResponding(app, anrMessage));
            }
        }
    }

    private final void addBroadcastToHistoryLocked(BroadcastRecord r) {
        if (r.callingUid >= 0) {
            System.arraycopy(this.mBroadcastHistory, 0, this.mBroadcastHistory, 1, MAX_BROADCAST_HISTORY - 1);
            r.finishTime = SystemClock.uptimeMillis();
            this.mBroadcastHistory[0] = r;
            System.arraycopy(this.mBroadcastSummaryHistory, 0, this.mBroadcastSummaryHistory, 1, MAX_BROADCAST_SUMMARY_HISTORY - 1);
            this.mBroadcastSummaryHistory[0] = r.intent;
        }
    }

    final void logBroadcastReceiverDiscardLocked(BroadcastRecord r) {
        if (r.nextReceiver > 0) {
            Object curReceiver = r.receivers.get(r.nextReceiver - 1);
            if (curReceiver instanceof BroadcastFilter) {
                BroadcastFilter bf = (BroadcastFilter) curReceiver;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(bf.owningUserId), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver - 1), Integer.valueOf(System.identityHashCode(bf)));
                return;
            } else {
                ResolveInfo ri = (ResolveInfo) curReceiver;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, Integer.valueOf(UserHandle.getUserId(ri.activityInfo.applicationInfo.uid)), Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver - 1), ri.toString());
                return;
            }
        }
        Slog.w(TAG, "Discarding broadcast before first receiver is invoked: " + r);
        EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, -1, Integer.valueOf(System.identityHashCode(r)), r.intent.getAction(), Integer.valueOf(r.nextReceiver), "NONE");
    }

    final boolean dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage, boolean needSep) {
        Intent intent;
        BroadcastRecord r;
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
                    br.dump(pw, "    ");
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
                    this.mOrderedBroadcasts.get(i2).dump(pw, "    ");
                }
            }
            if (dumpPackage == null || (this.mPendingBroadcast != null && dumpPackage.equals(this.mPendingBroadcast.callerPackage))) {
                if (needSep) {
                    pw.println();
                }
                pw.println("  Pending broadcast [" + this.mQueueName + "]:");
                if (this.mPendingBroadcast != null) {
                    this.mPendingBroadcast.dump(pw, "    ");
                } else {
                    pw.println("    (null)");
                }
                needSep = true;
            }
        }
        boolean printed3 = false;
        int i3 = 0;
        while (i3 < MAX_BROADCAST_HISTORY && (r = this.mBroadcastHistory[i3]) != null) {
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
                    r.dump(pw, "    ");
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
            i3++;
        }
        if (dumpPackage == null) {
            if (dumpAll) {
                i3 = 0;
                printed3 = false;
            }
            while (true) {
                if (i3 >= MAX_BROADCAST_SUMMARY_HISTORY || (intent = this.mBroadcastSummaryHistory[i3]) == null) {
                    break;
                }
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
                pw.print("  #");
                pw.print(i3);
                pw.print(": ");
                pw.println(intent.toShortString(false, true, true, false));
                Bundle bundle2 = intent.getExtras();
                if (bundle2 != null) {
                    pw.print("    extras: ");
                    pw.println(bundle2.toString());
                }
                i3++;
            }
        }
        return needSep;
    }
}
