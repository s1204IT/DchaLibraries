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
            int iArrayIndexScale = U.arrayIndexScale(Object[].class);
            if (((iArrayIndexScale - 1) & iArrayIndexScale) != 0) {
                throw new Error("array index scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(iArrayIndexScale);
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

    public AtomicReferenceArray(int i) {
        this.array = new Object[i];
    }

    public AtomicReferenceArray(E[] eArr) {
        this.array = Arrays.copyOf(eArr, eArr.length, Object[].class);
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

    public final void set(int i, E e) {
        U.putObjectVolatile(this.array, checkedByteOffset(i), e);
    }

    public final void lazySet(int i, E e) {
        U.putOrderedObject(this.array, checkedByteOffset(i), e);
    }

    public final E getAndSet(int i, E e) {
        return (E) U.getAndSetObject(this.array, checkedByteOffset(i), e);
    }

    public final boolean compareAndSet(int i, E e, E e2) {
        return compareAndSetRaw(checkedByteOffset(i), e, e2);
    }

    private boolean compareAndSetRaw(long j, E e, E e2) {
        return U.compareAndSwapObject(this.array, j, e, e2);
    }

    public final boolean weakCompareAndSet(int i, E e, E e2) {
        return compareAndSet(i, e, e2);
    }

    public final E getAndUpdate(int i, UnaryOperator<E> unaryOperator) {
        E raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, unaryOperator.apply(raw)));
        return raw;
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
        E raw;
        long jCheckedByteOffset = checkedByteOffset(i);
        do {
            raw = getRaw(jCheckedByteOffset);
        } while (!compareAndSetRaw(jCheckedByteOffset, raw, binaryOperator.apply(raw, e)));
        return raw;
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
        int length = this.array.length - 1;
        if (length == -1) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (true) {
            sb.append((Object) getRaw(byteOffset(i)));
            if (i == length) {
                sb.append(']');
                return sb.toString();
            }
            sb.append(',');
            sb.append(' ');
            i++;
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        Object objCopyOf = objectInputStream.readFields().get("array", (Object) null);
        if (objCopyOf == null || !objCopyOf.getClass().isArray()) {
            throw new InvalidObjectException("Not array type");
        }
        if (objCopyOf.getClass() != Object[].class) {
            objCopyOf = Arrays.copyOf((Object[]) objCopyOf, Array.getLength(objCopyOf), Object[].class);
        }
        U.putObjectVolatile(this, ARRAY, objCopyOf);
    }
}
