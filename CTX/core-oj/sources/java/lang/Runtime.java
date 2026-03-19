package java.lang;

import android.system.OsConstants;
import dalvik.annotation.optimization.FastNative;
import dalvik.system.VMDebug;
import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.util.EmptyArray;
import sun.reflect.CallerSensitive;

public class Runtime {
    private static Runtime currentRuntime = new Runtime();
    private static boolean finalizeOnExit;
    private boolean shuttingDown;
    private boolean tracingMethods;
    private List<Thread> shutdownHooks = new ArrayList();
    private volatile String[] mLibPaths = null;

    private static native void nativeExit(int i);

    private static native String nativeLoad(String str, ClassLoader classLoader);

    private static native void runFinalization0();

    @FastNative
    public native long freeMemory();

    public native void gc();

    @FastNative
    public native long maxMemory();

    @FastNative
    public native long totalMemory();

    public static Runtime getRuntime() {
        return currentRuntime;
    }

    private Runtime() {
    }

    public void exit(int i) {
        Thread[] threadArr;
        synchronized (this) {
            if (!this.shuttingDown) {
                this.shuttingDown = true;
                synchronized (this.shutdownHooks) {
                    threadArr = new Thread[this.shutdownHooks.size()];
                    this.shutdownHooks.toArray(threadArr);
                }
                for (Thread thread : threadArr) {
                    thread.start();
                }
                for (Thread thread2 : threadArr) {
                    try {
                        thread2.join();
                    } catch (InterruptedException e) {
                    }
                }
                if (finalizeOnExit) {
                    runFinalization();
                }
                nativeExit(i);
            }
        }
    }

    public void addShutdownHook(Thread thread) {
        if (thread == null) {
            throw new NullPointerException("hook == null");
        }
        if (this.shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        }
        if (thread.started) {
            throw new IllegalArgumentException("Hook has already been started");
        }
        synchronized (this.shutdownHooks) {
            if (this.shutdownHooks.contains(thread)) {
                throw new IllegalArgumentException("Hook already registered.");
            }
            this.shutdownHooks.add(thread);
        }
    }

    public boolean removeShutdownHook(Thread thread) {
        boolean zRemove;
        if (thread == null) {
            throw new NullPointerException("hook == null");
        }
        if (this.shuttingDown) {
            throw new IllegalStateException("VM already shutting down");
        }
        synchronized (this.shutdownHooks) {
            zRemove = this.shutdownHooks.remove(thread);
        }
        return zRemove;
    }

    public void halt(int i) {
        nativeExit(i);
    }

    @Deprecated
    public static void runFinalizersOnExit(boolean z) {
        finalizeOnExit = z;
    }

    public Process exec(String str) throws IOException {
        return exec(str, (String[]) null, (File) null);
    }

    public Process exec(String str, String[] strArr) throws IOException {
        return exec(str, strArr, (File) null);
    }

    public Process exec(String str, String[] strArr, File file) throws IOException {
        if (str.length() == 0) {
            throw new IllegalArgumentException("Empty command");
        }
        StringTokenizer stringTokenizer = new StringTokenizer(str);
        String[] strArr2 = new String[stringTokenizer.countTokens()];
        int i = 0;
        while (stringTokenizer.hasMoreTokens()) {
            strArr2[i] = stringTokenizer.nextToken();
            i++;
        }
        return exec(strArr2, strArr, file);
    }

    public Process exec(String[] strArr) throws IOException {
        return exec(strArr, (String[]) null, (File) null);
    }

    public Process exec(String[] strArr, String[] strArr2) throws IOException {
        return exec(strArr, strArr2, (File) null);
    }

    public Process exec(String[] strArr, String[] strArr2, File file) throws IOException {
        return new ProcessBuilder(strArr).environment(strArr2).directory(file).start();
    }

    public int availableProcessors() {
        return (int) Libcore.os.sysconf(OsConstants._SC_NPROCESSORS_CONF);
    }

    public void runFinalization() {
        VMRuntime.runFinalization(0L);
    }

    public void traceInstructions(boolean z) {
    }

    public void traceMethodCalls(boolean z) {
        if (z != this.tracingMethods) {
            if (z) {
                VMDebug.startMethodTracing();
            } else {
                VMDebug.stopMethodTracing();
            }
            this.tracingMethods = z;
        }
    }

