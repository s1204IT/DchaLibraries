package com.android.commands.monkey;

import android.app.ActivityManagerNative;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Environment;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.UserHandle;
import android.view.IWindowManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Monkey {
    private static final int DEBUG_ALLOW_ANY_RESTARTS = 0;
    private static final int DEBUG_ALLOW_ANY_STARTS = 0;
    private static final File TOMBSTONES_PATH = new File("/data/tombstones");
    public static Intent currentIntent;
    public static String currentPackage;
    private boolean mAbort;
    private IActivityManager mAm;
    private String[] mArgs;
    private String mCurArgData;
    MonkeyEventSource mEventSource;
    private boolean mGenerateHprof;
    private boolean mIgnoreCrashes;
    private boolean mIgnoreNativeCrashes;
    private boolean mIgnoreSecurityExceptions;
    private boolean mIgnoreTimeouts;
    private boolean mKillProcessAfterError;
    private boolean mMonitorNativeCrashes;
    private int mNextArg;
    private String mPkgBlacklistFile;
    private String mPkgWhitelistFile;
    private IPackageManager mPm;
    private String mReportProcessName;
    private boolean mSendNoEvents;
    private int mVerbose;
    private IWindowManager mWm;
    private boolean mCountEvents = true;
    private boolean mRequestAnrTraces = false;
    private boolean mRequestDumpsysMemInfo = false;
    private boolean mRequestAnrBugreport = false;
    private boolean mRequestWatchdogBugreport = false;
    private boolean mWatchdogWaiting = false;
    private boolean mRequestAppCrashBugreport = false;
    private boolean mGetPeriodicBugreport = false;
    private boolean mRequestPeriodicBugreport = false;
    private long mBugreportFrequency = 10;
    private boolean mRequestProcRank = false;
    private HashSet<String> mValidPackages = new HashSet<>();
    private HashSet<String> mInvalidPackages = new HashSet<>();
    private ArrayList<String> mMainCategories = new ArrayList<>();
    private ArrayList<ComponentName> mMainApps = new ArrayList<>();
    long mThrottle = 0;
    boolean mRandomizeThrottle = false;
    int mCount = 1000;
    long mSeed = 0;
    Random mRandom = null;
    long mDroppedKeyEvents = 0;
    long mDroppedPointerEvents = 0;
    long mDroppedTrackballEvents = 0;
    long mDroppedFlipEvents = 0;
    long mDroppedRotationEvents = 0;
    long mProfileWaitTime = 5000;
    long mDeviceSleepTime = 30000;
    boolean mRandomizeScript = false;
    boolean mScriptLog = false;
    private boolean mRequestBugreport = false;
    private String mSetupFileName = null;
    private ArrayList<String> mScriptFileNames = new ArrayList<>();
    private int mServerPort = -1;
    private HashSet<String> mTombstones = null;
    float[] mFactors = new float[11];
    private MonkeyNetworkMonitor mNetworkMonitor = new MonkeyNetworkMonitor();

    private boolean checkEnteringPackage(String pkg) {
        if (this.mInvalidPackages.size() > 0) {
            if (this.mInvalidPackages.contains(pkg)) {
                return false;
            }
        } else if (this.mValidPackages.size() > 0 && !this.mValidPackages.contains(pkg)) {
            return false;
        }
        return true;
    }

    private class ActivityController extends IActivityController.Stub {
        private ActivityController() {
        }

        public boolean activityStarting(Intent intent, String pkg) {
            boolean allow = Monkey.this.checkEnteringPackage(pkg);
            if (Monkey.this.mVerbose > 0) {
                StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
                System.out.println("    // " + (allow ? "Allowing" : "Rejecting") + " start of " + intent + " in package " + pkg);
                StrictMode.setThreadPolicy(savedPolicy);
            }
            Monkey.currentPackage = pkg;
            Monkey.currentIntent = intent;
            return allow;
        }

        public boolean activityResuming(String pkg) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            System.out.println("    // activityResuming(" + pkg + ")");
            boolean allow = Monkey.this.checkEnteringPackage(pkg);
            if (!allow && Monkey.this.mVerbose > 0) {
                System.out.println("    // " + (allow ? "Allowing" : "Rejecting") + " resume of package " + pkg);
            }
            Monkey.currentPackage = pkg;
            StrictMode.setThreadPolicy(savedPolicy);
            return allow;
        }

        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            System.err.println("// CRASH: " + processName + " (pid " + pid + ")");
            System.err.println("// Short Msg: " + shortMsg);
            System.err.println("// Long Msg: " + longMsg);
            System.err.println("// Build Label: " + Build.FINGERPRINT);
            System.err.println("// Build Changelist: " + Build.VERSION.INCREMENTAL);
            System.err.println("// Build Time: " + Build.TIME);
            System.err.println("// " + stackTrace.replace("\n", "\n// "));
            StrictMode.setThreadPolicy(savedPolicy);
            if (Monkey.this.mIgnoreCrashes && !Monkey.this.mRequestBugreport) {
                return false;
            }
            synchronized (Monkey.this) {
                if (!Monkey.this.mIgnoreCrashes) {
                    Monkey.this.mAbort = true;
                }
                if (Monkey.this.mRequestBugreport) {
                    Monkey.this.mRequestAppCrashBugreport = true;
                    Monkey.this.mReportProcessName = processName;
                }
            }
            return Monkey.this.mKillProcessAfterError ? false : true;
        }

        public int appEarlyNotResponding(String processName, int pid, String annotation) {
            return 0;
        }

        public int appNotResponding(String processName, int pid, String processStats) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            System.err.println("// NOT RESPONDING: " + processName + " (pid " + pid + ")");
            System.err.println(processStats);
            StrictMode.setThreadPolicy(savedPolicy);
            synchronized (Monkey.this) {
                Monkey.this.mRequestAnrTraces = true;
                Monkey.this.mRequestDumpsysMemInfo = true;
                Monkey.this.mRequestProcRank = true;
                if (Monkey.this.mRequestBugreport) {
                    Monkey.this.mRequestAnrBugreport = true;
                    Monkey.this.mReportProcessName = processName;
                }
            }
            if (!Monkey.this.mIgnoreTimeouts) {
                synchronized (Monkey.this) {
                    Monkey.this.mAbort = true;
                }
            }
            return Monkey.this.mKillProcessAfterError ? -1 : 1;
        }

        public int systemNotResponding(String message) {
            StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
            System.err.println("// WATCHDOG: " + message);
            StrictMode.setThreadPolicy(savedPolicy);
            synchronized (Monkey.this) {
                if (!Monkey.this.mIgnoreCrashes) {
                    Monkey.this.mAbort = true;
                }
                if (Monkey.this.mRequestBugreport) {
                    Monkey.this.mRequestWatchdogBugreport = true;
                }
                Monkey.this.mWatchdogWaiting = true;
            }
            synchronized (Monkey.this) {
                while (Monkey.this.mWatchdogWaiting) {
                    try {
                        Monkey.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return Monkey.this.mKillProcessAfterError ? -1 : 1;
        }
    }

    private void reportProcRank() {
        commandLineReport("procrank", "procrank");
    }

    private void reportAnrTraces() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
        }
        commandLineReport("anr traces", "cat /data/anr/traces.txt");
    }

    private void reportDumpsysMemInfo() {
        commandLineReport("meminfo", "dumpsys meminfo");
    }

    private void commandLineReport(String reportName, String command) {
        System.err.println(reportName + ":");
        Runtime.getRuntime();
        Writer logOutput = null;
        try {
            Process p = Runtime.getRuntime().exec(command);
            if (this.mRequestBugreport) {
                Writer logOutput2 = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), reportName), true));
                logOutput = logOutput2;
            }
            InputStream inStream = p.getInputStream();
            InputStreamReader inReader = new InputStreamReader(inStream);
            BufferedReader inBuffer = new BufferedReader(inReader);
            while (true) {
                String s = inBuffer.readLine();
                if (s == null) {
                    break;
                }
                if (this.mRequestBugreport) {
                    logOutput.write(s);
                    logOutput.write("\n");
                } else {
                    System.err.println(s);
                }
            }
            int status = p.waitFor();
            System.err.println("// " + reportName + " status was " + status);
            if (logOutput != null) {
                logOutput.close();
            }
        } catch (Exception e) {
            System.err.println("// Exception from " + reportName + ":");
            System.err.println(e.toString());
        }
    }

    private void writeScriptLog(int count) {
        try {
            Writer output = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), "scriptlog.txt"), true));
            output.write("iteration: " + count + " time: " + MonkeyUtils.toCalendarTime(System.currentTimeMillis()) + "\n");
            output.close();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }

    private void getBugreport(String reportName) {
        String bugreportName = (reportName + MonkeyUtils.toCalendarTime(System.currentTimeMillis())).replaceAll("[ ,:]", "_");
        commandLineReport(bugreportName + ".txt", "bugreport");
    }

    public static void main(String[] args) {
        Process.setArgV0("com.android.commands.monkey");
        int resultCode = new Monkey().run(args);
        System.exit(resultCode);
    }

    private int run(String[] args) {
        for (String s : args) {
            if ("--wait-dbg".equals(s)) {
                Debug.waitForDebugger();
            }
        }
        this.mVerbose = 0;
        this.mCount = 1000;
        this.mSeed = 0L;
        this.mThrottle = 0L;
        this.mArgs = args;
        this.mNextArg = 0;
        for (int i = 0; i < 11; i++) {
            this.mFactors[i] = 1.0f;
        }
        if (!processOptions() || !loadPackageLists()) {
            return -1;
        }
        if (this.mMainCategories.size() == 0) {
            this.mMainCategories.add("android.intent.category.LAUNCHER");
            this.mMainCategories.add("android.intent.category.MONKEY");
        }
        if (this.mSeed == 0) {
            this.mSeed = System.currentTimeMillis() + ((long) System.identityHashCode(this));
        }
        if (this.mVerbose > 0) {
            System.out.println(":Monkey: seed=" + this.mSeed + " count=" + this.mCount);
            if (this.mValidPackages.size() > 0) {
                Iterator<String> it = this.mValidPackages.iterator();
                while (it.hasNext()) {
                    System.out.println(":AllowPackage: " + it.next());
                }
            }
            if (this.mInvalidPackages.size() > 0) {
                Iterator<String> it2 = this.mInvalidPackages.iterator();
                while (it2.hasNext()) {
                    System.out.println(":DisallowPackage: " + it2.next());
                }
            }
            if (this.mMainCategories.size() != 0) {
                Iterator<String> it3 = this.mMainCategories.iterator();
                while (it3.hasNext()) {
                    System.out.println(":IncludeCategory: " + it3.next());
                }
            }
        }
        if (!checkInternalConfiguration()) {
            return -2;
        }
        if (!getSystemInterfaces()) {
            return -3;
        }
        if (!getMainApps()) {
            return -4;
        }
        this.mRandom = new Random(this.mSeed);
        if (this.mScriptFileNames != null && this.mScriptFileNames.size() == 1) {
            this.mEventSource = new MonkeySourceScript(this.mRandom, this.mScriptFileNames.get(0), this.mThrottle, this.mRandomizeThrottle, this.mProfileWaitTime, this.mDeviceSleepTime);
            this.mEventSource.setVerbose(this.mVerbose);
            this.mCountEvents = false;
        } else if (this.mScriptFileNames != null && this.mScriptFileNames.size() > 1) {
            if (this.mSetupFileName != null) {
                this.mEventSource = new MonkeySourceRandomScript(this.mSetupFileName, this.mScriptFileNames, this.mThrottle, this.mRandomizeThrottle, this.mRandom, this.mProfileWaitTime, this.mDeviceSleepTime, this.mRandomizeScript);
                this.mCount++;
            } else {
                this.mEventSource = new MonkeySourceRandomScript(this.mScriptFileNames, this.mThrottle, this.mRandomizeThrottle, this.mRandom, this.mProfileWaitTime, this.mDeviceSleepTime, this.mRandomizeScript);
            }
            this.mEventSource.setVerbose(this.mVerbose);
            this.mCountEvents = false;
        } else if (this.mServerPort != -1) {
            try {
                this.mEventSource = new MonkeySourceNetwork(this.mServerPort);
                this.mCount = Integer.MAX_VALUE;
            } catch (IOException e) {
                System.out.println("Error binding to network socket.");
                return -5;
            }
        } else {
            if (this.mVerbose >= 2) {
                System.out.println("// Seeded: " + this.mSeed);
            }
            this.mEventSource = new MonkeySourceRandom(this.mRandom, this.mMainApps, this.mThrottle, this.mRandomizeThrottle);
            this.mEventSource.setVerbose(this.mVerbose);
            for (int i2 = 0; i2 < 11; i2++) {
                if (this.mFactors[i2] <= 0.0f) {
                    ((MonkeySourceRandom) this.mEventSource).setFactors(i2, this.mFactors[i2]);
                }
            }
            ((MonkeySourceRandom) this.mEventSource).generateActivity();
        }
        if (!this.mEventSource.validate()) {
            return -5;
        }
        if (this.mGenerateHprof) {
            signalPersistentProcesses();
        }
        this.mNetworkMonitor.start();
        try {
            int crashedAtCycle = runMonkeyCycles();
            new MonkeyRotationEvent(0, false).injectEvent(this.mWm, this.mAm, this.mVerbose);
            this.mNetworkMonitor.stop();
            synchronized (this) {
                if (this.mRequestAnrTraces) {
                    reportAnrTraces();
                    this.mRequestAnrTraces = false;
                }
                if (this.mRequestAnrBugreport) {
                    System.out.println("Print the anr report");
                    getBugreport("anr_" + this.mReportProcessName + "_");
                    this.mRequestAnrBugreport = false;
                }
                if (this.mRequestWatchdogBugreport) {
                    System.out.println("Print the watchdog report");
                    getBugreport("anr_watchdog_");
                    this.mRequestWatchdogBugreport = false;
                }
                if (this.mRequestAppCrashBugreport) {
                    getBugreport("app_crash" + this.mReportProcessName + "_");
                    this.mRequestAppCrashBugreport = false;
                }
                if (this.mRequestDumpsysMemInfo) {
                    reportDumpsysMemInfo();
                    this.mRequestDumpsysMemInfo = false;
                }
                if (this.mRequestPeriodicBugreport) {
                    getBugreport("Bugreport_");
                    this.mRequestPeriodicBugreport = false;
                }
                if (this.mWatchdogWaiting) {
                    this.mWatchdogWaiting = false;
                    notifyAll();
                }
            }
            if (this.mGenerateHprof) {
                signalPersistentProcesses();
                if (this.mVerbose > 0) {
                    System.out.println("// Generated profiling reports in /data/misc");
                }
            }
            try {
                this.mAm.setActivityController((IActivityController) null);
                this.mNetworkMonitor.unregister(this.mAm);
            } catch (RemoteException e2) {
                if (crashedAtCycle >= this.mCount) {
                    crashedAtCycle = this.mCount - 1;
                }
            }
            if (this.mVerbose > 0) {
                System.out.print(":Dropped: keys=");
                System.out.print(this.mDroppedKeyEvents);
                System.out.print(" pointers=");
                System.out.print(this.mDroppedPointerEvents);
                System.out.print(" trackballs=");
                System.out.print(this.mDroppedTrackballEvents);
                System.out.print(" flips=");
                System.out.print(this.mDroppedFlipEvents);
                System.out.print(" rotations=");
                System.out.println(this.mDroppedRotationEvents);
            }
            this.mNetworkMonitor.dump();
            if (crashedAtCycle < this.mCount - 1) {
                System.err.println("** System appears to have crashed at event " + crashedAtCycle + " of " + this.mCount + " using seed " + this.mSeed);
                return crashedAtCycle;
            }
            if (this.mVerbose > 0) {
                System.out.println("// Monkey finished");
            }
            return 0;
        } catch (Throwable th) {
            new MonkeyRotationEvent(0, false).injectEvent(this.mWm, this.mAm, this.mVerbose);
            throw th;
        }
    }

    private boolean processOptions() {
        if (this.mArgs.length < 1) {
            showUsage();
            return false;
        }
        while (true) {
            try {
                String opt = nextOption();
                if (opt != null) {
                    if (opt.equals("-s")) {
                        this.mSeed = nextOptionLong("Seed");
                    } else if (opt.equals("-p")) {
                        this.mValidPackages.add(nextOptionData());
                    } else if (opt.equals("-c")) {
                        this.mMainCategories.add(nextOptionData());
                    } else if (opt.equals("-v")) {
                        this.mVerbose++;
                    } else if (opt.equals("--ignore-crashes")) {
                        this.mIgnoreCrashes = true;
                    } else if (opt.equals("--ignore-timeouts")) {
                        this.mIgnoreTimeouts = true;
                    } else if (opt.equals("--ignore-security-exceptions")) {
                        this.mIgnoreSecurityExceptions = true;
                    } else if (opt.equals("--monitor-native-crashes")) {
                        this.mMonitorNativeCrashes = true;
                    } else if (opt.equals("--ignore-native-crashes")) {
                        this.mIgnoreNativeCrashes = true;
                    } else if (opt.equals("--kill-process-after-error")) {
                        this.mKillProcessAfterError = true;
                    } else if (opt.equals("--hprof")) {
                        this.mGenerateHprof = true;
                    } else if (opt.equals("--pct-touch")) {
                        this.mFactors[0] = -nextOptionLong("touch events percentage");
                    } else if (opt.equals("--pct-motion")) {
                        this.mFactors[1] = -nextOptionLong("motion events percentage");
                    } else if (opt.equals("--pct-trackball")) {
                        this.mFactors[3] = -nextOptionLong("trackball events percentage");
                    } else if (opt.equals("--pct-rotation")) {
                        this.mFactors[4] = -nextOptionLong("screen rotation events percentage");
                    } else if (opt.equals("--pct-syskeys")) {
                        this.mFactors[7] = -nextOptionLong("system (key) operations percentage");
                    } else if (opt.equals("--pct-nav")) {
                        this.mFactors[5] = -nextOptionLong("nav events percentage");
                    } else if (opt.equals("--pct-majornav")) {
                        this.mFactors[6] = -nextOptionLong("major nav events percentage");
                    } else if (opt.equals("--pct-appswitch")) {
                        this.mFactors[8] = -nextOptionLong("app switch events percentage");
                    } else if (opt.equals("--pct-flip")) {
                        this.mFactors[9] = -nextOptionLong("keyboard flip percentage");
                    } else if (opt.equals("--pct-anyevent")) {
                        this.mFactors[10] = -nextOptionLong("any events percentage");
                    } else if (opt.equals("--pct-pinchzoom")) {
                        this.mFactors[2] = -nextOptionLong("pinch zoom events percentage");
                    } else if (opt.equals("--pkg-blacklist-file")) {
                        this.mPkgBlacklistFile = nextOptionData();
                    } else if (opt.equals("--pkg-whitelist-file")) {
                        this.mPkgWhitelistFile = nextOptionData();
                    } else if (opt.equals("--throttle")) {
                        this.mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                    } else if (opt.equals("--randomize-throttle")) {
                        this.mRandomizeThrottle = true;
                    } else if (opt.equals("--wait-dbg")) {
                        continue;
                    } else if (opt.equals("--dbg-no-events")) {
                        this.mSendNoEvents = true;
                    } else if (opt.equals("--port")) {
                        this.mServerPort = (int) nextOptionLong("Server port to listen on for commands");
                    } else if (opt.equals("--setup")) {
                        this.mSetupFileName = nextOptionData();
                    } else if (opt.equals("-f")) {
                        this.mScriptFileNames.add(nextOptionData());
                    } else if (opt.equals("--profile-wait")) {
                        this.mProfileWaitTime = nextOptionLong("Profile delay (in milliseconds) to wait between user action");
                    } else if (opt.equals("--device-sleep-time")) {
                        this.mDeviceSleepTime = nextOptionLong("Device sleep time(in milliseconds)");
                    } else if (opt.equals("--randomize-script")) {
                        this.mRandomizeScript = true;
                    } else if (opt.equals("--script-log")) {
                        this.mScriptLog = true;
                    } else if (opt.equals("--bugreport")) {
                        this.mRequestBugreport = true;
                    } else if (opt.equals("--periodic-bugreport")) {
                        this.mGetPeriodicBugreport = true;
                        this.mBugreportFrequency = nextOptionLong("Number of iterations");
                    } else {
                        if (opt.equals("-h")) {
                            showUsage();
                            return false;
                        }
                        System.err.println("** Error: Unknown option: " + opt);
                        showUsage();
                        return false;
                    }
                } else {
                    if (this.mServerPort == -1) {
                        String countStr = nextArg();
                        if (countStr == null) {
                            System.err.println("** Error: Count not specified");
                            showUsage();
                            return false;
                        }
                        try {
                            this.mCount = Integer.parseInt(countStr);
                        } catch (NumberFormatException e) {
                            System.err.println("** Error: Count is not a number");
                            showUsage();
                            return false;
                        }
                    }
                    return true;
                }
            } catch (RuntimeException ex) {
                System.err.println("** Error: " + ex.toString());
                showUsage();
                return false;
            }
        }
    }

    private static boolean loadPackageListFromFile(String fileName, HashSet<String> list) throws Throwable {
        boolean z;
        BufferedReader reader = null;
        try {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(fileName));
                while (true) {
                    try {
                        String s = reader2.readLine();
                        if (s == null) {
                            break;
                        }
                        String s2 = s.trim();
                        if (s2.length() > 0 && !s2.startsWith("#")) {
                            list.add(s2);
                        }
                    } catch (IOException e) {
                        ioe = e;
                        reader = reader2;
                        System.err.println(ioe);
                        z = false;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException ioe) {
                                System.err.println(ioe);
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException ioe2) {
                                System.err.println(ioe2);
                            }
                        }
                        throw th;
                    }
                }
                if (reader2 != null) {
                    try {
                        reader2.close();
                    } catch (IOException ioe3) {
                        System.err.println(ioe3);
                    }
                }
                z = true;
                reader = reader2;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e2) {
            ioe = e2;
        }
        return z;
    }

    private boolean loadPackageLists() {
        if ((this.mPkgWhitelistFile != null || this.mValidPackages.size() > 0) && this.mPkgBlacklistFile != null) {
            System.err.println("** Error: you can not specify a package blacklist together with a whitelist or individual packages (via -p).");
            return false;
        }
        if (this.mPkgWhitelistFile == null || loadPackageListFromFile(this.mPkgWhitelistFile, this.mValidPackages)) {
            return this.mPkgBlacklistFile == null || loadPackageListFromFile(this.mPkgBlacklistFile, this.mInvalidPackages);
        }
        return false;
    }

    private boolean checkInternalConfiguration() {
        return true;
    }

    private boolean getSystemInterfaces() {
        this.mAm = ActivityManagerNative.getDefault();
        if (this.mAm == null) {
            System.err.println("** Error: Unable to connect to activity manager; is the system running?");
            return false;
        }
        this.mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        if (this.mWm == null) {
            System.err.println("** Error: Unable to connect to window manager; is the system running?");
            return false;
        }
        this.mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (this.mPm == null) {
            System.err.println("** Error: Unable to connect to package manager; is the system running?");
            return false;
        }
        try {
            this.mAm.setActivityController(new ActivityController());
            this.mNetworkMonitor.register(this.mAm);
            return true;
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
            return false;
        }
    }

    private boolean getMainApps() {
        try {
            int N = this.mMainCategories.size();
            for (int i = 0; i < N; i++) {
                Intent intent = new Intent("android.intent.action.MAIN");
                String category = this.mMainCategories.get(i);
                if (category.length() > 0) {
                    intent.addCategory(category);
                }
                List<ResolveInfo> mainApps = this.mPm.queryIntentActivities(intent, (String) null, 0, UserHandle.myUserId());
                if (mainApps == null || mainApps.size() == 0) {
                    System.err.println("// Warning: no activities found for category " + category);
                } else {
                    if (this.mVerbose >= 2) {
                        System.out.println("// Selecting main activities from category " + category);
                    }
                    int NA = mainApps.size();
                    for (int a = 0; a < NA; a++) {
                        ResolveInfo r = mainApps.get(a);
                        String packageName = r.activityInfo.applicationInfo.packageName;
                        if (checkEnteringPackage(packageName)) {
                            if (this.mVerbose >= 2) {
                                System.out.println("//   + Using main activity " + r.activityInfo.name + " (from package " + packageName + ")");
                            }
                            this.mMainApps.add(new ComponentName(packageName, r.activityInfo.name));
                        } else if (this.mVerbose >= 3) {
                            System.out.println("//   - NOT USING main activity " + r.activityInfo.name + " (from package " + packageName + ")");
                        }
                    }
                }
            }
            if (this.mMainApps.size() == 0) {
                System.out.println("** No activities found to run, monkey aborted.");
                return false;
            }
            return true;
        } catch (RemoteException e) {
            System.err.println("** Failed talking with package manager!");
            return false;
        }
    }

    private int runMonkeyCycles() {
        int eventCounter = 0;
        int cycleCounter = 0;
        boolean shouldReportAnrTraces = false;
        boolean shouldReportDumpsysMemInfo = false;
        boolean shouldAbort = false;
        boolean systemCrashed = false;
        while (!systemCrashed && cycleCounter < this.mCount) {
            synchronized (this) {
                if (this.mRequestProcRank) {
                    reportProcRank();
                    this.mRequestProcRank = false;
                }
                if (this.mRequestAnrTraces) {
                    this.mRequestAnrTraces = false;
                    shouldReportAnrTraces = true;
                }
                if (this.mRequestAnrBugreport) {
                    getBugreport("anr_" + this.mReportProcessName + "_");
                    this.mRequestAnrBugreport = false;
                }
                if (this.mRequestWatchdogBugreport) {
                    System.out.println("Print the watchdog report");
                    getBugreport("anr_watchdog_");
                    this.mRequestWatchdogBugreport = false;
                }
                if (this.mRequestAppCrashBugreport) {
                    getBugreport("app_crash" + this.mReportProcessName + "_");
                    this.mRequestAppCrashBugreport = false;
                }
                if (this.mRequestPeriodicBugreport) {
                    getBugreport("Bugreport_");
                    this.mRequestPeriodicBugreport = false;
                }
                if (this.mRequestDumpsysMemInfo) {
                    this.mRequestDumpsysMemInfo = false;
                    shouldReportDumpsysMemInfo = true;
                }
                if (this.mMonitorNativeCrashes && checkNativeCrashes() && eventCounter > 0) {
                    System.out.println("** New native crash detected.");
                    if (this.mRequestBugreport) {
                        getBugreport("native_crash_");
                    }
                    this.mAbort = this.mAbort || !this.mIgnoreNativeCrashes || this.mKillProcessAfterError;
                }
                if (this.mAbort) {
                    shouldAbort = true;
                }
                if (this.mWatchdogWaiting) {
                    this.mWatchdogWaiting = false;
                    notifyAll();
                }
            }
            if (shouldReportAnrTraces) {
                shouldReportAnrTraces = false;
                reportAnrTraces();
            }
            if (shouldReportDumpsysMemInfo) {
                shouldReportDumpsysMemInfo = false;
                reportDumpsysMemInfo();
            }
            if (shouldAbort) {
                System.out.println("** Monkey aborted due to error.");
                System.out.println("Events injected: " + eventCounter);
                break;
            }
            if (this.mSendNoEvents) {
                eventCounter++;
                cycleCounter++;
            } else {
                if (this.mVerbose > 0 && eventCounter % 100 == 0 && eventCounter != 0) {
                    String calendarTime = MonkeyUtils.toCalendarTime(System.currentTimeMillis());
                    long systemUpTime = SystemClock.elapsedRealtime();
                    System.out.println("    //[calendar_time:" + calendarTime + " system_uptime:" + systemUpTime + "]");
                    System.out.println("    // Sending event #" + eventCounter);
                }
                MonkeyEvent ev = this.mEventSource.getNextEvent();
                if (ev != null) {
                    int injectCode = ev.injectEvent(this.mWm, this.mAm, this.mVerbose);
                    if (injectCode == 0) {
                        System.out.println("    // Injection Failed");
                        if (ev instanceof MonkeyKeyEvent) {
                            this.mDroppedKeyEvents++;
                        } else if (ev instanceof MonkeyMotionEvent) {
                            this.mDroppedPointerEvents++;
                        } else if (ev instanceof MonkeyFlipEvent) {
                            this.mDroppedFlipEvents++;
                        } else if (ev instanceof MonkeyRotationEvent) {
                            this.mDroppedRotationEvents++;
                        }
                    } else if (injectCode == -1) {
                        systemCrashed = true;
                        System.err.println("** Error: RemoteException while injecting event.");
                    } else if (injectCode == -2) {
                        systemCrashed = !this.mIgnoreSecurityExceptions;
                        if (systemCrashed) {
                            System.err.println("** Error: SecurityException while injecting event.");
                        }
                    }
                    if (!(ev instanceof MonkeyThrottleEvent)) {
                        eventCounter++;
                        if (this.mCountEvents) {
                            cycleCounter++;
                        }
                    }
                } else {
                    if (this.mCountEvents) {
                        break;
                    }
                    cycleCounter++;
                    writeScriptLog(cycleCounter);
                    if (this.mGetPeriodicBugreport && ((long) cycleCounter) % this.mBugreportFrequency == 0) {
                        this.mRequestPeriodicBugreport = true;
                    }
                }
            }
        }
        System.out.println("Events injected: " + eventCounter);
        return eventCounter;
    }

    private void signalPersistentProcesses() {
        try {
            this.mAm.signalPersistentProcesses(10);
            synchronized (this) {
                wait(2000L);
            }
        } catch (RemoteException e) {
            System.err.println("** Failed talking with activity manager!");
        } catch (InterruptedException e2) {
        }
    }

    private boolean checkNativeCrashes() {
        String[] tombstones = TOMBSTONES_PATH.list();
        if (tombstones == null || tombstones.length == 0) {
            this.mTombstones = null;
        } else {
            HashSet<String> newStones = new HashSet<>();
            for (String x : tombstones) {
                newStones.add(x);
            }
            result = this.mTombstones == null || !this.mTombstones.containsAll(newStones);
            this.mTombstones = newStones;
        }
        return result;
    }

    private String nextOption() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String arg = this.mArgs[this.mNextArg];
        if (!arg.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (arg.equals("--")) {
            return null;
        }
        if (arg.length() > 1 && arg.charAt(1) != '-') {
            if (arg.length() > 2) {
                this.mCurArgData = arg.substring(2);
                return arg.substring(0, 2);
            }
            this.mCurArgData = null;
            return arg;
        }
        this.mCurArgData = null;
        return arg;
    }

    private String nextOptionData() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private long nextOptionLong(String opt) {
        try {
            long result = Long.parseLong(nextOptionData());
            return result;
        } catch (NumberFormatException e) {
            System.err.println("** Error: " + opt + " is not a number");
            throw e;
        }
    }

    private String nextArg() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        this.mNextArg++;
        return str;
    }

    private void showUsage() {
        StringBuffer usage = new StringBuffer();
        usage.append("usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]\n");
        usage.append("              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]\n");
        usage.append("              [--ignore-crashes] [--ignore-timeouts]\n");
        usage.append("              [--ignore-security-exceptions]\n");
        usage.append("              [--monitor-native-crashes] [--ignore-native-crashes]\n");
        usage.append("              [--kill-process-after-error] [--hprof]\n");
        usage.append("              [--pct-touch PERCENT] [--pct-motion PERCENT]\n");
        usage.append("              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]\n");
        usage.append("              [--pct-nav PERCENT] [--pct-majornav PERCENT]\n");
        usage.append("              [--pct-appswitch PERCENT] [--pct-flip PERCENT]\n");
        usage.append("              [--pct-anyevent PERCENT] [--pct-pinchzoom PERCENT]\n");
        usage.append("              [--pkg-blacklist-file PACKAGE_BLACKLIST_FILE]\n");
        usage.append("              [--pkg-whitelist-file PACKAGE_WHITELIST_FILE]\n");
        usage.append("              [--wait-dbg] [--dbg-no-events]\n");
        usage.append("              [--setup scriptfile] [-f scriptfile [-f scriptfile] ...]\n");
        usage.append("              [--port port]\n");
        usage.append("              [-s SEED] [-v [-v] ...]\n");
        usage.append("              [--throttle MILLISEC] [--randomize-throttle]\n");
        usage.append("              [--profile-wait MILLISEC]\n");
        usage.append("              [--device-sleep-time MILLISEC]\n");
        usage.append("              [--randomize-script]\n");
        usage.append("              [--script-log]\n");
        usage.append("              [--bugreport]\n");
        usage.append("              [--periodic-bugreport]\n");
        usage.append("              COUNT\n");
        System.err.println(usage.toString());
    }
}
