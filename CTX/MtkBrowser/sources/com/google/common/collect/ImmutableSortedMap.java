package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;

public abstract class ImmutableSortedMap<K, V> extends ImmutableSortedMapFauxverideShim<K, V> implements NavigableMap<K, V> {
    private static final long serialVersionUID = 0;
    private transient ImmutableSortedMap<K, V> descendingMap;
    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();
    private static final ImmutableSortedMap<Comparable, Object> NATURAL_EMPTY_MAP = new EmptyImmutableSortedMap(NATURAL_ORDER);

    public static class Builder<K, V> extends ImmutableMap.Builder<K, V> {
        private final Comparator<? super K> comparator;

        public Builder(Comparator<? super K> comparator) {
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        @Override
        public ImmutableSortedMap<K, V> build() {
            return ImmutableSortedMap.fromEntries(this.comparator, false, this.size, this.entries);
        }

        @Override
        public Builder<K, V> put(K k, V v) {
            super.put((Object) k, (Object) v);
            return this;
        }
    }

    private static class SerializedForm extends ImmutableMap.SerializedForm {
        private static final long serialVersionUID = 0;
        private final Comparator<Object> comparator;

        SerializedForm(ImmutableSortedMap<?, ?> immutableSortedMap) {
            super(immutableSortedMap);
            this.comparator = immutableSortedMap.comparator();
        }

        @Override
        Object readResolve() {
            return createMap(new Builder(this.comparator));
        }
    }

    ImmutableSortedMap() {
    }

    ImmutableSortedMap(ImmutableSortedMap<K, V> immutableSortedMap) {
        this.descendingMap = immutableSortedMap;
    }

    static <K, V> ImmutableSortedMap<K, V> emptyMap(Comparator<? super K> comparator) {
        return Ordering.natural().equals(comparator) ? of() : new EmptyImmutableSortedMap(comparator);
    }

    static <K, V> ImmutableSortedMap<K, V> from(ImmutableSortedSet<K> immutableSortedSet, ImmutableList<V> immutableList) {
        return immutableSortedSet.isEmpty() ? emptyMap(immutableSortedSet.comparator()) : new RegularImmutableSortedMap((RegularImmutableSortedSet) immutableSortedSet, immutableList);
    }

    static <K, V> ImmutableSortedMap<K, V> fromEntries(Comparator<? super K> comparator, boolean z, int i, Map.Entry<K, V>... entryArr) {
        for (int i2 = 0; i2 < i; i2++) {
            Map.Entry<K, V> entry = entryArr[i2];
            entryArr[i2] = entryOf(entry.getKey(), entry.getValue());
        }
        if (!z) {
            sortEntries(comparator, i, entryArr);
            validateEntries(i, entryArr, comparator);
        }
        return fromSortedEntries(comparator, i, entryArr);
    }

    static <K, V> ImmutableSortedMap<K, V> fromSortedEntries(Comparator<? super K> comparator, int i, Map.Entry<K, V>[] entryArr) {
        if (i == 0) {
            return emptyMap(comparator);
        }
        ImmutableList.Builder builder = ImmutableList.builder();
        ImmutableList.Builder builder2 = ImmutableList.builder();
        for (int i2 = 0; i2 < i; i2++) {
            Map.Entry<K, V> entry = entryArr[i2];
            builder.add(entry.getKey());
            builder2.add(entry.getValue());
        }
        return new RegularImmutableSortedMap(new RegularImmutableSortedSet(builder.build(), comparator), builder2.build());
    }

    public static <K, V> ImmutableSortedMap<K, V> of() {
        return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
    }

    private static <K, V> void sortEntries(Comparator<? super K> comparator, int i, Map.Entry<K, V>[] entryArr) {
        Arrays.sort(entryArr, 0, i, Ordering.from(comparator).onKeys());
    }

    private static <K, V> void validateEntries(int i, Map.Entry<K, V>[] entryArr, Comparator<? super K> comparator) {
        for (int i2 = 1; i2 < i; i2++) {
            int i3 = i2 - 1;
            checkNoConflict(comparator.compare(entryArr[i3].getKey(), entryArr[i2].getKey()) != 0, "key", entryArr[i3], entryArr[i2]);
        }
    }

    @Override
    public Map.Entry<K, V> ceilingEntry(K k) {
        return tailMap((Object) k, true).firstEntry();
    }

    @Override
    public K ceilingKey(K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    @Override
    public Comparator<? super K> comparator() {
        return keySet().comparator();
    }

    @Override
    public boolean containsValue(Object obj) {
        return values().contains(obj);
    }

    abstract ImmutableSortedMap<K, V> createDescendingMap();

    @Override
    public ImmutableSortedSet<K> descendingKeySet() {
        return keySet().descendingSet();
    }

    @Override
    public ImmutableSortedMap<K, V> descendingMap() {
        ImmutableSortedMap<K, V> immutableSortedMap = this.descendingMap;
        if (immutableSortedMap != null) {
            return immutableSortedMap;
        }
        ImmutableSortedMap<K, V> immutableSortedMapCreateDescendingMap = createDescendingMap();
        this.descendingMap = immutableSortedMapCreateDescendingMap;
        return immutableSortedMapCreateDescendingMap;
    }

    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return super.entrySet();
    }

    @Override
    public Map.Entry<K, V> firstEntry() {
        if (isEmpty()) {
            return null;
        }
        return entrySet().asList().get(0);
    }

    @Override
    public K firstKey() {
        return keySet().first();
    }

    @Override
    public Map.Entry<K, V> floorEntry(K k) {
        return headMap((Object) k, true).lastEntry();
    }

    @Override
    public K floorKey(K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    @Override
    public ImmutableSortedMap<K, V> headMap(K k) {
        return headMap((Object) k, false);
    }

    @Override
    public abstract ImmutableSortedMap<K, V> headMap(K k, boolean z);

    @Override
    public Map.Entry<K, V> higherEntry(K k) {
        return tailMap((Object) k, false).firstEntry();
    }

    @Override
    public K higherKey(K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @Override
    boolean isPartialView() {
        return keySet().isPartialView() || values().isPartialView();
    }

    @Override
    public abstract ImmutableSortedSet<K> keySet();

    @Override
    public Map.Entry<K, V> lastEntry() {
        if (isEmpty()) {
            return null;
        }
        return entrySet().asList().get(size() - 1);
    }

    @Override
    public K lastKey() {
        return keySet().last();
    }

    @Override
    public Map.Entry<K, V> lowerEntry(K k) {
        return headMap((Object) k, false).lastEntry();
    }

    @Override
    public K lowerKey(K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @Override
    public ImmutableSortedSet<K> navigableKeySet() {
        return keySet();
    }

    @Override
    @Deprecated
    public final Map.Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final Map.Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return values().size();
    }

    @Override
    public ImmutableSortedMap<K, V> subMap(K k, K k2) {
        return subMap((Object) k, true, (Object) k2, false);
    }

    @Override
    public ImmutableSortedMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(k2);
        Preconditions.checkArgument(comparator().compare(k, k2) <= 0, "expected fromKey <= toKey but %s > %s", k, k2);
        return headMap((Object) k2, z2).tailMap((Object) k, z);
    }

    @Override
    public ImmutableSortedMap<K, V> tailMap(K k) {
        return tailMap((Object) k, true);
    }

    @Override
    public abstract ImmutableSortedMap<K, V> tailMap(K k, boolean z);

    @Override
    public abstract ImmutableCollection<V> values();

    @Override
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
