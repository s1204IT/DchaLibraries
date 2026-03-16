package java.lang;

import java.io.Serializable;
import java.lang.Enum;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import libcore.util.BasicLruCache;
import libcore.util.EmptyArray;

public abstract class Enum<E extends Enum<E>> implements Serializable, Comparable<E> {
    private static final long serialVersionUID = -4300926546619394005L;
    private static final BasicLruCache<Class<? extends Enum>, Object[]> sharedConstantsCache = new BasicLruCache<Class<? extends Enum>, Object[]>(64) {
        @Override
        protected Object[] create(Class<? extends Enum> enumType) {
            if (!enumType.isEnum()) {
                return null;
            }
            try {
                Method method = enumType.getDeclaredMethod("values", EmptyArray.CLASS);
                method.setAccessible(true);
                return (Object[]) method.invoke((Object[]) null, new Object[0]);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError("impossible", impossible);
            } catch (NoSuchMethodException impossible2) {
                throw new AssertionError("impossible", impossible2);
            } catch (InvocationTargetException impossible3) {
                throw new AssertionError("impossible", impossible3);
            }
        }
    };
    private final String name;
    private final int ordinal;

    protected Enum(String name, int ordinal) {
        this.name = name;
        this.ordinal = ordinal;
    }

    public final String name() {
        return this.name;
    }

    public final int ordinal() {
        return this.ordinal;
    }

    public String toString() {
        return this.name;
    }

    public final boolean equals(Object other) {
        return this == other;
    }

    public final int hashCode() {
        return (this.name == null ? 0 : this.name.hashCode()) + this.ordinal;
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Enums may not be cloned");
    }

    @Override
    public final int compareTo(E o) {
        return this.ordinal - o.ordinal;
    }

    public final Class<E> getDeclaringClass() {
        Class<E> cls = (Class<E>) getClass();
        Class superclass = cls.getSuperclass();
        return Enum.class == superclass ? cls : superclass;
    }

    public static <T extends Enum<T>> T valueOf(Class<T> cls, String str) {
        if (cls == null) {
            throw new NullPointerException("enumType == null");
        }
        if (str == null) {
            throw new NullPointerException("name == null");
        }
        Enum[] sharedConstants = getSharedConstants(cls);
        if (sharedConstants == null) {
            throw new IllegalArgumentException(cls + " is not an enum type");
        }
        for (Enum r0 : sharedConstants) {
            T t = (T) r0;
            if (str.equals(t.name())) {
                return t;
            }
        }
        throw new IllegalArgumentException(str + " is not a constant in " + cls.getName());
    }

    public static <T extends Enum<T>> T[] getSharedConstants(Class<T> cls) {
        return (T[]) ((Enum[]) sharedConstantsCache.get(cls));
    }

    protected final void finalize() {
    }
}
