package com.android.internal.os;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.ApplicationErrorReport;
import android.ddm.DdmRegister;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.DeadObjectException;
import android.os.Debug;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.util.Log;
import android.util.Slog;
import com.android.internal.logging.AndroidConfig;
import com.android.server.NetworkManagementSocketTagger;
import dalvik.system.VMRuntime;
import java.lang.Thread;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.LogManager;
import org.apache.harmony.luni.internal.util.TimezoneGetter;

public class RuntimeInit {
    static final boolean DEBUG = false;
    static final String TAG = "AndroidRuntime";
    private static boolean initialized;
    private static IBinder mApplicationObject;
    private static volatile boolean mCrashing = false;

    private static final native void nativeFinishInit();

    private static final native void nativeSetExitWithoutCleanup(boolean z);

    private static int Clog_e(String str, String str2, Throwable th) {
        return Log.printlns(4, 6, str, str2, th);
    }

    private static class LoggingHandler implements Thread.UncaughtExceptionHandler {
        public volatile boolean mTriggered;

        private LoggingHandler() {
            this.mTriggered = false;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable th) {
            this.mTriggered = true;
            if (RuntimeInit.mCrashing) {
                return;
            }
            if (RuntimeInit.mApplicationObject == null && 1000 == Process.myUid()) {
                RuntimeInit.Clog_e(RuntimeInit.TAG, "*** FATAL EXCEPTION IN SYSTEM PROCESS: " + thread.getName(), th);
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("FATAL EXCEPTION: ");
            sb.append(thread.getName());
            sb.append("\n");
            String strCurrentProcessName = ActivityThread.currentProcessName();
            if (strCurrentProcessName != null) {
                sb.append("Process: ");
                sb.append(strCurrentProcessName);
                sb.append(", ");
            }
            sb.append("PID: ");
            sb.append(Process.myPid());
            RuntimeInit.Clog_e(RuntimeInit.TAG, sb.toString(), th);
        }
    }

    private static class KillApplicationHandler implements Thread.UncaughtExceptionHandler {
        private final LoggingHandler mLoggingHandler;

        public KillApplicationHandler(LoggingHandler loggingHandler) {
            this.mLoggingHandler = (LoggingHandler) Objects.requireNonNull(loggingHandler);
        }

        @Override
        public void uncaughtException(Thread thread, Throwable th) {
            try {
                try {
                    ensureLogging(thread, th);
                    if (RuntimeInit.mCrashing) {
                        return;
                    }
                    boolean unused = RuntimeInit.mCrashing = true;
                    if (ActivityThread.currentActivityThread() != null) {
                        ActivityThread.currentActivityThread().stopProfiling();
                    }
                    ActivityManager.getService().handleApplicationCrash(RuntimeInit.mApplicationObject, new ApplicationErrorReport.ParcelableCrashInfo(th));
                } catch (Throwable th2) {
                    if (!(th2 instanceof DeadObjectException)) {
                        try {
                            RuntimeInit.Clog_e(RuntimeInit.TAG, "Error reporting crash", th2);
                        } catch (Throwable th3) {
                        }
                    }
                }
            } finally {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }

        private void ensureLogging(Thread thread, Throwable th) {
            if (!this.mLoggingHandler.mTriggered) {
                try {
                    this.mLoggingHandler.uncaughtException(thread, th);
                } catch (Throwable th2) {
                }
            }
        }
    }

    protected static final void commonInit() {
        LoggingHandler loggingHandler = new LoggingHandler();
        Thread.setUncaughtExceptionPreHandler(loggingHandler);
        Thread.setDefaultUncaughtExceptionHandler(new KillApplicationHandler(loggingHandler));
        TimezoneGetter.setInstance(new TimezoneGetter() {
            public String getId() {
                return SystemProperties.get("persist.sys.timezone");
            }
        });
        TimeZone.setDefault(null);
        LogManager.getLogManager().reset();
        new AndroidConfig();
        System.setProperty("http.agent", getDefaultUserAgent());
        NetworkManagementSocketTagger.install();
        if (SystemProperties.get("ro.kernel.android.tracing").equals(WifiEnterpriseConfig.ENGINE_ENABLE)) {
            Slog.i(TAG, "NOTE: emulator trace profiling enabled");
            Debug.enableEmulatorTraceOutput();
        }
        initialized = true;
    }

