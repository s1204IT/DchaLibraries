package com.mediatek.plugin.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {
    private static final String TAG = "PluginManager/ReflectUtils";

    public static Object callMethodOnObject(Object obj, String str, Object... objArr) {
        return callMethodOnObject(obj, getMethod(obj.getClass(), str, getTypeForParameters(objArr)), objArr);
    }

    public static Object callMethodOnObject(Object obj, Method method, Object... objArr) {
        try {
            return method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "<callMethodOnObject>", e);
            return null;
        } catch (InvocationTargetException e2) {
            Log.e(TAG, "<callMethodOnObject>", e2);
            return null;
        }
    }

    public static Object createInstance(String str, ClassLoader classLoader, Object... objArr) {
        try {
            return createInstance(getConstructor(classLoader.loadClass(str), getTypeForParameters(objArr)), objArr);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "<createInstance>", e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "<createInstance>", e2);
            return null;
        }
    }

    public static Object createInstance(String str, Object... objArr) {
        return createInstance(getConstructor(str, getTypeForParameters(objArr)), objArr);
    }

    public static Object createInstance(Constructor<?> constructor, Object... objArr) {
        try {
            return constructor.newInstance(objArr);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "<createInstance>", e);
            return null;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "<createInstance>", e2);
            return null;
        } catch (InstantiationException e3) {
            Log.e(TAG, "<createInstance>", e3);
            return null;
        } catch (InvocationTargetException e4) {
            Log.e(TAG, "<createInstance>", e4);
            return null;
        }
    }

    public static Constructor<?> getConstructor(String str, Class<?>... clsArr) {
        try {
            Constructor<?> constructor = Class.forName(str).getConstructor(clsArr);
            constructor.setAccessible(true);
            return constructor;
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "<getConstructor>", e);
            return null;
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "<getConstructor>", e2);
            return null;
        }
    }

    public static Method getMethod(Class<?> cls, String str, Class<?>... clsArr) {
        try {
            Method declaredMethod = cls.getDeclaredMethod(str, clsArr);
            declaredMethod.setAccessible(true);
            return declaredMethod;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "<getMethod>", e);
            return null;
        }
    }

    public static Constructor<?> getConstructor(Class<?> cls, Class<?>... clsArr) {
        try {
            Constructor<?> declaredConstructor = cls.getDeclaredConstructor(clsArr);
            declaredConstructor.setAccessible(true);
            return declaredConstructor;
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "<getConstructor>", e);
            return null;
        }
    }

    private static Class<?>[] getTypeForParameters(Object... objArr) {
        int i = 0;
        if (objArr.length == 0) {
            return new Class[0];
        }
        Class<?>[] clsArr = new Class[objArr.length];
        int length = objArr.length;
        int i2 = 0;
        while (i < length) {
            clsArr[i2] = objArr[i].getClass();
            i++;
            i2++;
        }
        return clsArr;
    }
}
