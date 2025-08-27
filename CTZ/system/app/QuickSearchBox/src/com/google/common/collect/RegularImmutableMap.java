package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.util.Map;

/* loaded from: classes.dex */
final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
    private static final long serialVersionUID = 0;
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] table;

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r5v1, types: [com.google.common.collect.RegularImmutableMap$NonTerminalMapEntry] */
    /* JADX WARN: Type inference failed for: r6v0, types: [com.google.common.collect.RegularImmutableMap, com.google.common.collect.RegularImmutableMap<K, V>] */
    RegularImmutableMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        this.entries = createEntryArray(i);
        int iClosedTableSize = Hashing.closedTableSize(i, 1.2d);
        this.table = createEntryArray(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        for (int i2 = 0; i2 < i; i2++) {
            ImmutableMapEntry.TerminalEntry<?, ?> nonTerminalMapEntry = terminalEntryArr[i2];
            Object key = nonTerminalMapEntry.getKey();
            int iSmear = Hashing.smear(key.hashCode()) & this.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            if (immutableMapEntry != null) {
                nonTerminalMapEntry = new NonTerminalMapEntry(nonTerminalMapEntry, immutableMapEntry);
            }
            this.table[iSmear] = nonTerminalMapEntry;
            this.entries[i2] = nonTerminalMapEntry;
            checkNoConflictInBucket(key, nonTerminalMapEntry, immutableMapEntry);
        }
    }

    /* JADX DEBUG: Multi-variable search result rejected for r7v0, resolved type: com.google.common.collect.RegularImmutableMap<K, V> */
    /* JADX WARN: Multi-variable type inference failed */
    RegularImmutableMap(Map.Entry<?, ?>[] entryArr) {
        ImmutableMapEntry<K, V> nonTerminalMapEntry;
        int length = entryArr.length;
        this.entries = createEntryArray(length);
        int iClosedTableSize = Hashing.closedTableSize(length, 1.2d);
        this.table = createEntryArray(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        for (int i = 0; i < length; i++) {
            Map.Entry<?, ?> entry = entryArr[i];
            Object key = entry.getKey();
            Object value = entry.getValue();
            CollectPreconditions.checkEntryNotNull(key, value);
            int iSmear = Hashing.smear(key.hashCode()) & this.mask;
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            if (immutableMapEntry == null) {
                nonTerminalMapEntry = new ImmutableMapEntry.TerminalEntry<>(key, value);
            } else {
                nonTerminalMapEntry = new NonTerminalMapEntry<>(key, value, immutableMapEntry);
            }
            this.table[iSmear] = nonTerminalMapEntry;
            this.entries[i] = nonTerminalMapEntry;
            checkNoConflictInBucket(key, nonTerminalMapEntry, immutableMapEntry);
        }
    }

    private void checkNoConflictInBucket(K k, ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
        while (immutableMapEntry2 != null) {
            checkNoConflict(!k.equals(immutableMapEntry2.getKey()), "key", immutableMapEntry, immutableMapEntry2);
            immutableMapEntry2 = immutableMapEntry2.getNextInKeyBucket();
        }
    }

    private static final class NonTerminalMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;

        NonTerminalMapEntry(K k, V v, ImmutableMapEntry<K, V> immutableMapEntry) {
            super(k, v);
            this.nextInKeyBucket = immutableMapEntry;
        }

        NonTerminalMapEntry(ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
            super(immutableMapEntry);
            this.nextInKeyBucket = immutableMapEntry2;
        }

        @Override // com.google.common.collect.ImmutableMapEntry
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        @Override // com.google.common.collect.ImmutableMapEntry
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return null;
        }
    }

    private ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override // com.google.common.collect.ImmutableMap, java.util.Map
    public V get(Object obj) {
        if (obj == null) {
            return null;
        }
        for (ImmutableMapEntry<K, V> nextInKeyBucket = this.table[Hashing.smear(obj.hashCode()) & this.mask]; nextInKeyBucket != null; nextInKeyBucket = nextInKeyBucket.getNextInKeyBucket()) {
            if (obj.equals(nextInKeyBucket.getKey())) {
                return nextInKeyBucket.getValue();
            }
        }
        return null;
    }

    @Override // java.util.Map
    public int size() {
        return this.entries.length;
    }

    @Override // com.google.common.collect.ImmutableMap
    boolean isPartialView() {
        return false;
    }

    @Override // com.google.common.collect.ImmutableMap
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet();
    }

    private class EntrySet extends ImmutableMapEntrySet<K, V> {
        private EntrySet() {
        }

        /* synthetic */ EntrySet(RegularImmutableMap regularImmutableMap, AnonymousClass1 anonymousClass1) {
            this();
        }

        @Override // com.google.common.collect.ImmutableMapEntrySet
        ImmutableMap<K, V> map() {
            return RegularImmutableMap.this;
        }

        /* JADX DEBUG: Method merged with bridge method: iterator()Ljava/util/Iterator; */
        @Override // com.google.common.collect.ImmutableSet, com.google.common.collect.ImmutableCollection, java.util.AbstractCollection, java.util.Collection, java.lang.Iterable, java.util.Set, java.util.NavigableSet
        public UnmodifiableIterator<Map.Entry<K, V>> iterator() {
            return asList().iterator();
        }

        @Override // com.google.common.collect.ImmutableCollection
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new RegularImmutableAsList(this, RegularImmutableMap.this.entries);
        }
    }
}
