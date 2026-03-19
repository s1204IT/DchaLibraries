package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.TransactionTooLargeException;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.app.procstats.ServiceState;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.FastPrintWriter;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ServiceRecord;
import com.android.server.job.controllers.JobStatus;
import com.mediatek.am.AMEventHookData;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.server.am.AMEventHook;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ActiveServices {
    static final int BG_START_TIMEOUT = 15000;
    static final String BRING_UP_BIND_SERVICE = "bind service";
    static final String BRING_UP_DELAYED_SERVICE = "delayed service";
    static final String BRING_UP_RESTART_SERVICE = "restart service";
    static final String BRING_UP_START_SERVICE = "start service";
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    private static final boolean LOG_SERVICE_START_STOP = false;
    static final int MAX_SERVICE_INACTIVITY = 1800000;
    static final int SERVICE_BACKGROUND_TIMEOUT = 200000;
    static final int SERVICE_MIN_RESTART_TIME_BETWEEN = 10000;
    static final int SERVICE_RESET_RUN_DURATION = 60000;
    static final int SERVICE_RESTART_DURATION = 1000;
    static final int SERVICE_RESTART_DURATION_FACTOR = 4;
    static final int SERVICE_TIMEOUT = 20000;
    final ActivityManagerService mAm;
    private String mCurrentCallerPackage;
    private int mCurrentCallerUid;
    String mLastAnrDump;
    final int mMaxStartingBackground;
    private static final String TAG = "ActivityManager";
    private static final String TAG_MU = TAG + "_MU";
    private static final String TAG_SERVICE = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE;
    private static final String TAG_SERVICE_EXECUTING = TAG + ActivityManagerDebugConfig.POSTFIX_SERVICE_EXECUTING;
    private static final boolean DEBUG_DELAYED_SERVICE = ActivityManagerDebugConfig.DEBUG_SERVICE;
    private static final boolean DEBUG_DELAYED_STARTS = DEBUG_DELAYED_SERVICE;
    final SparseArray<ServiceMap> mServiceMap = new SparseArray<>();
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> mServiceConnections = new ArrayMap<>();
    final ArrayList<ServiceRecord> mPendingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mRestartingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mDestroyingServices = new ArrayList<>();
    private ArrayList<ServiceRecord> mTmpCollectionResults = null;
    private String mBringUpReason = "";
    final Runnable mLastAnrDumpClearer = new Runnable() {
        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.mLastAnrDump = null;
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    };

    class ServiceMap extends Handler {
        static final int MSG_BG_START_TIMEOUT = 1;
        final ArrayList<ServiceRecord> mDelayedStartList;
        final ArrayMap<Intent.FilterComparison, ServiceRecord> mServicesByIntent;
        final ArrayMap<ComponentName, ServiceRecord> mServicesByName;
        final ArrayList<ServiceRecord> mStartingBackground;
        final int mUserId;

        ServiceMap(Looper looper, int userId) {
            super(looper);
            this.mServicesByName = new ArrayMap<>();
            this.mServicesByIntent = new ArrayMap<>();
            this.mDelayedStartList = new ArrayList<>();
            this.mStartingBackground = new ArrayList<>();
            this.mUserId = userId;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            rescheduleDelayedStarts();
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                default:
                    return;
            }
        }

        void ensureNotStartingBackground(ServiceRecord r) {
            if (this.mStartingBackground.remove(r)) {
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "No longer background starting: " + r);
                }
                rescheduleDelayedStarts();
            }
            if (!this.mDelayedStartList.remove(r) || !ActiveServices.DEBUG_DELAYED_STARTS) {
                return;
            }
            Slog.v(ActiveServices.TAG_SERVICE, "No longer delaying start: " + r);
        }

        void rescheduleDelayedStarts() {
            removeMessages(1);
            long now = SystemClock.uptimeMillis();
            int i = 0;
            int N = this.mStartingBackground.size();
            while (i < N) {
                ServiceRecord r = this.mStartingBackground.get(i);
                if (r.startingBgTimeout <= now) {
                    Slog.i(ActiveServices.TAG, "Waited long enough for: " + r);
                    this.mStartingBackground.remove(i);
                    N--;
                    i--;
                }
                i++;
            }
            while (this.mDelayedStartList.size() > 0 && this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ServiceRecord r2 = this.mDelayedStartList.remove(0);
                if (ActiveServices.DEBUG_DELAYED_STARTS) {
                    Slog.v(ActiveServices.TAG_SERVICE, "REM FR DELAY LIST (exec next): " + r2);
                }
                if (r2.pendingStarts.size() <= 0) {
                    Slog.w(ActiveServices.TAG, "**** NO PENDING STARTS! " + r2 + " startReq=" + r2.startRequested + " delayedStop=" + r2.delayedStop);
                    r2.delayed = false;
                } else {
                    if (ActiveServices.DEBUG_DELAYED_SERVICE && this.mDelayedStartList.size() > 0) {
                        Slog.v(ActiveServices.TAG_SERVICE, "Remaining delayed list:");
                        for (int i2 = 0; i2 < this.mDelayedStartList.size(); i2++) {
                            Slog.v(ActiveServices.TAG_SERVICE, "  #" + i2 + ": " + this.mDelayedStartList.get(i2));
                        }
                    }
                    r2.delayed = false;
                    try {
                        ActiveServices.this.mBringUpReason = ActiveServices.BRING_UP_DELAYED_SERVICE;
                        ActiveServices.this.mCurrentCallerPackage = null;
                        ActiveServices.this.mCurrentCallerUid = -1;
                        ActiveServices.this.startServiceInnerLocked(this, r2.pendingStarts.get(0).intent, r2, false, true);
                    } catch (TransactionTooLargeException e) {
                    }
                }
            }
            if (this.mStartingBackground.size() > 0) {
                ServiceRecord next = this.mStartingBackground.get(0);
                long when = next.startingBgTimeout > now ? next.startingBgTimeout : now;
                if (ActiveServices.DEBUG_DELAYED_SERVICE) {
                    Slog.v(ActiveServices.TAG_SERVICE, "Top bg start is " + next + ", can delay others up to " + when);
                }
                Message msg = obtainMessage(1);
                sendMessageAtTime(msg, when);
            }
            if (this.mStartingBackground.size() >= ActiveServices.this.mMaxStartingBackground) {
                return;
            }
            ActiveServices.this.mAm.backgroundServicesFinishedLocked(this.mUserId);
        }
    }

    public ActiveServices(ActivityManagerService service) {
        this.mAm = service;
        int maxBg = 0;
        try {
            maxBg = Integer.parseInt(SystemProperties.get("ro.config.max_starting_bg", "0"));
        } catch (RuntimeException e) {
        }
        this.mMaxStartingBackground = maxBg <= 0 ? ActivityManager.isLowRamDeviceStatic() ? 1 : 8 : maxBg;
    }

    ServiceRecord getServiceByName(ComponentName name, int callingUser) {
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "getServiceByName(" + name + "), callingUser = " + callingUser);
        }
        return getServiceMap(callingUser).mServicesByName.get(name);
    }

    boolean hasBackgroundServices(int callingUser) {
        ServiceMap smap = this.mServiceMap.get(callingUser);
        return smap != null && smap.mStartingBackground.size() >= this.mMaxStartingBackground;
    }

    private ServiceMap getServiceMap(int callingUser) {
        ServiceMap smap = this.mServiceMap.get(callingUser);
        if (smap == null) {
            ServiceMap smap2 = new ServiceMap(this.mAm.mHandler.getLooper(), callingUser);
            this.mServiceMap.put(callingUser, smap2);
            return smap2;
        }
        return smap;
    }

    ArrayMap<ComponentName, ServiceRecord> getServices(int callingUser) {
        return getServiceMap(callingUser).mServicesByName;
    }

    ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, String callingPackage, int userId) throws TransactionTooLargeException {
        boolean callerFg;
        if (DEBUG_DELAYED_STARTS) {
            Slog.v(TAG_SERVICE, "startService: " + service + " type=" + resolvedType + " args=" + service.getExtras());
        }
        if (caller != null) {
            ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.d(TAG_SERVICE, "SVC-startService: " + service + " callerApp=" + callerApp);
            }
            if (callerApp == null) {
                throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when starting service " + service);
            }
            callerFg = callerApp.setSchedGroup != 0;
        } else {
            callerFg = true;
        }
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, callingPackage, callingPid, callingUid, userId, true, callerFg, false);
        if (res == null) {
            return null;
        }
        if (res.record == null) {
            return new ComponentName("!", res.permission != null ? res.permission : "private to package");
        }
        ServiceRecord r = res.record;
        if (!this.mAm.mUserController.exists(r.userId)) {
            Slog.w(TAG, "Trying to start service with non-existent user! " + r.userId);
            return null;
        }
        if (!r.startRequested) {
            long token = Binder.clearCallingIdentity();
            try {
                int allowed = this.mAm.checkAllowBackgroundLocked(r.appInfo.uid, r.packageName, callingPid, true);
                if (allowed != 0) {
                    Slog.w(TAG, "Background start not allowed: service " + service + " to " + r.name.flattenToShortString() + " from pid=" + callingPid + " uid=" + callingUid + " pkg=" + callingPackage);
                    return null;
                }
            } finally {
                Binder.restoreCallingIdentity(token);
            }
        }
        ActivityManagerService.NeededUriGrants neededGrants = this.mAm.checkGrantUriPermissionFromIntentLocked(callingUid, r.packageName, service, service.getFlags(), null, r.userId);
        if (Build.isPermissionReviewRequired() && !requestStartTargetPermissionsReviewIfNeededLocked(r, callingPackage, callingUid, service, callerFg, userId)) {
            return null;
        }
        if (unscheduleServiceRestartLocked(r, callingUid, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "START SERVICE WHILE RESTART PENDING: " + r);
        }
        r.lastActivity = SystemClock.uptimeMillis();
        r.startRequested = true;
        r.delayedStop = false;
        r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), service, neededGrants));
        ServiceMap smap = getServiceMap(r.userId);
        boolean addToStarting = false;
        if (!callerFg && r.app == null && this.mAm.mUserController.hasStartedUserState(r.userId)) {
            ProcessRecord proc = this.mAm.getProcessRecordLocked(r.processName, r.appInfo.uid, false);
            if (proc == null || proc.curProcState > 11) {
                if (DEBUG_DELAYED_SERVICE) {
                    Slog.v(TAG_SERVICE, "Potential start delay of " + r + " in " + proc);
                }
                if (r.delayed) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "Continuing to delay: " + r);
                    }
                    return r.name;
                }
                if (smap.mStartingBackground.size() >= this.mMaxStartingBackground) {
                    Slog.i(TAG_SERVICE, "Delaying start of: " + r);
                    smap.mDelayedStartList.add(r);
                    r.delayed = true;
                    if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                        if (r.delayedServiceCallerPkg == null) {
                            r.delayedServiceCallerPkg = new ArrayList<>();
                        }
                        r.delayedServiceCallerPkg.add(callingPackage);
                        if (r.delayedServiceCallerUid == null) {
                            r.delayedServiceCallerUid = new ArrayList<>();
                        }
                        r.delayedServiceCallerUid.add(Integer.valueOf(callingUid));
                    }
                    return r.name;
                }
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Not delaying: " + r);
                }
                addToStarting = true;
            } else if (proc.curProcState >= 10) {
                addToStarting = true;
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Not delaying, but counting as bg: " + r);
                }
            } else if (DEBUG_DELAYED_STARTS) {
                StringBuilder sb = new StringBuilder(128);
                sb.append("Not potential delay (state=").append(proc.curProcState).append(' ').append(proc.adjType);
                String reason = proc.makeAdjReason();
                if (reason != null) {
                    sb.append(' ');
                    sb.append(reason);
                }
                sb.append("): ");
                sb.append(r.toString());
                Slog.v(TAG_SERVICE, sb.toString());
            }
        } else if (DEBUG_DELAYED_STARTS) {
            if (callerFg) {
                Slog.v(TAG_SERVICE, "Not potential delay (callerFg=" + callerFg + " uid=" + callingUid + " pid=" + callingPid + "): " + r);
            } else if (r.app != null) {
                Slog.v(TAG_SERVICE, "Not potential delay (cur app=" + r.app + "): " + r);
            } else {
                Slog.v(TAG_SERVICE, "Not potential delay (user " + r.userId + " not started): " + r);
            }
        }
        this.mBringUpReason = BRING_UP_START_SERVICE;
        this.mCurrentCallerPackage = callingPackage;
        this.mCurrentCallerUid = callingUid;
        return startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
    }

    private boolean requestStartTargetPermissionsReviewIfNeededLocked(ServiceRecord r, String callingPackage, int callingUid, Intent service, boolean callerFg, final int userId) {
        if (this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(r.packageName, r.userId)) {
            if (!callerFg) {
                Slog.w(TAG, "u" + r.userId + " Starting a service in package" + r.packageName + " requires a permissions review");
                return false;
            }
            IIntentSender target = this.mAm.getIntentSenderLocked(4, callingPackage, callingUid, userId, null, null, 0, new Intent[]{service}, new String[]{service.resolveType(this.mAm.mContext.getContentResolver())}, 1409286144, null);
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", r.packageName);
            intent.putExtra("android.intent.extra.INTENT", new IntentSender(target));
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW || !ActivityManagerService.IS_USER_BUILD) {
                Slog.i(TAG, "u" + r.userId + " Launching permission review for package " + r.packageName);
            }
            this.mAm.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ActiveServices.this.mAm.mContext.startActivityAsUser(intent, new UserHandle(userId));
                }
            });
            return false;
        }
        return true;
    }

    ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting) throws TransactionTooLargeException {
        ServiceState stracker = r.getTracker();
        if (stracker != null) {
            stracker.setStarted(true, this.mAm.mProcessStats.getMemFactorLocked(), r.lastActivity);
        }
        r.callStart = false;
        synchronized (r.stats.getBatteryStats()) {
            r.stats.startRunningLocked();
        }
        String error = bringUpServiceLocked(r, service.getFlags(), callerFg, false, false);
        if (error != null) {
            return new ComponentName("!!", error);
        }
        if (r.startRequested && addToStarting) {
            boolean first = smap.mStartingBackground.size() == 0;
            smap.mStartingBackground.add(r);
            r.startingBgTimeout = SystemClock.uptimeMillis() + 15000;
            if (DEBUG_DELAYED_SERVICE) {
                RuntimeException here = new RuntimeException("here");
                here.fillInStackTrace();
                Slog.v(TAG_SERVICE, "Starting background (first=" + first + "): " + r, here);
            } else if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Starting background (first=" + first + "): " + r);
            }
            if (first) {
                smap.rescheduleDelayedStarts();
            }
        } else if (callerFg) {
            smap.ensureNotStartingBackground(r);
        }
        return r.name;
    }

    private void stopServiceLocked(ServiceRecord service) {
        if (service.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "Delaying stop of pending: " + service);
            }
            service.delayedStop = true;
            return;
        }
        synchronized (service.stats.getBatteryStats()) {
            service.stats.stopRunningLocked();
        }
        service.startRequested = false;
        if (service.tracker != null) {
            service.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        service.callStart = false;
        bringDownServiceIfNeededLocked(service, false, false);
    }

    int stopServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopService: " + service + " type=" + resolvedType);
        }
        ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
        if (caller != null && callerApp == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when stopping service " + service);
        }
        ServiceLookupResult r = retrieveServiceLocked(service, resolvedType, null, Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false, false);
        if (r == null) {
            return 0;
        }
        if (r.record == null) {
            return -1;
        }
        long origId = Binder.clearCallingIdentity();
        try {
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.d(TAG_SERVICE, "SVC-Stopping service: " + r.record + ", app=" + callerApp);
            }
            stopServiceLocked(r.record);
            Binder.restoreCallingIdentity(origId);
            return 1;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(origId);
            throw th;
        }
    }

    void stopInBackgroundLocked(int uid) {
        ServiceMap services = this.mServiceMap.get(UserHandle.getUserId(uid));
        ArrayList<ServiceRecord> stopping = null;
        if (services == null) {
            return;
        }
        for (int i = services.mServicesByName.size() - 1; i >= 0; i--) {
            ServiceRecord service = services.mServicesByName.valueAt(i);
            if (service.appInfo.uid == uid && service.startRequested && this.mAm.mAppOpsService.noteOperation(63, uid, service.packageName) != 0 && stopping == null) {
                stopping = new ArrayList<>();
                stopping.add(service);
            }
        }
        if (stopping == null) {
            return;
        }
        for (int i2 = stopping.size() - 1; i2 >= 0; i2--) {
            ServiceRecord service2 = stopping.get(i2);
            service2.delayed = false;
            services.ensureNotStartingBackground(service2);
            stopServiceLocked(service2);
        }
    }

    IBinder peekServiceLocked(Intent service, String resolvedType, String callingPackage) {
        ServiceLookupResult r = retrieveServiceLocked(service, resolvedType, callingPackage, Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.getCallingUserId(), false, false, false);
        if (r == null) {
            return null;
        }
        if (r.record == null) {
            throw new SecurityException("Permission Denial: Accessing service from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + r.permission);
        }
        IntentBindRecord ib = r.record.bindings.get(r.record.intent);
        if (ib == null) {
            return null;
        }
        IBinder ret = ib.binder;
        return ret;
    }

    boolean stopServiceTokenLocked(ComponentName className, IBinder token, int startId) {
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "stopServiceToken: " + className + " " + token + " startId=" + startId);
        }
        ServiceRecord r = findServiceLocked(className, token, UserHandle.getCallingUserId());
        if (r == null) {
            return false;
        }
        if (startId >= 0) {
            ServiceRecord.StartItem si = r.findDeliveredStart(startId, false);
            if (si != null) {
                while (r.deliveredStarts.size() > 0) {
                    ServiceRecord.StartItem cur = r.deliveredStarts.remove(0);
                    cur.removeUriPermissionsLocked();
                    if (cur == si) {
                        break;
                    }
                }
            }
            if (r.getLastStartId() != startId) {
                return false;
            }
            if (r.deliveredStarts.size() > 0) {
                Slog.w(TAG, "stopServiceToken startId " + startId + " is last, but have " + r.deliveredStarts.size() + " remaining args");
            }
        }
        synchronized (r.stats.getBatteryStats()) {
            r.stats.stopRunningLocked();
        }
        r.startRequested = false;
        if (r.tracker != null) {
            r.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        r.callStart = false;
        long origId = Binder.clearCallingIdentity();
        bringDownServiceIfNeededLocked(r, false, false);
        Binder.restoreCallingIdentity(origId);
        return true;
    }

    public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, int flags) {
        int userId = UserHandle.getCallingUserId();
        long origId = Binder.clearCallingIdentity();
        try {
            ServiceRecord r = findServiceLocked(className, token, userId);
            if (r != null) {
                if (id != 0) {
                    if (notification == null) {
                        throw new IllegalArgumentException("null notification");
                    }
                    if (r.foregroundId != id) {
                        r.cancelNotification();
                        r.foregroundId = id;
                    }
                    notification.flags |= 64;
                    r.foregroundNoti = notification;
                    r.isForeground = true;
                    r.postNotification();
                    if (r.app != null) {
                        updateServiceForegroundLocked(r.app, true);
                    }
                    getServiceMap(r.userId).ensureNotStartingBackground(r);
                    this.mAm.notifyPackageUse(r.serviceInfo.packageName, 2);
                } else {
                    if (r.isForeground) {
                        r.isForeground = false;
                        if (r.app != null) {
                            this.mAm.updateLruProcessLocked(r.app, false, null);
                            updateServiceForegroundLocked(r.app, true);
                        }
                    }
                    if ((flags & 1) != 0) {
                        r.cancelNotification();
                        r.foregroundId = 0;
                        r.foregroundNoti = null;
                    } else if (r.appInfo.targetSdkVersion >= 21) {
                        r.stripForegroundServiceFlagFromNotification();
                        if ((flags & 2) != 0) {
                            r.foregroundId = 0;
                            r.foregroundNoti = null;
                        }
                    }
                }
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void updateServiceForegroundLocked(ProcessRecord proc, boolean oomAdj) {
        boolean anyForeground = false;
        int i = proc.services.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            ServiceRecord sr = proc.services.valueAt(i);
            if (!sr.isForeground) {
                i--;
            } else {
                anyForeground = true;
                break;
            }
        }
        this.mAm.updateProcessForegroundLocked(proc, anyForeground, oomAdj);
    }

    private void updateWhitelistManagerLocked(ProcessRecord proc) {
        proc.whitelistManager = false;
        for (int i = proc.services.size() - 1; i >= 0; i--) {
            ServiceRecord sr = proc.services.valueAt(i);
            if (sr.whitelistManager) {
                proc.whitelistManager = true;
                return;
            }
        }
    }

    public void updateServiceConnectionActivitiesLocked(ProcessRecord clientProc) {
        ArraySet<ProcessRecord> updatedProcesses = null;
        for (int i = 0; i < clientProc.connections.size(); i++) {
            ConnectionRecord conn = clientProc.connections.valueAt(i);
            ProcessRecord proc = conn.binding.service.app;
            if (proc != null && proc != clientProc) {
                if (updatedProcesses == null) {
                    updatedProcesses = new ArraySet<>();
                } else if (updatedProcesses.contains(proc)) {
                }
                updatedProcesses.add(proc);
                updateServiceClientActivitiesLocked(proc, null, false);
            }
        }
    }

    private boolean updateServiceClientActivitiesLocked(ProcessRecord proc, ConnectionRecord modCr, boolean updateLru) {
        if (modCr != null && modCr.binding.client != null && modCr.binding.client.activities.size() <= 0) {
            return false;
        }
        boolean anyClientActivities = false;
        for (int i = proc.services.size() - 1; i >= 0 && !anyClientActivities; i--) {
            ServiceRecord sr = proc.services.valueAt(i);
            for (int conni = sr.connections.size() - 1; conni >= 0 && !anyClientActivities; conni--) {
                ArrayList<ConnectionRecord> clist = sr.connections.valueAt(conni);
                int cri = clist.size() - 1;
                while (true) {
                    if (cri >= 0) {
                        ConnectionRecord cr = clist.get(cri);
                        if (cr.binding.client != null && cr.binding.client != proc && cr.binding.client.activities.size() > 0) {
                            anyClientActivities = true;
                            break;
                        }
                        cri--;
                    }
                }
            }
        }
        if (anyClientActivities == proc.hasClientActivities) {
            return false;
        }
        proc.hasClientActivities = anyClientActivities;
        if (updateLru) {
            this.mAm.updateLruProcessLocked(proc, anyClientActivities, null);
            return true;
        }
        return true;
    }

    int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service, String resolvedType, final IServiceConnection connection, int flags, final String callingPackage, final int userId) throws TransactionTooLargeException {
        ServiceState stracker;
        final ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "bindService: " + service + " type=" + resolvedType + " conn=" + connection.asBinder() + " flags=0x" + Integer.toHexString(flags) + " callerApp=" + callerApp);
        }
        if (callerApp == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when binding service " + service);
        }
        ActivityRecord activity = null;
        if (token != null && (activity = ActivityRecord.isInStackLocked(token)) == null) {
            Slog.w(TAG, "Binding with unknown activity: " + token);
            return 0;
        }
        int clientLabel = 0;
        PendingIntent clientIntent = null;
        boolean isCallerSystem = callerApp.info.uid == 1000;
        if (isCallerSystem) {
            service.setDefusable(true);
            clientIntent = (PendingIntent) service.getParcelableExtra("android.intent.extra.client_intent");
            if (clientIntent != null && (clientLabel = service.getIntExtra("android.intent.extra.client_label", 0)) != 0) {
                service = service.cloneFilter();
            }
        }
        if ((134217728 & flags) != 0) {
            this.mAm.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "BIND_TREAT_LIKE_ACTIVITY");
        }
        if ((16777216 & flags) != 0 && !isCallerSystem) {
            throw new SecurityException("Non-system caller " + caller + " (pid=" + Binder.getCallingPid() + ") set BIND_ALLOW_WHITELIST_MANAGEMENT when binding service " + service);
        }
        boolean callerFg = callerApp.setSchedGroup != 0;
        boolean isBindExternal = (Integer.MIN_VALUE & flags) != 0;
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, callingPackage, Binder.getCallingPid(), Binder.getCallingUid(), userId, true, callerFg, isBindExternal);
        if (res == null) {
            return 0;
        }
        if (res.record == null) {
            return -1;
        }
        final ServiceRecord s = res.record;
        boolean permissionsReviewRequired = false;
        if (Build.isPermissionReviewRequired() && this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(s.packageName, s.userId)) {
            permissionsReviewRequired = true;
            if (!callerFg) {
                Slog.w(TAG, "u" + s.userId + " Binding to a service in package" + s.packageName + " requires a permissions review");
                return 0;
            }
            final Intent serviceIntent = service;
            final boolean z = callerFg;
            Parcelable remoteCallback = new RemoteCallback(new RemoteCallback.OnResultListener() {
                public void onResult(Bundle result) {
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            long identity = Binder.clearCallingIdentity();
                            try {
                                if (!ActiveServices.this.mPendingServices.contains(s)) {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                    return;
                                }
                                if (ActiveServices.this.mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(s.packageName, s.userId)) {
                                    ActiveServices.this.unbindServiceLocked(connection);
                                } else {
                                    try {
                                        ActiveServices.this.mBringUpReason = ActiveServices.BRING_UP_BIND_SERVICE;
                                        ActiveServices.this.mCurrentCallerPackage = callingPackage;
                                        ActiveServices.this.mCurrentCallerUid = callerApp.info.uid;
                                        ActiveServices.this.bringUpServiceLocked(s, serviceIntent.getFlags(), z, false, false);
                                    } catch (RemoteException e) {
                                    }
                                }
                                ActivityManagerService.resetPriorityAfterLockedSection();
                            } finally {
                                Binder.restoreCallingIdentity(identity);
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                }
            });
            final Intent intent = new Intent("android.intent.action.REVIEW_PERMISSIONS");
            intent.addFlags(276824064);
            intent.putExtra("android.intent.extra.PACKAGE_NAME", s.packageName);
            intent.putExtra("android.intent.extra.REMOTE_CALLBACK", remoteCallback);
            if (ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW || !ActivityManagerService.IS_USER_BUILD) {
                Slog.i(TAG, "u" + s.userId + " Launching permission review for package " + s.packageName);
            }
            this.mAm.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    ActiveServices.this.mAm.mContext.startActivityAsUser(intent, new UserHandle(userId));
                }
            });
        }
        long origId = Binder.clearCallingIdentity();
        try {
            if (unscheduleServiceRestartLocked(s, callerApp.info.uid, false) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "BIND SERVICE WHILE RESTART PENDING: " + s);
            }
            if ((flags & 1) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                if (!s.hasAutoCreateConnections() && (stracker = s.getTracker()) != null) {
                    stracker.setBound(true, this.mAm.mProcessStats.getMemFactorLocked(), s.lastActivity);
                }
            }
            this.mAm.startAssociationLocked(callerApp.uid, callerApp.processName, callerApp.curProcState, s.appInfo.uid, s.name, s.processName);
            AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);
            ConnectionRecord c = new ConnectionRecord(b, activity, connection, flags, clientLabel, clientIntent);
            IBinder binder = connection.asBinder();
            ArrayList<ConnectionRecord> clist = s.connections.get(binder);
            if (clist == null) {
                clist = new ArrayList<>();
                s.connections.put(binder, clist);
            }
            clist.add(c);
            b.connections.add(c);
            if (activity != null) {
                if (activity.connections == null) {
                    activity.connections = new HashSet<>();
                }
                activity.connections.add(c);
            }
            b.client.connections.add(c);
            if ((c.flags & 8) != 0) {
                b.client.hasAboveClient = true;
            }
            if ((c.flags & 16777216) != 0) {
                s.whitelistManager = true;
            }
            if (s.app != null) {
                updateServiceClientActivitiesLocked(s.app, c, true);
            }
            ArrayList<ConnectionRecord> clist2 = this.mServiceConnections.get(binder);
            if (clist2 == null) {
                clist2 = new ArrayList<>();
                this.mServiceConnections.put(binder, clist2);
            }
            clist2.add(c);
            if ((flags & 1) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                this.mBringUpReason = BRING_UP_BIND_SERVICE;
                this.mCurrentCallerPackage = callingPackage;
                this.mCurrentCallerUid = callerApp.info.uid;
                if (bringUpServiceLocked(s, service.getFlags(), callerFg, false, permissionsReviewRequired) != null) {
                    return 0;
                }
            }
            if (s.app != null) {
                if (this.mAm.mWallpaperClassName != null && s.name.equals(this.mAm.mWallpaperClassName)) {
                    this.mAm.mWallpaperProcess = s.app;
                }
                if ((134217728 & flags) != 0) {
                    s.app.treatLikeActivity = true;
                }
                if (s.whitelistManager) {
                    s.app.whitelistManager = true;
                }
                this.mAm.updateLruProcessLocked(s.app, !s.app.hasClientActivities ? s.app.treatLikeActivity : true, b.client);
                this.mAm.updateOomAdjLocked(s.app);
            }
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Bind " + s + " with " + b + ": received=" + b.intent.received + " apps=" + b.intent.apps.size() + " doRebind=" + b.intent.doRebind);
            }
            if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.d(TAG_SERVICE, "SVC-Binding service: " + s + ", app=" + s.app + ", activity=" + activity);
            }
            if (s.app != null && b.intent.received) {
                try {
                    c.conn.connected(s.name, b.intent.binder);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure sending service " + s.shortName + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                }
                if (b.intent.apps.size() == 1 && b.intent.doRebind) {
                    requestServiceBindingLocked(s, b.intent, callerFg, true);
                }
            } else if (!b.intent.requested) {
                requestServiceBindingLocked(s, b.intent, callerFg, false);
            }
            getServiceMap(s.userId).ensureNotStartingBackground(s);
            Binder.restoreCallingIdentity(origId);
            return 1;
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    private void foo() {
    }

    void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
        long origId = Binder.clearCallingIdentity();
        try {
            if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "PUBLISHING " + r + " " + intent + ": " + service);
            }
            if (r != null) {
                Intent.FilterComparison filter = new Intent.FilterComparison(intent);
                IntentBindRecord b = r.bindings.get(filter);
                if (b != null && !b.received) {
                    b.binder = service;
                    b.requested = true;
                    b.received = true;
                    for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
                        ArrayList<ConnectionRecord> clist = r.connections.valueAt(conni);
                        for (int i = 0; i < clist.size(); i++) {
                            ConnectionRecord c = clist.get(i);
                            if (filter.equals(c.binding.intent.intent)) {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Publishing to: " + c);
                                }
                                try {
                                    c.conn.connected(r.name, service);
                                } catch (Exception e) {
                                    Slog.w(TAG, "Failure sending service " + r.name + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                                }
                            } else {
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Not publishing to: " + c);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Bound intent: " + c.binding.intent.intent);
                                }
                                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                                    Slog.v(TAG_SERVICE, "Published intent: " + intent);
                                }
                            }
                        }
                    }
                }
                serviceDoneExecutingLocked(r, this.mDestroyingServices.contains(r), false);
            }
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    boolean unbindServiceLocked(IServiceConnection connection) {
        IBinder binder = connection.asBinder();
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "unbindService: conn=" + binder);
        }
        ArrayList<ConnectionRecord> clist = this.mServiceConnections.get(binder);
        if (clist == null) {
            Slog.w(TAG, "Unbind failed: could not find connection for " + connection.asBinder());
            return false;
        }
        long origId = Binder.clearCallingIdentity();
        while (clist.size() > 0) {
            try {
                ConnectionRecord r = clist.get(0);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(TAG_SERVICE, "SVC-Unbinding service: " + r.binding.service + ", app=" + r.binding.service.app);
                }
                removeConnectionLocked(r, null, null);
                if (clist.size() > 0 && clist.get(0) == r) {
                    Slog.wtf(TAG, "Connection " + r + " not removed for binder " + binder);
                    clist.remove(0);
                }
                if (r.binding.service.app != null) {
                    if (r.binding.service.app.whitelistManager) {
                        updateWhitelistManagerLocked(r.binding.service.app);
                    }
                    if ((r.flags & 134217728) != 0) {
                        r.binding.service.app.treatLikeActivity = true;
                        this.mAm.updateLruProcessLocked(r.binding.service.app, !r.binding.service.app.hasClientActivities ? r.binding.service.app.treatLikeActivity : true, null);
                    }
                    this.mAm.updateOomAdjLocked(r.binding.service.app);
                }
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
        return true;
    }

    void unbindFinishedLocked(ServiceRecord r, Intent intent, boolean doRebind) {
        long origId = Binder.clearCallingIdentity();
        if (r != null) {
            try {
                Intent.FilterComparison filter = new Intent.FilterComparison(intent);
                IntentBindRecord b = r.bindings.get(filter);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "unbindFinished in " + r + " at " + b + ": apps=" + (b != null ? b.apps.size() : 0));
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                if (b != null) {
                    if (b.apps.size() <= 0 || inDestroying) {
                        b.doRebind = true;
                    } else {
                        boolean inFg = false;
                        int i = b.apps.size() - 1;
                        while (true) {
                            if (i >= 0) {
                                ProcessRecord client = b.apps.valueAt(i).client;
                                if (client != null && client.setSchedGroup != 0) {
                                    inFg = true;
                                    break;
                                }
                                i--;
                            } else {
                                break;
                            }
                        }
                        try {
                            requestServiceBindingLocked(r, b, inFg, true);
                        } catch (TransactionTooLargeException e) {
                        }
                    }
                }
                serviceDoneExecutingLocked(r, inDestroying, false);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    private final ServiceRecord findServiceLocked(ComponentName name, IBinder token, int userId) {
        ServiceRecord r = getServiceByName(name, userId);
        if (r == token) {
            return r;
        }
        return null;
    }

    private final class ServiceLookupResult {
        final String permission;
        final ServiceRecord record;

        ServiceLookupResult(ServiceRecord _record, String _permission) {
            this.record = _record;
            this.permission = _permission;
        }
    }

    private class ServiceRestarter implements Runnable {
        private ServiceRecord mService;

        ServiceRestarter(ActiveServices this$0, ServiceRestarter serviceRestarter) {
            this();
        }

        private ServiceRestarter() {
        }

        void setService(ServiceRecord service) {
            this.mService = service;
        }

        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    ActiveServices.this.performServiceRestartLocked(this.mService);
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }

    private ServiceLookupResult retrieveServiceLocked(Intent service, String resolvedType, String callingPackage, int callingPid, int callingUid, int userId, boolean createIfNeeded, boolean callingFromFg, boolean isBindExternal) {
        ServiceInfo sInfo;
        ServiceInfo sInfo2;
        BatteryStatsImpl.Uid.Pkg.Serv ss;
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "retrieveServiceLocked: " + service + " type=" + resolvedType + " callingUid=" + callingUid);
        }
        int userId2 = this.mAm.mUserController.handleIncomingUser(callingPid, callingUid, userId, false, 1, "service", null);
        ServiceMap smap = getServiceMap(userId2);
        ComponentName comp = service.getComponent();
        ServiceRecord r = comp != null ? smap.mServicesByName.get(comp) : null;
        if (r == null && !isBindExternal) {
            r = smap.mServicesByIntent.get(new Intent.FilterComparison(service));
        }
        if (r != null && (r.serviceInfo.flags & 4) != 0 && !callingPackage.equals(r.packageName)) {
            r = null;
        }
        if (r == null) {
            try {
                ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, resolvedType, 268436480, userId2);
                if (rInfo != null) {
                    ServiceInfo sInfo3 = rInfo.serviceInfo;
                    sInfo = sInfo3;
                } else {
                    sInfo = null;
                }
                if (sInfo == null) {
                    Slog.w(TAG_SERVICE, "Unable to start service " + service + " U=" + userId2 + ": not found");
                    return null;
                }
                ComponentName name = new ComponentName(sInfo.applicationInfo.packageName, sInfo.name);
                if ((sInfo.flags & 4) != 0) {
                    if (!isBindExternal) {
                        throw new SecurityException("BIND_EXTERNAL_SERVICE required for " + name);
                    }
                    if (!sInfo.exported) {
                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + name + " is not exported");
                    }
                    if ((sInfo.flags & 2) == 0) {
                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + name + " is not an isolatedProcess");
                    }
                    ApplicationInfo aInfo = AppGlobals.getPackageManager().getApplicationInfo(callingPackage, 1024, userId2);
                    if (aInfo == null) {
                        throw new SecurityException("BIND_EXTERNAL_SERVICE failed, could not resolve client package " + callingPackage);
                    }
                    ServiceInfo sInfo4 = new ServiceInfo(sInfo);
                    sInfo4.applicationInfo = new ApplicationInfo(sInfo4.applicationInfo);
                    sInfo4.applicationInfo.packageName = aInfo.packageName;
                    sInfo4.applicationInfo.uid = aInfo.uid;
                    ComponentName name2 = new ComponentName(aInfo.packageName, name.getClassName());
                    service.setComponent(name2);
                    name = name2;
                    sInfo = sInfo4;
                } else if (isBindExternal) {
                    throw new SecurityException("BIND_EXTERNAL_SERVICE failed, " + name + " is not an externalService");
                }
                if (userId2 > 0) {
                    if (this.mAm.isSingleton(sInfo.processName, sInfo.applicationInfo, sInfo.name, sInfo.flags) && this.mAm.isValidSingletonCall(callingUid, sInfo.applicationInfo.uid)) {
                        userId2 = 0;
                        smap = getServiceMap(0);
                    }
                    sInfo2 = new ServiceInfo(sInfo);
                    sInfo2.applicationInfo = this.mAm.getAppInfoForUser(sInfo2.applicationInfo, userId2);
                } else {
                    sInfo2 = sInfo;
                }
                ServiceRecord r2 = smap.mServicesByName.get(name);
                if (r2 == null && createIfNeeded) {
                    try {
                        Intent.FilterComparison filter = new Intent.FilterComparison(service.cloneFilter());
                        ServiceRestarter res = new ServiceRestarter(this, null);
                        BatteryStatsImpl stats = this.mAm.mBatteryStatsService.getActiveStatistics();
                        synchronized (stats) {
                            ss = stats.getServiceStatsLocked(sInfo2.applicationInfo.uid, sInfo2.packageName, sInfo2.name);
                        }
                        r = new ServiceRecord(this.mAm, ss, name, filter, sInfo2, callingFromFg, res);
                        res.setService(r);
                        smap.mServicesByName.put(name, r);
                        smap.mServicesByIntent.put(filter, r);
                        for (int i = this.mPendingServices.size() - 1; i >= 0; i--) {
                            ServiceRecord pr = this.mPendingServices.get(i);
                            if (pr.serviceInfo.applicationInfo.uid == sInfo2.applicationInfo.uid && pr.name.equals(name)) {
                                this.mPendingServices.remove(i);
                            }
                        }
                    } catch (RemoteException e) {
                        r = r2;
                    }
                } else {
                    r = r2;
                }
            } catch (RemoteException e2) {
            }
        }
        if (r == null) {
            return null;
        }
        if (this.mAm.checkComponentPermission(r.permission, callingPid, callingUid, r.appInfo.uid, r.exported) != 0) {
            if (r.exported) {
                Slog.w(TAG, "Permission Denial: Accessing service " + r.name + " from pid=" + callingPid + ", uid=" + callingUid + " requires " + r.permission);
                return new ServiceLookupResult(null, r.permission);
            }
            Slog.w(TAG, "Permission Denial: Accessing service " + r.name + " from pid=" + callingPid + ", uid=" + callingUid + " that is not exported from uid " + r.appInfo.uid);
            return new ServiceLookupResult(null, "not exported from uid " + r.appInfo.uid);
        }
        if (r.permission != null && callingPackage != null) {
            int opCode = AppOpsManager.permissionToOpCode(r.permission);
            if (opCode != -1 && this.mAm.mAppOpsService.noteOperation(opCode, callingUid, callingPackage) != 0) {
                Slog.w(TAG, "Appop Denial: Accessing service " + r.name + " from pid=" + callingPid + ", uid=" + callingUid + " requires appop " + AppOpsManager.opToName(opCode));
                return null;
            }
        }
        if (this.mAm.mIntentFirewall.checkService(r.name, service, callingUid, callingPid, resolvedType, r.appInfo)) {
            return new ServiceLookupResult(r, null);
        }
        return null;
    }

    private final void bumpServiceExecutingLocked(ServiceRecord r, boolean fg, String why) {
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, ">>> EXECUTING " + why + " of " + r + " in app " + r.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, ">>> EXECUTING " + why + " of " + r.shortName);
        }
        long now = SystemClock.uptimeMillis();
        if (r.executeNesting == 0) {
            r.executeFg = fg;
            ServiceState stracker = r.getTracker();
            if (stracker != null) {
                stracker.setExecuting(true, this.mAm.mProcessStats.getMemFactorLocked(), now);
            }
            if (r.app != null) {
                r.app.executingServices.add(r);
                r.app.execServicesFg |= fg;
                if (r.app.executingServices.size() == 1) {
                    scheduleServiceTimeoutLocked(r.app);
                }
            }
        } else if (r.app != null && fg && !r.app.execServicesFg) {
            r.app.execServicesFg = true;
            scheduleServiceTimeoutLocked(r.app);
        }
        r.executeFg |= fg;
        r.executeNesting++;
        r.executingStart = now;
    }

    private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i, boolean execInFg, boolean rebind) throws TransactionTooLargeException {
        if (r.app == null || r.app.thread == null) {
            return false;
        }
        if ((!i.requested || rebind) && i.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind");
                r.app.forceProcessStateUpTo(10);
                r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind, r.app.repProcState);
                if (!rebind) {
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + r, e);
                }
                boolean inDestroying = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                throw e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while binding " + r);
                }
                boolean inDestroying2 = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying2, inDestroying2);
                return false;
            }
        }
        return true;
    }

    private final boolean scheduleServiceRestartLocked(ServiceRecord r, boolean allowCancel) {
        boolean repeat;
        boolean canceled = false;
        if (this.mAm.isShuttingDownLocked()) {
            Slog.w(TAG, "Not scheduling restart of crashed service " + r.shortName + " - system is shutting down");
            return false;
        }
        ServiceMap smap = getServiceMap(r.userId);
        if (smap.mServicesByName.get(r.name) != r) {
            ServiceRecord cur = smap.mServicesByName.get(r.name);
            Slog.wtf(TAG, "Attempting to schedule restart of " + r + " when found in map: " + cur);
            return false;
        }
        long now = SystemClock.uptimeMillis();
        if ((r.serviceInfo.applicationInfo.flags & 8) == 0) {
            long minDuration = 1000;
            long resetTime = 60000;
            int N = r.deliveredStarts.size();
            if (N > 0) {
                for (int i = N - 1; i >= 0; i--) {
                    ServiceRecord.StartItem si = r.deliveredStarts.get(i);
                    si.removeUriPermissionsLocked();
                    if (si.intent != null) {
                        if (!allowCancel || (si.deliveryCount < 3 && si.doneExecutingCount < 6)) {
                            r.pendingStarts.add(0, si);
                            long dur = (SystemClock.uptimeMillis() - si.deliveredTime) * 2;
                            if (minDuration < dur) {
                                minDuration = dur;
                            }
                            if (resetTime < dur) {
                                resetTime = dur;
                            }
                        } else {
                            Slog.w(TAG, "Canceling start item " + si.intent + " in service " + r.name);
                            canceled = true;
                        }
                    }
                }
                r.deliveredStarts.clear();
            }
            r.totalRestartCount++;
            if (r.restartDelay == 0) {
                r.restartCount++;
                r.restartDelay = minDuration;
            } else if (now > r.restartTime + resetTime) {
                r.restartCount = 1;
                r.restartDelay = minDuration;
            } else {
                r.restartDelay *= 4;
                if (r.restartDelay < minDuration) {
                    r.restartDelay = minDuration;
                }
            }
            r.nextRestartTime = r.restartDelay + now;
            do {
                repeat = false;
                int i2 = this.mRestartingServices.size() - 1;
                while (true) {
                    if (i2 < 0) {
                        break;
                    }
                    ServiceRecord r2 = this.mRestartingServices.get(i2);
                    if (r2 != r && r.nextRestartTime >= r2.nextRestartTime - JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY && r.nextRestartTime < r2.nextRestartTime + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY) {
                        r.nextRestartTime = r2.nextRestartTime + JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY;
                        r.restartDelay = r.nextRestartTime - now;
                        repeat = true;
                        break;
                    }
                    i2--;
                }
            } while (repeat);
        } else {
            r.totalRestartCount++;
            r.restartCount = 0;
            r.restartDelay = 0L;
            r.nextRestartTime = now;
        }
        if (!this.mRestartingServices.contains(r)) {
            r.createdFromFg = false;
            this.mRestartingServices.add(r);
            r.makeRestarting(this.mAm.mProcessStats.getMemFactorLocked(), now);
        }
        r.cancelNotification();
        this.mAm.mHandler.removeCallbacks(r.restarter);
        this.mAm.mHandler.postAtTime(r.restarter, r.nextRestartTime);
        r.nextRestartTime = SystemClock.uptimeMillis() + r.restartDelay;
        Slog.w(TAG, "Scheduling restart of crashed service " + r.shortName + " in " + r.restartDelay + "ms");
        EventLog.writeEvent(EventLogTags.AM_SCHEDULE_SERVICE_RESTART, Integer.valueOf(r.userId), r.shortName, Long.valueOf(r.restartDelay));
        return canceled;
    }

    final void performServiceRestartLocked(ServiceRecord r) {
        if (!this.mRestartingServices.contains(r)) {
            return;
        }
        if (!isServiceNeeded(r, false, false)) {
            Slog.e(TAG, "Restarting service that is not needed: " + r);
            return;
        }
        try {
            this.mBringUpReason = BRING_UP_RESTART_SERVICE;
            this.mCurrentCallerPackage = null;
            this.mCurrentCallerUid = -1;
            bringUpServiceLocked(r, r.intent.getIntent().getFlags(), r.createdFromFg, true, false);
        } catch (TransactionTooLargeException e) {
        }
    }

    private final boolean unscheduleServiceRestartLocked(ServiceRecord r, int callingUid, boolean force) {
        if (!force && r.restartDelay == 0) {
            return false;
        }
        boolean removed = this.mRestartingServices.remove(r);
        if (removed || callingUid != r.appInfo.uid) {
            r.resetRestartCounter();
        }
        if (removed) {
            clearRestartingIfNeededLocked(r);
        }
        this.mAm.mHandler.removeCallbacks(r.restarter);
        return true;
    }

    private void clearRestartingIfNeededLocked(ServiceRecord r) {
        if (r.restartTracker == null) {
            return;
        }
        boolean stillTracking = false;
        int i = this.mRestartingServices.size() - 1;
        while (true) {
            if (i < 0) {
                break;
            }
            if (this.mRestartingServices.get(i).restartTracker != r.restartTracker) {
                i--;
            } else {
                stillTracking = true;
                break;
            }
        }
        if (stillTracking) {
            return;
        }
        r.restartTracker.setRestarting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        r.restartTracker = null;
    }

    private String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired) throws TransactionTooLargeException {
        ProcessRecord app;
        ProcessRecord proc;
        if (r.app != null && r.app.thread != null) {
            sendServiceArgsLocked(r, execInFg, false);
            return null;
        }
        if (!whileRestarting && r.restartDelay > 0) {
            return null;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Bringing up " + r + " " + r.intent);
        }
        if (this.mRestartingServices.remove(r)) {
            r.resetRestartCounter();
            clearRestartingIfNeededLocked(r);
        }
        if (r.delayed) {
            if (DEBUG_DELAYED_STARTS) {
                Slog.v(TAG_SERVICE, "REM FR DELAY LIST (bring up): " + r);
            }
            getServiceMap(r.userId).mDelayedStartList.remove(r);
            r.delayed = false;
        }
        if (!this.mAm.mUserController.hasStartedUserState(r.userId)) {
            String msg = "Unable to launch app " + r.appInfo.packageName + "/" + r.appInfo.uid + " for service " + r.intent.getIntent() + ": user " + r.userId + " is stopped";
            Slog.w(TAG, msg);
            bringDownServiceLocked(r);
            return msg;
        }
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(r.packageName, false, r.userId);
        } catch (RemoteException e) {
        } catch (IllegalArgumentException e2) {
            Slog.w(TAG, "Failed trying to unstop package " + r.packageName + ": " + e2);
        }
        boolean isolated = (r.serviceInfo.flags & 2) != 0;
        String procName = r.processName;
        if (isolated) {
            app = r.isolatedProc;
        } else {
            app = this.mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);
            if (ActivityManagerDebugConfig.DEBUG_MU) {
                Slog.v(TAG_MU, "bringUpServiceLocked: appInfo.uid=" + r.appInfo.uid + " app=" + app);
            }
            if (app != null && app.thread != null) {
                try {
                    app.addPackage(r.appInfo.packageName, r.appInfo.versionCode, this.mAm.mProcessStats);
                    realStartServiceLocked(r, app, execInFg);
                    return null;
                } catch (TransactionTooLargeException e3) {
                    throw e3;
                } catch (RemoteException e4) {
                    Slog.w(TAG, "Exception when starting service " + r.shortName, e4);
                }
            }
        }
        if (app == null && !permissionsReviewRequired) {
            String suppressAction = "allowed";
            if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                List<String> callerList = new ArrayList<>();
                List<Integer> callerUidList = new ArrayList<>();
                List<String> clientList = new ArrayList<>();
                List<Integer> clientUidList = new ArrayList<>();
                if (this.mCurrentCallerPackage != null) {
                    callerList.add(this.mCurrentCallerPackage);
                    callerUidList.add(Integer.valueOf(this.mCurrentCallerUid));
                }
                for (int conni = 0; conni < r.connections.size(); conni++) {
                    ProcessRecord client = null;
                    ArrayList<ConnectionRecord> clist = r.connections.valueAt(conni);
                    if (clist != null) {
                        for (int i = 0; i < clist.size(); i++) {
                            ConnectionRecord cr = clist.get(i);
                            if (cr != null && cr.binding != null) {
                                client = cr.binding.client;
                            }
                            if (client != null) {
                                for (int j = 0; j < client.pkgList.size(); j++) {
                                    clientList.add(client.pkgList.keyAt(j));
                                    clientUidList.add(Integer.valueOf(client.userId));
                                }
                            }
                        }
                    }
                }
                AMEventHookData.ReadyToStartComponent eventHookData = AMEventHookData.ReadyToStartComponent.createInstance();
                eventHookData.set(new Object[]{r.appInfo.packageName, Integer.valueOf(r.appInfo.uid), callerList, callerUidList, r.delayedServiceCallerPkg, r.delayedServiceCallerUid, clientList, clientUidList, this.mBringUpReason, "allowed"});
                this.mAm.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartComponent, eventHookData);
                suppressAction = eventHookData.getString(AMEventHookData.ReadyToStartComponent.Index.suppressAction);
                Slog.d(TAG, "[process suppression] suppressAction = " + suppressAction);
            }
            if (suppressAction == null || !(suppressAction.equals("delayed") || suppressAction.equals("skipped"))) {
                if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
                    AMEventHookData.PackageStoppedStatusChanged eventData = AMEventHookData.PackageStoppedStatusChanged.createInstance();
                    eventData.set(new Object[]{r.packageName, 0, "bringUpServiceLocked"});
                    this.mAm.getAMEventHook().hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData);
                }
                app = this.mAm.startProcessLocked(procName, r.appInfo, true, intentFlags, "service", r.name, false, isolated, false);
                if (app == null) {
                    String msg2 = "Unable to launch app " + r.appInfo.packageName + "/" + r.appInfo.uid + " for service " + r.intent.getIntent() + ": process is bad";
                    Slog.w(TAG, msg2);
                    bringDownServiceLocked(r);
                    return msg2;
                }
                if (isolated) {
                    r.isolatedProc = app;
                }
            } else {
                Slog.d(TAG, "[process suppression] bringUpServiceLocked : suppress process to start for service!");
                try {
                    AppGlobals.getPackageManager().setPackageStoppedState(r.packageName, true, r.userId);
                } catch (RemoteException e5) {
                    Slog.w(TAG, "RemoteException: " + e5);
                } catch (IllegalArgumentException e6) {
                    Slog.w(TAG, "Failed trying to stop package " + r.packageName + ": " + e6);
                }
                if (suppressAction.equals("delayed")) {
                    boolean canceled = scheduleServiceRestartLocked(r, true);
                    if (canceled) {
                        bringDownServiceLocked(r);
                        Slog.d(TAG, "[process suppression] restart canceled!");
                    }
                }
            }
        } else if ("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) {
            AMEventHookData.PackageStoppedStatusChanged eventData2 = AMEventHookData.PackageStoppedStatusChanged.createInstance();
            eventData2.set(new Object[]{r.packageName, 0, "bringUpServiceLocked"});
            this.mAm.getAMEventHook().hook(AMEventHook.Event.AM_PackageStoppedStatusChanged, eventData2);
        }
        if (("1".equals(SystemProperties.get("persist.runningbooster.support")) || "1".equals(SystemProperties.get("ro.mtk_aws_support"))) && app != null) {
            AMEventHookData.ReadyToStartService eventData3 = AMEventHookData.ReadyToStartService.createInstance();
            ArrayList<String> clientPkgList = null;
            ArrayList<Integer> clientUidList2 = null;
            if (this.mBringUpReason.equalsIgnoreCase(BRING_UP_RESTART_SERVICE)) {
                clientPkgList = new ArrayList<>();
                clientUidList2 = new ArrayList<>();
                for (int i2 = 0; i2 < r.connections.size(); i2++) {
                    ArrayList<ConnectionRecord> clist2 = r.connections.valueAt(i2);
                    for (int j2 = 0; j2 < clist2.size(); j2++) {
                        ConnectionRecord conn = clist2.get(j2);
                        if (conn.binding != null && (proc = conn.binding.client) != null) {
                            int clientUid = proc.uid;
                            for (Map.Entry<String, ProcessStats.ProcessStateHolder> entry : proc.pkgList.entrySet()) {
                                String clientPkgName = entry.getKey();
                                clientPkgList.add(clientPkgName);
                                clientUidList2.add(Integer.valueOf(clientUid));
                            }
                        }
                    }
                }
            }
            eventData3.set(new Object[]{r.appInfo.packageName, this.mCurrentCallerPackage, Integer.valueOf(this.mCurrentCallerUid), clientPkgList, clientUidList2, r.delayedServiceCallerPkg, r.delayedServiceCallerUid, this.mBringUpReason});
            this.mAm.getAMEventHook().hook(AMEventHook.Event.AM_ReadyToStartService, eventData3);
            r.delayedServiceCallerPkg = null;
            r.delayedServiceCallerUid = null;
        }
        if (!this.mPendingServices.contains(r)) {
            this.mPendingServices.add(r);
        }
        if (!r.delayedStop) {
            return null;
        }
        r.delayedStop = false;
        if (!r.startRequested) {
            return null;
        }
        if (DEBUG_DELAYED_STARTS) {
            Slog.v(TAG_SERVICE, "Applying delayed stop (in bring up): " + r);
        }
        stopServiceLocked(r);
        return null;
    }

    private final void requestServiceBindingsLocked(ServiceRecord r, boolean execInFg) throws TransactionTooLargeException {
        for (int i = r.bindings.size() - 1; i >= 0; i--) {
            IntentBindRecord ibr = r.bindings.valueAt(i);
            if (!requestServiceBindingLocked(r, ibr, execInFg, false)) {
                return;
            }
        }
    }

    private final void realStartServiceLocked(ServiceRecord r, ProcessRecord app, boolean execInFg) throws RemoteException {
        if (app.thread == null) {
            throw new RemoteException();
        }
        if (ActivityManagerDebugConfig.DEBUG_MU) {
            Slog.v(TAG_MU, "realStartServiceLocked, ServiceRecord.uid = " + r.appInfo.uid + ", ProcessRecord.uid = " + app.uid);
        }
        r.app = app;
        long jUptimeMillis = SystemClock.uptimeMillis();
        r.lastActivity = jUptimeMillis;
        r.restartTime = jUptimeMillis;
        if (this.mAm.mWallpaperClassName != null && r.name.equals(this.mAm.mWallpaperClassName)) {
            this.mAm.mWallpaperProcess = app;
        }
        boolean newService = app.services.add(r);
        bumpServiceExecutingLocked(r, execInFg, "create");
        this.mAm.updateLruProcessLocked(app, false, null);
        this.mAm.updateOomAdjLocked();
        boolean created = false;
        try {
            try {
                synchronized (r.stats.getBatteryStats()) {
                    r.stats.startLaunchedLocked();
                }
                this.mAm.notifyPackageUse(r.serviceInfo.packageName, 1);
                app.forceProcessStateUpTo(10);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(TAG_SERVICE, "AMS Creating service " + r);
                }
                app.thread.scheduleCreateService(r, r.serviceInfo, this.mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo), app.repProcState);
                r.postNotification();
                created = true;
                if (r.whitelistManager) {
                    app.whitelistManager = true;
                }
                requestServiceBindingsLocked(r, execInFg);
                updateServiceClientActivitiesLocked(app, null, true);
                if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
                    r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), null, null));
                }
                sendServiceArgsLocked(r, execInFg, true);
                if (r.delayed) {
                    if (DEBUG_DELAYED_STARTS) {
                        Slog.v(TAG_SERVICE, "REM FR DELAY LIST (new proc): " + r);
                    }
                    getServiceMap(r.userId).mDelayedStartList.remove(r);
                    r.delayed = false;
                }
                if (!r.delayedStop) {
                    return;
                }
                r.delayedStop = false;
                if (!r.startRequested) {
                    return;
                }
                if (DEBUG_DELAYED_STARTS) {
                    Slog.v(TAG_SERVICE, "Applying delayed stop (from start): " + r);
                }
                stopServiceLocked(r);
            } catch (DeadObjectException e) {
                Slog.w(TAG, "Application dead when creating service " + r);
                this.mAm.appDiedLocked(app);
                throw e;
            }
        } finally {
            if (!created) {
                boolean inDestroying = this.mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                if (newService) {
                    app.services.remove(r);
                    r.app = null;
                }
                if (!inDestroying) {
                    scheduleServiceRestartLocked(r, false);
                }
            }
        }
    }

    private final void sendServiceArgsLocked(ServiceRecord r, boolean execInFg, boolean oomAdjusted) throws TransactionTooLargeException {
        int N = r.pendingStarts.size();
        if (N == 0) {
            return;
        }
        while (r.pendingStarts.size() > 0) {
            Exception caughtException = null;
            ServiceRecord.StartItem si = null;
            try {
                si = r.pendingStarts.remove(0);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Sending arguments to: " + r + " " + r.intent + " args=" + si.intent);
                }
            } catch (TransactionTooLargeException e) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Transaction too large: intent=" + (si != null ? si.intent : "si=null"));
                }
                caughtException = e;
            } catch (RemoteException e2) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Crashed while sending args: " + r);
                }
                caughtException = e2;
            } catch (Exception e3) {
                Slog.w(TAG, "Unexpected exception", e3);
                caughtException = e3;
            }
            if (si.intent != null || N <= 1) {
                si.deliveredTime = SystemClock.uptimeMillis();
                r.deliveredStarts.add(si);
                si.deliveryCount++;
                if (si.neededGrants != null) {
                    this.mAm.grantUriPermissionUncheckedFromIntentLocked(si.neededGrants, si.getUriPermissionsLocked());
                }
                bumpServiceExecutingLocked(r, execInFg, "start");
                if (!oomAdjusted) {
                    oomAdjusted = true;
                    this.mAm.updateOomAdjLocked(r.app);
                }
                int flags = si.deliveryCount > 1 ? 2 : 0;
                if (si.doneExecutingCount > 0) {
                    flags |= 1;
                }
                r.app.thread.scheduleServiceArgs(r, si.taskRemoved, si.id, flags, si.intent);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(TAG_SERVICE, "SVC-Sent arguments: " + r + ", app=" + r.app + ", args=" + si.intent + ", flags=" + flags);
                }
                if (caughtException != null) {
                    boolean inDestroying = this.mDestroyingServices.contains(r);
                    serviceDoneExecutingLocked(r, inDestroying, inDestroying);
                    if (caughtException instanceof TransactionTooLargeException) {
                        throw ((TransactionTooLargeException) caughtException);
                    }
                    return;
                }
            }
        }
    }

    private final boolean isServiceNeeded(ServiceRecord r, boolean knowConn, boolean hasConn) {
        if (r.startRequested) {
            return true;
        }
        if (!knowConn) {
            hasConn = r.hasAutoCreateConnections();
        }
        return hasConn;
    }

    private final void bringDownServiceIfNeededLocked(ServiceRecord r, boolean knowConn, boolean hasConn) {
        if (isServiceNeeded(r, knowConn, hasConn) || this.mPendingServices.contains(r)) {
            return;
        }
        bringDownServiceLocked(r);
    }

    private final void bringDownServiceLocked(ServiceRecord r) {
        for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> c = r.connections.valueAt(conni);
            for (int i = 0; i < c.size(); i++) {
                ConnectionRecord cr = c.get(i);
                cr.serviceDead = true;
                try {
                    cr.conn.connected(r.name, (IBinder) null);
                } catch (Exception e) {
                    Slog.w(TAG, "Failure disconnecting service " + r.name + " to connection " + c.get(i).conn.asBinder() + " (in " + c.get(i).binding.client.processName + ")", e);
                }
            }
        }
        if (r.app != null && r.app.thread != null) {
            for (int i2 = r.bindings.size() - 1; i2 >= 0; i2--) {
                IntentBindRecord ibr = r.bindings.valueAt(i2);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Bringing down binding " + ibr + ": hasBound=" + ibr.hasBound);
                }
                if (ibr.hasBound) {
                    try {
                        bumpServiceExecutingLocked(r, false, "bring down unbind");
                        this.mAm.updateOomAdjLocked(r.app);
                        ibr.hasBound = false;
                        r.app.thread.scheduleUnbindService(r, ibr.intent.getIntent());
                    } catch (Exception e2) {
                        Slog.w(TAG, "Exception when unbinding service " + r.shortName, e2);
                        serviceProcessGoneLocked(r);
                    }
                }
            }
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Bringing down " + r + " " + r.intent);
        }
        r.destroyTime = SystemClock.uptimeMillis();
        ServiceMap smap = getServiceMap(r.userId);
        smap.mServicesByName.remove(r.name);
        smap.mServicesByIntent.remove(r.intent);
        r.totalRestartCount = 0;
        unscheduleServiceRestartLocked(r, 0, true);
        for (int i3 = this.mPendingServices.size() - 1; i3 >= 0; i3--) {
            if (this.mPendingServices.get(i3) == r) {
                this.mPendingServices.remove(i3);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Removed pending: " + r);
                }
            }
        }
        r.cancelNotification();
        r.isForeground = false;
        r.foregroundId = 0;
        r.foregroundNoti = null;
        r.clearDeliveredStartsLocked();
        r.pendingStarts.clear();
        if (r.app != null) {
            synchronized (r.stats.getBatteryStats()) {
                r.stats.stopLaunchedLocked();
            }
            r.app.services.remove(r);
            if (r.whitelistManager) {
                updateWhitelistManagerLocked(r.app);
            }
            if (r.app.thread != null) {
                updateServiceForegroundLocked(r.app, false);
                try {
                    bumpServiceExecutingLocked(r, false, "destroy");
                    this.mDestroyingServices.add(r);
                    r.destroying = true;
                    this.mAm.updateOomAdjLocked(r.app);
                    r.app.thread.scheduleStopService(r);
                } catch (Exception e3) {
                    Slog.w(TAG, "Exception when destroying service " + r.shortName, e3);
                    serviceProcessGoneLocked(r);
                }
            } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "Removed service that has no process: " + r);
            }
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Removed service that is not running: " + r);
        }
        if (r.bindings.size() > 0) {
            r.bindings.clear();
        }
        if (r.restarter instanceof ServiceRestarter) {
            ((ServiceRestarter) r.restarter).setService(null);
        }
        int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
        long now = SystemClock.uptimeMillis();
        if (r.tracker != null) {
            r.tracker.setStarted(false, memFactor, now);
            r.tracker.setBound(false, memFactor, now);
            if (r.executeNesting == 0) {
                r.tracker.clearCurrentOwner(r, false);
                r.tracker = null;
            }
        }
        smap.ensureNotStartingBackground(r);
    }

    void removeConnectionLocked(ConnectionRecord c, ProcessRecord skipApp, ActivityRecord skipAct) {
        IBinder binder = c.conn.asBinder();
        AppBindRecord b = c.binding;
        ServiceRecord s = b.service;
        ArrayList<ConnectionRecord> clist = s.connections.get(binder);
        if (clist != null) {
            clist.remove(c);
            if (clist.size() == 0) {
                s.connections.remove(binder);
            }
        }
        b.connections.remove(c);
        if (c.activity != null && c.activity != skipAct && c.activity.connections != null) {
            c.activity.connections.remove(c);
        }
        if (b.client != skipApp) {
            b.client.connections.remove(c);
            if ((c.flags & 8) != 0) {
                b.client.updateHasAboveClientLocked();
            }
            if ((c.flags & 16777216) != 0) {
                s.updateWhitelistManager();
                if (!s.whitelistManager && s.app != null) {
                    updateWhitelistManagerLocked(s.app);
                }
            }
            if (s.app != null) {
                updateServiceClientActivitiesLocked(s.app, c, true);
            }
        }
        ArrayList<ConnectionRecord> clist2 = this.mServiceConnections.get(binder);
        if (clist2 != null) {
            clist2.remove(c);
            if (clist2.size() == 0) {
                this.mServiceConnections.remove(binder);
            }
        }
        this.mAm.stopAssociationLocked(b.client.uid, b.client.processName, s.appInfo.uid, s.name);
        if (b.connections.size() == 0) {
            b.intent.apps.remove(b.client);
        }
        if (c.serviceDead) {
            return;
        }
        if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "Disconnecting binding " + b.intent + ": shouldUnbind=" + b.intent.hasBound);
        }
        if (s.app != null && s.app.thread != null && b.intent.apps.size() == 0 && b.intent.hasBound) {
            try {
                bumpServiceExecutingLocked(s, false, "unbind");
                if (b.client != s.app && (c.flags & 32) == 0 && s.app.setProcState <= 11) {
                    this.mAm.updateLruProcessLocked(s.app, false, null);
                }
                this.mAm.updateOomAdjLocked(s.app);
                b.intent.hasBound = false;
                b.intent.doRebind = false;
                s.app.thread.scheduleUnbindService(s, b.intent.intent.getIntent());
            } catch (Exception e) {
                Slog.w(TAG, "Exception when unbinding service " + s.shortName, e);
                serviceProcessGoneLocked(s);
            }
        }
        this.mPendingServices.remove(s);
        if ((c.flags & 1) == 0) {
            return;
        }
        boolean hasAutoCreate = s.hasAutoCreateConnections();
        if (!hasAutoCreate && s.tracker != null) {
            s.tracker.setBound(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
        }
        bringDownServiceIfNeededLocked(s, true, hasAutoCreate);
    }

    void serviceDoneExecutingLocked(ServiceRecord r, int type, int startId, int res) {
        boolean inDestroying = this.mDestroyingServices.contains(r);
        if (r != null) {
            if (type == 1) {
                r.callStart = true;
                switch (res) {
                    case 0:
                    case 1:
                        r.findDeliveredStart(startId, true);
                        r.stopIfKilled = false;
                        break;
                    case 2:
                        r.findDeliveredStart(startId, true);
                        if (r.getLastStartId() == startId) {
                            r.stopIfKilled = true;
                        }
                        break;
                    case 3:
                        ServiceRecord.StartItem si = r.findDeliveredStart(startId, false);
                        if (si != null) {
                            si.deliveryCount = 0;
                            si.doneExecutingCount++;
                            r.stopIfKilled = true;
                        }
                        break;
                    case 1000:
                        r.findDeliveredStart(startId, true);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown service start result: " + res);
                }
                if (res == 0) {
                    r.callStart = false;
                }
            } else if (type == 2) {
                if (!inDestroying) {
                    if (r.app != null) {
                        Slog.w(TAG, "Service done with onDestroy, but not inDestroying: " + r + ", app=" + r.app);
                    }
                } else if (r.executeNesting != 1) {
                    Slog.w(TAG, "Service done with onDestroy, but executeNesting=" + r.executeNesting + ": " + r);
                    r.executeNesting = 1;
                }
            }
            long origId = Binder.clearCallingIdentity();
            serviceDoneExecutingLocked(r, inDestroying, inDestroying);
            Binder.restoreCallingIdentity(origId);
            return;
        }
        Slog.w(TAG, "Done executing unknown service from pid " + Binder.getCallingPid());
    }

    private void serviceProcessGoneLocked(ServiceRecord r) {
        if (r.tracker != null) {
            int memFactor = this.mAm.mProcessStats.getMemFactorLocked();
            long now = SystemClock.uptimeMillis();
            r.tracker.setExecuting(false, memFactor, now);
            r.tracker.setBound(false, memFactor, now);
            r.tracker.setStarted(false, memFactor, now);
        }
        serviceDoneExecutingLocked(r, true, true);
    }

    private void serviceDoneExecutingLocked(ServiceRecord r, boolean inDestroying, boolean finishing) {
        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
            Slog.v(TAG_SERVICE, "<<< DONE EXECUTING " + r + ": nesting=" + r.executeNesting + ", inDestroying=" + inDestroying + ", app=" + r.app);
        } else if (ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
            Slog.v(TAG_SERVICE_EXECUTING, "<<< DONE EXECUTING " + r.shortName);
        }
        r.executeNesting--;
        if (r.executeNesting <= 0) {
            if (r.app != null) {
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Nesting at 0 of " + r.shortName);
                }
                r.app.execServicesFg = false;
                r.app.executingServices.remove(r);
                if (r.app.executingServices.size() == 0) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE || ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING) {
                        Slog.v(TAG_SERVICE_EXECUTING, "No more executingServices of " + r.shortName);
                    }
                    this.mAm.mHandler.removeMessages(12, r.app);
                    if (2 == ANRManager.enableANRDebuggingMechanism()) {
                        this.mAm.mAnrHandler.removeMessages(ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG, r.app);
                    }
                } else if (r.executeFg) {
                    int i = r.app.executingServices.size() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        if (r.app.executingServices.valueAt(i).executeFg) {
                            r.app.execServicesFg = true;
                            break;
                        }
                        i--;
                    }
                }
                if (inDestroying) {
                    if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                        Slog.v(TAG_SERVICE, "doneExecuting remove destroying " + r);
                    }
                    this.mDestroyingServices.remove(r);
                    r.bindings.clear();
                }
                this.mAm.updateOomAdjLocked(r.app);
            }
            r.executeFg = false;
            if (r.tracker != null) {
                r.tracker.setExecuting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                if (finishing) {
                    r.tracker.clearCurrentOwner(r, false);
                    r.tracker = null;
                }
            }
            if (finishing) {
                if (r.app != null && !r.app.persistent) {
                    r.app.services.remove(r);
                    if (r.whitelistManager) {
                        updateWhitelistManagerLocked(r.app);
                    }
                }
                r.app = null;
            }
            if (r.executeNesting < 0) {
                r.executeNesting = 0;
            }
        }
    }

    boolean attachApplicationLocked(ProcessRecord proc, String processName) throws RemoteException {
        boolean didSomething = false;
        if (this.mPendingServices.size() > 0) {
            ServiceRecord sr = null;
            int i = 0;
            while (i < this.mPendingServices.size()) {
                try {
                    sr = this.mPendingServices.get(i);
                    if (proc == sr.isolatedProc || (proc.uid == sr.appInfo.uid && processName.equals(sr.processName))) {
                        this.mPendingServices.remove(i);
                        i--;
                        proc.addPackage(sr.appInfo.packageName, sr.appInfo.versionCode, this.mAm.mProcessStats);
                        realStartServiceLocked(sr, proc, sr.createdFromFg);
                        didSomething = true;
                        if (!isServiceNeeded(sr, false, false)) {
                            bringDownServiceLocked(sr);
                        }
                    }
                    i++;
                } catch (RemoteException e) {
                    Slog.w(TAG, "Exception in new application when starting service " + sr.shortName, e);
                    throw e;
                }
            }
        }
        if (this.mRestartingServices.size() > 0) {
            for (int i2 = 0; i2 < this.mRestartingServices.size(); i2++) {
                ServiceRecord sr2 = this.mRestartingServices.get(i2);
                if (proc == sr2.isolatedProc || (proc.uid == sr2.appInfo.uid && processName.equals(sr2.processName))) {
                    this.mAm.mHandler.removeCallbacks(sr2.restarter);
                    this.mAm.mHandler.post(sr2.restarter);
                }
            }
        }
        return didSomething;
    }

    void processStartTimedOutLocked(ProcessRecord proc) {
        int i = 0;
        while (i < this.mPendingServices.size()) {
            ServiceRecord sr = this.mPendingServices.get(i);
            if ((proc.uid == sr.appInfo.uid && proc.processName.equals(sr.processName)) || sr.isolatedProc == proc) {
                Slog.w(TAG, "Forcing bringing down service: " + sr);
                sr.isolatedProc = null;
                this.mPendingServices.remove(i);
                i--;
                bringDownServiceLocked(sr);
            }
            i++;
        }
    }

    private boolean collectPackageServicesLocked(String packageName, Set<String> filterByClasses, boolean evenPersistent, boolean doit, boolean killProcess, ArrayMap<ComponentName, ServiceRecord> services) {
        boolean sameComponent;
        boolean didSomething = false;
        for (int i = services.size() - 1; i >= 0; i--) {
            ServiceRecord service = services.valueAt(i);
            if (packageName == null) {
                sameComponent = true;
            } else if (!service.packageName.equals(packageName)) {
                sameComponent = false;
            } else if (filterByClasses == null) {
                sameComponent = true;
            } else {
                sameComponent = filterByClasses.contains(service.name.getClassName());
            }
            if (sameComponent && (service.app == null || evenPersistent || !service.app.persistent)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                Slog.i(TAG, "  Force stopping service " + service);
                if (service.app != null) {
                    service.app.removed = killProcess;
                    if (!service.app.persistent) {
                        service.app.services.remove(service);
                        if (service.whitelistManager) {
                            updateWhitelistManagerLocked(service.app);
                        }
                    }
                }
                service.app = null;
                service.isolatedProc = null;
                if (this.mTmpCollectionResults == null) {
                    this.mTmpCollectionResults = new ArrayList<>();
                }
                this.mTmpCollectionResults.add(service);
            }
        }
        return didSomething;
    }

    boolean bringDownDisabledPackageServicesLocked(String packageName, Set<String> filterByClasses, int userId, boolean evenPersistent, boolean killProcess, boolean doit) {
        boolean didSomething = false;
        if (this.mTmpCollectionResults != null) {
            this.mTmpCollectionResults.clear();
        }
        if (userId == -1) {
            for (int i = this.mServiceMap.size() - 1; i >= 0; i--) {
                didSomething |= collectPackageServicesLocked(packageName, filterByClasses, evenPersistent, doit, killProcess, this.mServiceMap.valueAt(i).mServicesByName);
                if (!doit && didSomething) {
                    return true;
                }
            }
        } else {
            ServiceMap smap = this.mServiceMap.get(userId);
            if (smap != null) {
                ArrayMap<ComponentName, ServiceRecord> items = smap.mServicesByName;
                didSomething = collectPackageServicesLocked(packageName, filterByClasses, evenPersistent, doit, killProcess, items);
            }
        }
        if (this.mTmpCollectionResults != null) {
            for (int i2 = this.mTmpCollectionResults.size() - 1; i2 >= 0; i2--) {
                bringDownServiceLocked(this.mTmpCollectionResults.get(i2));
            }
            this.mTmpCollectionResults.clear();
        }
        return didSomething;
    }

    void cleanUpRemovedTaskLocked(TaskRecord tr, ComponentName component, Intent baseIntent) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        ArrayMap<ComponentName, ServiceRecord> alls = getServices(tr.userId);
        for (int i = alls.size() - 1; i >= 0; i--) {
            ServiceRecord sr = alls.valueAt(i);
            if (sr.packageName.equals(component.getPackageName())) {
                services.add(sr);
            }
        }
        for (int i2 = services.size() - 1; i2 >= 0; i2--) {
            ServiceRecord sr2 = services.get(i2);
            if (sr2.startRequested) {
                if ((sr2.serviceInfo.flags & 1) != 0) {
                    Slog.i(TAG, "Stopping service " + sr2.shortName + ": remove task");
                    stopServiceLocked(sr2);
                } else {
                    sr2.pendingStarts.add(new ServiceRecord.StartItem(sr2, true, sr2.makeNextStartId(), baseIntent, null));
                    if (sr2.app != null && sr2.app.thread != null) {
                        try {
                            sendServiceArgsLocked(sr2, true, false);
                        } catch (TransactionTooLargeException e) {
                        }
                    }
                }
            }
        }
    }

    final void killServicesLocked(ProcessRecord app, boolean allowRestart) {
        for (int i = app.connections.size() - 1; i >= 0; i--) {
            removeConnectionLocked(app.connections.valueAt(i), app, null);
        }
        updateServiceConnectionActivitiesLocked(app);
        app.connections.clear();
        app.whitelistManager = false;
        for (int i2 = app.services.size() - 1; i2 >= 0; i2--) {
            ServiceRecord sr = app.services.valueAt(i2);
            synchronized (sr.stats.getBatteryStats()) {
                sr.stats.stopLaunchedLocked();
            }
            if (sr.app != app && sr.app != null && !sr.app.persistent) {
                sr.app.services.remove(sr);
            }
            sr.app = null;
            sr.isolatedProc = null;
            sr.executeNesting = 0;
            sr.forceClearTracker();
            if (this.mDestroyingServices.remove(sr) && ActivityManagerDebugConfig.DEBUG_SERVICE) {
                Slog.v(TAG_SERVICE, "killServices remove destroying " + sr);
            }
            int numClients = sr.bindings.size();
            for (int bindingi = numClients - 1; bindingi >= 0; bindingi--) {
                IntentBindRecord b = sr.bindings.valueAt(bindingi);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "Killing binding " + b + ": shouldUnbind=" + b.hasBound);
                }
                b.binder = null;
                b.hasBound = false;
                b.received = false;
                b.requested = false;
                for (int appi = b.apps.size() - 1; appi >= 0; appi--) {
                    ProcessRecord proc = b.apps.keyAt(appi);
                    if (!proc.killedByAm && proc.thread != null) {
                        AppBindRecord abind = b.apps.valueAt(appi);
                        boolean hasCreate = false;
                        int conni = abind.connections.size() - 1;
                        while (true) {
                            if (conni < 0) {
                                break;
                            }
                            ConnectionRecord conn = abind.connections.valueAt(conni);
                            if ((conn.flags & 49) == 1) {
                                hasCreate = true;
                                break;
                            }
                            conni--;
                        }
                        if (!hasCreate) {
                        }
                    }
                }
            }
        }
        ServiceMap smap = getServiceMap(app.userId);
        for (int i3 = app.services.size() - 1; i3 >= 0; i3--) {
            ServiceRecord sr2 = app.services.valueAt(i3);
            if (!app.persistent) {
                app.services.removeAt(i3);
            }
            ServiceRecord curRec = smap.mServicesByName.get(sr2.name);
            if (curRec != sr2) {
                if (curRec != null) {
                    Slog.wtf(TAG, "Service " + sr2 + " in process " + app + " not same as in map: " + curRec);
                }
            } else if (allowRestart && sr2.crashCount >= 2 && (sr2.serviceInfo.applicationInfo.flags & 8) == 0) {
                Slog.w(TAG, "Service crashed " + sr2.crashCount + " times, stopping: " + sr2);
                EventLog.writeEvent(EventLogTags.AM_SERVICE_CRASHED_TOO_MUCH, Integer.valueOf(sr2.userId), Integer.valueOf(sr2.crashCount), sr2.shortName, Integer.valueOf(app.pid));
                bringDownServiceLocked(sr2);
            } else if (!allowRestart || !this.mAm.mUserController.isUserRunningLocked(sr2.userId, 0)) {
                bringDownServiceLocked(sr2);
            } else if (app != this.mAm.mWallpaperProcess || this.mAm.mIsWallpaperFg) {
                boolean canceled = scheduleServiceRestartLocked(sr2, true);
                if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.d(TAG_SERVICE, "killServicesLocked sr.startRequested: " + sr2.startRequested + " sr.stopIfKilled: " + sr2.stopIfKilled + " canceled: " + canceled);
                }
                if (sr2.startRequested && ((sr2.stopIfKilled || canceled) && sr2.pendingStarts.size() == 0)) {
                    sr2.startRequested = false;
                    if (sr2.tracker != null) {
                        sr2.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                    if (!sr2.hasAutoCreateConnections()) {
                        if (!ActivityManagerService.IS_USER_BUILD || ActivityManagerDebugConfig.DEBUG_SERVICE) {
                            Slog.d(TAG_SERVICE, "killServicesLocked no reason to restart");
                        }
                        bringDownServiceLocked(sr2);
                    }
                }
            } else {
                bringDownServiceLocked(sr2);
                this.mAm.mWallpaperProcess = null;
            }
        }
        if (!allowRestart) {
            app.services.clear();
            for (int i4 = this.mRestartingServices.size() - 1; i4 >= 0; i4--) {
                ServiceRecord r = this.mRestartingServices.get(i4);
                if (r.processName.equals(app.processName) && r.serviceInfo.applicationInfo.uid == app.info.uid) {
                    this.mRestartingServices.remove(i4);
                    clearRestartingIfNeededLocked(r);
                }
            }
            for (int i5 = this.mPendingServices.size() - 1; i5 >= 0; i5--) {
                ServiceRecord r2 = this.mPendingServices.get(i5);
                if (r2.processName.equals(app.processName) && r2.serviceInfo.applicationInfo.uid == app.info.uid) {
                    this.mPendingServices.remove(i5);
                }
            }
        }
        int i6 = this.mDestroyingServices.size();
        while (i6 > 0) {
            i6--;
            ServiceRecord sr3 = this.mDestroyingServices.get(i6);
            if (sr3.app == app) {
                sr3.forceClearTracker();
                this.mDestroyingServices.remove(i6);
                if (ActivityManagerDebugConfig.DEBUG_SERVICE) {
                    Slog.v(TAG_SERVICE, "killServices remove destroying " + sr3);
                }
            }
        }
        app.executingServices.clear();
    }

    ActivityManager.RunningServiceInfo makeRunningServiceInfoLocked(ServiceRecord r) {
        ActivityManager.RunningServiceInfo info = new ActivityManager.RunningServiceInfo();
        info.service = r.name;
        if (r.app != null) {
            info.pid = r.app.pid;
        }
        info.uid = r.appInfo.uid;
        info.process = r.processName;
        info.foreground = r.isForeground;
        info.activeSince = r.createTime;
        info.started = r.startRequested;
        info.clientCount = r.connections.size();
        info.crashCount = r.crashCount;
        info.lastActivityTime = r.lastActivity;
        if (r.isForeground) {
            info.flags |= 2;
        }
        if (r.startRequested) {
            info.flags |= 1;
        }
        if (r.app != null && r.app.pid == ActivityManagerService.MY_PID) {
            info.flags |= 4;
        }
        if (r.app != null && r.app.persistent) {
            info.flags |= 8;
        }
        for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
            ArrayList<ConnectionRecord> connl = r.connections.valueAt(conni);
            for (int i = 0; i < connl.size(); i++) {
                ConnectionRecord conn = connl.get(i);
                if (conn.clientLabel != 0) {
                    info.clientPackage = conn.binding.client.info.packageName;
                    info.clientLabel = conn.clientLabel;
                    return info;
                }
            }
        }
        return info;
    }

    List<ActivityManager.RunningServiceInfo> getRunningServiceInfoLocked(int maxNum, int flags) {
        ArrayList<ActivityManager.RunningServiceInfo> res = new ArrayList<>();
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", uid) == 0) {
                int[] users = this.mAm.mUserController.getUsers();
                for (int ui = 0; ui < users.length && res.size() < maxNum; ui++) {
                    ArrayMap<ComponentName, ServiceRecord> alls = getServices(users[ui]);
                    for (int i = 0; i < alls.size() && res.size() < maxNum; i++) {
                        ServiceRecord sr = alls.valueAt(i);
                        res.add(makeRunningServiceInfoLocked(sr));
                    }
                }
                for (int i2 = 0; i2 < this.mRestartingServices.size() && res.size() < maxNum; i2++) {
                    ServiceRecord r = this.mRestartingServices.get(i2);
                    ActivityManager.RunningServiceInfo info = makeRunningServiceInfoLocked(r);
                    info.restarting = r.nextRestartTime;
                    res.add(info);
                }
            } else {
                int userId = UserHandle.getUserId(uid);
                ArrayMap<ComponentName, ServiceRecord> alls2 = getServices(userId);
                for (int i3 = 0; i3 < alls2.size() && res.size() < maxNum; i3++) {
                    ServiceRecord sr2 = alls2.valueAt(i3);
                    res.add(makeRunningServiceInfoLocked(sr2));
                }
                for (int i4 = 0; i4 < this.mRestartingServices.size() && res.size() < maxNum; i4++) {
                    ServiceRecord r2 = this.mRestartingServices.get(i4);
                    if (r2.userId == userId) {
                        ActivityManager.RunningServiceInfo info2 = makeRunningServiceInfoLocked(r2);
                        info2.restarting = r2.nextRestartTime;
                        res.add(info2);
                    }
                }
            }
            return res;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public PendingIntent getRunningServiceControlPanelLocked(ComponentName name) {
        int userId = UserHandle.getUserId(Binder.getCallingUid());
        ServiceRecord r = getServiceByName(name, userId);
        if (r != null) {
            for (int conni = r.connections.size() - 1; conni >= 0; conni--) {
                ArrayList<ConnectionRecord> conn = r.connections.valueAt(conni);
                for (int i = 0; i < conn.size(); i++) {
                    if (conn.get(i).clientIntent != null) {
                        return conn.get(i).clientIntent;
                    }
                }
            }
        }
        return null;
    }

    void serviceTimeout(ProcessRecord proc) {
        String anrMessage = null;
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (proc.executingServices.size() == 0 || proc.thread == null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                long now = SystemClock.uptimeMillis();
                long maxTime = now - ((long) (proc.execServicesFg ? SERVICE_TIMEOUT : SERVICE_BACKGROUND_TIMEOUT));
                ServiceRecord timeout = null;
                long nextTime = 0;
                int i = proc.executingServices.size() - 1;
                while (true) {
                    if (i < 0) {
                        break;
                    }
                    ServiceRecord sr = proc.executingServices.valueAt(i);
                    if (sr.executingStart < maxTime) {
                        timeout = sr;
                        break;
                    } else {
                        if (sr.executingStart > nextTime) {
                            nextTime = sr.executingStart;
                        }
                        i--;
                    }
                }
                if (timeout == null || !this.mAm.mLruProcesses.contains(proc)) {
                    Message msg = this.mAm.mHandler.obtainMessage(12);
                    msg.obj = proc;
                    this.mAm.mHandler.sendMessageAtTime(msg, proc.execServicesFg ? 20000 + nextTime : 200000 + nextTime);
                } else {
                    Slog.w(TAG, "Timeout executing service: " + timeout);
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new FastPrintWriter(sw, false, 1024);
                    pw.println(timeout);
                    timeout.dump(pw, "    ");
                    pw.close();
                    this.mLastAnrDump = sw.toString();
                    this.mAm.mHandler.removeCallbacks(this.mLastAnrDumpClearer);
                    this.mAm.mHandler.postDelayed(this.mLastAnrDumpClearer, 7200000L);
                    anrMessage = "executing service " + timeout.shortName;
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (anrMessage != null) {
                    this.mAm.mAppErrors.appNotResponding(proc, null, null, false, anrMessage);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void scheduleServiceTimeoutLocked(ProcessRecord proc) {
        if (proc.executingServices.size() == 0 || proc.thread == null) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        Message msg = this.mAm.mHandler.obtainMessage(12);
        msg.obj = proc;
        this.mAm.mHandler.sendMessageAtTime(msg, proc.execServicesFg ? 20000 + now : 200000 + now);
        if (2 != ANRManager.enableANRDebuggingMechanism()) {
            return;
        }
        Message msg2 = this.mAm.mAnrHandler.obtainMessage(ANRManager.START_MONITOR_SERVICE_TIMEOUT_MSG, proc);
        this.mAm.mAnrHandler.sendMessageAtTime(msg2, 13333 + now);
    }

    List<ServiceRecord> collectServicesToDumpLocked(ActivityManagerService.ItemMatcher matcher, String dumpPackage) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        int[] users = this.mAm.mUserController.getUsers();
        for (int user : users) {
            ServiceMap smap = getServiceMap(user);
            if (smap.mServicesByName.size() > 0) {
                for (int si = 0; si < smap.mServicesByName.size(); si++) {
                    ServiceRecord r = smap.mServicesByName.valueAt(si);
                    if (matcher.match(r, r.name) && (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName))) {
                        services.add(r);
                    }
                }
            }
        }
        return services;
    }

    final class ServiceDumper {
        private final String[] args;
        private final boolean dumpAll;
        private final String dumpPackage;
        private final FileDescriptor fd;
        private final int opti;
        private final PrintWriter pw;
        private final ArrayList<ServiceRecord> services = new ArrayList<>();
        private final long nowReal = SystemClock.elapsedRealtime();
        private boolean needSep = false;
        private boolean printedAnything = false;
        private boolean printed = false;
        private final ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();

        ServiceDumper(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
            this.fd = fd;
            this.pw = pw;
            this.args = args;
            this.opti = opti;
            this.dumpAll = dumpAll;
            this.dumpPackage = dumpPackage;
            this.matcher.build(args, opti);
            int[] users = ActiveServices.this.mAm.mUserController.getUsers();
            for (int user : users) {
                ServiceMap smap = ActiveServices.this.getServiceMap(user);
                if (smap.mServicesByName.size() > 0) {
                    for (int si = 0; si < smap.mServicesByName.size(); si++) {
                        ServiceRecord r = smap.mServicesByName.valueAt(si);
                        if (this.matcher.match(r, r.name) && (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName))) {
                            this.services.add(r);
                        }
                    }
                }
            }
        }

        private void dumpHeaderLocked() {
            this.pw.println("ACTIVITY MANAGER SERVICES (dumpsys activity services)");
            if (ActiveServices.this.mLastAnrDump == null) {
                return;
            }
            this.pw.println("  Last ANR service:");
            this.pw.print(ActiveServices.this.mLastAnrDump);
            this.pw.println();
        }

        void dumpLocked() {
            dumpHeaderLocked();
            try {
                int[] users = ActiveServices.this.mAm.mUserController.getUsers();
                for (int user : users) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && this.services.get(serviceIdx).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            } else {
                                dumpServiceLocalLocked(r);
                            }
                        }
                        this.needSep |= this.printed;
                    }
                    dumpUserRemainsLocked(user);
                }
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            dumpRemainsLocked();
        }

        void dumpWithClient() {
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpHeaderLocked();
                } catch (Throwable th) {
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            try {
                int[] users = ActiveServices.this.mAm.mUserController.getUsers();
                for (int user : users) {
                    int serviceIdx = 0;
                    while (serviceIdx < this.services.size() && this.services.get(serviceIdx).userId != user) {
                        serviceIdx++;
                    }
                    this.printed = false;
                    if (serviceIdx < this.services.size()) {
                        this.needSep = false;
                        while (serviceIdx < this.services.size()) {
                            ServiceRecord r = this.services.get(serviceIdx);
                            serviceIdx++;
                            if (r.userId != user) {
                                break;
                            }
                            synchronized (ActiveServices.this.mAm) {
                                try {
                                    ActivityManagerService.boostPriorityForLockedSection();
                                    dumpServiceLocalLocked(r);
                                } finally {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            dumpServiceClient(r);
                        }
                        this.needSep |= this.printed;
                    }
                    synchronized (ActiveServices.this.mAm) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            dumpUserRemainsLocked(user);
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                }
            } catch (Exception e) {
                Slog.w(ActiveServices.TAG, "Exception in dumpServicesLocked", e);
            }
            synchronized (ActiveServices.this.mAm) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    dumpRemainsLocked();
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }

        private void dumpUserHeaderLocked(int user) {
            if (!this.printed) {
                if (this.printedAnything) {
                    this.pw.println();
                }
                this.pw.println("  User " + user + " active services:");
                this.printed = true;
            }
            this.printedAnything = true;
            if (!this.needSep) {
                return;
            }
            this.pw.println();
        }

        private void dumpServiceLocalLocked(ServiceRecord r) {
            dumpUserHeaderLocked(r.userId);
            this.pw.print("  * ");
            this.pw.println(r);
            if (this.dumpAll) {
                r.dump(this.pw, "    ");
                this.needSep = true;
                return;
            }
            this.pw.print("    app=");
            this.pw.println(r.app);
            this.pw.print("    created=");
            TimeUtils.formatDuration(r.createTime, this.nowReal, this.pw);
            this.pw.print(" started=");
            this.pw.print(r.startRequested);
            this.pw.print(" connections=");
            this.pw.println(r.connections.size());
            if (r.connections.size() <= 0) {
                return;
            }
            this.pw.println("    Connections:");
            for (int conni = 0; conni < r.connections.size(); conni++) {
                ArrayList<ConnectionRecord> clist = r.connections.valueAt(conni);
                for (int i = 0; i < clist.size(); i++) {
                    ConnectionRecord conn = clist.get(i);
                    this.pw.print("      ");
                    this.pw.print(conn.binding.intent.intent.getIntent().toShortString(false, false, false, false));
                    this.pw.print(" -> ");
                    ProcessRecord proc = conn.binding.client;
                    this.pw.println(proc != null ? proc.toShortString() : "null");
                }
            }
        }

        private void dumpServiceClient(ServiceRecord r) {
            IApplicationThread thread;
            ProcessRecord proc = r.app;
            if (proc == null || (thread = proc.thread) == null) {
                return;
            }
            this.pw.println("    Client:");
            this.pw.flush();
            try {
                TransferPipe tp = new TransferPipe();
                try {
                    thread.dumpService(tp.getWriteFd().getFileDescriptor(), r, this.args);
                    tp.setBufferPrefix("      ");
                    tp.go(this.fd, 2000L);
                } finally {
                    tp.kill();
                }
            } catch (RemoteException e) {
                this.pw.println("      Got a RemoteException while dumping the service");
            } catch (IOException e2) {
                this.pw.println("      Failure while dumping the service: " + e2);
            }
            this.needSep = true;
        }

        private void dumpUserRemainsLocked(int user) {
            ServiceMap smap = ActiveServices.this.getServiceMap(user);
            this.printed = false;
            int SN = smap.mDelayedStartList.size();
            for (int si = 0; si < SN; si++) {
                ServiceRecord r = smap.mDelayedStartList.get(si);
                if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + user + " delayed start services:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Delayed start ");
                    this.pw.println(r);
                }
            }
            this.printed = false;
            int SN2 = smap.mStartingBackground.size();
            for (int si2 = 0; si2 < SN2; si2++) {
                ServiceRecord r2 = smap.mStartingBackground.get(si2);
                if (this.matcher.match(r2, r2.name) && (this.dumpPackage == null || this.dumpPackage.equals(r2.appInfo.packageName))) {
                    if (!this.printed) {
                        if (this.printedAnything) {
                            this.pw.println();
                        }
                        this.pw.println("  User " + user + " starting in background:");
                        this.printed = true;
                    }
                    this.printedAnything = true;
                    this.pw.print("  * Starting bg ");
                    this.pw.println(r2);
                }
            }
        }

        private void dumpRemainsLocked() {
            if (ActiveServices.this.mPendingServices.size() > 0) {
                this.printed = false;
                for (int i = 0; i < ActiveServices.this.mPendingServices.size(); i++) {
                    ServiceRecord r = ActiveServices.this.mPendingServices.get(i);
                    if (this.matcher.match(r, r.name) && (this.dumpPackage == null || this.dumpPackage.equals(r.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Pending services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Pending ");
                        this.pw.println(r);
                        r.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (ActiveServices.this.mRestartingServices.size() > 0) {
                this.printed = false;
                for (int i2 = 0; i2 < ActiveServices.this.mRestartingServices.size(); i2++) {
                    ServiceRecord r2 = ActiveServices.this.mRestartingServices.get(i2);
                    if (this.matcher.match(r2, r2.name) && (this.dumpPackage == null || this.dumpPackage.equals(r2.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Restarting services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Restarting ");
                        this.pw.println(r2);
                        r2.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (ActiveServices.this.mDestroyingServices.size() > 0) {
                this.printed = false;
                for (int i3 = 0; i3 < ActiveServices.this.mDestroyingServices.size(); i3++) {
                    ServiceRecord r3 = ActiveServices.this.mDestroyingServices.get(i3);
                    if (this.matcher.match(r3, r3.name) && (this.dumpPackage == null || this.dumpPackage.equals(r3.appInfo.packageName))) {
                        this.printedAnything = true;
                        if (!this.printed) {
                            if (this.needSep) {
                                this.pw.println();
                            }
                            this.needSep = true;
                            this.pw.println("  Destroying services:");
                            this.printed = true;
                        }
                        this.pw.print("  * Destroy ");
                        this.pw.println(r3);
                        r3.dump(this.pw, "    ");
                    }
                }
                this.needSep = true;
            }
            if (this.dumpAll) {
                this.printed = false;
                for (int ic = 0; ic < ActiveServices.this.mServiceConnections.size(); ic++) {
                    ArrayList<ConnectionRecord> r4 = ActiveServices.this.mServiceConnections.valueAt(ic);
                    for (int i4 = 0; i4 < r4.size(); i4++) {
                        ConnectionRecord cr = r4.get(i4);
                        if (this.matcher.match(cr.binding.service, cr.binding.service.name) && (this.dumpPackage == null || (cr.binding.client != null && this.dumpPackage.equals(cr.binding.client.info.packageName)))) {
                            this.printedAnything = true;
                            if (!this.printed) {
                                if (this.needSep) {
                                    this.pw.println();
                                }
                                this.needSep = true;
                                this.pw.println("  Connection bindings to services:");
                                this.printed = true;
                            }
                            this.pw.print("  * ");
                            this.pw.println(cr);
                            cr.dump(this.pw, "    ");
                        }
                    }
                }
            }
            if (this.printedAnything) {
                return;
            }
            this.pw.println("  (nothing)");
        }
    }

    ServiceDumper newServiceDumperLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, String dumpPackage) {
        return new ServiceDumper(fd, pw, args, opti, dumpAll, dumpPackage);
    }

    protected boolean dumpService(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                int[] users = this.mAm.mUserController.getUsers();
                if ("all".equals(name)) {
                    for (int user : users) {
                        ServiceMap smap = this.mServiceMap.get(user);
                        if (smap != null) {
                            ArrayMap<ComponentName, ServiceRecord> alls = smap.mServicesByName;
                            for (int i = 0; i < alls.size(); i++) {
                                services.add(alls.valueAt(i));
                            }
                        }
                    }
                } else {
                    ComponentName componentName = name != null ? ComponentName.unflattenFromString(name) : null;
                    int objectId = 0;
                    if (componentName == null) {
                        try {
                            objectId = Integer.parseInt(name, 16);
                            name = null;
                            componentName = null;
                        } catch (RuntimeException e) {
                        }
                    }
                    for (int user2 : users) {
                        ServiceMap smap2 = this.mServiceMap.get(user2);
                        if (smap2 != null) {
                            ArrayMap<ComponentName, ServiceRecord> alls2 = smap2.mServicesByName;
                            for (int i2 = 0; i2 < alls2.size(); i2++) {
                                ServiceRecord r1 = alls2.valueAt(i2);
                                if (componentName != null) {
                                    if (r1.name.equals(componentName)) {
                                        services.add(r1);
                                    }
                                } else if (name != null) {
                                    if (r1.name.flattenToString().contains(name)) {
                                        services.add(r1);
                                    }
                                } else if (System.identityHashCode(r1) == objectId) {
                                    services.add(r1);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (services.size() <= 0) {
            return false;
        }
        boolean needSep = false;
        for (int i3 = 0; i3 < services.size(); i3++) {
            if (needSep) {
                pw.println();
            }
            needSep = true;
            dumpService("", fd, pw, services.get(i3), args, dumpAll);
        }
        return true;
    }

    private void dumpService(String prefix, FileDescriptor fd, PrintWriter pw, ServiceRecord r, String[] args, boolean dumpAll) {
        String innerPrefix = prefix + "  ";
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("SERVICE ");
                pw.print(r.shortName);
                pw.print(" ");
                pw.print(Integer.toHexString(System.identityHashCode(r)));
                pw.print(" pid=");
                if (r.app != null) {
                    pw.println(r.app.pid);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix);
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        if (r.app == null || r.app.thread == null) {
            return;
        }
        pw.print(prefix);
        pw.println("  Client:");
        pw.flush();
        try {
            TransferPipe tp = new TransferPipe();
            try {
                r.app.thread.dumpService(tp.getWriteFd().getFileDescriptor(), r, args);
                tp.setBufferPrefix(prefix + "    ");
                tp.go(fd);
            } finally {
                tp.kill();
            }
        } catch (RemoteException e) {
            pw.println(prefix + "    Got a RemoteException while dumping the service");
        } catch (IOException e2) {
            pw.println(prefix + "    Failure while dumping the service: " + e2);
        }
    }
}
