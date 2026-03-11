package com.android.launcher3.util;

public abstract class Provider<T> {
    public abstract T get();

    public static <T> Provider<T> of(final T t) {
        return new Provider<T>() {
            @Override
            public T get() {
                return (T) t;
            }
        };
    }
}
