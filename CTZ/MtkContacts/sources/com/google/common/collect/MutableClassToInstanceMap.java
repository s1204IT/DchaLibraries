package com.google.common.collect;

import com.google.common.collect.MapConstraints;
import com.google.common.primitives.Primitives;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MutableClassToInstanceMap<B> extends MapConstraints.ConstrainedMap<Class<? extends B>, B> implements ClassToInstanceMap<B> {
    private static final MapConstraint<Class<?>, Object> VALUE_CAN_BE_CAST_TO_KEY = new MapConstraint<Class<?>, Object>() {
        @Override
        public void checkKeyValue(Class<?> cls, Object obj) {
            MutableClassToInstanceMap.cast(cls, obj);
        }
    };
    private static final long serialVersionUID = 0;

    @Override
    public Set entrySet() {
        return super.entrySet();
    }

    @Override
    public void putAll(Map map) {
        super.putAll(map);
    }

    public static <B> MutableClassToInstanceMap<B> create() {
        return new MutableClassToInstanceMap<>(new HashMap());
    }

    public static <B> MutableClassToInstanceMap<B> create(Map<Class<? extends B>, B> map) {
        return new MutableClassToInstanceMap<>(map);
    }

    private MutableClassToInstanceMap(Map<Class<? extends B>, B> map) {
        super(map, VALUE_CAN_BE_CAST_TO_KEY);
    }

    @Override
    public <T extends B> T putInstance(Class<T> cls, T t) {
        return (T) cast(cls, put(cls, t));
    }

    @Override
    public <T extends B> T getInstance(Class<T> cls) {
        return (T) cast(cls, get(cls));
    }

    private static <B, T extends B> T cast(Class<T> cls, B b) {
        return (T) Primitives.wrap(cls).cast(b);
    }
}
