package com.google.common.collect;

import com.google.common.base.Preconditions;
import java.util.Map;

final class RegularImmutableSortedMap<K, V> extends ImmutableSortedMap<K, V> {
    private final transient RegularImmutableSortedSet<K> keySet;
    private final transient ImmutableList<V> valueList;

    private class EntrySet extends ImmutableMapEntrySet<K, V> {
        final RegularImmutableSortedMap this$0;

        private EntrySet(RegularImmutableSortedMap regularImmutableSortedMap) {
            this.this$0 = regularImmutableSortedMap;
        }

        @Override
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new ImmutableAsList<Map.Entry<K, V>>(this) {
                private final ImmutableList<K> keyList;
                final EntrySet this$1;

                {
                    this.this$1 = this;
                    this.keyList = this.this$1.this$0.keySet().asList();
                }

                @Override
                ImmutableCollection<Map.Entry<K, V>> delegateCollection() {
                    return this.this$1;
                }

                @Override
                public Map.Entry<K, V> get(int i) {
                    return Maps.immutableEntry(this.keyList.get(i), this.this$1.this$0.valueList.get(i));
                }
            };
        }

        @Override
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override
        ImmutableMap<K, V> map() {
            return this.this$0;
        }
    }

    RegularImmutableSortedMap(RegularImmutableSortedSet<K> regularImmutableSortedSet, ImmutableList<V> immutableList) {
        this.keySet = regularImmutableSortedSet;
        this.valueList = immutableList;
    }

    RegularImmutableSortedMap(RegularImmutableSortedSet<K> regularImmutableSortedSet, ImmutableList<V> immutableList, ImmutableSortedMap<K, V> immutableSortedMap) {
        super(immutableSortedMap);
        this.keySet = regularImmutableSortedSet;
        this.valueList = immutableList;
    }

    private ImmutableSortedMap<K, V> getSubMap(int i, int i2) {
        return (i == 0 && i2 == size()) ? this : i == i2 ? emptyMap(comparator()) : from(this.keySet.getSubSet(i, i2), this.valueList.subList(i, i2));
    }

    @Override
    ImmutableSortedMap<K, V> createDescendingMap() {
        return new RegularImmutableSortedMap((RegularImmutableSortedSet) this.keySet.descendingSet(), this.valueList.reverse(), this);
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet();
    }

    @Override
    public V get(Object obj) {
        int iIndexOf = this.keySet.indexOf(obj);
        if (iIndexOf == -1) {
            return null;
        }
        return this.valueList.get(iIndexOf);
    }

    @Override
    public ImmutableSortedMap<K, V> headMap(K k, boolean z) {
        return getSubMap(0, this.keySet.headIndex((K) Preconditions.checkNotNull(k), z));
    }

    @Override
    public ImmutableSortedSet<K> keySet() {
        return this.keySet;
    }

    @Override
    public ImmutableSortedMap<K, V> tailMap(K k, boolean z) {
        return getSubMap(this.keySet.tailIndex((K) Preconditions.checkNotNull(k), z), size());
    }

    @Override
    public ImmutableCollection<V> values() {
        return this.valueList;
    }
}
