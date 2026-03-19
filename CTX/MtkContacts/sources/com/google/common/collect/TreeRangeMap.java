package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import java.lang.Comparable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;

public final class TreeRangeMap<K extends Comparable, V> implements RangeMap<K, V> {
    private static final RangeMap EMPTY_SUB_RANGE_MAP = new RangeMap() {
        @Override
        public Object get(Comparable comparable) {
            return null;
        }

        @Override
        public Map.Entry<Range, Object> getEntry(Comparable comparable) {
            return null;
        }

        @Override
        public Range span() {
            throw new NoSuchElementException();
        }

        @Override
        public void put(Range range, Object obj) {
            Preconditions.checkNotNull(range);
            throw new IllegalArgumentException("Cannot insert range " + range + " into an empty subRangeMap");
        }

        @Override
        public void putAll(RangeMap rangeMap) {
            if (!rangeMap.asMapOfRanges().isEmpty()) {
                throw new IllegalArgumentException("Cannot putAll(nonEmptyRangeMap) into an empty subRangeMap");
            }
        }

        @Override
        public void clear() {
        }

        @Override
        public void remove(Range range) {
            Preconditions.checkNotNull(range);
        }

        @Override
        public Map<Range, Object> asMapOfRanges() {
            return Collections.emptyMap();
        }

        @Override
        public RangeMap subRangeMap(Range range) {
            Preconditions.checkNotNull(range);
            return this;
        }
    };
    private final NavigableMap<Cut<K>, RangeMapEntry<K, V>> entriesByLowerBound = Maps.newTreeMap();

    public static <K extends Comparable, V> TreeRangeMap<K, V> create() {
        return new TreeRangeMap<>();
    }

    private TreeRangeMap() {
    }

    private static final class RangeMapEntry<K extends Comparable, V> extends AbstractMapEntry<Range<K>, V> {
        private final Range<K> range;
        private final V value;

        RangeMapEntry(Cut<K> cut, Cut<K> cut2, V v) {
            this(Range.create(cut, cut2), v);
        }

        RangeMapEntry(Range<K> range, V v) {
            this.range = range;
            this.value = v;
        }

        @Override
        public Range<K> getKey() {
            return this.range;
        }

        @Override
        public V getValue() {
            return this.value;
        }

        public boolean contains(K k) {
            return this.range.contains(k);
        }

        Cut<K> getLowerBound() {
            return (Cut<K>) this.range.lowerBound;
        }

        Cut<K> getUpperBound() {
            return (Cut<K>) this.range.upperBound;
        }
    }

