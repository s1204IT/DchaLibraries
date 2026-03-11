package com.google.common.collect;

import java.io.Serializable;
import java.util.Map;

final class ImmutableMapKeySet<K, V> extends ImmutableSet<K> {
    private final ImmutableMap<K, V> map;

    private static class KeySetSerializedForm<K> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<K, ?> map;

        KeySetSerializedForm(ImmutableMap<K, ?> immutableMap) {
            this.map = immutableMap;
        }

        Object readResolve() {
            return this.map.keySet();
        }
    }

    ImmutableMapKeySet(ImmutableMap<K, V> immutableMap) {
        this.map = immutableMap;
    }

    @Override
    public boolean contains(Object obj) {
        return this.map.containsKey(obj);
    }

    @Override
    ImmutableList<K> createAsList() {
        return new ImmutableAsList<K>(this, this.map.entrySet().asList()) {
            final ImmutableMapKeySet this$0;
            final ImmutableList val$entryList;

            {
                this.this$0 = this;
                this.val$entryList = immutableList;
            }

            @Override
            ImmutableCollection<K> delegateCollection() {
                return this.this$0;
            }

            @Override
            public K get(int i) {
                return (K) ((Map.Entry) this.val$entryList.get(i)).getKey();
            }
        };
    }

    @Override
    boolean isPartialView() {
        return true;
    }

    @Override
    public UnmodifiableIterator<K> iterator() {
        return asList().iterator();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    Object writeReplace() {
        return new KeySetSerializedForm(this.map);
    }
}
