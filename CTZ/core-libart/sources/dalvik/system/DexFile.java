package dalvik.system;

import android.system.ErrnoException;
import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.DexPathList;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import libcore.io.Libcore;

@Deprecated
public final class DexFile {
    public static final int DEX2OAT_FOR_BOOT_IMAGE = 2;
    public static final int DEX2OAT_FOR_FILTER = 3;
    public static final int DEX2OAT_FOR_RELOCATION = 4;
    public static final int DEX2OAT_FROM_SCRATCH = 1;
    public static final int NO_DEXOPT_NEEDED = 0;

    @ReachabilitySensitive
    private Object mCookie;
    private final String mFileName;
    private Object mInternalCookie;

    private static native boolean closeDexFile(Object obj);

    private static native Object createCookieWithArray(byte[] bArr, int i, int i2);

    private static native Object createCookieWithDirectBuffer(ByteBuffer byteBuffer, int i, int i2);

    private static native Class defineClassNative(String str, ClassLoader classLoader, Object obj, DexFile dexFile) throws ClassNotFoundException, NoClassDefFoundError;

    private static native String[] getClassNameList(Object obj);

    private static native String[] getDexFileOptimizationStatus(String str, String str2) throws FileNotFoundException;

    public static native String[] getDexFileOutputPaths(String str, String str2) throws FileNotFoundException;

    public static native String getDexFileStatus(String str, String str2) throws FileNotFoundException;

    public static native int getDexOptNeeded(String str, String str2, String str3, String str4, boolean z, boolean z2) throws IOException;

    public static native String getNonProfileGuidedCompilerFilter(String str);

    public static native String getSafeModeCompilerFilter(String str);

    private static native long getStaticSizeOfDexFile(Object obj);

    private static native boolean isBackedByOatFile(Object obj);

    public static native boolean isDexOptNeeded(String str) throws IOException;

    public static native boolean isProfileGuidedCompilerFilter(String str);

    public static native boolean isValidCompilerFilter(String str);

    private static native Object openDexFileNative(String str, String str2, int i, ClassLoader classLoader, DexPathList.Element[] elementArr);

    private static native void setTrusted(Object obj);

    @Deprecated
    public DexFile(File file) throws IOException {
        this(file.getPath());
    }

    DexFile(File file, ClassLoader classLoader, DexPathList.Element[] elementArr) throws IOException {
        this(file.getPath(), classLoader, elementArr);
    }

    @Deprecated
    public DexFile(String str) throws IOException {
        this(str, (ClassLoader) null, (DexPathList.Element[]) null);
    }

    DexFile(String str, ClassLoader classLoader, DexPathList.Element[] elementArr) throws IOException {
        this.mCookie = openDexFile(str, null, 0, classLoader, elementArr);
        this.mInternalCookie = this.mCookie;
        this.mFileName = str;
    }

    DexFile(ByteBuffer byteBuffer) throws IOException {
        this.mCookie = openInMemoryDexFile(byteBuffer);
        this.mInternalCookie = this.mCookie;
        this.mFileName = null;
    }

    private DexFile(String str, String str2, int i, ClassLoader classLoader, DexPathList.Element[] elementArr) throws IOException {
        if (str2 != null) {
            try {
                String parent = new File(str2).getParent();
                if (Libcore.os.getuid() != Libcore.os.stat(parent).st_uid) {
                    throw new IllegalArgumentException("Optimized data directory " + parent + " is not owned by the current user. Shared storage cannot protect your application from code injection attacks.");
                }
            } catch (ErrnoException e) {
            }
        }
        this.mCookie = openDexFile(str, str2, i, classLoader, elementArr);
        this.mInternalCookie = this.mCookie;
        this.mFileName = str;
    }

    @Deprecated
    public static DexFile loadDex(String str, String str2, int i) throws IOException {
        return loadDex(str, str2, i, null, null);
    }

    static DexFile loadDex(String str, String str2, int i, ClassLoader classLoader, DexPathList.Element[] elementArr) throws IOException {
        return new DexFile(str, str2, i, classLoader, elementArr);
    }

