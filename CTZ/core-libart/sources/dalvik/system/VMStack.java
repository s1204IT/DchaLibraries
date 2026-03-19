package dalvik.system;

import dalvik.annotation.optimization.FastNative;

public final class VMStack {
    @FastNative
    public static native int fillStackTraceElements(Thread thread, StackTraceElement[] stackTraceElementArr);

    @FastNative
    public static native AnnotatedStackTraceElement[] getAnnotatedThreadStackTrace(Thread thread);

    @FastNative
    public static native ClassLoader getCallingClassLoader();

    @FastNative
    public static native ClassLoader getClosestUserClassLoader();

    @FastNative
    public static native Class<?> getStackClass2();

    @FastNative
    public static native StackTraceElement[] getThreadStackTrace(Thread thread);

    public static Class<?> getStackClass1() {
        return getStackClass2();
    }
}
