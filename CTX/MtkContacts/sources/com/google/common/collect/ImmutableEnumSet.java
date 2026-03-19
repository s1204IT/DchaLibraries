package com.google.common.collect;

import java.io.Serializable;
import java.lang.Enum;
import java.util.Collection;
import java.util.EnumSet;

final class ImmutableEnumSet<E extends Enum<E>> extends ImmutableSet<E> {
    private final transient EnumSet<E> delegate;
    private transient int hashCode;

    static <E extends Enum<E>> ImmutableSet<E> asImmutable(EnumSet<E> enumSet) {
        switch (enumSet.size()) {
            case 0:
                return ImmutableSet.of();
            case 1:
                return ImmutableSet.of(Iterables.getOnlyElement(enumSet));
            default:
                return new ImmutableEnumSet(enumSet);
        }
    }

    private ImmutableEnumSet(EnumSet<E> enumSet) {
        this.delegate = enumSet;
    }

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public UnmodifiableIterator<E> iterator() {
        return Iterators.unmodifiableIterator(this.delegate.iterator());
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean contains(Object obj) {
        return this.delegate.contains(obj);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return this.delegate.containsAll(collection);
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || this.delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        int i = this.hashCode;
        if (i != 0) {
            return i;
        }
        int iHashCode = this.delegate.hashCode();
        this.hashCode = iHashCode;
        return iHashCode;
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    @Override
    Object writeReplace() {
        return new EnumSerializedForm(this.delegate);
    }

    private static class EnumSerializedForm<E extends Enum<E>> implements Serializable {
        private static final long serialVersionUID = 0;
        final EnumSet<E> delegate;

        EnumSerializedForm(EnumSet<E> enumSet) {
            this.delegate = enumSet;
        }

        Object readResolve() {
            return new ImmutableEnumSet(this.delegate.clone());
        }
    }
}
