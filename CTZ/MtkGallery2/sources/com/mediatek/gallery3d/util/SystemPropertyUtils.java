package com.mediatek.gallery3d.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertyUtils {
    private static Method sGetIntMethod;
    private static Method sGetMethod;
    private static boolean sIsSystemPropertiesExist = false;
    private static boolean sHasChecked = false;

    public static int getInt(String str, int i) {
        if (isSystemPropertiesExist() && sGetIntMethod != null) {
            try {
                return ((Integer) sGetIntMethod.invoke(null, str, Integer.valueOf(i))).intValue();
            } catch (IllegalAccessException e) {
                return i;
            } catch (InvocationTargetException e2) {
                return i;
            }
        }
        return i;
    }

    public static String get(String str) {
        if (isSystemPropertiesExist() && sGetMethod != null) {
            try {
                return (String) sGetMethod.invoke(null, str);
            } catch (IllegalAccessException e) {
                return "";
            } catch (InvocationTargetException e2) {
                return "";
            }
        }
        return "";
    }

    private static boolean isSystemPropertiesExist() {
        if (!sHasChecked) {
            try {
                Class<?> clsLoadClass = SystemPropertyUtils.class.getClassLoader().loadClass("android.os.SystemProperties");
                sGetIntMethod = clsLoadClass.getDeclaredMethod("getInt", String.class, Integer.TYPE);
                sGetIntMethod.setAccessible(true);
                sGetMethod = clsLoadClass.getDeclaredMethod("get", String.class);
                sGetMethod.setAccessible(true);
                sIsSystemPropertiesExist = clsLoadClass != null;
                sHasChecked = true;
            } catch (ClassNotFoundException e) {
                sIsSystemPropertiesExist = false;
                sHasChecked = true;
            } catch (NoSuchMethodException e2) {
                android.util.Log.e("MtkGallery2/SystemPropertyUtils", "<isSystemPropertiesExist> NoSuchMethodException", e2);
            }
        }
        return sIsSystemPropertiesExist;
    }
}
