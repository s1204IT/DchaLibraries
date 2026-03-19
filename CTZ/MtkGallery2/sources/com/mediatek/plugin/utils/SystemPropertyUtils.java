package com.mediatek.plugin.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SystemPropertyUtils {
    private static final String TAG = "PluginManager/SystemPropertyUtils";
    private static Method sGetIntMethod;
    private static Method sGetMethod;
    private static Class<?> sSystemPropertiesClass;
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
                sIsSystemPropertiesExist = clsLoadClass != null;
                sHasChecked = true;
                if (sIsSystemPropertiesExist) {
                    sGetIntMethod = clsLoadClass.getDeclaredMethod("getInt", String.class, Integer.TYPE);
                    sGetIntMethod.setAccessible(true);
                    sGetMethod = clsLoadClass.getDeclaredMethod("get", String.class);
                    sGetMethod.setAccessible(true);
                }
            } catch (ClassNotFoundException e) {
                sIsSystemPropertiesExist = false;
                sHasChecked = true;
            } catch (NoSuchMethodException e2) {
                android.util.Log.e(TAG, "<isSystemPropertiesExist> NoSuchMethodException", e2);
            }
        }
        return sIsSystemPropertiesExist;
    }
}
