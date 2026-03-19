package com.google.common.base;

public final class Throwables {
    public static <X extends Throwable> void propagateIfInstanceOf(Throwable th, Class<X> cls) throws Throwable {
        if (th != null && cls.isInstance(th)) {
            throw cls.cast(th);
        }
    }

    public static void propagateIfPossible(Throwable th) throws Throwable {
        propagateIfInstanceOf(th, Error.class);
        propagateIfInstanceOf(th, RuntimeException.class);
    }

    public static <X extends Throwable> void propagateIfPossible(Throwable th, Class<X> cls) throws Throwable {
        propagateIfInstanceOf(th, cls);
        propagateIfPossible(th);
    }

    public static RuntimeException propagate(Throwable th) throws Throwable {
        propagateIfPossible((Throwable) Preconditions.checkNotNull(th));
        throw new RuntimeException(th);
    }
}
