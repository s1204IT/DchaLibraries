package java.lang;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.Enum;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import libcore.util.BasicLruCache;
import libcore.util.EmptyArray;

public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {
    private static final BasicLruCache<Class<? extends Enum>, Object[]> sharedConstantsCache = new BasicLruCache<Class<? extends Enum>, Object[]>(64) {
        protected Object[] create(Class<? extends Enum> cls) {
            if (!cls.isEnum()) {
                return null;
            }
            try {
                Method declaredMethod = cls.getDeclaredMethod("values", EmptyArray.CLASS);
                declaredMethod.setAccessible(true);
                return (Object[]) declaredMethod.invoke((Object[]) null, new Object[0]);
            } catch (IllegalAccessException e) {
                throw new AssertionError("impossible", e);
            } catch (NoSuchMethodException e2) {
                throw new AssertionError("impossible", e2);
            } catch (InvocationTargetException e3) {
                throw new AssertionError("impossible", e3);
            }
        }
    };
    private final String name;
    private final int ordinal;

    public final String name() {
        return this.name;
    }

    public final int ordinal() {
        return this.ordinal;
    }

    protected Enum(String str, int i) {
        this.name = str;
        this.ordinal = i;
    }

    public String toString() {
        return this.name;
    }

    public final boolean equals(Object obj) {
        return this == obj;
    }

    public final int hashCode() {
        return super.hashCode();
    }

    protected final Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public final int compareTo(E e) {
        if (getClass() != e.getClass() && getDeclaringClass() != e.getDeclaringClass()) {
            throw new ClassCastException();
        }
        return this.ordinal - e.ordinal;
    }

    public final Class<E> getDeclaringClass() {
        Class<E> cls = (Class<E>) getClass();
        Class superclass = cls.getSuperclass();
        return superclass == Enum.class ? cls : superclass;
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
            throw new IllegalArgumentException(cls.toString() + " is not an enum type.");
        }
        for (int length = sharedConstants.length - 1; length >= 0; length--) {
            T t = (T) sharedConstants[length];
            if (str.equals(t.name())) {
                return t;
            }
        }
        throw new IllegalArgumentException("No enum constant " + cls.getCanonicalName() + "." + str);
    }

    public static <T extends Enum<T>> T[] getSharedConstants(Class<T> cls) {
        return (T[]) ((Enum[]) sharedConstantsCache.get(cls));
    }

    protected final void finalize() {
    }

    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        throw new InvalidObjectException("can't deserialize enum");
    }

    private void readObjectNoData() throws ObjectStreamException {
        throw new InvalidObjectException("can't deserialize enum");
    }
}
