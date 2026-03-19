package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class Sets {
    private Sets() {
    }

    static abstract class ImprovedAbstractSet<E> extends AbstractSet<E> {
        ImprovedAbstractSet() {
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            return Sets.removeAllImpl(this, collection);
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            return super.retainAll((Collection) Preconditions.checkNotNull(collection));
        }
    }

    public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(E e, E... eArr) {
        return ImmutableEnumSet.asImmutable(EnumSet.of((Enum) e, (Enum[]) eArr));
    }

    public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(Iterable<E> iterable) {
        if (iterable instanceof ImmutableEnumSet) {
            return (ImmutableEnumSet) iterable;
        }
        if (iterable instanceof Collection) {
            Collection collection = (Collection) iterable;
            if (collection.isEmpty()) {
                return ImmutableSet.of();
            }
            return ImmutableEnumSet.asImmutable(EnumSet.copyOf(collection));
        }
        Iterator<E> it = iterable.iterator();
        if (it.hasNext()) {
            EnumSet enumSetOf = EnumSet.of((Enum) it.next());
            Iterators.addAll(enumSetOf, it);
            return ImmutableEnumSet.asImmutable(enumSetOf);
        }
        return ImmutableSet.of();
    }

    public static <E extends Enum<E>> EnumSet<E> newEnumSet(Iterable<E> iterable, Class<E> cls) {
        EnumSet<E> enumSetNoneOf = EnumSet.noneOf(cls);
        Iterables.addAll(enumSetNoneOf, iterable);
        return enumSetNoneOf;
    }

    public static <E> HashSet<E> newHashSet() {
        return new HashSet<>();
    }

    public static <E> HashSet<E> newHashSet(E... eArr) {
        HashSet<E> hashSetNewHashSetWithExpectedSize = newHashSetWithExpectedSize(eArr.length);
        Collections.addAll(hashSetNewHashSetWithExpectedSize, eArr);
        return hashSetNewHashSetWithExpectedSize;
    }

    public static <E> HashSet<E> newHashSetWithExpectedSize(int i) {
        return new HashSet<>(Maps.capacity(i));
    }

    public static <E> HashSet<E> newHashSet(Iterable<? extends E> iterable) {
        if (iterable instanceof Collection) {
            return new HashSet<>(Collections2.cast(iterable));
        }
        return newHashSet(iterable.iterator());
    }

    public static <E> HashSet<E> newHashSet(Iterator<? extends E> it) {
        HashSet<E> hashSetNewHashSet = newHashSet();
        Iterators.addAll(hashSetNewHashSet, it);
        return hashSetNewHashSet;
    }

    public static <E> Set<E> newConcurrentHashSet() {
        return newSetFromMap(new ConcurrentHashMap());
    }

    public static <E> Set<E> newConcurrentHashSet(Iterable<? extends E> iterable) {
        Set<E> setNewConcurrentHashSet = newConcurrentHashSet();
        Iterables.addAll(setNewConcurrentHashSet, iterable);
        return setNewConcurrentHashSet;
    }

    public static <E> LinkedHashSet<E> newLinkedHashSet() {
        return new LinkedHashSet<>();
    }

    public static <E> LinkedHashSet<E> newLinkedHashSetWithExpectedSize(int i) {
        return new LinkedHashSet<>(Maps.capacity(i));
    }

    public static <E> LinkedHashSet<E> newLinkedHashSet(Iterable<? extends E> iterable) {
        if (iterable instanceof Collection) {
            return new LinkedHashSet<>(Collections2.cast(iterable));
        }
        LinkedHashSet<E> linkedHashSetNewLinkedHashSet = newLinkedHashSet();
        Iterables.addAll(linkedHashSetNewLinkedHashSet, iterable);
        return linkedHashSetNewLinkedHashSet;
    }

    public static <E extends Comparable> TreeSet<E> newTreeSet() {
        return new TreeSet<>();
    }

    public static <E extends Comparable> TreeSet<E> newTreeSet(Iterable<? extends E> iterable) {
        TreeSet<E> treeSetNewTreeSet = newTreeSet();
        Iterables.addAll(treeSetNewTreeSet, iterable);
        return treeSetNewTreeSet;
    }

    public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
        return new TreeSet<>((Comparator) Preconditions.checkNotNull(comparator));
    }

    public static <E> Set<E> newIdentityHashSet() {
        return newSetFromMap(Maps.newIdentityHashMap());
    }

    public static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet() {
        return new CopyOnWriteArraySet<>();
    }

    public static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet(Iterable<? extends E> iterable) {
        Collection collectionNewArrayList;
        if (iterable instanceof Collection) {
            collectionNewArrayList = Collections2.cast(iterable);
        } else {
            collectionNewArrayList = Lists.newArrayList(iterable);
        }
        return new CopyOnWriteArraySet<>(collectionNewArrayList);
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection) {
        if (collection instanceof EnumSet) {
            return EnumSet.complementOf((EnumSet) collection);
        }
        Preconditions.checkArgument(!collection.isEmpty(), "collection is empty; use the other version of this method");
        return makeComplementByHand(collection, collection.iterator().next().getDeclaringClass());
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection, Class<E> cls) {
        Preconditions.checkNotNull(collection);
        if (collection instanceof EnumSet) {
            return EnumSet.complementOf((EnumSet) collection);
        }
        return makeComplementByHand(collection, cls);
    }

    private static <E extends Enum<E>> EnumSet<E> makeComplementByHand(Collection<E> collection, Class<E> cls) {
        EnumSet<E> enumSetAllOf = EnumSet.allOf(cls);
        enumSetAllOf.removeAll(collection);
        return enumSetAllOf;
    }

    public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
        return Platform.newSetFromMap(map);
    }

    public static abstract class SetView<E> extends AbstractSet<E> {
        private SetView() {
        }

        public ImmutableSet<E> immutableCopy() {
            return ImmutableSet.copyOf((Collection) this);
        }

        public <S extends Set<E>> S copyInto(S s) {
            s.addAll(this);
            return s;
        }
    }

    public static <E> SetView<E> union(final Set<? extends E> set, final Set<? extends E> set2) {
        Preconditions.checkNotNull(set, "set1");
        Preconditions.checkNotNull(set2, "set2");
        final SetView setViewDifference = difference(set2, set);
        return new SetView<E>() {
            {
                super();
            }

            @Override
            public int size() {
                return set.size() + setViewDifference.size();
            }

            @Override
            public boolean isEmpty() {
                return set.isEmpty() && set2.isEmpty();
            }

            @Override
            public Iterator<E> iterator() {
                return Iterators.unmodifiableIterator(Iterators.concat(set.iterator(), setViewDifference.iterator()));
            }

            @Override
            public boolean contains(Object obj) {
                return set.contains(obj) || set2.contains(obj);
            }

            @Override
            public <S extends Set<E>> S copyInto(S s) {
                s.addAll(set);
                s.addAll(set2);
                return s;
            }

            @Override
            public ImmutableSet<E> immutableCopy() {
                return new ImmutableSet.Builder().addAll((Iterable) set).addAll((Iterable) set2).build();
            }
        };
    }

    public static <E> SetView<E> intersection(final Set<E> set, final Set<?> set2) {
        Preconditions.checkNotNull(set, "set1");
        Preconditions.checkNotNull(set2, "set2");
        final Predicate predicateIn = Predicates.in(set2);
        return new SetView<E>() {
            {
                super();
            }

            @Override
            public Iterator<E> iterator() {
                return Iterators.filter(set.iterator(), predicateIn);
            }

            @Override
            public int size() {
                return Iterators.size(iterator());
            }

            @Override
            public boolean isEmpty() {
                return !iterator().hasNext();
            }

            @Override
            public boolean contains(Object obj) {
                return set.contains(obj) && set2.contains(obj);
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                return set.containsAll(collection) && set2.containsAll(collection);
            }
        };
    }

    public static <E> SetView<E> difference(final Set<E> set, final Set<?> set2) {
        Preconditions.checkNotNull(set, "set1");
        Preconditions.checkNotNull(set2, "set2");
        final Predicate predicateNot = Predicates.not(Predicates.in(set2));
        return new SetView<E>() {
            {
                super();
            }

            @Override
            public Iterator<E> iterator() {
                return Iterators.filter(set.iterator(), predicateNot);
            }

            @Override
            public int size() {
                return Iterators.size(iterator());
            }

            @Override
            public boolean isEmpty() {
                return set2.containsAll(set);
            }

            @Override
            public boolean contains(Object obj) {
                return set.contains(obj) && !set2.contains(obj);
            }
        };
    }

    public static <E> SetView<E> symmetricDifference(Set<? extends E> set, Set<? extends E> set2) {
        Preconditions.checkNotNull(set, "set1");
        Preconditions.checkNotNull(set2, "set2");
        return difference(union(set, set2), intersection(set, set2));
    }

    public static <E> Set<E> filter(Set<E> set, Predicate<? super E> predicate) {
        if (set instanceof SortedSet) {
            return filter((SortedSet) set, (Predicate) predicate);
        }
        if (set instanceof FilteredSet) {
            FilteredSet filteredSet = (FilteredSet) set;
            return new FilteredSet((Set) filteredSet.unfiltered, Predicates.and(filteredSet.predicate, predicate));
        }
        return new FilteredSet((Set) Preconditions.checkNotNull(set), (Predicate) Preconditions.checkNotNull(predicate));
    }

    private static class FilteredSet<E> extends Collections2.FilteredCollection<E> implements Set<E> {
        FilteredSet(Set<E> set, Predicate<? super E> predicate) {
            super(set, predicate);
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

    public static <E> SortedSet<E> filter(SortedSet<E> sortedSet, Predicate<? super E> predicate) {
        return Platform.setsFilterSortedSet(sortedSet, predicate);
    }

    static <E> SortedSet<E> filterSortedIgnoreNavigable(SortedSet<E> sortedSet, Predicate<? super E> predicate) {
        if (sortedSet instanceof FilteredSet) {
            FilteredSet filteredSet = (FilteredSet) sortedSet;
            return new FilteredSortedSet((SortedSet) filteredSet.unfiltered, Predicates.and(filteredSet.predicate, predicate));
        }
        return new FilteredSortedSet((SortedSet) Preconditions.checkNotNull(sortedSet), (Predicate) Preconditions.checkNotNull(predicate));
    }

    private static class FilteredSortedSet<E> extends FilteredSet<E> implements SortedSet<E> {
        FilteredSortedSet(SortedSet<E> sortedSet, Predicate<? super E> predicate) {
            super(sortedSet, predicate);
        }

        @Override
        public Comparator<? super E> comparator() {
            return ((SortedSet) this.unfiltered).comparator();
        }

        @Override
        public SortedSet<E> subSet(E e, E e2) {
            return new FilteredSortedSet(((SortedSet) this.unfiltered).subSet(e, e2), this.predicate);
        }

        @Override
        public SortedSet<E> headSet(E e) {
            return new FilteredSortedSet(((SortedSet) this.unfiltered).headSet(e), this.predicate);
        }

        @Override
        public SortedSet<E> tailSet(E e) {
            return new FilteredSortedSet(((SortedSet) this.unfiltered).tailSet(e), this.predicate);
        }

        @Override
        public E first() {
            return iterator().next();
        }

        public E last() {
            SortedSet sortedSetHeadSet = (SortedSet) this.unfiltered;
            while (true) {
                E e = (Object) sortedSetHeadSet.last();
                if (this.predicate.apply(e)) {
                    return e;
                }
                sortedSetHeadSet = sortedSetHeadSet.headSet(e);
            }
        }
    }

    public static <E> NavigableSet<E> filter(NavigableSet<E> navigableSet, Predicate<? super E> predicate) {
        if (navigableSet instanceof FilteredSet) {
            FilteredSet filteredSet = (FilteredSet) navigableSet;
            return new FilteredNavigableSet((NavigableSet) filteredSet.unfiltered, Predicates.and(filteredSet.predicate, predicate));
        }
        return new FilteredNavigableSet((NavigableSet) Preconditions.checkNotNull(navigableSet), (Predicate) Preconditions.checkNotNull(predicate));
    }

    private static class FilteredNavigableSet<E> extends FilteredSortedSet<E> implements NavigableSet<E> {
        FilteredNavigableSet(NavigableSet<E> navigableSet, Predicate<? super E> predicate) {
            super(navigableSet, predicate);
        }

        NavigableSet<E> unfiltered() {
            return (NavigableSet) this.unfiltered;
        }

        @Override
        public E lower(E e) {
            return (E) Iterators.getNext(headSet(e, false).descendingIterator(), null);
        }

        @Override
        public E floor(E e) {
            return (E) Iterators.getNext(headSet(e, true).descendingIterator(), null);
        }

        @Override
        public E ceiling(E e) {
            return (E) Iterables.getFirst(tailSet(e, true), null);
        }

        @Override
        public E higher(E e) {
            return (E) Iterables.getFirst(tailSet(e, false), null);
        }

        @Override
        public E pollFirst() {
            return (E) Iterables.removeFirstMatching(unfiltered(), this.predicate);
        }

        @Override
        public E pollLast() {
            return (E) Iterables.removeFirstMatching(unfiltered().descendingSet(), this.predicate);
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return Sets.filter((NavigableSet) unfiltered().descendingSet(), (Predicate) this.predicate);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.filter(unfiltered().descendingIterator(), this.predicate);
        }

        @Override
        public E last() {
            return descendingIterator().next();
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return Sets.filter((NavigableSet) unfiltered().subSet(e, z, e2, z2), (Predicate) this.predicate);
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return Sets.filter((NavigableSet) unfiltered().headSet(e, z), (Predicate) this.predicate);
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return Sets.filter((NavigableSet) unfiltered().tailSet(e, z), (Predicate) this.predicate);
        }
    }

    public static <B> Set<List<B>> cartesianProduct(List<? extends Set<? extends B>> list) {
        return CartesianSet.create(list);
    }

    public static <B> Set<List<B>> cartesianProduct(Set<? extends B>... setArr) {
        return cartesianProduct(Arrays.asList(setArr));
    }

    private static final class CartesianSet<E> extends ForwardingCollection<List<E>> implements Set<List<E>> {
        private final transient ImmutableList<ImmutableSet<E>> axes;
        private final transient CartesianList<E> delegate;

        static <E> Set<List<E>> create(List<? extends Set<? extends E>> list) {
            ImmutableList.Builder builder = new ImmutableList.Builder(list.size());
            Iterator<? extends Set<? extends E>> it = list.iterator();
            while (it.hasNext()) {
                ImmutableSet immutableSetCopyOf = ImmutableSet.copyOf((Collection) it.next());
                if (immutableSetCopyOf.isEmpty()) {
                    return ImmutableSet.of();
                }
                builder.add(immutableSetCopyOf);
            }
            final ImmutableList<E> immutableListBuild = builder.build();
            return new CartesianSet(immutableListBuild, new CartesianList(new ImmutableList<List<E>>() {
                @Override
                public int size() {
                    return immutableListBuild.size();
                }

                @Override
                public List<E> get(int i) {
                    return ((ImmutableSet) immutableListBuild.get(i)).asList();
                }

                @Override
                boolean isPartialView() {
                    return true;
                }
            }));
        }

        private CartesianSet(ImmutableList<ImmutableSet<E>> immutableList, CartesianList<E> cartesianList) {
            this.axes = immutableList;
            this.delegate = cartesianList;
        }

        @Override
        protected Collection<List<E>> delegate() {
            return this.delegate;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof CartesianSet) {
                return this.axes.equals(((CartesianSet) obj).axes);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            int i = 1;
            int size = size() - 1;
            for (int i2 = 0; i2 < this.axes.size(); i2++) {
                size = ~(~(size * 31));
            }
            UnmodifiableIterator<ImmutableSet<E>> it = this.axes.iterator();
            while (it.hasNext()) {
                ImmutableSet<E> next = it.next();
                i = ~(~((31 * i) + ((size() / next.size()) * next.hashCode())));
            }
            return ~(~(i + size));
        }
    }

    public static <E> Set<Set<E>> powerSet(Set<E> set) {
        return new PowerSet(set);
    }

    private static final class SubSet<E> extends AbstractSet<E> {
        private final ImmutableMap<E, Integer> inputSet;
        private final int mask;

        SubSet(ImmutableMap<E, Integer> immutableMap, int i) {
            this.inputSet = immutableMap;
            this.mask = i;
        }

        @Override
        public Iterator<E> iterator() {
            return new UnmodifiableIterator<E>() {
                final ImmutableList<E> elements;
                int remainingSetBits;

                {
                    this.elements = SubSet.this.inputSet.keySet().asList();
                    this.remainingSetBits = SubSet.this.mask;
                }

                @Override
                public boolean hasNext() {
                    return this.remainingSetBits != 0;
                }

                @Override
                public E next() {
                    int iNumberOfTrailingZeros = Integer.numberOfTrailingZeros(this.remainingSetBits);
                    if (iNumberOfTrailingZeros == 32) {
                        throw new NoSuchElementException();
                    }
                    this.remainingSetBits &= ~(1 << iNumberOfTrailingZeros);
                    return this.elements.get(iNumberOfTrailingZeros);
                }
            };
        }

        @Override
        public int size() {
            return Integer.bitCount(this.mask);
        }

        @Override
        public boolean contains(Object obj) {
            Integer num = this.inputSet.get(obj);
            if (num != null) {
                if (((1 << num.intValue()) & this.mask) != 0) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class PowerSet<E> extends AbstractSet<Set<E>> {
        final ImmutableMap<E, Integer> inputSet;

        PowerSet(Set<E> set) {
            ImmutableMap.Builder builder = ImmutableMap.builder();
            Iterator<E> it = ((Set) Preconditions.checkNotNull(set)).iterator();
            int i = 0;
            while (it.hasNext()) {
                builder.put(it.next(), Integer.valueOf(i));
                i++;
            }
            this.inputSet = builder.build();
            Preconditions.checkArgument(this.inputSet.size() <= 30, "Too many elements to create power set: %s > 30", Integer.valueOf(this.inputSet.size()));
        }

        @Override
        public int size() {
            return 1 << this.inputSet.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<Set<E>> iterator() {
            return new AbstractIndexedListIterator<Set<E>>(size()) {
                @Override
                protected Set<E> get(int i) {
                    return new SubSet(PowerSet.this.inputSet, i);
                }
            };
        }

        @Override
        public boolean contains(Object obj) {
            if (obj instanceof Set) {
                return this.inputSet.keySet().containsAll((Set) obj);
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PowerSet) {
                return this.inputSet.equals(((PowerSet) obj).inputSet);
            }
            return super.equals(obj);
        }

        @Override
        public int hashCode() {
            return this.inputSet.keySet().hashCode() << (this.inputSet.size() - 1);
        }

        @Override
        public String toString() {
            return "powerSet(" + this.inputSet + ")";
        }
    }

    static int hashCodeImpl(Set<?> set) {
        Iterator<?> it = set.iterator();
        int i = 0;
        while (it.hasNext()) {
            Object next = it.next();
            i = ~(~(i + (next != null ? next.hashCode() : 0)));
        }
        return i;
    }

    static boolean equalsImpl(Set<?> set, Object obj) {
        if (set == obj) {
            return true;
        }
        if (!(obj instanceof Set)) {
            return false;
        }
        Set set2 = (Set) obj;
        try {
            if (set.size() == set2.size()) {
                if (set.containsAll(set2)) {
                    return true;
                }
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public static <E> NavigableSet<E> unmodifiableNavigableSet(NavigableSet<E> navigableSet) {
        if ((navigableSet instanceof ImmutableSortedSet) || (navigableSet instanceof UnmodifiableNavigableSet)) {
            return navigableSet;
        }
        return new UnmodifiableNavigableSet(navigableSet);
    }

    static final class UnmodifiableNavigableSet<E> extends ForwardingSortedSet<E> implements Serializable, NavigableSet<E> {
        private static final long serialVersionUID = 0;
        private final NavigableSet<E> delegate;
        private transient UnmodifiableNavigableSet<E> descendingSet;

        UnmodifiableNavigableSet(NavigableSet<E> navigableSet) {
            this.delegate = (NavigableSet) Preconditions.checkNotNull(navigableSet);
        }

        @Override
        protected SortedSet<E> delegate() {
            return Collections.unmodifiableSortedSet(this.delegate);
        }

        @Override
        public E lower(E e) {
            return this.delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return this.delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return this.delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return this.delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            UnmodifiableNavigableSet<E> unmodifiableNavigableSet = this.descendingSet;
            if (unmodifiableNavigableSet == null) {
                UnmodifiableNavigableSet<E> unmodifiableNavigableSet2 = new UnmodifiableNavigableSet<>(this.delegate.descendingSet());
                this.descendingSet = unmodifiableNavigableSet2;
                unmodifiableNavigableSet2.descendingSet = this;
                return unmodifiableNavigableSet2;
            }
            return unmodifiableNavigableSet;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return Iterators.unmodifiableIterator(this.delegate.descendingIterator());
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return Sets.unmodifiableNavigableSet(this.delegate.subSet(e, z, e2, z2));
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return Sets.unmodifiableNavigableSet(this.delegate.headSet(e, z));
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return Sets.unmodifiableNavigableSet(this.delegate.tailSet(e, z));
        }
    }

    public static <E> NavigableSet<E> synchronizedNavigableSet(NavigableSet<E> navigableSet) {
        return Synchronized.navigableSet(navigableSet);
    }

    static boolean removeAllImpl(Set<?> set, Iterator<?> it) {
        boolean zRemove = false;
        while (it.hasNext()) {
            zRemove |= set.remove(it.next());
        }
        return zRemove;
    }

    static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
        Preconditions.checkNotNull(collection);
        if (collection instanceof Multiset) {
            collection = ((Multiset) collection).elementSet();
        }
        if ((collection instanceof Set) && collection.size() > set.size()) {
            return Iterators.removeAll(set.iterator(), collection);
        }
        return removeAllImpl(set, collection.iterator());
    }

    static class DescendingSet<E> extends ForwardingNavigableSet<E> {
        private final NavigableSet<E> forward;

        DescendingSet(NavigableSet<E> navigableSet) {
            this.forward = navigableSet;
        }

        @Override
        protected NavigableSet<E> delegate() {
            return this.forward;
        }

        @Override
        public E lower(E e) {
            return this.forward.higher(e);
        }

        @Override
        public E floor(E e) {
            return this.forward.ceiling(e);
        }

        @Override
        public E ceiling(E e) {
            return this.forward.floor(e);
        }

        @Override
        public E higher(E e) {
            return this.forward.lower(e);
        }

        @Override
        public E pollFirst() {
            return this.forward.pollLast();
        }

        @Override
        public E pollLast() {
            return this.forward.pollFirst();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            return this.forward;
        }

        @Override
        public Iterator<E> descendingIterator() {
            return this.forward.iterator();
        }

        @Override
        public NavigableSet<E> subSet(E e, boolean z, E e2, boolean z2) {
            return this.forward.subSet(e2, z2, e, z).descendingSet();
        }

        @Override
        public NavigableSet<E> headSet(E e, boolean z) {
            return this.forward.tailSet(e, z).descendingSet();
        }

        @Override
        public NavigableSet<E> tailSet(E e, boolean z) {
            return this.forward.headSet(e, z).descendingSet();
        }

        @Override
        public Comparator<? super E> comparator() {
            Comparator<? super E> comparator = this.forward.comparator();
            if (comparator == null) {
                return Ordering.natural().reverse();
            }
            return reverse(comparator);
        }

        private static <T> Ordering<T> reverse(Comparator<T> comparator) {
            return Ordering.from(comparator).reverse();
        }

        @Override
        public E first() {
            return this.forward.last();
        }

        @Override
        public SortedSet<E> headSet(E e) {
            return standardHeadSet(e);
        }

        @Override
        public E last() {
            return this.forward.first();
        }

        @Override
        public SortedSet<E> subSet(E e, E e2) {
            return standardSubSet(e, e2);
        }

        @Override
        public SortedSet<E> tailSet(E e) {
            return standardTailSet(e);
        }

        @Override
        public Iterator<E> iterator() {
            return this.forward.descendingIterator();
        }

        @Override
        public Object[] toArray() {
            return standardToArray();
        }

        @Override
        public <T> T[] toArray(T[] tArr) {
            return (T[]) standardToArray(tArr);
        }

        @Override
        public String toString() {
            return standardToString();
        }
    }
}
