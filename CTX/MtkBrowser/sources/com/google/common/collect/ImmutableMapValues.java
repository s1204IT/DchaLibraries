package com.google.common.collect;

import java.io.Serializable;
import java.util.Map;

final class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
    private final ImmutableMap<K, V> map;

    private static class SerializedForm<V> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<?, V> map;

        SerializedForm(ImmutableMap<?, V> immutableMap) {
            this.map = immutableMap;
        }

        Object readResolve() {
            return this.map.values();
        }
    }

    ImmutableMapValues(ImmutableMap<K, V> immutableMap) {
        this.map = immutableMap;
    }

    @Override
    public boolean contains(Object obj) {
        return obj != null && Iterators.contains(iterator(), obj);
    }

    @Override
    ImmutableList<V> createAsList() {
        return new ImmutableAsList<V>(this, this.map.entrySet().asList()) {
            final ImmutableMapValues this$0;
            final ImmutableList val$entryList;

            {
                this.this$0 = this;
                this.val$entryList = immutableList;
            }

            @Override
            ImmutableCollection<V> delegateCollection() {
                return this.this$0;
            }

            @Override
            public V get(int i) {
                return (V) ((Map.Entry) this.val$entryList.get(i)).getValue();
            }
        };
    }

    @Override
    boolean isPartialView() {
        return true;
    }

    @Override
    public UnmodifiableIterator<V> iterator() {
        return Maps.valueIterator((UnmodifiableIterator) this.map.entrySet().iterator());
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    Object writeReplace() {
        return new SerializedForm(this.map);
    }
}
