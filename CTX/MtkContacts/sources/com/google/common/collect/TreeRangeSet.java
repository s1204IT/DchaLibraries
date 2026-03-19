package com.google.common.collect;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.lang.Comparable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

public class TreeRangeSet<C extends Comparable<?>> extends AbstractRangeSet<C> {
    private transient Set<Range<C>> asRanges;
    private transient RangeSet<C> complement;
    final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;

    @Override
    public void addAll(RangeSet rangeSet) {
        super.addAll(rangeSet);
    }

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

    @Override
    public boolean isEmpty() {
        return super.isEmpty();
    }

    @Override
    public void removeAll(RangeSet rangeSet) {
        super.removeAll(rangeSet);
    }

    public static <C extends Comparable<?>> TreeRangeSet<C> create() {
        return new TreeRangeSet<>(new TreeMap());
    }

    public static <C extends Comparable<?>> TreeRangeSet<C> create(RangeSet<C> rangeSet) {
        TreeRangeSet<C> treeRangeSetCreate = create();
        treeRangeSetCreate.addAll(rangeSet);
        return treeRangeSetCreate;
    }

    private TreeRangeSet(NavigableMap<Cut<C>, Range<C>> navigableMap) {
        this.rangesByLowerBound = navigableMap;
    }

    @Override
    public Set<Range<C>> asRanges() {
        Set<Range<C>> set = this.asRanges;
        if (set != null) {
            return set;
        }
        AsRanges asRanges = new AsRanges();
        this.asRanges = asRanges;
        return asRanges;
    }

    final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {
        AsRanges() {
        }

        @Override
        protected Collection<Range<C>> delegate() {
            return TreeRangeSet.this.rangesByLowerBound.values();
        }

        @Override
        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        @Override
        public boolean equals(Object obj) {
            return Sets.equalsImpl(this, obj);
        }
    }

    @Override
    public Range<C> rangeContaining(C c) {
        Preconditions.checkNotNull(c);
        Map.Entry<Cut<C>, Range<C>> entryFloorEntry = this.rangesByLowerBound.floorEntry(Cut.belowValue(c));
        if (entryFloorEntry != null && entryFloorEntry.getValue().contains(c)) {
            return entryFloorEntry.getValue();
        }
        return null;
    }

    @Override
    public boolean encloses(Range<C> range) {
        Preconditions.checkNotNull(range);
        Map.Entry<Cut<C>, Range<C>> entryFloorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
        return entryFloorEntry != null && entryFloorEntry.getValue().encloses(range);
    }

    private Range<C> rangeEnclosing(Range<C> range) {
        Preconditions.checkNotNull(range);
        Map.Entry<Cut<C>, Range<C>> entryFloorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
        if (entryFloorEntry != null && entryFloorEntry.getValue().encloses(range)) {
            return entryFloorEntry.getValue();
        }
        return null;
    }

    @Override
    public Range<C> span() {
        Map.Entry<Cut<C>, Range<C>> entryFirstEntry = this.rangesByLowerBound.firstEntry();
        Map.Entry<Cut<C>, Range<C>> entryLastEntry = this.rangesByLowerBound.lastEntry();
        if (entryFirstEntry == null) {
            throw new NoSuchElementException();
        }
        return Range.create(entryFirstEntry.getValue().lowerBound, entryLastEntry.getValue().upperBound);
    }

    @Override
    public void add(Range<C> range) {
        Preconditions.checkNotNull(range);
        if (range.isEmpty()) {
            return;
        }
        Cut<C> cut = range.lowerBound;
        Cut<C> cut2 = range.upperBound;
        Map.Entry<Cut<C>, Range<C>> entryLowerEntry = this.rangesByLowerBound.lowerEntry(cut);
        if (entryLowerEntry != null) {
            Range<C> value = entryLowerEntry.getValue();
            if (value.upperBound.compareTo((Cut) cut) >= 0) {
                if (value.upperBound.compareTo((Cut) cut2) >= 0) {
                    cut2 = value.upperBound;
                }
                cut = value.lowerBound;
            }
        }
        Map.Entry<Cut<C>, Range<C>> entryFloorEntry = this.rangesByLowerBound.floorEntry(cut2);
        if (entryFloorEntry != null) {
            Range<C> value2 = entryFloorEntry.getValue();
            if (value2.upperBound.compareTo((Cut) cut2) >= 0) {
                cut2 = value2.upperBound;
            }
        }
        this.rangesByLowerBound.subMap(cut, cut2).clear();
        replaceRangeWithSameLowerBound(Range.create(cut, cut2));
    }

