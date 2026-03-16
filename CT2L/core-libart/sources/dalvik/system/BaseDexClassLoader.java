package dalvik.system;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BaseDexClassLoader extends ClassLoader {
    private final DexPathList pathList;

    public BaseDexClassLoader(String dexPath, File optimizedDirectory, String libraryPath, ClassLoader parent) {
        super(parent);
        this.pathList = new DexPathList(this, dexPath, libraryPath, optimizedDirectory);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        List<Throwable> suppressedExceptions = new ArrayList<>();
        Class<?> clsFindClass = this.pathList.findClass(name, suppressedExceptions);
        if (clsFindClass == null) {
            ClassNotFoundException cnfe = new ClassNotFoundException("Didn't find class \"" + name + "\" on path: " + this.pathList);
            for (Throwable t : suppressedExceptions) {
                cnfe.addSuppressed(t);
            }
            throw cnfe;
        }
        return clsFindClass;
    }

    @Override
    protected URL findResource(String name) {
        return this.pathList.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        return this.pathList.findResources(name);
    }

    @Override
    public String findLibrary(String name) {
        return this.pathList.findLibrary(name);
    }

    @Override
    protected synchronized Package getPackage(String name) {
        Package pack = null;
        synchronized (this) {
            if (name != null) {
                if (!name.isEmpty() && (pack = super.getPackage(name)) == null) {
                    pack = definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
                }
            }
        }
        return pack;
    }

    public String getLdLibraryPath() {
        StringBuilder result = new StringBuilder();
        File[] arr$ = this.pathList.getNativeLibraryDirectories();
        for (File directory : arr$) {
            if (result.length() > 0) {
                result.append(':');
            }
            result.append(directory);
        }
        return result.toString();
    }

    public String toString() {
        return getClass().getName() + "[" + this.pathList + "]";
    }
}
