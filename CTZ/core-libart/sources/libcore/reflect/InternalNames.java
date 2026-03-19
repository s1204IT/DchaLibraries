package libcore.reflect;

import java.lang.reflect.Array;

public final class InternalNames {
    private InternalNames() {
    }

    public static Class<?> getClass(ClassLoader classLoader, String str) {
        if (str.startsWith("[")) {
            return Array.newInstance(getClass(classLoader, str.substring(1)), 0).getClass();
        }
        if (str.equals("Z")) {
            return Boolean.TYPE;
        }
        if (str.equals("B")) {
            return Byte.TYPE;
        }
        if (str.equals("S")) {
            return Short.TYPE;
        }
        if (str.equals("I")) {
            return Integer.TYPE;
        }
        if (str.equals("J")) {
            return Long.TYPE;
        }
        if (str.equals("F")) {
            return Float.TYPE;
        }
        if (str.equals("D")) {
            return Double.TYPE;
        }
        if (str.equals("C")) {
            return Character.TYPE;
        }
        if (str.equals("V")) {
            return Void.TYPE;
        }
        String strReplace = str.substring(1, str.length() - 1).replace('/', '.');
        try {
            return classLoader.loadClass(strReplace);
        } catch (ClassNotFoundException e) {
            NoClassDefFoundError noClassDefFoundError = new NoClassDefFoundError(strReplace);
            noClassDefFoundError.initCause(e);
            throw noClassDefFoundError;
        }
    }

    public static String getInternalName(Class<?> cls) {
        if (cls.isArray()) {
            return '[' + getInternalName(cls.getComponentType());
        }
        if (cls == Boolean.TYPE) {
            return "Z";
        }
        if (cls == Byte.TYPE) {
            return "B";
        }
        if (cls == Short.TYPE) {
            return "S";
        }
        if (cls == Integer.TYPE) {
            return "I";
        }
        if (cls == Long.TYPE) {
            return "J";
        }
        if (cls == Float.TYPE) {
            return "F";
        }
        if (cls == Double.TYPE) {
            return "D";
        }
        if (cls == Character.TYPE) {
            return "C";
        }
        if (cls == Void.TYPE) {
            return "V";
        }
        return 'L' + cls.getName().replace('.', '/') + ';';
    }
}
