package android.os;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.IActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.INetworkManagementService;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.os.StrictMode;
import android.os.strictmode.CleartextNetworkViolation;
import android.os.strictmode.ContentUriWithoutPermissionViolation;
import android.os.strictmode.CustomViolation;
import android.os.strictmode.DiskReadViolation;
import android.os.strictmode.DiskWriteViolation;
import android.os.strictmode.FileUriExposedViolation;
import android.os.strictmode.InstanceCountViolation;
import android.os.strictmode.IntentReceiverLeakedViolation;
import android.os.strictmode.LeakedClosableViolation;
import android.os.strictmode.NetworkViolation;
import android.os.strictmode.NonSdkApiUsedViolation;
import android.os.strictmode.ResourceMismatchViolation;
import android.os.strictmode.ServiceConnectionLeakedViolation;
import android.os.strictmode.SqliteObjectLeakedViolation;
import android.os.strictmode.UnbufferedIoViolation;
import android.os.strictmode.UntaggedSocketViolation;
import android.os.strictmode.Violation;
import android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation;
import android.service.notification.ZenModeConfig;
import android.telephony.TelephonyManager;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Printer;
import android.util.Singleton;
import android.view.IWindowManager;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.RuntimeInit;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.HexDump;
import dalvik.system.BlockGuard;
import dalvik.system.CloseGuard;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class StrictMode {
    private static final int ALL_THREAD_DETECT_BITS = 63;
    private static final int ALL_VM_DETECT_BITS = -1073676544;
    public static final String CLEARTEXT_DETECTED_MSG = "Detected cleartext network traffic from UID ";
    private static final String CLEARTEXT_PROPERTY = "persist.sys.strictmode.clear";
    public static final int DETECT_CUSTOM = 8;
    public static final int DETECT_DISK_READ = 2;
    public static final int DETECT_DISK_WRITE = 1;
    public static final int DETECT_NETWORK = 4;
    public static final int DETECT_RESOURCE_MISMATCH = 16;
    public static final int DETECT_UNBUFFERED_IO = 32;
    public static final int DETECT_VM_ACTIVITY_LEAKS = 1024;
    public static final int DETECT_VM_CLEARTEXT_NETWORK = 16384;
    public static final int DETECT_VM_CLOSABLE_LEAKS = 512;
    public static final int DETECT_VM_CONTENT_URI_WITHOUT_PERMISSION = 32768;
    public static final int DETECT_VM_CURSOR_LEAKS = 256;
    public static final int DETECT_VM_FILE_URI_EXPOSURE = 8192;
    public static final int DETECT_VM_INSTANCE_LEAKS = 2048;
    public static final int DETECT_VM_NON_SDK_API_USAGE = 1073741824;
    public static final int DETECT_VM_REGISTRATION_LEAKS = 4096;
    public static final int DETECT_VM_UNTAGGED_SOCKET = Integer.MIN_VALUE;
    private static final boolean DISABLE = false;
    public static final String DISABLE_PROPERTY = "persist.sys.strictmode.disable";
    private static final int MAX_OFFENSES_PER_LOOP = 10;
    private static final int MAX_SPAN_TAGS = 20;
    private static final long MIN_DIALOG_INTERVAL_MS = 30000;
    private static final long MIN_LOG_INTERVAL_MS = 1000;
    private static final long MIN_VM_INTERVAL_MS = 1000;
    public static final int NETWORK_POLICY_ACCEPT = 0;
    public static final int NETWORK_POLICY_LOG = 1;
    public static final int NETWORK_POLICY_REJECT = 2;
    public static final int PENALTY_DEATH = 262144;
    public static final int PENALTY_DEATH_ON_CLEARTEXT_NETWORK = 33554432;
    public static final int PENALTY_DEATH_ON_FILE_URI_EXPOSURE = 67108864;
    public static final int PENALTY_DEATH_ON_NETWORK = 16777216;
    public static final int PENALTY_DIALOG = 131072;
    public static final int PENALTY_DROPBOX = 2097152;
    public static final int PENALTY_FLASH = 1048576;
    public static final int PENALTY_GATHER = 4194304;
    public static final int PENALTY_LOG = 65536;
    private static final int THREAD_PENALTY_MASK = 24576000;
    public static final String VISUAL_PROPERTY = "persist.sys.strictmode.visual";
    private static final int VM_PENALTY_MASK = 103088128;
    private static final String TAG = "StrictMode";
    private static final boolean LOG_V = Log.isLoggable(TAG, 2);
    private static final HashMap<Class, Integer> EMPTY_CLASS_LIMIT_MAP = new HashMap<>();
    private static volatile VmPolicy sVmPolicy = VmPolicy.LAX;
    private static final ViolationLogger LOGCAT_LOGGER = new ViolationLogger() {
        @Override
        public final void log(StrictMode.ViolationInfo violationInfo) {
            StrictMode.lambda$static$0(violationInfo);
        }
    };
    private static volatile ViolationLogger sLogger = LOGCAT_LOGGER;
    private static final ThreadLocal<OnThreadViolationListener> sThreadViolationListener = new ThreadLocal<>();
    private static final ThreadLocal<Executor> sThreadViolationExecutor = new ThreadLocal<>();
    private static final AtomicInteger sDropboxCallsInFlight = new AtomicInteger(0);
    private static final Consumer<String> sNonSdkApiUsageConsumer = new Consumer() {
        @Override
        public final void accept(Object obj) {
            StrictMode.onVmPolicyViolation(new NonSdkApiUsedViolation((String) obj));
        }
    };
    private static final ThreadLocal<ArrayList<ViolationInfo>> gatheredViolations = new ThreadLocal<ArrayList<ViolationInfo>>() {
        @Override
        protected ArrayList<ViolationInfo> initialValue() {
            return null;
        }
    };
    private static final ThreadLocal<ArrayList<ViolationInfo>> violationsBeingTimed = new ThreadLocal<ArrayList<ViolationInfo>>() {
        @Override
        protected ArrayList<ViolationInfo> initialValue() {
            return new ArrayList<>();
        }
    };
    private static final ThreadLocal<Handler> THREAD_HANDLER = new ThreadLocal<Handler>() {
        @Override
        protected Handler initialValue() {
            return new Handler();
        }
    };
    private static final ThreadLocal<AndroidBlockGuardPolicy> THREAD_ANDROID_POLICY = new ThreadLocal<AndroidBlockGuardPolicy>() {
        @Override
        protected AndroidBlockGuardPolicy initialValue() {
            return new AndroidBlockGuardPolicy(0);
        }
    };
    private static long sLastInstanceCountCheckMillis = 0;
    private static boolean sIsIdlerRegistered = false;
    private static final MessageQueue.IdleHandler sProcessIdleHandler = new MessageQueue.IdleHandler() {
        @Override
        public boolean queueIdle() {
            long jUptimeMillis = SystemClock.uptimeMillis();
            if (jUptimeMillis - StrictMode.sLastInstanceCountCheckMillis > 30000) {
                long unused = StrictMode.sLastInstanceCountCheckMillis = jUptimeMillis;
                StrictMode.conditionallyCheckInstanceCounts();
                return true;
            }
            return true;
        }
    };
    private static final HashMap<Integer, Long> sLastVmViolationTime = new HashMap<>();
    private static final Span NO_OP_SPAN = new Span() {
        @Override
        public void finish() {
        }
    };
    private static final ThreadLocal<ThreadSpanState> sThisThreadSpanState = new ThreadLocal<ThreadSpanState>() {
        @Override
        protected ThreadSpanState initialValue() {
            return new ThreadSpanState();
        }
    };
    private static Singleton<IWindowManager> sWindowManager = new Singleton<IWindowManager>() {
        @Override
        protected IWindowManager create() {
            return IWindowManager.Stub.asInterface(ServiceManager.getService(Context.WINDOW_SERVICE));
        }
    };

    @GuardedBy("StrictMode.class")
    private static final HashMap<Class, Integer> sExpectedActivityInstanceCount = new HashMap<>();

    public interface OnThreadViolationListener {
        void onThreadViolation(Violation violation);
    }

    public interface OnVmViolationListener {
        void onVmViolation(Violation violation);
    }

    public interface ViolationLogger {
        void log(ViolationInfo violationInfo);
    }

    static void lambda$static$0(ViolationInfo violationInfo) {
        String str;
        if (violationInfo.durationMillis != -1) {
            str = "StrictMode policy violation; ~duration=" + violationInfo.durationMillis + " ms:";
        } else {
            str = "StrictMode policy violation:";
        }
        Log.d(TAG, str + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + violationInfo.getStackTrace());
    }

    public static void setViolationLogger(ViolationLogger violationLogger) {
        if (violationLogger == null) {
            violationLogger = LOGCAT_LOGGER;
        }
        sLogger = violationLogger;
    }

    private StrictMode() {
    }

    public static final class ThreadPolicy {
        public static final ThreadPolicy LAX = new ThreadPolicy(0, null, null);
        final Executor mCallbackExecutor;
        final OnThreadViolationListener mListener;
        final int mask;

        private ThreadPolicy(int i, OnThreadViolationListener onThreadViolationListener, Executor executor) {
            this.mask = i;
            this.mListener = onThreadViolationListener;
            this.mCallbackExecutor = executor;
        }

        public String toString() {
            return "[StrictMode.ThreadPolicy; mask=" + this.mask + "]";
        }

        public static final class Builder {
            private Executor mExecutor;
            private OnThreadViolationListener mListener;
            private int mMask;

            public Builder() {
                this.mMask = 0;
                this.mMask = 0;
            }

            public Builder(ThreadPolicy threadPolicy) {
                this.mMask = 0;
                this.mMask = threadPolicy.mask;
                this.mListener = threadPolicy.mListener;
                this.mExecutor = threadPolicy.mCallbackExecutor;
            }

            public Builder detectAll() {
                detectDiskReads();
                detectDiskWrites();
                detectNetwork();
                int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
                if (targetSdkVersion >= 11) {
                    detectCustomSlowCalls();
                }
                if (targetSdkVersion >= 23) {
                    detectResourceMismatches();
                }
                if (targetSdkVersion >= 26) {
                    detectUnbufferedIo();
                }
                return this;
            }

            public Builder permitAll() {
                return disable(63);
            }

            public Builder detectNetwork() {
                return enable(4);
            }

            public Builder permitNetwork() {
                return disable(4);
            }

            public Builder detectDiskReads() {
                return enable(2);
            }

            public Builder permitDiskReads() {
                return disable(2);
            }

            public Builder detectCustomSlowCalls() {
                return enable(8);
            }

            public Builder permitCustomSlowCalls() {
                return disable(8);
            }

            public Builder permitResourceMismatches() {
                return disable(16);
            }

            public Builder detectUnbufferedIo() {
                return enable(32);
            }

            public Builder permitUnbufferedIo() {
                return disable(32);
            }

            public Builder detectResourceMismatches() {
                return enable(16);
            }

            public Builder detectDiskWrites() {
                return enable(1);
            }

            public Builder permitDiskWrites() {
                return disable(1);
            }

            public Builder penaltyDialog() {
                return enable(131072);
            }

            public Builder penaltyDeath() {
                return enable(262144);
            }

            public Builder penaltyDeathOnNetwork() {
                return enable(16777216);
            }

            public Builder penaltyFlashScreen() {
                return enable(1048576);
            }

            public Builder penaltyLog() {
                return enable(65536);
            }

            public Builder penaltyDropBox() {
                return enable(2097152);
            }

            public Builder penaltyListener(Executor executor, OnThreadViolationListener onThreadViolationListener) {
                if (executor == null) {
                    throw new NullPointerException("executor must not be null");
                }
                this.mListener = onThreadViolationListener;
                this.mExecutor = executor;
                return this;
            }

            public Builder penaltyListener(OnThreadViolationListener onThreadViolationListener, Executor executor) {
                return penaltyListener(executor, onThreadViolationListener);
            }

            private Builder enable(int i) {
                this.mMask = i | this.mMask;
                return this;
            }

            private Builder disable(int i) {
                this.mMask = (~i) & this.mMask;
                return this;
            }

            public ThreadPolicy build() {
                if (this.mListener == null && this.mMask != 0 && (this.mMask & 2555904) == 0) {
                    penaltyLog();
                }
                return new ThreadPolicy(this.mMask, this.mListener, this.mExecutor);
            }
        }
    }

    public static final class VmPolicy {
        public static final VmPolicy LAX = new VmPolicy(0, StrictMode.EMPTY_CLASS_LIMIT_MAP, null, null);
        final HashMap<Class, Integer> classInstanceLimit;
        final Executor mCallbackExecutor;
        final OnVmViolationListener mListener;
        final int mask;

        private VmPolicy(int i, HashMap<Class, Integer> map, OnVmViolationListener onVmViolationListener, Executor executor) {
            if (map == null) {
                throw new NullPointerException("classInstanceLimit == null");
            }
            this.mask = i;
            this.classInstanceLimit = map;
            this.mListener = onVmViolationListener;
            this.mCallbackExecutor = executor;
        }

        public String toString() {
            return "[StrictMode.VmPolicy; mask=" + this.mask + "]";
        }

        public static final class Builder {
            private HashMap<Class, Integer> mClassInstanceLimit;
            private boolean mClassInstanceLimitNeedCow;
            private Executor mExecutor;
            private OnVmViolationListener mListener;
            private int mMask;

            public Builder() {
                this.mClassInstanceLimitNeedCow = false;
                this.mMask = 0;
            }

            public Builder(VmPolicy vmPolicy) {
                this.mClassInstanceLimitNeedCow = false;
                this.mMask = vmPolicy.mask;
                this.mClassInstanceLimitNeedCow = true;
                this.mClassInstanceLimit = vmPolicy.classInstanceLimit;
                this.mListener = vmPolicy.mListener;
                this.mExecutor = vmPolicy.mCallbackExecutor;
            }

            public Builder setClassInstanceLimit(Class cls, int i) {
                if (cls == null) {
                    throw new NullPointerException("klass == null");
                }
                if (this.mClassInstanceLimitNeedCow) {
                    if (this.mClassInstanceLimit.containsKey(cls) && this.mClassInstanceLimit.get(cls).intValue() == i) {
                        return this;
                    }
                    this.mClassInstanceLimitNeedCow = false;
                    this.mClassInstanceLimit = (HashMap) this.mClassInstanceLimit.clone();
                } else if (this.mClassInstanceLimit == null) {
                    this.mClassInstanceLimit = new HashMap<>();
                }
                this.mMask |= 2048;
                this.mClassInstanceLimit.put(cls, Integer.valueOf(i));
                return this;
            }

            public Builder detectActivityLeaks() {
                return enable(1024);
            }

            public Builder permitActivityLeaks() {
                return disable(1024);
            }

            public Builder detectNonSdkApiUsage() {
                return enable(1073741824);
            }

            public Builder permitNonSdkApiUsage() {
                return disable(1073741824);
            }

            public Builder detectAll() {
                detectLeakedSqlLiteObjects();
                int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
                if (targetSdkVersion >= 11) {
                    detectActivityLeaks();
                    detectLeakedClosableObjects();
                }
                if (targetSdkVersion >= 16) {
                    detectLeakedRegistrationObjects();
                }
                if (targetSdkVersion >= 18) {
                    detectFileUriExposure();
                }
                if (targetSdkVersion >= 23 && SystemProperties.getBoolean(StrictMode.CLEARTEXT_PROPERTY, false)) {
                    detectCleartextNetwork();
                }
                if (targetSdkVersion >= 26) {
                    detectContentUriWithoutPermission();
                    detectUntaggedSockets();
                }
                return this;
            }

            public Builder detectLeakedSqlLiteObjects() {
                return enable(256);
            }

            public Builder detectLeakedClosableObjects() {
                return enable(512);
            }

            public Builder detectLeakedRegistrationObjects() {
                return enable(4096);
            }

            public Builder detectFileUriExposure() {
                return enable(8192);
            }

            public Builder detectCleartextNetwork() {
                return enable(16384);
            }

            public Builder detectContentUriWithoutPermission() {
                return enable(32768);
            }

            public Builder detectUntaggedSockets() {
                return enable(Integer.MIN_VALUE);
            }

            public Builder permitUntaggedSockets() {
                return disable(Integer.MIN_VALUE);
            }

            public Builder penaltyDeath() {
                return enable(262144);
            }

            public Builder penaltyDeathOnCleartextNetwork() {
                return enable(33554432);
            }

            public Builder penaltyDeathOnFileUriExposure() {
                return enable(67108864);
            }

            public Builder penaltyLog() {
                return enable(65536);
            }

            public Builder penaltyDropBox() {
                return enable(2097152);
            }

            public Builder penaltyListener(Executor executor, OnVmViolationListener onVmViolationListener) {
                if (executor == null) {
                    throw new NullPointerException("executor must not be null");
                }
                this.mListener = onVmViolationListener;
                this.mExecutor = executor;
                return this;
            }

            public Builder penaltyListener(OnVmViolationListener onVmViolationListener, Executor executor) {
                return penaltyListener(executor, onVmViolationListener);
            }

            private Builder enable(int i) {
                this.mMask = i | this.mMask;
                return this;
            }

            Builder disable(int i) {
                this.mMask = (~i) & this.mMask;
                return this;
            }

            public VmPolicy build() {
                if (this.mListener == null && this.mMask != 0 && (this.mMask & 2555904) == 0) {
                    penaltyLog();
                }
                return new VmPolicy(this.mMask, this.mClassInstanceLimit != null ? this.mClassInstanceLimit : StrictMode.EMPTY_CLASS_LIMIT_MAP, this.mListener, this.mExecutor);
            }
        }
    }

    public static void setThreadPolicy(ThreadPolicy threadPolicy) {
        setThreadPolicyMask(threadPolicy.mask);
        sThreadViolationListener.set(threadPolicy.mListener);
        sThreadViolationExecutor.set(threadPolicy.mCallbackExecutor);
    }

    public static void setThreadPolicyMask(int i) {
        setBlockGuardPolicy(i);
        Binder.setThreadStrictModePolicy(i);
    }

    private static void setBlockGuardPolicy(int i) {
        AndroidBlockGuardPolicy androidBlockGuardPolicy;
        if (i == 0) {
            BlockGuard.setThreadPolicy(BlockGuard.LAX_POLICY);
            return;
        }
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (threadPolicy instanceof AndroidBlockGuardPolicy) {
            androidBlockGuardPolicy = (AndroidBlockGuardPolicy) threadPolicy;
        } else {
            androidBlockGuardPolicy = THREAD_ANDROID_POLICY.get();
            BlockGuard.setThreadPolicy(androidBlockGuardPolicy);
        }
        androidBlockGuardPolicy.setPolicyMask(i);
    }

    private static void setCloseGuardEnabled(boolean z) {
        if (!(CloseGuard.getReporter() instanceof AndroidCloseGuardReporter)) {
            CloseGuard.setReporter(new AndroidCloseGuardReporter());
        }
        CloseGuard.setEnabled(z);
    }

    public static int getThreadPolicyMask() {
        return BlockGuard.getThreadPolicy().getPolicyMask();
    }

    public static ThreadPolicy getThreadPolicy() {
        return new ThreadPolicy(getThreadPolicyMask(), sThreadViolationListener.get(), sThreadViolationExecutor.get());
    }

    public static ThreadPolicy allowThreadDiskWrites() {
        return new ThreadPolicy(allowThreadDiskWritesMask(), sThreadViolationListener.get(), sThreadViolationExecutor.get());
    }

    public static int allowThreadDiskWritesMask() {
        int threadPolicyMask = getThreadPolicyMask();
        int i = threadPolicyMask & (-4);
        if (i != threadPolicyMask) {
            setThreadPolicyMask(i);
        }
        return threadPolicyMask;
    }

    public static ThreadPolicy allowThreadDiskReads() {
        return new ThreadPolicy(allowThreadDiskReadsMask(), sThreadViolationListener.get(), sThreadViolationExecutor.get());
    }

    public static int allowThreadDiskReadsMask() {
        int threadPolicyMask = getThreadPolicyMask();
        int i = threadPolicyMask & (-3);
        if (i != threadPolicyMask) {
            setThreadPolicyMask(i);
        }
        return threadPolicyMask;
    }

    private static ThreadPolicy allowThreadViolations() {
        ThreadPolicy threadPolicy = getThreadPolicy();
        setThreadPolicyMask(0);
        return threadPolicy;
    }

    private static VmPolicy allowVmViolations() {
        VmPolicy vmPolicy = getVmPolicy();
        sVmPolicy = VmPolicy.LAX;
        return vmPolicy;
    }

    public static boolean isBundledSystemApp(ApplicationInfo applicationInfo) {
        if (applicationInfo == null || applicationInfo.packageName == null) {
            return true;
        }
        if (!applicationInfo.isSystemApp() || applicationInfo.packageName.equals("com.android.vending") || applicationInfo.packageName.equals("com.android.chrome") || applicationInfo.packageName.equals(TelephonyManager.PHONE_PROCESS_NAME)) {
            return false;
        }
        return applicationInfo.packageName.equals(ZenModeConfig.SYSTEM_AUTHORITY) || applicationInfo.packageName.startsWith("android.") || applicationInfo.packageName.startsWith("com.android.");
    }

    public static void initThreadDefaults(ApplicationInfo applicationInfo) {
        ThreadPolicy.Builder builder = new ThreadPolicy.Builder();
        if ((applicationInfo != null ? applicationInfo.targetSdkVersion : 10000) >= 11) {
            builder.detectNetwork();
            builder.penaltyDeathOnNetwork();
        }
        if (!Build.IS_USER && !SystemProperties.getBoolean(DISABLE_PROPERTY, false)) {
            if (Build.IS_USERDEBUG) {
                if (isBundledSystemApp(applicationInfo)) {
                    builder.detectAll();
                    builder.penaltyDropBox();
                    if (SystemProperties.getBoolean(VISUAL_PROPERTY, false)) {
                        builder.penaltyFlashScreen();
                    }
                }
            } else if (Build.IS_ENG && isBundledSystemApp(applicationInfo)) {
                builder.detectAll();
                builder.penaltyDropBox();
                builder.penaltyLog();
                builder.penaltyFlashScreen();
            }
        }
        setThreadPolicy(builder.build());
    }

    public static void initVmDefaults(ApplicationInfo applicationInfo) {
        VmPolicy.Builder builder = new VmPolicy.Builder();
        if ((applicationInfo != null ? applicationInfo.targetSdkVersion : 10000) >= 24) {
            builder.detectFileUriExposure();
            builder.penaltyDeathOnFileUriExposure();
        }
        if (!Build.IS_USER && !SystemProperties.getBoolean(DISABLE_PROPERTY, false)) {
            if (Build.IS_USERDEBUG) {
                if (isBundledSystemApp(applicationInfo)) {
                    builder.detectAll();
                    builder.permitActivityLeaks();
                    builder.penaltyDropBox();
                }
            } else if (Build.IS_ENG && isBundledSystemApp(applicationInfo)) {
                builder.detectAll();
                builder.penaltyDropBox();
                builder.penaltyLog();
            }
        }
        setVmPolicy(builder.build());
    }

    public static void enableDeathOnFileUriExposure() {
        sVmPolicy = new VmPolicy(67108864 | sVmPolicy.mask | 8192, sVmPolicy.classInstanceLimit, sVmPolicy.mListener, sVmPolicy.mCallbackExecutor);
    }

    public static void disableDeathOnFileUriExposure() {
        sVmPolicy = new VmPolicy((-67117057) & sVmPolicy.mask, sVmPolicy.classInstanceLimit, sVmPolicy.mListener, sVmPolicy.mCallbackExecutor);
    }

    private static int parsePolicyFromMessage(String str) {
        int iIndexOf;
        if (str == null || !str.startsWith("policy=") || (iIndexOf = str.indexOf(32)) == -1) {
            return 0;
        }
        try {
            return Integer.parseInt(str.substring(7, iIndexOf));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean tooManyViolationsThisLoop() {
        return violationsBeingTimed.get().size() >= 10;
    }

    private static class AndroidBlockGuardPolicy implements BlockGuard.Policy {
        private ArrayMap<Integer, Long> mLastViolationTime;
        private int mPolicyMask;

        public AndroidBlockGuardPolicy(int i) {
            this.mPolicyMask = i;
        }

        public String toString() {
            return "AndroidBlockGuardPolicy; mPolicyMask=" + this.mPolicyMask;
        }

        public int getPolicyMask() {
            return this.mPolicyMask;
        }

        public void onWriteToDisk() {
            if ((this.mPolicyMask & 1) == 0 || StrictMode.tooManyViolationsThisLoop()) {
                return;
            }
            startHandlingViolationException(new DiskWriteViolation());
        }

        void onCustomSlowCall(String str) {
            if ((this.mPolicyMask & 8) == 0 || StrictMode.tooManyViolationsThisLoop()) {
                return;
            }
            startHandlingViolationException(new CustomViolation(str));
        }

        void onResourceMismatch(Object obj) {
            if ((this.mPolicyMask & 16) == 0 || StrictMode.tooManyViolationsThisLoop()) {
                return;
            }
            startHandlingViolationException(new ResourceMismatchViolation(obj));
        }

        public void onUnbufferedIO() {
            if ((this.mPolicyMask & 32) == 0 || StrictMode.tooManyViolationsThisLoop()) {
                return;
            }
            startHandlingViolationException(new UnbufferedIoViolation());
        }

        public void onReadFromDisk() {
            if ((this.mPolicyMask & 2) == 0 || StrictMode.tooManyViolationsThisLoop()) {
                return;
            }
            startHandlingViolationException(new DiskReadViolation());
        }

        public void onNetwork() {
            if ((this.mPolicyMask & 4) == 0) {
                return;
            }
            if ((this.mPolicyMask & 16777216) == 0) {
                if (StrictMode.tooManyViolationsThisLoop()) {
                    return;
                }
                startHandlingViolationException(new NetworkViolation());
                return;
            }
            throw new NetworkOnMainThreadException();
        }

        public void setPolicyMask(int i) {
            this.mPolicyMask = i;
        }

        void startHandlingViolationException(Violation violation) {
            ViolationInfo violationInfo = new ViolationInfo(violation, this.mPolicyMask);
            violationInfo.violationUptimeMillis = SystemClock.uptimeMillis();
            handleViolationWithTimingAttempt(violationInfo);
        }

        void handleViolationWithTimingAttempt(ViolationInfo violationInfo) {
            if (Looper.myLooper() != null && (violationInfo.mPolicy & StrictMode.THREAD_PENALTY_MASK) != 262144) {
                final ArrayList arrayList = (ArrayList) StrictMode.violationsBeingTimed.get();
                if (arrayList.size() >= 10) {
                    return;
                }
                arrayList.add(violationInfo);
                if (arrayList.size() > 1) {
                    return;
                }
                final IWindowManager iWindowManager = violationInfo.penaltyEnabled(1048576) ? (IWindowManager) StrictMode.sWindowManager.get() : null;
                if (iWindowManager != null) {
                    try {
                        iWindowManager.showStrictModeViolation(true);
                    } catch (RemoteException e) {
                    }
                }
                ((Handler) StrictMode.THREAD_HANDLER.get()).postAtFrontOfQueue(new Runnable() {
                    @Override
                    public final void run() {
                        StrictMode.AndroidBlockGuardPolicy.lambda$handleViolationWithTimingAttempt$0(this.f$0, iWindowManager, arrayList);
                    }
                });
                return;
            }
            violationInfo.durationMillis = -1;
            onThreadPolicyViolation(violationInfo);
        }

        public static void lambda$handleViolationWithTimingAttempt$0(AndroidBlockGuardPolicy androidBlockGuardPolicy, IWindowManager iWindowManager, ArrayList arrayList) {
            long jUptimeMillis = SystemClock.uptimeMillis();
            int i = 0;
            if (iWindowManager != null) {
                try {
                    iWindowManager.showStrictModeViolation(false);
                } catch (RemoteException e) {
                }
            }
            while (i < arrayList.size()) {
                ViolationInfo violationInfo = (ViolationInfo) arrayList.get(i);
                i++;
                violationInfo.violationNumThisLoop = i;
                violationInfo.durationMillis = (int) (jUptimeMillis - violationInfo.violationUptimeMillis);
                androidBlockGuardPolicy.onThreadPolicyViolation(violationInfo);
            }
            arrayList.clear();
        }

        void onThreadPolicyViolation(ViolationInfo violationInfo) {
            long jLongValue;
            if (StrictMode.LOG_V) {
                Log.d(StrictMode.TAG, "onThreadPolicyViolation; policy=" + violationInfo.mPolicy);
            }
            if (violationInfo.penaltyEnabled(4194304)) {
                ArrayList arrayList = (ArrayList) StrictMode.gatheredViolations.get();
                if (arrayList == null) {
                    arrayList = new ArrayList(1);
                    StrictMode.gatheredViolations.set(arrayList);
                }
                Iterator it = arrayList.iterator();
                while (it.hasNext()) {
                    if (violationInfo.getStackTrace().equals(((ViolationInfo) it.next()).getStackTrace())) {
                        return;
                    }
                }
                arrayList.add(violationInfo);
                return;
            }
            Integer numValueOf = Integer.valueOf(violationInfo.hashCode());
            if (this.mLastViolationTime != null) {
                Long l = this.mLastViolationTime.get(numValueOf);
                if (l != null) {
                    jLongValue = l.longValue();
                } else {
                    jLongValue = 0;
                }
            } else {
                this.mLastViolationTime = new ArrayMap<>(1);
                jLongValue = 0;
            }
            long jUptimeMillis = SystemClock.uptimeMillis();
            this.mLastViolationTime.put(numValueOf, Long.valueOf(jUptimeMillis));
            long j = jLongValue == 0 ? Long.MAX_VALUE : jUptimeMillis - jLongValue;
            if (violationInfo.penaltyEnabled(65536) && j > 1000) {
                StrictMode.sLogger.log(violationInfo);
            }
            final Violation violation = violationInfo.mViolation;
            int i = 131072;
            if (!violationInfo.penaltyEnabled(131072) || j <= 30000) {
                i = 0;
            }
            if (violationInfo.penaltyEnabled(2097152) && jLongValue == 0) {
                i |= 2097152;
            }
            if (i != 0) {
                int violationBit = violationInfo.getViolationBit() | i;
                if ((violationInfo.mPolicy & StrictMode.THREAD_PENALTY_MASK) == 2097152) {
                    StrictMode.dropboxViolationAsync(violationBit, violationInfo);
                } else {
                    StrictMode.handleApplicationStrictModeViolation(violationBit, violationInfo);
                }
            }
            if ((violationInfo.getPolicyMask() & 262144) == 0) {
                final OnThreadViolationListener onThreadViolationListener = (OnThreadViolationListener) StrictMode.sThreadViolationListener.get();
                Executor executor = (Executor) StrictMode.sThreadViolationExecutor.get();
                if (onThreadViolationListener != null && executor != null) {
                    try {
                        executor.execute(new Runnable() {
                            @Override
                            public final void run() {
                                StrictMode.AndroidBlockGuardPolicy.lambda$onThreadPolicyViolation$1(onThreadViolationListener, violation);
                            }
                        });
                        return;
                    } catch (RejectedExecutionException e) {
                        Log.e(StrictMode.TAG, "ThreadPolicy penaltyCallback failed", e);
                        return;
                    }
                }
                return;
            }
            throw new RuntimeException("StrictMode ThreadPolicy violation", violation);
        }

        static void lambda$onThreadPolicyViolation$1(OnThreadViolationListener onThreadViolationListener, Violation violation) {
            ThreadPolicy threadPolicyAllowThreadViolations = StrictMode.allowThreadViolations();
            try {
                onThreadViolationListener.onThreadViolation(violation);
            } finally {
                StrictMode.setThreadPolicy(threadPolicyAllowThreadViolations);
            }
        }
    }

    private static void dropboxViolationAsync(final int i, final ViolationInfo violationInfo) {
        int iIncrementAndGet = sDropboxCallsInFlight.incrementAndGet();
        if (iIncrementAndGet > 20) {
            sDropboxCallsInFlight.decrementAndGet();
            return;
        }
        if (LOG_V) {
            Log.d(TAG, "Dropboxing async; in-flight=" + iIncrementAndGet);
        }
        BackgroundThread.getHandler().post(new Runnable() {
            @Override
            public final void run() {
                StrictMode.lambda$dropboxViolationAsync$2(i, violationInfo);
            }
        });
    }

    static void lambda$dropboxViolationAsync$2(int i, ViolationInfo violationInfo) {
        handleApplicationStrictModeViolation(i, violationInfo);
        int iDecrementAndGet = sDropboxCallsInFlight.decrementAndGet();
        if (LOG_V) {
            Log.d(TAG, "Dropbox complete; in-flight=" + iDecrementAndGet);
        }
    }

    private static void handleApplicationStrictModeViolation(int i, ViolationInfo violationInfo) {
        int threadPolicyMask = getThreadPolicyMask();
        try {
            try {
                setThreadPolicyMask(0);
                IActivityManager service = ActivityManager.getService();
                if (service == null) {
                    Log.w(TAG, "No activity manager; failed to Dropbox violation.");
                } else {
                    service.handleApplicationStrictModeViolation(RuntimeInit.getApplicationObject(), i, violationInfo);
                }
            } catch (RemoteException e) {
                if (!(e instanceof DeadObjectException)) {
                    Log.e(TAG, "RemoteException handling StrictMode violation", e);
                }
            }
        } finally {
            setThreadPolicyMask(threadPolicyMask);
        }
    }

    private static class AndroidCloseGuardReporter implements CloseGuard.Reporter {
        private AndroidCloseGuardReporter() {
        }

        public void report(String str, Throwable th) {
            StrictMode.onVmPolicyViolation(new LeakedClosableViolation(str, th));
        }
    }

    static boolean hasGatheredViolations() {
        return gatheredViolations.get() != null;
    }

    static void clearGatheredViolations() {
        gatheredViolations.set(null);
    }

    public static void conditionallyCheckInstanceCounts() {
        VmPolicy vmPolicy = getVmPolicy();
        int size = vmPolicy.classInstanceLimit.size();
        if (size == 0) {
            return;
        }
        System.gc();
        System.runFinalization();
        System.gc();
        Class[] clsArr = (Class[]) vmPolicy.classInstanceLimit.keySet().toArray(new Class[size]);
        long[] jArrCountInstancesOfClasses = VMDebug.countInstancesOfClasses(clsArr, false);
        for (int i = 0; i < clsArr.length; i++) {
            Class cls = clsArr[i];
            int iIntValue = vmPolicy.classInstanceLimit.get(cls).intValue();
            long j = jArrCountInstancesOfClasses[i];
            if (j > iIntValue) {
                onVmPolicyViolation(new InstanceCountViolation(cls, j, iIntValue));
            }
        }
    }

    public static void setVmPolicy(VmPolicy vmPolicy) {
        synchronized (StrictMode.class) {
            sVmPolicy = vmPolicy;
            setCloseGuardEnabled(vmClosableObjectLeaksEnabled());
            Looper mainLooper = Looper.getMainLooper();
            if (mainLooper != null) {
                MessageQueue messageQueue = mainLooper.mQueue;
                if (vmPolicy.classInstanceLimit.size() == 0 || (sVmPolicy.mask & VM_PENALTY_MASK) == 0) {
                    messageQueue.removeIdleHandler(sProcessIdleHandler);
                    sIsIdlerRegistered = false;
                } else if (!sIsIdlerRegistered) {
                    messageQueue.addIdleHandler(sProcessIdleHandler);
                    sIsIdlerRegistered = true;
                }
            }
            int i = (sVmPolicy.mask & 16384) != 0 ? ((sVmPolicy.mask & 262144) == 0 && (sVmPolicy.mask & 33554432) == 0) ? 1 : 2 : 0;
            INetworkManagementService iNetworkManagementServiceAsInterface = INetworkManagementService.Stub.asInterface(ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE));
            if (iNetworkManagementServiceAsInterface != null) {
                try {
                    iNetworkManagementServiceAsInterface.setUidCleartextNetworkPolicy(Process.myUid(), i);
                } catch (RemoteException e) {
                }
            } else if (i != 0) {
                Log.w(TAG, "Dropping requested network policy due to missing service!");
            }
            if ((sVmPolicy.mask & 1073741824) != 0) {
                VMRuntime.setNonSdkApiUsageConsumer(sNonSdkApiUsageConsumer);
                VMRuntime.setDedupeHiddenApiWarnings(false);
            } else {
                VMRuntime.setNonSdkApiUsageConsumer((Consumer) null);
                VMRuntime.setDedupeHiddenApiWarnings(true);
            }
        }
    }

    public static VmPolicy getVmPolicy() {
        VmPolicy vmPolicy;
        synchronized (StrictMode.class) {
            vmPolicy = sVmPolicy;
        }
        return vmPolicy;
    }

    public static void enableDefaults() {
        setThreadPolicy(new ThreadPolicy.Builder().detectAll().penaltyLog().build());
        setVmPolicy(new VmPolicy.Builder().detectAll().penaltyLog().build());
    }

    public static boolean vmSqliteObjectLeaksEnabled() {
        return (sVmPolicy.mask & 256) != 0;
    }

    public static boolean vmClosableObjectLeaksEnabled() {
        return (sVmPolicy.mask & 512) != 0;
    }

    public static boolean vmRegistrationLeaksEnabled() {
        return (sVmPolicy.mask & 4096) != 0;
    }

    public static boolean vmFileUriExposureEnabled() {
        return (sVmPolicy.mask & 8192) != 0;
    }

    public static boolean vmCleartextNetworkEnabled() {
        return (sVmPolicy.mask & 16384) != 0;
    }

    public static boolean vmContentUriWithoutPermissionEnabled() {
        return (sVmPolicy.mask & 32768) != 0;
    }

    public static boolean vmUntaggedSocketEnabled() {
        return (sVmPolicy.mask & Integer.MIN_VALUE) != 0;
    }

    public static void onSqliteObjectLeaked(String str, Throwable th) {
        onVmPolicyViolation(new SqliteObjectLeakedViolation(str, th));
    }

    public static void onWebViewMethodCalledOnWrongThread(Throwable th) {
        onVmPolicyViolation(new WebViewMethodCalledOnWrongThreadViolation(th));
    }

    public static void onIntentReceiverLeaked(Throwable th) {
        onVmPolicyViolation(new IntentReceiverLeakedViolation(th));
    }

    public static void onServiceConnectionLeaked(Throwable th) {
        onVmPolicyViolation(new ServiceConnectionLeakedViolation(th));
    }

    public static void onFileUriExposed(Uri uri, String str) {
        String str2 = uri + " exposed beyond app through " + str;
        if ((sVmPolicy.mask & 67108864) != 0) {
            throw new FileUriExposedException(str2);
        }
        onVmPolicyViolation(new FileUriExposedViolation(str2));
    }

    public static void onContentUriWithoutPermission(Uri uri, String str) {
        onVmPolicyViolation(new ContentUriWithoutPermissionViolation(uri, str));
    }

    public static void onCleartextNetworkDetected(byte[] bArr) {
        byte[] bArr2;
        if (bArr != null) {
            if (bArr.length >= 20 && (bArr[0] & 240) == 64) {
                bArr2 = new byte[4];
                System.arraycopy(bArr, 16, bArr2, 0, 4);
            } else if (bArr.length >= 40 && (bArr[0] & 240) == 96) {
                bArr2 = new byte[16];
                System.arraycopy(bArr, 24, bArr2, 0, 16);
            }
        } else {
            bArr2 = null;
        }
        String str = CLEARTEXT_DETECTED_MSG + Process.myUid();
        if (bArr2 != null) {
            try {
                str = str + " to " + InetAddress.getByAddress(bArr2);
            } catch (UnknownHostException e) {
            }
        }
        onVmPolicyViolation(new CleartextNetworkViolation(str + HexDump.dumpHexString(bArr).trim() + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER), (sVmPolicy.mask & 33554432) != 0);
    }

    public static void onUntaggedSocket() {
        onVmPolicyViolation(new UntaggedSocketViolation());
    }

    public static void onVmPolicyViolation(Violation violation) {
        onVmPolicyViolation(violation, false);
    }

    public static void onVmPolicyViolation(final Violation violation, boolean z) {
        boolean z2 = (sVmPolicy.mask & 2097152) != 0;
        boolean z3 = (sVmPolicy.mask & 262144) != 0 || z;
        boolean z4 = (sVmPolicy.mask & 65536) != 0;
        ViolationInfo violationInfo = new ViolationInfo(violation, sVmPolicy.mask);
        violationInfo.numAnimationsRunning = 0;
        violationInfo.tags = null;
        violationInfo.broadcastIntentAction = null;
        Integer numValueOf = Integer.valueOf(violationInfo.hashCode());
        long jUptimeMillis = SystemClock.uptimeMillis();
        long jLongValue = Long.MAX_VALUE;
        synchronized (sLastVmViolationTime) {
            if (sLastVmViolationTime.containsKey(numValueOf)) {
                jLongValue = jUptimeMillis - sLastVmViolationTime.get(numValueOf).longValue();
            }
            if (jLongValue > 1000) {
                sLastVmViolationTime.put(numValueOf, Long.valueOf(jUptimeMillis));
            }
        }
        if (jLongValue <= 1000) {
            return;
        }
        if (z4 && sLogger != null && jLongValue > 1000) {
            sLogger.log(violationInfo);
        }
        int i = 2097152 | (ALL_VM_DETECT_BITS & sVmPolicy.mask);
        if (z2) {
            if (z3) {
                handleApplicationStrictModeViolation(i, violationInfo);
            } else {
                dropboxViolationAsync(i, violationInfo);
            }
        }
        if (z3) {
            System.err.println("StrictMode VmPolicy violation with POLICY_DEATH; shutting down.");
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
        if (sVmPolicy.mListener != null && sVmPolicy.mCallbackExecutor != null) {
            final OnVmViolationListener onVmViolationListener = sVmPolicy.mListener;
            try {
                sVmPolicy.mCallbackExecutor.execute(new Runnable() {
                    @Override
                    public final void run() {
                        StrictMode.lambda$onVmPolicyViolation$3(onVmViolationListener, violation);
                    }
                });
            } catch (RejectedExecutionException e) {
                Log.e(TAG, "VmPolicy penaltyCallback failed", e);
            }
        }
    }

    static void lambda$onVmPolicyViolation$3(OnVmViolationListener onVmViolationListener, Violation violation) {
        VmPolicy vmPolicyAllowVmViolations = allowVmViolations();
        try {
            onVmViolationListener.onVmViolation(violation);
        } finally {
            setVmPolicy(vmPolicyAllowVmViolations);
        }
    }

    static void writeGatheredViolationsToParcel(Parcel parcel) {
        ArrayList<ViolationInfo> arrayList = gatheredViolations.get();
        if (arrayList == null) {
            parcel.writeInt(0);
        } else {
            int iMin = Math.min(arrayList.size(), 3);
            parcel.writeInt(iMin);
            for (int i = 0; i < iMin; i++) {
                arrayList.get(i).writeToParcel(parcel, 0);
            }
        }
        gatheredViolations.set(null);
    }

    static void readAndHandleBinderCallViolations(Parcel parcel) {
        Throwable th = new Throwable();
        boolean z = (getThreadPolicyMask() & 4194304) != 0;
        int i = parcel.readInt();
        for (int i2 = 0; i2 < i; i2++) {
            ViolationInfo violationInfo = new ViolationInfo(parcel, !z);
            violationInfo.addLocalStack(th);
            BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
            if (threadPolicy instanceof AndroidBlockGuardPolicy) {
                ((AndroidBlockGuardPolicy) threadPolicy).handleViolationWithTimingAttempt(violationInfo);
            }
        }
    }

    private static void onBinderStrictModePolicyChange(int i) {
        setBlockGuardPolicy(i);
    }

    public static class Span {
        private final ThreadSpanState mContainerState;
        private long mCreateMillis;
        private String mName;
        private Span mNext;
        private Span mPrev;

        Span(ThreadSpanState threadSpanState) {
            this.mContainerState = threadSpanState;
        }

        protected Span() {
            this.mContainerState = null;
        }

        public void finish() {
            ThreadSpanState threadSpanState = this.mContainerState;
            synchronized (threadSpanState) {
                if (this.mName == null) {
                    return;
                }
                if (this.mPrev != null) {
                    this.mPrev.mNext = this.mNext;
                }
                if (this.mNext != null) {
                    this.mNext.mPrev = this.mPrev;
                }
                if (threadSpanState.mActiveHead == this) {
                    threadSpanState.mActiveHead = this.mNext;
                }
                threadSpanState.mActiveSize--;
                if (StrictMode.LOG_V) {
                    Log.d(StrictMode.TAG, "Span finished=" + this.mName + "; size=" + threadSpanState.mActiveSize);
                }
                this.mCreateMillis = -1L;
                this.mName = null;
                this.mPrev = null;
                this.mNext = null;
                if (threadSpanState.mFreeListSize < 5) {
                    this.mNext = threadSpanState.mFreeListHead;
                    threadSpanState.mFreeListHead = this;
                    threadSpanState.mFreeListSize++;
                }
            }
        }
    }

    private static class ThreadSpanState {
        public Span mActiveHead;
        public int mActiveSize;
        public Span mFreeListHead;
        public int mFreeListSize;

        private ThreadSpanState() {
        }
    }

    public static Span enterCriticalSpan(String str) {
        Span span;
        if (Build.IS_USER) {
            return NO_OP_SPAN;
        }
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("name must be non-null and non-empty");
        }
        ThreadSpanState threadSpanState = sThisThreadSpanState.get();
        synchronized (threadSpanState) {
            if (threadSpanState.mFreeListHead != null) {
                span = threadSpanState.mFreeListHead;
                threadSpanState.mFreeListHead = span.mNext;
                threadSpanState.mFreeListSize--;
            } else {
                span = new Span(threadSpanState);
            }
            span.mName = str;
            span.mCreateMillis = SystemClock.uptimeMillis();
            span.mNext = threadSpanState.mActiveHead;
            span.mPrev = null;
            threadSpanState.mActiveHead = span;
            threadSpanState.mActiveSize++;
            if (span.mNext != null) {
                span.mNext.mPrev = span;
            }
            if (LOG_V) {
                Log.d(TAG, "Span enter=" + str + "; size=" + threadSpanState.mActiveSize);
            }
        }
        return span;
    }

    public static void noteSlowCall(String str) {
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (!(threadPolicy instanceof AndroidBlockGuardPolicy)) {
            return;
        }
        ((AndroidBlockGuardPolicy) threadPolicy).onCustomSlowCall(str);
    }

    public static void noteResourceMismatch(Object obj) {
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (!(threadPolicy instanceof AndroidBlockGuardPolicy)) {
            return;
        }
        ((AndroidBlockGuardPolicy) threadPolicy).onResourceMismatch(obj);
    }

    public static void noteUnbufferedIO() {
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (!(threadPolicy instanceof AndroidBlockGuardPolicy)) {
            return;
        }
        threadPolicy.onUnbufferedIO();
    }

    public static void noteDiskRead() {
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (!(threadPolicy instanceof AndroidBlockGuardPolicy)) {
            return;
        }
        threadPolicy.onReadFromDisk();
    }

    public static void noteDiskWrite() {
        BlockGuard.Policy threadPolicy = BlockGuard.getThreadPolicy();
        if (!(threadPolicy instanceof AndroidBlockGuardPolicy)) {
            return;
        }
        threadPolicy.onWriteToDisk();
    }

    public static Object trackActivity(Object obj) {
        return new InstanceTracker(obj);
    }

    public static void incrementExpectedActivityCount(Class cls) {
        if (cls == null) {
            return;
        }
        synchronized (StrictMode.class) {
            if ((sVmPolicy.mask & 1024) == 0) {
                return;
            }
            Integer num = sExpectedActivityInstanceCount.get(cls);
            int iIntValue = 1;
            if (num != null) {
                iIntValue = 1 + num.intValue();
            }
            sExpectedActivityInstanceCount.put(cls, Integer.valueOf(iIntValue));
        }
    }

    public static void decrementExpectedActivityCount(Class cls) {
        if (cls == null) {
            return;
        }
        synchronized (StrictMode.class) {
            if ((sVmPolicy.mask & 1024) == 0) {
                return;
            }
            Integer num = sExpectedActivityInstanceCount.get(cls);
            int iIntValue = (num == null || num.intValue() == 0) ? 0 : num.intValue() - 1;
            if (iIntValue == 0) {
                sExpectedActivityInstanceCount.remove(cls);
            } else {
                sExpectedActivityInstanceCount.put(cls, Integer.valueOf(iIntValue));
            }
            int i = iIntValue + 1;
            if (InstanceTracker.getInstanceCount(cls) <= i) {
                return;
            }
            System.gc();
            System.runFinalization();
            System.gc();
            long jCountInstancesOfClass = VMDebug.countInstancesOfClass(cls, false);
            if (jCountInstancesOfClass > i) {
                onVmPolicyViolation(new InstanceCountViolation(cls, jCountInstancesOfClass, i));
            }
        }
    }

    public static final class ViolationInfo implements Parcelable {
        public static final Parcelable.Creator<ViolationInfo> CREATOR = new Parcelable.Creator<ViolationInfo>() {
            @Override
            public ViolationInfo createFromParcel(Parcel parcel) {
                return new ViolationInfo(parcel);
            }

            @Override
            public ViolationInfo[] newArray(int i) {
                return new ViolationInfo[i];
            }
        };
        public String broadcastIntentAction;
        public int durationMillis;
        private final Deque<StackTraceElement[]> mBinderStack;
        private final int mPolicy;
        private String mStackTrace;
        private final Violation mViolation;
        public int numAnimationsRunning;
        public long numInstances;
        public String[] tags;
        public int violationNumThisLoop;
        public long violationUptimeMillis;

        ViolationInfo(Violation violation, int i) {
            this.mBinderStack = new ArrayDeque();
            this.durationMillis = -1;
            int i2 = 0;
            this.numAnimationsRunning = 0;
            this.numInstances = -1L;
            this.mViolation = violation;
            this.mPolicy = i;
            this.violationUptimeMillis = SystemClock.uptimeMillis();
            this.numAnimationsRunning = ValueAnimator.getCurrentAnimationsCount();
            Intent intentBeingBroadcast = ActivityThread.getIntentBeingBroadcast();
            if (intentBeingBroadcast != null) {
                this.broadcastIntentAction = intentBeingBroadcast.getAction();
            }
            ThreadSpanState threadSpanState = (ThreadSpanState) StrictMode.sThisThreadSpanState.get();
            if (violation instanceof InstanceCountViolation) {
                this.numInstances = ((InstanceCountViolation) violation).getNumberOfInstances();
            }
            synchronized (threadSpanState) {
                int i3 = threadSpanState.mActiveSize;
                i3 = i3 > 20 ? 20 : i3;
                if (i3 != 0) {
                    this.tags = new String[i3];
                    for (Span span = threadSpanState.mActiveHead; span != null && i2 < i3; span = span.mNext) {
                        this.tags[i2] = span.mName;
                        i2++;
                    }
                }
            }
        }

        public String getStackTrace() {
            if (this.mStackTrace == null) {
                StringWriter stringWriter = new StringWriter();
                FastPrintWriter fastPrintWriter = new FastPrintWriter((Writer) stringWriter, false, 256);
                this.mViolation.printStackTrace(fastPrintWriter);
                for (StackTraceElement[] stackTraceElementArr : this.mBinderStack) {
                    fastPrintWriter.append((CharSequence) "# via Binder call with stack:\n");
                    for (StackTraceElement stackTraceElement : stackTraceElementArr) {
                        fastPrintWriter.append((CharSequence) "\tat ");
                        fastPrintWriter.append((CharSequence) stackTraceElement.toString());
                        fastPrintWriter.append('\n');
                    }
                }
                fastPrintWriter.flush();
                fastPrintWriter.close();
                this.mStackTrace = stringWriter.toString();
            }
            return this.mStackTrace;
        }

        public String getViolationDetails() {
            return this.mViolation.getMessage();
        }

        public int getPolicyMask() {
            return this.mPolicy;
        }

        boolean penaltyEnabled(int i) {
            return (i & this.mPolicy) != 0;
        }

        void addLocalStack(Throwable th) {
            this.mBinderStack.addFirst(th.getStackTrace());
        }

        public int getViolationBit() {
            if (this.mViolation instanceof DiskWriteViolation) {
                return 1;
            }
            if (this.mViolation instanceof DiskReadViolation) {
                return 2;
            }
            if (this.mViolation instanceof NetworkViolation) {
                return 4;
            }
            if (this.mViolation instanceof CustomViolation) {
                return 8;
            }
            if (this.mViolation instanceof ResourceMismatchViolation) {
                return 16;
            }
            if (this.mViolation instanceof UnbufferedIoViolation) {
                return 32;
            }
            if (this.mViolation instanceof SqliteObjectLeakedViolation) {
                return 256;
            }
            if (this.mViolation instanceof LeakedClosableViolation) {
                return 512;
            }
            if (this.mViolation instanceof InstanceCountViolation) {
                return 2048;
            }
            if ((this.mViolation instanceof IntentReceiverLeakedViolation) || (this.mViolation instanceof ServiceConnectionLeakedViolation)) {
                return 4096;
            }
            if (this.mViolation instanceof FileUriExposedViolation) {
                return 8192;
            }
            if (this.mViolation instanceof CleartextNetworkViolation) {
                return 16384;
            }
            if (this.mViolation instanceof ContentUriWithoutPermissionViolation) {
                return 32768;
            }
            if (this.mViolation instanceof UntaggedSocketViolation) {
                return Integer.MIN_VALUE;
            }
            if (this.mViolation instanceof NonSdkApiUsedViolation) {
                return 1073741824;
            }
            throw new IllegalStateException("missing violation bit");
        }

        public int hashCode() {
            int iHashCode;
            if (this.mViolation != null) {
                iHashCode = MetricsProto.MetricsEvent.TEXT_LONGPRESS + this.mViolation.hashCode();
            } else {
                iHashCode = 17;
            }
            if (this.numAnimationsRunning != 0) {
                iHashCode *= 37;
            }
            if (this.broadcastIntentAction != null) {
                iHashCode = (iHashCode * 37) + this.broadcastIntentAction.hashCode();
            }
            if (this.tags != null) {
                for (String str : this.tags) {
                    iHashCode = (iHashCode * 37) + str.hashCode();
                }
            }
            return iHashCode;
        }

        public ViolationInfo(Parcel parcel) {
            this(parcel, false);
        }

        public ViolationInfo(Parcel parcel, boolean z) {
            this.mBinderStack = new ArrayDeque();
            this.durationMillis = -1;
            this.numAnimationsRunning = 0;
            this.numInstances = -1L;
            this.mViolation = (Violation) parcel.readSerializable();
            int i = parcel.readInt();
            for (int i2 = 0; i2 < i; i2++) {
                StackTraceElement[] stackTraceElementArr = new StackTraceElement[parcel.readInt()];
                for (int i3 = 0; i3 < stackTraceElementArr.length; i3++) {
                    stackTraceElementArr[i3] = new StackTraceElement(parcel.readString(), parcel.readString(), parcel.readString(), parcel.readInt());
                }
                this.mBinderStack.add(stackTraceElementArr);
            }
            int i4 = parcel.readInt();
            if (z) {
                this.mPolicy = (-4194305) & i4;
            } else {
                this.mPolicy = i4;
            }
            this.durationMillis = parcel.readInt();
            this.violationNumThisLoop = parcel.readInt();
            this.numAnimationsRunning = parcel.readInt();
            this.violationUptimeMillis = parcel.readLong();
            this.numInstances = parcel.readLong();
            this.broadcastIntentAction = parcel.readString();
            this.tags = parcel.readStringArray();
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeSerializable(this.mViolation);
            parcel.writeInt(this.mBinderStack.size());
            for (StackTraceElement[] stackTraceElementArr : this.mBinderStack) {
                parcel.writeInt(stackTraceElementArr.length);
                for (StackTraceElement stackTraceElement : stackTraceElementArr) {
                    parcel.writeString(stackTraceElement.getClassName());
                    parcel.writeString(stackTraceElement.getMethodName());
                    parcel.writeString(stackTraceElement.getFileName());
                    parcel.writeInt(stackTraceElement.getLineNumber());
                }
            }
            parcel.dataPosition();
            parcel.writeInt(this.mPolicy);
            parcel.writeInt(this.durationMillis);
            parcel.writeInt(this.violationNumThisLoop);
            parcel.writeInt(this.numAnimationsRunning);
            parcel.writeLong(this.violationUptimeMillis);
            parcel.writeLong(this.numInstances);
            parcel.writeString(this.broadcastIntentAction);
            parcel.writeStringArray(this.tags);
            parcel.dataPosition();
        }

        public void dump(Printer printer, String str) {
            printer.println(str + "stackTrace: " + getStackTrace());
            printer.println(str + "policy: " + this.mPolicy);
            if (this.durationMillis != -1) {
                printer.println(str + "durationMillis: " + this.durationMillis);
            }
            if (this.numInstances != -1) {
                printer.println(str + "numInstances: " + this.numInstances);
            }
            if (this.violationNumThisLoop != 0) {
                printer.println(str + "violationNumThisLoop: " + this.violationNumThisLoop);
            }
            if (this.numAnimationsRunning != 0) {
                printer.println(str + "numAnimationsRunning: " + this.numAnimationsRunning);
            }
            printer.println(str + "violationUptimeMillis: " + this.violationUptimeMillis);
            if (this.broadcastIntentAction != null) {
                printer.println(str + "broadcastIntentAction: " + this.broadcastIntentAction);
            }
            if (this.tags != null) {
                String[] strArr = this.tags;
                int length = strArr.length;
                int i = 0;
                int i2 = 0;
                while (i < length) {
                    printer.println(str + "tag[" + i2 + "]: " + strArr[i]);
                    i++;
                    i2++;
                }
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    private static final class InstanceTracker {
        private static final HashMap<Class<?>, Integer> sInstanceCounts = new HashMap<>();
        private final Class<?> mKlass;

        public InstanceTracker(Object obj) {
            this.mKlass = obj.getClass();
            synchronized (sInstanceCounts) {
                Integer num = sInstanceCounts.get(this.mKlass);
                sInstanceCounts.put(this.mKlass, Integer.valueOf(num != null ? 1 + num.intValue() : 1));
            }
        }

        protected void finalize() throws Throwable {
            try {
                synchronized (sInstanceCounts) {
                    Integer num = sInstanceCounts.get(this.mKlass);
                    if (num != null) {
                        int iIntValue = num.intValue() - 1;
                        if (iIntValue > 0) {
                            sInstanceCounts.put(this.mKlass, Integer.valueOf(iIntValue));
                        } else {
                            sInstanceCounts.remove(this.mKlass);
                        }
                    }
                }
            } finally {
                super.finalize();
            }
        }

        public static int getInstanceCount(Class<?> cls) {
            int iIntValue;
            synchronized (sInstanceCounts) {
                Integer num = sInstanceCounts.get(cls);
                iIntValue = num != null ? num.intValue() : 0;
            }
            return iIntValue;
        }
    }
}
