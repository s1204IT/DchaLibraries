package org.apache.http.client.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Deprecated
public class CloneUtils {
    public static Object clone(Object obj) throws Throwable {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Cloneable) {
            Class<?> clazz = obj.getClass();
            try {
                Method m = clazz.getMethod("clone", null);
                try {
                    return m.invoke(obj, null);
                } catch (IllegalAccessException ex) {
                    throw new IllegalAccessError(ex.getMessage());
                } catch (InvocationTargetException ex2) {
                    Throwable cause = ex2.getCause();
                    if (cause instanceof CloneNotSupportedException) {
                        throw cause;
                    }
                    throw new Error("Unexpected exception", cause);
                }
            } catch (NoSuchMethodException ex3) {
                throw new NoSuchMethodError(ex3.getMessage());
            }
        }
        throw new CloneNotSupportedException();
    }

    private CloneUtils() {
    }
}
