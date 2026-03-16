package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

public abstract class AtomicReferenceFieldUpdater<T, V> {
    public abstract boolean compareAndSet(T t, V v, V v2);

    public abstract V get(T t);

    public abstract void lazySet(T t, V v);

    public abstract void set(T t, V v);

    public abstract boolean weakCompareAndSet(T t, V v, V v2);

    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl(tclass, vclass, fieldName);
    }

    protected AtomicReferenceFieldUpdater() {
    }

    public V getAndSet(T obj, V newValue) {
        V current;
        do {
            current = get(obj);
        } while (!compareAndSet(obj, current, newValue));
        return current;
    }

    private static final class AtomicReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
        private static final Unsafe unsafe = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;
        private final Class<V> vclass;

        AtomicReferenceFieldUpdaterImpl(Class<T> tclass, Class<V> vclass, String fieldName) {
            try {
                Field field = tclass.getDeclaredField(fieldName);
                Class<?> caller = VMStack.getStackClass2();
                int modifiers = field.getModifiers();
                Class<?> fieldClass = field.getType();
                if (vclass != fieldClass) {
                    throw new ClassCastException();
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = (!Modifier.isProtected(modifiers) || caller == tclass) ? null : caller;
                this.tclass = tclass;
                if (vclass == Object.class) {
                    this.vclass = null;
                } else {
                    this.vclass = vclass;
                }
                this.offset = unsafe.objectFieldOffset(field);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        void targetCheck(T obj) {
            if (!this.tclass.isInstance(obj)) {
                throw new ClassCastException();
            }
            if (this.cclass != null) {
                ensureProtectedAccess(obj);
            }
        }

        void updateCheck(T obj, V update) {
            if (!this.tclass.isInstance(obj) || (update != null && this.vclass != null && !this.vclass.isInstance(update))) {
                throw new ClassCastException();
            }
            if (this.cclass != null) {
                ensureProtectedAccess(obj);
            }
        }

        @Override
        public boolean compareAndSet(T obj, V expect, V update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null || (update != null && this.vclass != null && this.vclass != update.getClass())) {
                updateCheck(obj, update);
            }
            return unsafe.compareAndSwapObject(obj, this.offset, expect, update);
        }

        @Override
        public boolean weakCompareAndSet(T obj, V expect, V update) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null || (update != null && this.vclass != null && this.vclass != update.getClass())) {
                updateCheck(obj, update);
            }
            return unsafe.compareAndSwapObject(obj, this.offset, expect, update);
        }

        @Override
        public void set(T obj, V newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null || (newValue != null && this.vclass != null && this.vclass != newValue.getClass())) {
                updateCheck(obj, newValue);
            }
            unsafe.putObjectVolatile(obj, this.offset, newValue);
        }

        @Override
        public void lazySet(T obj, V newValue) {
            if (obj == null || obj.getClass() != this.tclass || this.cclass != null || (newValue != null && this.vclass != null && this.vclass != newValue.getClass())) {
                updateCheck(obj, newValue);
            }
            unsafe.putOrderedObject(obj, this.offset, newValue);
        }

        @Override
        public V get(T t) {
            if (t == null || t.getClass() != this.tclass || this.cclass != null) {
                targetCheck(t);
            }
            return (V) unsafe.getObjectVolatile(t, this.offset);
        }

        private void ensureProtectedAccess(T obj) {
            if (this.cclass.isInstance(obj)) {
            } else {
                throw new RuntimeException(new IllegalAccessException("Class " + this.cclass.getName() + " can not access a protected member of class " + this.tclass.getName() + " using an instance of " + obj.getClass().getName()));
            }
        }
    }
}
