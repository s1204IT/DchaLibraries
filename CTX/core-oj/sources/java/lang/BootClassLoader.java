package java.lang;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

class BootClassLoader extends ClassLoader {
    private static BootClassLoader instance;

    @FindBugsSuppressWarnings({"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
    public static synchronized BootClassLoader getInstance() {
        if (instance == null) {
            instance = new BootClassLoader();
        }
        return instance;
    }

    public BootClassLoader() {
        super(null);
    }

    @Override
    protected Class<?> findClass(String str) throws ClassNotFoundException {
        return Class.classForName(str, false, null);
    }

    @Override
    protected URL findResource(String str) {
        return VMClassLoader.getResource(str);
    }

    @Override
    protected Enumeration<URL> findResources(String str) throws IOException {
        return Collections.enumeration(VMClassLoader.getResources(str));
    }

    @Override
    protected Package getPackage(String str) {
        Package packageDefinePackage;
        if (str != null && !str.isEmpty()) {
            synchronized (this) {
                packageDefinePackage = super.getPackage(str);
                if (packageDefinePackage == null) {
                    packageDefinePackage = definePackage(str, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
                }
            }
            return packageDefinePackage;
        }
        return null;
    }

    @Override
    public URL getResource(String str) {
        return findResource(str);
    }

    @Override
    protected Class<?> loadClass(String str, boolean z) throws ClassNotFoundException {
        Class<?> clsFindLoadedClass = findLoadedClass(str);
        if (clsFindLoadedClass == null) {
            return findClass(str);
        }
        return clsFindLoadedClass;
    }

    @Override
    public Enumeration<URL> getResources(String str) throws IOException {
        return findResources(str);
    }
}
