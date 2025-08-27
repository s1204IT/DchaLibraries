package com.google.common.base;

import java.io.Serializable;

/* loaded from: classes.dex */
public final class Suppliers {

    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Supplier<T> delegate;

        public String toString() {
            return "Suppliers.memoize(" + this.delegate + ")";
        }
    }

    static class ExpiringMemoizingSupplier<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Supplier<T> delegate;
        final long durationNanos;

        public String toString() {
            return "Suppliers.memoizeWithExpiration(" + this.delegate + ", " + this.durationNanos + ", NANOS)";
        }
    }
}
