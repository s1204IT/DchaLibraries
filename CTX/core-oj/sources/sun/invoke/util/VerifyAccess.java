package sun.invoke.util;

import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;

public class VerifyAccess {
    static final boolean $assertionsDisabled = false;
    private static final boolean ALLOW_NESTMATE_ACCESS = false;
    private static final int ALL_ACCESS_MODES = 7;
    private static final int PACKAGE_ALLOWED = 8;
    private static final int PACKAGE_ONLY = 0;
    private static final int PROTECTED_OR_PACKAGE_ALLOWED = 12;

    private VerifyAccess() {
    }

    public static boolean isMemberAccessible(Class<?> cls, Class<?> cls2, int i, Class<?> cls3, int i2) {
        if (i2 == 0 || !isClassAccessible(cls, cls3, i2)) {
            return false;
        }
        if (cls2 == cls3 && (i2 & 2) != 0) {
            return true;
        }
        int i3 = i & 7;
        if (i3 == 4) {
            if ((i2 & 12) != 0 && isSamePackage(cls2, cls3)) {
                return true;
            }
            int i4 = i2 & 4;
            if (i4 == 0) {
                return false;
            }
            if (((i & 8) != 0 && !isRelatedClass(cls, cls3)) || i4 == 0 || !isSubClass(cls3, cls2)) {
                return false;
            }
            return true;
        }
        switch (i3) {
            case 0:
                if ((i2 & 8) == 0 || !isSamePackage(cls2, cls3)) {
                    return false;
                }
                return true;
            case 1:
                return true;
            case 2:
                return false;
            default:
                throw new IllegalArgumentException("bad modifiers: " + Modifier.toString(i));
        }
    }

    static boolean isRelatedClass(Class<?> cls, Class<?> cls2) {
        return cls == cls2 || isSubClass(cls, cls2) || isSubClass(cls2, cls);
    }

    static boolean isSubClass(Class<?> cls, Class<?> cls2) {
        return cls2.isAssignableFrom(cls) && !cls.isInterface();
    }

    public static boolean isClassAccessible(Class<?> cls, Class<?> cls2, int i) {
        if (i == 0) {
            return false;
        }
        if (Modifier.isPublic(cls.getModifiers())) {
            return true;
        }
        return (i & 8) != 0 && isSamePackage(cls2, cls);
    }

    public static boolean isTypeVisible(Class<?> cls, Class<?> cls2) {
        ClassLoader classLoader;
        if (cls == cls2) {
            return true;
        }
        while (cls.isArray()) {
            cls = cls.getComponentType();
        }
        if (cls.isPrimitive() || cls == Object.class || (classLoader = cls.getClassLoader()) == null) {
            return true;
        }
        ClassLoader classLoader2 = cls2.getClassLoader();
        if (classLoader2 == null) {
            return false;
        }
        if (classLoader == classLoader2 || loadersAreRelated(classLoader, classLoader2, true)) {
            return true;
        }
        try {
            if (cls == classLoader2.loadClass(cls.getName())) {
                return true;
            }
            return false;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isTypeVisible(MethodType methodType, Class<?> cls) {
        int iParameterCount = methodType.parameterCount();
        int i = -1;
        while (i < iParameterCount) {
            if (isTypeVisible(i < 0 ? methodType.returnType() : methodType.parameterType(i), cls)) {
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean isSamePackage(Class<?> cls, Class<?> cls2) {
        if (cls.isArray() || cls2.isArray()) {
            throw new IllegalArgumentException();
        }
        if (cls == cls2) {
            return true;
        }
        if (cls.getClassLoader() != cls2.getClassLoader()) {
            return false;
        }
        String name = cls.getName();
        String name2 = cls2.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        if (iLastIndexOf != name2.lastIndexOf(46)) {
            return false;
        }
        for (int i = 0; i < iLastIndexOf; i++) {
            if (name.charAt(i) != name2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static String getPackageName(Class<?> cls) {
        String name = cls.getName();
        int iLastIndexOf = name.lastIndexOf(46);
        return iLastIndexOf < 0 ? "" : name.substring(0, iLastIndexOf);
    }

    public static boolean isSamePackageMember(Class<?> cls, Class<?> cls2) {
        if (cls == cls2) {
            return true;
        }
        return isSamePackage(cls, cls2) && getOutermostEnclosingClass(cls) == getOutermostEnclosingClass(cls2);
    }

    private static Class<?> getOutermostEnclosingClass(Class<?> cls) {
        while (true) {
            Class<?> enclosingClass = cls.getEnclosingClass();
            if (enclosingClass != null) {
                cls = enclosingClass;
            } else {
                return cls;
            }
        }
    }

    private static boolean loadersAreRelated(ClassLoader classLoader, ClassLoader classLoader2, boolean z) {
        if (classLoader == classLoader2 || classLoader == null || (classLoader2 == null && !z)) {
            return true;
        }
        for (ClassLoader parent = classLoader2; parent != null; parent = parent.getParent()) {
            if (parent == classLoader) {
                return true;
            }
        }
        if (z) {
            return false;
        }
        while (classLoader != null) {
            if (classLoader == classLoader2) {
                return true;
            }
            classLoader = classLoader.getParent();
        }
        return false;
    }

    public static boolean classLoaderIsAncestor(Class<?> cls, Class<?> cls2) {
        return loadersAreRelated(cls.getClassLoader(), cls2.getClassLoader(), true);
    }
}
