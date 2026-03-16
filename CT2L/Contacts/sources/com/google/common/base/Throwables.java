package com.google.common.base;

public final class Throwables {
    public static <X extends Throwable> void propagateIfInstanceOf(Throwable throwable, Class<X> declaredType) throws Throwable {
        if (throwable != null && declaredType.isInstance(throwable)) {
            throw declaredType.cast(throwable);
        }
    }
}
