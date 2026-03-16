package java.util.concurrent.atomic;

import sun.misc.Unsafe;

public class AtomicMarkableReference<V> {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final long pairOffset = objectFieldOffset(UNSAFE, "pair", AtomicMarkableReference.class);
    private volatile Pair<V> pair;

    private static class Pair<T> {
        final boolean mark;
        final T reference;

        private Pair(T reference, boolean mark) {
            this.reference = reference;
            this.mark = mark;
        }

        static <T> Pair<T> of(T reference, boolean mark) {
            return new Pair<>(reference, mark);
        }
    }

    public AtomicMarkableReference(V initialRef, boolean initialMark) {
        this.pair = Pair.of(initialRef, initialMark);
    }

    public V getReference() {
        return this.pair.reference;
    }

    public boolean isMarked() {
        return this.pair.mark;
    }

    public V get(boolean[] markHolder) {
        Pair<V> pair = this.pair;
        markHolder[0] = pair.mark;
        return pair.reference;
    }

    public boolean weakCompareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        return compareAndSet(expectedReference, newReference, expectedMark, newMark);
    }

    public boolean compareAndSet(V expectedReference, V newReference, boolean expectedMark, boolean newMark) {
        Pair<V> current = this.pair;
        return expectedReference == current.reference && expectedMark == current.mark && ((newReference == current.reference && newMark == current.mark) || casPair(current, Pair.of(newReference, newMark)));
    }

    public void set(V newReference, boolean newMark) {
        Pair<V> current = this.pair;
        if (newReference != current.reference || newMark != current.mark) {
            this.pair = Pair.of(newReference, newMark);
        }
    }

    public boolean attemptMark(V expectedReference, boolean newMark) {
        Pair<V> current = this.pair;
        return expectedReference == current.reference && (newMark == current.mark || casPair(current, Pair.of(expectedReference, newMark)));
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
