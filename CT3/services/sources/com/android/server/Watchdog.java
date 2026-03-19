package com.android.server;

import android.app.IActivityController;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Debug;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.am.ActivityManagerService;
import com.mediatek.aee.ExceptionLog;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Watchdog extends Thread {
    static final long CHECK_INTERVAL = 30000;
    static final int COMPLETED = 0;
    static final boolean DB = false;
    static final long DEFAULT_TIMEOUT = 60000;
    public static final String[] NATIVE_STACKS_OF_INTEREST = {"/system/bin/audioserver", "/system/bin/cameraserver", "/system/bin/drmserver", "/system/bin/mediadrmserver", "/system/bin/mediaserver", "/system/bin/sdcard", "/system/bin/surfaceflinger", "media.codec", "media.extractor", "com.android.bluetooth"};
    static final int OVERDUE = 3;
    static final boolean RECORD_KERNEL_THREADS = true;
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
    int mPhonePid;
    ContentResolver mResolver;

    public interface Monitor {
        void monitor();
    }

    private native void native_dumpKernelStacks(String str);

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
        int OldTime = SystemProperties.getInt("service.sf.reboot", 0) + 1;
        if (OldTime > 9) {
            OldTime = 9;
        }
        SystemProperties.set("service.sf.reboot", String.valueOf(OldTime));
    }

    public final class HandlerChecker implements Runnable {
        private Monitor mCurrentMonitor;
        private final Handler mHandler;
        private final String mName;
        private long mStartTime;
        private final long mWaitMax;
        private final ArrayList<Monitor> mMonitors = new ArrayList<>();
        private boolean mCompleted = true;

        HandlerChecker(Handler handler, String name, long waitMaxMillis) {
            this.mHandler = handler;
            this.mName = name;
            this.mWaitMax = waitMaxMillis;
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
            long latency = SystemClock.uptimeMillis() - this.mStartTime;
            if (latency < this.mWaitMax / 2) {
                return 1;
            }
            if (latency < this.mWaitMax) {
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
            return this.mCurrentMonitor == null ? "Blocked in handler on " + this.mName + " (" + getThread().getName() + ")" : "Blocked in monitor " + this.mCurrentMonitor.getClass().getName() + " on " + this.mName + " (" + getThread().getName() + ")";
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
        public void onReceive(Context c, Intent intent) {
            if (intent.getIntExtra("nowait", 0) != 0) {
                Watchdog.this.rebootSystem("Received ACTION_REBOOT broadcast");
            } else {
                Slog.w(Watchdog.TAG, "Unsupported ACTION_REBOOT broadcast: " + intent);
            }
        }
    }

    private static final class BinderThreadMonitor implements Monitor {
        BinderThreadMonitor(BinderThreadMonitor binderThreadMonitor) {
            this();
        }

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
        this.mMonitorChecker = new HandlerChecker(FgThread.getHandler(), "foreground thread", DEFAULT_TIMEOUT);
        this.mHandlerCheckers.add(this.mMonitorChecker);
        this.mHandlerCheckers.add(new HandlerChecker(new Handler(Looper.getMainLooper()), "main thread", DEFAULT_TIMEOUT));
        this.mHandlerCheckers.add(new HandlerChecker(UiThread.getHandler(), "ui thread", DEFAULT_TIMEOUT));
        this.mHandlerCheckers.add(new HandlerChecker(IoThread.getHandler(), "i/o thread", DEFAULT_TIMEOUT));
        this.mHandlerCheckers.add(new HandlerChecker(DisplayThread.getHandler(), "display thread", DEFAULT_TIMEOUT));
        addMonitor(new BinderThreadMonitor(null));
        if (!SystemProperties.get("ro.have_aee_feature").equals("1")) {
            return;
        }
        this.exceptionHWT = new ExceptionLog();
    }

    public void init(Context context, ActivityManagerService activity) {
        this.mResolver = context.getContentResolver();
        this.mActivity = activity;
        context.registerReceiver(new RebootRequestReceiver(), new IntentFilter("android.intent.action.REBOOT"), "android.permission.REBOOT", null);
        if (this.exceptionHWT == null) {
            return;
        }
        this.exceptionHWT.WDTMatterJava(0L);
    }

    public void processStarted(String name, int pid) {
        synchronized (this) {
            if ("com.android.phone".equals(name)) {
                this.mPhonePid = pid;
            }
        }
    }

    public void setActivityController(IActivityController controller) {
        synchronized (this) {
            this.mController = controller;
        }
    }

    public void setAllowRestart(boolean allowRestart) {
        synchronized (this) {
            this.mAllowRestart = allowRestart;
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

    public void addThread(Handler thread) {
        addThread(thread, DEFAULT_TIMEOUT);
    }

    public void addThread(Handler thread, long timeoutMillis) {
        synchronized (this) {
            if (isAlive()) {
                throw new RuntimeException("Threads can't be added once the Watchdog is running");
            }
            String name = thread.getLooper().getThread().getName();
            this.mHandlerCheckers.add(new HandlerChecker(thread, name, timeoutMillis));
        }
    }

    void rebootSystem(String reason) {
        Slog.i(TAG, "Rebooting system because: " + reason);
        IPowerManager pms = ServiceManager.getService("power");
        try {
            pms.reboot(false, reason, false);
        } catch (RemoteException e) {
        }
    }

    private int evaluateCheckerCompletionLocked() {
        int state = 0;
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            HandlerChecker hc = this.mHandlerCheckers.get(i);
            state = Math.max(state, hc.getCompletionStateLocked());
        }
        return state;
    }

    private ArrayList<HandlerChecker> getBlockedCheckersLocked() {
        ArrayList<HandlerChecker> checkers = new ArrayList<>();
        for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
            HandlerChecker hc = this.mHandlerCheckers.get(i);
            if (hc.isOverdueLocked()) {
                checkers.add(hc);
            }
        }
        return checkers;
    }

    private String describeCheckersLocked(ArrayList<HandlerChecker> checkers) {
        StringBuilder builder = new StringBuilder(128);
        for (int i = 0; i < checkers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(checkers.get(i).describeBlockedStateLocked());
        }
        return builder.toString();
    }

    @Override
    public void run() {
        IActivityController controller;
        boolean waitedHalf = false;
        while (true) {
            boolean mSFHang = false;
            if (this.exceptionHWT != null && !waitedHalf) {
                this.exceptionHWT.WDTMatterJava(300L);
            }
            int debuggerWasConnected = 0;
            Slog.w(TAG, "SWT Watchdog before synchronized:" + SystemClock.uptimeMillis());
            synchronized (this) {
                Slog.w(TAG, "SWT Watchdog after synchronized:" + SystemClock.uptimeMillis());
                for (int i = 0; i < this.mHandlerCheckers.size(); i++) {
                    HandlerChecker hc = this.mHandlerCheckers.get(i);
                    hc.scheduleCheckLocked();
                }
                long start = SystemClock.uptimeMillis();
                for (long timeout = CHECK_INTERVAL; timeout > 0; timeout = CHECK_INTERVAL - (SystemClock.uptimeMillis() - start)) {
                    if (Debug.isDebuggerConnected()) {
                        debuggerWasConnected = 2;
                    }
                    try {
                        wait(timeout);
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, e);
                    }
                    if (Debug.isDebuggerConnected()) {
                        debuggerWasConnected = 2;
                    }
                }
                long SFHangTime = GetSFStatus();
                Slog.w(TAG, "**Get SF Time **" + SFHangTime);
                if (SFHangTime > 40000) {
                    Slog.v(TAG, "**SF hang Time **" + SFHangTime);
                    mSFHang = true;
                } else {
                    int waitState = evaluateCheckerCompletionLocked();
                    if (waitState == 0) {
                        waitedHalf = false;
                    } else if (waitState != 1) {
                        if (waitState == 2) {
                            if (!waitedHalf) {
                                if (this.exceptionHWT != null) {
                                    this.exceptionHWT.WDTMatterJava(360L);
                                }
                                ArrayList<Integer> pids = new ArrayList<>();
                                pids.add(Integer.valueOf(Process.myPid()));
                                ActivityManagerService.dumpStackTraces(true, pids, (ProcessCpuTracker) null, (SparseArray<Boolean>) null, NATIVE_STACKS_OF_INTEREST);
                                waitedHalf = true;
                            }
                        }
                    }
                }
                ArrayList<HandlerChecker> blockedCheckers = getBlockedCheckersLocked();
                String subject = describeCheckersLocked(blockedCheckers);
                boolean allowRestart = this.mAllowRestart;
                Slog.e(TAG, "**SWT happen **" + subject);
                if (mSFHang && subject.isEmpty()) {
                    subject = "surfaceflinger  hang.";
                }
                EventLog.writeEvent(EventLogTags.WATCHDOG, subject);
                if (this.exceptionHWT != null) {
                    this.exceptionHWT.WDTMatterJava(420L);
                }
                ArrayList<Integer> pids2 = new ArrayList<>();
                pids2.add(Integer.valueOf(Process.myPid()));
                if (this.mPhonePid > 0) {
                    pids2.add(Integer.valueOf(this.mPhonePid));
                }
                ActivityManagerService.dumpStackTraces(!waitedHalf, pids2, (ProcessCpuTracker) null, (SparseArray<Boolean>) null, NATIVE_STACKS_OF_INTEREST);
                SystemClock.sleep(2000L);
                dumpKernelStackTraces();
                doSysRq('w');
                doSysRq('l');
                Slog.v(TAG, "** save all info before killnig system server **");
                this.mActivity.addErrorToDropBox("watchdog", null, "system_server", null, null, subject, null, null, null);
                synchronized (this) {
                    controller = this.mController;
                }
                if (!mSFHang && controller != null) {
                    Slog.i(TAG, "Reporting stuck state to activity controller");
                    try {
                        Binder.setDumpDisabled("Service dumps disabled due to hung system process.");
                        Slog.i(TAG, "Binder.setDumpDisabled");
                        int res = controller.systemNotResponding(subject);
                        if (res >= 0) {
                            Slog.i(TAG, "Activity controller requested to coninue to wait");
                            waitedHalf = false;
                        } else {
                            Slog.i(TAG, "Activity controller requested to reboot");
                        }
                    } catch (RemoteException e2) {
                    }
                }
                if (Debug.isDebuggerConnected()) {
                    debuggerWasConnected = 2;
                }
                if (debuggerWasConnected >= 2) {
                    Slog.w(TAG, "Debugger connected: Watchdog is *not* killing the system process");
                } else if (debuggerWasConnected > 0) {
                    Slog.w(TAG, "Debugger was connected: Watchdog is *not* killing the system process");
                } else if (!allowRestart) {
                    Slog.w(TAG, "Restart not allowed: Watchdog is *not* killing the system process");
                } else {
                    Slog.w(TAG, "*** WATCHDOG KILLING SYSTEM PROCESS: " + subject);
                    for (int i2 = 0; i2 < blockedCheckers.size(); i2++) {
                        Slog.w(TAG, blockedCheckers.get(i2).getName() + " stack trace:");
                        StackTraceElement[] stackTrace = blockedCheckers.get(i2).getThread().getStackTrace();
                        for (StackTraceElement element : stackTrace) {
                            Slog.w(TAG, "    at " + element);
                        }
                    }
                    Slog.w(TAG, "*** GOODBYE!");
                    if (mSFHang) {
                        Slog.w(TAG, "SF hang!");
                        if (GetSFReboot() > 3) {
                            Slog.w(TAG, "SF hang reboot time larger than 3 time, reboot device!");
                            rebootSystem("Maybe SF driver hang,reboot device.");
                        } else {
                            SetSFReboot();
                        }
                    }
                    if (mSFHang) {
                        Slog.v(TAG, "killing surfaceflinger for surfaceflinger hang");
                        String[] sf = {"/system/bin/surfaceflinger"};
                        int[] pid_sf = Process.getPidsForCommands(sf);
                        if (pid_sf[0] > 0) {
                            Process.killProcess(pid_sf[0]);
                        }
                        Slog.v(TAG, "killing surfaceflinger end");
                    } else {
                        Process.killProcess(Process.myPid());
                    }
                    System.exit(10);
                }
                waitedHalf = false;
            }
        }
    }

    private void doSysRq(char c) {
        try {
            FileWriter sysrq_trigger = new FileWriter("/proc/sysrq-trigger");
            sysrq_trigger.write(c);
            sysrq_trigger.close();
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write to /proc/sysrq-trigger", e);
        }
    }

    private File dumpKernelStackTraces() {
        String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", (String) null);
        if (tracesPath == null || tracesPath.length() == 0) {
            return null;
        }
        native_dumpKernelStacks(tracesPath);
        return new File(tracesPath);
    }
}