    @CallerSensitive
    public void load(String str) {
        load0(VMStack.getStackClass1(), str);
    }

    private void checkTargetSdkVersionForLoad(String str) {
        int targetSdkVersion = VMRuntime.getRuntime().getTargetSdkVersion();
        if (targetSdkVersion > 24) {
            throw new UnsupportedOperationException(str + " is not supported on SDK " + targetSdkVersion);
        }
    }

    void load(String str, ClassLoader classLoader) {
        checkTargetSdkVersionForLoad("java.lang.Runtime#load(String, ClassLoader)");
        System.logE("java.lang.Runtime#load(String, ClassLoader) is private and will be removed in a future Android release");
        if (str == null) {
            throw new NullPointerException("absolutePath == null");
        }
        String strNativeLoad = nativeLoad(str, classLoader);
        if (strNativeLoad != null) {
            throw new UnsatisfiedLinkError(strNativeLoad);
        }
    }

    synchronized void load0(Class<?> cls, String str) {
        if (!new File(str).isAbsolute()) {
            throw new UnsatisfiedLinkError("Expecting an absolute path of the library: " + str);
        }
        if (str == null) {
            throw new NullPointerException("filename == null");
        }
        String strNativeLoad = nativeLoad(str, cls.getClassLoader());
        if (strNativeLoad != null) {
            throw new UnsatisfiedLinkError(strNativeLoad);
        }
    }

    @CallerSensitive
    public void loadLibrary(String str) {
        loadLibrary0(VMStack.getCallingClassLoader(), str);
    }

    public void loadLibrary(String str, ClassLoader classLoader) {
        checkTargetSdkVersionForLoad("java.lang.Runtime#loadLibrary(String, ClassLoader)");
        System.logE("java.lang.Runtime#loadLibrary(String, ClassLoader) is private and will be removed in a future Android release");
        loadLibrary0(classLoader, str);
    }

    synchronized void loadLibrary0(ClassLoader classLoader, String str) {
        if (str.indexOf(File.separatorChar) != -1) {
            throw new UnsatisfiedLinkError("Directory separator should not appear in library name: " + str);
        }
        if (classLoader != null) {
            String strFindLibrary = classLoader.findLibrary(str);
            if (strFindLibrary == null) {
                throw new UnsatisfiedLinkError(((Object) classLoader) + " couldn't find \"" + System.mapLibraryName(str) + "\"");
            }
            String strNativeLoad = nativeLoad(strFindLibrary, classLoader);
            if (strNativeLoad != null) {
                throw new UnsatisfiedLinkError(strNativeLoad);
            }
            return;
        }
        String strMapLibraryName = System.mapLibraryName(str);
        ArrayList arrayList = new ArrayList();
        String strNativeLoad2 = null;
        for (String str2 : getLibPaths()) {
            String str3 = str2 + strMapLibraryName;
            arrayList.add(str3);
            if (IoUtils.canOpenReadOnly(str3) && (strNativeLoad2 = nativeLoad(str3, classLoader)) == null) {
                return;
            }
        }
        if (strNativeLoad2 != null) {
            throw new UnsatisfiedLinkError(strNativeLoad2);
        }
        throw new UnsatisfiedLinkError("Library " + str + " not found; tried " + ((Object) arrayList));
    }

    private String[] getLibPaths() {
        if (this.mLibPaths == null) {
            synchronized (this) {
                if (this.mLibPaths == null) {
                    this.mLibPaths = initLibPaths();
                }
            }
        }
        return this.mLibPaths;
    }

    private static String[] initLibPaths() {
        String property = System.getProperty("java.library.path");
        if (property == null) {
            return EmptyArray.STRING;
        }
        String[] strArrSplit = property.split(":");
        for (int i = 0; i < strArrSplit.length; i++) {
            if (!strArrSplit[i].endsWith("/")) {
                strArrSplit[i] = strArrSplit[i] + "/";
            }
        }
        return strArrSplit;
    }

    @Deprecated
    public InputStream getLocalizedInputStream(InputStream inputStream) {
        return inputStream;
    }

    @Deprecated
    public OutputStream getLocalizedOutputStream(OutputStream outputStream) {
        return outputStream;
    }
}
