package com.google.common.collect;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

abstract class ImmutableAsList<E> extends ImmutableList<E> {
    abstract ImmutableCollection<E> delegateCollection();

    ImmutableAsList() {
    }

    @Override
    public boolean contains(Object obj) {
        return delegateCollection().contains(obj);
    }

    @Override
    public int size() {
        return delegateCollection().size();
    }

    @Override
    public boolean isEmpty() {
        return delegateCollection().isEmpty();
    }

    @Override
    boolean isPartialView() {
        return delegateCollection().isPartialView();
    }

    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableCollection<?> collection;

        SerializedForm(ImmutableCollection<?> immutableCollection) {
            this.collection = immutableCollection;
        }

        Object readResolve() {
            return this.collection.asList();
        }
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(delegateCollection());
    }
}
