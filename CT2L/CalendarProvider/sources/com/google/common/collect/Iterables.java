package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.Iterator;

public final class Iterables {
    public static String toString(Iterable<?> iterable) {
        return Iterators.toString(iterable.iterator());
    }

    public static <T> T getOnlyElement(Iterable<T> iterable) {
        return (T) Iterators.getOnlyElement(iterable.iterator());
    }

    public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(fromIterable);
        Preconditions.checkNotNull(function);
        return new IterableWithToString<T>() {
            @Override
            public Iterator<T> iterator() {
                return Iterators.transform(fromIterable.iterator(), function);
            }
        };
    }

    static abstract class IterableWithToString<E> implements Iterable<E> {
        IterableWithToString() {
        }

        public String toString() {
            return Iterables.toString(this);
        }
    }
}
