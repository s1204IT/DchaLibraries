package com.google.common.collect;

import android.R;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.NavigableSet;

public abstract class ImmutableSortedSet<E> extends ImmutableSortedSetFauxverideShim<E> implements SortedIterable<E>, NavigableSet<E> {
    final transient Comparator<? super E> comparator;
    transient ImmutableSortedSet<E> descendingSet;
    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();
    private static final ImmutableSortedSet<Comparable> NATURAL_EMPTY_SET = new EmptyImmutableSortedSet(NATURAL_ORDER);

    public static final class Builder<E> extends ImmutableSet.Builder<E> {
        private final Comparator<? super E> comparator;

        public Builder(Comparator<? super E> comparator) {
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        @Override
        public Builder<E> add(E e) {
            super.add((Object) e);
            return this;
        }

        @Override
        public Builder<E> add(E... eArr) {
            super.add((Object[]) eArr);
            return this;
        }

        @Override
        public Builder<E> addAll(Iterable<? extends E> iterable) {
            super.addAll((Iterable) iterable);
            return this;
        }

        @Override
        public ImmutableSortedSet<E> build() {
            ImmutableSortedSet<E> immutableSortedSetConstruct = ImmutableSortedSet.construct(this.comparator, this.size, this.contents);
            this.size = immutableSortedSetConstruct.size();
            return immutableSortedSetConstruct;
        }
    }

    private static class SerializedForm<E> implements Serializable {
        private static final long serialVersionUID = 0;
        final Comparator<? super E> comparator;
        final Object[] elements;

        public SerializedForm(Comparator<? super E> comparator, Object[] objArr) {
            this.comparator = comparator;
            this.elements = objArr;
        }

        Object readResolve() {
            return new Builder(this.comparator).add(this.elements).build();
        }
    }

    ImmutableSortedSet(Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    static <E> ImmutableSortedSet<E> construct(Comparator<? super E> comparator, int i, E... eArr) {
        if (i == 0) {
            return emptySet(comparator);
        }
        ObjectArrays.checkElementsNotNull(eArr, i);
        Arrays.sort(eArr, 0, i, comparator);
        int i2 = 1;
        for (int i3 = 1; i3 < i; i3++) {
            R.color colorVar = (Object) eArr[i3];
            if (comparator.compare(colorVar, (Object) eArr[i2 - 1]) != 0) {
                eArr[i2] = colorVar;
                i2++;
            }
        }
        Arrays.fill(eArr, i2, i, (Object) null);
        return new RegularImmutableSortedSet(ImmutableList.asImmutableList(eArr, i2), comparator);
    }

    private static <E> ImmutableSortedSet<E> emptySet() {
        return (ImmutableSortedSet<E>) NATURAL_EMPTY_SET;
    }

    static <E> ImmutableSortedSet<E> emptySet(Comparator<? super E> comparator) {
        return NATURAL_ORDER.equals(comparator) ? emptySet() : new EmptyImmutableSortedSet(comparator);
    }

    private void readObject(ObjectInputStream objectInputStream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    static int unsafeCompare(Comparator<?> comparator, Object obj, Object obj2) {
        return comparator.compare(obj, obj2);
    }

    public E ceiling(E e) {
        return (E) Iterables.getFirst(tailSet((Object) e, true), null);
    }

    @Override
    public Comparator<? super E> comparator() {
        return this.comparator;
    }

    ImmutableSortedSet<E> createDescendingSet() {
        return new DescendingImmutableSortedSet(this);
    }

    @Override
    public abstract UnmodifiableIterator<E> descendingIterator();

    @Override
    public ImmutableSortedSet<E> descendingSet() {
        ImmutableSortedSet<E> immutableSortedSet = this.descendingSet;
        if (immutableSortedSet != null) {
            return immutableSortedSet;
        }
        ImmutableSortedSet<E> immutableSortedSetCreateDescendingSet = createDescendingSet();
        this.descendingSet = immutableSortedSetCreateDescendingSet;
        immutableSortedSetCreateDescendingSet.descendingSet = this;
        return immutableSortedSetCreateDescendingSet;
    }

    public E first() {
        return iterator().next();
    }

    public E floor(E e) {
        return (E) Iterators.getNext(headSet((Object) e, true).descendingIterator(), null);
    }

    @Override
    public ImmutableSortedSet<E> headSet(E e) {
        return headSet((Object) e, false);
    }

    @Override
    public ImmutableSortedSet<E> headSet(E e, boolean z) {
        return headSetImpl(Preconditions.checkNotNull(e), z);
    }

    abstract ImmutableSortedSet<E> headSetImpl(E e, boolean z);

    public E higher(E e) {
        return (E) Iterables.getFirst(tailSet((Object) e, false), null);
    }

    abstract int indexOf(Object obj);

    @Override
    public abstract UnmodifiableIterator<E> iterator();

    public E last() {
        return descendingIterator().next();
    }

    public E lower(E e) {
        return (E) Iterators.getNext(headSet((Object) e, false).descendingIterator(), null);
    }

    @Override
    @Deprecated
    public final E pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final E pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSortedSet<E> subSet(E e, E e2) {
        return subSet((Object) e, true, (Object) e2, false);
    }

    @Override
    public ImmutableSortedSet<E> subSet(E e, boolean z, E e2, boolean z2) {
        Preconditions.checkNotNull(e);
        Preconditions.checkNotNull(e2);
        Preconditions.checkArgument(this.comparator.compare(e, e2) <= 0);
        return subSetImpl(e, z, e2, z2);
    }

    abstract ImmutableSortedSet<E> subSetImpl(E e, boolean z, E e2, boolean z2);

    @Override
    public ImmutableSortedSet<E> tailSet(E e) {
        return tailSet((Object) e, true);
    }

    @Override
    public ImmutableSortedSet<E> tailSet(E e, boolean z) {
        return tailSetImpl(Preconditions.checkNotNull(e), z);
    }

    abstract ImmutableSortedSet<E> tailSetImpl(E e, boolean z);

    int unsafeCompare(Object obj, Object obj2) {
        return unsafeCompare(this.comparator, obj, obj2);
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this.comparator, toArray());
    }
}
