package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;

/* loaded from: classes.dex */
public abstract class ImmutableSortedMap<K, V> extends ImmutableSortedMapFauxverideShim<K, V> implements NavigableMap<K, V> {
    private static final long serialVersionUID = 0;
    private transient ImmutableSortedMap<K, V> descendingMap;
    private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();
    private static final ImmutableSortedMap<Comparable, Object> NATURAL_EMPTY_MAP = new EmptyImmutableSortedMap(NATURAL_ORDER);

    abstract ImmutableSortedMap<K, V> createDescendingMap();

    public abstract ImmutableSortedMap<K, V> headMap(K k, boolean z);

    /* JADX DEBUG: Method merged with bridge method: keySet()Lcom/google/common/collect/ImmutableSet; */
    /* JADX DEBUG: Method merged with bridge method: keySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public abstract ImmutableSortedSet<K> keySet();

    public abstract ImmutableSortedMap<K, V> tailMap(K k, boolean z);

    /* JADX DEBUG: Method merged with bridge method: values()Ljava/util/Collection; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map, java.util.SortedMap
    public abstract ImmutableCollection<V> values();

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public /* bridge */ /* synthetic */ NavigableMap headMap(Object obj, boolean z) {
        return headMap((ImmutableSortedMap<K, V>) obj, z);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.NavigableMap, java.util.SortedMap
    public /* bridge */ /* synthetic */ SortedMap headMap(Object obj) {
        return headMap((ImmutableSortedMap<K, V>) obj);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.NavigableMap
    public /* bridge */ /* synthetic */ NavigableMap subMap(Object obj, boolean z, Object obj2, boolean z2) {
        return subMap((boolean) obj, z, (boolean) obj2, z2);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    public /* bridge */ /* synthetic */ NavigableMap tailMap(Object obj, boolean z) {
        return tailMap((ImmutableSortedMap<K, V>) obj, z);
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.NavigableMap, java.util.SortedMap
    public /* bridge */ /* synthetic */ SortedMap tailMap(Object obj) {
        return tailMap((ImmutableSortedMap<K, V>) obj);
    }

    static <K, V> ImmutableSortedMap<K, V> emptyMap(Comparator<? super K> comparator) {
        if (Ordering.natural().equals(comparator)) {
            return of();
        }
        return new EmptyImmutableSortedMap(comparator);
    }

    static <K, V> ImmutableSortedMap<K, V> fromSortedEntries(Comparator<? super K> comparator, int i, Map.Entry<K, V>[] entryArr) {
        if (i == 0) {
            return emptyMap(comparator);
        }
        ImmutableList.Builder builder = ImmutableList.builder();
        ImmutableList.Builder builder2 = ImmutableList.builder();
        for (int i2 = 0; i2 < i; i2++) {
            Map.Entry<K, V> entry = entryArr[i2];
            builder.add((ImmutableList.Builder) entry.getKey());
            builder2.add((ImmutableList.Builder) entry.getValue());
        }
        return new RegularImmutableSortedMap(new RegularImmutableSortedSet(builder.build(), comparator), builder2.build());
    }

    static <K, V> ImmutableSortedMap<K, V> from(ImmutableSortedSet<K> immutableSortedSet, ImmutableList<V> immutableList) {
        if (immutableSortedSet.isEmpty()) {
            return emptyMap(immutableSortedSet.comparator());
        }
        return new RegularImmutableSortedMap((RegularImmutableSortedSet) immutableSortedSet, immutableList);
    }

    public static <K, V> ImmutableSortedMap<K, V> of() {
        return (ImmutableSortedMap<K, V>) NATURAL_EMPTY_MAP;
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

    private static <K, V> void sortEntries(Comparator<? super K> comparator, int i, Map.Entry<K, V>[] entryArr) {
        Arrays.sort(entryArr, 0, i, Ordering.from(comparator).onKeys());
    }

    private static <K, V> void validateEntries(int i, Map.Entry<K, V>[] entryArr, Comparator<? super K> comparator) {
        for (int i2 = 1; i2 < i; i2++) {
            int i3 = i2 - 1;
            checkNoConflict(comparator.compare(entryArr[i3].getKey(), entryArr[i2].getKey()) != 0, "key", entryArr[i3], entryArr[i2]);
        }
    }

    public static class Builder<K, V> extends ImmutableMap.Builder<K, V> {
        private final Comparator<? super K> comparator;

        /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.lang.Object */
        /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.lang.Object */
        /* JADX WARN: Multi-variable type inference failed */
        @Override // com.google.common.collect.ImmutableMap.Builder
        public /* bridge */ /* synthetic */ ImmutableMap.Builder put(Object obj, Object obj2) {
            return put((Builder<K, V>) obj, obj2);
        }

        public Builder(Comparator<? super K> comparator) {
            this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        }

        @Override // com.google.common.collect.ImmutableMap.Builder
        public Builder<K, V> put(K k, V v) {
            super.put((Builder<K, V>) k, (K) v);
            return this;
        }

        /* JADX DEBUG: Method merged with bridge method: build()Lcom/google/common/collect/ImmutableMap; */
        @Override // com.google.common.collect.ImmutableMap.Builder
        public ImmutableSortedMap<K, V> build() {
            return ImmutableSortedMap.fromEntries(this.comparator, false, this.size, this.entries);
        }
    }

    ImmutableSortedMap() {
    }

    ImmutableSortedMap(ImmutableSortedMap<K, V> immutableSortedMap) {
        this.descendingMap = immutableSortedMap;
    }

    public int size() {
        return values().size();
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public boolean containsValue(Object obj) {
        return values().contains(obj);
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return keySet().isPartialView() || values().isPartialView();
    }

    /* JADX DEBUG: Method merged with bridge method: entrySet()Ljava/util/Set; */
    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        return super.entrySet();
    }

    @Override // java.util.SortedMap
    public Comparator<? super K> comparator() {
        return keySet().comparator();
    }

    @Override // java.util.SortedMap
    public K firstKey() {
        return keySet().first();
    }

    @Override // java.util.SortedMap
    public K lastKey() {
        return keySet().last();
    }

    @Override // java.util.NavigableMap, java.util.SortedMap
    public ImmutableSortedMap<K, V> headMap(K k) {
        return headMap((ImmutableSortedMap<K, V>) k, false);
    }

    /* JADX DEBUG: Method merged with bridge method: subMap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/SortedMap; */
    @Override // java.util.NavigableMap, java.util.SortedMap
    public ImmutableSortedMap<K, V> subMap(K k, K k2) {
        return subMap((boolean) k, true, (boolean) k2, false);
    }

    @Override // java.util.NavigableMap
    public ImmutableSortedMap<K, V> subMap(K k, boolean z, K k2, boolean z2) {
        Preconditions.checkNotNull(k);
        Preconditions.checkNotNull(k2);
        Preconditions.checkArgument(comparator().compare(k, k2) <= 0, "expected fromKey <= toKey but %s > %s", k, k2);
        return headMap((ImmutableSortedMap<K, V>) k2, z2).tailMap((ImmutableSortedMap<K, V>) k, z);
    }

    @Override // java.util.NavigableMap, java.util.SortedMap
    public ImmutableSortedMap<K, V> tailMap(K k) {
        return tailMap((ImmutableSortedMap<K, V>) k, true);
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> lowerEntry(K k) {
        return headMap((ImmutableSortedMap<K, V>) k, false).lastEntry();
    }

    @Override // java.util.NavigableMap
    public K lowerKey(K k) {
        return (K) Maps.keyOrNull(lowerEntry(k));
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> floorEntry(K k) {
        return headMap((ImmutableSortedMap<K, V>) k, true).lastEntry();
    }

    @Override // java.util.NavigableMap
    public K floorKey(K k) {
        return (K) Maps.keyOrNull(floorEntry(k));
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> ceilingEntry(K k) {
        return tailMap((ImmutableSortedMap<K, V>) k, true).firstEntry();
    }

    @Override // java.util.NavigableMap
    public K ceilingKey(K k) {
        return (K) Maps.keyOrNull(ceilingEntry(k));
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> higherEntry(K k) {
        return tailMap((ImmutableSortedMap<K, V>) k, false).firstEntry();
    }

    @Override // java.util.NavigableMap
    public K higherKey(K k) {
        return (K) Maps.keyOrNull(higherEntry(k));
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> firstEntry() {
        if (isEmpty()) {
            return null;
        }
        return entrySet().asList().get(0);
    }

    @Override // java.util.NavigableMap
    public Map.Entry<K, V> lastEntry() {
        if (isEmpty()) {
            return null;
        }
        return entrySet().asList().get(size() - 1);
    }

    @Override // java.util.NavigableMap
    @Deprecated
    public final Map.Entry<K, V> pollFirstEntry() {
        throw new UnsupportedOperationException();
    }

    @Override // java.util.NavigableMap
    @Deprecated
    public final Map.Entry<K, V> pollLastEntry() {
        throw new UnsupportedOperationException();
    }

    /* JADX DEBUG: Method merged with bridge method: descendingMap()Ljava/util/NavigableMap; */
    @Override // java.util.NavigableMap
    public ImmutableSortedMap<K, V> descendingMap() {
        ImmutableSortedMap<K, V> immutableSortedMap = this.descendingMap;
        if (immutableSortedMap == null) {
            ImmutableSortedMap<K, V> immutableSortedMapCreateDescendingMap = createDescendingMap();
            this.descendingMap = immutableSortedMapCreateDescendingMap;
            return immutableSortedMapCreateDescendingMap;
        }
        return immutableSortedMap;
    }

    /* JADX DEBUG: Method merged with bridge method: navigableKeySet()Ljava/util/NavigableSet; */
    @Override // java.util.NavigableMap
    public ImmutableSortedSet<K> navigableKeySet() {
        return keySet();
    }

    /* JADX DEBUG: Method merged with bridge method: descendingKeySet()Ljava/util/NavigableSet; */
    @Override // java.util.NavigableMap
    public ImmutableSortedSet<K> descendingKeySet() {
        return keySet().descendingSet();
    }

    private static class SerializedForm extends ImmutableMap.SerializedForm {
        private static final long serialVersionUID = 0;
        private final Comparator<Object> comparator;

        SerializedForm(ImmutableSortedMap<?, ?> immutableSortedMap) {
            super(immutableSortedMap);
            this.comparator = immutableSortedMap.comparator();
        }

        @Override // com.google.common.collect.ImmutableMap.SerializedForm
        Object readResolve() {
            return createMap(new Builder(this.comparator));
        }
    }

    @Override // com.google.common.collect.ImmutableMap
    Object writeReplace() {
        return new SerializedForm(this);
    }
}
