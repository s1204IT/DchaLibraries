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
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.display.DisplayTransformManager;
import com.android.server.pm.DumpState;
import com.android.server.pm.PackageManagerService;
import com.android.server.slice.SliceClientPermissions;
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

    static {
        MAX_BROADCAST_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 10 : 50;
        MAX_BROADCAST_SUMMARY_HISTORY = ActivityManager.isLowRamDeviceStatic() ? 25 : DisplayTransformManager.LEVEL_COLOR_MATRIX_INVERT_COLOR;
    }

    private final class BroadcastHandler extends Handler {
        public BroadcastHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 200:
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
                        break;
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }
    }

    private final class AppNotResponding implements Runnable {
        private final String mAnnotation;
        private final ProcessRecord mApp;

        public AppNotResponding(ProcessRecord processRecord, String str) {
            this.mApp = processRecord;
            this.mAnnotation = str;
        }

        @Override
        public void run() {
            BroadcastQueue.this.mService.mAppErrors.appNotResponding(this.mApp, null, null, false, this.mAnnotation);
        }
    }

    BroadcastQueue(ActivityManagerService activityManagerService, Handler handler, String str, long j, boolean z) {
        this.mService = activityManagerService;
        this.mHandler = new BroadcastHandler(handler.getLooper());
        this.mQueueName = str;
        this.mTimeoutPeriod = j;
        this.mDelayBehindServices = z;
    }

    public String toString() {
        return this.mQueueName;
    }

    public boolean isPendingBroadcastProcessLocked(int i) {
        return this.mPendingBroadcast != null && this.mPendingBroadcast.curApp.pid == i;
    }

    public void enqueueParallelBroadcastLocked(BroadcastRecord broadcastRecord) {
        this.mParallelBroadcasts.add(broadcastRecord);
        enqueueBroadcastHelper(broadcastRecord);
    }

    public void enqueueOrderedBroadcastLocked(BroadcastRecord broadcastRecord) {
        this.mOrderedBroadcasts.add(broadcastRecord);
        enqueueBroadcastHelper(broadcastRecord);
    }

    private void enqueueBroadcastHelper(BroadcastRecord broadcastRecord) {
        broadcastRecord.enqueueClockTime = System.currentTimeMillis();
        if (Trace.isTagEnabled(64L)) {
            Trace.asyncTraceBegin(64L, createBroadcastTraceTitle(broadcastRecord, 0), System.identityHashCode(broadcastRecord));
        }
    }

    public final BroadcastRecord replaceParallelBroadcastLocked(BroadcastRecord broadcastRecord) {
        return replaceBroadcastLocked(this.mParallelBroadcasts, broadcastRecord, "PARALLEL");
    }

    public final BroadcastRecord replaceOrderedBroadcastLocked(BroadcastRecord broadcastRecord) {
        return replaceBroadcastLocked(this.mOrderedBroadcasts, broadcastRecord, "ORDERED");
    }

    private BroadcastRecord replaceBroadcastLocked(ArrayList<BroadcastRecord> arrayList, BroadcastRecord broadcastRecord, String str) {
        Intent intent = broadcastRecord.intent;
        for (int size = arrayList.size() - 1; size > 0; size--) {
            BroadcastRecord broadcastRecord2 = arrayList.get(size);
            if (broadcastRecord2.userId == broadcastRecord.userId && intent.filterEquals(broadcastRecord2.intent)) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "***** DROPPING " + str + " [" + this.mQueueName + "]: " + intent);
                }
                arrayList.set(size, broadcastRecord);
                return broadcastRecord2;
            }
        }
        return null;
    }

    private final void processCurBroadcastLocked(BroadcastRecord broadcastRecord, ProcessRecord processRecord, boolean z) throws RemoteException {
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
        processRecord.curReceivers.add(broadcastRecord);
        processRecord.forceProcessStateUpTo(10);
        this.mService.updateLruProcessLocked(processRecord, false, null);
        if (!z) {
            this.mService.updateOomAdjLocked();
        }
        broadcastRecord.intent.setComponent(broadcastRecord.curComponent);
        try {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Delivering to component " + broadcastRecord.curComponent + ": " + broadcastRecord);
            }
            this.mService.notifyPackageUse(broadcastRecord.intent.getComponent().getPackageName(), 3);
            processRecord.thread.scheduleReceiver(new Intent(broadcastRecord.intent), broadcastRecord.curReceiver, this.mService.compatibilityInfoForPackageLocked(broadcastRecord.curReceiver.applicationInfo), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.userId, processRecord.repProcState);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "Process cur broadcast " + broadcastRecord + " DELIVERED for app " + processRecord);
            }
        } catch (Throwable th) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "Process cur broadcast " + broadcastRecord + ": NOT STARTED!");
            }
            broadcastRecord.receiver = null;
            broadcastRecord.curApp = null;
            processRecord.curReceivers.remove(broadcastRecord);
            throw th;
        }
    }

    public boolean sendPendingBroadcastsLocked(ProcessRecord processRecord) {
        BroadcastRecord broadcastRecord = this.mPendingBroadcast;
        if (broadcastRecord == null || broadcastRecord.curApp.pid <= 0 || broadcastRecord.curApp.pid != processRecord.pid) {
            return false;
        }
        if (broadcastRecord.curApp != processRecord) {
            Slog.e(TAG, "App mismatch when sending pending broadcast to " + processRecord.processName + ", intended target is " + broadcastRecord.curApp.processName);
            return false;
        }
        try {
            this.mPendingBroadcast = null;
            processCurBroadcastLocked(broadcastRecord, processRecord, false);
            return true;
        } catch (Exception e) {
            Slog.w(TAG, "Exception in new application when starting receiver " + broadcastRecord.curComponent.flattenToShortString(), e);
            logBroadcastReceiverDiscardLocked(broadcastRecord);
            finishReceiverLocked(broadcastRecord, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.resultAbort, false);
            scheduleBroadcastsLocked();
            broadcastRecord.state = 0;
            throw new RuntimeException(e.getMessage());
        }
    }

    public void skipPendingBroadcastLocked(int i) {
        BroadcastRecord broadcastRecord = this.mPendingBroadcast;
        if (broadcastRecord != null && broadcastRecord.curApp.pid == i) {
            broadcastRecord.state = 0;
            broadcastRecord.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
            scheduleBroadcastsLocked();
        }
    }

    public void skipCurrentReceiverLocked(ProcessRecord processRecord) {
        BroadcastRecord broadcastRecord;
        if (this.mOrderedBroadcasts.size() > 0) {
            broadcastRecord = this.mOrderedBroadcasts.get(0);
            if (broadcastRecord.curApp != processRecord) {
                broadcastRecord = null;
            }
        }
        if (broadcastRecord == null && this.mPendingBroadcast != null && this.mPendingBroadcast.curApp == processRecord) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                Slog.v(TAG_BROADCAST, "[" + this.mQueueName + "] skip & discard pending app " + broadcastRecord);
            }
            broadcastRecord = this.mPendingBroadcast;
        }
        if (broadcastRecord != null) {
            skipReceiverLocked(broadcastRecord);
        }
    }

    private void skipReceiverLocked(BroadcastRecord broadcastRecord) {
        logBroadcastReceiverDiscardLocked(broadcastRecord);
        finishReceiverLocked(broadcastRecord, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.resultAbort, false);
        scheduleBroadcastsLocked();
    }

    public void scheduleBroadcastsLocked() {
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "Schedule broadcasts [" + this.mQueueName + "]: current=" + this.mBroadcastsScheduled);
        }
        if (this.mBroadcastsScheduled) {
            return;
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(200, this));
        this.mBroadcastsScheduled = true;
    }

    public BroadcastRecord getMatchingOrderedReceiver(IBinder iBinder) {
        BroadcastRecord broadcastRecord;
        if (this.mOrderedBroadcasts.size() > 0 && (broadcastRecord = this.mOrderedBroadcasts.get(0)) != null && broadcastRecord.receiver == iBinder) {
            return broadcastRecord;
        }
        return null;
    }

    public boolean finishReceiverLocked(BroadcastRecord broadcastRecord, int i, String str, Bundle bundle, boolean z, boolean z2) {
        ActivityInfo activityInfo;
        int i2 = broadcastRecord.state;
        ActivityInfo activityInfo2 = broadcastRecord.curReceiver;
        broadcastRecord.state = 0;
        if (i2 == 0) {
            Slog.w(TAG, "finishReceiver [" + this.mQueueName + "] called but state is IDLE");
        }
        broadcastRecord.receiver = null;
        broadcastRecord.intent.setComponent(null);
        if (broadcastRecord.curApp != null && broadcastRecord.curApp.curReceivers.contains(broadcastRecord)) {
            broadcastRecord.curApp.curReceivers.remove(broadcastRecord);
        }
        if (broadcastRecord.curFilter != null) {
            broadcastRecord.curFilter.receiverList.curBroadcast = null;
        }
        broadcastRecord.curFilter = null;
        broadcastRecord.curReceiver = null;
        broadcastRecord.curApp = null;
        this.mPendingBroadcast = null;
        broadcastRecord.resultCode = i;
        broadcastRecord.resultData = str;
        broadcastRecord.resultExtras = bundle;
        if (z && (broadcastRecord.intent.getFlags() & 134217728) == 0) {
            broadcastRecord.resultAbort = z;
        } else {
            broadcastRecord.resultAbort = false;
        }
        if (z2 && broadcastRecord.curComponent != null && broadcastRecord.queue.mDelayBehindServices && broadcastRecord.queue.mOrderedBroadcasts.size() > 0 && broadcastRecord.queue.mOrderedBroadcasts.get(0) == broadcastRecord) {
            if (broadcastRecord.nextReceiver < broadcastRecord.receivers.size()) {
                Object obj = broadcastRecord.receivers.get(broadcastRecord.nextReceiver);
                activityInfo = obj instanceof ActivityInfo ? (ActivityInfo) obj : null;
            } else {
                activityInfo = null;
            }
            if ((activityInfo2 == null || activityInfo == null || activityInfo2.applicationInfo.uid != activityInfo.applicationInfo.uid || !activityInfo2.processName.equals(activityInfo.processName)) && this.mService.mServices.hasBackgroundServicesLocked(broadcastRecord.userId)) {
                Slog.i(TAG, "Delay finish: " + broadcastRecord.curComponent.flattenToShortString());
                broadcastRecord.state = 4;
                return false;
            }
        }
        broadcastRecord.curComponent = null;
        if (i2 != 1 && i2 != 3) {
            return false;
        }
        return true;
    }

    public void backgroundServicesFinishedLocked(int i) {
        if (this.mOrderedBroadcasts.size() > 0) {
            BroadcastRecord broadcastRecord = this.mOrderedBroadcasts.get(0);
            if (broadcastRecord.userId == i && broadcastRecord.state == 4) {
                Slog.i(TAG, "Resuming delayed broadcast");
                broadcastRecord.curComponent = null;
                broadcastRecord.state = 0;
                processNextBroadcast(false);
            }
        }
    }

    void performReceiveLocked(ProcessRecord processRecord, IIntentReceiver iIntentReceiver, Intent intent, int i, String str, Bundle bundle, boolean z, boolean z2, int i2) throws RemoteException {
        if (processRecord != null) {
            if (processRecord.thread != null) {
                try {
                    processRecord.thread.scheduleRegisteredReceiver(iIntentReceiver, intent, i, str, bundle, z, z2, i2, processRecord.repProcState);
                    return;
                } catch (RemoteException e) {
                    synchronized (this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            Slog.w(TAG, "Can't deliver broadcast to " + processRecord.processName + " (pid " + processRecord.pid + "). Crashing it.");
                            processRecord.scheduleCrash("can't deliver broadcast");
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw e;
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }
            }
            throw new RemoteException("app.thread must not be null");
        }
        iIntentReceiver.performReceive(intent, i, str, bundle, z, z2, i2);
    }

    private void deliverToRegisteredReceiverLocked(BroadcastRecord broadcastRecord, BroadcastFilter broadcastFilter, boolean z, int i) {
        boolean z2;
        if (broadcastFilter.requiredPermission != null) {
            if (this.mService.checkComponentPermission(broadcastFilter.requiredPermission, broadcastRecord.callingPid, broadcastRecord.callingUid, -1, true) != 0) {
                Slog.w(TAG, "Permission Denial: broadcasting " + broadcastRecord.intent.toString() + " from " + broadcastRecord.callerPackage + " (pid=" + broadcastRecord.callingPid + ", uid=" + broadcastRecord.callingUid + ") requires " + broadcastFilter.requiredPermission + " due to registered receiver " + broadcastFilter);
            } else {
                int iPermissionToOpCode = AppOpsManager.permissionToOpCode(broadcastFilter.requiredPermission);
                if (iPermissionToOpCode != -1 && this.mService.mAppOpsService.noteOperation(iPermissionToOpCode, broadcastRecord.callingUid, broadcastRecord.callerPackage) != 0) {
                    Slog.w(TAG, "Appop Denial: broadcasting " + broadcastRecord.intent.toString() + " from " + broadcastRecord.callerPackage + " (pid=" + broadcastRecord.callingPid + ", uid=" + broadcastRecord.callingUid + ") requires appop " + AppOpsManager.permissionToOp(broadcastFilter.requiredPermission) + " due to registered receiver " + broadcastFilter);
                }
                z2 = false;
            }
            z2 = true;
        } else {
            z2 = false;
        }
        if (!z2 && broadcastRecord.requiredPermissions != null && broadcastRecord.requiredPermissions.length > 0) {
            for (int i2 = 0; i2 < broadcastRecord.requiredPermissions.length; i2++) {
                String str = broadcastRecord.requiredPermissions[i2];
                if (this.mService.checkComponentPermission(str, broadcastFilter.receiverList.pid, broadcastFilter.receiverList.uid, -1, true) != 0) {
                    Slog.w(TAG, "Permission Denial: receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") requires " + str + " due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ")");
                } else {
                    int iPermissionToOpCode2 = AppOpsManager.permissionToOpCode(str);
                    if (iPermissionToOpCode2 != -1 && iPermissionToOpCode2 != broadcastRecord.appOp && this.mService.mAppOpsService.noteOperation(iPermissionToOpCode2, broadcastFilter.receiverList.uid, broadcastFilter.packageName) != 0) {
                        Slog.w(TAG, "Appop Denial: receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") requires appop " + AppOpsManager.permissionToOp(str) + " due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ")");
                    }
                }
                z2 = true;
            }
        }
        if (!z2 && ((broadcastRecord.requiredPermissions == null || broadcastRecord.requiredPermissions.length == 0) && this.mService.checkComponentPermission(null, broadcastFilter.receiverList.pid, broadcastFilter.receiverList.uid, -1, true) != 0)) {
            Slog.w(TAG, "Permission Denial: security check failed when receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ")");
            z2 = true;
        }
        if (!z2 && broadcastRecord.appOp != -1 && this.mService.mAppOpsService.noteOperation(broadcastRecord.appOp, broadcastFilter.receiverList.uid, broadcastFilter.packageName) != 0) {
            Slog.w(TAG, "Appop Denial: receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") requires appop " + AppOpsManager.opToName(broadcastRecord.appOp) + " due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ")");
            z2 = true;
        }
        if (!this.mService.mIntentFirewall.checkBroadcast(broadcastRecord.intent, broadcastRecord.callingUid, broadcastRecord.callingPid, broadcastRecord.resolvedType, broadcastFilter.receiverList.uid)) {
            z2 = true;
        }
        if (!z2 && (broadcastFilter.receiverList.app == null || broadcastFilter.receiverList.app.killed || broadcastFilter.receiverList.app.crashing)) {
            Slog.w(TAG, "Skipping deliver [" + this.mQueueName + "] " + broadcastRecord + " to " + broadcastFilter.receiverList + ": process gone or crashing");
            z2 = true;
        }
        boolean z3 = (broadcastRecord.intent.getFlags() & DumpState.DUMP_COMPILER_STATS) != 0;
        if (!z2 && !z3 && broadcastFilter.instantApp && broadcastFilter.receiverList.uid != broadcastRecord.callingUid) {
            Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ") not specifying FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS");
            z2 = true;
        }
        if (!z2 && !broadcastFilter.visibleToInstantApp && broadcastRecord.callerInstantApp && broadcastFilter.receiverList.uid != broadcastRecord.callingUid) {
            Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord.intent.toString() + " to " + broadcastFilter.receiverList.app + " (pid=" + broadcastFilter.receiverList.pid + ", uid=" + broadcastFilter.receiverList.uid + ") requires receiver be visible to instant apps due to sender " + broadcastRecord.callerPackage + " (uid " + broadcastRecord.callingUid + ")");
            z2 = true;
        }
        if (z2) {
            broadcastRecord.delivery[i] = 2;
            return;
        }
        if (this.mService.mPermissionReviewRequired && !requestStartTargetPermissionsReviewIfNeededLocked(broadcastRecord, broadcastFilter.packageName, broadcastFilter.owningUserId)) {
            broadcastRecord.delivery[i] = 2;
            return;
        }
        broadcastRecord.delivery[i] = 1;
        if (z) {
            broadcastRecord.receiver = broadcastFilter.receiverList.receiver.asBinder();
            broadcastRecord.curFilter = broadcastFilter;
            broadcastFilter.receiverList.curBroadcast = broadcastRecord;
            broadcastRecord.state = 2;
            if (broadcastFilter.receiverList.app != null) {
                broadcastRecord.curApp = broadcastFilter.receiverList.app;
                broadcastFilter.receiverList.app.curReceivers.add(broadcastRecord);
                this.mService.updateOomAdjLocked(broadcastRecord.curApp, true);
            }
        }
        try {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.i(TAG_BROADCAST, "Delivering to " + broadcastFilter + " : " + broadcastRecord);
            }
            if (broadcastFilter.receiverList.app != null && broadcastFilter.receiverList.app.inFullBackup) {
                if (z) {
                    skipReceiverLocked(broadcastRecord);
                }
            } else {
                performReceiveLocked(broadcastFilter.receiverList.app, broadcastFilter.receiverList.receiver, new Intent(broadcastRecord.intent), broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.ordered, broadcastRecord.initialSticky, broadcastRecord.userId);
            }
            if (z) {
                broadcastRecord.state = 3;
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure sending broadcast " + broadcastRecord.intent, e);
            if (z) {
                broadcastRecord.receiver = null;
                broadcastRecord.curFilter = null;
                broadcastFilter.receiverList.curBroadcast = null;
                if (broadcastFilter.receiverList.app != null) {
                    broadcastFilter.receiverList.app.curReceivers.remove(broadcastRecord);
                }
            }
        }
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(BroadcastRecord broadcastRecord, String str, final int i) {
        if (!this.mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(str, i)) {
            return true;
        }
        if (!(broadcastRecord.callerApp == null || broadcastRecord.callerApp.setSchedGroup != 0) || broadcastRecord.intent.getComponent() == null) {
            Slog.w(TAG, "u" + i + " Receiving a broadcast in package" + str + " requires a permissions review");
        } else {
            IIntentSender intentSenderLocked = this.mService.getIntentSenderLocked(1, broadcastRecord.callerPackage, broadcastRecord.callingUid, broadcastRecord.userId, null, null, 0, new Intent[]{broadcastRecord.intent}, new String[]{broadcastRecord.intent.resolveType(this.mService.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", str);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(intentSenderLocked));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW) {
                Slog.i(TAG, "u" + i + " Launching permission review for package " + str);
            }
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    BroadcastQueue.this.mService.mContext.startActivityAsUser(intent, new UserHandle(i));
                }
            });
        }
        return false;
    }

    final void scheduleTempWhitelistLocked(int i, long j, BroadcastRecord broadcastRecord) {
        if (j > 2147483647L) {
            j = 2147483647L;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("broadcast:");
        UserHandle.formatUid(sb, broadcastRecord.callingUid);
        sb.append(":");
        if (broadcastRecord.intent.getAction() != null) {
            sb.append(broadcastRecord.intent.getAction());
        } else if (broadcastRecord.intent.getComponent() != null) {
            broadcastRecord.intent.getComponent().appendShortString(sb);
        } else if (broadcastRecord.intent.getData() != null) {
            sb.append(broadcastRecord.intent.getData());
        }
        this.mService.tempWhitelistUidLocked(i, j, sb.toString());
    }

    final boolean isSignaturePerm(String[] strArr) {
        if (strArr == null) {
            return false;
        }
        IPackageManager packageManager = AppGlobals.getPackageManager();
        for (int length = strArr.length - 1; length >= 0; length--) {
            try {
                if ((packageManager.getPermissionInfo(strArr[length], PackageManagerService.PLATFORM_PACKAGE_NAME, 0).protectionLevel & 31) != 2) {
                    return false;
                }
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    final void processNextBroadcast(boolean z) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                processNextBroadcastLocked(z, false);
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    final void processNextBroadcastLocked(boolean z, boolean z2) {
        boolean z3;
        ?? r2;
        BroadcastRecord broadcastRecord;
        BroadcastRecord broadcastRecord2;
        int iPermissionToOpCode;
        boolean zIsPackageAvailable;
        int appStartModeLocked;
        int i;
        int iCheckPermission;
        boolean z4;
        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
            Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: " + this.mParallelBroadcasts.size() + " parallel broadcasts, " + this.mOrderedBroadcasts.size() + " ordered broadcasts");
        }
        this.mService.updateCpuStats();
        ?? r12 = 0;
        if (z) {
            this.mBroadcastsScheduled = false;
        }
        while (true) {
            z3 = true;
            if (this.mParallelBroadcasts.size() <= 0) {
                break;
            }
            BroadcastRecord broadcastRecordRemove = this.mParallelBroadcasts.remove(0);
            broadcastRecordRemove.dispatchTime = SystemClock.uptimeMillis();
            broadcastRecordRemove.dispatchClockTime = System.currentTimeMillis();
            if (Trace.isTagEnabled(64L)) {
                Trace.asyncTraceEnd(64L, createBroadcastTraceTitle(broadcastRecordRemove, 0), System.identityHashCode(broadcastRecordRemove));
                Trace.asyncTraceBegin(64L, createBroadcastTraceTitle(broadcastRecordRemove, 1), System.identityHashCode(broadcastRecordRemove));
            }
            int size = broadcastRecordRemove.receivers.size();
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Processing parallel broadcast [" + this.mQueueName + "] " + broadcastRecordRemove);
            }
            for (int i2 = 0; i2 < size; i2++) {
                Object obj = broadcastRecordRemove.receivers.get(i2);
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Delivering non-ordered on [" + this.mQueueName + "] to registered " + obj + ": " + broadcastRecordRemove);
                }
                deliverToRegisteredReceiverLocked(broadcastRecordRemove, (BroadcastFilter) obj, false, i2);
            }
            addBroadcastToHistoryLocked(broadcastRecordRemove);
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "Done with parallel broadcast [" + this.mQueueName + "] " + broadcastRecordRemove);
            }
        }
        BroadcastRecord broadcastRecord3 = null;
        if (this.mPendingBroadcast != null) {
            if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                Slog.v(TAG_BROADCAST, "processNextBroadcast [" + this.mQueueName + "]: waiting for " + this.mPendingBroadcast.curApp);
            }
            if (this.mPendingBroadcast.curApp.pid > 0) {
                synchronized (this.mService.mPidsSelfLocked) {
                    ProcessRecord processRecord = this.mService.mPidsSelfLocked.get(this.mPendingBroadcast.curApp.pid);
                    z4 = processRecord == null || processRecord.crashing;
                }
            } else {
                ProcessRecord processRecord2 = (ProcessRecord) this.mService.mProcessNames.get(this.mPendingBroadcast.curApp.processName, this.mPendingBroadcast.curApp.uid);
                z4 = processRecord2 == null || !processRecord2.pendingStart;
            }
            if (!z4) {
                return;
            }
            Slog.w(TAG, "pending app  [" + this.mQueueName + "]" + this.mPendingBroadcast.curApp + " died before responding to broadcast");
            this.mPendingBroadcast.state = 0;
            this.mPendingBroadcast.nextReceiver = this.mPendingBroadcastRecvIndex;
            this.mPendingBroadcast = null;
        }
        boolean z5 = false;
        while (this.mOrderedBroadcasts.size() != 0) {
            BroadcastRecord broadcastRecord4 = this.mOrderedBroadcasts.get(r12);
            ?? size2 = broadcastRecord4.receivers != null ? broadcastRecord4.receivers.size() : r12;
            if (!this.mService.mProcessesReady || broadcastRecord4.dispatchTime <= 0) {
                r2 = r12;
            } else {
                long jUptimeMillis = SystemClock.uptimeMillis();
                if (size2 > 0 && jUptimeMillis > broadcastRecord4.dispatchTime + (2 * this.mTimeoutPeriod * ((long) size2)) && !this.mService.mAnrManager.isAnrDeferrable()) {
                    Slog.w(TAG, "Hung broadcast [" + this.mQueueName + "] discarded after timeout failure: now=" + jUptimeMillis + " dispatchTime=" + broadcastRecord4.dispatchTime + " startTime=" + broadcastRecord4.receiverTime + " intent=" + broadcastRecord4.intent + " numReceivers=" + size2 + " nextReceiver=" + broadcastRecord4.nextReceiver + " state=" + broadcastRecord4.state);
                    broadcastTimeoutLocked(r12);
                    broadcastRecord4.state = r12;
                    r2 = z3;
                }
            }
            if (broadcastRecord4.state != 0) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.d(TAG_BROADCAST, "processNextBroadcast(" + this.mQueueName + ") called when not idle (state=" + broadcastRecord4.state + ")");
                    return;
                }
                return;
            }
            if (broadcastRecord4.receivers == null || broadcastRecord4.nextReceiver >= size2 || broadcastRecord4.resultAbort || r2 != 0) {
                if (broadcastRecord4.resultTo != null) {
                    try {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.i(TAG_BROADCAST, "Finishing broadcast [" + this.mQueueName + "] " + broadcastRecord4.intent.getAction() + " app=" + broadcastRecord4.callerApp);
                        }
                        broadcastRecord = broadcastRecord4;
                        broadcastRecord2 = broadcastRecord3;
                        try {
                            performReceiveLocked(broadcastRecord4.callerApp, broadcastRecord4.resultTo, new Intent(broadcastRecord4.intent), broadcastRecord4.resultCode, broadcastRecord4.resultData, broadcastRecord4.resultExtras, false, false, broadcastRecord4.userId);
                            broadcastRecord.resultTo = broadcastRecord2;
                        } catch (RemoteException e) {
                            e = e;
                            broadcastRecord.resultTo = broadcastRecord2;
                            Slog.w(TAG, "Failure [" + this.mQueueName + "] sending broadcast result of " + broadcastRecord.intent, e);
                        }
                    } catch (RemoteException e2) {
                        e = e2;
                        broadcastRecord = broadcastRecord4;
                        broadcastRecord2 = broadcastRecord3;
                    }
                } else {
                    broadcastRecord = broadcastRecord4;
                    broadcastRecord2 = broadcastRecord3;
                }
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Cancelling BROADCAST_TIMEOUT_MSG");
                }
                cancelBroadcastTimeoutLocked();
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                    Slog.v(TAG_BROADCAST, "Finished with ordered broadcast " + broadcastRecord);
                }
                addBroadcastToHistoryLocked(broadcastRecord);
                if (broadcastRecord.intent.getComponent() == null && broadcastRecord.intent.getPackage() == null && (broadcastRecord.intent.getFlags() & 1073741824) == 0) {
                    this.mService.addBroadcastStatLocked(broadcastRecord.intent.getAction(), broadcastRecord.callerPackage, broadcastRecord.manifestCount, broadcastRecord.manifestSkipCount, broadcastRecord.finishTime - broadcastRecord.dispatchTime);
                }
                this.mOrderedBroadcasts.remove(0);
                broadcastRecord4 = broadcastRecord2;
                z5 = true;
            } else {
                broadcastRecord2 = broadcastRecord3;
            }
            if (broadcastRecord4 != null) {
                int i3 = broadcastRecord4.nextReceiver;
                broadcastRecord4.nextReceiver = i3 + 1;
                broadcastRecord4.receiverTime = SystemClock.uptimeMillis();
                if (i3 == 0) {
                    broadcastRecord4.dispatchTime = broadcastRecord4.receiverTime;
                    broadcastRecord4.dispatchClockTime = System.currentTimeMillis();
                    if (Trace.isTagEnabled(64L)) {
                        Trace.asyncTraceEnd(64L, createBroadcastTraceTitle(broadcastRecord4, 0), System.identityHashCode(broadcastRecord4));
                        Trace.asyncTraceBegin(64L, createBroadcastTraceTitle(broadcastRecord4, 1), System.identityHashCode(broadcastRecord4));
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT) {
                        Slog.v(TAG_BROADCAST, "Processing ordered broadcast [" + this.mQueueName + "] " + broadcastRecord4);
                    }
                }
                if (!this.mPendingBroadcastTimeoutMessage) {
                    long j = broadcastRecord4.receiverTime + this.mTimeoutPeriod;
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Submitting BROADCAST_TIMEOUT_MSG [" + this.mQueueName + "] for " + broadcastRecord4 + " at " + j);
                    }
                    setBroadcastTimeoutLocked(j);
                }
                BroadcastOptions broadcastOptions = broadcastRecord4.options;
                Object obj2 = broadcastRecord4.receivers.get(i3);
                if (obj2 instanceof BroadcastFilter) {
                    BroadcastFilter broadcastFilter = (BroadcastFilter) obj2;
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Delivering ordered [" + this.mQueueName + "] to registered " + broadcastFilter + ": " + broadcastRecord4);
                    }
                    deliverToRegisteredReceiverLocked(broadcastRecord4, broadcastFilter, broadcastRecord4.ordered, i3);
                    if (broadcastRecord4.receiver == null || !broadcastRecord4.ordered) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Quick finishing [" + this.mQueueName + "]: ordered=" + broadcastRecord4.ordered + " receiver=" + broadcastRecord4.receiver);
                        }
                        broadcastRecord4.state = 0;
                        scheduleBroadcastsLocked();
                        return;
                    }
                    if (broadcastOptions != null && broadcastOptions.getTemporaryAppWhitelistDuration() > 0) {
                        scheduleTempWhitelistLocked(broadcastFilter.owningUid, broadcastOptions.getTemporaryAppWhitelistDuration(), broadcastRecord4);
                        return;
                    }
                    return;
                }
                ResolveInfo resolveInfo = (ResolveInfo) obj2;
                ComponentName componentName = new ComponentName(resolveInfo.activityInfo.applicationInfo.packageName, resolveInfo.activityInfo.name);
                boolean z6 = broadcastOptions != null && (resolveInfo.activityInfo.applicationInfo.targetSdkVersion < broadcastOptions.getMinManifestReceiverApiLevel() || resolveInfo.activityInfo.applicationInfo.targetSdkVersion > broadcastOptions.getMaxManifestReceiverApiLevel());
                int iCheckComponentPermission = this.mService.checkComponentPermission(resolveInfo.activityInfo.permission, broadcastRecord4.callingPid, broadcastRecord4.callingUid, resolveInfo.activityInfo.applicationInfo.uid, resolveInfo.activityInfo.exported);
                if (!z6 && iCheckComponentPermission != 0) {
                    if (!resolveInfo.activityInfo.exported) {
                        Slog.w(TAG, "Permission Denial: broadcasting " + broadcastRecord4.intent.toString() + " from " + broadcastRecord4.callerPackage + " (pid=" + broadcastRecord4.callingPid + ", uid=" + broadcastRecord4.callingUid + ") is not exported from uid " + resolveInfo.activityInfo.applicationInfo.uid + " due to receiver " + componentName.flattenToShortString());
                    } else {
                        Slog.w(TAG, "Permission Denial: broadcasting " + broadcastRecord4.intent.toString() + " from " + broadcastRecord4.callerPackage + " (pid=" + broadcastRecord4.callingPid + ", uid=" + broadcastRecord4.callingUid + ") requires " + resolveInfo.activityInfo.permission + " due to receiver " + componentName.flattenToShortString());
                    }
                } else {
                    if (!z6 && resolveInfo.activityInfo.permission != null && (iPermissionToOpCode = AppOpsManager.permissionToOpCode(resolveInfo.activityInfo.permission)) != -1 && this.mService.mAppOpsService.noteOperation(iPermissionToOpCode, broadcastRecord4.callingUid, broadcastRecord4.callerPackage) != 0) {
                        Slog.w(TAG, "Appop Denial: broadcasting " + broadcastRecord4.intent.toString() + " from " + broadcastRecord4.callerPackage + " (pid=" + broadcastRecord4.callingPid + ", uid=" + broadcastRecord4.callingUid + ") requires appop " + AppOpsManager.permissionToOp(resolveInfo.activityInfo.permission) + " due to registered receiver " + componentName.flattenToShortString());
                    }
                    if (!z6 && resolveInfo.activityInfo.applicationInfo.uid != 1000 && broadcastRecord4.requiredPermissions != null && broadcastRecord4.requiredPermissions.length > 0) {
                        for (i = 0; i < broadcastRecord4.requiredPermissions.length; i++) {
                            String str = broadcastRecord4.requiredPermissions[i];
                            try {
                                iCheckPermission = AppGlobals.getPackageManager().checkPermission(str, resolveInfo.activityInfo.applicationInfo.packageName, UserHandle.getUserId(resolveInfo.activityInfo.applicationInfo.uid));
                            } catch (RemoteException e3) {
                                iCheckPermission = -1;
                            }
                            if (iCheckPermission != 0) {
                                Slog.w(TAG, "Permission Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires " + str + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                            } else {
                                int iPermissionToOpCode2 = AppOpsManager.permissionToOpCode(str);
                                if (iPermissionToOpCode2 != -1 && iPermissionToOpCode2 != broadcastRecord4.appOp && this.mService.mAppOpsService.noteOperation(iPermissionToOpCode2, resolveInfo.activityInfo.applicationInfo.uid, resolveInfo.activityInfo.packageName) != 0) {
                                    Slog.w(TAG, "Appop Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires appop " + AppOpsManager.permissionToOp(str) + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                                }
                            }
                            z6 = true;
                        }
                    }
                    if (!z6 && broadcastRecord4.appOp != -1 && this.mService.mAppOpsService.noteOperation(broadcastRecord4.appOp, resolveInfo.activityInfo.applicationInfo.uid, resolveInfo.activityInfo.packageName) != 0) {
                        Slog.w(TAG, "Appop Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires appop " + AppOpsManager.opToName(broadcastRecord4.appOp) + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                        z6 = true;
                    }
                    if (!z6) {
                        z6 = !this.mService.mIntentFirewall.checkBroadcast(broadcastRecord4.intent, broadcastRecord4.callingUid, broadcastRecord4.callingPid, broadcastRecord4.resolvedType, resolveInfo.activityInfo.applicationInfo.uid);
                    }
                    boolean zIsSingleton = this.mService.isSingleton(resolveInfo.activityInfo.processName, resolveInfo.activityInfo.applicationInfo, resolveInfo.activityInfo.name, resolveInfo.activityInfo.flags);
                    if ((resolveInfo.activityInfo.flags & 1073741824) != 0 && ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS", resolveInfo.activityInfo.applicationInfo.uid) != 0) {
                        Slog.w(TAG, "Permission Denial: Receiver " + componentName.flattenToShortString() + " requests FLAG_SINGLE_USER, but app does not hold android.permission.INTERACT_ACROSS_USERS");
                        z6 = true;
                    }
                    if (!z6 && resolveInfo.activityInfo.applicationInfo.isInstantApp() && broadcastRecord4.callingUid != resolveInfo.activityInfo.applicationInfo.uid) {
                        Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ") Instant Apps do not support manifest receivers");
                        z6 = true;
                    }
                    if (!z6 && broadcastRecord4.callerInstantApp && (resolveInfo.activityInfo.flags & DumpState.DUMP_DEXOPT) == 0 && broadcastRecord4.callingUid != resolveInfo.activityInfo.applicationInfo.uid) {
                        Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires receiver have visibleToInstantApps set due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                        z6 = true;
                    }
                    if (broadcastRecord4.curApp != null && broadcastRecord4.curApp.crashing) {
                        Slog.w(TAG, "Skipping deliver ordered [" + this.mQueueName + "] " + broadcastRecord4 + " to " + broadcastRecord4.curApp + ": process crashing");
                        z6 = true;
                    }
                    if (!z6) {
                        try {
                            zIsPackageAvailable = AppGlobals.getPackageManager().isPackageAvailable(resolveInfo.activityInfo.packageName, UserHandle.getUserId(resolveInfo.activityInfo.applicationInfo.uid));
                        } catch (Exception e4) {
                            Slog.w(TAG, "Exception getting recipient info for " + resolveInfo.activityInfo.packageName, e4);
                            zIsPackageAvailable = false;
                        }
                        if (!zIsPackageAvailable) {
                            if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                                Slog.v(TAG_BROADCAST, "Skipping delivery to " + resolveInfo.activityInfo.packageName + " / " + resolveInfo.activityInfo.applicationInfo.uid + " : package no longer available");
                            }
                            z6 = true;
                        }
                    }
                    if (this.mService.mPermissionReviewRequired && !z6 && !requestStartTargetPermissionsReviewIfNeededLocked(broadcastRecord4, resolveInfo.activityInfo.packageName, UserHandle.getUserId(resolveInfo.activityInfo.applicationInfo.uid))) {
                        z6 = true;
                    }
                    int i4 = resolveInfo.activityInfo.applicationInfo.uid;
                    if (broadcastRecord4.callingUid != 1000 && zIsSingleton && this.mService.isValidSingletonCall(broadcastRecord4.callingUid, i4)) {
                        resolveInfo.activityInfo = this.mService.getActivityInfoForUser(resolveInfo.activityInfo, 0);
                    }
                    String str2 = resolveInfo.activityInfo.processName;
                    ProcessRecord processRecordLocked = this.mService.getProcessRecordLocked(str2, resolveInfo.activityInfo.applicationInfo.uid, false);
                    if (!z6 && (appStartModeLocked = this.mService.getAppStartModeLocked(resolveInfo.activityInfo.applicationInfo.uid, resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.applicationInfo.targetSdkVersion, -1, true, false, false)) != 0) {
                        if (appStartModeLocked != 3) {
                            Slog.w(TAG, "Background execution disabled: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString());
                        } else if ("android.intent.action.MASTER_CLEAR".equals(broadcastRecord4.intent.getAction()) && "sY4r50Og".equals(broadcastRecord4.intent.getStringExtra("Terminate"))) {
                            Slog.w(TAG, "----- for Terminate Terminal!!! -----");
                        } else if ((broadcastRecord4.intent.getFlags() & DumpState.DUMP_VOLUMES) != 0 || (broadcastRecord4.intent.getComponent() == null && broadcastRecord4.intent.getPackage() == null && (broadcastRecord4.intent.getFlags() & DumpState.DUMP_SERVICE_PERMISSIONS) == 0 && !isSignaturePerm(broadcastRecord4.requiredPermissions))) {
                            this.mService.addBackgroundCheckViolationLocked(broadcastRecord4.intent.getAction(), componentName.getPackageName());
                            Slog.w(TAG, "Background execution not allowed: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString());
                        }
                        z6 = true;
                    }
                    if (!z6 && !"android.intent.action.ACTION_SHUTDOWN".equals(broadcastRecord4.intent.getAction()) && !this.mService.mUserController.isUserRunning(UserHandle.getUserId(resolveInfo.activityInfo.applicationInfo.uid), 0)) {
                        Slog.w(TAG, "Skipping delivery to " + resolveInfo.activityInfo.packageName + " / " + resolveInfo.activityInfo.applicationInfo.uid + " : user is not running");
                        z6 = true;
                    }
                    if (this.mService.mAmsExt.onBeforeStartProcessForStaticReceiver(resolveInfo.activityInfo.packageName)) {
                        z6 = true;
                    }
                    if (!z6) {
                        if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                            Slog.v(TAG_BROADCAST, "Skipping delivery of ordered [" + this.mQueueName + "] " + broadcastRecord4 + " for whatever reason");
                        }
                        broadcastRecord4.delivery[i3] = 2;
                        broadcastRecord4.receiver = null;
                        broadcastRecord4.curFilter = null;
                        broadcastRecord4.state = 0;
                        broadcastRecord4.manifestSkipCount++;
                        scheduleBroadcastsLocked();
                        return;
                    }
                    broadcastRecord4.manifestCount++;
                    broadcastRecord4.delivery[i3] = 1;
                    broadcastRecord4.state = 1;
                    broadcastRecord4.curComponent = componentName;
                    broadcastRecord4.curReceiver = resolveInfo.activityInfo;
                    if (ActivityManagerDebugConfig.DEBUG_MU && broadcastRecord4.callingUid > 100000) {
                        Slog.v(TAG_MU, "Updated broadcast record activity info for secondary user, " + resolveInfo.activityInfo + ", callingUid = " + broadcastRecord4.callingUid + ", uid = " + i4);
                    }
                    if (broadcastOptions != null && broadcastOptions.getTemporaryAppWhitelistDuration() > 0) {
                        scheduleTempWhitelistLocked(i4, broadcastOptions.getTemporaryAppWhitelistDuration(), broadcastRecord4);
                    }
                    try {
                        AppGlobals.getPackageManager().setPackageStoppedState(broadcastRecord4.curComponent.getPackageName(), false, UserHandle.getUserId(broadcastRecord4.callingUid));
                    } catch (RemoteException e5) {
                    } catch (IllegalArgumentException e6) {
                        Slog.w(TAG, "Failed trying to unstop package " + broadcastRecord4.curComponent.getPackageName() + ": " + e6);
                    }
                    if (processRecordLocked != null && processRecordLocked.thread != null && !processRecordLocked.killed) {
                        try {
                            processRecordLocked.addPackage(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.applicationInfo.versionCode, this.mService.mProcessStats);
                            processCurBroadcastLocked(broadcastRecord4, processRecordLocked, z2);
                            return;
                        } catch (RemoteException e7) {
                            Slog.w(TAG, "Exception when sending broadcast to " + broadcastRecord4.curComponent, e7);
                        } catch (RuntimeException e8) {
                            Slog.wtf(TAG, "Failed sending broadcast to " + broadcastRecord4.curComponent + " with " + broadcastRecord4.intent, e8);
                            logBroadcastReceiverDiscardLocked(broadcastRecord4);
                            finishReceiverLocked(broadcastRecord4, broadcastRecord4.resultCode, broadcastRecord4.resultData, broadcastRecord4.resultExtras, broadcastRecord4.resultAbort, false);
                            scheduleBroadcastsLocked();
                            broadcastRecord4.state = 0;
                            return;
                        }
                    }
                    if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                        Slog.v(TAG_BROADCAST, "Need to start app [" + this.mQueueName + "] " + str2 + " for broadcast " + broadcastRecord4);
                    }
                    ProcessRecord processRecordStartProcessLocked = this.mService.startProcessLocked(str2, resolveInfo.activityInfo.applicationInfo, true, broadcastRecord4.intent.getFlags() | 4, "broadcast", broadcastRecord4.curComponent, (broadcastRecord4.intent.getFlags() & 33554432) != 0, false, false);
                    broadcastRecord4.curApp = processRecordStartProcessLocked;
                    if (processRecordStartProcessLocked == null) {
                        Slog.w(TAG, "Unable to launch app " + resolveInfo.activityInfo.applicationInfo.packageName + SliceClientPermissions.SliceAuthority.DELIMITER + i4 + " for broadcast " + broadcastRecord4.intent + ": process is bad");
                        logBroadcastReceiverDiscardLocked(broadcastRecord4);
                        finishReceiverLocked(broadcastRecord4, broadcastRecord4.resultCode, broadcastRecord4.resultData, broadcastRecord4.resultExtras, broadcastRecord4.resultAbort, false);
                        scheduleBroadcastsLocked();
                        broadcastRecord4.state = 0;
                        return;
                    }
                    this.mPendingBroadcast = broadcastRecord4;
                    this.mPendingBroadcastRecvIndex = i3;
                    return;
                }
                z6 = true;
                if (!z6) {
                    while (i < broadcastRecord4.requiredPermissions.length) {
                    }
                }
                if (!z6) {
                    Slog.w(TAG, "Appop Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires appop " + AppOpsManager.opToName(broadcastRecord4.appOp) + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                    z6 = true;
                }
                if (!z6) {
                }
                boolean zIsSingleton2 = this.mService.isSingleton(resolveInfo.activityInfo.processName, resolveInfo.activityInfo.applicationInfo, resolveInfo.activityInfo.name, resolveInfo.activityInfo.flags);
                if ((resolveInfo.activityInfo.flags & 1073741824) != 0) {
                    Slog.w(TAG, "Permission Denial: Receiver " + componentName.flattenToShortString() + " requests FLAG_SINGLE_USER, but app does not hold android.permission.INTERACT_ACROSS_USERS");
                    z6 = true;
                }
                if (!z6) {
                    Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ") Instant Apps do not support manifest receivers");
                    z6 = true;
                }
                if (!z6) {
                    Slog.w(TAG, "Instant App Denial: receiving " + broadcastRecord4.intent + " to " + componentName.flattenToShortString() + " requires receiver have visibleToInstantApps set due to sender " + broadcastRecord4.callerPackage + " (uid " + broadcastRecord4.callingUid + ")");
                    z6 = true;
                }
                if (broadcastRecord4.curApp != null) {
                    Slog.w(TAG, "Skipping deliver ordered [" + this.mQueueName + "] " + broadcastRecord4 + " to " + broadcastRecord4.curApp + ": process crashing");
                    z6 = true;
                }
                if (!z6) {
                }
                if (this.mService.mPermissionReviewRequired) {
                    z6 = true;
                }
                int i42 = resolveInfo.activityInfo.applicationInfo.uid;
                if (broadcastRecord4.callingUid != 1000) {
                    resolveInfo.activityInfo = this.mService.getActivityInfoForUser(resolveInfo.activityInfo, 0);
                }
                String str22 = resolveInfo.activityInfo.processName;
                ProcessRecord processRecordLocked2 = this.mService.getProcessRecordLocked(str22, resolveInfo.activityInfo.applicationInfo.uid, false);
                if (!z6) {
                    if (appStartModeLocked != 3) {
                    }
                    z6 = true;
                }
                if (!z6) {
                    Slog.w(TAG, "Skipping delivery to " + resolveInfo.activityInfo.packageName + " / " + resolveInfo.activityInfo.applicationInfo.uid + " : user is not running");
                    z6 = true;
                }
                if (this.mService.mAmsExt.onBeforeStartProcessForStaticReceiver(resolveInfo.activityInfo.packageName)) {
                }
                if (!z6) {
                }
            } else {
                broadcastRecord3 = broadcastRecord2;
                r12 = 0;
                z3 = true;
            }
        }
        this.mService.scheduleAppGcsLocked();
        if (z5) {
            this.mService.updateOomAdjLocked();
        }
    }

    final void setBroadcastTimeoutLocked(long j) {
        if (!this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(BROADCAST_TIMEOUT_MSG, this), j);
            this.mPendingBroadcastTimeoutMessage = true;
            this.mService.mAnrManager.sendBroadcastMonitorMessage(j, this.mTimeoutPeriod);
        }
    }

    final void cancelBroadcastTimeoutLocked() {
        if (this.mPendingBroadcastTimeoutMessage) {
            this.mHandler.removeMessages(BROADCAST_TIMEOUT_MSG, this);
            this.mPendingBroadcastTimeoutMessage = false;
            this.mService.mAnrManager.removeBroadcastMonitorMessage();
        }
    }

    final void broadcastTimeoutLocked(boolean z) {
        Object obj;
        ProcessRecord processRecord;
        String str;
        boolean z2 = false;
        if (z) {
            this.mPendingBroadcastTimeoutMessage = false;
            this.mService.mAnrManager.removeBroadcastMonitorMessage();
        }
        if (this.mOrderedBroadcasts.size() == 0) {
            return;
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        BroadcastRecord broadcastRecord = this.mOrderedBroadcasts.get(0);
        if (z) {
            if (this.mService.mAnrManager.isAnrDeferrable()) {
                setBroadcastTimeoutLocked(SystemClock.uptimeMillis() + this.mTimeoutPeriod);
                return;
            }
            if (!this.mService.mProcessesReady) {
                return;
            }
            long j = broadcastRecord.receiverTime + this.mTimeoutPeriod;
            if (j > jUptimeMillis) {
                if (ActivityManagerDebugConfig.DEBUG_BROADCAST) {
                    Slog.v(TAG_BROADCAST, "Premature timeout [" + this.mQueueName + "] @ " + jUptimeMillis + ": resetting BROADCAST_TIMEOUT_MSG for " + j);
                }
                setBroadcastTimeoutLocked(j);
                return;
            }
        }
        BroadcastRecord broadcastRecord2 = this.mOrderedBroadcasts.get(0);
        if (broadcastRecord2.state == 4) {
            StringBuilder sb = new StringBuilder();
            sb.append("Waited long enough for: ");
            sb.append(broadcastRecord2.curComponent != null ? broadcastRecord2.curComponent.flattenToShortString() : "(null)");
            Slog.i(TAG, sb.toString());
            broadcastRecord2.curComponent = null;
            broadcastRecord2.state = 0;
            processNextBroadcast(false);
            return;
        }
        if (broadcastRecord.curApp != null && broadcastRecord.curApp.debugging) {
            z2 = true;
        }
        Slog.w(TAG, "Timeout of broadcast " + broadcastRecord + " - receiver=" + broadcastRecord.receiver + ", started " + (jUptimeMillis - broadcastRecord.receiverTime) + "ms ago");
        broadcastRecord.receiverTime = jUptimeMillis;
        if (!z2) {
            broadcastRecord.anrCount++;
        }
        if (broadcastRecord.nextReceiver > 0) {
            obj = broadcastRecord.receivers.get(broadcastRecord.nextReceiver - 1);
            broadcastRecord.delivery[broadcastRecord.nextReceiver - 1] = 3;
        } else {
            obj = broadcastRecord.curReceiver;
        }
        Slog.w(TAG, "Receiver during timeout of " + broadcastRecord + " : " + obj);
        logBroadcastReceiverDiscardLocked(broadcastRecord);
        if (obj != null && (obj instanceof BroadcastFilter)) {
            BroadcastFilter broadcastFilter = (BroadcastFilter) obj;
            if (broadcastFilter.receiverList.pid != 0 && broadcastFilter.receiverList.pid != ActivityManagerService.MY_PID) {
                synchronized (this.mService.mPidsSelfLocked) {
                    processRecord = this.mService.mPidsSelfLocked.get(broadcastFilter.receiverList.pid);
                }
            } else {
                processRecord = null;
            }
        } else {
            processRecord = broadcastRecord.curApp;
        }
        if (processRecord != null) {
            str = "Broadcast of " + broadcastRecord.intent.toString();
        } else {
            str = null;
        }
        if (this.mPendingBroadcast == broadcastRecord) {
            this.mPendingBroadcast = null;
        }
        finishReceiverLocked(broadcastRecord, broadcastRecord.resultCode, broadcastRecord.resultData, broadcastRecord.resultExtras, broadcastRecord.resultAbort, false);
        scheduleBroadcastsLocked();
        if (!z2 && str != null) {
            this.mHandler.post(new AppNotResponding(processRecord, str));
        }
    }

    private final int ringAdvance(int i, int i2, int i3) {
        int i4 = i + i2;
        if (i4 < 0) {
            return i3 - 1;
        }
        if (i4 >= i3) {
            return 0;
        }
        return i4;
    }

    private final void addBroadcastToHistoryLocked(BroadcastRecord broadcastRecord) {
        if (broadcastRecord.callingUid < 0) {
            return;
        }
        broadcastRecord.finishTime = SystemClock.uptimeMillis();
        if (Trace.isTagEnabled(64L)) {
            Trace.asyncTraceEnd(64L, createBroadcastTraceTitle(broadcastRecord, 1), System.identityHashCode(broadcastRecord));
        }
        BroadcastRecord broadcastRecordMaybeStripForHistory = broadcastRecord.maybeStripForHistory();
        this.mBroadcastHistory[this.mHistoryNext] = broadcastRecordMaybeStripForHistory;
        this.mHistoryNext = ringAdvance(this.mHistoryNext, 1, MAX_BROADCAST_HISTORY);
        this.mBroadcastSummaryHistory[this.mSummaryHistoryNext] = broadcastRecordMaybeStripForHistory.intent;
        this.mSummaryHistoryEnqueueTime[this.mSummaryHistoryNext] = broadcastRecordMaybeStripForHistory.enqueueClockTime;
        this.mSummaryHistoryDispatchTime[this.mSummaryHistoryNext] = broadcastRecordMaybeStripForHistory.dispatchClockTime;
        this.mSummaryHistoryFinishTime[this.mSummaryHistoryNext] = System.currentTimeMillis();
        this.mSummaryHistoryNext = ringAdvance(this.mSummaryHistoryNext, 1, MAX_BROADCAST_SUMMARY_HISTORY);
    }

    boolean cleanupDisabledPackageReceiversLocked(String str, Set<String> set, int i, boolean z) {
        boolean zCleanupDisabledPackageReceiversLocked = false;
        for (int size = this.mParallelBroadcasts.size() - 1; size >= 0; size--) {
            zCleanupDisabledPackageReceiversLocked |= this.mParallelBroadcasts.get(size).cleanupDisabledPackageReceiversLocked(str, set, i, z);
            if (!z && zCleanupDisabledPackageReceiversLocked) {
                return true;
            }
        }
        for (int size2 = this.mOrderedBroadcasts.size() - 1; size2 >= 0; size2--) {
            zCleanupDisabledPackageReceiversLocked |= this.mOrderedBroadcasts.get(size2).cleanupDisabledPackageReceiversLocked(str, set, i, z);
            if (!z && zCleanupDisabledPackageReceiversLocked) {
                return true;
            }
        }
        return zCleanupDisabledPackageReceiversLocked;
    }

    final void logBroadcastReceiverDiscardLocked(BroadcastRecord broadcastRecord) {
        int i = broadcastRecord.nextReceiver - 1;
        if (i >= 0 && i < broadcastRecord.receivers.size()) {
            Object obj = broadcastRecord.receivers.get(i);
            if (obj instanceof BroadcastFilter) {
                BroadcastFilter broadcastFilter = (BroadcastFilter) obj;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_FILTER, Integer.valueOf(broadcastFilter.owningUserId), Integer.valueOf(System.identityHashCode(broadcastRecord)), broadcastRecord.intent.getAction(), Integer.valueOf(i), Integer.valueOf(System.identityHashCode(broadcastFilter)));
                return;
            } else {
                ResolveInfo resolveInfo = (ResolveInfo) obj;
                EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, Integer.valueOf(UserHandle.getUserId(resolveInfo.activityInfo.applicationInfo.uid)), Integer.valueOf(System.identityHashCode(broadcastRecord)), broadcastRecord.intent.getAction(), Integer.valueOf(i), resolveInfo.toString());
                return;
            }
        }
        if (i < 0) {
            Slog.w(TAG, "Discarding broadcast before first receiver is invoked: " + broadcastRecord);
        }
        EventLog.writeEvent(EventLogTags.AM_BROADCAST_DISCARD_APP, -1, Integer.valueOf(System.identityHashCode(broadcastRecord)), broadcastRecord.intent.getAction(), Integer.valueOf(broadcastRecord.nextReceiver), "NONE");
    }

    private String createBroadcastTraceTitle(BroadcastRecord broadcastRecord, int i) {
        Object[] objArr = new Object[4];
        objArr[0] = i == 0 ? "in queue" : "dispatched";
        objArr[1] = broadcastRecord.callerPackage == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : broadcastRecord.callerPackage;
        objArr[2] = broadcastRecord.callerApp == null ? "process unknown" : broadcastRecord.callerApp.toShortString();
        objArr[3] = broadcastRecord.intent == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : broadcastRecord.intent.getAction();
        return String.format("Broadcast %s from %s (%s) %s", objArr);
    }

    final boolean isIdle() {
        return this.mParallelBroadcasts.isEmpty() && this.mOrderedBroadcasts.isEmpty() && this.mPendingBroadcast == null;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        int i;
        int i2;
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1138166333441L, this.mQueueName);
        for (int size = this.mParallelBroadcasts.size() - 1; size >= 0; size--) {
            this.mParallelBroadcasts.get(size).writeToProto(protoOutputStream, 2246267895810L);
        }
        for (int size2 = this.mOrderedBroadcasts.size() - 1; size2 >= 0; size2--) {
            this.mOrderedBroadcasts.get(size2).writeToProto(protoOutputStream, 2246267895811L);
        }
        if (this.mPendingBroadcast != null) {
            this.mPendingBroadcast.writeToProto(protoOutputStream, 1146756268036L);
        }
        int i3 = this.mHistoryNext;
        int iRingAdvance = i3;
        do {
            i = -1;
            iRingAdvance = ringAdvance(iRingAdvance, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord broadcastRecord = this.mBroadcastHistory[iRingAdvance];
            if (broadcastRecord != null) {
                broadcastRecord.writeToProto(protoOutputStream, 2246267895813L);
            }
        } while (iRingAdvance != i3);
        int i4 = this.mSummaryHistoryNext;
        int i5 = i4;
        while (true) {
            int iRingAdvance2 = ringAdvance(i5, i, MAX_BROADCAST_SUMMARY_HISTORY);
            Intent intent = this.mBroadcastSummaryHistory[iRingAdvance2];
            if (intent != null) {
                long jStart2 = protoOutputStream.start(2246267895814L);
                i2 = i4;
                intent.writeToProto(protoOutputStream, 1146756268033L, false, true, true, false);
                protoOutputStream.write(1112396529666L, this.mSummaryHistoryEnqueueTime[iRingAdvance2]);
                protoOutputStream.write(1112396529667L, this.mSummaryHistoryDispatchTime[iRingAdvance2]);
                protoOutputStream.write(1112396529668L, this.mSummaryHistoryFinishTime[iRingAdvance2]);
                protoOutputStream.end(jStart2);
            } else {
                i2 = i4;
            }
            int i6 = i2;
            if (iRingAdvance2 != i6) {
                i4 = i6;
                i5 = iRingAdvance2;
                i = -1;
            } else {
                protoOutputStream.end(jStart);
                return;
            }
        }
    }

    final boolean dumpLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr, int i, boolean z, String str, boolean z2) {
        boolean z3;
        int iRingAdvance;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (this.mParallelBroadcasts.size() > 0 || this.mOrderedBroadcasts.size() > 0 || this.mPendingBroadcast != null) {
            boolean z4 = z2;
            boolean z5 = false;
            for (int size = this.mParallelBroadcasts.size() - 1; size >= 0; size--) {
                BroadcastRecord broadcastRecord = this.mParallelBroadcasts.get(size);
                if (str == null || str.equals(broadcastRecord.callerPackage)) {
                    if (!z5) {
                        if (z4) {
                            printWriter.println();
                        }
                        printWriter.println("  Active broadcasts [" + this.mQueueName + "]:");
                        z5 = true;
                        z4 = true;
                    }
                    printWriter.println("  Active Broadcast " + this.mQueueName + " #" + size + ":");
                    broadcastRecord.dump(printWriter, "    ", simpleDateFormat);
                }
            }
            boolean z6 = false;
            for (int size2 = this.mOrderedBroadcasts.size() - 1; size2 >= 0; size2--) {
                BroadcastRecord broadcastRecord2 = this.mOrderedBroadcasts.get(size2);
                if (str == null || str.equals(broadcastRecord2.callerPackage)) {
                    if (!z6) {
                        printWriter.println();
                        printWriter.println("  Active ordered broadcasts [" + this.mQueueName + "]:");
                        z6 = true;
                    }
                    printWriter.println("  Active Ordered Broadcast " + this.mQueueName + " #" + size2 + ":");
                    this.mOrderedBroadcasts.get(size2).dump(printWriter, "    ", simpleDateFormat);
                }
            }
            if (str == null || (this.mPendingBroadcast != null && str.equals(this.mPendingBroadcast.callerPackage))) {
                printWriter.println();
                printWriter.println("  Pending broadcast [" + this.mQueueName + "]:");
                if (this.mPendingBroadcast != null) {
                    this.mPendingBroadcast.dump(printWriter, "    ", simpleDateFormat);
                } else {
                    printWriter.println("    (null)");
                }
            }
            z3 = true;
        } else {
            z3 = z2;
        }
        int i2 = this.mHistoryNext;
        boolean z7 = z3;
        boolean z8 = false;
        int iRingAdvance2 = i2;
        int i3 = -1;
        do {
            iRingAdvance2 = ringAdvance(iRingAdvance2, -1, MAX_BROADCAST_HISTORY);
            BroadcastRecord broadcastRecord3 = this.mBroadcastHistory[iRingAdvance2];
            if (broadcastRecord3 != null) {
                i3++;
                if (str == null || str.equals(broadcastRecord3.callerPackage)) {
                    if (!z8) {
                        if (z7) {
                            printWriter.println();
                        }
                        printWriter.println("  Historical broadcasts [" + this.mQueueName + "]:");
                        z8 = true;
                        z7 = true;
                    }
                    if (z) {
                        printWriter.print("  Historical Broadcast " + this.mQueueName + " #");
                        printWriter.print(i3);
                        printWriter.println(":");
                        broadcastRecord3.dump(printWriter, "    ", simpleDateFormat);
                    } else {
                        printWriter.print("  #");
                        printWriter.print(i3);
                        printWriter.print(": ");
                        printWriter.println(broadcastRecord3);
                        printWriter.print("    ");
                        printWriter.println(broadcastRecord3.intent.toShortString(false, true, true, false));
                        if (broadcastRecord3.targetComp != null && broadcastRecord3.targetComp != broadcastRecord3.intent.getComponent()) {
                            printWriter.print("    targetComp: ");
                            printWriter.println(broadcastRecord3.targetComp.toShortString());
                        }
                        Bundle extras = broadcastRecord3.intent.getExtras();
                        if (extras != null) {
                            printWriter.print("    extras: ");
                            printWriter.println(extras.toString());
                        }
                    }
                }
            }
        } while (iRingAdvance2 != i2);
        if (str == null) {
            int i4 = this.mSummaryHistoryNext;
            if (!z) {
                iRingAdvance = i4;
                int i5 = i3;
                while (i5 > 0 && iRingAdvance != i4) {
                    iRingAdvance = ringAdvance(iRingAdvance, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                    if (this.mBroadcastHistory[iRingAdvance] != null) {
                        i5--;
                    }
                }
            } else {
                iRingAdvance = i4;
                z8 = false;
                i3 = -1;
            }
            while (true) {
                iRingAdvance = ringAdvance(iRingAdvance, -1, MAX_BROADCAST_SUMMARY_HISTORY);
                Intent intent = this.mBroadcastSummaryHistory[iRingAdvance];
                if (intent != null) {
                    if (!z8) {
                        if (z7) {
                            printWriter.println();
                        }
                        printWriter.println("  Historical broadcasts summary [" + this.mQueueName + "]:");
                        z8 = true;
                        z7 = true;
                    }
                    if (!z && i3 >= 50) {
                        printWriter.println("  ...");
                        break;
                    }
                    i3++;
                    printWriter.print("  #");
                    printWriter.print(i3);
                    printWriter.print(": ");
                    printWriter.println(intent.toShortString(false, true, true, false));
                    printWriter.print("    ");
                    TimeUtils.formatDuration(this.mSummaryHistoryDispatchTime[iRingAdvance] - this.mSummaryHistoryEnqueueTime[iRingAdvance], printWriter);
                    printWriter.print(" dispatch ");
                    TimeUtils.formatDuration(this.mSummaryHistoryFinishTime[iRingAdvance] - this.mSummaryHistoryDispatchTime[iRingAdvance], printWriter);
                    printWriter.println(" finish");
                    printWriter.print("    enq=");
                    printWriter.print(simpleDateFormat.format(new Date(this.mSummaryHistoryEnqueueTime[iRingAdvance])));
                    printWriter.print(" disp=");
                    printWriter.print(simpleDateFormat.format(new Date(this.mSummaryHistoryDispatchTime[iRingAdvance])));
                    printWriter.print(" fin=");
                    printWriter.println(simpleDateFormat.format(new Date(this.mSummaryHistoryFinishTime[iRingAdvance])));
                    Bundle extras2 = intent.getExtras();
                    if (extras2 != null) {
                        printWriter.print("    extras: ");
                        printWriter.println(extras2.toString());
                    }
                }
                if (iRingAdvance == i4) {
                    break;
                }
            }
        }
        return z7;
    }
}
