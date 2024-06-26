package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.ListIterator;
@GwtCompatible
/* loaded from: a.zip:com/google/common/collect/UnmodifiableListIterator.class */
public abstract class UnmodifiableListIterator<E> extends UnmodifiableIterator<E> implements ListIterator<E> {
    @Override // java.util.ListIterator
    @Deprecated
    public final void add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.ListIterator
    @Deprecated
    public final void set(E e) {
        throw new UnsupportedOperationException();
    }
}
