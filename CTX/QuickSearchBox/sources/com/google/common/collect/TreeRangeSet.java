package com.google.common.collect;

import java.lang.Comparable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public class TreeRangeSet<C extends Comparable<?>> extends AbstractRangeSet<C> {
    private transient Set<Range<C>> asRanges;
    final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;

    final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {
        final TreeRangeSet this$0;

        AsRanges(TreeRangeSet treeRangeSet) {
            this.this$0 = treeRangeSet;
        }

        @Override
        public Collection<Range<C>> delegate() {
            return this.this$0.rangesByLowerBound.values();
        }

        @Override
        public boolean equals(Object obj) {
            return Sets.equalsImpl(this, obj);
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }
    }

    static final class RangesByUpperBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
        private final Range<Cut<C>> upperBoundWindow;

        private RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> navigableMap, Range<Cut<C>> range) {
            this.rangesByLowerBound = navigableMap;
            this.upperBoundWindow = range;
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> range) {
            return range.isConnected(this.upperBoundWindow) ? new RangesByUpperBound(this.rangesByLowerBound, range.intersection(this.upperBoundWindow)) : ImmutableSortedMap.of();
        }

        @Override
        public Comparator<? super Cut<C>> comparator() {
            return Ordering.natural();
        }

        @Override
        public boolean containsKey(Object obj) {
            return get(obj) != null;
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            PeekingIterator peekingIterator = Iterators.peekingIterator((this.upperBoundWindow.hasUpperBound() ? this.rangesByLowerBound.headMap(this.upperBoundWindow.upperEndpoint(), false).descendingMap().values() : this.rangesByLowerBound.descendingMap().values()).iterator());
            if (peekingIterator.hasNext() && this.upperBoundWindow.upperBound.isLessThan(((Range) peekingIterator.peek()).upperBound)) {
                peekingIterator.next();
            }
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>(this, peekingIterator) {
                final RangesByUpperBound this$0;
                final PeekingIterator val$backingItr;

                {
                    this.this$0 = this;
                    this.val$backingItr = peekingIterator;
                }

                @Override
                public Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!this.val$backingItr.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) this.val$backingItr.next();
                    return this.this$0.upperBoundWindow.lowerBound.isLessThan(range.upperBound) ? Maps.immutableEntry(range.upperBound, range) : (Map.Entry) endOfData();
                }
            };
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> entryIterator() {
            Map.Entry entryLowerEntry;
            Iterator<Range<C>> it = (this.upperBoundWindow.hasLowerBound() && (entryLowerEntry = this.rangesByLowerBound.lowerEntry((Cut<C>) this.upperBoundWindow.lowerEndpoint())) != null) ? this.upperBoundWindow.lowerBound.isLessThan(((Range) entryLowerEntry.getValue()).upperBound) ? this.rangesByLowerBound.tailMap(entryLowerEntry.getKey(), true).values().iterator() : this.rangesByLowerBound.tailMap(this.upperBoundWindow.lowerEndpoint(), true).values().iterator() : this.rangesByLowerBound.values().iterator();
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>(this, it) {
                final RangesByUpperBound this$0;
                final Iterator val$backingItr;

                {
                    this.this$0 = this;
                    this.val$backingItr = it;
                }

                @Override
                public Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!this.val$backingItr.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) this.val$backingItr.next();
                    return this.this$0.upperBoundWindow.upperBound.isLessThan(range.upperBound) ? (Map.Entry) endOfData() : Maps.immutableEntry(range.upperBound, range);
                }
            };
        }

        @Override
        public Range<C> get(Object obj) {
            if (obj instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) obj;
                    if (!this.upperBoundWindow.contains(cut)) {
                        return null;
                    }
                    Map.Entry<Cut<C>, Range<C>> entryLowerEntry = this.rangesByLowerBound.lowerEntry(cut);
                    if (entryLowerEntry != null && entryLowerEntry.getValue().upperBound.equals(cut)) {
                        return entryLowerEntry.getValue();
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> cut, boolean z) {
            return subMap(Range.upTo(cut, BoundType.forBoolean(z)));
        }

        @Override
        public boolean isEmpty() {
            return this.upperBoundWindow.equals(Range.all()) ? this.rangesByLowerBound.isEmpty() : !entryIterator().hasNext();
        }

        @Override
        public int size() {
            return this.upperBoundWindow.equals(Range.all()) ? this.rangesByLowerBound.size() : Iterators.size(entryIterator());
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> cut, boolean z, Cut<C> cut2, boolean z2) {
            return subMap(Range.range(cut, BoundType.forBoolean(z), cut2, BoundType.forBoolean(z2)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> cut, boolean z) {
            return subMap(Range.downTo(cut, BoundType.forBoolean(z)));
        }
    }

    @Override
    public Set<Range<C>> asRanges() {
        Set<Range<C>> set = this.asRanges;
        if (set != null) {
            return set;
        }
        AsRanges asRanges = new AsRanges(this);
        this.asRanges = asRanges;
        return asRanges;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
