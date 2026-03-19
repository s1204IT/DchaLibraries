package com.mediatek.camera.portability;

import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtil {
    public static Object callMethodOnObject(Object obj, Method method, Object... objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            Log.e("ReflectUtil", "[callMethodOnObject]", e);
            return null;
        } catch (InvocationTargetException e2) {
            Log.e("ReflectUtil", "[callMethodOnObject]", e2);
            return null;
        }
    }

    public static Method getMethod(Class<?> cls, String str, Class<?>... clsArr) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            Log.e("ReflectUtil", "[getMethod]", e);
            return null;
        }
    }
}
