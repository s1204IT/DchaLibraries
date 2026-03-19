package com.android.server;

import android.util.ArrayMap;
import com.android.internal.annotations.VisibleForTesting;

public final class LocalServices {
    private static final ArrayMap<Class<?>, Object> sLocalServiceObjects = new ArrayMap<>();

    private LocalServices() {
    }

    public static <T> T getService(Class<T> cls) {
        T t;
        synchronized (sLocalServiceObjects) {
            t = (T) sLocalServiceObjects.get(cls);
        }
        return t;
    }

    public static <T> void addService(Class<T> cls, T t) {
        synchronized (sLocalServiceObjects) {
            if (sLocalServiceObjects.containsKey(cls)) {
                throw new IllegalStateException("Overriding service registration");
            }
            sLocalServiceObjects.put(cls, t);
        }
    }

    @VisibleForTesting
    public static <T> void removeServiceForTest(Class<T> cls) {
        synchronized (sLocalServiceObjects) {
            sLocalServiceObjects.remove(cls);
        }
    }
}
