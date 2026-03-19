package com.android.server.am;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.IApplicationThread;
import android.app.IInstrumentationWatcher;
import android.app.IUiAutomationConnection;
import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import android.content.res.CompatibilityInfo;
import android.os.Binder;
import android.os.Bundle;
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
import android.util.PrintWriterPrinter;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.app.procstats.ProcessState;
import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.os.BatteryStatsImpl;
import com.mediatek.anrmanager.ANRManager;
import java.io.PrintWriter;
import java.util.ArrayList;

final class ProcessRecord {
    private static final String TAG = "ActivityManager";
    int adjSeq;
    Object adjSource;
    int adjSourceProcState;
    Object adjTarget;
    String adjType;
    int adjTypeCode;
    Dialog anrDialog;
    boolean bad;
    ProcessState baseProcessTracker;
    boolean cached;
    CompatibilityInfo compat;
    Dialog crashDialog;
    Runnable crashHandler;
    boolean crashing;
    ActivityManager.ProcessErrorStateInfo crashingReport;
    int curAdj;
    long curCpuTime;
    BatteryStatsImpl.Uid.Proc curProcBatteryStats;
    int curRawAdj;
    BroadcastRecord curReceiver;
    int curSchedGroup;
    IBinder.DeathRecipient deathRecipient;
    boolean debugging;
    boolean empty;
    ComponentName errorReportReceiver;
    boolean execServicesFg;
    long fgInteractionTime;
    boolean forceCrashReport;
    IBinder forcingToForeground;
    boolean foregroundActivities;
    boolean foregroundServices;
    int[] gids;
    boolean hasAboveClient;
    boolean hasClientActivities;
    boolean hasShownUi;
    boolean hasStartedServices;
    public boolean inFullBackup;
    final ApplicationInfo info;
    long initialIdlePss;
    String instructionSet;
    Bundle instrumentationArguments;
    ComponentName instrumentationClass;
    ApplicationInfo instrumentationInfo;
    String instrumentationProfileFile;
    ComponentName instrumentationResultClass;
    IUiAutomationConnection instrumentationUiAutomationConnection;
    IInstrumentationWatcher instrumentationWatcher;
    long interactionEventTime;
    final boolean isolated;
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
    long lastWakeTime;
    int lruSeq;
    private final BatteryStatsImpl mBatteryStats;
    int maxAdj;
    long nextPssTime;
    boolean notCachedSinceIdle;
    boolean notResponding;
    ActivityManager.ProcessErrorStateInfo notRespondingReport;
    boolean pendingUiClean;
    boolean persistent;
    int pid;
    ArraySet<String> pkgDeps;
    String procStatFile;
    boolean procStateChanged;
    final String processName;
    boolean removed;
    boolean repForegroundActivities;
    boolean reportLowMemory;
    boolean reportedInteraction;
    String requiredAbi;
    boolean serviceHighRam;
    boolean serviceb;
    int setAdj;
    boolean setIsForeground;
    int setRawAdj;
    int setSchedGroup;
    String shortStringName;
    boolean starting;
    String stringName;
    boolean systemNoUi;
    IApplicationThread thread;
    boolean treatLikeActivity;
    int trimMemoryLevel;
    final int uid;
    UidRecord uidRecord;
    boolean unlocked;
    final int userId;
    boolean usingWrapper;
    int verifiedAdj;
    Dialog waitDialog;
    boolean waitedForDebugger;
    String waitingToKill;
    boolean whitelistManager;
    final ArrayMap<String, ProcessStats.ProcessStateHolder> pkgList = new ArrayMap<>();
    int curProcState = -1;
    int repProcState = -1;
    int setProcState = -1;
    int pssProcState = -1;
    final ArrayList<ActivityRecord> activities = new ArrayList<>();
    final ArraySet<ServiceRecord> services = new ArraySet<>();
    final ArraySet<ServiceRecord> executingServices = new ArraySet<>();
    final ArraySet<ConnectionRecord> connections = new ArraySet<>();
    final ArraySet<ReceiverList> receivers = new ArraySet<>();
    final ArrayMap<String, ContentProviderRecord> pubProviders = new ArrayMap<>();
    final ArrayList<ContentProviderConnection> conProviders = new ArrayList<>();