    @Override
    public V get(K k) {
        Map.Entry<Range<K>, V> entry = getEntry(k);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    @Override
    public Map.Entry<Range<K>, V> getEntry(K k) {
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryFloorEntry = this.entriesByLowerBound.floorEntry(Cut.belowValue(k));
        if (entryFloorEntry != null && entryFloorEntry.getValue().contains(k)) {
            return entryFloorEntry.getValue();
        }
        return null;
    }

    @Override
    public void put(Range<K> range, V v) {
        if (!range.isEmpty()) {
            Preconditions.checkNotNull(v);
            remove(range);
            this.entriesByLowerBound.put(range.lowerBound, new RangeMapEntry(range, v));
        }
    }

    @Override
    public void putAll(RangeMap<K, V> rangeMap) {
        for (Map.Entry<Range<K>, V> entry : rangeMap.asMapOfRanges().entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        this.entriesByLowerBound.clear();
    }

    @Override
    public Range<K> span() {
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryFirstEntry = this.entriesByLowerBound.firstEntry();
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLastEntry = this.entriesByLowerBound.lastEntry();
        if (entryFirstEntry == null) {
            throw new NoSuchElementException();
        }
        return Range.create(entryFirstEntry.getValue().getKey().lowerBound, entryLastEntry.getValue().getKey().upperBound);
    }

    private void putRangeMapEntry(Cut<K> cut, Cut<K> cut2, V v) {
        this.entriesByLowerBound.put(cut, new RangeMapEntry(cut, cut2, v));
    }

    @Override
    public void remove(Range<K> range) {
        if (range.isEmpty()) {
            return;
        }
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLowerEntry = this.entriesByLowerBound.lowerEntry((Cut<K>) range.lowerBound);
        if (entryLowerEntry != null) {
            RangeMapEntry<K, V> value = entryLowerEntry.getValue();
            if (value.getUpperBound().compareTo((Cut) range.lowerBound) > 0) {
                if (value.getUpperBound().compareTo((Cut) range.upperBound) > 0) {
                    putRangeMapEntry(range.upperBound, value.getUpperBound(), entryLowerEntry.getValue().getValue());
                }
                putRangeMapEntry(value.getLowerBound(), range.lowerBound, entryLowerEntry.getValue().getValue());
            }
        }
        Map.Entry<Cut<K>, RangeMapEntry<K, V>> entryLowerEntry2 = this.entriesByLowerBound.lowerEntry((Cut<K>) range.upperBound);
        if (entryLowerEntry2 != null) {
            RangeMapEntry<K, V> value2 = entryLowerEntry2.getValue();
            if (value2.getUpperBound().compareTo((Cut) range.upperBound) > 0) {
                putRangeMapEntry(range.upperBound, value2.getUpperBound(), entryLowerEntry2.getValue().getValue());
                this.entriesByLowerBound.remove(range.lowerBound);
            }
        }
        this.entriesByLowerBound.subMap((Cut<K>) range.lowerBound, (Cut<K>) range.upperBound).clear();
    }

    @Override
    public Map<Range<K>, V> asMapOfRanges() {
        return new AsMapOfRanges();
    }

    private final class AsMapOfRanges extends AbstractMap<Range<K>, V> {
        private AsMapOfRanges() {
        }

        @Override
        public boolean containsKey(Object obj) {
            return get(obj) != null;
        }

        @Override
        public V get(Object obj) {
            if (obj instanceof Range) {
                Range range = (Range) obj;
                RangeMapEntry rangeMapEntry = (RangeMapEntry) TreeRangeMap.this.entriesByLowerBound.get(range.lowerBound);
                if (rangeMapEntry != null && rangeMapEntry.getKey().equals(range)) {
                    return (V) rangeMapEntry.getValue();
                }
                return null;
            }
            return null;
        }

        @Override
        public Set<Map.Entry<Range<K>, V>> entrySet() {
            return new AbstractSet<Map.Entry<Range<K>, V>>() {
                @Override
                public Iterator<Map.Entry<Range<K>, V>> iterator() {
                    return TreeRangeMap.this.entriesByLowerBound.values().iterator();
                }

                @Override
                public int size() {
                    return TreeRangeMap.this.entriesByLowerBound.size();
                }
            };
        }
    }

    @Override
    public RangeMap<K, V> subRangeMap(Range<K> range) {
        if (range.equals(Range.all())) {
            return this;
        }
        return new SubRangeMap(range);
    }

    private RangeMap<K, V> emptySubRangeMap() {
        return EMPTY_SUB_RANGE_MAP;
    }

    private class SubRangeMap implements RangeMap<K, V> {
        private final Range<K> subRange;

        SubRangeMap(Range<K> range) {
            this.subRange = range;
        }

        @Override
        public V get(K k) {
            if (this.subRange.contains(k)) {
                return (V) TreeRangeMap.this.get(k);
            }
            return null;
        }

        @Override
        public Map.Entry<Range<K>, V> getEntry(K k) {
            Map.Entry<Range<K>, V> entry;
            if (this.subRange.contains(k) && (entry = TreeRangeMap.this.getEntry(k)) != null) {
                return Maps.immutableEntry(entry.getKey().intersection(this.subRange), entry.getValue());
            }
            return null;
        }

        @Override
        public Range<K> span() {
            Cut cut;
            Cut upperBound;
            Map.Entry entryFloorEntry = TreeRangeMap.this.entriesByLowerBound.floorEntry(this.subRange.lowerBound);
            if (entryFloorEntry == null || ((RangeMapEntry) entryFloorEntry.getValue()).getUpperBound().compareTo((Cut) this.subRange.lowerBound) <= 0) {
                cut = (Cut) TreeRangeMap.this.entriesByLowerBound.ceilingKey(this.subRange.lowerBound);
                if (cut == null || cut.compareTo((Cut) this.subRange.upperBound) >= 0) {
                    throw new NoSuchElementException();
                }
            } else {
                cut = this.subRange.lowerBound;
            }
            Map.Entry entryLowerEntry = TreeRangeMap.this.entriesByLowerBound.lowerEntry(this.subRange.upperBound);
            if (entryLowerEntry == null) {
                throw new NoSuchElementException();
            }
            if (((RangeMapEntry) entryLowerEntry.getValue()).getUpperBound().compareTo((Cut) this.subRange.upperBound) >= 0) {
                upperBound = this.subRange.upperBound;
            } else {
                upperBound = ((RangeMapEntry) entryLowerEntry.getValue()).getUpperBound();
            }
            return Range.create(cut, upperBound);
        }

        @Override
        public void put(Range<K> range, V v) {
            Preconditions.checkArgument(this.subRange.encloses(range), "Cannot put range %s into a subRangeMap(%s)", range, this.subRange);
            TreeRangeMap.this.put(range, v);
        }

        @Override
        public void putAll(RangeMap<K, V> rangeMap) {
            if (rangeMap.asMapOfRanges().isEmpty()) {
                return;
            }
            Range<K> rangeSpan = rangeMap.span();
            Preconditions.checkArgument(this.subRange.encloses(rangeSpan), "Cannot putAll rangeMap with span %s into a subRangeMap(%s)", rangeSpan, this.subRange);
            TreeRangeMap.this.putAll(rangeMap);
        }

        @Override
        public void clear() {
            TreeRangeMap.this.remove(this.subRange);
        }

        @Override
        public void remove(Range<K> range) {
            if (range.isConnected(this.subRange)) {
                TreeRangeMap.this.remove(range.intersection(this.subRange));
            }
        }

        @Override
        public RangeMap<K, V> subRangeMap(Range<K> range) {
            if (!range.isConnected(this.subRange)) {
                return TreeRangeMap.this.emptySubRangeMap();
            }
            return TreeRangeMap.this.subRangeMap(range.intersection(this.subRange));
        }

        @Override
        public Map<Range<K>, V> asMapOfRanges() {
            return new SubRangeMapAsMap();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RangeMap) {
                return asMapOfRanges().equals(((RangeMap) obj).asMapOfRanges());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return asMapOfRanges().hashCode();
        }

        @Override
        public String toString() {
            return asMapOfRanges().toString();
        }

        class SubRangeMapAsMap extends AbstractMap<Range<K>, V> {
            SubRangeMapAsMap() {
            }

            @Override
            public boolean containsKey(Object obj) {
                return get(obj) != null;
            }

            @Override
            public V get(Object obj) {
                RangeMapEntry rangeMapEntry;
                try {
                    if (obj instanceof Range) {
                        Range range = (Range) obj;
                        if (SubRangeMap.this.subRange.encloses(range) && !range.isEmpty()) {
                            if (range.lowerBound.compareTo((Cut) SubRangeMap.this.subRange.lowerBound) == 0) {
                                Map.Entry entryFloorEntry = TreeRangeMap.this.entriesByLowerBound.floorEntry(range.lowerBound);
                                if (entryFloorEntry != null) {
                                    rangeMapEntry = (RangeMapEntry) entryFloorEntry.getValue();
                                } else {
                                    rangeMapEntry = null;
                                }
                            } else {
                                rangeMapEntry = (RangeMapEntry) TreeRangeMap.this.entriesByLowerBound.get(range.lowerBound);
                            }
                            if (rangeMapEntry != null && rangeMapEntry.getKey().isConnected(SubRangeMap.this.subRange) && rangeMapEntry.getKey().intersection(SubRangeMap.this.subRange).equals(range)) {
                                return (V) rangeMapEntry.getValue();
                            }
                        }
                        return null;
                    }
                    return null;
                } catch (ClassCastException e) {
                    return null;
                }
            }

            @Override
            public V remove(Object obj) {
                V v = (V) get(obj);
                if (v != null) {
                    TreeRangeMap.this.remove((Range) obj);
                    return v;
                }
                return null;
            }

            @Override
            public void clear() {
                SubRangeMap.this.clear();
            }

            private boolean removeEntryIf(Predicate<? super Map.Entry<Range<K>, V>> predicate) {
                ArrayList arrayListNewArrayList = Lists.newArrayList();
                for (Map.Entry<Range<K>, V> entry : entrySet()) {
                    if (predicate.apply(entry)) {
                        arrayListNewArrayList.add(entry.getKey());
                    }
                }
                Iterator it = arrayListNewArrayList.iterator();
                while (it.hasNext()) {
                    TreeRangeMap.this.remove((Range) it.next());
                }
                return !arrayListNewArrayList.isEmpty();
            }

            @Override
            public Set<Range<K>> keySet() {
                return new Maps.KeySet<Range<K>, V>(this) {
                    @Override
                    public boolean remove(Object obj) {
                        return SubRangeMapAsMap.this.remove(obj) != null;
                    }

                    @Override
                    public boolean retainAll(Collection<?> collection) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(collection)), Maps.keyFunction()));
                    }
                };
            }

