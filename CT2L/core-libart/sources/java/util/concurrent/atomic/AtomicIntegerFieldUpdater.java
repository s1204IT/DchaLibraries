package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

public abstract class AtomicIntegerFieldUpdater<T> {
    public abstract boolean compareAndSet(T t, int i, int i2);

    public abstract int get(T t);

    public abstract void lazySet(T t, int i);

    public abstract void set(T t, int i);

    public abstract boolean weakCompareAndSet(T t, int i, int i2);

    public static <U> AtomicIntegerFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return new AtomicIntegerFieldUpdaterImpl(tclass, fieldName);
    }

    protected AtomicIntegerFieldUpdater() {
    }

    public int getAndSet(T obj, int newValue) {
        int current;
        do {
            current = get(obj);
        } while (!compareAndSet(obj, current, newValue));
        return current;
    }

    public int getAndIncrement(T obj) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current + 1;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public int getAndDecrement(T obj) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current - 1;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public int getAndAdd(T obj, int delta) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current + delta;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public int incrementAndGet(T obj) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current + 1;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    public int decrementAndGet(T obj) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current - 1;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    public int addAndGet(T obj, int delta) {
        int current;
        int next;
        do {
            current = get(obj);
            next = current + delta;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    private static class AtomicIntegerFieldUpdaterImpl<T> extends AtomicIntegerFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        AtomicIntegerFieldUpdaterImpl(Class<T> tclass, String fieldName) {
            try {
                Field field = tclass.getDeclaredField(fieldName);
                Class<?> caller = VMStack.getStackClass2();
                int modifiers = field.getModifiers();
                Class<?> fieldt = field.getType();
                if (fieldt != Integer.TYPE) {
                    throw new IllegalArgumentException("Must be integer type");
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = (!Modifier.isProtected(modifiers) || caller == tclass) ? null : caller;
                this.tclass = tclass;
                this.offset = unsafe.objectFieldOffset(field);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        private void fullCheck(T obj) {
            if (!this.tclass.isInstance(obj)) {
                throw new ClassCastException();
            }
            if (this.cclass != null) {
                ensureProtectedAccess(obj);
            }
        }

        @Override
        public boolean compareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.compareAndSwapInt(obj, this.offset, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, int expect, int update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.compareAndSwapInt(obj, this.offset, expect, update);
        }

        @Override
        public void set(T obj, int newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            unsafe.putIntVolatile(obj, this.offset, newValue);
        }

        @Override
        public void lazySet(T obj, int newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            unsafe.putOrderedInt(obj, this.offset, newValue);
        }

        @Override
        public final int get(T obj) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.getIntVolatile(obj, this.offset);
        }

        private void ensureProtectedAccess(T obj) {
            if (this.cclass.isInstance(obj)) {
            } else {
                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
        }
    }
}
