package com.android.server;

import android.app.IActivityController;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hidl.manager.V1_0.IServiceManager;
import android.os.Binder;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.am.ActivityManagerService;
import com.android.server.backup.BackupManagerConstants;
import com.mediatek.aee.ExceptionLog;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Watchdog extends Thread {
    static final long CHECK_INTERVAL = 30000;
    static final int COMPLETED = 0;
    static final boolean DB = false;
    static final boolean DEBUG = false;
    static final long DEFAULT_TIMEOUT = 60000;
    static final int OVERDUE = 3;
    static final String TAG = "Watchdog";
    static final int TIME_SF_WAIT = 20000;
    static final int WAITED_HALF = 2;
    static final int WAITING = 1;
    static Watchdog sWatchdog;
    ExceptionLog exceptionHWT;
    ActivityManagerService mActivity;
    boolean mAllowRestart;
    IActivityController mController;
    final ArrayList<HandlerChecker> mHandlerCheckers;
    final HandlerChecker mMonitorChecker;
    final OpenFdMonitor mOpenFdMonitor;
    int mPhonePid;
    ContentResolver mResolver;
    public static final String[] NATIVE_STACKS_OF_INTEREST = {"/system/bin/audioserver", "/system/bin/cameraserver", "/system/bin/drmserver", "/system/bin/mediadrmserver", "/system/bin/mediaserver", "/system/bin/sdcard", "/system/bin/surfaceflinger", "media.extractor", "media.metrics", "media.codec", "com.android.bluetooth", "statsd", "com.android.commands.monkey"};
    public static final List<String> HAL_INTERFACES_OF_INTEREST = Arrays.asList("android.hardware.audio@2.0::IDevicesFactory", "android.hardware.audio@4.0::IDevicesFactory", "android.hardware.bluetooth@1.0::IBluetoothHci", "android.hardware.camera.provider@2.4::ICameraProvider", "android.hardware.graphics.composer@2.1::IComposer", "android.hardware.media.omx@1.0::IOmx", "android.hardware.media.omx@1.0::IOmxStore", "android.hardware.sensors@1.0::ISensors", "android.hardware.vr@1.0::IVr");
    private static final ProcessCpuTracker mProcessStats = new ProcessCpuTracker(false);

    public interface Monitor {
        void monitor();
    }

    public long GetSFStatus() {
        if (this.exceptionHWT != null) {
            return this.exceptionHWT.SFMatterJava(0L, 0L);
        }
        return 0L;
    }

    public static int GetSFReboot() {
        return SystemProperties.getInt("service.sf.reboot", 0);
    }

    public static void SetSFReboot() {
        int i = SystemProperties.getInt("service.sf.reboot", 0) + 1;
        if (i > 9) {
            i = 9;
        }
        SystemProperties.set("service.sf.reboot", String.valueOf(i));
    }

    public final class HandlerChecker implements Runnable {
        private Monitor mCurrentMonitor;
        private final Handler mHandler;
        private final String mName;
        private long mStartTime;
        private final long mWaitMax;
        private final ArrayList<Monitor> mMonitors = new ArrayList<>();
        private boolean mCompleted = true;

        HandlerChecker(Handler handler, String str, long j) {
            this.mHandler = handler;
            this.mName = str;
            this.mWaitMax = j;
        }

        public void addMonitor(Monitor monitor) {
            this.mMonitors.add(monitor);
        }

        public void scheduleCheckLocked() {
            if (this.mMonitors.size() == 0 && this.mHandler.getLooper().getQueue().isPolling()) {
                this.mCompleted = true;
            } else {
                if (!this.mCompleted) {
                    return;
                }
                this.mCompleted = false;
                this.mCurrentMonitor = null;
                this.mStartTime = SystemClock.uptimeMillis();
                this.mHandler.postAtFrontOfQueue(this);
            }
        }

        public boolean isOverdueLocked() {
            return !this.mCompleted && SystemClock.uptimeMillis() > this.mStartTime + this.mWaitMax;
        }

        public int getCompletionStateLocked() {
            if (this.mCompleted) {
                return 0;
            }
            long jUptimeMillis = SystemClock.uptimeMillis() - this.mStartTime;
            if (jUptimeMillis < this.mWaitMax / 2) {
                return 1;
            }
            if (jUptimeMillis < this.mWaitMax) {
                return 2;
            }
            return 3;
        }

        public Thread getThread() {
            return this.mHandler.getLooper().getThread();
        }

        public String getName() {
            return this.mName;
        }

        public String describeBlockedStateLocked() {
            if (this.mCurrentMonitor == null) {
                return "Blocked in handler on " + this.mName + " (" + getThread().getName() + ")";
            }
            return "Blocked in monitor " + this.mCurrentMonitor.getClass().getName() + " on " + this.mName + " (" + getThread().getName() + ")";
        }

        @Override
        public void run() {
            int size = this.mMonitors.size();
            for (int i = 0; i < size; i++) {
                synchronized (Watchdog.this) {
                    this.mCurrentMonitor = this.mMonitors.get(i);
                }
                this.mCurrentMonitor.monitor();
            }
            synchronized (Watchdog.this) {
                this.mCompleted = true;
                this.mCurrentMonitor = null;
            }
        }
    }

    final class RebootRequestReceiver extends BroadcastReceiver {
        RebootRequestReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra("nowait", 0) != 0) {
                Watchdog.this.rebootSystem("Received ACTION_REBOOT broadcast");
                return;
            }
            Slog.w(Watchdog.TAG, "Unsupported ACTION_REBOOT broadcast: " + intent);
        }
    }

    private static final class BinderThreadMonitor implements Monitor {
        private BinderThreadMonitor() {
        }

        @Override
        public void monitor() {
            Binder.blockUntilThreadAvailable();
        }
    }

    public static Watchdog getInstance() {
        if (sWatchdog == null) {
            sWatchdog = new Watchdog();
        }
        return sWatchdog;
    }

    private Watchdog() {
        super("watchdog");
        this.mHandlerCheckers = new ArrayList<>();
        this.mAllowRestart = true;
        this.mMonitorChecker = new HandlerChecker(FgThread.getHandler(), "foreground thread", 60000L);
        this.mHandlerCheckers.add(this.mMonitorChecker);
        this.mHandlerCheckers.add(new HandlerChecker(new Handler(Looper.getMainLooper()), "main thread", 60000L));
        this.mHandlerCheckers.add(new HandlerChecker(UiThread.getHandler(), "ui thread", 60000L));
        this.mHandlerCheckers.add(new HandlerChecker(IoThread.getHandler(), "i/o thread", 60000L));
        this.mHandlerCheckers.add(new HandlerChecker(DisplayThread.getHandler(), "display thread", 60000L));
        addMonitor(new BinderThreadMonitor());
        this.mOpenFdMonitor = OpenFdMonitor.create();
        this.exceptionHWT = new ExceptionLog();
    }

    public void init(Context context, ActivityManagerService activityManagerService) {
        this.mResolver = context.getContentResolver();
        this.mActivity = activityManagerService;
        context.registerReceiver(new RebootRequestReceiver(), new IntentFilter("android.intent.action.REBOOT"), "android.permission.REBOOT", null);
        if (this.exceptionHWT != null) {
            this.exceptionHWT.WDTMatterJava(0L);
        }
    }

    public void processStarted(String str, int i) {
        synchronized (this) {
            if ("com.android.phone".equals(str)) {
                this.mPhonePid = i;
            }
        }
    }

    public void setActivityController(IActivityController iActivityController) {
        synchronized (this) {
            this.mController = iActivityController;
        }
    }

    public void setAllowRestart(boolean z) {
        synchronized (this) {
            this.mAllowRestart = z;
        }
    }

    public void addMonitor(Monitor monitor) {
        synchronized (this) {
            if (isAlive()) {
                throw new RuntimeException("Monitors can't be added once the Watchdog is running");
            }
            this.mMonitorChecker.addMonitor(monitor);
        }
    }

    public void addThread(Handler handler) {
        addThread(handler, 60000L);
    }

    public void addThread(Handler handler, long j) {
        synchronized (this) {
            if (isAlive()) {
                throw new RuntimeException("Threads can't be added once the Watchdog is running");
            }
            this.mHandlerCheckers.add(new HandlerChecker(handler, handler.getLooper().getThread().getName(), j));
        }
    }

    void rebootSystem(String str) {
        Slog.i(TAG, "Rebooting system because: " + str);
        try {
            ServiceManager.getService("power").reboot(false, str, false);
        } catch (RemoteException e) {
        }
    }

    private int evaluateCheckerCompletionLocked() {
        int iMax = 0;
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            iMax = Math.max(iMax, this.mHandlerCheckers.get(i).getCompletionStateLocked());
        }
        return iMax;
    }

    private ArrayList<HandlerChecker> getBlockedCheckersLocked() {
        ArrayList<HandlerChecker> arrayList = new ArrayList<>();
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            HandlerChecker handlerChecker = this.mHandlerCheckers.get(i);
            if (handlerChecker.isOverdueLocked()) {
                arrayList.add(handlerChecker);
            }
        }
        return arrayList;
    }

    private String describeCheckersLocked(List<HandlerChecker> list) {
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < list.size(); i++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(list.get(i).describeBlockedStateLocked());
        }
        return sb.toString();
    }

    private ArrayList<Integer> getInterestingHalPids() {
        try {
            ArrayList<IServiceManager.InstanceDebugInfo> arrayListDebugDump = IServiceManager.getService().debugDump();
            HashSet hashSet = new HashSet();
            for (IServiceManager.InstanceDebugInfo instanceDebugInfo : arrayListDebugDump) {
                if (instanceDebugInfo.pid != -1 && HAL_INTERFACES_OF_INTEREST.contains(instanceDebugInfo.interfaceName)) {
                    hashSet.add(Integer.valueOf(instanceDebugInfo.pid));
                }
            }
            return new ArrayList<>(hashSet);
        } catch (RemoteException e) {
            return new ArrayList<>();
        }
    }

    private ArrayList<Integer> getInterestingNativePids() {
        ArrayList<Integer> interestingHalPids = getInterestingHalPids();
        int[] pidsForCommands = Process.getPidsForCommands(NATIVE_STACKS_OF_INTEREST);
        if (pidsForCommands != null) {
            interestingHalPids.ensureCapacity(interestingHalPids.size() + pidsForCommands.length);
            for (int i : pidsForCommands) {
                interestingHalPids.add(Integer.valueOf(i));
            }
        }
        return interestingHalPids;
    }

    @Override
    public void run() {
        boolean z;
        char c;
        List<HandlerChecker> blockedCheckersLocked;
        String str;
        boolean z2;
        boolean z3;
        List<HandlerChecker> list;
        IActivityController iActivityController;
        boolean zMonitor;
        List<HandlerChecker> listEmptyList;
        String strDescribeCheckersLocked;
        mProcessStats.init();
        while (true) {
            z = false;
            while (true) {
                if (this.exceptionHWT != null && !z) {
                    this.exceptionHWT.WDTMatterJava(300L);
                }
                synchronized (this) {
                    for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
                        this.mHandlerCheckers.get(i).scheduleCheckLocked();
                    }
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    c = 0;
                    for (long jUptimeMillis2 = 30000; jUptimeMillis2 > 0; jUptimeMillis2 = 30000 - (SystemClock.uptimeMillis() - jUptimeMillis)) {
                        if (Debug.isDebuggerConnected()) {
                            c = 2;
                        }
                        try {
                            wait(jUptimeMillis2);
                        } catch (InterruptedException e) {
                            Log.wtf(TAG, e);
                        }
                        if (Debug.isDebuggerConnected()) {
                            c = 2;
                        }
                    }
                    long jGetSFStatus = GetSFStatus();
                    if (jGetSFStatus > 40000) {
                        Slog.v(TAG, "**SF hang Time **" + jGetSFStatus);
                        blockedCheckersLocked = getBlockedCheckersLocked();
                        str = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                        z2 = true;
                    } else {
                        if (this.mOpenFdMonitor != null) {
                            zMonitor = this.mOpenFdMonitor.monitor();
                        } else {
                            zMonitor = false;
                        }
                        if (!zMonitor) {
                            int iEvaluateCheckerCompletionLocked = evaluateCheckerCompletionLocked();
                            if (iEvaluateCheckerCompletionLocked != 0) {
                                if (iEvaluateCheckerCompletionLocked != 1) {
                                    if (iEvaluateCheckerCompletionLocked == 2) {
                                        if (!z) {
                                            if (this.exceptionHWT != null) {
                                                this.exceptionHWT.WDTMatterJava(360L);
                                            }
                                            ArrayList arrayList = new ArrayList();
                                            arrayList.add(Integer.valueOf(Process.myPid()));
                                            ActivityManagerService.dumpStackTraces(true, (ArrayList<Integer>) arrayList, (ProcessCpuTracker) null, (SparseArray<Boolean>) null, getInterestingNativePids());
                                            z = true;
                                        }
                                    } else {
                                        listEmptyList = getBlockedCheckersLocked();
                                        strDescribeCheckersLocked = describeCheckersLocked(listEmptyList);
                                    }
                                }
                            }
                        } else {
                            listEmptyList = Collections.emptyList();
                            strDescribeCheckersLocked = "Open FD high water mark reached";
                        }
                        blockedCheckersLocked = listEmptyList;
                        str = strDescribeCheckersLocked;
                        z2 = false;
                    }
                    z3 = this.mAllowRestart;
                }
                break;
            }
        }
        if (Debug.isDebuggerConnected()) {
            c = 2;
        }
        if (c < 2) {
            Slog.w(TAG, "Debugger connected: Watchdog is *not* killing the system process");
        } else if (c > 0) {
            Slog.w(TAG, "Debugger was connected: Watchdog is *not* killing the system process");
        } else if (!z3) {
            Slog.w(TAG, "Restart not allowed: Watchdog is *not* killing the system process");
        } else {
            Slog.w(TAG, "*** WATCHDOG KILLING SYSTEM PROCESS: " + str);
            WatchdogDiagnostics.diagnoseCheckers(list);
            Slog.w(TAG, "*** GOODBYE!");
            this.exceptionHWT.WDTMatterJava(330L);
            if (z2) {
                Slog.w(TAG, "SF hang!");
                if (GetSFReboot() > 3) {
                    Slog.w(TAG, "SF hang reboot time larger than 3 time, reboot device!");
                    rebootSystem("Maybe SF driver hang,reboot device.");
                } else {
                    SetSFReboot();
                }
            }
            if (z2) {
                Slog.v(TAG, "killing surfaceflinger for surfaceflinger hang");
                int[] pidsForCommands = Process.getPidsForCommands(new String[]{"/system/bin/surfaceflinger"});
                z = false;
                if (pidsForCommands[0] > 0) {
                    Process.killProcess(pidsForCommands[0]);
                }
                Slog.v(TAG, "killing surfaceflinger end");
            } else {
                z = false;
                Process.killProcess(Process.myPid());
            }
            System.exit(10);
        }
        z = false;
        synchronized (this) {
            iActivityController = this.mController;
        }
        if (!z2 && iActivityController != null) {
            Slog.i(TAG, "Reporting stuck state to activity controller");
            try {
                Binder.setDumpDisabled("Service dumps disabled due to hung system process.");
                Slog.i(TAG, "Binder.setDumpDisabled");
                if (iActivityController.systemNotResponding(str) >= 0) {
                    Slog.i(TAG, "Activity controller requested to coninue to wait");
                } else {
                    Slog.i(TAG, "Activity controller requested to reboot");
                }
            } catch (RemoteException e2) {
            }
        }
        if (Debug.isDebuggerConnected()) {
        }
        if (c < 2) {
        }
        z = false;
    }

    private void doSysRq(char c) {
        try {
            FileWriter fileWriter = new FileWriter("/proc/sysrq-trigger");
            fileWriter.write(c);
            fileWriter.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }

    public static final class OpenFdMonitor {
        private static final int FD_HIGH_WATER_MARK = 12;
        private final File mDumpDir;
        private final File mFdHighWaterMark;

        public static OpenFdMonitor create() {
            if (!Build.IS_DEBUGGABLE) {
                return null;
            }
            String str = SystemProperties.get("dalvik.vm.stack-trace-dir", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            if (str.isEmpty()) {
                return null;
            }
            try {
                return new OpenFdMonitor(new File(str), new File("/proc/self/fd/" + (Os.getrlimit(OsConstants.RLIMIT_NOFILE).rlim_cur - 12)));
            } catch (ErrnoException e) {
                Slog.w(Watchdog.TAG, "Error thrown from getrlimit(RLIMIT_NOFILE)", e);
                return null;
            }
        }

        OpenFdMonitor(File file, File file2) {
            this.mDumpDir = file;
            this.mFdHighWaterMark = file2;
        }

        private void dumpOpenDescriptors() {
            try {
                File fileCreateTempFile = File.createTempFile("anr_fd_", BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, this.mDumpDir);
                int iWaitFor = new ProcessBuilder(new String[0]).command("/system/bin/lsof", "-p", String.valueOf(Process.myPid())).redirectErrorStream(true).redirectOutput(fileCreateTempFile).start().waitFor();
                if (iWaitFor != 0) {
                    Slog.w(Watchdog.TAG, "Unable to dump open descriptors, lsof return code: " + iWaitFor);
                    fileCreateTempFile.delete();
                }
            } catch (IOException | InterruptedException e) {
                Slog.w(Watchdog.TAG, "Unable to dump open descriptors: " + e);
            }
        }

        public boolean monitor() {
            if (this.mFdHighWaterMark.exists()) {
                dumpOpenDescriptors();
                return true;
            }
            return false;
        }
    }
}
