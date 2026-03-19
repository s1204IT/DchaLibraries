package dalvik.system;

import dalvik.annotation.optimization.FastNative;
import java.lang.ref.FinalizerReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class VMRuntime {
    public static final int SDK_VERSION_CUR_DEVELOPMENT = 10000;
    private static Consumer<String> nonSdkApiUsageConsumer;
    private int targetSdkVersion = SDK_VERSION_CUR_DEVELOPMENT;
    private static final VMRuntime THE_ONE = new VMRuntime();
    private static final Map<String, String> ABI_TO_INSTRUCTION_SET_MAP = new HashMap(16);

    public static native boolean didPruneDalvikCache();

    public static native String getCurrentInstructionSet();

    public static native boolean isBootClassPathOnDisk(String str);

    private native void nativeSetTargetHeapUtilization(float f);

    public static native void registerAppInfo(String str, String[] strArr);

    public static native void registerSensitiveThread();

    public static native void setDedupeHiddenApiWarnings(boolean z);

    public static native void setProcessPackageName(String str);

    public static native void setSystemDaemonThreadPriority();

    private native void setTargetSdkVersionNative(int i);

    @FastNative
    public native long addressOf(Object obj);

    public native String bootClassPath();

    public native void clampGrowthLimit();

    public native String classPath();

    public native void clearGrowthLimit();

    public native void concurrentGC();

    public native void disableJitCompilation();

    public native float getTargetHeapUtilization();

    public native boolean hasUsedHiddenApi();

    @FastNative
    public native boolean is64Bit();

    @FastNative
    public native boolean isCheckJniEnabled();

    @FastNative
    public native boolean isDebuggerActive();

    public native boolean isJavaDebuggable();

    @FastNative
    public native boolean isNativeDebuggable();

    @FastNative
    public native Object newNonMovableArray(Class<?> cls, int i);

    @FastNative
    public native Object newUnpaddedArray(Class<?> cls, int i);

    public native void preloadDexCaches();

    public native String[] properties();

    public native void registerNativeAllocation(int i);

    public native void registerNativeFree(int i);

    public native void requestConcurrentGC();

    public native void requestHeapTrim();

    public native void runHeapTasks();

    public native void setHiddenApiAccessLogSamplingRate(int i);

    public native void setHiddenApiExemptions(String[] strArr);

    public native void startHeapTaskProcessor();

    public native void startJitCompilation();

    public native void stopHeapTaskProcessor();

    public native void trimHeap();

    public native void updateProcessState(int i);

    public native String vmInstructionSet();

    public native String vmLibrary();

    public native String vmVersion();

    static {
        ABI_TO_INSTRUCTION_SET_MAP.put("armeabi", "arm");
        ABI_TO_INSTRUCTION_SET_MAP.put("armeabi-v7a", "arm");
        ABI_TO_INSTRUCTION_SET_MAP.put("mips", "mips");
        ABI_TO_INSTRUCTION_SET_MAP.put("mips64", "mips64");
        ABI_TO_INSTRUCTION_SET_MAP.put("x86", "x86");
        ABI_TO_INSTRUCTION_SET_MAP.put("x86_64", "x86_64");
        ABI_TO_INSTRUCTION_SET_MAP.put("arm64-v8a", "arm64");
        nonSdkApiUsageConsumer = null;
    }

    private VMRuntime() {
    }

    public static VMRuntime getRuntime() {
        return THE_ONE;
    }

    public float setTargetHeapUtilization(float f) {
        float targetHeapUtilization;
        if (f <= 0.0f || f >= 1.0f) {
            throw new IllegalArgumentException(f + " out of range (0,1)");
        }
        synchronized (this) {
            targetHeapUtilization = getTargetHeapUtilization();
            nativeSetTargetHeapUtilization(f);
        }
        return targetHeapUtilization;
    }

    public synchronized void setTargetSdkVersion(int i) {
        this.targetSdkVersion = i;
        setTargetSdkVersionNative(this.targetSdkVersion);
    }

    public synchronized int getTargetSdkVersion() {
        return this.targetSdkVersion;
    }

    @Deprecated
    public long getMinimumHeapSize() {
        return 0L;
    }

    @Deprecated
    public long setMinimumHeapSize(long j) {
        return 0L;
    }

    @Deprecated
    public void gcSoftReferences() {
    }

    @Deprecated
    public void runFinalizationSync() {
        System.runFinalization();
    }

    @Deprecated
    public boolean trackExternalAllocation(long j) {
        return true;
    }

    @Deprecated
    public void trackExternalFree(long j) {
    }

    @Deprecated
    public long getExternalBytesAllocated() {
        return 0L;
    }

    public static void runFinalization(long j) {
        try {
            FinalizerReference.finalizeAllEnqueued(j);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String getInstructionSet(String str) {
        String str2 = ABI_TO_INSTRUCTION_SET_MAP.get(str);
        if (str2 == null) {
            throw new IllegalArgumentException("Unsupported ABI: " + str);
        }
        return str2;
    }

    public static boolean is64BitInstructionSet(String str) {
        return "arm64".equals(str) || "x86_64".equals(str) || "mips64".equals(str);
    }

    public static boolean is64BitAbi(String str) {
        return is64BitInstructionSet(getInstructionSet(str));
    }

    public static void setNonSdkApiUsageConsumer(Consumer<String> consumer) {
        nonSdkApiUsageConsumer = consumer;
    }
}