    @Override
    public void remove(Range<C> range) {
        Preconditions.checkNotNull(range);
        if (range.isEmpty()) {
            return;
        }
        Map.Entry<Cut<C>, Range<C>> entryLowerEntry = this.rangesByLowerBound.lowerEntry(range.lowerBound);
        if (entryLowerEntry != null) {
            Range<C> value = entryLowerEntry.getValue();
            if (value.upperBound.compareTo((Cut) range.lowerBound) >= 0) {
                if (range.hasUpperBound() && value.upperBound.compareTo((Cut) range.upperBound) >= 0) {
                    replaceRangeWithSameLowerBound(Range.create(range.upperBound, value.upperBound));
                }
                replaceRangeWithSameLowerBound(Range.create(value.lowerBound, range.lowerBound));
            }
        }
        Map.Entry<Cut<C>, Range<C>> entryFloorEntry = this.rangesByLowerBound.floorEntry(range.upperBound);
        if (entryFloorEntry != null) {
            Range<C> value2 = entryFloorEntry.getValue();
            if (range.hasUpperBound() && value2.upperBound.compareTo((Cut) range.upperBound) >= 0) {
                replaceRangeWithSameLowerBound(Range.create(range.upperBound, value2.upperBound));
            }
        }
        this.rangesByLowerBound.subMap(range.lowerBound, range.upperBound).clear();
    }

    private void replaceRangeWithSameLowerBound(Range<C> range) {
        if (range.isEmpty()) {
            this.rangesByLowerBound.remove(range.lowerBound);
        } else {
            this.rangesByLowerBound.put(range.lowerBound, range);
        }
    }

    @Override
    public RangeSet<C> complement() {
        RangeSet<C> rangeSet = this.complement;
        if (rangeSet != null) {
            return rangeSet;
        }
        Complement complement = new Complement();
        this.complement = complement;
        return complement;
    }

    static final class RangesByUpperBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
        private final Range<Cut<C>> upperBoundWindow;

        RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> navigableMap) {
            this.rangesByLowerBound = navigableMap;
            this.upperBoundWindow = Range.all();
        }

        private RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> navigableMap, Range<Cut<C>> range) {
            this.rangesByLowerBound = navigableMap;
            this.upperBoundWindow = range;
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> range) {
            if (range.isConnected(this.upperBoundWindow)) {
                return new RangesByUpperBound(this.rangesByLowerBound, range.intersection(this.upperBoundWindow));
            }
            return ImmutableSortedMap.of();
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> cut, boolean z, Cut<C> cut2, boolean z2) {
            return subMap(Range.range(cut, BoundType.forBoolean(z), cut2, BoundType.forBoolean(z2)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> cut, boolean z) {
            return subMap(Range.upTo(cut, BoundType.forBoolean(z)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> cut, boolean z) {
            return subMap(Range.downTo(cut, BoundType.forBoolean(z)));
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
        public Range<C> get(Object obj) {
            Map.Entry<Cut<C>, Range<C>> entryLowerEntry;
            if (obj instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) obj;
                    if (this.upperBoundWindow.contains(cut) && (entryLowerEntry = this.rangesByLowerBound.lowerEntry(cut)) != null && entryLowerEntry.getValue().upperBound.equals(cut)) {
                        return entryLowerEntry.getValue();
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> entryIterator() {
            Map.Entry entryLowerEntry;
            final Iterator<Range<C>> it;
            if (!this.upperBoundWindow.hasLowerBound() || (entryLowerEntry = this.rangesByLowerBound.lowerEntry((Cut<C>) this.upperBoundWindow.lowerEndpoint())) == null) {
                it = this.rangesByLowerBound.values().iterator();
            } else if (this.upperBoundWindow.lowerBound.isLessThan(((Range) entryLowerEntry.getValue()).upperBound)) {
                it = this.rangesByLowerBound.tailMap(entryLowerEntry.getKey(), true).values().iterator();
            } else {
                it = this.rangesByLowerBound.tailMap(this.upperBoundWindow.lowerEndpoint(), true).values().iterator();
            }
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!it.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) it.next();
                    if (RangesByUpperBound.this.upperBoundWindow.upperBound.isLessThan(range.upperBound)) {
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
                collectionValues = this.rangesByLowerBound.headMap(this.upperBoundWindow.upperEndpoint(), false).descendingMap().values();
            } else {
                collectionValues = this.rangesByLowerBound.descendingMap().values();
            }
            final PeekingIterator peekingIterator = Iterators.peekingIterator(collectionValues.iterator());
            if (peekingIterator.hasNext() && this.upperBoundWindow.upperBound.isLessThan(((Range) peekingIterator.peek()).upperBound)) {
                peekingIterator.next();
            }
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!peekingIterator.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) peekingIterator.next();
                    if (RangesByUpperBound.this.upperBoundWindow.lowerBound.isLessThan(range.upperBound)) {
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

    private static final class ComplementRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final Range<Cut<C>> complementLowerBoundWindow;
        private final NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound;
        private final NavigableMap<Cut<C>, Range<C>> positiveRangesByUpperBound;

        ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> navigableMap) {
            this(navigableMap, Range.all());
        }

        private ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> navigableMap, Range<Cut<C>> range) {
            this.positiveRangesByLowerBound = navigableMap;
            this.positiveRangesByUpperBound = new RangesByUpperBound(navigableMap);
            this.complementLowerBoundWindow = range;
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> range) {
            if (!this.complementLowerBoundWindow.isConnected(range)) {
                return ImmutableSortedMap.of();
            }
            return new ComplementRangesByLowerBound(this.positiveRangesByLowerBound, range.intersection(this.complementLowerBoundWindow));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> cut, boolean z, Cut<C> cut2, boolean z2) {
            return subMap(Range.range(cut, BoundType.forBoolean(z), cut2, BoundType.forBoolean(z2)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> cut, boolean z) {
            return subMap(Range.upTo(cut, BoundType.forBoolean(z)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> cut, boolean z) {
            return subMap(Range.downTo(cut, BoundType.forBoolean(z)));
        }

        @Override
        public Comparator<? super Cut<C>> comparator() {
            return Ordering.natural();
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> entryIterator() {
            Collection<Range<C>> collectionValues;
            final Cut cutBelowAll;
            if (this.complementLowerBoundWindow.hasLowerBound()) {
                collectionValues = this.positiveRangesByUpperBound.tailMap((Cut<C>) this.complementLowerBoundWindow.lowerEndpoint(), this.complementLowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values();
            } else {
                collectionValues = this.positiveRangesByUpperBound.values();
            }
            final PeekingIterator peekingIterator = Iterators.peekingIterator(collectionValues.iterator());
            if (this.complementLowerBoundWindow.contains(Cut.belowAll()) && (!peekingIterator.hasNext() || ((Range) peekingIterator.peek()).lowerBound != Cut.belowAll())) {
                cutBelowAll = Cut.belowAll();
            } else if (peekingIterator.hasNext()) {
                cutBelowAll = ((Range) peekingIterator.next()).upperBound;
            } else {
                return Iterators.emptyIterator();
            }
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                Cut<C> nextComplementRangeLowerBound;

                {
                    this.nextComplementRangeLowerBound = cutBelowAll;
                }

                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    Range rangeCreate;
                    if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.upperBound.isLessThan(this.nextComplementRangeLowerBound) || this.nextComplementRangeLowerBound == Cut.aboveAll()) {
                        return (Map.Entry) endOfData();
                    }
                    if (peekingIterator.hasNext()) {
                        Range range = (Range) peekingIterator.next();
                        rangeCreate = Range.create(this.nextComplementRangeLowerBound, range.lowerBound);
                        this.nextComplementRangeLowerBound = range.upperBound;
                    } else {
                        rangeCreate = Range.create(this.nextComplementRangeLowerBound, Cut.aboveAll());
                        this.nextComplementRangeLowerBound = Cut.aboveAll();
                    }
                    return Maps.immutableEntry(rangeCreate.lowerBound, rangeCreate);
                }
            };
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            Cut<C> cutAboveAll;
            Cut<C> cutHigherKey;
            if (this.complementLowerBoundWindow.hasUpperBound()) {
                cutAboveAll = (Cut) this.complementLowerBoundWindow.upperEndpoint();
            } else {
                cutAboveAll = Cut.aboveAll();
            }
            final PeekingIterator peekingIterator = Iterators.peekingIterator(this.positiveRangesByUpperBound.headMap(cutAboveAll, this.complementLowerBoundWindow.hasUpperBound() && this.complementLowerBoundWindow.upperBoundType() == BoundType.CLOSED).descendingMap().values().iterator());
            if (peekingIterator.hasNext()) {
                if (((Range) peekingIterator.peek()).upperBound == Cut.aboveAll()) {
                    cutHigherKey = ((Range) peekingIterator.next()).lowerBound;
                } else {
                    cutHigherKey = this.positiveRangesByLowerBound.higherKey(((Range) peekingIterator.peek()).upperBound);
                }
            } else {
                if (!this.complementLowerBoundWindow.contains(Cut.belowAll()) || this.positiveRangesByLowerBound.containsKey(Cut.belowAll())) {
                    return Iterators.emptyIterator();
                }
                cutHigherKey = this.positiveRangesByLowerBound.higherKey(Cut.belowAll());
            }
            final Cut cut = (Cut) MoreObjects.firstNonNull(cutHigherKey, Cut.aboveAll());
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                Cut<C> nextComplementRangeUpperBound;

                {
                    this.nextComplementRangeUpperBound = cut;
                }

                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (this.nextComplementRangeUpperBound == Cut.belowAll()) {
                        return (Map.Entry) endOfData();
                    }
                    if (!peekingIterator.hasNext()) {
                        if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(Cut.belowAll())) {
                            Range rangeCreate = Range.create(Cut.belowAll(), this.nextComplementRangeUpperBound);
                            this.nextComplementRangeUpperBound = Cut.belowAll();
                            return Maps.immutableEntry(Cut.belowAll(), rangeCreate);
                        }
                    } else {
                        Range range = (Range) peekingIterator.next();
                        Range rangeCreate2 = Range.create(range.upperBound, this.nextComplementRangeUpperBound);
                        this.nextComplementRangeUpperBound = range.lowerBound;
                        if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(rangeCreate2.lowerBound)) {
                            return Maps.immutableEntry(rangeCreate2.lowerBound, rangeCreate2);
                        }
                    }
                    return (Map.Entry) endOfData();
                }
            };
        }

        @Override
        public int size() {
            return Iterators.size(entryIterator());
        }

        @Override
        public Range<C> get(Object obj) {
            if (obj instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) obj;
                    Map.Entry<Cut<C>, Range<C>> entryFirstEntry = tailMap((Cut) cut, true).firstEntry();
                    if (entryFirstEntry != null && entryFirstEntry.getKey().equals(cut)) {
                        return entryFirstEntry.getValue();
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        public boolean containsKey(Object obj) {
            return get(obj) != null;
        }
    }

    private final class Complement extends TreeRangeSet<C> {
        Complement() {
            super(new ComplementRangesByLowerBound(TreeRangeSet.this.rangesByLowerBound));
        }

        @Override
        public void add(Range<C> range) {
            TreeRangeSet.this.remove(range);
        }

        @Override
        public void remove(Range<C> range) {
            TreeRangeSet.this.add(range);
        }

        @Override
        public boolean contains(C c) {
            return !TreeRangeSet.this.contains(c);
        }

        @Override
        public RangeSet<C> complement() {
            return TreeRangeSet.this;
        }
    }

    private static final class SubRangeSetRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final Range<Cut<C>> lowerBoundWindow;
        private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
        private final NavigableMap<Cut<C>, Range<C>> rangesByUpperBound;
        private final Range<C> restriction;

        private SubRangeSetRangesByLowerBound(Range<Cut<C>> range, Range<C> range2, NavigableMap<Cut<C>, Range<C>> navigableMap) {
            this.lowerBoundWindow = (Range) Preconditions.checkNotNull(range);
            this.restriction = (Range) Preconditions.checkNotNull(range2);
            this.rangesByLowerBound = (NavigableMap) Preconditions.checkNotNull(navigableMap);
            this.rangesByUpperBound = new RangesByUpperBound(navigableMap);
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> range) {
            if (!range.isConnected(this.lowerBoundWindow)) {
                return ImmutableSortedMap.of();
            }
            return new SubRangeSetRangesByLowerBound(this.lowerBoundWindow.intersection(range), this.restriction, this.rangesByLowerBound);
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> cut, boolean z, Cut<C> cut2, boolean z2) {
            return subMap(Range.range(cut, BoundType.forBoolean(z), cut2, BoundType.forBoolean(z2)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> cut, boolean z) {
            return subMap(Range.upTo(cut, BoundType.forBoolean(z)));
        }

        @Override
        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> cut, boolean z) {
            return subMap(Range.downTo(cut, BoundType.forBoolean(z)));
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
        public Range<C> get(Object obj) {
            if (obj instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) obj;
                    if (this.lowerBoundWindow.contains(cut) && cut.compareTo((Cut) this.restriction.lowerBound) >= 0 && cut.compareTo((Cut) this.restriction.upperBound) < 0) {
                        if (cut.equals(this.restriction.lowerBound)) {
                            Range range = (Range) Maps.valueOrNull(this.rangesByLowerBound.floorEntry(cut));
                            if (range != null && range.upperBound.compareTo((Cut) this.restriction.lowerBound) > 0) {
                                return range.intersection(this.restriction);
                            }
                        } else {
                            Range range2 = (Range) this.rangesByLowerBound.get(cut);
                            if (range2 != null) {
                                return range2.intersection(this.restriction);
                            }
                        }
                    }
                    return null;
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> entryIterator() {
            final Iterator<Range<C>> it;
            if (this.restriction.isEmpty()) {
                return Iterators.emptyIterator();
            }
            if (this.lowerBoundWindow.upperBound.isLessThan(this.restriction.lowerBound)) {
                return Iterators.emptyIterator();
            }
            if (this.lowerBoundWindow.lowerBound.isLessThan(this.restriction.lowerBound)) {
                it = this.rangesByUpperBound.tailMap(this.restriction.lowerBound, false).values().iterator();
            } else {
                it = this.rangesByLowerBound.tailMap(this.lowerBoundWindow.lowerBound.endpoint(), this.lowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values().iterator();
            }
            final Cut cut = (Cut) Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!it.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) it.next();
                    if (!cut.isLessThan(range.lowerBound)) {
                        Range rangeIntersection = range.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                        return Maps.immutableEntry(rangeIntersection.lowerBound, rangeIntersection);
                    }
                    return (Map.Entry) endOfData();
                }
            };
        }

        @Override
        Iterator<Map.Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            if (this.restriction.isEmpty()) {
                return Iterators.emptyIterator();
            }
            Cut cut = (Cut) Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            final Iterator<Range<C>> it = this.rangesByLowerBound.headMap((Cut<C>) cut.endpoint(), cut.typeAsUpperBound() == BoundType.CLOSED).descendingMap().values().iterator();
            return new AbstractIterator<Map.Entry<Cut<C>, Range<C>>>() {
                @Override
                protected Map.Entry<Cut<C>, Range<C>> computeNext() {
                    if (!it.hasNext()) {
                        return (Map.Entry) endOfData();
                    }
                    Range range = (Range) it.next();
                    if (SubRangeSetRangesByLowerBound.this.restriction.lowerBound.compareTo((Cut) range.upperBound) < 0) {
                        Range rangeIntersection = range.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                        if (SubRangeSetRangesByLowerBound.this.lowerBoundWindow.contains(rangeIntersection.lowerBound)) {
                            return Maps.immutableEntry(rangeIntersection.lowerBound, rangeIntersection);
                        }
                        return (Map.Entry) endOfData();
                    }
                    return (Map.Entry) endOfData();
                }
            };
        }

        @Override
        public int size() {
            return Iterators.size(entryIterator());
        }
    }

    @Override
    public RangeSet<C> subRangeSet(Range<C> range) {
        return range.equals(Range.all()) ? this : new SubRangeSet(range);
    }

    private final class SubRangeSet extends TreeRangeSet<C> {
        private final Range<C> restriction;

        SubRangeSet(Range<C> range) {
            super(new SubRangeSetRangesByLowerBound(Range.all(), range, TreeRangeSet.this.rangesByLowerBound));
            this.restriction = range;
        }

        @Override
        public boolean encloses(Range<C> range) {
            Range rangeRangeEnclosing;
            return (this.restriction.isEmpty() || !this.restriction.encloses(range) || (rangeRangeEnclosing = TreeRangeSet.this.rangeEnclosing(range)) == null || rangeRangeEnclosing.intersection(this.restriction).isEmpty()) ? false : true;
        }

        @Override
        public Range<C> rangeContaining(C c) {
            Range<C> rangeRangeContaining;
            if (this.restriction.contains(c) && (rangeRangeContaining = TreeRangeSet.this.rangeContaining(c)) != null) {
                return rangeRangeContaining.intersection(this.restriction);
            }
            return null;
        }

        @Override
        public void add(Range<C> range) {
            Preconditions.checkArgument(this.restriction.encloses(range), "Cannot add range %s to subRangeSet(%s)", range, this.restriction);
            super.add(range);
        }

        @Override
        public void remove(Range<C> range) {
            if (range.isConnected(this.restriction)) {
                TreeRangeSet.this.remove(range.intersection(this.restriction));
            }
        }

        @Override
        public boolean contains(C c) {
            return this.restriction.contains(c) && TreeRangeSet.this.contains(c);
        }

        @Override
        public void clear() {
            TreeRangeSet.this.remove(this.restriction);
        }

        @Override
        public RangeSet<C> subRangeSet(Range<C> range) {
            if (range.encloses(this.restriction)) {
                return this;
            }
            if (range.isConnected(this.restriction)) {
                return new SubRangeSet(this.restriction.intersection(range));
            }
            return ImmutableRangeSet.of();
        }
    }
}
