package java.lang;

import android.system.ErrnoException;
import android.system.StructPasswd;
import android.system.StructUtsname;
import dalvik.system.VMRuntime;
import dalvik.system.VMStack;
import java.io.BufferedInputStream;
import java.io.Console;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.channels.Channel;
import java.nio.channels.spi.SelectorProvider;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import libcore.icu.ICU;
import libcore.io.Libcore;

public final class System {
    private static final int ARRAYCOPY_SHORT_BOOLEAN_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_BYTE_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_CHAR_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_DOUBLE_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_FLOAT_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_INT_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_LONG_ARRAY_THRESHOLD = 32;
    private static final int ARRAYCOPY_SHORT_SHORT_ARRAY_THRESHOLD = 32;
    private static boolean justRanFinalization;
    private static boolean runGC;
    private static final Object lock = new Object();
    public static final PrintStream err = new PrintStream(new FileOutputStream(FileDescriptor.err));
    public static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
    public static final InputStream in = new BufferedInputStream(new FileInputStream(FileDescriptor.in));
    private static final Properties unchangeableSystemProperties = initUnchangeableSystemProperties();
    private static Properties systemProperties = createSystemProperties();
    private static final String lineSeparator = getProperty("line.separator");

    public static native void arraycopy(Object obj, int i, Object obj2, int i2, int i3);

    private static native void arraycopyBooleanUnchecked(boolean[] zArr, int i, boolean[] zArr2, int i2, int i3);

    private static native void arraycopyByteUnchecked(byte[] bArr, int i, byte[] bArr2, int i2, int i3);

    private static native void arraycopyCharUnchecked(char[] cArr, int i, char[] cArr2, int i2, int i3);

    private static native void arraycopyDoubleUnchecked(double[] dArr, int i, double[] dArr2, int i2, int i3);

    private static native void arraycopyFloatUnchecked(float[] fArr, int i, float[] fArr2, int i2, int i3);

    private static native void arraycopyIntUnchecked(int[] iArr, int i, int[] iArr2, int i2, int i3);

    private static native void arraycopyLongUnchecked(long[] jArr, int i, long[] jArr2, int i2, int i3);

    private static native void arraycopyShortUnchecked(short[] sArr, int i, short[] sArr2, int i2, int i3);

    public static native long currentTimeMillis();

    public static native int identityHashCode(Object obj);

    private static native void log(char c, String str, Throwable th);

    public static native String mapLibraryName(String str);

    public static native long nanoTime();

    private static native void setFieldImpl(String str, String str2, Object obj);

    private static native String[] specialProperties();

    public static void setIn(InputStream newIn) {
        setFieldImpl("in", "Ljava/io/InputStream;", newIn);
    }

    public static void setOut(PrintStream newOut) {
        setFieldImpl("out", "Ljava/io/PrintStream;", newOut);
    }

    public static void setErr(PrintStream newErr) {
        setFieldImpl("err", "Ljava/io/PrintStream;", newErr);
    }

    private System() {
    }