    private static String getDefaultUserAgent() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("Dalvik/");
        sb.append(System.getProperty("java.vm.version"));
        sb.append(" (Linux; U; Android ");
        String str = Build.VERSION.RELEASE;
        if (str.length() <= 0) {
            str = "1.0";
        }
        sb.append(str);
        if ("REL".equals(Build.VERSION.CODENAME)) {
            String str2 = Build.MODEL;
            if (str2.length() > 0) {
                sb.append("; ");
                sb.append(str2);
            }
        }
        String str3 = Build.ID;
        if (str3.length() > 0) {
            sb.append(" Build/");
            sb.append(str3);
        }
        sb.append(")");
        return sb.toString();
    }

    protected static Runnable findStaticMain(String str, String[] strArr, ClassLoader classLoader) {
        try {
            try {
                Method method = Class.forName(str, true, classLoader).getMethod("main", String[].class);
                int modifiers = method.getModifiers();
                if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                    throw new RuntimeException("Main method is not public and static on " + str);
                }
                return new MethodAndArgsCaller(method, strArr);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Missing static main on " + str, e);
            } catch (SecurityException e2) {
                throw new RuntimeException("Problem getting static main on " + str, e2);
            }
        } catch (ClassNotFoundException e3) {
            throw new RuntimeException("Missing class when invoking static main " + str, e3);
        }
    }

    public static final void main(String[] strArr) {
        enableDdms();
        if (strArr.length == 2 && strArr[1].equals("application")) {
            redirectLogStreams();
        }
        commonInit();
        nativeFinishInit();
    }

    protected static Runnable applicationInit(int i, String[] strArr, ClassLoader classLoader) {
        nativeSetExitWithoutCleanup(true);
        VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        VMRuntime.getRuntime().setTargetSdkVersion(i);
        Arguments arguments = new Arguments(strArr);
        Trace.traceEnd(64L);
        return findStaticMain(arguments.startClass, arguments.startArgs, classLoader);
    }

    public static void redirectLogStreams() {
        System.out.close();
        System.setOut(new AndroidPrintStream(4, "System.out"));
        System.err.close();
        System.setErr(new AndroidPrintStream(5, "System.err"));
    }

    public static void wtf(String str, Throwable th, boolean z) {
        try {
            if (ActivityManager.getService().handleApplicationWtf(mApplicationObject, str, z, new ApplicationErrorReport.ParcelableCrashInfo(th))) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        } catch (Throwable th2) {
            if (!(th2 instanceof DeadObjectException)) {
                Slog.e(TAG, "Error reporting WTF", th2);
                Slog.e(TAG, "Original WTF:", th);
            }
        }
    }

    public static final void setApplicationObject(IBinder iBinder) {
        mApplicationObject = iBinder;
    }

    public static final IBinder getApplicationObject() {
        return mApplicationObject;
    }

    static final void enableDdms() {
        DdmRegister.registerHandlers();
    }

    static class Arguments {
        String[] startArgs;
        String startClass;

        Arguments(String[] strArr) throws IllegalArgumentException {
            parseArgs(strArr);
        }

        private void parseArgs(String[] strArr) throws IllegalArgumentException {
            int i = 0;
            while (true) {
                if (i >= strArr.length) {
                    break;
                }
                String str = strArr[i];
                if (str.equals("--")) {
                    i++;
                    break;
                } else if (!str.startsWith("--")) {
                    break;
                } else {
                    i++;
                }
            }
            if (i == strArr.length) {
                throw new IllegalArgumentException("Missing classname argument to RuntimeInit!");
            }
            int i2 = i + 1;
            this.startClass = strArr[i];
            this.startArgs = new String[strArr.length - i2];
            System.arraycopy(strArr, i2, this.startArgs, 0, this.startArgs.length);
        }
    }

    static class MethodAndArgsCaller implements Runnable {
        private final String[] mArgs;
        private final Method mMethod;

        public MethodAndArgsCaller(Method method, String[] strArr) {
            this.mMethod = method;
            this.mArgs = strArr;
        }

        @Override
        public void run() {
            try {
                this.mMethod.invoke(null, this.mArgs);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e2) {
                Throwable cause = e2.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                throw new RuntimeException(e2);
            }
        }
    }
}
