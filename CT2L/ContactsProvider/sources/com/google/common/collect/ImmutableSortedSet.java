package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public abstract class ImmutableSortedSet<E> extends ImmutableSortedSetFauxverideShim<E> implements SortedIterable<E>, SortedSet<E> {
    final transient Comparator<? super E> comparator;
    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();
    private static final ImmutableSortedSet<Comparable> NATURAL_EMPTY_SET = new EmptyImmutableSortedSet(NATURAL_ORDER);

    abstract ImmutableSortedSet<E> headSetImpl(E e, boolean z);

    abstract int indexOf(Object obj);

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    abstract ImmutableSortedSet<E> subSetImpl(E e, boolean z, E e2, boolean z2);

    abstract ImmutableSortedSet<E> tailSetImpl(E e, boolean z);

    private static <E> ImmutableSortedSet<E> emptySet() {
        return (ImmutableSortedSet<E>) NATURAL_EMPTY_SET;
    }

    static <E> ImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
        return NATURAL_ORDER.equals(comparator) ? emptySet() : new EmptyImmutableSortedSet(comparator);
    }

    private static <E> ImmutableSortedSet<E> copyOfInternal(Comparator<? super E> comparator, Iterator<? extends E> elements) {
        ImmutableList<E> list = ImmutableList.copyOf(SortedIterables.sortedUnique(comparator, elements));
        return list.isEmpty() ? emptySet(comparator) : new RegularImmutableSortedSet(list, comparator);
    }

    public static final class Builder<E> extends ImmutableSet.Builder<E> {
        private final Comparator<? super E> comparator;

        public Builder(Comparator<? super E> comparator) {
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        @Override
        public Builder<E> add(E element) {
            super.add((Object) element);
            return this;
        }

        @Override
        public Builder<E> add(E... elements) {
            super.add((Object[]) elements);
            return this;
        }

        @Override
        public ImmutableSortedSet<E> build() {
            return ImmutableSortedSet.copyOfInternal(this.comparator, this.contents.iterator());
        }
    }

    int unsafeCompare(Object a, Object b) {
        return unsafeCompare(this.comparator, a, b);
    }

    static int unsafeCompare(Comparator<?> comparator, Object a, Object b) {
        return comparator.compare(a, b);
    }

    ImmutableSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    @Override
    public ImmutableSortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    ImmutableSortedSet<E> headSet(E toElement, boolean inclusive) {
        return headSetImpl(Preconditions.checkNotNull(toElement), inclusive);
    }

    @Override
    public ImmutableSortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    ImmutableSortedSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        Preconditions.checkNotNull(fromElement);
        Preconditions.checkNotNull(toElement);
        Preconditions.checkArgument(this.comparator.compare(fromElement, toElement) <= 0);
        return subSetImpl(fromElement, fromInclusive, toElement, toInclusive);
    }

    @Override
    public ImmutableSortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    ImmutableSortedSet<E> tailSet(E fromElement, boolean inclusive) {
        return tailSetImpl(Preconditions.checkNotNull(fromElement), inclusive);
    }

    private static class SerializedForm<E> implements Serializable {
        private static final long serialVersionUID = 0;
        final Comparator<? super E> comparator;
        final Object[] elements;

        public SerializedForm(Comparator<? super E> comparator, Object[] elements) {
            this.comparator = comparator;
            this.elements = elements;
        }

        Object readResolve() {
            return new Builder(this.comparator).add(this.elements).build();
        }
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this.comparator, toArray());
    }
}
