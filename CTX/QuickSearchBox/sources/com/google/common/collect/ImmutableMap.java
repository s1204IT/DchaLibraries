package com.google.common.collect;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMapEntry;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

public abstract class ImmutableMap<K, V> implements Serializable, Map<K, V> {
    private static final Map.Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Map.Entry[0];
    private transient ImmutableSet<Map.Entry<K, V>> entrySet;
    private transient ImmutableSet<K> keySet;
    private transient ImmutableCollection<V> values;

    public static class Builder<K, V> {
        ImmutableMapEntry.TerminalEntry<K, V>[] entries;
        int size;

        public Builder() {
            this(4);
        }

        Builder(int i) {
            this.entries = new ImmutableMapEntry.TerminalEntry[i];
            this.size = 0;
        }

        private void ensureCapacity(int i) {
            if (i > this.entries.length) {
                this.entries = (ImmutableMapEntry.TerminalEntry[]) ObjectArrays.arraysCopyOf(this.entries, ImmutableCollection.Builder.expandedCapacity(this.entries.length, i));
            }
        }

        public ImmutableMap<K, V> build() {
            switch (this.size) {
                case 0:
                    return ImmutableMap.of();
                case 1:
                    return ImmutableMap.of((Object) this.entries[0].getKey(), (Object) this.entries[0].getValue());
                default:
                    return new RegularImmutableMap(this.size, this.entries);
            }
        }

        public Builder<K, V> put(K k, V v) {
            ensureCapacity(this.size + 1);
            ImmutableMapEntry.TerminalEntry<K, V> terminalEntryEntryOf = ImmutableMap.entryOf(k, v);
            ImmutableMapEntry.TerminalEntry<K, V>[] terminalEntryArr = this.entries;
            int i = this.size;
            this.size = i + 1;
            terminalEntryArr[i] = terminalEntryEntryOf;
            return this;
        }
    }

    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        private final Object[] keys;
        private final Object[] values;

        SerializedForm(ImmutableMap<?, ?> immutableMap) {
            this.keys = new Object[immutableMap.size()];
            this.values = new Object[immutableMap.size()];
            UnmodifiableIterator<Map.Entry<?, ?>> it = immutableMap.entrySet().iterator();
            int i = 0;
            while (true) {
                int i2 = i;
                if (!it.hasNext()) {
                    return;
                }
                Map.Entry<?, ?> next = it.next();
                this.keys[i2] = next.getKey();
                this.values[i2] = next.getValue();
                i = i2 + 1;
            }
        }

        Object createMap(Builder<Object, Object> builder) {
            for (int i = 0; i < this.keys.length; i++) {
                builder.put(this.keys[i], this.values[i]);
            }
            return builder.build();
        }

        Object readResolve() {
            return createMap(new Builder<>());
        }
    }

    ImmutableMap() {
    }

    static void checkNoConflict(boolean z, String str, Map.Entry<?, ?> entry, Map.Entry<?, ?> entry2) {
        if (z) {
            return;
        }
        throw new IllegalArgumentException("Multiple entries with same " + str + ": " + entry + " and " + entry2);
    }

    public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
        if ((map instanceof ImmutableMap) && !(map instanceof ImmutableSortedMap)) {
            ImmutableMap<K, V> immutableMap = (ImmutableMap) map;
            if (!immutableMap.isPartialView()) {
                return immutableMap;
            }
        } else if (map instanceof EnumMap) {
            return copyOfEnumMapUnsafe(map);
        }
        Map.Entry[] entryArr = (Map.Entry[]) map.entrySet().toArray(EMPTY_ENTRY_ARRAY);
        switch (entryArr.length) {
            case 0:
                return of();
            case 1:
                Map.Entry entry = entryArr[0];
                return of(entry.getKey(), entry.getValue());
            default:
                return new RegularImmutableMap(entryArr);
        }
    }

    private static <K extends Enum<K>, V> ImmutableMap<K, V> copyOfEnumMap(Map<K, ? extends V> map) {
        EnumMap enumMap = new EnumMap(map);
        for (Map.Entry<K, V> entry : enumMap.entrySet()) {
            CollectPreconditions.checkEntryNotNull(entry.getKey(), entry.getValue());
        }
        return ImmutableEnumMap.asImmutable(enumMap);
    }

    private static <K, V> ImmutableMap<K, V> copyOfEnumMapUnsafe(Map<? extends K, ? extends V> map) {
        return copyOfEnumMap((EnumMap) map);
    }

    static <K, V> ImmutableMapEntry.TerminalEntry<K, V> entryOf(K k, V v) {
        CollectPreconditions.checkEntryNotNull(k, v);
        return new ImmutableMapEntry.TerminalEntry<>(k, v);
    }

    public static <K, V> ImmutableMap<K, V> of() {
        return ImmutableBiMap.of();
    }

    public static <K, V> ImmutableMap<K, V> of(K k, V v) {
        return ImmutableBiMap.of((Object) k, (Object) v);
    }

    @Override
    @Deprecated
    public final void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(Object obj) {
        return get(obj) != null;
    }

    @Override
    public boolean containsValue(Object obj) {
        return values().contains(obj);
    }

    abstract ImmutableSet<Map.Entry<K, V>> createEntrySet();

    ImmutableSet<K> createKeySet() {
        return new ImmutableMapKeySet(this);
    }

    @Override
    public ImmutableSet<Map.Entry<K, V>> entrySet() {
        ImmutableSet<Map.Entry<K, V>> immutableSet = this.entrySet;
        if (immutableSet != null) {
            return immutableSet;
        }
        ImmutableSet<Map.Entry<K, V>> immutableSetCreateEntrySet = createEntrySet();
        this.entrySet = immutableSetCreateEntrySet;
        return immutableSetCreateEntrySet;
    }

    @Override
    public boolean equals(Object obj) {
        return Maps.equalsImpl(this, obj);
    }

    public abstract V get(Object obj);

    @Override
    public int hashCode() {
        return entrySet().hashCode();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    abstract boolean isPartialView();

    @Override
    public ImmutableSet<K> keySet() {
        ImmutableSet<K> immutableSet = this.keySet;
        if (immutableSet != null) {
            return immutableSet;
        }
        ImmutableSet<K> immutableSetCreateKeySet = createKeySet();
        this.keySet = immutableSetCreateKeySet;
        return immutableSetCreateKeySet;
    }

    @Override
    @Deprecated
    public final V put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public final V remove(Object obj) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        return Maps.toStringImpl(this);
    }

    @Override
    public ImmutableCollection<V> values() {
        ImmutableCollection<V> immutableCollection = this.values;
        if (immutableCollection != null) {
            return immutableCollection;
        }
        ImmutableMapValues immutableMapValues = new ImmutableMapValues(this);
        this.values = immutableMapValues;
        return immutableMapValues;
    }

    Object writeReplace() {
        return new SerializedForm(this);
    }
}
