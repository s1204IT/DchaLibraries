package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class Iterators {
    static final UnmodifiableListIterator<Object> EMPTY_LIST_ITERATOR = new UnmodifiableListIterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public Object previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int previousIndex() {
            return -1;
        }
    };
    private static final Iterator<Object> EMPTY_MODIFIABLE_ITERATOR = new Iterator<Object>() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            CollectPreconditions.checkRemove(false);
        }
    };

    private static class PeekingImpl<E> implements PeekingIterator<E> {
        private boolean hasPeeked;
        private final Iterator<? extends E> iterator;
        private E peekedElement;

        public PeekingImpl(Iterator<? extends E> it) {
            this.iterator = (Iterator) Preconditions.checkNotNull(it);
        }

        @Override
        public boolean hasNext() {
            return this.hasPeeked || this.iterator.hasNext();
        }

        @Override
        public E next() {
            if (!this.hasPeeked) {
                return this.iterator.next();
            }
            E e = this.peekedElement;
            this.hasPeeked = false;
            this.peekedElement = null;
            return e;
        }

        @Override
        public E peek() {
            if (!this.hasPeeked) {
                this.peekedElement = this.iterator.next();
                this.hasPeeked = true;
            }
            return this.peekedElement;
        }

        @Override
        public void remove() {
            Preconditions.checkState(!this.hasPeeked, "Can't remove after you've peeked at next");
            this.iterator.remove();
        }
    }

    public static <T> boolean addAll(Collection<T> collection, Iterator<? extends T> it) {
        Preconditions.checkNotNull(collection);
        Preconditions.checkNotNull(it);
        boolean zAdd = false;
        while (it.hasNext()) {
            zAdd |= collection.add(it.next());
        }
        return zAdd;
    }

    public static <T> boolean any(Iterator<T> it, Predicate<? super T> predicate) {
        return indexOf(it, predicate) != -1;
    }

    static void clear(Iterator<?> it) {
        Preconditions.checkNotNull(it);
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    public static boolean contains(Iterator<?> it, Object obj) {
        return any(it, Predicates.equalTo(obj));
    }

    public static boolean elementsEqual(Iterator<?> it, Iterator<?> it2) {
        while (it.hasNext()) {
            if (!it2.hasNext() || !Objects.equal(it.next(), it2.next())) {
                return false;
            }
        }
        return !it2.hasNext();
    }

    @Deprecated
    public static <T> UnmodifiableIterator<T> emptyIterator() {
        return emptyListIterator();
    }

    static <T> UnmodifiableListIterator<T> emptyListIterator() {
        return (UnmodifiableListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    static <T> Iterator<T> emptyModifiableIterator() {
        return (Iterator<T>) EMPTY_MODIFIABLE_ITERATOR;
    }

    public static <T> UnmodifiableIterator<T> forArray(T... tArr) {
        return forArray(tArr, 0, tArr.length, 0);
    }

    static <T> UnmodifiableListIterator<T> forArray(T[] tArr, int i, int i2, int i3) {
        Preconditions.checkArgument(i2 >= 0);
        Preconditions.checkPositionIndexes(i, i + i2, tArr.length);
        Preconditions.checkPositionIndex(i3, i2);
        return i2 == 0 ? emptyListIterator() : new AbstractIndexedListIterator<T>(i2, i3, tArr, i) {
            final Object[] val$array;
            final int val$offset;

            {
                this.val$array = tArr;
                this.val$offset = i;
            }

            @Override
            protected T get(int i4) {
                return (T) this.val$array[this.val$offset + i4];
            }
        };
    }

    public static <T> T getNext(Iterator<? extends T> it, T t) {
        return it.hasNext() ? it.next() : t;
    }

    public static <T> T getOnlyElement(Iterator<T> it) {
        T next = it.next();
        if (!it.hasNext()) {
            return next;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("expected one element but was: <" + next);
        for (int i = 0; i < 4 && it.hasNext(); i++) {
            sb.append(", " + it.next());
        }
        if (it.hasNext()) {
            sb.append(", ...");
        }
        sb.append('>');
        throw new IllegalArgumentException(sb.toString());
    }

    public static <T> int indexOf(Iterator<T> it, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate, "predicate");
        int i = 0;
        while (it.hasNext()) {
            if (predicate.apply(it.next())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static <T> PeekingIterator<T> peekingIterator(Iterator<? extends T> it) {
        return it instanceof PeekingImpl ? (PeekingImpl) it : new PeekingImpl(it);
    }

    static <T> T pollNext(Iterator<T> it) {
        if (!it.hasNext()) {
            return null;
        }
        T next = it.next();
        it.remove();
        return next;
    }

    public static boolean removeAll(Iterator<?> it, Collection<?> collection) {
        return removeIf(it, Predicates.in(collection));
    }

    public static <T> boolean removeIf(Iterator<T> it, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate);
        boolean z = false;
        while (it.hasNext()) {
            if (predicate.apply(it.next())) {
                it.remove();
                z = true;
            }
        }
        return z;
    }

    public static <T> UnmodifiableIterator<T> singletonIterator(T t) {
        return new UnmodifiableIterator<T>(t) {
            boolean done;
            final Object val$value;

            {
                this.val$value = t;
            }

            @Override
            public boolean hasNext() {
                return !this.done;
            }

            @Override
            public T next() {
                if (this.done) {
                    throw new NoSuchElementException();
                }
                this.done = true;
                return (T) this.val$value;
            }
        };
    }

    public static int size(Iterator<?> it) {
        int i = 0;
        while (it.hasNext()) {
            it.next();
            i++;
        }
        return i;
    }

    public static <F, T> Iterator<T> transform(Iterator<F> it, Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(function);
        return new TransformedIterator<F, T>(it, function) {
            final Function val$function;

            {
                this.val$function = function;
            }

            @Override
            T transform(F f) {
                return (T) this.val$function.apply(f);
            }
        };
    }

    public static <T> UnmodifiableIterator<T> unmodifiableIterator(Iterator<T> it) {
        Preconditions.checkNotNull(it);
        return it instanceof UnmodifiableIterator ? (UnmodifiableIterator) it : new UnmodifiableIterator<T>(it) {
            final Iterator val$iterator;

            {
                this.val$iterator = it;
            }

            @Override
            public boolean hasNext() {
                return this.val$iterator.hasNext();
            }

            @Override
            public T next() {
                return (T) this.val$iterator.next();
            }
        };
    }
}
