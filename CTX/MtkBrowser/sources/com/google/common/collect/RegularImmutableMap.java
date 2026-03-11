package com.google.common.collect;

import com.google.common.collect.ImmutableMapEntry;
import java.util.Map;

final class RegularImmutableMap<K, V> extends ImmutableMap<K, V> {
    private static final long serialVersionUID = 0;
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] table;

    private class EntrySet extends ImmutableMapEntrySet<K, V> {
        final RegularImmutableMap this$0;

        private EntrySet(RegularImmutableMap regularImmutableMap) {
            this.this$0 = regularImmutableMap;
        }

        @Override
        ImmutableList<Map.Entry<K, V>> createAsList() {
            return new RegularImmutableAsList(this, this.this$0.entries);
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

    private static final class NonTerminalMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        private final ImmutableMapEntry<K, V> nextInKeyBucket;

        NonTerminalMapEntry(ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
            super(immutableMapEntry);
            this.nextInKeyBucket = immutableMapEntry2;
        }

        NonTerminalMapEntry(K k, V v, ImmutableMapEntry<K, V> immutableMapEntry) {
            super(k, v);
            this.nextInKeyBucket = immutableMapEntry;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return null;
        }
    }

    RegularImmutableMap(int i, ImmutableMapEntry.TerminalEntry<?, ?>[] terminalEntryArr) {
        this.entries = createEntryArray(i);
        int iClosedTableSize = Hashing.closedTableSize(i, 1.2d);
        this.table = createEntryArray(iClosedTableSize);
        this.mask = iClosedTableSize - 1;
        for (int i2 = 0; i2 < i; i2++) {
            ?? nonTerminalMapEntry = terminalEntryArr[i2];
            Object key = nonTerminalMapEntry.getKey();
            int iSmear = this.mask & Hashing.smear(key.hashCode());
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            if (immutableMapEntry != null) {
                nonTerminalMapEntry = new NonTerminalMapEntry(nonTerminalMapEntry, immutableMapEntry);
            }
            this.table[iSmear] = nonTerminalMapEntry;
            this.entries[i2] = nonTerminalMapEntry;
            checkNoConflictInBucket(key, nonTerminalMapEntry, immutableMapEntry);
        }
    }

    RegularImmutableMap(Map.Entry<?, ?>[] entryArr) {
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
            int iSmear = this.mask & Hashing.smear(key.hashCode());
            ImmutableMapEntry<K, V> immutableMapEntry = this.table[iSmear];
            ImmutableMapEntry<K, V> terminalEntry = immutableMapEntry == null ? new ImmutableMapEntry.TerminalEntry<>(key, value) : new NonTerminalMapEntry<>(key, value, immutableMapEntry);
            this.table[iSmear] = terminalEntry;
            this.entries[i] = terminalEntry;
            checkNoConflictInBucket(key, terminalEntry, immutableMapEntry);
        }
    }

    private void checkNoConflictInBucket(K k, ImmutableMapEntry<K, V> immutableMapEntry, ImmutableMapEntry<K, V> immutableMapEntry2) {
        while (immutableMapEntry2 != null) {
            checkNoConflict(!k.equals(immutableMapEntry2.getKey()), "key", immutableMapEntry, immutableMapEntry2);
            immutableMapEntry2 = immutableMapEntry2.getNextInKeyBucket();
        }
    }

    private ImmutableMapEntry<K, V>[] createEntryArray(int i) {
        return new ImmutableMapEntry[i];
    }

    @Override
    ImmutableSet<Map.Entry<K, V>> createEntrySet() {
        return new EntrySet();
    }

    @Override
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

    @Override
    boolean isPartialView() {
        return false;
    }

    @Override
    public int size() {
        return this.entries.length;
    }
}
