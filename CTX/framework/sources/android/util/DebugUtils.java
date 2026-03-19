package android.util;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

public class DebugUtils {
    public static boolean isObjectSelected(Object obj) {
        Method declaredMethod;
        String str = System.getenv("ANDROID_OBJECT_FILTER");
        if (str == null || str.length() <= 0) {
            return false;
        }
        String[] strArrSplit = str.split("@");
        if (obj.getClass().getSimpleName().matches(strArrSplit[0])) {
            boolean zMatches = false;
            for (int i = 1; i < strArrSplit.length; i++) {
                String[] strArrSplit2 = strArrSplit[i].split("=");
                Class<?> cls = obj.getClass();
                Class<?> cls2 = cls;
                while (true) {
                    try {
                        declaredMethod = cls2.getDeclaredMethod("get" + strArrSplit2[0].substring(0, 1).toUpperCase(Locale.ROOT) + strArrSplit2[0].substring(1), (Class[]) null);
                        Class<? super Object> superclass = cls.getSuperclass();
                        if (superclass == null || declaredMethod != null) {
                            break;
                        }
                        cls2 = superclass;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e2) {
                        e2.printStackTrace();
                    } catch (InvocationTargetException e3) {
                        e3.printStackTrace();
                    }
                }
                if (declaredMethod != null) {
                    Object objInvoke = declaredMethod.invoke(obj, (Object[]) null);
                    zMatches |= (objInvoke != null ? objInvoke.toString() : "null").matches(strArrSplit2[1]);
                }
            }
            return zMatches;
        }
        return false;
    }

    public static void buildShortClassTag(Object obj, StringBuilder sb) {
        int iLastIndexOf;
        if (obj == null) {
            sb.append("null");
            return;
        }
        String simpleName = obj.getClass().getSimpleName();
        if ((simpleName == null || simpleName.isEmpty()) && (iLastIndexOf = (simpleName = obj.getClass().getName()).lastIndexOf(46)) > 0) {
            simpleName = simpleName.substring(iLastIndexOf + 1);
        }
        sb.append(simpleName);
        sb.append('{');
        sb.append(Integer.toHexString(System.identityHashCode(obj)));
    }

    public static void printSizeValue(PrintWriter printWriter, long j) {
        String str;
        float f = j;
        String str2 = "";
        if (f > 900.0f) {
            str2 = "KB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "MB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "GB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "TB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "PB";
            f /= 1024.0f;
        }
        if (f < 1.0f) {
            str = String.format("%.2f", Float.valueOf(f));
        } else if (f < 10.0f) {
            str = String.format("%.1f", Float.valueOf(f));
        } else if (f < 100.0f) {
            str = String.format("%.0f", Float.valueOf(f));
        } else {
            str = String.format("%.0f", Float.valueOf(f));
        }
        printWriter.print(str);
        printWriter.print(str2);
    }

    public static String sizeValueToString(long j, StringBuilder sb) {
        String str;
        if (sb == null) {
            sb = new StringBuilder(32);
        }
        float f = j;
        String str2 = "";
        if (f > 900.0f) {
            str2 = "KB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "MB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "GB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "TB";
            f /= 1024.0f;
        }
        if (f > 900.0f) {
            str2 = "PB";
            f /= 1024.0f;
        }
        if (f < 1.0f) {
            str = String.format("%.2f", Float.valueOf(f));
        } else if (f < 10.0f) {
            str = String.format("%.1f", Float.valueOf(f));
        } else if (f < 100.0f) {
            str = String.format("%.0f", Float.valueOf(f));
        } else {
            str = String.format("%.0f", Float.valueOf(f));
        }
        sb.append(str);
        sb.append(str2);
        return sb.toString();
    }

    public static String valueToString(Class<?> cls, String str, int i) {
        for (Field field : cls.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType().equals(Integer.TYPE) && field.getName().startsWith(str)) {
                try {
                    if (i == field.getInt(null)) {
                        return constNameWithoutPrefix(str, field);
                    }
                    continue;
                } catch (IllegalAccessException e) {
                }
            }
        }
        return Integer.toString(i);
    }

    public static String flagsToString(Class<?> cls, String str, int i) {
        StringBuilder sb = new StringBuilder();
        boolean z = i == 0;
        for (Field field : cls.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers) && field.getType().equals(Integer.TYPE) && field.getName().startsWith(str)) {
                try {
                    int i2 = field.getInt(null);
                    if (i2 == 0 && z) {
                        return constNameWithoutPrefix(str, field);
                    }
                    if ((i & i2) != 0) {
                        i &= ~i2;
                        sb.append(constNameWithoutPrefix(str, field));
                        sb.append('|');
                    }
                } catch (IllegalAccessException e) {
                }
            }
        }
        if (i != 0 || sb.length() == 0) {
            sb.append(Integer.toHexString(i));
        } else {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static String constNameWithoutPrefix(String str, Field field) {
        return field.getName().substring(str.length());
    }
}
