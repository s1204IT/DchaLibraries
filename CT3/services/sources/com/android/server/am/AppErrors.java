package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.BenesseExtension;
import android.os.Binder;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import com.android.internal.app.ProcessMap;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.Watchdog;
import com.android.server.am.AppErrorDialog;
import com.mediatek.am.AMEventHookAction;
import com.mediatek.am.AMEventHookData;
import com.mediatek.am.AMEventHookResult;
import com.mediatek.anrmanager.ANRManager;
import com.mediatek.anrmanager.ANRManager.AnrDumpRecord;
import com.mediatek.anrmanager.ANRManager.BinderDumpThread;
import com.mediatek.server.am.AMEventHook;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class AppErrors {
    static final String BC_LOG_DIR = "/factory/anrlog";
    static final String BC_LOG_LIST_FILE = "/factory/anrlog/anr.log";
    static final int BC_MAX_LOG_FILES = 40;
    static final int BC_MAX_LOG_LINES = 200;
    static final String FACTORY_DIR = "/factory/";
    private static final String TAG = "ActivityManager";
    static SimpleDateFormat sAnrFileDateFormat;
    private ArraySet<String> mAppsNotReportingCrashes;
    private final Context mContext;
    private final ActivityManagerService mService;
    private final ProcessMap<Long> mProcessCrashTimes = new ProcessMap<>();
    private final ProcessMap<Long> mProcessCrashTimesPersistent = new ProcessMap<>();
    private final ProcessMap<BadProcessInfo> mBadProcesses = new ProcessMap<>();
    private AMEventHook mAMEventHook = AMEventHook.createInstance();

    AppErrors(Context context, ActivityManagerService service) {
        this.mService = service;
        this.mContext = context;
    }

    boolean dumpLocked(FileDescriptor fd, PrintWriter pw, boolean needSep, String dumpPackage) {
        if (!this.mProcessCrashTimes.getMap().isEmpty()) {
            boolean printed = false;
            long now = SystemClock.uptimeMillis();
            ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
            int processCount = pmap.size();
            for (int ip = 0; ip < processCount; ip++) {
                String pname = pmap.keyAt(ip);
                SparseArray<Long> uids = pmap.valueAt(ip);
                int uidCount = uids.size();
                for (int i = 0; i < uidCount; i++) {
                    int puid = uids.keyAt(i);
                    ProcessRecord r = (ProcessRecord) this.mService.mProcessNames.get(pname, puid);
                    if (dumpPackage == null || (r != null && r.pkgList.containsKey(dumpPackage))) {
                        if (!printed) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Time since processes crashed:");
                            printed = true;
                        }
                        pw.print("    Process ");
                        pw.print(pname);
                        pw.print(" uid ");
                        pw.print(puid);
                        pw.print(": last crashed ");
                        TimeUtils.formatDuration(now - uids.valueAt(i).longValue(), pw);
                        pw.println(" ago");
                    }
                }
            }
        }
        if (!this.mBadProcesses.getMap().isEmpty()) {
            boolean printed2 = false;
            ArrayMap<String, SparseArray<BadProcessInfo>> pmap2 = this.mBadProcesses.getMap();
            int processCount2 = pmap2.size();
            for (int ip2 = 0; ip2 < processCount2; ip2++) {
                String pname2 = pmap2.keyAt(ip2);
                SparseArray<BadProcessInfo> uids2 = pmap2.valueAt(ip2);
                int uidCount2 = uids2.size();
                for (int i2 = 0; i2 < uidCount2; i2++) {
                    int puid2 = uids2.keyAt(i2);
                    ProcessRecord r2 = (ProcessRecord) this.mService.mProcessNames.get(pname2, puid2);
                    if (dumpPackage == null || (r2 != null && r2.pkgList.containsKey(dumpPackage))) {
                        if (!printed2) {
                            if (needSep) {
                                pw.println();
                            }
                            needSep = true;
                            pw.println("  Bad processes:");
                            printed2 = true;
                        }
                        BadProcessInfo info = uids2.valueAt(i2);
                        pw.print("    Bad process ");
                        pw.print(pname2);
                        pw.print(" uid ");
                        pw.print(puid2);
                        pw.print(": crashed at time ");
                        pw.println(info.time);
                        if (info.shortMsg != null) {
                            pw.print("      Short msg: ");
                            pw.println(info.shortMsg);
                        }
                        if (info.longMsg != null) {
                            pw.print("      Long msg: ");
                            pw.println(info.longMsg);
                        }
                        if (info.stack != null) {
                            pw.println("      Stack:");
                            int lastPos = 0;
                            for (int pos = 0; pos < info.stack.length(); pos++) {
                                if (info.stack.charAt(pos) == '\n') {
                                    pw.print("        ");
                                    pw.write(info.stack, lastPos, pos - lastPos);
                                    pw.println();
                                    lastPos = pos + 1;
                                }
                            }
                            if (lastPos < info.stack.length()) {
                                pw.print("        ");
                                pw.write(info.stack, lastPos, info.stack.length() - lastPos);
                                pw.println();
                            }
                        }
                    }
                }
            }
        }
        return needSep;
    }

    boolean isBadProcessLocked(ApplicationInfo info) {
        return this.mBadProcesses.get(info.processName, info.uid) != null;
    }

    void clearBadProcessLocked(ApplicationInfo info) {
        this.mBadProcesses.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(ApplicationInfo info) {
        this.mProcessCrashTimes.remove(info.processName, info.uid);
    }

    void resetProcessCrashTimeLocked(boolean resetEntireUser, int appId, int userId) {
        ArrayMap<String, SparseArray<Long>> pmap = this.mProcessCrashTimes.getMap();
        for (int ip = pmap.size() - 1; ip >= 0; ip--) {
            SparseArray<Long> ba = pmap.valueAt(ip);
            for (int i = ba.size() - 1; i >= 0; i--) {
                boolean remove = false;
                int entUid = ba.keyAt(i);
                if (!resetEntireUser) {
                    if (userId == -1) {
                        if (UserHandle.getAppId(entUid) == appId) {
                            remove = true;
                        }
                    } else if (entUid == UserHandle.getUid(userId, appId)) {
                        remove = true;
                    }
                } else if (UserHandle.getUserId(entUid) == userId) {
                    remove = true;
                }
                if (remove) {
                    ba.removeAt(i);
                }
            }
            if (ba.size() == 0) {
                pmap.removeAt(ip);
            }
        }
    }

    void loadAppsNotReportingCrashesFromConfigLocked(String appsNotReportingCrashesConfig) {
        if (appsNotReportingCrashesConfig == null) {
            return;
        }
        String[] split = appsNotReportingCrashesConfig.split(",");
        if (split.length <= 0) {
            return;
        }
        this.mAppsNotReportingCrashes = new ArraySet<>();
        Collections.addAll(this.mAppsNotReportingCrashes, split);
    }

    void killAppAtUserRequestLocked(ProcessRecord app, Dialog fromDialog) {
        app.crashing = false;
        app.crashingReport = null;
        app.notResponding = false;
        app.notRespondingReport = null;
        if (app.anrDialog == fromDialog) {
            app.anrDialog = null;
        }
        if (app.waitDialog == fromDialog) {
            app.waitDialog = null;
        }
        if (app.pid <= 0 || app.pid == ActivityManagerService.MY_PID) {
            return;
        }
        handleAppCrashLocked(app, "user-terminated", null, null, null, null);
        app.kill("user request after error", true);
    }

    void scheduleAppCrashLocked(int uid, int initialPid, String packageName, String message) {
        ProcessRecord proc = null;
        synchronized (this.mService.mPidsSelfLocked) {
            int i = 0;
            while (true) {
                if (i >= this.mService.mPidsSelfLocked.size()) {
                    break;
                }
                ProcessRecord p = this.mService.mPidsSelfLocked.valueAt(i);
                if (p.uid == uid) {
                    if (p.pid == initialPid) {
                        proc = p;
                        break;
                    } else if (p.pkgList.containsKey(packageName)) {
                        proc = p;
                    }
                }
                i++;
            }
        }
        if (proc == null) {
            Slog.w(TAG, "crashApplication: nothing for uid=" + uid + " initialPid=" + initialPid + " packageName=" + packageName);
        } else {
            proc.scheduleCrash(message);
        }
    }

    void crashApplication(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo) {
        long origId = Binder.clearCallingIdentity();
        try {
            crashApplicationInner(r, crashInfo);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    void crashApplicationInner(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo) {
        Intent appErrorIntent;
        long timeMillis = System.currentTimeMillis();
        String shortMsg = crashInfo.exceptionClassName;
        String longMsg = crashInfo.exceptionMessage;
        String stackTrace = crashInfo.stackTrace;
        if (shortMsg != null && longMsg != null) {
            longMsg = shortMsg + ": " + longMsg;
        } else if (shortMsg != null) {
            longMsg = shortMsg;
        }
        AppErrorResult result = new AppErrorResult();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (handleAppCrashInActivityController(r, crashInfo, shortMsg, longMsg, stackTrace, timeMillis)) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (r != null && r.instrumentationClass != null) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (r != null) {
                    this.mService.mBatteryStatsService.noteProcessCrash(r.processName, r.uid);
                }
                AppErrorDialog.Data data = new AppErrorDialog.Data();
                data.result = result;
                data.proc = r;
                data.exceptionMsg = longMsg;
                if (r == null || !makeAppCrashingLocked(r, shortMsg, longMsg, stackTrace, data)) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Message msg = Message.obtain();
                msg.what = 1;
                TaskRecord task = data.task;
                msg.obj = data;
                this.mService.mUiHandler.sendMessage(msg);
                ActivityManagerService.resetPriorityAfterLockedSection();
                int res = result.get();
                MetricsLogger.action(this.mContext, 316, res);
                if (res == 6 || res == 7) {
                    res = 1;
                }
                synchronized (this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (res == 5) {
                            stopReportingCrashesLocked(r);
                        }
                        if (res == 3) {
                            this.mService.removeProcessLocked(r, false, true, "crash");
                            if (task != null) {
                                try {
                                    this.mService.startActivityFromRecents(task.taskId, ActivityOptions.makeBasic().toBundle());
                                } catch (IllegalArgumentException e) {
                                    if (task.intent != null && task.intent.getCategories() != null && task.intent.getCategories().contains("android.intent.category.LAUNCHER")) {
                                        this.mService.startActivityInPackage(task.mCallingUid, task.mCallingPackage, task.intent, null, null, null, 0, 0, ActivityOptions.makeBasic().toBundle(), task.userId, null, null);
                                    }
                                }
                            }
                        }
                        if (res == 1) {
                            long orig = Binder.clearCallingIdentity();
                            try {
                                this.mService.mStackSupervisor.handleAppCrashLocked(r);
                                if (!r.persistent) {
                                    this.mService.removeProcessLocked(r, false, false, "crash");
                                    this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                                }
                            } finally {
                                Binder.restoreCallingIdentity(orig);
                            }
                        }
                        appErrorIntent = res == 2 ? createAppErrorIntentLocked(r, timeMillis, crashInfo) : null;
                        if (r != null && !r.isolated && res != 3) {
                            this.mProcessCrashTimes.put(r.info.processName, r.uid, Long.valueOf(SystemClock.uptimeMillis()));
                        }
                    } catch (Throwable th) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (appErrorIntent != null) {
                    try {
                        this.mContext.startActivityAsUser(appErrorIntent, new UserHandle(r.userId));
                    } catch (ActivityNotFoundException e2) {
                        Slog.w(TAG, "bug report receiver dissappeared", e2);
                    }
                }
            } catch (Throwable th2) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th2;
            }
        }
    }

    private boolean handleAppCrashInActivityController(ProcessRecord r, ApplicationErrorReport.CrashInfo crashInfo, String shortMsg, String longMsg, String stackTrace, long timeMillis) {
        String str;
        if (this.mService.mController == null) {
            return false;
        }
        if (r == null) {
            str = null;
        } else {
            try {
                str = r.processName;
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
                return false;
            }
        }
        int pid = r != null ? r.pid : Binder.getCallingPid();
        int uid = r != null ? r.info.uid : Binder.getCallingUid();
        if (!this.mService.mController.appCrashed(str, pid, shortMsg, longMsg, timeMillis, crashInfo.stackTrace)) {
            if ("1".equals(SystemProperties.get("ro.debuggable", "0")) && "Native crash".equals(crashInfo.exceptionClassName)) {
                Slog.w(TAG, "Skip killing native crashed app " + str + "(" + pid + ") during testing");
                return true;
            }
            Slog.w(TAG, "Force-killing crashed app " + str + " at watcher's request");
            if (r != null) {
                if (!makeAppCrashingLocked(r, shortMsg, longMsg, stackTrace, null)) {
                    r.kill("crash", true);
                    return true;
                }
                return true;
            }
            Process.killProcess(pid);
            ActivityManagerService.killProcessGroup(uid, pid);
            return true;
        }
        return false;
    }

    private boolean makeAppCrashingLocked(ProcessRecord app, String shortMsg, String longMsg, String stackTrace, AppErrorDialog.Data data) {
        app.crashing = true;
        app.crashingReport = generateProcessError(app, 1, null, shortMsg, longMsg, stackTrace);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
        return handleAppCrashLocked(app, "force-crash", shortMsg, longMsg, stackTrace, data);
    }

    void startAppProblemLocked(ProcessRecord app) {
        app.errorReportReceiver = null;
        for (int userId : this.mService.mUserController.getCurrentProfileIdsLocked()) {
            if (app.userId == userId) {
                app.errorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mContext, app.info.packageName, app.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(app);
    }

    private ActivityManager.ProcessErrorStateInfo generateProcessError(ProcessRecord app, int condition, String activity, String shortMsg, String longMsg, String stackTrace) {
        ActivityManager.ProcessErrorStateInfo report = new ActivityManager.ProcessErrorStateInfo();
        report.condition = condition;
        report.processName = app.processName;
        report.pid = app.pid;
        report.uid = app.info.uid;
        report.tag = activity;
        report.shortMsg = shortMsg;
        report.longMsg = longMsg;
        report.stackTrace = stackTrace;
        return report;
    }

    Intent createAppErrorIntentLocked(ProcessRecord r, long timeMillis, ApplicationErrorReport.CrashInfo crashInfo) {
        ApplicationErrorReport report = createAppErrorReportLocked(r, timeMillis, crashInfo);
        if (report == null) {
            return null;
        }
        Intent result = new Intent("android.intent.action.APP_ERROR");
        result.setComponent(r.errorReportReceiver);
        result.putExtra("android.intent.extra.BUG_REPORT", report);
        result.addFlags(268435456);
        return result;
    }

    private ApplicationErrorReport createAppErrorReportLocked(ProcessRecord r, long timeMillis, ApplicationErrorReport.CrashInfo crashInfo) {
        if (r.errorReportReceiver == null) {
            return null;
        }
        if (!r.crashing && !r.notResponding && !r.forceCrashReport) {
            return null;
        }
        ApplicationErrorReport report = new ApplicationErrorReport();
        report.packageName = r.info.packageName;
        report.installerPackageName = r.errorReportReceiver.getPackageName();
        report.processName = r.processName;
        report.time = timeMillis;
        report.systemApp = (r.info.flags & 1) != 0;
        if (r.crashing || r.forceCrashReport) {
            report.type = 1;
            report.crashInfo = crashInfo;
        } else if (r.notResponding) {
            report.type = 2;
            report.anrInfo = new ApplicationErrorReport.AnrInfo();
            report.anrInfo.activity = r.notRespondingReport.tag;
            report.anrInfo.cause = r.notRespondingReport.shortMsg;
            report.anrInfo.info = r.notRespondingReport.longMsg;
        }
        return report;
    }

    boolean handleAppCrashLocked(ProcessRecord app, String reason, String shortMsg, String longMsg, String stackTrace, AppErrorDialog.Data data) {
        Long l;
        Long crashTime;
        long now = SystemClock.uptimeMillis();
        if (!app.isolated) {
            crashTime = (Long) this.mProcessCrashTimes.get(app.info.processName, app.uid);
            l = (Long) this.mProcessCrashTimesPersistent.get(app.info.processName, app.uid);
        } else {
            l = null;
            crashTime = null;
        }
        if (crashTime != null && now < crashTime.longValue() + 60000) {
            Slog.w(TAG, "Process " + app.info.processName + " has crashed too many times: killing!");
            EventLog.writeEvent(EventLogTags.AM_PROCESS_CRASHED_TOO_MUCH, Integer.valueOf(app.userId), app.info.processName, Integer.valueOf(app.uid));
            this.mService.mStackSupervisor.handleAppCrashLocked(app);
            if (!app.persistent) {
                EventLog.writeEvent(EventLogTags.AM_PROC_BAD, Integer.valueOf(app.userId), Integer.valueOf(app.uid), app.info.processName);
                if (!app.isolated) {
                    this.mBadProcesses.put(app.info.processName, app.uid, new BadProcessInfo(now, shortMsg, longMsg, stackTrace));
                    this.mProcessCrashTimes.remove(app.info.processName, app.uid);
                }
                app.bad = true;
                app.removed = true;
                this.mService.removeProcessLocked(app, false, false, "crash");
                this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                return false;
            }
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        } else {
            TaskRecord affectedTask = this.mService.mStackSupervisor.finishTopRunningActivityLocked(app, reason);
            if (data != null) {
                data.task = affectedTask;
            }
            if (data != null && l != null && now < l.longValue() + 60000) {
                data.repeating = true;
            }
        }
        for (int i = app.services.size() - 1; i >= 0; i--) {
            ServiceRecord sr = app.services.valueAt(i);
            sr.crashCount++;
        }
        if (BenesseExtension.getDchaState() == 0) {
            ArrayList<ActivityRecord> activities = app.activities;
            if (app == this.mService.mHomeProcess && activities.size() > 0 && (this.mService.mHomeProcess.info.flags & 1) == 0) {
                for (int activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                    ActivityRecord r = activities.get(activityNdx);
                    if (r.isHomeActivity()) {
                        Log.i(TAG, "Clearing package preferred activities from " + r.packageName);
                        try {
                            ActivityThread.getPackageManager().clearPackagePreferredActivities(r.packageName);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
        }
        if (!app.isolated) {
            this.mProcessCrashTimes.put(app.info.processName, app.uid, Long.valueOf(now));
            this.mProcessCrashTimesPersistent.put(app.info.processName, app.uid, Long.valueOf(now));
        }
        if (app.crashHandler != null) {
            this.mService.mHandler.post(app.crashHandler);
            return true;
        }
        return true;
    }

    void handleShowAppErrorUi(Message msg) {
        AppErrorDialog.Data data = (AppErrorDialog.Data) msg.obj;
        boolean showBackground = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ProcessRecord proc = data.proc;
                AppErrorResult res = data.result;
                if (proc != null && proc.crashDialog != null) {
                    Slog.e(TAG, "App already has crash dialog: " + proc);
                    if (res != null) {
                        res.set(AppErrorDialog.ALREADY_SHOWING);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                boolean isBackground = UserHandle.getAppId(proc.uid) >= 10000 && proc.pid != ActivityManagerService.MY_PID;
                int[] currentProfileIdsLocked = this.mService.mUserController.getCurrentProfileIdsLocked();
                int length = currentProfileIdsLocked.length;
                for (int i = 0; i < length; i++) {
                    int userId = currentProfileIdsLocked[i];
                    isBackground &= proc.userId != userId;
                }
                if (isBackground && !showBackground) {
                    Slog.w(TAG, "Skipping crash dialog of " + proc + ": background");
                    if (res != null) {
                        res.set(AppErrorDialog.BACKGROUND_USER);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                boolean zContains = this.mAppsNotReportingCrashes != null ? this.mAppsNotReportingCrashes.contains(proc.info.packageName) : false;
                if (this.mService.canShowErrorDialogs() && !zContains) {
                    AMEventHookData.BeforeShowAppErrorDialog eventData = AMEventHookData.BeforeShowAppErrorDialog.createInstance();
                    List<MtkAppErrorDialog> dialogList = new ArrayList<>();
                    eventData.set(new Object[]{data, dialogList, this.mContext, this.mService, data.proc.processName, data.proc.info.packageName, Integer.valueOf(data.proc.uid)});
                    AMEventHookResult eventResult = this.mAMEventHook.hook(AMEventHook.Event.AM_BeforeShowAppErrorDialog, eventData);
                    if (AMEventHookResult.hasAction(eventResult, AMEventHookAction.AM_ReplaceDialog)) {
                        proc.crashDialog = dialogList.get(0);
                    } else {
                        proc.crashDialog = new AppErrorDialog(this.mContext, this.mService, data);
                    }
                } else if (res != null) {
                    res.set(AppErrorDialog.CANT_SHOW);
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (data.proc.crashDialog != null) {
                    data.proc.crashDialog.show();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void stopReportingCrashesLocked(ProcessRecord proc) {
        if (this.mAppsNotReportingCrashes == null) {
            this.mAppsNotReportingCrashes = new ArraySet<>();
        }
        this.mAppsNotReportingCrashes.add(proc.info.packageName);
    }

    final void appNotResponding(ProcessRecord app, ActivityRecord activity, ActivityRecord parent, boolean aboveSystem, String annotation) {
        boolean isSilentANR;
        File tracesFile;
        String cpuInfo;
        int pid;
        ActivityManagerService activityManagerService = this.mService;
        if (ActivityManagerService.mANRManager.isANRFlowSkipped(app.pid, app.processName, annotation, this.mService.mShuttingDown, app.notResponding, app.crashing)) {
            return;
        }
        ArrayList<Integer> firstPids = new ArrayList<>(5);
        SparseArray<Boolean> lastPids = new SparseArray<>(20);
        if (this.mService.mController != null) {
            try {
                if (this.mService.mController.appEarlyNotResponding(app.processName, app.pid, annotation) < 0 && app.pid != ActivityManagerService.MY_PID) {
                    app.kill("anr", true);
                }
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        }
        long anrTime = SystemClock.uptimeMillis();
        this.mService.updateCpuStatsNow();
        ActivityManagerService activityManagerService2 = this.mService;
        ActivityManagerService.mANRManager.enableTraceLog(false);
        ActivityManagerService activityManagerService3 = this.mService;
        ActivityManagerService.mANRManager.enableBinderLog(false);
        ANRManager.AnrDumpRecord anrDumpRecord = null;
        StringBuilder info = new StringBuilder();
        ActivityManagerService activityManagerService4 = this.mService;
        ANRManager aNRManager = ActivityManagerService.mANRManager;
        if (2 == ANRManager.enableANRDebuggingMechanism()) {
            try {
                if (app.pid == Process.myPid()) {
                    app.thread.dumpAllMessageHistory();
                } else {
                    app.thread.dumpMessageHistory();
                }
            } catch (Exception e2) {
                Slog.e(TAG, "Error happens when dumping message history", e2);
            }
        }
        boolean showBackground = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        ActivityManagerService activityManagerService5 = this.mService;
        ANRManager aNRManager2 = ActivityManagerService.mANRManager;
        if (ANRManager.enableANRDebuggingMechanism() != 0) {
            ActivityManagerService activityManagerService6 = this.mService;
            ANRManager aNRManager3 = ActivityManagerService.mANRManager;
            aNRManager3.getClass();
            ANRManager.BinderDumpThread binderDumpThread = aNRManager3.new BinderDumpThread(app.pid);
            binderDumpThread.start();
            if (!this.mService.mAnrDumpMgr.mDumpList.containsKey(app)) {
                ActivityManagerService activityManagerService7 = this.mService;
                ANRManager aNRManager4 = ActivityManagerService.mANRManager;
                aNRManager4.getClass();
                anrDumpRecord = aNRManager4.new AnrDumpRecord(app.pid, app.crashing, app.processName, app.toString(), activity != null ? activity.shortComponentName : null, (parent == null || parent.app == null) ? -1 : parent.app.pid, parent != null ? parent.shortComponentName : null, annotation, anrTime);
                if (2 == ANRManager.enableANRDebuggingMechanism()) {
                    ActivityManagerService activityManagerService8 = this.mService;
                    ActivityManagerService.mANRManager.updateProcessStats();
                    StringBuilder sb = new StringBuilder();
                    ActivityManagerService activityManagerService9 = this.mService;
                    StringBuilder sbAppend = sb.append(ActivityManagerService.mANRManager.getAndroidTime());
                    ActivityManagerService activityManagerService10 = this.mService;
                    String cpuInfo2 = sbAppend.append(ActivityManagerService.mANRManager.getProcessState()).append("\n").toString();
                    if (anrDumpRecord != null) {
                        anrDumpRecord.mCpuInfo = cpuInfo2;
                    }
                    Slog.i(TAG, cpuInfo2.toString());
                }
                this.mService.mAnrDumpMgr.startAsyncDump(anrDumpRecord);
            }
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    Slog.i(TAG, "appNotResponding-got this lock: " + app + " " + annotation);
                    if (this.mService.mShuttingDown) {
                        ActivityManagerService activityManagerService11 = this.mService;
                        ActivityManagerService.mANRManager.enableTraceLog(true);
                        ActivityManagerService activityManagerService12 = this.mService;
                        ActivityManagerService.mANRManager.enableBinderLog(true);
                        this.mService.mAnrDumpMgr.cancelDump(anrDumpRecord);
                        Slog.i(TAG, "During shutdown skipping ANR: " + app + " " + annotation);
                        return;
                    }
                    if (app.notResponding) {
                        ActivityManagerService activityManagerService13 = this.mService;
                        ActivityManagerService.mANRManager.enableTraceLog(true);
                        ActivityManagerService activityManagerService14 = this.mService;
                        ActivityManagerService.mANRManager.enableBinderLog(true);
                        this.mService.mAnrDumpMgr.cancelDump(anrDumpRecord);
                        Slog.i(TAG, "Skipping duplicate ANR: " + app + " " + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (app.crashing) {
                        ActivityManagerService activityManagerService15 = this.mService;
                        ActivityManagerService.mANRManager.enableTraceLog(true);
                        ActivityManagerService activityManagerService16 = this.mService;
                        ActivityManagerService.mANRManager.enableBinderLog(true);
                        this.mService.mAnrDumpMgr.cancelDump(anrDumpRecord);
                        Slog.i(TAG, "Crashing app skipping ANR: " + app + " " + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    app.notResponding = true;
                    EventLog.writeEvent(EventLogTags.AM_ANR, Integer.valueOf(app.userId), Integer.valueOf(app.pid), app.processName, Integer.valueOf(app.info.flags), annotation);
                    isSilentANR = (showBackground || app.isInterestingToUserLocked() || app.pid == ActivityManagerService.MY_PID) ? false : true;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (anrDumpRecord != null) {
                        synchronized (anrDumpRecord) {
                            this.mService.mAnrDumpMgr.dumpAnrDebugInfo(anrDumpRecord, false);
                        }
                    }
                    this.mService.mAnrDumpMgr.removeDumpRecord(anrDumpRecord);
                    ActivityManagerService activityManagerService17 = this.mService;
                    Boolean isCoredumping = ActivityManagerService.mANRManager.isProcDoCoredump(app.pid);
                    StringBuilder sbAppend2 = new StringBuilder().append(anrDumpRecord.mCpuInfo);
                    ActivityManagerService activityManagerService18 = this.mService;
                    ANRManager aNRManager5 = ActivityManagerService.mANRManager;
                    anrDumpRecord.mCpuInfo = sbAppend2.append(ANRManager.mMessageMap.get(Integer.valueOf(app.pid))).toString();
                    if (!isCoredumping.booleanValue()) {
                        this.mService.addErrorToDropBox("anr", app, app.processName, activity, parent, annotation, anrDumpRecord != null ? anrDumpRecord.mCpuInfo : "", null, null);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        } else {
            Slog.i(TAG, "ANR_DEBUGGING_MECHANISM is disabled");
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (this.mService.mShuttingDown) {
                        Slog.i(TAG, "During shutdown skipping ANR: " + app + " " + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (app.notResponding) {
                        Slog.i(TAG, "Skipping duplicate ANR: " + app + " " + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    if (app.crashing) {
                        Slog.i(TAG, "Crashing app skipping ANR: " + app + " " + annotation);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                    app.notResponding = true;
                    EventLog.writeEvent(EventLogTags.AM_ANR, Integer.valueOf(app.userId), Integer.valueOf(app.pid), app.processName, Integer.valueOf(app.info.flags), annotation);
                    firstPids.add(Integer.valueOf(app.pid));
                    isSilentANR = (showBackground || app.isInterestingToUserLocked() || app.pid == ActivityManagerService.MY_PID) ? false : true;
                    if (!isSilentANR) {
                        int parentPid = app.pid;
                        if (parent != null && parent.app != null && parent.app.pid > 0) {
                            parentPid = parent.app.pid;
                        }
                        if (parentPid != app.pid) {
                            firstPids.add(Integer.valueOf(parentPid));
                        }
                        if (ActivityManagerService.MY_PID != app.pid && ActivityManagerService.MY_PID != parentPid) {
                            firstPids.add(Integer.valueOf(ActivityManagerService.MY_PID));
                        }
                        for (int i = this.mService.mLruProcesses.size() - 1; i >= 0; i--) {
                            ProcessRecord r = this.mService.mLruProcesses.get(i);
                            if (r != null && r.thread != null && (pid = r.pid) > 0 && pid != app.pid && pid != parentPid && pid != ActivityManagerService.MY_PID) {
                                if (r.persistent) {
                                    firstPids.add(Integer.valueOf(pid));
                                    if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                        Slog.i(TAG, "Adding persistent proc: " + r);
                                    }
                                } else {
                                    lastPids.put(pid, Boolean.TRUE);
                                    if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                        Slog.i(TAG, "Adding ANR proc: " + r);
                                    }
                                }
                            }
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    info.setLength(0);
                    info.append("ANR in ").append(app.processName);
                    if (activity != null && activity.shortComponentName != null) {
                        info.append(" (").append(activity.shortComponentName).append(")");
                    }
                    info.append("\n");
                    info.append("PID: ").append(app.pid).append("\n");
                    if (annotation != null) {
                        info.append("Reason: ").append(annotation).append("\n");
                    }
                    if (parent != null && parent != activity) {
                        info.append("Parent: ").append(parent.shortComponentName).append("\n");
                    }
                    ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                    String[] nativeProcs = Watchdog.NATIVE_STACKS_OF_INTEREST;
                    if (isSilentANR) {
                        ActivityManagerService activityManagerService19 = this.mService;
                        tracesFile = ActivityManagerService.dumpStackTraces(true, firstPids, (ProcessCpuTracker) null, lastPids, (String[]) null);
                    } else {
                        ActivityManagerService activityManagerService20 = this.mService;
                        tracesFile = ActivityManagerService.dumpStackTraces(true, firstPids, processCpuTracker, lastPids, nativeProcs);
                    }
                    if (tracesFile != null) {
                        saveBenesseAnrLog(tracesFile, app.processName);
                    }
                    this.mService.updateCpuStatsNow();
                    synchronized (this.mService.mProcessCpuTracker) {
                        cpuInfo = this.mService.mProcessCpuTracker.printCurrentState(anrTime);
                    }
                    info.append(processCpuTracker.printCurrentLoad());
                    info.append(cpuInfo);
                    info.append(processCpuTracker.printCurrentState(anrTime));
                    Slog.e(TAG, info.toString());
                    if (tracesFile == null) {
                        Process.sendSignal(app.pid, 3);
                    }
                    this.mService.addErrorToDropBox("anr", app, app.processName, activity, parent, annotation, cpuInfo, tracesFile, null);
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
        }
        if (this.mService.mController != null) {
            try {
                ActivityManagerService activityManagerService21 = this.mService;
                ANRManager aNRManager6 = ActivityManagerService.mANRManager;
                int res = ANRManager.enableANRDebuggingMechanism() != 0 ? this.mService.mController.appNotResponding(app.processName, app.pid, anrDumpRecord != null ? anrDumpRecord.mInfo.toString() : "") : this.mService.mController.appNotResponding(app.processName, app.pid, info.toString());
                if (res != 0) {
                    if (res < 0 && app.pid != ActivityManagerService.MY_PID) {
                        app.kill("anr", true);
                        return;
                    }
                    synchronized (this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            this.mService.mServices.scheduleServiceTimeoutLocked(app);
                        } finally {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
            } catch (RemoteException e3) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        }
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                this.mService.mBatteryStatsService.noteProcessAnr(app.processName, app.uid);
                if (isSilentANR) {
                    app.kill("bg anr", true);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                ActivityManagerService activityManagerService22 = this.mService;
                ANRManager aNRManager7 = ActivityManagerService.mANRManager;
                if (ANRManager.enableANRDebuggingMechanism() != 0) {
                    makeAppNotRespondingLocked(app, activity != null ? activity.shortComponentName : null, annotation != null ? "ANR " + annotation : "ANR", anrDumpRecord != null ? anrDumpRecord.mInfo.toString() : "");
                } else {
                    makeAppNotRespondingLocked(app, activity != null ? activity.shortComponentName : null, annotation != null ? "ANR " + annotation : "ANR", info.toString());
                }
                Message msg = Message.obtain();
                HashMap<String, Object> map = new HashMap<>();
                msg.what = 2;
                msg.obj = map;
                msg.arg1 = aboveSystem ? 1 : 0;
                map.put("app", app);
                if (activity != null) {
                    map.put("activity", activity);
                }
                this.mService.mUiHandler.sendMessage(msg);
                ActivityManagerService.resetPriorityAfterLockedSection();
            } catch (Throwable th3) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th3;
            }
        }
    }

    private void makeAppNotRespondingLocked(ProcessRecord app, String activity, String shortMsg, String longMsg) {
        app.notResponding = true;
        app.notRespondingReport = generateProcessError(app, 2, activity, shortMsg, longMsg, null);
        startAppProblemLocked(app);
        app.stopFreezingAllLocked();
    }

    void handleShowAnrUi(Message msg) {
        Dialog d;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                HashMap<String, Object> data = (HashMap) msg.obj;
                ProcessRecord proc = (ProcessRecord) data.get("app");
                if (proc != null && proc.anrDialog != null) {
                    Slog.e(TAG, "App already has anr dialog: " + proc);
                    MetricsLogger.action(this.mContext, 317, -2);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Intent intent = new Intent("android.intent.action.ANR");
                if (!this.mService.mProcessesReady) {
                    intent.addFlags(1342177280);
                }
                this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, 0);
                if (BenesseExtension.getDchaState() != 0) {
                    MetricsLogger.action(this.mContext, 317, -1);
                    this.mService.killAppAtUsersRequest(proc, null);
                    d = null;
                } else {
                    if (!this.mService.canShowErrorDialogs()) {
                        ActivityManagerService activityManagerService = this.mService;
                        ANRManager aNRManager = ActivityManagerService.mANRManager;
                        if (ANRManager.enableANRDebuggingMechanism() != 0) {
                        }
                    }
                    d = new AppNotRespondingDialog(this.mService, this.mContext, proc, (ActivityRecord) data.get("activity"), msg.arg1 != 0);
                    try {
                        proc.anrDialog = d;
                    } catch (Throwable th) {
                        th = th;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (d != null) {
                    d.show();
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    static final class BadProcessInfo {
        final String longMsg;
        final String shortMsg;
        final String stack;
        final long time;

        BadProcessInfo(long time, String shortMsg, String longMsg, String stack) {
            this.time = time;
            this.shortMsg = shortMsg;
            this.longMsg = longMsg;
            this.stack = stack;
        }
    }

    void saveBenesseAnrLog(File tracesFile, String processName) throws Throwable {
        File logListFile;
        FileOutputStream fos;
        FileOutputStream fos2;
        FileOutputStream fos3;
        ZipOutputStream zos;
        FileInputStream fis;
        if (sAnrFileDateFormat == null) {
            sAnrFileDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        }
        String date = sAnrFileDateFormat.format(new Date());
        File dir = new File(BC_LOG_DIR);
        if (!dir.isDirectory()) {
            dir.mkdirs();
            dir.setExecutable(true, false);
            dir.setReadable(true, false);
            dir.setWritable(true, false);
        }
        String[] array = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File arg0, String arg1) {
                return AppErrors.m945com_android_server_am_AppErrors_lambda$1(arg0, arg1);
            }
        });
        if (array != null) {
            ArrayList<String> list = new ArrayList<>();
            Collections.addAll(list, array);
            Collections.sort(list);
            if (ActivityManagerDebugConfig.DEBUG_ANR) {
                Log.e(TAG, " ----- size : " + list.size() + " -----");
            }
            if (ActivityManagerDebugConfig.DEBUG_ANR) {
                list.forEach(new Consumer() {
                    @Override
                    public void accept(Object arg0) {
                        AppErrors.m946com_android_server_am_AppErrors_lambda$2((String) arg0);
                    }
                });
            }
            while (true) {
                if (list.size() < 40) {
                    break;
                }
                File delFile = new File("/factory/anrlog/" + list.remove(0));
                if (!delFile.delete()) {
                    Log.e(TAG, "----- Failed to delete Oldest log! : " + delFile.getAbsolutePath() + " -----");
                    break;
                }
            }
        }
        String bcLogFileName = "/factory/anrlog/" + date + "_" + processName + ".zip";
        boolean bSuccess = false;
        Throwable th = null;
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zos2 = null;
        FileInputStream fis2 = null;
        try {
            FileOutputStream fos4 = new FileOutputStream(bcLogFileName);
            try {
                zos = new ZipOutputStream(fos4);
                try {
                    fis = new FileInputStream(tracesFile.getAbsolutePath());
                } catch (Throwable th2) {
                    th = th2;
                    zos2 = zos;
                    fileOutputStream = fos4;
                }
            } catch (Throwable th3) {
                th = th3;
                fileOutputStream = fos4;
            }
            try {
                ZipEntry ze = new ZipEntry(date + "_" + processName);
                zos.putNextEntry(ze);
                byte[] buf = new byte[1024];
                while (true) {
                    int len = fis.read(buf);
                    if (len == -1) {
                        break;
                    } else {
                        zos.write(buf, 0, len);
                    }
                }
                bSuccess = true;
                if (fis != null) {
                    try {
                        try {
                            fis.close();
                            th = null;
                        } catch (IOException e) {
                            e = e;
                            Log.e(TAG, "----- Failed to write ANR log! -----", e);
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                    if (zos == null) {
                        try {
                            zos.close();
                        } catch (Throwable th5) {
                            th = th5;
                            if (th != null) {
                                if (th != th) {
                                    th.addSuppressed(th);
                                    th = th;
                                }
                            }
                            if (fos4 != null) {
                            }
                            if (bSuccess) {
                            }
                            byte[] logLine = (date + " " + processName + "\n").getBytes();
                            logListFile = new File(BC_LOG_LIST_FILE);
                            if (logListFile.exists()) {
                            }
                        }
                        th = th;
                        if (fos4 != null) {
                            try {
                                fos4.close();
                            } catch (Throwable th6) {
                                th = th6;
                                if (th != null) {
                                    if (th != th) {
                                        th.addSuppressed(th);
                                        th = th;
                                    }
                                }
                                if (th != null) {
                                }
                            }
                            th = th;
                            if (th != null) {
                                throw th;
                            }
                        } else {
                            th = th;
                            if (th != null) {
                            }
                        }
                    } else {
                        th = th;
                        if (fos4 != null) {
                        }
                    }
                } else {
                    th = null;
                    if (zos == null) {
                    }
                }
                if (bSuccess) {
                    File f = new File(bcLogFileName);
                    f.setReadable(true, false);
                    f.setWritable(true, false);
                }
                byte[] logLine2 = (date + " " + processName + "\n").getBytes();
                logListFile = new File(BC_LOG_LIST_FILE);
                if (logListFile.exists()) {
                    ArrayList<String> list2 = new ArrayList<>();
                    Throwable th7 = null;
                    FileReader fileReader = null;
                    BufferedReader br = null;
                    try {
                        FileReader fr = new FileReader(logListFile);
                        try {
                            BufferedReader br2 = new BufferedReader(fr);
                            while (true) {
                                try {
                                    String line = br2.readLine();
                                    if (line == null) {
                                        break;
                                    } else {
                                        list2.add(line + "\n");
                                    }
                                } catch (Throwable th8) {
                                    th = th8;
                                    br = br2;
                                    fileReader = fr;
                                    if (br == null) {
                                    }
                                    if (fileReader != null) {
                                    }
                                    if (th != null) {
                                    }
                                }
                            }
                            if (br2 != null) {
                                try {
                                    try {
                                        br2.close();
                                    } catch (IOException e2) {
                                        e = e2;
                                        Log.e(TAG, "----- Failed to read ANR count! -----", e);
                                        return;
                                    }
                                } catch (Throwable th9) {
                                    th7 = th9;
                                }
                            }
                            if (fr != null) {
                                try {
                                    fr.close();
                                } catch (Throwable th10) {
                                    th = th10;
                                    if (th7 != null) {
                                        if (th7 != th) {
                                            th7.addSuppressed(th);
                                            th = th7;
                                        }
                                    }
                                    if (th == null) {
                                    }
                                }
                                th = th7;
                            } else {
                                th = th7;
                            }
                            if (th == null) {
                                throw th;
                            }
                            if (list2.size() < BC_MAX_LOG_LINES) {
                                Throwable th11 = null;
                                FileOutputStream fos5 = null;
                                try {
                                    fos2 = new FileOutputStream(logListFile, true);
                                } catch (Throwable th12) {
                                    th = th12;
                                }
                                try {
                                    fos2.write(logLine2);
                                    if (fos2 != null) {
                                        try {
                                            try {
                                                fos2.close();
                                            } catch (IOException e3) {
                                                e = e3;
                                                Log.e(TAG, "----- Failed to write ANR count(2)! -----", e);
                                            }
                                        } catch (Throwable th13) {
                                            th11 = th13;
                                        }
                                    }
                                    if (th11 != null) {
                                        throw th11;
                                    }
                                } catch (Throwable th14) {
                                    th = th14;
                                    fos5 = fos2;
                                    try {
                                        throw th;
                                    } catch (Throwable th15) {
                                        th11 = th;
                                        th = th15;
                                        if (fos5 != null) {
                                            try {
                                                fos5.close();
                                            } catch (Throwable th16) {
                                                if (th11 == null) {
                                                    th11 = th16;
                                                } else if (th11 != th16) {
                                                    th11.addSuppressed(th16);
                                                }
                                            }
                                        }
                                        if (th11 != null) {
                                            throw th;
                                        }
                                        throw th11;
                                    }
                                }
                            } else {
                                if (!logListFile.delete()) {
                                    Log.e(TAG, "----- Failed to delete ANR count! -----");
                                    return;
                                }
                                Throwable th17 = null;
                                FileOutputStream fos6 = null;
                                try {
                                    fos = new FileOutputStream(logListFile);
                                } catch (Throwable th18) {
                                    th = th18;
                                }
                                try {
                                    for (int loop = (list2.size() - 200) + 1; loop < list2.size(); loop++) {
                                        fos.write(list2.get(loop).getBytes());
                                    }
                                    fos.write(logLine2);
                                    if (fos != null) {
                                        try {
                                            try {
                                                fos.close();
                                            } catch (IOException e4) {
                                                e = e4;
                                                Log.e(TAG, "----- Failed to write ANR count(3)! -----", e);
                                                return;
                                            }
                                        } catch (Throwable th19) {
                                            th17 = th19;
                                        }
                                    }
                                    if (th17 != null) {
                                        throw th17;
                                    }
                                    logListFile.setReadable(true, false);
                                    logListFile.setWritable(true, false);
                                } catch (Throwable th20) {
                                    th = th20;
                                    fos6 = fos;
                                    try {
                                        throw th;
                                    } catch (Throwable th21) {
                                        th17 = th;
                                        th = th21;
                                        if (fos6 != null) {
                                            try {
                                                try {
                                                    fos6.close();
                                                } catch (Throwable th22) {
                                                    if (th17 == null) {
                                                        th17 = th22;
                                                    } else if (th17 != th22) {
                                                        th17.addSuppressed(th22);
                                                    }
                                                }
                                            } catch (IOException e5) {
                                                e = e5;
                                                Log.e(TAG, "----- Failed to write ANR count(3)! -----", e);
                                                return;
                                            }
                                        }
                                        if (th17 != null) {
                                            throw th;
                                        }
                                        throw th17;
                                    }
                                }
                            }
                        } catch (Throwable th23) {
                            th = th23;
                            fileReader = fr;
                        }
                    } catch (Throwable th24) {
                        th = th24;
                    }
                } else {
                    Throwable th25 = null;
                    FileOutputStream fos7 = null;
                    try {
                        fos3 = new FileOutputStream(logListFile);
                    } catch (Throwable th26) {
                        th = th26;
                    }
                    try {
                        fos3.write(logLine2);
                        if (fos3 != null) {
                            try {
                                try {
                                    fos3.close();
                                } catch (Throwable th27) {
                                    th25 = th27;
                                }
                            } catch (IOException e6) {
                                e = e6;
                                Log.e(TAG, "----- Failed to write ANR count(1)! -----", e);
                            }
                        }
                        if (th25 != null) {
                            throw th25;
                        }
                        logListFile.setReadable(true, false);
                        logListFile.setWritable(true, false);
                    } catch (Throwable th28) {
                        th = th28;
                        fos7 = fos3;
                        if (fos7 != null) {
                        }
                        if (th25 != null) {
                        }
                    }
                }
            } catch (Throwable th29) {
                th = th29;
                fis2 = fis;
                zos2 = zos;
                fileOutputStream = fos4;
                try {
                    throw th;
                } catch (Throwable th30) {
                    th = th;
                    th = th30;
                    if (fis2 != null) {
                        try {
                            try {
                                fis2.close();
                            } catch (Throwable th31) {
                                if (th == null) {
                                    th = th31;
                                } else if (th != th31) {
                                    th.addSuppressed(th31);
                                }
                            }
                        } catch (IOException e7) {
                            e = e7;
                            Log.e(TAG, "----- Failed to write ANR log! -----", e);
                            if (bSuccess) {
                            }
                            byte[] logLine22 = (date + " " + processName + "\n").getBytes();
                            logListFile = new File(BC_LOG_LIST_FILE);
                            if (logListFile.exists()) {
                            }
                        }
                    }
                    if (zos2 == null) {
                        try {
                            zos2.close();
                        } catch (Throwable th32) {
                            th = th32;
                            if (th != null) {
                                if (th != th) {
                                    th.addSuppressed(th);
                                    th = th;
                                }
                            }
                            if (fileOutputStream != null) {
                            }
                            if (th != null) {
                            }
                        }
                        th = th;
                    } else {
                        th = th;
                    }
                    if (fileOutputStream != null) {
                        try {
                            fileOutputStream.close();
                        } catch (Throwable th33) {
                            th = th33;
                            if (th != null) {
                                if (th != th) {
                                    th.addSuppressed(th);
                                    th = th;
                                }
                            }
                            if (th != null) {
                            }
                        }
                        th = th;
                    } else {
                        th = th;
                    }
                    if (th != null) {
                        throw th;
                    }
                    throw th;
                }
            }
        } catch (Throwable th34) {
            th = th34;
        }
    }

    static boolean m945com_android_server_am_AppErrors_lambda$1(File d, String n) {
        return n.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}_.+\\.zip");
    }

    static void m946com_android_server_am_AppErrors_lambda$2(String s) {
        Log.e(TAG, "  ----- " + s + " -----");
    }
}