            @Override
            public Set<Map.Entry<Range<K>, V>> entrySet() {
                return new Maps.EntrySet<Range<K>, V>() {
                    @Override
                    Map<Range<K>, V> map() {
                        return SubRangeMapAsMap.this;
                    }

                    @Override
                    public Iterator<Map.Entry<Range<K>, V>> iterator() {
                        if (SubRangeMap.this.subRange.isEmpty()) {
                            return Iterators.emptyIterator();
                        }
                        final Iterator<V> it = TreeRangeMap.this.entriesByLowerBound.tailMap((Cut) MoreObjects.firstNonNull(TreeRangeMap.this.entriesByLowerBound.floorKey(SubRangeMap.this.subRange.lowerBound), SubRangeMap.this.subRange.lowerBound), true).values().iterator();
                        return new AbstractIterator<Map.Entry<Range<K>, V>>() {
                            @Override
                            protected Map.Entry<Range<K>, V> computeNext() {
                                while (it.hasNext()) {
                                    RangeMapEntry rangeMapEntry = (RangeMapEntry) it.next();
                                    if (rangeMapEntry.getLowerBound().compareTo((Cut) SubRangeMap.this.subRange.upperBound) >= 0) {
                                        break;
                                    }
                                    if (rangeMapEntry.getUpperBound().compareTo((Cut) SubRangeMap.this.subRange.lowerBound) > 0) {
                                        return Maps.immutableEntry(rangeMapEntry.getKey().intersection(SubRangeMap.this.subRange), rangeMapEntry.getValue());
                                    }
                                }
                                return (Map.Entry) endOfData();
                            }
                        };
                    }

                    @Override
                    public boolean retainAll(Collection<?> collection) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.not(Predicates.in(collection)));
                    }

                    @Override
                    public int size() {
                        return Iterators.size(iterator());
                    }

                    @Override
                    public boolean isEmpty() {
                        return !iterator().hasNext();
                    }
                };
            }

            @Override
            public Collection<V> values() {
                return new Maps.Values<Range<K>, V>(this) {
                    @Override
                    public boolean removeAll(Collection<?> collection) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.in(collection), Maps.valueFunction()));
                    }

                    @Override
                    public boolean retainAll(Collection<?> collection) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(collection)), Maps.valueFunction()));
                    }
                };
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RangeMap) {
            return asMapOfRanges().equals(((RangeMap) obj).asMapOfRanges());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return asMapOfRanges().hashCode();
    }

    @Override
    public String toString() {
        return this.entriesByLowerBound.values().toString();
    }
}
