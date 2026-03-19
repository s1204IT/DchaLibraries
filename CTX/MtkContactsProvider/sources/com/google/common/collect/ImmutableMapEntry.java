package com.google.common.collect;

abstract class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {
    abstract ImmutableMapEntry<K, V> getNextInKeyBucket();

    abstract ImmutableMapEntry<K, V> getNextInValueBucket();

    ImmutableMapEntry(K k, V v) {
        super(k, v);
        CollectPreconditions.checkEntryNotNull(k, v);
    }

    ImmutableMapEntry(ImmutableMapEntry<K, V> immutableMapEntry) {
        super(immutableMapEntry.getKey(), immutableMapEntry.getValue());
    }

    static final class TerminalEntry<K, V> extends ImmutableMapEntry<K, V> {
        TerminalEntry(K k, V v) {
            super(k, v);
        }

        @Override
        ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return null;
        }

        @Override
        ImmutableMapEntry<K, V> getNextInValueBucket() {
            return null;
        }
    }
}
