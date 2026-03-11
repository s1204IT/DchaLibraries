package com.google.common.collect;

import com.google.common.annotations.VisibleForTesting;
import java.lang.Comparable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import javax.annotation.Nullable;

@VisibleForTesting
final class TreeRangeSet$RangesByUpperBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
    private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
    private final Range<Cut<C>> upperBoundWindow;

    private TreeRangeSet$RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound, Range<Cut<C>> upperBoundWindow) {
        this.rangesByLowerBound = rangesByLowerBound;
        this.upperBoundWindow = upperBoundWindow;
    }

    private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
        if (window.isConnected(this.upperBoundWindow)) {
            return new TreeRangeSet$RangesByUpperBound(this.rangesByLowerBound, window.intersection(this.upperBoundWindow));
        }
        return ImmutableSortedMap.of();
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
        return subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
        return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
        return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
    }

    @Override
    public Comparator<? super Cut<C>> comparator() {
        return Ordering.natural();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return get(key) != null;
    }

    @Override
    public Range<C> get(@Nullable Object key) {
        Map.Entry<Cut<C>, Range<C>> candidate;
        if (key instanceof Cut) {
            try {
                Cut<C> cut = (Cut) key;
                if (this.upperBoundWindow.contains(cut) && (candidate = this.rangesByLowerBound.lowerEntry(cut)) != null && candidate.getValue().upperBound.equals(cut)) {
                    return candidate.getValue();
                }
            } catch (ClassCastException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    Iterator<Map.Entry<Cut<C>, Range<C>>> entryIterator() {
        Map.Entry<Cut<C>, Range<C>> entryLowerEntry;
        final Iterator<Range<C>> it;
        if (!this.upperBoundWindow.hasLowerBound() || (entryLowerEntry = this.rangesByLowerBound.lowerEntry((Cut) this.upperBoundWindow.lowerEndpoint())) == null) {
            it = this.rangesByLowerBound.values().iterator();
        } else if (this.upperBoundWindow.lowerBound.isLessThan(entryLowerEntry.getValue().upperBound)) {
            it = this.rangesByLowerBound.tailMap(entryLowerEntry.getKey(), true).values().iterator();
        } else {
            it = this.rangesByLowerBound.tailMap((Cut) this.upperBoundWindow.lowerEndpoint(), true).values().iterator();
        }
        return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
            @Override
            public Map.Entry<Cut<C>, Range<C>> computeNext() {
                if (!it.hasNext()) {
                    return (Map.Entry) endOfData();
                }
                Range<C> range = (Range) it.next();
                if (TreeRangeSet$RangesByUpperBound.this.upperBoundWindow.upperBound.isLessThan(range.upperBound)) {
                    return (Map.Entry) endOfData();
                }
                return Maps.immutableEntry(range.upperBound, range);
            }
        };
    }

    @Override
    Iterator<Map.Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
        Collection<Range<C>> collectionValues;
        if (this.upperBoundWindow.hasUpperBound()) {
            collectionValues = this.rangesByLowerBound.headMap((Cut) this.upperBoundWindow.upperEndpoint(), false).descendingMap().values();
        } else {
            collectionValues = this.rangesByLowerBound.descendingMap().values();
        }
        final PeekingIterator peekingIterator = Iterators.peekingIterator(collectionValues.iterator());
        if (peekingIterator.hasNext() && this.upperBoundWindow.upperBound.isLessThan(((Range) peekingIterator.peek()).upperBound)) {
            peekingIterator.next();
        }
        return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
            @Override
            public Map.Entry<Cut<C>, Range<C>> computeNext() {
                if (!peekingIterator.hasNext()) {
                    return (Map.Entry) endOfData();
                }
                Range<C> range = (Range) peekingIterator.next();
                if (TreeRangeSet$RangesByUpperBound.this.upperBoundWindow.lowerBound.isLessThan(range.upperBound)) {
                    return Maps.immutableEntry(range.upperBound, range);
                }
                return (Map.Entry) endOfData();
            }
        };
    }

    @Override
    public int size() {
        if (this.upperBoundWindow.equals(Range.all())) {
            return this.rangesByLowerBound.size();
        }
        return Iterators.size(entryIterator());
    }

    @Override
    public boolean isEmpty() {
        if (this.upperBoundWindow.equals(Range.all())) {
            return this.rangesByLowerBound.isEmpty();
        }
        return !entryIterator().hasNext();
    }
}
