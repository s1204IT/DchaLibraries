package com.google.common.collect;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.SortedLists;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.lang.Comparable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class ImmutableRangeSet<C extends Comparable> extends AbstractRangeSet<C> implements Serializable {
    private transient ImmutableRangeSet<C> complement;
    private final transient ImmutableList<Range<C>> ranges;
    private static final ImmutableRangeSet<Comparable<?>> EMPTY = new ImmutableRangeSet<>(ImmutableList.of());
    private static final ImmutableRangeSet<Comparable<?>> ALL = new ImmutableRangeSet<>(ImmutableList.of(Range.all()));

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public boolean contains(Comparable comparable) {
        return super.contains(comparable);
    }

    @Override
    public boolean enclosesAll(RangeSet rangeSet) {
        return super.enclosesAll(rangeSet);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public static <C extends Comparable> ImmutableRangeSet<C> of() {
        return EMPTY;
    }

    static <C extends Comparable> ImmutableRangeSet<C> all() {
        return ALL;
    }

    public static <C extends Comparable> ImmutableRangeSet<C> of(Range<C> range) {
        Preconditions.checkNotNull(range);
        if (range.isEmpty()) {
            return of();
        }
        if (range.equals(Range.all())) {
            return all();
        }
        return new ImmutableRangeSet<>(ImmutableList.of(range));
    }

    public static <C extends Comparable> ImmutableRangeSet<C> copyOf(RangeSet<C> rangeSet) {
        Preconditions.checkNotNull(rangeSet);
        if (rangeSet.isEmpty()) {
            return of();
        }
        if (rangeSet.encloses(Range.all())) {
            return all();
        }
        if (rangeSet instanceof ImmutableRangeSet) {
            ImmutableRangeSet<C> immutableRangeSet = (ImmutableRangeSet) rangeSet;
            if (!immutableRangeSet.isPartialView()) {
                return immutableRangeSet;
            }
        }
        return new ImmutableRangeSet<>(ImmutableList.copyOf((Collection) rangeSet.asRanges()));
    }

    ImmutableRangeSet(ImmutableList<Range<C>> immutableList) {
        this.ranges = immutableList;
    }

    private ImmutableRangeSet(ImmutableList<Range<C>> immutableList, ImmutableRangeSet<C> immutableRangeSet) {
        this.ranges = immutableList;
        this.complement = immutableRangeSet;
    }

    @Override
    public boolean encloses(Range<C> range) {
        int iBinarySearch = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), range.lowerBound, Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
        return iBinarySearch != -1 && this.ranges.get(iBinarySearch).encloses(range);
    }

    @Override
    public Range<C> rangeContaining(C c) {
        int iBinarySearch = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), Cut.belowValue(c), Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
        if (iBinarySearch == -1) {
            return null;
        }
        Range<C> range = this.ranges.get(iBinarySearch);
        if (range.contains(c)) {
            return range;
        }
        return null;
    }

    @Override
    public Range<C> span() {
        if (this.ranges.isEmpty()) {
            throw new NoSuchElementException();
        }
        return Range.create(this.ranges.get(0).lowerBound, this.ranges.get(this.ranges.size() - 1).upperBound);
    }

    @Override
    public boolean isEmpty() {
        return this.ranges.isEmpty();
    }

    @Override
    public void add(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addAll(RangeSet<C> rangeSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAll(RangeSet<C> rangeSet) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableSet<Range<C>> asRanges() {
        if (this.ranges.isEmpty()) {
            return ImmutableSet.of();
        }
        return new RegularImmutableSortedSet(this.ranges, Range.RANGE_LEX_ORDERING);
    }

    private final class ComplementRanges extends ImmutableList<Range<C>> {
        private final boolean positiveBoundedAbove;
        private final boolean positiveBoundedBelow;
        private final int size;

        ComplementRanges() {
            this.positiveBoundedBelow = ((Range) ImmutableRangeSet.this.ranges.get(0)).hasLowerBound();
            this.positiveBoundedAbove = ((Range) Iterables.getLast(ImmutableRangeSet.this.ranges)).hasUpperBound();
            int size = ImmutableRangeSet.this.ranges.size() - 1;
            size = this.positiveBoundedBelow ? size + 1 : size;
            this.size = this.positiveBoundedAbove ? size + 1 : size;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public Range<C> get(int i) {
            Cut<C> cutBelowAll;
            Cut<C> cutAboveAll;
            Preconditions.checkElementIndex(i, this.size);
            if (!this.positiveBoundedBelow) {
                cutBelowAll = ((Range) ImmutableRangeSet.this.ranges.get(i)).upperBound;
            } else {
                cutBelowAll = i == 0 ? Cut.belowAll() : ((Range) ImmutableRangeSet.this.ranges.get(i - 1)).upperBound;
            }
            if (!this.positiveBoundedAbove || i != this.size - 1) {
                cutAboveAll = ((Range) ImmutableRangeSet.this.ranges.get(i + (!this.positiveBoundedBelow ? 1 : 0))).lowerBound;
            } else {
                cutAboveAll = Cut.aboveAll();
            }
            return Range.create(cutBelowAll, cutAboveAll);
        }

        @Override
        boolean isPartialView() {
            return true;
        }
    }

    @Override
    public ImmutableRangeSet<C> complement() {
        ImmutableRangeSet<C> immutableRangeSet = this.complement;
        if (immutableRangeSet != null) {
            return immutableRangeSet;
        }
        if (this.ranges.isEmpty()) {
            ImmutableRangeSet<C> immutableRangeSetAll = all();
            this.complement = immutableRangeSetAll;
            return immutableRangeSetAll;
        }
        if (this.ranges.size() == 1 && this.ranges.get(0).equals(Range.all())) {
            ImmutableRangeSet<C> immutableRangeSetOf = of();
            this.complement = immutableRangeSetOf;
            return immutableRangeSetOf;
        }
        ImmutableRangeSet<C> immutableRangeSet2 = new ImmutableRangeSet<>(new ComplementRanges(), this);
        this.complement = immutableRangeSet2;
        return immutableRangeSet2;
    }

    private ImmutableList<Range<C>> intersectRanges(final Range<C> range) {
        final int iBinarySearch;
        int size;
        if (this.ranges.isEmpty() || range.isEmpty()) {
            return ImmutableList.of();
        }
        if (range.encloses(span())) {
            return this.ranges;
        }
        if (range.hasLowerBound()) {
            iBinarySearch = SortedLists.binarySearch(this.ranges, (Function<? super E, Cut<C>>) Range.upperBoundFn(), range.lowerBound, SortedLists.KeyPresentBehavior.FIRST_AFTER, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
        } else {
            iBinarySearch = 0;
        }
        if (range.hasUpperBound()) {
            size = SortedLists.binarySearch(this.ranges, (Function<? super E, Cut<C>>) Range.lowerBoundFn(), range.upperBound, SortedLists.KeyPresentBehavior.FIRST_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
        } else {
            size = this.ranges.size();
        }
        final int i = size - iBinarySearch;
        if (i == 0) {
            return ImmutableList.of();
        }
        return (ImmutableList<Range<C>>) new ImmutableList<Range<C>>() {
            @Override
            public int size() {
                return i;
            }

            @Override
            public Range<C> get(int i2) {
                Preconditions.checkElementIndex(i2, i);
                return (i2 == 0 || i2 == i + (-1)) ? ((Range) ImmutableRangeSet.this.ranges.get(i2 + iBinarySearch)).intersection(range) : (Range) ImmutableRangeSet.this.ranges.get(i2 + iBinarySearch);
            }

            @Override
            boolean isPartialView() {
                return true;
            }
        };
    }

    @Override
    public ImmutableRangeSet<C> subRangeSet(Range<C> range) {
        if (!isEmpty()) {
            Range<C> rangeSpan = span();
            if (range.encloses(rangeSpan)) {
                return this;
            }
            if (range.isConnected(rangeSpan)) {
                return new ImmutableRangeSet<>(intersectRanges(range));
            }
        }
        return of();
    }

    public ImmutableSortedSet<C> asSet(DiscreteDomain<C> discreteDomain) {
        Preconditions.checkNotNull(discreteDomain);
        if (isEmpty()) {
            return ImmutableSortedSet.of();
        }
        Range<C> rangeCanonical = span().canonical(discreteDomain);
        if (!rangeCanonical.hasLowerBound()) {
            throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded below");
        }
        if (!rangeCanonical.hasUpperBound()) {
            try {
                discreteDomain.maxValue();
            } catch (NoSuchElementException e) {
                throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded above");
            }
        }
        return new AsSet(discreteDomain);
    }

    private final class AsSet extends ImmutableSortedSet<C> {
        private final DiscreteDomain<C> domain;
        private transient Integer size;

        AsSet(DiscreteDomain<C> discreteDomain) {
            super(Ordering.natural());
            this.domain = discreteDomain;
        }

        @Override
        public int size() {
            Integer numValueOf = this.size;
            if (numValueOf == null) {
                long size = 0;
                UnmodifiableIterator it = ImmutableRangeSet.this.ranges.iterator();
                while (it.hasNext()) {
                    size += (long) ContiguousSet.create((Range) it.next(), this.domain).size();
                    if (size >= 2147483647L) {
                        break;
                    }
                }
                numValueOf = Integer.valueOf(Ints.saturatedCast(size));
                this.size = numValueOf;
            }
            return numValueOf.intValue();
        }

        @Override
        public UnmodifiableIterator<C> iterator() {
            return new AbstractIterator<C>() {
                Iterator<C> elemItr = Iterators.emptyIterator();
                final Iterator<Range<C>> rangeItr;

                {
                    this.rangeItr = ImmutableRangeSet.this.ranges.iterator();
                }

                @Override
                protected C computeNext() {
                    while (!this.elemItr.hasNext()) {
                        if (this.rangeItr.hasNext()) {
                            this.elemItr = ContiguousSet.create(this.rangeItr.next(), AsSet.this.domain).iterator();
                        } else {
                            return (C) endOfData();
                        }
                    }
                    return this.elemItr.next();
                }
            };
        }

        @Override
        public UnmodifiableIterator<C> descendingIterator() {
            return new AbstractIterator<C>() {
                Iterator<C> elemItr = Iterators.emptyIterator();
                final Iterator<Range<C>> rangeItr;

                {
                    this.rangeItr = ImmutableRangeSet.this.ranges.reverse().iterator();
                }

                @Override
                protected C computeNext() {
                    while (!this.elemItr.hasNext()) {
                        if (this.rangeItr.hasNext()) {
                            this.elemItr = ContiguousSet.create(this.rangeItr.next(), AsSet.this.domain).descendingIterator();
                        } else {
                            return (C) endOfData();
                        }
                    }
                    return this.elemItr.next();
                }
            };
        }

        ImmutableSortedSet<C> subSet(Range<C> range) {
            return ImmutableRangeSet.this.subRangeSet((Range) range).asSet(this.domain);
        }

        @Override
        ImmutableSortedSet<C> headSetImpl(C c, boolean z) {
            return subSet(Range.upTo(c, BoundType.forBoolean(z)));
        }

        @Override
        ImmutableSortedSet<C> subSetImpl(C c, boolean z, C c2, boolean z2) {
            if (!z && !z2 && Range.compareOrThrow(c, c2) == 0) {
                return ImmutableSortedSet.of();
            }
            return subSet(Range.range(c, BoundType.forBoolean(z), c2, BoundType.forBoolean(z2)));
        }

        @Override
        ImmutableSortedSet<C> tailSetImpl(C c, boolean z) {
            return subSet(Range.downTo(c, BoundType.forBoolean(z)));
        }

        @Override
        public boolean contains(Object obj) {
            if (obj == null) {
                return false;
            }
            try {
                return ImmutableRangeSet.this.contains((Comparable) obj);
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        int indexOf(Object obj) {
            if (contains(obj)) {
                Comparable comparable = (Comparable) obj;
                long size = 0;
                UnmodifiableIterator it = ImmutableRangeSet.this.ranges.iterator();
                while (it.hasNext()) {
                    Range range = (Range) it.next();
                    if (range.contains(comparable)) {
                        return Ints.saturatedCast(size + ((long) ContiguousSet.create(range, this.domain).indexOf(comparable)));
                    }
                    size += (long) ContiguousSet.create(range, this.domain).size();
                }
                throw new AssertionError("impossible");
            }
            return -1;
        }

        @Override
        boolean isPartialView() {
            return ImmutableRangeSet.this.ranges.isPartialView();
        }

        @Override
        public String toString() {
            return ImmutableRangeSet.this.ranges.toString();
        }

        @Override
        Object writeReplace() {
            return new AsSetSerializedForm(ImmutableRangeSet.this.ranges, this.domain);
        }
    }

    private static class AsSetSerializedForm<C extends Comparable> implements Serializable {
        private final DiscreteDomain<C> domain;
        private final ImmutableList<Range<C>> ranges;

        AsSetSerializedForm(ImmutableList<Range<C>> immutableList, DiscreteDomain<C> discreteDomain) {
            this.ranges = immutableList;
            this.domain = discreteDomain;
        }

        Object readResolve() {
            return new ImmutableRangeSet(this.ranges).asSet(this.domain);
        }
    }

    boolean isPartialView() {
        return this.ranges.isPartialView();
    }

    public static <C extends Comparable<?>> Builder<C> builder() {
        return new Builder<>();
    }

    public static class Builder<C extends Comparable<?>> {
        private final RangeSet<C> rangeSet = TreeRangeSet.create();

        public Builder<C> add(Range<C> range) {
            if (range.isEmpty()) {
                throw new IllegalArgumentException("range must not be empty, but was " + range);
            }
            if (!this.rangeSet.complement().encloses(range)) {
                for (Range<C> range2 : this.rangeSet.asRanges()) {
                    Preconditions.checkArgument(!range2.isConnected(range) || range2.intersection(range).isEmpty(), "Ranges may not overlap, but received %s and %s", range2, range);
                }
                throw new AssertionError("should have thrown an IAE above");
            }
            this.rangeSet.add(range);
            return this;
        }

        public Builder<C> addAll(RangeSet<C> rangeSet) {
            Iterator<Range<C>> it = rangeSet.asRanges().iterator();
            while (it.hasNext()) {
                add(it.next());
            }
            return this;
        }

        public ImmutableRangeSet<C> build() {
            return ImmutableRangeSet.copyOf(this.rangeSet);
        }
    }

    private static final class SerializedForm<C extends Comparable> implements Serializable {
        private final ImmutableList<Range<C>> ranges;

        SerializedForm(ImmutableList<Range<C>> immutableList) {
            this.ranges = immutableList;
        }

        Object readResolve() {
            if (this.ranges.isEmpty()) {
                return ImmutableRangeSet.of();
            }
            if (this.ranges.equals(ImmutableList.of(Range.all()))) {
                return ImmutableRangeSet.all();
            }
            return new ImmutableRangeSet(this.ranges);
        }
    }

    Object writeReplace() {
        return new SerializedForm(this.ranges);
    }
}
