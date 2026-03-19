package android.icu.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;

public class ClassLoaderUtil {
    private static volatile ClassLoader BOOTSTRAP_CLASSLOADER;

    private static class BootstrapClassLoader extends ClassLoader {
        BootstrapClassLoader() {
            super(Object.class.getClassLoader());
        }
    }

    private static ClassLoader getBootstrapClassLoader() {
        ClassLoader bootstrapClassLoader;
        if (BOOTSTRAP_CLASSLOADER == null) {
            synchronized (ClassLoaderUtil.class) {
                if (BOOTSTRAP_CLASSLOADER == null) {
                    if (System.getSecurityManager() != null) {
                        bootstrapClassLoader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                            @Override
                            public ClassLoader run2() {
                                return new BootstrapClassLoader();
                            }
                        });
                    } else {
                        bootstrapClassLoader = new BootstrapClassLoader();
                    }
                    BOOTSTRAP_CLASSLOADER = bootstrapClassLoader;
                }
            }
        }
        return BOOTSTRAP_CLASSLOADER;
    }

    public static ClassLoader getClassLoader(Class<?> cls) {
        ClassLoader classLoader = cls.getClassLoader();
        if (classLoader == null) {
            return getClassLoader();
        }
        return classLoader;
    }

    public static ClassLoader getClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader == null) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if (systemClassLoader == null) {
                return getBootstrapClassLoader();
            }
            return systemClassLoader;
        }
        return contextClassLoader;
    }
}
