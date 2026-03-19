package com.android.internal.os;

import android.os.Trace;
import android.provider.SettingsStringUtil;
import dalvik.system.DelegateLastClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class ClassLoaderFactory {
    private static final String PATH_CLASS_LOADER_NAME = PathClassLoader.class.getName();
    private static final String DEX_CLASS_LOADER_NAME = DexClassLoader.class.getName();
    private static final String DELEGATE_LAST_CLASS_LOADER_NAME = DelegateLastClassLoader.class.getName();

    private static native String createClassloaderNamespace(ClassLoader classLoader, int i, String str, String str2, boolean z, boolean z2);

    private ClassLoaderFactory() {
    }

    public static boolean isValidClassLoaderName(String str) {
        return str != null && (isPathClassLoaderName(str) || isDelegateLastClassLoaderName(str));
    }

    public static boolean isPathClassLoaderName(String str) {
        return str == null || PATH_CLASS_LOADER_NAME.equals(str) || DEX_CLASS_LOADER_NAME.equals(str);
    }

    public static boolean isDelegateLastClassLoaderName(String str) {
        return DELEGATE_LAST_CLASS_LOADER_NAME.equals(str);
    }

    public static ClassLoader createClassLoader(String str, String str2, ClassLoader classLoader, String str3) {
        if (isPathClassLoaderName(str3)) {
            return new PathClassLoader(str, str2, classLoader);
        }
        if (isDelegateLastClassLoaderName(str3)) {
            return new DelegateLastClassLoader(str, str2, classLoader);
        }
        throw new AssertionError("Invalid classLoaderName: " + str3);
    }

    public static ClassLoader createClassLoader(String str, String str2, String str3, ClassLoader classLoader, int i, boolean z, String str4) {
        boolean z2;
        ClassLoader classLoaderCreateClassLoader = createClassLoader(str, str2, classLoader, str4);
        String[] strArrSplit = str.split(SettingsStringUtil.DELIMITER);
        int length = strArrSplit.length;
        int i2 = 0;
        while (true) {
            if (i2 < length) {
                if (!strArrSplit[i2].startsWith("/vendor/")) {
                    i2++;
                } else {
                    z2 = true;
                    break;
                }
            } else {
                z2 = false;
                break;
            }
        }
        Trace.traceBegin(64L, "createClassloaderNamespace");
        String strCreateClassloaderNamespace = createClassloaderNamespace(classLoaderCreateClassLoader, i, str2, str3, z, z2);
        Trace.traceEnd(64L);
        if (strCreateClassloaderNamespace != null) {
            throw new UnsatisfiedLinkError("Unable to create namespace for the classloader " + classLoaderCreateClassLoader + ": " + strCreateClassloaderNamespace);
        }
        return classLoaderCreateClassLoader;
    }
}
