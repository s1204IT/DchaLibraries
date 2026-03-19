package sun.reflect.misc;

import java.lang.reflect.Proxy;

public final class ReflectUtil {
    private ReflectUtil() {
    }

    public static Class<?> forName(String str) throws ClassNotFoundException {
        checkPackageAccess(str);
        return Class.forName(str);
    }

    public static Object newInstance(Class<?> cls) throws IllegalAccessException, InstantiationException {
        checkPackageAccess(cls);
        return cls.newInstance();
    }

    private static boolean isSubclassOf(Class<?> cls, Class<?> cls2) {
        while (cls != null) {
            if (cls == cls2) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    public static void checkPackageAccess(Class<?> cls) {
        checkPackageAccess(cls.getName());
        if (isNonPublicProxyClass(cls)) {
            checkProxyPackageAccess(cls);
        }
    }

    public static void checkPackageAccess(String str) {
        int iLastIndexOf;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            String strReplace = str.replace('/', '.');
            if (strReplace.startsWith("[") && (iLastIndexOf = strReplace.lastIndexOf(91) + 2) > 1 && iLastIndexOf < strReplace.length()) {
                strReplace = strReplace.substring(iLastIndexOf);
            }
            int iLastIndexOf2 = strReplace.lastIndexOf(46);
            if (iLastIndexOf2 != -1) {
                securityManager.checkPackageAccess(strReplace.substring(0, iLastIndexOf2));
            }
        }
    }

    public static boolean isPackageAccessible(Class<?> cls) {
        try {
            checkPackageAccess(cls);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private static boolean isAncestor(ClassLoader classLoader, ClassLoader classLoader2) {
        do {
            classLoader2 = classLoader2.getParent();
            if (classLoader == classLoader2) {
                return true;
            }
        } while (classLoader2 != null);
        return false;
    }

    public static boolean needsPackageAccessCheck(ClassLoader classLoader, ClassLoader classLoader2) {
        if (classLoader == null || classLoader == classLoader2) {
            return false;
        }
        if (classLoader2 == null) {
            return true;
        }
        return !isAncestor(classLoader, classLoader2);
    }

    public static void checkProxyPackageAccess(Class<?> cls) {
        if (System.getSecurityManager() != null && Proxy.isProxyClass(cls)) {
            for (Class<?> cls2 : cls.getInterfaces()) {
                checkPackageAccess(cls2);
            }
        }
    }

    public static void checkProxyPackageAccess(ClassLoader classLoader, Class<?>... clsArr) {
        if (System.getSecurityManager() != null) {
            for (Class<?> cls : clsArr) {
                if (needsPackageAccessCheck(classLoader, cls.getClassLoader())) {
                    checkPackageAccess(cls);
                }
            }
        }
    }

    public static boolean isNonPublicProxyClass(Class<?> cls) {
        String name = cls.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        return Proxy.isProxyClass(cls) && !(iLastIndexOf != -1 ? name.substring(0, iLastIndexOf) : "").isEmpty();
    }
}
