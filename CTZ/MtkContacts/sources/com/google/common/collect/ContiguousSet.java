package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import java.lang.Comparable;
import java.util.NoSuchElementException;

public abstract class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {
    final DiscreteDomain<C> domain;

    @Override
    abstract ContiguousSet<C> headSetImpl(C c, boolean z);

    public abstract ContiguousSet<C> intersection(ContiguousSet<C> contiguousSet);

    public abstract Range<C> range();

    public abstract Range<C> range(BoundType boundType, BoundType boundType2);

    @Override
    abstract ContiguousSet<C> subSetImpl(C c, boolean z, C c2, boolean z2);

    @Override
    abstract ContiguousSet<C> tailSetImpl(C c, boolean z);

    public static <C extends Comparable> ContiguousSet<C> create(Range<C> range, DiscreteDomain<C> discreteDomain) {
        Range<C> rangeIntersection;
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(discreteDomain);
        try {
            if (!range.hasLowerBound()) {
                rangeIntersection = range.intersection(Range.atLeast(discreteDomain.minValue()));
            } else {
                rangeIntersection = range;
            }
            if (!range.hasUpperBound()) {
                rangeIntersection = rangeIntersection.intersection(Range.atMost(discreteDomain.maxValue()));
            }
            if (rangeIntersection.isEmpty() || Range.compareOrThrow(range.lowerBound.leastValueAbove(discreteDomain), range.upperBound.greatestValueBelow(discreteDomain)) > 0) {
                return new EmptyContiguousSet(discreteDomain);
            }
            return new RegularContiguousSet(rangeIntersection, discreteDomain);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException(e);
        }
    }

    ContiguousSet(DiscreteDomain<C> discreteDomain) {
        super(Ordering.natural());
        this.domain = discreteDomain;
    }

    @Override
    public ContiguousSet<C> headSet(C c) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(c), false);
    }

    @Override
    public ContiguousSet<C> headSet(C c, boolean z) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(c), z);
    }

    @Override
    public ContiguousSet<C> subSet(C c, C c2) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(c2);
        Preconditions.checkArgument(comparator().compare(c, c2) <= 0);
        return subSetImpl((Comparable) c, true, (Comparable) c2, false);
    }

    @Override
    public ContiguousSet<C> subSet(C c, boolean z, C c2, boolean z2) {
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(c2);
        Preconditions.checkArgument(comparator().compare(c, c2) <= 0);
        return subSetImpl((Comparable) c, z, (Comparable) c2, z2);
    }

    @Override
    public ContiguousSet<C> tailSet(C c) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(c), true);
    }

    @Override
    public ContiguousSet<C> tailSet(C c, boolean z) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(c), z);
    }

    @Override
    public String toString() {
        return range().toString();
    }

    @Deprecated
    public static <E> ImmutableSortedSet.Builder<E> builder() {
        throw new UnsupportedOperationException();
    }
}
