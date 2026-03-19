package com.android.commands.monkey;

import android.app.ActivityManager;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ComponentInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageItemInfo;
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
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Monkey {
    private static final int DEBUG_ALLOW_ANY_RESTARTS = 0;
    private static final int DEBUG_ALLOW_ANY_STARTS = 0;
    private static final String TOMBSTONE_PREFIX = "tombstone_";
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
    private String mMatchDescription;
    private boolean mMonitorNativeCrashes;
    private int mNextArg;
    private String mPkgBlacklistFile;
    private String mPkgWhitelistFile;
    private IPackageManager mPm;
    private String mReportProcessName;
    private boolean mSendNoEvents;
    private int mVerbose;
    private IWindowManager mWm;
    private static final File TOMBSTONES_PATH = new File("/data/tombstones");
    private static int NUM_READ_TOMBSTONE_RETRIES = 5;
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
    private HashSet<Long> mTombstones = null;
    float[] mFactors = new float[12];
    private MonkeyNetworkMonitor mNetworkMonitor = new MonkeyNetworkMonitor();
    private boolean mPermissionTargetSystem = false;

    private class ActivityController extends IActivityController.Stub {
        private ActivityController() {
        }

        public boolean activityStarting(Intent intent, String str) {
            boolean zIsActivityStartingAllowed = isActivityStartingAllowed(intent, str);
            if (Monkey.this.mVerbose > 0) {
                StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
                Logger logger = Logger.out;
                StringBuilder sb = new StringBuilder();
                sb.append("    // ");
                sb.append(zIsActivityStartingAllowed ? "Allowing" : "Rejecting");
                sb.append(" start of ");
                sb.append(intent);
                sb.append(" in package ");
                sb.append(str);
                logger.println(sb.toString());
                StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            }
            Monkey.currentPackage = str;
            Monkey.currentIntent = intent;
            return zIsActivityStartingAllowed;
        }

        private boolean isActivityStartingAllowed(Intent intent, String str) {
            if (MonkeyUtils.getPackageFilter().checkEnteringPackage(str)) {
                return true;
            }
            Set<String> categories = intent.getCategories();
            if (intent.getAction() == "android.intent.action.MAIN" && categories != null && categories.contains("android.intent.category.HOME")) {
                try {
                    if (str.equals(((PackageItemInfo) Monkey.this.mPm.resolveIntent(intent, intent.getType(), 0, UserHandle.myUserId()).activityInfo).packageName)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Logger.err.println("** Failed talking with package manager!");
                    return false;
                }
            }
            return false;
        }

        public boolean activityResuming(String str) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
            Logger.out.println("    // activityResuming(" + str + ")");
            boolean z = MonkeyUtils.getPackageFilter().checkEnteringPackage(str);
            if (!z && Monkey.this.mVerbose > 0) {
                Logger logger = Logger.out;
                StringBuilder sb = new StringBuilder();
                sb.append("    // ");
                sb.append(z ? "Allowing" : "Rejecting");
                sb.append(" resume of package ");
                sb.append(str);
                logger.println(sb.toString());
            }
            Monkey.currentPackage = str;
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            return z;
        }

        public boolean appCrashed(String str, int i, String str2, String str3, long j, String str4) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
            Logger.out.println("// CRASH: " + str + " (pid " + i + ")");
            Logger logger = Logger.out;
            StringBuilder sb = new StringBuilder();
            sb.append("// Short Msg: ");
            sb.append(str2);
            logger.println(sb.toString());
            Logger.out.println("// Long Msg: " + str3);
            Logger.out.println("// Build Label: " + Build.FINGERPRINT);
            Logger.out.println("// Build Changelist: " + Build.VERSION.INCREMENTAL);
            Logger.out.println("// Build Time: " + Build.TIME);
            Logger.out.println("// " + str4.replace("\n", "\n// "));
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            if (Monkey.this.mMatchDescription == null || str2.contains(Monkey.this.mMatchDescription) || str3.contains(Monkey.this.mMatchDescription) || str4.contains(Monkey.this.mMatchDescription)) {
                if (!Monkey.this.mIgnoreCrashes || Monkey.this.mRequestBugreport) {
                    synchronized (Monkey.this) {
                        if (!Monkey.this.mIgnoreCrashes) {
                            Monkey.this.mAbort = true;
                        }
                        if (Monkey.this.mRequestBugreport) {
                            Monkey.this.mRequestAppCrashBugreport = true;
                            Monkey.this.mReportProcessName = str;
                        }
                    }
                    return !Monkey.this.mKillProcessAfterError;
                }
                return false;
            }
            return false;
        }

        public int appEarlyNotResponding(String str, int i, String str2) {
            return 0;
        }

        public int appNotResponding(String str, int i, String str2) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
            Logger.out.println("// NOT RESPONDING: " + str + " (pid " + i + ")");
            Logger.out.println(str2);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            if (Monkey.this.mMatchDescription == null || str2.contains(Monkey.this.mMatchDescription)) {
                synchronized (Monkey.this) {
                    Monkey.this.mRequestAnrTraces = true;
                    Monkey.this.mRequestDumpsysMemInfo = true;
                    Monkey.this.mRequestProcRank = true;
                    if (Monkey.this.mRequestBugreport) {
                        Monkey.this.mRequestAnrBugreport = true;
                        Monkey.this.mReportProcessName = str;
                    }
                }
                if (!Monkey.this.mIgnoreTimeouts) {
                    synchronized (Monkey.this) {
                        Monkey.this.mAbort = true;
                    }
                }
            }
            return Monkey.this.mKillProcessAfterError ? -1 : 1;
        }

        public int systemNotResponding(String str) {
            StrictMode.ThreadPolicy threadPolicyAllowThreadDiskWrites = StrictMode.allowThreadDiskWrites();
            Logger.out.println("// WATCHDOG: " + str);
            StrictMode.setThreadPolicy(threadPolicyAllowThreadDiskWrites);
            synchronized (Monkey.this) {
                if (Monkey.this.mMatchDescription == null || str.contains(Monkey.this.mMatchDescription)) {
                    if (!Monkey.this.mIgnoreCrashes) {
                        Monkey.this.mAbort = true;
                    }
                    if (Monkey.this.mRequestBugreport) {
                        Monkey.this.mRequestWatchdogBugreport = true;
                    }
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
        File[] fileArrListFiles = new File("/data/anr/").listFiles();
        if (fileArrListFiles != null) {
            File file = null;
            long j = 0;
            for (File file2 : fileArrListFiles) {
                long jLastModified = file2.lastModified();
                if (jLastModified > j) {
                    file = file2;
                    j = jLastModified;
                }
            }
            if (file != null) {
                commandLineReport("anr traces", "cat " + file.getAbsolutePath());
            }
        }
    }

    private void reportDumpsysMemInfo() {
        commandLineReport("meminfo", "dumpsys meminfo");
    }

    private void commandLineReport(String str, String str2) {
        BufferedWriter bufferedWriter;
        Logger.err.println(str + ":");
        Runtime.getRuntime();
        try {
            Process processExec = Runtime.getRuntime().exec(str2);
            if (this.mRequestBugreport) {
                bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), str), true));
            } else {
                bufferedWriter = null;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(processExec.getInputStream()));
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                if (this.mRequestBugreport) {
                    try {
                        bufferedWriter.write(line);
                        bufferedWriter.write("\n");
                    } catch (IOException e) {
                        while (bufferedReader.readLine() != null) {
                        }
                        Logger.err.println(e.toString());
                        int iWaitFor = processExec.waitFor();
                        Logger.err.println("// " + str + " status was " + iWaitFor);
                        if (bufferedWriter == null) {
                        }
                    }
                } else {
                    Logger.err.println(line);
                }
            }
            int iWaitFor2 = processExec.waitFor();
            Logger.err.println("// " + str + " status was " + iWaitFor2);
            if (bufferedWriter == null) {
                bufferedWriter.close();
            }
        } catch (Exception e2) {
            Logger.err.println("// Exception from " + str + ":");
            Logger.err.println(e2.toString());
        }
    }

    private void writeScriptLog(int i) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getLegacyExternalStorageDirectory(), "scriptlog.txt"), true));
            bufferedWriter.write("iteration: " + i + " time: " + MonkeyUtils.toCalendarTime(System.currentTimeMillis()) + "\n");
            bufferedWriter.close();
        } catch (IOException e) {
            Logger.err.println(e.toString());
        }
    }

    private void getBugreport(String str) {
        commandLineReport((str + MonkeyUtils.toCalendarTime(System.currentTimeMillis())).replaceAll("[ ,:]", "_") + ".txt", "bugreport");
    }

    public static void main(String[] strArr) {
        Process.setArgV0("com.android.commands.monkey");
        Logger.err.println("args: " + Arrays.toString(strArr));
        System.exit(new Monkey().run(strArr));
    }

    private int run(String[] strArr) {
        for (String str : strArr) {
            if ("--wait-dbg".equals(str)) {
                Debug.waitForDebugger();
            }
        }
        this.mVerbose = 0;
        this.mCount = 1000;
        this.mSeed = 0L;
        this.mThrottle = 0L;
        this.mArgs = strArr;
        for (String str2 : strArr) {
            Logger.err.println(" arg: \"" + str2 + "\"");
        }
        this.mNextArg = 0;
        for (int i = 0; i < 12; i++) {
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
            Logger.out.println(":Monkey: seed=" + this.mSeed + " count=" + this.mCount);
            MonkeyUtils.getPackageFilter().dump();
            if (this.mMainCategories.size() != 0) {
                Iterator<String> it = this.mMainCategories.iterator();
                while (it.hasNext()) {
                    Logger.out.println(":IncludeCategory: " + it.next());
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
                Logger.out.println("Error binding to network socket.");
                return -5;
            }
        } else {
            if (this.mVerbose >= 2) {
                Logger.out.println("// Seeded: " + this.mSeed);
            }
            this.mEventSource = new MonkeySourceRandom(this.mRandom, this.mMainApps, this.mThrottle, this.mRandomizeThrottle, this.mPermissionTargetSystem);
            this.mEventSource.setVerbose(this.mVerbose);
            for (int i2 = 0; i2 < 12; i2++) {
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
            int iRunMonkeyCycles = runMonkeyCycles();
            new MonkeyRotationEvent(0, false).injectEvent(this.mWm, this.mAm, this.mVerbose);
            this.mNetworkMonitor.stop();
            synchronized (this) {
                if (this.mRequestAnrTraces) {
                    reportAnrTraces();
                    this.mRequestAnrTraces = false;
                }
                if (this.mRequestAnrBugreport) {
                    Logger.out.println("Print the anr report");
                    getBugreport("anr_" + this.mReportProcessName + "_");
                    this.mRequestAnrBugreport = false;
                }
                if (this.mRequestWatchdogBugreport) {
                    Logger.out.println("Print the watchdog report");
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
                    Logger.out.println("// Generated profiling reports in /data/misc");
                }
            }
            try {
                this.mAm.setActivityController((IActivityController) null, true);
                this.mNetworkMonitor.unregister(this.mAm);
            } catch (RemoteException e2) {
                if (iRunMonkeyCycles >= this.mCount) {
                    iRunMonkeyCycles = this.mCount - 1;
                }
            }
            if (this.mVerbose > 0) {
                Logger.out.println(":Dropped: keys=" + this.mDroppedKeyEvents + " pointers=" + this.mDroppedPointerEvents + " trackballs=" + this.mDroppedTrackballEvents + " flips=" + this.mDroppedFlipEvents + " rotations=" + this.mDroppedRotationEvents);
            }
            this.mNetworkMonitor.dump();
            if (iRunMonkeyCycles < this.mCount - 1) {
                Logger.err.println("** System appears to have crashed at event " + iRunMonkeyCycles + " of " + this.mCount + " using seed " + this.mSeed);
                return iRunMonkeyCycles;
            }
            if (this.mVerbose > 0) {
                Logger.out.println("// Monkey finished");
                return 0;
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
        try {
            HashSet hashSet = new HashSet();
            while (true) {
                String strNextOption = nextOption();
                if (strNextOption != null) {
                    if (strNextOption.equals("-s")) {
                        this.mSeed = nextOptionLong("Seed");
                    } else if (strNextOption.equals("-p")) {
                        hashSet.add(nextOptionData());
                    } else if (strNextOption.equals("-c")) {
                        this.mMainCategories.add(nextOptionData());
                    } else if (strNextOption.equals("-v")) {
                        this.mVerbose++;
                    } else if (strNextOption.equals("--ignore-crashes")) {
                        this.mIgnoreCrashes = true;
                    } else if (strNextOption.equals("--ignore-timeouts")) {
                        this.mIgnoreTimeouts = true;
                    } else if (strNextOption.equals("--ignore-security-exceptions")) {
                        this.mIgnoreSecurityExceptions = true;
                    } else if (strNextOption.equals("--monitor-native-crashes")) {
                        this.mMonitorNativeCrashes = true;
                    } else if (strNextOption.equals("--ignore-native-crashes")) {
                        this.mIgnoreNativeCrashes = true;
                    } else if (strNextOption.equals("--kill-process-after-error")) {
                        this.mKillProcessAfterError = true;
                    } else if (strNextOption.equals("--hprof")) {
                        this.mGenerateHprof = true;
                    } else if (strNextOption.equals("--match-description")) {
                        this.mMatchDescription = nextOptionData();
                    } else if (strNextOption.equals("--pct-touch")) {
                        this.mFactors[0] = -nextOptionLong("touch events percentage");
                    } else if (strNextOption.equals("--pct-motion")) {
                        this.mFactors[1] = -nextOptionLong("motion events percentage");
                    } else if (strNextOption.equals("--pct-trackball")) {
                        this.mFactors[3] = -nextOptionLong("trackball events percentage");
                    } else if (strNextOption.equals("--pct-rotation")) {
                        this.mFactors[4] = -nextOptionLong("screen rotation events percentage");
                    } else if (strNextOption.equals("--pct-syskeys")) {
                        this.mFactors[8] = -nextOptionLong("system (key) operations percentage");
                    } else if (strNextOption.equals("--pct-nav")) {
                        this.mFactors[6] = -nextOptionLong("nav events percentage");
                    } else if (strNextOption.equals("--pct-majornav")) {
                        this.mFactors[7] = -nextOptionLong("major nav events percentage");
                    } else if (strNextOption.equals("--pct-appswitch")) {
                        this.mFactors[9] = -nextOptionLong("app switch events percentage");
                    } else if (strNextOption.equals("--pct-flip")) {
                        this.mFactors[10] = -nextOptionLong("keyboard flip percentage");
                    } else if (strNextOption.equals("--pct-anyevent")) {
                        this.mFactors[11] = -nextOptionLong("any events percentage");
                    } else if (strNextOption.equals("--pct-pinchzoom")) {
                        this.mFactors[2] = -nextOptionLong("pinch zoom events percentage");
                    } else if (strNextOption.equals("--pct-permission")) {
                        this.mFactors[5] = -nextOptionLong("runtime permission toggle events percentage");
                    } else if (strNextOption.equals("--pkg-blacklist-file")) {
                        this.mPkgBlacklistFile = nextOptionData();
                    } else if (strNextOption.equals("--pkg-whitelist-file")) {
                        this.mPkgWhitelistFile = nextOptionData();
                    } else if (strNextOption.equals("--throttle")) {
                        this.mThrottle = nextOptionLong("delay (in milliseconds) to wait between events");
                    } else if (strNextOption.equals("--randomize-throttle")) {
                        this.mRandomizeThrottle = true;
                    } else if (!strNextOption.equals("--wait-dbg")) {
                        if (strNextOption.equals("--dbg-no-events")) {
                            this.mSendNoEvents = true;
                        } else if (strNextOption.equals("--port")) {
                            this.mServerPort = (int) nextOptionLong("Server port to listen on for commands");
                        } else if (strNextOption.equals("--setup")) {
                            this.mSetupFileName = nextOptionData();
                        } else if (strNextOption.equals("-f")) {
                            this.mScriptFileNames.add(nextOptionData());
                        } else if (strNextOption.equals("--profile-wait")) {
                            this.mProfileWaitTime = nextOptionLong("Profile delay (in milliseconds) to wait between user action");
                        } else if (strNextOption.equals("--device-sleep-time")) {
                            this.mDeviceSleepTime = nextOptionLong("Device sleep time(in milliseconds)");
                        } else if (strNextOption.equals("--randomize-script")) {
                            this.mRandomizeScript = true;
                        } else if (strNextOption.equals("--script-log")) {
                            this.mScriptLog = true;
                        } else if (strNextOption.equals("--bugreport")) {
                            this.mRequestBugreport = true;
                        } else if (strNextOption.equals("--periodic-bugreport")) {
                            this.mGetPeriodicBugreport = true;
                            this.mBugreportFrequency = nextOptionLong("Number of iterations");
                        } else if (strNextOption.equals("--permission-target-system")) {
                            this.mPermissionTargetSystem = true;
                        } else {
                            if (strNextOption.equals("-h")) {
                                showUsage();
                                return false;
                            }
                            Logger.err.println("** Error: Unknown option: " + strNextOption);
                            showUsage();
                            return false;
                        }
                    }
                } else {
                    MonkeyUtils.getPackageFilter().addValidPackages(hashSet);
                    if (this.mServerPort == -1) {
                        String strNextArg = nextArg();
                        if (strNextArg == null) {
                            Logger.err.println("** Error: Count not specified");
                            showUsage();
                            return false;
                        }
                        try {
                            this.mCount = Integer.parseInt(strNextArg);
                        } catch (NumberFormatException e) {
                            Logger.err.println("** Error: Count is not a number: \"" + strNextArg + "\"");
                            showUsage();
                            return false;
                        }
                    }
                    return true;
                }
            }
        } catch (RuntimeException e2) {
            Logger.err.println("** Error: " + e2.toString());
            showUsage();
            return false;
        }
    }

    private static boolean loadPackageListFromFile(String str, Set<String> set) throws Throwable {
        BufferedReader bufferedReader = null;
        try {
            try {
                BufferedReader bufferedReader2 = new BufferedReader(new FileReader(str));
                while (true) {
                    try {
                        String line = bufferedReader2.readLine();
                        if (line == null) {
                            try {
                                bufferedReader2.close();
                                return true;
                            } catch (IOException e) {
                                Logger.err.println("" + e);
                                return true;
                            }
                        }
                        String strTrim = line.trim();
                        if (strTrim.length() > 0 && !strTrim.startsWith("#")) {
                            set.add(strTrim);
                        }
                    } catch (IOException e2) {
                        e = e2;
                        bufferedReader = bufferedReader2;
                        Logger.err.println("" + e);
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e3) {
                                Logger.err.println("" + e3);
                            }
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        bufferedReader = bufferedReader2;
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (IOException e4) {
                                Logger.err.println("" + e4);
                            }
                        }
                        throw th;
                    }
                }
            } catch (IOException e5) {
                e = e5;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private boolean loadPackageLists() {
        if ((this.mPkgWhitelistFile != null || MonkeyUtils.getPackageFilter().hasValidPackages()) && this.mPkgBlacklistFile != null) {
            Logger.err.println("** Error: you can not specify a package blacklist together with a whitelist or individual packages (via -p).");
            return false;
        }
        HashSet hashSet = new HashSet();
        if (this.mPkgWhitelistFile != null && !loadPackageListFromFile(this.mPkgWhitelistFile, hashSet)) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addValidPackages(hashSet);
        HashSet hashSet2 = new HashSet();
        if (this.mPkgBlacklistFile != null && !loadPackageListFromFile(this.mPkgBlacklistFile, hashSet2)) {
            return false;
        }
        MonkeyUtils.getPackageFilter().addInvalidPackages(hashSet2);
        return true;
    }

    private boolean checkInternalConfiguration() {
        return true;
    }

    private boolean getSystemInterfaces() {
        this.mAm = ActivityManager.getService();
        if (this.mAm == null) {
            Logger.err.println("** Error: Unable to connect to activity manager; is the system running?");
            return false;
        }
        this.mWm = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        if (this.mWm == null) {
            Logger.err.println("** Error: Unable to connect to window manager; is the system running?");
            return false;
        }
        this.mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        if (this.mPm == null) {
            Logger.err.println("** Error: Unable to connect to package manager; is the system running?");
            return false;
        }
        try {
            this.mAm.setActivityController(new ActivityController(), true);
            this.mNetworkMonitor.register(this.mAm);
            return true;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
            return false;
        }
    }

    private boolean getMainApps() {
        try {
            int size = this.mMainCategories.size();
            for (int i = 0; i < size; i++) {
                Intent intent = new Intent("android.intent.action.MAIN");
                String str = this.mMainCategories.get(i);
                if (str.length() > 0) {
                    intent.addCategory(str);
                }
                List list = this.mPm.queryIntentActivities(intent, (String) null, 0, UserHandle.myUserId()).getList();
                if (list == null || list.size() == 0) {
                    Logger.err.println("// Warning: no activities found for category " + str);
                } else {
                    if (this.mVerbose >= 2) {
                        Logger.out.println("// Selecting main activities from category " + str);
                    }
                    int size2 = list.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        ResolveInfo resolveInfo = (ResolveInfo) list.get(i2);
                        String str2 = ((PackageItemInfo) ((ComponentInfo) resolveInfo.activityInfo).applicationInfo).packageName;
                        if (MonkeyUtils.getPackageFilter().checkEnteringPackage(str2)) {
                            if (this.mVerbose >= 2) {
                                Logger.out.println("//   + Using main activity " + ((PackageItemInfo) resolveInfo.activityInfo).name + " (from package " + str2 + ")");
                            }
                            this.mMainApps.add(new ComponentName(str2, ((PackageItemInfo) resolveInfo.activityInfo).name));
                        } else if (this.mVerbose >= 3) {
                            Logger.out.println("//   - NOT USING main activity " + ((PackageItemInfo) resolveInfo.activityInfo).name + " (from package " + str2 + ")");
                        }
                    }
                }
            }
            if (this.mMainApps.size() == 0) {
                Logger.out.println("** No activities found to run, monkey aborted.");
                return false;
            }
            return true;
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with package manager!");
            return false;
        }
    }

    private int runMonkeyCycles() {
        boolean z = false;
        int i = 0;
        int i2 = 0;
        boolean z2 = false;
        boolean z3 = false;
        boolean z4 = false;
        while (!z) {
            try {
                if (i >= this.mCount) {
                    break;
                }
                synchronized (this) {
                    if (this.mRequestProcRank) {
                        reportProcRank();
                        this.mRequestProcRank = false;
                    }
                    if (this.mRequestAnrTraces) {
                        this.mRequestAnrTraces = false;
                        z2 = true;
                    }
                    if (this.mRequestAnrBugreport) {
                        getBugreport("anr_" + this.mReportProcessName + "_");
                        this.mRequestAnrBugreport = false;
                    }
                    if (this.mRequestWatchdogBugreport) {
                        Logger.out.println("Print the watchdog report");
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
                        z3 = true;
                    }
                    if (this.mMonitorNativeCrashes && checkNativeCrashes() && i2 > 0) {
                        Logger.out.println("** New native crash detected.");
                        if (this.mRequestBugreport) {
                            getBugreport("native_crash_");
                        }
                        this.mAbort = this.mAbort || !this.mIgnoreNativeCrashes || this.mKillProcessAfterError;
                    }
                    if (this.mAbort) {
                        z4 = true;
                    }
                    if (this.mWatchdogWaiting) {
                        this.mWatchdogWaiting = false;
                        notifyAll();
                    }
                }
                if (z2) {
                    reportAnrTraces();
                    z2 = false;
                }
                if (z3) {
                    reportDumpsysMemInfo();
                    z3 = false;
                }
                if (z4) {
                    Logger.out.println("** Monkey aborted due to error.");
                    Logger.out.println("Events injected: " + i2);
                    return i2;
                }
                if (this.mSendNoEvents) {
                    i2++;
                    i++;
                } else {
                    if (this.mVerbose > 0 && i2 % 100 == 0 && i2 != 0) {
                        String calendarTime = MonkeyUtils.toCalendarTime(System.currentTimeMillis());
                        long jElapsedRealtime = SystemClock.elapsedRealtime();
                        Logger.out.println("    //[calendar_time:" + calendarTime + " system_uptime:" + jElapsedRealtime + "]");
                        Logger logger = Logger.out;
                        StringBuilder sb = new StringBuilder();
                        sb.append("    // Sending event #");
                        sb.append(i2);
                        logger.println(sb.toString());
                    }
                    MonkeyEvent nextEvent = this.mEventSource.getNextEvent();
                    if (nextEvent != null) {
                        int iInjectEvent = nextEvent.injectEvent(this.mWm, this.mAm, this.mVerbose);
                        if (iInjectEvent == 0) {
                            Logger.out.println("    // Injection Failed");
                            if (nextEvent instanceof MonkeyKeyEvent) {
                                this.mDroppedKeyEvents++;
                            } else if (nextEvent instanceof MonkeyMotionEvent) {
                                this.mDroppedPointerEvents++;
                            } else if (nextEvent instanceof MonkeyFlipEvent) {
                                this.mDroppedFlipEvents++;
                            } else if (nextEvent instanceof MonkeyRotationEvent) {
                                this.mDroppedRotationEvents++;
                            }
                        } else if (iInjectEvent == -1) {
                            Logger.err.println("** Error: RemoteException while injecting event.");
                            z = true;
                        } else if (iInjectEvent == -2 && (!this.mIgnoreSecurityExceptions)) {
                            Logger.err.println("** Error: SecurityException while injecting event.");
                        }
                        if (!(nextEvent instanceof MonkeyThrottleEvent)) {
                            i2++;
                            if (this.mCountEvents) {
                                i++;
                            }
                        }
                    } else {
                        if (this.mCountEvents) {
                            break;
                        }
                        i++;
                        writeScriptLog(i);
                        if (this.mGetPeriodicBugreport && ((long) i) % this.mBugreportFrequency == 0) {
                            this.mRequestPeriodicBugreport = true;
                        }
                    }
                }
            } catch (RuntimeException e) {
                Logger.error("** Error: A RuntimeException occurred:", e);
            }
        }
        Logger.out.println("Events injected: " + i2);
        return i2;
    }

    private void signalPersistentProcesses() {
        try {
            this.mAm.signalPersistentProcesses(10);
            synchronized (this) {
                wait(2000L);
            }
        } catch (RemoteException e) {
            Logger.err.println("** Failed talking with activity manager!");
        } catch (InterruptedException e2) {
        }
    }

    private boolean checkNativeCrashes() {
        String[] list = TOMBSTONES_PATH.list();
        if (list == null || list.length == 0) {
            this.mTombstones = null;
            return false;
        }
        HashSet<Long> hashSet = new HashSet<>();
        boolean z = false;
        for (String str : list) {
            if (str.startsWith(TOMBSTONE_PREFIX)) {
                File file = new File(TOMBSTONES_PATH, str);
                hashSet.add(Long.valueOf(file.lastModified()));
                if (this.mTombstones == null || !this.mTombstones.contains(Long.valueOf(file.lastModified()))) {
                    waitForTombstoneToBeWritten(Paths.get(TOMBSTONES_PATH.getPath(), str));
                    Logger.out.println("** New tombstone found: " + file.getAbsolutePath() + ", size: " + file.length());
                    z = true;
                }
            }
        }
        this.mTombstones = hashSet;
        return z;
    }

    private void waitForTombstoneToBeWritten(Path path) {
        boolean z = false;
        int i = 0;
        while (true) {
            try {
                if (i >= NUM_READ_TOMBSTONE_RETRIES) {
                    break;
                }
                long size = Files.size(path);
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                }
                if (size <= 0 || Files.size(path) != size) {
                    i++;
                } else {
                    z = true;
                    break;
                }
            } catch (IOException e2) {
                Logger.err.println("Failed to get tombstone file size: " + e2.toString());
            }
        }
        if (!z) {
            Logger.err.println("Incomplete tombstone file.");
        }
    }

    private String nextOption() {
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        if (!str.startsWith("-")) {
            return null;
        }
        this.mNextArg++;
        if (str.equals("--")) {
            return null;
        }
        if (str.length() > 1 && str.charAt(1) != '-') {
            if (str.length() > 2) {
                this.mCurArgData = str.substring(2);
                return str.substring(0, 2);
            }
            this.mCurArgData = null;
            return str;
        }
        this.mCurArgData = null;
        Logger.err.println("arg=\"" + str + "\" mCurArgData=\"" + this.mCurArgData + "\" mNextArg=" + this.mNextArg + " argwas=\"" + this.mArgs[this.mNextArg - 1] + "\" nextarg=\"" + this.mArgs[this.mNextArg] + "\"");
        return str;
    }

    private String nextOptionData() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mNextArg >= this.mArgs.length) {
            return null;
        }
        String str = this.mArgs[this.mNextArg];
        Logger.err.println("data=\"" + str + "\"");
        this.mNextArg = this.mNextArg + 1;
        return str;
    }

    private long nextOptionLong(String str) {
        try {
            return Long.parseLong(nextOptionData());
        } catch (NumberFormatException e) {
            Logger.err.println("** Error: " + str + " is not a number");
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
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("usage: monkey [-p ALLOWED_PACKAGE [-p ALLOWED_PACKAGE] ...]\n");
        stringBuffer.append("              [-c MAIN_CATEGORY [-c MAIN_CATEGORY] ...]\n");
        stringBuffer.append("              [--ignore-crashes] [--ignore-timeouts]\n");
        stringBuffer.append("              [--ignore-security-exceptions]\n");
        stringBuffer.append("              [--monitor-native-crashes] [--ignore-native-crashes]\n");
        stringBuffer.append("              [--kill-process-after-error] [--hprof]\n");
        stringBuffer.append("              [--match-description TEXT]\n");
        stringBuffer.append("              [--pct-touch PERCENT] [--pct-motion PERCENT]\n");
        stringBuffer.append("              [--pct-trackball PERCENT] [--pct-syskeys PERCENT]\n");
        stringBuffer.append("              [--pct-nav PERCENT] [--pct-majornav PERCENT]\n");
        stringBuffer.append("              [--pct-appswitch PERCENT] [--pct-flip PERCENT]\n");
        stringBuffer.append("              [--pct-anyevent PERCENT] [--pct-pinchzoom PERCENT]\n");
        stringBuffer.append("              [--pct-permission PERCENT]\n");
        stringBuffer.append("              [--pkg-blacklist-file PACKAGE_BLACKLIST_FILE]\n");
        stringBuffer.append("              [--pkg-whitelist-file PACKAGE_WHITELIST_FILE]\n");
        stringBuffer.append("              [--wait-dbg] [--dbg-no-events]\n");
        stringBuffer.append("              [--setup scriptfile] [-f scriptfile [-f scriptfile] ...]\n");
        stringBuffer.append("              [--port port]\n");
        stringBuffer.append("              [-s SEED] [-v [-v] ...]\n");
        stringBuffer.append("              [--throttle MILLISEC] [--randomize-throttle]\n");
        stringBuffer.append("              [--profile-wait MILLISEC]\n");
        stringBuffer.append("              [--device-sleep-time MILLISEC]\n");
        stringBuffer.append("              [--randomize-script]\n");
        stringBuffer.append("              [--script-log]\n");
        stringBuffer.append("              [--bugreport]\n");
        stringBuffer.append("              [--periodic-bugreport]\n");
        stringBuffer.append("              [--permission-target-system]\n");
        stringBuffer.append("              COUNT\n");
        Logger.err.println(stringBuffer.toString());
    }
}
