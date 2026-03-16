package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

public abstract class AtomicLongFieldUpdater<T> {
    public abstract boolean compareAndSet(T t, long j, long j2);

    public abstract long get(T t);

    public abstract void lazySet(T t, long j);

    public abstract void set(T t, long j);

    public abstract boolean weakCompareAndSet(T t, long j, long j2);

    public static <U> AtomicLongFieldUpdater<U> newUpdater(Class<U> tclass, String fieldName) {
        return AtomicLong.VM_SUPPORTS_LONG_CAS ? new CASUpdater(tclass, fieldName) : new LockedUpdater(tclass, fieldName);
    }

    protected AtomicLongFieldUpdater() {
    }

    public long getAndSet(T obj, long newValue) {
        long current;
        do {
            current = get(obj);
        } while (!compareAndSet(obj, current, newValue));
        return current;
    }

    public long getAndIncrement(T obj) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current + 1;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public long getAndDecrement(T obj) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current - 1;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public long getAndAdd(T obj, long delta) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current + delta;
        } while (!compareAndSet(obj, current, next));
        return current;
    }

    public long incrementAndGet(T obj) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current + 1;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    public long decrementAndGet(T obj) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current - 1;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    public long addAndGet(T obj, long delta) {
        long current;
        long next;
        do {
            current = get(obj);
            next = current + delta;
        } while (!compareAndSet(obj, current, next));
        return next;
    }

    private static class CASUpdater<T> extends AtomicLongFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        CASUpdater(Class<T> tclass, String fieldName) {
            try {
                Field field = tclass.getDeclaredField(fieldName);
                Class<?> caller = VMStack.getStackClass2();
                int modifiers = field.getModifiers();
                Class<?> fieldt = field.getType();
                if (fieldt != Long.TYPE) {
                    throw new IllegalArgumentException("Must be long type");
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
        public boolean compareAndSet(T obj, long expect, long update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.compareAndSwapLong(obj, this.offset, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, long expect, long update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.compareAndSwapLong(obj, this.offset, expect, update);
        }

        @Override
        public void set(T obj, long newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            unsafe.putLongVolatile(obj, this.offset, newValue);
        }

        @Override
        public void lazySet(T obj, long newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            unsafe.putOrderedLong(obj, this.offset, newValue);
        }

        @Override
        public long get(T obj) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            return unsafe.getLongVolatile(obj, this.offset);
        }

        private void ensureProtectedAccess(T obj) {
            if (this.cclass.isInstance(obj)) {
            } else {
                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
        }
    }

    private static class LockedUpdater<T> extends AtomicLongFieldUpdater<T> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;

        LockedUpdater(Class<T> tclass, String fieldName) {
            try {
                Field field = tclass.getDeclaredField(fieldName);
                Class<?> caller = VMStack.getStackClass2();
                int modifiers = field.getModifiers();
                Class<?> fieldt = field.getType();
                if (fieldt != Long.TYPE) {
                    throw new IllegalArgumentException("Must be long type");
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
        public boolean compareAndSet(T obj, long expect, long update) {
            boolean z;
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            synchronized (this) {
                long v = unsafe.getLong(obj, this.offset);
                if (v != expect) {
                    z = false;
                } else {
                    unsafe.putLong(obj, this.offset, update);
                    z = true;
                }
            }
            return z;
        }

        @Override
        public boolean weakCompareAndSet(T obj, long expect, long update) {
            return compareAndSet(obj, expect, update);
        }

        @Override
        public void set(T obj, long newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            synchronized (this) {
                unsafe.putLong(obj, this.offset, newValue);
            }
        }

        @Override
        public void lazySet(T obj, long newValue) {
            set(obj, newValue);
        }

        @Override
        public long get(T obj) {
            long j;
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null) {
                fullCheck(obj);
            }
            synchronized (this) {
                j = unsafe.getLong(obj, this.offset);
            }
            return j;
        }

        private void ensureProtectedAccess(T obj) {
            if (this.cclass.isInstance(obj)) {
            } else {
                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
        }
    }
}
