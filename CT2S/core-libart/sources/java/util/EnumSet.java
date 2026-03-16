package java.util;

import java.io.Serializable;
import java.lang.Enum;

public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, Serializable {
    private static final long serialVersionUID = 1009687484059888093L;
    final Class<E> elementClass;

    abstract void complement();

    abstract void setRange(E e, E e2);

    EnumSet(Class<E> cls) {
        this.elementClass = cls;
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
        if (!elementType.isEnum()) {
            throw new ClassCastException(elementType.getClass().getName() + " is not an Enum");
        }
        Enum[] sharedConstants = Enum.getSharedConstants(elementType);
        return sharedConstants.length <= 64 ? new MiniEnumSet(elementType, sharedConstants) : new HugeEnumSet(elementType, sharedConstants);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
        EnumSet<E> set = noneOf(elementType);
        set.complement();
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
        EnumSet<E> set = noneOf(s.elementClass);
        set.addAll(s);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
        if (c instanceof EnumSet) {
            return copyOf((EnumSet) c);
        }
        if (c.isEmpty()) {
            throw new IllegalArgumentException("empty collection");
        }
        Iterator<E> iterator = c.iterator();
        E element = iterator.next();
        EnumSet<E> set = noneOf(element.getDeclaringClass());
        set.add(element);
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
        EnumSet<E> set = noneOf(s.elementClass);
        set.addAll(s);
        set.complement();
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e) {
        EnumSet<E> set = noneOf(e.getDeclaringClass());
        set.add(e);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2) {
        EnumSet<E> set = of(e1);
        set.add(e2);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3) {
        EnumSet<E> set = of(e1, e2);
        set.add(e3);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4) {
        EnumSet<E> set = of(e1, e2, e3);
        set.add(e4);
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumSet<E> set = of(e1, e2, e3, e4);
        set.add(e5);
        return set;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> of(E start, E... others) {
        EnumSet<E> set = of(start);
        for (E e : others) {
            set.add(e);
        }
        return set;
    }

    public static <E extends Enum<E>> EnumSet<E> range(E start, E end) {
        if (start.compareTo(end) > 0) {
            throw new IllegalArgumentException("start is behind end");
        }
        EnumSet<E> set = noneOf(start.getDeclaringClass());
        set.setRange(start, end);
        return set;
    }

    @Override
    public EnumSet<E> mo1clone() {
        try {
            return (EnumSet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    boolean isValidType(Class<?> cls) {
        return cls == this.elementClass || cls.getSuperclass() == this.elementClass;
    }

    private static class SerializationProxy<E extends Enum<E>> implements Serializable {
        private static final long serialVersionUID = 362491234563181265L;
        private Class<E> elementType;
        private E[] elements;

        private SerializationProxy() {
        }

        private Object readResolve() {
            EnumSet<E> set = EnumSet.noneOf(this.elementType);
            for (E e : this.elements) {
                set.add(e);
            }
            return set;
        }
    }

    Object writeReplace() {
        SerializationProxy proxy = new SerializationProxy();
        proxy.elements = (Enum[]) toArray(new Enum[0]);
        proxy.elementType = this.elementClass;
        return proxy;
    }
}
