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
import android.net.Uri;
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
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.ProcessMap;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.RescueParty;
import com.android.server.UiModeManagerService;
import com.android.server.Watchdog;
import com.android.server.am.AppErrorDialog;
import com.android.server.am.AppNotRespondingDialog;
import com.android.server.backup.BackupManagerConstants;
import com.mediatek.cta.CtaManagerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AppErrors {
    static final String BC_LOG_DIR = "/factory/anrlog";
    static final String BC_LOG_LIST_FILE = "/factory/anrlog/anr.log";
    static final int BC_MAX_LOG_FILES = 40;
    static final int BC_MAX_LOG_LINES = 200;
    static final String FACTORY_DIR = "/factory/";
    private static final String TAG = "ActivityManager";
    private ArraySet<String> mAppsNotReportingCrashes;
    private final Context mContext;
    private final ActivityManagerService mService;
    private final ProcessMap<Long> mProcessCrashTimes = new ProcessMap<>();
    private final ProcessMap<Long> mProcessCrashTimesPersistent = new ProcessMap<>();
    private final ProcessMap<BadProcessInfo> mBadProcesses = new ProcessMap<>();

    AppErrors(Context context, ActivityManagerService activityManagerService) {
        context.assertRuntimeOverlayThemable();
        this.mService = activityManagerService;
        this.mContext = context;
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j, String str) {
        ArrayMap arrayMap;
        String str2;
        SparseArray sparseArray;
        long j2;
        int i;
        String str3;
        SparseArray sparseArray2;
        String str4 = str;
        if (this.mProcessCrashTimes.getMap().isEmpty() && this.mBadProcesses.getMap().isEmpty()) {
            return;
        }
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1112396529665L, SystemClock.uptimeMillis());
        long j3 = 1138166333441L;
        long j4 = 2246267895810L;
        if (!this.mProcessCrashTimes.getMap().isEmpty()) {
            ArrayMap map = this.mProcessCrashTimes.getMap();
            int size = map.size();
            int i2 = 0;
            while (i2 < size) {
                long jStart2 = protoOutputStream.start(j4);
                String str5 = (String) map.keyAt(i2);
                SparseArray sparseArray3 = (SparseArray) map.valueAt(i2);
                int size2 = sparseArray3.size();
                protoOutputStream.write(j3, str5);
                int i3 = 0;
                while (i3 < size2) {
                    int iKeyAt = sparseArray3.keyAt(i3);
                    ProcessRecord processRecord = (ProcessRecord) this.mService.mProcessNames.get(str5, iKeyAt);
                    if (str4 != null && (processRecord == null || !processRecord.pkgList.containsKey(str4))) {
                        j2 = jStart;
                        str3 = str5;
                        sparseArray2 = sparseArray3;
                        i = size2;
                    } else {
                        j2 = jStart;
                        i = size2;
                        long jStart3 = protoOutputStream.start(2246267895810L);
                        protoOutputStream.write(1120986464257L, iKeyAt);
                        str3 = str5;
                        sparseArray2 = sparseArray3;
                        protoOutputStream.write(1112396529666L, ((Long) sparseArray3.valueAt(i3)).longValue());
                        protoOutputStream.end(jStart3);
                    }
                    i3++;
                    size2 = i;
                    jStart = j2;
                    str5 = str3;
                    sparseArray3 = sparseArray2;
                }
                protoOutputStream.end(jStart2);
                i2++;
                j3 = 1138166333441L;
                j4 = 2246267895810L;
            }
        }
        long j5 = jStart;
        if (!this.mBadProcesses.getMap().isEmpty()) {
            ArrayMap map2 = this.mBadProcesses.getMap();
            int size3 = map2.size();
            int i4 = 0;
            while (i4 < size3) {
                long jStart4 = protoOutputStream.start(2246267895811L);
                String str6 = (String) map2.keyAt(i4);
                SparseArray sparseArray4 = (SparseArray) map2.valueAt(i4);
                int size4 = sparseArray4.size();
                protoOutputStream.write(1138166333441L, str6);
                int i5 = 0;
                while (i5 < size4) {
                    int iKeyAt2 = sparseArray4.keyAt(i5);
                    ProcessRecord processRecord2 = (ProcessRecord) this.mService.mProcessNames.get(str6, iKeyAt2);
                    if (str4 != null && (processRecord2 == null || !processRecord2.pkgList.containsKey(str4))) {
                        arrayMap = map2;
                        str2 = str6;
                        sparseArray = sparseArray4;
                    } else {
                        BadProcessInfo badProcessInfo = (BadProcessInfo) sparseArray4.valueAt(i5);
                        arrayMap = map2;
                        long jStart5 = protoOutputStream.start(2246267895810L);
                        protoOutputStream.write(1120986464257L, iKeyAt2);
                        str2 = str6;
                        sparseArray = sparseArray4;
                        protoOutputStream.write(1112396529666L, badProcessInfo.time);
                        protoOutputStream.write(1138166333443L, badProcessInfo.shortMsg);
                        protoOutputStream.write(1138166333444L, badProcessInfo.longMsg);
                        protoOutputStream.write(1138166333445L, badProcessInfo.stack);
                        protoOutputStream.end(jStart5);
                    }
                    i5++;
                    map2 = arrayMap;
                    str6 = str2;
                    sparseArray4 = sparseArray;
                    str4 = str;
                }
                protoOutputStream.end(jStart4);
                i4++;
                str4 = str;
            }
        }
        protoOutputStream.end(j5);
    }

    boolean dumpLocked(FileDescriptor fileDescriptor, PrintWriter printWriter, boolean z, String str) {
        boolean z2;
        AppErrors appErrors = this;
        if (!appErrors.mProcessCrashTimes.getMap().isEmpty()) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            ArrayMap map = appErrors.mProcessCrashTimes.getMap();
            int size = map.size();
            z2 = z;
            int i = 0;
            boolean z3 = false;
            while (i < size) {
                String str2 = (String) map.keyAt(i);
                SparseArray sparseArray = (SparseArray) map.valueAt(i);
                int size2 = sparseArray.size();
                boolean z4 = z3;
                boolean z5 = z2;
                for (int i2 = 0; i2 < size2; i2++) {
                    int iKeyAt = sparseArray.keyAt(i2);
                    ProcessRecord processRecord = (ProcessRecord) appErrors.mService.mProcessNames.get(str2, iKeyAt);
                    if (str == null || (processRecord != null && processRecord.pkgList.containsKey(str))) {
                        if (!z4) {
                            if (z5) {
                                printWriter.println();
                            }
                            printWriter.println("  Time since processes crashed:");
                            z5 = true;
                            z4 = true;
                        }
                        printWriter.print("    Process ");
                        printWriter.print(str2);
                        printWriter.print(" uid ");
                        printWriter.print(iKeyAt);
                        printWriter.print(": last crashed ");
                        TimeUtils.formatDuration(jUptimeMillis - ((Long) sparseArray.valueAt(i2)).longValue(), printWriter);
                        printWriter.println(" ago");
                    }
                }
                i++;
                z2 = z5;
                z3 = z4;
            }
        } else {
            z2 = z;
        }
        if (!appErrors.mBadProcesses.getMap().isEmpty()) {
            ArrayMap map2 = appErrors.mBadProcesses.getMap();
            int size3 = map2.size();
            int i3 = 0;
            boolean z6 = false;
            while (i3 < size3) {
                String str3 = (String) map2.keyAt(i3);
                SparseArray sparseArray2 = (SparseArray) map2.valueAt(i3);
                int size4 = sparseArray2.size();
                boolean z7 = z6;
                int i4 = 0;
                while (i4 < size4) {
                    int iKeyAt2 = sparseArray2.keyAt(i4);
                    ProcessRecord processRecord2 = (ProcessRecord) appErrors.mService.mProcessNames.get(str3, iKeyAt2);
                    if (str == null || (processRecord2 != null && processRecord2.pkgList.containsKey(str))) {
                        if (!z7) {
                            if (z2) {
                                printWriter.println();
                            }
                            printWriter.println("  Bad processes:");
                            z2 = true;
                            z7 = true;
                        }
                        BadProcessInfo badProcessInfo = (BadProcessInfo) sparseArray2.valueAt(i4);
                        printWriter.print("    Bad process ");
                        printWriter.print(str3);
                        printWriter.print(" uid ");
                        printWriter.print(iKeyAt2);
                        printWriter.print(": crashed at time ");
                        printWriter.println(badProcessInfo.time);
                        if (badProcessInfo.shortMsg != null) {
                            printWriter.print("      Short msg: ");
                            printWriter.println(badProcessInfo.shortMsg);
                        }
                        if (badProcessInfo.longMsg != null) {
                            printWriter.print("      Long msg: ");
                            printWriter.println(badProcessInfo.longMsg);
                        }
                        if (badProcessInfo.stack != null) {
                            printWriter.println("      Stack:");
                            int i5 = 0;
                            for (int i6 = 0; i6 < badProcessInfo.stack.length(); i6++) {
                                if (badProcessInfo.stack.charAt(i6) == '\n') {
                                    printWriter.print("        ");
                                    printWriter.write(badProcessInfo.stack, i5, i6 - i5);
                                    printWriter.println();
                                    i5 = i6 + 1;
                                }
                            }
                            if (i5 < badProcessInfo.stack.length()) {
                                printWriter.print("        ");
                                printWriter.write(badProcessInfo.stack, i5, badProcessInfo.stack.length() - i5);
                                printWriter.println();
                            }
                        }
                    }
                    i4++;
                    appErrors = this;
                }
                i3++;
                z6 = z7;
                appErrors = this;
            }
        }
        return z2;
    }

    boolean isBadProcessLocked(ApplicationInfo applicationInfo) {
        return this.mBadProcesses.get(applicationInfo.processName, applicationInfo.uid) != null;
    }

    void clearBadProcessLocked(ApplicationInfo applicationInfo) {
        this.mBadProcesses.remove(applicationInfo.processName, applicationInfo.uid);
    }

    void resetProcessCrashTimeLocked(ApplicationInfo applicationInfo) {
        this.mProcessCrashTimes.remove(applicationInfo.processName, applicationInfo.uid);
    }

    void resetProcessCrashTimeLocked(boolean z, int i, int i2) {
        ArrayMap map = this.mProcessCrashTimes.getMap();
        for (int size = map.size() - 1; size >= 0; size--) {
            SparseArray sparseArray = (SparseArray) map.valueAt(size);
            for (int size2 = sparseArray.size() - 1; size2 >= 0; size2--) {
                boolean z2 = false;
                int iKeyAt = sparseArray.keyAt(size2);
                if (z ? UserHandle.getUserId(iKeyAt) == i2 : !(i2 != -1 ? iKeyAt != UserHandle.getUid(i2, i) : UserHandle.getAppId(iKeyAt) != i)) {
                    z2 = true;
                }
                if (z2) {
                    sparseArray.removeAt(size2);
                }
            }
            if (sparseArray.size() == 0) {
                map.removeAt(size);
            }
        }
    }

    void loadAppsNotReportingCrashesFromConfigLocked(String str) {
        if (str != null) {
            String[] strArrSplit = str.split(",");
            if (strArrSplit.length > 0) {
                this.mAppsNotReportingCrashes = new ArraySet<>();
                Collections.addAll(this.mAppsNotReportingCrashes, strArrSplit);
            }
        }
    }

    void killAppAtUserRequestLocked(ProcessRecord processRecord, Dialog dialog) {
        if (processRecord.anrDialog == dialog) {
            processRecord.anrDialog = null;
        }
        if (processRecord.waitDialog == dialog) {
            processRecord.waitDialog = null;
        }
        killAppImmediateLocked(processRecord, "user-terminated", "user request after error");
    }

    private void killAppImmediateLocked(ProcessRecord processRecord, String str, String str2) {
        processRecord.crashing = false;
        processRecord.crashingReport = null;
        processRecord.notResponding = false;
        processRecord.notRespondingReport = null;
        if (processRecord.pid > 0 && processRecord.pid != ActivityManagerService.MY_PID) {
            handleAppCrashLocked(processRecord, str, null, null, null, null);
            processRecord.kill(str2, true);
        }
    }

    void scheduleAppCrashLocked(int i, int i2, String str, int i3, String str2, boolean z) {
        final ProcessRecord processRecord;
        synchronized (this.mService.mPidsSelfLocked) {
            int i4 = 0;
            processRecord = null;
            while (true) {
                if (i4 >= this.mService.mPidsSelfLocked.size()) {
                    break;
                }
                ProcessRecord processRecordValueAt = this.mService.mPidsSelfLocked.valueAt(i4);
                if (i < 0 || processRecordValueAt.uid == i) {
                    if (processRecordValueAt.pid != i2) {
                        if (processRecordValueAt.pkgList.containsKey(str) && (i3 < 0 || processRecordValueAt.userId == i3)) {
                            processRecord = processRecordValueAt;
                        }
                    } else {
                        processRecord = processRecordValueAt;
                        break;
                    }
                }
                i4++;
            }
        }
        if (processRecord == null) {
            Slog.w(TAG, "crashApplication: nothing for uid=" + i + " initialPid=" + i2 + " packageName=" + str + " userId=" + i3);
            return;
        }
        processRecord.scheduleCrash(str2);
        if (z) {
            this.mService.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    AppErrors.lambda$scheduleAppCrashLocked$0(this.f$0, processRecord);
                }
            }, 5000L);
        }
    }

    public static void lambda$scheduleAppCrashLocked$0(AppErrors appErrors, ProcessRecord processRecord) {
        synchronized (appErrors.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                appErrors.killAppImmediateLocked(processRecord, "forced", "killed for invalid state");
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
    }

    void crashApplication(ProcessRecord processRecord, ApplicationErrorReport.CrashInfo crashInfo) {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        long jClearCallingIdentity = Binder.clearCallingIdentity();
        try {
            crashApplicationInner(processRecord, crashInfo, callingPid, callingUid);
        } finally {
            Binder.restoreCallingIdentity(jClearCallingIdentity);
        }
    }

    void crashApplicationInner(ProcessRecord processRecord, ApplicationErrorReport.CrashInfo crashInfo, int i, int i2) throws Throwable {
        String str;
        Intent intentCreateAppErrorIntentLocked;
        Set<String> categories;
        long jCurrentTimeMillis = System.currentTimeMillis();
        String str2 = crashInfo.exceptionClassName;
        String str3 = crashInfo.exceptionMessage;
        String str4 = crashInfo.stackTrace;
        if (str2 != null && str3 != null) {
            str3 = str2 + ": " + str3;
        } else {
            if (str2 != null) {
                str = str2;
            }
            if (processRecord != null && processRecord.persistent) {
                RescueParty.notePersistentAppCrash(this.mContext, processRecord.uid);
            }
            AppErrorResult appErrorResult = new AppErrorResult();
            synchronized (this.mService) {
                try {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (handleAppCrashInActivityController(processRecord, crashInfo, str2, str, str4, jCurrentTimeMillis, i, i2)) {
                            return;
                        }
                        if (processRecord != null && processRecord.instr != null) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        if (processRecord != null) {
                            this.mService.mBatteryStatsService.noteProcessCrash(processRecord.processName, processRecord.uid);
                        }
                        AppErrorDialog.Data data = new AppErrorDialog.Data();
                        data.result = appErrorResult;
                        data.proc = processRecord;
                        if (processRecord != null && makeAppCrashingLocked(processRecord, str2, str, str4, data)) {
                            Message messageObtain = Message.obtain();
                            messageObtain.what = 1;
                            TaskRecord taskRecord = data.task;
                            messageObtain.obj = data;
                            this.mService.mUiHandler.sendMessage(messageObtain);
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            int i3 = appErrorResult.get();
                            MetricsLogger.action(this.mContext, 316, i3);
                            int i4 = (i3 == 6 || i3 == 7) ? 1 : i3;
                            synchronized (this.mService) {
                                try {
                                    ActivityManagerService.boostPriorityForLockedSection();
                                    if (i4 == 5) {
                                        stopReportingCrashesLocked(processRecord);
                                    }
                                    intentCreateAppErrorIntentLocked = null;
                                    if (i4 == 3) {
                                        this.mService.removeProcessLocked(processRecord, false, true, "crash");
                                        if (taskRecord != null) {
                                            try {
                                                this.mService.startActivityFromRecents(taskRecord.taskId, ActivityOptions.makeBasic().toBundle());
                                            } catch (IllegalArgumentException e) {
                                                if (taskRecord.intent != null) {
                                                    categories = taskRecord.intent.getCategories();
                                                } else {
                                                    categories = null;
                                                }
                                                if (categories != null && categories.contains("android.intent.category.LAUNCHER")) {
                                                    this.mService.getActivityStartController().startActivityInPackage(taskRecord.mCallingUid, i, i2, taskRecord.mCallingPackage, taskRecord.intent, null, null, null, 0, 0, new SafeActivityOptions(ActivityOptions.makeBasic()), taskRecord.userId, null, "AppErrors", false);
                                                }
                                            }
                                        }
                                    }
                                    if (i4 == 1) {
                                        long jClearCallingIdentity = Binder.clearCallingIdentity();
                                        try {
                                            this.mService.mStackSupervisor.handleAppCrashLocked(processRecord);
                                            if (!processRecord.persistent) {
                                                this.mService.removeProcessLocked(processRecord, false, false, "crash");
                                                this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                                            }
                                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                                        } catch (Throwable th) {
                                            Binder.restoreCallingIdentity(jClearCallingIdentity);
                                            throw th;
                                        }
                                    }
                                    if (i4 == 8) {
                                        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS");
                                        intent.setData(Uri.parse("package:" + processRecord.info.packageName));
                                        intent.addFlags(268435456);
                                        if (BenesseExtension.getDchaState() == 0) {
                                            intentCreateAppErrorIntentLocked = intent;
                                        }
                                    }
                                    if (i4 == 2) {
                                        intentCreateAppErrorIntentLocked = createAppErrorIntentLocked(processRecord, jCurrentTimeMillis, crashInfo);
                                    }
                                    if (processRecord != null && !processRecord.isolated && i4 != 3) {
                                        this.mProcessCrashTimes.put(processRecord.info.processName, processRecord.uid, Long.valueOf(SystemClock.uptimeMillis()));
                                    }
                                } finally {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            if (intentCreateAppErrorIntentLocked != null) {
                                try {
                                    this.mContext.startActivityAsUser(intentCreateAppErrorIntentLocked, new UserHandle(processRecord.userId));
                                    return;
                                } catch (ActivityNotFoundException e2) {
                                    Slog.w(TAG, "bug report receiver dissappeared", e2);
                                    return;
                                }
                            }
                            return;
                        }
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
        }
        str = str3;
        if (processRecord != null) {
            RescueParty.notePersistentAppCrash(this.mContext, processRecord.uid);
        }
        AppErrorResult appErrorResult2 = new AppErrorResult();
        synchronized (this.mService) {
        }
    }

    private boolean handleAppCrashInActivityController(ProcessRecord processRecord, ApplicationErrorReport.CrashInfo crashInfo, String str, String str2, String str3, long j, int i, int i2) {
        String str4;
        if (this.mService.mController == null) {
            return false;
        }
        if (processRecord != null) {
            try {
                str4 = processRecord.processName;
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        } else {
            str4 = null;
        }
        int i3 = processRecord != null ? processRecord.pid : i;
        int i4 = processRecord != null ? processRecord.info.uid : i2;
        if (!this.mService.mController.appCrashed(str4, i3, str, str2, j, crashInfo.stackTrace)) {
            if ("1".equals(SystemProperties.get("ro.debuggable", "0")) && "Native crash".equals(crashInfo.exceptionClassName)) {
                Slog.w(TAG, "Skip killing native crashed app " + str4 + "(" + i3 + ") during testing");
            } else {
                Slog.w(TAG, "Force-killing crashed app " + str4 + " at watcher's request");
                if (processRecord != null) {
                    if (!makeAppCrashingLocked(processRecord, str, str2, str3, null)) {
                        processRecord.kill("crash", true);
                    }
                } else {
                    Process.killProcess(i3);
                    ActivityManagerService.killProcessGroup(i4, i3);
                }
            }
            return true;
        }
        return false;
    }

    private boolean makeAppCrashingLocked(ProcessRecord processRecord, String str, String str2, String str3, AppErrorDialog.Data data) {
        processRecord.crashing = true;
        processRecord.crashingReport = generateProcessError(processRecord, 1, null, str, str2, str3);
        startAppProblemLocked(processRecord);
        processRecord.stopFreezingAllLocked();
        return handleAppCrashLocked(processRecord, "force-crash", str, str2, str3, data);
    }

    void startAppProblemLocked(ProcessRecord processRecord) {
        processRecord.errorReportReceiver = null;
        for (int i : this.mService.mUserController.getCurrentProfileIds()) {
            if (processRecord.userId == i) {
                processRecord.errorReportReceiver = ApplicationErrorReport.getErrorReportReceiver(this.mContext, processRecord.info.packageName, processRecord.info.flags);
            }
        }
        this.mService.skipCurrentReceiverLocked(processRecord);
    }

    private ActivityManager.ProcessErrorStateInfo generateProcessError(ProcessRecord processRecord, int i, String str, String str2, String str3, String str4) {
        ActivityManager.ProcessErrorStateInfo processErrorStateInfo = new ActivityManager.ProcessErrorStateInfo();
        processErrorStateInfo.condition = i;
        processErrorStateInfo.processName = processRecord.processName;
        processErrorStateInfo.pid = processRecord.pid;
        processErrorStateInfo.uid = processRecord.info.uid;
        processErrorStateInfo.tag = str;
        processErrorStateInfo.shortMsg = str2;
        processErrorStateInfo.longMsg = str3;
        processErrorStateInfo.stackTrace = str4;
        return processErrorStateInfo;
    }

    Intent createAppErrorIntentLocked(ProcessRecord processRecord, long j, ApplicationErrorReport.CrashInfo crashInfo) {
        ApplicationErrorReport applicationErrorReportCreateAppErrorReportLocked = createAppErrorReportLocked(processRecord, j, crashInfo);
        if (applicationErrorReportCreateAppErrorReportLocked == null) {
            return null;
        }
        Intent intent = new Intent("android.intent.action.APP_ERROR");
        intent.setComponent(processRecord.errorReportReceiver);
        intent.putExtra("android.intent.extra.BUG_REPORT", applicationErrorReportCreateAppErrorReportLocked);
        intent.addFlags(268435456);
        return intent;
    }

    private ApplicationErrorReport createAppErrorReportLocked(ProcessRecord processRecord, long j, ApplicationErrorReport.CrashInfo crashInfo) {
        if (processRecord.errorReportReceiver == null) {
            return null;
        }
        if (!processRecord.crashing && !processRecord.notResponding && !processRecord.forceCrashReport) {
            return null;
        }
        ApplicationErrorReport applicationErrorReport = new ApplicationErrorReport();
        applicationErrorReport.packageName = processRecord.info.packageName;
        applicationErrorReport.installerPackageName = processRecord.errorReportReceiver.getPackageName();
        applicationErrorReport.processName = processRecord.processName;
        applicationErrorReport.time = j;
        applicationErrorReport.systemApp = (processRecord.info.flags & 1) != 0;
        if (processRecord.crashing || processRecord.forceCrashReport) {
            applicationErrorReport.type = 1;
            applicationErrorReport.crashInfo = crashInfo;
        } else if (processRecord.notResponding) {
            applicationErrorReport.type = 2;
            applicationErrorReport.anrInfo = new ApplicationErrorReport.AnrInfo();
            applicationErrorReport.anrInfo.activity = processRecord.notRespondingReport.tag;
            applicationErrorReport.anrInfo.cause = processRecord.notRespondingReport.shortMsg;
            applicationErrorReport.anrInfo.info = processRecord.notRespondingReport.longMsg;
        }
        return applicationErrorReport;
    }

    boolean handleAppCrashLocked(ProcessRecord processRecord, String str, String str2, String str3, String str4, AppErrorDialog.Data data) {
        Long l;
        Long l2;
        long j;
        boolean z;
        ArrayList<ActivityRecord> arrayList;
        int size;
        long jUptimeMillis = SystemClock.uptimeMillis();
        boolean z2 = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        boolean z3 = processRecord.curProcState == 4;
        if (!processRecord.isolated) {
            l = (Long) this.mProcessCrashTimes.get(processRecord.info.processName, processRecord.uid);
            l2 = (Long) this.mProcessCrashTimesPersistent.get(processRecord.info.processName, processRecord.uid);
        } else {
            l = null;
            l2 = null;
        }
        boolean z4 = false;
        for (int size2 = processRecord.services.size() - 1; size2 >= 0; size2--) {
            ServiceRecord serviceRecordValueAt = processRecord.services.valueAt(size2);
            if (jUptimeMillis > serviceRecordValueAt.restartTime + 60000) {
                serviceRecordValueAt.crashCount = 1;
            } else {
                serviceRecordValueAt.crashCount++;
            }
            if (serviceRecordValueAt.crashCount < this.mService.mConstants.BOUND_SERVICE_MAX_CRASH_RETRY && (serviceRecordValueAt.isForeground || z3)) {
                z4 = true;
            }
        }
        if (l != null && jUptimeMillis < l.longValue() + 60000) {
            Slog.w(TAG, "Process " + processRecord.info.processName + " has crashed too many times: killing!");
            EventLog.writeEvent(EventLogTags.AM_PROCESS_CRASHED_TOO_MUCH, Integer.valueOf(processRecord.userId), processRecord.info.processName, Integer.valueOf(processRecord.uid));
            this.mService.mStackSupervisor.handleAppCrashLocked(processRecord);
            if (!processRecord.persistent) {
                EventLog.writeEvent(EventLogTags.AM_PROC_BAD, Integer.valueOf(processRecord.userId), Integer.valueOf(processRecord.uid), processRecord.info.processName);
                if (!processRecord.isolated) {
                    j = jUptimeMillis;
                    this.mBadProcesses.put(processRecord.info.processName, processRecord.uid, new BadProcessInfo(jUptimeMillis, str2, str3, str4));
                    this.mProcessCrashTimes.remove(processRecord.info.processName, processRecord.uid);
                } else {
                    j = jUptimeMillis;
                }
                processRecord.bad = true;
                processRecord.removed = true;
                this.mService.removeProcessLocked(processRecord, false, z4, "crash");
                this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                if (!z2) {
                    return false;
                }
            } else {
                j = jUptimeMillis;
            }
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        } else {
            j = jUptimeMillis;
            TaskRecord taskRecordFinishTopCrashedActivitiesLocked = this.mService.mStackSupervisor.finishTopCrashedActivitiesLocked(processRecord, str);
            if (data != null) {
                data.task = taskRecordFinishTopCrashedActivitiesLocked;
            }
            if (data != null && l2 != null && j < l2.longValue() + 60000) {
                z = true;
                data.repeating = true;
            }
            if (data != null && z4) {
                data.isRestartableForService = z;
            }
            arrayList = processRecord.activities;
            if (processRecord == this.mService.mHomeProcess && arrayList.size() > 0 && BenesseExtension.getDchaState() == 0 && (this.mService.mHomeProcess.info.flags & 1) == 0) {
                for (size = arrayList.size() - 1; size >= 0; size--) {
                    ActivityRecord activityRecord = arrayList.get(size);
                    if (activityRecord.isActivityTypeHome()) {
                        Log.i(TAG, "Clearing package preferred activities from " + activityRecord.packageName);
                        try {
                            ActivityThread.getPackageManager().clearPackagePreferredActivities(activityRecord.packageName);
                        } catch (RemoteException e) {
                        }
                    }
                }
            }
            if (!processRecord.isolated) {
                long j2 = j;
                this.mProcessCrashTimes.put(processRecord.info.processName, processRecord.uid, Long.valueOf(j2));
                this.mProcessCrashTimesPersistent.put(processRecord.info.processName, processRecord.uid, Long.valueOf(j2));
            }
            if (processRecord.crashHandler == null) {
                this.mService.mHandler.post(processRecord.crashHandler);
                return true;
            }
            return true;
        }
        z = true;
        if (data != null) {
            data.isRestartableForService = z;
        }
        arrayList = processRecord.activities;
        if (processRecord == this.mService.mHomeProcess) {
            while (size >= 0) {
            }
        }
        if (!processRecord.isolated) {
        }
        if (processRecord.crashHandler == null) {
        }
    }

    void handleShowAppErrorUi(Message message) {
        AppErrorDialog.Data data = (AppErrorDialog.Data) message.obj;
        boolean z = true;
        boolean z2 = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        AppErrorDialog appErrorDialog = null;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                ProcessRecord processRecord = data.proc;
                AppErrorResult appErrorResult = data.result;
                if (processRecord == null) {
                    Slog.e(TAG, "handleShowAppErrorUi: proc is null");
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                String str = processRecord.info.packageName;
                int i = processRecord.userId;
                if (processRecord.crashDialog != null) {
                    Slog.e(TAG, "App already has crash dialog: " + processRecord);
                    if (appErrorResult != null) {
                        appErrorResult.set(AppErrorDialog.ALREADY_SHOWING);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                boolean z3 = UserHandle.getAppId(processRecord.uid) >= 10000 && processRecord.pid != ActivityManagerService.MY_PID;
                boolean z4 = z3;
                for (int i2 : this.mService.mUserController.getCurrentProfileIds()) {
                    z4 &= i != i2;
                }
                if (z4 && !z2) {
                    Slog.w(TAG, "Skipping crash dialog of " + processRecord + ": background");
                    if (appErrorResult != null) {
                        appErrorResult.set(AppErrorDialog.BACKGROUND_USER);
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                boolean z5 = Settings.Global.getInt(this.mContext.getContentResolver(), "show_first_crash_dialog", 0) != 0;
                boolean z6 = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "show_first_crash_dialog_dev_option", 0, this.mService.mUserController.getCurrentUserId()) != 0;
                if (this.mAppsNotReportingCrashes == null || !this.mAppsNotReportingCrashes.contains(processRecord.info.packageName)) {
                    z = false;
                }
                if ((this.mService.canShowErrorDialogs() || z2) && !z && (z5 || z6 || data.repeating)) {
                    appErrorDialog = new AppErrorDialog(this.mContext, this.mService, data);
                    processRecord.crashDialog = appErrorDialog;
                    String str2 = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    if (processRecord.crashingReport != null) {
                        str2 = processRecord.crashingReport.longMsg;
                    }
                    if (CtaManagerFactory.getInstance().makeCtaManager().showPermErrorDialog(this.mContext, processRecord.uid, processRecord.processName, processRecord.info.packageName, str2)) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        return;
                    }
                } else if (appErrorResult != null) {
                    appErrorResult.set(AppErrorDialog.CANT_SHOW);
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (appErrorDialog != null) {
                    Slog.i(TAG, "Showing crash dialog for package " + str + " u" + i);
                    appErrorDialog.show();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    void stopReportingCrashesLocked(ProcessRecord processRecord) {
        if (this.mAppsNotReportingCrashes == null) {
            this.mAppsNotReportingCrashes = new ArraySet<>();
        }
        this.mAppsNotReportingCrashes.add(processRecord.info.packageName);
    }

    static boolean isInterestingForBackgroundTraces(ProcessRecord processRecord) {
        if (processRecord.pid == ActivityManagerService.MY_PID || processRecord.isInterestingToUserLocked()) {
            return true;
        }
        return (processRecord.info != null && "com.android.systemui".equals(processRecord.info.packageName)) || processRecord.hasTopUi || processRecord.hasOverlayUi;
    }

    final void appNotResponding(ProcessRecord processRecord, ActivityRecord activityRecord, ActivityRecord activityRecord2, boolean z, String str) {
        String[] strArr;
        ArrayList arrayList;
        String strPrintCurrentState;
        String str2;
        int i;
        int i2;
        ArrayList arrayList2 = new ArrayList(5);
        SparseArray sparseArray = new SparseArray(20);
        if (this.mService.mController != null) {
            try {
                if (this.mService.mController.appEarlyNotResponding(processRecord.processName, processRecord.pid, str) < 0 && processRecord.pid != ActivityManagerService.MY_PID) {
                    processRecord.kill("anr", true);
                }
            } catch (RemoteException e) {
                this.mService.mController = null;
                Watchdog.getInstance().setActivityController(null);
            }
        }
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.mService.updateCpuStatsNow();
        boolean z2 = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                if (this.mService.mShuttingDown) {
                    Slog.i(TAG, "During shutdown skipping ANR: " + processRecord + " " + str);
                    return;
                }
                if (processRecord.notResponding) {
                    Slog.i(TAG, "Skipping duplicate ANR: " + processRecord + " " + str);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (processRecord.crashing) {
                    Slog.i(TAG, "Crashing app skipping ANR: " + processRecord + " " + str);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (processRecord.killedByAm) {
                    Slog.i(TAG, "App already killed by AM skipping ANR: " + processRecord + " " + str);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (processRecord.killed) {
                    Slog.i(TAG, "Skipping died app ANR: " + processRecord + " " + str);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                processRecord.notResponding = true;
                EventLog.writeEvent(EventLogTags.AM_ANR, Integer.valueOf(processRecord.userId), Integer.valueOf(processRecord.pid), processRecord.processName, Integer.valueOf(processRecord.info.flags), str);
                arrayList2.add(Integer.valueOf(processRecord.pid));
                boolean z3 = (z2 || isInterestingForBackgroundTraces(processRecord)) ? false : true;
                if (!z3) {
                    int i3 = processRecord.pid;
                    if (activityRecord2 != null && activityRecord2.app != null && activityRecord2.app.pid > 0) {
                        i3 = activityRecord2.app.pid;
                    }
                    if (i3 != processRecord.pid) {
                        arrayList2.add(Integer.valueOf(i3));
                    }
                    if (ActivityManagerService.MY_PID != processRecord.pid && ActivityManagerService.MY_PID != i3) {
                        arrayList2.add(Integer.valueOf(ActivityManagerService.MY_PID));
                    }
                    int size = this.mService.mLruProcesses.size() - 1;
                    while (size >= 0) {
                        ProcessRecord processRecord2 = this.mService.mLruProcesses.get(size);
                        if (processRecord2 == null || processRecord2.thread == null || (i2 = processRecord2.pid) <= 0 || i2 == processRecord.pid || i2 == i3 || i2 == ActivityManagerService.MY_PID) {
                            i = i3;
                        } else if (processRecord2.persistent) {
                            arrayList2.add(Integer.valueOf(i2));
                            if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                StringBuilder sb = new StringBuilder();
                                i = i3;
                                sb.append("Adding persistent proc: ");
                                sb.append(processRecord2);
                                Slog.i(TAG, sb.toString());
                            }
                        } else {
                            i = i3;
                            if (processRecord2.treatLikeActivity) {
                                arrayList2.add(Integer.valueOf(i2));
                                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                    Slog.i(TAG, "Adding likely IME: " + processRecord2);
                                }
                            } else {
                                sparseArray.put(i2, Boolean.TRUE);
                                if (ActivityManagerDebugConfig.DEBUG_ANR) {
                                    Slog.i(TAG, "Adding ANR proc: " + processRecord2);
                                }
                            }
                        }
                        size--;
                        i3 = i;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (this.mService.mAnrManager.startAnrDump(this.mService, processRecord, activityRecord, activityRecord2, z, str, z2)) {
                    return;
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.setLength(0);
                sb2.append("ANR in ");
                sb2.append(processRecord.processName);
                if (activityRecord != null && activityRecord.shortComponentName != null) {
                    sb2.append(" (");
                    sb2.append(activityRecord.shortComponentName);
                    sb2.append(")");
                }
                sb2.append("\n");
                sb2.append("PID: ");
                sb2.append(processRecord.pid);
                sb2.append("\n");
                if (str != null) {
                    sb2.append("Reason: ");
                    sb2.append(str);
                    sb2.append("\n");
                }
                if (activityRecord2 != null && activityRecord2 != activityRecord) {
                    sb2.append("Parent: ");
                    sb2.append(activityRecord2.shortComponentName);
                    sb2.append("\n");
                }
                ProcessCpuTracker processCpuTracker = new ProcessCpuTracker(true);
                if (z3) {
                    int i4 = 0;
                    while (true) {
                        if (i4 >= Watchdog.NATIVE_STACKS_OF_INTEREST.length) {
                            strArr = null;
                            break;
                        } else {
                            if (Watchdog.NATIVE_STACKS_OF_INTEREST[i4].equals(processRecord.processName)) {
                                strArr = new String[]{processRecord.processName};
                                break;
                            }
                            i4++;
                        }
                    }
                } else {
                    strArr = Watchdog.NATIVE_STACKS_OF_INTEREST;
                }
                int[] pidsForCommands = strArr == null ? null : Process.getPidsForCommands(strArr);
                if (pidsForCommands != null) {
                    arrayList = new ArrayList(pidsForCommands.length);
                    for (int i5 : pidsForCommands) {
                        arrayList.add(Integer.valueOf(i5));
                    }
                } else {
                    arrayList = null;
                }
                File fileDumpStackTraces = ActivityManagerService.dumpStackTraces(true, (ArrayList<Integer>) arrayList2, z3 ? null : processCpuTracker, (SparseArray<Boolean>) (z3 ? null : sparseArray), (ArrayList<Integer>) arrayList);
                if (fileDumpStackTraces != null) {
                    saveBenesseAnrLog(fileDumpStackTraces, processRecord.processName);
                }
                this.mService.updateCpuStatsNow();
                synchronized (this.mService.mProcessCpuTracker) {
                    strPrintCurrentState = this.mService.mProcessCpuTracker.printCurrentState(jUptimeMillis);
                }
                sb2.append(processCpuTracker.printCurrentLoad());
                sb2.append(strPrintCurrentState);
                sb2.append(processCpuTracker.printCurrentState(jUptimeMillis));
                Slog.e(TAG, sb2.toString());
                if (fileDumpStackTraces == null) {
                    Process.sendSignal(processRecord.pid, 3);
                }
                StatsLog.write(79, processRecord.uid, processRecord.processName, activityRecord == null ? UiModeManagerService.Shell.NIGHT_MODE_STR_UNKNOWN : activityRecord.shortComponentName, str, processRecord.info != null ? processRecord.info.isInstantApp() ? 2 : 1 : 0, processRecord != null ? processRecord.isInterestingToUserLocked() ? 2 : 1 : 0);
                this.mService.addErrorToDropBox("anr", processRecord, processRecord.processName, activityRecord, activityRecord2, str, strPrintCurrentState, fileDumpStackTraces, null);
                if (this.mService.mController != null) {
                    try {
                        int iAppNotResponding = this.mService.mController.appNotResponding(processRecord.processName, processRecord.pid, sb2.toString());
                        if (iAppNotResponding != 0) {
                            if (iAppNotResponding < 0 && processRecord.pid != ActivityManagerService.MY_PID) {
                                processRecord.kill("anr", true);
                                return;
                            }
                            synchronized (this.mService) {
                                try {
                                    ActivityManagerService.boostPriorityForLockedSection();
                                    this.mService.mServices.scheduleServiceTimeoutLocked(processRecord);
                                } finally {
                                    ActivityManagerService.resetPriorityAfterLockedSection();
                                }
                            }
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            return;
                        }
                        str2 = null;
                    } catch (RemoteException e2) {
                        str2 = null;
                        this.mService.mController = null;
                        Watchdog.getInstance().setActivityController(null);
                    }
                } else {
                    str2 = null;
                }
                synchronized (this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        this.mService.mBatteryStatsService.noteProcessAnr(processRecord.processName, processRecord.uid);
                        if (z3) {
                            processRecord.kill("bg anr", true);
                            return;
                        }
                        makeAppNotRespondingLocked(processRecord, activityRecord != null ? activityRecord.shortComponentName : str2, str != null ? "ANR " + str : "ANR", sb2.toString());
                        Message messageObtain = Message.obtain();
                        messageObtain.what = 2;
                        messageObtain.obj = new AppNotRespondingDialog.Data(processRecord, activityRecord, z);
                        this.mService.mUiHandler.sendMessage(messageObtain);
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    } finally {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                    }
                }
            } finally {
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    private void makeAppNotRespondingLocked(ProcessRecord processRecord, String str, String str2, String str3) {
        processRecord.notResponding = true;
        processRecord.notRespondingReport = generateProcessError(processRecord, 2, str, str2, str3, null);
        startAppProblemLocked(processRecord);
        processRecord.stopFreezingAllLocked();
    }

    void handleShowAnrUi(Message message) {
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                AppNotRespondingDialog.Data data = (AppNotRespondingDialog.Data) message.obj;
                ProcessRecord processRecord = data.proc;
                if (processRecord == null) {
                    Slog.e(TAG, "handleShowAnrUi: proc is null");
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                if (processRecord.anrDialog != null) {
                    Slog.e(TAG, "App already has anr dialog: " + processRecord);
                    MetricsLogger.action(this.mContext, 317, -2);
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    return;
                }
                Intent intent = new Intent("android.intent.action.ANR");
                if (!this.mService.mProcessesReady) {
                    intent.addFlags(1342177280);
                }
                this.mService.broadcastIntentLocked(null, null, intent, null, null, 0, null, null, null, -1, null, false, false, ActivityManagerService.MY_PID, 1000, 0);
                boolean z = Settings.Secure.getInt(this.mContext.getContentResolver(), "anr_show_background", 0) != 0;
                AppNotRespondingDialog appNotRespondingDialog = null;
                if ((this.mService.canShowErrorDialogs() || z) && BenesseExtension.getDchaState() == 0) {
                    appNotRespondingDialog = new AppNotRespondingDialog(this.mService, this.mContext, data);
                    processRecord.anrDialog = appNotRespondingDialog;
                } else {
                    MetricsLogger.action(this.mContext, 317, -1);
                    this.mService.killAppAtUsersRequest(processRecord, null);
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (appNotRespondingDialog != null) {
                    appNotRespondingDialog.show();
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
    }

    static final class BadProcessInfo {
        final String longMsg;
        final String shortMsg;
        final String stack;
        final long time;

        BadProcessInfo(long j, String str, String str2, String str3) {
            this.time = j;
            this.shortMsg = str;
            this.longMsg = str2;
            this.stack = str3;
        }
    }

    void saveBenesseAnrLog(File file, String str) throws Exception {
        boolean z;
        File file2;
        Throwable th;
        Throwable th2;
        FileOutputStream fileOutputStream;
        Throwable th3;
        boolean z2;
        Throwable th4;
        Throwable th5;
        Throwable th6;
        String strSubstring = file.getName().substring(4);
        File file3 = new File(BC_LOG_DIR);
        if (!file3.isDirectory()) {
            file3.mkdirs();
            file3.setExecutable(true, false);
            file3.setReadable(true, false);
            file3.setWritable(true, false);
        }
        String[] list = file3.list(new FilenameFilter() {
            @Override
            public final boolean accept(File file4, String str2) {
                return str2.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{3}_.+\\.zip");
            }
        });
        if (list != null) {
            ArrayList arrayList = new ArrayList();
            Collections.addAll(arrayList, list);
            Collections.sort(arrayList);
            if (ActivityManagerDebugConfig.DEBUG_ANR) {
                Log.e(TAG, " ----- size : " + arrayList.size() + " -----");
            }
            if (ActivityManagerDebugConfig.DEBUG_ANR) {
                arrayList.forEach(new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        Log.e(AppErrors.TAG, "  ----- " + ((String) obj) + " -----");
                    }
                });
            }
            while (true) {
                if (arrayList.size() < 40) {
                    break;
                }
                File file4 = new File("/factory/anrlog/" + ((String) arrayList.remove(0)));
                if (!file4.delete()) {
                    Log.e(TAG, "----- Failed to delete Oldest log! : " + file4.getAbsolutePath() + " -----");
                    break;
                }
            }
        }
        String str2 = "/factory/anrlog/" + strSubstring + "_" + str + ".zip";
        Throwable th7 = null;
        try {
            fileOutputStream = new FileOutputStream(str2);
        } catch (IOException e) {
            e = e;
            z = false;
        }
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
            try {
                FileInputStream fileInputStream = new FileInputStream(file.getAbsolutePath());
                try {
                    zipOutputStream.putNextEntry(new ZipEntry(strSubstring + "_" + str));
                    byte[] bArr = new byte[1024];
                    while (true) {
                        int i = fileInputStream.read(bArr);
                        if (i != -1) {
                            zipOutputStream.write(bArr, 0, i);
                        } else {
                            try {
                                break;
                            } catch (Throwable th8) {
                                th = th8;
                                z2 = true;
                                try {
                                    throw th;
                                } catch (Throwable th9) {
                                    boolean z3 = z2;
                                    th4 = th;
                                    th = th9;
                                    z = z3;
                                    $closeResource(th4, zipOutputStream);
                                    throw th;
                                }
                            }
                        }
                    }
                    $closeResource(null, fileInputStream);
                    try {
                        $closeResource(null, zipOutputStream);
                        try {
                            $closeResource(null, fileOutputStream);
                            z = true;
                        } catch (IOException e2) {
                            e = e2;
                            z = true;
                            Log.e(TAG, "----- Failed to write ANR log! -----", e);
                        }
                        if (z) {
                            File file5 = new File(str2);
                            file5.setReadable(true, false);
                            file5.setWritable(true, false);
                        }
                        byte[] bytes = (strSubstring + " " + str + "\n").getBytes();
                        file2 = new File(BC_LOG_LIST_FILE);
                        if (file2.exists()) {
                            try {
                                try {
                                    new FileOutputStream(file2).write(bytes);
                                    file2.setReadable(true, false);
                                    file2.setWritable(true, false);
                                    return;
                                } finally {
                                }
                            } catch (IOException e3) {
                                Log.e(TAG, "----- Failed to write ANR count(1)! -----", e3);
                                return;
                            }
                        }
                        ArrayList arrayList2 = new ArrayList();
                        try {
                            FileReader fileReader = new FileReader(file2);
                            try {
                                BufferedReader bufferedReader = new BufferedReader(fileReader);
                                while (true) {
                                    try {
                                        String line = bufferedReader.readLine();
                                        if (line == null) {
                                            break;
                                        }
                                        arrayList2.add(line + "\n");
                                    } catch (Throwable th10) {
                                        try {
                                            throw th10;
                                        } catch (Throwable th11) {
                                            th = th10;
                                            th2 = th11;
                                            $closeResource(th, bufferedReader);
                                            throw th2;
                                        }
                                    }
                                }
                                $closeResource(null, bufferedReader);
                                if (arrayList2.size() < 200) {
                                    try {
                                        FileOutputStream fileOutputStream2 = new FileOutputStream(file2, true);
                                        try {
                                            fileOutputStream2.write(bytes);
                                            return;
                                        } finally {
                                            $closeResource(th7, fileOutputStream2);
                                        }
                                    } catch (IOException e4) {
                                        Log.e(TAG, "----- Failed to write ANR count(2)! -----", e4);
                                        return;
                                    }
                                }
                                if (!file2.delete()) {
                                    Log.e(TAG, "----- Failed to delete ANR count! -----");
                                    return;
                                }
                                try {
                                    FileOutputStream fileOutputStream3 = new FileOutputStream(file2);
                                    try {
                                        try {
                                            for (int size = (arrayList2.size() - 200) + 1; size < arrayList2.size(); size++) {
                                                fileOutputStream3.write(((String) arrayList2.get(size)).getBytes());
                                            }
                                            fileOutputStream3.write(bytes);
                                            file2.setReadable(true, false);
                                            file2.setWritable(true, false);
                                        } catch (Throwable th12) {
                                            th7 = th12;
                                            throw th7;
                                        }
                                    } finally {
                                    }
                                } catch (IOException e5) {
                                    Log.e(TAG, "----- Failed to write ANR count(3)! -----", e5);
                                }
                            } finally {
                                $closeResource(null, fileReader);
                            }
                        } catch (IOException e6) {
                            Log.e(TAG, "----- Failed to read ANR count! -----", e6);
                        }
                    } catch (Throwable th13) {
                        th = th13;
                        z = true;
                        throw th;
                    }
                } catch (Throwable th14) {
                    try {
                        throw th14;
                    } catch (Throwable th15) {
                        th5 = th14;
                        th6 = th15;
                        $closeResource(th5, fileInputStream);
                        throw th6;
                    }
                }
            } catch (Throwable th16) {
                th = th16;
                z2 = false;
            }
        } catch (Throwable th17) {
            th = th17;
            th3 = null;
            z = false;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }
}