    public static void arraycopy(char[] src, int srcPos, char[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyCharUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(byte[] src, int srcPos, byte[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyByteUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(short[] src, int srcPos, short[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyShortUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(int[] src, int srcPos, int[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyIntUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(long[] src, int srcPos, long[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyLongUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(float[] src, int srcPos, float[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyFloatUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(double[] src, int srcPos, double[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyDoubleUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void arraycopy(boolean[] src, int srcPos, boolean[] dst, int dstPos, int length) {
        if (src == null) {
            throw new NullPointerException("src == null");
        }
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (srcPos < 0 || dstPos < 0 || length < 0 || srcPos > src.length - length || dstPos > dst.length - length) {
            throw new ArrayIndexOutOfBoundsException("src.length=" + src.length + " srcPos=" + srcPos + " dst.length=" + dst.length + " dstPos=" + dstPos + " length=" + length);
        }
        if (length <= 32) {
            if (src == dst && srcPos < dstPos && dstPos < srcPos + length) {
                for (int i = length - 1; i >= 0; i--) {
                    dst[dstPos + i] = src[srcPos + i];
                }
                return;
            }
            for (int i2 = 0; i2 < length; i2++) {
                dst[dstPos + i2] = src[srcPos + i2];
            }
            return;
        }
        arraycopyBooleanUnchecked(src, srcPos, dst, dstPos, length);
    }

    public static void exit(int code) {
        Runtime.getRuntime().exit(code);
    }

    public static void gc() {
        boolean shouldRunGC;
        synchronized (lock) {
            shouldRunGC = justRanFinalization;
            if (shouldRunGC) {
                justRanFinalization = false;
            } else {
                runGC = true;
            }
        }
        if (shouldRunGC) {
            Runtime.getRuntime().gc();
        }
    }

    public static String getenv(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        return Libcore.os.getenv(name);
    }

    public static Map<String, String> getenv() {
        Map<String, String> map = new HashMap<>();
        String[] arr$ = Libcore.os.environ();
        for (String entry : arr$) {
            int index = entry.indexOf(61);
            if (index != -1) {
                map.put(entry.substring(0, index), entry.substring(index + 1));
            }
        }
        return new SystemEnvironment(map);
    }

    public static Channel inheritedChannel() throws IOException {
        return SelectorProvider.provider().inheritedChannel();
    }

    public static Properties getProperties() {
        return systemProperties;
    }

    private static Properties initUnchangeableSystemProperties() {
        VMRuntime runtime = VMRuntime.getRuntime();
        Properties p = new Properties();
        p.put("java.boot.class.path", runtime.bootClassPath());
        p.put("java.class.path", runtime.classPath());
        p.put("java.class.version", "50.0");
        p.put("java.compiler", "");
        p.put("java.ext.dirs", "");
        p.put("java.version", "0");
        String javaHome = getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = "/system";
        }
        p.put("java.home", javaHome);
        p.put("java.specification.name", "Dalvik Core Library");
        p.put("java.specification.vendor", "The Android Project");
        p.put("java.specification.version", "0.9");
        p.put("java.vendor", "The Android Project");
        p.put("java.vendor.url", "http://www.android.com/");
        p.put("java.vm.name", "Dalvik");
        p.put("java.vm.specification.name", "Dalvik Virtual Machine Specification");
        p.put("java.vm.specification.vendor", "The Android Project");
        p.put("java.vm.specification.version", "0.9");
        p.put("java.vm.vendor", "The Android Project");
        p.put("java.vm.version", runtime.vmVersion());
        p.put("file.separator", "/");
        p.put("line.separator", "\n");
        p.put("path.separator", ":");
        p.put("java.runtime.name", "Android Runtime");
        p.put("java.runtime.version", "0.9");
        p.put("java.vm.vendor.url", "http://www.android.com/");
        p.put("file.encoding", "UTF-8");
        p.put("user.language", "en");
        p.put("user.region", "US");
        try {
            StructPasswd passwd = Libcore.os.getpwuid(Libcore.os.getuid());
            p.put("user.name", passwd.pw_name);
            StructUtsname info = Libcore.os.uname();
            p.put("os.arch", info.machine);
            p.put("os.name", info.sysname);
            p.put("os.version", info.release);
            p.put("android.icu.library.version", ICU.getIcuVersion());
            p.put("android.icu.unicode.version", ICU.getUnicodeVersion());
            p.put("android.icu.cldr.version", ICU.getCldrVersion());
            parsePropertyAssignments(p, specialProperties());
            parsePropertyAssignments(p, runtime.properties());
            return p;
        } catch (ErrnoException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void initUnchangeableSystemProperty(String name, String value) {
        checkPropertyName(name);
        unchangeableSystemProperties.put(name, value);
    }

    private static void setDefaultChangeableProperties(Properties p) {
        p.put("java.io.tmpdir", "/tmp");
        p.put("user.home", "");
    }

    private static Properties createSystemProperties() {
        Properties p = new PropertiesWithNonOverrideableDefaults(unchangeableSystemProperties);
        setDefaultChangeableProperties(p);
        return p;
    }

    private static void parsePropertyAssignments(Properties p, String[] assignments) {
        for (String assignment : assignments) {
            int split = assignment.indexOf(61);
            String key = assignment.substring(0, split);
            String value = assignment.substring(split + 1);
            p.put(key, value);
        }
    }

    public static String getProperty(String propertyName) {
        return getProperty(propertyName, null);
    }

    public static String getProperty(String name, String defaultValue) {
        checkPropertyName(name);
        return systemProperties.getProperty(name, defaultValue);
    }

    public static String setProperty(String name, String value) {
        checkPropertyName(name);
        return (String) systemProperties.setProperty(name, value);
    }

    public static String clearProperty(String name) {
        checkPropertyName(name);
        return (String) systemProperties.remove(name);
    }

    private static void checkPropertyName(String name) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
        }
    }

    public static Console console() {
        return Console.getConsole();
    }

    public static SecurityManager getSecurityManager() {
        return null;
    }

    public static String lineSeparator() {
        return lineSeparator;
    }

    public static void load(String pathName) {
        Runtime.getRuntime().load(pathName, VMStack.getCallingClassLoader());
    }

    public static void loadLibrary(String libName) {
        Runtime.getRuntime().loadLibrary(libName, VMStack.getCallingClassLoader());
    }

    public static void logE(String message) {
        log('E', message, null);
    }

    public static void logE(String message, Throwable th) {
        log('E', message, th);
    }

    public static void logI(String message) {
        log('I', message, null);
    }

    public static void logI(String message, Throwable th) {
        log('I', message, th);
    }

    public static void logW(String message) {
        log('W', message, null);
    }

    public static void logW(String message, Throwable th) {
        log('W', message, th);
    }

    public static void runFinalization() {
        boolean shouldRunGC;
        synchronized (lock) {
            shouldRunGC = runGC;
            runGC = false;
        }
        if (shouldRunGC) {
            Runtime.getRuntime().gc();
        }
        Runtime.getRuntime().runFinalization();
        synchronized (lock) {
            justRanFinalization = true;
        }
    }

    @Deprecated
    public static void runFinalizersOnExit(boolean flag) {
        Runtime.runFinalizersOnExit(flag);
    }

    public static void setProperties(Properties p) {
        PropertiesWithNonOverrideableDefaults userProperties = new PropertiesWithNonOverrideableDefaults(unchangeableSystemProperties);
        if (p != null) {
            userProperties.putAll(p);
        } else {
            setDefaultChangeableProperties(userProperties);
        }
        systemProperties = userProperties;
    }

    public static void setSecurityManager(SecurityManager sm) {
        if (sm != null) {
            throw new SecurityException();
        }
    }

    static final class PropertiesWithNonOverrideableDefaults extends Properties {
        PropertiesWithNonOverrideableDefaults(Properties defaults) {
            super(defaults);
        }

        @Override
        public Object put(Object key, Object value) {
            if (!this.defaults.containsKey(key)) {
                return super.put(key, value);
            }
            System.logE("Ignoring attempt to set property \"" + key + "\" to value \"" + value + "\".");
            return this.defaults.get(key);
        }

        @Override
        public Object remove(Object key) {
            if (!this.defaults.containsKey(key)) {
                return super.remove(key);
            }
            System.logE("Ignoring attempt to remove property \"" + key + "\".");
            return null;
        }
    }

    static class SystemEnvironment extends AbstractMap<String, String> {
        private final Map<String, String> map;

        public SystemEnvironment(Map<String, String> map) {
            this.map = Collections.unmodifiableMap(map);
        }

        @Override
        public Set<Map.Entry<String, String>> entrySet() {
            return this.map.entrySet();
        }

        @Override
        public String get(Object key) {
            return this.map.get(toNonNullString(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return this.map.containsKey(toNonNullString(key));
        }

        @Override
        public boolean containsValue(Object value) {
            return this.map.containsValue(toNonNullString(value));
        }

        private String toNonNullString(Object o) {
            if (o == null) {
                throw new NullPointerException("o == null");
            }
            return (String) o;
        }
    }
}
