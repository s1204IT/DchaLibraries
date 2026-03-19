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
    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl(tclass, fieldName, VMStack.getStackClass1());
    }

    protected AtomicIntegerFieldUpdater() {
    }

    public int getAndSet(T obj, int newValue) {
        int prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public int getAndIncrement(T obj) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public int getAndDecrement(T obj) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public int getAndAdd(T obj, int delta) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public int incrementAndGet(T obj) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev + 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public int decrementAndGet(T obj) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev - 1;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public int addAndGet(T obj, int delta) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = prev + delta;
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final int getAndUpdate(T obj, IntUnaryOperator updateFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final int updateAndGet(T obj, IntUnaryOperator updateFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(obj, prev, next));
        return next;
    }

    public final int getAndAccumulate(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return prev;
    }

    public final int accumulateAndGet(T obj, int x, IntBinaryOperator accumulatorFunction) {
        int prev;
        int next;
        do {
            prev = get(obj);
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(obj, prev, next));
        return next;
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

        private final void accessCheck(T obj) {
            if (this.cclass.isInstance(obj)) {
                return;
            }
            throwAccessCheckException(obj);
        }

        private final void throwAccessCheckException(T obj) {
            if (this.cclass != this.tclass) {
                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
            throw new ClassCastException();
        }

        @Override
        public final boolean compareAndSet(T obj, int expect, int update) {
            accessCheck(obj);
            return U.compareAndSwapInt(obj, this.offset, expect, update);
        }

        @Override
        public final boolean weakCompareAndSet(T obj, int expect, int update) {
            accessCheck(obj);
            return U.compareAndSwapInt(obj, this.offset, expect, update);
        }

        @Override
        public final void set(T obj, int newValue) {
            accessCheck(obj);
            U.putIntVolatile(obj, this.offset, newValue);
        }

        @Override
        public final void lazySet(T obj, int newValue) {
            accessCheck(obj);
            U.putOrderedInt(obj, this.offset, newValue);
        }

        @Override
        public final int get(T obj) {
            accessCheck(obj);
            return U.getIntVolatile(obj, this.offset);
        }

        @Override
        public final int getAndSet(T obj, int newValue) {
            accessCheck(obj);
            return U.getAndSetInt(obj, this.offset, newValue);
        }

        @Override
        public final int getAndAdd(T obj, int delta) {
            accessCheck(obj);
            return U.getAndAddInt(obj, this.offset, delta);
        }

        @Override
        public final int getAndIncrement(T obj) {
            return getAndAdd(obj, 1);
        }

        @Override
        public final int getAndDecrement(T obj) {
            return getAndAdd(obj, -1);
        }

        @Override
        public final int incrementAndGet(T obj) {
            return getAndAdd(obj, 1) + 1;
        }

        @Override
        public final int decrementAndGet(T obj) {
            return getAndAdd(obj, -1) - 1;
        }

        @Override
        public final int addAndGet(T obj, int delta) {
            return getAndAdd(obj, delta) + delta;
        }
    }
}
