package com.mediatek.server.telecom;

import android.telecom.Log;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionHelper {
    private static final String TAG = ReflectionHelper.class.getSimpleName();
    private static Result sFailure = new Result(false, null);

    public static Result callMethod(Object obj, String str, String str2, Object... objArr) throws InvocationTargetException {
        try {
            Class<?> cls = Class.forName(str);
            Class<?>[] clsArr = new Class[objArr.length];
            for (int i = 0; i < objArr.length; i++) {
                clsArr[i] = objArr[i].getClass();
            }
            Method declaredMethod = cls.getDeclaredMethod(str2, clsArr);
            declaredMethod.setAccessible(true);
            return new Result(true, declaredMethod.invoke(obj, objArr));
        } catch (ClassNotFoundException e) {
            e = e;
            Log.e(TAG, e, "[callMethod]Failed to call %s.%s", new Object[]{str, str2});
            return sFailure;
        } catch (IllegalAccessException e2) {
            e = e2;
            Log.e(TAG, e, "[callMethod]Failed to call %s.%s", new Object[]{str, str2});
            return sFailure;
        } catch (NoSuchMethodException e3) {
            e = e3;
            Log.e(TAG, e, "[callMethod]Failed to call %s.%s", new Object[]{str, str2});
            return sFailure;
        } catch (InvocationTargetException e4) {
            Log.e(TAG, e4, "[callMethod]Exception occurred to the invoke of %s.%s, throw it", new Object[]{str, str2});
            throw e4;
        }
    }

    public static Result callStaticMethod(String str, String str2, Object... objArr) throws InvocationTargetException {
        return callMethod(null, str, str2, objArr);
    }

    public static class Result {
        public Object mReturn;
        public boolean mSuccess;

        public Result(boolean z, Object obj) {
            this.mSuccess = z;
            this.mReturn = obj;
        }
    }
}
