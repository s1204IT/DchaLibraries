package dalvik.system;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

public class BaseDexClassLoader extends ClassLoader {
    private static volatile Reporter reporter = null;
    private final DexPathList pathList;

    public interface Reporter {
        void report(List<BaseDexClassLoader> list, List<String> list2);
    }

    public BaseDexClassLoader(String str, File file, String str2, ClassLoader classLoader) {
        this(str, file, str2, classLoader, false);
    }

    public BaseDexClassLoader(String str, File file, String str2, ClassLoader classLoader, boolean z) {
        super(classLoader);
        this.pathList = new DexPathList(this, str, str2, null, z);
        if (reporter != null) {
            reportClassLoaderChain();
        }
    }

    private void reportClassLoaderChain() {
        boolean z;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList.add(this);
        arrayList2.add(String.join(File.pathSeparator, this.pathList.getDexPaths()));
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        for (ClassLoader parent2 = getParent(); parent2 != null && parent2 != parent; parent2 = parent2.getParent()) {
            if (parent2 instanceof BaseDexClassLoader) {
                BaseDexClassLoader baseDexClassLoader = (BaseDexClassLoader) parent2;
                arrayList.add(baseDexClassLoader);
                arrayList2.add(String.join(File.pathSeparator, baseDexClassLoader.pathList.getDexPaths()));
            } else {
                z = false;
                break;
            }
        }
        z = true;
        if (z) {
            reporter.report(arrayList, arrayList2);
        }
    }

    public BaseDexClassLoader(ByteBuffer[] byteBufferArr, ClassLoader classLoader) {
        super(classLoader);
        this.pathList = new DexPathList(this, byteBufferArr);
    }

    @Override
    protected Class<?> findClass(String str) throws ClassNotFoundException {
        ArrayList arrayList = new ArrayList();
        Class<?> clsFindClass = this.pathList.findClass(str, arrayList);
        if (clsFindClass == null) {
            ClassNotFoundException classNotFoundException = new ClassNotFoundException("Didn't find class \"" + str + "\" on path: " + this.pathList);
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                classNotFoundException.addSuppressed((Throwable) it.next());
            }
            throw classNotFoundException;
        }
        return clsFindClass;
    }

    public void addDexPath(String str) {
        addDexPath(str, false);
    }

    public void addDexPath(String str, boolean z) {
        this.pathList.addDexPath(str, null, z);
    }

    public void addNativePath(Collection<String> collection) {
        this.pathList.addNativePath(collection);
    }

    @Override
    protected URL findResource(String str) {
        return this.pathList.findResource(str);
    }

    @Override
    protected Enumeration<URL> findResources(String str) {
        return this.pathList.findResources(str);
    }

    @Override
    public String findLibrary(String str) {
        return this.pathList.findLibrary(str);
    }

    @Override
    protected synchronized Package getPackage(String str) {
        if (str != null) {
            if (!str.isEmpty()) {
                Package packageDefinePackage = super.getPackage(str);
                if (packageDefinePackage == null) {
                    packageDefinePackage = definePackage(str, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
                }
                return packageDefinePackage;
            }
        }
        return null;
    }

    public String getLdLibraryPath() {
        StringBuilder sb = new StringBuilder();
        for (File file : this.pathList.getNativeLibraryDirectories()) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(file);
        }
        return sb.toString();
    }

    public String toString() {
        return getClass().getName() + "[" + this.pathList + "]";
    }

    public static void setReporter(Reporter reporter2) {
        reporter = reporter2;
    }

    public static Reporter getReporter() {
        return reporter;
    }
}
