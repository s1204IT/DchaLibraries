package com.mediatek.server.pm;

import android.util.Slog;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class ReflectionHelper {
    ReflectionHelper() {
    }

    public static Class getNonPublicInnerClass(Class cls, String str) {
        for (Class<?> cls2 : cls.getDeclaredClasses()) {
            if (cls2.toString().contains(str)) {
                return cls2;
            }
        }
        return null;
    }

    public static Field getNonPublicField(Class cls, String str) {
        try {
            return cls.getDeclaredField(str);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getObjectValue(Field field, Object obj) {
        field.setAccessible(true);
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            return null;
        }
    }

    public static boolean getBooleanValue(Field field, Object obj) {
        field.setAccessible(true);
        try {
            return field.getBoolean(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            return false;
        }
    }

    public static int getIntValue(Field field, Object obj) {
        field.setAccessible(true);
        try {
            return field.getInt(obj);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return 0;
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
            return 0;
        }
    }

    public static Object getObjectValue(Class cls, String str, Object obj) {
        return getObjectValue(getNonPublicField(cls, str), obj);
    }

    public static boolean getBooleanValue(Class cls, String str, Object obj) {
        return getBooleanValue(getNonPublicField(cls, str), obj);
    }

    public static int getIntValue(Class cls, String str) {
        return getIntValue(getNonPublicField(cls, str), cls);
    }

    public static Method getMethod(Class cls, String str, Class... clsArr) {
        Method declaredMethod;
        try {
            declaredMethod = cls.getDeclaredMethod(str, clsArr);
            if (declaredMethod != null) {
                try {
                    declaredMethod.setAccessible(true);
                } catch (NoSuchMethodException e) {
                    e = e;
                    e.printStackTrace();
                }
            }
        } catch (NoSuchMethodException e2) {
            e = e2;
            declaredMethod = null;
        }
        return declaredMethod;
    }

    public static Object callMethod(Method method, Object obj, Object... objArr) {
        Object objInvoke;
        Object obj2 = null;
        if (method == null) {
            return null;
        }
        try {
            objInvoke = method.invoke(obj, objArr);
        } catch (IllegalAccessException e) {
            e = e;
        } catch (IllegalArgumentException e2) {
            e = e2;
        } catch (InvocationTargetException e3) {
            e = e3;
        }
        try {
            Slog.d("PmsExtImpl", "callMethod:" + method.getName() + " ret=" + objInvoke);
            return objInvoke;
        } catch (IllegalAccessException e4) {
            e = e4;
            obj2 = objInvoke;
            e.printStackTrace();
            return obj2;
        } catch (IllegalArgumentException e5) {
            e = e5;
            obj2 = objInvoke;
            e.printStackTrace();
            return obj2;
        } catch (InvocationTargetException e6) {
            e = e6;
            obj2 = objInvoke;
            e.printStackTrace();
            return obj2;
        }
    }

    public static void setFieldValue(Class cls, Object obj, String str, Object obj2) {
        try {
            Field declaredField = cls.getDeclaredField(str);
            declaredField.setAccessible(true);
            declaredField.set(obj, obj2);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
        } catch (NoSuchFieldException e3) {
            e3.printStackTrace();
        }
    }

    public static void setFieldValue(Object obj, String str, Object obj2) {
        setFieldValue(obj.getClass(), obj, str, obj2);
    }
}
