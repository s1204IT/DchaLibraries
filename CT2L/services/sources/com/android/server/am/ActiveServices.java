package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.ProcessStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.FastPrintWriter;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ServiceRecord;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ActiveServices {
    static final int BG_START_TIMEOUT = 15000;
    static final boolean DEBUG_DELAYED_SERVICE = false;
    static final boolean DEBUG_DELAYED_STARTS = false;
    static final boolean DEBUG_MU = false;
    static final boolean DEBUG_SERVICE = false;
    static final boolean DEBUG_SERVICE_EXECUTING = false;
    static final int LAST_ANR_LIFETIME_DURATION_MSECS = 7200000;
    static final boolean LOG_SERVICE_START_STOP = false;
    static final int MAX_SERVICE_INACTIVITY = 1800000;
    static final int SERVICE_BACKGROUND_TIMEOUT = 200000;
    static final int SERVICE_MIN_RESTART_TIME_BETWEEN = 10000;
    static final int SERVICE_RESET_RUN_DURATION = 60000;
    static final int SERVICE_RESTART_DURATION = 1000;
    static final int SERVICE_RESTART_DURATION_FACTOR = 4;
    static final int SERVICE_TIMEOUT = 20000;
    static final String TAG = "ActivityManager";
    static final String TAG_MU = "ActivityManagerServiceMU";
    final ActivityManagerService mAm;
    String mLastAnrDump;
    final int mMaxStartingBackground;
    final SparseArray<ServiceMap> mServiceMap = new SparseArray<>();
    final ArrayMap<IBinder, ArrayList<ConnectionRecord>> mServiceConnections = new ArrayMap<>();
    final ArrayList<ServiceRecord> mPendingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mRestartingServices = new ArrayList<>();
    final ArrayList<ServiceRecord> mDestroyingServices = new ArrayList<>();
    final Runnable mLastAnrDumpClearer = new Runnable() {
        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                ActiveServices.this.mLastAnrDump = null;
            }
        }
    };

    static final class DelayingProcess extends ArrayList<ServiceRecord> {
        long timeoout;

        DelayingProcess() {
        }
    }

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
                        rescheduleDelayedStarts();
                        break;
                    }
                    return;
                default:
                    return;
            }
        }

        void ensureNotStartingBackground(ServiceRecord r) {
            if (this.mStartingBackground.remove(r)) {
                rescheduleDelayedStarts();
            }
            if (this.mDelayedStartList.remove(r)) {
            }
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
                if (r2.pendingStarts.size() <= 0) {
                    Slog.w(ActiveServices.TAG, "**** NO PENDING STARTS! " + r2 + " startReq=" + r2.startRequested + " delayedStop=" + r2.delayedStop);
                }
                r2.delayed = false;
                ActiveServices.this.startServiceInnerLocked(this, r2.pendingStarts.get(0).intent, r2, false, true);
            }
            if (this.mStartingBackground.size() > 0) {
                ServiceRecord next = this.mStartingBackground.get(0);
                long when = next.startingBgTimeout > now ? next.startingBgTimeout : now;
                Message msg = obtainMessage(1);
                sendMessageAtTime(msg, when);
            }
            if (this.mStartingBackground.size() < ActiveServices.this.mMaxStartingBackground) {
                ActiveServices.this.mAm.backgroundServicesFinishedLocked(this.mUserId);
            }
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

    ComponentName startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, int userId) {
        boolean callerFg;
        if (caller != null) {
            ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
            if (callerApp == null) {
                throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when starting service " + service);
            }
            callerFg = callerApp.setSchedGroup != 0;
        } else {
            callerFg = true;
        }
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, callingPid, callingUid, userId, true, callerFg);
        if (res == null) {
            return null;
        }
        if (res.record == null) {
            return new ComponentName("!", res.permission != null ? res.permission : "private to package");
        }
        ServiceRecord r = res.record;
        if (!this.mAm.getUserManagerLocked().exists(r.userId)) {
            Slog.d(TAG, "Trying to start service with non-existent user! " + r.userId);
            return null;
        }
        ActivityManagerService.NeededUriGrants neededGrants = this.mAm.checkGrantUriPermissionFromIntentLocked(callingUid, r.packageName, service, service.getFlags(), null, r.userId);
        if (unscheduleServiceRestartLocked(r, callingUid, false)) {
        }
        r.lastActivity = SystemClock.uptimeMillis();
        r.startRequested = true;
        r.delayedStop = false;
        r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), service, neededGrants));
        ServiceMap smap = getServiceMap(r.userId);
        boolean addToStarting = false;
        if (!callerFg && r.app == null && this.mAm.mStartedUsers.get(r.userId) != null) {
            ProcessRecord proc = this.mAm.getProcessRecordLocked(r.processName, r.appInfo.uid, false);
            if (proc == null || proc.curProcState > 8) {
                if (r.delayed) {
                    return r.name;
                }
                if (smap.mStartingBackground.size() >= this.mMaxStartingBackground) {
                    Slog.i(TAG, "Delaying start of: " + r);
                    smap.mDelayedStartList.add(r);
                    r.delayed = true;
                    return r.name;
                }
                addToStarting = true;
            } else if (proc.curProcState >= 7) {
                addToStarting = true;
            }
        }
        return startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
    }

    ComponentName startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting) {
        ProcessStats.ServiceState stracker = r.getTracker();
        if (stracker != null) {
            stracker.setStarted(true, this.mAm.mProcessStats.getMemFactorLocked(), r.lastActivity);
        }
        r.callStart = false;
        synchronized (r.stats.getBatteryStats()) {
            r.stats.startRunningLocked();
        }
        String error = bringUpServiceLocked(r, service.getFlags(), callerFg, false);
        if (error != null) {
            return new ComponentName("!!", error);
        }
        if (r.startRequested && addToStarting) {
            boolean first = smap.mStartingBackground.size() == 0;
            smap.mStartingBackground.add(r);
            r.startingBgTimeout = SystemClock.uptimeMillis() + 15000;
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
        ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
        if (caller != null && callerApp == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when stopping service " + service);
        }
        ServiceLookupResult r = retrieveServiceLocked(service, resolvedType, Binder.getCallingPid(), Binder.getCallingUid(), userId, false, false);
        if (r != null) {
            if (r.record != null) {
                long origId = Binder.clearCallingIdentity();
                try {
                    stopServiceLocked(r.record);
                    Binder.restoreCallingIdentity(origId);
                    return 1;
                } catch (Throwable th) {
                    Binder.restoreCallingIdentity(origId);
                    throw th;
                }
            }
            return -1;
        }
        return 0;
    }

    IBinder peekServiceLocked(Intent service, String resolvedType) {
        ServiceLookupResult r = retrieveServiceLocked(service, resolvedType, Binder.getCallingPid(), Binder.getCallingUid(), UserHandle.getCallingUserId(), false, false);
        if (r == null) {
            return null;
        }
        if (r.record == null) {
            throw new SecurityException("Permission Denial: Accessing service " + r.record.name + " from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + " requires " + r.permission);
        }
        IntentBindRecord ib = r.record.bindings.get(r.record.intent);
        if (ib == null) {
            return null;
        }
        IBinder ret = ib.binder;
        return ret;
    }

    boolean stopServiceTokenLocked(ComponentName className, IBinder token, int startId) {
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

    public void setServiceForegroundLocked(ComponentName className, IBinder token, int id, Notification notification, boolean removeNotification) {
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
                } else {
                    if (r.isForeground) {
                        r.isForeground = false;
                        if (r.app != null) {
                            this.mAm.updateLruProcessLocked(r.app, false, null);
                            updateServiceForegroundLocked(r.app, true);
                        }
                    }
                    if (removeNotification) {
                        r.cancelNotification();
                        r.foregroundId = 0;
                        r.foregroundNoti = null;
                    } else if (r.appInfo.targetSdkVersion >= 21) {
                        r.stripForegroundServiceFlagFromNotification();
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
        }
        return true;
    }

    int bindServiceLocked(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, int userId) {
        ProcessStats.ServiceState stracker;
        ProcessRecord callerApp = this.mAm.getRecordForAppLocked(caller);
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
        if (callerApp.info.uid == 1000) {
            try {
                clientIntent = (PendingIntent) service.getParcelableExtra("android.intent.extra.client_intent");
            } catch (RuntimeException e) {
            }
            if (clientIntent != null && (clientLabel = service.getIntExtra("android.intent.extra.client_label", 0)) != 0) {
                service = service.cloneFilter();
            }
        }
        if ((134217728 & flags) != 0) {
            this.mAm.enforceCallingPermission("android.permission.MANAGE_ACTIVITY_STACKS", "BIND_TREAT_LIKE_ACTIVITY");
        }
        boolean callerFg = callerApp.setSchedGroup != 0;
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, Binder.getCallingPid(), Binder.getCallingUid(), userId, true, callerFg);
        if (res == null) {
            return 0;
        }
        if (res.record == null) {
            return -1;
        }
        ServiceRecord s = res.record;
        long origId = Binder.clearCallingIdentity();
        try {
            if (unscheduleServiceRestartLocked(s, callerApp.info.uid, false)) {
            }
            if ((flags & 1) != 0) {
                s.lastActivity = SystemClock.uptimeMillis();
                if (!s.hasAutoCreateConnections() && (stracker = s.getTracker()) != null) {
                    stracker.setBound(true, this.mAm.mProcessStats.getMemFactorLocked(), s.lastActivity);
                }
            }
            this.mAm.startAssociationLocked(callerApp.uid, callerApp.processName, s.appInfo.uid, s.name, s.processName);
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
                if (bringUpServiceLocked(s, service.getFlags(), callerFg, false) != null) {
                    return 0;
                }
            }
            if (s.app != null) {
                if ((134217728 & flags) != 0) {
                    s.app.treatLikeActivity = true;
                }
                this.mAm.updateLruProcessLocked(s.app, s.app.hasClientActivities || s.app.treatLikeActivity, b.client);
                this.mAm.updateOomAdjLocked(s.app);
            }
            if (s.app != null && b.intent.received) {
                try {
                    c.conn.connected(s.name, b.intent.binder);
                } catch (Exception e2) {
                    Slog.w(TAG, "Failure sending service " + s.shortName + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e2);
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

    void publishServiceLocked(ServiceRecord r, Intent intent, IBinder service) {
        long origId = Binder.clearCallingIdentity();
        if (r != null) {
            try {
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
                                try {
                                    c.conn.connected(r.name, service);
                                } catch (Exception e) {
                                    Slog.w(TAG, "Failure sending service " + r.name + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                                }
                            }
                        }
                    }
                }
                serviceDoneExecutingLocked(r, this.mDestroyingServices.contains(r), false);
            } finally {
                Binder.restoreCallingIdentity(origId);
            }
        }
    }

    boolean unbindServiceLocked(IServiceConnection connection) {
        IBinder binder = connection.asBinder();
        ArrayList<ConnectionRecord> clist = this.mServiceConnections.get(binder);
        if (clist == null) {
            Slog.w(TAG, "Unbind failed: could not find connection for " + connection.asBinder());
            return false;
        }
        long origId = Binder.clearCallingIdentity();
        while (clist.size() > 0) {
            try {
                ConnectionRecord r = clist.get(0);
                removeConnectionLocked(r, null, null);
                if (clist.size() > 0 && clist.get(0) == r) {
                    Slog.wtf(TAG, "Connection " + r + " not removed for binder " + binder);
                    clist.remove(0);
                }
                if (r.binding.service.app != null) {
                    if ((r.flags & 134217728) != 0) {
                        r.binding.service.app.treatLikeActivity = true;
                        this.mAm.updateLruProcessLocked(r.binding.service.app, r.binding.service.app.hasClientActivities || r.binding.service.app.treatLikeActivity, null);
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
                boolean inDestroying = this.mDestroyingServices.contains(r);
                if (b != null) {
                    if (b.apps.size() > 0 && !inDestroying) {
                        boolean inFg = false;
                        int i = b.apps.size() - 1;
                        while (true) {
                            if (i >= 0) {
                                ProcessRecord client = b.apps.valueAt(i).client;
                                if (client == null || client.setSchedGroup == 0) {
                                    i--;
                                } else {
                                    inFg = true;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        requestServiceBindingLocked(r, b, inFg, true);
                    } else {
                        b.doRebind = true;
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

        private ServiceRestarter() {
        }

        void setService(ServiceRecord service) {
            this.mService = service;
        }

        @Override
        public void run() {
            synchronized (ActiveServices.this.mAm) {
                ActiveServices.this.performServiceRestartLocked(this.mService);
            }
        }
    }

    private ServiceLookupResult retrieveServiceLocked(Intent service, String resolvedType, int callingPid, int callingUid, int userId, boolean createIfNeeded, boolean callingFromFg) {
        ServiceRecord r;
        BatteryStatsImpl.Uid.Pkg.Serv ss;
        int userId2 = this.mAm.handleIncomingUser(callingPid, callingUid, userId, false, 1, "service", (String) null);
        ServiceMap smap = getServiceMap(userId2);
        ComponentName comp = service.getComponent();
        if (comp == null) {
            r = null;
        } else {
            r = smap.mServicesByName.get(comp);
        }
        if (r == null) {
            ServiceRecord r2 = smap.mServicesByIntent.get(new Intent.FilterComparison(service));
            r = r2;
        }
        if (r == null) {
            try {
                ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, resolvedType, 1024, userId2);
                ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
                if (sInfo == null) {
                    Slog.w(TAG, "Unable to start service " + service + " U=" + userId2 + ": not found");
                    return null;
                }
                ComponentName name = new ComponentName(sInfo.applicationInfo.packageName, sInfo.name);
                if (userId2 > 0) {
                    if (this.mAm.isSingleton(sInfo.processName, sInfo.applicationInfo, sInfo.name, sInfo.flags) && this.mAm.isValidSingletonCall(callingUid, sInfo.applicationInfo.uid)) {
                        userId2 = 0;
                        smap = getServiceMap(0);
                    }
                    ServiceInfo sInfo2 = new ServiceInfo(sInfo);
                    sInfo2.applicationInfo = this.mAm.getAppInfoForUser(sInfo2.applicationInfo, userId2);
                    sInfo = sInfo2;
                }
                ServiceRecord r3 = smap.mServicesByName.get(name);
                if (r3 == null && createIfNeeded) {
                    try {
                        Intent.FilterComparison filter = new Intent.FilterComparison(service.cloneFilter());
                        ServiceRestarter res = new ServiceRestarter();
                        BatteryStatsImpl stats = this.mAm.mBatteryStatsService.getActiveStatistics();
                        synchronized (stats) {
                            ss = stats.getServiceStatsLocked(sInfo.applicationInfo.uid, sInfo.packageName, sInfo.name);
                        }
                        r = new ServiceRecord(this.mAm, ss, name, filter, sInfo, callingFromFg, res);
                        res.setService(r);
                        smap.mServicesByName.put(name, r);
                        smap.mServicesByIntent.put(filter, r);
                        for (int i = this.mPendingServices.size() - 1; i >= 0; i--) {
                            ServiceRecord pr = this.mPendingServices.get(i);
                            if (pr.serviceInfo.applicationInfo.uid == sInfo.applicationInfo.uid && pr.name.equals(name)) {
                                this.mPendingServices.remove(i);
                            }
                        }
                    } catch (RemoteException e) {
                        r = r3;
                    }
                } else {
                    r = r3;
                }
            } catch (RemoteException e2) {
            }
        }
        if (r != null) {
            if (this.mAm.checkComponentPermission(r.permission, callingPid, callingUid, r.appInfo.uid, r.exported) != 0) {
                if (!r.exported) {
                    Slog.w(TAG, "Permission Denial: Accessing service " + r.name + " from pid=" + callingPid + ", uid=" + callingUid + " that is not exported from uid " + r.appInfo.uid);
                    return new ServiceLookupResult(null, "not exported from uid " + r.appInfo.uid);
                }
                Slog.w(TAG, "Permission Denial: Accessing service " + r.name + " from pid=" + callingPid + ", uid=" + callingUid + " requires " + r.permission);
                return new ServiceLookupResult(null, r.permission);
            }
            if (!this.mAm.mIntentFirewall.checkService(r.name, service, callingUid, callingPid, resolvedType, r.appInfo)) {
                return null;
            }
            return new ServiceLookupResult(r, null);
        }
        return null;
    }

    private final void bumpServiceExecutingLocked(ServiceRecord r, boolean fg, String why) {
        long now = SystemClock.uptimeMillis();
        if (r.executeNesting == 0) {
            r.executeFg = fg;
            ProcessStats.ServiceState stracker = r.getTracker();
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

    private final boolean requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i, boolean execInFg, boolean rebind) {
        if (r.app == null || r.app.thread == null) {
            return false;
        }
        if ((!i.requested || rebind) && i.apps.size() > 0) {
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind");
                r.app.forceProcessStateUpTo(7);
                r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind, r.app.repProcState);
                if (!rebind) {
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (RemoteException e) {
                return false;
            }
        }
        return true;
    }

    private final boolean scheduleServiceRestartLocked(ServiceRecord r, boolean allowCancel) {
        boolean repeat;
        boolean canceled = false;
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
                    if (r2 != r && r.nextRestartTime >= r2.nextRestartTime - 10000 && r.nextRestartTime < r2.nextRestartTime + 10000) {
                        r.nextRestartTime = r2.nextRestartTime + 10000;
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
        if (this.mRestartingServices.contains(r)) {
            bringUpServiceLocked(r, r.intent.getIntent().getFlags(), r.createdFromFg, true);
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
        if (r.restartTracker != null) {
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
            if (!stillTracking) {
                r.restartTracker.setRestarting(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                r.restartTracker = null;
            }
        }
    }

    private final String bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting) {
        ProcessRecord app;
        if (r.app != null && r.app.thread != null) {
            sendServiceArgsLocked(r, execInFg, false);
            return null;
        }
        if (!whileRestarting && r.restartDelay > 0) {
            return null;
        }
        if (this.mRestartingServices.remove(r)) {
            clearRestartingIfNeededLocked(r);
        }
        if (r.delayed) {
            getServiceMap(r.userId).mDelayedStartList.remove(r);
            r.delayed = false;
        }
        if (this.mAm.mStartedUsers.get(r.userId) == null) {
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
        if (!isolated) {
            app = this.mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);
            if (app != null && app.thread != null) {
                try {
                    app.addPackage(r.appInfo.packageName, r.appInfo.versionCode, this.mAm.mProcessStats);
                    realStartServiceLocked(r, app, execInFg);
                    return null;
                } catch (RemoteException e3) {
                    Slog.w(TAG, "Exception when starting service " + r.shortName, e3);
                }
            }
        } else {
            app = r.isolatedProc;
        }
        if (app == null) {
            ProcessRecord app2 = this.mAm.startProcessLocked(procName, r.appInfo, true, intentFlags, "service", r.name, false, isolated, false);
            if (app2 == null) {
                String msg2 = "Unable to launch app " + r.appInfo.packageName + "/" + r.appInfo.uid + " for service " + r.intent.getIntent() + ": process is bad";
                Slog.w(TAG, msg2);
                bringDownServiceLocked(r);
                return msg2;
            }
            if (isolated) {
                r.isolatedProc = app2;
            }
        }
        if (!this.mPendingServices.contains(r)) {
            this.mPendingServices.add(r);
        }
        if (r.delayedStop) {
            r.delayedStop = false;
            if (r.startRequested) {
                stopServiceLocked(r);
            }
        }
        return null;
    }

    private final void requestServiceBindingsLocked(ServiceRecord r, boolean execInFg) {
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
        r.app = app;
        long jUptimeMillis = SystemClock.uptimeMillis();
        r.lastActivity = jUptimeMillis;
        r.restartTime = jUptimeMillis;
        app.services.add(r);
        bumpServiceExecutingLocked(r, execInFg, "create");
        this.mAm.updateLruProcessLocked(app, false, null);
        this.mAm.updateOomAdjLocked();
        try {
            try {
                synchronized (r.stats.getBatteryStats()) {
                    r.stats.startLaunchedLocked();
                }
                this.mAm.ensurePackageDexOpt(r.serviceInfo.packageName);
                app.forceProcessStateUpTo(7);
                app.thread.scheduleCreateService(r, r.serviceInfo, this.mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo), app.repProcState);
                r.postNotification();
                if (1 == 0) {
                    app.services.remove(r);
                    r.app = null;
                    scheduleServiceRestartLocked(r, false);
                    return;
                }
            } catch (DeadObjectException e) {
                Slog.w(TAG, "Application dead when creating service " + r);
                this.mAm.appDiedLocked(app);
                if (0 == 0) {
                    app.services.remove(r);
                    r.app = null;
                    scheduleServiceRestartLocked(r, false);
                    return;
                }
            }
            requestServiceBindingsLocked(r, execInFg);
            updateServiceClientActivitiesLocked(app, null, true);
            if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
                r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), null, null));
            }
            sendServiceArgsLocked(r, execInFg, true);
            if (r.delayed) {
                getServiceMap(r.userId).mDelayedStartList.remove(r);
                r.delayed = false;
            }
            if (r.delayedStop) {
                r.delayedStop = false;
                if (r.startRequested) {
                    stopServiceLocked(r);
                }
            }
        } catch (Throwable th) {
            if (0 == 0) {
                app.services.remove(r);
                r.app = null;
                scheduleServiceRestartLocked(r, false);
                return;
            }
            throw th;
        }
    }

    private final void sendServiceArgsLocked(ServiceRecord r, boolean execInFg, boolean oomAdjusted) {
        int N = r.pendingStarts.size();
        if (N != 0) {
            while (r.pendingStarts.size() > 0) {
                try {
                    ServiceRecord.StartItem si = r.pendingStarts.remove(0);
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
                        int flags = 0;
                        if (si.deliveryCount > 1) {
                            flags = 0 | 2;
                        }
                        if (si.doneExecutingCount > 0) {
                            flags |= 1;
                        }
                        r.app.thread.scheduleServiceArgs(r, si.taskRemoved, si.id, flags, si.intent);
                    }
                } catch (RemoteException e) {
                    return;
                } catch (Exception e2) {
                    Slog.w(TAG, "Unexpected exception", e2);
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
        if (!isServiceNeeded(r, knowConn, hasConn) && !this.mPendingServices.contains(r)) {
            bringDownServiceLocked(r);
        }
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
        r.destroyTime = SystemClock.uptimeMillis();
        ServiceMap smap = getServiceMap(r.userId);
        smap.mServicesByName.remove(r.name);
        smap.mServicesByIntent.remove(r.intent);
        r.totalRestartCount = 0;
        unscheduleServiceRestartLocked(r, 0, true);
        for (int i3 = this.mPendingServices.size() - 1; i3 >= 0; i3--) {
            if (this.mPendingServices.get(i3) == r) {
                this.mPendingServices.remove(i3);
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
            }
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
        if (!c.serviceDead) {
            if (s.app != null && s.app.thread != null && b.intent.apps.size() == 0 && b.intent.hasBound) {
                try {
                    bumpServiceExecutingLocked(s, false, "unbind");
                    if (b.client != s.app && (c.flags & 32) == 0 && s.app.setProcState <= 8) {
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
            if ((c.flags & 1) != 0) {
                boolean hasAutoCreate = s.hasAutoCreateConnections();
                if (!hasAutoCreate && s.tracker != null) {
                    s.tracker.setBound(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                }
                bringDownServiceIfNeededLocked(s, true, hasAutoCreate);
            }
        }
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
                    Slog.wtfStack(TAG, "Service done with onDestroy, but not inDestroying: " + r);
                } else if (r.executeNesting != 1) {
                    Slog.wtfStack(TAG, "Service done with onDestroy, but executeNesting=" + r.executeNesting + ": " + r);
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
        r.executeNesting--;
        if (r.executeNesting <= 0) {
            if (r.app != null) {
                r.app.execServicesFg = false;
                r.app.executingServices.remove(r);
                if (r.app.executingServices.size() == 0) {
                    this.mAm.mHandler.removeMessages(12, r.app);
                } else if (r.executeFg) {
                    int i = r.app.executingServices.size() - 1;
                    while (true) {
                        if (i < 0) {
                            break;
                        }
                        if (!r.app.executingServices.valueAt(i).executeFg) {
                            i--;
                        } else {
                            r.app.execServicesFg = true;
                            break;
                        }
                    }
                }
                if (inDestroying) {
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
                }
                r.app = null;
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

    private boolean collectForceStopServicesLocked(String name, int userId, boolean evenPersistent, boolean doit, ArrayMap<ComponentName, ServiceRecord> services, ArrayList<ServiceRecord> result) {
        boolean didSomething = false;
        for (int i = 0; i < services.size(); i++) {
            ServiceRecord service = services.valueAt(i);
            if ((name == null || service.packageName.equals(name)) && (service.app == null || evenPersistent || !service.app.persistent)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                Slog.i(TAG, "  Force stopping service " + service);
                if (service.app != null) {
                    service.app.removed = true;
                    if (!service.app.persistent) {
                        service.app.services.remove(service);
                    }
                }
                service.app = null;
                service.isolatedProc = null;
                result.add(service);
            }
        }
        return didSomething;
    }

    boolean forceStopLocked(String name, int userId, boolean evenPersistent, boolean doit) {
        boolean didSomething = false;
        ArrayList<ServiceRecord> services = new ArrayList<>();
        if (userId == -1) {
            for (int i = 0; i < this.mServiceMap.size(); i++) {
                didSomething |= collectForceStopServicesLocked(name, userId, evenPersistent, doit, this.mServiceMap.valueAt(i).mServicesByName, services);
                if (!doit && didSomething) {
                    return true;
                }
            }
        } else {
            ServiceMap smap = this.mServiceMap.get(userId);
            if (smap != null) {
                ArrayMap<ComponentName, ServiceRecord> items = smap.mServicesByName;
                didSomething = collectForceStopServicesLocked(name, userId, evenPersistent, doit, items, services);
            }
        }
        int N = services.size();
        for (int i2 = 0; i2 < N; i2++) {
            bringDownServiceLocked(services.get(i2));
        }
        return didSomething;
    }

    void cleanUpRemovedTaskLocked(TaskRecord tr, ComponentName component, Intent baseIntent) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        ArrayMap<ComponentName, ServiceRecord> alls = getServices(tr.userId);
        for (int i = 0; i < alls.size(); i++) {
            ServiceRecord sr = alls.valueAt(i);
            if (sr.packageName.equals(component.getPackageName())) {
                services.add(sr);
            }
        }
        for (int i2 = 0; i2 < services.size(); i2++) {
            ServiceRecord sr2 = services.get(i2);
            if (sr2.startRequested) {
                if ((sr2.serviceInfo.flags & 1) != 0) {
                    Slog.i(TAG, "Stopping service " + sr2.shortName + ": remove task");
                    stopServiceLocked(sr2);
                } else {
                    sr2.pendingStarts.add(new ServiceRecord.StartItem(sr2, true, sr2.makeNextStartId(), baseIntent, null));
                    if (sr2.app != null && sr2.app.thread != null) {
                        sendServiceArgsLocked(sr2, true, false);
                    }
                }
            }
        }
    }

    final void killServicesLocked(ProcessRecord app, boolean allowRestart) {
        for (int i = app.services.size() - 1; i >= 0; i--) {
            ServiceRecord sr = app.services.valueAt(i);
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
            if (this.mDestroyingServices.remove(sr)) {
            }
            int numClients = sr.bindings.size();
            for (int bindingi = numClients - 1; bindingi >= 0; bindingi--) {
                IntentBindRecord b = sr.bindings.valueAt(bindingi);
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
        for (int i2 = app.connections.size() - 1; i2 >= 0; i2--) {
            removeConnectionLocked(app.connections.valueAt(i2), app, null);
        }
        updateServiceConnectionActivitiesLocked(app);
        app.connections.clear();
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
            } else if (!allowRestart) {
                bringDownServiceLocked(sr2);
            } else {
                boolean canceled = scheduleServiceRestartLocked(sr2, true);
                if (sr2.startRequested && ((sr2.stopIfKilled || canceled) && sr2.pendingStarts.size() == 0)) {
                    sr2.startRequested = false;
                    if (sr2.tracker != null) {
                        sr2.tracker.setStarted(false, this.mAm.mProcessStats.getMemFactorLocked(), SystemClock.uptimeMillis());
                    }
                    if (!sr2.hasAutoCreateConnections()) {
                        bringDownServiceLocked(sr2);
                    }
                }
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
        int conni = r.connections.size() - 1;
        loop0: while (true) {
            if (conni < 0) {
                break;
            }
            ArrayList<ConnectionRecord> connl = r.connections.valueAt(conni);
            for (int i = 0; i < connl.size(); i++) {
                ConnectionRecord conn = connl.get(i);
                if (conn.clientLabel != 0) {
                    info.clientPackage = conn.binding.client.info.packageName;
                    info.clientLabel = conn.clientLabel;
                    break loop0;
                }
            }
            conni--;
        }
        return info;
    }

    List<ActivityManager.RunningServiceInfo> getRunningServiceInfoLocked(int maxNum, int flags) {
        ArrayList<ActivityManager.RunningServiceInfo> res = new ArrayList<>();
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (ActivityManager.checkUidPermission("android.permission.INTERACT_ACROSS_USERS_FULL", uid) == 0) {
                int[] users = this.mAm.getUsersLocked();
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
            if (proc.executingServices.size() != 0 && proc.thread != null) {
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
                    if (sr.executingStart >= maxTime) {
                        if (sr.executingStart > nextTime) {
                            nextTime = sr.executingStart;
                        }
                        i--;
                    } else {
                        timeout = sr;
                        break;
                    }
                }
                if (timeout != null && this.mAm.mLruProcesses.contains(proc)) {
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
                } else {
                    Message msg = this.mAm.mHandler.obtainMessage(12);
                    msg.obj = proc;
                    this.mAm.mHandler.sendMessageAtTime(msg, proc.execServicesFg ? 20000 + nextTime : 200000 + nextTime);
                }
                if (anrMessage != null) {
                    this.mAm.appNotResponding(proc, null, null, false, anrMessage);
                }
            }
        }
    }

    void scheduleServiceTimeoutLocked(ProcessRecord proc) {
        if (proc.executingServices.size() != 0 && proc.thread != null) {
            long now = SystemClock.uptimeMillis();
            Message msg = this.mAm.mHandler.obtainMessage(12);
            msg.obj = proc;
            this.mAm.mHandler.sendMessageAtTime(msg, proc.execServicesFg ? 20000 + now : 200000 + now);
        }
    }

    void dumpServicesLocked(FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        TransferPipe tp;
        boolean needSep = false;
        boolean printedAnything = false;
        ActivityManagerService.ItemMatcher matcher = new ActivityManagerService.ItemMatcher();
        matcher.build(args, opti);
        pw.println("ACTIVITY MANAGER SERVICES (dumpsys activity services)");
        try {
            if (this.mLastAnrDump != null) {
                pw.println("  Last ANR service:");
                pw.print(this.mLastAnrDump);
                pw.println();
            }
            int[] users = this.mAm.getUsersLocked();
            for (int user : users) {
                ServiceMap smap = getServiceMap(user);
                boolean printed = false;
                if (smap.mServicesByName.size() > 0) {
                    long nowReal = SystemClock.elapsedRealtime();
                    needSep = false;
                    for (int si = 0; si < smap.mServicesByName.size(); si++) {
                        ServiceRecord r = smap.mServicesByName.valueAt(si);
                        if (matcher.match(r, r.name) && (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName))) {
                            if (!printed) {
                                if (printedAnything) {
                                    pw.println();
                                }
                                pw.println("  User " + user + " active services:");
                                printed = true;
                            }
                            printedAnything = true;
                            if (needSep) {
                                pw.println();
                            }
                            pw.print("  * ");
                            pw.println(r);
                            if (dumpAll) {
                                r.dump(pw, "    ");
                                needSep = true;
                            } else {
                                pw.print("    app=");
                                pw.println(r.app);
                                pw.print("    created=");
                                TimeUtils.formatDuration(r.createTime, nowReal, pw);
                                pw.print(" started=");
                                pw.print(r.startRequested);
                                pw.print(" connections=");
                                pw.println(r.connections.size());
                                if (r.connections.size() > 0) {
                                    pw.println("    Connections:");
                                    for (int conni = 0; conni < r.connections.size(); conni++) {
                                        ArrayList<ConnectionRecord> clist = r.connections.valueAt(conni);
                                        for (int i = 0; i < clist.size(); i++) {
                                            ConnectionRecord conn = clist.get(i);
                                            pw.print("      ");
                                            pw.print(conn.binding.intent.intent.getIntent().toShortString(false, false, false, false));
                                            pw.print(" -> ");
                                            ProcessRecord proc = conn.binding.client;
                                            pw.println(proc != null ? proc.toShortString() : "null");
                                        }
                                    }
                                }
                            }
                            if (dumpClient && r.app != null && r.app.thread != null) {
                                pw.println("    Client:");
                                pw.flush();
                                try {
                                    try {
                                        tp = new TransferPipe();
                                    } catch (RemoteException e) {
                                        pw.println("      Got a RemoteException while dumping the service");
                                    }
                                } catch (IOException e2) {
                                    pw.println("      Failure while dumping the service: " + e2);
                                }
                                try {
                                    r.app.thread.dumpService(tp.getWriteFd().getFileDescriptor(), r, args);
                                    tp.setBufferPrefix("      ");
                                    tp.go(fd, 2000L);
                                    tp.kill();
                                    needSep = true;
                                } catch (Throwable th) {
                                    tp.kill();
                                    throw th;
                                }
                            }
                        }
                    }
                    needSep |= printed;
                }
                boolean printed2 = false;
                int SN = smap.mDelayedStartList.size();
                for (int si2 = 0; si2 < SN; si2++) {
                    ServiceRecord r2 = smap.mDelayedStartList.get(si2);
                    if (matcher.match(r2, r2.name) && (dumpPackage == null || dumpPackage.equals(r2.appInfo.packageName))) {
                        if (!printed2) {
                            if (printedAnything) {
                                pw.println();
                            }
                            pw.println("  User " + user + " delayed start services:");
                            printed2 = true;
                        }
                        printedAnything = true;
                        pw.print("  * Delayed start ");
                        pw.println(r2);
                    }
                }
                boolean printed3 = false;
                int SN2 = smap.mStartingBackground.size();
                for (int si3 = 0; si3 < SN2; si3++) {
                    ServiceRecord r3 = smap.mStartingBackground.get(si3);
                    if (matcher.match(r3, r3.name) && (dumpPackage == null || dumpPackage.equals(r3.appInfo.packageName))) {
                        if (!printed3) {
                            if (printedAnything) {
                                pw.println();
                            }
                            pw.println("  User " + user + " starting in background:");
                            printed3 = true;
                        }
                        printedAnything = true;
                        pw.print("  * Starting bg ");
                        pw.println(r3);
                    }
                }
            }
        } catch (Exception e3) {
            Slog.w(TAG, "Exception in dumpServicesLocked", e3);
        }
        if (this.mPendingServices.size() > 0) {
            boolean printed4 = false;
            for (int i2 = 0; i2 < this.mPendingServices.size(); i2++) {
                ServiceRecord r4 = this.mPendingServices.get(i2);
                if (matcher.match(r4, r4.name) && (dumpPackage == null || dumpPackage.equals(r4.appInfo.packageName))) {
                    printedAnything = true;
                    if (!printed4) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Pending services:");
                        printed4 = true;
                    }
                    pw.print("  * Pending ");
                    pw.println(r4);
                    r4.dump(pw, "    ");
                }
            }
            needSep = true;
        }
        if (this.mRestartingServices.size() > 0) {
            boolean printed5 = false;
            for (int i3 = 0; i3 < this.mRestartingServices.size(); i3++) {
                ServiceRecord r5 = this.mRestartingServices.get(i3);
                if (matcher.match(r5, r5.name) && (dumpPackage == null || dumpPackage.equals(r5.appInfo.packageName))) {
                    printedAnything = true;
                    if (!printed5) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Restarting services:");
                        printed5 = true;
                    }
                    pw.print("  * Restarting ");
                    pw.println(r5);
                    r5.dump(pw, "    ");
                }
            }
            needSep = true;
        }
        if (this.mDestroyingServices.size() > 0) {
            boolean printed6 = false;
            for (int i4 = 0; i4 < this.mDestroyingServices.size(); i4++) {
                ServiceRecord r6 = this.mDestroyingServices.get(i4);
                if (matcher.match(r6, r6.name) && (dumpPackage == null || dumpPackage.equals(r6.appInfo.packageName))) {
                    printedAnything = true;
                    if (!printed6) {
                        if (needSep) {
                            pw.println();
                        }
                        needSep = true;
                        pw.println("  Destroying services:");
                        printed6 = true;
                    }
                    pw.print("  * Destroy ");
                    pw.println(r6);
                    r6.dump(pw, "    ");
                }
            }
            needSep = true;
        }
        if (dumpAll) {
            boolean printed7 = false;
            for (int ic = 0; ic < this.mServiceConnections.size(); ic++) {
                ArrayList<ConnectionRecord> r7 = this.mServiceConnections.valueAt(ic);
                for (int i5 = 0; i5 < r7.size(); i5++) {
                    ConnectionRecord cr = r7.get(i5);
                    if (matcher.match(cr.binding.service, cr.binding.service.name) && (dumpPackage == null || (cr.binding.client != null && dumpPackage.equals(cr.binding.client.info.packageName)))) {
                        printedAnything = true;
                        if (!printed7) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Connection bindings to services:");
                            printed7 = true;
                        }
                        pw.print("  * ");
                        pw.println(cr);
                        cr.dump(pw, "    ");
                    }
                }
            }
        }
        if (!printedAnything) {
            pw.println("  (nothing)");
        }
    }

    protected boolean dumpService(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<ServiceRecord> services = new ArrayList<>();
        synchronized (this.mAm) {
            int[] users = this.mAm.getUsersLocked();
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
        }
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
        }
        if (r.app != null && r.app.thread != null) {
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
}
