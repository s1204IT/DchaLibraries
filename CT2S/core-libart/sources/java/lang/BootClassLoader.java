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
        super(null, true);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return Class.classForName(name, false, null);
    }

    @Override
    protected URL findResource(String name) {
        return VMClassLoader.getResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String resName) throws IOException {
        return Collections.enumeration(VMClassLoader.getResources(resName));
    }

    @Override
    protected Package getPackage(String name) {
        Package pack = null;
        if (name != null && !name.isEmpty()) {
            synchronized (this) {
                pack = super.getPackage(name);
                if (pack == null) {
                    pack = definePackage(name, "Unknown", "0.0", "Unknown", "Unknown", "0.0", "Unknown", null);
                }
            }
        }
        return pack;
    }

    @Override
    public URL getResource(String resName) {
        return findResource(resName);
    }

    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = findLoadedClass(className);
        if (clazz == null) {
            return findClass(className);
        }
        return clazz;
    }

    @Override
    public Enumeration<URL> getResources(String resName) throws IOException {
        return findResources(resName);
    }
}
