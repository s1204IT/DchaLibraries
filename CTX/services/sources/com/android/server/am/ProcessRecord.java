package com.android.server.am;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.DebugUtils;
import android.util.EventLog;
import android.util.Slog;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.am.ProcessList;
import com.android.server.connectivity.NetworkAgentInfo;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public final class ProcessRecord {
    private static final String TAG = "ActivityManager";
    int adjSeq;
    Object adjSource;
    int adjSourceProcState;
    Object adjTarget;
    public String adjType;
    int adjTypeCode;
    Dialog anrDialog;
    boolean bad;
    ProcessState baseProcessTracker;
    boolean cached;
    CompatibilityInfo compat;
    int completedAdjSeq;
    boolean containsCycle;
    Dialog crashDialog;
    Runnable crashHandler;
    boolean crashing;
    ActivityManager.ProcessErrorStateInfo crashingReport;
    int curAdj;
    long curCpuTime;
    BatteryStatsImpl.Uid.Proc curProcBatteryStats;
    int curRawAdj;
    int curSchedGroup;
    IBinder.DeathRecipient deathRecipient;
    boolean debugging;
    boolean empty;
    ComponentName errorReportReceiver;
    boolean execServicesFg;
    long fgInteractionTime;
    boolean forceCrashReport;
    Object forcingToImportant;
    boolean foregroundActivities;
    boolean foregroundServices;
    int[] gids;
    boolean hasAboveClient;
    boolean hasClientActivities;
    boolean hasOverlayUi;
    boolean hasShownUi;
    boolean hasStartedServices;
    boolean hasTopUi;
    String hostingNameStr;
    String hostingType;
    public boolean inFullBackup;
    final ApplicationInfo info;
    long initialIdlePss;
    ActiveInstrumentation instr;
    String instructionSet;
    long interactionEventTime;
    final boolean isolated;
    String isolatedEntryPoint;
    String[] isolatedEntryPointArgs;
    boolean killed;
    boolean killedByAm;
    long lastActivityTime;
    long lastCachedPss;
    long lastCachedSwapPss;
    long lastCpuTime;
    long lastLowMemory;
    long lastProviderTime;
    long lastPss;
    long lastPssTime;
    long lastRequestedGc;
    long lastStateTime;
    long lastSwapPss;
    int lruSeq;
    private final BatteryStatsImpl mBatteryStats;
    private final ActivityManagerService mService;
    int maxAdj;
    long nextPssTime;
    boolean notCachedSinceIdle;
    boolean notResponding;
    ActivityManager.ProcessErrorStateInfo notRespondingReport;
    boolean pendingStart;
    boolean pendingUiClean;
    boolean persistent;
    public int pid;
    ArraySet<String> pkgDeps;
    String procStatFile;
    boolean procStateChanged;
    final String processName;
    int pssStatType;
    boolean removed;
    int renderThreadTid;
    boolean repForegroundActivities;
    boolean reportLowMemory;
    boolean reportedInteraction;
    String requiredAbi;
    boolean runningRemoteAnimation;
    int savedPriority;
    String seInfo;
    boolean serviceHighRam;
    boolean serviceb;
    int setAdj;
    int setRawAdj;
    int setSchedGroup;
    String shortStringName;
    long startSeq;
    long startTime;
    int startUid;
    boolean starting;
    String stringName;
    boolean systemNoUi;
    public IApplicationThread thread;
    boolean treatLikeActivity;
    int trimMemoryLevel;
    final int uid;
    UidRecord uidRecord;
    boolean unlocked;
    final int userId;
    boolean usingWrapper;
    int verifiedAdj;
    int vrThreadTid;
    Dialog waitDialog;
    boolean waitedForDebugger;
    String waitingToKill;
    long whenUnimportant;
    boolean whitelistManager;
    final ArrayMap<String, ProcessStats.ProcessStateHolder> pkgList = new ArrayMap<>();
    final ProcessList.ProcStateMemTracker procStateMemTracker = new ProcessList.ProcStateMemTracker();
    int curProcState = 19;
    int repProcState = 19;
    int setProcState = 19;
    int pssProcState = 19;
    final ArraySet<BroadcastRecord> curReceivers = new ArraySet<>();
    final ArrayList<ActivityRecord> activities = new ArrayList<>();
    final ArrayList<TaskRecord> recentTasks = new ArrayList<>();
    final ArraySet<ServiceRecord> services = new ArraySet<>();
    final ArraySet<ServiceRecord> executingServices = new ArraySet<>();
    final ArraySet<ConnectionRecord> connections = new ArraySet<>();
    final ArraySet<ReceiverList> receivers = new ArraySet<>();
    public final ArrayMap<String, ContentProviderRecord> pubProviders = new ArrayMap<>();
    final ArrayList<ContentProviderConnection> conProviders = new ArrayList<>();

    void setStartParams(int i, String str, String str2, String str3, long j) {
        this.startUid = i;
        this.hostingType = str;
        this.hostingNameStr = str2;
        this.seInfo = str3;
        this.startTime = j;
    }

    void dump(PrintWriter printWriter, String str) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        printWriter.print(str);
        printWriter.print("user #");
        printWriter.print(this.userId);
        printWriter.print(" uid=");
        printWriter.print(this.info.uid);
        if (this.uid != this.info.uid) {
            printWriter.print(" ISOLATED uid=");
            printWriter.print(this.uid);
        }
        printWriter.print(" gids={");
        if (this.gids != null) {
            for (int i = 0; i < this.gids.length; i++) {
                if (i != 0) {
                    printWriter.print(", ");
                }
                printWriter.print(this.gids[i]);
            }
        }
        printWriter.println("}");
        printWriter.print(str);
        printWriter.print("requiredAbi=");
        printWriter.print(this.requiredAbi);
        printWriter.print(" instructionSet=");
        printWriter.println(this.instructionSet);
        if (this.info.className != null) {
            printWriter.print(str);
            printWriter.print("class=");
            printWriter.println(this.info.className);
        }
        if (this.info.manageSpaceActivityName != null) {
            printWriter.print(str);
            printWriter.print("manageSpaceActivityName=");
            printWriter.println(this.info.manageSpaceActivityName);
        }
        printWriter.print(str);
        printWriter.print("dir=");
        printWriter.print(this.info.sourceDir);
        printWriter.print(" publicDir=");
        printWriter.print(this.info.publicSourceDir);
        printWriter.print(" data=");
        printWriter.println(this.info.dataDir);
        printWriter.print(str);
        printWriter.print("packageList={");
        for (int i2 = 0; i2 < this.pkgList.size(); i2++) {
            if (i2 > 0) {
                printWriter.print(", ");
            }
            printWriter.print(this.pkgList.keyAt(i2));
        }
        printWriter.println("}");
        if (this.pkgDeps != null) {
            printWriter.print(str);
            printWriter.print("packageDependencies={");
            for (int i3 = 0; i3 < this.pkgDeps.size(); i3++) {
                if (i3 > 0) {
                    printWriter.print(", ");
                }
                printWriter.print(this.pkgDeps.valueAt(i3));
            }
            printWriter.println("}");
        }
        printWriter.print(str);
        printWriter.print("compat=");
        printWriter.println(this.compat);
        if (this.instr != null) {
            printWriter.print(str);
            printWriter.print("instr=");
            printWriter.println(this.instr);
        }
        printWriter.print(str);
        printWriter.print("thread=");
        printWriter.println(this.thread);
        printWriter.print(str);
        printWriter.print("pid=");
        printWriter.print(this.pid);
        printWriter.print(" starting=");
        printWriter.println(this.starting);
        printWriter.print(str);
        printWriter.print("lastActivityTime=");
        TimeUtils.formatDuration(this.lastActivityTime, jUptimeMillis, printWriter);
        printWriter.print(" lastPssTime=");
        TimeUtils.formatDuration(this.lastPssTime, jUptimeMillis, printWriter);
        printWriter.print(" pssStatType=");
        printWriter.print(this.pssStatType);
        printWriter.print(" nextPssTime=");
        TimeUtils.formatDuration(this.nextPssTime, jUptimeMillis, printWriter);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("adjSeq=");
        printWriter.print(this.adjSeq);
        printWriter.print(" lruSeq=");
        printWriter.print(this.lruSeq);
        printWriter.print(" lastPss=");
        DebugUtils.printSizeValue(printWriter, this.lastPss * 1024);
        printWriter.print(" lastSwapPss=");
        DebugUtils.printSizeValue(printWriter, this.lastSwapPss * 1024);
        printWriter.print(" lastCachedPss=");
        DebugUtils.printSizeValue(printWriter, this.lastCachedPss * 1024);
        printWriter.print(" lastCachedSwapPss=");
        DebugUtils.printSizeValue(printWriter, this.lastCachedSwapPss * 1024);
        printWriter.println();
        printWriter.print(str);
        printWriter.print("procStateMemTracker: ");
        this.procStateMemTracker.dumpLine(printWriter);
        printWriter.print(str);
        printWriter.print("cached=");
        printWriter.print(this.cached);
        printWriter.print(" empty=");
        printWriter.println(this.empty);
        if (this.serviceb) {
            printWriter.print(str);
            printWriter.print("serviceb=");
            printWriter.print(this.serviceb);
            printWriter.print(" serviceHighRam=");
            printWriter.println(this.serviceHighRam);
        }
        if (this.notCachedSinceIdle) {
            printWriter.print(str);
            printWriter.print("notCachedSinceIdle=");
            printWriter.print(this.notCachedSinceIdle);
            printWriter.print(" initialIdlePss=");
            printWriter.println(this.initialIdlePss);
        }
        printWriter.print(str);
        printWriter.print("oom: max=");
        printWriter.print(this.maxAdj);
        printWriter.print(" curRaw=");
        printWriter.print(this.curRawAdj);
        printWriter.print(" setRaw=");
        printWriter.print(this.setRawAdj);
        printWriter.print(" cur=");
        printWriter.print(this.curAdj);
        printWriter.print(" set=");
        printWriter.println(this.setAdj);
        printWriter.print(str);
        printWriter.print("curSchedGroup=");
        printWriter.print(this.curSchedGroup);
        printWriter.print(" setSchedGroup=");
        printWriter.print(this.setSchedGroup);
        printWriter.print(" systemNoUi=");
        printWriter.print(this.systemNoUi);
        printWriter.print(" trimMemoryLevel=");
        printWriter.println(this.trimMemoryLevel);
        if (this.vrThreadTid != 0) {
            printWriter.print(str);
            printWriter.print("vrThreadTid=");
            printWriter.println(this.vrThreadTid);
        }
        printWriter.print(str);
        printWriter.print("curProcState=");
        printWriter.print(this.curProcState);
        printWriter.print(" repProcState=");
        printWriter.print(this.repProcState);
        printWriter.print(" pssProcState=");
        printWriter.print(this.pssProcState);
        printWriter.print(" setProcState=");
        printWriter.print(this.setProcState);
        printWriter.print(" lastStateTime=");
        TimeUtils.formatDuration(this.lastStateTime, jUptimeMillis, printWriter);
        printWriter.println();
        if (this.hasShownUi || this.pendingUiClean || this.hasAboveClient || this.treatLikeActivity) {
            printWriter.print(str);
            printWriter.print("hasShownUi=");
            printWriter.print(this.hasShownUi);
            printWriter.print(" pendingUiClean=");
            printWriter.print(this.pendingUiClean);
            printWriter.print(" hasAboveClient=");
            printWriter.print(this.hasAboveClient);
            printWriter.print(" treatLikeActivity=");
            printWriter.println(this.treatLikeActivity);
        }
        if (this.hasTopUi || this.hasOverlayUi || this.runningRemoteAnimation) {
            printWriter.print(str);
            printWriter.print("hasTopUi=");
            printWriter.print(this.hasTopUi);
            printWriter.print(" hasOverlayUi=");
            printWriter.print(this.hasOverlayUi);
            printWriter.print(" runningRemoteAnimation=");
            printWriter.println(this.runningRemoteAnimation);
        }
        if (this.foregroundServices || this.forcingToImportant != null) {
            printWriter.print(str);
            printWriter.print("foregroundServices=");
            printWriter.print(this.foregroundServices);
            printWriter.print(" forcingToImportant=");
            printWriter.println(this.forcingToImportant);
        }
        if (this.reportedInteraction || this.fgInteractionTime != 0) {
            printWriter.print(str);
            printWriter.print("reportedInteraction=");
            printWriter.print(this.reportedInteraction);
            if (this.interactionEventTime != 0) {
                printWriter.print(" time=");
                TimeUtils.formatDuration(this.interactionEventTime, SystemClock.elapsedRealtime(), printWriter);
            }
            if (this.fgInteractionTime != 0) {
                printWriter.print(" fgInteractionTime=");
                TimeUtils.formatDuration(this.fgInteractionTime, SystemClock.elapsedRealtime(), printWriter);
            }
            printWriter.println();
        }
        if (this.persistent || this.removed) {
            printWriter.print(str);
            printWriter.print("persistent=");
            printWriter.print(this.persistent);
            printWriter.print(" removed=");
            printWriter.println(this.removed);
        }
        if (this.hasClientActivities || this.foregroundActivities || this.repForegroundActivities) {
            printWriter.print(str);
            printWriter.print("hasClientActivities=");
            printWriter.print(this.hasClientActivities);
            printWriter.print(" foregroundActivities=");
            printWriter.print(this.foregroundActivities);
            printWriter.print(" (rep=");
            printWriter.print(this.repForegroundActivities);
            printWriter.println(")");
        }
        if (this.lastProviderTime > 0) {
            printWriter.print(str);
            printWriter.print("lastProviderTime=");
            TimeUtils.formatDuration(this.lastProviderTime, jUptimeMillis, printWriter);
            printWriter.println();
        }
        if (this.hasStartedServices) {
            printWriter.print(str);
            printWriter.print("hasStartedServices=");
            printWriter.println(this.hasStartedServices);
        }
        if (this.pendingStart) {
            printWriter.print(str);
            printWriter.print("pendingStart=");
            printWriter.println(this.pendingStart);
        }
        printWriter.print(str);
        printWriter.print("startSeq=");
        printWriter.println(this.startSeq);
        if (this.setProcState > 9) {
            printWriter.print(str);
            printWriter.print("lastCpuTime=");
            printWriter.print(this.lastCpuTime);
            if (this.lastCpuTime > 0) {
                printWriter.print(" timeUsed=");
                TimeUtils.formatDuration(this.curCpuTime - this.lastCpuTime, printWriter);
            }
            printWriter.print(" whenUnimportant=");
            TimeUtils.formatDuration(this.whenUnimportant - jUptimeMillis, printWriter);
            printWriter.println();
        }
        printWriter.print(str);
        printWriter.print("lastRequestedGc=");
        TimeUtils.formatDuration(this.lastRequestedGc, jUptimeMillis, printWriter);
        printWriter.print(" lastLowMemory=");
        TimeUtils.formatDuration(this.lastLowMemory, jUptimeMillis, printWriter);
        printWriter.print(" reportLowMemory=");
        printWriter.println(this.reportLowMemory);
        if (this.killed || this.killedByAm || this.waitingToKill != null) {
            printWriter.print(str);
            printWriter.print("killed=");
            printWriter.print(this.killed);
            printWriter.print(" killedByAm=");
            printWriter.print(this.killedByAm);
            printWriter.print(" waitingToKill=");
            printWriter.println(this.waitingToKill);
        }
        if (this.debugging || this.crashing || this.crashDialog != null || this.notResponding || this.anrDialog != null || this.bad) {
            printWriter.print(str);
            printWriter.print("debugging=");
            printWriter.print(this.debugging);
            printWriter.print(" crashing=");
            printWriter.print(this.crashing);
            printWriter.print(" ");
            printWriter.print(this.crashDialog);
            printWriter.print(" notResponding=");
            printWriter.print(this.notResponding);
            printWriter.print(" ");
            printWriter.print(this.anrDialog);
            printWriter.print(" bad=");
            printWriter.print(this.bad);
            if (this.errorReportReceiver != null) {
                printWriter.print(" errorReportReceiver=");
                printWriter.print(this.errorReportReceiver.flattenToShortString());
            }
            printWriter.println();
        }
        if (this.whitelistManager) {
            printWriter.print(str);
            printWriter.print("whitelistManager=");
            printWriter.println(this.whitelistManager);
        }
        if (this.isolatedEntryPoint != null || this.isolatedEntryPointArgs != null) {
            printWriter.print(str);
            printWriter.print("isolatedEntryPoint=");
            printWriter.println(this.isolatedEntryPoint);
            printWriter.print(str);
            printWriter.print("isolatedEntryPointArgs=");
            printWriter.println(Arrays.toString(this.isolatedEntryPointArgs));
        }
        if (this.activities.size() > 0) {
            printWriter.print(str);
            printWriter.println("Activities:");
            for (int i4 = 0; i4 < this.activities.size(); i4++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.activities.get(i4));
            }
        }
        if (this.recentTasks.size() > 0) {
            printWriter.print(str);
            printWriter.println("Recent Tasks:");
            for (int i5 = 0; i5 < this.recentTasks.size(); i5++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.recentTasks.get(i5));
            }
        }
        if (this.services.size() > 0) {
            printWriter.print(str);
            printWriter.println("Services:");
            for (int i6 = 0; i6 < this.services.size(); i6++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.services.valueAt(i6));
            }
        }
        if (this.executingServices.size() > 0) {
            printWriter.print(str);
            printWriter.print("Executing Services (fg=");
            printWriter.print(this.execServicesFg);
            printWriter.println(")");
            for (int i7 = 0; i7 < this.executingServices.size(); i7++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.executingServices.valueAt(i7));
            }
        }
        if (this.connections.size() > 0) {
            printWriter.print(str);
            printWriter.println("Connections:");
            for (int i8 = 0; i8 < this.connections.size(); i8++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.connections.valueAt(i8));
            }
        }
        if (this.pubProviders.size() > 0) {
            printWriter.print(str);
            printWriter.println("Published Providers:");
            for (int i9 = 0; i9 < this.pubProviders.size(); i9++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.pubProviders.keyAt(i9));
                printWriter.print(str);
                printWriter.print("    -> ");
                printWriter.println(this.pubProviders.valueAt(i9));
            }
        }
        if (this.conProviders.size() > 0) {
            printWriter.print(str);
            printWriter.println("Connected Providers:");
            for (int i10 = 0; i10 < this.conProviders.size(); i10++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.conProviders.get(i10).toShortString());
            }
        }
        if (!this.curReceivers.isEmpty()) {
            printWriter.print(str);
            printWriter.println("Current Receivers:");
            for (int i11 = 0; i11 < this.curReceivers.size(); i11++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.curReceivers.valueAt(i11));
            }
        }
        if (this.receivers.size() > 0) {
            printWriter.print(str);
            printWriter.println("Receivers:");
            for (int i12 = 0; i12 < this.receivers.size(); i12++) {
                printWriter.print(str);
                printWriter.print("  - ");
                printWriter.println(this.receivers.valueAt(i12));
            }
        }
    }

    ProcessRecord(ActivityManagerService activityManagerService, BatteryStatsImpl batteryStatsImpl, ApplicationInfo applicationInfo, String str, int i) {
        this.mService = activityManagerService;
        this.mBatteryStats = batteryStatsImpl;
        this.info = applicationInfo;
        this.isolated = applicationInfo.uid != i;
        this.uid = i;
        this.userId = UserHandle.getUserId(i);
        this.processName = str;
        this.pkgList.put(applicationInfo.packageName, new ProcessStats.ProcessStateHolder(applicationInfo.longVersionCode));
        this.maxAdj = NetworkAgentInfo.EVENT_NETWORK_LINGER_COMPLETE;
        this.setRawAdj = -10000;
        this.curRawAdj = -10000;
        this.verifiedAdj = -10000;
        this.setAdj = -10000;
        this.curAdj = -10000;
        this.persistent = false;
        this.removed = false;
        long jUptimeMillis = SystemClock.uptimeMillis();
        this.nextPssTime = jUptimeMillis;
        this.lastPssTime = jUptimeMillis;
        this.lastStateTime = jUptimeMillis;
    }

    public void setPid(int i) {
        this.pid = i;
        this.procStatFile = null;
        this.shortStringName = null;
        this.stringName = null;
    }

    public void makeActive(IApplicationThread iApplicationThread, ProcessStatsService processStatsService) {
        if (this.thread == null) {
            ProcessState processState = this.baseProcessTracker;
            if (processState != null) {
                processState.setState(-1, processStatsService.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList);
                processState.makeInactive();
            }
            this.baseProcessTracker = processStatsService.getProcessStateLocked(this.info.packageName, this.uid, this.info.longVersionCode, this.processName);
            this.baseProcessTracker.makeActive();
            for (int i = 0; i < this.pkgList.size(); i++) {
                ProcessStats.ProcessStateHolder processStateHolderValueAt = this.pkgList.valueAt(i);
                if (processStateHolderValueAt.state != null && processStateHolderValueAt.state != processState) {
                    processStateHolderValueAt.state.makeInactive();
                }
                processStateHolderValueAt.state = processStatsService.getProcessStateLocked(this.pkgList.keyAt(i), this.uid, this.info.longVersionCode, this.processName);
                if (processStateHolderValueAt.state != this.baseProcessTracker) {
                    processStateHolderValueAt.state.makeActive();
                }
            }
        }
        this.thread = iApplicationThread;
    }

    public void makeInactive(ProcessStatsService processStatsService) {
        this.thread = null;
        ProcessState processState = this.baseProcessTracker;
        if (processState != null) {
            if (processState != null) {
                processState.setState(-1, processStatsService.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList);
                processState.makeInactive();
            }
            this.baseProcessTracker = null;
            for (int i = 0; i < this.pkgList.size(); i++) {
                ProcessStats.ProcessStateHolder processStateHolderValueAt = this.pkgList.valueAt(i);
                if (processStateHolderValueAt.state != null && processStateHolderValueAt.state != processState) {
                    processStateHolderValueAt.state.makeInactive();
                }
                processStateHolderValueAt.state = null;
            }
        }
    }

    public void clearRecentTasks() {
        for (int size = this.recentTasks.size() - 1; size >= 0; size--) {
            this.recentTasks.get(size).clearRootProcess();
        }
        this.recentTasks.clear();
    }

    public boolean isInterestingToUserLocked() {
        int size = this.activities.size();
        for (int i = 0; i < size; i++) {
            if (this.activities.get(i).isInterestingToUserLocked()) {
                return true;
            }
        }
        int size2 = this.services.size();
        for (int i2 = 0; i2 < size2; i2++) {
            if (this.services.valueAt(i2).isForeground) {
                return true;
            }
        }
        return false;
    }

    public void stopFreezingAllLocked() {
        int size = this.activities.size();
        while (size > 0) {
            size--;
            this.activities.get(size).stopFreezingScreenLocked(true);
        }
    }

    public void unlinkDeathRecipient() {
        if (this.deathRecipient != null && this.thread != null) {
            this.thread.asBinder().unlinkToDeath(this.deathRecipient, 0);
        }
        this.deathRecipient = null;
    }

    void updateHasAboveClientLocked() {
        this.hasAboveClient = false;
        for (int size = this.connections.size() - 1; size >= 0; size--) {
            if ((this.connections.valueAt(size).flags & 8) != 0) {
                this.hasAboveClient = true;
                return;
            }
        }
    }

    int modifyRawOomAdj(int i) {
        if (!this.hasAboveClient || i < 0) {
            return i;
        }
        if (i < 100) {
            return 100;
        }
        if (i < 200) {
            return 200;
        }
        if (i < 900) {
            return 900;
        }
        return i < 906 ? i + 1 : i;
    }

    void scheduleCrash(String str) {
        if (!this.killedByAm && this.thread != null) {
            if (this.pid == Process.myPid()) {
                Slog.w(TAG, "scheduleCrash: trying to crash system process!");
                return;
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                try {
                    this.thread.scheduleCrash(str);
                } catch (RemoteException e) {
                    kill("scheduleCrash for '" + str + "' failed", true);
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }
    }

    void kill(String str, boolean z) {
        if (this.killedByAm || !this.mService.mAmsExt.shouldKilledByAm(this.processName, str)) {
            return;
        }
        Trace.traceBegin(64L, "kill");
        if (this.mService != null && (z || this.info.uid == this.mService.mCurOomAdjUid)) {
            this.mService.reportUidInfoMessageLocked(TAG, "Killing " + toShortString() + " (adj " + this.setAdj + "): " + str, this.info.uid);
        }
        if (this.pid > 0) {
            EventLog.writeEvent(EventLogTags.AM_KILL, Integer.valueOf(this.userId), Integer.valueOf(this.pid), this.processName, Integer.valueOf(this.setAdj), str);
            Process.killProcessQuiet(this.pid);
            ActivityManagerService.killProcessGroup(this.uid, this.pid);
        } else {
            this.pendingStart = false;
        }
        if (!this.persistent) {
            this.killed = true;
            this.killedByAm = true;
        }
        Trace.traceEnd(64L);
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1120986464257L, this.pid);
        protoOutputStream.write(1138166333442L, this.processName);
        if (this.info.uid < 10000) {
            protoOutputStream.write(1120986464259L, this.uid);
        } else {
            protoOutputStream.write(1120986464260L, this.userId);
            protoOutputStream.write(1120986464261L, UserHandle.getAppId(this.info.uid));
            if (this.uid != this.info.uid) {
                protoOutputStream.write(1120986464262L, UserHandle.getAppId(this.uid));
            }
        }
        protoOutputStream.write(1133871366151L, this.persistent);
        protoOutputStream.end(jStart);
    }

    public String toShortString() {
        if (this.shortStringName != null) {
            return this.shortStringName;
        }
        StringBuilder sb = new StringBuilder(128);
        toShortString(sb);
        String string = sb.toString();
        this.shortStringName = string;
        return string;
    }

    void toShortString(StringBuilder sb) {
        sb.append(this.pid);
        sb.append(':');
        sb.append(this.processName);
        sb.append('/');
        if (this.info.uid < 10000) {
            sb.append(this.uid);
            return;
        }
        sb.append('u');
        sb.append(this.userId);
        int appId = UserHandle.getAppId(this.info.uid);
        if (appId >= 10000) {
            sb.append('a');
            sb.append(appId - 10000);
        } else {
            sb.append('s');
            sb.append(appId);
        }
        if (this.uid != this.info.uid) {
            sb.append('i');
            sb.append(UserHandle.getAppId(this.uid) - 99000);
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("ProcessRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        toShortString(sb);
        sb.append('}');
        String string = sb.toString();
        this.stringName = string;
        return string;
    }

    public String makeAdjReason() {
        if (this.adjSource != null || this.adjTarget != null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(' ');
            if (this.adjTarget instanceof ComponentName) {
                sb.append(((ComponentName) this.adjTarget).flattenToShortString());
            } else if (this.adjTarget != null) {
                sb.append(this.adjTarget.toString());
            } else {
                sb.append("{null}");
            }
            sb.append("<=");
            if (this.adjSource instanceof ProcessRecord) {
                sb.append("Proc{");
                sb.append(((ProcessRecord) this.adjSource).toShortString());
                sb.append("}");
            } else if (this.adjSource != null) {
                sb.append(this.adjSource.toString());
            } else {
                sb.append("{null}");
            }
            return sb.toString();
        }
        return null;
    }

    public boolean addPackage(String str, long j, ProcessStatsService processStatsService) {
        if (!this.pkgList.containsKey(str)) {
            ProcessStats.ProcessStateHolder processStateHolder = new ProcessStats.ProcessStateHolder(j);
            if (this.baseProcessTracker != null) {
                processStateHolder.state = processStatsService.getProcessStateLocked(str, this.uid, j, this.processName);
                this.pkgList.put(str, processStateHolder);
                if (processStateHolder.state != this.baseProcessTracker) {
                    processStateHolder.state.makeActive();
                    return true;
                }
                return true;
            }
            this.pkgList.put(str, processStateHolder);
            return true;
        }
        return false;
    }

    public int getSetAdjWithServices() {
        if (this.setAdj >= 900 && this.hasStartedServices) {
            return 800;
        }
        return this.setAdj;
    }

    public void forceProcessStateUpTo(int i) {
        if (this.repProcState > i) {
            this.repProcState = i;
            this.curProcState = i;
        }
    }

    public void resetPackageList(ProcessStatsService processStatsService) {
        int size = this.pkgList.size();
        if (this.baseProcessTracker == null) {
            if (size != 1) {
                this.pkgList.clear();
                this.pkgList.put(this.info.packageName, new ProcessStats.ProcessStateHolder(this.info.longVersionCode));
                return;
            }
            return;
        }
        this.baseProcessTracker.setState(-1, processStatsService.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList);
        if (size != 1) {
            for (int i = 0; i < size; i++) {
                ProcessStats.ProcessStateHolder processStateHolderValueAt = this.pkgList.valueAt(i);
                if (processStateHolderValueAt.state != null && processStateHolderValueAt.state != this.baseProcessTracker) {
                    processStateHolderValueAt.state.makeInactive();
                }
            }
            this.pkgList.clear();
            ProcessState processStateLocked = processStatsService.getProcessStateLocked(this.info.packageName, this.uid, this.info.longVersionCode, this.processName);
            ProcessStats.ProcessStateHolder processStateHolder = new ProcessStats.ProcessStateHolder(this.info.longVersionCode);
            processStateHolder.state = processStateLocked;
            this.pkgList.put(this.info.packageName, processStateHolder);
            if (processStateLocked != this.baseProcessTracker) {
                processStateLocked.makeActive();
            }
        }
    }

    public String[] getPackageList() {
        int size = this.pkgList.size();
        if (size == 0) {
            return null;
        }
        String[] strArr = new String[size];
        for (int i = 0; i < this.pkgList.size(); i++) {
            strArr[i] = this.pkgList.keyAt(i);
        }
        return strArr;
    }
}
