package sun.reflect;

import java.lang.reflect.Modifier;

public class Reflection {
    public static void ensureMemberAccess(Class<?> cls, Class<?> cls2, Object obj, int i) throws IllegalAccessException {
        if (cls == null || cls2 == null) {
            throw new InternalError();
        }
        if (!verifyMemberAccess(cls, cls2, obj, i)) {
            throw new IllegalAccessException("Class " + cls.getName() + " can not access a member of class " + cls2.getName() + " with modifiers \"" + Modifier.toString(i) + "\"");
        }
    }

    public static boolean verifyMemberAccess(Class<?> cls, Class<?> cls2, Object obj, int i) {
        boolean z;
        boolean zIsSameClassPackage;
        Class<?> cls3;
        if (cls == cls2) {
            return true;
        }
        if (Modifier.isPublic(cls2.getAccessFlags())) {
            z = false;
            zIsSameClassPackage = false;
        } else {
            boolean zIsSameClassPackage2 = isSameClassPackage(cls, cls2);
            if (!zIsSameClassPackage2) {
                return false;
            }
            zIsSameClassPackage = zIsSameClassPackage2;
            z = true;
        }
        if (Modifier.isPublic(i)) {
            return true;
        }
        boolean z2 = Modifier.isProtected(i) && isSubclassOf(cls, cls2);
        if (!z2 && !Modifier.isPrivate(i)) {
            if (!z) {
                zIsSameClassPackage = isSameClassPackage(cls, cls2);
                z = true;
            }
            if (zIsSameClassPackage) {
                z2 = true;
            }
        }
        if (!z2) {
            return false;
        }
        if (Modifier.isProtected(i)) {
            if (obj != null) {
                cls3 = obj.getClass();
            } else {
                cls3 = cls2;
            }
            if (cls3 != cls) {
                if (!z) {
                    zIsSameClassPackage = isSameClassPackage(cls, cls2);
                }
                if (!zIsSameClassPackage && !isSubclassOf(cls3, cls)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isSameClassPackage(Class<?> cls, Class<?> cls2) {
        return isSameClassPackage(cls.getClassLoader(), cls.getName(), cls2.getClassLoader(), cls2.getName());
    }

    private static boolean isSameClassPackage(ClassLoader classLoader, String str, ClassLoader classLoader2, String str2) {
        int i;
        int i2;
        if (classLoader != classLoader2) {
            return false;
        }
        int iLastIndexOf = str.lastIndexOf(46);
        int iLastIndexOf2 = str2.lastIndexOf(46);
        if (iLastIndexOf == -1 || iLastIndexOf2 == -1) {
            return iLastIndexOf == iLastIndexOf2;
        }
        if (str.charAt(0) == '[') {
            int i3 = 0;
            do {
                i3++;
            } while (str.charAt(i3) == '[');
            if (str.charAt(i3) != 'L') {
                throw new InternalError("Illegal class name " + str);
            }
            i = i3;
        } else {
            i = 0;
        }
        if (str2.charAt(0) == '[') {
            int i4 = 0;
            do {
                i4++;
            } while (str2.charAt(i4) == '[');
            if (str2.charAt(i4) != 'L') {
                throw new InternalError("Illegal class name " + str2);
            }
            i2 = i4;
        } else {
            i2 = 0;
        }
        int i5 = iLastIndexOf - i;
        if (i5 != iLastIndexOf2 - i2) {
            return false;
        }
        return str.regionMatches(false, i, str2, i2, i5);
    }

    static boolean isSubclassOf(Class<?> cls, Class<?> cls2) {
        while (cls != null) {
            if (cls == cls2) {
                return true;
            }
            cls = cls.getSuperclass();
        }
        return false;
    }
}
