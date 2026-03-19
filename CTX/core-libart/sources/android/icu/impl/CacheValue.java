package android.icu.impl;

import android.icu.util.ICUException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

public abstract class CacheValue<V> {
    private static volatile Strength strength = Strength.SOFT;
    private static final CacheValue NULL_VALUE = new NullValue();

    public enum Strength {
        STRONG,
        SOFT
    }

    public abstract V get();

    public abstract V resetIfCleared(V v);

    public static void setStrength(Strength strength2) {
        strength = strength2;
    }

    public static boolean futureInstancesWillBeStrong() {
        return strength == Strength.STRONG;
    }

    public static <V> CacheValue<V> getInstance(V v) {
        if (v == null) {
            return NULL_VALUE;
        }
        return strength == Strength.STRONG ? new StrongValue(v) : new SoftValue(v);
    }

    public boolean isNull() {
        return false;
    }

    private static final class NullValue<V> extends CacheValue<V> {
        private NullValue() {
        }

        @Override
        public boolean isNull() {
            return true;
        }

        @Override
        public V get() {
            return null;
        }

        @Override
        public V resetIfCleared(V v) {
            if (v != null) {
                throw new ICUException("resetting a null value to a non-null value");
            }
            return null;
        }
    }

    private static final class StrongValue<V> extends CacheValue<V> {
        private V value;

        StrongValue(V v) {
            this.value = v;
        }

        @Override
        public V get() {
            return this.value;
        }

        @Override
        public V resetIfCleared(V v) {
            return this.value;
        }
    }

    private static final class SoftValue<V> extends CacheValue<V> {
        private volatile Reference<V> ref;

        SoftValue(V v) {
            this.ref = new SoftReference(v);
        }

        @Override
        public V get() {
            return this.ref.get();
        }

        @Override
        public synchronized V resetIfCleared(V v) {
            V v2 = this.ref.get();
            if (v2 != null) {
                return v2;
            }
            this.ref = new SoftReference(v);
            return v;
        }
    }
}
