package android.icu.impl;

import android.icu.util.ICUException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

public abstract class CacheValue<V> {
    private static volatile Strength strength = Strength.SOFT;
    private static final CacheValue NULL_VALUE = new NullValue(null);

    public abstract V get();

    public abstract V resetIfCleared(V v);

    public enum Strength {
        STRONG,
        SOFT;

        public static Strength[] valuesCustom() {
            return values();
        }
    }

    public static void setStrength(Strength strength2) {
        strength = strength2;
    }

    public static boolean futureInstancesWillBeStrong() {
        return strength == Strength.STRONG;
    }

    public static <V> CacheValue<V> getInstance(V value) {
        if (value == null) {
            return NULL_VALUE;
        }
        return strength == Strength.STRONG ? new StrongValue(value) : new SoftValue(value);
    }

    public boolean isNull() {
        return false;
    }

    private static final class NullValue<V> extends CacheValue<V> {
        NullValue(NullValue nullValue) {
            this();
        }

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
        public V resetIfCleared(V value) {
            if (value != null) {
                throw new ICUException("resetting a null value to a non-null value");
            }
            return null;
        }
    }

    private static final class StrongValue<V> extends CacheValue<V> {
        private V value;

        StrongValue(V value) {
            this.value = value;
        }

        @Override
        public V get() {
            return this.value;
        }

        @Override
        public V resetIfCleared(V value) {
            return this.value;
        }
    }

    private static final class SoftValue<V> extends CacheValue<V> {
        private Reference<V> ref;

        SoftValue(V value) {
            this.ref = new SoftReference(value);
        }

        @Override
        public V get() {
            return this.ref.get();
        }

        @Override
        public synchronized V resetIfCleared(V value) {
            V oldValue = this.ref.get();
            if (oldValue == null) {
                this.ref = new SoftReference(value);
                return value;
            }
            return oldValue;
        }
    }
}
