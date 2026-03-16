package android.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class DebugUtils {
    public static boolean isObjectSelected(Object object) {
        Method declaredMethod;
        boolean match = false;
        String s = System.getenv("ANDROID_OBJECT_FILTER");
        if (s != null && s.length() > 0) {
            String[] selectors = s.split("@");
            if (object.getClass().getSimpleName().matches(selectors[0])) {
                for (int i = 1; i < selectors.length; i++) {
                    String[] pair = selectors[i].split("=");
                    Class<?> klass = object.getClass();
                    Class<?> parent = klass;
                    do {
                        try {
                            declaredMethod = parent.getDeclaredMethod("get" + pair[0].substring(0, 1).toUpperCase(Locale.ROOT) + pair[0].substring(1), (Class[]) null);
                            parent = klass.getSuperclass();
                            if (parent == null) {
                                break;
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e2) {
                            e2.printStackTrace();
                        } catch (InvocationTargetException e3) {
                            e3.printStackTrace();
                        }
                    } while (declaredMethod == null);
                    if (declaredMethod != null) {
                        Object value = declaredMethod.invoke(object, (Object[]) null);
                        match |= (value != null ? value.toString() : "null").matches(pair[1]);
                    }
                }
            }
        }
        return match;
    }

    public static void buildShortClassTag(Object cls, StringBuilder out) {
        int end;
        if (cls == null) {
            out.append("null");
            return;
        }
        String simpleName = cls.getClass().getSimpleName();
        if ((simpleName == null || simpleName.isEmpty()) && (end = (simpleName = cls.getClass().getName()).lastIndexOf(46)) > 0) {
            simpleName = simpleName.substring(end + 1);
        }
        out.append(simpleName);
        out.append('{');
        out.append(Integer.toHexString(System.identityHashCode(cls)));
    }
}
