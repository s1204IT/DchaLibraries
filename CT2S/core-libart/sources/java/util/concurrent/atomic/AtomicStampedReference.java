package java.util.concurrent.atomic;

import sun.misc.Unsafe;

public class AtomicStampedReference<V> {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final long pairOffset = objectFieldOffset(UNSAFE, "pair", AtomicStampedReference.class);
    private volatile Pair<V> pair;

    private static class Pair<T> {
        final T reference;
        final int stamp;

        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }

        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<>(reference, stamp);
        }
    }

    public AtomicStampedReference(V initialRef, int initialStamp) {
        this.pair = Pair.of(initialRef, initialStamp);
    }

    public V getReference() {
        return this.pair.reference;
    }

    public int getStamp() {
        return this.pair.stamp;
    }

    public V get(int[] stampHolder) {
        Pair<V> pair = this.pair;
        stampHolder[0] = pair.stamp;
        return pair.reference;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp) {
        return compareAndSet(expectedReference, newReference, expectedStamp, newStamp);
    }

    public boolean compareAndSet(V expectedReference, V newReference, int expectedStamp, int newStamp) {
        Pair<V> current = this.pair;
        return expectedReference == current.reference && expectedStamp == current.stamp && ((newReference == current.reference && newStamp == current.stamp) || casPair(current, Pair.of(newReference, newStamp)));
    }

    public void set(V newReference, int newStamp) {
        Pair<V> current = this.pair;
        if (newReference != current.reference || newStamp != current.stamp) {
            this.pair = Pair.of(newReference, newStamp);
        }
    }

    public boolean attemptStamp(V expectedReference, int newStamp) {
        Pair<V> current = this.pair;
        return expectedReference == current.reference && (newStamp == current.stamp || casPair(current, Pair.of(expectedReference, newStamp)));
    }

    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
    }

    static long objectFieldOffset(Unsafe UNSAFE2, String field, Class<?> klazz) {
        try {
            return UNSAFE2.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }
}
