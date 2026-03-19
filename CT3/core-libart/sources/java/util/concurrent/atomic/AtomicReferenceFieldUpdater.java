package java.util.concurrent.atomic;

import dalvik.system.VMStack;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;

public abstract class AtomicReferenceFieldUpdater<T, V> {
    public abstract boolean compareAndSet(T t, V v, V v2);

    public abstract V get(T t);

    public abstract void lazySet(T t, V v);

    public abstract void set(T t, V v);

    public abstract boolean weakCompareAndSet(T t, V v, V v2);

    @CallerSensitive
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> tclass, Class<W> vclass, String fieldName) {
        return new AtomicReferenceFieldUpdaterImpl(tclass, vclass, fieldName, VMStack.getStackClass1());
    }

    protected AtomicReferenceFieldUpdater() {
    }

    public V getAndSet(T obj, V newValue) {
        V prev;
        do {
            prev = get(obj);
        } while (!compareAndSet(obj, prev, newValue));
        return prev;
    }

    public final V getAndUpdate(T t, UnaryOperator<V> unaryOperator) {
        V v;
        do {
            v = (V) get(t);
        } while (!compareAndSet(t, v, unaryOperator.apply(v)));
        return v;
    }

    public final V updateAndGet(T t, UnaryOperator<V> unaryOperator) {
        V v;
        V v2;
        do {
            v = get(t);
            v2 = (V) unaryOperator.apply(v);
        } while (!compareAndSet(t, v, v2));
        return v2;
    }

    public final V getAndAccumulate(T t, V v, BinaryOperator<V> binaryOperator) {
        V v2;
        do {
            v2 = (V) get(t);
        } while (!compareAndSet(t, v2, binaryOperator.apply(v2, v)));
        return v2;
    }

    public final V accumulateAndGet(T t, V v, BinaryOperator<V> binaryOperator) {
        V v2;
        V v3;
        do {
            v2 = get(t);
            v3 = (V) binaryOperator.apply(v2, v);
        } while (!compareAndSet(t, v2, v3));
        return v3;
    }

    private static final class AtomicReferenceFieldUpdaterImpl<T, V> extends AtomicReferenceFieldUpdater<T, V> {
        private static final Unsafe U = Unsafe.getUnsafe();
        private final Class<?> cclass;
        private final long offset;
        private final Class<T> tclass;
        private final Class<V> vclass;

        AtomicReferenceFieldUpdaterImpl(Class<T> cls, Class<V> cls2, String str, Class<?> cls3) {
            try {
                Field declaredField = cls.getDeclaredField(str);
                int modifiers = declaredField.getModifiers();
                if (cls2 != declaredField.getType()) {
                    throw new ClassCastException();
                }
                if (cls2.isPrimitive()) {
                    throw new IllegalArgumentException("Must be reference type");
                }
                if (!Modifier.isVolatile(modifiers)) {
                    throw new IllegalArgumentException("Must be volatile type");
                }
                this.cclass = Modifier.isProtected(modifiers) ? cls3 : cls;
                this.tclass = cls;
                this.vclass = cls2;
                this.offset = U.objectFieldOffset(declaredField);
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

        private final void valueCheck(V v) {
            if (v == null || this.vclass.isInstance(v)) {
                return;
            }
            throwCCE();
        }

        static void throwCCE() {
            throw new ClassCastException();
        }

        @Override
        public final boolean compareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, this.offset, expect, update);
        }

        @Override
        public final boolean weakCompareAndSet(T obj, V expect, V update) {
            accessCheck(obj);
            valueCheck(update);
            return U.compareAndSwapObject(obj, this.offset, expect, update);
        }

        @Override
        public final void set(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putObjectVolatile(obj, this.offset, newValue);
        }

        @Override
        public final void lazySet(T obj, V newValue) {
            accessCheck(obj);
            valueCheck(newValue);
            U.putOrderedObject(obj, this.offset, newValue);
        }

        @Override
        public final V get(T t) {
            accessCheck(t);
            return (V) U.getObjectVolatile(t, this.offset);
        }

        @Override
        public final V getAndSet(T t, V v) {
            accessCheck(t);
            valueCheck(v);
            return (V) U.getAndSetObject(t, this.offset, v);
        }
    }
}
