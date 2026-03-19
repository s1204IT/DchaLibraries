package java.util;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.Enum;

public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable {
    private static Enum<?>[] ZERO_LENGTH_ENUM_ARRAY = new Enum[0];
    final Class<E> elementType;
    final Enum<?>[] universe;

    abstract void addAll();

    abstract void addRange(E e, E e2);

    abstract void complement();

    EnumSet(Class<E> cls, Enum<?>[] enumArr) {
        this.elementType = cls;
        this.universe = enumArr;
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> cls) {
        Enum[] universe = getUniverse(cls);
        if (universe == null) {
            throw new ClassCastException(((Object) cls) + " not an enum");
        }
        if (universe.length <= 64) {
            return new RegularEnumSet(cls, universe);
        }
        return new JumboEnumSet(cls, universe);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> cls) {
        EnumSet<E> enumSetNoneOf = noneOf(cls);
        enumSetNoneOf.addAll();
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> enumSet) {
        return enumSet.clone();
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> collection) {
        if (collection instanceof EnumSet) {
            return ((EnumSet) collection).clone();
        }
        if (collection.isEmpty()) {
            throw new IllegalArgumentException("Collection is empty");
        }
        Iterator<E> it = collection.iterator();
        EnumSet<E> enumSetOf = of(it.next());
        while (it.hasNext()) {
            enumSetOf.add(it.next());
        }
        return enumSetOf;
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> enumSet) {
        EnumSet<E> enumSetCopyOf = copyOf((EnumSet) enumSet);
        enumSetCopyOf.complement();
        return enumSetCopyOf;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e, E e2) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        enumSetNoneOf.add(e2);
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e, E e2, E e3) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        enumSetNoneOf.add(e2);
        enumSetNoneOf.add(e3);
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e, E e2, E e3, E e4) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        enumSetNoneOf.add(e2);
        enumSetNoneOf.add(e3);
        enumSetNoneOf.add(e4);
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e, E e2, E e3, E e4, E e5) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        enumSetNoneOf.add(e2);
        enumSetNoneOf.add(e3);
        enumSetNoneOf.add(e4);
        enumSetNoneOf.add(e5);
        return enumSetNoneOf;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> of(E e, E... eArr) {
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.add(e);
        for (E e2 : eArr) {
            enumSetNoneOf.add(e2);
        }
        return enumSetNoneOf;
    }

    public static <E extends Enum<E>> EnumSet<E> range(E e, E e2) {
        if (e.compareTo(e2) > 0) {
            throw new IllegalArgumentException(((Object) e) + " > " + ((Object) e2));
        }
        EnumSet<E> enumSetNoneOf = noneOf(e.getDeclaringClass());
        enumSetNoneOf.addRange(e, e2);
        return enumSetNoneOf;
    }

    @Override
    public EnumSet<E> clone() {
        try {
            return (EnumSet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    final void typeCheck(E e) {
        Class<?> cls = e.getClass();
        if (cls != this.elementType && cls.getSuperclass() != this.elementType) {
            throw new ClassCastException(((Object) cls) + " != " + ((Object) this.elementType));
        }
    }

    private static <E extends Enum<E>> E[] getUniverse(Class<E> cls) {
        return cls.getEnumConstantsShared();
    }

    private static class SerializationProxy<E extends Enum<E>> implements Serializable {
        private static final long serialVersionUID = 362491234563181265L;
        private final Class<E> elementType;
        private final Enum<?>[] elements;

        SerializationProxy(EnumSet<E> enumSet) {
            this.elementType = enumSet.elementType;
            this.elements = (Enum[]) enumSet.toArray(EnumSet.ZERO_LENGTH_ENUM_ARRAY);
        }

        private Object readResolve() {
            EnumSet enumSetNoneOf = EnumSet.noneOf(this.elementType);
            for (Enum<?> r0 : this.elements) {
                enumSetNoneOf.add(r0);
            }
            return enumSetNoneOf;
        }
    }

    Object writeReplace() {
        return new SerializationProxy(this);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Proxy required");
    }
}
