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
    public static <U, W> AtomicReferenceFieldUpdater<U, W> newUpdater(Class<U> cls, Class<W> cls2, String str) {
        return new AtomicReferenceFieldUpdaterImpl(cls, cls2, str, VMStack.getStackClass1());
    }

    protected AtomicReferenceFieldUpdater() {
    }

    public V getAndSet(T t, V v) {
        V v2;
        do {
            v2 = get(t);
        } while (!compareAndSet(t, v2, v));
        return v2;
    }

    public final V getAndUpdate(T t, UnaryOperator<V> unaryOperator) {
        V v;
        do {
            v = get(t);
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
            v2 = get(t);
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

        private final void valueCheck(V v) {
            if (v != null && !this.vclass.isInstance(v)) {
                throwCCE();
            }
        }

        static void throwCCE() {
            throw new ClassCastException();
        }

        @Override
        public final boolean compareAndSet(T t, V v, V v2) {
            accessCheck(t);
            valueCheck(v2);
            return U.compareAndSwapObject(t, this.offset, v, v2);
        }

        @Override
        public final boolean weakCompareAndSet(T t, V v, V v2) {
            accessCheck(t);
            valueCheck(v2);
            return U.compareAndSwapObject(t, this.offset, v, v2);
        }

        @Override
        public final void set(T t, V v) {
            accessCheck(t);
            valueCheck(v);
            U.putObjectVolatile(t, this.offset, v);
        }

        @Override
        public final void lazySet(T t, V v) {
            accessCheck(t);
            valueCheck(v);
            U.putOrderedObject(t, this.offset, v);
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
