package java.util.concurrent.atomic;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import sun.misc.Unsafe;

public class AtomicReferenceArray<E> implements Serializable {
    private static final int ABASE;
    private static final long ARRAY;
    private static final int ASHIFT;
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long serialVersionUID = -6209656149925076980L;
    private final Object[] array;

    static {
        try {
            ARRAY = U.objectFieldOffset(AtomicReferenceArray.class.getDeclaredField("array"));
            ABASE = U.arrayBaseOffset(Object[].class);
            int scale = U.arrayIndexScale(Object[].class);
            if (((scale - 1) & scale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
    }

    private long checkedByteOffset(int i) {
        if (i < 0 || i >= this.array.length) {
            throw new IndexOutOfBoundsException("index " + i);
        }
        return byteOffset(i);
    }

    private static long byteOffset(int i) {
        return (((long) i) << ASHIFT) + ((long) ABASE);
    }

    public AtomicReferenceArray(int length) {
        this.array = new Object[length];
    }

    public AtomicReferenceArray(E[] array) {
        this.array = Arrays.copyOf(array, array.length, Object[].class);
    }

    public final int length() {
        return this.array.length;
    }

    public final E get(int i) {
        return getRaw(checkedByteOffset(i));
    }

    private E getRaw(long j) {
        return (E) U.getObjectVolatile(this.array, j);
    }

    public final void set(int i, E newValue) {
        U.putObjectVolatile(this.array, checkedByteOffset(i), newValue);
    }

    public final void lazySet(int i, E newValue) {
        U.putOrderedObject(this.array, checkedByteOffset(i), newValue);
    }

    public final E getAndSet(int i, E e) {
        return (E) U.getAndSetObject(this.array, checkedByteOffset(i), e);
    }

    public final boolean compareAndSet(int i, E expect, E update) {
        return compareAndSetRaw(checkedByteOffset(i), expect, update);
    }

    private boolean compareAndSetRaw(long offset, E expect, E update) {
        return U.compareAndSwapObject(this.array, offset, expect, update);
    }

    public final boolean weakCompareAndSet(int i, E expect, E update) {
        return compareAndSet(i, expect, update);
    }

    public final E getAndUpdate(int i, UnaryOperator<E> unaryOperator) {
        E e;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            e = (E) getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, e, unaryOperator.apply(e)));
        return e;
    }

    public final E updateAndGet(int i, UnaryOperator<E> unaryOperator) {
        E raw;
        E e;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            e = (E) unaryOperator.apply(raw);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, e));
        return e;
    }

    public final E getAndAccumulate(int i, E e, BinaryOperator<E> binaryOperator) {
        E e2;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            e2 = (E) getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, e2, binaryOperator.apply(e2, e)));
        return e2;
    }

    public final E accumulateAndGet(int i, E e, BinaryOperator<E> binaryOperator) {
        E raw;
        E e2;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
            e2 = (E) binaryOperator.apply(raw, e);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, e2));
        return e2;
    }

    public String toString() {
        int iMax = this.array.length - 1;
        if (iMax == -1) {
            return "[]";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        while (true) {
            b.append(getRaw(byteOffset(i)));
            if (i == iMax) {
                return b.append(']').toString();
            }
            b.append(',').append(' ');
            i++;
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        Object a = s.readFields().get("array", (Object) null);
        if (a == null || !a.getClass().isArray()) {
            throw new InvalidObjectException("Not array type");
        }
        if (a.getClass() != Object[].class) {
            a = Arrays.copyOf((Object[]) a, Array.getLength(a), Object[].class);
        }
        U.putObjectVolatile(this, ARRAY, a);
    }
}
