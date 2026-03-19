package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Set;

public abstract class ForwardingSet<E> extends ForwardingCollection<E> implements Set<E> {
    @Override
    protected abstract Set<E> delegate();

    protected ForwardingSet() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || delegate().equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    protected boolean standardRemoveAll(Collection<?> collection) {
        return Sets.removeAllImpl(this, (Collection<?>) Preconditions.checkNotNull(collection));
    }

    protected boolean standardEquals(Object obj) {
        return Sets.equalsImpl(this, obj);
    }

    protected int standardHashCode() {
        return Sets.hashCodeImpl(this);
    }
}
