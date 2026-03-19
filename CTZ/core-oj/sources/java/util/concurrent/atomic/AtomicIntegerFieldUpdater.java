package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;
import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;

public abstract class AtomicIntegerFieldUpdater<T> {
    public abstract boolean compareAndSet(T t, int i, int i2);

    public abstract int get(T t);

    public abstract void lazySet(T t, int i);

    public abstract void set(T t, int i);

    public abstract boolean weakCompareAndSet(T t, int i, int i2);

    @CallerSensitive
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> cls, String str) {
        return new AtomicIntegerFieldUpdaterImpl(cls, str, VMStack.getStackClass1());
    }

    protected AtomicIntegerFieldUpdater() {
    }

    public int getAndSet(T t, int i) {
        int i2;
        do {
            i2 = get(t);
        } while (!compareAndSet(t, i2, i));
        return i2;
    }

    public int getAndIncrement(T t) {
        int i;
        do {
            i = get(t);
        } while (!compareAndSet(t, i, i + 1));
        return i;
    }

    public int getAndDecrement(T t) {
        int i;
        do {
            i = get(t);
        } while (!compareAndSet(t, i, i - 1));
        return i;
    }

    public int getAndAdd(T t, int i) {
        int i2;
        do {
            i2 = get(t);
        } while (!compareAndSet(t, i2, i2 + i));
        return i2;
    }

    public int incrementAndGet(T t) {
        int i;
        int i2;
        do {
            i = get(t);
            i2 = i + 1;
        } while (!compareAndSet(t, i, i2));
        return i2;
    }

    public int decrementAndGet(T t) {
        int i;
        int i2;
        do {
            i = get(t);
            i2 = i - 1;
        } while (!compareAndSet(t, i, i2));
        return i2;
    }

    public int addAndGet(T t, int i) {
        int i2;
        int i3;
        do {
            i2 = get(t);
            i3 = i2 + i;
        } while (!compareAndSet(t, i2, i3));
        return i3;
    }

    public final int getAndUpdate(T t, IntUnaryOperator intUnaryOperator) {
        int i;
        do {
            i = get(t);
        } while (!compareAndSet(t, i, intUnaryOperator.applyAsInt(i)));
        return i;
    }

    public final int updateAndGet(T t, IntUnaryOperator intUnaryOperator) {
        int i;
        int iApplyAsInt;
        do {
            i = get(t);
            iApplyAsInt = intUnaryOperator.applyAsInt(i);
        } while (!compareAndSet(t, i, iApplyAsInt));
        return iApplyAsInt;
    }

    public final int getAndAccumulate(T t, int i, IntBinaryOperator intBinaryOperator) {
        int i2;
        do {
            i2 = get(t);
        } while (!compareAndSet(t, i2, intBinaryOperator.applyAsInt(i2, i)));
        return i2;
    }

    public final int accumulateAndGet(T t, int i, IntBinaryOperator intBinaryOperator) {
        int i2;
        int iApplyAsInt;
        do {
            i2 = get(t);
            iApplyAsInt = intBinaryOperator.applyAsInt(i2, i);
        } while (!compareAndSet(t, i2, iApplyAsInt));
        return iApplyAsInt;
    }

    private static final class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
        private static final Unsafe U = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        AtomicIntegerFieldUpdaterImpl(final Class<T> cls, final String str, Class<?> cls2) {
            try {
                Field field = (Field) AccessController.doPrivileged(new PrivilegedExceptionAction<Field>() {
                    @Override
                    public Field run() throws NoSuchFieldException {
                        return cls.getDeclaredField(str);
                    }
                });
                int modifiers = field.getModifiers();
                if (field.getType() != Integer.TYPE) {
                    throw new IllegalArgumentException("Must be integer type");
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = Modifier.isProtected(modifiers) ? cls2 : cls;
                this.tclass = cls;
                this.offset = U.objectFieldOffset(field);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private final void accessCheck(T t) {
            if (!this.cclass.isInstance(t)) {
                throwAccessCheckException(t);
            }
        }

        private final void throwAccessCheckException(T t) {
            if (this.cclass == this.tclass) {
                throw new ClassCastException();
            }
            throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + t.getClass().getName()));
        }

        @Override
        public final boolean compareAndSet(T t, int i, int i2) {
            accessCheck(t);
            return U.compareAndSwapInt(t, this.offset, i, i2);
        }

        @Override
        public final boolean weakCompareAndSet(T t, int i, int i2) {
            accessCheck(t);
            return U.compareAndSwapInt(t, this.offset, i, i2);
        }

        @Override
        public final void set(T t, int i) {
            accessCheck(t);
            U.putIntVolatile(t, this.offset, i);
        }

        @Override
        public final void lazySet(T t, int i) {
            accessCheck(t);
            U.putOrderedInt(t, this.offset, i);
        }

        @Override
        public final int get(T t) {
            accessCheck(t);
            return U.getIntVolatile(t, this.offset);
        }

        @Override
        public final int getAndSet(T t, int i) {
            accessCheck(t);
            return U.getAndSetInt(t, this.offset, i);
        }

        @Override
        public final int getAndAdd(T t, int i) {
            accessCheck(t);
            return U.getAndAddInt(t, this.offset, i);
        }

        @Override
        public final int getAndIncrement(T t) {
            return getAndAdd(t, 1);
        }

        @Override
        public final int getAndDecrement(T t) {
            return getAndAdd(t, -1);
        }

        @Override
        public final int incrementAndGet(T t) {
            return getAndAdd(t, 1) + 1;
        }

        @Override
        public final int decrementAndGet(T t) {
            return getAndAdd(t, -1) - 1;
        }

        @Override
        public final int addAndGet(T t, int i) {
            return getAndAdd(t, i) + i;
        }
    }
}
