package com.android.internal.os;

import android.accounts.GrantCredentialsPermissionActivity;
import android.app.ActivityManagerNative;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport;
import android.ddm.DdmRegister;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.AndroidConfig;
import com.android.internal.os.ZygoteInit;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.VMRuntime;
import java.lang.Thread;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.TimeZone;
import java.util.logging.LogManager;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public class RuntimeInit {
    private static final boolean DEBUG = false;
    private static final String TAG = "AndroidRuntime";
    private static boolean initialized;
    private static IBinder mApplicationObject;
    private static volatile boolean mCrashing = false;

    private static final native void nativeFinishInit();

    private static final native void nativeSetExitWithoutCleanup(boolean z);

    private static final native void nativeZygoteInit();

    static {
        DdmRegister.registerHandlers();
    }

    private static int Clog_e(String tag, String msg, Throwable tr) {
        return Log.println_native(4, 6, tag, msg + '\n' + Log.getStackTraceString(tr));
    }

    private static class UncaughtHandler implements Thread.UncaughtExceptionHandler {
        private UncaughtHandler() {
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                try {
                    if (RuntimeInit.mCrashing) {
                        return;
                    }
                    boolean unused = RuntimeInit.mCrashing = true;
                    if (RuntimeInit.mApplicationObject == null) {
                        RuntimeInit.Clog_e(RuntimeInit.TAG, "*** FATAL EXCEPTION IN SYSTEM PROCESS: " + t.getName(), e);
                    } else {
                        StringBuilder message = new StringBuilder();
                        message.append("FATAL EXCEPTION: ").append(t.getName()).append("\n");
                        String processName = ActivityThread.currentProcessName();
                        if (processName != null) {
                            message.append("Process: ").append(processName).append(", ");
                        }
                        message.append("PID: ").append(Process.myPid());
                        RuntimeInit.Clog_e(RuntimeInit.TAG, message.toString(), e);
                    }
                    ActivityManagerNative.getDefault().handleApplicationCrash(RuntimeInit.mApplicationObject, new ApplicationErrorReport.CrashInfo(e));
                } finally {
                    Process.killProcess(Process.myPid());
                    System.exit(10);
                }
            } catch (Throwable t2) {
                try {
                    RuntimeInit.Clog_e(RuntimeInit.TAG, "Error reporting crash", t2);
                } catch (Throwable th) {
                }
            }
        }
    }

    private static final void commonInit() {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtHandler());
        TimezoneGetter.setInstance(new TimezoneGetter() {
            public String getId() {
                return SystemProperties.get("persist.sys.timezone");
            }
        });
        TimeZone.setDefault(null);
        LogManager.getLogManager().reset();
        new AndroidConfig();
        String userAgent = getDefaultUserAgent();
        System.setProperty("http.agent", userAgent);
        NetworkManagementSocketTagger.install();
        String trace = SystemProperties.get("ro.kernel.android.tracing");
        if (trace.equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
            Slog.i(TAG, "NOTE: emulator trace profiling enabled");
            Debug.enableEmulatorTraceOutput();
        }
        initialized = true;
    }

    private static String getDefaultUserAgent() {
        StringBuilder result = new StringBuilder(64);
        result.append("Dalvik/");
        result.append(System.getProperty("java.vm.version"));
        result.append(" (Linux; U; Android ");
        String version = Build.VERSION.RELEASE;
        if (version.length() <= 0) {
            version = "1.0";
        }
        result.append(version);
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String model = Build.MODEL;
            if (model.length() > 0) {
                result.append("; ");
                result.append(model);
            }
        }
        String id = Build.ID;
        if (id.length() > 0) {
            result.append(" Build/");
            result.append(id);
        }
        result.append(")");
        return result.toString();
    }

    private static void invokeStaticMain(String className, String[] argv, ClassLoader classLoader) throws ZygoteInit.MethodAndArgsCaller {
        try {
            Class<?> cl = Class.forName(className, true, classLoader);
            try {
                Method m = cl.getMethod("main", String[].class);
                int modifiers = m.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                    throw new RuntimeException("Main method is not public and static on " + className);
                }
                throw new ZygoteInit.MethodAndArgsCaller(m, argv);
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Missing static main on " + className, ex);
            } catch (SecurityException ex2) {
                throw new RuntimeException("Problem getting static main on " + className, ex2);
            }
        } catch (ClassNotFoundException ex3) {
            throw new RuntimeException("Missing class when invoking static main " + className, ex3);
        }
    }

    public static final void main(String[] argv) {
        if (argv.length == 2 && argv[1].equals(GrantCredentialsPermissionActivity.EXTRAS_PACKAGES)) {
            redirectLogStreams();
        }
        commonInit();
        nativeFinishInit();
    }

    public static final void zygoteInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) throws ZygoteInit.MethodAndArgsCaller {
        redirectLogStreams();
        commonInit();
        nativeZygoteInit();
        applicationInit(targetSdkVersion, argv, classLoader);
    }

    public static void wrapperInit(int targetSdkVersion, String[] argv) throws ZygoteInit.MethodAndArgsCaller {
        applicationInit(targetSdkVersion, argv, null);
    }

    private static void applicationInit(int targetSdkVersion, String[] argv, ClassLoader classLoader) throws ZygoteInit.MethodAndArgsCaller {
        nativeSetExitWithoutCleanup(true);
        VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);
        try {
            Arguments args = new Arguments(argv);
            invokeStaticMain(args.startClass, args.startArgs, classLoader);
        } catch (IllegalArgumentException ex) {
            Slog.e(TAG, ex.getMessage());
        }
    }

    public static void redirectLogStreams() {
        System.out.close();
        System.setOut(new AndroidPrintStream(4, "System.out"));
        System.err.close();
        System.setErr(new AndroidPrintStream(5, "System.err"));
    }

    public static void wtf(String tag, Throwable t, boolean system) {
        try {
            if (ActivityManagerNative.getDefault().handleApplicationWtf(mApplicationObject, tag, system, new ApplicationErrorReport.CrashInfo(t))) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        } catch (Throwable t2) {
            Slog.e(TAG, "Error reporting WTF", t2);
            Slog.e(TAG, "Original WTF:", t);
        }
    }

    public static final void setApplicationObject(IBinder app) {
        mApplicationObject = app;
    }

    public static final IBinder getApplicationObject() {
        return mApplicationObject;
    }

    static class Arguments {
        String[] startArgs;
        String startClass;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int curArg = 0;
            while (true) {
                if (curArg >= args.length) {
                    break;
                }
                String arg = args[curArg];
                if (arg.equals("--")) {
                    curArg++;
                    break;
                } else if (!arg.startsWith("--")) {
                    break;
                } else {
                    curArg++;
                }
            }
            if (curArg == args.length) {
                throw new IllegalArgumentException("Missing classname argument to RuntimeInit!");
            }
            int curArg2 = curArg + 1;
            this.startClass = args[curArg];
            this.startArgs = new String[args.length - curArg2];
            System.arraycopy(args, curArg2, this.startArgs, 0, this.startArgs.length);
        }
    }
}
