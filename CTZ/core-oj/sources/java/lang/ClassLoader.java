package java.lang;

import dalvik.system.PathClassLoader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.CompoundEnumeration;
import sun.reflect.CallerSensitive;

public abstract class ClassLoader {
    private transient long allocator;
    private transient long classTable;
    private final HashMap<String, Package> packages;
    private final ClassLoader parent;
    public final Map<List<Class<?>>, Class<?>> proxyCache;

    private static class SystemClassLoader {
        public static ClassLoader loader = ClassLoader.createSystemClassLoader();

        private SystemClassLoader() {
        }
    }

    private static ClassLoader createSystemClassLoader() {
        return new PathClassLoader(System.getProperty("java.class.path", "."), System.getProperty("java.library.path", ""), BootClassLoader.getInstance());
    }

    private static Void checkCreateClassLoader() {
        return null;
    }

    private ClassLoader(Void r1, ClassLoader classLoader) {
        this.proxyCache = new HashMap();
        this.packages = new HashMap<>();
        this.parent = classLoader;
    }

    protected ClassLoader(ClassLoader classLoader) {
        this(checkCreateClassLoader(), classLoader);
    }

    protected ClassLoader() {
        this(checkCreateClassLoader(), getSystemClassLoader());
    }

    public Class<?> loadClass(String str) throws ClassNotFoundException {
        return loadClass(str, false);
    }

    protected Class<?> loadClass(String str, boolean z) throws ClassNotFoundException {
        Class<?> clsFindBootstrapClassOrNull;
        Class<?> clsFindLoadedClass = findLoadedClass(str);
        if (clsFindLoadedClass == null) {
            try {
                if (this.parent != null) {
                    clsFindBootstrapClassOrNull = this.parent.loadClass(str, false);
                } else {
                    clsFindBootstrapClassOrNull = findBootstrapClassOrNull(str);
                }
                clsFindLoadedClass = clsFindBootstrapClassOrNull;
            } catch (ClassNotFoundException e) {
            }
            if (clsFindLoadedClass == null) {
                return findClass(str);
            }
            return clsFindLoadedClass;
        }
        return clsFindLoadedClass;
    }

    protected Class<?> findClass(String str) throws ClassNotFoundException {
        throw new ClassNotFoundException(str);
    }

    @Deprecated
    protected final Class<?> defineClass(byte[] bArr, int i, int i2) throws ClassFormatError {
        throw new UnsupportedOperationException("can't load this type of class file");
    }

    protected final Class<?> defineClass(String str, byte[] bArr, int i, int i2) throws ClassFormatError {
        throw new UnsupportedOperationException("can't load this type of class file");
    }

    protected final Class<?> defineClass(String str, byte[] bArr, int i, int i2, ProtectionDomain protectionDomain) throws ClassFormatError {
        throw new UnsupportedOperationException("can't load this type of class file");
    }

    protected final Class<?> defineClass(String str, ByteBuffer byteBuffer, ProtectionDomain protectionDomain) throws ClassFormatError {
        throw new UnsupportedOperationException("can't load this type of class file");
    }

    protected final void resolveClass(Class<?> cls) {
    }

    protected final Class<?> findSystemClass(String str) throws ClassNotFoundException {
        return Class.forName(str, false, getSystemClassLoader());
    }

    private Class<?> findBootstrapClassOrNull(String str) {
        return null;
    }

    protected final Class<?> findLoadedClass(String str) {
        ClassLoader classLoader;
        if (this == BootClassLoader.getInstance()) {
            classLoader = null;
        } else {
            classLoader = this;
        }
        return VMClassLoader.findLoadedClass(classLoader, str);
    }

    protected final void setSigners(Class<?> cls, Object[] objArr) {
    }

    public URL getResource(String str) {
        URL bootstrapResource;
        if (this.parent != null) {
            bootstrapResource = this.parent.getResource(str);
        } else {
            bootstrapResource = getBootstrapResource(str);
        }
        if (bootstrapResource == null) {
            return findResource(str);
        }
        return bootstrapResource;
    }

    public Enumeration<URL> getResources(String str) throws IOException {
        Enumeration[] enumerationArr = new Enumeration[2];
        if (this.parent != null) {
            enumerationArr[0] = this.parent.getResources(str);
        } else {
            enumerationArr[0] = getBootstrapResources(str);
        }
        enumerationArr[1] = findResources(str);
        return new CompoundEnumeration(enumerationArr);
    }

    protected URL findResource(String str) {
        return null;
    }

    protected Enumeration<URL> findResources(String str) throws IOException {
        return Collections.emptyEnumeration();
    }

    @CallerSensitive
    protected static boolean registerAsParallelCapable() {
        return true;
    }

    public static URL getSystemResource(String str) {
        ClassLoader systemClassLoader = getSystemClassLoader();
        if (systemClassLoader == null) {
            return getBootstrapResource(str);
        }
        return systemClassLoader.getResource(str);
    }

    public static Enumeration<URL> getSystemResources(String str) throws IOException {
        ClassLoader systemClassLoader = getSystemClassLoader();
        if (systemClassLoader == null) {
            return getBootstrapResources(str);
        }
        return systemClassLoader.getResources(str);
    }

    private static URL getBootstrapResource(String str) {
        return null;
    }

    private static Enumeration<URL> getBootstrapResources(String str) throws IOException {
        return null;
    }

    public InputStream getResourceAsStream(String str) {
        URL resource = getResource(str);
        if (resource != null) {
            try {
                return resource.openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    public static InputStream getSystemResourceAsStream(String str) {
        URL systemResource = getSystemResource(str);
        if (systemResource != null) {
            try {
                return systemResource.openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

    @CallerSensitive
    public final ClassLoader getParent() {
        return this.parent;
    }

    @CallerSensitive
    public static ClassLoader getSystemClassLoader() {
        return SystemClassLoader.loader;
    }

    protected Package definePackage(String str, String str2, String str3, String str4, String str5, String str6, String str7, URL url) throws IllegalArgumentException {
        Package r13;
        synchronized (this.packages) {
            if (this.packages.get(str) != null) {
                throw new IllegalArgumentException(str);
            }
            r13 = new Package(str, str2, str3, str4, str5, str6, str7, url, this);
            this.packages.put(str, r13);
        }
        return r13;
    }

    protected Package getPackage(String str) {
        Package r3;
        synchronized (this.packages) {
            r3 = this.packages.get(str);
        }
        return r3;
    }

    protected Package[] getPackages() {
        HashMap map;
        synchronized (this.packages) {
            map = new HashMap(this.packages);
        }
        return (Package[]) map.values().toArray(new Package[map.size()]);
    }

    protected String findLibrary(String str) {
        return null;
    }

    public void setDefaultAssertionStatus(boolean z) {
    }

    public void setPackageAssertionStatus(String str, boolean z) {
    }

    public void setClassAssertionStatus(String str, boolean z) {
    }

    public void clearAssertionStatus() {
    }
}
