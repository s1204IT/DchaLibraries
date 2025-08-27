package com.android.launcher3.util;

/* loaded from: classes.dex */
public abstract class Provider<T> {
    public abstract T get();

    public static <T> Provider<T> of(final T t) {
        return new Provider<T>() { // from class: com.android.launcher3.util.Provider.1
            @Override // com.android.launcher3.util.Provider
            public T get() {
                return (T) t;
            }
        };
    }
}
