package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;
import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;

public abstract class AtomicLongFieldUpdater<T> {
    public abstract boolean compareAndSet(T t, long j, long j2);

    public abstract long get(T t);

    public abstract void lazySet(T t, long j);

    public abstract void set(T t, long j);

    public abstract boolean weakCompareAndSet(T t, long j, long j2);

    @CallerSensitive
    public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> cls, String str) {
        Class stackClass1 = VMStack.getStackClass1();
        if (AtomicLong.VM_SUPPORTS_LONG_CAS) {
            return new CASUpdater(cls, str, stackClass1);
        }
        return new LockedUpdater(cls, str, stackClass1);
    }

    protected AtomicLongFieldUpdater() {
    }

    public long getAndSet(T t, long j) {
        long j2;
        do {
            j2 = get(t);
        } while (!compareAndSet(t, j2, j));
        return j2;
    }

    public long getAndIncrement(T t) {
        long j;
        do {
            j = get(t);
        } while (!compareAndSet(t, j, j + 1));
        return j;
    }

    public long getAndDecrement(T t) {
        long j;
        do {
            j = get(t);
        } while (!compareAndSet(t, j, j - 1));
        return j;
    }

    public long getAndAdd(T t, long j) {
        long j2;
        do {
            j2 = get(t);
        } while (!compareAndSet(t, j2, j2 + j));
        return j2;
    }

    public long incrementAndGet(T t) {
        long j;
        long j2;
        do {
            j = get(t);
            j2 = j + 1;
        } while (!compareAndSet(t, j, j2));
        return j2;
    }

    public long decrementAndGet(T t) {
        long j;
        long j2;
        do {
            j = get(t);
            j2 = j - 1;
        } while (!compareAndSet(t, j, j2));
        return j2;
    }

    public long addAndGet(T t, long j) {
        long j2;
        long j3;
        do {
            j2 = get(t);
            j3 = j2 + j;
        } while (!compareAndSet(t, j2, j3));
        return j3;
    }

    public final long getAndUpdate(T t, LongUnaryOperator longUnaryOperator) {
        long j;
        do {
            j = get(t);
        } while (!compareAndSet(t, j, longUnaryOperator.applyAsLong(j)));
        return j;
    }

    public final long updateAndGet(T t, LongUnaryOperator longUnaryOperator) {
        long j;
        long jApplyAsLong;
        do {
            j = get(t);
            jApplyAsLong = longUnaryOperator.applyAsLong(j);
        } while (!compareAndSet(t, j, jApplyAsLong));
        return jApplyAsLong;
    }

    public final long getAndAccumulate(T t, long j, LongBinaryOperator longBinaryOperator) {
        long j2;
        do {
            j2 = get(t);
        } while (!compareAndSet(t, j2, longBinaryOperator.applyAsLong(j2, j)));
        return j2;
    }

    public final long accumulateAndGet(T t, long j, LongBinaryOperator longBinaryOperator) {
        long j2;
        long jApplyAsLong;
        do {
            j2 = get(t);
            jApplyAsLong = longBinaryOperator.applyAsLong(j2, j);
        } while (!compareAndSet(t, j2, jApplyAsLong));
        return jApplyAsLong;
    }

    private static final class CASUpdater<T> extends AtomicLongFieldUpdater<T> {
        private static final Unsafe U = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        CASUpdater(Class<T> cls, String str, Class<?> cls2) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                int modifiers = declaredField.getModifiers();
                if (declaredField.getType() != Long.TYPE) {
                    throw new IllegalArgumentException("Must be long type");
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = Modifier.isProtected(modifiers) ? cls2 : cls;
                this.tclass = cls;
                this.offset = U.objectFieldOffset(declaredField);
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
        public final boolean compareAndSet(T t, long j, long j2) {
            accessCheck(t);
            return U.compareAndSwapLong(t, this.offset, j, j2);
        }

        @Override
        public final boolean weakCompareAndSet(T t, long j, long j2) {
            accessCheck(t);
            return U.compareAndSwapLong(t, this.offset, j, j2);
        }

        @Override
        public final void set(T t, long j) {
            accessCheck(t);
            U.putLongVolatile(t, this.offset, j);
        }

        @Override
        public final void lazySet(T t, long j) {
            accessCheck(t);
            U.putOrderedLong(t, this.offset, j);
        }

        @Override
        public final long get(T t) {
            accessCheck(t);
            return U.getLongVolatile(t, this.offset);
        }

        @Override
        public final long getAndSet(T t, long j) {
            accessCheck(t);
            return U.getAndSetLong(t, this.offset, j);
        }

        @Override
        public final long getAndAdd(T t, long j) {
            accessCheck(t);
            return U.getAndAddLong(t, this.offset, j);
        }

        @Override
        public final long getAndIncrement(T t) {
            return getAndAdd(t, 1L);
        }

        @Override
        public final long getAndDecrement(T t) {
            return getAndAdd(t, -1L);
        }

        @Override
        public final long incrementAndGet(T t) {
            return getAndAdd(t, 1L) + 1;
        }

        @Override
        public final long decrementAndGet(T t) {
            return getAndAdd(t, -1L) - 1;
        }

        @Override
        public final long addAndGet(T t, long j) {
            return getAndAdd(t, j) + j;
        }
    }

    private static final class LockedUpdater<T> extends AtomicLongFieldUpdater<T> {
        private static final Unsafe U = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        LockedUpdater(Class<T> cls, String str, Class<?> cls2) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                int modifiers = declaredField.getModifiers();
                if (declaredField.getType() != Long.TYPE) {
                    throw new IllegalArgumentException("Must be long type");
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = Modifier.isProtected(modifiers) ? cls2 : cls;
                this.tclass = cls;
                this.offset = U.objectFieldOffset(declaredField);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private final void accessCheck(T t) {
            if (!this.cclass.isInstance(t)) {
                throw accessCheckException(t);
            }
        }

        private final RuntimeException accessCheckException(T t) {
            if (this.cclass == this.tclass) {
                return new ClassCastException();
            }
            return new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + t.getClass().getName()));
        }

        @Override
        public final boolean compareAndSet(T t, long j, long j2) {
            accessCheck(t);
            synchronized (this) {
                if (U.getLong(t, this.offset) != j) {
                    return false;
                }
                U.putLong(t, this.offset, j2);
                return true;
            }
        }

        @Override
        public final boolean weakCompareAndSet(T t, long j, long j2) {
            return compareAndSet(t, j, j2);
        }

        @Override
        public final void set(T t, long j) {
            accessCheck(t);
            synchronized (this) {
                U.putLong(t, this.offset, j);
            }
        }

        @Override
        public final void lazySet(T t, long j) {
            set(t, j);
        }

        @Override
        public final long get(T t) {
            long j;
            accessCheck(t);
            synchronized (this) {
                j = U.getLong(t, this.offset);
            }
            return j;
        }
    }
}
