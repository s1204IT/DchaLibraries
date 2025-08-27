package com.google.common.collect;

import java.util.NoSuchElementException;

/* loaded from: classes.dex */
public abstract class AbstractSequentialIterator<T> extends UnmodifiableIterator<T> {
    private T nextOrNull;

    protected abstract T computeNext(T t);

    protected AbstractSequentialIterator(T t) {
        this.nextOrNull = t;
    }

    @Override // java.util.Iterator
    public final boolean hasNext() {
        return this.nextOrNull != null;
    }

    @Override // java.util.Iterator
    public final T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            return this.nextOrNull;
        } finally {
            this.nextOrNull = computeNext(this.nextOrNull);
        }
    }
}
