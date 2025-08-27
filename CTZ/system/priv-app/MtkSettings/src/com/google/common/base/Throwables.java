package com.google.common.base;

/* loaded from: classes.dex */
public final class Throwables {
    /* JADX INFO: Thrown type has an unknown type hierarchy: X extends java.lang.Throwable */
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
}