    void dump(PrintWriter pw, String prefix) {
        long wtime;
        long now = SystemClock.uptimeMillis();
        pw.print(prefix);
        pw.print("user #");
        pw.print(this.userId);
        pw.print(" uid=");
        pw.print(this.info.uid);
        if (this.uid != this.info.uid) {
            pw.print(" ISOLATED uid=");
            pw.print(this.uid);
        }
        pw.print(" gids={");
        if (this.gids != null) {
            for (int gi = 0; gi < this.gids.length; gi++) {
                if (gi != 0) {
                    pw.print(", ");
                }
                pw.print(this.gids[gi]);
            }
        }
        pw.println("}");
        pw.print(prefix);
        pw.print("requiredAbi=");
        pw.print(this.requiredAbi);
        pw.print(" instructionSet=");
        pw.println(this.instructionSet);
        if (this.info.className != null) {
            pw.print(prefix);
            pw.print("class=");
            pw.println(this.info.className);
        }
        if (this.info.manageSpaceActivityName != null) {
            pw.print(prefix);
            pw.print("manageSpaceActivityName=");
            pw.println(this.info.manageSpaceActivityName);
        }
        pw.print(prefix);
        pw.print("dir=");
        pw.print(this.info.sourceDir);
        pw.print(" publicDir=");
        pw.print(this.info.publicSourceDir);
        pw.print(" data=");
        pw.println(this.info.dataDir);
        pw.print(prefix);
        pw.print("packageList={");
        for (int i = 0; i < this.pkgList.size(); i++) {
            if (i > 0) {
                pw.print(", ");
            }
            pw.print(this.pkgList.keyAt(i));
        }
        pw.println("}");
        if (this.pkgDeps != null) {
            pw.print(prefix);
            pw.print("packageDependencies={");
            for (int i2 = 0; i2 < this.pkgDeps.size(); i2++) {
                if (i2 > 0) {
                    pw.print(", ");
                }
                pw.print(this.pkgDeps.valueAt(i2));
            }
            pw.println("}");
        }
        pw.print(prefix);
        pw.print("compat=");
        pw.println(this.compat);
        if (this.instrumentationClass != null || this.instrumentationProfileFile != null || this.instrumentationArguments != null) {
            pw.print(prefix);
            pw.print("instrumentationClass=");
            pw.print(this.instrumentationClass);
            pw.print(" instrumentationProfileFile=");
            pw.println(this.instrumentationProfileFile);
            pw.print(prefix);
            pw.print("instrumentationArguments=");
            pw.println(this.instrumentationArguments);
            pw.print(prefix);
            pw.print("instrumentationInfo=");
            pw.println(this.instrumentationInfo);
            if (this.instrumentationInfo != null) {
                this.instrumentationInfo.dump(new PrintWriterPrinter(pw), prefix + "  ");
            }
        }
        pw.print(prefix);
        pw.print("thread=");
        pw.println(this.thread);
        pw.print(prefix);
        pw.print("pid=");
        pw.print(this.pid);
        pw.print(" starting=");
        pw.println(this.starting);
        pw.print(prefix);
        pw.print("lastActivityTime=");
        TimeUtils.formatDuration(this.lastActivityTime, now, pw);
        pw.print(" lastPssTime=");
        TimeUtils.formatDuration(this.lastPssTime, now, pw);
        pw.print(" nextPssTime=");
        TimeUtils.formatDuration(this.nextPssTime, now, pw);
        pw.println();
        pw.print(prefix);
        pw.print("adjSeq=");
        pw.print(this.adjSeq);
        pw.print(" lruSeq=");
        pw.print(this.lruSeq);
        pw.print(" lastPss=");
        DebugUtils.printSizeValue(pw, this.lastPss * 1024);
        pw.print(" lastSwapPss=");
        DebugUtils.printSizeValue(pw, this.lastSwapPss * 1024);
        pw.print(" lastCachedPss=");
        DebugUtils.printSizeValue(pw, this.lastCachedPss * 1024);
        pw.print(" lastCachedSwapPss=");
        DebugUtils.printSizeValue(pw, this.lastCachedSwapPss * 1024);
        pw.println();
        pw.print(prefix);
        pw.print("cached=");
        pw.print(this.cached);
        pw.print(" empty=");
        pw.println(this.empty);
        if (this.serviceb) {
            pw.print(prefix);
            pw.print("serviceb=");
            pw.print(this.serviceb);
            pw.print(" serviceHighRam=");
            pw.println(this.serviceHighRam);
        }
        if (this.notCachedSinceIdle) {
            pw.print(prefix);
            pw.print("notCachedSinceIdle=");
            pw.print(this.notCachedSinceIdle);
            pw.print(" initialIdlePss=");
            pw.println(this.initialIdlePss);
        }
        pw.print(prefix);
        pw.print("oom: max=");
        pw.print(this.maxAdj);
        pw.print(" curRaw=");
        pw.print(this.curRawAdj);
        pw.print(" setRaw=");
        pw.print(this.setRawAdj);
        pw.print(" cur=");
        pw.print(this.curAdj);
        pw.print(" set=");
        pw.println(this.setAdj);
        pw.print(prefix);
        pw.print("curSchedGroup=");
        pw.print(this.curSchedGroup);
        pw.print(" setSchedGroup=");
        pw.print(this.setSchedGroup);
        pw.print(" systemNoUi=");
        pw.print(this.systemNoUi);
        pw.print(" trimMemoryLevel=");
        pw.println(this.trimMemoryLevel);
        pw.print(prefix);
        pw.print("curProcState=");
        pw.print(this.curProcState);
        pw.print(" repProcState=");
        pw.print(this.repProcState);
        pw.print(" pssProcState=");
        pw.print(this.pssProcState);
        pw.print(" setProcState=");
        pw.print(this.setProcState);
        pw.print(" lastStateTime=");
        TimeUtils.formatDuration(this.lastStateTime, now, pw);
        pw.println();
        if (this.hasShownUi || this.pendingUiClean || this.hasAboveClient || this.treatLikeActivity) {
            pw.print(prefix);
            pw.print("hasShownUi=");
            pw.print(this.hasShownUi);
            pw.print(" pendingUiClean=");
            pw.print(this.pendingUiClean);
            pw.print(" hasAboveClient=");
            pw.print(this.hasAboveClient);
            pw.print(" treatLikeActivity=");
            pw.println(this.treatLikeActivity);
        }
        if (this.setIsForeground || this.foregroundServices || this.forcingToForeground != null) {
            pw.print(prefix);
            pw.print("setIsForeground=");
            pw.print(this.setIsForeground);
            pw.print(" foregroundServices=");
            pw.print(this.foregroundServices);
            pw.print(" forcingToForeground=");
            pw.println(this.forcingToForeground);
        }
        if (this.reportedInteraction || this.fgInteractionTime != 0) {
            pw.print(prefix);
            pw.print("reportedInteraction=");
            pw.print(this.reportedInteraction);
            if (this.interactionEventTime != 0) {
                pw.print(" time=");
                TimeUtils.formatDuration(this.interactionEventTime, SystemClock.elapsedRealtime(), pw);
            }
            if (this.fgInteractionTime != 0) {
                pw.print(" fgInteractionTime=");
                TimeUtils.formatDuration(this.fgInteractionTime, SystemClock.elapsedRealtime(), pw);
            }
            pw.println();
        }
        if (this.persistent || this.removed) {
            pw.print(prefix);
            pw.print("persistent=");
            pw.print(this.persistent);
            pw.print(" removed=");
            pw.println(this.removed);
        }
        if (this.hasClientActivities || this.foregroundActivities || this.repForegroundActivities) {
            pw.print(prefix);
            pw.print("hasClientActivities=");
            pw.print(this.hasClientActivities);
            pw.print(" foregroundActivities=");
            pw.print(this.foregroundActivities);
            pw.print(" (rep=");
            pw.print(this.repForegroundActivities);
            pw.println(")");
        }
        if (this.lastProviderTime > 0) {
            pw.print(prefix);
            pw.print("lastProviderTime=");
            TimeUtils.formatDuration(this.lastProviderTime, now, pw);
            pw.println();
        }
        if (this.hasStartedServices) {
            pw.print(prefix);
            pw.print("hasStartedServices=");
            pw.println(this.hasStartedServices);
        }
        if (this.setProcState >= 10) {
            synchronized (this.mBatteryStats) {
                wtime = this.mBatteryStats.getProcessWakeTime(this.info.uid, this.pid, SystemClock.elapsedRealtime());
            }
            pw.print(prefix);
            pw.print("lastWakeTime=");
            pw.print(this.lastWakeTime);
            pw.print(" timeUsed=");
            TimeUtils.formatDuration(wtime - this.lastWakeTime, pw);
            pw.println("");
            pw.print(prefix);
            pw.print("lastCpuTime=");
            pw.print(this.lastCpuTime);
            pw.print(" timeUsed=");
            TimeUtils.formatDuration(this.curCpuTime - this.lastCpuTime, pw);
            pw.println("");
        }
        pw.print(prefix);
        pw.print("lastRequestedGc=");
        TimeUtils.formatDuration(this.lastRequestedGc, now, pw);
        pw.print(" lastLowMemory=");
        TimeUtils.formatDuration(this.lastLowMemory, now, pw);
        pw.print(" reportLowMemory=");
        pw.println(this.reportLowMemory);
        if (this.killed || this.killedByAm || this.waitingToKill != null) {
            pw.print(prefix);
            pw.print("killed=");
            pw.print(this.killed);
            pw.print(" killedByAm=");
            pw.print(this.killedByAm);
            pw.print(" waitingToKill=");
            pw.println(this.waitingToKill);
        }
        if (this.debugging || this.crashing || this.crashDialog != null || this.notResponding || this.anrDialog != null || this.bad) {
            pw.print(prefix);
            pw.print("debugging=");
            pw.print(this.debugging);
            pw.print(" crashing=");
            pw.print(this.crashing);
            pw.print(" ");
            pw.print(this.crashDialog);
            pw.print(" notResponding=");
            pw.print(this.notResponding);
            pw.print(" ");
            pw.print(this.anrDialog);
            pw.print(" bad=");
            pw.print(this.bad);
            if (this.errorReportReceiver != null) {
                pw.print(" errorReportReceiver=");
                pw.print(this.errorReportReceiver.flattenToShortString());
            }
            pw.println();
        }
        if (this.whitelistManager) {
            pw.print(prefix);
            pw.print("whitelistManager=");
            pw.println(this.whitelistManager);
        }
        if (this.activities.size() > 0) {
            pw.print(prefix);
            pw.println("Activities:");
            for (int i3 = 0; i3 < this.activities.size(); i3++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.activities.get(i3));
            }
        }
        if (this.services.size() > 0) {
            pw.print(prefix);
            pw.println("Services:");
            for (int i4 = 0; i4 < this.services.size(); i4++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.services.valueAt(i4));
            }
        }
        if (this.executingServices.size() > 0) {
            pw.print(prefix);
            pw.print("Executing Services (fg=");
            pw.print(this.execServicesFg);
            pw.println(")");
            for (int i5 = 0; i5 < this.executingServices.size(); i5++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.executingServices.valueAt(i5));
            }
        }
        if (this.connections.size() > 0) {
            pw.print(prefix);
            pw.println("Connections:");
            for (int i6 = 0; i6 < this.connections.size(); i6++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.connections.valueAt(i6));
            }
        }
        if (this.pubProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Published Providers:");
            for (int i7 = 0; i7 < this.pubProviders.size(); i7++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.pubProviders.keyAt(i7));
                pw.print(prefix);
                pw.print("    -> ");
                pw.println(this.pubProviders.valueAt(i7));
            }
        }
        if (this.conProviders.size() > 0) {
            pw.print(prefix);
            pw.println("Connected Providers:");
            for (int i8 = 0; i8 < this.conProviders.size(); i8++) {
                pw.print(prefix);
                pw.print("  - ");
                pw.println(this.conProviders.get(i8).toShortString());
            }
        }
        if (this.curReceiver != null) {
            pw.print(prefix);
            pw.print("curReceiver=");
            pw.println(this.curReceiver);
        }
        if (this.receivers.size() <= 0) {
            return;
        }
        pw.print(prefix);
        pw.println("Receivers:");
        for (int i9 = 0; i9 < this.receivers.size(); i9++) {
            pw.print(prefix);
            pw.print("  - ");
            pw.println(this.receivers.valueAt(i9));
        }
    }

    ProcessRecord(BatteryStatsImpl _batteryStats, ApplicationInfo _info, String _processName, int _uid) {
        this.mBatteryStats = _batteryStats;
        this.info = _info;
        this.isolated = _info.uid != _uid;
        this.uid = _uid;
        this.userId = UserHandle.getUserId(_uid);
        this.processName = _processName;
        this.pkgList.put(_info.packageName, new ProcessStats.ProcessStateHolder(_info.versionCode));
        this.maxAdj = ANRManager.START_MONITOR_BROADCAST_TIMEOUT_MSG;
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

    public void setPid(int _pid) {
        this.pid = _pid;
        this.procStatFile = null;
        this.shortStringName = null;
        this.stringName = null;
    }

    public void makeActive(IApplicationThread _thread, ProcessStatsService tracker) {
        if (this.thread == null) {
            ProcessState origBase = this.baseProcessTracker;
            if (origBase != null) {
                origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList);
                origBase.makeInactive();
            }
            this.baseProcessTracker = tracker.getProcessStateLocked(this.info.packageName, this.uid, this.info.versionCode, this.processName);
            this.baseProcessTracker.makeActive();
            for (int i = 0; i < this.pkgList.size(); i++) {
                ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
                if (holder.state != null && holder.state != origBase) {
                    holder.state.makeInactive();
                }
                holder.state = tracker.getProcessStateLocked(this.pkgList.keyAt(i), this.uid, this.info.versionCode, this.processName);
                if (holder.state != this.baseProcessTracker) {
                    holder.state.makeActive();
                }
            }
        }
        this.thread = _thread;
    }

    public void makeInactive(ProcessStatsService tracker) {
        this.thread = null;
        ProcessState origBase = this.baseProcessTracker;
        if (origBase == null) {
            return;
        }
        if (origBase != null) {
            origBase.setState(-1, tracker.getMemFactorLocked(), SystemClock.uptimeMillis(), this.pkgList);
            origBase.makeInactive();
        }
        this.baseProcessTracker = null;
        for (int i = 0; i < this.pkgList.size(); i++) {
            ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
            if (holder.state != null && holder.state != origBase) {
                holder.state.makeInactive();
            }
            holder.state = null;
        }
    }

    public boolean isInterestingToUserLocked() {
        int size = this.activities.size();
        for (int i = 0; i < size; i++) {
            ActivityRecord r = this.activities.get(i);
            if (r.isInterestingToUserLocked()) {
                return true;
            }
        }
        return false;
    }

    public void stopFreezingAllLocked() {
        int i = this.activities.size();
        while (i > 0) {
            i--;
            this.activities.get(i).stopFreezingScreenLocked(true);
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
        for (int i = this.connections.size() - 1; i >= 0; i--) {
            ConnectionRecord cr = this.connections.valueAt(i);
            if ((cr.flags & 8) != 0) {
                this.hasAboveClient = true;
                return;
            }
        }
    }

    int modifyRawOomAdj(int adj) {
        if (this.hasAboveClient && adj >= 0) {
            if (adj < 100) {
                return 100;
            }
            if (adj < 200) {
                return 200;
            }
            if (adj < 900) {
                return 900;
            }
            if (adj < 906) {
                return adj + 1;
            }
            return adj;
        }
        return adj;
    }

    void scheduleCrash(String message) {
        if (this.killedByAm || this.thread == null) {
            return;
        }
        if (this.pid == Process.myPid()) {
            Slog.w(TAG, "scheduleCrash: trying to crash system process!");
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            this.thread.scheduleCrash(message);
        } catch (RemoteException e) {
            kill("scheduleCrash for '" + message + "' failed", true);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    void kill(String reason, boolean noisy) {
        if (this.killedByAm) {
            return;
        }
        Trace.traceBegin(64L, "kill");
        if (noisy) {
            Slog.i(TAG, "Killing " + toShortString() + " (adj " + this.setAdj + "): " + reason);
        }
        EventLog.writeEvent(EventLogTags.AM_KILL, Integer.valueOf(this.userId), Integer.valueOf(this.pid), this.processName, Integer.valueOf(this.setAdj), reason);
        Process.killProcessQuiet(this.pid);
        ActivityManagerService.killProcessGroup(this.uid, this.pid);
        if (!this.persistent) {
            this.killed = true;
            this.killedByAm = true;
        }
        Trace.traceEnd(64L);
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
        if (this.uid == this.info.uid) {
            return;
        }
        sb.append('i');
        sb.append(UserHandle.getAppId(this.uid) - 99000);
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
        if (this.adjSource == null && this.adjTarget == null) {
            return null;
        }
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

    public boolean addPackage(String pkg, int versionCode, ProcessStatsService tracker) {
        if (!this.pkgList.containsKey(pkg)) {
            ProcessStats.ProcessStateHolder holder = new ProcessStats.ProcessStateHolder(versionCode);
            if (this.baseProcessTracker != null) {
                holder.state = tracker.getProcessStateLocked(pkg, this.uid, versionCode, this.processName);
                this.pkgList.put(pkg, holder);
                if (holder.state != this.baseProcessTracker) {
                    holder.state.makeActive();
                    return true;
                }
                return true;
            }
            this.pkgList.put(pkg, holder);
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

    public void forceProcessStateUpTo(int newState) {
        if (this.repProcState <= newState) {
            return;
        }
        this.repProcState = newState;
        this.curProcState = newState;
    }

    public void resetPackageList(ProcessStatsService tracker) {
        int N = this.pkgList.size();
        if (this.baseProcessTracker != null) {
            long now = SystemClock.uptimeMillis();
            this.baseProcessTracker.setState(-1, tracker.getMemFactorLocked(), now, this.pkgList);
            if (N == 1) {
                return;
            }
            for (int i = 0; i < N; i++) {
                ProcessStats.ProcessStateHolder holder = this.pkgList.valueAt(i);
                if (holder.state != null && holder.state != this.baseProcessTracker) {
                    holder.state.makeInactive();
                }
            }
            this.pkgList.clear();
            ProcessState ps = tracker.getProcessStateLocked(this.info.packageName, this.uid, this.info.versionCode, this.processName);
            ProcessStats.ProcessStateHolder holder2 = new ProcessStats.ProcessStateHolder(this.info.versionCode);
            holder2.state = ps;
            this.pkgList.put(this.info.packageName, holder2);
            if (ps == this.baseProcessTracker) {
                return;
            }
            ps.makeActive();
            return;
        }
        if (N == 1) {
            return;
        }
        this.pkgList.clear();
        this.pkgList.put(this.info.packageName, new ProcessStats.ProcessStateHolder(this.info.versionCode));
    }

    public String[] getPackageList() {
        int size = this.pkgList.size();
        if (size == 0) {
            return null;
        }
        String[] list = new String[size];
        for (int i = 0; i < this.pkgList.size(); i++) {
            list[i] = this.pkgList.keyAt(i);
        }
        return list;
    }
}
