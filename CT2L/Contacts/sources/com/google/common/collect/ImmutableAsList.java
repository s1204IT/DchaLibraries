package com.google.common.collect;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

final class ImmutableAsList<E> extends RegularImmutableList<E> {
    private final transient ImmutableCollection<E> collection;

    ImmutableAsList(Object[] array, ImmutableCollection<E> collection) {
        super(array, 0, array.length);
        this.collection = collection;
    }

    @Override
    public boolean contains(Object target) {
        return this.collection.contains(target);
    }

    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableCollection<?> collection;

        SerializedForm(ImmutableCollection<?> collection) {
            this.collection = collection;
        }

        Object readResolve() {
            return this.collection.asList();
        }
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this.collection);
    }
}
