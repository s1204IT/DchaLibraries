package com.google.common.primitives;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Primitives {
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE;
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE_TYPE;

    static {
        HashMap map = new HashMap(16);
        HashMap map2 = new HashMap(16);
        add(map, map2, Boolean.TYPE, Boolean.class);
        add(map, map2, Byte.TYPE, Byte.class);
        add(map, map2, Character.TYPE, Character.class);
        add(map, map2, Double.TYPE, Double.class);
        add(map, map2, Float.TYPE, Float.class);
        add(map, map2, Integer.TYPE, Integer.class);
        add(map, map2, Long.TYPE, Long.class);
        add(map, map2, Short.TYPE, Short.class);
        add(map, map2, Void.TYPE, Void.class);
        PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(map);
        WRAPPER_TO_PRIMITIVE_TYPE = Collections.unmodifiableMap(map2);
    }

    private static void add(Map<Class<?>, Class<?>> map, Map<Class<?>, Class<?>> map2, Class<?> cls, Class<?> cls2) {
        map.put(cls, cls2);
        map2.put(cls2, cls);
    }

    public static <T> Class<T> wrap(Class<T> cls) {
        Preconditions.checkNotNull(cls);
        Class<T> cls2 = (Class) PRIMITIVE_TO_WRAPPER_TYPE.get(cls);
        return cls2 == null ? cls : cls2;
    }
}