    public String getName() {
        return this.mFileName;
    }

    public String toString() {
        if (this.mFileName != null) {
            return getName();
        }
        return "InMemoryDexFile[cookie=" + Arrays.toString((long[]) this.mCookie) + "]";
    }

    public void close() throws IOException {
        if (this.mInternalCookie != null) {
            if (closeDexFile(this.mInternalCookie)) {
                this.mInternalCookie = null;
            }
            this.mCookie = null;
        }
    }

    public Class loadClass(String str, ClassLoader classLoader) {
        return loadClassBinaryName(str.replace('.', '/'), classLoader, null);
    }

    public Class loadClassBinaryName(String str, ClassLoader classLoader, List<Throwable> list) {
        return defineClass(str, classLoader, this.mCookie, this, list);
    }

    private static Class defineClass(String str, ClassLoader classLoader, Object obj, DexFile dexFile, List<Throwable> list) {
        try {
            return defineClassNative(str, classLoader, obj, dexFile);
        } catch (ClassNotFoundException e) {
            if (list != null) {
                list.add(e);
            }
            return null;
        } catch (NoClassDefFoundError e2) {
            if (list != null) {
                list.add(e2);
            }
            return null;
        }
    }

    public Enumeration<String> entries() {
        return new DFEnum(this);
    }

    private static class DFEnum implements Enumeration<String> {
        private int mIndex = 0;
        private String[] mNameList;

        DFEnum(DexFile dexFile) {
            this.mNameList = DexFile.getClassNameList(dexFile.mCookie);
        }

        @Override
        public boolean hasMoreElements() {
            return this.mIndex < this.mNameList.length;
        }

        @Override
        public String nextElement() {
            String[] strArr = this.mNameList;
            int i = this.mIndex;
            this.mIndex = i + 1;
            return strArr[i];
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mInternalCookie != null && !closeDexFile(this.mInternalCookie)) {
                throw new AssertionError("Failed to close dex file in finalizer.");
            }
            this.mInternalCookie = null;
            this.mCookie = null;
        } finally {
            super.finalize();
        }
    }

    private static Object openDexFile(String str, String str2, int i, ClassLoader classLoader, DexPathList.Element[] elementArr) throws IOException {
        String absolutePath;
        String absolutePath2 = new File(str).getAbsolutePath();
        if (str2 == null) {
            absolutePath = null;
        } else {
            absolutePath = new File(str2).getAbsolutePath();
        }
        return openDexFileNative(absolutePath2, absolutePath, i, classLoader, elementArr);
    }

    private static Object openInMemoryDexFile(ByteBuffer byteBuffer) throws IOException {
        if (byteBuffer.isDirect()) {
            return createCookieWithDirectBuffer(byteBuffer, byteBuffer.position(), byteBuffer.limit());
        }
        return createCookieWithArray(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
    }

    boolean isBackedByOatFile() {
        return isBackedByOatFile(this.mCookie);
    }

    void setTrusted() {
        setTrusted(this.mCookie);
    }

    public static int getDexOptNeeded(String str, String str2, String str3, boolean z, boolean z2) throws IOException {
        return getDexOptNeeded(str, str2, str3, null, z, z2);
    }

    public static final class OptimizationInfo {
        private final String reason;
        private final String status;

        private OptimizationInfo(String str, String str2) {
            this.status = str;
            this.reason = str2;
        }

        public String getStatus() {
            return this.status;
        }

        public String getReason() {
            return this.reason;
        }
    }

    public static OptimizationInfo getDexFileOptimizationInfo(String str, String str2) throws FileNotFoundException {
        String[] dexFileOptimizationStatus = getDexFileOptimizationStatus(str, str2);
        return new OptimizationInfo(dexFileOptimizationStatus[0], dexFileOptimizationStatus[1]);
    }

    public long getStaticSizeOfDexFile() {
        return getStaticSizeOfDexFile(this.mCookie);
    }
}
